/*
 * Copyright 2020 Jeff Hain
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

/**
 * Oval drawer based on a poly drawer.
 * Much faster than bresenham-like algorithm for large ovals.
 */
public class PolyOvalDrawer implements InterfaceOvalDrawer {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final InterfacePointDrawer pointDrawer;
    private final InterfaceLineDrawer lineDrawer;
    private final InterfaceRectDrawer rectDrawer;
    private final InterfacePolyDrawer polyDrawer;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public PolyOvalDrawer(
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer,
            InterfacePolyDrawer polyDrawer) {
        this.pointDrawer = LangUtils.requireNonNull(pointDrawer);
        this.lineDrawer = LangUtils.requireNonNull(lineDrawer);
        this.rectDrawer = LangUtils.requireNonNull(rectDrawer);
        this.polyDrawer = LangUtils.requireNonNull(polyDrawer);
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
                this.pointDrawer,
                this.lineDrawer,
                this.rectDrawer,
                this.polyDrawer);
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
                this.pointDrawer,
                this.lineDrawer,
                this.rectDrawer,
                this.polyDrawer);
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
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer,
            InterfacePolyDrawer polyDrawer) {

        if (DEBUG) {
            Dbg.log();
            Dbg.log("drawOval("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ",,,,)");
        }
        
        final boolean areHorVerFlipped = false;
        final boolean isFillElseDraw = false;
        
        OvalOrArc_asPoly.drawOrFillOvalOrArc(
                clip,
                x, y, xSpan, ySpan,
                0.0, 360.0,
                areHorVerFlipped,
                isFillElseDraw,
                //
                pointDrawer,
                lineDrawer,
                rectDrawer,
                polyDrawer);
    }

    /**
     * Takes care of span reduction in case of overflow.
     */
    public static void fillOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped,
            //
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer,
            InterfacePolyDrawer polyDrawer) {

        if (DEBUG) {
            Dbg.log();
            Dbg.log("fillOval("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + areHorVerFlipped
                    + ",,,,)");
        }
        
        final boolean isFillElseDraw = true;
        
        OvalOrArc_asPoly.drawOrFillOvalOrArc(
                clip,
                x, y, xSpan, ySpan,
                0.0, 360.0,
                areHorVerFlipped,
                isFillElseDraw,
                //
                pointDrawer,
                lineDrawer,
                rectDrawer,
                polyDrawer);
    }
}
