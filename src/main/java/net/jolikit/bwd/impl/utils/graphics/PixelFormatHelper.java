/*
 * Copyright 2020 Jeff Hain
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

import java.util.ArrayList;

import net.jolikit.bwd.api.graphics.Argb32;

/**
 * Helper class to convert various pixel formats (typically used
 * by backing libraries) to ARGB32.
 * Alpha premultiplication is not taken into account:
 * it must be taken care of (if needed) before or after using this helper.
 * 
 * Pixel formats are represented by an int, typically the ordinal
 * of some (made-up, or from the backing library) enum.
 * 
 * Masks must not be more than 8 bits long.
 * If backing library masks are larger,
 * you should only consider their MSBits.
 */
public class PixelFormatHelper {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyFormatData {
        final int aMask;
        final int rMask;
        final int gMask;
        final int bMask;
        //
        final byte aShift;
        final byte rShift;
        final byte gShift;
        final byte bShift;
        public MyFormatData(
                int aMask,
                int rMask,
                int gMask,
                int bMask) {
            this.aMask = aMask;
            this.rMask = rMask;
            this.gMask = gMask;
            this.bMask = bMask;
            
            this.aShift = computeCptShift(aMask);
            this.rShift = computeCptShift(rMask);
            this.gShift = computeCptShift(gMask);
            this.bShift = computeCptShift(bMask);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MAX_FORMAT = 1000;
    
    private final ArrayList<MyFormatData> dataByFormat = new ArrayList<MyFormatData>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PixelFormatHelper() {
    }
    
    /**
     * Each mask must not use more than 8 bits,
     * for masked values to fit in ARGB32 components.
     * 
     * Format is arbitrarily bounded to 1000, to allow for use as index
     * (fast access) without wasting memory.
     * 
     * @param format Must be in [0,1000].
     * @param aMask Mask for alpha.
     * @param rMask Mask for red.
     * @param gMask Mask for green.
     * @param bMask Mask for blue.
     * @throws IllegalArgumentException if format is larger than 1000.
     */
    public void addFormat(
            int format,
            //
            int aMask,
            int rMask,
            int gMask,
            int bMask) {
        
        if ((format < 0) || (format > MAX_FORMAT)) {
            throw new IllegalArgumentException("format [" + format + "] must be in [0," + MAX_FORMAT + "]");
        }
        
        final MyFormatData data = new MyFormatData(aMask, rMask, gMask, bMask);
        
        /*
         * Storing format data.
         */
        
        while (this.dataByFormat.size() <= format) {
            this.dataByFormat.add(null);
        }
        this.dataByFormat.set(format, data);
    }
    
    /**
     * If alpha mask is 0, using 0xFF as alpha.
     * 
     * @param format Format to use. Must be one of those added.
     * @param pixel Pixel in the specified format.
     * @return The corresponding ARGB32 value.
     * @throws IndexOutOfBoundsException or IllegalArgumentException
     *         if the format is unknown.
     */
    public int toArgb32(int format, int pixel) {
        // IndexOutOfBoundsException if format not in index range.
        final MyFormatData data = this.dataByFormat.get(format);
        if (data == null) {
            throw new IllegalArgumentException("unknown format: " + format);
        }
        
        return BindingColorUtils.toArgb32FromPixelWithMasksAndShifts(
                pixel,
                data.aMask, data.rMask, data.gMask, data.bMask,
                data.aShift, data.rShift, data.gShift, data.bShift);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * If the mask is zero, returns 0.
     * 
     * @param mask Mask for a component. Must not be more than 8 bits long.
     * @return The unsigned right-shift for mask (1) bits to become int LSBits.
     * @throws IllegalArgumentException if the mask is more than 8 bits long.
     */
    private static byte computeCptShift(int mask) {
        final int tail = Integer.numberOfTrailingZeros(mask);
        final byte shift;
        if (tail == 32) {
            // Empty mask.
            shift = 0;
        } else {
            {
                final int head = Integer.numberOfLeadingZeros(mask);
                final int maskLength = (32 - (head + tail));
                if (maskLength > 8) {
                    throw new IllegalArgumentException("invalid mask: " + Argb32.toString(mask));
                }
            }
            shift = (byte) tail;
        }
        return shift;
    }
}
