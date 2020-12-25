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
package net.jolikit.time.clocks.hard;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.RethrowException;
import net.jolikit.threading.locks.InterfaceCondilock;
import net.jolikit.time.clocks.ClocksUtils;

/**
 * Condilock based on a hard clock.
 * 
 * Durations and times specified or returned by waiting methods,
 * are in the time frame of the clock, and need not to be confused
 * with system time.
 * 
 * Methods awaiting for a timeout to elapse do not take care of clock's time
 * modifications, i.e. don't wait for a time to occur, but for the given amount
 * of clock's time to actually flow. This ensures homogeneity with JDK's
 * conditions implementations, which rely on System.nanoTime().
 * 
 * Await methods do not listen to modification of clock's time or time speed,
 * but the resulting inaccuracy is limited by the maxSystemWaitTimeNs value,
 * which also limits the error in case of system time jump, which is a "quiet"
 * modification (not listenable).
 * 
 * To take care accurately of clock's modifications, you must together
 * retrieve clock time and initiate the wait on a condition, from within
 * the lock of the condition you await on, and this condition must be signaled
 * on clock's modification (clocks modification listeners being notified
 * of the modification AFTER it occurred).
 * Note that this does not prevent inaccuracies due to system time jumps,
 * so the maxSystemWaitTimeNs value remains useful in that case for methods
 * awaiting for a deadline to occur.
 */
public class HardClockCondilock implements InterfaceCondilock {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class AwaitInLockCallable implements Callable<Boolean> {
        private final InterfaceBooleanCondition booleanCondition;
        private final long initialTimeoutNs;
        public AwaitInLockCallable(
                final InterfaceBooleanCondition booleanCondition,
                long timeoutNs) {
            this.booleanCondition = booleanCondition;
            this.initialTimeoutNs = timeoutNs;
        }
        @Override
        public Boolean call() throws InterruptedException {
            long timeoutNs = this.initialTimeoutNs;
            while (!this.booleanCondition.isTrue()) {
                if (timeoutNs <= 0) {
                    return Boolean.FALSE;
                }
                timeoutNs = awaitNanos(timeoutNs);
            }
            return Boolean.TRUE;
        }
    }

    private class AwaitUntilTimeoutTimeInLockCallable implements Callable<Boolean> {
        private final InterfaceBooleanCondition booleanCondition;
        private final long endTimeoutTimeNs;
        public AwaitUntilTimeoutTimeInLockCallable(
                final InterfaceBooleanCondition booleanCondition,
                long endTimeoutTimeNs) {
            this.booleanCondition = booleanCondition;
            this.endTimeoutTimeNs = endTimeoutTimeNs;
        }
        @Override
        public Boolean call() throws InterruptedException {
            while (!this.booleanCondition.isTrue()) {
                if (timeoutTimeNs() >= this.endTimeoutTimeNs) {
                    return Boolean.FALSE;
                }
                awaitUntilNanosTimeoutTime(this.endTimeoutTimeNs);
            }
            return Boolean.TRUE;
        }
    }

