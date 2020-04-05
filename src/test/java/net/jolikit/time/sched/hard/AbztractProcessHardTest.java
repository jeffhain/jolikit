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
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.DefaultThreadFactory;
import net.jolikit.lang.Unchecked;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.hard.ControllableSystemTimeClock;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * To test AbstractProcess, using hard scheduling.
 * In hard package to avoid cycles.
 */
public class AbztractProcessHardTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final double TOLERANCE_S = 0.1;
    private static final long TOLERANCE_MS = TimeUtils.sToMillis(TOLERANCE_S);
    
    /**
     * Hopefully enough calls so that all tested cases are
     * likely enough to be triggered.
     */
    private static final int NBR_OF_START_STOP_CALLS = 1000 * 1000;
    
    /**
     * To avoid memory explosion as schedules pile-up in the scheduler.
     */
    private static final int FLUSH_START_STOP_EVERY = 100;
    
    private static final int NBR_OF_PROCESSING_THREADS = 4;
    
    private static final int NBR_OF_CALLING_THREADS = 4;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyMethod {
        ON_BEGIN,
        ON_END,
        PROCESS
    }
    
    private static class MyRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MyRuntimeException(String message) {
            super("for test : " + message);
        }
    }
    
    private class MyMethodHelper {
        final AbstractProcess owner;
        final AtomicReference<String> problem;
        final MyMethod method;
        boolean beingCalled;
        boolean mustThrow;
        boolean didLastCallThrow;
        boolean mustStoreThrownIntoList;
        ArrayList<Throwable> thrownList;
        public MyMethodHelper(
                AbstractProcess owner,
                AtomicReference<String> problem,
                MyMethod method) {
            this.owner = owner;
            this.problem = problem;
            this.method = method;
        }
        /**
         * @param duringCall Can be null.
         */
        void onCall(Runnable duringCall) {
            if (DEBUG) {
                Dbg.log(
                        "onCall(...) : " + this.method + " on " + this.owner.hashCode(),
                        new RuntimeException("for stack"));
            }
            if (this.beingCalled) {
                this.setProblemIfNull(this.method + " called recursively");
                throw new AssertionError("called recursively");
            }
            this.beingCalled = true;
            try {
                if (duringCall != null) {
                    duringCall.run();
                }

                this.didLastCallThrow = this.mustThrow;
                if (this.mustThrow) {
                    final MyRuntimeException exception = new MyRuntimeException(
                            "from " + this.method);
                    if (this.mustStoreThrownIntoList) {
                        this.thrownList.add(exception);
                    }
                    if (DEBUG) {
                        Dbg.log(this.method + " : throwing");
                    }
                    throw exception;
                }
            } finally {
                this.beingCalled = false;
            }
        }
        void configureThrowing(
                boolean mustThrow,
                boolean mustStoreThrownIntoList,
                ArrayList<Throwable> thrownList) {
            this.mustThrow = mustThrow;
            this.mustStoreThrownIntoList = mustStoreThrownIntoList;
            this.thrownList = thrownList;
        }
        private void setProblemIfNull(String problem) {
            if (this.problem.compareAndSet(null, problem)) {
                new RuntimeException().printStackTrace();
            }
        }
    }
    
    private class MyProcess extends AbstractProcess {
        
        private final double exceptionProbability_onBegin;
        private final double exceptionProbability_onEnd;
        private final double exceptionProbability_process;
        private boolean mustStoreThrownToTlThrownList;

        private final ThreadLocal<ArrayList<Throwable>> tlThrownList =
                new ThreadLocal<ArrayList<Throwable>>() {
            @Override
            protected ArrayList<Throwable> initialValue() {
                return new ArrayList<Throwable>();
            }
        };

        private final AtomicReference<String> problem = new AtomicReference<String>();
        
        private final MyMethodHelper onBegin_helper = new MyMethodHelper(this, problem, MyMethod.ON_BEGIN);
        
        private final MyMethodHelper onEnd_helper = new MyMethodHelper(this, problem, MyMethod.ON_END);
        
        private final MyMethodHelper process_helper = new MyMethodHelper(this, problem, MyMethod.PROCESS);
        
        private volatile MyMethod lastCalled;
        
        // Expected successive calls cases.
        private volatile boolean process_calledAfterOnBeginWithoutException = false;
        private volatile boolean onEnd_calledAfterProcessWithException = false;
        private volatile boolean onEnd_calledAfterProcessWithoutException = false;
        private volatile boolean onBegin_calledAfterOnEndWithException = false;
        private volatile boolean onBegin_calledAfterOnEndWithoutException = false;
        
        private volatile long last_theoreticalTimeNs;
        private volatile long last_actualTimeNs;

        public MyProcess(
                final InterfaceScheduler scheduler,
                double exceptionProbability) {
            this(
                    scheduler,
                    exceptionProbability,
                    exceptionProbability,
                    exceptionProbability);
        }
        
        public MyProcess(
                final InterfaceScheduler scheduler,
                double exceptionProbability_onBegin,
                double exceptionProbability_onEnd,
                double exceptionProbability_process) {
            super(scheduler);
            this.exceptionProbability_onBegin = exceptionProbability_onBegin;
            this.exceptionProbability_onEnd = exceptionProbability_onEnd;
            this.exceptionProbability_process = exceptionProbability_process;
        }
        
        private void setProblemIfNull(String problem) {
            if (this.problem.compareAndSet(null, problem)) {
                Dbg.log(problem, new RuntimeException("for stack"));
            }
        }
        
        private boolean computeTrueFalse(double exceptionProbability) {
            final boolean ret;
            if (exceptionProbability == 0.0) {
                ret = false;
            } else if (exceptionProbability == 1.0) {
                ret = true;
            } else {
                ret = random.nextDouble() < exceptionProbability;
            }
            return ret;
        }
        
        @Override
        protected void onBegin() {
            final MyMethodHelper myHelper = this.onBegin_helper;
            myHelper.onCall(new Runnable() {
                @Override
                public void run() {
                    if (onEnd_helper.beingCalled) {
                        setProblemIfNull("onBegin() called while onEnd() is being called");
                    }
                    if (process_helper.beingCalled) {
                        setProblemIfNull("onBegin() called while process(...) is being called");
                    }
                    if ((lastCalled == MyMethod.ON_BEGIN) && (!onBegin_helper.didLastCallThrow)) {
                        setProblemIfNull("onBegin() called after onBegin() that did not throw an exception");
                    }
                    if (lastCalled == MyMethod.ON_END) {
                        if (onEnd_helper.didLastCallThrow) {
                            onBegin_calledAfterOnEndWithException = true;
                        } else {
                            onBegin_calledAfterOnEndWithoutException = true;
                        }
                    }
                    if (lastCalled == MyMethod.PROCESS) {
                        setProblemIfNull("onBegin() called after process(...) (exception: " + process_helper.didLastCallThrow + ")");
                    }
                    lastCalled = MyMethod.ON_BEGIN;
                    
                    final boolean mustThrow = computeTrueFalse(exceptionProbability_onBegin);
                    myHelper.configureThrowing(
                            mustThrow,
                            mustStoreThrownToTlThrownList,
                            tlThrownList.get());
                }
            });
        }
        
        @Override
        protected void onEnd() {
            final MyMethodHelper myHelper = this.onEnd_helper;
            myHelper.onCall(new Runnable() {
                @Override
                public void run() {
                    if (onBegin_helper.beingCalled) {
                        setProblemIfNull("onEnd() called while onBegin() is being called");
                    }
                    if (process_helper.beingCalled) {
                        setProblemIfNull("onEnd() called while process(...) is being called");
                    }
                    if (lastCalled == MyMethod.ON_END) {
                        setProblemIfNull("onEnd() called after onEnd() (exception: " + onEnd_helper.didLastCallThrow + ")");
                    }
                    if (lastCalled == MyMethod.PROCESS) {
                        if (process_helper.didLastCallThrow) {
                            onEnd_calledAfterProcessWithException = true;
                        } else {
                            onEnd_calledAfterProcessWithoutException = true;
                        }
                    }
                    lastCalled = MyMethod.ON_END;
                    
                    final boolean mustThrow = computeTrueFalse(exceptionProbability_onEnd);
                    myHelper.configureThrowing(
                            mustThrow,
                            mustStoreThrownToTlThrownList,
                            tlThrownList.get());
                }
            });
        }
        
        @Override
        protected long process(final long theoreticalTimeNs, final long actualTimeNs) {
            final MyMethodHelper myHelper = this.process_helper;
            myHelper.onCall(new Runnable() {
                @Override
                public void run() {
                    last_theoreticalTimeNs = theoreticalTimeNs;
                    last_actualTimeNs = actualTimeNs;
                    if (onBegin_helper.beingCalled) {
                        setProblemIfNull("process(...) called while onBegin() is being called");
                    }
                    if (onEnd_helper.beingCalled) {
                        setProblemIfNull("process(...) called while onEnd() is being called");
                    }
                    if (lastCalled == MyMethod.ON_BEGIN) {
                        if (onBegin_helper.didLastCallThrow) {
                            setProblemIfNull("process(...) called after onBegin() that did throw an exception");
                        } else {
                            process_calledAfterOnBeginWithoutException = true;
                        }
                    }
                    if (lastCalled == MyMethod.ON_END) {
                        setProblemIfNull("process(...) called after onEnd() (exception: " + onEnd_helper.didLastCallThrow + ")");
                    }
                    if ((lastCalled == MyMethod.PROCESS) && process_helper.didLastCallThrow) {
                        setProblemIfNull("process(...) called after process(...) that did throw an exception");
                    }
                    MyMethod previousCalled = lastCalled;
                    lastCalled = MyMethod.PROCESS;
                    
                    // Only allowing to throw an exception for the PROCESS call following an ON_BEGIN call,
                    // or, in case of many successive PROCESS calls, we would always most likely end up
                    // with an exception, and never enter the case of a regular ON_END call after a
                    // treatment's stop.
                    final boolean mustThrow =
                            (previousCalled != MyMethod.PROCESS)
                            && computeTrueFalse(exceptionProbability_process);
                    myHelper.configureThrowing(
                            mustThrow,
                            mustStoreThrownToTlThrownList,
                            tlThrownList.get());
                    
                    // For less risk of "while (true)" loop around calls to process,
                    // which could slow things down a lot.
                    Thread.yield();
                }
            });
            
            // Zero delay until next process, else consecutive start/stop would keep canceling it.
            return actualTimeNs;
        }
    }
    
    private class MyCallerRunnable implements Runnable {
        private final MyProcess process;
        public MyCallerRunnable(MyProcess process) {
            this.process = process;
        }
        @Override
        public void run() {
            int counter = 0;
            for (int i = 0; i < NBR_OF_START_STOP_CALLS; i++) {
                double uniform_0_1 = random.nextDouble();
                if (uniform_0_1 < 0.5) {
                    this.process.start();
                    if (uniform_0_1 < 0.25) {
                        // Letting a chance for treatments to actually start.
                        Thread.yield();
                    }
                } else {
                    this.process.stop();
                    if (uniform_0_1 < 0.75) {
                        // Letting a chance for treatments to actually start.
                        Thread.yield();
                    }
                }
                
                if (((++counter) % FLUSH_START_STOP_EVERY) == 0) {
                    waitForNoMoreAsapSchedule((HardScheduler)this.process.getScheduler());
                }
            }
            // Making sure last call is a stop.
            this.process.stop();
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Random random = new Random(123456789L);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_start() {
        final boolean mustUseStartAfter = false;
        this.test_startXxx(mustUseStartAfter);
    }

    public void test_startAfterNs_long() {
        final boolean mustUseStartAfter = true;
        this.test_startXxx(mustUseStartAfter);
    }

    public void test_startXxx(boolean mustUseStartAfter) {
        final ControllableSystemTimeClock clock = new ControllableSystemTimeClock();
        clock.setTimeSpeed(0.0);
        clock.setTimeNs(TimeUtils.sToNs(10.0));

        final boolean daemon = true;
        final HardScheduler scheduler = HardScheduler.newSingleThreadedInstance(
                clock,
                "SCHEDULER",
                daemon);

        final double exceptionProbability = 0.0;
        final MyProcess process = new MyProcess(
                scheduler,
                exceptionProbability) {
            @Override
            public long process(long theoreticalTimeNs, long actualTimeNs) {
                super.process(theoreticalTimeNs, actualTimeNs);
                // We want only one call to this method.
                this.stop();
                return 0;
            }
        };

        final long startAfterDelayMs = 2 * TOLERANCE_MS;
        
        // Making scheduler thread busy for some time.
        final long blockingDelayMs = startAfterDelayMs + TOLERANCE_MS * 10;
        // Timed schedules have priority over ASAP schedules,
        // so using a timed schedule with no delay to make sure
        // the sleep always gets to run first.
        scheduler.executeAfterNs(new Runnable() {
            @Override
            public void run() {
                Unchecked.sleepMs(blockingDelayMs);
            }
        }, 0L);

        // Starting time.
        clock.setTimeSpeed(1.0);

        final double t1S = clock.getTimeS();
        if (mustUseStartAfter) {
            process.startAfterNs(startAfterDelayMs * (1000L * 1000L));
        } else {
            process.start();
        }

        // Waiting long enough for start to have its effect
        // and process be called once.
        Unchecked.sleepMs(blockingDelayMs + TOLERANCE_MS);
        
        scheduler.shutdownNow(false);

        final double t2S_theo = TimeUtils.nsToS(process.last_theoreticalTimeNs);
        final double t2S_actu = TimeUtils.nsToS(process.last_actualTimeNs);
        
        final double blockingDelayS = TimeUtils.millisToS(blockingDelayMs);
        final double startAfterDelayS = TimeUtils.millisToS(startAfterDelayMs);
        final double expected_t2S_actu = t1S + blockingDelayS;
        final double expected_t2S_theo = t1S + (mustUseStartAfter ? startAfterDelayS : blockingDelayS);
        
        final boolean good1 = Math.abs(t2S_theo - expected_t2S_theo) < TOLERANCE_S;
        final boolean good2 = Math.abs(t2S_actu - expected_t2S_actu) < TOLERANCE_S;
        if (!(good1 && good2)) {
            System.out.println("mustUseStartAfter = " + mustUseStartAfter);
            System.out.println("good1 = " + good1);
            System.out.println("good2 = " + good2);
            System.out.println("startAfterDelayS = " + startAfterDelayS);
            System.out.println("blockingDelayS =   " + blockingDelayS);
            System.out.println("t1S =      " + t1S);
            System.out.println("expected_t2S_theo = " + expected_t2S_theo);
            System.out.println("expected_t2S_actu = " + expected_t2S_actu);
            System.out.println("t2S_theo = " + t2S_theo);
            System.out.println("t2S_actu = " + t2S_actu);
            fail();
        }
    }

    public void test_concurrentMess_noException() {
        final double exceptionProbability = 0.0;
        this.test_concurrentMess(exceptionProbability);
    }

    public void test_concurrentMess_someExceptions() {
        final double exceptionProbability = 0.1;
        this.test_concurrentMess(exceptionProbability);
    }

    public void test_concurrentMess_alwaysExceptions() {
        final double exceptionProbability = 1.0;
        this.test_concurrentMess(exceptionProbability);
    }
    
    /*
     * Anti-suppressions swallowings.
     */
    
    /**
     * Tests that (for each thread) exceptions that are thrown first
     * are those that bubble up, without being suppressed by eventual exceptions
     * thrown from finally blocks.
     * 
     * NB: This easily tests the handling done in
     * AbstractRepeatableRunnable.run() method,
     * but hardly those done in AbstractProcess
     * (errors rare when commenting the handling out).
     */
    public void test_antiSuppressionsSwallowings() {
        final double exceptionProbability_onBegin = 0.75;
        final double exceptionProbability_onEnd = 0.75;
        final double exceptionProbability_process = 0.25;
        final boolean mustTestAntiSuppressionsSwallowings = true;
        this.test_concurrentMess(
                exceptionProbability_onBegin,
                exceptionProbability_onEnd,
                exceptionProbability_process,
                mustTestAntiSuppressionsSwallowings);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses mustTestAntiSuppressionsSwallowings = false.
     */
    private void test_concurrentMess(
            double exceptionProbability) {
        final boolean mustTestAntiSuppressionsSwallowings = false;
        this.test_concurrentMess(
                exceptionProbability,
                exceptionProbability,
                exceptionProbability,
                mustTestAntiSuppressionsSwallowings);
    }
    
    /**
     * Testing nothing goes wrong with concurrent non-FIFO scheduler,
     * and concurrent use of start/stop.
     */
    private void test_concurrentMess(
            double exceptionProbability_onBegin,
            double exceptionProbability_process,
            double exceptionProbability_onEnd,
            boolean mustTestAntiSuppressionsSwallowings) {
        final ControllableSystemTimeClock clock = new ControllableSystemTimeClock();
        clock.setTimeSpeed(1.0);
        clock.setTimeNs(0L);
        
        final AtomicReference<MyProcess> processRef;
        final UncaughtExceptionHandler exceptionHandler;
        if (mustTestAntiSuppressionsSwallowings) {
            processRef = new AtomicReference<MyProcess>();
            exceptionHandler = new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread throwingThread, Throwable caught) {
                    final MyProcess process = processRef.get();
                    final ArrayList<Throwable> thrownList = process.tlThrownList.get();
                    if (thrownList.size() == 0) {
                        process.setProblemIfNull("caught an exception, but none was thrown");
                    } else {
                        final Throwable firstThrown = thrownList.get(0);
                        if (firstThrown != caught) {
                            process.setProblemIfNull("caught exception is not first thrown");
                            Dbg.log("caught:", caught);
                            for (int i = 0; i < thrownList.size(); i++) {
                                final Throwable thrown = thrownList.get(i);
                                if (thrown == caught) {
                                    Dbg.log("(next is caught one)");
                                }
                                Dbg.log("thrownList[" + i + "] =", thrown);
                            }
                        }
                        // Cleanup for next check.
                        thrownList.clear();
                    }
                }
            };
        } else {
            processRef = null;
            exceptionHandler = new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    // quiet
                }
            };
        }
        final boolean daemon = true;
        final ThreadFactory backingThreadFactory = null;
        final ThreadFactory threadFactory = new DefaultThreadFactory(
                backingThreadFactory,
                exceptionHandler);
        final HardScheduler scheduler = HardScheduler.newInstance(
                clock,
                "SCHEDULER",
                daemon,
                NBR_OF_PROCESSING_THREADS,
                threadFactory);
        
        final MyProcess process = new MyProcess(
                scheduler,
                exceptionProbability_onBegin,
                exceptionProbability_onEnd,
                exceptionProbability_process);
        if (mustTestAntiSuppressionsSwallowings) {
            processRef.set(process);
            process.mustStoreThrownToTlThrownList = true;
        }

        /*
         * 
         */
        
        // For less risk of having all start/stop calls done before
        // any processing thread actually starts to work.
        scheduler.startWorkerThreadsIfNeeded();
        
        final ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < NBR_OF_CALLING_THREADS; i++) {
            executor.execute(new MyCallerRunnable(process));
        }
        
        Unchecked.shutdownAndAwaitTermination(executor);
        requestAndWaitForWorkersDeath(scheduler);

        /*
         * 
         */
        
        // assert equals, to have eventual problem to appear on JUnit report
        assertEquals(null, process.problem.get());

        assertEquals(
                (exceptionProbability_onBegin < 1.0),
                process.process_calledAfterOnBeginWithoutException);
        assertEquals(
                (exceptionProbability_onBegin < 1.0)
                && (exceptionProbability_process > 0.0),
                process.onEnd_calledAfterProcessWithException);
        assertEquals(
                (exceptionProbability_process < 1.0),
                process.onEnd_calledAfterProcessWithoutException);

        assertEquals(
                (exceptionProbability_onEnd > 0.0),
                process.onBegin_calledAfterOnEndWithException);
        assertEquals(
                (exceptionProbability_onEnd < 1.0),
                process.onBegin_calledAfterOnEndWithoutException);
    }
    
    /*
     * 
     */
    
    private static void requestAndWaitForWorkersDeath(HardScheduler scheduler) {
        scheduler.shutdown();
        try {
            scheduler.waitForNoMoreRunningWorkerSystemTimeNs(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static void waitForNoMoreAsapSchedule(HardScheduler scheduler) {
        final long initialNs = System.nanoTime();
        while (scheduler.getNbrOfPendingAsapSchedules() != 0) {
            if ((System.nanoTime() - initialNs > TimeUtils.sToNs(10.0))
                    && (scheduler.getNbrOfRunningWorkers() == 0)) {
                // Not waiting forever.
                throw new RuntimeException("no (more) worker");
            }
            Thread.yield();
        }
    }
}
