/*
 * Copyright 2024 Jeff Hain
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
 * Hard clock always returning zero time,
 * and zero time speed.
 * 
 * Useful for treatments that need a (hard) clock,
 * but in use cases where clock values don't matter.
 */
public class ZeroHardClock implements InterfaceHardClock {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ZeroHardClock() {
    }
    
    @Override
    public long getTimeNs() {
        return 0L;
    }
    
    @Override
    public double getTimeS() {
        return 0.0;
    }
    
    @Override
    public double getTimeSpeed() {
        return 0.0;
    }
}
