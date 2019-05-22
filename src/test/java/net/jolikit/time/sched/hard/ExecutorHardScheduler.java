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
package net.jolikit.time.sched.hard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.sched.AbstractScheduler;
import net.jolikit.time.sched.DefaultScheduling;
import net.jolikit.time.sched.InterfaceCancellable;
import net.jolikit.time.sched.InterfaceSchedulable;

/**
 * Scheduler based on implementations of JDK's Executor interfaces.
 */
public class ExecutorHardScheduler extends AbstractScheduler {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Calls onCancel() on rejected user runnables if they are cancellables,
     * then provides user runnables to the user-specified REH.
     */
    private static class MyRehWrapper implements RejectedExecutionHandler {
        private final RejectedExecutionHandler handler;
        /**
         * @param handler Can be null, for internal usage.
         */
        public MyRehWrapper(RejectedExecutionHandler handler) {
            this.handler = handler;
        }
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            final Runnable userRunnable;
            if (runnable instanceof MySchedulableCommand) {
                final MySchedulableCommand impl = (MySchedulableCommand) runnable;
                userRunnable = impl.schedulable;
            } else {
                userRunnable = runnable;
            }
            try {
                if (userRunnable instanceof InterfaceCancellable) {
                    final InterfaceCancellable userCancellable = (InterfaceCancellable) userRunnable;
                    userCancellable.onCancel();
                }
            } finally {
                final RejectedExecutionHandler handler = this.handler;
                if (handler != null) {
                    handler.rejectedExecution(userRunnable, executor);
                }
            }
        }
    }

    private class MySchedulableCommand extends DefaultScheduling implements Runnable {
        private final InterfaceSchedulable schedulable;
        /**
         * For ASAP schedules.
         */
        public MySchedulableCommand(InterfaceSchedulable schedulable) {
            this.schedulable = schedulable;
        }
        /**
         * For timed schedules.
         */
        public MySchedulableCommand(
                InterfaceSchedulable schedulable,
                long theoreticalTimeNs) {
            this.schedulable = schedulable;
            this.setTheoreticalTimeNs(theoreticalTimeNs);
        }
        @Override
        public void run() {
            final long actualTimeNs = clock.getTimeNs();

            final DefaultScheduling scheduling = this;
            scheduling.updateBeforeRun(actualTimeNs);
            
            this.schedulable.setScheduling(scheduling);
            this.schedulable.run();
            
            final boolean mustRepeat = scheduling.isNextTheoreticalTimeSet();
            if (mustRepeat) {
                final long nextNs = scheduling.getNextTheoreticalTimeNs();
                scheduling.setTheoreticalTimeNs(nextNs);
                
                final long timeAfterRunNs = clock.getTimeNs();
                final long delayNs = NumbersUtils.minusBounded(nextNs, timeAfterRunNs);
                scheduleCommand(this, delayNs, nextNs);
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final InterfaceHardClock clock;

    private final ThreadPoolExecutor asapExecutor;
    private final ScheduledThreadPoolExecutor timedExecutor;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a scheduler based on JDK's executors implementations:
     * does not take clock's modification into account, and only works
     * for clocks with time speed of 1.
     */
    public ExecutorHardScheduler(
            final InterfaceHardClock clock,
            int nbrOfThreadsForAsapSchedules,
            int nbrOfThreadsForTimedSchedules) {
        this(
                clock,
                (ThreadPoolExecutor) Executors.newFixedThreadPool(nbrOfThreadsForAsapSchedules),
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(nbrOfThreadsForTimedSchedules),
                new MyRehWrapper(null));
    }

    /**
     * Creates a scheduler based on JDK's executors implementations:
     * does not take clock's modification into account, and only works
     * for clocks with time speed of 1.
     */
    public ExecutorHardScheduler(
            final InterfaceHardClock clock,
            int nbrOfThreadsForAsapSchedules,
            int nbrOfThreadsForTimedSchedules,
            final ThreadFactory threadFactory) {
        this(
                clock,
                (ThreadPoolExecutor) Executors.newFixedThreadPool(nbrOfThreadsForAsapSchedules, threadFactory),
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(nbrOfThreadsForTimedSchedules, threadFactory),
                new MyRehWrapper(null));
    }

    /**
     * Creates a scheduler based on the specified executors.
     * 
     * The handler is passed as parameter, to avoid user forgetting
     * to set it in the provided executors.
     * 
     * If they implement InterfaceCancellable, the runnables are handed
     * by the handler after call to onCancel(), and even if it throws.
     * 
     * @param handler Rejected execution handler, set into both
     *        specified executors. Must not be null.
     */
    public ExecutorHardScheduler(
            final InterfaceHardClock clock,
            final ThreadPoolExecutor asapSchedulesExecutor,
            final ScheduledThreadPoolExecutor timedSchedulesExecutor,
            final RejectedExecutionHandler handler) {
        LangUtils.requireNonNull(handler);
        
        final MyRehWrapper myHandler = new MyRehWrapper(handler);
        
        asapSchedulesExecutor.setRejectedExecutionHandler(myHandler);
        timedSchedulesExecutor.setRejectedExecutionHandler(myHandler);
        
        this.clock = clock;
        
        this.asapExecutor = asapSchedulesExecutor;
        this.timedExecutor = timedSchedulesExecutor;
    }

    public Executor getAsapSchedulesExecutor() {
        return this.asapExecutor;
    }
    
    public ScheduledExecutorService getTimedSchedulesExecutor() {
        return this.timedExecutor;
    }
    
    public void shutdown() {
        this.asapExecutor.shutdown();
        this.timedExecutor.shutdown();
    }
    
    public List<Runnable> shutdownNow() {
        final ArrayList<Runnable> runnableList = new ArrayList<Runnable>();
        runnableList.addAll(this.asapExecutor.shutdownNow());
        runnableList.addAll(this.timedExecutor.shutdownNow());
        return runnableList;
    }

    @Override
    public InterfaceHardClock getClock() {
        return this.clock;
    }

    @Override
    public void execute(Runnable runnable) {
        if (runnable instanceof InterfaceSchedulable) {
            final MySchedulableCommand command = new MySchedulableCommand(
                    (InterfaceSchedulable) runnable);
            this.asapExecutor.execute(command);
        } else {
            this.asapExecutor.execute(runnable);
        }
    }

    @Override
    public void executeAtNs(
            Runnable runnable,
            long timeNs) {
        final long delayNs = NumbersUtils.minusBounded(timeNs, this.clock.getTimeNs());
        if (runnable instanceof InterfaceSchedulable) {
            final MySchedulableCommand command = new MySchedulableCommand(
                    (InterfaceSchedulable) runnable,
                    timeNs);
            this.scheduleCommand(command, delayNs, timeNs);
        } else {
            this.timedExecutor.schedule(runnable, delayNs, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void executeAfterNs(
            Runnable runnable,
            long delayNs) {
        if (runnable instanceof InterfaceSchedulable) {
            final long timeNs = NumbersUtils.plusBounded(this.clock.getTimeNs(), delayNs);
            final MySchedulableCommand command = new MySchedulableCommand(
                    (InterfaceSchedulable) runnable,
                    timeNs);
            this.scheduleCommand(command, delayNs, timeNs);
        } else {
            this.timedExecutor.schedule(runnable, delayNs, TimeUnit.NANOSECONDS);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void scheduleCommand(
            final MySchedulableCommand command,
            long delayNs,
            long timeNs) {
        this.timedExecutor.schedule(command, delayNs, TimeUnit.NANOSECONDS);
    }
}
