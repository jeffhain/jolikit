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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import net.jolikit.lang.InterfaceFactory;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.hard.SystemTimeClock;
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
        private final int nbrOfThreads;
        private final String info;
        public MyExecutorData(
            final Executor executor,
            final InterfaceClock clock,
            int nbrOfThreads,
            final String info) {
            this.executor = executor;
            this.clock = clock;
            this.nbrOfThreads = nbrOfThreads;
            this.info = info;
        }
        @Override
        public String toString() {
            return "[" + this.info + ", " + this.nbrOfThreads + " thread(s)]";
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
                    // will have one more sequence of calls to run
                    endCounter.incrementAndGet();
                    this.executor.execute(this);
                } else if (this.rescheduleType == MyRescheduleType.EXECUTE_AFTER_0) {
                    // will have one more sequence of calls to run
                    endCounter.incrementAndGet();
                    asScheduler(this.executor).executeAfterNs(this, 0L);
                } else if (this.rescheduleType == MyRescheduleType.EXECUTE_AFTER_1NS) {
                    // will have one more sequence of calls to run
                    endCounter.incrementAndGet();
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
    
    /**
     * Little hack for new line when number of threads changes.
     */
    private static final MyExecutorData BLANK_LINE = new MyExecutorData(null, null, 0, null);
    
    private static final int MAX_NBR_OF_THREADS = 2 * Runtime.getRuntime().availableProcessors();
    
    private final AtomicLong endCounter = new AtomicLong();
    
    /**
     * Notified by scheduled that decrements counter to zero.
     */
    private final Object endMutex = new Object();
    
    protected int nbrOfK = 2;
    protected int nbrOfRuns = 100;
    
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
        
        final HardExecutorsPerf tester = new HardExecutorsPerf();
        tester.nbrOfK = 1;
        tester.nbrOfRuns = 10;
        
        System.out.println("t1 = time elapsed up to last executeXxx called");
        System.out.println("t2 = time elapsed up to last runnable called");
        
        final InterfaceHardClock clock = new SystemTimeClock();
        
        /*
         * 
         */
        
        final ArrayList<MyExecutorData> executorDataList = new ArrayList<MyExecutorData>();
        for (int nbrOfThreads = 1; nbrOfThreads <= MAX_NBR_OF_THREADS; nbrOfThreads *= 2) {
            if (true && (nbrOfThreads == 1)) {
                TimerHardScheduler scheduler = new TimerHardScheduler(clock);
                MyExecutorData data = new MyExecutorData(
                    scheduler,
                    clock,
                    1, // Timer is mono-threaded.
                    scheduler.getClass().getSimpleName());
                executorDataList.add(data);
            }
            if (true) {
                ExecutorHardScheduler scheduler = new ExecutorHardScheduler(clock, nbrOfThreads, nbrOfThreads);
                MyExecutorData data = new MyExecutorData(
                    scheduler,
                    clock,
                    nbrOfThreads,
                    scheduler.getClass().getSimpleName());
                executorDataList.add(data);
            }
            if (true) {
                HardScheduler scheduler = HardScheduler.newInstance(clock, "THREAD", true, nbrOfThreads);
                MyExecutorData data = new MyExecutorData(
                    scheduler,
                    clock,
                    nbrOfThreads,
                    scheduler.getClass().getSimpleName());
                executorDataList.add(data);
            }
            if (true) {
                HardExecutor executor = HardExecutor.newInstance("THREAD", true, nbrOfThreads);
                MyExecutorData data = new MyExecutorData(
                    executor,
                    clock,
                    nbrOfThreads,
                    executor.getClass().getSimpleName());
                executorDataList.add(data);
            }
            
            executorDataList.add(BLANK_LINE);
        }
        
        /*
         * 
         */
        
        if (true) {
            tester.bench_execute(executorDataList, 0, null);
        }
        if (true) {
            tester.bench_executeAfterNs(executorDataList, 0, null);
        }
        if (true) {
            tester.bench_executeAfterNs(executorDataList, 9, MyRescheduleType.EXECUTE_AFTER_0);
        }
        if (true) {
            tester.bench_executeAfterNs(executorDataList, 9, MyRescheduleType.EXECUTE_AFTER_1NS);
        }
        
        System.out.println("--- ..." + HardExecutorsPerf.class.getSimpleName() + " ---");
    }
    
    private void bench_execute(
        final Collection<MyExecutorData> schedulersData,
        int nbrOfReSchedules,
        final MyRescheduleType rescheduleType) {
        
        int nbrOfSchedules = nbrOfReSchedules+1;
        int nbrOfCallsPerRun = 100000/nbrOfSchedules;
        
        for (int nbrOfSqrt = 0; nbrOfSqrt <= 100; nbrOfSqrt += 100) {
            System.out.println("");
            for (MyExecutorData schedulerData : schedulersData) {
                if (schedulerData == BLANK_LINE) {
                    System.out.println("");
                    continue;
                }
                final Executor executor = schedulerData.executor;
                MyNShotCancellableFactory sFactory = new MyNShotCancellableFactory(
                    executor,
                    nbrOfSchedules,
                    rescheduleType,
                    nbrOfSqrt);
                MyAsapCallerFactory cFactory = new MyAsapCallerFactory(
                    executor,
                    sFactory);
                
                String benchInfo = getBenchInfo("execute", nbrOfReSchedules, rescheduleType, nbrOfSqrt);
                for (int k = 0; k < nbrOfK; k++) {
                    this.bench_executeXXX(schedulerData, benchInfo, cFactory, nbrOfRuns, nbrOfCallsPerRun);
                }
            }
        }
    }
    
    private void bench_executeAfterNs(
        final List<MyExecutorData> executorDataList,
        int nbrOfReSchedules,
        final MyRescheduleType rescheduleType) {
        
        int nbrOfSchedules = nbrOfReSchedules+1;
        int nbrOfCallsPerRun = 100000/nbrOfSchedules;
        
        for (int nbrOfSqrt = 0; nbrOfSqrt <= 100; nbrOfSqrt += 100) {
            System.out.println("");
            for (MyExecutorData schedulerData : executorDataList) {
                if (schedulerData == BLANK_LINE) {
                    System.out.println("");
                    continue;
                }
                final InterfaceScheduler scheduler =
                    asSchedulerElseNull(
                        schedulerData.executor);
                if (scheduler == null) {
                    continue;
                }
                MyNShotCancellableFactory sFactory = new MyNShotCancellableFactory(
                    scheduler,
                    nbrOfSchedules,
                    rescheduleType,
                    nbrOfSqrt);
                MyTimedCallerFactory cFactory = new MyTimedCallerFactory(
                    schedulerData.clock,
                    scheduler,
                    sFactory);
                
                String benchInfo = getBenchInfo("executeAfterNs", nbrOfReSchedules, rescheduleType, nbrOfSqrt);
                for (int k = 0; k < nbrOfK; k++) {
                    this.bench_executeXXX(schedulerData, benchInfo, cFactory, nbrOfRuns, nbrOfCallsPerRun);
                }
            }
        }
    }
    
    /**
     * Doing multiple runs, to avoid using huge memory by scheduling
     * millions of works in one shot.
     */
    private void bench_executeXXX(
        MyExecutorData schedulerData,
        String benchInfo,
        MyInterfaceCallerFactory cFactory,
        int nbrOfRuns,
        int nbrOfCallsPerRun) {
        
        // for each test to start with about the same memory
        System.gc();
        
        final int nbrOfCallingThreads = schedulerData.nbrOfThreads;
        
        final int nbrOfCallsPerThread = nbrOfCallsPerRun / nbrOfCallingThreads;
        // Recomputing number of calls per run,
        // for it to be exact, not to stall waiting
        // for calls that won't be made.
        nbrOfCallsPerRun = nbrOfCallsPerThread * nbrOfCallingThreads;
        
        /*
         * 
         */
        
        long callsDurationNs = 0;
        long a = System.nanoTime();
        for (int k = 0; k < nbrOfRuns; k++) {
            this.endCounter.set(nbrOfCallsPerRun);
            ExecutorService executor = Executors.newCachedThreadPool();
            long u = System.nanoTime();
            for (int t = 0; t < nbrOfCallingThreads; t++) {
                executor.execute(cFactory.newInstance(nbrOfCallsPerThread));
            }
            Unchecked.shutdownAndAwaitTermination(executor);
            long v = System.nanoTime();
            callsDurationNs += (v-u);
            this.waitForEnd();
        }
        long b = System.nanoTime();
        System.out.println(benchInfo + ": " + schedulerData + ": t1 = " + TestUtils.nsToSRounded(callsDurationNs) + " s");
        System.out.println(benchInfo + ": " + schedulerData + ": t2 = " + TestUtils.nsToSRounded(b-a) + " s");
    }
    
    /*
     * 
     */
    
    private static String getBenchInfo(String methodName, int nbrOfReSchedules, MyRescheduleType rescheduleType, int nbrOfSqrt) {
        String benchInfo = methodName;
        if (nbrOfReSchedules != 0) {
            benchInfo += ", " + nbrOfReSchedules + " " + rescheduleType;
        }
        benchInfo += (nbrOfSqrt == 0) ? "(no work)" : "(work)";
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
