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
 *  \note Everything in the pixel format structure is read-only.
 * typedef struct SDL_PixelFormat
 * {
 *     Uint32 format;
 *     SDL_Palette *palette;
 *     Uint8 BitsPerPixel;
 *     Uint8 BytesPerPixel;
 *     Uint8 padding[2];
 *     Uint32 Rmask;
 *     Uint32 Gmask;
 *     Uint32 Bmask;
 *     Uint32 Amask;
 *     Uint8 Rloss;
 *     Uint8 Gloss;
 *     Uint8 Bloss;
 *     Uint8 Aloss;
 *     Uint8 Rshift;
 *     Uint8 Gshift;
 *     Uint8 Bshift;
 *     Uint8 Ashift;
 *     int refcount;
 *     struct SDL_PixelFormat *next;
 * } SDL_PixelFormat;
 */
public class SDL_PixelFormat extends Structure {
    public static class ByReference extends SDL_PixelFormat implements Structure.ByReference {
    }
    public static class ByValue extends SDL_PixelFormat implements Structure.ByValue {
    }
    /**
     * @see SdlPixelFormat
     */
    public int format;
    public SDL_Palette.ByReference palette;
    public byte BitsPerPixel;
    public byte BytesPerPixel;
    public byte padding1;
    public byte padding2;
    public int Rmask;
    public int Gmask;
    public int Bmask;
    public int Amask;
    public byte Rloss;
    public byte Gloss;
    public byte Bloss;
    public byte Aloss;
    public byte Rshift;
    public byte Gshift;
    public byte Bshift;
    public byte Ashift;
    public int refcount;
    /**
     * Using Pointer, else got java.lang.StackOverflowError.
     */
    public Pointer next;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "format", "palette", "BitsPerPixel", "BytesPerPixel",
                "padding1", "padding2",
                "Rmask", "Gmask", "Bmask", "Amask",
                "Rloss", "Gloss", "Bloss", "Aloss",
                "Rshift", "Gshift", "Bshift", "Ashift",
                "refcount", "next");
    }
}
