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

import net.jolikit.time.sched.AbstractRepeatableRunnable;
import net.jolikit.time.sched.InterfaceScheduler;
import junit.framework.TestCase;

/**
 * To test AbstractRepeatableRunnable.
 */
public class AbztractRepeatableRunnableTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final long PERIOD_NS = 10L;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MyRuntimeException(String message) {
            super("for test : " + message);
        }
    }
    
    private static class MyRepRunnable extends AbstractRepeatableRunnable {
        private boolean mustCancelInOnBegin = false;
        private boolean mustCancelInOnEnd = false;
        private boolean mustCancelInOnDone = false;
        private boolean mustCancelInRunImpl = false;
        private boolean mustThrowExceptionInOnBegin = false;
        private boolean mustThrowExceptionInOnEnd = false;
        private boolean mustThrowExceptionInOnDone = false;
        private boolean mustThrowExceptionInRunImpl = false;
        private int nbrOfOnBeginCalls = 0;
        private int nbrOfOnEndCalls = 0;
        private int nbrOfOnDoneCalls = 0;
        private int nbrOfRunImplCalls = 0;
        private long runImpl_theoreticalTimeNs = Long.MIN_VALUE;
        private long runImpl_actualTimeNs = Long.MIN_VALUE;
        private boolean mustSetNextTheoreticalTimeInOnBegin = false;
        private boolean mustSetNextTheoreticalTimeInRunImpl = false;
        private long nextTheoreticalTimeToUseNs = 0;
        public MyRepRunnable(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected void onBegin() {
            nbrOfOnBeginCalls++;
            assertFalse(this.isPending());
            assertTrue(this.isOnBeginBeingCalled());
            assertFalse(this.isOnEndBeingCalled());
            assertFalse(this.isOnDoneBeingCalled());
            assertFalse(this.isRepeating());
            assertFalse(this.isTerminatingOrDone());
            assertFalse(this.isDone());
            assertFalse(this.isCancelled());
            if (mustCancelInOnBegin) {
                onCancel();
            }
            if (mustSetNextTheoreticalTimeInOnBegin) {
                this.setNextTheoreticalTimeNs(nextTheoreticalTimeToUseNs);
            }
            if (mustThrowExceptionInOnBegin) {
                throw new MyRuntimeException("from onBegin()");
            }
        }
        @Override
        protected void onEnd() {
            nbrOfOnEndCalls++;
            assertFalse(this.isPending());
            assertFalse(this.isOnBeginBeingCalled());
            assertTrue(this.isOnEndBeingCalled());
            assertFalse(this.isOnDoneBeingCalled());
            assertFalse(this.isRepeating());
            assertTrue(this.isTerminatingOrDone());
            assertFalse(this.isDone());
            if (mustCancelInOnEnd) {
                onCancel();
            }
            if (mustThrowExceptionInOnEnd) {
                throw new MyRuntimeException("from onEnd()");
            }
        }
        @Override
        protected void onDone() {
            nbrOfOnDoneCalls++;
            assertFalse(this.isPending());
            assertFalse(this.isOnBeginBeingCalled());
            assertFalse(this.isOnEndBeingCalled());
            assertTrue(this.isOnDoneBeingCalled());
            assertFalse(this.isRepeating());
            assertTrue(this.isTerminatingOrDone());
            assertFalse(this.isDone());
            if (mustCancelInOnDone) {
                onCancel();
            }
            if (mustThrowExceptionInOnDone) {
                throw new MyRuntimeException("from onDone()");
            }
        }
        @Override
        protected void runImpl(long theoreticalTimeNs, long actualTimeNs) {
            nbrOfRunImplCalls++;
            runImpl_theoreticalTimeNs = theoreticalTimeNs;
            runImpl_actualTimeNs = actualTimeNs;
            assertFalse(this.isPending());
            assertFalse(this.isOnBeginBeingCalled());
            assertFalse(this.isOnEndBeingCalled());
            assertFalse(this.isOnDoneBeingCalled());
            assertTrue(this.isRepeating());
            assertFalse(this.isTerminatingOrDone());
            assertFalse(this.isDone());
            assertFalse(this.isCancelled());
            if (mustCancelInRunImpl) {
                onCancel();
            }
            if (mustSetNextTheoreticalTimeInRunImpl) {
                this.setNextTheoreticalTimeNs(nextTheoreticalTimeToUseNs);
            }
            if (mustThrowExceptionInRunImpl) {
                throw new MyRuntimeException("from runImpl()");
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_AbstractRepeatableRunnable() {
        // Already covered.
    }
    
    public void test_reset() {
        final TestScheduler scheduler = new TestScheduler();
        final MyRepRunnable runnable = new MyRepRunnable(scheduler);
        
        runnable.mustSetNextTheoreticalTimeInRunImpl = true;
        
        runnable.clearTheoreticalTime();
        runnable.run();
        assertFalse(runnable.isPending());
        assertTrue(runnable.isTheoreticalTimeSet());
        
        runnable.reset();
        assertTrue(runnable.isPending());
        assertFalse(runnable.isTheoreticalTimeSet());
    }
    
    public void test_run() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * 1 call then done
         */
        
        {
            long nowNs = scheduler.getClock().getTimeNs();
            
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.clearTheoreticalTime();
            runnable.run();
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(nowNs, runnable.runImpl_theoreticalTimeNs);
            assertEquals(nowNs, runnable.runImpl_actualTimeNs);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }

        /*
         * 2 calls then done
         */
        
        {
            long nowNs = scheduler.getClock().getTimeNs();
            
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = nowNs + PERIOD_NS;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            assertEquals(runnable.nextTheoreticalTimeToUseNs, runnable.getTheoreticalTimeNs());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(nowNs, runnable.runImpl_theoreticalTimeNs);
            assertEquals(nowNs, runnable.runImpl_actualTimeNs);
            assertEquals(0, runnable.nbrOfOnEndCalls);
            assertEquals(0, runnable.nbrOfOnDoneCalls);
            assertFalse(runnable.isDone());
            assertFalse(runnable.isCancelled());
            
            /*
             * 
             */
            
            nowNs = scheduler.setAndGetTimeNs(runnable.getTheoreticalTimeNs());
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(2, runnable.nbrOfRunImplCalls);
            assertEquals(nowNs, runnable.runImpl_theoreticalTimeNs);
            assertEquals(nowNs, runnable.runImpl_actualTimeNs);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }

        /*
         * onCancel() called in onBegin(): triggers termination
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            runnable.mustCancelInOnBegin = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            // Not set.
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(0, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertTrue(runnable.isCancelled());
        }

        /*
         * onCancel() called in runImpl(...): triggers termination
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            runnable.mustCancelInRunImpl = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            // Set to 1L after call to onCancel(), but then cleared
            // after call to runImpl(...), due to runnable now being cancelled.
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertTrue(runnable.isCancelled());
        }

        /*
         * onCancel() called in onEnd(): does not cause trouble (does nothing)
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustCancelInOnEnd = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }

        /*
         * onCancel() called in onDone(): does not cause trouble (does nothing)
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustCancelInOnDone = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }

        /*
         * exception in onBegin(): terminates (not cancelled)
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            runnable.mustThrowExceptionInOnBegin = true;
            
            runnable.clearTheoreticalTime();
            try {
                runnable.run();
            } catch (Exception ignore) {
            }
            // Not set.
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(0, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }

        /*
         * exception in runImpl(...): terminates (not cancelled)
         */
        
        {
            scheduler.reset();
            
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            runnable.mustThrowExceptionInRunImpl = true;
            
            runnable.clearTheoreticalTime();
            try {
                runnable.run();
            } catch (Exception ignore) {
            }
            // No re-schedule.
            assertFalse(runnable.isTheoreticalTimeSet());
            assertEquals(0, scheduler.get_executeAtNs_callCount());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }

        /*
         * exception in onEnd() called by run(): still calls onDone(), and sets status to done
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustThrowExceptionInOnEnd = true;
            
            runnable.clearTheoreticalTime();
            try {
                runnable.run();
            } catch (Exception ignore) {
            }
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }

        /*
         * exception in onDone() called by run(): still sets status to done
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustThrowExceptionInOnDone = true;
            
            runnable.clearTheoreticalTime();
            try {
                runnable.run();
            } catch (Exception ignore) {
            }
            assertFalse(runnable.isTheoreticalTimeSet());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertFalse(runnable.isCancelled());
        }
    }
    
    public void test_onCancel() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * onCancel before process
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertEquals(0, runnable.nbrOfOnBeginCalls);
            assertEquals(0, runnable.nbrOfRunImplCalls);
            assertEquals(0, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertTrue(runnable.isCancelled());
        }

        /*
         * exception in onDone(), called by onCancel(): still sets status to done
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustThrowExceptionInOnDone = true;
            try {
                runnable.onCancel();
            } catch (Exception ignore) {
            }
            assertEquals(0, runnable.nbrOfOnBeginCalls);
            assertEquals(0, runnable.nbrOfRunImplCalls);
            assertEquals(0, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertTrue(runnable.isCancelled());
        }

        /*
         * onCancel() after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            assertEquals(runnable.nextTheoreticalTimeToUseNs, runnable.getTheoreticalTimeNs());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(0, runnable.nbrOfOnEndCalls);
            assertEquals(0, runnable.nbrOfOnDoneCalls);
            assertFalse(runnable.isDone());
            assertFalse(runnable.isCancelled());

            runnable.onCancel();
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertTrue(runnable.isCancelled());
        }

        /*
         * exception in onEnd(), called by onCancel: still calls onDone, and sets status to done
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            assertEquals(runnable.nextTheoreticalTimeToUseNs, runnable.getTheoreticalTimeNs());
            
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(0, runnable.nbrOfOnEndCalls);
            assertEquals(0, runnable.nbrOfOnDoneCalls);
            assertFalse(runnable.isDone());
            assertFalse(runnable.isCancelled());

            runnable.mustThrowExceptionInOnEnd = true;
            try {
                runnable.onCancel();
            } catch (Exception ignore) {
            }
            assertEquals(1, runnable.nbrOfOnBeginCalls);
            assertEquals(1, runnable.nbrOfRunImplCalls);
            assertEquals(1, runnable.nbrOfOnEndCalls);
            assertEquals(1, runnable.nbrOfOnDoneCalls);
            assertTrue(runnable.isDone());
            assertTrue(runnable.isCancelled());
        }
    }
    
    /**
     * Tests that scheduling.isRepeating() and scheduling.isTheoreticalTimeSet()
     * are always false after run() if user called onCancel() in it,
     * even if user did set next theoretical time in every possible method.
     */
    public void test_neverRepeatIfCancelled() {
        final TestScheduler scheduler = new TestScheduler();
        
        for (boolean mustCancelInOnBegin : new boolean[]{false,true}) {
            for (boolean mustCancelInRunImpl : new boolean[]{false,true}) {
                if (DEBUG) {
                    System.out.println();
                    System.out.println("mustCancelInOnBegin = " + mustCancelInOnBegin);
                    System.out.println("mustCancelInRunImpl = " + mustCancelInRunImpl);
                }
                
                final MyRepRunnable runnable = new MyRepRunnable(scheduler) {
                    @Override
                    public void onCancel() {
                        super.onCancel();
                        // Should never cause re-schedule, since we just cancelled.
                        this.setNextTheoreticalTimeNs(1L);
                    }
                    @Override
                    public void onEnd() {
                        super.onEnd();
                        // Should never cause re-schedule, since we are stopping.
                        this.setNextTheoreticalTimeNs(1L);
                    }
                    @Override
                    public void onDone() {
                        super.onDone();
                        // Should never cause re-schedule, since we are stopping.
                        this.setNextTheoreticalTimeNs(1L);
                    }
                };

                runnable.mustCancelInOnBegin = mustCancelInOnBegin;
                runnable.mustCancelInRunImpl = mustCancelInRunImpl;
                final boolean expectedCancelled = (mustCancelInOnBegin || mustCancelInRunImpl);
                if (expectedCancelled) {
                    // Asking for re-schedule, to check it will be ignored
                    // due to onCancel() call(s).
                    runnable.mustSetNextTheoreticalTimeInOnBegin = true;
                    runnable.mustSetNextTheoreticalTimeInRunImpl = true;
                    runnable.nextTheoreticalTimeToUseNs = 1L;
                }

                runnable.clearTheoreticalTime();
                runnable.run();

                assertEquals(1, runnable.nbrOfOnBeginCalls);
                if (mustCancelInOnBegin) {
                    assertEquals(0, runnable.nbrOfRunImplCalls);
                } else {
                    assertEquals(1, runnable.nbrOfRunImplCalls);
                }
                assertEquals(1, runnable.nbrOfOnEndCalls);
                assertEquals(1, runnable.nbrOfOnDoneCalls);
                assertTrue(runnable.isDone());
                assertEquals(expectedCancelled, runnable.isCancelled());
                assertFalse(runnable.isRepeating());
                assertFalse(runnable.isTheoreticalTimeSet());
            }
        }
    }

    /*
     * life cycle methods
     */
    
    public void test_isPending() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertTrue(runnable.isPending());
        }

        /*
         * value after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isPending());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertFalse(runnable.isPending());
        }
    }
    
    public void test_isOnBeginBeingCalled() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertFalse(runnable.isOnBeginBeingCalled());
        }

        /*
         * value after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isOnBeginBeingCalled());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertFalse(runnable.isOnBeginBeingCalled());
        }
    }
    
    public void test_isRepeating() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertFalse(runnable.isRepeating());
        }

        /*
         * value after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertTrue(runnable.isRepeating());
            assertEquals(1, scheduler.get_executeAtNs_callCount());
            assertEquals(runnable.nextTheoreticalTimeToUseNs, scheduler.get_executeAtNs_timeNs());
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isRepeating());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertFalse(runnable.isRepeating());
        }
    }

    public void test_isOnEndBeingCalled() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertFalse(runnable.isOnEndBeingCalled());
        }

        /*
         * value after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isOnEndBeingCalled());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertFalse(runnable.isOnEndBeingCalled());
        }
    }

    public void test_isOnDoneBeingCalled() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertFalse(runnable.isOnDoneBeingCalled());
        }

        /*
         * value after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isOnDoneBeingCalled());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertFalse(runnable.isOnDoneBeingCalled());
        }
    }

    public void test_isDone() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertFalse(runnable.isDone());
        }

        /*
         * value after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isDone());
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertTrue(runnable.isDone());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertTrue(runnable.isDone());
        }
    }

    public void test_isTerminatingOrDone() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertFalse(runnable.isTerminatingOrDone());
        }

        /*
         * value after run()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isTerminatingOrDone());
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertTrue(runnable.isTerminatingOrDone());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertTrue(runnable.isTerminatingOrDone());
        }
    }
    
    public void test_isCancelled() {
        final TestScheduler scheduler = new TestScheduler();
        
        /*
         * default
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            assertFalse(runnable.isCancelled());
        }

        /*
         * value after process
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);

            runnable.mustSetNextTheoreticalTimeInRunImpl = true;
            runnable.nextTheoreticalTimeToUseNs = 1L;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isCancelled());
            
            runnable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            runnable.clearTheoreticalTime();
            runnable.run();
            
            assertFalse(runnable.isCancelled());
        }

        /*
         * value after onCancel
         */
        
        {
            final MyRepRunnable runnable = new MyRepRunnable(scheduler);
            
            runnable.onCancel();
            assertTrue(runnable.isCancelled());
        }
    }
    
    /*
     * First theoretical time.
     */
    
    public void test_firstTheoreticalTime_notSet() {
        final TestScheduler scheduler = new TestScheduler();
        final MyRepRunnable runnable = new MyRepRunnable(scheduler);

        long nowNs = scheduler.getClock().getTimeNs();
        
        runnable.run();
        
        assertEquals(nowNs, runnable.runImpl_theoreticalTimeNs);
        assertEquals(nowNs, runnable.runImpl_actualTimeNs);
    }

    public void test_firstTheoreticalTime_set() {
        final TestScheduler scheduler = new TestScheduler();
        final MyRepRunnable runnable = new MyRepRunnable(scheduler);

        long nowNs = scheduler.getClock().getTimeNs();
        
        runnable.setNextTheoreticalTimeNs(nowNs - 1);
        runnable.run();
        
        assertEquals(nowNs - 1, runnable.runImpl_theoreticalTimeNs);
        assertEquals(nowNs, runnable.runImpl_actualTimeNs);
    }

    /*
     * Re-schedules.
     */
    
    public void test_reschedule_noOverride() {
        final TestScheduler scheduler = new TestScheduler();
        final MyRepRunnable runnable = new MyRepRunnable(scheduler);

        runnable.mustSetNextTheoreticalTimeInRunImpl = true;
        runnable.nextTheoreticalTimeToUseNs = 3L;

        runnable.run();

        assertEquals(0, scheduler.get_execute_callCount());
        assertEquals(1, scheduler.get_executeAtNs_callCount());
        assertEquals(0, scheduler.get_executeAfterNs_callCount());

        assertSame(runnable, scheduler.get_executeAtNs_runnable());
        assertEquals(3L, scheduler.get_executeAtNs_timeNs());
    }

    public void test_reschedule_noReschedule() {
        final TestScheduler scheduler = new TestScheduler();
        final AtomicLong lastRescheduleTimeNsRef = new AtomicLong();
        final MyRepRunnable runnable = new MyRepRunnable(scheduler) {
            @Override
            protected void rescheduleAtNs(long timeNs) {
                lastRescheduleTimeNsRef.set(timeNs);
            }
        };

        runnable.mustSetNextTheoreticalTimeInRunImpl = true;
        runnable.nextTheoreticalTimeToUseNs = 3L;

        runnable.run();

        assertEquals(0, scheduler.get_execute_callCount());
        assertEquals(0, scheduler.get_executeAtNs_callCount());
        assertEquals(0, scheduler.get_executeAfterNs_callCount());

        assertEquals(3L, lastRescheduleTimeNsRef.get());
    }

    public void test_reschedule_asapReschedule() {
        final TestScheduler scheduler = new TestScheduler();
        final AtomicLong lastRescheduleTimeNsRef = new AtomicLong();
        final MyRepRunnable runnable = new MyRepRunnable(scheduler) {
            @Override
            protected void rescheduleAtNs(long timeNs) {
                lastRescheduleTimeNsRef.set(timeNs);
                scheduler.execute(this);
            }
        };

        runnable.mustSetNextTheoreticalTimeInRunImpl = true;
        runnable.nextTheoreticalTimeToUseNs = 3L;

        runnable.run();

        assertEquals(1, scheduler.get_execute_callCount());
        assertEquals(0, scheduler.get_executeAtNs_callCount());
        assertEquals(0, scheduler.get_executeAfterNs_callCount());

        assertSame(runnable, scheduler.get_execute_runnable());
        assertEquals(3L, lastRescheduleTimeNsRef.get());
    }
    
    /*
     * Anti-suppressions swallowings.
     */

    /**
     * Tests both calls to swallowIfTryThrew(),
     * in run() and in terminateIfNeeded().
     */
    public void test_antiSuppressionsSwallowings() {
        final TestScheduler scheduler = new TestScheduler();
        
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 4; j++) {
                final MyRepRunnable runnable = new MyRepRunnable(scheduler);
              
                runnable.mustThrowExceptionInOnBegin = (i == 0);
                runnable.mustThrowExceptionInRunImpl = (i == 1) || (j == 1);
                runnable.mustThrowExceptionInOnEnd = (i == 2) || (j == 2);
                runnable.mustThrowExceptionInOnDone = (j == 3);
                
                Throwable catched = null;
                try {
                    runnable.run();
                    fail();
                } catch (MyRuntimeException e) {
                    catched = e;
                }

                String expectedMethod = null;
                switch (i) {
                    case 0: expectedMethod = "onBegin()"; break;
                    case 1: expectedMethod = "runImpl()"; break;
                    case 2: expectedMethod = "onEnd()"; break;
                    case 3: expectedMethod = "onDone()"; break;
                    default:
                        throw new AssertionError("" + i);
                }
                assertEquals("for test : from " + expectedMethod, catched.getMessage());
            }
        }
    }
}
