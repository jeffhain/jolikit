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
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * Utilities for graphical primitives.
 */
public class GprimUtils {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * NB: Tests pass even if using 0 here,
     * but we prefer to have some tolerance.
     */
    private static final double MAG_REL_EPS_EPSILON = 1e-15;
    
    /**
     * Area threshold for oval bounding box, below which we don't bother
     * to accurately compute whether drawing or filling an oval might
     * touch pixels in the clip.
     *  
     * If too large, we might take a lot of time drawing or filling
     * large arcs or ovals that are entirely outside of clip.
     * If too small, we might take more time deciding whether
     * arcs or ovals must be drawn or filled, that it would take
     * to just draw or fill them, whether or not they are out of clip.
     * 
     * A simple worst-case bench show that mustDrawOvalOrArc(...)
     * accurate computation can go up to around 0.3 second for 1 million calls,
     * and that mustFillOvalOrArc(...) is about twice faster.
     * That's 0.3 millisecond per call.
     * Assuming each pixel to draw would take on the order of 100 cycles
     * to compute where to draw it and then draw it,
     * with 3 billion cycles per second, it would take 33 nanoseconds
     * to fill an oval of 1 pixel, 0.33 millisecond = 330_000 nanoseconds
     * to fill an oval of 10_000 pixels.
     * 
     * NB: Being one or two orders of magnitude wrong on this constant
     * shouldn't hurt much anyway.
     */
    static final double OVAL_ACCURATE_MUST_DRAW_COMPUTATION_THRESHOLD_AREA = 10 * 1000;

    /**
     * Tolerance to approximate angles very close to 360, as being 360,
     * so that we can optimize by going full oval by just checking
     * "spanDeg == 360.0" if all oval pixels are to be painted.
     * 
     * Also used as span to use instead of tiny non-zero spans,
     * to avoid underflow which would not allow to discriminate
     * tiny from zero.
     * 
     * Idea: "toleranceRad * Integer.MAX_VALUE <= 1 pixel".
     */
    static final double SPAN_TOL_DEG = Math.toDegrees(1.0/Integer.MAX_VALUE); 
    
    /*
     * 
     */
    
    /**
     * To make sure isCw(...) and isCcw(...) return rather true than false
     * in tight cases or in case of rounding errors.
     * 
     * Addition tolerance: useful when values to compare
     * are of small magnitude.
     */
    private static final double CW_CCW_TOL_ADD = 1e-15;
    
    /**
     * To make sure isCw(...) and isCcw(...) return rather true than false
     * in tight cases or in case of rounding errors.
     * 
     * Multiplication tolerance: useful when values to compare
     * are of large magnitude.
     */
    private static final double CW_CCW_TOL_MUL = 1e-15;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int PATTERN_BIT_MASK_INT = 0xFFFF;
    
    /**
     * All bits at 1.
     */
    public static final short PLAIN_PATTERN = (short) PATTERN_BIT_MASK_INT;
    
    private static final int PATTERNS_COUNT = (1 << 16);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static long computeSegmentPixelLength(
            int x1, int y1, int x2, int y2) {
        final long xLength = 1 + computeAbsDelta(x1, x2);
        final long yLength = 1 + computeAbsDelta(y1, y2);
        return Math.max(xLength, yLength);
    }

    public static long computeHorVerSegmentPixelLength(
            int c1, int c2) {
        return 1 + computeAbsDelta(c1, c2);
    }

    public static void checkFactorAndPixelNum(int factor, int pixelNum) {
        NbrsUtils.requireInRange(1, 256, factor, "factor");
        NbrsUtils.requireSupOrEq(0, pixelNum, "pixelNum");
    }

    public static void checkArcAngles(double startDeg, double spanDeg) {
        if (NbrsUtils.isNaNOrInfinite(startDeg)) {
            throw new IllegalArgumentException("startDeg [" + startDeg + "] must not be NaN of +-Infinity");
        }
        if (NbrsUtils.isNaNOrInfinite(spanDeg)) {
            throw new IllegalArgumentException("spanDeg [" + spanDeg + "] must not be NaN of +-Infinity");
        }
    }
    
    public static void checkPolyArgs(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        /*
         * Not bothering to check pointCount
         * against arrays length, if any issue
         * it will throw while iterating on points.
         */
        LangUtils.requireNonNull(xArr);
        LangUtils.requireNonNull(yArr);
        NbrsUtils.requireSupOrEq(0, pointCount, "pointCount");
    }
    
    /*
     * 
     */
    
    public static GRect computePolyBoundingBox(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        if (pointCount <= 0) {
            return GRect.DEFAULT_EMPTY;
        }
        int xMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;
        for (int i = 0; i < pointCount; i++) {
            final int x = xArr[i];
            final int y = yArr[i];
            xMin = Math.min(xMin, x);
            xMax = Math.max(xMax, x);
            yMin = Math.min(yMin, y);
            yMax = Math.max(yMax, y);
        }
        final int xSpan = xMax - xMin + 1;
        final int ySpan = yMax - yMin + 1;
        return GRect.valueOf(
                xMin,
                yMin,
                xSpan,
                ySpan);
    }

    /*
     * 
     */
    
