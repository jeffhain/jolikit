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

/**
 * A controllable clock is a clock which time and time speed
 * can be set as single values (and not functions of something else).
 * 
 * Therefore, these clocks can only be controlled in a linear way,
 * and non-linear time evolutions (like sinusoidal) can only be
 * approximated by successive linear sequences.
 * 
 * It is still possible to have a clock to compute its time and
 * time speed on each get in a non-linear way, but its listeners
 * would still have to be notified of time modifications
 * (non-linearities) in a discrete way, and could only retrieve
 * instantaneous time and time speed.
 */
public interface InterfaceControllableClock extends InterfaceListenableClock {
    
    /**
     * @param timeNs Time, in nanoseconds.
     */
    public void setTimeNs(long timeNs);

    /**
     * @param timeSpeed time speed, as defined by InterfaceClock.getTimeSpeed() method.
     */
    public void setTimeSpeed(double timeSpeed);
    
    /**
     * @param timeNs Time, in nanoseconds.
     * @param timeSpeed time speed, as defined by InterfaceClock.getTimeSpeed() method.
     */
    public void setTimeNsAndTimeSpeed(long timeNs, double timeSpeed);
}
