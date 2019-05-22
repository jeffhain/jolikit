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
package net.jolikit.time.clocks;

public interface InterfaceEnslavedClock extends InterfaceClock {

    /**
     * Master clock (using master/slave terminology rather than parent/child,
     * for this clock's time, when not modified "manually", is totally
     * determined by its _master_ clock, and has no freedom as can be
     * provided to a child by its parents).
     * 
     * @return Clock which this clock's time speed is relative to (not null).
     */
    public InterfaceClock getMasterClock();
    
    /**
     * Must throw UnsupportedOperationException if can't be implemented.
     * 
     * @return Master's time, in nanoseconds, for the specified slave time, in nanoseconds.
     * @throws UnsupportedOperationException if not implemented.
     */
    public long computeMasterTimeNs(long slaveTimeNs);
}
