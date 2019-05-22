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
 *  \brief Mouse wheel event structure (event.wheel.*)
 * typedef struct SDL_MouseWheelEvent
 * {
 *     Uint32 type;        // ::SDL_MOUSEWHEEL
 *     Uint32 timestamp;
 *     Uint32 windowID;    // The window with mouse focus, if any
 *     Uint32 which;       // The mouse instance id, or SDL_TOUCH_MOUSEID
 *     Sint32 x;           // The amount scrolled horizontally, positive to the right and negative to the left
 *     Sint32 y;           // The amount scrolled vertically, positive away from the user and negative toward the user
 *     Uint32 direction;   // Set to one of the SDL_MOUSEWHEEL_* defines. When FLIPPED the values in X and Y will be opposite. Multiply by -1 to change them back
 * } SDL_MouseWheelEvent;
 */
public class SDL_MouseWheelEvent extends Structure {
    public static class ByReference extends SDL_MouseWheelEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_MouseWheelEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public int windowID;
    public int which;
    public int x;
    public int y;
    public int direction;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "windowID", "which",
                "x", "y", "direction");
    }
}
