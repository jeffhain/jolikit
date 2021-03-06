/*
 * Copyright 2020-2021 Jeff Hain
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

import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.ObjectWrapper;

public class QtjBwdWritableImage extends AbstractQtjBwdImage implements InterfaceBwdWritableImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final QtjImageHelper backingImageHelper;
    
    private final InterfaceBwdGraphics graphics;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param width Image width. Must be > 0.
     * @param height Image height. Must be > 0.
     * @param qtStuffsPoolRef (in,out) Reference to a pool of Qt objects
     *        managed by image graphics class.
     * @throws NullPointerException if binding or qtStuffsPoolRef
     *         or disposalListener is null.
     * @throws IllegalArgumentException if width or height is <= 0.
     */
    public QtjBwdWritableImage(
        InterfaceBwdBindingImpl binding,
        int width,
        int height,
        ObjectWrapper<Object> qtStuffsPoolRef,
        InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        this.checkAndSetWritableImageDims(width, height);
        
        final boolean isImageGraphics = true;
        final GRect box = this.getRect();
        
        final QImage backingImage = new QImage(
            width,
            height,
            QtjPaintUtils.FORMAT_ARGB_PRE);
        
        final QtjImageHelper backingImageHelper =
            new QtjImageHelper(backingImage);
        this.backingImageHelper = backingImageHelper;
        
        final QtjBwdGraphics graphics = new QtjBwdGraphics(
            binding,
            box,
            //
            isImageGraphics,
            backingImageHelper,
            //
            qtStuffsPoolRef);
        this.graphics = graphics;
        
        graphics.init();
        
        /*
         * NB: Default image content is undefined
         * (for example, can easily be what was on
         * a previously disposed image of same dimensions),
         * so we take care to clear it here.
         */
        final int initialArgb32 = graphics.getArgb32();
        graphics.setColor(BwdColor.TRANSPARENT);
        graphics.clearRect(graphics.getBox());
        graphics.setArgb32(initialArgb32);
    }
    
    @Override
    public QtjImageHelper getBackingImageHelper() {
        return this.backingImageHelper;
    }

    @Override
    public InterfaceBwdGraphics getGraphics() {
        return this.graphics;
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
        this.graphics.finish();
        
        this.backingImageHelper.getImage().dispose();
    }
}
