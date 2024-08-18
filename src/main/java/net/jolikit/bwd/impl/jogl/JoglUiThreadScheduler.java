/*
 * Copyright 2019-2024 Jeff Hain
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
package net.jolikit.bwd.impl.jogl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jogamp.newt.Display;
import com.jogamp.opengl.Threading;

import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.sched.AbstractUiThreadScheduler;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.basics.CancellableUtils;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Uses NEWT's display's Event Dispatch Thread as UI thread.
 */
public class JoglUiThreadScheduler extends AbstractUiThreadScheduler {
    
    /*
     * TODO jogl Threading with NEWT + JOGL can be pretty complicated,
     * since there can be multiple threads involved, and not always
     * in the same way:
     * - NEWT:
     *   "NEWT's event model is pretty simple.
     *   It spawns one Event Dispatch Thread (EDT) for each unique Display
     *   which role is to handle:
     *   - input events
     *   - window lifecycle actions (window visibility, resize, .. etc)
     *   - NOT rendering".
     * - JOGL:
     *   If com.jogamp.opengl.Threading.isSingleThreaded() is true,
     *   OpenGL rendering must be done in some OpenGL thread,
     *   using com.jogamp.opengl.Threading.invokeOnOpenGLThread(...),
     *   else it can be done in another thread.
     * ===> What we do:
     * - We use a single display, to have a single NEWT EDT,
     *   and use it as UI thread.
     * - We call com.jogamp.opengl.Threading.disableSingleThreading() on
     *   binding creation, for com.jogamp.opengl.Threading.isSingleThreaded()
     *   to be false and not have to deal with an OpenGL thread,
     *   and be able to do OpenGL rendering in NEWT EDT (our UI thread).
     * ===> NB:
     * - If com.jogamp.opengl.Threading.isSingleThreaded() is true,
     *   we could manage to do rendering in OpenGL thread instead of UI thread:
     *   this should improve performances, but would also increase overall
     *   CPU consumption, as well as code complexity, so for both simplicity
     *   and reduced overhead, and because this binding is quite fast already
     *   without this "optimization", we prefer not to do it.
     * - For us, in practice, com.jogamp.opengl.Threading.isSingleThreaded()
     *   has always been false, so code dealing with it being true
     *   has never been tested.
     * - Initially, this binding was designed with using its creator thread
     *   as UI thread, and taking care to switch to it before processing
     *   events from NEWT EDT, or before processing GL listeners calls
     *   done in NEWT EDT (in which case we just ensured an ASAP repaint
     *   in UI thread).
     *   This could have caused trouble, for example due to concurrent
     *   window modification from NEWT EDT.
     *   There might still be some workarounds that are no longer needed
     *   now that everything is done in NEWT EDT, but it should not hurt
     *   to keep them around, especially if for some reason we would like
     *   for example to use eventual OpenGL thread as UI thread.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    /**
     * Need that due to EDTUtil.invoke(...) executing synchronously if called
     * from EDT, which we don't want, for execute(...) to always
     * (and consistently) be asynchronous.
     */
    private static final boolean MUST_CALL_invoke_FROM_TIMING_THREAD = true;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Display display;
    
    private final HardScheduler timingScheduler;
    
    private final AtomicBoolean processXxxUntilShutdownAlreadyCalled = new AtomicBoolean(false);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     */
    public JoglUiThreadScheduler(
            HardClockTimeType timingSchedulerHardClockTimeType,
            UncaughtExceptionHandler exceptionHandler,
            Display display,
            double newtEdtPollPeriodS) {
        this(
                newTimingScheduler(
                        timingSchedulerHardClockTimeType,
                        "JoglUI-timing",
                        exceptionHandler),
                        exceptionHandler,
                        display,
                        newtEdtPollPeriodS);
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
    public JoglUiThreadScheduler(
            HardScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler,
            Display display,
            double newtEdtPollPeriodS) {
        super(
                timingScheduler,
                        exceptionHandler,
                        MUST_CALL_invoke_FROM_TIMING_THREAD);
        this.timingScheduler = timingScheduler;
        
        Threading.disableSingleThreading();
        final boolean isJoglSingleThreaded = Threading.isSingleThreaded();
        if (isJoglSingleThreaded) {
            // Letting user know that it didn't work.
            throw new BindingError("could not disable JOGL single threading");
        }

        this.display = LangUtils.requireNonNull(display);
        
        {
            final long pollPeriodMs =
                    TimeUtils.sToMillisNoUnderflow(newtEdtPollPeriodS);
            display.getEDTUtil().setPollPeriod(pollPeriodMs);
        }

        if (display.getEDTUtil().isRunning()) {
            if (DEBUG) {
                Dbg.log("NEWT EDT : already running");
            }
        } else {
            /*
             * NB: Sometimes passing here, sometimes not.
             */
            if (DEBUG) {
                Dbg.log("NEWT EDT : starting it");
            }
            display.getEDTUtil().start();
        }
        
        if (DEBUG) {
            if (display != null) {
                Dbg.log("EDTUtil.getPollPeriod() = " + display.getEDTUtil().getPollPeriod());
            }
        }
    }
    
    /*
     * 
     */

    @Override
    public boolean isWorkerThread() {
        return display.getEDTUtil().isCurrentThreadEDT();
    }
    
    /*
     * 
     */

    /**
     * Must NOT be called in UI thread.
     * 
     * @throws IllegalStateException if current thread is UI thread.
     *         or if has already been called.
     */
    public void waitUntilShutdownUninterruptibly() {
        this.checkIsNotWorkerThread();
        if (!this.processXxxUntilShutdownAlreadyCalled.compareAndSet(false, true)) {
            throw new IllegalStateException("already called");
        }
        if (this.timingScheduler.isShutdown()) {
            // Already shut down.
            return;
        }

        /*
         * Waiting until shut down, while the UI thread does its work.
         */
        while (true) {
            if (this.timingScheduler.isShutdown()) {
                /*
                 * Happens on shutdown.
                 */
                if (DEBUG) {
                    Dbg.log("break (shutdown)");
                }
                break;
            }

            try {
                Thread.sleep(1L);
            } catch (InterruptedException ignore) {
                // We ignore interrupt.
            }
        }
    }

    public void shutdownNow() {
        final boolean mustInterruptWorkingWorkers = false;
        this.timingScheduler.shutdownNow(mustInterruptWorkingWorkers);

        final boolean wait = false;
        final boolean didIt = this.display.getEDTUtil().invokeStop(wait, new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    Dbg.log("display's EDT shut down");
                }
            }
        });
        if (!didIt) {
            // Fine, that just means it's already down.
            if (DEBUG) {
                Dbg.log("display's EDT already shut down");
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void runLater(Runnable runnable) {
        if (this.timingScheduler.isShutdown()) {
            // Shutting down.
            CancellableUtils.call_onCancel_IfCancellable(runnable);
            return;
        }
        
        final boolean wait = false;

        final boolean didIt = this.display.getEDTUtil().invoke(wait, runnable);
        if (!didIt) {
            if (DEBUG) {
                Dbg.log("EDTUtil.invoke(...) failed");
            }
            /*
             * Can happen on shutdown.
             */
            CancellableUtils.call_onCancel_IfCancellable(runnable);
        }
    }
}
