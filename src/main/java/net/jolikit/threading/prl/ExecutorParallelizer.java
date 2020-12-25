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
package net.jolikit.threading.prl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.RethrowException;

/**
 * Simple parallelizer, based on a regular Executor (if you specify a
 * ForkJoinPool, only its execute(Runnable) method will be used).
 * 
 * worthToSplit() method can be called once in user thread before eventually
 * parallelizing, but it does not need to be made thread-safe for that matter.
 * 
 * If a splittable splits into a splitmergable, the splitmergable will just be
 * considered as a splittable, i.e. no merge will be done.
 * 
 * This implementation is reentrant, i.e. execute(Runnable) method can be used
 * from within a runnable or splittable or splitmergable being executed.
 * 
 * This implementation always uses the caller thread as a worker thread,
 * to avoid deadlocks when caller threads are worker threads
 * of the backing executor.
 * This increases actual parallelism by one.
 * 
 * Exceptions:
 * Throwables thrown by runnables/splittables/splitmergables are catched,
 * and the first one detected is rethrown in the initial execute(...) call.
 * If any throwable is catched, computations are aborted (early stop).
 * If no exception handler is specified, they are not propagated
 * to the backing executor, and the ones following the first detected
 * are just ignored.
 * If an exception handler is specified, it's called on each catched exception.
 * Note that if the handler rethrows an exception, letting it go
 * up the call stack into a backing executor which threads die on exceptions,
 * it could cause actual parallelism to quietly converge to one
 * and silently hurt performances.
 * 
 * Interrupts:
 * If the caller thread or a worker thread of the backing executor
 * is interrupted before or during the parallelization,
 * the interrupt status is preserved by the parallelization.
 */
public class ExecutorParallelizer implements InterfaceParallelizer {
    
    /*
     * Tasks are enqueued/dequeued in LIFO. It causes more contention on a same
     * end of the queue, but causes possibly way more split-and-not-yet-run
     * tasks, is more cache-friendly, and causes "inner shots" to be completed
     * early.
     * 
     * Could be more efficient if using CAS and spinning here-and-there instead
     * of synchronized/wait/notifyAll, but this implementation is designed to be
     * simple.
     */
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * A bit faster than LinkedList, and causes less garbage.
     */
    private static class MyLifo<T> {
        /**
         * Initially small to ensure a few grows, to make sure that works.
         */
        private Object[] elements = new Object[2];
        private int size;
        public int size() {
            return this.size;
        }
        public void addLast(T element) {
            if (this.size == this.elements.length) {
                this.grow();
            }
            this.elements[this.size++] = element;
        }
        public T removeLast() {
            final int index = --this.size;
            @SuppressWarnings("unchecked")
            final T res = (T) this.elements[index];
            this.elements[index] = null;
            return res;
        }
        private void grow() {
            final int previousCapacity = this.elements.length;
            if (previousCapacity == Integer.MAX_VALUE) {
                throw new IllegalStateException("full");
            }
            // Never stagnates, as long as previous capacity is >= 2.
            final int newCapacity = (int) (previousCapacity * 1.5);
            final Object[] newElements = new Object[newCapacity];
            System.arraycopy(this.elements, 0, newElements, 0, this.size);
            this.elements = newElements;
        }
    }
    
    /*
     * 
     */

    /**
     * Interface for internal tasks, for code clarity.
     * 
     * Not extending Runnable, and taking care not to name
     * the method "run",
     * to make it clear that these tasks are not runnables
     * and therefore not executed by the backing executor.
     */
    private interface MyInterfaceTask {
        public void runTask();
    }
    
    /**
     * Task for splittables.
     */
    private static class MySpTask implements MyInterfaceTask {
        private final InterfaceSplittable sp;
        private final MyShotHandler shotHandler;
        private int depth;
        public MySpTask(
                InterfaceSplittable sp,
                MyShotHandler shotHandler,
                int depth) {
            this.sp = sp;
            this.shotHandler = shotHandler;
            this.depth = depth;
        }
        @Override
        public void runTask() {
            final MyShotHandler shotHandler = this.shotHandler;
            try {
                if (!shotHandler.didATaskCatchAThrowable()) {
                    /*
                     * Forking.
                     */
                    this.forkAsNeeded();
                    /*
                     * Running.
                     */
                    this.sp.run();
                }
            } catch (Throwable t) {
                shotHandler.onThrowable(t);
            } finally {
                shotHandler.onTaskCompletion();
            }
        }
        private void forkAsNeeded() {
            final MyShotHandler shotHandler = this.shotHandler;
            while ((this.depth < shotHandler.maxDepth)
                    && this.sp.worthToSplit()) {
                final MySpTask right = new MySpTask(
                        this.sp.split(),
                        shotHandler,
                        ++this.depth);
                
                shotHandler.enqueueTaskAndExecuteHandler(right);
            }
        }
    }
    
