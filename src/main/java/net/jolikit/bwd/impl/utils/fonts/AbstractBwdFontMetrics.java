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

import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Optional class to make it easier to implement InterfaceBwdFontMetrics.
 */
public abstract class AbstractBwdFontMetrics implements InterfaceBwdFontMetrics {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int[] EMPTY_CACHE = new int[0];

    /**
     * Optimization to avoid garbage for common characters.
     * 
     * In this class we only use this cache for computeCharWidth(...),
     * but computeCharWidth_cacheIfInRange(...) is protected so that
     * extending classes can also use cache when implementing
     * computeTextWidth_twoOrMoreCp(..).
     */
    private int[] widthByCodePoint = EMPTY_CACHE;
    
    private int ascent;
    private int descent;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractBwdFontMetrics() {
    }
    
    @Override
    public String toString() {
        return "[h = "
                + this.height()
                + ", a = "
                + this.ascent()
                + ", d = "
                + this.descent()
                + "]";
    }
    
    @Override
    public int ascent() {
        return this.ascent;
    }
    
    @Override
    public int descent() {
        return this.descent;
    }
    
    @Override
    public int height() {
        return this.ascent + this.descent;
    }
    
    @Override
    public int computeCharWidth(int codePoint) {
        BwdUnicode.checkCodePoint(codePoint);
        
        return this.computeCharWidth_cacheIfInRange(codePoint, null);
    }
    
    @Override
    public int computeTextWidth(String text) {
        if (text.length() <= 2) {
            if (text.length() == 0) {
                return 0;
            }
            final int cp1 = text.codePointAt(0);
            final int cp1Size = Character.charCount(cp1);
            final boolean onlyOneCp = (cp1Size == text.length());
            if (onlyOneCp) {
                return this.computeCharWidth_cacheIfInRange(cp1, text);
            }
        }
        return this.computeTextWidth_twoOrMoreCp(text);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Must not use char cache width.
     * 
     * @param codePoint A valid code point (must be checked before call).
     * @param cpText The string corresponding to the specified code point,
     *        not to uselessly recompute it. Can be null.
     * @return The char width to return from computeCharWidth(...).
     */
    protected abstract int computeCharWidth_noCache(int codePoint, String cpText);
    
    /**
     * @param text Some text, containing at least two code points.
     * @return The text width to return from computeTextWidth(...).
     */
    protected abstract int computeTextWidth_twoOrMoreCp(String text);
    
    /*
     * 
     */
    
    /**
     * Uses char cache width if the specified code point is in cache range.
     * 
     * Not private, for implementations of computeTextWidth_twoOrMoreCp(...)
     * that want to compute text width as a sum of char width, to be able
     * to use char width cache.
     * 
     * @param codePoint A valid code point (must be checked before call).
     * @param cpText The string corresponding to the specified code point,
     *        not to uselessly recompute it. Can be null.
     * @return The char width to return from computeCharWidth(...).
     */
    protected int computeCharWidth_cacheIfInRange(int codePoint, String cpText) {
        final int[] widthByCodePoint = this.widthByCodePoint;
        if (codePoint < widthByCodePoint.length) {
            return widthByCodePoint[codePoint];
        }
        return this.computeCharWidth_noCache(codePoint, cpText);
    }

    /**
     * Useful for implementing computeCharWidth_noCache(...)
     * by delegating to string based treatments.
     * 
     * @param codePoint A valid code point (must be checked before call).
     * @param cpText The string corresponding to the specified code point,
     *        not to uselessly recompute it. Can be null.
     * @return The specified string if it(s not null, else the string
     *         corresponding to the specified code point.
     */
    protected static String stringOfCp(int codePoint, String cpText) {
        if (cpText == null) {
            cpText = LangUtils.stringOfCodePoint(codePoint);
        }
        return cpText;
    }

    /*
     * 
     */
    
    /**
     * Must be called in constructor.
     * 
     * Backing descent sign can be either positive, or negative
     * (such as for AWT or SDL), internally we use a positive value,
     * to keep things simple, since there is no gain from complexifying
     * the notion of descent with a sense (Occam's razor).
     * 
     * @param backingAscent Positive or negative (using absolute value), in pixels.
     * @param backingDescent Positive or negative (using absolute value), in pixels.
     */
    protected final void initialize_final(
            double backingAscent,
            double backingDescent) {
        
        if (DEBUG) {
            Dbg.log("backingAscent = " + backingAscent);
            Dbg.log("backingDescent = " + backingDescent);
        }
        
        /*
         * Example of issue with descent:
         * With Qt 4.8, font of "Batang" family, of size 1, has -1 descent
         * (looks like a rounding or near-zero issue,
         * because for higher sizes it gets positive).
         * 
         * Also, we ensure consistency, i.e. that
         * fontHeight = ascent + descent
         * because often libraries don't respect this equality.
         */
        
        /*
         * NaN: not accepting.
         */
        
        if (Double.isNaN(backingAscent)
                || Double.isNaN(backingDescent)) {
            throw new IllegalArgumentException("NaN");
        }
        
        /*
         * Using absolutes always, rather than 0 if wrong sign, for:
         * - In case of sign error due to approximation errors,
         *   the magnitude of the error should be small (in absolute).
         * - In case of unexpected sign due to the backing library
         *   doing a sign error, we would still get the proper value.
         * Ex.:
         * - SWT: family = Cambria Math, size = 7: backing ascent = -10, backing descent = 17.
         *   (in this case, using absolute value makes more appropriate bounding boxes,
         *   even though glyphs still tend to leak outside)
         * - AWT: family = Microsoft Himalaya, size = 21: backing ascent = 9, backing descent = 7 (positive!).
         */
        backingAscent = Math.abs(backingAscent);
        backingDescent = Math.abs(backingDescent);

        /*
         * Font height must be at least 1 (in case backing ascent and descent are zero).
         * 
         * Not ceiling or rounding ascent and descent separately,
         * to have only one fp-to-int approximation.
         * 
         * We also ensure that the largest of ascent or descent
         * ends up being the largest as integer too.
         * If both are zero, our algorithm is designed to ensure
         * that ascent is 1, for it's usually larger than descent.
         */

        final double backingFontHeight = backingAscent + backingDescent;
        // Ceiling to make sure all the (theoretically drawn) pixels are covered.
        // Must be at least 1.
        final int fontHeight = Math.max(1, BindingCoordsUtils.ceilToInt(backingFontHeight));
        
        final double baOverBhRatio;
        if (backingFontHeight == 0.0) {
            baOverBhRatio = 1.0;
        } else {
            baOverBhRatio = (backingAscent / backingFontHeight);
        }
        final int ascent = BindingCoordsUtils.roundToInt(fontHeight * baOverBhRatio);
        final int descent = fontHeight - ascent;
        
        this.ascent = ascent;
        this.descent = descent;

        if (DEBUG) {
            Dbg.log("ascent = " + ascent);
            Dbg.log("descent = " + descent);
            Dbg.log("fontHeight = " + this.height());
        }
    }
    
    /**
     * @param cacheSize Size of the cache for char width. Must be >= 0.
     *        If called, must be called (last preferably) in constructor,
     *        and will use computeCharWidth_noCache(...) to init
     *        char width cache for code points in [0,widthCacheSize[.
     */
    protected final void initCharWidthCache_final(int cacheSize) {
        if (cacheSize == 0) {
            return;
        }
        this.widthByCodePoint = new int[cacheSize];
        for (int cp = 0; cp < cacheSize; cp++) {
            this.widthByCodePoint[cp] = this.computeCharWidth_noCache(cp, null);
        }
    }
}
