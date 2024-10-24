/*
 * Copyright 2019-2024 Jeff Hain
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
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

/**
 * To test that window and client bounds and coordinates,
 * and drawings in client coordinates,
 * and mouse coordinates in client,
 * are consistent, without client scaling.
 */
public class HostCoordsScale1BwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TARGET_FONT_HEIGHT = 13;
    
    /**
     * NB: Can be set to true to check that normally identical fonts created
     * after some screen resolution change, still draw text of same size.
     */
    private static final boolean MUST_CREATE_FONT_AT_EACH_PAINTING = false;

    /**
     * Large enough to see window drag area by default, even on Widows 10.
     */
    private static final int INITIAL_WIDTH = 400;
    private static final int INITIAL_HEIGHT = 7 * (TARGET_FONT_HEIGHT + 2);
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font = null;
    
    private GPoint lastMouseMovePosInScreen = GPoint.ZERO;
    private GPoint lastMouseMovePosInClient = GPoint.ZERO;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HostCoordsScale1BwdTestCase() {
    }

    public HostCoordsScale1BwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostCoordsScale1BwdTestCase(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * 
     */
    
    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        super.onMouseMoved(event);
        
        this.lastMouseMovePosInScreen = event.posInScreen();
        this.lastMouseMovePosInClient = event.posInClient();
        
        // To get displayed move pos updated.
        this.getHost().makeAllDirtyAndEnsurePendingClientPainting();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final GRect box = g.getBox();

        final int x = box.x();
        final int y = box.y();
        
        /*
         * 
         */
        
        final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
        
        final GRect screenBounds = this.getBinding().getScreenBounds();
        final GRect windowBounds = this.getHost().getWindowBounds();
        final GRect clientBounds = this.getHost().getClientBounds();
        
        final GPoint bindingMousePosInScreen = this.getBinding().getMousePosInScreen();
        final GPoint lastMouseMovePosInScreen = this.lastMouseMovePosInScreen;
        final GPoint lastMouseMovePosInClient = this.lastMouseMovePosInClient;
        
        /*
         * 
         */

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        /*
         * Drawing lines, to help figure out client positioning
         * even if no text is drawn (as on Mac with Allegro5).
         */

        // Blue rectangle: just outside the box: must not be visible.
        g.setColor(BwdColor.BLUE);
        g.drawRect(box.withBordersDeltas(-1, -1, 1, 1));

        // Red rectangle: the box: must be visible.
        g.setColor(BwdColor.RED);
        g.drawRect(box);
        
        g.setColor(BwdColor.GREEN);
        g.drawLine(box.xMid(), y, box.xMax(), box.yMid());
        g.drawLine(box.xMax(), box.yMid(), box.xMid(), box.yMax());
        g.drawLine(box.xMid(), box.yMax(), x, box.yMid());
        g.drawLine(x, box.yMid(), box.xMid(), y);

        /*
         * 
         */
        
        if (MUST_CREATE_FONT_AT_EACH_PAINTING) {
            if (this.font != null) {
                this.font.dispose();
                this.font = null;
            }
        }
        
        InterfaceBwdFont font = this.font;
        if (font == null) {
            font = fontHome.newFontWithClosestHeight(TARGET_FONT_HEIGHT);
            this.font = font;
        }

        g.setFont(font);
        final int lineHeight = font.metrics().height() + 1;
        
        // +2 to be 1-pixel blank line away from red box. 
        int textX = x + 2;
        int textY = y + 2;
        g.setColor(BwdColor.BLACK);
        
        g.drawText(textX, textY, "screen = " + screenBounds);
        textY += lineHeight;

        g.drawText(textX, textY, "window = " + windowBounds);
        textY += lineHeight;

        g.drawText(textX, textY, "client = " + clientBounds);
        textY += lineHeight;

        g.drawText(textX, textY, "mouse pos in screen = " + bindingMousePosInScreen);
        textY += lineHeight;

        g.drawText(textX, textY, "move pos in screen = " + lastMouseMovePosInScreen);
        textY += lineHeight;

        g.drawText(textX, textY, "move pos in client = " + lastMouseMovePosInClient);
        textY += lineHeight;
        
        return null;
    }
}
