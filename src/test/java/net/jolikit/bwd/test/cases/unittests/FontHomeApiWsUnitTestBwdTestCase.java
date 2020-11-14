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
package net.jolikit.bwd.test.cases.unittests;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;

/**
 * To test InterfaceBwdFontHome.newFontWithSize(...).
 */
public class FontHomeApiWsUnitTestBwdTestCase extends AbstractFontUnitTestBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final int INCR_BY_ONE_UP_TO_FONT_SIZE_OR_HEIGHT = 20;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontHomeApiWsUnitTestBwdTestCase() {
    }
    
    public FontHomeApiWsUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new FontHomeApiWsUnitTestBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new FontHomeApiWsUnitTestBwdTestCase(this.getBinding());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<Integer> computeFontSizeOrHeightList(InterfaceBwdFontHome fontHome) {
        
        final int minFontSize = fontHome.getMinFontSize();
        final int maxFontSize = fontHome.getMaxFontSize();
        
        final List<Integer> fontSizeList = new ArrayList<Integer>();
        if (this.isQtjOnWin()) {
            final int minMaxDelta = maxFontSize - minFontSize;
            
            fontSizeList.add(minFontSize);
            //
            fontSizeList.add(minFontSize + 2);
            fontSizeList.add(minFontSize + 4);
            //
            fontSizeList.add(minFontSize + minMaxDelta / 2);
            //
            fontSizeList.add(maxFontSize);
        } else {
            int tmpFontSize = minFontSize;
            while (tmpFontSize <= maxFontSize) {
                fontSizeList.add(tmpFontSize);
                
                if (tmpFontSize < Math.min(maxFontSize, INCR_BY_ONE_UP_TO_FONT_SIZE_OR_HEIGHT)) {
                    tmpFontSize++;
                } else {
                    if (tmpFontSize <= maxFontSize / 2) {
                        tmpFontSize *= 2;
                    } else if (tmpFontSize < maxFontSize) {
                        tmpFontSize = maxFontSize;
                    } else {
                        break;
                    }
                }
            }
        }
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
            this.checkFontSize(font, fontSize);
            this.checkSizeHeightRatio(font);
        } finally {
            font.dispose();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void checkFontSize(InterfaceBwdFont font, int specifiedFontSize) {
        
        boolean gotError = false;
        
        final int fontSize = font.size();
        if (fontSize != specifiedFontSize) {
            Dbg.log("ERROR : fontSize [" + fontSize + "] != specified fontSize [" + specifiedFontSize + "]");
            gotError = true;
        }
        
        if (gotError) {
            this.onError(font);
        }
    }

    private void checkSizeHeightRatio(InterfaceBwdFont font) {
        
        boolean gotError = false;
        
        final int fontSize = font.size();
        
        final InterfaceBwdFontMetrics metrics = font.metrics();
        final int ascent = metrics.ascent();
        if (ascent < 1) {
            Dbg.log("ERROR : ascent [" + ascent + "] < 1");
            gotError = true;
        }
        final int descent = metrics.descent();
        if (descent < 0) {
            Dbg.log("ERROR : descent [" + descent + "] < 0");
            gotError = true;
        }
        final int fontHeight = metrics.height();
        
        final String family = font.family();
        if (this.isCrazyFontFamily(family)) {
            return;
        }
        
        /*
         * Checking that font height is not too far from font size,
         * which could legitimately happen according to spec, but should not
         * in our bindings which min/max font size limits are supposed to avoid
         * the use of a fallback size.
         */
        
        // Using +-1 to add a tolerance to avoid issues when rounding to small mathematical integers.
        final double heightOverSizeRatioHi = (fontHeight + 1.0) / (double) fontSize;
        final double minHeightOverSizeRatioHi = 1.0 / MAX_SIZE_HEIGHT_RATIO;
        if (heightOverSizeRatioHi < minHeightOverSizeRatioHi) {
            Dbg.log("ERROR : (font height + 1) / font size [" + heightOverSizeRatioHi + "] < " + minHeightOverSizeRatioHi);
            gotError = true;
        }

        final double heightOverSizeRatioLo = (fontHeight - 1.0) / (double) (fontSize + 1.0);
        final double maxHeightOverSizeRatioLo = MAX_SIZE_HEIGHT_RATIO;
        if (heightOverSizeRatioLo > maxHeightOverSizeRatioLo) {
            Dbg.log("ERROR : (font height - 1) / (font size + 1) [" + heightOverSizeRatioLo + "] > " + maxHeightOverSizeRatioLo);
            gotError = true;
        }

        if (gotError) {
            this.onError(font);
        }
    }
}