    /**
     * @param angDeg Angle in degrees.
     * @return Angle in degrees, normalized into [-180.0,180.0[.
     */
    public static double normalize_neg180_180(double angDeg) {
        
        /*
         * Only using "%" if magnitude is above the usual case
         * of operations with angles in +-360 deg,
         * for it can be slow.
         */
        
        if ((angDeg < -720.0) || (angDeg > 720.0)) {
            angDeg %= 360.0;
        }
        
        /*
         * 
         */
        
        while (angDeg < -180.0) {
            angDeg += 360.0;
        }
        
        /*
         * Even if we always use "% 360.0" initially,
         * "== 180.0" Can happen due to precision loss,
         * after above "+=",
         * when input is of small magnitude.
         */
        
        while (angDeg >= 180.0) {
            angDeg -= 360.0;
        }
        
        return angDeg;
    }
    
    /**
     * @param angDeg Angle in degrees.
     * @return Angle in degrees, normalized into [0.0,360.0[.
     */
    public static double normalize_0_360(double angDeg) {
        
        /*
         * Only using "%" if magnitude is above the usual case
         * of operations with angles in +-360 deg,
         * for it can be slow.
         */
        
        if ((angDeg < -720.0) || (angDeg > 720.0)) {
            angDeg %= 360.0;
        }
        
        /*
         * 
         */
        
        while (angDeg < 0.0) {
            angDeg += 360.0;
        }
        
        /*
         * Even if we always use "% 360.0" initially,
         * "== 360.0" Can happen due to precision loss,
         * after above "+=",
         * when input is of small magnitude.
         */
        
        while (angDeg >= 360.0) {
            angDeg -= 360.0;
        }
        
        return angDeg;
    }
    
    /*
     * 
     */
    
    /**
     * @param startDeg Start angle in degrees.
     * @return Start angle in degrees, normalized into [0.0,360.0[.
     */
    public static double computeNormalizedStartDeg(double startDeg) {
        return normalize_0_360(startDeg);
    }
    
    /**
     * @param spanDeg Span in degrees.
     * @return Span in degrees, clamped into [-360.0,360.0].
     */
    public static double computeClampedSpanDeg(double spanDeg) {
        return NbrsUtils.toRange(-360.0, 360.0, spanDeg);
    }
    
    /**
     * Angle rework to have span in [0,360] when dealing with arcs.
     * 
     * @param normalizedStartDeg Start angle in degrees, in [0.0,360.0[.
     * @param clampedSpanDeg Span in degrees, in [-360.0,360.0].
     * @return Reworked start angle in degrees, in [0.0,360.0[,
     *         to be used along with reworked span.
     */
    public static double computeReworkedStartDeg(
            double normalizedStartDeg,
            double clampedSpanDeg) {
        double reworkedStartDeg = normalizedStartDeg;
        if (clampedSpanDeg < 0.0) {
            reworkedStartDeg += Math.min(clampedSpanDeg, -SPAN_TOL_DEG);
            // Taking back into [0.0,360.0[ if needed.
            reworkedStartDeg = computeNormalizedStartDeg(reworkedStartDeg);
        }
        return reworkedStartDeg;
    }

    /**
     * Angle rework to have span in [0,360] when dealing with arcs.
     * 
     * @param clampedSpanDeg Span in degrees, in [-360.0,360.0].
     * @return Reworked span in degrees, in [0.0,360.0],
     *         to be used along with reworked start angle.
     */
    public static double computeReworkedSpanDeg(double clampedSpanDeg) {
        double reworkedSpanDeg =  Math.abs(clampedSpanDeg);
        if (reworkedSpanDeg >= (360.0 - SPAN_TOL_DEG)) {
            reworkedSpanDeg = 360.0;
        } else if ((reworkedSpanDeg != 0.0)
                && (reworkedSpanDeg < SPAN_TOL_DEG)) {
            reworkedSpanDeg = SPAN_TOL_DEG;
        }
        return reworkedSpanDeg;
    }
    
    /*
     * 
     */

    public static double scalarProduct(
            double v1x,
            double v1y,
            double v2x,
            double v2y) {
        return v1x * v2x + v1y * v2y;
    }

    /**
     * @return True if (v1,v2) is clockwise on screen, or is straight,
     *         false otherwise.
     */
    public static boolean isCw(
            double v1x,
            double v1y,
            double v2x,
            double v2y) {
        if (false) {
            return v1x * v2y > v1y * v2x;
        }
        /*
         * (O,A,B) clockwise:
         * det([[xB-xA, xC-xB],
         *      [yB-yA, yC-yB]]) < 0
         * <=>
         * (xB-xA)*(yC-yB) < (yB-yA)*(xC-xB)
         * 
         * with
         * A = O (origin, (0,0)),
         * (O,B) = v1,
         * (O,C) = v2,
         * v1x*(v2y-v1y) < v1y*(v2x-v1x).
         */
        final double t1 = v1x * (v2y - v1y);
        final double t2 = v1y * (v2x - v1x);
        final double tolFactor = ((t1 > 0.0) ? 1 + CW_CCW_TOL_MUL : 1 - CW_CCW_TOL_MUL);
        final boolean res = (t1 * tolFactor + CW_CCW_TOL_ADD >= t2);
        if (DEBUG) {
            Dbg.log("isCw(" + v1x + ", " + v1y + ", " + v2x + ", " + v2y + ") = " + res);
        }
        return res;
    }

