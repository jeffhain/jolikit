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
package net.jolikit.bwd.impl.sdl2;

import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLibImage;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Surface;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaUtils;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.LangUtils;

import com.sun.jna.Pointer;

public class SdlBwdImage extends AbstractBwdImage {
    
    /*
     * TODO sdl For 1-bit, 8-bits and 16-bits BMP images,
     * format is messed-up (color masks are 0, BPP is always 1, etc.),
     * but bytes content look fine.
     * As a result since color masks are 0, all these BMP appear black.
     */
    
    /*
     * Storing image as an int array of pixels, to avoid, at each drawing,
     * the JNA overhead of reading pixels one by one or even in bulk,
     * and the overhead of pixel format conversion.
     */
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    private static final SdlJnaLibImage LIB_IMG = SdlJnaLibImage.INSTANCE;
    
    /**
     * Image pixels, as a sequence of rows.
     */
    private final int[] premulArgb32Arr;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param filePath Path of an image file.
     * @throws NullPointerException if the specified path is null.
     * @throws IllegalArgumentException if could not load the image.
     */
    public SdlBwdImage(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        final Pointer surfPtr = getBackingImagePointer(filePath);
        final SDL_Surface surf = SdlJnaUtils.newAndRead(SDL_Surface.ByValue.class, surfPtr);
        
        final int[] premulArgb32Arr;
        final int width;
        final int height;
        try {
            width = surf.w;
            height = surf.h;
            final boolean premul = true;
            premulArgb32Arr = SdlUtils.surfToArgb32Arr(surf, premul);
        } finally {
            LIB.SDL_FreeSurface(surf);
        }
        
        this.premulArgb32Arr = premulArgb32Arr;
        this.setWidth_final(width);
        this.setHeight_final(height);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int index = y * this.getWidth() + x;
        final int premulArgb32 = this.premulArgb32Arr[index];
        return BindingColorUtils.toNonPremulAxyz32(premulArgb32);
    }

    @Override
    protected void disposeImpl() {
        // Nothing to do.
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    int[] getPremulArgb32Arr() {
        return this.premulArgb32Arr;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @throws IllegalArgumentException if could not load the image.
     */
    private static Pointer getBackingImagePointer(String filePath) {
        LangUtils.requireNonNull(filePath);
        final Pointer backingImagePtr = LIB_IMG.IMG_Load(filePath);
        if (backingImagePtr == null) {
            throw new IllegalArgumentException("could not load image at " + filePath + ": " + LIB.SDL_GetError());
        }
        return backingImagePtr;
    }
}
