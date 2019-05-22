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

import net.jolikit.lang.LangUtils;

public class LocksUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @return True if lock is not held by a thread, throws exception otherwise.
     * @throws IllegalStateException if a thread does hold the lock.
     */
    public static boolean checkNotLocked(InterfaceCheckerLock lock) {
        if (lock.isLocked()) {
            // Comma after "held", because the main point of this method
            // is to check lock status, not whether or not lock is
            // held by current thread.
            if (lock.isHeldByCurrentThread()) {
                throw new IllegalStateException(
                        "Lock held, by current thread ["
                                + LangUtils.toString(Thread.currentThread())
                                + "], lock = " + lock);
            } else {
                throw new IllegalStateException(
                        "Lock held, not by current thread ["
                                + LangUtils.toString(Thread.currentThread())
                                + "], possible owner thread is ["
                                + LangUtils.toString(lock.getOwnerBestEffort())
                                + "], lock = " + lock);
            }
        }
        return true;
    }

    /**
     * @return True if lock is not held by another thread, throws exception otherwise.
     * @throws ConcurrentModificationException if another thread does hold the lock.
     */
    public static boolean checkNotHeldByAnotherThread(InterfaceCheckerLock lock) {
        if (lock.isLocked() && (!lock.isHeldByCurrentThread())) {
            throw new ConcurrentModificationException(
                    "Lock held by another thread, current thread is ["
                            + LangUtils.toString(Thread.currentThread())
                            + "], possible owner thread is ["
                            + LangUtils.toString(lock.getOwnerBestEffort())
                            + "], lock = " + lock);
        }
        return true;
    }

    /**
     * @return True if lock is held by current thread, throws exception otherwise.
     * @throws ConcurrentModificationException if the calling thread does not hold the lock.
     */
    public static boolean checkHeldByCurrentThread(InterfaceCheckerLock lock) {
        if (!lock.isHeldByCurrentThread()) {
            if (lock.isLocked()) {
                // If lock went held by some thread since current thread
                // called "isHeldByCurrentThread" method, no problem.
                throw new ConcurrentModificationException(
                        "Lock not held by current thread ["
                                + LangUtils.toString(Thread.currentThread())
                                + "], lock = " + lock);
            } else {
                // If lock went un-held by some thread since current thread
                // called "isHeldByCurrentThread" method, no problem.
                throw new ConcurrentModificationException(
                        "Lock not held by a thread, current thread is ["
                                + LangUtils.toString(Thread.currentThread())
                                + "], lock = " + lock);
            }
        }
        return true;
    }

    /**
     * @return True if lock is not held by current thread, throws exception otherwise.
     * @throws IllegalStateException if the calling thread holds the lock.
     */
    public static boolean checkNotHeldByCurrentThread(InterfaceCheckerLock lock) {
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException(
                    "Lock held by current thread ["
                            + LangUtils.toString(Thread.currentThread())
                            + "], lock = " + lock);
        }
        return true;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private LocksUtils() {
    }
}
