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

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.LangUtils;

/**
 * To test how text containing special characters has its width computed
 * and how it is rendered, and whether these are consistent or not.
 * 
 * The rich variety of related behaviors is a compelling reason why
 * our drawText(...) spec is so relaxed.
 */
public class TextCasesBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_COLUMNS = 2;
    
    private static final String[] TO_INSERT_ARR = new String[]{
        /**
         * With Allegro and SDL, computed text width stops at it,
         * as well as text drawing
         * (unless we special-case for it of course).
         */
        LangUtils.stringOfCodePoint(BwdUnicode.NUL),
        /**
         * With Qt, computed text width stops at it,
         * but NOT text drawing.
         */
        LangUtils.stringOfCodePoint(BwdUnicode.ST),
        /*
         * Spaces (some).
         */
        LangUtils.stringOfCodePoint(BwdUnicode.SPACE),
        LangUtils.stringOfCodePoint(BwdUnicode.NBSP),
        /*
         * Newlines (some).
         */
        LangUtils.stringOfCodePoint(BwdUnicode.CR),
        LangUtils.stringOfCodePoint(BwdUnicode.LF),
        LangUtils.stringOfCodePoint(BwdUnicode.CR) + LangUtils.stringOfCodePoint(BwdUnicode.LF),
        LangUtils.stringOfCodePoint(BwdUnicode.NEL),
        /*
         * Control characters (some).
         */
        LangUtils.stringOfCodePoint(BwdUnicode.SOH),
        LangUtils.stringOfCodePoint(BwdUnicode.STX),
        LangUtils.stringOfCodePoint(BwdUnicode.BACKSPACE),
        LangUtils.stringOfCodePoint(BwdUnicode.HT),
        LangUtils.stringOfCodePoint(BwdUnicode.VT),
        LangUtils.stringOfCodePoint(BwdUnicode.FF),
        LangUtils.stringOfCodePoint(BwdUnicode.DELETE),
        LangUtils.stringOfCodePoint(BwdUnicode.SOS),
        /**
         * "combining diacritical marks for symbols"
         * With SWT, computed text width shrinks when this is inserted.
         */
        LangUtils.stringOfCodePoint(0x20F0),
        /**
         * With Qt, computed text width shrinks when this is inserted.
         */
        LangUtils.stringOfCodePoint(BwdUnicode.ORC),
        /**
         * "supplementary private use area-b"
         * With Qt, computed text width shrinks when this is inserted.
         */
        LangUtils.stringOfCodePoint(0x10FFFC),
    };
    
    private static final int INITIAL_WIDTH = 550;
    private static final int INITIAL_HEIGHT = 300;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public TextCasesBwdTestCase() {
    }

    public TextCasesBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new TextCasesBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new TextCasesBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect box = g.getBox();

        final int xSpan = box.xSpan();
        
        /*
         * 
         */
        
        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        /*
         * 
         */
        
        g.setColor(BwdColor.BLACK);
        final InterfaceBwdFont font = g.getFont();
        
        final int nbrOfRows = 1 + (TO_INSERT_ARR.length - 1) / NBR_OF_COLUMNS;
        
        int tii = 0;
        int tmpX = 0;
        for (int col = 0; col < NBR_OF_COLUMNS; col++) {
            int tmpY = 0;
            for (int row = 0; row < nbrOfRows; row++) {
                if (tii == TO_INSERT_ARR.length) {
                    // did last one already.
                    break;
                }
                final String toInsert = TO_INSERT_ARR[tii++];
                
                final String hexCpStr = computeHexCpStr(toInsert);
                final String sample = "a" + toInsert + "b";
                final int sampleWidth = font.fontMetrics().computeTextWidth(sample);
                final String text = "a+" + hexCpStr + "+b -> w = " + sampleWidth + ", " + sample;
                g.drawText(tmpX, tmpY, text);
                
                // 2 * in case of newline
                tmpY += 2 * font.fontMetrics().fontHeight() + 1;
            }
            tmpX += xSpan / NBR_OF_COLUMNS;
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Ex.: "0xD+0xA"
     */
    private static String computeHexCpStr(String text) {
        final StringBuilder sb = new StringBuilder();
        int ci = 0;
        while (ci < text.length()) {
            final int cp = text.codePointAt(ci);
            if (ci > 0) {
                sb.append("+");
            }
            sb.append(BwdUnicode.toDisplayString(cp));
            ci += Character.charCount(cp);
        }
        return sb.toString();
    }
}
