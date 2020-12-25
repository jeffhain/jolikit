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
 * A splittable to run multiple runnables in parallel.
 * If a runnable is also a splittable, it can be further split.
 * 
 * Considers worth to split as long as it contains at least 2 runnables.
 * 
 * Can contain at most Integer.MAX_VALUE (or so, depending on JVM) runnables.
 */
public class RunnableSplittable implements InterfaceSplittable {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Runnable[] runnableArr;
    
    private final int from;
    
    private int length;
    
    /**
     * RunnableSplittable to use for splitting attempts.
     * 
     * If runnableArr only contains a single splittable, which is itself a
     * RunnableSplittable which contains a single splittable, etc.,
     * with the last splittable allowing for any and many splits,
     * this reference allows us to directly access the RunnableSplittable
     * that contains it, without having to go through multiple
     * RunnableSplittable for each worthToSplit() and split() call
     * (having piles of RunnableSplittables could be a usual pattern).
     */
    private RunnableSplittable toTrySplit;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Does not check that runnables in [from,from+length[ are not null,
     * but they must not be null.
     * 
     * @param runnableArr Array containing runnables in [from,from+length[.
     * @param from Index of first runnable.
     * @param length Number of runnables. Must be > 0.
     * @throws NullPointerException if the specified array is null.
     * @throws IllegalArgumentException if length is 0.
     * @throws IndexOutOfBoundsException if [from,from+length[ is outside array
     *         bounds.
     */
    public <ER extends Runnable> RunnableSplittable(
            ER[] runnableArr,
            int from,
            int length) {
        // Implicit null check.
        LangUtils.checkBounds(runnableArr.length, from, length);
        // Must have something to run.
        NbrsUtils.requireSup(0, length, "length");
        this.runnableArr = runnableArr;
        this.from = from;
        this.length = length;
        this.toTrySplit = this;
    }
    
    /**
     * Convenience constructor.
     * 
     * @param runnableColl Collection containing the runnables to run.
     * @throws IllegalArgumentException if collection is empty.
     * @throws NullPointerException if the specified collection is null.
     */
    public <ER extends Runnable> RunnableSplittable(Collection<ER> runnableColl) {
        // Implicit null check.
        final Runnable[] runnableArr = runnableColl.toArray(new Runnable[runnableColl.size()]);
        // Must have something to run.
        NbrsUtils.requireSup(0, runnableArr.length, "length");
        this.runnableArr = runnableArr;
        this.from = 0;
        this.length = runnableArr.length;
        this.toTrySplit = this;
    }

    @Override
    public boolean worthToSplit() {
        return worthToSplit(this.toTrySplit);
    }

    @Override
    public InterfaceSplittable split() {
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
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Low-overhead constructor for internal usage.
     */
    private <ER extends Runnable> RunnableSplittable(
            Void nnul,
            //
            ER[] runnableArr,
            int from,
            int length) {
        this.runnableArr = runnableArr;
        this.from = from;
        this.length = length;
        this.toTrySplit = this;
    }

    /*
     * Static methods to be sure not to use "this" by mistake.
     */
    
    private static boolean worthToSplit(RunnableSplittable that) {
        if (that.length > 1) {
            return true;
        }
        // Length is never 0.
        final Runnable theOne = that.runnableArr[that.from];
        if (theOne instanceof InterfaceSplittable) {
            final InterfaceSplittable impl = (InterfaceSplittable) theOne;
            if (impl.worthToSplit()) {
                if (impl instanceof RunnableSplittable) {
                    that.toTrySplit = (RunnableSplittable) impl;
                }
                return true;
            }
        }
        return false;
    }

    private static InterfaceSplittable split(RunnableSplittable that) {
        if (that.length > 1) {
            // Amount that we keep (allows from to be final, and might also help
            // with memory caching, since we keep lower array).
            final int halfish = (that.length >> 1);
            final RunnableSplittable res = new RunnableSplittable(
                    null,
                    that.runnableArr,
                    that.from + halfish,
                    that.length - halfish);
            that.length = halfish;
            return res;
        } else {
            // Since "that" is worth to split (else we would not be here) and
            // its length is 1, it necessarily contain a splittable.
            final InterfaceSplittable impl = (InterfaceSplittable) that.runnableArr[that.from];
            return impl.split();
        }
    }
}
