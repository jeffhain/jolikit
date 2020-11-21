/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.lwjgl3;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.OsUtils;

import org.lwjgl.glfw.GLFW;

public class LwjglHostBoundsHelper extends AbstractHostBoundsHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final GRect decorationInsets;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public LwjglHostBoundsHelper(
            InterfaceBackingWindowHolder holder,
            GRect decorationInsets) {
        super(holder);
        this.decorationInsets = LangUtils.requireNonNull(decorationInsets);
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
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final long window = (Long) holder.getBackingWindow();
        
        final int[] tmpX = new int[1];
        final int[] tmpY = new int[1];
        
        // In lwjgl, window means client area.
        GLFW.glfwGetWindowPos(window, tmpX, tmpY);
        final int x = tmpX[0];
        final int y = tmpY[0];
        
        GLFW.glfwGetWindowSize(window, tmpX, tmpY);
        final int width = tmpX[0];
        final int height = tmpY[0];
        
        if (false) {
            // In device pixels.
            GLFW.glfwGetFramebufferSize(window, tmpX, tmpY);
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
        final long window = (Long) holder.getBackingWindow();
        
        /*
         * TODO lwjgl When X span shrinks, which can correspond to left
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
            // In lwjgl, window means the client area.
            GLFW.glfwSetWindowPos(
                    window,
                    firstXPos,
                    firstYPos);
        }
        
        /*
         * 
         */
        
        // In lwjgl, window means client area.
        GLFW.glfwSetWindowSize(
                window,
                targetClientBounds.xSpan(),
                targetClientBounds.ySpan());
        
        /*
         * 
         */
        
        /*
         * TODO lwjgl On Mac, need to always reposition window here,
         * because spans modifications move top-left corner
         * and not bottom-right one.
         */
        if (OsUtils.isMac()
                || !(mustSetXPosFirst && mustSetYPosFirst)) {
            GLFW.glfwSetWindowPos(
                    window,
                    targetClientBounds.x(),
                    targetClientBounds.y());
        }
    }
    
    @Override
    protected void setWindowBounds_raw(GRect targetWindowBounds) {
        super.setWindowBounds_raw(targetWindowBounds);
    }
}
