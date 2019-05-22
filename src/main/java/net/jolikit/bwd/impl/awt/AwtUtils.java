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
package net.jolikit.bwd.impl.awt;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;

import net.jolikit.bwd.api.graphics.GRect;

public class AwtUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param rect A backing rectangle.
     * @return Corresponding GRect.
     */
    public static GRect toGRect(Rectangle rect) {
        return GRect.valueOf(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @param window A window.
     * @param g A graphics of the specified window, with or without a clip.
     * @return The corresponding dirty rectangle to use, in client area
     *         coordinates.
     */
    public static GRect dirtyRectMovedFromWindowToClient(Window window, Graphics g) {
        final Rectangle clip = g.getClipBounds();
        final GRect dirtyRect;
        if (clip == null) {
            dirtyRect = GRect.DEFAULT_HUGE;
        } else {
            dirtyRect = dirtyRectMovedFromWindowToClient(
                    window,
                    clip.x,
                    clip.y,
                    clip.width,
                    clip.height);
        }
        return dirtyRect;
    }
    
    /**
     * @param window A window.
     * @param x Clip X in window coordinates.
     * @param y Clip Y in window coordinates.
     * @param xSpan Clip X span in window coordinates.
     * @param ySpan Clip Y span in window coordinates.
     * @return The corresponding dirty rectangle to use, in client area
     *         coordinates.
     */
    public static GRect dirtyRectMovedFromWindowToClient(
            Window window,
            int x, int y, int width, int height) {
        final Insets insets = window.getInsets();
        return GRect.valueOf(
                x - insets.left,
                y - insets.top,
                width,
                height);
    }
    
    /*
     * 
     */
    
    /**
     * Only returns true if both isShowing() and isVisible()
     * are true.
     */
    public static boolean isShowing(Window window) {
        /*
         * TODO awt isShowing() seems to return the boolean
         * set with setVisible(...), but isVisible() is more
         * terminologically consistent with setVisible(...),
         * so we're not sure which to use, but using both
         * shouldn't hurt (better be wrongly false
         * than wrongly true).
         */
        return window.isShowing()
                && window.isVisible();
    }
    
    public static boolean isIconified(Window window) {
        if (window instanceof Frame) {
            final Frame frame = (Frame) window;
            return (frame.getExtendedState() & Frame.ICONIFIED) != 0;
        } else {
            // Dialogs never iconified.
            return false;
        }
    }
    
    public static boolean isShowingAndDeiconified(Window window) {
        return isShowing(window)
                && (!isIconified(window));
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private AwtUtils() {
    }
}
