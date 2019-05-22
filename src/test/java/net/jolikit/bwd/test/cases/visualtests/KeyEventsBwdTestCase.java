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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;

/**
 * Shows key pressed/typed/released events.
 */
public class KeyEventsBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int INITIAL_WIDTH = 400;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final ArrayList<BwdEvent> lastEventList = new ArrayList<BwdEvent>();

    private int keyEventCount = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public KeyEventsBwdTestCase() {
    }

    public KeyEventsBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new KeyEventsBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new KeyEventsBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * 
     */
    
    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        if (DEBUG) {
            Dbg.log("");
            Dbg.log("onKeyPressed : " + event);
        }
        this.onKeyEvent(event);
    }

    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        if (DEBUG) {
            Dbg.log("onKeyTyped : " + event);
        }
        this.onKeyEvent(event);
    }

    @Override
    public void onKeyReleased(BwdKeyEventPr event) {
        if (DEBUG) {
            Dbg.log("onKeyReleased : " + event);
        }
        this.onKeyEvent(event);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final GRect box = g.getBoxInClient();
        
        final int x = box.x();
        final int y = box.y();
        final int ySpan = box.ySpan();

        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int lineHeight = metrics.fontHeight();
        
        /*
         * "Background".
         */
        
        {
            g.setColor(BwdColor.GREEN);
            g.fillRect(box);

            final String text = "Type some key...";
            g.setColor(BwdColor.BLACK);
            g.drawText(1, box.yMax() - (lineHeight + 1), text);
        }

        /*
         * 
         */
        
        // Removing first while too large for display.
        final int bottomSpareLines = 2;
        final int maxSize = Math.max(0, (ySpan / lineHeight) - bottomSpareLines);
        while (this.lastEventList.size() > maxSize) {
            this.lastEventList.remove(0);
        }
        
        /*
         * 
         */

        int tmpY = y;
        
        final int size = this.lastEventList.size();
        
        // Starts at 1.
        int tmpEventId = this.keyEventCount - size + 1;
        
        for (int i = 0; i < size; i++) {
            final BwdEvent event = this.lastEventList.get(i);
            
            final String eventStr = getEventStr(event, tmpEventId);

            g.setColor(BwdColor.BLACK);
            g.drawText(x, tmpY, eventStr);
            tmpY += lineHeight;
            tmpEventId++;
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void onKeyEvent(BwdEvent event) {
        this.keyEventCount++;
        
        // Inserting at last position.
        this.lastEventList.add(event);
        
        this.getHost().ensurePendingClientPainting();
    }
    
    private String getEventStr(BwdEvent event, int eventId) {
        String eventStr = null;
        
        if (event.getEventType() == BwdEventType.KEY_PRESSED) {
            final BwdKeyEventPr eventImpl = (BwdKeyEventPr) event;
            final String keyStr = BwdKeys.toString(eventImpl.getKey());
            final String locStr = BwdKeyLocations.toString(eventImpl.getKeyLocation());
            eventStr = eventId + ": PRESSED: (key = " + keyStr + ", loc = " + locStr + ")";
            
        } else if (event.getEventType() == BwdEventType.KEY_TYPED) {
            final BwdKeyEventT eventImpl = (BwdKeyEventT) event;
            final String cpStr = eventImpl.getUnicodeCharacter();
            final int cp = eventImpl.getCodePoint();
            final String isRepeatStr = (eventImpl.isRepeat() ? " (R)" : "");
            eventStr = eventId + ": TYPED: (cp = " + cp + ", char = " + cpStr + ")" + isRepeatStr;
            
        } else if (event.getEventType() == BwdEventType.KEY_RELEASED) {
            final BwdKeyEventPr eventImpl = (BwdKeyEventPr) event;
            final String keyStr = BwdKeys.toString(eventImpl.getKey());
            final String locStr = BwdKeyLocations.toString(eventImpl.getKeyLocation());
            eventStr = eventId + ": RELEASED: (key = " + keyStr + ", loc = " + locStr + ")";
        } else {
            throw new AssertionError("" + event);
        }
        
        return eventStr;
    }
}
