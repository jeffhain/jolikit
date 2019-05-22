/*
 * Copyright 2019 Jeff Hain
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

/**
 * Testing perfs of regular and huge-specific algorithms,
 * around our default threshold between these algorithms
 * (to check that it's neither too large nor too small,
 * at least when using a very fast clipped point drawer).
 */
public class OvalOrArcPerf {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int NBR_OF_RUNS = 4;

    private static final int NBR_OF_CALLS = 10;
    
    /**
     * Benching at our default threshold.
     */
    private static final int OVAL_SPAN = DefaultHugeAlgoSwitch.DEFAULT_HUGE_SPAN_THRESHOLD;

    /**
     * Spans for oval and clip intersection box.
     */
    private static final int BOX_SPAN = 1000;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyClippedPointDrawer implements InterfaceClippedPointDrawer {
        int antiOptim = 0;
        @Override
        public void drawPointInClip(int x, int y) {
            this.antiOptim += x;
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        newRun(args);
    }

    public static void newRun(String[] args) {
        new OvalOrArcPerf().run(args);
    }
    
    public OvalOrArcPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void run(String[] args) {
        System.out.println("--- " + OvalOrArcPerf.class.getSimpleName() + "... ---");
        
        bench_oval_draw_regular();
        
        bench_oval_draw_huge();
        
        bench_oval_fill_regular();
        
        bench_oval_fill_huge();
        
        bench_arc_draw_regular();
        
        bench_arc_draw_huge();
        
        bench_arc_fill_regular();
        
        bench_arc_fill_huge();
        
        System.out.println("--- ..." + OvalOrArcPerf.class.getSimpleName() + " ---");
    }
    
    /*
     * 
     */

    private static void bench_oval_draw_regular() {
        bench(false, false, false);
    }

    private static void bench_oval_draw_huge() {
        bench(false, false, true);
    }

    private static void bench_oval_fill_regular() {
        bench(false, true, false);
    }

    private static void bench_oval_fill_huge() {
        bench(false, true, true);
    }

    private static void bench_arc_draw_regular() {
        bench(true, false, false);
    }

    private static void bench_arc_draw_huge() {
        bench(true, false, true);
    }

    private static void bench_arc_fill_regular() {
        bench(true, true, false);
    }
    
    private static void bench_arc_fill_huge() {
        bench(true, true, true);
    }
    
    /*
     * 
     */
    
    private static void bench(
            boolean isArcElseOval,
            boolean isFillElseDraw,
            boolean isHugeElseRegular) {
        
        System.out.println();
        System.out.println(
                "bench_huge(...) : "
                        + (isArcElseOval ? "arc" : "oval")
                        + ", " + (isFillElseDraw ? "fill" : "draw")
                        + ", " + (isHugeElseRegular ? "huge" : "regular"));
        
        for (int k = 0; k < NBR_OF_RUNS; k++) {
            
            final MyClippedPointDrawer clippedPointDrawer = new MyClippedPointDrawer();
            
            final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
            final DefaultClippedRectDrawer clippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
            
            final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
            final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
            final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, clippedRectDrawer);
            
            final GRect oval = GRect.valueOf(0, 0, OVAL_SPAN, OVAL_SPAN);
            
            // Clip contained in oval, with BOX spans,
            // and such as oval curve passes in it
            // to avoid special treatments.
            final GRect clip = GRect.valueOf(oval.xMid(), 0, BOX_SPAN, BOX_SPAN);
            if (!oval.contains(clip)) {
                throw new AssertionError();
            }
            
            final int hugeSpanThreshold = (isHugeElseRegular ? 0 : Integer.MAX_VALUE);
            final DefaultHugeAlgoSwitch hugeAlgoSwitch = new DefaultHugeAlgoSwitch(
                    hugeSpanThreshold);
            
            final long a = System.nanoTime();
            for (int i = 0; i < NBR_OF_CALLS; i++) {
                
                final double startDeg = (isArcElseOval ? 123.456 + i : 0.0);
                // Almost full if arc, since we compare times.
                final double spanDeg = (isArcElseOval ? 359.0 : 360.0);

                if (isFillElseDraw) {
                    final boolean areHorVerFlipped = false;
                    OvalOrArc_anyFill.fillOvalOrArc(
                            clip,
                            oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                            startDeg, spanDeg,
                            areHorVerFlipped,
                            //
                            hugeAlgoSwitch,
                            //
                            clippedLineDrawer,
                            //
                            pointDrawer,
                            lineDrawer,
                            rectDrawer);
                } else {
                    OvalOrArc_anyDraw.drawOvalOrArc(
                            clip,
                            oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                            startDeg, spanDeg,
                            //
                            hugeAlgoSwitch,
                            //
                            clippedLineDrawer,
                            //
                            pointDrawer,
                            lineDrawer,
                            rectDrawer);
                }
            }
            final long b = System.nanoTime();
            
            if (clippedPointDrawer.antiOptim == 0) {
                System.out.println("antiOptim");
            }
            
            final String method;
            if (isFillElseDraw) {
                method = OvalOrArc_anyFill.class.getSimpleName() + ".fillOvalOrArc(...)";
            } else {
                method = OvalOrArc_anyDraw.class.getSimpleName() + ".drawOvalOrArc(...)";
            }
            if (isHugeElseRegular) {
                System.out.println(
                        NBR_OF_CALLS + " calls on " + method + ", "
                                + BOX_SPAN + " span box, took " + TestUtils.nsToSRounded(b-a) + " s");
            } else {
                System.out.println(
                        NBR_OF_CALLS + " calls on " + method + ", "
                                + OVAL_SPAN + " span oval, took " + TestUtils.nsToSRounded(b-a) + " s");
            }
        }
    }
}
