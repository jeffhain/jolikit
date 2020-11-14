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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.Arrays;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test support of various Unicode ranges, either in the first plane
 * (BMP), or outside of it, in astral planes.
 * In particular, testing corresponding width computation,
 * with NUL and ST control character somewhere within the text,
 * to check that the eventual removal of these special code points,
 * if done, is properly done in spite of the use of two chars
 * for some code points (i.e. no char index vs code point index bug).
 */
public class UnicodeBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int FONT_SIZE = 16;
    
    /**
     * Our best font for displaying various Unicode characters.
     */
    private static final String UNIFONT_FILE_PATH = BwdTestResources.TEST_FONT_UNIFONT_8_0_01_TTF;

    private static final int[] CP_ARR = new int[]{
        
        /**
         * Our begin mark.
         */
        BwdUnicode.ZERO,
        
        /*
         * In BMP, one char in Java String.
         */
        
        /**
         * Latin word.
         */
        BwdUnicode.t, BwdUnicode.e, BwdUnicode.x, BwdUnicode.t,
        /**
         * Greek word.
         */
        0x03BA, 0x03B5, 0x03AF, 0x03BC, 0x03B5, 0x03BD, 0x03BF,
        /**
         * Cyrillic word.
         */
        0x0442, 0x0435, 0x043A, 0x0441, 0x0442,
        /**
         * Hebrew word.
         */
        0x05D8, 0x05B6, 0x05E7, 0x05E1, 0x05D8,
        /**
         * Arabic word.
         */
        0x0646, 0x0635,
        /**
         * Biohazard.
         */
        0x2623,
        /**
         * Ying yang.
         */
        0x262F,
        /**
         * Atom symbol.
         */
        0x269B,
        /**
         * Chinese word.
         */
        0x6587, 0x672C,
        
        /*
         * Outside BMP, two chars in Java String.
         */
        
        /*
         * Egyptian hieroglyphs.
         */
        /**
         * Man-arms-uplifted.
         */
        0x13020,
        /**
         * Same, upside-down.
         */
        0x13021,
        /**
         * A slow one.
         */
        0x130BA,
        /**
         * Manneken pis.
         */
        0x130C2,
        /**
         * Scarabug.
         */
        0x131A3,
        /*
         * Modern hieroglyphs.
         */
        /**
         * Volcano.
         */
        0x1F30B,
        /**
         * Globe showing Europe-Africa.
         */
        0x1F30D,
        /**
         * Globe showing Americas.
         */
        0x1F30E,
        /**
         * Globe showing Asia-Australia.
         */
        0x1F30F,
        /**
         * Tulip.
         */
        0x1F337,
        /**
         * Bread.
         */
        0x1F35E,
        /**
         * Musical notes.
         */
        0x1F3B6,
        /**
         * Turtle.
         */
        0x1F422,
        /**
         * Alien.
         */
        0x1F47D,
        /**
         * Books.
         */
        0x1F4DA,
        /**
         * Statue of liberty.
         */
        0x1F5FD,
        /**
         * Moai.
         */
        0x1F5FF,
        /**
         * Face with tears of joy.
         */
        0x1F602,
        /**
         * Winking face.
         */
        0x1F609,
        /**
         * See-no-evil monkey.
         */
        0x1F648,
        /**
         * Hear-no-evil monkey.
         */
        0x1F649,
        /**
         * Speak-no-evil monkey.
         */
        0x1F64A,
        
        /*
         * 
         */
        
        /**
         * Our end mark.
         */
        BwdUnicode.ONE,
    };

    private static final int INITIAL_WIDTH = 600;
    private static final int INITIAL_HEIGHT = 150;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont unifont;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public UnicodeBwdTestCase() {
    }

    public UnicodeBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new UnicodeBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new UnicodeBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * 
     */
    
    @Override
    public List<String> getUserFontFilePathListElseNull() {
        return Arrays.asList(UNIFONT_FILE_PATH);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect box = g.getBox();

        final InterfaceBwdFont defaultFont = g.getFont();
        final int dh = defaultFont.metrics().height() + 1;
        
        /*
         * 
         */
        
        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        /*
         * Ensuring unifont.
         */
        
        if (this.unifont == null) {
            final InterfaceBwdFontHome fontHome = getBinding().getFontHome();
            /*
             * Should be our unifont.
             */
            final BwdFontKind unifontKind = fontHome.getLoadedUserFontKindSet().first();
            this.unifont = fontHome.newFontWithSize(unifontKind, FONT_SIZE);
        }
        
        /*
         * 
         */
        
        g.setColor(BwdColor.BLACK);
        
        final int fh = this.unifont.metrics().height() + 1;
        int y = 0;
        
        for (boolean mustInsertSpecial : new boolean[]{false, true}) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < CP_ARR.length; i++) {
                final int cp = CP_ARR[i];
                if (mustInsertSpecial) {
                    if (i == 1) {
                        // After first.
                        sb.appendCodePoint(BwdUnicode.NUL);
                    } else if (i == CP_ARR.length - 1) {
                        // Before last.
                        sb.appendCodePoint(BwdUnicode.ST);
                    }
                }
                sb.appendCodePoint(cp);
            }
            final String text = sb.toString();
            
            g.setFont(this.unifont);
            g.drawText(0, y, text);
            y += fh;
            
            g.setFont(defaultFont);
            for (boolean glyphPerGlyph : new boolean[]{false, true}) {
                int width = 0;
                if (glyphPerGlyph) {
                    int ci = 0;
                    while (ci < text.length()) {
                        final int cp = text.codePointAt(ci);
                        width += this.unifont.metrics().computeCharWidth(cp);
                        ci += Character.charCount(cp);
                    }
                    g.drawText(0, y, "glyphs widths sum = " + width);
                } else {
                    width = this.unifont.metrics().computeTextWidth(text);
                    g.drawText(0, y, "text width = " + width);
                }
                y += dh;
            }
            y += dh;
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
