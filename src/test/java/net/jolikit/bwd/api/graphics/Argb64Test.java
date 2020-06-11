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

import junit.framework.TestCase;

public class Argb64Test extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * Because using Math.nextAfter(...) can be too tight.
     */
    private static final double FP_EPSILON = 1e-15;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final int[] BAD_INT16_ARR = new int[]{
        -1, 65536,
        Integer.MIN_VALUE, Integer.MAX_VALUE};
    
    private static final double[] BAD_FP_ARR = new double[]{
        -0.1, 1.1,
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.NaN};
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_toArgb64FromInt16_4int() {
        for (int bad16 : BAD_INT16_ARR) {
            try {
                Argb64.toArgb64FromInt16(bad16, 0, 0, 0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb64.toArgb64FromInt16(0, bad16, 0, 0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb64.toArgb64FromInt16(0, 0, bad16, 0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb64.toArgb64FromInt16(0, 0, 0, bad16);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb64(0x0000000000000000L, Argb64.toArgb64FromInt16(0x0000, 0x0000, 0x0000, 0x0000));
        checkEqualsArgb64(0xFFFF000000000000L, Argb64.toArgb64FromInt16(0xFFFF, 0x0000, 0x0000, 0x0000));
        checkEqualsArgb64(0x0000FFFF00000000L, Argb64.toArgb64FromInt16(0x0000, 0xFFFF, 0x0000, 0x0000));
        checkEqualsArgb64(0x00000000FFFF0000L, Argb64.toArgb64FromInt16(0x0000, 0x0000, 0xFFFF, 0x0000));
        checkEqualsArgb64(0x000000000000FFFFL, Argb64.toArgb64FromInt16(0x0000, 0x0000, 0x0000, 0xFFFF));
        checkEqualsArgb64(0x0001000200030004L, Argb64.toArgb64FromInt16(0x0001, 0x0002, 0x0003, 0x0004));
    }

    public void test_toArgb64FromFp_4double() {
        for (double badFp : BAD_FP_ARR) {
            try {
                Argb64.toArgb64FromFp(badFp, 0.0, 0.0, 0.0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb64.toArgb64FromFp(0.0, badFp, 0.0, 0.0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb64.toArgb64FromFp(0.0, 0.0, badFp, 0.0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb64.toArgb64FromFp(0.0, 0.0, 0.0, badFp);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb64(0x0000000000000000L, Argb64.toArgb64FromFp(0.0, 0.0, 0.0, 0.0));
        checkEqualsArgb64(0x0001000900FFFF00L, Argb64.toArgb64FromFp(1.0/65535, 9.0/65535, 255.0/65535, (255.0 * 256)/65535));
        checkEqualsArgb64(0xFFFCFFFDFFFEFFFFL, Argb64.toArgb64FromFp(65532.0/65535, 65533.0/65535, 65534.0/65535, 1.0));
    }
    
    /*
     * 
     */
    
    public void test_toInt16FromFp_double_and_toFpFromInt16_int() {
        for (double badFp : BAD_FP_ARR) {
            try {
                Argb64.toInt16FromFp(badFp);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        for (int bad16 : BAD_INT16_ARR) {
            try {
                Argb64.toFpFromInt16(bad16);
                fail();
            } catch (IllegalArgumentException expected) {}
        }

        for (int int16 = 0; int16 <= 65535; int16++) {
            final double minFp = Math.max(0.0, ((int16 - 0.5) / 65535));
            final double maxFp = Math.min(1.0, ((int16 + 0.5) / 65535));

            if (DEBUG) {
                System.out.println();
                System.out.println("int8 = " + int16);
                System.out.println("minFp = " + minFp);
                System.out.println("maxFp = " + maxFp);
            }

            assertEquals(int16, Argb64.toInt16FromFp(aBitUp(minFp)));
            assertEquals(int16, Argb64.toInt16FromFp(aBitDown(maxFp)));

            final double fp = Argb64.toFpFromInt16(int16);
            if (DEBUG) {
                System.out.println("fp = " + fp);
            }
            if (fp < minFp) {
                throw new AssertionError(fp + " < " + minFp);
            }
            if (fp > maxFp) {
                throw new AssertionError(fp + " > " + maxFp);
            }
        }
    }

    /*
     * 
     */
    
    public void test_toString_long() {
        assertEquals("0x0000000000000000", Argb64.toString(0x0000000000000000L));
        assertEquals("0x123456789ABCDEF0", Argb64.toString(0x123456789ABCDEF0L));
        assertEquals("0xFFFFFFFFFFFFFFFF", Argb64.toString(0xFFFFFFFFFFFFFFFFL));
    }
    
    /*
     * 
     */
    
    public void test_isOpaque_long() {
        assertFalse(Argb64.isOpaque(0x0000FFFFFFFFFFFFL));
        assertFalse(Argb64.isOpaque(0x7FFFFFFFFFFFFFFFL));
        assertFalse(Argb64.isOpaque(0xFFFEFFFFFFFFFFFFL));
        //
        assertTrue(Argb64.isOpaque(0xFFFF000000000000L));
        assertTrue(Argb64.isOpaque(0xFFFFFFFFFFFFFFFFL));
    }
    
    /*
     * 
     */
    
    public void test_getAlpha8_long() {
        assertEquals(0, Argb64.getAlpha8(0x0000FFFFFFFFFFFFL));
        assertEquals(0x12, Argb64.getAlpha8(0x123456789ABCDEF0L));
        assertEquals(0xFF, Argb64.getAlpha8(0xFFFF000000000000L));
    }
    
    public void test_getRed8_long() {
        assertEquals(0, Argb64.getRed8(0xFFFF0000FFFFFFFFL));
        assertEquals(0x56, Argb64.getRed8(0x123456789ABCDEF0L));
        assertEquals(0xFF, Argb64.getRed8(0x0000FFFF00000000L));
    }
    
    public void test_getGreen8_long() {
        assertEquals(0, Argb64.getGreen8(0xFFFFFFFF0000FFFFL));
        assertEquals(0x9A, Argb64.getGreen8(0x123456789ABCDEF0L));
        assertEquals(0xFF, Argb64.getGreen8(0x00000000FFFF0000L));
    }
    
    public void test_getBlue8_long() {
        assertEquals(0, Argb64.getBlue8(0xFFFFFFFFFFFF0000L));
        assertEquals(0xDE, Argb64.getBlue8(0x123456789ABCDEF0L));
        assertEquals(0xFF, Argb64.getBlue8(0x000000000000FFFFL));
    }
    
    /*
     * 
     */
    
    public void test_getAlpha16_long() {
        assertEquals(0, Argb64.getAlpha16(0x0000FFFFFFFFFFFFL));
        assertEquals(0x1234, Argb64.getAlpha16(0x123456789ABCDEF0L));
        assertEquals(0xFFFF, Argb64.getAlpha16(0xFFFF000000000000L));
    }
    
    public void test_getRed16_long() {
        assertEquals(0, Argb64.getRed16(0xFFFF0000FFFFFFFFL));
        assertEquals(0x5678, Argb64.getRed16(0x123456789ABCDEF0L));
        assertEquals(0xFFFF, Argb64.getRed16(0x0000FFFF00000000L));
    }
    
    public void test_getGreen16_long() {
        assertEquals(0, Argb64.getGreen16(0xFFFFFFFF0000FFFFL));
        assertEquals(0x9ABC, Argb64.getGreen16(0x123456789ABCDEF0L));
        assertEquals(0xFFFF, Argb64.getGreen16(0x00000000FFFF0000L));
    }
    
    public void test_getBlue16_long() {
        assertEquals(0, Argb64.getBlue16(0xFFFFFFFFFFFF0000L));
        assertEquals(0xDEF0, Argb64.getBlue16(0x123456789ABCDEF0L));
        assertEquals(0xFFFF, Argb64.getBlue16(0x000000000000FFFFL));
    }
    
    /*
     * 
     */
    
    public void test_getAlphaFp_long() {
        assertEquals(0.0, Argb64.getAlphaFp(0x0000FFFFFFFFFFFFL));
        assertEquals(1.0/65535, Argb64.getAlphaFp(0x0001000200030004L));
        assertEquals(1.0, Argb64.getAlphaFp(0xFFFF000000000000L));
    }

    public void test_getRedFp_long() {
        assertEquals(0.0, Argb64.getRedFp(0xFFFF0000FFFFFFFFL));
        assertEquals(2.0/65535, Argb64.getRedFp(0x0001000200030004L));
        assertEquals(1.0, Argb64.getRedFp(0x0000FFFF00000000L));
    }
    
    public void test_getGreenFp_long() {
        assertEquals(0.0, Argb64.getGreenFp(0xFFFFFFFF0000FFFFL));
        assertEquals(3.0/65535, Argb64.getGreenFp(0x0001000200030004L));
        assertEquals(1.0, Argb64.getGreenFp(0x00000000FFFF0000L));
    }
    
    public void test_getBlueFp_long() {
        assertEquals(0.0, Argb64.getBlueFp(0xFFFFFFFFFFFF0000L));
        assertEquals(4.0/65535, Argb64.getBlueFp(0x0001000200030004L));
        assertEquals(1.0, Argb64.getBlueFp(0x000000000000FFFFL));
    }
    
    /*
     * 
     */
    
    public void test_toOpaque_long() {
        checkEqualsArgb64(0xFFFF8123456789A1L, Argb64.toOpaque(0x00008123456789A1L));
        checkEqualsArgb64(0xFFFF8123456789A1L, Argb64.toOpaque(0xFFFF8123456789A1L));
        checkEqualsArgb64(0xFFFF8123456789A1L, Argb64.toOpaque(0x80018123456789A1L));
    }

    public void test_withAlpha16_long_int() {
        for (int bad16 : BAD_INT16_ARR) {
            try {
                Argb64.withAlpha16(0L, bad16);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb64(0x987656789ABCDEF0L, Argb64.withAlpha16(0x123456789ABCDEF0L, 0x9876));
    }

    public void test_withAlphaFp_long_double() {
        for (double badFp : BAD_FP_ARR) {
            try {
                Argb64.withAlphaFp(0L, badFp);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb64(0x800056789ABCDEF0L, Argb64.withAlphaFp(0x123456789ABCDEF0L, 0.5));
    }

    public void test_inverted_long() {
        checkEqualsArgb64(0x0000FFFDFFFEFFFFL, Argb64.inverted(0x0000000200010000L));
        checkEqualsArgb64(0xFFFFFFFDFFFEFFFFL, Argb64.inverted(0xFFFF000200010000L));
        checkEqualsArgb64(0x9876FFFEFFFDFFFCL, Argb64.inverted(0x9876000100020003L));
    }
    
    public void test_interpolated_2long_double() {
        for (double badT : BAD_FP_ARR) {
            try {
                Argb64.interpolated(0L, 0L, badT);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb64(0x0000000000000000L, Argb64.interpolated(0x0000000000000000L, 0x0000000000000000L, 0.0));
        checkEqualsArgb64(0x0000000000000000L, Argb64.interpolated(0x0000000000000000L, 0x0000000000000000L, 0.5));
        checkEqualsArgb64(0x0000000000000000L, Argb64.interpolated(0x0000000000000000L, 0x0000000000000000L, 1.0));
        
        checkEqualsArgb64(0x0000000000000000L, Argb64.interpolated(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL, 0.0));
        checkEqualsArgb64(0xFFFFFFFFFFFFFFFFL, Argb64.interpolated(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL, 1.0));
        
        checkEqualsArgb64(0xFFFFFFFFFFFFFFFFL, Argb64.interpolated(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L, 0.0));
        checkEqualsArgb64(0x0000000000000000L, Argb64.interpolated(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L, 1.0));
        
        // Digits multiples of 4, for exactness.
        checkEqualsArgb64(0x44CCCC4488CCCC88L, Argb64.interpolated(0x44CCCC4488CCCC88L, 0xCC4444CCCC8888CCL, 0.0));
        checkEqualsArgb64(0x66AAAA6699BBBB99L, Argb64.interpolated(0x44CCCC4488CCCC88L, 0xCC4444CCCC8888CCL, 0.25));
        checkEqualsArgb64(0x88888888AAAAAAAAL, Argb64.interpolated(0x44CCCC4488CCCC88L, 0xCC4444CCCC8888CCL, 0.5));
        checkEqualsArgb64(0xAA6666AABB9999BBL, Argb64.interpolated(0x44CCCC4488CCCC88L, 0xCC4444CCCC8888CCL, 0.75));
        checkEqualsArgb64(0xCC4444CCCC8888CCL, Argb64.interpolated(0x44CCCC4488CCCC88L, 0xCC4444CCCC8888CCL, 1.0));
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static double aBitDown(double valueFp) {
        return valueFp - FP_EPSILON;
    }
    
    private static double aBitUp(double valueFp) {
        return valueFp + FP_EPSILON;
    }
    
    private static void checkEqualsArgb64(long expectedArgb64, long actualArgb64) {
        if (actualArgb64 != expectedArgb64) {
            final String expectedStr = Argb64.toString(expectedArgb64);
            final String actualStr = Argb64.toString(actualArgb64);
            // Handier error message than decimal digits.
            assertEquals(expectedStr, actualStr);
        }
    }
}
