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
package net.jolikit.bwd.impl.awt;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.fonts.InterfaceCanFontDisplayComputer;
import net.jolikit.bwd.impl.utils.fonts.InterfaceFontDisposeCallListener;

public class AwtBwdFont extends AbstractBwdFont<Font> implements Comparable<AwtBwdFont> {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Should not hurt, since we don't use distorted graphics.
     */
    private static final Graphics2D G2D_FOR_METRICS;
    static {
        // Don't need alpha for metrics.
        final BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        G2D_FOR_METRICS = bi.createGraphics();
    }

    private final AwtBwdFontMetrics metrics;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AwtBwdFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            Font backingFont) {
        super(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                backingFont);
        
        final FontMetrics backingMetrics = G2D_FOR_METRICS.getFontMetrics(backingFont);
        this.metrics = new AwtBwdFontMetrics(
                backingMetrics,
                backingFont);
    }
    
    @Override
    public int compareTo(AwtBwdFont other) {
        return this.compareToImpl(other);
    }

    /*
     * 
     */

    @Override
    public InterfaceBwdFontMetrics fontMetrics() {
        return this.metrics;
    }
}
