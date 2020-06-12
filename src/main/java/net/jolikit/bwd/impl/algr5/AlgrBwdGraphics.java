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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_COLOR;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_FONT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_LOCKED_REGION;
import net.jolikit.bwd.impl.algr5.jlib.AlgrBitmapFlag;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLibFont;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.fonts.BindingTextUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.Dbg;

import com.sun.jna.Pointer;

public class AlgrBwdGraphics extends AbstractIntArrayBwdGraphics {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    /**
     * TODO algr The NUL character (cp = 0) causes following characters
     * not to be drawn. We prefer to just remove NULs from the text
     * to draw, then draw glyph by glyph, to allow for characters
     * combining etc., even if it causes NUL not to be drawn when
     * the font has a glyph for it.
     */
    private static final boolean MUST_DRAW_TEXT_GLYPH_BY_GLYPH = false;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyTextDataAccessor {
        final int[] argb32Arr;
        final GRect maxTextRelativeRect;
        public MyTextDataAccessor(
                int[] argb32Arr,
                GRect maxTextRelativeRect) {
            this.argb32Arr = argb32Arr;
            this.maxTextRelativeRect = maxTextRelativeRect;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    private static final AlgrJnaLibFont LIB_FONT = AlgrJnaLibFont.INSTANCE;
    
    /*
     * 
     */
    
    private ALLEGRO_COLOR backingColor;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for root graphics.
     */
    public AlgrBwdGraphics(
            InterfaceBwdBinding binding,
            GRect box,
            //
            int[] pixelArr,
            int pixelArrScanlineStride) {
        this(
                binding,
                box,
                box, // initialClip
                //
                pixelArr,
                pixelArrScanlineStride);
    }

    /*
     * 
     */

    @Override
    public AlgrBwdGraphics newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".newChildGraphics(" + childBox + ")");
        }
        final GRect childInitialClip = this.getInitialClipInBase().intersected(childBox);
        return new AlgrBwdGraphics(
                this.getBinding(),
                childBox,
                childInitialClip,
                //
                this.getPixelArr(),
                this.getPixelArrScanlineStride());
    }

    /*
     * 
     */

    @Override
    public AlgrBwdFont getFont() {
        return (AlgrBwdFont) super.getFont();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void finishImpl() {
    }

    /*
     * 
     */
    
    @Override
    protected void setBackingArgb64(long argb64) {
        super.setBackingArgb64(argb64);
        this.backingColor = algrColorOf(argb64);
    }
    
    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        // Nothing to do.
    }

    @Override
    protected void setBackingState(
        boolean mustSetClip,
        GRect clipInBase,
        //
        boolean mustSetTransform,
        GTransform transform,
        //
        boolean mustSetColor,
        long argb64,
        //
        boolean mustSetFont,
        InterfaceBwdFont font) {
        
        this.setBackingStateDefaultImpl(
                mustSetClip,
                clipInBase,
                //
                mustSetTransform,
                transform,
                //
                mustSetColor,
                argb64,
                //
                mustSetFont,
                font);
    }

    /*
     * 
     */
    
    @Override
    protected int getArrayColor32FromArgb32(int argb32) {
        return BindingColorUtils.toPremulAxyz32(argb32);
    }
    
    @Override
    protected int getArgb32FromArrayColor32(int premulArgb32) {
        return BindingColorUtils.toNonPremulAxyz32(premulArgb32);
    }

    @Override
    protected int toInvertedArrayColor32(int premulArgb32) {
        return BindingColorUtils.toInvertedPremulAxyz32_noCheck(premulArgb32);
    }
    
    @Override
    protected int getArrayColorAlpha8(int premulArgb32) {
        return Argb32.getAlpha8(premulArgb32);
    }

    @Override
    protected int blendArrayColor32(int srcPremulArgb32, int dstPremulArgb32) {
        return BindingColorUtils.blendPremulAxyz32_srcOver(srcPremulArgb32, dstPremulArgb32);
    }

    /*
     * Text.
     */
    
    @Override
    protected Object getTextDataAccessor(
            String text,
            GRect maxTextRelativeRect) {
        
        final AlgrBwdFont font = (AlgrBwdFont) this.getFont();
        
        final int maxTextWidth = maxTextRelativeRect.xSpan();
        final int maxTextHeight = maxTextRelativeRect.ySpan();

        final ALLEGRO_FONT backingFont = font.getBackingFont();

        LIB.al_set_new_bitmap_flags(AlgrBitmapFlag.ALLEGRO_MEMORY_BITMAP.intValue());
        final Pointer textBitmap = LIB.al_create_bitmap(maxTextWidth, maxTextHeight);
        if (textBitmap == null) {
            throw new BindingError("could not create bitmap: " + LIB.al_get_errno());
        }
        final int[] argb32Arr;
        final int width;
        final int height;
        try {
            width = LIB.al_get_bitmap_width(textBitmap);
            height = LIB.al_get_bitmap_height(textBitmap);

            final Pointer prevBitmap = LIB.al_get_target_bitmap();
            final int textBitmapLockFormat = AlgrFormatUtils.getLockFormat(textBitmap);
            final Pointer textRegionPtr = LIB.al_lock_bitmap(
                    textBitmap,
                    textBitmapLockFormat,
                    AlgrJnaLib.ALLEGRO_LOCK_READWRITE);
            if (textRegionPtr == null) {
                throw new BindingError("could not lock bitmap: " + LIB.al_get_errno());
            }
            try {
                final ALLEGRO_LOCKED_REGION region = AlgrJnaUtils.newAndRead(
                        ALLEGRO_LOCKED_REGION.class,
                        textRegionPtr);

                // We need to zeroize the region, because al_create_bitmap(...)
                // doesn't do it.
                final int byteSize = region.pitch * height;
                region.data.clear(byteSize);

                LIB.al_set_target_bitmap(textBitmap);
                try {
                    final float x0 = 0.0f - maxTextRelativeRect.x();
                    final float y0 = 0.0f - maxTextRelativeRect.y();
                    if (MUST_DRAW_TEXT_GLYPH_BY_GLYPH) {
                        float x = x0;
                        
                        final InterfaceBwdFontMetrics fontMetrics = font.fontMetrics();

                        int ci = 0;
                        while (ci < text.length()) {
                            final int cp = text.codePointAt(ci);
                            
                            /*
                             * TODO algr If glyph width is zero,
                             * something might actually be drawn,
                             * such as for NUL (cp = 0), but we still draw it,
                             * in case user would want that.
                             */
                            LIB_FONT.al_draw_glyph(
                                    backingFont,
                                    this.backingColor,
                                    x,
                                    y0,
                                    cp);

                            final int glyphWidth = fontMetrics.computeCharWidth(cp);
                            x += (float) glyphWidth;
                            
                            ci += Character.charCount(cp);
                        }
                    } else {
                        text = BindingTextUtils.withoutNul(text);
                        
                        /*
                         * TODO algr On Windows at least, can be very slow.
                         * 
                         * The flags parameter can be 0 or one of the following flags:
                         * ALLEGRO_ALIGN_LEFT - Draw the text left-aligned (same as 0).
                         * ALLEGRO_ALIGN_CENTRE - Draw the text centered around the given position.
                         * ALLEGRO_ALIGN_RIGHT - Draw the text right-aligned to the given position.
                         * 
                         * TODO algr On my Mac, for ALLEGRO_ALIGN_LEFT, nothing is drawn,
                         * and with other flags, JVM crashes on strlen:
                         * "Stack: [0x000070000c4a9000,0x000070000c5a9000],  sp=0x000070000c5a6d70,  free space=1015k
                         * Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
                         * C  [libsystem_c.dylib+0x1b52]  strlen+0x12
                         * C  [liballegro.5.2.2.dylib+0x4af06]  al_ref_cstr+0x1c
                         * C  [liballegro_font.5.2.2.dylib+0x4178]  al_draw_text+0x2f
                         * C  [jna7682826563893169809.tmp+0xdf14]  ffi_call_unix64+0x4c"
                         * ===> Tried variations on arguments types, didn't help.
                         * ===> Maybe an issue due to Allegro not being compiled on this Mac?
                         * ===> Whatever, giving up on drawing text with Allegro on Mac.
                         */
                        final int flags = AlgrJnaLibFont.ALLEGRO_ALIGN_LEFT;
                        LIB_FONT.al_draw_text(
                                backingFont,
                                this.backingColor,
                                x0,
                                y0,
                                flags,
                                text);
                    }
                } finally {
                    LIB.al_set_target_bitmap(prevBitmap);
                }

                argb32Arr = AlgrPaintUtils.regionToArgb32Arr(
                        region,
                        width,
                        height);
            } finally {
                LIB.al_unlock_bitmap(textBitmap);
            }
        } finally {
            LIB.al_destroy_bitmap(textBitmap);
        }

        final MyTextDataAccessor accessor = new MyTextDataAccessor(
                argb32Arr,
                maxTextRelativeRect);
        return accessor;
    }

    @Override
    protected void disposeTextDataAccessor(Object textDataAccessor) {
        // Nothing to do.
    }

    @Override
    protected GRect getRenderedTextRelativeRect(Object textDataAccessor) {
        final MyTextDataAccessor accessor = (MyTextDataAccessor) textDataAccessor;
        return accessor.maxTextRelativeRect;
    }

    @Override
    protected int getTextColor32(
            String text,
            Object textDataAccessor,
            int xInText,
            int yInText) {
        final MyTextDataAccessor accessor = (MyTextDataAccessor) textDataAccessor;
        final int index = yInText * accessor.maxTextRelativeRect.xSpan() + xInText;
        final int argb32 = accessor.argb32Arr[index];
        final int premulArgb32 = this.getArrayColor32FromArgb32(argb32);
        return premulArgb32;
    }

    /*
     * Images.
     */

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final AlgrBwdImage imageImpl = (AlgrBwdImage) image;
        final int[] premulArgb32Arr = imageImpl.getPremulArgb32Arr();
        return premulArgb32Arr;
    }
    
    @Override
    protected void disposeImageDataAccessor(Object imageDataAccessor) {
        // Nothing to do.
    }

    @Override
    protected int getImageColor32(
            InterfaceBwdImage image,
            Object imageDataAccessor,
            int xInImage,
            int yInImage) {
        final int[] premulArgb32Arr = (int[]) imageDataAccessor;
        final int index = yInImage * image.getWidth() + xInImage;
        final int premulArgb32 = premulArgb32Arr[index];
        return premulArgb32;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private AlgrBwdGraphics(
            InterfaceBwdBinding binding,
            GRect box,
            GRect initialClip,
            //
            int[] pixelArr,
            int pixelArrScanlineStride) {
        super(
                binding,
                box,
                initialClip,
                //
                pixelArr,
                pixelArrScanlineStride);
    }
    
    /*
     * 
     */
    
    private static ALLEGRO_COLOR algrColorOf(long argb64) {
        final float red = (float) Argb64.getRedFp(argb64);
        final float green = (float) Argb64.getGreenFp(argb64);
        final float blue = (float) Argb64.getBlueFp(argb64);
        final float alpha = (float) Argb64.getAlphaFp(argb64);
        return algrColorOf(red, green, blue, alpha);
    }
    
    private static ALLEGRO_COLOR algrColorOf(float red, float green, float blue, float alpha) {
        final ALLEGRO_COLOR color = new ALLEGRO_COLOR();
        color.r = red;
        color.g = green;
        color.b = blue;
        color.a = alpha;
        return color;
    }
}
