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
package net.jolikit.time.sched;

import net.jolikit.lang.NbrsUtils;
import net.jolikit.time.TimeUtils;

/**
 * Optional class that makes it easier to implement schedulers.
 */
public abstract class AbstractScheduler implements InterfaceScheduler {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractScheduler() {
    }
    
    @Override
    public void executeAfterNs(
            Runnable runnable,
            long delayNs) {
        final long nowNs = this.getClock().getTimeNs();
        // Bounded not to throw if user specifies a huge delay,
        // in which case it's fine to have the time wrong
        // since the execution should never occur in practice.
        final long timeNs = NbrsUtils.plusBounded(nowNs, delayNs);
        this.executeAtNs(runnable, timeNs);
    }

    @Override
    public void executeAtS(
            Runnable runnable,
            double timeS) {
        final long timeNs = TimeUtils.sToNs(timeS);
        this.executeAtNs(runnable, timeNs);
    }

    @Override
    public void executeAfterS(
            Runnable runnable,
            double delayS) {
        /*
         * Switching to long before adding delay, to avoid
         * the precision loss of using current time as double
         * for large time values (internal reference time should
         * be a long), and for NaN argument to count as zero
         * as for executeAtS(...) implementation.
         * 
         * Not using executeAfterNs(...), not to pollute the call stack
         * with too many avoidable calls.
         */
        final long delayNs = TimeUtils.sToNs(delayS);
        final long nowNs = this.getClock().getTimeNs();
        final long timeNs = NbrsUtils.plusBounded(nowNs, delayNs);
        this.executeAtNs(runnable, timeNs);
    }
}
