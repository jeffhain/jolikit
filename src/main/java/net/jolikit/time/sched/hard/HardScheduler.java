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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.threading.locks.InterfaceCondilock;
import net.jolikit.threading.locks.LockCondilock;
import net.jolikit.threading.locks.MonitorCondilock;
import net.jolikit.time.clocks.ClocksUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.InterfaceClockModificationListener;
import net.jolikit.time.clocks.hard.HardClockCondilock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.sched.AbstractDefaultScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.SchedUtils;
import net.jolikit.time.sched.WorkerThreadChecker;

/**
 * Scheduler based on a hard clock, and on possibly multiple
 * threads, common for ASAP and timed schedules.
 * 
 * Main differences with JDK's ScheduledThreadPoolExecutor
 * (other than those listed in InterfaceScheduler javadoc):
 * - more:
 *   - Handles scheduling according to a clock which time speed might change,
 *     as well as time (other than due to real time flowing).
 *   - Hopefully has less overhead, especially for timed (non-ASAP) schedules.
 *   - Methods to start/stop schedules acceptance and processing.
 *   - ASAP and timed schedules queues can have a max capacity,
 *     over which schedules are rejected.
 * - different:
 *   - Uses fixed threads instead of a thread pool.
 *   - Timed fairness: timed schedules that can currently be executed
 *     are executed by submission order, and not by theoretical time order,
 *     which would cause recent schedules for times far in the past
 *     to postpone (potentially forever) execution of older schedules
 *     for more recent times inferior to current time.
 * 
 * This scheduler provides methods to:
 * - start/stop schedules acceptance,
 * - start/stop schedules processing by worker threads,
 * - cancel independently ASAP and timed pending schedules.
 * It might be a bad idea to allow for independent acceptance or processing
 * of ASAP and timed schedules, since ASAP or timed schedules treatments
 * might respectively do timed and ASAP schedules, so no such feature
 * is provided.
 * 
 * Scheduling fairness: when a timed schedule is eligible along with an
 * ASAP schedule, the one that has been scheduled first has priority.
 * 
 * Workers runnables don't catch any throwable, so worker threads might die
 * on the first exception thrown by a runnable, but workers runnables
 * are designed such as if a runnable throws an exception, calling
 * worker's runnable again will make it continue working properly.
 * Making sure that worker's runnables are called again after they threw
 * an exception can be done easily, using a thread factory that properly
 * wraps worker's runnables with runnables containing proper try/catch
 * and loop.
 * After returning normally, a worker's runnable is not supposed to be called again
 * (by whatever runnable a thread factory could wrap around it), or workers death
 * would not be possible.
 * 
 * Threads are lazily started, as well as listening to clocks modifications,
 * to avoid "this" publication before the instance is fully constructed.
 * You can force early threads start with startWorkerThreadsIfNeeded().
 * 
 * Listening to clocks modifications is also automatically removed when
 * no more worker thread is running and no clock time waitXxx method is
 * being used.
 * Add/removal of listener for clocks modifications is done within a state lock,
 * and involves calling addListener, removeListener and getMasterClock methods
 * of listenable and enslaved clocks: to avoid deadlock possibilities, implementations
 * of these methods should therefore never try to acquire a lock in which another
 * thread could be concurrently calling a method triggering such add/removal, i.e. a
 * clock time waitXxx method of this class, or worker's runnables.
 * 
 * NB: executeAfterXxx(...) methods schedule the specified runnable
 * for current time plus the specified delay, which makes the
 * actually waited delay sensible to clock's time jumps.
 * If you need waited delay not to be sensible to time jumps of
 * your clock (either due to time modification or system time jumps),
 * you can use a clock with identical time speed, but a time that
 * does not jump, by using System.nanoTime() instead of
 * System.currentTimeMillis() as reference hard time.
 */
public class HardScheduler extends AbstractDefaultScheduler implements InterfaceWorkerAwareScheduler {
    
    /*
     * locks order to respect (to avoid deadlocks):
     * - "A > B" <=> "can lock B in A, but not A in B"
     * - clock's modification lock > stateMutex > noRunningWorkerMutex > schedLock
     * 
     * Except clock's modification lock, which is an external lock,
     * all these locks can be acquired by user thread when calling
     * public methods of this class.
     * 
     * (Eventually) locking treatments done in each lock (must respect locks order):
     * - clock's modification lock:
     *   - locking of schedLock and noRunningWorkerMutex (to signal waiting workers).
     * - stateMutex:
     *   - internal listener added to/removed from clocks
     * - noRunningWorkerMutex:
     *   - schedLock (to retrieve the number of pending schedules)
     */
    
    /*
     * NB: Not designed for being backing by an Executor,
     * but could maybe be added (would allow to make schedulers
     * based on executors based on schedulers etc., and check
     * it would still work, even though with more overhead).
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean AZZERTIONS = false;
    
    /**
     * True to chain signaling for workers.
     * 
     * Advantage of signal chaining: does not wakes up all workers if there is
     * only one runnable to process.
     * Disadvantage: might take more time to wake up all workers to process
     * a burst of runnables.
     */
    private static final boolean COMMON_SIGNAL_CHAINING = true;

    /**
     * Same as default in PriorityQueue.
     */
    private static final int INITIAL_PRIORITY_QUEUE_CAPACITY = 11;
    
    private static final int DEFAULT_ASAP_QUEUE_CAPACITY = Integer.MAX_VALUE;
    private static final int DEFAULT_TIMED_QUEUE_CAPACITY = Integer.MAX_VALUE;

    private static final long DEFAULT_MAX_SYSTEM_WAIT_TIME_NS = Long.MAX_VALUE;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * This clock listener is to be registered before possible clock time waits,
     * and unregistered when no more clock wait is possible (i.e. no more
     * running worker waiting for a schedule's time to occur, and no clock time
     * wait from a public waitXXX method).
     * 
     * The advantage of factoring signaling of different conditions in a same
     * listener, is to reduce the amount and frequency of listeners
     * registering/unregistering, which could be much more high otherwise.
     * 
     * Meanwhile, this listener might be registered at times where it's not
     * needed, for example if schedules processing is stopped and no clock time
     * waitXXX method is being used.
     */
    private class MyClockListener implements InterfaceClockModificationListener {
        @Override
        public void onClockModification(InterfaceClock clock) {
            /*
             * Since we are typically not in stateMutex here,
             * processing might become started just after we
             * retrieved corresponding boolean value, but in
             * that case signaling is done by processing start
             * treatment.
             */
            if (mustProcessSchedules()) {
                schedClockTimeCondilock.signalAllInLock();
            } else {
                // Will be signaled when processing is started.
            }
            noRunningWorkerClockTimeCondilock.signalAllInLock();
        }
    }

    /*
     * Workers runnable.
     */
    
    private class MyWorkerRunnable implements Runnable {
        private volatile boolean started = false;
        /**
         * Volatile, in case run gets called again by another thread.
         */
        private volatile boolean done = false;
        /**
         * Not logging errors in this method: user can wrap
         * this runnable using a thread factory, else default
         * JDK handling is used.
         */
        public void run() {
            if (this.done) {
                throw new IllegalStateException("worker done");
            }
            
            nbrOfRunningWorkers.incrementAndGet();
            if (!this.started) {
                // This counter needs to be incremented after
                // number of running workers, for it is checked
                // whether or not to wait for no more running workers.
                nbrOfStartedWorkers.incrementAndGet();
                this.started = true;
            }
            try {
                incrementClockWaitersCountAndAddListenerIfNeeded();
                workerRun();
                this.done = true;
            } finally {
                if (nbrOfRunningWorkers.decrementAndGet() == 0) {
                    noRunningWorkerSystemTimeCondilock.signalAllInLock();
                }
                decrementClockWaiterCountAndRemoveListenerIfNeeded();
            }
        }
    }

