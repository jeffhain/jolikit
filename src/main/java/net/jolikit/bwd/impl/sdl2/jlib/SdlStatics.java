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

public class SdlStatics {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Constants defined here to avoid cyclic dependency.
     */
    
    private static final int SDL_RLEACCEL = 0x00000002;
    
    private static final int SDLK_SCANCODE_MASK = (1<<30);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static int SDL_SCANCODE_TO_KEYCODE(int X) {
        return X | SDLK_SCANCODE_MASK;
    }
    
    /**
     * #define SDL_FOURCC(A, B, C, D) \
     *     ((SDL_static_cast(Uint32, SDL_static_cast(Uint8, (A))) << 0) | \
     *      (SDL_static_cast(Uint32, SDL_static_cast(Uint8, (B))) << 8) | \
     *      (SDL_static_cast(Uint32, SDL_static_cast(Uint8, (C))) << 16) | \
     *      (SDL_static_cast(Uint32, SDL_static_cast(Uint8, (D))) << 24))
     */
    public static int SDL_FOURCC(int A, int B, int C, int D) {
        return A | (B << 8) | (C << 16) | (D << 24);
    }

    /**
     * #define SDL_DEFINE_PIXELFOURCC(A, B, C, D) SDL_FOURCC(A, B, C, D)
     */
    public static int SDL_DEFINE_PIXELFOURCC(int A, int B, int C, int D) {
        return SDL_FOURCC(A, B, C, D);
    }
    
    /**
     * #define SDL_DEFINE_PIXELFORMAT(type, order, layout, bits, bytes) \
     *    ((1 << 28) | ((type) << 24) | ((order) << 20) | ((layout) << 16) | \
     *    ((bits) << 8) | ((bytes) << 0))
     */
    public static int SDL_DEFINE_PIXELFORMAT(int type, int order, int layout, int bits, int bytes) {
        return ((1 << 28)
                | (type << 24)
                | (order << 20)
                | (layout << 16)
                | (bits << 8)
                | (bytes << 0));
    }
    
    /**
     * #define SDL_PIXELFLAG(X)    (((X) >> 28) & 0x0F)
     */
    public static int SDL_PIXELFLAG(int X) {
        return ((X >> 28) & 0x0F);
    }
    
    /**
     * #define SDL_PIXELTYPE(X)    (((X) >> 24) & 0x0F)
     */
    public static int SDL_PIXELTYPE(int X) {
        return ((X >> 24) & 0x0F);
    }
    
    /**
     * #define SDL_PIXELORDER(X)   (((X) >> 20) & 0x0F)
     */
    public static int SDL_PIXELORDER(int X) {
        return ((X >> 20) & 0x0F);
    }

    /**
     * #define SDL_PIXELLAYOUT(X)  (((X) >> 16) & 0x0F)
     */
    public static int SDL_PIXELLAYOUT(int X) {
        return ((X >> 16) & 0x0F);
    }

    /**
     * #define SDL_BITSPERPIXEL(X) (((X) >> 8) & 0xFF)
     */
    public static int SDL_BITSPERPIXEL(int X) {
        return ((X >> 8) & 0xFF);
    }

    /**
     * / * The flag is set to 1 because 0x1? is not in the printable ASCII range * /
     * #define SDL_ISPIXELFORMAT_FOURCC(format)    \
     *    ((format) && (SDL_PIXELFLAG(format) != 1))
     */
    public static boolean SDL_ISPIXELFORMAT_FOURCC(int format) {
        // In C language, anything != 0 is true (if I recall).
        return (format != 0) && (SdlStatics.SDL_PIXELFLAG(format) != 1);
    }

    /**
     * #define SDL_ISPIXELFORMAT_INDEXED(format)   \
     *   (!SDL_ISPIXELFORMAT_FOURCC(format) && \
     *    ((SDL_PIXELTYPE(format) == SDL_PIXELTYPE_INDEX1) || \
     *     (SDL_PIXELTYPE(format) == SDL_PIXELTYPE_INDEX4) || \
     *     (SDL_PIXELTYPE(format) == SDL_PIXELTYPE_INDEX8)))
     */
    public static boolean SDL_ISPIXELFORMAT_INDEXED(int format) {
        return (!SDL_ISPIXELFORMAT_FOURCC(format) &&
                ((SDL_PIXELTYPE(format) == SdlPixelType.SDL_PIXELTYPE_INDEX1.intValue()) ||
                        (SDL_PIXELTYPE(format) == SdlPixelType.SDL_PIXELTYPE_INDEX4.intValue()) ||
                        (SDL_PIXELTYPE(format) == SdlPixelType.SDL_PIXELTYPE_INDEX8.intValue())));
    }
    
    /**
     * Evaluates to true if the surface needs to be locked before access.
     * 
     * #define SDL_MUSTLOCK(S) (((S)->flags & SDL_RLEACCEL) != 0)
     */
    public static boolean SDL_MUSTLOCK(SDL_Surface surface) {
        return (surface.flags & SDL_RLEACCEL) != 0;
    }
    
    /**
     *  Used as a mask when testing buttons in buttonstate.
     *   - Button 1:  Left mouse button
     *   - Button 2:  Middle mouse button
     *   - Button 3:  Right mouse button
     * 
     * #define SDL_BUTTON(X)       (1 << ((X)-1))
     */
    public static int SDL_BUTTON(int X) {
        return (1 << (X - 1));
    }
}
