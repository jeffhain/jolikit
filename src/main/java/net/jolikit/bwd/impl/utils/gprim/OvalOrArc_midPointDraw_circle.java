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
 * Algorithms specific for full circles outline drawing.
 * 
 * Uses "Midpoint circle algorithm", with longs not to overflow,
 * and with separate cases for odd or even spans so that we can
 * stick to integer arithmetic.
 * Also takes care not to draw a same pixel twice,
 * which would cause additional color blending.
 */
public class OvalOrArc_midPointDraw_circle {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Takes care of span reduction in case of overflow.
     */
    public static void drawCircle(
            GRect clip,
            int x, int y, int span,
            //
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfacePointDrawer pointDrawer,
            InterfaceRectDrawer rectDrawer) {

        if (DEBUG) {
            Dbg.log("drawCircle("
                    + clip
                    + ", " + x + ", " + y + ", " + span
                    + ",,,)");
        }
        
        span = GRect.trimmedSpan(x, span);
        span = GRect.trimmedSpan(y, span);

        /*
         * Special case for tiny circles.
         */
        
        if (span <= 2) {
            rectDrawer.drawRect(clip, x, y, span, span);
            return;
        }
        
        /*
         * 
         */

        if (!GprimUtils.mustDrawOvalOrArc(clip, x, y, span, span)) {
            if (DEBUG) {
                Dbg.log("must not draw");
            }
            return;
        }
        
        /*
         * 
         */

        if (NbrsUtils.isOdd(span)) {
            drawCircle_oddSpan(
                    clip,
                    x, y, span,
                    pointDrawer);
        } else {
            drawCircle_evenSpan(
                    clip,
                    x, y, span,
                    pointDrawer);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private OvalOrArc_midPointDraw_circle() {
    }
    
    /*
     * 
     */

    /**
     * @param span Must be >= 2.
     */
    private static void drawCircle_oddSpan(
            GRect clip,
            int x, int y, int span,
            InterfacePointDrawer pointDrawer) {

        final int radius = ((span - 1) >> 1);
        int xCenter = x + radius;
        int yCenter = y + radius;

        /*
         * x*x + y*y = r*r
         * 
         * (x,y) in circle:
         * x*x + y*y < r*r
         * x*x < r*r - y*y
         */
        final int r = radius;
        final long r2 = r * (long) r;
        x = 0;
        y = r;

        while (x <= y) {
            drawHeightCirclePoints(
                    clip,
                    xCenter, yCenter, x, y,
                    pointDrawer);
            x++;

            /*
             * Whether if (x, y-0.5) is in circle:
             * x*x + (y-0.5)*(y-0.5) < r*r
             * (2*y-1)*(2*y-1) < 4*(r*r - x*x)
             * 4*y*y - 4*y + 1 < 4*(r*r - x*x)
             * 4*y*y - 4*y < 4*(r*r - x*x) - 1
             * y*y - y < (r*r - x*x) - 0.25
             * 
             * This last form is not subject to overflows (using longs),
             * and since we use integers we can use "0" instead of "0.25":
             * 
             * y*y - y < r*r - x*x
             * or
             * x*x < r*r - y*y + y (must not overflow either, since y starts with r)
             */

            // (x+1) * (x+1) = x^2 + 2*x + 1
            long x2 = x * (long) x;
            long r2MY2PY = (r2 - y * (long) y) + y;
            if (x2 >= r2MY2PY) {
                // If equality, should paint the pixel with the most circle in it.
                y--;
            }
        }
    }

    /**
     * @param span Must be >= 2.
     */
    private static void drawCircle_evenSpan(
            GRect clip,
            int x, int y, int span,
            InterfacePointDrawer pointDrawer) {
        
        /*
         * Evenness-divided-by-two handling:
         * actual radius to consider is (r + 0.5),
         * actual xCenter is (xCenter + 0.5),
         * actual yCenter is (yCenter + 0.5),
         * actual x is (x + 0.5),
         * actual y is (y + 0.5).
         * 
         * Idea:
         * - do it in doubles.
         * - then, change to integers.
         */

        final int r = ((span - 1) >> 1);
        final int xCenter = x + r;
        final int yCenter = y + r;

        x = 0;
        y = r;

        /*
         * x*x + y*y = r*r
         * 
         * (x,y) in circle:
         * x*x + y*y < r*r
         * x*x < r*r - y*y
         */
        final long r2PR = r * (long) r + r;

        while (x <= y) {
            drawHeightCirclePoints_evenSpan(
                    clip,
                    xCenter,
                    yCenter,
                    x + 1,
                    y + 1,
                    pointDrawer);
            x++;

            /*
             * Whether if (x, y-0.5) is in circle:
             * x*x + (y-0.5)*(y-0.5) < r*r
             * (2*y-1)*(2*y-1) < 4*(r*r - x*x)
             * 4*y*y - 4*y + 1 < 4*(r*r - x*x)
             * 4*y*y - 4*y < 4*(r*r - x*x) - 1
             * y*y - y < (r*r - x*x) - 0.25
             * 
             * This last form is not subject to overflows (using longs),
             * and since we use integers we can use "0" instead of "0.25":
             * 
             * y*y - y < r*r - x*x
             * or
             * x*x < r*r - y*y + y (must not overflow either, since y starts with r)
             */

            // (x+1) * (x+1) = x^2 + 2*x + 1
            // Actually should be "x*x + x + 0.25", but we can ignore 0.25.
            long x2 = x * (long) x + x;
            // Actually should be "(r*r - y*y) + r + 0.5", but we can ignore0.5.
            long r2MY2PY = (r2PR - y * (long) y);
            if (x2 >= r2MY2PY) {
                // If equality, should paint the pixel with the most circle in it.
                y--;
            }
        }
    }
    
    /*
     * 
     */
    
    private static void drawHeightCirclePoints(
            GRect clip,
            int xCenter, int yCenter,
            int x, int y,
            InterfacePointDrawer pointDrawer) {
        if (x == 0) {
            if (y == 0) {
                pointDrawer.drawPoint(clip, xCenter, yCenter);
            } else {
                pointDrawer.drawPoint(clip, xCenter, yCenter + y);
                pointDrawer.drawPoint(clip, xCenter, yCenter - y);
                pointDrawer.drawPoint(clip, xCenter + y, yCenter);
                pointDrawer.drawPoint(clip, xCenter - y, yCenter);
            }
        } else {
            if (y == 0) {
                pointDrawer.drawPoint(clip, xCenter + x, yCenter);
                pointDrawer.drawPoint(clip, xCenter - x, yCenter);
                pointDrawer.drawPoint(clip, xCenter, yCenter + x);
                pointDrawer.drawPoint(clip, xCenter, yCenter - x);
            } else {
                if (x == y) {
                    pointDrawer.drawPoint(clip, xCenter + x, yCenter + x);
                    pointDrawer.drawPoint(clip, xCenter + x, yCenter - x);
                    pointDrawer.drawPoint(clip, xCenter - x, yCenter + x);
                    pointDrawer.drawPoint(clip, xCenter - x, yCenter - x);
                } else {
                    pointDrawer.drawPoint(clip, xCenter + x, yCenter + y);
                    pointDrawer.drawPoint(clip, xCenter + x, yCenter - y);
                    pointDrawer.drawPoint(clip, xCenter - x, yCenter + y);
                    pointDrawer.drawPoint(clip, xCenter - x, yCenter - y);
                    pointDrawer.drawPoint(clip, xCenter + y, yCenter + x);
                    pointDrawer.drawPoint(clip, xCenter + y, yCenter - x);
                    pointDrawer.drawPoint(clip, xCenter - y, yCenter + x);
                    pointDrawer.drawPoint(clip, xCenter - y, yCenter - x);
                }
            }
        }
    }

    /**
     * Center point is (actual x - 0.5, actual y - 0.5).
     */
    private static void drawHeightCirclePoints_evenSpan(
            GRect clip,
            int xCenter, int yCenter,
            int x, int y,
            InterfacePointDrawer pointDrawer) {
        if (((x == 0) || (x == 1))
                && ((y == 0) || (y == 1))) {
            pointDrawer.drawPoint(clip, xCenter, yCenter);
            pointDrawer.drawPoint(clip, xCenter, yCenter+1);
            pointDrawer.drawPoint(clip, xCenter+1, yCenter);
            pointDrawer.drawPoint(clip, xCenter+1, yCenter+1);
        } else {
            if (x == y) {
                pointDrawer.drawPoint(clip, xCenter + x, yCenter + x);
                pointDrawer.drawPoint(clip, xCenter + x, yCenter+1 - x);
                pointDrawer.drawPoint(clip, xCenter+1 - x, yCenter + x);
                pointDrawer.drawPoint(clip, xCenter+1 - x, yCenter+1 - x);
            } else {
                pointDrawer.drawPoint(clip, xCenter + x, yCenter + y);
                pointDrawer.drawPoint(clip, xCenter + x, yCenter+1 - y);
                pointDrawer.drawPoint(clip, xCenter+1 - x, yCenter + y);
                pointDrawer.drawPoint(clip, xCenter+1 - x, yCenter+1 - y);
                pointDrawer.drawPoint(clip, xCenter + y, yCenter + x);
                pointDrawer.drawPoint(clip, xCenter + y, yCenter+1 - x);
                pointDrawer.drawPoint(clip, xCenter+1 - y, yCenter + x);
                pointDrawer.drawPoint(clip, xCenter+1 - y, yCenter+1 - x);
            }
        }
    }
}
