/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.test.cases.utils;

import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.lang.PostPaddedAtomicInteger;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * To stop benches that might take too long.
 * 
 * Doesn't use thread interrupt, for it could disturb benched treatments.
 * 
 * Each instance must be used for one bench at a time.
 * 
 * This should have much less overhead than checking System.nanoTime()
 * after each call, and checking it every few calls might cause
 * too long benches if called treatments are really slow.
 */
public class BenchTimeoutManager {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyTimeoutRunnable implements Runnable {
        /**
         * Padded to avoid false sharing.
         * 0 = false
         * 1 = true
         */
        private final PostPaddedAtomicInteger timeoutFlag =
                new PostPaddedAtomicInteger();
        @Override
        public void run() {
            this.timeoutFlag.set(1);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Static instance, not to have tons of instances and their threads
     * pile up when doing a lot of benches.
     */
    private static final HardScheduler INTERRUPTER =
            BwdTestUtils.newHardScheduler(
                    "BENCH INTERRUPTER", 1);
    
    /**
     * Reference only used in bench thread(s).
     * No need to be volatile, even if bench calls isTimeoutReached()
     * from multiple threads, since is set before bench starts.
     */
    private MyTimeoutRunnable newestTimeoutRunnable;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BenchTimeoutManager() {
    }
    
    /**
     * To be called just before bench start.
     * 
     * @param benchTimeoutS Timeout, in seconds, after which bench
     *        must time out.
     */
    public void onBenchStart(double benchTimeoutS) {
        /*
         * A new instance for each bench, for timeouts
         * of previous benches not to interfere.
         */
        final MyTimeoutRunnable runnable = new MyTimeoutRunnable();
        this.newestTimeoutRunnable = runnable;
        INTERRUPTER.executeAfterS(runnable, benchTimeoutS);
    }
    
    /**
     * To be called during bench, possibly from multiple threads,
     * as long as anything mutated before call to onBenchStart()
     * is visible to them.
     */
    public boolean isTimeoutReached() {
        // Not supposed to be null, since onBenchStart() must have been called before.
        final MyTimeoutRunnable runnable = this.newestTimeoutRunnable;
        return runnable.timeoutFlag.get() != 0;
    }
    
    /**
     * To be called after bench end, on same thread than onBenchStart().
     */
    public void onBenchEnd() {
        /*
         * Not doing much here, but if we ever need
         * to do mandatory cleanup this is the place.
         */
        // For NPE in isTimeoutReached()
        // if onBenchStart() is not called next time.
        this.newestTimeoutRunnable = null;
    }
}
