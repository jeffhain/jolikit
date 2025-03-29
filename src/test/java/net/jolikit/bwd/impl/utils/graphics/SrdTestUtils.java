/*
 * Copyright 2025 Jeff Hain
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

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.awt.BihTestUtils;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.awt.TestImageTypeEnum;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.ExecutorParallelizer;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.PrlUtils;

public class SrdTestUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Same as Jimsizr's AbstractParallelScaler.MAX_RUNNABLE_COUNT_PER_CORE.
     */
    private static final int DISCREPANCY = 10;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyDefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger THREAD_NUM_PROVIDER =
            new AtomicInteger();
        public MyDefaultThreadFactory() {
        }
        @Override
        public Thread newThread(Runnable runnable) {
            final int threadNum = THREAD_NUM_PROVIDER.incrementAndGet();
            final String threadName =
                SrdTestUtils.class.getClass().getSimpleName()
                + "-PRL-"
                + threadNum;
            
            final Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            
            return thread;
        }
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private SrdTestUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Need actual and pretended parallelism to be able to check
     * fake parallelism (parallel code, but a single thread),
     * since code relies on parallelizer.getParallelism().
     * 
     * @param actualParallelism For ThreadPoolExecutor.
     * @param returnedParallelism For parallelizer.getParallelism().
     * @return A parallelizer using daemon threads.
     */
    public static InterfaceParallelizer newParallelizer(
        int actualParallelism,
        int returnedParallelism) {
        final ThreadPoolExecutor tpe = new ThreadPoolExecutor(
            actualParallelism,
            actualParallelism,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new MyDefaultThreadFactory());
        final int maxDepth =
            PrlUtils.computeMaxDepth(
                returnedParallelism,
                DISCREPANCY);
        return new ExecutorParallelizer(
            tpe,
            returnedParallelism,
            maxDepth);
    }
    
    public static void shutdownNow(InterfaceParallelizer parallelizer) {
        // Best effort.
        if (parallelizer instanceof ExecutorParallelizer) {
            final ExecutorParallelizer impl =
                (ExecutorParallelizer) parallelizer;
            final Executor executor = impl.getExecutor();
            if (executor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor) executor).shutdownNow();
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * @return ARGB premul or non-premul helper.
     */
    public static InterfaceColorTypeHelper getColorTypeHelper(boolean premul) {
        final InterfaceColorTypeHelper ret;
        if (premul) {
            ret = PremulArgbHelper.getInstance();
        } else {
            ret = NonPremulArgbHelper.getInstance();
        }
        return ret;
    }
    
    /**
     * @param imageTypeEnum Must correspond to supported helpers.
     */
    public static InterfaceColorTypeHelper getColorTypeHelper(
        TestImageTypeEnum imageTypeEnum) {
        
        final InterfaceColorTypeHelper ret;
        if (imageTypeEnum == TestImageTypeEnum.TYPE_INT_ARGB) {
            ret = NonPremulArgbHelper.getInstance();
        } else if (imageTypeEnum == TestImageTypeEnum.TYPE_INT_ARGB_PRE) {
            ret = PremulArgbHelper.getInstance();
        } else if (
            BindingBasicsUtils.NATIVE_IS_LITTLE
            ? (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR_PRE)
                : (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_RGBA_PRE)) {
            ret = PremulNativeRgbaHelper.getInstance();
        } else {
            throw new IllegalArgumentException("" + imageTypeEnum);
        }
        
        return ret;
    }
    
    /*
     * 
     */
    
    public static double randomMinMax(Random random, double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    /**
     * Makes sure close pixels don't have too different values.
     * 
     * @param opaque If image has no alpha, will have no effect.
     */
    public static void randomizeCloseNeighboors(
        Random random,
        BufferedImage image,
        int maxOrthoNeighboorDelta,
        boolean opaque,
        int minTranslucentAlpha8) {
        
        final BufferedImageHelper imageHelper =
            new BufferedImageHelper(image);
        final int width = image.getWidth();
        final int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                
                /*
                 * Each pixel not too different from
                 * the one above and before,
                 * for each pixel not be too different from
                 * its neighboors, else bicubic scaling
                 * might cause large deltas (in particular
                 * in case of low alpha).
                 */
                final int argb32 =
                    newRandomArgb32Pixel(
                        random,
                        maxOrthoNeighboorDelta,
                        opaque,
                        minTranslucentAlpha8,
                        imageHelper,
                        x,
                        y);
                
                imageHelper.setNonPremulArgb32At(x, y, argb32);
            }
        }
    }
    
    /*
     * 
     */
    
    public static void checkIsValidColor(
        InterfaceColorTypeHelper colorTypeHelper,
        int color32) {
        if (colorTypeHelper.isPremul()) {
            final int a8 = Argb32.getAlpha8(color32);
            final int b8 = Argb32.getRed8(color32);
            final int c8 = Argb32.getGreen8(color32);
            final int d8 = Argb32.getBlue8(color32);
            final int validColor32 =
                colorTypeHelper.toValidPremul32(a8, b8, c8, d8);
            if (color32 != validColor32) {
                throw new AssertionError(
                    "not valid premul color: "
                        + Argb32.toString(color32));
            }
        }
    }
    
    /**
     * If actual image is of type TYPE_BYTE_BINARY,
     * to avoid fails due to edge cases around threshold,
     * computes the ratio of white pixels of both images,
     * and converts it into a 8-bits component as if it was
     * a floating-point color in [0,1].
     * That works as long as the image is not too small.
     */
    public static int computeMaxCptDelta(
        BufferedImageHelper expectedDstHelper,
        BufferedImageHelper actualDstHelper,
        int neighborDelta) {
        
        final int ret;
        if (expectedDstHelper.getImageType() == BufferedImage.TYPE_BYTE_BINARY) {
            ret = computeMaxCptDelta_binary(
                expectedDstHelper,
                actualDstHelper,
                neighborDelta);
        } else {
            ret = computeMaxCptDelta_notBinary(
                expectedDstHelper,
                actualDstHelper,
                neighborDelta);
        }
        
        return ret;
    }
    
    public static int computeMeanColor32(
        InterfaceColorTypeHelper colorTypeHelper,
        InterfaceSrcPixels pixels,
        GRect rect) {
        if (rect.isEmpty()) {
            throw new IllegalArgumentException("rect must not be empty");
        }
        /*
         * fp values are biased
         * (twice less fp span for 0 and 255),
         * so we do mean on int values.
         */
        long a8Sum = 0;
        long b8Sum = 0;
        long c8Sum = 0;
        long d8Sum = 0;
        for (int y = rect.y(); y <= rect.yMax(); y++) {
            for (int x = rect.x(); x <= rect.xMax(); x++) {
                final int color32 = pixels.getColor32At(x, y);
                /*
                 * Our interpolations are done in premul,
                 * so checking mean premul color.
                 */
                final int premulColor32 =
                    colorTypeHelper.asPremul32FromType(color32);
                a8Sum += Argb32.getAlpha8(premulColor32);
                b8Sum += Argb32.getRed8(premulColor32);
                c8Sum += Argb32.getGreen8(premulColor32);
                d8Sum += Argb32.getBlue8(premulColor32);
            }
        }
        final double divisor = 255.0 * rect.areaLong();
        final int premulColor32 = Argb32.toArgb32FromFp(
            a8Sum / divisor,
            b8Sum / divisor,
            c8Sum / divisor,
            d8Sum / divisor);
        return colorTypeHelper.asTypeFromPremul32(premulColor32);
    }
    
    public static void checkCloseArgb32Over(
        InterfaceSrcPixels expected,
        InterfaceSrcPixels actual,
        GRect checkArea,
        int cptDeltaTol) {
        
        final GRect rect = expected.getRect();
        TestCase.assertEquals(rect, actual.getRect());
        
        for (int y = checkArea.y(); y <= checkArea.yMax(); y++) {
            for (int x = checkArea.x(); x <= checkArea.xMax(); x++) {
                final int expectedColor32 = expected.getColor32At(x, y);
                final int actualColor32 = actual.getColor32At(x, y);
                checkCloseColor32(
                    expectedColor32,
                    actualColor32,
                    cptDeltaTol);
            }
        }
    }
    
    public static void checkCloseColor32(
        int expectedColor32,
        int actualColor32,
        int cptDeltaTol) {
        
        final int maxCptDelta = BihTestUtils.getMaxCptDelta(
            expectedColor32,
            actualColor32);
        if (maxCptDelta > cptDeltaTol) {
            final String expectedStr = Argb32.toString(expectedColor32);
            final String actualStr = Argb32.toString(actualColor32);
            System.out.println();
            System.out.println("cptDeltaTol = " + cptDeltaTol
                + " = 0x" + Integer.toHexString(cptDeltaTol));
            System.out.println("maxCptDelta = " + maxCptDelta
                + " = 0x" + Integer.toHexString(maxCptDelta));
            System.out.println("expected = " + expectedStr);
            System.out.println("actual =   " + actualStr);
            throw new AssertionError(expectedStr + " != " + actualStr);
        }
    }
    
    /*
     * 
     */
    
    /**
     * Trivial implementation, based on single-pixel methods,
     * to use as reference for image copy correctness,
     * other than for exception checks.
     */
    public static void copyImage_reference(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper) {
        
        // Will throw if not same dimensions.
        final int width = Math.max(srcHelper.getWidth(), dstHelper.getWidth());
        final int height = Math.max(srcHelper.getHeight(), dstHelper.getHeight());
        
        BihTestUtils.copyImage_reference(
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
    
    public static void printPixels(String name, InterfaceSrcPixels pixels) {
        System.out.println(name + ":" + pixels);
    }
    
    public static String toStringPixels(int[][] pixelArrArr) {
        final int height = pixelArrArr.length;
        if (height == 0) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder();
        final int width = pixelArrArr[0].length;
        for (int j = 0; j < height; j++) {
            sb.append("\n");
            final int[] pixelArr = pixelArrArr[j];
            for (int i = 0; i < width; i++) {
                if (i == 0) {
                    sb.append("[");
                } else {
                    sb.append(",");
                }
                sb.append(Argb32.toString(pixelArr[i]));
                if (i == width - 1) {
                    sb.append("]");
                }
            }
        }
        return sb.toString();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param opaque If image has no alpha, will have no effect.
     */
    private static int newRandomArgb32Pixel(
        Random random,
        int maxOrthoNeighboorDelta,
        boolean opaque,
        int minTranslucentAlpha8,
        BufferedImageHelper srcHelper,
        int x,
        int y) {
        
        final int fallbackArgb32 = 0xFF888888;
        final int topArgb32 =
            (y != 0) ? srcHelper.getNonPremulArgb32At(x, y-1)
                : fallbackArgb32;
        final int leftArgb32 =
            (x != 0) ? srcHelper.getNonPremulArgb32At(x-1, y)
                : fallbackArgb32;
        
        // Half the time adding 1 before division by 2,
        // to avoid bias towards zero due to remainer ignoring.
        final int divBonus = (random.nextBoolean() ? 1 : 0);
        
        final int maxDelta = maxOrthoNeighboorDelta;
        int a8;
        if (opaque) {
            a8 = 0xFF;
        } else {
            a8 = ((Argb32.getAlpha8(topArgb32)
                + Argb32.getAlpha8(leftArgb32)
                + divBonus) >> 1);
            a8 += randomMinMax(random, -maxDelta, maxDelta);
            a8 = NbrsUtils.toRange(minTranslucentAlpha8, 0xFF, a8);
        }
        
        int r8 = ((Argb32.getRed8(topArgb32)
            + Argb32.getRed8(leftArgb32)
            + divBonus) >> 1);
        int g8 = ((Argb32.getGreen8(topArgb32)
            + Argb32.getGreen8(leftArgb32)
            + divBonus) >> 1);
        int b8 = ((Argb32.getBlue8(topArgb32)
            + Argb32.getBlue8(leftArgb32)
            + divBonus) >> 1);
        r8 += randomMinMax(random, -maxDelta, maxDelta);
        g8 += randomMinMax(random, -maxDelta, maxDelta);
        b8 += randomMinMax(random, -maxDelta, maxDelta);
        r8 = NbrsUtils.toRange(0, 0xFF, r8);
        g8 = NbrsUtils.toRange(0, 0xFF, g8);
        b8 = NbrsUtils.toRange(0, 0xFF, b8);
        
        return BindingColorUtils.toAbcd32_noCheck(a8, r8, g8, b8);
    }
    
    /*
     * 
     */
    
    private static int computeMaxCptDelta_binary(
        BufferedImageHelper expectedDstHelper,
        BufferedImageHelper actualDstHelper,
        int neighborDelta) {
        
        if ((expectedDstHelper.getImageType() != BufferedImage.TYPE_BYTE_BINARY)
            || (actualDstHelper.getImageType() != BufferedImage.TYPE_BYTE_BINARY)) {
            throw new IllegalArgumentException();
        }
        
        final int w = expectedDstHelper.getWidth();
        final int h = expectedDstHelper.getHeight();
        
        int diffCount = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int actualArgb32 =
                    actualDstHelper.getNonPremulArgb32At(x, y);
                final int minNearbyDelta =
                    getMinNearbyMaxCptDelta(
                        x,
                        y,
                        actualArgb32,
                        expectedDstHelper,
                        neighborDelta);
                if (minNearbyDelta != 0) {
                    diffCount++;
                }
            }
        }
        
        final int area = w * h;
        return BihTestUtils.getBinaryCpt8Delta(
            area,
            diffCount);
    }
    
    private static int computeMaxCptDelta_notBinary(
        BufferedImageHelper expectedDstHelper,
        BufferedImageHelper actualDstHelper,
        int neighborDelta) {
        
        int ret = 0;
        
        final int w = expectedDstHelper.getWidth();
        final int h = expectedDstHelper.getHeight();
        
        L1 : for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int actualArgb32 =
                    actualDstHelper.getNonPremulArgb32At(x, y);
                final int minNearbyDelta =
                    getMinNearbyMaxCptDelta(
                        x,
                        y,
                        actualArgb32,
                        expectedDstHelper,
                        neighborDelta);
                ret = Math.max(ret, minNearbyDelta);
                if (ret == 0xFF) {
                    // Can't be worse.
                    break L1;
                }
            }
        }
        
        return ret;
    }
    
    /**
     * Computes minOnPixels(maxForPixelOnCpts()).
     * 
     * @return The min, on each compared pixel, of the max delta
     *         between each component.
     */
    private static int getMinNearbyMaxCptDelta(
        int x,
        int y,
        int actualDstArgb32,
        BufferedImageHelper expectedDstHelper,
        int neighborDelta) {
        
        final int w = expectedDstHelper.getWidth();
        final int h = expectedDstHelper.getHeight();
        
        int minNearbyDelta = Integer.MAX_VALUE;
        L1 : for (int j = -neighborDelta; j <= neighborDelta; j++) {
            final int yy = NbrsUtils.toRange(0, h-1, y + j);
            for (int i = -neighborDelta; i <= neighborDelta; i++) {
                final int xx = NbrsUtils.toRange(0, w-1, x + i);
                final int expectedArgb32 =
                    expectedDstHelper.getNonPremulArgb32At(xx, yy);
                final int pixelMaxCptDelta =
                    BihTestUtils.getMaxCptDelta(
                        expectedArgb32,
                        actualDstArgb32);
                minNearbyDelta = Math.min(
                    minNearbyDelta,
                    pixelMaxCptDelta);
                if (minNearbyDelta == 0) {
                    break L1;
                }
            }
        }
        
        return minNearbyDelta;
    }
}
