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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To see the default font, and variations of it with various sizes:
 * their info, and text drawn with them.
 */
public class DefaultFontBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 20;
    private static final int NBR_OF_FONT_SIZE = MAX_FONT_SIZE - MIN_FONT_SIZE + 1;
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DefaultFontBwdTestCase() {
    }

    public DefaultFontBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new DefaultFontBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new DefaultFontBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect box = g.getBox();

        final int yMin = box.y();
        
        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        
        /*
         * 
         */
        
        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        /*
         * 
         */
        
        final int x = 0;
        int tmpY = yMin;
        
        final InterfaceBwdFont defaultFont = g.getFont();
        
        for (int i = 0; i < NBR_OF_FONT_SIZE; i++) {
            final int fontSize = MIN_FONT_SIZE + i;
            final boolean isDefaultFontSize = (fontSize == defaultFont.size());
            
            final InterfaceBwdFont font = fontHome.newFontWithSize(fontSize);
            try {
                final InterfaceBwdFontMetrics metrics = font.metrics();
                final int fontHeight = metrics.height();
                
                final String text = "" + font;
                
                final int fillArgb32;
                if (isDefaultFontSize) {
                    fillArgb32 = 0xFFC0C0C0;
                } else {
                    fillArgb32 = 0xFF808080;
                }
                g.setArgb32(fillArgb32);
                g.fillRect(x, tmpY, metrics.computeTextWidth(text), fontHeight);
                
                g.setColor(BwdColor.BLACK);
                g.setFont(font);
                g.drawText(x, tmpY, text);
                tmpY += fontHeight + 1;
                
            } finally {
                font.dispose();
            }
        }
        
        return null;
    }
}
