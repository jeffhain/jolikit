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
package net.jolikit.threading.prl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.lang.RethrowException;

/**
 * Implements splitmergables parallelization, sharing a queue
 * with splittables parallelization.
 */
class ExecPrlzrSpm {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * AtomicReference holds reference to first completed splitmergable, if any.
     */
    private static class MySpmTask extends AtomicReference<InterfaceSplitmergable> implements Runnable {
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
        public void run() {
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
        final ExecPrlzrSpm owner;
        final int maxDepth;
        /**
         * Write guarded by synchronization on this.
         */
        private volatile Throwable firstThrown;
        public MyShotHandler(
            ExecPrlzrSpm owner,
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
        public void enqueueTask(MySpmTask task) {
            this.incrementAndGet();
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                this.owner.queueAndMutex;
            synchronized (queueAndMutex) {
                queueAndMutex.addLast(task);
                // To wake up the thread in execute(Runnable) method that would
                // be waiting for some task (or completion of its shot).
                queueAndMutex.notifyAll();
            }
        }
        public void enqueueTaskAndExecuteHandler(MySpmTask task) {
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
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                this.owner.queueAndMutex;
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
            
            final ExecPrlzrSpm owner = this.owner;
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                owner.queueAndMutex;
            
            while (true) {
                final Runnable task;
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
                
                /*
                 * Can be a splitmergable task or just a splittable task
                 * (since queue is shared).
                 * Does one fork/run(/merge) pass, and then completes the task.
                 */
                task.run();
                
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
    
    private final int maxDepth;
    
    /**
     * Used as mutex to guard itself, and to wait/notify for work and/or shots
     * completion.
     */
    private final ExecPrlzrLifo<Runnable> queueAndMutex;
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ExecPrlzrSpm(
        Executor executor,
        int maxDepth,
        ExecPrlzrLifo<Runnable> queueAndMutex,
        UncaughtExceptionHandler exceptionHandler) {
        
        this.executor = executor;
        this.maxDepth = maxDepth;
        this.queueAndMutex = queueAndMutex;
        this.exceptionHandler = exceptionHandler;
    }
    
    public void executePrl(InterfaceSplitmergable splitmergable) {
        
        final MyShotHandler shotHandler =
            new MyShotHandler(
                this,
                this.maxDepth);
        
        final MySpmTask task =
            new MySpmTask(
                splitmergable,
                shotHandler,
                0, // depth
                null); // parent
        
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
        
        final InterfaceSplitmergable resultHolder = task.get();
        if (resultHolder != splitmergable) {
            splitmergable.merge(resultHolder, null);
        }
    }
}
