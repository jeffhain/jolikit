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
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.test.utils.TestUtils;

/**
 * Not bothering to bench polylines,
 * it's about the same as drawing polygons.
 */
public class GraphicsBencher_drawAndFillPoly {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int DEFAULT_NBR_OF_RUNS = 2;

    private static final int DEFAULT_NBR_OF_CALLS = 100;
    
    private static final double SPIRAL_ROUND_COUNT = 10.0;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final int nbrOfRuns;
    
    private final int nbrOfCalls;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public GraphicsBencher_drawAndFillPoly(
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
    public static void bench_drawPolygon_static(InterfaceBwdGraphics g) {
        final GraphicsBencher_drawAndFillPoly bencher = new GraphicsBencher_drawAndFillPoly(
                DEFAULT_NBR_OF_RUNS,
                DEFAULT_NBR_OF_CALLS);
        bencher.bench_drawPolygon(g);
    }

    /**
     * Uses a default bencher.
     */
    public static void bench_fillPolygon_static(InterfaceBwdGraphics g) {
        final GraphicsBencher_drawAndFillPoly bencher = new GraphicsBencher_drawAndFillPoly(
                DEFAULT_NBR_OF_RUNS,
                DEFAULT_NBR_OF_CALLS);
        bencher.bench_fillPolygon(g);
    }

    /*
     * 
     */

    public void bench_drawPolygon(InterfaceBwdGraphics g) {
        final boolean isFill = false;
        this.bench_drawOrFillPolygon(g, isFill);
    }

    public void bench_fillPolygon(InterfaceBwdGraphics g) {
        final boolean isFill = true;
        this.bench_drawOrFillPolygon(g, isFill);
    }

    /*
     * 
     */
    
    public void bench_drawOrFillPolygon(
            InterfaceBwdGraphics g,
            boolean isFill) {
        
        final GRect clipInBase = g.getClipInBase();
        final int clipSpan = clipInBase.xSpan();
        
        System.out.println("nbrOfCalls = " + this.nbrOfCalls);
        System.out.println("clipSpan = " + clipSpan);

        for (int polygonSpan : new int[] {100, clipSpan, 10 * clipSpan, 100 * clipSpan}) {
            for (int pointCount : new int[] {100, 1000, 10000}) {
                for (boolean ellipseElseSpiral : new boolean[] {true, false}) {
                    final int xMaxRadius = polygonSpan / 2;
                    final int yMaxRadius = polygonSpan / 2;
                    final double roundCount = SPIRAL_ROUND_COUNT;
                    final int[] xArr = new int[pointCount];
                    final int[] yArr = new int[pointCount];
                    if (ellipseElseSpiral) {
                        final double stepRad = (2*Math.PI) / pointCount;
                        for (int k = 0; k < pointCount; k++) {
                            final double angRad = k * stepRad;
                            xArr[k] = (int) (xMaxRadius * Math.sin(angRad));
                            yArr[k] = (int) (yMaxRadius * Math.cos(angRad));
                        }
                    } else {
                        BwdTestUtils.computeSpiralPolygonPoints(
                                xMaxRadius,
                                yMaxRadius,
                                pointCount,
                                roundCount,
                                xArr,
                                yArr);
                    }
                    // For centering in clip, without having to add a transform.
                    final GRect clip = g.getClipInBase();
                    for (int k = 0; k < pointCount; k++) {
                        xArr[k] += clip.xMid();
                        yArr[k] += clip.yMid();
                    }

                    for (int k = 0; k < this.nbrOfRuns; k++) {
                        long a = System.nanoTime();
                        for (int i = 0; i < this.nbrOfCalls; i++) {
                            if (isFill) {
                                g.fillPolygon(xArr, yArr, pointCount);
                            } else {
                                g.drawPolygon(xArr, yArr, pointCount);
                            }
                        }
                        long b = System.nanoTime();
                        
                        final StringBuilder sb = new StringBuilder();
                        sb.append(isFill ? "fillPolygon()" : "drawPolygon()");
                        sb.append(": (" + polygonSpan);
                        sb.append(", " + pointCount);
                        sb.append(" pts), " + (ellipseElseSpiral ? "ellipse" : " spiral"));
                        System.out.println(
                                sb.toString() + ", took "
                                + TestUtils.nsToSRounded(b-a) + " s");
                    }
                }
            }
        }
    }
}
