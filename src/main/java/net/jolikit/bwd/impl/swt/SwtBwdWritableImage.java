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
package net.jolikit.bwd.impl.swt;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.NumbersUtils;

import org.eclipse.swt.widgets.Display;

public class SwtBwdWritableImage extends AbstractSwtBwdImage implements InterfaceBwdWritableImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int[] premulArgb32Arr;

    private final InterfaceBwdGraphics graphics;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param width Image width. Must be > 0.
     * @param height Image height. Must be > 0.
     * @param display Is also the device.
     * @throws NullPointerException if binding or display or disposalListener is null.
     * @throws IllegalArgumentException if width or height is <= 0.
     */
    public SwtBwdWritableImage(
            InterfaceBwdBinding binding,
            int width,
            int height,
            Display display,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        this.checkAndSetWritableImageDims(width, height);
        
        final int pixelCapacity = NumbersUtils.timesExact(width, height);
        final int[] premulArgb32Arr = new int[pixelCapacity];
        this.premulArgb32Arr = premulArgb32Arr;
        
        final boolean isImageGraphics = true;
        final GRect box = this.getRect();
        final int[] pixelArr = premulArgb32Arr;
        final int pixelArrScanlineStride = width;
        final SwtBwdGraphics graphics = new SwtBwdGraphics(
                binding,
                display,
                isImageGraphics,
                box,
                pixelArr,
                pixelArrScanlineStride);
        this.graphics = graphics;
        
        graphics.init();
    }
    
    @Override
    public InterfaceBwdGraphics getGraphics() {
        return this.graphics;
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
