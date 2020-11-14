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
package net.jolikit.bwd.impl.utils.fonts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;
import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.impl.utils.fonts.FontOfHeightHelper.InterfaceFontCreator;
import net.jolikit.test.utils.TestUtils;

public class FontOfHeightHelperTest extends TestCase {

    /*
     * Most of the tests are done using Integer.MAX_VALUE as max number of fonts
     * created, for best result to always be found, which makes testing much easier.
     * It also allows to let occur some eventual infinite looping issue.
     * 
     * We don't cover cases where fonts of inferior size have superior height
     * (crazy cases where we give no guarantee on the closeness
     * of the returned font).
     * 
     * Some stats can be printed to help figure out best algorithm configuration,
     * but it's better to actually tune algorithm on actual fonts, which our
     * random fonts don't model very well (much less intermediary fonts
     * with actual fonts for some reason).
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    private static final boolean MUST_PRINT_TUNING_STATS = DEBUG || false;

    /**
     * For tests to run faster, when we don't tune.
     */
    private static final boolean DISABLE_ALL_FONTS_ARCHETYPES_TEST = true;
    
    /**
     * Number of calls, with a random target height each time,
     * per couple of (font, algorithm configuration).
     */
    private static final int NBR_OF_CALLS = 100;

    private static final int MIN_FONT_SIZE = 1;

    /**
     * Small enough for (brute force) tests to go fast, and to allow for
     * font heights much larger than font size.
     * Large enough to allow for font heights much smaller than font size,
     * and to smooth out stats due to having various size/height combinations.
     */
    private static final int MAX_FONT_SIZE = 100;

    private static final int MIN_FONT_HEIGHT = 1;

    /*
     * Fonts archetypes configs.
     */

    /**
     * Not using 0.5 or 2.0, which would give pathologically early
     * and exact results when first loop factor is 2.
     */
    private static final double[] MEAN_FONT_SLOPE_ARR = new double[]{0.7, 1.0, 1.4};

    /**
     * 0 for fontHeight=f(fontSize) to be a perfect line.
     * MAX_FONT_SIZE for font height to be very non-linear
     * (NB: it depend a bit on mean font slope).
     */
    private static final int[] MAX_HEIGHT_DELTA_FROM_REF_HEIGHT_ARR = new int[]{0, 5, 10, MAX_FONT_SIZE};

    /*
     * 
     */

    /**
     * Number of randomly generated font used
     * for each kind of test configuration,
     * not to have results particular to a given font.
     */
    private static final int NBR_OF_RANDOM_FONTS = 5;

    /*
     * Algos configs.
     */

    /**
     * Large enough for expected result to always be found.
     */
    private static final int DEFAULT_MAX_NBR_OF_FONT_CREATIONS = Integer.MAX_VALUE;

    private static final double[] FIRST_LOOP_FACTOR_ARR = new double[]{1.1, 1.25, 1.5, 2.0};
    private static final int[] FIRST_LOOP_PROP_ARR = new int[]{0, 1, 2, 3, 4, 5};
    private static final int[] SECOND_LOOP_PROP_ARR = new int[]{0, 1, 2, 3, 4, 5};

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyHeightSearchType {
        CLOSEST,
        FLOOR_ELSE_CLOSEST,
        CEILING_ELSE_CLOSEST;
    }

    private static class MyCfdc implements InterfaceCanFontDisplayComputer {
        @Override
        public boolean canFontDisplay(InterfaceBwdFont font, int codePoint) {
            return false;
        }
    }

    /**
     * We only use font height from it.
     */
    private static class MyFontMetrics extends AbstractBwdFontMetrics {
        public MyFontMetrics(int fontHeight) {
            if (fontHeight <= 0) {
                throw new IllegalArgumentException("" + fontHeight);
            }
            final int backingAscent = fontHeight / 2;
            final int backingDescent = fontHeight - backingAscent;
            this.initialize_final(backingAscent, backingDescent);
        }
        @Override
        protected int computeCharWidth_noCache(int codePoint, String cpText) {
            return 0;
        }
        @Override
        protected int computeTextWidth_twoOrMoreCp(String text) {
            return 0;
        }
    }

