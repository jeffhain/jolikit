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
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Watch out: is a scheduling (in enslaved clock time frame),
     * and also contains a scheduling (in root clock time frame).
     */
    private class MyEnslavedSchedulable extends DefaultScheduling implements InterfaceSchedulable {
        private final InterfaceSchedulable schedulable;
        private InterfaceScheduling rootScheduling;
        /**
         * For ASAP schedules.
         */
        public MyEnslavedSchedulable(InterfaceSchedulable schedulable) {
            this.schedulable = schedulable;
        }
        /**
         * For timed schedules.
         */
        public MyEnslavedSchedulable(
                InterfaceSchedulable schedulable,
                long theoreticalTimeNs) {
            this.schedulable = schedulable;
            this.setTheoreticalTimeNs(theoreticalTimeNs);
        }
        @Override
        public void setScheduling(InterfaceScheduling rootScheduling) {
            this.rootScheduling = rootScheduling;
        }
        @Override
        public void run() {
            final long enslavedActualTimeNs = enslavedClock.getTimeNs();
            this.updateBeforeRun(enslavedActualTimeNs);
            
            this.schedulable.setScheduling(this);
            this.schedulable.run();
            
            final boolean mustRepeat = this.isNextTheoreticalTimeSet();
            if (mustRepeat) {
                final long enslavedNextNs = this.getNextTheoreticalTimeNs();
                this.setTheoreticalTimeNs(enslavedNextNs);
                
                final long rootNextNs = ClocksUtils.computeRootTimeNs(
                        enslavedClock,
                        enslavedNextNs);
                this.rootScheduling.setNextTheoreticalTimeNs(rootNextNs);
            }
        }
        @Override
        public void onCancel() {
            this.schedulable.onCancel();
        }
    }

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
        if (runnable instanceof InterfaceSchedulable) {
            final MyEnslavedSchedulable schedule =
                    new MyEnslavedSchedulable(
                            (InterfaceSchedulable) runnable);
            this.rootScheduler.execute(schedule);
        } else {
            this.rootScheduler.execute(runnable);
        }
    }

    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        final long rootTimeNs = ClocksUtils.computeRootTimeNs(
                enslavedClock,
                timeNs);
        if (runnable instanceof InterfaceSchedulable) {
            final MyEnslavedSchedulable schedule =
                    new MyEnslavedSchedulable(
                            (InterfaceSchedulable) runnable,
                            timeNs);
            this.rootScheduler.executeAtNs(schedule, rootTimeNs);
        } else {
            this.rootScheduler.executeAtNs(runnable, rootTimeNs);
        }
    }
}
