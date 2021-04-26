/*
 * Copyright 2019-2021 Jeff Hain
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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;

/**
 * To show how theoretical ovals draw over pixels.
 */
public class TheoreticalOvalOnGridBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    /**
     * Even, so that inner cell span is odd.
     */
    private static final int CELL_SPAN = 40;

    private static final GRect OVAL = GRect.valueOf(0, 0, 11, 6);

    private static final int ROW_COUNT = OVAL.ySpan();
    private static final int COL_COUNT = OVAL.xSpan();
    
    private static final int MIN_X_SPAN = CELL_SPAN * COL_COUNT + 1;
    private static final int MIN_Y_SPAN = CELL_SPAN * ROW_COUNT + 1;
    
    private static final int INITIAL_WIDTH = MIN_X_SPAN;
    private static final int INITIAL_HEIGHT = MIN_Y_SPAN;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public TheoreticalOvalOnGridBwdTestCase() {
    }
    
    public TheoreticalOvalOnGridBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new TheoreticalOvalOnGridBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new TheoreticalOvalOnGridBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect box = g.getBox();
        final int xSpan = box.xSpan();
        final int ySpan = box.ySpan();

        /*
         * Drawing pixels grid.
         */

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);

        g.setColor(BwdColor.BLACK);
        for (int i = 0; i <= COL_COUNT; i++) {
            int xx = i * CELL_SPAN;
            g.drawLine(xx, 0, xx, ySpan);
            g.drawText(xx, CELL_SPAN/2, "" + i);
        }
        for (int i = 0; i <= ROW_COUNT; i++) {
            int yy = i * CELL_SPAN;
            g.drawLine(0, yy, xSpan, yy);
            g.drawText(0, yy + CELL_SPAN/2, "" + i);
        }

        /*
         * Drawing the oval, pixel by pixel.
         */

        g.setColor(BwdColor.GREEN);

        final Set<GPoint> theoPointSet = computeExpectedDrawOvalPixelSet(
                GRect.valueOf(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
                OVAL.x() + CELL_SPAN/2,
                OVAL.y() + CELL_SPAN/2,
                OVAL.xSpan() * CELL_SPAN + 1 - CELL_SPAN,
                OVAL.ySpan() * CELL_SPAN + 1 - CELL_SPAN);

        for (GPoint point : theoPointSet) {
            g.drawPoint(point.x(), point.y());
        }

        /*
         * Drawing oval center.
         */

        int cx = (int) ((OVAL.x() + OVAL.xSpan() * 0.5) * CELL_SPAN);
        int cy = (int) ((OVAL.y() + OVAL.ySpan() * 0.5) * CELL_SPAN);
        int cr = CELL_SPAN/4;
        g.fillRect(cx - cr, cy - cr, 2*cr+1, 2*cr+1);

        /*
         * Drawing oval related coordinates.
         */

        g.setColor(BwdColor.BLACK);
        for (int i = 1; i <= OVAL.xSpan()/2; i++) {
            int xx = i * CELL_SPAN;
            g.drawText(cx - xx - CELL_SPAN/4, cy + CELL_SPAN/4, "" + i);
            g.drawText(cx + xx - CELL_SPAN/4, cy + CELL_SPAN/4, "" + i);
        }
        for (int i = 1; i <= OVAL.ySpan()/2; i++) {
            int yy = i * CELL_SPAN;
            g.drawText(cx - CELL_SPAN/4, cy - yy + CELL_SPAN/4, "" + i);
            g.drawText(cx - CELL_SPAN/4, cy + yy + CELL_SPAN/4, "" + i);
        }
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static Set<GPoint> computeExpectedDrawOvalPixelSet(
            GRect clip,
            int x,
            int y,
            int xSpan,
            int ySpan) {
        
        if (DEBUG) {
            Dbg.log();
            Dbg.log("computeExpectedDrawOvalPixelSet(...)");
        }
        
        final Set<GPoint> result = new HashSet<GPoint>();
        
        /*
         * Special cases.
         */
        
        if ((xSpan <= 0) || (ySpan <= 0)) {
            return result;
        }
        
        if (xSpan == 1) {
            for (int j = 0; j < ySpan; j++) {
                result.add(GPoint.valueOf(x, y + j));
            }
            return result;
        }
        
        if (ySpan == 1) {
            for (int i = 0; i < xSpan; i++) {
                result.add(GPoint.valueOf(x + i, y));
            }
            return result;
        }
        
        /*
         * 
         */

        double rx = (xSpan - 1) * 0.5;
        double ry = (ySpan - 1) * 0.5;

        double cx = x + rx;
        double cy = y + ry;
        
        /*
         * To make sure we cover all pixels covered by the circle,
         * instead of using "infinitely small" steps, we ensure that
         * the step is at most 0.5, and we consider pixels at radii
         * {r-0.5, r, r+0.5}.
         */
        
        double maxR = Math.max(rx, ry);
        // 1/a = tan(ang)
        // ang = atan(1/a)
        
        double stepRad = Math.atan(1.0/maxR) * 0.5;
        
        // Not bothering with symmetries.
        final double angSpanRad = 2 * Math.PI;
        
        // Reworking angStep for PI/2 to be (approximately) a multiple of it,
        // to have a regular spacing from 0 to PI/2,
        // and be able to iterate on integers.
        int stepCount = (int) Math.ceil(angSpanRad / stepRad);
        stepRad = angSpanRad / stepCount;
        
        if (DEBUG) {
            Dbg.log("cx = " + cx);
            Dbg.log("cy = " + cy);
            Dbg.log("rx = " + rx);
            Dbg.log("ry = " + ry);
            Dbg.log("stepRad = " + stepRad);
            Dbg.log("stepCount = " + stepCount);
        }
        
        for (int i = 0; i <= stepCount; i++) {
            if (DEBUG) {
                Dbg.log();
            }
            for (int k = -1; k <= 1; k++) {
                final double theoRx = rx + k * 0.5;
                final double theoRy = ry + k * 0.5;
                
                double angRad = i * stepRad;
                
                final double theoDx = theoRx * StrictMath.cos(angRad);
                final double theoDy = theoRy * StrictMath.sin(angRad);
                final double theoR = Math.sqrt(theoDx*theoDx + theoDy*theoDy);

                final int tmpX = BindingCoordsUtils.roundToInt(cx + theoDx);
                final int tmpY = BindingCoordsUtils.roundToInt(cy + theoDy);

                final double tmpDx = tmpX - cx;
                final double tmpDy = tmpY - cy;
                
                final boolean canLog = (k == 0);
                if (DEBUG && canLog) {
                    Dbg.log("k = " + k);
                    Dbg.log("theoRx = " + theoRx);
                    Dbg.log("theoRy = " + theoRy);
                    Dbg.log("cx = " + cx);
                    Dbg.log("theoRx * StrictMath.cos(angRad) = " + (theoRx * StrictMath.cos(angRad)));
                    Dbg.log("angDeg = " + Math.toDegrees(angRad));
                    Dbg.log("theoDx = " + theoDx + ", theoDy = " + theoDy);
                    Dbg.log("tmpX = " + tmpX + ", tmpY = " + tmpY);
                    Dbg.log("tmpDx = " + tmpDx + ", tmpDy = " + tmpDy);
                }

                /*
                 * We consider the oval overlaps the pixel,
                 * if nine interpolated points over the pixel
                 * are not either all outside or all inside the oval.
                 */
                int inOvalCount = 0;
                for (int u = -1; u <= 1; u++) {
                    for (int v = -1; v <= 1; v++) {
                        if (isInRadius(
                                rx,
                                ry,
                                tmpDx + u * 0.5,
                                tmpDy + v * 0.5,
                                canLog)) {
                            inOvalCount++;
                        }
                    }
                }
                if (DEBUG && canLog) {
                    Dbg.log("inOvalCount = " + inOvalCount);
                }
                
                // If a corner is exactly over the oval,
                // depending on which side others are, we can have a wrong value here,
                // but it doesn't hurt, since we use multiple radii and small enough spans.
                final boolean isOverOval =
                        (inOvalCount != 0)
                        && (inOvalCount != 9);
                if (isOverOval) {
                    if (clip.contains(tmpX, tmpY)) {
                        final GPoint point = GPoint.valueOf(tmpX, tmpY);
                        result.add(point);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * @return True if strictly in.
     */
    private static boolean isInRadius(
            double rx, double ry,
            double dx, double dy,
            boolean canLog) {
        // Local radius on the oval, at same angle than (dx,dy).
        final double localR;
        // Always false to use and check
        // the computation with circle case.
        if (false && (rx == ry)) {
            /*
             * Circle.
             */
            localR = rx;
        } else {
            /*
             * General case.
             */
            final double angRad = StrictMath.atan2(dy * rx, dx * ry);
            if (DEBUG && canLog) {
                Dbg.log("dx = " + dx + ", dy = " + dy + ", angDeg = " + Math.toDegrees(angRad));
            }
            final double ox = rx * StrictMath.cos(angRad);
            final double oy = ry * StrictMath.sin(angRad);
            localR = Math.sqrt(ox*ox + oy*oy);
        }
        if (DEBUG && canLog) {
            Dbg.log("localR = " + localR);
        }
        return (Math.sqrt(dx*dx + dy*dy) < localR);
    }
}
