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
package net.jolikit.bwd.test.cases.unittests;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.lang.Dbg;

/**
 * To test InterfaceBwdFont API.
 */
public class FontApiUnitTestBwdTestCase extends AbstractFontUnitTestBwdTestCase {
    
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
     * to avoid a too slow test with some libraries,
     * especially Qt on Mac.
     */
    private static final int[][] CP_RANGE_ARR = new int[][]{
        {0x0000, 0x0FFF}, // 0x1000 ones at BMP start
        {0xFF00, 0xFFFF}, // 0x100 ones at BMP end
        {0x10000, 0x100FF}, // 0x100 ones at first astral plane start
        {0x1FF00, 0x1FFFF}, // 0x100 ones at first astral plane end
        {0x10FF00, 0x10FFFF}, // 0x100 ones at Unicode range end
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

    public FontApiUnitTestBwdTestCase() {
    }
    
    public FontApiUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new FontApiUnitTestBwdTestCase(binding);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<Integer> computeFontSizeOrHeightList(InterfaceBwdFontHome fontHome) {
        final int minFontSize = fontHome.getMinFontSize();
        final int maxFontSize = fontHome.getMinFontSize();
        
        final List<Integer> fontSizeList = new ArrayList<Integer>();
        
        fontSizeList.add(minFontSize);
        //
        fontSizeList.add(BASIC_FONT_SIZE);
        //
        fontSizeList.add(maxFontSize);
        
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
            this.checkCanDisplay(fontHome, font);
        } finally {
            font.dispose();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void checkCanDisplay(
            InterfaceBwdFontHome fontHome,
            InterfaceBwdFont font) {
        
        boolean gotError = false;
        
        /*
         * Invalid code points.
         */
        
        for (int badCp : BAD_CP_ARR) {
            if (DEBUG) {
                Dbg.log("checking canDisplay(" + BwdUnicode.toDisplayString(badCp) + ")");
            }
            try {
                font.canDisplay(badCp);
                Dbg.log("ERROR : canDisplay(...) didn't throw IAE for " + BwdUnicode.toDisplayString(badCp));
                gotError = true;
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        /*
         * Some libraries don't provide a "can display" information.
         * For these we can use FontBox for user fonts since we have
         * a file path, but for system fonts we are helpless, so we
         * only consider user fonts here.
         */
        
        final boolean canRelyOnCanDisplay = fontHome.getLoadedUserFontKindSet().contains(font.kind());
        if (canRelyOnCanDisplay) {
            for (int[] minMaxCpArr : CP_RANGE_ARR) {
                final int minCp = minMaxCpArr[0];
                final int maxCp = minMaxCpArr[1];
                if (DEBUG) {
                    final String minCpStr = BwdUnicode.toDisplayString(minCp);
                    final String maxCpStr = BwdUnicode.toDisplayString(maxCp);
                    Dbg.log("checking canDisplay(int) in [" + minCpStr + ", " + maxCpStr + "]");
                }
                for (int cp = minCp; cp <= maxCp; cp++) {
                    // Checking at least that it doesn't throw.
                    final boolean can = font.canDisplay(cp);
                    
                    // All fonts should be able to display these.
                    final boolean mustCan =
                            (cp >= (int) '0') && (cp <= (int) '9')
                            || (cp >= (int) 'A') && (cp <= (int) 'Z')
                            || (cp >= (int) 'a') && (cp <= (int) 'z');
                    if (mustCan) {
                        if (!can) {
                            Dbg.log("ERROR : canDisplay(...) : false for code point " + BwdUnicode.toDisplayString(cp) + " (" + (char) cp + ")");
                            gotError = true;
                        }
                    }
                }
            }
        }
        
        if (gotError) {
            this.onError(font);
        }
    }
}
