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
                    final String typeStr = getPixelTypeStr(
                        imageTypeEnum,
                        pixelFormat,
                        imagePremul);
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
                            final String typeFromStr = getPixelTypeStr(
                                imageTypeEnum,
                                pixelFormat,
                                imagePremul);
                            final String typeToStr = getPixelTypeStr(
                                null,
                                pixelFormatTo,
                                premulTo);
                            System.out.println(bulkNbrOfCalls + " call"
                                + (bulkNbrOfCalls >= 2 ? "s" : "")
                                + ", " + methodStr
                                + ", " + (withTranslucency ? "(tr)" : "(op)")
                                + ", " + typeFromStr
                                + "->" + typeToStr
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
                                    pixelFormatFrom,
                                    premulFrom);
                                antiOptim += helper.getArgb32At(0, 0, imagePremul);
                            }
                            final long b = System.nanoTime();
                            if (antiOptim == 0) {
                                System.out.println("rare");
                            }
                            final String methodStr = "setPix()(" + width + "x" + height + ")";
                            final String typeFromStr = getPixelTypeStr(
                                null,
                                pixelFormatFrom,
                                premulFrom);
                            final String typeToStr = getPixelTypeStr(
                                imageTypeEnum,
                                pixelFormat,
                                imagePremul);
                            System.out.println(bulkNbrOfCalls + " call"
                                + (bulkNbrOfCalls >= 2 ? "s" : "")
                                + ", " + methodStr
                                + ", " + (withTranslucency ? "(tr)" : "(op)")
                                + ", " + typeFromStr
                                + "->" + typeToStr
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
        
        for (BufferedImage imageFrom : BihTestUtils.newImageList_forBench(width, height)) {
            if (withTranslucency
                && (imageFrom.getTransparency() != Transparency.TRANSLUCENT)) {
                // N/A
                continue;
            }
            
            final int imageTypeFrom = imageFrom.getType();
            final BihPixelFormat pixelFormatFrom =
                BufferedImageHelper.computePixelFormat(imageFrom);
            if (pixelFormatFrom != null) {
                if (!QUADRATIC_PIXEL_FORMAT_SET.contains(pixelFormatFrom)) {
                    continue;
                }
            } else {
                if (!QUADRATIC_SRC_IMAGE_TYPE_SET.contains(imageTypeFrom)) {
                    continue;
                }
            }
            
            final ImageTypeEnum imageTypeEnumFrom =
                ImageTypeEnum.enumByType().get(imageTypeFrom);
            final boolean premulFrom = imageFrom.isAlphaPremultiplied();
            
            // Randomizing input.
            {
                final Random random = TestUtils.newRandom123456789L();
                final BufferedImageHelper helperForSet =
                    new BufferedImageHelper(imageFrom);
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
            
            for (BufferedImage imageTo : BihTestUtils.newImageList_forBench(width, height)) {
                if (withTranslucency
                    && (imageTo.getTransparency() != Transparency.TRANSLUCENT)) {
                    // N/A
                    continue;
                }
                
                final int imageTypeTo = imageTo.getType();
                final BihPixelFormat pixelFormatTo =
                    BufferedImageHelper.computePixelFormat(imageTo);
                if (pixelFormatTo != null) {
                    if (!QUADRATIC_PIXEL_FORMAT_SET.contains(pixelFormatTo)) {
                        continue;
                    }
                } else {
                    if (!QUADRATIC_SRC_IMAGE_TYPE_SET.contains(imageTypeTo)) {
                        continue;
                    }
                }
                
                final ImageTypeEnum imageTypeEnumTo =
                    ImageTypeEnum.enumByType().get(imageTypeTo);
                final boolean premulTo = imageTo.isAlphaPremultiplied();
                
                /*
                 * 
                 */
                
                final List<BufferedImageHelper> helperFromList = new ArrayList<>();
                final List<BufferedImageHelper> helperToList = new ArrayList<>();
                {
                    /*
                     * Optimizations order to go crescendo
                     * on max used optimization,
                     * with priority to "from" over "to". 
                     */
                    final List<Integer> optimFromList = new ArrayList<>();
                    final List<Integer> optimToList = new ArrayList<>();
                    final int maxOptim = 2; // cma+arr
                    for (int hi = 0; hi <= maxOptim; hi++) {
                        for (int lo = 0; lo <= hi; lo++) {
                            // "from" high first.
                            optimFromList.add(hi);
                            optimToList.add(lo);
                            if (lo < hi) {
                                optimFromList.add(lo);
                                optimToList.add(hi);
                            }
                        }
                    }
                    final int optimPairCount = optimFromList.size();
                    for (int i = 0; i < optimPairCount; i++) {
                        final int optimFrom = optimFromList.get(i);
                        final int optimTo = optimToList.get(i);
                        final boolean cmaFrom = (optimFrom >= 1);
                        final boolean aduFrom = (optimFrom == 2);
                        final boolean cmaTo = (optimTo >= 1);
                        final boolean aduTo = (optimTo == 2);
                        final BufferedImageHelper helperFrom =
                            new BufferedImageHelper(
                                imageFrom, cmaFrom, aduFrom);
                        if (helperFrom.isArrayDirectUseAllowed()
                            && (!helperFrom.isArrayDirectlyUsed())) {
                            // Optimization ineffective: not benching it.
                            continue;
                        }
                        final BufferedImageHelper helperTo =
                            new BufferedImageHelper(
                                imageTo, cmaTo, aduTo);
                        if (helperTo.isArrayDirectUseAllowed()
                            && (!helperTo.isArrayDirectlyUsed())) {
                            // Optimization ineffective: not benching it.
                            continue;
                        }
                        helperFromList.add(helperFrom);
                        helperToList.add(helperTo);
                    }
                }
                final int helperPairCount = helperFromList.size();
                for (int helperIndex = 0; helperIndex < helperPairCount; helperIndex++) {
                    final BufferedImageHelper helperFrom = helperFromList.get(helperIndex);
                    final BufferedImageHelper helperTo = helperToList.get(helperIndex);
                    
                    for (int k = 0; k < NBR_OF_RUNS; k++) {
                        int antiOptim = 0;
                        final long a = System.nanoTime();
                        for (int i = 0; i < bulkNbrOfCalls; i++) {
                            BufferedImageHelper.copyImage(
                                helperFrom,
                                0,
                                0,
                                helperTo,
                                0,
                                0,
                                width,
                                height);
                            antiOptim += helperTo.getNonPremulArgb32At(0, 0);
                        }
                        final long b = System.nanoTime();
                        if (antiOptim == 0) {
                            System.out.println("rare");
                        }
                        final String methodStr = "copy()(" + width + "x" + height + ")";
                        final String typeFromStr = getPixelTypeStr(
                            imageTypeEnumFrom,
                            pixelFormatFrom,
                            premulFrom);
                        final String typeToStr = getPixelTypeStr(
                            imageTypeEnumTo,
                            pixelFormatTo,
                            premulTo);
                        System.out.println(bulkNbrOfCalls + " call"
                            + (bulkNbrOfCalls >= 2 ? "s" : "")
                            + ", " + methodStr
                            + ", " + (withTranslucency ? "(tr)" : "(op)")
                            + ", " + typeFromStr
                            + "->" + typeToStr
                            + toStringHelperCapabilitiesForBulk(helperFrom, helperTo)
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
        BufferedImageHelper helperFrom,
        BufferedImageHelper helperTo) {
        
        final StringBuilder sb1 = new StringBuilder();
        if (helperFrom.isArrayDirectlyUsed()) {
            if (!helperFrom.isColorModelAvoidingAllowed()) {
                throw new AssertionError();
            }
            sb1.append("(cma+arr)");
        } else if (helperFrom.isColorModelAvoidingAllowed()) {
            sb1.append("(cma)");
        } else {
            sb1.append("()");
        }
        
        final StringBuilder sb2 = new StringBuilder();
        if (helperTo.isArrayDirectlyUsed()) {
            if (!helperTo.isColorModelAvoidingAllowed()) {
                throw new AssertionError();
            }
            sb2.append("(cma+arr)");
        } else if (helperTo.isColorModelAvoidingAllowed()) {
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
