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

import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.Lock;

/**
 * Interface for locks that provide methods to check whether or not
 * they are held, by current thread or by another thread.
 * 
 * One of the rare Jolikit classes designed not for faster execution,
 * but for safer coding.
 */
public interface InterfaceCheckerLock extends Lock {

    /**
     * @return True if this lock is locked, false otherwise.
     */
    public boolean isLocked();
    
    /**
     * @return True if this lock is held by current thread, false otherwise.
     */
    public boolean isHeldByCurrentThread();
    
    /**
     * @return The number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread.
     */
    public int getHoldCount();
    
    /**
     * @return A best effort estimation (possibly null) of the owner,
     *         or null if not owned.
     */
    public Thread getOwnerBestEffort();
    
    /*
     * Check methods, which are convenience methods for actual locks, since
     * they can be implemented based on other methods, but which need to be
     * defined here for passive locks.
     * 
     * These methods return true if the check is positive, to allow for call in an assertion.
     */
    
    /**
     * @return True if lock is not held by a thread, throws exception otherwise.
     * @throws IllegalStateException if a thread does hold the lock.
     */
    public boolean checkNotLocked();

    /**
     * @return True if lock is not held by another thread, throws exception otherwise.
     * @throws ConcurrentModificationException if another thread does hold the lock.
     */
    public boolean checkNotHeldByAnotherThread();

    /**
     * This method can be used to check critical sections are not being run by rogue threads.
     * 
     * @return True if lock is held by current thread, throws exception otherwise.
     * @throws ConcurrentModificationException if the calling thread does not hold the lock.
     */
    public boolean checkHeldByCurrentThread();
    
    /**
     * @return True if lock is not held by current thread, throws exception otherwise.
     * @throws IllegalStateException if the calling thread holds the lock.
     */
    public boolean checkNotHeldByCurrentThread();
}