    /**
     * @return True if (v1,v2) is counter-clockwise on screen, or is straight,
     *         false otherwise.
     */
    public static boolean isCcw(
            double v1x,
            double v1y,
            double v2x,
            double v2y) {
        if (false) {
            return v1x * v2y < v1y * v2x;
        }
        final double t1 = v1x * (v2y - v1y);
        final double t2 = v1y * (v2x - v1x);
        final double tolFactor = ((t2 > 0.0) ? 1 + CW_CCW_TOL_MUL : 1 - CW_CCW_TOL_MUL);
        final boolean res = (t2 * tolFactor + CW_CCW_TOL_ADD >= t1);
        if (DEBUG) {
            Dbg.log("isCcw(" + v1x + ", " + v1y + ", " + v2x + ", " + v2y + ") = " + res);
        }
        return res;
    }
    
    /**
     * @param spanDeg Span in degrees from (v1x,v1y) to (v2x,v2y).
     *        Must be in [0.0,360.0].
     * @return True if (vx,vy) is in then angular range from
     *         (v1x,v1y) to (v2x,v2y).
     */
    public static boolean isInAngularRange(
            double vx, double vy,
            double spanDeg,
            double v1x, double v1y,
            double v2x, double v2y) {
        final boolean res;
        if (spanDeg <= 180.0) {
            res = isCcw(v1x, v1y, vx, vy)
                    && isCw(v2x, v2y, vx, vy);
        } else {
            res = isCcw(v1x, v1y, vx, vy)
                    || isCw(v2x, v2y, vx, vy);
        }
        return res;
    }
    
    /*
     * 
     */
    
    public static double sinDeg(double angDeg) {
        /*
         * Optimization for values we use for oval.
         */
        
        if (angDeg == 0.0) {
            return angDeg;
        }
        if (angDeg == 360.0) {
            return 0.0;
        }
        
        /*
         * 
         */
        
        if (Math.abs(angDeg) >= 360.0) {
            angDeg %= 360;
        }
        
        if (angDeg == -180.0) {
            // Exact.
            return -0.0;
        }
        if (angDeg == 180.0) {
            // Exact.
            return 0.0;
        }
        
        return Math.sin(angDeg * (Math.PI/180.0));
    }

    public static double cosDeg(double angDeg) {
        /*
         * Optimization for values we use for oval.
         */
        
        if ((angDeg == 0.0)
                || (angDeg == 360.0)) {
            return 1.0;
        }
        
        /*
         * 
         */
        
        if (Math.abs(angDeg) >= 360.0) {
            angDeg %= 360;
        }
        
        if ((angDeg == -90.0)
                || (angDeg == 90.0)
                || (angDeg == -270.0)
                || (angDeg == 270.0)) {
            // Exact.
            return 0.0;
        }
        
        return Math.cos(angDeg * (Math.PI/180.0));
    }
    
    /*
     * 
     */
    
    /**
     * For non-plain patterns, pixel num must not be computed,
     * which is made explicit in the code by this method
     * not just being called "isPatternPlain".
     * 
     * @return True if the specified pattern is not plain
     *         (i.e. if it doesn't only contain 1-bits).
     */
    public static boolean mustComputePixelNum(short pattern) {
        return (pattern != PLAIN_PATTERN);
    }
    
    /**
     * For int pixelNum.
     * 
     * @param factor Must be in [1,256].
     * @param pixelNumLong Must be >= 0.
     * @return pixelNum normalized in [0, factor * 2^16[.
     */
    public static int pixelNumNormalized(int factor, int pixelNum) {
        return (pixelNum % (factor * PATTERNS_COUNT));
    }
    
    /**
     * For long pixelNum.
     * 
     * @param factor Must be in [1,256].
     * @param pixelNumLong Must be >= 0.
     * @return pixelNum normalized in [0, factor * 2^16[.
     */
    public static int pixelNumNormalizedLong(int factor, long pixelNumLong) {
        return (int) (pixelNumLong % (factor * PATTERNS_COUNT));
    }
    
    /**
     * For long amount.
     * 
     * @param factor Must be in [1,256].
     * @param pixelNum Must be >= 0.
     * @param amount Must be >= 0.
     * @return (pixelNum + amount) normalized in [0, factor * 2^16[.
     */
    public static int pixelNumPlusLongAmountNormalized(int factor, int pixelNum, long amount) {
        final long pixelNumLong = pixelNum + amount;
        return pixelNumNormalizedLong(factor, pixelNumLong);
    }

    /**
     * Convenience method when advancing pixelNum
     * according to segment length at once.
     * 
     * @param factor Must be in [1,256].
     * @param pixelNum Must be >= 0.
     * @return (pixelNum + (segment pixel length)) normalized in [0, factor * 2^16[.
     */
    public static int pixelNumPlusSegmentPixelLengthNormalized(
            int x1, int y1, int x2, int y2,
            int factor,
            int pixelNum) {
        final long pixelLength = computeSegmentPixelLength(x1, y1, x2, y2);
        return pixelNumPlusLongAmountNormalized(factor, pixelNum, pixelLength);
    }

    /*
     * Line.
     */
    
    /**
     * Line equation:
     * y = a * x + b
     * 
     * a:
     * y1 = a * x1 + b
     * y2 = a * x2 + b
     * y2 - y1 = a * (x2 - x1)
     * a = dy/dx
     * 
     * +-0.0 if segment is horizontal.
     * +-Infinity if segment is vertical.
     * NaN if segment is a point (already taken care of).
     */
    public static double computeLineA(double x1d, double y1d, double x2d, double y2d) {
        // double already, to avoid overflow.
        final double lineDxd = x2d - x1d;
        final double lineDyd = y2d - y1d;
        final double a = lineDyd / lineDxd;
        return a;
    }
    
