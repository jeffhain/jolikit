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

import net.jolikit.lang.LangUtils;

/**
 * A runnable which provides its theoretical and actual execution times,
 * makes it easy to ask for its re-schedule at a new specified theoretical time
 * using setNextTheoreticalTimeNs(...), keeps track of its current life cycle,
 * and notifies of its life cycle changes through hook methods.
 * 
 * Allows for multiple schedules until its state is "done".
 * 
 * Must not be used for multiple concurrent executions, as its state is tied
 * to at most a single schedule and is not guarded, but its isXxx() state
 * reading methods can be used concurrently and are non-blocking.
 */
public abstract class AbstractRepeatableRunnable implements InterfaceCancellable {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int STATUS_FIRST_RUN_PENDING = 0;
    private static final int STATUS_ON_BEGIN_CALL_INITIATED = 1;
    private static final int STATUS_REPEATING = 2;
    
    private static final int STATUS_ON_END_CALL_INITIATED = 3;
    private static final int STATUS_ON_DONE_CALL_INITIATED = 4;
    private static final int STATUS_DONE = 5;
    
    /*
     * Using minus "regular" status, which allows to use absolute value in if's,
     * and not to use another volatile variable to store cancellation
     * information.
     */
    
    private static final int STATUS_ON_END_CALL_INITIATED_CANCELLED = -STATUS_ON_END_CALL_INITIATED;
    private static final int STATUS_ON_DONE_CALL_INITIATED_CANCELLED = -STATUS_ON_DONE_CALL_INITIATED;
    private static final int STATUS_DONE_CANCELLED = -STATUS_DONE;
    
    private final InterfaceScheduler scheduler;
    
    /**
     * Status volatile to be readable concurrently
     * (otherwise, it would not need to be).
     */
    private volatile int status = STATUS_FIRST_RUN_PENDING;
    
    /**
     * Whether theoreticalTimeNs value is set.
     * 
     * In practice, must be set for timed schedules or re-schedules,
     * is not set for ASAP schedules, and is unset before calls
     * to runImpl(...), within which user must re-set it
     * to cause a re-schedule.
     * 
     * Volatile to avoid issue in case of concurrent reads.
     */
    private volatile boolean isTheoreticalTimeSet = false;
    
    /**
     * Theoretical time for next call to runImpl(...), in nanoseconds.
     */
    private long theoreticalTimeNs;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractRepeatableRunnable(InterfaceScheduler scheduler) {
        this.scheduler = LangUtils.requireNonNull(scheduler);
    }
    
    /*
     * 
     */

    public InterfaceScheduler getScheduler() {
        return this.scheduler;
    }
    
    /*
     * 
     */
    
    /**
     * To be called before submitting this runnable to a scheduler,
     * for first call to runImpl(...) to use proper theoretical time,
     * and from runImpl(...) to request a new execution.
     * 
     * @param nextTheoreticalTimeNs Theoretical time, in nanoseconds,
     *        at which next call to runImpl(...) must occur.
     */
    public void setNextTheoreticalTimeNs(long nextTheoreticalTimeNs) {
        this.setTheoreticalTimeNs(nextTheoreticalTimeNs);
    }

    /**
     * Must be called again if it did set a next theoretical time
     * in the last set scheduling, except if it throws an exception,
     * in which case it must not be called again.
     * 
     * If termination (call to onEnd() and onDone()) has been initiated already,
     * just returns false.
     * 
     * Overriding implementations must call super.
     */
    @Override
    public void run() {
        // Could also just check "Math.abs(this.status) == STATUS_DONE",
        // but this way we are robust to bad usage (process(...) should
        // only be called by scheduler, and is not supposed to be called
        // concurrently with onCancel()).
        if (Math.abs(this.status) >= STATUS_ON_END_CALL_INITIATED) {
            return;
        }
        
        // Theoretical time retrieved before call to onBegin(),
        // in case user changes it in onBegin(), which must have no effect.
        final boolean initialTheoTimeSet = this.isTheoreticalTimeSet();
        final long initialTheoTimeNs;
        if (initialTheoTimeSet) {
            initialTheoTimeNs = this.getTheoreticalTimeNs();
        } else {
            initialTheoTimeNs = Long.MIN_VALUE;
        }
        
        boolean mustRepeat = false;
        try {
            if (this.status == STATUS_FIRST_RUN_PENDING) {
                this.status = STATUS_ON_BEGIN_CALL_INITIATED;
                this.onBegin();
                if (this.status == STATUS_DONE_CANCELLED) {
                    // User called onCancel() in onBegin().
                    // Clearing theoretical time for consistency,
                    // in case user ever set it during onBegin() call.
                    this.clearTheoreticalTime();
                    return;
                }
                this.status = STATUS_REPEATING;
            }

            final long actualTimeNs = this.scheduler.getClock().getTimeNs();
            final long theoreticalTimeNs;
            if (initialTheoTimeSet) {
                theoreticalTimeNs = initialTheoTimeNs;
            } else {
                theoreticalTimeNs = actualTimeNs;
            }
            
            this.clearTheoreticalTime();

            this.runImpl(theoreticalTimeNs, actualTimeNs);
            
            mustRepeat = this.isTheoreticalTimeSet();
        } finally {
            if (mustRepeat) {
                if (this.status == STATUS_DONE_CANCELLED) {
                    // User called onCancel() in runImpl(...).
                    mustRepeat = false;
                    // Clearing theoretical time for consistency.
                    this.clearTheoreticalTime();
                }
            } else {
                if (this.status == STATUS_DONE_CANCELLED) {
                    // User called onCancel() in onBegin() or runImpl(...).
                    // Done already.
                } else {
                    final boolean dueToCancellation = false;
                    this.terminateIfNeeded(dueToCancellation);
                }
            }
        }
        if (mustRepeat) {
            final long nextNs = this.getTheoreticalTimeNs();
            this.rescheduleAtNs(nextNs);
        }
        return;
    }
    
