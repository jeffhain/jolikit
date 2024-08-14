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
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Executor executor;

    private final int parallelism;
    
    private final int maxDepth;
    
    private final ExecPrlzrSp prlzrSp;
    
    private final ExecPrlzrSpm prlzrSpm;
    
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
     *         thrown by a worker thread (possibly while helping
     *         with a concurrent parallelization).
     */
    @Override
    public void execute(Runnable runnable) {
        if ((this.parallelism <= 1)
            || (!(runnable instanceof InterfaceSplittable))) {
            // Implicit null check.
            runnable.run();
        } else {
            final InterfaceSplittable splittable = (InterfaceSplittable) runnable;
            // Implicit null check.
            if (!splittable.worthToSplit()) {
                splittable.run();
            } else {
                /*
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
