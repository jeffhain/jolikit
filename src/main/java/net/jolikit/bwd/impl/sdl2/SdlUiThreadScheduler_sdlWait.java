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
package net.jolikit.bwd.impl.sdl2;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Event;
import net.jolikit.bwd.impl.sdl2.jlib.SdlEventType;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_UserEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaUtils;
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

/**
 * Corresponds to "solution 2" described in SdlUiThreadScheduler.
 * 
 * Scheduler that uses SDL threading primitives to wait for
 * schedules or SDL events.
 */
public class SdlUiThreadScheduler_sdlWait extends AbstractScheduler implements InterfaceWorkerAwareScheduler {
    
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

    private static final int BARRIER_VALUE = 0;
    
    /*
     * 
     */
    
    /**
     * User event generated when an ASAP or timed schedule must be processed.
     */
    private static final int MY_SCHED_PROCESS_EVENT_TYPE = SdlEventType.SDL_USEREVENT.intValue();
    
    /**
     * User event generated when must shut down.
     */
    private static final int MY_POISON_PILL_EVENT_TYPE = SdlEventType.SDL_USEREVENT.intValue() + 1;
    
    /*
     * 
     */

    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;

    private final Thread uiThread;

    private final AtomicBoolean processXxxUntilShutdownAlreadyCalled = new AtomicBoolean(false);
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    private final HardScheduler timingScheduler;
    
    private final MyUiScheduler uiScheduler;
    
    /**
     * Codes should become unused before reuse, since the collection in which
     * runnables are stored has a capacity < int period.
     */
    private final AtomicInteger nextRunnableKey = new AtomicInteger();

    /**
     * Guarded by synchronization on itself.
     */
    private final Map<Integer,Runnable> runnableByKey = new HashMap<Integer,Runnable>();

    /**
     * Barrier to ensure visibility of user SDL event values that we posted,
     * at the time and in the thread we retrieve them.
     * Might be useless due to using synchronization around runnableByKey,
     * but better be paranoid.
     * Not private on purpose.
     */
    volatile int barrier = BARRIER_VALUE;
    
