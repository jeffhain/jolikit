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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.awt.BihTestUtils;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.awt.TestImageTypeEnum;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Tests complementary to the ones extending AbstractSrdTest
 * (which unlike this one, test with partial src rectangles).
 */
public class ScaledRectDrawingTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    private static final boolean MUST_PRINT_IMAGE_ON_ERROR =
        BihTestUtils.MUST_PRINT_IMAGE_ON_ERROR;
    
    /**
     * True to compute tolerated deltas to copy in ScaledRectDrawingTestGen,
     * and visually check that they make sense (or debug if not),
     * false to test algos against them (for non-regression,
     * and to test that true parallelism is as accurate
     * as fake parallelism).
     */
    private static final boolean MAX_DELTA_COMPUTATION_MODE = false;
    
    /**
     * Should be large enough to cover various pixel cases,
     * and trigger eventual issues (some won't show up if too small).
     * 
     * With growths to x4 for each span, (64,32) gives
     * (256,128) destination images, i.e. images with 32 * 1024 pixels,
     * which should be enough to pass most thresholds.
     * In case it's not enough, we also use small thresholds
     * for tested implementations.
     */
    private static final int SRC_IMAGE_MAX_WIDTH = 64;
    private static final int SRC_IMAGE_MAX_HEIGHT = 32;
    
    /**
     * The larger the slower,
     * but also the more random cases covered.
     */
    private static final int RANDOM_DRAW_COUNT = 100;
    
    /**
     * When MAX_DELTA_COMPUTATION_MODE is true,
     * if encountering a larger delta (i.e. 0xFF),
     * we fail, because we don't mean to measure accuracy
     * on completely inaccurate operations.
     */
    private static final int MAX_COMPUTED_DELTA = 0xFE;
    
    private static int getCheckNeighborDelta(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake) {
        
        final int checkNeighborDelta;
        if (cmpType == MyCmpType.WITH_REF) {
            /*
             * AWT/JLK might not round to the same source pixel.
             * With splits on resize, it's worse.
             */
            final boolean withSplit = (prlTrueElseFake != null);
            final boolean gotPixelDeltaDueToSplits =
                (opType != MyOpType.COPY)
                && withSplit;
            if (gotPixelDeltaDueToSplits) {
                checkNeighborDelta = 2;
            } else {
                checkNeighborDelta = 1;
            }
        } else {
            checkNeighborDelta = 0;
        }
        return checkNeighborDelta;
    }
    
    /**
     * Max component delta by use case,
     * for tests scenarios done in this class.
     * 
     * If empty, means no constraint.
     */
    private static final SortedMap<String,Integer> MAX_DELTA_BY_CASE;
    static {
        final SortedMap<String,Integer> m =  new TreeMap<>();
        if (MAX_DELTA_COMPUTATION_MODE) {
            // Will compute map thresholds.
            MAX_DELTA_BY_CASE = m;
        } else {
            // Will check that map thresholds are still valid.
            ScaledRectDrawingTestGen.populateMaxDeltaByCase(m);
            //
            MAX_DELTA_BY_CASE = Collections.unmodifiableSortedMap(m);
        }
    }
    
    private static String computeDeltaCase(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake,
        TestImageTypeEnum imageTypeEnum) {
        
        final StringBuilder sb = new StringBuilder();
        
        if (cmpType == MyCmpType.WITH_REF) {
            sb.append("vsRef");
        } else if (cmpType == MyCmpType.WITH_BACK) {
            sb.append("vsBack");
        } else {
            throw new IllegalArgumentException("" + cmpType);
        }
        
        sb.append("-");
        if (opType == MyOpType.COPY) {
            sb.append("copy");
        } else if (opType == MyOpType.RANDOM_NON_UNIFORM) {
            sb.append("resz-nonUni");
        } else if ((opType == MyOpType.RANDOM_UNIFORM_UP)
            || (opType == MyOpType.POWERS_OF_TWO)) {
            sb.append("resz-uni");
        } else {
            throw new IllegalArgumentException("" + opType);
        }
        
        sb.append("-(");
        sb.append(toStringImageTypeCase(imageTypeEnum));
        sb.append(")");
        
        sb.append("-");
        if (prlTrueElseFake == null) {
            sb.append("seq");
        } else {
            // Computing deltas with "fakePrl",
            // using them for checks for both "fakePrl" and "truePrl".
            sb.append("prl");
        }
        
        return sb.toString();
    }
    
    private static String toStringImageTypeCase(TestImageTypeEnum imageTypeEnum) {
        final String ret;
        switch (imageTypeEnum) {
            case TYPE_INT_ARGB: ret = "ARGB_N"; break;
            case TYPE_INT_ARGB_PRE: ret = "ARGB_P"; break;
            case TYPE_CUSTOM_INT_ABGR_PRE: ret = "ABGR_P"; break;
            case TYPE_CUSTOM_INT_BGRA_PRE: ret = "BGRA_P"; break;
            default:
                throw new IllegalArgumentException("" + imageTypeEnum);
        }
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * @return Map of reference drawer (possibly null) by drawer to test.
     */
    private Map<InterfaceScaledRectDrawer,InterfaceScaledRectDrawer> newRefDrawerByTestedDrawer(
        Boolean prlTrueElseFake) {
        
        // Linked for iteration determinism.
        final Map<InterfaceScaledRectDrawer,InterfaceScaledRectDrawer> refByTested =
            new LinkedHashMap<>();
        
        /*
         * 
         */
        
        final boolean withSplit = (prlTrueElseFake != null);
        final int splitThreshold;
        if (withSplit) {
            splitThreshold = Math.min(32, SRC_IMAGE_MAX_WIDTH);
        } else {
            splitThreshold = Integer.MAX_VALUE;
        }
        
        /*
         * NEAREST
         */
        
        {
            final InterfaceScaledRectDrawer ref =
                new SrdNearestAwt();
            
            refByTested.put(new SrdNearest(splitThreshold), ref);
        }
        
        /*
         * BOXSAMPLED
         */
        
        {
            // No AWT equivalent.
            final InterfaceScaledRectDrawer ref = null;
            
            refByTested.put(new SrdBoxsampled(
                splitThreshold,
                splitThreshold),
                ref);
        }
        
        /*
         * ITERATIVE_BILINEAR
         */
        
        {
            final InterfaceScaledRectDrawer ref =
                new SrdIterBiliAwt();
            
            refByTested.put(new SrdIterBili(splitThreshold), ref);
        }
        
        /*
         * ITERATIVE_BICUBIC
         */
        
        {
            final InterfaceScaledRectDrawer ref =
                new SrdIterBicuAwt();
            
            refByTested.put(new SrdIterBicu(splitThreshold), ref);
        }
        
        /*
         * ScaledRectDrawing API
         */
        
        if (withSplit) {
            for (BwdScalingType scalingType : BwdScalingType.values()) {
                final InterfaceScaledRectDrawer drawer =
                    new InterfaceScaledRectDrawer() {
                    
                    @Override
                    public String toString() {
                        return "ScaledRectDrawing.drawScaledRect(" + scalingType + ")";
                    }
                    
                    @Override
                    public void drawScaledRect(
                        InterfaceParallelizer parallelizer,
                        InterfaceColorTypeHelper colorTypeHelper,
                        //
                        InterfaceSrcPixels srcPixels,
                        GRect srcRect,
                        //
                        GRect dstRect,
                        GRect dstClip,
                        InterfaceRowDrawer dstRowDrawer) {
                        
                        ScaledRectDrawing.drawScaledRect(
                            parallelizer,
                            scalingType,
                            colorTypeHelper,
                            //
                            srcPixels,
                            srcRect,
                            //
                            dstRect,
                            dstClip,
                            dstRowDrawer);
                    }
                };
                refByTested.put(drawer, null);
            }
        }
        
        return refByTested;
    }
    
    /*
     * 
     */
    
    /**
     * Not too small min growth,
     * else need more tolerance (pixels more diluted).
     */
    private static final double MIN_GROWTH = 1.4;
    
    /**
     * More than 2, to go beyond special cases,
     * for growths or shrinkings inferior to 2.
     */
    private static final double MAX_GROWTH = 2.4;
    
    /*
     * 
     */
    
    /**
     * Not too large else need more tolerance.
     * Large enough for colors to move around.
     */
    private static final int MAX_ORTHO_NEIGHBOR_DELTA = 0x0F;
    
    /**
     * Not too small else color can be altered a lot
     * due to operating in alpha-premultiplied color format.
     */
    private static final int MIN_TRANSLUCENT_ALPHA8 = 0xF0;
    
    /*
     * 
     */
    
    /**
     * Only those for which we have an InterfaceColorTypeHelper.
     */
    private static final List<TestImageTypeEnum> IMAGE_TYPE_ENUM_LIST;
    static {
        final List<TestImageTypeEnum> list = new ArrayList<>();
        list.add(TestImageTypeEnum.TYPE_INT_ARGB);
        list.add(TestImageTypeEnum.TYPE_INT_ARGB_PRE);
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            list.add(TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR_PRE);
        } else {
            list.add(TestImageTypeEnum.TYPE_CUSTOM_INT_RGBA_PRE);
        }
        IMAGE_TYPE_ENUM_LIST = Collections.unmodifiableList(list);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Initial copy/resize type.
     */
    private enum MyOpType {
        /**
         * dst spans = src spans
         */
        COPY,
        /**
         * Growth in a vicinity of two (which could be
         * a trigger limit for different scaling).
         */
        RANDOM_UNIFORM_UP,
        /**
         * Both growth and shrinking
         * (might be a trigger for different scaling).
         */
        RANDOM_NON_UNIFORM,
        /**
         * Growth a power of two (x2, x4, etc.)
         * (might trigger special cases).
         */
        POWERS_OF_TWO,
    }
    
    private enum MyCmpType {
        /**
         * Test against a reference implementation.
         */
        WITH_REF,
        /**
         * Test that inverse resizing, by resizing and then
         * resizing back to initial spans, is close to identity.
         * 
         * This allows to test somewhat accurate scaling
         * in a generic way, both for growth and shrinking
         * (assuming the chance for a bug in both cancelling itself
         * being low).
         */
        WITH_BACK,
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawingTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_copy_vsWithRef_seq() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_REF, null);
    }
    
    public void test_copy_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_REF, false);
    }
    
    public void test_copy_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_REF, true);
    }
    
    public void test_resize_randomUniform_vsWithRef_seq() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_REF, null);
    }
    
    public void test_resize_randomUniform_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_REF, false);
    }
    
    public void test_resize_randomUniform_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_REF, true);
    }
    
    public void test_resize_randomUniform_vsWithBack_seq() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_BACK, null);
    }
    
    public void test_resize_randomUniform_vsWithBack_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_BACK, false);
    }
    
    public void test_resize_randomUniform_vsWithBack_truePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_BACK, true);
    }
    
    public void test_resize_randomNonUniform_vsWithRef_seq() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_REF, null);
    }
    
    public void test_resize_randomNonUniform_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_REF, false);
    }
    
    public void test_resize_randomNonUniform_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_REF, true);
    }
    
    public void test_resize_randomNonUniform_vsWithBack_seq() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_BACK, null);
    }
    
    public void test_resize_randomNonUniform_vsWithBack_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_BACK, false);
    }
    
    public void test_resize_randomNonUniform_vsWithBack_truePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_BACK, true);
    }
    
    public void test_resize_powersOfTwo_vsWithRef_seq() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_REF, null);
    }
    
    public void test_resize_powersOfTwo_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_REF, false);
    }
    
    public void test_resize_powersOfTwo_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_REF, true);
    }
    
    public void test_resize_powersOfTwo_vsWithBack_seq() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_BACK, null);
    }
    
    public void test_resize_powersOfTwo_vsWithBack_fakePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_BACK, false);
    }
    
    public void test_resize_powersOfTwo_vsWithBack_truePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_BACK, true);
    }
    
    /*
     * 
     */
    
    /**
     * False parallelism allows to ensure sequential and identical
     * ordering of parallel runnables executions, and deterministic
     * triggering of split code coordinates issues.
     * True parallelism allows to trigger eventual concurrency bugs.
     * 
     * @param prlTrueElseFake True to use actual parallelism,
     *        false to use an executor with a single thread,
     *        null for sequential.
     */
    public void test_xxx(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake) {
        
        if ((opType == MyOpType.COPY)
            && (cmpType == MyCmpType.WITH_BACK)) {
            // Pointless, since we use a singel image type.
            throw new IllegalArgumentException();
        }
        
        final InterfaceParallelizer parallelizer;
        if (prlTrueElseFake != null) {
            // 2 for easier debug.
            final int actualParallelism = (prlTrueElseFake ? 2 : 1);
            final int returnedParallelism = 2;
            parallelizer = SrdTestUtils.newParallelizer(
                actualParallelism,
                returnedParallelism);
        } else {
            final int actualParallelism = 1;
            final int returnedParallelism = 1;
            parallelizer = SrdTestUtils.newParallelizer(
                actualParallelism,
                returnedParallelism);
        }
        try {
            this.test_xxx_exec(
                opType,
                cmpType,
                prlTrueElseFake,
                parallelizer);
        } finally {
            SrdTestUtils.shutdownNow(parallelizer);
        }
    }
    
    public void test_xxx_exec(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake,
        InterfaceParallelizer parallelizer) {
        
        for (Map.Entry<InterfaceScaledRectDrawer,InterfaceScaledRectDrawer> entry :
            newRefDrawerByTestedDrawer(prlTrueElseFake).entrySet()) {
            final InterfaceScaledRectDrawer drawer = entry.getKey();
            final InterfaceScaledRectDrawer refDrawer = entry.getValue();
            
            // Working hack.
            final boolean isNearest =
                drawer.toString().toLowerCase().contains("nearest");
            if (isNearest != (opType == MyOpType.COPY)) {
                /*
                 * If drawer is nearest and we have resize,
                 * won't be accurate enough to skipping.
                 * If drawer is not nearest and we have no resize,
                 * not interesting (should delegate to NEAREST) so skipping.
                 */
                continue;
            }
            
            /*
             * Same random for same tests for each drawer,
             * for fairness and not have unexpected failures.
             */
            final Random random = BihTestUtils.newRandom();
            
            for (TestImageTypeEnum imageTypeEnum : IMAGE_TYPE_ENUM_LIST) {
                final int srcWidth =
                    1 + random.nextInt(SRC_IMAGE_MAX_WIDTH);
                final int srcHeight =
                    1 + random.nextInt(SRC_IMAGE_MAX_HEIGHT);
                final BufferedImage srcImage =
                    BihTestUtils.newImage(
                        srcWidth,
                        srcHeight,
                        imageTypeEnum);
                
                // If image type has alpha, always having some translucent pixels.
                SrdTestUtils.randomizeCloseNeighboors(
                    random,
                    srcImage,
                    MAX_ORTHO_NEIGHBOR_DELTA,
                    false,
                    MIN_TRANSLUCENT_ALPHA8);
                
                final int myDrawCount;
                if (opType == MyOpType.COPY) {
                    // Don't gain much by doing more.
                    myDrawCount = 1;
                } else if (opType == MyOpType.POWERS_OF_TWO) {
                    // Just 4 to cover each span x4 per count.
                    myDrawCount = 4 * RANDOM_DRAW_COUNT;
                } else {
                    myDrawCount = RANDOM_DRAW_COUNT;
                }
                
                for (int rk = 0; rk < myDrawCount; rk++) {
                    final int dstWidth;
                    final int dstHeight;
                    if (opType == MyOpType.COPY) {
                        dstWidth = srcWidth;
                        dstHeight = srcHeight;
                    } else if (opType == MyOpType.RANDOM_UNIFORM_UP) {
                        dstWidth = newGrownSpan(random, srcWidth);
                        dstHeight = newGrownSpan(random, srcHeight);
                    } else if (opType == MyOpType.RANDOM_NON_UNIFORM) {
                        if (random.nextBoolean()) {
                            dstWidth = newGrownSpan(random, srcWidth);
                            dstHeight = newShrinkedSpan(random, srcHeight);
                        } else {
                            dstWidth = newShrinkedSpan(random, srcWidth);
                            dstHeight = newGrownSpan(random, srcHeight);
                        }
                    } else if (opType == MyOpType.POWERS_OF_TWO) {
                        // Covering all x2/x4 cases with rkm4 in 0..3
                        // (x4 makes test slow, so better not do it
                        // over and over).
                        final int rkm4 = rk % 4;
                        dstWidth = (srcWidth << (1 << (rkm4 / 2)));
                        dstHeight = (srcHeight << (1 << (rkm4 % 2)));
                    } else {
                        throw new AssertionError();
                    }
                    
                    if (DEBUG) {
                        System.out.println();
                        System.out.println("opType = " + opType);
                        System.out.println("cmpType = " + cmpType);
                        System.out.println("prlTrueElseFake = " + prlTrueElseFake);
                        System.out.println("parallelizer = " + parallelizer);
                        System.out.println("drawer = " + drawer);
                        System.out.println("refDrawer = " + refDrawer);
                        System.out.println("imageTypeEnum = " + imageTypeEnum);
                        System.out.println("srcImage = " + toStringInfo(srcImage));
                        System.out.println("dstWidth = " + dstWidth);
                        System.out.println("dstHeight = " + dstHeight);
                    }
                    
                    if ((cmpType == MyCmpType.WITH_REF)
                        && (refDrawer == null)) {
                        // N/A
                        continue;
                    }
                    
                    test_yyy(
                        opType,
                        cmpType,
                        prlTrueElseFake,
                        parallelizer,
                        //
                        drawer,
                        refDrawer,
                        imageTypeEnum,
                        srcImage,
                        dstWidth,
                        dstHeight);
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void tearDown() {
        if (MAX_DELTA_COMPUTATION_MODE) {
            /*
             * Use the last log, after all tests ran.
             */
            if (MAX_DELTA_BY_CASE.size() != 0) {
                System.out.println();
                System.out.println("MAX_DELTA_BY_CASE:");
                for (Map.Entry<String,Integer> e : MAX_DELTA_BY_CASE.entrySet()) {
                    final String deltaCase = e.getKey();
                    final int maxDelta = e.getValue();
                    System.out.println("m.put(\"" + deltaCase + "\", " + maxDelta + ");");
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void test_yyy(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake,
        InterfaceParallelizer parallelizer,
        //
        InterfaceScaledRectDrawer drawer,
        InterfaceScaledRectDrawer refDrawer,
        TestImageTypeEnum imageTypeEnum,
        final BufferedImage srcImage,
        int dstWidth,
        int dstHeight) {
        
        final int srcWidth = srcImage.getWidth();
        final int srcHeight = srcImage.getHeight();
        
        final String deltaCase = computeDeltaCase(
            opType,
            cmpType,
            prlTrueElseFake,
            imageTypeEnum);
        
        // If null, means must compare with back.
        final InterfaceScaledRectDrawer cmpDrawer;
        if (cmpType == MyCmpType.WITH_REF) {
            cmpDrawer = LangUtils.requireNonNull(refDrawer);
        } else if (cmpType == MyCmpType.WITH_BACK) {
            cmpDrawer = null;
        } else {
            throw new IllegalArgumentException("" + cmpType);
        }
        
        /*
         * Resizing.
         */
        
        final BufferedPixels srcPixels =
            new BufferedPixels(srcImage);
        
        final BufferedImage dstImage =
            BihTestUtils.newImage(
                dstWidth,
                dstHeight,
                imageTypeEnum);
        final BufferedPixels dstPixels =
            new BufferedPixels(dstImage);
        final BufferedImage cmpImage =
            (cmpDrawer == null) ? null
                : BihTestUtils.newImage(
                    dstWidth,
                    dstHeight,
                    imageTypeEnum);
        final BufferedImageHelper cmpHelper =
            (cmpDrawer == null) ? null
                : new BufferedImageHelper(cmpImage);
        final BufferedPixels cmpPixels =
            (cmpDrawer == null) ? null
                : new BufferedPixels(cmpImage);
        /*
         * No need to randomize dstPixels
         * since it uses use Copy/Src drawer.
         */
        if (cmpDrawer != null) {
            final BufferedImageHelper dstHelper =
                new BufferedImageHelper(dstImage);
            SrdTestUtils.copyImage_reference(
                dstHelper,
                cmpHelper);
        }
        //
        final InterfaceColorTypeHelper colorTypeHelper =
            SrdTestUtils.getColorTypeHelper(imageTypeEnum);
        
        if (DEBUG) {
            System.out.println();
            System.out.println("drawer.drawScaledRect("
                + parallelizer
                + ","
                + colorTypeHelper
                + ","
                + toStringInfo(srcImage)
                + ","
                + toStringInfo(dstImage)
                + ")");
        }
        drawer.drawScaledRect(
            parallelizer,
            colorTypeHelper,
            //
            srcPixels,
            srcPixels.getRect(),
            //
            dstPixels.getRect(),
            dstPixels.getRect(),
            dstPixels);
        
        /*
         * 
         */
        
        final BufferedImage expImage;
        final BufferedImage actImage;
        if (cmpType == MyCmpType.WITH_REF) {
            if (DEBUG) {
                System.out.println();
                System.out.println("drawer.drawScaledRect("
                    + parallelizer
                    + ","
                    + colorTypeHelper
                    + ","
                    + toStringInfo(srcImage)
                    + ","
                    + toStringInfo(cmpImage)
                    + ")");
            }
            drawer.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                srcPixels,
                srcPixels.getRect(),
                //
                cmpPixels.getRect(),
                cmpPixels.getRect(),
                cmpPixels);
            expImage = cmpImage;
            actImage = dstImage;
        } else if (cmpType == MyCmpType.WITH_BACK) {
            /*
             * Resizing back.
             */
            
            final int backScanlineStride = srcWidth + 1;
            final BufferedImage backImage =
                BihTestUtils.newImage(
                    backScanlineStride,
                    srcWidth,
                    srcHeight,
                    imageTypeEnum);
            final BufferedPixels backPixels =
                new BufferedPixels(backImage);
            /*
             * No need to randomize backPixels
             * since it uses Copy/Src drawer.
             */
            if (DEBUG) {
                System.out.println();
                System.out.println("(back) drawer.drawScaledRect("
                    + parallelizer
                    + ","
                    + colorTypeHelper
                    + ","
                    + toStringInfo(dstImage)
                    + ","
                    + toStringInfo(backImage)
                    + ")");
            }
            drawer.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                dstPixels,
                dstPixels.getRect(),
                //
                backPixels.getRect(),
                backPixels.getRect(),
                backPixels);
            
            expImage = srcImage;
            actImage = backImage;
        } else {
            throw new IllegalArgumentException("" + cmpType);
        }
        
        /*
         * 
         */
        
        final int checkNeighborDelta =
            getCheckNeighborDelta(
                opType,
                cmpType,
                prlTrueElseFake);
        
        /*
         * 
         */
        
        final BufferedImageHelper expHelper = new BufferedImageHelper(expImage);
        final BufferedImageHelper actHelper = new BufferedImageHelper(actImage);
        
        final int maxCptDelta =
            SrdTestUtils.computeMaxCptDelta(
                expHelper,
                actHelper,
                checkNeighborDelta);
        
        /*
         * 
         */
        
        // If null, means no constraint.
        final Integer cptDeltaTolRef =
            (MAX_DELTA_COMPUTATION_MODE
                ? MAX_COMPUTED_DELTA : MAX_DELTA_BY_CASE.get(deltaCase));
        
        if ((cptDeltaTolRef != null)
            && (maxCptDelta > cptDeltaTolRef)) {
            final int cptDeltaTol = cptDeltaTolRef;
            System.out.println();
            if (MUST_PRINT_IMAGE_ON_ERROR) {
                if (cmpType == MyCmpType.WITH_BACK) {
                    final BufferedImageHelper dstHelper =
                        new BufferedImageHelper(dstImage);
                    BihTestUtils.printImage("dstHelper", dstHelper);
                    BihTestUtils.printImageDiff(
                        "expHelper(src)",
                        expHelper,
                        "actHelper(back)",
                        actHelper);
                } else {
                    final BufferedImageHelper srcHelper =
                        new BufferedImageHelper(srcImage);
                    BihTestUtils.printImage("srcHelper", srcHelper);
                    BihTestUtils.printImageDiff(
                        "expHelper(cmp)",
                        expHelper,
                        "actHelper(dst)",
                        actHelper);
                }
            }
            System.out.println("opType = " + opType);
            System.out.println("cmpType = " + cmpType);
            System.out.println("prlTrueElseFake = " + prlTrueElseFake);
            System.out.println("deltaCase = " + deltaCase);
            System.out.println("drawer = " + drawer);
            System.out.println("refDrawer = " + refDrawer);
            System.out.println("cmpDrawer = " + cmpDrawer);
            System.out.println("imageTypeEnum = " + imageTypeEnum);
            System.out.println("srcImage = " + toStringInfo(srcImage));
            System.out.println("dstImage = " + toStringInfo(dstImage));
            System.out.println("cptDeltaTol = " + cptDeltaTol
                + " = 0x" + Integer.toHexString(cptDeltaTol));
            System.out.println("maxCptDelta = " + maxCptDelta
                + " = 0x" + Integer.toHexString(maxCptDelta));
            fail("not good enough");
        }
        
        /*
         * Only computing delta for sequential and fake parallel cases,
         * not to let delta due to concurrency issue slip into
         * tolerated deltas.
         * Fake parallel case can have different delta than sequential case,
         * but only in case of drawImage() usage, for it gets less accurate
         * in case of clipping.
         */
        if (MAX_DELTA_COMPUTATION_MODE
            && ((prlTrueElseFake == null)
                || (!prlTrueElseFake))) {
            Integer prevRef = MAX_DELTA_BY_CASE.get(deltaCase);
            if ((prevRef == null)
                || (maxCptDelta > prevRef.intValue())) {
                MAX_DELTA_BY_CASE.put(deltaCase, maxCptDelta);
            }
        }
    }
    
    /*
     * 
     */
    
    private static String toStringInfo(BufferedImage image) {
        return new BufferedImageHelper(image, false, false).toString();
    }
    
    /*
     * 
     */
    
    private static int newGrownSpan(Random random, int span) {
        final double growth = SrdTestUtils.randomMinMax(random, MIN_GROWTH, MAX_GROWTH);
        return (int) Math.rint(span * growth);
    }
    
    private static int newShrinkedSpan(Random random, int span) {
        final double shrinking = SrdTestUtils.randomMinMax(random, MIN_GROWTH, MAX_GROWTH);
        return Math.max(1, (int) Math.rint(span / shrinking));
    }
}
