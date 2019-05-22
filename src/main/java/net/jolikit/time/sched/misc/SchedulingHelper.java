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
package net.jolikit.time.sched.misc;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.lang.InterfaceFactory;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.InterfaceControllableClock;
import net.jolikit.time.clocks.hard.EnslavedControllableHardClock;
import net.jolikit.time.clocks.hard.ThinControllableSystemClock;
import net.jolikit.time.clocks.soft.RootSoftClock;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.hard.HardScheduler;
import net.jolikit.time.sched.soft.SoftScheduler;

/**
 * Helper class to create clocks and schedulers for soft or hard scheduling.
 */
public class SchedulingHelper {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final String SCHED_HELPER_THREAD_NAME_PREFIX = "SCHED_HELPER";
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------

    public enum SchedulingType {
        /**
         * Scheduling based on a hard clock.
         */
        HARD,
        /**
         * Scheduling based on a soft clock which time tries to follow
         * the time of a hard clock.
         * Allows for soft scheduling without going too fast.
         */
        SOFT_HARD_BASED,
        /**
         * Scheduling based on a soft clock which time immediately jumps
         * to the next planned schedule.
         */
        SOFT_AFAP
    }

    public enum SoftAsapExecutionType {
        /**
         * execute(Runnable) executes the specified runnable synchronously.
         */
        SYNCHRONOUS,
        /**
         * execute(Runnable) executes the specified runnable asynchronously.
         */
        ASYNCHRONOUS
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private class MyHardSchedulerFactory implements InterfaceFactory<InterfaceScheduler> {
        private final int schedulerFactoryId = SCHEDULER_FACTORY_ID_GENERATOR.incrementAndGet();
        private final AtomicInteger sfThreadNameIdGenerator = new AtomicInteger();
        @Override
        public InterfaceScheduler newInstance() {
            return HardScheduler.newInstance(
                    hardClock,
                    this.getSchedulerFactoryNewThreadNamePrefix(),
                    true, // daemon
                    1, // nbrOfThreads
                    Integer.MAX_VALUE, // asapQueueCapacity
                    Integer.MAX_VALUE, // timedQueueCapacity
                    threadFactory);
        }
        private String getSchedulerFactoryNewThreadNamePrefix() {
            return SCHED_HELPER_THREAD_NAME_PREFIX
                    + "_"
                    + this.schedulerFactoryId
                    + "_"
                    + this.sfThreadNameIdGenerator.incrementAndGet();
        }
    }

    private class MySoftSchedulerFactory implements InterfaceFactory<InterfaceScheduler> {
        @Override
        public InterfaceScheduler newInstance() {
            if (softScheduler == null) {
                // Class only used in case of soft scheduling.
                throw new AssertionError();
            }
            return softScheduler;
        }
    }

    private static class MyHardDataExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private class MySoftDataExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            softScheduler.execute(command);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final AtomicInteger SCHEDULER_FACTORY_ID_GENERATOR = new AtomicInteger();
    
    /*
     * 
     */
    
    private final ThreadFactory threadFactory;

    private final EnslavedControllableHardClock hardClock;

    private final RootSoftClock rootSoftClock;
    private final SoftScheduler softScheduler;

    private final InterfaceFactory<InterfaceScheduler> schedulerFactory;

