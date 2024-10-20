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
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

public class ScaledRectDrawerWithAlgo implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Data common to splittables, to save memory.
     */
    private class MyCmnData {
        final InterfaceColorTypeHelper colorTypeHelper;
        final InterfaceSrcPixels srcPixels;
        final GRect srcRect;
        final GRect dstRect;
        final GRect dstRectClipped;
        final InterfaceRowDrawer dstRowDrawer;
        final double dstLineCost;
        final double minAreaCostForSplit;
        public MyCmnData(
            InterfaceColorTypeHelper colorTypeHelper,
            InterfaceSrcPixels srcPixels,
            GRect srcRect,
            GRect dstRect,
            GRect dstRectClipped,
            InterfaceRowDrawer dstRowDrawer,
            double dstLineCost,
            double minAreaCostForSplit) {
            this.colorTypeHelper = colorTypeHelper;
            this.srcPixels = srcPixels;
            this.srcRect = srcRect;
            this.dstRect = dstRect;
            this.dstRectClipped = dstRectClipped;
            this.dstRowDrawer = dstRowDrawer;
            this.dstLineCost = dstLineCost;
            this.minAreaCostForSplit = minAreaCostForSplit;
        }
    }
            
    private class MySplittable implements InterfaceSplittable {
        final MyCmnData cmn;
        private int dstYStart;
        private int dstYEnd;
        public MySplittable(
            MyCmnData cmn,
            int dstYStart,
            int dstYEnd) {
            if (dstYStart > dstYEnd) {
                throw new IllegalArgumentException(dstYStart + " > " + dstYEnd);
            }
            this.cmn = cmn;
            this.dstYStart = dstYStart;
            this.dstYEnd = dstYEnd;
        }
        public void configure(int dstYStart, int dstYEnd) {
            this.dstYStart = dstYStart;
            this.dstYEnd = dstYEnd;
        }
        @Override
        public String toString() {
            return "[" + this.dstYStart + "," + this.dstYEnd + "]";
        }
        @Override
        public void run() {
            algo.drawScaledRectChunk(
                this.cmn.colorTypeHelper,
                this.cmn.srcPixels,
                this.cmn.srcRect,
                this.cmn.dstRect,
                this.cmn.dstRectClipped,
                this.dstYStart,
                this.dstYEnd,
                this.cmn.dstRowDrawer);
        }
        @Override
        public boolean worthToSplit() {
            return ScaledRectUtils.isWorthToSplit(
                this.cmn.dstLineCost,
                this.dstYStart,
                this.dstYEnd,
                this.cmn.minAreaCostForSplit);
        }
        @Override
        public InterfaceSplittable split() {
            final int dstYMid =
                this.dstYStart
                + ((this.dstYEnd - this.dstYStart) >> 1);
            final MySplittable ret = new MySplittable(
                this.cmn,
                dstYMid + 1,
                this.dstYEnd);
            // Configured last, since it changes dstYEnd.
            this.configure(this.dstYStart, dstYMid);
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final ThreadLocal<PpTlData> TL_DATA_0 =
        new ThreadLocal<PpTlData>() {
        @Override
        public PpTlData initialValue() {
            return new PpTlData();
        }
    };
    
    private final InterfaceScaledRectAlgo algo;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerWithAlgo(InterfaceScaledRectAlgo algo) {
        this.algo = algo;
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
        
        /*
         * Guard closes and args checks.
         */
        
        if (srcRect.isEmpty()) {
            return;
        }
        
        final GRect srcPixelsRect = srcPixels.getRect();
        if (!srcPixelsRect.contains(srcRect)) {
            throw new IllegalArgumentException(
                "srcRect ("
                    + srcRect
                    + ") is not included in srcPixels.getRect() ("
                    + srcPixelsRect
                    + ")");
        }
        
        if (!dstRect.overlaps(dstClip)) {
            return;
        }
        
        /*
         * 
         */
        
        final int maxItCount =
            Math.max(
                Math.abs(srcRect.xSpan() - dstRect.xSpan()),
                Math.abs(srcRect.ySpan() - dstRect.ySpan()));
        final int itCount =
            Math.min(
                maxItCount,
                this.algo.computeIterationCount(srcRect, dstRect));
        if (itCount >= 2) {
            final PpTlData tl = TL_DATA_0.get();
            //
            InterfaceSrcPixels itSrcPixels = null;
            IntArrSrcPixels itSrcPixelsInterm = null;
            int[] itSrcColor32Arr = null;
            int itSrcScanlineStride = 0;
            GRect itSrcRect = null;
            //
            GRect itDstRect = null;
            GRect itDstClip = null;
            InterfaceRowDrawer itDstRowDrawer = null;
            IntArrCopyRowDrawer itDstRowDrawerInterm = null;
            int[] itDstColor32Arr = null;
            int itDstScanlineStride = 0;
            
            final int maxXSpan = Math.max(srcRect.xSpan(), dstRect.xSpan());
            final int maxYSpan = Math.max(srcRect.ySpan(), dstRect.ySpan());
            // Max intermediary area might be larger
            // than max of src and dst area.
            final int maxArea = NbrsUtils.timesBounded(maxXSpan, maxYSpan);
            
            // double to avoid overflow issues here.
            final double dstDxMinFp = dstRect.x() - (double) srcRect.x();
            final double dstDyMinFp = dstRect.y() - (double) srcRect.y();
            final double dstDxMaxFp = dstRect.xMax() - (double) srcRect.xMax();
            final double dstDyMaxFp = dstRect.yMax() - (double) srcRect.yMax();
            
            for (int i = 0; i < itCount; i++) {
                // In ]0,1].
                final double ratio = (i + 1) / (double) itCount;
                if (i == 0) {
                    itSrcPixels = srcPixels;
                    itSrcRect = srcRect;
                } else {
                    if (i == 1) {
                        itSrcColor32Arr = tl.tmpBigArr1.getArr(maxArea);
                        itSrcPixelsInterm = new IntArrSrcPixels();
                    }
                    /*
                     * Previous dst becomes src.
                     * Switching src/dst arrays.
                     */
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
                    itSrcPixelsInterm.configure(
                        itSrcRect,
                        itSrcColor32Arr,
                        itSrcScanlineStride);
                    itSrcPixels = itSrcPixelsInterm;
                }
                //
                if (i < itCount - 1) {
                    // int part, to make sure not to remove too much.
                    itDstRect = srcRect.withBordersDeltas(
                        (int) (dstDxMinFp * ratio),
                        (int) (dstDyMinFp * ratio),
                        (int) (dstDxMaxFp * ratio),
                        (int) (dstDyMaxFp * ratio));
                    if (i == 0) {
                        itDstColor32Arr = tl.tmpBigArr2.getArr(maxArea);
                        itDstRowDrawerInterm = new IntArrCopyRowDrawer();
                    }
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
                    // to be itDstColor32Arr[0].
                    final GTransform transformArrToDst =
                        GTransform.valueOf(
                            GRotation.ROT_0,
                            -itDstRect.x(),
                            -itDstRect.y());
                    itDstRowDrawerInterm.configure(
                        transformArrToDst,
                        itDstColor32Arr,
                        itDstScanlineStride);
                    itDstRowDrawer = itDstRowDrawerInterm;
                } else {
                    itDstRect = dstRect;
                    itDstClip = dstClip;
                    itDstRowDrawer = dstRowDrawer;
                }
                runSeqOrPrl(
                    parallelizer,
                    colorTypeHelper,
                    itSrcPixels,
                    itSrcRect,
                    itDstRect,
                    itDstClip,
                    itDstRowDrawer);
            }
        } else {
            runSeqOrPrl(
                parallelizer,
                colorTypeHelper,
                srcPixels,
                srcRect,
                dstRect,
                dstClip,
                dstRowDrawer);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void runSeqOrPrl(
        InterfaceParallelizer parallelizer,
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer) {
        
        final GRect dstRectClipped = dstRect.intersected(dstClip);
        
        final int dstYStart = dstRectClipped.y();
        final int dstYEnd = dstRectClipped.yMax();
        
        boolean didGoPrl = false;
        if (parallelizer.getParallelism() >= 2) {
            final double dstLineCost =
                ScaledRectUtils.computeDstLineCost(
                    srcRect,
                    dstRect,
                    dstRectClipped);
            final double minAreaCostForSplit = this.algo.getAreaThresholdForSplit();
            if (ScaledRectUtils.isWorthToSplit(
                dstLineCost,
                dstYStart,
                dstYEnd,
                minAreaCostForSplit)) {
                didGoPrl = true;
                
                final MyCmnData cmn =
                    new MyCmnData(
                        colorTypeHelper,
                        srcPixels,
                        srcRect,
                        dstRect,
                        dstRectClipped,
                        dstRowDrawer,
                        dstLineCost,
                        minAreaCostForSplit);
                
                final MySplittable splittable =
                    new MySplittable(
                        cmn,
                        dstYStart,
                        dstYEnd);
                
                parallelizer.execute(splittable);
            }
        }
        
        if (!didGoPrl) {
            this.algo.drawScaledRectChunk(
                colorTypeHelper,
                srcPixels,
                srcRect,
                dstRect,
                dstRectClipped,
                dstYStart,
                dstYEnd,
                dstRowDrawer);
        }
    }
}
