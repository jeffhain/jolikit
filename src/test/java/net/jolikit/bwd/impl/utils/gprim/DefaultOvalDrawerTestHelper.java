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

class DefaultOvalDrawerTestHelper extends AbstractDrawerTestHelper<GRect> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DefaultOvalDrawer ovalDrawer;
    
    /*
     * temps
     */
    
    private final PixelFigStatusOvalAlgo.MyTemps tmpHugeOvalAlgoTemps =
            new PixelFigStatusOvalAlgo.MyTemps();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultOvalDrawerTestHelper(
            InterfaceHugeAlgoSwitch hugeAlgoSwitch,
            InterfaceClippedPointDrawer clippedPointDrawer) {
        final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
        final DefaultClippedRectDrawer defaultClippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, defaultClippedRectDrawer);
        final DefaultOvalDrawer ovalDrawer = new DefaultOvalDrawer(
                hugeAlgoSwitch,
                clippedLineDrawer,
                pointDrawer,
                lineDrawer,
                rectDrawer);
        this.ovalDrawer = ovalDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return true;
    }
    
    @Override
    public void callDrawMethod(GRect clip, GRect drawingArgs) {
        final GRect rect = drawingArgs;
        this.ovalDrawer.drawOval(clip, rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    @Override
    public void callFillMethod(GRect clip, GRect drawingArgs) {
        final GRect rect = drawingArgs;
        // NB: True not tested, but not implemented.
        final boolean areHorVerFlipped = false;
        this.ovalDrawer.fillOval(
                clip,
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan(),
                areHorVerFlipped);
    }

    @Override
    public long getAllowedNbrOfDanglingPixels(GRect drawingArgs) {
        return 0;
    }

    @Override
    public GRect computeBoundingBox(GRect drawingArgs) {
        final GRect oval = drawingArgs;
        return oval;
    }

    @Override
    public PixelFigStatus computePixelFigStatus(
            GRect drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        /*
         * This algorithm is used for drawing huge ovals,
         * but has originally been designed for this test class.
         */
        final GRect oval = drawingArgs;
        return PixelFigStatusOvalAlgo.computePixelFigStatus(
                oval,
                isFillElseDraw,
                pixel.x(),
                pixel.y(),
                //
                this.tmpHugeOvalAlgoTemps);
    }
}
