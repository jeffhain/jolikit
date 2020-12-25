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
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;

/**
 * Algorithms specific for oval or arc filling.
 * 
 * Uses "Midpoint circle algorithm", adapted for ovals,
 * and with doubles not to overflow
 * and to handle both odd or even spans,
 * and with angular range checks for arcs.
 * Also takes care not to draw a same pixel twice,
 * which would cause additional color blending.
 */
public class OvalOrArc_midPointFill {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Takes care of span reduction in case of overflow.
     * 
     * @param startDeg Must be in [0.0,360.0[.
     * @param spanDeg Must be in [0,360.0].
     * @param areHorVerFlipped True if transform rotation is 90 or 270 degrees,
     *        in which case it should be more efficient to fill using
     *        vertical lines than horizontal lines.
     */
    public static void fillOvalOrArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            boolean areHorVerFlipped,
            //
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer) {

        if (DEBUG) {
            Dbg.log("fillOvalOrArc("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ", " + areHorVerFlipped
                    + ",,,,)");
        }
        
        final boolean isEmpty =
                (xSpan <= 0)
                || (ySpan <= 0)
                || (spanDeg == 0.0);
        if (isEmpty) {
            return;
        }

        xSpan = GRect.trimmedSpan(x, xSpan);
        ySpan = GRect.trimmedSpan(y, ySpan);

        /*
         * 
         */

        final boolean isFull = (spanDeg == 360.0);
        
        /*
         * Special case for ovals with a tiny span.
         */

        if ((xSpan <= 2) || (ySpan <= 2)) {
            if (isFull) {
                // Easy.
                rectDrawer.drawRect(clip, x, y, xSpan, ySpan);
                return;
            }

            // Draw is enough for filling this.
            OvalOrArc_midPointDraw.drawOvalOrArc(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    //
                    clippedPointDrawer,
                    clippedLineDrawer,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer);
            return;
        }
        
        /*
         * 
         */

        if (!GprimUtils.mustFillOvalOrArc(clip, x, y, xSpan, ySpan)) {
            if (DEBUG) {
                Dbg.log("must not fill");
            }
            return;
        }
        
        /*
         * Special case for ovals containing the clip.
         */

        if (isFull
                && GprimUtils.isClipInOval(clip, x, y, xSpan, ySpan)) {
            if (DEBUG) {
                Dbg.log("fill clip");
            }
            rectDrawer.fillRect(
                    clip,
                    clip.x(), clip.y(), clip.xSpan(), clip.ySpan(),
                    areHorVerFlipped);
            return;
        }

        /*
         * TODO optim Could, if worth the additional code:
         * - Use areHorVerFlipped.
         * - Special case depending on odd or even spans, as done
         *   for circles drawing.
         * - Special case for circles.
         */

        fillOvalOrArc_fp(
                clip,
                x, y, xSpan, ySpan,
                startDeg, spanDeg,
                clippedPointDrawer,
                lineDrawer);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private OvalOrArc_midPointFill() {
    }
    
    /*
     * 
     */
    
    /**
     * @param oxSpan Must be >= 2.
     * @param oySpan Must be >= 2.
     */
    private static void fillOvalOrArc_fp(
            GRect clip,
            int ox, int oy, int oxSpan, int oySpan,
            double startDeg, double spanDeg,
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceLineDrawer lineDrawer) {

        /*
         * Derived from outline drawing case, drawing lines
         * instead of just points.
         */

        final boolean oddXSpan = NbrsUtils.isOdd(oxSpan);
        final boolean oddYSpan = NbrsUtils.isOdd(oySpan);

        final double rx = ((oxSpan - 1) * 0.5);
        final double ry = ((oySpan - 1) * 0.5);
        final double cx = ox + rx;
        final double cy = oy + ry;

        final double xOffset = (oddXSpan ? 0.0 : 0.5);
        final double yOffset = (oddYSpan ? 0.0 : 0.5);

        /*
         * 
         */
        
        final double cosStartDeg = GprimUtils.cosDeg(startDeg);
        final double sinStartDeg = GprimUtils.sinDeg(startDeg);
        final double cosEndDeg = GprimUtils.cosDeg(startDeg + spanDeg);
        final double sinEndDeg = GprimUtils.sinDeg(startDeg + spanDeg);
        // Scaled angular vectors, so that we don't have to unscale (dx,dy).
        final double v1x = rx * cosStartDeg;
        final double v1y = ry * -sinStartDeg;
        final double v2x = rx * cosEndDeg;
        final double v2y = ry * -sinEndDeg;
        
        /*
         * 
         */
        
        // To avoid drawing a same line twice,
        // where both loops end up.
        final double lastDrawnDxForLoop1;
        final double lastDrawnDyForLoop1;
        {
            double tmpLastDrawnDx = Double.NaN;
            double tmpLastDrawnDy = Double.NaN;

            final double r = ry;
            final double a = ry/rx;

            final double r2 = r*r;
            final double a2 = a*a;

            double dx = xOffset;
            double dy = r;
            
            // Only bothering to draw if y gets decremented.
            boolean drawPending = false;
            boolean mustDrawNow = false;

            double prevDx = dx;
            double prevDy = dy;
            while (dx * a <= dy) {
                prevDx = dx;
                prevDy = dy;
                drawPending = true;

                dx++;

                double dx2 = dx * dx;
                double a2dx2 = a2 * dx2;
                double r2Mdy2Pdy = (r2 - dy * dy) + dy - 0.25;
                if (a2dx2 >= r2Mdy2Pdy) {
                    dy--;
                    mustDrawNow = true;

                    double a2dx2Mdx = a2 * (dx2 - dx + 0.25);
                    double r2Mdy2 = (r2 - dy * dy);
                    if (a2dx2Mdx >= r2Mdy2) {
                        dx--;
                    }
                }

                if (mustDrawNow) {
                    if (DEBUG) {
                        Dbg.log("drawTwoHorizontalLines_fp_angleCheck(1):");
                        Dbg.log("clip = " + clip);
                        Dbg.log("cx = " + cx);
                        Dbg.log("cy = " + cy);
                        Dbg.log("prevDx = " + prevDx);
                        Dbg.log("prevDy = " + prevDy);
                    }
                    drawTwoHorizontalLines_fp_angleCheck(
                            clip,
                            cx, cy,
                            prevDx, prevDy,
                            startDeg, spanDeg,
                            v1x, v1y, v2x, v2y,
                            clippedPointDrawer,
                            lineDrawer);
                    tmpLastDrawnDx = prevDx;
                    tmpLastDrawnDy = prevDy;
                    drawPending = false;
                    mustDrawNow = false;
                }
            }

            if (DEBUG) {
                Dbg.log("dy = " + dy);
                Dbg.log("r  = " + r);
            }
            if (drawPending) {
                mustDrawNow = true;
            }
            if (mustDrawNow) {
                if (DEBUG) {
                    Dbg.log("drawTwoHorizontalLines_fp_angleCheck(2):");
                    Dbg.log("clip = " + clip);
                    Dbg.log("cx = " + cx);
                    Dbg.log("cy = " + cy);
                    Dbg.log("prevDx = " + prevDx);
                    Dbg.log("prevDy = " + prevDy);
                }
                drawTwoHorizontalLines_fp_angleCheck(
                        clip,
                        cx, cy,
                        prevDx, prevDy,
                        startDeg, spanDeg,
                        v1x, v1y, v2x, v2y,
                        clippedPointDrawer,
                        lineDrawer);
                tmpLastDrawnDx = prevDx;
                tmpLastDrawnDy = prevDy;
                drawPending = false;
                mustDrawNow = false;
            }

            lastDrawnDxForLoop1 = tmpLastDrawnDx;
            lastDrawnDyForLoop1 = tmpLastDrawnDy;
        }

        if (DEBUG) {
            Dbg.log("lastDrawnDxForLoop1 = " + lastDrawnDxForLoop1);
            Dbg.log("lastDrawnDyForLoop1 = " + lastDrawnDyForLoop1);
        }

        {
            /*
             * Iterating on y.
             */
            final double r = rx;
            final double a = rx/ry;

            final double r2 = r*r;
            final double a2 = a*a;
            
            double dy = yOffset;
            double dx = r;

            boolean drawPending = true;

            while (dy * a <= dx) {
                if (dy == lastDrawnDyForLoop1) {
                    /*
                     * Eventually extending the already drawn segment at this y.
                     */
                    if (drawPending
                            && (dx > lastDrawnDxForLoop1)) {
                        final double maxDxDrawnAtY = lastDrawnDxForLoop1;
                        drawLeftAndRightExtension_fp_angleCheck(
                                clip,
                                cx, cy,
                                dx, dy,
                                maxDxDrawnAtY,
                                startDeg, spanDeg,
                                v1x, v1y, v2x, v2y,
                                clippedPointDrawer,
                                lineDrawer);
                    }
                    break;
                }
                if (drawPending) {
                    if (DEBUG) {
                        Dbg.log("drawTwoHorizontalLines_fp(3):");
                        Dbg.log("clip = " + clip);
                        Dbg.log("cx = " + cx);
                        Dbg.log("cy = " + cy);
                        Dbg.log("dx = " + dx);
                        Dbg.log("dy = " + dy);
                    }
                    drawTwoHorizontalLines_fp_angleCheck(
                            clip,
                            cx, cy,
                            dx, dy,
                            startDeg, spanDeg,
                            v1x, v1y, v2x, v2y,
                            clippedPointDrawer,
                            lineDrawer);
                }
                dy++;
                if (DEBUG) {
                    Dbg.log("dy++ to " + dy);
                }
                drawPending = true;

                double dy2 = dy * dy;
                double a2dy2 = a2 * dy2;
                double r2Mdx2Pdx = (r2 - dx * dx) + dx - 0.25;
                if (a2dy2 >= r2Mdx2Pdx) {
                    dx--;
                    if (DEBUG) {
                        Dbg.log("dx-- to " + dx);
                    }

                    double a2dy2Mdy = a2 * (dy2 - dy + 0.25);
                    double r2Mdx2 = (r2 - dx * dx);
                    if (a2dy2Mdy >= r2Mdx2) {
                        dy--;
                        if (DEBUG) {
                            Dbg.log("dy-- to " + dy);
                        }
                        // No need to draw this time, since we already
                        // drew a larger horizontal line for this y.
                        drawPending = false;
                    }
                }
            }
        }
    }
    
    /**
     * (cx +- dx) and (cy +- dy) must be mathematical integers.
     */
    private static void drawTwoHorizontalLines_fp_angleCheck(
            GRect clip,
            double cx, double cy,
            double dx, double dy,
            double startDeg, double spanDeg,
            double v1x, double v1y,
            double v2x, double v2y,
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceLineDrawer lineDrawer) {
        
        final int cxMdx = (int) (cx - dx);
        final int cxPdx = (int) (cx + dx);
        final int cyMdy = (int) (cy - dy);
        final int cyPdy = (int) (cy + dy);

        final boolean isFull = (spanDeg == 360.0);
        if (isFull) {
            if (DEBUG) {
                Dbg.log("lineDrawer.drawLine(1)(" + cxMdx + "," + cyPdy + "," + cxPdx + "," + cyPdy + ")");
            }
            lineDrawer.drawLine(clip, cxMdx, cyPdy, cxPdx, cyPdy);
            if (cyMdy != cyPdy) {
                if (DEBUG) {
                    Dbg.log("lineDrawer.drawLine(2)(" + cxMdx + "," + cyMdy + "," + cxPdx + "," + cyMdy + ")");
                }
                lineDrawer.drawLine(clip, cxMdx, cyMdy, cxPdx, cyMdy);
            }
        } else {
            drawHorizontalLine_integer_angleCheck(
                    clip,
                    cxMdx, cxPdx,
                    cyPdy,
                    cx, cy,
                    spanDeg,
                    v1x, v1y, v2x, v2y,
                    clippedPointDrawer);
            // Taking care not to draw a same pixel twice,
            // which would mess up alpha rendering.
            if (cyMdy != cyPdy) {
                drawHorizontalLine_integer_angleCheck(
                        clip,
                        cxMdx, cxPdx,
                        cyMdy,
                        cx, cy,
                        spanDeg,
                        v1x, v1y, v2x, v2y,
                        clippedPointDrawer);
            }
        }
    }
    
    private static void drawHorizontalLine_integer_angleCheck(
            GRect clip,
            int xMin, int xMax,
            int y,
            double cx, double cy,
            double spanDeg,
            double v1x, double v1y,
            double v2x, double v2y,
            InterfaceClippedPointDrawer clippedPointDrawer) {
        if (DEBUG) {
            Dbg.log("drawHorizontalLine_integer_angleCheck("
                    + clip
                    + ", " + xMin + ", " + xMax
                    + ", " + y
                    + ", " + cx + ", " + cy
                    + ", " + spanDeg
                    + ", " + v1x + ", " + v1y + ", " + v2x + ", " + v2y
                    + ",)");
        }
        
        if ((y < clip.y()) || (y > clip.yMax())) {
            return;
        }
        xMin = Math.max(xMin, clip.x());
        xMax = Math.min(xMax, clip.xMax());

        final double dy = y - cy;
        for (int x = xMin; x <= xMax; x++) {
            final double dx = x - cx;
            if (GprimUtils.isInAngularRange(dx, dy, spanDeg, v1x, v1y, v2x, v2y)) {
                if (DEBUG) {
                    Dbg.log("clippedPointDrawer.drawPointInClip(" + x + ", " + y + ")");
                }
                clippedPointDrawer.drawPointInClip(x, y);
            } else {
                if (DEBUG) {
                    Dbg.log("NOT in angular range, dx = " + dx + ", dy = " + dy + ", spanDeg = " + spanDeg);
                }
            }
        }
    }
    
    /**
     * To eventually extend the horizontal drawn at (end of) first loop,
     * when second loop reaches the junction point too but would like
     * to draw more at the same "y" coordinate.
     */
    private static void drawLeftAndRightExtension_fp_angleCheck(
            GRect clip,
            double cx, double cy,
            double dx, double dy,
            double maxDxDrawnAtY,
            double startDeg, double spanDeg,
            double v1x, double v1y,
            double v2x, double v2y,
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceLineDrawer lineDrawer) {
        final int x1Far = (int) (cx - dx);
        final int x1Close = (int) (cx - (maxDxDrawnAtY + 1.0));
        final int x2Far = (int) (cx + dx);
        final int x2Close = (int) (cx + (maxDxDrawnAtY + 1.0));
        final int y1 = (int) (cy - dy);
        final int y2 = (int) (cy + dy);
        
        final boolean isFull = (spanDeg == 360.0);
        if (isFull) {
            if (DEBUG) {
                Dbg.log("lineDrawer.drawLine(ext_1_left)(" + x1Far + "," + y1 + "," + x1Close + "," + y1 + ")");
            }
            lineDrawer.drawLine(clip, x1Far, y1, x1Close, y1);
            if (DEBUG) {
                Dbg.log("lineDrawer.drawLine(ext_1_right)(" + x2Close + "," + y1 + "," + x2Far + "," + y1 + ")");
            }
            lineDrawer.drawLine(clip, x2Close, y1, x2Far, y1);
            if (y2 != y1) {
                if (DEBUG) {
                    Dbg.log("lineDrawer.drawLine(ext_2_left)(" + x1Far + "," + y2 + "," + x1Close + "," + y2 + ")");
                }
                lineDrawer.drawLine(clip, x1Far, y2, x1Close, y2);
                if (DEBUG) {
                    Dbg.log("lineDrawer.drawLine(ext_2_right)(" + x2Close + "," + y2 + "," + x2Far + "," + y2 + ")");
                }
                lineDrawer.drawLine(clip, x2Close, y2, x2Far, y2);
            }
        } else {
            drawHorizontalLine_integer_angleCheck(
                    clip,
                    x1Far, x1Close,
                    y1,
                    cx, cy,
                    spanDeg,
                    v1x, v1y, v2x, v2y,
                    clippedPointDrawer);
            drawHorizontalLine_integer_angleCheck(
                    clip,
                    x2Close, x2Far,
                    y1,
                    cx, cy,
                    spanDeg,
                    v1x, v1y, v2x, v2y,
                    clippedPointDrawer);
            if (y2 != y1) {
                drawHorizontalLine_integer_angleCheck(
                        clip,
                        x1Far, x1Close,
                        y2,
                        cx, cy,
                        spanDeg,
                        v1x, v1y, v2x, v2y,
                        clippedPointDrawer);
                drawHorizontalLine_integer_angleCheck(
                        clip,
                        x2Close, x2Far,
                        y2,
                        cx, cy,
                        spanDeg,
                        v1x, v1y, v2x, v2y,
                        clippedPointDrawer);
            }
        }
    }
}
