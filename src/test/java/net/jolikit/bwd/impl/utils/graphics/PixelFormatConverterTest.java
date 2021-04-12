/*
 * Copyright 2021 Jeff Hain
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

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;

public class PixelFormatConverterTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_exceptions() {
        for (int tooWideMask : new int[] {
            0x1FF,
            0x101,
            0x808,
            0x001FF000,
            0x000FF800}) {
            try {
                PixelFormatConverter.valueOf(1, tooWideMask, 4, 8);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                PixelFormatConverter.valueOf(1, 2, tooWideMask, 8);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                PixelFormatConverter.valueOf(1, 2, 4, tooWideMask);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        /*
         * Zero RGB masks.
         */
        
        try {
            PixelFormatConverter.valueOf(1, 0, 4, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            PixelFormatConverter.valueOf(1, 2, 0, 8);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            PixelFormatConverter.valueOf(1, 2, 4, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void test_alphaMaskZero() {
        final PixelFormatConverter converter =
            PixelFormatConverter.valueOf(
                0, 0x00FF0000, 0x0000FF00, 0x000000FF);
        
        checkEquals(0xFF345678, converter.toArgb32(0x12345678));
        checkEquals(0x00345678, converter.toPixel(0xFF345678));
    }
    
    public void test_simple() {
        PixelFormatConverter converter;
        
        // ARGB32.
        converter = PixelFormatConverter.valueOf(
            0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);
        checkEquals(0x12345678, converter.toArgb32(0x12345678));
        checkEquals(0x12345678, converter.toPixel(0x12345678));
        
        // Not ARGB32.
        converter = PixelFormatConverter.valueOf(
            0x000000FF, 0x0000FF00, 0x00FF0000, 0xFF000000);
        checkEquals(0x78563412, converter.toArgb32(0x12345678));
        checkEquals(0x12345678, converter.toPixel(0x78563412));
        
        // Various bit sizes, contiguous.
        converter = PixelFormatConverter.valueOf(
            0x0F000000, 0x00FF0000, 0x0000E000, 0x00001000);
        checkEquals(0xFF81B6FF, converter.toArgb32(0x0F81B000));
        checkEquals(0x0F81B000, converter.toPixel(0xFF81B6FF));
        
        // Various bit sizes, not contiguous.
        converter = PixelFormatConverter.valueOf(
            0x0F000000, 0x000FF000, 0x000000E0, 0x00000001);
        checkEquals(0xFF81B6FF, converter.toArgb32(0x0F0810A1));
        checkEquals(0x0F0810A1, converter.toPixel(0xFF81B6FF));
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void checkEquals(int expected, int actual) {
        final String e = Argb32.toString(expected);
        final String a = Argb32.toString(actual);
        if (!e.equals(a)) {
            System.out.println("expected = " + e);
            System.out.println("actual =   " + a);
        }
        assertEquals(e, a);
    }
}
