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

import java.util.Comparator;

/**
 * Public for use from within hard and soft sub-packages.
 * Should not be used outside this-or-sub-packages.
 */
public abstract class AbstractDefaultScheduler extends AbstractScheduler {
    
    //--------------------------------------------------------------------------
    // PROTECTED CLASSES
    //--------------------------------------------------------------------------

    /**
     * Timed schedule for raw runnables.
     */
    protected static class MyRunnableSchedule {
        private final Runnable runnable;
        /**
         * Needed even for raw runnables,
         * for comparison in time sorted collections.
         */
        private long theoreticalTimeNs;
        private long sequenceNumber;
        public MyRunnableSchedule(Runnable runnable) {
            this.runnable = runnable;
        }
        public Runnable getRunnable() {
            return this.runnable;
        }
        public final long getTheoreticalTimeNs_monomorphic() {
            return this.theoreticalTimeNs;
        }
        public void setTheoreticalTimeNs(long theoreticalTimeNs) {
            this.theoreticalTimeNs = theoreticalTimeNs;
        }
        public long getSequenceNumber() {
            return this.sequenceNumber;
        }
        public void setSequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }
    }

    /**
     * Timed schedule for schedulables, which is also the scheduling to use.
     */
    protected static class MySchedulableSchedule extends MyRunnableSchedule implements InterfaceScheduling {
        private long actualTimeNs;
        private boolean isNextTheoreticalTimeSet = false;
        private long nextTheoreticalTimeNs;
        public MySchedulableSchedule(InterfaceSchedulable schedulable) {
            super(schedulable);
        }
        /**
         * Theoretical time must be set aside.
         * 
         * Clears next theoretical time information.
         */
        public void configureBeforeRun(long actualTimeNs) {
            this.actualTimeNs = actualTimeNs;
            
            this.isNextTheoreticalTimeSet = false;
            this.nextTheoreticalTimeNs = Long.MAX_VALUE;
        }
        /*
         * 
         */
        public final boolean isNextTheoreticalTimeSet_monomorphic() {
            return this.isNextTheoreticalTimeSet;
        }
        public final long getNextTheoreticalTimeNs_monomorphic() {
            return this.nextTheoreticalTimeNs;
        }
        /*
         * 
         */
        @Override
        public long getTheoreticalTimeNs() {
            return this.getTheoreticalTimeNs_monomorphic();
        }
        @Override
        public long getActualTimeNs() {
            return this.actualTimeNs;
        }
        @Override
        public void setNextTheoreticalTimeNs(long nextTheoreticalTimeNs) {
            this.nextTheoreticalTimeNs = nextTheoreticalTimeNs;
            this.isNextTheoreticalTimeSet = true;
        }
        @Override
        public boolean isNextTheoreticalTimeSet() {
            return this.isNextTheoreticalTimeSet_monomorphic();
        }
        @Override
        public void clearNextTheoreticalTime() {
            this.isNextTheoreticalTimeSet = false;
        }
        @Override
        public long getNextTheoreticalTimeNs() {
            return this.getNextTheoreticalTimeNs_monomorphic();
        }
    }

    protected static class MyScheduleComparator implements Comparator<MyRunnableSchedule> {
        public MyScheduleComparator() {
        }
        @Override
        public int compare(
                final MyRunnableSchedule a,
                final MyRunnableSchedule b) {
            if (a.theoreticalTimeNs < b.theoreticalTimeNs) {
                return -1;
            } else if (a.theoreticalTimeNs > b.theoreticalTimeNs) {
                return 1;
            } else {
                // Breaking ties with sequence number.
                // Being robust to crazy huge sequence numbers
                // (a.x < b.x is not robust near Long.MAX_VALUE).
                if (a.sequenceNumber - b.sequenceNumber < 0) {
                    return -1;
                } else {
                    if (a == b) {
                        // To be consistent with equals, in case it could help.
                        return 0;
                    }
                    return 1;
                }
            }
        }
    }
}
