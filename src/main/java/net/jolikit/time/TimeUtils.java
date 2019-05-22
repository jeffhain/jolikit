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
package net.jolikit.time;

/**
 * Utilities to deal with time.
 */
public class TimeUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * seconds <-> nanoseconds.
     * 
     * Since we use "millis" for milliseconds, we could consider using "nanos"
     * here, but we don't, because we like it short, for consistency with
     * our code where we use "Ns" suffix to indicate nanoseconds, and because
     * it makes it easier to distinguish them just from the size difference.
     */
    
    /**
     * @param ns Time or duration in nanoseconds.
     * @return The specified time or duration, in seconds.
     */
    public static double nsToS(long ns) {
        return ns * 1e-9;
    }

    /**
     * @param ns Time or duration in nanoseconds.
     * @return The specified time or duration, in seconds.
     */
    public static double nsToS(double ns) {
        return ns * 1e-9;
    }

    /**
     * @param s Time or duration in seconds.
     * @return The specified time or duration, in nanoseconds.
     */
    public static long sToNs(double s) {
        return nsDoubleToNsLong(s * 1e9);
    }

    /**
     * Similar to sToNs(double), but when the time or duration
     * is inferior to the nanosecond, but different from +-0.0,
     * returns 1 (if positive) or -1 (if negative) instead of 0.
     * 
     * Useful to avoid treatments becoming busy due to a too small
     * configured period.
     * 
     * @param s Time or duration in seconds.
     * @return The specified time or duration, in nanoseconds.
     */
    public static long sToNsNoUnderflow(double s) {
        long ret = sToNs(s);
        if ((ret == 0L) && (s != 0.0)) {
            // If s is NaN, we don't pass here and return 0.
            ret = ((s < 0.0) ? -1L : 1L);
        }
        return ret;
    }

    /**
     * Uses casting, not rounding.
     * 
     * @param ns Time or duration in nanoseconds, as double.
     * @return The specified time or duration, in nanoseconds, as long.
     */
    public static long nsDoubleToNsLong(double ns) {
        // Casting instead of using Math.rint (Math.round being
        // quite wrong for large values), for performance
        // (about 20 or 30 times faster).
        // 1 nanosecond of rounding error should not hurt much,
        // especially since even using rint does not provide
        // bijectivity between long and double values.
        return (long) ns;
    }
    
    /*
     * seconds <-> milliseconds.
     * 
     * Using "millis" instead of "ms", to avoid confusion with "ns",
     * especially because "ms" comes first in auto completion,
     * but should be used less (at least in our code where we prefer
     * the nanoseconds unit for accurate low level timestamps).
     */
    
    /**
     * @param millis Time or duration in milliseconds.
     * @return The specified time or duration, in seconds.
     */
    public static double millisToS(long millis) {
        return millis * 1e-3;
    }

    /**
     * @param millis Time or duration in milliseconds.
     * @return The specified time or duration, in seconds.
     */
    public static double millisToS(double millis) {
        return millis * 1e-3;
    }

    /**
     * @param s Time or duration in seconds.
     * @return The specified time or duration, in milliseconds.
     */
    public static long sToMillis(double s) {
        return millisDoubleToMillisLong(s * 1e3);
    }

    /**
     * Similar to sToMillis(double), but when the time or duration
     * is inferior to the millisecond, but different from +-0.0,
     * returns 1 (if positive) or -1 (if negative) instead of 0.
     * 
     * Useful to avoid treatments becoming busy due to a too small
     * configured period.
     * 
     * @param s Time or duration in seconds.
     * @return The specified time or duration, in milliseconds.
     */
    public static long sToMillisNoUnderflow(double s) {
        long ret = sToMillis(s);
        if ((ret == 0L) && (s != 0.0)) {
            // If s is NaN, we don't pass here and return 0.
            ret = ((s < 0.0) ? -1L : 1L);
        }
        return ret;
    }

    /**
     * Uses casting, not rounding.
     * 
     * @param millis Time or duration in milliseconds, as double.
     * @return The specified time or duration, in milliseconds, as long.
     */
    public static long millisDoubleToMillisLong(double millis) {
        /*
         * Not rounding, for consistency with nsDoubleToNsLong(double),
         * and for speed.
         */
        return (long) millis;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private TimeUtils() {
    }
}
