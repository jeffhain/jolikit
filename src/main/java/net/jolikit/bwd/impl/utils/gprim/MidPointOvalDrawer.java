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

public class MidPointOvalDrawer implements InterfaceOvalDrawer {

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

    public MidPointOvalDrawer(
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
    public void drawOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan) {
        drawOval(
                clip,
                x, y, xSpan, ySpan,
                //
                this.clippedPointDrawer,
                this.clippedLineDrawer,
                //
                this.pointDrawer,
                this.lineDrawer,
                this.rectDrawer);
    }

    @Override
    public void fillOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped) {
        fillOval(
                clip,
                x, y, xSpan, ySpan,
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
    public static void drawOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            //
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer) {

        if (DEBUG) {
            Dbg.log();
            Dbg.log("drawOval("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ",,,,,)");
        }

        OvalOrArc_midPointDraw.drawOvalOrArc(
                clip,
                x, y, xSpan, ySpan,
                0.0, 360.0,
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
    public static void fillOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
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
            Dbg.log("fillOval("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + areHorVerFlipped
                    + ",,,,,)");
        }

        OvalOrArc_midPointFill.fillOvalOrArc(
                clip,
                x, y, xSpan, ySpan,
                0.0, 360.0,
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
