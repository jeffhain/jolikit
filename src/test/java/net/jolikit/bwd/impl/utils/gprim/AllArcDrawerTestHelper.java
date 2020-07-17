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

public class AllArcDrawerTestHelper extends AbstractDrawerTestHelper<TestArcArgs> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MidPointArcDrawer midPointArcDrawer;
    
    private final PolyArcDrawer polyArcDrawer;
    
    /*
     * temps
     */
    
    private final PixelFigStatusOvalAlgo.MyTemps tmpHugeOvalAlgoTemps =
            new PixelFigStatusOvalAlgo.MyTemps();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AllArcDrawerTestHelper(
            InterfaceColorDrawer colorDrawer,
            InterfaceClippedPointDrawer clippedPointDrawer) {
        
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultClippedRectDrawer clippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        
        final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, clippedRectDrawer);
        final DefaultPolyDrawer polyDrawer = new DefaultPolyDrawer(
                colorDrawer,
                clippedPointDrawer,
                clippedLineDrawer,
                lineDrawer,
                rectDrawer);
        
        final MidPointArcDrawer midPointArcDrawer = new MidPointArcDrawer(
                clippedPointDrawer,
                clippedLineDrawer,
                pointDrawer,
                lineDrawer,
                rectDrawer);
        this.midPointArcDrawer = midPointArcDrawer;
        
        final PolyArcDrawer polyArcDrawer = new PolyArcDrawer(
                pointDrawer,
                lineDrawer,
                rectDrawer,
                polyDrawer); 
        this.polyArcDrawer = polyArcDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return true;
    }
    
    @Override
    public void callDrawMethod(GRect clip, TestArcArgs drawingArgs) {
        final GRect oval = drawingArgs.getOval();
        
        final InterfaceArcDrawer arcDrawer;
        if (drawingArgs.getMustUsePolyAlgo()) {
            arcDrawer = this.polyArcDrawer;
        } else {
            arcDrawer = this.midPointArcDrawer;
        }
        
        arcDrawer.drawArc(
                clip,
                oval.x(),
                oval.y(),
                oval.xSpan(),
                oval.ySpan(),
                drawingArgs.getStartDeg(),
                drawingArgs.getSpanDeg());
    }
    
    @Override
    public void callFillMethod(GRect clip, TestArcArgs drawingArgs) {
        final GRect oval = drawingArgs.getOval();
        
        // NB: True not tested, but not implemented.
        final boolean areHorVerFlipped = false;
        
        final InterfaceArcDrawer arcDrawer;
        if (drawingArgs.getMustUsePolyAlgo()) {
            arcDrawer = this.polyArcDrawer;
        } else {
            arcDrawer = this.midPointArcDrawer;
        }
        
        arcDrawer.fillArc(
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
    public long getAllowedNbrOfDanglingPixels(
            boolean isFillElseDraw,
            TestArcArgs drawingArgs) {
        if (isFillElseDraw) {
            final double spanDeg = drawingArgs.getReworkedSpanDeg();
            final boolean isMidPointAlgo = !drawingArgs.getMustUsePolyAlgo();
            if (isMidPointAlgo
                    && (spanDeg < 10.0)) {
                // Might have many, due to angular constraint
                // excluding a lot of pixels.
                return (drawingArgs.getOval().maxSpan() / 2);
            } else if (isMidPointAlgo
                    && (spanDeg < 30.0)) {
                // Might still have more than 3.
                return 6;
            } else if (spanDeg < 135.0) {
                // Might have 3, two at edges extremities
                // and one at center.
                return 3;
            } else if (spanDeg < 360.0) {
                // Might have 2, at edges extremities.
                return 2;
            } else {
                return 0;
            }
        } else {
            if (drawingArgs.getReworkedSpanDeg() == 360.0) {
                return 0;
            } else {
                return 2;
            }
        }
    }

    @Override
    public GRect computeBoundingBox(TestArcArgs drawingArgs) {
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
            TestArcArgs drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        return PixelFigStatusArcAlgo.computePixelFigStatus(
                drawingArgs,
                isFillElseDraw,
                pixel.x(),
                pixel.y(),
                //
                this.tmpHugeOvalAlgoTemps);
    }
}
