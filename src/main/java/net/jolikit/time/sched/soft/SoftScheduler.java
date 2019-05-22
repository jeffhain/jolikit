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
package net.jolikit.time.sched.soft;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.clocks.InterfaceClockModificationListener;
import net.jolikit.time.clocks.soft.InterfaceSoftClock;
import net.jolikit.time.sched.AbstractDefaultScheduler;
import net.jolikit.time.sched.InterfaceSchedulable;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.WorkerThreadChecker;

/**
 * Scheduler for soft scheduling, i.e. for processing schedulables
 * in a single thread, according to a soft clock.
 * 
 * Clock setTimeNs(...) method is allowed to behave like
 * a time advance request, and cause clock time to advance less
 * than specified, typically due to new schedules being done
 * during the call, for a time earlier than specified.
 * 
 * For a same execution date, timed schedules (done with executeAtXxx(...)
 * or executeAfterXxx(...)) don't have priority over ASAP schedules
 * (done with execute(Runnable)), which on the contrary can be the case
 * with hard schedulers (to avoid ASAP schedules spam to prevent timed schedules
 * to ever execute), since this class implements ASAP schedules as
 * timed schedules to be executed at current time (unless they are
 * executed synchronously).
 * 
 * Typical usage:
 * - initial calls to executeXxx(...)
 * - start (blocks until done)
 * - suspend/resume
 * - stop, or let run until last schedule, or forever if none
 */
public class SoftScheduler extends AbstractDefaultScheduler implements InterfaceWorkerAwareScheduler {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * For defensive programming.
     */
    private class MyPublicClock implements InterfaceSoftClock {
        @Override
        public double getTimeS() {
            return rootClock.getTimeS();
        }
        @Override
        public long getTimeNs() {
            return rootClock.getTimeNs();
        }
        @Override
        public double getTimeSpeed() {
            return rootClock.getTimeSpeed();
        }
        /*
         * 
         */
        @Override
        public boolean addListener(InterfaceClockModificationListener listener) {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean removeListener(InterfaceClockModificationListener listener) {
            throw new UnsupportedOperationException();
        }
        /*
         * 
         */
        @Override
        public void setTimeNs(long timeNs) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void setTimeSpeed(double timeSpeed) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void setTimeNsAndTimeSpeed(long timeNs, double timeSpeed) {
            throw new UnsupportedOperationException();
        }
    }

