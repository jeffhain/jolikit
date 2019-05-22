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
package net.jolikit.time.sched.soft;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.time.clocks.soft.RootSoftClock;
import net.jolikit.time.sched.InterfaceSchedulable;
import net.jolikit.time.sched.InterfaceScheduling;

public class SoftSchedulerTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MySchedulable implements InterfaceSchedulable {
        private InterfaceScheduling scheduling;
        
        private boolean mustCallAgainOnce;
        private long nextTheoreticalTimeNs;
        
        private boolean runCalled;
        private boolean onCancelCalled;
        
        private long callTimeNs;
        private int callNum;
        
        @Override
        public void setScheduling(InterfaceScheduling scheduling) {
            this.scheduling = scheduling;
        }
        @Override
        public void run() {
            final InterfaceScheduling scheduling = this.scheduling;
            final long theoreticalTimeNs = scheduling.getTheoreticalTimeNs();
            final long actualTimeNs = scheduling.getActualTimeNs();
            
            if (DEBUG) {
                Dbg.log(this.hashCode() + " : run(), theoretical = " + theoreticalTimeNs + ", actual = " + actualTimeNs);
            }
            
            // Always equal for soft scheduling.
            assertEquals(theoreticalTimeNs, actualTimeNs);
            this.callTimeNs = actualTimeNs;
            
            this.callNum = ++counter;
            this.runCalled = true;
            boolean callAgain = this.mustCallAgainOnce;
            if (callAgain) {
                scheduling.setNextTheoreticalTimeNs(this.nextTheoreticalTimeNs);
                this.mustCallAgainOnce = false;
            }
        }
        @Override
        public void onCancel() {
            this.onCancelCalled = true;
        }
    }
    
    /*
     * 
     */
    
    private interface MyInterfaceTimeAdvanceController {
        /**
         * @param timeNs Requested time, in nanoseconds.
         * @return Granted time, in nanoseconds.
         */
        public long requestTimeAdvance(long timeNs);
    }
    
    private static enum MyNewScheduleType {
        BEFORE_CURRENT_TIME,
        AT_CURRENT_TIME,
        AT_REQUESTED_TIME,
        AFTER_REQUESTED_TIME,
    }
    
    private static enum MyGrantedTimeType {
        BEFORE_CURRENT_TIME,
        BEFORE_EARLIEST_SCHEDULE_TIME,
        AT_EARLIEST_SCHEDULE_TIME,
        AFTER_EARLIEST_SCHEDULE_TIME,
        AFTER_REQUESTED_TIME,
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private int counter = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_executeSyncOrAsync_beforeStart() {
        final RootSoftClock clock = new RootSoftClock();
        clock.setTimeNs(10L);

        for (boolean mustExecuteAsapSchedulesSynchronously : new boolean[]{false,true}) {
            final SoftScheduler scheduler = new SoftScheduler(
                    clock,
                    mustExecuteAsapSchedulesSynchronously);

            final AtomicBoolean executed = new AtomicBoolean();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    executed.set(true);

                    scheduler.stop();
                }
            };

            if (mustExecuteAsapSchedulesSynchronously) {
                try {
                    scheduler.execute(runnable);
                    fail();
                } catch (ConcurrentModificationException e) {
                    // ok
                }
            } else {
                scheduler.execute(runnable);
                assertFalse(executed.get());

                scheduler.start();
                assertTrue(executed.get());
            }
        }
    }

    public void test_executeSyncOrAsync_afterStart() {
        final RootSoftClock clock = new RootSoftClock();
        clock.setTimeNs(10L);

        for (final boolean mustExecuteAsapSchedulesSynchronously : new boolean[]{false,true}) {
            final SoftScheduler scheduler = new SoftScheduler(
                    clock,
                    mustExecuteAsapSchedulesSynchronously);

            final AtomicBoolean executed = new AtomicBoolean();
            
            final Runnable timedSchedule = new Runnable() {
                @Override
                public void run() {
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            executed.set(true);

                            scheduler.stop();
                        }
                    };
                    
                    scheduler.execute(runnable);
                    assertEquals(mustExecuteAsapSchedulesSynchronously, executed.get());
                }
            };
            scheduler.executeAfterNs(timedSchedule, 0L);

            scheduler.start();
            
            assertTrue(executed.get());
        }
    }

    /*
     * 
     */
    
    public void test_execute_Runnable() {
        final RootSoftClock clock = new RootSoftClock();
        clock.setTimeNs(10L);
        
        final SoftScheduler scheduler = new SoftScheduler(clock);

        /*
         * scheduling
         */
        
        final MySchedulable schedulable1 = new MySchedulable();
        scheduler.execute(schedulable1);

        /*
         * running
         */
        
        scheduler.start();
        
        /*
         * checking
         */
        
        assertTrue(schedulable1.runCalled);
        assertFalse(schedulable1.onCancelCalled);

        // Schedulable called at clock's time.
        assertEquals(10L, schedulable1.callTimeNs);
    }

    public void test_executeAtNs_Runnable_long() {
        final RootSoftClock clock = new RootSoftClock();
        // Clock's time will be set with earliest schedule time,
        // on call to start method: this weird time must not cause trouble.
        clock.setTimeNs(Long.MAX_VALUE);
        
        final SoftScheduler scheduler = new SoftScheduler(clock);

        /*
         * scheduling
         */
        
        final MySchedulable schedulable1 = new MySchedulable();
        schedulable1.mustCallAgainOnce = true;
        schedulable1.nextTheoreticalTimeNs = 20L;
        scheduler.executeAtNs(schedulable1, 12L);

        final MySchedulable schedulable2 = new MySchedulable();
        schedulable2.mustCallAgainOnce = true;
        schedulable2.nextTheoreticalTimeNs = 20L;
        scheduler.executeAtNs(schedulable2, 11L);

        /*
         * running
         */
        
        scheduler.start();
        
        /*
         * checking
         */
        
        assertTrue(schedulable1.runCalled);
        assertFalse(schedulable1.onCancelCalled);
        
        assertTrue(schedulable2.runCalled);
        assertFalse(schedulable2.onCancelCalled);

        assertEquals(20L, schedulable1.callTimeNs);
        assertEquals(20L, schedulable2.callTimeNs);
        
        // FIFO order : for time 20, schedulable2 called before schedulable1,
        // because has been scheduled for that time before.
        assertTrue(schedulable2.callNum + 1 == schedulable1.callNum);
    }

    public void test_executeAfterNs_Runnable_long() {
        final RootSoftClock clock = new RootSoftClock();
        clock.setTimeNs(10L);
        
        final SoftScheduler scheduler = new SoftScheduler(clock);

        /*
         * Scheduling.
         */
        
        final MySchedulable schedulable1 = new MySchedulable();
        scheduler.executeAfterNs(schedulable1, 2L);

        final MySchedulable schedulable2 = new MySchedulable();
        scheduler.executeAfterNs(schedulable2, 1L);

        /*
         * Running.
         */
        
        scheduler.start();
        
        /*
         * Checking.
         */
        
        assertTrue(schedulable1.runCalled);
        assertFalse(schedulable1.onCancelCalled);
        
        assertTrue(schedulable2.runCalled);
        assertFalse(schedulable2.onCancelCalled);

        assertEquals(12L, schedulable1.callTimeNs);
        assertEquals(11L, schedulable2.callTimeNs);
        
        assertTrue(schedulable2.callNum + 1 == schedulable1.callNum);
    }
    
    /*
     * 
     */

    public void test_scheduleInThePast() {
        final RootSoftClock clock = new RootSoftClock();
        
        final SoftScheduler scheduler = new SoftScheduler(clock);

        /*
         * scheduling
         */

        // first schedule sets clock's time
        final MySchedulable schedulable1 = new MySchedulable();
        // second schedule in the past
        schedulable1.mustCallAgainOnce = true;
        schedulable1.nextTheoreticalTimeNs = 10L;
        scheduler.executeAtNs(schedulable1, 20L);
        
        /*
         * running
         */

        try {
            scheduler.start();
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
    
    /*
     * 
     */

    public void test_timeAdvanceRequest_xxx() {
        for (MyNewScheduleType newScheduleType : MyNewScheduleType.values()) {
            for (MyGrantedTimeType grantedTimeType : MyGrantedTimeType.values()) {
                final long firstScheduleTimeNs = 20L;
                
                long newScheduleTimeNs;
                switch (newScheduleType) {
                case BEFORE_CURRENT_TIME: newScheduleTimeNs = -1L; break;
                case AT_CURRENT_TIME: newScheduleTimeNs = 0L; break;
                case AT_REQUESTED_TIME: newScheduleTimeNs = 20L; break;
                case AFTER_REQUESTED_TIME: newScheduleTimeNs = 21L; break;
                default:
                    throw new AssertionError("" + newScheduleType);
                }
                
                final long earliestScheduleTimeNs = Math.min(firstScheduleTimeNs, newScheduleTimeNs);
                
                long firstGrantedTimeNs;
                switch (grantedTimeType) {
                case BEFORE_CURRENT_TIME: firstGrantedTimeNs = -1L; break;
                case BEFORE_EARLIEST_SCHEDULE_TIME: firstGrantedTimeNs = earliestScheduleTimeNs - 1L; break;
                case AT_EARLIEST_SCHEDULE_TIME: firstGrantedTimeNs = earliestScheduleTimeNs; break;
                case AFTER_EARLIEST_SCHEDULE_TIME: firstGrantedTimeNs = earliestScheduleTimeNs + 1L; break;
                case AFTER_REQUESTED_TIME: firstGrantedTimeNs = firstScheduleTimeNs + 1L; break;
                default:
                    throw new AssertionError("" + grantedTimeType);
                }
                
                final boolean exceptionExpected =
                        (newScheduleType == MyNewScheduleType.BEFORE_CURRENT_TIME)
                        //
                        || (grantedTimeType == MyGrantedTimeType.BEFORE_CURRENT_TIME)
                        || (grantedTimeType == MyGrantedTimeType.BEFORE_EARLIEST_SCHEDULE_TIME)
                        || (grantedTimeType == MyGrantedTimeType.AFTER_EARLIEST_SCHEDULE_TIME)
                        || (grantedTimeType == MyGrantedTimeType.AFTER_REQUESTED_TIME);
                
                if (DEBUG) {
                    Dbg.log();
                    Dbg.log("newScheduleType = " + newScheduleType);
                    Dbg.log("grantedTimeType = " + grantedTimeType);
                    Dbg.log("firstScheduleTimeNs = " + firstScheduleTimeNs);
                    Dbg.log("newScheduleTimeNs = " + newScheduleTimeNs);
                    Dbg.log("firstGrantedTimeNs = " + firstGrantedTimeNs);
                    Dbg.log("exceptionExpected = " + exceptionExpected);
                }
                
                this.test_timeAdvanceRequest(
                        firstScheduleTimeNs,
                        newScheduleTimeNs,
                        firstGrantedTimeNs,
                        exceptionExpected);
            }
        }
    }

    /**
     * @param firstScheduleTimeNs Time for first schedule done.
     * @param newScheduleTimeNs Time for the schedule added in first time advance request.
     * @param firstGrantedTimeNs Granted time for first time advance request.
     */
    public void test_timeAdvanceRequest(
            long firstScheduleTimeNs,
            final long newScheduleTimeNs,
            final long firstGrantedTimeNs,
            boolean exceptionExpected) {
        this.reset();
        
        // Just using atomic to have a ref.
        final AtomicReference<MyInterfaceTimeAdvanceController> controllerRef =
                new AtomicReference<MyInterfaceTimeAdvanceController>();
        
        final RootSoftClock clock = new RootSoftClock() {
            @Override
            public void setTimeNs(long timeNs) {
                final MyInterfaceTimeAdvanceController controller = controllerRef.get();
                if (controller != null) {
                    timeNs = controller.requestTimeAdvance(timeNs);
                }
                super.setTimeNs(timeNs);
            }
        };
        
        final SoftScheduler scheduler = new SoftScheduler(clock);

        /*
         * scheduling
         */

        final MySchedulable schedulable1 = new MySchedulable();
        
        final MySchedulable schedulable2 = new MySchedulable();
        
        controllerRef.set(new MyInterfaceTimeAdvanceController() {
            @Override
            public long requestTimeAdvance(long timeNs) {
                // Making sure we don't get used again.
                controllerRef.set(null);
                
                scheduler.executeAtNs(schedulable2, newScheduleTimeNs);
                return firstGrantedTimeNs;
            }
        });
        
        scheduler.executeAtNs(schedulable1, firstScheduleTimeNs);
        
        /*
         * running
         */

        if (exceptionExpected) {
            try {
                scheduler.start();
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        } else {
            scheduler.start();
            
            if (newScheduleTimeNs >= firstScheduleTimeNs) {
                assertEquals(1, schedulable1.callNum);
                assertEquals(2, schedulable2.callNum);
            } else {
                assertEquals(1, schedulable2.callNum);
                assertEquals(2, schedulable1.callNum);
            }
            
            assertTrue(schedulable1.runCalled);
            assertFalse(schedulable1.onCancelCalled);
            assertEquals(firstScheduleTimeNs, schedulable1.callTimeNs);
            
            assertTrue(schedulable2.runCalled);
            assertFalse(schedulable2.onCancelCalled);
            assertEquals(newScheduleTimeNs, schedulable2.callTimeNs);
        }
    }

    /*
     * 
     */
    
    public void test_isWorkerThread_andChecks() {
        final RootSoftClock clock = new RootSoftClock();
        clock.setTimeNs(10L);
        
        final SoftScheduler scheduler = new SoftScheduler(clock);
        
        assertFalse(scheduler.isWorkerThread());
        try {
            scheduler.checkIsWorkerThread();
            fail();
        } catch (ConcurrentModificationException e) {
            // ok
        }
        scheduler.checkIsNotWorkerThread();
        
        /*
         * 
         */

        final Runnable checkingRunnable = new Runnable() {
            @Override
            public void run() {
                assertTrue(scheduler.isWorkerThread());
                
                scheduler.checkIsWorkerThread();
                try {
                    scheduler.checkIsNotWorkerThread();
                    fail();
                } catch (IllegalStateException e) {
                    // ok
                }
            }
        };
        for (boolean asapElseTimed : new boolean[]{false,true}) {
            if (asapElseTimed) {
                scheduler.execute(checkingRunnable);
            } else {
                scheduler.executeAtNs(checkingRunnable, clock.getTimeNs());
            }
        }
        
        scheduler.getAlienExecutor().execute(checkingRunnable);

        scheduler.executeAfterNs(new Runnable() {
            public void run() {
                scheduler.stop();
            }
        }, 1L);

        scheduler.start();

        /*
         * 
         */
        
        assertFalse(scheduler.isWorkerThread());
        try {
            scheduler.checkIsWorkerThread();
            fail();
        } catch (ConcurrentModificationException e) {
            // ok
        }
        scheduler.checkIsNotWorkerThread();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void reset() {
        this.counter = 0;
    }
}
