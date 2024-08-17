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
package net.jolikit.time.sched.hard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jolikit.lang.InterfaceBooleanCondition;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.locks.InterfaceCondilock;
import net.jolikit.threading.locks.LockCondilock;
import net.jolikit.threading.locks.MonitorCondilock;
import net.jolikit.time.sched.InterfaceWorkerAwareExecutor;
import net.jolikit.time.sched.SchedUtils;
import net.jolikit.time.sched.WorkerThreadChecker;

/**
 * Executor derived from HardScheduler,
 * only implementing Executor interface,
 * but still making use of InterfaceCancellable
 * (in lieu of rejected execution handler)
 * and of InterfaceWorkerAware.
 * The point is not to depend on InterfaceScheduler
 * and InterfaceHardClock when we don't need timed schedules,
 * and not have corresponding treatments overhead
 * (checks for eventual timed schedules, calls to clock for time,
 * wrappers around runnables to hold a sequencer for fairness,
 * etc.).
 * 
 * Calling it HardExecutor and putting in "hard" package,
 * even though it doesn't depend on InterfaceHardClock,
 * because it is conceptually still tied to the notion
 * of hard (and not soft) time and scheduling, due to having
 * a waitForNoMoreRunningWorkerSystemTimeNs() method
 * and using thread(s), and to emphasize the similarity
 * with HardScheduler.
 * 
 * As for HardScheduler, it's trivial to implement
 * an ExecutorService on top of it,
 * by extending JDK's AbstractExecutorService,
 * cf. HardExecutorService.
 * 
 * Main differences with JDK's ThreadPoolExecutor:
 * - more:
 *   - Hopefully has less overhead.
 *   - Methods to start/stop schedules acceptance and processing.
 *   - schedules queue can have a max capacity,
 *     over which schedules are rejected.
 * - different:
 *   - Uses fixed threads instead of a thread pool.
 * 
 * This executor provides methods to:
 * - start/stop schedules acceptance,
 * - start/stop schedules processing by worker threads,
 * - cancel pending schedules.
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
 */
public class HardExecutor implements InterfaceWorkerAwareExecutor {
    
    /*
     * locks order to respect (to avoid deadlocks):
     * - "A > B" <=> "can lock B in A, but not A in B"
     * - stateMutex > noRunningWorkerMutex > schedLock
     * 
     * All these locks can be acquired by user thread when calling
     * public methods of this class.
     * 
     * (Eventually) locking treatments done in each lock (must respect locks order):
     * - noRunningWorkerMutex:
     *   - schedLock (to retrieve the number of pending schedules)
     */
    
    /*
     * NB: Not designed for being backed by an Executor,
     * but could maybe be added (would allow to make schedulers
     * based on executors based on schedulers etc., and check
     * it would still work, even though with more overhead).
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Helps in benches in most cases, possibly due to having workers
     * wait less and be ready earlier for processing more work.
     * We want to minimize overhead under highest stress,
     * so we do that.
     */
    private static final boolean MUST_SIGNAL_ALL_ON_SUBMIT = true;
    
