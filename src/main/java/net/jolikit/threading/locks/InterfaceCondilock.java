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

import java.util.concurrent.locks.Condition;

import net.jolikit.lang.InterfaceBooleanCondition;

/**
 * Interface for conditions that provide a way to run treatments in their lock.
 */
public interface InterfaceCondilock extends Condition, InterfaceLocker {
    
    /*
     * Reason for awaitXxxWhileFalseInLockXxx methods not to check interruption
     * status if boolean condition is first evaluated as true:
     * If the user does not want the cost of an interruption status check
     * when the boolean condition is true, he would need to check the
     * boolean condition himself first, which would always lead to a
     * double evaluation if it's false (with interruption check in between).
     * With current design, if the user wants interruption status check,
     * he can still do it himself before calling the method.
     * 
     * Doing best effort to respect the naming conventions of Condition
     * (timeouts and deadlines by default computed with different times
     * (typically System.nanoTime() and System.currentTimeMillis())
     * without it appearing in method's name, etc.).
     */
    
    /**
     * @return Time, in nanoseconds, used for timeouts measurement.
     */
    public long timeoutTimeNs();

    /**
     * @return Time, in nanoseconds, used for deadlines measurement.
     */
    public long deadlineTimeNs();
    
    /*
     * Convenience method, not to create garbage using InterfaceLocker.
     */

    /**
     * Calls signal() in lock.
     */
    public void signalInLock();
    
    /**
     * Calls signalAll() in lock.
     */
    public void signalAllInLock();

    /*
     * Convenience methods. These methods could be defined statically in an
     * utility class, with the condilock as supplementary argument, and use
     * InterfaceLocker's method when wanting to run some code in lock, but
     * that could generate much Runnable/Callable garbage, so we define them
     * here instead. If efficient non-garbage lambda get available, this
     * interface could be simplified... or not, for it's handy to have these
     * methods directly available on the condilock.
     */

    /**
     * Is roughly equivalent to calling awaitNanos(long) with corresponding timeout,
     * but typically involves less calls to timing method, especially if end timeout
     * time is already known.
     * 
     * @param endTimeoutTimeNs Timeout time to wait for, in nanoseconds.
     *        This is not a timeout, but a time compared to timeoutTimeNs(),
     *        to compute the timeout to wait.
     * @throws InterruptedException if current thread is interrupted.
     */
    public void awaitUntilNanosTimeoutTime(long endTimeoutTimeNs) throws InterruptedException;

    /**
     * Method to wait until a deadline, which does not require to provide a Date object
     * (unlike awaitUntil(Date)).
     * 
     * @param deadlineNs Deadline to wait for, in nanoseconds.
     * @return True if this method returned before the specified deadline could be reached,
     *         false otherwise.
     * @throws InterruptedException if current thread is interrupted.
     */
    public boolean awaitUntilNanos(long deadlineNs) throws InterruptedException;

    /**
     * Method to wait for a boolean condition to be true, uninterruptibly.
     * 
     * If interruption of current thread is detected while waiting,
     * interruption status is restored before returning.
     * 
     * @param booleanCondition Condition waited to be true (waiting while it is false).
     */
    public void awaitWhileFalseInLockUninterruptibly(InterfaceBooleanCondition booleanCondition);

    /**
     * Method to wait for a boolean condition to be true,
     * or specified timeout to be elapsed.
     * 
     * If the specified condition is true when this method is called,
     * it returns true whatever the timeout.
     * Else, it is evaluated again in lock, before eventual wait(s).
     * 
     * @param booleanCondition Condition waited to be true (waiting while it is false).
     * @param timeoutNs Timeout, in nanoseconds, after which this method returns false.
     * @return True if the boolean condition was true before timeout elapsed, false otherwise.
     * @throws InterruptedException if current thread is interrupted, possibly unless the specified
     *         boolean condition is first evaluated as true (authorized for performances).
     */
    public boolean awaitNanosWhileFalseInLock(
            InterfaceBooleanCondition booleanCondition,
            long timeoutNs) throws InterruptedException;

