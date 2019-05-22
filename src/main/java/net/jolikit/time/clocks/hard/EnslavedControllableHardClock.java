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

import net.jolikit.threading.locks.InterfaceCheckerLock;
import net.jolikit.time.clocks.EnslavedControllableClock;

/**
 * Controllable hard clock, enslaved to a master hard clock.
 */
public class EnslavedControllableHardClock extends EnslavedControllableClock implements InterfaceHardClock {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public EnslavedControllableHardClock(InterfaceHardClock masterClock) {
        super(masterClock);
    }

    public EnslavedControllableHardClock(
            InterfaceHardClock masterClock,
            InterfaceCheckerLock modificationLock) {
        super(
                masterClock,
                modificationLock);
    }

    @Override
    public InterfaceHardClock getMasterClock() {
        return (InterfaceHardClock) super.getMasterClock();
    }
}
