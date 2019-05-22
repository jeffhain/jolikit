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
import java.util.concurrent.locks.ReentrantLock;

/**
 * An extension of ReentrantLock to implement InterfaceCheckerLock.
 * 
 * Can be named (useful for locking investigations), and have a reference to
 * other locks that must not be held by threads attempting to lock this lock,
 * which is useful to ensure locking order and detect deadlock possibilities.
 */
public class ReentrantCheckerLock extends ReentrantLock implements InterfaceCheckerLock {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * Locks that must not be held by current thread on any
     * lock attempt of this lock.
     */
    private final InterfaceCheckerLock[] smallerLockArr;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a non fair lock with no name.
     */
    public ReentrantCheckerLock() {
        this(null, false);
    }

    /**
     * Creates a non fair lock with the specified name.
     * 
     * @param name Lock's name.
     */
    public ReentrantCheckerLock(String name) {
        this(name, false);
    }

    /**
     * Creates a lock with no name and the specified fairness.
     * 
     * @param fair Lock's fairness.
     */
    public ReentrantCheckerLock(boolean fair) {
        this(null, fair);
    }

    /**
     * Creates a lock with the specified name, fairness, and smaller locks.
     * 
     * @param name Lock's name.
     * @param fair Lock's fairness.
     * @param smallerLocks Locks that must not be held by current thread
     *        on any lock attempt of this lock.
     * @param NullPointerException if one of the smaller locks is null.
     */
    public ReentrantCheckerLock(
            String name,
            boolean fair,
            InterfaceCheckerLock... smallerLocks) {
        super(fair);
        this.name = name;
        for (InterfaceCheckerLock smallerLock : smallerLocks) {
            if (smallerLock == null) {
                throw new NullPointerException();
            }
        }
        this.smallerLockArr = smallerLocks;
    }

    @Override
    public String toString() {
        final String superString = super.toString();
        if ((this.name == null) && (this.smallerLockArr.length == 0)) {
            return superString;
        }
        
        final StringBuilder sb = new StringBuilder(superString);
        if (this.name != null) {
            sb.append("[");
            sb.append(this.name);
            sb.append("]");
        }
        if (this.smallerLockArr.length != 0) {
            sb.append("[smallerLocks={");
            for (int i = 0; i < this.smallerLockArr.length; i++) {
                final InterfaceCheckerLock smallerLock = this.smallerLockArr[i];
                if (i != 0) {
                    sb.append(",");
                }
                // Simple toString for smaller locks, to avoid too long string
                // in case of long chain of smaller locks.
                sb.append(smallerLock.getClass().getName());
                sb.append("@");
                sb.append(Integer.toHexString(smallerLock.hashCode()));
            }
            sb.append("}]");
        }
        return sb.toString();
    }

    /**
     * @return Lock's name, or null if it has none.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return A new array containing smaller locks, or an empty array.
     */
    public InterfaceCheckerLock[] getSmallerLockArr() {
        if (this.smallerLockArr.length == 0) {
            return this.smallerLockArr;
        } else {
            return this.smallerLockArr.clone();
        }
    }

    @Override
    public Thread getOwnerBestEffort() {
        return super.getOwner();
    }

    @Override
    public void lock() {
        this.checkNoSmallerLockHeldIfThisNotHeld();
        super.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.checkNoSmallerLockHeldIfThisNotHeld();
        super.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        this.checkNoSmallerLockHeldIfThisNotHeld();
        return super.tryLock();
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        this.checkNoSmallerLockHeldIfThisNotHeld();
        return super.tryLock(timeout, unit);
    }

    @Override
    public boolean checkNotLocked() {
        return LocksUtils.checkNotLocked(this);
    }

    @Override
    public boolean checkNotHeldByAnotherThread() {
        return LocksUtils.checkNotHeldByAnotherThread(this);
    }

    @Override
    public boolean checkHeldByCurrentThread() {
        return LocksUtils.checkHeldByCurrentThread(this);
    }

    @Override
    public boolean checkNotHeldByCurrentThread() {
        return LocksUtils.checkNotHeldByCurrentThread(this);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * If smaller locks exist, and this lock is not held by current thread,
     * checks that smaller locks are not already held by current thread,
     * i.e. that this lock will not be acquired from within a smaller lock.
     */
    private boolean checkNoSmallerLockHeldIfThisNotHeld() {
        if (this.smallerLockArr.length != 0) {
            if (!this.isHeldByCurrentThread()) {
                // Lock not yet acquired: checking that smaller locks
                // are not held.
                for (InterfaceCheckerLock smallerLock : this.smallerLockArr) {
                    smallerLock.checkNotHeldByCurrentThread();
                }
            }
        }
        return true;
    }
}
