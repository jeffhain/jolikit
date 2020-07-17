/*
 * Copyright 2020 Jeff Hain
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
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.test.utils.TestUtils;

public class GraphicsBencher_drawAndFillArc {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int DEFAULT_NBR_OF_RUNS = 2;

    private static final int DEFAULT_NBR_OF_CALLS = 100;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final int nbrOfRuns;

    private final int nbrOfCalls;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public GraphicsBencher_drawAndFillArc(
            int nbrOfRuns,
            int nbrOfCalls) {
        this.nbrOfRuns = nbrOfRuns;
        this.nbrOfCalls = nbrOfCalls;
    }

    /*
     * 
     */

    /**
     * Uses a default bencher.
     */
    public static void bench_drawArc_static(InterfaceBwdGraphics g) {
        final GraphicsBencher_drawAndFillArc bencher = new GraphicsBencher_drawAndFillArc(
                DEFAULT_NBR_OF_RUNS,
                DEFAULT_NBR_OF_CALLS);
        bencher.bench_drawArc(g);
    }

    /**
     * Uses a default bencher.
     */
    public static void bench_fillArc_static(InterfaceBwdGraphics g) {
        final GraphicsBencher_drawAndFillArc bencher = new GraphicsBencher_drawAndFillArc(
                DEFAULT_NBR_OF_RUNS,
                DEFAULT_NBR_OF_CALLS);
        bencher.bench_fillArc(g);
    }

    /*
     * 
     */

    public void bench_drawArc(InterfaceBwdGraphics g) {
        final boolean isFillElseDraw = false;
        this.bench_drawOrFillArc(g, isFillElseDraw);
    }

    public void bench_fillArc(InterfaceBwdGraphics g) {
        final boolean isFillElseDraw = true;
        this.bench_drawOrFillArc(g, isFillElseDraw);
    }

    /*
     * 
     */

    public void bench_drawOrFillArc(
            InterfaceBwdGraphics g,
            boolean isFillElseDraw) {

        final GRect clipInBase = g.getClipInBase();
        final int clipSpan = clipInBase.xSpan();

        final double startDeg = 0.0;

        System.out.println("nbrOfCalls = " + this.nbrOfCalls);
        System.out.println("clipSpan = " + clipSpan);

        for (int ovalXSpan : new int[] {
                clipSpan / 10,
                clipSpan / 4,
                clipSpan / 2,
                (clipSpan / 4) * 3,
                clipSpan,
                5 * clipSpan,
                10 * clipSpan,
                50 * clipSpan,
                100 * clipSpan,
                500 * clipSpan,
                1000 * clipSpan,
                5 * 1000 * clipSpan}) {
            // Avoiding circle.
            final int ovalYSpan = (int) (ovalXSpan * 0.9);
            /*
             * Making sure arc passes through clip,
             * at angle zero (which we always cover),
             * to avoid cases where drawing is optimized away.
             */
            final GRect clip = g.getClipInBase();
            
            final int openingPixelSpan = (Math.min(clip.ySpan(), ovalYSpan) / 4);
            final double closeToFullSpanDeg =
                    360.0 - openingPixelSpan * (180.0/Math.PI)/(ovalXSpan/2);
            for (double spanDeg : new double[] {
                    10.0,
                    45.0,
                    90.0,
                    180.0,
                    270.0,
                    closeToFullSpanDeg,
                    360.0}) {

                for (boolean clipAtCenterElseOnArc : new boolean[] {
                        false,
                        true}) {
                    final int ovalX;
                    if (clipAtCenterElseOnArc) {
                        ovalX = (clip.xMid() - ovalXSpan / 2);
                    } else {
                        ovalX = (clip.xMax() - (ovalXSpan - 1));
                    }
                    final int ovalY = (clip.yMid() - ovalYSpan / 2);
                    
                    for (int k = 0; k < this.nbrOfRuns; k++) {
                        long a = System.nanoTime();
                        for (int i = 0; i < this.nbrOfCalls; i++) {
                            if (isFillElseDraw) {
                                g.fillArc(
                                        ovalX, ovalY, ovalXSpan, ovalYSpan,
                                        startDeg, spanDeg);
                            } else {
                                g.drawArc(
                                        ovalX, ovalY, ovalXSpan, ovalYSpan,
                                        startDeg, spanDeg);
                            }
                        }
                        long b = System.nanoTime();

                        final StringBuilder sb = new StringBuilder();
                        sb.append(isFillElseDraw ? "fillArc()" : "drawArc()");
                        sb.append(": (" + ovalXSpan);
                        sb.append(", " + spanDeg + ")");
                        sb.append(", clip: " + (clipAtCenterElseOnArc ? "at center" : "on arc"));
                        System.out.println(
                                sb.toString()
                                + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                    }
                }
            }
        }
    }
}
