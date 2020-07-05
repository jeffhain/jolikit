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

public class HugeOvalDrawer implements InterfaceOvalDrawer {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final InterfaceClippedLineDrawer clippedLineDrawer;

    private final InterfaceRectDrawer rectDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HugeOvalDrawer(
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfaceRectDrawer rectDrawer) {
        
        this.clippedLineDrawer = LangUtils.requireNonNull(clippedLineDrawer);
        
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
                this.clippedLineDrawer);
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
                this.clippedLineDrawer,
                //
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
            InterfaceClippedLineDrawer clippedLineDrawer) {

        if (DEBUG) {
            Dbg.log();
            Dbg.log("drawOval("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ",)");
        }

        final boolean mustFill = false;
        final boolean areHorVerFlipped = false;
        
        final InterfaceRectDrawer rectDrawer = null;
        
        OvalOrArc_huge.drawOrFillHugeOvalOrArc(
                clip,
                x, y, xSpan, ySpan,
                0.0, 360.0,
                areHorVerFlipped,
                mustFill,
                //
                clippedLineDrawer,
                //
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
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfaceRectDrawer rectDrawer) {

        if (DEBUG) {
            Dbg.log();
            Dbg.log("fillOval("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + areHorVerFlipped
                    + ",,)");
        }

        final boolean mustFill = true;
        
        OvalOrArc_huge.drawOrFillHugeOvalOrArc(
                clip,
                x, y, xSpan, ySpan,
                0.0, 360.0,
                areHorVerFlipped,
                mustFill,
                //
                clippedLineDrawer,
                //
                rectDrawer);
    }
}
