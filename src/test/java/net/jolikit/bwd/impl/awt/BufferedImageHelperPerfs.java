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

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final boolean MUST_BENCH_CLEAR_METHOD = true;
    private static final boolean MUST_BENCH_BULK_GET_METHODS = true;
    private static final boolean MUST_BENCH_BULK_SET_METHODS = true;
    private static final boolean MUST_BENCH_BULK_COPY_METHODS = true;
    
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
        }
        
        if (MUST_BENCH_CLEAR_METHOD) {
            bench_clearRect_3840_2160();
        }
        
        if (MUST_BENCH_BULK_GET_METHODS) {
            bench_getPixelsInto_3840_2160_opaque();
            
            bench_getPixelsInto_3840_2160_translucent();
        }
        
        if (MUST_BENCH_BULK_SET_METHODS) {
            bench_setPixelsFrom_3840_2160_opaque();
            
            bench_setPixelsFrom_3840_2160_translucent();
        }
        
        if (MUST_BENCH_BULK_COPY_METHODS) {
            bench_copyImage_3840_2160_opaque();
            
            bench_copyImage_3840_2160_translucent();
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
        
        for (BufferedImage image : BihTestUtils.newImageList_forBench(width, height)) {
            if ((getPremulElseNonPremul || withTranslucency)
                && (image.getTransparency() == Transparency.OPAQUE)) {
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
                    final String typeStr = getPixelTypeStr(
                        imageTypeEnum,
                        pixelFormat,
                        imagePremul);
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
        bench_setArgb32At_xxx(false, false);
    }
    
    private static void bench_setArgb32At_nonPremul_translucent() {
        bench_setArgb32At_xxx(false, true);
    }
    
    private static void bench_setArgb32At_premul_opaque() {
        bench_setArgb32At_xxx(true, false);
    }
    
    private static void bench_setArgb32At_premul_translucent() {
        bench_setArgb32At_xxx(true, true);
    }
    
    private static void bench_setArgb32At_xxx(
        boolean setPremulElseNonPremul,
        boolean withTranslucency) {
        
        final int width = SMALL_IMAGE_WIDTH;
        final int height = SMALL_IMAGE_HEIGHT;
        
        for (BufferedImage image : BihTestUtils.newImageList_forBench(width, height)) {
            if ((setPremulElseNonPremul || withTranslucency)
                && (image.getTransparency() == Transparency.OPAQUE)) {
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
                            helper.setPremulArgb32At(x, y, toSet);
                        } else {
                            helper.setNonPremulArgb32At(x, y, toSet);
                        }
                    }
                    final long b = System.nanoTime();
                    final String typeStr = getPixelTypeStr(
                        imageTypeEnum,
                        pixelFormat,
                        imagePremul);
                    final String methodStr;
                    if (setPremulElseNonPremul) {
                        methodStr = "setP()";
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
    
    private void bench_clearRect_3840_2160() {
        bench_clearRect_xxx(10, 3840, 2160);
    }
    
    private static void bench_clearRect_xxx(
        int bulkNbrOfCalls,
        int width,
        int height) {
        
        /*
         * No need to bench premul vs non-premul,
         * or translucent vs opaque,
         * since input color is only converted once
         * into pixel to set.
         */
        final int clearNonPremulArgb32 = 0x87654321;
        
        // Separation between input types.
        System.out.println();
        
        for (BufferedImage image : BihTestUtils.newImageList_forBench(width, height)) {
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            final int imageType = image.getType();
            
            final ImageTypeEnum imageTypeEnum =
                ImageTypeEnum.enumByType().get(imageType);
            
            // clearRect() uses single-pixel CMA avoidance,
            // not (allow flag, drawImage()).
            for (BufferedImageHelper helper : BihTestUtils.newHelperListForSinglePixel(image)) {
                
                for (int k = 0; k < NBR_OF_RUNS; k++) {
                    int antiOptim = 0;
                    final long a = System.nanoTime();
                    for (int i = 0; i < bulkNbrOfCalls; i++) {
                        helper.clearRect(0, 0, width, height, clearNonPremulArgb32, false);
                        antiOptim += helper.getArgb32At(0, 0, imagePremul);
                    }
                    final long b = System.nanoTime();
                    if (antiOptim == 0) {
                        System.out.println("rare");
                    }
                    final String methodStr = "clear()(" + width + "x" + height + ")";
                    final String typeStr = getPixelTypeStr(
                        imageTypeEnum,
                        pixelFormat,
                        imagePremul);
                    System.out.println(bulkNbrOfCalls + " call"
                        + (bulkNbrOfCalls >= 2 ? "s" : "")
                        + ", " + methodStr
                        + ", " + typeStr
                        // clearRect() uses single-pixel CMA avoidance,
                        // not (allow flag, drawImage()).
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
        bench_getPixelsInto_xxx(1, 3840, 2160, false);
    }
    
    private static void bench_getPixelsInto_3840_2160_translucent() {
        bench_getPixelsInto_xxx(1, 3840, 2160, true);
    }
    
    private static void bench_getPixelsInto_xxx(
        int bulkNbrOfCalls,
        int width,
        int height,
        boolean withTranslucency) {
        
        final int color32ArrScanlineStride = width;
        final int[] color32Arr = new int[color32ArrScanlineStride * height];
        
        for (BufferedImage image : BihTestUtils.newImageList_forBench(width, height)) {
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
            
            for (BihPixelFormat dstPixelFormat : BihPixelFormat.values()) {
                if (!QUADRATIC_PIXEL_FORMAT_SET.contains(dstPixelFormat)) {
                    continue;
                }
                
                for (boolean dstPremul : BihTestUtils.newPremulArr(dstPixelFormat)) {
                    
                    for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                        
                        for (int k = 0; k < NBR_OF_RUNS; k++) {
                            int antiOptim = 0;
                            final long a = System.nanoTime();
                            for (int i = 0; i < bulkNbrOfCalls; i++) {
                                helper.getPixelsInto(
                                    0,
                                    0,
                                    //
                                    color32Arr,
                                    color32ArrScanlineStride,
                                    dstPixelFormat,
                                    dstPremul,
                                    0,
                                    0,
                                    //
                                    image.getWidth(),
                                    image.getHeight());
                                antiOptim += color32Arr[0];
                            }
                            final long b = System.nanoTime();
                            if (antiOptim == 0) {
                                System.out.println("rare");
                            }
                            final String methodStr = "getPix()(" + width + "x" + height + ")";
                            final String srcTypeStr = getPixelTypeStr(
                                imageTypeEnum,
                                pixelFormat,
                                imagePremul);
                            final String dstTypeStr = getPixelTypeStr(
                                null,
                                dstPixelFormat,
                                dstPremul);
                            System.out.println(bulkNbrOfCalls + " call"
                                + (bulkNbrOfCalls >= 2 ? "s" : "")
                                + ", " + methodStr
                                + ", " + (withTranslucency ? "(tr)" : "(op)")
                                + ", " + srcTypeStr
                                + "->" + dstTypeStr
                                + toStringHelperCapabilitiesForBulk(helper)
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
        bench_setPixelsFrom_xxx(1, 3840, 2160, false);
    }
    
    private static void bench_setPixelsFrom_3840_2160_translucent() {
        bench_setPixelsFrom_xxx(1, 3840, 2160, true);
    }
    
    private static void bench_setPixelsFrom_xxx(
        int bulkNbrOfCalls,
        int width,
        int height,
        boolean withTranslucency) {
        
        final int color32ArrScanlineStride = width;
        final int[] color32Arr = new int[color32ArrScanlineStride * height];
        
        for (BihPixelFormat srcPixelFormat : BihPixelFormat.values()) {
            if (withTranslucency
                && (!srcPixelFormat.hasAlpha())) {
                // N/A
                continue;
            }
            
            if (!QUADRATIC_PIXEL_FORMAT_SET.contains(srcPixelFormat)) {
                continue;
            }
            
            for (boolean srcPremul : BihTestUtils.newPremulArr(srcPixelFormat)) {
                
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
                            if (srcPremul) {
                                argb32 = BindingColorUtils.toPremulAxyz32(argb32);
                            }
                            final int pixel =
                                srcPixelFormat.toPixelFromArgb32(
                                    argb32);
                            color32Arr[y * color32ArrScanlineStride + x] = pixel;
                        }
                    }
                }
                
                // Separation between input types.
                System.out.println();
                
                for (BufferedImage image : BihTestUtils.newImageList_forBench(width, height)) {
                    
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
                                    srcPixelFormat,
                                    srcPremul,
                                    0,
                                    0,
                                    //
                                    0,
                                    0,
                                    //
                                    width,
                                    height);
                                antiOptim += helper.getArgb32At(0, 0, imagePremul);
                            }
                            final long b = System.nanoTime();
                            if (antiOptim == 0) {
                                System.out.println("rare");
                            }
                            final String methodStr = "setPix()(" + width + "x" + height + ")";
                            final String srcTypeStr = getPixelTypeStr(
                                null,
                                srcPixelFormat,
                                srcPremul);
                            final String dstTypeStr = getPixelTypeStr(
                                imageTypeEnum,
                                pixelFormat,
                                imagePremul);
                            System.out.println(bulkNbrOfCalls + " call"
                                + (bulkNbrOfCalls >= 2 ? "s" : "")
                                + ", " + methodStr
                                + ", " + (withTranslucency ? "(tr)" : "(op)")
                                + ", " + srcTypeStr
                                + "->" + dstTypeStr
                                + toStringHelperCapabilitiesForBulk(helper)
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
    
    private static void bench_copyImage_3840_2160_opaque() {
        bench_copyImage_xxx(1, 3840, 2160, false);
    }
    
    private static void bench_copyImage_3840_2160_translucent() {
        bench_copyImage_xxx(1, 3840, 2160, true);
    }
    
    private static void bench_copyImage_xxx(
        int bulkNbrOfCalls,
        int width,
        int height,
        boolean withTranslucency) {
        
        for (BufferedImage srcImage : BihTestUtils.newImageList_forBench(width, height)) {
            if (withTranslucency
                && (srcImage.getTransparency() != Transparency.TRANSLUCENT)) {
                // N/A
                continue;
            }
            
            final int srcImageType = srcImage.getType();
            final BihPixelFormat srcPixelFormat =
                BufferedImageHelper.computePixelFormat(srcImage);
            if (srcPixelFormat != null) {
                if (!QUADRATIC_PIXEL_FORMAT_SET.contains(srcPixelFormat)) {
                    continue;
                }
            } else {
                if (!QUADRATIC_SRC_IMAGE_TYPE_SET.contains(srcImageType)) {
                    continue;
                }
            }
            
            final ImageTypeEnum srcImageTypeEnum =
                ImageTypeEnum.enumByType().get(srcImageType);
            final boolean srcPremul = srcImage.isAlphaPremultiplied();
            
            // Randomizing input.
            {
                final Random random = TestUtils.newRandom123456789L();
                final BufferedImageHelper helperForSet =
                    new BufferedImageHelper(srcImage);
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
            
            for (BufferedImage dstImage : BihTestUtils.newImageList_forBench(width, height)) {
                if (withTranslucency
                    && (dstImage.getTransparency() != Transparency.TRANSLUCENT)) {
                    // N/A
                    continue;
                }
                
                final int dstImageType = dstImage.getType();
                final BihPixelFormat dstPixelFormat =
                    BufferedImageHelper.computePixelFormat(dstImage);
                if (dstPixelFormat != null) {
                    if (!QUADRATIC_PIXEL_FORMAT_SET.contains(dstPixelFormat)) {
                        continue;
                    }
                } else {
                    if (!QUADRATIC_SRC_IMAGE_TYPE_SET.contains(dstImageType)) {
                        continue;
                    }
                }
                
                final ImageTypeEnum dstImageTypeEnum =
                    ImageTypeEnum.enumByType().get(dstImageType);
                final boolean dstPremul = dstImage.isAlphaPremultiplied();
                
                /*
                 * 
                 */
                
                final List<BufferedImageHelper> srcHelperList = new ArrayList<>();
                final List<BufferedImageHelper> dstHelperList = new ArrayList<>();
                {
                    /*
                     * Optimizations order to go crescendo
                     * on max used optimization,
                     * with priority to "src" over "dst". 
                     */
                    final List<Integer> srcOptimList = new ArrayList<>();
                    final List<Integer> dstOptimList = new ArrayList<>();
                    final int maxOptim = 2; // cma+arr
                    for (int hi = 0; hi <= maxOptim; hi++) {
                        for (int lo = 0; lo <= hi; lo++) {
                            // "dst" high first.
                            srcOptimList.add(hi);
                            dstOptimList.add(lo);
                            if (lo < hi) {
                                srcOptimList.add(lo);
                                dstOptimList.add(hi);
                            }
                        }
                    }
                    final int optimPairCount = srcOptimList.size();
                    for (int i = 0; i < optimPairCount; i++) {
                        final int srcOptim = srcOptimList.get(i);
                        final int dstOptim = dstOptimList.get(i);
                        final boolean srcCma = (srcOptim >= 1);
                        final boolean srcAdu = (srcOptim == 2);
                        final boolean dstCma = (dstOptim >= 1);
                        final boolean dstAdu = (dstOptim == 2);
                        final BufferedImageHelper srcHelper =
                            new BufferedImageHelper(
                                srcImage, srcCma, srcAdu);
                        if (srcHelper.isArrayDirectUseAllowed()
                            && (!srcHelper.isArrayDirectlyUsed())) {
                            // Optimization ineffective: not benching it.
                            continue;
                        }
                        final BufferedImageHelper dstHelper =
                            new BufferedImageHelper(
                                dstImage, dstCma, dstAdu);
                        if (dstHelper.isArrayDirectUseAllowed()
                            && (!dstHelper.isArrayDirectlyUsed())) {
                            // Optimization ineffective: not benching it.
                            continue;
                        }
                        srcHelperList.add(srcHelper);
                        dstHelperList.add(dstHelper);
                    }
                }
                final int helperPairCount = srcHelperList.size();
                for (int helperIndex = 0; helperIndex < helperPairCount; helperIndex++) {
                    final BufferedImageHelper srcHelper = srcHelperList.get(helperIndex);
                    final BufferedImageHelper dstHelper = dstHelperList.get(helperIndex);
                    
                    for (int k = 0; k < NBR_OF_RUNS; k++) {
                        int antiOptim = 0;
                        final long a = System.nanoTime();
                        for (int i = 0; i < bulkNbrOfCalls; i++) {
                            BufferedImageHelper.copyImage(
                                srcHelper,
                                0,
                                0,
                                dstHelper,
                                0,
                                0,
                                width,
                                height);
                            antiOptim += dstHelper.getNonPremulArgb32At(0, 0);
                        }
                        final long b = System.nanoTime();
                        if (antiOptim == 0) {
                            System.out.println("rare");
                        }
                        final String methodStr = "copy()(" + width + "x" + height + ")";
                        final String srcTypeStr = getPixelTypeStr(
                            srcImageTypeEnum,
                            srcPixelFormat,
                            srcPremul);
                        final String dstTypeStr = getPixelTypeStr(
                            dstImageTypeEnum,
                            dstPixelFormat,
                            dstPremul);
                        System.out.println(bulkNbrOfCalls + " call"
                            + (bulkNbrOfCalls >= 2 ? "s" : "")
                            + ", " + methodStr
                            + ", " + (withTranslucency ? "(tr)" : "(op)")
                            + ", " + srcTypeStr
                            + "->" + dstTypeStr
                            + toStringHelperCapabilitiesForBulk(srcHelper, dstHelper)
                            + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static String getPixelTypeStr(
        ImageTypeEnum imageTypeEnum,
        BihPixelFormat pixelFormat,
        boolean premul) {
        final String ret;
        if (pixelFormat != null) {
            ret = pixelFormat + (premul ? "_P" : "");
        } else if (imageTypeEnum != null) {
            ret = imageTypeEnum.toString();
        } else {
            ret = "TYPE_CUSTOM(unknown)";
        }
        return ret;
    }
    
    private static String toStringHelperCapabilitiesForSinglePixel(
        BufferedImageHelper helper) {
        
        final String ret;
        if (helper.isArrayDirectlyUsed()) {
            if (!helper.isColorModelAvoidedForSinglePixelMethods()) {
                throw new AssertionError();
            }
            ret = ", (cma+arr)";
        } else if (helper.isColorModelAvoidedForSinglePixelMethods()) {
            ret = ", (cma)";
        } else {
            ret = "";
        }
        return ret;
    }
    
    private static String toStringHelperCapabilitiesForBulk(
        BufferedImageHelper helper) {
        
        final String ret;
        if (helper.isArrayDirectlyUsed()) {
            if (!helper.isColorModelAvoidingAllowed()) {
                throw new AssertionError();
            }
            ret = ", (cma+arr)";
        } else if (helper.isColorModelAvoidingAllowed()) {
            ret = ", (cma)";
        } else {
            ret = "";
        }
        return ret;
    }
    
    private static String toStringHelperCapabilitiesForBulk(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper) {
        
        final StringBuilder sb1 = new StringBuilder();
        if (srcHelper.isArrayDirectlyUsed()) {
            if (!srcHelper.isColorModelAvoidingAllowed()) {
                throw new AssertionError();
            }
            sb1.append("(cma+arr)");
        } else if (srcHelper.isColorModelAvoidingAllowed()) {
            sb1.append("(cma)");
        } else {
            sb1.append("()");
        }
        
        final StringBuilder sb2 = new StringBuilder();
        if (dstHelper.isArrayDirectlyUsed()) {
            if (!dstHelper.isColorModelAvoidingAllowed()) {
                throw new AssertionError();
            }
            sb2.append("(cma+arr)");
        } else if (dstHelper.isColorModelAvoidingAllowed()) {
            sb2.append("(cma)");
        } else {
            sb2.append("()");
        }
        
        final String ret;
        if ((sb1.length() > 2)
            || (sb2.length() > 2)) {
            ret = ", " + sb1.toString() + "->" + sb2.toString();
        } else {
            ret = ""; 
        }
        return ret;
    }
}
