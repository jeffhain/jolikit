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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.image.Image;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public abstract class AbstractJfxBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param disposalListener Must not be null.
     * @throws NullPointerException if disposalListener is null.
     */
    public AbstractJfxBwdImage(
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * In practice, non-null for writable images using an int array graphics
     * null for writable images using a GC graphics or for images from files.
     * 
     * @return The backing alpha premultiplied int array of ARGB pixels,
     *         or null if none.
     */
    abstract int[] getPremulArgb32ArrElseNull();
    
    /**
     * In practice, non-null for images from files,
     * null for writable images.
     * 
     * @return The backing image, or null if none.
     */
    abstract Image getBackingImageElseNull();
    
    /**
     * Must never be null.
     * 
     * For writable images using an int array graphics, the returned image
     * must never be modified once returned, since drawing images on GC
     * is asynchronous and we don't know when it's done.
     * Otherwise, if making this image red, calling GraphicsContext.drawImage()
     * with it, and then making it green and calling GraphicsContext.drawImage()
     * with it again, actual painting might correspond to drawing of two
     * green images.
     * 
     * @return A corresponding image instance, for drawing on a GC
     *         or pixels reading.
     */
    abstract Image getBackingImageForGcDrawOrRead();
}
