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
package net.jolikit.bwd.impl.algr5.jlib;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import net.jolikit.bwd.impl.utils.basics.OsUtils;

/**
 * Java binding for (a subset of) "allegro_ttf" part of Allegro5.
 */
public interface AlgrJnaLibTtf extends Library {

    public static final AlgrJnaLibTtf INSTANCE = (AlgrJnaLibTtf) (
            (OsUtils.isWindows() ? Native.loadLibrary("allegro_ttf-5.2", AlgrJnaLibTtf.class)
                    : (OsUtils.isMac() ? Native.loadLibrary("allegro_ttf.5.2.2", AlgrJnaLibTtf.class)
                            // Unsupported.
                            : null)));

    //--------------------------------------------------------------------------
    // allegro_ttf.h
    //--------------------------------------------------------------------------

    public static final int ALLEGRO_TTF_NO_KERNING = 1;
    public static final int ALLEGRO_TTF_MONOCHROME = 2;
    public static final int ALLEGRO_TTF_NO_AUTOHINT = 4;

    /**
     * ALLEGRO_TTF_FUNC(ALLEGRO_FONT *, al_load_ttf_font, (char const *filename, int size, int flags));
     * 
     * Loads a TrueType font from a file using the FreeType library.
     * Quoting from the FreeType FAQ this means support for many different font formats:
     * TrueType, OpenType, Type1, CID, CFF, Windows FON/FNT, X11 PCF, and others
     * 
     * The size parameter determines the size the font will be rendered at, specified in pixel.
     * The standard font size is measured in units per EM,
     * if you instead want to specify the size as the total height of glyphs in pixel,
     * pass it as a negative value.
     */
    public Pointer al_load_ttf_font(String filename, int size, int flags);

    /**
     * ALLEGRO_TTF_FUNC(ALLEGRO_FONT *, al_load_ttf_font_f, (ALLEGRO_FILE *file, char const *filename, int size, int flags));
     */

    /**
     * ALLEGRO_TTF_FUNC(ALLEGRO_FONT *, al_load_ttf_font_stretch, (char const *filename, int w, int h, int flags));
     */
    public Pointer al_load_ttf_font_stretch(String filename, int w, int h, int flags);

    /**
     * ALLEGRO_TTF_FUNC(ALLEGRO_FONT *, al_load_ttf_font_stretch_f, (ALLEGRO_FILE *file, char const *filename, int w, int h, int flags));
     */

    /**
     * ALLEGRO_TTF_FUNC(bool, al_init_ttf_addon, (void));
     */
    public boolean al_init_ttf_addon();

    /**
     * ALLEGRO_TTF_FUNC(void, al_shutdown_ttf_addon, (void));
     */
    public void al_shutdown_ttf_addon();

    /**
     * ALLEGRO_TTF_FUNC(uint32_t, al_get_allegro_ttf_version, (void));
     */
    public int al_get_allegro_ttf_version();
}
