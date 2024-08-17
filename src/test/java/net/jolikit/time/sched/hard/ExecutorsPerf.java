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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import net.jolikit.lang.InterfaceFactory;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.SystemTimeClock;
import net.jolikit.time.clocks.hard.ZeroHardClock;
import net.jolikit.time.sched.InterfaceCancellable;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Class to bench performances of hard executors,
 * including schedulers.
 */
public class HardExecutorsPerf {
    
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
        private final InterfaceClock clock;
        private final int nbrOfCallers;
        private final int nbrOfWorkers;
        public MyExecutorData(
            final Executor executor,
            final InterfaceClock clock,
            int nbrOfCallers,
            int nbrOfThreads) {
            this.executor = executor;
            this.clock = clock;
            this.nbrOfCallers = nbrOfCallers;
            this.nbrOfWorkers = nbrOfThreads;
        }
        public String getInfo() {
            return "[" + this.executor.getClass().getSimpleName()
                + ", " + this.nbrOfCallers + " caller(s)"
                + ", " + this.nbrOfWorkers + " worker(s)"
                + ")]";
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
        // volatile in case scheduler switches threads
        // without memory synchronization
        private volatile int nbrOfSchedules;
        private final MyRescheduleType rescheduleType;
        private final int nbrOfSqrt;
        public MyNShotCancellable(
            final Executor executor,
            int nbrOfSchedules,
            final MyRescheduleType rescheduleType,
            int nbrOfSqrt) {
            if (nbrOfSchedules < 1) {
                throw new IllegalArgumentException();
            }
            this.executor = executor;
            this.nbrOfSchedules = nbrOfSchedules;
            this.rescheduleType = rescheduleType;
            this.nbrOfSqrt = nbrOfSqrt;
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
        public MyNShotCancellableFactory(
            final Executor executor,
            int nbrOfSchedules,
            final MyRescheduleType rescheduleType,
            int nbrOfSqrt) {
            this.executor = executor;
            this.nbrOfSchedules = nbrOfSchedules;
            this.rescheduleType = rescheduleType;
            this.nbrOfSqrt = nbrOfSqrt;
        }
        @Override
        public InterfaceCancellable newInstance() {
            return new MyNShotCancellable(
                this.executor,
                this.nbrOfSchedules,
                this.rescheduleType,
                this.nbrOfSqrt);
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
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MIN_PARALLELISM = 1;
    
    /**
     * Twice to bench core contention.
     */
    private static final int MAX_PARALLELISM = 2 * Runtime.getRuntime().availableProcessors();
    
    private static final int NBR_OF_RUNS = 2;
    
    /**
     * Doing multiple bursts, to avoid using huge memory by scheduling
     * millions of works in one shot
     * (we don't have backpressure as with ringbuffers).
     */
    private static final int NBR_OF_BURSTS = 10;
    private static final int NBR_OF_CALLS_PER_BURST = 100 * 1000;
    private static final int NBR_OF_CALLS = NBR_OF_BURSTS * NBR_OF_CALLS_PER_BURST;
    
    private final AtomicLong endCounter = new AtomicLong();
    
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
        new HardExecutorsPerf().run();
    }
    
    public HardExecutorsPerf() {
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run() {
        System.out.println("--- " + HardExecutorsPerf.class.getSimpleName() + "... ---");
        System.out.println("number of calls = " + NBR_OF_CALLS);
        
        System.out.println("t1 = time elapsed up to last executeXxx called");
        System.out.println("t2 = time elapsed up to last runnable called");
        
        this.benchThroughput();
        
        System.out.println("--- ..." + HardExecutorsPerf.class.getSimpleName() + " ---");
    }
    
    private void benchThroughput() {
        @SuppressWarnings("unused")
        int nbrOfCallers;
        @SuppressWarnings("unused")
        int nbrOfWorkers;
        for (int parallelism = MIN_PARALLELISM; parallelism <= MAX_PARALLELISM; parallelism *= 2) {
            System.out.println();
            System.out.println("parallelism = "+parallelism);
            if (true && parallelism >= 2) {
                if (true) {
                    benchThroughput(nbrOfCallers = 1, nbrOfWorkers = parallelism);
                }
                if (true && parallelism >= 4) {
                    benchThroughput(nbrOfCallers = 2, nbrOfWorkers = parallelism);
                }
                if (true) {
                    benchThroughput(nbrOfCallers = parallelism, nbrOfWorkers = 1);
                }
                if (true && parallelism >= 4) {
                    benchThroughput(nbrOfCallers = parallelism, nbrOfWorkers = 2);
                }
            }
            if (true) {
                benchThroughput(nbrOfCallers = parallelism, nbrOfWorkers = parallelism);
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
        
        if (true) {
            this.bench_execute(executorDataList, 0, null);
        }
        if (true) {
            this.bench_executeAfterNs(executorDataList, 0, null);
        }
        if (true) {
            this.bench_executeAfterNs(executorDataList, 9, MyRescheduleType.EXECUTE_AFTER_0);
        }
        if (true) {
            this.bench_executeAfterNs(executorDataList, 9, MyRescheduleType.EXECUTE_AFTER_1NS);
        }
        
        for (MyExecutorData executorData : executorDataList) {
            executorData.shutdown();
        }
    }
    
    private void bench_execute(
        final List<MyExecutorData> executorDataList,
        int nbrOfReSchedules,
        final MyRescheduleType rescheduleType) {
        
        for (boolean withWork : new boolean[] {false, true}) {
            final int nbrOfSqrt = (withWork ? 100 : 0);
            
            System.out.println();
            
            for (MyExecutorData executorData : executorDataList) {
                final Executor executor = executorData.executor;
                final int nbrOfSchedules = nbrOfReSchedules + 1;
                MyNShotCancellableFactory sFactory = new MyNShotCancellableFactory(
                    executor,
                    nbrOfSchedules,
                    rescheduleType,
                    nbrOfSqrt);
                MyAsapCallerFactory cFactory = new MyAsapCallerFactory(
                    executor,
                    sFactory);
                
                String benchInfo = getBenchInfo(
                    "execute",
                    nbrOfReSchedules,
                    rescheduleType,
                    nbrOfSqrt,
                    executorData);
                for (int k = 0; k < NBR_OF_RUNS; k++) {
                    this.bench_executeXXX(
                        executorData,
                        benchInfo,
                        cFactory,
                        nbrOfReSchedules);
                }
            }
        }
    }
    
    private void bench_executeAfterNs(
        final List<MyExecutorData> executorDataList,
        int nbrOfReSchedules,
        final MyRescheduleType rescheduleType) {
        
        for (boolean withWork : new boolean[] {false, true}) {
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
                MyNShotCancellableFactory sFactory = new MyNShotCancellableFactory(
                    scheduler,
                    nbrOfSchedules,
                    rescheduleType,
                    nbrOfSqrt);
                MyTimedCallerFactory cFactory = new MyTimedCallerFactory(
                    executorData.clock,
                    scheduler,
                    sFactory);
                
                String benchInfo = getBenchInfo(
                    "executeAfterNs",
                    nbrOfReSchedules,
                    rescheduleType,
                    nbrOfSqrt,
                    executorData);
                for (int k = 0; k < NBR_OF_RUNS; k++) {
                    this.bench_executeXXX(
                        executorData,
                        benchInfo,
                        cFactory,
                        nbrOfReSchedules);
                }
            }
        }
    }
    
    private void bench_executeXXX(
        MyExecutorData executorData,
        String benchInfo,
        MyInterfaceCallerFactory cFactory,
        int nbrOfReSchedules) {
        
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
        
        long callsDurationNs = 0;
        final long a = System.nanoTime();
        for (int kb = 0; kb < NBR_OF_BURSTS; kb++) {
            this.endCounter.set(NBR_OF_CALLS_PER_BURST);
            final ExecutorService executor = Executors.newCachedThreadPool();
            final long u = System.nanoTime();
            for (int i = 0; i < nbrOfCallers; i++) {
                final int callCountForCaller = 
                    (i == 0) ? callCountPerBurstForFirstCaller :
                        minCallCountPerBurstPerCaller;
                executor.execute(cFactory.newInstance(callCountForCaller));
            }
            this.waitForEnd();
            final long v = System.nanoTime();
            callsDurationNs += (v-u);
            Unchecked.shutdownAndAwaitTermination(executor);
        }
        final long b = System.nanoTime();
        System.out.println(benchInfo + ": t1 = " + TestUtils.nsToSRounded(callsDurationNs) + " s");
        System.out.println(benchInfo + ": t2 = " + TestUtils.nsToSRounded(b-a) + " s");
    }
    
    /*
     * 
     */
    
    private static String getBenchInfo(
        String methodName,
        int nbrOfReSchedules,
        MyRescheduleType rescheduleType,
        int nbrOfSqrt,
        MyExecutorData executorData) {
        String benchInfo = methodName;
        if (nbrOfReSchedules != 0) {
            benchInfo += ", " + nbrOfReSchedules + " " + rescheduleType;
        }
        benchInfo += (nbrOfSqrt == 0) ? "(no work)" : "(work)";
        benchInfo += ": " + executorData.getInfo();
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
        
        if (true && (nbrOfWorkers == 1)) {
            final SystemTimeClock clock = new SystemTimeClock();
            final TimerHardScheduler scheduler = new TimerHardScheduler(clock);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
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
        if (true) {
            final SystemTimeClock clock = new SystemTimeClock();
            final ExecutorHardScheduler scheduler =
                new ExecutorHardScheduler(
                    clock,
                    nbrOfWorkers,
                    nbrOfWorkers);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
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
        if (true) {
            final SystemTimeClock clock = new SystemTimeClock();
            final HardScheduler scheduler =
                HardScheduler.newInstance(
                    clock,
                    "THREAD",
                    true,
                    nbrOfWorkers);
            final MyExecutorData data = new MyExecutorData(
                scheduler,
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
        if (true) {
            final ZeroHardClock clock = new ZeroHardClock();
            final HardExecutor executor =
                HardExecutor.newInstance(
                    "THREAD",
                    true,
                    nbrOfWorkers);
            final MyExecutorData data = new MyExecutorData(
                executor,
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
