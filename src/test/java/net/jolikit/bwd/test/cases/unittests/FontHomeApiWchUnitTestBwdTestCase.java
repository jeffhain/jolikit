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
 * To test InterfaceBwdFontHome.newFontWithClosestHeight(...).
 */
public class FontHomeApiWchUnitTestBwdTestCase extends AbstractFontUnitTestBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final int INCR_BY_ONE_UP_TO_FONT_SIZE_OR_HEIGHT = 20;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontHomeApiWchUnitTestBwdTestCase() {
    }
    
    public FontHomeApiWchUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new FontHomeApiWchUnitTestBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new FontHomeApiWchUnitTestBwdTestCase(this.getBinding());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<Integer> computeFontSizeOrHeightList(InterfaceBwdFontHome fontHome) {

        final int minFontSize = fontHome.getMinFontSize();
        final int maxFontSize = fontHome.getMaxFontSize();
        
        final List<Integer> fontHeightList = new ArrayList<Integer>();
        if (this.isQtjOnWin()) {
            /*
             * Really not many, because newFontWithClosestHeight(...)
             * typically creates a few fonts internally.
             */
            final int minMaxDelta = maxFontSize - minFontSize;
            
            fontHeightList.add(minFontSize + minMaxDelta / 3);
            //
            fontHeightList.add(maxFontSize - minMaxDelta / 3);
        } else {
            // Target font height can be outside font size validity range,
            // just needs to be > 0.
            final int minFontHeight = 1;
            final int maxFontHeightToUse = (int) (2.0 * maxFontSize);
            
            int tmpFontHeight = minFontHeight;
            while (tmpFontHeight <= maxFontHeightToUse) {
                fontHeightList.add(tmpFontHeight);
                
                if (tmpFontHeight < Math.min(maxFontHeightToUse, INCR_BY_ONE_UP_TO_FONT_SIZE_OR_HEIGHT)) {
                    tmpFontHeight++;
                } else {
                    if (tmpFontHeight <= maxFontHeightToUse / 2) {
                        tmpFontHeight *= 2;
                    } else if (tmpFontHeight < maxFontHeightToUse) {
                        tmpFontHeight = maxFontHeightToUse;
                    } else {
                        break;
                    }
                }
            }
        }
        return fontHeightList;
    }

    @Override
    protected void testFontsOfKindForSizeOrHeight(
            InterfaceBwdFontHome fontHome,
            BwdFontKind fontKind,
            int targetFontHeight) {
        if (DEBUG) {
            Dbg.log("creating font : kind = " + fontKind + ", target height = " + targetFontHeight);
        }
        final InterfaceBwdFont font = fontHome.newFontWithClosestHeight(fontKind, targetFontHeight);
        try {
            if (DEBUG) {
                Dbg.log("checking font " + font);
            }
            this.checkFontSizeForHeightPastLimits(fontHome, font, targetFontHeight);
            this.checkHeightCloseness(fontHome, font, targetFontHeight);
        } finally {
            font.dispose();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * Checks for when target height is out of font size validity range,
     * or at its bounds.
     */
    private void checkFontSizeForHeightPastLimits(
            InterfaceBwdFontHome fontHome,
            InterfaceBwdFont font,
            int targetFontHeight) {
        
        boolean gotError = false;
        
        final int minFontSize = fontHome.getMinFontSize();
        final int maxFontSize = fontHome.getMaxFontSize();
        
        final int fontSize = font.size();
        if ((targetFontHeight > minFontSize)
                && (targetFontHeight < maxFontSize)) {
            // Not this method's business.
            return;
        }
        
        final String family = font.family();
        if (this.isCrazyFontFamily(family)) {
            return;
        }
        
        if (targetFontHeight <= minFontSize / MAX_SIZE_HEIGHT_RATIO) {
            if (fontSize != minFontSize) {
                gotError = true;
                Dbg.log(
                        "ERROR : targetFontHeight = " + targetFontHeight
                        + ", minFontSize = " + minFontSize
                        + ", fontSize = " + fontSize);
            }
        } else if (targetFontHeight >= maxFontSize * MAX_SIZE_HEIGHT_RATIO) {
            if (fontSize != maxFontSize) {
                gotError = true;
                Dbg.log(
                        "ERROR : targetFontHeight = " + targetFontHeight
                        + ", maxFontSize = " + maxFontSize
                        + ", fontSize = " + fontSize);
            }
        }
        
        if (gotError) {
            this.onError(font);
        }
    }

    /**
     * Checks for when target height is within font size validity range.
     */
    private void checkHeightCloseness(
            InterfaceBwdFontHome fontHome,
            InterfaceBwdFont font,
            int targetFontHeight) {
        
        final InterfaceBwdFontMetrics metrics = font.metrics();
        final int fontHeight = metrics.height();
        
        boolean gotError = false;
        
        final int minFontSize = fontHome.getMinFontSize();
        final int maxFontSize = fontHome.getMaxFontSize();
        
        if ((targetFontHeight < minFontSize)
                || (targetFontHeight > maxFontSize)) {
            // Not this method's business.
            return;
        }
        
        final String family = font.family();
        if (this.isCrazyFontFamily(family)) {
            return;
        }

        final boolean isTargetHeightCloseToSizeLimits =
                (targetFontHeight <= minFontSize + targetFontHeight / 2)
                || (targetFontHeight >= maxFontSize - targetFontHeight / 2);
        
        /*
         * Checking that font height is not too far from target font height.
         */
        
        // Using +-1 to add a tolerance to avoid issues when rounding to small mathematical integers.
        final double heightOverTargetHeightRatioHi = (fontHeight + 1.0) / (double) targetFontHeight;
        final double minHeightOverTargetHeightRatioHi;
        if (isTargetHeightCloseToSizeLimits) {
            minHeightOverTargetHeightRatioHi = 1.0 / MAX_SIZE_HEIGHT_RATIO;
        } else {
            minHeightOverTargetHeightRatioHi = 0.9;
        }
        if (heightOverTargetHeightRatioHi < minHeightOverTargetHeightRatioHi) {
            gotError = true;
            Dbg.log(
                    "ERROR : (font height + 1) / target font height [" + heightOverTargetHeightRatioHi + "] < " + minHeightOverTargetHeightRatioHi
                    + ", target font height = " + targetFontHeight);
        }

        final double heightOverTargetHeightRatioLo = (fontHeight - 1.0) / (double) (targetFontHeight + 1.0);
        final double maxHeightOverTargetHeightRatioLo;
        if (isTargetHeightCloseToSizeLimits) {
            maxHeightOverTargetHeightRatioLo = MAX_SIZE_HEIGHT_RATIO;
        } else {
            maxHeightOverTargetHeightRatioLo = 1.1;
        }
        if (heightOverTargetHeightRatioLo > maxHeightOverTargetHeightRatioLo) {
            gotError = true;
            Dbg.log(
                    "ERROR : (font height - 1) / (target font height + 1) [" + heightOverTargetHeightRatioLo + "] > " + maxHeightOverTargetHeightRatioLo
                    + ", target font height = " + targetFontHeight);
        }

        if (gotError) {
            this.onError(font);
        }
    }
}
