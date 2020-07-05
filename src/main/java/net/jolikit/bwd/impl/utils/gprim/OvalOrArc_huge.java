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
 * Algorithm for drawing huge ovals or arcs, for which Bresenham-like algorithms
 * are too slow.
 */
public class OvalOrArc_huge {
    
    /*
     * Here we iterate on pixels in oval and clip intersection.
     * I could be much faster to draw segments between points of the curve
     * computed with trigonometric methods and an angular step,
     * but it would also be much less pixel-accurate, easily causing
     * shifts of hundreds or thousands of pixels for really huge ovals.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void drawOrFillHugeOvalOrArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            boolean areHorVerFlipped,
            boolean mustFill,
            //
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfaceRectDrawer rectDrawer) {
        
        if (DEBUG) {
            Dbg.log("drawOrFillHugeOvalOrArc("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ", " + areHorVerFlipped
                    + ", " + mustFill
                    + ",,)");
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
        
        final boolean isFull = (spanDeg == 360.0);
        
        /*
         * 
         */
        
        if (mustFill) {
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
        } else {
            if (!GprimUtils.mustDrawOvalOrArc(clip, x, y, xSpan, ySpan)) {
                if (DEBUG) {
                    Dbg.log("must not draw");
                }
                return;
            }
        }

        /*
         * 
         */
        
        final GRect oval = GRect.valueOf(x, y, xSpan, ySpan);
        final PixelFigStatusArcDef arcDef = new PixelFigStatusArcDef(
                oval,
                startDeg,
                spanDeg);
        
        final PixelFigStatusOvalAlgo.MyTemps ovalAlgoTemps =
                new PixelFigStatusOvalAlgo.MyTemps();
        
        /*
         * Totally brute force algorithm: just iterating on
         * all pixels in the intersection of clip and oval.
         */
        
        final GRect box = clip.intersected(oval);

        for (int j = 0; j < box.ySpan(); j++) {
            final int py = box.y() + j;
            
            // x1 > x2 means neither defined.
            int x1 = Integer.MAX_VALUE;
            int x2 = Integer.MIN_VALUE;
            
            for (int i = 0; i < box.xSpan(); i++) {
                final int px = box.x() + i;
                
                final PixelFigStatus pixelStatus =
                        PixelFigStatusArcAlgo.computePixelFigStatus(
                                arcDef,
                                mustFill,
                                px,
                                py,
                                //
                                ovalAlgoTemps);
                
                /*
                 * Drawing if required (of course), but also if "allowed",
                 * to avoid case where two pixels are "allowed" because
                 * either should be painted, and where painting neither
                 * causes a hole in the curve.
                 * 
                 * It might cause thicker curves than with regular algorithm,
                 * but it shouldn't hurt much for huge figures.
                 */
                final boolean mustDrawPixel =
                        (pixelStatus == PixelFigStatus.PIXEL_REQUIRED)
                        || (pixelStatus == PixelFigStatus.PIXEL_ALLOWED);
                
                if (mustDrawPixel) {
                    if (x1 > x2) {
                        x1 = px;
                    }
                    x2 = px;
                }
                
                if ((!mustDrawPixel)
                        || (i == box.xSpan() - 1)) {
                    if (x1 <= x2) {
                        clippedLineDrawer.drawHorizontalLineInClip(
                                x1, x2, py,
                                1, GprimUtils.PLAIN_PATTERN, 0);
                        x1 = Integer.MAX_VALUE;
                        x2 = Integer.MIN_VALUE;
                    }
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private OvalOrArc_huge() {
    }
}
