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
package net.jolikit.bwd.api.events;

import java.util.EventListener;

/**
 * Interface to listen to BWD events.
 * 
 * @see #BwdEventType for general events description.
 * 
 * Not providing a general onAnyEvent(BwdEvent) method,
 * only event-specific methods, for:
 * - Event-specific methods, despite the induced copy/paste when wanting
 *   to apply a same logic to all event types, are easier to work with than
 *   a single onAnyEvent(...) method, in particular when reading and searching
 *   the code to figure out what is done on which event, so we have these.
 * - Having also an onAnyEvent(BwdEvent) method would cause redundancy
 *   and confusion.
 * - Whoever prefers to use an onAnyEvent(BwdEvent) method can still make
 *   its own, and call it from the specific methods.
 * 
 * Extending EventListener, in case that could help (doesn't cost much,
 * and we already refused for events to extend EventObject).
 */
public interface InterfaceBwdEventListener extends EventListener {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Window events.
     */
    
    /**
     * Precondition: (!showing) && (!closed).
     */
    public void onWindowShown(BwdWindowEvent event);
    
    /**
     * Precondition: showing, which implies (!closed).
     */
    public void onWindowHidden(BwdWindowEvent event);
    
    /**
     * Precondition: showing && (!iconified) && (!focused), which implies (!closed).
     */
    public void onWindowFocusGained(BwdWindowEvent event);
    
    /**
     * Precondition: focused, which implies showing && (!iconified), which implies (!closed).
     */
    public void onWindowFocusLost(BwdWindowEvent event);
    
    /**
     * Precondition: showing && (!focused) && (!iconified), which implies (!closed).
     */
    public void onWindowIconified(BwdWindowEvent event);

    /**
     * Precondition: showing && iconified, which implies (!focused) && (!closed).
     */
    public void onWindowDeiconified(BwdWindowEvent event);

    /**
     * Precondition: showing && (!iconified) && (!maximized), which implies (!closed).
     */
    public void onWindowMaximized(BwdWindowEvent event);
    
    /**
     * Precondition: showing && (!iconified) && maximized, which implies (!closed).
     */
    public void onWindowDemaximized(BwdWindowEvent event);
    
    /**
     * Precondition: showing && (!iconified), which implies (!closed).
     */
    public void onWindowMoved(BwdWindowEvent event);
    
    /**
     * Precondition: showing && (!iconified), which implies (!closed).
     */
    public void onWindowResized(BwdWindowEvent event);

    /**
     * Precondition: (!showing) && (!focused) && (!closed).
     */
    public void onWindowClosed(BwdWindowEvent event);
    
    /*
     * Key events.
     */

    /**
     * Precondition: (!closed).
     */
    public void onKeyPressed(BwdKeyEventPr event);

    /**
     * Precondition: (!closed).
     */
    public void onKeyTyped(BwdKeyEventT event);

    /**
     * Precondition: (!closed).
     */
    public void onKeyReleased(BwdKeyEventPr event);
    
    /*
     * Mouse events.
     */

    /**
     * Precondition: (!closed).
     */
    public void onMousePressed(BwdMouseEvent event);

    /**
     * Precondition: (!closed).
     */
    public void onMouseReleased(BwdMouseEvent event);
    
    /**
     * This method is special, as it must not be called by the binding,
     * but, if ever, by a higher level layer, such as a toolkit,
     * depending on which "component" the mouse was at press
     * and release time.
     * We still define this method here, for high level layers
     * not to have to extend this interface just for it,
     * and for consistency with BwdEventType in which this event
     * is defined as well.
     * 
     * Precondition: (!closed).
     */
    public void onMouseClicked(BwdMouseEvent event);

    /**
     * Precondition: (!closed).
     */
    public void onMouseEnteredClient(BwdMouseEvent event);

    /**
     * Precondition: (!closed).
     */
    public void onMouseExitedClient(BwdMouseEvent event);

    /**
     * Precondition: (!closed).
     */
    public void onMouseMoved(BwdMouseEvent event);

    /**
     * Precondition: (!closed).
     */
    public void onMouseDragged(BwdMouseEvent event);
    
    /*
     * Wheel events.
     */

    /**
     * Precondition: (!closed).
     */
    public void onWheelRolled(BwdWheelEvent event);
}
