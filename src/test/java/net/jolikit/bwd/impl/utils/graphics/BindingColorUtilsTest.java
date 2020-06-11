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
package net.jolikit.bwd.impl.utils.graphics;

import java.util.Random;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.lang.NumbersUtils;

public class BindingColorUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final long RANDOM_SEED = (false ? System.nanoTime() : 123456789L);
    
    private static final int NBR_OF_CALLS = 1000 * 1000;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_toAbcd32_noCheck_4int() {
        assertEquals(0x12345678, BindingColorUtils.toAbcd32_noCheck(0x12, 0x34, 0x56, 0x78));
    }
    
    public void test_toInt8FromFp_noCheck_double() {
        assertEquals(0, BindingColorUtils.toInt8FromFp_noCheck(0.0));
        //
        assertEquals(0, BindingColorUtils.toInt8FromFp_noCheck(0.4/255.0));
        assertEquals(1, BindingColorUtils.toInt8FromFp_noCheck(0.6/255.0));
        assertEquals(1, BindingColorUtils.toInt8FromFp_noCheck(1.4/255.0));
        assertEquals(2, BindingColorUtils.toInt8FromFp_noCheck(1.6/255.0));
        //
        assertEquals(127, BindingColorUtils.toInt8FromFp_noCheck(127.4/255.0));
        assertEquals(128, BindingColorUtils.toInt8FromFp_noCheck(127.6/255.0));
        //
        assertEquals(253, BindingColorUtils.toInt8FromFp_noCheck(253.4/255.0));
        assertEquals(254, BindingColorUtils.toInt8FromFp_noCheck(253.6/255.0));
        assertEquals(254, BindingColorUtils.toInt8FromFp_noCheck(254.4/255.0));
        assertEquals(255, BindingColorUtils.toInt8FromFp_noCheck(254.6/255.0));
        //
        assertEquals(255, BindingColorUtils.toInt8FromFp_noCheck(1.0));
    }

    public void test_toFpFromInt8_noCheck_int() {
        assertEquals(0.0, BindingColorUtils.toFpFromInt8_noCheck(0));
        //
        assertEquals(1.0/255.0, BindingColorUtils.toFpFromInt8_noCheck(1));
        //
        assertEquals(127.0/255.0, BindingColorUtils.toFpFromInt8_noCheck(127));
        assertEquals(128.0/255.0, BindingColorUtils.toFpFromInt8_noCheck(128));
        //
        assertEquals(254.0/255.0, BindingColorUtils.toFpFromInt8_noCheck(254));
        //
        assertEquals(1.0, BindingColorUtils.toFpFromInt8_noCheck(255));
    }
    
    public void test_withAlpha8_noCheck_2int() {
        checkEqual_color32(0x9A345678, BindingColorUtils.withAlpha8_noCheck(0x12345678, 0x9A));
    }

    public void test_toPremul8_noCheck_int_double() {
        checkEqual_color32(0x44, BindingColorUtils.toPremul8_noCheck(0x88, 0.5));
    }
    
    public void test_toNonPremul8_noCheck_int_double() {
        checkEqual_color32(0x88, BindingColorUtils.toNonPremul8_noCheck(0x44, 1.0/0.5));
    }
    
    /*
     * 
     */

    public void test_toPremulAxyz32_int() {
        checkEqual_color32(0x80224466, BindingColorUtils.toPremulAxyz32(0x804488CC));
    }

    public void test_toPremulXyza32_int() {
        checkEqual_color32(0x22446680, BindingColorUtils.toPremulXyza32(0x4488CC80));
    }
    
    /*
     * 
     */

    public void test_toNonPremulAxyz32_int() {
        // Rounding non-errors.
        checkEqual_color32(0x804487CB, BindingColorUtils.toNonPremulAxyz32(0x80224466));
    }
    
    public void test_toNonPremulXyza32_int() {
        // Rounding non-errors.
        checkEqual_color32(0x4487CB80, BindingColorUtils.toNonPremulXyza32(0x22446680));
    }
    
    /*
     * 
     */

    public void test_toInvertedPremulAxyz32_noCheck_int() {
        checkEqual_color32(0x80807F70, BindingColorUtils.toInvertedPremulAxyz32_noCheck(0x80000110));
    }

    public void test_toInvertedPremulXyza32_noCheck_int() {
        checkEqual_color32(0x807F7080, BindingColorUtils.toInvertedPremulXyza32_noCheck(0x00011080));
    }

    /*
     * 
     */

    public void test_blendPremulAxyz32_srcOver_2int_specific() {
        
        /*
         * NB: Alpha pre-multiplied components make no sense if they are larger
         * than the alpha value, so we don't bother testing colors with such components.
         */
        
        checkEqual_premulAxyz32("00_00_00_00", blendPremulAxyz32_srcOver("00_00_00_00", "00_00_00_00"));
        checkEqual_premulAxyz32("FF_01_02_03", blendPremulAxyz32_srcOver("FF_01_02_03", "FF_03_02_01"));
        checkEqual_premulAxyz32("FF_03_02_01", blendPremulAxyz32_srcOver("00_00_00_00", "FF_03_02_01"));
        checkEqual_premulAxyz32("FF_01_02_03", blendPremulAxyz32_srcOver("FF_01_02_03", "00_00_00_00"));
        
        if (BindingColorUtils.CAN_ASSUME_THAT_DST_ALPHA_8_IS_255) {
        } else {
            checkEqual_premulAxyz32("C0_00_00_30", blendPremulAxyz32_srcOver("80_00_00_00", "80_00_00_60"));
            checkEqual_premulAxyz32("B0_00_00_30", blendPremulAxyz32_srcOver("80_00_00_00", "60_00_00_60"));
        }
    }
    
    public void test_blendPremulAxyz32_srcOver_2int_random() {
        final Random random = new Random(RANDOM_SEED);
        
        for (int i = 0; i < NBR_OF_CALLS; i++) {
            final int srcAxyz32 = random.nextInt();
            final int dstAxyz32 = random.nextInt();
            final int srcPremulAxyz32 = BindingColorUtils.toPremulAxyz32(srcAxyz32);
            int dstPremulAxyz32 = BindingColorUtils.toPremulAxyz32(dstAxyz32);
            if (BindingColorUtils.CAN_ASSUME_THAT_DST_ALPHA_8_IS_255) {
                dstPremulAxyz32 |= 0xFF000000;
            }

            final int expectedPremulAxyz32 = ref_blendPremulAxyz32_srcOver(srcPremulAxyz32, dstPremulAxyz32);
            final int actualPremulAxyz32 = BindingColorUtils.blendPremulAxyz32_srcOver(srcPremulAxyz32, dstPremulAxyz32);
            if (expectedPremulAxyz32 != actualPremulAxyz32) {
                System.out.println("i = " + i);
                System.out.println("srcPremulAxyz32 =      " + toStringColor32(srcPremulAxyz32));
                System.out.println("dstPremulAxyz32 =      " + toStringColor32(dstPremulAxyz32));
                System.out.println("expectedPremulAxyz32 = " + toStringColor32(expectedPremulAxyz32));
                System.out.println("actualPremulAxyz32 =   " + toStringColor32(actualPremulAxyz32));
                throwNotEqual(expectedPremulAxyz32, actualPremulAxyz32);
            }
        }
    }

    public void test_blendPremulXyza32_srcOver_2int_random() {
        final Random random = new Random(RANDOM_SEED);
        
        for (int i = 0; i < NBR_OF_CALLS; i++) {
            final int srcXyza32 = random.nextInt();
            final int dstXyza32 = random.nextInt();
            final int srcPremulXyza32 = BindingColorUtils.toPremulXyza32(srcXyza32);
            int dstPremulXyza32 = BindingColorUtils.toPremulXyza32(dstXyza32);
            if (BindingColorUtils.CAN_ASSUME_THAT_DST_ALPHA_8_IS_255) {
                dstPremulXyza32 |= 0x000000FF;
            }

            final int expectedPremulXyza32 = toXyza32(ref_blendPremulAxyz32_srcOver(toAxyz32(srcPremulXyza32), toAxyz32(dstPremulXyza32)));
            final int actualPremulXyza32 = BindingColorUtils.blendPremulXyza32_srcOver(srcPremulXyza32, dstPremulXyza32);
            if (expectedPremulXyza32 != actualPremulXyza32) {
                System.out.println("i = " + i);
                System.out.println("srcPremulXyza32 =      " + toStringColor32(srcPremulXyza32));
                System.out.println("dstPremulXyza32 =      " + toStringColor32(dstPremulXyza32));
                System.out.println("expectedPremulXyza32 = " + toStringColor32(expectedPremulXyza32));
                System.out.println("actualPremulXyza32 =   " + toStringColor32(actualPremulXyza32));
                throwNotEqual(expectedPremulXyza32, actualPremulXyza32);
            }
        }
    }

    /*
     * 
     */
    
    public void test_INT_MULT_0_255_2int_specific() {
        // NB: We want exact result for edge cases,
        // to make sure we don't go out of bounds.
        
        for (int a = 0; a <= 255; a++) {
            if (DEBUG) {
                System.out.println("a = " + a);
            }
            assertEquals(0, BindingColorUtils.INT_MULT_0_255(a, 0));
            assertEquals(a, BindingColorUtils.INT_MULT_0_255(a, 255));
        }
    }

    public void test_INT_MULT_0_255_2int_random() {
        // Our computation seem good enough to always round
        // to the theoretical value.
        final int maxDelta = 0;
        
        final Random random = new Random(RANDOM_SEED);
        
        for (int i = 0; i < NBR_OF_CALLS; i++) {
            final int a = random.nextInt(256);
            final int b = random.nextInt(256);
            final int expected = ref_INT_MULT_0_255(a, b);
            final int actual = BindingColorUtils.INT_MULT_0_255(a, b);
            if (Math.abs(actual - expected) > maxDelta) {
                throw new AssertionError(
                        "a = " + a + ", b = " + b
                        + ",  expected = " + expected + ", actual = " + actual
                        + ", maxDelta = " + maxDelta);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int toXyza32(int axyz32) {
        final int a8 = Argb32.getAlpha8(axyz32);
        final int x8 = Argb32.getRed8(axyz32);
        final int y8 = Argb32.getGreen8(axyz32);
        final int z8 = Argb32.getBlue8(axyz32);
        return BindingColorUtils.toAbcd32_noCheck(x8, y8, z8, a8);
    }
    
    private static int toAxyz32(int xyza32) {
        final int x8 = Argb32.getAlpha8(xyza32);
        final int y8 = Argb32.getRed8(xyza32);
        final int z8 = Argb32.getGreen8(xyza32);
        final int a8 = Argb32.getBlue8(xyza32);
        return BindingColorUtils.toAbcd32_noCheck(a8, x8, y8, z8);
    }
    
    /*
     * 
     */
    
    private static int ref_blendPremulAxyz32_srcOver(int srcPremulAxyz32, int dstPremulAxyz32) {
        final double srcAlpha = Argb32.getAlphaFp(srcPremulAxyz32);
        final double srcPremulX = Argb32.getRedFp(srcPremulAxyz32);
        final double srcPremulY = Argb32.getGreenFp(srcPremulAxyz32);
        final double srcPremulZ = Argb32.getBlueFp(srcPremulAxyz32);
        
        final double dstAlpha = Argb32.getAlphaFp(dstPremulAxyz32);
        final double dstPremulX = Argb32.getRedFp(dstPremulAxyz32);
        final double dstPremulY = Argb32.getGreenFp(dstPremulAxyz32);
        final double dstPremulZ = Argb32.getBlueFp(dstPremulAxyz32);
        
        final double resAlpha = srcAlpha + (1.0 - srcAlpha) * dstAlpha;
        final double resPremulX = srcPremulX + (1.0 - srcAlpha) * dstPremulX;
        final double resPremulY = srcPremulY + (1.0 - srcAlpha) * dstPremulY;
        final double resPremulZ = srcPremulZ + (1.0 - srcAlpha) * dstPremulZ;
        
        final int resPremulAxyz32 = Argb32.toArgb32FromFp(resAlpha, resPremulX, resPremulY, resPremulZ);
        checkValidPremulAxyz32(resPremulAxyz32);
        return resPremulAxyz32;
    }
    
    private static int blendPremulAxyz32_srcOver(String srcPremulAxyz32Str, String dstPremulAxyz32Str) {
        final int resPremulAxyz32 = BindingColorUtils.blendPremulAxyz32_srcOver(
                toPremulAxyz32FromString(srcPremulAxyz32Str),
                toPremulAxyz32FromString(dstPremulAxyz32Str));
        checkValidPremulAxyz32(resPremulAxyz32);
        return resPremulAxyz32;
    }
    
    /**
     * @param premulAxyz32Str "AA_XX_YY_ZZ", like "FF_00_00_00".
     * @return axyz32Premul.
     */
    private static int toPremulAxyz32FromString(String premulAxyz32Str) {
        int premulAxyz32 = 0;
        for (int i = 0; i < 4; i++) {
            final String cptStr = premulAxyz32Str.substring(3*i, 3*(i+1) - 1);
            final int cpt = Integer.parseInt(cptStr, 16);
            premulAxyz32 |= (cpt << (8*(3-i)));
        }
        checkValidPremulAxyz32(premulAxyz32);
        return premulAxyz32;
    }
    
    /**
     * Checks that each component is <= alpha.
     */
    private static void checkValidPremulAxyz32(int premulAxyz32) {
        final int alpha8 = Argb32.getAlpha8(premulAxyz32);
        final int x8 = Argb32.getRed8(premulAxyz32);
        final int y8 = Argb32.getGreen8(premulAxyz32);
        final int z8 = Argb32.getBlue8(premulAxyz32);
        final boolean ok = (x8 <= alpha8) && (y8 <= alpha8) && (z8 <= alpha8);
        if (!ok) {
            throw new AssertionError("inconsistent alpha pre-multiplied color : " + toStringColor32(premulAxyz32));
        }
    }
    
    /*
     * 
     */
    
    private static int ref_INT_MULT_0_255(int a, int b) {
        return (int) Math.rint((a * b) / 255.0);
    }
    
    /**
     * @param cpt A value in [0,1].
     * @return A string representation that (should) always have the same length.
     */
    private static String toString_0_1(double cpt) {
        final StringBuilder sb = new StringBuilder();
        
        sb.append(NumbersUtils.toStringNoCSN(cpt));
        
        // "0." and up to 17 digits.
        final int maxSize = 2 + 17;
        while (sb.length() < maxSize) {
            sb.append("0");
        }
        
        return sb.toString();
    }
    
    private static String toStringColor32(int color32) {
        final StringBuilder sb = new StringBuilder();
        
        sb.append(Argb32.toString(color32));
        
        final double a = Argb32.getAlphaFp(color32);
        final double b = Argb32.getRedFp(color32);
        final double c = Argb32.getGreenFp(color32);
        final double d = Argb32.getBlueFp(color32);
        sb.append("(");
        sb.append(toString_0_1(a));
        sb.append(", ");
        sb.append(toString_0_1(b));
        sb.append(", ");
        sb.append(toString_0_1(c));
        sb.append(", ");
        sb.append(toString_0_1(d));
        sb.append(")");
        
        return sb.toString();
    }
    
    private static void checkEqual_premulAxyz32(String expectedPremulAxyz32Str, int actualPremulAxyz32) {
        checkEqual_color32(toPremulAxyz32FromString(expectedPremulAxyz32Str), actualPremulAxyz32);
    }
    
    /**
     * Can also be used for a single 8 bits component,
     * will just print useless zeros.
     */
    private static void checkEqual_color32(int expectedColor32, int actualColor32) {
        if (expectedColor32 != actualColor32) {
            System.out.println("expectedColor32 = " + toStringColor32(expectedColor32));
            System.out.println("actualColor32 =   " + toStringColor32(actualColor32));
            throwNotEqual(expectedColor32, actualColor32);
        }
    }
    
    private static void throwNotEqual(int expectedColor32, int actualColor32) {
        throw new AssertionError("expected = " + toStringColor32(expectedColor32) + ", actual = " + toStringColor32(actualColor32));
    }
}
