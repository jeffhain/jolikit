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

import java.util.ArrayList;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLibTtf;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Color;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Surface;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.fonts.BindingTextUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;

import com.sun.jna.Pointer;

public class SdlBwdGraphics extends AbstractIntArrayBwdGraphics {
    
    /*
     * Not using per-pixel SDL treatments, such as SDL_DRAW_PUTPIXEL_BPP_4,
     * for it's extremely slow (from Java a least).
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * TODO sdl For some reasons, SDL seem to draw the text with random
     * {r,g,b} components (but not alpha), and as if it would use some color's
     * instance-specific hash code instead (if the color instance is always
     * the same, the random color is always the same).
     * To work that around the best we can, we just replace whatever pixel
     * with non-zero alpha with the user-specified {r,g,b}, using a combination
     * of user-specified alpha and anti-aliased alpha.
     */
    private static final boolean MUST_ENFORCE_COLOR = true;
    
    /**
     * TODO sdl Using TTF_RenderGlyph_XXX(...) rather than
     * TTF_RenderText_XXX(...), else we can't seem to render code points
     * above 255 (Java/C string issue?), and the NUL character (cp = 0)
     * causes following characters not to be drawn.
     * Seems only about twice slower than using TTF_RenderText_XXX(...).
     * 
     * NB: If using TTF_RenderGlyph_XXX(...), characters (*) for which there is
     * no glyph seem to always be drawn as a little rectangle, while with
     * TTF_RenderText_XXX(...), for some of these characters, '?' is used.
     * (*) And other than NUL, for which nothing is drawn,
     * even if the font has a glyph for it.
     * 
     * NB: Could special case to TTF_RenderText_XXX(...) when we only have
     * drawable code points in [0,255], but we prefer to keep things simple.
     */
    private static final boolean MUST_DRAW_TEXT_GLYPH_BY_GLYPH = true;
    
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
    
    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    private static final SdlJnaLibTtf LIB_TTF = SdlJnaLibTtf.INSTANCE;

