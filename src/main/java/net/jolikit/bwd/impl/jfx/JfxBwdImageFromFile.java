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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.image.Image;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class JfxBwdImageFromFile extends AbstractJfxBwdImage {
    
    /*
     * Storing pixels as original Image, not as an int array of pixels,
     * for if user would prefer that (for example for performances
     * when drawing on an int array graphics), he can always write it
     * on a writable image configured to use int arrays.
     */
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Image backingImage;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param filePath Path of an image file.
     * @param disposalListener Must not be null.
     * @throws NullPointerException if filePath or disposalListener is null.
     * @throws IllegalArgumentException if could not load the specified image.
     */
    public JfxBwdImageFromFile(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        final Image backingImage = new Image("file:" + filePath);
        final Exception exception = backingImage.getException();
        if (exception != null) {
            /*
             * "com.sun.javafx.iio.ImageStorageException: No loader for image data"
             */
            throw new IllegalArgumentException(
                    "could not load image at "
                            + filePath, exception);
        }
        
        /**
         * TODO jfx For some images types, the backing image
         * is properly created, but drawing does nothing.
         * As it turns out, such backing images have width
         * and height of zero.
         * To detect these non-supported cases (for which we must throw),
         * we just throw whenever width or height is 0.
         */
        if ((backingImage.getWidth() == 0)
                || (backingImage.getHeight() == 0)) {
            throw new IllegalArgumentException(
                    "could not load image at "
                            + filePath
                            + " (zero width or height)");
        }

        this.backingImage = backingImage;
        final int width = BindingCoordsUtils.ceilToInt(backingImage.getWidth());
        final int height = BindingCoordsUtils.ceilToInt(backingImage.getHeight());
        this.setWidth_final(width);
        this.setHeight_final(height);
    }
    
    public Image getBackingImage() {
        return this.backingImage;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        return this.backingImage.getPixelReader().getArgb(x, y);
    }

    @Override
    protected void disposeImpl() {
        // Nothing to do.
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    @Override
    int[] getPremulArgb32ArrElseNull() {
        return null;
    }
    
    @Override
    Image getBackingImageElseNull() {
        return this.backingImage;
    }
    
    @Override
    Image getBackingImageForGcDrawOrRead() {
        return this.backingImage;
    }
}
