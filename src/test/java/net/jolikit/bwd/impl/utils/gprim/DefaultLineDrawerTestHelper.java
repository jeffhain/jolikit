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

class DefaultLineDrawerTestHelper extends AbstractDrawerTestHelper<TestLineArgs> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DefaultLineDrawer lineDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultLineDrawerTestHelper(InterfaceClippedPointDrawer clippedPointDrawer) {
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
        this.lineDrawer = lineDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return false;
    }
    
    @Override
    public void callDrawMethod(GRect clip, TestLineArgs drawingArgs) {
        if (drawingArgs.isLineStipple()) {
            if (drawingArgs.pattern != GprimUtils.PLAIN_PATTERN) {
                // Non-plain stipples not handled by generic tests.
                throw new UnsupportedOperationException();
            }
            this.lineDrawer.drawLine(
                    clip,
                    drawingArgs.x1,
                    drawingArgs.y1,
                    drawingArgs.x2,
                    drawingArgs.y2,
                    //
                    drawingArgs.factor,
                    drawingArgs.pattern,
                    drawingArgs.pixelNum);
        } else {
            this.lineDrawer.drawLine(
                    clip,
                    drawingArgs.x1,
                    drawingArgs.y1,
                    drawingArgs.x2,
                    drawingArgs.y2);
        }
    }
    
    @Override
    public void callFillMethod(GRect clip, TestLineArgs drawingArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAllowedNbrOfDanglingPixels(TestLineArgs drawingArgs) {
        // 0, not 2, because we compute bounding box exactly. 
        return 0;
    }

    @Override
    public GRect computeBoundingBox(TestLineArgs drawingArgs) {
        return drawingArgs.computeBoundingBox();
    }

    @Override
    public PixelFigStatus computePixelFigStatus(
            TestLineArgs drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        
        if ((drawingBBox.xSpan() == 1)
                && (pixel.x() == drawingBBox.x())) {
            return PixelFigStatus.PIXEL_REQUIRED;
        }
        if ((drawingBBox.ySpan() == 1)
                && (pixel.y() == drawingBBox.y())) {
            return PixelFigStatus.PIXEL_REQUIRED;
        }
        
        final int nbrOfSurroundingEightsIn =
                GprimTestUtilz.computeNbrOfSurroundingEightsAboveElseLeftOfLine(
                        drawingArgs.x1,
                        drawingArgs.y1,
                        drawingArgs.x2,
                        drawingArgs.y2,
                        pixel);
        
        return GprimUtils.computePixelFigStatus(
                isFillElseDraw,
                nbrOfSurroundingEightsIn);
    }
}