    private static final int DEFAULT_QUEUE_CAPACITY = Integer.MAX_VALUE;

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
                    noRunningWorkerSystemTimeCondilock.signalAllInLock();
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
     * Little queue for use instead of a LinkedList,
     * not so much for speed than for lower memory footprint,
     * in particular in case of schedules bursts
     * (but once it grows, it doesn't shrink).
     */
    private static class MyQueue {
        private Runnable[] arr = new Runnable[2];
        private int beginIndex = 0;
        private int endIndex = -1;
        private int size = 0;
        public int size() {
            return this.size;
        }
        public boolean addLast(Runnable schedule) {
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
        public Runnable removeFirst() {
            if (this.size == 0) {
                throw new NoSuchElementException();
            }
            return removeFirst_notEmpty();
        }
        public Runnable pollFirst() {
            if (this.size == 0) {
                return null;
            }
            return removeFirst_notEmpty();
        }
        private Runnable removeFirst_notEmpty() {
            final Runnable schedule = this.arr[this.beginIndex++];
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
            final int newCapacity = LangUtils.increasedArrayLength(
                this.arr.length, this.arr.length + 1);
            final Runnable[] newArr = new Runnable[newCapacity];
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
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final MyNoRunningWorkerBC noRunningWorkerBooleanCondition = new MyNoRunningWorkerBC();
    
    /*
     * schedLock
     */
    
    /**
     * Lock waited on for scheduling.
     * Guards some runnables collections.
     */
    private final ReentrantLock schedLock = new ReentrantLock();
    private final Condition schedCondition = this.schedLock.newCondition();
    private final InterfaceCondilock schedSystemTimeCondilock =
        new LockCondilock(this.schedLock, this.schedCondition);

    /*
     * noRunningWorkerMutex
     */
    
    private final Object noRunningWorkerMutex = new Object();
    private final InterfaceCondilock noRunningWorkerSystemTimeCondilock =
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
     * Capacity of queue for the user, whatever the current
     * inner capacity of its implementation, which might eventually grow.
     */
    private final int queueCapacity;
    
    /**
     * Guarded by schedLock.
     */
    private final MyQueue schedQueue = new MyQueue();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /*
     * Threadless instances.
     */
    
    /**
     * @return An executor working during call to startAndWorkInCurrentThread(),
     *         that guarantees FIFO order for schedules,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static HardExecutor newThreadlessInstance() {
        return newThreadlessInstance(
                DEFAULT_QUEUE_CAPACITY);
    }
    
    /**
     * @param queueCapacity Capacity (>=0) for schedules queue.
     *        When full, new schedules are canceled.
     * @return An executor working during call to startAndWorkInCurrentThread(),
     *         that guarantees FIFO order for schedules.
     */
    public static HardExecutor newThreadlessInstance(
            int queueCapacity) {
        return new HardExecutor(
                true, // isThreadless
                null, // threadNamePrefix
                false, // daemon
                1, // nbrOfThreads
                queueCapacity,
                null); // threadFactory
    }
    
    /*
     * Single-threaded instances.
     */

    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @return A single-threaded executor,
     *         that guarantees FIFO order for schedules,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static HardExecutor newSingleThreadedInstance(
            String threadNamePrefix,
            boolean daemon) {
        return newSingleThreadedInstance(
                threadNamePrefix,
                daemon,
                null); // threadFactory
    }

    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param threadFactory If null, default threads are created.
     * @return A single-threaded executor,
     *         that guarantees FIFO order for schedules,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static HardExecutor newSingleThreadedInstance(
            String threadNamePrefix,
            boolean daemon,
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
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param nbrOfThreads Number of threads to use. Must be >= 1.
     * @return An executor using the specified number of threads,
     *         that guarantees FIFO order for schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static HardExecutor newInstance(
            String threadNamePrefix,
            boolean daemon,
            int nbrOfThreads) {
        return newInstance(
                threadNamePrefix,
                daemon,
                nbrOfThreads,
                null); // threadFactory
    }

    /**
     * @param threadNamePrefix Prefix for worker threads names.
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param nbrOfThreads Number of threads to use. Must be >= 1.
     * @param threadFactory If null, default threads are created.
     * @return An executor using the specified number of threads,
     *         that guarantees FIFO order for schedules only if single-threaded,
     *         and uses Integer.MAX_VALUE for queue capacity.
     */
    public static HardExecutor newInstance(
            String threadNamePrefix,
            boolean daemon,
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
     *        Can be null, in which case worker threads names are not set.
     * @param daemon Daemon flag set to each thread.
     * @param nbrOfThreads Number of threads to use. Must be >= 1.
     * @param queueCapacity Capacity (>=0) for schedules queue.
     *        When full, new schedules are canceled.
     * @param threadFactory If null, default threads are created.
     * @return An executor using the specified number of threads,
     *         that guarantees FIFO order for schedules only if single-threaded.
     */
    public static HardExecutor newInstance(
            String threadNamePrefix,
            boolean daemon,
            int nbrOfThreads,
            int queueCapacity,
            ThreadFactory threadFactory) {
        return new HardExecutor(
                false, // isThreadless
                threadNamePrefix,
                daemon,
                nbrOfThreads,
                queueCapacity,
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
            return this.schedQueue.size();
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
            this.schedSystemTimeCondilock.signalAllInLock();
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
        Runnable schedule;
        while ((schedule = this.pollFirstScheduleInLock()) != null) {
            SchedUtils.call_onCancel_IfCancellable(schedule);
        }
    }

    /**
     * @param runnables Collection where to add runnables
     *        of drained pending schedules,
     *        in the order they were scheduled.
     */
    public void drainPendingRunnablesInto(Collection<? super Runnable> runnables) {
        int n = 0;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            n = this.schedQueue.size();
            for (int i = 0; i < n; i++) {
                final Runnable schedule = this.schedQueue.removeFirst();
                runnables.add(schedule);
            }
        } finally {
            if (n != 0) {
                // Signaling in finally, in case we had an exception
                // while adding into output collection.
                this.signalWorkersAfterScheduleRemoval();
            }
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
        
        this.drainPendingRunnablesInto(runnables);
        
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
    
    /*
     * scheduling
     */

    @Override
    public void execute(Runnable runnable) {
        if (this.enqueueRunnableIfPossible(runnable)) {
            this.tryStartWorkerThreadsOnScheduleSubmit();
            this.enqueueRunnableIfPossible(runnable);
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
    private HardExecutor(
        boolean isThreadless,
        final String threadNamePrefix,
        boolean daemon,
        int nbrOfThreads,
        int queueCapacity,
        final ThreadFactory threadFactory) {
        
        NbrsUtils.requireSupOrEq(1, nbrOfThreads, "nbrOfThreads");
        
        NbrsUtils.requireSup(0, queueCapacity, "queueCapacity");
        
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
        
        this.queueCapacity = queueCapacity;

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

    private Runnable pollFirstScheduleInLock() {
        final Runnable ret;
        final Lock schedLock = this.schedLock;
        schedLock.lock();
        try {
            ret = this.schedQueue.pollFirst();
            if (ret != null) {
                this.signalWorkersAfterScheduleRemoval();
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
     * to process them.
     */
    private void signalWorkersAfterScheduleRemoval() {
        this.schedCondition.signalAll();
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
     * @return True if there are pending schedules that might be processed by the worker,
     *         or false if the worker shall die.
     */
    private boolean waitForWorkOrDeath_schedLocked() {
        while (true) {
            /*
             * Passing here either:
             * - On initial call.
             * - After a schedule's execution.
             * - After end of a wait (for a schedule).
             */

            final int pendingScheduleCount = schedQueue.size();
            if (pendingScheduleCount != 0) {
                if (mustProcessSchedules()) {
                    return true;
                }
            } else {
                if (isShutdown()) {
                    // Here current worker starts to die.
                    return false;
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
    
    private void workerRun() {
        final Lock schedLock = this.schedLock;
        while (true) {
            Runnable runnable;
            
            schedLock.lock();
            try {
                // Loop to avoid getting out-and-in synchronization
                // when we are done waiting for a schedule's time.
                while (true) {
                    final boolean gotWorkElseMustDie =
                        waitForWorkOrDeath_schedLocked();
                    if (!gotWorkElseMustDie) {
                        return;
                    }
                    // Here, we have schedule(s) in queue, and we are supposed to work.
                    
                    runnable = this.schedQueue.removeFirst();
                    
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
    private boolean enqueueRunnableIfRoom_schedLocked(Runnable schedule) {
        if (this.schedQueue.size() == this.queueCapacity) {
            return false;
        }
        return this.schedQueue.addLast(schedule);
    }

    /**
     * Enqueues if room and accepted, and if workers are started
     * or some calling thread be used as worker later.
     * Cancels the schedule if could not enqueue due to no room
     * or not being accepted.
     * 
     * @param runnable runnable.
     * @return True if need workers start for retry, false otherwise.
     */
    private boolean enqueueRunnableIfPossible(Runnable runnable) {
        
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
                enqueued = this.enqueueRunnableIfRoom_schedLocked(runnable);
            }
            if (enqueued) {
                if (MUST_SIGNAL_ALL_ON_SUBMIT) {
                    this.schedCondition.signalAll();
                } else {
                    this.schedCondition.signal();
                }
            } else {
                // will cancel outside lock (unless exception while trying to enqueue,
                // but then it will go up the call stack and notify the user that there
                // was a problem)
            }
        } finally {
            schedLock.unlock();
        }

        if (!enqueued) {
            SchedUtils.call_onCancel_IfCancellable(runnable);
        }
        
        return false;
    }
}
