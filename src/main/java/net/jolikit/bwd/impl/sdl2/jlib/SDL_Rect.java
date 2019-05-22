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
 *  \brief A rectangle, with the origin at the upper left.
 *
 *  \sa SDL_RectEmpty
 *  \sa SDL_RectEquals
 *  \sa SDL_HasIntersection
 *  \sa SDL_IntersectRect
 *  \sa SDL_UnionRect
 *  \sa SDL_EnclosePoints
 * typedef struct SDL_Rect
 * {
 *     int x, y;
 *     int w, h;
 * } SDL_Rect;
 */
public class SDL_Rect extends Structure {
    public static class ByReference extends SDL_Rect implements Structure.ByReference {
        public ByReference() {
        }
        public ByReference(int x, int y, int w, int h) {
            super(x, y, w, h);
        }
    }
    public static class ByValue extends SDL_Rect implements Structure.ByValue {
        public ByValue() {
        }
        public ByValue(int x, int y, int w, int h) {
            super(x, y, w, h);
        }
    }
    public int x;
    public int y;
    public int w;
    public int h;
    public SDL_Rect() {
    }
    public SDL_Rect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("x", "y", "w", "h");
    }
}
