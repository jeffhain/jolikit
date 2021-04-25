/*
 * Copyright 2021 Jeff Hain
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
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.graphics.IntArrHolder;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcOverRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcPixels;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawer;
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
     * @param transform Transform from base to user.
     * @param dstImageHelper Helper for the image backing the specified graphics.
     *        Must be backed by an alpha-premultipled ARGB32 array.
     * @param g Graphics to draw into. Only used when not drawing through
     *        destination image helper.
     */
    public void drawBufferedImageOnG(
        int xShiftInUser,
        int yShiftInUser,
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
        boolean accurateImageScaling,
        //
        BufferedImageHelper dstImageHelper,
        Graphics2D g) {
        
        final boolean mustUseRedefinedScaling =
            accurateImageScaling
            && (!ScaledRectDrawer.isExactWithClosest(
                sxSpan, sySpan,
                dxSpan, dySpan));
        if (mustUseRedefinedScaling) {
            final BufferedImage dstImage = dstImageHelper.getImage();
            {
                final boolean withAlpha = true;
                final boolean premul = true;
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
                true);
            final IntArrSrcPixels srcPixels = this.tmpSrcPixels;
            srcPixels.configure(
                sxSpan,
                sySpan,
                srcPremulArgb32Arr,
                sxSpan);
            
            final GRect srcRectInImg = GRect.valueOf(0, 0, sxSpan, sySpan);
            final GRect dstRectInUser = GRect.valueOf(dx, dy, dxSpan, dySpan);
            final GRect dstClipInUser = clipInUser;
            
            final IntArrSrcOverRowDrawer rowDrawer = this.tmpRowDrawer;
            final int[] dstPremulArgb32Arr =
                BufferedImageHelper.getIntPixelArr(dstImage);
            rowDrawer.configure(
                transform,
                dstPremulArgb32Arr,
                dstImage.getWidth());
            
            ScaledRectDrawer.drawRectScaled(
                parallelizer,
                accurateImageScaling,
                srcPixels,
                srcRectInImg,
                dstRectInUser,
                dstClipInUser,
                rowDrawer);
        } else {
            final int _x = dx + xShiftInUser;
            final int _y = dy + yShiftInUser;
            
            final ImageObserver observer = null;
            
            /*
             * Using simplest applicable method,
             * in case it would help perfs.
             */
            
            final boolean drawingPart =
                (sx != 0)
                || (sy != 0)
                || (sxSpan != srcImage.getWidth())
                || (sySpan != srcImage.getHeight());
            if (drawingPart) {
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
                final boolean scaling =
                    (dxSpan != sxSpan)
                    || (dySpan != sySpan);
                if (scaling) {
                    g.drawImage(srcImage, _x, _y, dxSpan, dySpan, observer);
                } else {
                    g.drawImage(srcImage, _x, _y, observer);
                }
            }
        }
    }
}
