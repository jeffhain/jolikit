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
package net.jolikit.time.sched.hard;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.jolikit.lang.InterfaceFactory;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.PostPaddedAtomicInteger;
import net.jolikit.lang.PostPaddedAtomicLong;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.basics.InterfaceCancellable;
import net.jolikit.threading.execs.FixedThreadExecutor;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.SystemTimeClock;
import net.jolikit.time.clocks.hard.ZeroHardClock;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Class to bench performances of (threaded) executors and schedulers.
 */
public class ExecutorsPerf {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_RUNS = 2;
    
    /**
     * Doing multiple bursts, to avoid using huge memory by scheduling
     * millions of works in one shot
     * (we don't have backpressure as with ringbuffers).
     */
    private static final int NBR_OF_BURSTS = 10;
    private static final int NBR_OF_CALLS_PER_BURST = 100 * 1000;
    
    private static final int MIN_PARALLELISM = 1;
    
    /**
     * Twice to bench core contention.
     */
    private static final int MAX_PARALLELISM =
        2 * Runtime.getRuntime().availableProcessors();
    
    /*
     * 
     */

    private static final boolean MUST_BENCH_EXECUTE = true;
    
    private static final boolean MUST_BENCH_EXECUTE_AFTER_XXX = true;
    private static final boolean MUST_BENCH_EXECUTE_AFTER_0NS = MUST_BENCH_EXECUTE_AFTER_XXX && true;
    private static final boolean MUST_BENCH_EXECUTE_AFTER_R9_0NS = MUST_BENCH_EXECUTE_AFTER_XXX && false;
    private static final boolean MUST_BENCH_EXECUTE_AFTER_R9_1NS = MUST_BENCH_EXECUTE_AFTER_XXX && false;
    
    /*
     * 
     */
    
    private static final boolean MUST_BENCH_1_N = true;
    private static final boolean MUST_BENCH_2_N = true;
    private static final boolean MUST_BENCH_N_1 = true;
    private static final boolean MUST_BENCH_N_2 = true;
    private static final boolean MUST_BENCH_N_N = true;
    
    private static final boolean MUST_BENCH_WITH_WORK = false;
    private static final int WORK_SQRT_COUNT = 10;
    
    /*
     * 
     */

    /**
     * Allows to detect performance problems
     * due to worker wakeup problems.
     */
    private static final boolean MUST_LOG_WORKERS_COUNT = true;
    
    /**
     * Caller count is multiplied by this
     * to obtain the number of run() used
     * for short time worker count.
     */
    private static final int SHORT_TIME_FACTOR = 1000;
    
    /*
     * 
     */
    
    private static final boolean MUST_BENCH_TIMER = false;

    private static final boolean MUST_BENCH_TPE_STPE = true;

    private static final boolean MUST_BENCH_HS_ADVCED_QUEUE = false;
    private static final boolean MUST_BENCH_HS_BASIC_QUEUE = false;
    private static final boolean MUST_BENCH_HS_ADAP_QUEUE = true;
    
    private static final boolean MUST_BENCH_FTE_ADVCED_QUEUE = false;
    private static final boolean MUST_BENCH_FTE_BASIC_QUEUE = false;
    private static final boolean MUST_BENCH_FTE_ADAP_QUEUE = true;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyRescheduleType {
        EXECUTE,
        EXECUTE_AFTER_0,
        EXECUTE_AFTER_1NS,
    }
    
    private static class MyExecutorData {
        private final Executor executor;
        private final String executorName;
        private final InterfaceClock clock;
        private final int nbrOfCallers;
        private final int nbrOfWorkers;
        public MyExecutorData(
            final Executor executor,
            final String executorName,
            final InterfaceClock clock,
            int nbrOfCallers,
            int nbrOfThreads) {
            this.executor = executor;
            this.executorName = executorName;
            this.clock = clock;
            this.nbrOfCallers = nbrOfCallers;
            this.nbrOfWorkers = nbrOfThreads;
        }
        public String getInfo() {
            return "(" + this.nbrOfCallers + " pub"
                + "," + this.nbrOfWorkers + " wkr"
                + "), "
                + executorName;
        }
        public void shutdown() {
            throw new UnsupportedOperationException();
        }
    }
    
