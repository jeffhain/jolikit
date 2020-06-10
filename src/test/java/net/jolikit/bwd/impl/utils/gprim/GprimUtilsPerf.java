/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.utils.gprim;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.test.utils.TestUtils;

public class GprimUtilsPerf {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int NBR_OF_RUNS = 4;

    private static final int NBR_OF_CALLS = 1000 * 1000;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        newRun(args);
    }

    public static void newRun(String[] args) {
        new GprimUtilsPerf().run(args);
    }
    
    public GprimUtilsPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void run(String[] args) {
        System.out.println("--- " + GprimUtilsPerf.class.getSimpleName() + "... ---");
        
        bench_mustXxxOvalOrArc_draw_belowThreshold();

        bench_mustXxxOvalOrArc_draw_aboveThreshold();
        
        bench_mustXxxOvalOrArc_fill_belowThreshold();
        
        bench_mustXxxOvalOrArc_fill_aboveThreshold();
        
        System.out.println("--- ..." + GprimUtilsPerf.class.getSimpleName() + " ---");
    }
    
    /*
     * 
     */

    private static void bench_mustXxxOvalOrArc_draw_belowThreshold() {
        bench_mustXxxOvalOrArc(false, false);
    }

    private static void bench_mustXxxOvalOrArc_draw_aboveThreshold() {
        bench_mustXxxOvalOrArc(false, true);
    }
    
    private static void bench_mustXxxOvalOrArc_fill_belowThreshold() {
        bench_mustXxxOvalOrArc(true, false);
    }

    private static void bench_mustXxxOvalOrArc_fill_aboveThreshold() {
        bench_mustXxxOvalOrArc(true, true);
    }
    
    /*
     * 
     */
    
    private static void bench_mustXxxOvalOrArc(
            boolean isFillElseDraw,
            boolean isAboveElseBelowThreshold) {

        System.out.println();
        System.out.println(
                "bench_mustXxxOvalOrArc(...) : "
                        + (isFillElseDraw ? "fill" : "draw")
                        + ", " + (isAboveElseBelowThreshold ? "above threashold" : "below threshold"));

        for (int k = 0; k < NBR_OF_RUNS; k++) {
            
            final int spanThreshold = (int) Math.sqrt(GprimUtils.OVAL_ACCURATE_MUST_DRAW_COMPUTATION_THRESHOLD_AREA);
            final int ovalSpan;
            if (isAboveElseBelowThreshold) {
                ovalSpan = (int) (spanThreshold * 1.1);
            } else {
                ovalSpan = (int) (spanThreshold / 1.1);
            }
            
            // Worst-case: not a circle (+1),
            // so that atan2 gets called.
            final GRect oval = GRect.valueOf(0, 0, ovalSpan + 1, ovalSpan);
            
            final GRect clip;
            if (isFillElseDraw) {
                /*
                 * Worst-case clip: overlaps oval,
                 * and with just p4 (x,yMax) in oval.
                 */
                clip = oval.withPosDeltas(
                        (int) (0.75 * oval.xSpan()),
                        (int) (-0.75 * oval.ySpan()));
            } else {
                /*
                 * Worst-case clip: overlaps oval,
                 * and with just p4 (x,yMax) out of oval.
                 */
                clip = GRect.valueOf(
                        oval.xMid() - (oval.xSpan() / 4),
                        oval.yMid(),
                        (oval.xSpan() / 4),
                        (oval.ySpan() / 2) - 2);
            }
            
            int antiOptim = 0;
            
            final long a = System.nanoTime();
            for (int i = 0; i < NBR_OF_CALLS; i++) {
                final boolean res;
                if (isFillElseDraw) {
                    res = GprimUtils.mustFillOvalOrArc(
                            clip,
                            oval.x(), oval.y(), oval.xSpan(), oval.ySpan());
                } else {
                    res = GprimUtils.mustDrawOvalOrArc(
                            clip,
                            oval.x(), oval.y(), oval.xSpan(), oval.ySpan());
                }
                if (res) {
                    antiOptim++;
                } else {
                    // We design inputs such as must return true
                    // (to make sure we don't pass into some early return case).
                    throw new AssertionError();
                }
            }
            final long b = System.nanoTime();
            
            TestUtils.blackHole(antiOptim);
            
            final String method;
            if (isFillElseDraw) {
                method = GprimUtils.class.getSimpleName() + ".mustFillOvalOrArc(...)";
            } else {
                method = GprimUtils.class.getSimpleName() + ".mustDrawOvalOrArc(...)";
            }
            if (isAboveElseBelowThreshold) {
                System.out.println(
                        NBR_OF_CALLS + " calls on " + method + ", took " + TestUtils.nsToSRounded(b-a) + " s");
            } else {
                System.out.println(
                        NBR_OF_CALLS + " calls on " + method + ", took " + TestUtils.nsToSRounded(b-a) + " s");
            }
        }
    }
}
