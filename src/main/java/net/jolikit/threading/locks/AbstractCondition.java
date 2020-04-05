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

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class for conditions, which allows for the definition of a
 * max wait time, for deadline waits not to become aware too late of
 * eventual system time jumps.
 * 
 * Max wait time configuration is retrieved from an overridable method,
 * to allow for memory-cheap static configuration.
 */
abstract class AbstractCondition extends AbstractCondition0 {
    
    /*
     * Most wait methods declared final, which allows for use of static
     * implementations without fear of having to call an instance method
     * that might have been overridden.
     */
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    @Override
    public final void await() throws InterruptedException {
        ConditionsUtilz.mainWait_TT_forever_orChunk(
                this, ConditionsUtilz.NO_ELAPSED_TIMEOUT_TIME_NS);
    }

    @Override
    public final void awaitUninterruptibly() {
        // Preferring to have an exception in case of interrupt,
        // than checking interrupt status at start all the time
        // (which is done by await() already).
        boolean interrupted = false;
        while (true) {
            try {
                ConditionsUtilz.mainWait_TT_forever_orChunk(
                        this, ConditionsUtilz.NO_ELAPSED_TIMEOUT_TIME_NS);
                // signaled or spurious wake-up
                // (or short wait due to getMaxBlockingWaitChunkNs(long))
                break;
            } catch (InterruptedException e) {
                // Not restoring interrupt status here,
                // else would keep spinning on InterruptedException
                // being thrown.
                interrupted = true;
            }
        }
        if (interrupted) {
            // Restoring interrupt status.
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * @return An estimation of remaining timeout, or the specified
     *         timeout if it was approximated as infinite.
     * @throws InterruptedException if current thread is interrupted.
     */
    @Override
    public final long awaitNanos(long timeoutNs) throws InterruptedException {
        final boolean mustComputeTimeToWaitFor =
                ConditionsUtilz.isTimeoutNotHuge_and_areMainWaitsTimed(
                        this, timeoutNs);
        if (mustComputeTimeToWaitFor) {
            // No problem if wraps, since it should wrap back
            // when we remove current time.
            final long endTimeoutTimeNs = this.timeoutTimeNs() + timeoutNs;
            ConditionsUtilz.mainWait_TT_timeout_orChunk(
                    this, timeoutNs, ConditionsUtilz.NO_ELAPSED_TIMEOUT_TIME_NS);
            return endTimeoutTimeNs - this.timeoutTimeNs();
        } else {
            ConditionsUtilz.mainWait_TT_forever_orChunk(
                    this, ConditionsUtilz.NO_ELAPSED_TIMEOUT_TIME_NS);
            return timeoutNs;
        }
    }

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    @Override
    public final boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        final long timeoutNs = TimeUnit.NANOSECONDS.convert(timeout, unit);
        return (this.awaitNanos(timeoutNs) > 0);
    }

    /**
     * @throws InterruptedException if current thread is interrupted.
     */
    @Override
    public final boolean awaitUntil(Date deadline) throws InterruptedException {
        final long deadlineNs = TimeUnit.MILLISECONDS.toNanos(deadline.getTime());
        return ConditionsUtilz.mainWait_DT_until_orChunk(this, deadlineNs);
    }
}
