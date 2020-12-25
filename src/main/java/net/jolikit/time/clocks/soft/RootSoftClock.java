/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.time.clocks.soft;

import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.locks.MonitorCondilock;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.ClockListeners;
import net.jolikit.time.clocks.ClocksUtils;
import net.jolikit.time.clocks.InterfaceClockModificationListener;
import net.jolikit.time.clocks.SignalingClockListener;
import net.jolikit.time.clocks.hard.HardClockCondilock;
import net.jolikit.time.clocks.hard.InterfaceHardClock;

/**
 * Simple implementation for non-enslaved (root) soft clocks.
 * 
 * Time setting methods block until the specified time is allowed to occur
 * (behave a bit like the "time advance request" method of HLA norm,
 * but to keep things simple we didn't want to add an interface for it).
 * 
 * Constructor with no hard clock argument creates an ASAP clock.
 * Constructor with hard clock argument creates a clock which time,
 * by default, advances according to absolute time speed of the
 * hard clock, but which can also be switched to ASAP mode.
 * 
 * Not thread-safe (for soft clocks, thread safety would be more
 * of a performance handicap than a typical use case requirement).
 */
public class RootSoftClock implements InterfaceSoftClock {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceHardClock hardClock;

    private final ClockListeners listeners;
    
    private final Object waitMutex = new Object();
    private final HardClockCondilock hardClockTimeWaitCondilock;
    private final InterfaceClockModificationListener conditionSignalingClockListener;

    /*
     * 
     */
    
    private double timeS;
    private long timeNs;
    
    /*
     * 
     */
    
    /**
     * Amount of lateness, relative to (hard clock's time - annulled lateness)
     * above which current lateness is added to amount of annulled lateness.
     */
    private long latenessThresholdNs = 0;
    
    /**
     * Opposite of lateness threshold, except if lateness threshold
     * is Long.MAX_VALUE, in which case Long.MIN_VALUE is used, to
     * make sure lateness is not forgiven if it is Long.MIN_VALUE.
     */
    private long negLatenessThresholdNs;
    
    /**
     * Current amount of annulled lateness (>= 0).
     */
    private long annulledLatenessNs = 0;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a clock with time of 0 and time speed of +Infinity
     * (for ASAP scheduling), with Long.MAX_VALUE lateness threshold.
     */
    public RootSoftClock() {
        this.listeners = new ClockListeners(this);
        
        this.hardClock = null;
        this.hardClockTimeWaitCondilock = null;
        this.conditionSignalingClockListener = null;

        this.setLatenessThresholdNS(Long.MAX_VALUE);
    }
    
    /**
     * Creates a clock with time of 0 and time speed of 1 (relative to master clock),
     * with Long.MAX_VALUE lateness threshold.
     * 
     * Must be started before use.
     * @param hardClock Master hard clock.
     */
    public RootSoftClock(InterfaceHardClock hardClock) {
        if (hardClock == null) {
            throw new NullPointerException();
        }
        this.listeners = new ClockListeners(this);
        
        this.hardClock = hardClock;
        this.hardClockTimeWaitCondilock = new HardClockCondilock(
                hardClock,
                new MonitorCondilock(this.waitMutex));
        this.conditionSignalingClockListener =
                new SignalingClockListener(this.hardClockTimeWaitCondilock);
        
        this.setLatenessThresholdNS_private(Long.MAX_VALUE);
    }
    
    public void setMaxSystemWaitTimeNs(long maxSystemWaitTimeNs) {
        this.hardClockTimeWaitCondilock.setMaxSystemWaitTimeNs(maxSystemWaitTimeNs);
    }
    
    public long getLatenessThresholdNS() {
        return this.latenessThresholdNs;
    }

    /**
     * Only relevant for non-ASAP clocks (i.e. if a hard clock has been specified).
     * 
     * Current lateness is forgiven each time it is seen (from within setTimeXXX methods)
     * to be strictly superior to this threshold, from within setTimeXXX methods.
     * 
     * Long.MAX_VALUE is a special value that ensures the soft clock always
     * tries to catch up with the hard clock.
     * 
     * @param latenessThresholdNs Amount of lateness (>=0) above which all current lateness is forgiven.
     */
    public void setLatenessThresholdNS(long latenessThresholdNs) {
        this.setLatenessThresholdNS_private(latenessThresholdNs);
    }

