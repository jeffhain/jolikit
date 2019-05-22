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

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.Unchecked;

/**
 * Wrapper to catch and handle exceptions thrown by a client,
 * and keep track of its state.
 * Also take care not to forward any event to the client
 * if it's closed, but doesn't ensure more consistency than that.
 */
public class ClientWrapperForHost implements InterfaceBwdClient {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final BaseBwdBindingConfig bindingConfig;
    
    private final InterfaceBwdClient client;
    
    /*
     * 
     */
    
    private boolean showing = false;
    private boolean focused = false;
    private boolean iconified = false;
    private boolean maximized = false;
    
    private boolean movedPending = false;
    private boolean resizedPending = false;
    
    private boolean closed = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param bindingConfig Must not be null.
     * @param client Must not be null.
     */
    public ClientWrapperForHost(
            BaseBwdBindingConfig bindingConfig,
            InterfaceBwdClient client) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
        this.client = LangUtils.requireNonNull(client);
    }
    
    /*
     * 
     */
    
    /**
     * @return The client wrapped by this wrapper.
     */
    public InterfaceBwdClient getClient() {
        return this.client;
    }
    
    /*
     * 
     */

    /**
     * @return Whether the state is SHOWING for client events observers.
     */
    public boolean isShowing() {
        return this.showing;
    }

    /**
     * @return Whether the state is FOCUSED for client events observers.
     */
    public boolean isFocused() {
        return this.focused;
    }

    /**
     * @return Whether the state is ICONIFIED for client events observers.
     */
    public boolean isIconified() {
        return this.iconified;
    }

    /**
     * @return Whether the state is MAXIMIZED for client events observers.
     */
    public boolean isMaximized() {
        return this.maximized;
    }

    /**
     * @return The "movedPending" flag value, which is set to false
     *         just before MOVED event is sent to the client.
     */
    public boolean isMovedPending() {
        return this.movedPending;
    }

    /**
     * Sets "movedPending" flag to true.
     */
    public void setMovedPending() {
        this.movedPending = true;
    }

    /**
     * @return The "resizedPending" flag value, which is set to false
     *         just before RESIZED event is sent to the client.
     */
    public boolean isResizedPending() {
        return this.resizedPending;
    }

    /**
     * Sets "resizedPending" flag to true.
     */
    public void setResizedPending() {
        this.resizedPending = true;
    }

    /**
     * @return Whether the state is CLOSED for client events observers.
     */
    public boolean isClosed() {
        return this.closed;
    }

    /*
     * Window events.
     */
    
    @Override
    public void onWindowShown(BwdWindowEvent event) {
        this.showing = true;
        try {
            this.client.onWindowShown(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.showing = false;
        try {
            this.client.onWindowHidden(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowFocusGained(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.focused = true;
        try {
            this.client.onWindowFocusGained(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowFocusLost(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.focused = false;
        try {
            this.client.onWindowFocusLost(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.iconified = true;
        try {
            this.client.onWindowIconified(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowDeiconified(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.iconified = false;
        try {
            this.client.onWindowDeiconified(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.maximized = true;
        try {
            this.client.onWindowMaximized(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.maximized = false;
        try {
            this.client.onWindowDemaximized(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowMoved(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.movedPending = false;
        try {
            this.client.onWindowMoved(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowResized(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.resizedPending = false;
        try {
            this.client.onWindowResized(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            this.client.onWindowClosed(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    /*
     * Key events.
     */
    
    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onKeyPressed(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onKeyTyped(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onKeyReleased(BwdKeyEventPr event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onKeyReleased(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    /*
     * Mouse events.
     */
    
    @Override
    public void onMousePressed(BwdMouseEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onMousePressed(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onMouseReleased(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onMouseClicked(BwdMouseEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onMouseClicked(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onMouseEnteredClient(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onMouseExitedClient(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onMouseMoved(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onMouseDragged(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }
    
    /*
     * Wheel events.
     */

    @Override
    public void onWheelRolled(BwdWheelEvent event) {
        if (this.closed) {
            return;
        }
        try {
            this.client.onWheelRolled(event);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    /*
     * Client methods.
     */

    @Override
    public void setHost(Object host) {
        try {
            this.client.setHost(host);
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public void processEventualBufferedEvents() {
        try {
            this.client.processEventualBufferedEvents();
        } catch (Throwable t) {
            this.onThrowable(t);
        }
    }

    @Override
    public List<GRect> paintClient(InterfaceBwdGraphics g, GRect dirtyRect) {
        // Not final because compiler not smart enough.
        List<GRect> ret;
        try {
            ret = this.client.paintClient(g, dirtyRect);
        } catch (Throwable t) {
            this.onThrowable(t);
            // Pretending everything was painted,
            // so that we can see what was done
            // before throwing.
            ret = GRect.DEFAULT_HUGE_IN_LIST;
        }
        return ret;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void onThrowable(Throwable t) {
        // Checked dynamically, so that can be configured late in tests.
        if (this.bindingConfig.getMustUseExceptionHandlerForClient()) {
            this.bindingConfig.getExceptionHandler().uncaughtException(Thread.currentThread(), t);
        } else {
            // This hack allows to factor much code.
            Unchecked.throwIt(t);
        }
    }
}
