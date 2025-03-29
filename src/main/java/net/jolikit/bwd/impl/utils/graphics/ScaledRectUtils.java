/*
 * Copyright 2019-2025 Jeff Hain
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
package net.jolikit.bwd.impl.utils.graphics;

import net.jolikit.bwd.api.graphics.GRect;

public class ScaledRectUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Args checks for InterfaceScaledRectDrawer.drawScaledRect().
     * 
     * @return True if must return early (i.e. nothing to draw).
     */
    public static boolean drawArgsCheckAndMustReturn(
        GRect srcImageRect,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip) {
        
        if (srcRect.isEmpty()) {
            return true;
        }
        
        if (!srcImageRect.contains(srcRect)) {
            throw new IllegalArgumentException(
                "srcRect ("
                    + srcRect
                    + ") is not included in srcImageRect ("
                    + srcImageRect
                    + ")");
        }
        
        return (!dstRect.overlaps(dstClip));
    }
    
    /**
     * If any destination span is zero, returns true.
     * If any source span is zero, returns false (undefined can't be exact).
     * 
     * @return True if NEAREST algorithm is exact for the specified
     *         scale change.
     */
    public static boolean isNearestExact(
        int sxSpan,
        int sySpan,
        int dxSpan,
        int dySpan) {
        final boolean ret;
        if ((dxSpan == 0)
            || (dySpan == 0)) {
            ret = true;
        } else if ((sxSpan == 0)
            || (sySpan == 0)) {
            ret = false;
        } else {
            // True if no scale change or pixel-aligned growth. 
            ret = (dxSpan % sxSpan == 0)
                && (dySpan % sySpan == 0);
        }
        return ret;
    }
    
    public static boolean isWorthToSplit(
        int areaThresholdForSplit,
        int widthClipped,
        int heightClipped) {
        
        return (heightClipped >= 2)
            && (widthClipped * heightClipped >= areaThresholdForSplit);
    }
    
    public static boolean isWorthToSplit(
        int srcAreaThresholdForSplit,
        int dstAreaThresholdForSplit,
        double srcAreaOverDstArea,
        int dstWidthClipped,
        int dstHeightClipped) {
        
        if (dstHeightClipped <= 1) {
            return false;
        }
        
        final int areaClipped = dstWidthClipped * dstHeightClipped;
        final double srcAreaClippedFp =
            areaClipped * srcAreaOverDstArea;
        return (srcAreaClippedFp >= srcAreaThresholdForSplit)
            || (areaClipped >= dstAreaThresholdForSplit);
    }
}
