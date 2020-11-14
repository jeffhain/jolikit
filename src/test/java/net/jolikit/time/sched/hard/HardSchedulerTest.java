/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.time.sched.hard;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;
import net.jolikit.lang.DefaultThreadFactory;
import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.locks.MonitorCondilock;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.InterfaceClockModificationListener;
import net.jolikit.time.clocks.hard.EnslavedControllableHardClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.hard.NanoTimeClock;
import net.jolikit.time.clocks.hard.SystemTimeClock;
import net.jolikit.time.sched.InterfaceCancellable;
import net.jolikit.time.sched.InterfaceScheduler;

public class HardSchedulerTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final long REAL_TIME_TOLERANCE_MS = 200L;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyThrowableType {
        /**
         * When these occur, our worker threads run again.
         */
        EXCEPTION,
        /**
         * When these occur, our worker threads terminate.
         */
        ERROR
    }

    private static class MyRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MyRuntimeException() {
            super("for test");
        }
    }
    
    private static class MyError extends Error {
        private static final long serialVersionUID = 1L;
        public MyError() {
            super("for test");
        }
    }
    
    private static class MyExceptionHandler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            if (throwable instanceof Exception) {
                // We keep going.
            } else {
                Unchecked.throwIt(throwable);
            }
        }
    }
    
    private static class MyThreadFactory extends DefaultThreadFactory {
        public MyThreadFactory() {
            super(null, new MyExceptionHandler());
        }
    };

    private static class MySchedulingReport {
        private final long runCallTimeNs;
        /**
         * 1 for first runnable, etc.
         * Only considers last run call.
         */
        private final long orderNum;
        public MySchedulingReport(
                long runCallTimeNs,
                long orderNum) {
            this.runCallTimeNs = runCallTimeNs;
            this.orderNum = orderNum;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[runCallTimeNs=");
            sb.append(runCallTimeNs);
            sb.append(",orderNum=");
            sb.append(orderNum);
            sb.append("]");
            return sb.toString();
        }
    }

    private class MyRunnable implements InterfaceCancellable {
        private final Object reportMutex = new Object();
        private final InterfaceClock clock;
        /**
         * Report for last run call.
         */
        private volatile MySchedulingReport report;
        private volatile int nbrOfRunCalls;
        private volatile int nbrOfOnCancelCalls;
        private final long sleepTimeMsInRun;
        private final long sleepTimeMsInOnCancel;
        private final MyThrowableType throwableInRun;
        private final MyThrowableType throwableInOnCancel;
        private volatile boolean runInterrupted;
        private volatile boolean onCancelInterrupted;
        public MyRunnable(InterfaceClock clock) {
            this(clock, 0);
        }
        public MyRunnable(
                InterfaceClock clock,
                long sleepTimeMsInRun) {
            this(
                    clock,
                    sleepTimeMsInRun,
                    0);
        }
        public MyRunnable(
                InterfaceClock clock,
                long sleepTimeMsInRun,
                long sleepTimeMsInOnCancel) {
            this(
                    clock,
                    sleepTimeMsInRun,
                    sleepTimeMsInOnCancel,
                    null,
                    null);
        }
        public MyRunnable(
                InterfaceClock clock,
                long sleepTimeMsInRun,
                long sleepTimeMsInOnCancel,
                final MyThrowableType throwableInRun,
                final MyThrowableType throwableInOnCancel) {
            this.clock = clock;
            this.sleepTimeMsInRun = sleepTimeMsInRun;
            this.sleepTimeMsInOnCancel = sleepTimeMsInOnCancel;
            this.throwableInRun = throwableInRun;
            this.throwableInOnCancel = throwableInOnCancel;
        }
        @Override
        public void run() {
            final long runCallTimeNs = this.clock.getTimeNs();

            this.nbrOfRunCalls++;

            try {
                if (this.sleepTimeMsInRun > 0) {
                    try {
                        Thread.sleep(this.sleepTimeMsInRun);
                    } catch (InterruptedException e) {
                        this.runInterrupted = true;
                    }
                }

                throwIfNeeded(this.throwableInRun);
            } finally {
                final long orderNum = counter.incrementAndGet();

                this.report = new MySchedulingReport(
                        runCallTimeNs,
                        orderNum);
                synchronized (reportMutex) {
                    reportMutex.notify();
                }
            }
        }
        @Override
        public void onCancel() {
            this.nbrOfOnCancelCalls++;

            try {
                if (this.sleepTimeMsInOnCancel > 0) {
                    try {
                        Thread.sleep(this.sleepTimeMsInOnCancel);
                    } catch (InterruptedException e) {
                        this.onCancelInterrupted = true;
                    }
                }

                throwIfNeeded(this.throwableInOnCancel);
            } finally {
                this.report = new MySchedulingReport(
                        0L,
                        0L);
                synchronized (reportMutex) {
                    reportMutex.notify();
                }
            }
        }
        public boolean runCalled() {
            return this.nbrOfRunCalls != 0;
        }
        public boolean onCancelCalled() {
            return this.nbrOfOnCancelCalls != 0;
        }
        /**
         * Blocking.
         * @return Valid report if last run didn't ask for new schedule or threw an exception,
         *         or an all-zero report if onCancel was called while first run asked for new schedule.
         */
        public MySchedulingReport waitAndGetReport() {
            if (this.report == null) {
                synchronized (this.reportMutex) {
                    while (this.report == null) {
                        try {
                            this.reportMutex.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return this.report;
        }
        private void throwIfNeeded(MyThrowableType throwableType) {
            if (throwableType == MyThrowableType.EXCEPTION) {
                throw new MyRuntimeException();
            } else if (throwableType == MyThrowableType.ERROR) {
                throw new MyError();
            }
        }
    }

    private static class MyListenableClock extends EnslavedControllableHardClock {
        private HashSet<InterfaceClockModificationListener> listeners = new HashSet<InterfaceClockModificationListener>();
        public MyListenableClock(InterfaceHardClock masterClock) {
            super(masterClock);
        }
        @Override
        public boolean addListener(InterfaceClockModificationListener listener) {
            this.listeners.add(listener);
            return super.addListener(listener);
        }
        @Override
        public boolean removeListener(InterfaceClockModificationListener listener) {
            this.listeners.remove(listener);
            return super.removeListener(listener);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final long REAL_TIME_TOLERANCE_NS = REAL_TIME_TOLERANCE_MS * 1000L * 1000L;
    private static final double REAL_TIME_TOLERANCE_S = REAL_TIME_TOLERANCE_MS * 1e-3;

    private final Random random = TestUtils.newRandom123456789L();
    
    private final AtomicLong counter = new AtomicLong();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_toString() {
        final HardScheduler scheduler = HardScheduler.newSingleThreadedInstance(
                new SystemTimeClock(),
                "",
                true);

        assertNotNull(scheduler.toString());
        
        shutdownNowAndWait(scheduler);
    }

    /*
     * Configuration.
     */

    public void test_setMaxSystemWaitTimeNs_long() {
        final EnslavedControllableHardClock backingClock = getClockForTest();
        backingClock.setTimeNs(0);
        // Clock for which we can modify the time,
        // but which is not listenable.
        final InterfaceHardClock clock = new InterfaceHardClock() {
            @Override
            public double getTimeS() {
                return backingClock.getTimeS();
            }
            @Override
            public long getTimeNs() {
                return backingClock.getTimeNs();
            }
            @Override
            public double getTimeSpeed() {
                return backingClock.getTimeSpeed();
            }
        };
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {

            final long maxSystemWaitTimeNs = TimeUtils.sToNs(1.0);

            // Resetting max wait time, and workers waits, for
            // new max wait time to be taken into account.
            scheduler.setMaxSystemWaitTimeNs(maxSystemWaitTimeNs);

            // Scheduling for sometime far into the future,
            // then jumping to that time:
            // the schedule should occur maxSystemWaitTimeS
            // from now (unless spurious wake-up), when the
            // scheduler realizes he's late.
            final MyRunnable runnable = new MyRunnable(clock);
            final long startTimeNs = clock.getTimeNs();
            final long timeJumpNs = maxSystemWaitTimeNs + TimeUtils.sToNs(10.0);
            final long jumpedTimeNs = startTimeNs + timeJumpNs;
            scheduler.executeAtNs(runnable, jumpedTimeNs);
            // Letting time for schedule wait to start.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            // Setting clock's time to theoretical time (plus the time
            // we waited for the schedule wait to start), i.e. ideally
            // the scheduler should stop to wait right after, but it
            // won't be notified on this modification since the clock
            // is not listenable.
            backingClock.setTimeNs(jumpedTimeNs + REAL_TIME_TOLERANCE_NS);
            final MySchedulingReport report = runnable.waitAndGetReport();
            assertEquals(TimeUtils.nsToS(jumpedTimeNs + maxSystemWaitTimeNs), TimeUtils.nsToS(report.runCallTimeNs), REAL_TIME_TOLERANCE_S);
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /*
     * Getters.
     */

    public void test_getClock() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            assertEquals(clock, scheduler.getClock());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_getNbrOfRunningWorkers() {
        final InterfaceHardClock clock = getClockForTest();

        /*
         * Schedulers with 2 threads each.
         * Killing workers one by one, and checking
         * number of running workers each time.
         */

        // Thread factory quietly swallowing MyError, to avoid spam.
        final ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable runnable) {
                final Runnable wrapper = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            if (t instanceof MyError) {
                                // Quiet (to avoid spam).
                            } else {
                                Unchecked.throwIt(t);
                            }
                        }
                    }
                };
                return new Thread(wrapper);
            }
        };

        final ArrayList<HardScheduler> schedulerList = createNThreadsSchedulers(
                clock,
                2,
                threadFactory);

        // Letting time for workers to get NOT running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        // Starting workers, and checking.
        for (HardScheduler scheduler : schedulerList) {
            assertEquals(0, scheduler.getNbrOfRunningWorkers());
            scheduler.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {

            int expectedNbrOfAliveWorkers = scheduler.getNbrOfRunningWorkers();

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = MyThrowableType.ERROR,
                        throwableInOnCancel = null);
                scheduler.execute(runnable);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);
                // Letting time for worker thread to die.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                expectedNbrOfAliveWorkers--;
                assertEquals(expectedNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = MyThrowableType.ERROR,
                        throwableInOnCancel = null);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);
                // Letting time for worker thread to die.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                expectedNbrOfAliveWorkers--;
                assertEquals(expectedNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_getNbrOfIdleWorkers() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        // Letting time for workers to get NOT running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        // Starting workers, and checking.
        for (HardScheduler scheduler : schedulerList) {
            assertEquals(0, scheduler.getNbrOfIdleWorkers());
            scheduler.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (HardScheduler scheduler : schedulerList) {

            final int nbrOfWorkers = scheduler.getNbrOfRunningWorkers();

            assertEquals(nbrOfWorkers, scheduler.getNbrOfIdleWorkers());

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        2 * REAL_TIME_TOLERANCE_MS);
                scheduler.execute(runnable);
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers-1, scheduler.getNbrOfIdleWorkers());
                // Waiting for run to finish.
                runnable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers, scheduler.getNbrOfIdleWorkers());
            }

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        2 * REAL_TIME_TOLERANCE_MS);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers-1, scheduler.getNbrOfIdleWorkers());
                // Waiting for run to finish.
                runnable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers, scheduler.getNbrOfIdleWorkers());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_getNbrOfWorkingWorkers() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        // Starting workers.
        for (HardScheduler scheduler : schedulerList) {
            assertEquals(0, scheduler.getNbrOfWorkingWorkers());
            scheduler.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (HardScheduler scheduler : schedulerList) {

            assertEquals(0, scheduler.getNbrOfWorkingWorkers());

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        2 * REAL_TIME_TOLERANCE_MS);
                scheduler.execute(runnable);
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, scheduler.getNbrOfWorkingWorkers());
                // Waiting for run to finish.
                runnable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(0, scheduler.getNbrOfWorkingWorkers());
            }

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        2 * REAL_TIME_TOLERANCE_MS);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, scheduler.getNbrOfWorkingWorkers());
                // Waiting for run to finish.
                runnable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(0, scheduler.getNbrOfWorkingWorkers());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_getNbrOfPendingSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            assertEquals(0, scheduler.getNbrOfPendingSchedules());

            final MyRunnable runnable1 = new MyRunnable(clock);
            scheduler.execute(runnable1);
            assertEquals(1, scheduler.getNbrOfPendingSchedules());

            final MyRunnable runnable2 = new MyRunnable(clock);
            scheduler.executeAtNs(runnable2, clock.getTimeNs());
            assertEquals(2, scheduler.getNbrOfPendingSchedules());

            scheduler.startProcessing();

            // Waiting for runnables to be processed.
            runnable1.waitAndGetReport();
            runnable2.waitAndGetReport();

            assertEquals(0, scheduler.getNbrOfPendingSchedules());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_getNbrOfPendingAsapSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            final MyRunnable runnable1 = new MyRunnable(clock);
            scheduler.execute(runnable1);
            assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            final MyRunnable runnable2 = new MyRunnable(clock);
            scheduler.execute(runnable2);
            assertEquals(2, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            scheduler.startProcessing();

            // Waiting for runnables to be processed.
            runnable1.waitAndGetReport();
            runnable2.waitAndGetReport();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_getNbrOfPendingTimedSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            final MyRunnable runnable1 = new MyRunnable(clock);
            scheduler.executeAtNs(runnable1, clock.getTimeNs());
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

            final MyRunnable runnable2 = new MyRunnable(clock);
            scheduler.executeAtNs(runnable2, clock.getTimeNs());
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(2, scheduler.getNbrOfPendingTimedSchedules());

            scheduler.startProcessing();

            // Waiting for runnables to be processed.
            runnable1.waitAndGetReport();
            runnable2.waitAndGetReport();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_isWorkersDeathRequested() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            assertFalse(scheduler.isShutdown());
            scheduler.shutdown();
            assertTrue(scheduler.isShutdown());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /*
     * Controls.
     */
    
    public void test_startAndWorkInCurrentThread() {
        final InterfaceHardClock clock = getClockForTest();
        final HardScheduler scheduler = HardScheduler.newThreadlessInstance(clock);
        
        final List<String> runTagList = new ArrayList<String>();
        
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                runTagList.add("1");
            }
        });
        
        scheduler.executeAfterS(new Runnable() {
            @Override
            public void run() {
                runTagList.add("2");
            }
        }, 0.1);
        
        // Will throw, so that we can then check that we can call again
        // for more processing.
        scheduler.executeAfterS(new Runnable() {
            @Override
            public void run() {
                runTagList.add("3");
                // Terminating exceptionally.
                throw new MyRuntimeException();
            }
        }, 0.2);
        
        try {
            scheduler.startAndWorkInCurrentThread();
            fail();
        } catch (MyRuntimeException e) {
            // ok
        }
        
        assertEquals("[1, 2, 3]", Arrays.toString(runTagList.toArray()));
        
        assertEquals(0, scheduler.getNbrOfPendingSchedules());
        assertEquals(0, scheduler.getNbrOfRunningWorkers());
        assertEquals(0, scheduler.getNbrOfIdleWorkers());
        
        /*
         * Calling again.
         */
        
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                runTagList.add("4");
            }
        });
        
        scheduler.executeAfterS(new Runnable() {
            @Override
            public void run() {
                runTagList.add("5");
                // Terminating normally.
                scheduler.shutdown();
                scheduler.stop();
            }
        }, 0.1);

        scheduler.startAndWorkInCurrentThread();

        assertEquals("[1, 2, 3, 4, 5]", Arrays.toString(runTagList.toArray()));
        
        assertEquals(0, scheduler.getNbrOfPendingSchedules());
        assertEquals(0, scheduler.getNbrOfRunningWorkers());
        assertEquals(0, scheduler.getNbrOfIdleWorkers());
        
        shutdownNowAndWait(scheduler);
    }

    public void test_startWorkerThreadsIfNeeded() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        // Letting time for workers to get NOT running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        // Starting workers, and checking.
        for (HardScheduler scheduler : schedulerList) {
            assertEquals(0, scheduler.getNbrOfRunningWorkers());
            scheduler.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (HardScheduler scheduler : schedulerList) {
            assertNotSame(0, scheduler.getNbrOfRunningWorkers());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_start() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            // stop is supposed tested:
            // we check start gets things back up.
            scheduler.stop();

            scheduler.start();

            final long startTimeNs = clock.getTimeNs();

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.execute(runnable);
                final MySchedulingReport report = runnable.waitAndGetReport();
                // Processed right away.
                assertEquals(startTimeNs, report.runCallTimeNs, REAL_TIME_TOLERANCE_NS);
            }

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.executeAtNs(runnable, startTimeNs);
                final MySchedulingReport report = runnable.waitAndGetReport();
                // Processed right away.
                assertEquals(startTimeNs, report.runCallTimeNs, REAL_TIME_TOLERANCE_NS);
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_stop() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stop();

            // Checking schedules are not processed, and not accepted.

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.execute(runnable);
                // letting time for processing (not) to occur
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertTrue(runnable.onCancelCalled());
            }

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                // letting time for processing (not) to occur
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertTrue(runnable.onCancelCalled());
            }

            // Checking schedules were really not accepted,
            // i.e. didn't make it into pending schedules.
            // Supposes getNbrOfXXX methods work.

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_startAccepting() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            // stop is supposed tested:
            // we check startAccepting gets accepting back up.
            scheduler.stop();

            scheduler.startAccepting();

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.execute(runnable);
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertFalse(runnable.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
            }

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertFalse(runnable.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_stopAccepting() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {
            // Checking schedules are not accepted,
            // and pending schedules are processed.

            // To test timed schedules are still processed
            // when new ones are not accepted.
            // This runnable will be processed in a few.
            final MyRunnable toBeProcessedRunnable = new MyRunnable(clock);
            final long theoreticalTimeNs = clock.getTimeNs() + TimeUtils.sToNs(1.0);
            scheduler.executeAtNs(toBeProcessedRunnable, theoreticalTimeNs);
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

            // To test ASAP schedules are still processed
            // when new ones are not accepted.
            // Large enough to still have pending ASAP schedules
            // when acceptance gets stopped.
            final int nbrOfAsapSchedules = 4;
            for (int i = 0; i < nbrOfAsapSchedules; i++) {
                scheduler.execute(new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 10));
            }

            scheduler.stopAccepting();

            final int nbrOfAsapSchedulesAfterStopAcception = scheduler.getNbrOfPendingAsapSchedules();
            assertTrue(nbrOfAsapSchedulesAfterStopAcception != 0);
            // Letting time for ASAP schedules to be processed.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.execute(runnable);
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertTrue(runnable.onCancelCalled());
            }

            {
                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertTrue(runnable.onCancelCalled());
            }

            // Checking schedules were really not accepted,
            // i.e. didn't make it into pending schedules.
            // Supposes getNbrOfXXX methods work.
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

            // Checking timed schedule has been processed.
            toBeProcessedRunnable.waitAndGetReport();
            assertEquals(1, toBeProcessedRunnable.nbrOfRunCalls);
            assertFalse(toBeProcessedRunnable.onCancelCalled());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_startProcessing() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            {
                // stopProcessing is supposed tested:
                // we check startProcessing gets things back up.
                scheduler.stopProcessing();

                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.execute(runnable);

                // Letting time for runnable (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                // Runnable should start to be processed after that call.
                scheduler.startProcessing();

                runnable.waitAndGetReport();
                assertTrue(runnable.runCalled());
                assertFalse(runnable.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            }

            {
                // stopProcessing is supposed tested:
                // we check startProcessing gets things back up.
                scheduler.stopProcessing();

                final MyRunnable runnable = new MyRunnable(clock);
                scheduler.executeAtNs(runnable, clock.getTimeNs());

                // Letting time for runnable (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                // Runnable should start to be processed after that call.
                scheduler.startProcessing();

                runnable.waitAndGetReport();
                assertTrue(runnable.runCalled());
                assertFalse(runnable.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_stopProcessing() {
        final InterfaceHardClock clock = getClockForTest();

        /*
         * Scheduling when processing already stopped.
         */

        {
            final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);
            
            for (HardScheduler scheduler : schedulerList) {
                scheduler.stopProcessing();

                final MyRunnable runnable1 = new MyRunnable(clock);
                scheduler.execute(runnable1);

                final MyRunnable runnable2 = new MyRunnable(clock);
                scheduler.executeAtNs(runnable2, clock.getTimeNs());

                // Letting time for runnables (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
                assertFalse(runnable1.runCalled());
                assertFalse(runnable1.onCancelCalled());
                assertFalse(runnable2.runCalled());
                assertFalse(runnable2.onCancelCalled());
            }
            
            shutdownNowAndWait(schedulerList);
        }

        /*
         * Stopping processing when scheduling is going on.
         */

        {
            final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);
            
            for (HardScheduler scheduler : schedulerList) {

                // Runnables that wait for a bit, and enough
                // of them for pending schedules queues to contain
                // a few of them before we start to sleep.
                final int nbrOfSchedules = 10;
                final int sleepTimeMsInRun = 10;
                for (int i = 0; i < nbrOfSchedules; i++) {
                    MyRunnable runnable1 = new MyRunnable(
                            clock,
                            sleepTimeMsInRun);
                    scheduler.execute(runnable1);
                    MyRunnable runnable2 = new MyRunnable(
                            clock,
                            sleepTimeMsInRun);
                    scheduler.executeAtNs(runnable2, clock.getTimeNs());
                }

                // Stopping processing, which is supposed to make pending
                // schedules not processed.
                scheduler.stopProcessing();
                // Retrieving numbers of pending schedules right after the stop.
                // Since schedules processing is stopped, this number must remain
                // the same, which we check after a little sleep.
                final int nbrOfPendingAsapSchedules1 = scheduler.getNbrOfPendingAsapSchedules();
                final int nbrOfPendingTimedSchedules1 = scheduler.getNbrOfPendingTimedSchedules();
                sleepMS(REAL_TIME_TOLERANCE_MS);
                final int nbrOfPendingAsapSchedules2 = scheduler.getNbrOfPendingAsapSchedules();
                final int nbrOfPendingTimedSchedules2 = scheduler.getNbrOfPendingTimedSchedules();

                assertEquals(nbrOfPendingAsapSchedules1, nbrOfPendingAsapSchedules2);
                assertEquals(nbrOfPendingTimedSchedules1, nbrOfPendingTimedSchedules2);
            }
            
            shutdownNowAndWait(schedulerList);
        }
    }

    public void test_cancelPendingSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {
            // Supposing stopProcessing works.
            scheduler.stopProcessing();

            /*
             * Canceling pending schedules right after scheduling.
             */

            {
                final MyRunnable runnable1 = new MyRunnable(clock);
                scheduler.execute(runnable1);
                final MyRunnable runnable2 = new MyRunnable(clock);
                scheduler.executeAtNs(runnable2, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                scheduler.cancelPendingSchedules();

                assertFalse(runnable1.runCalled());
                assertFalse(runnable2.runCalled());

                assertTrue(runnable1.onCancelCalled());
                assertTrue(runnable2.onCancelCalled());

                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }

            /*
             * Testing schedules cancellation stops if one throws an exception.
             */

            {
                // Getting non-throwing runnables surrounded
                // with throwing ones, to handle different cases
                // of iteration.
                final MyRunnable runnableExceptionAsap1 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(runnableExceptionAsap1);
                final MyRunnable runnableExceptionTimed1 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(runnableExceptionTimed1, clock.getTimeNs());

                final MyRunnable runnableAsap = new MyRunnable(clock);
                scheduler.execute(runnableAsap);
                final MyRunnable runnableTimed = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

                final MyRunnable runnableExceptionAsap2 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(runnableExceptionAsap2);
                final MyRunnable runnableExceptionTimed2 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(runnableExceptionTimed2, clock.getTimeNs());

                assertEquals(3, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(3, scheduler.getNbrOfPendingTimedSchedules());

                try {
                    scheduler.cancelPendingSchedules();
                    fail();
                } catch (Exception e) {
                    // ok
                }

                assertFalse(runnableAsap.runCalled());
                assertFalse(runnableTimed.runCalled());
                assertFalse(runnableExceptionAsap1.runCalled());
                assertFalse(runnableExceptionAsap2.runCalled());
                assertFalse(runnableExceptionTimed1.runCalled());
                assertFalse(runnableExceptionTimed2.runCalled());

                assertFalse(runnableAsap.onCancelCalled() && runnableExceptionAsap1.onCancelCalled() && runnableExceptionAsap2.onCancelCalled());
                assertFalse(runnableTimed.onCancelCalled() && runnableExceptionTimed1.onCancelCalled() && runnableExceptionTimed2.onCancelCalled());

                assertNotSame(0, scheduler.getNbrOfPendingAsapSchedules());
                assertNotSame(0, scheduler.getNbrOfPendingTimedSchedules());

                // Continuing cancellation until no more pending schedule.
                while (true) {
                    try {
                        scheduler.cancelPendingSchedules();
                    } catch (Exception e) {
                        // quiet
                    }
                    if (scheduler.getNbrOfPendingSchedules() == 0) {
                        break;
                    }
                }
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_cancelPendingAsapSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {

            /*
             * Canceling pending ASAP schedules right after scheduling.
             */

            {
                scheduler.stopProcessing();

                final MyRunnable runnableAsap = new MyRunnable(clock);
                scheduler.execute(runnableAsap);
                final MyRunnable runnableTimed = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                scheduler.cancelPendingAsapSchedules();

                assertFalse(runnableAsap.runCalled());
                assertTrue(runnableAsap.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());

                // Timed schedule not canceled.
                assertFalse(runnableTimed.runCalled());
                assertFalse(runnableTimed.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                // Starting processing for timed schedule.
                scheduler.startProcessing();

                // Waiting for timed schedule to be processed.
                runnableTimed.waitAndGetReport();

                assertTrue(runnableTimed.runCalled());
                assertFalse(runnableTimed.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }

            /*
             * Testing schedules cancellation stops if one throws an exception.
             */

            {
                scheduler.stopProcessing();

                // Getting non-throwing ASAP runnable surrounded
                // with throwing ones, to handle different cases
                // of iteration.
                final MyRunnable runnableExceptionAsap1 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(runnableExceptionAsap1);

                final MyRunnable runnableAsap = new MyRunnable(clock);
                scheduler.execute(runnableAsap);
                final MyRunnable runnableTimed = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

                final MyRunnable runnableExceptionAsap2 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(runnableExceptionAsap2);

                assertEquals(3, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                try {
                    scheduler.cancelPendingAsapSchedules();
                    fail();
                } catch (Exception e) {
                    // ok
                }

                assertFalse(runnableAsap.runCalled());
                assertFalse(runnableExceptionAsap1.runCalled());
                assertFalse(runnableExceptionAsap2.runCalled());

                assertFalse(runnableAsap.onCancelCalled()
                        && runnableExceptionAsap1.onCancelCalled()
                        && runnableExceptionAsap2.onCancelCalled());

                assertNotSame(0, scheduler.getNbrOfPendingAsapSchedules());

                // Continuing cancellation until no more pending ASAP schedule.
                while (true) {
                    try {
                        scheduler.cancelPendingAsapSchedules();
                    } catch (Exception e) {
                        // quiet
                    }
                    if (scheduler.getNbrOfPendingAsapSchedules() == 0) {
                        break;
                    }
                }

                // Timed schedule not canceled.
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                // Starting processing for timed schedule.
                scheduler.startProcessing();

                // Waiting for timed schedule to be processed.
                runnableTimed.waitAndGetReport();

                assertTrue(runnableTimed.runCalled());
                assertFalse(runnableTimed.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_cancelPendingTimedSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {

            /*
             * Canceling pending timed schedules right after scheduling.
             */

            {
                scheduler.stopProcessing();

                final MyRunnable runnableAsap = new MyRunnable(clock);
                scheduler.execute(runnableAsap);
                final MyRunnable runnableTimed = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                scheduler.cancelPendingTimedSchedules();

                assertFalse(runnableTimed.runCalled());
                assertTrue(runnableTimed.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

                // ASAP schedule not canceled.
                assertFalse(runnableAsap.runCalled());
                assertFalse(runnableAsap.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());

                // Starting processing for ASAP schedule.
                scheduler.startProcessing();

                // Waiting for ASAP schedule to be processed.
                runnableAsap.waitAndGetReport();

                assertTrue(runnableAsap.runCalled());
                assertFalse(runnableAsap.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            }

            /*
             * Testing schedules cancellation stops if one throws an exception.
             */

            {
                scheduler.stopProcessing();

                // Getting non-throwing timed runnable surrounded
                // with throwing ones, to handle different cases
                // of iteration.
                final MyRunnable runnableExceptionTimed1 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(runnableExceptionTimed1, clock.getTimeNs());

                final MyRunnable runnableAsap = new MyRunnable(clock);
                scheduler.execute(runnableAsap);
                final MyRunnable runnableTimed = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

                final MyRunnable runnableExceptionTimed2 = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(runnableExceptionTimed2, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(3, scheduler.getNbrOfPendingTimedSchedules());

                try {
                    scheduler.cancelPendingTimedSchedules();
                    fail();
                } catch (Exception e) {
                    // ok
                }

                assertFalse(runnableTimed.runCalled());
                assertFalse(runnableExceptionTimed1.runCalled());
                assertFalse(runnableExceptionTimed2.runCalled());

                assertFalse(
                        runnableTimed.onCancelCalled()
                        && runnableExceptionTimed1.onCancelCalled()
                        && runnableExceptionTimed2.onCancelCalled());

                assertNotSame(0, scheduler.getNbrOfPendingTimedSchedules());

                // Continuing cancellation until no more pending timed schedule.
                while (true) {
                    try {
                        scheduler.cancelPendingTimedSchedules();
                    } catch (Exception e) {
                        // quiet
                    }
                    if (scheduler.getNbrOfPendingTimedSchedules() == 0) {
                        break;
                    }
                }

                // ASAP schedule not canceled.
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());

                // Starting processing for ASAP schedule.
                scheduler.startProcessing();

                // Waiting for ASAP schedule to be processed.
                runnableAsap.waitAndGetReport();

                assertTrue(runnableAsap.runCalled());
                assertFalse(runnableAsap.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_drainPendingAsapRunnablesInto_ObjectVector() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {

            /*
             * No runnable.
             */

            {
                // Must work with collection of Object.
                ArrayList<Object> runnables = new ArrayList<Object>();
                Object foo = new Object();
                runnables.add(foo);
                scheduler.drainPendingAsapRunnablesInto(runnables);

                // Nothing added, nor removed.
                assertEquals(1, runnables.size());
                assertEquals(foo, runnables.get(0));
            }

            /*
             * Runnables.
             */

            {
                scheduler.stopProcessing();

                final MyRunnable runnableAsap1 = new MyRunnable(clock);
                scheduler.execute(runnableAsap1);
                final MyRunnable runnableAsap2 = new MyRunnable(clock);
                scheduler.execute(runnableAsap2);
                final MyRunnable runnableTimed = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

                assertEquals(2, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                final ArrayList<Object> runnables = new ArrayList<Object>();
                final Object foo = new Object();
                runnables.add(foo);
                scheduler.drainPendingAsapRunnablesInto(runnables);

                // Checking drained.
                assertEquals(3, runnables.size());
                assertEquals(foo, runnables.get(0));
                assertEquals(runnableAsap1, runnables.get(1));
                assertEquals(runnableAsap2, runnables.get(2));

                assertFalse(runnableAsap1.runCalled());
                assertFalse(runnableAsap1.onCancelCalled());
                assertFalse(runnableAsap2.runCalled());
                assertFalse(runnableAsap2.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());

                // Timed runnable not drained.
                assertFalse(runnableTimed.runCalled());
                assertFalse(runnableTimed.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_drainPendingTimedRunnablesInto_ObjectVector() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {

            /*
             * No runnable.
             */

            {
                // Must work with collection of Object
                final ArrayList<Object> runnables = new ArrayList<Object>();
                final Object foo = new Object();
                runnables.add(foo);
                scheduler.drainPendingTimedRunnablesInto(runnables);

                // Nothing added, nor removed.
                assertEquals(1, runnables.size());
                assertEquals(foo, runnables.get(0));
            }

            /*
             * Runnables.
             */

            {
                scheduler.stopProcessing();

                final MyRunnable runnableAsap = new MyRunnable(clock);
                scheduler.execute(runnableAsap);
                final MyRunnable runnableTimed1 = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed1, clock.getTimeNs());
                final MyRunnable runnableTimed2 = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTimed2, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(2, scheduler.getNbrOfPendingTimedSchedules());

                final ArrayList<Object> runnables = new ArrayList<Object>();
                final Object foo = new Object();
                runnables.add(foo);
                scheduler.drainPendingTimedRunnablesInto(runnables);

                // Checking drained.
                assertEquals(3, runnables.size());
                assertEquals(foo, runnables.get(0));
                assertTrue(runnables.contains(runnableTimed1));
                assertTrue(runnables.contains(runnableTimed2));

                assertFalse(runnableTimed1.runCalled());
                assertFalse(runnableTimed1.onCancelCalled());
                assertFalse(runnableTimed2.runCalled());
                assertFalse(runnableTimed2.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

                // ASAP runnable not drained.
                assertFalse(runnableAsap.runCalled());
                assertFalse(runnableAsap.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }
    
    public void test_workersWakeUpOnSchedules_cancel_asap() {
        final boolean mustCancelElseDrain = true;
        final boolean mustAsapElseTimed = true;
        for (int k = 0; k < 5; k++) {
            test_workersWakeUpOnSchedulesXxx(mustCancelElseDrain, mustAsapElseTimed);
        }
    }
    
    public void test_workersWakeUpOnSchedules_cancel_timed() {
        final boolean mustCancelElseDrain = true;
        final boolean mustAsapElseTimed = false;
        for (int k = 0; k < 5; k++) {
            test_workersWakeUpOnSchedulesXxx(mustCancelElseDrain, mustAsapElseTimed);
        }
    }
    
    public void test_workersWakeUpOnSchedules_drain_asap() {
        final boolean mustCancelElseDrain = false;
        final boolean mustAsapElseTimed = true;
        for (int k = 0; k < 5; k++) {
            test_workersWakeUpOnSchedulesXxx(mustCancelElseDrain, mustAsapElseTimed);
        }
    }
    
    public void test_workersWakeUpOnSchedules_drain_timed() {
        final boolean mustCancelElseDrain = false;
        final boolean mustAsapElseTimed = false;
        for (int k = 0; k < 5; k++) {
            test_workersWakeUpOnSchedulesXxx(mustCancelElseDrain, mustAsapElseTimed);
        }
    }
    
    public void test_workersWakeUpOnSchedulesXxx(
            boolean mustCancelElseDrain,
            boolean mustAsapElseTimed) {

        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {

            scheduler.startWorkerThreadsIfNeeded();
            // Letting time for worker to go live.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(1, scheduler.getNbrOfRunningWorkers());

            // For worker to wait for runnable to be processable.
            scheduler.stopProcessing();
            
            final MyRunnable runnable = new MyRunnable(clock);
            if (mustAsapElseTimed) {
                scheduler.execute(runnable);
            } else {
                scheduler.executeAtNs(runnable, Long.MAX_VALUE);
            }

            scheduler.shutdown();

            assertEquals(1, scheduler.getNbrOfPendingSchedules());
            assertEquals(1, scheduler.getNbrOfRunningWorkers());

            if (mustCancelElseDrain) {
                if (mustAsapElseTimed) {
                    scheduler.cancelPendingAsapSchedules();
                } else {
                    scheduler.cancelPendingTimedSchedules();
                }
            } else {
                final ArrayList<Object> runnables = new ArrayList<Object>();
                if (mustAsapElseTimed) {
                    scheduler.drainPendingAsapRunnablesInto(runnables);
                } else {
                    scheduler.drainPendingTimedRunnablesInto(runnables);
                }
            }
            assertEquals(0, scheduler.getNbrOfPendingSchedules());
            // Letting time for worker to die.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(0, scheduler.getNbrOfRunningWorkers());
        }

        shutdownNowAndWait(schedulerList);
    }

    public void test_interruptWorkers() {
        final InterfaceHardClock clock = getClockForTest();

        /*
         * Schedulers with 2 workers each, for we put one ASAP and one timed
         * schedule in each of them, and want them to be processed together,
         * so we can interrupt them together.
         * 
         * Not using our default thread factory, for we want to make sure
         * the workers never die on interrupt.
         */

        final ArrayList<HardScheduler> schedulerList = createNThreadsSchedulers(
                clock,
                2,
                null);

        /*
         * Checking interrupting idle workers doesn't hurt.
         */

        // Starting workers.
        for (HardScheduler scheduler : schedulerList) {
            scheduler.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (HardScheduler scheduler : schedulerList) {

            final int nbrOfWorkers = scheduler.getNbrOfRunningWorkers();

            // Checking interrupting idle workers doesn't hurt.
            scheduler.interruptWorkers();
            // Letting time for workers to recover from interrupt.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(nbrOfWorkers, scheduler.getNbrOfRunningWorkers());
        }

        /*
         * Checking interrupting working workers works.
         */

        for (HardScheduler scheduler : schedulerList) {

            final MyRunnable runnable1 = new MyRunnable(
                    clock,
                    Long.MAX_VALUE);
            scheduler.execute(runnable1);
            final MyRunnable runnable2 = new MyRunnable(
                    clock,
                    Long.MAX_VALUE);
            scheduler.executeAtNs(runnable2, clock.getTimeNs());

            // Letting time for runnables to be called.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            // Runnables being called.
            assertTrue(runnable1.runCalled());
            assertTrue(runnable2.runCalled());
            assertFalse(runnable1.onCancelCalled());
            assertFalse(runnable2.onCancelCalled());

            scheduler.interruptWorkers();

            // Letting time for interrupt to propagate.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertTrue(runnable1.runInterrupted);
            assertTrue(runnable2.runInterrupted);
            assertFalse(runnable1.onCancelCalled());
            assertFalse(runnable2.onCancelCalled());
            assertFalse(runnable1.onCancelInterrupted);
            assertFalse(runnable2.onCancelInterrupted);
        }
        
        /*
         * Checking interrupting working workers works
         * even when scheduler is calling onCancel().
         */

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        
        for (final HardScheduler scheduler : schedulerList) {

            final MyRunnable runnableToCancel = new MyRunnable(
                    clock,
                    sleepTimeMsInRun = 0,
                    sleepTimeMsInOnCancel = Long.MAX_VALUE);
            scheduler.executeAfterNs(runnableToCancel, REAL_TIME_TOLERANCE_NS/2);
            
            final MyRunnable runnableCanceller = new MyRunnable(clock) {
                @Override
                public void run() {
                    super.run();
                    // Will cause runnable1 execution to be rejected,
                    // i.e. onCancel() called synchronously.
                    scheduler.cancelPendingTimedSchedules();
                }
            };
            scheduler.execute(runnableCanceller);

            // Letting time for run() to be called
            // and then onCancel() to start being called.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            
            assertTrue(runnableCanceller.runCalled());
            assertFalse(runnableCanceller.onCancelCalled());
            assertFalse(runnableCanceller.runInterrupted);
            
            assertFalse(runnableToCancel.runCalled());
            assertTrue(runnableToCancel.onCancelCalled());
            // We didn't interrupt yet.
            assertFalse(runnableToCancel.runInterrupted);
            assertFalse(runnableToCancel.onCancelInterrupted);
            
            scheduler.interruptWorkers();

            // Letting time for interrupt to propagate.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertFalse(runnableToCancel.runInterrupted);
            assertTrue(runnableToCancel.onCancelInterrupted);
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_shutdown() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {
            // Supposing stopProcessing works.
            scheduler.stopProcessing();

            /*
             * Getting some pending schedules, one ASAP, and one timed.
             */

            final MyRunnable runnableAsap = new MyRunnable(clock);
            scheduler.execute(runnableAsap);

            final MyRunnable runnableTimed = new MyRunnable(clock);
            scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

            /*
             * Requesting workers death when idle.
             */

            scheduler.shutdown();

            // Making sure no more schedules are accepted.
            final MyRunnable runnableAsapRejected = new MyRunnable(clock);
            scheduler.execute(runnableAsapRejected);
            assertFalse(runnableAsapRejected.runCalled());
            assertTrue(runnableAsapRejected.onCancelCalled());
            //
            final MyRunnable runnableTimedRejected = new MyRunnable(clock);
            scheduler.executeAtNs(runnableTimedRejected, clock.getTimeNs());
            assertFalse(runnableTimedRejected.runCalled());
            assertTrue(runnableTimedRejected.onCancelCalled());

            // Letting time for workers NOT to die (since there are pending schedules).
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
            assertTrue(scheduler.getNbrOfRunningWorkers() > 0);
            assertTrue(scheduler.getNbrOfIdleWorkers() > 0);
            assertTrue(scheduler.getNbrOfWorkingWorkers() == 0);

            /*
             * Starting processing, and then verifying schedules
             * were correctly executed before workers death.
             */

            scheduler.startProcessing();

            // Waiting for schedules to be processed.
            runnableAsap.waitAndGetReport();
            runnableTimed.waitAndGetReport();

            // Checking correct schedules execution.
            assertTrue(runnableAsap.runCalled());
            assertFalse(runnableAsap.onCancelCalled());
            //
            assertEquals(1, runnableTimed.nbrOfRunCalls);
            assertFalse(runnableTimed.onCancelCalled());

            // Letting time for workers to die.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertEquals(0, scheduler.getNbrOfRunningWorkers());
            assertEquals(0, scheduler.getNbrOfIdleWorkers());
            assertEquals(0, scheduler.getNbrOfWorkingWorkers());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /*
     * Waits.
     */

    public void test_waitForNoMoreRunningWorkerSystemTime_long() {
        test_waitForNoMoreRunningWorkerXXXTime_long(true);
    }

    public void test_waitForNoMoreRunningWorkerClockTime_long() {
        test_waitForNoMoreRunningWorkerXXXTime_long(false);
    }

    public void test_waitForNoMoreRunningWorkerXXXTime_long(boolean systemWait) {
        final double timeSpeed = 2.0;

        final EnslavedControllableHardClock clock = getClockForTest();
        clock.setTimeSpeed(timeSpeed);

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        final long timeoutNs = TimeUtils.sToNs(1.0);
        final long systemTimeoutNs;
        if (systemWait) {
            systemTimeoutNs = timeoutNs;
        } else {
            systemTimeoutNs = (long)(timeoutNs / timeSpeed);
        }

        for (HardScheduler scheduler : schedulerList) {

            final MyRunnable runnableAsap = new MyRunnable(clock);
            final MyRunnable runnableTimed = new MyRunnable(clock);

            scheduler.stopProcessing();

            scheduler.execute(runnableAsap);
            scheduler.executeAtNs(runnableTimed, clock.getTimeNs());

            // No need to wait for workers to get running, since
            // they are not considered as "no more running" if they
            // have not all been started yet.
            if (false) {
                sleepMS(REAL_TIME_TOLERANCE_MS);
            }

            /*
             * Waiting timeout.
             */

            long a = System.nanoTime();
            boolean doneBeforeTimeout = false;
            try {
                if (systemWait) {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerSystemTimeNs(timeoutNs);
                } else {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerClockTimeNs(timeoutNs);
                }
            } catch (InterruptedException e) {
                fail();
            }
            long b = System.nanoTime();
            assertFalse(doneBeforeTimeout);
            assertEquals(systemTimeoutNs, b-a, REAL_TIME_TOLERANCE_NS);

            /*
             * Wait interrupted (need to take care to interrupt while in the wait,
             * for interrupted status to be cleared by exception catch).
             */

            final Thread tester = Thread.currentThread();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sleepMS(TimeUnit.NANOSECONDS.toMillis(systemTimeoutNs/2));
                    tester.interrupt();
                }
            }).start();

            try {
                if (systemWait) {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerSystemTimeNs(timeoutNs);
                } else {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerClockTimeNs(timeoutNs);
                }
                // Wait must throw InterruptedException.
                fail();
            } catch (InterruptedException e) {
                // quiet
            }

            /*
             * Not waiting much if any (schedules processed, then dying).
             */

            scheduler.startProcessing();
            scheduler.shutdown();

            a = System.nanoTime();
            try {
                if (systemWait) {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerSystemTimeNs(timeoutNs);
                } else {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerClockTimeNs(timeoutNs);
                }
            } catch (InterruptedException e) {
                fail();
            }
            b = System.nanoTime();
            assertTrue(doneBeforeTimeout);
            assertEquals(0, b-a, REAL_TIME_TOLERANCE_NS);

            // No more running worker.
            assertEquals(0, scheduler.getNbrOfRunningWorkers());

            // Checking schedules have been executed before wait returned.
            assertTrue(runnableAsap.runCalled());
            assertFalse(runnableAsap.onCancelCalled());
            assertTrue(runnableTimed.runCalled());
            assertFalse(runnableTimed.onCancelCalled());

            /*
             * Not waiting at all (no running worker when wait starts).
             */

            a = System.nanoTime();
            try {
                if (systemWait) {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerSystemTimeNs(timeoutNs);
                } else {
                    doneBeforeTimeout = scheduler.waitForNoMoreRunningWorkerClockTimeNs(timeoutNs);
                }
            } catch (InterruptedException e) {
                fail();
            }
            b = System.nanoTime();
            assertTrue(doneBeforeTimeout);
            assertEquals(0, b-a, REAL_TIME_TOLERANCE_NS);
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /*
     * Scheduling.
     */

    public void test_execute_fifoOrder() {
        final EnslavedControllableHardClock clock = getClockForTest();

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (InterfaceScheduler scheduler : schedulerList) {
            // Reseting counter.
            this.counter.set(0);

            final ArrayList<MyRunnable> runnableList = new ArrayList<MyRunnable>();

            final int nbrOfRunnables = 100000;

            for (int i = 0; i < nbrOfRunnables; i++) {
                final MyRunnable runnable = new MyRunnable(clock);
                runnableList.add(runnable);
                // clock not supposed to have any impact on ASAP scheduling
                // (to stop them, must stop scheduler)
                clock.setTimeNs(TimeUtils.sToNs(random.nextDouble()));
                clock.setTimeSpeed(random.nextDouble());
                scheduler.execute(runnable);
            }

            for (int i = 0; i < nbrOfRunnables; i++) {
                final MyRunnable runnable = runnableList.get(i);
                assertEquals(i+1,runnable.waitAndGetReport().orderNum);
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /**
     * Testing FIFO order of schedules for a same time
     * (supposing clock's time does not go backward,
     * and only 1 thread is used for timed schedules).
     */
    public void test_executeAt_executeAfter_fifoOrder() {
        final EnslavedControllableHardClock clock = getClockForTest();
        clock.setTimeSpeed(0.0);
        final long nowNs = 123456789L;
        clock.setTimeNs(nowNs);

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (InterfaceScheduler scheduler : schedulerList) {
            // Reseting counter.
            this.counter.set(0);

            final int nbrOfRunnables = 100000;

            final ArrayList<MyRunnable> runnableList = new ArrayList<MyRunnable>();

            for (int i = 0; i < nbrOfRunnables; i++) {
                final MyRunnable runnable = new MyRunnable(clock);
                runnableList.add(runnable);
                if ((i&1) == 0) {
                    scheduler.executeAtNs(runnable, nowNs);
                } else {
                    scheduler.executeAfterNs(runnable, 0L);
                }
            }

            for (int i = 0; i < nbrOfRunnables; i++) {
                final MyRunnable runnable = runnableList.get(i);
                assertEquals(i+1,runnable.waitAndGetReport().orderNum);
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }
    
    /**
     * Testing that timed schedules are properly ordered,
     * first according to time, second according to a sequence number.
     */
    public void test_executeAt_ordering() {
        final EnslavedControllableHardClock clock = getClockForTest();
        clock.setTimeSpeed(0.0);
        final long nowNs = 123456789L;
        clock.setTimeNs(nowNs);

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (InterfaceScheduler scheduler : schedulerList) {
            // Reseting counter.
            this.counter.set(0);

            final MyRunnable s7 = new MyRunnable(clock);
            scheduler.executeAtNs(s7, Long.MAX_VALUE);
            final MyRunnable s8 = new MyRunnable(clock);
            scheduler.executeAtNs(s8, Long.MAX_VALUE);

            final MyRunnable s5 = new MyRunnable(clock);
            scheduler.executeAtNs(s5, Long.MAX_VALUE - 1);
            final MyRunnable s6 = new MyRunnable(clock);
            scheduler.executeAtNs(s6, Long.MAX_VALUE - 1);

            final MyRunnable s3 = new MyRunnable(clock);
            scheduler.executeAtNs(s3, nowNs + 1L);
            final MyRunnable s4 = new MyRunnable(clock);
            scheduler.executeAtNs(s4, nowNs + 1L);

            final MyRunnable s1 = new MyRunnable(clock);
            scheduler.executeAtNs(s1, Long.MIN_VALUE);
            final MyRunnable s2 = new MyRunnable(clock);
            scheduler.executeAtNs(s2, Long.MIN_VALUE);
            
            /*
             * Real time speed: s1 to s4 will be quickly executed.
             */
            
            clock.setTimeSpeed(1.0);
            sleepMS(REAL_TIME_TOLERANCE_MS);
            
            assertEquals(1, s1.waitAndGetReport().orderNum);
            assertEquals(2, s2.waitAndGetReport().orderNum);
            
            assertEquals(3, s3.waitAndGetReport().orderNum);
            assertEquals(4, s4.waitAndGetReport().orderNum);
            
            assertEquals(0, s5.nbrOfRunCalls);
            assertEquals(0, s6.nbrOfRunCalls);
            
            assertEquals(0, s7.nbrOfRunCalls);
            assertEquals(0, s8.nbrOfRunCalls);
            
            /*
             * Infinite time speed: s5 to s8 will be quickly executed,
             * (Long.MAV_VALUE is not a special value here).
             */
            
            clock.setTimeSpeed(Double.POSITIVE_INFINITY);
            sleepMS(REAL_TIME_TOLERANCE_MS);
            
            assertEquals(5, s5.waitAndGetReport().orderNum);
            assertEquals(6, s6.waitAndGetReport().orderNum);
            
            assertEquals(7, s7.waitAndGetReport().orderNum);
            assertEquals(8, s8.waitAndGetReport().orderNum);
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /*
     * General tests (not method-specific).
     */

    /**
     * Testing scheduler's behavior when time is modified
     * (using zero time speed not to be disturbed by real time).
     */
    public void test_modifiableClock_timeModified() {
        final EnslavedControllableHardClock clock = getClockForTest();

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (InterfaceScheduler scheduler : schedulerList) {
            clock.setTimeSpeed(0.0);
            clock.setTimeNs(0L);

            MyRunnable runnable;

            /*
             * ASAP
             */

            runnable = new MyRunnable(clock);
            scheduler.execute(runnable);
            assertEquals(0L, runnable.waitAndGetReport().runCallTimeNs);

            /*
             * At time (now).
             */

            runnable = new MyRunnable(clock);
            scheduler.executeAtNs(runnable,0L);

            assertEquals(0L, runnable.waitAndGetReport().runCallTimeNs);

            /*
             * At time (future).
             */

            runnable = new MyRunnable(clock);
            scheduler.executeAtNs(runnable,1L);

            // Letting time for processing (not) to occur.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertFalse(runnable.runCalled());

            // Clock change supposed to stop scheduler's wait.
            clock.setTimeNs(1L);
            assertEquals(1L, runnable.waitAndGetReport().runCallTimeNs);

            /*
             * At time (future) with multiple time changes.
             */

            runnable = new MyRunnable(clock);
            scheduler.executeAtNs(runnable,1000L);

            clock.setTimeNs(500L);
            clock.setTimeNs(100L);
            clock.setTimeNs(900L);
            clock.setTimeNs(199L);
            clock.setTimeNs(999L);

            // Letting time for processing (not) to occur.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertFalse(runnable.runCalled());

            clock.setTimeNs(1000L);
            assertEquals(1000L, runnable.waitAndGetReport().runCallTimeNs);
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /**
     * Testing scheduler's behavior when time speed is modified:
     * wait time must be updated accordingly.
     */
    public void test_modifiableClock_timeSpeedModified() {
        final long startTimeNs = TimeUtils.sToNs(1000.0);

        final EnslavedControllableHardClock clock = getClockForTest();

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (InterfaceScheduler scheduler : schedulerList) {
            clock.setTimeSpeed(0.0);
            clock.setTimeNs(startTimeNs);

            /*
             * 
             */

            final long durationMs = 1000L;
            final long durationNs = durationMs * (1000L * 1000L);
            final double halfTimeSpeed = 0.5;

            final long dummyWaitMs = durationMs/10;

            final MyRunnable runnable = new MyRunnable(clock);
            scheduler.executeAfterNs(runnable, durationNs);
            final long sysStartMs = System.currentTimeMillis();

            // Waiting some real time (clock time not flowing).
            sleepMS(dummyWaitMs);
            // Starting clock time.
            clock.setTimeSpeed(1.0);
            // Letting clock time flow at real time speed, for half the total duration.
            sleepMS(durationMs/2);
            // Stopping clock time.
            clock.setTimeSpeed(0.0);
            // Waiting some real time (clock time not flowing).
            sleepMS(dummyWaitMs);
            // Starting clock time, half real time speed.
            clock.setTimeSpeed(halfTimeSpeed);
            // Letting clock time flow at half real time speed,
            // for the remaining duration before schedule,
            // i.e. half the duration in clock time,
            // and the duration in real time (since clock time is twice slower).

            final MySchedulingReport report = runnable.waitAndGetReport();
            final long sysEndMs = System.currentTimeMillis();

            // Checking real time spent.
            assertEquals(durationMs/2 + 2*dummyWaitMs + durationMs, sysEndMs - sysStartMs, REAL_TIME_TOLERANCE_MS);
            // Checking scheduling actual time.
            assertEquals(startTimeNs+durationNs, report.runCallTimeNs, REAL_TIME_TOLERANCE_NS);
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_schedulingFairness() {
        final EnslavedControllableHardClock clock = getClockForTest();
        clock.setTimeSpeed(0.0);
        clock.setTimeNs(0L);

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (InterfaceScheduler scheduler : schedulerList) {
            // Stopping scheduler while we setup schedules.
            ((HardScheduler) scheduler).stopProcessing();

            final MyRunnable scheduledAsap1 = new MyRunnable(clock);
            final MyRunnable scheduledInFuture = new MyRunnable(clock);
            final MyRunnable scheduledAsap2 = new MyRunnable(clock);
            final MyRunnable scheduledInPast = new MyRunnable(clock);
            final MyRunnable scheduledAsap3 = new MyRunnable(clock);
            scheduler.execute(scheduledAsap1);
            scheduler.executeAtNs(scheduledInFuture, clock.getTimeNs() + REAL_TIME_TOLERANCE_NS);
            scheduler.execute(scheduledAsap2);
            scheduler.executeAtNs(scheduledInPast, clock.getTimeNs() - REAL_TIME_TOLERANCE_NS);
            scheduler.execute(scheduledAsap3);

            // Starting scheduler and clock.
            ((HardScheduler)scheduler).startProcessing();
            clock.setTimeSpeed(1.0);
            
            /*
             * Asap1 and Asap2 executed before InPast because scheduled before.
             */
            final long num1 = scheduledAsap1.waitAndGetReport().orderNum;
            final long num2 = scheduledAsap2.waitAndGetReport().orderNum;
            final long num3 = scheduledInPast.waitAndGetReport().orderNum;
            final long num4 = scheduledAsap3.waitAndGetReport().orderNum;
            final long num5 = scheduledInFuture.waitAndGetReport().orderNum;
            assertEquals(num2, num1 + 1);
            assertEquals(num3, num2 + 1);
            assertEquals(num4, num3 + 1);
            assertEquals(num5, num4 + 1);
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /**
     * Testing that worker threads survive exceptions.
     */
    public void test_workersSurvivalFromExceptions() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        // Starting workers.
        for (HardScheduler scheduler : schedulerList) {
            scheduler.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (HardScheduler scheduler : schedulerList) {

            final int initialNbrOfAliveWorkers = scheduler.getNbrOfRunningWorkers();

            /*
             * Regular call.
             */

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = null);
                scheduler.execute(runnable);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);
            }

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = null);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);
            }

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = null);
                scheduler.executeAfterNs(runnable, 0);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);
            }

            /*
             * Exception in run: workers survives.
             */

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = MyThrowableType.EXCEPTION,
                        throwableInOnCancel = null);
                scheduler.execute(runnable);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);

                // Letting time to wrapping runnable to call worker's runnable again.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                assertEquals(initialNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = MyThrowableType.EXCEPTION,
                        throwableInOnCancel = null);
                scheduler.executeAtNs(runnable, clock.getTimeNs());
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);

                // Letting time to wrapping runnable to call worker's runnable again.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                assertEquals(initialNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }

            {
                final MyRunnable runnable = new MyRunnable(
                        clock,
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = MyThrowableType.EXCEPTION,
                        throwableInOnCancel = null);
                scheduler.executeAfterNs(runnable, 0);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);

                // Letting time to wrapping runnable to call worker's runnable again.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                assertEquals(initialNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_clocksListenerAddRemove() {
        final MyListenableClock clock = getListenableClockForTest();
        final HardScheduler scheduler = HardScheduler.newSingleThreadedInstance(clock, "SCHEDULER_0", true);

        // Not added by constructor.
        assertEquals(0, clock.listeners.size());

        // Removed after wait.
        try {
            scheduler.waitForNoMoreRunningWorkerSystemTimeNs(REAL_TIME_TOLERANCE_NS);
        } catch (InterruptedException ignore) {
        }
        assertEquals(0, clock.listeners.size());

        // Added when worker threads start.
        scheduler.startWorkerThreadsIfNeeded();
        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);
        assertEquals(1, clock.listeners.size());

        // Not removed after wait, since worker threads are still running.
        try {
            scheduler.waitForNoMoreRunningWorkerSystemTimeNs(REAL_TIME_TOLERANCE_NS);
        } catch (InterruptedException ignore) {
        }
        assertEquals(1, clock.listeners.size());

        // Removed when worker threads die.
        scheduler.shutdown();
        // Letting time for workers to die.
        sleepMS(REAL_TIME_TOLERANCE_MS);
        assertEquals(0, clock.listeners.size());
        
        shutdownNowAndWait(scheduler);
    }

    public void test_queuesCapacity() {
        final InterfaceHardClock clock = getClockForTest();

        final int nbrOfThreads = 1;
        final int asapQueueCapacity = 2;
        final int timedQueueCapacity = 3;

        final ArrayList<HardScheduler> schedulerList = createNThreadsSchedulers(
                clock,
                nbrOfThreads,
                asapQueueCapacity,
                timedQueueCapacity,
                null);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        MyRunnable runnableTmp;

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            // Filling ASAP queue.
            for (int i = 0; i < asapQueueCapacity; i++) {
                runnableTmp = new MyRunnable(clock);
                scheduler.execute(runnableTmp);
                assertFalse(runnableTmp.runCalled());
                assertFalse(runnableTmp.onCancelCalled());
            }

            // Checking new ASAP schedules are rejected.
            runnableTmp = new MyRunnable(clock);
            scheduler.execute(runnableTmp);
            assertFalse(runnableTmp.runCalled());
            assertTrue(runnableTmp.onCancelCalled());

            // Filling timed queue.
            long nowNs = clock.getTimeNs();
            for (int i = 0; i < timedQueueCapacity; i++) {
                runnableTmp = new MyRunnable(clock);
                scheduler.executeAtNs(runnableTmp, nowNs);
                assertFalse(runnableTmp.runCalled());
                assertFalse(runnableTmp.onCancelCalled());
            }

            // Checking new timed schedules are rejected.
            runnableTmp = new MyRunnable(clock);
            scheduler.executeAtNs(runnableTmp, nowNs);
            assertFalse(runnableTmp.runCalled());
            assertTrue(runnableTmp.onCancelCalled());
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /**
     * Testing no schedule is lost when stressing scheduling and processing start and stop.
     */
    public void test_schedulingStress() {
        final InterfaceHardClock clock = getClockForTest();

        final int nbrOfThreads = 3;

        final ArrayList<HardScheduler> schedulerList = createNThreadsSchedulers(
                clock,
                nbrOfThreads);

        final Random random = TestUtils.newRandom123456789L();

        for (final HardScheduler scheduler : schedulerList) {

            // Multiple runs, for smaller queues, and higher chance of messy scheduling.
            final int nbrOfRun = 50;
            for (int k = 0; k < nbrOfRun; k++) {

                final int nbrOfCallsPerThread = 1000;
                final int nbrOfCalls = nbrOfCallsPerThread * nbrOfThreads;

                final ArrayList<MyRunnable> runnables = new ArrayList<MyRunnable>(nbrOfCalls);
                for (int i = 0; i < nbrOfCalls; i++) {
                    runnables.add(new MyRunnable(clock));
                }

                final AtomicInteger runnableIndex = new AtomicInteger();

                final ExecutorService executor = Executors.newCachedThreadPool();
                for (int t = 0; t < nbrOfThreads; t++) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < nbrOfCallsPerThread; i++) {
                                final MyRunnable runnable = runnables.get(runnableIndex.getAndIncrement());

                                final int uniform_0_3 = random.nextInt(4);

                                if (uniform_0_3 <= 1) {
                                    scheduler.execute(runnable);
                                } else {
                                    scheduler.executeAtNs(runnable, clock.getTimeNs());
                                }

                                if ((uniform_0_3&1) == 0) {
                                    scheduler.startProcessing();
                                } else {
                                    scheduler.stopProcessing();
                                }
                            }
                            // Making sure we don't end up with processing stopped.
                            scheduler.startProcessing();
                        }
                    });
                }
                Unchecked.shutdownAndAwaitTermination(executor);

                // Checking all runnables have been processed.
                for (MyRunnable runnable : runnables) {
                    runnable.waitAndGetReport();
                    assertTrue(runnable.runCalled());
                    assertFalse(runnable.runInterrupted);
                    assertFalse(runnable.onCancelCalled());
                }
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    /**
     * Calls all methods concurrently, no exception should be thrown.
     */
    public void test_allConcurrentCalls() {
        final InterfaceHardClock clock = getClockForTest();

        final int nbrOfThreads = 3;
        final int nbrOfCallsPerThread = 100;
        // Multiple runs, for smaller queues, and higher chance of messy scheduling.
        final int nbrOfRun = 50;

        final ArrayList<HardScheduler> schedulerList = createNThreadsSchedulers(
                clock,
                nbrOfThreads);

        final Random random = TestUtils.newRandom123456789L();

        for (final HardScheduler scheduler : schedulerList) {

            ExecutorService executor = Executors.newCachedThreadPool();
            
            for (int k = 0; k < nbrOfRun; k++) {
                for (int t = 0; t < nbrOfThreads; t++) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < nbrOfCallsPerThread; i++) {
                                /*
                                 * Not calling:
                                 * - toString() and appendString(StringBuilder)
                                 * - setMaxSystemWaitTimeNs(long)
                                 * - getClock()
                                 * - start() and stop() (redundant with others)
                                 * - shutdown() (dead end)
                                 * - waitForNoMoreRunningWorkerClockTimeNs(long)
                                 * - waitForNoMoreRunningWorkerSystemTimeNs(long)
                                 */
                                final int nbrOfMethods = 20;
                                final int uniform_1_N = 1 + random.nextInt(nbrOfMethods);
                                switch (uniform_1_N) {
                                case 1:
                                    scheduler.getNbrOfRunningWorkers();
                                    break;
                                case 2:
                                    scheduler.getNbrOfIdleWorkers();
                                    break;
                                case 3:
                                    scheduler.getNbrOfWorkingWorkers();
                                    break;
                                case 4:
                                    scheduler.getNbrOfPendingSchedules();
                                    break;
                                case 5:
                                    scheduler.getNbrOfPendingAsapSchedules();
                                    break;
                                case 6:
                                    scheduler.getNbrOfPendingTimedSchedules();
                                    break;
                                case 7:
                                    scheduler.startWorkerThreadsIfNeeded();
                                    break;
                                case 8:
                                    scheduler.startAccepting();
                                    break;
                                case 9:
                                    scheduler.stopAccepting();
                                    break;
                                case 10:
                                    scheduler.startProcessing();
                                    break;
                                case 11:
                                    scheduler.stopProcessing();
                                    break;
                                case 12:
                                    scheduler.cancelPendingSchedules();
                                    break;
                                case 13:
                                    scheduler.cancelPendingAsapSchedules();
                                    break;
                                case 14:
                                    scheduler.cancelPendingTimedSchedules();
                                    break;
                                case 15:
                                    scheduler.drainPendingAsapRunnablesInto(new ArrayList<Object>());
                                    break;
                                case 16:
                                    scheduler.drainPendingTimedRunnablesInto(new ArrayList<Object>());
                                    break;
                                case 17:
                                    scheduler.interruptWorkers();
                                    break;
                                case 18:
                                    scheduler.execute(new MyRunnable(clock));
                                    break;
                                case 19:
                                    scheduler.executeAtNs(new MyRunnable(clock), clock.getTimeNs());
                                    break;
                                case 20:
                                    scheduler.executeAfterNs(new MyRunnable(clock), 1L);
                                    break;
                                default:
                                    throw new AssertionError(uniform_1_N);
                                }
                            }
                            // Making sure we don't end up with processing stopped.
                            scheduler.startProcessing();
                        }
                    });
                }
                // Waiting for pending schedules to be processed or canceled.
                while (scheduler.getNbrOfPendingSchedules() != 0) {
                    sleepMS(REAL_TIME_TOLERANCE_MS);
                }
            }
            Unchecked.shutdownAndAwaitTermination(executor);
        }
        
        shutdownNowAndWait(schedulerList);
    }
    
    /*
     * InterfaceWorkerAwareScheduler
     */
    
    public void test_isWorkerThread_andChecks_withThreads() {
        final InterfaceHardClock clock = getClockForTest();
        final int nbrOfThreads = 2;
        final ArrayList<HardScheduler> schedulerList = createNThreadsSchedulers(
                clock,
                nbrOfThreads);

        for (final HardScheduler scheduler : schedulerList) {
            
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
            
            for (boolean asapElseTimed : new boolean[]{false,true}) {
                final AtomicInteger res = new AtomicInteger();
                final MonitorCondilock condilock = new MonitorCondilock();
                final MyRunnable checkingRunnable = new MyRunnable(clock) {
                    @Override
                    public void run() {
                        res.set(scheduler.isWorkerThread() ? 1 : -1);
                        
                        try {
                            scheduler.checkIsWorkerThread();
                            // ok
                        } catch (Throwable t) {
                            res.set(2); // bad value
                        }
                        try {
                            scheduler.checkIsNotWorkerThread();
                            res.set(3); // bad value
                        } catch (IllegalStateException e) {
                            // ok
                        }

                        condilock.signalAllInLock();
                    }
                };
                if (asapElseTimed) {
                    scheduler.execute(checkingRunnable);
                } else {
                    scheduler.executeAtNs(checkingRunnable, clock.getTimeNs());
                }
                condilock.awaitWhileFalseInLockUninterruptibly(new InterfaceBooleanCondition() {
                    @Override
                    public boolean isTrue() {
                        return res.get() != 0;
                    }
                });
                
                assertEquals(1, res.get());
            }
        }
        
        shutdownNowAndWait(schedulerList);
    }

    public void test_isWorkerThread_andChecks_threadless() {
        final InterfaceHardClock clock = getClockForTest();
        final HardScheduler scheduler = HardScheduler.newThreadlessInstance(clock);
        
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
        
        final MyRunnable checkingRunnable = new MyRunnable(clock) {
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

        // Delay large enough for it to occur after
        // checking runnables (ASAP schedules having
        // priority over timed ones).
        scheduler.executeAfterNs(new Runnable() {
            public void run() {
                scheduler.shutdown();
            }
        }, REAL_TIME_TOLERANCE_NS);

        scheduler.startAndWorkInCurrentThread();

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
        
        shutdownNowAndWait(scheduler);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /*
     * 
     */
    
    private static void shutdownNowAndWait(List<HardScheduler> schedulerList) {
        for (HardScheduler scheduler : schedulerList) {
            shutdownNowAndWait(scheduler);
        }
    }

    private static void shutdownNowAndWait(HardScheduler scheduler) {
        /*
         * TODO For some enigmaticalistiquesquish reason,
         * interrupting formerly tested scheduler's workers,
         * causes interruption of workers of subsequent tests,
         * before we try to shut them down, as if the JVM
         * was reusing OS threads for new Java threads,
         * if that makes sense.
         * As a result, we don't interrupt here.
         */
        final boolean mustInterruptWorkingWorkers = false;
        scheduler.shutdownNow(mustInterruptWorkingWorkers);
        try {
            scheduler.waitForNoMoreRunningWorkerSystemTimeNs(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * clocks based on nanoTime, not to have trouble with system time jumps
     */

    private static EnslavedControllableHardClock getClockForTest() {
        return new EnslavedControllableHardClock(new NanoTimeClock());
    }

    private static MyListenableClock getListenableClockForTest() {
        return new MyListenableClock(new NanoTimeClock());
    }

    /*
     * 
     */

    /**
     * These schedulers ensure FIFO order for ASAP schedules
     * and for timed schedules for a same time.
     */
    private static ArrayList<HardScheduler> createFifoSchedulers(
            final InterfaceHardClock clock) {
        return createNThreadsSchedulers(
                clock,
                1);
    }

    /**
     * @param nbrOfThreads Number of threads for schedulers.
     */
    private static ArrayList<HardScheduler> createNThreadsSchedulers(
            final InterfaceHardClock clock,
            int nbrOfThreads) {
        final ThreadFactory threadFactory = new MyThreadFactory();
        return createNThreadsSchedulers(
                clock,
                nbrOfThreads,
                threadFactory);
    }

    private static ArrayList<HardScheduler> createNThreadsSchedulers(
            final InterfaceHardClock clock,
            int nbrOfThreads,
            final ThreadFactory threadFactory) {
        return createNThreadsSchedulers(
                clock,
                nbrOfThreads,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                threadFactory);
    }

    private static ArrayList<HardScheduler> createNThreadsSchedulers(
            final InterfaceHardClock clock,
            int nbrOfThreads,
            int asapQueueCapacity,
            int timedQueueCapacity,
            final ThreadFactory threadFactory) {
        final boolean daemon = true;
        final HardScheduler scheduler1 = HardScheduler.newInstance(
                clock,
                "SCHEDULER_1",
                daemon,
                nbrOfThreads,
                asapQueueCapacity,
                timedQueueCapacity,
                threadFactory);
        /*
         * NB: After features simplifications,
         * we no (longer) have another breed of hard scheduler to create,
         * but we keep current method in case we add one back in the future.
         */

        final ArrayList<HardScheduler> schedulerList = new ArrayList<HardScheduler>();
        schedulerList.add(scheduler1);

        return schedulerList;
    }

    private static void sleepMS(long ms) {
        Unchecked.sleepMs(ms);
    }
}
