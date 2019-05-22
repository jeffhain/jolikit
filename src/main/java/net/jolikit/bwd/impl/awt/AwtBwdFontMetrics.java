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
package net.jolikit.bwd.impl.awt;

import java.awt.Font;
import java.awt.FontMetrics;

import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontMetrics;

public class AwtBwdFontMetrics extends AbstractBwdFontMetrics {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * TODO awt Caching is especially useful because FontMetrics.charWidth(int)
     * seems to create an array of 256 ints whenever it gets called with
     * a code point < 256, even though AWT might use an extending implementation
     * that doesn't.
     */
    private static final int WIDTH_CACHE_SIZE = 256;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final FontMetrics backingMetrics;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AwtBwdFontMetrics(
            FontMetrics backingMetrics,
            Font backingFont) {
        
        if (false) {
            // If we ever want these.
            final int backingLineHeight = backingMetrics.getHeight();
        }

        this.initialize_final(
                backingMetrics.getMaxAscent(),
                backingMetrics.getMaxDescent());
        
        if (false) {
            // Not used metrics:
            int leading = backingMetrics.getLeading();
        }
        
        this.backingMetrics = backingMetrics;
        
        /*
         * 
         */
        
        this.initCharWidthCache_final(WIDTH_CACHE_SIZE);
    }
    
    public FontMetrics getBackingMetrics() {
        return this.backingMetrics;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int computeCharWidth_noCache(int codePoint, String cpText) {
        return this.backingCharWidth(codePoint);
    }
    
    @Override
    protected int computeTextWidth_twoOrMoreCp(String text) {
        return this.backingTextWidth(text);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /*
     * TODO awt AWT code point width might be negative,
     * such as for U+09E3 (BENGALI VOWEL SIGN VOCALIC LL).
     * This might be a bug, or not, since Unicode and fonts stuffs
     * are such a mess already.
     * In our world, we consider that it is a bug,
     * and make sure the result is >= 0.
     */
    
    /**
     * @param codePoint Must be valid.
     */
    private int backingCharWidth(int codePoint) {
        final int width = this.backingMetrics.charWidth(codePoint);
        return Math.max(0, width);
    }
    
    private int backingTextWidth(String text) {
        final int width = this.backingMetrics.stringWidth(text);
        return Math.max(0, width);
    }
}
