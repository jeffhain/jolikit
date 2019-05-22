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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.image.Image;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.LangUtils;

public class JfxBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Image backingImage;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JfxBwdImage(
            Image backingImage,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        this.backingImage = LangUtils.requireNonNull(backingImage);
        this.setWidth_final(BindingCoordsUtils.ceilToInt(backingImage.getWidth()));
        this.setHeight_final(BindingCoordsUtils.ceilToInt(backingImage.getHeight()));
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
}
