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

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;

class AllOvalDrawerTestHelper extends AbstractDrawerTestHelper<TestOvalArgs> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MidPointOvalDrawer midPointOvalDrawer;

    private final HugeOvalDrawer hugeOvalDrawer;

    /*
     * temps
     */
    
    private final PixelFigStatusOvalAlgo.MyTemps tmpHugeOvalAlgoTemps =
            new PixelFigStatusOvalAlgo.MyTemps();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AllOvalDrawerTestHelper(
            InterfaceClippedPointDrawer clippedPointDrawer) {
        
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultClippedRectDrawer clippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        
        final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, clippedRectDrawer);
        
        final MidPointOvalDrawer midPointOvalDrawer = new MidPointOvalDrawer(
                clippedPointDrawer,
                clippedLineDrawer,
                pointDrawer,
                lineDrawer,
                rectDrawer);
        this.midPointOvalDrawer = midPointOvalDrawer;
        
        final HugeOvalDrawer hugeOvalDrawer = new HugeOvalDrawer(
                clippedLineDrawer,
                rectDrawer);
        this.hugeOvalDrawer = hugeOvalDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return true;
    }
    
    @Override
    public void callDrawMethod(GRect clip, TestOvalArgs drawingArgs) {
        final GRect rect = drawingArgs.getOval();
        
        final InterfaceOvalDrawer ovalDrawer;
        if (drawingArgs.getMustUseHugeAlgo()) {
            ovalDrawer = this.hugeOvalDrawer;
        } else {
            ovalDrawer = this.midPointOvalDrawer;
        }
        
        ovalDrawer.drawOval(
                clip,
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    @Override
    public void callFillMethod(GRect clip, TestOvalArgs drawingArgs) {
        final GRect rect = drawingArgs.getOval();
        
        // NB: True not tested, but not implemented.
        final boolean areHorVerFlipped = false;
        
        final InterfaceOvalDrawer ovalDrawer;
        if (drawingArgs.getMustUseHugeAlgo()) {
            ovalDrawer = this.hugeOvalDrawer;
        } else {
            ovalDrawer = this.midPointOvalDrawer;
        }
        
        ovalDrawer.fillOval(
                clip,
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan(),
                areHorVerFlipped);
    }

    @Override
    public long getAllowedNbrOfDanglingPixels(
            boolean isFillElseDraw,
            TestOvalArgs drawingArgs) {
        return 0;
    }

    @Override
    public GRect computeBoundingBox(TestOvalArgs drawingArgs) {
        final GRect oval = drawingArgs.getOval();
        return oval;
    }

    @Override
    public PixelFigStatus computePixelFigStatus(
            TestOvalArgs drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        /*
         * This algorithm is used for drawing huge ovals,
         * but has originally been designed for this test class.
         */
        final GRect oval = drawingArgs.getOval();
        return PixelFigStatusOvalAlgo.computePixelFigStatus(
                oval,
                isFillElseDraw,
                pixel.x(),
                pixel.y(),
                //
                this.tmpHugeOvalAlgoTemps);
    }
}
