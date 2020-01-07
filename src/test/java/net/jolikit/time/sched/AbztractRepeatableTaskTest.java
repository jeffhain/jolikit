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
package net.jolikit.time.sched;

import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

/**
 * To test AbstractRepeatableTask.
 */
public class AbztractRepeatableTaskTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final long PERIOD_NS = 10L;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyMethod {
        ON_BEGIN,
        ON_END,
        ON_DONE,
        RUN_IMPL
    }
    
    private static class MyRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MyRuntimeException() {
            super("for test");
        }
    }
    
    private static class MyMethodHelper {
        final AbstractRepeatableTask owner;
        final MyMethod method;
        boolean beingCalled;
        int callCount;
        boolean mustRequestCancellation;
        boolean mustCancel;
        boolean mustThrow;
        public MyMethodHelper(
                AbstractRepeatableTask owner,
                MyMethod method) {
            this.owner = owner;
            this.method = method;
        }
        void onCall(Runnable duringCall) {
            if (this.beingCalled) {
                throw new AssertionError(this.method + " called recursively");
            }
            this.beingCalled = true;
            try {
                this.callCount++;

                if (duringCall != null) {
                    duringCall.run();
                }

                if (this.mustRequestCancellation) {
                    this.owner.requestCancellation();
                }
                
                if (this.mustCancel) {
                    this.owner.cancel(false);
                }

                if (this.mustThrow) {
                    throw new MyRuntimeException();
                }
            } finally {
                this.beingCalled = false;
            }
        }
    }
    
    private class MyRepTask extends AbstractRepeatableTask {
        
        private final MyMethodHelper onBegin_helper = new MyMethodHelper(this, MyMethod.ON_BEGIN);
        
        private final MyMethodHelper onEnd_helper = new MyMethodHelper(this, MyMethod.ON_END);
        
        private final MyMethodHelper onDone_helper = new MyMethodHelper(this, MyMethod.ON_DONE);
        
        private final MyMethodHelper runImpl_helper = new MyMethodHelper(this, MyMethod.RUN_IMPL);
        
        private long runImpl_theoreticalTimeNs = Long.MIN_VALUE;
        private long runImpl_actualTimeNs = Long.MIN_VALUE;
        private boolean runImpl_mustSetNextTheoreticalTime = true;
        
        public MyRepTask(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        
        @Override
        protected void onBegin() {
            assertFalse(this.isPending());
            assertTrue(this.isOnBeginBeingCalled());
            assertFalse(this.isRepeating());
            assertFalse(this.isOnEndBeingCalled());
            assertFalse(this.isOnDoneBeingCalled());
            assertFalse(this.isDone());
            assertFalse(this.isTerminatingOrDone());
            assertFalse(this.isCancellationRequested());
            assertFalse(this.isCancelled());
            
            this.onBegin_helper.onCall(new Runnable() {
                public void run() {
                }
            });
        }
        
        @Override
        protected void onEnd() {
            assertFalse(this.isPending());
            assertFalse(this.isOnBeginBeingCalled());
            assertFalse(this.isRepeating());
            assertTrue(this.isOnEndBeingCalled());
            assertFalse(this.isOnDoneBeingCalled());
            assertFalse(this.isDone());
            assertTrue(this.isTerminatingOrDone());
            
            this.onEnd_helper.onCall(new Runnable() {
                public void run() {
                }
            });
        }
        
        @Override
        protected void onDone() {
            assertFalse(this.isPending());
            assertFalse(this.isOnBeginBeingCalled());
            assertFalse(this.isRepeating());
            assertFalse(this.isOnEndBeingCalled());
            assertTrue(this.isOnDoneBeingCalled());
            assertFalse(this.isDone());
            assertTrue(this.isTerminatingOrDone());
            
            this.onDone_helper.onCall(new Runnable() {
                public void run() {
                }
            });
        }

        @Override
        protected void runImpl(final long theoreticalTimeNs, final long actualTimeNs) {
            assertFalse(this.isPending());
            assertFalse(this.isOnBeginBeingCalled());
            assertTrue(this.isRepeating());
            assertFalse(this.isOnEndBeingCalled());
            assertFalse(this.isOnDoneBeingCalled());
            assertFalse(this.isDone());
            assertFalse(this.isTerminatingOrDone());
            
            this.runImpl_helper.onCall(new Runnable() {
                public void run() {
                    runImpl_theoreticalTimeNs = theoreticalTimeNs;
                    runImpl_actualTimeNs = actualTimeNs;
                    
                    if (runImpl_mustSetNextTheoreticalTime) {
                        setNextTheoreticalTimeNs(plusBounded(actualTimeNs, PERIOD_NS));
                    }
                }
            });
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * general
     */
    
    public void test_general() {
        final TestScheduler scheduler = new TestScheduler();

        final MyRepTask task = new MyRepTask(scheduler);
        //
        assertEquals(0, task.onBegin_helper.callCount);
        assertEquals(0, task.runImpl_helper.callCount);
        assertEquals(0, task.onEnd_helper.callCount);
        assertEquals(0, task.onDone_helper.callCount);
        assertTrue(task.isPending());
        assertFalse(task.isOnBeginBeingCalled());
        assertFalse(task.isRepeating());
        assertFalse(task.isOnEndBeingCalled());
        assertFalse(task.isOnDoneBeingCalled());
        assertFalse(task.isDone());
        assertFalse(task.isTerminatingOrDone());
        assertFalse(task.isCancellationRequested());
        assertFalse(task.isCancelled());
        
        /*
         * Running (starting periodic run).
         */
        
        long nowNs = scheduler.getClock().getTimeNs();
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(0, task.onEnd_helper.callCount);
        assertEquals(0, task.onDone_helper.callCount);
        assertEquals(nowNs, task.runImpl_theoreticalTimeNs);
        assertEquals(nowNs, task.runImpl_actualTimeNs);
        assertEquals(nowNs + PERIOD_NS, task.getTheoreticalTimeNs());
        assertFalse(task.isCancellationRequested());
        assertFalse(task.isCancelled());
        
        /*
         * Running again.
         */
        
        nowNs = scheduler.setAndGetTimeNs(task.getTheoreticalTimeNs());

        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(2, task.runImpl_helper.callCount);
        assertEquals(0, task.onEnd_helper.callCount);
        assertEquals(0, task.onDone_helper.callCount);
        assertEquals(nowNs, task.runImpl_theoreticalTimeNs);
        assertEquals(nowNs, task.runImpl_actualTimeNs);
        assertEquals(nowNs + PERIOD_NS, task.getTheoreticalTimeNs());
        assertFalse(task.isCancellationRequested());
        assertFalse(task.isCancelled());
        
        /*
         * Canceling.
         */
        
        task.cancel(false);
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(2, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        assertTrue(task.isCancelled());
        
        /*
         * Canceling must be idempotent.
         */
        
        task.cancel(false);
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(2, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        assertTrue(task.isCancelled());
        
        /*
         * Must not run again if cancelled.
         */
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(2, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        assertTrue(task.isCancelled());
        
        /*
         * Calling onCancel() must no longer have effect either.
         */
        
        task.onCancel();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(2, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        assertTrue(task.isCancelled());
    }
    
    /*
     * onBegin
     */
    
    public void test_onBegin_requestsCancellation() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        task.onBegin_helper.mustRequestCancellation = true;
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(0, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        assertTrue(task.isCancelled());
    }
    
    public void test_onBegin_cancels() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        task.onBegin_helper.mustCancel = true;
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(0, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        assertTrue(task.isCancelled());
    }
    
    public void test_onBegin_throws() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        task.onBegin_helper.mustThrow = true;
        
        try {
            task.run();
            fail();
        } catch (MyRuntimeException e) {
            // ok
        }
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(0, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertFalse(task.isCancellationRequested());
        assertFalse(task.isCancelled());
    }
    
    /*
     * onEnd
     */
    
    /**
     * Just checking that doesn't hurt.
     */
    public void test_onEnd_requestsCancellation() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onEnd_helper.mustRequestCancellation = true;
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        // Too late for cancellation to occur:
        // termination was already initiated.
        assertFalse(task.isCancelled());
    }
    
    /**
     * Just checking that doesn't hurt.
     */
    public void test_onEnd_cancels() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onEnd_helper.mustCancel = true;
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        // Too late for cancellation to occur:
        // termination was already initiated.
        assertFalse(task.isCancelled());
    }
    
    /**
     * Just checking that doesn't hurt.
     */
    public void test_onEnd_throws() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onEnd_helper.mustThrow = true;
        
        try {
            task.run();
            fail();
        } catch (MyRuntimeException e) {
            // ok
        }
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertFalse(task.isCancellationRequested());
        assertFalse(task.isCancelled());
    }
    
    /*
     * onDone
     */
    
    /**
     * Just checking that doesn't hurt.
     */
    public void test_onDone_requestsCancellation() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onDone_helper.mustRequestCancellation = true;
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        // Too late for cancellation to occur:
        // termination was already initiated.
        assertFalse(task.isCancelled());
    }
    
    /**
     * Just checking that doesn't hurt.
     */
    public void test_onDone_cancels() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onDone_helper.mustCancel = true;
        
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertTrue(task.isCancellationRequested());
        // Too late for cancellation to occur:
        // termination was already initiated.
        assertFalse(task.isCancelled());
    }
    
    /**
     * Just checking that doesn't hurt.
     */
    public void test_onDone_throws() {
        final TestScheduler scheduler = new TestScheduler();
        
        final MyRepTask task = new MyRepTask(scheduler);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onDone_helper.mustThrow = true;
        
        try {
            task.run();
            fail();
        } catch (MyRuntimeException e) {
            // ok
        }
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(1, task.onEnd_helper.callCount);
        assertEquals(1, task.onDone_helper.callCount);
        assertFalse(task.isCancellationRequested());
        assertFalse(task.isCancelled());
    }
    
    /*
     * First theoretical time.
     */
    
    public void test_firstTheoreticalTime_notSet() {
        final TestScheduler scheduler = new TestScheduler();
        final MyRepTask task = new MyRepTask(scheduler);

        long nowNs = scheduler.getClock().getTimeNs();
        
        task.run();
        
        assertEquals(nowNs, task.runImpl_theoreticalTimeNs);
        assertEquals(nowNs, task.runImpl_actualTimeNs);
    }

    public void test_firstTheoreticalTime_set() {
        final TestScheduler scheduler = new TestScheduler();
        final MyRepTask task = new MyRepTask(scheduler);

        long nowNs = scheduler.getClock().getTimeNs();
        
        task.setNextTheoreticalTimeNs(nowNs - 1);
        task.run();
        
        assertEquals(nowNs - 1, task.runImpl_theoreticalTimeNs);
        assertEquals(nowNs, task.runImpl_actualTimeNs);
    }
    
    /*
     * Re-schedules.
     */
    
    public void test_reschedule_noOverride() {
        final TestScheduler scheduler = new TestScheduler();
        final MyRepTask task = new MyRepTask(scheduler);

        task.runImpl_mustSetNextTheoreticalTime = true;

        task.run();

        assertEquals(0, scheduler.get_execute_callCount());
        assertEquals(1, scheduler.get_executeAtNs_callCount());
        assertEquals(0, scheduler.get_executeAfterNs_callCount());

        assertSame(task, scheduler.get_executeAtNs_runnable());
        final long nowNs = scheduler.getClock().getTimeNs();
        assertEquals(nowNs + PERIOD_NS, scheduler.get_executeAtNs_timeNs());
    }

    public void test_reschedule_noReschedule() {
        final TestScheduler scheduler = new TestScheduler();
        final AtomicLong lastRescheduleTimeNsRef = new AtomicLong();
        final MyRepTask task = new MyRepTask(scheduler) {
            @Override
            protected void rescheduleTaskAtNs(long timeNs) {
                lastRescheduleTimeNsRef.set(timeNs);
            }
        };

        task.runImpl_mustSetNextTheoreticalTime = true;

        task.run();

        assertEquals(0, scheduler.get_execute_callCount());
        assertEquals(0, scheduler.get_executeAtNs_callCount());
        assertEquals(0, scheduler.get_executeAfterNs_callCount());

        final long nowNs = scheduler.getClock().getTimeNs();
        assertEquals(nowNs + PERIOD_NS, lastRescheduleTimeNsRef.get());
    }

    public void test_reschedule_asapReschedule() {
        final TestScheduler scheduler = new TestScheduler();
        final AtomicLong lastRescheduleTimeNsRef = new AtomicLong();
        final MyRepTask task = new MyRepTask(scheduler) {
            @Override
            protected void rescheduleTaskAtNs(long timeNs) {
                lastRescheduleTimeNsRef.set(timeNs);
                scheduler.execute(this);
            }
        };

        task.runImpl_mustSetNextTheoreticalTime = true;

        task.run();

        assertEquals(1, scheduler.get_execute_callCount());
        assertEquals(0, scheduler.get_executeAtNs_callCount());
        assertEquals(0, scheduler.get_executeAfterNs_callCount());

        assertSame(task, scheduler.get_execute_runnable());
        final long nowNs = scheduler.getClock().getTimeNs();
        assertEquals(nowNs + PERIOD_NS, lastRescheduleTimeNsRef.get());
    }
}