    /**
     * Starts listening to hard clock's modifications.
     * Does nothing if there is no hard clock.
     */
    public void startListening() {
        if (this.hardClock != null) {
            ClocksUtils.addListenerToAll(this.hardClock, this.conditionSignalingClockListener);
        }
    }

    /**
     * Stops listening to hard clock's modifications.
     * Does nothing if there is no hard clock.
     */
    public void stopListening() {
        if (this.hardClock != null) {
            ClocksUtils.removeListenerFromAll(this.hardClock, this.conditionSignalingClockListener);
        }
    }
    
    /*
     * 
     */

    @Override
    public double getTimeS() {
        return this.timeS;
    }
    
    @Override
    public long getTimeNs() {
        return this.timeNs;
    }
    
    @Override
    public double getTimeSpeed() {
        if (this.hardClock == null) {
            return Double.POSITIVE_INFINITY;
        } else {
            return ClocksUtils.computeAbsoluteTimeSpeed(this.hardClock);
        }
    }

    /*
     * 
     */
    
    @Override
    public boolean addListener(InterfaceClockModificationListener listener) {
        return this.listeners.addListener(listener);
    }
    
    @Override
    public boolean removeListener(InterfaceClockModificationListener listener) {
        return this.listeners.removeListener(listener);
    }

    /*
     * 
     */
    
    /**
     * Waits until requested time can be reached, if needed, then sets it.
     */
    @Override
    public void setTimeNs(long timeNs) {
        this.requestTimeModificationNS(timeNs);
        this.timeS = TimeUtils.nsToS(timeNs);
        this.timeNs = timeNs;
        this.listeners.notifyListeners();
    }
    
    /**
     * Time speed is infinite if there is no reference hard clock,
     * else it's hard clock's absolute time speed.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public void setTimeSpeed(double timeSpeed) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void setTimeNsAndTimeSpeed(long timeNs, double timeSpeed) {
        throw new UnsupportedOperationException();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void setLatenessThresholdNS_private(long latenessThresholdNs) {
        if (latenessThresholdNs < 0) {
            throw new IllegalArgumentException(
                    "lateness threshold [" + latenessThresholdNs + " ns] must be >= 0");
        }
        this.latenessThresholdNs = latenessThresholdNs;
        if (latenessThresholdNs == Long.MAX_VALUE) {
            this.negLatenessThresholdNs = Long.MIN_VALUE;
        } else {
            this.negLatenessThresholdNs = -latenessThresholdNs;
        }
    }

    /**
     * @return Granted time, i.e. a time between clock's time before call and requested time.
     */
    private long requestTimeModificationNS(long requestedTimeNs) {
        if (this.hardClock == null) {
            // ASAP soft clock.
            return requestedTimeNs;
        }
        // requestedTimeNs in soft clock time.
        synchronized (this.waitMutex) {
            while (true) {
                // Hard time, not considering annulled lateness.
                final long rectifiedHardTimeNs = NbrsUtils.minusBounded(
                        this.hardClock.getTimeNs(),
                        this.annulledLatenessNs);
                final long clockWaitTimeNs = NbrsUtils.minusBounded(
                        requestedTimeNs,
                        rectifiedHardTimeNs);
                if (clockWaitTimeNs <= 0) {
                    /*
                     * Soft clock on time or late: done waiting.
                     * Also updating annulled lateness, in case
                     * it is needed.
                     */
                    if (clockWaitTimeNs < this.negLatenessThresholdNs) {
                        /*
                         * We are too late: forgiving current lateness (not trying to catch it up).
                         * "lateness" = "minus soft clock wait time",
                         * but we don't take minus, for -Long.MIN_VALUE does not exist as long.
                         */
                        this.annulledLatenessNs = NbrsUtils.minusBounded(
                                this.annulledLatenessNs,
                                clockWaitTimeNs);
                    }
                    return requestedTimeNs;
                }
                
                try {
                    this.hardClockTimeWaitCondilock.awaitNanos(clockWaitTimeNs);
                } catch (InterruptedException e) {
                    // quiet
                }
            }
        }
    }
}
