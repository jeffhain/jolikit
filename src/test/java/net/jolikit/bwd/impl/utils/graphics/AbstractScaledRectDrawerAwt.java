/*
 * Copyright 2024-2025 Jeff Hain
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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Arrays;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.utils.graphics.PpTlData.PooledIntArrHolder;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

/**
 * Supports premul or non-premul color type,
 * but only in ARGB (not bothering with other formats).
 */
public abstract class AbstractScaledRectDrawerAwt implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Data common to splittables, to save memory.
     */
    private class MyCmnData {
        final BufferedImage srcImg;
        final GRect srcRectInImg;
        final GRect dstRectClippedInImg;
        final BufferedImage dstImg;
        final double srcAreaOverDstArea;
        public MyCmnData(
            BufferedImage srcImg,
            GRect srcRectInImg,
            GRect dstRectClippedInImg,
            BufferedImage dstImg,
            double srcAreaOverDstArea) {
            this.srcImg = srcImg;
            this.srcRectInImg = srcRectInImg;
            this.dstRectClippedInImg = dstRectClippedInImg;
            this.dstImg = dstImg;
            this.srcAreaOverDstArea = srcAreaOverDstArea;
        }
    }
            
    private class MySplittable implements InterfaceSplittable {
        final MyCmnData cmn;
        private int dstYStartInImg;
        private int dstYEndInImg;
        public MySplittable(
            MyCmnData cmn,
            int dstYStartInImg,
            int dstYEndInImg) {
            if (dstYStartInImg > dstYEndInImg) {
                throw new IllegalArgumentException(dstYStartInImg + " > " + dstYEndInImg);
            }
            this.cmn = cmn;
            this.dstYStartInImg = dstYStartInImg;
            this.dstYEndInImg = dstYEndInImg;
        }
        public void configure(int dstYStartInImg, int dstYEndInImg) {
            this.dstYStartInImg = dstYStartInImg;
            this.dstYEndInImg = dstYEndInImg;
        }
        @Override
        public String toString() {
            return "[" + this.dstYStartInImg + "," + this.dstYEndInImg + "]";
        }
        @Override
        public void run() {
            drawScaledRectChunk(
                this.cmn.srcImg,
                this.cmn.srcRectInImg,
                this.cmn.dstRectClippedInImg,
                this.dstYStartInImg,
                this.dstYEndInImg,
                this.cmn.dstImg);
        }
        @Override
        public boolean worthToSplit() {
            final int currentHeight =
                (this.dstYEndInImg - this.dstYStartInImg + 1);
            return ScaledRectUtils.isWorthToSplit(
                getSrcAreaThresholdForSplit(),
                getDstAreaThresholdForSplit(),
                this.cmn.srcAreaOverDstArea,
                this.cmn.dstRectClippedInImg.xSpan(),
                currentHeight);
        }
        @Override
        public InterfaceSplittable split() {
            final int dstYMidInImg =
                this.dstYStartInImg
                + ((this.dstYEndInImg - this.dstYStartInImg) >> 1);
            final MySplittable ret = new MySplittable(
                this.cmn,
                dstYMidInImg + 1,
                this.dstYEndInImg);
            // Configured last, since it changes dstYEndInImg.
            this.configure(this.dstYStartInImg, dstYMidInImg);
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Object renderingHint;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractScaledRectDrawerAwt(Object renderingHint) {
        this.renderingHint = renderingHint;
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
        
        final GRect srcPixelsRect = srcPixels.getRect();
        
        final int sw = srcRect.xSpan();
        final int sh = srcRect.ySpan();
        
        final GRect dstRectClipped = dstRect.intersected(dstClip);
        
        final PpTlData tl = PpTlData.DEFAULT_TL_DATA.get();
        
        // Possibly null.
        final int[] srcColor32Arr = srcPixels.color32Arr();
        
        final PooledIntArrHolder tmpBigArrHolder1 = tl.borrowBigArrHolder();
        final PooledIntArrHolder tmpBigArrHolder2 = tl.borrowBigArrHolder();
        final PooledIntArrHolder tmpArrHolder1 = tl.borrowArrHolder();

        final int[] srcPixelArr;
        final int srcPixelArrScanlineStride;
        final int srcImgWidth;
        final int srcImgHeight;
        final GRect srcRectInImg;
        if (srcColor32Arr != null) {
            srcPixelArr = srcColor32Arr;
            srcPixelArrScanlineStride = srcPixels.getScanlineStride();
            srcRectInImg = srcRect.withPosDeltas(
                -srcPixelsRect.x(),
                -srcPixelsRect.y());
            // Might not need all this area depending on srcRect,
            // but simplest to compute and doesn't hurt.
            srcImgWidth = srcPixelsRect.xSpan();
            srcImgHeight = srcPixelsRect.ySpan();
        } else {
            srcPixelArr = tmpBigArrHolder1.getArr(sw * sh);
            srcPixelArrScanlineStride = sw;
            srcRectInImg = srcRect.withPos(0, 0);
            srcImgWidth = sw;
            srcImgHeight = sh;
            for (int y = srcRect.y(); y <= srcRect.yMax(); y++) {
                final int offset = (y - srcRect.y()) * srcPixelArrScanlineStride;
                for (int x = srcRect.x(); x <= srcRect.xMax(); x++) {
                    final int srcColor32 = srcPixels.getColor32At(x, y);
                    srcPixelArr[offset + (x - srcRect.x())] = srcColor32;
                }
            }
        }
        
        final int bufImgType;
        if (colorTypeHelper.isPremul()) {
            bufImgType = BufferedImage.TYPE_INT_ARGB_PRE;
        } else {
            bufImgType = BufferedImage.TYPE_INT_ARGB;
        }
        
        final BufferedImage srcImg =
            BufferedImageHelper.newBufferedImageWithIntArray(
                srcPixelArr,
                srcPixelArrScanlineStride,
                //
                srcImgWidth,
                srcImgHeight,
                //
                bufImgType);
        
        final int dstRectClippedArea = dstRectClipped.area();
        final int[] dstPixelArr = tmpBigArrHolder2.getArr(dstRectClippedArea);
        final int dstPixelArrScanlineStride = dstRectClipped.xSpan();
        // Need to zeroize because AWT will blend into it.
        Arrays.fill(dstPixelArr, 0, dstRectClippedArea, 0);
        final BufferedImage dstImg =
            BufferedImageHelper.newBufferedImageWithIntArray(
                dstPixelArr,
                dstPixelArrScanlineStride,
                //
                dstRectClipped.xSpan(),
                dstRectClipped.ySpan(),
                //
                bufImgType);

        /*
         * scaling
         */
        
        // dstImg corresponds to the dstRectClipped.
        final GRect dstRectClippedInImg =
            dstRectClipped.withPos(0, 0);
        runSeqOrPrl(
            parallelizer,
            srcImg,
            srcRectInImg,
            dstRectClippedInImg,
            dstImg);
        
        /*
         * writing rows
         */

        final int[] dstImgRowArr = tmpArrHolder1.getArr(dstRectClippedInImg.xSpan());
        final BufferedImageHelper dstImgHelper =
            new BufferedImageHelper(dstImg);
        for (int j = 0; j < dstRectClippedInImg.ySpan(); j++) {
            final int y = dstRectClippedInImg.y() + j;
            for (int i = 0; i < dstRectClippedInImg.xSpan(); i++) {
                final boolean premul = colorTypeHelper.isPremul();
                final int argb32 = dstImgHelper.getArgb32At(i, y, premul);
                dstImgRowArr[i] = argb32;
            }
            final int rowOffset = 0;
            final int dstX = dstRectClipped.x();
            final int dstY = dstRectClipped.y() + j;
            final int length = dstRectClipped.xSpan();
            dstRowDrawer.drawRow(
                dstImgRowArr,
                rowOffset,
                dstX,
                dstY,
                length);
        }
        
        srcImg.flush();
        dstImg.flush();
        
        tmpBigArrHolder1.release();
        tmpBigArrHolder2.release();
        tmpArrHolder1.release();
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected abstract int getSrcAreaThresholdForSplit();
    
    protected abstract int getDstAreaThresholdForSplit();
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void runSeqOrPrl(
        InterfaceParallelizer parallelizer,
        //
        BufferedImage srcImg,
        GRect srcRectInImg,
        //
        GRect dstRectClippedInImg,
        BufferedImage dstImg) {
        
        final GRect dstRectInImg =
            GRect.valueOf(
                0,
                0,
                dstImg.getWidth(),
                dstImg.getHeight());
        
        final int dstYStartInImg = dstRectClippedInImg.y();
        final int dstYEndInImg = dstRectClippedInImg.yMax();
        
        boolean didGoPrl = false;
        if (parallelizer.getParallelism() >= 2) {
            final double srcAreaOverDstArea =
                srcRectInImg.areaLong() / (double) dstRectInImg.areaLong();
            if (ScaledRectUtils.isWorthToSplit(
                getSrcAreaThresholdForSplit(),
                getDstAreaThresholdForSplit(),
                srcAreaOverDstArea,
                dstRectClippedInImg.xSpan(),
                dstRectClippedInImg.ySpan())) {
                
                didGoPrl = true;
                
                final MyCmnData cmn =
                    new MyCmnData(
                        srcImg,
                        srcRectInImg,
                        dstRectClippedInImg,
                        dstImg,
                        srcAreaOverDstArea);
                
                final MySplittable splittable =
                    new MySplittable(
                        cmn,
                        dstYStartInImg,
                        dstYEndInImg);
                
                parallelizer.execute(splittable);
            }
        }
        
        if (!didGoPrl) {
            this.drawScaledRectChunk(
                srcImg,
                srcRectInImg,
                dstRectClippedInImg,
                dstYStartInImg,
                dstYEndInImg,
                dstImg);
        }
    }
    
    private void drawScaledRectChunk(
        BufferedImage srcImg,
        GRect srcRectInImg,
        GRect dstRectClippedInImg,
        int dstYStartInImg,
        int dstYEndInImg,
        BufferedImage dstImg) {
        
        // We assume we can do that concurrently,
        // which seems to work.
        final Graphics2D dstG = dstImg.createGraphics();
        
        // This clip is what allows parallel work chunks
        // not to step on each other's toes
        // (src and dst rects are the same for each chunk).
        dstG.setClip(
            dstRectClippedInImg.x(),
            dstYStartInImg,
            dstRectClippedInImg.xSpan(),
            (dstYEndInImg - dstYStartInImg + 1));
        
        dstG.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            this.renderingHint);
        
        /*
         * Using simplest applicable method,
         * in case it would help perfs.
         */
        
        final ImageObserver observer = null;
        
        final int sx = srcRectInImg.x();
        final int sy = srcRectInImg.y();
        final int sxSpan = srcRectInImg.xSpan();
        final int sySpan = srcRectInImg.ySpan();
        
        final int dx = 0;
        final int dy = 0;
        final int dxSpan = dstImg.getWidth();
        final int dySpan = dstImg.getHeight();
        
        final boolean onlyUsingPartOfSrcImg =
            (sx != 0)
            || (sy != 0)
            || (sxSpan != srcImg.getWidth())
            || (sySpan != srcImg.getHeight());
        if (onlyUsingPartOfSrcImg) {
            dstG.drawImage(
                srcImg,
                dx, // dx1
                dy, // dy1
                dx + dxSpan, // dx2 (exclusive)
                dy + dySpan, // dy2 (exclusive)
                sx, // sx1
                sy, // sy1
                sx + sxSpan, // sx2 (exclusive)
                sy + sySpan, // sy2 (exclusive)
                observer);
        } else {
            final boolean gotScaling =
                (dxSpan != sxSpan)
                || (dySpan != sySpan);
            if (gotScaling) {
                dstG.drawImage(
                    srcImg,
                    dx, // x
                    dy, // y
                    dxSpan, // width
                    dySpan, // height
                    observer);
            } else {
                dstG.drawImage(
                    srcImg,
                    dx, // x
                    dy, // y
                    observer);
            }
        }
        
        dstG.dispose();
    }
}
