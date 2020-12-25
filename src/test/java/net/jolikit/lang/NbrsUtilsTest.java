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
package net.jolikit.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.jolikit.test.utils.NbrsTestUtils;
import net.jolikit.test.utils.TestUtils;
import junit.framework.TestCase;

public class NbrsUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final int NBR_OF_VALUES_BIG = 1000 * 1000;

    private static final int NBR_OF_VALUES_SMALL = 10 * 1000;
    
    private static final double ACCURATE_PI_OP_SIN_EPSILON = 1e-10;
    private static final double ACCURATE_PI_OP_DEFAULT_EPSILON = 3e-15;

    private static final int[] PADDING_UP_TO_ARR = new int[]{
        Integer.MIN_VALUE,
        Integer.MIN_VALUE/2,
        -1,
        0,
        1,
        2,
        11,
        23,
        // Can be large.
        67
    };
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final List<Integer> EVEN_INT_VALUES;
    static {
        ArrayList<Integer> values = new ArrayList<Integer>();
        values.add(0);
        values.add(-2);
        values.add(-4);
        values.add(-6);
        values.add(2);
        values.add(4);
        values.add(6);
        values.add(Integer.MIN_VALUE);
        values.add(Integer.MIN_VALUE+2);
        values.add(Integer.MIN_VALUE+4);
        values.add(Integer.MAX_VALUE-1);
        values.add(Integer.MAX_VALUE-3);
        values.add(Integer.MAX_VALUE-5);
        EVEN_INT_VALUES = Collections.unmodifiableList(values);
    }

    private static final List<Long> EVEN_LONG_VALUES;
    static {
        ArrayList<Long> values = new ArrayList<Long>();
        values.add(0L);
        values.add(-4L);
        values.add(-6L);
        values.add(2L);
        values.add(4L);
        values.add(6L);
        values.add(Long.MIN_VALUE);
        values.add(Long.MIN_VALUE+2);
        values.add(Long.MIN_VALUE+4);
        values.add(Long.MAX_VALUE-1);
        values.add(Long.MAX_VALUE-3);
        values.add(Long.MAX_VALUE-5);
        EVEN_LONG_VALUES = Collections.unmodifiableList(values);
    }

    private static final List<Integer> ODD_INT_VALUES;
    static {
        ArrayList<Integer> values = new ArrayList<Integer>();
        values.add(-1);
        values.add(-3);
        values.add(-5);
        values.add(1);
        values.add(3);
        values.add(5);
        values.add(Integer.MIN_VALUE+1);
        values.add(Integer.MIN_VALUE+3);
        values.add(Integer.MIN_VALUE+5);
        values.add(Integer.MAX_VALUE);
        values.add(Integer.MAX_VALUE-2);
        values.add(Integer.MAX_VALUE-4);
        ODD_INT_VALUES = Collections.unmodifiableList(values);
    }

    private static final List<Long> ODD_LONG_VALUES;
    static {
        ArrayList<Long> values = new ArrayList<Long>();
        values.add(-1L);
        values.add(-3L);
        values.add(-5L);
        values.add(1L);
        values.add(3L);
        values.add(5L);
        values.add(Long.MIN_VALUE+1);
        values.add(Long.MIN_VALUE+3);
        values.add(Long.MIN_VALUE+5);
        values.add(Long.MAX_VALUE);
        values.add(Long.MAX_VALUE-2);
        values.add(Long.MAX_VALUE-4);
        ODD_LONG_VALUES = Collections.unmodifiableList(values);
    }

    private final Random random = TestUtils.newRandom123456789L();
    
    private final NbrsTestUtils utils = new NbrsTestUtils(this.random);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_equal_2float() {
        assertTrue(NbrsUtils.equal(-0.0f, +0.0f));
        
        assertTrue(NbrsUtils.equal(1.1f, 1.1f));
        assertFalse(NbrsUtils.equal(1.1f, -1.1f));
        assertFalse(NbrsUtils.equal(1.1f, 1.2f));
        
        assertTrue(NbrsUtils.equal(Float.NaN, Float.NaN));
        assertFalse(NbrsUtils.equal(Float.NaN, 1.1f));
        assertFalse(NbrsUtils.equal(1.1f, Float.NaN));
    }
    
    public void test_equal_2double() {
        assertTrue(NbrsUtils.equal(-0.0, +0.0));
        
        assertTrue(NbrsUtils.equal(1.1, 1.1));
        assertFalse(NbrsUtils.equal(1.1, -1.1));
        assertFalse(NbrsUtils.equal(1.1, 1.2));
        
        assertTrue(NbrsUtils.equal(Double.NaN, Double.NaN));
        assertFalse(NbrsUtils.equal(Double.NaN, 1.1));
        assertFalse(NbrsUtils.equal(1.1, Double.NaN));
    }
    
    public void test_isMathematicalInteger_float() {
        assertFalse(NbrsUtils.isMathematicalInteger(Float.NaN));
        assertFalse(NbrsUtils.isMathematicalInteger(Float.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isMathematicalInteger(Float.POSITIVE_INFINITY));
        
        for (float matInt : new float[]{-2,-1,0,1,2,(1<<23)-2}) {
            float matIntMUlp = Float.intBitsToFloat(Float.floatToRawIntBits(matInt)-1);
            float matIntPUlp = Float.intBitsToFloat(Float.floatToRawIntBits(matInt)+1);
            assertFalse(NbrsUtils.isMathematicalInteger(matIntMUlp));
            assertTrue(NbrsUtils.isMathematicalInteger(matInt));
            assertFalse(NbrsUtils.isMathematicalInteger(matIntPUlp));
        }

        for (int i = 0; i < NBR_OF_VALUES_BIG; i++) {
            int matInt = this.utils.randomIntUniMag();
            float value = (float)matInt;
            assertTrue(NbrsUtils.isMathematicalInteger(value));
            if (Math.abs(value) <= (1<<23)-2) {
                // If adding or removing an ulp,
                // must no longer be a mathematical integer.
                int pm1 = -1 + 2 * this.random.nextInt(2);
                value = Float.intBitsToFloat(Float.floatToRawIntBits(value)+pm1);
                assertFalse(NbrsUtils.isMathematicalInteger(value));
            }
        }
    }

    public void test_isMathematicalInteger_double() {
        assertFalse(NbrsUtils.isMathematicalInteger(Double.NaN));
        assertFalse(NbrsUtils.isMathematicalInteger(Double.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isMathematicalInteger(Double.POSITIVE_INFINITY));
        
        for (double matInt : new double[]{-2,-1,0,1,2,(1L<<52)-2}) {
            double matIntMUlp = Double.longBitsToDouble(Double.doubleToRawLongBits(matInt)-1);
            double matIntPUlp = Double.longBitsToDouble(Double.doubleToRawLongBits(matInt)+1);
            assertFalse(NbrsUtils.isMathematicalInteger(matIntMUlp));
            assertTrue(NbrsUtils.isMathematicalInteger(matInt));
            assertFalse(NbrsUtils.isMathematicalInteger(matIntPUlp));
        }

        for (int i = 0; i < NBR_OF_VALUES_BIG; i++) {
            long matInt = this.utils.randomLongUniMag();
            double value = (double)matInt;
            assertTrue(NbrsUtils.isMathematicalInteger(value));
            if (Math.abs(value) <= (1L<<52)-2) {
                // If adding or removing an ulp,
                // must no longer be a mathematical integer.
                int pm1 = -1 + 2 * this.random.nextInt(2);
                value = Double.longBitsToDouble(Double.doubleToRawLongBits(value)+pm1);
                assertFalse(NbrsUtils.isMathematicalInteger(value));
            }
        }
    }

    public void test_isEquidistant_float() {
        assertFalse(NbrsUtils.isEquidistant(Float.NaN));
        assertFalse(NbrsUtils.isEquidistant(Float.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isEquidistant(Float.POSITIVE_INFINITY));

        for (float equi : new float[]{-2.5f,-1.5f,-0.5f,0.5f,1.5f,2.5f,(1<<23)-0.5f,(1<<23)-1.5f}) {
            float equiMUlp = Float.intBitsToFloat(Float.floatToRawIntBits(equi)-1);
            float equiPUlp = Float.intBitsToFloat(Float.floatToRawIntBits(equi)+1);
            assertFalse(NbrsUtils.isEquidistant(equiMUlp));
            assertTrue(NbrsUtils.isEquidistant(equi));
            assertFalse(NbrsUtils.isEquidistant(equiPUlp));
        }

        for (int i = 0; i < NBR_OF_VALUES_BIG; i++) {
            int matInt = this.utils.randomIntUniMag();
            float value = (float)matInt;
            // Not equidistant since mathematical integer.
            assertFalse(NbrsUtils.isEquidistant(value));
            if (Math.abs(value) <= (1<<23)) {
                // Going half a unit towards zero (unless we start from 0).
                value = value + ((value > 0) ? -0.5f : 0.5f);
                // Now must be equidistant.
                assertTrue(NbrsUtils.isEquidistant(value));
                // If adding or removing an ulp,
                // must no longer be equidistant.
                int pm1 = -1 + 2 * this.random.nextInt(2);
                value = Float.intBitsToFloat(Float.floatToRawIntBits(value)+pm1);
                assertFalse(NbrsUtils.isEquidistant(value));
            }
        }
    }

    public void test_isEquidistant_double() {
        assertFalse(NbrsUtils.isEquidistant(Double.NaN));
        assertFalse(NbrsUtils.isEquidistant(Double.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isEquidistant(Double.POSITIVE_INFINITY));
        
        for (double equi : new double[]{-2.5,-1.5,-0.5,0.5,1.5,2.5,(1L<<52)-0.5,(1L<<52)-1.5}) {
            double equiMUlp = Double.longBitsToDouble(Double.doubleToRawLongBits(equi)-1);
            double equiPUlp = Double.longBitsToDouble(Double.doubleToRawLongBits(equi)+1);
            assertFalse(NbrsUtils.isEquidistant(equiMUlp));
            assertTrue(NbrsUtils.isEquidistant(equi));
            assertFalse(NbrsUtils.isEquidistant(equiPUlp));
        }

        for (int i = 0; i < NBR_OF_VALUES_BIG; i++) {
            long matInt = this.utils.randomLongUniMag();
            double value = (double)matInt;
            // Not equidistant since mathematical integer.
            assertFalse(NbrsUtils.isEquidistant(value));
            if (Math.abs(value) <= (1L<<52)) {
                // Going half a unit towards zero (unless we start from 0).
                value = value + ((value > 0) ? -0.5 : 0.5);
                // Now must be equidistant.
                assertTrue(NbrsUtils.isEquidistant(value));
                // If adding or removing an ulp,
                // must no longer be equidistant.
                int pm1 = -1 + 2 * this.random.nextInt(2);
                value = Double.longBitsToDouble(Double.doubleToRawLongBits(value)+pm1);
                assertFalse(NbrsUtils.isEquidistant(value));
            }
        }
    }

    public void test_isNaNOrInfinite_float() {
        assertTrue(NbrsUtils.isNaNOrInfinite(Float.NaN));
        assertTrue(NbrsUtils.isNaNOrInfinite(Float.NEGATIVE_INFINITY));
        assertTrue(NbrsUtils.isNaNOrInfinite(Float.POSITIVE_INFINITY));
        
        assertFalse(NbrsUtils.isNaNOrInfinite(Float.MIN_VALUE));
        assertFalse(NbrsUtils.isNaNOrInfinite(NbrsUtils.FLOAT_MIN_NORMAL));
        assertFalse(NbrsUtils.isNaNOrInfinite(Float.MAX_VALUE));
        assertFalse(NbrsUtils.isNaNOrInfinite(-1.1f));
        assertFalse(NbrsUtils.isNaNOrInfinite(-1.0f));
        assertFalse(NbrsUtils.isNaNOrInfinite(-0.1f));
        assertFalse(NbrsUtils.isNaNOrInfinite(-0.0f));
        assertFalse(NbrsUtils.isNaNOrInfinite(0.0f));
        assertFalse(NbrsUtils.isNaNOrInfinite(0.1f));
        assertFalse(NbrsUtils.isNaNOrInfinite(1.0f));
        assertFalse(NbrsUtils.isNaNOrInfinite(1.1f));
    }
    
    public void test_isNaNOrInfinite_double() {
        assertTrue(NbrsUtils.isNaNOrInfinite(Double.NaN));
        assertTrue(NbrsUtils.isNaNOrInfinite(Double.NEGATIVE_INFINITY));
        assertTrue(NbrsUtils.isNaNOrInfinite(Double.POSITIVE_INFINITY));
        
        assertFalse(NbrsUtils.isNaNOrInfinite(Double.MIN_VALUE));
        assertFalse(NbrsUtils.isNaNOrInfinite(NbrsUtils.DOUBLE_MIN_NORMAL));
        assertFalse(NbrsUtils.isNaNOrInfinite(Double.MAX_VALUE));
        assertFalse(NbrsUtils.isNaNOrInfinite(-1.1));
        assertFalse(NbrsUtils.isNaNOrInfinite(-1.0));
        assertFalse(NbrsUtils.isNaNOrInfinite(-0.1));
        assertFalse(NbrsUtils.isNaNOrInfinite(-0.0));
        assertFalse(NbrsUtils.isNaNOrInfinite(0.0));
        assertFalse(NbrsUtils.isNaNOrInfinite(0.1));
        assertFalse(NbrsUtils.isNaNOrInfinite(1.0));
        assertFalse(NbrsUtils.isNaNOrInfinite(1.1));
    }

    public void test_signFromBit_float() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            float value = this.utils.randomFloatWhatever();
            int ref = (Float.floatToRawIntBits(value) < 0 ? -1 : 1);
            int res = NbrsUtils.signFromBit(value);
            boolean ok = (ref == res);
            assertTrue(ok);
        }
    }

    public void test_signFromBit_double() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            double value = this.utils.randomDoubleWhatever();
            long ref = (Double.doubleToRawLongBits(value) < 0 ? -1L : 1L);
            long res = NbrsUtils.signFromBit(value);
            boolean ok = (ref == res);
            assertTrue(ok);
        }
    }
    
    /*
     * 
     */
    
    public void test_isInRange_3int() {
        assertTrue(NbrsUtils.isInRange(3, 7, 3));
        assertTrue(NbrsUtils.isInRange(3, 7, 5));
        assertTrue(NbrsUtils.isInRange(3, 7, 7));
        
        assertFalse(NbrsUtils.isInRange(3, 7, Integer.MIN_VALUE));
        assertFalse(NbrsUtils.isInRange(3, 7, 2));
        assertFalse(NbrsUtils.isInRange(3, 7, 8));
        assertFalse(NbrsUtils.isInRange(3, 7, Integer.MAX_VALUE));
        
        assertFalse(NbrsUtils.isInRange(7, 3, Integer.MIN_VALUE));
        assertFalse(NbrsUtils.isInRange(7, 3, 2));
        assertFalse(NbrsUtils.isInRange(7, 3, 3));
        assertFalse(NbrsUtils.isInRange(7, 3, 5));
        assertFalse(NbrsUtils.isInRange(7, 3, 7));
        assertFalse(NbrsUtils.isInRange(7, 3, 8));
        assertFalse(NbrsUtils.isInRange(7, 3, Integer.MAX_VALUE));
    }

    public void test_isInRange_3long() {
        assertTrue(NbrsUtils.isInRange(3L, 7L, 3L));
        assertTrue(NbrsUtils.isInRange(3L, 7L, 5L));
        assertTrue(NbrsUtils.isInRange(3L, 7L, 7L));
        
        assertFalse(NbrsUtils.isInRange(3L, 7L, Long.MIN_VALUE));
        assertFalse(NbrsUtils.isInRange(3L, 7L, 2L));
        assertFalse(NbrsUtils.isInRange(3L, 7L, 8L));
        assertFalse(NbrsUtils.isInRange(3L, 7L, Long.MAX_VALUE));
        
        assertFalse(NbrsUtils.isInRange(7L, 3L, Long.MIN_VALUE));
        assertFalse(NbrsUtils.isInRange(7L, 3L, 2L));
        assertFalse(NbrsUtils.isInRange(7L, 3L, 3L));
        assertFalse(NbrsUtils.isInRange(7L, 3L, 5L));
        assertFalse(NbrsUtils.isInRange(7L, 3L, 7L));
        assertFalse(NbrsUtils.isInRange(7L, 3L, 8L));
        assertFalse(NbrsUtils.isInRange(7L, 3L, Long.MAX_VALUE));
    }

    public void test_isInRange_3float() {
        assertTrue(NbrsUtils.isInRange(3.f, 7.f, 3.f));
        assertTrue(NbrsUtils.isInRange(3.f, 7.f, 5.f));
        assertTrue(NbrsUtils.isInRange(3.f, 7.f, 7.f));
        
        assertFalse(NbrsUtils.isInRange(3.f, 7.f, Float.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isInRange(3.f, 7.f, 2.f));
        assertFalse(NbrsUtils.isInRange(3.f, 7.f, 8.f));
        assertFalse(NbrsUtils.isInRange(3.f, 7.f, Float.POSITIVE_INFINITY));
        
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, Float.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, 2.f));
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, 3.f));
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, 5.f));
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, 7.f));
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, 8.f));
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, Float.POSITIVE_INFINITY));
        
        assertFalse(NbrsUtils.isInRange(7.f, 3.f, Float.NaN));
        assertFalse(NbrsUtils.isInRange(Float.NaN, 7.f, 5.f));
        assertFalse(NbrsUtils.isInRange(3.f, Float.NaN, 5.f));
        assertFalse(NbrsUtils.isInRange(3.f, 7.f, Float.NaN));
        
        assertFalse(NbrsUtils.isInRange(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN));
    }

    public void test_isInRange_3double() {
        assertTrue(NbrsUtils.isInRange(3., 7., 3.));
        assertTrue(NbrsUtils.isInRange(3., 7., 5.));
        assertTrue(NbrsUtils.isInRange(3., 7., 7.));
        
        assertFalse(NbrsUtils.isInRange(3., 7., Double.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isInRange(3., 7., 2.));
        assertFalse(NbrsUtils.isInRange(3., 7., 8.));
        assertFalse(NbrsUtils.isInRange(3., 7., Double.POSITIVE_INFINITY));
        
        assertFalse(NbrsUtils.isInRange(7., 3., Double.NEGATIVE_INFINITY));
        assertFalse(NbrsUtils.isInRange(7., 3., 2.));
        assertFalse(NbrsUtils.isInRange(7., 3., 3.));
        assertFalse(NbrsUtils.isInRange(7., 3., 5.));
        assertFalse(NbrsUtils.isInRange(7., 3., 7.));
        assertFalse(NbrsUtils.isInRange(7., 3., 8.));
        assertFalse(NbrsUtils.isInRange(7., 3., Double.POSITIVE_INFINITY));
        
        assertFalse(NbrsUtils.isInRange(7., 3., Double.NaN));
        assertFalse(NbrsUtils.isInRange(Double.NaN, 7., 5.));
        assertFalse(NbrsUtils.isInRange(3., Double.NaN, 5.));
        assertFalse(NbrsUtils.isInRange(3., 7., Double.NaN));
        
        assertFalse(NbrsUtils.isInRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN));
    }

    /*
     * 
     */
    
    public void test_checkIsInRange_3int() {
        try {
            NbrsUtils.checkIsInRange(3, 7, 2);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        
        assertTrue(NbrsUtils.checkIsInRange(3, 7, 5));
    }
    
    public void test_checkIsInRange_3long() {
        try {
            NbrsUtils.checkIsInRange(3L, 7L, 2L);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        assertTrue(NbrsUtils.checkIsInRange(3L, 7L, 5L));
    }
    
    public void test_checkIsInRange_3float() {
        try {
            NbrsUtils.checkIsInRange(3.f, 7.f, 2.f);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        assertTrue(NbrsUtils.checkIsInRange(3.f, 7.f, 5.f));
    }
    
    public void test_checkIsInRange_3double() {
        try {
            NbrsUtils.checkIsInRange(3., 7., 2.);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        assertTrue(NbrsUtils.checkIsInRange(3., 7., 5.));
    }
    
    /*
     * 
     */

    public void test_isInRangeSigned_2int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.isInRangeSigned(0, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertTrue(NbrsUtils.isInRangeSigned(Integer.MIN_VALUE, 32));
        assertTrue(NbrsUtils.isInRangeSigned(0, 32));
        assertTrue(NbrsUtils.isInRangeSigned(Integer.MAX_VALUE, 32));

        assertFalse(NbrsUtils.isInRangeSigned(Integer.MIN_VALUE, 8));
        assertFalse(NbrsUtils.isInRangeSigned(-129, 8));
        //
        assertTrue(NbrsUtils.isInRangeSigned(-128, 8));
        assertTrue(NbrsUtils.isInRangeSigned(0, 8));
        assertTrue(NbrsUtils.isInRangeSigned(127, 8));
        //
        assertFalse(NbrsUtils.isInRangeSigned(128, 8));
        assertFalse(NbrsUtils.isInRangeSigned(Integer.MAX_VALUE, 8));

        assertFalse(NbrsUtils.isInRangeSigned(Integer.MIN_VALUE, 1));
        assertFalse(NbrsUtils.isInRangeSigned(-2, 1));
        //
        assertTrue(NbrsUtils.isInRangeSigned(-1, 1));
        assertTrue(NbrsUtils.isInRangeSigned(0, 1));
        //
        assertFalse(NbrsUtils.isInRangeSigned(1, 1));
        assertFalse(NbrsUtils.isInRangeSigned(Integer.MAX_VALUE, 1));
    }

    public void test_isInRangeSigned_long_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.isInRangeSigned(0L, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertTrue(NbrsUtils.isInRangeSigned(Long.MIN_VALUE, 64));
        assertTrue(NbrsUtils.isInRangeSigned(0L, 64));
        assertTrue(NbrsUtils.isInRangeSigned(Long.MAX_VALUE, 64));

        assertFalse(NbrsUtils.isInRangeSigned(Long.MIN_VALUE, 8));
        assertFalse(NbrsUtils.isInRangeSigned(-129L, 8));
        //
        assertTrue(NbrsUtils.isInRangeSigned(-128L, 8));
        assertTrue(NbrsUtils.isInRangeSigned(0L, 8));
        assertTrue(NbrsUtils.isInRangeSigned(127L, 8));
        //
        assertFalse(NbrsUtils.isInRangeSigned(128L, 8));
        assertFalse(NbrsUtils.isInRangeSigned(Long.MAX_VALUE, 8));

        assertFalse(NbrsUtils.isInRangeSigned(Long.MIN_VALUE, 1));
        assertFalse(NbrsUtils.isInRangeSigned(-2L, 1));
        //
        assertTrue(NbrsUtils.isInRangeSigned(-1L, 1));
        assertTrue(NbrsUtils.isInRangeSigned(0L, 1));
        //
        assertFalse(NbrsUtils.isInRangeSigned(1L, 1));
        assertFalse(NbrsUtils.isInRangeSigned(Long.MAX_VALUE, 1));
    }

    public void test_isInRangeUnsigned_2int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,32,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.isInRangeUnsigned(0, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertFalse(NbrsUtils.isInRangeUnsigned(Integer.MIN_VALUE, 31));
        assertFalse(NbrsUtils.isInRangeUnsigned(-1, 31));
        //
        assertTrue(NbrsUtils.isInRangeUnsigned(0, 31));
        assertTrue(NbrsUtils.isInRangeUnsigned(Integer.MAX_VALUE, 31));
        
        assertFalse(NbrsUtils.isInRangeUnsigned(Integer.MIN_VALUE, 8));
        assertFalse(NbrsUtils.isInRangeUnsigned(-1, 8));
        //
        assertTrue(NbrsUtils.isInRangeUnsigned(0, 8));
        assertTrue(NbrsUtils.isInRangeUnsigned(128, 8));
        assertTrue(NbrsUtils.isInRangeUnsigned(255, 8));
        //
        assertFalse(NbrsUtils.isInRangeUnsigned(256, 8));
        assertFalse(NbrsUtils.isInRangeUnsigned(Integer.MAX_VALUE, 8));

        assertFalse(NbrsUtils.isInRangeUnsigned(Integer.MIN_VALUE, 1));
        assertFalse(NbrsUtils.isInRangeUnsigned(-1, 1));
        //
        assertTrue(NbrsUtils.isInRangeUnsigned(0, 1));
        assertTrue(NbrsUtils.isInRangeUnsigned(1, 1));
        //
        assertFalse(NbrsUtils.isInRangeUnsigned(2, 1));
        assertFalse(NbrsUtils.isInRangeUnsigned(Integer.MAX_VALUE, 1));
    }

    public void test_isInRangeUnsigned_long_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,64,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.isInRangeUnsigned(0L, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertFalse(NbrsUtils.isInRangeUnsigned(Long.MIN_VALUE, 63));
        assertFalse(NbrsUtils.isInRangeUnsigned(-1L, 63));
        //
        assertTrue(NbrsUtils.isInRangeUnsigned(0L, 63));
        assertTrue(NbrsUtils.isInRangeUnsigned(Long.MAX_VALUE, 63));
        
        assertFalse(NbrsUtils.isInRangeUnsigned(Long.MIN_VALUE, 8));
        assertFalse(NbrsUtils.isInRangeUnsigned(-1L, 8));
        //
        assertTrue(NbrsUtils.isInRangeUnsigned(0L, 8));
        assertTrue(NbrsUtils.isInRangeUnsigned(128L, 8));
        assertTrue(NbrsUtils.isInRangeUnsigned(255L, 8));
        //
        assertFalse(NbrsUtils.isInRangeUnsigned(256L, 8));
        assertFalse(NbrsUtils.isInRangeUnsigned(Long.MAX_VALUE, 8));

        assertFalse(NbrsUtils.isInRangeUnsigned(Long.MIN_VALUE, 1));
        assertFalse(NbrsUtils.isInRangeUnsigned(-1L, 1));
        //
        assertTrue(NbrsUtils.isInRangeUnsigned(0L, 1));
        assertTrue(NbrsUtils.isInRangeUnsigned(1L, 1));
        //
        assertFalse(NbrsUtils.isInRangeUnsigned(2L, 1));
        assertFalse(NbrsUtils.isInRangeUnsigned(Long.MAX_VALUE, 1));
    }
    
    /*
     * 
     */
    
    public void test_checkIsInRangeSigned_2int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.checkIsInRangeSigned(0, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertTrue(NbrsUtils.checkIsInRangeSigned(-128, 8));
        assertTrue(NbrsUtils.checkIsInRangeSigned(127, 8));
        try {
            NbrsUtils.checkIsInRangeSigned(-129, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            NbrsUtils.checkIsInRangeSigned(128, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
    
    public void test_checkIsInRangeSigned_long_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.checkIsInRangeSigned(0L, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertTrue(NbrsUtils.checkIsInRangeSigned(-128L, 8));
        assertTrue(NbrsUtils.checkIsInRangeSigned(127L, 8));
        try {
            NbrsUtils.checkIsInRangeSigned(-129L, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            NbrsUtils.checkIsInRangeSigned(128L, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
    
    public void test_checkIsInRangeUnsigned_2int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,32,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.checkIsInRangeUnsigned(0, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertTrue(NbrsUtils.checkIsInRangeUnsigned(0, 8));
        assertTrue(NbrsUtils.checkIsInRangeUnsigned(255, 8));
        try {
            NbrsUtils.checkIsInRangeUnsigned(-1, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            NbrsUtils.checkIsInRangeUnsigned(256, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
    
    public void test_checkIsInRangeUnsigned_long_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,64,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.checkIsInRangeUnsigned(0L, bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertTrue(NbrsUtils.checkIsInRangeUnsigned(0L, 8));
        assertTrue(NbrsUtils.checkIsInRangeUnsigned(255L, 8));
        try {
            NbrsUtils.checkIsInRangeUnsigned(-1L, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            NbrsUtils.checkIsInRangeUnsigned(256L, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
    
    /*
     * 
     */
    
    public void test_toRange_3int() {
        assertEquals(10,NbrsUtils.toRange(10, 5, 9));
        assertEquals(5,NbrsUtils.toRange(10, 5, 11));
        
        for (int min=-10;min<=10;min++) {
            for (int max=min;max<=10;max++) {
                assertEquals(min, NbrsUtils.toRange(min, max, min-1));
                for (int value=min;value<=max;value++) {
                    assertEquals(value, NbrsUtils.toRange(min, max, value));
                }
                assertEquals(max, NbrsUtils.toRange(min, max, max+1));
            }
        }
    }

    public void test_toRange_3long() {
        assertEquals(10L,NbrsUtils.toRange(10L, 5L, 9L));
        assertEquals(5L,NbrsUtils.toRange(10L, 5L, 11L));
        
        for (long min=-10;min<=10;min++) {
            for (long max=min;max<=10;max++) {
                assertEquals(min, NbrsUtils.toRange(min, max, min-1));
                for (long value=min;value<=max;value++) {
                    assertEquals(value, NbrsUtils.toRange(min, max, value));
                }
                assertEquals(max, NbrsUtils.toRange(min, max, max+1));
            }
        }
    }

    public void test_toRange_3float() {
        assertEquals(10.0f,NbrsUtils.toRange(10.0f, 5.0f, 9.0f));
        assertEquals(5.0f,NbrsUtils.toRange(10.0f, 5.0f, 11.0f));
        
        for (float min=-10;min<=10;min++) {
            for (float max=min;max<=10;max++) {
                assertEquals(min, NbrsUtils.toRange(min, max, min-1));
                for (float value=min;value<=max;value++) {
                    assertEquals(value, NbrsUtils.toRange(min, max, value));
                }
                assertEquals(max, NbrsUtils.toRange(min, max, max+1));
            }
        }
    }

    public void test_toRange_3double() {
        assertEquals(10.0,NbrsUtils.toRange(10.0, 5.0, 9.0));
        assertEquals(5.0,NbrsUtils.toRange(10.0, 5.0, 11.0));
        
        for (double min=-10;min<=10;min++) {
            for (double max=min;max<=10;max++) {
                assertEquals(min, NbrsUtils.toRange(min, max, min-1));
                for (double value=min;value<=max;value++) {
                    assertEquals(value, NbrsUtils.toRange(min, max, value));
                }
                assertEquals(max, NbrsUtils.toRange(min, max, max+1));
            }
        }
    }

    /*
     * 
     */

    public void test_intMaskMSBits0_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.intMaskMSBits0(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0xFFFFFFFF,NbrsUtils.intMaskMSBits0(0));
        assertEquals(0x7FFFFFFF,NbrsUtils.intMaskMSBits0(1));
        assertEquals(1,NbrsUtils.intMaskMSBits0(31));
        assertEquals(0,NbrsUtils.intMaskMSBits0(32));
    }
    
    public void test_intMaskMSBits1_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.intMaskMSBits1(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0,NbrsUtils.intMaskMSBits1(0));
        assertEquals(0x80000000,NbrsUtils.intMaskMSBits1(1));
        assertEquals(0xFFFFFFFE,NbrsUtils.intMaskMSBits1(31));
        assertEquals(0xFFFFFFFF,NbrsUtils.intMaskMSBits1(32));
    }

    public void test_intMaskLSBits0_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.intMaskLSBits0(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0xFFFFFFFF,NbrsUtils.intMaskLSBits0(0));
        assertEquals(0xFFFFFFFE,NbrsUtils.intMaskLSBits0(1));
        assertEquals(0x80000000,NbrsUtils.intMaskLSBits0(31));
        assertEquals(0,NbrsUtils.intMaskLSBits0(32));
    }

    public void test_intMaskLSBits1_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.intMaskLSBits1(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0,NbrsUtils.intMaskLSBits1(0));
        assertEquals(1,NbrsUtils.intMaskLSBits1(1));
        assertEquals(0x7FFFFFFF,NbrsUtils.intMaskLSBits1(31));
        assertEquals(0xFFFFFFFF,NbrsUtils.intMaskLSBits1(32));
    }

    /*
     * 
     */
    
    public void test_longMaskMSBits0_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.longMaskMSBits0(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0xFFFFFFFFFFFFFFFFL,NbrsUtils.longMaskMSBits0(0));
        assertEquals(0x7FFFFFFFFFFFFFFFL,NbrsUtils.longMaskMSBits0(1));
        assertEquals(1L,NbrsUtils.longMaskMSBits0(63));
        assertEquals(0L,NbrsUtils.longMaskMSBits0(64));
    }
    
    public void test_longMaskMSBits1_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.longMaskMSBits1(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0L,NbrsUtils.longMaskMSBits1(0));
        assertEquals(0x8000000000000000L,NbrsUtils.longMaskMSBits1(1));
        assertEquals(0xFFFFFFFFFFFFFFFEL,NbrsUtils.longMaskMSBits1(63));
        assertEquals(0xFFFFFFFFFFFFFFFFL,NbrsUtils.longMaskMSBits1(64));
    }

    public void test_longMaskLSBits0_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.longMaskLSBits0(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0xFFFFFFFFFFFFFFFFL,NbrsUtils.longMaskLSBits0(0));
        assertEquals(0xFFFFFFFFFFFFFFFEL,NbrsUtils.longMaskLSBits0(1));
        assertEquals(0x8000000000000000L,NbrsUtils.longMaskLSBits0(63));
        assertEquals(0L,NbrsUtils.longMaskLSBits0(64));
    }

    public void test_longMaskLSBits1_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,-1,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.longMaskLSBits1(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        assertEquals(0L,NbrsUtils.longMaskLSBits1(0));
        assertEquals(1L,NbrsUtils.longMaskLSBits1(1));
        assertEquals(0x7FFFFFFFFFFFFFFFL,NbrsUtils.longMaskLSBits1(63));
        assertEquals(0xFFFFFFFFFFFFFFFFL,NbrsUtils.longMaskLSBits1(64));
    }

    /*
     * 
     */
    
    public void test_byteAsUnsigned_byte() {
        final long signedMin = Byte.MIN_VALUE;
        final long signedMax = Byte.MAX_VALUE;
        final long unsignedMax = 2*signedMax+1;
        
        assertEquals(signedMax+1,NbrsUtils.byteAsUnsigned((byte)signedMin));
        assertEquals(unsignedMax,NbrsUtils.byteAsUnsigned((byte)-1));
        assertEquals(0,NbrsUtils.byteAsUnsigned((byte)0));
        assertEquals(1,NbrsUtils.byteAsUnsigned((byte)1));
        assertEquals(signedMax,NbrsUtils.byteAsUnsigned((byte)signedMax));
    }
    
    public void test_shortAsUnsigned(short value) {
        final long signedMin = Short.MIN_VALUE;
        final long signedMax = Short.MAX_VALUE;
        final long unsignedMax = 2*signedMax+1;
        
        assertEquals(signedMax+1,NbrsUtils.shortAsUnsigned((short)signedMin));
        assertEquals(unsignedMax,NbrsUtils.shortAsUnsigned((short)-1));
        assertEquals(0,NbrsUtils.shortAsUnsigned((short)0));
        assertEquals(1,NbrsUtils.shortAsUnsigned((short)1));
        assertEquals(signedMax,NbrsUtils.shortAsUnsigned((short)signedMax));
    }
    
    public void test_intAsUnsigned(int value) {
        final long signedMin = Integer.MIN_VALUE;
        final long signedMax = Integer.MAX_VALUE;
        final long unsignedMax = 2*signedMax+1;
        
        assertEquals(signedMax+1,NbrsUtils.intAsUnsigned((int)signedMin));
        assertEquals(unsignedMax,NbrsUtils.intAsUnsigned((int)-1));
        assertEquals(0,NbrsUtils.intAsUnsigned((int)0));
        assertEquals(1,NbrsUtils.intAsUnsigned((int)1));
        assertEquals(signedMax,NbrsUtils.intAsUnsigned((int)signedMax));
    }

    /*
     * 
     */
    
    public void test_isValidBitSizeForSignedInt_int() {
        assertFalse(NbrsUtils.isValidBitSizeForSignedInt(Integer.MIN_VALUE));
        assertFalse(NbrsUtils.isValidBitSizeForSignedInt(-1));
        assertFalse(NbrsUtils.isValidBitSizeForSignedInt(0));
        
        assertTrue(NbrsUtils.isValidBitSizeForSignedInt(1));
        assertTrue(NbrsUtils.isValidBitSizeForSignedInt(32));
        
        assertFalse(NbrsUtils.isValidBitSizeForSignedInt(33));
        assertFalse(NbrsUtils.isValidBitSizeForSignedInt(Integer.MAX_VALUE));
    }
    
    public void test_isValidBitSizeForSignedLong_int() {
        assertFalse(NbrsUtils.isValidBitSizeForSignedLong(Integer.MIN_VALUE));
        assertFalse(NbrsUtils.isValidBitSizeForSignedLong(-1));
        assertFalse(NbrsUtils.isValidBitSizeForSignedLong(0));
        
        assertTrue(NbrsUtils.isValidBitSizeForSignedLong(1));
        assertTrue(NbrsUtils.isValidBitSizeForSignedLong(64));
        
        assertFalse(NbrsUtils.isValidBitSizeForSignedLong(65));
        assertFalse(NbrsUtils.isValidBitSizeForSignedLong(Integer.MAX_VALUE));
    }
    
    public void test_isValidBitSizeForUnsignedInt_int() {
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedInt(Integer.MIN_VALUE));
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedInt(-1));
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedInt(0));
        
        assertTrue(NbrsUtils.isValidBitSizeForUnsignedInt(1));
        assertTrue(NbrsUtils.isValidBitSizeForUnsignedInt(31));
        
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedInt(32));
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedInt(Integer.MAX_VALUE));
    }

    public void test_isValidBitSizeForUnsignedLong_int() {
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedLong(Integer.MIN_VALUE));
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedLong(-1));
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedLong(0));
        
        assertTrue(NbrsUtils.isValidBitSizeForUnsignedLong(1));
        assertTrue(NbrsUtils.isValidBitSizeForUnsignedLong(63));
        
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedLong(64));
        assertFalse(NbrsUtils.isValidBitSizeForUnsignedLong(Integer.MAX_VALUE));
    }

    /*
     * 
     */
    
    public void test_checkBitSizeForSignedInt_int() {
        assertEquals(true,NbrsUtils.checkBitSizeForSignedInt(32));
        
        try {
            NbrsUtils.checkBitSizeForSignedInt(33);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void test_checkBitSizeForSignedLong_int() {
        assertEquals(true,NbrsUtils.checkBitSizeForSignedLong(64));
        
        try {
            NbrsUtils.checkBitSizeForSignedLong(65);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void test_checkBitSizeForUnsignedInt_int() {
        assertEquals(true,NbrsUtils.checkBitSizeForUnsignedInt(31));
        
        try {
            NbrsUtils.checkBitSizeForUnsignedInt(32);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
    
    public void test_checkBitSizeForUnsignedLong_int() {
        assertEquals(true,NbrsUtils.checkBitSizeForUnsignedLong(63));
        
        try {
            NbrsUtils.checkBitSizeForUnsignedLong(64);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /*
     * 
     */
    
    public void test_minSignedIntForBitSize_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.minSignedIntForBitSize(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(-1,NbrsUtils.minSignedIntForBitSize(1));
        assertEquals(-2,NbrsUtils.minSignedIntForBitSize(2));
        assertEquals(-4,NbrsUtils.minSignedIntForBitSize(3));
        assertEquals(Integer.MIN_VALUE>>1,NbrsUtils.minSignedIntForBitSize(31));
        assertEquals(Integer.MIN_VALUE,NbrsUtils.minSignedIntForBitSize(32));
    }

    public void test_minSignedLongForBitSize_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.minSignedLongForBitSize(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(-1L,NbrsUtils.minSignedLongForBitSize(1));
        assertEquals(-2L,NbrsUtils.minSignedLongForBitSize(2));
        assertEquals(-4L,NbrsUtils.minSignedLongForBitSize(3));
        assertEquals(Long.MIN_VALUE>>1,NbrsUtils.minSignedLongForBitSize(63));
        assertEquals(Long.MIN_VALUE,NbrsUtils.minSignedLongForBitSize(64));
    }

    public void test_maxSignedIntForBitSize_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,33,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.maxSignedIntForBitSize(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(0,NbrsUtils.maxSignedIntForBitSize(1));
        assertEquals(1,NbrsUtils.maxSignedIntForBitSize(2));
        assertEquals(3,NbrsUtils.maxSignedIntForBitSize(3));
        assertEquals(Integer.MAX_VALUE>>1,NbrsUtils.maxSignedIntForBitSize(31));
        assertEquals(Integer.MAX_VALUE,NbrsUtils.maxSignedIntForBitSize(32));
    }
    
    public void test_maxSignedLongForBitSize_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,65,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.maxSignedLongForBitSize(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(0L,NbrsUtils.maxSignedLongForBitSize(1));
        assertEquals(1L,NbrsUtils.maxSignedLongForBitSize(2));
        assertEquals(3L,NbrsUtils.maxSignedLongForBitSize(3));
        assertEquals(Long.MAX_VALUE>>1,NbrsUtils.maxSignedLongForBitSize(63));
        assertEquals(Long.MAX_VALUE,NbrsUtils.maxSignedLongForBitSize(64));
    }
    
    public void test_maxUnsignedIntForBitSize_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,32,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.maxUnsignedIntForBitSize(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(1,NbrsUtils.maxUnsignedIntForBitSize(1));
        assertEquals(3,NbrsUtils.maxUnsignedIntForBitSize(2));
        assertEquals(7,NbrsUtils.maxUnsignedIntForBitSize(3));
        assertEquals(Integer.MAX_VALUE>>1,NbrsUtils.maxUnsignedIntForBitSize(30));
        assertEquals(Integer.MAX_VALUE,NbrsUtils.maxUnsignedIntForBitSize(31));
    }

    public void test_maxUnsignedLongForBitSize_int() {
        for (int bitSize : new int[]{Integer.MIN_VALUE,0,64,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.maxUnsignedLongForBitSize(bitSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(1L,NbrsUtils.maxUnsignedLongForBitSize(1));
        assertEquals(3L,NbrsUtils.maxUnsignedLongForBitSize(2));
        assertEquals(7L,NbrsUtils.maxUnsignedLongForBitSize(3));
        assertEquals(Long.MAX_VALUE>>1,NbrsUtils.maxUnsignedLongForBitSize(62));
        assertEquals(Long.MAX_VALUE,NbrsUtils.maxUnsignedLongForBitSize(63));
    }

    /*
     * 
     */
    
    public void test_bitSizeForSignedValue_int() {
        /*
         * Considering
         * minSignedIntForBitSize
         * and
         * maxSignedIntForBitSize
         * work.
         */
        for (int bitSize=1;bitSize<=32;bitSize++) {
            int min = NbrsUtils.minSignedIntForBitSize(bitSize);
            int max = NbrsUtils.maxSignedIntForBitSize(bitSize);
            
            assertEquals(bitSize, NbrsUtils.bitSizeForSignedValue(min));
            assertEquals(bitSize, NbrsUtils.bitSizeForSignedValue(max));
            
            if (bitSize != 32) {
                // higher bit size for exterior values
                assertEquals(bitSize+1, NbrsUtils.bitSizeForSignedValue(min-1));
                assertEquals(bitSize+1, NbrsUtils.bitSizeForSignedValue(max+1));
            }
        }
    }
    
    public void test_bitSizeForSignedValue_long() {
        /*
         * Considering
         * minSignedLongForBitSize
         * and
         * maxSignedLongForBitSize
         * work.
         */
        for (int bitSize=1;bitSize<=64;bitSize++) {
            long min = NbrsUtils.minSignedLongForBitSize(bitSize);
            long max = NbrsUtils.maxSignedLongForBitSize(bitSize);
            
            assertEquals(bitSize, NbrsUtils.bitSizeForSignedValue(min));
            assertEquals(bitSize, NbrsUtils.bitSizeForSignedValue(max));
            
            if (bitSize != 64) {
                // higher bit size for exterior values
                assertEquals(bitSize+1, NbrsUtils.bitSizeForSignedValue(min-1));
                assertEquals(bitSize+1, NbrsUtils.bitSizeForSignedValue(max+1));
            }
        }
    }
    
    public void test_bitSizeForUnsignedValue_int() {
        /*
         * Considering
         * maxUnsignedIntForBitSize
         * work.
         */
        for (int bitSize=1;bitSize<=31;bitSize++) {
            int max = NbrsUtils.maxUnsignedIntForBitSize(bitSize);
            
            assertEquals(bitSize, NbrsUtils.bitSizeForUnsignedValue(max));
            
            if (bitSize != 31) {
                // higher bit size for exterior values
                assertEquals(bitSize+1, NbrsUtils.bitSizeForUnsignedValue(max+1));
            }
        }
    }
    
    public void test_bitSizeForUnsignedValue_long() {
        /*
         * Considering
         * maxUnsignedLongForBitSize
         * work.
         */
        for (int bitSize=1;bitSize<=63;bitSize++) {
            long max = NbrsUtils.maxUnsignedLongForBitSize(bitSize);
            
            assertEquals(bitSize, NbrsUtils.bitSizeForUnsignedValue(max));
            
            if (bitSize != 63) {
                // higher bit size for exterior values
                assertEquals(bitSize+1, NbrsUtils.bitSizeForUnsignedValue(max+1));
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_signum_int() {
        assertEquals(-1,NbrsUtils.signum(Integer.MIN_VALUE));
        assertEquals(-1,NbrsUtils.signum(-1));
        assertEquals(0,NbrsUtils.signum(0));
        assertEquals(1,NbrsUtils.signum(1));
        assertEquals(1,NbrsUtils.signum(Integer.MAX_VALUE));
    }
    
    public void test_signum_long() {
        assertEquals(-1,NbrsUtils.signum(Long.MIN_VALUE));
        assertEquals(-1,NbrsUtils.signum(-1L));
        assertEquals(0,NbrsUtils.signum(0L));
        assertEquals(1,NbrsUtils.signum(1L));
        assertEquals(1,NbrsUtils.signum(Long.MAX_VALUE));
    }

    public void test_isEven_int() {
        for (Integer even : EVEN_INT_VALUES) {
            assertTrue(NbrsUtils.isEven(even));
        }
        
        for (Integer odd : ODD_INT_VALUES) {
            assertFalse(NbrsUtils.isEven(odd));
        }
    }

    public void test_isEven_long() {
        for (Long even : EVEN_LONG_VALUES) {
            assertTrue(NbrsUtils.isEven(even));
        }
        
        for (Long odd : ODD_LONG_VALUES) {
            assertFalse(NbrsUtils.isEven(odd));
        }
    }

    public void test_isOdd_int() {
        for (Integer even : EVEN_INT_VALUES) {
            assertFalse(NbrsUtils.isOdd(even));
        }
        
        for (Integer odd : ODD_INT_VALUES) {
            assertTrue(NbrsUtils.isOdd(odd));
        }
    }

    public void test_isOdd_long() {
        for (Long even : EVEN_LONG_VALUES) {
            assertFalse(NbrsUtils.isOdd(even));
        }
        
        for (Long odd : ODD_LONG_VALUES) {
            assertTrue(NbrsUtils.isOdd(odd));
        }
    }

    public void test_haveSameEvenness_2int() {
        for (Integer even : EVEN_INT_VALUES) {
            for (Integer odd : ODD_INT_VALUES) {
                assertFalse(NbrsUtils.haveSameEvenness(even, odd));
                assertFalse(NbrsUtils.haveSameEvenness(odd, even));
            }
        }
        
        for (Integer even1 : EVEN_INT_VALUES) {
            for (Integer even2 : EVEN_INT_VALUES) {
                assertTrue(NbrsUtils.haveSameEvenness(even1, even2));
            }
        }
        
        for (Integer odd1 : ODD_INT_VALUES) {
            for (Integer odd2 : ODD_INT_VALUES) {
                assertTrue(NbrsUtils.haveSameEvenness(odd1, odd2));
            }
        }
    }
    
    public void test_haveSameEvenness_2long() {
        for (Long even : EVEN_LONG_VALUES) {
            for (Long odd : ODD_LONG_VALUES) {
                assertFalse(NbrsUtils.haveSameEvenness(even, odd));
                assertFalse(NbrsUtils.haveSameEvenness(odd, even));
            }
        }
        
        for (Long even1 : EVEN_LONG_VALUES) {
            for (Long even2 : EVEN_LONG_VALUES) {
                assertTrue(NbrsUtils.haveSameEvenness(even1, even2));
            }
        }
        
        for (Long odd1 : ODD_LONG_VALUES) {
            for (Long odd2 : ODD_LONG_VALUES) {
                assertTrue(NbrsUtils.haveSameEvenness(odd1, odd2));
            }
        }
    }
    
    public void test_haveSameSign_2int() {
        for (int i = 1; i < 10; i++) {
            for (int j = 1; j < 10;j++) {
                assertTrue(NbrsUtils.haveSameSign(i, j));
                assertTrue(NbrsUtils.haveSameSign(-i, -j));
                assertFalse(NbrsUtils.haveSameSign(i, -j));
                assertFalse(NbrsUtils.haveSameSign(-i, j));
            }
            assertTrue(NbrsUtils.haveSameSign(0, i));
            assertTrue(NbrsUtils.haveSameSign(i, 0));
            assertFalse(NbrsUtils.haveSameSign(0, -i));
            assertFalse(NbrsUtils.haveSameSign(-i, 0));
        }
        
        assertTrue(NbrsUtils.haveSameSign(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertTrue(NbrsUtils.haveSameSign(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertTrue(NbrsUtils.haveSameSign(0, Integer.MAX_VALUE));
        assertTrue(NbrsUtils.haveSameSign(Integer.MAX_VALUE, 0));
        assertFalse(NbrsUtils.haveSameSign(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertFalse(NbrsUtils.haveSameSign(Integer.MIN_VALUE, 0));
        assertFalse(NbrsUtils.haveSameSign(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertFalse(NbrsUtils.haveSameSign(0, Integer.MIN_VALUE));
    }
    
    public void test_haveSameSign_2long() {
        for (long i = 1; i < 10; i++) {
            for (long j = 1; j < 10; j++) {
                assertTrue(NbrsUtils.haveSameSign(i, j));
                assertTrue(NbrsUtils.haveSameSign(-i, -j));
                assertFalse(NbrsUtils.haveSameSign(i, -j));
                assertFalse(NbrsUtils.haveSameSign(-i, j));
            }
            assertTrue(NbrsUtils.haveSameSign(0L, i));
            assertTrue(NbrsUtils.haveSameSign(i, 0L));
            assertFalse(NbrsUtils.haveSameSign(0L, -i));
            assertFalse(NbrsUtils.haveSameSign(-i, 0L));
        }
        
        assertTrue(NbrsUtils.haveSameSign(Long.MAX_VALUE, Long.MAX_VALUE));
        assertTrue(NbrsUtils.haveSameSign(Long.MIN_VALUE, Long.MIN_VALUE));
        assertTrue(NbrsUtils.haveSameSign(0L, Long.MAX_VALUE));
        assertTrue(NbrsUtils.haveSameSign(Long.MAX_VALUE, 0L));
        assertFalse(NbrsUtils.haveSameSign(Long.MIN_VALUE, Long.MAX_VALUE));
        assertFalse(NbrsUtils.haveSameSign(Long.MIN_VALUE, 0L));
        assertFalse(NbrsUtils.haveSameSign(Long.MAX_VALUE, Long.MIN_VALUE));
        assertFalse(NbrsUtils.haveSameSign(0L, Long.MIN_VALUE));
    }
    
    public void test_isPowerOfTwo_int() {
        for (int i = -128; i <= 128; i++) {
            final boolean expected = (i > 0) && (i == Integer.highestOneBit(i));
            assertEquals(expected, NbrsUtils.isPowerOfTwo(i));
        }
        
        for (int k = 0; k <= 30; k++) {
            assertTrue(NbrsUtils.isPowerOfTwo(1<<k));
        }
        
        assertFalse(NbrsUtils.isPowerOfTwo(Integer.MIN_VALUE));
        assertFalse(NbrsUtils.isPowerOfTwo(Integer.MIN_VALUE+1));
        assertFalse(NbrsUtils.isPowerOfTwo(Integer.MIN_VALUE+2));

        assertFalse(NbrsUtils.isPowerOfTwo(Integer.MAX_VALUE));
        assertFalse(NbrsUtils.isPowerOfTwo(Integer.MAX_VALUE-1));
        assertFalse(NbrsUtils.isPowerOfTwo(Integer.MAX_VALUE-2));
    }
    
    public void test_isPowerOfTwo_long() {
        for (long i = -128; i <= 128; i++) {
            final boolean expected = (i > 0) && (i == Long.highestOneBit(i));
            assertEquals(expected, NbrsUtils.isPowerOfTwo(i));
        }
        
        for (int k = 0; k <= 62; k++) {
            assertTrue(NbrsUtils.isPowerOfTwo(1L<<k));
        }
        
        assertFalse(NbrsUtils.isPowerOfTwo(Long.MIN_VALUE));
        assertFalse(NbrsUtils.isPowerOfTwo(Long.MIN_VALUE+1));
        assertFalse(NbrsUtils.isPowerOfTwo(Long.MIN_VALUE+2));

        assertFalse(NbrsUtils.isPowerOfTwo(Long.MAX_VALUE));
        assertFalse(NbrsUtils.isPowerOfTwo(Long.MAX_VALUE-1));
        assertFalse(NbrsUtils.isPowerOfTwo(Long.MAX_VALUE-2));
    }

    public void test_isSignedPowerOfTwo_int() {
        for (int i = -128; i <= 128; i++) {
            final boolean expected =
                (i != 0)
                && ((i == Integer.MIN_VALUE)
                        || (Math.abs(i) == Integer.highestOneBit(Math.abs(i))));
            assertEquals(expected, NbrsUtils.isSignedPowerOfTwo(i));
        }
        
        for (int k = 0; k <= 30; k++) {
            assertTrue(NbrsUtils.isSignedPowerOfTwo(1<<k));
            assertTrue(NbrsUtils.isSignedPowerOfTwo(-(1<<k)));
        }
        assertTrue(NbrsUtils.isSignedPowerOfTwo(Integer.MIN_VALUE));
        
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Integer.MIN_VALUE+1));
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Integer.MIN_VALUE+2));

        assertFalse(NbrsUtils.isSignedPowerOfTwo(Integer.MAX_VALUE));
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Integer.MAX_VALUE-1));
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Integer.MAX_VALUE-2));
    }

    public void test_isSignedPowerOfTwo_long() {
        for (long i = -128; i <= 128; i++) {
            final boolean expected =
                (i != 0)
                && ((i == Long.MIN_VALUE)
                        || (Math.abs(i) == Long.highestOneBit(Math.abs(i))));
            assertEquals(expected, NbrsUtils.isSignedPowerOfTwo(i));
        }
        
        for (int k = 0; k <= 62; k++) {
            assertTrue(NbrsUtils.isSignedPowerOfTwo(1L<<k));
            assertTrue(NbrsUtils.isSignedPowerOfTwo(-(1L<<k)));
        }
        assertTrue(NbrsUtils.isSignedPowerOfTwo(Long.MIN_VALUE));
        
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Long.MIN_VALUE+1));
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Long.MIN_VALUE+2));

        assertFalse(NbrsUtils.isSignedPowerOfTwo(Long.MAX_VALUE));
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Long.MAX_VALUE-1));
        assertFalse(NbrsUtils.isSignedPowerOfTwo(Long.MAX_VALUE-2));
    }
    
    public void test_floorPowerOfTwo_int() {
        for (int i : new int[]{
                Integer.MIN_VALUE,
                Integer.MIN_VALUE+1,
                -1,
                0}) {
            try {
                NbrsUtils.floorPowerOfTwo(i);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int i : new int[]{(1<<30)+1,Integer.MAX_VALUE}) {
            assertEquals(1<<30, NbrsUtils.floorPowerOfTwo(i));
        }

        for (int i = 1; i <= 128; i++) {
            final int expected = 1<<((int)Math.floor(Math.log(i)/Math.log(2)));
            assertEquals(expected, NbrsUtils.floorPowerOfTwo(i));
        }

        for (int k = 0; k <= 30; k++) {
            final int pot = (1<<k);
            if (k == 0) {
                // underflow
            } else {
                assertEquals(pot/2, NbrsUtils.floorPowerOfTwo(pot-1));
            }
            assertEquals(pot, NbrsUtils.floorPowerOfTwo(pot));
            if (k == 0) {
                // floorPowerOfTwo(2) is not 1, but still 2
                assertEquals(pot+1, NbrsUtils.floorPowerOfTwo(pot+1));
            } else {
                assertEquals(pot, NbrsUtils.floorPowerOfTwo(pot+1));
            }
        }
    }

    public void test_floorPowerOfTwo_long() {
        for (long i : new long[]{
                Long.MIN_VALUE,
                Long.MIN_VALUE+1,
                -1L,
                0L}) {
            try {
                NbrsUtils.floorPowerOfTwo(i);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (long i : new long[]{(1L<<62)+1,Long.MAX_VALUE}) {
            assertEquals(1L<<62, NbrsUtils.floorPowerOfTwo(i));
        }

        for (long i = 1; i <= 128; i++) {
            final int expected = 1<<((int)Math.floor(Math.log(i)/Math.log(2)));
            assertEquals(expected, NbrsUtils.floorPowerOfTwo(i));
        }
        
        for (int k = 0; k <= 62; k++) {
            final long pot = (1L<<k);
            if (k == 0) {
                // underflow
            } else {
                assertEquals(pot/2, NbrsUtils.floorPowerOfTwo(pot-1));
            }
            assertEquals(pot, NbrsUtils.floorPowerOfTwo(pot));
            if (k == 0) {
                // floorPowerOfTwo(2) is not 1, but still 2
                assertEquals(pot+1, NbrsUtils.floorPowerOfTwo(pot+1));
            } else {
                assertEquals(pot, NbrsUtils.floorPowerOfTwo(pot+1));
            }
        }
    }

    public void test_ceilingPowerOfTwo_int() {
        for (int i : new int[]{
                Integer.MIN_VALUE,
                Integer.MIN_VALUE+1,
                -1,
                (1<<30)+1,
                Integer.MAX_VALUE-1,
                Integer.MAX_VALUE}) {
            try {
                NbrsUtils.ceilingPowerOfTwo(i);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int i = 0; i <= 128; i++) {
            final int expected = (i == 0) ? 1 : 1<<((int)Math.ceil(Math.log(i)/Math.log(2)));
            assertEquals(expected, NbrsUtils.ceilingPowerOfTwo(i));
        }
        
        for (int k = 0; k <= 30; k++) {
            final int pot = (1<<k);
            if (k == 1) {
                // ceilingPowerOfTwo(1) is not 2, but still 1
            } else {
                assertEquals(pot, NbrsUtils.ceilingPowerOfTwo(pot-1));
            }
            assertEquals(pot, NbrsUtils.ceilingPowerOfTwo(pot));
            if (k == 30) {
                // overflow
            } else {
                assertEquals(2*pot, NbrsUtils.ceilingPowerOfTwo(pot+1));
            }
        }
    }
    
    public void test_ceilingPowerOfTwo_long() {
        for (long i : new long[]{
                Long.MIN_VALUE,
                Long.MIN_VALUE+1,
                -1,
                (1L<<62)+1,
                Long.MAX_VALUE-1,
                Long.MAX_VALUE}) {
            try {
                NbrsUtils.ceilingPowerOfTwo(i);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (long i = 0; i <= 128; i++) {
            final long expected = (i == 0) ? 1 : 1L<<((int)Math.ceil(Math.log(i)/Math.log(2)));
            assertEquals(expected, NbrsUtils.ceilingPowerOfTwo(i));
        }
        
        for (int k = 0; k <= 62; k++) {
            final long pot = (1L<<k);
            if (k == 1) {
                // ceilingPowerOfTwo(1) is not 2, but still 1
            } else {
                assertEquals(pot, NbrsUtils.ceilingPowerOfTwo(pot-1));
            }
            assertEquals(pot, NbrsUtils.ceilingPowerOfTwo(pot));
            if (k == 62) {
                // overflow
            } else {
                assertEquals(2*pot, NbrsUtils.ceilingPowerOfTwo(pot+1));
            }
        }
    }
    
    public void test_meanLow_2int() {
        for (int a = -100; a <= 100; a++) {
            for (int b = -100; b <= 100; b++) {
                int expected = (int) Math.floor((a+b)/2.0);
                if (false) {
                    // Expected value can also be computed like this
                    // (only integer arithmetic).
                    if (((a|b) < 0) && NbrsUtils.isOdd(a+b)) {
                        // if a+b is odd (inexact),
                        // and one is negative (rounded to the lower)
                        expected = (a+b-1)/2;
                    } else {
                        // if a+b is even (exact),
                        // or both >= 0 (rounded to the lower)
                        expected = (a+b)/2;
                    }
                }
                assertEquals(expected, NbrsUtils.meanLow(a, b));
            }
        }

        assertEquals(Integer.MIN_VALUE, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MIN_VALUE)); // exact
        assertEquals(Integer.MIN_VALUE, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MIN_VALUE+1)); // rounded-
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MIN_VALUE+2)); // exact
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MIN_VALUE+3)); // rounded-

        assertEquals(-3, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MAX_VALUE-4)); // rounded-
        assertEquals(-2, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MAX_VALUE-3)); // exact
        assertEquals(-2, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MAX_VALUE-2)); // rounded-
        assertEquals(-1, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MAX_VALUE-1)); // exact
        assertEquals(-1, NbrsUtils.meanLow(Integer.MIN_VALUE, Integer.MAX_VALUE)); // rounded-
        assertEquals(0, NbrsUtils.meanLow(Integer.MIN_VALUE+1, Integer.MAX_VALUE)); // exact
        assertEquals(0, NbrsUtils.meanLow(Integer.MIN_VALUE+2, Integer.MAX_VALUE)); // rounded-
        assertEquals(1, NbrsUtils.meanLow(Integer.MIN_VALUE+3, Integer.MAX_VALUE)); // exact
        assertEquals(1, NbrsUtils.meanLow(Integer.MIN_VALUE+4, Integer.MAX_VALUE)); // rounded-
        assertEquals(2, NbrsUtils.meanLow(Integer.MIN_VALUE+5, Integer.MAX_VALUE)); // exact
        assertEquals(2, NbrsUtils.meanLow(Integer.MIN_VALUE+6, Integer.MAX_VALUE)); // rounded-

        assertEquals(Integer.MAX_VALUE, NbrsUtils.meanLow(Integer.MAX_VALUE, Integer.MAX_VALUE)); // exact
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.meanLow(Integer.MAX_VALUE-1, Integer.MAX_VALUE)); // rounded-
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.meanLow(Integer.MAX_VALUE-2, Integer.MAX_VALUE)); // exact
        assertEquals(Integer.MAX_VALUE-2, NbrsUtils.meanLow(Integer.MAX_VALUE-3, Integer.MAX_VALUE)); // rounded-
    }

    public void test_meanLow_2long() {
        for (long a=-100;a<=100;a++) {
            for (long b=-100;b<=100;b++) {
                long expected = (long)Math.floor((a+b)/2.0);
                assertEquals(expected, NbrsUtils.meanLow(a, b));
            }
        }

        assertEquals(Long.MIN_VALUE, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MIN_VALUE)); // exact
        assertEquals(Long.MIN_VALUE, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MIN_VALUE+1)); // rounded-
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MIN_VALUE+2)); // exact
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MIN_VALUE+3)); // rounded-

        assertEquals(-3L, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MAX_VALUE-4)); // rounded-
        assertEquals(-2L, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MAX_VALUE-3)); // exact
        assertEquals(-2L, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MAX_VALUE-2)); // rounded-
        assertEquals(-1L, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MAX_VALUE-1)); // exact
        assertEquals(-1L, NbrsUtils.meanLow(Long.MIN_VALUE, Long.MAX_VALUE)); // rounded-
        assertEquals(0L, NbrsUtils.meanLow(Long.MIN_VALUE+1, Long.MAX_VALUE)); // exact
        assertEquals(0L, NbrsUtils.meanLow(Long.MIN_VALUE+2, Long.MAX_VALUE)); // rounded-
        assertEquals(1L, NbrsUtils.meanLow(Long.MIN_VALUE+3, Long.MAX_VALUE)); // exact
        assertEquals(1L, NbrsUtils.meanLow(Long.MIN_VALUE+4, Long.MAX_VALUE)); // rounded-
        assertEquals(2L, NbrsUtils.meanLow(Long.MIN_VALUE+5, Long.MAX_VALUE)); // exact
        assertEquals(2L, NbrsUtils.meanLow(Long.MIN_VALUE+6, Long.MAX_VALUE)); // rounded-

        assertEquals(Long.MAX_VALUE, NbrsUtils.meanLow(Long.MAX_VALUE, Long.MAX_VALUE)); // exact
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.meanLow(Long.MAX_VALUE-1, Long.MAX_VALUE)); // rounded-
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.meanLow(Long.MAX_VALUE-2, Long.MAX_VALUE)); // exact
        assertEquals(Long.MAX_VALUE-2, NbrsUtils.meanLow(Long.MAX_VALUE-3, Long.MAX_VALUE)); // rounded-
    }

    public void test_meanSml_2int() {
        for (int a = -100; a <= 100; a++) {
            for (int b = -100; b <= 100; b++) {
                int expected = (a+b)/2;
                assertEquals(expected, NbrsUtils.meanSml(a, b));
            }
        }

        assertEquals(Integer.MIN_VALUE, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MIN_VALUE)); // exact
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MIN_VALUE+1)); // rounded+
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MIN_VALUE+2)); // exact
        assertEquals(Integer.MIN_VALUE+2, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MIN_VALUE+3)); // rounded+

        assertEquals(-2, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MAX_VALUE-4)); // rounded+
        assertEquals(-2, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MAX_VALUE-3)); // exact
        assertEquals(-1, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MAX_VALUE-2)); // rounded+
        assertEquals(-1, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MAX_VALUE-1)); // exact
        assertEquals(0, NbrsUtils.meanSml(Integer.MIN_VALUE, Integer.MAX_VALUE)); // rounded+
        assertEquals(0, NbrsUtils.meanSml(Integer.MIN_VALUE+1, Integer.MAX_VALUE)); // exact
        assertEquals(0, NbrsUtils.meanSml(Integer.MIN_VALUE+2, Integer.MAX_VALUE)); // rounded-
        assertEquals(1, NbrsUtils.meanSml(Integer.MIN_VALUE+3, Integer.MAX_VALUE)); // exact
        assertEquals(1, NbrsUtils.meanSml(Integer.MIN_VALUE+4, Integer.MAX_VALUE)); // rounded-
        assertEquals(2, NbrsUtils.meanSml(Integer.MIN_VALUE+5, Integer.MAX_VALUE)); // exact
        assertEquals(2, NbrsUtils.meanSml(Integer.MIN_VALUE+6, Integer.MAX_VALUE)); // rounded-

        assertEquals(Integer.MAX_VALUE, NbrsUtils.meanSml(Integer.MAX_VALUE, Integer.MAX_VALUE)); // exact
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.meanSml(Integer.MAX_VALUE-1, Integer.MAX_VALUE)); // rounded-
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.meanSml(Integer.MAX_VALUE-2, Integer.MAX_VALUE)); // exact
        assertEquals(Integer.MAX_VALUE-2, NbrsUtils.meanSml(Integer.MAX_VALUE-3, Integer.MAX_VALUE)); // rounded-
    }
    
    public void test_meanSml_2long() {
        for (long a=-100;a<=100;a++) {
            for (long b=-100;b<=100;b++) {
                long expected = (a+b)/2;
                assertEquals(expected, NbrsUtils.meanSml(a, b));
            }
        }

        assertEquals(Long.MIN_VALUE, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MIN_VALUE)); // exact
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MIN_VALUE+1)); // rounded+
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MIN_VALUE+2)); // exact
        assertEquals(Long.MIN_VALUE+2, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MIN_VALUE+3)); // rounded+

        assertEquals(-2L, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MAX_VALUE-4)); // rounded+
        assertEquals(-2L, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MAX_VALUE-3)); // exact
        assertEquals(-1L, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MAX_VALUE-2)); // rounded+
        assertEquals(-1L, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MAX_VALUE-1)); // exact
        assertEquals(0L, NbrsUtils.meanSml(Long.MIN_VALUE, Long.MAX_VALUE)); // rounded+
        assertEquals(0L, NbrsUtils.meanSml(Long.MIN_VALUE+1, Long.MAX_VALUE)); // exact
        assertEquals(0L, NbrsUtils.meanSml(Long.MIN_VALUE+2, Long.MAX_VALUE)); // rounded-
        assertEquals(1L, NbrsUtils.meanSml(Long.MIN_VALUE+3, Long.MAX_VALUE)); // exact
        assertEquals(1L, NbrsUtils.meanSml(Long.MIN_VALUE+4, Long.MAX_VALUE)); // rounded-
        assertEquals(2L, NbrsUtils.meanSml(Long.MIN_VALUE+5, Long.MAX_VALUE)); // exact
        assertEquals(2L, NbrsUtils.meanSml(Long.MIN_VALUE+6, Long.MAX_VALUE)); // rounded-

        assertEquals(Long.MAX_VALUE, NbrsUtils.meanSml(Long.MAX_VALUE, Long.MAX_VALUE)); // exact
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.meanSml(Long.MAX_VALUE-1, Long.MAX_VALUE)); // rounded-
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.meanSml(Long.MAX_VALUE-2, Long.MAX_VALUE)); // exact
        assertEquals(Long.MAX_VALUE-2, NbrsUtils.meanSml(Long.MAX_VALUE-3, Long.MAX_VALUE)); // rounded-
    }

    public void test_negHalfWidth_2int() {
        // min > max
        try {
            NbrsUtils.negHalfWidth(2, 1);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        
        for (int min=-100;min<=100;min++) {
            for (int max=min;max<=100;max++) {
                int expected = -((max-min+1)/2);
                assertEquals(expected, NbrsUtils.negHalfWidth(min, max));
            }
        }

        assertEquals(0, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MIN_VALUE)); // rounded
        assertEquals(-1, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MIN_VALUE+1)); // exact
        assertEquals(-1, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MIN_VALUE+2)); // rounded
        assertEquals(-2, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MIN_VALUE+3)); // exact
        
        assertEquals(Integer.MIN_VALUE+2, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MAX_VALUE-3)); // rounded
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MAX_VALUE-2)); // exact
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MAX_VALUE-1)); // rounded
        assertEquals(Integer.MIN_VALUE, NbrsUtils.negHalfWidth(Integer.MIN_VALUE, Integer.MAX_VALUE)); // exact
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.negHalfWidth(Integer.MIN_VALUE+1, Integer.MAX_VALUE)); // rounded
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.negHalfWidth(Integer.MIN_VALUE+2, Integer.MAX_VALUE)); // exact
        assertEquals(Integer.MIN_VALUE+2, NbrsUtils.negHalfWidth(Integer.MIN_VALUE+3, Integer.MAX_VALUE)); // rounded

        assertEquals(0, NbrsUtils.negHalfWidth(Integer.MAX_VALUE, Integer.MAX_VALUE)); // rounded
        assertEquals(-1, NbrsUtils.negHalfWidth(Integer.MAX_VALUE-1, Integer.MAX_VALUE)); // exact
        assertEquals(-1, NbrsUtils.negHalfWidth(Integer.MAX_VALUE-2, Integer.MAX_VALUE)); // rounded
        assertEquals(-2, NbrsUtils.negHalfWidth(Integer.MAX_VALUE-3, Integer.MAX_VALUE)); // exact
    }

    public void test_negHalfWidth_2long() {
        // min > max
        try {
            NbrsUtils.negHalfWidth(2L, 1L);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        
        for (long min=-100;min<=100;min++) {
            for (long max=min;max<=100;max++) {
                long expected = -((max-min+1)/2);
                assertEquals(expected, NbrsUtils.negHalfWidth(min, max));
            }
        }

        assertEquals(0L, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MIN_VALUE)); // rounded
        assertEquals(-1L, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MIN_VALUE+1)); // exact
        assertEquals(-1L, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MIN_VALUE+2)); // rounded
        assertEquals(-2L, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MIN_VALUE+3)); // exact
        
        assertEquals(Long.MIN_VALUE+2, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MAX_VALUE-3)); // rounded
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MAX_VALUE-2)); // exact
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MAX_VALUE-1)); // rounded
        assertEquals(Long.MIN_VALUE, NbrsUtils.negHalfWidth(Long.MIN_VALUE, Long.MAX_VALUE)); // exact
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.negHalfWidth(Long.MIN_VALUE+1, Long.MAX_VALUE)); // rounded
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.negHalfWidth(Long.MIN_VALUE+2, Long.MAX_VALUE)); // exact
        assertEquals(Long.MIN_VALUE+2, NbrsUtils.negHalfWidth(Long.MIN_VALUE+3, Long.MAX_VALUE)); // rounded

        assertEquals(0L, NbrsUtils.negHalfWidth(Long.MAX_VALUE, Long.MAX_VALUE)); // rounded
        assertEquals(-1L, NbrsUtils.negHalfWidth(Long.MAX_VALUE-1, Long.MAX_VALUE)); // exact
        assertEquals(-1L, NbrsUtils.negHalfWidth(Long.MAX_VALUE-2, Long.MAX_VALUE)); // rounded
        assertEquals(-2L, NbrsUtils.negHalfWidth(Long.MAX_VALUE-3, Long.MAX_VALUE)); // exact
    }
    
    public void test_moduloSignedPowerOfTwo_2int() {
        for (int opSign=-1;opSign<=1;opSign+=2) {
            for (int p=0;p<=31;p++) {
                int npot = Integer.MIN_VALUE>>(31-p);
                // sign has no effect if npot is min possible value
                int spot = opSign * npot;
                for (int a = -100; a <= 100; a++) {
                    int expected = a % spot;
                    assertEquals(expected, NbrsUtils.moduloSignedPowerOfTwo(a, spot));
                }
            }
        }
        
        final int p30 = (1<<30);
        final int np31 = Integer.MIN_VALUE;
        final int p31m1 = Integer.MAX_VALUE;
        
        ArrayList<Integer> sp30s = new ArrayList<Integer>();
        sp30s.add(p30);
        sp30s.add(-p30);
        
        /*
         * reasonable powers
         */
        
        for (Integer sp30 : sp30s) {
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(np31, sp30));
            assertEquals(-p30+1, NbrsUtils.moduloSignedPowerOfTwo(np31+1, sp30));
            assertEquals(-p30+2, NbrsUtils.moduloSignedPowerOfTwo(np31+2, sp30));
            assertEquals(-p30+3, NbrsUtils.moduloSignedPowerOfTwo(np31+3, sp30));
            assertEquals(-3, NbrsUtils.moduloSignedPowerOfTwo(-p30-3, sp30));
            assertEquals(-2, NbrsUtils.moduloSignedPowerOfTwo(-p30-2, sp30));
            assertEquals(-1, NbrsUtils.moduloSignedPowerOfTwo(-p30-1, sp30));
            //
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(-p30, sp30));
            //
            assertEquals(-p30+1, NbrsUtils.moduloSignedPowerOfTwo(-p30+1, sp30));
            assertEquals(-p30+2, NbrsUtils.moduloSignedPowerOfTwo(-p30+2, sp30));
            assertEquals(-p30+3, NbrsUtils.moduloSignedPowerOfTwo(-p30+3, sp30));
            assertEquals(-3, NbrsUtils.moduloSignedPowerOfTwo(-3, sp30));
            assertEquals(-2, NbrsUtils.moduloSignedPowerOfTwo(-2, sp30));
            assertEquals(-1, NbrsUtils.moduloSignedPowerOfTwo(-1, sp30));
            //
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(0, sp30));
            //
            assertEquals(1, NbrsUtils.moduloSignedPowerOfTwo(1, sp30));
            assertEquals(2, NbrsUtils.moduloSignedPowerOfTwo(2, sp30));
            assertEquals(3, NbrsUtils.moduloSignedPowerOfTwo(3, sp30));
            assertEquals(p30-3, NbrsUtils.moduloSignedPowerOfTwo(p30-3, sp30));
            assertEquals(p30-2, NbrsUtils.moduloSignedPowerOfTwo(p30-2, sp30));
            assertEquals(p30-1, NbrsUtils.moduloSignedPowerOfTwo(p30-1, sp30));
            //
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(p30, sp30));
            //
            assertEquals(1, NbrsUtils.moduloSignedPowerOfTwo(p30+1, sp30));
            assertEquals(2, NbrsUtils.moduloSignedPowerOfTwo(p30+2, sp30));
            assertEquals(3, NbrsUtils.moduloSignedPowerOfTwo(p30+3, sp30));
            assertEquals(p30-3, NbrsUtils.moduloSignedPowerOfTwo(p31m1-2, sp30));
            assertEquals(p30-2, NbrsUtils.moduloSignedPowerOfTwo(p31m1-1, sp30));
            assertEquals(p30-1, NbrsUtils.moduloSignedPowerOfTwo(p31m1, sp30));
        }
        
        /*
         * Integer.MIN_VALUE power
         * (value never changed, except if Integer.MIN_VALUE itself, in which case it gives 0)
         */
        
        assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(np31, np31));
        assertEquals(np31+1, NbrsUtils.moduloSignedPowerOfTwo(np31+1, np31));
        assertEquals(np31+2, NbrsUtils.moduloSignedPowerOfTwo(np31+2, np31));
        assertEquals(np31+3, NbrsUtils.moduloSignedPowerOfTwo(np31+3, np31));
        assertEquals(-p30-3, NbrsUtils.moduloSignedPowerOfTwo(-p30-3, np31));
        assertEquals(-p30-2, NbrsUtils.moduloSignedPowerOfTwo(-p30-2, np31));
        assertEquals(-p30-1, NbrsUtils.moduloSignedPowerOfTwo(-p30-1, np31));
        //
        assertEquals(-p30, NbrsUtils.moduloSignedPowerOfTwo(-p30, np31));
        //
        assertEquals(-p30+1, NbrsUtils.moduloSignedPowerOfTwo(-p30+1, np31));
        assertEquals(-p30+2, NbrsUtils.moduloSignedPowerOfTwo(-p30+2, np31));
        assertEquals(-p30+3, NbrsUtils.moduloSignedPowerOfTwo(-p30+3, np31));
        assertEquals(-3, NbrsUtils.moduloSignedPowerOfTwo(-3, np31));
        assertEquals(-2, NbrsUtils.moduloSignedPowerOfTwo(-2, np31));
        assertEquals(-1, NbrsUtils.moduloSignedPowerOfTwo(-1, np31));
        //
        assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(0, np31));
        //
        assertEquals(1, NbrsUtils.moduloSignedPowerOfTwo(1, np31));
        assertEquals(2, NbrsUtils.moduloSignedPowerOfTwo(2, np31));
        assertEquals(3, NbrsUtils.moduloSignedPowerOfTwo(3, np31));
        assertEquals(p30-3, NbrsUtils.moduloSignedPowerOfTwo(p30-3, np31));
        assertEquals(p30-2, NbrsUtils.moduloSignedPowerOfTwo(p30-2, np31));
        assertEquals(p30-1, NbrsUtils.moduloSignedPowerOfTwo(p30-1, np31));
        //
        assertEquals(p30, NbrsUtils.moduloSignedPowerOfTwo(p30, np31));
        //
        assertEquals(p30+1, NbrsUtils.moduloSignedPowerOfTwo(p30+1, np31));
        assertEquals(p30+2, NbrsUtils.moduloSignedPowerOfTwo(p30+2, np31));
        assertEquals(p30+3, NbrsUtils.moduloSignedPowerOfTwo(p30+3, np31));
        assertEquals(p31m1-2, NbrsUtils.moduloSignedPowerOfTwo(p31m1-2, np31));
        assertEquals(p31m1-1, NbrsUtils.moduloSignedPowerOfTwo(p31m1-1, np31));
        assertEquals(p31m1, NbrsUtils.moduloSignedPowerOfTwo(p31m1, np31));
    }
    
    public void test_moduloSignedPowerOfTwo_2long() {
        for (int opSign=-1;opSign<=1;opSign+=2) {
            for (long p=0;p<=63;p++) {
                long npot = Long.MIN_VALUE>>(63-p);
                // sign has no effect if npot is min possible value
                long spot = opSign * npot;
                for (long a=-100;a<=100;a++) {
                    long expected = a % spot;
                    assertEquals(expected, NbrsUtils.moduloSignedPowerOfTwo(a, spot));
                }
            }
        }
        
        final long p62 = (1L<<62);
        final long np63 = Long.MIN_VALUE;
        final long p63m1 = Long.MAX_VALUE;
        
        ArrayList<Long> sp62s = new ArrayList<Long>();
        sp62s.add(p62);
        sp62s.add(-p62);
        
        /*
         * reasonable powers
         */
        
        for (Long sp62 : sp62s) {
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(np63, sp62));
            assertEquals(-p62+1, NbrsUtils.moduloSignedPowerOfTwo(np63+1, sp62));
            assertEquals(-p62+2, NbrsUtils.moduloSignedPowerOfTwo(np63+2, sp62));
            assertEquals(-p62+3, NbrsUtils.moduloSignedPowerOfTwo(np63+3, sp62));
            assertEquals(-3, NbrsUtils.moduloSignedPowerOfTwo(-p62-3, sp62));
            assertEquals(-2, NbrsUtils.moduloSignedPowerOfTwo(-p62-2, sp62));
            assertEquals(-1, NbrsUtils.moduloSignedPowerOfTwo(-p62-1, sp62));
            //
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(-p62, sp62));
            //
            assertEquals(-p62+1, NbrsUtils.moduloSignedPowerOfTwo(-p62+1, sp62));
            assertEquals(-p62+2, NbrsUtils.moduloSignedPowerOfTwo(-p62+2, sp62));
            assertEquals(-p62+3, NbrsUtils.moduloSignedPowerOfTwo(-p62+3, sp62));
            assertEquals(-3, NbrsUtils.moduloSignedPowerOfTwo(-3, sp62));
            assertEquals(-2, NbrsUtils.moduloSignedPowerOfTwo(-2, sp62));
            assertEquals(-1, NbrsUtils.moduloSignedPowerOfTwo(-1, sp62));
            //
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(0, sp62));
            //
            assertEquals(1, NbrsUtils.moduloSignedPowerOfTwo(1, sp62));
            assertEquals(2, NbrsUtils.moduloSignedPowerOfTwo(2, sp62));
            assertEquals(3, NbrsUtils.moduloSignedPowerOfTwo(3, sp62));
            assertEquals(p62-3, NbrsUtils.moduloSignedPowerOfTwo(p62-3, sp62));
            assertEquals(p62-2, NbrsUtils.moduloSignedPowerOfTwo(p62-2, sp62));
            assertEquals(p62-1, NbrsUtils.moduloSignedPowerOfTwo(p62-1, sp62));
            //
            assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(p62, sp62));
            //
            assertEquals(1, NbrsUtils.moduloSignedPowerOfTwo(p62+1, sp62));
            assertEquals(2, NbrsUtils.moduloSignedPowerOfTwo(p62+2, sp62));
            assertEquals(3, NbrsUtils.moduloSignedPowerOfTwo(p62+3, sp62));
            assertEquals(p62-3, NbrsUtils.moduloSignedPowerOfTwo(p63m1-2, sp62));
            assertEquals(p62-2, NbrsUtils.moduloSignedPowerOfTwo(p63m1-1, sp62));
            assertEquals(p62-1, NbrsUtils.moduloSignedPowerOfTwo(p63m1, sp62));
        }
        
        /*
         * Long.MIN_VALUE power
         * (value never changed, except if Long.MIN_VALUE itself, in which case it gives 0)
         */
        
        assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(np63, np63));
        assertEquals(np63+1, NbrsUtils.moduloSignedPowerOfTwo(np63+1, np63));
        assertEquals(np63+2, NbrsUtils.moduloSignedPowerOfTwo(np63+2, np63));
        assertEquals(np63+3, NbrsUtils.moduloSignedPowerOfTwo(np63+3, np63));
        assertEquals(-p62-3, NbrsUtils.moduloSignedPowerOfTwo(-p62-3, np63));
        assertEquals(-p62-2, NbrsUtils.moduloSignedPowerOfTwo(-p62-2, np63));
        assertEquals(-p62-1, NbrsUtils.moduloSignedPowerOfTwo(-p62-1, np63));
        //
        assertEquals(-p62, NbrsUtils.moduloSignedPowerOfTwo(-p62, np63));
        //
        assertEquals(-p62+1, NbrsUtils.moduloSignedPowerOfTwo(-p62+1, np63));
        assertEquals(-p62+2, NbrsUtils.moduloSignedPowerOfTwo(-p62+2, np63));
        assertEquals(-p62+3, NbrsUtils.moduloSignedPowerOfTwo(-p62+3, np63));
        assertEquals(-3, NbrsUtils.moduloSignedPowerOfTwo(-3, np63));
        assertEquals(-2, NbrsUtils.moduloSignedPowerOfTwo(-2, np63));
        assertEquals(-1, NbrsUtils.moduloSignedPowerOfTwo(-1, np63));
        //
        assertEquals(0, NbrsUtils.moduloSignedPowerOfTwo(0, np63));
        //
        assertEquals(1, NbrsUtils.moduloSignedPowerOfTwo(1, np63));
        assertEquals(2, NbrsUtils.moduloSignedPowerOfTwo(2, np63));
        assertEquals(3, NbrsUtils.moduloSignedPowerOfTwo(3, np63));
        assertEquals(p62-3, NbrsUtils.moduloSignedPowerOfTwo(p62-3, np63));
        assertEquals(p62-2, NbrsUtils.moduloSignedPowerOfTwo(p62-2, np63));
        assertEquals(p62-1, NbrsUtils.moduloSignedPowerOfTwo(p62-1, np63));
        //
        assertEquals(p62, NbrsUtils.moduloSignedPowerOfTwo(p62, np63));
        //
        assertEquals(p62+1, NbrsUtils.moduloSignedPowerOfTwo(p62+1, np63));
        assertEquals(p62+2, NbrsUtils.moduloSignedPowerOfTwo(p62+2, np63));
        assertEquals(p62+3, NbrsUtils.moduloSignedPowerOfTwo(p62+3, np63));
        assertEquals(p63m1-2, NbrsUtils.moduloSignedPowerOfTwo(p63m1-2, np63));
        assertEquals(p63m1-1, NbrsUtils.moduloSignedPowerOfTwo(p63m1-1, np63));
        assertEquals(p63m1, NbrsUtils.moduloSignedPowerOfTwo(p63m1, np63));
    }

    public void test_log2_int() {
        for (int value : new int[]{Integer.MIN_VALUE,0}) {
            try {
                NbrsUtils.log2(value);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        for (int p=0;p<=30;p++) {
            int pot = (1<<p);
            
            if (p != 0) {
                assertEquals(p-1, NbrsUtils.log2(pot-1));
            }
            assertEquals(p, NbrsUtils.log2(pot));
            assertEquals(p, NbrsUtils.log2(pot+pot-1));
            if (p != 30) {
                assertEquals(p+1, NbrsUtils.log2(pot+pot));
            }
        }
    }
    
    public void test_log2_long() {
        for (long value : new long[]{Long.MIN_VALUE,0}) {
            try {
                NbrsUtils.log2(value);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int p=0;p<=62;p++) {
            long pot = (1L<<p);
            
            if (p != 0) {
                assertEquals(p-1, NbrsUtils.log2(pot-1));
            }
            assertEquals(p, NbrsUtils.log2(pot));
            assertEquals(p, NbrsUtils.log2(pot+pot-1));
            if (p != 62) {
                assertEquals(p+1, NbrsUtils.log2(pot+pot));
            }
        }
    }

    public void test_abs_int() {
        for (int value : new int[]{
                Integer.MIN_VALUE,
                Integer.MIN_VALUE+1,
                -1,
                0,
                1,
                Integer.MAX_VALUE-1,
                Integer.MAX_VALUE
        }) {
            assertEquals(Math.abs(value), NbrsUtils.abs(value));
        }
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = this.utils.randomIntWhatever();
            assertEquals(Math.abs(value), NbrsUtils.abs(value));
        }
    }

    public void test_abs_long() {
        for (long value : new long[]{
                Long.MIN_VALUE,
                Long.MIN_VALUE+1,
                -1L,
                0L,
                1L,
                Long.MAX_VALUE-1,
                Long.MAX_VALUE
        }) {
            assertEquals(Math.abs(value), NbrsUtils.abs(value));
        }
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = this.utils.randomLongWhatever();
            assertEquals(Math.abs(value), NbrsUtils.abs(value));
        }
    }

    public void test_absNeg_int() {
        for (int value : new int[]{
                Integer.MIN_VALUE,
                Integer.MIN_VALUE+1,
                -1,
                0,
                1,
                Integer.MAX_VALUE-1,
                Integer.MAX_VALUE
        }) {
            assertEquals(-Math.abs(value), NbrsUtils.absNeg(value));
        }
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = this.utils.randomIntWhatever();
            assertEquals(-Math.abs(value), NbrsUtils.absNeg(value));
        }
    }

    public void test_absNeg_long() {
        for (long value : new long[]{
                Long.MIN_VALUE,
                Long.MIN_VALUE+1,
                -1L,
                0L,
                1L,
                Long.MAX_VALUE-1,
                Long.MAX_VALUE
        }) {
            assertEquals(-Math.abs(value), NbrsUtils.absNeg(value));
        }
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = this.utils.randomLongWhatever();
            assertEquals(-Math.abs(value), NbrsUtils.absNeg(value));
        }
    }

    public void test_intHash_long() {
        
        /*
         * preserving value if in int range
         */
        
        long[] valuesInIntRange = new long[]{
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE+1, Integer.MAX_VALUE-1,
                Integer.MIN_VALUE+2, Integer.MAX_VALUE-2,
                -37,37,
                -4,4,
                -3,3,
                -2,2,
                -1,1,
                0};
        for (int i = 0; i < valuesInIntRange.length; i++) {
            long value = valuesInIntRange[i];
            assertEquals(value, NbrsUtils.intHash(value));
        }
        
        /*
         * hashing if out of int range
         */
        
        assertNotSame("hash", Long.MIN_VALUE, NbrsUtils.intHash(Long.MIN_VALUE));
        
        assertNotSame("hash", Long.MAX_VALUE, NbrsUtils.intHash(Long.MAX_VALUE));
        assertNotSame("hash", -Long.MAX_VALUE, NbrsUtils.intHash(-Long.MAX_VALUE));
        
        assertNotSame("hash", Long.MAX_VALUE-1, NbrsUtils.intHash(Long.MAX_VALUE-1));
        assertNotSame("hash", -(Long.MAX_VALUE-1), NbrsUtils.intHash(-(Long.MAX_VALUE-1)));
    }
    
    /*
     * 
     */
    
    public void test_asByte_short() {
        final short[] badArr = new short[]{
                Short.MIN_VALUE,
                (short)(Byte.MIN_VALUE-1),
                (short)(Byte.MAX_VALUE+1),
                Short.MAX_VALUE,
        };
        for (short value : badArr) {
            try {
                NbrsUtils.asByte(value);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        final short[] goodArr = new short[]{
                (short)Byte.MIN_VALUE,
                (short)(Byte.MIN_VALUE+1),
                -2, -1, 0, 1, 2,
                (short)(Byte.MAX_VALUE-1),
                (short)Byte.MAX_VALUE,
        };
        for (short value : goodArr) {
            assertEquals(value, NbrsUtils.asByte(value));
        }
    }

    public void test_asByte_int() {
        final int[] badArr = new int[]{
                Integer.MIN_VALUE,
                (int)(Byte.MIN_VALUE-1),
                (int)(Byte.MAX_VALUE+1),
                Integer.MAX_VALUE,
        };
        for (int value : badArr) {
            try {
                NbrsUtils.asByte(value);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        final int[] goodArr = new int[]{
                (int)Byte.MIN_VALUE,
                (int)(Byte.MIN_VALUE+1),
                -2, -1, 0, 1, 2,
                (int)(Byte.MAX_VALUE-1),
                (int)Byte.MAX_VALUE,
        };
        for (int value : goodArr) {
            assertEquals(value, NbrsUtils.asByte(value));
        }
    }

    public void test_asByte_long() {
        final long[] badArr = new long[]{
                Long.MIN_VALUE,
                (long)(Byte.MIN_VALUE-1),
                (long)(Byte.MAX_VALUE+1),
                Long.MAX_VALUE,
        };
        for (long value : badArr) {
            try {
                NbrsUtils.asByte(value);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        final long[] goodArr = new long[]{
                (long)Byte.MIN_VALUE,
                (long)(Byte.MIN_VALUE+1),
                -2L, -1L, 0L, 1L, 2L,
                (long)(Byte.MAX_VALUE-1),
                (long)Byte.MAX_VALUE,
        };
        for (long value : goodArr) {
            assertEquals(value, NbrsUtils.asByte(value));
        }
    }

    public void test_asShort_int() {
        final int[] badArr = new int[]{
                Integer.MIN_VALUE,
                (int)(Short.MIN_VALUE-1),
                (int)(Short.MAX_VALUE+1),
                Integer.MAX_VALUE,
        };
        for (int value : badArr) {
            try {
                NbrsUtils.asShort(value);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        final int[] goodArr = new int[]{
                (int)Short.MIN_VALUE,
                (int)(Short.MIN_VALUE+1),
                -2, -1, 0, 1, 2,
                (int)(Short.MAX_VALUE-1),
                (int)Short.MAX_VALUE,
        };
        for (int value : goodArr) {
            assertEquals(value, NbrsUtils.asShort(value));
        }
    }

    public void test_asShort_long() {
        final long[] badArr = new long[]{
                Long.MIN_VALUE,
                (long)(Short.MIN_VALUE-1L),
                (long)(Short.MAX_VALUE+1L),
                Long.MAX_VALUE,
        };
        for (long value : badArr) {
            try {
                NbrsUtils.asShort(value);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        final long[] goodArr = new long[]{
                (long)Short.MIN_VALUE,
                (long)(Short.MIN_VALUE+1L),
                -2L, -1L, 0L, 1L, 2L,
                (long)(Short.MAX_VALUE-1L),
                (long)Short.MAX_VALUE,
        };
        for (long value : goodArr) {
            assertEquals(value, NbrsUtils.asShort(value));
        }
    }

    public void test_asInt_long() {
        final long[] badArr = new long[]{
                Long.MIN_VALUE,
                (long)(Integer.MIN_VALUE-1L),
                (long)(Integer.MAX_VALUE+1L),
                Long.MAX_VALUE,
        };
        for (long value : badArr) {
            try {
                NbrsUtils.asInt(value);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        final long[] goodArr = new long[]{
                (long)Integer.MIN_VALUE,
                (long)(Integer.MIN_VALUE+1L),
                -2L, -1L, 0L, 1L, 2L,
                (long)(Integer.MAX_VALUE-1L),
                (long)Integer.MAX_VALUE,
        };
        for (long value : goodArr) {
            assertEquals(value, NbrsUtils.asInt(value));
        }
    }

    /*
     * 
     */
    
    public void test_toByte_short() {
        final short[] valueArr = new short[]{
                Short.MIN_VALUE,
                (short)(Byte.MIN_VALUE-1),
                (short)Byte.MIN_VALUE,
                (short)(Byte.MIN_VALUE+1),
                -2, -1, 0, 1, 2,
                (short)(Byte.MAX_VALUE-1),
                (short)Byte.MAX_VALUE,
                (short)(Byte.MAX_VALUE+1),
                Short.MAX_VALUE,
        };
        for (short value : valueArr) {
            final short expected = (short) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, value));
            assertEquals(expected, NbrsUtils.toByte(value));
        }
    }

    public void test_toByte_int() {
        final int[] valueArr = new int[]{
                Integer.MIN_VALUE,
                (int)(Byte.MIN_VALUE-1),
                (int)Byte.MIN_VALUE,
                (int)(Byte.MIN_VALUE+1),
                -2, -1, 0, 1, 2,
                (int)(Byte.MAX_VALUE-1),
                (int)Byte.MAX_VALUE,
                (int)(Byte.MAX_VALUE+1),
                Integer.MAX_VALUE,
        };
        for (int value : valueArr) {
            final int expected = Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, value));
            assertEquals(expected, NbrsUtils.toByte(value));
        }
    }

    public void test_toByte_long() {
        final long[] valueArr = new long[]{
                Long.MIN_VALUE,
                (long)(Byte.MIN_VALUE-1L),
                (long)Byte.MIN_VALUE,
                (long)(Byte.MIN_VALUE+1L),
                -2L, -1L, 0L, 1L, 2L,
                (long)(Byte.MAX_VALUE-1L),
                (long)Byte.MAX_VALUE,
                (long)(Byte.MAX_VALUE+1L),
                Long.MAX_VALUE,
        };
        for (long value : valueArr) {
            final long expected = Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, value));
            assertEquals(expected, NbrsUtils.toByte(value));
        }
    }

    public void test_toShort_int() {
        final int[] valueArr = new int[]{
                Integer.MIN_VALUE,
                (int)(Short.MIN_VALUE-1),
                (int)Short.MIN_VALUE,
                (int)(Short.MIN_VALUE+1),
                -2, -1, 0, 1, 2,
                (int)(Short.MAX_VALUE-1),
                (int)Short.MAX_VALUE,
                (int)(Short.MAX_VALUE+1),
                Integer.MAX_VALUE,
        };
        for (int value : valueArr) {
            final int expected = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
            assertEquals(expected, NbrsUtils.toShort(value));
        }
    }

    public void test_toShort_long() {
        final long[] valueArr = new long[]{
                Long.MIN_VALUE,
                (long)(Short.MIN_VALUE-1L),
                (long)Short.MIN_VALUE,
                (long)(Short.MIN_VALUE+1L),
                -2L, -1L, 0L, 1L, 2L,
                (long)(Short.MAX_VALUE-1L),
                (long)Short.MAX_VALUE,
                (long)(Short.MAX_VALUE+1L),
                Long.MAX_VALUE,
        };
        for (long value : valueArr) {
            final long expected = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
            assertEquals(expected, NbrsUtils.toShort(value));
        }
    }

    public void test_toInt_long() {
        final long[] valueArr = new long[]{
                Long.MIN_VALUE,
                (long)(Integer.MIN_VALUE-1L),
                (long)Integer.MIN_VALUE,
                (long)(Integer.MIN_VALUE+1L),
                -2L, -1L, 0L, 1L, 2L,
                (long)(Integer.MAX_VALUE-1L),
                (long)Integer.MAX_VALUE,
                (long)(Integer.MAX_VALUE+1L),
                Long.MAX_VALUE,
        };
        for (long value : valueArr) {
            final long expected = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
            assertEquals(expected, NbrsUtils.toInt(value));
        }
    }

    /*
     * 
     */
    
    public void test_plusExact_2int() {
        for (int b : new int[]{-1,Integer.MIN_VALUE}) {
            try {
                NbrsUtils.plusExact(Integer.MIN_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        for (int b : new int[]{1,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.plusExact(Integer.MAX_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(Integer.MIN_VALUE, NbrsUtils.plusExact(Integer.MIN_VALUE,0));
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.plusExact(Integer.MIN_VALUE,1));
        assertEquals(Integer.MIN_VALUE+2, NbrsUtils.plusExact(Integer.MIN_VALUE,2));
        assertEquals(Integer.MIN_VALUE+3, NbrsUtils.plusExact(Integer.MIN_VALUE,3));
        assertEquals(-4, NbrsUtils.plusExact(Integer.MIN_VALUE,Integer.MAX_VALUE-3));
        assertEquals(-3, NbrsUtils.plusExact(Integer.MIN_VALUE,Integer.MAX_VALUE-2));
        assertEquals(-2, NbrsUtils.plusExact(Integer.MIN_VALUE,Integer.MAX_VALUE-1));
        assertEquals(-1, NbrsUtils.plusExact(Integer.MIN_VALUE,Integer.MAX_VALUE));
        for (int a = -100; a <= 100; a++) {
            for (int b = -100; b <= 100; b++) {
                assertEquals(a+b, NbrsUtils.plusExact(a,b));
            }
        }
        assertEquals(-1, NbrsUtils.plusExact(Integer.MAX_VALUE,Integer.MIN_VALUE));
        assertEquals(0, NbrsUtils.plusExact(Integer.MAX_VALUE,Integer.MIN_VALUE+1));
        assertEquals(1, NbrsUtils.plusExact(Integer.MAX_VALUE,Integer.MIN_VALUE+2));
        assertEquals(2, NbrsUtils.plusExact(Integer.MAX_VALUE,Integer.MIN_VALUE+3));
        assertEquals(Integer.MAX_VALUE-3, NbrsUtils.plusExact(Integer.MAX_VALUE,-3));
        assertEquals(Integer.MAX_VALUE-2, NbrsUtils.plusExact(Integer.MAX_VALUE,-2));
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.plusExact(Integer.MAX_VALUE,-1));
        assertEquals(Integer.MAX_VALUE, NbrsUtils.plusExact(Integer.MAX_VALUE,0));
    }

    public void test_plusExact_2long() {
        for (long b : new long[]{-1,Long.MIN_VALUE}) {
            try {
                NbrsUtils.plusExact(Long.MIN_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        for (long b : new long[]{1,Long.MAX_VALUE}) {
            try {
                NbrsUtils.plusExact(Long.MAX_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(Long.MIN_VALUE, NbrsUtils.plusExact(Long.MIN_VALUE,0));
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.plusExact(Long.MIN_VALUE,1));
        assertEquals(Long.MIN_VALUE+2, NbrsUtils.plusExact(Long.MIN_VALUE,2));
        assertEquals(Long.MIN_VALUE+3, NbrsUtils.plusExact(Long.MIN_VALUE,3));
        assertEquals(-4, NbrsUtils.plusExact(Long.MIN_VALUE,Long.MAX_VALUE-3));
        assertEquals(-3, NbrsUtils.plusExact(Long.MIN_VALUE,Long.MAX_VALUE-2));
        assertEquals(-2, NbrsUtils.plusExact(Long.MIN_VALUE,Long.MAX_VALUE-1));
        assertEquals(-1, NbrsUtils.plusExact(Long.MIN_VALUE,Long.MAX_VALUE));
        for (long a=-100;a<=100;a++) {
            for (long b=-100;b<=100;b++) {
                assertEquals(a+b, NbrsUtils.plusExact(a,b));
            }
        }
        assertEquals(-1, NbrsUtils.plusExact(Long.MAX_VALUE,Long.MIN_VALUE));
        assertEquals(0, NbrsUtils.plusExact(Long.MAX_VALUE,Long.MIN_VALUE+1));
        assertEquals(1, NbrsUtils.plusExact(Long.MAX_VALUE,Long.MIN_VALUE+2));
        assertEquals(2, NbrsUtils.plusExact(Long.MAX_VALUE,Long.MIN_VALUE+3));
        assertEquals(Long.MAX_VALUE-3, NbrsUtils.plusExact(Long.MAX_VALUE,-3));
        assertEquals(Long.MAX_VALUE-2, NbrsUtils.plusExact(Long.MAX_VALUE,-2));
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.plusExact(Long.MAX_VALUE,-1));
        assertEquals(Long.MAX_VALUE, NbrsUtils.plusExact(Long.MAX_VALUE,0));
    }

    public void test_plusBounded_2int() {
        assertEquals(Integer.MIN_VALUE, NbrsUtils.plusBounded(Integer.MIN_VALUE,Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, NbrsUtils.plusBounded(Integer.MIN_VALUE,-1));
        //
        assertEquals(Integer.MIN_VALUE, NbrsUtils.plusBounded(Integer.MIN_VALUE,0));
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.plusBounded(Integer.MIN_VALUE,1));
        assertEquals(Integer.MIN_VALUE+2, NbrsUtils.plusBounded(Integer.MIN_VALUE,2));
        assertEquals(Integer.MIN_VALUE+3, NbrsUtils.plusBounded(Integer.MIN_VALUE,3));
        assertEquals(-4, NbrsUtils.plusBounded(Integer.MIN_VALUE,Integer.MAX_VALUE-3));
        assertEquals(-3, NbrsUtils.plusBounded(Integer.MIN_VALUE,Integer.MAX_VALUE-2));
        assertEquals(-2, NbrsUtils.plusBounded(Integer.MIN_VALUE,Integer.MAX_VALUE-1));
        assertEquals(-1, NbrsUtils.plusBounded(Integer.MIN_VALUE,Integer.MAX_VALUE));
        for (int a = -100; a <= 100; a++) {
            for (int b = -100; b <= 100; b++) {
                assertEquals(a+b, NbrsUtils.plusBounded(a,b));
            }
        }
        assertEquals(-1, NbrsUtils.plusBounded(Integer.MAX_VALUE,Integer.MIN_VALUE));
        assertEquals(0, NbrsUtils.plusBounded(Integer.MAX_VALUE,Integer.MIN_VALUE+1));
        assertEquals(1, NbrsUtils.plusBounded(Integer.MAX_VALUE,Integer.MIN_VALUE+2));
        assertEquals(2, NbrsUtils.plusBounded(Integer.MAX_VALUE,Integer.MIN_VALUE+3));
        assertEquals(Integer.MAX_VALUE-3, NbrsUtils.plusBounded(Integer.MAX_VALUE,-3));
        assertEquals(Integer.MAX_VALUE-2, NbrsUtils.plusBounded(Integer.MAX_VALUE,-2));
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.plusBounded(Integer.MAX_VALUE,-1));
        assertEquals(Integer.MAX_VALUE, NbrsUtils.plusBounded(Integer.MAX_VALUE,0));
        //
        assertEquals(Integer.MAX_VALUE, NbrsUtils.plusBounded(Integer.MAX_VALUE,1));
        assertEquals(Integer.MAX_VALUE, NbrsUtils.plusBounded(Integer.MAX_VALUE,Integer.MAX_VALUE));
    }

    public void test_plusBounded_2long() {
        assertEquals(Long.MIN_VALUE, NbrsUtils.plusBounded(Long.MIN_VALUE,Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, NbrsUtils.plusBounded(Long.MIN_VALUE,-1));
        //
        assertEquals(Long.MIN_VALUE, NbrsUtils.plusBounded(Long.MIN_VALUE,0));
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.plusBounded(Long.MIN_VALUE,1));
        assertEquals(Long.MIN_VALUE+2, NbrsUtils.plusBounded(Long.MIN_VALUE,2));
        assertEquals(Long.MIN_VALUE+3, NbrsUtils.plusBounded(Long.MIN_VALUE,3));
        assertEquals(-4, NbrsUtils.plusBounded(Long.MIN_VALUE,Long.MAX_VALUE-3));
        assertEquals(-3, NbrsUtils.plusBounded(Long.MIN_VALUE,Long.MAX_VALUE-2));
        assertEquals(-2, NbrsUtils.plusBounded(Long.MIN_VALUE,Long.MAX_VALUE-1));
        assertEquals(-1, NbrsUtils.plusBounded(Long.MIN_VALUE,Long.MAX_VALUE));
        for (long a=-100;a<=100;a++) {
            for (long b=-100;b<=100;b++) {
                assertEquals(a+b, NbrsUtils.plusBounded(a,b));
            }
        }
        assertEquals(-1, NbrsUtils.plusBounded(Long.MAX_VALUE,Long.MIN_VALUE));
        assertEquals(0, NbrsUtils.plusBounded(Long.MAX_VALUE,Long.MIN_VALUE+1));
        assertEquals(1, NbrsUtils.plusBounded(Long.MAX_VALUE,Long.MIN_VALUE+2));
        assertEquals(2, NbrsUtils.plusBounded(Long.MAX_VALUE,Long.MIN_VALUE+3));
        assertEquals(Long.MAX_VALUE-3, NbrsUtils.plusBounded(Long.MAX_VALUE,-3));
        assertEquals(Long.MAX_VALUE-2, NbrsUtils.plusBounded(Long.MAX_VALUE,-2));
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.plusBounded(Long.MAX_VALUE,-1));
        assertEquals(Long.MAX_VALUE, NbrsUtils.plusBounded(Long.MAX_VALUE,0));
        //
        assertEquals(Long.MAX_VALUE, NbrsUtils.plusBounded(Long.MAX_VALUE,1));
        assertEquals(Long.MAX_VALUE, NbrsUtils.plusBounded(Long.MAX_VALUE,Long.MAX_VALUE));
    }

    public void test_minusExact_2int() {
        for (int b : new int[]{1,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.minusExact(Integer.MIN_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        for (int b : new int[]{-1,Integer.MIN_VALUE}) {
            try {
                NbrsUtils.minusExact(Integer.MAX_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(Integer.MIN_VALUE, NbrsUtils.minusExact(Integer.MIN_VALUE,0));
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.minusExact(Integer.MIN_VALUE,-1));
        assertEquals(Integer.MIN_VALUE+2, NbrsUtils.minusExact(Integer.MIN_VALUE,-2));
        assertEquals(Integer.MIN_VALUE+3, NbrsUtils.minusExact(Integer.MIN_VALUE,-3));
        assertEquals(-3, NbrsUtils.minusExact(Integer.MIN_VALUE,Integer.MIN_VALUE+3));
        assertEquals(-2, NbrsUtils.minusExact(Integer.MIN_VALUE,Integer.MIN_VALUE+2));
        assertEquals(-1, NbrsUtils.minusExact(Integer.MIN_VALUE,Integer.MIN_VALUE+1));
        assertEquals(0, NbrsUtils.minusExact(Integer.MIN_VALUE,Integer.MIN_VALUE));
        for (int a = -100; a <= 100; a++) {
            for (int b = -100; b <= 100; b++) {
                assertEquals(a-b, NbrsUtils.minusExact(a,b));
            }
        }
        assertEquals(0, NbrsUtils.minusExact(Integer.MAX_VALUE,Integer.MAX_VALUE));
        assertEquals(1, NbrsUtils.minusExact(Integer.MAX_VALUE,Integer.MAX_VALUE-1));
        assertEquals(2, NbrsUtils.minusExact(Integer.MAX_VALUE,Integer.MAX_VALUE-2));
        assertEquals(3, NbrsUtils.minusExact(Integer.MAX_VALUE,Integer.MAX_VALUE-3));
        assertEquals(Integer.MAX_VALUE-3, NbrsUtils.minusExact(Integer.MAX_VALUE,3));
        assertEquals(Integer.MAX_VALUE-2, NbrsUtils.minusExact(Integer.MAX_VALUE,2));
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.minusExact(Integer.MAX_VALUE,1));
        assertEquals(Integer.MAX_VALUE, NbrsUtils.minusExact(Integer.MAX_VALUE,0));
    }

    public void test_minusExact_2long() {
        for (long b : new long[]{1,Long.MAX_VALUE}) {
            try {
                NbrsUtils.minusExact(Long.MIN_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        for (long b : new long[]{-1,Long.MIN_VALUE}) {
            try {
                NbrsUtils.minusExact(Long.MAX_VALUE,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(Long.MIN_VALUE, NbrsUtils.minusExact(Long.MIN_VALUE,0));
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.minusExact(Long.MIN_VALUE,-1));
        assertEquals(Long.MIN_VALUE+2, NbrsUtils.minusExact(Long.MIN_VALUE,-2));
        assertEquals(Long.MIN_VALUE+3, NbrsUtils.minusExact(Long.MIN_VALUE,-3));
        assertEquals(-3, NbrsUtils.minusExact(Long.MIN_VALUE,Long.MIN_VALUE+3));
        assertEquals(-2, NbrsUtils.minusExact(Long.MIN_VALUE,Long.MIN_VALUE+2));
        assertEquals(-1, NbrsUtils.minusExact(Long.MIN_VALUE,Long.MIN_VALUE+1));
        assertEquals(0, NbrsUtils.minusExact(Long.MIN_VALUE,Long.MIN_VALUE));
        for (long a=-100;a<=100;a++) {
            for (long b=-100;b<=100;b++) {
                assertEquals(a-b, NbrsUtils.minusExact(a,b));
            }
        }
        assertEquals(0, NbrsUtils.minusExact(Long.MAX_VALUE,Long.MAX_VALUE));
        assertEquals(1, NbrsUtils.minusExact(Long.MAX_VALUE,Long.MAX_VALUE-1));
        assertEquals(2, NbrsUtils.minusExact(Long.MAX_VALUE,Long.MAX_VALUE-2));
        assertEquals(3, NbrsUtils.minusExact(Long.MAX_VALUE,Long.MAX_VALUE-3));
        assertEquals(Long.MAX_VALUE-3, NbrsUtils.minusExact(Long.MAX_VALUE,3));
        assertEquals(Long.MAX_VALUE-2, NbrsUtils.minusExact(Long.MAX_VALUE,2));
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.minusExact(Long.MAX_VALUE,1));
        assertEquals(Long.MAX_VALUE, NbrsUtils.minusExact(Long.MAX_VALUE,0));
    }
    
    public void test_minusBounded_2int() {
        assertEquals(Integer.MIN_VALUE, NbrsUtils.minusBounded(Integer.MIN_VALUE,Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, NbrsUtils.minusBounded(Integer.MIN_VALUE,1));
        //
        assertEquals(Integer.MIN_VALUE, NbrsUtils.minusBounded(Integer.MIN_VALUE,0));
        assertEquals(Integer.MIN_VALUE+1, NbrsUtils.minusBounded(Integer.MIN_VALUE,-1));
        assertEquals(Integer.MIN_VALUE+2, NbrsUtils.minusBounded(Integer.MIN_VALUE,-2));
        assertEquals(Integer.MIN_VALUE+3, NbrsUtils.minusBounded(Integer.MIN_VALUE,-3));
        assertEquals(-3, NbrsUtils.minusBounded(Integer.MIN_VALUE,Integer.MIN_VALUE+3));
        assertEquals(-2, NbrsUtils.minusBounded(Integer.MIN_VALUE,Integer.MIN_VALUE+2));
        assertEquals(-1, NbrsUtils.minusBounded(Integer.MIN_VALUE,Integer.MIN_VALUE+1));
        assertEquals(0, NbrsUtils.minusBounded(Integer.MIN_VALUE,Integer.MIN_VALUE));
        for (int a = -100; a <= 100; a++) {
            for (int b = -100; b <= 100; b++) {
                assertEquals(a-b, NbrsUtils.minusBounded(a,b));
            }
        }
        assertEquals(0, NbrsUtils.minusBounded(Integer.MAX_VALUE,Integer.MAX_VALUE));
        assertEquals(1, NbrsUtils.minusBounded(Integer.MAX_VALUE,Integer.MAX_VALUE-1));
        assertEquals(2, NbrsUtils.minusBounded(Integer.MAX_VALUE,Integer.MAX_VALUE-2));
        assertEquals(3, NbrsUtils.minusBounded(Integer.MAX_VALUE,Integer.MAX_VALUE-3));
        assertEquals(Integer.MAX_VALUE-3, NbrsUtils.minusBounded(Integer.MAX_VALUE,3));
        assertEquals(Integer.MAX_VALUE-2, NbrsUtils.minusBounded(Integer.MAX_VALUE,2));
        assertEquals(Integer.MAX_VALUE-1, NbrsUtils.minusBounded(Integer.MAX_VALUE,1));
        assertEquals(Integer.MAX_VALUE, NbrsUtils.minusBounded(Integer.MAX_VALUE,0));
        //
        assertEquals(Integer.MAX_VALUE, NbrsUtils.minusBounded(Integer.MAX_VALUE,-1));
        assertEquals(Integer.MAX_VALUE, NbrsUtils.minusBounded(Integer.MAX_VALUE,Integer.MIN_VALUE));
    }

    public void test_minusBounded_2long() {
        assertEquals(Long.MIN_VALUE, NbrsUtils.minusBounded(Long.MIN_VALUE,Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, NbrsUtils.minusBounded(Long.MIN_VALUE,1));
        //
        assertEquals(Long.MIN_VALUE, NbrsUtils.minusBounded(Long.MIN_VALUE,0));
        assertEquals(Long.MIN_VALUE+1, NbrsUtils.minusBounded(Long.MIN_VALUE,-1));
        assertEquals(Long.MIN_VALUE+2, NbrsUtils.minusBounded(Long.MIN_VALUE,-2));
        assertEquals(Long.MIN_VALUE+3, NbrsUtils.minusBounded(Long.MIN_VALUE,-3));
        assertEquals(-3, NbrsUtils.minusBounded(Long.MIN_VALUE,Long.MIN_VALUE+3));
        assertEquals(-2, NbrsUtils.minusBounded(Long.MIN_VALUE,Long.MIN_VALUE+2));
        assertEquals(-1, NbrsUtils.minusBounded(Long.MIN_VALUE,Long.MIN_VALUE+1));
        assertEquals(0, NbrsUtils.minusBounded(Long.MIN_VALUE,Long.MIN_VALUE));
        for (long a=-100;a<=100;a++) {
            for (long b=-100;b<=100;b++) {
                assertEquals(a-b, NbrsUtils.minusBounded(a,b));
            }
        }
        assertEquals(0, NbrsUtils.minusBounded(Long.MAX_VALUE,Long.MAX_VALUE));
        assertEquals(1, NbrsUtils.minusBounded(Long.MAX_VALUE,Long.MAX_VALUE-1));
        assertEquals(2, NbrsUtils.minusBounded(Long.MAX_VALUE,Long.MAX_VALUE-2));
        assertEquals(3, NbrsUtils.minusBounded(Long.MAX_VALUE,Long.MAX_VALUE-3));
        assertEquals(Long.MAX_VALUE-3, NbrsUtils.minusBounded(Long.MAX_VALUE,3));
        assertEquals(Long.MAX_VALUE-2, NbrsUtils.minusBounded(Long.MAX_VALUE,2));
        assertEquals(Long.MAX_VALUE-1, NbrsUtils.minusBounded(Long.MAX_VALUE,1));
        assertEquals(Long.MAX_VALUE, NbrsUtils.minusBounded(Long.MAX_VALUE,0));
        //
        assertEquals(Long.MAX_VALUE, NbrsUtils.minusBounded(Long.MAX_VALUE,-1));
        assertEquals(Long.MAX_VALUE, NbrsUtils.minusBounded(Long.MAX_VALUE,Long.MIN_VALUE));
    }

    public void test_timesExact_2int() {
        final int bitSize = 32;
        
        for (long[] factors : newInRangeFactors(bitSize)) {
            final int a = NbrsUtils.asInt(factors[0]);
            final int b = NbrsUtils.asInt(factors[1]);
            assertEquals(a*b, NbrsUtils.timesExact(a,b));
        }

        for (long[] factors : newAboveRangeFactors(bitSize)) {
            final int a = NbrsUtils.asInt(factors[0]);
            final int b = NbrsUtils.asInt(factors[1]);
            try {
                NbrsUtils.timesExact(a,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }

        for (long[] factors : newBelowRangeFactors(bitSize)) {
            final int a = NbrsUtils.asInt(factors[0]);
            final int b = NbrsUtils.asInt(factors[1]);
            try {
                NbrsUtils.timesExact(a,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
    }

    public void test_timesExact_2long() {
        final int bitSize = 64;
        
        for (long[] factors : newInRangeFactors(bitSize)) {
            final long a = factors[0];
            final long b = factors[1];
            assertEquals(a*b, NbrsUtils.timesExact(a,b));
        }

        for (long[] factors : newAboveRangeFactors(bitSize)) {
            final long a = factors[0];
            final long b = factors[1];
            try {
                NbrsUtils.timesExact(a,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }

        for (long[] factors : newBelowRangeFactors(bitSize)) {
            final long a = factors[0];
            final long b = factors[1];
            try {
                NbrsUtils.timesExact(a,b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
    }
    
    public void test_timesBounded_2int() {
        final int bitSize = 32;
        
        for (long[] factors : newInRangeFactors(bitSize)) {
            final int a = NbrsUtils.asInt(factors[0]);
            final int b = NbrsUtils.asInt(factors[1]);
            assertEquals(a*b, NbrsUtils.timesBounded(a,b));
        }

        for (long[] factors : newAboveRangeFactors(bitSize)) {
            final int a = NbrsUtils.asInt(factors[0]);
            final int b = NbrsUtils.asInt(factors[1]);
            assertEquals(Integer.MAX_VALUE, NbrsUtils.timesBounded(a,b));
        }

        for (long[] factors : newBelowRangeFactors(bitSize)) {
            final int a = NbrsUtils.asInt(factors[0]);
            final int b = NbrsUtils.asInt(factors[1]);
            assertEquals(Integer.MIN_VALUE, NbrsUtils.timesBounded(a,b));
        }
    }

    public void test_timesBounded_2long() {
        final int bitSize = 64;
        
        for (long[] factors : newInRangeFactors(bitSize)) {
            final long a = factors[0];
            final long b = factors[1];
            assertEquals(a*b, NbrsUtils.timesBounded(a,b));
        }

        for (long[] factors : newAboveRangeFactors(bitSize)) {
            final long a = factors[0];
            final long b = factors[1];
            assertEquals(Long.MAX_VALUE, NbrsUtils.timesBounded(a,b));
        }

        for (long[] factors : newBelowRangeFactors(bitSize)) {
            final long a = factors[0];
            final long b = factors[1];
            assertEquals(Long.MIN_VALUE, NbrsUtils.timesBounded(a,b));
        }
    }
    
    /*
     * close values
     */
    
    public void test_round_float() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            float value = randomFloatWhatever();
            int ref;
            if (NbrsUtils.isMathematicalInteger(value) || NbrsUtils.isNaNOrInfinite(value)) {
                ref = (int)value; // exact, or closest int, or 0 if NaN
            } else {
                boolean neg = (value < 0);
                int lowerMag = (int)value;
                float postCommaPart = value - lowerMag;
                if (neg) {
                    ref = (postCommaPart < -0.5f) ? lowerMag-1 : lowerMag;
                } else {
                    ref = (postCommaPart < 0.5f) ? lowerMag : lowerMag+1;
                }
            }
            int res = NbrsUtils.round(value);
            boolean ok = (ref == res);
            if (!ok) {
                printCallerName();
                System.out.println("value = " + value);
                System.out.println("ref = " + ref);
                System.out.println("res = " + res);
            }
            assertTrue(ok);
        }
    }

    public void test_round_double() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            double value = randomDoubleWhatever();
            long ref;
            if (NbrsUtils.isMathematicalInteger(value) || NbrsUtils.isNaNOrInfinite(value)) {
                ref = (long)value; // exact, or closest int, or 0 if NaN
            } else {
                boolean neg = (value < 0);
                long lowerMag = (long)value;
                double postCommaPart = value - lowerMag;
                if (neg) {
                    ref = (postCommaPart < -0.5) ? lowerMag-1 : lowerMag;
                } else {
                    ref = (postCommaPart < 0.5) ? lowerMag : lowerMag+1;
                }
            }
            long res = NbrsUtils.round(value);
            boolean ok = (ref == res);
            if (!ok) {
                printCallerName();
                System.out.println("value = " + value);
                System.out.println("ref = " + ref);
                System.out.println("res = " + res);
            }
            assertTrue(ok);
        }
    }

    public void test_roundEven_float() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            float value = randomFloatWhatever();
            int ref;
            if (NbrsUtils.isMathematicalInteger(value) || NbrsUtils.isNaNOrInfinite(value)) {
                ref = (int)value; // exact, or closest int, or 0 if NaN
            } else {
                boolean neg = (value < 0);
                int lowerMag = (int)value;
                if (NbrsUtils.isEquidistant(value)) {
                    ref = (((lowerMag&1) == 0) ? lowerMag : lowerMag + (neg ? -1 : 1));
                } else {
                    float postCommaPart = value - lowerMag;
                    if (neg) {
                        ref = (postCommaPart < -0.5f) ? lowerMag-1 : lowerMag;
                    } else {
                        ref = (postCommaPart < 0.5f) ? lowerMag : lowerMag+1;
                    }
                }
            }
            int res = NbrsUtils.roundEven(value);
            boolean ok = (ref == res);
            if (!ok) {
                printCallerName();
                System.out.println("value = " + value);
                System.out.println("ref = " + ref);
                System.out.println("res = " + res);
            }
            assertTrue(ok);
        }
    }

    public void test_roundEven_double() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            double value = randomDoubleWhatever();
            long ref;
            if (NbrsUtils.isMathematicalInteger(value) || NbrsUtils.isNaNOrInfinite(value)) {
                ref = (long)value; // exact, or closest int, or 0 if NaN
            } else {
                boolean neg = (value < 0);
                long lowerMag = (long)value;
                if (NbrsUtils.isEquidistant(value)) {
                    ref = (((lowerMag&1) == 0) ? lowerMag : lowerMag + (neg ? -1 : 1));
                } else {
                    double postCommaPart = value - lowerMag;
                    if (neg) {
                        ref = (postCommaPart < -0.5) ? lowerMag-1 : lowerMag;
                    } else {
                        ref = (postCommaPart < 0.5) ? lowerMag : lowerMag+1;
                    }
                }
            }
            long res = NbrsUtils.roundEven(value);
            boolean ok = (ref == res);
            if (!ok) {
                printCallerName();
                System.out.println("value = " + value);
                System.out.println("ref = " + ref);
                System.out.println("res = " + res);
            }
            assertTrue(ok);
        }
    }
    
    /*
     * close int values
     */
    
    public void test_floorToInt_double() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            double value = randomDoubleWhatever();
            int ref = (int) Math.floor(value);
            int res = NbrsUtils.floorToInt(value);
            assertEquals(ref, res);
        }
    }
    
    public void test_ceilToInt_double() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            double value = randomDoubleWhatever();
            int ref = (int) Math.ceil(value);
            int res = NbrsUtils.ceilToInt(value);
            assertEquals(ref, res);
        }
    }

    public void test_roundToInt_double() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            double value = randomDoubleWhatever();
            // Assuming our round(double) rounds correctly
            // (for Math one it depends on the JDK version).
            int ref = (int)(double)NbrsUtils.round(value);
            int res = NbrsUtils.roundToInt(value);
            boolean ok = (ref == res);
            if (!ok) {
                printCallerName();
                System.out.println("value = " + value);
                System.out.println("ref = " + ref);
                System.out.println("res = " + res);
            }
            assertTrue(ok);
        }
    }

    public void test_roundEvenToInt_double() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            double value = randomDoubleWhatever();
            // Assuming our roundEven(double) rounds correctly.
            int ref = NbrsUtils.toInt(NbrsUtils.roundEven(value));
            int res = NbrsUtils.roundEvenToInt(value);
            boolean ok = (ref == res);
            if (!ok) {
                printCallerName();
                System.out.println("value = " + value);
                System.out.println("ref = " + ref);
                System.out.println("res = " + res);
            }
            assertTrue(ok);
        }
    }

    /*
     * 
     */
    
    public void test_twoPow(int power) {
        for (int p=-1074;p<=1023;p++) {
            assertEquals(Math.pow(2.0,p), NbrsUtils.twoPow(p));
        }
        assertEquals(Double.NEGATIVE_INFINITY, NbrsUtils.twoPow(-1075));
        assertEquals(Double.POSITIVE_INFINITY, NbrsUtils.twoPow(1024));
    }
    
    public void test_twoPowAsIntExact_int() {
        for (int power : new int[]{Integer.MIN_VALUE, -1, 31, Integer.MAX_VALUE}) {
            try {
                NbrsUtils.twoPowAsIntExact(power);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        for (int power = 0; power <= 30; power++) {
            assertEquals((int)StrictMath.pow(2.0, power), NbrsUtils.twoPowAsIntExact(power));
        }
    }

    public void test_twoPowAsIntBounded_int() {
        for (int power : new int[]{Integer.MIN_VALUE, -1, 31, Integer.MAX_VALUE}) {
            if (power < 0) {
                assertEquals(1, NbrsUtils.twoPowAsIntBounded(power));
            } else {
                assertEquals((1<<30), NbrsUtils.twoPowAsIntBounded(power));
            }
        }
        
        for (int power = 0; power <= 30; power++) {
            assertEquals((int)StrictMath.pow(2.0, power), NbrsUtils.twoPowAsIntBounded(power));
        }
    }

    public void test_twoPowAsLongExact_int() {
        for (int power : new int[]{Integer.MIN_VALUE, -1, 63, Integer.MAX_VALUE}) {
            try {
                NbrsUtils.twoPowAsLongExact(power);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        for (int power = 0; power <= 62; power++) {
            assertEquals((long)StrictMath.pow(2.0, power), NbrsUtils.twoPowAsLongExact(power));
        }
    }

    public void test_twoPowAsLongBounded_int() {
        for (int power : new int[]{Integer.MIN_VALUE, -1, 63, Integer.MAX_VALUE}) {
            if (power < 0) {
                assertEquals(1L, NbrsUtils.twoPowAsLongBounded(power));
            } else {
                assertEquals((1L<<62), NbrsUtils.twoPowAsLongBounded(power));
            }
        }
        
        for (int power = 0; power <= 62; power++) {
            assertEquals((long)StrictMath.pow(2.0, power), NbrsUtils.twoPowAsLongBounded(power));
        }
    }
        
    public void test_pow2_int() {
        ArrayList<Integer> values = new ArrayList<Integer>();
        values.add(0);
        values.add(3);
        values.add(Integer.MIN_VALUE);
        values.add(Integer.MAX_VALUE);
        for (Integer value : values) {
            assertEquals(value * value, NbrsUtils.pow2(value));
        }
    }

    public void test_pow2_long() {
        ArrayList<Long> values = new ArrayList<Long>();
        values.add(0L);
        values.add(3L);
        values.add(Long.MIN_VALUE);
        values.add(Long.MAX_VALUE);
        for (Long value : values) {
            assertEquals(value * value, NbrsUtils.pow2(value));
        }
    }

    public void test_pow2_float() {
        this.test_pow2_float(false);
    }

    public void test_pow2_strict_float() {
        this.test_pow2_float(true);
    }

    public void test_pow2_float(boolean strict) {
        ArrayList<Float> values = new ArrayList<Float>();
        values.add(0.0f);
        values.add(0.1f);
        values.add(Float.MIN_VALUE);
        values.add(NbrsUtils.FLOAT_MIN_NORMAL);
        values.add(Float.MAX_VALUE);
        values.add(Float.NaN);
        values.add(Float.NEGATIVE_INFINITY);
        values.add(Float.POSITIVE_INFINITY);
        for (Float value : values) {
            assertEquals(value * value, (strict ? NbrsUtils.pow2_strict(value) : NbrsUtils.pow2(value)));
        }
    }

    public void test_pow2_double() {
        this.test_pow2_double(false);
    }
    
    public void test_pow2_strict_double() {
        this.test_pow2_double(true);
    }
    
    public void test_pow2_double(boolean strict) {
        ArrayList<Double> values = new ArrayList<Double>();
        values.add(0.0);
        values.add(0.1);
        values.add(Double.MIN_VALUE);
        values.add(NbrsUtils.DOUBLE_MIN_NORMAL);
        values.add(Double.MAX_VALUE);
        values.add(Double.NaN);
        values.add(Double.NEGATIVE_INFINITY);
        values.add(Double.POSITIVE_INFINITY);
        for (Double value : values) {
            assertEquals(value * value, (strict ? NbrsUtils.pow2_strict(value) : NbrsUtils.pow2(value)));
        }
    }

    public void test_pow3_int() {
        ArrayList<Integer> values = new ArrayList<Integer>();
        values.add(0);
        values.add(3);
        values.add(Integer.MIN_VALUE);
        values.add(Integer.MAX_VALUE);
        for (Integer value : values) {
            assertEquals(value * value * value, NbrsUtils.pow3(value));
        }
    }

    public void test_pow3_long() {
        ArrayList<Long> values = new ArrayList<Long>();
        values.add(0L);
        values.add(3L);
        values.add(Long.MIN_VALUE);
        values.add(Long.MAX_VALUE);
        for (Long value : values) {
            assertEquals(value * value * value, NbrsUtils.pow3(value));
        }
    }

    public void test_pow3_float() {
        this.test_pow3_float(false);
    }
    
    public void test_pow3_strict_float() {
        this.test_pow3_float(true);
    }
    
    public void test_pow3_float(boolean strict) {
        ArrayList<Float> values = new ArrayList<Float>();
        values.add(0.0f);
        values.add(0.1f);
        values.add(Float.MIN_VALUE);
        values.add(NbrsUtils.FLOAT_MIN_NORMAL);
        values.add(Float.MAX_VALUE);
        values.add(Float.NaN);
        values.add(Float.NEGATIVE_INFINITY);
        values.add(Float.POSITIVE_INFINITY);
        for (Float value : values) {
            assertEquals(value * value * value, (strict ? NbrsUtils.pow3_strict(value) : NbrsUtils.pow3(value)));
        }
    }

    public void test_pow3_double() {
        this.test_pow3_double(false);
    }
    
    public void test_pow3_strict_double() {
        this.test_pow3_double(true);
    }
    
    public void test_pow3_double(boolean strict) {
        ArrayList<Double> values = new ArrayList<Double>();
        values.add(0.0);
        values.add(0.1);
        values.add(Double.MIN_VALUE);
        values.add(NbrsUtils.DOUBLE_MIN_NORMAL);
        values.add(Double.MAX_VALUE);
        values.add(Double.NaN);
        values.add(Double.NEGATIVE_INFINITY);
        values.add(Double.POSITIVE_INFINITY);
        for (Double value : values) {
            assertEquals(value * value * value, (strict ? NbrsUtils.pow3_strict(value) : NbrsUtils.pow3(value)));
        }
    }
    
    /*
     * 
     */

    public void test_plus2PI_double() {
        this.test_plus2PI_double(false);
    }
    
    public void test_plus2PI_strict_double() {
        this.test_plus2PI_double(true);
    }
    
    public void test_plus2PI_double(boolean strict) {
        {
            // Using StrictMath.sin as a measure of the angle, since
            // around PI we have sin(x) ~= x.
            // 2* because at 2*Math.PI we have twice the ULP of Math.PI.
            final double expected = 2*StrictMath.sin(Math.PI);
            final double value = -2*Math.PI;
            final double actual = (strict ? NbrsUtils.plus2PI_strict(value) : NbrsUtils.plus2PI(value));
            final double relDelta = NbrsTestUtils.relDelta(expected, actual);
            assertEquals(0.0, relDelta, ACCURATE_PI_OP_SIN_EPSILON);
        }
        if (strict) {
            assertEquals(2*Math.PI, NbrsUtils.plus2PI_strict(0.0));
            assertEquals(3*Math.PI, NbrsUtils.plus2PI_strict(Math.PI));
        } else {
            assertEquals(2*Math.PI, NbrsUtils.plus2PI(0.0));
            assertEquals(3*Math.PI, NbrsUtils.plus2PI(Math.PI));
        }
        
        // Testing monotonicity around threshold.
        if (strict) {
            double a = NbrsUtils.plus2PI_strict(Math.nextAfter(-Math.PI, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.plus2PI_strict(-Math.PI);
            double c = NbrsUtils.plus2PI_strict(Math.nextAfter(-Math.PI, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        } else {
            double a = NbrsUtils.plus2PI(Math.nextAfter(-Math.PI, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.plus2PI(-Math.PI);
            double c = NbrsUtils.plus2PI(Math.nextAfter(-Math.PI, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            final double ref = value + 2*Math.PI;
            final double actual = (strict ? NbrsUtils.plus2PI_strict(value) : NbrsUtils.plus2PI(value));
            
            // Testing is close to +2*Math.PI.
            final double minDelta = NbrsTestUtils.minDelta(ref, actual);
            assertEquals(0.0, minDelta, ACCURATE_PI_OP_DEFAULT_EPSILON);
        }
    }

    public void test_minus2PI_double() {
        this.test_minus2PI_double(false);
    }
    
    public void test_minus2PI_strict_double() {
        this.test_minus2PI_double(true);
    }
    
    public void test_minus2PI_double(boolean strict) {
        {
            // Using StrictMath.sin as a measure of the angle, since
            // around PI we have sin(x) ~= x.
            // 2* because at 2*Math.PI we have twice the ULP of Math.PI.
            final double expected = -2*StrictMath.sin(Math.PI);
            final double value = 2*Math.PI;
            final double actual = (strict ? NbrsUtils.minus2PI_strict(value) : NbrsUtils.minus2PI(value));
            final double relDelta = NbrsTestUtils.relDelta(expected, actual);
            assertEquals(0.0, relDelta, ACCURATE_PI_OP_SIN_EPSILON);
        }
        if (strict) {
            assertEquals(-2*Math.PI, NbrsUtils.minus2PI_strict(0.0));
            assertEquals(-3*Math.PI, NbrsUtils.minus2PI_strict(-Math.PI));
        } else {
            assertEquals(-2*Math.PI, NbrsUtils.minus2PI(0.0));
            assertEquals(-3*Math.PI, NbrsUtils.minus2PI(-Math.PI));
        }
        
        // Testing monotonicity around threshold.
        if (strict) {
            double a = NbrsUtils.minus2PI_strict(Math.nextAfter(Math.PI, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.minus2PI_strict(Math.PI);
            double c = NbrsUtils.minus2PI_strict(Math.nextAfter(Math.PI, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        } else {
            double a = NbrsUtils.minus2PI(Math.nextAfter(Math.PI, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.minus2PI(Math.PI);
            double c = NbrsUtils.minus2PI(Math.nextAfter(Math.PI, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            final double ref = value - 2*Math.PI;
            final double actual = (strict ? NbrsUtils.minus2PI_strict(value) : NbrsUtils.minus2PI(value));
            
            // Testing is close to -2*Math.PI.
            final double minDelta = NbrsTestUtils.minDelta(ref, actual);
            assertEquals(0.0, minDelta, ACCURATE_PI_OP_DEFAULT_EPSILON);
        }
    }
    
    public void test_plusPI_double() {
        this.test_plusPI_double(false);
    }
    
    public void test_plusPI_strict_double() {
        this.test_plusPI_double(true);
    }
    
    public void test_plusPI_double(boolean strict) {
        {
            // Using StrictMath.sin as a measure of the angle, since
            // around PI we have sin(x) ~= x.
            final double expected = StrictMath.sin(Math.PI);
            final double value = -Math.PI;
            final double actual = (strict ? NbrsUtils.plusPI_strict(value) : NbrsUtils.plusPI(value));
            final double relDelta = NbrsTestUtils.relDelta(expected, actual);
            assertEquals(0.0, relDelta, ACCURATE_PI_OP_SIN_EPSILON);
        }
        if (strict) {
            assertEquals(Math.PI, NbrsUtils.plusPI_strict(0.0));
            assertEquals(2*Math.PI, NbrsUtils.plusPI_strict(Math.PI));
        } else {
            assertEquals(Math.PI, NbrsUtils.plusPI(0.0));
            assertEquals(2*Math.PI, NbrsUtils.plusPI(Math.PI));
        }
        
        // Testing monotonicity around threshold.
        if (strict) {
            double a = NbrsUtils.plusPI_strict(Math.nextAfter(-Math.PI/2, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.plusPI_strict(-Math.PI/2);
            double c = NbrsUtils.plusPI_strict(Math.nextAfter(-Math.PI/2, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        } else {
            double a = NbrsUtils.plusPI(Math.nextAfter(-Math.PI/2, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.plusPI(-Math.PI/2);
            double c = NbrsUtils.plusPI(Math.nextAfter(-Math.PI/2, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            final double ref = value + Math.PI;
            final double actual = (strict ? NbrsUtils.plusPI_strict(value) : NbrsUtils.plusPI(value));
            
            // Testing is close to +Math.PI.
            final double minDelta = NbrsTestUtils.minDelta(ref, actual);
            assertEquals(0.0, minDelta, ACCURATE_PI_OP_DEFAULT_EPSILON);
        }
    }
    
    public void test_minusPI_double() {
        this.test_minusPI_double(false);
    }
    
    public void test_minusPI_strict_double() {
        this.test_minusPI_double(true);
    }
    
    public void test_minusPI_double(boolean strict) {
        {
            // Using StrictMath.sin as a measure of the angle, since
            // around PI we have sin(x) ~= x.
            final double expected = -StrictMath.sin(Math.PI);
            final double value = Math.PI;
            final double actual = (strict ? NbrsUtils.minusPI_strict(value) : NbrsUtils.minusPI(value));
            final double relDelta = NbrsTestUtils.relDelta(expected, actual);
            assertEquals(0.0, relDelta, ACCURATE_PI_OP_SIN_EPSILON);
        }
        if (strict) {
            assertEquals(-Math.PI, NbrsUtils.minusPI_strict(0.0));
            assertEquals(-2*Math.PI, NbrsUtils.minusPI_strict(-Math.PI));
        } else {
            assertEquals(-Math.PI, NbrsUtils.minusPI(0.0));
            assertEquals(-2*Math.PI, NbrsUtils.minusPI(-Math.PI));
        }
        
        // Testing monotonicity around threshold.
        if (strict) {
            double a = NbrsUtils.minusPI_strict(Math.nextAfter(Math.PI/2, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.minusPI_strict(Math.PI/2);
            double c = NbrsUtils.minusPI_strict(Math.nextAfter(Math.PI/2, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        } else {
            double a = NbrsUtils.minusPI(Math.nextAfter(Math.PI/2, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.minusPI(Math.PI/2);
            double c = NbrsUtils.minusPI(Math.nextAfter(Math.PI/2, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            final double ref = value - Math.PI;
            final double actual = (strict ? NbrsUtils.minusPI_strict(value) : NbrsUtils.minusPI(value));
            
            // Testing is close to -Math.PI.
            final double minDelta = NbrsTestUtils.minDelta(ref, actual);
            assertEquals(0.0, minDelta, ACCURATE_PI_OP_DEFAULT_EPSILON);
        }
    }

    public void test_plusPIO2_double() {
        this.test_plusPIO2_double(false);
    }
    
    public void test_plusPIO2_strict_double() {
        this.test_plusPIO2_double(true);
    }
    
    public void test_plusPIO2_double(boolean strict) {
        {
            // Using StrictMath.sin as a measure of the angle, since
            // around PI we have sin(x) ~= x.
            // 0.5* because at Math.PI/2 we have half the ULP of Math.PI.
            final double expected = 0.5*StrictMath.sin(Math.PI);
            final double value = -Math.PI/2;
            final double actual = (strict ? NbrsUtils.plusPIO2_strict(value) : NbrsUtils.plusPIO2(value));
            final double relDelta = NbrsTestUtils.relDelta(expected, actual);
            assertEquals(0.0, relDelta, ACCURATE_PI_OP_SIN_EPSILON);
        }
        if (strict) {
            assertEquals(Math.PI/2, NbrsUtils.plusPIO2_strict(0.0));
            assertEquals(Math.PI, NbrsUtils.plusPIO2_strict(Math.PI/2));
        } else {
            assertEquals(Math.PI/2, NbrsUtils.plusPIO2(0.0));
            assertEquals(Math.PI, NbrsUtils.plusPIO2(Math.PI/2));
        }
        
        // Testing monotonicity around threshold.
        if (strict) {
            double a = NbrsUtils.plusPIO2_strict(Math.nextAfter(-Math.PI/4, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.plusPIO2_strict(-Math.PI/4);
            double c = NbrsUtils.plusPIO2_strict(Math.nextAfter(-Math.PI/4, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        } else {
            double a = NbrsUtils.plusPIO2(Math.nextAfter(-Math.PI/4, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.plusPIO2(-Math.PI/4);
            double c = NbrsUtils.plusPIO2(Math.nextAfter(-Math.PI/4, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            final double ref = value + Math.PI/2;
            final double actual = (strict ? NbrsUtils.plusPIO2_strict(value) : NbrsUtils.plusPIO2(value));
            
            // Testing is close to +Math.PI/2.
            final double minDelta = NbrsTestUtils.minDelta(ref, actual);
            assertEquals(0.0, minDelta, ACCURATE_PI_OP_DEFAULT_EPSILON);
        }
    }
    
    public void test_minusPIO2_double() {
        this.test_minusPIO2_double(false);
    }
    
    public void test_minusPIO2_strict_double() {
        this.test_minusPIO2_double(true);
    }
    
    public void test_minusPIO2_double(boolean strict) {
        {
            // Using StrictMath.sin as a measure of the angle, since
            // around PI we have sin(x) ~= x.
            // 0.5* because at Math.PI/2 we have half the ULP of Math.PI.
            final double expected = -0.5*StrictMath.sin(Math.PI);
            final double value = Math.PI/2;
            final double actual = (strict ? NbrsUtils.minusPIO2_strict(value) : NbrsUtils.minusPIO2(value));
            final double relDelta = NbrsTestUtils.relDelta(expected, actual);
            assertEquals(0.0, relDelta, ACCURATE_PI_OP_SIN_EPSILON);
        }
        if (strict) {
            assertEquals(-Math.PI/2, NbrsUtils.minusPIO2_strict(0.0));
            assertEquals(-Math.PI, NbrsUtils.minusPIO2_strict(-Math.PI/2));
        } else {
            assertEquals(-Math.PI/2, NbrsUtils.minusPIO2(0.0));
            assertEquals(-Math.PI, NbrsUtils.minusPIO2(-Math.PI/2));
        }
        
        // Testing monotonicity around threshold.
        if (strict) {
            double a = NbrsUtils.minusPIO2_strict(Math.nextAfter(Math.PI/4, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.minusPIO2_strict(Math.PI/4);
            double c = NbrsUtils.minusPIO2_strict(Math.nextAfter(Math.PI/4, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        } else {
            double a = NbrsUtils.minusPIO2(Math.nextAfter(Math.PI/4, Double.NEGATIVE_INFINITY));
            double b = NbrsUtils.minusPIO2(Math.PI/4);
            double c = NbrsUtils.minusPIO2(Math.nextAfter(Math.PI/4, Double.POSITIVE_INFINITY));
            assertTrue(a <= b);
            assertTrue(b <= c);
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            final double ref = value - Math.PI/2;
            final double actual = (strict ? NbrsUtils.minusPIO2_strict(value) : NbrsUtils.minusPIO2(value));
            
            // Testing is close to -Math.PI/2.
            final double minDelta = NbrsTestUtils.minDelta(ref, actual);
            assertEquals(0.0, minDelta, ACCURATE_PI_OP_DEFAULT_EPSILON);
        }
    }

    /*
     * 
     */
    
    public void test_checkRadix_int() {
        for (int radix : new int[]{Integer.MIN_VALUE,1,37,Integer.MAX_VALUE}) {
            try {
                NbrsUtils.checkRadix(radix);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int radix : new int[]{2,36}) {
            assertTrue(NbrsUtils.checkRadix(radix));
        }
    }

    public void test_computeNbrOfChars_2int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = random.nextInt();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int expected = Integer.toString(value, radix).length();
            assertEquals(expected,NbrsUtils.computeNbrOfChars(value, radix));
        }
    }
    
    public void test_computeNbrOfChars_long_int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = random.nextLong();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int expected = Long.toString(value, radix).length();
            assertEquals(expected,NbrsUtils.computeNbrOfChars(value, radix));
        }
    }
    
    public void test_computeNbrOfChars_3int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = random.nextInt();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int nbrOfChars = Integer.toString(value, radix).length();
            final int oneIfNeg = ((value < 0) ? 1 : 0);
            final int nbrOfDigits = nbrOfChars - oneIfNeg;
            final int paddingUpTo = nbrOfDigits + (10-random.nextInt(20));
            final int expected = oneIfNeg + Math.max(nbrOfDigits, paddingUpTo);
            assertEquals(expected,NbrsUtils.computeNbrOfChars(value, radix, paddingUpTo));
        }
    }
    
    public void test_computeNbrOfChars_long_2int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = random.nextLong();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int nbrOfChars = Long.toString(value, radix).length();
            final int oneIfNeg = ((value < 0) ? 1 : 0);
            final int nbrOfDigits = nbrOfChars - oneIfNeg;
            final int paddingUpTo = nbrOfDigits + (10-random.nextInt(20));
            final int expected = oneIfNeg + Math.max(nbrOfDigits, paddingUpTo);
            assertEquals(expected,NbrsUtils.computeNbrOfChars(value, radix, paddingUpTo));
        }
    }
    
    public void test_computeNbrOfDigits_2int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = random.nextInt();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int nbrOfChars = Integer.toString(value, radix).length();
            final int oneIfNeg = ((value < 0) ? 1 : 0);
            final int expected = nbrOfChars - oneIfNeg;
            assertEquals(expected,NbrsUtils.computeNbrOfDigits(value, radix));
        }
    }
    
    public void test_computeNbrOfDigits_long_int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = random.nextLong();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int nbrOfChars = Long.toString(value, radix).length();
            final int oneIfNeg = ((value < 0) ? 1 : 0);
            final int expected = nbrOfChars - oneIfNeg;
            assertEquals(expected,NbrsUtils.computeNbrOfDigits(value, radix));
        }
    }
    
    public void test_computeNbrOfDigits_3int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = random.nextInt();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int nbrOfChars = Integer.toString(value, radix).length();
            final int oneIfNeg = ((value < 0) ? 1 : 0);
            final int nbrOfDigits = nbrOfChars - oneIfNeg;
            final int paddingUpTo = nbrOfDigits + (10-random.nextInt(20));
            final int expected = Math.max(nbrOfDigits, paddingUpTo);
            assertEquals(expected,NbrsUtils.computeNbrOfDigits(value, radix, paddingUpTo));
        }
    }

    public void test_computeNbrOfDigits_long_2int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = random.nextLong();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final int nbrOfChars = Long.toString(value, radix).length();
            final int oneIfNeg = ((value < 0) ? 1 : 0);
            final int nbrOfDigits = nbrOfChars - oneIfNeg;
            final int paddingUpTo = nbrOfDigits + (10-random.nextInt(20));
            final int expected = Math.max(nbrOfDigits, paddingUpTo);
            assertEquals(expected,NbrsUtils.computeNbrOfDigits(value, radix, paddingUpTo));
        }
    }
    
    /*
     * 
     */

    public void test_toString_int() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = random.nextInt();
            final String expected = Integer.toString(value);
            assertEquals(expected,NbrsUtils.toString(value));
        }
    }

    public void test_toString_long() {
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = random.nextLong();
            final String expected = Long.toString(value);
            assertEquals(expected,NbrsUtils.toString(value));
        }
    }

    public void test_toString_2int() {
        for (int radix=Character.MIN_RADIX;radix<=Character.MAX_RADIX;radix++) {
            assertEquals(Integer.toString(Integer.MIN_VALUE, radix).toUpperCase(),NbrsUtils.toString(Integer.MIN_VALUE, radix));
            assertEquals(Integer.toString(Integer.MAX_VALUE, radix).toUpperCase(),NbrsUtils.toString(Integer.MAX_VALUE, radix));
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final int value = random.nextInt();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final String expected = Integer.toString(value, radix).toUpperCase();
            assertEquals(expected,NbrsUtils.toString(value, radix));
        }
    }

    public void test_toString_long_int() {
        for (int radix=Character.MIN_RADIX;radix<=Character.MAX_RADIX;radix++) {
            assertEquals(Long.toString(Long.MIN_VALUE, radix).toUpperCase(),NbrsUtils.toString(Long.MIN_VALUE, radix));
            assertEquals(Long.toString(Long.MAX_VALUE, radix).toUpperCase(),NbrsUtils.toString(Long.MAX_VALUE, radix));
        }
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final long value = random.nextLong();
            final int radix = Character.MIN_RADIX + random.nextInt(Character.MAX_RADIX-Character.MIN_RADIX+1);
            final String expected = Long.toString(value, radix).toUpperCase();
            assertEquals(expected,NbrsUtils.toString(value, radix));
        }
    }

    public void test_toString_3int() {
        
        assertEquals("0", NbrsUtils.toString(0, 16, 0));
        assertEquals("0F", NbrsUtils.toString(0xF, 16, 2));
        assertEquals("-000FF", NbrsUtils.toString(-0xFF, 16, 5));

        /*
         * 
         */
        
        for (int value : new int[]{
                Integer.MIN_VALUE,
                Integer.MIN_VALUE/2,
                Integer.MIN_VALUE/3,
                -123456789,
                -1,
                0,
                1,
                123456789,
                Integer.MAX_VALUE/3,
                Integer.MAX_VALUE/2,
                Integer.MAX_VALUE}) {
            for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
                for (int paddingUpTo : PADDING_UP_TO_ARR) {
                    if (DEBUG) {
                        System.out.println();
                        System.out.println("value = " + value);
                        System.out.println("radix = " + radix);
                        System.out.println("paddingUpTo = " + paddingUpTo);
                    }
                    final String expected = ref_toString(value, radix, paddingUpTo);
                    final String actual = NbrsUtils.toString(value, radix, paddingUpTo);
                    assertEquals(expected, actual);
                }
            }
        }
    }

    public void test_toString_long_2int() {

        assertEquals("0", NbrsUtils.toString(0L, 16, 0));
        assertEquals("0F", NbrsUtils.toString(0xFL, 16, 2));
        assertEquals("-000FF", NbrsUtils.toString(-0xFFL, 16, 5));

        /*
         * 
         */
        
        for (long value : new long[]{
                Long.MIN_VALUE,
                Long.MIN_VALUE/2,
                Long.MIN_VALUE/3,
                -123456789L,
                -1L,
                0L,
                1L,
                123456789L,
                Long.MAX_VALUE/3,
                Long.MAX_VALUE/2,
                Long.MAX_VALUE}) {
            for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
                for (int paddingUpTo : PADDING_UP_TO_ARR) {
                    if (DEBUG) {
                        System.out.println();
                        System.out.println("value = " + value);
                        System.out.println("radix = " + radix);
                        System.out.println("paddingUpTo = " + paddingUpTo);
                    }
                    final String expected = ref_toString(value, radix, paddingUpTo);
                    final String actual = NbrsUtils.toString(value, radix, paddingUpTo);
                    assertEquals(expected, actual);
                }
            }
        }
    }

    /*
     * 
     */
    
    public void test_checkBitPositionsByte_2int() {
        final int maxBisPos = 8;
        
        for (int[] range : new int[][]{{0,maxBisPos+1},{-1,maxBisPos},{-1,maxBisPos+1},{5,4}}) {
            try {
                NbrsUtils.checkBitPositionsByte(range[0], range[1]);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int[] range : new int[][]{{0, 0},{0,maxBisPos},{maxBisPos,maxBisPos}}) {
            assertTrue(NbrsUtils.checkBitPositionsByte(range[0], range[1]));
        }
    }
    
    public void test_checkBitPositionsShort_2int() {
        final int maxBisPos = 16;
        
        for (int[] range : new int[][]{{0,maxBisPos+1},{-1,maxBisPos},{-1,maxBisPos+1},{5,4}}) {
            try {
                NbrsUtils.checkBitPositionsShort(range[0], range[1]);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int[] range : new int[][]{{0, 0},{0,maxBisPos},{maxBisPos,maxBisPos}}) {
            assertTrue(NbrsUtils.checkBitPositionsShort(range[0], range[1]));
        }
    }
    
    public void test_checkBitPositionsInt_2int() {
        final int maxBisPos = 32;
        
        for (int[] range : new int[][]{{0,maxBisPos+1},{-1,maxBisPos},{-1,maxBisPos+1},{5,4}}) {
            try {
                NbrsUtils.checkBitPositionsInt(range[0], range[1]);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int[] range : new int[][]{{0, 0},{0,maxBisPos},{maxBisPos,maxBisPos}}) {
            assertTrue(NbrsUtils.checkBitPositionsInt(range[0], range[1]));
        }
    }
    
    public void test_checkBitPositionsLong_2int() {
        final int maxBisPos = 64;
        
        for (int[] range : new int[][]{{0,maxBisPos+1},{-1,maxBisPos},{-1,maxBisPos+1},{5,4}}) {
            try {
                NbrsUtils.checkBitPositionsLong(range[0], range[1]);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        for (int[] range : new int[][]{{0, 0},{0,maxBisPos},{maxBisPos,maxBisPos}}) {
            assertTrue(NbrsUtils.checkBitPositionsLong(range[0], range[1]));
        }
    }
    
    /*
     * 
     */
    
    public void test_toStringBits_byte() {
        assertEquals("00010010",NbrsUtils.toStringBits((byte)0x12));
        assertEquals("11110010",NbrsUtils.toStringBits((byte)0xF2));
    }
    
    public void test_toStringBits_short() {
        assertEquals("0001001000110100",NbrsUtils.toStringBits((short)0x1234));
        assertEquals("1111001000110100",NbrsUtils.toStringBits((short)0xF234));
    }
    
    public void test_toStringBits_int() {
        assertEquals("00010010001101000101011001111000",NbrsUtils.toStringBits(0x12345678));
        assertEquals("11110010001101000101011001111000",NbrsUtils.toStringBits(0xF2345678));
    }
    
    public void test_toStringBits_long() {
        assertEquals("0001001000110100010101100111100000010011010101110010010001101000",NbrsUtils.toStringBits(0x1234567813572468L));
        assertEquals("1111001000110100010101100111100000010011010101110010010001101000",NbrsUtils.toStringBits(0xF234567813572468L));
    }
    
    /*
     * 
     */
    
    public void test_toStringHex_byte() {
        assertEquals("0x00",NbrsUtils.toStringHex((byte)0x00));
        assertEquals("0x01",NbrsUtils.toStringHex((byte)0x01));
        assertEquals("0x80",NbrsUtils.toStringHex((byte)0x80));
        //
        assertEquals("0x02",NbrsUtils.toStringHex((byte)0x02));
        assertEquals("0x10",NbrsUtils.toStringHex((byte)0x10));
        //
        assertEquals("0x12",NbrsUtils.toStringHex((byte)0x12));
        assertEquals("0xF2",NbrsUtils.toStringHex((byte)0xF2));
    }
    
    public void test_toStringHex_short() {
        assertEquals("0x0000",NbrsUtils.toStringHex((short)0x0000));
        assertEquals("0x0001",NbrsUtils.toStringHex((short)0x0001));
        assertEquals("0x8000",NbrsUtils.toStringHex((short)0x8000));
        assertEquals("0xFFFF",NbrsUtils.toStringHex((short)0xFFFF));
        //
        assertEquals("0x0034",NbrsUtils.toStringHex((short)0x0034));
        assertEquals("0x0230",NbrsUtils.toStringHex((short)0x0230));
        assertEquals("0x1200",NbrsUtils.toStringHex((short)0x1200));
        //
        assertEquals("0x1234",NbrsUtils.toStringHex((short)0x1234));
        assertEquals("0xF234",NbrsUtils.toStringHex((short)0xF234));
    }
    
    public void test_toStringHex_int() {
        assertEquals("0x00000000",NbrsUtils.toStringHex(0x00000000));
        assertEquals("0x00000001",NbrsUtils.toStringHex(0x00000001));
        assertEquals("0x80000000",NbrsUtils.toStringHex(0x80000000));
        assertEquals("0xFFFFFFFF",NbrsUtils.toStringHex(0xFFFFFFFF));
        //
        assertEquals("0x00005678",NbrsUtils.toStringHex(0x00005678));
        assertEquals("0x00345600",NbrsUtils.toStringHex(0x00345600));
        assertEquals("0x12340000",NbrsUtils.toStringHex(0x12340000));
        //
        assertEquals("0x12345678",NbrsUtils.toStringHex(0x12345678));
        assertEquals("0xF2345678",NbrsUtils.toStringHex(0xF2345678));
    }
    
    public void test_toStringHex_long() {
        assertEquals("0x0000000000000000",NbrsUtils.toStringHex(0x0000000000000000L));
        assertEquals("0x0000000000000001",NbrsUtils.toStringHex(0x0000000000000001L));
        assertEquals("0x8000000000000000",NbrsUtils.toStringHex(0x8000000000000000L));
        assertEquals("0xFFFFFFFFFFFFFFFF",NbrsUtils.toStringHex(0xFFFFFFFFFFFFFFFFL));
        //
        assertEquals("0x0000000013572468",NbrsUtils.toStringHex(0x0000000013572468L));
        assertEquals("0x0000567813570000",NbrsUtils.toStringHex(0x0000567813570000L));
        assertEquals("0x1234567800000000",NbrsUtils.toStringHex(0x1234567800000000L));
        //
        assertEquals("0x1234567813572468",NbrsUtils.toStringHex(0x1234567813572468L));
        assertEquals("0xF234567813572468",NbrsUtils.toStringHex(0xF234567813572468L));
    }
    
    /*
     * 
     */

    public void test_toStringBits_byte_2int_2boolean() {
        final byte bits = (byte)0x12;
        final int maxBitPosExcl = 8;
        @SuppressWarnings("unused")
        int first;
        @SuppressWarnings("unused")
        int lastExcl;
        @SuppressWarnings("unused")
        boolean bigEndian;
        @SuppressWarnings("unused")
        boolean padding;
        /*
         * ko
         */
        for (int[] range : new int[][]{{0,maxBitPosExcl+1},{-1,maxBitPosExcl},{-1,maxBitPosExcl+1},{5,4}}) {
            try {
                NbrsUtils.toStringBits(bits, first = range[0], lastExcl = range[1], bigEndian = true, padding = false);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        /*
         * big endian
         */
        assertEquals("00010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = true));
        assertEquals("00010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = false));
        assertEquals("__01001_",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = true));
        assertEquals("01001",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = false));
        /*
         * little endian
         */
        assertEquals("01001000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = true));
        assertEquals("01001000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = false));
        assertEquals("_10010__",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = true));
        assertEquals("10010",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = false));
    }

    public void test_toStringBits_short_2int_2boolean() {
        final short bits = (short)0x12;
        final int maxBitPosExcl = 16;
        @SuppressWarnings("unused")
        int first;
        @SuppressWarnings("unused")
        int lastExcl;
        @SuppressWarnings("unused")
        boolean bigEndian;
        @SuppressWarnings("unused")
        boolean padding;
        /*
         * ko
         */
        for (int[] range : new int[][]{{0,maxBitPosExcl+1},{-1,maxBitPosExcl},{-1,maxBitPosExcl+1},{5,4}}) {
            try {
                NbrsUtils.toStringBits(bits, first = range[0], lastExcl = range[1], bigEndian = true, padding = false);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        /*
         * big endian
         */
        assertEquals("0000000000010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = true));
        assertEquals("0000000000010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = false));
        assertEquals("__0000000001001_",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = true));
        assertEquals("0000000001001",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = false));
        /*
         * little endian
         */
        assertEquals("0100100000000000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = true));
        assertEquals("0100100000000000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = false));
        assertEquals("_1001000000000__",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = true));
        assertEquals("1001000000000",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = false));
    }

    public void test_toStringBits_int_2int_2boolean() {
        final int bits = (int)0x12;
        final int maxBitPosExcl = 32;
        @SuppressWarnings("unused")
        int first;
        @SuppressWarnings("unused")
        int lastExcl;
        @SuppressWarnings("unused")
        boolean bigEndian;
        @SuppressWarnings("unused")
        boolean padding;
        /*
         * ko
         */
        for (int[] range : new int[][]{{0,maxBitPosExcl+1},{-1,maxBitPosExcl},{-1,maxBitPosExcl+1},{5,4}}) {
            try {
                NbrsUtils.toStringBits(bits, first = range[0], lastExcl = range[1], bigEndian = true, padding = false);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        /*
         * big endian
         */
        assertEquals("00000000000000000000000000010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = true));
        assertEquals("00000000000000000000000000010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = false));
        assertEquals("__00000000000000000000000001001_",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = true));
        assertEquals("00000000000000000000000001001",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = false));
        /*
         * little endian
         */
        assertEquals("01001000000000000000000000000000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = true));
        assertEquals("01001000000000000000000000000000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = false));
        assertEquals("_10010000000000000000000000000__",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = true));
        assertEquals("10010000000000000000000000000",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = false));
    }

    public void test_toStringBits_long_2int_2boolean() {
        final long bits = (long)0x12;
        final int maxBitPosExcl = 64;
        @SuppressWarnings("unused")
        int first;
        @SuppressWarnings("unused")
        int lastExcl;
        @SuppressWarnings("unused")
        boolean bigEndian;
        @SuppressWarnings("unused")
        boolean padding;
        /*
         * ko
         */
        for (int[] range : new int[][]{{0,maxBitPosExcl+1},{-1,maxBitPosExcl},{-1,maxBitPosExcl+1},{5,4}}) {
            try {
                NbrsUtils.toStringBits(bits, first = range[0], lastExcl = range[1], bigEndian = true, padding = false);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        /*
         * big endian
         */
        assertEquals("0000000000000000000000000000000000000000000000000000000000010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = true));
        assertEquals("0000000000000000000000000000000000000000000000000000000000010010",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = true, padding = false));
        assertEquals("__0000000000000000000000000000000000000000000000000000000001001_",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = true));
        assertEquals("0000000000000000000000000000000000000000000000000000000001001",NbrsUtils.toStringBits(bits, first = 2, lastExcl = maxBitPosExcl-1, bigEndian = true, padding = false));
        /*
         * little endian
         */
        assertEquals("0100100000000000000000000000000000000000000000000000000000000000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = true));
        assertEquals("0100100000000000000000000000000000000000000000000000000000000000",NbrsUtils.toStringBits(bits, first = 0, lastExcl = maxBitPosExcl, bigEndian = false, padding = false));
        assertEquals("_1001000000000000000000000000000000000000000000000000000000000__",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = true));
        assertEquals("1001000000000000000000000000000000000000000000000000000000000",NbrsUtils.toStringBits(bits, first = 1, lastExcl = maxBitPosExcl-2, bigEndian = false, padding = false));
    }
    
    /*
     * 
     */
    
    public void test_toStringCSN_double() {
        
        /*
         * Testing that bounds are correctly handled
         * (errors like < instead of <= could make it not so).
         */
        
        assertEquals("1.0E-3",NbrsUtils.toStringCSN(NbrsUtils.NO_CSN_MIN_BOUND_INCL));
        assertEquals("1.0E7",NbrsUtils.toStringCSN(NbrsUtils.NO_CSN_MAX_BOUND_EXCL));

        /*
         * zeros
         */

        assertEquals("0.0E0",NbrsUtils.toStringCSN(0.0));
        assertEquals("-0.0E0",NbrsUtils.toStringCSN(-0.0));

        /*
         * 
         */
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            
            final String resString = NbrsUtils.toStringCSN(value);
            final boolean quickCheckOK =
                (resString.contains("E") || Double.isNaN(value) || Double.isInfinite(value))
                && (!resString.startsWith("."))
                && (!resString.contains(".E"));
            if (!quickCheckOK) {
                System.out.println("value =     " + value);
                System.out.println("resString = " + resString);
            }
            assertTrue(quickCheckOK);
            
            final double resValue = Double.parseDouble(resString);
            if (!NbrsUtils.equal(value, resValue)) {
                System.out.println("value =     " + value);
                System.out.println("resString = " + resString);
                System.out.println("resValue =  " + resValue);
            }
            assertEquals(value, resValue);
        }
    }
    
    public void test_toStringNoCSN_double() {
        
        /*
         * Testing that bounds are correctly handled
         * (errors like < instead of <= could make it not so).
         */
        
        // Does not have bug 4428022 (does not return "0.0010").
        assertEquals("0.001",NbrsUtils.toStringNoCSN(NbrsUtils.NO_CSN_MIN_BOUND_INCL));
        assertEquals("10000000.0",NbrsUtils.toStringNoCSN(NbrsUtils.NO_CSN_MAX_BOUND_EXCL));
        
        /*
         * zeros
         */

        assertEquals("0.0",NbrsUtils.toStringNoCSN(0.0));
        assertEquals("-0.0",NbrsUtils.toStringNoCSN(-0.0));

        /*
         * 
         */
        
        for (int i = 0; i < NBR_OF_VALUES_SMALL; i++) {
            final double value = this.utils.randomDoubleWhatever();
            
            final String resString = NbrsUtils.toStringNoCSN(value);
            final boolean quickCheckOK =
                (!resString.contains("E"))
                && (!resString.startsWith("."))
                && (!resString.endsWith("."));
            if (!quickCheckOK) {
                System.out.println("value =     " + value);
                System.out.println("resString = " + resString);
            }
            assertTrue(quickCheckOK);
            
            final double resValue = Double.parseDouble(resString);
            if (!NbrsUtils.equal(value, resValue)) {
                System.out.println("value =     " + value);
                System.out.println("resString = " + resString);
                System.out.println("resValue =  " + resValue);
            }
            assertEquals(value, resValue);
        }
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For tests and benches.
     */
    static String ref_toString(int value, int radix, int paddingUpTo) {
        String str = Integer.toString(value, radix);
        str = upperCased(str, radix);
        str = NbrsUtils.leftPaddedWithZero(str, paddingUpTo);
        return str;
    }

    /**
     * For tests and benches.
     */
    static String ref_toString(long value, int radix, int paddingUpTo) {
        String str = Long.toString(value, radix);
        str = upperCased(str, radix);
        str = NbrsUtils.leftPaddedWithZero(str, paddingUpTo);
        return str;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static String upperCased(String str, int radix) {
        if (radix >= 11) {
            boolean foundLowerCase = false;
            for (int i = 0; i < str.length(); i++) {
                final char c = str.charAt(i);
                if ((c >= 'a') && (c <= 'z')) {
                    foundLowerCase = true;
                    break;
                }
            }
            
            if (foundLowerCase) {
                str = str.toUpperCase();
            }
        }

        return str;
    }

    /*
     * 
     */
    
    private static void printCallerName() {
        printCallerMethodName(1,null);
    }
    
    /**
     * Beware that depending on whether methods are private, etc.,
     * intermediary method calls such as to "access$0" might be done
     * in-between method calls that are explicit in the code.
     * 
     * Not private, to avoid accessor call.
     * 
     * @param depth 0 for caller method name, 1 for caller's caller name, etc.
     */
    private static void printCallerMethodName(int depth, String suffix) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        final String callerName = ste[2+depth].getMethodName();
        System.out.println();
        System.out.println(callerName + "()" + ((suffix == null) ? "" : " " + suffix));
    }
    
    /*
     * 
     */
    
    private float randomFloatWhatever() {
        return this.utils.randomFloatWhatever();
    }

    private double randomDoubleWhatever() {
        return this.utils.randomDoubleWhatever();
    }

    /**
     * Useful not to each time test both foo(a,b) and foo(b,a):
     * foo(a,b) suffices.
     * 
     * @return An array twice as long as the specified array,
     *         containing original pairs and permuted pairs
     *         (i.e. {b,a} for each {a,b} pair).
     */
    private static long[][] originalAndPermuted(long[][] pairs) {
        final long[][] result = new long[2*pairs.length][];
        for (int i = 0; i < pairs.length; i++) {
            final long[] pair = pairs[i];
            result[i] = pair;
            result[i+pairs.length] = new long[]{pair[1],pair[0]};
        }
        return result;
    }

    private static long mid(int bitSize) {
        // Need even number of bits for ou mid trick.
        if ((bitSize < 8) || (bitSize > 64) || ((bitSize&1) != 0)) {
            throw new IllegalArgumentException();
        }
        // Mean magnitude (to test special cases around these magnitudes),
        // as an even number such as
        // 2 * mid*mid - 1
        // = 2 * (1L<<(bitSize-2)) - 1
        // = (1L<<(bitSize-1)) - 1
        // = max
        return (1L<<((bitSize/2)-1));
    }
    
    /**
     * @return Array of factors which product is in range
     *         for a signed integer type with the specified number of bits.
     */
    private static long[][] newInRangeFactors(int bitSize) {
        if ((bitSize < 8) || (bitSize > 64)) {
            throw new IllegalArgumentException();
        }
        final long min = (Long.MIN_VALUE>>(64-bitSize));
        final long max = (Long.MAX_VALUE>>(64-bitSize));
        final long mid = mid(bitSize);
        final long[][] pairs = new long[][]{
                // -hi, +lo
                {min,1},
                {min/2,2},
                {min/3,3},
                // -hi, -lo
                {(min+1),-1},
                {(min+1)/2,-2},
                {(min+1)/3,-3},
                // -hi, 0
                {min,0},
                // +hi, 0
                {max,0},
                // +hi, +lo
                {max,1},
                {max/2,2},
                {max/3,3},
                // +hi, -lo
                {max,-1},
                {max/2,-2},
                {max/3,-3},
                // mid (+,+), product < max
                {2*mid,mid-1},
                {2*mid-1,mid},
                {2*mid-1,mid-1},
                // mid (-,-), product < max
                {-2*mid,-mid+1},
                {-2*mid+1,-mid},
                {-2*mid+1,-mid+1},
                // mid (+,-), product = min
                {2*mid,-mid},
                {4*mid,-mid/2},
                // mid (-,+), product = min
                {-2*mid,mid},
                {-4*mid,mid/2}};
        return originalAndPermuted(pairs);
    }

    /**
     * @return Array of factors which product is above range
     *         for a signed integer type with the specified number of bits.
     */
    private static long[][] newAboveRangeFactors(int bitSize) {
        if ((bitSize < 8) || (bitSize > 64)) {
            throw new IllegalArgumentException();
        }
        final long min = (Long.MIN_VALUE>>(64-bitSize));
        final long max = (Long.MAX_VALUE>>(64-bitSize));
        final long mid = mid(bitSize);
        final long[][] pairs = new long[][]{
                // -hi, -lo
                {min,-1},
                {min/2,-2},
                // -hi, -hi
                {min,min},
                // +hi, +hi
                {max,max},
                // +hi, +lo
                {max,2},
                {max/2,3},
                {max/3,4},
                // mid (+,+), product > max
                {2*mid,mid+1},
                {2*mid+1,mid},
                {2*mid+1,mid+1},
                // mid (-,-), product > max
                {-2*mid,-mid-1},
                {-2*mid-1,-mid},
                {-2*mid-1,-mid-1}};
        return originalAndPermuted(pairs);
    }

    /**
     * @return Array of factors which product is below range
     *         for a signed integer type with the specified number of bits.
     */
    private static long[][] newBelowRangeFactors(int bitSize) {
        if ((bitSize < 8) || (bitSize > 64)) {
            throw new IllegalArgumentException();
        }
        final long min = (Long.MIN_VALUE>>(64-bitSize));
        final long max = (Long.MAX_VALUE>>(64-bitSize));
        final long mid = mid(bitSize);
        final long[][] pairs = new long[][]{
                // -hi, +lo
                {min,2},
                {min/2,3},
                {min/3,4},
                // -hi, +hi
                {min,max},
                // +hi, -lo
                {max,-2},
                {max/2,-3},
                {max/3,-4},
                // mid (+,-), product < min
                {2*mid,-mid-1},
                {2*mid+1,-mid},
                {2*mid+1,-mid-1},
                // mid (-,+), product < min
                {-2*mid,mid+1},
                {-2*mid-1,mid},
                {-2*mid-1,mid+1}};
        return originalAndPermuted(pairs);
    }
}
