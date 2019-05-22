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
package net.jolikit.bwd.test.cases.unittests;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * To test InterfaceBwdFontMetrics API.
 */
public class FontMetricsApiUnitTestBwdTestCase extends AbstractFontUnitTestBwdTestCase {
    
    /*
     * NB: Lessons learned from now removed checks
     * (because our spec doesn't mandate the behaviors they tested):
     * - With some special code points, such as diacritical marks
     *   (0x300, 0x301, etc.), computed text width of "<cp><cp>" can be 0,
     *   while computed text width of "<cp>" can be > 0.
     * - With SWT, computed text width shrinks when 0x20F0
     *   ("combining diacritical marks for symbols") is inserted.
     * - With Qt, computed text width shrinks when 0xFFFC
     *   ("object replacement character") or 0x10FFFC
     *   ("supplementary private use area-b") is inserted.
     * - With Qt, computed text width stops at 0x9C (string terminator),
     *   but not drawing.
     * - With Allegro and SDL, computed text width stops at NUL,
     *   as well as text drawing.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * We want to do checks on many code points,
     * so we don't use many font sizes, just min, max, and this one.
     */
    private static final int BASIC_FONT_SIZE = 20;
    
    /**
     * Not testing the whole Unicode range,
     * to avoid a too slow test with some libraries.
     */
    private static final int[][] CP_RANGE_ARR = new int[][]{
        {0, 0xFFF}, // 0x1000 ones starting BMP
        {0xF000, 0xFFFF}, // 0x1000 ones ending BMP
        {0x10000, 0x10FFF}, // 0x1000 ones starting first astral plane
        {0x1F000, 0x1FFFF}, // 0x1000 ones ending first astral plane
        {0x10F000, 0x10FFFF}, // 0x1000 ones at end of Unicode range
    };
    
    private static final int[] BAD_CP_ARR = new int[]{
        Integer.MIN_VALUE,
        -1,
        BwdUnicode.MAX_10FFFF + 1,
        Integer.MAX_VALUE,
    };
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontMetricsApiUnitTestBwdTestCase() {
    }
    
    public FontMetricsApiUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new FontMetricsApiUnitTestBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new FontMetricsApiUnitTestBwdTestCase(this.getBinding());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<Integer> computeFontSizeOrHeightList(InterfaceBwdFontHome fontHome) {
        /*
         * Not testing max font size,
         * for it could take much time with some libraries.
         */
        final int minFontSize = fontHome.getMinFontSize();
        
        final List<Integer> fontSizeList = new ArrayList<Integer>();
        
        fontSizeList.add(minFontSize);
        //
        fontSizeList.add(BASIC_FONT_SIZE);
        
        return fontSizeList;
    }

    @Override
    protected void testFontsOfKindForSizeOrHeight(
            InterfaceBwdFontHome fontHome,
            BwdFontKind fontKind,
            int fontSize) {
        if (DEBUG) {
            Dbg.log("creating font : kind = " + fontKind + ", size = " + fontSize);
        }
        final InterfaceBwdFont font = fontHome.newFontWithSize(fontKind, fontSize);
        try {
            if (DEBUG) {
                Dbg.log("checking font " + font);
            }
            this.checkVerticalMetricsValidity(font);
            this.checkHorizontalMetricsValidity(font);
        } finally {
            font.dispose();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void checkVerticalMetricsValidity(InterfaceBwdFont font) {
        
        boolean gotError = false;
        
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int ascent = metrics.fontAscent();
        if (ascent < 0) {
            Dbg.log("ERROR : ascent [" + ascent + "] must be >= 0");
            gotError = true;
        }
        final int descent = metrics.fontDescent();
        if (descent < 0) {
            Dbg.log("ERROR : descent [" + descent + "] must be >= 0");
            gotError = true;
        }
        final int fontHeight = metrics.fontHeight();
        if (fontHeight <= 0) {
            Dbg.log("ERROR : fontHeight [" + fontHeight + "] must be >= 1");
            gotError = true;
        }
        if (fontHeight != ascent + descent) {
            Dbg.log("ERROR : fontHeight [" + fontHeight + "] != ascent [" + ascent + "] + descent [" + descent + "]");
            gotError = true;
        }
        
        if (gotError) {
            this.onError(font);
        }
    }
    
    private void checkHorizontalMetricsValidity(InterfaceBwdFont font) {
        
        boolean gotError = false;
        
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        
        /*
         * Invalid code points.
         */
        
        for (int badCp : BAD_CP_ARR) {
            try {
                metrics.computeCharWidth(badCp);
                Dbg.log("ERROR : computeCharWidth(...) didn't throw IAE for " + BwdUnicode.toDisplayString(badCp));
                gotError = true;
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        /*
         * 
         */

        for (int[] minMaxCpArr : CP_RANGE_ARR) {
            final int minCp = minMaxCpArr[0];
            final int maxCp = minMaxCpArr[1];
            for (int cp = minCp; cp <= maxCp; cp++) {
                /*
                 * 'cp'
                 */
                final int cpWidth = metrics.computeCharWidth(cp);
                if (cpWidth < 0) {
                    Dbg.log(
                            "ERROR : computeCharWidth(cp) : width [" + cpWidth + "] is negative for code point "
                                    + BwdUnicode.toDisplayString(cp) + " (" + (char) cp + ")");
                    gotError = true;
                }
                /*
                 * "cp"
                 */
                final String cpText = LangUtils.stringOfCodePoint(cp);
                final int cpTextWidth = metrics.computeTextWidth(cpText);
                {
                    if (cpTextWidth < 0) {
                        Dbg.log(
                                "ERROR : computeTextWidth(cp) : width [" + cpTextWidth + "] is negative for code point "
                                        + BwdUnicode.toDisplayString(cp) + " (" + (char) cp + ")");
                        gotError = true;
                    }
                    if (cpWidth != cpTextWidth) {
                        Dbg.log(
                                "ERROR : computeCharWidth(cp) [" + cpWidth + "] != computeTextWidth(cp) [" + cpTextWidth + "] for code point "
                                        + BwdUnicode.toDisplayString(cp) + " (" + (char) cp + ")");
                        gotError = true;
                    }
                }
            }
        }
        
        if (gotError) {
            this.onError(font);
        }
    }
}
