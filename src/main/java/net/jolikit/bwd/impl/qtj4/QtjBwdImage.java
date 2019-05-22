/*
 * Copyright 2019 Jeff Hain
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

import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.LangUtils;

import java.util.List;

import com.trolltech.qt.gui.QImage;

public class QtjBwdImage extends AbstractBwdImage {
    
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
    private final int[] argb32Arr;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjBwdImage(
            QImage backingImage,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        this.backingImage = LangUtils.requireNonNull(backingImage);
        
        final List<Integer> colorTable = backingImage.colorTable();
        final int[] argb32Arr;
        if ((colorTable != null)
                && (colorTable.size() != 0)) {
            final int size = colorTable.size();
            argb32Arr = new int[size];
            for (int i = 0; i < size; i++) {
                argb32Arr[i] = colorTable.get(i);
            }
        } else {
            argb32Arr = null;
        }
        this.argb32Arr = argb32Arr;
        
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
        final int pixel = this.backingImage.pixel(x, y);
        final int argb32;
        if (this.argb32Arr != null) {
            argb32 = this.argb32Arr[pixel];
        } else {
            argb32 = pixel;
        }
        return argb32;
    }

    @Override
    protected void disposeImpl() {
        this.backingImage.dispose();
    }
}
