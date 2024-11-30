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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.utils.graphics.IntArrCopyRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.IntArrHolder;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcPixels;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawing;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Utilities to draw scaled images onto a JavaFX graphics context.
 */
public class JfxImgDrawingUtils {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final boolean canReuseInternalImage;
    
    /*
     * temps
     */
    
    private final IntArrSrcPixels tmpSrcPixels = new IntArrSrcPixels();

    private final IntArrCopyRowDrawer tmpRowDrawer = new IntArrCopyRowDrawer();
    
    private final IntArrHolder tmpIntArr1 = new IntArrHolder();
    private final IntArrHolder tmpIntArr2 = new IntArrHolder();
    
    /**
     * No need to preserve content for this buffer.
     */
    private final JfxGraphicBuffer tmpBackingBuffer = new JfxGraphicBuffer();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JfxImgDrawingUtils(boolean canReuseInternalImage) {
        this.canReuseInternalImage = canReuseInternalImage;
    }

    public void drawBackingImageOnGc(
        double dx,
        double dy,
        int dxSpan,
        int dySpan,
        //
        Image image,
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
        GraphicsContext gc) {
        
        final boolean mustUseJfxScaling =
            computeMustUseJfxScaling(
                sxSpan,
                sySpan,
                dxSpan,
                dySpan,
                scalingType,
                mustUseBackingImageScalingIfApplicable);
        if (mustUseJfxScaling) {
            drawBackingImagePartScaledOver(
                dx, dy, dxSpan, dySpan,
                //
                image,
                //
                sx, sy, sxSpan, sySpan,
                //
                gc);
        } else {
            /*
             * Scaling into a scaled buffer.
             * 
             * Using intermediate src int array, because JavaFX's PixelReader
             * getArgb() method has high overhead.
             * 
             * Using intermediate dst int array, because JavaFX's PixelWriter
             * has high overhead (fires one event per call)
             * and is not thread-safe (we might use parallelism). 
             */
            
            final int imgWidth = (int) image.getWidth();
            final int imgHeight = (int) image.getHeight();
            final int minSrcLength = imgWidth * imgHeight;
            final int[] srcArgbArr = this.tmpIntArr1.getArr(minSrcLength);
            image.getPixelReader().getPixels(
                0, 0, imgWidth, imgHeight,
                JfxPaintUtils.FORMAT_INT_ARGB_PRE,
                srcArgbArr, 0, imgWidth);
            final IntArrSrcPixels srcPixels = this.tmpSrcPixels;
            final GRect srcPixelsRect =
                GRect.valueOf(0, 0, imgWidth, imgHeight);
            srcPixels.configure(
                srcPixelsRect,
                srcArgbArr,
                imgWidth);
            
            final int minDstLength = dxSpan * dySpan;
            final int[] scaledBufferArr = this.tmpIntArr2.getArr(minDstLength);
            final IntArrCopyRowDrawer dstRowDrawer = this.tmpRowDrawer;
            dstRowDrawer.configure(
                GTransform.IDENTITY,
                scaledBufferArr,
                dxSpan);
            
            final GRect srcRect = GRect.valueOf(sx, sy, sxSpan, sySpan);
            final GRect dstRect = GRect.valueOf(0, 0, dxSpan, dySpan);
            final GRect dstClip = dstRect;
            
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
            
            /*
             * Copying the scaled buffer into a scaled image.
             */
            
            final WritableImage scaledImage =
                this.getTmpImage(dxSpan, dySpan);
            
            final int scaledBufferOffset = 0;
            final int scaledBufferScanlineStride = dxSpan;
            scaledImage.getPixelWriter().setPixels(
                0, 0, dxSpan, dySpan,
                JfxPaintUtils.FORMAT_INT_ARGB_PRE,
                scaledBufferArr,
                scaledBufferOffset,
                scaledBufferScanlineStride);
            
            /*
             * Drawing the scaled image.
             */
            
            drawBackingImageAt(
                dx, dy,
                scaledImage,
                gc);
        }
    }

