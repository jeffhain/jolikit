/*
 * Copyright 2024 Jeff Hain
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
package net.jolikit.threading.execs;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;
import net.jolikit.lang.BooleanWrapper;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.DefaultThreadFactory;
import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.ObjectWrapper;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.basics.InterfaceCancellable;
import net.jolikit.threading.locks.MonitorCondilock;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.SystemTimeClock;

public class FixedThreadExecutorTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final long REAL_TIME_TOLERANCE_MS = 200L;
    
    private static final int DEFAULT_MULTI_WORKER_COUNT = 3;
    
    /**
     * For execution times measurement,
     * which can help for debug.
     */
    private static final InterfaceClock CLOCK = new SystemTimeClock();
    
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
    }

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
        @Override
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
        private final long id = nextRunnableId.getAndIncrement();
        private final Object reportMutex = new Object();
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
        public MyRunnable() {
            this(0);
        }
        public MyRunnable(
                long sleepTimeMsInRun) {
            this(
                    sleepTimeMsInRun,
                    0);
        }
        public MyRunnable(
                long sleepTimeMsInRun,
                long sleepTimeMsInOnCancel) {
            this(
                    sleepTimeMsInRun,
                    sleepTimeMsInOnCancel,
                    null,
                    null);
        }
        public MyRunnable(
                long sleepTimeMsInRun,
                long sleepTimeMsInOnCancel,
                final MyThrowableType throwableInRun,
                final MyThrowableType throwableInOnCancel) {
            this.sleepTimeMsInRun = sleepTimeMsInRun;
            this.sleepTimeMsInOnCancel = sleepTimeMsInOnCancel;
            this.throwableInRun = throwableInRun;
            this.throwableInOnCancel = throwableInOnCancel;
        }
        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "-" + this.id;
        }
        @Override
        public void run() {
            final long runCallTimeNs = CLOCK.getTimeNs();

            this.nbrOfRunCalls++;

            try {
                if (this.sleepTimeMsInRun > 0) {
                    try {
                        Thread.sleep(this.sleepTimeMsInRun);
                    } catch (@SuppressWarnings("unused") InterruptedException e) {
                        this.runInterrupted = true;
                    }
                }

                throwIfNeeded(this.throwableInRun);
            } finally {
                final long orderNum = nextOrderNum.incrementAndGet();

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
                    } catch (@SuppressWarnings("unused") InterruptedException e) {
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
                        if (DEBUG) {
                            Dbg.log(
                                this,
                                " : waitAndGetReport : waiting...");
                        }
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

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final long REAL_TIME_TOLERANCE_NS = REAL_TIME_TOLERANCE_MS * 1000L * 1000L;
    
    /**
     * For debug.
     */
    private final AtomicLong nextRunnableId = new AtomicLong();
    
    private final AtomicLong nextOrderNum = new AtomicLong();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_constructor_threaded() {
        final String threadNamePrefix = null;
        final Boolean daemon = null;
        @SuppressWarnings("unused")
        int nbrOfThreads;
        @SuppressWarnings("unused")
        int queueCapacity;
        @SuppressWarnings("unused")
        int maxWorkerCountForBasicQueue;
        final ThreadFactory threadFactory = null;
        
        @SuppressWarnings("unused")
        Object o;

        // good
        {
            o = new FixedThreadExecutor(
                threadNamePrefix,
                daemon,
                nbrOfThreads = 1,
                queueCapacity = 1,
                maxWorkerCountForBasicQueue = 0,
                threadFactory);
        }
        
        // bad nbrOfThreads
        for (int bad : new int[] {Integer.MIN_VALUE, -1, 0}) {
            try {
                o = new FixedThreadExecutor(
                    threadNamePrefix,
                    daemon,
                    nbrOfThreads = bad,
                    queueCapacity = 1,
                    maxWorkerCountForBasicQueue = 0,
                    threadFactory);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
        
        // bad queueCapacity
        for (int bad : new int[] {Integer.MIN_VALUE, -1, 0}) {
            try {
                o = new FixedThreadExecutor(
                    threadNamePrefix,
                    daemon,
                    nbrOfThreads = 1,
                    queueCapacity = bad,
                    maxWorkerCountForBasicQueue = 0,
                    threadFactory);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
        
        // bad maxWorkerCountForBasicQueue
        for (int bad : new int[] {Integer.MIN_VALUE, -1}) {
            try {
                o = new FixedThreadExecutor(
                    threadNamePrefix,
                    daemon,
                    nbrOfThreads = 1,
                    queueCapacity = 1,
                    maxWorkerCountForBasicQueue = bad,
                    threadFactory);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
    }
    
    public void test_constructor_threaded_prefixAndDaemon() {
        final int nbrOfThreads = 1;
        final int queueCapacity = 1;
        final int maxWorkerCountForBasicQueue = 1;

        for (String tfPrefix : new String[] {null, "tfPrefix"}) {
            for (Boolean tfDaemon : new Boolean[] {null, false, true}) {
                final ThreadFactory tf =
                    new ThreadFactory() {
                    
                    @Override
                    public Thread newThread(Runnable runnable) {
                        final Thread thread = new Thread(runnable);
                        if (tfPrefix != null) {
                            thread.setName(tfPrefix);
                        }
                        if (tfDaemon != null) {
                            thread.setDaemon(tfDaemon);
                        }
                        return thread;
                    }
                };
                for (String ctorPrefix : new String[] {null, "ctorPrefix"}) {
                    for (Boolean ctorDaemon : new Boolean[] {null, false, true}) {
                        final FixedThreadExecutor executor =
                            new FixedThreadExecutor(
                                ctorPrefix,
                                ctorDaemon,
                                nbrOfThreads,
                                queueCapacity,
                                maxWorkerCountForBasicQueue,
                                tf);
                        
                        final ObjectWrapper<String> actualNameW = new ObjectWrapper<>();
                        final BooleanWrapper actualDaemonW = new BooleanWrapper();
                        final Runnable runnable =
                            new Runnable() {
                            
                                @Override
                                public void run() {
                                    actualNameW.value = Thread.currentThread().getName();
                                    actualDaemonW.value = Thread.currentThread().isDaemon();
                                }
                            };
                        executor.execute(runnable);
                        //
                        Unchecked.shutdownAndAwaitTermination(executor);
                        final String actualName = actualNameW.value;
                        final boolean actualDaemon = actualDaemonW.value;
                        
                        if (ctorPrefix != null) {
                            assertTrue(actualName.startsWith(ctorPrefix));
                        } else if (tfPrefix != null) {
                            assertTrue(actualName.startsWith(tfPrefix));
                        } else {
                            // The default.
                            assertTrue(actualName.startsWith("Thread-"));
                        }
                        
                        if (ctorDaemon != null) {
                            assertEquals(ctorDaemon.booleanValue(), actualDaemon);
                        } else if (tfDaemon != null) {
                            assertEquals(tfDaemon.booleanValue(), actualDaemon);
                        } else {
                            // The default.
                            assertFalse(actualDaemon);
                        }
                    }
                }
            }
        }
    }
    
    public void test_constructor_threadless() {
        @SuppressWarnings("unused")
        int queueCapacity;
        @SuppressWarnings("unused")
        int maxWorkerCountForBasicQueue;
        
        @SuppressWarnings("unused")
        Object o;

        // good
        {
            o = new FixedThreadExecutor(
                queueCapacity = 1,
                maxWorkerCountForBasicQueue = 0);
        }
        
        // bad queueCapacity
        for (int bad : new int[] {Integer.MIN_VALUE, -1, 0}) {
            try {
                o = new FixedThreadExecutor(
                    queueCapacity = bad,
                    maxWorkerCountForBasicQueue = 0);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
        
        // bad maxWorkerCountForBasicQueue
        for (int bad : new int[] {Integer.MIN_VALUE, -1}) {
            try {
                o = new FixedThreadExecutor(
                    queueCapacity = 1,
                    maxWorkerCountForBasicQueue = bad);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_toString() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();
        executorList.addAll(newThreadlessExecutors());

        for (FixedThreadExecutor executor : executorList) {
            assertNotNull(executor.toString());
            
            shutdownNowAndWait(executor);
        }
    }

    /*
     * Getters.
     */

    public void test_getNbrOfWorkers() {
        
        {
            final List<FixedThreadExecutor> executorList = newThreadlessExecutors();
            
            for (FixedThreadExecutor executor : executorList) {
                assertEquals(1, executor.getNbrOfWorkers());
                
                shutdownNowAndWait(executor);
            }
        }
        
        {
            final List<FixedThreadExecutor> executorList = newFifoExecutors();

            for (FixedThreadExecutor executor : executorList) {
                assertEquals(1, executor.getNbrOfWorkers());
                
                shutdownNowAndWait(executor);
            }
        }
        
        {
            final int nbrOfWorkers = DEFAULT_MULTI_WORKER_COUNT;
            final List<FixedThreadExecutor> executorList = newExecutors(nbrOfWorkers);

            for (FixedThreadExecutor executor : executorList) {
                assertEquals(nbrOfWorkers, executor.getNbrOfWorkers());
                
                shutdownNowAndWait(executor);
            }
        }
    }
    
    public void test_getNbrOfRunningWorkers() {
        /*
         * Executors with 2 threads each.
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

        final List<FixedThreadExecutor> executorList = newExecutors(
                2,
                Integer.MAX_VALUE,
                threadFactory);

        // Letting time for workers to get NOT running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        // Starting workers, and checking.
        for (FixedThreadExecutor executor : executorList) {
            assertEquals(0, executor.getNbrOfRunningWorkers());
            executor.startWorkerThreadsIfNeeded();
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

        for (FixedThreadExecutor executor : executorList) {

            int expectedNbrOfAliveWorkers = executor.getNbrOfRunningWorkers();

            {
                final MyRunnable runnable = new MyRunnable(
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = MyThrowableType.ERROR,
                        throwableInOnCancel = null);
                executor.execute(runnable);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);
                // Letting time for worker thread to die.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                expectedNbrOfAliveWorkers--;
                assertEquals(expectedNbrOfAliveWorkers, executor.getNbrOfRunningWorkers());
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_getNbrOfIdleWorkers() {
        final List<FixedThreadExecutor> executorList = newExecutors(
            DEFAULT_MULTI_WORKER_COUNT);

        // Letting time for workers to get NOT running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        // Starting workers, and checking.
        for (FixedThreadExecutor executor : executorList) {
            assertEquals(0, executor.getNbrOfIdleWorkers());
            executor.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (FixedThreadExecutor executor : executorList) {

            final int nbrOfWorkers = executor.getNbrOfRunningWorkers();

            assertEquals(nbrOfWorkers, executor.getNbrOfIdleWorkers());

            {
                final MyRunnable runnable = new MyRunnable(
                        2 * REAL_TIME_TOLERANCE_MS);
                executor.execute(runnable);
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers-1, executor.getNbrOfIdleWorkers());
                // Waiting for run to finish.
                runnable.waitAndGetReport();
                // Letting time for executor to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(nbrOfWorkers, executor.getNbrOfIdleWorkers());
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_getNbrOfWorkingWorkers() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        // Starting workers.
        for (FixedThreadExecutor executor : executorList) {
            assertEquals(0, executor.getNbrOfWorkingWorkers());
            executor.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (FixedThreadExecutor executor : executorList) {

            assertEquals(0, executor.getNbrOfWorkingWorkers());

            {
                final MyRunnable runnable = new MyRunnable(
                        2 * REAL_TIME_TOLERANCE_MS);
                executor.execute(runnable);
                // Letting time for processing to begin.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, executor.getNbrOfWorkingWorkers());
                // Waiting for run to finish.
                runnable.waitAndGetReport();
                // Letting time for executor to get aware worker finished its work.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(0, executor.getNbrOfWorkingWorkers());
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_getNbrOfPendingSchedules() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {
            executor.stopProcessing();

            assertEquals(0, executor.getNbrOfPendingSchedules());

            final MyRunnable runnable1 = new MyRunnable();
            executor.execute(runnable1);
            assertEquals(1, executor.getNbrOfPendingSchedules());

            final MyRunnable runnable2 = new MyRunnable();
            executor.execute(runnable2);
            assertEquals(2, executor.getNbrOfPendingSchedules());

            executor.startProcessing();

            // Waiting for runnables to be processed.
            runnable1.waitAndGetReport();
            runnable2.waitAndGetReport();

            assertEquals(0, executor.getNbrOfPendingSchedules());
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_isWorkersDeathRequested() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {
            assertFalse(executor.isShutdown());
            executor.shutdown();
            assertTrue(executor.isShutdown());
        }
        
        shutdownNowAndWait(executorList);
    }

    /*
     * Controls.
     */
    
    public void test_startAndWorkInCurrentThread() {
        final FixedThreadExecutor executor = FixedThreadExecutor.newThreadlessInstance();
        
        final List<String> runTagList = new ArrayList<String>();
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                runTagList.add("1");
            }
        });
        
        // Will throw, so that we can then check that we can call again
        // for more processing.
        executor.execute(new Runnable() {
            @Override
            public void run() {
                runTagList.add("2");
                // Terminating exceptionally.
                throw new MyRuntimeException();
            }
        });
        
        try {
            executor.startAndWorkInCurrentThread();
            fail();
        } catch (@SuppressWarnings("unused") MyRuntimeException e) {
            // ok
        }
        
        assertEquals("[1, 2]", Arrays.toString(runTagList.toArray()));
        
        assertEquals(0, executor.getNbrOfPendingSchedules());
        assertEquals(0, executor.getNbrOfRunningWorkers());
        assertEquals(0, executor.getNbrOfIdleWorkers());
        
        /*
         * Calling again.
         */
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                runTagList.add("3");
            }
        });
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                runTagList.add("4");
                // Terminating normally.
                executor.shutdown();
                executor.stop();
            }
        });

        executor.startAndWorkInCurrentThread();

        assertEquals("[1, 2, 3, 4]", Arrays.toString(runTagList.toArray()));
        
        assertEquals(0, executor.getNbrOfPendingSchedules());
        assertEquals(0, executor.getNbrOfRunningWorkers());
        assertEquals(0, executor.getNbrOfIdleWorkers());
        
        shutdownNowAndWait(executor);
    }

    public void test_startWorkerThreadsIfNeeded() {
        final List<FixedThreadExecutor> executorList = newExecutors(
            DEFAULT_MULTI_WORKER_COUNT);

        // Letting time for workers to get NOT running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        // Starting workers, and checking.
        for (FixedThreadExecutor executor : executorList) {
            assertEquals(0, executor.getNbrOfRunningWorkers());
            executor.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (FixedThreadExecutor executor : executorList) {
            assertNotSame(0, executor.getNbrOfRunningWorkers());
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_start() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {
            // stop is supposed tested:
            // we check start gets things back up.
            executor.stop();

            executor.start();

            final long startTimeNs = CLOCK.getTimeNs();

            {
                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);
                final MySchedulingReport report = runnable.waitAndGetReport();
                // Processed right away.
                assertEquals(startTimeNs, report.runCallTimeNs, REAL_TIME_TOLERANCE_NS);
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_stop() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {
            executor.stop();

            // Checking schedules are not processed, and not accepted.

            {
                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);
                // letting time for processing (not) to occur
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertTrue(runnable.onCancelCalled());
            }

            // Checking schedules were really not accepted,
            // i.e. didn't make it into pending schedules.
            // Supposes getNbrOfPendingSchedules() method works.

            assertEquals(0, executor.getNbrOfPendingSchedules());
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_startAccepting() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {
            // stop is supposed tested:
            // we check startAccepting gets accepting back up.
            executor.stop();

            executor.startAccepting();

            {
                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertFalse(runnable.onCancelCalled());
                assertEquals(1, executor.getNbrOfPendingSchedules());
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_stopAccepting() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (FixedThreadExecutor executor : executorList) {
            // Checking schedules are not accepted,
            // and pending schedules are processed.

            // To test schedules are still processed
            // when new ones are not accepted.
            // Large enough to still have pending schedules
            // when acceptance gets stopped.
            final int nbrOfSchedules = 4;
            for (int i = 0; i < nbrOfSchedules; i++) {
                executor.execute(new MyRunnable(
                    sleepTimeMsInRun = 10));
            }

            executor.stopAccepting();

            final int nbrOfSchedulesAfterStopAcception =
                executor.getNbrOfPendingSchedules();
            assertTrue(nbrOfSchedulesAfterStopAcception != 0);
            // Letting time for schedules to be processed.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(0, executor.getNbrOfPendingSchedules());

            {
                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);
                // Letting time for processing (not) to occur.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertFalse(runnable.runCalled());
                assertTrue(runnable.onCancelCalled());
            }

            {
                final MyRunnable runnable = new MyRunnable();
                final Runnable notCancellable = new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                };
                try {
                    executor.execute(notCancellable);
                    fail();
                } catch (@SuppressWarnings("unused") RejectedExecutionException e) {
                    // ok
                }
                assertFalse(runnable.runCalled());
            }

            // Checking schedules were really not accepted,
            // i.e. didn't make it into pending schedules.
            // Supposes getNbrOfPendingSchedules() method work.
            assertEquals(0, executor.getNbrOfPendingSchedules());
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_startProcessing() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {
            {
                // stopProcessing is supposed tested:
                // we check startProcessing gets things back up.
                executor.stopProcessing();

                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);

                // Letting time for runnable (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                // Runnable should start to be processed after that call.
                executor.startProcessing();

                runnable.waitAndGetReport();
                assertTrue(runnable.runCalled());
                assertFalse(runnable.onCancelCalled());
                assertEquals(0, executor.getNbrOfPendingSchedules());
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_stopProcessing() {
        /*
         * Scheduling when processing already stopped.
         */

        {
            final List<FixedThreadExecutor> executorList = newFifoExecutors();
            
            for (FixedThreadExecutor executor : executorList) {
                executor.stopProcessing();

                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);

                // Letting time for runnables (not) to be processed.
                sleepMS(REAL_TIME_TOLERANCE_MS);
                assertEquals(1, executor.getNbrOfPendingSchedules());
                assertFalse(runnable.runCalled());
                assertFalse(runnable.onCancelCalled());
            }
            
            shutdownNowAndWait(executorList);
        }

        /*
         * Stopping processing when scheduling is going on.
         */

        {
            final List<FixedThreadExecutor> executorList = newFifoExecutors();
            
            for (FixedThreadExecutor executor : executorList) {

                // Runnables that wait for a bit, and enough
                // of them for pending schedules queues to contain
                // a few of them before we start to sleep.
                final int nbrOfSchedules = 10;
                final int sleepTimeMsInRun = 10;
                for (int i = 0; i < nbrOfSchedules; i++) {
                    MyRunnable runnable = new MyRunnable(
                            sleepTimeMsInRun);
                    executor.execute(runnable);
                }

                // Stopping processing, which is supposed to make pending
                // schedules not processed.
                executor.stopProcessing();
                // Retrieving numbers of pending schedules right after the stop.
                // Since schedules processing is stopped, this number must remain
                // the same, which we check after a little sleep.
                final int nbrOfPendingSchedules1 = executor.getNbrOfPendingSchedules();
                sleepMS(REAL_TIME_TOLERANCE_MS);
                final int nbrOfPendingSchedules2 = executor.getNbrOfPendingSchedules();

                assertEquals(nbrOfPendingSchedules1, nbrOfPendingSchedules2);
            }
            
            shutdownNowAndWait(executorList);
        }
    }

    public void test_cancelPendingSchedules() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (FixedThreadExecutor executor : executorList) {

            /*
             * Canceling pending schedules right after scheduling.
             */

            {
                executor.stopProcessing();

                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);

                assertEquals(1, executor.getNbrOfPendingSchedules());

                executor.cancelPendingSchedules();

                assertFalse(runnable.runCalled());
                assertTrue(runnable.onCancelCalled());
                assertEquals(0, executor.getNbrOfPendingSchedules());
            }

            /*
             * Testing schedules cancellation stops if one throws an exception.
             */

            {
                executor.stopProcessing();

                // Getting non-throwing runnable surrounded
                // with throwing ones, to handle different cases
                // of iteration.
                final MyRunnable runnableException1 = new MyRunnable(
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                executor.execute(runnableException1);

                final MyRunnable runnable = new MyRunnable();
                executor.execute(runnable);

                final MyRunnable runnableException2 = new MyRunnable(
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = MyThrowableType.EXCEPTION);
                executor.execute(runnableException2);

                assertEquals(3, executor.getNbrOfPendingSchedules());

                try {
                    executor.cancelPendingSchedules();
                    fail();
                } catch (@SuppressWarnings("unused") Exception e) {
                    // ok
                }

                assertFalse(runnable.runCalled());
                assertFalse(runnableException1.runCalled());
                assertFalse(runnableException2.runCalled());

                assertFalse(runnable.onCancelCalled()
                        && runnableException1.onCancelCalled()
                        && runnableException2.onCancelCalled());

                assertNotSame(0, executor.getNbrOfPendingSchedules());

                // Continuing cancellation until no more pending schedule.
                while (true) {
                    try {
                        executor.cancelPendingSchedules();
                    } catch (@SuppressWarnings("unused") Exception e) {
                        // quiet
                    }
                    if (executor.getNbrOfPendingSchedules() == 0) {
                        break;
                    }
                }
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_drainPendingRunnablesInto_ObjectVector() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {

            /*
             * No runnable.
             */

            {
                // Must work with collection of Object.
                ArrayList<Object> runnables = new ArrayList<Object>();
                Object foo = new Object();
                runnables.add(foo);
                executor.drainPendingRunnablesInto(runnables);

                // Nothing added, nor removed.
                assertEquals(1, runnables.size());
                assertEquals(foo, runnables.get(0));
            }

            /*
             * Runnables.
             */

            {
                executor.stopProcessing();

                final MyRunnable runnable1 = new MyRunnable();
                executor.execute(runnable1);
                final MyRunnable runnable2 = new MyRunnable();
                executor.execute(runnable2);

                assertEquals(2, executor.getNbrOfPendingSchedules());

                final ArrayList<Object> runnables = new ArrayList<Object>();
                final Object foo = new Object();
                runnables.add(foo);
                executor.drainPendingRunnablesInto(runnables);
                
                // Checking drained.
                assertEquals(3, runnables.size());
                assertEquals(foo, runnables.get(0));
                assertEquals(runnable1, runnables.get(1));
                assertEquals(runnable2, runnables.get(2));

                assertFalse(runnable1.runCalled());
                assertFalse(runnable1.onCancelCalled());
                assertFalse(runnable2.runCalled());
                assertFalse(runnable2.onCancelCalled());
                assertEquals(0, executor.getNbrOfPendingSchedules());
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_workersWakeUpOnSchedules_cancel() {
        final boolean mustCancelElseDrain = true;
        final boolean mustThrowDuringDrain = false;
        for (int k = 0; k < 5; k++) {
            test_workersWakeUpOnSchedulesXxx(
                mustCancelElseDrain,
                mustThrowDuringDrain);
        }
    }
    
    public void test_workersWakeUpOnSchedules_drain() {
        final boolean mustCancelElseDrain = false;
        final boolean mustThrowDuringDrain = false;
        for (int k = 0; k < 5; k++) {
            test_workersWakeUpOnSchedulesXxx(
                mustCancelElseDrain,
                mustThrowDuringDrain);
        }
    }
    
    public void test_workersWakeUpOnSchedules_drainWithException() {
        final boolean mustCancelElseDrain = false;
        final boolean mustThrowDuringDrain = true;
        for (int k = 0; k < 5; k++) {
            test_workersWakeUpOnSchedulesXxx(
                mustCancelElseDrain,
                mustThrowDuringDrain);
        }
    }

    public void test_workersWakeUpOnSchedulesXxx(
            boolean mustCancelElseDrain,
            boolean mustThrowDuringDrain) {

        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (FixedThreadExecutor executor : executorList) {

            executor.startWorkerThreadsIfNeeded();
            // Letting time for worker to go live.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(1, executor.getNbrOfRunningWorkers());

            // For worker to wait for runnable to be processable.
            executor.stopProcessing();
            
            final MyRunnable runnable = new MyRunnable();
            executor.execute(runnable);

            executor.shutdown();

            // Letting time for workers to be awoken by shutdown()
            // (not to mistake it for tested awakening).
            sleepMS(REAL_TIME_TOLERANCE_MS);
            
            assertEquals(1, executor.getNbrOfPendingSchedules());
            assertEquals(1, executor.getNbrOfRunningWorkers());

            if (mustCancelElseDrain) {
                executor.cancelPendingSchedules();
            } else {
                if (mustThrowDuringDrain) {
                    final ArrayList<Object> runnables = new ArrayList<Object>() {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public boolean add(Object toAdd) {
                            throw new RuntimeException();
                        }
                    };
                    try {
                        executor.drainPendingRunnablesInto(runnables);
                        fail();
                    } catch (@SuppressWarnings("unused") RuntimeException e) {
                        // ok
                    }
                } else {
                    final ArrayList<Object> runnables = new ArrayList<Object>();
                    executor.drainPendingRunnablesInto(runnables);
                }
            }
            assertEquals(0, executor.getNbrOfPendingSchedules());
            // Letting time for worker to die.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(0, executor.getNbrOfRunningWorkers());
        }

        shutdownNowAndWait(executorList);
    }

    public void test_interruptWorkers() {
        /*
         * Executors with 1 worker each, for we put one schedule
         * in each of them, and want them to be processed together,
         * so we can interrupt them together.
         * 
         * Not using our default thread factory, for we want to make sure
         * the workers never die on interrupt.
         */

        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        /*
         * Checking interrupting idle workers doesn't hurt.
         */

        // Starting workers.
        for (FixedThreadExecutor executor : executorList) {
            executor.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (FixedThreadExecutor executor : executorList) {

            final int nbrOfWorkers = executor.getNbrOfRunningWorkers();

            // Checking interrupting idle workers doesn't hurt.
            executor.interruptWorkers();
            // Letting time for workers to recover from interrupt.
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(nbrOfWorkers, executor.getNbrOfRunningWorkers());
        }

        /*
         * Checking interrupting working workers works.
         */

        for (FixedThreadExecutor executor : executorList) {

            final MyRunnable runnable = new MyRunnable(
                    Long.MAX_VALUE);
            executor.execute(runnable);

            // Letting time for runnables to be called.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            // Runnable being called.
            assertTrue(runnable.runCalled());
            assertFalse(runnable.onCancelCalled());

            executor.interruptWorkers();

            // Letting time for interrupt to propagate.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertTrue(runnable.runInterrupted);
            assertFalse(runnable.onCancelCalled());
            assertFalse(runnable.onCancelInterrupted);
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_shutdown() {
        final List<FixedThreadExecutor> executorList = newExecutors(
            DEFAULT_MULTI_WORKER_COUNT);

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        for (FixedThreadExecutor executor : executorList) {
            // Supposing stopProcessing works.
            executor.stopProcessing();

            /*
             * Getting some pending schedule.
             */

            final MyRunnable runnable = new MyRunnable();
            executor.execute(runnable);

            /*
             * Requesting workers death when idle.
             */

            executor.shutdown();

            // Making sure no more schedules are accepted.
            final MyRunnable runnableRejected = new MyRunnable();
            executor.execute(runnableRejected);
            assertFalse(runnableRejected.runCalled());
            assertTrue(runnableRejected.onCancelCalled());

            // Letting time for workers NOT to die (since there are pending schedules).
            sleepMS(REAL_TIME_TOLERANCE_MS);
            assertEquals(1, executor.getNbrOfPendingSchedules());
            assertTrue(executor.getNbrOfRunningWorkers() > 0);
            assertTrue(executor.getNbrOfIdleWorkers() > 0);
            assertTrue(executor.getNbrOfWorkingWorkers() == 0);

            /*
             * Starting processing, and then verifying schedules
             * were correctly executed before workers death.
             */

            executor.startProcessing();

            // Waiting for schedules to be processed.
            runnable.waitAndGetReport();

            // Checking correct schedules execution.
            assertTrue(runnable.runCalled());
            assertFalse(runnable.onCancelCalled());

            // Letting time for workers to die.
            sleepMS(REAL_TIME_TOLERANCE_MS);

            assertEquals(0, executor.getNbrOfRunningWorkers());
            assertEquals(0, executor.getNbrOfIdleWorkers());
            assertEquals(0, executor.getNbrOfWorkingWorkers());
        }
        
        shutdownNowAndWait(executorList);
    }

    /*
     * Waits.
     */

    public void test_waitForNoMoreRunningWorker_long() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        final long timeoutNs = TimeUtils.sToNs(1.0);

        for (FixedThreadExecutor executor : executorList) {

            final MyRunnable runnable = new MyRunnable();

            executor.stopProcessing();

            executor.execute(runnable);

            // No need to wait for workers to get running, since
            // they are not considered as "no more running" if they
            // have not all been started yet.

            /*
             * Waiting timeout.
             */

            long a = System.nanoTime();
            boolean doneBeforeTimeout = false;
            try {
                doneBeforeTimeout = executor.waitForNoMoreRunningWorker(timeoutNs);
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                fail();
            }
            long b = System.nanoTime();
            assertFalse(doneBeforeTimeout);
            assertEquals(timeoutNs, b-a, REAL_TIME_TOLERANCE_NS);

            /*
             * Wait interrupted (need to take care to interrupt while in the wait,
             * for interrupted status to be cleared by exception catch).
             */

            final Thread tester = Thread.currentThread();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sleepMS(TimeUnit.NANOSECONDS.toMillis(timeoutNs/2));
                    tester.interrupt();
                }
            }).start();

            try {
                doneBeforeTimeout = executor.waitForNoMoreRunningWorker(timeoutNs);
                // Wait must throw InterruptedException.
                fail();
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                // quiet
            }

            /*
             * Not waiting much if any (schedules processed, then dying).
             */

            executor.startProcessing();
            executor.shutdown();

            a = System.nanoTime();
            try {
                doneBeforeTimeout = executor.waitForNoMoreRunningWorker(timeoutNs);
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                fail();
            }
            b = System.nanoTime();
            assertTrue(doneBeforeTimeout);
            assertEquals(0, b-a, REAL_TIME_TOLERANCE_NS);

            // No more running worker.
            assertEquals(0, executor.getNbrOfRunningWorkers());

            // Checking schedules have been executed before wait returned.
            assertTrue(runnable.runCalled());
            assertFalse(runnable.onCancelCalled());

            /*
             * Not waiting at all (no running worker when wait starts).
             */

            a = System.nanoTime();
            try {
                doneBeforeTimeout = executor.waitForNoMoreRunningWorker(timeoutNs);
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                fail();
            }
            b = System.nanoTime();
            assertTrue(doneBeforeTimeout);
            assertEquals(0, b-a, REAL_TIME_TOLERANCE_NS);
        }
        
        shutdownNowAndWait(executorList);
    }

    /*
     * Scheduling.
     */

    public void test_execute_null() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();
        
        for (Executor executor : executorList) {
            try {
                executor.execute(null);
                fail();
            } catch (@SuppressWarnings("unused") NullPointerException e) {
                // ok
            }
        }
    }
    
    public void test_execute_fifoOrder() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        for (Executor executor : executorList) {
            this.nextOrderNum.set(0);

            final ArrayList<MyRunnable> runnableList = new ArrayList<MyRunnable>();

            final int nbrOfRunnables = 100 * 1000;

            for (int i = 0; i < nbrOfRunnables; i++) {
                final MyRunnable runnable = new MyRunnable();
                runnableList.add(runnable);
                executor.execute(runnable);
            }

            for (int i = 0; i < nbrOfRunnables; i++) {
                final MyRunnable runnable = runnableList.get(i);
                assertEquals(i+1,runnable.waitAndGetReport().orderNum);
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    /*
     * General tests (not method-specific).
     */

    /**
     * Testing that worker threads survive exceptions.
     */
    public void test_workersSurvivalFromExceptions() {
        final List<FixedThreadExecutor> executorList = newFifoExecutors();

        @SuppressWarnings("unused")
        long sleepTimeMsInRun;
        @SuppressWarnings("unused")
        long sleepTimeMsInOnCancel;
        @SuppressWarnings("unused")
        MyThrowableType throwableInRun;
        @SuppressWarnings("unused")
        MyThrowableType throwableInOnCancel;

        // Starting workers.
        for (FixedThreadExecutor executor : executorList) {
            executor.startWorkerThreadsIfNeeded();
        }

        // Letting time for workers to get running.
        sleepMS(REAL_TIME_TOLERANCE_MS);

        for (FixedThreadExecutor executor : executorList) {

            final int initialNbrOfAliveWorkers = executor.getNbrOfRunningWorkers();

            /*
             * Regular call.
             */

            {
                final MyRunnable runnable = new MyRunnable(
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = null,
                        throwableInOnCancel = null);
                executor.execute(runnable);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);
            }

            /*
             * Exception in run: workers survives.
             */

            {
                final MyRunnable runnable = new MyRunnable(
                        sleepTimeMsInRun = 0,
                        sleepTimeMsInOnCancel = 0,
                        throwableInRun = MyThrowableType.EXCEPTION,
                        throwableInOnCancel = null);
                executor.execute(runnable);
                runnable.waitAndGetReport();
                assertEquals(1, runnable.nbrOfRunCalls);
                assertEquals(0, runnable.nbrOfOnCancelCalls);

                // Letting time to wrapping runnable to call worker's runnable again.
                sleepMS(REAL_TIME_TOLERANCE_MS);

                assertEquals(initialNbrOfAliveWorkers, executor.getNbrOfRunningWorkers());
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_queueCapacity() {
        final int queueCapacity = 2;

        final List<FixedThreadExecutor> executorList =
            newExecutors(
                DEFAULT_MULTI_WORKER_COUNT,
                queueCapacity,
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

        for (FixedThreadExecutor executor : executorList) {
            executor.stopProcessing();

            // Filling queue.
            for (int i = 0; i < queueCapacity; i++) {
                runnableTmp = new MyRunnable();
                executor.execute(runnableTmp);
                assertFalse(runnableTmp.runCalled());
                assertFalse(runnableTmp.onCancelCalled());
            }

            // Checking new schedules are rejected.
            runnableTmp = new MyRunnable();
            executor.execute(runnableTmp);
            assertFalse(runnableTmp.runCalled());
            assertTrue(runnableTmp.onCancelCalled());
        }
        
        shutdownNowAndWait(executorList);
    }

    /**
     * Testing no schedule is lost when stressing scheduling and processing start and stop.
     */
    public void test_schedulingStress() {
        final int nbrOfThreads = DEFAULT_MULTI_WORKER_COUNT;

        final List<FixedThreadExecutor> executorList = newExecutors(
                nbrOfThreads);

        final Random random = TestUtils.newRandom123456789L();

        for (final FixedThreadExecutor executor : executorList) {

            // Multiple runs, for smaller queues, and higher chance of messy scheduling.
            final int nbrOfRun = 50;
            for (int k = 0; k < nbrOfRun; k++) {

                final int nbrOfCallsPerThread = 1000;
                final int nbrOfCalls = nbrOfCallsPerThread * nbrOfThreads;

                final ArrayList<MyRunnable> runnables = new ArrayList<MyRunnable>(nbrOfCalls);
                for (int i = 0; i < nbrOfCalls; i++) {
                    runnables.add(new MyRunnable());
                }

                final AtomicInteger runnableIndex = new AtomicInteger();

                final ExecutorService jdkExecutor = Executors.newCachedThreadPool();
                for (int t = 0; t < nbrOfThreads; t++) {
                    jdkExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < nbrOfCallsPerThread; i++) {
                                final MyRunnable runnable = runnables.get(
                                    runnableIndex.getAndIncrement());

                                executor.execute(runnable);

                                if (random.nextBoolean()) {
                                    executor.startProcessing();
                                } else {
                                    executor.stopProcessing();
                                }
                            }
                            // Making sure we don't end up with processing stopped.
                            executor.startProcessing();
                        }
                    });
                }
                Unchecked.shutdownAndAwaitTermination(jdkExecutor);

                // Checking all runnables have been processed.
                for (MyRunnable runnable : runnables) {
                    runnable.waitAndGetReport();
                    assertTrue(runnable.runCalled());
                    assertFalse(runnable.runInterrupted);
                    assertFalse(runnable.onCancelCalled());
                }
            }
        }
        
        shutdownNowAndWait(executorList);
    }

    /**
     * Calls all methods concurrently, no exception should be thrown.
     */
    public void test_allConcurrentCalls() {
        final int nbrOfThreads = DEFAULT_MULTI_WORKER_COUNT;
        final int nbrOfCallsPerThread = 100;
        // Multiple runs, for smaller queues, and higher chance of messy scheduling.
        final int nbrOfRun = 50;

        final List<FixedThreadExecutor> executorList = newExecutors(
                nbrOfThreads);

        final Random random = TestUtils.newRandom123456789L();

        for (final FixedThreadExecutor executor : executorList) {

            ExecutorService jdkExecutor = Executors.newCachedThreadPool();
            
            for (int k = 0; k < nbrOfRun; k++) {
                for (int t = 0; t < nbrOfThreads; t++) {
                    jdkExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < nbrOfCallsPerThread; i++) {
                                /*
                                 * Not calling:
                                 * - toString() and appendString(StringBuilder)
                                 * - start() and stop() (redundant with others)
                                 * - shutdown() (dead end)
                                 * - waitForNoMoreRunningWorkerSystemTimeNs(long)
                                 */
                                final int nbrOfMethods = 14;
                                final int uniform_1_N = 1 + random.nextInt(nbrOfMethods);
                                switch (uniform_1_N) {
                                case 1:
                                    executor.getNbrOfRunningWorkers();
                                    break;
                                case 2:
                                    executor.getNbrOfIdleWorkers();
                                    break;
                                case 3:
                                    executor.getNbrOfWorkingWorkers();
                                    break;
                                case 4:
                                    executor.getNbrOfPendingSchedules();
                                    break;
                                case 5:
                                    executor.getNbrOfPendingSchedules();
                                    break;
                                case 6:
                                    executor.startWorkerThreadsIfNeeded();
                                    break;
                                case 7:
                                    executor.startAccepting();
                                    break;
                                case 8:
                                    executor.stopAccepting();
                                    break;
                                case 9:
                                    executor.startProcessing();
                                    break;
                                case 10:
                                    executor.stopProcessing();
                                    break;
                                case 11:
                                    executor.cancelPendingSchedules();
                                    break;
                                case 12:
                                    executor.drainPendingRunnablesInto(new ArrayList<Object>());
                                    break;
                                case 13:
                                    executor.interruptWorkers();
                                    break;
                                case 14:
                                    executor.execute(new MyRunnable());
                                    break;
                                default:
                                    throw new AssertionError(uniform_1_N);
                                }
                            }
                            // Making sure we don't end up with processing stopped.
                            executor.startProcessing();
                        }
                    });
                }
                // Waiting for pending schedules to be processed or canceled.
                while (executor.getNbrOfPendingSchedules() != 0) {
                    sleepMS(REAL_TIME_TOLERANCE_MS);
                }
            }
            Unchecked.shutdownAndAwaitTermination(jdkExecutor);
        }
        
        shutdownNowAndWait(executorList);
    }
    
    /*
     * InterfaceWorkerAware
     */
    
    public void test_isWorkerThread_andChecks_withThreads() {
        final int nbrOfThreads = 2;
        final List<FixedThreadExecutor> executorList = newExecutors(
                nbrOfThreads);

        for (final FixedThreadExecutor executor : executorList) {
            
            assertFalse(executor.isWorkerThread());
            try {
                executor.checkIsWorkerThread();
                fail();
            } catch (@SuppressWarnings("unused") ConcurrentModificationException e) {
                // ok
            }
            executor.checkIsNotWorkerThread();
            
            /*
             * 
             */
            
            final AtomicInteger res = new AtomicInteger();
            final MonitorCondilock condilock = new MonitorCondilock();
            final MyRunnable checkingRunnable = new MyRunnable() {
                @Override
                public void run() {
                    res.set(executor.isWorkerThread() ? 1 : -1);
                    
                    try {
                        executor.checkIsWorkerThread();
                        // ok
                    } catch (@SuppressWarnings("unused") Throwable e) {
                        res.set(2); // bad value
                    }
                    try {
                        executor.checkIsNotWorkerThread();
                        res.set(3); // bad value
                    } catch (@SuppressWarnings("unused") IllegalStateException e) {
                        // ok
                    }
                    
                    condilock.signalAllInLock();
                }
            };
            executor.execute(checkingRunnable);
            condilock.awaitWhileFalseInLockUninterruptibly(new InterfaceBooleanCondition() {
                @Override
                public boolean isTrue() {
                    return res.get() != 0;
                }
            });
            
            assertEquals(1, res.get());
        }
        
        shutdownNowAndWait(executorList);
    }

    public void test_isWorkerThread_andChecks_threadless() {
        final FixedThreadExecutor executor = FixedThreadExecutor.newThreadlessInstance();
        
        assertFalse(executor.isWorkerThread());
        try {
            executor.checkIsWorkerThread();
            fail();
        } catch (@SuppressWarnings("unused") ConcurrentModificationException e) {
            // ok
        }
        executor.checkIsNotWorkerThread();

        /*
         * 
         */
        
        final MyRunnable checkingRunnable = new MyRunnable() {
            @Override
            public void run() {
                assertTrue(executor.isWorkerThread());
                
                executor.checkIsWorkerThread();
                try {
                    executor.checkIsNotWorkerThread();
                    fail();
                } catch (@SuppressWarnings("unused") IllegalStateException e) {
                    // ok
                }
            }
        };
        executor.execute(checkingRunnable);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                executor.shutdown();
            }
        });

        executor.startAndWorkInCurrentThread();

        /*
         * 
         */
        
        assertFalse(executor.isWorkerThread());
        try {
            executor.checkIsWorkerThread();
            fail();
        } catch (@SuppressWarnings("unused") ConcurrentModificationException e) {
            // ok
        }
        executor.checkIsNotWorkerThread();
        
        shutdownNowAndWait(executor);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /*
     * 
     */
    
    private static void shutdownNowAndWait(List<FixedThreadExecutor> executorList) {
        for (FixedThreadExecutor executor : executorList) {
            shutdownNowAndWait(executor);
        }
    }

    private static void shutdownNowAndWait(FixedThreadExecutor executor) {
        /*
         * TODO For some enigmaticalistiquesquish reason,
         * interrupting formerly tested executor's workers,
         * causes interruption of workers of subsequent tests,
         * before we try to shut them down, as if the JVM
         * was reusing OS threads for new Java threads,
         * if that makes sense.
         * As a result, we don't interrupt here.
         */
        final boolean mustInterruptWorkingWorkers = false;
        executor.shutdownNow(mustInterruptWorkingWorkers);
        try {
            executor.waitForNoMoreRunningWorker(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 
     */
    
    /**
     * Executors with a single worker,
     * to ensure FIFO order for schedules.
     */
    private static List<FixedThreadExecutor> newFifoExecutors() {
        return newExecutors(1);
    }

    private static List<FixedThreadExecutor> newExecutors(
            int nbrOfThreads) {
        final ThreadFactory threadFactory = new MyThreadFactory();
        return newExecutors(
                nbrOfThreads,
                Integer.MAX_VALUE,
                threadFactory);
    }

    private static List<FixedThreadExecutor> newExecutors(
        int nbrOfThreads,
        int queueCapacity,
        ThreadFactory threadFactory) {
        
        final boolean daemon = true;
        
        @SuppressWarnings("unused")
        int maxWorkerCountForBasicQueue;
        
        final List<FixedThreadExecutor> executorList = new ArrayList<FixedThreadExecutor>();
        
        // Executor with basic queue.
        executorList.add(
            new FixedThreadExecutor(
                "FTE_BAS",
                daemon,
                nbrOfThreads,
                queueCapacity,
                (maxWorkerCountForBasicQueue = Integer.MAX_VALUE),
                threadFactory));
        
        // Executor with advanced queue.
        executorList.add(
            new FixedThreadExecutor(
                "FTE_ADV",
                daemon,
                nbrOfThreads,
                queueCapacity,
                (maxWorkerCountForBasicQueue = 0),
                threadFactory));
        
        return executorList;
    }

    /*
     * 
     */
    
    private static List<FixedThreadExecutor> newThreadlessExecutors() {
        return newThreadlessExecutors(Integer.MAX_VALUE);
    }
    
    private static List<FixedThreadExecutor> newThreadlessExecutors(
        int queueCapacity) {
        
        @SuppressWarnings("unused")
        int maxWorkerCountForBasicQueue;
        
        final List<FixedThreadExecutor> executorList = new ArrayList<FixedThreadExecutor>();
        
        // Executor with basic queue.
        executorList.add(
            new FixedThreadExecutor(
                queueCapacity,
                (maxWorkerCountForBasicQueue = Integer.MAX_VALUE)));
        
        // Executor with advanced queue.
        executorList.add(
            new FixedThreadExecutor(
                queueCapacity,
                (maxWorkerCountForBasicQueue = 0)));
        
        return executorList;
    }
    
    /*
     * 
     */
    
    private static void sleepMS(long ms) {
        Unchecked.sleepMs(ms);
    }
}
