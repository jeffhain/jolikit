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

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.lang.NbrsUtils;

/**
 * Converter between a pixel format (typically used
 * by backing libraries) and ARGB32.
 * Alpha premultiplication is not taken into account:
 * it must be taken care of (if needed) before or after using this converter.
 * 
 * Masks must not be more than 8 bits long.
 * If backing library masks are larger,
 * you should only consider their MSBits.
 */
public class PixelFormatConverter {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * If your format is ARGB32, you should use this instance,
     * which uses quick identity conversion.
     */
    public static final PixelFormatConverter ARGB32_CONVERTER =
        new PixelFormatConverter(
            0xFF000000,
            0x00FF0000,
            0x0000FF00,
            0x000000FF) {
        @Override
        public int toArgb32(int pixel) {
            return pixel;
        }
        @Override
        public int toPixel(int argb32) {
            return argb32;
        }
    };
    
    /*
     * 
     */
    
    private final int aMask;
    private final int rMask;
    private final int gMask;
    private final int bMask;
    
    private final byte aShift;
    private final byte rShift;
    private final byte gShift;
    private final byte bShift;
    
    private final byte aBitSize;
    private final byte rBitSize;
    private final byte gBitSize;
    private final byte bBitSize;
    
    private final double aFfDivMax;
    private final double rFfDivMax;
    private final double gFfDivMax;
    private final double bFfDivMax;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[" + NbrsUtils.toStringHex(this.aMask)
        + "," + NbrsUtils.toStringHex(this.rMask)
        + "," + NbrsUtils.toStringHex(this.gMask)
        + "," + NbrsUtils.toStringHex(this.bMask)
        + ",]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hc = this.aMask;
        hc = hc * prime + this.rMask;
        hc = hc * prime + this.gMask;
        hc = hc * prime + this.bMask;
        return hc;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PixelFormatConverter)) {
            return false;
        }
        final PixelFormatConverter other = (PixelFormatConverter) obj;
        // Alpha tested last as it's the most likely to be equal.
        return (this.rMask == other.rMask)
            && (this.gMask == other.gMask)
            && (this.bMask == other.bMask)
            && (this.aMask == other.aMask);
    }
    
    /*
     * 
     */
    
    /**
     * Masks bit size must be <= 8.
     * They might overlap (not checked).
     * 
     * @param aMask Can be zero, in which case 0xFF is used as alpha8 for ARGB32.
     * @param rMask Must not be zero.
     * @param gMask Must not be zero.
     * @param bMask Must not be zero.
     * @throws IllegalArgumentException if a mask is invalid.
     */
    public static PixelFormatConverter valueOf(
        int aMask,
        int rMask,
        int gMask,
        int bMask) {
        
        final PixelFormatConverter ret;
        if ((aMask == ARGB32_CONVERTER.aMask)
            && (rMask == ARGB32_CONVERTER.rMask)
            && (gMask == ARGB32_CONVERTER.gMask)
            && (bMask == ARGB32_CONVERTER.bMask)) {
            /*
             * ARGB32 case: we want the optimized converter.
             */
            ret = ARGB32_CONVERTER;
        } else {
            ret = new PixelFormatConverter(
                aMask, rMask, gMask, bMask);
        }
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * If alpha mask is 0, using 0xFF as alpha.
     * 
     * @param pixel Pixel in the specified format.
     * @return The corresponding ARGB32 value.
     */
    public int toArgb32(int pixel) {
        final int alpha8;
        if (this.aMask == 0) {
            // "no alpha" in formats means always opaque
            // (always transparent would be pointless).
            alpha8 = 0xFF;
        } else {
            alpha8 = cpt8FromPixelAndMask(pixel, this.aMask, this.aShift, this.aFfDivMax);
        }
        
        final int red8 = cpt8FromPixelAndMask(pixel, this.rMask, this.rShift, this.rFfDivMax);
        final int green8 = cpt8FromPixelAndMask(pixel, this.gMask, this.gShift, this.gFfDivMax);
        final int blue8 = cpt8FromPixelAndMask(pixel, this.bMask, this.bShift, this.bFfDivMax);
        
        return BindingColorUtils.toAbcd32_noCheck(alpha8, red8, green8, blue8);
    }

    /**
     * @param argb32 ARGB32 value.
     * @return The corresponding pixel value.
     */
    public int toPixel(int argb32) {
        final int alpha8 = Argb32.getAlpha8(argb32);
        final int red8 = Argb32.getRed8(argb32);
        final int green8 = Argb32.getGreen8(argb32);
        final int blue8 = Argb32.getBlue8(argb32);
        
        final int aBits = toPixelBitsFromCpt8AndMask(
            alpha8, this.aShift, this.aBitSize);
        final int rBits = toPixelBitsFromCpt8AndMask(
            red8, this.rShift, this.rBitSize);
        final int gBits = toPixelBitsFromCpt8AndMask(
            green8, this.gShift, this.gBitSize);
        final int bBits = toPixelBitsFromCpt8AndMask(
            blue8, this.bShift, this.bBitSize);
        
        return aBits | rBits | gBits | bBits;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Masks bit size must be <= 8.
     * They might overlap (not checked).
     * 
     * @param aMask Can be zero, in which case 0xFF is used as alpha8 for ARGB32.
     * @param rMask Must not be zero.
     * @param gMask Must not be zero.
     * @param bMask Must not be zero.
     * @throws IllegalArgumentException if a mask is invalid.
     */
    private PixelFormatConverter(
            int aMask,
            int rMask,
            int gMask,
            int bMask) {
        checkMaskNotZero(rMask, "rMask");
        checkMaskNotZero(gMask, "gMask");
        checkMaskNotZero(bMask, "bMask");
        
        this.aMask = aMask;
        this.rMask = rMask;
        this.gMask = gMask;
        this.bMask = bMask;
        
        this.aShift = computeCptShift(aMask);
        this.rShift = computeCptShift(rMask);
        this.gShift = computeCptShift(gMask);
        this.bShift = computeCptShift(bMask);
        
        this.aBitSize = computeCptBitSize(aMask, this.aShift);
        this.rBitSize = computeCptBitSize(rMask, this.rShift);
        this.gBitSize = computeCptBitSize(gMask, this.gShift);
        this.bBitSize = computeCptBitSize(bMask, this.bShift);
        
        checkMaskBitSize(this.aMask, this.aBitSize);
        checkMaskBitSize(this.rMask, this.rBitSize);
        checkMaskBitSize(this.gMask, this.gBitSize);
        checkMaskBitSize(this.bMask, this.bBitSize);
        
        if (aMask == 0) {
            this.aFfDivMax = Double.NaN;
        } else {
            this.aFfDivMax = computeFfDivMax(aMask, this.aShift, this.aBitSize);
        }
        this.rFfDivMax = computeFfDivMax(rMask, this.rShift, this.rBitSize);
        this.gFfDivMax = computeFfDivMax(gMask, this.gShift, this.gBitSize);
        this.bFfDivMax = computeFfDivMax(bMask, this.bShift, this.bBitSize);
    }
    
    /*
     * 
     */
    
    private static void checkMaskNotZero(int mask, String name) {
        if (mask == 0) {
            throw new IllegalArgumentException(
                name + " [" + NbrsUtils.toStringHex(mask)
                + "] must not be zero");
        }
    }
    
    private static void checkMaskBitSize(int mask, int bitSize) {
        if (bitSize > 8) {
            throw new IllegalArgumentException(
                "mask [" + NbrsUtils.toStringHex(mask)
                + "] is more than 8 bits wide: bitSize = " + bitSize);
        }
    }
    
    /*
     * 
     */

    /**
     * If the mask is zero, returns 0.
     * 
     * @param mask Mask for a component.
     * @return The unsigned right-shift for mask (1) bits to become int LSBits.
     */
    private static byte computeCptShift(int mask) {
        final int tail = Integer.numberOfTrailingZeros(mask);
        final byte shift;
        if (tail == 32) {
            // Empty mask.
            shift = 0;
        } else {
            shift = (byte) tail;
        }
        return shift;
    }
    
    private static byte computeCptBitSize(int mask, int shift) {
        return (byte) (32 - Integer.numberOfLeadingZeros(mask >>> shift));
    }

    /**
     * ffDivMax = (0xFF / (double) cptMax)
     * with cptMax = ((1<<cptBitSize)-1)
     */
    private static double computeFfDivMax(int mask, int shift, int bitSize) {
        final int cptMax = ((1 << bitSize) - 1);
        return (0xFF / (double) cptMax);
    }

    /*
     * 
     */
    
    private static int cpt8FromPixelAndMask(int pixel, int mask, int shift, double ffDivMax) {
        final int cptN = ((pixel & mask) >>> shift);
        final double cptFp255 = (cptN * ffDivMax);
        return (int) (cptFp255 + 0.5);
    }
    
    private static int toPixelBitsFromCpt8AndMask(
        int cpt8, int shift, int bitSize) {
        return ((cpt8 >> (8 - bitSize)) << shift);
    }
}