    /**
     * Line equation:
     * y = a * x + b
     * 
     * b:
     * b = y - a * x
     * 
     * y if segment is horizontal.
     * +-Infinity or NaN (if x = 0) if segment is vertical.
     * NaN if segment is a point (already taken care of).
     */
    public static double computeLineB(double x1d, double y1d, double x2d, double y2d, double a) {
        // Using the x with lowest absolute value,
        // to minimize the impact of the error
        // coming from "(dy/dx) * x" accuracy loss.
        final double b;
        if (Math.abs(x1d) < Math.abs(x2d)) {
            b = y1d - a * x1d;
        } else {
            b = y2d - a * x2d;
        }
        return b;
    }
    
    /**
     * @return Constant x or the line if both x positions are equal,
     *         NaN otherwise.
     */
    public static double computeLineC(double x1d, double x2d) {
        final double c;
        if (x1d == x2d) {
            c = x1d;
        } else {
            c = Double.NaN;
        }
        return c;
    }
    
    /*
     * 
     */
    
    /**
     * If the specified oval is not large enough, it's not worth to do accurate
     * computations, so in this case this method just returns whether
     * oval bounding box and the specified rectangle overlap.
     * 
     * @return True if drawing the specified oval might concern pixels
     *         that are in the specified clip. 
     */
    public static boolean mustDrawOvalOrArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan) {

        final boolean mustDoAccurateComputation =
                (xSpan * (double) ySpan > OVAL_ACCURATE_MUST_DRAW_COMPUTATION_THRESHOLD_AREA);
        
        final int clipX = clip.x();
        final int clipY = clip.y();
        final int clipXSpan = clip.xSpan();
        final int clipYSpan = clip.ySpan();
        
        final boolean mustDraw;
        if (mustDoAccurateComputation) {
            final double margin = 1.0;
            // First check: case of ovals out of clip
            // (the most common not-to-draw case for non-huge ovals).
            final boolean rectIsOut = computeIsRectOutOfOval(
                    x, y, xSpan, ySpan,
                    clipX, clipY, clipXSpan, clipYSpan,
                    margin);
            if (rectIsOut) {
                mustDraw = false;
            } else {
                // Second check: case of ovals around clip.
                final boolean rectIsIn = computeIsRectInOval(
                        x, y, xSpan, ySpan,
                        clipX, clipY, clipXSpan, clipYSpan,
                        margin);
                if (rectIsIn) {
                    mustDraw = false;
                } else {
                    mustDraw = true;
                }
            }
        } else {
            final boolean overlap = GRect.overlap(
                    x, y, xSpan, ySpan,
                    clipX, clipY, clipXSpan, clipYSpan);
            mustDraw = overlap;
        }
        return mustDraw;
    }

    /**
     * If the specified oval is not large enough, it's not worth to do accurate
     * computations, so in this case this method just returns whether
     * oval bounding box and the specified rectangle overlap.
     * 
     * @return True if filling the specified oval might concern pixels
     *         that are in the specified clip. 
     */
    public static boolean mustFillOvalOrArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan) {
        
        final boolean mustDoAccurateComputation =
                (xSpan * (double) ySpan > OVAL_ACCURATE_MUST_DRAW_COMPUTATION_THRESHOLD_AREA);
        
        final int clipX = clip.x();
        final int clipY = clip.y();
        final int clipXSpan = clip.xSpan();
        final int clipYSpan = clip.ySpan();
        
        final boolean mustFill;
        if (mustDoAccurateComputation) {
            final double margin = 1.0;
            final boolean rectIsOut = computeIsRectOutOfOval(
                    x, y, xSpan, ySpan,
                    clipX, clipY, clipXSpan, clipYSpan,
                    margin);
            if (rectIsOut) {
                mustFill = false;
            } else {
                mustFill = true;
            }
        } else {
            final boolean overlap = GRect.overlap(
                    x, y, xSpan, ySpan,
                    clipX, clipY, clipXSpan, clipYSpan);
            mustFill = overlap;
        }
        return mustFill;
    }
    
    public static boolean isClipInOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan) {
        final double margin = 1.0;
        return computeIsRectInOval(
                x, y, xSpan, ySpan,
                clip.x(), clip.y(), clip.xSpan(), clip.ySpan(),
                margin);
    }

    /*
     * 
     */
    
    /**
     * If the oval must be filled, and using a margin of zero,
     * then if this method returns true, all the pixels of
     * the specified rectangle must be filled.
     * 
     * If the oval must be drawn, and using a margin of one,
     * then if this method returns true, none of the pixels of
     * the specified rectangle must be drawn.
     * 
     * @param margin Used to enlarge the specified rectangle
     *        before non-exact checks.
     * @return True if the specified rectangle, enlarged by the specified margin,
     *         lies within the area of the specified oval, false otherwise.
     */
    public static boolean computeIsRectInOval(
            int ox, int oy, int oxSpan, int oySpan,
            int x, int y, int xSpan, int ySpan,
            double margin) {
        
        /*
         * Oval equation (x and y being counted from center):
         * x = rx * cos(ang)
         * y = ry * sin(ang)
         * 
         * cos(ang) = x/rx
         * sin(ang) = y/ry
         * 
         * tan(ang) = (y/ry) / (x/rx)
         * tan(ang) = (y*rx) / (x*ry)
         * ang = atan2(y * rx, x * ry);
         */
        
        // Oval x and y radius.
        final double orx = (oxSpan - 1) * 0.5;
        final double ory = (oySpan - 1) * 0.5;
        // Oval center.
        final double ocx = ox + orx;
        final double ocy = oy + ory;
        
        final int xMax = x + xSpan - 1;
        final int yMax = y + ySpan - 1;
        
        final double p1dx = (x - margin) - ocx;
        final double p1dy = (y - margin) - ocy;
        final boolean p1In = isPointInOval(ocx, ocy, orx, ory, p1dx, p1dy);
        if (!p1In) {
            if (DEBUG) {
                System.out.println("p1 is out");
            }
            return false;
        }
        
        final double p2dx = (xMax + margin) - ocx;
        final double p2dy = (y - margin) - ocy;
        final boolean p2In = isPointInOval(ocx, ocy, orx, ory, p2dx, p2dy);
        if (!p2In) {
            if (DEBUG) {
                System.out.println("p2 is out");
            }
            return false;
        }
        
        final double p3dx = (xMax + margin) - ocx;
        final double p3dy = (yMax + margin) - ocy;
        final boolean p3In = isPointInOval(ocx, ocy, orx, ory, p3dx, p3dy);
        if (!p3In) {
            if (DEBUG) {
                System.out.println("p3 is out");
            }
            return false;
        }
        
        final double p4dx = (x - margin) - ocx;
        final double p4dy = (yMax + margin) - ocy;
        final boolean p4In = isPointInOval(ocx, ocy, orx, ory, p4dx, p4dy);
        if (!p4In) {
            if (DEBUG) {
                System.out.println("p4 is out");
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * If the oval must be drawn or filled, and using a margin of one,
     * then if this method returns true, none of the pixels of
     * the specified rectangle must be drawn.
     * 
     * @param margin Used to enlarge the specified rectangle
     *        before non-exact checks.
     * @return True if the specified rectangle, enlarged by the specified margin,
     *         lies outside of the area of the specified oval, false otherwise.
     */
    public static boolean computeIsRectOutOfOval(
            int ox, int oy, int oxSpan, int oySpan,
            int x, int y, int xSpan, int ySpan,
            double margin) {
        
        if (!GRect.overlap(
                ox, oy, oxSpan, oySpan,
                x, y, xSpan, ySpan)) {
            if (DEBUG) {
                System.out.println("dont overlap");
            }
            return true;
        }
        
        /*
         * Oval bounding box and rectangle overlap.
         * Rectangle is out only if all of its corners
         * are in a same quadrant of the oval,
         * and its corners closest to oval center
         * is not in the oval.
         */
        
        /*
         * Oval equation (x and y being counted from center):
         * x = rx * cos(ang)
         * y = ry * sin(ang)
         * 
         * cos(ang) = x/rx
         * sin(ang) = y/ry
         * 
         * tan(ang) = (y/ry) / (x/rx)
         * tan(ang) = (y*rx) / (x*ry)
         * ang = atan2(y * rx, x * ry);
         */
        
        // Oval x and y radius.
        final double orx = (oxSpan - 1) * 0.5;
        final double ory = (oySpan - 1) * 0.5;
        
        // Oval center.
        final double ocx = ox + orx;
        final double ocy = oy + ory;
        
        final int xMax = x + xSpan - 1;
        final int yMax = y + ySpan - 1;
        
        final double p1dx = (x - margin) - ocx;
        final double p1dy = (y - margin) - ocy;
        if ((p1dx > 0.0)
                && (p1dy > 0.0)
                && (!isPointInOval(ocx, ocy, orx, ory, p1dx, p1dy))) {
            if (DEBUG) {
                System.out.println("p1 is out");
            }
            return true;
        }
        
        final double p2dx = (xMax + margin) - ocx;
        final double p2dy = (y - margin) - ocy;
        if ((p2dx < 0.0)
                && (p2dy > 0.0)
                && (!isPointInOval(ocx, ocy, orx, ory, p2dx, p2dy))) {
            if (DEBUG) {
                System.out.println("p2 is out");
            }
            return true;
        }
        
        final double p3dx = (xMax + margin) - ocx;
        final double p3dy = (yMax + margin) - ocy;
        if ((p3dx < 0.0)
                && (p3dy < 0.0)
                && (!isPointInOval(ocx, ocy, orx, ory, p3dx, p3dy))) {
            if (DEBUG) {
                System.out.println("p3 is out");
            }
            return true;
        }
        
        final double p4dx = (x - margin) - ocx;
        final double p4dy = (yMax + margin) - ocy;
        if ((p4dx > 0.0)
                && (p4dy < 0.0)
                && (!isPointInOval(ocx, ocy, orx, ory, p4dx, p4dy))) {
            if (DEBUG) {
                System.out.println("p4 is out");
            }
            return true;
        }
        
        return false;
    }

    public static boolean isPointInOval(
            double ocx, double ocy, double orx, double ory,
            double pdx, double pdy) {
        
        final double pDistSq = distSq(pdx, pdy);
        
        final double distSqOnOval = distSqOnOval(
                orx, ory,
                pdx, pdy);

        final boolean pIsIn = (pDistSq < distSqOnOval);

        return pIsIn;
    }
    
    public static double distSqOnOval(double rx, double ry, double angRad) {
        final double xOnCurve = rx * Math.cos(angRad);
        final double yOnCurve = ry * Math.sin(angRad);
        return distSq(xOnCurve, yOnCurve);
    }
    
    public static double distSqOnOval(double rx, double ry, double pdx, double pdy) {
        final boolean isCircle = (rx == ry);
        
        final double distSqOnCurve;
        if (isCircle) {
            distSqOnCurve = NbrsUtils.pow2(rx);
        } else {
            // Not angle in the plane, but angle in formula.
            final double pAngRad = Math.atan2(pdy * rx, pdx * ry);
            distSqOnCurve = distSqOnOval(rx, ry, pAngRad);
        }
        
        return distSqOnCurve;
    }
    
    public static double distSq(double dx, double dy) {
        return dx * dx + dy * dy;
    }
    
    /*
     * 
     */
    
    /**
     * @return An epsilon relative to the magnitude of max(1,|value|).
     */
    public static double magRelEps(int value) {
        // double early to avoid issue with Integer.MIN_VALUE.
        final double vAbs = Math.max(1.0, Math.abs((double) value));
        final double eps = vAbs - vAbs * (1.0 - MAG_REL_EPS_EPSILON);
        return eps;
    }
    
    /**
     * Named after The Hateful Eights,
     * and sounds like Surrounding Heights.
     * 
     * @param xyArrForReuse Can be null. Reused as return value if has proper length,
     *        i.e. a length of 16.
     * @return An array containing coordinates of corners of the specified pixel,
     *         and of middles of its sides, with some epsilons to move them
     *         closer to pixel center to make sure they are not considered
     *         as belonging to surrounding pixels.
     */
    public static double[] computeSurroundingEightsArr(
            int x,
            int y,
            double[] xyArrForReuse) {
        
        final double xEps = magRelEps(x);
        final double yEps = magRelEps(y);
        
        final double xhme = 0.5 - xEps;
        final double yhme = 0.5 - yEps;
        
        final int length = 2 * (4 * 2);
        final double[] xyArr;
        if ((xyArrForReuse != null) && (xyArrForReuse.length == length)) {
            xyArr = xyArrForReuse;
        } else {
            xyArr = new double[length];
        }
        
        int i = 0;
        
        /*
         * Corners.
         */
        
        xyArr[i++] = x + xhme;
        xyArr[i++] = y + yhme;
        //
        xyArr[i++] = x + xhme;
        xyArr[i++] = y - yhme;
        //
        xyArr[i++] = x - xhme;
        xyArr[i++] = y + yhme;
        //
        xyArr[i++] = x - xhme;
        xyArr[i++] = y - yhme;
        
        /*
         * Sides middles.
         */
        
        xyArr[i++] = x + xhme;
        xyArr[i++] = y;
        //
        xyArr[i++] = x - xhme;
        xyArr[i++] = y;
        //
        xyArr[i++] = x;
        xyArr[i++] = y + yhme;
        //
        xyArr[i++] = x;
        xyArr[i++] = y - yhme;
        
        return xyArr;
    }
    
    /**
     * @return True if the specified AB and CD segments intersect each other,
     *         false otherwise.
     */
    public static boolean doSegmentsIntersect(
            double xA,
            double yA,
            double xB,
            double yB,
            double xC,
            double yC,
            double xD,
            double yD) {
        
        /*
         * Moving origin to (xA, yA),
         * for lower magnitudes in computations
         * (also helps for debug).
         */
        
        xB -= xA;
        yB -= yA;
        xC -= xA;
        yC -= yA;
        xD -= xA;
        yD -= yA;
        xA = 0.0;
        yA = 0.0;
        
        /*
         * I don't remember where I took it from, I think either:
         * - JTS 1.7
         * - GeoTools 2.3.5
         * - comp.graphics.algorithms Frequently Asked Questions
         */
        
        double dxAB = xB - xA;
        double dyAB = yB - yA;
        double dxCD = xD - xC;
        double dyCD = yD - yC;

        double x = dyAB * dxCD;
        double y = dxAB * dyCD;

        // Coordinates of the intersection, if any.
        x = ((yC - yA) * (dxAB * dxCD) + (x * xA) - (y * xC)) / (x - y);
        y = (Math.abs(dxCD) > Math.abs(dxAB)) ? (dyCD / dxCD) * (x - xC) + yC : (dyAB / dxAB) * (x - xA) + yA;

        if ((dxAB != 0.0)
                && !((dxAB < 0) ? ((x <= xA) && (x >= xA + dxAB)) : ((x >= xA) && (x <= xA + dxAB)))) {
            return false;
        }
        if ((dxCD != 0.0)
                && !((dxCD < 0) ? ((x <= xC) && (x >= xC + dxCD)) : ((x >= xC) && (x <= xC + dxCD)))) {
            return false;
        }
        if ((dyAB != 0.0)
                && !((dyAB < 0) ? ((y <= yA) && (y >= yA + dyAB)) : ((y >= yA) && (y <= yA + dyAB)))) {
            return false;
        }
        if ((dyCD != 0.0)
                && !((dyCD < 0) ? ((y <= yC) && (y >= yC + dyCD)) : ((y >= yC) && (y <= yC + dyCD)))) {
            return false;
        }
        
        return true;
    }

    /**
     * @param mustEnlargeElseShrinkPixel True to return true in case of ties,
     *        false to return false in case of ties.
     * @return True if the specified segment intersects a side of the specified pixel.
     */
    public static boolean doSegmentIntersectASurroundingSide(
            double x1, double y1, double x2, double y2,
            int x, int y,
            boolean mustEnlargeElseShrinkPixel) {
        
        final double xEps = magRelEps(x);
        final double yEps = magRelEps(y);
        
        final double dx;
        final double dy;
        if (mustEnlargeElseShrinkPixel) {
            dx = 0.5 + xEps;
            dy = 0.5 + yEps;
        } else {
            dx = 0.5 - xEps;
            dy = 0.5 - yEps;
        }
        
        for (int k = -1; k <= 1; k += 2) {
            {
                // left then right
                final double sx = x + k * dx;
                if (doSegmentsIntersect(
                        x1, y1, x2, y2,
                        sx, y - dy, sx, y + dy)) {
                    if (DEBUG) {
                        Dbg.log("===> segments intersect");
                    }
                    return true;
                }
            }
            {
                // bottom then top
                final double sy = y + k * dy;
                if (doSegmentsIntersect(
                        x1, y1, x2, y2,
                        x - dx, sy, x + dx, sy)) {
                    if (DEBUG) {
                        Dbg.log("===> segments intersect");
                    }
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * @param startDeg Must be in [0.0,360.0[.
     * @param spanDeg Must be in [0.0,360.0].
     */
    public static boolean isInAngularRange(
            double dx, double dy,
            double startDeg, double spanDeg,
            double sinStart, double cosStart,
            double sinEnd, double cosEnd,
            double rx, double ry) {

        if (DEBUG) {
            Dbg.log("isInAngularRange("
                    + dx + ", " + dy
                    + ", " + startDeg + ", " + spanDeg
                    + ",,,,"
                    + ", " + rx + ", " + ry
                    + ")");
        }
        
        /*
         * At this point, is oval is a pixel
         * or a horizontal or vertical line,
         * we know the pixel is in its bounding box,
         * else oval helper would have returned 0
         * and we would have returned earlier.
         */

        final boolean isIn;
        if ((rx == 0.0)
                && (ry == 0.0)) {
            // One-pixel oval.
            isIn = (spanDeg > 0.0);
            if (DEBUG) {
                Dbg.log("isIn (pixel) = " + isIn);
            }
            return isIn;
        }
        
        if (rx == 0.0) {
            // Vertical oval.
            if (dy < 0.0) {
                isIn = overlapStartSpanStartExclEndExclDeg(startDeg, spanDeg, 0.0, 180.0);
            } else if (dy > 0.0) {
                isIn = overlapStartSpanStartExclEndExclDeg(startDeg, spanDeg, 180.0, 360.0);
            } else {
                isIn = (spanDeg > 0.0);
            }
            if (DEBUG) {
                Dbg.log("isIn (vertical) = " + isIn);
            }
            return isIn;
        }
        
        if (ry == 0.0) {
            // Horizontal oval.
            if (dx < 0.0) {
                isIn = overlapStartSpanStartExclEndExclDeg(startDeg, spanDeg, 90.0, 270.0);
            } else if (dx > 0.0) {
                isIn = overlapStartSpanStartExclEndExclDeg(startDeg, spanDeg, -90.0, 90.0);
            } else {
                isIn = (spanDeg > 0.0);
            }
            if (DEBUG) {
                Dbg.log("isIn (horizontal) = " + isIn);
            }
            return isIn;
        }
        
        /*
         * Here, we got rx > 0.0 and ry > 0.0.
         */

        final boolean optim = true;
        if (optim) {
            isIn = isInAngularRange_rxyPos_fast(
                    dx, dy,
                    spanDeg,
                    sinStart, cosStart,
                    sinEnd, cosEnd,
                    rx, ry);
        } else {
            isIn = isInAngularRange_rxyPos_slow(
                    dx, dy,
                    startDeg, spanDeg,
                    rx, ry);
        }
        
        return isIn;
    }
    
    public static boolean overlapStartSpanStartEndDeg(
            double start1Deg,
            double span1Deg,
            double start2Deg,
            double end2Deg) {
        
        if (DEBUG) {
            Dbg.log("overlapStartSpanStartEndDeg("
                    + start1Deg + ", " + span1Deg
                    + ", " + start2Deg + ", " + end2Deg + ")");
        }
        
        final double end1Deg = start1Deg + span1Deg;
        final double span2Deg = normalize_0_360(end2Deg - start2Deg);
        
        if (DEBUG) {
            Dbg.log("end1Deg = " + end1Deg + ", span2Deg = " + span2Deg + ")");
        }
        
        return containsDeg(start1Deg, span1Deg, start2Deg)
                || containsDeg(start1Deg, span1Deg, end2Deg)
                || containsDeg(start2Deg, span2Deg, start1Deg)
                || containsDeg(start2Deg, span2Deg, end1Deg);
    }

    /**
     * Second domain : bounds excluded.
     * 
     * Domains must not be empty or tiny.
     */
    public static boolean overlapStartSpanStartExclEndExclDeg(
            double start1Deg,
            double span1Deg,
            double start2DegExcl,
            double end2DegExcl) {
        
        if (DEBUG) {
            Dbg.log("overlapStartSpanStartExclEndExclDeg("
                    + start1Deg + ", " + span1Deg
                    + ", " + start2DegExcl + ", " + end2DegExcl + ")");
        }
        
        // Second domain : bounds excluded.
        // Not using Math.nextAfter(...), for the ulp
        // would be too small for values close to zero.
        // Domains must not be tiny in our tests,
        // so that should not cause bounds to swap.
        double start2Deg = start2DegExcl + SPAN_TOL_DEG;
        double end2Deg = end2DegExcl - SPAN_TOL_DEG;
        
        return overlapStartSpanStartEndDeg(
                start1Deg, span1Deg,
                start2Deg, end2Deg);
    }

    /**
     * NB: For (angDeg - startDeg = 0), returns true even when spanDeg is 0.0,
     * which has therefore to be tested before.
     * 
     * @param spanDeg Must be in [0.0,360.0].
     * @return True if angDeg is in [startDeg, startDeg + spanDeg],
     *         modulo 360, false otherwise.
     */
    public static boolean containsDeg(double startDeg, double spanDeg, double angDeg) {
        if (DEBUG) {
            Dbg.log("containsDeg(" + startDeg + ", " + spanDeg + ", " + angDeg + ")");
        }
        
        /*
         * Using values of small magnitudes, for most accurate computations,
         * not to get things false in our ulp-wise unit tests.
         */
        
        startDeg = normalize_neg180_180(startDeg);
        angDeg = normalize_neg180_180(angDeg);
        if (DEBUG) {
            Dbg.log("startDeg (2) = " + startDeg + ", angDeg (2) = " + angDeg + ")");
        }
        
        final double endDeg = normalize_neg180_180(startDeg + spanDeg);
        
        final double startToAngDeltaDeg = normalize_neg180_180(angDeg - startDeg);
        final double endToAngDeltaDeg = normalize_neg180_180(angDeg - endDeg);
        
        final boolean ret;
        if (Math.abs(startToAngDeltaDeg) < Math.abs(endToAngDeltaDeg)) {
            // Will compute belonging from start.
            final double startToEndDeltaDeg = normalize_neg180_180(endDeg - startDeg);
            if (DEBUG) {
                Dbg.log("startToEndDeltaDeg = " + startToEndDeltaDeg);
            }
            if (startToEndDeltaDeg >= 0.0) {
                ret = (startToAngDeltaDeg >= 0.0) && (startToAngDeltaDeg <= startToEndDeltaDeg);
            } else {
                ret = (startToAngDeltaDeg >= 0.0) || (startToAngDeltaDeg <= startToEndDeltaDeg);
            }
        } else {
            // Will compute belonging from end.
            final double endToStartDeltaDeg = normalize_neg180_180(startDeg - endDeg);
            if (DEBUG) {
                Dbg.log("endToStartDeltaDeg = " + endToStartDeltaDeg);
            }
            if (endToStartDeltaDeg >= 0.0) {
                ret = (endToAngDeltaDeg <= 0.0) || (endToAngDeltaDeg >= endToStartDeltaDeg);
            } else {
                ret = (endToAngDeltaDeg <= 0.0) && (endToAngDeltaDeg >= endToStartDeltaDeg);
            }
        }
        
        if (DEBUG) {
            Dbg.log("ret = " + ret + " (startToAngDeltaDeg = " + startToAngDeltaDeg + ", endToAngDeltaDeg = " + endToAngDeltaDeg + ")");
        }
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Slow way (using atan2(...)).
     * 
     * @param startDeg Must be in [0.0,360.0[.
     * @param spanDeg Must be in [0.0,360.0].
     * @param rx Must be > 0.
     * @param ry Must be > 0.
     */
    static boolean isInAngularRange_rxyPos_slow(
            double dx, double dy,
            double startDeg, double spanDeg,
            double rx, double ry) {
        /*
         * Oval equation (dx and dy being counted from center):
         * dx = rx * cos(ang)
         * dy = ry * -sin(ang)
         * 
         * cos(ang) = dx/rx
         * sin(ang) = -dy/ry
         * 
         * tan(ang) = -(dy/ry) / (dx/rx)
         * tan(ang) = -(dy*rx) / (dx*ry)
         * ang = atan2(-dy * rx, dx * ry);
         */

        final double vx = dx * ry;
        final double vy = -dy * rx;
        
        // In [-180.0,180.0].
        final double angDeg = Math.atan2(vy, vx) * (180.0/Math.PI);

        // In [-180.0-360.0,180.0].
        double deltaDeg = angDeg - startDeg;

        // Normalizing delta in [0.0,360.0].
        while (deltaDeg < 0.0) {
            deltaDeg += 360.0;
        }

        final boolean isIn = (deltaDeg <= spanDeg);
        return isIn;
    }
    
    /**
     * Optimized way.
     * 
     * @param spanDeg Must be in [0.0,360.0].
     * @param rx Must be > 0.
     * @param ry Must be > 0.
     */
    static boolean isInAngularRange_rxyPos_fast(
            double dx, double dy,
            double spanDeg,
            double sinStart, double cosStart,
            double sinEnd, double cosEnd,
            double rx, double ry) {
        /*
         * dx = rx * cos(ang)
         * dy = ry * -sin(ang)
         */
        final double v1x = rx * cosStart;
        final double v1y = ry * -sinStart;
        final double v2x = rx * cosEnd;
        final double v2y = ry * -sinEnd;
        final boolean isIn = isInAngularRange(
                dx, dy,
                spanDeg,
                v1x, v1y,
                v2x, v2y);
        return isIn;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private GprimUtils() {
    }

    private static long computeAbsDelta(int v1, int v2) {
        return Math.abs(v2 - (long) v1);
    }
}
