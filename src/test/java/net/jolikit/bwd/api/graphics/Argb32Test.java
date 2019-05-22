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
package net.jolikit.bwd.api.graphics;

import junit.framework.TestCase;

public class Argb32Test extends TestCase {

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

    private static final int[] BAD_INT8_ARR = new int[]{
        -1, 256,
        Integer.MIN_VALUE, Integer.MAX_VALUE};
    
    private static final double[] BAD_FP_ARR = new double[]{
        -0.1, 1.1,
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.NaN};
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_toArgb32FromInt8_4int() {
        for (int bad8 : BAD_INT8_ARR) {
            try {
                Argb32.toArgb32FromInt8(bad8, 0, 0, 0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb32.toArgb32FromInt8(0, bad8, 0, 0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb32.toArgb32FromInt8(0, 0, bad8, 0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb32.toArgb32FromInt8(0, 0, 0, bad8);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb32(0x00000000, Argb32.toArgb32FromInt8(0x00, 0x00, 0x00, 0x00));
        checkEqualsArgb32(0xFF000000, Argb32.toArgb32FromInt8(0xFF, 0x00, 0x00, 0x00));
        checkEqualsArgb32(0x00FF0000, Argb32.toArgb32FromInt8(0x00, 0xFF, 0x00, 0x00));
        checkEqualsArgb32(0x0000FF00, Argb32.toArgb32FromInt8(0x00, 0x00, 0xFF, 0x00));
        checkEqualsArgb32(0x000000FF, Argb32.toArgb32FromInt8(0x00, 0x00, 0x00, 0xFF));
        checkEqualsArgb32(0x01020304, Argb32.toArgb32FromInt8(0x01, 0x02, 0x03, 0x04));
    }

    public void test_toArgb32FromFp_4double() {
        for (double badFp : BAD_FP_ARR) {
            try {
                Argb32.toArgb32FromFp(badFp, 0.0, 0.0, 0.0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb32.toArgb32FromFp(0.0, badFp, 0.0, 0.0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb32.toArgb32FromFp(0.0, 0.0, badFp, 0.0);
                fail();
            } catch (IllegalArgumentException expected) {}
            try {
                Argb32.toArgb32FromFp(0.0, 0.0, 0.0, badFp);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb32(0x00000000, Argb32.toArgb32FromFp(0.0, 0.0, 0.0, 0.0));
        checkEqualsArgb32(0x01090FF0, Argb32.toArgb32FromFp(1.0/255, 9.0/255, 15.0/255, (15.0 * 16)/255));
        checkEqualsArgb32(0xFCFDFEFF, Argb32.toArgb32FromFp(252.0/255, 253.0/255, 254.0/255, 1.0));
    }
    
    /*
     * 
     */
    
    public void test_toInt8FromFp_double_and_toFpFromInt8_int() {
        for (double badFp : BAD_FP_ARR) {
            try {
                Argb32.toInt8FromFp(badFp);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        for (int bad8 : BAD_INT8_ARR) {
            try {
                Argb32.toFpFromInt8(bad8);
                fail();
            } catch (IllegalArgumentException expected) {}
        }

        for (int int8 = 0; int8 <= 255; int8++) {
            final double minFp = Math.max(0.0, ((int8 - 0.5) / 255));
            final double maxFp = Math.min(1.0, ((int8 + 0.5) / 255));

            if (DEBUG) {
                System.out.println();
                System.out.println("int8 = " + int8);
                System.out.println("minFp = " + minFp);
                System.out.println("maxFp = " + maxFp);
            }

            assertEquals(int8, Argb32.toInt8FromFp(aBitUp(minFp)));
            assertEquals(int8, Argb32.toInt8FromFp(aBitDown(maxFp)));

            final double fp = Argb32.toFpFromInt8(int8);
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
    
    public void test_toString_int() {
        assertEquals("0x00000000", Argb32.toString(0x00000000));
        assertEquals("0x12345678", Argb32.toString(0x12345678));
        assertEquals("0x9ABCDEF0", Argb32.toString(0x9ABCDEF0));
        assertEquals("0xFFFFFFFF", Argb32.toString(0xFFFFFFFF));
    }
    
    /*
     * 
     */
    
    public void test_isOpaque_int() {
        assertFalse(Argb32.isOpaque(0x00FFFFF));
        assertFalse(Argb32.isOpaque(0x7FFFFFF));
        assertFalse(Argb32.isOpaque(0xFEFFFFF));
        //
        assertTrue(Argb32.isOpaque(0xFF000000));
        assertTrue(Argb32.isOpaque(0xFFFFFFFF));
    }

    /*
     * 
     */
    
    public void test_getAlpha8_int() {
        assertEquals(0, Argb32.getAlpha8(0x00FFFFFF));
        assertEquals(0x12, Argb32.getAlpha8(0x12345678));
        assertEquals(0xFF, Argb32.getAlpha8(0xFF000000));
    }
    
    public void test_getRed8_int() {
        assertEquals(0, Argb32.getRed8(0xFF00FFFF));
        assertEquals(0x34, Argb32.getRed8(0x12345678));
        assertEquals(0xFF, Argb32.getRed8(0x00FF0000));
    }
    
    public void test_getGreen8_int() {
        assertEquals(0, Argb32.getGreen8(0xFFFF00FF));
        assertEquals(0x56, Argb32.getGreen8(0x12345678));
        assertEquals(0xFF, Argb32.getGreen8(0x0000FF00));
    }
    
    public void test_getBlue8_int() {
        assertEquals(0, Argb32.getBlue8(0xFFFFFF00));
        assertEquals(0x78, Argb32.getBlue8(0x12345678));
        assertEquals(0xFF, Argb32.getBlue8(0x000000FF));
    }
    
    /*
     * 
     */
    
    public void test_getAlphaFp_int() {
        assertEquals(0.0, Argb32.getAlphaFp(0x00FFFFFF));
        assertEquals(1.0/255, Argb32.getAlphaFp(0x01020304));
        assertEquals(1.0, Argb32.getAlphaFp(0xFF000000));
    }

    public void test_getRedFp_int() {
        assertEquals(0.0, Argb32.getRedFp(0xFF00FFFF));
        assertEquals(2.0/255, Argb32.getRedFp(0x01020304));
        assertEquals(1.0, Argb32.getRedFp(0x00FF0000));
    }
    
    public void test_getGreenFp_int() {
        assertEquals(0.0, Argb32.getGreenFp(0xFFFF00FF));
        assertEquals(3.0/255, Argb32.getGreenFp(0x01020304));
        assertEquals(1.0, Argb32.getGreenFp(0x0000FF00));
    }
    
    public void test_getBlueFp_int() {
        assertEquals(0.0, Argb32.getBlueFp(0xFFFFFF00));
        assertEquals(4.0/255, Argb32.getBlueFp(0x01020304));
        assertEquals(1.0, Argb32.getBlueFp(0x000000FF));
    }

    /*
     * 
     */
    
    public void test_withAlpha8_2int() {
        for (int bad8 : BAD_INT8_ARR) {
            try {
                Argb32.withAlpha8(0, bad8);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb32(0x9A345678, Argb32.withAlpha8(0x12345678, 0x9A));
    }

    public void test_withAlphaFp_int_double() {
        for (double badFp : BAD_FP_ARR) {
            try {
                Argb32.withAlphaFp(0, badFp);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb32(0x80345678, Argb32.withAlphaFp(0x12345678, 0.5));
    }

    public void test_inverted_int() {
        checkEqualsArgb32(0x00FDFEFF, Argb32.inverted(0x00020100));
        checkEqualsArgb32(0xFFFDFEFF, Argb32.inverted(0xFF020100));
        checkEqualsArgb32(0x98FEFDFC, Argb32.inverted(0x98010203));
    }
    
    public void test_interpolated_2int_double() {
        for (double badT : BAD_FP_ARR) {
            try {
                Argb32.interpolated(0, 0, badT);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb32(0x00000000, Argb32.interpolated(0x00000000, 0x00000000, 0.0));
        checkEqualsArgb32(0x00000000, Argb32.interpolated(0x00000000, 0x00000000, 0.5));
        checkEqualsArgb32(0x00000000, Argb32.interpolated(0x00000000, 0x00000000, 1.0));
        
        checkEqualsArgb32(0x00000000, Argb32.interpolated(0x00000000, 0xFFFFFFFF, 0.0));
        checkEqualsArgb32(0xFFFFFFFF, Argb32.interpolated(0x00000000, 0xFFFFFFFF, 1.0));
        
        checkEqualsArgb32(0xFFFFFFFF, Argb32.interpolated(0xFFFFFFFF, 0x00000000, 0.0));
        checkEqualsArgb32(0x00000000, Argb32.interpolated(0xFFFFFFFF, 0x00000000, 1.0));
        
        // Digits multiples of 4, for exactness.
        checkEqualsArgb32(0x4CC48CC8, Argb32.interpolated(0x4CC48CC8, 0xC44CC88C, 0.0));
        checkEqualsArgb32(0x6AA69BB9, Argb32.interpolated(0x4CC48CC8, 0xC44CC88C, 0.25));
        checkEqualsArgb32(0x8888AAAA, Argb32.interpolated(0x4CC48CC8, 0xC44CC88C, 0.5));
        checkEqualsArgb32(0xA66AB99B, Argb32.interpolated(0x4CC48CC8, 0xC44CC88C, 0.75));
        checkEqualsArgb32(0xC44CC88C, Argb32.interpolated(0x4CC48CC8, 0xC44CC88C, 1.0));
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
    
    private static void checkEqualsArgb32(int expectedArgb32, int actualArgb32) {
        if (actualArgb32 != expectedArgb32) {
            final String expectedStr = Argb32.toString(expectedArgb32);
            final String actualStr = Argb32.toString(actualArgb32);
            // Handier error message than decimal digits.
            assertEquals(expectedStr, actualStr);
        }
    }
}
