/*
 * Copyright 2019-2020 Jeff Hain
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
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.impl.sdl2.jlib.SDL_Event;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_UserEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SdlEventType;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.sched.BindingSchedUtils;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.bwd.impl.utils.sched.UiThreadChecker;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.AbstractScheduler;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Scheduler that does all user schedules using a hard scheduler,
 * and just uses polling for system SDL events, using a repeated process.
 */
public class SdlUiThreadScheduler extends AbstractScheduler implements InterfaceWorkerAwareScheduler {
    
    /*
     * Not to recreate a hard scheduler from scratch using SDL threading
     * primitives, we use a HardScheduler to handle most of the scheduling work.
     * Now we have two possible solutions:
     * 
     * 1) Have HardScheduler's thread be the UI thread, in which case we can't
     *    use SDL wait in it (because it would not be woken up by scheduler's
     *    internal signals), and have to use polling with some small period to
     *    process system SDL events.
     *    In this case, for user ASAP schedules we have again two possibilities:
     *    a) Generate user SDL events, and then wake up the wait for early
     *       polling, which would allow for events processing to be ordered
     *       according to events ideal scheduling (whatever the polling period).
     *    b) Just use hard scheduler's execute(Runnable) methods, which would
     *       ensure ASAP processing but possibly in inconsistent order with
     *       system SDL events (due to polling period), but it shouldn't hurt
     *       much since system events are already pooled under the hood.
     * 
     * 2) Have HardScheduler's thread be a "timing thread", and have UI thread
     *    blocking on SDL wait, and be woken up:
     *    - when a system SDL event is generated,
     *    - when an ASAP or timed schedule must be processed, due to a user
     *      SDL event being generated from the timing thread when appropriate.
     *    In this case, we need to have some user SDL "poison" event to be
     *    pushed on shut down (additionally to HardScheduler's shut down), so
     *    that the UI thread doesn't get stuck in SDL wait.
     * 
     * We would like to choose solution 2, for it both preserves
     * ordering between SDL events and ASAP schedules, and theoretically does
     * not delay processing for system SDL events, but it turns out that
     * SDL_WaitEvent(...) actually does polling, without wakeups,
     * even in case of user events, which would often cause a 10ms delay
     * for ASAP schedules.
     * 
     * Also, solution 1.a is both more complicated (code-wise and dynamic-wise),
     * and slower than solution 1.b (benched immediate ASAP re-schedule
     * of a same runnable at 0.3 millisecond for 1.a, instead of 1 microsecond
     * for solution 1.b).
     * 
     * As a result, we choose solution 1.b, but also keep solution 1.a
     * as dead code for educational purpose and as a fallback
     * in case there would be issues with 1.b.
     */

    /*
     * TODO sdl On Mac, we get this warning once at startup,
     * which is weird since AFAIK we only call SDL threading primitives
     * from main thread, which we use as UI thread.
     * "2019-01-26 13:06:42.968 java[541:22423] WARNING: nextEventMatchingMask should
     * only be called from the Main Thread! This will throw an exception in the future."
     * 
     * Cf. the issue "Workaround to run under MacOS"
     * (github/littlevgl/pc_simulator_sdl_eclipse).
     * 
     * Cf. also Allegro5 binding, for which we have a workaround
     * against that.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * True for first sub-solution, false for second one.
     */
    private static final boolean MUST_EXECUTE_ASAP_THROUGH_USER_SDL_EVENT = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MySchedInfo {
        final Runnable runnable;
        public MySchedInfo(Runnable runnable) {
            this.runnable = runnable;
        }
    }
    