    private interface MyInterfaceCallerFactory {
        public Runnable newInstance(int nbrOfCalls);
    }
    
    private class MyNShotCancellable implements InterfaceCancellable {
        private final Executor executor;
        // Not volatile, to avoid false sharing,
        // so assuming scheduler doesn't switch threads
        // without proper memory synchronization.
        private int nbrOfSchedules;
        private final MyRescheduleType rescheduleType;
        private final int nbrOfSqrt;
        private Thread firstRunningThread = null;
        private final List<Thread> workerList;
        public MyNShotCancellable(
            final Executor executor,
            int nbrOfSchedules,
            final MyRescheduleType rescheduleType,
            int nbrOfSqrt,
            final List<Thread> workerList) {
            if (nbrOfSchedules < 1) {
                throw new IllegalArgumentException();
            }
            this.executor = executor;
            this.nbrOfSchedules = nbrOfSchedules;
            this.rescheduleType = rescheduleType;
            this.nbrOfSqrt = nbrOfSqrt;
            this.workerList = workerList;
        }
        public Thread getFirstRunningThread() {
            return this.firstRunningThread;
        }
        @Override
        public void run() {
            try {
                this.innerRun();
            } finally {
                this.onDone();
            }
        }
        @Override
        public void onCancel() {
            this.onDone();
        }
        private void innerRun() {
            if (this.firstRunningThread == null) {
                this.firstRunningThread = Thread.currentThread();
            }
            if (this.workerList != null) {
                this.workerList.add(Thread.currentThread());
            }
            for (int i = this.nbrOfSqrt; --i >= 0;) {
                if (Math.sqrt(i) == 10.1) {
                    throw new RuntimeException();
                }
            }
            if ((--this.nbrOfSchedules) > 0) {
                if (this.rescheduleType == MyRescheduleType.EXECUTE) {
                    this.executor.execute(this);
                } else if (this.rescheduleType == MyRescheduleType.EXECUTE_AFTER_0) {
                    asScheduler(this.executor).executeAfterNs(this, 0L);
                } else if (this.rescheduleType == MyRescheduleType.EXECUTE_AFTER_1NS) {
                    asScheduler(this.executor).executeAfterNs(this, 1);
                } else {
                    throw new AssertionError(this.rescheduleType);
                }
            }
        }
        private void onDone() {
            if (endCounter.decrementAndGet() == 0) {
                synchronized (endMutex) {
                    endMutex.notify();
                }
            }
        }
    }
    
    private class MyNShotCancellableFactory implements InterfaceFactory<InterfaceCancellable> {
        private final Executor executor;
        private final int nbrOfSchedules;
        private final MyRescheduleType rescheduleType;
        private final int nbrOfSqrt;
        private final List<MyNShotCancellable> createdList;
        private final List<Thread> workerList;
        public MyNShotCancellableFactory(
            final Executor executor,
            int nbrOfSchedules,
            final MyRescheduleType rescheduleType,
            int nbrOfSqrt,
            final List<MyNShotCancellable> createdList,
            final List<Thread> workerList) {
            this.executor = executor;
            this.nbrOfSchedules = nbrOfSchedules;
            this.rescheduleType = rescheduleType;
            this.nbrOfSqrt = nbrOfSqrt;
            this.createdList = createdList;
            this.workerList = workerList;
        }
        @Override
        public InterfaceCancellable newInstance() {
            final MyNShotCancellable ret =
                new MyNShotCancellable(
                    this.executor,
                    this.nbrOfSchedules,
                    this.rescheduleType,
                    this.nbrOfSqrt,
                    this.workerList);
            if (this.createdList != null) {
                this.createdList.add(ret);
            }
            return ret;
        }
    }
    
