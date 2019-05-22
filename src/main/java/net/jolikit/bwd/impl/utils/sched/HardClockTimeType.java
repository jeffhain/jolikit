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
package net.jolikit.bwd.impl.utils.sched;

/**
 * Type of time provided by a hard clock.
 */
public enum HardClockTimeType {
    /**
     * clock.getTimeNs() returns System.nanoTime().
     * Pros: Very precise, and not subject to system time jumps.
     */
    SYSTEM_NANO_TIME,
    /**
     * clock.getTimeNs() returns System.nanoTime() minus an offset
     * such that it starts at zero.
     * Pros: Very precise, and not subject to system time jumps,
     *       easy to read, similar timestamps at each run.
     */
    NANO_TIME_ZERO,
    /**
     * clock.getTimeNs() returns System.currentTimeMillis() * (1000L * 1000L).
     * Pros: Aligned with system time, if you need it.
     * Cons: Not very precise: usually only 10ms resolution.
     */
    SYSTEM_CURRENT_TIME_MILLIS,
    /**
     * clock.getTimeNs() returns ThinTime.currentTimeNanos().
     * Pros: Both very precise, and aligned with system time, if you need it.
     * Cons: Slower than other time types, since uses them both internally.
     */
    THIN_TIME;
}
