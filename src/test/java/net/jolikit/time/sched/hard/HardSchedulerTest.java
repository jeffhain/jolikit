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
import net.jolikit.threading.locks.MonitorCondilock;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.InterfaceClockModificationListener;
import net.jolikit.time.clocks.hard.EnslavedControllableHardClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.hard.NanoTimeClock;
import net.jolikit.time.clocks.hard.SystemTimeClock;
import net.jolikit.time.sched.InterfaceSchedulable;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceScheduling;

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
        private final long theoreticalTimeNs;
        private final long actualTimeNs;
        /**
         * 1 for first schedulable, etc.
         * Only considers last process call.
         */
        private final long orderNum;
        public MySchedulingReport(
                long theoreticalTimeNs,
                long actualTimeNs,
                long orderNum) {
            this.theoreticalTimeNs = theoreticalTimeNs;
            this.actualTimeNs = actualTimeNs;
            this.orderNum = orderNum;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[theoreticalTimeNs=");
            sb.append(theoreticalTimeNs);
            sb.append(",actualTimeNs=");
            sb.append(actualTimeNs);
            sb.append(",orderNum=");
            sb.append(orderNum);
            sb.append("]");
            return sb.toString();
        }
    }

    private class MySchedulable implements InterfaceSchedulable {
        private InterfaceScheduling scheduling;
        private final Object reportMutex = new Object();
        /**
         * Report for last process call.
         */
        private volatile MySchedulingReport report;
        private volatile int nbrOfRunCalls;
        private volatile int nbrOfOnCancelCalls;
        private int nbrOfReschedules;
        private final long sleepTimeMsInRun;
        private final long sleepTimeMsInOnCancel;
        private final MyThrowableType throwableInRun;
        private final MyThrowableType throwableInOnCancel;
        private volatile boolean runInterrupted;
        private volatile boolean onCancelInterrupted;
        public MySchedulable() {
            this(0);
        }
        public MySchedulable(long sleepTimeMsInRun) {
            this(
                    0,
                    sleepTimeMsInRun);
        }
        public MySchedulable(
                int nbrOfReschedules,
                long sleepTimeMsInRun) {
            this(
                    nbrOfReschedules,
                    sleepTimeMsInRun,
                    0);
        }
        public MySchedulable(
                int nbrOfReschedules,
                long sleepTimeMsInRun,
                long sleepTimeMsInOnCancel) {
            this(
                    nbrOfReschedules,
                    sleepTimeMsInRun,
                    sleepTimeMsInOnCancel,
                    null,
                    null);
        }
        public MySchedulable(
                int nbrOfReschedules,
                long sleepTimeMsInRun,
                long sleepTimeMsInOnCancel,
                final MyThrowableType throwableInRun,
                final MyThrowableType throwableInOnCancel) {
            this.nbrOfReschedules = nbrOfReschedules;
            this.sleepTimeMsInRun = sleepTimeMsInRun;
            this.sleepTimeMsInOnCancel = sleepTimeMsInOnCancel;
            this.throwableInRun = throwableInRun;
            this.throwableInOnCancel = throwableInOnCancel;
        }
        @Override
        public void setScheduling(InterfaceScheduling scheduling) {
            this.scheduling = scheduling;
        }
        @Override
        public void run() {
            final InterfaceScheduling scheduling = this.scheduling;
            final long theoreticalTimeNs = scheduling.getTheoreticalTimeNs();
            final long actualTimeNs = scheduling.getActualTimeNs();

            this.nbrOfRunCalls++;

            final boolean mustReport =
                    (this.nbrOfReschedules == 0)
                    || (this.throwableInRun != null);
            try {
                if (this.sleepTimeMsInRun > 0) {
                    try {
                        Thread.sleep(this.sleepTimeMsInRun);
                    } catch (InterruptedException e) {
                        this.runInterrupted = true;
                    }
                }

                throwIfNeeded(this.throwableInRun);

                if (this.nbrOfReschedules > 0) {
                    this.nbrOfReschedules--;
                    scheduling.setNextTheoreticalTimeNs(actualTimeNs);
                }
            } finally {
                if (mustReport) {
                    final long orderNum = counter.incrementAndGet();

                    this.report = new MySchedulingReport(
                            theoreticalTimeNs,
                            actualTimeNs,
                            orderNum);
                    synchronized (reportMutex) {
                        reportMutex.notify();
                    }
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
                        0,
                        0,
                        0);
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

    private final Random random = new Random(123456789L);
    
    private final AtomicLong counter = new AtomicLong();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_toString() {
        HardScheduler scheduler = HardScheduler.newSingleThreadedInstance(new SystemTimeClock(), "", true);

        assertNotNull(scheduler.toString());
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
            final MySchedulable schedulable = new MySchedulable();
            final long startTimeNs = clock.getTimeNs();
            final long timeJumpNs = maxSystemWaitTimeNs + TimeUtils.sToNs(10.0);
            final long jumpedTimeNs = startTimeNs + timeJumpNs;
            scheduler.executeAtNs(schedulable, jumpedTimeNs);
            // Letting time for schedule wait to start.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            // Setting clock's time to theoretical time (plus the time
            // we waited for the schedule wait to start), i.e. ideally
            // the scheduler should stop to wait right after, but it
            // won't be notified on this modification since the clock
            // is not listenable.
            backingClock.setTimeNs(jumpedTimeNs + REAL_TIME_TOLERANCE_NS);
            final MySchedulingReport report = schedulable.waitAndGetReport();
            assertEquals(TimeUtils.nsToS(jumpedTimeNs), TimeUtils.nsToS(report.theoreticalTimeNs), REAL_TIME_TOLERANCE_S);
            assertEquals(TimeUtils.nsToS(jumpedTimeNs + maxSystemWaitTimeNs), TimeUtils.nsToS(report.actualTimeNs), REAL_TIME_TOLERANCE_S);
        }
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
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {

            int expectedNbrOfAliveWorkers = scheduler.getNbrOfRunningWorkers();

            {
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = MyThrowableType.ERROR,
                        throwableInOnCancel = null);
                scheduler.execute(schedulable);
                schedulable.waitAndGetReport();
                assertEquals(1, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);
                // Letting time for worker thread to die.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                expectedNbrOfAliveWorkers--;
                assertEquals(expectedNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }

            {
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = MyThrowableType.ERROR,
                        throwableInOnCancel = null);
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                schedulable.waitAndGetReport();
                assertEquals(1, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);
                // Letting time for worker thread to die.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                expectedNbrOfAliveWorkers--;
                assertEquals(expectedNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }
        }
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
                final MySchedulable schedulable = new MySchedulable(2 * REAL_TIME_TOLERANCE_MS);
                scheduler.execute(schedulable);
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers-1, scheduler.getNbrOfIdleWorkers());
                // Waiting for process to finish.
                schedulable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers, scheduler.getNbrOfIdleWorkers());
            }

            {
                final MySchedulable schedulable = new MySchedulable(2 * REAL_TIME_TOLERANCE_MS);
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers-1, scheduler.getNbrOfIdleWorkers());
                // Waiting for process to finish.
                schedulable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers, scheduler.getNbrOfIdleWorkers());
            }
        }
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
                final MySchedulable schedulable = new MySchedulable(2 * REAL_TIME_TOLERANCE_MS);
                scheduler.execute(schedulable);
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, scheduler.getNbrOfWorkingWorkers());
                // Waiting for process to finish.
                schedulable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(0, scheduler.getNbrOfWorkingWorkers());
            }

            {
                final MySchedulable schedulable = new MySchedulable(2 * REAL_TIME_TOLERANCE_MS);
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, scheduler.getNbrOfWorkingWorkers());
                // Waiting for process to finish.
                schedulable.waitAndGetReport();
                // Letting time for scheduler to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(0, scheduler.getNbrOfWorkingWorkers());
            }
        }
    }

    public void test_getNbrOfPendingSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            assertEquals(0, scheduler.getNbrOfPendingSchedules());

            final MySchedulable schedulable1 = new MySchedulable();
            scheduler.execute(schedulable1);
            assertEquals(1, scheduler.getNbrOfPendingSchedules());

            final MySchedulable schedulable2 = new MySchedulable();
            scheduler.executeAtNs(schedulable2, clock.getTimeNs());
            assertEquals(2, scheduler.getNbrOfPendingSchedules());

            scheduler.startProcessing();

            // Waiting for schedulables to be processed.
            schedulable1.waitAndGetReport();
            schedulable2.waitAndGetReport();

            assertEquals(0, scheduler.getNbrOfPendingSchedules());
        }
    }

    public void test_getNbrOfPendingAsapSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            final MySchedulable schedulable1 = new MySchedulable();
            scheduler.execute(schedulable1);
            assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            final MySchedulable schedulable2 = new MySchedulable();
            scheduler.execute(schedulable2);
            assertEquals(2, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            scheduler.startProcessing();

            // Waiting for schedulables to be processed.
            schedulable1.waitAndGetReport();
            schedulable2.waitAndGetReport();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
        }
    }

    public void test_getNbrOfPendingTimedSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

            final MySchedulable schedulable1 = new MySchedulable();
            scheduler.executeAtNs(schedulable1, clock.getTimeNs());
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

            final MySchedulable schedulable2 = new MySchedulable();
            scheduler.executeAtNs(schedulable2, clock.getTimeNs());
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(2, scheduler.getNbrOfPendingTimedSchedules());

            scheduler.startProcessing();

            // Waiting for schedulables to be processed.
            schedulable1.waitAndGetReport();
            schedulable2.waitAndGetReport();

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
        }
    }

    public void test_isWorkersDeathRequested() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            assertFalse(scheduler.isShutdown());
            scheduler.shutdown();
            assertTrue(scheduler.isShutdown());
        }
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
                final MySchedulable schedulable = new MySchedulable();
                scheduler.execute(schedulable);
                final MySchedulingReport report = schedulable.waitAndGetReport();
                // Processed right away.
                assertEquals(startTimeNs, report.actualTimeNs, REAL_TIME_TOLERANCE_NS);
            }

            {
                final MySchedulable schedulable = new MySchedulable();
                scheduler.executeAtNs(schedulable, startTimeNs);
                final MySchedulingReport report = schedulable.waitAndGetReport();
                // Processed right away.
                assertEquals(startTimeNs, report.actualTimeNs, REAL_TIME_TOLERANCE_NS);
            }
        }
    }

    public void test_stop() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stop();

            // Checking schedules are not processed, and not accepted.

            {
                final MySchedulable schedulable = new MySchedulable();
                scheduler.execute(schedulable);
                // letting time for processing (not) to occur
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(schedulable.runCalled());
                assertTrue(schedulable.onCancelCalled());
            }

            {
                final MySchedulable schedulable = new MySchedulable();
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                // letting time for processing (not) to occur
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(schedulable.runCalled());
                assertTrue(schedulable.onCancelCalled());
            }

            // Checking schedules were really not accepted,
            // i.e. didn't make it into pending schedules.
            // Supposes getNbrOfXXX methods work.

            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
        }
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
                final MySchedulable schedulable = new MySchedulable();
                scheduler.execute(schedulable);
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(schedulable.runCalled());
                assertFalse(schedulable.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
            }

            {
                final MySchedulable schedulable = new MySchedulable();
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(schedulable.runCalled());
                assertFalse(schedulable.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
            }
        }
    }

    public void test_stopAccepting() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {
            // Checking schedules are not accepted,
            // and pending schedules are processed.

            // To test timed schedules are still processed
            // when new ones are not accepted.
            // This schedulable will be processed in a few,
            // but just once, since re-schedule shall be refused.
            final MySchedulable toBeProcessedSchedulable = new MySchedulable(
                    nbrOfReschedules = 1,
                    sleepTimeMsInProcess = 0,
                    sleepTimeMsInOnCancel = 0,
                    throwableInProcess = null,
                    throwableInOnCancel = null);
            final long theoreticalTimeNs = clock.getTimeNs() + TimeUtils.sToNs(1.0);
            scheduler.executeAtNs(toBeProcessedSchedulable, theoreticalTimeNs);
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

            // To test ASAP schedules are still processed
            // when new ones are not accepted.
            // Large enough to still have pending ASAP schedules
            // when acceptance gets stopped.
            final int nbrOfAsapSchedules = 4;
            final int sleepTimeMsInRun = 10;
            for (int i = 0; i < nbrOfAsapSchedules; i++) {
                scheduler.execute(new MySchedulable(sleepTimeMsInRun));
            }

            scheduler.stopAccepting();

            final int nbrOfAsapSchedulesAfterStopAcception = scheduler.getNbrOfPendingAsapSchedules();
            assertTrue(nbrOfAsapSchedulesAfterStopAcception != 0);
            // Letting time for ASAP schedules to be processed.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());

            {
                final MySchedulable schedulable = new MySchedulable();
                scheduler.execute(schedulable);
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(schedulable.runCalled());
                assertTrue(schedulable.onCancelCalled());
            }

            {
                final MySchedulable schedulable = new MySchedulable();
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(schedulable.runCalled());
                assertTrue(schedulable.onCancelCalled());
            }

            // Checking schedules were really not accepted,
            // i.e. didn't make it into pending schedules.
            // Supposes getNbrOfXXX methods work.
            assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

            // Checking timed schedule has been processed,
            // but not its re-schedule.
            toBeProcessedSchedulable.waitAndGetReport();
            assertEquals(1, toBeProcessedSchedulable.nbrOfRunCalls);
            assertTrue(toBeProcessedSchedulable.onCancelCalled());
        }
    }

    public void test_startProcessing() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {
            {
                // stopProcessing is supposed tested:
                // we check startProcessing gets things back up.
                scheduler.stopProcessing();

                final MySchedulable schedulable = new MySchedulable();
                scheduler.execute(schedulable);

                // Letting time for schedulable (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                // Schedulable should start to be processed after that call.
                scheduler.startProcessing();

                schedulable.waitAndGetReport();
                assertTrue(schedulable.runCalled());
                assertFalse(schedulable.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            }

            {
                // stopProcessing is supposed tested:
                // we check startProcessing gets things back up.
                scheduler.stopProcessing();

                final MySchedulable schedulable = new MySchedulable();
                scheduler.executeAtNs(schedulable, clock.getTimeNs());

                // Letting time for schedulable (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                // Schedulable should start to be processed after that call.
                scheduler.startProcessing();

                schedulable.waitAndGetReport();
                assertTrue(schedulable.runCalled());
                assertFalse(schedulable.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }
        }
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

                final MySchedulable schedulable1 = new MySchedulable();
                scheduler.execute(schedulable1);

                final MySchedulable schedulable2 = new MySchedulable();
                scheduler.executeAtNs(schedulable2, clock.getTimeNs());

                // Letting time for schedulables (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
                assertFalse(schedulable1.runCalled());
                assertFalse(schedulable1.onCancelCalled());
                assertFalse(schedulable2.runCalled());
                assertFalse(schedulable2.onCancelCalled());
            }
        }

        /*
         * Stopping processing when scheduling is going on.
         */

        {
            final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);
            
            for (HardScheduler scheduler : schedulerList) {

                // Schedulables that wait for a bit, and enough
                // of them for pending schedules queues to contain
                // a few of them before we start to sleep.
                final int nbrOfSchedules = 10;
                final int sleepTimeMsInRun = 10;
                for (int i = 0; i < nbrOfSchedules; i++) {
                    MySchedulable schedulable1 = new MySchedulable(sleepTimeMsInRun);
                    scheduler.execute(schedulable1);
                    MySchedulable schedulable2 = new MySchedulable(sleepTimeMsInRun);
                    scheduler.executeAtNs(schedulable2, clock.getTimeNs());
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
        }
        
        /*
         * Stopping processing from within a schedulable execution,
         * which should NOT cause the next schedule to be cancelled,
         * just remain pending.
         */

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);
        
        for (final HardScheduler scheduler : schedulerList) {

            final int nbrOfReschedules = 1;
            final long sleepTimeMsInRun = 0;
            final MySchedulable schedulable = new MySchedulable(
                    nbrOfReschedules,
                    sleepTimeMsInRun) {
                @Override
                public void run() {
                    super.run();

                    scheduler.stopProcessing();
                }
            };

            scheduler.execute(schedulable);

            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(0, schedulable.nbrOfOnCancelCalls);
            assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
        }
    }

    public void test_cancelPendingSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {
            // Supposing stopProcessing works.
            scheduler.stopProcessing();

            /*
             * Canceling pending schedules right after scheduling.
             */

            {
                final MySchedulable schedulable1 = new MySchedulable();
                scheduler.execute(schedulable1);
                final MySchedulable schedulable2 = new MySchedulable();
                scheduler.executeAtNs(schedulable2, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                scheduler.cancelPendingSchedules();

                assertFalse(schedulable1.runCalled());
                assertFalse(schedulable2.runCalled());

                assertTrue(schedulable1.onCancelCalled());
                assertTrue(schedulable2.onCancelCalled());

                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }

            /*
             * Testing schedules cancellation stops if one throws an exception.
             */

            {
                // Getting non-throwing schedulables surrounded
                // with throwing ones, to handle different cases
                // of iteration.
                final MySchedulable schedulableExceptionAsap1 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(schedulableExceptionAsap1);
                final MySchedulable schedulableExceptionTimed1 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(schedulableExceptionTimed1, clock.getTimeNs());

                final MySchedulable schedulableAsap = new MySchedulable();
                scheduler.execute(schedulableAsap);
                final MySchedulable schedulableTimed = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

                final MySchedulable schedulableExceptionAsap2 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(schedulableExceptionAsap2);
                final MySchedulable schedulableExceptionTimed2 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(schedulableExceptionTimed2, clock.getTimeNs());

                assertEquals(3, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(3, scheduler.getNbrOfPendingTimedSchedules());

                try {
                    scheduler.cancelPendingSchedules();
                    fail();
                } catch (Exception e) {
                    // ok
                }

                assertFalse(schedulableAsap.runCalled());
                assertFalse(schedulableTimed.runCalled());
                assertFalse(schedulableExceptionAsap1.runCalled());
                assertFalse(schedulableExceptionAsap2.runCalled());
                assertFalse(schedulableExceptionTimed1.runCalled());
                assertFalse(schedulableExceptionTimed2.runCalled());

                assertFalse(schedulableAsap.onCancelCalled() && schedulableExceptionAsap1.onCancelCalled() && schedulableExceptionAsap2.onCancelCalled());
                assertFalse(schedulableTimed.onCancelCalled() && schedulableExceptionTimed1.onCancelCalled() && schedulableExceptionTimed2.onCancelCalled());

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
    }

    public void test_cancelPendingAsapSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {

            /*
             * Canceling pending ASAP schedules right after scheduling.
             */

            {
                scheduler.stopProcessing();

                final MySchedulable schedulableAsap = new MySchedulable();
                scheduler.execute(schedulableAsap);
                final MySchedulable schedulableTimed = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                scheduler.cancelPendingAsapSchedules();

                assertFalse(schedulableAsap.runCalled());
                assertTrue(schedulableAsap.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());

                // Timed schedule not canceled.
                assertFalse(schedulableTimed.runCalled());
                assertFalse(schedulableTimed.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                // Starting processing for timed schedule.
                scheduler.startProcessing();

                // Waiting for timed schedule to be processed.
                schedulableTimed.waitAndGetReport();

                assertTrue(schedulableTimed.runCalled());
                assertFalse(schedulableTimed.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }

            /*
             * Testing schedules cancellation stops if one throws an exception.
             */

            {
                scheduler.stopProcessing();

                // Getting non-throwing ASAP schedulable surrounded
                // with throwing ones, to handle different cases
                // of iteration.
                final MySchedulable schedulableExceptionAsap1 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(schedulableExceptionAsap1);

                final MySchedulable schedulableAsap = new MySchedulable();
                scheduler.execute(schedulableAsap);
                final MySchedulable schedulableTimed = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

                final MySchedulable schedulableExceptionAsap2 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.execute(schedulableExceptionAsap2);

                assertEquals(3, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                try {
                    scheduler.cancelPendingAsapSchedules();
                    fail();
                } catch (Exception e) {
                    // ok
                }

                assertFalse(schedulableAsap.runCalled());
                assertFalse(schedulableExceptionAsap1.runCalled());
                assertFalse(schedulableExceptionAsap2.runCalled());

                assertFalse(schedulableAsap.onCancelCalled() && schedulableExceptionAsap1.onCancelCalled() && schedulableExceptionAsap2.onCancelCalled());

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
                schedulableTimed.waitAndGetReport();

                assertTrue(schedulableTimed.runCalled());
                assertFalse(schedulableTimed.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());
            }
        }
    }

    public void test_cancelPendingTimedSchedules() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {

            /*
             * Canceling pending timed schedules right after scheduling.
             */

            {
                scheduler.stopProcessing();

                final MySchedulable schedulableAsap = new MySchedulable();
                scheduler.execute(schedulableAsap);
                final MySchedulable schedulableTimed = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                scheduler.cancelPendingTimedSchedules();

                assertFalse(schedulableTimed.runCalled());
                assertTrue(schedulableTimed.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

                // ASAP schedule not canceled.
                assertFalse(schedulableAsap.runCalled());
                assertFalse(schedulableAsap.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());

                // Starting processing for ASAP schedule.
                scheduler.startProcessing();

                // Waiting for ASAP schedule to be processed.
                schedulableAsap.waitAndGetReport();

                assertTrue(schedulableAsap.runCalled());
                assertFalse(schedulableAsap.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            }

            /*
             * Testing schedules cancellation stops if one throws an exception.
             */

            {
                scheduler.stopProcessing();

                // Getting non-throwing timed schedulable surrounded
                // with throwing ones, to handle different cases
                // of iteration.
                final MySchedulable schedulableExceptionTimed1 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(schedulableExceptionTimed1, clock.getTimeNs());

                final MySchedulable schedulableAsap = new MySchedulable();
                scheduler.execute(schedulableAsap);
                final MySchedulable schedulableTimed = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

                final MySchedulable schedulableExceptionTimed2 = new MySchedulable(
                        nbrOfReschedules = 0,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                scheduler.executeAtNs(schedulableExceptionTimed2, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(3, scheduler.getNbrOfPendingTimedSchedules());

                try {
                    scheduler.cancelPendingTimedSchedules();
                    fail();
                } catch (Exception e) {
                    // ok
                }

                assertFalse(schedulableTimed.runCalled());
                assertFalse(schedulableExceptionTimed1.runCalled());
                assertFalse(schedulableExceptionTimed2.runCalled());

                assertFalse(
                        schedulableTimed.onCancelCalled()
                        && schedulableExceptionTimed1.onCancelCalled()
                        && schedulableExceptionTimed2.onCancelCalled());

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
                schedulableAsap.waitAndGetReport();

                assertTrue(schedulableAsap.runCalled());
                assertFalse(schedulableAsap.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());
            }
        }
    }

    public void test_drainPendingAsapRunnablesInto_ObjectVector() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {

            /*
             * No schedulable.
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
             * Schedulables.
             */

            {
                scheduler.stopProcessing();

                final MySchedulable schedulableAsap1 = new MySchedulable();
                scheduler.execute(schedulableAsap1);
                final MySchedulable schedulableAsap2 = new MySchedulable();
                scheduler.execute(schedulableAsap2);
                final MySchedulable schedulableTimed = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

                assertEquals(2, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());

                final ArrayList<Object> runnables = new ArrayList<Object>();
                final Object foo = new Object();
                runnables.add(foo);
                scheduler.drainPendingAsapRunnablesInto(runnables);

                // Checking drained.
                assertEquals(3, runnables.size());
                assertEquals(foo, runnables.get(0));
                assertEquals(schedulableAsap1, runnables.get(1));
                assertEquals(schedulableAsap2, runnables.get(2));

                assertFalse(schedulableAsap1.runCalled());
                assertFalse(schedulableAsap1.onCancelCalled());
                assertFalse(schedulableAsap2.runCalled());
                assertFalse(schedulableAsap2.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingAsapSchedules());

                // Timed schedulable not drained.
                assertFalse(schedulableTimed.runCalled());
                assertFalse(schedulableTimed.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingTimedSchedules());
            }            
        }
    }

    public void test_drainPendingTimedRunnablesInto_ObjectVector() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (HardScheduler scheduler : schedulerList) {

            /*
             * No schedulable.
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
             * Schedulables.
             */

            {
                scheduler.stopProcessing();

                final MySchedulable schedulableAsap = new MySchedulable();
                scheduler.execute(schedulableAsap);
                final MySchedulable schedulableTimed1 = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed1, clock.getTimeNs());
                final MySchedulable schedulableTimed2 = new MySchedulable();
                scheduler.executeAtNs(schedulableTimed2, clock.getTimeNs());

                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
                assertEquals(2, scheduler.getNbrOfPendingTimedSchedules());

                final ArrayList<Object> runnables = new ArrayList<Object>();
                final Object foo = new Object();
                runnables.add(foo);
                scheduler.drainPendingTimedRunnablesInto(runnables);

                // Checking drained.
                assertEquals(3, runnables.size());
                assertEquals(foo, runnables.get(0));
                assertTrue(runnables.contains(schedulableTimed1));
                assertTrue(runnables.contains(schedulableTimed2));

                assertFalse(schedulableTimed1.runCalled());
                assertFalse(schedulableTimed1.onCancelCalled());
                assertFalse(schedulableTimed2.runCalled());
                assertFalse(schedulableTimed2.onCancelCalled());
                assertEquals(0, scheduler.getNbrOfPendingTimedSchedules());

                // ASAP schedulable not drained.
                assertFalse(schedulableAsap.runCalled());
                assertFalse(schedulableAsap.onCancelCalled());
                assertEquals(1, scheduler.getNbrOfPendingAsapSchedules());
            }            
        }
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

            final MySchedulable schedulable1 = new MySchedulable(Long.MAX_VALUE);
            scheduler.execute(schedulable1);
            final MySchedulable schedulable2 = new MySchedulable(Long.MAX_VALUE);
            scheduler.executeAtNs(schedulable2, clock.getTimeNs());

            // Letting time for schedulables to be called.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            // Schedulables being called.
            assertTrue(schedulable1.runCalled());
            assertTrue(schedulable2.runCalled());
            assertFalse(schedulable1.onCancelCalled());
            assertFalse(schedulable2.onCancelCalled());

            scheduler.interruptWorkers();

            // Letting time for interruption to propagate.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertTrue(schedulable1.runInterrupted);
            assertTrue(schedulable2.runInterrupted);
            assertFalse(schedulable1.onCancelCalled());
            assertFalse(schedulable2.onCancelCalled());
            assertFalse(schedulable1.onCancelInterrupted);
            assertFalse(schedulable2.onCancelInterrupted);
        }
        
        /*
         * Checking interrupting working workers works
         * even when scheduler is calling onCancel().
         */

        for (final HardScheduler scheduler : schedulerList) {

            final MySchedulable schedulable = new MySchedulable(
                    1,
                    0L,
                    Long.MAX_VALUE) {
                @Override
                public void run() {
                    super.run();
                    // Will cause re-schedule to be rejected,
                    // i.e. onCancel() called just after this call.
                    scheduler.stopAccepting();
                }
            };
            scheduler.execute(schedulable);

            // Letting time for run() to be called
            // and then onCancel() to start being called.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            
            assertTrue(schedulable.runCalled());
            assertTrue(schedulable.onCancelCalled());
            assertFalse(schedulable.runInterrupted);
            assertFalse(schedulable.onCancelInterrupted);

            scheduler.interruptWorkers();

            // Letting time for interruption to propagate.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertFalse(schedulable.runInterrupted);
            assertTrue(schedulable.onCancelInterrupted);
        }
    }

    public void test_shutdown() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (HardScheduler scheduler : schedulerList) {
            // Supposing stopProcessing works.
            scheduler.stopProcessing();

            /*
             * Getting some pending schedules, one ASAP,
             * and one timed that ask for one re-schedule
             * (to make sure the re-schedule is properly cancelled).
             */

            final MySchedulable schedulableAsap = new MySchedulable();
            scheduler.execute(schedulableAsap);

            final MySchedulable schedulableTimed = new MySchedulable(
                    nbrOfReschedules = 1,
                    sleepTimeMsInProcess = 0,
                    sleepTimeMsInOnCancel = 0,
                    throwableInProcess = null,
                    throwableInOnCancel = null);
            scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

            /*
             * Requesting workers death when idle.
             */

            scheduler.shutdown();

            // Making sure no more schedules are accepted.
            MySchedulable schedulableAsapRejected = new MySchedulable();
            scheduler.execute(schedulableAsapRejected);
            assertFalse(schedulableAsapRejected.runCalled());
            assertTrue(schedulableAsapRejected.onCancelCalled());
            //
            MySchedulable schedulableTimedRejected = new MySchedulable();
            scheduler.executeAtNs(schedulableTimedRejected, clock.getTimeNs());
            assertFalse(schedulableTimedRejected.runCalled());
            assertTrue(schedulableTimedRejected.onCancelCalled());

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
            schedulableAsap.waitAndGetReport();
            schedulableTimed.waitAndGetReport();

            // Checking correct schedules execution.
            assertTrue(schedulableAsap.runCalled());
            assertFalse(schedulableAsap.onCancelCalled());
            //
            assertEquals(1, schedulableTimed.nbrOfRunCalls);
            assertTrue(schedulableTimed.onCancelCalled());

            // Letting time for workers to die.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertEquals(0, scheduler.getNbrOfRunningWorkers());
            assertEquals(0, scheduler.getNbrOfIdleWorkers());
            assertEquals(0, scheduler.getNbrOfWorkingWorkers());
        }
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

            final MySchedulable schedulableAsap = new MySchedulable();
            final MySchedulable schedulableTimed = new MySchedulable();

            scheduler.stopProcessing();

            scheduler.execute(schedulableAsap);
            scheduler.executeAtNs(schedulableTimed, clock.getTimeNs());

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
            assertTrue(schedulableAsap.runCalled());
            assertFalse(schedulableAsap.onCancelCalled());
            assertTrue(schedulableTimed.runCalled());
            assertFalse(schedulableTimed.onCancelCalled());

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

            final ArrayList<MySchedulable> schedulableList = new ArrayList<MySchedulable>();

            final int nbrOfSchedulables = 100000;

            for (int i = 0; i < nbrOfSchedulables; i++) {
                final MySchedulable schedulable = new MySchedulable();
                schedulableList.add(schedulable);
                // clock not supposed to have any impact on ASAP scheduling
                // (to stop them, must stop scheduler)
                clock.setTimeNs(TimeUtils.sToNs(random.nextDouble()));
                clock.setTimeSpeed(random.nextDouble());
                scheduler.execute(schedulable);
            }

            for (int i = 0; i < nbrOfSchedulables; i++) {
                final MySchedulable schedulable = schedulableList.get(i);
                assertEquals(i+1,schedulable.waitAndGetReport().orderNum);
            }
        }
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

            final int nbrOfSchedulables = 100000;

            final ArrayList<MySchedulable> schedulableList = new ArrayList<MySchedulable>();

            for (int i = 0; i < nbrOfSchedulables; i++) {
                final MySchedulable schedulable = new MySchedulable();
                schedulableList.add(schedulable);
                if ((i&1) == 0) {
                    scheduler.executeAtNs(schedulable, nowNs);
                } else {
                    scheduler.executeAfterNs(schedulable, 0L);
                }
            }

            for (int i = 0; i < nbrOfSchedulables; i++) {
                final MySchedulable schedulable = schedulableList.get(i);
                assertEquals(i+1,schedulable.waitAndGetReport().orderNum);
            }
        }
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

            final MySchedulable s7 = new MySchedulable();
            scheduler.executeAtNs(s7, Long.MAX_VALUE);
            final MySchedulable s8 = new MySchedulable();
            scheduler.executeAtNs(s8, Long.MAX_VALUE);

            final MySchedulable s5 = new MySchedulable();
            scheduler.executeAtNs(s5, Long.MAX_VALUE - 1);
            final MySchedulable s6 = new MySchedulable();
            scheduler.executeAtNs(s6, Long.MAX_VALUE - 1);

            final MySchedulable s3 = new MySchedulable();
            scheduler.executeAtNs(s3, nowNs + 1L);
            final MySchedulable s4 = new MySchedulable();
            scheduler.executeAtNs(s4, nowNs + 1L);

            final MySchedulable s1 = new MySchedulable();
            scheduler.executeAtNs(s1, Long.MIN_VALUE);
            final MySchedulable s2 = new MySchedulable();
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

            MySchedulable schedulable;

            /*
             * ASAP
             */

            schedulable = new MySchedulable();
            scheduler.execute(schedulable);
            assertEquals(0L, schedulable.waitAndGetReport().theoreticalTimeNs);
            assertEquals(0L, schedulable.waitAndGetReport().actualTimeNs);

            /*
             * At time (now).
             */

            schedulable = new MySchedulable();
            scheduler.executeAtNs(schedulable,0L);

            assertEquals(0L, schedulable.waitAndGetReport().theoreticalTimeNs);
            assertEquals(0L, schedulable.waitAndGetReport().actualTimeNs);

            /*
             * At time (future).
             */

            schedulable = new MySchedulable();
            scheduler.executeAtNs(schedulable,1L);

            // Letting time for processing (not) to occur.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertFalse(schedulable.runCalled());

            // Clock change supposed to stop scheduler's wait.
            clock.setTimeNs(1L);
            assertEquals(1L, schedulable.waitAndGetReport().theoreticalTimeNs);
            assertEquals(1L, schedulable.waitAndGetReport().actualTimeNs);

            /*
             * At time (future) with multiple time changes.
             */

            schedulable = new MySchedulable();
            scheduler.executeAtNs(schedulable,1000L);

            clock.setTimeNs(500L);
            clock.setTimeNs(100L);
            clock.setTimeNs(900L);
            clock.setTimeNs(199L);
            clock.setTimeNs(999L);

            // Letting time for processing (not) to occur.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertFalse(schedulable.runCalled());

            clock.setTimeNs(1000L);
            assertEquals(1000L, schedulable.waitAndGetReport().theoreticalTimeNs);
            assertEquals(1000L, schedulable.waitAndGetReport().actualTimeNs);
        }
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
            final long durationNs = durationMs*1000L*1000L;
            final double halfTimeSpeed = 0.5;

            final long dummyWaitMs = durationMs/10;

            final MySchedulable schedulable = new MySchedulable();
            scheduler.executeAfterNs(schedulable, durationNs);
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

            final MySchedulingReport report = schedulable.waitAndGetReport();
            final long sysEndMs = System.currentTimeMillis();

            // Checking real time spent.
            assertEquals(durationMs/2 + 2*dummyWaitMs + durationMs, sysEndMs - sysStartMs, REAL_TIME_TOLERANCE_MS);
            // Checking scheduling theoretical time.
            assertEquals(startTimeNs+durationNs, report.theoreticalTimeNs);
            // Checking scheduling actual time.
            assertEquals(startTimeNs+durationNs, report.actualTimeNs, REAL_TIME_TOLERANCE_NS);
        }
    }

    /**
     * Testing priority of timed schedules over ASAP schedules.
     */
    public void test_timedSchedPriority() {
        final EnslavedControllableHardClock clock = getClockForTest();
        clock.setTimeSpeed(0.0);
        clock.setTimeNs(0L);

        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        for (InterfaceScheduler scheduler : schedulerList) {
            // Stopping scheduler while we setup schedules.
            ((HardScheduler) scheduler).stopProcessing();

            // Schedulables.
            final MySchedulable scheduledInPast = new MySchedulable();
            final MySchedulable scheduledAsap = new MySchedulable();
            final MySchedulable scheduledInFuture = new MySchedulable();
            scheduler.execute(scheduledAsap);
            scheduler.executeAtNs(scheduledInPast, clock.getTimeNs() - REAL_TIME_TOLERANCE_NS);
            scheduler.executeAtNs(scheduledInFuture, clock.getTimeNs() + REAL_TIME_TOLERANCE_NS);

            // Starting scheduler and clock.
            ((HardScheduler)scheduler).startProcessing();
            clock.setTimeSpeed(1.0);
            
            // In past < ASAP < in future.
            final long num1 = scheduledInPast.waitAndGetReport().orderNum;
            final long num2 = scheduledAsap.waitAndGetReport().orderNum;
            final long num3 = scheduledInFuture.waitAndGetReport().orderNum;
            assertEquals(num2, num1 + 1);
            assertEquals(num3, num2 + 1);
        }
    }

    /**
     * Testing process method reschedules unless if exception,
     * in which case worker threads must survive.
     */
    public void test_processReschedulesAndExceptions() {
        final InterfaceHardClock clock = getClockForTest();
        final ArrayList<HardScheduler> schedulerList = createFifoSchedulers(clock);

        @SuppressWarnings("unused")
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
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
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 1,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = null);
                scheduler.execute(schedulable);
                schedulable.waitAndGetReport();
                assertEquals(2, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);
            }

            {
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 1,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = null);
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                schedulable.waitAndGetReport();
                assertEquals(2, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);
            }

            {
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 1,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = null,
                        throwableInOnCancel = null);
                scheduler.executeAfterNs(schedulable, 0);
                schedulable.waitAndGetReport();
                assertEquals(2, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);
            }

            /*
             * Exception in process: doesn't reschedule, and scheduler survives.
             */

            {
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 1,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = MyThrowableType.EXCEPTION,
                        throwableInOnCancel = null);
                scheduler.execute(schedulable);
                schedulable.waitAndGetReport();
                assertEquals(1, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);

                // Letting time to wrapping runnable to call worker's runnable again.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                assertEquals(initialNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }

            {
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 1,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = MyThrowableType.EXCEPTION,
                        throwableInOnCancel = null);
                scheduler.executeAtNs(schedulable, clock.getTimeNs());
                schedulable.waitAndGetReport();
                assertEquals(1, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);

                // Letting time to wrapping runnable to call worker's runnable again.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                assertEquals(initialNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }

            {
                final MySchedulable schedulable = new MySchedulable(
                        nbrOfReschedules = 1,
                        sleepTimeMsInProcess = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInProcess = MyThrowableType.EXCEPTION,
                        throwableInOnCancel = null);
                scheduler.executeAfterNs(schedulable, 0);
                schedulable.waitAndGetReport();
                assertEquals(1, schedulable.nbrOfRunCalls);
                assertEquals(0, schedulable.nbrOfOnCancelCalls);

                // Letting time to wrapping runnable to call worker's runnable again.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                assertEquals(initialNbrOfAliveWorkers, scheduler.getNbrOfRunningWorkers());
            }
        }
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
        int nbrOfReschedules;
        @SuppressWarnings("unused")
        long sleepTimeMsInProcess;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInProcess;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        MySchedulable schedulableTmp;

        for (HardScheduler scheduler : schedulerList) {
            scheduler.stopProcessing();

            // Filling ASAP queue.
            for (int i = 0; i < asapQueueCapacity; i++) {
                scheduler.execute(new MySchedulable());
            }

            // Checking new ASAP schedules are rejected.
            schedulableTmp = new MySchedulable();
            scheduler.execute(schedulableTmp);
            assertFalse(schedulableTmp.runCalled());
            assertTrue(schedulableTmp.onCancelCalled());

            final long processSleepMs = 1000L;

            // Filling timed queue.
            long nowNs = clock.getTimeNs();
            MySchedulable schedulable = new MySchedulable(
                    nbrOfReschedules = 1,
                    sleepTimeMsInProcess = processSleepMs,
                    sleepTimeMsInOnCancel = 0,
                    throwableInProcess = null,
                    throwableInOnCancel = null);
            scheduler.executeAtNs(schedulable, nowNs);
            for (int i = 1; i < timedQueueCapacity; i++) {
                scheduler.executeAtNs(new MySchedulable(), nowNs);
            }

            // Checking new timed schedules are rejected.
            schedulableTmp = new MySchedulable();
            scheduler.executeAtNs(schedulableTmp, nowNs);
            assertFalse(schedulableTmp.runCalled());
            assertTrue(schedulableTmp.onCancelCalled());

            // Starting processing, waiting for first timed schedule
            // (which process waits for a bit) to start being processed,
            // then we add a new timed schedule: the re-schedule must
            // be canceled since the timed queue is now full again.
            scheduler.startProcessing();
            sleepMS(processSleepMs/2);
            // New schedule added into timed queue.
            schedulableTmp = new MySchedulable();
            scheduler.executeAtNs(schedulableTmp, nowNs);
            assertFalse(schedulableTmp.runCalled());
            assertFalse(schedulableTmp.onCancelCalled());
            // Waiting for schedulable to be done.
            schedulable.waitAndGetReport();
            // Checking schedulable was canceled instead of re-scheduled.
            assertEquals(1, schedulable.nbrOfRunCalls);
            assertTrue(schedulable.onCancelCalled());
        }
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

        final Random random = new Random();
        random.setSeed(123456789L);

        for (final HardScheduler scheduler : schedulerList) {

            // Multiple runs, for smaller queues, and higher chance of messy scheduling.
            final int nbrOfRun = 50;
            for (int k = 0; k < nbrOfRun; k++) {

                final int nbrOfCallsPerThread = 1000;
                final int nbrOfCalls = nbrOfCallsPerThread * nbrOfThreads;

                final ArrayList<MySchedulable> schedulables = new ArrayList<MySchedulable>(nbrOfCalls);
                for (int i = 0; i < nbrOfCalls; i++) {
                    schedulables.add(new MySchedulable());
                }

                final AtomicInteger schedulableIndex = new AtomicInteger();

                final ExecutorService executor = Executors.newCachedThreadPool();
                for (int t = 0; t < nbrOfThreads; t++) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < nbrOfCallsPerThread; i++) {
                                final InterfaceSchedulable schedulable = schedulables.get(schedulableIndex.getAndIncrement());

                                final int uniform_0_3 = random.nextInt(4);

                                if (uniform_0_3 <= 1) {
                                    scheduler.execute(schedulable);
                                } else {
                                    scheduler.executeAtNs(schedulable, clock.getTimeNs());
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

                // Checking all schedulables have been processed.
                for (MySchedulable schedulable : schedulables) {
                    schedulable.waitAndGetReport();
                    assertTrue(schedulable.runCalled());
                    assertFalse(schedulable.runInterrupted);
                    assertFalse(schedulable.onCancelCalled());
                }
            }
        }
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

        final Random random = new Random();
        random.setSeed(123456789L);

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
                                    scheduler.execute(new MySchedulable());
                                    break;
                                case 19:
                                    scheduler.executeAtNs(new MySchedulable(), clock.getTimeNs());
                                    break;
                                case 20:
                                    scheduler.executeAfterNs(new MySchedulable(), 1L);
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
                final Runnable checkingRunnable = new Runnable() {
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
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

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
        final HardScheduler scheduler1 = HardScheduler.newInstance(
                clock,
                "SCHEDULER_1",
                true,
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
