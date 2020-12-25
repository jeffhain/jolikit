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
package net.jolikit.threading.locks;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * Utilities to implement condilocks.
 * 
 * Using AbstractCondition in signatures, to avoid a cyclic dependency.
 */
class CondilocksUtilz {

    /*
     * Operations ordering:
     * 1) boolean condition check
     * 2) time check
     * 3) interrupt status check
     * 4) waiting
     * 5) goto 1
     * This means that we are optimistic: if a boolean condition becomes true
     * while we were waiting, we consider it did before the timeout elapsed.
     */

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static Lock newDefaultLock() {
        return new ReentrantLock();
    }
    
    /*
     * 
     */

    /**
     * Useful to know if it's worth to compute remaining time to wait for,
     * when waiting for a boolean condition to be true.
     * 
     * @param condilock A condilock.
     * @param timeoutNs A timeout, in nanoseconds.
     * @return True either if the specified timeout is not huge,
     *         or if getMustUseMaxBlockingWaitChunks() is true.
     */
    public static boolean isTimeoutNotHuge_or_mustWaitByChunks(
            AbstractCondition condilock,
            long timeoutNs) {
        // Need to use timing if using max blocking wait chunks (typically with timed waits),
        // else argument for getMaxBlockingWaitChunkNs(long) will always be 0.
        return ConditionsUtilz.isTimeoutNotHuge(timeoutNs)
                || condilock.getMustUseMaxBlockingWaitChunks();
    }

    /**
     * Useful to know if it's worth to compute remaining time to wait for,
     * when waiting for a boolean condition to be true.
     * 
     * @param condilock A condilock.
     * @param endTimeNs An end time, in nanoseconds.
     * @return True either if the specified end time is not huge,
     *         or if getMustUseMaxBlockingWaitChunks() is true.
     */
    public static boolean isEndTimeNotHuge_or_mustWaitByChunks(
            AbstractCondition condilock,
            long endTimeNs) {
        // Need to use timing if using max blocking wait chunks (typically with timed waits),
        // else argument for getMaxBlockingWaitChunkNs(long) will always be 0.
        return ConditionsUtilz.isEndTimeNotHuge(endTimeNs)
                || condilock.getMustUseMaxBlockingWaitChunks();
    }

    /*
     * Waiting implementation.
     */
    
    /**
     * Doesn't wait.
     * 
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void nothingWait() throws InterruptedException {
        LangUtils.throwIfInterrupted();
    }

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void yieldingWait() throws InterruptedException {
        Thread.yield();
        LangUtils.throwIfInterrupted();
    }

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void sleepingWait() throws InterruptedException {
        // Using parking wait over Thread.sleep(long), for its
        // (possibly) smaller granularity.
        //
        // Parking wait can be messed-up by someone calling
        // LockSupport.unpark(our_current_thread) frequently,
        // but we consider that should not happen or not hurt.
        //
        // In case parkNanos would be very accurate,
        // we make sure not to wait too small of a time,
        // else this would rather be a busy wait.
        LockSupport.parkNanos(100L * 1000L);
        LangUtils.throwIfInterrupted();
    }

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void mutexWaitNs(
            Object mutex,
            long timeoutNs) throws InterruptedException {
        // No need to test if timeout is eternal: this method already
        // handles the case of huge timeouts.
        LangUtils.waitNs(mutex, timeoutNs);
    }

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void lockConditionWaitNs(
            Condition condition,
            long timeoutNs) throws InterruptedException {
        final boolean mustComputeTimeToWaitFor =
                ConditionsUtilz.isTimeoutNotHuge(timeoutNs);
        if (mustComputeTimeToWaitFor) {
            // This is the only place where backing's condition timing
            // is used. Note that we always use our own timing above it.
            @SuppressWarnings("unused")
            long unused = condition.awaitNanos(timeoutNs);
        } else {
            condition.await();
        }
    }

    /*
     * When boolean condition is initially false.
     */
    
    /**
     * @return True if timeout elapsed.
     */
    public static boolean whenInitiallyFalse_TT_timeout(long timeoutNs) throws InterruptedException {
        
        LangUtils.throwIfInterrupted();
        
        if (timeoutNs <= 0) {
            return true;
        }
        
        return false;
    }

    /**
     * @return True if end timeout time was reached.
     */
    public static boolean whenInitiallyFalse_TT_until(
            AbstractCondition condilock,
            long endTimeoutTimeNs) throws InterruptedException {
        
        LangUtils.throwIfInterrupted();
        
        if (ConditionsUtilz.isEndTimePassed_TT(condilock, endTimeoutTimeNs)) {
            return true;
        }
        
        return false;
    }

