/*
 * Copyright 2025 Jeff Hain
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
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.utils.graphics.PpTlData.PooledIntArrHolder;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Iterates for downscaling to never divide span by more than two per step,
 * but does not iterate for upscaling.
 */
public class SrdIterForDown implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double MAX_STEP_DOWNSCALING = 2.0;
    
    private final InterfaceScaledRectDrawer drawer;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public SrdIterForDown(
        final InterfaceScaledRectDrawer drawer) {
        this.drawer = LangUtils.requireNonNull(drawer);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[iter-" + this.drawer + "]";
    }
    
    @Override
    public void drawScaledRect(
        InterfaceParallelizer parallelizer,
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer) {
        
        if (ScaledRectUtils.drawArgsCheckAndMustReturn(
            srcPixels.getRect(),
            srcRect,
            //
            dstRect,
            dstClip)) {
            return;
        }
        
        final int sw = srcRect.xSpan();
        final int sh = srcRect.ySpan();
        final int dw = dstRect.xSpan();
        final int dh = dstRect.ySpan();
        
        final int maxXSpan = Math.max(sw, dw);
        final int maxYSpan = Math.max(sh, dh);
        // Max intermediary area might be larger
        // than max of src and dst area.
        final int maxArea = NbrsUtils.timesBounded(maxXSpan, maxYSpan);
        
        InterfaceSrcPixels itSrcPixels = srcPixels;
        GRect itSrcRect = srcRect;
        IntArrSrcPixels itTmpSrcPixels = null;
        int[] itSrcColor32Arr = null;
        int itSrcScanlineStride = 0;
        //
        GRect itDstRect = null;
        GRect itDstClip = null;
        InterfaceRowDrawer itDstRowDrawer = null;
        IntArrCopyRowDrawer itTmpDstRowDrawer = null;
        int[] itDstColor32Arr = null;
        int itDstScanlineStride = 0;
        
        PpTlData tl = null;
        PooledIntArrHolder tmpBigArrHolder1 = null;
        PooledIntArrHolder tmpBigArrHolder2 = null;
        
        while (true) {
            int tmpSw = itSrcRect.xSpan();
            int tmpSh = itSrcRect.ySpan();
            final int tmpDw = this.computeDownscaledSpan(tmpSw, dw);
            final int tmpDh = this.computeDownscaledSpan(tmpSh, dh);
            /*
             * If true, we got too far down,
             * or just reached destination spans:
             * will finish with destination image.
             */
            final boolean isLastDraw =
                (tmpDw <= dw)
                && (tmpDh <= dh);
            
            if ((tl == null)
                && (!isLastDraw)) {
                tl = PpTlData.DEFAULT_TL_DATA.get();
                //
                tmpBigArrHolder1 = tl.borrowBigArrHolder();
                tmpBigArrHolder2 = tl.borrowBigArrHolder();
            }
            
            // Can be first, but is never last.
            final boolean isFirstIt = (itDstRect == null);
            
            /*
             * Preparing iteration src.
             */
            
            if (isFirstIt) {
                /*
                 * First round: using initial source.
                 */
            } else {
                /*
                 * Non-first round: previous dst becomes src.
                 * Switching src/dst arrays.
                 */
                if (itSrcColor32Arr == null) {
                    itSrcColor32Arr = tmpBigArrHolder1.getArr(maxArea);
                    itTmpSrcPixels = new IntArrSrcPixels();
                }
                {
                    final int[] tmp = itSrcColor32Arr;
                    itSrcColor32Arr = itDstColor32Arr;
                    itDstColor32Arr = tmp;
                }
                {
                    final int tmp = itSrcScanlineStride;
                    itSrcScanlineStride = itDstScanlineStride;
                    itDstScanlineStride = tmp;
                }
                itSrcRect = itDstRect;
                //
                itTmpSrcPixels.configure(
                    itSrcRect,
                    itSrcColor32Arr,
                    itSrcScanlineStride);
                itSrcPixels = itTmpSrcPixels;
            }
            
            /*
             * Prepating iteration dst.
             */
            
            if (isLastDraw) {
                itDstRowDrawer = dstRowDrawer;
                itDstRect = dstRect;
                itDstClip = dstClip;
            } else {
                if (isFirstIt) {
                    itDstColor32Arr = tmpBigArrHolder2.getArr(maxArea);
                    // Must use Copy row drower for iterations,
                    // else need to zeroize temporary arrays before use.
                    itTmpDstRowDrawer = new IntArrCopyRowDrawer();
                }
                
                // Using (0,0) position for itDstRect for all non-last iterations,
                // to avoid risk of overflow due to eventual huge intermediary span
                // starting at eventually huge srcRect or dstRect position.
                itDstRect = GRect.valueOf(
                    0,
                    0,
                    tmpDw,
                    tmpDh);
                /*
                 * Always using itDstRect as itDstClip
                 * for non-last iterations, for all pixels
                 * of itSrcRect to have been computed and not
                 * be undefined (or zero if we did zeroizations),
                 * since they can be used for last pixels computations
                 * even if out of src area corresponding to dstClip.
                 */
                itDstClip = itDstRect;
                itDstScanlineStride = itDstClip.xSpan();
                // We want (itDstRect.x(),itDstRect.y())
                // to be itDstColor32Arr[0], but it's already (0,0).
                final GTransform transformArrToDst = GTransform.IDENTITY;
                itTmpDstRowDrawer.configure(
                    transformArrToDst,
                    itDstColor32Arr,
                    itDstScanlineStride);
                itDstRowDrawer = itTmpDstRowDrawer;
            }
            
            this.drawer.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                itSrcPixels,
                itSrcRect,
                //
                itDstRect,
                itDstClip,
                itDstRowDrawer);
            
            if (isLastDraw) {
                break;
            }
        }
        
        if (tl != null) {
            tmpBigArrHolder1.release();
            tmpBigArrHolder2.release();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private int computeDownscaledSpan(int previousSpan, int dstSpan) {
        /*
         * Ceil to make sure span is never divided
         * by more than maxStepDownscaling,
         * unless factor is too close to 1
         * in which case we force downscaling.
         */
        int ret = Math.max(dstSpan, (int) Math.ceil(previousSpan * (1.0 / MAX_STEP_DOWNSCALING)));
        if ((ret > dstSpan) && (ret == previousSpan)) {
            // Did not downscale, but could: forcing downscaling.
            ret--;
        }
        return ret;
    }
}
