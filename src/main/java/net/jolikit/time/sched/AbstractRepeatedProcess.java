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
 * Handy class to implement a treatment executed repeatedly,
 * which execution can be stopped and started at will.
 * 
 * onBegin/onEnd/run methods are not called from
 * within start and stop methods, but in a schedulable.
 * Also, a mutex ensures main memory synchronization
 * for calls to onBegin/onEnd/run methods,
 * for safe use of multi-threaded schedulers.
 * 
 * start and stop methods are thread-safe, and not blocking other
 * than due to scheduler's call, which is not supposed
 * to block for long.
 */
public abstract class AbstractRepeatedProcess {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean AZZERTIONS = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyTask extends AbstractTask {
        /**
         * Set on new schedulable, then read in locker's lock.
         */
        private MyTask previousToStop;
        public MyTask(Lock lock) {
            super(lock);
        }
        @Override
        protected void onBegin() {
            try {
                // Making sure the past is buried before starting the present.
                this.cancelPreviousIfNeeded();
            } finally {
                AbstractRepeatedProcess.this.onBegin();
            }
        }
        @Override
        protected void onEnd() {
            AbstractRepeatedProcess.this.onEnd();
        }
        @Override
        protected void onDone() {
            // Needed if cancelled before call to onBegin().
            this.cancelPreviousIfNeeded();
        }
        @Override
        protected void runImpl() {
            /*
             * If scheduling is null, can be due to scheduler mistakenly wrapping
             * a schedulable with a simple runnable, and then not setting
             * scheduling properly.
             */
            final InterfaceScheduling scheduling = this.getScheduling();
            final long nextNs = AbstractRepeatedProcess.this.process(
                    scheduling.getTheoreticalTimeNs(),
                    scheduling.getActualTimeNs());
            if (this.isCancellationRequested()) {
                // Not setting next theoretical time,
                // which could cause a new call.
            } else {
                scheduling.setNextTheoreticalTimeNs(nextNs);
            }
        }
        private void cancelPreviousIfNeeded() {
            final MyTask pts = this.previousToStop;
            if (pts != null) {
                this.previousToStop = null;
                if (!pts.isDone()) {
                    pts.cancelPreviousAndThisIfNeeded();
                }
            }
        }
        private void cancelPreviousAndThisIfNeeded() {
            if(AZZERTIONS)LangUtils.azzert(this.isCancellationRequested());
            try {
                this.cancelPreviousIfNeeded();
            } finally {
                super.onCancel();
            }
        }
    }

