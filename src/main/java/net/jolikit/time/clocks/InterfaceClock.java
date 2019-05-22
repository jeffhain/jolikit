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
 * This interface provides time in seconds and nanoseconds,
 * in case one would like accuracy or even exactness
 * with one type or the other.
 * 
 * Time in nanoseconds typically has a technical utility (scheduling related to
 * system time, etc.), and time in seconds a functional utility (computations
 * with physical quantities, etc.).
 * 
 * Implementations might require time in seconds to be in the range
 * of time in nanoseconds, or not (in which case only time in seconds
 * should be used).
 * 
 * It's common to have a "paused" state defined for clocks, stating:
 * - either that clock's time is neither flowing with system time,
 *   nor flowing in some discrete and deliberate way (clock "stopped"),
 * - or that clock's time is only flowing in a discrete and deliberate way.
 * Though, this interface defines no such state for clocks, because:
 * - the first meaning makes it mostly (*) redundant with a time speed of zero,
 *   and makes it impossible (on its own) to figure out whether clock's time
 *   is flowing with system time, or flowing in a discrete and deliberate way,
 *   (*) (when the clock is paused, it could make its
 *       time-speed-when-it-is-not-paused available, but we consider it to be
 *       useless, and even to be dangerous, since it would invite to make
 *       presumptions about clock's time-speed-when-it-(will-be/was)-unpaused)
 * - the second meaning typically refers to a constant property of the clock,
 *   which should be defined through typing rather than with a method providing
 *   a boolean value that would never change.
 * 
 * This interface is intended to be extended by two interfaces:
 * - one for "hard clocks", which time is changed by hardware, and flows with
 *   system time (not using the term real time, since system time can jump
 *   around real time).
 * - one for "soft clocks", which time is changed by software, only
 *   deliberately, and flows at a rate not necessarily directly related to a
 *   system time.
 * Treatments that want to discriminate between the two types of clocks
 * can use the extending interfaces, others can just use this interface.
 * 
 * Clocks use-cases (for time speed >= 0, not considering possible
 * backward-time usages):
 * (a) - (hard clock, ts = 0)             ===> time does not flow ("pause")
 * (b) - (hard clock, 0 < ts < +Infinity) ===> time flows with system time
 * (c) - (hard clock, ts = +Infinity)     ===> pointless (time reaches bound instantly), unless just for an instant.
 * (d) - (soft clock, ts = 0)             ===> time not to change ("pause")
 * (e) - (soft clock, 0 < ts < +Infinity) ===> time to change only deliberately
 * (f) - (soft clock, ts = +Infinity)     ===> time to change only deliberately, as fast as possible
 * Meanwhile, in whatever case, it's still possible for time and time speed to
 * jump around, according to time configuration, synchronization, system time
 * jumps (for hard clocks), or other reset procedures.
 * 
 * Naming conventions:
 * - time modification : by software, or by system time jumps (for hard clocks
 *   only),
 * - time change : by real time flowing.
 */
public interface InterfaceClock {

    /**
     * @return The current time, in nanoseconds.
     */
    public long getTimeNs();

    /**
     * Convenience method.
     * 
     * @return The current time, in seconds.
     */
    public double getTimeS();

    /**
     * If you don't know the time speed, you can either throw UnsupportedOperationException
     * or return 1.0, depending on whether you prefer fail-fast, or
     * best effort/best-guess/sweep-problems-under-the-carpet.
     * 
     * @return The time speed, as clock_time_speed/master_time_speed,
     *         master time being time of a master clock (if any)
     *         or real time ("wall clock time").
     */
    public double getTimeSpeed();
}
