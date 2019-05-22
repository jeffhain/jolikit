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
package net.jolikit.threading.locks;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.lang.RethrowException;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.ConcUnit;

/**
 * Test some methods of known condilocks.
 */
public class CondilocksTest extends TestCase {

    /*
     * To keep things simple and test more code,
     * some tests assume that there is no spurious wake-up.
     * If a test fails possibly due to a spurious wake-up,
     * run it again for check.
     * 
     * Splitting test methods between timeouts
     * for them not to run for too long.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * Increase if too small for your system.
     */
    private static final long TOLERANCE_NS = 50L * 1000L * 1000L;

    private static final long SMALL_NS = 2 * TOLERANCE_NS;

    /**
     * Just one hour, to check the usual path
     * of non-pathologically-huge times or timeouts.
     */
    private static final long BIG_NS = 3600L * 1000L * 1000L * 1000L;
    private static final long MAX_NS = Long.MAX_VALUE;

    /*
     * Tested condilocks.
     */

    private static ArrayList<MyCondilockWrapper> newCondilockWrapperList() {
        final ArrayList<MyCondilockWrapper> list = new ArrayList<MyCondilockWrapper>();
        //
        list.add(new MyCondilockWrapper(newYieldingCondilock(), 0L));
        //
        // Typically sleeping for more than 1ns,
        // but our tolerance is larger than the error.
        list.add(new MyCondilockWrapper(newSleepingCondilock(), 1L));
        //
        list.add(new MyCondilockWrapper(newMonitorCondilock(1L), 1L));
        list.add(new MyCondilockWrapper(newMonitorCondilock(SMALL_NS), SMALL_NS));
        list.add(new MyCondilockWrapper(newMonitorCondilock(BIG_NS), BIG_NS));
        list.add(new MyCondilockWrapper(newMonitorCondilock(MAX_NS), MAX_NS));
        //
        list.add(new MyCondilockWrapper(newLockCondilock(1L), 1L));
        list.add(new MyCondilockWrapper(newLockCondilock(SMALL_NS), SMALL_NS));
        list.add(new MyCondilockWrapper(newLockCondilock(BIG_NS), BIG_NS));
        list.add(new MyCondilockWrapper(newLockCondilock(MAX_NS), MAX_NS));
        return list;
    }

    private static InterfaceCondilock newYieldingCondilock() {
        return new YieldingCondilock();
    }

    private static InterfaceCondilock newSleepingCondilock() {
        return new SleepingCondilock();
    }

    private static InterfaceCondilock newMonitorCondilock(
            long maxBlockingWaitChunkNs) {
        return new MyMonitorCondilock(maxBlockingWaitChunkNs);
    }

