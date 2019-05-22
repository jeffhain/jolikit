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
package net.jolikit.bwd.impl.jfx;

import javafx.geometry.Bounds;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontMetrics;

import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;

public class JfxBwdFontMetrics extends AbstractBwdFontMetrics {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int WIDTH_CACHE_SIZE = 256;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Null if not using internal API.
     */
    private final FontMetrics backingMetrics;
    
    private final Font backingFont;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public JfxBwdFontMetrics(
            Font font,
            boolean mustUseInternalApiForFontMetrics) {

        final FontMetrics backingMetrics;
        final double backingAscent;
        final double backingDescent;
        if (mustUseInternalApiForFontMetrics) {
            backingMetrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(font);
            
            backingAscent = backingMetrics.getMaxAscent();
            backingDescent = backingMetrics.getMaxDescent();
        } else {
            backingMetrics = null;
            
            // TODO jfx For non-monospaced romanish fonts,
            // "W" should be about the largest character
            // (if we ever hit its advance somewhere).
            final Bounds textBounds =
                    textBounds_noBackingMetrics(
                            font,
                            "W");
            final double fontHeight = textBounds.getHeight();

            // TODO jfx Inventing a baseline.
            final double baselineRatio = 0.8;
            
            backingAscent = fontHeight * baselineRatio;
            backingDescent = fontHeight - backingAscent;
        }

        this.initialize_final(
                backingAscent,
                backingDescent);
        
        this.backingMetrics = backingMetrics;
        this.backingFont = font;
        
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
        if (this.backingMetrics != null) {
            final float widthFp = this.backingMetrics.computeStringWidth(text);
            return BindingCoordsUtils.ceilToInt(widthFp);
        } else {
            final Bounds textBounds =
                    textBounds_noBackingMetrics(
                            this.backingFont,
                            text);
            return BindingCoordsUtils.ceilToInt(textBounds.getWidth());
        }
    }
    
    private static Bounds textBounds_noBackingMetrics(Font font, String text) {
        // TODO jfx A hammer for a fly?
        final Text jfxText = new Text(text);
        jfxText.setFont(font);
        return jfxText.getLayoutBounds();
    }
}
