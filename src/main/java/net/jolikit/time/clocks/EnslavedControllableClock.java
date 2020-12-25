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
package net.jolikit.time.clocks;

import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.locks.InterfaceCheckerLock;
import net.jolikit.threading.locks.ReentrantCheckerLock;
import net.jolikit.time.TimeUtils;

/**
 * Class for controllable linearly enslaved clocks.
 * 
 * This class is thread-safe if master clock is thread-safe
 * (might be overkill for soft clocks, but is handy for hard clocks).
 * 
 * A checker lock is used to make sure clock is not modified while
 * listeners are being notified of a modification.
 * To avoid deadlocks, you must therefore follow some rule.
 */
public class EnslavedControllableClock implements InterfaceEnslavedControllableClock {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final AtomicReference<ClockLinearTimeRef> timeRef = new AtomicReference<ClockLinearTimeRef>();
    
    private final InterfaceClock masterClock;
    
    /**
     * We need a checker lock, for we don't want to allow another time change
     * while a time change is already in process and listeners being called.
     */
    private final InterfaceCheckerLock modificationLock;
    
    private final ClockListeners listeners;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a modifiable clock enslaved to the specified master clock,
     * with same time (ns) than master, and time speed of 1.
     * 
     * Uses an internal lock for clock modification listeners calls.
     * 
     * @param masterClock Master clock (not null).
     */
    public EnslavedControllableClock(InterfaceClock masterClock) {
        this(
                masterClock,
                null,
                true);
    }
    
    /**
     * Creates a modifiable clock enslaved to the specified master clock,
     * with same time (ns) than master, and time speed of 1.
     * 
     * @param masterClock Master clock (not null).
     * @param modificationLock Modification lock to use for
     *        clock modification listeners calls (not null).
     */
    public EnslavedControllableClock(
            final InterfaceClock masterClock,
            final InterfaceCheckerLock modificationLock) {
        this(
                masterClock,
                modificationLock,
                false);
    }

    /**
     * Thread-safe and non-blocking if master clock is.
     */
    @Override
    public double getTimeS() {
        return TimeUtils.nsToS(this.getTimeNs());
    }

    /**
     * Thread-safe and non-blocking if master clock is.
     */
    @Override
    public long getTimeNs() {
        return this.timeRef.get().computeSlaveTimeNs(this.masterClock.getTimeNs());
    }

    /**
     * Thread-safe and non-blocking.
     * @return Time speed (relative to master clock).
     */
    @Override
    public double getTimeSpeed() {
        return this.timeRef.get().getTimeSpeed();
    }

    @Override
    public InterfaceClock getMasterClock() {
        return this.masterClock;
    }

    @Override
    public long computeMasterTimeNs(long slaveTimeNs) {
        return this.timeRef.get().computeMasterTimeNs(slaveTimeNs);
    }

    @Override
    public boolean addListener(InterfaceClockModificationListener listener) {
        return this.listeners.addListener(listener);
    }
    
    @Override
    public boolean removeListener(InterfaceClockModificationListener listener) {
        return this.listeners.removeListener(listener);
    }

    /*
     * Setter methods are thread-safe if master clock is,
     * and non-blocking if master clock is and there are no listeners.
     */
    
    @Override
    public void setTimeNs(long timeNs) {
        if (this.listeners.isEmpty()) {
            this.setTimeNs_NO_NOTIFICATION(timeNs);
        } else {
            this.modificationLock.checkNotHeldByCurrentThread();
            this.modificationLock.lock();
            try {
                this.setTimeNs_NO_NOTIFICATION(timeNs);
                this.listeners.notifyListeners();
            } finally {
                this.modificationLock.unlock();
            }
        }
    }
    
    @Override
    public void setTimeSpeed(double timeSpeed) {
        if (this.listeners.isEmpty()) {
            final long masterTimeNs = this.masterClock.getTimeNs();
            this.setTimeSpeed_NO_NOTIFICATION(masterTimeNs, timeSpeed);
        } else {
            this.modificationLock.checkNotHeldByCurrentThread();
            this.modificationLock.lock();
            try {
                // Retrieving master time from within lock, for it to be close
                // to actual modification time.
                final long masterTimeNs = this.masterClock.getTimeNs();
                this.setTimeSpeed_NO_NOTIFICATION(masterTimeNs, timeSpeed);
                this.listeners.notifyListeners();
            } finally {
                this.modificationLock.unlock();
            }
        }
    }

    @Override
    public void setTimeNsAndTimeSpeed(long timeNs, double timeSpeed) {
        if (this.listeners.isEmpty()) {
            this.setTimeNsAndTimeSpeed_NO_NOTIFICATION(timeNs, timeSpeed);
        } else {
            this.modificationLock.checkNotHeldByCurrentThread();
            this.modificationLock.lock();
            try {
                this.setTimeNsAndTimeSpeed_NO_NOTIFICATION(timeNs, timeSpeed);
                this.listeners.notifyListeners();
            } finally {
                this.modificationLock.unlock();
            }
        }
    }