    private static InterfaceCondilock newLockCondilock(
            long maxBlockingWaitChunkNs) {
        return new MyLockCondilock(maxBlockingWaitChunkNs);
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyMethod {
        AWAIT(true, false, false),
        //
        AWAIT_UNINTERRUPTIBLY(false, false, false),
        //
        AWAIT_NANOS_long(true, true, false),
        AWAIT_long_tu(true, true, false),
        AWAIT_UNTIL_Date(true, true, false),
        AWAIT_UNTIL_NANOS_TIMEOUT_TIME_long(true, true, false),
        AWAIT_UNTIL_NANOS_long(true, true, false),
        //
        AWAIT_WHILE_FALSE_IN_LOCK_UNINTERRUPTIBLY_bc(false, false, true),
        //
        AWAIT_NANOS_WHILE_FALSE_IN_LOCK__bc(true, true, true),
        AWAIT_UNTIL_NANOS_TIMEOUT_TIME_WHILE_FALSE_IN_LOCK__bc(true, true, true),
        AWAIT_UNTLI_NANOS_WHILE_FALSE_IN_LOCK__bc(true, true, true);
        final boolean interruptible;
        final boolean waitingForTime;
        final boolean waitingForBc;
        private MyMethod(boolean interruptible, boolean waitingForTime, boolean waitingForBc) {
            this.interruptible = interruptible;
            this.waitingForTime = waitingForTime;
            this.waitingForBc = waitingForBc;
        }
    }

    /**
     * A condilock plus info about it.
     */
    private static class MyCondilockWrapper {
        final InterfaceCondilock condilock;
        final long maxWaitChunkNs;
        public MyCondilockWrapper(
                InterfaceCondilock condilock,
                long maxWaitChunkNs) {
            this.condilock = condilock;
            this.maxWaitChunkNs = maxWaitChunkNs;
        }
        @Override
        public String toString() {
            return "[" + this.condilock.getClass().getSimpleName() + ", maxWaitChunkNs = " + this.maxWaitChunkNs + "]";
        }
    }

    /**
     * To wait (once) using a condilock.
     */
    private static class MyWaiter {
        final MyCondilockWrapper cw;
        final long timeoutNs;
        volatile boolean bcValue = false;
        final InterfaceBooleanCondition bc = new InterfaceBooleanCondition() {
            @Override
            public boolean isTrue() {
                return bcValue;
            }
        };
        /**
         * Uses max possible timeout (so that it hopefully blocks
         * if used without being specified with other constructor).
         */
        public MyWaiter(MyCondilockWrapper cw) {
            this(cw, MAX_NS);
        }
        public MyWaiter(
                MyCondilockWrapper cw,
                long timeoutNs) {
            this.cw = cw;
            this.timeoutNs = timeoutNs;
        }
        @Override
        public String toString() {
            return this.getClass().getSimpleName()
                    + "[cw : " + this.cw
                    + ", timeoutNs = " + this.timeoutNs + "]";
        }
        public void setBcTrueAndSignalAllInLock() {
            this.bcValue = true;
            this.cw.condilock.signalAllInLock();
        }
        public void awaitInLock(MyMethod method) throws InterruptedException {
            call_xxx_InLock(this.cw.condilock, method, this.bc, this.timeoutNs);
        }
    }

    /**
     * For wait monitoring.
     */
    private static class MyWaiterHelper {
        private final MyWaiter waiter;
        private final AtomicReference<Thread> runningThread = new AtomicReference<Thread>();

        private volatile boolean stopIfInterruptedOnNormalWaitEnd = false;
        private volatile boolean rewaitIfNormalWaitEndAndDidntStop = false;

        private volatile boolean normalWaitEndEncountered = false;
        private volatile boolean interruptedExceptionThrown = false;
        private volatile boolean interruptedStatusSetOnLastNormalWaitEnd = false;
        private final AtomicLong actualEndTimeNs = new AtomicLong(Long.MAX_VALUE);
        public MyWaiterHelper(MyWaiter waiter) {
            this.waiter = waiter;
        }
        public void launchWait(final MyMethod method, Executor executor) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runningThread.set(Thread.currentThread());
                    try {
                        do {
                            waiter.awaitInLock(method);
                            normalWaitEndEncountered = true;
                            if (stopIfInterruptedOnNormalWaitEnd) {
                                if (Thread.interrupted()) {
                                    interruptedStatusSetOnLastNormalWaitEnd = true;
                                    break;
                                }
                            } else {
                                interruptedStatusSetOnLastNormalWaitEnd = Thread.currentThread().isInterrupted();
                            }
                        } while (rewaitIfNormalWaitEndAndDidntStop);
                    } catch (InterruptedException e) {
                        interruptedExceptionThrown = true;
                    } finally {
                        actualEndTimeNs.set(nowNs());
                    }
                }
            });
        }
        /**
         * @return Interruption time, in nanoseconds.
         */
        public long interruptRunnerWhenKnown() {
            Thread runner;
            while ((runner = this.runningThread.get()) == null) {
                Thread.yield();
            }
            final long interruptTimeNs = nowNs();
            runner.interrupt();
            return interruptTimeNs;
        }
        public long getEndTimeNsBlocking() {
            long endTimeNs;
            while ((endTimeNs = this.actualEndTimeNs.get()) == Long.MAX_VALUE) {
                Thread.yield();
            }
            return endTimeNs;
        }
        /**
         * @return True if (possibly multiple) waiting(s) has terminated,
         *         false otherwise (possibly because we didn't start to wait yet).
         */
        public boolean waitingNotTerminated() {
            return (this.actualEndTimeNs.get() == Long.MAX_VALUE);
        }
        public boolean isNormalWaitEndEncountered() {
            return this.normalWaitEndEncountered;
        }
        public boolean isInterruptedExceptionThrown() {
            return this.interruptedExceptionThrown;
        }
        public boolean isInterruptedStatusSetOnLastNormalWaitEnd() {
            return this.interruptedStatusSetOnLastNormalWaitEnd;
        }
    }

    /*
     * 
     */

    private static class MyMonitorCondilock extends MonitorCondilock {
        private final long maxBlockingWaitChunkNs;
        public MyMonitorCondilock(long maxBlockingWaitChunkNs) {
            this.maxBlockingWaitChunkNs = maxBlockingWaitChunkNs;
        }
        @Override
        protected boolean getMustUseMaxBlockingWaitChunks() {
            return ConditionsUtilz.isTimeoutNotHuge(this.maxBlockingWaitChunkNs);
        }
        @Override
        public long getMaxBlockingWaitChunkNs(long elapsedTimeNs) {
            return this.maxBlockingWaitChunkNs;
        }
        @Override
        public long getMaxDeadlineBlockingWaitChunkNs() {
            return this.maxBlockingWaitChunkNs;
        }
    }

    private static class MyLockCondilock extends LockCondilock {
        private final long maxBlockingWaitChunkNs;
        public MyLockCondilock(long maxBlockingWaitChunkNs) {
            this.maxBlockingWaitChunkNs = maxBlockingWaitChunkNs;
        }
        @Override
        protected boolean getMustUseMaxBlockingWaitChunks() {
            return ConditionsUtilz.isTimeoutNotHuge(this.maxBlockingWaitChunkNs);
        }
        @Override
        public long getMaxBlockingWaitChunkNs(long elapsedTimeNs) {
            return this.maxBlockingWaitChunkNs;
        }
        @Override
        public long getMaxDeadlineBlockingWaitChunkNs() {
            return this.maxBlockingWaitChunkNs;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final ConcUnit cu = new ConcUnit();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void testStoppedOnMaxWaitChunk() {
        for (MyCondilockWrapper cw : newCondilockWrapperList()) {
            if (cw.maxWaitChunkNs > SMALL_NS) {
                // Would wait for too long for test.
                continue;
            }

            for (MyMethod method : MyMethod.values()) {
                if (method.waitingForBc) {
                    // Would never stop.
                    continue;
                }

                testStoppedOnTime(new MyWaiter(cw), method, cw.maxWaitChunkNs);
            }
        }
    }

    /*
     * 
     */

    public void testStoppedOnTime_NEG_SMALL_NS() {
        testStoppedOnTime(-SMALL_NS);
    }

    public void testStoppedOnTime_ZERO_NS() {
        testStoppedOnTime(0L);
    }

    public void testStoppedOnTime_ONE_NS() {
        testStoppedOnTime(1L);
    }

    public void testStoppedOnTime_SMALL_NS() {
        testStoppedOnTime(SMALL_NS);
    }

    public void testStoppedOnTime(long timeoutNs) {
        for (MyCondilockWrapper cw : newCondilockWrapperList()) {
            for (MyMethod method : MyMethod.values()) {
                if (!method.waitingForTime) {
                    continue;
                }
                if ((!method.waitingForBc)
                        && (cw.maxWaitChunkNs < timeoutNs)) {
                    // Would stop too early.
                    continue;
                }

                final long expectedNs;
                if (method.waitingForTime) {
                    expectedNs = Math.max(0L, timeoutNs);
                } else {
                    expectedNs = cw.maxWaitChunkNs;
                }

                testStoppedOnTime(new MyWaiter(cw, timeoutNs), method, expectedNs);
            }
        }
    }

    /*
     * 
     */

    public void testStoppedByBcAndSignal_SMALL_NS() {
        testStoppedByBcAndSignal(SMALL_NS);
    }

    public void testStoppedByBcAndSignal_BIG_NS() {
        testStoppedByBcAndSignal(BIG_NS);
    }

    public void testStoppedByBcAndSignal_MAX_NS() {
        testStoppedByBcAndSignal(MAX_NS);
    }

    public void testStoppedByBcAndSignal(long timeoutNs) {
        for (MyCondilockWrapper cw : newCondilockWrapperList()) {
            for (MyMethod method : MyMethod.values()) {
                if ((cw.maxWaitChunkNs < timeoutNs)
                        && (!method.waitingForBc)) {
                    // Would stop too early.
                    continue;
                }

                final boolean possibleSpuriousWaitStop = (!method.waitingForBc);

                testStoppedByBcAndSignal(
                        new MyWaiter(cw, timeoutNs), method, possibleSpuriousWaitStop);
            }
        }
    }

    /*
     * 
     */

    public void testInterruptionStatusPreserved_NEG_SMALL_NS() {
        testInterruptionStatusPreserved(-SMALL_NS);
    }

    public void testInterruptionStatusPreserved_ZERO_NS() {
        testInterruptionStatusPreserved(0L);
    }

    public void testInterruptionStatusPreserved_ONE_NS() {
        testInterruptionStatusPreserved(1L);
    }

    public void testInterruptionStatusPreserved_SMALL_NS() {
        testInterruptionStatusPreserved(SMALL_NS);
    }

    /**
     * Uninterruptible methods must preserve
     * (restore if they actually did wait)
     * interruption status.
     */
    public void testInterruptionStatusPreserved(long timeoutNs) {
        for (MyCondilockWrapper cw : newCondilockWrapperList()) {
            for (MyMethod method : MyMethod.values()) {
                if (method.interruptible) {
                    continue;
                }

                for (boolean initiallyInterrupted : new boolean[]{false, true}) {
                    testInterruptionStatusPreserved(
                            new MyWaiter(cw, timeoutNs), method, initiallyInterrupted);
                }
            }
        }
    }

    /*
     * 
     */

    public static void testThrowsIfInitiallyInterrupted_NEG_SMALL_NS() {
        testThrowsIfInitiallyInterrupted(-SMALL_NS);
    }

    public static void testThrowsIfInitiallyInterrupted_ZERO_NS() {
        testThrowsIfInitiallyInterrupted(0L);
    }

    public static void testThrowsIfInitiallyInterrupted_ONE_NS() {
        testThrowsIfInitiallyInterrupted(1L);
    }

    public static void testThrowsIfInitiallyInterrupted_SMALL_NS() {
        testThrowsIfInitiallyInterrupted(SMALL_NS);
    }

    public static void testThrowsIfInitiallyInterrupted(long timeoutNs) {
        for (MyCondilockWrapper cw : newCondilockWrapperList()) {
            for (MyMethod method : MyMethod.values()) {
                if (!method.interruptible) {
                    continue;
                }

                testThrowsIfInitiallyInterrupted(new MyWaiter(cw, timeoutNs), method);
            }
        }
    }

    /*
     * 
     */

    public void testStoppedByInterruption_SMALL_NS() {
        testStoppedByInterruption(SMALL_NS);
    }

    public void testStoppedByInterruption_BIG_NS() {
        testStoppedByInterruption(BIG_NS);
    }

    public void testStoppedByInterruption_MAX_NS() {
        testStoppedByInterruption(MAX_NS);
    }

    public void testStoppedByInterruption(long timeoutNs) {
        for (MyCondilockWrapper cw : newCondilockWrapperList()) {
            for (MyMethod method : MyMethod.values()) {
                if (!method.interruptible) {
                    continue;
                }

                if ((cw.maxWaitChunkNs < timeoutNs)
                        && (!method.waitingForBc)) {
                    // Would stop too early.
                    continue;
                }

                final boolean possibleSpuriousWaitStop = (!method.waitingForBc);

                testStoppedByInterruption(
                        new MyWaiter(cw, timeoutNs), method, possibleSpuriousWaitStop);
            }
        }
    }

    /*
     * 
     */

    public void testNotStoppedByInterruption_SMALL_NS() {
        testNotStoppedByInterruption(SMALL_NS);
    }

    public void testNotStoppedByInterruption_BIG_NS() {
        testNotStoppedByInterruption(BIG_NS);
    }

    public void testNotStoppedByInterruption_MAX_NS() {
        testNotStoppedByInterruption(MAX_NS);
    }

    public void testNotStoppedByInterruption(long timeoutNs) {
        for (MyCondilockWrapper cw : newCondilockWrapperList()) {
            for (MyMethod method : MyMethod.values()) {
                if (method.interruptible) {
                    continue;
                }

                if ((cw.maxWaitChunkNs < timeoutNs)
                        && (!method.waitingForBc)) {
                    // Would stop too early.
                    continue;
                }

                final boolean possibleSpuriousWaitStop = (!method.waitingForBc);

                testNotStoppedByInterruption(
                        new MyWaiter(cw, timeoutNs), method, possibleSpuriousWaitStop);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void tearDown() {
        cu.assertNoError();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * Tests that the specified waiter waits for the specified duration.
     */
    private static void testStoppedOnTime(
            MyWaiter waiter,
            MyMethod method,
            long durationNs) {

        if (DEBUG) {
            Dbg.log("testStoppedOnTime(" + waiter + ", " + method + ", " + durationNs + ")");
        }

        long a = nowNs();
        if (DEBUG) {
            Dbg.log("a = " + a);
        }

        try {
            waiter.awaitInLock(method);
        } catch (InterruptedException e) {
            throw new RethrowException(e);
        }

        long b = nowNs();
        if (DEBUG) {
            Dbg.log("b = " + b);
        }

        final long dtNs = (b-a);
        if (Math.abs(dtNs - durationNs) > TOLERANCE_NS) {
            assertTrue("expected " + durationNs + " ns, got " + dtNs + " ns", false);
        }
    }

    /**
     * Tests that boolean condition + signaling stops the wait
     * (supposing spurious wake-ups can't stop it,
     * i.e. that we are waiting for a boolean condition).
     */
    private static void testStoppedByBcAndSignal(
            MyWaiter waiter,
            MyMethod method,
            boolean possibleSpuriousWaitStop) {

        if (DEBUG) {
            Dbg.log("testStoppedByBcAndSignal(" + waiter + ", " + method + ", " + possibleSpuriousWaitStop + ")");
        }

        final ExecutorService executor = Executors.newCachedThreadPool();

        final MyWaiterHelper helper = new MyWaiterHelper(waiter);

        helper.stopIfInterruptedOnNormalWaitEnd = false;
        helper.rewaitIfNormalWaitEndAndDidntStop = possibleSpuriousWaitStop;
        helper.launchWait(method, executor);

        sleepNs(TOLERANCE_NS);

        // Rewaiting if spurious, so must still be waiting.
        assertTrue(helper.waitingNotTerminated());

        if (possibleSpuriousWaitStop) {
            // Wait stop due to signal, must not be considered as a spurious wake-up.
            helper.rewaitIfNormalWaitEndAndDidntStop = false;
        }
        final long stopTimeNs = nowNs();
        waiter.setBcTrueAndSignalAllInLock();

        assertTrue(Math.abs(helper.getEndTimeNsBlocking() - stopTimeNs) <= TOLERANCE_NS);

        assertFalse(helper.isInterruptedExceptionThrown()); // No interruption.
        assertFalse(helper.isInterruptedStatusSetOnLastNormalWaitEnd()); // No interruption.

        Unchecked.shutdownAndAwaitTermination(executor);
    }

    private static void testInterruptionStatusPreserved(
            MyWaiter waiter,
            MyMethod method,
            boolean initiallyInterrupted) {

        if (DEBUG) {
            Dbg.log("testInterruptionStatusPreserved(" + waiter + ", " + method + ", " + initiallyInterrupted + ")");
        }

        final ExecutorService executor = Executors.newCachedThreadPool();
        setBcAndSignalAfter(waiter, executor, TOLERANCE_NS);
        if (initiallyInterrupted) {
            Thread.currentThread().interrupt();
        } else {
            Thread.interrupted();
        }

        try {
            waiter.awaitInLock(method);
        } catch (InterruptedException e) {
            // Only calling uninterruptible methods.
            throw new AssertionError();
        }

        assertEquals(initiallyInterrupted, Thread.interrupted());
        Unchecked.shutdownAndAwaitTermination(executor);
    }

    private static void testThrowsIfInitiallyInterrupted(
            MyWaiter waiter,
            MyMethod method) {

        if (DEBUG) {
            Dbg.log("testThrowsIfInitiallyInterrupted(" + waiter + ", " + method + ")");
        }

        assertFalse(Thread.interrupted());

        Thread.currentThread().interrupt();
        try {
            waiter.awaitInLock(method);
            fail();
        } catch (InterruptedException e) {
            // quiet
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Tests that interruption stops the wait, by making wait method throw InterruptedException.
     */
    private static void testStoppedByInterruption(
            MyWaiter waiter,
            MyMethod method,
            boolean possibleSpuriousWaitStop) {

        if (DEBUG) {
            Dbg.log("testStoppedByInterruption(" + waiter + ", " + method + ", " + possibleSpuriousWaitStop + ")");
        }

        final ExecutorService executor = Executors.newCachedThreadPool();

        final MyWaiterHelper helper = new MyWaiterHelper(waiter);

        // Only stopping due to condilock's InterruptedException.
        helper.stopIfInterruptedOnNormalWaitEnd = false;
        helper.rewaitIfNormalWaitEndAndDidntStop = possibleSpuriousWaitStop;
        helper.launchWait(method, executor);

        sleepNs(TOLERANCE_NS);

        assertTrue(helper.waitingNotTerminated());

        final long interruptTimeNs = helper.interruptRunnerWhenKnown();

        assertTrue(Math.abs(helper.getEndTimeNsBlocking() - interruptTimeNs) <= TOLERANCE_NS);

        assertTrue(helper.isInterruptedExceptionThrown());
        if (possibleSpuriousWaitStop) {
            // Can have waiting thread interrupted right after wait method (normally) ended,
            // so having interruption status even though it has not been set by wait method after
            // catching an InterruptedException (case of non-signalable condilocks,
            // or signalable condilocks not waiting for a boolean condition).
        } else {
            assertFalse(helper.isInterruptedStatusSetOnLastNormalWaitEnd());
        }

        Unchecked.shutdownAndAwaitTermination(executor);
    }

    /**
     * Tests that interruption doesn't end the wait,
     * and that interruption status is restored before returning.
     */
    private static void testNotStoppedByInterruption(
            MyWaiter waiter,
            MyMethod method,
            boolean possibleSpuriousWaitStop) {

        if (DEBUG) {
            Dbg.log("testNotStoppedByInterruption(" + waiter + ", " + method + ", " + possibleSpuriousWaitStop + ")");
        }

        final ExecutorService executor = Executors.newCachedThreadPool();

        final MyWaiterHelper helper = new MyWaiterHelper(waiter);

        helper.stopIfInterruptedOnNormalWaitEnd = true; // Our only way to stop.
        helper.rewaitIfNormalWaitEndAndDidntStop = possibleSpuriousWaitStop; // In case of spurious wait stop before interruption.
        helper.launchWait(method, executor);

        sleepNs(TOLERANCE_NS);

        if (possibleSpuriousWaitStop) {
            if (helper.isNormalWaitEndEncountered()) {
                // Didn't interrupt yet.
                assertFalse(helper.isInterruptedStatusSetOnLastNormalWaitEnd());
                System.out.println("rare : waiting ended before interruption");
                return;
            }
        } else {
            assertTrue(helper.waitingNotTerminated());
        }

        /*
         * Checking interruption doesn't stop the wait, with soft-check
         * in case of possible spurious wake-up, which we suppose rare,
         * else this test is irrelevant.
         * (Can have thread interrupted just after a spurious wait stop,
         * which would look like having the wait being stopped by interruption.)
         */

        helper.interruptRunnerWhenKnown();

        sleepNs(TOLERANCE_NS);

        assertFalse(helper.isInterruptedExceptionThrown());
        if (possibleSpuriousWaitStop) {
            if (helper.isNormalWaitEndEncountered()) {
                System.out.println("rare : waiting ended after interruption");
                return;
            }
        } else {
            assertTrue(helper.waitingNotTerminated());
            assertFalse(helper.isNormalWaitEndEncountered());
        }

        /*
         * Stopping the wait, and checking interruption status has been restored.
         */

        final long stopTimeNs = nowNs();
        waiter.setBcTrueAndSignalAllInLock();

        assertTrue(Math.abs(helper.getEndTimeNsBlocking() - stopTimeNs) <= TOLERANCE_NS);

        assertFalse(helper.isInterruptedExceptionThrown());
        assertTrue(helper.isInterruptedStatusSetOnLastNormalWaitEnd());

        Unchecked.shutdownAndAwaitTermination(executor);
    }

    /*
     * 
     */

    /**
     * Calls a wait method on a condilock,
     * acquiring the lock before if needed by the method.
     * 
     * @param condilock Condilock on which to call the method.
     * @param method Method to call.
     * @param bc Only used if the method takes a BC as argument.
     * @param timeoutNs Only used if the method takes a time or timeour as argument.
     * @return Value returned by the method, or null if it returns void.
     * @throws InterruptedException if interrupted.
     */
    private static Object call_xxx_InLock(
            final InterfaceCondilock condilock,
            final MyMethod method,
            final InterfaceBooleanCondition bc,
            final long timeoutNs) throws InterruptedException {
        Object ret = null;
        try {
            final boolean mustTakeLock = (!method.waitingForBc);
            if (mustTakeLock) {
                ret = condilock.callInLock(new Callable<Object>() {
                    @Override
                    public Object call() throws InterruptedException {
                        return call_xxx(condilock, method, bc, timeoutNs);
                    }
                });
            } else {
                ret = call_xxx(condilock, method, bc, timeoutNs);
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RethrowException(e);
        }
        return ret;
    }

    /**
     * Calls a wait method on a condilock, without prior lock acquiring.
     * 
     * @param condilock Condilock on which to call the method.
     * @param method Method to call.
     * @param bc Only used if the method takes a BC as argument.
     * @param timeoutNs Either in timeout time or deadline time, depending on the method.
     *        Only used if the method takes a time or timeout as argument.
     * @return Value returned by the method, or null if it returns void.
     * @throws InterruptedException if interrupted.
     */
    private static Object call_xxx(
            InterfaceCondilock condilock,
            MyMethod method,
            InterfaceBooleanCondition bc,
            long timeoutNs) throws InterruptedException {
        switch (method) {
        case AWAIT: {
            condilock.await();
            return null;
        }
        /*
         * 
         */
        case AWAIT_UNINTERRUPTIBLY: {
            condilock.awaitUninterruptibly();
            return null;
        }
        /*
         * 
         */
        case AWAIT_NANOS_long: {
            return condilock.awaitNanos(timeoutNs);
        }
        case AWAIT_long_tu: {
            return condilock.await(timeoutNs, TimeUnit.NANOSECONDS);
        }
        case AWAIT_UNTIL_Date: {
            final long nowNs = condilock.deadlineTimeNs();
            final long endTimeNs = NumbersUtils.plusBounded(nowNs, timeoutNs);
            final Date endDate = newDate(endTimeNs);
            return condilock.awaitUntil(endDate);
        }
        case AWAIT_UNTIL_NANOS_TIMEOUT_TIME_long: {
            final long nowNs = condilock.timeoutTimeNs();
            final long endTimeNs = NumbersUtils.plusBounded(nowNs, timeoutNs);
            condilock.awaitUntilNanosTimeoutTime(endTimeNs);
            return null;
        }
        case AWAIT_UNTIL_NANOS_long: {
            final long nowNs = condilock.deadlineTimeNs();
            final long endTimeNs = NumbersUtils.plusBounded(nowNs, timeoutNs);
            return condilock.awaitUntilNanos(endTimeNs);
        }
        /*
         * 
         */
        case AWAIT_WHILE_FALSE_IN_LOCK_UNINTERRUPTIBLY_bc: {
            condilock.awaitWhileFalseInLockUninterruptibly(bc);
            return null;
        }
        /*
         * 
         */
        case AWAIT_NANOS_WHILE_FALSE_IN_LOCK__bc: {
            return condilock.awaitNanosWhileFalseInLock(bc, timeoutNs);
        }
        case AWAIT_UNTIL_NANOS_TIMEOUT_TIME_WHILE_FALSE_IN_LOCK__bc: {
            final long nowNs = condilock.timeoutTimeNs();
            final long endTimeNs = NumbersUtils.plusBounded(nowNs, timeoutNs);
            return condilock.awaitUntilNanosTimeoutTimeWhileFalseInLock(bc, endTimeNs);
        }
        case AWAIT_UNTLI_NANOS_WHILE_FALSE_IN_LOCK__bc: {
            final long nowNs = condilock.deadlineTimeNs();
            final long endTimeNs = NumbersUtils.plusBounded(nowNs, timeoutNs);
            return condilock.awaitUntilNanosWhileFalseInLock(bc, endTimeNs);
        }
        default:
            throw new AssertionError("" + method);
        }
    }

    /*
     * misc
     */

    private static long nsToMs(long ns) {
        final long ms = ns / (1000L * 1000L);
        return ms;
    }

    private static Date newDate(long timeNs) {
        final long tineMs = nsToMs(timeNs);
        return new Date(tineMs);
    }

    private static void sleepNs(long ns) {
        final long ms = nsToMs(ns);
        Unchecked.sleepMs(ms);
    }

    /**
     * Time for durations measures.
     */
    private static long nowNs() {
        /**
         * To avoid wrapping.
         */
        return LangUtils.getNanoTimeFromClassLoadNs();
    }

    private static void setBcAndSignalAfter(
            final MyWaiter waiter,
            final Executor executor,
            final long ns) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // For signal to happen after wait
                // (useful if wait blocks for a long
                // time or forever).
                sleepNs(ns);
                waiter.setBcTrueAndSignalAllInLock();
            }
        });
    }
}
