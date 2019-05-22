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

import net.jolikit.bwd.impl.sdl2.jlib.SDL_Color;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Java binding for (a subset of) SDL2_ttf.
 * 
 * Uses SDL_SetError and SDL_GetError from SDL library.
 * 
 * "SDL_ttf is a TrueType font rendering library that is used with the SDL library,
 * and almost as portable.
 * It depends on freetype2 to handle the TrueType font data.
 * It allows a programmer to use multiple TrueType fonts
 * without having to code a font rendering routine themselves.
 * With the power of outline fonts and antialiasing,
 * high quality text output can be obtained without much effort."
 */
public interface SdlJnaLibTtf extends Library {

    public static final SdlJnaLibTtf INSTANCE = (SdlJnaLibTtf) Native.loadLibrary("SDL2_ttf", SdlJnaLibTtf.class);

    //--------------------------------------------------------------------------
    // SDL_ttf.h
    //--------------------------------------------------------------------------
    
    /**
     * Initialize the TTF engine - returns 0 if successful, -1 on error
     * 
     * extern DECLSPEC int SDLCALL TTF_Init(void);
     */
    public int TTF_Init();
    
    /**
     * Open a font file and create a font of the specified point size.
     * Some .fon fonts will have several sizes embedded in the file, so the
     * point size becomes the index of choosing which size.  If the value
     * is too high, the last indexed size will be the default.
     *
     * @param ptsize Font with floor point size will be returned, if any
     * 
     * extern DECLSPEC TTF_Font * SDLCALL TTF_OpenFont(const char *file, int ptsize);
     */
    public Pointer TTF_OpenFont(String file, int ptsize);
    
    /**
     * "This function loads a specific font face from the file,
     * and is useful if your font file contains several font faces.
     * A font index of 0 is always the first font in the file. "
     * 
     * extern DECLSPEC TTF_Font * SDLCALL TTF_OpenFontIndex(const char *file, int ptsize, long index);
     */
    public Pointer TTF_OpenFontIndex(String file, int ptsize, long index);

    /*
     * 
     */
    
    /**
     * extern DECLSPEC int SDLCALL TTF_GetFontStyle(const TTF_Font *font);
     */
    public int TTF_GetFontStyle(Pointer font);
    
    /**
     * @see SdlTtfStyle
     * 
     * extern DECLSPEC void SDLCALL TTF_SetFontStyle(TTF_Font *font, int style);
     */
    public void TTF_SetFontStyle(Pointer font, int style);
    
    /**
     * extern DECLSPEC int SDLCALL TTF_GetFontOutline(const TTF_Font *font);
     */
    public int TTF_GetFontOutline(Pointer font);
    
    /**
     * extern DECLSPEC void SDLCALL TTF_SetFontOutline(TTF_Font *font, int outline);
     */
    public void TTF_SetFontOutline(Pointer font, int outline);
    
    /*
     * 
     */

    /**
     * extern DECLSPEC int SDLCALL TTF_GetFontHinting(const TTF_Font *font);
     */
    public int TTF_GetFontHinting(Pointer font);
    
    /**
     * @see SdlTtfHinting
     * 
     * extern DECLSPEC void SDLCALL TTF_SetFontHinting(TTF_Font *font, int hinting);
     */
    public void TTF_SetFontHinting(Pointer font, int hinting);
    
    /**
     * Get the total height of the font - usually equal to point size.
     * (TODO sdl Does that mean that height is in points, not in pixels?)
     * 
     * extern DECLSPEC int SDLCALL TTF_FontHeight(const TTF_Font *font);
     */
    public int TTF_FontHeight(Pointer font);
    
    /**
     * Get the offset from the baseline to the top of the font
     * This is a positive value, relative to the baseline.
     * 
     * extern DECLSPEC int SDLCALL TTF_FontAscent(const TTF_Font *font);
     */
    public int TTF_FontAscent(Pointer font);
    
    /**
     * Get the offset from the baseline to the bottom of the font
     * This is a negative value, relative to the baseline.
     * 
     * extern DECLSPEC int SDLCALL TTF_FontDescent(const TTF_Font *font);
     */
    public int TTF_FontDescent(Pointer font);
    
