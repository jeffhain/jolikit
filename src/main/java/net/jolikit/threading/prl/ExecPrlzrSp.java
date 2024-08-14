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

import net.jolikit.lang.RethrowException;

/**
 * Implements splittables parallelization, sharing a queue
 * with splitmergables parallelization.
 */
class ExecPrlzrSp {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MySpTask implements Runnable {
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
        public void run() {
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
        final ExecPrlzrSp owner;
        final int maxDepth;
        /**
         * Write guarded by synchronization on this.
         */
        private volatile Throwable firstThrown;
        public MyShotHandler(
            ExecPrlzrSp owner,
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
        public void enqueueTask(MySpTask task) {
            this.incrementAndGet();
            final ExecPrlzrLifo<Runnable> queueAndMutex = this.owner.queueAndMutex;
            synchronized (queueAndMutex) {
                queueAndMutex.addLast(task);
                // To wake up the thread in execute(Runnable) method that would
                // be waiting for some task (or completion of its shot).
                queueAndMutex.notifyAll();
            }
        }
        public void enqueueTaskAndExecuteHandler(MySpTask task) {
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
            
            final ExecPrlzrSp owner = this.owner;
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
                 * Can be just a splittable task or also a splitmergable task
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
    
    public ExecPrlzrSp(
        Executor executor,
        int maxDepth,
        ExecPrlzrLifo<Runnable> queueAndMutex,
        UncaughtExceptionHandler exceptionHandler) {
        
        this.executor = executor;
        this.maxDepth = maxDepth;
        this.queueAndMutex = queueAndMutex;
        this.exceptionHandler = exceptionHandler;
    }
    
    public void executePrl(InterfaceSplittable splittable) {
        
        final MyShotHandler shotHandler = new MyShotHandler(
            this,
            this.maxDepth);
        
        final MySpTask task =
            new MySpTask(
                splittable,
                shotHandler,
                0); // depth
        
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
    }
}
