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
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;

public class DefaultClippedLineDrawer implements InterfaceClippedLineDrawer {

    /*
     * Algorithms:
     * Using Bresenham if segment is in clip,
     * and something else if there is clipping involved,
     * which might not be properly drawable with a Bresenham
     * between two pixels in clip.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private InterfaceClippedPointDrawer clippedPointDrawer;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Must configure before use.
     */
    public DefaultClippedLineDrawer() {
        this.clippedPointDrawer = null;
    }
    
    /**
     * @param clippedPointDrawer Must not be null.
     */
    public DefaultClippedLineDrawer(InterfaceClippedPointDrawer clippedPointDrawer) {
        this.clippedPointDrawer = LangUtils.requireNonNull(clippedPointDrawer);
    }
    
    /**
     * @param clippedPointDrawer Must not be null.
     */
    public void configure(InterfaceClippedPointDrawer clippedPointDrawer) {
        this.clippedPointDrawer = LangUtils.requireNonNull(clippedPointDrawer);
    }

    /*
     * Instance methods.
     */

    @Override
    public int drawHorizontalLineInClip(
            int x1, int x2, int y,
            int factor, short pattern, int pixelNum) {
        return drawHorizontalLineInClip(
                x1, x2, y,
                factor, pattern, pixelNum,
                this.clippedPointDrawer);
    }

    @Override
    public int drawVerticalLineInClip(
            int x, int y1, int y2,
            int factor, short pattern, int pixelNum) {
        return drawVerticalLineInClip(
                x, y1, y2,
                factor, pattern, pixelNum,
                this.clippedPointDrawer);
    }

    @Override
    public int drawGeneralLineInClip(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        return drawGeneralLineInClip(
                x1, y1, x2, y2,
                factor, pattern, pixelNum,
                this.clippedPointDrawer);
    }

    @Override
    public int drawGeneralLineClipped(
            GRect clip,
            int x1, int y1, int x2, int y2,
            double x1d, double y1d, double x2d, double y2d,
            int factor, short pattern, int pixelNum) {
        return drawGeneralLineClipped(
                clip,
                x1, y1, x2, y2,
                x1d, y1d, x2d, y2d,
                factor, pattern, pixelNum,
                this.clippedPointDrawer);
    }

    /*
     * Static methods.
     */

    public static int drawHorizontalLineInClip(
            int x1, int x2, int y,
            int factor, short pattern, int pixelNum,
            InterfaceClippedPointDrawer clippedPointDrawer) {

        if (DEBUG) {
            Dbg.log("drawHorizontalLineInClip("
                    + x1 + ", " + x2 + ", " + y
                    + ", " + factor + ", " + pattern + ", " + pixelNum + ",)");
        }

        final boolean mustComputePixelNum =
                GprimUtils.mustComputePixelNum(pattern);

        // Iterating from first to second point,
        // for stipple continuity.
        final int step = (x1 > x2 ? -1 : 1);

        // A for loop wouldn't work with Integer.MAX_VALUE bound.
        int x = x1;
        if (mustComputePixelNum) {
            int myPixelNum = pixelNum;
            while (true) {
                if (LineStippleUtils.mustDraw(factor, pattern, myPixelNum)) {
                    clippedPointDrawer.drawPointInClip(x, y);
                }
                myPixelNum++;
                if (x == x2) {
                    break;
                }
                x += step;
            }
            final long segmentPixelLength =
                    GprimUtils.computeHorVerSegmentPixelLength(x1, x2);
            pixelNum = GprimUtils.pixelNumPlusLongAmountNormalized(
                    factor, pixelNum,
                    segmentPixelLength);
        } else {
            while (true) {
                clippedPointDrawer.drawPointInClip(x, y);
                if (x == x2) {
                    break;
                }
                x += step;
            }
        }

        return pixelNum;
    }

