/*
 * Copyright 2019-2024 Jeff Hain
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

import com.sun.jna.Pointer;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
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
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.fonts.BindingTextUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.graphics.InterfaceColorTypeHelper;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.lang.Dbg;

public class AlgrBwdGraphics extends AbstractIntArrayBwdGraphics {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    /**
     * TODO algr The NUL character (cp = 0) causes following characters
     * not to be drawn. We prefer to just remove NULs from the text
     * to draw, then use al_draw_text(), to allow for characters
     * combining etc., even if it causes NUL not to be drawn when
     * the font has a glyph for it.
     */
    private static final boolean MUST_DRAW_TEXT_GLYPH_BY_GLYPH = false;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Clipped Text Data Accessor.
     */
    private static class MyCtda {
        final int[] argb32Arr;
        final GRect rectInText;
        public MyCtda(
                int[] argb32Arr,
                GRect rectInText) {
            this.argb32Arr = argb32Arr;
            this.rectInText = rectInText;
        }
        public int getScanlineStride() {
            return this.rectInText.xSpan();
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
    
    /**
     * Costly to compute, and only used for text drawing,
     * so nullifying it on color setting,
     * and computing it before text drawing if needed (lazily).
     */
    private ALLEGRO_COLOR backingColor;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for root graphics.
     */
    public AlgrBwdGraphics(
        InterfaceBwdBindingImpl binding,
        GRect box,
        //
        boolean isImageGraphics,
        int[] pixelArr,
        int pixelArrScanlineStride) {
        this(
                binding,
                topLeftOf(box),
                box,
                box, // initialClip
                //
                isImageGraphics,
                pixelArr,
                pixelArrScanlineStride);
    }

    /*
     * 
     */

    @Override
    public AlgrBwdGraphics newChildGraphics(
            GRect childBox,
            GRect childMaxInitialClip) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(
                    this.getClass().getSimpleName() + "-" + this.hashCode()
                    + ".newChildGraphics(" + childBox
                    + "," + childMaxInitialClip + ")");
        }
        
        final GRect childInitialClip =
                this.getInitialClipInBase().intersected(
                        childMaxInitialClip.intersected(childBox));
        
        return new AlgrBwdGraphics(
                this.getBinding(),
                this.getRootBoxTopLeft(),
                childBox,
                childInitialClip,
                //
                this.isImageGraphics(),
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
        // Nothing to do.
    }

    /*
     * 
     */
    
    @Override
    protected void setBackingArgb(int argb32, BwdColor colorElseNull) {
        super.setBackingArgb(argb32, colorElseNull);
        this.backingColor = null;
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
        int argb32,
        BwdColor colorElseNull,
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
                argb32,
                colorElseNull,
                //
                mustSetFont,
                font);
    }

    /*
     * 
     */
    
    @Override
    protected InterfaceColorTypeHelper getArrayColorHelper() {
        return PremulArgbHelper.getInstance();
    }

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
    protected Object getClippedTextDataAccessor(
            String text,
            GRect maxClippedTextRectInText) {
        
        final AlgrBwdFont font = (AlgrBwdFont) this.getFont();
        
        final int mcTextWidth = maxClippedTextRectInText.xSpan();
        final int mcTextHeight = maxClippedTextRectInText.ySpan();

        final ALLEGRO_FONT backingFont = font.getBackingFont();

        LIB.al_set_new_bitmap_flags(AlgrBitmapFlag.ALLEGRO_MEMORY_BITMAP.intValue());
        final Pointer mcTextBitmap = LIB.al_create_bitmap(
                mcTextWidth,
                mcTextHeight);
        if (mcTextBitmap == null) {
            throw new BindingError("could not create bitmap: " + LIB.al_get_errno());
        }
        final int[] mcArgb32Arr;
        try {
            final Pointer prevBitmap = LIB.al_get_target_bitmap();
            final int textBitmapLockFormat = AlgrFormatUtils.getLockFormat(mcTextBitmap);
            final Pointer mcTextRegionPtr = LIB.al_lock_bitmap(
                    mcTextBitmap,
                    textBitmapLockFormat,
                    AlgrJnaLib.ALLEGRO_LOCK_READWRITE);
            if (mcTextRegionPtr == null) {
                throw new BindingError("could not lock bitmap: " + LIB.al_get_errno());
            }
            
            if (this.backingColor == null) {
                this.backingColor = AlgrUtils.newColor(this.getArgb64());
            }
            
            try {
                final ALLEGRO_LOCKED_REGION mcRegion = AlgrJnaUtils.newAndRead(
                        ALLEGRO_LOCKED_REGION.class,
                        mcTextRegionPtr);

                // We need to zeroize the region,
                // because al_create_bitmap(...) doesn't do it.
                final int byteSize = mcRegion.pitch * mcTextHeight;
                mcRegion.data.clear(byteSize);

                LIB.al_set_target_bitmap(mcTextBitmap);
                try {
                    final float x0 = 0.0f - maxClippedTextRectInText.x();
                    final float y0 = 0.0f - maxClippedTextRectInText.y();
                    if (MUST_DRAW_TEXT_GLYPH_BY_GLYPH) {
                        float x = x0;
                        
                        final InterfaceBwdFontMetrics fontMetrics = font.metrics();
                        
                        int ci = 0;
                        while (ci < text.length()) {
                            final int cp = text.codePointAt(ci);
                            
                            /*
                             * TODO algr On Windows at least, can be very slow.
                             * 
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
                        // Removing nuls else stops early.
                        text = BindingTextUtils.withoutNul(text);
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

                mcArgb32Arr = AlgrPaintUtils.regionToArgb32Arr(
                        mcRegion,
                        mcTextWidth,
                        mcTextHeight);
            } finally {
                LIB.al_unlock_bitmap(mcTextBitmap);
            }
        } finally {
            LIB.al_destroy_bitmap(mcTextBitmap);
        }

        final MyCtda accessor =
                new MyCtda(
                        mcArgb32Arr,
                        maxClippedTextRectInText);
        return accessor;
    }

    @Override
    protected void disposeClippedTextDataAccessor(
            Object clippedTextDataAccessor) {
        // Nothing to do.
    }

    @Override
    protected GRect getRenderedClippedTextRectInText(
            Object clippedTextDataAccessor) {
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        return accessor.rectInText;
    }

    @Override
    protected int getTextColor32(
            String text,
            Object clippedTextDataAccessor,
            int xInClippedText,
            int yInClippedText) {
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        final int index = yInClippedText * accessor.getScanlineStride() + xInClippedText;
        final int argb32 = accessor.argb32Arr[index];
        final int premulArgb32 = this.getArrayColor32FromArgb32(argb32);
        return premulArgb32;
    }

    /*
     * Images.
     */

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final AbstractAlgrBwdImage imageImpl = (AbstractAlgrBwdImage) image;
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
        return premulArgb32Arr[index];
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private AlgrBwdGraphics(
            InterfaceBwdBindingImpl binding,
            GPoint rootBoxTopLeft,
            GRect box,
            GRect initialClip,
            //
            boolean isImageGraphics,
            int[] pixelArr,
            int pixelArrScanlineStride) {
        super(
                binding,
                rootBoxTopLeft,
                box,
                initialClip,
                //
                isImageGraphics,
                pixelArr,
                pixelArrScanlineStride);
    }
}
