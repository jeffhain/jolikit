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

public class BwdColorTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final int[] BAD_INT8_ARR = new int[]{
        -1, 256,
        Integer.MIN_VALUE, Integer.MAX_VALUE};

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
    
    public void test_TRANSPARENT() {
        assertEquals(0L, BwdColor.TRANSPARENT.toArgb64());
    }

    /*
     * 
     */
    
    public void test_colorByName() {
        try {
            BwdColor.colorByName().clear();
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertSame(BwdColor.colorByName(), BwdColor.colorByName());
        
        // We got that many.
        assertEquals(147, BwdColor.colorByName().size());
    }
    
    /*
     * 
     */
    
    public void test_valueOfArgb32_int() {
        final int argb32 = 0x12345678;
        final BwdColor color = BwdColor.valueOfArgb32(argb32);
        assertEquals(0x1212343456567878L, color.toArgb64());
    }
    
    public void test_valueOfArgb64_long() {
        final long argb64 = 0x123456789ABCDEF0L;
        final BwdColor color = BwdColor.valueOfArgb64(argb64);
        assertEquals(argb64, color.toArgb64());
    }
    
    /*
     * 
     */
    
    public void test_valueOfRgb8_3int() {
        for (int bad8 : BAD_INT8_ARR) {
            try {
                BwdColor.valueOfRgb8(bad8, 0, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfRgb8(0, bad8, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfRgb8(0, 0, bad8);
            } catch (IllegalArgumentException expected) {}
        }
        
        final BwdColor color = BwdColor.valueOfRgb8(0x12, 0x34, 0x56);
        assertEquals(0xFFFF, color.getAlpha16());
        assertEquals(0x1212, color.getRed16());
        assertEquals(0x3434, color.getGreen16());
        assertEquals(0x5656, color.getBlue16());
    }
    
    public void test_valueOfRgb16_3int() {
        for (int bad16 : BAD_INT16_ARR) {
            try {
                BwdColor.valueOfRgb16(bad16, 0, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfRgb16(0, bad16, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfRgb16(0, 0, bad16);
            } catch (IllegalArgumentException expected) {}
        }
        
        final BwdColor color = BwdColor.valueOfRgb16(0x1234, 0x5678, 0x9ABC);
        assertEquals(0xFFFF, color.getAlpha16());
        assertEquals(0x1234, color.getRed16());
        assertEquals(0x5678, color.getGreen16());
        assertEquals(0x9ABC, color.getBlue16());
    }
    
    public void test_valueOfRgbFp_3double() {
        for (double badFp : BAD_FP_ARR) {
            try {
                BwdColor.valueOfRgbFp(badFp, 0, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfRgbFp(0, badFp, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfRgbFp(0, 0, badFp);
            } catch (IllegalArgumentException expected) {}
        }
        
        final BwdColor color = BwdColor.valueOfRgbFp(0.25, 0.5, 0.75);
        assertEquals(0xFFFF, color.getAlpha16());
        assertEquals(0x4000, color.getRed16());
        // just above 0.5
        assertEquals(0x8000, color.getGreen16());
        // just below 0.75
        assertEquals(0xBFFF, color.getBlue16());
    }
    
    /*
     * 
     */
    
    public void test_valueOfAFpRgb8_double_3int() {
        for (double badFp : BAD_FP_ARR) {
            try {
                BwdColor.valueOfAFpRgb8(badFp, 0, 0, 0);
            } catch (IllegalArgumentException expected) {}
        }
        for (int bad8 : BAD_INT8_ARR) {
            try {
                BwdColor.valueOfAFpRgb8(0.0, bad8, 0, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfAFpRgb8(0.0, 0, bad8, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfAFpRgb8(0.0, 0, 0, bad8);
            } catch (IllegalArgumentException expected) {}
        }
        
        final BwdColor color = BwdColor.valueOfAFpRgb8(0.25, 0x12, 0x34, 0x56);
        assertEquals(0x4000, color.getAlpha16());
        assertEquals(0x1212, color.getRed16());
        assertEquals(0x3434, color.getGreen16());
        assertEquals(0x5656, color.getBlue16());
    }
    
    public void test_valueOfAFpRgb16_double_3int() {
        for (double badFp : BAD_FP_ARR) {
            try {
                BwdColor.valueOfAFpRgb16(badFp, 0, 0, 0);
            } catch (IllegalArgumentException expected) {}
        }
        for (int bad16 : BAD_INT16_ARR) {
            try {
                BwdColor.valueOfAFpRgb16(0.0, bad16, 0, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfAFpRgb16(0.0, 0, bad16, 0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfAFpRgb16(0.0, 0, 0, bad16);
            } catch (IllegalArgumentException expected) {}
        }
        
        final BwdColor color = BwdColor.valueOfAFpRgb16(0.25, 0x1234, 0x5678, 0x9ABC);
        assertEquals(0x4000, color.getAlpha16());
        assertEquals(0x1234, color.getRed16());
        assertEquals(0x5678, color.getGreen16());
        assertEquals(0x9ABC, color.getBlue16());
    }

    public void test_valueOfAFpRgbFp_4double() {
        for (double badFp : BAD_FP_ARR) {
            try {
                BwdColor.valueOfAFpRgbFp(badFp, 0.0, 0.0, 0.0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfAFpRgbFp(0.0, badFp, 0.0, 0.0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfAFpRgbFp(0.0, 0.0, badFp, 0.0);
            } catch (IllegalArgumentException expected) {}
            try {
                BwdColor.valueOfAFpRgbFp(0.0, 0.0, 0.0, badFp);
            } catch (IllegalArgumentException expected) {}
        }
        
        final BwdColor color = BwdColor.valueOfAFpRgbFp(0.25, 0.375, 0.625, 0.875);
        assertEquals(0x4000, color.getAlpha16());
        assertEquals(0x6000, color.getRed16());
        assertEquals(0x9FFF, color.getGreen16());
        assertEquals(0xDFFF, color.getBlue16());
    }

    /*
     * 
     */
    
    public void test_withAlphaFp_double() {
        for (double badFp : BAD_FP_ARR) {
            try {
                Argb64.withAlphaFp(0L, badFp);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb64(0x800056789ABCDEF0L, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).withAlphaFp(0.5));
    }

    public void test_inverted() {
        checkEqualsArgb64(0x0000FFFDFFFEFFFFL, BwdColor.valueOfArgb64(0x0000000200010000L).inverted());
        checkEqualsArgb64(0xFFFFFFFDFFFEFFFFL, BwdColor.valueOfArgb64(0xFFFF000200010000L).inverted());
        checkEqualsArgb64(0x9876FFFEFFFDFFFCL, BwdColor.valueOfArgb64(0x9876000100020003L).inverted());
    }
    
    public void test_interpolated_BwdColor_double() {
        for (double badT : BAD_FP_ARR) {
            try {
                Argb64.interpolated(0L, 0L, badT);
                fail();
            } catch (IllegalArgumentException expected) {}
        }
        
        checkEqualsArgb64(0x0000000000000000L, BwdColor.valueOfArgb64(0x0000000000000000L).interpolated(BwdColor.valueOfArgb64(0x0000000000000000L), 0.0));
        checkEqualsArgb64(0x0000000000000000L, BwdColor.valueOfArgb64(0x0000000000000000L).interpolated(BwdColor.valueOfArgb64(0x0000000000000000L), 0.5));
        checkEqualsArgb64(0x0000000000000000L, BwdColor.valueOfArgb64(0x0000000000000000L).interpolated(BwdColor.valueOfArgb64(0x0000000000000000L), 1.0));
        
        checkEqualsArgb64(0x0000000000000000L, BwdColor.valueOfArgb64(0x0000000000000000L).interpolated(BwdColor.valueOfArgb64(0xFFFFFFFFFFFFFFFFL), 0.0));
        checkEqualsArgb64(0xFFFFFFFFFFFFFFFFL, BwdColor.valueOfArgb64(0x0000000000000000L).interpolated(BwdColor.valueOfArgb64(0xFFFFFFFFFFFFFFFFL), 1.0));
        
        checkEqualsArgb64(0xFFFFFFFFFFFFFFFFL, BwdColor.valueOfArgb64(0xFFFFFFFFFFFFFFFFL).interpolated(BwdColor.valueOfArgb64(0x0000000000000000L), 0.0));
        checkEqualsArgb64(0x0000000000000000L, BwdColor.valueOfArgb64(0xFFFFFFFFFFFFFFFFL).interpolated(BwdColor.valueOfArgb64(0x0000000000000000L), 1.0));
        
        // Digits multiples of 4, for exactness.
        checkEqualsArgb64(0x44CCCC4488CCCC88L, BwdColor.valueOfArgb64(0x44CCCC4488CCCC88L).interpolated(BwdColor.valueOfArgb64(0xCC4444CCCC8888CCL), 0.0));
        checkEqualsArgb64(0x66AAAA6699BBBB99L, BwdColor.valueOfArgb64(0x44CCCC4488CCCC88L).interpolated(BwdColor.valueOfArgb64(0xCC4444CCCC8888CCL), 0.25));
        checkEqualsArgb64(0x88888888AAAAAAAAL, BwdColor.valueOfArgb64(0x44CCCC4488CCCC88L).interpolated(BwdColor.valueOfArgb64(0xCC4444CCCC8888CCL), 0.5));
        checkEqualsArgb64(0xAA6666AABB9999BBL, BwdColor.valueOfArgb64(0x44CCCC4488CCCC88L).interpolated(BwdColor.valueOfArgb64(0xCC4444CCCC8888CCL), 0.75));
        checkEqualsArgb64(0xCC4444CCCC8888CCL, BwdColor.valueOfArgb64(0x44CCCC4488CCCC88L).interpolated(BwdColor.valueOfArgb64(0xCC4444CCCC8888CCL), 1.0));
    }
    
    /*
     * 
     */
    
    public void test_toString() {
        final long argb64 = 0x123456789ABCDEF0L;
        final BwdColor color = BwdColor.valueOfArgb64(argb64);
        assertEquals(Argb64.toString(argb64), color.toString());
    }
    
    public void test_hashCode() {
        final long argb64 = 0x123456789ABCDEF0L;
        final BwdColor color = BwdColor.valueOfArgb64(argb64);
        assertEquals(color.toArgb32(), color.hashCode());
    }
    
    public void test_equals_Object() {
        final long argb64 = 0x123456789ABCDEF0L;
        final BwdColor color = BwdColor.valueOfArgb64(argb64);
        
        assertFalse(color.equals(null));
        assertFalse(color.equals(new Object()));
        
        assertFalse(color.equals(BwdColor.valueOfArgb64(argb64 + 1)));
        
        assertTrue(color.equals(BwdColor.valueOfArgb64(argb64)));
    }
    
    public void test_compareTo_BwdColor() {
        try {
            BwdColor.TRANSPARENT.compareTo(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        final long argb64 = 0x123456789ABCDEF0L;
        assertEquals(-1, BwdColor.valueOfArgb64(argb64).compareTo(BwdColor.valueOfArgb64(argb64 + 1)));
        assertEquals(0, BwdColor.valueOfArgb64(argb64).compareTo(BwdColor.valueOfArgb64(argb64)));
        assertEquals(1, BwdColor.valueOfArgb64(argb64).compareTo(BwdColor.valueOfArgb64(argb64 - 1)));
    }
    
    /*
     * 
     */
    
    public void test_toArgb32() {
        final long argb64 = 0x123456789ABCDEF0L;
        final BwdColor color = BwdColor.valueOfArgb64(argb64);
        
        assertEquals(0x12569ADE, color.toArgb32());
    }
    
    public void test_toArgb64() {
        final long argb64 = 0x123456789ABCDEF0L;
        final BwdColor color = BwdColor.valueOfArgb64(argb64);
        
        assertEquals(argb64, color.toArgb64());
    }
    
    /*
     * 
     */

    public void test_getAlpha8() {
        assertEquals(0, BwdColor.valueOfArgb64(0x0000FFFFFFFFFFFFL).getAlpha8());
        assertEquals(0x12, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getAlpha8());
        assertEquals(0xFF, BwdColor.valueOfArgb64(0xFFFF000000000000L).getAlpha8());
    }
    
    public void test_getRed8() {
        assertEquals(0, BwdColor.valueOfArgb64(0xFFFF0000FFFFFFFFL).getRed8());
        assertEquals(0x56, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getRed8());
        assertEquals(0xFF, BwdColor.valueOfArgb64(0x0000FFFF00000000L).getRed8());
    }
    
    public void test_getGreen8() {
        assertEquals(0, BwdColor.valueOfArgb64(0xFFFFFFFF0000FFFFL).getGreen8());
        assertEquals(0x9A, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getGreen8());
        assertEquals(0xFF, BwdColor.valueOfArgb64(0x00000000FFFF0000L).getGreen8());
    }
    
    public void test_getBlue8() {
        assertEquals(0, BwdColor.valueOfArgb64(0xFFFFFFFFFFFF0000L).getBlue8());
        assertEquals(0xDE, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getBlue8());
        assertEquals(0xFF, BwdColor.valueOfArgb64(0x000000000000FFFFL).getBlue8());
    }
    
    /*
     * 
     */
    
    public void test_getAlpha16() {
        assertEquals(0, BwdColor.valueOfArgb64(0x0000FFFFFFFFFFFFL).getAlpha16());
        assertEquals(0x1234, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getAlpha16());
        assertEquals(0xFFFF, BwdColor.valueOfArgb64(0xFFFF000000000000L).getAlpha16());
    }
    
    public void test_getRed16() {
        assertEquals(0, BwdColor.valueOfArgb64(0xFFFF0000FFFFFFFFL).getRed16());
        assertEquals(0x5678, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getRed16());
        assertEquals(0xFFFF, BwdColor.valueOfArgb64(0x0000FFFF00000000L).getRed16());
    }
    
    public void test_getGreen16() {
        assertEquals(0, BwdColor.valueOfArgb64(0xFFFFFFFF0000FFFFL).getGreen16());
        assertEquals(0x9ABC, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getGreen16());
        assertEquals(0xFFFF, BwdColor.valueOfArgb64(0x00000000FFFF0000L).getGreen16());
    }
    
    public void test_getBlue16() {
        assertEquals(0, BwdColor.valueOfArgb64(0xFFFFFFFFFFFF0000L).getBlue16());
        assertEquals(0xDEF0, BwdColor.valueOfArgb64(0x123456789ABCDEF0L).getBlue16());
        assertEquals(0xFFFF, BwdColor.valueOfArgb64(0x000000000000FFFFL).getBlue16());
    }
    
    /*
     * 
     */
    
    public void test_getAlphaFp() {
        assertEquals(0.0, BwdColor.valueOfArgb64(0x0000FFFFFFFFFFFFL).getAlphaFp());
        assertEquals(1.0/65535, BwdColor.valueOfArgb64(0x0001000200030004L).getAlphaFp());
        assertEquals(1.0, BwdColor.valueOfArgb64(0xFFFF000000000000L).getAlphaFp());
    }

    public void test_getRedFp() {
        assertEquals(0.0, BwdColor.valueOfArgb64(0xFFFF0000FFFFFFFFL).getRedFp());
        assertEquals(2.0/65535, BwdColor.valueOfArgb64(0x0001000200030004L).getRedFp());
        assertEquals(1.0, BwdColor.valueOfArgb64(0x0000FFFF00000000L).getRedFp());
    }
    
    public void test_getGreenFp() {
        assertEquals(0.0, BwdColor.valueOfArgb64(0xFFFFFFFF0000FFFFL).getGreenFp());
        assertEquals(3.0/65535, BwdColor.valueOfArgb64(0x0001000200030004L).getGreenFp());
        assertEquals(1.0, BwdColor.valueOfArgb64(0x00000000FFFF0000L).getGreenFp());
    }
    
    public void test_getBlueFp() {
        assertEquals(0.0, BwdColor.valueOfArgb64(0xFFFFFFFFFFFF0000L).getBlueFp());
        assertEquals(4.0/65535, BwdColor.valueOfArgb64(0x0001000200030004L).getBlueFp());
        assertEquals(1.0, BwdColor.valueOfArgb64(0x000000000000FFFFL).getBlueFp());
    }
    
    /*
     * 
     */
    
    public void test_isOpaque() {
        assertFalse(BwdColor.valueOfArgb64(0x0000FFFFFFFFFFFFL).isOpaque());
        assertFalse(BwdColor.valueOfArgb64(0x7FFF000000000000L).isOpaque());
        assertFalse(BwdColor.valueOfArgb64(0xFFFE000000000000L).isOpaque());
        //
        assertTrue(BwdColor.valueOfArgb64(0xFFFF000000000000L).isOpaque());
        assertTrue(BwdColor.valueOfArgb64(0xFFFFFFFFFFFFFFFFL).isOpaque());
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void checkEqualsArgb64(long expectedArgb64, BwdColor actualColor) {
        final long actualArgb64 = actualColor.toArgb64();
        if (actualArgb64 != expectedArgb64) {
            final String expectedStr = Argb64.toString(expectedArgb64);
            final String actualStr = Argb64.toString(actualArgb64);
            // Handier error message than decimal digits.
            assertEquals(expectedStr, actualStr);
        }
    }
}
