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
package net.jolikit.time.sched.hard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.PostPaddedAtomicInteger;
import net.jolikit.threading.basics.CancellableUtils;
import net.jolikit.threading.basics.WorkerThreadChecker;
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

/**
 * Scheduler based on a hard clock, and on possibly multiple
 * worker threads, common for ASAP and timed schedules.
 * 
 * Main differences with JDK's ScheduledThreadPoolExecutor
 * (other than those listed in InterfaceScheduler javadoc):
 * - more:
 *   - Usually better throughput (but only noticeable for tiny tasks),
 *     especially for ASAP schedules in case of single or just a few
 *     worker threads (using a simpler queue in these cases).
 *   - Methods to start/stop schedules acceptance and processing
 *     by worker threads.
 *   - Methods to cancel or drain pending schedules.
 *   - Handles scheduling according to a clock which time speed might change,
 *     as well as time (other than due to real time flowing).
 * - different:
 *   - Uses fixed threads instead of a thread pool.
 *   - Threads naming and deamon flag are specified aside from
 *     the thread factory, not to have to create or configure
 *     a specific one for these very situational settings.
 *   - When queue reaches max capacity, runnables are rejected
 *     instead of applying backpressure by having execute() to block.
 *     This allows to remove risks of deadlocks when execute()
 *     is called by worker threads, and to be notified of the overload
 *     and adapt accordingly.
 *   - If the runnable implements InterfaceCancellable,
 *     on rejection by execute() the onCancel() method is called
 *     instead of throwing RejectedExecutionException.
 *     This allows to use rejection as a normal and lighter mechanism
 *     (for example instead of applying backpressure, as described above).
 *   - Thread interrupts are means to interrupt user code being executed,
 *     while in TPE interrupts are used for workers management
 *     and locks are used to protect user code against them.
 *   - Scheduling fairness: when multiple schedules (ASAP or timed)
 *     are eligible to be processed, the one that has been scheduled first
 *     is processed first. This prevents newly submitted timed schedules
 *     for a time far in the past to undefinately postpone the processing
 *     of schedules long done for a higher time inferior to current time,
 *     and same between ASAP and timed schedules depending on which
 *     would have priority over the other without this fairness mechanism.
 * 
 * It might be a bad idea to allow for independent acceptance or processing
 * of ASAP and timed schedules, since ASAP or timed schedules treatments
 * might respectively do timed and ASAP schedules, so no such feature
 * is provided.
 * 
 * Workers runnables don't catch any throwable, so worker threads might die
 * on the first exception thrown by a runnable, but workers runnables
 * are designed such as if a runnable throws an exception, calling
 * worker's runnable again will make it continue working properly.
 * Making sure that worker's runnables are called again after they threw
 * an exception can be done easily, using a thread factory that properly
 * wraps worker's runnables with runnables containing proper try/catch
 * and loop.
 * After completing normally, a worker's runnable is not supposed to be
 * called again (by whatever runnable a thread factory could wrap around it),
 * else workers normal completion would not be possible.
 * 
 * Threads are started lazily or by call to startWorkerThreadsIfNeeded().
 * 
 * Listening to clocks modifications is automatically removed when no more
 * worker thread is running and no clock time waitXxx method is being used.
 * Add/removal of listener for clocks modifications is done within a state lock,
 * and involves calling addListener, removeListener and getMasterClock methods
 * of listenable and enslaved clocks: to avoid deadlock possibilities, implementations
 * of these methods should therefore never try to acquire a lock in which another
 * thread could be concurrently calling a method triggering such add/removal,
 * i.e. a clock time waitXxx method of this class, or worker's runnables.
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
     * - clock's modification lock > stateMutex > noRunningWorkerMutex > asapPutLock >= schedLock
     * 
     * Except clock's modification lock, which is an external lock,
     * all of these locks can be acquired by user thread when calling
     * public methods of this class.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean AZZERTIONS = false;
    
    /**
     * Same as default in PriorityQueue.
     */
    private static final int INITIAL_PRIORITY_QUEUE_CAPACITY = 11;
    
    private static final int DEFAULT_ASAP_QUEUE_CAPACITY = Integer.MAX_VALUE;
    private static final int DEFAULT_TIMED_QUEUE_CAPACITY = Integer.MAX_VALUE;
    
    /**
     * Above a few workers, benches get slower with basic queue in case of
     * single publisher, and much slower as worker count increases.
     */
    private static final int DEFAULT_MAX_WORKER_COUNT_FOR_BASIC_ASAP_QUEUE = 4;
    
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
            if (mustProcessSchedules(getProcessSchedulesStatus())) {
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
        @Override
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
     * Node hack: Using a same object for both ASAP schedule
     * and linked queue node, to avoid having to create
     * two objects for each ASAP schedule.
     * 
     * Caution: Due to queue structure, this causes the runnable
     * to change node/schedule on remove, and poll to return
     * a different instance than peek, but holding
     * the same runnable and sequence number.
     */
    protected static class MyNode extends MySequencedSchedule {
        /**
         * Next node if any, null if last node
         * (including when empty, i.e. head = last).
         */
        MyNode next;
        public MyNode(Runnable runnable) {
            super(runnable);
        }
    }
    
    /**
     * Derived from LinkedBlockingQueue,
     * with locks acquiring moved out of it,
     * and no notFull usage (using offer, not blocking put).
     * 
     * In practice, the put lock is either asapPutLock or schedLock,
     * and the take lock is schedLock.
     * 
     * The extended AtomicInteger holds the size for
     * concurrent (dual lock) implementation
     * (avoiding a separate object to reduce risk of cache miss).
     */
    private static abstract class MyAbstractAsapQueue extends PostPaddedAtomicInteger {
        private static final long serialVersionUID = 1L;
        final int capacity;
        private boolean queueEmptyBeforeLastAdd = false;
        private boolean queueNotEmptyAfterLastRemove = false;
        /**
         * Used on take.
         */
        private MyNode head;
        /**
         * Used on put.
         */
        private MyNode last;
        public MyAbstractAsapQueue(int capacity) {
            this.capacity = capacity;
            this.last = this.head = new MyNode(null);
        }
        /*
         * Methods usable in the put lock or in the take lock
         * (i.e. in the lock if they are the same,
         * else typically callable concurrently).
         */
        public abstract int size();
        /*
         * Methods to be used in the put lock.
         */
        /**
         * @return True if could enqueue (i.f. was not full).
         */
        public abstract boolean offerLast(MyNode node);
        void addLast_structure(MyNode node) {
            // assert last.next == null;
            this.last = this.last.next = node;
        }
        /**
         * Allows to know whether signaling of take lock must be done,
         * since it's not done by the queue.
         * 
         * @return True if the queue was empty just before the last add,
         *         false otherwise.
         */
        public final boolean wasEmptyBeforeLastAdd() {
            return this.queueEmptyBeforeLastAdd;
        }
        /**
         * To be called on add.
         */
        final void setWasEmptyBeforeLastAdd(boolean val) {
            this.queueEmptyBeforeLastAdd = val;
        }
        /*
         * Methods to be used in the take lock.
         */
        public abstract MySequencedSchedule peekFirst();
        /**
         * Node hack: Not supposed to be called when empty,
         * but accidentally works like a peekFirst_structure()
         * (i.e. returns null if empty),
         * due to using same object for node and schedule.
         */
        MySequencedSchedule getFirst_structure() {
            return this.head.next;
        }
        /**
         * Node hack: Always returns another instance than peekFirst(),
         * but holding the same runnable and sequence number.
         */
        public abstract MySequencedSchedule pollFirst();
        /**
         * Node hack: Always returns another instance than getFirst_structure(),
         * but holding the same runnable and sequence number.
         */
        MySequencedSchedule removeFirst_structure() {
            // assert head.item == null;
            /*
             * Removing head node, and first node
             * gets to be used as new head.
             */
            MyNode h = this.head;
            MyNode first = h.next;
            first.drainDataInto(h);
            h.next = h; // help GC
            this.head = first;
            return h;
        }
        /**
         * Allows to know whether signaling of the take lock must be done,
         * since it's not done by the queue.
         * 
         * @return True if the queue was not empty just after the last removal,
         *         false otherwise.
         */
        public final boolean wasNotEmptyAfterLastRemove() {
            return this.queueNotEmptyAfterLastRemove;
        }
        /**
         * To be called on removal.
         */
        final void setWasNotEmptyAfterLastRemove(boolean val) {
            this.queueNotEmptyAfterLastRemove = val;
        }
    }
    
    /*
     * 
     */
    
    /**
     * Implementation guarded by a single lock.
     */
    private static class MyBasicAsapQueue extends MyAbstractAsapQueue {
        private static final long serialVersionUID = 1L;
        private int size = 0;
        public MyBasicAsapQueue(int capacity) {
            super(capacity);
        }
        @Override
        public int size() {
            return this.size;
        }
        @Override
        public boolean offerLast(MyNode node) {
            int c = -1;
            if (this.size < this.capacity) {
                this.addLast_structure(node);
                c = this.size++;
                this.setWasEmptyBeforeLastAdd(c == 0);
            }
            return c >= 0;
        }
        @Override
        public MySequencedSchedule peekFirst() {
            /*
             * Node hack: don't need size check, but still using it
             * to avoid cache miss on head when empty.
             */
            MySequencedSchedule ret = null;
            if (this.size > 0) {
                ret = this.getFirst_structure();
            }
            return ret;
        }
        @Override
        public MySequencedSchedule pollFirst() {
            MySequencedSchedule ret = null;
            if (this.size > 0) {
                ret = this.removeFirst_structure();
                final int oldSize = this.size--;
                this.setWasNotEmptyAfterLastRemove(oldSize > 1);
            }
            return ret;
        }
    }
    
    /*
     * 
     */
    
    /**
     * Implementation guarded by a put lock and a take lock,
     * with concurrently readable size.
     */
    private static class MyDualLockAsapQueue extends MyAbstractAsapQueue {
        private static final long serialVersionUID = 1L;
        public MyDualLockAsapQueue(int capacity) {
            super(capacity);
        }
        @Override
        public int size() {
            final AtomicInteger sizeAto = this;
            return sizeAto.get();
        }
        @Override
        public boolean offerLast(MyNode node) {
            final AtomicInteger sizeAto = this;
            int oldSize = -1;
            if (sizeAto.get() < this.capacity) {
                this.addLast_structure(node);
                oldSize = sizeAto.getAndIncrement();
                this.setWasEmptyBeforeLastAdd(oldSize == 0);
            }
            return (oldSize >= 0);
        }
        @Override
        public MySequencedSchedule peekFirst() {
            final AtomicInteger sizeAto = this;
            MySequencedSchedule ret = null;
            if (sizeAto.get() > 0) {
                ret = this.getFirst_structure();
            }
            return ret;
        }
        @Override
        public MySequencedSchedule pollFirst() {
            final AtomicInteger sizeAto = this;
            MySequencedSchedule ret = null;
            if (sizeAto.get() > 0) {
                ret = this.removeFirst_structure();
                final int oldSize = sizeAto.getAndDecrement();
                this.setWasNotEmptyAfterLastRemove(oldSize > 1);
            }
            return ret;
        }
    }
    
    /*
     * 
     */
    
    /**
     * PriorityQueue allowing to use different priorities
     * depending on whether the schedules are to be executed
     * in the future (using theoretical time), or if they are
     * eligible to be executed now (using sequence number).
     */
    private static class MyFairPriorityQueue {
        private final int capacity;
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
        public MyFairPriorityQueue(int capacity) {
            this.capacity = capacity;
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
        public boolean offer(MyTimedSchedule sched) {
            if (this.size() == this.capacity) {
                return false;
            }
            return this.futureQueue.add(sched);
        }
        public MyTimedSchedule peek() {
            MyTimedSchedule ret = this.currentQueue.peek();
            if (ret == null) {
                ret = this.futureQueue.peek();
            }
            return ret;
        }
        public MyTimedSchedule poll() {
            MyTimedSchedule ret = this.currentQueue.poll();
            if (ret == null) {
                ret = this.futureQueue.poll();
            }
            return ret;
        }
        public MyTimedSchedule remove() {
            MyTimedSchedule ret = this.currentQueue.poll();
            if (ret == null) {
                ret = this.futureQueue.remove();
            }
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MyNoRunningWorkerBC noRunningWorkerBooleanCondition =
        new MyNoRunningWorkerBC();
    
    private final InterfaceHardClock clock;
    
    /*
     * schedLock
     */
    
    /**
     * Lock waited on by workers,
     * and also acquired by publishers.
     */
    private final ReentrantLock schedLock = new ReentrantLock();
    private final Condition schedCondition = this.schedLock.newCondition();
    private final InterfaceCondilock schedSystemTimeCondilock =
        new LockCondilock(this.schedLock, this.schedCondition);
    private final HardClockCondilock schedClockTimeCondilock;
    
    /*
     * asapPutLock
     */
    
    private final ReentrantLock asapPutLock;
    
    /*
     * noRunningWorkerMutex
     */
    
    private final Object noRunningWorkerMutex = new Object();
    private final InterfaceCondilock noRunningWorkerSystemTimeCondilock =
        new MonitorCondilock(this.noRunningWorkerMutex);
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
    private final AtomicInteger acceptSchedulesStatus =
        new PostPaddedAtomicInteger();
    
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
    private final AtomicInteger processSchedulesStatus =
        new PostPaddedAtomicInteger();
    
    /*
     * 
     */
    
    /**
     * Guarded by schedLock and possibly asapPutLock
     * depending on the implementation.
     */
    private final MyAbstractAsapQueue asapSchedQueue;
    
    /**
     * Guarded by schedLock.
     */
    private final MyFairPriorityQueue timedSchedQueue;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Complete constructor for non-threadless instances.
     * Constructs a scheduler using the specified number of worker threads,
     * that guarantees FIFO order for ASAP schedules only if single-threaded.
     * 
     * Allows to extend this class, and to use non-default
     * worker count thresholds for ASAP queues types.
     * 
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @param asapQueueCapacity Capacity (>=0) for ASAP schedules queue.
     *        When full, new schedules are rejected.
     * @param timedQueueCapacity Capacity (>=0) for timed schedules queue.
     *        When full, new schedules are rejected.
     * @param maxWorkerCountForBasicAsapQueue Must be >= 0.
     *        When worker count is strictly superior to this value,
     *        an ASAP queue more suited to high number of workers is used.
     *        Default for newXxx() construction methods is 4.
     * @param threadFactory If null, default threads are created.
     */
    public HardScheduler(
        InterfaceHardClock clock,
        String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads,
        int asapQueueCapacity,
        int timedQueueCapacity,
        int maxWorkerCountForBasicAsapQueue,
        ThreadFactory threadFactory) {
        this(
            false, // isThreadless
            clock,
            threadNamePrefix,
            daemon,
            nbrOfThreads,
            asapQueueCapacity,
            timedQueueCapacity,
            maxWorkerCountForBasicAsapQueue,
            threadFactory);
    }
    
    /**
     * Complete constructor for threadless instances.
     * Guarantees FIFO order for schedules,
     * since only caller thread is used for work.
     * 
     * Allows to extend this class, and to use non-default
     * worker count thresholds for ASAP queues types.
     * 
     * @param asapQueueCapacity Capacity (>=0) for ASAP schedules queue.
     *        When full, new schedules are rejected.
     * @param timedQueueCapacity Capacity (>=0) for timed schedules queue.
     *        When full, new schedules are rejected.
     * @param maxWorkerCountForBasicAsapQueue Must be >= 0.
     *        When worker count is strictly superior to this value,
     *        an ASAP queue more suited to high number of workers is used.
     *        Default for newXxx() construction methods is 4.
     */
    public HardScheduler(
        InterfaceHardClock clock,
        int asapQueueCapacity,
        int timedQueueCapacity,
        int maxWorkerCountForBasicAsapQueue) {
        this(
            true, // isThreadless
            clock,
            null, // threadNamePrefix
            null, // daemon
            1, // nbrOfThreads
            asapQueueCapacity,
            timedQueueCapacity,
            maxWorkerCountForBasicAsapQueue,
            null); // threadFactory
    }
    
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
     *        When full, new schedules are rejected.
     * @param timedQueueCapacity Capacity (>=0) for timed schedules queue.
     *        When full, new schedules are rejected.
     * @return A scheduler working during call to startAndWorkInCurrentThread(),
     *         that guarantees FIFO order for ASAP schedules.
     */
    public static HardScheduler newThreadlessInstance(
        InterfaceHardClock clock,
        int asapQueueCapacity,
        int timedQueueCapacity) {
        return new HardScheduler(
            clock,
            asapQueueCapacity,
            timedQueueCapacity,
            DEFAULT_MAX_WORKER_COUNT_FOR_BASIC_ASAP_QUEUE);
    }
    
    /*
     * Single-threaded instances.
     */
    
    /**
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @return A single-threaded scheduler,
     *         that guarantees FIFO order for ASAP schedules,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newSingleThreadedInstance(
        InterfaceHardClock clock,
        String threadNamePrefix,
        Boolean daemon) {
        return newSingleThreadedInstance(
            clock,
            threadNamePrefix,
            daemon,
            null); // threadFactory
    }
    
    /**
     * @param clock Hard clock to use.
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param threadFactory If null, default threads are created.
     * @return A single-threaded scheduler,
     *         that guarantees FIFO order for ASAP schedules,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newSingleThreadedInstance(
        InterfaceHardClock clock,
        String threadNamePrefix,
        Boolean daemon,
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
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @return A scheduler using the specified number of worker threads,
     *         that guarantees FIFO order for ASAP schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newInstance(
        InterfaceHardClock clock,
        String threadNamePrefix,
        Boolean daemon,
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
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @param threadFactory If null, default threads are created.
     * @return A scheduler using the specified number of worker threads,
     *         that guarantees FIFO order for ASAP schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queues capacities.
     */
    public static HardScheduler newInstance(
        InterfaceHardClock clock,
        String threadNamePrefix,
        Boolean daemon,
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
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @param asapQueueCapacity Capacity (>=0) for ASAP schedules queue.
     *        When full, new schedules are rejected.
     * @param timedQueueCapacity Capacity (>=0) for timed schedules queue.
     *        When full, new schedules are rejected.
     * @param threadFactory If null, default threads are created.
     * @return A scheduler using the specified number of worker threads,
     *         that guarantees FIFO order for ASAP schedules only if single-threaded.
     */
    public static HardScheduler newInstance(
        InterfaceHardClock clock,
        String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads,
        int asapQueueCapacity,
        int timedQueueCapacity,
        ThreadFactory threadFactory) {
        return new HardScheduler(
            clock,
            threadNamePrefix,
            daemon,
            nbrOfThreads,
            asapQueueCapacity,
            timedQueueCapacity,
            DEFAULT_MAX_WORKER_COUNT_FOR_BASIC_ASAP_QUEUE,
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
        
        final int acceptStatus = this.getAcceptSchedulesStatus();
        final int processStatus = this.getProcessSchedulesStatus();
        
        if (isShutdown(processStatus)) {
            sb.append(",shutdown");
        }
        
        sb.append(",running:");
        sb.append(this.getNbrOfRunningWorkers());
        sb.append(",working:");
        sb.append(this.getNbrOfWorkingWorkers());
        sb.append(",idle:");
        sb.append(this.getNbrOfIdleWorkers());
        
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
     * @return The number of worker threads specified to the constructor,
     *         and 1 for threadless instances.
     */
    public int getNbrOfWorkers() {
        return this.workerThreadArr.length;
    }
    
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
     * @return The number of pending schedules,
     *         or Integer.MAX_VALUE if it overflows int range.
     */
    public int getNbrOfPendingSchedules() {
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            return Math.max(0,
                this.asapSchedQueue.size()
                + this.timedSchedQueue.size());
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
        return isShutdown(this.getProcessSchedulesStatus());
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
            final int acceptStatus = this.getAcceptSchedulesStatus();
            if (this.isShutdown()) {
                return;
            }
            
            if (isWorkersStartNeeded(acceptStatus)) {
                if (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                    this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_YES);
                } else if (acceptStatus == ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED) {
                    this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_NO);
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
            final int acceptStatus = this.getAcceptSchedulesStatus();
            if (!isWorkersStartNeeded(acceptStatus)) {
                // Worker threads already started.
                return false;
            }
            if (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_YES);
            } else if (acceptStatus == ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED) {
                this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_NO);
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
            final int acceptStatus = this.getAcceptSchedulesStatus();
            if (acceptStatus == ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED) {
                this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED);
            } else if (acceptStatus == ACCEPT_SCHEDULES_NO) {
                this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_YES);
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
            final int acceptStatus = this.getAcceptSchedulesStatus();
            if (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_NO_AND_WORKERS_START_NEEDED);
            } else if (acceptStatus == ACCEPT_SCHEDULES_YES) {
                this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_NO);
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
            final int processStatus = this.getProcessSchedulesStatus();
            if (processStatus == PROCESS_SCHEDULES_NO) {
                this.setProcessSchedulesStatus(PROCESS_SCHEDULES_YES);
                wasStopped = true;
            } else if (processStatus == PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS) {
                this.setProcessSchedulesStatus(PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS);
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
            this.signalAllWorkersInSchedLock();
        }
    }
    
    /**
     * Tells workers to get on strike after their current work (if any).
     */
    public void stopProcessing() {
        synchronized (this.stateMutex) {
            final int processStatus = this.getProcessSchedulesStatus();
            if (processStatus == PROCESS_SCHEDULES_YES) {
                this.setProcessSchedulesStatus(PROCESS_SCHEDULES_NO);
            } else if (processStatus == PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS) {
                this.setProcessSchedulesStatus(PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS);
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
        while ((schedule = this.pollFirstAsapScheduleInSchedLock()) != null) {
            CancellableUtils.call_onCancel_IfCancellable(
                schedule.removeRunnable());
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
        while ((schedule = this.pollTimedScheduleInSchedLock()) != null) {
            CancellableUtils.call_onCancel_IfCancellable(
                schedule.removeRunnable());
        }
    }
    
    /**
     * @param runnables Collection where to add runnables
     *        of drained pending ASAP schedules,
     *        in the order they were scheduled.
     */
    public void drainPendingAsapRunnablesInto(Collection<? super Runnable> runnables) {
        boolean gotSome = false;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            while (true) {
                final MySequencedSchedule schedule =
                    this.asapSchedQueue.pollFirst();
                if (schedule == null) {
                    break;
                }
                gotSome = true;
                runnables.add(schedule.removeRunnable());
            }
        } finally {
            if (gotSome) {
                // Signaling in finally, in case we had an exception
                // while adding into output collection.
                this.signalAllWorkersAfterSchedulesRemoval();
            }
            schedLock.unlock();
        }
    }
    
    /**
     * @param runnables Collection where to add runnables
     *        of drained pending timed schedules.
     */
    public void drainPendingTimedRunnablesInto(Collection<? super Runnable> runnables) {
        int n = 0;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            n = this.timedSchedQueue.size();
            for (int i = 0; i < n; i++) {
                final MyTimedSchedule schedule = this.timedSchedQueue.remove();
                runnables.add(schedule.removeRunnable());
            }
        } finally {
            if (n != 0) {
                // Signaling in finally, in case we had an exception
                // while adding into output collection.
                this.signalAllWorkersAfterSchedulesRemoval();
            }
            schedLock.unlock();
        }
    }
    
    /**
     * Interrupts worker threads if any, for the purpose of interrupting
     * user treatments (this scheduler only using uninterruptible waits).
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
     * makes each worker runnable complete normally when
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
     * - if there are no more pending schedules,
     *   this scheduler can be considered terminated.
     */
    public void shutdown() {
        final boolean actualRequest;
        synchronized (this.stateMutex) {
            final int processStatus = this.getProcessSchedulesStatus();
            actualRequest = !isShutdown(processStatus);
            if (actualRequest) {
                /*
                 * Changing accept status in both asapPutLock and schedLock,
                 * to avoid concurrency with execute()'s
                 * "if (accept) { enqueue(); }" logic,
                 * which could lead to enqueuing a runnable
                 * after all workers died (it could then
                 * still be drained, but would not have been
                 * properly rejected or cancelled on submit,
                 * nor drained by a shutdownNow() call).
                 * NB: It is not mandatory to do that if asapPutLock
                 * and schedLock are the same, because then
                 * workers are not doing any concurrent bookkeeping
                 * and shutdownNow()'s drain would be waiting
                 * for the lock to be released by execute(),
                 * but we still take it in this case for consistency.
                 */
                final boolean mustLockBoth =
                    !this.isSchedLock(this.asapPutLock);
                if (mustLockBoth) {
                    this.asapPutLock.lock();
                }
                this.schedLock.lock();
                try {
                    // NO, whether or not worker threads have been started already.
                    this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_NO);
                    
                    if (processStatus == PROCESS_SCHEDULES_YES) {
                        this.setProcessSchedulesStatus(PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS);
                    } else {
                        this.setProcessSchedulesStatus(PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS);
                    }
                } finally {
                    this.schedLock.unlock();
                    if (mustLockBoth) {
                        this.asapPutLock.unlock();
                    }
                }
            }
        }
        if (actualRequest) {
            // Signaling eventually waiting workers,
            // for them to start dying if needed.
            this.signalAllWorkersInSchedLock();
        }
    }
    
    /**
     * Convenience method, implemented with other public methods:
     * calls shutdown(), then stopProcessing(), then eventually
     * interruptWorkers(), and then methods to drain pending schedules.
     */
    public List<Runnable> shutdownNow(boolean mustInterruptWorkingWorkers) {
        // Never more accepting, and dying when idle
        // and not waiting for a timed schedule's time.
        this.shutdown();
        // No more processing (we will drain pending schedules).
        this.stopProcessing();
        if (mustInterruptWorkingWorkers) {
            // Poking eventual working workers.
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
     * Waits for no more worker to be running,
     * or for the specified timeout (in system time) to elapse.
     * 
     * @param timeoutNs Timeout, in system time, in nanoseconds.
     * @return True if there was no more running worker before
     *         the timeout elapsed, false otherwise.
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
        
        LangUtils.requireNonNull(runnable);
        
        // Optimistically allocated outside putLock.
        final MySequencedSchedule schedule = new MyNode(runnable);
        
        if (this.enqueueScheduleIfPossible(schedule)) {
            this.tryStartWorkerThreadsOnScheduleSubmit();
            this.enqueueScheduleIfPossible(schedule);
        }
    }
    
    @Override
    public void executeAtNs(
        Runnable runnable,
        long timeNs) {
        
        LangUtils.requireNonNull(runnable);
        
        // Optimistically allocated outside putLock.
        final MyTimedSchedule schedule = new MyTimedSchedule(
            runnable,
            timeNs);
        
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
    
    private boolean isSchedLock(Lock lock) {
        return (lock == this.schedLock);
    }
    
    private Lock getLockForPut(boolean timed) {
        // If timed schedule, will use timed queue, so always using schedLock.
        return (timed ? this.schedLock : this.asapPutLock);
    }
    
    /*
     * 
     */
    
    /**
     * @param isThreadless If true, no worker thread is created, and
     *        user calling thread is used as worker thread, during
     *        startAndWorkInCurrentThread() method call.
     */
    private HardScheduler(
        boolean isThreadless,
        final InterfaceHardClock clock,
        final String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads,
        int asapQueueCapacity,
        int timedQueueCapacity,
        int maxWorkerCountForBasicAsapQueue,
        final ThreadFactory threadFactory) {
        
        NbrsUtils.requireSupOrEq(1, nbrOfThreads, "nbrOfThreads");
        
        NbrsUtils.requireSup(0, asapQueueCapacity, "asapQueueCapacity");
        NbrsUtils.requireSup(0, timedQueueCapacity, "timedQueueCapacity");
        
        NbrsUtils.requireSupOrEq(
            0,
            maxWorkerCountForBasicAsapQueue,
            "maxWorkerCountForBasicAsapQueue");
        
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
        
        this.clock = LangUtils.requireNonNull(clock);
        
        /*
         * 
         */
        
        this.schedClockTimeCondilock =
            new HardClockCondilock(clock, this.schedSystemTimeCondilock);
        this.noRunningWorkerClockTimeCondilock =
            new HardClockCondilock(clock, this.noRunningWorkerSystemTimeCondilock);
        
        this.schedClockTimeCondilock.setMaxSystemWaitTimeNs(
            DEFAULT_MAX_SYSTEM_WAIT_TIME_NS);
        this.noRunningWorkerClockTimeCondilock.setMaxSystemWaitTimeNs(
            DEFAULT_MAX_SYSTEM_WAIT_TIME_NS);
        
        /*
         * 
         */
        
        final boolean mustUseBasicAsapQueue =
            (nbrOfThreads <= maxWorkerCountForBasicAsapQueue);
        if (mustUseBasicAsapQueue) {
            this.asapPutLock = this.schedLock;
            this.asapSchedQueue = new MyBasicAsapQueue(
                asapQueueCapacity);
        } else {
            this.asapPutLock = new ReentrantLock();
            this.asapSchedQueue = new MyDualLockAsapQueue(
                asapQueueCapacity);
        }
        
        this.timedSchedQueue = new MyFairPriorityQueue(timedQueueCapacity);
        
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
                if (daemon != null) {
                    thread.setDaemon(daemon);
                }
                if (threadNamePrefix != null) {
                    final int threadNum = i+1;
                    thread.setName(threadNamePrefix + "-" + threadNum);
                }
                
                this.workerThreadArr[i] = thread;
                this.workerThreadSet.put(thread, Boolean.TRUE);
                this.workerRunnables[i] = runnable;
            }
        }
        
        this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED);
        
        this.setProcessSchedulesStatus(PROCESS_SCHEDULES_YES);
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
    
    private int getAcceptSchedulesStatus() {
        return this.acceptSchedulesStatus.get();
    }
    
    private void setAcceptSchedulesStatus(int status) {
        this.acceptSchedulesStatus.set(status);
    }
    
    private static boolean mustAcceptSchedules(int acceptStatus) {
        // YES tested before, to optimize for the general case.
        return (acceptStatus == ACCEPT_SCHEDULES_YES)
            || (acceptStatus == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED);
    }
    
    /*
     * 
     */
    
    private int getProcessSchedulesStatus() {
        return this.processSchedulesStatus.get();
    }
    
    private void setProcessSchedulesStatus(int status) {
        this.processSchedulesStatus.set(status);
    }
    
    private static boolean mustProcessSchedules(int processStatus) {
        // YES tested before, to optimize for the general case.
        return (processStatus == PROCESS_SCHEDULES_YES)
            || (processStatus == PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS);
    }
    
    /*
     * 
     */
    
    private boolean isWorkersStartNeeded() {
        return isWorkersStartNeeded(this.getAcceptSchedulesStatus());
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
    
    private MySequencedSchedule pollFirstAsapScheduleInSchedLock() {
        final MySequencedSchedule ret;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            ret = this.asapSchedQueue.pollFirst();
            /*
             * Only need to signal if became empty,
             * in case workers were waiting to be allowed to process
             * remaining schedules to complete after a shutdown.
             */
            if ((ret != null)
                && (!this.asapSchedQueue.wasNotEmptyAfterLastRemove())) {
                this.signalAllWorkersAfterSchedulesRemoval();
            }
        } finally {
            schedLock.unlock();
        }
        return ret;
    }
    
    private MyTimedSchedule pollTimedScheduleInSchedLock() {
        final MyTimedSchedule ret;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            ret = this.timedSchedQueue.poll();
            if (ret != null) {
                this.signalAllWorkersAfterSchedulesRemoval();
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
     * Must be called after schedules drain or cancelling,
     * to wake up workers that would be waiting to be allowed
     * to process them before completing after a shutdown,
     * or waiting for the time to process a timed schedule.
     */
    private void signalAllWorkersAfterSchedulesRemoval() {
        this.schedCondition.signalAll();
    }
    
    /**
     * Usually called outside stateMutex, to reduce locks nesting
     * and stateMutex's locking time, but could be called within.
     */
    private void signalAllWorkersInSchedLock() {
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
            
            final int nbrOfProcessablesAsapSchedules =
                this.asapSchedQueue.size();
            // Integer.MAX_VALUE+Integer.MAX_VALUE = -2, so it's OK to compare against 0.
            final int nbrOfProcessablesSchedules =
                this.timedSchedQueue.size() + nbrOfProcessablesAsapSchedules;
            final int processStatus = this.getProcessSchedulesStatus();
            if (nbrOfProcessablesSchedules != 0) {
                if (mustProcessSchedules(processStatus)) {
                    return nbrOfProcessablesAsapSchedules;
                }
            } else {
                if (isShutdown(processStatus)) {
                    // Here current worker starts to die.
                    return Integer.MIN_VALUE;
                }
            }
            
            /*
             * Waiting for a runnable,
             * or for being supposed to work,
             * or for death.
             * 
             * NB: Instead here we could call awaitUninterruptibly(),
             * to prevent idle workers to be pointlessly awoken
             * when user calls interruptWorkers(),
             * and then Thread.interrupted() to protect new runnables
             * against old interrupts,
             * but we prefer to call a single method and
             * get interrupt status cleared immediately if not useful.
             */
            try {
                this.schedCondition.await();
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                /*
                 * Not restoring interrupt status,
                 * else next runnable to process would get interrupted.
                 */
            }
        }
    }
    
    /**
     * @param nbrOfAsapToProcess ASAP queue size.
     * @param firstTimedSched Can be null.
     * @param timeAfterWaitNs Only used if firstTimedSched is not null.
     * @return True if got an ASAP schedule to process now,
     *         or false if no ASAP schedule must be process yet.
     */
    private boolean getAsapScheduleToProcessNow_schedLocked(
        int nbrOfAsapToProcess,
        MyTimedSchedule firstTimedSched,
        long timeAfterWaitNs) {
        final boolean ret;
        if (nbrOfAsapToProcess > 0) {
            // Not null.
            final MySequencedSchedule firstAsapSched =
                this.asapSchedQueue.peekFirst();
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
                    ret = mustProcessAsapElseTimed;
                } else {
                    ret = true;
                }
            } else {
                ret = true;
            }
        } else {
            ret = false;
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
                        final int nbrOfAsapToProcessOrNeg =
                            this.waitForWorkOrDeath_schedLocked();
                        if (nbrOfAsapToProcessOrNeg < 0) {
                            return;
                        }
                        nbrOfAsapToProcess = nbrOfAsapToProcessOrNeg;
                    }
                    // Here, we have schedule(s) in queues,
                    // and we are supposed to work.
                    
                    final int oldTimedQueueSize = this.timedSchedQueue.size();
                    
                    // Peeking earliest timed schedule if any.
                    final MyTimedSchedule firstTimedSched;
                    final long timeAfterWaitNs;
                    if (oldTimedQueueSize != 0) {
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
                    
                    final boolean mustProcessFirstAsapSchedule =
                        this.getAsapScheduleToProcessNow_schedLocked(
                            nbrOfAsapToProcess,
                            firstTimedSched,
                            timeAfterWaitNs);
                    
                    if (mustProcessFirstAsapSchedule) {
                        // Not null.
                        final MySequencedSchedule asapSchedToProcess =
                            this.asapSchedQueue.pollFirst();
                        if (this.asapSchedQueue.wasNotEmptyAfterLastRemove()) {
                            this.schedCondition.signal();
                        }
                        runnable = asapSchedToProcess.removeRunnable();
                    } else {
                        // If we pass here, that means an eligible schedule is in
                        // timed queue.
                        if(AZZERTIONS)LangUtils.azzert(firstTimedSched != null);
                        
                        // Wait time (in clock time) before schedule time.
                        final long clockWaitTimeNs = NbrsUtils.minusBounded(
                            firstTimedSched.getTheoreticalTimeNs(),
                            timeAfterWaitNs);
                        if (clockWaitTimeNs > 0) {
                            try {
                                this.schedClockTimeCondilock.awaitNanos(clockWaitTimeNs);
                            } catch (@SuppressWarnings("unused") InterruptedException e) {
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
                        /*
                         * No need to signal other eventually waiting workers,
                         * since none must be waiting for a time
                         * superior to removed schedule's theoretical time.
                         * NB: Could think of a way of not having all workers
                         * wake up at once for a same timed schedule
                         * (like adding random tiny delays to wait times
                         * and calling signal() after remove,
                         * but that doesn't help).
                         */
                        runnable = firstTimedSched.removeRunnable();
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
        }
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
            final int acceptStatusInMutex = this.getAcceptSchedulesStatus();
            if (acceptStatusInMutex == ACCEPT_SCHEDULES_YES_AND_WORKERS_START_NEEDED) {
                // Starting even if processing is stopped (not to have
                // to eventually start thread when starting processing).
                this.startWorkerThreads_stateLocked();
                this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_YES);
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * @return True if runnable could be enqueued, false otherwise.
     */
    private boolean enqueueAsapScheduleIfRoom_schedLocked(MyNode schedule) {
        schedule.setSequenceNumber(this.sequencer);
        final boolean didEnqueue = this.asapSchedQueue.offerLast(schedule);
        if (didEnqueue) {
            this.sequencer++;
        }
        return didEnqueue;
    }
    
    /**
     * Sets sequence number before queuing.
     * @return True if schedule could be enqueued, false otherwise.
     */
    private boolean enqueueTimedScheduleIfRoom_schedLocked(MyTimedSchedule schedule) {
        schedule.setSequenceNumber(this.sequencer);
        final boolean didEnqueue = this.timedSchedQueue.offer(schedule);
        if (didEnqueue) {
            this.sequencer++;
        }
        return didEnqueue;
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
        
        boolean enqueuedTimedOrAsapAndWasEmpty = false;
        boolean enqueued = false;
        
        final boolean isTimedSchedule =
            (schedule instanceof MyTimedSchedule);
        
        final Lock lockForPut = this.getLockForPut(isTimedSchedule);
        final boolean mustSignalInLockForPut =
            this.isSchedLock(lockForPut);
        lockForPut.lock();
        try {
            /*
             * Need to check accept status in lockForPut,
             * cf. comment in shutdown().
             */
            final int acceptStatus = this.getAcceptSchedulesStatus();
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
                if (isTimedSchedule) {
                    enqueued = this.enqueueTimedScheduleIfRoom_schedLocked(
                        (MyTimedSchedule) schedule);
                    enqueuedTimedOrAsapAndWasEmpty = enqueued;
                } else {
                    enqueued = this.enqueueAsapScheduleIfRoom_schedLocked(
                        (MyNode) schedule);
                    enqueuedTimedOrAsapAndWasEmpty =
                        enqueued
                        && this.asapSchedQueue.wasEmptyBeforeLastAdd();
                }
                if (enqueuedTimedOrAsapAndWasEmpty
                    && mustSignalInLockForPut) {
                    this.schedCondition.signal();
                }
            }
        } finally {
            lockForPut.unlock();
        }
        
        if (enqueuedTimedOrAsapAndWasEmpty) {
            if (!mustSignalInLockForPut) {
                this.schedLock.lock();
                try {
                    this.schedCondition.signal();
                } finally {
                    this.schedLock.unlock();
                }
            }
        } else {
            if (!enqueued) {
                CancellableUtils.call_onCancel_IfCancellableElseThrowREE(
                    schedule.removeRunnable());
            }
        }
        
        return false;
    }
}
