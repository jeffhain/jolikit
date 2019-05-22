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
package net.jolikit.bwd.test.utils;

import java.util.LinkedList;

import net.jolikit.time.clocks.hard.NanoTimeClock;

/**
 * Helper class to estimate events hertz (occurrences per second),
 * for example paints per second (PPS).
 */
public class HertzHelper {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Not too few, to smooth things.
     * Not too many, to be reactive.
     */
    private static final int MAX_NBR_OF_EVENTS_TO_CONSIDER = 10;
    
    private static final NanoTimeClock CLOCK = new NanoTimeClock();
    
    private final LinkedList<Double> lastEventTimeSList = new LinkedList<Double>();
    
    private double frequencyHz = 0.0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public HertzHelper() {
    }
    
    public void onEvent() {
        final double nowS = CLOCK.getTimeS();

        this.lastEventTimeSList.add(nowS);
        int size = this.lastEventTimeSList.size();
        
        if (size == 1) {
            // Can't compute a frequency with only one event.
            return;
        }
        
        if (size > MAX_NBR_OF_EVENTS_TO_CONSIDER) {
            this.lastEventTimeSList.removeFirst();
            size--;
        }
        
        final double durationS = this.lastEventTimeSList.getLast() - this.lastEventTimeSList.getFirst();
        final double meanDelayS = durationS / (size - 1);
        
        this.frequencyHz = size / (durationS + meanDelayS);
    }
    
    /**
     * @return An estimation of the frequency at which onEvent() is called,
     *         in hertz.
     */
    public double getFrequencyHz() {
        return this.frequencyHz;
    }
    
    /**
     * @return An estimation of the frequency at which onEvent() is called,
     *         in hertz, rounded to the closest int.
     */
    public int getFrequencyHzRounded() {
        return (int) Math.rint(this.frequencyHz);
    }
}
