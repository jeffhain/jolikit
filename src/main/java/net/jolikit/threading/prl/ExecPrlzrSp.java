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
import net.jolikit.threading.basics.InterfaceCancellable;

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
        /**
         * Called from shot handler's runTasksFromQueue().
         */
        @Override
        public void run() {
            final MyShotHandler shotHandler = this.shotHandler;
            try {
                this.forkRun();
            } finally {
                shotHandler.onEnqueuedTaskCompletion();
            }
        }
        public void forkRun() {
            /*
             * Forking.
             */
            try {
                this.forkAsNeeded();
            } catch (Throwable e) {
                this.shotHandler.onThrowable(e);
            }
            /*
             * Running.
             */
            try {
                this.sp.run();
            } catch (Throwable e) {
                this.shotHandler.onThrowable(e);
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
     * The extended AtomicInteger holds the number of tasks enqueued,
     * and is notified when it reaches 0, for call to executePrl()
     * to complete only once all enqueued tasks from this shot
     * have been dequeued, even in case of exceptions,
     * because we don't want them to linger in the queue and be
     * executed and throw into UEH during subsequent parallelizations,
     * which could be confusing.
     */
    private static class MyShotHandler extends AtomicInteger implements InterfaceCancellable {
        private static final long serialVersionUID = 1L;
        final ExecPrlzrSp owner;
        final int maxDepth;
        /**
         * Write guarded by synchronization on this.
         */
        private volatile Throwable firstDetected;
        public MyShotHandler(
            ExecPrlzrSp owner,
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
                /*
                 * Will be rethrown by executePrl().
                 */
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
        public void enqueueTask(MySpTask task) {
            this.incrementAndGet();
            
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                this.owner.queueAndMutex;
            synchronized (queueAndMutex) {
                queueAndMutex.addLast(task);
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
        /**
         * Must be called on enqueued task completion (normal or not).
         */
        public void onEnqueuedTaskCompletion() {
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
                queueAndMutex.notifyAll();
            }
        }
        /**
         * Completion means all enqueued tasks have been ran:
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
        public boolean shotCompleted() {
            return (this.get() == 0);
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
            final ExecPrlzrSp owner = this.owner;
            final ExecPrlzrLifo<Runnable> queueAndMutex =
                owner.queueAndMutex;
            
            /*
             * Shot might be completed when entering this method,
             * and might also be completed due to completion
             * of tasks not ran in current thread.
             */
            while (!this.shotCompleted()) {
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
        
        /*
         * Forking and running root task in user thread,
         * to minimize need for worker threads
         * and corresponding context switches.
         */
        
        task.forkRun();
        
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
        
        final Throwable firstDetected = shotHandler.firstDetected;
        if (firstDetected != null) {
            throw new RethrowException(firstDetected);
        }
    }
}
