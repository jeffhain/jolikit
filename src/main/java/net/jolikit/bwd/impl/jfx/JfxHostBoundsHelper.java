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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;

public class JfxHostBoundsHelper extends AbstractHostBoundsHelper {

    /*
     * TODO jfx Due to the heavy asynchronous nature of JavaFX, and more
     * precisely due to bounds being stored as separate properties,
     * when computing spans we must use max(0,_) to avoid to eventually
     * end up with negative values, and we set values even if they didn't
     * (appear) to have changed, to avoid false negatives.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * TODO jfx Stage and Pane bounds can be inconsistent.
     */
    private static final boolean MUST_USE_CONFIGURED_INSETS = true;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * TODO jfx If an undecorated window span is set to 0,
     * JavaFX makes it 1 at rendering.
     */
    private static final int MIN_WINDOW_WIDTH_IF_UNDECORATED = 1;
    private static final int MIN_WINDOW_HEIGHT_IF_UNDECORATED = 1;

    /*
     * 
     */
    
    private final GRect decorationInsets;
    
    private final int minWindowWidthIfDecorated;

    private final int minWindowHeightIfDecorated;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JfxHostBoundsHelper(
            InterfaceBackingWindowHolder holder,
            GRect decorationInsets,
            int minWindowWidthIfDecorated,
            int minWindowHeightIfDecorated) {
        super(holder);
        this.decorationInsets = decorationInsets;
        this.minWindowWidthIfDecorated = minWindowWidthIfDecorated;
        this.minWindowHeightIfDecorated = minWindowHeightIfDecorated;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected GRect getInsetsDecorated_raw() {
        if (MUST_USE_CONFIGURED_INSETS) {
            return this.decorationInsets;
        } else {
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final Stage window = (Stage) holder.getBackingWindow();
            final Scene scene = window.getScene();
            final Pane pane = (Pane) scene.getRoot();

            final int left = (int) scene.getX();
            final int top = (int) scene.getY();

            final int windowXSpan = (int) window.getWidth();
            final int windowYSpan = (int) window.getHeight();
            final int clientXSpan = (int) pane.getWidth();
            final int clientYSpan = (int) pane.getHeight();

            final int right = Math.max(0, (windowXSpan - clientXSpan) - left);
            final int bottom = Math.max(0, (windowYSpan - clientYSpan) - top);

            return GRect.valueOf(left, top, right, bottom);
        }
    }

    /*
     * 
     */

    @Override
    protected GRect getClientBounds_raw() {
        if (MUST_USE_CONFIGURED_INSETS) {
            return super.getClientBounds_raw();
        } else {
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final Stage window = (Stage) holder.getBackingWindow();
            final Scene scene = window.getScene();
            final Pane pane = (Pane) scene.getRoot();

            final int x = (int) (window.getX() + scene.getX());
            final int y = (int) (window.getY() + scene.getY());
            final int width = (int) pane.getWidth();
            final int height = (int) pane.getHeight();

            return GRect.valueOf(x, y, width, height);
        }
    }
    
    @Override
    protected GRect getWindowBounds_raw() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Stage window = (Stage) holder.getBackingWindow();
        
        final int x = (int) window.getX();
        final int y = (int) window.getY();
        final int width = (int) window.getWidth();
        final int height = (int) window.getHeight();
        
        return GRect.valueOf(x, y, width, height);
    }

    /*
     * 
     */
    
