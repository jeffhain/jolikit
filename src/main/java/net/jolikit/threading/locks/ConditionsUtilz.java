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

import net.jolikit.lang.NbrsUtils;

/**
 * Utilities to implement conditions.
 * 
 * Using AbstractCondition0 in signatures, to avoid a cyclic dependency.
 */
class ConditionsUtilz {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Argument for first call to getMaxBlockingWaitChunkNs(long).
     */
    static final long NO_ELAPSED_TIMEOUT_TIME_NS = 0L;
    
    /**
     * Threshold (inclusive), in nanoseconds, for a timeout to be considered infinite.
     * 
     * Not just using Long.MAX_VALUE due to some wait methods eventually
     * decreasing remaining timeout for other wait methods.
     */
    private static final long INFINITE_TIMEOUT_THRESHOLD_NS = Long.MAX_VALUE/2;
    
    /**
     * Threshold (inclusive), in nanoseconds, for a date to be considered infinite.
     */
    private static final long INFINITE_DATE_THRESHOLD_NS = Long.MAX_VALUE;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param timeoutNs A timeout, in nanoseconds.
     * @return True if the specified timeout is small enough to be worth timing.
     */
    public static boolean isTimeoutNotHuge(long timeoutNs) {
        return (timeoutNs < INFINITE_TIMEOUT_THRESHOLD_NS);
    }

    /**
     * @param endTimeNs An end time, in nanoseconds.
     * @return True if the specified end time is small enough to be worth timing.
     */
    public static boolean isEndTimeNotHuge(long endTimeNs) {
        return (endTimeNs < INFINITE_DATE_THRESHOLD_NS);
    }

    
    /**
     * Useful to know if it's worth to compute time to wait for,
     * when waiting for a timeout.
     * 
     * @param timeoutNs A timeout, in nanoseconds.
     * @return True if the specified timeout is not huge
     *         and areMainWaitsTimed() is true.
     */
    public static boolean isTimeoutNotHuge_and_areMainWaitsTimed(
            AbstractCondition0 condition,
            long timeoutNs) {
        return ConditionsUtilz.isTimeoutNotHuge(timeoutNs)
                && condition.areMainWaitsTimed();
    }
    
    /**
     * Useful to know if it's worth to compute time to wait for,
     * when waiting for an end time.
     * 
     * @param endTimeNs An end time, in nanoseconds.
     * @return True if the specified end time is not huge
     *         and areMainWaitsTimed() is true.
     */
    public static boolean isEndTimeNotHuge_and_areMainWaitsTimed(
            AbstractCondition0 condition,
            long endTimeNs) {
        return ConditionsUtilz.isEndTimeNotHuge(endTimeNs)
                && condition.areMainWaitsTimed();
    }
    
    /*
     * 
     */

    /**
     * Useful not to bother calling timeoutTimeNs() when timeout time is huge.
     */
    public static boolean isEndTimePassed_TT(
            AbstractCondition0 condition,
            long timeoutTimeNs) {
        if (ConditionsUtilz.isEndTimeNotHuge(timeoutTimeNs)) {
            return (condition.timeoutTimeNs() > timeoutTimeNs);
        } else {
            return false;
        }
    }

    /**
     * Useful not to bother calling deadlineTimeNs() when deadline time is huge.
     */
    public static boolean isEndTimePassed_DT(
            AbstractCondition0 condition,
            long deadlineTimeNs) {
        if (ConditionsUtilz.isEndTimeNotHuge(deadlineTimeNs)) {
            return (condition.deadlineTimeNs() > deadlineTimeNs);
        } else {
            return false;
        }
    }

    /*
     * Timeout time : timeout.
     */
    
    /**
     * @param timeoutNs Recent computation of remaining timeout.
     * @param elapsedTimeNs Argument for getMaxBlockingWaitChunkNs(long) method.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void mainWait_TT_timeout_orChunk(
            AbstractCondition0 condition,
            long timeoutNs,
            long elapsedTimeNs) throws InterruptedException {
        if (condition.getMustUseMaxBlockingWaitChunks()) {
            final long reducedTimeoutNs = Math.min(
                    timeoutNs,
                    condition.getMaxBlockingWaitChunkNs(elapsedTimeNs));
            condition.mainWait_TT_timeout_upTo(reducedTimeoutNs);
        } else {
            condition.mainWait_TT_timeout_upTo(timeoutNs);
        }
    }

    /*
     * Timeout time : forever.
     */
    
    /**
     * @param elapsedTimeNs Argument for getMaxBlockingWaitChunkNs(long) method.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void mainWait_TT_forever_orChunk(
            AbstractCondition0 condition,
            long elapsedTimeNs) throws InterruptedException {
        if (condition.getMustUseMaxBlockingWaitChunks()) {
            final long timeoutNs = condition.getMaxBlockingWaitChunkNs(elapsedTimeNs);
            condition.mainWait_TT_timeout_upTo(timeoutNs);
        } else {
            condition.mainWait_TT_timeout_upTo(Long.MAX_VALUE);
        }
    }

    /*
     * Deadline time : until.
     */
    
    /**
     * @return True if this method returned before the specified deadline could be reached,
     *         false otherwise.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static boolean mainWait_DT_until_orChunk(
            AbstractCondition0 condition,
            long deadlineNs) throws InterruptedException {
        /*
         * Switching to timeout time.
         */
        final boolean mustComputeTimeToWaitFor =
                isEndTimeNotHuge_and_areMainWaitsTimed(condition, deadlineNs);
        final long timeoutNs;
        if (mustComputeTimeToWaitFor) {
            final long deltaNs = NbrsUtils.minusBounded(
                    deadlineNs,
                    condition.deadlineTimeNs());
            timeoutNs = Math.min(
                    deltaNs,
                    condition.getMaxDeadlineBlockingWaitChunkNs());
        } else {
            timeoutNs = condition.getMaxDeadlineBlockingWaitChunkNs();
        }
        
        mainWait_TT_timeout_orChunk(
                condition, timeoutNs, ConditionsUtilz.NO_ELAPSED_TIMEOUT_TIME_NS);
        
        return isEndTimePassed_DT(condition, deadlineNs);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private ConditionsUtilz() {
    }
}