    private class AwaitUntilInLockCallable implements Callable<Boolean> {
        private final InterfaceBooleanCondition booleanCondition;
        private final long deadlineNs;
        public AwaitUntilInLockCallable(
                final InterfaceBooleanCondition booleanCondition,
                long deadlineNs) {
            this.booleanCondition = booleanCondition;
            this.deadlineNs = deadlineNs;
        }
        @Override
        public Boolean call() throws InterruptedException {
            boolean moreToWait = true;
            while (!this.booleanCondition.isTrue()) {
                if (!moreToWait) {
                    return Boolean.FALSE;
                }
                moreToWait = awaitUntilNanos(this.deadlineNs);
            }
            return Boolean.TRUE;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final long DEFAULT_MAX_SYSTEM_WAIT_TIME_NS = Long.MAX_VALUE;
    
    private final InterfaceHardClock clock;
    
    private final InterfaceCondilock systemTimeCondilock;
    
    /**
     * Always > 0.
     * Used for waits until a deadline occurs, as can be done in system time based conditions,
     * but also for waits until a timeout elapses, because hard clock's absolute time speed
     * might change without this condition being signaled.
     */
    private volatile long maxSystemWaitTimeNs = DEFAULT_MAX_SYSTEM_WAIT_TIME_NS;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a condilock with a default max system wait time of Long.MAX_VALUE nanosecond.
     * 
     * @param clock Wait durations or deadlines are specified in the time frame of this clock.
     * @param systemTimeCondition The condition used to actually wait.
     */
    public HardClockCondilock(
            final InterfaceHardClock clock,
            final InterfaceCondilock systemTimeCondition) {
        this.clock = clock;
        this.systemTimeCondilock = systemTimeCondition;
    }

    /**
     * To prevent too long waits, in case of time modifications (time, or time speed)
     * that could not be listened at (due to system time jumps, or a clock being non-listenable)
     * but would imply a shortening of the duration to wait.
     * 
     * @param maxSystemWaitTimeNs Max system time to wait, in nanoseconds,
     *        for duration-bound waits. Must be > 0.
     */
    public void setMaxSystemWaitTimeNs(long maxSystemWaitTimeNs) {
        NbrsUtils.requireSup(0, maxSystemWaitTimeNs, "maxSystemWaitTimeNs");
        this.maxSystemWaitTimeNs = maxSystemWaitTimeNs;
    }
    
    /*
     * 
     */

    @Override
    public void runInLock(Runnable runnable) {
        this.systemTimeCondilock.runInLock(runnable);
    }

    @Override
    public <V> V callInLock(Callable<V> callable) throws Exception {
        return this.systemTimeCondilock.callInLock(callable);
    }
    
    /*
     * 
     */
    
    @Override
    public void signal() {
        this.systemTimeCondilock.signal();
    }
    
    @Override
    public void signalAll() {
        this.systemTimeCondilock.signalAll();
    }
    
    /*
     * 
     */

    @Override
    public void signalInLock() {
        this.systemTimeCondilock.signalInLock();
    }
    
    @Override
    public void signalAllInLock() {
        this.systemTimeCondilock.signalAllInLock();
    }
    
    /*
     * 
     */
    
    @Override
    public long timeoutTimeNs() {
        /*
         * TODO We theoretically use the clock as time reference for timeouts,
         * but actually, for pure timeout waits, we only use its time speed,
         * which we use to compute chunks of system time durations to wait.
         * This makes this condilock more similar to regular condilocks,
         * which timeouts are measured using System.nanoTime(), which
         * doesn't jump around as this clock or System.currentTimeMillis()
         * can do.
         * However, it would be better to have a separate clock for timeout time,
         * which would have the same time speed than this condilock's clock,
         * but which time could not be modified (just change as real time flows,
         * without jumps, but at its own speed, which could itself be modified);
         * in particular, it would make awaitUntilNanosTimeoutTimeWhileFalseInLock(...)
         * method more in pair with awaitNanosWhileFalseInLock(...), whereas for now
         * it is rather similar to awaitUntilNanosWhileFalseInLock(...).
         */
        return this.clock.getTimeNs();
    }

    @Override
    public long deadlineTimeNs() {
        return this.clock.getTimeNs();
    }

    /*
     * 
     */
    
    @Override
    public void await() throws InterruptedException {
        this.systemTimeCondilock.await();
    }
    
    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        final long ns = TimeUnit.NANOSECONDS.convert(time, unit);
        return (this.awaitNanos(ns) > 0);
    }
    
    @Override
    public long awaitNanos(long clockWaitTimeNs) throws InterruptedException {
        final double absoluteTimeSpeed = ClocksUtils.computeAbsoluteTimeSpeed(this.clock);
        
        final long remainingSystemWaitTimeNs = this.awaitNanos_internal(clockWaitTimeNs, absoluteTimeSpeed);

        final long remainingClockWaitTimeNs =
                ClocksUtils.computeDtNsMulTimeSpeed(
                        remainingSystemWaitTimeNs,
                        absoluteTimeSpeed);
        return remainingClockWaitTimeNs;
    }
    
    @Override
    public void awaitUntilNanosTimeoutTime(long endTimeoutTimeNs) throws InterruptedException {
        final double absoluteTimeSpeed = ClocksUtils.computeAbsoluteTimeSpeed(this.clock);
        // timeout time = deadline time
        this.awaitUntilNanos_internal(endTimeoutTimeNs, absoluteTimeSpeed);
    }

    @Override
    public void awaitUninterruptibly() {
        this.systemTimeCondilock.awaitUninterruptibly();
    }
    
    @Override
    public boolean awaitUntil(Date deadline) throws InterruptedException {
        return this.awaitUntilNanos(TimeUnit.MILLISECONDS.toNanos(deadline.getTime()));
    }

