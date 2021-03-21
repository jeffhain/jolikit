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
package net.jolikit.bwd.impl.qtj4;

import com.trolltech.qt.core.QRect;
import com.trolltech.qt.gui.QContentsMargins;
import com.trolltech.qt.gui.QWidget;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;

/**
 * Works for either QFrame or QDialog windows.
 */
public class QtjHostBoundsHelper extends AbstractHostBoundsHelper {
    
    /*
     * Qt positions and dimensions
     * (cf. http://doc.qt.io/qt-4.8/application-windows.html):
     * 
     * For a window:
     * - window top-left position in screen:
     *   - retrieval:
     *     - (x(), y())
     *     - (pos().x(), pos(),y())
     *     - (frameGeometry().x(), frameGeometry().y())
     *   - setting:
     *     - move(x, y)
     * - window dimensions:
     *   - retrieval:
     *     - (frameGeometry().width(), frameGeometry().height())
     * - content position in screen:
     *   - retrieval:
     *     - (geometry().x(), geometry().y())
     * - content dimensions:
     *   - retrieval:
     *     - (width(), height())
     *     - (geometry().width(), geometry().height())
     * 
     * For a widget:
     * - top-left position in parent:
     *   - retrieval:
     *     - (x(), y())
     *     - (pos().x(), pos(),y())
     * - dimensions:
     *   - retrieval:
     *     - (width(), height())
     */
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjHostBoundsHelper(
        InterfaceBackingWindowHolder holder) {
        super(holder);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected GRect getInsetsDecorated_rawInOs() {
        if (false) {
            /*
             * TODO qtj Doesn't work, returns (0,0,0,0).
             */
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final QWidget window = (QWidget) holder.getBackingWindow();
            final QContentsMargins backingInsets = window.getContentsMargins();
            return GRect.valueOf(
                    backingInsets.left,
                    backingInsets.top,
                    backingInsets.right,
                    backingInsets.bottom);
        } else {
            return super.getInsetsDecorated_rawInOs();
        }
    }

    /*
     * 
     */
    
    @Override
    protected GRect getClientBounds_rawInOs() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final QWidget window = (QWidget) holder.getBackingWindow();
        /*
         * TODO qtj Can have bounds with zero spans, typically with
         * (-32000,-32000) position, when was/is (?) iconified.
         */
        final QRect rect = window.geometry();
        return QtjUtils.toGRect(rect);
    }

    @Override
    protected GRect getWindowBounds_rawInOs() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final QWidget window = (QWidget) holder.getBackingWindow();
        final QRect rect = window.frameGeometry();
        return QtjUtils.toGRect(rect);
    }

    /*
     * 
     */
    
    @Override
    protected void setClientBounds_rawInOs(GRect targetClientBounds) {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final QWidget window = (QWidget) holder.getBackingWindow();
        
        window.setGeometry(
                targetClientBounds.x(),
                targetClientBounds.y(),
                targetClientBounds.xSpan(),
                targetClientBounds.ySpan());
    }

    @Override
    protected void setWindowBounds_rawInOs(GRect targetWindowBounds) {
        if (false) {
            /*
             * TODO qtj Doesn't work, because setFixedSize(...)
             * sets client spans, not window spans.
             */
            
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final QWidget window = (QWidget) holder.getBackingWindow();
            
            /*
             * TODO qtj When X span shrinks, which can correspond to left
             * being dragged to the right, we set position last, so that the window
             * doesn't flash away with its old large span still valid for an instant,
             * and when X span grows, which can correspond to left being
             * dragged to the left, we set position fist, for the same reason.
             * Same for Y.
             * NB: Could also just delegate to super implementation.
             */
            
            final GRect oldWindowBounds = this.getWindowBounds_rawInOs();
            final boolean mustSetXPosFirst = (targetWindowBounds.xSpan() > oldWindowBounds.xSpan());
            final boolean mustSetYPosFirst = (targetWindowBounds.ySpan() > oldWindowBounds.ySpan());
            if (mustSetXPosFirst || mustSetYPosFirst) {
                final int firstXPos = (mustSetXPosFirst ? targetWindowBounds.x() : oldWindowBounds.x());
                final int firstYPos = (mustSetYPosFirst ? targetWindowBounds.y() : oldWindowBounds.y());
                window.move(
                        firstXPos,
                        firstYPos);
            }
            
            /*
             * 
             */
            
            window.setFixedSize(
                    targetWindowBounds.xSpan(),
                    targetWindowBounds.ySpan());

            /*
             * 
             */
            
            if (!(mustSetXPosFirst && mustSetYPosFirst)) {
                window.move(
                        targetWindowBounds.x(),
                        targetWindowBounds.y());
            }
        } else {
            super.setWindowBounds_rawInOs(targetWindowBounds);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * To help make sense of it all.
     */
    private void logBackingCoords() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final QWidget window = (QWidget) holder.getBackingWindow();
        
        logWithHostPrefix("window.getContentsMargins().left = " + window.getContentsMargins().left);
        logWithHostPrefix("window.getContentsMargins().top = " + window.getContentsMargins().top);
        logWithHostPrefix("window.getContentsMargins().right = " + window.getContentsMargins().right);
        logWithHostPrefix("window.getContentsMargins().bottom = " + window.getContentsMargins().bottom);
        logWithHostPrefix("window.x() = " + window.x());
        logWithHostPrefix("window.y() = " + window.y());
        logWithHostPrefix("window.pos().x() = " + window.pos().x());
        logWithHostPrefix("window.pos().y() = " + window.pos().y());
        logWithHostPrefix("window.width() = " + window.width());
        logWithHostPrefix("window.height() = " + window.height());
        logWithHostPrefix("window.widthMM() = " + window.widthMM());
        logWithHostPrefix("window.heightMM() = " + window.heightMM());
        logWithHostPrefix("window.geometry().x() = " + window.geometry().x());
        logWithHostPrefix("window.geometry().y() = " + window.geometry().y());
        logWithHostPrefix("window.geometry().width() = " + window.geometry().width());
        logWithHostPrefix("window.geometry().height() = " + window.geometry().height());
        logWithHostPrefix("window.frameGeometry().x() = " + window.frameGeometry().x());
        logWithHostPrefix("window.frameGeometry().y() = " + window.frameGeometry().y());
        logWithHostPrefix("window.frameGeometry().width() = " + window.frameGeometry().width());
        logWithHostPrefix("window.frameGeometry().height() = " + window.frameGeometry().height());
    }
}