    /**
     * Get the recommended spacing between lines of text for this font.
     * 
     * extern DECLSPEC int SDLCALL TTF_FontLineSkip(const TTF_Font *font);
     */
    public int TTF_FontLineSkip(Pointer font);
    
    /**
     * Get whether or not kerning is allowed for this font.
     * 
     * extern DECLSPEC int SDLCALL TTF_GetFontKerning(const TTF_Font *font);
     */
    public int TTF_GetFontKerning(Pointer font);

    /**
     * Set whether or not kerning is allowed for this font.
     * 
     * extern DECLSPEC void SDLCALL TTF_SetFontKerning(TTF_Font *font, int allowed);
     */
    public void TTF_SetFontKerning(Pointer font, int allowed);
    
    /**
     * Get the number of faces of the font
     * 
     * extern DECLSPEC long SDLCALL TTF_FontFaces(const TTF_Font *font);
     */
    public long TTF_FontFaces(Pointer font);
    
    /**
     * extern DECLSPEC int SDLCALL TTF_FontFaceIsFixedWidth(const TTF_Font *font);
     */
    public int TTF_FontFaceIsFixedWidth(Pointer font);
    
    /**
     * extern DECLSPEC char * SDLCALL TTF_FontFaceFamilyName(const TTF_Font *font);
     */
    public String TTF_FontFaceFamilyName(Pointer font);
    
    /**
     * extern DECLSPEC char * SDLCALL TTF_FontFaceStyleName(const TTF_Font *font);
     */
    public String TTF_FontFaceStyleName(Pointer font);
    
    /**
     * Check whether a glyph is provided by the font or not.
     * 
     * extern DECLSPEC int SDLCALL TTF_GlyphIsProvided(const TTF_Font *font, Uint16 ch);
     * 
     * @return The index of the glyph for ch in font, or 0 for an undefined character code.
     */
    public int TTF_GlyphIsProvided(Pointer font, char ch);
    
    /**
     * Get the metrics (dimensions) of a glyph.
     * To understand what these metrics mean, here is a useful link:
     * http://freetype.sourceforge.net/freetype2/docs/tutorial/step2.html
     * 
     * extern DECLSPEC int SDLCALL TTF_GlyphMetrics(TTF_Font *font, Uint16 ch, int *minx, int *maxx, int *miny, int *maxy, int *advance);
     */
    public int TTF_GlyphMetrics(
            Pointer font, char ch,
            IntByReference minx, IntByReference maxx,
            IntByReference miny, IntByReference maxy,
            IntByReference advance);
    
    /**
     * Get the dimensions of a rendered string of text
     * 
     * extern DECLSPEC int SDLCALL TTF_SizeText(TTF_Font *font, const char *text, int *w, int *h);
     */
    public int TTF_SizeText(Pointer font, String text, IntByReference w, IntByReference h);
    
    /**
     * extern DECLSPEC int SDLCALL TTF_SizeUTF8(TTF_Font *font, const char *text, int *w, int *h);
     */
    public int TTF_SizeUTF8(Pointer font, String text, IntByReference w, IntByReference h);

    /**
     * extern DECLSPEC int SDLCALL TTF_SizeUNICODE(TTF_Font *font, const Uint16 *text, int *w, int *h);
     */
    public int TTF_SizeUNICODE(Pointer font, String text, IntByReference w, IntByReference h);
    
    /**
     * Create an 8-bit palettized surface and render the given text at
     * fast quality with the given font and color.  The 0 pixel is the
     * colorkey, giving a transparent background, and the 1 pixel is set
     * to the text color.
     * This function returns the new surface, or NULL if there was an error.
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderText_Solid(TTF_Font *font, const char *text, SDL_Color fg);
     */
    public Pointer TTF_RenderText_Solid(Pointer font, String text, SDL_Color fg);
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderUTF8_Solid(TTF_Font *font, const char *text, SDL_Color fg);
     */
    public Pointer TTF_RenderUTF8_Solid(Pointer font, String text, SDL_Color fg);
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderUNICODE_Solid(TTF_Font *font, const Uint16 *text, SDL_Color fg);
     */
    public Pointer TTF_RenderUNICODE_Solid(Pointer font, String text, SDL_Color fg);
    