    /**
     * Does nothing, just used to get font's disposed boolean
     * set on call to dispose().
     */
    private static class MyFdcl implements InterfaceFontDisposeCallListener {
        public MyFdcl() {
        }
        @Override
        public void onFontDisposeCalled(InterfaceBwdFont font) {
        }
    }

    private class MyFont extends AbstractBwdFont<Object> {
        private final InterfaceBwdFontMetrics metrics;
        public MyFont(
                BwdFontId fontId,
                int fontHeight) {
            super(
                    homeId,
                    fontId,
                    DEFAULT_CFDC,
                    new MyFdcl(),
                    new Object()); // backingFont
            this.metrics = new MyFontMetrics(fontHeight);
        }
        @Override
        public InterfaceBwdFontMetrics fontMetrics() {
            return this.metrics;
        }
    }

    private class MyFontCreator implements InterfaceFontCreator {
        final List<Integer> fontHeightByFontSize;
        public MyFontCreator(List<Integer> fontHeightByFontSize) {
            this.fontHeightByFontSize = fontHeightByFontSize;
        }
        @Override
        public MyFont newFontWithSize(
                BwdFontKind fontKind,
                int fontSize) {
            if ((fontSize < MIN_FONT_SIZE)
                    || (fontSize > MAX_FONT_SIZE)) {
                throw new IllegalArgumentException("" + fontSize);
            }
            final BwdFontId fontId = new BwdFontId(fontKind, fontSize);
            final int fontHeight = this.fontHeightByFontSize.get(fontSize);
            final MyFont font = new MyFont(fontId, fontHeight);
            return font;
        }
    }

    private class MyCountedFontCreator extends MyFontCreator {
        /**
         * To count created fonts, and check font disposal.
         * Best to clear it from time to time, to avoid leak.
         */
        final Set<MyFont> createdFontSet = new HashSet<MyFont>();
        public MyCountedFontCreator(List<Integer> fontHeightByFontSize) {
            super(fontHeightByFontSize);
        }
        @Override
        public MyFont newFontWithSize(
                BwdFontKind fontKind,
                int fontSize) {
            final MyFont font = super.newFontWithSize(fontKind, fontSize);
            this.createdFontSet.add(font);
            if (DEBUG) {
                System.out.println("created font (counted) : " + font);
            }
            return font;
        }
    }

    /**
     * Overriding to simplify the API.
     */
    private static class MyFontOfHeightHelper extends FontOfHeightHelper {
        public MyFontOfHeightHelper(
                int maxNbrOfFontCreations,
                double firstLoopFactor,
                int firstLoopMaxLinearTries,
                int secondLoopMaxLinearTries) {
            super(
                    maxNbrOfFontCreations,
                    firstLoopFactor,
                    firstLoopMaxLinearTries,
                    secondLoopMaxLinearTries);
        }
        public InterfaceBwdFont newFontForTargetHeight(
                InterfaceFontCreator fontCreator,
                int targetFontHeight,
                MyHeightSearchType heightSearchType) {
            if (heightSearchType == MyHeightSearchType.CLOSEST) {
                return this.newFontWithClosestHeight(
                        fontCreator,
                        MIN_FONT_SIZE,
                        MAX_FONT_SIZE,
                        FONT_KIND,
                        targetFontHeight);
            } else if (heightSearchType == MyHeightSearchType.FLOOR_ELSE_CLOSEST) {
                return this.newFontWithFloorElseClosestHeight(
                        fontCreator,
                        MIN_FONT_SIZE,
                        MAX_FONT_SIZE,
                        FONT_KIND,
                        targetFontHeight);
            } else if (heightSearchType == MyHeightSearchType.CEILING_ELSE_CLOSEST) {
                return this.newFontWithCeilingElseClosestHeight(
                        fontCreator,
                        MIN_FONT_SIZE,
                        MAX_FONT_SIZE,
                        FONT_KIND,
                        targetFontHeight);
            } else {
                throw new IllegalArgumentException("" + heightSearchType);
            }
        }
    }

