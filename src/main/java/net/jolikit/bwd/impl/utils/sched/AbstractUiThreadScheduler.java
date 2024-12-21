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
package net.jolikit.bwd.impl.utils.sched;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

import net.jolikit.lang.DefaultThreadFactory;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.basics.CancellableUtils;
import net.jolikit.threading.basics.InterfaceCancellable;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.sched.AbstractScheduler;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Optional helper class to implement a worker aware scheduler
 * executing in UI thread, when already having a method to run runnables
 * asynchronously in UI thread, and a method to compute whether current thread
 * is UI thread.
 * 
 * Timing executor thread is not UI thread, and the only foreign code
 * that it calls or can call are runLater(Runnable) method of this class
 * and onCancel() method of cancellable runnables.
 * runLater(Runnable) and onCancel() can also be called in UI thread.
 */
public abstract class AbstractUiThreadScheduler extends AbstractScheduler implements InterfaceWorkerAwareScheduler {

    /*
     * There is no rejected execution handler for
     * EventQueue.invokeLater(Runnable), so we only consider that a Runnable is
     * rejected if invokeLater(Runnable) throws.
     */
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Does not use exception handler: to handle thrown exceptions,
     * specify an exception handler for timing scheduler.
     */
    private class MyTimingThreadRunnable implements InterfaceCancellable {
        final Runnable runnable;
        public MyTimingThreadRunnable(Runnable runnable) {
            this.runnable = runnable;
        }
        @Override
        public void run() {
            wrapAndCallRunLater(this.runnable);
        }
        @Override
        public void onCancel() {
            CancellableUtils.call_onCancel_IfCancellable(this.runnable);
        }
    }
    
    /**
     * For ASAP (from user or timing thread) execution (in UI thread) of a runnable.
     * Uses exception handler if user code throws.
     */
    private class MyRunnableWrapper implements InterfaceCancellable {
        private final Runnable runnable;
        public MyRunnableWrapper(Runnable runnable) {
            this.runnable = runnable;
        }
        @Override
        public void run() {
            try {
                this.runnable.run();
            } catch (Throwable t) {
                exceptionHandler.uncaughtException(Thread.currentThread(), t);
            }
        }
        @Override
        public void onCancel() {
            try {
                CancellableUtils.call_onCancel_IfCancellable(this.runnable);
            } catch (Throwable t) {
                exceptionHandler.uncaughtException(Thread.currentThread(), t);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * For timed schedules, and eventually ASAP schedules.
     */
    private final InterfaceScheduler timingScheduler;
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    private final boolean mustCallRunLaterFromTimingThread;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Calls runLater(Runnable) from user thread, not necessarily timing thread.
     * 
     * @param timingScheduler Scheduler used to wait for timed (non-ASAP)
     *        schedules, and eventually to call runLater(...) ASAP.
     *        Cf. newTimingScheduler(...) helper method to create
     *        a timing scheduler.
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     */
    public AbstractUiThreadScheduler(
            InterfaceScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler) {
        this(
                timingScheduler,
                exceptionHandler,
                false); // mustCallRunLaterFromTimingThread
    }
    
    /**
     * Note that if using a soft scheduler as timing scheduler, it must use
     * UI thread as soft thread (else soft thread and UI thread would interfere
     * with each other), so you might as well want to use your soft scheduler
     * directly, but in case this could help (possibly due to backing usage of
     * EventQueue.invokeLater(Runnable)), we allow for the specified timing
     * scheduler to be soft (i.e. have a soft clock).
     * 
     * @param timingScheduler Scheduler used to wait for timed (non-ASAP)
     *        schedules, and eventually to call runLater(...) ASAP.
     *        Cf. newTimingScheduler(...) helper method to create
     *        a timing scheduler.
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     * @param mustCallRunLaterFromTimingThread Useful if runLater(...) must
     *        never be called in UI thread (such as JOGL's
     *        Threading.invokeOnOpenGLThread(...) method).
     */
    public AbstractUiThreadScheduler(
            InterfaceScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler,
            boolean mustCallRunLaterFromTimingThread) {
        this.timingScheduler = LangUtils.requireNonNull(timingScheduler);
        this.exceptionHandler = LangUtils.requireNonNull(exceptionHandler);
        this.mustCallRunLaterFromTimingThread = mustCallRunLaterFromTimingThread;
    }
    
    /*
     * 
     */

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
        LangUtils.requireNonNull(runnable);
        
        if (this.mustCallRunLaterFromTimingThread) {
            final MyTimingThreadRunnable cancellable =
                    new MyTimingThreadRunnable(runnable);
            this.timingScheduler.execute(cancellable);
        } else {
            wrapAndCallRunLater(runnable);
        }
    }

    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        LangUtils.requireNonNull(runnable);
        // Need a wrapper which will be called by scheduler once time is
        // reached, and then will delegate actual runnable execution to
        // UI thread.
        final MyTimingThreadRunnable cancellable =
                new MyTimingThreadRunnable(runnable);
        this.timingScheduler.executeAtNs(cancellable, timeNs);
    }

    /*
     * 
     */
    
    /**
     * Helper method to create a timing scheduler.
     */
    public static HardScheduler newTimingScheduler(
            HardClockTimeType timingSchedulerHardClockTimeType,
            String timingSchedulerThreadNamePrefix,
            UncaughtExceptionHandler exceptionHandler) {
        
        final InterfaceHardClock timingSchedulerClock =
                BindingSchedUtils.newClock(timingSchedulerHardClockTimeType);
        
        final boolean timingDaemon = true;
        
        /*
         * exceptionHandler useful for timing scheduler
         * in case runLater(Runnable) or onCancel() throw
         * in timing thread.
         */
        final ThreadFactory timingThreadFactory = new DefaultThreadFactory(
                null,
                exceptionHandler);
        
        final HardScheduler timingScheduler = HardScheduler.newSingleThreadedInstance(
                timingSchedulerClock,
                timingSchedulerThreadNamePrefix,
                timingDaemon,
                timingThreadFactory);
        
        return timingScheduler;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Method used to get runnables to execute in UI thread, for both ASAP
     * and timed schedules.
     */
    protected abstract void runLater(Runnable runnable);
    
    protected InterfaceScheduler getTimingScheduler() {
        return this.timingScheduler;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * If runLater(Runnable) threw, if it's a RejectedExecutionException
     * it's re-thrown, else calls onCancel() or throws REE depending on whether
     * the specified runnable is a cancellable.
     */
    private void wrapAndCallRunLater(
            final Runnable runnable) {
        final Runnable runnableWrapper = new MyRunnableWrapper(runnable);
        
        RejectedExecutionException ree = null;
        boolean completedNormally = false;
        try {
            this.runLater(runnableWrapper);
            completedNormally = true;
        } catch (RejectedExecutionException e) {
            ree = e;
            throw e;
        } finally {
            if (!completedNormally) {
                if (ree != null) {
                    // Rejection already taken care of.
                } else {
                    CancellableUtils.call_onCancel_IfCancellableElseThrowREE(runnable);
                }
            }
        }
    }
}
