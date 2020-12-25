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
package net.jolikit.time.sched.hard;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import net.jolikit.lang.NbrsUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.sched.AbstractScheduler;

/**
 * Scheduler based on an implementation of JDK's Timer class.
 * 
 * To ensure that onCancel() method of cancellables is properly called
 * on timer's cancellation or purge, we would need to keep track
 * of pending schedules aside from the Timer, which would add
 * an unacceptable overhead. We prefer to disable calls to
 * Timer's cancel() and purge() methods, or any other added
 * cancellation method, by using a private instance of Timer.
 */
public class TimerHardScheduler extends AbstractScheduler {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyTimerTask extends TimerTask {
        private final Runnable runnable;
        public MyTimerTask(Runnable runnable) {
            this.runnable = runnable;
        }
        @Override
        public void run() {
            this.runnable.run();
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final InterfaceHardClock clock;

    /**
     * Not using Timer's at-time schedule method,
     * since Timer's time might not be synchronized
     * with clock's time.
     */
    private final Timer timer = new Timer();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a scheduler based on JDK's Timer implementation:
     * does not take clock's modification into account, and only works
     * for clocks with time speed of 1.
     */
    public TimerHardScheduler(InterfaceHardClock clock) {
        this.clock = clock;
    }

    @Override
    public InterfaceClock getClock() {
        return this.clock;
    }

    @Override
    public void execute(Runnable runnable) {
        final MyTimerTask task = new MyTimerTask(runnable);
        this.timer.schedule(task, 0L);
    }

    @Override
    public void executeAtNs(
            Runnable runnable,
            long timeNs) {
        final long delayNs = NbrsUtils.minusBounded(timeNs, this.clock.getTimeNs());
        executeTimed(runnable, delayNs, timeNs);
    }

    @Override
    public void executeAfterNs(
            Runnable runnable,
            long delayNs) {
        final long timeNs = NbrsUtils.plusBounded(this.clock.getTimeNs(), delayNs);
        executeTimed(runnable, delayNs, timeNs);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void executeTimed(
            final Runnable runnable,
            long delayNs,
            long timeNs) {
        /*
         * Timer does not accept already schedule tasks for new schedules,
         * so we need to create one each time.
         */
        final MyTimerTask task = new MyTimerTask(runnable);
        long delayMs = TimeUnit.NANOSECONDS.toMillis(delayNs);
        if (delayMs < 0) {
            // Timer does not accept negative delays.
            delayMs = 0;
        }
        // Never using Timer's at-time scheduling method,
        // for it requires to create a Date object each time,
        // and because clock's time might not be system time.
        this.timer.schedule(task, delayMs);
    }
}
