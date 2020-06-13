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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public abstract class AbstractAlgrBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param disposalListener Must not be null.
     * @throws NullPointerException if disposalListener is null.
     */
    public AbstractAlgrBwdImage(
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int[] premulArgb32Arr = this.getPremulArgb32Arr();
        final int index = y * this.getWidth() + x;
        final int premulArgb32 = premulArgb32Arr[index];
        final int argb32 = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
        return argb32;
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * TODO algr For performances when drawing image on graphics,
     * we store its content as an array of premultiplied colors.
     * Note that as a result, in getArgb32AtImpl(...), we compute
     * the non-premultiplied color from a premultiplied one,
     * which can damage RGB components in case of semi transparent images.
     * 
     * @return For read only purpose.
     */
    abstract int[] getPremulArgb32Arr();
}
