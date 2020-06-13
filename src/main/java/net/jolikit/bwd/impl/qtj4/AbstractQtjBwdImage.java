/*
 * Copyright 2020 Jeff Hain
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

import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QImage.Format;

public abstract class AbstractQtjBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param disposalListener Must not be null.
     * @throws NullPointerException if disposalListener is null.
     */
    public AbstractQtjBwdImage(
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
    }
    
    public abstract QImage getBackingImage();

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Can be null.
     */
    protected abstract int[] getPremulArgb32ArrElseNull();
    
    /**
     * If the backing image has a color table,
     * it's copied into this array (to make it memory-friendly),
     * and backing image's pixel(...) value is the index to use in this table.
     * Else, this array is null.
     * 
     * @return Can be null.
     */
    protected abstract int[] getColorTableArgb32ArrElseNull();

    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int[] premulArgb32Arr = this.getPremulArgb32ArrElseNull();
        final int argb32;
        if (premulArgb32Arr != null) {
            final int index = y * this.getWidth() + x;
            final int premulArgb32 = premulArgb32Arr[index];
            argb32 = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
        } else {
            final QImage backingImage = this.getBackingImage();
            final int pixelOrIndex = backingImage.pixel(x, y);
            final int[] colorTableArgb32Arr = this.getColorTableArgb32ArrElseNull();
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
            if (QtjPixelFormatUtils.isAlphaPremultiplied(format)) {
                argb32 = BindingColorUtils.toNonPremulAxyz32(backingArgb32);
            } else {
                argb32 = backingArgb32;
            }
        }
        return argb32;
    }
}
