/*
 * Copyright 2019-2025 Jeff Hain
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PackedColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * Helper class to read and write pixels from/into a BufferedImage,
 * faster and/or more reliably than with BufferedImage.getRGB(),
 * BufferedImage.setRGB(), or even Graphics.drawImage().
 * 
 * Also provides methods for creating images wrapped around
 * a specified int array and/or with specific ARGB components order.
 * 
 * Non-goals:
 * - images resizing.
 * - optimization with parallelization.
 * - optimization causing much garbage,
 *   such as creating an intermediary ARGB image
 *   to accelerate copy between two images of slow types.
 * 
 * Static methods are thread-safe.
 * Non-static methods are not, even single pixel read methods.
 */
public class BufferedImageHelper {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * TODO awt For a 500k pixels image, if pixel format is not ARGB,
     * it makes drawImage() much slower : about 15 ms instead of about 2 ms
     * (i.e. as slow as reading the image pixel by pixel).
     * As a result, we always use ARGB for reading pixels,
     * and then convert pixels to the proper format afterwards.
     */
    static final BihPixelFormat DRAW_IMAGE_FAST_DST_PIXEL_FORMAT = BihPixelFormat.ARGB32;
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Formats of pixels managed efficiently by this helper.
     * Covers all BufferedImage.TYPE_INT_XXX formats
     * and a few more.
     * 
     * For formats with alpha,
     * doesn't indicate whether the pixel is alpha premultiplied:
     * it has to be specified aside.
     */
    public enum BihPixelFormat {
        /**
         * Usually the best format for performances
         * and compatibility with JDK treatments
         * (unless for those that would not support alpha well).
         * Equivalent to BufferedImage.TYPE_INT_ARGB or
         * TYPE_INT_ARGB_PRE depending on whether using
         * alpha-premul.
         */
        ARGB32(true, true, 0, 1, 2, 3),
        /**
         * OpenGL-friendly format when native is little endian.
         */
        ABGR32(true, true, 0, 3, 2, 1),
        /**
         * OpenGL-friendly format when native is big endian.
         */
        RGBA32(true, false, 3, 0, 1, 2),
        /**
         * For completeness.
         */
        BGRA32(true, false, 3, 2, 1, 0),
        /*
         * Similar color formats but without alpha,
         * useful to allow for better performances,
         * less storage on disk, and because some
         * image saving treatments don't handle alpha well.
         * 
         * We can't define formats like RGBX24 or BGRX24,
         * because PackedColorModel considers
         * that a mask overflows a 24 bits pixel
         * if it's not within its 24 LSBits.
         */
        /**
         * Equivalent to ARGB32 where alpha would always be 0xFF,
         * regardless of the value of the 8 MSBits of the int.
         * Also equivalent to BufferedImage.TYPE_INT_RGB.
         */
        XRGB24(false, true, -1, 1, 2, 3),
        /**
         * Equivalent to ABGR32 where alpha would always be 0xFF,
         * regardless of the value of the 8 MSBits of the int.
         * Also equivalent to BufferedImage.TYPE_INT_BGR.
         */
        XBGR24(false, true, -1, 3, 2, 1);
        private final boolean hasAlpha;
        private final boolean areColorsInLsbElseMsb;
        private final int aIndex;
        private final int rIndex;
        private final int gIndex;
        private final int bIndex;
        private BihPixelFormat(
            boolean hasAlpha,
            boolean areColorsInLsbElseMsb,
            final int aIndex,
            final int rIndex,
            final int gIndex,
            final int bIndex) {
            this.hasAlpha = hasAlpha;
            this.areColorsInLsbElseMsb = areColorsInLsbElseMsb;
            this.aIndex = aIndex;
            this.rIndex = rIndex;
            this.gIndex = gIndex;
            this.bIndex = bIndex;
        }
        /**
         * @return True if has alpha.
         */
        public boolean hasAlpha() {
            return this.hasAlpha;
        }
        /**
         * @return True if colors (RGB or BGR) are in the 24 LSBits,
         *         false if they are in the 24 MSBits.
         */
        public boolean areColorsInLsbElseMsb() {
            return this.areColorsInLsbElseMsb;
        }
        /**
         * @return Index of alpha, starting at 0 for MSByte, or -1 if without.
         */
        public int aIndex() {
            return this.aIndex;
        }
        /**
         * @return Index of red, starting at 0 for MSByte.
         */
        public int rIndex() {
            return this.rIndex;
        }
        /**
         * @return Index of green, starting at 0 for MSByte.
         */
        public int gIndex() {
            return this.gIndex;
        }
        /**
         * @return Index of blue, starting at 0 for MSByte.
         */
        public int bIndex() {
            return this.bIndex;
        }
        /**
         * @return The format with same color components as this one
         *         and with alpha (this one if it already has alpha).
         */
        public BihPixelFormat withAlpha() {
            final BihPixelFormat ret;
            switch (this) {
                case XRGB24: ret = BihPixelFormat.ARGB32; break;
                case XBGR24: ret = BihPixelFormat.ABGR32; break;
                default: ret = this;
            }
            return ret;
        }
        public int toArgb32FromPixel(int pixel) {
            int ret = pixel;
            switch (this) {
                case ABGR32:
                case XBGR24: ret = BindingColorUtils.toArgb32FromAbgr32(ret); break;
                case RGBA32: ret = BindingColorUtils.toArgb32FromRgba32(ret); break;
                case BGRA32: ret = BindingColorUtils.toArgb32FromBgra32(ret); break;
                default: break;
            }
            if (!this.hasAlpha) {
                ret = Argb32.toOpaque(ret);
            }
            return ret;
        }
        public int toPixelFromArgb32(int argb32) {
            int ret = argb32;
            if (!this.hasAlpha) {
                // Always zeroizing (unused) alpha byte,
                // rather than letting it undefined.
                ret = (ret & 0xFFFFFF);
            }
            switch (this) {
                case ABGR32:
                case XBGR24: ret = BindingColorUtils.toAbgr32FromArgb32(ret); break;
                case RGBA32: ret = BindingColorUtils.toRgba32FromArgb32(ret); break;
                case BGRA32: ret = BindingColorUtils.toBgra32FromArgb32(ret); break;
                default: break;
            }
            return ret;
        }
        /*
         * Convenience methods for BufferedImage specificities.
         */
        /**
         * @return The index of the component corresponding to the specified band.
         * @throws IllegalArgumentException if the specified band does not correspond
         *         to A, R, G or B, i.e. is not in [0,3].
         */
        public int componentIndexForBand(int band) {
            final int ret;
            switch (band) {
                case A_BAND: ret = this.aIndex; break;
                case R_BAND: ret = this.rIndex; break;
                case G_BAND: ret = this.gIndex; break;
                case B_BAND: ret = this.bIndex; break;
                default: throw new IllegalArgumentException();
            }
            return ret;
        }
        /**
         * @return The corresponding buffered image type,
         *         or TYPE_CUSTOM if no specific one exists.
         * @throws IllegalArgumentException if this format
         *         has no alpha and premul is true.
         */
        public int toImageType(boolean premul) {
            if (premul && (!this.hasAlpha())) {
                throw new IllegalArgumentException();
            }
            final int ret;
            switch (this) {
                case ARGB32: ret =
                    (premul ? BufferedImage.TYPE_INT_ARGB_PRE
                        : BufferedImage.TYPE_INT_ARGB); break;
                case XRGB24: ret = BufferedImage.TYPE_INT_RGB; break;
                case XBGR24: ret = BufferedImage.TYPE_INT_BGR; break;
                default: ret = BufferedImage.TYPE_CUSTOM; break;
            }
            return ret;
        }
        /**
         * NB: The eventual alpha part of image type
         * is not covered by BihPixelFormat,
         * so it must be treated aside.
         * 
         * @return The corresponding BihPixelFormat, of null if none.
         */
        public static BihPixelFormat fromImageType(int imageType) {
            final BihPixelFormat ret;
            switch (imageType) {
                case BufferedImage.TYPE_INT_ARGB:
                case BufferedImage.TYPE_INT_ARGB_PRE: ret = BihPixelFormat.ARGB32; break;
                case BufferedImage.TYPE_INT_RGB: ret = BihPixelFormat.XRGB24; break;
                case BufferedImage.TYPE_INT_BGR: ret = BihPixelFormat.XBGR24; break;
                default: ret = null; break;
            }
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Type of color model avoiding.
     */
    private enum MySinglePixelCmaType {
        /**
         * Means using the color model.
         */
        NONE,
        INT_BIH_PIXEL_FORMAT,
        USHORT_555_RGB,
        USHORT_565_RGB,
        USHORT_GRAY,
        BYTE_GRAY;
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
    
    private static final int[] RGB_BAND_ARR = new int[] {R_BAND, G_BAND, B_BAND};
    private static final int[] RGBA_BAND_ARR = new int[] {R_BAND, G_BAND, B_BAND, A_BAND};
    
    private static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0); 
    
    /*
     * 
     */
    
    private final BufferedImage image;
    
    private final int imageWidth;
    
    private final int imageHeight;
    
    /**
     * -1 if could not be computed.
     * Mandatory if using arrays.
     */
    private final int scanlineStride;
    
    /**
     * Null if could not compute a matching BihPixelFormat.
     */
    private final BihPixelFormat pixelFormat;
    
    private final boolean isCmaAllowed;
    private final boolean isAduAllowed;
    
    private final MySinglePixelCmaType singlePixelCmaType;
    
    private final int[] intPixelArr;
    
    private final short[] shortPixelArr;
    
    private final byte[] bytePixelArr;
    
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
    
    /**
     * Uses true for allowColorModelAvoiding
     * and for allowArrayDirectUse.
     * 
     * @throws NullPointerException if the specified image is null.
     */
    public BufferedImageHelper(BufferedImage image) {
        this(
            image,
            true, // allowColorModelAvoiding
            true); // allowArrayDirectUse
    }
    
    /**
     * @param allowColorModelAvoiding If true, avoiding
     *        color model for pixels reads/writes when possible
     *        (and not too tricky).
     *        Note that color model might end up being only avoided
     *        for some methods, for example for bulk methods (using drawImage())
     *        but not for single pixel methods, causing inconsistencies,
     *        although in practice with basic images it boils down to
     *        a difference in brightness for gray image types (cf. below).
     *        There are two reasons to do that are:
     *        First, to improve performances, because color model
     *        pixel<->color conversions often preserve the value
     *        but still use much CPU, and because it allows 
     *        to retrieve alpha-premultiplied pixels directly
     *        (colorModel API only works with non-premultiplied values).
     *        Second, to properly read/write pixel values for TYPE_BYTE_GRAY
     *        and TYPE_USHORT_GRAY images, for which color model makes
     *        read colors lighter than pixel and written pixels
     *        darker than color. Ex.: with TYPE_BYTE_GRAY images,
     *        colorModel.getRGB() turns 0x50 pixel into 0xFF989898
     *        instead of 0xFF505050.
     * @param allowArrayDirectUse If true, this helper is allowed
     *        to use image internal array directly (which causes
     *        call to "theTrackable.setUntrackable()" in the buffer)),
     *        to improve performances.
     *        Has no effect if this helper does not avoid color model,
     *        in which case the array is never used directly.
     * @throws NullPointerException if the specified image is null.
     */
    public BufferedImageHelper(
        BufferedImage image,
        boolean allowColorModelAvoiding,
        boolean allowArrayDirectUse) {
        
        this.image = image;
        this.imageWidth = image.getWidth();
        this.imageHeight = image.getHeight();
        
        final Raster raster = image.getRaster();
        
        this.scanlineStride = getScanlineStride(image);
        
        // Implicit null check.
        final BihPixelFormat pixelFormat =
            computePixelFormat(image);
        this.pixelFormat = pixelFormat;
        
        final boolean isCmaAllowed = allowColorModelAvoiding;
        final boolean isAduAllowed = allowColorModelAvoiding && allowArrayDirectUse;
        this.isCmaAllowed = isCmaAllowed;
        this.isAduAllowed = isAduAllowed;
        
        final MySinglePixelCmaType singlePixelCmaType =
            computeSinglePixelCmaType(
                image,
                allowColorModelAvoiding,
                pixelFormat);
        this.singlePixelCmaType = singlePixelCmaType;
        
        int[] intArr = null;
        short[] shortArr = null;
        byte[] byteArr = null;
        if (isAduAllowed) {
            if ((singlePixelCmaType == MySinglePixelCmaType.INT_BIH_PIXEL_FORMAT)
                && hasSimpleArrayOfType(
                    image,
                    DataBuffer.TYPE_INT)) {
                final DataBufferInt buffer =
                    (DataBufferInt) raster.getDataBuffer();
                intArr = buffer.getData();
                
            } else if (((singlePixelCmaType == MySinglePixelCmaType.USHORT_555_RGB)
                || (singlePixelCmaType == MySinglePixelCmaType.USHORT_565_RGB)
                || (singlePixelCmaType == MySinglePixelCmaType.USHORT_GRAY))
                && hasSimpleArrayOfType(
                    image,
                    DataBuffer.TYPE_USHORT)) {
                final DataBufferUShort buffer =
                    (DataBufferUShort) raster.getDataBuffer();
                shortArr = buffer.getData();
                
            } else if ((singlePixelCmaType == MySinglePixelCmaType.BYTE_GRAY)
                && hasSimpleArrayOfType(
                    image,
                    DataBuffer.TYPE_BYTE)) {
                final DataBufferByte buffer =
                    (DataBufferByte) raster.getDataBuffer();
                byteArr = buffer.getData();
            }
        }
        this.intPixelArr = intArr;
        this.shortPixelArr = shortArr;
        this.bytePixelArr = byteArr;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[(");
        sb.append(this.imageWidth);
        sb.append("x");
        sb.append(this.imageHeight);
        sb.append(",type=");
        if (this.pixelFormat != null) {
            sb.append(this.pixelFormat);
        } else {
            sb.append(this.image.getType());
        }
        if (this.image.isAlphaPremultiplied()) {
            sb.append(",premul");
        }
        if (this.isCmaAllowed) {
            sb.append(",cma");
        }
        if (this.intPixelArr != null) {
            sb.append(",intArr");
        }
        if (this.shortPixelArr != null) {
            sb.append(",shortArr");
        }
        if (this.bytePixelArr != null) {
            sb.append(",byteArr");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * @return The image this helper works with.
     */
    public BufferedImage getImage() {
        return this.image;
    }
    
    /**
     * @return The BihPixelFormat format corresponding to the image,
     *         or null if none.
     */
    public BihPixelFormat getPixelFormat() {
        return this.pixelFormat;
    }
    
    /**
     * Note that isColorModelAvoidingAllowed() being false
     * implies isArrayDirectUseAllowed() being false.
     * 
     * @return True if color model avoiding is allowed.
     */
    public boolean isColorModelAvoidingAllowed() {
        return this.isCmaAllowed;
    }
    
    /**
     * Note that isArrayDirectUseAllowed() being true
     * implies isColorModelAvoidingAllowed() being true.
     * 
     * @return True if array direct use is allowed.
     */
    public boolean isArrayDirectUseAllowed() {
        return this.isAduAllowed;
    }
    
    /*
     * 
     */
    
    /**
     * @return The corresponding BihPixelFormat format, or null if none.
     * @throws NullPointerException if the specified image is null.
     */
    public static BihPixelFormat computePixelFormat(BufferedImage image) {
        
        // Implicit null check.
        final ColorModel colorModel = image.getColorModel();
        final int numColorCpt = colorModel.getNumColorComponents();
        final int numCpt = colorModel.getNumComponents();
        final int pixelSize = colorModel.getPixelSize();
        
        BihPixelFormat ret = null;
        /*
         * Not bothering to check color space type,
         * which should be ICC_ColorSpace.TYPE_RGB
         * in practice, for that should be redundant
         * with the detailed checks we already do.
         */
        if ((colorModel instanceof PackedColorModel)
            && (numColorCpt == 3)
            && (((numCpt == 3) && (pixelSize == 24))
                || ((numCpt == 4) && (pixelSize == 32)))) {
            final PackedColorModel pcm = (PackedColorModel) colorModel;
            final int mr = pcm.getMask(R_BAND);
            final int mg = pcm.getMask(G_BAND);
            final int mb = pcm.getMask(B_BAND);
            if (mg == 0x0000FF00) {
                // Computing alpha-less format.
                if ((mr == 0x00FF0000)
                    && (mb == 0x000000FF)) {
                    ret = BihPixelFormat.XRGB24;
                } else if ((mr == 0x000000FF)
                    && (mb == 0x00FF0000)) {
                    ret = BihPixelFormat.XBGR24;
                }
                // Eventual promotion to alpha.
                if ((ret != null)
                    && (numCpt == 4)) {
                    final int ma = pcm.getMask(A_BAND);
                    if (ma == 0xFF000000) {
                        ret = ret.withAlpha();
                    }
                }
            } else if (mg == 0x00FF0000) {
                // In this case we only have formats with alpha.
                if (numCpt == 4) {
                    final int ma = pcm.getMask(A_BAND);
                    if (ma == 0x000000FF) {
                        if ((mr == 0xFF000000)
                            && (mb == 0x0000FF00)) {
                            ret = BihPixelFormat.RGBA32;
                        } else if ((mr == 0x0000FF00)
                            && (mb == 0xFF000000)) {
                            ret = BihPixelFormat.BGRA32;
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    /*
     * Utilities to create buffered images wrapped on an int array of pixels.
     */
    
    /**
     * @param pixelArr The int array of pixels to use, or null for the array
     *        to use to be created and owned by the internal DataBufferInt
     *        (i.e. no call to "theTrackable.setUntrackable()", unless
     *        the array is retrieved afterwards with DataBufferInt.getData()).
     * @param scanlineStride Scanline stride to use. Must be >= width.
     * @param width Image width. Must be >= 1.
     * @param height Image height. Must be >= 1.
     * @param imageType Type of the backing BufferedImage.
     *        Only supporting BufferedImage.TYPE_INT_XXX types
     *        {TYPE_INT_ARGB, TYPE_INT_ARGB_PRE, TYPE_INT_BGR, TYPE_INT_RGB}.
     * @return The created buffered image, with 32 bits pixels if there is
     *         alpha component, and 24 bits pixels otherwise.
     * @throws IllegalArgumentException if the specified width or height
     *         are not strictly positive, or the specified scanline stride
     *         is inferior to width, or this triplet of values is too large
     *         for int range or for the specified array length (if any).
     * @throws ArithmeticException if the specified width and height
     *         cause int overflow when multiplied with each other.
     * @throws OutOfMemoryError if the specified array is null
     *         and the array to create is too large for VM limit.
     */
    public static BufferedImage newBufferedImageWithIntArray(
        int[] pixelArr,
        int scanlineStride,
        //
        int width,
        int height,
        //
        int imageType) {
        
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
        
        // Can use BihPixelFormat, because it covers
        // all the image type we want to support.
        final BihPixelFormat pixelFormat =
            BihPixelFormat.fromImageType(
                imageType);
        if (pixelFormat == null) {
            throw new IllegalArgumentException("" + imageType);
        }
        
        final boolean isRasterPremultiplied =
            (imageType == BufferedImage.TYPE_INT_ARGB_PRE);
        
        return newBufferedImageWithIntArray(
            pixelArr,
            scanlineStride,
            width,
            height,
            //
            pixelFormat,
            isRasterPremultiplied);
    }
    
    /**
     * @param pixelArr The int array of pixels to use, or null for the array
     *        to use to be created and owned by the internal DataBufferInt
     *        (i.e. no call to "theTrackable.setUntrackable()", unless
     *        the array is retrieved afterwards with DataBufferInt.getData()).
     * @param scanlineStride Scanline stride to use. Must be >= width.
     * @param width Image width. Must be >= 1.
     * @param height Image height. Must be >= 1.
     * @param pixelFormat The pixel format to use.
     * @param premul Whether pixels must be alpha premultiplied.
     * @return The created buffered image, with 32 bits pixels if there is
     *         alpha component, and 24 bits pixels otherwise.
     * @throws NullPointerException if the specified pixel format is null.
     * @throws IllegalArgumentException if the specified width or height
     *         are not strictly positive, or the specified scanline stride
     *         is inferior to width, or this triplet of values is too large
     *         for int range or for the specified array length (if any),
     *         or the specified pixel format has no alpha and premul is true.
     * @throws ArithmeticException if the specified width and height
     *         cause int overflow when multiplied with each other.
     * @throws OutOfMemoryError if the specified array is null
     *         and the array to create is too large for VM limit.
     */
    public static BufferedImage newBufferedImageWithIntArray(
        int[] pixelArr,
        int scanlineStride,
        //
        int width,
        int height,
        //
        BihPixelFormat pixelFormat,
        boolean premul) {
        // Implicit null check.
        if ((!pixelFormat.hasAlpha()) && premul) {
            throw new IllegalArgumentException(
                "premul can't be true for " + pixelFormat);
        }
        return newBufferedImageWithIntArray(
            pixelArr,
            scanlineStride,
            width,
            height,
            //
            premul,
            pixelFormat.aIndex(),
            //
            pixelFormat.rIndex(),
            pixelFormat.gIndex(),
            pixelFormat.bIndex());
    }
    
    /**
     * Components indexes increase from MSByte to LSByte.
     * 
     * @param pixelArr The int array of pixels to use, or null for the array
     *        to use to be created and owned by the internal DataBufferInt
     *        (i.e. no call to "theTrackable.setUntrackable()", unless
     *        the array is retrieved afterwards with DataBufferInt.getData()).
     * @param scanlineStride Scanline stride to use. Must be >= width.
     * @param width Image width. Must be >= 1.
     * @param height Image height. Must be >= 1.
     * @param premul True if the raster must be alpha-premultiplied.
     * @param aIndex Index of alpha component byte,
     *        in [0,3], or -1 if no alpha component.
     * @param rIndex Index of red component byte,
     *        in [0,3] if having alpha, else in [1,3].
     * @param gIndex Index of green component byte,
     *        in [0,3] if having alpha, else in [1,3].
     * @param bIndex Index of blue component byte,
     *        in [0,3] if having alpha, else in [1,3].
     * @return The created buffered image, with 32 bits pixels
     *         if having alpha, and 24 bits pixels otherwise.
     * @throws IllegalArgumentException if the specified width or height
     *         are not strictly positive, or the specified scanline stride
     *         is inferior to width, or this triplet of values is too large
     *         for int range or for the specified array length (if any),
     *         or component indexes are out of range,
     *         or alpha index is -1 but premul is true,
     *         or some components use a same index.
     * @throws ArithmeticException if the specified width and height
     *         cause int overflow when multiplied with each other.
     * @throws OutOfMemoryError if the specified array is null
     *         and the array to create is too large for VM limit.
     */
    public static BufferedImage newBufferedImageWithIntArray(
        int[] pixelArr,
        int scanlineStride,
        //
        int width,
        int height,
        //
        boolean premul,
        int aIndex,
        //
        int rIndex,
        int gIndex,
        int bIndex) {
        
        NbrsUtils.requireSupOrEq(1, width, "width");
        NbrsUtils.requireSupOrEq(1, height, "height");
        NbrsUtils.requireSupOrEq(width, scanlineStride, "scanlineStride");
        {
            final int bound =
                (pixelArr != null) ? pixelArr.length : Integer.MAX_VALUE;
            // Check in long to avoid overflow/wrapping.
            NbrsUtils.requireInfOrEq(
                bound,
                (height - 1) * (long) scanlineStride + width,
                "(height-1)*scanlineStride+width");
        }
        
        final int area = NbrsUtils.timesExact(width, height);
        
        if (premul) {
            NbrsUtils.requireInRange(0, 3, aIndex, "aIndex");
        } else {
            NbrsUtils.requireInRange(-1, 3, aIndex, "aIndex");
        }
        
        final boolean gotAlpha = (aIndex >= 0);
        if (gotAlpha) {
            NbrsUtils.requireInRange(0, 3, rIndex, "rIndex");
            NbrsUtils.requireInRange(0, 3, gIndex, "gIndex");
            NbrsUtils.requireInRange(0, 3, bIndex, "bIndex");
        } else {
            NbrsUtils.requireInRange(1, 3, rIndex, "rIndex");
            NbrsUtils.requireInRange(1, 3, gIndex, "gIndex");
            NbrsUtils.requireInRange(1, 3, bIndex, "bIndex");
        }
        
        /*
         * 
         */
        
        final int bitSize = (gotAlpha ? 32 : 24);
        
        final int rMask = computeCptMask(rIndex);
        final int gMask = computeCptMask(gIndex);
        final int bMask = computeCptMask(bIndex);
        final int aMask = computeCptMask(aIndex);
        
        // Index unicity check (can't count on JDK treatments for it).
        // Fast no-GC check by using masks.
        {
            int allMask = rMask | gMask | bMask;
            if (gotAlpha) {
                allMask |= aMask;
            }
            if (Integer.bitCount(allMask) != bitSize) {
                throw new IllegalArgumentException(
                    "some components share a same index");
            }
        }
        
        /*
         * 
         */
        
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
                premul,
                DataBuffer.TYPE_INT);
        } else {
            bitMasks = new int[] {rMask, gMask, bMask};
            colorModel = new DirectColorModel(bitSize, rMask, gMask, bMask);
        }
        
        final SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT,
            width,
            height,
            scanlineStride,
            bitMasks);
        final DataBuffer dataBuffer;
        if (pixelArr != null) {
            dataBuffer = new DataBufferInt(
                pixelArr,
                area);
        } else {
            final int arrayLength =
                (height - 1) * scanlineStride + width;
            dataBuffer = new DataBufferInt(arrayLength);
        }
        final Point upperLeftLocation = new Point(0,0);
        final WritableRaster raster = Raster.createWritableRaster(
            sampleModel,
            dataBuffer,
            upperLeftLocation);
        final BufferedImage image = new BufferedImage(
            colorModel,
            raster,
            premul,
            null);
        return image;
    }
    
    /*
     * Internal int array.
     */
    
    /**
     * @return True if the specified image is backed by a single int array
     *         of pixels that can be used directly and simply:
     *         DataBufferInt buffer, one pixel per data element,
     *         raster (width,height) equal to image (width,height),
     *         no translation, and a retrievable scanline stride
     *         (getScanlineStride(image) != -1).
     */
    public static boolean hasSimpleIntArray(BufferedImage image) {
        return hasSimpleArrayOfType(
            image,
            DataBuffer.TYPE_INT);
    }
    
    /**
     * @return True if hasSimpleIntArray() is true with the specified image
     *         and its pixels match the specified pixel format and
     *         alpha premultiplied flag.
     */
    public static boolean hasCompatibleSimpleIntArray(
        BufferedImage image,
        BihPixelFormat pixelFormat,
        boolean premul) {
        return (getCstErrorNoCompatibleSimpleIntArray(
            image,
            pixelFormat,
            premul) == null);
    }
    
    /**
     * @param image An image.
     * @param pixelFormat Pixel format.
     * @param premul Alpha premultiplied flag.
     * @return The specified image.
     * @throws IllegalArgumentException if the specified image
     *         is not backed by a 'simple' int array
     *         (cf. hasSimpleIntArray()) compatible
     *         with the specified pixel and premul format.
     */
    public static BufferedImage requireCompatibleSimpleIntArray(
        BufferedImage image,
        BihPixelFormat pixelFormat,
        boolean premul) {
        final String error = getCstErrorNoCompatibleSimpleIntArray(
            image,
            pixelFormat,
            premul);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return image;
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
    public static int[] getIntArray(BufferedImage image) {
        final WritableRaster raster = image.getRaster();
        final DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
        return dataBuffer.getData();
    }
    
    /**
     * @return The scanline stride of the backing array,
     *         or -1 if could not be found.
     */
    public static int getScanlineStride(BufferedImage image) {
        final int ret;
        final SampleModel sampleModel = image.getSampleModel();
        if (sampleModel instanceof SinglePixelPackedSampleModel) {
            /*
             * For TYPE_INT_XXX and TYPE_USHORT_XXX_RGB images.
             */
            ret = ((SinglePixelPackedSampleModel) sampleModel).getScanlineStride();
        } else if (sampleModel instanceof ComponentSampleModel) {
            /*
             * For TYPE_(X)BYTE_XXX and TYPE_USHORT_GRAY images
             * (PixelInterleavedSampleModel subclass in practice).
             */
            ret = ((ComponentSampleModel) sampleModel).getScanlineStride();
        } else if (sampleModel instanceof MultiPixelPackedSampleModel) {
            /*
             * For TYPE_BYTE_BINARY images.
             */
            ret = ((MultiPixelPackedSampleModel) sampleModel).getScanlineStride();
        } else {
            ret = -1;
        }
        return ret;
    }
    
    /*
     * Internal array.
     */
    
    /**
     * @return The int array directly used, or null if none.
     */
    public int[] getIntArrayDirectlyUsed() {
        return this.intPixelArr;
    }
    
    /**
     * @return The short array directly used, or null if none.
     */
    public short[] getShortArrayDirectlyUsed() {
        return this.shortPixelArr;
    }
    
    /**
     * @return The byte array directly used, or null if none.
     */
    public byte[] getByteArrayDirectlyUsed() {
        return this.bytePixelArr;
    }
    
    /**
     * @return Internal array scanline stride,
     *         if could be retrieved, else -1.
     */
    public int getScanlineStride() {
        return this.scanlineStride;
    }
    
    /*
     * Base single pixel methods.
     */
    
    /**
     * @param premul Whether the result must be alpha-premultiplied.
     * @return The ARGB 32 at the specified location.
     * @throws ArrayIndexOutOfBoundsException if the specified position
     *         is out of range.
     */
    public int getArgb32At(
        int x,
        int y,
        boolean premul) {
        /*
         * Most common and optimized cases first.
         */
        final int ret;
        if (this.intPixelArr != null) {
            final int index = this.toIndex(x, y);
            final int pixel = this.intPixelArr[index];
            ret = this.convertPixelToArgb32_INT_BIH_PIXEL_FORMAT(
                pixel,
                premul);
            
        } else if (this.shortPixelArr != null) {
            final int index = this.toIndex(x, y);
            final short pixel = this.shortPixelArr[index];
            if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_555_RGB) {
                ret = this.convertPixelToArgb32_USHORT_555_RGB(
                    pixel,
                    premul);
            } else if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_565_RGB) {
                ret = this.convertPixelToArgb32_USHORT_565_RGB(
                    pixel,
                    premul);
            } else {
                // USHORT_GRAY
                ret = this.convertPixelToArgb32_USHORT_GRAY(
                    pixel,
                    premul);
            }
            
        } else if (this.bytePixelArr != null) {
            final int index = this.toIndex(x, y);
            final byte pixel = this.bytePixelArr[index];
            ret = this.convertPixelToArgb32_BYTE_GRAY(
                pixel,
                premul);
            
        } else {
            ret = this.getArgb32At_raster(x, y, premul);
        }
        return ret;
    }
    
    /**
     * If the image has no alpha, actually meaning is doesn't support
     * transparency, you might want to give an opaque color here.
     * 
     * @param argb32 An ARGB 32 value.
     * @param premul Whether the specified value is alpha-premultiplied.
     * @throws ArrayIndexOutOfBoundsException if the specified position
     *         is out of range.
     */
    public void setArgb32At(
        int x,
        int y,
        int argb32,
        boolean premul) {
        /*
         * Most common and optimized cases first.
         */
        if (this.intPixelArr != null) {
            final int index = this.toIndex(x, y);
            final int pixel =
                this.convertArgb32ToPixel_INT_BIH_PIXEL_FORMAT(
                    argb32,
                    premul);
            this.intPixelArr[index] = pixel;
            
        } else if (this.shortPixelArr != null) {
            final int index = this.toIndex(x, y);
            final short pixel;
            if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_555_RGB) {
                pixel = this.convertArgb32ToPixel_USHORT_555_RGB(
                    argb32,
                    premul);
            } else if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_565_RGB) {
                pixel = this.convertArgb32ToPixel_USHORT_565_RGB(
                    argb32,
                    premul);
            } else if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_GRAY) {
                pixel = this.convertArgb32ToPixel_USHORT_GRAY(
                    argb32,
                    premul);
            } else {
                throw new AssertionError();
            }
            this.shortPixelArr[index] = pixel;
            
        } else if (this.bytePixelArr != null) {
            final int index = this.toIndex(x, y);
            final byte pixel =
                this.convertArgb32ToPixel_BYTE_GRAY(
                    argb32,
                    premul);
            this.bytePixelArr[index] = pixel;
            
        } else {
            this.setArgb32At_raster(x, y, argb32, premul);
        }
    }
    
    /*
     * Derived single pixel methods.
     */
    
    /**
     * @return The ARGB 32, not alpha premultiplied, at the specified location.
     * @throws ArrayIndexOutOfBoundsException if the specified position
     *         is out of range.
     */
    public int getNonPremulArgb32At(
        int x,
        int y) {
        final boolean premul = false;
        return this.getArgb32At(x, y, premul);
    }
    
    /**
     * @return The alpha premultiplied ARGB 32 at the specified location.
     * @throws ArrayIndexOutOfBoundsException if the specified position
     *         is out of range.
     */
    public int getPremulArgb32At(
        int x,
        int y) {
        final boolean premul = true;
        return this.getArgb32At(x, y, premul);
    }
    
    /**
     * If the image has no alpha, actually meaning is doesn't support
     * transparency, you might want to give an opaque color here.
     * 
     * @param argb32 Must not be alpha premultiplied.
     * @throws ArrayIndexOutOfBoundsException if the specified position
     *         is out of range.
     */
    public void setNonPremulArgb32At(
        int x,
        int y,
        int argb32) {
        final boolean premul = false;
        this.setArgb32At(x, y, argb32, premul);
    }
    
    /**
     * If the image has no alpha, actually meaning is doesn't support
     * transparency, you might want to give an opaque color here.
     * 
     * @param premulArgb32 Must be alpha premultiplied.
     * @throws ArrayIndexOutOfBoundsException if the specified position
     *         is out of range.
     */
    public void setPremulArgb32At(
        int x,
        int y,
        int premulArgb32) {
        final boolean premul = true;
        this.setArgb32At(x, y, premulArgb32, premul);
    }
    
    /*
     * 
     */
    
    /**
     * @param argb32 An ARGB 32 value.
     * @param premul Whether the specified value is alpha-premultiplied.
     * @throws IllegalArgumentException if a specified position
     *         is out of range.
     */
    public void clearRect(
        int x, int y, int width, int height,
        int argb32,
        boolean premul) {
        
        /*
         * Checks.
         */
        
        this.checkRectInImage(x, "x", y, "y", width, "width", height, "height");
        
        if ((width == 0) || (height == 0)) {
            // Easy.
            return;
        }
        
        /*
         * Clear.
         */
        
        final Object data =
            this.convertArgb32ToData(argb32, premul);
        
        if (this.intPixelArr != null) {
            final int pixel = ((int[]) data)[0];
            fillRect_intArr(
                this.intPixelArr,
                this.scanlineStride,
                x,
                y,
                width,
                height,
                //
                pixel);
        } else if (this.shortPixelArr != null) {
            final short pixel = ((short[]) data)[0];
            fillRect_shortArr(
                this.shortPixelArr,
                this.scanlineStride,
                x,
                y,
                width,
                height,
                //
                pixel);
        } else if (this.bytePixelArr != null) {
            final byte pixel = ((byte[]) data)[0];
            fillRect_byteArr(
                this.bytePixelArr,
                this.scanlineStride,
                x,
                y,
                width,
                height,
                //
                pixel);
        } else {
            final WritableRaster raster = this.image.getRaster();
            for (int j = 0; j < height; j++) {
                final int py = y + j;
                for (int i = 0; i < width; i++) {
                    final int px = x + i;
                    raster.setDataElements(px, py, data);
                }
            }
        }
    }
    
    /*
     * Bulk get/set methods, for performances.
     * 
     * For bulk get/set methods, adding pixel format argument
     * (instead of sticking to ARGB32),
     * to allow for various kinds of input and output,
     * because the overhead is relatively small.
     */
    
    /**
     * Behavior is undefined if helper image array
     * is the specified one.
     * 
     * @param srcX X in image of the area to copy.
     * @param srcY Y in image of the area to copy.
     * @param color32Arr (out) Must not be helper's image pixel array,
     *        else behavior is undefined.
     * @param color32ArrScanlineStride Must be >= width.
     * @param pixelFormatTo Pixel format to use for output.
     * @param premulTo Whether output must be alpha-premultiplied.
     * @param dstX X in array of the area to copy.
     * @param dstY Y in array of the area to copy.
     * @param width Width of the area to copy. Must be >= 0.
     * @param height Height of the area to copy. Must be >= 0.
     * @throws NullPointerException if the specified array or
     *         pixel format is null.
     * @throws IllegalArgumentException if the specified pixel format
     *         has no alpha and the specified premul is true,
     *         or the specified area and scanline stride
     *         are not consistent with image dimensions
     *         and array length.
     */
    public void getPixelsInto(
        int srcX,
        int srcY,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatTo,
        boolean premulTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        /*
         * Checks.
         */
        
        LangUtils.requireNonNull(color32Arr);
        // Implicit null check.
        if ((!pixelFormatTo.hasAlpha()) && premulTo) {
            throw new IllegalArgumentException(
                "premul can't be true for " + pixelFormatTo);
        }
        
        if (color32Arr == this.intPixelArr) {
            /*
             * Behavior undefined, so we can throw.
             * Should guard against most misuses.
             */
            throw new IllegalArgumentException(
                "color32Arr is image array");
        }
        
        this.checkRectInImage(srcX, "srcX", srcY, "srcY", width, "width", height, "height");
        
        final int dw = color32ArrScanlineStride;
        NbrsUtils.requireSupOrEq(width, dw, "color32ArrScanlineStride");
        NbrsUtils.requireInRange(0, dw - 1, dstX, "dstX");
        NbrsUtils.requireInRange(0, dw - dstX, width, "width");
        final int dh = computeArrayHeight(
            color32Arr.length,
            color32ArrScanlineStride,
            dstX,
            width);
        NbrsUtils.requireInRange(0, dh - 1, dstY, "dstY");
        NbrsUtils.requireInRange(0, dh - dstY, height, "height");
        
        if ((width == 0) || (height == 0)) {
            // Easy.
            return;
        }
        
        /*
         * Getting pixels.
         */
        
        this.getPixelsInto_noCheck(
            srcX,
            srcY,
            //
            color32Arr,
            color32ArrScanlineStride,
            pixelFormatTo,
            premulTo,
            dstX,
            dstY,
            //
            width,
            height);
    }
    
    /**
     * Behavior is undefined if helper image array
     * is the specified one.
     * 
     * @param color32Arr (in) Must not be helper's image pixel array,
     *        else behavior is undefined.
     * @param color32ArrScanlineStride Must be >= width.
     * @param pixelFormatFrom Pixel format of input.
     * @param premulFrom Whether input is alpha-premultiplied.
     * @param srcX X in array of the area to copy.
     * @param srcY Y in array of the area to copy.
     * @param dstX X in image of the area to copy.
     * @param dstY Y in image of the area to copy.
     * @param width Width of the area to copy. Must be >= 0.
     * @param height Height of the area to copy. Must be >= 0.
     * @throws NullPointerException if the specified array or
     *         pixel format is null.
     * @throws IllegalArgumentException if the specified pixel format
     *         has no alpha and the specified premul is true,
     *         or the specified area and scanline stride
     *         are not consistent with image dimensions
     *         and array length.
     */
    public void setPixelsFrom(
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        int srcX,
        int srcY,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        /*
         * Checks.
         */
        
        LangUtils.requireNonNull(color32Arr);
        // Implicit null check.
        if ((!pixelFormatFrom.hasAlpha()) && premulFrom) {
            throw new IllegalArgumentException(
                "premul can't be true for " + pixelFormatFrom);
        }
        
        if (color32Arr == this.intPixelArr) {
            // Behavior undefined per spec in this case,
            // so we have the right to throw
            // if we happen to know they are the same.
            throw new IllegalArgumentException(
                "color32Arr is image array");
        }
        
        this.checkRectInImage(dstX, "dstX", dstY, "dstY", width, "width", height, "height");
        
        final int sw = color32ArrScanlineStride;
        NbrsUtils.requireSupOrEq(width, sw, "color32ArrScanlineStride");
        NbrsUtils.requireInRange(0, sw - 1, srcX, "srcX");
        NbrsUtils.requireInRange(0, sw - srcX, width, "width");
        final int sh = computeArrayHeight(
            color32Arr.length,
            color32ArrScanlineStride,
            srcX,
            width);
        NbrsUtils.requireInRange(0, sh - 1, srcY, "srcY");
        NbrsUtils.requireInRange(0, sh - srcY, height, "height");
        
        if ((width == 0) || (height == 0)) {
            // Easy.
            return;
        }
        
        /*
         * Getting pixels.
         */
        
        this.setPixelsFrom_noCheck(
            color32Arr,
            color32ArrScanlineStride,
            pixelFormatFrom,
            premulFrom,
            srcX,
            srcY,
            //
            dstX,
            dstY,
            //
            width,
            height);
    }
    
    /*
     * Bulk copy.
     */
    
    /**
     * Respects both helpers directives regarding
     * color model avoidance and array direct use.
     * 
     * Behavior is undefined if helpers images
     * are different but share a same array.
     * 
     * @param helperFrom Helper of source image.
     * @param srcX X in source image of the area to copy.
     * @param srcY Y in source image of the area to copy.
     * @param helperTo Helper of destination image.
     * @param dstX X in destination image of the area to copy.
     * @param dstY Y in destination image of the area to copy.
     * @param width Width of the area to copy. Must be >= 0.
     * @param height Height of the area to copy. Must be >= 0.
     * @throws NullPointerException if a helper is null.
     * @throws IllegalArgumentException if the source area
     *         does not fit in source image,
     *         or the destination area does not fit in
     *         destination image, of source and destination
     *         images are a same object.
     */
    public static void copyImage(
        BufferedImageHelper helperFrom,
        int srcX,
        int srcY,
        //
        BufferedImageHelper helperTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        /*
         * Checks.
         */
        
        // Implicit null checks.
        final BufferedImage imageFrom = helperFrom.getImage();
        final BufferedImage imageTo = helperTo.getImage();
        
        if (imageFrom == imageTo) {
            throw new IllegalArgumentException(
                "images are a same object");
        }
        if (sameAndNotNull(helperFrom.intPixelArr, helperTo.intPixelArr)
            || sameAndNotNull(helperFrom.shortPixelArr, helperTo.shortPixelArr)
            || sameAndNotNull(helperFrom.bytePixelArr, helperTo.bytePixelArr)) {
            /*
             * Behavior undefined, so we can throw.
             * Should guard against most misuses.
             */
            throw new IllegalArgumentException(
                "images use a same array");
        }
        
        final int sw = imageFrom.getWidth();
        final int sh = imageFrom.getHeight();
        final int dw = imageTo.getWidth();
        final int dh = imageTo.getHeight();
        NbrsUtils.requireInRange(0, sw - 1, srcX, "srcX");
        NbrsUtils.requireInRange(0, sh - 1, srcY, "srcY");
        NbrsUtils.requireInRange(0, dw - 1, dstX, "dstX");
        NbrsUtils.requireInRange(0, dh - 1, dstY, "dstY");
        
        final int maxXRange = Math.min(sw - srcX, dw - dstX);
        final int maxYRange = Math.min(sh - srcY, dh - dstY);
        NbrsUtils.requireInRange(0, maxXRange, width, "width");
        NbrsUtils.requireInRange(0, maxYRange, height, "height");
        
        if ((width == 0) || (height == 0)) {
            // Easy.
            return;
        }
        
        /*
         * Copying image.
         */
        
        copyImage_noCheck(
            helperFrom,
            srcX,
            srcY,
            //
            helperTo,
            dstX,
            dstY,
            //
            width,
            height);
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    boolean isColorModelAvoidedForSinglePixelMethods() {
        return this.singlePixelCmaType != MySinglePixelCmaType.NONE;
    }
    
    boolean isArrayDirectlyUsed() {
        return (this.intPixelArr != null)
            || (this.shortPixelArr != null)
            || (this.bytePixelArr != null);
    }
    
    /**
     * drawImage() max component error (over) estimation,
     * to guard against bad drawImage() uses. 
     * Not public because of semi-redundant (hyperstatic) arguments,
     * and returns a work value.
     * 
     * drawImage() seem to use alpha-premultiplied values internally
     * even if both its input and output images are not alpha-premultiplied
     * (and even if none has an alpha component,
     * i.e. using XRGB24 for graphics image doesn't help).
     * For example, it converts (non-premul) 0x01374EBD
     * into (premul) 0x01000001 and then into (non-premul) 0x010000FF.
     * 
     * In some cases, drawImage() can give results off by one
     * compared to our single-pixel treatments
     * (ex.: (ARGB32,p)->(ARGB32,np):
     *     when expecting 0x16B98046
     *  drawImage() gives 0x16B97F46),
     * but this is just a rounding error and only seem to occur
     * for alpha <= 0xFE, i.e. for non-opaque colors,
     * for which having rounding errors is expectable
     * due to alpha-premultiplication.
     * 
     * If drawing a TYPE_INT_ARGB_PRE image containing
     * 0x80302010 pixels, which corresponds to 0x80604020 when
     * not alpha-premultiplied, into a TYPE_INT_RGB image,
     * whatever its previous content the resulting image
     * will contain 0xFF302010 instead of 0xFF604020,
     * i.e. it will contain the alpha-premultiplied
     * color components, with an opaque alpha.
     * 
     * @return A value equal or superior to the max error on components
     *         when using drawImage() in the specified context,
     *         compared to using get/set with non-premul ARGB32.
     */
    static int getDrawImageMaxCptDelta(
        int imageTypeFrom,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        //
        int imageTypeTo,
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
        /*
         * From most generic/common to most specific/unusual cases.
         */
        
        final int max = 0xFF;
        
        if (isArgb32PrePermu(imageTypeFrom, pixelFormatFrom, premulFrom)) {
            if (isArgb32PrePermu(imageTypeTo, pixelFormatTo, premulTo)) {
                return 0;
            } else if (isArgb32Permu(imageTypeTo, pixelFormatTo, premulTo)) {
                return 1;
            } else {
                return max;
            }
        }
        
        if (isArgb32Permu(imageTypeFrom, pixelFormatFrom, premulFrom)) {
            if (isArgb32PrePermu(imageTypeTo, pixelFormatTo, premulTo)) {
                return 0;
            } else {
                return max;
            }
        }
        
        if (isRgb24Permu(imageTypeFrom, pixelFormatFrom)) {
            if (isGray(imageTypeTo)
                || (imageTypeTo == BufferedImage.TYPE_BYTE_BINARY)
                || (imageTypeTo == BufferedImage.TYPE_BYTE_INDEXED)) {
                return max;
            } else {
                return 0;
            }
        }
        
        if (isUShortRgb(imageTypeFrom)) {
            if (isGray(imageTypeTo)
                || (imageTypeTo == BufferedImage.TYPE_BYTE_BINARY)
                || (imageTypeTo == BufferedImage.TYPE_BYTE_INDEXED)) {
                return max;
            } else {
                return 0;
            }
        }
        
        if (isGray(imageTypeFrom)) {
            if (imageTypeTo == BufferedImage.TYPE_BYTE_INDEXED) {
                return max;
            } else {
                return 0;
            }
        }
        
        if (imageTypeFrom == BufferedImage.TYPE_BYTE_BINARY) {
            if (imageTypeTo == BufferedImage.TYPE_BYTE_INDEXED) {
                return max;
            } else {
                return 0;
            }
        }
        
        if (imageTypeFrom == BufferedImage.TYPE_BYTE_INDEXED) {
            if ((imageTypeTo == BufferedImage.TYPE_BYTE_BINARY)
                || isGray(imageTypeTo)) {
                return max;
            } else {
                return 0;
            }
        }
        
        // No idea: worst case.
        return max;
    }
    
    static boolean isDrawImageMaxCptDeltaSmallEnough(
        int drawImageMaxCptDelta) {
        return (drawImageMaxCptDelta <= 1);
    }
    
    static boolean mustUseDrawImageOverPixelLoop(
        int imageTypeFrom,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        //
        int imageTypeTo,
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
        final int maxCptDelta =
            getDrawImageMaxCptDelta(
                imageTypeFrom,
                pixelFormatFrom,
                premulFrom,
                imageTypeTo,
                pixelFormatTo,
                premulTo);
        final boolean ret;
        if (isDrawImageMaxCptDeltaSmallEnough(maxCptDelta)) {
            ret = isDrawImageFasterThanPixelLoop(
                imageTypeFrom,
                pixelFormatFrom,
                imageTypeTo,
                pixelFormatTo);
        } else {
            ret = false;
        }
        return ret;
    }
    
    /**
     * Must only be used if
     * isDrawImageMaxCptDeltaSmallEnough(getDrawImageMaxCptDelta())
     * returns true for the specified arguments,
     * as other cases are not handled by this treatment.
     */
    static boolean isDrawImageFasterThanPixelLoop(
        int imageTypeFrom,
        BihPixelFormat pixelFormatFrom,
        //
        int imageTypeTo,
        BihPixelFormat pixelFormatTo) {
        
        if ((pixelFormatTo == BihPixelFormat.ABGR32)
            || (pixelFormatTo == BihPixelFormat.RGBA32)
            || (pixelFormatTo == BihPixelFormat.BGRA32)) {
            if ((imageTypeFrom == BufferedImage.TYPE_3BYTE_BGR)
                || (imageTypeFrom == BufferedImage.TYPE_4BYTE_ABGR)
                || (imageTypeFrom == BufferedImage.TYPE_4BYTE_ABGR_PRE)) {
                /*
                 * For these cases, speeds about equivalent or faster,
                 * so we stick to drawImage().
                 */
                return true;
            } else {
                /*
                 * For all the other cases of output,
                 * drawImage() is slow.
                 */
                return false;
            }
        }
        
        if ((pixelFormatFrom == BihPixelFormat.ABGR32)
            || (pixelFormatFrom == BihPixelFormat.RGBA32)
            || (pixelFormatFrom == BihPixelFormat.BGRA32)) {
            if ((imageTypeTo == BufferedImage.TYPE_INT_ARGB_PRE)
                || (imageTypeTo == BufferedImage.TYPE_INT_ARGB)) {
                /*
                 * For these cases of output,
                 * drawImage() is usually slightly slower.
                 */
                return false;
            }
        }

        return true;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @throws ArrayIndexOutOfBoundsException if not in image.
     */
    private int toIndex(int x, int y) {
        if (((x|y) < 0)
            || (x >= this.imageWidth)
            || (y >= this.imageHeight)) {
            // Same message as JDK.
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
        return y * this.scanlineStride + x;
    }
    
    private int toIndex_noCheck(int x, int y) {
        return y * this.scanlineStride + x;
    }
    
    private static MySinglePixelCmaType computeSinglePixelCmaType(
        BufferedImage image,
        boolean allowColorModelAvoiding,
        BihPixelFormat pixelFormat) {
        
        final Raster raster = image.getRaster();
        // Means data element arrays of length 1.
        final boolean gotScalarPixels =
            (raster.getNumDataElements() == 1);
        
        final int imageType = image.getType();
        
        final MySinglePixelCmaType ret;
        if (allowColorModelAvoiding
            && gotScalarPixels) {
            if (pixelFormat != null) {
                ret = MySinglePixelCmaType.INT_BIH_PIXEL_FORMAT;
            } else if (imageType == BufferedImage.TYPE_USHORT_555_RGB) {
                ret = MySinglePixelCmaType.USHORT_555_RGB;
            } else if (imageType == BufferedImage.TYPE_USHORT_565_RGB) {
                ret = MySinglePixelCmaType.USHORT_565_RGB;
            } else if (imageType == BufferedImage.TYPE_USHORT_GRAY) {
                ret = MySinglePixelCmaType.USHORT_GRAY;
            } else if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
                ret = MySinglePixelCmaType.BYTE_GRAY;
            } else {
                ret = MySinglePixelCmaType.NONE;
            }
        } else {
            ret = MySinglePixelCmaType.NONE;
        }
        return ret;
    }
    
    /*
     * 
     */
    
    private static boolean hasSimpleArrayOfType(
        BufferedImage image,
        int bufferType) {
        return (getCstErrorNoSimpleArrayOfType(
            image,
            bufferType) == null);
    }
    
    /**
     * @return String with constant error message,
     *         or null if the specified image has
     *         a simple array of the specified type,
     *         i.e. with one pixel per data element,
     *         raster (width,height) equal to image (width,height),
     *         no translation, and a retrievable scanline stride
     *         (getScanlineStride(image) != -1).
     */
    private static String getCstErrorNoSimpleArrayOfType(
        BufferedImage image,
        int bufferType) {
        
        final WritableRaster raster = image.getRaster();
        final DataBuffer buffer = raster.getDataBuffer();
        
        String error = null;
        
        if (buffer == null) {
            error = "no buffer";
        }
        
        if ((error == null)
            && (buffer.getDataType() != bufferType)) {
            error = "bad buffer type";
        }
        
        if ((error == null)
            && (raster.getNumDataElements() != 1)) {
            error = "multiple data elements";
        }
        
        if ((error == null)
            && ((raster.getWidth() != image.getWidth())
                || (raster.getHeight() != image.getHeight()))) {
            error = "raster (width,height) != image (width,height)";
        }
        
        if ((error == null)
            && ((raster.getSampleModelTranslateX() != 0)
                || (raster.getSampleModelTranslateY() != 0))) {
            error = "raster translation";
        }
        
        if ((error == null)
            && (getScanlineStride(image) == -1)) {
            error = "scanline stride not retrievable";
        }
        
        return error;
    }
    
    /**
     * @return String with constant error message,
     *         or null if the specified image has
     *         a compatible simple int array.
     */
    private static String getCstErrorNoCompatibleSimpleIntArray(
        BufferedImage image,
        BihPixelFormat pixelFormat,
        boolean premul) {
        
        String error = getCstErrorNoSimpleArrayOfType(
            image,
            DataBuffer.TYPE_INT);
        
        if (error == null) {
            if (premul) {
                if (!image.isAlphaPremultiplied()) {
                    error = "image alpha not premultiplied";
                }
            } else {
                if (image.isAlphaPremultiplied()) {
                    error = "image alpha premultiplied";
                }
            }
        }
        
        if (error == null) {
            // We need PackedColorModel to be able to check components masks
            // (but, as drawImage(), we don't want to use its eventual
            // pixel-to-color conversion treatments).
            if (image.getColorModel() instanceof PackedColorModel) {
                final PackedColorModel colorModel = (PackedColorModel) image.getColorModel();
                final WritableRaster raster = image.getRaster();
                final SampleModel sampleModel = raster.getSampleModel();
                
                /*
                 * Checks from fastest to heaviests,
                 * even though all are quite fast.
                 */
                
                final int expectedNumBands = (pixelFormat.hasAlpha() ? 4 : 3);
                if (sampleModel.getNumBands() != expectedNumBands) {
                    error = "bad num bands";
                }
                
                if (error == null) {
                    final int[] expectedBandArr =
                        (pixelFormat.hasAlpha() ? RGBA_BAND_ARR : RGB_BAND_ARR);
                    boolean foundBadMask = false;
                    for (int band : expectedBandArr) {
                        final int componentIndex =
                            pixelFormat.componentIndexForBand(band);
                        final int expectedMask = 0xFF << ((3 - componentIndex) << 3);
                        if (colorModel.getMask(band) != expectedMask) {
                            foundBadMask = true;
                            break;
                        }
                    }
                    if (foundBadMask) {
                        error = "bad component mask";
                    }
                }
            } else {
                error = "bad color model";
            }
        }
        
        return error;
    }
    
    /*
     * 
     */
    
    /**
     * Does not check (x,y), does not check
     * for int array (not called if having int array).
     * Useful for bulk methods.
     */
    private int getArgb32At_noCheck_shortByteArrOrRaster(
        int x,
        int y,
        boolean premul) {
        final int ret;
        if (this.shortPixelArr != null) {
            final int index = this.toIndex_noCheck(x, y);
            final short pixel = this.shortPixelArr[index];
            if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_555_RGB) {
                ret = this.convertPixelToArgb32_USHORT_555_RGB(
                    pixel,
                    premul);
            } else if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_565_RGB) {
                ret = this.convertPixelToArgb32_USHORT_565_RGB(
                    pixel,
                    premul);
            } else {
                // USHORT_GRAY
                ret = this.convertPixelToArgb32_USHORT_GRAY(
                    pixel,
                    premul);
            }
            
        } else if (this.bytePixelArr != null) {
            final int index = this.toIndex_noCheck(x, y);
            final byte pixel = this.bytePixelArr[index];
            ret = this.convertPixelToArgb32_BYTE_GRAY(
                pixel,
                premul);
            
        } else {
            ret = this.getArgb32At_raster(x, y, premul);
        }
        return ret;
    }
    
    /**
     * Does not check (x,y), does not check
     * for int array (not called if having int array).
     * Useful for bulk methods.
     */
    private void setArgb32At_noCheck_shortByteArrOrRaster(
        int x,
        int y,
        int argb32,
        boolean premul) {
        if (this.shortPixelArr != null) {
            final int index = this.toIndex_noCheck(x, y);
            final short pixel;
            if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_555_RGB) {
                pixel = this.convertArgb32ToPixel_USHORT_555_RGB(
                    argb32,
                    premul);
            } else if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_565_RGB) {
                pixel = this.convertArgb32ToPixel_USHORT_565_RGB(
                    argb32,
                    premul);
            } else if (this.singlePixelCmaType == MySinglePixelCmaType.USHORT_GRAY) {
                pixel = this.convertArgb32ToPixel_USHORT_GRAY(
                    argb32,
                    premul);
            } else {
                throw new AssertionError();
            }
            this.shortPixelArr[index] = pixel;
            
        } else if (this.bytePixelArr != null) {
            final int index = this.toIndex_noCheck(x, y);
            final byte pixel =
                this.convertArgb32ToPixel_BYTE_GRAY(
                    argb32,
                    premul);
            this.bytePixelArr[index] = pixel;
            
        } else {
            this.setArgb32At_raster(x, y, argb32, premul);
        }
    }
    
    /*
     * ARGB32 get/set using raster.
     */
    
    private int getArgb32At_raster(
        int x,
        int y,
        boolean premul) {
        
        final WritableRaster raster = this.image.getRaster();
        
        final Object data = raster.getDataElements(
            x,
            y,
            this.tmpDataLazy);
        this.tmpDataLazy = data;
        
        return this.convertDataToArgb32(data, premul);
    }
    
    private void setArgb32At_raster(
        int x,
        int y,
        int argb32,
        boolean premul) {
        
        final WritableRaster raster = this.image.getRaster();
        
        final Object data =
            this.convertArgb32ToData(argb32, premul);
        
        raster.setDataElements(x, y, data);
    }
    
    /*
     * convertXxxToArgb32
     */
    
    private int convertDataToArgb32(
        Object data,
        boolean premul) {
        
        final int argb32;
        switch (this.singlePixelCmaType) {
            case NONE: {
                argb32 = this.convertDataToArgb32_colorModel(
                    data,
                    premul);
            } break;
            case INT_BIH_PIXEL_FORMAT: {
                argb32 = this.convertPixelToArgb32_INT_BIH_PIXEL_FORMAT(
                    ((int[]) data)[0],
                    premul);
            } break;
            case USHORT_555_RGB: {
                argb32 = this.convertPixelToArgb32_USHORT_555_RGB(
                    ((short[]) data)[0],
                    premul);
            } break;
            case USHORT_565_RGB: {
                argb32 = this.convertPixelToArgb32_USHORT_565_RGB(
                    ((short[]) data)[0],
                    premul);
            } break;
            case USHORT_GRAY: {
                argb32 = this.convertPixelToArgb32_USHORT_GRAY(
                    ((short[]) data)[0],
                    premul);
            } break;
            case BYTE_GRAY: {
                argb32 = this.convertPixelToArgb32_BYTE_GRAY(
                    ((byte[]) data)[0],
                    premul);
            } break;
            default: throw new AssertionError();
        }
        return argb32;
    }
    
    private int convertDataToArgb32_colorModel(
        Object data,
        boolean premul) {
        final ColorModel colorModel = this.image.getColorModel();
        int argb32 = colorModel.getRGB(data);
        if (premul) {
            argb32 = BindingColorUtils.toPremulAxyz32(argb32);
        }
        return argb32;
    }
    
    private int convertPixelToArgb32_INT_BIH_PIXEL_FORMAT(
        int pixel,
        boolean premul) {
        int argb32 = this.pixelFormat.toArgb32FromPixel(pixel);
        if (premul != this.image.isAlphaPremultiplied()) {
            if (premul) {
                argb32 = BindingColorUtils.toPremulAxyz32(argb32);
            } else {
                argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
            }
        }
        return argb32;
    }
    
    private int convertPixelToArgb32_USHORT_555_RGB(
        short pixel,
        boolean premul) {
        final int bMask = ((1 << 5) - 1);
        final int gMask = (bMask << 5);
        final int rMask = (gMask << 5);
        final int r8Hi = ((pixel & rMask) >>> (10 - 3));
        final int g8Hi = ((pixel & gMask) >>> (5 - 3));
        final int b8Hi = ((pixel & bMask) << 3);
        final int r8 = (r8Hi | (r8Hi >>> 5));
        final int g8 = (g8Hi | (g8Hi >>> 5));
        final int b8 = (b8Hi | (b8Hi >>> 5));
        int argb32 = BindingColorUtils.toAbcd32_noCheck(0xFF, r8, g8, b8);
        if (premul) {
            argb32 = BindingColorUtils.toPremulAxyz32(argb32);
        }
        return argb32;
    }
    
    private int convertPixelToArgb32_USHORT_565_RGB(
        short pixel,
        boolean premul) {
        final int bMask = ((1 << 5) - 1);
        final int gMask = ((1 << 6) - 1) << 5;
        final int rMask = ((1 << 5) - 1) << 11;
        final int r8Hi = ((pixel & rMask) >>> (11 - 3));
        final int g8Hi = ((pixel & gMask) >>> (5 - 2));
        final int b8Hi = ((pixel & bMask) << 3);
        final int r8 = (r8Hi | (r8Hi >>> 5));
        final int g8 = (g8Hi | (g8Hi >>> 6));
        final int b8 = (b8Hi | (b8Hi >>> 5));
        int argb32 = BindingColorUtils.toAbcd32_noCheck(0xFF, r8, g8, b8);
        if (premul) {
            argb32 = BindingColorUtils.toPremulAxyz32(argb32);
        }
        return argb32;
    }
    
    private int convertPixelToArgb32_USHORT_GRAY(
        short pixel,
        boolean premul) {
        // Can only use the MSByte of the value.
        final int uValHi = pixel & 0xFF00;
        int argb32 = 0xFF000000 | (uValHi << 8) | uValHi | (uValHi >>> 8);
        if (premul) {
            argb32 = BindingColorUtils.toPremulAxyz32(argb32);
        }
        return argb32;
    }
    
    private int convertPixelToArgb32_BYTE_GRAY(
        byte pixel,
        boolean premul) {
        final int uVal = pixel & 0xFF;
        int argb32 = 0xFF000000 | (uVal << 16) | (uVal << 8) | uVal;
        if (premul) {
            argb32 = BindingColorUtils.toPremulAxyz32(argb32);
        }
        return argb32;
    }
    
    /*
     * convertArgb32ToXxx
     */
    
    private Object convertArgb32ToData(
        int argb32,
        boolean premul) {
        
        final Object data;
        switch (this.singlePixelCmaType) {
            case NONE: {
                data = this.convertArgb32ToData_colorModel(
                    argb32,
                    premul);
            } break;
            case INT_BIH_PIXEL_FORMAT: {
                final int pixel =
                    this.convertArgb32ToPixel_INT_BIH_PIXEL_FORMAT(
                        argb32,
                        premul);
                final int[] dataImpl = this.ensureAndGetDataLazy_int();
                dataImpl[0] = pixel;
                data = dataImpl;
            } break;
            case USHORT_555_RGB: {
                final short pixel =
                    this.convertArgb32ToPixel_USHORT_555_RGB(
                        argb32,
                        premul);
                final short[] dataImpl = this.ensureAndGetDataLazy_short();
                dataImpl[0] = pixel;
                data = dataImpl;
            } break;
            case USHORT_565_RGB: {
                final short pixel =
                    this.convertArgb32ToPixel_USHORT_565_RGB(
                        argb32,
                        premul);
                final short[] dataImpl = this.ensureAndGetDataLazy_short();
                dataImpl[0] = pixel;
                data = dataImpl;
            } break;
            case USHORT_GRAY: {
                final short pixel =
                    this.convertArgb32ToPixel_USHORT_GRAY(
                        argb32,
                        premul);
                final short[] dataImpl = this.ensureAndGetDataLazy_short();
                dataImpl[0] = pixel;
                data = dataImpl;
            } break;
            case BYTE_GRAY: {
                final byte pixel =
                    this.convertArgb32ToPixel_BYTE_GRAY(
                        argb32,
                        premul);
                final byte[] dataImpl = this.ensureAndGetDataLazy_byte();
                dataImpl[0] = pixel;
                data = dataImpl;
            } break;
            default: throw new AssertionError();
        }
        return data;
    }
    
    private Object convertArgb32ToData_colorModel(
        int argb32,
        boolean premul) {
        if (premul) {
            argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
        }
        final ColorModel colorModel = this.image.getColorModel();
        final Object oldData = this.tmpDataLazy;
        final Object newData = colorModel.getDataElements(
            argb32,
            oldData);
        if (oldData == null) {
            this.tmpDataLazy = newData;
        }
        return newData;
    }
    
    private int convertArgb32ToPixel_INT_BIH_PIXEL_FORMAT(
        int argb32,
        boolean premul) {
        if (premul != this.image.isAlphaPremultiplied()) {
            if (premul) {
                argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
            } else {
                argb32 = BindingColorUtils.toPremulAxyz32(argb32);
            }
        }
        return this.pixelFormat.toPixelFromArgb32(argb32);
    }
    
    private short convertArgb32ToPixel_USHORT_555_RGB(
        int argb32,
        boolean premul) {
        if (premul) {
            argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
        }
        final int mask5 = ((1 << 5) - 1);
        final int rMask = (mask5 << (16 + 3));
        final int gMask = (mask5 << (8 + 3));
        final int bMask = (mask5 << 3);
        // s = already shifted
        final int r5s = ((argb32 & rMask) >>> (3 + 3 + 3));
        final int g5s = ((argb32 & gMask) >>> (3 + 3));
        final int b5s = ((argb32 & bMask) >>> 3);
        return (short) (r5s | g5s | b5s);
    }
    
    private short convertArgb32ToPixel_USHORT_565_RGB(
        int argb32,
        boolean premul) {
        if (premul) {
            argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
        }
        final int mask5 = ((1 << 5) - 1);
        final int mask6 = ((1 << 6) - 1);
        final int rMask = (mask5 << (16 + 3));
        final int gMask = (mask6 << (8 + 2));
        final int bMask = (mask5 << 3);
        // s = already shifted
        final int r5s = ((argb32 & rMask) >>> (3 + 2 + 3));
        final int g6s = ((argb32 & gMask) >>> (2 + 3));
        final int b5s = ((argb32 & bMask) >>> 3);
        return (short) (r5s | g6s | b5s);
    }
    
    private short convertArgb32ToPixel_USHORT_GRAY(
        int argb32,
        boolean premul) {
        if (premul) {
            argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
        }
        final int r8 = Argb32.getRed8(argb32);
        final int g8 = Argb32.getGreen8(argb32);
        final int b8 = Argb32.getBlue8(argb32);
        final int uVal = (int) ((r8 + g8 + b8) * (1.0 / 3) + 0.5);
        // Also using uVal for LSByte, for 0x00 to give 0x0000
        // and 0xFF to give 0xFFFF and have full range covered.
        return (short) ((uVal << 8) | uVal);
    }
    
    private byte convertArgb32ToPixel_BYTE_GRAY(
        int argb32,
        boolean premul) {
        if (premul) {
            argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
        }
        final int r8 = Argb32.getRed8(argb32);
        final int g8 = Argb32.getGreen8(argb32);
        final int b8 = Argb32.getBlue8(argb32);
        return (byte) ((r8 + g8 + b8) * (1.0 / 3) + 0.5);
    }
    
    /*
     * 
     */
    
    private void getPixelsInto_noCheck(
        int srcX,
        int srcY,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatTo,
        boolean premulTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if (this.intPixelArr != null) {
            this.getPixelsInto_noCheck_intArr(
                srcX,
                srcY,
                //
                color32Arr,
                color32ArrScanlineStride,
                pixelFormatTo,
                premulTo,
                dstX,
                dstY,
                //
                width,
                height);
        } else if (this.isColorModelAvoidingAllowed()
            && mustUseDrawImageOverPixelLoop(
                this.image.getType(),
                this.pixelFormat,
                this.image.isAlphaPremultiplied(),
                //
                DRAW_IMAGE_FAST_DST_PIXEL_FORMAT.toImageType(premulTo),
                DRAW_IMAGE_FAST_DST_PIXEL_FORMAT,
                premulTo)) {
            this.getPixelsInto_noCheck_drawImage(
                srcX,
                srcY,
                //
                color32Arr,
                color32ArrScanlineStride,
                pixelFormatTo,
                premulTo,
                dstX,
                dstY,
                //
                width,
                height);
        } else {
            this.getPixelsInto_noCheck_shortByteArrOrRaster(
                srcX,
                srcY,
                //
                color32Arr,
                color32ArrScanlineStride,
                pixelFormatTo,
                premulTo,
                dstX,
                dstY,
                //
                width,
                height);
        }
    }
    
    private void setPixelsFrom_noCheck(
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        int srcX,
        int srcY,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if (this.intPixelArr != null) {
            this.setPixelsFrom_noCheck_intArr(
                color32Arr,
                color32ArrScanlineStride,
                pixelFormatFrom,
                premulFrom,
                srcX,
                srcY,
                //
                dstX,
                dstY,
                //
                width,
                height);
        } else if (this.isColorModelAvoidingAllowed()
            && mustUseDrawImageOverPixelLoop(
                pixelFormatFrom.toImageType(premulFrom),
                pixelFormatFrom,
                premulFrom,
                //
                this.image.getType(),
                this.pixelFormat,
                this.image.isAlphaPremultiplied())) {
            this.setPixelsFrom_noCheck_drawImage(
                color32Arr,
                color32ArrScanlineStride,
                pixelFormatFrom,
                premulFrom,
                srcX,
                srcY,
                //
                dstX,
                dstY,
                //
                width,
                height);
        } else {
            this.setPixelsFrom_noCheck_shortByteArrOrRaster(
                color32Arr,
                color32ArrScanlineStride,
                pixelFormatFrom,
                premulFrom,
                srcX,
                srcY,
                //
                dstX,
                dstY,
                //
                width,
                height);
        }
    }
    
    private static void copyImage_noCheck(
        BufferedImageHelper helperFrom,
        int srcX,
        int srcY,
        //
        BufferedImageHelper helperTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        final BufferedImage imageFrom = helperFrom.getImage();
        final BufferedImage imageTo = helperTo.getImage();
        
        if ((helperFrom.intPixelArr != null)
            && (helperTo.intPixelArr != null)) {
            copyImage_noCheck_2intArr(
                helperFrom,
                srcX,
                srcY,
                //
                helperTo,
                dstX,
                dstY,
                //
                width,
                height);
        } else if (helperFrom.intPixelArr != null) {
            helperTo.setPixelsFrom_noCheck(
                helperFrom.intPixelArr,
                helperFrom.scanlineStride,
                helperFrom.pixelFormat,
                helperFrom.image.isAlphaPremultiplied(),
                srcX,
                srcY,
                //
                dstX,
                dstY,
                //
                width,
                height);
        } else if (helperTo.intPixelArr != null) {
            helperFrom.getPixelsInto_noCheck(
                srcX,
                srcY,
                //
                helperTo.intPixelArr,
                helperTo.scanlineStride,
                helperTo.pixelFormat,
                helperTo.image.isAlphaPremultiplied(),
                dstX,
                dstY,
                //
                width,
                height);
        } else if (helperFrom.isColorModelAvoidingAllowed()
            && helperTo.isColorModelAvoidingAllowed()
            && mustUseDrawImageOverPixelLoop(
                imageFrom.getType(),
                helperFrom.getPixelFormat(),
                imageFrom.isAlphaPremultiplied(),
                //
                imageTo.getType(),
                helperTo.getPixelFormat(),
                imageTo.isAlphaPremultiplied())) {
            copyImage_noCheck_drawImage(
                helperFrom,
                srcX,
                srcY,
                //
                helperTo,
                dstX,
                dstY,
                //
                width,
                height);
        } else {
            copyImage_noCheck_shortByteArrOrRaster(
                helperFrom,
                srcX,
                srcY,
                //
                helperTo,
                dstX,
                dstY,
                //
                width,
                height);
        }
    }
    
    /*
     * 
     */
    
    private void getPixelsInto_noCheck_intArr(
        int srcX,
        int srcY,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatTo,
        boolean premulTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        /*
         * If need rework,
         * doing it after each line copy,
         * to reduce cache misses.
         */
        final boolean needFormatRework =
            (pixelFormatTo != this.pixelFormat);
        final boolean needPremulRework =
            (premulTo != this.image.isAlphaPremultiplied());
        if ((!needFormatRework)
            && (!needPremulRework)
            && (width == this.scanlineStride)
            && (width == color32ArrScanlineStride)) {
            final int area = width * height;
            System.arraycopy(
                this.intPixelArr,
                srcY * this.scanlineStride,
                color32Arr,
                dstY * color32ArrScanlineStride,
                area);
        } else {
            final boolean isAlphaInMSByte = this.pixelFormat.areColorsInLsbElseMsb();
            for (int j = 0; j < height; j++) {
                final int srcLineOffset = (srcY + j) * this.scanlineStride + srcX;
                final int dstLineOffset = (dstY + j) * color32ArrScanlineStride + dstX;
                
                System.arraycopy(
                    this.intPixelArr,
                    srcLineOffset,
                    color32Arr,
                    dstLineOffset,
                    width);
                
                // Memory-friendly reverse-loop,
                // assuming arraycopy() is forward-looping.
                if (needFormatRework) {
                    for (int i = width; --i >= 0;) {
                        final int index = dstLineOffset + i;
                        int pixel = color32Arr[index];
                        int argb32 = this.pixelFormat.toArgb32FromPixel(pixel);
                        if (needPremulRework) {
                            if (premulTo) {
                                argb32 = BindingColorUtils.toPremulAxyz32(argb32);
                            } else {
                                argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
                            }
                        }
                        pixel = pixelFormatTo.toPixelFromArgb32(argb32);
                        color32Arr[index] = pixel;
                    }
                } else if (needPremulRework) {
                    for (int i = width; --i >= 0;) {
                        final int index = dstLineOffset + i;
                        int pixel = color32Arr[index];
                        if (premulTo) {
                            if (isAlphaInMSByte) {
                                pixel = BindingColorUtils.toPremulAxyz32(pixel);
                            } else {
                                pixel = BindingColorUtils.toPremulXyza32(pixel);
                            }
                        } else {
                            if (isAlphaInMSByte) {
                                pixel = BindingColorUtils.toNonPremulAxyz32(pixel);
                            } else {
                                pixel = BindingColorUtils.toNonPremulXyza32(pixel);
                            }
                        }
                        color32Arr[index] = pixel;
                    }
                }
            }
        }
    }
    
    private void setPixelsFrom_noCheck_intArr(
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        int srcX,
        int srcY,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        /*
         * If need rework,
         * doing it after each line copy,
         * to reduce cache misses.
         */
        final boolean needFormatRework =
            (pixelFormatFrom != this.pixelFormat);
        final boolean needPremulRework =
            (premulFrom != this.image.isAlphaPremultiplied());
        if ((!needFormatRework)
            && (!needPremulRework)
            && (width == this.scanlineStride)
            && (width == color32ArrScanlineStride)) {
            final int area = width * height;
            System.arraycopy(
                color32Arr,
                srcY * color32ArrScanlineStride,
                this.intPixelArr,
                dstY * this.scanlineStride,
                area);
        } else {
            final boolean isAlphaInMSByte = this.pixelFormat.areColorsInLsbElseMsb();
            for (int j = 0; j < height; j++) {
                final int srcLineOffset = (srcY + j) * color32ArrScanlineStride + srcX;
                final int dstLineOffset = (dstY + j) * this.scanlineStride + dstX;
                
                System.arraycopy(
                    color32Arr,
                    srcLineOffset,
                    this.intPixelArr,
                    dstLineOffset,
                    width);
                
                // Memory-friendly reverse-loop,
                // assuming arraycopy() is forward-looping.
                if (needFormatRework) {
                    for (int i = width; --i >= 0;) {
                        final int index = dstLineOffset + i;
                        int pixel = this.intPixelArr[index];
                        int argb32 = pixelFormatFrom.toArgb32FromPixel(pixel);
                        if (needPremulRework) {
                            if (premulFrom) {
                                argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
                            } else {
                                argb32 = BindingColorUtils.toPremulAxyz32(argb32);
                            }
                        }
                        pixel = this.pixelFormat.toPixelFromArgb32(argb32);
                        this.intPixelArr[index] = pixel;
                    }
                } else if (needPremulRework) {
                    for (int i = width; --i >= 0;) {
                        final int index = dstLineOffset + i;
                        int pixel = this.intPixelArr[index];
                        if (premulFrom) {
                            if (isAlphaInMSByte) {
                                pixel = BindingColorUtils.toNonPremulAxyz32(pixel);
                            } else {
                                pixel = BindingColorUtils.toNonPremulXyza32(pixel);
                            }
                        } else {
                            if (isAlphaInMSByte) {
                                pixel = BindingColorUtils.toPremulAxyz32(pixel);
                            } else {
                                pixel = BindingColorUtils.toPremulXyza32(pixel);
                            }
                        }
                        this.intPixelArr[index] = pixel;
                    }
                }
            }
        }
    }
    
    private static void copyImage_noCheck_2intArr(
        BufferedImageHelper helperFrom,
        int srcX,
        int srcY,
        //
        BufferedImageHelper helperTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        final int[] arrFrom = helperFrom.intPixelArr;
        final int scanlineStrideFrom = helperFrom.scanlineStride;
        final int[] arrTo = helperTo.intPixelArr;
        final int scanlineStrideTo = helperTo.scanlineStride;
        
        final BihPixelFormat pixelFormatFrom = helperFrom.pixelFormat;
        final boolean premulFrom = helperFrom.image.isAlphaPremultiplied();
        final BihPixelFormat pixelFormatTo = helperTo.pixelFormat;
        final boolean premulTo = helperTo.image.isAlphaPremultiplied();
        
        /*
         * If need rework,
         * doing it after each line copy,
         * to reduce cache misses.
         */
        final boolean needFormatRework =
            (pixelFormatFrom != pixelFormatTo);
        final boolean needPremulRework =
            (premulFrom != premulTo);
        if ((!needFormatRework)
            && (!needPremulRework)
            && (width == scanlineStrideFrom)
            && (width == scanlineStrideTo)) {
            final int areaTo = width * height;
            System.arraycopy(
                arrFrom,
                srcY * scanlineStrideFrom,
                arrTo,
                dstY * scanlineStrideTo,
                areaTo);
        } else {
            for (int j = 0; j < height; j++) {
                final int srcLineOffset = (srcY + j) * scanlineStrideFrom + srcX;
                final int dstLineOffset = (dstY + j) * scanlineStrideTo + dstX;
                
                System.arraycopy(
                    arrFrom,
                    srcLineOffset,
                    arrTo,
                    dstLineOffset,
                    width);
                
                // Memory-friendly reverse-loop,
                // assuming arraycopy() is forward-looping.
                if (needFormatRework) {
                    for (int i = width; --i >= 0;) {
                        final int index = dstLineOffset + i;
                        int pixel = arrTo[index];
                        int argb32 = pixelFormatFrom.toArgb32FromPixel(pixel);
                        if (needPremulRework) {
                            if (premulTo) {
                                argb32 = BindingColorUtils.toPremulAxyz32(argb32);
                            } else {
                                argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
                            }
                        }
                        pixel = pixelFormatTo.toPixelFromArgb32(argb32);
                        arrTo[index] = pixel;
                    }
                } else if (needPremulRework) {
                    final boolean isAlphaInMSByte =
                        pixelFormatTo.areColorsInLsbElseMsb();
                    for (int i = width; --i >= 0;) {
                        final int index = dstLineOffset + i;
                        int pixel = arrTo[index];
                        if (premulTo) {
                            if (isAlphaInMSByte) {
                                pixel = BindingColorUtils.toPremulAxyz32(pixel);
                            } else {
                                pixel = BindingColorUtils.toPremulXyza32(pixel);
                            }
                        } else {
                            if (isAlphaInMSByte) {
                                pixel = BindingColorUtils.toNonPremulAxyz32(pixel);
                            } else {
                                pixel = BindingColorUtils.toNonPremulXyza32(pixel);
                            }
                        }
                        arrTo[index] = pixel;
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private void getPixelsInto_noCheck_shortByteArrOrRaster(
        int srcX,
        int srcY,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatTo,
        boolean premulTo,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        for (int j = 0; j < height; j++) {
            final int sy = srcY + j;
            final int dstLineOffset =
                (dstY + j) * color32ArrScanlineStride + dstX;
            for (int i = 0; i < width; i++) {
                final int sx = srcX + i;
                final int argb32 =
                    this.getArgb32At_noCheck_shortByteArrOrRaster(
                        sx,
                        sy,
                        premulTo);
                final int color32 =
                    pixelFormatTo.toPixelFromArgb32(
                        argb32);
                final int indexTo = dstLineOffset + i;
                color32Arr[indexTo] = color32;
            }
        }
    }
    
    private void setPixelsFrom_noCheck_shortByteArrOrRaster(
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        int srcX,
        int srcY,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        for (int j = 0; j < height; j++) {
            final int dy = dstY + j;
            final int srcLineOffset =
                (srcY + j) * color32ArrScanlineStride + srcX;
            for (int i = 0; i < width; i++) {
                final int dx = dstX + i;
                final int indexFrom = srcLineOffset + i;
                final int color32 = color32Arr[indexFrom];
                final int argb32 =
                    pixelFormatFrom.toArgb32FromPixel(color32);
                this.setArgb32At_noCheck_shortByteArrOrRaster(
                    dx,
                    dy,
                    argb32,
                    premulFrom);
            }
        }
    }
    
    private static void copyImage_noCheck_shortByteArrOrRaster(
        BufferedImageHelper helperFrom,
        int srcX,
        int srcY,
        //
        BufferedImageHelper helperTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        final boolean premulTo = helperTo.image.isAlphaPremultiplied();
        
        for (int j = 0; j < height; j++) {
            final int sy = srcY + j;
            final int dy = dstY + j;
            for (int i = 0; i < width; i++) {
                final int sx = srcX + i;
                final int dx = dstX + i;
                final int argb32 =
                    helperFrom.getArgb32At_noCheck_shortByteArrOrRaster(
                        sx,
                        sy,
                        premulTo);
                helperTo.setArgb32At(dx, dy, argb32, premulTo);
            }
        }
    }
    
    /*
     * 
     */
    
    private void getPixelsInto_noCheck_drawImage(
        int srcX,
        int srcY,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatTo,
        boolean premulTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if ((color32ArrScanlineStride == 0) || (height == 0)) {
            /*
             * SampleModel creation would throw.
             * Nothing to do.
             */
            return;
        }
        
        if (this.image.getTransparency() != Transparency.OPAQUE) {
            // Need to reset destination pixels before using
            // drawImage(), for it does blending.
            fillRect_intArr(
                color32Arr,
                color32ArrScanlineStride,
                dstX,
                dstY,
                width,
                height,
                //
                0);
        }
        
        final BihPixelFormat tmpImageToPixelFormat = DRAW_IMAGE_FAST_DST_PIXEL_FORMAT;
        final BufferedImage tmpImageTo =
            newBufferedImageWithIntArray(
                color32Arr,
                color32ArrScanlineStride,
                //
                dstX + width,
                dstY + height,
                //
                tmpImageToPixelFormat,
                premulTo);
        final Graphics2D g = tmpImageTo.createGraphics();
        try {
            g.drawImage(
                this.image,
                //
                dstX, // dx1
                dstY, // dy1
                dstX + width, // dx2 (exclusive)
                dstY + height, // dy2 (exclusive)
                //
                srcX, // sx1
                srcY, // sy1
                srcX + width, // sx2 (exclusive)
                srcY + height, // sy2 (exclusive)
                //
                null);
        } finally {
            g.dispose();
        }
        
        /*
         * Eventually reordering components as expected
         * (premul or not is already ensured through tmp image).
         */
        
        if (pixelFormatTo != tmpImageToPixelFormat) {
            for (int j = 0; j < height; j++) {
                final int lineOffset =
                    (dstY + j) * color32ArrScanlineStride + dstX;
                for (int i = 0; i < width; i++) {
                    final int index = lineOffset + i;
                    color32Arr[index] =
                        pixelFormatTo.toPixelFromArgb32(
                            color32Arr[index]);
                }
            }
        }
    }
    
    private void setPixelsFrom_noCheck_drawImage(
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        int srcX,
        int srcY,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if ((color32ArrScanlineStride == 0) || (height == 0)) {
            /*
             * SampleModel creation would throw.
             * Nothing to do.
             */
            return;
        }
        
        final BufferedImage tmpImageFrom =
            newBufferedImageWithIntArray(
                color32Arr,
                color32ArrScanlineStride,
                //
                srcX + width,
                srcY + height,
                //
                pixelFormatFrom,
                premulFrom);
        final Graphics2D g = this.image.createGraphics();
        try {
            if (pixelFormatFrom.hasAlpha()) {
                // Need to reset destination pixels before using
                // drawImage(), for it does blending.
                g.setBackground(COLOR_TRANSPARENT);
                g.clearRect(dstX, dstY, width, height);
            }
            g.drawImage(
                tmpImageFrom,
                //
                dstX, // dx1
                dstY, // dy1
                dstX + width, // dx2 (exclusive)
                dstY + height, // dy2 (exclusive)
                //
                srcX, // sx1
                srcY, // sy1
                srcX + width, // sx2 (exclusive)
                srcY + height, // sy2 (exclusive)
                //
                null);
        } finally {
            g.dispose();
        }
    }   
    
    private static void copyImage_noCheck_drawImage(
        BufferedImageHelper helperFrom,
        int srcX,
        int srcY,
        //
        BufferedImageHelper helperTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        final BufferedImage imageFrom = helperFrom.getImage();
        final BufferedImage imageTo = helperTo.getImage();
        
        final boolean srcHasAlpha =
            (imageFrom.getTransparency() == Transparency.TRANSLUCENT);
        
        final Graphics2D g = imageTo.createGraphics();
        try {
            if (srcHasAlpha) {
                // Need to reset destination pixels before using
                // drawImage(), for it does blending.
                g.setBackground(COLOR_TRANSPARENT);
                g.clearRect(dstX, dstY, width, height);
            }
            g.drawImage(
                imageFrom,
                //
                dstX, // dx1
                dstY, // dy1
                dstX + width, // dx2 (exclusive)
                dstY + height, // dy2 (exclusive)
                //
                srcX, // sx1
                srcY, // sy1
                srcX + width, // sx2 (exclusive)
                srcY + height, // sy2 (exclusive)
                //
                null);
        } finally {
            g.dispose();
        }
    }
    
    /*
     * 
     */
    
    private int[] ensureAndGetDataLazy_int() {
        int[] data = (int[]) this.tmpDataLazy;
        if (data == null) {
            data = new int[1];
            this.tmpDataLazy = data;
        }
        return data;
    }
    
    private short[] ensureAndGetDataLazy_short() {
        short[] data = (short[]) this.tmpDataLazy;
        if (data == null) {
            data = new short[1];
            this.tmpDataLazy = data;
        }
        return data;
    }
    
    private byte[] ensureAndGetDataLazy_byte() {
        byte[] data = (byte[]) this.tmpDataLazy;
        if (data == null) {
            data = new byte[1];
            this.tmpDataLazy = data;
        }
        return data;
    }
    
    /*
     * 
     */
    
    private void checkRectInImage(
        int x,
        String xName,
        int y,
        String yName,
        int width,
        String widthName,
        int height,
        String heightName) {
        final int iw = this.imageWidth;
        final int ih = this.imageHeight;
        NbrsUtils.requireInRange(0, iw - 1, x, xName);
        NbrsUtils.requireInRange(0, ih - 1, y, yName);
        NbrsUtils.requireInRange(0, iw - x, width, widthName);
        NbrsUtils.requireInRange(0, ih - y, height, heightName);
    }
    
    private static boolean sameAndNotNull(Object a, Object b) {
        return (a != null) && (a == b);
    }
    
    private static int computeArrayHeight(
        int arrayLength,
        int scanlineStride,
        int arrX,
        int width) {
        final int aw = scanlineStride;
        final int fullLineCount = arrayLength / aw;
        final int partialLineLength = arrayLength - aw * fullLineCount;
        final int ah;
        if ((partialLineLength != 0)
            && (partialLineLength - width >= arrX)) {
            // Partial line large enough to count.
            ah = fullLineCount + 1;
        } else {
            ah = fullLineCount;
        }
        return ah;
    }
    
    private static void fillRect_intArr(
        int[] arr,
        int arrScanlineStride,
        int x,
        int y,
        int width,
        int height,
        //
        int val) {
        
        int lineOffset = y * arrScanlineStride + x;
        if (width == arrScanlineStride) {
            final int area = width * height;
            Arrays.fill(arr, lineOffset, lineOffset + area, val);
        } else {
            for (int j = 0; j < height; j++) {
                Arrays.fill(arr, lineOffset, lineOffset + width, val);
                lineOffset += arrScanlineStride;
            }
        }
    }
    
    private static void fillRect_shortArr(
        short[] arr,
        int arrScanlineStride,
        int x,
        int y,
        int width,
        int height,
        //
        short val) {
        
        int lineOffset = y * arrScanlineStride + x;
        if (width == arrScanlineStride) {
            final int area = width * height;
            Arrays.fill(arr, lineOffset, lineOffset + area, val);
        } else {
            for (int j = 0; j < height; j++) {
                Arrays.fill(arr, lineOffset, lineOffset + width, val);
                lineOffset += arrScanlineStride;
            }
        }
    }
    
    private static void fillRect_byteArr(
        byte[] arr,
        int arrScanlineStride,
        int x,
        int y,
        int width,
        int height,
        //
        byte val) {
        
        int lineOffset = y * arrScanlineStride + x;
        if (width == arrScanlineStride) {
            final int area = width * height;
            Arrays.fill(arr, lineOffset, lineOffset + area, val);
        } else {
            for (int j = 0; j < height; j++) {
                Arrays.fill(arr, lineOffset, lineOffset + width, val);
                lineOffset += arrScanlineStride;
            }
        }
    }
    
    /*
     * 
     */
    
    private static int computeCptMask(int cptIndex) {
        final int cptMask;
        if (cptIndex < 0) {
            cptMask = 0;
        } else {
            cptMask = (0xFF << (32 - (cptIndex + 1) * 8));
        }
        return cptMask;
    }
    
    /*
     * 
     */
    
    private static boolean isArgb32PrePermu(int imageType) {
        return (imageType == BufferedImage.TYPE_INT_ARGB_PRE)
            || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE);
    }
    
    private static boolean isArgb32Permu(int imageType) {
        return (imageType == BufferedImage.TYPE_INT_ARGB)
            || (imageType == BufferedImage.TYPE_4BYTE_ABGR);
    }
    
    private static boolean isRgb24Permu(int imageType) {
        return (imageType == BufferedImage.TYPE_INT_RGB)
            || (imageType == BufferedImage.TYPE_INT_BGR)
            || (imageType == BufferedImage.TYPE_3BYTE_BGR);
    }
    
    private static boolean isUShortRgb(int imageType) {
        return (imageType == BufferedImage.TYPE_USHORT_555_RGB)
            || (imageType == BufferedImage.TYPE_USHORT_565_RGB);
    }
    
    private static boolean isGray(int imageType) {
        return (imageType == BufferedImage.TYPE_USHORT_GRAY)
            || (imageType == BufferedImage.TYPE_BYTE_GRAY);
    }
    
    /*
     * 
     */
    
    private static boolean isArgb32PrePermu(
        BihPixelFormat pixelFormat,
        boolean premul) {
        return (pixelFormat != null)
            && premul;
    }
    
    private static boolean isArgb32Permu(
        BihPixelFormat pixelFormat,
        boolean premul) {
        return (pixelFormat != null)
            && pixelFormat.hasAlpha()
            && (!premul);
    }
    
    private static boolean isRgb24Permu(
        BihPixelFormat pixelFormat) {
        return (pixelFormat != null)
            && (!pixelFormat.hasAlpha());
    }
    
    /*
     * 
     */
    
    private static boolean isArgb32PrePermu(
        int imageType,
        BihPixelFormat pixelFormat,
        boolean premul) {
        return isArgb32PrePermu(pixelFormat, premul)
            || isArgb32PrePermu(imageType);
    }
    
    private static boolean isArgb32Permu(
        int imageType,
        BihPixelFormat pixelFormat,
        boolean premul) {
        return isArgb32Permu(pixelFormat, premul)
            || isArgb32Permu(imageType);
    }
    
    private static boolean isRgb24Permu(
        int imageType,
        BihPixelFormat pixelFormat) {
        return isRgb24Permu(pixelFormat)
            || isRgb24Permu(imageType);
    }
}
