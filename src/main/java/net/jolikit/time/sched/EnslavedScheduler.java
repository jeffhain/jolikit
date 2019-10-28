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
package net.jolikit.time.sched;

import net.jolikit.time.clocks.ClocksUtils;
import net.jolikit.time.clocks.InterfaceEnslavedClock;

/**
 * If enslaved clock might be modified, this scheduler
 * should not be used, as used root scheduler won't be
 * notified of enslaved clock modification.
 * 
 * For enslaved soft clock, this class is the only way to go,
 * as only the time of the root clock is modified (enslaved
 * clock's times changing accordingly).
 * 
 * For enslaved hard clocks, it is also possible to use a
 * hard scheduler directly based on the enslaved clock, and
 * this is mandatory if the enslaved clock might be modified,
 * but using enslaved schedulers for enslaved clocks, allows
 * for transparent use of hard or soft scheduling.
 */
public class EnslavedScheduler extends AbstractScheduler {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final InterfaceEnslavedClock enslavedClock;
    
    private final InterfaceScheduler rootScheduler;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param enslavedClock An enslaved clock, which time is used
     *        for this scheduler's schedules.
     * @param rootScheduler A scheduler based on root clock,
     *        i.e. enslaved clock's last master clock.
     */
    public EnslavedScheduler(
            final InterfaceEnslavedClock enslavedClock,
            final InterfaceScheduler rootScheduler) {
        this.enslavedClock = enslavedClock;
        this.rootScheduler = rootScheduler;
    }

    @Override
    public InterfaceEnslavedClock getClock() {
        return this.enslavedClock;
    }

    @Override
    public void execute(Runnable runnable) {
        this.rootScheduler.execute(runnable);
    }

    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        final long rootTimeNs = ClocksUtils.computeRootTimeNs(
                this.enslavedClock,
                timeNs);
        this.rootScheduler.executeAtNs(runnable, rootTimeNs);
    }
}
