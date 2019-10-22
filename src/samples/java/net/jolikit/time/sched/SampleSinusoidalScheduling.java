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
package net.jolikit.time.sched;

import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.hard.ControllableSystemTimeClock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.soft.RootSoftClock;
import net.jolikit.time.sched.hard.HardScheduler;
import net.jolikit.time.sched.soft.SoftScheduler;

/**
 * Repeated (with delay) scheduling using a sinusoidal time clock,
 * and either soft or hard scheduling.
 */
public class SampleSinusoidalScheduling {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean MUST_USE_SOFT_SCHEDULING = true;
    
    private static final double INITIAL_MASTER_TIME_S = 1000.0;
    
    private static final double SINUSOID_PERIOD_S = 4.0;
    
    /**
     * To make sure sinusoidal time speed is never negative, nor too small.
     */
    private static final double MAX_SINUSOID_AMPLITUDE_S = (SINUSOID_PERIOD_S/Math.PI) * 0.9;
    
    /**
     * The peak-to-peak amplitude.
     */
    private static final double SINUSOID_AMPLITUDE_S = Math.min(MAX_SINUSOID_AMPLITUDE_S, 1.0);
    
    private static final double PROCESS_DELAY_S = 0.1;
    
    /**
     * Small because we have variable time speed, and system wait time
     * is computed using time speed before wait.
     */
    private static final double MAX_SYSTEM_WAIT_TIME_S = 0.001;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Sinusoidal time relative to master clock.
     * 
     * t2 = t1 + (amplitude/2) * (1 + sin((2*Pi/period) * t1))
     * dt2/dt1 = 1 + (amplitude/2) * ((2*Pi/period) * cos((2*Pi/period) * t1))
     */
    private static class MySinusoidalClock implements InterfaceHardClock {
        private final InterfaceClock master;
        public MySinusoidalClock(InterfaceClock master) {
            this.master = master;
        }
        @Override
        public long getTimeNs() {
            return TimeUtils.sToNs(getTimeS());
        }
        @Override
        public double getTimeS() {
            final double masterS = this.master.getTimeS();
            return masterS + (SINUSOID_AMPLITUDE_S/2) * (1.0 + StrictMath.sin((2*Math.PI/SINUSOID_PERIOD_S) * masterS));
        }
        @Override
        public double getTimeSpeed() {
            final double masterS = this.master.getTimeS();
            return 1.0 + ((SINUSOID_AMPLITUDE_S/2) * (2*Math.PI/SINUSOID_PERIOD_S)) * StrictMath.cos((2*Math.PI/SINUSOID_PERIOD_S) * masterS);
        }
    }
    
    private static class MyRepeatedProcess extends AbstractRepeatedProcess {
        public MyRepeatedProcess(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            final double nowS = nsToS(actualTimeNs);
            System.out.println("nowS = " + nowS);
            System.out.flush();
            return sToNs(nsToS(actualTimeNs) + PROCESS_DELAY_S);
        }
    };

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        
        /*
         * Creating clocks.
         */
        
        final ControllableSystemTimeClock masterClock = new ControllableSystemTimeClock();
        // Making sure master time doesn't flow while we initialize.
        masterClock.setTimeSpeed(0.0);
        masterClock.setTimeNs(TimeUtils.sToNs(INITIAL_MASTER_TIME_S));
        
        final InterfaceHardClock sinusoidalClock = new MySinusoidalClock(masterClock);
        
        /*
         * Creating scheduler.
         */
        
        final InterfaceScheduler scheduler;
        if (MUST_USE_SOFT_SCHEDULING) {
            final RootSoftClock rootClock = new RootSoftClock(sinusoidalClock);
            // RootSoftClock starts at 0 by default.
            rootClock.setTimeNs(sinusoidalClock.getTimeNs());
            rootClock.setMaxSystemWaitTimeNs(TimeUtils.sToNs(MAX_SYSTEM_WAIT_TIME_S));
            
            final SoftScheduler schedulerImpl = new SoftScheduler(rootClock);
            scheduler = schedulerImpl;
        } else {
            final HardScheduler schedulerImpl = HardScheduler.newThreadlessInstance(sinusoidalClock);
            schedulerImpl.setMaxSystemWaitTimeNs(TimeUtils.sToNs(MAX_SYSTEM_WAIT_TIME_S));
            scheduler = schedulerImpl;
        }
        
        /*
         * Creating/starting process.
         */
        
        final AbstractRepeatedProcess process = new MyRepeatedProcess(scheduler);
        // Schedules first run.
        process.start();
        
        /*
         * Starting scheduler.
         */
        
        // Now letting master time flow.
        masterClock.setTimeSpeed(1.0);
        
        if (MUST_USE_SOFT_SCHEDULING) {
            final SoftScheduler schedulerImpl = (SoftScheduler) scheduler;
            schedulerImpl.start();
        } else {
            final HardScheduler schedulerImpl = (HardScheduler) scheduler;
            schedulerImpl.startAndWorkInCurrentThread();
        }
    }
}
