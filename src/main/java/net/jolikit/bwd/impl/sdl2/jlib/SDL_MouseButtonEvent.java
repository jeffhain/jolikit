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

import com.sun.jna.Structure;

/**
 *  \brief Mouse button event structure (event.button.*)
 * typedef struct SDL_MouseButtonEvent
 * {
 *     Uint32 type;        // ::SDL_MOUSEBUTTONDOWN or ::SDL_MOUSEBUTTONUP
 *     Uint32 timestamp;
 *     Uint32 windowID;    // The window with mouse focus, if any
 *     Uint32 which;       // The mouse instance id, or SDL_TOUCH_MOUSEID
 *     Uint8 button;       // The mouse button index
 *     Uint8 state;        // ::SDL_PRESSED or ::SDL_RELEASED
 *     Uint8 clicks;       // 1 for single-click, 2 for double-click, etc.
 *     Uint8 padding1;
 *     Sint32 x;           // X coordinate, relative to window
 *     Sint32 y;           // Y coordinate, relative to window
 * } SDL_MouseButtonEvent;
 */
public class SDL_MouseButtonEvent extends Structure {
    public static class ByReference extends SDL_MouseButtonEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_MouseButtonEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public int windowID;
    public int which;
    public byte button;
    public byte state;
    public byte clicks;
    public byte padding1;
    public int x;
    public int y;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "windowID", "which",
                "button", "state", "clicks", "padding1",
                "x", "y");
    }
}
