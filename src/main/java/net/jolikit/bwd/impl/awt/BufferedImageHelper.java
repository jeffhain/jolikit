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
package net.jolikit.bwd.impl.awt;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.NumbersUtils;

public class BufferedImageHelper {

    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    public enum BihPixelFormat {
        /**
         * Our usual format.
         */
        ARGB32,
        /**
         * OpenGL-friendly format when native is little endian.
         */
        ABGR32,
        /**
         * OpenGL-friendly format when native is big endian.
         */
        RGBA32;
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final BufferedImage image;
    
    private final boolean mustUseGetSample;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BufferedImageHelper(BufferedImage image) {
        this.image = image;
        this.mustUseGetSample = isSafeToUseRasterGetSample(image);
    }
    
    public BufferedImage getImage() {
        return this.image;
    }
    
    /*
     * Utilities to create buffered images wrapped on an int array of pixels.
     */
    
    /**
     * Components indexes increase from MSByte to LSByte,
     * and must start at 0 for first present component.
     * 
     * @param pixelArr The int array of pixels to use.
     * @param width Image width.
     * @param height Image height.
     * @param isRasterPremultiplied
     * @param rIndex Index, in [0,3], of red component byte.
     * @param gIndex Value in [0,3], of green component byte.
     * @param bIndex Value in [0,3], of blue component byte.
     * @param aIndex Value in [0,3], of alpha component byte,
     *        or -1 if no alpha component.
     * @return The created buffered image, with 32 bits pixels if there is
     *         alpha component, and 24 bits pixels otherwise.
     */
    public static BufferedImage newBufferedImage(
            int[] pixelArr,
            int width,
            int height,
            //
            boolean isRasterPremultiplied,
            int aIndex,
            //
            int rIndex,
            int gIndex,
            int bIndex) {
        
        final int pixelCapacity = NumbersUtils.timesExact(width, height);
        
        final boolean gotAlpha = (aIndex >= 0);
        final int bitSize = (gotAlpha ? 32 : 24);
        
        final int rMask = computeCptMask(bitSize, rIndex);
        final int gMask = computeCptMask(bitSize, gIndex);
        final int bMask = computeCptMask(bitSize, bIndex);
        final int aMask = computeCptMask(bitSize, aIndex);
        
        final int[] bitMasks;
        final DirectColorModel colorModel;
        if (gotAlpha) {
            bitMasks = new int[] {rMask, gMask, bMask, aMask};
            colorModel = new DirectColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    bitSize,
                    rMask,
                    gMask,
                    bMask,
                    aMask,
                    isRasterPremultiplied,
                    DataBuffer.TYPE_INT);
        } else {
            bitMasks = new int[] {rMask, gMask, bMask};
            colorModel = new DirectColorModel(bitSize, rMask, gMask, bMask);
        }
        
        final SampleModel sampleModel = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT,
                width,
                height,
                bitMasks);
        final DataBuffer dataBuffer = new DataBufferInt(
                pixelArr,
                pixelCapacity);
        final Point upperLeftLocation = new Point(0,0);
        final WritableRaster raster = Raster.createWritableRaster(
                sampleModel,
                dataBuffer,
                upperLeftLocation);
        final BufferedImage image = new BufferedImage(
                colorModel,
                raster,
                isRasterPremultiplied,
                null);
        return image;
    }
    
    /*
     * 
     */
    
    /**
     * @param pixelFormat Pixel format to use for output.
     * @param premul Whether output must be alpha-premultiplied.
     * @param color32Arr (out)
     */
    public static void copyImageIntoArray(
            BufferedImage image,
            //
            BihPixelFormat pixelFormat,
            boolean premul,
            //
            int[] color32Arr,
            int width,
            int height) {

        /*
         * A more high level and simpler way would be to create a BufferedImage
         * around the destination int array, and then draw the specified image
         * into its graphics, with drawImage(BufferedImage, null, 0, 0),
         * but for some reason it's 5 or 10 times slower than using these
         * low level APIs, so we prefer to use them.
         */

        final BufferedImageHelper instance = new BufferedImageHelper(image);
        final WritableRaster raster = instance.image.getRaster();
        final int transferType = raster.getTransferType();
        final int numBands = raster.getNumBands();
        final Object tmpData = newTmpData(transferType, numBands);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color32 = instance.getPixelAt(
                        x,
                        y,
                        pixelFormat,
                        premul,
                        tmpData);
                color32Arr[y * width + x] = color32;
            }
        }
    }
    
    /*
     * 
     */

    /**
     * Works on the image used to create this instance.
     * 
     * @param pixelFormat Format for the returned pixel.
     * @return The pixel at the specified location, not alpha premultiplied.
     */
    public int getPixelAt(
            int x,
            int y,
            //
            BihPixelFormat pixelFormat) {
        final boolean premul = false;
        return this.getPixelAt(x, y, pixelFormat, premul);
    }

    /**
     * Works on the image used to create this instance.
     * 
     * @param pixelFormat Format for the returned pixel.
     * @param premul Whether the returned pixel must be alpha premultiplied. 
     * @return The pixel at the specified location, not alpha premultiplied.
     */
    public int getPixelAt(
            int x,
            int y,
            //
            BihPixelFormat pixelFormat,
            boolean premul) {
        /*
         * Letting the method create one if needed.
         * 
         * NB: Could reuse an instance field to avoid creating
         * one for each pixel read, but that would not be thread-safe,
         * and some people might assume reads are.
         */
        final Object tmpData = null;
        return this.getPixelAt(
                x,
                y,
                //
                pixelFormat,
                premul,
                //
                tmpData);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param pixelFormat Format for the returned pixel.
     * @param premul Whether the returned pixel must be alpha premultiplied. 
     * @param tmpData Can be null, in which case one is created if needed.
     * @return The pixel at the specified location.
     */
    private int getPixelAt(
            int x,
            int y,
            //
            BihPixelFormat pixelFormat,
            boolean premul,
            //
            Object tmpData) {

        final WritableRaster raster = this.image.getRaster();

        int color32;
        if (this.mustUseGetSample) {
            /*
             * Faster way.
             */
            final int numBands = raster.getNumBands();
            /*
             * "band" = index of bit mask in bit mask array,
             * so 0 = red, 1 = green, 2 = blue, and 3 = alpha (if any).
             */
            final int alpha8 = ((numBands == 4) ? raster.getSample(x, y, 3) : 0xFF);
            final int red8 = raster.getSample(x, y, 0);
            final int green8 = raster.getSample(x, y, 1);
            final int blue8 = raster.getSample(x, y, 2);
            color32 = computePixel(alpha8, red8, green8, blue8, pixelFormat);
        } else {
            /*
             * Slower way, but more general.
             */
            if (tmpData == null) {
                final int numBands = raster.getNumBands();
                final int transferType = raster.getTransferType();
                // Lazy creation.
                tmpData = newTmpData(transferType, numBands);
            }
            final ColorModel colorModelFrom = this.image.getColorModel();
            raster.getDataElements(x, y, tmpData);
            final int alpha8 = colorModelFrom.getAlpha(tmpData);
            final int red8 = colorModelFrom.getRed(tmpData);
            final int green8 = colorModelFrom.getGreen(tmpData);
            final int blue8 = colorModelFrom.getBlue(tmpData);
            color32 = computePixel(alpha8, red8, green8, blue8, pixelFormat);
        }
        
        if (premul != this.image.isAlphaPremultiplied()) {
            if (premul) {
                if (pixelFormat == BihPixelFormat.RGBA32) {
                    color32 = BindingColorUtils.toPremulXyza32(color32);
                } else {
                    color32 = BindingColorUtils.toPremulAxyz32(color32);
                }
            } else {
                if (pixelFormat == BihPixelFormat.RGBA32) {
                    color32 = BindingColorUtils.toNonPremulXyza32(color32);
                } else {
                    color32 = BindingColorUtils.toNonPremulAxyz32(color32);
                }
            }
        }

        return color32;
    }
    
    /*
     * 
     */
    
    private static boolean isSafeToUseRasterGetSample(BufferedImage image) {
        final WritableRaster raster = image.getRaster();
        final ColorModel colorModel = image.getColorModel();
        final int transferType = colorModel.getTransferType();

        final int numBands = raster.getNumBands();

        final ColorSpace colorSpace = colorModel.getColorSpace();
        final int colorSpaceType = colorSpace.getType();
        
        /*
         * Not sure how to use Raster.getSample(...)
         * if these conditions don't hold.
         */
        final boolean result =
                (colorSpaceType == ColorSpace.TYPE_RGB)
                // Because it can be < 3, even if color space is RGB.
                && (numBands >= 3)
                && (!containsNon8BitsSizeCpt(colorModel))
                && ((transferType == DataBuffer.TYPE_BYTE)
                        || (transferType == DataBuffer.TYPE_SHORT)
                        || (transferType == DataBuffer.TYPE_USHORT)
                        || (transferType == DataBuffer.TYPE_INT));
        return result;
    }
    
    /*
     * 
     */
    
    private static int computePixel(
            int alpha8,
            int red8,
            int green8,
            int blue8,
            BihPixelFormat pixelFormat) {
        switch (pixelFormat) {
        case ARGB32: {
            return (alpha8 << 24) | (red8 << 16) | (green8 << 8) | (blue8 << 0);
        }
        case ABGR32: {
            return (alpha8 << 24) | (blue8 << 16) | (green8 << 8) | (red8 << 0);
        }
        case RGBA32: {
            return (red8 << 24) | (green8 << 16) | (blue8 << 8) | (alpha8 << 0);
        }
        default:
            throw new IllegalArgumentException("" + pixelFormat);
        }
    }
    
    private static int computeCptMask(int bitSize, int cptIndex) {
        if ((cptIndex < -1) || (cptIndex > 3)) {
            throw new IllegalArgumentException("cptIndex [" + cptIndex + "] must be in [-1,3]");
        }
        final int cptMask;
        if (cptIndex < 0) {
            cptMask = 0;
        } else {
            cptMask = (0xFF << (bitSize - (cptIndex + 1) * 8));
        }
        return cptMask;
    }
    
    /**
     * @return True if the bit size of at least one component is not 8.
     */
    private static boolean containsNon8BitsSizeCpt(ColorModel colorModel) {
        final int[] cptBitSizeArr = colorModel.getComponentSize();

        boolean foundNon8BitsSizeCpt = false;
        for (int cptSize : cptBitSizeArr) {
            if (cptSize != 8) {
                foundNon8BitsSizeCpt = true;
                break;
            }
        }

        return foundNon8BitsSizeCpt;
    }

    private static Object newTmpData(
            int transferType,
            int numBands) {
        final Object tmpData;
        switch (transferType) {
        case DataBuffer.TYPE_BYTE: {
            tmpData = new byte[numBands];
        } break;
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT: {
            tmpData = new short[numBands];
        } break;
        case DataBuffer.TYPE_INT: {
            tmpData = new int[numBands];
        } break;
        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE: {
            tmpData = new double[numBands];
        } break;
        default:
            throw new BindingError("" + transferType);
        }
        return tmpData;
    }
}
