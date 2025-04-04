/*
 * Copyright 2020-2025 Jeff Hain
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

import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public abstract class AbstractAwtBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param disposalListener Must not be null.
     * @throws NullPointerException if disposalListener is null.
     */
    public AbstractAwtBwdImage(
        InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
    }
    
    /**
     * For read only purpose.
     * 
     * Using TYPE_INT_ARGB_PRE format for both accuracy and speed
     * when using drawImage() directly, possibly with other images
     * of same type (like graphics backing image/array).
     * 
     * Not providing access to the originally loaded image
     * if it was not (ARGB,premul), to have it garbaged and save memory.
     * 
     * @return Helper for the internal (ARGB,premul) copy of the image,
     *         or for the image itself if it was already (ARGB,premul).
     */
    public abstract BufferedImageHelper getBufferedImageHelperArgbPre();
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int[] premulArgb32Arr = this.getPremulArgb32Arr();
        final int index = y * this.getWidth() + x;
        final int premulArgb32 = premulArgb32Arr[index];
        return BindingColorUtils.toNonPremulAxyz32(premulArgb32);
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @return For read only purpose.
     */
    abstract int[] getPremulArgb32Arr();
}
