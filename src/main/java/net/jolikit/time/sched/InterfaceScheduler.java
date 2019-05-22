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

import java.util.concurrent.Executor;

import net.jolikit.time.clocks.InterfaceClock;

/**
 * An executor that can handle implementations of InterfaceSchedulable and
 * InterfaceCancellable, in addition to regular Runnables, and provides
 * methods for ASAP or timed schedules.
 * 
 * Also provides access to (or to a view of) the clock it uses as time frame.
 * 
 * Runnable execution methods defined in this interface (including
 * execute(Runnable)) must recognize implementations of InterfaceSchedulable
 * or InterfaceCancellable, and process them accordingly (call to
 * setScheduling(...) just before call to run(), and call to onCancel() in case
 * of cancellation, in addition or instead of providing the Runnable
 * (implementing InterfaceCancellable) to some rejected execution handler
 * (if any)).
 * 
 * To wrap a runnable that might be a schedulable, you must not use a simple
 * runnable, as it would not allow for setScheduling(...) and onCancel()
 * methods to be called, and yield NPEs or other bugs.
 * Instead, you have to use a schedulable, as follows:
 * final InterfaceSchedulable wrapper = new InterfaceSchedulable() {
 *     @Override
 *     public void setScheduling(InterfaceScheduling scheduling) {
 *         SchedUtils.call_setScheduling_IfSchedulable(runnable, scheduling);
 *     }
 *     @Override
 *     public void run() {
 *         runnable.run();
 *     }
 *     @Override
 *     public void onCancel() {
 *         SchedUtils.call_onCancel_IfCancellable(runnable);
 *     }
 * };
 * 
 * Main differences with JDK's ScheduledExecutorService interface:
 * - more:
 *   - Can be used transparently for either hard or soft scheduling (no waiting,
 *     such as with futures, which makes no sense when using a single thread).
 *   - Schedulables can re-schedule themselves at each execution, without having
 *     to depend on this interface, making repetition more flexible than with
 *     scheduleAtFixedRate(...) or scheduleWithFixedDelay(...).
 *   - For runnables implementing InterfaceSchedulable, cancellation can be
 *     dealt with by each of them, instead of having to use a rejected execution
 *     handler (which remains possible, as an additional feature).
 * - less:
 *   - Does not provide a number of facilities, like futures, or invokeAny
 *     method, but these can be added at a higher level.
 */
public interface InterfaceScheduler extends Executor {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return A clock which time (time and time speed) is identical to the time
     *         of the clock used by this scheduler (typically this clock, or a
     *         wrapper around it, for defensive programming).
     */
    public InterfaceClock getClock();
    
    /*
     * 
     */
    
    /**
     * Depending on implementation, this method might or might not guarantee
     * FIFO ordering (unlike executeAfterNs(Runnable,0L) which necessarily
     * does not guarantee it in case of time jumps).
     * 
     * For schedulables, theoretical execution time must be set to actual
     * execution time, and not to current time at the time this method is
     * called (if you desire so, you can use executeAfterNs(Runnable,0L) instead).
     * 
     * @param runnable Runnable to execute ASAP.
     */
    @Override
    public void execute(Runnable runnable);
    
    /**
     * @param runnable Runnable to execute at the specified time.
     * @param timeNs Time at which the specified runnable must be executed,
     *        in nanoseconds, and in the time frame of scheduler's clock.
     */
    public void executeAtNs(
            Runnable runnable,
            long timeNs);

    /**
     * Convenience method (can be implemented on top of executeAtNs(...)).
     * 
     * @param runnable Runnable to execute after the specified delay elapsed.
     * @param delayNs Delay after which the specified runnable must be executed,
     *        in nanoseconds, and in the time frame of scheduler's clock.
     */
    public void executeAfterNs(
            Runnable runnable,
            long delayNs);
    
    /**
     * Convenience method (can be implemented on top of executeAtNs(...)).
     * 
     * @param runnable Runnable to execute at the specified time.
     * @param timeS Time at which the specified runnable must be executed,
     *        in seconds, and in the time frame of scheduler's clock.
     */
    public void executeAtS(
            Runnable runnable,
            double timeS);

    /**
     * Convenience method (can be implemented on top of executeAtNs(...)).
     * 
     * @param runnable Runnable to execute after the specified delay elapsed.
     * @param delayS Delay after which the specified runnable must be executed,
     *        in seconds, and in the time frame of scheduler's clock.
     */
    public void executeAfterS(
            Runnable runnable,
            double delayS);
}
