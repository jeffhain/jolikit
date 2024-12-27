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
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.test.utils.TestUtils;

public class BufferedImageHelperPerfs {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_RUNS = 2;
    private static final int NBR_OF_CALLS = 10 * 1000 * 1000;
    
    private static final boolean MUST_BENCH_SINGLE_PIXEL_GET_METHODS = true;
    private static final boolean MUST_BENCH_SINGLE_PIXEL_SET_METHODS = true;
    private static final boolean MUST_BENCH_BULK_GET_METHODS = true;
    private static final boolean MUST_BENCH_BULK_SET_METHODS = true;
    
    /*
     * Small image, for all array to stay in cache
     * and just bench computations.
     */
    
    private static final int SMALL_IMAGE_WIDTH = 10;
    private static final int SMALL_IMAGE_HEIGHT = 10;
    
    /*
     * Allowed types of pixels for type-to-type conversions
     * (for bench not to take ages).
     */
    
    private static final Set<BihPixelFormat> QUADRATIC_PIXEL_FORMAT_SET =
        new TreeSet<>(Arrays.asList(
            /*
             * The basic.
             */
            BihPixelFormat.ARGB32,
            /*
             * To have another alpha format than ARGB,
             * and one with alpha in LSByte,
             * for conversions to/from itself or another format.
             */
            BihPixelFormat.RGBA32,
            /*
             * The basic opaque format.
             */
            BihPixelFormat.XRGB24));
    
