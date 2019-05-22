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
package net.jolikit.bwd.test.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.events.InterfaceBwdEventListener;
import net.jolikit.bwd.impl.utils.HostStateUtils;
import net.jolikit.lang.Dbg;

public class BwdClientEventListenerMock implements InterfaceBwdEventListener {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SPAM = DEBUG && false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private boolean mustStoreEvents = false;
    
    private boolean mustStoreErrors = false;

    private boolean mustCallOnEventError = false;
    
    /*
     * Stored errors.
     */
    
    private final List<String> errorList = new ArrayList<String>();

    /*
     * Stored events.
     */
    
    private final List<BwdEvent> eventList = new ArrayList<BwdEvent>();
    
    private final Map<BwdEventType,List<BwdEvent>> eventListByType = new TreeMap<BwdEventType,List<BwdEvent>>();
    {
        for (BwdEventType eventType : BwdEventType.values()) {
            this.eventListByType.put(eventType, new ArrayList<BwdEvent>());
        }
    }
    
    private final List<BwdEvent> showingStateEventList = new ArrayList<BwdEvent>();
    private final List<BwdEvent> focusedStateEventList = new ArrayList<BwdEvent>();
    private final List<BwdEvent> iconifiedStateEventList = new ArrayList<BwdEvent>();
    private final List<BwdEvent> maximizedStateEventList = new ArrayList<BwdEvent>();
    
    /*
     * Client state according to received events.
     */
    
    private boolean clientStateShowing = false;
    private boolean clientStateFocused = false;
    private boolean clientStateIconified = false;
    private boolean clientStateMaximized = false;
    private boolean clientStateClosed = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BwdClientEventListenerMock() {
    }

    /**
     * False by default.
     */
    public void setMustStoreEvents(boolean mustStoreEvents) {
        this.mustStoreEvents = mustStoreEvents;
    }
    
    /**
     * False by default.
     */
    public void setMustStoreErrors(boolean mustStoreErrors) {
        this.mustStoreErrors = mustStoreErrors;
    }
    
    /**
     * False by default.
     */
    public void setMustCallOnEventError(boolean mustCallOnEventError) {
        this.mustCallOnEventError = mustCallOnEventError;
    }
    
    /*
     * Client state.
     */
    
    public boolean getClientStateShowing() {
        return this.clientStateShowing;
    }
    
    public boolean getClientStateFocused() {
        return this.clientStateFocused;
    }
    
    public boolean getClientStateIconified() {
        return this.clientStateIconified;
    }
    
    public boolean getClientStateMaximized() {
        return this.clientStateMaximized;
    }
    
    public boolean getClientStateClosed() {
        return this.clientStateClosed;
    }

    /*
     * Stored errors.
     */
    
    public List<String> getErrorList() {
        return this.errorList;
    }
    
    /*
     * Stored events.
     */
    
    public void clearStoredEvents() {
        this.eventList.clear();
        
        for (List<BwdEvent> eventListOfType : this.eventListByType.values()) {
            eventListOfType.clear();
        }
        
        this.showingStateEventList.clear();
        this.focusedStateEventList.clear();
        this.iconifiedStateEventList.clear();
        this.maximizedStateEventList.clear();
    }
    
    /**
     * @return The internal list.
     */
    public List<BwdEvent> getEventList() {
        return this.eventList;
    }
    
    /**
     * @return The internal map.
     */
    public Map<BwdEventType,List<BwdEvent>> getEventListByType() {
        return this.eventListByType;
    }
    
    /**
     * Convenience method.
     * 
     * @return The number of received events for the specified type.
     */
    public int getEventCountForType(BwdEventType eventType) {
        return this.eventListByType.get(eventType).size();
    }
    
    /**
     * @return The internal list.
     */
    public List<BwdEvent> getShowingStateEventList() {
        return this.showingStateEventList;
    }
    
    /**
     * @return The internal list.
     */
    public List<BwdEvent> getFocusedStateEventList() {
        return this.focusedStateEventList;
    }
    
    /**
     * @return The internal list.
     */
    public List<BwdEvent> getIconifiedStateEventList() {
        return this.iconifiedStateEventList;
    }
    
    /**
     * @return The internal list.
     */
    public List<BwdEvent> getMaximizedStateEventList() {
        return this.maximizedStateEventList;
    }
    
    /*
     * Window events.
     */
    