    /**
     * After call to this method, this repeatable runnable is in done state.
     * If termination (call to onEnd() and onDone()) has been initiated already,
     * calling this method does nothing.
     * 
     * Overriding implementations must call super.
     */
    @Override
    public void onCancel() {
        final boolean dueToCancellation = true;
        this.terminateIfNeeded(dueToCancellation);
    }

    /*
     * life cycle methods
     */
    
    /**
     * Makes this object ready for a new sequence of schedules.
     * Must not be called concurrently with actual usage of this runnable.
     */
    public void reset() {
        this.status = STATUS_FIRST_RUN_PENDING;
        this.isTheoreticalTimeSet = false;
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onBegin() call has not yet been initiated, false otherwise.
     */
    public boolean isPending() {
        return this.status == STATUS_FIRST_RUN_PENDING;
    }
    
    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onBegin() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnBeginBeingCalled() {
        return this.status == STATUS_ON_BEGIN_CALL_INITIATED;
    }
    
    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if runImpl(...) is being called or if a call to it
     *         is expected (i.e. after a call to onBegin() that completed
     *         normally and did not cancel), false otherwise.
     */
    public boolean isRepeating() {
        return this.status == STATUS_REPEATING;
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onEnd() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnEndBeingCalled() {
        return Math.abs(this.status) == STATUS_ON_END_CALL_INITIATED;
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onDone() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnDoneBeingCalled() {
        return Math.abs(this.status) == STATUS_ON_DONE_CALL_INITIATED;
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if onDone() call has detectably finished, false otherwise.
     */
    public boolean isDone() {
        return Math.abs(this.status) == STATUS_DONE;
    }

    /**
     * Thread-safe and non-blocking.
     * 
     * @return True if termination (call to onEnd() and then onDone(), or just
     *         onDone()) has been initiated, false otherwise.
     */
    public boolean isTerminatingOrDone() {
        return Math.abs(this.status) >= STATUS_ON_END_CALL_INITIATED;
    }
    
    /**
     * Thread-safe and non-blocking.
     * 
     * Note: A repeatable runnable can be cancelled but not yet done.
     * 
     * @return True if this repeatable runnable is being cancelled (i.e. is being
     *         terminated due to cancellation), or has been cancelled and is
     *         done, false otherwise.
     */
    public boolean isCancelled() {
        return this.status < 0;
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
     * not this runnable directly, but a wrapper,
     * or use another scheduling method.
     */
    protected void rescheduleAtNs(long timeNs) {
        this.scheduler.executeAtNs(this, timeNs);
    }

    /*
     * 
     */

    /**
     * Called before first call to runImpl(...), if any.
     * 
     * Calling onCancel() in this method ensures that runImpl(...)
     * won't be called next,
     * and ensures a call to onEnd() and then onDone().
     * 
     * clearNextTheoreticalTime() is called just after this,
     * to make sure that the only theoretical time used
     * for re-scheduling is the one set from within runImpl(...).
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
     * Not called if onCancel() has been called.
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
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Clears theoretical time information.
     */
    void clearTheoreticalTime() {
        this.isTheoreticalTimeSet = false;
    }
    
    /**
     * @return Whether theoretical time is set.
     */
    boolean isTheoreticalTimeSet() {
        return this.isTheoreticalTimeSet;
    }

    /**
     * @param theoreticalTimeNs Theoretical time, in nanoseconds,
     *        at which next call to runImpl(...) must occur.
     */
    void setTheoreticalTimeNs(long theoreticalTimeNs) {
        this.theoreticalTimeNs = theoreticalTimeNs;
        this.isTheoreticalTimeSet = true;
    }
    
    /**
     * @return Theoretical time, in nanoseconds,
     *         at which next call to runImpl(...) must occur.
     * @throws IllegalStateException if theoretical time is not set.
     */
    long getTheoreticalTimeNs() {
        if (!this.isTheoreticalTimeSet) {
            throw new IllegalStateException();
        }
        return this.theoreticalTimeNs;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * If termination has been initiated already, does nothing
     * (can happen if onCancel() is called twice, or is called
     * in onEnd() or onDone() (both of which are bad usages)).
     */
    private void terminateIfNeeded(boolean dueToCancellation) {
        if (Math.abs(this.status) >= STATUS_ON_END_CALL_INITIATED) {
            return;
        }
        
        try {
            // Not just == STATUS_REPEATING, in case canceling from onBegin().
            if (this.status >= STATUS_ON_BEGIN_CALL_INITIATED) {
                this.status = (dueToCancellation ? STATUS_ON_END_CALL_INITIATED_CANCELLED : STATUS_ON_END_CALL_INITIATED);
                this.onEnd();
            }
        } finally {
            this.status = (dueToCancellation ? STATUS_ON_DONE_CALL_INITIATED_CANCELLED : STATUS_ON_DONE_CALL_INITIATED);
            try {
                this.onDone();
            } finally {
                this.status = (dueToCancellation ? STATUS_DONE_CANCELLED : STATUS_DONE);
                
                // Clearing next theoretical time for consistency,
                // in case user ever set it during onBegin(),
                // runImpl(...), onEnd() or onDone() call.
                this.clearTheoreticalTime();
            }
        }
    }
}
