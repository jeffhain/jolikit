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
package net.jolikit.bwd.impl.swt;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import net.jolikit.bwd.impl.utils.sched.AbstractUiThreadScheduler;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.bwd.impl.utils.sched.UiThreadChecker;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.basics.CancellableUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.sched.AbstractScheduler;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.hard.HardScheduler;

public class SwtUiThreadScheduler extends AbstractScheduler implements InterfaceWorkerAwareScheduler {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SPAM = DEBUG && false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyUiScheduler extends AbstractUiThreadScheduler {
        public MyUiScheduler(
                HardScheduler timingScheduler,
                UncaughtExceptionHandler exceptionHandler) {
            super(
                    timingScheduler,
                    exceptionHandler);
        }
        @Override
        protected InterfaceScheduler getTimingScheduler() {
            return super.getTimingScheduler();
        }
        @Override
        public boolean isWorkerThread() {
            return (Thread.currentThread() == display.getThread());
        }
        @Override
        protected void runLater(Runnable runnable) {
            runLaterImpl(runnable);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Display display;
    private final UncaughtExceptionHandler exceptionHandler;
    
    private final HardScheduler timingScheduler;
    
    private final MyUiScheduler uiScheduler;

    private final AtomicBoolean processXxxUntilShutdownAlreadyCalled = new AtomicBoolean(false);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     */
    public SwtUiThreadScheduler(
            HardClockTimeType timingSchedulerHardClockTimeType,
            UncaughtExceptionHandler exceptionHandler,
            Display display) {
        this(
                AbstractUiThreadScheduler.newTimingScheduler(
                        timingSchedulerHardClockTimeType,
                        "SwtUI-timing",
                        exceptionHandler),
                        exceptionHandler,
                        display);
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
    public SwtUiThreadScheduler(
            HardScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler,
            Display display) {
        this.exceptionHandler = LangUtils.requireNonNull(exceptionHandler);
        
        this.uiScheduler = new MyUiScheduler(
                timingScheduler,
                exceptionHandler);
        this.timingScheduler = (HardScheduler) this.uiScheduler.getTimingScheduler();
        
        this.display = LangUtils.requireNonNull(display);
    }

    /*
     * 
     */

    @Override
    public boolean isWorkerThread() {
        return this.uiScheduler.isWorkerThread();
    }
    
    @Override
    public void checkIsWorkerThread() {
        UiThreadChecker.checkIsUiThread(this);
    }

    @Override
    public void checkIsNotWorkerThread() {
        UiThreadChecker.checkIsNotUiThread(this);
    }
    
    /*
     * 
     */
    
    @Override
    public InterfaceClock getClock() {
        return this.uiScheduler.getClock();
    }

    @Override
    public void execute(Runnable runnable) {
        this.uiScheduler.execute(runnable);
    }

    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        this.uiScheduler.executeAtNs(runnable, timeNs);
    }

    /*
     * 
     */
    
    /**
     * Must be called in UI thread.
     * 
     * @throws ConcurrentModificationException if current thread is not UI thread.
     * @throws IllegalStateException if has already been called.
     */
    public void processUntilShutdownUninterruptibly() {
        this.checkIsWorkerThread();
        if (!this.processXxxUntilShutdownAlreadyCalled.compareAndSet(false, true)) {
            throw new IllegalStateException("already called");
        }

        while (!this.timingScheduler.isShutdown()) {
            /*
             * In case of exception, readAndDispatch() throws either the exception itself,
             * or SWTException, so we need to catch and keep going here.
             * Ex.:
             * Exception in thread "main" org.eclipse.swt.SWTException: Failed to execute runnable (...)
             *     at org.eclipse.swt.SWT.error(Unknown Source)
             *     at org.eclipse.swt.SWT.error(Unknown Source)
             *     at org.eclipse.swt.widgets.Synchronizer.runAsyncMessages(Unknown Source)
             *     at org.eclipse.swt.widgets.Display.runAsyncMessages(Unknown Source)
             *     at org.eclipse.swt.widgets.Display.readAndDispatch(Unknown Source)
             */
            // True by default not to sleep if throws.
            boolean gotMore = true;
            try {
                gotMore = this.display.readAndDispatch();
            } catch (Throwable t) {
                if (t instanceof SWTException) {
                    // Extracting the actual exception.
                    t = t.getCause();
                }
                this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
            }
            if (!gotMore) {
                try {
                    // Sleeps until we have things to do.
                    this.display.sleep();
                } catch (NullPointerException e) {
                    /*
                     * TODO swt On Mac, in benches, can have:
                     * java.lang.NullPointerException
                     * at org.eclipse.swt.widgets.Widget.drawRect(Unknown Source)
                     * at org.eclipse.swt.widgets.Canvas.drawRect(Unknown Source)
                     * at org.eclipse.swt.widgets.Display.windowProc(Unknown Source)
                     * at org.eclipse.swt.internal.cocoa.OS.objc_msgSend_bool(Native Method)
                     * at org.eclipse.swt.internal.cocoa.NSRunLoop.runMode(Native Method)
                     * at org.eclipse.swt.widgets.Display.sleep(Unknown Source)
                     */
                }
            }
        }
    }

    public void shutdownNow() {
        
        /*
         * Making sure timing thread will terminate.
         * 
         * Doing it before sending poison pill to UI thread,
         * so that UI thread can detect shut down when
         * processing the poison pill (if we're ever called
         * out of UI thread here).
         */
        
        final boolean mustInterruptWorkingWorkers = false;
        this.timingScheduler.shutdownNow(mustInterruptWorkingWorkers);
        
        /*
         * Making sure UI thread will terminate.
         */
        
        this.execute(new Runnable() {
            @Override
            public void run() {
                /*
                 * Nothing to do, we just wanted to wake up the eventual sleep,
                 * so that timing scheduler shut down can be checked and loop
                 * exited.
                 */
            }
        });
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void runLaterImpl(Runnable runnable) {
        if (this.timingScheduler.isShutdown()) {
            // Shutting down.
            CancellableUtils.call_onCancel_IfCancellableElseThrowREE(runnable);
            return;
        }
        /*
         * TODO swt Display.asyncExec(...) throws SWTException
         * ("ERROR_DEVICE_DISPOSED")
         * if the receiver has been disposed, so we check it,
         * even though isDisposed() might not be thread-safe
         * (we might be in the timing thread here).
         * 
         * Separate if, so that the call stack can tell
         * where the cancellation comes from.
         */
        if (this.display.isDisposed()) {
            CancellableUtils.call_onCancel_IfCancellableElseThrowREE(runnable);
            return;
        }

        if (DEBUG_SPAM) {
            Dbg.logPr(this, "Display.asyncExec(" + runnable + ")...");
        }
        try {
            /*
             * TODO swt In SWT.java:
             * "NOTE: Exceptions thrown in syncExec and asyncExec must be wrapped."
             * Not sure what that means.
             */
            this.display.asyncExec(runnable);
        } finally {
            if (DEBUG_SPAM) {
                Dbg.logPr(this, "...Display.asyncExec(" + runnable + ")");
            }
        }
    }
}