    @Override
    public void onWindowShown(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_SHOWN, event);
    }
    
    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_HIDDEN, event);
    }
    
    @Override
    public void onWindowFocusGained(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_FOCUS_GAINED, event);
    }
    
    @Override
    public void onWindowFocusLost(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_FOCUS_LOST, event);
    }
    
    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_ICONIFIED, event);
    }
    
    @Override
    public void onWindowDeiconified(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_DEICONIFIED, event);
    }
    
    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_MAXIMIZED, event);
    }
    
    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_DEMAXIMIZED, event);
    }

    @Override
    public void onWindowMoved(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_MOVED, event);
    }
    
    @Override
    public void onWindowResized(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_RESIZED, event);
    }

    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        this.onAnyEvent(BwdEventType.WINDOW_CLOSED, event);
    }

    /*
     * Key events.
     */

    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        this.onAnyEvent(BwdEventType.KEY_PRESSED, event);
    }

    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        this.onAnyEvent(BwdEventType.KEY_TYPED, event);
    }

    @Override
    public void onKeyReleased(BwdKeyEventPr event) {
        this.onAnyEvent(BwdEventType.KEY_RELEASED, event);
    }

    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        this.onAnyEvent(BwdEventType.MOUSE_PRESSED, event);
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        this.onAnyEvent(BwdEventType.MOUSE_RELEASED, event);
    }

    @Override
    public void onMouseClicked(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        this.onAnyEvent(BwdEventType.MOUSE_ENTERED_CLIENT, event);
    }

    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        this.onAnyEvent(BwdEventType.MOUSE_EXITED_CLIENT, event);
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        this.onAnyEvent(BwdEventType.MOUSE_MOVED, event);
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        this.onAnyEvent(BwdEventType.MOUSE_DRAGGED, event);
    }

    /*
     * Wheel events.
     */

    @Override
    public void onWheelRolled(BwdWheelEvent event) {
        this.onAnyEvent(BwdEventType.WHEEL_ROLLED, event);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected void onAnyEvent(BwdEventType expectedEventType, BwdEvent event) {
        if (DEBUG) {
            final boolean isSpam = HostStateUtils.isSpamEventType(expectedEventType);
            if ((!isSpam) || DEBUG_SPAM) {
                Dbg.log(cid(), "onAnyEvent(" + expectedEventType + ", " + event + ")");
            }
        }
        
        final BwdEventType eventType = event.getEventType();
        
        if (eventType != expectedEventType) {
            throw new IllegalArgumentException(eventType + " != " + expectedEventType);
        }
        
        /*
         * 
         */
        
        if (this.mustStoreEvents) {
            this.eventList.add(event);
            
            final List<BwdEvent> eventListOfType = this.eventListByType.get(eventType);
            eventListOfType.add(event);
            
            switch (eventType) {
            case WINDOW_SHOWN:
            case WINDOW_HIDDEN: {
                this.showingStateEventList.add(event);
            } break;
            case WINDOW_FOCUS_GAINED:
            case WINDOW_FOCUS_LOST: {
                this.focusedStateEventList.add(event);
            } break;
            case WINDOW_ICONIFIED:
            case WINDOW_DEICONIFIED: {
                this.iconifiedStateEventList.add(event);
            } break;
            case WINDOW_MAXIMIZED:
            case WINDOW_DEMAXIMIZED: {
                this.maximizedStateEventList.add(event);
            } break;
            default:
                break;
            }
        }
        
        /*
         * 
         */
        
        try {
            this.checkClientStateConsistencyIfNeeded(event);
        } finally {
            
            /*
             * Updating event state.
             */
            
            switch (eventType) {
            case WINDOW_SHOWN:
            case WINDOW_HIDDEN: {
                this.clientStateShowing = (eventType == BwdEventType.WINDOW_SHOWN);
            } break;
            case WINDOW_FOCUS_GAINED:
            case WINDOW_FOCUS_LOST: {
                this.clientStateFocused = (eventType == BwdEventType.WINDOW_FOCUS_GAINED);
            } break;
            case WINDOW_ICONIFIED:
            case WINDOW_DEICONIFIED: {
                this.clientStateIconified = (eventType == BwdEventType.WINDOW_ICONIFIED);
            } break;
            case WINDOW_MAXIMIZED:
            case WINDOW_DEMAXIMIZED: {
                this.clientStateMaximized = (eventType == BwdEventType.WINDOW_MAXIMIZED);
            } break;
            case WINDOW_CLOSED: {
                this.clientStateClosed = true;
            } break;
            default:
                break;
            }
        }
    }
    
    /**
     * This default implementation throws IllegalStateException.
     */
    protected void onEventError(String errorMsg) {
        throw new IllegalStateException(errorMsg);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * To help identity client in logs.
     */
    private Object cid() {
        return this.hashCode();
    }
    
    /**
     * Must be called BEFORE event state update.
     */
    private void checkClientStateConsistencyIfNeeded(BwdEvent event) {
        if (this.mustStoreErrors || this.mustCallOnEventError) {
            final boolean compatible = HostStateUtils.isEventTypeCompatibleWithClientState(
                    this.clientStateShowing,
                    this.clientStateFocused,
                    this.clientStateIconified,
                    this.clientStateMaximized,
                    this.clientStateClosed,
                    //
                    event.getEventType());
            if (!compatible) {
                final String stateStr = HostStateUtils.toStringHostState(
                        this.clientStateShowing,
                        this.clientStateFocused,
                        this.clientStateIconified,
                        this.clientStateMaximized,
                        this.clientStateClosed);
                final String errorMsg = "state is " + stateStr + ", and got event : " + event;
                if (this.mustStoreErrors) {
                    this.errorList.add(errorMsg);
                }
                if (this.mustCallOnEventError) {
                    this.onEventError(errorMsg);
                }
            }
        }
    }
}
