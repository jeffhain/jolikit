/*
 * Copyright 2019-2024 Jeff Hain
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
 * Null object pattern.
 * 
 * All methods do nothing by default.
 * 
 * Designed to be extended, with overriding only the desired methods.
 */
public class NoOpBwdEventListener implements InterfaceBwdEventListener {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public NoOpBwdEventListener() {
    }
    
    /*
     * Window events.
     */
    
    @Override
    public void onWindowShown(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowFocusGained(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowFocusLost(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowDeiconified(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowMoved(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onWindowResized(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        this.onAnyBwdEvent(event);
    }

    /*
     * Key events.
     */

    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onKeyReleased(BwdKeyEventPr event) {
        this.onAnyBwdEvent(event);
    }

    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    @Override
    public void onMouseClicked(BwdMouseEvent event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        this.onAnyBwdEvent(event);
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        this.onAnyBwdEvent(event);
    }

    /*
     * Wheel events.
     */

    @Override
    public void onWheelRolled(BwdWheelEvent event) {
        this.onAnyBwdEvent(event);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Called by default specific onXxx() methods implementations.
     * Allows to easily intercept event of multiple types.
     * 
     * Overriding must call super.
     * This default implementation does nothing.
     */
    @SuppressWarnings("unused")
    protected void onAnyBwdEvent(BwdEvent event) {
    }
}
