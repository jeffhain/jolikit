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

public class Argb3264Test extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int[] BAD_INT8_ARR = new int[]{
        -1, 256,
        Integer.MIN_VALUE, Integer.MAX_VALUE};
    
    private static final int[] BAD_INT16_ARR = new int[]{
        -1, 65536,
        Integer.MIN_VALUE, Integer.MAX_VALUE};
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_toArgb64_int() {
        assertEquals(0x121256569A9ADEDEL, Argb3264.toArgb64(0x12569ADE));
    }
    
    public void test_toArgb32_long() {
        assertEquals(0x12569ADE, Argb3264.toArgb32(0x123456789ABCDEF0L));
    }
    
    /*
     * 
     */

    public void test_toInt16FromInt8_int() {
        for (int bad8 : BAD_INT8_ARR) {
            try {
                Argb3264.toInt16FromInt8(bad8);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(0, Argb3264.toInt16FromInt8(0));
        assertEquals(0x1212, Argb3264.toInt16FromInt8(0x12));
        assertEquals(0x7F7F, Argb3264.toInt16FromInt8(0x7F));
        assertEquals(0x8080, Argb3264.toInt16FromInt8(0x80));
        assertEquals(0xFFFF, Argb3264.toInt16FromInt8(0xFF));
    }
    
    public void test_toInt8FromInt16_int() {
        for (int bad16 : BAD_INT16_ARR) {
            try {
                Argb3264.toInt8FromInt16(bad16);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertEquals(0, Argb3264.toInt8FromInt16(0));
        
        // not rounding
        assertEquals(0x12, Argb3264.toInt8FromInt16(0x12FF));
        assertEquals(0x80, Argb3264.toInt8FromInt16(0x8080));
        
        assertEquals(0xFF, Argb3264.toInt8FromInt16(0xFF00));
        assertEquals(0xFF, Argb3264.toInt8FromInt16(0xFFFF));
    }
}
