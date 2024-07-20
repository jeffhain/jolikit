/*
 * Copyright 2024 Jeff Hain
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
package net.jolikit.time.sched;

import java.util.ConcurrentModificationException;

/**
 * Interface for worker threads owners that are aware of their threads.
 */
public interface InterfaceWorkerAware {
    
    /*
     * Not defining an isWorkerThread(Thread) method,
     * for it to be implementable even if it would, when true,
     * only be computable from within a worker thread.
     */
    
    /**
     * @return Whether current thread is, currently at least,
     *         a worker thread of this scheduler.
     */
    public boolean isWorkerThread();

    /**
     * Convenience method.
     * 
     * @throws ConcurrentModificationException if current thread
     *         is not a worker thread of this scheduler.
     */
    public void checkIsWorkerThread();

    /**
     * Convenience method.
     * 
     * @throws IllegalStateException if current thread
     *         is a worker thread of this scheduler.
     */
    public void checkIsNotWorkerThread();
}