    public static int drawVerticalLineInClip(
            int x, int y1, int y2,
            int factor, short pattern, int pixelNum,
            InterfaceClippedPointDrawer clippedPointDrawer) {

        if (DEBUG) {
            Dbg.log("drawVerticalLineInClip("
                    + x + ", " + y1 + ", " + y2
                    + ", " + factor + ", " + pattern + ", " + pixelNum + ",)");
        }

        final boolean mustComputePixelNum =
                GprimUtils.mustComputePixelNum(pattern);

        // Iterating from first to second point,
        // for stipple continuity.
        final int step = (y1 > y2 ? -1 : 1);

        // A for loop wouldn't work with Integer.MAX_VALUE bound.
        int y = y1;
        if (mustComputePixelNum) {
            int myPixelNum = pixelNum;
            while (true) {
                if (LineStippleUtils.mustDraw(factor, pattern, myPixelNum)) {
                    clippedPointDrawer.drawPointInClip(x, y);
                }
                myPixelNum++;
                if (y == y2) {
                    break;
                }
                y += step;
            }
            
            final long segmentPixelLength =
                    GprimUtils.computeHorVerSegmentPixelLength(y1, y2);
            pixelNum = GprimUtils.pixelNumPlusLongAmountNormalized(
                    factor, pixelNum,
                    segmentPixelLength);
        } else {
            while (true) {
                clippedPointDrawer.drawPointInClip(x, y);
                if (y == y2) {
                    break;
                }
                y += step;
            }
        }

        return pixelNum;
    }

    public static int drawGeneralLineInClip(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum,
            InterfaceClippedPointDrawer clippedPointDrawer) {

        if (DEBUG) {
            Dbg.log("drawGeneralLineInClip("
                    + x1 + ", " + y1 + ", " + x2 + ", " + y2
                    + ", " + factor + ", " + pattern + ", " + pixelNum + ",)");
        }

        // Clip span is at most Integer.MAX_VALUE,
        // so these delta can't be Integer.MIN_VALUE,
        // so we can negate them safely.
        int dx = x2 - x1;
        int dy = y2 - y1;

        final int xStep;
        final int yStep;
        if (dx < 0) {
            dx = -dx;
            xStep = -1;
        } else {
            xStep = 1;
        }
        if (dy < 0) {
            dy = -dy;
            yStep = -1;
        } else {
            yStep = 1;
        }

        int x = x1;
        int y = y1;

        final boolean mustComputePixelNum =
                GprimUtils.mustComputePixelNum(pattern);
        int myPixelNum = pixelNum;

        if ((!mustComputePixelNum)
                || LineStippleUtils.mustDraw(
                factor, pattern, myPixelNum)) {
            clippedPointDrawer.drawPointInClip(x, y);
        }
        myPixelNum++;
        
        final int tdx = dx + dx;
        final int tdy = dy + dy;

        if (dx > dy)  {
            int fraction = tdy - dx;
            while (x != x2) {
                if (fraction >= 0) {
                    y += yStep;
                    fraction -= tdx;
                }
                x += xStep;
                fraction += tdy;
                if ((!mustComputePixelNum)
                        || LineStippleUtils.mustDraw(
                        factor, pattern, myPixelNum)) {
                    clippedPointDrawer.drawPointInClip(x, y);
                }
                myPixelNum++;
            }
        } else {
            int fraction = tdx - dy;
            while (y != y2) {
                if (fraction >= 0) {
                    x += xStep;
                    fraction -= tdy;
                }
                y += yStep;
                fraction += tdx;
                if ((!mustComputePixelNum)
                        || LineStippleUtils.mustDraw(
                        factor, pattern, myPixelNum)) {
                    clippedPointDrawer.drawPointInClip(x, y);
                }
                myPixelNum++;
            }
        }

        if (mustComputePixelNum) {
            pixelNum = GprimUtils.pixelNumPlusSegmentPixelLengthNormalized(
                    x1, y1, x2, y2,
                    factor, pixelNum);
        }
        
        return pixelNum;
    }

