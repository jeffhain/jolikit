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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import net.jolikit.lang.LangUtils;

/**
 * ExecutorService based on a HardExecutor.
 */
public class HardExecutorService extends AbstractExecutorService {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final HardExecutor hardExecutor;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param hardExecutor Must not be null.
     * @throws NullPointerException if the specified executor is null.
     */
    public HardExecutorService(HardExecutor hardExecutor) {
        this.hardExecutor = LangUtils.requireNonNull(hardExecutor);
    }
    
    /**
     * @return The backing HardExecutor.
     */
    public HardExecutor getHardExecutor() {
        return this.hardExecutor;
    }
    
    @Override
    public void execute(Runnable command) {
        this.hardExecutor.execute(command);
    }

    @Override
    public void shutdown() {
        this.hardExecutor.shutdown();
    }
    
    @Override
    public List<Runnable> shutdownNow() {
        // Can always interrupt workerd directly from HardScheduler.
        final boolean mustInterruptWorkingWorkers = false;
        return this.hardExecutor.shutdownNow(
            mustInterruptWorkingWorkers);
    }
    
    @Override
    public boolean isShutdown() {
        return this.hardExecutor.isShutdown();
    }
    
    @Override
    public boolean isTerminated() {
        return this.hardExecutor.isShutdown()
            && (this.hardExecutor.getNbrOfRunningWorkers() == 0);
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        final long timeoutNs = unit.toNanos(timeout);
        return this.hardExecutor.waitForNoMoreRunningWorkerSystemTimeNs(
            timeoutNs);
    }
}
