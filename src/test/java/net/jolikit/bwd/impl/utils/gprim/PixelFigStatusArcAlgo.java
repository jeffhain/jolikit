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

/**
 * Algorithm to compute whether a pixel must, could, or must not be painted
 * when drawing or filling an arc.
 */
public class PixelFigStatusArcAlgo {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static PixelFigStatus computePixelFigStatus(
            TestArcArgs hugeArcDef,
            boolean isFillElseDraw,
            int x,
            int y,
            //
            PixelFigStatusOvalAlgo.MyTemps hugeOvalAlgoTemps) {
        
        if (DEBUG) {
            Dbg.log("computePixelFigStatus("
                    + hugeArcDef
                    + ", " + isFillElseDraw
                    + ", " + x
                    + ", " + y
                    + ",)");
        }

        final GRect oval = hugeArcDef.getOval();
        final double startDeg = hugeArcDef.getReworkedStartDeg();
        final double spanDeg = hugeArcDef.getReworkedSpanDeg();
        
        /*
         * Special cases.
         */
        
        if (spanDeg == 0.0) {
            if (DEBUG) {
                Dbg.log("ret (1) = PIXEL_NOT_ALLOWED : nothing to fill");
            }
            return PixelFigStatus.PIXEL_NOT_ALLOWED;
        }
        
        final PixelFigStatus pixelOvalStatus =
                PixelFigStatusOvalAlgo.computePixelFigStatus(
                        oval,
                        isFillElseDraw,
                        x,
                        y,
                        //
                        hugeOvalAlgoTemps);

        if (DEBUG) {
            Dbg.log("pixelOvalStatus = " + pixelOvalStatus + " for pixel (" + x + ", " + y + ")");
        }
        
        if (pixelOvalStatus == PixelFigStatus.PIXEL_NOT_ALLOWED) {
            if (DEBUG) {
                Dbg.log("ret (2) = " + pixelOvalStatus);
            }
            // All out of oval, so out of arc area.
            return pixelOvalStatus;
        }
        
        if (spanDeg == 360.0) {
            if (DEBUG) {
                Dbg.log("ret (3) = " + pixelOvalStatus);
            }
            // Arc is full oval.
            return pixelOvalStatus;
        }
        
        /*
         * Here:
         * - span < 360: need to check angles.
         * - pixelOvalStatus is either PIXEL_REQUIRED or PIXEL_ALLOWED.
         */
        
        final int ox = oval.x();
        final int oy = oval.y();
        final int oxSpan = oval.xSpan();
        final int oySpan = oval.ySpan();
        
        /*
         * Fine if rx and/or ry is zero,
         * further treatments being robust
         * to such special cases.
         */
        
        final double rx = (oxSpan - 1) * 0.5;
        final double ry = (oySpan - 1) * 0.5;
        
        final double sinStart = hugeArcDef.getSinStart();
        final double cosStart = hugeArcDef.getCosStart();
        final double sinEnd = hugeArcDef.getSinEnd();
        final double cosEnd = hugeArcDef.getCosEnd();
        
        // Oval center.
        final double cx = ox + rx;
        final double cy = oy + ry;
        
        /*
         * Prolonging start/end angles segments past oval curve,
         * otherwise, we could have pixels on oval curve,
         * that don't intersect them, and which center
         * is not in angular range either, and being between
         * pixels that either intersect them or are in angular range,
         * causing gaps on the arc.
         * Simply using twice the radius is enough.
         */
        
        final double twoRx = rx + rx;
        final double twoRy = ry + ry;
        
        // Point for start angle.
        final double sx = cx + twoRx * cosStart;
        final double sy = cy + twoRy * -sinStart;
        
        // Point for end angle.
        final double ex = cx + twoRx * cosEnd;
        final double ey = cy + twoRy * -sinEnd;
        
        final double dx = x - cx;
        final double dy = y - cy;
        if (DEBUG) {
            Dbg.log("cx = " + cx);
            Dbg.log("cy = " + cy);
            Dbg.log("sx = " + sx);
            Dbg.log("sy = " + sy);
            Dbg.log("ex = " + ex);
            Dbg.log("ey = " + ey);
            Dbg.log("dx = " + dx);
            Dbg.log("dy = " + dy);
        }

        /*
         * isPixelOverXxxSegment : Useful not to miss pixels
         * which center is not in angular range, but over which
         * the mathematical figure to draw or fill still passes,
         * in particular in case of tiny angular spans in which
         * there would be no pixel center.
         */
        
        // No need to enlarge, so shrinking to be conservative.
        // Also, enlarging can cause trouble, if segment is included in the pixel.
        final boolean mustEnlargeElseShrinkPixel = false;
        
        final boolean isPixelOverStartSegmentTight =
                GprimUtils.doSegmentIntersectASurroundingSide(
                        cx, cy, sx, sy,
                        x, y,
                        mustEnlargeElseShrinkPixel);
        final boolean isPixelOverStartSegmentLoose =
                isPixelOverStartSegmentTight
                || doCloseSegmentIntersectASurroundingSide(
                        cx, cy, sx, sy,
                        x, y,
                        mustEnlargeElseShrinkPixel);
        if (DEBUG) {
            Dbg.log("isPixelOverStartSegmentTight = " + isPixelOverStartSegmentTight);
            Dbg.log("isPixelOverStartSegmentLoose = " + isPixelOverStartSegmentLoose);
        }

        final boolean isPixelOverEndSegmentTight =
                GprimUtils.doSegmentIntersectASurroundingSide(
                        cx, cy, ex, ey,
                        x, y,
                        mustEnlargeElseShrinkPixel);
        final boolean isPixelOverEndSegmentLoose =
                isPixelOverEndSegmentTight
                || doCloseSegmentIntersectASurroundingSide(
                        cx, cy, ex, ey,
                        x, y,
                        mustEnlargeElseShrinkPixel);
        if (DEBUG) {
            Dbg.log("isPixelOverEndSegmentTight = " + isPixelOverEndSegmentTight);
            Dbg.log("isPixelOverEndSegmentLoose = " + isPixelOverEndSegmentLoose);
        }
        
        /*
         * We assume the drawer also checks whether a pixel
         * can be drawn or not depending on angular range,
         * by checking whether its center belongs or not
         * to the angular range, and not whether some part
         * of the pixel belongs or not to it.
         * Our implementation does that, and it allows
         * to keep things simple.
         */
        final boolean isPixelCenterInAngularRange =
                GprimUtils.isInAngularRange(
                        dx, dy,
                        startDeg, spanDeg,
                        sinStart, cosStart,
                        sinEnd, cosEnd,
                        rx, ry);
        if (DEBUG) {
            Dbg.log("isPixelCenterInAngularRange = " + isPixelCenterInAngularRange);
        }
        
        final PixelFigStatus ret;
        if (isPixelOverStartSegmentLoose
                || isPixelOverEndSegmentLoose) {
            /*
             * Pixel overlaps start or end segment.
             * 
             * Considering the pixel can be drawn for arc
             * if it can be drawn for oval, but taking care
             * not to use PIXEL_REQUIRED, to allow for drawing treatments
             * to be simple and just rule out pixels which center
             * is not in angular range, or to use rounded coordinates
             * when drawing start and end segments.
             */
            if (pixelOvalStatus == PixelFigStatus.PIXEL_REQUIRED) {
                ret = PixelFigStatus.PIXEL_ALLOWED;
            } else {
                ret = pixelOvalStatus;
            }
        } else if (isPixelCenterInAngularRange) {
            if ((pixelOvalStatus == PixelFigStatus.PIXEL_REQUIRED)
                    && ((oxSpan == 1)
                            || (oySpan == 1))) {
                /*
                 * Oval on a line: angular range for pixels is ambiguous.
                 * Allowing for pixel not to be painted.
                 */
                ret = PixelFigStatus.PIXEL_ALLOWED;
            } else {
                /*
                 * Clearly in angular range: using oval status.
                 */
                ret = pixelOvalStatus;
            }
        } else {
            /*
             * Clearly out of angular range.
             */
            ret = PixelFigStatus.PIXEL_NOT_ALLOWED;
        }
        
        if (DEBUG) {
            Dbg.log("ret (5) = " + ret);
        }
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private PixelFigStatusArcAlgo() {
    }
    
    /**
     * Checks the eight segments with adjacent extremities (moving both),
     * to handle the case of arcs drawing based on polygons,
     * which use rounded coordinates for segments.
     */
    private static boolean doCloseSegmentIntersectASurroundingSide(
            double cx, double cy, double segExtrX, double segExtrY,
            int x, int y,
            boolean mustEnlargeElseShrinkPixel) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if ((i == 0) && (j == 0)) {
                    // Not doing "exact" segment.
                    continue;
                }
                final boolean hit = GprimUtils.doSegmentIntersectASurroundingSide(
                        cx + i, cy + j, segExtrX + i, segExtrY + j,
                        x, y,
                        mustEnlargeElseShrinkPixel);
                if (hit) {
                    return true;
                }
            }
        }
        return false;
    }
}