    public static int drawGeneralLineClipped(
            GRect clip,
            int x1, int y1, int x2, int y2,
            double x1d, double y1d, double x2d, double y2d,
            int factor, short pattern, int pixelNum,
            InterfaceClippedPointDrawer clippedPointDrawer) {

        if (DEBUG) {
            Dbg.log("drawGeneralLineClipped("
                    + clip
                    + ", " + x1 + ", " + y1 + ", " + x2 + ", " + y2
                    + ", x1d, y1d, x2d, y2d"
                    + ", " + factor + ", " + pattern + ", " + pixelNum + ",)");
            Dbg.log("x1d (1) = " + NumbersUtils.toStringNoCSN(x1d));
            Dbg.log("y1d (1) = " + NumbersUtils.toStringNoCSN(y1d));
            Dbg.log("x2d (1) = " + NumbersUtils.toStringNoCSN(x2d));
            Dbg.log("y2d (1) = " + NumbersUtils.toStringNoCSN(y2d));
        }

        /*
         * Making sure x1 <= x2.
         */
        
        final boolean p1p2Swap = (x1d > x2d);
        if (DEBUG) {
            Dbg.log("p1p2Swap = " + p1p2Swap);
        }
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

        if (DEBUG) {
            Dbg.log("x1d (2) = " + NumbersUtils.toStringNoCSN(x1d));
            Dbg.log("y1d (2) = " + NumbersUtils.toStringNoCSN(y1d));
            Dbg.log("x2d (2) = " + NumbersUtils.toStringNoCSN(x2d));
            Dbg.log("y2d (2) = " + NumbersUtils.toStringNoCSN(y2d));
        }
        final int roundedX1 = BindingCoordsUtils.roundToInt(x1d);
        final int roundedY1 = BindingCoordsUtils.roundToInt(y1d);
        final int roundedX2 = BindingCoordsUtils.roundToInt(x2d);
        final int roundedY2 = BindingCoordsUtils.roundToInt(y2d);
        if (DEBUG) {
            Dbg.log("roundedX1 = " + roundedX1);
            Dbg.log("roundedY1 = " + roundedY1);
            Dbg.log("roundedX2 = " + roundedX2);
            Dbg.log("roundedY2 = " + roundedY2);
        }

        // Is >= 0.
        final double dxd = x2d - x1d;
        // Can be negative.
        final double dyd = y2d - y1d;

        final double absDyd = Math.abs(dyd);

        final boolean mustIterateOnX = (absDyd < dxd);
        if (DEBUG) {
            Dbg.log("mustIterateOnX = " + mustIterateOnX);
        }
        
        /*
         * Pixel num stuffs.
         */

        final boolean mustComputePixelNum =
                GprimUtils.mustComputePixelNum(pattern);

        final long segmentPixelLength;
        final int myInitialPixelNum;
        if (mustComputePixelNum) {
            segmentPixelLength =
                    GprimUtils.computeSegmentPixelLength(
                            x1, y1, x2, y2);
            if (p1p2Swap) {
                /*
                 * We will start from last pixelNum,
                 * and pixelNum needs to decrease as we draw.
                 */
                myInitialPixelNum = GprimUtils.pixelNumPlusLongAmountNormalized(
                        factor,
                        pixelNum,
                        segmentPixelLength - 1);
            } else {
                /*
                 * We will start from first pixelNum,
                 * and pixelNum needs to increase as we draw.
                 */
                myInitialPixelNum = pixelNum;
            }
            if (DEBUG) {
                Dbg.log("myInitialPixelNum = " + myInitialPixelNum);
            }
        } else {
            // Not used.
            segmentPixelLength = 0;
            myInitialPixelNum = pixelNum;
        }

        // Both values initialized when reaching first pixel in clip.
        int myPixelNum = Integer.MIN_VALUE;
        int myPixelNumStep = 0;
        
        /*
         * 
         */
        
        if (mustIterateOnX) {
            /*
             * Will iterate on x.
             */
            // y = a * x + b
            final double a = (y2d - y1d) / (x2d - x1d);
            final double b = y1d - a * x1d;
            if (DEBUG) {
                Dbg.log("a = " + a);
                Dbg.log("b = " + b);
            }

            final int yMin = Math.min(roundedY1, roundedY2);
            final int yMax = Math.max(roundedY1, roundedY2);

            // A for loop wouldn't work with Integer.MAX_VALUE bound.
            int x = roundedX1;
            while (true) {
                // TODO optim Could compute next x where y changes,
                // and draw horizontal pixels fast until then,
                // and do min/max checks in specific loops.
                final double yd = a * x + b;
                final int y = BindingCoordsUtils.roundToInt(yd);
                if (DEBUG) {
                    Dbg.log("loop (1) : y = " + y + ", check against [" + yMin + ", " + yMax + "]");
                }
                if ((y >= yMin) && (y <= yMax)) {
                    if (mustComputePixelNum) {
                        if (myPixelNumStep == 0) {
                            myPixelNumStep = (p1p2Swap ? -1 : 1);
                            final int myPixelNumFactor = myPixelNumStep;
                            myPixelNum = myInitialPixelNum + myPixelNumFactor * (x - x1);
                        }
                    }
                    final boolean mustDraw =
                            (!mustComputePixelNum)
                            || LineStippleUtils.mustDraw(
                                    factor, pattern, myPixelNum);
                    if (DEBUG) {
                        Dbg.log("(loop 1) (" + x + ", " + y
                                + " from " + NumbersUtils.toStringNoCSN(yd)
                                + "), pixelNum = " + myPixelNum
                                + " : mustDraw = " + mustDraw);
                    }
                    myPixelNum += myPixelNumStep;
                    if (DEBUG) {
                        Dbg.log("pixelNum updated to " + myPixelNum);
                    }
                    if (mustDraw) {
                        clippedPointDrawer.drawPointInClip(x, y);
                    }
                }
                if (x == roundedX2) {
                    break;
                }
                x++;
            }
        } else {
            /*
             * Will iterate on y.
             * Same formula, invert x and y.
             */
            final double aa = (x2d - x1d) / (y2d - y1d);
            final double bb = x1d - aa * y1d;
            if (DEBUG) {
                Dbg.log("aa (act,draw) = " + aa);
                Dbg.log("bb (act,draw) = " + bb);
            }

            final int yStep = ((dyd < 0.0) ? -1 : 1);
            
            // A for loop wouldn't work with Integer.MIN_VALUE
            // or Integer.MAX_VALUE bound.
            int y = roundedY1;
            while (true) {
                final double xd = aa * y + bb;
                final int x = BindingCoordsUtils.roundToInt(xd);
                if (DEBUG) {
                    Dbg.log("loop (2) : x = " + x + ", check against [" + roundedX1 + ", " + roundedX2 + "]");
                }
                if ((x >= roundedX1) && (x <= roundedX2)) {
                    if (mustComputePixelNum) {
                        if (myPixelNumStep == 0) {
                            myPixelNumStep = (p1p2Swap ? -1 : 1);
                            final int myPixelNumFactor = myPixelNumStep * yStep;
                            myPixelNum = myInitialPixelNum + myPixelNumFactor * (y - y1);
                        }
                    }
                    final boolean mustDraw =
                            (!mustComputePixelNum)
                            || LineStippleUtils.mustDraw(
                                    factor, pattern, myPixelNum);
                    if (DEBUG) {
                        Dbg.log("(loop 2) (" + x + " from " + NumbersUtils.toStringNoCSN(xd)
                                + ", " + y
                                + "), pixelNum = " + myPixelNum
                                + " : mustDraw = " + mustDraw);
                    }
                    if (mustDraw) {
                        clippedPointDrawer.drawPointInClip(x, y);
                    }
                    myPixelNum += myPixelNumStep;
                    if (DEBUG) {
                        Dbg.log("pixelNum updated to " + myPixelNum);
                    }
                }
                if (y == roundedY2) {
                    break;
                }
                y += yStep;
            }
        }

        if (mustComputePixelNum) {
            pixelNum = GprimUtils.pixelNumPlusLongAmountNormalized(
                    factor,
                    pixelNum,
                    segmentPixelLength);
        }

        return pixelNum;
    }
}