    /**
     * Our reference brute force implementation.
     */
    private static class MyRefFontOfHeightHelper extends MyFontOfHeightHelper {
        public MyRefFontOfHeightHelper() {
            super(
                    1, // Unused.
                    2.0, // Unused.
                    0, // Unused.
                    0); // Unused.
        }
        @Override
        InterfaceBwdFont[] computeFloorCeilingFontArr(
                InterfaceFontCreator fontCreator,
                int minFontSize,
                int maxFontSize,
                BwdFontKind fontKind,
                int targetFontHeight) {

            final InterfaceBwdFont[] result = new InterfaceBwdFont[2];

            /*
             * 1) Computing one closest font (can be multiple ones, i.e. of different sizes).
             */

            InterfaceBwdFont closestFont = null;
            double closestDelta = Double.NaN;
            for (int fontSize = minFontSize; fontSize <= maxFontSize; fontSize++) {
                final InterfaceBwdFont newFont = fontCreator.newFontWithSize(fontKind, fontSize);
                if (closestFont == null) {
                    closestFont = newFont;
                    closestDelta = FontOfHeightHelper.relDeltaOfStrictPos(
                            closestFont.fontMetrics().fontHeight(),
                            targetFontHeight);
                } else {
                    final double newDelta = FontOfHeightHelper.relDeltaOfStrictPos(
                            newFont.fontMetrics().fontHeight(),
                            targetFontHeight);
                    if ((newDelta < closestDelta)
                            || ((newDelta == closestDelta)
                                    && (newFont.fontMetrics().fontHeight() > closestFont.fontMetrics().fontHeight()))) {
                        closestFont.dispose();
                        closestFont = newFont;
                        closestDelta = newDelta;
                    }
                }
            }

            /*
             * 2) Computing adjacent-height floor or ceiling font if any,
             *    and result.
             */

            final int closestS = closestFont.fontSize();
            final int closestH = closestFont.fontMetrics().fontHeight();

            if (closestH < targetFontHeight) {
                result[0] = closestFont;

                // Looking for ceiling font.
                InterfaceBwdFont ceilingFont = null;
                for (int tmpFontSize = closestS + 1; tmpFontSize <= MAX_FONT_SIZE; tmpFontSize++) {
                    final InterfaceBwdFont tmpFont = fontCreator.newFontWithSize(FONT_KIND, tmpFontSize);
                    if (tmpFont.fontMetrics().fontHeight() > targetFontHeight) {
                        ceilingFont = tmpFont;
                        break;
                    }
                }
                result[1] = ceilingFont;
            } else if (closestH > targetFontHeight) {
                // Looking for floor font.
                InterfaceBwdFont floorFont = null;
                for (int tmpFontSize = closestS - 1; tmpFontSize >= MIN_FONT_SIZE; tmpFontSize--) {
                    final InterfaceBwdFont tmpFont = fontCreator.newFontWithSize(FONT_KIND, tmpFontSize);
                    if (tmpFont.fontMetrics().fontHeight() < targetFontHeight) {
                        floorFont = tmpFont;
                        break;
                    }
                }
                result[0] = floorFont;

                result[1] = closestFont;
            } else {
                result[0] = closestFont;
            }

            return result;
        }
    }

    /*
     * 
     */

    private static class MyFontDescr {
        /**
         * An effectively immutable list containing random but (not strictly)
         * increasing font heights between indexes MIN_FONT_SIZE and MAX_FONT_SIZE.
         */
        final List<Integer> fontHeightByFontSize;
        public MyFontDescr(List<Integer> fontHeightByFontSize) {
            this.fontHeightByFontSize = fontHeightByFontSize;
        }
        @Override
        public String toString() {
            return this.fontHeightByFontSize.toString();
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final MyCfdc DEFAULT_CFDC = new MyCfdc();

    private static final BwdFontKind FONT_KIND = new BwdFontKind("myFamily");
    private static final BwdFontId FONT_ID_1 = new BwdFontId(FONT_KIND, MIN_FONT_SIZE);
    private static final BwdFontId FONT_ID_2 = new BwdFontId(FONT_KIND, MIN_FONT_SIZE + 1);

    private final int homeId = 1;

    /**
     * Fixed seed for determinism.
     */
    private final Random randomSeedGenerator = TestUtils.newRandom123456789L();

    /*
     * Stats.
     */

    private double meanNbrOfFontsCreatedPerCallOnLastTest = 0.0;
    private long maxNbrOfFontsCreatedPerCallOnLastTest = 0;

    private final TreeMap<Double,List<String>> configListByMeanNbrOfFontsCreatedPerCall =
            new TreeMap<Double,List<String>>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontOfHeightHelperTest() {
    }

    /*
     * One method per archetype of font.
     * 
     * Useful for analyzing and tuning algorithm
     * with a single archetype of input, which makes things simpler.
     */

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs0d7_mhd0() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(0.7, 0);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs0d7_mhd5() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(0.7, 5);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs0d7_mhd10() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(0.7, 10);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs0d7_mhdMAX() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(0.7, MAX_FONT_SIZE);
        this.printStatsIfConfigured();
    }

