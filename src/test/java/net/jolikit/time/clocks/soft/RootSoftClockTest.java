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
package net.jolikit.time.clocks.soft;

import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.hard.ControllableSystemTimeClock;
import junit.framework.TestCase;

public class RootSoftClockTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final double TOLERANCE_S = 0.1;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_RootSoftClock_nonAFAP() {
        final ControllableSystemTimeClock hardClock = new ControllableSystemTimeClock();
        hardClock.setTimeNsAndTimeSpeed(0L, 1.0);
        
        final RootSoftClock clock = new RootSoftClock(hardClock);
        
        // Must not wait (current time).
        setSoftTimeSAndCheckWaitTimeS(
            clock,
            hardClock.getTimeS(),
            0.0);
        
        // Must wait (future time).
        {
            double timeJumpS = 1.0;
            setSoftTimeSAndCheckWaitTimeS(
                clock,
                hardClock.getTimeS() + timeJumpS,
                timeJumpS);
        }
        
        // Must not wait (past time).
        setSoftTimeSAndCheckWaitTimeS(
            clock,
            hardClock.getTimeS() - 1.0,
            0.0);
    }

    public void test_RootSoftClock_nonAFAP_latenessThreshold() {
        final ControllableSystemTimeClock hardClock = new ControllableSystemTimeClock();
        hardClock.setTimeNsAndTimeSpeed(0L, 1.0);
        
        final RootSoftClock clock = new RootSoftClock(hardClock);
        
        // Default value (always tries to catch up).
        assertEquals(Long.MAX_VALUE, clock.getLatenessThresholdNS());
        
        final double latenessThresholdS = 0.9;
        clock.setLatenessThresholdNS(TimeUtils.sToNs(latenessThresholdS));
        
        /*
         * Target time in the past: will return immediately.
         * Lateness superior to threshold: will store/forgive it.
         */
        
        hardClock.setTimeNs(TimeUtils.sToNs(1000.0));
        double expectedAnnulledLatenessS;
        {
            final double latenessS = 3.0;
            setSoftTimeSAndCheckWaitTimeS(clock, 1000.0 - latenessS, 0.0);
            expectedAnnulledLatenessS =
                latenessS / RootSoftClock.ANNULLED_LATENESS_DIVISOR;
            assertEquals(
                expectedAnnulledLatenessS,
                TimeUtils.nsToS(clock.getAnnuledLatenessNs()),
                TOLERANCE_S);
        }
        
        // Setting threshold resets annulled lateness.
        clock.setLatenessThresholdNS(TimeUtils.sToNs(latenessThresholdS));
        assertEquals(0L, clock.getAnnuledLatenessNs());
        
        /*
         * Target time in the past: will return immediately.
         * Lateness inferior to threshold: will not store/forgive it.
         */
        
        hardClock.setTimeNs(TimeUtils.sToNs(1000.0));
        setSoftTimeSAndCheckWaitTimeS(clock, 999.5, 0.0);
        assertEquals(0L, clock.getAnnuledLatenessNs());
        
        /*
         * Target time is hard time: will return immediately.
         */
        
        setSoftTimeSAndCheckWaitTimeS(clock, 1000.0, 0.0);
        
        /*
         * Target time in the future: will wait.
         */
        
        setSoftTimeSAndCheckWaitTimeS(clock, 1002.0, 2.0);
        
        /*
         * Target time in the past: will return immediately.
         * Lateness superior to threshold:
         * will store/forgive some of it (not all).
         */
        
        hardClock.setTimeNs(TimeUtils.sToNs(1010.0));
        {
            final double latenessS = 1.0;
            // 1009.0
            final double targetTimeS = 1010.0 - latenessS;
            setSoftTimeSAndCheckWaitTimeS(clock, targetTimeS, 0.0);
            expectedAnnulledLatenessS =
                1.0 / RootSoftClock.ANNULLED_LATENESS_DIVISOR;
            assertEquals(
                expectedAnnulledLatenessS,
                TimeUtils.nsToS(clock.getAnnuledLatenessNs()),
                TOLERANCE_S);
        }
        
        /*
         * Target time in the future: will wait some,
         * taking annulled lateness into account.
         */
        
        {
            // 1010.0 (hard time) + 1.0
            final double targetTimeS = 1011.0;
            /*
             * wait time
             * = 1011.0 (target time)
             *   - (1010.0 (hard time) - 0.5 (annulled lateness))
             * = 1011.0 - 1009.5 = 1.5
             */
            final double expectedWaitDurationS = 1.5;
            setSoftTimeSAndCheckWaitTimeS(
                clock,
                targetTimeS,
                expectedWaitDurationS);
            assertEquals(
                expectedAnnulledLatenessS,
                TimeUtils.nsToS(clock.getAnnuledLatenessNs()),
                TOLERANCE_S);
        }
    }

    public void test_RootSoftClock_AFAP() {
        final RootSoftClock clock = new RootSoftClock();
        
        // Must not wait.
        setSoftTimeSAndCheckWaitTimeS(clock, 10.0, 0.0);
        
        // Must not wait.
        setSoftTimeSAndCheckWaitTimeS(clock, 100.0, 0.0);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static double realTimeS() {
        return TimeUtils.nsToS(System.nanoTime());
    }
    
    private static void setSoftTimeSAndCheckWaitTimeS(
        RootSoftClock clock,
        double targetTimeS,
        double expectedWaitDurationS) {
        final double oldTimeS = clock.getTimeS();
        final double aS = realTimeS();
        clock.setTimeNs(TimeUtils.sToNs(targetTimeS));
        final double bS = realTimeS();
        final double actualWaitDurationS = bS - aS;
        if (!(Math.abs(actualWaitDurationS - expectedWaitDurationS) < TOLERANCE_S)) {
            System.out.println("oldTimeS =    " + oldTimeS);
            System.out.println("targetTimeS = " + targetTimeS);
            System.out.println("expectedWaitDurationS = " + expectedWaitDurationS);
            System.out.println("actualWaitDurationS =   " + actualWaitDurationS);
            fail();
        }
    }
}
