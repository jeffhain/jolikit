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

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.jolikit.lang.InterfaceBooleanCondition;

/**
 * Condilock that does not actually wait, signal, or lock.
 * 
 * Useful to deal with concurrency-designed treatments
 * when running the whole application in a single thread.
 */
public class PassiveCondilock implements InterfaceCondilock {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Does nothing.
     */
    @Override
    public void await() throws InterruptedException {
    }

    /**
     * Does nothing.
     */
    @Override
    public void awaitUninterruptibly() {
    }

    /**
     * Does nothing else than returning zero.
     * @return zero.
     */
    @Override
    public long awaitNanos(long timeoutNs) throws InterruptedException {
        return 0L;
    }

    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean awaitUntil(Date deadline) throws InterruptedException {
        return true;
    }

    /*
     * 
     */
    
    /**
     * Does not lock before running runnable.
     * @param runnable Runnable to run.
     */
    public void runInLock(Runnable runnable) {
        runnable.run();
    }

    /**
     * Does not lock before calling callable.
     * @param callable Callable to call.
     */
    public <V> V callInLock(Callable<V> callable) throws Exception {
        return callable.call();
    }

    /*
     * 
     */

    /**
     * Does nothing.
     */
    @Override
    public void signal() {
    }

    /**
     * Does nothing.
     */
    @Override
    public void signalAll() {
    }

    /*
     * 
     */

    /**
     * Does nothing.
     */
    @Override
    public void signalInLock() {
    }
    
    /**
     * Does nothing.
     */
    @Override
    public void signalAllInLock() {
    }

    /*
     * 
     */

    /**
     * @return 0.
     */
    @Override
    public long timeoutTimeNs() {
        return 0L;
    }

    /**
     * @return 0.
     */
    @Override
    public long deadlineTimeNs() {
        return 0L;
    }

    /**
     * Does nothing.
     */
    @Override
    public void awaitUntilNanosTimeoutTime(long endTimeoutTimeNs) throws InterruptedException {
    }

    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean awaitUntilNanos(long deadlineNs) throws InterruptedException {
        return true;
    }

    /**
     * @throws IllegalStateException if the specified condition is not true.
     */
    @Override
    public void awaitWhileFalseInLockUninterruptibly(InterfaceBooleanCondition booleanCondition) {
        if (!booleanCondition.isTrue()) {
            throw new IllegalStateException("boolean condition must be true");
        }
    }

    /**
     * Does nothing else than returning boolean condition's state.
     * @return True if the boolean condition is true, false otherwise.
     */
    @Override
    public boolean awaitNanosWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long timeoutNs) throws InterruptedException {
        return booleanCondition.isTrue();
    }

    /**
     * Does nothing else than returning boolean condition's state.
     * @return True if the boolean condition is true, false otherwise.
     */
    @Override
    public boolean awaitUntilNanosTimeoutTimeWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long endTimeoutTimeNs) throws InterruptedException {
        return booleanCondition.isTrue();
    }

    /**
     * Does nothing else than returning boolean condition's state.
     * @return True if the boolean condition is true, false otherwise.
     */
    @Override
    public boolean awaitUntilNanosWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long deadlineNs) throws InterruptedException {
        return booleanCondition.isTrue();
    }
}
