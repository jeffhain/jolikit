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

import net.jolikit.lang.InterfaceBooleanCondition;

/**
 * Abstract class for condilocks.
 */
abstract class AbstractCondilock extends AbstractCondition implements InterfaceCondilock {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    @Override
    public final void awaitUntilNanosTimeoutTime(long endTimeoutTimeNs) throws InterruptedException {
        final boolean mustComputeTimeToWaitFor =
                ConditionsUtilz.isEndTimeNotHuge_and_areMainWaitsTimed(
                        this, endTimeoutTimeNs);
        if (mustComputeTimeToWaitFor) {
            CondilocksUtilz.mainWait_TT_until_orChunk_withCurrentTime(
                    this, endTimeoutTimeNs, this.timeoutTimeNs(), ConditionsUtilz.NO_ELAPSED_TIMEOUT_TIME_NS);
        } else {
            ConditionsUtilz.mainWait_TT_forever_orChunk(
                    this, ConditionsUtilz.NO_ELAPSED_TIMEOUT_TIME_NS);
        }
    }

    @Override
    public final boolean awaitUntilNanos(long deadlineNs) throws InterruptedException {
        return ConditionsUtilz.mainWait_DT_until_orChunk(this, deadlineNs);
    }

    @Override
    public final void awaitWhileFalseInLockUninterruptibly(InterfaceBooleanCondition booleanCondition) {
        /*
         * Similar to awaitUninterruptibly(), but waiting for a boolean condition.
         */
        boolean interrupted = false;
        while (true) {
            try {
                if (this.awaitUntilNanosTimeoutTimeWhileFalseInLock(booleanCondition, Long.MAX_VALUE)) {
                    break;
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