    /*
     * 
     */
    
    private class MyNoRunningWorkerBC implements InterfaceBooleanCondition {
        @Override
        public boolean isTrue() {
            if ((nbrOfStartedWorkers.get() != workerThreadArr.length)
                    && (getNbrOfPendingSchedules() != 0)) {
                // Not all workers started yet, but they are being started since there
                // are some pending schedules: in this case, we never consider that there
                // is no running worker, to make their lazy-start transparent.
                return false;
            }
            return getNbrOfRunningWorkers() == 0;
        }
    }
    
    /*
     * 
     */
    
    /**
     * Little queue for use instead of a LinkedList,
     * not so much for speed than for lower memory footprint,
     * in particular in case of ASAP schedules bursts
     * (but once it grows, it doesn't shrink).
     */
    private static class MyAsapQueue {
        private MySequencedSchedule[] arr = new MySequencedSchedule[2];
        private int beginIndex = 0;
        private int endIndex = -1;
        private int size = 0;
        public int size() {
            return this.size;
        }
        public boolean add(MySequencedSchedule schedule) {
            if (this.arr.length == this.size) {
                this.growArr();
            }
            if (++this.endIndex == this.arr.length) {
                this.endIndex = 0;
            }
            this.arr[this.endIndex] = schedule;
            ++this.size;
            return true;
        }
        public MySequencedSchedule first() {
            if (this.size == 0) {
                throw new NoSuchElementException();
            }
            return this.arr[this.beginIndex];
        }
        public MySequencedSchedule remove() {
            if (this.size == 0) {
                throw new NoSuchElementException();
            }
            return remove_notEmpty();
        }
        public MySequencedSchedule poll() {
            if (this.size == 0) {
                return null;
            }
            return remove_notEmpty();
        }
        private MySequencedSchedule remove_notEmpty() {
            final MySequencedSchedule schedule = this.arr[this.beginIndex++];
            if (this.beginIndex == this.arr.length) {
                this.beginIndex = 0;
            }
            if (--this.size == 0) {
                this.beginIndex = 0;
                this.endIndex = -1;
            }
            return schedule;
        }
        private void growArr() {
            final int newCapacity = LangUtils.increasedArrayLength(this.arr.length, this.arr.length + 1);
            final MySequencedSchedule[] newArr = new MySequencedSchedule[newCapacity];
            if (this.beginIndex <= this.endIndex) {
                System.arraycopy(this.arr, this.beginIndex, newArr, 0, this.size);
            } else {
                final int beginSize = this.arr.length - this.beginIndex;
                System.arraycopy(this.arr, this.beginIndex, newArr, 0, beginSize);
                System.arraycopy(this.arr, 0, newArr, beginSize, this.endIndex + 1);
            }
            this.arr = newArr;
            this.beginIndex = 0;
            this.endIndex = this.size - 1;
        }
    }
    
