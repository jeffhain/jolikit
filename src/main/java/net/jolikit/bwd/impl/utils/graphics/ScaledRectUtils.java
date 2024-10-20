/*
 * Copyright 2019-2024 Jeff Hain
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
import net.jolikit.lang.NbrsUtils;

public class ScaledRectUtils {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

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

    /**
     * The shrinking is applied such as ensuring that the removed parts are
     * "floored", i.e. that the kept parts are "ceiled".
     * For example, if old rectangle is (0,0,4,4), new rectangle is (0,0,2,4),
     * and old peer rectangle is (0,0,7,7), the result will be (0,0,4,7),
     * not (0,0,3,7).
     * 
     * @param oldRect Old rectangle. Must not be empty (since newRect
     *        must be included in it and not be empty either).
     * @param newRect New rectangle, which must be included in oldRect.
     *        Must not be empty.
     * @param oldPeerRect Old peer rectangle.
     * @return The rectangle corresponding to shrinking old peer rectangle
     *         in the same way old rectangle was shrunk to obtain new rectangle.
     * @throws IllegalArgumentException if oldRect.contains(newRect) is false.
     */
    public static GRect computeNewPeerRect(
            GRect oldRect,
            GRect newRect,
            GRect oldPeerRect) {
        
        if (!oldRect.contains(newRect)) {
            throw new IllegalArgumentException("newRect must be contained in oldRect");
        }

        final boolean gotXSpanReduction = (newRect.xSpan() < oldRect.xSpan());
        final boolean gotYSpanReduction = (newRect.ySpan() < oldRect.ySpan());
        if (DEBUG) {
            System.out.println("gotXSpanReduction = " + gotXSpanReduction);
            System.out.println("gotYSpanReduction = " + gotYSpanReduction);
        }

        if (!(gotXSpanReduction || gotYSpanReduction)) {
            return oldPeerRect;
        }

        final int newPeerX;
        final int newPeerXSpan;
        if (gotXSpanReduction) {
            if (DEBUG) {
                System.out.println("computing newPeerX and newPeerXSpan:");
            }
            final long newPeerXAndXSpan = computeNewPeerPosAndSpan(
                    oldRect.x(),
                    oldRect.xSpan(),
                    newRect.x(),
                    newRect.xSpan(),
                    oldPeerRect.x(),
                    oldPeerRect.xSpan());
            newPeerX = (int) (newPeerXAndXSpan >> 32);
            newPeerXSpan = (int) newPeerXAndXSpan;
        } else {
            newPeerX = oldPeerRect.x();
            newPeerXSpan = oldPeerRect.xSpan();
        }

        final int newPeerY;
        final int newPeerYSpan;
        if (gotYSpanReduction) {
            if (DEBUG) {
                System.out.println("computing newPeerY and newPeerYSpan:");
            }
            final long newPeerYAndYSpan = computeNewPeerPosAndSpan(
                    oldRect.y(),
                    oldRect.ySpan(),
                    newRect.y(),
                    newRect.ySpan(),
                    oldPeerRect.y(),
                    oldPeerRect.ySpan());
            newPeerY = (int) (newPeerYAndYSpan >> 32);
            newPeerYSpan = (int) newPeerYAndYSpan;
        } else {
            newPeerY = oldPeerRect.y();
            newPeerYSpan = oldPeerRect.ySpan();
        }

        final GRect newPeerRect = GRect.valueOf(
                newPeerX,
                newPeerY,
                newPeerXSpan,
                newPeerYSpan);

        return newPeerRect;
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    static boolean isWorthToSplit(
        double dstLineCost,
        int dstYStart,
        int dstYEnd,
        double minAreaCostForSplit) {
        final int dstLineCount = (dstYEnd - dstYStart + 1);
        final boolean ret;
        if (dstLineCount <= 1) {
            ret = false;
        } else {
            final double areaCost = dstLineCount * dstLineCost;
            ret = (areaCost >= minAreaCostForSplit);  
        }
        return ret;
    }
    
    static double computeDstLineCost(
        GRect srcRect,
        GRect dstRect,
        GRect dstRectClipped) {
        final double dstPixelCost = computeDstPixelCost(srcRect, dstRect);
        return dstRectClipped.xSpan() * dstPixelCost;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For use only if (newSpan < oldSpan).
     * 
     * @return A long with pos in 32 MSBits and span in 32 LSBits.
     */
    private static long computeNewPeerPosAndSpan(
            int oldPos,
            int oldSpan,
            int newPos,
            int newSpan,
            int oldPeerPos,
            int oldPeerSpan) {
        
        final int oldMaxPos = oldPos + oldSpan - 1;
        final int newMaxPos = newPos + newSpan - 1;
        
        final int newPeerPos;
        final int newPeerSpan;
        // Parts removed from old rectangle to new rectangle.
        // No to handle overflow because oldRect must be included in newRect.
        final int minSidePad = newPos - oldPos;
        final int maxSidePad = oldMaxPos - newMaxPos;
        if (DEBUG) {
            System.out.println("minSidePad = " + minSidePad);
            System.out.println("maxSidePad = " + maxSidePad);
        }

        // If only denominator is 0, is +Infinity, which gives Integer.MAX_VALUE when cast into int.
        // If both are 0, is NaN, which gives 0 when cast into int.
        final double spanRatio = oldPeerSpan / (double) oldSpan;
        if (DEBUG) {
            System.out.println("spanRatio = " + spanRatio);
        }

        // Parts to remove from old peer rectangle.
        // Cast to int does flooring, since all values are >= 0.
        final int peerMinSidePad = (int) (spanRatio * minSidePad);
        final int peerMaxSidePad = (int) (spanRatio * maxSidePad);
        if (DEBUG) {
            System.out.println("peerMinSidePad = " + peerMinSidePad);
            System.out.println("peerMaxSidePad = " + peerMaxSidePad);
        }

        // Avoiding overflows when reworking coordinates.
        newPeerPos =  NbrsUtils.plusBounded(oldPeerPos, peerMinSidePad);
        if (newSpan == 0) {
            newPeerSpan = 0;
        } else {
            newPeerSpan = Math.max(0, oldPeerSpan - (peerMinSidePad + peerMaxSidePad));
        }
        if (DEBUG) {
            System.out.println("newPeerPos = " + newPeerPos);
            System.out.println("newPeerSpan = " + newPeerSpan);
        }
        
        return (((long) newPeerPos) << 32 | newPeerSpan);
    }
    
    /*
     * 
     */
    
    private static double computeDstPixelCost(
        GRect srcRect,
        GRect dstRect) {
        final long srcArea = srcRect.areaLong();
        final long dstArea = dstRect.areaLong();
        final double ret;
        if (srcArea <= dstArea) {
            ret = 1.0;
        } else {
            ret = (srcArea / (double) dstArea);
        }
        return ret;
    }
}
