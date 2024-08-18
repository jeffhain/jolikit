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

/**
 * Interface for cancellation-aware runnables.
 * To use with executors that can recognize it, else won't work (onCancel()
 * won't be called).
 * 
 * Advantage over raw runnables:
 * - Are aware of their cancellation and can act accordingly (instead of having
 *   to use the big and uneasy indirection of a rejected execution handler,
 *   which can still be used aside).
 * 
 * Typical usage:
 * public void onCancel() {
 *     // Pending execution has been cancelled.
 * }
 */
public interface InterfaceCancellable extends Runnable {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Called when the execution is cancelled.
     * 
     * Can be called from any thread (like from a thread shutting down an
     * executor, or from a thread cancelling a task), but it does not mandate
     * thread safety, which depending on design can be ensured by enclosing
     * treatments if needed.
     * 
     * Might be called synchronously from within a run() call, for example
     * if a repeating task cancels itself from one of its executions.
     */
    public void onCancel();
}
