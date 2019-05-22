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
import net.jolikit.threading.locks.InterfaceCheckerLock;

/**
 * Controllable hard clock, enslaved to thin system time clock.
 */
public class ThinControllableSystemClock extends EnslavedControllableHardClock {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param thinTime ThinTime instance to use.
     */
    public ThinControllableSystemClock(ThinTime thinTime) {
        super(new ThinSystemClock(thinTime));
    }

    /**
     * @param thinTime ThinTime instance to use.
     */
    public ThinControllableSystemClock(
            final ThinTime thinTime,
            final InterfaceCheckerLock modificationLock) {
        super(
                new ThinSystemClock(thinTime),
                modificationLock);
    }

    /**
     * Uses a default ThinTime instance.
     */
    public ThinControllableSystemClock() {
        this(ThinTime.getDefaultInstance());
    }
}
