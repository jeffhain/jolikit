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
package net.jolikit.bwd.impl.swt;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

public class SwtHostBoundsHelper extends AbstractHostBoundsHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final GRect undecoratedInsets;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SwtHostBoundsHelper(
            InterfaceBackingWindowHolder holder,
            GRect undecoratedInsets) {
        super(holder);
        this.undecoratedInsets = LangUtils.requireNonNull(undecoratedInsets);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected GRect getInsetsUndecorated_raw() {
        return this.undecoratedInsets;
    }

    @Override
    protected GRect getInsetsDecorated_raw() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Shell window = (Shell) holder.getBackingWindow();
        return computeInsetsDecorated(window);
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
        final Shell window = (Shell) holder.getBackingWindow();
        
        final Rectangle windowRectangle = window.getBounds();
        return SwtUtils.toGRect(windowRectangle);
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
        final Shell window = (Shell) holder.getBackingWindow();
        
        /*
         * TODO swt When X span shrinks, which can correspond to left
         * being dragged to the right, we set position last, so that the window
         * doesn't flash away with its old large span still valid for an instant,
         * and when X span grows, which can correspond to left being
         * dragged to the left, we set position fist, for the same reason.
         * Same for Y.
         */
        
        final GRect oldWindowBounds = this.getWindowBounds_raw();
        final boolean mustSetXPosFirst = (targetWindowBounds.xSpan() > oldWindowBounds.xSpan());
        final boolean mustSetYPosFirst = (targetWindowBounds.ySpan() > oldWindowBounds.ySpan());
        if (mustSetXPosFirst || mustSetYPosFirst) {
            final int firstXPos = (mustSetXPosFirst ? targetWindowBounds.x() : oldWindowBounds.x());
            final int firstYPos = (mustSetYPosFirst ? targetWindowBounds.y() : oldWindowBounds.y());
            window.setLocation(
                    firstXPos,
                    firstYPos);
        }
        
        /*
         * 
         */
        
        window.setSize(
                targetWindowBounds.xSpan(),
                targetWindowBounds.ySpan());
        
        /*
         * 
         */
        
        if (!(mustSetXPosFirst && mustSetYPosFirst)) {
            window.setLocation(
                    targetWindowBounds.x(),
                    targetWindowBounds.y());
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static GRect computeInsetsDecorated(Shell window) {
        /*
         * TODO swt We don't have the location of client area
         * within the window, so we suppose it's centered along x,
         * and that the bottom border is the same as left and right borders.
         */

        final Rectangle windowRectangle = window.getBounds();
        // Its (x,y) is always (0,0).
        final Rectangle clientAreaRectangle = window.getClientArea();
        
        // Hopefully dxSpan is even, else means left and right borders
        // have different spans.
        final int dxSpan = windowRectangle.width - clientAreaRectangle.width;
        final int dySpan = windowRectangle.height - clientAreaRectangle.height;
        
        /*
         * TODO swt During host unit test, step 72:
         * "Exception in thread "main" net.jolikit.bwd.impl.utils.BindingError:
         * window = Rectangle {0, 23, 1280, 752}, client area = Rectangle {0, 0, 157, 54}"
         * But we prefer to be tolerant.
         */
        final boolean mustThrowIfWeird = false;
        if (mustThrowIfWeird
                && NumbersUtils.isOdd(dxSpan)) {
            throw new BindingError("window = " + windowRectangle + ", client area = " + clientAreaRectangle);
        }
        
        int leftRightBottom = dxSpan / 2;
        int top = dySpan - leftRightBottom;
        if ((leftRightBottom < 0)
                || (top < 0)) {
            // Should not happen.
            if (mustThrowIfWeird) {
                throw new BindingError("window = " + windowRectangle + ", client area = " + clientAreaRectangle);
            } else {
                leftRightBottom = Math.max(leftRightBottom, 0);
                top = Math.max(top, 0);
            }
        }
        
        return GRect.valueOf(
                leftRightBottom,
                top,
                leftRightBottom,
                leftRightBottom);
    }
}
