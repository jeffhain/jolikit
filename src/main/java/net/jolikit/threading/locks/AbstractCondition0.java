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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import net.jolikit.lang.LangUtils;

/**
 * Super class for AbstractCondition, for static utility methods to work on it,
 * without cyclic dependency with AbstractCondition.
 */
abstract class AbstractCondition0 implements Condition {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Time, in nanoseconds, used for timeout measurements.
     */
    public long timeoutTimeNs() {
        /*
         * Using System.nanoTime() here, over System.currentTimeMillis():
         * - advantages:
         *   - (mandatory, for homogeneity with JDK's conditions)
         *     it doesn't take care of system time jumps,
         *   - more precise, and more accurate for small durations,
         * - disadvantages:
         *   - slower (more or less) on some systems,
         *   - has a drift (more or less) on some systems, making
         *     estimation of remaining wait time inaccurate for long timeouts.
         */
        return LangUtils.getNanoTimeFromClassLoadNs();
    }
    
    /**
     * @return Time, in nanoseconds, used for deadlines measurements.
     */
    public long deadlineTimeNs() {
        /*
         * Not using ThinTime.
         * If using ThinTime, would need to use an instance specific to this
         * condition, to avoid contention of all conditions on a same instance.
         * If using ThinTime, could in some places rely on deadlineTimeNs()
         * where we currently rely on timeoutTimeNs() for its accuracy.
         */
        return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Useful not to uselessly compute wait durations.
     * 
     * Main wait type is the one used by methods that only wait for
     * a duration, not for a boolean condition.
     * For waiting for a boolean condition, condilocks might use multi-tiered
     * wait strategies (busy spin, then yielding, etc.),
     * with main wait strategy as last tier.
     * 
     * @return True if main waits takes wait duration into account,
     *         false if they're only short waits that ignore specified
     *         wait durations.
     */
    protected abstract boolean areMainWaitsTimed();
    
    /**
     * Method for timeout waits that don't need estimation of remaining wait time.
     * 
     * Should never use it directly, but always through xxx_orChunk methods,
     * which take care of eventual max wait time chunks.
     * 
     * @param timeoutNs Max duration to wait for, in nanoseconds, in timeout time.
     * @throws InterruptedException if current thread is interrupted.
     */
    protected abstract void mainWait_TT_timeout_upTo(long timeoutNs) throws InterruptedException;

    /*
     * Configuration.
     */
    
    /**
     * Default implementation returns false.
     * 
     * You need to override this method so that it returns true
     * to enable use of getMaxBlockingWaitChunkNs(long).
     * 
     * If areMainWaitsTimed() returns false, bothering to cut waits in chunks
     * makes no sense, so in this case this method should return false as well.
     * 
     * @return True if must use getMaxBlockingWaitChunkNs(long)
     *         for blocking waits, false otherwise, i.e. if do
     *         not need to wait for smaller chunks than specified
     *         timeouts.
     */
    protected boolean getMustUseMaxBlockingWaitChunks() {
        return false;
    }
    
    /**
     * Default implementation returns Long.MAX_VALUE.
     * 
     * You need to override getMustUseMaxBlockingWaitChunks() so that
     * it returns true to enable use of this method.
     * 
     * Useful for when time might jump, or for blocking waits
     * that might not be signaled, or not be able to observe
     * boolean condition new value right away
     * (such as when using lazySet(...)).
     * 
     * @param elapsedTimeNs Duration (>=0), in nanoseconds, elapsed since
     *        blocking wait loop started.
     *        Can be used to enlarge wait chunk as this number grows,
     *        typically because the more time has been waited, the
     *        less a wait stop is likely to have been missed (which
     *        might not be the case though, if all waiters are signaled
     *        but only one can actually stop waiting each time).
     * @return Max duration, in nanoseconds, for next blocking wait
     *         (whether waiting for a timeout or a deadline).
     */
    protected long getMaxBlockingWaitChunkNs(long elapsedTimeNs) {
        return Long.MAX_VALUE;
    }
    
    /**
     * Default implementation returns Long.MAX_VALUE.
     * 
     * Useful not to become aware too late of eventual
     * system time jumps (is used in addition to getMaxBlockingWaitChunkNs(long)).
     * 
     * @return Max duration, in nanoseconds, for each blocking wait for a deadline.
     */
    protected long getMaxDeadlineBlockingWaitChunkNs() {
        return Long.MAX_VALUE;
    }
}
