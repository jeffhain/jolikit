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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.TimeUtils;

/**
 * Similar to AbstractRepeatableRunnable, but it can be cancelled concurrently,
 * and its public methods are thread-safe except setNextTheoreticalTimeNs(...)
 * which is not meant to be used concurrently with execution.
 * 
 * Protected onBegin(), onEnd(), onDone() and runImpl(...) methods are called
 * within a lock which can be specified by the user.
 * Allowing users to specify this lock allows them, for example, to figure out
 * whether the task has been executed, and if not, to cancel it, and know it has
 * not been executed concurrently in the mean time.
 * Example:
 * lock.lock();
 * try {
 *     // Here we are in the lock, so none of
 *     // onBegin()/onEnd()/onDone()/runImpl(...)
 *     // is being executed.
 *     if (task.isDone()) {
 *         if (task.isCancelled()) {
 *             // Done because has been cancelled.
 *         } else {
 *             // Done without cancellation.
 *         }
 *     } else {
 *         // This call will call onEnd() and onDone() synchronously,
 *         // and return true.
 *         // No need to interrupt since not being run concurrently.
 *         task.cancel(false);
 *     }
 * } finally {
 *     lock.unlock();
 * }
 */
public abstract class AbstractRepeatableTask implements InterfaceCancellable {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean AZZERTIONS = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyRepRunnable extends AbstractRepeatableRunnable {
        public MyRepRunnable(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        public void run() {
            if (AbstractRepeatableTask.this.isCancellationRequested()) {
                // Canceling now, else will end up done as not being cancelled
                // (normal way of being done, when no new execution is planned).
                this.onCancel();
            } else {
                super.run();
            }
        }
        /*
         * 
         */
        @Override
        protected void onBegin() {
            AbstractRepeatableTask.this.onBegin();
        }
        @Override
        protected void onEnd() {
            AbstractRepeatableTask.this.onEnd();
        }
        @Override
        protected void onDone() {
            AbstractRepeatableTask.this.onDone();
        }
        @Override
        protected void runImpl(long theoreticalTimeNs, long actualTimeNs) {
            if (AbstractRepeatableTask.this.isCancellationRequested()) {
                // Cancellation might have been requested in onBegin(),
                // in which case we want to cancel now.
                this.onCancel();
            } else {
                AbstractRepeatableTask.this.runImpl(theoreticalTimeNs, actualTimeNs);
            }
        }
        /**
         * Overriding to re-schedule the task,
         * not the (non-thread-safe) repeatable runnable. 
         */
        @Override
        protected void rescheduleAtNs(long timeNs) {
            rescheduleTaskAtNs(timeNs);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Also used as run() running nullification mutex,
     * to avoid creation of a specific object as mutex.
     */
    private final MyRepRunnable repRunnable;
    
    private final Lock lock;
    
    private volatile boolean cancellationRequested;

    /**
     * The thread executing the run() method
     * (but not onCancel() method, except if called in run()).
     */
    private volatile Thread runnerThread = null;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractRepeatableTask(InterfaceScheduler scheduler) {
        this.repRunnable = new MyRepRunnable(scheduler);
        // Avoiding the null check of the other constructor.
        this.lock = new ReentrantLock();
    }
    
    public AbstractRepeatableTask(
            InterfaceScheduler scheduler,
            Lock lock) {
        this.repRunnable = new MyRepRunnable(scheduler);
        this.lock = LangUtils.requireNonNull(lock);
    }
    
    /*
     * 
     */

    public InterfaceScheduler getScheduler() {
        return this.repRunnable.getScheduler();
    }
    
    /*
     * 
     */
    
    /**
     * Not thread-safe.
     * 
     * To be called before submitting this runnable to a scheduler,
     * for first call to runImpl(...) to use proper theoretical time,
     * and from runImpl(...) to request a new execution.
     * 
     * @param nextTheoreticalTimeNs Theoretical time, in nanoseconds,
     *        at which next call to runImpl(...) must occur.
     */
    public void setNextTheoreticalTimeNs(long nextTheoreticalTimeNs) {
        this.repRunnable.setNextTheoreticalTimeNs(nextTheoreticalTimeNs);
    }

    /**
     * Thread-safe.
     */
    @Override
    public void run() {
        // Need locking to protect against concurrent calls of onCancel().
        final Lock lock = this.lock;
        lock.lock();
        try {
            this.run_locked();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Thread-safe.
     */
    @Override
    public void onCancel() {
        this.requestCancellation();
        
        final Lock lock = this.lock;
        lock.lock();
        try {
            this.repRunnable.onCancel();
        } finally {
            lock.unlock();
        }
    }

    /*
     * Life cycle methods, same as those in AbstractRepeatableRunnable.
     */
    
    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onBegin() call has not yet been initiated, false otherwise.
     */
    public boolean isPending() {
        return this.repRunnable.isPending();
    }
    
    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onBegin() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnBeginBeingCalled() {
        return this.repRunnable.isOnBeginBeingCalled();
    }
    
    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if runImpl(...) is being called or if an additional call to it
     *         is scheduled, false otherwise.
     */
    public boolean isRepeating() {
        return this.repRunnable.isRepeating();
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onEnd() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnEndBeingCalled() {
        return this.repRunnable.isOnEndBeingCalled();
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onDone() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnDoneBeingCalled() {
        return this.repRunnable.isOnDoneBeingCalled();
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onDone() call has detectably finished, false otherwise.
     */
    public boolean isDone() {
        return this.repRunnable.isDone();
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if termination (call to onEnd() and then onDone(), or just
     *         onDone()) has been initiated, false otherwise.
     */
    public boolean isTerminatingOrDone() {
        return this.repRunnable.isTerminatingOrDone();
    }
    
    /**
     * Thread-safe and non-blocking.
     * 
     * Note: A task can be cancelled but not yet done.
     * 
     * @return True if this task is being cancelled (i.e. is being
     *         terminated due to cancellation), or has been cancelled and is
     *         done, false otherwise.
     */
    public boolean isCancelled() {
        return this.repRunnable.isCancelled();
    }

    /*
     * Life cycle methods, additional to those in AbstractRepeatableRunnable.
     */
    
    /**
     * This method tries to acquire task's lock:
     * - if it succeeds, does the cancellation if needed,
     * - if it does not succeed, eventually interrupts the worker, as specified.
     * 
     * If worker gets interrupted, it might be currently running any of the methods
     * onBegin(), onEnd(), onDone(), and runImpl(...).
     * 
     * @param mayInterruptIfRunning True if runner might be interrupted in an attempt
     *        to quicken the cancellation, false otherwise.
     * @return True if the task is cancelled upon completion of this method,
     *         false otherwise (but then it might get cancelled later by a worker thread
     *         that notices that cancellation has been requested, and that current or next
     *         schedule should therefore be cancelled).
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        this.requestCancellation();
        
        final Lock lock = this.lock;
        final boolean didLock = lock.tryLock();
        if (didLock) {
            try {
                return this.doCancelIfNeeded_locked();
            } finally {
                lock.unlock();
            }
        } else {
            // run() (or onCancel()) method of this class is being executed.
            // As we don't want to wait for it to finish before calling onCancel()
            // ourselves, we will let it call onCancel() itself once it's done.
            // We eventually help it by interrupting it, if we detect it might not have
            // stopped to execute run() method, and is not calling onCancel() method.
            if (mayInterruptIfRunning) {
                this.interruptProcessRunnerIfRunning();
            }
            return false;
        }
    }
    
    /**
     * Method for soft cancellation: does not attempt to actually
     * cancel in current thread, but just indicates that cancellation is
     * requested, which leads to actual cancellation if worker thread reads it
     * before attempting to execute.
     */
    public void requestCancellation() {
        this.cancellationRequested = true;
    }
    
    /**
     * @return True if requestCancellation() has ever been called,
     *         either directly or from cancel(boolean) or onCancel().
     */
    public boolean isCancellationRequested() {
        return this.cancellationRequested;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Used for reschedules, after user called setNextTheoreticalTimeNs(...).
     * 
     * This default implementation uses scheduler.executeAtNs(this, timeNs).
     * 
     * Can be overridden for example if wanting to schedule
     * not this task directly, but a wrapper,
     * or use another scheduling method.
     */
    protected void rescheduleTaskAtNs(long timeNs) {
        this.getScheduler().executeAtNs(this, timeNs);
    }

    /*
     * 
     */

    /**
     * Called before first call to runImpl(...), if any.
     * 
     * Requesting cancellation or calling cancel(boolean) in this method
     * ensures that runImpl(...) won't be called next,
     * and ensures a call to onEnd() and then onDone().
     * 
     * Default implementation does nothing.
     */
    protected void onBegin() {
    }
    
    /**
     * Called after last call to runImpl(...), if any:
     * - in a call to run(), after runImpl(...) completed normally
     *   without asking for repetition or did throw an exception,
     * - in a call to onCancel(), if a call to onBegin() has been initiated.
     * 
     * Cancellation status is up to date when this method is called,
     * so you can check it in it, to know whether or not this method
     * is called due to a cancellation.
     * 
     * Default implementation does nothing.
     */
    protected void onEnd() {
    }
    
    /**
     * Called:
     * - in a call to run(), after call to onEnd(),
     * - in a call to onCancel(), after call to onEnd() if any.
     * 
     * Cancellation status is up to date when this method is called,
     * so you can check it in it, to know whether or not this method
     * is called due to a cancellation.
     * 
     * Default implementation does nothing.
     */
    protected void onDone() {
    }

    /**
     * Implement your run() in it.
     * 
     * Not called if cancellation has been requested.
     * 
     * Call setNextTheoreticalTimeNs(...) from within this method
     * for a new call to be scheduled for the specified time.
     * 
     * Note that for first call, if setNextTheoreticalTimeNs(...) has not
     * properly been called already, theoretical time will be set with
     * actual time.
     * 
     * @param theoreticalTimeNs Theoretical time, in nanoseconds, for which
     *        this call was scheduled.
     * @param actualTimeNs Actual time, in nanoseconds, at which this call
     *        occurs.
     */
    protected abstract void runImpl(long theoreticalTimeNs, long actualTimeNs);
    
    /*
     * Helper methods for next theoretical time computation.
     * Final so that can be safely used in constructors.
     */
    
    /**
     * Useful to add two nanoseconds durations, or time and duration,
     * without wrapping due to overflow.
     * 
     * @return The closest long to the mathematical sum of v1 and v2.
     */
    protected final long plusBounded(long v1, long v2) {
        return NumbersUtils.plusBounded(v1, v2);
    }
    
    /**
     * Useful to subtract two nanoseconds durations, or time and duration,
     * without wrapping due to overflow.
     * 
     * @return The closest long to the mathematical difference between v1 and v2.
     */
    protected final long minusBounded(long v1, long v2) {
        return NumbersUtils.minusBounded(v1, v2);
    }
    
    /**
     * @param s Time or duration in seconds.
     * @return The specified time or duration, in nanoseconds.
     */
    protected final long sToNs(double s) {
        return TimeUtils.sToNs(s);
    }
    
    /**
     * @param ns Time or duration in nanoseconds.
     * @return The specified time or duration, in seconds.
     */
    protected final double nsToS(long ns) {
        return TimeUtils.nsToS(ns);
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For tests.
     * Not thread-safe.
     */
    long getTheoreticalTimeNs() {
        return this.repRunnable.getTheoreticalTimeNs();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private Object getRunnerThreadNullificationMutex() {
        return this.repRunnable;
    }
    
    private boolean run_locked() {
        if(AZZERTIONS)LangUtils.azzert(this.runnerThread == null);
        this.runnerThread = Thread.currentThread();
        
        boolean mustRepeat = false;
        try {
            this.repRunnable.run();
            mustRepeat = this.repRunnable.isTheoreticalTimeSet();
        } finally {
            this.nullifyRunnerThread();
            
            // Clearing interruption status, in case of interruption by cancel(boolean) method,
            // for it not to propagate further, before calling onCancel() method.
            // Interruption status might also have been set in run() method, for whatever
            // reason, in which case we still clear it.
            Thread.interrupted();
            
            if (mustRepeat
                    && this.isCancellationRequested()) {
                /*
                 * Doing cancellation now, in case not already done
                 * by the thread that requested cancellation
                 * (typically due to failing to acquire the lock).
                 */
                this.repRunnable.onCancel();
                mustRepeat = false;
            }
        }
        return mustRepeat;
    }

    private void nullifyRunnerThread() {
        synchronized (this.getRunnerThreadNullificationMutex()) {
            this.runnerThread = null;
        }
    }
    
    private void interruptProcessRunnerIfRunning() {
        synchronized (this.getRunnerThreadNullificationMutex()) {
            final Thread runner = this.runnerThread;
            if (runner != null) {
                runner.interrupt();
            }
        }
    }
    
    private boolean doCancelIfNeeded_locked() {
        if (this.isTerminatingOrDone()) {
            // Already terminating.
            return false;
        }
        this.onCancel();
        return true;
    }
}
