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
package net.jolikit.bwd.impl.utils.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;

public class ScaledRectDrawerWithAlgoTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyAlgo extends AbstractScaledRectAlgo {

        int srcAreaThresholdForSplit;
        int dstAreaThresholdForSplit;
        double iterationSpanGrowthFactor;
        double iterationSpanShrinkFactor;
        final List<GRect> srcRectList = new ArrayList<>();
        final List<GRect> dstRectList = new ArrayList<>();
        final List<GRect> dstRectClippedList = new ArrayList<>();
        
        public MyAlgo() {
            this.clear();
        }
        
        public final void clear() {
            if (DEBUG) {
                System.out.println("clear()");
            }
            // By default, no parallelism.
            this.srcAreaThresholdForSplit = Integer.MAX_VALUE;
            this.dstAreaThresholdForSplit = Integer.MAX_VALUE;
            //
            this.iterationSpanGrowthFactor = Double.POSITIVE_INFINITY;
            this.iterationSpanShrinkFactor = 0.0;
            this.srcRectList.clear();
            this.dstRectList.clear();
            this.dstRectClippedList.clear();
        }
        
        @Override
        public int getSrcAreaThresholdForSplit() {
            return this.srcAreaThresholdForSplit;
        }
        
        @Override
        public int getDstAreaThresholdForSplit() {
            return this.dstAreaThresholdForSplit;
        }
        
        @Override
        public double getIterationSpanGrowthFactor() {
            return this.iterationSpanGrowthFactor;
        }

        @Override
        public double getIterationSpanShrinkFactor() {
            return this.iterationSpanShrinkFactor;
        }

