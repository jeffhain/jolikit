/*
 * Copyright 2021 Jeff Hain
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

import java.util.Random;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.BindingPrlUtils;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;

public class ScaledRectDrawerPerf {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_RUNS = 4;
    
    /**
     * Enough to have a good gain, not more because CPUs
     * with more core are still not that common.
     */
    private static final int PARALLELISM = 4;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyPixels
    extends IntArrSrcPixels
    implements InterfaceRowDrawer {
        public MyPixels() {
        }
        public void reset(
            int width,
            int height) {
            this.configure(
                width,
                height,
                new int[width * height],
                width);
        }
        /**
         * When not using a random value, using a fixed one,
         * which allows to bench regularity optimizations.
         * 
         * @param randomProba Probability to use a random value.
         */
        public void randomize(Random random, double randomProba) {
            final int width = this.getWidth();
            final int height = this.getHeight();
            final int defaultColor32 = random.nextInt();
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    final int color32;
                    if (random.nextDouble() < randomProba) {
                        color32 = random.nextInt();
                    } else {
                        color32 = defaultColor32;
                    }
                    this.setColor32At(i, j, color32);
                }
            }
        }
        @Override
        public void drawRow(
            int[] rowArr,
            int rowOffset,
            int dstX,
            int dstY,
            int length) {
            
            /*
             * Not actually drawing (not wanting to bench that),
             * just using input.
             */
            
            int sum = 0;
            for (int i = 0; i < length; i++) {
                final int color32 = rowArr[rowOffset + i];
                sum += color32;
            }
            TestUtils.blackHole(sum);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final InterfaceParallelizer PRLZR =
        BindingPrlUtils.newParallelizer(
            new BaseBwdBindingConfig(),
            PARALLELISM,
            ScaledRectDrawerPerf.class.getClass().getSimpleName());

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        newRun(args);
    }

    public static void newRun(String[] args) {
        new ScaledRectDrawerPerf().run(args);
    }
    
    public ScaledRectDrawerPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void run(String[] args) {
        System.out.println("--- " + ScaledRectDrawerPerf.class.getSimpleName() + "... ---");
        System.out.println("MIN_AREA_COST_FOR_SPLIT_CLOSEST = " + ScaledRectDrawer.MIN_AREA_COST_FOR_SPLIT_CLOSEST);
        System.out.println("MIN_AREA_COST_FOR_SPLIT_SMOOTH = " + ScaledRectDrawer.MIN_AREA_COST_FOR_SPLIT_SMOOTH);
        
        /*
         * scale = 1
         */
        
        for (int[] argArr : new int[][]{
            {1000, 1000, 10},
        }) {
            System.out.println();
            final int srcSpans = argArr[0];
            final int dstSpans = argArr[1];
            final int nbrOfCalls = argArr[2];
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }

        /*
         * scale = 2
         */
        
        for (int[] argArr : new int[][]{
            {500, 1000, 10},
        }) {
            System.out.println();
            final int srcSpans = argArr[0];
            final int dstSpans = argArr[1];
            final int nbrOfCalls = argArr[2];
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }

        /*
         * scale = 0.5
         */
        
        for (int[] argArr : new int[][]{
            {2000, 1000, 10},
        }) {
            System.out.println();
            final int srcSpans = argArr[0];
            final int dstSpans = argArr[1];
            final int nbrOfCalls = argArr[2];
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }

        /*
         * scale = 1.5
         */
        
        for (int[] argArr : new int[][]{
            {667, 1000, 10},
        }) {
            System.out.println();
            final int srcSpans = argArr[0];
            final int dstSpans = argArr[1];
            final int nbrOfCalls = argArr[2];
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }

        /*
         * scale = 1/1.5
         */
        
        for (int[] argArr : new int[][]{
            {1500, 1000, 10},
        }) {
            System.out.println();
            final int srcSpans = argArr[0];
            final int dstSpans = argArr[1];
            final int nbrOfCalls = argArr[2];
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }

        /*
         * scale = 3.333
         */
        
        for (int[] argArr : new int[][]{
            {300, 1000, 10},
        }) {
            System.out.println();
            final int srcSpans = argArr[0];
            final int dstSpans = argArr[1];
            final int nbrOfCalls = argArr[2];
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }

        /*
         * scale = 1/3.1
         */
        
        for (int[] argArr : new int[][]{
            {31, 10, 100000},
            {310, 100, 1000},
            {3100, 1000, 10},
        }) {
            System.out.println();
            final int srcSpans = argArr[0];
            final int dstSpans = argArr[1];
            final int nbrOfCalls = argArr[2];
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }
        
        System.out.println("--- ..." + ScaledRectDrawerPerf.class.getSimpleName() + " ---");
    }
    
    private void bench_drawRectScaled(
        int srcSpans,
        int dstSpans,
        int nbrOfCalls) {
        
        System.out.println(
            "scale = " + (dstSpans / (double) srcSpans));
        
        final Random random = TestUtils.newRandom123456789L();
        
        final MyPixels input = new MyPixels();
        input.reset(srcSpans, srcSpans);

        final MyPixels output = new MyPixels();
        output.reset(dstSpans, dstSpans);

        final GRect srcRect = GRect.valueOf(
            0, 0, srcSpans, srcSpans);
        final GRect dstRect = GRect.valueOf(
            0, 0, dstSpans, dstSpans);
        
        for (boolean smoothElseClosest : new boolean[] {false,true}) {
            // Pixels values don't matter for closest algorithm.
            final double[] randomProbaArr =
                (smoothElseClosest ? new double[] {1.0, 0.1, 0.0} : new double[] {1.0});
            for (double randomProba : randomProbaArr) {
                input.randomize(random, randomProba);
                
                for (boolean prlElseSeq : new boolean[] {false,true}) {
                    final InterfaceParallelizer parallelizer;
                    if (prlElseSeq) {
                        parallelizer = PRLZR;
                    } else {
                        parallelizer = SequentialParallelizer.getDefault();
                    }
                    for (int k = 0; k < NBR_OF_RUNS; k++) {
                        final long a = System.nanoTime();
                        for (int i = 0; i < nbrOfCalls; i++) {
                            callDrawRectScaled(
                                parallelizer,
                                smoothElseClosest,
                                input,
                                srcRect,
                                dstRect,
                                output);
                        }
                        final long b = System.nanoTime();
                        System.out.println(nbrOfCalls + " calls"
                            + ", spans (" + srcSpans + " -> " + dstSpans + ")"
                            + ", " + (smoothElseClosest ? "smooth" : "closest")
                            + ", prl = " + parallelizer.getParallelism()
                            + (smoothElseClosest ? ", rndProb = " + randomProba : "") 
                            + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static void callDrawRectScaled(
        InterfaceParallelizer parallelizer,
        boolean mustUseSmoothElseClosest,
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        GRect dstRect,
        InterfaceRowDrawer rowDrawer) {
        
        final GRect dstClip = dstRect;
        ScaledRectDrawer.drawRectScaled(
            parallelizer,
            mustUseSmoothElseClosest,
            srcPixels,
            srcRect,
            dstRect,
            dstClip,
            rowDrawer);
    }
}
