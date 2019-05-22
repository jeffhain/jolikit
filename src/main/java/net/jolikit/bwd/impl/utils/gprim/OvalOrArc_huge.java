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
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void drawOrFillHugeOvalOrArc(
            GRect clip,
            int ox, int oy, int oxSpan, int oySpan,
            double startDeg, double spanDeg,
            boolean isFillElseDraw,
            InterfaceClippedLineDrawer clippedLineDrawer) {
        
        final GRect oval = GRect.valueOf(ox, oy, oxSpan, oySpan);
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
            final int y = box.y() + j;
            
            // x1 > x2 means neither defined.
            int x1 = Integer.MAX_VALUE;
            int x2 = Integer.MIN_VALUE;
            
            for (int i = 0; i < box.xSpan(); i++) {
                final int x = box.x() + i;
                
                final PixelFigStatus pixelStatus =
                        PixelFigStatusArcAlgo.computePixelFigStatus(
                                arcDef,
                                isFillElseDraw,
                                x,
                                y,
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
                        x1 = x;
                    }
                    x2 = x;
                }
                
                if ((!mustDrawPixel)
                        || (i == box.xSpan() - 1)) {
                    if (x1 <= x2) {
                        clippedLineDrawer.drawHorizontalLineInClip(
                                x1, x2, y,
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
