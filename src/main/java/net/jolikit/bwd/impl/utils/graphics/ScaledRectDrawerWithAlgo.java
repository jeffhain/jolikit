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
import net.jolikit.bwd.impl.utils.graphics.PpTlData.PooledIntArrHolder;
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
        
        if (ScaledRectUtils.drawArgsCheckAndMustReturn(
            srcPixels,
            srcRect,
            //
            dstRect,
            dstClip)) {
            return;
        }
        
        /*
         * 
         */
        
        final double itSpanGrowthFactor = this.algo.getIterationSpanGrowthFactor();
        if (!(itSpanGrowthFactor > 1.0)) {
            throw new IllegalArgumentException(
                "iteration span growth factor ["
                    + itSpanGrowthFactor
                    + "] must be > 1");
        }
        
        final double itSpanShrinkFactor = this.algo.getIterationSpanShrinkFactor();
        if (!((itSpanShrinkFactor >= 0.0) && (itSpanShrinkFactor < 1.0))) {
            throw new IllegalArgumentException(
                "iteration span shrink factor ["
                    + itSpanShrinkFactor
                    + "] must be in [0,1[");
        }
        
        /*
         * 
         */
        
        final boolean xGrowth = (dstRect.xSpan() > srcRect.xSpan());
        final boolean yGrowth = (dstRect.ySpan() > srcRect.ySpan());
        
        final double xSpanFactor;
        if (xGrowth) {
            xSpanFactor = itSpanGrowthFactor;
        } else {
            xSpanFactor = itSpanShrinkFactor;
        }
        final double ySpanFactor;
        if (yGrowth) {
            ySpanFactor = itSpanGrowthFactor;
        } else {
            ySpanFactor = itSpanShrinkFactor;
        }
        
        int itKDstXSpan =
            computeNextItDstRectSpan(
                srcRect.xSpan(),
                xSpanFactor,
                dstRect.xSpan());
        int itKDstYSpan =
            computeNextItDstRectSpan(
                srcRect.ySpan(),
                ySpanFactor,
                dstRect.ySpan());
        
        final boolean gotIter =
            (itKDstXSpan != dstRect.xSpan())
            || (itKDstYSpan != dstRect.ySpan());
        if (gotIter) {
            final PpTlData tl = PpTlData.DEFAULT_TL_DATA.get();
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
            
            final PooledIntArrHolder tmpBigArrHolder1 = tl.borrowBigArrHolder();
            final PooledIntArrHolder tmpBigArrHolder2 = tl.borrowBigArrHolder();
            
            boolean isLastIt = false;
            while (!isLastIt) {
                if (itSrcPixels == null) {
                    itSrcPixels = srcPixels;
                    itSrcRect = srcRect;
                } else {
                    if (itSrcPixelsInterm == null) {
                        itSrcColor32Arr = tmpBigArrHolder1.getArr(maxArea);
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
                final boolean isFirstIt = (itDstRect == null);
                
                if (isFirstIt) {
                    itDstColor32Arr = tmpBigArrHolder2.getArr(maxArea);
                    itDstRowDrawerInterm = new IntArrCopyRowDrawer();
                } else {
                    itKDstXSpan =
                        computeNextItDstRectSpan(
                            itKDstXSpan,
                            xSpanFactor,
                            dstRect.xSpan());
                    itKDstYSpan =
                        computeNextItDstRectSpan(
                            itKDstYSpan,
                            ySpanFactor,
                            dstRect.ySpan());
                }
                
                isLastIt =
                    ((itKDstXSpan == dstRect.xSpan())
                    && (itKDstYSpan == dstRect.ySpan()));
                
                if (!isLastIt) {
                    // Using (0,0) position for itDstRect for all non-last iterations,
                    // to avoid risk of overflow due to eventual huge intermediary span
                    // starting at eventually huge srcRect or dstRect position.
                    itDstRect = GRect.valueOf(
                        0,
                        0,
                        itKDstXSpan,
                        itKDstYSpan);
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
                    /*
                     * Last iteration.
                     */
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
            
            tmpBigArrHolder1.release();
            tmpBigArrHolder2.release();
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
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * factor is never 1, which is checked on retrieval.
     * 
     * @param factor Is > 1 for span growth, < 1 for span shrink.
     */
    static int computeNextItDstRectSpan(
        int prevItDstRectSpan,
        double factor,
        int dstRectSpan) {
        
        int ret;
        if (factor > 1.0) {
            // floor() not to grow more than the specified factor...
            ret = (int) Math.floor(prevItDstRectSpan * factor);
            // ...unless if we did not grow,
            // in which case we force growth if possible.
            if ((ret == prevItDstRectSpan)
                // Need this check to avoid wrapping
                // when dstRectSpan is Integer.MAX_VALUE
                // and has already been reached.
                && (ret < Integer.MAX_VALUE)) {
                ret++;
            }
            // Making sure we don't go above target span.
            ret = Math.min(dstRectSpan, ret);
        } else {
            // ceil() not to shrink more than the specified factor...
            ret = (int) Math.ceil(prevItDstRectSpan * factor);
            // ...unless if we did not shrink,
            // in which case we force shrinking if possible.
            if (ret == prevItDstRectSpan) {
                // Fine if ret goes from 1 to 0 here,
                // since it will be raised
                // by subsequent max(dstRectSpan,_),
                // since we don't iterate with empty dstRect.
                ret--;
            }
            // Making sure we don't go below target span.
            ret = Math.max(dstRectSpan, ret);
        }
        return ret;
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
