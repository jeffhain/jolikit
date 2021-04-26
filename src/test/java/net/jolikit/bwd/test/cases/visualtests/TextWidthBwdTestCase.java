/*
 * Copyright 2019-2021 Jeff Hain
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
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.lang.LangUtils;

/**
 * Shows results of computeCharWidth(...) and computeTextWidth(...)
 * for a few particular set of values, to quickly figure out how bindings
 * are behaving.
 */
public class TextWidthBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final String[] USER_FONT_FILE_PATH_ARR = new String[]{
        BwdTestResources.TEST_FONT_FREE_MONO_OTF,
        BwdTestResources.TEST_FONT_LUCIDA_CONSOLE_TTF,
    };
    
    private static final int TARGET_FONT_HEIGHT = 20;
    
    private static final int[] CP_ARR = new int[]{
        BwdUnicode.NUL,
        BwdUnicode.US, // Control character.
        BwdUnicode.BACKSPACE,
        BwdUnicode.HT,
        BwdUnicode.SPACE,
        BwdUnicode.EXCLAMATION_MARK, // Thin glyph.
        BwdUnicode.W, // Wide glyph.
        BwdUnicode.NBSP,
        BwdUnicode.SOFT_HYPHEN,
    };
    
    private static final int INITIAL_WIDTH = 400;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public TextWidthBwdTestCase() {
    }
    
    public TextWidthBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new TextWidthBwdTestCase(binding);
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
        return Arrays.asList(USER_FONT_FILE_PATH_ARR);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final GRect clientBox = g.getBox();
        
        /*
         * 
         */
        
        g.setColor(BwdColor.WHITE);
        g.clearRect(clientBox);
        
        /*
         * 
         */
        
        g.setColor(BwdColor.BLACK);
        
        final InterfaceBwdFont defaultFont = g.getFont();
        final InterfaceBwdFontMetrics defaultFontMetrics = defaultFont.metrics();
        
        int textX = 0;
        int textY = 0;
        g.drawText(textX, textY, "{cw(cp), tw(cp), tw(cpcp), tw(cpcpcp)}");
        textY += defaultFontMetrics.height() + 1;
        
        final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
        
        // Just using default font kind,
        // since behaviors seem consistent across fonts.
        final InterfaceBwdFont font = fontHome.newFontWithClosestHeight(TARGET_FONT_HEIGHT);
        try {
            final InterfaceBwdFontMetrics fontMetrics = font.metrics();

            g.drawText(textX, textY, "font = " + font);
            textY += defaultFontMetrics.height() + 1;

            for (int cp : CP_ARR) {
                final String cps = LangUtils.stringOfCodePoint(cp);
                final int charWidth = fontMetrics.computeCharWidth(cp);
                final int text1Width = fontMetrics.computeTextWidth(cps);
                final int text2Width = fontMetrics.computeTextWidth(cps + cps);
                final int text3Width = fontMetrics.computeTextWidth(cps + cps + cps);
                g.drawText(
                        textX,
                        textY,
                        "cp = " + BwdUnicode.toDisplayString(cp) + " : {"
                                + charWidth + ", " + text1Width + ", " + text2Width + ", " + text3Width
                                + "} (cp = " + LangUtils.stringOfCodePoint(cp) + ")");

                textY += defaultFontMetrics.height() + 1;
            }
        } finally {
            font.dispose();
        }

        return null;
    }
}
