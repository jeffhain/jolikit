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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Abstract class for scaled rect drawers implementations
 * based on AWT drawImage().
 * 
 * Only meant for use in tests, to check that our algorithms
 * don't give something too far from AWT (when AWT is accurate),
 * and not meant to be fast and not parallelized,
 * since it's not an efficient way to use drawImage(),
 * due to row drawer overhead.
 */
public class BaseAwtSrd implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Object renderingHint;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public BaseAwtSrd(Object renderingHint) {
        this.renderingHint = renderingHint;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
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
        
        /*
         * 
         */
        
        final GRect srcPixelsRect = srcPixels.getRect();
        
        final int sw = srcRect.xSpan();
        final int sh = srcRect.ySpan();
        
        final GRect dstRectClipped = dstRect.intersected(dstClip);
        
        // Possibly null.
        final int[] srcColor32Arr = srcPixels.color32Arr();
        
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
            srcPixelArr = new int[sw * sh];
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
        
        final BihPixelFormat bufImgPixelFormat;
        if ((colorTypeHelper instanceof NonPremulArgbHelper)
            || (colorTypeHelper instanceof PremulArgbHelper)) {
            bufImgPixelFormat = BihPixelFormat.ARGB32;
        } else if (colorTypeHelper instanceof PremulNativeRgbaHelper) {
            if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
                bufImgPixelFormat = BihPixelFormat.ABGR32;
            } else {
                bufImgPixelFormat = BihPixelFormat.RGBA32;
            }
        } else {
            throw new IllegalArgumentException("" + colorTypeHelper);
        }
        final boolean bufImgPremul = colorTypeHelper.isPremul();
        
        final BufferedImage srcImg =
            BufferedImageHelper.newBufferedImageWithIntArray(
                srcPixelArr,
                srcPixelArrScanlineStride,
                //
                srcImgWidth,
                srcImgHeight,
                //
                bufImgPixelFormat,
                bufImgPremul);
        
        final int dstRectClippedArea = dstRectClipped.area();
        /*
         * If we used temps here, would not need to zeroize because we draw with
         * AlphaComposite.Src (and in cases where it's reliable).
         * 
         * The eventual blending can and must be taken care of by the row drawer.
         */
        final int[] dstPixelArr = new int[dstRectClippedArea];
        final int dstPixelArrScanlineStride = dstRectClipped.xSpan();
        final BufferedImage dstImg =
            BufferedImageHelper.newBufferedImageWithIntArray(
                dstPixelArr,
                dstPixelArrScanlineStride,
                //
                dstRectClipped.xSpan(),
                dstRectClipped.ySpan(),
                //
                bufImgPixelFormat,
                bufImgPremul);

        /*
         * scaling
         */
        
        // dstImg corresponds to the dstRectClipped.
        final GRect dstRectClippedInImg =
            dstRectClipped.withPos(0, 0);
        callDrawImage(
            srcImg,
            srcRectInImg,
            dstRectClippedInImg,
            dstImg);
        
        /*
         * writing rows
         */

        final int[] dstImgRowArr = new int[dstRectClippedInImg.xSpan()];
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
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void callDrawImage(
        BufferedImage srcImg,
        GRect srcRectInImg,
        //
        GRect dstRectClippedInImg,
        BufferedImage dstImg) {
        
        final Graphics2D g = dstImg.createGraphics();
        try {
            g.setClip(
                dstRectClippedInImg.x(),
                dstRectClippedInImg.y(),
                dstRectClippedInImg.xSpan(),
                dstRectClippedInImg.ySpan());
            
            /*
             * Only reliable if no more than two
             * of (srcX,srcY,dstX,dstY) are zero,
             * which is always the case here.
             */
            g.setComposite(AlphaComposite.Src);
            
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                this.renderingHint);
            
            /*
             * 
             */
            
            final int sx = srcRectInImg.x();
            final int sy = srcRectInImg.y();
            final int sxSpan = srcRectInImg.xSpan();
            final int sySpan = srcRectInImg.ySpan();
            
            final ImageObserver observer = null;
            g.drawImage(
                srcImg,
                0, // dx1
                0, // dy1
                dstImg.getWidth(), // dx2 (exclusive)
                dstImg.getHeight(), // dy2 (exclusive)
                sx, // sx1
                sy, // sy1
                sx + sxSpan, // sx2 (exclusive)
                sy + sySpan, // sy2 (exclusive)
                observer);
        } finally {
            g.dispose();
        }
    }
}
