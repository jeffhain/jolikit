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
import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.lang.RethrowException;
import net.jolikit.threading.basics.InterfaceCancellable;

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
        /**
         * Called from shot handler's runTasksFromQueue().
         */
        @Override
        public void run() {
            this.forkRunMerge();
        }
        /**
         * Does not throw.
         */
        public void forkRunMerge() {
            final MyShotHandler shotHandler = this.shotHandler;
            MySpmTask currentTask = this;
            /*
             * Forking.
             */
            try {
                while ((currentTask.depth < shotHandler.maxDepth)
                    && currentTask.spm.worthToSplit()) {
                    
                    final int childrenDepth = currentTask.depth + 1;
                    /*
                     * Unlike with splittable,
                     * we can't reuse a same task for
                     * the new state of spm after split,
                     * because we need to keep track
                     * of parent tasks for merging.
                     */
                    final MySpmTask left = new MySpmTask(
                        currentTask.spm,
                        shotHandler,
                        childrenDepth,
                        currentTask);
                    final MySpmTask right = new MySpmTask(
                        currentTask.spm.split(),
                        shotHandler,
                        childrenDepth,
                        currentTask);
                    
                    /*
                     * Doing this after split, to consider
                     * that there was no split if split threw.
                     * We can do this before enqueuing,
                     * since even in case of rejected execution
                     * right task will at least be processed
                     * by user thread.
                     */
                    currentTask = left;
                    
                    shotHandler.enqueueTaskAndExecuteHandler(right);
                }
            } catch (Throwable e) {
                shotHandler.onThrowable(e);
            }
            /*
             * Running.
             */
            try {
                currentTask.spm.run();
            } catch (Throwable e) {
                shotHandler.onThrowable(e);
            }
            /*
             * Merging.
             */
            mergeAsNeeded(currentTask);
        }
        private static void mergeAsNeeded(MySpmTask currentTask) {
            final InterfaceSplitmergable currentSpm = currentTask.spm;
            
            MySpmTask parent = currentTask.parent;
            while (parent != null) {
                InterfaceSplitmergable peer = parent.get();
                if (peer == null) {
                    // CAS ensures visibility on peer for merges.
                    if (parent.compareAndSet(null, currentSpm)) {
                        // Peer will take care of merging.
                        return;
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
                try {
                    currentSpm.merge(currentSpm, peer);
                } catch (Throwable e) {
                    currentTask.shotHandler.onThrowable(e);
                }
                currentTask = parent;
                parent = parent.parent;
            }
            
            /*
             * Here parent is null: current task is root task,
             * so currentSpm holds the result and we are done.
             */
            currentTask.shotHandler.onMergeCompletion(currentSpm);
        }
    }
    
    /*
     * 
     */
    
    /**
     * Info specific to a call to parallelizer.execute(Runnable).
     * 
     * Extended AtomicReference holds reference to result holding SPM
     * (i.e. the holder of the final merge, if any).
     */
    private static class MyShotHandler
    extends AtomicReference<InterfaceSplitmergable>
    implements InterfaceCancellable {
        private static final long serialVersionUID = 1L;
        final ExecPrlzrSpm owner;
        final int maxDepth;
        /**
         * Write guarded by synchronization on this.
         */
        private volatile Throwable firstDetected;
        public MyShotHandler(
            ExecPrlzrSpm owner,
            int maxDepth) {
            this.owner = owner;
            this.maxDepth = maxDepth;
        }
        /**
         * @param throwable
         * @return True if was first detected.
         */
        private synchronized boolean setIfFirst(Throwable throwable) {
            boolean didSet = false;
            if (this.firstDetected == null) {
                this.firstDetected = throwable;
                didSet = true;
            }
            return didSet;
        }
        public void onThrowable(Throwable throwable) {
            final boolean didSet = this.setIfFirst(throwable);
            if (didSet) {
                // Will be rethrown by executePrl().
            } else {
                final UncaughtExceptionHandler handler = owner.exceptionHandler;
                if (handler != null) {
                    try {
                        handler.uncaughtException(Thread.currentThread(), throwable);
                    } catch (@SuppressWarnings("unused") Throwable e) {
                        // ignored (as if null handler)
                    }
                }
            }
        }
        public void enqueueTask(MySpmTask task) {
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                this.owner.queueAndMutex;
            synchronized (queueAndMutex) {
                queueAndMutex.addLast(task);
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
             * If the executor applies backpressure on queue full,
             * the parallelizer might block, that's why use of
             * such executors is discouraged.
             * If the executor throws RejectedExecutionException,
             * our tasks will catch it and treat it as any other issue
             * (throwable) thrown during execution, so in this case
             * the parallelization will complete exceptionally.
             * If the executor recognizes InterfaceCancellable
             * and rejects the task by calling onCancel(),
             * due to queue full, shutdown, or else,
             * we will just do nothing in onCancel() method
             * and will just not benefit from the attempted parallelism
             * (at worse, remaining tasks will be taken care of
             * by calling thread).
             */
            this.owner.executor.execute(this);
        }
        private void onMergeCompletion(InterfaceSplitmergable mergedRootSpm) {
            // Lazy set is fine since we use synchronized afterwards.
            this.lazySet(mergedRootSpm);
            this.notifyAllQueueInLock();
        }
        private void notifyAllQueueInLock() {
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                this.owner.queueAndMutex;
            synchronized (queueAndMutex) {
                queueAndMutex.notifyAll();
            }
        }
        /**
         * Completion means all enqueued tasks have been ran
         * (and merged up to root task and into shot handler):
         * - to avoid risk of indefinte queue growth in case
         *   of systematic exceptions,
         * - to avoid having to check for thrown exception
         *   before doing work, which would add an overhead
         *   we don't want for the normal completion cases,
         * - to avoid task exceptions to be fed to UEH during
         *   subsequent parallelizations, which could be
         *   confusing to the user,
         * - in case user wants to do best effort work
         *   even in case of exceptions.
         * 
         * @return True if all enqueued tasks of this shot
         *         have been ran and completed (normally or not).
         */
        public boolean isShotCompleted() {
            final InterfaceSplitmergable resultSpm = this.get();
            return (resultSpm != null);
        }
        /**
         * Runs tasks from queue until queue is found empty.
         * Called from executor.
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
            this.runTasksFromQueue(untilShotCompletion);
        }
        @Override
        public void onCancel() {
            /*
             * Doing nothing here. At worse remaining tasks
             * will be taken care of by calling thread.
             */
        }
        /**
         * @param untilShotCompletion If true keeps running tasks
         *        until shot completion, else completes as soon as queue
         *        is found empty.
         */
        public void runTasksFromQueue(boolean untilShotCompletion) {
            final ExecPrlzrSpm owner = this.owner;
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                owner.queueAndMutex;
            
            /*
             * Shot might be completed when entering this method,
             * and might also be completed due to completion
             * of tasks not ran in current thread.
             */
            while (!this.isShotCompleted()) {
                final Runnable task;
                synchronized (queueAndMutex) {
                    if (untilShotCompletion) {
                        boolean interrupted = false;
                        // If queue is empty, waiting for more tasks
                        // or for shot completion.
                        while ((queueAndMutex.size() == 0)
                            && (!this.isShotCompleted())) {
                            try {
                                queueAndMutex.wait();
                            } catch (@SuppressWarnings("unused") InterruptedException e) {
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
                         * If isShotCompleted() is true, that means that
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
    
    /**
     * Must only be called if the specified splitmergable is worth to split.
     */
    public void executePrl(InterfaceSplitmergable splitmergable) {
        
        final MyShotHandler shotHandler =
            new MyShotHandler(
                this,
                this.maxDepth);
        
        final MySpmTask rootTask =
            new MySpmTask(
                splitmergable,
                shotHandler,
                0, // depth
                null); // parent
        
        /*
         * Running root task in user thread,
         * to minimize need for worker threads
         * and corresponding context switches.
         */
        
        rootTask.forkRunMerge();
        
        /*
         * Parallel execution happens here.
         * 
         * Always using calling thread as worker thread, in case it is a thread
         * of executor, for we need to wait for shot completion, and we don't
         * want to block tasks execution for it could cause deadlocks.
         */
        
        final boolean untilShotCompletion = true;
        shotHandler.runTasksFromQueue(untilShotCompletion);
        
        /*
         * 
         */
        
        final InterfaceSplitmergable resultHolder = shotHandler.get();
        if (resultHolder != splitmergable) {
            try {
                splitmergable.merge(resultHolder, null);
            } catch (Throwable e) {
                shotHandler.onThrowable(e);
            }
        }
        
        /*
         * 
         */
        
        final Throwable firstDetected = shotHandler.firstDetected;
        if (firstDetected != null) {
            throw new RethrowException(firstDetected);
        }
    }
}
