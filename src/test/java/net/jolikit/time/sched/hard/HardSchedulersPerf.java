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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import net.jolikit.lang.InterfaceFactory;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.hard.SystemTimeClock;
import net.jolikit.time.sched.InterfaceSchedulable;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceScheduling;

/**
 * Class to bench performances of hard schedulers.
 */
public class HardSchedulersPerf {

    //--------------------------------------------------------------------------
    // PROTECTED CLASSES
    //--------------------------------------------------------------------------
    
    protected enum RescheduleType {
        RE_PROCESS,
        EXECUTE,
        EXECUTE_AT_NOW,
        EXECUTE_AT_SOON,
    }

    protected static class SchedulerData {
        private final InterfaceScheduler scheduler;
        private final InterfaceClock clock;
        private final int nbrOfThreads;
        private final String info;
        public SchedulerData(
                final InterfaceScheduler scheduler,
                final InterfaceClock clock,
                int nbrOfThreads,
                final String info) {
            this.scheduler = scheduler;
            this.clock = clock;
            this.nbrOfThreads = nbrOfThreads;
            this.info = info;
        }
        @Override
        public String toString() {
            return "[" + this.info + ", " + this.nbrOfThreads + " thread(s)]";
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private interface MyInterfaceCallerFactory {
        public Runnable newInstance(int nbrOfCalls);
    }
    
    private class MyNShotSchedulable implements InterfaceSchedulable {
        private final InterfaceScheduler scheduler;
        private InterfaceScheduling scheduling;
        // volatile in case scheduler switches threads
        // without memory synchronization
        private volatile int nbrOfSchedules;
        private final RescheduleType rescheduleType;
        private final int nbrOfSqrt;
        public MyNShotSchedulable(
                final InterfaceScheduler scheduler,
                int nbrOfSchedules,
                final RescheduleType rescheduleType,
                int nbrOfSqrt) {
            if (nbrOfSchedules < 1) {
                throw new IllegalArgumentException();
            }
            this.scheduler = scheduler;
            this.nbrOfSchedules = nbrOfSchedules;
            this.rescheduleType = rescheduleType;
            this.nbrOfSqrt = nbrOfSqrt;
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
            
            boolean mustRepeat = false;
            try {
                final long nextNs = this.innerProcess(theoreticalTimeNs, actualTimeNs);
                mustRepeat = (nextNs != MY_NO_REPEAT_NEXT_NS);
                if (mustRepeat) {
                    scheduling.setNextTheoreticalTimeNs(nextNs);
                }
            } finally {
                if (!mustRepeat) {
                    this.onDone();
                }
            }
        }
        @Override
        public void onCancel() {
            this.onDone();
        }
        private long innerProcess(
                long theoreticalTimeNs,
                long actualTimeNs) {
            for (int i = this.nbrOfSqrt; --i >= 0;) {
                if (Math.sqrt(i) == 10.1) {
                    throw new RuntimeException();
                }
            }
            if ((--this.nbrOfSchedules) == 0) {
                return MY_NO_REPEAT_NEXT_NS;
            }
            if (this.rescheduleType == RescheduleType.RE_PROCESS) {
                return actualTimeNs;
            } else if (this.rescheduleType == RescheduleType.EXECUTE) {
                // will have one more sequence of calls to run
                endCounter.incrementAndGet();
                this.scheduler.execute(this);
                return MY_NO_REPEAT_NEXT_NS;
            } else if (this.rescheduleType == RescheduleType.EXECUTE_AT_NOW) {
                // will have one more sequence of calls to run
                endCounter.incrementAndGet();
                this.scheduler.executeAtNs(this, actualTimeNs);
                return MY_NO_REPEAT_NEXT_NS;
            } else if (this.rescheduleType == RescheduleType.EXECUTE_AT_SOON) {
                // will have one more sequence of calls to run
                endCounter.incrementAndGet();
                this.scheduler.executeAtNs(this, actualTimeNs + 1);
                return MY_NO_REPEAT_NEXT_NS;
            } else {
                throw new AssertionError(this.rescheduleType);
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
    
    private class MyNShotSchedulableFactory implements InterfaceFactory<InterfaceSchedulable> {
        private final InterfaceScheduler scheduler;
        private final int nbrOfSchedules;
        private final RescheduleType rescheduleType;
        private final int nbrOfSqrt;
        public MyNShotSchedulableFactory(
                final InterfaceScheduler scheduler,
                int nbrOfSchedules,
                final RescheduleType rescheduleType,
                int nbrOfSqrt) {
            this.scheduler = scheduler;
            this.nbrOfSchedules = nbrOfSchedules;
            this.rescheduleType = rescheduleType;
            this.nbrOfSqrt = nbrOfSqrt;
        }
        @Override
        public InterfaceSchedulable newInstance() {
            return new MyNShotSchedulable(
                    this.scheduler,
                    this.nbrOfSchedules,
                    this.rescheduleType,
                    this.nbrOfSqrt);
        }
    }

    /**
     * Triggers scheduler through execute method.
     */
    private static class MyAsapCallerFactory implements MyInterfaceCallerFactory {
        private final InterfaceScheduler scheduler;
        private final InterfaceFactory<InterfaceSchedulable> scheduledFactory;
        public MyAsapCallerFactory(
                InterfaceScheduler scheduler,
                 InterfaceFactory<InterfaceSchedulable> scheduledFactory) {
            this.scheduler = scheduler;
            this.scheduledFactory = scheduledFactory;
        }
        @Override
        public Runnable newInstance(final int nbrOfCalls) {
            return new Runnable() {
                @Override
                public void run() {
                    final InterfaceScheduler localScheduler = scheduler;
                    final InterfaceFactory<InterfaceSchedulable> localScheduledFactory = scheduledFactory;
                    for (int i = 0; i < nbrOfCalls; i++) {
                        localScheduler.execute(localScheduledFactory.newInstance());
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
        private final InterfaceFactory<InterfaceSchedulable> scheduledFactory;
        public MyTimedCallerFactory(
                InterfaceClock clock,
                InterfaceScheduler scheduler,
                InterfaceFactory<InterfaceSchedulable> scheduledFactory) {
            this.clock = clock;
            this.scheduler = scheduler;
            this.scheduledFactory = scheduledFactory;
        }
        @Override
        public Runnable newInstance(final int nbrOfCalls) {
            return new Runnable() {
                @Override
                public void run() {
                    final InterfaceClock localClock = clock;
                    final InterfaceScheduler localScheduler = scheduler;
                    final InterfaceFactory<InterfaceSchedulable> localScheduledFactory = scheduledFactory;
                    for (int i = 0; i < nbrOfCalls; i++) {
                        localScheduler.executeAtNs(
                                localScheduledFactory.newInstance(),
                                localClock.getTimeNs());
                    }
                }
            };
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final long MY_NO_REPEAT_NEXT_NS = Long.MAX_VALUE;
    
    /**
     * Little hack for new line when number of threads changes.
     */
    private static final SchedulerData BLANK_LINE = new SchedulerData(null, null, 0, null);
    
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
        new HardSchedulersPerf().run(args);
    }
    
    public HardSchedulersPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run(String[] args) {
        System.out.println("--- " + HardSchedulersPerf.class.getSimpleName() + "... ---");
        
        final HardSchedulersPerf tester = new HardSchedulersPerf();
        tester.nbrOfK = 1;
        tester.nbrOfRuns = 10;
        
        System.out.println("t1 = time elapsed up to last executeXXX called");
        System.out.println("t2 = time elapsed up to last schedulable called");

        final InterfaceHardClock clock = new SystemTimeClock();

        /*
         * 
         */

        final ArrayList<SchedulerData> schedulersData = new ArrayList<SchedulerData>();
        for (int nbrOfThreads = 1; nbrOfThreads <= MAX_NBR_OF_THREADS; nbrOfThreads *= 2) {
            if (nbrOfThreads == 1) {
                TimerHardScheduler scheduler = new TimerHardScheduler(clock);
                SchedulerData data = new SchedulerData(
                        scheduler,
                        clock,
                        1, // Timer is mono-threaded.
                        scheduler.getClass().getSimpleName());
                schedulersData.add(data);
            }
            if (true) {
                ExecutorHardScheduler scheduler = new ExecutorHardScheduler(clock, nbrOfThreads, nbrOfThreads);
                SchedulerData data = new SchedulerData(
                        scheduler,
                        clock,
                        nbrOfThreads,
                        scheduler.getClass().getSimpleName());
                schedulersData.add(data);
            }
            if (true) {
                HardScheduler scheduler = HardScheduler.newInstance(clock, "THREAD", true, nbrOfThreads);
                SchedulerData data = new SchedulerData(
                        scheduler,
                        clock,
                        nbrOfThreads,
                        scheduler.getClass().getSimpleName());
                schedulersData.add(data);
            }

            schedulersData.add(BLANK_LINE);
        }

        /*
         * 
         */

        tester.bench_execute(schedulersData, 0, null);
        tester.bench_execute(schedulersData, 9, RescheduleType.RE_PROCESS);
        tester.bench_executeAtNs(schedulersData, 0, null);
        tester.bench_executeAtNs(schedulersData, 9, RescheduleType.RE_PROCESS);
        tester.bench_executeAtNs(schedulersData, 9, RescheduleType.EXECUTE_AT_NOW);
        tester.bench_executeAtNs(schedulersData, 9, RescheduleType.EXECUTE_AT_SOON);

        System.out.println("--- ..." + HardSchedulersPerf.class.getSimpleName() + " ---");
    }

    private void bench_execute(
            final Collection<SchedulerData> schedulersData,
            int nbrOfReSchedules,
            final RescheduleType rescheduleType) {

        int nbrOfSchedules = nbrOfReSchedules+1;
        int nbrOfCallsPerRun = 100000/nbrOfSchedules;

        for (int nbrOfSqrt = 0; nbrOfSqrt <= 100; nbrOfSqrt += 100) {
            System.out.println("");
            for (SchedulerData schedulerData : schedulersData) {
                if (schedulerData == BLANK_LINE) {
                    System.out.println("");
                    continue;
                }
                final InterfaceScheduler scheduler = schedulerData.scheduler;
                MyNShotSchedulableFactory sFactory = new MyNShotSchedulableFactory(
                        scheduler,
                        nbrOfSchedules,
                        rescheduleType,
                        nbrOfSqrt);
                MyAsapCallerFactory cFactory = new MyAsapCallerFactory(
                        scheduler,
                        sFactory);

                String benchInfo = getBenchInfo("execute", nbrOfReSchedules, rescheduleType, nbrOfSqrt);
                for (int k = 0; k < nbrOfK; k++) {
                    this.bench_executeXXX(schedulerData, benchInfo, cFactory, nbrOfRuns, nbrOfCallsPerRun);
                }
            }
        }
    }
    
    private void bench_executeAtNs(
            final Collection<SchedulerData> schedulersData,
            int nbrOfReSchedules,
            final RescheduleType rescheduleType) {
        
        int nbrOfSchedules = nbrOfReSchedules+1;
        int nbrOfCallsPerRun = 100000/nbrOfSchedules;
        
        for (int nbrOfSqrt = 0; nbrOfSqrt <= 100; nbrOfSqrt += 100) {
            System.out.println("");
            for (SchedulerData schedulerData : schedulersData) {
                if (schedulerData == BLANK_LINE) {
                    System.out.println("");
                    continue;
                }
                final InterfaceScheduler scheduler = schedulerData.scheduler;
                MyNShotSchedulableFactory sFactory = new MyNShotSchedulableFactory(
                        scheduler,
                        nbrOfSchedules,
                        rescheduleType,
                        nbrOfSqrt);
                MyTimedCallerFactory cFactory = new MyTimedCallerFactory(
                        schedulerData.clock,
                        scheduler,
                        sFactory);

                String benchInfo = getBenchInfo("executeAt", nbrOfReSchedules, rescheduleType, nbrOfSqrt);
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
            SchedulerData schedulerData,
            String benchInfo,
            MyInterfaceCallerFactory cFactory,
            int nbrOfRuns,
            int nbrOfCallsPerRun) {
        
        // for each test to start with about the same memory
        System.gc();

        final int nbrOfCallingThreads = schedulerData.nbrOfThreads;
        
        final int nbrOfCallsPerThread = nbrOfCallsPerRun / nbrOfCallingThreads;
        
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
    
    private static String getBenchInfo(String methodName, int nbrOfReSchedules, RescheduleType rescheduleType, int nbrOfSqrt) {
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
}
