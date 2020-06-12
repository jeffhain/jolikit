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
package net.jolikit.bwd.impl.utils.gprim;

import junit.framework.TestCase;

public class LineStippleUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public LineStippleUtilsTest() {
    }
    
    public void test_mustDraw_int_short_int() {

        /*
         * Empty pattern : never draw.
         */
        
        for (int pixelNum = 0; pixelNum <= 100; pixelNum++) {
            assertFalse(LineStippleUtils.mustDraw(1, (short) 0, pixelNum));
            assertFalse(LineStippleUtils.mustDraw(2, (short) 0, pixelNum));
            assertFalse(LineStippleUtils.mustDraw(20, (short) 0, pixelNum));
            assertFalse(LineStippleUtils.mustDraw(40, (short) 0, pixelNum));
        }
        
        /*
         * Plain pattern : always draw.
         */
        
        for (int pixelNum = 0; pixelNum <= 100; pixelNum++) {
            assertTrue(LineStippleUtils.mustDraw(1, GprimUtils.PLAIN_PATTERN, pixelNum));
            assertTrue(LineStippleUtils.mustDraw(2, GprimUtils.PLAIN_PATTERN, pixelNum));
            assertTrue(LineStippleUtils.mustDraw(20, GprimUtils.PLAIN_PATTERN, pixelNum));
            assertTrue(LineStippleUtils.mustDraw(40, GprimUtils.PLAIN_PATTERN, pixelNum));
        }
        
        /*
         * 
         */
        
        {
            final short pattern = (short) 0x0001;
            for (int factor : new int[]{1, 2, 20, 40}) {
                for (int i = 0; i < factor; i++) {
                    assertTrue(LineStippleUtils.mustDraw(factor, pattern, 0 + i));
                }
                for (int pixelNum = factor; pixelNum <= factor * 16 - 1; pixelNum++) {
                    assertFalse(LineStippleUtils.mustDraw(factor, pattern, pixelNum));
                }
                assertTrue(LineStippleUtils.mustDraw(factor, pattern, factor * 16));
            }
        }
    }
}
