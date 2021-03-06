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

public class MidPointArcDrawer implements InterfaceArcDrawer {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final InterfaceClippedPointDrawer clippedPointDrawer;
    private final InterfaceClippedLineDrawer clippedLineDrawer;
    
    private final InterfacePointDrawer pointDrawer;
    private final InterfaceLineDrawer lineDrawer;
    private final InterfaceRectDrawer rectDrawer;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public MidPointArcDrawer(
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer) {
        
        this.clippedPointDrawer = LangUtils.requireNonNull(clippedPointDrawer);
        this.clippedLineDrawer = LangUtils.requireNonNull(clippedLineDrawer);
        
        this.pointDrawer = LangUtils.requireNonNull(pointDrawer);
        this.lineDrawer = LangUtils.requireNonNull(lineDrawer);
        this.rectDrawer = LangUtils.requireNonNull(rectDrawer);
    }

    /*
     * Instance methods.
     */

    @Override
    public void drawArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        drawArc(
                clip,
                x, y, xSpan, ySpan,
                startDeg, spanDeg,
                //
                this.clippedPointDrawer,
                this.clippedLineDrawer,
                //
                this.pointDrawer,
                this.lineDrawer,
                this.rectDrawer);
    }

    @Override
    public void fillArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            boolean areHorVerFlipped) {
        fillArc(
                clip,
                x, y, xSpan, ySpan,
                startDeg, spanDeg,
                areHorVerFlipped,
                //
                this.clippedPointDrawer,
                this.clippedLineDrawer,
                //
                this.pointDrawer,
                this.lineDrawer,
                this.rectDrawer);
    }

    /*
     * Static methods.
     */
    
    /**
     * Takes care of span reduction in case of overflow.
     */
    public static void drawArc(
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
            Dbg.log();
            Dbg.log("drawArc("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ",,,,,)");
        }
        
        startDeg = GprimUtils.computeNormalizedStartDeg(startDeg);
        spanDeg = GprimUtils.computeClampedSpanDeg(spanDeg);
        
        startDeg = GprimUtils.computeReworkedStartDeg(startDeg, spanDeg);
        spanDeg = GprimUtils.computeReworkedSpanDeg(spanDeg);

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
    }

    /**
     * Takes care of span reduction in case of overflow.
     */
    public static void fillArc(
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
            Dbg.log();
            Dbg.log("fillArc("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ", " + areHorVerFlipped
                    + ",,,,,)");
        }
        
        startDeg = GprimUtils.computeNormalizedStartDeg(startDeg);
        spanDeg = GprimUtils.computeClampedSpanDeg(spanDeg);
        
        startDeg = GprimUtils.computeReworkedStartDeg(startDeg, spanDeg);
        spanDeg = GprimUtils.computeReworkedSpanDeg(spanDeg);

        OvalOrArc_midPointFill.fillOvalOrArc(
                clip,
                x, y, xSpan, ySpan,
                startDeg, spanDeg,
                areHorVerFlipped,
                //
                clippedPointDrawer,
                clippedLineDrawer,
                //
                pointDrawer,
                lineDrawer,
                rectDrawer);
    }
}
