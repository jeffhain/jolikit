/*
 * Copyright 2019-2024 Jeff Hain
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
 * Utilities to deal with argb32 colors, as a primitive int.
 */
public class Argb32 {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Non-color constants.
     * Must be defined before eventual usage for static colors creations.
     */
    
    private static final int INT_255 = 255;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param alpha8 Alpha component, in [0,255].
     * @param red8 Red component, in [0,255].
     * @param green8 Green component, in [0,255].
     * @param blue8 Blue component, in [0,255].
     * @return The corresponding 32 bits ARGB color.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static int toArgb32FromInt8(int alpha8, int red8, int green8, int blue8) {
        checkRgb8(red8, green8, blue8);
        checkAlpha8(alpha8);
        return toArgb32FromInt8_noCheck(alpha8, red8, green8, blue8);
    }
    
    /**
     * @param alphaFp Alpha component, in [0,1].
     * @param redFp Red component, in [0,1].
     * @param greenFp Green component, in [0,1].
     * @param blueFp Blue component, in [0,1].
     * @return The corresponding 32 bits ARGB color.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static int toArgb32FromFp(double alphaFp, double redFp, double greenFp, double blueFp) {
        checkAlphaFp(alphaFp);
        checkRgbFp(redFp, greenFp, blueFp);
        return toArgb32FromFp_noCheck(alphaFp, redFp, greenFp, blueFp);
    }
    
    /*
     * Floating points to/from integers.
     */
    
    /**
     * @param valueFp Must be in [0,1].
     * @return Corresponding value in [0,255].
     * @throws IllegalArgumentException if valueFp is out of range.
     */
    public static int toInt8FromFp(double valueFp) {
        checkFp("valueFp", valueFp);
        return toInt8FromFp_noCheck(valueFp);
    }

    /**
     * @param value8 Must be in [0,255].
     * @return Corresponding value in [0,1].
     * @throws IllegalArgumentException if value8 is out of range.
     */
    public static double toFpFromInt8(int value8) {
        checkInt8("value8", value8);
        return toFpFromInt8_noCheck(value8);
    }

    /*
     * 
     */

    /**
     * Actually works for any components order,
     * since makes no assumption about it
     * and just puts the hexadecimal values.
     * 
     * @param argb32 A 32 bits ARGB color.
     * @return A string of the form 0xAARRGGBB.
     */
    public static String toString(int argb32) {
        return NbrsUtils.toStringHex(argb32);
    }

    /*
     * 
     */
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return True if the specified color is opaque,
     *         i.e. if its alpha component is 0xFF.
     */
    public static boolean isOpaque(int argb32) {
        final int alpha8 = getAlpha8(argb32);
        return (alpha8 == 0xFF);
    }

