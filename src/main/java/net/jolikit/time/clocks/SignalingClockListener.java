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
package net.jolikit.time.clocks;

import net.jolikit.threading.locks.InterfaceCondilock;

/**
 * Class to signal a condition being waited on for a hard clock to reach a time,
 * when any modification occurs to this clock or its eventual master clocks
 * (since a master's time or absolute time speed modification, typically modifies
 * the duration that needs to be waited over).
 * 
 * Of course, the signal is effective only for treatments that retrieve clock's time
 * from within condition's lock. If clock's time or time speed are retrieved outside
 * this lock, and then the lock acquired for a wait, the clock might have been modified
 * before the lock was acquired, and retrieved time or time speed be obsolete.
 */
public class SignalingClockListener implements InterfaceClockModificationListener {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceCondilock acquirableCondition;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param acquirableCondition Acquirable condition to call signalAllInLock()
     *        when a clock listened at has been modified.
     */
    public SignalingClockListener(InterfaceCondilock acquirableCondition) {
        this.acquirableCondition = acquirableCondition;
    }

    @Override
    public void onClockModification(InterfaceClock clock) {
        this.acquirableCondition.signalAllInLock();
    }
}