    /**
     * Executor to run runnables in soft thread, in case of soft scheduling,
     * or in current thread, in case of hard scheduling.
     * Allows to access data of soft thread transparently.
     */
    private final Executor dataExecutor;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param schedulingType Scheduling type.
     * @param softAsapExecutionType Execution type for ASAP schedules, in case of soft scheduling.
     *        Use ASYNCHRONOUS if you have no clue, to preserve hard-scheduling-like asynchronish behavior.
     * @param latenessThresholdNs See RootSoftClock class. Only relevant in case of SOFT_HARD_BASED scheduling.
     * @param initialTimeNs Initial time, in nanoseconds.
     * @param initialTimeSpeedIfNotAfap Initial time speed. Only relevant if scheduling is not SOFT_AFAP.
     * @param threadFactory Thread factory to use. Only relevant in case of hard scheduling. Can be null.
     */
    public SchedulingHelper(
            SchedulingType schedulingType,
            SoftAsapExecutionType softAsapExecutionType,
            long latenessThresholdNs,
            long initialTimeNs,
            double initialTimeSpeedIfNotAfap,
            ThreadFactory threadFactory) {
        final EnslavedControllableHardClock hardClock;
        final RootSoftClock rootSoftClock;
        final SoftScheduler rootSoftScheduler;
        final Executor dataExecutor;
        final InterfaceFactory<InterfaceScheduler> schedulerFactory;
        if (schedulingType != SchedulingType.HARD) {
            if (schedulingType == SchedulingType.SOFT_AFAP) {
                hardClock = null;

                rootSoftClock = new RootSoftClock();
                rootSoftClock.setTimeNs(initialTimeNs);
            } else if (schedulingType == SchedulingType.SOFT_HARD_BASED) {
                hardClock = new ThinControllableSystemClock();
                /*
                 * Setting hard clock time before soft clock starts to listen to it,
                 * so that it doesn't try to jump of something "accordingly".
                 */
                hardClock.setTimeNsAndTimeSpeed(initialTimeNs, 0.0);

                rootSoftClock = new RootSoftClock(hardClock);
                
                // Starting listening to hard clock's modifications.
                rootSoftClock.startListening();
                
                /*
                 * Good practice to set initial soft time before lateness threshold,
                 * else, if lateness threshold is too small, it might set some
                 * "annulled lateness" in the soft clock, and cause an offset
                 * with hard clock (though it couldn't happen here since we already
                 * set hard clock's time).
                 */
                rootSoftClock.setTimeNs(initialTimeNs);
                rootSoftClock.setLatenessThresholdNS(latenessThresholdNs);
            } else {
                throw new AssertionError(schedulingType);
            }
            
            boolean mustExecuteAsapSchedulesSynchronously;
            if (softAsapExecutionType == SoftAsapExecutionType.SYNCHRONOUS) {
                mustExecuteAsapSchedulesSynchronously = true;
            } else if (softAsapExecutionType == SoftAsapExecutionType.ASYNCHRONOUS) {
                mustExecuteAsapSchedulesSynchronously = false;
            } else {
                throw new AssertionError(softAsapExecutionType);
            }
            rootSoftScheduler = new SoftScheduler(
                    rootSoftClock,
                    mustExecuteAsapSchedulesSynchronously);

            schedulerFactory = new MySoftSchedulerFactory();

            dataExecutor = new MySoftDataExecutor();
        } else {
            hardClock = new ThinControllableSystemClock();
            hardClock.setTimeNsAndTimeSpeed(initialTimeNs, 0.0);

            rootSoftClock = null;
            rootSoftScheduler = null;

            schedulerFactory = new MyHardSchedulerFactory();

            dataExecutor = new MyHardDataExecutor();
        }

        this.threadFactory = threadFactory;

        this.hardClock = hardClock;

        this.rootSoftClock = rootSoftClock;
        this.softScheduler = rootSoftScheduler;

        this.schedulerFactory = schedulerFactory;

        this.dataExecutor = dataExecutor;

        /*
         * Starting time last, to avoid offset due to
         * spending time in this constructor.
         */
        if (hardClock != null) {
            hardClock.setTimeSpeed(initialTimeSpeedIfNotAfap);
        }
    }

    /**
     * @return Executor that runs runnables in soft thread in case of soft scheduling,
     *         or in current thread in case of hard scheduling.
     */
    public Executor getDataExecutor() {
        return this.dataExecutor;
    }

    /**
     * @return Clock used by provided scheduler(s).
     */
    public InterfaceClock getClock() {
        if (this.softScheduling()) {
            return this.rootSoftClock;
        } else {
            return this.hardClock;
        }
    }

