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

import java.util.ArrayList;
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
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

/**
 * Shows all events.
 */
public class EventsBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 400;
    private static final GPoint INITIAL_CLIENT_SPANS =
        GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final long TEN_DIGITS_ENSURER = 1000L * 1000L * 1000L;
    
    private int nextEventId = 1;
    
    private final ArrayList<BwdEvent> newEventList = new ArrayList<BwdEvent>();
    private final ArrayList<Long> newEventTimeList = new ArrayList<Long>();

    /**
     * Only drawing the text for each new event once,
     * each on one image.
     * Doing that because some libraries (such as Allegro5)
     * have utterly slow text drawing, which could slow
     * down or block everything including events firings.
     */
    private List<InterfaceBwdWritableImage> imgList =
        new ArrayList<InterfaceBwdWritableImage>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public EventsBwdTestCase() {
    }

    public EventsBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        // Else can't see that event.
        this.setCloseOnMiddleClickActivated(false);
        // Else can't see what happens when dragging mouse exits client.
        this.setDragOnLeftClickActivated(false);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new EventsBwdTestCase(binding);
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
        
        this.newEventList.add(event);
        this.newEventTimeList.add(System.nanoTime());
        
        this.getHost().ensurePendingClientPainting();
    }

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final GRect box = g.getBox();
        
        final int x = box.x();
        final int y = box.y();
        final int ySpan = box.ySpan();

        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.metrics();
        final int lineHeight = metrics.height();
        
        /*
         * "Background".
         */

        g.setColor(BwdColor.GREEN);
        g.fillRect(box);
        
        /*
         * Adding image for each new event.
         */

        final int newEventCount = this.newEventList.size();
        for (int i = 0; i < newEventCount; i++) {
            final BwdEvent event = this.newEventList.get(i);
            final long timeNs = this.newEventTimeList.get(i);
            
            final int eventId = this.nextEventId++;
            final String eventStr = getEventStr(event, timeNs, eventId);

            final InterfaceBwdWritableImage img =
                getBinding().newWritableImage(box.xSpan(), lineHeight);
            img.getGraphics().drawText(0, 0, eventStr);
            this.imgList.add(img);
        }
        this.newEventList.clear();
        this.newEventTimeList.clear();
        
        /*
         * Removing images surplus.
         */
        
        final int bottomSpareLines = 2;
        final int maxShownEventCount = Math.max(0, (ySpan / lineHeight) - bottomSpareLines);
        final int imgSurplus = (this.imgList.size() - maxShownEventCount);
        if (imgSurplus > 0) {
            final int imgCount = this.imgList.size();
            final List<InterfaceBwdWritableImage> newImgList =
                new ArrayList<InterfaceBwdWritableImage>();
            for (int i = 0; i < imgCount; i++) {
                final InterfaceBwdWritableImage img = this.imgList.get(i);
                if (i < imgSurplus) {
                    img.dispose();
                } else {
                    newImgList.add(img);
                }
            }
            this.imgList.clear();
            this.imgList.addAll(newImgList);
        }
        
        /*
         * Drawing images.
         */
        
        int tmpY = y;
        final int imgCount = this.imgList.size();
        for (int i = 0; i < imgCount; i++) {
            final InterfaceBwdWritableImage img = this.imgList.get(i);

            g.drawImage(x, tmpY, img);
            
            tmpY += lineHeight;
        }
        
        /*
         * 
         */
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private String getEventStr(
        BwdEvent event,
        long timeNs,
        int eventId) {
        
        timeNs += TEN_DIGITS_ENSURER;
        
        final String timeNsStr = Long.toString(timeNs);
        String timeModuloS =
            timeNsStr.substring(timeNsStr.length() - 10, timeNsStr.length() - 3);
        timeModuloS = timeModuloS.substring(0, 1) + "." + timeModuloS.substring(1);
        
        final String eventStr = "(" + timeModuloS + ")" + eventId + " : " + event;
        return eventStr;
    }
}