    /**
     * @return True if deadline was reached.
     */
    public static boolean whenInitiallyFalse_DT_until(
            AbstractCondition condilock,
            long deadlineNs) throws InterruptedException {
        
        LangUtils.throwIfInterrupted();
        
        if (ConditionsUtilz.isEndTimePassed_DT(condilock, deadlineNs)) {
            return true;
        }
        
        return false;
    }
    
    /*
     * 
     */
    
    /**
     * For use after acquiring the lock required for waiting.
     * Can be optimistic, and return true (boolean condition true
     * before timeout elapsed), as if boolean condition turned true
     * just before trying to acquire the lock.
     * 
     * @return True if boolean condition is true, false otherwise.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static boolean afterLock(InterfaceBooleanCondition booleanCondition) throws InterruptedException {
        LangUtils.throwIfInterrupted();
        return booleanCondition.isTrue();
    }

    /*
     * Main : timeout time : timeout.
     */

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    public static boolean mainWaitWhileFalse_TT_timeout(
            AbstractCondition condilock,
            InterfaceBooleanCondition booleanCondition,
            long timeoutNs) throws InterruptedException {
        
        final boolean mustComputeRemainingTimeToWaitFor =
                isTimeoutNotHuge_or_mustWaitByChunks(condilock, timeoutNs);
        if (mustComputeRemainingTimeToWaitFor) {
            /*
             * Using method based on end timeout time, for it is more accurate
             * than accumulating error with intermediate timeouts computations.
             */
            final long currentTimeoutTimeNs = condilock.timeoutTimeNs();
            final long endTimeoutTimeNs = NbrsUtils.plusBounded(currentTimeoutTimeNs, timeoutNs);
            return mainWaitWhileFalse_TT_until_orChunk_withCurrentTime(
                    condilock,
                    booleanCondition,
                    endTimeoutTimeNs,
                    currentTimeoutTimeNs);
        } else {
            return mainWaitWhileFalse_forever_noChunks(
                    condilock,
                    booleanCondition);
        }
    }

    /*
     * Main : timeout time : until.
     */

    /**
     * Boolean condition is evaluated once before any timing consideration.
     * 
     * @throws InterruptedException if current thread is interrupted.
     */
    public static boolean mainWaitWhileFalse_TT_until(
            AbstractCondition condilock,
            InterfaceBooleanCondition booleanCondition,
            long endTimeoutTimeNs) throws InterruptedException {
        
        final boolean mustComputeRemainingTimeToWaitFor =
                isEndTimeNotHuge_or_mustWaitByChunks(condilock, endTimeoutTimeNs);
        if (mustComputeRemainingTimeToWaitFor) {
            final long currentTimeoutTimeNs = condilock.timeoutTimeNs();
            return mainWaitWhileFalse_TT_until_orChunk_withCurrentTime(
                    condilock,
                    booleanCondition,
                    endTimeoutTimeNs,
                    currentTimeoutTimeNs);
        } else {
            return mainWaitWhileFalse_forever_noChunks(
                    condilock,
                    booleanCondition);
        }
    }
    
    /*
     * Timeout time : until : with current time computed.
     */

    /**
     * @param endTimeoutTimeNs Timeout time to wait for, in nanoseconds.
     * @param currentTimeoutTimeNs A recent result of timeoutTimeNs() method.
     * @param elapsedTimeNs Argument for getMaxBlockingWaitChunkNs(long) method.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void mainWait_TT_until_orChunk_withCurrentTime(
            AbstractCondition condilock,
            long endTimeoutTimeNs,
            long currentTimeoutTimeNs,
            long elapsedTimeNs) throws InterruptedException {
        final long timeoutNs = NbrsUtils.minusBounded(
                endTimeoutTimeNs,
                currentTimeoutTimeNs);
        ConditionsUtilz.mainWait_TT_timeout_orChunk(
                condilock, timeoutNs, elapsedTimeNs);
    }

    /*
     * Main : deadline time : until.
     */