    private final InterfaceSdlEventListener sdlEventListener;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * UI thread is set to be the constructing thread.
     * 
     * @param exceptionHandler For exceptions thrown from timing or UI thread.
     *        Must not be null.
     * @param sdlEventListener Listener for SDL events, except the ones internal
     *        to this scheduler.
     */
    public SdlUiThreadScheduler_sdlWait(
            HardClockTimeType timingSchedulerHardClockTimeType,
            UncaughtExceptionHandler exceptionHandler,
            InterfaceSdlEventListener sdlEventListener) {
        this(
                AbstractUiThreadScheduler.newTimingScheduler(
                        timingSchedulerHardClockTimeType,
                        "SdlUI-timing",
                        exceptionHandler),
                        exceptionHandler,
                        sdlEventListener);
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
     * @param sdlEventListener Listener for SDL events, except the ones internal
     *        to this scheduler.
     */
    public SdlUiThreadScheduler_sdlWait(
            HardScheduler timingScheduler,
            UncaughtExceptionHandler exceptionHandler,
            InterfaceSdlEventListener sdlEventListener) {
        this.uiThread = Thread.currentThread();
        
        this.exceptionHandler = LangUtils.requireNonNull(exceptionHandler);
        
        this.uiScheduler = new MyUiScheduler(
                timingScheduler,
                exceptionHandler);
        this.timingScheduler = (HardScheduler) this.uiScheduler.getTimingScheduler();
        
        this.sdlEventListener = sdlEventListener;
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
        if (!this.isWorkerThread()) {
            throw new ConcurrentModificationException("must be called in UI thread");
        }
        if (!this.processXxxUntilShutdownAlreadyCalled.compareAndSet(false, true)) {
            throw new IllegalStateException("already called");
        }
        if (this.timingScheduler.isShutdown()) {
            // Already shut down.
            return;
        }
        
        final SDL_Event eventUnion = new SDL_Event();
        while (true) {
            if (DEBUG) {
                Dbg.log("SDL_WaitEvent...");
            }
            final int ret = LIB.SDL_WaitEvent(eventUnion);
            if (DEBUG) {
                Dbg.log("...SDL_WaitEvent ret = " + ret);
            }
            final boolean gotSome = (ret == 1);
            if (!gotSome) {
                if (this.timingScheduler.isShutdown()) {
                    /*
                     * Happens on shutdown.
                     */
                    if (DEBUG) {
                        Dbg.log("break (shutdown, ret = 0)");
                    }
                    break;
                } else {
                    if (DEBUG) {
                        Dbg.log("break (throw)");
                    }
                    throw new BindingError("SDL_WaitEvent : ret = " + ret);
                }
            }
            
            final int type = eventUnion.type();
            if (DEBUG) {
                Dbg.log("event type = " + eventUnion.toStringType() + " : " + type);
            }
            
            if (type == MY_SCHED_PROCESS_EVENT_TYPE) {
                final SDL_UserEvent.ByValue scheduleEvent = SdlJnaUtils.getUserEvent(eventUnion);
                // This is always a simple runnable,
                // user runnable is within it.
                final Runnable runnable = getRunnable(scheduleEvent);
                
                try {
                    runnable.run();
                } catch (Throwable t) {
                    this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
                
            } else if (type == MY_POISON_PILL_EVENT_TYPE) {
                if (DEBUG) {
                    Dbg.log("break (poison pill)");
                }
                break;
                
            } else {
                try {
                    this.sdlEventListener.onEvent(eventUnion);
                } catch (Throwable t) {
                    this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }
        }
    }

    /**
     * We want to be brutal and be a "shutdownNow", as a way to ensure
     * no schedule for later will be executed and try to push an SDL event,
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
         * 
         * NB: This poison event is useless in practice if calling SDL_Quit()
         * after current method, which we do on binding shut down, and which
         * causes SDL_WaitEvent(...) to return with no event, in which case we
         * detect the shut down. But we use it anyway for consistency, in case
         * at some point we want to shut down this scheduler without quitting
         * SDL.
         */
        
        final int code = 0; // Not used, whatever fits.
        final SDL_Event eventUnion = newUserEventUnion(
                MY_POISON_PILL_EVENT_TYPE,
                code);
        pushEventUnion(eventUnion);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static SDL_Event newUserEventUnion(
            int userEventType,
            int code) {
        final SDL_Event eventUnion = new SDL_Event();
        eventUnion.setType(SDL_UserEvent.ByValue.class);
        final SDL_UserEvent.ByValue userEvent = SdlJnaUtils.getUserEvent(eventUnion);
        // Only need to set type, with a valid user event type.
        userEvent.type = userEventType;
        userEvent.code = code;
        return eventUnion;
    }
    
    private void runLaterImpl(Runnable runnable) {
        if (this.timingScheduler.isShutdown()) {
            // Shutting down.
            CancellableUtils.call_onCancel_IfCancellable(runnable);
            return;
        }
        
        final int runnableKey = this.nextRunnableKey.getAndIncrement();
        final SDL_Event eventUnion = newUserEventUnion(
                MY_SCHED_PROCESS_EVENT_TYPE,
                runnableKey);
        
        synchronized (this.runnableByKey) {
            this.runnableByKey.put(runnableKey, runnable);
        }
        
        // Volatile write for visibility.
        this.barrier = BARRIER_VALUE;
        
        // If fails, throws, in which case super class will take care of calling
        // onCancel() if our runnable is a cancellable.
        pushEventUnion(eventUnion);
    }

    /**
     * @throws IllegalStateException if fails.
     */
    private static void pushEventUnion(SDL_Event eventUnion) {
        final int ret = LIB.SDL_PushEvent(eventUnion);
        if (ret != 1) {
            throw new BindingError("ret = " + ret);
        }
    }
    
    private Runnable getRunnable(SDL_UserEvent scheduleEvent) {
        // Volatile read for visibility.
        if (this.barrier != BARRIER_VALUE) {
            throw new BindingError();
        }

        final int runnableKey;
        final Runnable runnable;
        synchronized (this.runnableByKey) {
            runnableKey = scheduleEvent.code;
            runnable = this.runnableByKey.remove(runnableKey);
        }
        if (runnable == null) {
            throw new BindingError("no runnable for key " + runnableKey);
        }
        
        return runnable;
    }
}