    @Override
    public void setTimeOffsetNS(long timeOffsetNS) {
        if (this.listeners.isEmpty()) {
            this.setTimeOffsetNS_NO_NOTIFICATION(timeOffsetNS);
        } else {
            this.modificationLock.checkNotHeldByCurrentThread();
            this.modificationLock.lock();
            try {
                this.setTimeOffsetNS_NO_NOTIFICATION(timeOffsetNS);
                this.listeners.notifyListeners();
            } finally {
                this.modificationLock.unlock();
            }
        }
    }

    @Override
    public void setTimeOffsetNSAndTimeSpeed(long timeOffsetNS, double timeSpeed) {
        if (this.listeners.isEmpty()) {
            this.setTimeOffsetNSAndTimeSpeed_NO_NOTIFICATION(timeOffsetNS, timeSpeed);
        } else {
            this.modificationLock.checkNotHeldByCurrentThread();
            this.modificationLock.lock();
            try {
                this.setTimeOffsetNSAndTimeSpeed_NO_NOTIFICATION(timeOffsetNS, timeSpeed);
                this.listeners.notifyListeners();
            } finally {
                this.modificationLock.unlock();
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private EnslavedControllableClock(
            final InterfaceClock masterClock,
            final InterfaceCheckerLock modificationLock,
            boolean internalLock) {
        if (internalLock) {
            LangUtils.azzert(modificationLock == null);
            this.modificationLock = new ReentrantCheckerLock(this.hashCode()+"_MODIF_LOCK");
        } else {
            if (modificationLock == null) {
                throw new NullPointerException("modification lock must not be null");
            }
            this.modificationLock = modificationLock;
        }
        
        final long masterTimeNs = masterClock.getTimeNs();
        ClockLinearTimeRef timeRef = new ClockLinearTimeRef();
        timeRef.setRefMasterTimeNs(masterTimeNs);
        timeRef.setRefTimeNs(masterTimeNs);
        timeRef.setTimeSpeed(1.0);
        this.timeRef.set(timeRef);
        
        this.masterClock = masterClock;
        
        this.listeners = new ClockListeners(this);
    }

    /*
     * time and offset
     */

    private void setTimeNs_NO_NOTIFICATION(long timeNs) {
        final long masterTimeNs = this.masterClock.getTimeNs();
        this.setRefTimesNS_NO_NOTIFICATION(masterTimeNs, timeNs);
    }

    private void setTimeOffsetNS_NO_NOTIFICATION(long timeOffsetNS) {
        final long masterTimeNs = this.masterClock.getTimeNs();
        this.setRefTimesNS_NO_NOTIFICATION(masterTimeNs, NbrsUtils.plusBounded(masterTimeNs, timeOffsetNS));
    }
    
    private void setRefTimesNS_NO_NOTIFICATION(long masterTimeNs, long timeNs) {
        ClockLinearTimeRef oldRef;
        final ClockLinearTimeRef newRef = new ClockLinearTimeRef();
        newRef.setRefMasterTimeNs(masterTimeNs);
        newRef.setRefTimeNs(timeNs);
        do {
            oldRef = this.timeRef.get();
            newRef.copyTimeSpeed(oldRef);
        } while (!this.timeRef.compareAndSet(oldRef, newRef));
    }

    /*
     * time speed
     */
    
    private void setTimeSpeed_NO_NOTIFICATION(
            long masterTimeNs,
            double timeSpeed) {
        ClockLinearTimeRef oldRef;
        final ClockLinearTimeRef newRef = new ClockLinearTimeRef();
        newRef.setTimeSpeed(timeSpeed);
        // Reference time needs to be changed,
        // for time speed must only take effect from now,
        // and not have a retroactive effect.
        newRef.setRefMasterTimeNs(masterTimeNs);
        do {
            oldRef = this.timeRef.get();
            oldRef.computeSlaveTimeExtrapolationAtMasterTimeInto(
                    masterTimeNs,
                    newRef);
        } while (!this.timeRef.compareAndSet(oldRef, newRef));
    }

    /*
     * time and offset and time speed
     */
    
    private void setTimeNsAndTimeSpeed_NO_NOTIFICATION(long timeNs, double timeSpeed) {
        final long masterTimeNs = this.masterClock.getTimeNs();
        this.setRefTimesNSAndTimeSpeed_NO_NOTIFICATION(masterTimeNs, timeNs, timeSpeed);
    }

    private void setTimeOffsetNSAndTimeSpeed_NO_NOTIFICATION(long timeOffsetNS, double timeSpeed) {
        final long masterTimeNs = this.masterClock.getTimeNs();
        this.setRefTimesNSAndTimeSpeed_NO_NOTIFICATION(masterTimeNs, NbrsUtils.plusBounded(masterTimeNs, timeOffsetNS), timeSpeed);
    }
    
    private void setRefTimesNSAndTimeSpeed_NO_NOTIFICATION(long masterTimeNs, long timeNs, double timeSpeed) {
        final ClockLinearTimeRef newRef = new ClockLinearTimeRef();
        newRef.setRefMasterTimeNs(masterTimeNs);
        newRef.setRefTimeNs(timeNs);
        newRef.setTimeSpeed(timeSpeed);
        // No need to use CAS, since we set everything.
        this.timeRef.set(newRef);
    }
}
