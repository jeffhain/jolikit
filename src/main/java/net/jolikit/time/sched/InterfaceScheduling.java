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
 * Interface for the exchange zone between a schedulable and its scheduler,
 * or for intermediary treatments between them.
 * 
 * Typically owned by a scheduler.
 */
public interface InterfaceScheduling {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Methods for use by schedulable (user code).
     */
    
    /**
     * Must only be used from within the call to run() being executed just after
     * InterfaceSchedulable.setScheduling(...) has been called, else behavior
     * of these methods, and of the scheduler that provided it, is undefined.
     * 
     * Behavior is undefined if not set.
     * This allows for optimization once code is trusted.
     * 
     * @return Time, in nanoseconds, for which execution is planned.
     */
    public long getTheoreticalTimeNs();
    
    /**
     * Must only be used from within the call to run() being executed just after
     * InterfaceSchedulable.setScheduling(...) has been called, else behavior
     * of these methods, and of the scheduler that provided it, is undefined.
     * 
     * Behavior is undefined if not set.
     * This allows for optimization once code is trusted.
     * 
     * @return Time, in nanoseconds, at which execution started.
     */
    public long getActualTimeNs();
    
    /**
     * Must only be used from within the call to run() being executed just after
     * InterfaceSchedulable.setScheduling(...) has been called, else behavior
     * of these methods, and of the scheduler that provided it, is undefined.
     * 
     * If this method is not called, no new execution is planned.
     * 
     * If run() method throws, the new schedule, if any, is discarded
     * (no onCancel() call) (allows for fail-fast, and helps avoiding
     * infinite scheduling in case of trouble).
     * 
     * @param nextTheoreticalTimeNs Time, in nanoseconds, at which the
     *        schedulable must be executed again, i.e. the theoretical time
     *        to use for next schedule.
     */
    public void setNextTheoreticalTimeNs(long nextTheoreticalTimeNs);
    
    /*
     * Methods for use by intermediary treatments
     * (typically, utility classes between user code and scheduler code).
     */
    
    /**
     * @return Whether next theoretical time has been set and not yet cleared
     *         by the scheduler.
     */
    public boolean isNextTheoreticalTimeSet();

    /**
     * Clears next theoretical time information.
     * 
     * Useful to ensure non-repetition after next theoretical time setting,
     * for example in case of error.
     */
    public void clearNextTheoreticalTime();

    /**
     * Must only be called after call to run() method, and not concurrently.
     * 
     * Having this method avoids having to add intermediary instances to
     * spy on calls to setNextTheoreticalTimeNs(long), if wanting to know
     * for example if another execution has been planned.
     * 
     * Behavior is undefined if not set.
     * This allows for optimization once code is trusted.
     * 
     * @return Next theoretical time, in nanoseconds, as set by call to
     *         setNextTheoreticalTimeNs(long).
     */
    public long getNextTheoreticalTimeNs();
}
