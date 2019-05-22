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
 * \brief A collection of pixels used in software blitting.
 *
 * \note  This structure should be treated as read-only, except for \c pixels,
 *        which, if not NULL, contains the raw pixel data for the surface.
 * 
 * typedef struct SDL_Surface
 * {
 *     Uint32 flags;               / **< Read-only * /
 *     SDL_PixelFormat *format;    / **< Read-only * /
 *     int w, h;                   / **< Read-only * /
 *     int pitch;                  / **< Read-only * /
 *     void *pixels;               / **< Read-write * /
 * 
 *     / ** Application data associated with the surface * /
 *     void *userdata;             / **< Read-write * /
 * 
 *     / ** information needed for surfaces requiring locks * /
 *     int locked;                 / **< Read-only * /
 *     void *lock_data;            / **< Read-only * /
 * 
 *     / ** clipping information * /
 *     SDL_Rect clip_rect;         / **< Read-only * /
 * 
 *     / ** info for fast blit mapping to other surfaces * /
 *     struct SDL_BlitMap *map;    / **< Private * /
 * 
 *     / ** Reference count -- used when freeing surface * /
 *     int refcount;               / **< Read-mostly * /
 * } SDL_Surface;
 */
public class SDL_Surface extends Structure {
    public static class ByReference extends SDL_Surface implements Structure.ByReference {
        public ByReference() {
        }
        public ByReference(Pointer pointer) {
            super(pointer);
        }
    }
    public static class ByValue extends SDL_Surface implements Structure.ByValue {
        public ByValue() {
        }
        public ByValue(Pointer pointer) {
            super(pointer);
        }
    }
    public SDL_Surface() {
    }
    public SDL_Surface(Pointer pointer) {
        super(pointer);
    }
    public int flags;
    public SDL_PixelFormat.ByReference format;
    public int w;
    public int h;
    public int pitch;
    public Pointer pixels;
    public Pointer userdata;
    public int locked;
    public Pointer lock_data;
    public SDL_Rect.ByValue clip_rect;
    public Pointer map;
    public int refcount;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "flags", "format", "w", "h",
                "pitch", "pixels", "userdata",
                "locked", "lock_data",
                "clip_rect", "map", "refcount");
    }
}