    /**
     * Boolean condition is evaluated once before any timing consideration.
     * 
     * @throws InterruptedException if current thread is interrupted.
     */
    public static boolean mainWaitWhileFalse_DT_until(
            AbstractCondition condilock,
            InterfaceBooleanCondition booleanCondition,
            long deadlineNs) throws InterruptedException {
        
        final boolean mustComputeRemainingTimeToWaitFor =
                isEndTimeNotHuge_or_mustWaitByChunks(condilock, deadlineNs);
        if (mustComputeRemainingTimeToWaitFor) {
            final long deadlineTimeNs = condilock.deadlineTimeNs();
            return mainWaitWhileFalse_DT_until_orChunk_withCurrentTime(
                    condilock,
                    booleanCondition,
                    deadlineNs,
                    deadlineTimeNs);
        } else {
            return mainWaitWhileFalse_forever_noChunks(
                    condilock,
                    booleanCondition);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private CondilocksUtilz() {
    }
    
    /*
     * Main : timeout time : until : with current time computed.
     */
    
    /**
     * @param currentTimeoutTimeNs A recent result of timeoutTimeNs() method.
     * @throws InterruptedException if current thread is interrupted.
     */
    private static boolean mainWaitWhileFalse_TT_until_orChunk_withCurrentTime(
            AbstractCondition condilock,
            InterfaceBooleanCondition booleanCondition,
            long endTimeoutTimeNs,
            long currentTimeoutTimeNs) throws InterruptedException {
        
        LangUtils.throwIfInterrupted();
        
        final long initialTimeoutTimeNs = currentTimeoutTimeNs;
        while (true) {
            if (currentTimeoutTimeNs >= endTimeoutTimeNs) {
                return false;
            }
            
            final long elapsedTimeoutTimeNs = Math.max(0L, currentTimeoutTimeNs - initialTimeoutTimeNs);
            mainWait_TT_until_orChunk_withCurrentTime(
                    condilock,
                    endTimeoutTimeNs,
                    currentTimeoutTimeNs,
                    elapsedTimeoutTimeNs);
            
            if (booleanCondition.isTrue()) {
                return true;
            }
            
            currentTimeoutTimeNs = condilock.timeoutTimeNs();
        }
    }
    
    /*
     * Main : deadline time : until : with current time computed.
     */
    
    /**
     * @param currentDeadlineTimeNs A recent result of deadlineTimeNs() method.
     * @throws InterruptedException if current thread is interrupted.
     */
    private static boolean mainWaitWhileFalse_DT_until_orChunk_withCurrentTime(
            AbstractCondition condilock,
            InterfaceBooleanCondition booleanCondition,
            long deadlineNs,
            long currentDeadlineTimeNs) throws InterruptedException {

        LangUtils.throwIfInterrupted();

        final long initialDeadlineTimeNs = currentDeadlineTimeNs;
        while (true) {
            if (currentDeadlineTimeNs >= deadlineNs) {
                return false;
            }

            final long elapsedDeadlineTimeNs = Math.max(0L, currentDeadlineTimeNs - initialDeadlineTimeNs);
            mainWait_DT_until_orChunk_withCurrentTime(
                    condilock,
                    deadlineNs,
                    currentDeadlineTimeNs,
                    elapsedDeadlineTimeNs);

            if (booleanCondition.isTrue()) {
                return true;
            }

            currentDeadlineTimeNs = condilock.deadlineTimeNs();
        }
    }

    /**
     * @param deadlineNs Deadline time to wait for, in nanoseconds.
     * @param currentDeadlineTimeNs A recent result of deadlineTimeNs() method.
     * @param elapsedTimeNs Argument for getMaxBlockingWaitChunkNs(long) method.
     * @throws InterruptedException if current thread is interrupted.
     */
    private static void mainWait_DT_until_orChunk_withCurrentTime(
            AbstractCondition condilock,
            long deadlineNs,
            long currentDeadlineTimeNs,
            long elapsedTimeNs) throws InterruptedException {
        /*
         * Switching to timeout time.
         */
        if (condilock.areMainWaitsTimed()) {
            final long timeoutNs = NbrsUtils.minusBounded(
                    deadlineNs,
                    currentDeadlineTimeNs);
            final long reducedTimeoutNs = Math.min(
                    timeoutNs,
                    condilock.getMaxDeadlineBlockingWaitChunkNs());
            ConditionsUtilz.mainWait_TT_timeout_orChunk(
                    condilock, reducedTimeoutNs, elapsedTimeNs);
        } else {
            condilock.mainWait_TT_timeout_upTo(Long.MAX_VALUE);
        }
    }

    /*
     * Main : forever : no max wait chunks.
     */
    
    /**
     * Must not be used if main waits use duration and
     * getMustUseMaxBlockingWaitChunks() returns true,
     * for it does not make use of getMaxBlockingWaitChunkNs(long).
     * 
     * @throws InterruptedException if current thread is interrupted.
     */
    private static boolean mainWaitWhileFalse_forever_noChunks(
            AbstractCondition condilock,
            InterfaceBooleanCondition booleanCondition) throws InterruptedException {
        while (true) {
            condilock.mainWait_TT_timeout_upTo(Long.MAX_VALUE);
            
            if (booleanCondition.isTrue()) {
                return true;
            }
        }
    }
}
