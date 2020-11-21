/*
 * Copyright 2019-2020 Jeff Hain
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
import com.sun.jna.ptr.IntByReference;

import net.jolikit.lang.OsUtils;

/**
 * Java binding for (a subset of) "allegro_font" part of Allegro5.
 */
public interface AlgrJnaLibFont extends Library {

    public static final AlgrJnaLibFont INSTANCE = (AlgrJnaLibFont) (
            // allegro_font-5.2.dll
            (OsUtils.isWindows() ? Native.loadLibrary("allegro_font-5.2", AlgrJnaLibFont.class)
                    // liballegro_font.dylib
                    : (OsUtils.isMac() ? Native.loadLibrary("allegro_font.5.2.2", AlgrJnaLibFont.class)
                            // Unsupported.
                            : null)));

    //--------------------------------------------------------------------------
    // allegro_font.h
    //--------------------------------------------------------------------------

    public static final int ALLEGRO_NO_KERNING = -1;
    public static final int ALLEGRO_ALIGN_LEFT = 0;
    public static final int ALLEGRO_ALIGN_CENTRE = 1;
    public static final int ALLEGRO_ALIGN_CENTER = 1;
    public static final int ALLEGRO_ALIGN_RIGHT = 2;
    public static final int ALLEGRO_ALIGN_INTEGER = 4;

    /**
     * ALLEGRO_FONT_FUNC(bool, al_register_font_loader, (const char *ext, ALLEGRO_FONT *(*load)(const char *filename, int size, int flags)));
     */
    public boolean al_register_font_loader(String ext, InterfaceAlgrFontLoadMethod fontLoadMethod);

    /**
     * ALLEGRO_FONT_FUNC(ALLEGRO_FONT *, al_load_bitmap_font, (const char *filename));
     */
    public Pointer al_load_bitmap_font(String ext, String filename);

    /**
     * ALLEGRO_FONT_FUNC(ALLEGRO_FONT *, al_load_bitmap_font_flags, (const char *filename, int flags));
     */
    public Pointer al_load_bitmap_font_flags(String filename, int flags);
    
    /**
     * ALLEGRO_FONT_FUNC(ALLEGRO_FONT *, al_load_font, (const char *filename, int size, int flags));
     * 
     * Loads a font from disk.
     * This will use al_load_bitmap_font if you pass the name of a known bitmap format,
     * or else al_load_ttf_font.
     * Bitmap and TTF fonts are affected by the current bitmap flags at the time the font is loaded.
     */
    public Pointer al_load_font(String filename, int size, int flags);

    /**
     * ALLEGRO_FONT_FUNC(ALLEGRO_FONT *, al_grab_font_from_bitmap, (ALLEGRO_BITMAP *bmp, int n, const int ranges[]));
     */
    public Pointer al_grab_font_from_bitmap(Pointer bmp, int n, int[] ranges);
    
