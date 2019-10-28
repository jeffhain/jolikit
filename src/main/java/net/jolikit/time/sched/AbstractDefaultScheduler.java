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
     * Can be used for raw runnables, when wanting to asign a sequnce number
     * to their schedules, to implement fairness with timed schedules,
     * for example to prevent a spam of timed schedules in the past
     * to prevent ASAP schedules from being executed.
     */
    protected static class MySequencedSchedule {
        final Runnable runnable;
        long sequenceNumber;
        public MySequencedSchedule(Runnable runnable) {
            this.runnable = runnable;
        }
        public Runnable getRunnable() {
            return this.runnable;
        }
        public long getSequenceNumber() {
            return this.sequenceNumber;
        }
        public void setSequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }
    }

    protected static class MyTimedSchedule extends MySequencedSchedule {
        private final long theoreticalTimeNs;
        public MyTimedSchedule(
                Runnable runnable,
                long theoreticalTimeNs) {
            super(runnable);
            this.theoreticalTimeNs = theoreticalTimeNs;
        }
        public long getTheoreticalTimeNs() {
            return this.theoreticalTimeNs;
        }
    }
    
    protected static class MyTimedScheduleComparator implements Comparator<MyTimedSchedule> {
        public MyTimedScheduleComparator() {
        }
        @Override
        public int compare(
                final MyTimedSchedule a,
                final MyTimedSchedule b) {
            final long aNs = a.getTheoreticalTimeNs();
            final long bNs = b.getTheoreticalTimeNs();
            final int cmpI;
            if (aNs < bNs) {
                cmpI = -1;
            } else if (aNs > bNs) {
                cmpI = 1;
            } else {
                // Breaking ties with sequence number.
                cmpI = compareSequenceNumbers(
                        a.sequenceNumber,
                        b.sequenceNumber);
            }
            return cmpI;
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected static int compareSequenceNumbers(long sa, long sb) {
        // Being robust to crazy huge sequence numbers
        // (sa < sb is not robust near Long.MAX_VALUE).
        final long cmpL = (sa - sb);
        final int cmpI;
        if (cmpL < 0) {
            cmpI = -1;
        } else if (cmpL > 0) {
            cmpI = 1;
        } else {
            cmpI = 0;
        }
        return cmpI;
    }
}