    /**
     * PriorityQueue allowing to use different priorities
     * depending on whether the schedules are to be executed
     * in the future (using theoretical time), or if they are
     * eligible to be executed now (using sequence number).
     */
    private static class MyFairPriorityQueue {
        /**
         * NB: Could merge this queue with ASAP-specific queue,
         * since all currently executable schedules are to be executed
         * according to their sequence number,
         * but we prefer to keep the ASAP/timed separation,
         * in part because it is already visible in the API
         * (separate counts), and for performances
         * (ASAP queue much simpler (no comparator)
         * and likely faster).
         */
        private final PriorityQueue<MyTimedSchedule> currentQueue =
            new PriorityQueue<MyTimedSchedule>(
                INITIAL_PRIORITY_QUEUE_CAPACITY,
                SEQUENCED_SCHEDULE_COMPARATOR);
        private final PriorityQueue<MyTimedSchedule> futureQueue =
            new PriorityQueue<MyTimedSchedule>(
                INITIAL_PRIORITY_QUEUE_CAPACITY,
                TIMED_SCHEDULE_COMPARATOR);
        public MyFairPriorityQueue() {
        }
        /**
         * Moves schedules of theoretical time <= nowNs
         * from futureQueue to currentQueue,
         * for them to be ordered by sequence number
         * instead of by theoretical time.
         * 
         * to be called before peek()/poll()/remove() usages
         * when time might have changed or queue might have been modified,
         * if caring about related ordering change.
         * 
         * If never called, this queue just behaves like a PriorityQueue
         * using TIMED_SCHEDULE_COMPARATOR.
         */
        public void moveCurrentSchedulesToTheirQueue(long nowNs) {
            while (true) {
                final MyTimedSchedule sched = this.futureQueue.peek();
                if ((sched != null)
                    && (sched.getTheoreticalTimeNs() <= nowNs)) {
                    final MySequencedSchedule forCheck = this.futureQueue.remove();
                    if(AZZERTIONS)LangUtils.azzert(forCheck == sched);
                    this.currentQueue.add(sched);
                } else {
                    break;
                }
            }
        }
        public int size() {
            return this.currentQueue.size() + this.futureQueue.size();
        }
        /**
         * @return true
         */
        public boolean add(MyTimedSchedule sched) {
            return this.futureQueue.add(sched);
        }
        public MyTimedSchedule peek() {
            final MyTimedSchedule ret;
            if (this.currentQueue.size() != 0) {
                ret = this.currentQueue.peek();
            } else {
                ret = this.futureQueue.peek();
            }
            return ret;
        }
        public MyTimedSchedule poll() {
            final MyTimedSchedule ret;
            if (this.currentQueue.size() != 0) {
                ret = this.currentQueue.poll();
            } else {
                ret = this.futureQueue.poll();
            }
            return ret;
        }
        public MyTimedSchedule remove() {
            final MyTimedSchedule ret;
            if (this.currentQueue.size() != 0) {
                // Faster than remove(), and we know not empty.
                ret = this.currentQueue.poll();
            } else {
                ret = this.futureQueue.remove();
            }
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final MyNoRunningWorkerBC noRunningWorkerBooleanCondition = new MyNoRunningWorkerBC();
    
    private final InterfaceHardClock clock;

    /*
     * schedLock
     */
    
    /**
     * Lock waited on for scheduling.
     * Guards some runnables collections.
     */
    private final ReentrantLock schedLock = new ReentrantLock();
    private final Condition schedCondition = this.schedLock.newCondition();
    private final InterfaceCondilock schedSystemTimeCondilock = new LockCondilock(this.schedLock, this.schedCondition);
    private final HardClockCondilock schedClockTimeCondilock;

    /*
     * noRunningWorkerMutex
     */
    
    private final Object noRunningWorkerMutex = new Object();
    private final InterfaceCondilock noRunningWorkerSystemTimeCondilock = new MonitorCondilock(this.noRunningWorkerMutex);
    private final HardClockCondilock noRunningWorkerClockTimeCondilock;

    /*
     * 
     */

    /**
     * Guarded by stateMutex.
     * Number of threads possibly waiting on main or noRunningWorker conditions,
     * used to know whether or not to add or remove clock listener.
     */
    private int clockWaiterCount;

    private final MyClockListener clockListener = new MyClockListener();
    
    /*
     * 
     */
    
    /**
     * To be used within schedLock only.
     * 
     * Derived from java.util.concurrent.ScheduledThreadPoolExecutor
     * (where it is a static AtomicLong, which we don't need since
     * we only use it to compare schedules of a same scheduler).
     * 
     * Sequence number to break scheduling ties, and in turn to
     * guarantee FIFO order among tied entries.
     */
    private long sequencer = 0;

    /**
     * True if must work during call to startAndWorkInCurrentThread().
     */
    private final boolean isThreadless;

    /**
     * If isThreadless is true, guarded by stateMutex,
     * else not guarded since effectively immutable after instance construction.
     */
    private final Thread[] workerThreadArr;
    
    /**
     * For isWorkerThread().
     * Identity because user might override threads hashCode().
     * 
     * If isThreadless is true, guarded by stateMutex,
     * else not guarded since effectively immutable after instance construction.
     */
    private final IdentityHashMap<Thread,Boolean> workerThreadSet;
    
    /**
     * If isThreadless is true, guarded by stateMutex,
     * else not guarded since effectively immutable after instance construction.
     */
    private final MyWorkerRunnable[] workerRunnables;

    /**
     * Number of workers that went started.
     * This number is only incremented when a worker gets running
     * for its first time, and never decremented.
     */
    private final AtomicInteger nbrOfStartedWorkers = new AtomicInteger();
    
    /**
     * Number of running workers.
     * A worker can become non-running due to an exception thrown by a
     * runnable, but then be called again and re-become running.
     */
    private final AtomicInteger nbrOfRunningWorkers = new AtomicInteger();
    
    /*
     * 
     */
    
    /**
     * Guards acceptSchedulesStatus and processSchedulesStatus transitions,
     * and eventually workers related collections.
     */
    private final Object stateMutex = new Object();
    
    /*
     * 
     */
    
    private static final int ACCEPT_SCHEDULES_YES = 0;
    private static final int ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED = 1;
    private static final int ACCEPT_SCHEDULES_NO = 2;
    private static final int ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED = 3;
    /**
     * To be set only from within constructor or stateMutex.
     * 
     * 0 for YES status, for fast checks in this general case.
     * 
     * We use schedules acceptance status variable to also hold the information that worker threads start
     * (which involves "this" publication) is needed, to avoid doing it in constructor (while this is not
     * yet fully constructed), or having to check yet another volatile variable to know if it's needed.
     * 
     * Without this trick, just to check schedules acceptance, we could just use a volatile boolean
     * instead of this atomic integer.
     * 
     * Possible transitions:
     * - YES -> NO (stopping acceptance)
     * - YES_AND -> YES (accepting a schedule) or NO_AND (stopping acceptance)
     * - NO -> YES (starting acceptance)
     * - NO_AND -> YES_AND (starting acceptance)
     */
    private volatile int acceptSchedulesStatus;

    /*
     * 
     */
    
    private static final int PROCESS_SCHEDULES_YES = 0;
    private static final int PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS = 1;
    private static final int PROCESS_SCHEDULES_NO = 2;
    private static final int PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS = 3;
    /**
     * To be set only from within constructor or stateMutex.
     * 
     * 0 for YES status, for fast checks in this general case.
     * 
     * Possible transitions:
     * - YES -> NO (stopping processing) or YES_AND (shutdown)
     * - YES_AND -> NO_AND (stopping processing after shutdown)
     * - NO -> YES (starting processing) or NO_AND (starting processing after shutdown)
     * - NO_AND -> YES_AND (starting processing after shutdown)
     */
    private volatile int processSchedulesStatus;

    /*
     * 
     */
    
    /**
     * Capacity of ASAP queue for the user, whatever the current
     * inner capacity of its implementation, which might eventually grow.
     */
    private final int asapQueueCapacity;
    
    /**
     * Capacity of timed queue for the user, whatever the current
     * inner capacity of its implementation, which might eventually grow.
     */
    private final int timedQueueCapacity;

    /**
     * Guarded by schedLock.
     */
    private final MyAsapQueue asapSchedQueue = new MyAsapQueue();

    /**
     * Guarded by schedLock.
     */
    private final MyFairPriorityQueue timedSchedQueue =
            new MyFairPriorityQueue();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /*
     * Threadless instances.
     */
    
    /**
     * @param clock Hard clock to use.
     * @return A scheduler working during call to startAndWorkInCurrentThread(),
     *         that guarantees FIFO order for ASAP schedules,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newThreadlessInstance(InterfaceHardClock clock) {
        return newThreadlessInstance(
                clock,
                DEFAULT_ASAP_QUEUE_CAPACITY,
                DEFAULT_TIMED_QUEUE_CAPACITY);
    }
    
    /**
     * @param clock Hard clock to use.
     * @param asapQueueCapacity Capacity (>=0) for ASAP schedules queue.
     *        When full, new schedules are canceled.
     * @param timedQueueCapacity Capacity (>=0) for timed schedules queue.
     *        When full, new schedules are canceled.
     * @return A scheduler working during call to startAndWorkInCurrentThread(),
     *         that guarantees FIFO order for ASAP schedules.
     */
    public static HardScheduler newThreadlessInstance(
            InterfaceHardClock clock,
            int asapQueueCapacity,
            int timedQueueCapacity) {
        return new HardScheduler(
                true, // isThreadless
                clock,
                null, // threadNamePrefix
                false, // daemon
                1, // nbrOfThreads
                asapQueueCapacity,
                timedQueueCapacity,
                null); // threadFactory
    }
    
    /*
     * Single-threaded instances.
     */

    /**
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @return A single-threaded scheduler,
     *         that guarantees FIFO order for ASAP schedules,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newSingleThreadedInstance(
            InterfaceHardClock clock,
            String threadNamePrefix,
            boolean daemon) {
        return newSingleThreadedInstance(
                clock,
                threadNamePrefix,
                daemon,
                null); // threadFactory
    }

    /**
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param threadFactory If null, default threads are created.
     * @return A single-threaded scheduler,
     *         that guarantees FIFO order for ASAP schedules,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newSingleThreadedInstance(
            InterfaceHardClock clock,
            String threadNamePrefix,
            boolean daemon,
            ThreadFactory threadFactory) {
        return newInstance(
                clock,
                threadNamePrefix,
                daemon,
                1, // nbrOfThreads
                threadFactory);
    }
    
    /*
     * Instances using possibly multiple threads.
     */

    /**
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param nbrOfThreads Number of threads to use. Must be >= 1.
     * @return A scheduler using the specified number of threads,
     *         that guarantees FIFO order for ASAP schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newInstance(
            InterfaceHardClock clock,
            String threadNamePrefix,
            boolean daemon,
            int nbrOfThreads) {
        return newInstance(
                clock,
                threadNamePrefix,
                daemon,
                nbrOfThreads,
                null); // threadFactory
    }

    /**
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param nbrOfThreads Number of threads to use. Must be >= 1.
     * @param threadFactory If null, default threads are created.
     * @return A scheduler using the specified number of threads,
     *         that guarantees FIFO order for ASAP schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newInstance(
            InterfaceHardClock clock,
            String threadNamePrefix,
            boolean daemon,
            int nbrOfThreads,
            ThreadFactory threadFactory) {
        return newInstance(
                clock,
                threadNamePrefix,
                daemon,
                nbrOfThreads,
                DEFAULT_ASAP_QUEUE_CAPACITY,
                DEFAULT_TIMED_QUEUE_CAPACITY,
                threadFactory);
    }

    /**
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param nbrOfThreads Number of threads to use. Must be >= 1.
     * @param asapQueueCapacity Capacity (>=0) for ASAP schedules queue.
     *        When full, new schedules are canceled.
     * @param timedQueueCapacity Capacity (>=0) for timed schedules queue.
     *        When full, new schedules are canceled.
     * @param threadFactory If null, default threads are created.
     * @return A scheduler using the specified number of threads,
     *         that guarantees FIFO order for ASAP schedules only if single-threaded.
     */
    public static HardScheduler newInstance(
            InterfaceHardClock clock,
            String threadNamePrefix,
            boolean daemon,
            int nbrOfThreads,
            int asapQueueCapacity,
            int timedQueueCapacity,
            ThreadFactory threadFactory) {
        return new HardScheduler(
                false, // isThreadless
                clock,
                threadNamePrefix,
                daemon,
                nbrOfThreads,
                asapQueueCapacity,
                timedQueueCapacity,
                threadFactory);
    }

    /*
     * 
     */
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[");
        sb.append(super.toString());
        sb.append(",clock:");
        sb.append(this.clock);

        final int acceptStatus = this.acceptSchedulesStatus;
        final int processStatus = this.processSchedulesStatus;
        
        if (isShutdown(processStatus)) {
            sb.append(",shutdown");
        }
        
        sb.append(",running:");
        sb.append(this.getNbrOfRunningWorkers());
        sb.append(",working:");
        sb.append(this.getNbrOfWorkingWorkers());
        sb.append(",idle:");
        sb.append(this.getNbrOfIdleWorkers());

        // Numbers of ASAP and timed schedules might not
        // be coherent, for in case of common workers,
        // they are retrieved in different schedLock calls,
        // but that shouldn't hurt, since in case of specific
        // workers there can be no coherence anyway.
        sb.append(",ASAP:");
        sb.append(this.getNbrOfPendingAsapSchedules());
        sb.append(",timed:");
        sb.append(this.getNbrOfPendingTimedSchedules());

        sb.append(",accepting:");
        sb.append(mustAcceptSchedules(acceptStatus));
        sb.append(",processing:");
        sb.append(mustProcessSchedules(processStatus));
        sb.append("]");
        
        return sb.toString();
    }

    /*
     * configuration
     */

    /**
     * Max system time to wait, when waiting a clock's time (not system time),
     * which can be done by worker threads, or waitXXX methods of this class.
     * 
     * This setting can be helpful even when using listenable clocks,
     * for system time might jump around typically without notification,
     * or clocks with variable time speed.
     * 
     * Default is Long.MAX_VALUE.
     * 
     * @param maxSystemWaitTimeNs Max system time to wait, in nanoseconds.
     *        Must be > 0.
     */
    public void setMaxSystemWaitTimeNs(long maxSystemWaitTimeNs) {
        this.schedClockTimeCondilock.setMaxSystemWaitTimeNs(maxSystemWaitTimeNs);
        this.noRunningWorkerClockTimeCondilock.setMaxSystemWaitTimeNs(maxSystemWaitTimeNs);
    }
    
    /*
     * 
     */
    
    @Override
    public boolean isWorkerThread() {
        final Thread currentThread = Thread.currentThread();
        if (this.isThreadless) {
            synchronized (this.stateMutex) {
                return this.workerThreadSet.containsKey(currentThread);
            }
        } else {
            // Effectively immutable after scheduler's construction,
            // so no need to synchronize or such.
            return this.workerThreadSet.containsKey(currentThread);
        }
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
     * getters
     */

    /**
     * @return The clock used by this scheduler.
     */
    @Override
    public InterfaceHardClock getClock() {
        return this.clock;
    }

    /**
     * @return The number of worker threads in work method (either idle or working).
     */
    public int getNbrOfRunningWorkers() {
        return this.nbrOfRunningWorkers.get();
    }

    /**
     * @return An estimation of the number of worker threads waiting for work.
     */
    public int getNbrOfIdleWorkers() {
        /*
         * We could count threads queued to acquire schedLock but don't,
         * because:
         * - schedLock can also be locked by user threads, which would include
         *   them in the count,
         * - schedLock should not take much time to acquire, since no
         *   significant treatments are done within it; so we can lag a bit
         *   and count corresponding workers as still being working.
         */
        final ReentrantLock schedLock = this.schedLock;
        schedLock.lock();
        try {
            return schedLock.getWaitQueueLength(this.schedCondition);
        } finally {
            schedLock.unlock();
        }
    }

    /**
     * @return An estimation of the number of worker threads processing runnables,
     *         i.e. number of running workers - number of idle workers.
     *         This estimation is ensured to be >= 0.
     */
    public int getNbrOfWorkingWorkers() {
        // Max for safety.
        return Math.max(0, this.getNbrOfRunningWorkers() - this.getNbrOfIdleWorkers());
    }

    /**
     * @return The number of pending schedules.
     */
    public int getNbrOfPendingSchedules() {
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            return this.asapSchedQueue.size() + this.timedSchedQueue.size();
        } finally {
            schedLock.unlock();
        }
    }

    /**
     * @return The number of pending ASAP schedules.
     */
    public int getNbrOfPendingAsapSchedules() {
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            return this.asapSchedQueue.size();
        } finally {
            schedLock.unlock();
        }
    }