    /**
     * ALLEGRO_FONT_FUNC(ALLEGRO_FONT *, al_create_builtin_font, (void));
     */
    public Pointer al_create_builtin_font();
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_ustr, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x, float y, int flags, ALLEGRO_USTR const *ustr));
     * 
     * ALLEGRO_USTR:
     * Introduced in 5.0.0
     * An opaque type representing a string. ALLEGRO_USTRs normally contain UTF-8 encoded strings,
     * but they may be used to hold any byte sequences, including NULs.
     */
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_text, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x, float y, int flags,
     * char const *text));
     */
    public void al_draw_text(ALLEGRO_FONT font, ALLEGRO_COLOR color, float x, float y, int flags, String text);
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_justified_text, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x1, float x2, float y, float diff, int flags, char const *text));
     */
    public void al_draw_justified_text(ALLEGRO_FONT font, ALLEGRO_COLOR color, float x1, float x2, float y, float diff, int flags, String text);
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_justified_ustr, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x1, float x2, float y, float diff, int flags, ALLEGRO_USTR const *text));
     */
    
    /**
     * ALLEGRO_FONT_PRINTFUNC(void, al_draw_textf, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x, float y, int flags, char const *format, ...), 6, 7);
     */
    
    /**
     * ALLEGRO_FONT_PRINTFUNC(void, al_draw_justified_textf, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x1, float x2, float y, float diff, int flags, char const *format, ...), 8, 9);
     */
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_text_width, (const ALLEGRO_FONT *f, const char *str));
     */
    public int al_get_text_width(ALLEGRO_FONT font, String str);
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_ustr_width, (const ALLEGRO_FONT *f, const ALLEGRO_USTR *ustr));
     */
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_font_line_height, (const ALLEGRO_FONT *f));
     */
    public int al_get_font_line_height(ALLEGRO_FONT font);
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_font_ascent, (const ALLEGRO_FONT *f));
     */
    public int al_get_font_ascent(ALLEGRO_FONT font);
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_font_descent, (const ALLEGRO_FONT *f));
     */
    public int al_get_font_descent(ALLEGRO_FONT font);
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_destroy_font, (ALLEGRO_FONT *f));
     */
    public void al_destroy_font(ALLEGRO_FONT font);
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_get_ustr_dimensions, (const ALLEGRO_FONT *f, ALLEGRO_USTR const *text, int *bbx, int *bby, int *bbw, int *bbh));
     */
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_get_text_dimensions, (const ALLEGRO_FONT *f, char const *text, int *bbx, int *bby, int *bbw, int *bbh));
     */
    public void al_get_text_dimensions(ALLEGRO_FONT font, String text, IntByReference bbx, IntByReference bby, IntByReference bbw, IntByReference bbh);
    
    /**
     * ALLEGRO_FONT_FUNC(bool, al_init_font_addon, (void));
     */
    public boolean al_init_font_addon();
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_shutdown_font_addon, (void));
     */
    public void al_shutdown_font_addon();
    
    /**
     * ALLEGRO_FONT_FUNC(uint32_t, al_get_allegro_font_version, (void));
     */
    public int al_get_allegro_font_version();
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_font_ranges, (ALLEGRO_FONT *font, int ranges_count, int *ranges));
     */
    public int al_get_font_ranges(ALLEGRO_FONT font, int ranges_count, int[] ranges);
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_glyph, (const ALLEGRO_FONT *font,  ALLEGRO_COLOR color, float x, float y, int codepoint));
     */
    public void al_draw_glyph(ALLEGRO_FONT font, ALLEGRO_COLOR color, float x, float y, int codepoint);
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_glyph_width, (const ALLEGRO_FONT *f, int codepoint));
     */
    public int al_get_glyph_width(ALLEGRO_FONT font, int codepoint);
    
    /**
     * ALLEGRO_FONT_FUNC(bool, al_get_glyph_dimensions, (const ALLEGRO_FONT *f, int codepoint, int *bbx, int *bby, int *bbw, int *bbh));
     */
    public boolean al_get_glyph_dimensions(ALLEGRO_FONT font, int codepoint, IntByReference bbx, IntByReference bby, IntByReference bbw, IntByReference bbh);
    
    /**
     * ALLEGRO_FONT_FUNC(int, al_get_glyph_advance, (const ALLEGRO_FONT *f, int codepoint1, int codepoint2));
     */
    public int al_get_glyph_advance(ALLEGRO_FONT font, int codepoint1, int codepoint2);
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_multiline_text, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x, float y, float max_width, float line_height, int flags, const char *text));
     */
    public void al_draw_multiline_text(ALLEGRO_FONT font, ALLEGRO_COLOR color, float x, float y, float max_width, float line_height, int flags, String text);
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_multiline_textf, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x, float y, float max_width, float line_height, int flags, const char *format, ...));
     */
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_draw_multiline_ustr, (const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x, float y, float max_width, float line_height, int flags, const ALLEGRO_USTR *text));
     */
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_do_multiline_text, (const ALLEGRO_FONT *font, float max_width, const char *text, bool (*cb)(int line_num, const char *line, int size, void *extra), void *extra));
     */
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_do_multiline_ustr, (const ALLEGRO_FONT *font, float max_width, const ALLEGRO_USTR *ustr, bool (*cb)(int line_num, const ALLEGRO_USTR *line, void *extra), void *extra));
     */
    
    /**
     * ALLEGRO_FONT_FUNC(void, al_set_fallback_font, (ALLEGRO_FONT *font, ALLEGRO_FONT *fallback));
     */
    public void al_set_fallback_font(ALLEGRO_FONT font, ALLEGRO_FONT fallback);
    
    /**
     * ALLEGRO_FONT_FUNC(ALLEGRO_FONT *, al_get_fallback_font, (ALLEGRO_FONT *font));
     */
    public Pointer al_get_fallback_font(ALLEGRO_FONT font);
}
