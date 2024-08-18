/*
 * Copyright 2019-2024 Jeff Hain
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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.threading.basics.InterfaceCancellable;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.soft.RootSoftClock;

public class SoftSchedulerTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyRunnable implements InterfaceCancellable {
        private final InterfaceClock clock;
        
        private boolean runCalled;
        private boolean onCancelCalled;
        
        private long runCallTimeNs;
        private int runCallNum;
        
        public MyRunnable(InterfaceClock clock) {
            this.clock = clock;
        }
        @Override
        public void run() {
            final long runCallTimeNs = this.clock.getTimeNs();
            
            if (DEBUG) {
                Dbg.log(this.hashCode() + " : run(), runCallTimeNs = " + runCallTimeNs);
            }
            
            this.runCallTimeNs = runCallTimeNs;
            
            this.runCallNum = ++counter;
            this.runCalled = true;
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
            final UncaughtExceptionHandler exceptionHandler = null;
            final SoftScheduler scheduler = new SoftScheduler(
                    clock,
                    mustExecuteAsapSchedulesSynchronously,
                    exceptionHandler);

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
            final UncaughtExceptionHandler exceptionHandler = null;
            final SoftScheduler scheduler = new SoftScheduler(
                    clock,
                    mustExecuteAsapSchedulesSynchronously,
                    exceptionHandler);

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
        
        final MyRunnable runnable1 = new MyRunnable(clock);
        scheduler.execute(runnable1);

        /*
         * running
         */
        
        scheduler.start();
        
        /*
         * checking
         */
        
        assertTrue(runnable1.runCalled);
        assertFalse(runnable1.onCancelCalled);

        // Runnable called at clock's time.
        assertEquals(10L, runnable1.runCallTimeNs);
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
        
        final MyRunnable runnable1 = new MyRunnable(clock);
        final MyRunnable runnable2 = new MyRunnable(clock);
        final MyRunnable runnable3 = new MyRunnable(clock);
        
        scheduler.executeAtNs(runnable1, 12L);
        scheduler.executeAtNs(runnable2, 11L);
        scheduler.executeAtNs(runnable3, 11L);

        /*
         * running
         */
        
        scheduler.start();
        
        /*
         * checking
         */
        
        assertTrue(runnable1.runCalled);
        assertFalse(runnable1.onCancelCalled);
        
        assertTrue(runnable2.runCalled);
        assertFalse(runnable2.onCancelCalled);

        assertTrue(runnable3.runCalled);
        assertFalse(runnable3.onCancelCalled);

        assertEquals(12L, runnable1.runCallTimeNs);
        assertEquals(11L, runnable2.runCallTimeNs);
        assertEquals(11L, runnable3.runCallTimeNs);
        
        // FIFO order : for time 11, runnable2 called before runnable3,
        // because has been scheduled for that time before.
        assertEquals(runnable2.runCallNum + 1, runnable3.runCallNum);
    }

    public void test_executeAfterNs_Runnable_long() {
        final RootSoftClock clock = new RootSoftClock();
        clock.setTimeNs(10L);
        
        final SoftScheduler scheduler = new SoftScheduler(clock);

        /*
         * Scheduling.
         */
        
        final MyRunnable runnable1 = new MyRunnable(clock);
        scheduler.executeAfterNs(runnable1, 2L);

        final MyRunnable runnable2 = new MyRunnable(clock);
        scheduler.executeAfterNs(runnable2, 1L);

        /*
         * Running.
         */
        
        scheduler.start();
        
        /*
         * Checking.
         */
        
        assertTrue(runnable1.runCalled);
        assertFalse(runnable1.onCancelCalled);
        
        assertTrue(runnable2.runCalled);
        assertFalse(runnable2.onCancelCalled);

        assertEquals(12L, runnable1.runCallTimeNs);
        assertEquals(11L, runnable2.runCallTimeNs);
        
        assertEquals(runnable2.runCallNum + 1, runnable1.runCallNum);
    }
    
    /*
     * 
     */
    
    public void test_schedulingFairness() {
        final RootSoftClock clock = new RootSoftClock();
        
        final SoftScheduler scheduler = new SoftScheduler(clock);

        /*
         * scheduling
         */
        
        final long nowNs = clock.getTimeNs();
        final long futureNs = nowNs + 1L;
        
        final MyRunnable scheduledAsap1 = new MyRunnable(clock);
        final MyRunnable scheduledInFuture = new MyRunnable(clock);
        final MyRunnable scheduledAsap2 = new MyRunnable(clock);
        final MyRunnable scheduledAtNow = new MyRunnable(clock);
        final MyRunnable scheduledAsap3 = new MyRunnable(clock);
        
        scheduler.execute(scheduledAsap1);
        scheduler.executeAtNs(scheduledInFuture, futureNs);
        scheduler.execute(scheduledAsap2);
        scheduler.executeAtNs(scheduledAtNow, nowNs);
        scheduler.execute(scheduledAsap3);

        /*
         * running
         */
        
        scheduler.start();
        
        /*
         * checking
         */
        
        assertTrue(scheduledAsap1.runCalled);
        assertFalse(scheduledAsap1.onCancelCalled);
        
        assertTrue(scheduledAsap2.runCalled);
        assertFalse(scheduledAsap2.onCancelCalled);

        assertTrue(scheduledAtNow.runCalled);
        assertFalse(scheduledAtNow.onCancelCalled);

        assertTrue(scheduledAsap3.runCalled);
        assertFalse(scheduledAsap3.onCancelCalled);

        assertTrue(scheduledInFuture.runCalled);
        assertFalse(scheduledInFuture.onCancelCalled);

        assertEquals(0L, scheduledAsap1.runCallTimeNs);
        assertEquals(0L, scheduledAsap2.runCallTimeNs);
        assertEquals(nowNs, scheduledAtNow.runCallTimeNs);
        assertEquals(0L, scheduledAsap3.runCallTimeNs);
        assertEquals(futureNs, scheduledInFuture.runCallTimeNs);
        
        assertEquals(scheduledAsap1.runCallNum + 1, scheduledAsap2.runCallNum);
        assertEquals(scheduledAsap2.runCallNum + 1, scheduledAtNow.runCallNum);
        assertEquals(scheduledAtNow.runCallNum + 1, scheduledAsap3.runCallNum);
        assertEquals(scheduledAsap3.runCallNum + 1, scheduledInFuture.runCallNum);
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
        final MyRunnable runnable1 = new MyRunnable(clock) {
            @Override
            public void run() {
                super.run();
                
                final MyRunnable runnable2 = new MyRunnable(clock);
                final long pastNs = clock.getTimeNs() - 1;
                scheduler.executeAtNs(runnable2, pastNs);
            }
        };
        scheduler.executeAtNs(runnable1, 10L);
        
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

        final MyRunnable runnable1 = new MyRunnable(clock);
        
        final MyRunnable runnable2 = new MyRunnable(clock);
        
        controllerRef.set(new MyInterfaceTimeAdvanceController() {
            @Override
            public long requestTimeAdvance(long timeNs) {
                // Making sure we don't get used again.
                controllerRef.set(null);
                
                scheduler.executeAtNs(runnable2, newScheduleTimeNs);
                return firstGrantedTimeNs;
            }
        });
        
        scheduler.executeAtNs(runnable1, firstScheduleTimeNs);
        
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
                assertEquals(1, runnable1.runCallNum);
                assertEquals(2, runnable2.runCallNum);
            } else {
                assertEquals(1, runnable2.runCallNum);
                assertEquals(2, runnable1.runCallNum);
            }
            
            assertTrue(runnable1.runCalled);
            assertFalse(runnable1.onCancelCalled);
            assertEquals(firstScheduleTimeNs, runnable1.runCallTimeNs);
            
            assertTrue(runnable2.runCalled);
            assertFalse(runnable2.onCancelCalled);
            assertEquals(newScheduleTimeNs, runnable2.runCallTimeNs);
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
            @Override
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
