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
package net.jolikit.bwd.impl.utils;

import net.jolikit.bwd.api.events.BwdEventType;

public class HostStateUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Has always the same size (easier to compare line by line).
     */
    public static String toStringHostState(
            boolean showing,
            boolean focused,
            boolean iconified,
            boolean maximized,
            boolean movedPending,
            boolean resizedPending,
            boolean closed) {
        return "[" + (showing ? "sho" : "hid")
                + ", " + (focused ? "foc" : "unf")
                + ", " + (iconified ? "ico" : "dei")
                + ", " + (maximized ? "max" : "dem")
                + ", " + (movedPending ? "mov" : "---")
                + ", " + (resizedPending ? "res" : "---")
                + ", " + (closed ? "clo" : "ope") + "]";
    }
    
    /**
     * Has always the same size (easier to compare line by line).
     */
    public static String toStringHostState(
            boolean showing,
            boolean focused,
            boolean iconified,
            boolean maximized,
            boolean closed) {
        return "[" + (showing ? "sho" : "hid")
                + ", " + (focused ? "foc" : "unf")
                + ", " + (iconified ? "ico" : "dei")
                + ", " + (maximized ? "max" : "dem")
                + ", " + (closed ? "clo" : "ope") + "]";
    }

    public static String toStringBackingState(
            boolean showing,
            boolean focused,
            boolean iconified,
            boolean maximized) {
        return "[" + (showing ? "sho" : "hid")
                + ", " + (focused ? "foc" : "unf")
                + ", " + (iconified ? "ico" : "dei")
                + ", " + (maximized ? "max" : "dem") + "]";
    }
    
    /*
     * 
     */
    
    public static boolean isSpamEventType(BwdEventType eventType) {
        return (eventType == BwdEventType.WINDOW_MOVED)
                || (eventType == BwdEventType.WINDOW_RESIZED)
                || (eventType == BwdEventType.MOUSE_MOVED)
                || (eventType == BwdEventType.MOUSE_DRAGGED)
                || (eventType == BwdEventType.WHEEL_ROLLED);
    }

    /**
     * To check whether a fired event type is compatible
     * with the client state observed so far.
     */
    public static boolean isEventTypeCompatibleWithClientState(
            boolean clientStateShowing,
            boolean clientStateFocused,
            boolean clientStateIconified,
            boolean clientStateMaximized,
            boolean clientStateClosed,
            //
            BwdEventType eventType) {
        
        if (clientStateClosed) {
            return false;
        }
        switch (eventType) {
        case WINDOW_SHOWN: return (!clientStateShowing);
        case WINDOW_HIDDEN: return clientStateShowing;
        case WINDOW_FOCUS_GAINED: return clientStateShowing && (!clientStateIconified) && (!clientStateFocused);
        case WINDOW_FOCUS_LOST: return clientStateShowing && (!clientStateIconified) && clientStateFocused;
        case WINDOW_ICONIFIED: return clientStateShowing && (!clientStateFocused) && (!clientStateIconified);
        case WINDOW_DEICONIFIED: return clientStateShowing && (!clientStateFocused) && clientStateIconified;
        case WINDOW_MAXIMIZED: return clientStateShowing && (!clientStateIconified) && (!clientStateMaximized);
        case WINDOW_DEMAXIMIZED: return clientStateShowing && (!clientStateIconified) && clientStateMaximized;
        case WINDOW_MOVED: return clientStateShowing && (!clientStateIconified);
        case WINDOW_RESIZED: return clientStateShowing && (!clientStateIconified);
        case WINDOW_CLOSED: return (!clientStateShowing) && (!clientStateFocused);
        //
        case KEY_PRESSED:
        case KEY_TYPED:
        case KEY_RELEASED:
        //
        case MOUSE_PRESSED:
        case MOUSE_RELEASED:
        case MOUSE_ENTERED_CLIENT:
        case MOUSE_EXITED_CLIENT:
        case MOUSE_MOVED:
        case MOUSE_DRAGGED:
        //
        case WHEEL_ROLLED: {
            return true;
        }
        default:
            throw new AssertionError("" + eventType);
        }
    }

    /**
     * For key/mouse/wheel events, the compatibility constraints are tighter
     * than the (quite relaxed) preconditions defined in event listener,
     * which doesn't hurt because it's acceptable for them to be violated
     * from time to time, and which helps figuring out when these events
     * occur in states where they are not really supposed to.
     * 
     * @return False if state read from host at event time is consistent
     *         with the event (some inconsistencies can be normal, due to
     *         events being typically processed asynchronously).
     */
    public static boolean isReadStateCompatibleWithEventType(
            BwdEventType eventType,
            boolean readStateShowing,
            boolean readStateFocused,
            boolean readStateIconified,
            boolean readStateMaximized,
            boolean readStateClosed) {
        switch (eventType) {
        case WINDOW_SHOWN: return (!readStateClosed) && readStateShowing;
        // Allowing host to be closed, since must be fired on closing if was showing.
        case WINDOW_HIDDEN: return (!readStateShowing);
        case WINDOW_FOCUS_GAINED: return (!readStateClosed) && readStateShowing && (!readStateIconified) && readStateFocused;
        // Allowing host to be closed, since must be fired on closing if was focused.
        // Allowing host to be hidden or iconified, since must be fired on hiding or iconification if was focused.
        case WINDOW_FOCUS_LOST: return (!readStateFocused);
        case WINDOW_ICONIFIED: return (!readStateClosed) && readStateShowing && (!readStateFocused) && readStateIconified;
        case WINDOW_DEICONIFIED: return (!readStateClosed) && readStateShowing && (!readStateFocused) && (!readStateIconified);
        case WINDOW_MAXIMIZED: return (!readStateClosed) && readStateShowing && (!readStateIconified) && readStateMaximized;
        case WINDOW_DEMAXIMIZED: return (!readStateClosed) && readStateShowing && (!readStateIconified) && (!readStateMaximized);
        case WINDOW_MOVED: return (!readStateClosed) && readStateShowing && (!readStateIconified);
        case WINDOW_RESIZED: return (!readStateClosed) && readStateShowing && (!readStateIconified);
        // Allowing host to be closed, since is fired afterwards.
        case WINDOW_CLOSED: return (!readStateShowing) && (!readStateFocused);
        //
        case KEY_PRESSED:
        case KEY_TYPED:
        case KEY_RELEASED: {
            return (!readStateClosed) && readStateShowing && (!readStateIconified) && readStateFocused;
        }
        //
        case MOUSE_PRESSED:
        case MOUSE_RELEASED:
        case MOUSE_ENTERED_CLIENT: {
            return (!readStateClosed) && readStateShowing && (!readStateIconified);
        }
        case MOUSE_EXITED_CLIENT: {
            return (!readStateClosed);
        }
        case MOUSE_MOVED:
        case MOUSE_DRAGGED: {
            return (!readStateClosed) && readStateShowing && (!readStateIconified);
        }
        //
        case WHEEL_ROLLED: {
            return (!readStateClosed) && readStateShowing && (!readStateIconified) && readStateFocused;
        }
        default:
            throw new AssertionError("" + eventType);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private HostStateUtils() {
    }
}
