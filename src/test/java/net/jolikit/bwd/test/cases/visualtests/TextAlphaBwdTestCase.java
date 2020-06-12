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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test alpha blending of text with alpha.
 */
public class TextAlphaBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TEXT_RGB_32 = 0xFFFFFF;
    
    private static final int ALPHA_COUNT = 10;
    private static final int TARGET_FONT_HEIGHT = 20;
    private static final int LINE_HEIGHT = TARGET_FONT_HEIGHT + 1;
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = ALPHA_COUNT * LINE_HEIGHT;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public TextAlphaBwdTestCase() {
    }
    
    public TextAlphaBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new TextAlphaBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new TextAlphaBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceBwdFontHome fontHome = binding.getFontHome();

        if (this.font == null) {
            final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
            this.font = fontHome.newFontWithClosestHeight(defaultFont.fontKind(), TARGET_FONT_HEIGHT);
        }

        final GRect box = g.getBox();
        final int x = box.x();
        final int y = box.y();

        g.setArgb32(0xFF4080C0);
        g.fillRect(box);
        
        g.setFont(this.font);
        for (int i = 0; i < ALPHA_COUNT; i++) {
            // From 0 to 1.
            final double alphaFp = i / (double) (ALPHA_COUNT - 1);
            
            final String text = "alphaFp = " + alphaFp;
            final int argb32 = Argb32.withAlphaFp(TEXT_RGB_32, alphaFp);
            g.setArgb32(argb32);
            g.drawText(
                    x,
                    y + i * LINE_HEIGHT,
                    text);
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