    /**
     * Triggers executor through execute method.
     */
    private static class MyAsapCallerFactory implements MyInterfaceCallerFactory {
        private final Executor executor;
        private final InterfaceFactory<InterfaceCancellable> cancellableFactory;
        public MyAsapCallerFactory(
            Executor executor,
            InterfaceFactory<InterfaceCancellable> cancellableFactory) {
            this.executor = executor;
            this.cancellableFactory = cancellableFactory;
        }
        @Override
        public Runnable newInstance(final int nbrOfCalls) {
            return new Runnable() {
                @Override
                public void run() {
                    final Executor localExecutor = executor;
                    final InterfaceFactory<InterfaceCancellable> localCancellableFactory =
                        cancellableFactory;
                    for (int i = 0; i < nbrOfCalls; i++) {
                        localExecutor.execute(localCancellableFactory.newInstance());
                    }
                }
            };
        }
    }
    
    /**
     * Triggers scheduler through executeAt method, using current time.
     */
    private static class MyTimedCallerFactory implements MyInterfaceCallerFactory {
        private final InterfaceClock clock;
        private final InterfaceScheduler scheduler;
        private final InterfaceFactory<InterfaceCancellable> cancellableFactory;
        public MyTimedCallerFactory(
            InterfaceClock clock,
            InterfaceScheduler scheduler,
            InterfaceFactory<InterfaceCancellable> cancellableFactory) {
            this.clock = clock;
            this.scheduler = scheduler;
            this.cancellableFactory = cancellableFactory;
        }
        @Override
        public Runnable newInstance(final int nbrOfCalls) {
            return new Runnable() {
                @Override
                public void run() {
                    final InterfaceClock localClock = clock;
                    final InterfaceScheduler localScheduler = scheduler;
                    final InterfaceFactory<InterfaceCancellable> localCancellableFactory = cancellableFactory;
                    for (int i = 0; i < nbrOfCalls; i++) {
                        localScheduler.executeAtNs(
                            localCancellableFactory.newInstance(),
                            localClock.getTimeNs());
                    }
                }
            };
        }
    }
    
    /*
     * 
     */
    
    private static class MyConcAddList<E> extends AbstractList<E> {
        private final AtomicInteger nextIndex = new PostPaddedAtomicInteger();
        private final Object[] arr;
        public MyConcAddList(int capacity) {
            this.arr = new Object[capacity];
        }
        @Override
        public int size() {
            return this.nextIndex.get();
        }
        @Override
        public boolean add(E e) {
            final int index = this.nextIndex.getAndIncrement();
            this.arr[index] = e;
            return true;
        }
        /**
         * Must be called only once done adding,
         * after proper memory synchronization.
         */
        @Override
        public void clear() {
            final int size = this.size();
            for (int i = size; --i >= 0;) {
                this.arr[i] = null;
            }
            this.nextIndex.set(0);
        }
        /**
         * Must be called only once done adding,
         * after proper memory synchronization.
         */
        @Override
        public E get(int index) {
            final int size = this.size();
            if (index >= size) {
                throw new ArrayIndexOutOfBoundsException(index + " >= " + size);
            }
            return (E) this.arr[index];
        }
        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_CALLS =
        NbrsUtils.timesExact(NBR_OF_BURSTS, NBR_OF_CALLS_PER_BURST);
    
    private final AtomicLong endCounter = new PostPaddedAtomicLong();
    
    /**
     * Notified by scheduled that decrements counter to zero.
     */
    private final Object endMutex = new Object();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        System.out.println(TestUtils.getJVMInfo());
        newRun(args);
    }
    
    public static void newRun(String[] args) {
        new ExecutorsPerf().run();
    }
    