    private class MyPollProcess extends AbstractProcess {
        final long pollPeriodNs;
        public MyPollProcess(
                InterfaceScheduler scheduler,
                double pollPeriodS) {
            super(scheduler);
            this.pollPeriodNs = sToNsNoUnderflow(pollPeriodS);
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            if (hardScheduler.isShutdown()) {
                // Shutting down.
                this.stop();
                return 0;
            }
            
            if (DEBUG) {
                Dbg.logPr(this, "process(" + theoreticalTimeNs + "," + actualTimeNs + ")");
            }
            pollEvents();
            
            /*
             * Using theoretical time to compute next time,
             * because this treatment should have some priority,
             * but still taking care not to schedule in the past,
             * to avoid lateness drift that could prevent other repetitions
             * and overbusiness once we can keep up again.
             */
            final long nextNs = Math.max(
                plusBounded(theoreticalTimeNs, this.pollPeriodNs),
                actualTimeNs);
            if (DEBUG) {
                Dbg.logPr(this, "nextNs = " + nextNs);
            }
            return nextNs;
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
    
    /*
     * 
     */

    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;

    private final Thread uiThread;
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    /**
     * This is the UI scheduler if and only if
     * MUST_EXECUTE_ASAP_THROUGH_USER_SDL_EVENT
     * is false.
     */
    private final HardScheduler hardScheduler;
    
    private final MyPollProcess pollProcess;
    private final InterfaceSdlEventListener sdlEventListener;
    
    /**
     * Codes should become unused before reuse, since the collection in which
     * infos are stored has a capacity < int period.
     */
    private final AtomicInteger nextSchedInfoKey = new AtomicInteger();

    /**
     * Guarded by synchronization on itself.
     */
    private final Map<Integer,MySchedInfo> schedInfoByKey = new HashMap<Integer,MySchedInfo>();

    /**
     * Barrier to ensure visibility of user SDL event values that we posted,
     * at the time and in the thread we retrieve them.
     * Might be useless due to using synchronization around schedInfoByKey,
     * but better be paranoid.
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
     * @param sdlEventListener Listener for SDL events, except the ones internal
     *        to this scheduler.
     */
    public SdlUiThreadScheduler(
            HardClockTimeType hardClockTimeType,
            InterfaceSdlEventListener sdlEventListener,
            double pollPeriodS,
            UncaughtExceptionHandler exceptionHandler) {
        
        this.exceptionHandler = LangUtils.requireNonNull(exceptionHandler);
        
        this.uiThread = Thread.currentThread();
        
        final InterfaceHardClock clock = BindingSchedUtils.newClock(hardClockTimeType); 
        this.hardScheduler = HardScheduler.newThreadlessInstance(clock);
        
        this.sdlEventListener = sdlEventListener;
        
        /*
         * Using the hard scheduler, directly.
         * If we use user SDL events for ASAP schedules,
         * it's required, since it needs to be executed
         * to process user's ASAP schedules.
         * If we don't, then hard scheduler is semantically
         * equivalent to UI scheduler, so it doesn't hurt.
         */
        this.pollProcess = new MyPollProcess(
                this.hardScheduler,
                pollPeriodS);
    }

    /*
     * 
     */

    @Override
    public boolean isWorkerThread() {
        return (Thread.currentThread() == this.uiThread);
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
        return this.hardScheduler.getClock();
    }

    @Override
    public void execute(Runnable runnable) {
        if (MUST_EXECUTE_ASAP_THROUGH_USER_SDL_EVENT) {
            if (this.hardScheduler.isShutdown()) {
                // Shutting down.
                return;
            }
            this.executeWithSdlUserEvent(runnable);
            // Restarting poll process to execute it ASAP.
            this.pollProcess.start();
        } else {
            this.hardScheduler.execute(runnable);
        }
    }

    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        this.hardScheduler.executeAtNs(runnable, timeNs);
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
        if (this.hardScheduler.isShutdown()) {
            // Already shut down.
            return;
        }
        
        this.pollProcess.start();
        
        // Keeps working until handler throws,
        // or startAndWorkInCurrentThread() completed normally.
        while (true) {
            try {
                this.hardScheduler.startAndWorkInCurrentThread();
                // Completed normally: we are done.
                break;
            } catch (Throwable t) {
                this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
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
        final boolean mustInterruptWorkingWorkers = false;
        
        // Useless due to subsequent brutal shut down, but cleaner.
        this.pollProcess.stop();
        
        this.hardScheduler.shutdownNow(mustInterruptWorkingWorkers);
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
    
    private void executeWithSdlUserEvent(Runnable runnable) {
        final int schedInfoKey = this.nextSchedInfoKey.getAndIncrement();
        final SDL_Event eventUnion = newUserEventUnion(
                MY_SCHED_PROCESS_EVENT_TYPE,
                schedInfoKey);
        
        final MySchedInfo schedInfo = new MySchedInfo(runnable);
        
        synchronized (this.schedInfoByKey) {
            this.schedInfoByKey.put(schedInfoKey, schedInfo);
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
    
    private MySchedInfo getSchedInfo(SDL_UserEvent scheduleEvent) {
        // Volatile read for visibility.
        if (this.barrier != BARRIER_VALUE) {
            throw new BindingError();
        }

        final int schedInfoKey;
        final MySchedInfo schedInfo;
        synchronized (this.schedInfoByKey) {
            schedInfoKey = scheduleEvent.code;
            schedInfo = this.schedInfoByKey.remove(schedInfoKey);
        }
        if (schedInfo == null) {
            throw new BindingError("no schedule info for key " + schedInfoKey);
        }
        
        return schedInfo;
    }
    
    /*
     * 
     */
    
    private void pollEvents() {
        
        /*
         * Not calling SDL_PumpEvents after each event processing,
         * in case it would have some overhead.
         * It could also cause infinite loop.
         * 
         * TODO sdl All seems to work even if we don't pump,
         * but it looks cleaner to pump like a shadok.
         */
        
        LIB.SDL_PumpEvents();
        
        /*
         * TODO sdl SDL_PollEvent keeps providing events that are
         * pushed during the loop (for example by the executed runnables),
         * which can cause infinite loop, and ASAP schedules priority
         * over timed schedules (which we don't want).
         * As a workaround, we first retrieve all the pumped events,
         * and secondly process them.
         */

        ArrayList<SDL_Event> eventUnionList = null;
        while (true) {
            final SDL_Event eventUnion = new SDL_Event();
            final int ret = LIB.SDL_PollEvent(eventUnion);
            final boolean gotSome = (ret == 1);
            if (gotSome) {
                if (this.hardScheduler.isShutdown()) {
                    // Shutting down.
                    return;
                }
                if (eventUnionList == null) {
                    eventUnionList = new ArrayList<SDL_Event>();
                }
                eventUnionList.add(eventUnion);
            } else {
                break;
            }
        }
        
        final int size = ((eventUnionList != null) ? eventUnionList.size() : 0);
        if (DEBUG) {
            Dbg.log("events in batch: " + size);
        }
        for (int i = 0; i < size; i++) {
            if (this.hardScheduler.isShutdown()) {
                // Shutting down.
                return;
            }
            final SDL_Event eventUnion = eventUnionList.get(i);
            
            final int type = eventUnion.type();
            if (DEBUG) {
                Dbg.log("event type = " + eventUnion.toStringType() + " : " + type);
            }

            if (type == MY_SCHED_PROCESS_EVENT_TYPE) {
                final SDL_UserEvent.ByValue scheduleEvent = SdlJnaUtils.getUserEvent(eventUnion);
                final MySchedInfo schedInfo = getSchedInfo(scheduleEvent);
                
                try {
                    schedInfo.runnable.run();
                } catch (Throwable t) {
                    this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
                }

            } else {
                try {
                    this.sdlEventListener.onEvent(eventUnion);
                } catch (Throwable t) {
                    this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }
        }
    }
}