        @Override
        public void drawScaledRectChunk(
            InterfaceColorTypeHelper colorTypeHelper,
            InterfaceSrcPixels srcPixels,
            GRect srcRect,
            GRect dstRect,
            GRect dstRectClipped,
            int dstYStart,
            int dstYEnd,
            InterfaceRowDrawer dstRowDrawer) {
            if (DEBUG) {
                System.out.println("srcRect = " + srcRect);
                System.out.println("dstRect = " + dstRect);
                System.out.println("dstRectClipped = " + dstRectClipped);
            }
            this.srcRectList.add(srcRect);
            this.dstRectList.add(dstRect);
            this.dstRectClippedList.add(dstRectClipped);
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerWithAlgoTest() {
    }
    
    public void test_iterationsSpansFactorsBounds() {
        final MyAlgo algo = new MyAlgo();
        final ScaledRectDrawerWithAlgo drawer =
            new ScaledRectDrawerWithAlgo(algo);
        
        final InterfaceParallelizer parallelizer = new SequentialParallelizer();
        final InterfaceColorTypeHelper colorTypeHelper = PremulArgbHelper.getInstance();
        final IntArrSrcPixels srcPixels = new IntArrSrcPixels();
        final IntArrCopyRowDrawer dstRowDrawer = new IntArrCopyRowDrawer();
        
        final GRect srcRect = GRect.valueOf(0, 0, 1024, 1024);
        srcPixels.setRect(srcRect);
        final GRect dstRect = srcRect;
        final GRect dstClip = dstRect;
        
        for (double badGrowthFactor : new double[] {
            Double.NEGATIVE_INFINITY,
            -Double.MAX_VALUE,
            -1.0,
            0.0,
            0.9,
            1.0}) {
            
            algo.clear();
            algo.iterationSpanGrowthFactor = badGrowthFactor;
            
            try {
                drawer.drawScaledRect(
                    parallelizer,
                    colorTypeHelper,
                    srcPixels,
                    srcRect,
                    dstRect,
                    dstClip,
                    dstRowDrawer);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
        
        for (double goodGrowthFactor : new double[] {
            1.1,
            Double.MAX_VALUE,
            Double.POSITIVE_INFINITY}) {
            
            algo.clear();
            algo.iterationSpanGrowthFactor = goodGrowthFactor;
            
            drawer.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                srcPixels,
                srcRect,
                dstRect,
                dstClip,
                dstRowDrawer);
        }
        
        for (double badShrinkFactor : new double[] {
            Double.NEGATIVE_INFINITY,
            -Double.MAX_VALUE,
            -1.0,
            -0.1,
            1.0,
            1.1,
            Double.MAX_VALUE,
            Double.POSITIVE_INFINITY}) {
            
            algo.clear();
            algo.iterationSpanShrinkFactor = badShrinkFactor;
            
            try {
                drawer.drawScaledRect(
                    parallelizer,
                    colorTypeHelper,
                    srcPixels,
                    srcRect,
                    dstRect,
                    dstClip,
                    dstRowDrawer);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
        
        for (double goodShrinkFactor : new double[] {
            -0.0,
            0.0,
            0.9}) {
            
            algo.clear();
            algo.iterationSpanShrinkFactor = goodShrinkFactor;
            
            drawer.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                srcPixels,
                srcRect,
                dstRect,
                dstClip,
                dstRowDrawer);
        }
    }
    
    /*
     * 
     */
    
    public void test_iterationsRects_exactValues() {
        test_iterationsRects_xxx(
            2.0,
            0.25,
            GRect.valueOf(0, 0, 200, 1024),
            Arrays.asList(
                GRect.valueOf(0, 0, 400, 256),
                GRect.valueOf(0, 0, 800, 200)),
            GRect.valueOf(0, 0, 1024, 200));
    }
    
    public void test_iterationsRects_boundingFactors() {
        test_iterationsRects_xxx(
            Math.nextAfter(2.0, Double.NEGATIVE_INFINITY),
            Math.nextAfter(0.25, Double.POSITIVE_INFINITY),
            GRect.valueOf(0, 0, 200, 1024),
            Arrays.asList(
                GRect.valueOf(0, 0, 399, 257),
                GRect.valueOf(0, 0, 797, 200)),
            GRect.valueOf(0, 0, 1024, 200));
    }
    
    public void test_iterationsRects_forcedMoves() {
        test_iterationsRects_xxx(
            Math.nextAfter(1.0, Double.POSITIVE_INFINITY),
            Math.nextAfter(1.0, Double.NEGATIVE_INFINITY),
            GRect.valueOf(0, 0, 200, 1024),
            Arrays.asList(
                GRect.valueOf(0, 0, 201, 1023),
                GRect.valueOf(0, 0, 202, 1023)),
            GRect.valueOf(0, 0, 203, 1023));
    }
    
    public void test_iterationsRects_forcedMovesMinBound() {
        test_iterationsRects_xxx(
            Math.nextAfter(1.0, Double.POSITIVE_INFINITY),
            Math.nextAfter(1.0, Double.NEGATIVE_INFINITY),
            GRect.valueOf(0, 0, 10, 2),
            Arrays.asList(
                GRect.valueOf(0, 0, 11, 1)),
            GRect.valueOf(0, 0, 12, 1));
    }
    
    public void test_iterationsRects_forcedMovesMaxBound() {
        /*
         * Can't test Integer.MAX_VALUE bound through iterations
         * due to array length JVM limit.
         */
        
        final double factor = Math.nextAfter(1.0, Double.POSITIVE_INFINITY);
        final int dstRectSpan = Integer.MAX_VALUE;
        
        final int expected = Integer.MAX_VALUE;
        
        for (final int prevItDstRectSpan : new int[] {
            Integer.MAX_VALUE - 1,
            Integer.MAX_VALUE}) {
            
            final int actual = ScaledRectDrawerWithAlgo.computeNextItDstRectSpan(
                prevItDstRectSpan,
                factor,
                dstRectSpan);
            
            assertEquals(expected, actual);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void test_iterationsRects_xxx(
        double iterationSpanGrowthFactor,
        double iterationSpanShrinkFactor,
        GRect srcRect,
        List<GRect> expectedInterRectList,
        GRect dstRect) {
        
        final MyAlgo algo = new MyAlgo();
        final ScaledRectDrawerWithAlgo drawer =
            new ScaledRectDrawerWithAlgo(algo);
        
        final InterfaceParallelizer parallelizer = new SequentialParallelizer();
        final InterfaceColorTypeHelper colorTypeHelper = PremulArgbHelper.getInstance();
        final IntArrSrcPixels srcPixels = new IntArrSrcPixels();
        final IntArrCopyRowDrawer dstRowDrawer = new IntArrCopyRowDrawer();
        
        /*
         * 
         */
        
        srcPixels.setRect(srcRect);
        
        algo.iterationSpanGrowthFactor = iterationSpanGrowthFactor;
        algo.iterationSpanShrinkFactor = iterationSpanShrinkFactor;
        GRect dstClip = dstRect.withSpans(1, 1);
        
        drawer.drawScaledRect(
            parallelizer,
            colorTypeHelper,
            srcPixels,
            srcRect,
            dstRect,
            dstClip,
            dstRowDrawer);
        
        final int expInterRectCount = expectedInterRectList.size();
        final int expItCount = expInterRectCount + 1;
        
        assertEquals(expItCount, algo.srcRectList.size());
        //
        assertEquals(srcRect, algo.srcRectList.get(0));
        for (int i = 0; i < expInterRectCount; i++) {
            final GRect expInterRectK = expectedInterRectList.get(i);
            //
            assertEquals(expInterRectK, algo.dstRectList.get(i));
            assertEquals(expInterRectK, algo.dstRectClippedList.get(i));
            //
            assertEquals(expInterRectK, algo.srcRectList.get(i + 1));
        }
        assertEquals(dstRect, algo.dstRectList.get(expItCount - 1));
        assertEquals(dstRect.intersected(dstClip), algo.dstRectClippedList.get(expItCount - 1));
    }
}
