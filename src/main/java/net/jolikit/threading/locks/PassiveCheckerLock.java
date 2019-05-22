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

/**
 * Lock that does not actually lock, which isLocked() and isHeldByCurrentThread()
 * methods always return false, and which check methods always return true.
 * 
 * If only one thread is used, can replace transparently actual locks,
 * as long as no treatment relies on isLocked() or isHeldByCurrenThread() methods
 * to return true, or on check methods to throw an exception, to work properly.
 * 
 * Useful to deal with concurrency-designed treatments
 * when running the whole application in a single thread.
 */
public class PassiveCheckerLock implements InterfaceCheckerLock {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Does nothing.
     */
    @Override
    public void lock() {
    }

    /**
     * Does nothing.
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
    }

    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean tryLock() {
        return true;
    }

    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return true;
    }

    /**
     * Does nothing.
     */
    @Override
    public void unlock() {
    }

    /**
     * @return A new passive condition.
     */
    @Override
    public Condition newCondition() {
        return new PassiveCondilock();
    }

    /*
     * 
     */
    
    /**
     * @return false.
     */
    @Override
    public boolean isLocked() {
        return false;
    }
    
    /**
     * @return false.
     */
    @Override
    public boolean isHeldByCurrentThread() {
        return false;
    }
    
    /**
     * @return 0.
     */
    @Override
    public int getHoldCount() {
        return 0;
    }

    /**
     * @return null.
     */
    @Override
    public Thread getOwnerBestEffort() {
        return null;
    }
    
    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean checkNotLocked() {
        return true;
    }

    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean checkNotHeldByAnotherThread() {
        return true;
    }

    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean checkHeldByCurrentThread() {
        return true;
    }
    
    /**
     * Does nothing else than returning true.
     * @return true.
     */
    @Override
    public boolean checkNotHeldByCurrentThread() {
        return true;
    }
}
