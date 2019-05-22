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
package net.jolikit.bwd.impl.algr5;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.AlgrEventType;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaUtils;
import net.jolikit.bwd.impl.utils.sched.BindingSchedUtils;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.bwd.impl.utils.sched.UiThreadChecker;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.sched.AbstractRepeatedProcess;
import net.jolikit.time.sched.AbstractScheduler;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.hard.HardScheduler;

import com.sun.jna.Pointer;

public class AlgrUiThreadScheduler extends AbstractScheduler implements InterfaceWorkerAwareScheduler {
    
    /*
     * TODO algr We would like for UI thread to wait on both user and system
     * events, and use user events to wake it up to execute ASAP or timed
     * schedules, using al_wait_for_event(event_queue, null) and then
     * al_get_next_event(event_queue, eventUnion) (for the boolean info),
     * but we can't seem to get al_emit_user_event(...) to work, as it just
     * calls the event destructor synchronously and returns false.
     * It does so even if the event source is properly initialized with
     * al_init_user_event_source(...), registered with
     * al_register_event_source(...), with al_is_event_source_registered(...)
     * returning true, and the user event has all fields (source, etc.)
     * properly set).
     * 
     * As a result, we just use a HardScheduler as UI scheduler, and have it
     * poll system events from time to time.
     * It doesn't allow to process system events ASAP, but it's simpler to code
     * (no user events), and gives a better throughput for ASAP schedules.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyPollProcess extends AbstractRepeatedProcess {
        final long pollPeriodNs;
        public MyPollProcess(
                InterfaceScheduler scheduler,
                double pollPeriodS) {
            super(scheduler);
            this.pollPeriodNs = sToNsNoUnderflow(pollPeriodS);
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            if (uiScheduler.isShutdown()) {
                // Shutting down.
                this.stop();
                return 0;
            }
            
            onPollTime(theoreticalTimeNs, actualTimeNs);
            /*
             * Using theoretical time could cause ramp-up if processing
             * always takes more time than polling period.
             * Using actual time at method start suffices to prevent ramp-up,
             * without having to use freshly clock time here, which would
             * unnecessarily increase actual period from the amount
             * of time processing took.
             */
            return plusBounded(actualTimeNs, this.pollPeriodNs);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;

    private final Thread uiThread;
    
    private final Pointer event_queue;
    private final InterfaceAlgrEventListener algrEventListener;
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    private final HardScheduler uiScheduler;
    
    private final MyPollProcess pollProcess;

    private final AtomicBoolean processXxxUntilShutdownAlreadyCalled = new AtomicBoolean(false);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * UI thread is set to be the constructing thread.
     * 
     * @param algrEventListener Listener for Allegro events, except the ones internal
     *        to this scheduler.
     */
    public AlgrUiThreadScheduler(
            HardClockTimeType hardClockTimeType,
            UncaughtExceptionHandler exceptionHandler,
            Pointer event_queue,
            double pollPeriodS,
            InterfaceAlgrEventListener algrEventListener) {
        
        this.uiThread = Thread.currentThread();
        
        this.event_queue = event_queue;
        this.algrEventListener = algrEventListener;
        
        this.exceptionHandler = LangUtils.requireNonNull(exceptionHandler);
        
        final InterfaceHardClock clock = BindingSchedUtils.newClock(hardClockTimeType); 
        this.uiScheduler = HardScheduler.newThreadlessInstance(clock);
        
        /*
         * Using the hard scheduler, directly, for it's
         * semantically equivalent to "this", for
         * scheduling at least.
         */
        this.pollProcess = new MyPollProcess(
                this.uiScheduler,
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
        if (this.uiScheduler.isShutdown()) {
            // Already shut down.
            return;
        }

        this.pollProcess.start();
        
        // Keeps working until handler throws,
        // or startAndWorkInCurrentThread() completed normally.
        while (true) {
            try {
                this.uiScheduler.startAndWorkInCurrentThread();
                // Completed normally: we are done.
                break;
            } catch (Throwable t) {
                this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
            }
        }
    }

    public void shutdownNow() {
        final boolean mustInterruptWorkingWorkers = false;
        
        // Useless due to subsequent brutal shut down, but cleaner.
        this.pollProcess.stop();

        this.uiScheduler.shutdownNow(mustInterruptWorkingWorkers);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void onPollTime(long theoreticalTimeNs, long actualTimeNs) {
        
        /*
         * To avoid eventual infinite loop due to processing events
         * pushed while processing polled events, we first poll all
         * the events we can, and then process them.
         */

        final ArrayList<ALLEGRO_EVENT> eventUnionList = new ArrayList<ALLEGRO_EVENT>();
        while (true) {
            final ALLEGRO_EVENT eventUnion = new ALLEGRO_EVENT();
            final boolean gotSome = LIB.al_get_next_event(this.event_queue, eventUnion);
            if (gotSome) {
                if (this.uiScheduler.isShutdown()) {
                    // Shutting down.
                    return;
                }
                eventUnionList.add(eventUnion);
            } else {
                break;
            }
        }
        
        final int size = eventUnionList.size();
        for (int i = 0; i < size; i++) {
            if (this.uiScheduler.isShutdown()) {
                // Shutting down.
                return;
            }
            final ALLEGRO_EVENT eventUnion = eventUnionList.get(i);
            
            final AlgrEventType eventType = AlgrJnaUtils.getEventType(eventUnion);
            if (DEBUG) {
                Dbg.log("event type = " + eventType);
            }
            try {
                this.algrEventListener.onEvent(eventUnion);
            } catch (Throwable t) {
                this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
            }
        }
    }
}
