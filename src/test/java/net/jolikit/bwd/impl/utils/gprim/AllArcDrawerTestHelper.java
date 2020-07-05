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

    private final HugeArcDrawer hugeArcDrawer;

    /*
     * temps
     */
    
    private final PixelFigStatusOvalAlgo.MyTemps tmpHugeOvalAlgoTemps =
            new PixelFigStatusOvalAlgo.MyTemps();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AllArcDrawerTestHelper(InterfaceClippedPointDrawer clippedPointDrawer) {
        
        final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
        final DefaultClippedRectDrawer defaultClippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, defaultClippedRectDrawer);
        
        final MidPointArcDrawer midPointArcDrawer = new MidPointArcDrawer(
                clippedLineDrawer,
                pointDrawer,
                lineDrawer,
                rectDrawer);
        this.midPointArcDrawer = midPointArcDrawer;
        
        final HugeArcDrawer hugeArcDrawer = new HugeArcDrawer(
                clippedLineDrawer,
                rectDrawer);
        this.hugeArcDrawer = hugeArcDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return true;
    }
    
    @Override
    public void callDrawMethod(GRect clip, TestArcArgs drawingArgs) {
        final GRect oval = drawingArgs.getOval();
        
        final InterfaceArcDrawer arcDrawer;
        if (drawingArgs.getMustUseHugeAlgo()) {
            arcDrawer = this.hugeArcDrawer;
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
        if (drawingArgs.getMustUseHugeAlgo()) {
            arcDrawer = this.hugeArcDrawer;
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
            if (spanDeg < 10.0) {
                // Might have many, due to angular constraint
                // excluding a lot of pixels.
                return Math.max(
                        drawingArgs.getOval().xSpan(),
                        drawingArgs.getOval().ySpan()) / 2;
            } else if (spanDeg < 90.0) {
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
        /*
         * This algorithm is used for drawing huge arcs,
         * but has originally been designed for this test class.
         */
        return PixelFigStatusArcAlgo.computePixelFigStatus(
                drawingArgs.getPixelFigStatusArcDef(),
                isFillElseDraw,
                pixel.x(),
                pixel.y(),
                //
                this.tmpHugeOvalAlgoTemps);
    }
}
