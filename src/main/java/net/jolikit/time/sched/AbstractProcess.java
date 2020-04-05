/*
 * Copyright 2019-2020 Jeff Hain
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

import net.jolikit.lang.ExceptionsUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.TimeUtils;

/**
 * Similar to AbstractRepeatableTask, but it can be started and stopped at will.
 * For this class, repetition is the rule rather than the special case,
 * and to reflect this its API is a bit different than the one of
 * AbstractRepeatableTask (and of AbstractRepeatableRunnable):
 * - "void runImpl(...)" is replaced with "long process(...)",
 *   theoretical time for next schedule being the return value
 *   (harder to forget than a call to setNextTheoreticalTimeNs(...)).
 * - "process" more or less implies the notion of repetition,
 *   so "repeatable" is not added to the class name.
 * - start() and stop() methods replace the facts of submitting to a scheduler
 *   and then eventually canceling current schedule if any.
 * 
 * Protected onBegin(), onEnd() and process(...) methods are not called from
 * within start() and stop() methods, but asynchronously using the specified
 * scheduler.
 * 
 * start() and stop() methods are thread-safe, and not blocking other
 * than due to scheduler's call, which is not supposed to block for long.
 */
public abstract class AbstractProcess {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean AZZERTIONS = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyRepTask extends AbstractRepeatableTask {
        /**
         * Set on new task, then read in lock.
         */
        private MyRepTask previousToStop;
        public MyRepTask(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        public MyRepTask(
                InterfaceScheduler scheduler,
                Lock lock) {
            super(
                    scheduler,
                    lock);
        }
        @Override
        protected void rescheduleTaskAtNs(long timeNs) {
            rescheduleCurrentTaskAtNs(this, timeNs);
        }
        @Override
        protected void onBegin() {
            boolean tryCompletedNormally = false;
            try {
                // Making sure the past is buried before starting the present.
                this.cancelPreviousIfNeeded();
                tryCompletedNormally = true;
            } finally {
                try {
                    AbstractProcess.this.onBegin();
                } catch (Throwable t) {
                    // NB: Could reproduce the potential issue
                    // when this is not done, but only after
                    // billions of start/stop calls in related
                    // concurrent test.
                    ExceptionsUtils.swallowIfTryThrew(t, tryCompletedNormally);
                }
            }
        }
        @Override
        protected void onEnd() {
            AbstractProcess.this.onEnd();
        }
        @Override
        protected void onDone() {
            // Needed if cancelled before call to onBegin(),
            // in which case onBegin() is not called.
            this.cancelPreviousIfNeeded();
        }
        @Override
        protected void runImpl(long theoreticalTimeNs, long actualTimeNs) {
            final long nextNs = AbstractProcess.this.process(
                    theoreticalTimeNs,
                    actualTimeNs);
            if (this.isCancellationRequested()) {
                // Not setting next theoretical time,
                // which could cause a new call.
            } else {
                this.setNextTheoreticalTimeNs(nextNs);
            }
        }
        private void cancelPreviousIfNeeded() {
            final MyRepTask pts = this.previousToStop;
            if (pts != null) {
                this.previousToStop = null;
                if (!pts.isDone()) {
                    pts.cancelPreviousAndThisIfNeeded();
                }
            }
        }
        private void cancelPreviousAndThisIfNeeded() {
            if(AZZERTIONS)LangUtils.azzert(this.isCancellationRequested());
            boolean tryCompletedNormally = false;
            try {
                this.cancelPreviousIfNeeded();
                tryCompletedNormally = true;
            } finally {
                try {
                    super.onCancel();
                } catch (Throwable t) {
                    // NB: Can't seem to test this call,
                    // but code looks good.
                    ExceptionsUtils.swallowIfTryThrew(t, tryCompletedNormally);
                }
            }
        }
    }

    private class MyStopCancellable implements InterfaceCancellable {
        private volatile MyRepTask toCancel;
        private final boolean mayInterruptIfRunning;
        public MyStopCancellable(
                final MyRepTask toCancel,
                final boolean mayInterruptIfRunning) {
            this.toCancel = toCancel;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
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
            final MyRepTask toCancel = this.toCancel;
            // Testing for nullity, but should not need to,
            // since process(...) and onCancel() calls should be exclusive.
            if (toCancel != null) {
                // Interrupting even if task's onEnd() or onDone() are being
                // called, because it might be due to an exception
                // (and not cancellation).
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
     * Lock to prevent concurrent usage of runnables.
     */
    private final Lock lock;

    private final Object startStopMutex = new Object();
    
    /**
     * Last started task.
     * Written in start stop mutex.
     * Volatile for readability outside start stop mutex.
     */
    private volatile MyRepTask lastStartedTask;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractProcess(InterfaceScheduler scheduler) {
        this(
                scheduler,
                new ReentrantLock());
    }

    /**
     * @param lock The lock to guard calls to abstract methods.
     */
    public AbstractProcess(
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
        final MyRepTask startTask = newStartTaskOrNullIfStartPending();
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
        final MyRepTask startTask = newStartTaskOrNullIfStartPending();
        if (startTask != null) {
            final InterfaceScheduler scheduler = this.getScheduler();
            
            final long nowNs = scheduler.getClock().getTimeNs();
            final long timeNs = plusBounded(nowNs, delayNs);
            
            startTask.setNextTheoreticalTimeNs(timeNs);
            scheduler.executeAtNs(startTask, timeNs);
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
        final MyRepTask oldStartedTask;
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
         * to avoid possible deadlock with our lock,
         * if runnable is rejected (onCancel()
         * called by current thread).
         * 
         * NB: If stop() is called from within process(...),
         * we don't need do to that.
         * Could add a requestCancellation() or weakStop() method
         * that would not do it (but then, cancellation flag would
         * no longer indicate that we did this "stop" schedule,
         * which some of this class code assumes).
         */
        this.scheduler.execute(new MyStopCancellable(oldStartedTask, mayInterruptIfRunning));
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
        final MyRepTask task = this.lastStartedTask;
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
        final MyRepTask task = this.lastStartedTask;
        return (task != null)
                && (!task.isCancellationRequested())
                && (!task.isTerminatingOrDone());
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Used for reschedules, after process(...) calls.
     * 
     * This default implementation uses scheduler.executeAtNs(task, timeNs).
     * 
     * Can be overridden for example if wanting to schedule
     * not the task directly, but a wrapper,
     * or use another scheduling method.
     */
    protected void rescheduleCurrentTaskAtNs(Runnable task, long timeNs) {
        this.getScheduler().executeAtNs(task, timeNs);
    }

    /*
     * 
     */
    
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
     * - process(...) did throw an exception.
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
    private MyRepTask newStartTaskOrNullIfStartPending() {
        final MyRepTask newStartedTask;
        
        synchronized (this.startStopMutex) {
            final MyRepTask oldStartedTask = this.lastStartedTask;
            if ((oldStartedTask != null)
                    && (!oldStartedTask.isCancellationRequested())
                    && oldStartedTask.isPending()) {
                // Start still pending: nothing to schedule.
                newStartedTask = null;
            } else {
                newStartedTask = new MyRepTask(
                        this.scheduler,
                        this.lock);
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
