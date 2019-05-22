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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class AlgrHostBoundsHelper extends AbstractHostBoundsHelper {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * TODO algr Because spans constraints on window actually apply
     * on client area, despite the method name.
     */
    private static final boolean MUST_USE_CLIENT_SPANS_FOR_WINDOW_CONSTRAINTS = true;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    
    private final PixelCoordsConverter pixelCoordsConverter;
    
    private final GRect decorationInsets;
    
    private final boolean mustResizeDisplayExplicitlyOnBoundsSetting;
    
    private final boolean mustRestoreWindowPositionAfterDisplayResize;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AlgrHostBoundsHelper(
            InterfaceBackingWindowHolder holder,
            PixelCoordsConverter pixelCoordsConverter,
            GRect decorationInsets,
            boolean mustResizeDisplayExplicitlyOnBoundsSetting,
            boolean mustRestoreWindowPositionAfterDisplayResize) {
        super(holder);
        this.pixelCoordsConverter = LangUtils.requireNonNull(pixelCoordsConverter);
        this.decorationInsets = LangUtils.requireNonNull(decorationInsets);
        this.mustResizeDisplayExplicitlyOnBoundsSetting = mustResizeDisplayExplicitlyOnBoundsSetting;
        this.mustRestoreWindowPositionAfterDisplayResize = mustRestoreWindowPositionAfterDisplayResize;
    }
    
    /*
     * 
     */

    public void resizeDisplay(int targetWidth, int targetHeight) {
        if (DEBUG) {
            Dbg.logPr(this, "resizeDisplay(" + targetWidth + ", " + targetHeight + ")");
        }
        
        final InterfaceBackingWindowHolder holder = this.getHolder();
        if (holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                Dbg.logPr(this, "resizeDisplay() : host closed, aborting");
            }
            return;
        }
        final Pointer window = (Pointer) holder.getBackingWindow();
        
        final GRect w1 = this.getWindowBounds_raw();
        
        final int targetWidthInDevice = this.pixelCoordsConverter.computeXSpanInDevicePixel(targetWidth);
        final int targetHeightInDevice = this.pixelCoordsConverter.computeYSpanInDevicePixel(targetHeight);
        /*
         * TODO algr On Windows, need to call this after resize from native decoration
         * (which, luckily on Windows, generates a DISPLAY_RESIZE event at the end),
         * else display appears shrinked or grown depending on resize.
         */
        final boolean didIt = LIB.al_resize_display(
                window,
                targetWidthInDevice,
                targetHeightInDevice);
        if (!didIt) {
            throw new BindingError("could not resize display: " + LIB.al_get_errno());
        }
        
        if (this.mustRestoreWindowPositionAfterDisplayResize) {
            LIB.al_set_window_position(
                    window,
                    w1.x(),
                    w1.y());
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected GRect getInsetsDecorated_raw() {
        return this.decorationInsets;
    }

    /*
     * 
     */

    @Override
    protected GRect getClientBounds_raw() {
        return super.getClientBounds_raw();
    }

    @Override
    protected GRect getWindowBounds_raw() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Pointer window = (Pointer) holder.getBackingWindow();
        
        final GRect insets = this.getInsets_raw();
        final int left = insets.x();
        final int top = insets.y();
        final int right = insets.xSpan();
        final int bottom = insets.ySpan();
        
        final IntByReference xRef = new IntByReference();
        final IntByReference yRef = new IntByReference();
        LIB.al_get_window_position(window, xRef, yRef);
        final int x = xRef.getValue();
        final int y = yRef.getValue();
        
        final int displayWidthInDevice = LIB.al_get_display_width(window);
        final int displayHeightInDevice = LIB.al_get_display_height(window);
        final int displayWidth = this.pixelCoordsConverter.computeXSpanInOsPixel(displayWidthInDevice);
        final int displayHeight = this.pixelCoordsConverter.computeYSpanInOsPixel(displayHeightInDevice);
        // Works because we update display size on window resize.
        final int width = displayWidth + (left + right);
        final int height = displayHeight + (top + bottom);
        
        return GRect.valueOf(x, y, width, height);
    }

    /*
     * 
     */

    @Override
    protected void setClientBounds_raw(GRect targetClientBounds) {
        super.setClientBounds_raw(targetClientBounds);
    }

    @Override
    protected void setWindowBounds_raw(GRect targetWindowBounds) {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Pointer window = (Pointer) holder.getBackingWindow();
        
        final GRect insets = this.getInsets_raw();
        final int left = insets.x();
        final int top = insets.y();
        final int right = insets.xSpan();
        final int bottom = insets.ySpan();
        
        /*
         * TODO algr Can's set spans, just min/max constraints,
         * so we set them to desired spans, and then relax them
         * to allow for drag-induced resizes.
         */
        
        final int targetWidth = targetWindowBounds.xSpan() + (MUST_USE_CLIENT_SPANS_FOR_WINDOW_CONSTRAINTS ? - (left + right) : 0);
        final int targetHeight = targetWindowBounds.ySpan() + (MUST_USE_CLIENT_SPANS_FOR_WINDOW_CONSTRAINTS ? - (top + bottom) : 0);
        
        final int targetWidthInDevice = this.pixelCoordsConverter.computeXSpanInDevicePixel(targetWidth);
        final int targetHeightInDevice = this.pixelCoordsConverter.computeXSpanInDevicePixel(targetHeight);
        final boolean didIt1 = LIB.al_set_window_constraints(
                window,
                // Min spans.
                targetWidthInDevice,
                targetHeightInDevice,
                // Max spans.
                targetWidthInDevice,
                targetHeightInDevice);
        if (!didIt1) {
            throw new BindingError(
                    "could not set window size ("
                            + targetWidth
                            + ","
                            + targetHeight
                            + "): "
                            + LIB.al_get_errno());
        }
        
        // Not too large, for example Integer.MAX_VALUE seem to cause
        // some wrapping, since it seem to have the same effect than using zero.
        final int maxHugeConstraint = Integer.MAX_VALUE/2;
        
        final boolean didIt2 = LIB.al_set_window_constraints(
                window,
                // Min spans.
                0,
                0,
                // Max spans.
                maxHugeConstraint,
                maxHugeConstraint);
        if (!didIt2) {
            throw new BindingError(
                    "could not set window constraints (0,0,"
                            + maxHugeConstraint
                            + ","
                            + maxHugeConstraint
                            + "): "
                            + LIB.al_get_errno());
        }
        
        /*
         * TODO algr We set position last, so that when left is dragged
         * to the right, or top is dragged down, the window doesn't flash away
         * with its old large span still valid for an instant.
         * Could also cause a little bit less flickering if setting
         * X or Y position first when spans grow, but not worth the
         * code complication overhead.
         */
        
        LIB.al_set_window_position(
                window,
                targetWindowBounds.x(),
                targetWindowBounds.y());
        
        if (this.mustResizeDisplayExplicitlyOnBoundsSetting) {
            if (DEBUG) {
                Dbg.logPr(this, "setWindowBounds(...) : resizing display...");
            }
            /*
             * Will generate a DISPLAY_RESIZE event.
             */
            resizeDisplay(targetWidth, targetHeight);
        }
    }
}
