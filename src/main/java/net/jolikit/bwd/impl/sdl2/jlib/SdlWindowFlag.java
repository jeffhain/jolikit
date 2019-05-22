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
package net.jolikit.bwd.impl.sdl2.jlib;

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

/**
 * enum SDL_WindowFlags
 * 
 * \brief The flags on a window
 *
 * \sa SDL_GetWindowFlags()
 */
public enum SdlWindowFlag implements InterfaceIntValued {
    SDL_WINDOW_FULLSCREEN(0x00000001),         // fullscreen window
    SDL_WINDOW_OPENGL(0x00000002),             // window usable with OpenGL context
    SDL_WINDOW_SHOWN(0x00000004),              // window is visible
    SDL_WINDOW_HIDDEN(0x00000008),             // window is not visible
    SDL_WINDOW_BORDERLESS(0x00000010),         // no window decoration
    SDL_WINDOW_RESIZABLE(0x00000020),          // window can be resized
    SDL_WINDOW_MINIMIZED(0x00000040),          // window is minimized
    SDL_WINDOW_MAXIMIZED(0x00000080),          // window is maximized
    SDL_WINDOW_INPUT_GRABBED(0x00000100),      // window has grabbed input focus
    SDL_WINDOW_INPUT_FOCUS(0x00000200),        // window has input focus
    SDL_WINDOW_MOUSE_FOCUS(0x00000400),        // window has mouse focus
    SDL_WINDOW_FULLSCREEN_DESKTOP(( 0x00000001 | 0x00001000 )),
    SDL_WINDOW_FOREIGN(0x00000800),            // window not created by SDL
    SDL_WINDOW_ALLOW_HIGHDPI(0x00002000),      // window should be created in high-DPI mode if supported
    SDL_WINDOW_MOUSE_CAPTURE(0x00004000);       // window has mouse captured (unrelated to INPUT_GRABBED)
    
    private static final IntValuedHelper<SdlWindowFlag> HELPER =
            new IntValuedHelper<SdlWindowFlag>(SdlWindowFlag.values());
    
    private final int intValue;
    
    private SdlWindowFlag(int intValue) {
        this.intValue = intValue;
    }
    
    @Override
    public int intValue() {
        return this.intValue;
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlWindowFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
