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
package net.jolikit.bwd.impl.swt;

import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontMetrics;
import net.jolikit.lang.LangUtils;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;

public class SwtBwdFontMetrics extends AbstractBwdFontMetrics {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int WIDTH_CACHE_SIZE = 256;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final SwtFontMetricsHelper helper;
    
    private final Font backingFont;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SwtBwdFontMetrics(
            FontMetrics backingMetrics,
            SwtFontMetricsHelper helper,
            Font backingFont) {
        
        if (false) {
            // If we ever want these.
            final int backingLeading = backingMetrics.getLeading();
            final int backingLineHeight = backingMetrics.getHeight();
        }

        this.initialize_final(
                backingMetrics.getAscent(),
                backingMetrics.getDescent());

        this.helper = LangUtils.requireNonNull(helper);
        
        this.backingFont = LangUtils.requireNonNull(backingFont);

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
    
    private int backingTextWidth(String text) {
        return this.helper.computeTextWidth(this.backingFont, text);
    }
}
