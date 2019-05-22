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
package net.jolikit.bwd.impl.utils.sched;

import net.jolikit.time.clocks.hard.ControllableSystemTimeClock;
import net.jolikit.time.clocks.hard.EnslavedControllableHardClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.hard.NanoTimeClock;
import net.jolikit.time.clocks.hard.NanoTimeZeroClock;
import net.jolikit.time.clocks.hard.ThinControllableSystemClock;

public class BindingSchedUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Designed for use when creating a binding.
     * 
     * Providing controllable clocks, so that user can pause clock time,
     * slow it down, accelerate it, etc.
     * 
     * @param hardClockTimeType Must not be null.
     * @return A new controllable clock returning the specified time type by default.
     */
    public static InterfaceHardClock newClock(HardClockTimeType hardClockTimeType) {
        switch (hardClockTimeType) {
        case SYSTEM_NANO_TIME: return new EnslavedControllableHardClock(new NanoTimeClock());
        case NANO_TIME_ZERO: return new EnslavedControllableHardClock(new NanoTimeZeroClock());
        case SYSTEM_CURRENT_TIME_MILLIS: return new ControllableSystemTimeClock();
        case THIN_TIME: return new ThinControllableSystemClock();
        default:
            throw new IllegalArgumentException("" + hardClockTimeType);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingSchedUtils() {
    }
}
