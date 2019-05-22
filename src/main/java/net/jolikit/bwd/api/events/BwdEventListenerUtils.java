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

import java.util.List;

/**
 * Utilities to deal with BWD events listeners.
 */
public class BwdEventListenerUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param listener Must not be null.
     * @param event Must not be null.
     */
    public static void callProperMethod(
            InterfaceBwdEventListener listener,
            BwdEvent event) {
        
        final BwdEventType eventType = event.getEventType();
        switch (eventType) {
        case WINDOW_SHOWN: listener.onWindowShown((BwdWindowEvent) event); break;
        case WINDOW_HIDDEN: listener.onWindowHidden((BwdWindowEvent) event); break;
        case WINDOW_FOCUS_GAINED: listener.onWindowFocusGained((BwdWindowEvent) event); break;
        case WINDOW_FOCUS_LOST: listener.onWindowFocusLost((BwdWindowEvent) event); break;
        case WINDOW_ICONIFIED: listener.onWindowIconified((BwdWindowEvent) event); break;
        case WINDOW_DEICONIFIED: listener.onWindowDeiconified((BwdWindowEvent) event); break;
        case WINDOW_MAXIMIZED: listener.onWindowMaximized((BwdWindowEvent) event); break;
        case WINDOW_DEMAXIMIZED: listener.onWindowDemaximized((BwdWindowEvent) event); break;
        case WINDOW_MOVED: listener.onWindowMoved((BwdWindowEvent) event); break;
        case WINDOW_RESIZED: listener.onWindowResized((BwdWindowEvent) event); break;
        case WINDOW_CLOSED: listener.onWindowClosed((BwdWindowEvent) event); break;
        //
        case KEY_PRESSED: listener.onKeyPressed((BwdKeyEventPr) event); break;
        case KEY_TYPED: listener.onKeyTyped((BwdKeyEventT) event); break;
        case KEY_RELEASED: listener.onKeyReleased((BwdKeyEventPr) event); break;
        //
        case MOUSE_PRESSED: listener.onMousePressed((BwdMouseEvent) event); break;
        case MOUSE_RELEASED: listener.onMouseReleased((BwdMouseEvent) event); break;
        case MOUSE_CLICKED: listener.onMouseClicked((BwdMouseEvent) event); break;
        case MOUSE_ENTERED_CLIENT: listener.onMouseEnteredClient((BwdMouseEvent) event); break;
        case MOUSE_EXITED_CLIENT: listener.onMouseExitedClient((BwdMouseEvent) event); break;
        case MOUSE_MOVED: listener.onMouseMoved((BwdMouseEvent) event); break;
        case MOUSE_DRAGGED: listener.onMouseDragged((BwdMouseEvent) event); break;
        //
        case WHEEL_ROLLED: listener.onWheelRolled((BwdWheelEvent) event); break;
        default:
            throw new IllegalArgumentException("" + event);
        }
    }

    /**
     * @param listeners Must not be null.
     * @param event Must not be null.
     */
    public static <T extends InterfaceBwdEventListener> void callProperMethods(
            List<T> listeners,
            BwdEvent event) {
        
        final int size = listeners.size();
        for (int i = 0; i < size; i++) {
            final InterfaceBwdEventListener listener = listeners.get(i);
            callProperMethod(listener, event);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdEventListenerUtils() {
    }
}
