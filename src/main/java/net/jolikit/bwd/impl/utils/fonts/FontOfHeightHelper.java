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
package net.jolikit.bwd.impl.utils.fonts;

import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;

/**
 * Class to implement font homes newFontWithXxxHeight(...) methods.
 */
public class FontOfHeightHelper {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /*
     * NB: Could have these values in configuration,
     * but we don't want to messify configuration with this.
     */
    
    /**
     * Also, doesn't hurt if is a bit large, for most of the time reaching
     * the target only takes a few iterations, and even if for some bindings
     * font creation entails font file reading, it shouldn't hurt to read
     * a font file repeatedly due to usual system level caching.
     * 
     * Though, if its overly large, when target height is larger than max
     * possible height that a font can get, the algorithm can waste time
     * try fonts up to max allowed font size for the binding (in case
     * that could help font height increase), so it's better not to have
     * it much bigger than needed.
     */
    private static final int DEFAULT_MAX_NBR_OF_FONT_CREATIONS = 20;
    
    private static final double DEFAULT_FIRST_LOOP_FACTOR = 1.5;
    
    private static final int DEFAULT_FIRST_LOOP_MAX_LINEAR_TRIES = 3;
    
    private static final int DEFAULT_SECOND_LOOP_MAX_LINEAR_TRIES = 3;
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    public interface InterfaceFontCreator {
        public InterfaceBwdFont newFontWithSize(BwdFontKind fontKind, int fontSize);
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final int maxNbrOfFontCreations;
    
    private final double firstLoopFactor;
    private final int firstLoopMaxLinearTries;
    private final int secondLoopMaxLinearTries;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses a default configuration.
     */
    public FontOfHeightHelper() {
        this(
                DEFAULT_MAX_NBR_OF_FONT_CREATIONS,
                DEFAULT_FIRST_LOOP_FACTOR,
                DEFAULT_FIRST_LOOP_MAX_LINEAR_TRIES,
                DEFAULT_SECOND_LOOP_MAX_LINEAR_TRIES);
    }

    /**
     * @param maxNbrOfFontCreations Must be >= 1.
     * @param firstLoopFactor Must be > 0.
     * @param firstLoopMaxLinearTries Must be >= 0.
     * @param secondLoopMaxLinearTries Must be >= 0.
     */
    public FontOfHeightHelper(
            int maxNbrOfFontCreations,
            double firstLoopFactor,
            int firstLoopMaxLinearTries,
            int secondLoopMaxLinearTries) {
        if (maxNbrOfFontCreations <= 0) {
            throw new IllegalArgumentException("" + maxNbrOfFontCreations);
        }
        if (firstLoopFactor <= 0.0) {
            throw new IllegalArgumentException("" + firstLoopFactor);
        }
        if (firstLoopMaxLinearTries < 0) {
            throw new IllegalArgumentException("" + firstLoopMaxLinearTries);
        }
        if (secondLoopMaxLinearTries < 0) {
            throw new IllegalArgumentException("" + secondLoopMaxLinearTries);
        }
        this.maxNbrOfFontCreations = maxNbrOfFontCreations;
        this.firstLoopFactor = firstLoopFactor;
        this.firstLoopMaxLinearTries = firstLoopMaxLinearTries;
        this.secondLoopMaxLinearTries = secondLoopMaxLinearTries;
    }
    
    public InterfaceBwdFont newFontWithClosestHeight(
            InterfaceFontCreator fontCreator,
            int minFontSize,
            int maxFontSize,
            BwdFontKind fontKind,
            int targetFontHeight) {
        
        final InterfaceBwdFont[] floorCeilingArr = this.computeFloorCeilingFontArr(
                fontCreator,
                minFontSize,
                maxFontSize,
                fontKind,
                targetFontHeight);
        final InterfaceBwdFont floorFont = floorCeilingArr[0];
        final InterfaceBwdFont ceilingFont = floorCeilingArr[1];
        
        final InterfaceBwdFont bestFont = computeClosestFontAndDisposeOther(
                targetFontHeight,
                floorFont,
                ceilingFont);
        
        logResultFont(bestFont, targetFontHeight);
        return bestFont;
    }

    public InterfaceBwdFont newFontWithFloorElseClosestHeight(
            InterfaceFontCreator fontCreator,
            int minFontSize,
            int maxFontSize,
            BwdFontKind fontKind,
            int targetFontHeight) {
        
        final InterfaceBwdFont[] floorCeilingArr = this.computeFloorCeilingFontArr(
                fontCreator,
                minFontSize,
                maxFontSize,
                fontKind,
                targetFontHeight);
        
        final InterfaceBwdFont floorFont = floorCeilingArr[0];
        final InterfaceBwdFont ceilingFont = floorCeilingArr[1];
        
        final InterfaceBwdFont bestFont;
        if (floorFont != null) {
            if (ceilingFont != null) {
                ceilingFont.dispose();
            }
            bestFont = floorFont;
        } else {
            bestFont = ceilingFont;
        }
        
        logResultFont(bestFont, targetFontHeight);
        return bestFont;
    }

    public InterfaceBwdFont newFontWithCeilingElseClosestHeight(
            InterfaceFontCreator fontCreator,
            int minFontSize,
            int maxFontSize,
            BwdFontKind fontKind,
            int targetFontHeight) {
        
        final InterfaceBwdFont[] floorCeilingArr = this.computeFloorCeilingFontArr(
                fontCreator,
                minFontSize,
                maxFontSize,
                fontKind,
                targetFontHeight);
        
        final InterfaceBwdFont floorFont = floorCeilingArr[0];
        final InterfaceBwdFont ceilingFont = floorCeilingArr[1];
        
        final InterfaceBwdFont bestFont;
        if (ceilingFont != null) {
            if (floorFont != null) {
                floorFont.dispose();
            }
            bestFont = ceilingFont;
        } else {
            bestFont = floorFont;
        }
        
        logResultFont(bestFont, targetFontHeight);
        return bestFont;
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /*
     * Methods visible/overridable for tests.
     */
    
    /**
     * If the target height is met, the font can be put in any slot,
     * but the remaining slot must be null.
     * 
     * @return {floorFont,ceilingFont}, any two can be null but not both.
     */
    InterfaceBwdFont[] computeFloorCeilingFontArr(
            InterfaceFontCreator fontCreator,
            int minFontSize,
            int maxFontSize,
            BwdFontKind fontKind,
            int targetFontHeight) {
        
        if (targetFontHeight <= 0) {
            throw new IllegalArgumentException("targetFontHeight [" + targetFontHeight + "] must be > 0");
        }

        if (DEBUG) {
            Dbg.log();
            Dbg.log("computeFloorCeilingFontArr(" + fontKind + "," + targetFontHeight + ")");
        }
        
        final InterfaceBwdFont[] result = new InterfaceBwdFont[2];

        /*
         * There is sometimes not a linear relationship between a font size
         * and its height, so our algorithm typically uses a phased approach,
         * where at first we assume linearity, which allows to quickly get
         * near the optimal font size for linear fonts, and then just use
         * a factor of 2 to multiply or divide previously used font size
         * if we didn't enclose the target with two fonts yet, or use the
         * middle between enclosing fonts sizes once we did.
         * 
         * Algorithm in short:
         * Configuration parameters:
         * - Max number of fonts created per call (>= 1).
         * - Factor for non-linear computations in first loop.
         * - Max number of linear computations for part 2).
         * - Max number of linear computations for part 3).
         * Invariants:
         * - Whenever the target is reached, or we are sure we can't get closer,
         *   we stop (obviously!).
         * 1) First guess is done using target font height as font size
         *    (taken back into allowed font size range).
         * 2) We try to get closer to best font size by computing a better
         *    font size from the current closest font we have,
         *    first with linear approach and then with multiplications (if below)
         *    or divisions (if above) with some factor.
         *    While we do that, if we ever cross the target font height and
         *    end up with a font below and one above, we go to the next step.
         * 3) We try to get closer to best font size by computing a better
         *    font size from the current enclosing fonts we have,
         *    first with linear approach and then by taking the middle size.
         */

        InterfaceBwdFont onlyFont = null;
        
        // Second loop fonts.
        InterfaceBwdFont belowFont = null;
        InterfaceBwdFont aboveFont = null;
        
        final int maxNbrOfFontCreations = this.maxNbrOfFontCreations;
        final int maxLinearTriesForLoop1 = this.firstLoopMaxLinearTries;
        final int maxLinearTriesForLoop2 = this.secondLoopMaxLinearTries;
        final double factorForLoop1 = this.firstLoopFactor;
        
        int nbrOfFontsCreated = 0;
        
        /*
         * 1) First guess.
         */
        
        {
            final int onlyS = NumbersUtils.toRange(minFontSize, maxFontSize, targetFontHeight);
            onlyFont = fontCreator.newFontWithSize(fontKind, onlyS);
            nbrOfFontsCreated++;
            if (DEBUG) {
                Dbg.log("onlyFont (initial) = " + onlyFont);
            }
            final int onlyH = onlyFont.fontMetrics().fontHeight();
            if ((onlyH == targetFontHeight)
                    || (nbrOfFontsCreated == maxNbrOfFontCreations)) {
                logResultFont(onlyFont, targetFontHeight);
                result[0] = onlyFont;
                return result;
            }
            final boolean onlyIsBelowElseAbove = (onlyH < targetFontHeight);
            final boolean boundHit = (onlyIsBelowElseAbove ? (onlyS == maxFontSize) : (onlyS == minFontSize));
            if (boundHit) {
                if (DEBUG) {
                    Dbg.log("bound hit");
                }
                logResultFont(onlyFont, targetFontHeight);
                if (onlyIsBelowElseAbove) {
                    result[0] = onlyFont;
                } else {
                    result[1] = onlyFont;
                }
                return result;
            }
        }

        /*
         * 2) First loop.
         */
        
        {
            int loopNbr = 0;
            // This loop's result is either bestFont, or (belowFont,aboveFont) couple
            // in which case bestFont must be set to null.
            while (nbrOfFontsCreated < maxNbrOfFontCreations) {
                loopNbr++;
                final int onlyS = onlyFont.fontSize();
                final int onlyH = onlyFont.fontMetrics().fontHeight();
                if (DEBUG) {
                    Dbg.log();
                    Dbg.log("loop1 : " + loopNbr);
                    Dbg.log("nbrOfFontsCreated = " + nbrOfFontsCreated);
                    Dbg.log("onlyFont size = " + onlyS);
                    Dbg.log("onlyFont height = " + onlyH);
                }
                
                /*
                 * Computing new font size.
                 */

                int tmpNewFontSize = -1;
                if (loopNbr <= maxLinearTriesForLoop1) {
                    final double targetHeightOverHeightRatio = targetFontHeight / (double) onlyH;
                    if (DEBUG) {
                        Dbg.log("targetHeightOverHeightRatio (linear) = " + targetHeightOverHeightRatio);
                    }
                    tmpNewFontSize = BindingCoordsUtils.roundToInt(onlyS * targetHeightOverHeightRatio);
                    if (DEBUG) {
                        Dbg.log("tmpNewFontSize (from one) = " + tmpNewFontSize);
                    }
                } else {
                    // Will hopefully get both a below and an above font next.
                    tmpNewFontSize = (int) (onlyS * (onlyH < targetFontHeight ? factorForLoop1 : 1.0/factorForLoop1));
                    if (DEBUG) {
                        Dbg.log("tmpNewFontSize (factor) = " + tmpNewFontSize);
                    }
                }
                final boolean onlyIsBelowElseAbove = (onlyH < targetFontHeight);
                if (tmpNewFontSize == onlyS) {
                    tmpNewFontSize = NumbersUtils.plusBounded(tmpNewFontSize, (onlyIsBelowElseAbove ? 1 : -1));
                    if (DEBUG) {
                        Dbg.log("tmpNewFontSize (+- 1) = " + tmpNewFontSize);
                    }
                }
                
                tmpNewFontSize = NumbersUtils.toRange(minFontSize, maxFontSize, tmpNewFontSize);
                
                /*
                 * Creating new font, and checks
                 * whether we are done or keep trying.
                 */
                
                final InterfaceBwdFont newFont = fontCreator.newFontWithSize(fontKind, tmpNewFontSize);
                nbrOfFontsCreated++;
                if (DEBUG) {
                    Dbg.log("newFont = " + newFont);
                }
                final int newS = newFont.fontSize();
                final int newH = newFont.fontMetrics().fontHeight();
                if (newH == targetFontHeight) {
                    // Bingo.
                    onlyFont.dispose();
                    onlyFont = newFont;
                    break;
                }
                final boolean newIsBelowElseAbove = (newH < targetFontHeight);
                final boolean boundHit = (newIsBelowElseAbove ? (newS == maxFontSize) : (newS == minFontSize));
                if (boundHit) {
                    // Can't get closer.
                    if (DEBUG) {
                        Dbg.log("bound hit");
                    }
                    onlyFont.dispose();
                    onlyFont = newFont;
                    break;
                }

                final boolean craziness = (onlyIsBelowElseAbove ? (newH < onlyH) : (newH > onlyH));
                if (craziness) {
                    // Sticking to current font.
                    if (DEBUG) {
                        Dbg.log("craziness");
                    }
                    newFont.dispose();
                    break;
                }
                
                final boolean crossed = (newIsBelowElseAbove != onlyIsBelowElseAbove);
                if (crossed) {
                    if (newIsBelowElseAbove) {
                        belowFont = newFont;
                        aboveFont = onlyFont;
                    } else {
                        belowFont = onlyFont;
                        aboveFont = newFont;
                    }
                    onlyFont = null;
                    break;
                }
                
                // New font might not have a closer height,
                // but it's size is different. Will try more.
                onlyFont.dispose();
                onlyFont = newFont;
            }
        }
        
        /*
         * 3) Second loop.
         */

        if (onlyFont != null) {
            // Could not find an (aboveFont,belowFont) couple.
            final int bestH = onlyFont.fontMetrics().fontHeight();
            final boolean bestIsBelowElseAbove = (bestH < targetFontHeight);
            if (bestIsBelowElseAbove) {
                result[0] = onlyFont;
            } else {
                result[1] = onlyFont;
            }
        } else {
            // Means above and below fonts are non-null.
            
            int loopNbr = 0;
            // This loop must not use onlyFont (null).
            // This loop's result is the (belowFont,aboveFont) couple.
            while (nbrOfFontsCreated < maxNbrOfFontCreations) {
                loopNbr++;
                if (DEBUG) {
                    final int belowS = belowFont.fontSize();
                    final int belowH = belowFont.fontMetrics().fontHeight();
                    final int aboveS = aboveFont.fontSize();
                    final int aboveH = aboveFont.fontMetrics().fontHeight();
                    Dbg.log();
                    Dbg.log("loop2 : " + loopNbr);
                    Dbg.log("nbrOfFontsCreated = " + nbrOfFontsCreated);
                    Dbg.log("belowFont size = " + belowS);
                    Dbg.log("belowFont height = " + belowH);
                    Dbg.log("aboveFont size = " + aboveS);
                    Dbg.log("aboveFont height = " + aboveH);
                }
                
                /*
                 * Computing new font size.
                 */
                
                int tmpNewFontSize = -1;
                {
                    final int belowS = belowFont.fontSize();
                    final int belowH = belowFont.fontMetrics().fontHeight();
                    final int aboveS = aboveFont.fontSize();
                    final int aboveH = aboveFont.fontMetrics().fontHeight();
                    if (loopNbr <= maxLinearTriesForLoop2) {
                        final double ratio = (targetFontHeight - belowH) / (double) (aboveH - belowH);
                        tmpNewFontSize = BindingCoordsUtils.roundToInt(belowS + ratio * (aboveS - belowS));
                        if (DEBUG) {
                            Dbg.log("tmpNewFontSize (from both) (linear) = " + tmpNewFontSize);
                        }
                        if ((tmpNewFontSize == belowS)
                                || (tmpNewFontSize == aboveS)) {
                            tmpNewFontSize = NumbersUtils.plusBounded(tmpNewFontSize, ((tmpNewFontSize == belowS) ? 1 : -1));
                            if (DEBUG) {
                                Dbg.log("tmpNewFontSize (+- 1) = " + tmpNewFontSize + " for " + fontKind + " (aboveS - belowS = " + (aboveS - belowS) + ")");
                            }
                        }
                    } else {
                        tmpNewFontSize = BindingCoordsUtils.roundToInt(belowS + (aboveS - belowS) * 0.5);
                        if (DEBUG) {
                            Dbg.log("tmpNewFontSize (bisect) = " + tmpNewFontSize + " for " + fontKind + " (aboveS - belowS = " + (aboveS - belowS) + ")");
                        }
                    }
                }

                /*
                 * Creating new font, and checks
                 * whether we are done or keep trying.
                 */
                
                final InterfaceBwdFont newFont = fontCreator.newFontWithSize(fontKind, tmpNewFontSize);
                nbrOfFontsCreated++;
                if (DEBUG) {
                    Dbg.log("newFont = " + newFont);
                }
                final int newH = newFont.fontMetrics().fontHeight();
                if (newH < targetFontHeight) {
                    if (newH < belowFont.fontMetrics().fontHeight()) {
                        // Craziness.
                        if (DEBUG) {
                            Dbg.log("craziness (below)");
                        }
                        newFont.dispose();
                        break;
                    }
                    // Replacing below font with new font.
                    belowFont.dispose();
                    belowFont = newFont;
                } else if (newH > targetFontHeight) {
                    if (newH > aboveFont.fontMetrics().fontHeight()) {
                        // Craziness.
                        if (DEBUG) {
                            Dbg.log("craziness (above)");
                        }
                        newFont.dispose();
                        break;
                    }
                    // Replacing above font with new font.
                    aboveFont.dispose();
                    aboveFont = newFont;
                } else {
                    // Bingo.
                    belowFont.dispose();
                    aboveFont.dispose();
                    belowFont = newFont;
                    aboveFont = null;
                    break;
                }
                
                /*
                 * Checking what we can do with new (below,above) couple.
                 */
                
                final int belowS = belowFont.fontSize();
                final int aboveS = aboveFont.fontSize();
                if (aboveS - belowS == 1) {
                    // Can't try anything more.
                    if (DEBUG) {
                        Dbg.log("contiguous");
                    }
                    break;
                }
            }
            
            result[0] = belowFont;
            result[1] = aboveFont;
        }
        
        return result;
    }
    
    /**
     * Any font can be null.
     */
    static InterfaceBwdFont computeClosestFontAndDisposeOther(
            int targetFontHeight,
            InterfaceBwdFont font1,
            InterfaceBwdFont font2) {

        final InterfaceBwdFont bestFont = computeClosestFont(targetFontHeight, font1, font2);

        if (bestFont == font1) {
            if (font2 != null) {
                font2.dispose();
            }
        } else {
            if (font1 != null) {
                font1.dispose();
            }
        }

        return bestFont;
    }

    /**
     * Any font can be null.
     */
    static InterfaceBwdFont computeClosestFont(
            int targetFontHeight,
            InterfaceBwdFont font1,
            InterfaceBwdFont font2) {

        if (DEBUG) {
            Dbg.log("computeBestFont(...)");
            Dbg.log("targetFontHeight = " + targetFontHeight);
            Dbg.log("font1 = " + font1);
            Dbg.log("font2 = " + font2);
        }

        final InterfaceBwdFont bestFont;
        if (font1 == null) {
            bestFont = font2;
        } else if (font2 == null) {
            bestFont = font1;
        } else {
            final int h1 = font1.fontMetrics().fontHeight();
            final int h2 = font2.fontMetrics().fontHeight();

            final double relDelta1 = relDeltaOfStrictPos(h1, targetFontHeight);
            final double relDelta2 = relDeltaOfStrictPos(h2, targetFontHeight);
            if (DEBUG) {
                Dbg.log("relDelta1 = " + relDelta1);
                Dbg.log("relDelta2 = " + relDelta2);
            }

            final boolean font2IsFurther = (relDelta2 > relDelta1);

            if (font2IsFurther) {
                // Giving up if iterating gets us further.
                if (DEBUG) {
                    Dbg.log("font2 is further");
                }
                bestFont = font1;
            } else {
                final boolean font2IsAsFar = (relDelta2 == relDelta1);
                if (font2IsAsFar) {
                    if (DEBUG) {
                        Dbg.log("font2 is as far");
                    }
                    // If both as far, we prefer the largest one, for readability.
                    if (h2 > h1) {
                        bestFont = font2;
                    } else {
                        bestFont = font1;
                    }
                } else {
                    bestFont = font2;
                }
            }
        }

        if (DEBUG) {
            Dbg.log("bestFont = " + bestFont);
        }

        return bestFont;
    }
    
    /**
     * @param a Must be > 0.
     * @param b Must be > 0.
     * @return |a-b|/max(|a|,|b|)
     */
    static double relDeltaOfStrictPos(int a, int b) {
        if (a <= 0) {
            throw new IllegalArgumentException("" + a);
        }
        if (b <= 0) {
            throw new IllegalArgumentException("" + b);
        }
        final int numerator = Math.abs(a - b);
        final int denominator = Math.max(a, b);
        return numerator / (double) denominator;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For debug. Does nothing if not DEBUG.
     */
    private void logResultFont(InterfaceBwdFont resultFont, int targetFontHeight) {
        if (DEBUG) {
            final int bestFontHeight = resultFont.fontMetrics().fontHeight();
            Dbg.log("targetFontHeight = " + targetFontHeight);
            if (bestFontHeight == targetFontHeight) {
                Dbg.log("exact height");
            } else {
                Dbg.log("not exact height");
            }
        }
    }
}
