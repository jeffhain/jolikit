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

import net.jolikit.time.sched.AbstractSmartSchedulable;
import net.jolikit.time.sched.InterfaceScheduling;
import net.jolikit.time.sched.DefaultScheduling;
import junit.framework.TestCase;

/**
 * To test AbstractSmartSchedulable.
 */
public class AbztractSmartSchedulableTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MySmartSchedulable extends AbstractSmartSchedulable {
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
        private boolean mustSetNextTheoreticalTimeInOnBegin = false;
        private boolean mustSetNextTheoreticalTimeInRunImpl = false;
        private long nextTheoreticalTimeToUseNs = 0;
        public void resetAndSetScheduling(DefaultScheduling scheduling) {
            resetScheduling(scheduling);
            this.setScheduling(scheduling);
        }
        @Override
        protected void onBegin() {
            final InterfaceScheduling scheduling = this.getScheduling();
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
                scheduling.setNextTheoreticalTimeNs(nextTheoreticalTimeToUseNs);
            }
            if (mustThrowExceptionInOnBegin) {
                throw new RuntimeException("for test");
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
                throw new RuntimeException("for test");
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
                throw new RuntimeException("for test");
            }
        }
        @Override
        protected void runImpl() {
            final InterfaceScheduling scheduling = this.getScheduling();
            nbrOfRunImplCalls++;
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
                scheduling.setNextTheoreticalTimeNs(nextTheoreticalTimeToUseNs);
            }
            if (mustThrowExceptionInRunImpl) {
                throw new RuntimeException("for test");
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_AbstractSmartSchedulable() {
        // Already covered.
    }
    
    public void test_reset() {
        final MySmartSchedulable schedulable = new MySmartSchedulable();
        final DefaultScheduling scheduling = new DefaultScheduling();
        
        schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
        
        schedulable.resetAndSetScheduling(scheduling);
        schedulable.run();
        assertFalse(schedulable.isPending());
        
        schedulable.reset();
        assertTrue(schedulable.isPending());
    }
    
    public void test_run() {
        
        /*
         * 1 call then done
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }

        /*
         * 2 calls then done
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            assertEquals(schedulable.nextTheoreticalTimeToUseNs, scheduling.getNextTheoreticalTimeNs());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(0, schedulable.nbrOfOnEndCalls);
            assertEquals(0, schedulable.nbrOfOnDoneCalls);
            assertFalse(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
            
            /*
             * 
             */
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(2, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }

        /*
         * onCancel() called in onBegin(): triggers termination
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            schedulable.mustCancelInOnBegin = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            // Not set.
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(0, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertTrue(schedulable.isCancelled());
        }

        /*
         * onCancel() called in runImpl(): triggers termination
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            schedulable.mustCancelInRunImpl = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            // Set to 1L after call to onCancel(), but then cleared
            // after call to runImpl(), due to schedulable now being cancelled.
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertTrue(schedulable.isCancelled());
        }

        /*
         * onCancel() called in onEnd(): does not cause trouble (does nothing)
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustCancelInOnEnd = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }

        /*
         * onCancel() called in onDone(): does not cause trouble (does nothing)
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustCancelInOnDone = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }

        /*
         * exception in onBegin(): terminates (not cancelled)
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            schedulable.mustThrowExceptionInOnBegin = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            try {
                schedulable.run();
            } catch (Exception ignore) {
            }
            // Not set.
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(0, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }

        /*
         * exception in runImpl(): terminates (not cancelled)
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            schedulable.mustThrowExceptionInRunImpl = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            try {
                schedulable.run();
            } catch (Exception ignore) {
            }
            // Not set.
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }

        /*
         * exception in onEnd() called by run(): still calls onDone(), and sets status to done
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustThrowExceptionInOnEnd = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            try {
                schedulable.run();
            } catch (Exception ignore) {
            }
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }

        /*
         * exception in onDone() called by run(): still sets status to done
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustThrowExceptionInOnDone = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            try {
                schedulable.run();
            } catch (Exception ignore) {
            }
            assertFalse(scheduling.isNextTheoreticalTimeSet());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertFalse(schedulable.isCancelled());
        }
    }
    
    public void test_onCancel() {
        /*
         * onCancel before process
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertEquals(0, schedulable.nbrOfOnBeginCalls);
            assertEquals(0, schedulable.nbrOfRunImplCalls);
            assertEquals(0, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertTrue(schedulable.isCancelled());
        }

        /*
         * exception in onDone(), called by onCancel(): still sets status to done
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.mustThrowExceptionInOnDone = true;
            try {
                schedulable.onCancel();
            } catch (Exception ignore) {
            }
            assertEquals(0, schedulable.nbrOfOnBeginCalls);
            assertEquals(0, schedulable.nbrOfRunImplCalls);
            assertEquals(0, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertTrue(schedulable.isCancelled());
        }

        /*
         * onCancel() after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            assertEquals(schedulable.nextTheoreticalTimeToUseNs, scheduling.getNextTheoreticalTimeNs());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(0, schedulable.nbrOfOnEndCalls);
            assertEquals(0, schedulable.nbrOfOnDoneCalls);
            assertFalse(schedulable.isDone());
            assertFalse(schedulable.isCancelled());

            schedulable.onCancel();
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertTrue(schedulable.isCancelled());
        }

        /*
         * exception in onEnd(), called by onCancel: still calls onDone, and sets status to done
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            assertEquals(schedulable.nextTheoreticalTimeToUseNs, scheduling.getNextTheoreticalTimeNs());
            
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(0, schedulable.nbrOfOnEndCalls);
            assertEquals(0, schedulable.nbrOfOnDoneCalls);
            assertFalse(schedulable.isDone());
            assertFalse(schedulable.isCancelled());

            schedulable.mustThrowExceptionInOnEnd = true;
            try {
                schedulable.onCancel();
            } catch (Exception ignore) {
            }
            assertEquals(1, schedulable.nbrOfOnBeginCalls);
            assertEquals(1, schedulable.nbrOfRunImplCalls);
            assertEquals(1, schedulable.nbrOfOnEndCalls);
            assertEquals(1, schedulable.nbrOfOnDoneCalls);
            assertTrue(schedulable.isDone());
            assertTrue(schedulable.isCancelled());
        }
    }
    
    /**
     * Tests that scheduling.isRepeating() and scheduling.isNextTheoreticalTimeSet()
     * are always false after run() if user called onCancel() in it,
     * even if user did set next theoretical time in every possible method.
     */
    public void test_neverRepeatIfCancelled() {
        for (boolean mustCancelInOnBegin : new boolean[]{false,true}) {
            for (boolean mustCancelInRunImpl : new boolean[]{false,true}) {
                if (DEBUG) {
                    System.out.println();
                    System.out.println("mustCancelInOnBegin = " + mustCancelInOnBegin);
                    System.out.println("mustCancelInRunImpl = " + mustCancelInRunImpl);
                }
                
                final MySmartSchedulable schedulable = new MySmartSchedulable() {
                    @Override
                    public void onCancel() {
                        super.onCancel();
                        // Should never cause re-schedule, since we just cancelled.
                        this.getScheduling().setNextTheoreticalTimeNs(1L);
                    }
                    @Override
                    public void onEnd() {
                        super.onEnd();
                        // Should never cause re-schedule, since we are stopping.
                        this.getScheduling().setNextTheoreticalTimeNs(1L);
                    }
                    @Override
                    public void onDone() {
                        super.onDone();
                        // Should never cause re-schedule, since we are stopping.
                        this.getScheduling().setNextTheoreticalTimeNs(1L);
                    }
                };
                final DefaultScheduling scheduling = new DefaultScheduling();

                schedulable.mustCancelInOnBegin = mustCancelInOnBegin;
                schedulable.mustCancelInRunImpl = mustCancelInRunImpl;
                final boolean expectedCancelled = (mustCancelInOnBegin || mustCancelInRunImpl);
                if (expectedCancelled) {
                    // Asking for re-schedule, to check it will be ignored
                    // due to onCancel() call(s).
                    schedulable.mustSetNextTheoreticalTimeInOnBegin = true;
                    schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
                    schedulable.nextTheoreticalTimeToUseNs = 1L;
                }

                schedulable.resetAndSetScheduling(scheduling);
                schedulable.run();

                assertEquals(1, schedulable.nbrOfOnBeginCalls);
                if (mustCancelInOnBegin) {
                    assertEquals(0, schedulable.nbrOfRunImplCalls);
                } else {
                    assertEquals(1, schedulable.nbrOfRunImplCalls);
                }
                assertEquals(1, schedulable.nbrOfOnEndCalls);
                assertEquals(1, schedulable.nbrOfOnDoneCalls);
                assertTrue(schedulable.isDone());
                assertEquals(expectedCancelled, schedulable.isCancelled());
                assertFalse(schedulable.isRepeating());
                assertFalse(schedulable.getScheduling().isNextTheoreticalTimeSet());
            }
        }
    }

    /*
     * life cycle methods
     */
    
    public void test_isPending() {
        
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertTrue(schedulable.isPending());
        }

        /*
         * value after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isPending());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertFalse(schedulable.isPending());
        }
    }
    
    public void test_isOnBeginBeingCalled() {
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertFalse(schedulable.isOnBeginBeingCalled());
        }

        /*
         * value after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isOnBeginBeingCalled());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertFalse(schedulable.isOnBeginBeingCalled());
        }
    }
    
    public void test_isRepeating() {
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertFalse(schedulable.isRepeating());
        }

        /*
         * value after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertTrue(schedulable.isRepeating());
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isRepeating());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertFalse(schedulable.isRepeating());
        }
    }

    public void test_isOnEndBeingCalled() {
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertFalse(schedulable.isOnEndBeingCalled());
        }

        /*
         * value after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isOnEndBeingCalled());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertFalse(schedulable.isOnEndBeingCalled());
        }
    }

    public void test_isOnDoneBeingCalled() {
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertFalse(schedulable.isOnDoneBeingCalled());
        }

        /*
         * value after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isOnDoneBeingCalled());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertFalse(schedulable.isOnDoneBeingCalled());
        }
    }

    public void test_isDone() {
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertFalse(schedulable.isDone());
        }

        /*
         * value after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isDone());
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertTrue(schedulable.isDone());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertTrue(schedulable.isDone());
        }
    }

    public void test_isTerminatingOrDone() {
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertFalse(schedulable.isTerminatingOrDone());
        }

        /*
         * value after run()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isTerminatingOrDone());
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertTrue(schedulable.isTerminatingOrDone());
        }

        /*
         * value after onCancel()
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertTrue(schedulable.isTerminatingOrDone());
        }
    }
    
    public void test_isCancelled() {
        /*
         * default
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            assertFalse(schedulable.isCancelled());
        }

        /*
         * value after process
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            final DefaultScheduling scheduling = new DefaultScheduling();

            schedulable.mustSetNextTheoreticalTimeInRunImpl = true;
            schedulable.nextTheoreticalTimeToUseNs = 1L;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isCancelled());
            
            schedulable.mustSetNextTheoreticalTimeInRunImpl = false;
            
            schedulable.resetAndSetScheduling(scheduling);
            schedulable.run();
            
            assertFalse(schedulable.isCancelled());
        }

        /*
         * value after onCancel
         */
        
        {
            final MySmartSchedulable schedulable = new MySmartSchedulable();
            
            schedulable.onCancel();
            assertTrue(schedulable.isCancelled());
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void resetScheduling(DefaultScheduling scheduling) {
        scheduling.configureBeforeRun(0, 0);
    }
}
