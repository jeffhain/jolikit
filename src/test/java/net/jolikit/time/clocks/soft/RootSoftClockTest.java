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
package net.jolikit.time.clocks.soft;

import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.hard.ControllableSystemTimeClock;
import junit.framework.TestCase;

public class RootSoftClockTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final double REAL_TIME_TOLERANCE_S = 0.1;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final long REAL_TIME_TOLERANCE_NS = TimeUtils.sToNs(REAL_TIME_TOLERANCE_S);
    
    private static final long ONE_SECOND_NS = TimeUtils.sToNs(1.0);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_RootSoftClock_nonAFAP() {
        final ControllableSystemTimeClock hardClock = new ControllableSystemTimeClock();
        hardClock.setTimeNsAndTimeSpeed(0L, 1.0);
        
        final RootSoftClock clock = new RootSoftClock(hardClock);
        
        // Must not wait (current time).
        long aNs = nowNs();
        clock.setTimeNs(hardClock.getTimeNs());
        long bNs = nowNs();
        assertTrue(Math.abs(bNs-aNs) < REAL_TIME_TOLERANCE_NS);
        
        // Must wait (future time).
        long timeJumpNs = ONE_SECOND_NS;
        aNs = nowNs();
        clock.setTimeNs(hardClock.getTimeNs() + timeJumpNs);
        bNs = nowNs();
        assertTrue(Math.abs(bNs-aNs - timeJumpNs) < REAL_TIME_TOLERANCE_NS);
        
        // Must not wait (past time).
        timeJumpNs = -ONE_SECOND_NS;
        aNs = nowNs();
        clock.setTimeNs(hardClock.getTimeNs() + timeJumpNs);
        bNs = nowNs();
        assertTrue(Math.abs(bNs-aNs) < REAL_TIME_TOLERANCE_NS);
    }

    public void test_RootSoftClock_nonAFAP_latenessThreshold() {
        final ControllableSystemTimeClock hardClock = new ControllableSystemTimeClock();
        hardClock.setTimeNsAndTimeSpeed(0L, 1.0);
        
        final RootSoftClock clock = new RootSoftClock(hardClock);
        
        /*
         * Clock late, but lateness inferior to threshold:
         * soft clock will catch up hard clock.
         */
        
        // Modifying hard clock's time to make root clock late.
        hardClock.setTimeNs(clock.getTimeNs() + 1000 * ONE_SECOND_NS);
        
        // Default value (always tries to catch up).
        assertEquals(Long.MAX_VALUE, clock.getLatenessThresholdNS());

        // Clock late, but catches up (just waits 1 second, after catching up 1000 seconds).
        
        // Does not forgive lateness (1000 s < +Infinity).
        clock.setTimeNs(clock.getTimeNs());
        
        // Not waiting (catches up the lateness).
        long timeJumpNs = ONE_SECOND_NS;
        long aNs = nowNs();
        clock.setTimeNs(clock.getTimeNs() + timeJumpNs);
        long bNs = nowNs();
        assertTrue(Math.abs(bNs-aNs) < REAL_TIME_TOLERANCE_NS);

        /*
         * Clock late, but lateness above threshold:
         * lateness forgiven.
         */
        
        // lateness threshold (forgives lateness above that)
        clock.setLatenessThresholdNS(TimeUtils.sToNs(100.0));
        
        // Modifying hard clock's time to make root clock late (more than threshold).
        hardClock.setTimeNs(clock.getTimeNs() + 1000 * ONE_SECOND_NS);

        // forgives lateness (1000 > 100)
        clock.setTimeNs(clock.getTimeNs());
        
        // Waits (lateness forgiven).
        timeJumpNs = ONE_SECOND_NS;
        aNs = nowNs();
        clock.setTimeNs(clock.getTimeNs() + timeJumpNs);
        bNs = nowNs();
        assertTrue(Math.abs(bNs-aNs - timeJumpNs) < REAL_TIME_TOLERANCE_NS);
    }

    public void test_RootSoftClock_AFAP() {
        final RootSoftClock clock = new RootSoftClock();
        
        // Must not wait.
        long aNs = nowNs();
        clock.setTimeNs(10 * ONE_SECOND_NS);
        long bNs = nowNs();
        assertTrue(bNs-aNs < REAL_TIME_TOLERANCE_NS);
        
        // Must not wait.
        aNs = nowNs();
        clock.setTimeNs(100 * ONE_SECOND_NS);
        bNs = nowNs();
        assertTrue(bNs-aNs < REAL_TIME_TOLERANCE_NS);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static long nowNs() {
        return System.nanoTime();
    }
}
