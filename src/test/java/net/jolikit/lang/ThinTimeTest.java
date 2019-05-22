/*
 * Copyright 2019 Jeff Hain
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
package net.jolikit.lang;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.jolikit.lang.ThinTime;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.ConcUnit;
import junit.framework.TestCase;

public class ThinTimeTest extends TestCase {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Allows to test static behavior.
     */
    private static class MyThinTime extends ThinTime {
        private long sctm;
        private long snt;
        public MyThinTime(
                double futureToleranceRatio,
                long initialMinSctmJumpMs,
                long systemTimeZeroMs) {
            super(
                    futureToleranceRatio,
                    initialMinSctmJumpMs,
                    systemTimeZeroMs);
        }
        @Override
        protected long getSctm() {
            return sctm;
        }
        @Override
        protected long getSnt() {
            return snt;
        }
    }
    
    private class MyCallerRunnable implements Runnable {
        private final long nbrOfCalls;
        public MyCallerRunnable(long nbrOfCalls) {
            this.nbrOfCalls = nbrOfCalls;
        }
        @Override
        public void run() {
            long previousRefNs = System.currentTimeMillis() * (1000L * 1000L);
            long previousCtn = ThinTime.currentTimeNanos();

            for (int i = 0; i < this.nbrOfCalls; i++) {
                final long forwardToleranceNs = 50L * 1000L * 1000L;

                long ref1Ns = previousRefNs;
                long ctn = ThinTime.currentTimeNanos();
                long ref2Ns = System.currentTimeMillis() * (1000L * 1000L);
                long deltaRefNs = ref2Ns - ref1Ns;
                if (deltaRefNs < 0) {
                    cu.onError("time backward jump : " + deltaRefNs + " ns");
                    cu.fail();
                } else {
                    if (Math.abs(deltaRefNs) > (1000L * 1000L * 1000L)) {
                        cu.onError("spent more than 1 second (maybe too many threads) : " + deltaRefNs + " ns");
                    } else {
                        if (ctn < previousCtn) {
                            // If actual time backward jumps a bit, but we don't
                            // notice, ctn might also backward jump and we might
                            // notice it, so it's not necessarily abnormal.
                            cu.onError("ctn backward jump : " + (ctn - previousCtn) + " ns");
                        }
                        if (ctn < ref1Ns) {
                            cu.onError("ctn [" + ctn + "] < ref1NS [" + ref1Ns + "]");
                        } else if (ctn > ref2Ns + forwardToleranceNs) {
                            long surplus = (ctn - (ref2Ns + forwardToleranceNs));
                            cu.onError("ctn [" + ctn + "] > ref2NS [" + ref2Ns + "]  +  forwardToleranceNS [" + forwardToleranceNs + "] by " + surplus + " ns");
                        }
                    }
                }
                previousRefNs = ref2Ns;
                previousCtn = ctn;
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    // We consider the average granularity of ThinTime (for currentTimeMicros
    // and currentTimeNanos) must be at least 10 microseconds.
    private static final long MIN_THIN_TIME_GRANULARITY_NS = 10L * 1000L;
    
    private static final long S_TO_MS = 1000L;
    private static final long MS_TO_NS = 1000L * 1000L;
    private static final long S_TO_NS = 1000L * 1000L * 1000L;
    
    private final ConcUnit cu = new ConcUnit();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_currentTimeMillis() {

        final long toleranceMs = 1000L;
        assertTrue(Math.abs(ThinTime.currentTimeMillis() - System.currentTimeMillis()) < toleranceMs);
    }
    
    public void test_currentTimeMicros_granularity() {

        final long toleranceMs = 1000L;
        assertTrue(Math.abs(ThinTime.currentTimeMicros()/1000L - System.currentTimeMillis()) < toleranceMs);
        
        final int nbrOfRounds = 100000;
        
        long previousTime = 0;
        long time;
        long dateA;
        long dateB;
        
        int nbrOfChanges;
        
        nbrOfChanges = 0;
        dateA = System.nanoTime();
        for (int i = 0; i < nbrOfRounds; i++) {
            time = ThinTime.currentTimeMicros();
            if (time != previousTime) {
                nbrOfChanges++;
            }
            previousTime = time;
        }
        dateB = System.nanoTime();
        assertTrue(nbrOfChanges >= (long)((dateB - dateA)/(double)MIN_THIN_TIME_GRANULARITY_NS));
    }
    
    public void test_currentTimeNanos_granularity() {

        final long toleranceMs = 1000L;
        assertTrue(Math.abs(ThinTime.currentTimeNanos()/1000000L - System.currentTimeMillis()) < toleranceMs);
        
        final int nbrOfRounds = 100000;
        long previousTime = 0;
        long time;
        long dateA;
        long dateB;
        
        int nbrOfChanges;
        
        nbrOfChanges = 0;
        dateA = System.nanoTime();
        for (int i = 0; i < nbrOfRounds; i++) {
            time = ThinTime.currentTimeNanos();
            if (time != previousTime) {
                nbrOfChanges++;
            }
            previousTime = time;
        }
        dateB = System.nanoTime();
        assertTrue(nbrOfChanges >= (long)((dateB - dateA)/(double)MIN_THIN_TIME_GRANULARITY_NS));
    }

    public void test_sequentialBehavior_minSCTMJump() {
        final double futureToleranceRatio = 2.0;
        final long initialMinSctmJumpMs = 1000L;
        final long systemTimeZeroMs = 123;
        MyThinTime tt = new MyThinTime(
                futureToleranceRatio,
                initialMinSctmJumpMs,
                systemTimeZeroMs);
        
        assertEquals(initialMinSctmJumpMs, tt.getMinSctmJumpMs());

        // doesn't get lower on first call,
        // which initializes previous SCTM value
        tt.sctm += 100L;
        tt.currentTimeNanos_();
        assertEquals(initialMinSctmJumpMs, tt.getMinSctmJumpMs());

        // gets lower
        tt.sctm += 100L;
        tt.currentTimeNanos_();
        assertEquals(100L, tt.getMinSctmJumpMs());
        
        // gets lower again
        tt.sctm += 10L;
        tt.currentTimeNanos_();
        assertEquals(10L, tt.getMinSctmJumpMs());
        
        // doesn't grow
        tt.sctm += 100L;
        tt.currentTimeNanos_();
        assertEquals(10L, tt.getMinSctmJumpMs());
    }

    public void test_sequentialBehavior_regular() {
        final double futureToleranceRatio = 2.0;
        final long initialMinSctmJumpMs = 1000L;
        final long systemTimeZeroMs = 123;
        MyThinTime tt = new MyThinTime(
                futureToleranceRatio,
                initialMinSctmJumpMs,
                systemTimeZeroMs);
        
        long expectedNs;
        long expectedRefSctm;
        long expectedRefSnt;
        long minSctmJumpMs;

        // initial time
        expectedNs = (tt.sctm - systemTimeZeroMs) * MS_TO_NS;
        expectedRefSctm = -systemTimeZeroMs;
        expectedRefSnt = 0;
        minSctmJumpMs = initialMinSctmJumpMs;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call didn't change time ref
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());

        // sctm and snt both getting ahead 10 seconds
        tt.sctm += 10 * S_TO_MS;
        tt.snt += 10 * S_TO_NS;
        expectedNs += 10 * S_TO_NS;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call didn't change time ref
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump (smaller than our jump)
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());
    }

    public void test_sequentialBehavior_backwardJumps() {
        final double futureToleranceRatio = 2.0;
        final long initialMinSctmJumpMs = 1000L;
        final long systemTimeZeroMs = 123;
        MyThinTime tt = new MyThinTime(
                futureToleranceRatio,
                initialMinSctmJumpMs,
                systemTimeZeroMs);
        
        long expectedNs = tt.currentTimeNanos_();
        long expectedRefSctm = tt.getTimeRef().getRefSctm();
        long expectedRefSnt = tt.getTimeRef().getRefSnt();
        long minSctmJumpMs = tt.getMinSctmJumpMs();

        // snt jumped 1ns forward: cnt changes accordingly,
        // and time ref is not recomputed
        tt.snt += 1;
        expectedNs += 1;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call didn't change time ref
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());

        // snt jumped back 1ns: not returning a time < to previously returned,
        // but since computed ctn is not < sctm, time ref is not recomputed
        tt.snt -= 1;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call didn't change time ref
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());
        
        // snt jumped backward 1ns again: not returning a time < to previously returned,
        // and since computed ctn is < sctm, time ref is recomputed
        tt.snt -= 1;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call changed time ref
        expectedRefSctm = tt.sctm - systemTimeZeroMs;
        expectedRefSnt = tt.snt;
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());
        
        // sctm jumped backward, of exactly "future tolerance ratio * min SCTM jump":
        // ctn just goes backward 1ns (due to previous backward jump of stn),
        // and time ref is unchanged
        tt.sctm -= (long)(futureToleranceRatio * minSctmJumpMs);
        expectedNs -= 1;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call didn't change time ref
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());
        
        // sctm jumped backward 1ms, i.e. now computed ctn is past future tolerance.
        // Backward time jump is detected: time ref is recomputed.
        tt.sctm -= 1;
        expectedNs = (tt.sctm - systemTimeZeroMs) * MS_TO_NS;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call did change time ref
        expectedRefSctm = tt.sctm - systemTimeZeroMs;
        expectedRefSnt = tt.snt;
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump (1ms is smaller, but not considering backward jumps)
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());
    }

    public void test_sequentialBehavior_forwardJumps() {
        final double futureToleranceRatio = 2.0;
        final long initialMinSctmJumpMs = 1000L;
        final long systemTimeZeroMs = 123;
        MyThinTime tt = new MyThinTime(
                futureToleranceRatio,
                initialMinSctmJumpMs,
                systemTimeZeroMs);
        
        long deltaNs;
        
        long expectedNs = tt.currentTimeNanos_();
        long expectedRefSctm = tt.getTimeRef().getRefSctm();
        long expectedRefSnt = tt.getTimeRef().getRefSnt();
        long minSctmJumpMs = tt.getMinSctmJumpMs();
        
        // moving snt at future tolerance
        deltaNs = (long)(futureToleranceRatio * minSctmJumpMs * MS_TO_NS);
        tt.snt += deltaNs;
        expectedNs += deltaNs;
        // computing ctn
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call didn't change time ref
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());

        // moving snt 1ns past future tolerance:
        // won't go past it, and will recompute time ref,
        // but won't return "sctm - offset" as current time,
        // since this would make returned time jump backward:
        // instead, returns the same as previously returned
        tt.snt += 1;
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call did change time ref
        expectedRefSctm = tt.sctm - systemTimeZeroMs;
        expectedRefSnt = tt.snt;
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());

        // moving sctm 10s ahead, to get away of previous returned time,
        // and moving snt 1ns past future tolerance:
        // time ref will be recomputed
        tt.sctm += 10 * S_TO_MS;
        tt.snt += 10 * S_TO_NS + (long)(futureToleranceRatio * minSctmJumpMs * MS_TO_NS) + 1;
        expectedNs = (tt.sctm - systemTimeZeroMs) * MS_TO_NS;
        assertEquals(expectedNs, tt.currentTimeNanos_());
        // call did change time ref
        expectedRefSctm = tt.sctm - systemTimeZeroMs;
        expectedRefSnt = tt.snt;
        assertEquals(expectedRefSctm, tt.getTimeRef().getRefSctm());
        assertEquals(expectedRefSnt, tt.getTimeRef().getRefSnt());
        // call did't change min SCTM jump (which is smaller)
        assertEquals(minSctmJumpMs, tt.getMinSctmJumpMs());
    }
    
    /*
     * 
     */
    
    public void test_currentTimeNanos_behavior() {
        final ExecutorService executor = Executors.newCachedThreadPool();
        
        final long nbrOfCalls = 1000L * 1000L;
        
        final int nbrOfThreads = 1 + Runtime.getRuntime().availableProcessors();
        
        for (int i = 0; i < nbrOfThreads; i++) {
            executor.execute(new MyCallerRunnable(nbrOfCalls));
        }
        
        Unchecked.shutdownAndAwaitTermination(executor);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void tearDown() {
        cu.assertNoError();
    }
}
