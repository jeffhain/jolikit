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
package net.jolikit.bwd.impl.sdl2;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.utils.AbstractHostBoundsHelper;
import net.jolikit.bwd.impl.utils.InterfaceBackingWindowHolder;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.OsUtils;

public class SdlHostBoundsHelper extends AbstractHostBoundsHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    
    private final GRect decorationInsets;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SdlHostBoundsHelper(
            InterfaceBackingWindowHolder holder,
            GRect decorationInsets) {
        super(holder);
        this.decorationInsets = LangUtils.requireNonNull(decorationInsets);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected GRect getInsetsDecorated_rawInOs() {
        final GRect insets;
        if (false) {
            final IntByReference top = new IntByReference();
            final IntByReference left = new IntByReference();
            final IntByReference bottom = new IntByReference();
            final IntByReference right = new IntByReference();
            final InterfaceBackingWindowHolder holder = this.getHolder();
            final Pointer window = (Pointer) holder.getBackingWindow();
            /*
             * TODO sdl Might not succeed (and on Windows, it doesn't),
             * so we never use it, for consistently depending on
             * configured value.
             * Maybe will work when in stable version.
             */
            final int ret = LIB.SDL_GetWindowBordersSize(window, top, left, bottom, right);
            final boolean success = (ret == 0);
            if (success) {
                insets = GRect.valueOf(
                        left.getValue(),
                        top.getValue(),
                        right.getValue(),
                        bottom.getValue());
            } else {
                insets = this.decorationInsets;
            }
        } else {
            insets = this.decorationInsets;
        }
        return insets;
    }

    /*
     * 
     */
    
    @Override
    protected GRect getClientBounds_rawInOs() {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Pointer window = (Pointer) holder.getBackingWindow();
        
        final IntByReference tmpXRef = new IntByReference();
        final IntByReference tmpYRef = new IntByReference();
        
        // In sdl, window means the client area.
        LIB.SDL_GetWindowPosition(window, tmpXRef, tmpYRef);
        final int x = tmpXRef.getValue();
        final int y = tmpYRef.getValue();
        
        LIB.SDL_GetWindowSize(window, tmpXRef, tmpYRef);
        final int width = tmpXRef.getValue();
        final int height = tmpYRef.getValue();

        return GRect.valueOf(x, y, width, height);
    }

    @Override
    protected GRect getWindowBounds_rawInOs() {
        return super.getWindowBounds_rawInOs();
    }

    /*
     * 
     */
    
    @Override
    protected void setClientBounds_rawInOs(GRect targetClientBounds) {
        final InterfaceBackingWindowHolder holder = this.getHolder();
        final Pointer window = (Pointer) holder.getBackingWindow();
        
        /*
         * NB: There is apparently no window flickering out of bounds,
         * when moving left border to the right or top border to bottom,
         * and setting new position first and then new spans.
         * Else, would need to set position last in these cases.
         */
        
        // In sdl, window means the client area.
        LIB.SDL_SetWindowPosition(
                window,
                targetClientBounds.x(),
                targetClientBounds.y());
        
        final int newWidth = targetClientBounds.xSpan();
        final int newHeight = targetClientBounds.ySpan();
        
        /*
         * TODO sdl On Mac, if we don't shake the window like this,
         * setting size twice in a row with different values,
         * we have trouble getting this bounds setting to work
         * (easy to see with bounds grips test case).
         */
        if (OsUtils.isMac()) {
            LIB.SDL_SetWindowSize(
                    window,
                    newWidth,
                    newHeight + 1);
        }
        
        // In sdl, window means the client area.
        LIB.SDL_SetWindowSize(
                window,
                newWidth,
                newHeight);
    }
    
    @Override
    protected void setWindowBounds_rawInOs(GRect targetWindowBounds) {
        super.setWindowBounds_rawInOs(targetWindowBounds);
    }
}
