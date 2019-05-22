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
 * Condilock based on an object's monitor.
 */
public class MonitorCondilock extends AbstractCondilock {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Object mutex;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a condilock based on a default mutex.
     */
    public MonitorCondilock() {
        this(new Object());
    }

    /**
     * @param mutex Object which monitor is to be used for locking.
     */
    public MonitorCondilock(Object mutex) {
        this.mutex = mutex;
    }
    
    /*
     * 
     */

    /**
     * @return Object which monitor is used for locking.
     */
    public Object getMutex() {
        return this.mutex;
    }

    /*
     * 
     */

    @Override
    public void runInLock(Runnable runnable) {
        synchronized (this.mutex) {
            runnable.run();
        }
    }

    @Override
    public <V> V callInLock(Callable<V> callable) throws Exception {
        synchronized (this.mutex) {
            return callable.call();
        }
    }

    /*
     * 
     */

    @Override
    public void signal() {
        this.mutex.notify();
    }

    @Override
    public void signalAll() {
        this.mutex.notifyAll();
    }

    /*
     * 
     */

    @Override
    public void signalInLock() {
        synchronized (this.mutex) {
            this.mutex.notify();
        }
    }

    @Override
    public void signalAllInLock() {
        synchronized (this.mutex) {
            this.mutex.notifyAll();
        }
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

        synchronized (this.mutex) {
            if (CondilocksUtilz.afterLock(booleanCondition)) {
                return true;
            }
            return CondilocksUtilz.mainWaitWhileFalse_TT_timeout(
                    this, booleanCondition, timeoutNs);
        }
    }

    @Override
    public boolean awaitUntilNanosTimeoutTimeWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long endTimeoutTimeNs) throws InterruptedException {
        if (booleanCondition.isTrue()) {
            return true;
        }

        if (CondilocksUtilz.whenInitiallyFalse_TT_until(this, endTimeoutTimeNs)) {
            return false;
        }

        synchronized (this.mutex) {
            if (CondilocksUtilz.afterLock(booleanCondition)) {
                return true;
            }
            return CondilocksUtilz.mainWaitWhileFalse_TT_until(
                    this, booleanCondition, endTimeoutTimeNs);
        }
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

        synchronized (this.mutex) {
            if (CondilocksUtilz.afterLock(booleanCondition)) {
                return true;
            }
            return CondilocksUtilz.mainWaitWhileFalse_DT_until(
                    this, booleanCondition, deadlineNs);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected boolean areMainWaitsTimed() {
        return true;
    }
    
    @Override
    protected void mainWait_TT_timeout_upTo(long timeoutNs) throws InterruptedException {
        CondilocksUtilz.mutexWaitNs(this.mutex, timeoutNs);
    }
}