    /**
     * @return The number of pending timed schedules.
     */
    public int getNbrOfPendingTimedSchedules() {
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            return this.timedSchedQueue.size();
        } finally {
            schedLock.unlock();
        }
    }

    /**
     * @return True if shutdown() or shutdownNow(...) has been called, false otherwise.
     */
    public boolean isShutdown() {
        return isShutdown(this.processSchedulesStatus);
    }
    
    /*
     * controls
     */
    
    /**
     * Blocks until no more running workers, uninterruptibly.
     * 
     * If already shutdown, does nothing (other than checks).
     * 
     * If throws from user code, this method can be called again,
     * which will each time ensure an inner call to start() (as for first call),
     * in case stop() or similar methods have been called in the mean time.
     * 
     * @throws IllegalStateException if this scheduler is not threadless.
     */
    public void startAndWorkInCurrentThread() {
        this.checkThreadless();

        final Thread workerThread = Thread.currentThread();
        final MyWorkerRunnable workerRunnable;

        synchronized (this.stateMutex) {
            final int acceptStatus = this.acceptSchedulesStatus;
            if (this.isShutdown()) {
                return;
            }
            
            if (isWorkersStartNeeded(acceptStatus)) {
                if (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                    this.acceptSchedulesStatus = ACCEPT_SCHEDULES_YES;
                } else if (acceptStatus == ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED) {
                    this.acceptSchedulesStatus = ACCEPT_SCHEDULES_NO;
                }
                
                this.workerThreadArr[0] = workerThread;
                this.workerThreadSet.put(workerThread, Boolean.TRUE);
                
                workerRunnable = new MyWorkerRunnable();
                this.workerRunnables[0] = workerRunnable;
            } else {
                workerRunnable = this.workerRunnables[0];
            }

            // start() is thread-safe, but doing it in this
            // synchronized block for less interleaving.
            this.start();
        }
        
        try {
            /*
             * Working, uninterruptibly (we assume interrupts are
             * only addressed to user code, not to scheduler code).
             */
            workerRunnable.run();
        } finally {
            synchronized (this.stateMutex) {
                this.workerThreadSet.remove(workerThread);
            }
        }
    }
    
