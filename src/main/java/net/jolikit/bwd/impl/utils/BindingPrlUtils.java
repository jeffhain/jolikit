/*
 * Copyright 2021-2024 Jeff Hain
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
package net.jolikit.bwd.impl.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.lang.DefaultThreadFactory;
import net.jolikit.threading.prl.ExecutorParallelizer;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;
import net.jolikit.time.sched.hard.HardExecutor;

public class BindingPrlUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Using a HardExecutor instead of a ThreadPoolExecutor,
     * not primarily for its lower overhead,
     * but to avoid spurious ThreadPoolExecutor
     * shutdown and RejectedExecutionException throwings
     * that seem to occur under stress for some reason.
     */
    private static final boolean MUST_USE_HARD_EXEC_FOR_PRLZR_EXECUTOR = true;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static InterfaceParallelizer newParallelizer(
        BaseBwdBindingConfig bindingConfig,
        int parallelism,
        final String threadNamePrefix) {
        final InterfaceParallelizer ret;
        if (parallelism == 1) {
            ret = SequentialParallelizer.getDefault();
        } else {
            final boolean daemon = true;
            /*
             * Uses a default uncaught exception handler,
             * not the one configured in binding config:
             * - To avoid exceptions being handled twice by the configured handler.
             * - To ensure that exceptions are swallowed, and don't make
             *   parallelizer's threads die (HardScheduler doesn't replace
             *   dead threads with new ones as ThreadPoolExecutor does).
             */
            final ThreadFactory prlThreadFactory = new DefaultThreadFactory() {
                final AtomicInteger threadNum = new AtomicInteger();
                @Override
                public Thread newThread(Runnable runnable) {
                    final Thread thread = super.newThread(runnable);
                    final String name =
                        threadNamePrefix
                        + "-" + this.threadNum.incrementAndGet();
                    thread.setName(name);
                    thread.setDaemon(daemon);
                    return thread;
                }
            };
            final Executor executor;
            if (MUST_USE_HARD_EXEC_FOR_PRLZR_EXECUTOR) {
                // Thread name set by our thread factory.
                final String namePrefixForHardScheduler = null;
                executor = HardExecutor.newInstance(
                        namePrefixForHardScheduler,
                        daemon,
                        parallelism,
                        prlThreadFactory);
            } else {
                executor = Executors.newFixedThreadPool(
                        parallelism,
                        prlThreadFactory);
            }
            
            /*
             * Max value, because painters splits unbalance
             * is not bounded.
             */
            final int maxDepth = Integer.MAX_VALUE;
            final ConfiguredExceptionHandler exceptionHandler =
                    new ConfiguredExceptionHandler(bindingConfig);
            ret = new ExecutorParallelizer(
                    executor,
                    parallelism,
                    maxDepth,
                    exceptionHandler);
        }
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingPrlUtils() {
    }
}
