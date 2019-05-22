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

public class ThinSystemClockTest extends TestCase {

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
        final ThinSystemClock clock = new ThinSystemClock();
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

        // Testing the time changes while System.currentTimeMillis does not change.
        boolean thin = false;
        for (int i = 0; i < maxNbrOfTries; i++) {
            final long ctm1Ms = System.currentTimeMillis();
            final double time1S = clock.getTimeS();
            final double time2S = clock.getTimeS();
            final long ctm2Ms = System.currentTimeMillis();
            if ((ctm1Ms == ctm2Ms) && (time1S != time2S)) {
                thin = true;
                break;
            }
        }
        assertTrue(thin);
    }

    public void test_getTimeNs() {
        final ThinSystemClock clock = new ThinSystemClock();
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

        // Testing the time changes while System.currentTimeMillis does not change.
        boolean thin = false;
        for (int i = 0; i < maxNbrOfTries; i++) {
            final long ctm1Ms = System.currentTimeMillis();
            final long time1Ns = clock.getTimeNs();
            final long time2Ns = clock.getTimeNs();
            final long ctm2Ms = System.currentTimeMillis();
            if ((ctm1Ms == ctm2Ms) && (time1Ns != time2Ns)) {
                thin = true;
                break;
            }
        }
        assertTrue(thin);
    }
    
    public void test_getTimeSpeed() {
        final ThinSystemClock clock = new ThinSystemClock();
        assertEquals(1.0, clock.getTimeSpeed());
    }
}
