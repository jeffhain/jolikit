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
 * \brief The SDL keysym structure, used in key events.
 *
 * \note  If you are looking for translated character input, see the ::SDL_TEXTINPUT event.
 *
 * typedef struct SDL_Keysym
 * {
 *    SDL_Scancode scancode;      // < SDL physical key code - see ::SDL_Scancode for details
 *    SDL_Keycode sym;            // < SDL virtual key code - see ::SDL_Keycode for details
 *     Uint16 mod;                 // < current key modifiers
 *     Uint32 unused;
 * } SDL_Keysym;
 * 
 * "typedef Sint32 SDL_Keycode;"
 */
public class SDL_Keysym extends Structure {
    public static class ByReference extends SDL_Keysym implements Structure.ByReference {
    }
    public static class ByValue extends SDL_Keysym implements Structure.ByValue {
    }
    /**
     * SDL_Scancode ("physical key codes")
     * 
     * "Scancodes represent the physical position of the keys,
     * modeled after a standard QWERTY keyboard,
     * while Keycodes are the character obtained by pressing the key.
     * On an AZERTY keyboard, pressing A will emit a 'Q' scancode and an 'a' keycode."
     */
    public int scancode;
    /**
     * SDL_Keycode ("virtual key codes")
     */
    public int sym;
    public short mod;
    public int unused;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("scancode", "sym", "mod", "unused");
    }
}
