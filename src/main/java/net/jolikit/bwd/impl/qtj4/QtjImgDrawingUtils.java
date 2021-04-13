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
package net.jolikit.bwd.impl.qtj4;

import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QTransform;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.graphics.IntArrHolder;
import net.jolikit.bwd.impl.utils.graphics.IntArrRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcPixels;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawer;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Utilities to draw scaled images onto a Qt painter.
 */
public class QtjImgDrawingUtils {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * temps
     */
    
    private final IntArrSrcPixels tmpSrcPixels = new IntArrSrcPixels();
    
    private final IntArrRowDrawer tmpRowDrawer = new IntArrRowDrawer();
    
    private final IntArrHolder tmpIntArr = new IntArrHolder();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjImgDrawingUtils() {
    }
    
    public void drawBackingImageOnPainter(
        int xShiftInUser,
        int yShiftInUser,
        QTransform backingTransform,
        QTransform tmpBackingTransform,
        //
        int dx,
        int dy,
        int dxSpan,
        int dySpan,
        //
        QtjImageHelper imageHelper,
        //
        int sx,
        int sy,
        int sxSpan,
        int sySpan,
        //
        InterfaceParallelizer parallelizer,
        boolean mustEnsureSmoothImageScaling,
        //
        QPainter painter) {
        
        final QImage image = imageHelper.getImage();
        
        if (mustEnsureSmoothImageScaling) {
            
            /*
             * Scaling into a scaled buffer.
             * 
             * Doing scaling on alpha-premultipled pixels.
             * Less accurate, but faster to read/write pixels
             * for the common case of alpha-premultiplied images,
             * and we need perfs badly with QImage slowness.
             */
            final int[] srcArgbArr = imageHelper.getSnapshotPremulArgb32Arr(
                sx, sy, sxSpan, sySpan);
            final int srcScanlineStride = imageHelper.getSnapshotScanlineStride();
            final IntArrSrcPixels srcPixels = this.tmpSrcPixels;
            srcPixels.configure(
                sxSpan,
                sySpan,
                srcArgbArr,
                srcScanlineStride);
            
            final int minDstLength = dxSpan * dySpan;
            final int[] scaledBufferArr = this.tmpIntArr.getArr(minDstLength);
            final IntArrRowDrawer rowDrawer = this.tmpRowDrawer;
            rowDrawer.configure(scaledBufferArr, dxSpan);
            
            final GRect srcRect = GRect.valueOf(sx, sy, sxSpan, sySpan);
            final GRect dstRect = GRect.valueOf(0, 0, dxSpan, dySpan);
            final GRect dstClip = dstRect;
            ScaledRectDrawer.drawRectScaled(
                parallelizer,
                mustEnsureSmoothImageScaling,
                srcPixels,
                srcRect,
                dstRect,
                dstClip,
                rowDrawer);
            
            /*
             * Copying the scaled buffer into a scaled image.
             */
            
            final QImage scaledImage =
                new QImage(
                    dxSpan,
                    dySpan,
                    QtjPaintUtils.FORMAT_ARGB_PRE);
            final QtjImageHelper scaledImageHelper = new QtjImageHelper(scaledImage);
            scaledImageHelper.setImgPremulArgb32Over(
                0, 0, dxSpan, dySpan,
                scaledBufferArr,
                dxSpan);
            
            /*
             * Drawing the scaled image.
             */
            
            final int drawX = dx + xShiftInUser;
            final int drawY = dy + yShiftInUser;
            painter.drawImage(drawX, drawY, scaledImage);
            
            scaledImage.dispose();
        } else {
            final int drawX;
            final int drawY;
            final boolean scaling = (dxSpan != sxSpan) || (dySpan != sySpan);
            if (scaling) {
                final GPoint drawPos = setTransformAndGetDrawPosWhenScaling(
                    xShiftInUser,
                    yShiftInUser,
                    tmpBackingTransform,
                    //
                    dx, dy, dxSpan, dySpan,
                    //
                    image,
                    //
                    sxSpan, sySpan,
                    //
                    painter);
                
                drawX = drawPos.x();
                drawY = drawPos.y();
            } else {
                drawX = dx + xShiftInUser;
                drawY = dy + yShiftInUser;
            }
            try {
                this.drawBackingImageOnPainter_painterAlgo(
                    scaling,
                    drawX,
                    drawY,
                    //
                    dx, dy, dxSpan, dySpan,
                    //
                    image,
                    //
                    sx, sy, sxSpan, sySpan,
                    //
                    painter);
            } finally {
                if (scaling) {
                    painter.setTransform(backingTransform);
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void drawBackingImageOnPainter_painterAlgo(
        boolean scaling,
        int drawX,
        int drawY,
        //
        int dx,
        int dy,
        int dxSpan,
        int dySpan,
        //
        QImage image,
        //
        int sx,
        int sy,
        int sxSpan,
        int sySpan,
        //
        QPainter painter) {
        
        final int imageWidth = image.width();
        final int imageHeight = image.height();
        
        /*
         * Using simplest applicable method,
         * in case it would help perfs.
         */
        
        final boolean drawingPart =
            (sx != 0)
            || (sy != 0)
            || (sxSpan != imageWidth)
            || (sySpan != imageHeight);
        if (drawingPart || scaling) {
            painter.drawImage(
                drawX, drawY,
                image,
                sx, sy, sxSpan, sySpan);
        } else {
            painter.drawImage(drawX, drawY, image);
        }
    }
    
    /**
     * For use when there is image scaling,
     * and when using QPainter's image scaling algorithm.
     * 
     * @param painter (in,out)
     */
    private static GPoint setTransformAndGetDrawPosWhenScaling(
        int xShiftInUser,
        int yShiftInUser,
        QTransform tmpBackingTransform,
        //
        int dx,
        int dy,
        int dxSpan,
        int dySpan,
        //
        QImage image,
        //
        int sxSpan,
        int sySpan,
        //
        QPainter painter) {
        
        final QTransform backingTransformToCombine = tmpBackingTransform;
        backingTransformToCombine.reset();
        
        final double xScale = (dxSpan / (double) sxSpan);
        final double yScale = (dySpan / (double) sySpan);
        
        /*
         * TODO qtj Seems we need these complicated shift hacks
         * to get proper results, at least for drawn image bounds.
         */
        
        {
            final int specialXShiftInUserForTransform = xShiftInUser + 1;
            final int specialYShiftInUserForTransform = yShiftInUser + 1;
            backingTransformToCombine.translate(
                specialXShiftInUserForTransform,
                specialYShiftInUserForTransform);
        }
        
        backingTransformToCombine.scale(xScale, yScale);
        
        final int specialXShiftInUser = -1;
        final int specialYShiftInUser = -1;
        
        final int drawX = BindingCoordsUtils.ceilToInt((1.0/xScale) * (dx + specialXShiftInUser));
        final int drawY = BindingCoordsUtils.ceilToInt((1.0/yScale) * (dy + specialYShiftInUser));
        
        final boolean combine = true;
        painter.setTransform(backingTransformToCombine, combine);
        
        return GPoint.valueOf(drawX, drawY);
    }
}
