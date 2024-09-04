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
package net.jolikit.threading.prl;

interface InterfaceParallelizerForTests extends InterfaceParallelizer {
    
    /**
     * If nothing to describe, must return an empty string.
     * 
     * @return A string describing properties specific to this type
     *         and instance of parallelizer, i.e. other than its class
     *         and its parallelism.
     */
    public String getSpeDescr();
    
    /**
     * @return True if it's allowed to call the parallelizer
     *         from one of its worker threads, or from the user thread
     *         that is calling it if it's used as worker thread,
     *         false otherwise.
     */
    public boolean isReentrant();
    
    /**
     * @return True if executor rejects cancellables by calling onCancel(),
     *         false if it always rejected using RejectedExecutionException. 
     */
    public boolean executorRejectsWithOnCancelIfCancellable();
    
    /**
     * Shuts down the parallelizer and its backing resources.
     * Must complete only once the shutdown terminated.
     */
    public void shutdownAndWait();
}
