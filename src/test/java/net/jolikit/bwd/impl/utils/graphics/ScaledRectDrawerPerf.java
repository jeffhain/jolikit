/*
 * Copyright 2021-2024 Jeff Hain
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
import java.util.List;
import java.util.Random;

import net.jolikit.bwd.api.graphics.Argb32;
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
    
    /**
     * To tune area thresholds for parallelization splits.
     */
    private static final boolean SPLIT_TUNING_MODE = false;

    /**
     * For quick bench of our code for basic cases.
     */
    private static final boolean BASIC_BENCH_MODE = false || SPLIT_TUNING_MODE;

    /*
     * 
     */
    
    private static final boolean MUST_BENCH_NEAREST_AWT = (!BASIC_BENCH_MODE);
    private static final boolean MUST_BENCH_NEAREST = true;
    private static final boolean MUST_BENCH_BOXSAMPLED = true;
    private static final boolean MUST_BENCH_BILINEAR_AWT = (!BASIC_BENCH_MODE);
    private static final boolean MUST_BENCH_BILINEAR = true;
    private static final boolean MUST_BENCH_BOXSAMPLED_BILINEAR = true;
    private static final boolean MUST_BENCH_BICUBIC_AWT = (!BASIC_BENCH_MODE);
    private static final boolean MUST_BENCH_BICUBIC = true;
    private static final boolean MUST_BENCH_BOXSAMPLED_BICUBIC = true;
    
    /*
     * 
     */
    
    private static final boolean MUST_BENCH_SEQUENTIAL = (!BASIC_BENCH_MODE);

    private static final boolean MUST_BENCH_NON_PREMUL = (!BASIC_BENCH_MODE);

    private static final boolean MUST_BENCH_TRANSPARENT = (!BASIC_BENCH_MODE);

    private static final boolean MUST_BENCH_REPEATING_COLORS = (!BASIC_BENCH_MODE);
    
    /*
     * 
     */
    
    /**
     * 3 for 1 load/warmup run + 2 meaningful runs.
     */
    private static final int NBR_OF_RUNS = 3;
    
    private static final int NBR_OF_CALLS = (SPLIT_TUNING_MODE || MUST_BENCH_SEQUENTIAL ? 10 : 100);
    
    private static final int SPAN_FACTOR = (SPLIT_TUNING_MODE ? 4 : 1);
    
    private static final int PARALLELISM =
        Runtime.getRuntime().availableProcessors();
    
    /*
     * 
     */
    
    private static final int[][] SRC_DST_SPANS_ARR = new int[][] {
        {1000, 101},
        {1000, 500},
        {1000, 505},
        {1000, 1000},
        {505, 1000},
        {500, 1000},
        {101, 1000},
    };
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyPixels
    extends IntArrSrcPixels
    implements InterfaceRowDrawer {
        public MyPixels() {
        }
        public void reset(GRect rect) {
            this.configure(
                rect,
                new int[rect.area()],
                rect.xSpan());
        }
        /**
         * Low newColorProba allows to bench regularity optimizations.
         * 
         * @param newColorProba Probability for each pixel to be
         *        a new random value, vs the same as the previous one
         *        (pixels being filled row by row).
         */
        public void randomize(
            Random random,
            InterfaceColorTypeHelper colorTypeHelper,
            int minAlpha8,
            double newColorProba) {
            
            final GRect rect = this.getRect();
            final int w = rect.xSpan();
            final int h = rect.ySpan();
            int prevNonPremulColor32 = randomNonPremulArgb32(random, minAlpha8);
            for (int j = 0; j < h; j++) {
                final int y = rect.y() + j;
                for (int i = 0; i < w; i++) {
                    final int x = rect.x() + i;
                    final int nonPremulColor32;
                    if (random.nextDouble() < newColorProba) {
                        nonPremulColor32 = randomNonPremulArgb32(random, minAlpha8);
                        prevNonPremulColor32 = nonPremulColor32;
                    } else {
                        nonPremulColor32 = prevNonPremulColor32;
                    }
                    final int color32 =
                        colorTypeHelper.asTypeFromNonPremul32(
                            nonPremulColor32);
                    super.setColor32At(x, y, color32);
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
        final long a = System.nanoTime();
        System.out.println("--- " + ScaledRectDrawerPerf.class.getSimpleName() + "... ---");
        System.out.println("AREA_THRESHOLD_FOR_SPLIT (NEAREST) = "
            + ScaledRectAlgoNearest.AREA_THRESHOLD_FOR_SPLIT);
        System.out.println("AREA_THRESHOLD_FOR_SPLIT (BOXSAMPLED) = "
            + ScaledRectAlgoBoxsampled.AREA_THRESHOLD_FOR_SPLIT);
        System.out.println("AREA_THRESHOLD_FOR_SPLIT (BILINEAR) = "
            + ScaledRectAlgoBilinear.AREA_THRESHOLD_FOR_SPLIT);
        System.out.println("AREA_THRESHOLD_FOR_SPLIT (BICUBIC) = "
            + ScaledRectAlgoBicubic.AREA_THRESHOLD_FOR_SPLIT);
        
        for (int[] srcDstSpans : SRC_DST_SPANS_ARR) {
            System.out.println();
            final int srcSpans = SPAN_FACTOR * srcDstSpans[0];
            final int dstSpans = SPAN_FACTOR * srcDstSpans[1];
            final int nbrOfCalls = NBR_OF_CALLS;
            bench_drawRectScaled(srcSpans, dstSpans, nbrOfCalls);
        }

        final long b = System.nanoTime();
        System.out.println("--- ..." + ScaledRectDrawerPerf.class.getSimpleName()
            + ", " + TestUtils.nsToSRounded(b-a) + " s ---");
    }
    
    /**
     * Works because we only use ARGB in these tests.
     */
    private static int randomNonPremulArgb32(
        Random random,
        int minAlpha8) {
        final int alpha8 = minAlpha8 + random.nextInt(0xFF - minAlpha8 + 1);
        return Argb32.withAlpha8(random.nextInt(), alpha8);
    }
    
    private void bench_drawRectScaled(
        int srcSpans,
        int dstSpans,
        int nbrOfCalls) {
        
        System.out.println(
            "scale = " + (dstSpans / (double) srcSpans));
        
        final Random random = TestUtils.newRandom123456789L();
        // To have exactly the same input
        // for each drawer, when parameters are the same.
        final long seedBeforeRandomize = random.nextLong();
        
        final MyPixels input = new MyPixels();
        final GRect srcRect = GRect.valueOf(0, 0, srcSpans, srcSpans);
        input.reset(srcRect);

        final MyPixels output = new MyPixels();
        final GRect dstRect = GRect.valueOf(0, 0, dstSpans, dstSpans);
        output.reset(dstRect);

        final GRect dstClip = dstRect;
        
        final List<InterfaceScaledRectDrawer> drawerList = new ArrayList<>();
        if (MUST_BENCH_NEAREST_AWT) {
            drawerList.add(new ScaledRectDrawerNearestAwt() {
                @Override
                public String toString() {
                    return "NEAREST_AWT";
                }
            });
        }
        if (MUST_BENCH_NEAREST) {
            drawerList.add(new ScaledRectDrawerNearest() {
                @Override
                public String toString() {
                    return "NEAREST";
                }
            });
        }
        if (MUST_BENCH_BOXSAMPLED) {
            drawerList.add(new ScaledRectDrawerBoxsampled() {
                @Override
                public String toString() {
                    return "BOXSAMPLED";
                }
            });
        }
        if (MUST_BENCH_BILINEAR_AWT) {
            drawerList.add(new ScaledRectDrawerBilinearAwt() {
                @Override
                public String toString() {
                    return "BILINEAR_AWT";
                }
            });
        }
        if (MUST_BENCH_BILINEAR) {
            drawerList.add(new ScaledRectDrawerBilinear() {
                @Override
                public String toString() {
                    return "BILINEAR";
                }
            });
        }
        if (MUST_BENCH_BOXSAMPLED_BILINEAR) {
            drawerList.add(new ScaledRectDrawerBoxsampledBilinear() {
                @Override
                public String toString() {
                    return "BOXSAMPLED_BILINEAR";
                }
            });
        }
        if (MUST_BENCH_BICUBIC_AWT) {
            drawerList.add(new ScaledRectDrawerBicubicAwt() {
                @Override
                public String toString() {
                    return "BICUBIC_AWT";
                }
            });
        }
        if (MUST_BENCH_BICUBIC) {
            drawerList.add(new ScaledRectDrawerBicubic() {
                @Override
                public String toString() {
                    return "BICUBIC";
                }
            });
        }
        if (MUST_BENCH_BOXSAMPLED_BICUBIC) {
            drawerList.add(new ScaledRectDrawerBoxsampledBicubic() {
                @Override
                public String toString() {
                    return "BOXSAMPLED_BICUBIC";
                }
            });
        }
        
        for (boolean prlElseSeq : new boolean[] {false,true}) {
            if ((!MUST_BENCH_SEQUENTIAL)
                && (!prlElseSeq)) {
                continue;
            }
            final InterfaceParallelizer parallelizer;
            if (prlElseSeq) {
                if (PARALLELISM <= 1) {
                    continue;
                }
                System.out.println();
                parallelizer = PRLZR;
            } else {
                parallelizer = SequentialParallelizer.getDefault();
            }
            
            for (InterfaceScaledRectDrawer drawer : drawerList) {
                
                for (boolean premul : new boolean[] {false,true}) {
                    if ((!MUST_BENCH_NON_PREMUL)
                        && (!premul)) {
                        continue;
                    }
                    final InterfaceColorTypeHelper colorTypeHelper =
                        ScaledRectTestUtils.getColorTypeHelper(premul);
                    
                    for (boolean opaque : new boolean[] {false,true}) {
                        if ((!MUST_BENCH_TRANSPARENT)
                            && (!opaque)) {
                            continue;
                        }
                        final int minAlpha8 = (opaque ? 0xFF : 0x00);
                        
                        // Slower first, faster last (the usual benching order).
                        for (double newColorProba : new double[] {1.0, 0.1}) {
                            if ((!MUST_BENCH_REPEATING_COLORS)
                                && (newColorProba < 1.0)) {
                                continue;
                            }
                            random.setSeed(seedBeforeRandomize);
                            input.randomize(
                                random,
                                colorTypeHelper,
                                minAlpha8,
                                newColorProba);

                            for (int k = 0; k < NBR_OF_RUNS; k++) {
                                final long a = System.nanoTime();
                                for (int i = 0; i < nbrOfCalls; i++) {
                                    drawer.drawScaledRect(
                                        parallelizer,
                                        colorTypeHelper,
                                        input,
                                        srcRect,
                                        dstRect,
                                        dstClip,
                                        output);
                                }
                                final long b = System.nanoTime();
                                final String premulStr =
                                    (premul ? "(premul)" : "(non-pr)");
                                final String opaqueStr =
                                    (opaque ? "(op)" : "(tr)");
                                final String pixChgProbaStr =
                                    (newColorProba < 1.0 ? "(reg)" : "(rnd)");
                                System.out.println(nbrOfCalls + " calls"
                                    + ", spans (" + srcSpans + " -> " + dstSpans + ")"
                                    + ", prl = " + parallelizer.getParallelism()
                                    + ", " + drawer
                                    + " " + premulStr + opaqueStr + pixChgProbaStr
                                    + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                            }
                        }
                    }
                }
            }
        }
    }
}
