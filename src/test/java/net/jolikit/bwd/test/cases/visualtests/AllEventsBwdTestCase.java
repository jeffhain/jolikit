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

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * Shows all events.
 */
public class AllEventsBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int INITIAL_WIDTH = 900;
    private static final int INITIAL_HEIGHT = 400;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AllEventsBwdTestCase() {
    }

    public AllEventsBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        
        this.setMustStoreEvents(true);
        
        this.setCloseOnMiddleClickActivated(false);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new AllEventsBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new AllEventsBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void onAnyEvent(BwdEventType expectedEventType, BwdEvent event) {
        super.onAnyEvent(expectedEventType, event);
        
        this.getHost().ensurePendingClientPainting();
    }

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

        g.setColor(BwdColor.GREEN);
        g.fillRect(box);

        /*
         * 
         */
        
        // Removing first while too large for display.
        final int bottomSpareLines = 2;
        final int maxShownEventCount = Math.max(0, (ySpan / lineHeight) - bottomSpareLines);
        
        /*
         * 
         */

        int tmpY = y;
        
        final List<BwdEvent> eventList = this.getEventList();
        final int eventCount = eventList.size();
        
        final int shownEventCount = Math.min(eventCount, maxShownEventCount);
        
        int eventIndex = eventCount - shownEventCount;
        
        for (int i = 0; i < shownEventCount; i++) {
            final BwdEvent event = eventList.get(eventIndex);
            
            final int eventId = eventIndex + 1;
            final String eventStr = getEventStr(event, eventId);

            g.setColor(BwdColor.BLACK);
            g.drawText(x, tmpY, eventStr);
            tmpY += lineHeight;
            eventIndex++;
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private String getEventStr(BwdEvent event, int eventId) {
        final String eventStr = eventId + " : " + event;
        return eventStr;
    }
}
