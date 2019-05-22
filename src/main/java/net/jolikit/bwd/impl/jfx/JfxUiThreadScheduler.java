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
package net.jolikit.bwd.impl.jfx;

import java.lang.Thread.UncaughtExceptionHandler;

import javafx.application.Platform;
import net.jolikit.bwd.impl.utils.sched.AbstractUiThreadScheduler;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.time.sched.SchedUtils;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Uses JavaFX Application Thread as UI thread.
 */
public class JfxUiThreadScheduler extends AbstractUiThreadScheduler {

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
    public JfxUiThreadScheduler(
            HardClockTimeType timingSchedulerHardClockTimeType,
            UncaughtExceptionHandler exceptionHandler) {
        this(
                newTimingScheduler(
                        timingSchedulerHardClockTimeType,
                        "JfxUI-timing",
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
    public JfxUiThreadScheduler(
            HardScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler) {
        super(
                timingScheduler,
                exceptionHandler);
        this.timingScheduler = timingScheduler;
    }
    
    @Override
    public boolean isWorkerThread() {
        return Platform.isFxApplicationThread();
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
        
        Platform.runLater(runnable);
    }
}
