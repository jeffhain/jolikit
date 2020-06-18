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

import java.util.Random;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;

class DefaultRectDrawerTestHelper extends AbstractDrawerTestHelper<GRect> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Random random = new Random(123456789L);
    
    private final DefaultRectDrawer rectDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultRectDrawerTestHelper(InterfaceClippedPointDrawer clippedPointDrawer) {
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedLineDrawer);
        final DefaultClippedRectDrawer defaultClippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, defaultClippedRectDrawer);
        this.rectDrawer = rectDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return true;
    }
    
    @Override
    public void callDrawMethod(GRect clip, GRect drawingArgs) {
        final GRect rect = drawingArgs;
        this.rectDrawer.drawRect(clip, rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    @Override
    public void callFillMethod(GRect clip, GRect drawingArgs) {
        final GRect rect = drawingArgs;
        final boolean areHorVerFlipped = this.random.nextBoolean();
        this.rectDrawer.fillRect(
                clip,
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan(),
                areHorVerFlipped);
    }

    @Override
    public long getAllowedNbrOfDanglingPixels(
            boolean isFillElseDraw,
            GRect drawingArgs) {
        return 0;
    }

    @Override
    public GRect computeBoundingBox(GRect drawingArgs) {
        final GRect rect = drawingArgs;
        return rect;
    }

    @Override
    public PixelFigStatus computePixelFigStatus(
            GRect drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        
        final GRect rect = drawingArgs;
        
        final int x = pixel.x();
        final int y = pixel.y();
        
        final PixelFigStatus ret;
        if (isFillElseDraw) {
            if (rect.contains(x, y)) {
                ret = PixelFigStatus.PIXEL_REQUIRED; 
            } else {
                ret = PixelFigStatus.PIXEL_NOT_ALLOWED; 
            }
        } else {
            if ((x == rect.x())
                    || (x == rect.xMax())) {
                // On vertical lines.
                if ((y >= rect.y())
                        && (y <= rect.yMax())) {
                    // On vertical sides.
                    ret = PixelFigStatus.PIXEL_REQUIRED; 
                } else {
                    ret = PixelFigStatus.PIXEL_NOT_ALLOWED; 
                }
            } else if ((y == rect.y())
                    || (y == rect.yMax())) {
                // On horizontal lines.
                if ((x >= rect.x())
                        && (x <= rect.xMax())) {
                    // On horizontal sides.
                    ret = PixelFigStatus.PIXEL_REQUIRED; 
                } else {
                    ret = PixelFigStatus.PIXEL_NOT_ALLOWED; 
                }
            } else {
                ret = PixelFigStatus.PIXEL_NOT_ALLOWED; 
            }
        }
        
        return ret;
    }
}

