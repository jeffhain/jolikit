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
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test that window and client bounds and coordinates,
 * and drawings in client coordinates,
 * and mouse coordinates in client,
 * are consistent.
 */
public class HostCoordsRegularBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TARGET_FONT_HEIGHT = 13;
    
    /**
     * NB: Can be set to true to check that normally identical fonts created
     * after some screen resolution change, still draw text of same size.
     */
    private static final boolean MUST_CREATE_FONT_AT_EACH_PAINTING = false;

    private static final int INITIAL_WIDTH = 300;
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

    public HostCoordsRegularBwdTestCase() {
    }

    public HostCoordsRegularBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostCoordsRegularBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new HostCoordsRegularBwdTestCase(this.getBinding());
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
        
        this.lastMouseMovePosInScreen =
                GPoint.valueOf(
                        event.xInScreen(),
                        event.yInScreen());
        this.lastMouseMovePosInClient =
                GPoint.valueOf(
                        event.xInClient(),
                        event.yInClient());
        
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
            font = fontHome.newFontWithClosestHeight(
                    fontHome.getDefaultFont().kind(),
                    TARGET_FONT_HEIGHT);
            this.font = font;
        }

        g.setFont(font);
        final int lineHeight = font.metrics().height() + 1;
        
        int tmpY = y;
        g.setColor(BwdColor.BLACK);
        
        g.drawText(x, tmpY, "screen = " + screenBounds);
        tmpY += lineHeight;

        g.drawText(x, tmpY, "window = " + windowBounds);
        tmpY += lineHeight;

        g.drawText(x, tmpY, "client = " + clientBounds);
        tmpY += lineHeight;

        g.drawText(x, tmpY, "mouse pos in screen = " + bindingMousePosInScreen);
        tmpY += lineHeight;

        g.drawText(x, tmpY, "move pos in screen = " + lastMouseMovePosInScreen);
        tmpY += lineHeight;

        g.drawText(x, tmpY, "move pos in client = " + lastMouseMovePosInClient);
        tmpY += lineHeight;
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