    /**
     * This method can be used instead of awaitNanosWhileFalseInLock(...),
     * and along with timeoutTimeNs(), to easily design multiple layers of waiting,
     * without having to compute remaining timeout in each of them.
     * This is useful when awaitUntilNanosWhileFalseInLock(...) can't be used,
     * for it is typically based on System.currentTimeMillis(), which is not
     * accurate, and might jump around.
     * 
     * Ex.: instead of:
     * public Train waitForTrain(long timeoutNs) {
     *    final long endTimeoutTimeNs = NumbersUtils.plusBounded(System.nanoTime(), timeoutNs);
     *    final TrainWaitStopBc booleanCondition = new TrainWaitStopBc();
     *    while (true) {
     *       final Train train = getNextTrainToArrive();
     *       if (condilock.awaitNanosWhileFalseInLock(booleanCondition, timeoutNs)) {
     *          if (didTrainArrive(train)) {
     *             // Got the train.
     *             return train;
     *          } else {
     *             // Wait stopped not because the train arrived,
     *             // but because another train has been added,
     *             // which might arrive earlier.
     *             // Need to compute remaining timeout.
     *             timeoutNs = endTimeoutTimeNs - System.nanoTime();
     *          }
     *       } else {
     *          // Timeout elapsed.
     *          return null;
     *       }
     *    }
     * }
     * one can do:
     * public Train waitForTrain(long timeoutNs) {
     *    final long endTimeoutTimeNs = NumbersUtils.plusBounded(condilock.timeoutTimeNs(), timeoutNs);
     *    final TrainWaitStopBc booleanCondition = new TrainWaitStopBc();
     *    while (true) {
     *       final Train train = getNextTrainToArrive();
     *       if (condilock.awaitUntilNanosTimeoutTimeWhileFalseInLock(booleanCondition, endTimeoutTimeNs)) {
     *          if (didTrainArrive(train)) {
     *             return train;
     *          }
     *       } else {
     *          return null;
     *       }
     *    }
     * }
     * Note: instead of piling-up wait layers, one could also design
     * a complex boolean condition, but it could be slow and slow down
     * things when being called in lock.
     * 
     * If the specified condition is true when this method is called,
     * it returns true whatever the end timeout time.
     * Else, it is evaluated again in lock, before eventual wait(s).
     * 
     * @param booleanCondition Condition waited to be true (waiting while it is false).
     * @param endTimeoutTimeNs Timeout time, in nanoseconds, after which this method returns false.
     *        This is not a timeout, but a time compared to timeoutTimeNs(),
     *        to decide whether one can still wait or not.
     * @return True if the boolean condition was true before end timeout time occurred, false otherwise.
     * @throws InterruptedException if current thread is interrupted, possibly unless the specified
     *         boolean condition is first evaluated as true (authorized for performances).
     */
    public boolean awaitUntilNanosTimeoutTimeWhileFalseInLock(
            final InterfaceBooleanCondition booleanCondition,
            long endTimeoutTimeNs) throws InterruptedException;

    /**
     * Method to wait for a boolean condition to be true,
     * or specified deadline to occur.
     * 
     * If the specified condition is true when this method is called,
     * it returns true whatever the deadline.
     * Else, it is evaluated again in lock, before eventual wait(s).
     * 
     * @param booleanCondition Condition waited to be true (waiting while it is false).
     * @param deadlineNs Deadline, in nanoseconds, after which this method returns false.
     * @return True if the boolean condition was true before deadline occurred, false otherwise.
     * @throws InterruptedException if current thread is interrupted, possibly unless the specified
     *         boolean condition is first evaluated as true (authorized for performances).
     */
    public boolean awaitUntilNanosWhileFalseInLock(
            final InterfaceBooleanCondition booleanCondition,
            long deadlineNs) throws InterruptedException;
}
