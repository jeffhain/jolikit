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

/**
 * Hard clock based on System.currentTimeMillis().
 * 
 * Better to call it a system clock than a real time clock,
 * since system clock can jump around of and away from real time.
 */
public class SystemTimeClock implements InterfaceHardClock {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SystemTimeClock DEFAULT_INSTANCE = new SystemTimeClock();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SystemTimeClock() {
    }
    
    /**
     * @return A default instance.
     */
    public static SystemTimeClock getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public double getTimeS() {
        return System.currentTimeMillis() * 1e-3;
    }

    @Override
    public long getTimeNs() {
        return System.currentTimeMillis() * (1000L * 1000L);
    }
    
    @Override
    public double getTimeSpeed() {
        return 1.0;
    }
}