    private class MyStopSchedulable implements InterfaceSchedulable {
        private volatile MyTask toCancel;
        private final boolean mayInterruptIfRunning;
        public MyStopSchedulable(
                final MyTask toCancel,
                final boolean mayInterruptIfRunning) {
            this.toCancel = toCancel;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
        }
        @Override
        public void setScheduling(InterfaceScheduling scheduling) {
            // Not used.
        }
        @Override
        public void run() {
            this.cancelIt();
        }
        @Override
        public void onCancel() {
            // Cancellation propagation.
            this.cancelIt();
        }
        private void cancelIt() {
            final MyTask toCancel = this.toCancel;
            // Testing if toStop is null, but should not need to,
            // since process(...) and onCancel() calls should be exclusive.
            if (toCancel != null) {
                // Interrupting even if in onEnd() or onDone(), because might be in these
                // treatments due to an exception (and not cancellation).
                toCancel.cancel(this.mayInterruptIfRunning);
                this.toCancel = null;
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final InterfaceScheduler scheduler;

    /**
     * Lock to prevent concurrent usage of schedulables.
     */
    private final Lock lock;

    private final Object startStopMutex = new Object();
    
    /**
     * Last started task.
     * Written in start stop mutex.
     * Volatile for readability outside start stop mutex.
     */
    private volatile MyTask lastStartedTask;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractRepeatedProcess(InterfaceScheduler scheduler) {
        this(
                scheduler,
                new ReentrantLock());
    }

    /**
     * @param locker The locker to guard calls to abstract methods.
     */
    public AbstractRepeatedProcess(
            final InterfaceScheduler scheduler,
            final Lock lock) {
        this.scheduler = LangUtils.requireNonNull(scheduler);
        this.lock = lock;
    }

    public InterfaceScheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * Thread-safe.
     * 
     * Starts or restarts repetition ASAP.
     * Does nothing if the first process(...) call is still pending
     * from a previous call to any start method.
     * 
     * First call to process(...) will have theoretical time
     * identical to actual time.
     */
    public void start() {
        final MyTask startTask = newStartTaskOrNullIfStartPending();
        if (startTask != null) {
            this.scheduler.execute(startTask);
        }
    }

    /**
     * Thread-safe.
     * 
     * Starts or restart repetition after the specified delay.
     * Does nothing if the first process(...) call is still pending
     * from a previous call to any start method.
     * As a result, if you want to reset the delay for the first
     * process(...) call, you must first call stop(...) before,
     * or just use start() instead and use a "firstCall" boolean
     * (reset to true in onBegin()) to special-case your treatments
     * in process(...).
     * 
     * First call to process(...) will have theoretical time
     * corresponding to the time the last effective call to
     * startAfterNs(...) was done.
     * 
     * Compared to start(), this method adds the overhead of
     * a clock time retrieval.
     * 
     * @param delayNs Delay before first subsequent call to process(...),
     *        in nanoseconds.
     */
    public void startAfterNs(long delayNs) {
        final MyTask startTask = newStartTaskOrNullIfStartPending();
        if (startTask != null) {
            this.scheduler.executeAfterNs(startTask, delayNs);
        }
    }

    /**
     * Convenience method.
     * Uses startAfterNs(...).
     * 
     * @param delayS Delay before first subsequent call to process(...),
     *        in seconds.
     */
    public void startAfterS(double delayS) {
        final long delayNs = TimeUtils.sToNs(delayS);
        this.startAfterNs(delayNs);
    }

    /**
     * Equivalent to stop(false).
     */
    public void stop() {
        this.stop(false);
    }

    /**
     * Thread-safe.
     * Stops repetition if any, as well as any pending call to process(...)
     * due to a call to any start method.
     */
    public void stop(boolean mayInterruptIfRunning) {
        final MyTask oldStartedTask;
        synchronized (this.startStopMutex) {
            oldStartedTask = this.lastStartedTask;
            if ((oldStartedTask == null)
                    || oldStartedTask.isCancellationRequested()
                    || oldStartedTask.isTerminatingOrDone()) {
                // Nothing to stop, or already stopped, or stopping.
                return;
            }
            // Keeping reference to this old task, for eventual subsequently
            // started tasks to stop it themselves, if it has not
            // already been done.
            oldStartedTask.requestCancellation();
        }
        /*
         * Need to schedule outside startStopMutex,
         * to avoid possible deadlock with locker,
         * if schedulable is rejected (onCancel() called by current thread).
         * 
         * NB: If stop() is called from within process(...),
         * we don't need do to that.
         * Could add a requestCancellation() or weakStop() method
         * that would not do it (but then, cancellation flag would
         * no longer indicate that we did this schedule).
         */
        this.scheduler.execute(new MyStopSchedulable(oldStartedTask, mayInterruptIfRunning));
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if has been started and is not yet done, i.e. if we are between
     *         a pending onBegin() call, and onEnd() not yet finished, or if stop()
     *         has been called soon enough that previously pending onBegin() call
     *         won't even be executed but we don't know it yet, false otherwise.
     */
    public boolean isAlive() {
        final MyTask task = this.lastStartedTask;
        return (task != null) && (!task.isDone());
    }

    /**
     * A started treatment is necessarily alive, but an alive
     * treatment is not started if it stop has been requested.
     * 
     * Thread-safe and non-blocking.
     * 
     * @return True if has been started and stop not yet requested,
     *         i.e. if process(...) method is to be called repeatedly,
     *         false otherwise.
     */
    public boolean isStarted() {
        final MyTask task = this.lastStartedTask;
        return (task != null)
                && (!task.isCancellationRequested())
                && (!task.isTerminatingOrDone());
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Called before first call to process(...),
     * i.e. after a call to start() method of this class.
     * 
     * If this method throws an exception, repetition is aborted,
     * and onEnd() is called.
     * 
     * Default implementation does nothing.
     */
    protected void onBegin() {
    }

    /**
     * Called if and only if onBegin() has been called,
     * and after last call to process(...) if any,
     * if either of the following events occur:
     * - stop(...) or start() have been called,
     * - process(...) did throw an exception or returned Long.MAX_VALUE.
     * 
     * This method might be called directly after onBegin(),
     * without intermediary call to process(...),
     * if stop(...) has been called soon enough.
     * 
     * Default implementation does nothing.
     */
    protected void onEnd() {
    }

    /**
     * Implement your process here.
     * 
     * To cancel repetition in this method, just call stop(),
     * after which the returned value will be discarded.
     * 
     * @return Next theoretical execution time, in nanoseconds.
     */
    protected abstract long process(long theoreticalTimeNs, long actualTimeNs);
    
    /*
     * Helper methods for next theoretical time computation.
     * Final so that can be safely used in constructors.
     */
    
    /**
     * Useful to add two nanoseconds durations, or time and duration,
     * without wrapping due to overflow, in particular when one of
     * the values is a configured duration, which could be huge.
     * 
     * @return The closest long to the mathematical sum of v1 and v2.
     */
    protected final long plusBounded(long v1, long v2) {
        return NumbersUtils.plusBounded(v1, v2);
    }
    
    /**
     * Useful to subtract two nanoseconds durations, or time and duration,
     * without wrapping due to overflow, in particular when one of
     * the values is a configured duration, which could be huge.
     * 
     * @return The closest long to the mathematical difference between v1 and v2.
     */
    protected final long minusBounded(long v1, long v2) {
        return NumbersUtils.minusBounded(v1, v2);
    }
    
    /**
     * @param ns Time or duration in nanoseconds.
     * @return The specified time or duration, in seconds.
     */
    protected final double nsToS(long ns) {
        return TimeUtils.nsToS(ns);
    }
    
    /**
     * @param s Time or duration in seconds.
     * @return The specified time or duration, in nanoseconds.
     */
    protected final long sToNs(double s) {
        return TimeUtils.sToNs(s);
    }

    /**
     * Similar to sToNs(double), but when the time or duration
     * is inferior to the nanosecond, but different from +-0.0,
     * returns 1 (if positive) or -1 (if negative) instead of 0.
     * 
     * Useful to avoid treatments becoming busy due to a too small
     * configured period.
     * 
     * @param s Time or duration in seconds.
     * @return The specified time or duration, in nanoseconds.
     */
    protected final long sToNsNoUnderflow(double s) {
        return TimeUtils.sToNsNoUnderflow(s);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * NB: You need to schedule the returned task outside startStopMutex,
     * to avoid possible deadlock with lock, if task is rejected
     * (onCancel() called by current thread).
     */
    private MyTask newStartTaskOrNullIfStartPending() {
        final MyTask newStartedTask;
        
        synchronized (this.startStopMutex) {
            final MyTask oldStartedTask = this.lastStartedTask;
            if ((oldStartedTask != null)
                    && (!oldStartedTask.isCancellationRequested())
                    && oldStartedTask.isPending()) {
                // Start still pending: nothing to schedule.
                newStartedTask = null;
            } else {
                newStartedTask = new MyTask(this.lock);
                this.lastStartedTask = newStartedTask;
                
                if ((oldStartedTask != null) && (!oldStartedTask.isTerminatingOrDone())) {
                    oldStartedTask.requestCancellation();
                    newStartedTask.previousToStop = oldStartedTask;
                }
            }
        }
        
        return newStartedTask;
    }
}
