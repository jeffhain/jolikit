/*
 * Copyright 2019-2020 Jeff Hain
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

import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class QtjBwdImageFromFile extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final QImage backingImage;
    
    /**
     * If the backing image has a color table,
     * it's copied into this array (to make it memory-friendly),
     * and backing image's pixel(...) value is the index to use in this table.
     * Else, this array is null.
     */
    private final int[] colorTableArgb32Arr;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param filePath Path of an image file.
     * @param disposalListener Must not be null.
     * @throws NullPointerException if filePath or disposalListener is null.
     * @throws IllegalArgumentException if could not load the specified image.
     */
    public QtjBwdImageFromFile(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        // TODO qtj Called fileName, but is actually a file path.
        final String fileName = filePath;
        final QImage backingImage = new QImage(fileName);
        // Not supporting asynchronous image loads
        // (for our UI API, asynchronous network load
        // is not the job of the UI library).
        if ((backingImage.width() == 0)
                || (backingImage.height() == 0)) {
            throw new IllegalArgumentException(
                    "could not properly load image at "
                            + filePath
                            + " (zero width or height)");
        }
        this.backingImage = backingImage;
        
        final List<Integer> colorTable = backingImage.colorTable();
        final int[] colorTableArgb32Arr;
        if ((colorTable != null)
                && (colorTable.size() != 0)) {
            final int size = colorTable.size();
            colorTableArgb32Arr = new int[size];
            for (int i = 0; i < size; i++) {
                colorTableArgb32Arr[i] = colorTable.get(i);
            }
        } else {
            colorTableArgb32Arr = null;
        }
        this.colorTableArgb32Arr = colorTableArgb32Arr;
        
        this.setWidth_final(backingImage.width());
        this.setHeight_final(backingImage.height());
    }
    
    public QImage getBackingImage() {
        return this.backingImage;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final QImage backingImage = this.getBackingImage();
        final int pixelOrIndex = backingImage.pixel(x, y);
        final int[] colorTableArgb32Arr = this.colorTableArgb32Arr;
        /*
         * TODO qtj For 4 bits BMP images (at least), can have a color table,
         * but QImage.pixel(int,int) returning an actual color (from the table)
         * instead of an index for the table.
         * As best effort workaround, if the value is out of color table bounds,
         * we assume it's a pixel value.
         */
        final int pixel;
        if ((colorTableArgb32Arr != null)
                && (pixelOrIndex >= 0)
                && (pixelOrIndex < colorTableArgb32Arr.length)) {
            pixel = colorTableArgb32Arr[pixelOrIndex];
        } else {
            pixel = pixelOrIndex;
        }
        final Format format = backingImage.format();
        final int backingArgb32 = QtjPixelFormatUtils.toArgb32(format, pixel);
        final int argb32;
        if (QtjPixelFormatUtils.isAlphaPremultiplied(format)) {
            argb32 = BindingColorUtils.toNonPremulAxyz32(backingArgb32);
        } else {
            argb32 = backingArgb32;
        }
        return argb32;
    }

    @Override
    protected void disposeImpl() {
        this.backingImage.dispose();
    }
}
