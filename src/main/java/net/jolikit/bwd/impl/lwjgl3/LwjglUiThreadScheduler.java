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
package net.jolikit.bwd.impl.lwjgl3;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.impl.utils.basics.BindingError;
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

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

public class LwjglUiThreadScheduler extends AbstractScheduler implements InterfaceWorkerAwareScheduler {
    
    /*
     * http://www.glfw.org/docs/latest/intro.html#thread_safety
     * http://forum.lwjgl.org/index.php?topic=5827.15
     */
    
    static {
        /*
         * Unused stuffs.
         */
        if (false) {
            GLFW.glfwPollEvents();
            
            final double timeoutS = 1.0;
            GLFW.glfwWaitEventsTimeout(timeoutS);
        }
    }
    
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
            return (Thread.currentThread() == uiThread);
        }
        @Override
        protected void runLater(Runnable runnable) {
            runLaterImpl(runnable);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Thread uiThread;

    /**
     * TODO lwjgl We need a dummy window, for GLFW.glfwWaitEvents() to work
     * even if the user doesn't have a window up.
     */
    private final long dummyWindowHandle;
    
    private final HardScheduler timingScheduler;
    private final UncaughtExceptionHandler exceptionHandler;
    
    private final MyUiScheduler uiScheduler;
    
    /**
     * Guarded by synchronization on itself.
     */
    private final List<Runnable> runnableList = new ArrayList<Runnable>();
    
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
    public LwjglUiThreadScheduler(
            HardClockTimeType timingSchedulerHardClockTimeType,
            UncaughtExceptionHandler exceptionHandler) {
        this(
                AbstractUiThreadScheduler.newTimingScheduler(
                        timingSchedulerHardClockTimeType,
                        "JfxUI-timing",
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
    public LwjglUiThreadScheduler(
            HardScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler) {
        
        this.uiThread = Thread.currentThread();
        
        this.exceptionHandler = LangUtils.requireNonNull(exceptionHandler);
        
        this.uiScheduler = new MyUiScheduler(
                timingScheduler,
                exceptionHandler);
        this.timingScheduler = (HardScheduler) this.uiScheduler.getTimingScheduler();

        this.dummyWindowHandle = newDummyHiddenWindow();
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
        
        while (true) {
            if (DEBUG) {
                Dbg.log("glfwWaitEvents...");
            }
            // Executes already posted events synchronously,
            // eventually waiting if there are none yet,
            // and then returns.
            try {
                GLFW.glfwWaitEvents();
            } catch (Throwable t) {
                this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
            }
            if (DEBUG) {
                Dbg.log("...glfwWaitEvents");
            }
            
            if (this.timingScheduler.isShutdown()) {
                /*
                 * Happens on shutdown.
                 */
                if (DEBUG) {
                    Dbg.log("break (shutdown)");
                }
                break;
            }
            
            final List<Runnable> currentList;
            synchronized (this.runnableList) {
                currentList = new ArrayList<Runnable>(this.runnableList);
                this.runnableList.clear();
            }
            
            for (Runnable runnable : currentList) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }
        }
    }

    /**
     * We want to be brutal and be a "shutdownNow", as a way to ensure
     * no schedule for later will be executed and try to push a SDL event,
     * which might succeed if SDL has been re-initialized at that time,
     * and cause interferences between successive bindings.
     */
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
        
        // When waking up, we will notice that timing scheduler
        // has been shut down, and will stop looping on glfwWaitEvents().
        GLFW.glfwPostEmptyEvent();
        
        Callbacks.glfwFreeCallbacks(this.dummyWindowHandle);
        GLFW.glfwDestroyWindow(this.dummyWindowHandle);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void runLaterImpl(Runnable runnable) {
        if (this.timingScheduler.isShutdown()) {
            // Shutting down.
            CancellableUtils.call_onCancel_IfCancellable(runnable);
            return;
        }
        
        synchronized (this.runnableList) {
            this.runnableList.add(runnable);
        }
        
        GLFW.glfwPostEmptyEvent();
    }
    
    private static long newDummyHiddenWindow() {
        GLFW.glfwDefaultWindowHints();
        // Hidden by default.
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
 
        final int width = 1;
        final int height = 1;
 
        final long monitorHandle = MemoryUtil.NULL;
        final long shareHandle = MemoryUtil.NULL;
        final long windowHandle = GLFW.glfwCreateWindow(width, height, "Hello World!", monitorHandle, shareHandle);
        if (windowHandle == MemoryUtil.NULL) {
            throw new BindingError("could not create window");
        }
        return windowHandle;
    }
}
