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
package net.jolikit.time.sched.misc;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.AbstractRepeatableTask;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * To test AbstractProcess, using soft scheduling.
 * In misc package to avoid cycles.
 */
public class AbztractProcessSoftTest extends TestCase {

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
        PROCESS
    }
    
    private static class MyRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MyRuntimeException() {
            super("for test");
        }
    }
    
    private static class MyMethodHelper {
        final AbstractProcess owner;
        final MyMethod method;
        boolean beingCalled;
        int callCount;
        boolean mustStop;
        boolean didLastCallStop;
        boolean mustThrow;
        boolean didLastCallThrow;
        public MyMethodHelper(
                AbstractProcess owner,
                MyMethod method) {
            this.owner = owner;
            this.method = method;
        }
        /**
         * @param duringCall Can be null.
         */
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

                this.didLastCallStop = this.mustStop;
                if (this.mustStop) {
                    this.owner.stop();
                }

                this.didLastCallThrow = this.mustThrow;
                if (this.mustThrow) {
                    this.didLastCallThrow = true;
                    throw new MyRuntimeException();
                }
            } finally {
                this.beingCalled = false;
            }
        }
    }

    private class MyProcess extends AbstractProcess {
        
        final MyMethodHelper onBegin_helper = new MyMethodHelper(this, MyMethod.ON_BEGIN);
        
        final MyMethodHelper onEnd_helper = new MyMethodHelper(this, MyMethod.ON_END);
        
        final MyMethodHelper process_helper = new MyMethodHelper(this, MyMethod.PROCESS);
        
        private long process_lastTheoreticalTimeNs;
        private long process_lastActualTimeNs;
        
        private MyMethod lastCalled;
        
        public MyProcess(InterfaceScheduler scheduler) {
            super(scheduler);
        }

        @Override
        protected void onBegin() {
            this.onBegin_helper.onCall(new Runnable() {
                @Override
                public void run() {
                    if (onEnd_helper.beingCalled) {
                        throw new AssertionError("onBegin() called while onEnd() is being called");
                    }
                    if (process_helper.beingCalled) {
                        throw new AssertionError("onBegin() called while process(...) is being called");
                    }
                    if (lastCalled == MyMethod.ON_BEGIN) {
                        throw new AssertionError("onBegin() called after onBegin() (exception: " + onBegin_helper.didLastCallThrow + ")");
                    }
                    if (lastCalled == MyMethod.PROCESS) {
                        throw new AssertionError("onBegin() called after process(...) (exception: " + process_helper.didLastCallThrow + ")");
                    }
                    lastCalled = MyMethod.ON_BEGIN;
                }
            });
        }

        @Override
        protected void onEnd() {
            this.onEnd_helper.onCall(new Runnable() {
                @Override
                public void run() {
                    if (onBegin_helper.beingCalled) {
                        throw new AssertionError("onEnd() called while onBegin() is being called");
                    }
                    if (process_helper.beingCalled) {
                        throw new AssertionError("onEnd() called while process(...) is being called");
                    }
                    if (lastCalled == MyMethod.ON_END) {
                        throw new AssertionError("onEnd() called after onEnd() (exception: " + onEnd_helper.didLastCallThrow + ")");
                    }
                    lastCalled = MyMethod.ON_END;
                }
            });
        }

        @Override
        protected long process(final long theoreticalTimeNs, final long actualTimeNs) {
            this.process_helper.onCall(new Runnable() {
                @Override
                public void run() {
                    process_lastTheoreticalTimeNs = theoreticalTimeNs;
                    process_lastActualTimeNs = actualTimeNs;
                    if (onBegin_helper.beingCalled) {
                        throw new AssertionError("process(...) called while onBegin() is being called");
                    }
                    if (onEnd_helper.beingCalled) {
                        throw new AssertionError("process(...) called while onEnd() is being called");
                    }
                    if ((lastCalled == MyMethod.ON_BEGIN) && onBegin_helper.didLastCallStop) {
                        throw new AssertionError("process(...) called after onBegin() that did stop");
                    }
                    if ((lastCalled == MyMethod.ON_BEGIN) && onBegin_helper.didLastCallThrow) {
                        throw new AssertionError("process(...) called after onBegin() that did throw an exception");
                    }
                    if (lastCalled == MyMethod.ON_END) {
                        throw new AssertionError("process(...) called after onEnd() (exception: " + onEnd_helper.didLastCallThrow + ")");
                    }
                    if ((lastCalled == MyMethod.PROCESS) && process_helper.didLastCallStop) {
                        throw new AssertionError("process(...) called after process(...) that did stop");
                    }
                    if ((lastCalled == MyMethod.PROCESS) && process_helper.didLastCallThrow) {
                        throw new AssertionError("process(...) called after process(...) that did throw an exception");
                    }
                    lastCalled = MyMethod.PROCESS;
                }
            });
            
            return actualTimeNs + PERIOD_NS;
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * general
     */
    
    public void test_general() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        assertFalse(process.isStarted());
        assertFalse(process.isAlive());
        
        /*
         * Starting.
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
                assertTrue(process.isStarted());
                assertTrue(process.isAlive());
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        final long nowNs = clock.getTimeNs();
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(1, process.process_helper.callCount);
                        assertEquals(0, process.onEnd_helper.callCount);
                        assertEquals(nowNs, process.process_lastTheoreticalTimeNs);
                        assertEquals(nowNs, process.process_lastActualTimeNs);
                        assertTrue(process.isStarted());
                        assertTrue(process.isAlive());
                    }
                });
            }
        }, whenNs);
        
        /*
         * After a few periods.
         */
        
        // Epsilon after process calls.
        final long epsNs = 1;
        whenNs += epsNs;
        
        {
            final int n = 10;
            whenNs += n * PERIOD_NS;
            scheduler.executeAtNs(new Runnable() {
                @Override
                public void run() {
                    final long nowNs = clock.getTimeNs();
                    assertEquals(1, process.onBegin_helper.callCount);
                    assertEquals(1 + n, process.process_helper.callCount);
                    assertEquals(0, process.onEnd_helper.callCount);
                    assertEquals(nowNs - epsNs, process.process_lastTheoreticalTimeNs);
                    assertEquals(nowNs - epsNs, process.process_lastActualTimeNs);
                    assertTrue(process.isStarted());
                    assertTrue(process.isAlive());
                }
            }, whenNs);
        }
        
        /*
         * Stopping.
         */

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.stop();
                assertFalse(process.isStarted());
                assertTrue(process.isAlive());
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        final long nowNs = clock.getTimeNs();
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(1 + 10, process.process_helper.callCount);
                        assertEquals(1, process.onEnd_helper.callCount);
                        assertEquals(nowNs - epsNs, process.process_lastTheoreticalTimeNs);
                        assertEquals(nowNs - epsNs, process.process_lastActualTimeNs);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);
        
        /*
         * No new calls after some time.
         */
        
        whenNs += 10 * PERIOD_NS;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.stop();
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(1 + 10, process.process_helper.callCount);
                        assertEquals(1, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);

        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        helper.startNowAndStopAtNs(stopTimeNs);
    }
    
    /*
     * start/stop
     */

    public void test_startStop() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        assertFalse(process.isStarted());
        assertFalse(process.isAlive());
        
        /*
         * Starting and stopping.
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
                assertTrue(process.isStarted());
                assertTrue(process.isAlive());
                
                process.stop();
                assertFalse(process.isStarted());
                assertTrue(process.isAlive());
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(0, process.onBegin_helper.callCount);
                        assertEquals(0, process.process_helper.callCount);
                        assertEquals(0, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);
        
        whenNs += 1;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(0, process.onBegin_helper.callCount);
                        assertEquals(0, process.process_helper.callCount);
                        assertEquals(0, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);
        
        /*
         * Stop must be idempotent.
         */
        
        whenNs += 1;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.stop();
                assertFalse(process.isStarted());
                assertFalse(process.isAlive());
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(0, process.onBegin_helper.callCount);
                        assertEquals(0, process.process_helper.callCount);
                        assertEquals(0, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);

        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        helper.startNowAndStopAtNs(stopTimeNs);
    }
    
    /*
     * onBegin
     */
    
    public void test_onBegin_stops() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        process.onBegin_helper.mustStop = true;
        
        /*
         * 
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Executed after things scheduled in start().
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(0, process.process_helper.callCount);
                        assertEquals(1, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);

        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        helper.startNowAndStopAtNs(stopTimeNs);
    }
    
    public void test_onBegin_throws() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        process.onBegin_helper.mustThrow = true;
        
        /*
         * 
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
            }
        }, whenNs);

        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        try {
            helper.startNowAndStopAtNs(stopTimeNs);
            fail();
        } catch (MyRuntimeException e) {
            assertEquals(1, process.onBegin_helper.callCount);
            assertEquals(0, process.process_helper.callCount);
            assertEquals(1, process.onEnd_helper.callCount);
        }
    }
    
    /*
     * onEnd
     */
    
    /**
     * Just checking that it doesn't hurt.
     */
    public void test_onEnd_stops() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        process.onEnd_helper.mustStop = true;
        
        /*
         * 
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(1, process.process_helper.callCount);
                        assertEquals(0, process.onEnd_helper.callCount);
                        assertTrue(process.isStarted());
                        assertTrue(process.isAlive());
                    }
                });
            }
        }, whenNs);
        
        whenNs += 1;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.stop();
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(1, process.process_helper.callCount);
                        assertEquals(1, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);
        
        whenNs += 1;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(1, process.process_helper.callCount);
                        assertEquals(1, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);
        
        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        helper.startNowAndStopAtNs(stopTimeNs);
    }
    
    /**
     * Just checking that it doesn't hurt.
     */
    public void test_onEnd_throws() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        process.onEnd_helper.mustThrow = true;
        
        /*
         * 
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
            }
        }, whenNs);
        
        whenNs += 1;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.stop();
            }
        }, whenNs);

        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        try {
            helper.startNowAndStopAtNs(stopTimeNs);
            fail();
        } catch (MyRuntimeException e) {
            assertEquals(1, process.onBegin_helper.callCount);
            assertEquals(1, process.process_helper.callCount);
            assertEquals(1, process.onEnd_helper.callCount);
            assertFalse(process.isStarted());
            assertFalse(process.isAlive());
        }
    }
    
    /*
     * process
     */
    
    public void test_process_stops() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        process.process_helper.mustStop = true;
        
        /*
         * 
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
                //
                scheduler.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertEquals(1, process.onBegin_helper.callCount);
                        assertEquals(1, process.process_helper.callCount);
                        assertEquals(1, process.onEnd_helper.callCount);
                        assertFalse(process.isStarted());
                        assertFalse(process.isAlive());
                    }
                });
            }
        }, whenNs);

        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        helper.startNowAndStopAtNs(stopTimeNs);
    }
    
    public void test_process_throws() {
        final SoftTestHelper helper = new SoftTestHelper();
        
        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();
        
        final InterfaceScheduler scheduler = helper.getScheduler();
        
        final MyProcess process = new MyProcess(scheduler);
        process.process_helper.mustThrow = true;
        
        /*
         * 
         */
        
        long whenNs = initialTimeNs;
        
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
            }
        }, whenNs);

        /*
         * 
         */
        
        final long stopTimeNs = whenNs + 1;
        try {
            helper.startNowAndStopAtNs(stopTimeNs);
            fail();
        } catch (MyRuntimeException e) {
            assertEquals(1, process.onBegin_helper.callCount);
            assertEquals(1, process.process_helper.callCount);
            assertEquals(1, process.onEnd_helper.callCount);
        }
    }
    
    /*
     * Re-schedules.
     */
    
    public void test_reschedule_exactPeriod() {
        this.test_reschedule(PERIOD_NS);
    }
    
    public void test_reschedule_halfPeriod() {
        this.test_reschedule(PERIOD_NS/2);
    }
    
    public void test_reschedule(final long myPeriodNs) {
        final SoftTestHelper helper = new SoftTestHelper();

        final InterfaceClock clock = helper.getClock();
        final long initialTimeNs = clock.getTimeNs();

        final InterfaceScheduler scheduler = helper.getScheduler();

        final AtomicReference<Runnable> lastRescheduleTaskRef = new AtomicReference<Runnable>();
        final AtomicLong lastRescheduleTimeNsRef = new AtomicLong();
        final MyProcess process = new MyProcess(scheduler) {
            @Override
            protected void rescheduleCurrentTaskAtNs(Runnable task, long timeNs) {
                scheduler.executeAfterNs(task, myPeriodNs);

                lastRescheduleTaskRef.set(task);
                lastRescheduleTimeNsRef.set(timeNs);
            }
        };

        /*
         * 
         */

        long whenNs = initialTimeNs;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                process.start();
            }
        }, whenNs);
        
        /*
         * 
         */
        
        whenNs += 1;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                // No getter to check the reference, but we know it must be a repeatable task.
                assertTrue(lastRescheduleTaskRef.get() instanceof AbstractRepeatableTask);

                assertEquals(1, process.process_helper.callCount);
                assertEquals(initialTimeNs + PERIOD_NS, lastRescheduleTimeNsRef.get());
            }
        }, whenNs);

        /*
         * 
         */
        
        whenNs += myPeriodNs - 1;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                // No second process yet.
                assertEquals(1, process.process_helper.callCount);
                assertEquals(initialTimeNs + PERIOD_NS, lastRescheduleTimeNsRef.get());
            }
        }, whenNs);

        /*
         * 
         */

        whenNs += 2;

        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                assertEquals(2, process.process_helper.callCount);
                assertEquals(initialTimeNs + PERIOD_NS + myPeriodNs, lastRescheduleTimeNsRef.get());
            }
        }, whenNs);

        /*
         * 
         */

        final long stopTimeNs = whenNs + 1;
        helper.startNowAndStopAtNs(stopTimeNs);
    }
}
