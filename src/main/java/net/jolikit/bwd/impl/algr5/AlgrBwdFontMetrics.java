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

import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_FONT;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLibFont;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontMetrics;
import net.jolikit.bwd.impl.utils.fonts.BindingTextUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

public class AlgrBwdFontMetrics extends AbstractBwdFontMetrics {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * False, for consistency with
     * AlgrBwdGraphics.MUST_DRAW_TEXT_GLYPH_BY_GLYPH,
     * and because if true we get weird values.
     */
    private static final boolean MUST_COMPUTE_TEXT_WIDTH_GLYPH_BY_GLYPH = false;
    
    /**
     * TODO algr Allegro width computation gets terribly slow as font size
     * increases, even if it's still only in the hundreds,
     * so we only bother to use a cache if the font height is not too large.
     */
    private static final int MAX_FONT_HEIGHT_FOR_CACHE = 128;
    
    private static final int WIDTH_CACHE_SIZE = 256;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final AlgrJnaLibFont LIB_FONT = AlgrJnaLibFont.INSTANCE;
    
    private final ALLEGRO_FONT backingFont;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AlgrBwdFontMetrics(ALLEGRO_FONT backingFont) {
        
        if (false) {
            // If we ever want these.
            final int backingLineHeight = LIB_FONT.al_get_font_line_height(backingFont);
        }
        
        this.initialize_final(
                LIB_FONT.al_get_font_ascent(backingFont),
                LIB_FONT.al_get_font_descent(backingFont));
        
        this.backingFont = LangUtils.requireNonNull(backingFont);

        /*
         * 
         */

        if (this.height() <= MAX_FONT_HEIGHT_FOR_CACHE) {
            this.initCharWidthCache_final(WIDTH_CACHE_SIZE);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int computeCharWidth_noCache(int codePoint, String cpText) {
        return this.backingGlyphWidth(codePoint);
    }
    
    @Override
    protected int computeTextWidth_twoOrMoreCp(String text) {
        if (MUST_COMPUTE_TEXT_WIDTH_GLYPH_BY_GLYPH) {
            return this.computeTextWidth_glyphByGlyph(text);
        } else {
            return this.computeTextWidth_oneShot(text);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private int computeTextWidth_glyphByGlyph(String text) {
        int textWidth = 0;
        int ci = 0;
        int prevCp = -1;
        while (ci < text.length()) {
            final int cp = text.codePointAt(ci);
            if (prevCp == -1) {
                // Will compute on next.
            } else {
                final int advance = backingGlyphAdvance(prevCp, cp);
                textWidth = NbrsUtils.plusExact(textWidth, advance);
            }
            prevCp = cp;

            ci += Character.charCount(cp);
        }
        if (prevCp >= 0) {
            // Last glyph width.
            final int glyphWidth = backingGlyphWidth(prevCp);
            textWidth = NbrsUtils.plusExact(textWidth, glyphWidth);
        }
        return textWidth;
    }

    private int computeTextWidth_oneShot(String text) {
        /*
         * TODO algr Need to do that, for consistency with
         * drawText(...), because backing text width computation
         * stops on NUL.
         */
        text = BindingTextUtils.withoutNul(text);
        
        return backingTextWidth(text);
    }

    /*
     * TODO algr al_get_glyph_width(...) and al_get_text_width(...),
     * for strings containing a single char, compute the actual
     * glyph width, not advance, for example they return a small value
     * for 0x21 (exclamation mark).
     * They also weirdly compute negative values for some code points,
     * like 0x20 (space) or 0xA0 (no-break space), and give a positive
     * result for 0x0 (NUL) even though nothing is drawn, whereas
     * al_get_text_width(...) with a string containing two NUL chars
     * returns 0.
     * As a result, we only use al_get_text_width(...),
     * with text of at least two code points,
     * and compute the width for a simple code point as
     * al_get_text_width(cp+cp) / 2.
     */
    
    /**
     * @param codePoint Must be valid.
     */
    private int backingGlyphWidth(int codePoint) {
        if (false) {
            // No need to do that, since this method
            // already returns 0 for NUL.
            if ((!MUST_COMPUTE_TEXT_WIDTH_GLYPH_BY_GLYPH)
                    && (codePoint == 0)) {
                // For consistency with computeTextWidth_oneShot(...).
                return 0;
            }
        }
        // Proper capacity for two code points in BMP.
        final StringBuilder sb = new StringBuilder(2);
        for (int i = 0; i < 2; i++) {
            sb.appendCodePoint(codePoint);
        }
        final String text = sb.toString(); 
        final int glyphWidth = LIB_FONT.al_get_text_width(this.backingFont, text) / 2;
        return Math.max(0, glyphWidth);
    }

    private int backingGlyphAdvance(int codePoint1, int codePoint2) {
        final int advance = LIB_FONT.al_get_glyph_advance(this.backingFont, codePoint1, codePoint2);
        return Math.max(0, advance);
    }

    private int backingTextWidth(String text) {
        if (text.length() == 0) {
            return 0;
        }
        final int cp1 = text.codePointAt(0);
        final int cp1Size = Character.charCount(cp1);
        final boolean onlyOneCp = (cp1Size == text.length());
        if (onlyOneCp) {
            return this.backingGlyphWidth(cp1);
        } else {
            final int textWidth = LIB_FONT.al_get_text_width(this.backingFont, text);
            // Max(0,_) in case Allegro ever goes crazy
            // (as it does with al_get_glyph_width(...)).
            return Math.max(0, textWidth);
        }
    }
}
