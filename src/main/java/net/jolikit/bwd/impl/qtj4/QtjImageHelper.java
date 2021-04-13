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

import java.util.List;

import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QImage.Format;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.graphics.PixelFormatConverter;
import net.jolikit.lang.NbrsUtils;

/**
 * Only allows color setting into images that have no color table.
 * 
 * TODO qtj Qt docs say that QImage.pixel(int,int)
 * and QImage.setPixel(int,int,int) are slow
 * and that QNativePointer is preferable,
 * but at least from Java it's clearly the other way around
 * (some bench taking 20ms with pixel()/setPixel(),
 * of which only a small part is due to these methods,
 * and 65ms with QNativePointer.byteAt()/setByteAt()).
 * ===> In this class, we stick to QImage.pixel()/setPixel().
 * 
 * That said, QImage.pixel() still being kind of slow,
 * to avoid its overhead when needing a fresh int array of pixels
 * (such as when doing smooth scaling), this class maintains
 * an alpha-premultiplied ARGB32 snapshot of image.
 */
public class QtjImageHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final QImage image;
    
    private final boolean premul;
    
    private final PixelFormatConverter converter;
    
    /*
     * For fast access to width/height.
     */
    
    private final int width;
    private final int height;
    
    /**
     * Non-null if a non-empty color table, which all image pixels are
     * indexes of, has been found, else null and QImage is considered
     * to directly contain pixel values in QImage's format.
     */
    private final int[] xxxArgb32ByColorIndex;
    
    /*
     * Java alpha-premultiplied ARGB32 pixels cache.
     */
    
    private int[] snapshotPremulArgb32Arr;
    
    private GRect snapshotDirtyBox;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjImageHelper(QImage image) {
        // Implicit null check.
        final Format format = image.format();
        
        this.image = image;
        
        this.premul = QtjPixelFormatUtils.isAlphaPremultiplied(format);
        
        this.converter = QtjPixelFormatUtils.getConverter(format);
        
        final int w = image.width();
        final int h = image.height();
        this.width = w;
        this.height = h;
        
        // Need to call that last, since uses other fields.
        this.xxxArgb32ByColorIndex = this.computeXxxArgb32ByColorIndex(image);
        
        // Snapshot created lazily: all dirty by default.
        this.snapshotDirtyBox = GRect.valueOf(0, 0, w, h);
    }
    
    public boolean isPremul() {
        return this.premul;
    }
    
    /**
     * After modifying this image, must call setReadOnlyCacheDirty(),
     * unless you modify it through methods of this class.
     */
    public QImage getImage() {
        return this.image;
    }
    
    public int getWidth() {
        return this.width;
    }
    
    public int getHeight() {
        return this.height;
    }
    
    /*
     * Methods for reading from and writing into image.
     */
    
    /**
     * Reads from image.
     * 
     * @return ARGB32 premultiplited or not depending on isPremul().
     */
    public int getImgXxxArgb32At(int x, int y) {
        final int pixelOrIndex = this.image.pixel(x, y);
        final int ret;
        if (this.xxxArgb32ByColorIndex != null) {
            ret = this.xxxArgb32ByColorIndex[pixelOrIndex];
        } else {
            ret = this.pixelToXxxArgb32(pixelOrIndex);
        }
        return ret;
    }
    
    /**
     * Reads from image.
     */
    public int getImgPremulArgb32At(int x, int y) {
        final int xxxArgb32 = this.getImgXxxArgb32At(x, y);
        return this.argb32XxxToPremul(xxxArgb32);
    }
    
    /**
     * Reads from image.
     */
    public int getImgNonPremulArgb32At(int x, int y) {
        final int xxxArgb32 = this.getImgXxxArgb32At(x, y);
        return this.argb32XxxToNonPremul(xxxArgb32);
    }
    
    /**
     * Reads from image.
     */
    public void getImgPremulArgb32Over(
        int x,
        int y,
        int xSpan,
        int ySpan,
        int[] dstPremulArgb32Arr,
        int dstScanlineStride) {

        for (int j = 0; j < ySpan; j++) {
            final int py = y + j;
            final int offset = py * dstScanlineStride;
            for (int i = 0; i < xSpan; i++) {
                final int px = x + i;
                final int index = offset + px;
                dstPremulArgb32Arr[index] = this.getImgPremulArgb32At(px, py);
            }
        }
    }

    /**
     * Writes into image, and updates snapshot dirty box.
     */
    public void setImgPremulArgb32Over(
        int x,
        int y,
        int xSpan,
        int ySpan,
        int[] srcPremulArgb32Arr,
        int srcScanlineStride) {

        for (int j = 0; j < ySpan; j++) {
            final int py = y + j;
            final int offset = py * srcScanlineStride;
            for (int i = 0; i < xSpan; i++) {
                final int px = x + i;
                final int index = offset + px;
                final int premulArgb32 = srcPremulArgb32Arr[index];
                this.setImgPremulArgb32At_noDirty(px, py, premulArgb32);
            }
        }
        
        this.ensureSnapshotDirtyOver(x, y, xSpan, ySpan);
    }

    /*
     * Methods for making snapshot dirty.
     */
    
    /**
     * Only growing the dirty box within image rect.
     * This allows to avoid overflows in case of huge positions.
     */
    public void ensureSnapshotDirtyOver(int x, int y) {
        /*
         * toRange(...) only works for non-zero spans.
         */
        x = NbrsUtils.toRange(0, this.width - 1, x);
        y = NbrsUtils.toRange(0, this.height - 1, y);
        this.snapshotDirtyBox = this.snapshotDirtyBox.unionBoundingBox(x, y);
    }

    /**
     * Only growing the dirty box within image rect.
     * This allows to avoid overflows in case of huge positions.
     */
    public void ensureSnapshotDirtyOver(
            int x,
            int y,
            int xSpan,
            int ySpan) {
        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }
        
        // Dirty box is rectangular, so only need
        // to use two extremities on a diagonal.
        
        // Top-left.
        this.ensureSnapshotDirtyOver(x, y);
        
        // Bottom-right.
        final int xMax = (int) Math.min(Integer.MAX_VALUE, x + (long) xSpan + 1);
        final int yMax = (int) Math.min(Integer.MAX_VALUE, y + (long) ySpan + 1);
        this.ensureSnapshotDirtyOver(xMax, yMax);
    }

    /**
     * To be called whenever some point is drawn.
     * 
     * @param transform Transform between image frame (1) and user frame (2).
     */
    public void onPointDrawing(GTransform transform, int x, int y) {
        
        final int xInImg = transform.xIn1(x, y);
        final int yInImg = transform.yIn1(x, y);
        this.ensureSnapshotDirtyOver(xInImg, yInImg);
    }

    /**
     * To be called whenever some line is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onLineDrawing(GTransform transform, int x1, int y1, int x2, int y2) {
        
        final int x1InImg = transform.xIn1(x1, y1);
        final int y1InImg = transform.yIn1(x1, y1);
        final int x2InImg = transform.xIn1(x2, y2);
        final int y2InImg = transform.yIn1(x2, y2);
        this.ensureSnapshotDirtyOver(x1InImg, y1InImg);
        this.ensureSnapshotDirtyOver(x2InImg, y2InImg);
    }

    /**
     * To be called whenever some rectangle is drawn.
     * 
     * @param transform Transform between image frame (1) and user frame (2).
     */
    public void onRectDrawing(GTransform transform, int x, int y, int xSpan, int ySpan) {
        
        final GRotation rotation = transform.rotation();
        
        final int xInImg = transform.minXIn1(x, y, xSpan, ySpan);
        final int yInImg = transform.minYIn1(x, y, xSpan, ySpan);
        final int xSpanInImg = rotation.xSpanInOther(xSpan, ySpan);
        final int ySpanInImg = rotation.ySpanInOther(xSpan, ySpan);
        
        this.ensureSnapshotDirtyOver(
            xInImg,
            yInImg,
            xSpanInImg,
            ySpanInImg);
    }
    
    /**
     * To be called whenever some text is drawn.
     * 
     * @param transform Transform between image frame (1) and user frame (2).
     */
    public void onTextDrawing(
            GTransform transform,
            int x,
            int y,
            String text,
            InterfaceBwdFont font) {
        
        /*
         * TODO qtj This supposes that text will actually
         * not leak outside its theoretical box.
         */
        
        final InterfaceBwdFontMetrics metrics = font.metrics();
        final int xSpan = metrics.computeTextWidth(text);
        final int ySpan = metrics.height();
        
        this.onRectDrawing(transform, x, y, xSpan, ySpan);
    }

    /*
     * Methods for updating and reading from snapshot.
     */

    /**
     * @return Premultiplied ARGB32 read from snapshot,
     *         after having ensured that it was up to date at
     *         the specified pixel.
     */
    public int getSnapshotPremulArgb32At(int x, int y) {
        this.ensureSnapshotUpToDateOver(x, y);
        
        final int index = y * this.width + x;
        return this.snapshotPremulArgb32Arr[index];
    }

    /**
     * @return Non-premultiplied ARGB32 read from snapshot,
     *         after having ensured that it was up to date at
     *         the specified pixel.
     */
    public int getSnapshotNonPremulArgb32At(int x, int y) {
        final int premulArgb32 = this.getSnapshotPremulArgb32At(x, y);
        return BindingColorUtils.toNonPremulAxyz32(premulArgb32);
    }

    /**
     * @return Snapshot array, after having ensured that
     *         it was up to date over the specified rectangle.
     */
    public int[] getSnapshotPremulArgb32Arr(int x, int y, int xSpan, int ySpan) {
        this.ensureSnapshotUpToDateOver(x, y, xSpan, ySpan);
        return this.snapshotPremulArgb32Arr;
    }
    
    public int getSnapshotScanlineStride() {
        return this.width;
    }
    
    /*
     * Methods reading from snapshot and writing into image.
     */
    
    /**
     * Reads from snapshot, writes into image,
     * and updates snapshot dirty box.
     */
    public void invertPixels(int x, int y, int xSpan, int ySpan) {
        for (int j = 0; j < ySpan; j++) {
            final int py = y + j;
            for (int i = 0; i < xSpan; i++) {
                final int px = x + i;
                final int argb32 = this.getSnapshotNonPremulArgb32At(px, py);
                final int invertedArgb32 =
                        Argb32.inverted(argb32);
                this.setImgNonPremulArgb32At_noDirty(px, py, invertedArgb32);
            }
        }
        
        this.ensureSnapshotDirtyOver(x, y, xSpan, ySpan);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Null if could not compute a non-empty and valid color table.
     */
    private int[] computeXxxArgb32ByColorIndex(QImage image) {
        
        final int w = image.width();
        final int h = image.height();
        
        int[] ret = null;
        
        final List<Integer> colorTable = image.colorTable();
        if (colorTable != null) {
            final int size = colorTable.size();
            if (size != 0) {
                ret = new int[size];
                for (int i = 0; i < size; i++) {
                    final int pixel = colorTable.get(i).intValue();
                    ret[i] = this.pixelToXxxArgb32(pixel);
                }
                
                LoopJ : for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        final int index = image.pixel(i, j);
                        /*
                         * TODO qtj For 4 bits BMP images (at least), can have a color table,
                         * but QImage.pixel(int,int) returning an actual color (from the table)
                         * instead of an index for the table.
                         * As best effort workaround, if the value is out of color table bounds,
                         * we assume it's a pixel value.
                         */
                        if ((index >= 0)
                            && (index < ret.length)) {
                            // ok
                        } else {
                            ret = null;
                            break LoopJ;
                        }
                    }
                }
            }
        }
        
        return ret;
    }
    
    /*
     * 
     */
    
    private int argb32XxxToPremul(int xxxArgb32) {
        final int ret;
        if (this.premul) {
            ret = xxxArgb32;
        } else {
            ret = BindingColorUtils.toPremulAxyz32(xxxArgb32);
        }
        return ret;
    }

    private int argb32XxxToNonPremul(int xxxArgb32) {
        final int ret;
        if (this.premul) {
            ret = BindingColorUtils.toNonPremulAxyz32(xxxArgb32);
        } else {
            ret = xxxArgb32;
        }
        return ret;
    }

    private int argb32PremulToXxx(int premulArgb32) {
        final int ret;
        if (this.premul) {
            ret = premulArgb32;
        } else {
            ret = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
        }
        return ret;
    }

    private int argb32NonPremulToXxx(int argb32) {
        final int ret;
        if (this.premul) {
            ret = BindingColorUtils.toPremulAxyz32(argb32);
        } else {
            ret = argb32;
        }
        return ret;
    }

    /*
     * Snapshot making.
     */
    
    private void ensureSnapshotUpToDateOver(int x, int y) {
        final GRect oldDirty = this.snapshotDirtyBox;
        if (oldDirty.contains(x, y)) {
            /*
             * Could update just a part of dirty box,
             * but for simplicity just updating all of it.
             */
            this.updateSnapshotOver(oldDirty);
            this.snapshotDirtyBox = GRect.DEFAULT_EMPTY;
        }
    }
    
    private void ensureSnapshotUpToDateOver(int x, int y, int xSpan, int ySpan) {
        final GRect oldDirty = this.snapshotDirtyBox;
        if (oldDirty.overlaps(
            x, y, xSpan, ySpan)) {
            /*
             * Could update just a part of dirty box,
             * but for simplicity just updating all of it.
             */
            this.updateSnapshotOver(oldDirty);
            this.snapshotDirtyBox = GRect.DEFAULT_EMPTY;
        }
    }
    
    private void updateSnapshotOver(GRect rect) {
        if (this.snapshotPremulArgb32Arr == null) {
            this.snapshotPremulArgb32Arr = new int[this.width * this.height];
            // First dirty box covers the whole image,
            // so all of the array will be properly initialized.
        }
        /*
         * Could update just a part of dirty box,
         * but for simplicity just updating all of it.
         */
        this.getImgPremulArgb32Over(
            rect.x(),
            rect.y(),
            rect.xSpan(),
            rect.ySpan(),
            this.snapshotPremulArgb32Arr,
            this.width);
    }
    
    /*
     * Image/raw methods.
     */
    
    private int pixelToXxxArgb32(int pixel) {
        return this.converter.toArgb32(pixel);
    }
    
    private int xxxArgb32ToPixel(int xxxArgb32) {
        return this.converter.toPixel(xxxArgb32);
    }

    /**
     * Writes into image, and does not update dirty box.
     */
    private void setPixelOrIndexAt_noDirty(int x, int y, int pixelOrIndex) {
        this.image.setPixel(x, y, pixelOrIndex);
    }
    
    /**
     * Writes into image, and does not update dirty box.
     */
    private void setImgXxxArgb32At_noDirty(int x, int y, int xxxArgb32) {
        if (this.xxxArgb32ByColorIndex != null) {
            throw new UnsupportedOperationException(
                "not supporting write for images with color table");
        } else {
            final int pixel = this.xxxArgb32ToPixel(xxxArgb32);
            this.setPixelOrIndexAt_noDirty(x, y, pixel);
        }
    }
    
    /**
     * Writes into image, and does not update dirty box.
     */
    private void setImgPremulArgb32At_noDirty(int x, int y, int premulArgb32) {
        final int xxxArgb32 = this.argb32PremulToXxx(premulArgb32);
        this.setImgXxxArgb32At_noDirty(x, y, xxxArgb32);
    }
    
    /**
     * Writes into image, and does not update dirty box.
     */
    private void setImgNonPremulArgb32At_noDirty(int x, int y, int argb32) {
        final int xxxArgb32 = this.argb32NonPremulToXxx(argb32);
        this.setImgXxxArgb32At_noDirty(x, y, xxxArgb32);
    }
}
