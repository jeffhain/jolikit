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

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;

public class DefaultArcDrawerTestHelper extends AbstractDrawerTestHelper<PixelFigStatusArcDef> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DefaultArcDrawer arcDrawer;
    
    /*
     * temps
     */
    
    private final PixelFigStatusOvalAlgo.MyTemps tmpHugeOvalAlgoTemps =
            new PixelFigStatusOvalAlgo.MyTemps();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultArcDrawerTestHelper(
            InterfaceHugeAlgoSwitch hugeAlgoSwitch,
            InterfaceClippedPointDrawer clippedPointDrawer) {
        final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
        final DefaultClippedRectDrawer defaultClippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, defaultClippedRectDrawer);
        final DefaultArcDrawer arcDrawer = new DefaultArcDrawer(
                hugeAlgoSwitch,
                clippedLineDrawer,
                pointDrawer,
                lineDrawer,
                rectDrawer);
        this.arcDrawer = arcDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return true;
    }
    
    @Override
    public void callDrawMethod(GRect clip, PixelFigStatusArcDef drawingArgs) {
        final GRect oval = drawingArgs.getOval();
        this.arcDrawer.drawArc(
                clip,
                oval.x(),
                oval.y(),
                oval.xSpan(),
                oval.ySpan(),
                drawingArgs.getStartDeg(),
                drawingArgs.getSpanDeg());
    }
    
    @Override
    public void callFillMethod(GRect clip, PixelFigStatusArcDef drawingArgs) {
        final GRect oval = drawingArgs.getOval();
        // NB: True not tested, but not implemented.
        final boolean areHorVerFlipped = false;
        this.arcDrawer.fillArc(
                clip,
                oval.x(),
                oval.y(),
                oval.xSpan(),
                oval.ySpan(),
                drawingArgs.getStartDeg(),
                drawingArgs.getSpanDeg(),
                areHorVerFlipped);
    }

    @Override
    public long getAllowedNbrOfDanglingPixels(PixelFigStatusArcDef drawingArgs) {
        if (drawingArgs.getReworkedSpanDeg() == 360.0) {
            return 0;
        } else {
            return 2;
        }
    }

    @Override
    public GRect computeBoundingBox(PixelFigStatusArcDef drawingArgs) {
        /*
         * Doesn't hurt to return a box larger than needed.
         * Even helps for debug, since it gives geometric context
         * to arc pixels.
         */
        final GRect oval = drawingArgs.getOval();
        return oval;
    }

    @Override
    public PixelFigStatus computePixelFigStatus(
            PixelFigStatusArcDef drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        /*
         * This algorithm is used for drawing huge arcs,
         * but has originally been designed for this test class.
         */
        return PixelFigStatusArcAlgo.computePixelFigStatus(
                drawingArgs,
                isFillElseDraw,
                pixel.x(),
                pixel.y(),
                //
                this.tmpHugeOvalAlgoTemps);
    }
}
