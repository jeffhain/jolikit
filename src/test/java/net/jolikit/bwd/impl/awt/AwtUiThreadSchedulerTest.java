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
package net.jolikit.bwd.impl.awt;

import java.awt.EventQueue;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.lang.DefaultExceptionHandler;
import net.jolikit.lang.RethrowException;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.ConcUnit;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.hard.NanoTimeClock;
import net.jolikit.time.sched.InterfaceCancellable;
import net.jolikit.time.sched.InterfaceSchedulable;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceScheduling;
import net.jolikit.time.sched.hard.HardScheduler;

public class AwtUiThreadSchedulerTest extends TestCase {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private class MyCounter extends AtomicInteger {
        private static final long serialVersionUID = 1L;
        public synchronized void incr() {
            this.incrementAndGet();
            this.notifyAll();
        }
        public synchronized void join(int expectedCount) {
            int value;
            while ((value = this.get()) < expectedCount) {
                if (value > expectedCount) {
                    cu.assertTrue(value + " > " + expectedCount, false);
                }
                Unchecked.wait(this);
            }
        }
    }

    private class MyRunnable implements Runnable {
        final MyCounter runCount = new MyCounter();
        @Override
        public void run() {
            this.runCount.incr();
            cu.assertTrue(EventQueue.isDispatchThread());
        }
    }

    private class MyCancellable extends MyRunnable implements InterfaceCancellable {
        final MyCounter onCancelCount = new MyCounter();
        @Override
        public void onCancel() {
            this.onCancelCount.incr();
        }
    }

    private class MySchedulable extends MyCancellable implements InterfaceSchedulable {
        final MyCounter setSchedulingCount = new MyCounter();
        private boolean firstRun;
        private InterfaceScheduling scheduling;
        @Override
        public void setScheduling(InterfaceScheduling scheduling) {
            this.firstRun = (this.scheduling == null);
            this.scheduling = scheduling;
            this.setSchedulingCount.incr();
            cu.assertTrue(EventQueue.isDispatchThread());
        }
        @Override
        public void run() {
            super.run();
            if (this.firstRun) {
                this.scheduling.setNextTheoreticalTimeNs(
                        this.scheduling.getActualTimeNs() + 1L);
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private HardScheduler timingScheduler;
    private AwtUiThreadScheduler edtScheduler;

    private final ConcUnit cu = new ConcUnit();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_execute_runnable() {
        final MyRunnable runnable = new MyRunnable();
        this.edtScheduler.execute(runnable);
        runnable.runCount.join(1);
    }

    public void test_execute_cancellable() {
        final MyCancellable cancellable = new MyCancellable();
        this.edtScheduler.execute(cancellable);
        cancellable.runCount.join(1);
        cancellable.onCancelCount.join(0);
    }

    public void test_execute_schedulable() {
        final MySchedulable schedulable = new MySchedulable();
        this.edtScheduler.execute(schedulable);
        schedulable.setSchedulingCount.join(2);
        schedulable.runCount.join(2);
        schedulable.onCancelCount.join(0);
    }

    public void test_executeAtNs_runnable() {
        final MyRunnable runnable = new MyRunnable();
        this.edtScheduler.executeAtNs(runnable, 0);
        runnable.runCount.join(1);
    }

    public void test_executeAtNs_cancellable() {
        final MyCancellable cancellable = new MyCancellable();
        this.edtScheduler.executeAtNs(cancellable, 0);
        cancellable.runCount.join(1);
        cancellable.onCancelCount.join(0);
    }

    public void test_executeAtNs_schedulable() {
        final MySchedulable schedulable = new MySchedulable();
        this.edtScheduler.executeAtNs(schedulable, 0);
        schedulable.setSchedulingCount.join(2);
        schedulable.runCount.join(2);
        schedulable.onCancelCount.join(0);
    }

    public void test_isWorkerThread() {
        for (boolean expected : new boolean[]{false,true}) {
            final InterfaceScheduler scheduler;
            if (expected) {
                scheduler = this.edtScheduler;
            } else {
                scheduler = this.timingScheduler;
            }
            final AtomicInteger result = new AtomicInteger();
            scheduler.execute(new Runnable() {
                @Override
                public void run() {
                    result.set(edtScheduler.isWorkerThread() ? 1 : -1);
                    synchronized (result) {
                        result.notifyAll();
                    }
                }
            });
            synchronized (result) {
                while (result.get() == 0) {
                    Unchecked.wait(result);
                }
            }
            assertEquals(expected ? 1 : -1, result.get());
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void setUp() {
        final InterfaceHardClock clock = new NanoTimeClock();
        final boolean daemon = true;
        this.timingScheduler = HardScheduler.newSingleThreadedInstance(
                clock,
                "test",
                daemon);
        final boolean mustSwallowElseRethrow = true;
        final UncaughtExceptionHandler exceptionHandler =
                new DefaultExceptionHandler(
                        mustSwallowElseRethrow);
        this.edtScheduler = new AwtUiThreadScheduler(
                HardClockTimeType.SYSTEM_NANO_TIME,
                exceptionHandler);
    }

    @Override
    protected void tearDown() {
        this.timingScheduler.shutdown();
        try {
            this.timingScheduler.waitForNoMoreRunningWorkerSystemTimeNs(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            throw new RethrowException(e);
        }

        this.cu.assertNoError();
    }
}