    /**
     * Starts worker threads if not yet started, unless if shutdown,
     * or must use user calling thread, in which case it has no effect.
     * 
     * @return True if worker threads were started by this call,
     *         false if they were already started (whether or not
     *         they died later) or must be the user calling thread.
     * @throws IllegalStateException if this scheduler is threadless.
     */
    public boolean startWorkerThreadsIfNeeded() {
        this.checkNotThreadless();
        
        synchronized (this.stateMutex) {
            if (this.isShutdown()) {
                return false;
            }
            final int acceptStatus = this.acceptSchedulesStatus;
            if (!isWorkersStartNeeded(acceptStatus)) {
                // Worker threads already started.
                return false;
            }
            if (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_YES;
            } else if (acceptStatus == ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED) {
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_NO;
            }
            if (this.isThreadless) {
                return false;
            } else {
                this.startWorkerThreads_stateLocked();
                return true;
            }
        }
    }

    /**
     * Starts both schedules processing and schedules acceptance,
     * unless if shutdown, in which case only schedules processing
     * is started.
     */
    public void start() {
        // Getting ready.
        this.startProcessing();
        // Opening the doors.
        this.startAccepting();
    }

    /**
     * Stops both schedules acceptance and schedules processing.
     * 
     * If a schedule was being submitted while this method was being called,
     * it might remain pending, so you might (or might not) want to cancel
     * pending schedules afterwards.
     */
    public void stop() {
        // Closing the doors.
        this.stopAccepting();
        // Taking a nap.
        this.stopProcessing();
    }

    /**
     * Starting schedules (or self re-schedules) acceptance,
     * unless if shutdown, in which case it has no effect.
     */
    public void startAccepting() {
        synchronized (this.stateMutex) {
            if (this.isShutdown()) {
                // Need not to start acceptance.
                return;
            }
            final int acceptStatus = this.acceptSchedulesStatus;
            if (acceptStatus == ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED) {
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED;
            } else if (acceptStatus == ACCEPT_SCHEDULES_NO) {
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_YES;
            }
        }
    }

    /**
     * Stops schedules (or self re-schedules) acceptance:
     * new schedules are rejected (onCancel() method called).
     */
    public void stopAccepting() {
        synchronized (this.stateMutex) {
            if (this.isShutdown()) {
                // Acceptance already stopped.
                return;
            }
            final int acceptStatus = this.acceptSchedulesStatus;
            if (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED;
            } else if (acceptStatus == ACCEPT_SCHEDULES_YES) {
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_NO;
            }
        }
    }

    /**
     * Tells workers to process pending schedules (if any).
     * 
     * If shutdown and schedules processing stopped,
     * it is still possible to restart schedules processing, but since workers
     * might have already all died, this might have no effect.
     */
    public void startProcessing() {
        boolean mustSignal = false;
        synchronized (this.stateMutex) {
            final boolean wasStopped;
            final int processStatus = this.processSchedulesStatus;
            if (processStatus == PROCESS_SCHEDULES_NO) {
                this.processSchedulesStatus = PROCESS_SCHEDULES_YES;
                wasStopped = true;
            } else if (processStatus == PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS) {
                this.processSchedulesStatus = PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS;
                wasStopped = true;
            } else {
                // YES or YES_AND already
                wasStopped = false;
            }
            // Needs to be retrieved in stateMutex, since workers
            // are started in stateMutex.
            final boolean threadsStarted = !this.isWorkersStartNeeded();
            mustSignal = wasStopped && threadsStarted;
        }
        if (mustSignal) {
            // Signaling eventually waiting workers, for them
            // to start processing eventual pending schedules.
            this.signalWorkersForWork();
        }
    }

    /**
     * Tells workers to get on strike after their current work (if any).
     */
    public void stopProcessing() {
        synchronized (this.stateMutex) {
            final int processStatus = this.processSchedulesStatus;
            if (processStatus == PROCESS_SCHEDULES_YES) {
                this.processSchedulesStatus = PROCESS_SCHEDULES_NO;
            } else if (processStatus == PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS) {
                this.processSchedulesStatus = PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS;
            }
        }
    }

    /**
     * Cancels currently pending schedules, directly calling
     * onCancel() methods on corresponding cancellables.
     * 
     * If a same cancellable has multiple pending schedules, onCancel()
     * method is called as many times.
     * 
     * If an exception is throw by an onCancel method,
     * cancellation is stopped, so you might want to call
     * this method again to continue it.
     * 
     * A lock/unlock is done to retrieve each schedule,
     * to make sure onCancel() method is not called in lock,
     * and for other schedules to remain in queue if it
     * throws an exception.
     * To avoid too high contention while canceling,
     * you might therefore want to either:
     * - stop schedules acceptance and processing before,
     * - drain cancellables, and cancel them yourself.
     */
    public void cancelPendingSchedules() {
        this.cancelPendingAsapSchedules();
        this.cancelPendingTimedSchedules();
    }

    /**
     * Cancels currently pending ASAP schedules, directly calling
     * onCancel() methods on corresponding cancellables.
     * 
     * If a same cancellable has multiple pending ASAP schedules, onCancel()
     * method is called as many times.
     */
    public void cancelPendingAsapSchedules() {
        MySequencedSchedule schedule;
        while ((schedule = this.pollAsapScheduleInLock()) != null) {
            SchedUtils.call_onCancel_IfCancellable(schedule.getRunnable());
        }
    }
    
    /**
     * Cancels currently pending timed schedules, directly calling
     * onCancel() methods on corresponding cancellables.
     * 
     * If a same cancellable has multiple pending timed schedules, onCancel()
     * method is called as many times.
     */
    public void cancelPendingTimedSchedules() {
        MyTimedSchedule schedule = null;
        while ((schedule = this.pollTimedScheduleInLock()) != null) {
            SchedUtils.call_onCancel_IfCancellable(schedule.getRunnable());
        }
    }
    
