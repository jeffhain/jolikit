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

import net.jolikit.threading.locks.InterfaceCheckerLock;
import net.jolikit.time.clocks.EnslavedControllableClock;

/**
 * Controllable soft clock, enslaved to a master soft clock.
 */
public class EnslavedControllableSoftClock extends EnslavedControllableClock implements InterfaceSoftClock {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public EnslavedControllableSoftClock(InterfaceSoftClock masterClock) {
        super(masterClock);
    }
    
    public EnslavedControllableSoftClock(
            InterfaceSoftClock masterClock,
            InterfaceCheckerLock modificationLock) {
        super(
                masterClock,
                modificationLock);
    }

    @Override
    public InterfaceSoftClock getMasterClock() {
        return (InterfaceSoftClock) super.getMasterClock();
    }
}
