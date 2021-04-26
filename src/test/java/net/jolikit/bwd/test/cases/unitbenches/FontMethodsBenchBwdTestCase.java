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
package net.jolikit.bwd.test.cases.unitbenches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractUnitTestBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.lang.LangUtils;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.time.TimeUtils;

/**
 * To bench:
 * - canDisplay(...)
 * - computeCharWidth(...)
 * - computeTextWidth(...)
 */
public class FontMethodsBenchBwdTestCase extends AbstractUnitTestBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int MIN_CP = 0;
    /**
     * Not bothering with code points outside BMP.
     * Also allows for each run not to be too long.
     */
    private static final int MAX_CP = 0xFFFF;

    /**
     * Not many runs, because some libraries are really slow.
     * At least 2, to see warmup overhead, or eventual caching speedup.
     */
    private static final int NBR_OF_RUNS = 2;
    private static final int NBR_OF_CALLS = (MAX_CP - MIN_CP + 1);
    
    /*
     * 
     */

    private static final int[] TARGET_FONT_HEIGHT_ARR = new int[]{
        20,
        280};
    
    /**
     * One OTF, one TTF.
     */
    private static final String[] USER_FONT_FILE_PATH_ARR = new String[]{
        BwdTestResources.TEST_FONT_FREE_MONO_OTF,
        BwdTestResources.TEST_FONT_UNIFONT_8_0_01_TTF,
    };
    
    /**
     * Some rest time, else with some bindings no refresh is done until end.
     */
    private static final double REST_TIME_BETWEEN_BENCHES_S = 0.1;
    
    private static final int INITIAL_WIDTH = 600;
    private static final int INITIAL_HEIGHT = 400;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyMethodType {
        /**
         * canDisplay(...)
         */
        CAN_DISPLAY,
        /**
         * computeCharWidth(...)
         */
        COMPUTE_CHAR_WIDTH,
        /**
         * computeTextWidth(...)
         */
        COMPUTE_TEXT_WIDTH;
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final MyMethodType[] METHOD_TYPE_ARR = MyMethodType.values();
    
    private final List<BwdFontKind> fontKindList = new ArrayList<BwdFontKind>();
    
    private final List<String> reportList = new ArrayList<String>();
    
    private int nbrOfSteps;
    
    private int stepIndex;
    
    private InterfaceBwdFont currentFont = null;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontMethodsBenchBwdTestCase() {
    }
    
    public FontMethodsBenchBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new FontMethodsBenchBwdTestCase(binding);
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
    protected void onTestBegin() {
        
        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        
        this.fontKindList.addAll(fontHome.getLoadedUserFontKindSet());
        
        this.nbrOfSteps =
                NBR_OF_RUNS
                * this.fontKindList.size()
                * TARGET_FONT_HEIGHT_ARR.length
                * METHOD_TYPE_ARR.length;

        this.stepIndex = 0;
    }
    
    @Override
    protected void onTestEnd() {
        this.reportList.add("Done.");
    }

    @Override
    protected long testSome(long nowNs) {
        
        final int stepIndex = this.stepIndex++;
        
        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        
        /*
         * 
         */
        
        int tmpIndex = stepIndex;
        
        int tmpDivisor = NBR_OF_RUNS;
        final int runIndex = tmpIndex % tmpDivisor;
        tmpIndex /= tmpDivisor;

        tmpDivisor = this.fontKindList.size();
        final int fontKindIndex = tmpIndex % tmpDivisor;
        tmpIndex /= tmpDivisor;

        tmpDivisor = TARGET_FONT_HEIGHT_ARR.length;
        final int targetFontHeightIndex = tmpIndex % tmpDivisor;
        tmpIndex /= tmpDivisor;

        tmpDivisor = METHOD_TYPE_ARR.length;
        final int methodTypeIndex = tmpIndex % tmpDivisor;
        tmpIndex /= tmpDivisor;
        
        /*
         * 
         */

        final BwdFontKind fontKind = this.fontKindList.get(fontKindIndex);
        final int targetFontHeight = TARGET_FONT_HEIGHT_ARR[targetFontHeightIndex];
        final MyMethodType methodType = METHOD_TYPE_ARR[methodTypeIndex];
        
        /*
         * Bench.
         */
        
        {
            if (runIndex == 0) {
                this.currentFont = fontHome.newFontWithClosestHeight(fontKind, targetFontHeight);
            }
            final InterfaceBwdFont font = this.currentFont;
            try {
                final InterfaceBwdFontMetrics metrics = font.metrics();
                
                int antiOptim = 0;
                {
                    final long a = System.nanoTime();
                    for (int cp = MIN_CP; cp <= MAX_CP; cp++) {
                        final Object res = callMethod(font, methodType, cp);
                        antiOptim += res.hashCode();
                    }
                    final long b = System.nanoTime();
                    final double dtS = TestUtils.nsToSRounded(b - a);
                    final String report =
                            fontKind + ", h = " + metrics.height()
                            + ", " + NBR_OF_CALLS + " calls to " + methodType
                            + ", " + dtS + " s";
                    this.reportList.add(report);
                }
                TestUtils.blackHole(antiOptim);
            } finally {
                if (runIndex == NBR_OF_RUNS - 1) {
                    font.dispose();
                }
            }
        }
        
        /*
         * 
         */
        
        this.getHost().ensurePendingClientPainting();

        if (stepIndex == this.nbrOfSteps - 1) {
            // Done.
            return Long.MAX_VALUE;
        } else {
            return nowNs + TimeUtils.sToNs(REST_TIME_BETWEEN_BENCHES_S);
        }
    }

    @Override
    protected void drawCurrentState(InterfaceBwdGraphics g) {
        this.drawReport(g);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Causes a bit of overhead, but almost nothing compared to the overhead
     * of actual computations (when there is no caching).
     */
    private static Object callMethod(
            InterfaceBwdFont font,
            MyMethodType methodType,
            int codePoint) {
        if (methodType == MyMethodType.CAN_DISPLAY) {
            return font.canDisplay(codePoint);
        } else if (methodType == MyMethodType.COMPUTE_CHAR_WIDTH) {
            return font.metrics().computeCharWidth(codePoint);
        } else if (methodType == MyMethodType.COMPUTE_TEXT_WIDTH) {
            final String text = LangUtils.stringOfCodePoint(codePoint);
            return font.metrics().computeTextWidth(text);
        } else {
            throw new IllegalArgumentException("" + methodType);
        }
    }
    
    private void drawReport(InterfaceBwdGraphics g) {
        final InterfaceBwdFont font = g.getFont();
        final int lineHeight = font.metrics().height() + 1;

        g.setColor(BwdColor.BLACK);
        
        final int x = 0;
        int tmpY = 0;
        for (String report : this.reportList) {
            g.drawText(x, tmpY, report);
            tmpY += lineHeight;
        }
    }
}
