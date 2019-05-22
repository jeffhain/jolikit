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

import net.jolikit.lang.ThinTime;

/**
 * System clock based on ThinTime.
 */
public class ThinSystemClock implements InterfaceHardClock {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final ThinSystemClock DEFAULT_INSTANCE = new ThinSystemClock(
            ThinTime.getDefaultInstance());
    
    /*
     * 
     */

    private final ThinTime thinTime;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param thinTime ThinTime instance to use.
     */
    public ThinSystemClock(ThinTime thinTime) {
        this.thinTime = thinTime;
    }
    
    /**
     * Uses a default ThinTime instance.
     */
    public ThinSystemClock() {
        this(ThinTime.getDefaultInstance());
    }
    
    /**
     * @return A default instance.
     */
    public static ThinSystemClock getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public double getTimeS() {
        return this.thinTime.currentTimeSeconds_();
    }

    @Override
    public long getTimeNs() {
        return this.thinTime.currentTimeNanos_();
    }
    
    @Override
    public double getTimeSpeed() {
        return 1.0;
    }
}
