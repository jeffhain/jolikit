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
package net.jolikit.time.sched.misc;

import java.util.concurrent.ThreadFactory;

import net.jolikit.lang.LangUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.misc.SchedulingHelper.SchedulingType;
import net.jolikit.time.sched.misc.SchedulingHelper.SoftAsapExecutionType;
import net.jolikit.time.sched.soft.SoftScheduler;

/**
 * Helper class for running unit tests with SOFT scheduling.
 */
public class SoftTestHelper {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * SOFT_AFAP for tests to run fast.
     */
    private static final SchedulingType DEFAULT_SCHEDULING_TYPE = SchedulingType.SOFT_AFAP;
    
    private static final SoftAsapExecutionType DEFAULT_SOFT_ASAP_EXECUTION_TYPE = SoftAsapExecutionType.ASYNCHRONOUS;
    
    /**
     * Not zero, to avoid special case, and confusion between time and delay.
     * Far enough from 1 nanosecond steps, and from typical 1 second delays.
     */
    private static final long DEFAULT_INITIAL_TIME_NS = 1000L;
    
    private final SchedulingHelper schedulingHelper;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses SOFT_AFAP scheduling type,
     * ASYNCHRONOUS soft ASAP execution type,
     * and 1000 ns as initial time.
     */
    public SoftTestHelper() {
        this(
                DEFAULT_SCHEDULING_TYPE,
                DEFAULT_SOFT_ASAP_EXECUTION_TYPE,
                DEFAULT_INITIAL_TIME_NS);
    }
    
    /**
     * @param schedulingType Use SOFT_AFAP for tests to run fast,
     *        SOFT_HARD_BASED for more real time behavior.
     * @param softAsapExecutionType Must not be null.
     * @param initialTimeNs Initial clock time, in nanoseconds.
     */
    public SoftTestHelper(
            SchedulingType schedulingType,
            SoftAsapExecutionType softAsapExecutionType,
            long initialTimeNs) {
        LangUtils.requireNonNull(schedulingType);
        LangUtils.requireNonNull(softAsapExecutionType);
        if (schedulingType == SchedulingType.HARD) {
            throw new IllegalArgumentException("schedulingType must be a SOFT one");
        }
        
        final long latenessThresholdNs = Long.MAX_VALUE;
        final double initialTimeSpeedIfNotAfap = 1.0;
        final ThreadFactory threadFactory = null; // Unused.
        final SchedulingHelper schedulingHelper = new SchedulingHelper(
                schedulingType,
                softAsapExecutionType,
                latenessThresholdNs,
                initialTimeNs,
                initialTimeSpeedIfNotAfap,
                threadFactory);
        this.schedulingHelper = schedulingHelper;
    }
    
    /**
     * @return The clock.
     */
    public InterfaceClock getClock() {
        return this.schedulingHelper.getClock();
    }
    
    /**
     * @return The scheduler to execute the test.
     */
    public InterfaceScheduler getScheduler() {
        return this.schedulingHelper.getScheduler("unused");
    }
    
    /**
     * All the code to test is meant to be executed during this call,
     * through schedules.
     * 
     * Blocks until all planned schedules did execute,
     * or scheduler stopped, or an exception is thrown.
     * 
     * @param stopTimeNs Time, in nanoseconds, at which scheduler
     *        must be stopped, and this method complete normally.
     */
    public void startNowAndStopAtNs(long stopTimeNs) {
        this.getScheduler().executeAtNs(new Runnable() {
            @Override
            public void run() {
                ((SoftScheduler) getScheduler()).stop();
            }
        }, stopTimeNs);
        
        this.schedulingHelper.startSoftSchedulerIfSoftScheduling();
    }
    
    /**
     * For eventual early stop.
     */
    public void stop() {
        this.schedulingHelper.stopSoftSchedulerIfSoftScheduling();
    }
}
