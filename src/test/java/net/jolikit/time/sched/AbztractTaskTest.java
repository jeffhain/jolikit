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
package net.jolikit.time.sched;

import junit.framework.TestCase;

/**
 * To test AbstractTask.
 */
public class AbztractTaskTest extends TestCase {
    
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
        final AbstractTask owner;
        final MyMethod method;
        boolean beingCalled;
        int callCount;
        boolean mustRequestCancellation;
        boolean mustCancel;
        boolean mustThrow;
        public MyMethodHelper(
                AbstractTask owner,
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
    
    private class MyTask extends AbstractTask {
        
        private final MyMethodHelper onBegin_helper = new MyMethodHelper(this, MyMethod.ON_BEGIN);
        
        private final MyMethodHelper onEnd_helper = new MyMethodHelper(this, MyMethod.ON_END);
        
        private final MyMethodHelper onDone_helper = new MyMethodHelper(this, MyMethod.ON_DONE);
        
        private final MyMethodHelper runImpl_helper = new MyMethodHelper(this, MyMethod.RUN_IMPL);
        
        private long lastRunTimeNs = Long.MIN_VALUE;
        
        private boolean runImpl_mustSetNextTheoreticalTime = true;
        
        public MyTask() {
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
        protected void runImpl() {
            assertFalse(this.isPending());
            assertFalse(this.isOnBeginBeingCalled());
            assertTrue(this.isRepeating());
            assertFalse(this.isOnEndBeingCalled());
            assertFalse(this.isOnDoneBeingCalled());
            assertFalse(this.isDone());
            assertFalse(this.isTerminatingOrDone());
            
            final InterfaceScheduling scheduling = this.getScheduling();
            
            this.runImpl_helper.onCall(new Runnable() {
                public void run() {
                    final long theoreticalTimeNs = scheduling.getTheoreticalTimeNs();
                    final long actualTimeNs = scheduling.getActualTimeNs();
                    if (actualTimeNs != theoreticalTimeNs) {
                        // Soft scheduling, so always the same.
                        throw new AssertionError(actualTimeNs + " != " + theoreticalTimeNs);
                    }
                    lastRunTimeNs = actualTimeNs;
                    
                    if (runImpl_mustSetNextTheoreticalTime) {
                        scheduling.setNextTheoreticalTimeNs(plusBounded(actualTimeNs, PERIOD_NS));
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        // Just need to set it once for this test.
        task.setScheduling(scheduling);
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
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(1, task.runImpl_helper.callCount);
        assertEquals(0, task.onEnd_helper.callCount);
        assertEquals(0, task.onDone_helper.callCount);
        assertEquals(nowNs, task.lastRunTimeNs);
        assertEquals(nowNs + PERIOD_NS, scheduling.getNextTheoreticalTimeNs());
        assertFalse(task.isCancellationRequested());
        assertFalse(task.isCancelled());
        
        /*
         * Running again.
         */
        
        nowNs = scheduling.getNextTheoreticalTimeNs();
        
        scheduling.configureBeforeRun(nowNs, nowNs);
        task.run();
        //
        assertEquals(1, task.onBegin_helper.callCount);
        assertEquals(2, task.runImpl_helper.callCount);
        assertEquals(0, task.onEnd_helper.callCount);
        assertEquals(0, task.onDone_helper.callCount);
        assertEquals(nowNs, task.lastRunTimeNs);
        assertEquals(nowNs + PERIOD_NS, scheduling.getNextTheoreticalTimeNs());
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        task.onBegin_helper.mustRequestCancellation = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        task.onBegin_helper.mustCancel = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        task.onBegin_helper.mustThrow = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onEnd_helper.mustRequestCancellation = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onEnd_helper.mustCancel = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onEnd_helper.mustThrow = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onDone_helper.mustRequestCancellation = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onDone_helper.mustCancel = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
        
        final MyTask task = new MyTask();
        final DefaultScheduling scheduling = new DefaultScheduling();
        task.setScheduling(scheduling);
        
        /*
         * 
         */
        
        // To abort repetition.
        task.runImpl_mustSetNextTheoreticalTime = false;
        
        task.onDone_helper.mustThrow = true;
        
        long nowNs = 1000;
        
        scheduling.configureBeforeRun(nowNs, nowNs);
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
}
