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

import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLibTtf;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontMetrics;
import net.jolikit.bwd.impl.utils.fonts.BindingTextUtils;
import net.jolikit.lang.LangUtils;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class SdlBwdFontMetrics extends AbstractBwdFontMetrics {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * TODO sdl Considering SdlBwdGraphics.MUST_DRAW_TEXT_GLYPH_BY_GLYPH is true,
     * for consistency we compute text width glyph by glyph here,
     * which allows to get the width right at least when there are
     * NUL chars in-between chars with actual glyphs.
     * Also, if false, computed text width can be too large too large,
     * possibly due to computing a width for actually non-drawn code points.
     */
    private static final boolean MUST_COMPUTE_TEXT_WIDTH_GLYPH_BY_GLYPH = true;
    
    /**
     * Only used if MUST_COMPUTE_TEXT_WIDTH_GLYPH_BY_GLYPH is true.
     * 
     * TODO sdl If false, glyphs step on each other, so we let it true.
     */
    private static final boolean MUST_COMPUTE_GLYPH_WIDTH_WITH_TTF_GlyphMetrics = true;

    private static final int WIDTH_CACHE_SIZE = 256;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    private static final SdlJnaLibTtf LIB_TTF = SdlJnaLibTtf.INSTANCE;
    
    private final Pointer backingFont;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SdlBwdFontMetrics(Pointer backingFont) {
        
        if (false) {
            // If we ever want these.
            final int backingFontHeight = LIB_TTF.TTF_FontHeight(backingFont);
            // "skip" seems to imply that it corresponds to unused pixels,
            // so that it's the leading, but actually it's the line height.
            final int backingLineHeight = LIB_TTF.TTF_FontLineSkip(backingFont);
        }

        this.initialize_final(
                LIB_TTF.TTF_FontAscent(backingFont),
                LIB_TTF.TTF_FontDescent(backingFont));

        this.backingFont = LangUtils.requireNonNull(backingFont);

        /*
         * 
         */
        
        this.initCharWidthCache_final(WIDTH_CACHE_SIZE);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int computeCharWidth_noCache(int codePoint, String cpText) {
        cpText = stringOfCp(codePoint, cpText);
        return this.computeTextWidth_internal(cpText);
    }
    
    @Override
    protected int computeTextWidth_twoOrMoreCp(String text) {
        return this.computeTextWidth_internal(text);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private int computeTextWidth_internal(String text) {
        if (MUST_COMPUTE_TEXT_WIDTH_GLYPH_BY_GLYPH) {
            return this.computeTextWidth_glyphByGlyph(text);
        } else {
            return this.computeTextWidth_oneShot(text);
        }
    }
    
    private int computeTextWidth_glyphByGlyph(String text) {
        final IntByReference tmpIntRef = new IntByReference();
        
        int textWidth = 0;
        
        int ci = 0;
        while (ci < text.length()) {
            final int cp = text.codePointAt(ci);
            if (cp <= BwdUnicode.MAX_FFFF) {
                final int glyphWidth = this.backingGlyphWidth(cp, tmpIntRef);
                textWidth += glyphWidth;
            }
            ci += Character.charCount(cp);
        }

        return textWidth;
    }
    
    private int computeTextWidth_oneShot(String text) {
        /*
         * TODO sdl TTF_SizeText(...) stops computation on first NUL,
         * so we get rid of them.
         */
        text = BindingTextUtils.withoutNul(text);
        
        final IntByReference tmpIntRef = new IntByReference();
        return this.backingTextWidth(text, tmpIntRef);
    }
    
    /*
     * 
     */
    
    private int backingGlyphWidth(
            int codePoint,
            IntByReference tmpIntRef) {
        if (false) {
            // No need to do that, since this method
            // already returns 0 for NUL.
            if ((!MUST_COMPUTE_TEXT_WIDTH_GLYPH_BY_GLYPH)
                    && (codePoint == 0)) {
                // For consistency with computeTextWidth_oneShot(...).
                return 0;
            }
        }
        
        final IntByReference wRef = tmpIntRef;
        
        final int glyphWidth;
        if (MUST_COMPUTE_GLYPH_WIDTH_WITH_TTF_GlyphMetrics) {
            /*
             * TODO sdl TTF_GlyphMetrics(...) can return an advance > 0 for NUL
             * (while TTF_SizeText(...) is zero), but NUL can't be drawn
             * (whether with TTF_RenderGlyph_Blended(...) or TTF_RenderText_Blended(...)),
             * so we don't try to draw it, and for consistency we consider
             * that its width is 0. 
             */
            if (codePoint == 0) {
                glyphWidth = 0;
            } else {
                // In BMP so can cast.
                final char ch = (char) codePoint;
                
                final IntByReference minx = null;
                final IntByReference maxx = null;
                final IntByReference miny = null;
                final IntByReference maxy = null;
                final IntByReference advance = wRef;
                final int ret = LIB_TTF.TTF_GlyphMetrics(
                        this.backingFont,
                        ch,
                        minx, maxx, miny, maxy,
                        advance);
                if (ret != 0) {
                    throw new BindingError("TTF_GlyphMetrics : " + LIB.SDL_GetError());
                }
                glyphWidth = advance.getValue();
            }
        } else {
            final String cpStr = LangUtils.stringOfCodePoint(codePoint);
            glyphWidth = backingTextWidth(cpStr, tmpIntRef);
        }
        return glyphWidth;
    }
    
    private int backingTextWidth(
            String text,
            IntByReference tmpIntRef) {
        /*
         * TODO sdl TTF_SizeText(...): "-1 is returned on errors,
         * such as a glyph in the string not being found."
         * but it doesn't seem to actually occur.
         * 
         * TODO sdl TTF_SizeUTF8(...) seems not suited because by default JNA uses
         * default platform encoding, which might not be UTF8.
         * TTF_SizeUNICODE(...) looks complicated and dangerous as well because
         * it would require us to use a WString, and to hope SDL uses UTF-16 as Java.
         */
        final IntByReference wRef = tmpIntRef;
        final IntByReference hRef = null;
        final int ret = LIB_TTF.TTF_SizeText(this.backingFont, text, wRef, hRef);
        if (ret != 0) {
            throw new BindingError("TTF_SizeText : " + LIB.SDL_GetError());
        }
        return wRef.getValue();
    }
}
