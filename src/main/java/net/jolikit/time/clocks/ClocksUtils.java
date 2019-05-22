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

import net.jolikit.time.TimeUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.InterfaceClockModificationListener;
import net.jolikit.time.clocks.InterfaceEnslavedClock;
import net.jolikit.time.clocks.InterfaceListenableClock;

public class ClocksUtils {

    /*
     * Not bothering with using strictfp: these methods should be
     * deterministic as long as not using pathological times
     * and time speeds.
     */
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /*
     * time speed
     */
    
    /**
     * Sample use case: timeSpeedA is the (relative) time speed of a slave clock,
     * timeSpeedB the absolute time speed of its master clock,
     * and the result the absolute time speed of the slave clock.
     * 
     * @return Time speeds product, considering 0*Infinity gives 0,
     *         (we suppose time speed is actually never infinite).
     */
    public static double computeTimeSpeedsProduct(
            double timeSpeedA,
            double timeSpeedB) {
        double result = timeSpeedA * timeSpeedB;
        // 0*Infinity gives NaN
        if (Double.isNaN(result)) {
            // If NaN is due to 0*Infinity, we return 0, with the correct sign.
            // +Infinity (most common case), done first.
            if (timeSpeedA == Double.POSITIVE_INFINITY) {
                result = timeSpeedB;
            } else if (timeSpeedB == Double.POSITIVE_INFINITY) {
                result = timeSpeedA;
            } else if (timeSpeedA == Double.NEGATIVE_INFINITY) {
                result = -timeSpeedB;
            } else if (timeSpeedB == Double.NEGATIVE_INFINITY) {
                result = -timeSpeedA;
            }
        }
        return result;
    }

    /**
     * Sample use case: timeSpeedA is be the absolute time speed of a slave clock,
     * timeSpeedB the absolute time speed of its master clock,
     * and the result is the (relative) time speed of the slave clock.
     * 
     * @return timeSpeedA over timeSpeedB, considering that (+-0.0/(whatever!=0)) gives (+-0.0 * sign(whatever)),
     *         that (whatever/+-0.0) gives +-1.0 if whatever is +-0.0 and NaN otherwise, and that
     *         (+-Infinity/+-Infinity) gives +-1.0.
     */
    public static double computeTimeSpeedsRatio(
            double timeSpeedA,
            double timeSpeedB) {
        if (timeSpeedA == 0.0) {
            if (timeSpeedB == 0.0) {
                // +-1.0
                return (1.0/(timeSpeedA*timeSpeedB) > 0.0) ? 1.0 : -1.0;
            }
            // +-0.0, or NaN
            return timeSpeedA * Math.signum(timeSpeedB);
        }
        if (Double.isInfinite(timeSpeedA) && Double.isInfinite(timeSpeedB)) {
            // +-1.0
            return Math.signum(timeSpeedA * timeSpeedB);
        }
        if (timeSpeedB == 0.0) {
            return Double.NaN;
        }
        return timeSpeedA / timeSpeedB;
    }
    
    /**
     * @return clock_time_speed/real_time_speed.
     */
    public static double computeAbsoluteTimeSpeed(InterfaceClock clock) {
        // Doing the full computation, to take care
        // of zero sign, in case it would matter.
        double timeSpeed = clock.getTimeSpeed();
        InterfaceClock tmpClock = clock;
        while (tmpClock instanceof InterfaceEnslavedClock) {
            tmpClock = ((InterfaceEnslavedClock)tmpClock).getMasterClock();
            timeSpeed = computeTimeSpeedsProduct(timeSpeed, tmpClock.getTimeSpeed());
        }
        return timeSpeed;
    }
    
    /*
     * clock time
     */

    /**
     * @param clock A clock.
     * @param timeNs A time, in nanoseconds, in the specified clock's time.
     * @return Time, in nanoseconds, of the last master clock of the specified clock,
     *         corresponding to the specified clock's time.
     */
    public static long computeRootTimeNs(
            final InterfaceClock clock,
            long timeNs) {
        long tmpTimeNs = timeNs;
        InterfaceEnslavedClock tmpEnslavedClock;
        InterfaceClock tmpClock = clock;
        while (tmpClock instanceof InterfaceEnslavedClock) {
            tmpEnslavedClock = (InterfaceEnslavedClock) tmpClock;
            tmpTimeNs = tmpEnslavedClock.computeMasterTimeNs(tmpTimeNs);
            tmpClock = tmpEnslavedClock.getMasterClock();
        }
        return tmpTimeNs;
    }
    
    /*
     * time and time speed
     */

    /**
     * If time speed is NaN, returns 0.
     * 
     * @param durationNs Duration, in nanoseconds.
     * @param timeSpeed A time speed.
     * @return Duration, in nanoseconds, corresponding to the specified duration
     *         multiplied by the specified time speed.
     */
    public static long computeDtNsMulTimeSpeed(long durationNs, double timeSpeed) {
        if (timeSpeed == 1.0) {
            // Avoiding computation precision loss.
            return durationNs;
        }
        return TimeUtils.nsDoubleToNsLong(durationNs * timeSpeed);
    }

