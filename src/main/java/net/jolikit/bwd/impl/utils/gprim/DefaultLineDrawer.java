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
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;

public class DefaultLineDrawer implements InterfaceLineDrawer {

    /*
     * TODO upgrade If ever want to do anti-aliased bresenhams
     * (for lines, ovals, etc.):
     * http://members.chello.at/~easyfilter/bresenham.html
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * TODO jdk Can use Math.nextAfter(...) from JDK 6+.
     */
    private static final double ONE_MINUS_1_ULP = 0.9999999999999999;
    private static final double ONE_PLUS_1_ULP = 1.0000000000000002;

    private final InterfaceClippedLineDrawer clippedLineDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses a default clipped line drawer, using the specified
     * clipped point drawer.
     */
    public DefaultLineDrawer(InterfaceClippedPointDrawer clippedPointDrawer) {
        this.clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
    }
    
    public DefaultLineDrawer(InterfaceClippedLineDrawer clippedLineDrawer) {
        this.clippedLineDrawer = LangUtils.requireNonNull(clippedLineDrawer);
    }
    
    /*
     * Instance methods.
     */
    
    @Override
    public void drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2) {
        drawLine(
                clip,
                x1, y1, x2, y2,
                this.clippedLineDrawer);
    }

    @Override
    public int drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        return drawLine(
                clip,
                x1, y1, x2, y2,
                factor, pattern, pixelNum,
                this.clippedLineDrawer);
    }

    /*
     * Static methods.
     */
    
    public static void drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2,
            InterfaceClippedLineDrawer clippedLineDrawer) {
        drawLine(
                clip,
                x1, y1, x2, y2,
                1, GprimUtils.PLAIN_PATTERN, 0,
                clippedLineDrawer);
    }

    public static int drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum,
            InterfaceClippedLineDrawer clippedLineDrawer) {
        
        final int trueX1 = x1;
        final int trueY1 = y1;
        final int trueX2 = x2;
        final int trueY2 = y2;
        
        // First, making sure x1 <= x2.
        final boolean p1p2Swap = (x1 > x2);
        if (p1p2Swap) {
            int tmp = x1;
            x1 = x2;
            x2 = tmp;
            
            tmp = y1;
            y1 = y2;
            y2 = tmp;
        }

        // NB: Positive slope... goes down on screen, since y points downward.
        final boolean hadPositiveSlopeAfterSwap = (y1 < y2);
        int yMin;
        int yMax;
        if (hadPositiveSlopeAfterSwap) {
            yMin = y1;
            yMax = y2;
        } else {
            yMin = y2;
            yMax = y1;
        }

        // Fast check.
        if ((x2 < clip.x())
                || (yMax < clip.y())
                || (x1 > clip.xMax())
                || (yMin > clip.yMax())
                || clip.isEmpty()) {
            // Line outside clip (not all cases,
            // except for horizontal/vertical lines),
            // or empty clip.
            if (DEBUG) {
                Dbg.log("OUT (obvious)");
            }
            pixelNum = GprimUtils.pixelNumPlusSegmentPixelLengthNormalized(
                    x1, y1, x2, y2,
                    factor,
                    pixelNum);
            return pixelNum;
        }
        
        // Horizontal case first (should be the most used,
        // for cache-friendly fillings).
        if (y1 == y2) {
            pixelNum = pixelNumPlusDistToClip_horizontal(
                    clip,
                    trueX1,
                    factor,
                    pixelNum);
            
            x1 = Math.max(x1, clip.x());
            x2 = Math.min(x2, clip.xMax());
            if (p1p2Swap) {
                int tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            // Horizontal line, now clipped.
            if (DEBUG) {
                Dbg.log("IN CLIP (horizontal)");
            }
            pixelNum = clippedLineDrawer.drawHorizontalLineInClip(
                    x1, x2, y1,
                    factor, pattern, pixelNum);
            
            pixelNum = pixelNumPlusDistToClip_horizontal(
                    clip,
                    trueX2,
                    factor,
                    pixelNum);
            
            return pixelNum;
        }

        if (x1 == x2) {
            pixelNum = pixelNumPlusDistToClip_vertical(
                    clip,
                    trueY1,
                    factor,
                    pixelNum);
            
            yMin = Math.max(yMin, clip.y());
            yMax = Math.min(yMax, clip.yMax());
            if (hadPositiveSlopeAfterSwap) {
                y1 = yMin;
                y2 = yMax;
            } else {
                y2 = yMin;
                y1 = yMax;
            }
            if (p1p2Swap) {
                int tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            // Vertical line, now clipped.
            if (DEBUG) {
                Dbg.log("IN CLIP (vertical)");
            }
            pixelNum = clippedLineDrawer.drawVerticalLineInClip(
                    x1, y1, y2,
                    factor, pattern, pixelNum);
            
            pixelNum = pixelNumPlusDistToClip_vertical(
                    clip,
                    trueY2,
                    factor,
                    pixelNum);
            
            return pixelNum;
        }
        
        return clipLine_withSlope(
                clip,
                x1, y1, x2, y2,
                factor, pattern, pixelNum,
                p1p2Swap,
                hadPositiveSlopeAfterSwap,
                clippedLineDrawer);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int pixelNumPlusDistToClip_horizontal(
            GRect clip,
            int x,
            int factor,
            int pixelNum) {
        final long pixelDist = computePixelDistToClip_horizontal(
                clip,
                x);
        if (DEBUG) {
            Dbg.log("dist from x = " + x + " to clip = " + pixelDist);
        }
        pixelNum = GprimUtils.pixelNumPlusLongAmountNormalized(factor, pixelNum, pixelDist);
        return pixelNum;
    }
    
    private static int pixelNumPlusDistToClip_vertical(
            GRect clip,
            int y,
            int factor,
            int pixelNum) {
        final long pixelDist = computePixelDistToClip_vertical(
                clip,
                y);
        if (DEBUG) {
            Dbg.log("dist from y = " + y + " to clip = " + pixelDist);
        }
        pixelNum = GprimUtils.pixelNumPlusLongAmountNormalized(factor, pixelNum, pixelDist);
        return pixelNum;
    }
    
    private static long computePixelDistToClip_horizontal(
            GRect clip,
            int x) {
        final long skippedAmount;
        if (x < clip.x()) {
            skippedAmount = (clip.x() - (long) x);
        } else if (x > clip.xMax()) {
            skippedAmount = (x - (long) clip.xMax());
        } else {
            skippedAmount = 0;
        }
        return skippedAmount;
    }
    
    private static long computePixelDistToClip_vertical(
            GRect clip,
            int y) {
        final long skippedAmount;
        if (y < clip.y()) {
            skippedAmount = (clip.y() - (long) y);
        } else if (y > clip.yMax()) {
            skippedAmount = (y - (long) clip.yMax());
        } else {
            skippedAmount = 0;
        }
        return skippedAmount;
    }
    
    /*
     * 
     */

    /**
     * Must have x1 <= x2,
     * and clip not empty.
     */
    private static int clipLine_withSlope(
            final GRect clip,
            int x1,
            int y1,
            int x2,
            int y2,
            final int factor,
            final short pattern,
            int pixelNum,
            final boolean p1p2Swap,
            final boolean positiveSlope,
            InterfaceClippedLineDrawer clippedLineDrawer) {
        
        if (DEBUG) {
            Dbg.log("clipLine_withSlope(" + clip
                    + ", " + x1 + ", " + y1 + ", " + x2 + ", " + y2
                    + ", " + factor + ", " + pattern + ", " + pixelNum
                    + ", " + p1p2Swap + ", " + positiveSlope + ",)");
        }
        
        double x1d = x1;
        double y1d = y1;
        double x2d = x2;
        double y2d = y2;
        
        /*
         * Intersecting line with clip borders, if needed,
         * to draw only the relevant part.
         * While doing this, if we find out the line is outside clip,
         * we return.
         * Taking care to compute with doubles, to avoid integer overflow
         * and preserve line slope despite clipping.
         */

        /*
         * We actually consider the clip corresponding to input clip
         * enlarged by 0.5, with border excluded (to make sure resulting
         * coordinates always round into the clip).
         * This makes sure that clipping doesn't suppress pixels
         * that would be paint in clip area if there was no clipping.
         * 
         * While doing this, we ensure not to simply add 0.5 to actual
         * coordinates, since due to rounding errors it could allow for
         * coordinates that don't round into the clip.
         */
        
        final double h = 0.5;
        // Rounds to clipX.
        final double clipXExtended = (clip.x() - h) * (clip.x() <= 0 ? ONE_MINUS_1_ULP : ONE_PLUS_1_ULP);
        // Rounds to clipY.
        final double clipYExtended = (clip.y() - h) * (clip.y() <= 0 ? ONE_MINUS_1_ULP : ONE_PLUS_1_ULP);
        // Rounds to clipXMax.
        final double clipXMaxExtended = (clip.xMax() + h) * (clip.xMax() < 0 ? ONE_PLUS_1_ULP : ONE_MINUS_1_ULP);
        // Rounds to clipYMax.
        final double clipYMaxExtended = (clip.yMax() + h) * (clip.yMax() < 0 ? ONE_PLUS_1_ULP : ONE_MINUS_1_ULP);
        if (DEBUG) {
            Dbg.log("clipXExtended = " + clipXExtended);
            Dbg.log("clipYExtended = " + clipYExtended);
            Dbg.log("clipXMaxExtended = " + clipXMaxExtended);
            Dbg.log("clipYMaxExtended = " + clipYMaxExtended);
        }

        /*
         * Getting x coordinates back in range, if needed.
         */

        final boolean x1Out = (x1d < clipXExtended);
        final boolean x2Out = (x2d > clipXMaxExtended);
        final boolean someXOut = (x1Out || x2Out);
        double a = Double.NaN;
        double b = Double.NaN;
        boolean equationComputed = false;
        if (someXOut) {
            // y1 = a * x1 + b
            // y2 = a * x2 + b
            // a = (y2 - y1) / (x2 - x1)
            // b = y - a * x
            final double denomInv = 1.0 / (x2d - x1d);
            a = (y2d - y1d) * denomInv;
            b = y1d - a * x1d;
            if (DEBUG) {
                Dbg.log("a (act,clip1) = " + a);
                Dbg.log("b (act,clip1) = " + b);
            }
            equationComputed = true;
            // First, getting x coordinates in range.
            // No problem if computed y's are out of range, we will
            // take care of them later.
            if (x1Out) {
                x1d = clipXExtended;
                // Typically no longer a mathematical integer.
                y1d = a * x1d + b;
            }
            if (x2Out) {
                x2d = clipXMaxExtended;
                // Typically no longer a mathematical integer.
                y2d = a * x2d + b;
            }
            if (Math.max(y1d,y2d) < clipYExtended) {
                // Line above clip (all cases).
                if (DEBUG) {
                    Dbg.log("OUT : line above (all cases)");
                }
                pixelNum = GprimUtils.pixelNumPlusSegmentPixelLengthNormalized(
                        x1, y1, x2, y2,
                        factor,
                        pixelNum);
                return pixelNum;
            }
            if (Math.min(y1d,y2d) > clipYMaxExtended) {
                // Line below clip (all cases).
                if (DEBUG) {
                    Dbg.log("OUT : line below (all cases)");
                }
                pixelNum = GprimUtils.pixelNumPlusSegmentPixelLengthNormalized(
                        x1, y1, x2, y2,
                        factor,
                        pixelNum);
                return pixelNum;
            }
        }

        /*
         * Getting y coordinates back in range, if needed.
         * 
         * Mathematically we would only need to test against
         * lower or upper bound, depending on whether slope
         * is positive or negative, but we test against both
         * to guard against possible inaccuracies.
         */

        final boolean y1Out = (y1d < clipYExtended) || (y1d > clipYMaxExtended);
        final boolean y2Out = (y2d < clipYExtended) || (y2d > clipYMaxExtended);
        final boolean someYOut = (y1Out || y2Out);
        if (someYOut) {
            if (!equationComputed) {
                final double denomInv = 1.0 / (x2d - x1d);
                a = (y2d - y1d) * denomInv;
                b = y1d - a * x1d;
                if (DEBUG) {
                    Dbg.log("a (act,clip2) = " + a);
                    Dbg.log("b (act,clip2) = " + b);
                }
            }
            final double aInv = 1.0 / a;
            final double bDivA = b * aInv;
            // x = 1/a * y - b/a
            if (y1Out) {
                y1d = (positiveSlope ? clipYExtended : clipYMaxExtended);
                // Typically no longer a mathematical integer.
                x1d = aInv * y1d - bDivA;
                // x1 was already in clip, so since we get y1 back in clip range,
                // x1 should now be well in clip, so no need to ensure it's in
                // to guard against eventual rounding errors.
            }
            if (y2Out) {
                y2d = (positiveSlope ? clipYMaxExtended : clipYExtended);
                // Typically no longer a mathematical integer.
                x2d = aInv * y2d - bDivA;
                // x2 was already in clip, so since we get y2 back in clip range,
                // x2 should now be well in clip, so no need to ensure it's in
                // to guard against eventual rounding errors.
            }
        }
        
        /*
         * Ensuring initial p1/p2 order.
         */
        
        if (p1p2Swap) {
            {
                double tmp = x1d;
                x1d = x2d;
                x2d = tmp;
                
                tmp = y1d;
                y1d = y2d;
                y2d = tmp;
            }
            {
                int tmp = x1;
                x1 = x2;
                x2 = tmp;
                
                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
        }

        /*
         * 
         */
        
        if (someXOut || someYOut) {
            if (DEBUG) {
                Dbg.log("CLIPPED (general)");
                Dbg.log("x1d = " + NumbersUtils.toStringNoCSN(x1d));
                Dbg.log("y1d = " + NumbersUtils.toStringNoCSN(y1d));
                Dbg.log("x2d = " + NumbersUtils.toStringNoCSN(x2d));
                Dbg.log("y2d = " + NumbersUtils.toStringNoCSN(y2d));
            }
            
            pixelNum = clippedLineDrawer.drawGeneralLineClipped(
                    clip,
                    x1, y1, x2, y2,
                    x1d, y1d, x2d, y2d,
                    factor, pattern, pixelNum);
            
            return pixelNum;
        } else {
            if (DEBUG) {
                Dbg.log("IN CLIP");
            }
            
            return clippedLineDrawer.drawGeneralLineInClip(
                    x1, y1, x2, y2,
                    factor, pattern, pixelNum);
        }
    }
}
