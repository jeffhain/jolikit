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

/**
 * A default implementation.
 */
public class DefaultScheduling implements InterfaceScheduling {
    
    /*
     * Can do much checks, to help catch schedulers or user code bugs.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * True, to help catch schedulers or user code bugs.
     * 
     * As an optimization, could deactivate checks,
     * and add non-megamorphic variants of methods
     * for use by schedulers code.
     */
    private static final boolean MUST_DO_CHECKS = true;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Crazy default value, in case read with
     * getNextTheoreticalTimeNs_noCheck()
     * at the wrong time.
     */
    private static final long DEFAULT_NEXT_THEORETICAL_TIME_NS = Long.MAX_VALUE;
    
    private boolean isTheoreticalTimeSet = false;
    private long theoreticalTimeNs;
    
    private boolean isActualTimeSet = false;
    private long actualTimeNs;
    
    private boolean isNextTheoreticalTimeSet = false;
    private long nextTheoreticalTimeNs = DEFAULT_NEXT_THEORETICAL_TIME_NS;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHOD
    //--------------------------------------------------------------------------
    
    public DefaultScheduling() {
    }
    
    /**
     * To be called before InterfaceSchedulable.run(), and by safety also before
     * InterfaceSchedulable.setScheduling(...), if user would ever use
     * scheduling before entering run().
     * 
     * Clears next theoretical time information.
     */
    public void configureBeforeRun(
            long theoreticalTimeNs,
            long actualTimeNs) {
        this.theoreticalTimeNs = theoreticalTimeNs;
        this.isTheoreticalTimeSet = true;
        
        this.actualTimeNs = actualTimeNs;
        this.isActualTimeSet = true;
        
        this.isNextTheoreticalTimeSet = false;
        this.nextTheoreticalTimeNs = DEFAULT_NEXT_THEORETICAL_TIME_NS;
    }

    /**
     * Usable instead of configureBeforeRun(...) if wanting to rely
     * on previously set theoretical time.
     * 
     * If theoretical time is set (case of a timed schedule),
     * doesn't modify it, else (case of an ASAP schedule),
     * sets it with actual time.
     * 
     * Clears next theoretical time information.
     */
    public void updateBeforeRun(long actualTimeNs) {
        final boolean isAsapSchedule = !this.isTheoreticalTimeSet;
        if (isAsapSchedule) {
            this.theoreticalTimeNs = actualTimeNs;
            this.isTheoreticalTimeSet = true;
        }
        
        this.actualTimeNs = actualTimeNs;
        this.isActualTimeSet = true;
        
        if (this.isNextTheoreticalTimeSet) {
            this.isNextTheoreticalTimeSet = false;
            this.nextTheoreticalTimeNs = DEFAULT_NEXT_THEORETICAL_TIME_NS;
        }
    }
    
    /*
     * 
     */
    
    public boolean isTheoreticalTimeSet() {
        return this.isTheoreticalTimeSet;
    }

    public void setTheoreticalTimeNs(long theoreticalTimeNs) {
        this.theoreticalTimeNs = theoreticalTimeNs;
        this.isTheoreticalTimeSet = true;
    }
    
    @Override
    public long getTheoreticalTimeNs() {
        if (MUST_DO_CHECKS) {
            if (!this.isTheoreticalTimeSet) {
                throw new IllegalStateException();
            }
        }
        return this.theoreticalTimeNs;
    }
    
    /*
     * 
     */
    
    public boolean isActualTimeSet() {
        return this.isActualTimeSet;
    }

    public void setActualTimeNs(long actualTimeNs) {
        this.actualTimeNs = actualTimeNs;
        this.isActualTimeSet = true;
    }
    
    @Override
    public long getActualTimeNs() {
        if (MUST_DO_CHECKS) {
            if (!this.isActualTimeSet) {
                throw new IllegalStateException();
            }
        }
        return this.actualTimeNs;
    }
    
    /*
     * 
     */
    
    @Override
    public void setNextTheoreticalTimeNs(long nextTheoreticalTimeNs) {
        this.nextTheoreticalTimeNs = nextTheoreticalTimeNs;
        this.isNextTheoreticalTimeSet = true;
    }

    @Override
    public boolean isNextTheoreticalTimeSet() {
        return this.isNextTheoreticalTimeSet;
    }
    
    @Override
    public void clearNextTheoreticalTime() {
        this.isNextTheoreticalTimeSet = false;
        this.nextTheoreticalTimeNs = DEFAULT_NEXT_THEORETICAL_TIME_NS;
    }

    @Override
    public long getNextTheoreticalTimeNs() {
        if (MUST_DO_CHECKS) {
            if (!this.isNextTheoreticalTimeSet) {
                throw new IllegalStateException();
            }
        }
        return this.nextTheoreticalTimeNs;
    }
}
