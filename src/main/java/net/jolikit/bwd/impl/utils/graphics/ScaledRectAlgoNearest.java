/*
 * Copyright 2024 Jeff Hain
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
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;

public class ScaledRectAlgoNearest implements InterfaceScaledRectAlgo {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Two or four times higher would give a bit better perfs
     * for huge images, but would not allow to parallelize
     * smaller ones.
     */
    static final int AREA_THRESHOLD_FOR_SPLIT = 256 * 1024;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final ThreadLocal<PpTlData> TL_DATA =
        new ThreadLocal<PpTlData>() {
        @Override
        public PpTlData initialValue() {
            return new PpTlData();
        }
    };
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectAlgoNearest() {
    }
    
    @Override
    public int getAreaThresholdForSplit() {
        return AREA_THRESHOLD_FOR_SPLIT;
    }
    
    @Override
    public int computeIterationCount(
        GRect srcRect,
        GRect dstRect) {
        return 1;
    }
    
    /**
     * @param colorTypeHelper Not used.
     */
    @Override
    public void drawScaledRectChunk(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstRectClipped,
        int dstYStart,
        int dstYEnd,
        InterfaceRowDrawer dstRowDrawer) {
        
        // Possibly null.
        final int[] srcColor32Arr = srcPixels.color32Arr();
        final int srcScanlineStride = srcPixels.getScanlineStride();
        
        final int sx = srcRect.x();
        final int sy = srcRect.y();
        final int srw = srcRect.xSpan();
        final int srh = srcRect.ySpan();
        
        final int drw = dstRect.xSpan();
        final int drh = dstRect.ySpan();
        final int drcw = dstRectClipped.xSpan();
        
        final int dcj0 = (dstRectClipped.y() - dstRect.y());
        final int dci0 = (dstRectClipped.x() - dstRect.x());
        
        final double di_to_0_1_factor = 1.0 / drw;
        final double dj_to_0_1_factor = 1.0 / drh;
        
        // Optimization, to avoid useless scalings.
        final boolean gotXScaling = (drw != srw);
        final boolean gotYScaling = (drh != srh);
        
        final PpTlData tl = TL_DATA.get();
        
        boolean usingSrcColor32Arr = false;
        
        final int[] rowArr;
        // Optimization, to avoid computing columns scaling for each row.
        final int[] siByDicArr;
        if (gotXScaling) {
            rowArr = tl.tmpArr1.getArr(drcw);
            
            siByDicArr = tl.tmpArr2.getArr(drcw);
            for (int dic = 0; dic < drcw; dic++) {
                final int di = dci0 + dic;
                final int si = computeSrcIndex(
                    srw,
                    di_to_0_1_factor,
                    di);
                siByDicArr[dic] = si;
            }
        } else {
            // Src is already row-scaled.
            if (srcColor32Arr != null) {
                rowArr = srcColor32Arr;
                usingSrcColor32Arr = true;
            } else {
                rowArr = tl.tmpArr1.getArr(drcw);
            }
            
            siByDicArr = null;
        }
        
        int prevSj = -1;
        final int djcStart = dstYStart - dstRectClipped.y();
        final int djcEnd = djcStart + (dstYEnd - dstYStart);
        for (int djc = djcStart; djc <= djcEnd; djc++) {
            final int dj = dcj0 + djc;
            final int sj;
            if (gotYScaling) {
                sj = computeSrcIndex(
                    srh,
                    dj_to_0_1_factor,
                    dj);
            } else {
                sj = dj;
            }
            
            final int rowOffset;
            if (usingSrcColor32Arr) {
                // Using input pixel array directly.
                // Means there is no X scaling.
                final GRect srcPixelsRect = srcPixels.getRect();
                rowOffset =
                    (sy - srcPixelsRect.y() + sj) * srcScanlineStride
                    + sx - srcPixelsRect.x() + dci0;
            } else {
                rowOffset = 0;
                final boolean movedToNewSrcRow = (sj != prevSj);
                if (movedToNewSrcRow) {
                    final int py = sy + sj;
                    for (int dic = 0; dic < drcw; dic++) {
                        final int di = dci0 + dic;
                        final int si = (gotXScaling ? siByDicArr[dic] : di);
                        final int px = sx + si;
                        // Not bothering to optimize in case we got the src array,
                        // this method should be fast enough, and not do useless checks.
                        final int srcColor32 = srcPixels.getColor32At(px, py);
                        rowArr[dic] = srcColor32;
                    }
                }
            }
            
            final int dstX = dstRectClipped.x();
            final int dstY = dstRectClipped.y() + djc;
            final int length = drcw;
            dstRowDrawer.drawRow(
                rowArr,
                rowOffset,
                dstX,
                dstY,
                length);
            
            prevSj = sj;
        }
    }
    
    /**
     * Computes the row or column index in source,
     * for the given row or column index in destination.
     * 
     * If there is no scaling, si = di,
     * so don't bother to call this.
     * 
     * @param srcLength Number of pixels in src.
     * @param dstLengthInv Inverse of the number of pixels in dst.
     * @return The index, from row left or column top, of the src column or row.
     */
    private static int computeSrcIndex(
        int srcLength,
        double dstLengthInv,
        int di) {
        
        /*
         * +-0.5 to account for the fact that pixels centers
         * coordinates are 0.5 away from pixels edges.
         */
        final double H = 0.5;
        final double ratio_0_1 = (di + H) * dstLengthInv;
        final double sid = ratio_0_1 * srcLength - H;
        final int si = BindingCoordsUtils.roundToInt(sid);
        
        return si;
    }
}