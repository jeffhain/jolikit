/*
 * Copyright 2019-2021 Jeff Hain
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

import java.awt.Graphics2D;
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

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * Non-static methods may use instance-specific mutable state
 * (other than the buffered image), so they must not be used concurrently
 * (even read methods).
 */
public class BufferedImageHelper {
    
    /*
     * For reading and writing pixels, using raster
     * getSample() and setSample() methods
     * doesn't require a tmp for not creating garbage,
     * but it can only be used for certain kinds of images
     * (cf. computeIsSafeToUseRasterSample()),
     * and it is slower due to more bounds checks and data buffer
     * accesses, so we stick to the general method of
     * using raster getDataElements() and setDataElements().
     */
    
    /*
     * Must only use graphics for intermediary operations,
     * not for a whole public operation, since the point of this class
     * is to be a (faster) alternative to graphics usage.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * For a 500k pixels image, using drawImage() can be much faster
     * than reading pixels one by one : about 2 ms instead of about 15 ms.
     */
    private static final boolean MUST_READ_BULK_PIXELS_WITH_DRAW_IMAGE = true;
    
    /**
     * TODO awt For a 500k pixels image, if pixel format is not ARGB,
     * it makes drawImage() much slower : about 15 ms instead of about 2 ms
     * (i.e. as slow as reading the image pixel by pixel).
     * As a result, we always use ARGB for it, and then convert pixels
     * to the proper format afterwards.
     */
    private static final boolean MUST_ALWAYS_USE_ARGB_FOR_PIXELS_READING_IMAGE = true;
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Doesn't indicate whether the pixel is alpha premultiplied.
     */
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
    
