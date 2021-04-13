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
package net.jolikit.bwd.impl.utils;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdCursorManager;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;

/**
 * For non-graphical unit tests.
 */
class HostForApiTests extends AbstractBwdHost {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Object backingWindow = new Object();

    private boolean backingWindowShowing = false;
    private boolean backingWindowFocused = false;
    private boolean backingWindowIconified = false;
    private boolean backingWindowMaximized = false;

    private final GRect insets;
    
    private final GRect maximizedClientBounds;
    
    private GRect backingClientBounds;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param maximizedClientBounds Set on maximization. Can be null.
     */
    public HostForApiTests(
            BaseBwdBindingConfig bindingConfig,
            InterfaceBwdBindingImpl binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            HostOfFocusedClientHolder hostOfFocusedClientHolder,
            InterfaceBwdHost owner,
            String title,
            boolean decorated,
            boolean modal,
            InterfaceBwdClient client,
            //
            GRect insets,
            GRect maximizedClientBounds) {
        super(
                bindingConfig,
                binding,
                hostLifecycleListener,
                hostOfFocusedClientHolder,
                owner,
                title,
                decorated,
                modal,
                client);
        
        this.insets = insets;
        
        this.maximizedClientBounds = maximizedClientBounds;
        
        // Ignoring OS/BD scaling.
        this.backingClientBounds = bindingConfig.getDefaultClientOrWindowBoundsInOs();
        
        // Implicit null check.
        client.setHost(this);
        
        hostLifecycleListener.onHostCreated(this);
    }
    
    /*
     * 
     */
    
    @Override
    public InterfaceBwdHost newDialog(
            String title,
            boolean decorated,
            boolean modal,
            InterfaceBwdClient client) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public InterfaceBwdCursorManager getCursorManager() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Object getBackingWindow() {
        return this.backingWindow;
    }
    
    /*
     * 
     */
    
    /**
     * Useful for tests, usually not a feature of UI libraries.
     * 
     * Does nothing if isFocused() is false,
     * i.e. if host is not (showing, deiconified, focused).
     */
    public void requestFocusLoss() {
        if (!this.isFocused()) {
            return;
        }
        if (!this.backingWindowFocused) {
            return;
        }
        this.backingWindowFocused = false;
        this.onBackingWindowFocusLost();
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void paintClientNowOrLater() {
        // Ignoring.
    }
    
    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        // Ignoring.
    }
    
    @Override
    protected boolean isBackingWindowShowing() {
        return this.backingWindowShowing;
    }
    
    @Override
    protected void showBackingWindow() {
        if (this.backingWindowShowing) {
            return;
        }
        this.backingWindowShowing = true;
        // As if by called by a backing event listener.
        this.onBackingWindowShown();
    }
    
    @Override
    protected void hideBackingWindow() {
        if (!this.backingWindowShowing) {
            return;
        }
        
        this.requestFocusLoss();

        this.backingWindowShowing = false;
        // As if by called by a backing event listener.
        this.onBackingWindowHidden();
    }
    
    @Override
    protected boolean isBackingWindowFocused() {
        return this.backingWindowFocused;
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        if (this.backingWindowFocused) {
            return;
        }
        this.backingWindowFocused = true;
        this.onBackingWindowFocusGained();
    }
    
    @Override
    protected boolean doesSetBackingWindowIconifiedOrNotWork() {
        return true;
    }

    @Override
    protected boolean isBackingWindowIconified() {
        return this.backingWindowIconified;
    }
    
    @Override
    protected void setBackingWindowIconifiedOrNot(boolean desiredIconified) {
        if (!this.backingWindowShowing) {
            // This method is not supposed to work in this case.
            return;
        }
        
        if (desiredIconified) {
            this.requestFocusLoss();
        }
        
        if (desiredIconified != this.backingWindowIconified) {
            this.backingWindowIconified = desiredIconified;
            if (desiredIconified) {
                this.onBackingWindowIconified();
            } else {
                this.onBackingWindowDeiconified();
            }
        }
    }
    
    @Override
    protected boolean doesSetBackingWindowMaximizedOrNotWork() {
        return true;
    }
    
    @Override
    protected boolean isBackingWindowMaximized() {
        return this.backingWindowMaximized;
    }
    
    @Override
    protected void setBackingWindowMaximizedOrNot(boolean desiredMaximized) {
        if ((!this.backingWindowShowing)
                || this.backingWindowIconified) {
            // This method is not supposed to work in this case.
            return;
        }
        if (desiredMaximized != this.backingWindowMaximized) {
            this.backingWindowMaximized = desiredMaximized;
            if (desiredMaximized) {
                this.onBackingWindowMaximized();
            } else {
                this.onBackingWindowDemaximized();
            }
            if (desiredMaximized) {
                if (this.maximizedClientBounds != null) {
                    this.setBackingClientBoundsInOs(this.maximizedClientBounds);
                }
            } else {
                // Counting on non-maximized bounds restoring.
            }
        }
    }
    
    @Override
    protected GRect getBackingInsetsInOs() {
        return this.insets;
    }
    
    @Override
    protected GRect getBackingClientBoundsInOs() {
        return this.backingClientBounds;
    }
    
    @Override
    protected GRect getBackingWindowBoundsInOs() {
        return BindingCoordsUtils.computeWindowBounds(this.insets, this.backingClientBounds);
    }
    
    @Override
    protected void setBackingClientBoundsInOs(GRect targetClientBounds) {
        final GRect old = this.backingClientBounds;
        if (targetClientBounds.equals(old)) {
            return;
        }
        this.backingClientBounds = targetClientBounds;
        try {
            if ((old.x() != this.backingClientBounds.x())
                    || (old.y() != this.backingClientBounds.y())) {
                this.onBackingWindowMoved();
            }
        } finally {
            if ((old.xSpan() != this.backingClientBounds.xSpan())
                    || (old.ySpan() != this.backingClientBounds.ySpan())) {
                this.onBackingWindowResized();
            }
        }
    }
    
    @Override
    protected void setBackingWindowBoundsInOs(GRect targetWindowBounds) {
        final GRect targetClientBounds =
                BindingCoordsUtils.computeClientBounds(
                        this.insets,
                        targetWindowBounds);
        this.setBackingClientBoundsInOs(targetClientBounds);
    }
    
    @Override
    protected void closeBackingWindow() {
        
        /*
         * On closing, we wait for these to turn false
         * (we don't want to "force" them to false)
         * before firing CLOSED, so we need to set them
         * to false here.
         */
        
        this.backingWindowShowing = false;
        this.backingWindowFocused = false;
        
        /*
         * We set these to true to check that it doesn't hurt
         * if the backing host does that too.
         */
        
        this.backingWindowIconified = true;
        this.backingWindowMaximized = true;
        
        /*
         * NB: no "closed" flag to set to true,
         * since there is no isBackingWindowClosed() method.
         */
    }
    
    /*
     * Painting.
     */

    @Override
    protected InterfaceBwdGraphics newRootGraphics(GRect boxWithBorder) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void paintBackingClient(
        ScaleHelper scaleHelper,
        GPoint clientSpansInOs,
        GPoint bufferPosInCliInOs,
        GPoint bufferSpansInBd,
        List<GRect> paintedRectList) {
        throw new UnsupportedOperationException();
    }
}
