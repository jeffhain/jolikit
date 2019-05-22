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
package net.jolikit.time.clocks.hard;

import junit.framework.TestCase;
import net.jolikit.time.TimeUtils;

public class SystemTimeClockTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final double MAX_DELTA_S = 0.1;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_getInstance() {
        // Already covered.
    }

    public void test_getTimeS() {
        final SystemTimeClock clock = new SystemTimeClock();
        final int maxNbrOfTries = 1000;
        
        boolean closeTimesFound = false;
        for (int i = 0; i < maxNbrOfTries; i++) {
            final double ctmS = TimeUtils.millisToS(System.currentTimeMillis());
            final double timeS = clock.getTimeS();
            if (Math.abs(timeS - ctmS) <= MAX_DELTA_S) {
                closeTimesFound = true;
                break;
            }
        }
        
        assertTrue(closeTimesFound);
    }

    public void test_getTimeNs() {
        final SystemTimeClock clock = new SystemTimeClock();
        final int maxNbrOfTries = 1000;
        
        boolean closeTimesFound = false;
        for (int i = 0; i < maxNbrOfTries; i++) {
            final double ctmS = TimeUtils.millisToS(System.currentTimeMillis());
            final double timeS = TimeUtils.nsToS(clock.getTimeNs());
            if (Math.abs(timeS - ctmS) <= MAX_DELTA_S) {
                closeTimesFound = true;
                break;
            }
        }
        
        assertTrue(closeTimesFound);
    }
    
    public void test_getTimeSpeed() {
        final SystemTimeClock clock = new SystemTimeClock();
        assertEquals(1.0, clock.getTimeSpeed());
    }
}