    /**
     * @param runnables Collection where to add runnables
     *        of drained pending ASAP schedules,
     *        in the order they were scheduled.
     */
    public void drainPendingAsapRunnablesInto(Collection<? super Runnable> runnables) {
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            final int n = this.asapSchedQueue.size();
            for (int i = 0; i < n; i++) {
                final MySequencedSchedule schedule = this.asapSchedQueue.remove();
                runnables.add(schedule.getRunnable());
            }
            if (n != 0) {
                this.schedCondition.signalAll();
            }
        } finally {
            schedLock.unlock();
        }
    }

    /**
     * @param runnables Collection where to add runnables
     *        of drained pending timed schedules.
     */
    public void drainPendingTimedRunnablesInto(Collection<? super Runnable> runnables) {
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            final int n = this.timedSchedQueue.size();
            for (int i = 0; i < n; i++) {
                final MyTimedSchedule schedule = this.timedSchedQueue.remove();
                runnables.add(schedule.getRunnable());
            }
            if (n != 0) {
                this.schedCondition.signalAll();
            }
        } finally {
            schedLock.unlock();
        }
    }

    /**
     * Interrupts worker threads.
     * 
     * This method can be called in attempt to interrupt, if any,
     * current processings of runnables by worker threads.
     * 
     * If using calling thread as worker thread, and user call
     * has not been done already, does nothing.
     */
    public void interruptWorkers() {
        if (this.isThreadless) {
            synchronized (this.stateMutex) {
                final Thread workerThread = this.workerThreadArr[0];
                if (workerThread == null) {
                    /*
                     * User call not yet done
                     */
                } else {
                    workerThread.interrupt();
                }
            }
        } else {
            final Thread[] workerThreadArr = this.workerThreadArr;
            final int nbrOfThreads = this.workerThreadArr.length;
            for (int i = 0; i < nbrOfThreads; i++) {
                final Thread workerThread = workerThreadArr[i];
                workerThread.interrupt();
            }
        }
    }

    /**
     * This method irremediably stops schedules acceptance, and
     * makes each worker runnable return normally (i.e. die) when
     * it has no more schedule to process (ASAP or timed).
     * 
     * After call to this method, and in the purpose of quickening workers death,
     * you might want (or not want) to (and in that order if calling both):
     * - cancel or drain pending schedules (ASAP and timed),
     * - interrupt workers.
     * 
     * If shutdown, and there are no more running workers:
     * - you might wait to cancel or drain remaining pending schedules, or they
     *   will remain pending forever,
     * - if there are no more pending schedules, this scheduler can be considered terminated.
     */
    public void shutdown() {
        final boolean actualRequest;
        synchronized (this.stateMutex) {
            final int processStatus = this.processSchedulesStatus;
            actualRequest = !isShutdown(processStatus);
            if (actualRequest) {
                // NO, whether or not worker threads have been started already.
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_NO;

                if (processStatus == PROCESS_SCHEDULES_YES) {
                    this.processSchedulesStatus = PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS;
                } else {
                    this.processSchedulesStatus = PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS;
                }
            }
        }
        if (actualRequest) {
            // Signaling eventually waiting workers, for them to start dying if needed.
            this.signalWorkersForDeath();
        }
    }

    /**
     * Convenience method, implemented with other public methods:
     * calls shutdown(), then stopProcessing(), then eventually
     * interruptWorkers(), and then methods to drain pending schedules.
     */
    public List<Runnable> shutdownNow(boolean mustInterruptWorkingWorkers) {
        // Never more accepting, and dying when idle and not waiting for a timed schedule's time.
        this.shutdown();
        // No more processing (we will drain pending schedules).
        this.stopProcessing();
        if (mustInterruptWorkingWorkers) {
            // Poking eventual working worker.
            this.interruptWorkers();
        }
        
        /*
         * Draining pending runnables.
         */
        
        // Optimistic initial capacity.
        final int estimatedNbrToDrain = this.getNbrOfPendingSchedules();
        final ArrayList<Runnable> runnables = new ArrayList<Runnable>(estimatedNbrToDrain);
        
        this.drainPendingAsapRunnablesInto(runnables);
        this.drainPendingTimedRunnablesInto(runnables);
    
        return runnables;
    }

    /*
     * waits
     */
    
    /**
     * Waits for no more worker to be running, or for the specified timeout (in system time) to elapse.
     * 
     * @param timeoutNs Timeout, in system time, in nanoseconds.
     * @return True if there was no more running worker before the timeout elapsed, false otherwise.
     * @throws InterruptedException if the wait gets interrupted.
     */
    public boolean waitForNoMoreRunningWorkerSystemTimeNs(long timeoutNs) throws InterruptedException {
        return this.noRunningWorkerSystemTimeCondilock.awaitNanosWhileFalseInLock(
                this.noRunningWorkerBooleanCondition,
                timeoutNs);
    }
    
    /**
     * Waits for no more worker to be running, or for the specified timeout (in clock's time) to elapse.
     * 
     * @param timeoutNs Max duration to wait, in clock time, in nanoseconds.
     */
    public boolean waitForNoMoreRunningWorkerClockTimeNs(long timeoutNs) throws InterruptedException {
        incrementClockWaitersCountAndAddListenerIfNeeded();
        try {
            return this.noRunningWorkerClockTimeCondilock.awaitNanosWhileFalseInLock(
                    this.noRunningWorkerBooleanCondition,
                    timeoutNs);
        } finally {
            decrementClockWaiterCountAndRemoveListenerIfNeeded();
        }
    }

    /*
     * scheduling
     */

    @Override
    public void execute(Runnable runnable) {
        final MySequencedSchedule schedule = newAsapSchedule(runnable);

        if (this.enqueueScheduleIfPossible(schedule)) {
            this.tryStartWorkerThreadsOnScheduleSubmit();
            this.enqueueScheduleIfPossible(schedule);
        }
    }

    @Override
    public void executeAtNs(
            Runnable runnable,
            long timeNs) {
        final MyTimedSchedule schedule = newTimedSchedule(runnable, timeNs);

        if (this.enqueueScheduleIfPossible(schedule)) {
            this.tryStartWorkerThreadsOnScheduleSubmit();
            this.enqueueScheduleIfPossible(schedule);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @throws IllegalStateException if this scheduler is not threadless.
     */
    private void checkThreadless() {
        if (!this.isThreadless) {
            throw new IllegalStateException("this scheduler is not threadless");
        }
    }

    /**
     * @throws IllegalStateException if this scheduler is threadless.
     */
    private void checkNotThreadless() {
        if (this.isThreadless) {
            throw new IllegalStateException("this scheduler is threadless");
        }
    }
    
    /**
     * @param isThreadless If true, no worker thread is created, and
     *        user calling thread is used as worker thread, during
     *        startAndWorkInCurrentThread() method call.
     */
    private HardScheduler(
            boolean isThreadless,
            final InterfaceHardClock clock,
            final String threadNamePrefix,
            boolean daemon,
            int nbrOfThreads,
            int asapQueueCapacity,
            int timedQueueCapacity,
            final ThreadFactory threadFactory) {
        
        if (nbrOfThreads < 1) {
            throw new IllegalArgumentException("number of threads [" + nbrOfThreads + "] must be >= 1");
        }
        
        if (asapQueueCapacity <= 0) {
            throw new IllegalArgumentException("ASAP queue capacity [" + asapQueueCapacity + "] must be > 0");
        }
        if (timedQueueCapacity <= 0) {
            throw new IllegalArgumentException("timed queue capacity [" + asapQueueCapacity + "] must be > 0");
        }
        
        if (isThreadless) {
            final boolean instanceAdaptedForUserWorkerThread =
                    (threadNamePrefix == null)
                    && (nbrOfThreads == 1)
                    && (threadFactory == null);
            if (!instanceAdaptedForUserWorkerThread) {
                // Must not happen (due to internal code).
                throw new AssertionError();
            }
        }
        
        this.isThreadless = isThreadless;
        
        this.asapQueueCapacity = asapQueueCapacity;
        this.timedQueueCapacity = timedQueueCapacity;

        this.clock = clock;

        /*
         * 
         */
        
        this.schedClockTimeCondilock = new HardClockCondilock(clock, this.schedSystemTimeCondilock);
        this.noRunningWorkerClockTimeCondilock = new HardClockCondilock(clock, this.noRunningWorkerSystemTimeCondilock);

        this.schedClockTimeCondilock.setMaxSystemWaitTimeNs(DEFAULT_MAX_SYSTEM_WAIT_TIME_NS);
        this.noRunningWorkerClockTimeCondilock.setMaxSystemWaitTimeNs(DEFAULT_MAX_SYSTEM_WAIT_TIME_NS);

        /*
         * 
         */
        
        this.workerThreadArr = new Thread[nbrOfThreads];
        this.workerThreadSet = new IdentityHashMap<Thread,Boolean>(nbrOfThreads);
        this.workerRunnables = new MyWorkerRunnable[nbrOfThreads];
        if (isThreadless) {
            // Will be initialized on start.
        } else {
            for (int i = 0; i < nbrOfThreads; i++) {
                final MyWorkerRunnable runnable = new MyWorkerRunnable();
                
                final Thread thread;
                if (threadFactory != null) {
                    thread = threadFactory.newThread(runnable);
                } else {
                    thread = new Thread(runnable);
                }
                thread.setDaemon(daemon);
                if (threadNamePrefix != null) {
                    final int threadNum = i+1;
                    thread.setName(threadNamePrefix + "-" + threadNum);
                }
                
                this.workerThreadArr[i] = thread;
                this.workerThreadSet.put(thread, Boolean.TRUE);
                this.workerRunnables[i] = runnable;
            }
        }
        
        this.acceptSchedulesStatus = ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED;
        
        this.processSchedulesStatus = PROCESS_SCHEDULES_YES;
    }
    
    private void incrementClockWaitersCountAndAddListenerIfNeeded() {
        synchronized (this.stateMutex) {
            this.incrementClockWaiterCountAndAddListenerIfNeeded_stateLocked();
        }
    }
    
    private void decrementClockWaiterCountAndRemoveListenerIfNeeded() {
        synchronized (this.stateMutex) {
            this.decrementClockWaitersCountAndRemoveListenerIfNeeded_stateLocked();
        }
    }

    private void incrementClockWaiterCountAndAddListenerIfNeeded_stateLocked() {
        if (++this.clockWaiterCount == 1) {
            ClocksUtils.addListenerToAll(this.clock, this.clockListener);
        }
    }
    
    private void decrementClockWaitersCountAndRemoveListenerIfNeeded_stateLocked() {
        if (--this.clockWaiterCount == 0) {
            ClocksUtils.removeListenerFromAll(this.clock, this.clockListener);
        }
    }
    
    /*
     * 
     */

    private static boolean mustAcceptSchedules(int acceptStatus) {
        // YES tested before, to optimize the general case.
        return (acceptStatus == ACCEPT_SCHEDULES_YES)
        || (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED);
    }
    
    /*
     * 
     */
    
    private boolean mustProcessSchedules() {
        return mustProcessSchedules(this.processSchedulesStatus);
    }
    
    private static boolean mustProcessSchedules(int processStatus) {
        // YES tested before, to optimize the general case.
        return (processStatus == PROCESS_SCHEDULES_YES)
        || (processStatus == PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS);
    }
    
    /*
     * 
     */
    
    private boolean isWorkersStartNeeded() {
        return isWorkersStartNeeded(this.acceptSchedulesStatus);
    }
    
    private static boolean isWorkersStartNeeded(int acceptStatus) {
        return (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED)
        || (acceptStatus == ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED);
    }
    
    /*
     * 
     */
    
    private static boolean isShutdown(int processStatus) {
        return (processStatus == PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS)
        || (processStatus == PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS);
    }
    
    /*
     * 
     */

    private MySequencedSchedule pollAsapScheduleInLock() {
        final MySequencedSchedule ret;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            ret = this.asapSchedQueue.poll();
            if (ret != null) {
                this.schedCondition.signalAll();
            }
        } finally {
            schedLock.unlock();
        }
        return ret;
    }

    private MyTimedSchedule pollTimedScheduleInLock() {
        final MyTimedSchedule ret;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            ret = this.timedSchedQueue.poll();
            if (ret != null) {
                this.schedCondition.signalAll();
            }
        } finally {
            schedLock.unlock();
        }
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * Must not be called if isThreadless is false.
     */
    private void startWorkerThreads_stateLocked() {
        if (this.isThreadless) {
            throw new AssertionError();
        }
        for (int i = 0; i < this.workerThreadArr.length; i++) {
            this.workerThreadArr[i].start();
        }
    }

    /**
     * Usually called outside stateMutex, to reduce locks intertwining
     * and stateMutex's locking time, but could be called within.
     */
    private void signalWorkersForWork() {
        if (COMMON_SIGNAL_CHAINING) {
            this.schedSystemTimeCondilock.signalInLock();
        } else {
            this.schedSystemTimeCondilock.signalAllInLock();
        }
    }

    /**
     * Usually called outside stateMutex, to reduce locks interleaving
     * and stateMutex's locking time, but could be called within.
     */
    private void signalWorkersForDeath() {
        this.schedSystemTimeCondilock.signalAllInLock();
    }

    /**
     * If more than one runnable is processable,
     * signals condition before returning,
     * to eventually wake up a waiting worker.
     * 
     * @return Number of pending ASAP schedules that might be processed by the worker (>= 0),
     *         or Integer.MIN_VALUE if the worker shall die.
     */
    private int waitForWorkOrDeath_schedLocked() {
        while (true) {
            /*
             * Passing here either:
             * - On initial call.
             * - After a schedule's execution.
             * - After end of a wait (for a schedule, or, if the worker processes
             *   timed schedules, for a schedule's theoretical time).
             */

            final int nbrOfProcessablesAsapSchedules = asapSchedQueue.size();
            // Integer.MAX_VALUE+Integer.MAX_VALUE = -2, so it's OK to compare against 0.
            final int nbrOfProcessablesSchedules = timedSchedQueue.size() + nbrOfProcessablesAsapSchedules;
            if (nbrOfProcessablesSchedules != 0) {
                if (mustProcessSchedules()) {
                    if (COMMON_SIGNAL_CHAINING) {
                        if (nbrOfProcessablesSchedules > 1) {
                            // Signaling other worker, if any, to take care of the other
                            // processable schedule.
                            schedCondition.signal();
                        }
                    }
                    return nbrOfProcessablesAsapSchedules;
                }
            } else {
                if (isShutdown()) {
                    // Here current worker starts to die.
                    return Integer.MIN_VALUE;
                }
            }
            
            try {
                // Waiting for a runnable,
                // or for being supposed to work,
                // or for death.
                schedCondition.await();
            } catch (InterruptedException e) {
                // Quiet.
            }
        }
    }
    
    /**
     * @param nbrOfAsapToProcess ASAP queue size.
     * @param firstTimedSched Can be null.
     * @param timeAfterWaitNs Only used if firstTimedSched is not null.
     * @return The ASAP schedule to process now, NOT removed from the list,
     *         or null if no ASAP schedule must be process yet.
     */
    private MySequencedSchedule getAsapScheduleToProcessElseNull_schedLocked(
            int nbrOfAsapToProcess,
            MyTimedSchedule firstTimedSched,
            long timeAfterWaitNs) {
        final MySequencedSchedule ret;
        if (nbrOfAsapToProcess > 0) {
            // Not null.
            final MySequencedSchedule firstAsapSched =
                    this.asapSchedQueue.first();
            if (firstTimedSched != null) {
                final boolean isTimedScheduleEligible =
                        (timeAfterWaitNs >= firstTimedSched.getTheoreticalTimeNs());
                if (isTimedScheduleEligible) {
                    // Comparing sequence numbers to decide
                    // whether to execute ASAP or timed runnable.
                    final int cmp = compareSequenceNumbers(
                            firstAsapSched.getSequenceNumber(),
                            firstTimedSched.getSequenceNumber());
                    final boolean mustProcessAsapElseTimed = (cmp < 0);
                    if (mustProcessAsapElseTimed) {
                        ret = firstAsapSched;
                    } else {
                        ret = null;
                    }
                } else {
                    ret = firstAsapSched;
                }
            } else {
                ret = firstAsapSched;
            }
        } else {
            ret = null;
        }
        return ret;
    }
    
    private void workerRun() {
        final Lock schedLock = this.schedLock;
        while (true) {
            Runnable runnable;
            
            schedLock.lock();
            try {
                // Loop to avoid getting out-and-in synchronization
                // when we are done waiting for a schedule's time.
                while (true) {
                    final int nbrOfAsapToProcess;
                    {
                        final int nbrOfAsapToProcessOrNeg = waitForWorkOrDeath_schedLocked();
                        if (nbrOfAsapToProcessOrNeg < 0) {
                            return;
                        }
                        nbrOfAsapToProcess = nbrOfAsapToProcessOrNeg;
                    }
                    // Here, we have schedule(s) in queues, and we are supposed to work.
                    
                    // Peeking earliest timed schedule if any.
                    final MyTimedSchedule firstTimedSched;
                    final long timeAfterWaitNs;
                    if (this.timedSchedQueue.size() != 0) {
                        final long nowNs = this.clock.getTimeNs();
                        this.timedSchedQueue.moveCurrentSchedulesToTheirQueue(nowNs);
                        firstTimedSched = this.timedSchedQueue.peek();
                        if (firstTimedSched != null) {
                            timeAfterWaitNs = nowNs;
                        } else {
                            // Won't be used this time.
                            timeAfterWaitNs = Long.MIN_VALUE;
                        }
                    } else {
                        firstTimedSched = null;
                        // Won't be used this time.
                        timeAfterWaitNs = Long.MIN_VALUE;
                    }
                    
                    final MySequencedSchedule asapSchedToProcess =
                            this.getAsapScheduleToProcessElseNull_schedLocked(
                                    nbrOfAsapToProcess,
                                    firstTimedSched,
                                    timeAfterWaitNs);
                    
                    if (asapSchedToProcess != null) {
                        // ASAP FIFO.
                        final MySequencedSchedule forCheck = this.asapSchedQueue.remove();
                        if(AZZERTIONS)LangUtils.azzert(forCheck == asapSchedToProcess);
                        runnable = asapSchedToProcess.getRunnable();
                    } else {
                        // If we pass here, that means an eligible schedule is in
                        // timed queue.
                        if(AZZERTIONS)LangUtils.azzert(firstTimedSched != null);
                        
                        // Wait time (in clock time) before schedule time.
                        final long clockWaitTimeNs = NumbersUtils.minusBounded(
                                firstTimedSched.getTheoreticalTimeNs(),
                                timeAfterWaitNs);
                        if (clockWaitTimeNs > 0) {
                            try {
                                this.schedClockTimeCondilock.awaitNanos(clockWaitTimeNs);
                            } catch (InterruptedException e) {
                                // quiet
                            }
                            /*
                             * Here, 4 things might have happened:
                             * - Waiting time elapsed.
                             * - Notification of a new schedule, or of start/stop,
                             *   or clock modification, occurred.
                             * - Spurious wake-up occurred.
                             * - Waiting was interrupted.
                             */

                            // Will check for eventual new schedules.
                            continue;
                        }
                        
                        // Runnable will be called now: removing it from the queue.
                        final MySequencedSchedule forCheck = this.timedSchedQueue.remove();
                        if(AZZERTIONS)LangUtils.azzert(forCheck == firstTimedSched);
                        runnable = firstTimedSched.getRunnable();
                    }
                    // Existing anti-synchro loop.
                    break;
                } // End while.
            } finally {
                schedLock.unlock();
            }

            /*
             * 
             */

            runnable.run();
        } // End while.
    }

    /*
     * 
     */
    
    private static MySequencedSchedule newAsapSchedule(
            final Runnable runnable) {
        // Creating new schedule whether or not
        // it will be accepted (queue might be full),
        // to minimize treatments in lock.
        final MySequencedSchedule schedule = new MySequencedSchedule(runnable);
        return schedule;
    }
    
    private static MyTimedSchedule newTimedSchedule(
            final Runnable runnable,
            long timeNs) {
        // Creating new schedule whether or not
        // it will be accepted (queue might be full),
        // to minimize treatments in lock.
        final MyTimedSchedule schedule = new MyTimedSchedule(
                runnable,
                timeNs);
        return schedule;
    }
    
    /*
     * 
     */

    /**
     * Starts worker threads if the status retrieved from within stateMutex is
     * ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED.
     */
    private void tryStartWorkerThreadsOnScheduleSubmit() {
        synchronized (this.stateMutex) {
            // If shutdown, accept status is NO,
            // so no need to check for shutdown.
            final int acceptStatusInMutex = this.acceptSchedulesStatus;
            if (acceptStatusInMutex == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                // Starting even if processing is stopped (not to have
                // to eventually start thread when starting processing).
                this.startWorkerThreads_stateLocked();
                this.acceptSchedulesStatus = ACCEPT_SCHEDULES_YES;
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * @return True if runnable could be enqueued, false otherwise.
     */
    private boolean enqueueAsapScheduleIfRoom_schedLocked(MySequencedSchedule schedule) {
        if (this.asapSchedQueue.size() == this.asapQueueCapacity) {
            return false;
        }
        schedule.setSequenceNumber(this.sequencer++);
        return this.asapSchedQueue.add(schedule);
    }

    /**
     * Sets sequence number before queuing.
     * @return True if schedule could be enqueued, false otherwise.
     */
    private boolean enqueueTimedScheduleIfRoom_schedLocked(MyTimedSchedule schedule) {
        if (this.timedSchedQueue.size() == this.timedQueueCapacity) {
            return false;
        }
        schedule.setSequenceNumber(this.sequencer++);
        return this.timedSchedQueue.add(schedule);
    }
    
    /**
     * Enqueues if room and accepted, and if workers are started
     * or some calling thread be used as worker later.
     * Cancels the schedule if could not enqueue due to no room
     * or not being accepted.
     * 
     * @param schedule ASAP or timed schedule, depending on type.
     * @return True if need workers start for retry, false otherwise.
     */
    private boolean enqueueScheduleIfPossible(
            MySequencedSchedule schedule) {
        
        boolean enqueued = false;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            /*
             * Need to test schedules acceptance within schedLock while enqueuing,
             * else we could have the following:
             * - Some user or worker thread is in the process of submitting a schedule,
             *   but is not yet in schedLock.
             * - Some thread calls shutdownNow(...).
             * - The previous thread finally enqueues its schedule,
             *   and some worker thread will wait forever for schedules processing
             *   to be started so that it can process it.
             */
            final int acceptStatus = this.acceptSchedulesStatus;
            final boolean mustTryToEnqueue;
            if (acceptStatus == ACCEPT_SCHEDULES_YES) {
                // General case first.
                mustTryToEnqueue = true;
            } else {
                if (this.isThreadless) {
                    mustTryToEnqueue = (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED);
                } else {
                    if (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                        /*
                         * Must try to start workers and call again.
                         */
                        return true;
                    }
                    mustTryToEnqueue = false;
                }
            }
            if (mustTryToEnqueue) {
                if (schedule instanceof MyTimedSchedule) {
                    enqueued = this.enqueueTimedScheduleIfRoom_schedLocked((MyTimedSchedule) schedule);
                } else {
                    enqueued = this.enqueueAsapScheduleIfRoom_schedLocked(schedule);
                }
            }
            if (enqueued) {
                // Only need to signal one worker (if any is waiting),
                // since a same waiting thread can't be notified multiple times
                // (if not, would have to use signalAll() for cases where multiple
                // workers might be waiting, and multiple new schedules eligibles).
                this.schedCondition.signal();
            } else {
                // will cancel outside lock (unless exception while trying to enqueue,
                // but then it will go up the call stack and notify the user that there
                // was a problem)
            }
        } finally {
            schedLock.unlock();
        }

        if (!enqueued) {
            SchedUtils.call_onCancel_IfCancellable(schedule.getRunnable());
        }
        
        return false;
    }
}
