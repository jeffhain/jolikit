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
 *  \brief  The structure that defines a point
 *
 *  \sa SDL_EnclosePoints
 *  \sa SDL_PointInRect
 * typedef struct SDL_Point
 * {
 *     int x;
 *     int y;
 * } SDL_Point;
 */
public class SDL_Point extends Structure {
    public static class ByReference extends SDL_Point implements Structure.ByReference {
    }
    public static class ByValue extends SDL_Point implements Structure.ByValue {
    }
    public int x;
    public int y;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("x", "y");
    }
}
