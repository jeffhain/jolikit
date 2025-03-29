/*
 * Copyright 2024-2025 Jeff Hain
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
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;

public class BihTestUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    public static final boolean MUST_PRINT_IMAGE_ON_ERROR = false;

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private BihTestUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return A new Random instance with always the same seed for determinism.
     */
    public static Random newRandom() {
        return TestUtils.newRandom123456789L();
    }
    
    /*
     * 
     */
    
    /**
     * @return A TYPE_INT_ARGB image.
     */
    public static BufferedImage newImageArgb(int width, int height) {
        return new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_ARGB);
    }
    
    public static BufferedImage newImage(
        int width,
        int height,
        TestImageTypeEnum imageTypeEnum) {
        
        final int scanlineStride = width;
        return newImage(
            scanlineStride,
            width,
            height,
            imageTypeEnum);
    }
    
    /**
     * @param scanlineStride Only used for images with int arrays
     *        or gray images (i.e. with byte or short arrays).
     */
    public static BufferedImage newImage(
        int scanlineStride,
        int width,
        int height,
        TestImageTypeEnum imageTypeEnum) {
        
        final int imageType = imageTypeEnum.imageType();
        final BihPixelFormat pixelFormat = imageTypeEnum.pixelFormat();
        
        final BufferedImage ret;
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            if (pixelFormat != null) {
                ret = BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    scanlineStride,
                    //
                    width,
                    height,
                    //
                    imageTypeEnum.isPremul(),
                    pixelFormat.aIndex(),
                    //
                    pixelFormat.rIndex(),
                    pixelFormat.gIndex(),
                    pixelFormat.bIndex());
            } else {
                final boolean premul = imageTypeEnum.isPremul();
                final int aIndex;
                final int rIndex;
                final int gIndex;
                final int bIndex;
                if ((imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_GRAB_PRE)
                    || (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_GRAB)) {
                    aIndex = 2;
                    rIndex = 1;
                    gIndex = 0;
                    bIndex = 3;
                } else {
                    throw new AssertionError();
                }
                ret = BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    scanlineStride,
                    //
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
        } else {
            if (pixelFormat != null) {
                ret = BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    scanlineStride,
                    //
                    width,
                    height,
                    //
                    imageType);
            } else if ((imageType == BufferedImage.TYPE_BYTE_GRAY)
                || (imageType == BufferedImage.TYPE_USHORT_GRAY)) {
                ret = newGrayImage(
                    scanlineStride,
                    width,
                    height,
                    imageType);
            } else {
                /*
                 * Not using scanlineStride.
                 */
                ret = new BufferedImage(
                    width,
                    height,
                    //
                    imageType);
            }
        }
        return ret;
    }
    
    /**
     * Allows to create gray images with scanline stride,
     * for proper test coverage.
     * 
     * @param imageType Must be TYPE_BYTE_GRAY or TYPE_USHORT_GRAY.
     */
    public static BufferedImage newGrayImage(
        int scanlineStride,
        int width,
        int height,
        int imageType) {
        
        if ((imageType != BufferedImage.TYPE_BYTE_GRAY)
            && (imageType != BufferedImage.TYPE_USHORT_GRAY)) {
            throw new IllegalArgumentException("" + imageType);
        }
        
        NbrsUtils.requireSupOrEq(1, width, "width");
        NbrsUtils.requireSupOrEq(1, height, "height");
        NbrsUtils.requireSupOrEq(width, scanlineStride, "scanlineStride");
        
        final boolean is8Bits = (imageType == BufferedImage.TYPE_BYTE_GRAY);
        final int bitSize = (is8Bits ? 8 : 16);
        final int dataType = (is8Bits ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT);
        
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        final int[] nBits = {bitSize};
        final boolean hasAlpha = false;
        // As done in BufferedImage(int,int,int) constructor.
        final boolean premul = true;
        final ColorModel colorModel = new ComponentColorModel(
            cs,
            nBits,
            hasAlpha,
            premul,
            Transparency.OPAQUE,
            dataType);
        
        final Point upperLeftLocation = new Point(0,0);
        // bands=numComponents=numColorComponents=1
        final int bands = cs.getNumComponents();
        final int[] bandOffsets = new int[bands];
        for (int i = 0; i < bands; i++) {
            bandOffsets[i] = i;
        }
        final int pixelStride = bands;
        final WritableRaster raster = Raster.createInterleavedRaster(
            dataType,
            width,
            height,
            scanlineStride,
            pixelStride,
            bandOffsets,
            upperLeftLocation);
        
        final BufferedImage image = new BufferedImage(
            colorModel,
            raster,
            premul, // Only used if has alpha, so not used.
            null);
        return image;
    }
    
    /*
     * 
     */
    
    /**
     * @return Images of all BufferedImage types
     *         and all (BihPixelFormat,premul) types (which overlap a bit).
     *         For all these images, array has been created by the buffer
     *         (no call to theTrackable.setUntrackable()),
     *         and with scanline stride equal to width or larger.
     */
    public static List<BufferedImage> newImageListOfDimWithStrides(int width, int height) {
        final boolean withVariousStrides = true;
        return newImageListOfDim_xxx(width, height, withVariousStrides);
    }
    
    /**
     * With scanlineStride = width, for faster benches.
     * 
     * @return Images of all BufferedImage types
     *         and all (BihPixelFormat,premul) types (which overlap a bit).
     *         For all these images, array has been created by the buffer
     *         (no call to theTrackable.setUntrackable()).
     */
    public static List<BufferedImage> newImageListOfDimNoStrides(int width, int height) {
        final boolean withVariousStrides = false;
        return newImageListOfDim_xxx(width, height, withVariousStrides);
    }
    
    /*
     * 
     */
    
    /**
     * @return Helpers with no redundant capabilities for single pixel methods.
     */
    public static List<BufferedImageHelper> newHelperListForSinglePixel(BufferedImage image) {
        final List<BufferedImageHelper> ret = new ArrayList<>();
        
        for (BufferedImageHelper helper : newHelperList(image)) {
            if (helper.isColorModelAvoidingAllowed()
                && (!helper.isColorModelAvoidedForSinglePixelMethods())) {
                // Optimization ineffective for single pixel methods
                // (ineffective array optimization already taken care of
                // by newHelperList()).
                continue;
            }
            
            ret.add(helper);
        }
        
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * @return A list with all kinds of helpers to test.
     */
    public static List<BufferedImageHelper> newHelperList(BufferedImage image) {
        final List<BufferedImageHelper> ret = new ArrayList<>();
        final int maxOptim = 2;
        for (int optim = 0; optim <= maxOptim; optim++) {
            final boolean allowColorModelAvoiding = (optim >= 1);
            final boolean allowArrayDirectUse = (optim == 2);
            
            final BufferedImageHelper helper =
                new BufferedImageHelper(
                    image,
                    allowColorModelAvoiding,
                    allowArrayDirectUse);
            if (helper.isArrayDirectUseAllowed()
                && (!helper.isArrayDirectlyUsed())) {
                // Optimization ineffective: not testing/benching it.
            } else {
                ret.add(helper);
            }
        }
        return ret;
    }
    
    public static BufferedImageHelper newSameTypeImageAndHelper(
        BufferedImageHelper helper) {
        return newSameTypeImageAndHelper(
            helper,
            helper.getWidth(),
            helper.getHeight());
    }
    
    public static BufferedImageHelper newSameTypeImageAndHelper(
        BufferedImageHelper helper,
        int newWidth,
        int newHeight) {
        
        final BufferedImage image = helper.getImage();
        
        final TestImageTypeEnum imageTypeEnum =
            computeImageTypeEnum(image);
        final int oldScanlineStride = BufferedImageHelper.getScanlineStride(image);
        final int newScanlineStride;
        if (newWidth == helper.getWidth()) {
            newScanlineStride = oldScanlineStride;
        } else {
            if (oldScanlineStride == helper.getWidth()) {
                newScanlineStride = newWidth;
            } else {
                newScanlineStride = newWidth + 1;
            }
        }
        
        final BufferedImage newImage =
            newImage(
                newScanlineStride,
                newWidth,
                newHeight,
                imageTypeEnum);
        
        return new BufferedImageHelper(
            newImage,
            helper.isColorModelAvoidingAllowed(),
            helper.isArrayDirectUseAllowed());
    }
    
    /**
     * @return Never null (TestImageTypeEnum covers all standard image types
     *         and all BihPixelFormat).
     */
    public static TestImageTypeEnum computeImageTypeEnum(BufferedImage image) {
        final int imageType = image.getType();
        final TestImageTypeEnum ret;
        if (imageType != BufferedImage.TYPE_CUSTOM) {
            ret = TestImageTypeEnum.valueOfImageType(imageType);
        } else {
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            final boolean premul = image.isAlphaPremultiplied();
            if (pixelFormat != null) {
                ret = TestImageTypeEnum.valueOfPixelFormat(pixelFormat, premul);
            } else {
                if (premul) {
                    ret = TestImageTypeEnum.TYPE_CUSTOM_INT_GRAB_PRE;
                } else {
                    ret = TestImageTypeEnum.TYPE_CUSTOM_INT_GRAB;
                }
            }
        }
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * @param imageType Can be null (BihPixelFormat covers all alpha cases,
     *        so null pixel format meant premul not possible).
     */
    public static boolean[] newPremulArr(BihPixelFormat pixelFormat) {
        final boolean withTrue =
            (pixelFormat != null)
            && pixelFormat.hasAlpha();
        return newBooleanArr(withTrue);
    }
    
    /*
     * 
     */
    
    /**
     * Half chance of returning zero.
     * Helps to test X and Y non-zero issues separately.
     * 
     * @param bound Exclusive.
     */
    public static int randomPosOrZero(Random random, int bound) {
        final double twiceFp = bound * random.nextDouble();
        return ((twiceFp >= bound) ? 0 : (int) twiceFp);
    }
    
    /**
     * Higher probability of low and high values,
     * in particular of 0x00 and 0xFF.
     */
    public static int randomCpt_00_FF(Random random) {
        final int randomInt = random.nextInt();
        // Using 31 MSBits.
        final double u01 = (randomInt >>> 1) * (1.0 / Integer.MAX_VALUE);
        // Using 1 LSBit.
        final boolean ub = ((randomInt & 1) != 0);
        // Probably closer to zero than to one.
        double lowFp = u01 * u01;
        // Even more probably so.
        lowFp *= lowFp;
        int low8 = (int) (lowFp * 0xFF + 0.5);
        return (ub ? 0xFF - low8 : low8);
    }
    
    public static int randomArgb32(
        Random random,
        boolean opaque) {
        
        final int a8;
        if (opaque) {
            a8 = 0xFF;
        } else {
            a8 = randomCpt_00_FF(random);
        }
        final int r8 = randomCpt_00_FF(random);
        final int g8 = randomCpt_00_FF(random);
        final int b8 = randomCpt_00_FF(random);
        
        return BindingColorUtils.toAbcd32_noCheck(a8, r8, g8, b8);
    }
    
    /**
     * @param opaque If pixelFormat has no alpha, will have no effect.
     */
    public static void randomizeArray(
        Random random,
        //
        int[] color32Arr,
        int scanlineStride,
        BihPixelFormat pixelFormat,
        boolean premul,
        //
        int x,
        int y,
        int width,
        int height,
        //
        boolean opaque) {
        
        for (int j = 0; j < height; j++) {
            final int py = y + j;
            for (int i = 0; i < width; i++) {
                final int px = x + i;
                
                int argb32 = randomArgb32(random, opaque);
                if (premul) {
                    argb32 =
                        BindingColorUtils.toPremulAxyz32(
                            argb32);
                }
                int pixel = pixelFormat.toPixelFromArgb32(argb32);
                final int index = py * scanlineStride + px;
                color32Arr[index] = pixel;
            }
        }
    }
    
    /**
     * @param opaque If image has no alpha, will have no effect.
     */
    public static void randomizeHelper(
        Random random,
        BufferedImageHelper helper,
        boolean opaque) {
        
        randomizeHelper(
            random,
            helper,
            //
            0,
            0,
            helper.getWidth(),
            helper.getHeight(),
            //
            opaque);
    }
    
    /**
     * @param opaque If image has no alpha, will have no effect.
     */
    public static void randomizeHelper(
        Random random,
        BufferedImageHelper helper,
        //
        int x,
        int y,
        int width,
        int height,
        //
        boolean opaque) {
        
        for (int j = 0; j < height; j++) {
            final int py = y + j;
            for (int i = 0; i < width; i++) {
                final int px = x + i;
                final int argb32 = randomArgb32(random, opaque);
                helper.setNonPremulArgb32At(px, py, argb32);
            }
        }
    }
    
    /*
     * 
     */
    
    public static void fillHelperWithNonPremulArgb32(
        BufferedImageHelper helper,
        int nonPremulArgb32) {
        
        fillHelperWithNonPremulArgb32(
            helper,
            //
            0,
            0,
            helper.getWidth(),
            helper.getHeight(),
            //
            nonPremulArgb32);
    }
    
    public static void fillHelperWithNonPremulArgb32(
        BufferedImageHelper helper,
        //
        int x,
        int y,
        int width,
        int height,
        //
        int nonPremulArgb32) {
        
        helper.clearRect(x, y, width, height, nonPremulArgb32, false);
    }
    
    /*
     * 
     */
    
    public static void copyHelper(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper) {
        final int width = Math.max(srcHelper.getWidth(), dstHelper.getWidth());
        final int height = Math.max(srcHelper.getHeight(), dstHelper.getHeight());
        copyImage_reference(
            srcHelper,
            0,
            0,
            dstHelper,
            0,
            0,
            width,
            height);
    }
    
    /*
     * 
     */

    public static void copyArray(
        int[] srcArr,
        int srcScanlineStride,
        //
        int[] dstArr,
        int dstScanlineStride,
        //
        int width,
        int height) {
        
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                final int srcIndex = j * srcScanlineStride + i;
                final int dstIndex = j * dstScanlineStride + i;
                dstArr[dstIndex] = srcArr[srcIndex];
            }
        }
    }
    
    /*
     * 
     */
    
    public static int getBinaryCpt8Delta(
        int area,
        int diffCount) {
        final double ratioDelta = diffCount / (double) area;
        return (int) (ratioDelta * 0xFF + 0.5);
    }
    
    public static int getMaxCptDelta(
        int expectedColor32,
        int actualColor32) {
        final int ve1 = Argb32.getAlpha8(expectedColor32);
        final int va1 = Argb32.getAlpha8(actualColor32);
        final int ve2 = Argb32.getRed8(expectedColor32);
        final int va2 = Argb32.getRed8(actualColor32);
        final int ve3 = Argb32.getGreen8(expectedColor32);
        final int va3 = Argb32.getGreen8(actualColor32);
        final int ve4 = Argb32.getBlue8(expectedColor32);
        final int va4 = Argb32.getBlue8(actualColor32);
        int ret = Math.abs(va1 - ve1);
        ret = Math.max(ret, Math.abs(va2 - ve2));
        ret = Math.max(ret, Math.abs(va3 - ve3));
        ret = Math.max(ret, Math.abs(va4 - ve4));
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * Trivial implementation, based on single-pixel methods,
     * to use as reference for getPixelsInto() correctness,
     * other than for exception checks.
     */
    public static void getPixelsInto_reference(
        BufferedImageHelper helper,
        //
        int srcX,
        int srcY,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat dstPixelFormat,
        boolean dstPremul,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        for (int j = 0; j < height; j++) {
            final int lineOffset =
                (dstY + j) * color32ArrScanlineStride + dstX;
            for (int i = 0; i < width; i++) {
                final int dstIndex = lineOffset + i;
                final int argb32 = helper.getArgb32At(
                    srcX + i,
                    srcY + j,
                    dstPremul);
                final int color32 =
                    dstPixelFormat.toPixelFromArgb32(
                        argb32);
                color32Arr[dstIndex] = color32;
            }
        }
    }
    
    /**
     * Trivial implementation, based on single-pixel methods,
     * to use as reference for setPixelsFrom() correctness,
     * other than for exception checks.
     */
    public static void setPixelsFrom_reference(
        BufferedImageHelper helper,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat srcPixelFormat,
        boolean srcPremul,
        int srcX,
        int srcY,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        for (int j = 0; j < height; j++) {
            final int lineOffset =
                (srcY + j) * color32ArrScanlineStride + srcX;
            for (int i = 0; i < width; i++) {
                final int srcIndex = lineOffset + i;
                final int color32 = color32Arr[srcIndex];
                final int argb32 =
                    srcPixelFormat.toArgb32FromPixel(
                        color32);
                helper.setArgb32At(
                    dstX + i,
                    dstY + j,
                    argb32,
                    srcPremul);
            }
        }
    }
    
    /**
     * Trivial implementation, based on single-pixel methods,
     * to use as reference for copyImage() correctness,
     * other than for exception checks.
     */
    public static void copyImage_reference(
        BufferedImageHelper srcHelper,
        int srcX,
        int srcY,
        //
        BufferedImageHelper dstHelper,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        final boolean dstPremul = dstHelper.isAlphaPremultiplied();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                final int argb32 = srcHelper.getArgb32At(
                    srcX + i,
                    srcY + j,
                    dstPremul);
                dstHelper.setArgb32At(
                    dstX + i,
                    dstY + j,
                    argb32,
                    dstPremul);
            }
        }
    }
    
    /*
     * 
     */
    
    public static void checkColorEquals(int expectedColor32, int actualColor32) {
        if (expectedColor32 != actualColor32) {
            final String expectedStr = Argb32.toString(expectedColor32);
            final String actualStr = Argb32.toString(actualColor32);
            System.out.println("expectedColor32 = " + expectedStr);
            System.out.println("actualColor32 =   " + actualStr);
            throw new AssertionError(expectedStr + " != " + actualStr);
        }
    }
    
    public static void checkImageResult(
        BufferedImageHelper srcHelper,
        int srcX,
        int srcY,
        //
        BufferedImageHelper expectedDstHelper,
        //
        BufferedImageHelper actualDstHelper,
        //
        int dstX,
        int dstY,
        int width,
        int height,
        //
        int cptDeltaTolBin,
        int cptDeltaTolOther) {
        
        if (expectedDstHelper.getImageType() == BufferedImage.TYPE_BYTE_BINARY) {
            checkImageResult_dstBinary(
                srcHelper,
                //
                expectedDstHelper,
                //
                actualDstHelper,
                //
                dstX,
                dstY,
                width,
                height,
                //
                cptDeltaTolBin);
        } else {
            checkImageResult_dstNotBinary(
                srcHelper,
                srcX,
                srcY,
                //
                expectedDstHelper,
                //
                actualDstHelper,
                //
                dstX,
                dstY,
                width,
                height,
                //
                cptDeltaTolOther);
        }
    }
    
    /*
     * 
     */
    
    /**
     * @param ns A duration in nanoseconds.
     * @return The specified duration in seconds, rounded to 3 digits past comma.
     */
    public static double nsToSRounded(long ns) {
        return Math.round(ns / 1e6) / 1e3;
    }
    
    /*
     * 
     */
    
    public static void printImage(String name, BufferedImageHelper helper) {
        System.out.println(name + ": " + helper + ":");
        final boolean premul = helper.isAlphaPremultiplied();
        for (int j = 0; j < helper.getHeight(); j++) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < helper.getWidth(); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(Argb32.toString(helper.getArgb32At(i, j, premul)));
            }
            System.out.println(sb.toString());
        }
    }
    
    public static void printImageDiff(
        String expectedName,
        BufferedImageHelper expectedHelper,
        String actualName,
        BufferedImageHelper actualHelper) {
        System.out.println(expectedName + ": " + expectedHelper);
        System.out.println(actualName + ": " + actualHelper);
        final boolean premul =
            expectedHelper.isAlphaPremultiplied()
            && actualHelper.isAlphaPremultiplied();
        for (int j = 0; j < expectedHelper.getHeight(); j++) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < expectedHelper.getWidth(); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                final int expectedArgb32 =
                    expectedHelper.getArgb32At(i, j, premul);
                final int actualArgb32 =
                    actualHelper.getArgb32At(i, j, premul);
                if (expectedArgb32 == actualArgb32) {
                    sb.append(Argb32.toString(actualArgb32));
                } else {
                    sb.append(Argb32.toString(expectedArgb32));
                    sb.append("/");
                    sb.append(Argb32.toString(actualArgb32));
                }
            }
            System.out.println(sb.toString());
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Images of all TestImageTypeEnum (which covers all standard types
     *         and all BihPixelFormat, and more).
     */
    private static List<BufferedImage> newImageListOfDim_xxx(
        int width,
        int height,
        boolean withVariousStrides) {
        
        final List<BufferedImage> ret = new ArrayList<>();
        
        final int[] strideBonusArr;
        if (withVariousStrides) {
            strideBonusArr = new int[] {0, 1};
        } else {
            strideBonusArr = new int[] {0};
        }
        
        for (TestImageTypeEnum imageTypeEnum : TestImageTypeEnum.values()) {
            for (int strideBonus : strideBonusArr) {
                final int scanlineStride = width + strideBonus;
                final BufferedImage image =
                    newImage(
                        scanlineStride,
                        width,
                        height,
                        imageTypeEnum);
                
                ret.add(image);
            }
        }
        
        return ret;
    }
    
    /*
     * 
     */
    
    private static boolean[] newBooleanArr(boolean withTrue) {
        if (withTrue) {
            return new boolean[] {false, true};
        } else {
            return new boolean[] {false};
        }
    }
    
    /*
     * 
     */
    
    /**
     * For TYPE_BYTE_BINARY destination, not to fail on edge cases
     * when source pixel intensity is close to threshold,
     * we compare results mean intensity.
     */
    private static void checkImageResult_dstBinary(
        final BufferedImageHelper srcHelper,
        //
        final BufferedImageHelper expectedDstHelper,
        //
        final BufferedImageHelper actualDstHelper,
        //
        final int dstX,
        final int dstY,
        final int width,
        final int height,
        //
        final int binaryCpt8Tol) {
        
        final BufferedImage expectedImage = expectedDstHelper.getImage();
        final BufferedImage actualImage = actualDstHelper.getImage();
        
        if ((expectedImage.getType() != BufferedImage.TYPE_BYTE_BINARY)
            || (actualImage.getType() != BufferedImage.TYPE_BYTE_BINARY)) {
            throw new IllegalArgumentException();
        }
        
        final int area = width * height;
        if (area < 100) {
            // Not enough pixels: might exceed tolerance
            // due to bad luck.
            return;
        }
        
        int diffCount = 0;
        for (int y = dstY; y < dstY + height; y++) {
            for (int x = dstX; x < dstX + width; x++) {
                final int expectedArgb32 =
                    expectedDstHelper.getNonPremulArgb32At(x, y);
                final int actualArgb32 =
                    actualDstHelper.getNonPremulArgb32At(x, y);
                if (expectedArgb32 != actualArgb32) {
                    diffCount++;
                }
            }
        }
        final int binaryCpt8Delta = getBinaryCpt8Delta(
            area,
            diffCount);
        if (binaryCpt8Delta > binaryCpt8Tol) {
            System.out.println();
            if (MUST_PRINT_IMAGE_ON_ERROR) {
                printImage("srcHelper", srcHelper);
                printImageDiff(
                    "expectedDstHelper",
                    expectedDstHelper,
                    "actualDstHelper",
                    actualDstHelper);
            }
            System.out.println("srcHelper = " + srcHelper);
            System.out.println("expectedDstHelper = " + expectedDstHelper);
            System.out.println("actualDstHelper = " + actualDstHelper);
            System.out.println("area = " + area);
            System.out.println("diffCount = " + diffCount);
            System.out.println("binaryCpt8Tol = " + binaryCpt8Tol);
            System.out.println("binaryCpt8Delta = " + binaryCpt8Delta);
            throw new AssertionError();
        }
    }
    
    private static void checkImageResult_dstNotBinary(
        BufferedImageHelper srcHelper,
        int srcX,
        int srcY,
        //
        BufferedImageHelper expectedDstHelper,
        //
        BufferedImageHelper actualDstHelper,
        //
        int dstX,
        int dstY,
        int width,
        int height,
        //
        int cptDeltaTol) {
        
        for (int y = dstY; y < dstY + height; y++) {
            for (int x = dstX; x < dstX + width; x++) {
                final int expectedArgb32 =
                    expectedDstHelper.getNonPremulArgb32At(x, y);
                final int actualArgb32 =
                    actualDstHelper.getNonPremulArgb32At(x, y);
                
                final int maxCptDelta = getMaxCptDelta(
                    expectedArgb32,
                    actualArgb32);
                
                if (maxCptDelta > cptDeltaTol) {
                    final String expectedStr = Argb32.toString(expectedArgb32);
                    final String actualStr = Argb32.toString(actualArgb32);
                    System.out.println();
                    if (MUST_PRINT_IMAGE_ON_ERROR) {
                        printImage("srcHelper", srcHelper);
                        printImageDiff(
                            "expectedDstHelper",
                            expectedDstHelper,
                            "actualDstHelper",
                            actualDstHelper);
                    }
                    System.out.println("srcHelper = " + srcHelper);
                    System.out.println("srcX = " + srcX);
                    System.out.println("srcY = " + srcY);
                    System.out.println("expectedDstHelper = " + expectedDstHelper);
                    System.out.println("actualDstHelper = " + actualDstHelper);
                    System.out.println("dstX = " + dstX);
                    System.out.println("dstY = " + dstY);
                    System.out.println("width = " + width);
                    System.out.println("height = " + height);
                    System.out.println("cptDeltaTol = " + cptDeltaTol
                        + " = 0x" + Integer.toHexString(cptDeltaTol));
                    System.out.println("maxCptDelta = " + maxCptDelta
                        + " = 0x" + Integer.toHexString(maxCptDelta));
                    System.out.println("expected = " + expectedStr);
                    System.out.println("actual =   " + actualStr);
                    throw new AssertionError(expectedStr + " != " + actualStr);
                }
            }
        }
    }
}