    /**
     * Clock on which time and/or time speed (if non-AFAP)
     * can be set.
     * 
     * @return In case of non-AFAP scheduling (soft or hard),
     *         returns hard clock (which is controllable),
     *         else, i.e. in case of AFAP soft scheduling,
     *         returns the soft clock, which time might be
     *         modified but not its time speed.
     */
    public InterfaceControllableClock getControllableClock() {
        if (this.hardClock != null) {
            return this.hardClock;
        } else {
            return this.rootSoftClock;
        }
    }

    /**
     * In case of soft scheduling, this method starts soft scheduler,
     * else does nothing.
     */
    public void startSoftSchedulerIfSoftScheduling() {
        if (this.softScheduling()) {
            this.softScheduler.start();
        }
    }

    /**
     * In case of soft scheduling, this method stops soft scheduler,
     * else does nothing.
     */
    public void stopSoftSchedulerIfSoftScheduling() {
        if (this.softScheduling()) {
            this.softScheduler.stop();
        }
    }

    /*
     * schedulers
     */

    /**
     * In case of soft scheduling, just returns the soft scheduler.
     * 
     * @param threadNamePrefix In case of hard scheduling,
     *        prefix for worker threads names, else not used.
     *        Can be null, in which case worker threads names are not set.
     * @return A scheduler using the clock returned by getClock(),
     *         Integer.MAX_VALUE as queues capacity, and, in case of
     *         hard scheduling, a non-daemon thread.
     */
    public InterfaceScheduler getScheduler(String threadNamePrefix) {
        return getScheduler(
                threadNamePrefix,
                1);
    }

    /**
     * In case of soft scheduling, just returns the soft scheduler.
     * 
     * @param threadNamePrefix In case of hard scheduling,
     *        prefix for worker threads names, else not used.
     *        Can be null, in which case worker threads names are not set.
     * @param nbrOfThreads In case of hard scheduling, the number of threads
     *        to use, else not used. Must be >= 1.
     * @return A scheduler using the clock returned by getClock(),
     *         Integer.MAX_VALUE as queues capacity, and, in case of
     *         hard scheduling, non-daemon threads.
     */
    public InterfaceScheduler getScheduler(
            String threadNamePrefix,
            int nbrOfThreads) {
        return getScheduler(
                threadNamePrefix,
                nbrOfThreads,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE);
    }

    /**
     * In case of soft scheduling, just returns the soft scheduler.
     * 
     * @param threadNamePrefix In case of hard scheduling,
     *        prefix for worker threads names, else not used.
     *        Can be null, in which case worker threads names are not set.
     * @param nbrOfThreads In case of hard scheduling, the number of threads
     *        to use, else not used. Must be >= 1.
     * @param asapQueueCapacity Capacity (>=0) for ASAP schedules queue.
     *        When full, new schedules are canceled.
     * @param timedQueueCapacity Capacity (>=0) for timed schedules queue.
     *        When full, new schedules are canceled.
     * @return A scheduler using the clock returned by getClock(),
     *         and, in case of hard scheduling, non-daemon threads.
     */
    public InterfaceScheduler getScheduler(
            String threadNamePrefix,
            int nbrOfThreads,
            int asapQueueCapacity,
            int timedQueueCapacity) {
        if (this.softScheduling()) {
            return this.softScheduler;
        } else {
            return HardScheduler.newInstance(
                    this.hardClock,
                    threadNamePrefix,
                    false, // daemon
                    nbrOfThreads,
                    asapQueueCapacity,
                    timedQueueCapacity,
                    this.threadFactory);
        }
    }

    /*
     * 
     */

    /**
     * @return A factory of schedulers, providing schedulers as returned by
     *         getScheduler(String) method.
     */
    public InterfaceFactory<InterfaceScheduler> getSchedulerFactory() {
        return this.schedulerFactory;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private boolean softScheduling() {
        return this.rootSoftClock != null;
    }
}
