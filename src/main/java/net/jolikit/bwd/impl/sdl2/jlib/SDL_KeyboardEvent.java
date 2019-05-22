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
 * \brief Keyboard button event structure (event.key.*)
 * typedef struct SDL_KeyboardEvent
 * {
 *     Uint32 type;        // ::SDL_KEYDOWN or ::SDL_KEYUP
 *     Uint32 timestamp;
 *     Uint32 windowID;    // The window with keyboard focus, if any
 *     Uint8 state;        // ::SDL_PRESSED or ::SDL_RELEASED
 *     Uint8 repeat;       // Non-zero if this is a key repeat
 *     Uint8 padding2;
 *     Uint8 padding3;
 *     SDL_Keysym keysym;  // The key that was pressed or released
 * } SDL_KeyboardEvent;
 * 
 * "The type and state actually report the same information,
 * they just use different values to do it!"
 */
public class SDL_KeyboardEvent extends Structure {
    public static class ByReference extends SDL_KeyboardEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_KeyboardEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public int windowID;
    public byte state;
    public byte repeat;
    public byte padding2;
    public byte padding3;
    public SDL_Keysym keysym;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "windowID",
                "state", "repeat", "padding2", "padding3",
                "keysym");
    }
}