    public ExecutorsPerf() {
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run() {
        final long a = System.nanoTime();
        System.out.println("--- " + ExecutorsPerf.class.getSimpleName() + "... ---");
        System.out.println("number of calls = " + NBR_OF_CALLS);
        
        System.out.println("t = time elapsed up to last runnable called");
        
        this.benchThroughput();
        
        final long b = System.nanoTime();
        System.out.println("--- ..." + ExecutorsPerf.class.getSimpleName()
            + ", " + TestUtils.nsToSRounded(b-a) + " s ---");
    }
    
    private void benchThroughput() {
        @SuppressWarnings("unused")
        int nbrOfCallers;
        @SuppressWarnings("unused")
        int nbrOfWorkers;
        for (int threadCount = MIN_PARALLELISM;
            threadCount <= MAX_PARALLELISM;
            threadCount += ((threadCount < 4) ? 1 : threadCount)) {
            System.out.println();
            System.out.println("parallelism = "+threadCount);
            if (true && threadCount >= 2) {
                if (MUST_BENCH_1_N) {
                    benchThroughput(nbrOfCallers = 1, nbrOfWorkers = threadCount);
                }
                if (MUST_BENCH_2_N && threadCount >= 4) {
                    benchThroughput(nbrOfCallers = 2, nbrOfWorkers = threadCount);
                }
                if (MUST_BENCH_N_1) {
                    benchThroughput(nbrOfCallers = threadCount, nbrOfWorkers = 1);
                }
                if (MUST_BENCH_N_2 && threadCount >= 4) {
                    benchThroughput(nbrOfCallers = threadCount, nbrOfWorkers = 2);
                }
            }
            if (MUST_BENCH_N_N) {
                benchThroughput(nbrOfCallers = threadCount, nbrOfWorkers = threadCount);
            }
        }
    }
    
    private void benchThroughput(
        int nbrOfCallers,
        int nbrOfWorkers) {
        
        final ArrayList<MyExecutorData> executorDataList =
            newExecutorDataList(
                nbrOfCallers,
                nbrOfWorkers);
        
        if (MUST_BENCH_EXECUTE) {
            this.bench_execute(executorDataList, 0, null);
        }
        if (MUST_BENCH_EXECUTE_AFTER_0NS) {
            this.bench_executeAfterNs(executorDataList, 0, null);
        }
        if (MUST_BENCH_EXECUTE_AFTER_R9_0NS) {
            this.bench_executeAfterNs(executorDataList, 9, MyRescheduleType.EXECUTE_AFTER_0);
        }
        if (MUST_BENCH_EXECUTE_AFTER_R9_1NS) {
            this.bench_executeAfterNs(executorDataList, 9, MyRescheduleType.EXECUTE_AFTER_1NS);
        }
        
        for (MyExecutorData executorData : executorDataList) {
            executorData.shutdown();
        }
    }
    
    private static boolean[] withWorkArr() {
        if (MUST_BENCH_WITH_WORK) {
            return new boolean[] {false, true};
        } else {
            return new boolean[] {false};
        }
    }
    
    private void bench_execute(
        final List<MyExecutorData> executorDataList,
        int nbrOfReSchedules,
        final MyRescheduleType rescheduleType) {
        
        for (boolean withWork : withWorkArr()) {
            final int nbrOfSqrt = (withWork ? WORK_SQRT_COUNT : 0);
            
            System.out.println();
            
            for (MyExecutorData executorData : executorDataList) {
                final Executor executor = executorData.executor;
                final int nbrOfSchedules = nbrOfReSchedules + 1;
                final List<MyNShotCancellable> createdList =
                    (MUST_LOG_WORKERS_COUNT ? new MyConcAddList<>(NBR_OF_CALLS_PER_BURST) : null);
                final List<Thread> workerList =
                    (MUST_LOG_WORKERS_COUNT ? new MyConcAddList<>(NBR_OF_CALLS_PER_BURST) : null);
                final MyNShotCancellableFactory sFactory =
                    new MyNShotCancellableFactory(
                        executor,
                        nbrOfSchedules,
                        rescheduleType,
                        nbrOfSqrt,
                        createdList,
                        workerList);
                final MyAsapCallerFactory cFactory =
                    new MyAsapCallerFactory(
                        executor,
                        sFactory);
                
                for (int k = 0; k < NBR_OF_RUNS; k++) {
                    final String benchInfo = getBenchInfo(
                        "execute",
                        nbrOfReSchedules,
                        rescheduleType,
                        nbrOfSqrt,
                        k,
                        executorData);
                    this.bench_executeXXX(
                        executorData,
                        benchInfo,
                        cFactory,
                        nbrOfReSchedules,
                        createdList,
                        workerList);
                }
            }
        }
    }
    
    private void bench_executeAfterNs(
        final List<MyExecutorData> executorDataList,
        int nbrOfReSchedules,
        final MyRescheduleType rescheduleType) {
        
        for (boolean withWork : withWorkArr()) {
            final int nbrOfSqrt = (withWork ? 100 : 0);
            
            System.out.println();
            
            for (MyExecutorData executorData : executorDataList) {
                final InterfaceScheduler scheduler =
                    asSchedulerElseNull(
                        executorData.executor);
                if (scheduler == null) {
                    continue;
                }
                final int nbrOfSchedules = nbrOfReSchedules + 1;
                final List<MyNShotCancellable> createdList =
                    (MUST_LOG_WORKERS_COUNT ? new MyConcAddList<>(NBR_OF_CALLS_PER_BURST) : null);
                final List<Thread> workerList =
                    (MUST_LOG_WORKERS_COUNT ? new MyConcAddList<>(NBR_OF_CALLS_PER_BURST) : null);
                final MyNShotCancellableFactory sFactory =
                    new MyNShotCancellableFactory(
                        scheduler,
                        nbrOfSchedules,
                        rescheduleType,
                        nbrOfSqrt,
                        createdList,
                        workerList);
                final MyTimedCallerFactory cFactory =
                    new MyTimedCallerFactory(
                        executorData.clock,
                        scheduler,
                        sFactory);
                
                for (int k = 0; k < NBR_OF_RUNS; k++) {
                    final String benchInfo = getBenchInfo(
                        "executeAfterNs",
                        nbrOfReSchedules,
                        rescheduleType,
                        nbrOfSqrt,
                        k,
                        executorData);
                    this.bench_executeXXX(
                        executorData,
                        benchInfo,
                        cFactory,
                        nbrOfReSchedules,
                        createdList,
                        workerList);
                }
            }
        }
    }
    
    private void bench_executeXXX(
        MyExecutorData executorData,
        String benchInfo,
        MyInterfaceCallerFactory cFactory,
        int nbrOfReSchedules,
        List<MyNShotCancellable> createdList,
        List<Thread> workerList) {
        
        // for each test to start with about the same memory
        System.gc();
        
        final int nbrOfCallers = executorData.nbrOfCallers;
        
        final int nbrOfSchedules = nbrOfReSchedules + 1;

        final int callCountPerBurstForAllCallers = NBR_OF_CALLS_PER_BURST / nbrOfSchedules;
        // Division needs to be exact, else might wait forever.
        if (callCountPerBurstForAllCallers * nbrOfSchedules != NBR_OF_CALLS_PER_BURST) {
            throw new AssertionError();
        }

        final int minCallCountPerBurstPerCaller = callCountPerBurstForAllCallers / nbrOfCallers;
        final int callCountPerBurstForFirstCaller =
            callCountPerBurstForAllCallers - minCallCountPerBurstPerCaller * (nbrOfCallers - 1);
        
        /*
         * 
         */
        
        final Set<Thread> workerSet =
            (MUST_LOG_WORKERS_COUNT ? new HashSet<>() : null);
        // To measure whether same threads run multiple runnables
        // in sequence, of if we have a lot of context switches.
        int sumOfWorkerCountOverShortTime = 0;
        int shortTimeCount = 0;
        final Set<Thread> tmpWorkerSet =
            (MUST_LOG_WORKERS_COUNT ? new HashSet<>() : null);
        
        final long a = System.nanoTime();
        for (int kb = 0; kb < NBR_OF_BURSTS; kb++) {
            this.endCounter.set(NBR_OF_CALLS_PER_BURST);
            final ExecutorService executor = Executors.newCachedThreadPool();
            if (MUST_LOG_WORKERS_COUNT) {
                // Clear and use at each burst,
                // for it not to grow too large.
                createdList.clear();
                workerList.clear();
            }
            for (int i = 0; i < nbrOfCallers; i++) {
                final int callCountForCaller = 
                    (i == 0) ? callCountPerBurstForFirstCaller :
                        minCallCountPerBurstPerCaller;
                executor.execute(cFactory.newInstance(callCountForCaller));
            }
            this.waitForEnd();
            Unchecked.shutdownAndAwaitTermination(executor);
            if (MUST_LOG_WORKERS_COUNT) {
                for (int i = 0; i < createdList.size(); i++) {
                    final MyNShotCancellable runnable = createdList.get(i);
                    workerSet.add(runnable.getFirstRunningThread());
                }
                final int shortTimeRange = SHORT_TIME_FACTOR * nbrOfCallers;
                tmpWorkerSet.clear();
                for (int i = 0; i < workerList.size(); i++) {
                    tmpWorkerSet.add(workerList.get(i));
                    if (((i + 1) % shortTimeRange) == 0) {
                        sumOfWorkerCountOverShortTime += tmpWorkerSet.size();
                        shortTimeCount++;
                        tmpWorkerSet.clear();
                    }
                }
            }
        }
        final long b = System.nanoTime();
        System.out.println(benchInfo + " t = " + TestUtils.nsToSRounded(b-a) + " s");
        if (MUST_LOG_WORKERS_COUNT) {
            System.out.println(benchInfo
                + " total worker count = "
                + workerSet.size());
            final double meanWorkerCountOverShortTime =
                sumOfWorkerCountOverShortTime
                / (double) shortTimeCount;
            System.out.println(benchInfo
                + " short time worker count = "
                + (float) meanWorkerCountOverShortTime);
        }
    }
    
    /*
     * 
     */
    
    private static String getBenchInfo(
        String methodName,
        int nbrOfReSchedules,
        MyRescheduleType rescheduleType,
        int nbrOfSqrt,
        int k,
        MyExecutorData executorData) {
        String benchInfo = methodName;
        if (nbrOfReSchedules != 0) {
            benchInfo += ", " + nbrOfReSchedules + " " + rescheduleType;
        }
        benchInfo += (nbrOfSqrt == 0) ? "(no work)" : "(work)";
        benchInfo += ": " + executorData.getInfo();
        benchInfo += " : (run " + (k + 1) + ")";
        return benchInfo;
    }
    
    private void waitForEnd() {
        if (endCounter.get() > 0) {
            synchronized (endMutex) {
                while (endCounter.get() > 0) {
                    try {
                        endMutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static ArrayList<MyExecutorData> newExecutorDataList(
        final int nbrOfCallers,
        final int nbrOfWorkers) {
        
        final ArrayList<MyExecutorData> executorDataList = new ArrayList<MyExecutorData>();
        
        if (MUST_BENCH_TIMER && (nbrOfWorkers == 1)) {
            final SystemTimeClock clock = new SystemTimeClock();
            final TimerHardScheduler scheduler = new TimerHardScheduler(clock);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
                "HARD_SCHED_TIMER",
                clock,
                nbrOfCallers,
                1) { // Timer is mono-threaded.
                @Override
                public void shutdown() {
                    scheduler.shutdown();
                }
            };
            executorDataList.add(data);
        }
        if (MUST_BENCH_TPE_STPE) {
            final SystemTimeClock clock = new SystemTimeClock();
            final TpeStpeHardScheduler scheduler =
                new TpeStpeHardScheduler(
                    clock,
                    nbrOfWorkers,
                    nbrOfWorkers);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
                "HARD_SCHED_TPE_STPE",
                clock,
                nbrOfCallers,
                nbrOfWorkers) {
                @Override
                public void shutdown() {
                    scheduler.shutdown();
                }
            };
            executorDataList.add(data);
        }
        if (MUST_BENCH_HS_ADVCED_QUEUE) {
            final SystemTimeClock clock = new SystemTimeClock();
            final int maxWorkerCountForBasicAsapQueue = 0;
            final HardScheduler scheduler =
                new HardScheduler(
                    clock,
                    "THREAD",
                    true,
                    nbrOfWorkers,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    maxWorkerCountForBasicAsapQueue,
                    null);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
                "HS_ADVCED",
                clock,
                nbrOfCallers,
                nbrOfWorkers) {
                @Override
                public void shutdown() {
                    scheduler.shutdown();
                }
            };
            executorDataList.add(data);
        }
        if (MUST_BENCH_HS_BASIC_QUEUE) {
            final SystemTimeClock clock = new SystemTimeClock();
            final int maxWorkerCountForBasicAsapQueue = Integer.MAX_VALUE;
            final HardScheduler scheduler =
                new HardScheduler(
                    clock,
                    "THREAD",
                    true,
                    nbrOfWorkers,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    maxWorkerCountForBasicAsapQueue,
                    null);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
                "HS_BASIC",
                clock,
                nbrOfCallers,
                nbrOfWorkers) {
                @Override
                public void shutdown() {
                    scheduler.shutdown();
                }
            };
            executorDataList.add(data);
        }
        if (MUST_BENCH_HS_ADAP_QUEUE) {
            final SystemTimeClock clock = new SystemTimeClock();
            final HardScheduler scheduler =
                HardScheduler.newInstance(
                    clock,
                    "THREAD",
                    true,
                    nbrOfWorkers);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
                "HS_ADAP",
                clock,
                nbrOfCallers,
                nbrOfWorkers) {
                @Override
                public void shutdown() {
                    scheduler.shutdown();
                }
            };
            executorDataList.add(data);
        }
        if (MUST_BENCH_FTE_ADVCED_QUEUE) {
            final ZeroHardClock clock = new ZeroHardClock();
            final int maxWorkerCountForBasicQueue = 0;
            final FixedThreadExecutor executor =
                new FixedThreadExecutor(
                    "THREAD",
                    true,
                    nbrOfWorkers,
                    Integer.MAX_VALUE,
                    maxWorkerCountForBasicQueue,
                    null);
            final MyExecutorData data = new MyExecutorData(
                executor,
                "FTE_ADVCED",
                clock,
                nbrOfCallers,
                nbrOfWorkers) {
                @Override
                public void shutdown() {
                    executor.shutdown();
                }
            };
            executorDataList.add(data);
        }
        if (MUST_BENCH_FTE_BASIC_QUEUE) {
            final ZeroHardClock clock = new ZeroHardClock();
            final int maxWorkerCountForBasicQueue = Integer.MAX_VALUE;
            final FixedThreadExecutor executor =
                new FixedThreadExecutor(
                    "THREAD",
                    true,
                    nbrOfWorkers,
                    Integer.MAX_VALUE,
                    maxWorkerCountForBasicQueue,
                    null);
            final MyExecutorData data = new MyExecutorData(
                executor,
                "FTE_BASIC",
                clock,
                nbrOfCallers,
                nbrOfWorkers) {
                @Override
                public void shutdown() {
                    executor.shutdown();
                }
            };
            executorDataList.add(data);
        }
        if (MUST_BENCH_FTE_ADAP_QUEUE) {
            final ZeroHardClock clock = new ZeroHardClock();
            final FixedThreadExecutor executor =
                FixedThreadExecutor.newInstance(
                    "THREAD",
                    true,
                    nbrOfWorkers);
            final MyExecutorData data = new MyExecutorData(
                executor,
                "FTE_ADAP",
                clock,
                nbrOfCallers,
                nbrOfWorkers) {
                @Override
                public void shutdown() {
                    executor.shutdown();
                }
            };
            executorDataList.add(data);
        }
        
        return executorDataList;
    }

    /*
     * 
     */
    
    private static InterfaceScheduler asScheduler(Executor executor) {
        return (InterfaceScheduler) executor;
    }
    
    private static InterfaceScheduler asSchedulerElseNull(Executor executor) {
        final InterfaceScheduler ret;
        if (executor instanceof InterfaceScheduler) {
            ret = (InterfaceScheduler) executor;
        } else {
            ret = null;
        }
        return ret;
    }
}