    /**
     * Create an 8-bit palettized surface and render the given glyph at
     * fast quality with the given font and color.  The 0 pixel is the
     * colorkey, giving a transparent background, and the 1 pixel is set
     * to the text color.  The glyph is rendered without any padding or
     * centering in the X direction, and aligned normally in the Y direction.
     * This function returns the new surface, or NULL if there was an error.
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderGlyph_Solid(TTF_Font *font, Uint16 ch, SDL_Color fg);
     */
    public Pointer TTF_RenderGlyph_Solid(Pointer font, char ch, SDL_Color fg);
     
    /**
     * Create an 8-bit palettized surface and render the given text at
     * high quality with the given font and colors.  The 0 pixel is background,
     * while other pixels have varying degrees of the foreground color.
     * This function returns the new surface, or NULL if there was an error.
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderText_Shaded(TTF_Font *font, const char *text, SDL_Color fg, SDL_Color bg);
     */
    public Pointer TTF_RenderText_Shaded(Pointer font, String text, SDL_Color fg, SDL_Color bg);
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderUTF8_Shaded(TTF_Font *font, const char *text, SDL_Color fg, SDL_Color bg);
     */
    public Pointer TTF_RenderUTF8_Shaded(Pointer font, String text, SDL_Color fg, SDL_Color bg);
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderUNICODE_Shaded(TTF_Font *font, const Uint16 *text, SDL_Color fg, SDL_Color bg);
     */
    public Pointer TTF_RenderUNICODE_Shaded(Pointer font, String text, SDL_Color fg, SDL_Color bg);
    
    /**
     * Create an 8-bit palettized surface and render the given glyph at
     * high quality with the given font and colors.  The 0 pixel is background,
     * while other pixels have varying degrees of the foreground color.
     * The glyph is rendered without any padding or centering in the X
     * direction, and aligned normally in the Y direction.
     * This function returns the new surface, or NULL if there was an error.
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderGlyph_Shaded(TTF_Font *font, Uint16 ch, SDL_Color fg, SDL_Color bg);
     */
    public Pointer TTF_RenderGlyph_Shaded(Pointer font, char ch, SDL_Color fg, SDL_Color bg);
    
    /**
     * Create a 32-bit ARGB surface and render the given text at high quality,
     * using alpha blending to dither the font with the given color.
     * This function returns the new surface, or NULL if there was an error.
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderText_Blended(TTF_Font *font, const char *text, SDL_Color fg);
     */
    public Pointer TTF_RenderText_Blended(Pointer font, String text, SDL_Color fg);
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderUTF8_Blended(TTF_Font *font, const char *text, SDL_Color fg);
     */
    public Pointer TTF_RenderUTF8_Blended(Pointer font, String text, SDL_Color fg);
    
    /**
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderUNICODE_Blended(TTF_Font *font, const Uint16 *text, SDL_Color fg);
     */
    public Pointer TTF_RenderUNICODE_Blended(Pointer font, String text, SDL_Color fg);
    
    /**
     * Create a 32-bit ARGB surface and render the given glyph at high quality,
     * using alpha blending to dither the font with the given color.
     * The glyph is rendered without any padding or centering in the X
     * direction, and aligned normally in the Y direction.
     * This function returns the new surface, or NULL if there was an error.
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL TTF_RenderGlyph_Blended(TTF_Font *font, Uint16 ch, SDL_Color fg);
     */
    public Pointer TTF_RenderGlyph_Blended(Pointer font, char ch, SDL_Color fg);
    
    /**
     * Close an opened font file
     * 
     * extern DECLSPEC void SDLCALL TTF_CloseFont(TTF_Font *font);
     */
    public void TTF_CloseFont(Pointer font);
    
    /**
     * De-initialize the TTF engine
     * 
     * extern DECLSPEC void SDLCALL TTF_Quit(void);
     */
    public void TTF_Quit();
    
    /**
     * Check if the TTF engine is initialized
     * 
     * extern DECLSPEC int SDLCALL TTF_WasInit(void);
     */
    public int TTF_WasInit();

    /**
     * Get the kerning size of two glyphs
     * 
     * extern DECLSPEC int TTF_GetFontKerningSizeGlyphs(TTF_Font *font, Uint16 previous_ch, Uint16 ch);
     */
    public int TTF_GetFontKerningSizeGlyphs(Pointer font, char previous_ch, char ch);
}
