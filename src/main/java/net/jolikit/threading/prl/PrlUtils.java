/*
 * Copyright 2019-2024 Jeff Hain
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

import java.util.List;
import java.util.concurrent.Executor;

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * Utilities for parallelization.
 */
public class PrlUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double LOG2_INV = 1.0 / Math.log(2.0);
    
    /**
     * Null object pattern, but using a better prefix than "NULL".
     * 
     * split() method throws UnsupportedOperationException.
     */
    public static final InterfaceSplittable NOOP_SPLITTABLE = new InterfaceSplittable() {
        @Override
        public boolean worthToSplit() {
            return false;
        }
        @Override
        public InterfaceSplittable split() {
            throw new UnsupportedOperationException();
        }
        @Override
        public void run() {
        }
    };
    
    /**
     * Null object pattern, but using a better prefix than "NULL".
     * 
     * split() and merge(...) method throw UnsupportedOperationException.
     */
    public static final InterfaceSplitmergable NOOP_SPLITMERGABLE = new InterfaceSplitmergable() {
        @Override
        public boolean worthToSplit() {
            return false;
        }
        @Override
        public InterfaceSplitmergable split() {
            throw new UnsupportedOperationException();
        }
        @Override
        public void run() {
        }
        @Override
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            throw new UnsupportedOperationException();
        }
    };
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Equivalent to computeMaxDepth(parallelism, discrepancy = 0).
     * 
     * You might want to add about 2 to the returned max depth
     * to balance load among workers in case of uneven splits
     * or workers resources.
     * 
     * @param parallelism Must be >= 1.
     * @return Max splitting/forking depth needed to cover
     *         the specified parallelism.
     */
    public static int computeMaxDepth(int parallelism) {
        NbrsUtils.requireSupOrEq(1, parallelism, "parallelism");
        final double maxDepthFp = Math.log(parallelism) * LOG2_INV;
        return (int) Math.ceil(maxDepthFp);
    }
    
    /**
     * Method to compute max depth of split tree, from parallelism and a measure
     * of parallel treatments unbalance, or "discrepancy", which doesn't depend
     * on parallelism.
     * 
     * Warn: Discrepancy is mostly useful for pathological cases or tests.
     * In practice, the more parallelism and tasks we have,
     * the more tasks discrepancies average out among workers,
     * so using even just a discrepancy of 1 could cause many
     * unecessary splits, and instead of computing theoretical max depth
     * corresponding to a discrepancy, it's preferable to just add 2 or so
     * to max depth corresponding to parallelism (i.e. with a discrepancy of 0)
     * (cf. computeMaxDepth(parallelism)).
     * 
     * Discrepancy:
     * When a splittable is split, its amount of work W is split into
     * Wl (left) and Wr (right): W = Wl + Wr.
     * We define work discrepancy as a measure of the difference between Wl
     * and Wr when they are not equal, such as (with Wl >= Wr by convention):
     * Wl = Wr * (1 + discrepancy).
     * Similarly, we can define CPU discrepancy as a measure
     * of the difference of CPU time between threads.
     * The specified discrepancy is the sum of these two discrepancies.
     * 
     * @param parallelism Must be >= 1.
     * @param discrepancy Must be >= 0.
     * @return Max splitting/forking depth needed to cover
     *         the specified parallelism and balance the load
     *         considering the specified discrepancy.
     */
    public static int computeMaxDepth(
            int parallelism,
            int discrepancy) {
        NbrsUtils.requireSupOrEq(1, parallelism, "parallelism");
        NbrsUtils.requireSupOrEq(0, discrepancy, "discrepancy");
        /*
         * Wl = Wr * (1 + discrepancy)
         * gives:
         * Wl = W * ((1 + discrepancy) / (2 + discrepancy))
         * Wr = W * (1 / (2 + discrepancy))
         * and, with depth (in split tree) >= 0:
         * Wmax = Winitial * (((1 + discrepancy) / (2 + discrepancy)) ^ depth)
         * Wmin = Winitial * ((1 / (2 + discrepancy)) ^ depth)
         * 
         * For a discrepancy of zero we have:
         * MaxDepth = ceil(log2(parallelism))
         * 
         * For a non-zero discrepancy, we (arbitrarily) want to reach a depth
         * where Wmax is inferior or equal to work amount at max depth
         * for zero discrepancy, so we want depth such as,
         * with C = (1 + discrepancy) / (2 + discrepancy):
         * Winitial * (C ^ depth) = Winitial * (1 / (2 ^ log2(parallelism)))
         * C ^ depth = 1 / (2 ^ log2(parallelism))
         * or, with F = 1/C:
         * F ^ depth = 2 ^ log2(parallelism)
         * 2 ^ (depth * log2(F)) = 2 ^ log2(parallelism)
         * depth * log2(F) = log2(parallelism)
         * depth = log2(parallelism) / log2(F)
         */
        final double f = (2.0 + discrepancy) / (1.0 + discrepancy);
        final double maxDepthFp = Math.log(parallelism) / Math.log(f);
        return (int) Math.ceil(maxDepthFp);
    }

    /**
     * @param runnables Runnables.
     * @return A simple Runnable if zero or one runnable, else a splittable, to
     *         allow for parallel execution.
     */
    public static <R extends Runnable> Runnable toRunnableOrSplittable(List<R> runnables) {
        final int size = runnables.size();
        if (size == 0) {
            return LangUtils.NOOP_RUNNABLE;
        } else if (size == 1) {
            return runnables.get(0);
        } else {
            return new RunnableSplittable(runnables);
        }
    }

    /**
     * Only uses the parallelizer if its parallelism is at least 2,
     * and if there are at least two runnables, or a single splittable
     * worth to be split, else runs runnables sequentially.
     * 
     * This method allows not to bother creating a single Runnable
     * if having multiple ones and no parallelism, without having
     * to make parallelizer heavier with a method taking a list as argument.
     * 
     * @param parallelizer The executor to use, possibly a parallelizer
     *        (i.e. implementing InterfaceParallelizer).
     * @param runnableList The list of runnables to execute, possibly splittables
     *        (i.e. implementing InterfaceSplittable).
     */
    public static <R extends Runnable> void executeElseRun(
            Executor parallelizer,
            List<R> runnableList) {
        
        final int parallelism;
        if (parallelizer instanceof InterfaceParallelizer) {
            final InterfaceParallelizer prlItf = (InterfaceParallelizer) parallelizer;
            // Implicit null check.
            parallelism = prlItf.getParallelism();
        } else {
            if (parallelizer == null) {
                throw new NullPointerException();
            }
            parallelism = 1;
        }

        // Implicit null check.
        final int size = runnableList.size();
        
        /*
         * Eventually using the parallelizer.
         */
        
        boolean didExecute = false;
        
        if (parallelism >= 2) {
            if (size >= 2) {
                parallelizer.execute(new RunnableSplittable(runnableList));
                didExecute = true;
            } else if (size == 1) {
                final Runnable runnable = runnableList.get(0);
                if (runnable instanceof InterfaceSplittable) {
                    // The parallelizer should just execute sequentially
                    // if not worth to split, not bothering to do so
                    // here even if it could save a call stack element.
                    parallelizer.execute(runnable);
                    didExecute = true;
                }
            }
        }
        
        if (!didExecute) {
            /*
             * Falling back to sequential run.
             */
            
            for (int i = 0; i < size; i++) {
                final Runnable runnable = runnableList.get(i);
                runnable.run();
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private PrlUtils() {
    }
}
