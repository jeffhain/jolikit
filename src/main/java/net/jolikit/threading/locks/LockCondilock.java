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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import net.jolikit.lang.InterfaceBooleanCondition;

/**
 * Condilock based on a Lock and a Condition.
 */
public class LockCondilock extends AbstractCondilock {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Lock lock;
    private final Condition condition;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a condilock based on a default lock.
     */
    public LockCondilock() {
        this(CondilocksUtilz.newDefaultLock());
    }

    /**
     * Creates a condilock based on the specified lock
     * and a new condition obtained from it.
     */
    public LockCondilock(Lock lock) {
        this(
                lock,
                lock.newCondition());
    }

    /**
     * @param lock Lock to use for locking.
     * @param condition A condition based on the specified lock.
     */
    public LockCondilock(
            Lock lock,
            Condition condition) {
        this.lock = lock;
        this.condition = condition;
    }
    
    /*
     * 
     */

    /**
     * @return Lock used for locking.
     */
    public Lock getLock() {
        return this.lock;
    }
    
    /**
     * @return Condition used for waiting.
     */
    public Condition getCondition() {
        return this.condition;
    }

    /*
     * 
     */

    @Override
    public void runInLock(Runnable runnable) {
        final Lock lock = this.lock;
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <V> V callInLock(Callable<V> callable) throws Exception {
        final Lock lock = this.lock;
        lock.lock();
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    /*
     * 
     */
    
    @Override
    public void signal() {
        this.condition.signal();
    }
    
    @Override
    public void signalAll() {
        this.condition.signalAll();
    }
    
    /*
     * 
     */

    @Override
    public void signalInLock() {
        final Lock lock = this.lock;
        lock.lock();
        try {
            this.condition.signal();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void signalAllInLock() {
        final Lock lock = this.lock;
        lock.lock();
        try {
            this.condition.signalAll();
        } finally {
            lock.unlock();
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

        final Lock lock = this.lock;
        lock.lock();
        try {
            if (CondilocksUtilz.afterLock(booleanCondition)) {
                return true;
            }
            return CondilocksUtilz.mainWaitWhileFalse_TT_timeout(
                    this, booleanCondition, timeoutNs);
        } finally {
            lock.unlock();
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

        final Lock lock = this.lock;
        lock.lock();
        try {
            if (CondilocksUtilz.afterLock(booleanCondition)) {
                return true;
            }
            return CondilocksUtilz.mainWaitWhileFalse_TT_until(
                    this, booleanCondition, endTimeoutTimeNs);
        } finally {
            lock.unlock();
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

        final Lock lock = this.lock;
        lock.lock();
        try {
            if (CondilocksUtilz.afterLock(booleanCondition)) {
                return true;
            }
            return CondilocksUtilz.mainWaitWhileFalse_DT_until(
                    this, booleanCondition, deadlineNs);
        } finally {
            lock.unlock();
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
        CondilocksUtilz.lockConditionWaitNs(this.condition, timeoutNs);
    }
}
