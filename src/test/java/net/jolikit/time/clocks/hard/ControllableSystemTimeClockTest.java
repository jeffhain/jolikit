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
package net.jolikit.time.clocks.hard;

import net.jolikit.lang.Unchecked;
import net.jolikit.time.TimeUtils;
import junit.framework.TestCase;

public class ControllableSystemTimeClockTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final double REAL_TIME_TOLERANCE_S = 0.2;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final long REAL_TIME_TOLERANCE_MS = (long) (REAL_TIME_TOLERANCE_S * 1e3);

    private static final long ONE_SEC_MS = 1000L;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_getTimeS() {
        final ControllableSystemTimeClock clock = new ControllableSystemTimeClock();        
        clock.setTimeSpeed(0.0);
        // Exact as long in ns.
        final double timeS = 3.0;
        final long timeNs = (long) (timeS * 1e9);
        clock.setTimeNs(timeNs);
        Unchecked.sleepMs(REAL_TIME_TOLERANCE_MS);
        assertEquals(timeS, clock.getTimeS());
    }

    public void test_getTimeNs() {
        final ControllableSystemTimeClock clock = new ControllableSystemTimeClock();        
        clock.setTimeSpeed(0.0);
        clock.setTimeNs(3);
        Unchecked.sleepMs(REAL_TIME_TOLERANCE_MS);
        assertEquals(3,clock.getTimeNs());
    }

    public void test_getTimeSpeed() {
        ControllableSystemTimeClock clock = new ControllableSystemTimeClock();
        clock.setTimeSpeed(3.0);
        assertEquals(3.0,clock.getTimeSpeed());
    }

    public void test_setTimeSpeed_double() {
        final ControllableSystemTimeClock clock = new ControllableSystemTimeClock();

        final double timeSpeed = 3.0;
        clock.setTimeSpeed(timeSpeed);
        assertEquals(timeSpeed, clock.getTimeSpeed());
        
        final double startTimeS = 1.0;
        clock.setTimeNs(TimeUtils.sToNs(startTimeS));
        final double sleepSystemTimeDurationS = 2.0;
        Unchecked.sleepMs((long) (sleepSystemTimeDurationS * ONE_SEC_MS));
        final double expectedTimeS = startTimeS + timeSpeed * sleepSystemTimeDurationS;
        assertEquals(expectedTimeS, clock.getTimeS(), REAL_TIME_TOLERANCE_S);
    }
}
