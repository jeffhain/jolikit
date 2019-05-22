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
package net.jolikit.bwd.impl.qtj4;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.impl.utils.sched.AbstractUiThreadScheduler;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.bwd.impl.utils.sched.UiThreadChecker;
import net.jolikit.lang.Dbg;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.sched.AbstractScheduler;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.SchedUtils;
import net.jolikit.time.sched.hard.HardScheduler;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.gui.QApplication;

public class QtjUiThreadScheduler extends AbstractScheduler implements InterfaceWorkerAwareScheduler {

    /*
     * Using QCoreApplication.invokeLater(Runnable).
     * 
     * TODO qtj Tried using QCoreApplication.postEvent(QObject,QEvent) instead,
     * but then we got:
     * # A fatal error has been detected by the Java Runtime Environment:
     * #  EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x000007fef7d3eff0, pid=4744, tid=0x00000000000014bc
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

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
            if (false) {
                // Should also work.
                final QCoreApplication app = QCoreApplication.instance();
                if ((app == null) || (app.nativeId() == 0L)) {
                    // QCoreApplication not (yet/still) ready.
                    return false;
                }
                return (Thread.currentThread() == app.thread());
            } else {
                return (Thread.currentThread() == uiThread);
            }
        }
        @Override
        protected void runLater(Runnable runnable) {
            runLaterImpl(runnable);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final int BARRIER_VALUE = 0;
    
    /*
     * 
     */
    
    private final Thread uiThread;
    
    private final HardScheduler timingScheduler;

    private final MyUiScheduler uiScheduler;

    /**
     * Barrier to ensure visibility of runnable that we posted through Qt,
     * at the time and in the thread we retrieve them.
     * Might be useless, but better be paranoid.
     * Not private on purpose.
     */
    volatile int barrier = BARRIER_VALUE;

    private final AtomicBoolean processXxxUntilShutdownAlreadyCalled = new AtomicBoolean(false);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * UI thread is set to be the constructing thread.
     * 
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     */
    public QtjUiThreadScheduler(
            HardClockTimeType timingSchedulerHardClockTimeType,
            UncaughtExceptionHandler exceptionHandler) {
        this(
                AbstractUiThreadScheduler.newTimingScheduler(
                        timingSchedulerHardClockTimeType,
                        "QtjUI-timing",
                        exceptionHandler),
                        exceptionHandler);
    }
    
    /**
     * UI thread is set to be the constructing thread.
     * 
     * Useful if you want to specify the HardScheduler to use
     * as timing scheduler.
     * 
     * @param timingScheduler Scheduler used to wait for timed schedules.
     *        Cf. AbstractUiThreadScheduler.newTimingScheduler(...)
     *        helper method to create a timing scheduler.
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     */
    public QtjUiThreadScheduler(
            HardScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler) {
        this.uiThread = Thread.currentThread();
        
        this.uiScheduler = new MyUiScheduler(
                timingScheduler,
                exceptionHandler);
        this.timingScheduler = (HardScheduler) this.uiScheduler.getTimingScheduler();
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
        return this.timingScheduler.getClock();
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
        if (this.timingScheduler.isShutdown()) {
            // Already shut down.
            return;
        }
        
        if (DEBUG) {
            Dbg.log("QApplication.execStatic()...");
        }
        QApplication.execStatic();
        if (DEBUG) {
            Dbg.log("...QApplication.execStatic()");
            Dbg.log("QApplication.shutdown()...");
        }
        QApplication.shutdown();
        if (DEBUG) {
            Dbg.log("...QApplication.shutdown()");
        }
    }

    public void shutdownNow() {
        
        final boolean mustInterruptWorkingWorkers = false;
        this.timingScheduler.shutdownNow(mustInterruptWorkingWorkers);
        
        if (DEBUG) {
            Dbg.log("QApplication.shutdown()...");
        }
        QApplication.shutdown();
        if (DEBUG) {
            Dbg.log("...QApplication.shutdown()");
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void runLaterImpl(Runnable runnable) {
        if (this.timingScheduler.isShutdown()) {
            // Shutting down.
            SchedUtils.call_onCancel_IfCancellable(runnable);
            return;
        }
        
        /*
         * TODO qtj If application is not "ready",
         * invokeLater(...) might have no effect, silently
         * (runnable not put in the QInvokable).
         */
        if ((QCoreApplication.instance() == null)
                || (QCoreApplication.instance().nativeId() == 0L)) {
            SchedUtils.call_onCancel_IfCancellable(runnable);
            if (false) {
                throw new IllegalStateException("QCoreApplication not (yet/still) ready");
            } else {
                /*
                 * We prefer to be quiet, for it can happen on shutdown.
                 */
                return;
            }
        }
        
        // Volatile write for visibility.
        this.barrier = BARRIER_VALUE;
        
        QCoreApplication.invokeLater(runnable);
    }
}
