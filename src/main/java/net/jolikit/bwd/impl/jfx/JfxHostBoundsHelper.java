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
    protected GRect getInsetsDecorated_rawInOs() {
        final GRect ret;
        if (MUST_USE_CONFIGURED_INSETS) {
            ret = this.decorationInsets;
        } else {
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final Stage window = (Stage) holder.getBackingWindow();
            final Scene scene = window.getScene();
            final Pane pane = (Pane) scene.getRoot();

            final int leftInOs = (int) scene.getX();
            final int topInOs = (int) scene.getY();

            final int windowXSpanInOs = (int) window.getWidth();
            final int windowYSpanInOs = (int) window.getHeight();
            final int clientXSpanInOs = (int) pane.getWidth();
            final int clientYSpanInOs = (int) pane.getHeight();

            final int rightInOs = Math.max(0,
                (windowXSpanInOs - clientXSpanInOs) - leftInOs);
            final int bottomInOs = Math.max(0,
                (windowYSpanInOs - clientYSpanInOs) - topInOs);
            
            ret = GRect.valueOf(leftInOs, topInOs, rightInOs, bottomInOs);
        }
        return ret;
    }

    /*
     * 
     */

    @Override
    protected GRect getClientBounds_rawInOs() {
        final GRect ret;
        if (MUST_USE_CONFIGURED_INSETS) {
            ret = super.getClientBounds_rawInOs();
        } else {
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final Stage window = (Stage) holder.getBackingWindow();
            final Scene scene = window.getScene();
            final Pane pane = (Pane) scene.getRoot();

            final int xInOs = (int) (window.getX() + scene.getX());
            final int yInOs = (int) (window.getY() + scene.getY());
            final int widthInOs = (int) pane.getWidth();
            final int heightInOs = (int) pane.getHeight();
            
            ret = GRect.valueOf(xInOs, yInOs, widthInOs, heightInOs);
        }
        return ret;
    }
    
    @Override
    protected GRect getWindowBounds_rawInOs() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Stage window = (Stage) holder.getBackingWindow();
        
        final int xInOs = (int) window.getX();
        final int yInOs = (int) window.getY();
        final int widthInOs = (int) window.getWidth();
        final int heightInOs = (int) window.getHeight();
        
        return GRect.valueOf(xInOs, yInOs, widthInOs, heightInOs);
    }

    /*
     * 
     */
    
    @Override
    protected void setClientBounds_rawInOs(GRect targetClientBoundsInOs) {
        if (MUST_USE_CONFIGURED_INSETS) {
            super.setClientBounds_rawInOs(targetClientBoundsInOs);
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

            final int oldWindowXInOs = (int) window.getX();
            final int oldWindowYInOs = (int) window.getY();
            final int oldWindowWidthInOs = (int) window.getWidth();
            final int oldWindowHeightInOs = (int) window.getHeight();

            final int oldClientXInOs = (int) (window.getX() + scene.getX());
            final int oldClientYInOs = (int) (window.getY() + scene.getY());
            final int oldClientWidthInOs = (int) pane.getWidth();
            final int oldClientHeightInOs = (int) pane.getHeight();
            
            final int dxInOs = oldClientXInOs - oldWindowXInOs;
            final int dyInOs = oldClientYInOs - oldWindowYInOs;
            final int dxSpanInOs = oldWindowWidthInOs - oldClientWidthInOs;
            final int dySpanInOs = oldWindowHeightInOs - oldClientHeightInOs;
            
            final int newWindowXInOs = targetClientBoundsInOs.x() - dxInOs;
            final int newWindowYInOs = targetClientBoundsInOs.y() - dyInOs;
            final int newWindowWidthInOs = Math.max(0,
                targetClientBoundsInOs.xSpan() + dxSpanInOs);
            final int newWindowHeightInOs = Math.max(0,
                targetClientBoundsInOs.ySpan() + dySpanInOs);
            
            final GRect targetWindowBoundsInOs = GRect.valueOf(
                    newWindowXInOs,
                    newWindowYInOs,
                    newWindowWidthInOs,
                    newWindowHeightInOs);
            
            this.setWindowBounds_rawInOs(targetWindowBoundsInOs);
        }
    }

    @Override
    protected void setWindowBounds_rawInOs(GRect targetWindowBoundsInOs) {
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
        
        final int targetWindowXInOs = targetWindowBoundsInOs.x();
        final int targetWindowYInOs = targetWindowBoundsInOs.y();
        
        final int newWidthInOs;
        final int newHeightInOs;
        if (holder.isDecorated()) {
            newWidthInOs = Math.max(targetWindowBoundsInOs.xSpan(), this.minWindowWidthIfDecorated);
            newHeightInOs = Math.max(targetWindowBoundsInOs.ySpan(), this.minWindowHeightIfDecorated);
        } else {
            newWidthInOs = Math.max(targetWindowBoundsInOs.xSpan(), MIN_WINDOW_WIDTH_IF_UNDECORATED);
            newHeightInOs = Math.max(targetWindowBoundsInOs.ySpan(), MIN_WINDOW_HEIGHT_IF_UNDECORATED);
        }
        
        /*
         * 
         */
        
        final GRect oldWindowBoundsInOs = this.getWindowBounds_rawInOs();
        
        /*
         * 
         */
        
        final boolean mustSetXPosFirst =
            (targetWindowBoundsInOs.xSpan() > oldWindowBoundsInOs.xSpan());
        final boolean mustSetYPosFirst =
            (targetWindowBoundsInOs.ySpan() > oldWindowBoundsInOs.ySpan());
        if (mustSetXPosFirst) {
            window.setX(targetWindowXInOs);
        }
        if (mustSetYPosFirst) {
            window.setY(targetWindowYInOs);
        }
        
        /*
         * 
         */
        
        final int oldWidthInOs = (int) window.getWidth();
        final int oldHeightInOs = (int) window.getHeight();
        final int deltaWidthInOs = newWidthInOs - oldWidthInOs;
        final int deltaHeightInOs = newHeightInOs - oldHeightInOs;
        
        window.setWidth(newWidthInOs);
        window.setHeight(newHeightInOs);
        
        /*
         * 
         */

        if (!mustSetXPosFirst) {
            window.setX(targetWindowXInOs);
        }
        if (!mustSetYPosFirst) {
            window.setY(targetWindowYInOs);
        }

        /*
         * TODO jfx We want to ensure values consistency now,
         * else they are only kept consistent later
         * (due to the heavy asynchronicity of JavaFX).
         * Resizing the pane, because we can't directly set canvas spans,
         * which are bound to pane spans.
         * This helps at least when window is showing and deiconified.
         */

        final int oldClientWidthInOs = (int) pane.getWidth();
        final int oldClientHeightInOs = (int) pane.getHeight();

        final int newClientWidthInOs =
            oldClientWidthInOs + deltaWidthInOs;
        final int newClientHeightInOs =
            oldClientHeightInOs + deltaHeightInOs;
        
        pane.resize(
            newClientWidthInOs,
            newClientHeightInOs);
    }
}
