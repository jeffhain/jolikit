/*
 * Copyright 2019-2024 Jeff Hain
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
package net.jolikit.threading.basics;

import java.util.ConcurrentModificationException;

/**
 * Default implementations for InterfaceWorkerAwareScheduler's
 * checkIsWorkerThread(...) and checkIsNotWorkerThread(...) methods.
 */
public class WorkerThreadChecker {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @throws ConcurrentModificationException if current thread is not
     *         a worker thread.
     */
    public static void checkIsWorkerThread(InterfaceWorkerAware workerAware) {
        if (!workerAware.isWorkerThread()) {
            throw new ConcurrentModificationException(
                    "current thread ["
                            + Thread.currentThread()
                            + "] is not a worker thread of this scheduler");
        }
    }

    /**
     * @throws IllegalStateException if current thread is a worker thread.
     */
    public static void checkIsNotWorkerThread(InterfaceWorkerAware workerAware) {
        if (workerAware.isWorkerThread()) {
            throw new IllegalStateException(
                    "current thread ["
                            + Thread.currentThread()
                            + "] is a worker thread of this scheduler");
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private WorkerThreadChecker() {
    }
}