    @Override
    protected void setClientBounds_raw(GRect targetClientBounds) {
        if (MUST_USE_CONFIGURED_INSETS) {
            super.setClientBounds_raw(targetClientBounds);
        } else {
            /*
             * Still delegates to window bounds setting,
             * but should be lighter, and less inconsistency-prone,
             * than using super implementation.
             */
            
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final Stage window = (Stage) holder.getBackingWindow();
            final Scene scene = window.getScene();
            final Pane pane = (Pane) scene.getRoot();

            final int oldWindowX = (int) window.getX();
            final int oldWindowY = (int) window.getY();
            final int oldWindowWidth = (int) window.getWidth();
            final int oldWindowHeight = (int) window.getHeight();

            final int oldClientX = (int) (window.getX() + scene.getX());
            final int oldClientY = (int) (window.getY() + scene.getY());
            final int oldClientWidth = (int) pane.getWidth();
            final int oldClientHeight = (int) pane.getHeight();
            
            final int newWindowX = targetClientBounds.x() - (oldClientX - oldWindowX);
            final int newWindowY = targetClientBounds.y() - (oldClientY - oldWindowY);
            final int newWindowWidth = Math.max(0, targetClientBounds.xSpan() + (oldWindowWidth - oldClientWidth));
            final int newWindowHeight = Math.max(0, targetClientBounds.ySpan() + (oldWindowHeight - oldClientHeight));
            
            final GRect targetWindowBounds = GRect.valueOf(
                    newWindowX,
                    newWindowY,
                    newWindowWidth,
                    newWindowHeight);
            
            this.setWindowBounds_raw(targetWindowBounds);
        }
    }

    @Override
    protected void setWindowBounds_raw(GRect targetWindowBounds) {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Stage window = (Stage) holder.getBackingWindow();
        final Scene scene = window.getScene();
        final Pane pane = (Pane) scene.getRoot();
        
        /*
         * TODO jfx When X span shrinks, which can correspond to left
         * being dragged to the right, we set position last, so that the window
         * doesn't flash away with its old large span still valid for an instant,
         * and when X span grows, which can correspond to left being
         * dragged to the left, we set position first, for the same reason.
         * Same for Y.
         */
        
        final GRect oldWindowBounds = this.getWindowBounds_raw();
        final boolean mustSetXPosFirst = (targetWindowBounds.xSpan() > oldWindowBounds.xSpan());
        final boolean mustSetYPosFirst = (targetWindowBounds.ySpan() > oldWindowBounds.ySpan());
        if (mustSetXPosFirst) {
            window.setX(targetWindowBounds.x());
        }
        if (mustSetYPosFirst) {
            window.setY(targetWindowBounds.y());
        }
        
        /*
         * 
         */
        
        final int newWidth;
        final int newHeight;
        if (holder.isDecorated()) {
            newWidth = Math.max(targetWindowBounds.xSpan(), this.minWindowWidthIfDecorated);
            newHeight = Math.max(targetWindowBounds.ySpan(), this.minWindowHeightIfDecorated);
        } else {
            newWidth = Math.max(targetWindowBounds.xSpan(), MIN_WINDOW_WIDTH_IF_UNDECORATED);
            newHeight = Math.max(targetWindowBounds.ySpan(), MIN_WINDOW_HEIGHT_IF_UNDECORATED);
        }
        
        final int oldWidth = (int) window.getWidth();
        final int oldHeight = (int) window.getHeight();
        final int deltaWidth = newWidth - oldWidth;
        final int deltaHeight = newHeight - oldHeight;

        window.setWidth(newWidth);
        window.setHeight(newHeight);
        
        /*
         * 
         */

        if (!mustSetXPosFirst) {
            window.setX(targetWindowBounds.x());
        }
        if (!mustSetYPosFirst) {
            window.setY(targetWindowBounds.y());
        }

        /*
         * TODO jfx We want to ensure values consistency now,
         * else they are only kept consistent later
         * (due to the heavy asynchronicity of JavaFX).
         * Resizing the pane, because we can't directly set canvas spans,
         * which are bound to pane spans.
         * This helps at least when window is showing and deiconified.
         */

        final int oldClientWidth = (int) pane.getWidth();
        final int oldClientHeight = (int) pane.getHeight();

        final int newClientWidth = oldClientWidth + deltaWidth;
        final int newClientHeight = oldClientHeight + deltaHeight;

        pane.resize(newClientWidth, newClientHeight);
    }
}
