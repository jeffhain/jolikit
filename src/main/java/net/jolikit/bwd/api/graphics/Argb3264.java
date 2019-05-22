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

/**
 * Utilities to convert back and forth between argb32 and argb64.
 */
public class Argb3264 {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /*
     * 32 bits ARGB to/from 64 bits ARGB.
     */

    /**
     * Each component is converted such as 0xAB becomes 0xABAB, not 0xAB00.
     * 
     * @param argb32 A 32 bits ARGB color.
     * @return The corresponding 64 bits ARGB color.
     */
    public static long toArgb64(int argb32) {
        return Argb64.toArgb64FromInt16_noCheck(
                toInt16FromInt8_noCheck(Argb32.getAlpha8(argb32)),
                toInt16FromInt8_noCheck(Argb32.getRed8(argb32)),
                toInt16FromInt8_noCheck(Argb32.getGreen8(argb32)),
                toInt16FromInt8_noCheck(Argb32.getBlue8(argb32)));
    }

    /**
     * Each component is converted such as 0xABCD becomes 0xAB.
     * 
     * @param argb64 A 64 bits ARGB color.
     * @return The corresponding 32 bits ARGB color.
     */
    public static int toArgb32(long argb64) {
        return Argb32.toArgb32FromInt8_noCheck(
                Argb64.getAlpha8(argb64),
                Argb64.getRed8(argb64),
                Argb64.getGreen8(argb64),
                Argb64.getBlue8(argb64));
    }

    /*
     * 
     */
    
    /**
     * The value is converted such as 0xABCD becomes 0xAB.
     * 
     * @param value8 Must be in [0,255].
     * @return The corresponding value in [0,65535].
     * @throws IllegalArgumentException is the specified value is out of range.
     */
    public static int toInt16FromInt8(int value8) {
        Argb32.checkInt8("value8", value8);
        return toInt16FromInt8_noCheck(value8);
    }
    
    /**
     * The value is converted such as 0xABCD becomes 0xAB,
     * which means that we divide by 256 and take the integer part.
     * 
     * @param value16 Must be in [0,65535].
     * @return The corresponding value in [0,255].
     * @throws IllegalArgumentException is the specified value is out of range.
     */
    public static int toInt8FromInt16(int value16) {
        Argb64.checkInt16("value16", value16);
        return toInt8FromInt16_noCheck(value16);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private Argb3264() {
    }
    
    /**
     * @param value8 Must be in [0,255].
     */
    private static int toInt16FromInt8_noCheck(int value8) {
        // Using the 8 bits both as MSBits and LSBits,
        // for regular conversions (and for example
        // for 0xFF to give 0xFFFF, not 0xFF00).
        return (value8 << 8) | value8;
    }
    
    /**
     * @param value16 Must be in [0,65535].
     */
    private static int toInt8FromInt16_noCheck(int value16) {
        return (value16 >> 8);
    }
}
