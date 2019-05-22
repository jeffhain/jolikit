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
 * typedef struct SDL_Color
 * {
 *     Uint8 r;
 *     Uint8 g;
 *     Uint8 b;
 *     Uint8 a;
 * } SDL_Color;
 * #define SDL_Colour SDL_Color
 */
public class SDL_Color extends Structure {
    public static class ByReference extends SDL_Color implements Structure.ByReference {
    }
    public static class ByValue extends SDL_Color implements Structure.ByValue {
    }
    public byte r;
    public byte g;
    public byte b;
    public byte a;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("r", "g", "b", "a");
    }
}
