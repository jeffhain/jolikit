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
 * Algorithms specific for oval or arc outline drawing.
 * 
 * Uses "Midpoint circle algorithm", adapted for ovals,
 * and with doubles not to overflow
 * and to handle both odd or even spans,
 * and with angular range checks for arcs.
 * Also takes care not to draw a same pixel twice,
 * which would cause additional color blending.
 */
public class OvalOrArc_midPointDraw {
    
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
     */
    public static void drawOvalOrArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            //
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer) {

        if (DEBUG) {
            Dbg.log("drawOvalOrArc("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ",,,,,)");
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
         * Special case for full circle.
         */

        final boolean isFull = (spanDeg == 360.0);
        
        final boolean isFullCircle =
                isFull
                && (xSpan == ySpan);
        if (isFullCircle) {
            if (DEBUG) {
                Dbg.log("special case : full circle");
            }
            OvalOrArc_midPointDraw_circle.drawCircle(
                    clip,
                    x, y, xSpan,
                    //
                    clippedLineDrawer,
                    //
                    pointDrawer,
                    rectDrawer);
            return;
        }

        /*
         * Special case for ovals with a tiny span.
         */

        if (isFull
                && ((xSpan <= 2) || (ySpan <= 2))) {
            if (DEBUG) {
                Dbg.log("special case : full with a span <= 2");
            }
            // Easy.
            rectDrawer.drawRect(clip, x, y, xSpan, ySpan);
            return;
        }
        
        /*
         * Special case for ovals/arcs with a span of 1.
         */
        
        if ((xSpan == 1) || (ySpan == 1)) {
            if (DEBUG) {
                Dbg.log("special case : a span = 1");
            }
            final double rx = ((xSpan - 1) * 0.5);
            final double ry = ((ySpan - 1) * 0.5);
            final double cx = x + rx;
            final double cy = y + ry;
            OvalOrArc_span1.drawArc_xSpan1_or_ySpan1(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    cx, cy,
                    pointDrawer,
                    lineDrawer);
            return;
        }

        /*
         * 
         */

        if (!GprimUtils.mustDrawOvalOrArc(clip, x, y, xSpan, ySpan)) {
            if (DEBUG) {
                Dbg.log("must not draw");
            }
            return;
        }
        
        /*
         * 
         */

        drawOvalOrArc_fp(
                clip,
                x, y, xSpan, ySpan,
                startDeg, spanDeg,
                clippedPointDrawer,
                pointDrawer);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private OvalOrArc_midPointDraw() {
    }

    /*
     * 
     */
    
    /**
     * @param oxSpan Must be >= 2.
     * @param oySpan Must be >= 2.
     */
    private static void drawOvalOrArc_fp(
            GRect clip,
            int ox, int oy, int oxSpan, int oySpan,
            double startDeg, double spanDeg,
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfacePointDrawer pointDrawer) {

        final boolean isXSpanOdd = NbrsUtils.isOdd(oxSpan);
        final boolean isYSpanOdd = NbrsUtils.isOdd(oySpan);

        final double rx = ((oxSpan - 1) * 0.5);
        final double ry = ((oySpan - 1) * 0.5);
        final double cx = ox + rx;
        final double cy = oy + ry;

        /*
         * Oval equation (x and y being counted from center):
         * x = rx * cos(ang)
         * y = ry * sin(ang)
         */

        // Offsets to adjust coordinates to be either
        // in the middle of a pixel, or between two pixels.
        final double xOffset = (isXSpanOdd ? 0.0 : 0.5);
        final double yOffset = (isYSpanOdd ? 0.0 : 0.5);
        
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
         * Iterating on x where the curve is closer to an y = constant line,
         * and on y where the curve is closer to an x = constant line.
         */

        // To avoid drawing a same pixel twice,
        // where both loops end up.
        final double lastDrawnDxForLoop1;
        final double lastDrawnDyForLoop1;
        {
            double tmpLastDrawnDx = Double.NaN;
            double tmpLastDrawnDy = Double.NaN;

            /*
             * Iterating on x.
             * 
             * Let:
             * r = ry
             * a = ry/rx
             * 
             * dx = r/a * cos(ang)
             * dy = r * sin(ang)
             * 
             * a*dx = r * cos(ang)
             * y = r * sin(ang)
             * 
             * a*a*dx*dx + dy*dy = r*r
             * 
             * (dx,dy) in oval:
             * a*a*dx*dx + dy*dy < r*r
             * a*a*dx*dx < r*r - dy*dy
             */

            final double r = ry;
            final double a = ry/rx;

            final double r2 = r*r;
            final double a2 = a*a;

            double dx = xOffset;
            double dy = r; // yOffset included in radius.

            if (DEBUG) {
                Dbg.log("while (1) : dx = " + dx);
                Dbg.log("while (1) : dy = " + dy);
            }
            while (dx * a <= dy) {
                drawUpToFourPoints_fp(
                        clip,
                        cx, cy, dx, dy,
                        startDeg, spanDeg,
                        v1x, v1y, v2x, v2y,
                        clippedPointDrawer,
                        pointDrawer);
                tmpLastDrawnDx = dx;
                tmpLastDrawnDy = dy;
                dx++;
                if (DEBUG) {
                    Dbg.log("while (1) : dx++ to " + dx);
                }

                /*
                 * Whether if (dx, dy-0.5) is in oval:
                 * a*a*dx*dx + (dy-0.5)*(dy-0.5) < r*r
                 * (dy-0.5)*(dy-0.5) < r*r - a*a*dx*dx
                 * (2*dy-1)*(2*dy-1) < 4 * (r*r - a*a*dx*dx)
                 * 4*dy*dy - 4*dy + 1 < 4 * (r*r - a*a*dx*dx)
                 * dy*dy - dy + 0.25 < (r*r - a*a*dx*dx)
                 * a*a*dx*dx < r*r - dy*dy + dy - 0.25
                 * 
                 * NB: If using longs (in optimized and odd/even-specific implementations),
                 * this last form is not subject to overflows, and then since using
                 * mathematical integers we could use "0" instead of "0.25":
                 * a*a*dx*dx < r*r - dy*dy + dy
                 * or
                 * dy*dy - dy < r*r - a*a*dx*dx (must not overflow either)
                 */

                double dx2 = dx * dx;
                double a2dx2 = a2 * dx2;
                double r2Mdy2Pdy = (r2 - dy * dy) + dy - 0.25;
                if (a2dx2 >= r2Mdy2Pdy) {
                    // (dx, dy-0.5) is on or out of oval.
                    // If equality, that should paint the pixel with the most curve in it.
                    dy--;
                    if (DEBUG) {
                        Dbg.log("while (1) : dy-- to " + dy);
                    }

                    /*
                     * Here, when approaching the part where |dx/ds| ~= |dy/ds|,
                     * if we do nothing special (as with the "usual" midpoint algorithm),
                     * we could go too far away from the curve, since we would always
                     * advance x coordinate.
                     * To avoid that, we now check the point (dx-0.5, dy),
                     * and eventually do dx-- as well.
                     * 
                     * Whether if (dx-0.5, dy) is in oval:
                     * a*a*(dx-0.5)*(dx-0.5) + dy*dy < r*r
                     * a*a*(2*dx-1)*(2*dx-1) + 4*dy*dy < 4*r*r
                     * a*a*(4*dx*dx - 4*dx + 1) + 4*dy*dy < 4*r*r
                     * a*a*(dx*dx - dx + 0.25) + y*y < r*r
                     * a*a*(dx*dx - dx + 0.25) < r*r - dy*dy
                     */

                    double a2dx2Mdx = a2 * (dx2 - dx + 0.25);
                    double r2Mdy2 = (r2 - dy * dy);
                    if (a2dx2Mdx >= r2Mdy2) {
                        // (dx-0.5, dy) is on or out of oval.
                        dx--;
                        if (DEBUG) {
                            Dbg.log("while (1) : dx-- to " + dx);
                        }
                    }
                }
            }

            lastDrawnDxForLoop1 = tmpLastDrawnDx;
            lastDrawnDyForLoop1 = tmpLastDrawnDy;
        }
        {
            /*
             * Iterating on y.
             * 
             * Same but swapping x and y.
             */
            final double r = rx;
            final double a = rx/ry;

            final double r2 = r*r;
            final double a2 = a*a;

            double dy = yOffset;
            double dx = r;
            
            if (DEBUG) {
                Dbg.log("while (2) : dx = " + dx);
                Dbg.log("while (2) : dy = " + dy);
            }
            while (dy * a <= dx) {
                if ((dx == lastDrawnDxForLoop1)
                        && (dy == lastDrawnDyForLoop1)) {
                    break;
                }
                drawUpToFourPoints_fp(
                        clip,
                        cx, cy, dx, dy,
                        startDeg, spanDeg,
                        v1x, v1y, v2x, v2y,
                        clippedPointDrawer,
                        pointDrawer);
                dy++;
                if (DEBUG) {
                    Dbg.log("while (2) : dy++ to " + dy);
                }

                double dy2 = dy * dy;
                double a2dy2 = a2 * dy2;
                double r2Mdx2Pdx = (r2 - dx * dx) + dx - 0.25;
                if (a2dy2 >= r2Mdx2Pdx) {
                    dx--;
                    if (DEBUG) {
                        Dbg.log("while (2) : dx-- to " + dx);
                    }

                    double a2dy2Mdy = a2 * (dy2 - dy + 0.25);
                    double r2Mdx2 = (r2 - dx * dx);
                    if (a2dy2Mdy >= r2Mdx2) {
                        dy--;
                        if (DEBUG) {
                            Dbg.log("while (2) : dy-- to " + dy);
                        }
                    }
                }
            }
        }
    }
    
    /*
     * 
     */

    /**
     * (cx +- dx) and (cy +- dy) must be mathematical integers.
     */
    private static void drawUpToFourPoints_fp(
            GRect clip,
            double cx, double cy,
            double dx, double dy,
            double startDeg, double spanDeg,
            double v1x, double v1y,
            double v2x, double v2y,
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfacePointDrawer pointDrawer) {
        
        final boolean isFull = (spanDeg == 360.0);
        if (isFull) {
            drawUpToFourPoints_fp_noAngleCheck(
                    clip,
                    cx, cy, dx, dy,
                    pointDrawer);
        } else {
            drawUpToFourPoints_fp_angleCheck(
                    clip,
                    cx, cy, dx, dy,
                    startDeg, spanDeg,
                    v1x, v1y, v2x, v2y,
                    clippedPointDrawer);
        }
    }

    /**
     * Optimization with no angle check to draw full ovals fast.
     * 
     * (cx +- dx) and (cy +- dy) must be mathematical integers.
     */
    private static void drawUpToFourPoints_fp_noAngleCheck(
            GRect clip,
            double cx, double cy,
            double dx, double dy,
            InterfacePointDrawer pointDrawer) {
        
        final int cxMdx = (int) (cx - dx);
        final int cxPdx = (int) (cx + dx);
        final int cyMdy = (int) (cy - dy);
        final int cyPdy = (int) (cy + dy);

        if (cxPdx == cxMdx) {
            if (cyPdy == cyMdy) {
                pointDrawer.drawPoint(clip, cxPdx, cyPdy);
            } else {
                pointDrawer.drawPoint(clip, cxPdx, cyPdy);
                pointDrawer.drawPoint(clip, cxPdx, cyMdy);
            }
        } else {
            if (cyPdy == cyMdy) {
                pointDrawer.drawPoint(clip, cxPdx, cyPdy);
                pointDrawer.drawPoint(clip, cxMdx, cyPdy);
            } else {
                pointDrawer.drawPoint(clip, cxPdx, cyPdy);
                pointDrawer.drawPoint(clip, cxPdx, cyMdy);
                pointDrawer.drawPoint(clip, cxMdx, cyPdy);
                pointDrawer.drawPoint(clip, cxMdx, cyMdy);
            }
        }
    }

    /**
     * (cx +- dx) and (cy +- dy) must be mathematical integers.
     */
    private static void drawUpToFourPoints_fp_angleCheck(
            GRect clip,
            double cx, double cy,
            double dx, double dy,
            double startDeg, double spanDeg,
            double v1x, double v1y,
            double v2x, double v2y,
            InterfaceClippedPointDrawer clippedPointDrawer) {

        final int cxMdx = (int) (cx - dx);
        final int cxPdx = (int) (cx + dx);
        final int cyMdy = (int) (cy - dy);
        final int cyPdy = (int) (cy + dy);
        
        /*
         * Doing clip checks before heavier angle checks.
         */
        
        if (clip.contains(cxPdx, cyPdy)
                && GprimUtils.isInAngularRange(dx, dy, spanDeg, v1x, v1y, v2x, v2y)) {
            clippedPointDrawer.drawPointInClip(cxPdx, cyPdy);
        }
        if (cyPdy != cyMdy) {
            if (clip.contains(cxPdx, cyMdy)
                    && GprimUtils.isInAngularRange(dx, -dy, spanDeg, v1x, v1y, v2x, v2y)) {
                clippedPointDrawer.drawPointInClip(cxPdx, cyMdy);
            }
        }
        if (cxPdx != cxMdx) {
            if (clip.contains(cxMdx, cyPdy)
                    && GprimUtils.isInAngularRange(-dx, dy, spanDeg, v1x, v1y, v2x, v2y)) {
                clippedPointDrawer.drawPointInClip(cxMdx, cyPdy);
            }
            if (cyPdy != cyMdy) {
                if (clip.contains(cxMdx, cyMdy)
                        && GprimUtils.isInAngularRange(-dx, -dy, spanDeg, v1x, v1y, v2x, v2y)) {
                    clippedPointDrawer.drawPointInClip(cxMdx, cyMdy);
                }
            }
        }
    }
}
