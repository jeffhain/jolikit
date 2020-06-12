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

class DefaultPointDrawerTestHelper extends AbstractDrawerTestHelper<GPoint> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DefaultPointDrawer pointDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultPointDrawerTestHelper(InterfaceClippedPointDrawer clippedPointDrawer) {
        final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
        this.pointDrawer = pointDrawer;
    }
    
    @Override
    public boolean isFillSupported() {
        return false;
    }
    
    @Override
    public void callDrawMethod(GRect clip, GPoint drawingArgs) {
        final GPoint point = drawingArgs;
        this.pointDrawer.drawPoint(clip, point.x(), point.y());
    }
    
    @Override
    public void callFillMethod(GRect clip, GPoint drawingArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAllowedNbrOfDanglingPixels(GPoint drawingArgs) {
        // 0 because out bounding box is exact.
        return 0;
    }

    @Override
    public GRect computeBoundingBox(GPoint drawingArgs) {
        final GPoint point = drawingArgs;
        return GRect.valueOf(point.x(), point.y(), 1, 1);
    }

    @Override
    public PixelFigStatus computePixelFigStatus(
            GPoint drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        
        final GPoint point = drawingArgs;
        
        final PixelFigStatus ret;
        if (pixel.equals(point)) {
            ret = PixelFigStatus.PIXEL_REQUIRED; 
        } else {
            ret = PixelFigStatus.PIXEL_NOT_ALLOWED; 
        }
        return ret;
    }
}

