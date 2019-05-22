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

/**
 * Throwing object pattern.
 * 
 * All methods throw UnsupportedOperationException by default.
 * 
 * Can be used instead of NoOpBwdEventListener, when wanting to make sure
 * that non-overridden methods don't get called inadvertently.
 */
public class ThrowingBwdEventListener implements InterfaceBwdEventListener {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ThrowingBwdEventListener() {
    }
    
    /*
     * Window events.
     */
    
    @Override
    public void onWindowShown(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowFocusGained(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowFocusLost(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowDeiconified(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowMoved(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onWindowResized(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        throw new UnsupportedOperationException();
    }

    /*
     * Key events.
     */

    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onKeyReleased(BwdKeyEventPr event) {
        throw new UnsupportedOperationException();
    }

    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void onMouseClicked(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    /*
     * Wheel events.
     */

    @Override
    public void onWheelRolled(BwdWheelEvent event) {
        throw new UnsupportedOperationException();
    }
}
