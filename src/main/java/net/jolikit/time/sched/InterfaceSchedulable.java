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
package net.jolikit.time.sched;

/**
 * Interface for scheduling-aware, re-scheduling-able and cancellation-aware
 * runnables.
 * To use with executors that can recognize it, else won't work (getScheduling()
 * won't return a valid scheduling, and onCancel() won't be called).
 * 
 * Definitions:
 * - actual execution time
 *   = execution time
 *   = a measure of time just-or-soon before call to run().
 * - theoretical execution time
 *   = schedule time
 *   = time at which the next execution is planned to occur.
 * 
 * Advantage over raw runnables:
 * - Are aware of their theoretical execution time (sometimes we want to use it
 *   instead of actual execution time, for threads scheduling not to interfere
 *   with computations, for example when computing the time of next schedule).
 * - Are aware of their actual execution time (without additional call to clock,
 *   since schedulers are typically aware of it already).
 * - Can re-schedule themselves at will and at whatever times (much more agile
 *   than the fixed-period or fixed-delay schedulings from
 *   ScheduledExecutorService), and with less dependencies and overhead than
 *   if having to re-schedule themselves using scheduler and clock directly.
 * - Are aware of their cancellation and can act accordingly (instead of having
 *   to use the big and uneasy indirection of a rejected execution handler,
 *   which can still be used aside).
 * 
 * Making this interface extend InterfaceCancellable, and not just Runnable,
 * to avoid the boilerplate of importing multiple interfaces when wanting a
 * cancellation-aware schedulable, at the minimal cost of having to implement
 * onCancel() method even if not needed.
 * The main purpose of splitting onCancel() method in another interface,
 * is indeed not to relieve the user from the need to implement it, but
 * to relieve schedulers from the overhead of scheduling handling (which
 * typically involves actual time computation and multiple megamorphic calls),
 * when the user just want to implement a cancellation-aware runnable.
 * 
 * Using a radically new interface, rather than extending Runnable, would
 * make things simpler and faster in some ways (no need for a scheduling
 * object), but would be less familiar to users and less compatible with
 * the well-known and widely-used Executor interface.
 * 
 * Typical usage:
 * public void run() {
 *     final InterfaceScheduling scheduling = this.getScheduling();
 *     final long theoreticalTimeNs = scheduling.getTheoreticalTimeNs();
 *     final long actualTimeNs = scheduling.getActualTimeNs();
 *     
 *     // (...) do some work
 *     
 *     if (mustWorkAgainLater) {
 *         // Plans a subsequent execution of this schedulable one second
 *         // after actual time (could use theoretical time to avoid drift).
 *         // Will be discarded if current method throws.
 *         scheduling.setNextTheoreticalTimeNs(actualTimeNs + 1000L * 1000L * 1000L);
 *     }
 * }
 * public void onCancel() {
 *     // Pending execution has been cancelled.
 * }
 */
public interface InterfaceSchedulable extends InterfaceCancellable {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Called just before each call to run(), in the same thread,
     * by executors recognizing this interface, or by schedulers
     * (which must recognize it).
     * 
     * The specified scheduling must not be used (other than by scheduler code)
     * outside of subsequent call to run().
     * 
     * Although this might be a weird case, if the schedulable is executed
     * concurrently by a same or multiple schedulers, the specified scheduling
     * can be stored in a concurrent map with current thread as key.
     * 
     * @param scheduling Holds info about scheduling of current execution
     *        (theoretical time and actual time), and allows to plan another
     *        execution of this schedulable.
     */
    public void setScheduling(InterfaceScheduling scheduling);
}