    @Override
    public boolean awaitUntilNanos(long deadlineNs) throws InterruptedException {
        /*
         * Not using awaitUntil here, because:
         * - it usually relies on System.currentTimeMillis(), which might not be accurate enough,
         * - in case of system time jump, and if system time condition does not limit its wait durations,
         *   it could wait for a very long time.
         */
        
        final double absoluteTimeSpeed = ClocksUtils.computeAbsoluteTimeSpeed(this.clock);
        
        this.awaitUntilNanos_internal(deadlineNs, absoluteTimeSpeed);
        
        final long afterNs = this.deadlineTimeNs();
        return (afterNs < deadlineNs);
    }

    /*
     * 
     */

    @Override
    public void awaitWhileFalseInLockUninterruptibly(InterfaceBooleanCondition booleanCondition) {
        this.systemTimeCondilock.awaitWhileFalseInLockUninterruptibly(booleanCondition);
    }

    @Override
    public boolean awaitNanosWhileFalseInLock(
            final InterfaceBooleanCondition booleanCondition,
            long timeoutNs) throws InterruptedException {
        if (booleanCondition.isTrue()) {
            return true;
        }

        if (timeoutNs <= 0) {
            return false;
        }
        
        // This object creation should not hurt much, since we pass
        // here only when we are likely to actually wait.
        Callable<Boolean> calledInLock = new AwaitInLockCallable(booleanCondition, timeoutNs);
        return this.callBooleanConditionWaiterInLock(calledInLock);
    }
    
    @Override
    public boolean awaitUntilNanosTimeoutTimeWhileFalseInLock(
            final InterfaceBooleanCondition booleanCondition,
            long endTimeoutTimeNs) throws InterruptedException {
        if (booleanCondition.isTrue()) {
            return true;
        }

        if (this.timeoutTimeNs() >= endTimeoutTimeNs) {
            return false;
        }

        // This object creation should not hurt much, since we pass
        // here only when we are likely to actually wait.
        Callable<Boolean> calledInLock = new AwaitUntilTimeoutTimeInLockCallable(booleanCondition, endTimeoutTimeNs);
        return this.callBooleanConditionWaiterInLock(calledInLock);
    }

    @Override
    public boolean awaitUntilNanosWhileFalseInLock(
            final InterfaceBooleanCondition booleanCondition,
            long deadlineNs) throws InterruptedException {
        if (booleanCondition.isTrue()) {
            return true;
        }

        if (this.deadlineTimeNs() >= deadlineNs) {
            return false;
        }
        
        // This object creation should not hurt much, since we pass
        // here only when we are likely to actually wait.
        Callable<Boolean> calledInLock = new AwaitUntilInLockCallable(booleanCondition, deadlineNs);
        return this.callBooleanConditionWaiterInLock(calledInLock);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Remaining system wait time, in nanoseconds.
     */
    private long awaitNanos_internal(long clockWaitTimeNs, double absoluteTimeSpeed) throws InterruptedException {
        final long systemWaitTimeNs = ClocksUtils.computeSystemWaitTimeNs(clockWaitTimeNs, absoluteTimeSpeed);
        final long remainingSystemWaitTimeNs;
        if (systemWaitTimeNs <= 0) {
            // no need to wait
            remainingSystemWaitTimeNs = 0L;
        } else {
            final long actualSystemWaitTimeNs = Math.min(systemWaitTimeNs, this.maxSystemWaitTimeNs);
            remainingSystemWaitTimeNs = this.systemTimeCondilock.awaitNanos(actualSystemWaitTimeNs);
        }
        return remainingSystemWaitTimeNs;
    }

    /**
     * @return Remaining system wait time, in nanoseconds.
     */
    private long awaitUntilNanos_internal(long deadlineNs, double absoluteTimeSpeed) throws InterruptedException {
        final long beforeNs = this.deadlineTimeNs();
        final long clockWaitTimeNs = NbrsUtils.minusBounded(deadlineNs, beforeNs);
        return this.awaitNanos_internal(clockWaitTimeNs, absoluteTimeSpeed);
    }

    private boolean callBooleanConditionWaiterInLock(Callable<Boolean> calledInLock) throws InterruptedException {
        try {
            return this.systemTimeCondilock.callInLock(calledInLock).booleanValue();
        } catch (RuntimeException e) {
            // Letting RuntimeException go up.
            throw e;
        } catch (InterruptedException e) {
            // Letting InterruptedException go up.
            throw e;
        } catch (Exception e) {
            throw new RethrowException(e);
        }
    }
}
