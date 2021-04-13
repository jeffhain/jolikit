/*
 * Copyright 2019-2021 Jeff Hain
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

import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class QtjBwdImageFromFile extends AbstractQtjBwdImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final QtjImageHelper backingImageHelper;
    
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
        
        this.backingImageHelper = new QtjImageHelper(backingImage);
        
        final int width = backingImage.width();
        final int height = backingImage.height();
        this.setWidth_final(width);
        this.setHeight_final(height);
    }
    
    @Override
    public QtjImageHelper getBackingImageHelper() {
        return this.backingImageHelper;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final QtjImageHelper helper = this.getBackingImageHelper();
        return helper.getSnapshotNonPremulArgb32At(x, y);
    }
    
    @Override
    protected void disposeImpl() {
        this.backingImageHelper.getImage().dispose();
    }
}
