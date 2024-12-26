/*
 * Copyright 2019-2024 Jeff Hain
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
 * Non-static methods may use instance-specific mutable state
 * (other than the buffered image), so they must not be used concurrently
 * (even read methods).
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
    private static final BihPixelFormat TMP_IMAGE_PIXEL_FORMAT = BihPixelFormat.ARGB32;
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    /**
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
    
    /*
     * 
     */
    
    private final BufferedImage image;
    
    private final int width;
    
    private final int height;
    
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
        this.width = image.getWidth();
        this.height = image.getHeight();
        
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
     * @param width Image width. Must be >= 1.
     * @param height Image height. Must be >= 1.
     * @param imageType Type of the backing BufferedImage.
     *        Only supporting BufferedImage.TYPE_INT_XXX types
     *        {TYPE_INT_ARGB, TYPE_INT_ARGB_PRE, TYPE_INT_BGR, TYPE_INT_RGB}.
     * @return The created buffered image, with 32 bits pixels if there is
     *         alpha component, and 24 bits pixels otherwise.
     * @throws IllegalArgumentException if the specified width or height
     *         are not strictly positive, or the specified array is non-null
     *         and too small for width and height.
     * @throws ArithmeticException if the specified width and height
     *         cause int overflow when multiplied with each other.
     * @throws OutOfMemoryError if the specified array is null
     *         and the array to create is too large for VM limit.
     */
    public static BufferedImage newBufferedImageWithIntArray(
        int[] pixelArr,
        int width,
        int height,
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
     * @param width Image width. Must be >= 1.
     * @param height Image height. Must be >= 1.
     * @param pixelFormat The pixel format to use.
     * @param premul Whether pixels must be alpha premultiplied.
     * @return The created buffered image, with 32 bits pixels if there is
     *         alpha component, and 24 bits pixels otherwise.
     * @throws NullPointerException if the specified pixel format is null.
     * @throws IllegalArgumentException if the specified width or height
     *         are not strictly positive, or the specified array is non-null
     *         and too small for width and height,
     *         or the specified pixel format has no alpha and premul is true.
     * @throws ArithmeticException if the specified width and height
     *         cause int overflow when multiplied with each other.
     * @throws OutOfMemoryError if the specified array is null
     *         and the array to create is too large for VM limit.
     */
    public static BufferedImage newBufferedImageWithIntArray(
        int[] pixelArr,
        int width,
        int height,
        BihPixelFormat pixelFormat,
        boolean premul) {
        // Implicit null check.
        if ((!pixelFormat.hasAlpha()) && premul) {
            throw new IllegalArgumentException(
                "premul can't be true for " + pixelFormat);
        }
        return newBufferedImageWithIntArray(
            pixelArr,
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
     *         are not strictly positive, or the specified array is non-null
     *         and too small for width and height,
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
        
        final int area = NbrsUtils.timesExact(width, height);
        if (pixelArr != null) {
            NbrsUtils.requireSupOrEq(
                area,
                pixelArr.length,
                "pixelArr.length");
        }
        
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
            bitMasks);
        final DataBuffer dataBuffer;
        if (pixelArr != null) {
            dataBuffer = new DataBufferInt(
                pixelArr,
                area);
        } else {
            dataBuffer = new DataBufferInt(area);
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
     *         of pixels that can be used directly and simply
     *         (DataBufferInt buffer, one pixel per data element,
     *         raster (width,height) equal to image (width,height),
     *         width as scanline stride and no translation).
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
     *         is not backed by a single int array usable directly
     *         and simply (no translation etc.) and compatible
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
     * Does SRC_OVER blending.
     * 
     * @param premulArgb32 Alpha premultiplied 32 bits ARGB color to blend.
     * @throws ArrayIndexOutOfBoundsException if the specified position
     *         is out of range.
     */
    public void drawPointPremulAt(int x, int y, int premulArgb32) {
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
        
        this.setPremulArgb32At(x, y, newPremulArgb32);
    }
    
    /*
     * 
     */
    
    /**
     * Uses setArgb32At().
     * 
     * @param argb32 An ARGB 32 value.
     * @param premul Whether the specified value is alpha-premultiplied.
     * @throws ArrayIndexOutOfBoundsException if a specified position
     *         is out of range.
     */
    public void clearRect(
        int x, int y, int width, int height,
        int argb32,
        boolean premul) {
        /*
         * Converting color into image premul or not
         * for faster setting.
         */
        final boolean imagePremul =
            this.image.isAlphaPremultiplied();
        final int imageArgb32;
        if (premul != imagePremul) {
            if (imagePremul) {
                imageArgb32 = BindingColorUtils.toPremulAxyz32(argb32);
            } else {
                imageArgb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
            }
        } else {
            imageArgb32 = argb32;
        }
        for (int j = 0; j < height; j++) {
            final int py = y + j;
            for (int i = 0; i < width; i++) {
                final int px = x + i;
                this.setArgb32At(px, py, imageArgb32, imagePremul);
            }
        }
    }
    
    /**
     * Does SRC_OVER blending.
     * 
     * @param premulArgb32 Alpha premultiplied 32 bits ARGB color to blend.
     * @throws ArrayIndexOutOfBoundsException if a specified position
     *         is out of range.
     */
    public void fillRectPremul(
        int x, int y, int width, int height,
        int premulArgb32) {
        for (int j = 0; j < height; j++) {
            final int py = y + j;
            for (int i = 0; i < width; i++) {
                final int px = x + i;
                this.drawPointPremulAt(
                    px,
                    py,
                    premulArgb32);
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * @throws ArrayIndexOutOfBoundsException if a specified position
     *         is out of range.
     */
    public void invertPixels(int x, int y, int width, int height) {
        for (int j = 0; j < height; j++) {
            final int py = y + j;
            for (int i = 0; i < width; i++) {
                final int px = x + i;
                final int argb32 = this.getNonPremulArgb32At(px, py);
                final int invertedArgb32 = Argb32.inverted(argb32);
                this.setNonPremulArgb32At(px, py, invertedArgb32);
            }
        }
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
     * @param pixelFormatTo Pixel format to use for output.
     * @param premulTo Whether output must be alpha-premultiplied.
     * @throws NullPointerException if the specified array or
     *         pixel format is null.
     * @throws IllegalArgumentException if the specified pixel format
     *         has no alpha and the specified premul is true,
     *         or scanline stride is inferior to image width,
     *         or the specified array is too small for
     *         the specified scanline stride and
     *         image height and width.
     */
    public void getPixelsInto(
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        final int width = this.image.getWidth();
        final int height = this.image.getHeight();
        this.getPixelsInto(
            0, 0, width, height,
            color32Arr,
            color32ArrScanlineStride,
            pixelFormatTo,
            premulTo);
    }
    
    /**
     * Top-left pixel of src rectangle will be put at index 0
     * in the output array.
     * 
     * @param color32Arr (out) Must not be helper's image pixel array,
     *        else behavior is undefined.
     * @param color32ArrScanlineStride Must be >= 1.
     * @param pixelFormatTo Pixel format to use for output.
     * @param premulTo Whether output must be alpha-premultiplied.
     * @throws NullPointerException if the specified array or
     *         pixel format is null.
     * @throws IllegalArgumentException if the specified pixel format
     *         has no alpha and the specified premul is true,
     *         or the specified width or height is negative,
     *         or a specified position is out of image,
     *         or scanline stride is inferior to width,
     *         or the specified array is too small for
     *         the specified height, width and scanline stride.
     */
    public void getPixelsInto(
        int srcX,
        int srcY,
        int srcWidth,
        int srcHeight,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
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
            // Behavior undefined per spec in this case,
            // so we have the right to throw
            // if we happen to know they are the same.
            throw new IllegalArgumentException(
                "color32Arr is image array");
        }
        
        NbrsUtils.requireInRange(0, this.width - 1, srcX, "srcX");
        NbrsUtils.requireInRange(0, this.height - 1, srcY, "srcY");
        NbrsUtils.requireInRange(0, this.width - srcX, srcWidth, "srcWidth");
        NbrsUtils.requireInRange(0, this.height - srcY, srcHeight, "srcHeight");
        NbrsUtils.requireSupOrEq(
            srcWidth,
            color32ArrScanlineStride,
            "color32ArrScanlineStride");
        // Check in long to avoid overflow/wrapping.
        NbrsUtils.requireSupOrEq(
            (srcHeight - 1) * (long) color32ArrScanlineStride + srcWidth,
            color32Arr.length,
            "color32Arr.length");
        
        /*
         * Getting pixels.
         */
        
        if ((this.intPixelArr != null)
            && (pixelFormatTo == this.pixelFormat)) {
            /*
             * A lot faster than drawImage() if we only use arraycopy(),
             * and still faster if doing non-opaque premul rework
             * (but not if doing format rework).
             */
            this.getPixelsInto_noCheck_intArr_samePixelFormat(
                srcX,
                srcY,
                srcWidth,
                srcHeight,
                color32Arr,
                color32ArrScanlineStride,
                premulTo);
        } else {
            /*
             * drawImage() faster than pixel-per-pixel copy,
             * so using it whenever we can.
             * 
             * We only use drawImage() if avoiding color model,
             * for it avoids it.
             * 
             * drawImage() seem to use alpha-premultiplied values internally
             * even if both its input and output images are not alpha-premultiplied
             * (and even if none has an alpha component,
             * i.e. using XRGB24 for graphics image doesn't help).
             * For example, it converts (non-premul) 0x01374EBD
             * into (premul) 0x01000001 and then into (non-premul) 0x010000FF.
             * As a result, we don't use drawImage() if input image and
             * output are both not alpha-premultiplied.
             * 
             * NB: When avoiding color model and output is not alpha-premultiplied,
             * if input image is alpha-premultiplied drawImage() can give results
             * off by one compared to our single-pixel treatments
             * (ex.: (ARGB32,p)->(ARGB32,np):
             *     when expecting 0x16B98046
             *  drawImage() gives 0x16B97F46),
             * but since this is just a rounding error and only seem to occur
             * for alpha <= 0xFE, i.e. for non-opaque colors,
             * for which having rounding errors is expectable
             * due to alpha-premultiplication,
             * we still call drawImage() in this case for speed.
             * 
             * Note that all these guards still allow to use drawImage()
             * for the usual fast case of having color model avoided
             * and either input or output alpha-premultiplied.
             */
            final boolean canUseDrawImage =
                this.isColorModelAvoidingAllowed()
                && (this.image.isAlphaPremultiplied()
                    || premulTo);
            if (canUseDrawImage) {
                this.getPixelsInto_noCheck_drawImage(
                    srcX,
                    srcY,
                    srcWidth,
                    srcHeight,
                    color32Arr,
                    color32ArrScanlineStride,
                    pixelFormatTo,
                    premulTo);
            } else {
                this.getPixelsInto_noCheck_arrOrRaster(
                    srcX,
                    srcY,
                    srcWidth,
                    srcHeight,
                    color32Arr,
                    color32ArrScanlineStride,
                    pixelFormatTo,
                    premulTo);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    boolean isColorModelAvoidedForSinglePixelMethods() {
        return this.singlePixelCmaType != MySinglePixelCmaType.NONE;
    }
    
    boolean isArrayDirectlyUsedForSinglePixelMethods() {
        return (this.intPixelArr != null)
            || (this.shortPixelArr != null)
            || (this.bytePixelArr != null);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @throws ArrayIndexOutOfBoundsException if not in image.
     */
    private int toIndex(int x, int y) {
        if (((x|y) < 0)
            || (x >= this.width)
            || (y >= this.height)) {
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
    
    /**
     * @return The scanline stride of the backing array,
     *         or -1 if could not be found.
     */
    private static int getScanlineStride(BufferedImage image) {
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
     *         a simple array of the specified type.
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
            && (getScanlineStride(image) != image.getWidth())) {
            error = "scanline stride != width";
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
     * Does not check (x,y).
     * Useful for bulk methods.
     */
    private int getArgb32At_noCheck_arrOrRaster(
        int x,
        int y,
        boolean premul) {
        final int ret;
        if (this.intPixelArr != null) {
            final int index = this.toIndex_noCheck(x, y);
            final int pixel = this.intPixelArr[index];
            ret = this.convertPixelToArgb32_INT_BIH_PIXEL_FORMAT(
                pixel,
                premul);
        } else if (this.shortPixelArr != null) {
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
        
        final int argb32;
        switch (this.singlePixelCmaType) {
            case NONE: {
                argb32 = this.convertDataToArgb32(
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
    
    private void setArgb32At_raster(
        int x,
        int y,
        int argb32,
        boolean premul) {
        
        final WritableRaster raster = this.image.getRaster();
        
        final Object data;
        switch (this.singlePixelCmaType) {
            case NONE: {
                data = this.convertArgb32ToData(
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
        this.tmpDataLazy = data;
        
        raster.setDataElements(x, y, data);
    }
    
    /*
     * convertXxxToArgb32
     */
    
    private int convertDataToArgb32(
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
        if (premul) {
            argb32 = BindingColorUtils.toNonPremulAxyz32(argb32);
        }
        final ColorModel colorModel = this.image.getColorModel();
        return colorModel.getDataElements(
            argb32,
            this.tmpDataLazy);
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
    
    private void getPixelsInto_noCheck_intArr_samePixelFormat(
        int srcX,
        int srcY,
        int srcWidth,
        int srcHeight,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        boolean premulTo) {
        
        /*
         * Copying pixels into destination array.
         */
        final int areaTo = srcWidth * srcHeight;
        if ((srcWidth == this.scanlineStride)
            && (srcWidth == color32ArrScanlineStride)) {
            /*
             * Means getting some or all lines,
             * over the whole width=srcStride=dstStride,
             * so we can copy with a single arraycopy().
             */
            System.arraycopy(
                this.intPixelArr,
                srcY * this.scanlineStride,
                color32Arr,
                0,
                areaTo);
        } else {
            /*
             * Not full width, or different strides.
             */
            for (int j = 0; j < srcHeight; j++) {
                System.arraycopy(
                    this.intPixelArr,
                    (srcY + j) * this.scanlineStride + srcX,
                    color32Arr,
                    j * color32ArrScanlineStride,
                    srcWidth);
            }
        }
        /*
         * Eventual premul rework.
         */
        if (premulTo != this.image.isAlphaPremultiplied()) {
            /*
             * Means the (single) format has alpha,
             * so no need to ensure opaque before premul conversion.
             */
            final boolean isAlphaInMSByte = this.pixelFormat.areColorsInLsbElseMsb();
            for (int j = 0; j < srcHeight; j++) {
                final int dstStrideOffset = j * color32ArrScanlineStride;
                for (int i = 0; i < srcWidth; i++) {
                    final int index = dstStrideOffset + i;
                    int pixelFrom = color32Arr[index];
                    final int pixelTo;
                    if (premulTo) {
                        if (isAlphaInMSByte) {
                            pixelTo = BindingColorUtils.toPremulAxyz32(pixelFrom);
                        } else {
                            pixelTo = BindingColorUtils.toPremulXyza32(pixelFrom);
                        }
                    } else {
                        if (isAlphaInMSByte) {
                            pixelTo = BindingColorUtils.toNonPremulAxyz32(pixelFrom);
                        } else {
                            pixelTo = BindingColorUtils.toNonPremulXyza32(pixelFrom);
                        }
                    }
                    color32Arr[index] = pixelTo;
                }
            }
        }
    }
    
    private void getPixelsInto_noCheck_arrOrRaster(
        int srcX,
        int srcY,
        int srcWidth,
        int srcHeight,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
        for (int j = 0; j < srcHeight; j++) {
            final int sy = srcY + j;
            final int dstLineOffset =
                j * color32ArrScanlineStride;
            for (int i = 0; i < srcWidth; i++) {
                final int sx = srcX + i;
                final int argb32 =
                    this.getArgb32At_noCheck_arrOrRaster(
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
    
    private void getPixelsInto_noCheck_drawImage(
        int srcX,
        int srcY,
        int srcWidth,
        int srcHeight,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
        if ((color32ArrScanlineStride == 0) || (srcHeight == 0)) {
            /*
             * SampleModel creation would throw.
             * Nothing to do.
             */
            return;
        }
        
        // Need to reset output pixels before drawing into it
        // with image, for it does blending
        // (matters for non-opaque images).
        zeroizePixels(
            color32Arr,
            color32ArrScanlineStride,
            srcWidth,
            srcHeight);
        
        final BihPixelFormat tmpImagePixelFormat = TMP_IMAGE_PIXEL_FORMAT;
        final BufferedImage tmpImage = newBufferedImageWithIntArray(
            color32Arr,
            color32ArrScanlineStride,
            srcHeight,
            tmpImagePixelFormat,
            premulTo);
        final Graphics2D tmpG = tmpImage.createGraphics();
        try {
            tmpG.drawImage(
                this.image,
                //
                0, // dx1
                0, // dy1
                srcWidth, // dx2 (exclusive)
                srcHeight, // dy2 (exclusive)
                //
                srcX, // sx1
                srcY, // sy1
                srcX + srcWidth, // sx2 (exclusive)
                srcY + srcHeight, // sy2 (exclusive)
                //
                null);
        } finally {
            tmpG.dispose();
        }
        
        /*
         * Eventually reordering components as expected
         * (premul or not is already ensured through tmp image).
         */
        
        if (pixelFormatTo != tmpImagePixelFormat) {
            for (int y = 0; y < height; y++) {
                final int lineOffset = y * color32ArrScanlineStride;
                for (int x = 0; x < width; x++) {
                    final int index = lineOffset + x;
                    color32Arr[index] =
                        pixelFormatTo.toPixelFromArgb32(
                            color32Arr[index]);
                }
            }
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
    
    private static void zeroizePixels(
        int[] color32Arr,
        int color32ArrScanlineStride,
        int width,
        int height) {
        for (int y = 0; y < height; y++) {
            final int lineOffset = y * color32ArrScanlineStride;
            Arrays.fill(
                color32Arr,
                lineOffset,
                lineOffset + width,
                0);
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
}