    /**
     * Pixel format in Java, that gives RGBA32 when read natively.
     */
    public static final BihPixelFormat NATIVE_RGBA32_PIXEL_FORMAT;
    static {
        final BihPixelFormat pixelFormat;
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            pixelFormat = BihPixelFormat.ABGR32;
        } else {
            pixelFormat = BihPixelFormat.RGBA32;
        }
        NATIVE_RGBA32_PIXEL_FORMAT = pixelFormat;
    }
    
    public static final boolean PREMUL = true;
    public static final boolean NON_PREMUL = false;

    /*
     * "band" = index of bit mask in bit mask array
     * used by SampleModel.
     */
    
    private static final int R_BAND = 0;
    private static final int G_BAND = 1;
    private static final int B_BAND = 2;
    private static final int A_BAND = 3;

    /**
     * 0x81 = 10000001, to check that extreme bits are preserved.
     * Not using 0xFF to be more general, less special-case.
     * 
     * Always opaque colors for non-alpha components,
     * for them not to be modified or zeroized
     * due to alpha premultiplication.
     */
    private static final int[] CPT_EXTREME_BITS_BY_BAND = new int[4];
    static {
        CPT_EXTREME_BITS_BY_BAND[R_BAND] = 0xFF810000;
        CPT_EXTREME_BITS_BY_BAND[G_BAND] = 0xFF008100;
        CPT_EXTREME_BITS_BY_BAND[B_BAND] = 0xFF000081;
        CPT_EXTREME_BITS_BY_BAND[A_BAND] = 0x81000000;
    }
    
    private static final int[] R_G_B_BAND_ARR = new int[]{
        R_BAND,
        G_BAND,
        B_BAND,
    };

    private final BufferedImage image;
    
    /*
     * temps
     */
    
    /**
     * For instance methods, which are not meant to be thread-safe.
     */
    private Object tmpDataLazy = null;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BufferedImageHelper(BufferedImage image) {
        this.image = LangUtils.requireNonNull(image);
    }
    
    public BufferedImage getImage() {
        return this.image;
    }
    
    /*
     * Utilities to create buffered images wrapped on an int array of pixels.
     */
    
    /**
     * @param pixelArr The int array of pixels to use.
     * @param width Image width.
     * @param height Image height.
     * @param bufferedImageType Type of the backing BufferedImage.
     *        Only supporting BufferedImage.TYPE_INT_XXX types
     *        {ARGB, ARGB_PRE, BGR, RGB}.
     * @return The created buffered image, with 32 bits pixels if there is
     *         alpha component, and 24 bits pixels otherwise.
     */
    public static BufferedImage newBufferedImageWithIntArray(
            int[] pixelArr,
            int width,
            int height,
            int bufferedImageType) {
        
        /*
         * TODO awt If using for example RGBA here,
         * image drawing is much slower,
         * with much time spent here:
         * sun.java2d.loops.OpaqueCopyArgbToAny.Blit(CustomComponent.java:202)
         * sun.java2d.loops.GraphicsPrimitive.convertTo(GraphicsPrimitive.java:571)
         * sun.java2d.loops.MaskBlit$General.MaskBlit(MaskBlit.java:225)
         *    - locked sun.java2d.loops.MaskBlit$General@75c77d5e
         * sun.java2d.loops.Blit$GeneralMaskBlit.Blit(Blit.java:204)
         * sun.java2d.pipe.DrawImage.blitSurfaceData(DrawImage.java:959)
         * sun.java2d.pipe.DrawImage.renderImageCopy(DrawImage.java:577)
         * sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:67)
         * sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:1014)
         * sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3318)
         * sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3296)
         */
        final boolean isRasterPremultiplied;
        final int aIndex;
        final int rIndex;
        final int gIndex;
        final int bIndex;
        switch (bufferedImageType) {
        case BufferedImage.TYPE_INT_ARGB: {
            isRasterPremultiplied = false;
            aIndex = 0;
            rIndex = 1;
            gIndex = 2;
            bIndex = 3;
        } break;
        case BufferedImage.TYPE_INT_ARGB_PRE: {
            isRasterPremultiplied = true;
            aIndex = 0;
            rIndex = 1;
            gIndex = 2;
            bIndex = 3;
        } break;
        case BufferedImage.TYPE_INT_BGR: {
            isRasterPremultiplied = false;
            aIndex = -1;
            bIndex = 0;
            gIndex = 1;
            rIndex = 2;
        } break;
        case BufferedImage.TYPE_INT_RGB: {
            isRasterPremultiplied = false;
            aIndex = -1;
            rIndex = 0;
            gIndex = 1;
            bIndex = 2;
        } break;
        default:
            throw new IllegalArgumentException("" + bufferedImageType);
        }

        return newBufferedImageWithIntArray(
                pixelArr,
                width,
                height,
                //
                isRasterPremultiplied,
                aIndex,
                //
                rIndex,
                gIndex,
                bIndex);
    }

    /**
     * @param pixelArr The int array of pixels to use.
     * @param width Image width.
     * @param height Image height.
     * @param pixelFormat The pixel format to use.
     * @param premul Whether pixels must be alpha premultiplied.
     * @return The created buffered image, with 32 bits pixels if there is
     *         alpha component, and 24 bits pixels otherwise.
     */
    public static BufferedImage newBufferedImageWithIntArray(
            int[] pixelArr,
            int width,
            int height,
            BihPixelFormat pixelFormat,
            boolean premul) {
        
        final int aIndex;
        final int rIndex;
        final int gIndex;
        final int bIndex;
        switch (pixelFormat) {
        case ARGB32: {
            aIndex = 0;
            rIndex = 1;
            gIndex = 2;
            bIndex = 3;
        } break;
        case ABGR32: {
            aIndex = 0;
            rIndex = 3;
            gIndex = 2;
            bIndex = 1;
        } break;
        case RGBA32: {
            aIndex = 3;
            bIndex = 2;
            gIndex = 1;
            rIndex = 0;
        } break;
        default:
            throw new IllegalArgumentException("" + pixelFormat);
        }

        return newBufferedImageWithIntArray(
                pixelArr,
                width,
                height,
                //
                premul,
                aIndex,
                //
                rIndex,
                gIndex,
                bIndex);
    }

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
    public static BufferedImage newBufferedImageWithIntArray(
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
        
        final int pixelCapacity = NbrsUtils.timesExact(width, height);
        
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
     * Misc utilities.
     */
    
    /**
     * @param image An image.
     * @param withAlpha Alpha presence flag.
     * @param pixelFormat Pixel format.
     * @param premul Alpha premultiplied flag.
     * @return The specified image.
     * @throws IllegalArgumentException if the specified image
     *         is not backed by an int array of pixels (i.e. a DataBufferInt)
     *         which pixels are as specified.
     */
    public static BufferedImage requireCompatible(
            BufferedImage image,
            boolean withAlpha,
            BihPixelFormat pixelFormat,
            boolean premul) {

        if (premul && (!withAlpha)) {
            throw new IllegalArgumentException("alpha premultiplied, but with no alpha");
        }

        /*
         * 
         */
        
        if (!isWithIntArray(image)) {
            throw new IllegalArgumentException("image is not backed by an int array");
        }
        
        final WritableRaster raster = image.getRaster();
        final ColorModel colorModel = image.getColorModel();
        final SampleModel sampleModel = raster.getSampleModel();
        
        final int numBands = sampleModel.getNumBands();
        
        if (withAlpha) {
            checkNumBands(4, numBands);
            
            if (premul != image.isAlphaPremultiplied()) {
                throw new IllegalArgumentException(
                        "alpha premultiplied : expected " + premul
                        + ", got " + image.isAlphaPremultiplied());
            }
            
            checkBandDataIsConsistentWithArgb32(
                    colorModel,
                    sampleModel,
                    A_BAND);
        } else {
            checkNumBands(3, numBands);
        }

        for (int band : R_G_B_BAND_ARR) {
            checkBandDataIsConsistentWithArgb32(
                    colorModel,
                    sampleModel,
                    band);
        }

        return image;
    }
    
    /**
     * @param image An image.
     * @return True if the specified image is backed by an int array
     *         of pixels (i.e. a DataBufferInt).
     */
    public static boolean isWithIntArray(BufferedImage image) {
        final WritableRaster raster = image.getRaster();
        final DataBuffer dataBuffer = raster.getDataBuffer();
        return (dataBuffer instanceof DataBufferInt);
    }
    
    /**
     * Method useful if image pixel format is known and appropriate,
     * and if its scanline stride is known as well.
     * 
     * Note that the backing int array is marked as "untrackable"
     * before being returned, which could prevent some optimizations
     * (cf. DataBufferInt.getData()).
     * Note also that the int array is "untrackable" anyway
     * if it has been specified for image creation, which is
     * the case for our newBufferedImageWithIntArray() methods.
     * 
     * @param image An image.
     * @return The backing int array of pixels, or null if the specified image
     *         is not backed by an int array.
     * @throws ClassCastException if the specified image is not backed
     *         by an int array.
     */
    public static int[] getIntPixelArr(BufferedImage image) {
        final WritableRaster raster = image.getRaster();
        final DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
        return dataBuffer.getData();
    }
    
    /*
     * Base single pixel methods.
     */

    /**
     * @return The ARGB 32, not alpha premultiplied, at the specified location.
     */
    public int getArgb32At(
            int x,
            int y) {

        final WritableRaster raster = this.image.getRaster();

        final ColorModel colorModel = this.image.getColorModel();
        
        this.tmpDataLazy = raster.getDataElements(
                x,
                y,
                this.tmpDataLazy);

        return colorModel.getRGB(this.tmpDataLazy);
    }

    /**
     * If the image has no alpha, actually meaning is doesn't support
     * transparency, you might want to give an opaque color here.
     * 
     * @param argb32 Must not be alpha premultiplied.
     */
    public void setArgb32At(
            int x,
            int y,
            int argb32) {
        
        final WritableRaster raster = this.image.getRaster();

        final ColorModel colorModel = this.image.getColorModel();

        this.tmpDataLazy = colorModel.getDataElements(
                argb32,
                this.tmpDataLazy);

        raster.setDataElements(x, y, this.tmpDataLazy);
    }
    
    /*
     * Derived single pixel methods.
     */

    /**
     * @return The alpha premultiplied ARGB 32 at the specified location.
     */
    public int getPremulArgb32At(
            int x,
            int y) {

        final int argb32 = this.getArgb32At(x, y);
        
        return BindingColorUtils.toPremulAxyz32(argb32);
    }

    /**
     * Does SRC_OVER blending.
     * 
     * @param premulArgb32 Alpha premultiplied 32 bits ARGB color to blend.
     */
    public void drawPointAt(int x, int y, int premulArgb32) {
        final int srcPremulArgb32 = premulArgb32;
        
        final int newPremulArgb32;
        if (Argb32.isOpaque(srcPremulArgb32)) {
            // No need to get previous color for blending.
            newPremulArgb32 = srcPremulArgb32;
        } else {
            final int dstPremulArgb32 = this.getPremulArgb32At(x, y);
            
            newPremulArgb32 =
                    BindingColorUtils.blendPremulAxyz32_srcOver(
                            srcPremulArgb32,
                            dstPremulArgb32);
        }

        final int newArgb32 =
                BindingColorUtils.toNonPremulAxyz32(newPremulArgb32);
        
        this.setArgb32At(x, y, newArgb32);
    }
    
    /*
     * Bulk methods, for performances.
     * 
     * For bulk methods, adding pixel format and alpha premultiplied
     * arguments to allow for various kinds of input and output,
     * because the overhead is relatively small.
     */
    
    /**
     * Retrieves the whole image.
     * 
     * @param color32Arr (out) Must not be helper's image pixel array,
     *        else behavior is undefined.
     * @param pixelFormat Pixel format to use for output.
     * @param premul Whether output must be alpha-premultiplied.
     */
    public void getPixelsInto(
            int[] color32Arr,
            int color32ArrScanlineStride,
            //
            BihPixelFormat pixelFormat,
            boolean premul) {
        final int width = this.image.getWidth();
        final int height = this.image.getHeight();
        this.getPixelsInto(
            0, 0, width, height,
            color32Arr,
            color32ArrScanlineStride,
            pixelFormat,
            premul);
    }
    
    /**
     * Top-left pixel of src rectangle will be put at index 0
     * in the output array.
     * 
     * @param color32Arr (out) Must not be helper's image pixel array,
     *        else behavior is undefined.
     * @param pixelFormat Pixel format to use for output.
     * @param premul Whether output must be alpha-premultiplied.
     */
    public void getPixelsInto(
        int sx,
        int sy,
        int sxSpan,
        int sySpan,
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormat,
        boolean premul) {

        final int width = sxSpan;
        final int height = sySpan;

        if (MUST_READ_BULK_PIXELS_WITH_DRAW_IMAGE) {
            
            // Need to reset output pixels before drawing into it
            // with image, for it does blending
            // (matters for non-opaque images).
            zeroizePixels(
                color32Arr,
                color32ArrScanlineStride,
                width,
                height);
            
            final BihPixelFormat tmpPixelFormat;
            if (MUST_ALWAYS_USE_ARGB_FOR_PIXELS_READING_IMAGE) {
                tmpPixelFormat = BihPixelFormat.ARGB32;
            } else {
                tmpPixelFormat = pixelFormat;
            }
            final BufferedImage tmpImage = newBufferedImageWithIntArray(
                color32Arr,
                color32ArrScanlineStride,
                height,
                tmpPixelFormat,
                premul);
            final Graphics2D tmpG = tmpImage.createGraphics();
            try {
                tmpG.drawImage(
                    this.image,
                    //
                    0, // dx1
                    0, // dy1
                    width, // dx2 (exclusive)
                    height, // dy2 (exclusive)
                    //
                    sx, // sx1
                    sy, // sy1
                    sx + sxSpan, // sx2 (exclusive)
                    sy + sySpan, // sy2 (exclusive)
                    //
                    null);
            } finally {
                tmpG.dispose();
            }

            /*
             * Eventually reordering components as expected.
             */

            if (MUST_ALWAYS_USE_ARGB_FOR_PIXELS_READING_IMAGE) {
                final BihPixelFormat pixelFormatTo = pixelFormat;
                ensurePixelFormatFromArgb32(
                    color32Arr,
                    color32ArrScanlineStride,
                    width,
                    height,
                    pixelFormatTo);
            }
        } else {
            for (int j = 0; j < height; j++) {
                final int lineOffset =
                    j * color32ArrScanlineStride;
                for (int i = 0; i < width; i++) {
                    final int index = lineOffset + i;
                    final int color32 = getPixelAt(
                        sx + i,
                        sy + j,
                        pixelFormat,
                        premul);
                    color32Arr[index] = color32;
                }
            }
        }
    }
    
    public void clearRect(
            int x, int y, int xSpan, int ySpan,
            int argb32) {
        for (int j = 0; j < ySpan; j++) {
            final int py = y + j;
            for (int i = 0; i < xSpan; i++) {
                final int px = x + i;
                this.setArgb32At(px, py, argb32);
            }
        }
    }
    
    public void fillRect(
            int x, int y, int xSpan, int ySpan,
            int premulArgb32) {
        for (int j = 0; j < ySpan; j++) {
            final int py = y + j;
            for (int i = 0; i < xSpan; i++) {
                final int px = x + i;
                this.drawPointAt(
                        px,
                        py,
                        premulArgb32);
            }
        }
    }

    /*
     * 
     */
    
    public void invertPixels(int x, int y, int xSpan, int ySpan) {
        for (int j = 0; j < ySpan; j++) {
            final int py = y + j;
            for (int i = 0; i < xSpan; i++) {
                final int px = x + i;
                final int argb32 = this.getArgb32At(px, py);
                final int invertedArgb32 =
                        Argb32.inverted(argb32);
                this.setArgb32At(px, py, invertedArgb32);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param pixelFormat Format for the returned pixel.
     * @param premul Whether the returned pixel must be alpha premultiplied. 
     * @return The pixel at the specified location.
     */
    private int getPixelAt(
            int x,
            int y,
            //
            BihPixelFormat pixelFormat,
            boolean premul) {

        int argb32 = this.getArgb32At(x, y);
        
        if (premul) {
            argb32 = BindingColorUtils.toPremulAxyz32(argb32);
        }

        return toPixelFromArgb32(
                argb32,
                pixelFormat);
    }
    
    /*
     * 
     */
    
    private static int toPixelFromArgb32(
            int argb32,
            BihPixelFormat pixelFormat) {
        switch (pixelFormat) {
        case ARGB32: {
            return argb32;
        }
        case ABGR32: {
            return BindingColorUtils.toAbgr32FromArgb32(argb32);
        }
        case RGBA32: {
            return BindingColorUtils.toRgba32FromArgb32(argb32);
        }
        default:
            throw new IllegalArgumentException("" + pixelFormat);
        }
    }

    /*
     * 
     */
    
    private static void zeroizePixels(
            int[] color32Arr,
            int color32ArrScanlineStride,
            int width,
            int height) {
        for (int y = 0; y < height; y++) {
            final int lineOffset = y * color32ArrScanlineStride;
            for (int x = 0; x < width; x++) {
                final int index = lineOffset + x;
                color32Arr[index] = 0;
            }
        }
    }
    
    /**
     * The input pixels must be in ARGB32 format.
     */
    private static void ensurePixelFormatFromArgb32(
            int[] color32Arr,
            int color32ArrScanlineStride,
            int width,
            int height,
            BihPixelFormat pixelFormatTo) {
        
        if (pixelFormatTo == BihPixelFormat.ARGB32) {
            // Nothing to do.
            return;
        }

        for (int y = 0; y < height; y++) {
            final int lineOffset = y * color32ArrScanlineStride;
            if (pixelFormatTo == BihPixelFormat.ABGR32) {
                for (int x = 0; x < width; x++) {
                    final int index = lineOffset + x;
                    color32Arr[index] =
                            BindingColorUtils.toAbgr32FromArgb32(
                                    color32Arr[index]);
                }
            } else if (pixelFormatTo == BihPixelFormat.RGBA32) {
                for (int x = 0; x < width; x++) {
                    final int index = lineOffset + x;
                    color32Arr[index] =
                            BindingColorUtils.toRgba32FromArgb32(
                                    color32Arr[index]);
                }
            } else {
                throw new IllegalArgumentException("" + pixelFormatTo);
            }
        }
    }

    /*
     * 
     */

    private static int computeCptMask(int bitSize, int cptIndex) {
        NbrsUtils.requireInRange(-1, 3, cptIndex, "cptIndex");
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
    
    /*
     * 
     */
    
    private static void checkNumBands(int expectedNumBands, int actualNumBands) {
        if (actualNumBands != expectedNumBands) {
            throw new IllegalArgumentException(
                    "numBands : expected " + expectedNumBands
                    + ", got " + actualNumBands);
        }
    }
    
    private static void checkBandDataIsConsistentWithArgb32(
            ColorModel colorModel,
            SampleModel sampleModel,
            int band) {
        checkCptSizeIs8(sampleModel, band);
        
        checkConversionToFromDataStableFor(
                colorModel,
                CPT_EXTREME_BITS_BY_BAND[band]);
    }
    
    private static void checkCptSizeIs8(SampleModel sampleModel, int band) {
        final int cptSize = sampleModel.getSampleSize(band);
        if (cptSize != 8) {
            throw new IllegalArgumentException(
                    "component for band " + band
                    + " is not on 8 bits, but on " + cptSize + " bits");
        }
    }
    
    private static void checkConversionToFromDataStableFor(
            ColorModel colorModel,
            int argb32) {
        final Object data = colorModel.getDataElements(
                argb32,
                null);
        // Assuming bits are in the right order
        // (bits swapping shouldn't happen, too weird).
        final int argb32FromData = colorModel.getRGB(data);
        if (argb32FromData != argb32) {
            throw new IllegalArgumentException(
                    "conversion not stable : "
                    + Argb32.toString(argb32FromData)
                    + " != " + Argb32.toString(argb32));
        }
    }
    
    /*
     * 
     */
    
    /**
     * Keeping this method around, for the knowledge it contains,
     * and in case ever wanting to use raster getSample()
     * and setSample() methods.
     */
    @SuppressWarnings("unused")
    private static boolean computeIsSafeToUseRasterSample(BufferedImage image) {
        final WritableRaster raster = image.getRaster();
        final ColorModel colorModel = image.getColorModel();
        final int transferType = colorModel.getTransferType();

        final int numBands = raster.getNumBands();

        final ColorSpace colorSpace = colorModel.getColorSpace();
        final int colorSpaceType = colorSpace.getType();
        
        /*
         * Not sure how to use Raster.getSample(...)
         * and Raster.setSample(...) methods
         * if these conditions don't hold.
         * 
         * NB: For some reason, ColorModel.getRed(Object) etc.,
         * which are used if we don't use Raster.getSample(...) etc.,
         * don't support "DataBuffer.TYPE_SHORT" by default,
         * so adding it here might save us from throws.
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
}
