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

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.RethrowException;

/**
 * Parallelizer based on an Executor's execute(Runnable) method.
 * Use of executors applying backpressure on queue full,
 * such as ThreadPoolExecutor, is discouraged, as it could cause workers
 * to block on execute() calls forever.
 * FixedThreadExecutor on the other hand is well suited.
 * 
 * worthToSplit() method can be called once in calling thread before eventually
 * parallelizing, but it does not need to be made thread-safe for that matter.
 * 
 * If a splittable splits into a splitmergable, the splitmergable will just be
 * considered as a splittable, i.e. no merge will be done.
 * 
 * This implementation is reentrant, i.e. execute(Runnable) method can be used
 * from within a runnable or splittable or splitmergable being executed.
 * 
 * This implementation always uses the calling thread as a worker thread,
 * to avoid deadlocks when calling threads are worker threads
 * of the backing executor.
 * This increases actual parallelism by one.
 * 
 * Exceptions:
 * This parallelizer submits commands implementing InterfaceCancellable
 * to the backing executor, and does nothing in onCancel(),
 * to adapt gracefully to executor recognizing InterfaceCancellable
 * and rejecting on queue full instead of applying backpressure
 * like ThreadPoolExecutor
 * If the executor rejects on queue full but doesn't recognize
 * InterfaceCancellable, it should throw RejectedExecutionException
 * instead, so in this case the parallelization will complete
 * exceptionally.
 * First detected throwable thrown by runnable/splittables/splitmergables
 * is either thrown directly or wrapper in a RethrowException,
 * and subsequently detected throwables are fed to the UEH if any,
 * else silently swallowed, as any throwable thrown by the UEH
 * (for consistency with the no-UEH case, and to avoid risk
 * of taking backing executor's threads down).
 * Best effort work policy: all runnables, input one or resulting
 * from splits, are ran, and merged in case of splitmergables,
 * to do best effort work coverage even in case of exceptions;
 * it also allows not to have to add the overhead of a recurrent
 * problem check for fail-fast.
 * 
 * Interrupts:
 * If the calling thread or a worker thread of the backing executor
 * is interrupted before or during the parallelization,
 * the interrupt status is preserved by the parallelization.
 */
public class ExecutorParallelizer implements InterfaceParallelizer {
    
    /*
     * Tasks are enqueued/dequeued in LIFO. It causes more contention on a same
     * end of the queue, but causes possibly way more split-and-not-yet-run
     * tasks, is more cache-friendly, and causes "inner shots" to be completed
     * early.
     */
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Executor executor;

    private final int parallelism;
    
    private final int maxDepth;
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    private final ExecPrlzrSp prlzrSp;
    
    private final ExecPrlzrSpm prlzrSpm;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses no uncaught exception handler: it will only be possible to be aware
     * of the first detected exception, when it is rethrown in calling thread
     * at the end of the parallelization.
     * 
     * @param executor Executor to use. Use of executors applying backpressure
     *        on queue full, such as ThreadPoolExecutor, is discouraged,
     *        as it could cause workers to block on execute() calls forever.
     *        FixedThreadExecutor on the other hand is well suited.
     * @param executorParallelism Executor's parallelism. Must be >= 1.
     *        If is 1, the specified Executor won't be used,
     *        only the calling thread.
     * @param maxDepth Max depth of the split tree. Must be >= 0.
     *        0 means no split.
     * @throws NullPointerException if the specified executor is null.
     * @throws IllegalArgumentException if executorParallelism <= 0 or maxDepth < 0.
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
     * @param executor Executor to use. Use of executors applying backpressure
     *        on queue full, such as ThreadPoolExecutor, is discouraged,
     *        as it could cause workers to block on execute() calls forever.
     *        FixedThreadExecutor on the other hand is well suited.
     * @param executorParallelism Executor's parallelism. Must be >= 1.
     *        If is 1, the specified Executor won't be used,
     *        only the calling thread.
     * @param maxDepth Max depth of the split tree. Must be >= 0.
     *        0 means no split.
     * @param exceptionHandler Handler for exceptions throw during
     *        {worthToSplit(), split(), run(), merge(), execute()}
     *        calls (except first detected exception which is
     *        rethrown by this method), either in worker threads
     *        or in calling thread. Can be null.
     * @throws NullPointerException if the specified executor is null.
     * @throws IllegalArgumentException if executorParallelism <= 0 or maxDepth < 0.
     * @throws RethrowException wrapping first detected exception,
     *         or this exception directly.
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
        
        /*
         * Used as mutex to guard itself, and to wait/notify for work and/or shots
         * completion.
         * 
         * Queue shared for splittable and splitmergable parallelizations,
         * so that they can help each other and share threads without blocking them.
         */
        final ExecPrlzrLifo<Runnable> queueAndMutex =
            new ExecPrlzrLifo<Runnable>();

        this.prlzrSp = new ExecPrlzrSp(
            executor,
            maxDepth,
            queueAndMutex,
            exceptionHandler);
        this.prlzrSpm = new ExecPrlzrSpm(
            executor,
            maxDepth,
            queueAndMutex,
            exceptionHandler);
    }
    
    /**
     * @return The backing Executor.
     */
    public Executor getExecutor() {
        return this.executor;
    }

    /**
     * Actual parallelism may be one unit above, since calling thread can be used
     * as worker thread (allows to avoid starving if calling thread is a worker
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
     * @param runnable A Runnable, possibly splittable or splitmergable.
     * @throws NullPointerException if the specified runnable is null.
     * @throws First detected throwable thrown by calling thread or
     *         a worker thread, while executing this parallelization's work
     *         (not while helping with concurrent parallelizations),
     *         eventually wrapped in a RethrowException.
     *         Might be an exception from the runnable,
     *         or from the executor, such as RejectedExecutionException.
     */
    @Override
    public void execute(Runnable runnable) {
        if ((this.parallelism <= 1)
            || (!(runnable instanceof InterfaceSplittable))) {
            // Implicit null check.
            runnable.run();
        } else {
            // Never null here.
            final InterfaceSplittable splittable = (InterfaceSplittable) runnable;
            // We want to run even if worthToSplit() throws
            // (best effort work policy).
            boolean worthToSplit = false;
            Throwable firstDetected = null;
            try {
                worthToSplit = splittable.worthToSplit();
            } catch (Throwable e) {
                firstDetected = e;
            }
            if (!worthToSplit) {
                try {
                    splittable.run();
                } catch (Throwable e) {
                    if (firstDetected == null) {
                        firstDetected = e;
                    } else {
                        if (this.exceptionHandler != null) {
                            try {
                                this.exceptionHandler.uncaughtException(Thread.currentThread(), e);
                            } catch (Throwable e2) {
                                // ignored (as if null handler)
                            }
                        }
                    }
                }
                if (firstDetected != null) {
                    throw new RethrowException(firstDetected);
                }
            } else {
                /*
                 * worthToSplit() returned true.
                 * Using parallelism.
                 */
                if (splittable instanceof InterfaceSplitmergable) {
                    this.prlzrSpm.executePrl((InterfaceSplitmergable) splittable);
                } else {
                    this.prlzrSp.executePrl(splittable);
                }
            }
        }
    }
}
