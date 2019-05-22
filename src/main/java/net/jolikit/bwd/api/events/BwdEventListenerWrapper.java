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

import net.jolikit.lang.LangUtils;

/**
 * Delegates to a backing instance.
 * 
 * Designed to be extended, with overriding only the desired methods.
 */
public class BwdEventListenerWrapper implements InterfaceBwdEventListener {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBwdEventListener listener;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param listener Must not be null.
     */
    public BwdEventListenerWrapper(InterfaceBwdEventListener listener) {
        this.listener = LangUtils.requireNonNull(listener);
    }
    
    /*
     * Window events.
     */
    
    @Override
    public void onWindowShown(BwdWindowEvent event) {
        this.listener.onWindowShown(event);
    }
    
    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        this.listener.onWindowHidden(event);
    }
    
    @Override
    public void onWindowFocusGained(BwdWindowEvent event) {
        this.listener.onWindowFocusGained(event);
    }
    
    @Override
    public void onWindowFocusLost(BwdWindowEvent event) {
        this.listener.onWindowFocusLost(event);
    }
    
    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        this.listener.onWindowIconified(event);
    }
    
    @Override
    public void onWindowDeiconified(BwdWindowEvent event) {
        this.listener.onWindowDeiconified(event);
    }
    
    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        this.listener.onWindowMaximized(event);
    }
    
    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        this.listener.onWindowDemaximized(event);
    }
    
    @Override
    public void onWindowMoved(BwdWindowEvent event) {
        this.listener.onWindowMoved(event);
    }
    
    @Override
    public void onWindowResized(BwdWindowEvent event) {
        this.listener.onWindowResized(event);
    }
    
    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        this.listener.onWindowClosed(event);
    }

    /*
     * Key events.
     */

    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        this.listener.onKeyPressed(event);
    }

    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        this.listener.onKeyTyped(event);
    }

    @Override
    public void onKeyReleased(BwdKeyEventPr event) {
        this.listener.onKeyReleased(event);
    }

    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        this.listener.onMousePressed(event);
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        this.listener.onMouseReleased(event);
    }
    
    @Override
    public void onMouseClicked(BwdMouseEvent event) {
        this.listener.onMouseClicked(event);
    }

    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        this.listener.onMouseEnteredClient(event);
    }

    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        this.listener.onMouseExitedClient(event);
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        this.listener.onMouseMoved(event);
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        this.listener.onMouseDragged(event);
    }

    /*
     * Wheel events.
     */

    @Override
    public void onWheelRolled(BwdWheelEvent event) {
        this.listener.onWheelRolled(event);
    }
}
