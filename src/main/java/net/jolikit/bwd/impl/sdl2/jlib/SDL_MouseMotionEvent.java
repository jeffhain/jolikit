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
 *  \brief Mouse motion event structure (event.motion.*)
 * typedef struct SDL_MouseMotionEvent
 * {
 *     Uint32 type;        // ::SDL_MOUSEMOTION
 *     Uint32 timestamp;
 *     Uint32 windowID;    // The window with mouse focus, if any
 *     Uint32 which;       // The mouse instance id, or SDL_TOUCH_MOUSEID
 *     Uint32 state;       // The current button state
 *     Sint32 x;           // X coordinate, relative to window
 *     Sint32 y;           // Y coordinate, relative to window
 *     Sint32 xrel;        // The relative motion in the X direction
 *     Sint32 yrel;        // The relative motion in the Y direction
 * } SDL_MouseMotionEvent;
 */
public class SDL_MouseMotionEvent extends Structure {
    public static class ByReference extends SDL_MouseMotionEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_MouseMotionEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public int windowID;
    public int which;
    public int state;
    public int x;
    public int y;
    public int xrel;
    public int yrel;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "windowID", "which", "state",
                "x", "y", "xrel", "yrel");
    }
}
