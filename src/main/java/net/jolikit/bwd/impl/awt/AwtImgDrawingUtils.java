/*
 * Copyright 2021-2024 Jeff Hain
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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.graphics.IntArrHolder;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcOverRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcPixels;
import net.jolikit.bwd.impl.utils.graphics.InterfaceColorTypeHelper;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawing;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Utilities to draw scaled images onto an AWT graphics
 * backed by alpha-premultiplied ARGB32 buffered image.
 */
public class AwtImgDrawingUtils {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * temps
     */
    
    private final IntArrHolder tmpIntArr = new IntArrHolder();
    
    private final IntArrSrcPixels tmpSrcPixels =
        new IntArrSrcPixels();
    
    private final IntArrSrcOverRowDrawer tmpRowDrawer =
        new IntArrSrcOverRowDrawer();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AwtImgDrawingUtils() {
    }
    
    /**
     * @param xShiftInUser Only used when drawing through graphics.
     * @param yShiftInUser Only used when drawing through graphics.
     * @param rootBoxTopLeft Base coordinates of buffered image top-left pixel.
     * @param transform Transform from base to user.
     * @param dstImageHelper Helper for the image backing the specified graphics.
     *        Must be backed by an alpha-premultipled ARGB32 array.
     * @param g Graphics to draw into. Only used when not drawing through
     *        destination image helper.
     */
    public void drawBufferedImageOnG(
        int xShiftInUser,
        int yShiftInUser,
        GPoint rootBoxTopLeft,
        GTransform transform,
        GRect clipInUser,
        //
        int dx,
        int dy,
        int dxSpan,
        int dySpan,
        //
        BufferedImage srcImage,
        //
        int sx,
        int sy,
        int sxSpan,
        int sySpan,
        //
        InterfaceParallelizer parallelizer,
        BwdScalingType scalingType,
        boolean mustUseBackingImageScalingIfApplicable,
        //
        BufferedImageHelper dstImageHelper,
        Graphics2D g) {
        
        final boolean mustUseAwtScaling =
            computeMustUseAwtScaling(
                sxSpan,
                sySpan,
                dxSpan,
                dySpan,
                scalingType,
                mustUseBackingImageScalingIfApplicable);
        if (mustUseAwtScaling) {
            final int _x = dx + xShiftInUser;
            final int _y = dy + yShiftInUser;
            
            final Object renderingHint;
            if (scalingType == BwdScalingType.NEAREST) {
                renderingHint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
                
            } else if ((scalingType == BwdScalingType.BICUBIC)
                || (scalingType == BwdScalingType.BILICUBIC)) {
                // If BILICUBIC, only passing here when
                // equivalent to BICUBIC.
                renderingHint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                
            } else {
                throw new IllegalArgumentException();
            }
            
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                renderingHint);
            
            /*
             * Using simplest applicable method,
             * in case it would help perfs.
             */
            
            final ImageObserver observer = null;
            
            final boolean onlyUsingPartOfSrcImg =
                (sx != 0)
                || (sy != 0)
                || (sxSpan != srcImage.getWidth())
                || (sySpan != srcImage.getHeight());
            if (onlyUsingPartOfSrcImg) {
                g.drawImage(
                    srcImage,
                    _x, // dx1
                    _y, // dy1
                    _x + dxSpan, // dx2 (exclusive)
                    _y + dySpan, // dy2 (exclusive)
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
                    g.drawImage(srcImage, _x, _y, dxSpan, dySpan, observer);
                } else {
                    g.drawImage(srcImage, _x, _y, observer);
                }
            }
            
            // Restoring the default (null would throw).
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else {
            final InterfaceColorTypeHelper colorTypeHelper =
                PremulArgbHelper.getInstance();
            final boolean premul = colorTypeHelper.isPremul();
            
            final BufferedImage dstImage = dstImageHelper.getImage();
            {
                final boolean withAlpha = true;
                BufferedImageHelper.requireCompatible(
                    dstImage,
                    withAlpha,
                    BihPixelFormat.ARGB32,
                    premul);
            }
            
            final int[] srcPremulArgb32Arr =
                this.tmpIntArr.getArr(sxSpan * sySpan);
            final BufferedImageHelper srcImageHelper =
                new BufferedImageHelper(srcImage);
            srcImageHelper.getPixelsInto(
                sx, sy, sxSpan, sySpan,
                srcPremulArgb32Arr,
                sxSpan,
                BihPixelFormat.ARGB32,
                premul);
            final IntArrSrcPixels srcPixels = this.tmpSrcPixels;
            final GRect srcRectInImg = GRect.valueOf(0, 0, sxSpan, sySpan);
            srcPixels.configure(
                srcRectInImg,
                srcPremulArgb32Arr,
                sxSpan);
            
            final GRect dstRectInUser = GRect.valueOf(dx, dy, dxSpan, dySpan);
            final GRect dstClipInUser = clipInUser;
            
            final IntArrSrcOverRowDrawer dstRowDrawer = this.tmpRowDrawer;
            // fix: was just using transform instead.
            final GTransform transformBiToUser =
                BindingCoordsUtils.computeTransformBoxToUser(
                    rootBoxTopLeft,
                    transform);
            final int[] dstPremulArgb32Arr =
                BufferedImageHelper.getIntPixelArr(dstImage);
            dstRowDrawer.configure(
                transformBiToUser,
                dstPremulArgb32Arr,
                dstImage.getWidth());
            
            ScaledRectDrawing.drawScaledRect(
                parallelizer,
                scalingType,
                colorTypeHelper,
                //
                srcPixels,
                srcRectInImg,
                //
                dstRectInUser,
                dstClipInUser,
                dstRowDrawer);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static boolean computeMustUseAwtScaling(
        int sxSpan,
        int sySpan,
        int dxSpan,
        int dySpan,
        BwdScalingType scalingType,
        boolean mustUseBackingImageScalingIfApplicable) {
        
        /*
         * Regarding correctness only (compared to our redefined algorithms):
         * - RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
         *   is accurate in case of exact upscaling,
         *   and not too bad in other cases.
         * - RenderingHints.VALUE_INTERPOLATION_BILINEAR
         *   can't fit our needs, for it is much closer to
         *   RenderingHints.VALUE_INTERPOLATION_BICUBIC
         *   than to what we mean by bilinear.
         * - RenderingHints.VALUE_INTERPOLATION_BICUBIC
         *   is accurate enough if not dividing width or height
         *   by more than two.
         * 
         * Regarding speed, our BufferedImageHelper.getPixelsInto()
         * is not parallelized and can spend much time in
         * sun.java2d.SunGraphics2D.drawImage()
         * -(...)-> sun.java2d.loops.MaskBlit$General.MaskBlit
         * when having transparency,
         * so even though our drawScaledRect() is usually fast with parallelism
         * and our redefined scaling algorithms, and AWT scalings
         * are not always equivalent, we might want to allow for
         * delegating to AWT scalings when configured in the binding
         * and they are accurate enough.
         * Also, by default mustUseIntArrayGraphicsForXxx is true,
         * so we don't even pass here, so it would not break
         * default behavior.
         */
        
        final boolean ret;
        if (mustUseBackingImageScalingIfApplicable) {
            if (scalingType == BwdScalingType.NEAREST) {
                ret = true;
            } else if ((scalingType == BwdScalingType.BICUBIC)
                || (scalingType == BwdScalingType.BILICUBIC)) {
                ret = (dxSpan >= (sxSpan >> 1))
                    && (dySpan >= (sySpan >> 1));
            } else {
                ret = false;
            }
        } else {
            ret = false;
        }
        return ret;
    }
}
