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
package net.jolikit.bwd.impl.utils.sched;

import java.util.ConcurrentModificationException;

import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

/**
 * Default implementations for InterfaceWorkerAwareScheduler's
 * checkIsWorkerThread(...) and checkIsNotWorkerThread(...) methods,
 * with messages specific to the case of UI thread schedulers.
 */
public class UiThreadChecker {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @throws ConcurrentModificationException if current thread is not UI thread.
     */
    public static void checkIsUiThread(InterfaceWorkerAwareScheduler uiThreadScheduler) {
        if (!uiThreadScheduler.isWorkerThread()) {
            throw new ConcurrentModificationException(
                    "current thread ["
                            + Thread.currentThread()
                            + "] is not UI thread");
        }
    }

    /**
     * @throws IllegalStateException if current thread is UI thread.
     */
    public static void checkIsNotUiThread(InterfaceWorkerAwareScheduler uiThreadScheduler) {
        if (uiThreadScheduler.isWorkerThread()) {
            throw new IllegalStateException(
                    "current thread ["
                            + Thread.currentThread()
                            + "] is UI thread");
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private UiThreadChecker() {
    }
}