    /**
     * Task for splitmergables.
     * 
     * AtomicReference holds reference to first completed splitmergable, if any.
     */
    private static class MySpmTask extends AtomicReference<InterfaceSplitmergable> implements MyInterfaceTask {
        private static final long serialVersionUID = 1L;
        private final InterfaceSplitmergable spm;
        private final MyShotHandler shotHandler;
        private final int depth;
        private final MySpmTask parent;
        public MySpmTask(
                InterfaceSplitmergable spm,
                MyShotHandler shotHandler,
                int depth,
                MySpmTask parent) {
            this.spm = spm;
            this.shotHandler = shotHandler;
            this.depth = depth;
            this.parent = parent;
        }
        @Override
        public void runTask() {
            runTask(this, this.shotHandler);
        }
        /**
         * Using a static method, to make it clearer that we are not tied to
         * this specific node (at least after fork).
         */
        public static void runTask(
                MySpmTask currentNode,
                MyShotHandler shotHandler) {
            try {
                if (!shotHandler.didATaskCatchAThrowable()) {
                    /*
                     * Forking.
                     */
                    currentNode = forkAsNeeded(
                            currentNode,
                            shotHandler);
                    /*
                     * Running.
                     */
                    currentNode.spm.run();
                    /*
                     * Merging.
                     */
                    mergeAsNeeded(currentNode);
                }
            } catch (Throwable t) {
                shotHandler.onThrowable(t);
            } finally {
                shotHandler.onTaskCompletion();
            }
        }
        /**
         * @return New current node.
         */
        private static MySpmTask forkAsNeeded(
                MySpmTask currentNode,
                MyShotHandler shotHandler) {
            while ((currentNode.depth < shotHandler.maxDepth)
                    && currentNode.spm.worthToSplit()) {
                final int childrenDepth = currentNode.depth+1;
                final MySpmTask left = new MySpmTask(
                        currentNode.spm,
                        shotHandler,
                        childrenDepth,
                        currentNode);
                final MySpmTask right = new MySpmTask(
                        currentNode.spm.split(),
                        shotHandler,
                        childrenDepth,
                        currentNode);
                
                shotHandler.enqueueTaskAndExecuteHandler(right);

                currentNode = left;
            }
            return currentNode;
        }
        private static void mergeAsNeeded(MySpmTask currentNode) {
            final InterfaceSplitmergable currentSpm = currentNode.spm;
            MySpmTask parent = currentNode.parent;
            while (parent != null) {
                InterfaceSplitmergable peer = parent.get();
                if (peer == null) {
                    // CAS ensures visibility on peer for merges.
                    if (parent.compareAndSet(null, currentSpm)) {
                        // Peer will take care of merging.
                        break;
                    } else {
                        // Peer just CASed itself:
                        // we need to take care of merging.
                        peer = parent.get();
                    }
                } else {
                    // Peer already CASed itself:
                    // we need to take care of merging.
                }
                // We always merge into the SPM we ran,
                // even if it is a split from peer.
                currentSpm.merge(currentSpm, peer);
                currentNode = parent;
                parent = parent.parent;
            }
            if (currentNode.parent == null) {
                // Root node: setting the SPM that holds the result.
                // Lazy set is fine since we decrementAndGet a counter
                // on task completion, which has (among other things)
                // volatile write semantics (AFAIK).
                currentNode.lazySet(currentSpm);
            }
        }
    }
    
    /*
     * 
     */