    /**
     * Always exact first try for this one (font height = font size).
     */
    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1_mhd0() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.0, 0);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1_mhd5() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.0, 5);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1_mhd10() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.0, 10);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1_mhdMAX() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.0, MAX_FONT_SIZE);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1d4_mhd0() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.4, 0);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1d4_mhd5() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.4, 5);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1d4_mhd10() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.4, 10);
        this.printStatsIfConfigured();
    }

    public void test_newFontWithXxxHeight_allAlgoConfigs_mfs1d4_mhdMAX() {
        this.test_newFontWithXxxHeight_allAlgoConfigs(1.4, MAX_FONT_SIZE);
        this.printStatsIfConfigured();
    }

    /*
     * Maxi mix.
     */

    public void test_newFontWithXxxHeight_allAlgoConfigs_allFontArchetypes() {
        if (DISABLE_ALL_FONTS_ARCHETYPES_TEST) {
            if (DEBUG) {
                System.out.println("all fonts archetypes test DISABLED");
            }
            return;
        }
        for (double meanFontSlope : MEAN_FONT_SLOPE_ARR) {
            for (int maxHeightDeltaFromRefHeight : MAX_HEIGHT_DELTA_FROM_REF_HEIGHT_ARR) {
                this.test_newFontWithXxxHeight_allAlgoConfigs(
                        meanFontSlope,
                        maxHeightDeltaFromRefHeight);
            }
        }
        this.printStatsIfConfigured();
    }

    /*
     * 
     */

    public void test_newFontWithXxxHeight_allAlgoConfigs(
            double meanFontSlope,
            int maxHeightDeltaFromRefHeight) {

        final Random random = this.newRandom();

        /*
         * Using the same fonts for all algorithm configurations, for fairness.
         */
        final List<MyFontDescr> fontDescrList = new ArrayList<MyFontDescr>();
        for (int k = 0; k < NBR_OF_RANDOM_FONTS; k++) {
            final MyFontDescr fontDescr = newFontHeightByFontSize(
                    random,
                    meanFontSlope,
                    maxHeightDeltaFromRefHeight);
            if (DEBUG) {
                System.out.println("fontDescr[" + k + "] = " + fontDescr);
            }
            fontDescrList.add(fontDescr);
        }

        for (double firstLoopFactor : FIRST_LOOP_FACTOR_ARR) {
            for (int firstLoopProp : FIRST_LOOP_PROP_ARR) {
                for (int secondLoopProp : SECOND_LOOP_PROP_ARR) {
                    for (MyHeightSearchType heightSearchType : MyHeightSearchType.values()) {
                        this.test_newFontWithXxxHeight(
                                meanFontSlope,
                                maxHeightDeltaFromRefHeight,
                                fontDescrList,
                                //
                                firstLoopFactor,
                                firstLoopProp,
                                secondLoopProp,
                                //
                                heightSearchType);

                        if (mustPrintTuningStats(heightSearchType)) {
                            final double meanNbrOfFontsCreatedPerCall = this.meanNbrOfFontsCreatedPerCallOnLastTest;
                            final long maxNbrOfFontsCreatedPerCall = this.maxNbrOfFontsCreatedPerCallOnLastTest;

                            System.out.println();

                            final String stats =
                                    "{(mfs = " + meanFontSlope + ", mfd = " + maxHeightDeltaFromRefHeight
                                    + "), (flf = " + firstLoopFactor + ", flp = " + firstLoopProp + ", slp = " + secondLoopProp
                                    + "), maxNbrCreated = " + maxNbrOfFontsCreatedPerCall + "}";
                            System.out.println(stats);

                            System.out.println("meanNbrOfFontsCreatedPerCall = " + meanNbrOfFontsCreatedPerCall);
                            System.out.println("maxNbrOfFontsCreatedPerCall = " + maxNbrOfFontsCreatedPerCall);

                            final TreeMap<Double,List<String>> map = this.configListByMeanNbrOfFontsCreatedPerCall;
                            final double key = meanNbrOfFontsCreatedPerCall;

                            List<String> list = map.get(key);
                            if (list == null) {
                                list = new ArrayList<String>();
                                map.put(key, list);
                            }
                            list.add(stats);
                        }
                    }
                }
            }
        }
    }

    public void test_newFontWithXxxHeight(
            double meanFontSlope,
            int maxHeightDeltaFromRefHeight,
            List<MyFontDescr> fontDescrList,
            //
            double firstLoopFactor,
            int firstLoopMaxLinearTries,
            int secondLoopMaxLinearTries,
            //
            MyHeightSearchType heightSearchType) {

        if (DEBUG) {
            System.out.println();
            System.out.println("meanFontSlope = " + meanFontSlope);
            System.out.println("fontDescrList = " + fontDescrList);
            System.out.println("heightSearchType = " + heightSearchType);
            System.out.println("firstLoopFactor = " + firstLoopFactor);
            System.out.println("firstLoopMaxLinearTries = " + firstLoopMaxLinearTries);
            System.out.println("secondLoopMaxLinearTries = " + secondLoopMaxLinearTries);
        }

        final Random random = newRandom();

        final MyFontOfHeightHelper helper = new MyFontOfHeightHelper(
                DEFAULT_MAX_NBR_OF_FONT_CREATIONS,
                firstLoopFactor,
                firstLoopMaxLinearTries,
                secondLoopMaxLinearTries);

        final MyRefFontOfHeightHelper refHelper = new MyRefFontOfHeightHelper();

        final double maxRefHeight = MIN_FONT_HEIGHT + meanFontSlope * (MAX_FONT_SIZE - MIN_FONT_SIZE);

        long totalNbrOfFontsCreated = 0;
        long maxNbrOfFontsCreatedPerCall = 0;
        for (MyFontDescr fontDescr : fontDescrList) {
            final MyFontCreator refFontCreator = new MyFontCreator(fontDescr.fontHeightByFontSize);
            final MyCountedFontCreator fontCreator = new MyCountedFontCreator(fontDescr.fontHeightByFontSize);

            for (int i = 0; i < NBR_OF_CALLS; i++) {
                final int targetFontHeight = MIN_FONT_HEIGHT + (int) Math.rint(random.nextDouble() * (maxRefHeight - MIN_FONT_HEIGHT));
                if (DEBUG) {
                    System.out.println();
                    System.out.println("targetFontHeight = " + targetFontHeight);
                }
                final InterfaceBwdFont refFont = refHelper.newFontForTargetHeight(
                        refFontCreator,
                        targetFontHeight,
                        heightSearchType);
                if (DEBUG) {
                    System.out.println("refFont = " + refFont);
                }

                fontCreator.createdFontSet.clear();
                final InterfaceBwdFont resFont = helper.newFontForTargetHeight(
                        fontCreator,
                        targetFontHeight,
                        heightSearchType);
                final long nbrOfFontsCreatedOnCall = fontCreator.createdFontSet.size();
                totalNbrOfFontsCreated += nbrOfFontsCreatedOnCall;
                maxNbrOfFontsCreatedPerCall = Math.max(maxNbrOfFontsCreatedPerCall, nbrOfFontsCreatedOnCall);
                if (DEBUG) {
                    System.out.println("resFont = " + resFont);
                }
                
                /*
                 * Checks.
                 */

                final boolean mustBeExactFirstTry =
                        (meanFontSlope == 1.0)
                        && (maxHeightDeltaFromRefHeight == 0);
                if (mustBeExactFirstTry
                        && (nbrOfFontsCreatedOnCall != 1)) {
                    final String str = "not exact first try, resFont = " + resFont;
                    System.out.println(str);
                    throw new AssertionError(str);
                }

                checkFontsDisposal(fontCreator, (MyFont) resFont);

                checkSameHeight(refFont, resFont);
            }
        }

        this.meanNbrOfFontsCreatedPerCallOnLastTest = (totalNbrOfFontsCreated / (NBR_OF_CALLS * (double) fontDescrList.size()));
        this.maxNbrOfFontsCreatedPerCallOnLastTest = maxNbrOfFontsCreatedPerCall;
    }
    
    /*
     * 
     */
    
    public void test_newFontWithXxxHeight_maxNbrOfFontsCreated_fontDisposal() {
        final Random random = newRandom();

        final int[] maxNbrOfFontCreationArr = new int[]{1, 2, 3, 4};
        
        // Kind of maximizing non-linearity.
        final double meanFontSlope = 0.1;
        final int maxHeightDeltaFromRefHeight = MAX_FONT_SIZE;
        
        final double maxRefHeight = MIN_FONT_HEIGHT + meanFontSlope * (MAX_FONT_SIZE - MIN_FONT_SIZE);

        for (int k = 0; k < NBR_OF_RANDOM_FONTS; k++) {
            final MyFontDescr fontDescr = newFontHeightByFontSize(
                    random,
                    meanFontSlope,
                    maxHeightDeltaFromRefHeight);
            
            final MyCountedFontCreator fontCreator = new MyCountedFontCreator(fontDescr.fontHeightByFontSize);
            
            for (int maxNbrOfFontCreation : maxNbrOfFontCreationArr) {
                
                final double firstLoopFactor = 2.0;
                final int firstLoopMaxLinearTries = 0;
                final int secondLoopMaxLinearTries = 0;
                
                final MyFontOfHeightHelper helper = new MyFontOfHeightHelper(
                        maxNbrOfFontCreation,
                        firstLoopFactor,
                        firstLoopMaxLinearTries,
                        secondLoopMaxLinearTries);

                for (int i = 0; i < NBR_OF_CALLS; i++) {
                    final int targetFontHeight = MIN_FONT_HEIGHT + (int) Math.rint(random.nextDouble() * (maxRefHeight - MIN_FONT_HEIGHT));
                    
                    fontCreator.createdFontSet.clear();
                    final InterfaceBwdFont resFont = helper.newFontForTargetHeight(
                            fontCreator,
                            targetFontHeight,
                            MyHeightSearchType.CLOSEST);
                    final long nbrOfFontsCreatedOnCall = fontCreator.createdFontSet.size();

                    /*
                     * Checks.
                     */
                    
                    if (nbrOfFontsCreatedOnCall > maxNbrOfFontCreation) {
                        throw new AssertionError(nbrOfFontsCreatedOnCall + " > " + maxNbrOfFontCreation);
                    }
                    
                    checkFontsDisposal(fontCreator, (MyFont) resFont);
                }
            }
        }
    }
    
    /*
     * 
     */

    public void test_computeClosestFontAndDisposeOther_int_2font() {
        {
            final int targetFontHeight = 100;
            final int h1 = 80;
            final int h2 = 121; // Closer in relative delta.
            final MyFont font1 = new MyFont(FONT_ID_1, h1);
            final MyFont font2 = new MyFont(FONT_ID_2, h2);
            final InterfaceBwdFont resFont =
                    FontOfHeightHelper.computeClosestFontAndDisposeOther(
                            targetFontHeight,
                            font1,
                            font2);
            assertEquals(font2, resFont);
            assertTrue(font1.isDisposed());
            assertFalse(font2.isDisposed());
        }
        {
            final int targetFontHeight = 100;
            final int h1 = 90; // Closer.
            final int h2 = 121;
            final MyFont font1 = new MyFont(FONT_ID_1, h1);
            final MyFont font2 = new MyFont(FONT_ID_2, h2);
            final InterfaceBwdFont resFont =
                    FontOfHeightHelper.computeClosestFontAndDisposeOther(
                            targetFontHeight,
                            font1,
                            font2);
            assertEquals(font1, resFont);
            assertFalse(font1.isDisposed());
            assertTrue(font2.isDisposed());
        }
    }
    
    public void test_computeClosestFont_int_2font() {
        {
            final int targetFontHeight = 100;
            final int h1 = 80;
            final int h2 = 121; // Closer in relative delta.
            final MyFont font1 = new MyFont(FONT_ID_1, h1);
            final MyFont font2 = new MyFont(FONT_ID_2, h2);
            final InterfaceBwdFont resFont =
                    FontOfHeightHelper.computeClosestFont(
                            targetFontHeight,
                            font1,
                            font2);
            assertEquals(font2, resFont);
            assertFalse(font1.isDisposed());
            assertFalse(font2.isDisposed());
        }
        {
            final int targetFontHeight = 100;
            final int h1 = 90; // Closer.
            final int h2 = 121;
            final MyFont font1 = new MyFont(FONT_ID_1, h1);
            final MyFont font2 = new MyFont(FONT_ID_2, h2);
            final InterfaceBwdFont resFont =
                    FontOfHeightHelper.computeClosestFont(
                            targetFontHeight,
                            font1,
                            font2);
            assertEquals(font1, resFont);
            assertFalse(font1.isDisposed());
            assertFalse(font2.isDisposed());
        }
    }

    public void test_relDeltaOfStrictPos_2int() {
        assertEquals(10.0 / 110.0, FontOfHeightHelper.relDeltaOfStrictPos(100, 110));
        assertEquals(9.0 / 10.0, FontOfHeightHelper.relDeltaOfStrictPos(10, 1));
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @param meanFontSlope Mean slope of fontHeight=f(fontSize).
     *        Allows to simulate fonts with heights much smaller or much larger
     *        than their sizes.
     * @return An immutable list containing random but (not strictly) increasing
     *         font heights between indexes MIN_FONT_SIZE and MAX_FONT_SIZE.
     */
    private static MyFontDescr newFontHeightByFontSize(
            Random random,
            double meanFontSlope,
            int maxHeightDeltaFromRefHeight) {
        final List<Integer> fontHeightByFontSize = new ArrayList<Integer>(MAX_FONT_SIZE + 1);
        for (int i = 0; i < MIN_FONT_SIZE; i++) {
            fontHeightByFontSize.add(null);
        }

        int tmpFontHeight = MIN_FONT_HEIGHT;
        for (int fontSize = MIN_FONT_SIZE; fontSize <= MAX_FONT_SIZE; fontSize++) {
            final int refHeight = (int) Math.rint(fontSize * meanFontSlope);

            // Delta above or below.
            final int deltaFromRefHeight = -maxHeightDeltaFromRefHeight + random.nextInt(2 * maxHeightDeltaFromRefHeight + 1);

            // Making sure height doesn't decrease.
            tmpFontHeight = Math.max(tmpFontHeight, refHeight + deltaFromRefHeight);

            fontHeightByFontSize.add(tmpFontHeight);
        }
        return new MyFontDescr(
                Collections.unmodifiableList(fontHeightByFontSize));
    }

    private static void checkSameHeight(InterfaceBwdFont refFont, InterfaceBwdFont resFont) {
        final int refH = refFont.fontMetrics().fontHeight();
        final int resH = resFont.fontMetrics().fontHeight();
        if (resH != refH) {
            throw new AssertionError(resH + " != " + refH);
        }
    }
    
    private static void checkFontsDisposal(
            MyCountedFontCreator fontCreator,
            MyFont resFont) {
        for (MyFont font : fontCreator.createdFontSet) {
            if (font == resFont) {
                assertFalse(font.isDisposed());
            } else {
                assertTrue(font.isDisposed());
            }
        }
    }

    private static boolean mustPrintTuningStats(MyHeightSearchType heightSearchType) {
        // Only need to print for one search type,
        // since all use the same internal algorithm.
        return MUST_PRINT_TUNING_STATS && (heightSearchType == MyHeightSearchType.CLOSEST);
    }

    private void printStatsIfConfigured() {
        if (MUST_PRINT_TUNING_STATS) {
            System.out.println();
            // Ordered by best to worse.
            for (Map.Entry<Double,List<String>> entry : this.configListByMeanNbrOfFontsCreatedPerCall.entrySet()) {
                final double meanNbrOfFontsCreatedPerCall = entry.getKey();
                final List<String> statsList = entry.getValue();
                System.out.println(meanNbrOfFontsCreatedPerCall + " : " + statsList);
            }
        }
    }

    /*
     * 
     */

    private Random newRandom() {
        return new Random(this.randomSeedGenerator.nextLong());
    }
}
