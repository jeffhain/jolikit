/*
 * Copyright 2019-2025 Jeff Hain
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
package net.jolikit.bwd.impl.awt;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class AwtBwdWritableImage extends AbstractAwtBwdImage implements InterfaceBwdWritableImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final BufferedImageHelper bufferedImageHelperArgbPre;
    
    private final int[] premulArgb32Arr;

    private final InterfaceBwdGraphics graphics;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param width Image width. Must be > 0.
     * @param height Image height. Must be > 0.
     * @throws NullPointerException if binding or disposalListener is null.
     * @throws IllegalArgumentException if width or height is <= 0.
     */
    public AwtBwdWritableImage(
            AbstractAwtBwdBinding binding,
            int width,
            int height,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        this.checkAndSetWritableImageDims(width, height);
        
        final boolean isImageGraphics = true;
        final GRect box = this.getRect();
        
        final int pixelCapacity = box.area();
        final int[] premulArgb32Arr = new int[pixelCapacity];
        final int scanlineStride = width;
        final BufferedImageHelper backingHelper =
            new BufferedImageHelper(
                premulArgb32Arr,
                scanlineStride,
                //
                width,
                height,
                //
                AwtUtils.COMMON_BUFFERED_IMAGE_PIXEL_FORMAT_ARGB,
                AwtUtils.COMMON_BUFFERED_IMAGE_PREMUL);
        this.bufferedImageHelperArgbPre = backingHelper;

        this.premulArgb32Arr = premulArgb32Arr;
        
        final InterfaceBwdGraphics graphics =
            new AwtBwdGraphics(
                binding,
                box,
                //
                isImageGraphics,
                backingHelper);
        this.graphics = graphics;
        
        graphics.init();
    }

    @Override
    public InterfaceBwdGraphics getGraphics() {
        return this.graphics;
    }
    
    @Override
    public BufferedImageHelper getBufferedImageHelperArgbPre() {
        return this.bufferedImageHelperArgbPre;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void disposeImpl() {
        this.graphics.finish();
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    @Override
    int[] getPremulArgb32Arr() {
        return this.premulArgb32Arr;
    }
}
