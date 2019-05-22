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
package net.jolikit.bwd.impl.jogl;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;
import net.jolikit.lang.Dbg;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.newt.opengl.GLWindow;

public class JoglHostBoundsHelper extends AbstractHostBoundsHelper {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * TODO jogl At some point had lag on client/window bounds,
     * and scale issues on resize, or maximizing/demaximizing,
     * and doing this seemed to fix the issue.
     * Can no longer reproduce the issue (possibly due to a threading rework),
     * but letting this workaround activated, since it doesn't seem to hurt.
     */
    private static final boolean MUST_RETURN_LAST_RESHAPED_CLIENT_SPANS = true;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private int lastReshapedClientWidth;
    private int lastReshapedClientHeight;
    public void setLastReshapedClientSpans(
            int lastReshapedClientWidth,
            int lastReshapedClientHeight) {
        if (DEBUG) {
            Dbg.log(getHolder().getTitle() + " : lastReshapedClientWidth = " + lastReshapedClientWidth);
            Dbg.log(getHolder().getTitle() + " : lastReshapedClientHeight = " + lastReshapedClientHeight);
        }
        this.lastReshapedClientWidth = lastReshapedClientWidth;
        this.lastReshapedClientHeight = lastReshapedClientHeight;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JoglHostBoundsHelper(InterfaceBackingWindowHolder holder) {
        super(holder);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected GRect getInsetsDecorated_raw() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final GLWindow window = (GLWindow) holder.getBackingWindow();

        final InsetsImmutable insets = window.getInsets();

        final int left = insets.getLeftWidth();
        final int top = insets.getTopHeight();
        final int right = Math.max(0, insets.getTotalWidth() - insets.getLeftWidth());
        final int bottom = Math.max(0, insets.getTotalHeight() - insets.getTopHeight());
        return GRect.valueOf(left, top, right, bottom);
    }

    /*
     * 
     */

    @Override
    protected GRect getClientBounds_raw() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final GLWindow window = (GLWindow) holder.getBackingWindow();
        
        // In jogl, window means the client area.
        final Rectangle rectangle = window.getBounds();
        final int x = rectangle.getX();
        final int y = rectangle.getY();
        final int width;
        final int height;
        if (MUST_RETURN_LAST_RESHAPED_CLIENT_SPANS) {
            width = this.lastReshapedClientWidth;
            height = this.lastReshapedClientHeight;
        } else {
            width = rectangle.getWidth();
            height = rectangle.getHeight();
        }
        
        return GRect.valueOf(x, y, width, height);
    }
    
    @Override
    protected GRect getWindowBounds_raw() {
        return super.getWindowBounds_raw();
    }

    /*
     * 
     */
    
    @Override
    protected void setClientBounds_raw(GRect targetClientBounds) {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final GLWindow window = (GLWindow) holder.getBackingWindow();
        
        /*
         * TODO jogl When X span shrinks, which can correspond to left
         * being dragged to the right, we set position last, so that the window
         * doesn't flash away with its old large span still valid for an instant,
         * and when X span grows, which can correspond to left being
         * dragged to the left, we set position fist, for the same reason.
         * Same for Y.
         */
        
        final GRect oldClientBounds = this.getClientBounds_raw();
        final boolean mustSetXPosFirst = (targetClientBounds.xSpan() > oldClientBounds.xSpan());
        final boolean mustSetYPosFirst = (targetClientBounds.ySpan() > oldClientBounds.ySpan());
        if (mustSetXPosFirst || mustSetYPosFirst) {
            final int firstXPos = (mustSetXPosFirst ? targetClientBounds.x() : oldClientBounds.x());
            final int firstYPos = (mustSetYPosFirst ? targetClientBounds.y() : oldClientBounds.y());
            // In jogl, window means the client area.
            window.setPosition(
                    firstXPos,
                    firstYPos);
        }
        
        /*
         * TODO jogl Sometimes, synchronously during this call,
         * GLEventListener.display, reshape, and then display again twice,
         * get called.
         * We could optimize a bit by ignoring some,
         * like first two displays, but that could be hazardous
         * in case less calls are done.
         */
        
        // In jogl, window means the client area.
        window.setSize(
                targetClientBounds.xSpan(),
                targetClientBounds.ySpan());
        
        /*
         * 
         */
        
        if (!(mustSetXPosFirst && mustSetYPosFirst)) {
            window.setPosition(
                    targetClientBounds.x(),
                    targetClientBounds.y());
        }
    }
    
    @Override
    protected void setWindowBounds_raw(GRect targetWindowBounds) {
        super.setWindowBounds_raw(targetWindowBounds);
    }
}