    private static final Set<Integer> QUADRATIC_SRC_IMAGE_TYPE_SET =
        new TreeSet<>(Arrays.asList(
            /**
             * To have a color format for which
             * we never use array directly (only raster).
             */
            BufferedImage.TYPE_3BYTE_BGR,
            /**
             * To have a short format for which
             * we can use array directly.
             */
            BufferedImage.TYPE_USHORT_565_RGB,
            /**
             * To have a byte format for which
             * we can use array directly.
             */
            BufferedImage.TYPE_BYTE_GRAY));
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        newRun(args);
    }
    
    public static void newRun(String[] args) {
        new BufferedImageHelperPerfs().run(args);
    }
    
    public BufferedImageHelperPerfs() {
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run(String[] args) {
        final long a = System.nanoTime();
        System.out.println("--- " + BufferedImageHelperPerfs.class.getSimpleName() + "... ---");
        
        if (MUST_BENCH_SINGLE_PIXEL_GET_METHODS) {
            bench_getArgb32At_nonPremul_opaque();
            
            bench_getArgb32At_nonPremul_translucent();
            
            bench_getArgb32At_premul_opaque();
            
            bench_getArgb32At_premul_translucent();
        }
        
        if (MUST_BENCH_SINGLE_PIXEL_SET_METHODS) {
            bench_setArgb32At_nonPremul_opaque();
            
            bench_setArgb32At_nonPremul_translucent();
            
            bench_setArgb32At_premul_opaque();
            
            bench_setArgb32At_premul_translucent();
            
            bench_drawPointPremulAt_opaque();
            
            bench_drawPointPremulAt_translucent();
        }
        
        if (MUST_BENCH_BULK_GET_METHODS) {
            bench_getPixelsInto_3840_2160_opaque();
            
            bench_getPixelsInto_3840_2160_translucent();
        }
        
        if (MUST_BENCH_BULK_SET_METHODS) {
            bench_setPixelsFrom_3840_2160_opaque();
            
            bench_setPixelsFrom_3840_2160_translucent();
        }
        
        final long b = System.nanoTime();
        System.out.println("--- ..." + BufferedImageHelperPerfs.class.getSimpleName()
            + ", " + TestUtils.nsToSRounded(b-a) + " s ---");
    }
    
    /*
     * 
     */
    
    private static void bench_getArgb32At_nonPremul_opaque() {
        bench_getArgb32At_xxx(false, false);
    }
    
    private static void bench_getArgb32At_nonPremul_translucent() {
        bench_getArgb32At_xxx(false, true);
    }
    
    private static void bench_getArgb32At_premul_opaque() {
        bench_getArgb32At_xxx(true, false);
    }
    
    private static void bench_getArgb32At_premul_translucent() {
        bench_getArgb32At_xxx(true, true);
    }
    
    private static void bench_getArgb32At_xxx(
        boolean getPremulElseNonPremul,
        boolean withTranslucency) {
        
        final int width = SMALL_IMAGE_WIDTH;
        final int height = SMALL_IMAGE_HEIGHT;
        
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            if (withTranslucency
                && (image.getTransparency() != Transparency.TRANSLUCENT)) {
                // N/A
                continue;
            }
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            final int imageType = image.getType();
            final ImageTypeEnum imageTypeEnum =
                ImageTypeEnum.enumByType().get(imageType);
            
            System.out.println();
            
            {
                final Random random = TestUtils.newRandom123456789L();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = random.nextInt();
                        if (!withTranslucency) {
                            argb = Argb32.toOpaque(argb);
                        }
                        image.setRGB(x, y, argb);
                    }
                }
            }
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperListForSinglePixel(image)) {
                
                for (int k = 0; k < NBR_OF_RUNS; k++) {
                    int x = 0;
                    int y = 0;
                    int antiOptim = 0;
                    final long a = System.nanoTime();
                    for (int i = 0; i < NBR_OF_CALLS; i++) {
                        if (++x == width) {
                            x = 0;
                            if (++y == height) {
                                y = 0;
                            }
                        }
                        final int read;
                        if (getPremulElseNonPremul) {
                            read = helper.getPremulArgb32At(x, y);
                        } else {
                            read = helper.getNonPremulArgb32At(x, y);
                        }
                        antiOptim += read;
                    }
                    final long b = System.nanoTime();
                    if (antiOptim == 0) {
                        System.out.println("rare");
                    }
                    final String typeStr;
                    if (pixelFormat != null) {
                        typeStr = pixelFormat + (imagePremul ? "_P" : "");
                    } else {
                        typeStr = imageTypeEnum.toString();
                    }
                    final String methodStr;
                    if (getPremulElseNonPremul) {
                        methodStr = "getP()";
                    } else {
                        methodStr = "getNp()";
                    }
                    System.out.println(NBR_OF_CALLS + " calls"
                        + ", " + methodStr
                        + ", " + (withTranslucency ? "(tr)" : "(op)")
                        + ", " + typeStr
                        + toStringHelperCapabilitiesForSinglePixel(helper)
                        + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static void bench_setArgb32At_nonPremul_opaque() {
        bench_setArgb32At_drawPointPremulAt_xxx(false, true, false);
    }
    
    private static void bench_setArgb32At_nonPremul_translucent() {
        bench_setArgb32At_drawPointPremulAt_xxx(false, true, true);
    }
    
    private static void bench_setArgb32At_premul_opaque() {
        bench_setArgb32At_drawPointPremulAt_xxx(true, true, false);
    }
    
    private static void bench_setArgb32At_premul_translucent() {
        bench_setArgb32At_drawPointPremulAt_xxx(true, true, true);
    }
    
    private static void bench_drawPointPremulAt_opaque() {
        bench_setArgb32At_drawPointPremulAt_xxx(true, false, false);
    }
    
    private static void bench_drawPointPremulAt_translucent() {
        bench_setArgb32At_drawPointPremulAt_xxx(true, false, true);
    }
    
    private static void bench_setArgb32At_drawPointPremulAt_xxx(
        boolean setPremulElseNonPremul,
        boolean setElseDraw,
        boolean withTranslucency) {
        
        if ((!setElseDraw)
            && (!setPremulElseNonPremul)) {
            // draw only accepts premul.
            throw new IllegalArgumentException();
        }
        
        final int width = SMALL_IMAGE_WIDTH;
        final int height = SMALL_IMAGE_HEIGHT;
        
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            if (withTranslucency
                && (image.getTransparency() != Transparency.TRANSLUCENT)) {
                // N/A
                continue;
            }
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            final int imageType = image.getType();
            final ImageTypeEnum imageTypeEnum =
                ImageTypeEnum.enumByType().get(imageType);
            
            System.out.println();
            
            final int[] toSetArr = new int[width * height];
            {
                final Random random = TestUtils.newRandom123456789L();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = random.nextInt();
                        if (!withTranslucency) {
                            argb = Argb32.toOpaque(argb);
                        }
                        if (setPremulElseNonPremul) {
                            argb = BindingColorUtils.toPremulAxyz32(argb);
                        }
                        toSetArr[y * width + x] = argb;
                    }
                }
            }
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperListForSinglePixel(image)) {
                
                for (int k = 0; k < NBR_OF_RUNS; k++) {
                    int x = 0;
                    int y = 0;
                    final long a = System.nanoTime();
                    for (int i = 0; i < NBR_OF_CALLS; i++) {
                        if (++x == width) {
                            x = 0;
                            if (++y == height) {
                                y = 0;
                            }
                        }
                        final int toSet = toSetArr[y * width + x];
                        if (setPremulElseNonPremul) {
                            if (setElseDraw) {
                                helper.setPremulArgb32At(x, y, toSet);
                            } else {
                                helper.drawPointPremulAt(x, y, toSet);
                            }
                        } else {
                            helper.setNonPremulArgb32At(x, y, toSet);
                        }
                    }
                    final long b = System.nanoTime();
                    final String typeStr;
                    if (pixelFormat != null) {
                        typeStr = pixelFormat + (imagePremul ? "_P" : "");
                    } else {
                        typeStr = imageTypeEnum.toString();
                    }
                    final String methodStr;
                    if (setPremulElseNonPremul) {
                        if (setElseDraw) {
                            methodStr = "setP()";
                        } else {
                            methodStr = "drawP()";
                        }
                    } else {
                        methodStr = "setNp()";
                    }
                    System.out.println(NBR_OF_CALLS + " calls"
                        + ", " + methodStr
                        + ", " + (withTranslucency ? "(tr)" : "(op)")
                        + ", " + typeStr
                        + toStringHelperCapabilitiesForSinglePixel(helper)
                        + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static void bench_getPixelsInto_3840_2160_opaque() {
        bench_getPixelsInto_xxx(2, 3840, 2160, false);
    }
    
    private static void bench_getPixelsInto_3840_2160_translucent() {
        bench_getPixelsInto_xxx(2, 3840, 2160, true);
    }
    
    private static void bench_getPixelsInto_xxx(
        int bulkNbrOfCalls,
        int width,
        int height,
        boolean withTranslucency) {
        
        final int color32ArrScanlineStride = width;
        final int[] color32Arr = new int[color32ArrScanlineStride * height];
        
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            if (withTranslucency
                && (image.getTransparency() != Transparency.TRANSLUCENT)) {
                // N/A
                continue;
            }
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            final int imageType = image.getType();
            
            if (pixelFormat != null) {
                if (!QUADRATIC_PIXEL_FORMAT_SET.contains(pixelFormat)) {
                    continue;
                }
            } else {
                if (!QUADRATIC_SRC_IMAGE_TYPE_SET.contains(imageType)) {
                    continue;
                }
            }
            
            final ImageTypeEnum imageTypeEnum =
                ImageTypeEnum.enumByType().get(imageType);
            
            // Randomizing input.
            {
                final Random random = TestUtils.newRandom123456789L();
                final BufferedImageHelper helperForSet =
                    new BufferedImageHelper(image);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = random.nextInt();
                        if (withTranslucency) {
                            // Letting alpha value.
                        } else {
                            argb = Argb32.toOpaque(argb);
                        }
                        helperForSet.setNonPremulArgb32At(x, y, argb);
                    }
                }
            }
            
            // Separation between input types.
            System.out.println();
            
            for (BihPixelFormat pixelFormatTo : BihPixelFormat.values()) {
                if (!QUADRATIC_PIXEL_FORMAT_SET.contains(pixelFormatTo)) {
                    continue;
                }
                
                for (boolean premulTo : BihTestUtils.newPremulArr(pixelFormatTo)) {
                    
                    for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                        
                        for (int k = 0; k < NBR_OF_RUNS; k++) {
                            int antiOptim = 0;
                            final long a = System.nanoTime();
                            for (int i = 0; i < bulkNbrOfCalls; i++) {
                                helper.getPixelsInto(
                                    color32Arr,
                                    color32ArrScanlineStride,
                                    pixelFormatTo,
                                    premulTo);
                                antiOptim += color32Arr[0];
                            }
                            final long b = System.nanoTime();
                            if (antiOptim == 0) {
                                System.out.println("rare");
                            }
                            final String methodStr = "getPix()(" + width + "x" + height + ")";
                            final String typeFromStr;
                            if (pixelFormat != null) {
                                typeFromStr = pixelFormat + (imagePremul ? "_P" : "");
                            } else {
                                typeFromStr = imageTypeEnum.toString();
                            }
                            final String typeToStr = pixelFormatTo + (premulTo ? "_P" : "");
                            System.out.println(bulkNbrOfCalls + " call"
                                + (bulkNbrOfCalls >= 2 ? "s" : "")
                                + ", " + methodStr
                                + ", " + (withTranslucency ? "(tr)" : "(op)")
                                + ", " + typeFromStr
                                + "->" + typeToStr
                                + toStringHelperCapabilitiesPotential(helper)
                                + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                        }
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static void bench_setPixelsFrom_3840_2160_opaque() {
        bench_setPixelsFrom_xxx(2, 3840, 2160, false);
    }
    
    private static void bench_setPixelsFrom_3840_2160_translucent() {
        bench_setPixelsFrom_xxx(2, 3840, 2160, true);
    }
    
    private static void bench_setPixelsFrom_xxx(
        int bulkNbrOfCalls,
        int width,
        int height,
        boolean withTranslucency) {
        
        final int color32ArrScanlineStride = width;
        final int[] color32Arr = new int[color32ArrScanlineStride * height];
        
        for (BihPixelFormat pixelFormatFrom : BihPixelFormat.values()) {
            if (withTranslucency
                && (!pixelFormatFrom.hasAlpha())) {
                // N/A
                continue;
            }
            
            if (!QUADRATIC_PIXEL_FORMAT_SET.contains(pixelFormatFrom)) {
                continue;
            }
            
            for (boolean premulFrom : BihTestUtils.newPremulArr(pixelFormatFrom)) {
                
                // Randomizing input.
                {
                    final Random random = TestUtils.newRandom123456789L();
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int argb32 = random.nextInt();
                            if (withTranslucency) {
                                // Letting alpha value.
                            } else {
                                argb32 = Argb32.toOpaque(argb32);
                            }
                            if (premulFrom) {
                                argb32 = BindingColorUtils.toPremulAxyz32(argb32);
                            }
                            final int pixel =
                                pixelFormatFrom.toPixelFromArgb32(
                                    argb32);
                            color32Arr[y * color32ArrScanlineStride + x] = pixel;
                        }
                    }
                }
                
                // Separation between input types.
                System.out.println();
                
                for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
                    
                    final boolean imagePremul = image.isAlphaPremultiplied();
                    
                    final BihPixelFormat pixelFormat =
                        BufferedImageHelper.computePixelFormat(image);
                    final int imageType = image.getType();
                    
                    if (pixelFormat != null) {
                        if (!QUADRATIC_PIXEL_FORMAT_SET.contains(pixelFormat)) {
                            continue;
                        }
                    } else {
                        if (!QUADRATIC_SRC_IMAGE_TYPE_SET.contains(imageType)) {
                            continue;
                        }
                    }
                    
                    final ImageTypeEnum imageTypeEnum =
                        ImageTypeEnum.enumByType().get(imageType);
                    
                    for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                        
                        for (int k = 0; k < NBR_OF_RUNS; k++) {
                            int antiOptim = 0;
                            final long a = System.nanoTime();
                            for (int i = 0; i < bulkNbrOfCalls; i++) {
                                helper.setPixelsFrom(
                                    color32Arr,
                                    color32ArrScanlineStride,
                                    pixelFormatFrom,
                                    premulFrom);
                                antiOptim += helper.getArgb32At(0, 0, imagePremul);
                            }
                            final long b = System.nanoTime();
                            if (antiOptim == 0) {
                                System.out.println("rare");
                            }
                            final String methodStr = "setPix()(" + width + "x" + height + ")";
                            final String typeFromStr = pixelFormatFrom + (premulFrom ? "_P" : "");
                            final String typeToStr;
                            if (pixelFormat != null) {
                                typeToStr = pixelFormat + (imagePremul ? "_P" : "");
                            } else {
                                typeToStr = imageTypeEnum.toString();
                            }
                            System.out.println(bulkNbrOfCalls + " call"
                                + (bulkNbrOfCalls >= 2 ? "s" : "")
                                + ", " + methodStr
                                + ", " + (withTranslucency ? "(tr)" : "(op)")
                                + ", " + typeFromStr
                                + "->" + typeToStr
                                + toStringHelperCapabilitiesPotential(helper)
                                + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                        }
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static String toStringHelperCapabilitiesForSinglePixel(
        BufferedImageHelper helper) {
        
        final String ret;
        if (helper.isArrayDirectlyUsedForSinglePixelMethods()) {
            if (!helper.isColorModelAvoidedForSinglePixelMethods()) {
                throw new AssertionError();
            }
            ret = ", (cma)(arr)";
        } else if (helper.isColorModelAvoidedForSinglePixelMethods()) {
            ret = ", (cma)";
        } else {
            ret = "";
        }
        return ret;
    }
    
    private static String toStringHelperCapabilitiesPotential(
        BufferedImageHelper helper) {
        
        final String ret;
        if (helper.isArrayDirectUseAllowed()) {
            if (!helper.isColorModelAvoidingAllowed()) {
                throw new AssertionError();
            }
            ret = ", (?cma)(?arr)";
        } else if (helper.isColorModelAvoidingAllowed()) {
            ret = ", (?cma)";
        } else {
            ret = "";
        }
        return ret;
    }
}
