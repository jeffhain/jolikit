/*
 * Copyright 2024 Jeff Hain
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
package net.jolikit.threading.execs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
import net.jolikit.threading.locks.MonitorCondilock;

/**
 * Executor derived from HardScheduler.
 * - Removed: timed schedules, and dependency to clock interfaces
 *   (so always tied to system time or nano time, like
 *   ThreadPoolExecutor).
 * - Kept: use of InterfaceCancellable
 *   and of InterfaceWorkerAware.
 * - Added: implementation of ExecutorService interface
 *   (not implemented by HardScheduler because Future
 *   implies waits, and waits are not compatible
 *   with soft scheduling our hard scheduling wants
 *   to be consistent with).
 * - Advantages: Better throughput (workers don't need to
 *   check for timed schedules, no need to check current time,
 *   no need for fairness sequencer).
 * 
 * Main differences with JDK's ThreadPoolExecutor:
 * - more:
 *   - Usually better throughput (but only noticeable for tiny tasks),
 *     especially in case of single or just a few worker threads
 *     (using a simpler queue in these cases).
 *   - Methods to start/stop schedules acceptance and processing
 *     by worker threads.
 *   - Methods to cancel or drain pending schedules.
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
 */
public class FixedThreadExecutor
extends AbstractExecutorService
implements InterfaceWorkerAwareExecutor {
    
    /*
     * locks order to respect (to avoid deadlocks):
     * - "A > B" <=> "can lock B in A, but not A in B"
     * - stateMutex > noRunningWorkerMutex > putLock >= takeLock
     * 
     * All of these locks can be acquired by user thread when calling
     * public methods of this class.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int DEFAULT_QUEUE_CAPACITY = Integer.MAX_VALUE;
    
    /**
     * Above a few workers, benches get slower with basic queue in case of
     * single publisher, and much slower as worker count increases.
     */
    private static final int DEFAULT_MAX_WORKER_COUNT_FOR_BASIC_QUEUE = 4;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
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
                workerRun();
                this.done = true;
            } finally {
                if (nbrOfRunningWorkers.decrementAndGet() == 0) {
                    noRunningWorkerCondilock.signalAllInLock();
                }
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
     * Linked queue node class.
     */
    private static class MyNode {
        Runnable item;
        /**
         * Next node if any, null if last node
         * (including when empty, i.e. head = last).
         */
        MyNode next;
        MyNode(Runnable x) {
            this.item = x;
        }
    }
    
    /**
     * Derived from LinkedBlockingQueue,
     * with locks acquiring moved out of it,
     * and no notFull usage (using offer, not blocking put).
     * 
     * The extended AtomicInteger holds the size for
     * concurrent (dual lock) implementation
     * (avoiding a separate object to reduce risk of cache miss).
     */
    private static abstract class MyAbstractQueue extends PostPaddedAtomicInteger {
        private static final long serialVersionUID = 1L;
        final int capacity;
        /**
         * Used on take.
         */
        private MyNode head;
        /**
         * Used on put.
         */
        private MyNode last;
        private boolean queueEmptyBeforeLastAdd = false;
        private boolean queueNotEmptyAfterLastRemove = false;
        public MyAbstractQueue(int capacity) {
            this.capacity = capacity;
            this.last = this.head = new MyNode(null);
        }
        /*
         * Methods usable in putLock or in takeLock
         * (i.e. in the lock if they are the same,
         * else typically callable concurrently).
         */
        public abstract int size();
        /*
         * Methods to be used in putLock.
         */
        /**
         * @return True if could enqueue (i.f. was not full).
         */
        public abstract boolean offerLast(MyNode node);
        final void addLast_structure(MyNode node) {
            // assert last.next == null;
            this.last = this.last.next = node;
        }
        /**
         * Allows to know whether signaling of takeLock must be done,
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
         * Methods to be used in takeLock.
         */
        public abstract Runnable pollFirst();
        final Runnable removeFirst_structure() {
            // assert head.item == null;
            /*
             * Removing head node, and first node
             * gets to be used as new head.
             */
            MyNode h = this.head;
            MyNode first = h.next;
            h.next = h; // help GC
            this.head = first;
            Runnable x = first.item;
            first.item = null;
            return x;
        }
        /**
         * Allows to know whether signaling of takeLock must be done,
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
    private static class MyBasicQueue extends MyAbstractQueue {
        private static final long serialVersionUID = 1L;
        private int size = 0;
        public MyBasicQueue(int capacity) {
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
        public Runnable pollFirst() {
            Runnable ret = null;
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
     * Implementation guarded by a putLock and a takeLock,
     * with concurrently readable size.
     */
    private static class MyDualLockQueue extends MyAbstractQueue {
        private static final long serialVersionUID = 1L;
        public MyDualLockQueue(int capacity) {
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
        public Runnable pollFirst() {
            final AtomicInteger sizeAto = this;
            Runnable ret = null;
            if (sizeAto.get() > 0) {
                ret = this.removeFirst_structure();
                final int oldSize = sizeAto.getAndDecrement();
                this.setWasNotEmptyAfterLastRemove(oldSize > 1);
            }
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MyNoRunningWorkerBC noRunningWorkerBooleanCondition =
        new MyNoRunningWorkerBC();
    
    /*
     * takeLock
     */
    
    /**
     * Lock waited on by workers.
     */
    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition takeCondition = this.takeLock.newCondition();
    
    /*
     * putLock
     */
    
    /**
     * Lock acquired by publishers.
     * Might be identical to takeLock.
     */
    private final ReentrantLock putLock;
    
    /*
     * noRunningWorkerMutex
     */
    
    private final Object noRunningWorkerMutex = new Object();
    private final InterfaceCondilock noRunningWorkerCondilock =
        new MonitorCondilock(this.noRunningWorkerMutex);
    
    /*
     * 
     */
    
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
     * Guarded by takeLock and possibly putLock
     * depending on the implementation.
     */
    private final MyAbstractQueue schedQueue;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Complete constructor for non-threadless instances.
     * Constructs an executor using the specified number of worker threads,
     * that guarantees FIFO order for schedules only if single-threaded.
     * 
     * Allows to extend this class, and to use non-default
     * worker count thresholds for queues types.
     * 
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @param queueCapacity Capacity for schedules queue.
     *        Must be >= 0. When full, new schedules are rejected.
     * @param maxWorkerCountForBasicQueue Must be >= 0.
     *        When worker count is strictly superior to this value,
     *        a queue more suited to high number of workers is used.
     *        Default for newXxx() construction methods is 4.
     * @param threadFactory If null, default threads are created.
     */
    public FixedThreadExecutor(
        String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads,
        int queueCapacity,
        int maxWorkerCountForBasicQueue,
        ThreadFactory threadFactory) {
        this(
            false, // isThreadless
            threadNamePrefix,
            daemon,
            nbrOfThreads,
            queueCapacity,
            maxWorkerCountForBasicQueue,
            threadFactory);
    }
    
    /**
     * Complete constructor for threadless instances.
     * Guarantees FIFO order for schedules,
     * since only caller thread is used for work.
     * 
     * Allows to extend this class, and to use non-default
     * worker count thresholds for queues types.
     * 
     * @param queueCapacity Capacity for schedules queue.
     *        Must be >= 0. When full, new schedules are rejected.
     * @param maxWorkerCountForBasicQueue Must be >= 0.
     *        When worker count is strictly superior to this value,
     *        a queue more suited to high number of workers is used.
     *        Default for newXxx() construction methods is 4.
     */
    public FixedThreadExecutor(
        int queueCapacity,
        int maxWorkerCountForBasicQueue) {
        this(
            true, // isThreadless
            null, // threadNamePrefix
            null, // daemon
            1, // nbrOfThreads
            queueCapacity,
            maxWorkerCountForBasicQueue,
            null); // threadFactory
    }
    
    /*
     * Threadless instances.
     */
    
    /**
     * @return An executor working during call to startAndWorkInCurrentThread(),
     *         that guarantees FIFO order for schedules,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static FixedThreadExecutor newThreadlessInstance() {
        return newThreadlessInstance(
            DEFAULT_QUEUE_CAPACITY);
    }
    
    /**
     * @param queueCapacity Capacity (>=0) for schedules queue.
     *        When full, new schedules are rejected.
     * @return An executor working during call to startAndWorkInCurrentThread(),
     *         that guarantees FIFO order for schedules.
     */
    public static FixedThreadExecutor newThreadlessInstance(
        int queueCapacity) {
        return new FixedThreadExecutor(
            queueCapacity,
            DEFAULT_MAX_WORKER_COUNT_FOR_BASIC_QUEUE);
    }
    
    /*
     * Single-threaded instances.
     */
    
    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @return A single-threaded executor,
     *         that guarantees FIFO order for schedules,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static FixedThreadExecutor newSingleThreadedInstance(
        String threadNamePrefix,
        Boolean daemon) {
        return newSingleThreadedInstance(
            threadNamePrefix,
            daemon,
            null); // threadFactory
    }
    
    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param threadFactory If null, default threads are created.
     * @return A single-threaded executor,
     *         that guarantees FIFO order for schedules,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static FixedThreadExecutor newSingleThreadedInstance(
        String threadNamePrefix,
        Boolean daemon,
        ThreadFactory threadFactory) {
        return newInstance(
            threadNamePrefix,
            daemon,
            1, // nbrOfThreads
            threadFactory);
    }
    
    /*
     * Instances using possibly multiple threads.
     */
    
    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @return An executor using the specified number of worker threads,
     *         that guarantees FIFO order for schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static FixedThreadExecutor newInstance(
        String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads) {
        return newInstance(
            threadNamePrefix,
            daemon,
            nbrOfThreads,
            null); // threadFactory
    }
    
    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @param threadFactory If null, default threads are created.
     * @return An executor using the specified number of worker threads,
     *         that guarantees FIFO order for schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static FixedThreadExecutor newInstance(
        String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads,
        ThreadFactory threadFactory) {
        return newInstance(
            threadNamePrefix,
            daemon,
            nbrOfThreads,
            DEFAULT_QUEUE_CAPACITY,
            threadFactory);
    }
    
    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case the name eventually set
     *        by thread factory is preserved.
     * @param daemon Daemon flag set to each thread.
     *        Can be null, in which case the value eventually set
     *        by thread factory is preserved.
     * @param nbrOfThreads Number of worker threads to use. Must be >= 1.
     * @param queueCapacity Capacity (>=0) for schedules queue.
     *        When full, new schedules are rejected.
     * @param threadFactory If null, default threads are created.
     * @return An executor using the specified number of worker threads,
     *         that guarantees FIFO order for schedules only if single-threaded.
     */
    public static FixedThreadExecutor newInstance(
        String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads,
        int queueCapacity,
        ThreadFactory threadFactory) {
        return new FixedThreadExecutor(
            threadNamePrefix,
            daemon,
            nbrOfThreads,
            queueCapacity,
            DEFAULT_MAX_WORKER_COUNT_FOR_BASIC_QUEUE,
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
        
        sb.append(",schedules:");
        sb.append(this.getNbrOfPendingSchedules());
        
        sb.append(",accepting:");
        sb.append(mustAcceptSchedules(acceptStatus));
        sb.append(",processing:");
        sb.append(mustProcessSchedules(processStatus));
        sb.append("]");
        
        return sb.toString();
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
            // Effectively immutable after executor's construction,
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
         * We could count threads queued to acquire takeLock but don't,
         * because:
         * - takeLock can also be locked by user threads, which would include
         *   them in the count,
         * - takeLock should not take much time to acquire, since no
         *   significant treatments are done within it; so we can lag a bit
         *   and count corresponding workers as still being working.
         */
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            return takeLock.getWaitQueueLength(this.takeCondition);
        } finally {
            takeLock.unlock();
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
        final Lock takeLock = this.takeLock;
        takeLock.lock();
        try {
            return this.schedQueue.size();
        } finally {
            takeLock.unlock();
        }
    }
    
    /**
     * @return True if shutdown() or shutdownNow(...) has been called, false otherwise.
     */
    @Override
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
     * @throws IllegalStateException if this executor is not threadless.
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
             * only addressed to user code, not to executor code).
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
     * @throws IllegalStateException if this executor is threadless.
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
            this.signalAllWorkersInTakeLock();
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
        Runnable schedule;
        while ((schedule = this.pollFirstScheduleInTakeLock()) != null) {
            CancellableUtils.call_onCancel_IfCancellable(schedule);
        }
    }
    
    /**
     * @param runnables Collection where to add runnables
     *        of drained pending schedules,
     *        in the order they were scheduled.
     */
    public void drainPendingRunnablesInto(Collection<? super Runnable> runnables) {
        boolean gotSome = false;
        final Lock takeLock = this.takeLock;
        takeLock.lock();
        try {
            while (true) {
                final Runnable schedule =
                    this.schedQueue.pollFirst();
                if (schedule == null) {
                    break;
                }
                gotSome = true;
                runnables.add(schedule);
            }
        } finally {
            if (gotSome) {
                // Signaling in finally, in case we had an exception
                // while adding into output collection.
                this.signalAllWorkersAfterSchedulesRemoval();
            }
            takeLock.unlock();
        }
    }
    
    /**
     * Interrupts worker threads if any, for the purpose of interrupting
     * user treatments (this executor only using uninterruptible waits).
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
     * it has no more schedule to process.
     * 
     * After call to this method, and in the purpose of quickening workers death,
     * you might want (or not want) to (and in that order if calling both):
     * - cancel or drain pending schedules,
     * - interrupt workers.
     * 
     * If shutdown, and there are no more running workers:
     * - you might wait to cancel or drain remaining pending schedules, or they
     *   will remain pending forever,
     * - if there are no more pending schedules,
     *   this executor can be considered terminated.
     */
    @Override
    public void shutdown() {
        final boolean actualRequest;
        synchronized (this.stateMutex) {
            final int processStatus = this.getProcessSchedulesStatus();
            actualRequest = !isShutdown(processStatus);
            if (actualRequest) {
                /*
                 * Changing accept status in putLock,
                 * to avoid concurrency with execute()'s
                 * "if (accept) { enqueue(); }" logic,
                 * which could lead to enqueuing a runnable
                 * after all workers died (it could then
                 * still be drained, but would not have been
                 * properly rejected or cancelled on submit,
                 * nor drained by a shutdownNow() call).
                 * NB: It is not mandatory to do that if putLock
                 * and takeLock are the same, because then
                 * workers are not doing any concurrent bookkeeping
                 * and shutdownNow()'s drain would be waiting
                 * for the lock to be released by execute(),
                 * but we still take it in this case for consistency.
                 */
                this.putLock.lock();
                try {
                    // NO, whether or not worker threads have been started already.
                    this.setAcceptSchedulesStatus(ACCEPT_SCHEDULES_NO);
                    
                    if (processStatus == PROCESS_SCHEDULES_YES) {
                        this.setProcessSchedulesStatus(PROCESS_SCHEDULES_YES_AND_DIE_AFTERWARDS);
                    } else {
                        this.setProcessSchedulesStatus(PROCESS_SCHEDULES_NO_AND_DIE_AFTERWARDS);
                    }
                } finally {
                    this.putLock.unlock();
                }
            }
        }
        if (actualRequest) {
            // Signaling eventually waiting workers,
            // for them to start dying if needed.
            this.signalAllWorkersInTakeLock();
        }
    }
    
    /**
     * Convenience method, implemented with other public methods:
     * calls shutdown(), then stopProcessing(), then eventually
     * interruptWorkers(), and then methods to drain pending schedules.
     */
    public List<Runnable> shutdownNow(boolean mustInterruptWorkingWorkers) {
        // Never more accepting, and dying when idle.
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
        
        this.drainPendingRunnablesInto(runnables);
        
        return runnables;
    }
    
    /*
     * waits
     */
    
    /**
     * Waits for no more worker to be running,
     * or for the specified timeout to elapse.
     * 
     * @param timeoutNs Timeout, in system time, in nanoseconds.
     * @return True if there was no more running worker before
     *         the timeout elapsed, false otherwise.
     * @throws InterruptedException if the wait gets interrupted.
     */
    public boolean waitForNoMoreRunningWorker(long timeoutNs) throws InterruptedException {
        return this.noRunningWorkerCondilock.awaitNanosWhileFalseInLock(
            this.noRunningWorkerBooleanCondition,
            timeoutNs);
    }
    
    /*
     * scheduling
     */
    
    @Override
    public void execute(Runnable runnable) {
        
        LangUtils.requireNonNull(runnable);
        
        // Optimistically allocated outside putLock.
        final MyNode node = new MyNode(runnable);
        
        if (this.enqueueRunnableIfPossible(node)) {
            this.tryStartWorkerThreadsOnScheduleSubmit();
            this.enqueueRunnableIfPossible(node);
        }
    }
    
    /*
     * Complementary methods for ExecutorService.
     */
    
    /**
     * Does not interrupt workers.
     */
    @Override
    public List<Runnable> shutdownNow() {
        // Can always interrupt workers aside if needed.
        final boolean mustInterruptWorkingWorkers = false;
        return this.shutdownNow(mustInterruptWorkingWorkers);
    }
    
    @Override
    public boolean isTerminated() {
        return this.isShutdown()
            && (this.getNbrOfRunningWorkers() == 0);
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        final long timeoutNs = unit.toNanos(timeout);
        return this.waitForNoMoreRunningWorker(timeoutNs);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @throws IllegalStateException if this executor is not threadless.
     */
    private void checkThreadless() {
        if (!this.isThreadless) {
            throw new IllegalStateException("this executor is not threadless");
        }
    }
    
    /**
     * @throws IllegalStateException if this executor is threadless.
     */
    private void checkNotThreadless() {
        if (this.isThreadless) {
            throw new IllegalStateException("this executor is threadless");
        }
    }
    
    private boolean isTakeLock(Lock lock) {
        return (lock == this.takeLock);
    }
    
    /*
     * 
     */
    
    /**
     * @param isThreadless If true, no worker thread is created, and
     *        user calling thread is used as worker thread, during
     *        startAndWorkInCurrentThread() method call.
     */
    private FixedThreadExecutor(
        boolean isThreadless,
        final String threadNamePrefix,
        Boolean daemon,
        int nbrOfThreads,
        int queueCapacity,
        int maxWorkerCountForBasicQueue,
        final ThreadFactory threadFactory) {
        
        NbrsUtils.requireSupOrEq(1, nbrOfThreads, "nbrOfThreads");
        
        NbrsUtils.requireSup(0, queueCapacity, "queueCapacity");
        
        NbrsUtils.requireSupOrEq(
            0,
            maxWorkerCountForBasicQueue,
            "maxWorkerCountForBasicQueue");
        
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
        
        /*
         * 
         */
        
        final boolean mustUseBasicQueue =
            (nbrOfThreads <= maxWorkerCountForBasicQueue);
        if (mustUseBasicQueue) {
            this.putLock = this.takeLock;
            this.schedQueue = new MyBasicQueue(queueCapacity);
        } else {
            this.putLock = new ReentrantLock();
            this.schedQueue = new MyDualLockQueue(queueCapacity);
        }
        
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
    
    private Runnable pollFirstScheduleInTakeLock() {
        final Runnable ret;
        final Lock takeLock = this.takeLock;
        takeLock.lock();
        try {
            ret = this.schedQueue.pollFirst();
            /*
             * Only need to signal if became empty,
             * in case workers were waiting to be allowed to process
             * remaining schedules to complete after a shutdown.
             */
            if ((ret != null)
                && (!this.schedQueue.wasNotEmptyAfterLastRemove())) {
                this.signalAllWorkersAfterSchedulesRemoval();
            }
        } finally {
            takeLock.unlock();
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
     * to process them before completing after a shutdown.
     */
    private void signalAllWorkersAfterSchedulesRemoval() {
        this.takeCondition.signalAll();
    }
    
    /**
     * Usually called outside stateMutex, to reduce locks nesting
     * and stateMutex's locking time, but could be called within.
     */
    private void signalAllWorkersInTakeLock() {
        this.takeLock.lock();
        try {
            this.takeCondition.signalAll();
        } finally {
            this.takeLock.unlock();
        }
    }
    
    private void workerRun() {
        while (true) {
            final Runnable runnable = this.waitForRunnableOrDeath();
            if (runnable == null) {
                // Here current worker starts to die.
                break;
            }
            
            runnable.run();
        }
    }
    
    private Runnable waitForRunnableOrDeath() {
        
        Runnable runnable = null;
        
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            while (true) {
                /*
                 * Passing here either:
                 * - On initial call.
                 * - After a schedule's execution.
                 * - After end of a wait (for a schedule).
                 */
                
                final int processStatus = this.processSchedulesStatus.get();
                
                boolean queueFoundEmpty = false;
                if (mustProcessSchedules(processStatus)) {
                    runnable = this.schedQueue.pollFirst();
                    if (runnable != null) {
                        if (this.schedQueue.wasNotEmptyAfterLastRemove()) {
                            /*
                             * Signaling even if there is no other worker:
                             * doesn't seem to hurt, and could avoid issue
                             * if having something else waiting on it.
                             */
                            this.takeCondition.signal();
                        }
                        // Will run it.
                        break;
                    } else {
                        queueFoundEmpty = true;
                    }
                } else {
                    queueFoundEmpty = (this.schedQueue.size() == 0);
                }
                
                if (queueFoundEmpty) {
                    if (isShutdown(processStatus)) {
                        // Here current worker starts to die.
                        break;
                    } else {
                        // Will wait for more schedules.
                    }
                } else {
                    // Will wait for more schedules.
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
                    this.takeCondition.await();
                } catch (@SuppressWarnings("unused") InterruptedException e) {
                    /*
                     * Not restoring interrupt status,
                     * else next runnable to process would get interrupted.
                     */
                }
            }
        } finally {
            takeLock.unlock();
        }
        
        return runnable;
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
     * Enqueues if room and accepted, and if workers are started
     * or some calling thread be used as worker later.
     * Cancels the schedule if could not enqueue due to no room
     * or not being accepted.
     * 
     * @param node Node holding the runnable.
     * @return True if need workers start for retry, false otherwise.
     */
    private boolean enqueueRunnableIfPossible(MyNode node) {
        
        boolean enqueuedAndWasEmpty = false;
        boolean enqueued = false;
        
        final Lock putLock = this.putLock;
        final boolean mustSignalInPutLock =
            this.isTakeLock(putLock);
        putLock.lock();
        try {
            /*
             * Need to check accept status in putLock,
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
                enqueued = this.schedQueue.offerLast(node);
                if (enqueued) {
                    enqueuedAndWasEmpty = this.schedQueue.wasEmptyBeforeLastAdd();
                    if (enqueuedAndWasEmpty
                        && mustSignalInPutLock) {
                        this.takeCondition.signal();
                    }
                }
            }
        } finally {
            putLock.unlock();
        }
        
        /*
         * 
         */
        
        if (enqueuedAndWasEmpty) {
            if (!mustSignalInPutLock) {
                this.takeLock.lock();
                try {
                    this.takeCondition.signal();
                } finally {
                    this.takeLock.unlock();
                }
            }
        } else {
            if (!enqueued) {
                CancellableUtils.call_onCancel_IfCancellableElseThrowREE(node.item);
            }
        }
        
        return false;
    }
}