    /**
     * @param dxSpan Must be >= 1, else WritableImage constructor complains.
     * @param dySpan Must be >= 1, else WritableImage constructor complains.
     * @param buffer (in)
     * @param parallelizer Parallelizer to eventually use.
     */
    public void drawIntArrBufferOnGc(
        double dx,
        double dy,
        int dxSpan,
        int dySpan,
        //
        IntArrayGraphicBuffer buffer,
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
        GraphicsContext gc) {
        
        final boolean mustUseJfxScaling =
            computeMustUseJfxScaling(
                sxSpan,
                sySpan,
                dxSpan,
                dySpan,
                scalingType,
                mustUseBackingImageScalingIfApplicable);
        if (mustUseJfxScaling) {
            /*
             * Drawing the buffer into an image.
             */
            
            final WritableImage image =
                this.getTmpImage(sxSpan, sySpan);

            final int bufferArrOffset = 0;
            image.getPixelWriter().setPixels(
                sx, sy, sxSpan, sySpan,
                JfxPaintUtils.FORMAT_INT_ARGB_PRE,
                buffer.getPixelArr(),
                bufferArrOffset,
                buffer.getScanlineStride());
            
            /*
             * Drawing the image using backing treatments,
             * eventually with scaling.
             */
            
            drawBackingImagePartScaledOver(
                dx, dy, dxSpan, dySpan,
                //
                image,
                //
                sx, sy, sxSpan, sySpan,
                //
                gc);
        } else {
            /*
             * Scaling into a scaled buffer.
             */
            
            final IntArrSrcPixels srcPixels = this.tmpSrcPixels;
            final GRect srcPixelsRect = GRect.valueOf(
                0, 0, buffer.getWidth(), buffer.getHeight());
            srcPixels.configure(
                srcPixelsRect,
                buffer.getPixelArr(),
                buffer.getScanlineStride());
            
            final int minDstLength = dxSpan * dySpan;
            final int[] scaledBufferArr = this.tmpIntArr1.getArr(minDstLength);
            final IntArrCopyRowDrawer dstRowDrawer = this.tmpRowDrawer;
            dstRowDrawer.configure(
                GTransform.IDENTITY,
                scaledBufferArr,
                dxSpan);
            
            final GRect srcRect = GRect.valueOf(sx, sy, sxSpan, sySpan);
            final GRect dstRect = GRect.valueOf(0, 0, dxSpan, dySpan);
            final GRect dstClip = dstRect;
            
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
            
            /*
             * Copying the scaled buffer into a scaled image.
             */
            
            final WritableImage scaledImage =
                this.getTmpImage(dxSpan, dySpan);

            final int scaledBufferOffset = 0;
            final int scaledBufferScanlineStride = dxSpan;
            scaledImage.getPixelWriter().setPixels(
                0, 0, dxSpan, dySpan,
                JfxPaintUtils.FORMAT_INT_ARGB_PRE,
                scaledBufferArr,
                scaledBufferOffset,
                scaledBufferScanlineStride);

            /*
             * Drawing the scaled image.
             */
            
            drawBackingImageAt(
                dx, dy,
                scaledImage,
                gc);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static boolean computeMustUseJfxScaling(
        int sxSpan,
        int sySpan,
        int dxSpan,
        int dySpan,
        BwdScalingType scalingType,
        boolean mustUseBackingImageScalingIfApplicable) {
        /*
         * TODO jfx JavaFX uses bicubic(-ish) scaling by default,.
         * Cf. https://stackoverflow.com/questions/44445072/how-to-disable-linear-filtering-on-canvas-in-javafx
         * From JavaFX 12+ (JDK-8204060), GraphicsContext.setImageSmoothing(boolean)
         * allows to change this default scaling in some way,
         * but our binding is for JavaFX8 so we can't use it yet.
         * 
         * As a result we can only use backing scaling in case
         * of BICUBIC or BOXSAMPLED_BICUBIC.
         * 
         * JavaFX bicubic appears to be faster (asynchonous so measured
         * by FPS on resizing), but does not seem to use all covered pixels
         * on downscaling, so for BICUBIC we only use it if shrinking
         * does not divide width or height by more than two,
         * and for BOXSAMPLED_BICUBIC we do the same to avoid BOXSAMPLED to kick in.
         */
        final boolean ret;
        if (mustUseBackingImageScalingIfApplicable) {
            if ((scalingType == BwdScalingType.BICUBIC)
                || (scalingType == BwdScalingType.BOXSAMPLED_BICUBIC)) {
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
    
    private WritableImage getTmpImage(int width, int height) {
        final WritableImage ret;
        if (this.canReuseInternalImage) {
            this.tmpBackingBuffer.setSize(width, height);
            ret = this.tmpBackingBuffer.getImage();
        } else {
            ret = new WritableImage(width, height);
        }
        return ret;
    }
    
    /*
     * Using simplest applicable methods,
     * in case it would help perfs.
     */
    
    private static void drawBackingImageAt(
        double dx,
        double dy,
        //
        Image image,
        //
        GraphicsContext gc) {
        
        gc.drawImage(
            image,
            //
            dx, dy);
    }
    
    /**
     * Calls drawBackingImageAt() if applicable.
     */
    private static void drawBackingImageScaledOver(
        double dx,
        double dy,
        double dxSpan,
        double dySpan,
        //
        Image image,
        //
        GraphicsContext gc) {
        
        final boolean gotImgScaling =
            (dxSpan != image.getWidth())
            || (dySpan != image.getHeight());
        if (gotImgScaling) {
            gc.drawImage(
                image,
                //
                dx, dy, dxSpan, dySpan);
        } else {
            drawBackingImageAt(
                dx, dy,
                image,
                gc);
        }
    }
    
    /**
     * Calls drawBackingImageScaledOver() if applicable.
     */
    private static void drawBackingImagePartScaledOver(
        double dx,
        double dy,
        double dxSpan,
        double dySpan,
        //
        Image image,
        //
        double sx,
        double sy,
        double sxSpan,
        double sySpan,
        //
        GraphicsContext gc) {
        
        final boolean drawingPart =
            (sx != 0)
            || (sy != 0)
            || (sxSpan != image.getWidth())
            || (sySpan != image.getHeight());
        if (drawingPart) {
            gc.drawImage(
                image,
                //
                sx, sy, sxSpan, sySpan,
                //
                dx, dy, dxSpan, dySpan);
        } else {
            drawBackingImageScaledOver(
                dx, dy, dxSpan, dySpan,
                image,
                gc);
        }
    }
}
