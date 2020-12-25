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
package net.jolikit.bwd.api.graphics;

import net.jolikit.lang.NbrsUtils;

/**
 * Utilities to deal with argb64 colors, as a primitive long.
 * 
 * I'm not aware of any actual usage of this format, but it:
 * - is isomorphic to 32 bits ARGB format,
 * - allows for less precision loss, when doing successive
 *   color transformations,
 * - helps for gray color models with more than 256 values,
 * - helps for eventual future and more precise color models,
 *   because 32 bits ARGB is not precise enough to reproduce
 *   natural colors experience, or to match accurate optical sensors
 *   sensibility,
 * - uses less memory than the four floats of JavaFX Color.
 */
public class Argb64 {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Non-color constants.
     * Must be defined before eventual usage for static colors creations.
     */
    
    private static final int INT_65535 = 65535;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param alpha16 Alpha component, in [0,65535].
     * @param red16 Red component, in [0,65535].
     * @param green16 Green component, in [0,65535].
     * @param blue16 Blue component, in [0,65535].
     * @return The corresponding 64 bits ARGB color.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static long toArgb64FromInt16(int alpha16, int red16, int green16, int blue16) {
        checkAlpha16(alpha16);
        checkRgb16(red16, green16, blue16);
        return toArgb64FromInt16_noCheck(alpha16, red16, green16, blue16);
    }

    /**
     * @param alphaFp Alpha component, in [0,1].
     * @param redFp Red component, in [0,1].
     * @param greenFp Green component, in [0,1].
     * @param blueFp Blue component, in [0,1].
     * @return The corresponding 64 bits ARGB color.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static long toArgb64FromFp(double alphaFp, double redFp, double greenFp, double blueFp) {
        checkAlphaFp(alphaFp);
        checkRgbFp(redFp, greenFp, blueFp);
        return toArgb64FromFp_noCheck(alphaFp, redFp, greenFp, blueFp);
    }

    /*
     * Floating points to/from integers.
     */

    /**
     * @param valueFp Must be in [0,1].
     * @return Corresponding value in [0,65535].
     * @throws IllegalArgumentException if valueFp is out of range.
     */
    public static int toInt16FromFp(double valueFp) {
        checkFp("valueFp", valueFp);
        return toInt16FromFp_noCheck(valueFp);
    }

    /**
     * @param value16 Must be in [0,65535].
     * @return Corresponding value in [0,1].
     * @throws IllegalArgumentException if value16 is out of range.
     */
    public static double toFpFromInt16(int value16) {
        checkInt16("value16", value16);
        return toFpFromInt16_noCheck(value16);
    }

    /*
     * 
     */

    /**
     * Actually works for any components order,
     * since makes no assumption about it
     * and just puts the hexadecimal values.
     * 
     * @param argb64 A 64 bits ARGB color.
     * @return A string of the form 0xAAAARRRRGGGGBBBB.
     */
    public static String toString(long argb64) {
        return NbrsUtils.toStringHex(argb64);
    }
    
    /*
     * 
     */

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return True if the specified color is opaque,
     *         i.e. if its alpha component is 0xFFFF,
     *         false otherwise.
     */
    public static boolean isOpaque(long argb64) {
        final int alpha16 = getAlpha16(argb64);
        return (alpha16 == 0xFFFF);
    }