    /**
     * Only used if MUST_MONOCHROMATIZE is true.
     */
    private static final SDL_Color SDL_TEXT_RENDERING_COLOR = new SDL_Color();
    static {
        SDL_TEXT_RENDERING_COLOR.r = (byte) 0xFF;
        SDL_TEXT_RENDERING_COLOR.g = (byte) 0xFF;
        SDL_TEXT_RENDERING_COLOR.b = (byte) 0xFF;
        SDL_TEXT_RENDERING_COLOR.a = (byte) 0xFF;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for root graphics.
     */
    public SdlBwdGraphics(
            InterfaceBwdBinding binding,
            GRect box,
            //
            int[] clientPixelArr,
            int clientPixelArrScanlineStride) {
        this(
                binding,
                box,
                box, // baseClip
                //
                clientPixelArr,
                clientPixelArrScanlineStride);
    }

    /*
     * 
     */

    @Override
    public SdlBwdGraphics newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".newChildGraphics(" + childBox + ")");
        }
        final GRect childBaseClip = this.getBaseClipInClient().intersected(childBox);
        return new SdlBwdGraphics(
                this.getBinding(),
                childBox,
                childBaseClip,
                //
                this.getClientPixelArr(),
                this.getClientPixelArrScanlineStride());
    }

    /*
     * 
     */

    @Override
    public SdlBwdFont getFont() {
        return (SdlBwdFont) super.getFont();
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void finishImpl() {
    }

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        // Nothing to do.
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
        
        final int argb32 = this.getArgb32();
        
        SDL_Color backingFgColor;
        if (MUST_ENFORCE_COLOR) {
            /*
             * Since we don't use SDL text's (r,g,b),
             * nor its alpha directly, we don't bother
             * creating a color each time.
             */
            backingFgColor = SDL_TEXT_RENDERING_COLOR;
        } else {
            backingFgColor = new SDL_Color();
            backingFgColor.r = (byte) Argb32.getRed8(argb32);
            backingFgColor.g = (byte) Argb32.getGreen8(argb32);
            backingFgColor.b = (byte) Argb32.getBlue8(argb32);
            backingFgColor.a = (byte) Argb32.getAlpha8(argb32);
        }
        
        final SdlBwdFont font = (SdlBwdFont) this.getFont();
        
        /*
         * TODO sdl TTF_RenderText/Glyph_Shaded(...) paints
         * non-transparent backgrounds,
         * and TTF_RenderText/Glyph_Solid(...) draws terribly
         * (characters not even recognizable in non-large sizes),
         * so we stick to TTF_RenderText/Glyph_Blended(...).
         */
        
        final MyTextDataAccessor accessor;
        if (MUST_DRAW_TEXT_GLYPH_BY_GLYPH) {
            accessor = newTextDataAccessor_RenderGlyph_Blended(
                    text,
                    maxTextRelativeRect,
                    font,
                    backingFgColor,
                    argb32);
        } else {
            accessor = newTextDataAccessor_RenderText_Blended(
                    text,
                    maxTextRelativeRect,
                    font,
                    backingFgColor,
                    argb32);
        }
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
        final SdlBwdImage imageImpl = (SdlBwdImage) image;
        return imageImpl.getPremulArgb32Arr();
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

    private SdlBwdGraphics(
            InterfaceBwdBinding binding,
            GRect box,
            GRect baseClip,
            //
            int[] clientPixelArr,
            int clientPixelArrScanlineStride) {
        super(
                binding,
                box,
                baseClip,
                //
                clientPixelArr,
                clientPixelArrScanlineStride);
    }
    
    /*
     * 
     */
    
    /**
     * Uses TTF_RenderGlyph_Blended(...).
     */
    private static MyTextDataAccessor newTextDataAccessor_RenderGlyph_Blended(
            String text,
            GRect maxTextRelativeRect,
            //
            SdlBwdFont font,
            SDL_Color backingFgColor,
            int argb32) {

        final Pointer backingFont = font.getBackingFont();

        final InterfaceBwdFontMetrics fontMetrics = font.fontMetrics();

        // Reusing accessor class.
        final ArrayList<MyTextDataAccessor> glyphDataList =
                new ArrayList<MyTextDataAccessor>();

        int ci = 0;
        while (ci < text.length()) {
            final int cp = text.codePointAt(ci);
            if (cp <= BwdUnicode.MAX_FFFF) {
                final int theoreticalWidth = fontMetrics.computeCharWidth(cp);
                if (theoreticalWidth == 0) {
                    /*
                     * TODO sdl TTF_RenderGlyph_Blended(...)
                     * returns null if glyph width is zero,
                     * with the error "Text has zero width".
                     */
                } else {
                    final char ch = (char) cp;
                    final Pointer surfPtr = LIB_TTF.TTF_RenderGlyph_Blended(backingFont, ch, backingFgColor);
                    if (surfPtr == null) {
                        throw new BindingError(
                                "could not render code point: " + LIB.SDL_GetError()
                                + ", code point = " + BwdUnicode.toDisplayString(cp));
                    }
                    final SDL_Surface surface = SdlJnaUtils.newAndRead(SDL_Surface.ByValue.class, surfPtr);

                    final int[] glyphArgb32Arr;
                    final int glyphWidth;
                    final int glyphHeight;
                    try {
                        glyphWidth = surface.w;
                        glyphHeight = surface.h;
                        final boolean premul = false;
                        glyphArgb32Arr = SdlUtils.surfToArgb32Arr(surface, premul);
                    } finally {
                        LIB.SDL_FreeSurface(surface);
                    }

                    final GRect glyphRelativeRect = GRect.valueOf(0, 0, glyphWidth, glyphHeight);
                    glyphDataList.add(new MyTextDataAccessor(glyphArgb32Arr, glyphRelativeRect));
                }
            } else {
                /*
                 * Can't display it: ignoring
                 * (We assume that matches SDL text width computation,
                 * for which we use TTF_SizeText(...)).
                 */
            }
            ci += Character.charCount(cp);
        }

        final int glyphCount = glyphDataList.size();

        int width = 0;
        int height = 0;
        for (int i = 0; i < glyphCount; i++) {
            final MyTextDataAccessor glyphData = glyphDataList.get(i);
            width += glyphData.maxTextRelativeRect.xSpan();
            height = Math.max(height, glyphData.maxTextRelativeRect.ySpan());
        }

        final int pixelCount = NumbersUtils.timesExact(width, height);
        final int[] argb32Arr = new int[pixelCount];
        int glyphX = 0;
        for (int i = 0; i < glyphCount; i++) {
            final MyTextDataAccessor glyphData = glyphDataList.get(i);
            final int[] glyphArgb32Arr = glyphData.argb32Arr;
            final int glyphWidth = glyphData.maxTextRelativeRect.xSpan();
            final int glyphHeight = glyphData.maxTextRelativeRect.ySpan();
            for (int y = 0; y < glyphHeight; y++) {
                final int srcPos = y * glyphWidth;
                final int dstPos = (y * width) + glyphX;
                final int length = glyphWidth;
                System.arraycopy(glyphArgb32Arr, srcPos, argb32Arr, dstPos, length);
            }
            glyphX += glyphWidth;
        }

        final GRect actualTextRelativeRect = GRect.valueOf(0, 0, width, height);

        return newTextDataAccessor(
                argb32Arr,
                actualTextRelativeRect,
                argb32,
                maxTextRelativeRect);
    }

    /**
     * Uses TTF_RenderText_Blended(...).
     */
    private static MyTextDataAccessor newTextDataAccessor_RenderText_Blended(
            String text,
            GRect maxTextRelativeRect,
            //
            SdlBwdFont font,
            SDL_Color backingFgColor,
            int argb32) {
        
        final Pointer backingFont = font.getBackingFont();
        
        final InterfaceBwdFontMetrics fontMetrics = font.fontMetrics();

        final int[] argb32Arr;
        final GRect actualTextRelativeRect;
        /*
         * TODO sdl TTF_RenderText_Blended(...)
         * returns null if text width is zero,
         * with the error "Text has zero width".
         */
        final int theoreticalWidth = fontMetrics.computeTextWidth(text);
        if (theoreticalWidth == 0) {
            argb32Arr = new int[0];
            actualTextRelativeRect = GRect.valueOf(0, 0, 0, 0);
        } else {
            /*
             * TODO sdl TTF_RenderText_Blended(...) stops drawing on first NUL,
             * so we get rid of them.
             */
            text = BindingTextUtils.withoutNul(text);
            
            final Pointer surfPtr = LIB_TTF.TTF_RenderText_Blended(backingFont, text, backingFgColor);
            if (surfPtr == null) {
                throw new BindingError(
                        "could not render text: " + LIB.SDL_GetError()
                        + ", first code point = " + BwdUnicode.toDisplayString(text.codePointAt(0))
                        + ", text = " + text);
            }
            final SDL_Surface surface = SdlJnaUtils.newAndRead(SDL_Surface.ByValue.class, surfPtr);

            final int width;
            final int height;
            try {
                width = surface.w;
                height = surface.h;
                final boolean premul = false;
                argb32Arr = SdlUtils.surfToArgb32Arr(surface, premul);
            } finally {
                LIB.SDL_FreeSurface(surface);
            }

            actualTextRelativeRect = GRect.valueOf(0, 0, width, height);
        }

        return newTextDataAccessor(
                argb32Arr,
                actualTextRelativeRect,
                argb32,
                maxTextRelativeRect);
    }
    
    /**
     * Does eventual color enforcing.
     */
    private static MyTextDataAccessor newTextDataAccessor(
            int[] argb32Arr,
            GRect actualTextRelativeRect,
            //
            int argb32,
            GRect maxTextRelativeRect) {
        if (MUST_ENFORCE_COLOR) {
            enforceColor(argb32Arr, argb32);
        }
        
        // Eventually shrinking.
        maxTextRelativeRect = maxTextRelativeRect.intersected(actualTextRelativeRect);
        
        final MyTextDataAccessor accessor = new MyTextDataAccessor(
                argb32Arr,
                maxTextRelativeRect);
        
        return accessor;
    }

    /**
     * Replaces non-fully-transparent pixels with the specified ARGB 32.
     * 
     * @param argb32Arr (in,out)
     */
    private static void enforceColor(
            int[] argb32Arr,
            int argb32) {
        final int alpha8 = Argb32.getAlpha8(argb32);
        final double alphaFp = Argb32.toFpFromInt8(alpha8);
        for (int i = 0; i < argb32Arr.length; i++) {
            final int funnyArgb32 = argb32Arr[i];
            final int antiAliasedAlpha8 = Argb32.getAlpha8(funnyArgb32);
            if (antiAliasedAlpha8 != 0x00) {
                final double antiAliasedAlphaFp = Argb32.toFpFromInt8(antiAliasedAlpha8);
                final int reworkedArgb32 = Argb32.withAlphaFp(argb32, Math.sqrt(alphaFp * antiAliasedAlphaFp));
                argb32Arr[i] = reworkedArgb32;
            }
        }
    }
}
