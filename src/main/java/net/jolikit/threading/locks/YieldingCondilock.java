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

import java.util.concurrent.Callable;

import net.jolikit.lang.InterfaceBooleanCondition;

/**
 * Condilock that has no lock, and always yield-spins
 * while it waits for a boolean condition to be true.
 */
public class YieldingCondilock extends AbstractCondilock {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public YieldingCondilock() {
    }

    /*
     * 
     */

    @Override
    public void runInLock(Runnable runnable) {
        runnable.run();
    }

    @Override
    public <V> V callInLock(Callable<V> callable) throws Exception {
        return callable.call();
    }

    /*
     * 
     */
    
    @Override
    public void signal() {
        // Nothing to signal.
    }
    
    @Override
    public void signalAll() {
        // Nothing to signal.
    }

    /*
     * 
     */

    @Override
    public void signalInLock() {
        // Nothing to signal.
    }
    
    @Override
    public void signalAllInLock() {
        // Nothing to signal.
    }
    
    /*
     * 
     */
    
    @Override
    public boolean awaitNanosWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long timeoutNs) throws InterruptedException {
        if (booleanCondition.isTrue()) {
            return true;
        }

        if (CondilocksUtilz.whenInitiallyFalse_TT_timeout(timeoutNs)) {
            return false;
        }

        return CondilocksUtilz.mainWaitWhileFalse_TT_timeout(
                this, booleanCondition, timeoutNs);
    }

    @Override
    public boolean awaitUntilNanosTimeoutTimeWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long endTimeoutTimeNs)
            throws InterruptedException {
        if (booleanCondition.isTrue()) {
            return true;
        }

        if (CondilocksUtilz.whenInitiallyFalse_TT_until(this, endTimeoutTimeNs)) {
            return false;
        }

        return CondilocksUtilz.mainWaitWhileFalse_TT_until(
                this, booleanCondition, endTimeoutTimeNs);
    }

    @Override
    public boolean awaitUntilNanosWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long deadlineNs) throws InterruptedException {
        if (booleanCondition.isTrue()) {
            return true;
        }

        if (CondilocksUtilz.whenInitiallyFalse_DT_until(this, deadlineNs)) {
            return false;
        }

        return CondilocksUtilz.mainWaitWhileFalse_DT_until(
                this, booleanCondition, deadlineNs);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected boolean areMainWaitsTimed() {
        return false;
    }
    
    @Override
    protected final void mainWait_TT_timeout_upTo(long timeoutNs) throws InterruptedException {
        CondilocksUtilz.yieldingWait();
    }
}