    /*
     * 
     */

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The alpha component in [0,255].
     */
    public static int getAlpha8(long argb64) {
        return ((int) (argb64 >> 56)) & 0xFF;
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The red component in [0,255].
     */
    public static int getRed8(long argb64) {
        return ((int) (argb64 >> 40)) & 0xFF;
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The green component in [0,255].
     */
    public static int getGreen8(long argb64) {
        return ((int) (argb64 >> 24)) & 0xFF;
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The blue component in [0,255].
     */
    public static int getBlue8(long argb64) {
        return ((int) (argb64 >> 8)) & 0xFF;
    }
    
    /*
     * 
     */

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The alpha component in [0,65535].
     */
    public static int getAlpha16(long argb64) {
        return (int) ((argb64 >> 48) & 0xFFFF);
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The red component in [0,65535].
     */
    public static int getRed16(long argb64) {
        return (int) ((argb64 >> 32) & 0xFFFF);
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The green component in [0,65535].
     */
    public static int getGreen16(long argb64) {
        return (int) ((argb64 >> 16) & 0xFFFF);
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The blue component in [0,65535].
     */
    public static int getBlue16(long argb64) {
        return (int) (argb64 & 0xFFFF);
    }

    /*
     * 
     */

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The alpha component in [0,1].
     */
    public static double getAlphaFp(long argb64) {
        return toFpFromInt16_noCheck(getAlpha16(argb64));
    }
    
    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The red component in [0,1].
     */
    public static double getRedFp(long argb64) {
        return toFpFromInt16_noCheck(getRed16(argb64));
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The green component in [0,1].
     */
    public static double getGreenFp(long argb64) {
        return toFpFromInt16_noCheck(getGreen16(argb64));
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return The blue component in [0,1].
     */
    public static double getBlueFp(long argb64) {
        return toFpFromInt16_noCheck(getBlue16(argb64));
    }

    /*
     * Derived.
     */

    /**
     * @param argb64 A 64 bits ARGB color.
     * @return A 64 bits ARGB with same RGB as the specified color but opaque.
     */
    public static long toOpaque(long argb64) {
        return 0xFFFF000000000000L | argb64;
    }

    /**
     * @param argb64 A 64 bits ARGB color.
     * @param alpha16 Alpha component in [0,65535].
     * @return A 64 bits ARGB with same RGB as the specified color but with
     *         the specified alpha.
     * @throws IllegalArgumentException if alpha16 is out of range.
     */
    public static long withAlpha16(long argb64, int alpha16) {
        checkAlpha16(alpha16);
        final long rgb48 = (argb64 & 0xFFFFFFFFFFFFL);
        return (((long) alpha16) << 48) | rgb48;
    }
    
    /**
     * @param argb64 A 64 bits ARGB color.
     * @param alphaFp Alpha component in [0,1].
     * @return A 64 bits ARGB with same RGB as the specified color but with
     *         the specified alpha.
     * @throws IllegalArgumentException if alphaFp is out of range.
     */
    public static long withAlphaFp(long argb64, double alphaFp) {
        checkAlphaFp(alphaFp);
        final long rgb48 = (argb64 & 0xFFFFFFFFFFFFL);
        final int alpha16 = toInt16FromFp_noCheck(alphaFp);
        return (((long) alpha16) << 48) | rgb48;
    }
    
    /**
     * @param argb64 A 64 bits ARGB color.
     * @return A 64 bits ARGB with same alpha as the specified color but with
     *         each other component replaced with (65535 - cpt).
     */
    public static long inverted(long argb64) {
        final int alpha16 = getAlpha16(argb64);
        final int red16 = INT_65535 - getRed16(argb64);
        final int green16 = INT_65535 - getGreen16(argb64);
        final int blue16 = INT_65535 - getBlue16(argb64);
        return toArgb64FromInt16_noCheck(alpha16, red16, green16, blue16);
    }
    
    /**
     * @param argb64_1 First color, as a 64 bits ARGB.
     * @param argb64_2 Second color, as a 64 bits ARGB.
     * @param t Interpolation ratio, in [0,1].
     * @return A 64 bits ARGB which each component, alpha included,
     *         is interpolated as "cpt_1 * (1-t) + cpt_2 * t".
     * @throws IllegalArgumentException if t is out of range.
     */
    public static long interpolated(long argb64_1, long argb64_2, double t) {
        if (t == 0.0) {
            return argb64_1;
        }
        if (t == 1.0) {
            return argb64_2;
        }
        
        // Rejects NaN.
        if (!((t >= 0.0) && (t <= 1.0))) {
            throw new IllegalArgumentException("t [" + t + "] must be in [0,1]");
        }
        
        final double a1 = getAlphaFp(argb64_1);
        final double r1 = getRedFp(argb64_1);
        final double g1 = getGreenFp(argb64_1);
        final double b1 = getBlueFp(argb64_1);
        
        final double a2 = getAlphaFp(argb64_2);
        final double r2 = getRedFp(argb64_2);
        final double g2 = getGreenFp(argb64_2);
        final double b2 = getBlueFp(argb64_2);
        
        final double alphaFp = a1 + (a2 - a1) * t;
        final double redFp = r1 + (r2 - r1) * t;
        final double greenFp = g1 + (g2 - g1) * t;
        final double blueFp = b1 + (b2 - b1) * t;
        
        return toArgb64FromFp_noCheck(alphaFp, redFp, greenFp, blueFp);
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    static long toArgb64FromInt16_noCheck(int alpha16, int red16, int green16, int blue16) {
        long i = alpha16;
        i <<= 16;
        i |= red16;
        i <<= 16;
        i |= green16;
        i <<= 16;
        i |= blue16;
        return i;
    }

    /**
     * @param valueFp Must be in [0,1].
     */
    static int toInt16FromFp_noCheck(double valueFp) {
        return (int) (valueFp * INT_65535 + 0.5);
    }

    /**
     * @param value16 Must be in [0,65535].
     */
    static double toFpFromInt16_noCheck(int value16) {
        return value16 * (1.0 / INT_65535);
    }
    
    static void checkInt16(String name, int value16) {
        if ((value16 < 0) || (value16 > INT_65535)) {
            throw new IllegalArgumentException(name + " [" + value16 + "] must be in [0,65535]");
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private Argb64() {
    }
    
    /*
     * 
     */
    
    private static long toArgb64FromFp_noCheck(double alphaFp, double redFp, double greenFp, double blueFp) {
        return toArgb64FromInt16_noCheck(
                toInt16FromFp_noCheck(alphaFp),
                toInt16FromFp_noCheck(redFp),
                toInt16FromFp_noCheck(greenFp),
                toInt16FromFp_noCheck(blueFp));
    }
    
    /*
     * Checks.
     */
    
    private static void checkFp(String name, double valueFp) {
        // Rejects NaNs.
        if (!((valueFp >= 0.0) && (valueFp <= 1.0))) {
            throw new IllegalArgumentException(name + " [" + valueFp + "] must be in [0,1]");
        }
    }
    
    private static void checkAlpha16(int alpha16) {
        checkInt16("alpha16", alpha16);
    }

    private static void checkAlphaFp(double alphaFp) {
        checkFp("alphaFp", alphaFp);
    }

    private static void checkRgb16(int red16, int green16, int blue16) {
        checkInt16("red16", red16);
        checkInt16("green16", green16);
        checkInt16("blue16", blue16);
    }

    private static void checkRgbFp(double redFp, double greenFp, double blueFp) {
        checkFp("red", redFp);
        checkFp("green", greenFp);
        checkFp("blue", blueFp);
    }
}
