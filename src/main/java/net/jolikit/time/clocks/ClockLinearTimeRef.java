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

import net.jolikit.lang.NbrsUtils;

class ClockLinearTimeRef {

    /*
     * Computing in double or long, depending whether input or output are in
     * double or long, and preferably with double to avoid modulo arithmetic,
     * and preferably with long for accuracy.
     */
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Must be used as an immutable structure, i.e. once all fields are set,
     * and it has been given to treatments or be made visible to other threads,
     * it must not be modified.
     * This allows to have its fields not volatile nor final.
     */
    
    private long refMasterTimeNs;
    private long refTimeNs;
    
    private double timeSpeed;
    private double timeSpeedInv;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ClockLinearTimeRef() {
    }
    
    public void setRefMasterTimeNs(long refMasterTimeNs) {
        this.refMasterTimeNs = refMasterTimeNs;
    }

    public void setRefTimeNs(long refTimeNs) {
        this.refTimeNs = refTimeNs;
    }

    public void setTimeSpeed(double timeSpeed) {
        if (Double.isNaN(timeSpeed)) {
            throw new IllegalArgumentException(Double.toString(timeSpeed));
        }
        this.timeSpeed = timeSpeed;
        this.timeSpeedInv = 1.0/timeSpeed;
    }
    
    /**
     * Faster (no division).
     */
    public void copyTimeSpeed(ClockLinearTimeRef toCopy) {
        this.timeSpeed = toCopy.timeSpeed;
        this.timeSpeedInv = toCopy.timeSpeedInv;
    }

    public double getTimeSpeed() {
        return this.timeSpeed;
    }

    public long computeSlaveTimeNs(long masterTimeNs) {
        long dtNs = NbrsUtils.minusBounded(masterTimeNs, this.refMasterTimeNs);
        dtNs = ClocksUtils.computeDtNsMulTimeSpeed(dtNs, this.timeSpeed);
        return NbrsUtils.plusBounded(this.refTimeNs, dtNs);
    }
    
    public long computeMasterTimeNs(long slaveTimeNs) {
        long dtNs = NbrsUtils.minusBounded(slaveTimeNs, this.refTimeNs);
        dtNs = ClocksUtils.computeDtNsMulTimeSpeed(dtNs, this.timeSpeedInv);
        return NbrsUtils.plusBounded(this.refMasterTimeNs, dtNs);
    }
    
    /**
     * Computes the extrapolation of this time reference,
     * at the specified master time, into the specified time reference.
     * 
     * Only updates refTimeNs field:
     * refMasterTimeNs and timeSpeed must be updated aside
     * (for they might be set once and for all before multiple
     * calls to this method).
     * @param masterTimeNs Master time at which extrapolation must be done.
     * @param extrapolation (out) Time reference where extrapolated slave time
     *        must be set.
     */
    public void computeSlaveTimeExtrapolationAtMasterTimeInto(
            long masterTimeNs,
            ClockLinearTimeRef extrapolation) {
        long dtNs = NbrsUtils.minusBounded(masterTimeNs, this.refMasterTimeNs);
        dtNs = ClocksUtils.computeDtNsMulTimeSpeed(dtNs, this.timeSpeed);
        extrapolation.refTimeNs = NbrsUtils.plusBounded(this.refTimeNs, dtNs);
    }
}
