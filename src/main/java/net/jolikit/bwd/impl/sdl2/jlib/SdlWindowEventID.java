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
 * enum SDL_WindowEventID
 * 
 *  \brief Event subtype for window events
 */
public enum SdlWindowEventID implements InterfaceIntValued {
    SDL_WINDOWEVENT_NONE,           // Never used
    SDL_WINDOWEVENT_SHOWN,          // Window has been shown
    SDL_WINDOWEVENT_HIDDEN,         // Window has been hidden
    SDL_WINDOWEVENT_EXPOSED,        // Window has been exposed and should be redrawn
    SDL_WINDOWEVENT_MOVED,          // Window has been moved to data1, data2
    SDL_WINDOWEVENT_RESIZED,        // Window has been resized to data1xdata2
    SDL_WINDOWEVENT_SIZE_CHANGED,   // The window size has changed, either as a result of an API call or through the system or user changing the window size.
    SDL_WINDOWEVENT_MINIMIZED,      // Window has been minimized
    SDL_WINDOWEVENT_MAXIMIZED,      // Window has been maximized
    SDL_WINDOWEVENT_RESTORED,       // Window has been restored to normal size and position
    SDL_WINDOWEVENT_ENTER,          // Window has gained mouse focus
    SDL_WINDOWEVENT_LEAVE,          // Window has lost mouse focus
    SDL_WINDOWEVENT_FOCUS_GAINED,   // Window has gained keyboard focus
    SDL_WINDOWEVENT_FOCUS_LOST,     // Window has lost keyboard focus
    SDL_WINDOWEVENT_CLOSE;           // The window manager requests that the window be closed

    private static final IntValuedHelper<SdlWindowEventID> HELPER =
            new IntValuedHelper<SdlWindowEventID>(SdlWindowEventID.values());

    @Override
    public int intValue() {
        return this.ordinal();
    }

    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlWindowEventID valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
