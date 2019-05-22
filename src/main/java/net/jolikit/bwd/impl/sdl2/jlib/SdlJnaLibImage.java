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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Java binding for (a subset of) SDL2_image.
 * 
 * Uses SDL_SetError and SDL_GetError from SDL library.
 * 
 * https://www.libsdl.org/projects/SDL_image/
 */
public interface SdlJnaLibImage extends Library {
    
    public static final SdlJnaLibImage INSTANCE = (SdlJnaLibImage) Native.loadLibrary("SDL2_image", SdlJnaLibImage.class);

    //--------------------------------------------------------------------------
    // SDL_image.h
    //--------------------------------------------------------------------------

    /**
     * @see SdlImgInitFlag
     * 
     * Loads dynamic libraries and prepares them for use.
     * Flags should be one or more flags from IMG_InitFlags OR'd together.
     * It returns the flags successfully initialized, or 0 on failure.
     * 
     * extern DECLSPEC int SDLCALL IMG_Init(int flags);
     */
    public int IMG_Init(int flags);
    
    /**
     * Unloads libraries loaded with IMG_Init.
     * 
     * extern DECLSPEC void SDLCALL IMG_Quit(void);
     */
    public void IMG_Quit();
    
    /**
     * Load an image from an SDL data source.
     * The 'type' may be one of: "BMP", "GIF", "PNG", etc.
     *
     * If the image format supports a transparent pixel, SDL will set the
     * colorkey for the surface.  You can enable RLE acceleration
     * (TODO sdl What is it?) on the surface afterwards by calling:
     * SDL_SetColorKey(image, SDL_RLEACCEL, image->format->colorkey);
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL IMG_LoadTyped_RW(SDL_RWops *src, int freesrc, const char *type);
     */
    public Pointer IMG_LoadTyped_RW(Pointer src, int freesrc, String type);
    
    /*
     * Convenience functions
     */
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL IMG_Load(const char *file);
     */
    public Pointer IMG_Load(String file);
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL IMG_Load_RW(SDL_RWops *src, int freesrc);
     */
    public Pointer IMG_Load_RW(Pointer src, int freesrc);
    
    /**
     * Load an image directly into a render texture.
     * 
     * extern DECLSPEC SDL_Texture * SDLCALL IMG_LoadTexture(SDL_Renderer *renderer, const char *file);
     */
    public Pointer IMG_LoadTexture(Pointer renderer, String file);
}
