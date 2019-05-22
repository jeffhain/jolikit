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
package net.jolikit.bwd.impl.qtj4;

import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontMetrics;

import com.trolltech.qt.gui.QFontMetrics;

public class QtjBwdFontMetrics extends AbstractBwdFontMetrics {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int WIDTH_CACHE_SIZE = 256;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final QFontMetrics backingMetrics;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjBwdFontMetrics(QFontMetrics backingMetrics) {
        
        if (false) {
            // If we ever want these.
            final int backingFontHeight = backingMetrics.height();
            final int backingLeading = backingMetrics.leading();
            final int backingLineHeight = backingMetrics.lineSpacing();
        }

        this.initialize_final(
                backingMetrics.ascent(),
                backingMetrics.descent());

        this.backingMetrics = backingMetrics;
        
        /*
         * 
         */
        
        this.initCharWidthCache_final(WIDTH_CACHE_SIZE);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int computeCharWidth_noCache(int codePoint, String cpText) {
        cpText = stringOfCp(codePoint, cpText);
        return this.backingTextWidth(cpText);
    }
    
    @Override
    protected int computeTextWidth_twoOrMoreCp(String text) {
        return this.backingTextWidth(text);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /*
     * TODO qtj We don't use QFontMetrics.width(char),
     * because it's inconsistent with QFontMetrics.width(String).
     * For example, it can return non-zero values for control characters,
     * and different values for non-control characters.
     */
    
    private int backingTextWidth(String text) {
        /*
         * TODO qtj Can be negative, for example -1 for 0xFFFC,
         * so using max(0,_).
         */
        return Math.max(0, this.backingMetrics.width(text));
    }
}
