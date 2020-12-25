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
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.BindingStringUtils;
import net.jolikit.bwd.test.cases.utils.AbstractUnitTestBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.OsUtils;

/**
 * To factor test code.
 */
public abstract class AbstractFontUnitTestBwdTestCase extends AbstractUnitTestBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Max size/height or height/size ratio.
     * 2 should be large enough.
     */
    protected static final double MAX_SIZE_HEIGHT_RATIO = 2.0;

    /**
     * For debug, better than throwing.
     */
    private static final boolean MUST_EXIT_ON_ERROR = false;
    
    /**
     * There can be a lot, and messy ones too.
     */
    private static final boolean MUST_TEST_WITH_SYSTEM_FONTS_TOO = false;

    /**
     * For debug.
     * 
     * null for regular tests.
     */
    private static final BwdFontKind ONLY_FONT_KIND_TO_USE = (false ? new BwdFontKind("Fixedsys", false, false) : null);

    /**
     * Note that binding might fail to load any of them.
     */
    private static final String[] USER_FONT_FILE_PATH_ARR = new String[]{
        BwdTestResources.TEST_FONT_FREE_MONO_OTF,
        BwdTestResources.TEST_FONT_FREE_MONO_BOLD_OBLIQUE_OTF,
        //
        BwdTestResources.TEST_FONT_LUCIDA_CONSOLE_TTF,
        BwdTestResources.TEST_FONT_LUCIDA_SANS_UNICODE_TTF,
        //
        BwdTestResources.TEST_FONT_UNIFONT_8_0_01_TTF,
        BwdTestResources.TEST_FONT_WQY_MICROHEI_TTF,
    };
    
    private static final int TEXT_X = 10;
    private static final int TEXT_Y = 10;
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * TODO qtj On Windows 7, after about 1050 font loads,
     * Qt outputs the following error on each font load:
     * "QFontEngine::loadEngine: CreateFontIndirect failed ()"
     * and a default font is returned instead, with a default size.
     * 
     * So when testing Qt, we take care not to use too many fonts.
     */
    private boolean isQtjOnWin;

    /*
     * 
     */
    
    private List<BwdFontKind> fontKindList;
    private List<Integer> fontSizeOrHeightList;

    private int nbrOfSteps;
    
    private int stepIndex;
    
    private boolean done = false;
    
    private int errorCount = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractFontUnitTestBwdTestCase() {
    }
    
    public AbstractFontUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
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
    
    protected abstract List<Integer> computeFontSizeOrHeightList(InterfaceBwdFontHome fontHome);
    
    protected abstract void testFontsOfKindForSizeOrHeight(
            InterfaceBwdFontHome fontHome,
            BwdFontKind fontKind,
            int fontSizeOrHeight);

    protected boolean isQtjOnWin() {
        return this.isQtjOnWin;
    }
    
    /*
     * 
     */
    
    @Override
    protected void onTestBegin() {
        
        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        
        /*
         * 
         */
        
        test_newFontWithSize_int(fontHome);
        
        test_newFontWithClosestHeight_int(fontHome);
        
        test_newFontWithFloorElseClosestHeight_int(fontHome);
        
        test_newFontWithCeilingElseClosestHeight_int(fontHome);
        
        /*
         * Initialization.
         */
        
        this.isQtjOnWin =
                OsUtils.isWindows()
                && BwdTestUtils.isQtjBinding(binding);
        
        final int minFontSize = fontHome.getMinFontSize();
        final int maxFontSize = fontHome.getMaxFontSize();
        if (minFontSize < 1) {
            throw new AssertionError("" + minFontSize);
        }
        if (maxFontSize < minFontSize) {
            throw new AssertionError(maxFontSize + " < " + minFontSize);
        }
        
        final SortedSet<BwdFontKind> fontKindSet;
        if (ONLY_FONT_KIND_TO_USE != null) {
            fontKindSet = new TreeSet<BwdFontKind>();
            fontKindSet.add(ONLY_FONT_KIND_TO_USE);
        } else {
            final SortedSet<BwdFontKind> loadedFontKindSet =
                    fontHome.getLoadedFontKindSet();
            
            checkNoVerticalFontLoaded(loadedFontKindSet);
            
            if (MUST_TEST_WITH_SYSTEM_FONTS_TOO) {
                fontKindSet = loadedFontKindSet;
            } else {
                final SortedSet<BwdFontKind> userFontKindSet = fontHome.getLoadedUserFontKindSet();
                if (userFontKindSet.size() == 0) {
                    /*
                     * Can happen if binding fails to load any user font,
                     * or to figure out it loaded one.
                     * 
                     * Using up to a certain amount of any font kind.
                     */
                    fontKindSet = new TreeSet<BwdFontKind>();
                    int takenCount = 0;
                    for (BwdFontKind fontKind : loadedFontKindSet) {
                        if (takenCount >= USER_FONT_FILE_PATH_ARR.length) {
                            break;
                        }
                        fontKindSet.add(fontKind);
                        takenCount++;
                    }
                } else {
                    fontKindSet = userFontKindSet;
                }
            }
        }
        
        final List<BwdFontKind> fontKindList = new ArrayList<BwdFontKind>(fontKindSet);
        this.fontKindList = fontKindList;
        
        final List<Integer> fontSizeOrHeightList = this.computeFontSizeOrHeightList(fontHome);
        this.fontSizeOrHeightList = fontSizeOrHeightList;
        
        /*
         * 
         */
        
        this.nbrOfSteps = fontKindList.size() * fontSizeOrHeightList.size();
        
        this.stepIndex = 0;
    }
    
    @Override
    protected void onTestEnd() {
        this.done = true;
    }
    
    @Override
    protected long testSome(long nowNs) {
        
        final int stepIndex = this.stepIndex++;
        
        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceBwdFontHome fontHome = binding.getFontHome();

        final int nbrOfSizeOrHeight = this.fontSizeOrHeightList.size();
        final int fontKindIndex = (stepIndex / nbrOfSizeOrHeight);
        final int fontSizeOrHeightIndex = (stepIndex % nbrOfSizeOrHeight);
        
        final BwdFontKind fontKind = this.fontKindList.get(fontKindIndex);
        final int fontSizeOrHeight = this.fontSizeOrHeightList.get(fontSizeOrHeightIndex);
        
        this.testFontsOfKindForSizeOrHeight(
                fontHome,
                fontKind,
                fontSizeOrHeight);
        
        /*
         * 
         */
        
        this.getHost().ensurePendingClientPainting();
        
        if (stepIndex == this.nbrOfSteps - 1) {
            // Done.
            return Long.MAX_VALUE;
        } else {
            // ASAP.
            return Long.MIN_VALUE;
        }
    }

    @Override
    protected void drawCurrentState(InterfaceBwdGraphics g) {
        if (!this.done) {
            g.setColor(BwdColor.BLACK);
            g.drawText(TEXT_X, TEXT_Y, "Testing...");
            
            if (this.fontSizeOrHeightList == null) {
                // Didn't begin yet.
            } else {
                // Indicating what is done.
                final int stepIndex = this.stepIndex - 1;
                
                final int nbrOfSizeOrHeight = this.fontSizeOrHeightList.size();
                final int fontKindIndex = (stepIndex / nbrOfSizeOrHeight);
                final int fontSizeOrHeightIndex = (stepIndex % nbrOfSizeOrHeight);
                
                g.drawText(
                        TEXT_X,
                        TEXT_Y + (g.getFont().metrics().height() + 1),
                        "(" + (fontKindIndex + 1) + " / " + this.fontKindList.size() + ")"
                        + ", (" + (fontSizeOrHeightIndex + 1) + " / " + this.fontSizeOrHeightList.size() + ")"
                        + ", error count = " + this.errorCount);
            }
        } else {
            g.setColor(BwdColor.BLACK);
            if (this.errorCount != 0) {
                g.drawText(TEXT_X, TEXT_Y, "Testing done : KO : " + this.errorCount + " errors");
            } else {
                g.drawText(TEXT_X, TEXT_Y, "Testing done : OK.");
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * Fonts for which MAX_SIZE_HEIGHT_RATIO doesn't hold with some library/OS combos.
     */
    protected boolean isCrazyFontFamily(String family) {
        if (MUST_TEST_WITH_SYSTEM_FONTS_TOO) {
            return
                    /*
                     * For newFontWithSize(...) test, with Qt:
2432291994 1 ; ERROR : (font height - 1) / (font size + 1) [4.571428571428571] > 2.0
2432382171 1 ; font in error : [[Cambria Math, normal, 6], [h = 33, a = 19, d = 14]]
(...)
2493308740 1 ; ERROR : (font height - 1) / (font size + 1) [5.579419573377277] > 2.0
2493375134 1 ; font in error : [[Cambria Math, normal, 10922], [h = 60945, a = 34041, d = 26904]]
(...)
11765280365 1 ; ERROR : (font height - 1) / (font size + 1) [2.142857142857143] > 2.0
11765341474 1 ; font in error : [[System, normal, 6], [h = 16, a = 14, d = 2]]
(...)
11821757545 1 ; ERROR : (font height + 1) / font size [0.011719465299395715] < 0.5
11821801478 1 ; font in error : [[System, normal, 10922], [h = 127, a = 104, d = 23]]
                     */
                    (BindingStringUtils.containsIgnoreCase(family, "Cambria")
                            && BindingStringUtils.containsIgnoreCase(family, "Math"))
                    
                    /*
                     * DilleniaUPC, etc.
                     */
                    || BindingStringUtils.containsIgnoreCase(family, "UPC")
                    
                    /*
                     * "System" height seems to stall at 104 or 127
                     */
                    || BindingStringUtils.containsIgnoreCase(family, "System")
                    
                    /*
                     * SWT: (size,height) = (154,88) and (155,155)
                     */
                    || (BindingStringUtils.containsIgnoreCase(family, "Small") && BindingStringUtils.containsIgnoreCase(family, "Fonts"))
                    
                    || BindingStringUtils.containsIgnoreCase(family, "Fixedsys")
                    
                    /*
                     * STIXSizeFourSym, STIXSizeThreeSym, etc.
                     */
                    || BindingStringUtils.containsIgnoreCase(family, "STIX")
                    
                    || BindingStringUtils.containsIgnoreCase(family, "Consolas")
                    
                    || BindingStringUtils.containsIgnoreCase(family, "Zapfino")
                    
                    || BindingStringUtils.containsIgnoreCase(family, "Farisi")
                    ;
        }

        return false;
    }

    protected void onError(InterfaceBwdFont font) {
        this.errorCount++;
        Dbg.log("font in error : " + font);
        
        if (MUST_EXIT_ON_ERROR) {
            Dbg.flushAndExit();
        } else {
            // Just counting on error logs.
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Checks that no font kind starts with '@',
     * as specified by InterfaceBwdFontHome javadoc.
     */
    private static void checkNoVerticalFontLoaded(SortedSet<BwdFontKind> loadedFontKindSet) {
        for (BwdFontKind fontKind : loadedFontKindSet) {
            final String family = fontKind.family();
            if (family.startsWith("@")) {
                throw new AssertionError("vertical font kind: " + fontKind);
            }
        }
    }
    
    private static void test_newFontWithSize_int(InterfaceBwdFontHome fontHome) {
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        
        final int fontSize = defaultFont.size() + 1;
        final InterfaceBwdFont font = fontHome.newFontWithSize(fontSize);
        try {
            if (!font.kind().equals(defaultFont.kind())) {
                throw new AssertionError(font.kind() + " != " + defaultFont.kind());
            }
            if (font.size() != fontSize) {
                throw new AssertionError(font.size() + " != " + fontSize);
            }
        } finally {
            font.dispose();
        }
    }
    
    private static void test_newFontWithClosestHeight_int(InterfaceBwdFontHome fontHome) {
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        
        final int targetFontHeight = 2 * defaultFont.size();
        final InterfaceBwdFont expected = fontHome.newFontWithClosestHeight(
            defaultFont.kind(),
            targetFontHeight);
        final InterfaceBwdFont actual = fontHome.newFontWithClosestHeight(targetFontHeight);
        try {
            if (!actual.equals(expected)) {
                throw new AssertionError(actual + " != " + expected);
            }
        } finally {
            expected.dispose();
            actual.dispose();
        }
    }
    
    private static void test_newFontWithFloorElseClosestHeight_int(InterfaceBwdFontHome fontHome) {
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        
        final int targetFontHeight = 1;
        final InterfaceBwdFont expected = fontHome.newFontWithFloorElseClosestHeight(
            defaultFont.kind(),
            targetFontHeight);
        final InterfaceBwdFont actual = fontHome.newFontWithFloorElseClosestHeight(targetFontHeight);
        try {
            if (!actual.equals(expected)) {
                throw new AssertionError(actual + " != " + expected);
            }
        } finally {
            expected.dispose();
            actual.dispose();
        }
    }
    
    private static void test_newFontWithCeilingElseClosestHeight_int(InterfaceBwdFontHome fontHome) {
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        
        final int targetFontHeight = Integer.MAX_VALUE;
        final InterfaceBwdFont expected = fontHome.newFontWithCeilingElseClosestHeight(
            defaultFont.kind(),
            targetFontHeight);
        final InterfaceBwdFont actual = fontHome.newFontWithCeilingElseClosestHeight(targetFontHeight);
        try {
            if (!actual.equals(expected)) {
                throw new AssertionError(actual + " != " + expected);
            }
        } finally {
            expected.dispose();
            actual.dispose();
        }
    }
}