    private class MyAlienExecutor implements Executor {
        /**
         * Executes the specified command in the soft thread, if it is running,
         * else does not execute it.
         * 
         * @param command Command to execute in the soft thread, if it is running.
         */
        @Override
        public void execute(Runnable command) {
            alienRunnables.add(command);
            // Notify, in case soft thread is suspended.
            synchronized (stateMutex) {
                stateMutex.notify();
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final int INITIAL_QUEUE_CAPACITY = 100;

    /**
     * False by default, for more consistency with hard scheduling's asynchronism,
     * and so that execute(...) can be called before start().
     * 
     * True can help to figure out issues by causing larger call stacks,
     * and to trigger issues corresponding to quick treatments execution
     * in hard scheduling.
     */
    private static final boolean DEFAULT_MUST_EXECUTE_ASAP_SCHEDULES_SYNCHRONOUSLY = false;

    /**
     * True: execute(Runnable) executes runnables synchronously.
     * False: execute(Runnable) executes runnables asynchronously (but still at current time).
     */
    private final boolean mustExecuteAsapSchedulesSynchronously;

    /**
     * Optim: could be a bit faster to use an array for schedules at current time,
     * which could be a common case.
     */
    private final Queue<MyRunnableSchedule> currentSchedules = new PriorityQueue<MyRunnableSchedule>(
            INITIAL_QUEUE_CAPACITY,
            new MyScheduleComparator());

    private final ArrayList<MyRunnableSchedule> futureSchedules = new ArrayList<MyRunnableSchedule>();

    private long nextSequenceNumber = 0;

    /**
     * Thread that called start method, while schedules were pending
     * and scheduler was not started yet.
     */
    private final AtomicReference<Thread> softThreadRef = new AtomicReference<Thread>();

    private final Object stateMutex = new Object();
    private volatile boolean started;
    private volatile boolean suspended;

    private final InterfaceSoftClock rootClock;
    private final InterfaceSoftClock publicClock;

    private final MyAlienExecutor alienExecutor = new MyAlienExecutor();

    /**
     * To run runnables from alien threads in the soft thread.
     */
    private final LinkedBlockingQueue<Runnable> alienRunnables = new LinkedBlockingQueue<Runnable>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a scheduler which execute(Runnable) is asynchronous.
     */
    public SoftScheduler(InterfaceSoftClock rootClock) {
        this(
                rootClock,
                DEFAULT_MUST_EXECUTE_ASAP_SCHEDULES_SYNCHRONOUSLY);
    }

    /**
     * @param rootClock Soft clock which time is to be updated by this scheduler (only).
     * @param mustExecuteAsapSchedulesSynchronously True if execute(Runnable)
     *        must be synchronous, false otherwise.
     */
    public SoftScheduler(
            InterfaceSoftClock rootClock,
            boolean mustExecuteAsapSchedulesSynchronously) {
        this.rootClock = rootClock;
        this.publicClock = new MyPublicClock();
        this.mustExecuteAsapSchedulesSynchronously = mustExecuteAsapSchedulesSynchronously;
    }

    /*
     * 
     */

    /**
     * @return An executor that executes the specified commands
     *         in the soft thread, if it is running, else doesn't
     *         execute them.
     */
    public Executor getAlienExecutor() {
        return this.alienExecutor;
    }

    @Override
    public InterfaceSoftClock getClock() {
        return this.publicClock;
    }

    /**
     * Returns once stopped, or once no more schedule is planned
     * (returns immediately if there is no schedule planned).
     * 
     * @throws IllegalStateException if already started and current thread
     *         is not the thread that started it (soft thread).
     */
    public void start() {
        this.checkNotStartedAndSetSoftThreadWithCurrentThread();
        // Only one thread can pass here at a time, and that
        // is a thread that just became the soft thread.
        try {
            if (this.futureSchedules.size() != 0) {
                try {
                    // No problem if started/suspended are set concurrently with that.
                    this.started = true;
                    this.suspended = false;
                    this.myRun();
                } finally {
                    // Clearing schedules in case wcheduler was stopped while working.
                    this.currentSchedules.clear();
                    this.futureSchedules.clear();

                    this.started = false;
                    // No problem if suspended is set concurrently with that.
                    this.suspended = false;

                    // Executing remaining alien runnables if any.
                    try {
                        this.executeAlienRunnablesIfAny();
                    } finally {
                        // Clearing it if any threw.
                        this.alienRunnables.clear();
                    }
                }
            }
        } finally {
            // Done last, to make sure we throw exception if another thread
            // calls "start" while we are not done yet.
            this.softThreadRef.set(null);
        }
    }

    /**
     * Stops execution of soft schedules, after which they are cleared,
     * but remaining commands, if any, given to alien executor,
     * are still executed.
     * 
     * Has no effect if the soft thread is not running.
     * 
     * Can be called from any thread.
     */
    public void stop() {
        synchronized (stateMutex) {
            this.started = false;
            this.stateMutex.notify();
        }
    }

    /**
     * Suspends execution of soft schedules, but keeps executing
     * runnables specified to alien executor in soft thread.
     * 
     * Can be called from any thread.
     */
    public void suspend() {
        synchronized (stateMutex) {
            this.suspended = true;
            this.stateMutex.notify();
        }
    }

    /**
     * Resumes execution of soft schedules, if soft thread is running.
     * 
     * Can be called from any thread.
     */
    public void resume() {
        synchronized (stateMutex) {
            this.suspended = false;
            this.stateMutex.notify();
        }
    }
    
    /*
     * 
     */
    
    @Override
    public boolean isWorkerThread() {
        final Thread currentThread = Thread.currentThread();
        // Possibly null.
        final Thread workerThread = this.softThreadRef.get();
        return (currentThread == workerThread);
    }

    @Override
    public void checkIsWorkerThread() {
        WorkerThreadChecker.checkIsWorkerThread(this);
    }

    @Override
    public void checkIsNotWorkerThread() {
        WorkerThreadChecker.checkIsNotWorkerThread(this);
    }

    /*
     * 
     */

    @Override
    public void execute(Runnable runnable) {
        final long nowNs = this.rootClock.getTimeNs();
        if (this.mustExecuteAsapSchedulesSynchronously) {
            this.checkIsWorkerThread();
            this.checkScheduleNotInThePast(nowNs);
            
            if (runnable instanceof InterfaceSchedulable) {
                final InterfaceSchedulable schedulable = (InterfaceSchedulable) runnable;
                // Not using a same instance all the time,
                // for execute(...) calls might be reentrant.
                final MySchedulableSchedule tmpSchedule = new MySchedulableSchedule(schedulable);
                tmpSchedule.setTheoreticalTimeNs(nowNs);
                tmpSchedule.configureBeforeRun(nowNs);

                schedulable.setScheduling(tmpSchedule);
                schedulable.run();

                final boolean mustRepeat = tmpSchedule.isNextTheoreticalTimeSet_monomorphic();
                if (mustRepeat) {
                    final long nextNs = tmpSchedule.getNextTheoreticalTimeNs_monomorphic();
                    this.executeAtNs(schedulable, nextNs);
                }
            } else {
                runnable.run();
            }
        } else {
            this.executeAtNs(runnable, nowNs);
        }
    }

    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        this.ifRunningCheckIsWorkerThreadAndScheduleNotInPast(timeNs);

        final MyRunnableSchedule schedule;
        if (runnable instanceof InterfaceSchedulable) {
            final InterfaceSchedulable impl = (InterfaceSchedulable)runnable;
            schedule = new MySchedulableSchedule(impl);
        } else {
            schedule = new MyRunnableSchedule(runnable);
        }
        schedule.setTheoreticalTimeNs(timeNs);
        this.addToFutureSchedules(schedule);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private boolean checkNotStartedAndSetSoftThreadWithCurrentThread() {
        while (true) {
            if (this.softThreadRef.compareAndSet(null, Thread.currentThread())) {
                return true;
            } else {
                final Thread theSoftThread = this.softThreadRef.get();
                if (theSoftThread == null) {
                    // CAS failed, so soft thread was not null,
                    // but since then it went null, so we can try to set it again.
                    continue;
                } else {
                    throw new IllegalStateException("Scheduler already started. Soft thread is [" + theSoftThread + "].");
                }
            }
        }
    }

    private void ifRunningCheckIsWorkerThreadAndScheduleNotInPast(long scheduleTimeNs) {
        final Thread theSoftThread = this.softThreadRef.get();
        if (theSoftThread != null) {
            // Running.
            final Thread currentThread = Thread.currentThread();
            if (currentThread != theSoftThread) {
                throw new ConcurrentModificationException(
                        "Current thread ["
                                + currentThread
                                + "] is not soft thread ["
                                + theSoftThread
                                + "].");
            }
            
            this.checkScheduleNotInThePast(scheduleTimeNs);
        }
    }

    private void checkScheduleNotInThePast(long scheduleTimeNs) {
        final long nowNs = this.rootClock.getTimeNs();
        checkScheduleNotInThePast(scheduleTimeNs, nowNs);
    }

    private static void checkScheduleNotInThePast(long scheduleTimeNs, long nowNs) {
        if (scheduleTimeNs < nowNs) {
            throw new IllegalArgumentException("schedule in the past: " + scheduleTimeNs + " ns < " + nowNs + " ns");
        }
    }

    /*
     * 
     */

    /**
     * Sets sequence number.
     * 
     * @param schedule Schedule with schedulable set.
     */
    private void addToFutureSchedules(MyRunnableSchedule schedule) {
        schedule.setSequenceNumber(this.nextSequenceNumber++);
        this.futureSchedules.add(schedule);
    }

    /**
     * @return True if moved some schedules, false otherwise.
     */
    private boolean makeFutureSchedulesCurrent() {
        final boolean gotSomeToMove = (this.futureSchedules.size() != 0);
        if (gotSomeToMove) {
            for (int i = this.futureSchedules.size(); --i >= 0;) {
                this.currentSchedules.add(this.futureSchedules.get(i));
            }
            this.futureSchedules.clear();
        }
        return gotSomeToMove;
    }

    /**
     * Must be called in soft thread.
     */
    private void executeAlienRunnablesIfAny() {
        Runnable runnable;
        while ((runnable = this.alienRunnables.poll()) != null) {
            runnable.run();
        }
    }

    private void waitWhileStartedAndSuspendedButExecuteAlienRunnables() {
        // Call to execute alien runnables when not suspended.
        this.executeAlienRunnablesIfAny();
        while (this.started && this.suspended) {
            synchronized (this.stateMutex) {
                if (this.started && this.suspended && (this.alienRunnables.size() == 0)) {
                    try {
                        // Notified after started or suspended are set,
                        // or when an alien runnable is added.
                        this.stateMutex.wait();
                    } catch (InterruptedException e) {
                        // quiet
                    }
                }
            }
            // Call to execute alien runnables when suspended.
            this.executeAlienRunnablesIfAny();
        }
    }

    /**
     * Processes schedules until no more schedule
     * or until stopped.
     */
    private void myRun() {
        /*
         * Clock time will be reset to first schedule time,
         * so that user can just do schedules and then call start()
         * without having to bother to initialize clock time properly,
         * so here we ignore current clock time and just use
         * Long.MIN_VALUE to avoid any issue with initial schedules
         * being in the past.
         */
        long nowNs = Long.MIN_VALUE;
        while (this.started) {

            /*
             * processing schedules from soft thread
             */

            this.makeFutureSchedulesCurrent();

            // No new schedule gets enqueued during time advance,
            // so this one will still be the one to process
            // even after eventual wait.
            MyRunnableSchedule schedule = this.currentSchedules.peek();
            if (schedule == null) {
                // No schedule: we are done.
                break;
            }

            {
                final long theoreticalTimeNs = schedule.getTheoreticalTimeNs_monomorphic();
                
                // Should never throw and be useless,
                // due to checks done when creating schedules,
                // but that's a quick check.
                checkScheduleNotInThePast(theoreticalTimeNs, nowNs);

                /*
                 * Advancing time.
                 * 
                 * We handle the case where clock time moves less than requested,
                 * for example if new schedules are done during time advance
                 * and need to be executed before target time.
                 */

                final long timeNsBeforeSet = nowNs;
                /*
                 * We set clock time even if it didn't change,
                 * for clock modification listener to be called,
                 * allowing to be aware that a new sequence of executions
                 * occurs even if time didn't change.
                 */
                this.rootClock.setTimeNs(theoreticalTimeNs);
                nowNs = this.rootClock.getTimeNs();
                if (!NumbersUtils.isInRange(timeNsBeforeSet, theoreticalTimeNs, nowNs)) {
                    throw new IllegalArgumentException(
                            "new current time ["
                                    + nowNs
                                    + " ns] must be between previous one ["
                                    + timeNsBeforeSet
                                    + " ns] and requested one ["
                                    + theoreticalTimeNs
                                    + " ns]");
                }
            }
            /*
             * Adding eventual schedules done during time advance,
             * to those done at previous round.
             */
            final boolean gotNewSchedules = this.makeFutureSchedulesCurrent();
            if (gotNewSchedules) {
                schedule = this.currentSchedules.peek();
                final long theoreticalTimeNs = schedule.getTheoreticalTimeNs_monomorphic();
                // Earliest schedule time must be current time.
                if (theoreticalTimeNs != nowNs) {
                    throw new IllegalArgumentException(
                            "new current time ["
                                    + nowNs
                                    + " ns] is different from earliest schedule time ["
                                    + theoreticalTimeNs
                                    + " ns]");
                }
            }

            /*
             * Processing all schedules that are for current time.
             */

            do {
                this.waitWhileStartedAndSuspendedButExecuteAlienRunnables();
                if (!this.started) {
                    break;
                }

                // Schedule for now: removing and processing it.
                final Object forCheck = this.currentSchedules.remove();
                if (forCheck != schedule) {
                    throw new AssertionError(forCheck + " != " + schedule);
                }

                if (schedule instanceof MySchedulableSchedule) {
                    final MySchedulableSchedule impl = (MySchedulableSchedule) schedule;
                    final InterfaceSchedulable schedulable = (InterfaceSchedulable) impl.getRunnable();

                    impl.configureBeforeRun(nowNs);

                    schedulable.setScheduling(impl);
                    schedulable.run();

                    if (impl.isNextTheoreticalTimeSet_monomorphic()) {
                        final long nextNs = impl.getNextTheoreticalTimeNs_monomorphic();
                        this.checkScheduleNotInThePast(nextNs);

                        schedule.setTheoreticalTimeNs(nextNs);
                        this.addToFutureSchedules(schedule);
                    }
                } else {
                    schedule.getRunnable().run();
                }
            } while (((schedule = this.currentSchedules.peek()) != null)
                    && (schedule.getTheoreticalTimeNs_monomorphic() == nowNs));
        }
    }
}
