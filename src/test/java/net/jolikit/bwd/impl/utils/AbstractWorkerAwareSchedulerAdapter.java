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
package net.jolikit.bwd.impl.utils;

import net.jolikit.lang.LangUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

/**
 * Adapter to help implementing a worker aware thread scheduler
 * on top of a scheduler.
 */
public abstract class AbstractWorkerAwareSchedulerAdapter implements InterfaceWorkerAwareScheduler {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceScheduler scheduler;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param scheduler Must not be null.
     * @throws NullPointerException if the specified scheduler is null.
     */
    public AbstractWorkerAwareSchedulerAdapter(InterfaceScheduler scheduler) {
        this.scheduler = LangUtils.requireNonNull(scheduler);
    }

    /**
     * @return The wrapped scheduler.
     */
    public InterfaceScheduler getScheduler() {
        return this.scheduler;
    }
    
    /*
     * 
     */
    
    @Override
    public String toString() {
        return this.scheduler.toString();
    }
    
    /*
     * 
     */

    @Override
    public InterfaceClock getClock() {
        return this.scheduler.getClock();
    }
    
    @Override
    public void execute(Runnable runnable) {
        this.scheduler.execute(runnable);
    }
    
    @Override
    public void executeAtNs(Runnable runnable, long timeNs) {
        this.scheduler.executeAtNs(runnable, timeNs);
    }
    
    @Override
    public void executeAfterNs(Runnable runnable, long delayNs) {
        this.scheduler.executeAfterNs(runnable, delayNs);
    }
    
    @Override
    public void executeAtS(Runnable runnable, double timeS) {
        this.scheduler.executeAtS(runnable, timeS);
    }
    
    @Override
    public void executeAfterS(Runnable runnable, double delayS) {
        this.scheduler.executeAfterS(runnable, delayS);
    }
}
