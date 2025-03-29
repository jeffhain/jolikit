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
package net.jolikit.bwd.impl.awt;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.awt.PpTlData.PooledIntArrHolder;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.graphics.IntArrCopyRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcOverRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcPixels;
import net.jolikit.bwd.impl.utils.graphics.InterfaceRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawing;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Uses either ScaledRectDrawing or AWT drawImage,
 * possibly in parallel, and possibly iterating for downscaling.
 */
public class AwtPrlResizeImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double MAX_STEP_DOWNSCALING = 2.0;
    
    private static final AffineTransform BACKING_TRANSFORM_IDENTITY =
        new AffineTransform();
    
    /*
     * temps
     */
    
    private final IntArrSrcPixels tmpSrcPixels =
        new IntArrSrcPixels();
    
    private final IntArrCopyRowDrawer tmpCopyRowDrawer =
        new IntArrCopyRowDrawer();
    
    private final IntArrSrcOverRowDrawer tmpSrcOverRowDrawer =
        new IntArrSrcOverRowDrawer();
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public AwtPrlResizeImage() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Both source and destination images must be (ARGB,premul).
     * 
     * Uses AWT drawImage(), eventually iterating before last draw
     * on destination image.
     * The resizing must not involve BOXSAMPLED usage
     * (not supported by AWT).
     * 
     * @param xShiftInUser Only used when drawing through graphics.
     * @param yShiftInUser Only used when drawing through graphics.
     * @param rootBoxTopLeft Base coordinates of destination top-left pixel.
     * @param transform Transform from base to user.
     * @param dstGraphicsComposite Composite to use for drawing on destination.
     */
    public void drawImage(
        InterfaceParallelizer parallelizer,
        //
        BwdScalingType scalingType,
        boolean mustUseAwtScalingIfApplicable,
        int xShiftInUser,
        int yShiftInUser,
        GPoint rootBoxTopLeft,
        GTransform transform,
        Composite dstGraphicsComposite,
        //
        BufferedImageHelper srcHelper,
        GRect srcRect,
        //
        BufferedImageHelper dstHelper,
        GRect dstRect,
        GRect dstClip) {
        
        /*
         * Regarding correctness only (compared to our redefined algorithms):
         * - RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
         *   is accurate in case of exact upscaling,
         *   and not too bad in other cases.
         * - RenderingHints.VALUE_INTERPOLATION_BILINEAR
         *   is accurate enough if not dividing width or height
         *   by more than two.
         * - RenderingHints.VALUE_INTERPOLATION_BICUBIC
         *   is accurate enough if not dividing width or height
         *   by more than two.
         * 
         * Regarding speed, we use AWT algorithms to their full potential,
         * by using them directly instead of through InterfaceScaledRectDrawer
         * constraints overhead, and possibly in parallel.
         * As a result we always use them when we can.
         * 
         * NB: By default mustUseIntArrayGraphicsForXxx is true,
         * so we don't pass here.
         */
        
        if (!mustUseAwtScalingIfApplicable) {
            final boolean mustBlendElseCopy = true;
            drawImage_jlk(
                parallelizer,
                //
                scalingType,
                mustBlendElseCopy,
                rootBoxTopLeft,
                transform,
                //
                srcHelper,
                srcRect,
                //
                dstHelper,
                dstRect,
                dstClip);
            return;
        }
        
        final int sw = srcRect.xSpan();
        final int sh = srcRect.ySpan();
        final int dw = dstRect.xSpan();
        final int dh = dstRect.ySpan();
        final boolean gotDown = (dw < sw) || (dh < sh);
        final boolean gotUp = (dw > sw) || (dh > sh);
        
        if ((!gotDown)
            && (!gotUp)) {
            // No scaling: NEAREST is the fastest.
            scalingType = BwdScalingType.NEAREST;
        } else if ((scalingType == BwdScalingType.BOXSAMPLED)
            && ScaledRectUtils.isNearestExact(sw, sh, dw, dh)) {
            // Pixel-aligned growth.
            // NEAREST equivalent and faster.
            scalingType = BwdScalingType.NEAREST;
        }
        
        final BwdScalingType downscalingType;
        final BwdScalingType upscalingType;
        if ((scalingType == BwdScalingType.ITERATIVE_BILINEAR_BICUBIC)
            || (scalingType == BwdScalingType.BOXSAMPLED_BICUBIC)) {
            if (scalingType == BwdScalingType.ITERATIVE_BILINEAR_BICUBIC) {
                downscalingType = BwdScalingType.ITERATIVE_BILINEAR;
            } else {
                downscalingType = BwdScalingType.BOXSAMPLED;
            }
            upscalingType = BwdScalingType.ITERATIVE_BICUBIC;
        } else {
            // Either NEAREST, ITERATIVE_BILINEAR,
            // ITERATIVE_BICUBIC, or BOXSAMPLED.
            downscalingType = scalingType;
            upscalingType = scalingType;
        }
        
        PpTlData tl = null;

        final GRect dstRectWithAwtTransformShifts =
            dstRect.withPosDeltas(xShiftInUser, yShiftInUser);
        final GRect dstClipWithAwtTransformShifts =
            dstClip.withPosDeltas(xShiftInUser, yShiftInUser);
        
        final boolean needTwoScalings =
            gotDown
            && gotUp
            && (upscalingType != downscalingType);
        if (needTwoScalings) {
            tl = PpTlData.DEFAULT_TL_DATA.get();
            final PooledIntArrHolder tmpBigArrHolder =
                tl.borrowBigArrHolder();
            
            /*
             * Downscaling.
             */
            
            final int interWidth = Math.min(sw, dw);
            final int interHeight = Math.min(sh, dh);
            final GRect interRect = GRect.valueOf(0, 0, interWidth, interHeight);
            
            final int[] interArr = tmpBigArrHolder.getArr(interWidth * interHeight);
            
            final BufferedImage interImage =
                BufferedImageHelper.newBufferedImageWithIntArray(
                    interArr,
                    interWidth,
                    interWidth,
                    interHeight,
                    AwtPaintUtils.COMMON_BUFFERED_IMAGE_TYPE_ARGB_PRE);
            final BufferedImageHelper interHelper =
                new BufferedImageHelper(interImage);
            if (downscalingType == BwdScalingType.BOXSAMPLED) {
                final boolean mustBlendElseCopy = false;
                drawImage_jlk(
                    parallelizer,
                    //
                    scalingType,
                    mustBlendElseCopy,
                    rootBoxTopLeft,
                    transform,
                    //
                    srcHelper,
                    srcRect,
                    //
                    interHelper,
                    interRect,
                    interRect);
            } else {
                drawImage_awt_iteratingOrNot(
                    parallelizer,
                    //
                    downscalingType,
                    BACKING_TRANSFORM_IDENTITY,
                    AlphaComposite.Src,
                    //
                    srcHelper.getImage(),
                    srcRect,
                    //
                    interHelper.getImage(),
                    interRect,
                    interRect,
                    //
                    tl);
            }
            
            /*
             * Upscaling.
             */
            
            final AffineTransform dstGraphicsTransform =
                AwtUtils.computeGraphicsTransform(
                    rootBoxTopLeft,
                    transform);
            drawImage_awt_oneIteration(
                parallelizer,
                //
                upscalingType,
                dstGraphicsTransform,
                dstGraphicsComposite,
                //
                interHelper.getImage(),
                interRect,
                //
                dstHelper.getImage(),
                dstRectWithAwtTransformShifts,
                dstClipWithAwtTransformShifts);
            
            tmpBigArrHolder.release();
            
        } else {
            
            final BwdScalingType onlyScalingType;
            if (gotDown) {
                onlyScalingType = downscalingType;
            } else {
                onlyScalingType = upscalingType;
            }
            if (onlyScalingType == BwdScalingType.BOXSAMPLED) {
                final boolean mustBlendElseCopy = true;
                drawImage_jlk(
                    parallelizer,
                    //
                    scalingType,
                    mustBlendElseCopy,
                    rootBoxTopLeft,
                    transform,
                    //
                    srcHelper,
                    srcRect,
                    //
                    dstHelper,
                    dstRect,
                    dstClip);
            } else {
                final AffineTransform dstGraphicsTransform =
                    AwtUtils.computeGraphicsTransform(
                        rootBoxTopLeft,
                        transform);
                drawImage_awt_iteratingOrNot(
                    parallelizer,
                    //
                    onlyScalingType,
                    dstGraphicsTransform,
                    dstGraphicsComposite,
                    //
                    srcHelper.getImage(),
                    srcRect,
                    //
                    dstHelper.getImage(),
                    dstRectWithAwtTransformShifts,
                    dstClipWithAwtTransformShifts,
                    //
                    tl);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses ScaledRectDrawing, which uses
     * JLK InterfaceScaledRectDrawer algorithms.
     */
    private void drawImage_jlk(
        InterfaceParallelizer parallelizer,
        //
        BwdScalingType scalingType,
        boolean mustBlendElseCopy,
        GPoint rootBoxTopLeft,
        GTransform transform,
        //
        BufferedImageHelper toDrawHelper,
        GRect srcRect,
        //
        BufferedImageHelper dstHelper,
        GRect dstRect,
        GRect dstClip) {
        
        final IntArrSrcPixels srcPixels = this.tmpSrcPixels;
        final GRect toDrawImageRect = GRect.valueOf(
            0,
            0,
            toDrawHelper.getWidth(),
            toDrawHelper.getHeight());
        srcPixels.configure(
            toDrawImageRect,
            toDrawHelper.getIntArrayDirectlyUsed(),
            toDrawHelper.getWidth());
        
        // fix: was just using transform instead.
        final GTransform transformBiToUser =
            BindingCoordsUtils.computeTransformBoxToUser(
                rootBoxTopLeft,
                transform);
        final InterfaceRowDrawer dstRowDrawer;
        if (mustBlendElseCopy) {
            final IntArrSrcOverRowDrawer rowDrawer = this.tmpSrcOverRowDrawer;
            rowDrawer.configure(
                transformBiToUser,
                dstHelper.getIntArrayDirectlyUsed(),
                dstHelper.getScanlineStride());
            dstRowDrawer = rowDrawer;
        } else {
            final IntArrCopyRowDrawer rowDrawer = this.tmpCopyRowDrawer;
            rowDrawer.configure(
                transformBiToUser,
                dstHelper.getIntArrayDirectlyUsed(),
                dstHelper.getScanlineStride());
            dstRowDrawer = rowDrawer;
        }
        
        ScaledRectDrawing.drawScaledRect(
            parallelizer,
            scalingType,
            PremulArgbHelper.getInstance(),
            //
            srcPixels,
            srcRect,
            //
            dstRect,
            dstClip,
            dstRowDrawer);
    }
    
    /**
     * Uses AWT drawImage(), iterating or not.
     * 
     * @param tl Can be null.
     */
    private static void drawImage_awt_iteratingOrNot(
        InterfaceParallelizer parallelizer,
        //
        BwdScalingType scalingType,
        AffineTransform dstGraphicsTransform,
        Composite dstGraphicsComposite,
        //
        BufferedImage srcImage,
        GRect srcRect,
        //
        BufferedImage dstImage,
        GRect dstRect,
        GRect dstClip,
        //
        PpTlData tl) {
        
        final int sw = srcRect.xSpan();
        final int sh = srcRect.ySpan();
        final int dw = dstRect.xSpan();
        final int dh = dstRect.ySpan();
        
        final int maxXSpan = Math.max(sw, dw);
        final int maxYSpan = Math.max(sh, dh);
        // Max intermediary area might be larger
        // than max of src and dst area.
        final int maxArea = NbrsUtils.timesBounded(maxXSpan, maxYSpan);
        
        AffineTransform itTransform = null;
        Composite itComposite = null;
        //
        BufferedImage itSrcImage = srcImage;
        GRect itSrcRect = srcRect;
        int[] itSrcColor32Arr = null;
        int itSrcScanlineStride = 0;
        //
        GRect itDstRect = null;
        GRect itDstClip = null;
        BufferedImage itDstImage = null;
        int[] itDstColor32Arr = null;
        int itDstScanlineStride = 0;
        
        PooledIntArrHolder tmpBigArrHolder1 = null;
        PooledIntArrHolder tmpBigArrHolder2 = null;
        
        while (true) {
            int tmpSw = itSrcRect.xSpan();
            int tmpSh = itSrcRect.ySpan();
            final int tmpDw = computeDownscaledSpan(tmpSw, dw);
            final int tmpDh = computeDownscaledSpan(tmpSh, dh);
            /*
             * If true, we got too far down,
             * or just reached destination spans:
             * will finish with destination image.
             */
            final boolean isLastDraw =
                (tmpDw <= dw)
                && (tmpDh <= dh);
            
            if (!isLastDraw) {
                if (tl == null) {
                    tl = PpTlData.DEFAULT_TL_DATA.get();
                }
                tmpBigArrHolder1 = tl.borrowBigArrHolder();
                tmpBigArrHolder2 = tl.borrowBigArrHolder();
            }
            
            // Can be first, but is never last.
            final boolean isFirstIt = (itDstRect == null);
            
            /*
             * Prepating affine and composite.
             */
            
            if (isLastDraw) {
                itTransform = dstGraphicsTransform;
                itComposite = dstGraphicsComposite;
            } else {
                itTransform = BACKING_TRANSFORM_IDENTITY;
                // Must use Copy row drower for iterations,
                // else need to zeroize temporary arrays before use.
                itComposite = AlphaComposite.Src;
            }

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
                itSrcImage = BufferedImageHelper.newBufferedImageWithIntArray(
                    itSrcColor32Arr,
                    itSrcScanlineStride,
                    itSrcRect.xSpan(),
                    itSrcRect.ySpan(),
                    AwtPaintUtils.COMMON_BUFFERED_IMAGE_TYPE_ARGB_PRE);
            }
            
            /*
             * Prepating iteration dst.
             */
            
            if (isLastDraw) {
                itDstImage = dstImage;
                itDstRect = dstRect;
                itDstClip = dstClip;
            } else {
                if (isFirstIt) {
                    itDstColor32Arr = tmpBigArrHolder2.getArr(maxArea);
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
                itDstImage = BufferedImageHelper.newBufferedImageWithIntArray(
                    itDstColor32Arr,
                    itDstScanlineStride,
                    itDstRect.xSpan(),
                    itDstRect.ySpan(),
                    AwtPaintUtils.COMMON_BUFFERED_IMAGE_TYPE_ARGB_PRE);
            }
            
            drawImage_awt_oneIteration(
                parallelizer,
                //
                scalingType,
                itTransform,
                itComposite,
                //
                itSrcImage,
                itSrcRect,
                //
                itDstImage,
                itDstRect,
                itDstClip);
            
            if (isLastDraw) {
                break;
            }
        }
        
        if (tmpBigArrHolder1 != null) {
            tmpBigArrHolder1.release();
            tmpBigArrHolder2.release();
        }
    }
    
    /**
     * Uses AWT drawImage(), not iterating.
     * 
     * @param scalingType Must be one of NEAREST, ITERATIVE_BILINEAR
     *        or ITERATIVE_BICUBIC.
     */
    private static void drawImage_awt_oneIteration(
        InterfaceParallelizer parallelizer,
        //
        BwdScalingType scalingType,
        AffineTransform dstGraphicsTransform,
        Composite dstGraphicsComposite,
        //
        BufferedImage srcImage,
        GRect srcRect,
        //
        BufferedImage dstImage,
        GRect dstRect,
        GRect dstClip) {
        
        final int dstAreaThresholdForSplit;
        final Map<RenderingHints.Key, Object> hints;
        if (scalingType == BwdScalingType.NEAREST) {
            dstAreaThresholdForSplit = AwtPrlDrawImage.DEFAULT_NEAREST_DST_AREA_THRESHOLD_FOR_SPLIT;
            hints = AwtPrlDrawImage.HINTS_NEAREST;
        } else if (scalingType == BwdScalingType.ITERATIVE_BILINEAR) {
            dstAreaThresholdForSplit = AwtPrlDrawImage.DEFAULT_BILINEAR_DST_AREA_THRESHOLD_FOR_SPLIT;
            hints = AwtPrlDrawImage.HINTS_BILINEAR;
        } else if (scalingType == BwdScalingType.ITERATIVE_BICUBIC) {
            dstAreaThresholdForSplit = AwtPrlDrawImage.DEFAULT_BICUBIC_DST_AREA_THRESHOLD_FOR_SPLIT;
            hints = AwtPrlDrawImage.HINTS_BICUBIC;
        } else {
            throw new IllegalArgumentException("" + scalingType);
        }
        AwtPrlDrawImage.drawImage(
            parallelizer,
            dstAreaThresholdForSplit,
            hints,
            dstGraphicsTransform,
            dstGraphicsComposite,
            //
            srcImage,
            srcRect,
            //
            dstImage,
            dstRect,
            dstClip);
    }
    
    private static int computeDownscaledSpan(int previousSpan, int dstSpan) {
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