    /*
     * 
     */

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The alpha component in [0,255].
     */
    public static int getAlpha8(int argb32) {
        return (argb32 >>> 24);
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The red component in [0,255].
     */
    public static int getRed8(int argb32) {
        return (argb32 >> 16) & 0xFF;
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The green component in [0,255].
     */
    public static int getGreen8(int argb32) {
        return (argb32 >> 8) & 0xFF;
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The blue component in [0,255].
     */
    public static int getBlue8(int argb32) {
        return argb32 & 0xFF;
    }

    /*
     * 
     */

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The alpha component in [0,1].
     */
    public static double getAlphaFp(int argb32) {
        return toFpFromInt8_noCheck(getAlpha8(argb32));
    }
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The red component in [0,1].
     */
    public static double getRedFp(int argb32) {
        return toFpFromInt8_noCheck(getRed8(argb32));
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The green component in [0,1].
     */
    public static double getGreenFp(int argb32) {
        return toFpFromInt8_noCheck(getGreen8(argb32));
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The blue component in [0,1].
     */
    public static double getBlueFp(int argb32) {
        return toFpFromInt8_noCheck(getBlue8(argb32));
    }

    /*
     * Derived.
     */
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return A 32 bits ARGB with same RGB as the specified color but opaque.
     */
    public static int toOpaque(int argb32) {
        return 0xFF000000 | argb32;
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @param alpha8 Alpha component in [0,255].
     * @return A 32 bits ARGB with same RGB as the specified color but with
     *         the specified alpha.
     * @throws IllegalArgumentException if alpha8 is out of range.
     */
    public static int withAlpha8(int argb32, int alpha8) {
        checkAlpha8(alpha8);
        final int rgb24 = (argb32 & 0xFFFFFF);
        return (alpha8 << 24) | rgb24;
    }
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @param alphaFp Alpha component in [0,1].
     * @return A 32 bits ARGB with same RGB as the specified color but with
     *         the specified alpha.
     * @throws IllegalArgumentException if alphaFp is out of range.
     */
    public static int withAlphaFp(int argb32, double alphaFp) {
        checkAlphaFp(alphaFp);
        final int rgb24 = (argb32 & 0xFFFFFF);
        final int alpha8 = toInt8FromFp_noCheck(alphaFp);
        return (alpha8 << 24) | rgb24;
    }
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return A 32 bits ARGB with same alpha as the specified color but with
     *         each other component replaced with (255 - value).
     */
    public static int inverted(int argb32) {
        final int alpha8 = getAlpha8(argb32);
        final int red8 = INT_255 - getRed8(argb32);
        final int green8 = INT_255 - getGreen8(argb32);
        final int blue8 = INT_255 - getBlue8(argb32);
        return toArgb32FromInt8_noCheck(alpha8, red8, green8, blue8);
    }
    
    /**
     * @param argb32_1 First color, as a 32 bits ARGB.
     * @param argb32_2 Second color, as a 32 bits ARGB.
     * @param t Interpolation ratio, in [0,1].
     * @return A 32 bits ARGB which each component, alpha included,
     *         is interpolated as "cpt_1 * (1-t) + cpt_2 * t".
     * @throws IllegalArgumentException if t is out of range.
     */
    public static int interpolated(int argb32_1, int argb32_2, double t) {
        if (t == 0.0) {
            return argb32_1;
        }
        if (t == 1.0) {
            return argb32_2;
        }
        
        NbrsUtils.requireInRange(0.0, 1.0, t, "t");
        
        final double a1 = toFpFromInt8_noCheck(getAlpha8(argb32_1));
        final double r1 = toFpFromInt8_noCheck(getRed8(argb32_1));
        final double g1 = toFpFromInt8_noCheck(getGreen8(argb32_1));
        final double b1 = toFpFromInt8_noCheck(getBlue8(argb32_1));
        
        final double a2 = toFpFromInt8_noCheck(getAlpha8(argb32_2));
        final double r2 = toFpFromInt8_noCheck(getRed8(argb32_2));
        final double g2 = toFpFromInt8_noCheck(getGreen8(argb32_2));
        final double b2 = toFpFromInt8_noCheck(getBlue8(argb32_2));
        
        final double alphaFp = a1 + (a2 - a1) * t;
        final double redFp = r1 + (r2 - r1) * t;
        final double greenFp = g1 + (g2 - g1) * t;
        final double blueFp = b1 + (b2 - b1) * t;
        
        return toArgb32FromFp_noCheck(alphaFp, redFp, greenFp, blueFp);
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    static int toArgb32FromInt8_noCheck(int alpha8, int red8, int green8, int blue8) {
        int i = alpha8;
        i <<= 8;
        i |= red8;
        i <<= 8;
        i |= green8;
        i <<= 8;
        i |= blue8;
        return i;
    }
    
    /**
     * @param valueFp Must be in [0,1].
     */
    static int toInt8FromFp_noCheck(double valueFp) {
        /*
         * Using "(int) (valueFp * Math.nextDown(256.0))" would give
         * the same floating point span for each integer value,
         * instead of 0.5/255 span for 0 and 255, and 1.0/255
         * for the other 254 values.
         * But no library seem to do that, so not to surprise users
         * we stick to the usual way.
         * 
         * NB: These kinds of optimizations cause
         * toNonPremul(toPremul(np)) conversions to often not be identity,
         * even for alphas as high as 0xFE.
         * Ex:
         * - Smallest value case:
         *   np = 0x01010101 gives p = 0x01000000,
         *   so cptP8=0, alpha8=1,
         *   cpt8NpBis=(int)(0*255.0/1+0.5)=0,
         *   which causes npBis = 0x01000000.
         * - Largest value case:
         *   np = 0xFE7F7F7F gives p = 0xFE7F7F7F (same),
         *   so cptP8=0x7F=127, alpha8=0xFE=254,
         *   cpt8NpBis=(int)(127*255.0/254+0.5)=128=0x80,
         *   which causes npBis = 0xFE808080.
         */
        return (int) (valueFp * INT_255 + 0.5);
    }

    /**
     * @param value8 Must be in [0,255].
     */
    static double toFpFromInt8_noCheck(int value8) {
        return value8 * (1.0 / INT_255);
    }

    static void checkInt8(String name, int value8) {
        NbrsUtils.requireInRange(0, INT_255, value8, name);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private Argb32() {
    }

    /*
     * 
     */
    
    private static int toArgb32FromFp_noCheck(double alphaFp, double redFp, double greenFp, double blueFp) {
        return toArgb32FromInt8_noCheck(
                toInt8FromFp_noCheck(alphaFp),
                toInt8FromFp_noCheck(redFp),
                toInt8FromFp_noCheck(greenFp),
                toInt8FromFp_noCheck(blueFp));
    }
    
    /*
     * Checks.
     */
    
    private static void checkFp(String name, double valueFp) {
        NbrsUtils.requireInRange(0.0, 1.0, valueFp, name);
    }

    private static void checkAlpha8(int alpha8) {
        checkInt8("alpha8", alpha8);
    }

    private static void checkAlphaFp(double alphaFp) {
        checkFp("alphaFp", alphaFp);
    }

    private static void checkRgb8(int red8, int green8, int blue8) {
        checkInt8("red8", red8);
        checkInt8("green8", green8);
        checkInt8("blue8", blue8);
    }

    private static void checkRgbFp(double redFp, double greenFp, double blueFp) {
        checkFp("redFp", redFp);
        checkFp("greenFp", greenFp);
        checkFp("blueFp", blueFp);
    }
}