    /**
     * Info specific to a call to parallelizer.execute(Runnable).
     * 
     * The extended AtomicInteger holds the number of tasks started, and is
     * notified when it reaches 0.
     * If the backing executor fails to start a task (other than due to
     * shutdownNow() or equivalent), it should throw, so we should still
     * not wait for it indefinitely, and if we do so we can still be
     * interrupted.
     * 
     * Used as mutex for wait/notify, for both exceptional and normal
     * completion.
     */
    private static class MyShotHandler extends AtomicInteger implements Runnable {
        private static final long serialVersionUID = 1L;
        final ExecutorParallelizer owner;
        final int maxDepth;
        /**
         * Write guarded by synchronization on this.
         */
        private volatile Throwable firstThrown;
        public MyShotHandler(
                ExecutorParallelizer owner,
                int maxDepth) {
            this.owner = owner;
            this.maxDepth = maxDepth;
        }
        private synchronized void setIfFirst(Throwable throwable) {
            if (this.firstThrown == null) {
                this.firstThrown = throwable;
            }
        }
        public void onThrowable(Throwable throwable) {
            this.setIfFirst(throwable);
            
            final UncaughtExceptionHandler handler = owner.exceptionHandler;
            if (handler != null) {
                handler.uncaughtException(Thread.currentThread(), throwable);
            }
        }
        public void enqueueTask(MyInterfaceTask task) {
            this.incrementAndGet();
            final MyLifo<MyInterfaceTask> queueAndMutex = this.owner.queueAndMutex;
            synchronized (queueAndMutex) {
                queueAndMutex.addLast(task);
                // To wake up the thread in execute(Runnable) method that would
                // be waiting for some task (or completion of its shot).
                queueAndMutex.notifyAll();
            }
        }
        public void enqueueTaskAndExecuteHandler(MyInterfaceTask task) {
            this.enqueueTask(task);
            /*
             * Executing handler for each task enqueuing, to make sure we use
             * all available parallelism. At worse, some executions of handler
             * will abort early when finding out that the shot completed.
             * 
             * Can eventually throw RejectedExecutionException,
             * which our tasks will catch and treat as any other issue
             * (throwable) thrown during execution.
             */
            this.owner.executor.execute(this);
        }
        /**
         * Must be called on task completion (normal or not).
         */
        public void onTaskCompletion() {
            if (this.decrementAndGet() == 0) {
                this.onShotCompletion();
            }
        }
        /**
         * Must be called on completion of this shot, i.e. on completion of all
         * of its tasks.
         */
        private void onShotCompletion() {
            final MyLifo<MyInterfaceTask> queueAndMutex = this.owner.queueAndMutex;
            synchronized (queueAndMutex) {
                // To wake up the thread in execute(Runnable) method that would
                // be waiting for completion of this shot (or some task).
                queueAndMutex.notifyAll();
            }
        }
        /**
         * To decide whether a task must be executed, or completed right away.
         * 
         * @return True if a task did catch a Throwable during its
         *         {[fork,] run[, merge]} work.
         */
        public boolean didATaskCatchAThrowable() {
            return (this.firstThrown != null);
        }
        /**
         * @return True if all tasks of this shot completed (normally or not).
         */
        public boolean shotCompleted() {
            return (this.get() == 0);
        }
        /**
         * Forks/run/merge any task until this shot completes.
         */
        @Override
        public void run() {
            /*
             * We don't need to wait for shot completion here, because after
             * each split we execute shot handler in the backing executor,
             * which ensures that we won't miss any of our tasks.
             * Not waiting also helps freeing executor's workers earlier, in
             * case they could be used by other treatments as well.
             */
            final boolean untilShotCompletion = false;
            this.forkRunMergeAnyTask(untilShotCompletion);
        }
        /**
         * @param untilShotCompletion If true waits for shot completion
         *        before completing.
         */
        public void forkRunMergeAnyTask(boolean untilShotCompletion) {
            if (this.shotCompleted()) {
                return;
            }
            
            final ExecutorParallelizer owner = this.owner;
            final MyLifo<MyInterfaceTask> queueAndMutex = owner.queueAndMutex;
            
            while (true) {
                final MyInterfaceTask task;
                synchronized (queueAndMutex) {
                    if (untilShotCompletion) {
                        boolean interrupted = false;
                        // If queue is empty, waiting for more tasks
                        // or for shot completion.
                        while ((queueAndMutex.size() == 0)
                                && (!this.shotCompleted())) {
                            try {
                                queueAndMutex.wait();
                            } catch (InterruptedException e) {
                                /*
                                 * We don't want our wait to be interrupted,
                                 * but if current thread got interrupted,
                                 * we restore interrupt status after the wait,
                                 * to make user code (in split runnables) aware of it.
                                 */
                                interrupted = true;
                            }
                        }
                        if (interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    if (queueAndMutex.size() == 0) {
                        /*
                         * If untilShotCompletion is true, that means that
                         * either we did not wait, or stopped waiting because
                         * shot completed, so we're done.
                         * If it is false, then we're done as well because we
                         * don't have to wait for shot completion.
                         */
                        break;
                    }
                    // Here we know that the queue is not empty.
                    task = queueAndMutex.removeLast();
                }
                
                // Does one fork/run/merge pass, and then completes the task.
                // Not supposed to throw, since our tasks
                // catch any Throwable they get internally.
                task.runTask();
                
                if (this.shotCompleted()) {
                    // Our shot completed (not necessarily due to the completion
                    // of the task we just ran).
                    break;
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Executor executor;

    private final int parallelism;
    
    private final int maxDepth;
    
    /**
     * Used as mutex to guard itself, and to wait/notify for work and/or shots
     * completion.
     */
    private final MyLifo<MyInterfaceTask> queueAndMutex = new MyLifo<MyInterfaceTask>();
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses no uncaught exception handler: it will only be possible to be aware
     * of the first detected exception, when it is rethrown in caller thread
     * at the end of the parallelization.
     * 
     * @param executor Executor to use.
     * @param executorParallelism Executor's parallelism. Must be >= 1.
     *        If is 1, the specified Executor won't be used,
     *        only the user thread.
     * @param maxDepth Max depth of the split tree. Must be >= 0.
     *        0 means no split.
     * @throws NullPointerException if the specified executor is null.
     * @throws IllegalArgumentException if executorParallelism <= 0 or discrepancy < 0.
     */
    public ExecutorParallelizer(
            Executor executor,
            int executorParallelism,
            int maxDepth) {
        this(
                executor,
                executorParallelism,
                maxDepth,
                null); // exceptionHandler
    }

    /**
     * @param executor Executor to use.
     * @param executorParallelism Executor's parallelism. Must be >= 1.
     *        If is 1, the specified Executor won't be used,
     *        only the user thread.
     * @param maxDepth Max depth of the split tree. Must be >= 0.
     *        0 means no split.
     * @param exceptionHandler Handler for exceptions throw during
     *        {split, run, merge} executions. Can be null.
     *        Whether or not it is null, such exceptions cause
     *        an abort (early stop) of the parallelization. 
     *        Exceptions rethrown by this handler, if any,
     *        might go up the stack into the backing executor,
     *        which would reduce actual parallelism if its threads
     *        die on exceptions.
     * @throws NullPointerException if the specified executor is null.
     * @throws IllegalArgumentException if executorParallelism <= 0 or discrepancy < 0.
     */
    public ExecutorParallelizer(
            Executor executor,
            int executorParallelism,
            int maxDepth,
            UncaughtExceptionHandler exceptionHandler) {
        LangUtils.requireNonNull(executor);
        NbrsUtils.requireSup(0, executorParallelism, "executorParallelism");
        NbrsUtils.requireSupOrEq(0, maxDepth, "maxDepth");
        this.executor = executor;
        this.parallelism = executorParallelism;
        this.maxDepth = maxDepth;
        this.exceptionHandler = exceptionHandler;
    }
    
    /**
     * @return The backing Executor.
     */
    public Executor getExecutor() {
        return this.executor;
    }

    /**
     * Actual parallelism may be one unit above, since user thread can be used
     * as worker thread (allows to avoid starving if user thread is a worker
     * thread of the backing Executor).
     */
    @Override
    public int getParallelism() {
        return this.parallelism;
    }
    
    /**
     * @return The max depth of split tree. >= 0.
     */
    public int getMaxDepth() {
        return this.maxDepth;
    }

    /**
     * Can also directly throw any exception thrown by the specified runnable
     * if it's ran directly.
     * 
     * @param runnable A Runnable, possibly splittable or splitmergable.
     * @throws RethrowException wrapping first detected throwable
     *         thrown by a worker thread.
     */
    @Override
    public void execute(Runnable runnable) {
        if ((this.parallelism <= 1)
                || (!(runnable instanceof InterfaceSplittable))) {
            // Implicit null check.
            runnable.run();
            return;
        }
        
        final InterfaceSplittable splittable = (InterfaceSplittable) runnable;
        // Implicit null check.
        if (!splittable.worthToSplit()) {
            splittable.run();
            return;
        }
        
        /*
         * Using parallelism.
         */
        
        final MyShotHandler shotHandler = new MyShotHandler(
                this,
                this.maxDepth);

        final boolean isSplitmergable = (splittable instanceof InterfaceSplitmergable);
        final MyInterfaceTask task;
        if (isSplitmergable) {
            task = new MySpmTask(
                    (InterfaceSplitmergable) splittable,
                    shotHandler,
                    0, // depth
                    null); // parent
        } else {
            task = new MySpTask(
                    splittable,
                    shotHandler,
                    0); // depth
        }
        
        shotHandler.enqueueTask(task);

        /*
         * Parallel execution happens here.
         * 
         * Always using calling thread as worker thread, in case it is a thread
         * of executor, for we need to wait for shot completion, and we don't
         * want to block tasks execution for it could cause deadlocks.
         */
        
        final boolean untilShotCompletion = true;
        shotHandler.forkRunMergeAnyTask(untilShotCompletion);
        
        /*
         * 
         */

        final Throwable thrown = shotHandler.firstThrown;
        if (thrown != null) {
            throw new RethrowException(thrown);
        }
        
        if (isSplitmergable) {
            final InterfaceSplitmergable resultHolder = ((MySpmTask) task).get();
            if (resultHolder != splittable) {
                ((InterfaceSplitmergable) splittable).merge(resultHolder, null);
            }
        }
    }
}
