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

import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

import com.trolltech.qt.gui.QImage;

public class QtjBwdImageFromFile extends AbstractQtjBwdImage {
    
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
        
        final int width = backingImage.width();
        final int height = backingImage.height();
        this.setWidth_final(width);
        this.setHeight_final(height);
    }
    
    @Override
    public QImage getBackingImage() {
        return this.backingImage;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected int[] getPremulArgb32ArrElseNull() {
        return null;
    }

    @Override
    protected int[] getColorTableArgb32ArrElseNull() {
        return this.colorTableArgb32Arr;
    }
    
    @Override
    protected void disposeImpl() {
        this.backingImage.dispose();
    }
}
