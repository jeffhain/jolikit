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
 * Interface to be notified of clock's modifications,
 * i.e. changes in time speed, or time jumps (which can
 * be considered as a temporary infinite time speed),
 * usually other than due to system time jumps (which
 * can be tricky to detect and measure).
 */
public interface InterfaceClockModificationListener {

    /**
     * Called _after_ the modification has been done.
     */
    public void onClockModification(InterfaceClock clock);
}
