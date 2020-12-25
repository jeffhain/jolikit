/*
 * Copyright 2019-2020 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jolikit.threading.prl;

import java.util.Collection;

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * A splitmergable to run multiple runnables in parallel.
 * If a runnable is also a splittable or splitmergable, it can be further split
 * and merged.
 * 
 * Considers worth to split as long as it contains at least 2 runnables.
 * 
 * Can contain at most Integer.MAX_VALUE (or so, depending on JVM) runnables.
 */
public class RunnableSplitmergable implements InterfaceSplitmergable {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * To deal with splittables as splitmergables.
     */
    private static class MySplittableAdapter implements InterfaceSplitmergable {
        private final InterfaceSplittable splittable;
        public MySplittableAdapter(InterfaceSplittable splittable) {
            this.splittable = splittable;
        }
        public static InterfaceSplitmergable asSplitmergable(InterfaceSplittable splittable) {
            if (splittable instanceof InterfaceSplitmergable) {
                return (InterfaceSplitmergable) splittable;
            } else {
                return new MySplittableAdapter(splittable);
            }
        }
        @Override
        public boolean worthToSplit() {
            return this.splittable.worthToSplit();
        }
        @Override
        public InterfaceSplitmergable split() {
            return asSplitmergable(this.splittable.split());
        }
        @Override
        public void run() {
            this.splittable.run();
        }
        @Override
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            // Nothing to do.
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Runnable[] runnableArr;
    
    private final int from;
    
    private int length;
    
    /**
     * RunnableSplitmergable to use for splitting attempts.
     * 
     * If runnableArr only contains a single splitmergable, which is itself a
     * RunnableSplitmergable which contains a single splitmergable, etc.,
     * with the last splitmergable allowing for any and many splits,
     * this reference allows us to directly access the RunnableSplitmergable
     * that contains it, without having to go through multiple
     * RunnableSplitmergable for each worthToSplit() and split() call
     * (having piles of RunnableSplitmergables could be a usual pattern).
     */
    private RunnableSplitmergable toTrySplit;
    
    private final Runnable tailRunnable;
    
    /**
     * To know when to execute tail runnable, if any.
     */
    private int currentDepth = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Does not check that runnables in [from,from+length[ are not null.
     * 
     * @param runnableArr Array containing runnables in [from,from+length[.
     * @param from Index of first runnable.
     * @param length Number of runnables. Must be > 0.
     * @param tailRunnable Runnable executed after all the other specified
     *        runnables and their eventual splits have been executed.
     *        Can be null.
     * @throws NullPointerException if the specified array is null.
     * @throws IllegalArgumentException if length is 0.
     * @throws IndexOutOfBoundsException if [from,from+length[ is outside array
     *         bounds.
     */
    public <ER extends Runnable> RunnableSplitmergable(
            ER[] runnableArr,
            int from,
            int length,
            Runnable tailRunnable) {
        // Implicit null check.
        LangUtils.checkBounds(runnableArr.length, from, length);
        // Must have something to run.
        NbrsUtils.requireSup(0, length, "length");
        this.runnableArr = runnableArr;
        this.from = from;
        this.length = length;
        this.toTrySplit = this;
        this.tailRunnable = tailRunnable;
    }
    
    /**
     * Convenience constructor.
     * 
     * @param runnableColl Collection containing the runnables to run.
     * @param tailRunnable Runnable executed after all the other specified
     *        runnables and their eventual splits have been executed.
     *        Can be null.
     * @throws IllegalArgumentException if collection is empty.
     * @throws NullPointerException if the specified collection is null.
     */
    public <ER extends Runnable> RunnableSplitmergable(
            Collection<ER> runnableColl,
            Runnable tailRunnable) {
        // Implicit null check.
        final Runnable[] runnableArr = runnableColl.toArray(new Runnable[runnableColl.size()]);
        // Must have something to run.
        NbrsUtils.requireSup(0, runnableArr.length, "length");
        this.runnableArr = runnableArr;
        this.from = 0;
        this.length = runnableArr.length;
        this.toTrySplit = this;
        this.tailRunnable = tailRunnable;
    }

    @Override
    public boolean worthToSplit() {
        return worthToSplit(this.toTrySplit);
    }

    @Override
    public InterfaceSplitmergable split() {
        return split(this.toTrySplit);
    }

    @Override
    public void run() {
        final Runnable[] runnables = this.runnableArr;
        final int bound = this.from + this.length;
        for (int i = this.from; i < bound; i++) {
            final Runnable runnable = runnables[i];
            runnable.run();
        }
    }

    @Override
    public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
        final Runnable tailRunnable = this.tailRunnable;
        if (tailRunnable == null) {
            return;
        }
        
        /*
         * Decrementing depth of each (unless one if it's null),
         * and running the tail runnable if that brings us back to 0.
         */
        
        final RunnableSplitmergable aImpl = (RunnableSplitmergable) a;
        final RunnableSplitmergable bImpl = (RunnableSplitmergable) b;
        
        if (aImpl != null) {
            --aImpl.currentDepth;
        }
        if (bImpl != null) {
            --bImpl.currentDepth;
        }
        
        final int newDepth = ((aImpl != null) ? aImpl.currentDepth : bImpl.currentDepth);
        if (newDepth == 0) {
            tailRunnable.run();
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for internal usage, low-overhead and sets current depth.
     */
    private <ER extends Runnable> RunnableSplitmergable(
            ER[] runnableArr,
            int from,
            int length,
            Runnable tailRunnable,
            int currentDepth) {
        this.runnableArr = runnableArr;
        this.from = from;
        this.length = length;
        this.toTrySplit = this;
        this.tailRunnable = tailRunnable;
        this.currentDepth = currentDepth;
    }

    /*
     * Static methods to be sure not to use "this" by mistake.
     */
    
    private static boolean worthToSplit(RunnableSplitmergable that) {
        if (that.length > 1) {
            return true;
        }
        // Length is never 0.
        final Runnable theOne = that.runnableArr[that.from];
        if (theOne instanceof InterfaceSplittable) {
            final InterfaceSplittable impl = (InterfaceSplittable) theOne;
            if (impl.worthToSplit()) {
                if (impl instanceof RunnableSplitmergable) {
                    that.toTrySplit = (RunnableSplitmergable) impl;
                }
                return true;
            }
        }
        return false;
    }

    private static InterfaceSplitmergable split(RunnableSplitmergable that) {
        ++that.currentDepth;
        if (that.length > 1) {
            // Amount that we keep (allows from to be final, and might also help
            // with memory caching, since we keep lower array).
            final int halfish = (that.length >> 1);
            final RunnableSplitmergable res = new RunnableSplitmergable(
                    that.runnableArr,
                    that.from + halfish,
                    that.length - halfish,
                    that.tailRunnable,
                    that.currentDepth);
            that.length = halfish;
            return res;
        } else {
            // Since "that" is worth to split (else we would not be here) and
            // its length is 1, it necessarily contain a splittable.
            final InterfaceSplittable impl = (InterfaceSplittable) that.runnableArr[that.from];
            return MySplittableAdapter.asSplitmergable(impl.split());
        }
    }
}
