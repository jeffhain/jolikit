/*
 * Copyright 2020 Jeff Hain
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

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.utils.BwdTestUtils;

public class TextClippingBwdPainter {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final BwdColor BG_COLOR = BwdColor.BLACK;
    private static final BwdColor FG_COLOR = BwdColor.WHITE.withAlphaFp(0.5);

    private static final int TARGET_FONT_HEIGHT = 20;
    
    /**
     * For clip borders crossings.
     * Odd number of chars, to have a glyph on clip border.
     */
    private static final String TEXT_SHORT = "ABCDEFG";

    /**
     * To go through all the clip.
     */
    private static final String TEXT_LONG = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int CLIP_WIDTH = TARGET_FONT_HEIGHT * 13;
    private static final int CLIP_HEIGHT = TARGET_FONT_HEIGHT * 11;

    private static final int INITIAL_WIDTH = CLIP_WIDTH + TARGET_FONT_HEIGHT;
    private static final int INITIAL_HEIGHT = CLIP_HEIGHT + TARGET_FONT_HEIGHT;
    public static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBwdFont font;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public TextClippingBwdPainter(InterfaceBwdBinding binding) {
        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        this.font = fontHome.newFontWithClosestHeight(defaultFont.kind(), TARGET_FONT_HEIGHT);
    }
    
    public void paint(InterfaceBwdGraphics g) {

        final GRect box = g.getBox();

        g.setColor(BG_COLOR);
        g.fillRect(box);

        g.setColor(FG_COLOR);
        
        /*
         * Clip at (0,0), for its left and top borders to be on box border,
         * so that we have cases where partially drawn glyphs are also
         * partially out of box.
         */
        final GRect clipInBase = GRect.valueOf(0, 0, CLIP_WIDTH, CLIP_HEIGHT);
        g.addClipInBase(clipInBase);
        g.drawRect(clipInBase);
        
        g.setFont(this.font);
        
        /*
         * 
         */
        
        final InterfaceBwdFontMetrics metrics = this.font.metrics();
        final int textHeight = metrics.height();
        
        final int textWidthShort = metrics.computeTextWidth(TEXT_SHORT);
        final int textWidthShort_oneThird = (int) ((1/3.0) * textWidthShort);
        final int textWidthShort_twoThirds = (int) ((2/3.0) * textWidthShort);
        
        /*
         * Text entering clip.
         */
        
        for (GTransform transform : new GTransform[]{
                GTransform.valueOf(GRotation.ROT_0, 0, 0),
                GTransform.valueOf(GRotation.ROT_90, CLIP_WIDTH, 0),
                GTransform.valueOf(GRotation.ROT_180, CLIP_WIDTH, CLIP_HEIGHT),
                GTransform.valueOf(GRotation.ROT_270, 0, CLIP_HEIGHT),
        }) {
            g.addTransform(transform);
            g.drawText(-textWidthShort_twoThirds, textHeight, TEXT_SHORT);
            g.removeLastAddedTransform();
        }
        
        /*
         * Text exiting clip.
         */
        
        for (GTransform transform : new GTransform[]{
                GTransform.valueOf(GRotation.ROT_0, 0, 0),
                GTransform.valueOf(GRotation.ROT_90, CLIP_WIDTH, 0),
                GTransform.valueOf(GRotation.ROT_180, CLIP_WIDTH, CLIP_HEIGHT),
                GTransform.valueOf(GRotation.ROT_270, 0, CLIP_HEIGHT),
        }) {
            g.addTransform(transform);
            final int textX =
                    (transform.areHorVerFlipped() ? CLIP_HEIGHT : CLIP_WIDTH)
                    - textWidthShort_oneThird;
            g.drawText(textX, textHeight, TEXT_SHORT);
            g.removeLastAddedTransform();
        }
        
        /*
         * Text along clip borders.
         */
        
        for (GTransform transform : new GTransform[]{
                GTransform.valueOf(GRotation.ROT_0, 0, 0),
                GTransform.valueOf(GRotation.ROT_90, CLIP_WIDTH, 0),
                GTransform.valueOf(GRotation.ROT_180, CLIP_WIDTH, CLIP_HEIGHT),
                GTransform.valueOf(GRotation.ROT_270, 0, CLIP_HEIGHT),
        }) {
            g.addTransform(transform);
            final int textX =
                    ((transform.areHorVerFlipped() ? CLIP_HEIGHT : CLIP_WIDTH)
                            - textWidthShort) / 2;
            g.drawText(textX, -textHeight/2, TEXT_SHORT);
            g.removeLastAddedTransform();
        }
        
        /*
         * Text across clip.
         */
        
        for (GTransform transform : new GTransform[]{
                GTransform.valueOf(GRotation.ROT_0, 0, 0),
                GTransform.valueOf(GRotation.ROT_90, CLIP_WIDTH, 0),
        }) {
            g.addTransform(transform);
            final int centerX =
                    (transform.areHorVerFlipped() ? CLIP_HEIGHT : CLIP_WIDTH) / 2;
            final int centerY =
                    (transform.areHorVerFlipped() ? CLIP_WIDTH : CLIP_HEIGHT) / 2;
            BwdTestUtils.drawTextCentered(g, centerX, centerY, TEXT_LONG);
            g.removeLastAddedTransform();
        }
    }
}
