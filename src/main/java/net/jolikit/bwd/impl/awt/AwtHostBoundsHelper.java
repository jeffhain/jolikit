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
package net.jolikit.bwd.impl.awt;

import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;

public class AwtHostBoundsHelper extends AbstractHostBoundsHelper {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AwtHostBoundsHelper(InterfaceBackingWindowHolder holder) {
        super(holder);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected GRect getInsetsDecorated_raw() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Window window = (Window) holder.getBackingWindow();
        final Insets backingInsets = window.getInsets();
        return GRect.valueOf(
                backingInsets.left,
                backingInsets.top,
                backingInsets.right,
                backingInsets.bottom);
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
        final Window window = (Window) holder.getBackingWindow();
        
        final Point posInScreen;
        if (!window.isShowing()) {
            return GRect.DEFAULT_EMPTY;
        } else {
            try {
                posInScreen = window.getLocationOnScreen();
            } catch (IllegalComponentStateException e) {
                /*
                 * Can happen with
                 * "component must be showing on the screen to determine its location",
                 * if we are ever called when window is not showing,
                 * which might still happen in spite of the above check
                 * (window state management often being messy).
                 */
                return GRect.DEFAULT_EMPTY;
            }
        }
        
        final GRect windowBounds = GRect.valueOf(
                posInScreen.x,
                posInScreen.y,
                window.getWidth(),
                window.getHeight());
        
        return windowBounds;
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
        final Window window = (Window) holder.getBackingWindow();
        
        window.setBounds(
                targetWindowBounds.x(),
                targetWindowBounds.y(),
                targetWindowBounds.xSpan(),
                targetWindowBounds.ySpan());
    }
}
