/*
 * Copyright 2024 Jeff Hain
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

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;

public class BihTestUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Images of all BufferedImage types (except TYPE_CUSTOM).
     */
    public static List<BufferedImage> newImageList_allImageType(int width, int height) {
        final List<BufferedImage> ret = new ArrayList<>();
        
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = new BufferedImage(
                width,
                height,
                imageType);
            ret.add(image);
        }
        
        return ret;
    }
    
    /**
     * @return Images of all BufferedImage types
     *         and all (BihPixelFormat,premul) types (which overlap a bit).
     *         For all these images, array has been created by the buffer
     *         (no call to theTrackable.setUntrackable()),
     *         and with scanline stride equal to width or larger.
     */
    public static List<BufferedImage> newImageList(int width, int height) {
        return newImageList_xxx(width, height, true);
    }
    
    /**
     * With scanlineStride = width, for faster benches.
     * 
     * @return Images of all BufferedImage types
     *         and all (BihPixelFormat,premul) types (which overlap a bit).
     *         For all these images, array has been created by the buffer
     *         (no call to theTrackable.setUntrackable()).
     */
    public static List<BufferedImage> newImageList_forBench(int width, int height) {
        return newImageList_xxx(width, height, false);
    }
    
    /**
     * @return List of images of all possible (BihPixelFormat,premul) types,
     *         with array created by the buffer
     *         (no call to theTrackable.setUntrackable()),
     *         and with scanline stride equal to width or larger.
     */
    public static List<BufferedImage> newImageList_allPixelFormat(
        int width,
        int height) {
        return newImageList_allBihPixelFormat_xxx(width, height, true);
    }
    
    /**
     * With scanlineStride = width, for faster benches.
     * 
     * @return List of images of all possible (BihPixelFormat,premul) types,
     *         with array created by the buffer
     *         (no call to theTrackable.setUntrackable()).
     */
    public static List<BufferedImage> newImageList_allPixelFormat_forBench(
        int width,
        int height) {
        return newImageList_allBihPixelFormat_xxx(width, height, false);
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
                // Optimization ineffective for singlep pixel methods
                // (ineffective array optimization already taken care of
                // by newHelperList()).
                continue;
            }
            
            ret.add(helper);
        }
        
        return ret;
    }
    
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
    
    public static BufferedImageHelper newIdenticalImageAndHelper(
        BufferedImageHelper helper) {
        
        final BufferedImage image = helper.getImage();
        final BihPixelFormat pixelFormat = helper.getPixelFormat();
        
        final BufferedImage imageClone;
        if (pixelFormat != null) {
            imageClone =
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    image.getWidth(),
                    image.getHeight(),
                    pixelFormat,
                    image.isAlphaPremultiplied());
        } else {
            imageClone =
                new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    helper.getImage().getType());
        }
        return new BufferedImageHelper(
            imageClone,
            helper.isColorModelAvoidingAllowed(),
            helper.isArrayDirectUseAllowed());
    }
    
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
    
    public static boolean[] newBooleanArr(boolean withTrue) {
        if (withTrue) {
            return new boolean[] {false, true};
        } else {
            return new boolean[] {false};
        }
    }
    
    /*
     * 
     */
    
    public static int computeMaxCptDelta(
        BufferedImageHelper expectedHelper,
        BufferedImageHelper actualHelper) {
        
        final int width = expectedHelper.getImage().getWidth();
        final int height = expectedHelper.getImage().getHeight();
        
        int ret = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int expectedArgb32 = expectedHelper.getNonPremulArgb32At(x, y);
                final int actualArgb32 = actualHelper.getNonPremulArgb32At(x, y);
                ret = Math.max(ret, getMaxCptDelta(expectedArgb32, actualArgb32));
            }
        }
        return ret;
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
        int srcWidth,
        int srcHeight,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
        for (int j = 0; j < srcHeight; j++) {
            final int lineOffset =
                j * color32ArrScanlineStride;
            for (int i = 0; i < srcWidth; i++) {
                final int indexTo = lineOffset + i;
                final int argb32 = helper.getArgb32At(
                    srcX + i,
                    srcY + j,
                    premulTo);
                final int color32 =
                    pixelFormatTo.toPixelFromArgb32(
                        argb32);
                color32Arr[indexTo] = color32;
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
        //
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        //
        int dstX,
        int dstY,
        int dstWidth,
        int dstHeight) {
        
        for (int j = 0; j < dstHeight; j++) {
            final int lineOffset =
                j * color32ArrScanlineStride;
            for (int i = 0; i < dstWidth; i++) {
                final int indexFrom = lineOffset + i;
                final int color32 = color32Arr[indexFrom];
                final int argb32 =
                    pixelFormatFrom.toArgb32FromPixel(
                        color32);
                helper.setArgb32At(
                    dstX + i,
                    dstY + j,
                    argb32,
                    premulFrom);
            }
        }
    }
    
    /**
     * Trivial implementation, based on single-pixel methods,
     * to use as reference for copyImage() correctness,
     * other than for exception checks.
     */
    public static void copyImage_reference(
        BufferedImageHelper helperFrom,
        int srcX,
        int srcY,
        BufferedImageHelper helperTo,
        int dstX,
        int dstY,
        int width,
        int height) {
        
        final boolean premulTo = helperTo.getImage().isAlphaPremultiplied();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                final int argb32 = helperFrom.getArgb32At(
                    srcX + i,
                    srcY + j,
                    premulTo);
                helperTo.setArgb32At(
                    dstX + i,
                    dstY + j,
                    argb32,
                    premulTo);
            }
        }
    }
    
    /*
     * 
     */
    
    public static String toStringImageKind(BufferedImage image) {
        
        final StringBuilder sb = new StringBuilder();
        
        final boolean premul = image.isAlphaPremultiplied();
        
        if (image.getTransparency() == Transparency.OPAQUE) {
            sb.append("(op");
            if (premul) {
                sb.append(",p)");
            } else {
                sb.append(",np)");
            }
        } else if (image.getTransparency() == Transparency.TRANSLUCENT) {
            sb.append("(tr");
            if (premul) {
                sb.append(",p)");
            } else {
                sb.append(",np)");
            }
        } else {
            if (premul) {
                sb.append("(?,p)");
            } else {
                sb.append("(?,np)");
            }
        }
        
        final int imageType = image.getType();
        final ImageTypeEnum imageTypeEnum =
            ImageTypeEnum.enumByType().get(imageType);
        if (imageTypeEnum != null) {
            sb.append(imageTypeEnum);
        } else {
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            if (pixelFormat != null) {
                sb.append(pixelFormat);
            } else {
                sb.append("TYPE_CUSTOM");
            }
        }
        
        return sb.toString();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BihTestUtils() {
    }
    
    private static List<BufferedImage> newImageList_xxx(
        int width,
        int height,
        boolean withVariousStrides) {
        
        final List<BufferedImage> ret = new ArrayList<>();
        
        final Set<Integer> coveredImageTypeSet = new TreeSet<>();
        
        for (BufferedImage image : newImageList_allBihPixelFormat_xxx(
            width,
            height,
            withVariousStrides)) {
            
            ret.add(image);
            coveredImageTypeSet.add(image.getType());
        }
        
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            // Guards against image types already covered.
            if (coveredImageTypeSet.add(imageType)) {
                final BufferedImage image = new BufferedImage(
                    width,
                    height,
                    imageType);
                ret.add(image);
            }
        }
        
        return ret;
    }
    
    private static List<BufferedImage> newImageList_allBihPixelFormat_xxx(
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
        
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : newPremulArr(pixelFormat)) {
                for (int strideBonus : strideBonusArr) {
                    final int scanlineStride = width + strideBonus;
                    final BufferedImage image =
                        BufferedImageHelper.newBufferedImageWithIntArray(
                            null,
                            scanlineStride,
                            //
                            width,
                            height,
                            //
                            premul,
                            pixelFormat.aIndex(),
                            //
                            pixelFormat.rIndex(),
                            pixelFormat.gIndex(),
                            pixelFormat.bIndex());
                    ret.add(image);
                }
            }
        }
        
        return ret;
    }
}
