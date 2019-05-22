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

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *  \brief  The structure that defines a display mode
 *
 *  \sa SDL_GetNumDisplayModes()
 *  \sa SDL_GetDisplayMode()
 *  \sa SDL_GetDesktopDisplayMode()
 *  \sa SDL_GetCurrentDisplayMode()
 *  \sa SDL_GetClosestDisplayMode()
 *  \sa SDL_SetWindowDisplayMode()
 *  \sa SDL_GetWindowDisplayMode()
 * typedef struct
 * {
 *     Uint32 format;              // pixel format
 *     int w;                      // width, in screen coordinates
 *     int h;                      // height, in screen coordinates
 *     int refresh_rate;           // refresh rate (or zero for unspecified)
 *     void *driverdata;           // driver-specific data, initialize to 0
 * } SDL_DisplayMode;
 */
public class SDL_DisplayMode extends Structure {
    public static class ByReference extends SDL_DisplayMode implements Structure.ByReference {
    }
    public static class ByValue extends SDL_DisplayMode implements Structure.ByValue {
    }
    public int format;
    public int w;
    public int h;
    public int refresh_rate;
    public Pointer driverdata;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("format", "w", "h", "refresh_rate", "driverdata");
    }
}
