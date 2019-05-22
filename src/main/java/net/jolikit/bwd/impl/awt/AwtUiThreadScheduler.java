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
package net.jolikit.bwd.impl.awt;

import java.awt.EventQueue;
import java.lang.Thread.UncaughtExceptionHandler;

import net.jolikit.bwd.impl.utils.sched.AbstractUiThreadScheduler;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.time.sched.SchedUtils;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Uses EDT as worker thread.
 */
public class AwtUiThreadScheduler extends AbstractUiThreadScheduler {

    /*
     * Might be able to handle some EventQueue.invokeLater(...) related errors
     * using
     * Thread.setDefaultUncaughtExceptionHandler(...)
     * and/or
     * System.setProperty("sun.awt.exception.handler", ...) (removed in JDK 7),
     * but we don't want to set static things just for our binding.
     * We let user do it on their side if they want to.
     */
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final HardScheduler timingScheduler;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     */
    public AwtUiThreadScheduler(
            HardClockTimeType timingSchedulerHardClockTimeType,
            UncaughtExceptionHandler exceptionHandler) {
        this(
                newTimingScheduler(
                        timingSchedulerHardClockTimeType,
                        "AwtUI-timing",
                        exceptionHandler),
                        exceptionHandler);
    }
    
    /**
     * Useful if you want to specify the HardScheduler to use
     * as timing scheduler.
     * 
     * @param timingScheduler Scheduler used to wait for timed schedules.
     *        Cf. AbstractUiThreadScheduler.newTimingScheduler(...)
     *        helper method to create a timing scheduler.
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     */
    public AwtUiThreadScheduler(
            HardScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler) {
        super(
                timingScheduler,
                exceptionHandler);
        this.timingScheduler = timingScheduler;
    }
    
    @Override
    public boolean isWorkerThread() {
        return EventQueue.isDispatchThread();
    }
    
    public void shutdownAbruptly() {
        final boolean mustInterruptWorkingWorkers = false;
        this.timingScheduler.shutdownNow(mustInterruptWorkingWorkers);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void runLater(Runnable runnable) {
        if (this.timingScheduler.isShutdown()) {
            // Shutting down.
            SchedUtils.call_onCancel_IfCancellable(runnable);
            return;
        }
        
        EventQueue.invokeLater(runnable);
    }
}
