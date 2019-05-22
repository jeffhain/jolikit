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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_LOCKED_REGION;
import net.jolikit.bwd.impl.algr5.jlib.AlgrBitmapLoadFlag;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaUtils;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

import com.sun.jna.Pointer;

public class AlgrBwdImage extends AbstractBwdImage {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    
    /**
     * TODO algr For performances when drawing image on graphics,
     * we store its content as an array of premultiplied colors.
     * Note that as a result, in getArgb32AtImpl(...), we compute
     * the non-premultiplied color from a premultiplied one,
     * which can damage RGB components in case of semi transparent images.
     */
    private final int[] premulArgb32Arr;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param filePath Path of an image file.
     * @throws NullPointerException if the specified path is null.
     * @throws IllegalArgumentException if could not load the specified image.
     */
    public AlgrBwdImage(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        final Pointer imgBitmap = getBackingImagePointer(filePath);
        
        final int[] premulArgb32Arr;
        final int width;
        final int height;
        try {
            width = LIB.al_get_bitmap_width(imgBitmap);
            height = LIB.al_get_bitmap_height(imgBitmap);
            
            final int imgBitmapLockFormat = AlgrFormatUtils.getLockFormat(imgBitmap);
            final Pointer regionPtr = LIB.al_lock_bitmap(
                    imgBitmap,
                    imgBitmapLockFormat,
                    AlgrJnaLib.ALLEGRO_LOCK_READONLY);
            if (regionPtr == null) {
                throw new BindingError("could not lock bitmap: " + LIB.al_get_errno());
            }
            try {
                final ALLEGRO_LOCKED_REGION region = AlgrJnaUtils.newAndRead(ALLEGRO_LOCKED_REGION.class, regionPtr);
                if (DEBUG) {
                    Dbg.log("format = " + region.format);
                    Dbg.log("pitch = " + region.pitch);
                    Dbg.log("pixel_size = " + region.pixel_size);
                }
                
                premulArgb32Arr = AlgrPaintUtils.regionToArgb32Arr(
                        region,
                        width,
                        height);
                
                for (int i = 0; i < premulArgb32Arr.length; i++) {
                    final int argb32 = premulArgb32Arr[i];
                    final int premulArgb32 = BindingColorUtils.toPremulAxyz32(argb32);
                    premulArgb32Arr[i] = premulArgb32;
                }
            } finally {
                LIB.al_unlock_bitmap(imgBitmap);
            } 
        } finally {
            LIB.al_destroy_bitmap(imgBitmap);
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
        final int argb32 = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
        return argb32;
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
    
    private static Pointer getBackingImagePointer(String filePath) {
        LangUtils.requireNonNull(filePath);
        
        int flags = 0;
        flags |= AlgrBitmapLoadFlag.ALLEGRO_NO_PREMULTIPLIED_ALPHA.intValue();
        if (false) {
            /*
             * TODO algr Could use that for special-case for bmp and pcx files,
             * for perfs? (then, would have to keep track of which color model
             * is in use)
             */
            flags |= AlgrBitmapLoadFlag.ALLEGRO_KEEP_INDEX.intValue();
        }
        if (false) {
            /*
             * Not using that, to keep used format homogeneous.
             */
            flags |= AlgrBitmapLoadFlag.ALLEGRO_KEEP_BITMAP_FORMAT.intValue();
        }
        
        final Pointer bitmapPtr = LIB.al_load_bitmap_flags(filePath, flags);
        if (bitmapPtr == null) {
            throw new IllegalArgumentException("could not load image at " + filePath + ": " + LIB.al_get_errno());
        }
        return bitmapPtr;
    }
}
