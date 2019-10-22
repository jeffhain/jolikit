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
 * Schedulable with methods called at different life cycle transitions,
 * and isXXX methods to read current life cycle.
 * 
 * Methods of this class must not be used concurrently,
 * alone or with each other, except isXXX methods, which
 * also are non-blocking.
 */
public abstract class AbstractSmartSchedulable implements InterfaceSchedulable {
    
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
    
    /**
     * Status volatile to be readable concurrently
     * (otherwise, it would not need to be).
     */
    private volatile int status = STATUS_FIRST_RUN_PENDING;

    private InterfaceScheduling scheduling;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractSmartSchedulable() {
    }
    
    /*
     * 
     */

    @Override
    public void setScheduling(InterfaceScheduling scheduling) {
        this.scheduling = scheduling;
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
        
        boolean mustRepeat = false;
        try {
            if (this.status == STATUS_FIRST_RUN_PENDING) {
                this.status = STATUS_ON_BEGIN_CALL_INITIATED;
                this.onBegin();
                if (this.status == STATUS_DONE_CANCELLED) {
                    // User called onCancel() in onBegin().
                    // Clearing next theoretical time for consistency,
                    // in case user ever set it during onBegin() call.
                    this.scheduling.clearNextTheoreticalTime();
                    return;
                }
                this.status = STATUS_REPEATING;
            }
            
            this.runImpl();
            
            mustRepeat = this.scheduling.isNextTheoreticalTimeSet();
        } finally {
            if (mustRepeat) {
                if (this.status == STATUS_DONE_CANCELLED) {
                    // User called onCancel() in runImpl().
                    // Clearing next theoretical time for consistency,
                    // in case user ever set it during runImpl() call.
                    this.scheduling.clearNextTheoreticalTime();
                }
            } else {
                if (this.status == STATUS_DONE_CANCELLED) {
                    // User called onCancel() in onBegin() or runImpl().
                    // Done already.
                } else {
                    final boolean dueToCancellation = false;
                    this.terminateIfNeeded(
                            dueToCancellation,
                            this.scheduling);
                }
            }
        }
        return;
    }
    
    /**
     * After call to this method, this schedulable is in done state.
     * If termination (call to onEnd() and onDone()) has been initiated already,
     * calling this method does nothing.
     * 
     * Overriding implementations must call super.
     */
    @Override
    public void onCancel() {
        final boolean dueToCancellation = true;
        // Not supposed to be set during cancellation,
        // so null even if accidentally set.
        final InterfaceScheduling scheduling = null;
        terminateIfNeeded(
                dueToCancellation,
                scheduling);
    }

    /*
     * life cycle methods
     */
    
    /**
     * Makes this object ready for a new sequence of schedules.
     * Must not be called concurrently with actual usage of this schedulable.
     */
    public void reset() {
        this.status = STATUS_FIRST_RUN_PENDING;
    }

    /**
     * @return True if onBegin() call has not yet been initiated, false otherwise.
     */
    public boolean isPending() {
        return this.status == STATUS_FIRST_RUN_PENDING;
    }
    
    /**
     * @return True if onBegin() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnBeginBeingCalled() {
        return this.status == STATUS_ON_BEGIN_CALL_INITIATED;
    }
    
    /**
     * @return True if runImpl() is being called or if a call to it
     *         is expected (i.e. after a call to onBegin() that completed
     *         normally and did not cancel), false otherwise.
     */
    public boolean isRepeating() {
        return this.status == STATUS_REPEATING;
    }

    /**
     * @return True if onEnd() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnEndBeingCalled() {
        return Math.abs(this.status) == STATUS_ON_END_CALL_INITIATED;
    }

    /**
     * @return True if onDone() call has been initiated, and has not yet
     *         detectably finished, false otherwise.
     */
    public boolean isOnDoneBeingCalled() {
        return Math.abs(this.status) == STATUS_ON_DONE_CALL_INITIATED;
    }

    /**
     * @return True if onDone() call has detectably finished, false otherwise.
     */
    public boolean isDone() {
        return Math.abs(this.status) == STATUS_DONE;
    }

    /**
     * @return True if termination (call to onEnd() and then onDone(), or just
     *         onDone()) has been initiated, false otherwise.
     */
    public boolean isTerminatingOrDone() {
        return Math.abs(this.status) >= STATUS_ON_END_CALL_INITIATED;
    }
    
    /**
     * Note: A schedulable can be cancelled but not yet done.
     * 
     * @return True if this schedulable is being cancelled (i.e. is being
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
     * Called before first call to runImpl(), if any.
     * 
     * Calling onCancel() in this method ensures that runImpl()
     * won't be called next,
     * and ensures a call to onEnd() and then onDone().
     * 
     * scheduling.clearNextTheoreticalTime() is called
     * just after this, to make sure the only next theoretical time used
     * is the one set from within runImpl().
     * 
     * Default implementation does nothing.
     */
    protected void onBegin() {
    }
    
    /**
     * Called after last call to runImpl(), if any:
     * - in a call to run(), after runImpl() completed normally
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
     * @return The scheduling for use in runImpl().
     */
    protected final InterfaceScheduling getScheduling() {
        return this.scheduling;
    }

    /**
     * Implement your run() in it.
     * 
     * Not called if onCancel() has been called.
     */
    protected abstract void runImpl();
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * If termination has been initiated already, does nothing
     * (can happen if onCancel() is called twice, or is called
     * in onEnd() or onDone() (both of which are bad usages)).
     */
    private void terminateIfNeeded(
            boolean dueToCancellation,
            InterfaceScheduling scheduling) {
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
                
                if (scheduling != null) {
                    // Clearing next theoretical time for consistency,
                    // in case user ever set it during onBegin(),
                    // runImpl(), onEnd() or onDone() call.
                    scheduling.clearNextTheoreticalTime();
                }
            }
        }
    }
}
