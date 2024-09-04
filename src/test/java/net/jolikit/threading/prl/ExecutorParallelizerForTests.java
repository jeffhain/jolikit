/*
 * Copyright 2019 Jeff Hain
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jolikit.threading.execs.FixedThreadExecutor;

public class ExecutorParallelizerForTests extends ExecutorParallelizer implements InterfaceParallelizerForTests {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Executor executor;
    
    private final int discrepancy;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ExecutorParallelizerForTests(
        Executor executor,
        int executorParallelism,
        int discrepancy,
        UncaughtExceptionHandler exceptionHandler) {
        super(
            executor,
            executorParallelism,
            PrlUtils.computeMaxDepth(executorParallelism, discrepancy),
            exceptionHandler);
        this.executor = executor;
        this.discrepancy = discrepancy;
    }
    
    @Override
    public String getSpeDescr() {
        return "["
            + this.executor.getClass().getSimpleName()
            + ",discr = "
            + this.discrepancy
            + " (-> maxDepth = "
            + this.getMaxDepth()
            + ")]";
    }
    
    @Override
    public boolean isReentrant() {
        return true;
    }
    
    @Override
    public boolean executorRejectsWithOnCancelIfCancellable() {
        return (this.executor instanceof FixedThreadExecutor);
    }
    
    @Override
    public void shutdownAndWait() {
        if (this.executor instanceof ThreadPoolExecutor) {
            final ThreadPoolExecutor executor = (ThreadPoolExecutor) this.executor;
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            final FixedThreadExecutor executor = (FixedThreadExecutor) this.executor;
            executor.shutdown();
            try {
                executor.waitForNoMoreRunningWorker(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