    /**
     * If time speed is NaN, or if both parameters are 0, returns 0.
     * 
     * @param durationNs Duration, in nanoseconds.
     * @param timeSpeed A time speed.
     * @return Duration, in nanoseconds, corresponding to the specified duration
     *         divided by the specified time speed.
     */
    public static long computeDtNsDivTimeSpeed(long durationNs, double timeSpeed) {
        if (timeSpeed == 1.0) {
            // Avoiding computation precision loss.
            return durationNs;
        }
        return TimeUtils.nsDoubleToNsLong(durationNs / timeSpeed);
    }

    /**
     * Takes care of zero or infinity delta or time speed,
     * considering +-0.0 * +-Infinity gives +-0.0
     * (depending on values's signs) instead of NaN
     * (we suppose time speed is actually never infinite).
     * If any parameter is NaN, returns NaN.
     */
    public static double computeDtSMulTimeSpeed(double durationS, double timeSpeed) {
        double result = durationS * timeSpeed;
        if (Double.isNaN(result)) {
            // +-0.0 * +-Infinity, or +-Infinity * +-0.0, or any is NaN
            result = Math.signum(durationS) * Math.signum(timeSpeed);
        }
        return result;
    }

    /**
     * Takes care of zero or infinity delta or time speed,
     * considering +-0.0 / +-0.0 gives +-0.0,
     * and +-Infinity / +-Infinity gives +-Infinity
     * (depending on values's signs) instead of NaN
     * (we suppose time speed is actually never infinite).
     * If any parameter is NaN, returns NaN.
     */
    public static double computeDtSDivTimeSpeed(double durationS, double timeSpeed) {
        double result = durationS / timeSpeed;
        if (Double.isNaN(result)) {
            // +-0.0 / +-0.0, or +-Infinity / +-Infinity, or any is NaN
            result = durationS * Math.signum(timeSpeed);
        }
        return result;
    }
    
    /**
     * Computes the system duration corresponding to the specified clock duration,
     * ensuring the result is not 0 if its intermediary computed double value
     * is strictly superior to zero.
     * 
     * @param clockWaitTimeNs Clock wait time, in nanoseconds.
     * @param clock Clock defining the time reference for the specified wait time.
     * @return System wait time, in nanoseconds, corresponding to the specified clock wait time and absolute time speed.
     */
    public static long computeSystemWaitTimeNs(long clockWaitTimeNs, final InterfaceClock clock) {
        final double absoluteTimeSpeed = computeAbsoluteTimeSpeed(clock);
        return computeSystemWaitTimeNs(clockWaitTimeNs, absoluteTimeSpeed);
    }
    
    /**
     * Computes the system duration corresponding to the specified clock duration,
     * ensuring the result is not 0 if its intermediary computed double value
     * is strictly superior to zero.
     * 
     * @param clockWaitTimeNs Clock wait time, in nanoseconds.
     * @param clockAbsoluteTimeSpeed Clock absolute time speed.
     * @return System wait time, in nanoseconds, corresponding to the specified
     *         clock wait time and absolute time speed.
     */
    public static long computeSystemWaitTimeNs(long clockWaitTimeNs, double clockAbsoluteTimeSpeed) {
        long resultNs = computeDtNsDivTimeSpeed(clockWaitTimeNs, clockAbsoluteTimeSpeed);
        if ((resultNs == 0)
                && (clockWaitTimeNs != 0)
                && (Math.signum(clockWaitTimeNs) * Math.signum(clockAbsoluteTimeSpeed) > 0.0)) {
            // Making sure we wait some time if we had underflow from positive result.
            resultNs = 1;
        }
        return resultNs;
    }
    
    /*
     * listeners
     */
    
    /**
     * Adds the specified listener to all listenables clocks
     * from the specified clock to last master clock if any.
     * 
     * @param clock A clock.
     * @param listener A listener.
     * @return True if all calls to addListener methods returned true,
     *         or if there was no listenable clock, false otherwise.
     */
    public static boolean addListenerToAll(
            final InterfaceClock clock,
            final InterfaceClockModificationListener listener) {
        boolean result = true;
        InterfaceClock tmpClock = clock;
        while (true) {
            if (tmpClock instanceof InterfaceListenableClock) {
                result &= ((InterfaceListenableClock) tmpClock).addListener(listener);
            }
            if (tmpClock instanceof InterfaceEnslavedClock) {
                tmpClock = ((InterfaceEnslavedClock) tmpClock).getMasterClock();
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * @param clock A clock.
     * @param listener A listener.
     * @return True if all calls to removeListener methods returned true,
     *         or if there was no listenable clock, false otherwise.
     */
    public static boolean removeListenerFromAll(
            final InterfaceClock clock,
            final InterfaceClockModificationListener listener) {
        boolean result = true;
        InterfaceClock tmpClock = clock;
        while (true) {
            if (tmpClock instanceof InterfaceListenableClock) {
                result &= ((InterfaceListenableClock) tmpClock).removeListener(listener);
            }
            if (tmpClock instanceof InterfaceEnslavedClock) {
                tmpClock = ((InterfaceEnslavedClock) tmpClock).getMasterClock();
            } else {
                break;
            }
        }
        return result;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private ClocksUtils() {
    }
}
