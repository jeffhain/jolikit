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

/**
 * Abstract class for drawers testing.
 * 
 * @param ARG Type containing all drawing arguments.
 */
abstract class AbstractDrawerTestHelper<ARG> {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractDrawerTestHelper() {
    }
    
    /**
     * @return True if filling is supported for the drawer.
     */
    public abstract boolean isFillSupported();
    
    /**
     * Calls the drawXxx() method, that draws the outline.
     */
    public abstract void callDrawMethod(GRect clip, ARG drawingArgs);
    
    /**
     * Calls the fillXxx() method, that draws and fills the outline.
     * 
     * Only called if isFillSupported() is true.
     */
    public abstract void callFillMethod(GRect clip, ARG drawingArgs);
    
    /**
     * A dangling pixel is a pixel with no more than one neighbor,
     * or with only two adjacent neighbors, that are not on
     * clipped bounding box border.
     * Ex.: 0 for line (segment) drawing (not 2, because we know we compute
     * bounding box exactly for these), 0 for oval drawing, 2 for arc drawing,
     * many for stippled lines.
     * 
     * @return For a curve outline, the number of allowed dangling pixels,
     *         unless due to clipped bounding box border.
     */
    public abstract long getAllowedNbrOfDanglingPixels(
            boolean isFillElseDraw,
            ARG drawingArgs);
    
    /**
     * Allowed to leak some around actual drawing,
     * to make it easier to compute (for example in case of line stipple).
     * 
     * @return The bounding box for the specified drawing arguments.
     */
    public abstract GRect computeBoundingBox(ARG drawingArgs);
    
    /**
     * If filling is not supported, inner side doesn't make sense,
     * so you can choose either side of the curve to be inner side.
     * 
     * @param isFillElseDraw True if fill, false if draw.
     *        Useful because in come cases we don't want to draw all
     *        the outline that is filled in case of fill (such as
     *        for arcs, which angular limits are not drawn,
     *        letting the curve open).
     */
    public abstract PixelFigStatus computePixelFigStatus(
            ARG drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel);
}
