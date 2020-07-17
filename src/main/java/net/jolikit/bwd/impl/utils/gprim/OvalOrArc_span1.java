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
 * For ovals or arcs with one span of 1, and the other span >= 1.
 * 
 * Allows to rule out these pathological cases to make general algorithms
 * simpler (in particular, by not having to deal with some radius being zero).
 */
public class OvalOrArc_span1 {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param xSpan Must be >= 1.
     * @param ySpan Must be >= 1.
     * @param startDeg Must be in [0.0,360.0[.
     * @param spanDeg Must be in [0.0,360.0].
     */
    public static void drawArc_xSpan1_or_ySpan1(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            double cx, double cy,
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer) {
        
        if (xSpan == 1) {
            if (ySpan == 1) {
                if (DEBUG) {
                    Dbg.log("special case : both spans = 1");
                }
                /*
                 * xSpan = 1, ySpan = 1
                 */
                drawArc_internal_xSpan1_ySpan1(
                        clip,
                        x, y,
                        lineDrawer);
            } else {
                if (DEBUG) {
                    Dbg.log("special case : xSpan = 1, ySpan >= 2");
                }
                /*
                 * xSpan = 1, ySpan >= 2
                 */
                drawArc_internal_xSpan1_ySpan2OrMore(
                        clip,
                        x, y, xSpan, ySpan,
                        cy,
                        startDeg, spanDeg,
                        lineDrawer);
            }
        } else {
            if (DEBUG) {
                Dbg.log("special case : xSpan >= 2, ySpan = 1");
            }
            /*
             * xSpan >= 2, ySpan = 1
             */
            drawArc_internal_xSpan2OrMore_ySpan1(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    cx,
                    lineDrawer);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private OvalOrArc_span1() {
    }
    
    /**
     * @param xSpan Must be 1.
     * @param ySpan Must be 1.
     */
    private static void drawArc_internal_xSpan1_ySpan1(
            GRect clip,
            int x, int y,
            InterfaceLineDrawer lineDrawer) {
        lineDrawer.drawLine(
                clip,
                x, y, x, y);
    }
    
    /**
     * @param xSpan Must be >= 2.
     * @param ySpan Must be 1.
     * @param startDeg Must be in [0.0,360.0[.
     * @param spanDeg Must be in [0.0,360.0].
     */
    private static void drawArc_internal_xSpan2OrMore_ySpan1(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            double cx,
            InterfaceLineDrawer lineDrawer) {
        
        /*
         * Infinitely curved at the edges:
         * left pixels in range if range overlaps ]90,270[,
         * right pixels in range if range overlaps ]-90,90[.
         */
        
        // To take care not to draw middle pixel twice.
        double minXPainted = Integer.MAX_VALUE + 1.0;
        
        final boolean isPosXInAngularRange =
                (startDeg < 90.0)
                || (startDeg + spanDeg > 270.0);
        if (DEBUG) {
            Dbg.log("isPosXInAngularRange = " + isPosXInAngularRange);
        }
        if (isPosXInAngularRange) {
            final int cxIntPos = centerToIntPos(cx);
            final int x1 = cxIntPos;
            final int x2 = x + xSpan - 1;
            minXPainted = x1;
            if (DEBUG) {
                Dbg.log("lineDrawer.drawLine("
                        + clip
                        + ", " + x1 + ", " + y + ", " + x2 + ", " + y
                        + ")");
            }
            lineDrawer.drawLine(
                    clip,
                    x1, y, x2, y);
        }
        
        final boolean isNegXInAngularRange =
                ((startDeg + spanDeg > 90.0) && (startDeg < 270))
                || (startDeg + spanDeg > (360.0 + 90.0));
        if (DEBUG) {
            Dbg.log("isNegXInAngularRange = " + isNegXInAngularRange);
        }
        if (isNegXInAngularRange) {
            final int cxIntNeg = centerToIntNeg(cx);
            final int x1 = x;
            final int x2 = (int) Math.min(cxIntNeg, minXPainted - 1.0);
            if (x1 <= x2) {
                if (DEBUG) {
                    Dbg.log("lineDrawer.drawLine("
                            + clip
                            + ", " + x1 + ", " + y + ", " + x2 + ", " + y
                            + ")");
                }
                lineDrawer.drawLine(
                        clip,
                        x1, y, x2, y);
            }
        }
    }

    /**
     * @param xSpan Must be 1.
     * @param ySpan Must be >= 2.
     * @param startDeg Must be in [0.0,360.0[.
     * @param spanDeg Must be in [0.0,360.0].
     */
    private static void drawArc_internal_xSpan1_ySpan2OrMore(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double cy,
            double startDeg, double spanDeg,
            InterfaceLineDrawer lineDrawer) {
        
        /*
         * Infinitely curved at the edges:
         * top pixels in range if range overlaps ]0,180[,
         * bottom pixels in range if range overlaps ]180,360[.
         */

        // To take care not to draw middle pixel twice.
        double minYPainted = Integer.MAX_VALUE + 1.0;
        
        final boolean isPosYInAngularRange =
                (startDeg + spanDeg > 180.0);
        if (DEBUG) {
            Dbg.log("isPosYInAngularRange = " + isPosYInAngularRange);
        }
        if (isPosYInAngularRange) {
            final int cyIntPos = centerToIntPos(cy);
            final int y1 = cyIntPos;
            final int y2 = y + ySpan - 1;
            minYPainted = y1;
            if (DEBUG) {
                Dbg.log("lineDrawer.drawLine("
                        + clip
                        + ", " + x + ", " + y1 + ", " + x + ", " + y2
                        + ")");
            }
            lineDrawer.drawLine(
                    clip,
                    x, y1, x, y2);
        }
        
        final boolean isNegYInAngularRange =
                (startDeg < 180.0)
                || (startDeg + spanDeg > 360.0);
        if (DEBUG) {
            Dbg.log("isNegYInAngularRange = " + isNegYInAngularRange);
        }
        if (isNegYInAngularRange) {
            final int cyIntNeg = centerToIntNeg(cy);
            final int y1 = y;
            final int y2 = (int) Math.min(cyIntNeg, minYPainted - 1.0);
            if (y1 <= y2) {
                if (DEBUG) {
                    Dbg.log("lineDrawer.drawLine("
                            + clip
                            + ", " + x + ", " + y1 + ", " + x + ", " + y2
                            + ")");
                }
                lineDrawer.drawLine(
                        clip,
                        x, y1, x, y2);
            }
        }
    }

    private static int centerToIntPos(double cd) {
        final int cdi = (int) cd;
        if ((double) cdi == cd) {
            return cdi;
        } else {
            return (int) (cd + 0.5);
        }
    }

    private static int centerToIntNeg(double cd) {
        final int cdi = (int) cd;
        if ((double) cdi == cd) {
            return cdi;
        } else {
            return (int) (cd - 0.5);
        }
    }
}
