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
package net.jolikit.time.clocks.soft;

import net.jolikit.time.clocks.InterfaceControllableClock;

/**
 * Interface to type clocks as soft clocks.
 * 
 * A soft clock is a clock which time changes only when modified,
 * whatever its time speed.
 */
public interface InterfaceSoftClock extends InterfaceControllableClock {

    // overriding for javadoc
    
    /**
     * Value typically in [0,+Infinity], possibly in [-Infinity,+Infinity].
     * This time speed is a desired (and not necessarily actual) time speed.
     */
    @Override
    public double getTimeSpeed();
}
