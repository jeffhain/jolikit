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
 * Java binding for (a subset of) "allegro" part of Allegro5.
 */
public interface AlgrJnaLib extends Library {

    public static final AlgrJnaLib INSTANCE = (AlgrJnaLib) (
            (OsUtils.isWindows() ? Native.loadLibrary("allegro-5.2", AlgrJnaLib.class)
                    : (OsUtils.isMac() ? Native.loadLibrary("allegro.5.2.2", AlgrJnaLib.class)
                            // Unsupported.
                            : null)));

    //--------------------------------------------------------------------------
    // base.h
    //--------------------------------------------------------------------------

    public static final double ALLEGRO_PI = 3.14159265358979323846;

    /**
     * AL_FUNC(uint32_t, al_get_allegro_version, (void));
     */
    public int al_get_allegro_version();
    
    /**
     * AL_FUNC(int, al_run_main, (int argc, char **argv, int (*)(int, char **)));
     * 
     * int al_run_main(int argc, char **argv, int (*user_main)(int, char **))
     * 
     * This function is useful in cases where you don't have a main() function
     * but want to run Allegro (mostly useful in a wrapper library).
     * Under Windows and Linux this is no problem because you simply can call
     * al_install_system.
     * But some other system (like OSX) don't allow calling al_install_system
     * in the main thread. al_run_main will know what to do in that case.
     * The passed argc and argv will simply be passed on to user_main and
     * the return value of user_main will be returned.
     */
    public int al_run_main(int argc, String[] argv, InterfaceAlgrUserMain user_main);

    //--------------------------------------------------------------------------
    // monitor.h
    //--------------------------------------------------------------------------

    public static final int ALLEGRO_DEFAULT_DISPLAY_ADAPTER = -1;

    /**
     * AL_FUNC(int, al_get_num_video_adapters, (void));
     */
    public int al_get_num_video_adapters();

    /**
     * AL_FUNC(bool, al_get_monitor_info, (int adapter, ALLEGRO_MONITOR_INFO *info));
     */
    public boolean al_get_monitor_info(int adapter, ALLEGRO_MONITOR_INFO info);

    //--------------------------------------------------------------------------
    // fullscreen_mode.h
    //--------------------------------------------------------------------------

    /**
     * AL_FUNC(int, al_get_num_display_modes, (void));
     */
    public int al_get_num_display_modes();

    /**
     * AL_FUNC(ALLEGRO_DISPLAY_MODE*, al_get_display_mode, (int index, ALLEGRO_DISPLAY_MODE *mode));
     */
    public Pointer al_get_display_mode(int index, ALLEGRO_DISPLAY_MODE mode);

    //--------------------------------------------------------------------------
    // display.h
    //--------------------------------------------------------------------------

    /*
     * @see AlgrDisplayFlag
     * 
     * @see AlgrDisplayOption
     */

    public static final int ALLEGRO_DONTCARE = 0;
    public static final int ALLEGRO_REQUIRE = 1;
    public static final int ALLEGRO_SUGGEST = 2;

    /*
     * 
     */
    
    /**
     * Formally part of the primitives addon.
     */
    public static final int _ALLEGRO_PRIM_MAX_USER_ATTR = 10;

    /**
     * enum ALLEGRO_NEW_WINDOW_TITLE_MAX_SIZE
     */
    public static final int ALLEGRO_NEW_WINDOW_TITLE_MAX_SIZE = 255;

    /**
     * AL_FUNC(void, al_set_new_display_refresh_rate, (int refresh_rate));
     */
    public void al_set_new_display_refresh_rate(int refresh_rate);

    /**
     * AL_FUNC(void, al_set_new_display_flags, (int flags));
     */
    public void al_set_new_display_flags(int flags);

    /**
     * AL_FUNC(int,  al_get_new_display_refresh_rate, (void));
     */
    public int al_get_new_display_refresh_rate();
    
    /**
     * AL_FUNC(int,  al_get_new_display_flags, (void));
     */
    public int al_get_new_display_flags();

    /**
     * AL_FUNC(void, al_set_new_window_title, (const char *title));
     */
    public void al_set_new_window_title(String title);
    
    /**
     * AL_FUNC(const char *, al_get_new_window_title, (void));
     */
    public String al_get_new_window_title();
    
    /**
     * AL_FUNC(int, al_get_display_width,  (ALLEGRO_DISPLAY *display));
     */
    public int al_get_display_width(Pointer display);
    
    /**
     * AL_FUNC(int, al_get_display_height, (ALLEGRO_DISPLAY *display));
     */
    public int al_get_display_height(Pointer display);
    
    /**
     * AL_FUNC(int, al_get_display_format, (ALLEGRO_DISPLAY *display));
     */
    public int al_get_display_format(Pointer display);
    
    /**
     * AL_FUNC(int, al_get_display_refresh_rate, (ALLEGRO_DISPLAY *display));
     */
    public int al_get_display_refresh_rate(Pointer display);
    
    /**
     * AL_FUNC(int, al_get_display_flags,  (ALLEGRO_DISPLAY *display));
     */
    public int al_get_display_flags(Pointer display);
    
    /**
     * AL_FUNC(int, al_get_display_orientation, (ALLEGRO_DISPLAY* display));
     */
    public int al_get_display_orientation(Pointer display);
    
    /**
     * AL_FUNC(bool, al_set_display_flag, (ALLEGRO_DISPLAY *display, int flag, bool onoff));
     */
    public boolean al_set_display_flag(Pointer display, int flag, boolean onoff);

    /**
     * AL_FUNC(ALLEGRO_DISPLAY*, al_create_display, (int w, int h));
     * 
     * Create a display, or window, with the specified dimensions.
     * The parameters of the display are determined by the last calls to
     * al_set_new_display_*.
     * Default parameters are used if none are set explicitly.
     * Creating a new display will automatically make it the active one,
     * with the backbuffer selected for drawing.
     * 
     * Returns NULL on error.
     * 
     * Each display that uses OpenGL as a backend has a distinct OpenGL
     * rendering context associated with it. See al_set_target_bitmap
     * for the discussion about rendering contexts.
     */
    public Pointer al_create_display(int w, int h);
    
    /**
     * AL_FUNC(void,             al_destroy_display, (ALLEGRO_DISPLAY *display));
     */
    public void al_destroy_display(Pointer display);
    
    /**
     * AL_FUNC(ALLEGRO_DISPLAY*, al_get_current_display, (void));
     */
    public Pointer al_get_current_display();
    
    /**
     * Select the bitmap to which all subsequent drawing operations
     * in the calling thread will draw.
     * 
     * AL_FUNC(void,            al_set_target_bitmap, (ALLEGRO_BITMAP *bitmap));
     */
    public void al_set_target_bitmap(Pointer bitmap);
    
    /**
     * AL_FUNC(void,            al_set_target_backbuffer, (ALLEGRO_DISPLAY *display));
     */
    public void al_set_target_backbuffer(Pointer display);
    
    /**
     * AL_FUNC(ALLEGRO_BITMAP*, al_get_backbuffer,    (ALLEGRO_DISPLAY *display));
     */
    public Pointer al_get_backbuffer(Pointer display);
    
    /**
     * AL_FUNC(ALLEGRO_BITMAP*, al_get_target_bitmap, (void));
     */
    public Pointer al_get_target_bitmap();
    
    /**
     * AL_FUNC(bool, al_acknowledge_resize, (ALLEGRO_DISPLAY *display));
     */
    public boolean al_acknowledge_resize(Pointer display);
    
    /**
     * AL_FUNC(bool, al_resize_display,     (ALLEGRO_DISPLAY *display, int width, int height));
     */
    public boolean al_resize_display(Pointer display, int width, int height);
    
    /**
     * Copies or updates the front and back buffers so that what has been drawn
     * previously on the currently selected display becomes visible on screen.
     * Pointers to the special back and front buffer bitmaps remain valid and
     * retain their semantics as back and front buffers respectively, although
     * their contents may have changed.
     * 
     * Several display options change how this function behaves:
     * With ALLEGRO_SINGLE_BUFFER, no flipping is done. You still have to call
     * this function to display graphics, depending on how the used graphics
     * system works.
     * The ALLEGRO_SWAP_METHOD option may have additional information about what
     * kind of operation is used internally to flip the front and back buffers.
     * 
     * If ALLEGRO_VSYNC is 1, this function will force waiting for vsync.
     * If ALLEGRO_VSYNC is 2, this function will not wait for vsync.
     * With many drivers the vsync behavior is controlled by the user and not the
     * application, and ALLEGRO_VSYNC will not be set; in this case
     * al_flip_display will wait for vsync depending on the settings set in the
     * system's graphics preferences.
     * 
     * See also: al_set_new_display_flags, al_set_new_display_option
     * 
     * AL_FUNC(void, al_flip_display,       (void));
     */
    public void al_flip_display();
    
    /**
     * AL_FUNC(void, al_update_display_region, (int x, int y, int width, int height));
     */
    public void al_update_display_region(int x, int y, int width, int height);
    
    /**
     * AL_FUNC(bool, al_is_compatible_bitmap, (ALLEGRO_BITMAP *bitmap));
     */
    public boolean al_is_compatible_bitmap(Pointer bitmap);
    
    /**
     * AL_FUNC(bool, al_wait_for_vsync, (void));
     * 
     * Wait for the beginning of a vertical retrace. Some driver/card/monitor
     * combinations may not be capable of this.
     * ===> For portability, don't use it.
     */
    public void al_wait_for_vsync();
    
    /**
     * AL_FUNC(ALLEGRO_EVENT_SOURCE *, al_get_display_event_source, (ALLEGRO_DISPLAY *display));
     */
    public Pointer al_get_display_event_source(Pointer display);
    
    /**
     * AL_FUNC(void, al_set_display_icon, (ALLEGRO_DISPLAY *display, ALLEGRO_BITMAP *icon));
     */
    public void al_set_display_icon(Pointer display, Pointer icon);
    
    /**
     * AL_FUNC(void, al_set_display_icons, (ALLEGRO_DISPLAY *display, int num_icons, ALLEGRO_BITMAP *icons[]));
     */
    public void al_set_display_icons(Pointer display, int num_icons, Pointer[] icons);

    /*
     * Stuff for multihead/window management
     */

    /**
     * AL_FUNC(int, al_get_new_display_adapter, (void));
     */
    public int al_get_new_display_adapter();
    
    /**
     * AL_FUNC(void, al_set_new_display_adapter, (int adapter));
     */
    public void al_set_new_display_adapter(int adapter);
    
    /**
     * AL_FUNC(void, al_set_new_window_position, (int x, int y));
     */
    public void al_set_new_window_position(int x, int y);
    
    /**
     * AL_FUNC(void, al_get_new_window_position, (int *x, int *y));
     */
    public void al_get_new_window_position(IntByReference x, IntByReference y);
    
    /**
     * AL_FUNC(void, al_set_window_position, (ALLEGRO_DISPLAY *display, int x, int y));
     */
    public void al_set_window_position(Pointer display, int x, int y);
    
    /**
     * AL_FUNC(void, al_get_window_position, (ALLEGRO_DISPLAY *display, int *x, int *y));
     */
    public void al_get_window_position(Pointer display, IntByReference x, IntByReference y);
    
    /**
     * AL_FUNC(bool, al_set_window_constraints, (ALLEGRO_DISPLAY *display, int min_w, int min_h, int max_w, int max_h));
     */
    public boolean al_set_window_constraints(Pointer display, int min_w, int min_h, int max_w, int max_h);
    
    /**
     * AL_FUNC(bool, al_get_window_constraints, (ALLEGRO_DISPLAY *display, int *min_w, int *min_h, int *max_w, int *max_h));
     */
    public boolean al_get_window_constraints(Pointer display, IntByReference min_w, IntByReference min_h, IntByReference max_w, IntByReference max_h);

    /**
     * AL_FUNC(void, al_set_window_title, (ALLEGRO_DISPLAY *display, const char *title));
     */
    public void al_set_window_title(Pointer display, String title);

    /*
     * Defined in display_settings.c
     */

    /**
     * AL_FUNC(void, al_set_new_display_option, (int option, int value, int importance));
     */
    public void al_set_new_display_option(int option, int value, int importance);
    
    /**
     * AL_FUNC(int, al_get_new_display_option, (int option, int *importance));
     */
    public int al_get_new_display_option(int option, IntByReference importance);
    
    /**
     * AL_FUNC(void, al_reset_new_display_options, (void));
     */
    public void al_reset_new_display_options();
    
    /**
     * AL_FUNC(void, al_set_display_option, (ALLEGRO_DISPLAY *display, int option, int value));
     */
    public void al_set_display_option(Pointer display, int option, int value);
    
    /**
     * AL_FUNC(int, al_get_display_option, (ALLEGRO_DISPLAY *display, int option));
     */
    public int al_get_display_option(Pointer display, int option);

    /*
     * Deferred drawing
     */

    /**
     * AL_FUNC(void, al_hold_bitmap_drawing, (bool hold));
     */
    public void al_hold_bitmap_drawing(boolean hold);
    
    /**
     * AL_FUNC(bool, al_is_bitmap_drawing_held, (void));
     */
    public boolean al_is_bitmap_drawing_held();

    /**
     * AL_FUNC(void, al_acknowledge_drawing_halt, (ALLEGRO_DISPLAY *display));
     */
    public void al_acknowledge_drawing_halt(Pointer display);
    
    /**
     * AL_FUNC(void, al_acknowledge_drawing_resume, (ALLEGRO_DISPLAY *display));
     */
    public void al_acknowledge_drawing_resume(Pointer display);

    //--------------------------------------------------------------------------
    // drawing.h
    //--------------------------------------------------------------------------

    /**
     * Clear the complete target bitmap, but confined by the clipping rectangle.
     * 
     * AL_FUNC(void, al_clear_to_color, (ALLEGRO_COLOR color));
     */
    public void al_clear_to_color(ALLEGRO_COLOR color);

    /**
     * AL_FUNC(void, al_clear_depth_buffer, (float x)); // TODO algr z, not x
     */
    public void al_clear_depth_buffer(float z);

    /**
     * AL_FUNC(void, al_draw_pixel, (float x, float y, ALLEGRO_COLOR color));
     */
    public void al_draw_pixel(float x, float y, ALLEGRO_COLOR color);

    //--------------------------------------------------------------------------
    // bitmap_draw.h
    //--------------------------------------------------------------------------

    /*
     * Blitting.
     * 
     * @see AlgrBlitFlag
     */

    /**
     * AL_FUNC(void, al_draw_bitmap, (ALLEGRO_BITMAP *bitmap, float dx, float dy, int flags));
     */
    public void al_draw_bitmap(Pointer bitmap, float dx, float dy, int flags);

    /**
     * AL_FUNC(void, al_draw_bitmap_region, (ALLEGRO_BITMAP *bitmap, float sx, float sy, float sw, float sh, float dx, float dy, int flags));
     */
    public void al_draw_bitmap_region(Pointer bitmap, float sx, float sy, float sw, float sh, float dx, float dy, int flags);

    /**
     * AL_FUNC(void, al_draw_scaled_bitmap, (ALLEGRO_BITMAP *bitmap, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, int flags));
     */
    public void al_draw_scaled_bitmap(Pointer bitmap, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, int flags);

    /**
     * AL_FUNC(void, al_draw_rotated_bitmap, (ALLEGRO_BITMAP *bitmap, float cx, float cy, float dx, float dy, float angle, int flags));
     */
    public void al_draw_rotated_bitmap(Pointer bitmap, float cx, float cy, float dx, float dy, float angle, int flags);

    /**
     * AL_FUNC(void, al_draw_scaled_rotated_bitmap, (ALLEGRO_BITMAP *bitmap, float cx, float cy, float dx, float dy, float xscale, float yscale, float angle, int flags));
     */
    public void al_draw_scaled_rotated_bitmap(Pointer bitmap, float cx, float cy, float dx, float dy, float xscale, float yscale, float angle, int flags);

    /*
     * Tinted blitting.
     */

    /**
     * Like al_draw_bitmap but multiplies all colors in the bitmap with the given color. For example:
     * 
     * al_draw_tinted_bitmap(bitmap, al_map_rgba_f(1, 1, 1, 0.5), x, y, 0);
     * The above will draw the bitmap 50% transparently.
     * 
     * al_draw_tinted_bitmap(bitmap, al_map_rgba_f(1, 0, 0, 1), x, y, 0);
     * The above will only draw the red component of the bitmap.
     * 
     * AL_FUNC(void, al_draw_tinted_bitmap, (ALLEGRO_BITMAP *bitmap, ALLEGRO_COLOR tint, float dx, float dy, int flags));
     */
    public void al_draw_tinted_bitmap(Pointer bitmap, ALLEGRO_COLOR tint, float dx, float dy, int flags);

    /**
     * AL_FUNC(void, al_draw_tinted_bitmap_region, (ALLEGRO_BITMAP *bitmap, ALLEGRO_COLOR tint, float sx, float sy, float sw, float sh, float dx, float dy, int flags));
     */
    public void al_draw_tinted_bitmap_region(Pointer bitmap, ALLEGRO_COLOR tint, float sx, float sy, float sw, float sh, float dx, float dy, int flags);

    /**
     * AL_FUNC(void, al_draw_tinted_scaled_bitmap, (ALLEGRO_BITMAP *bitmap, ALLEGRO_COLOR tint, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, int flags));
     */
    public void al_draw_tinted_scaled_bitmap(Pointer bitmap, ALLEGRO_COLOR tint, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, int flags);

    /**
     * AL_FUNC(void, al_draw_tinted_rotated_bitmap, (ALLEGRO_BITMAP *bitmap, ALLEGRO_COLOR tint, float cx, float cy, float dx, float dy, float angle, int flags));
     */
    public void al_draw_tinted_rotated_bitmap(Pointer bitmap, ALLEGRO_COLOR tint, float cx, float cy, float dx, float dy, float angle, int flags);

    /**
     * AL_FUNC(void, al_draw_tinted_scaled_rotated_bitmap, (ALLEGRO_BITMAP *bitmap, ALLEGRO_COLOR tint, float cx, float cy, float dx, float dy, float xscale, float yscale, float angle, int flags));
     */
    public void al_draw_tinted_scaled_rotated_bitmap(Pointer bitmap, ALLEGRO_COLOR tint, float cx, float cy, float dx, float dy, float xscale, float yscale, float angle, int flags);

    /**
     * AL_FUNC(void, al_draw_tinted_scaled_rotated_bitmap_region, (
     *     ALLEGRO_BITMAP *bitmap,
     *     float sx, float sy, float sw, float sh,
     *     ALLEGRO_COLOR tint,
     *     float cx, float cy, float dx, float dy, float xscale, float yscale,
     *     float angle, int flags));
     */
    public void al_draw_tinted_scaled_rotated_bitmap_region(
            Pointer bitmap,
            float sx, float sy, float sw, float sh,
            ALLEGRO_COLOR tint,
            float cx, float cy, float dx, float dy, float xscale, float yscale,
            float angle, int flags);

    //--------------------------------------------------------------------------
    // bitmap_io.h
    //--------------------------------------------------------------------------

    /*
     * @see AlgrBitmapLoadFlag
     */

    /**
     * AL_FUNC(bool, al_register_bitmap_loader, (const char *ext, ALLEGRO_IIO_LOADER_FUNCTION loader));
     */
    public boolean al_register_bitmap_loader(String ext, InterfaceAlgrIioLoader_bitmap_String_int loader);

    /**
     * AL_FUNC(bool, al_register_bitmap_saver, (const char *ext, ALLEGRO_IIO_SAVER_FUNCTION saver));
     */
    public boolean al_register_bitmap_saver(String ext, InterfaceAlgrIioSaver_boolean_String_bitmap saver);

    /**
     * AL_FUNC(bool, al_register_bitmap_loader_f, (const char *ext, ALLEGRO_IIO_FS_LOADER_FUNCTION fs_loader));
     */
    public boolean al_register_bitmap_loader_f(String ext, InterfaceAlgrIioFsLoader_bitmap_file_int fs_loader);

    /**
     * AL_FUNC(bool, al_register_bitmap_saver_f, (const char *ext, ALLEGRO_IIO_FS_SAVER_FUNCTION fs_saver));
     */
    public boolean al_register_bitmap_saver_f(String ext, InterfaceAlgrIioFsSaver_boolean_file_bitmap fs_saver);

    /**
     * AL_FUNC(bool, al_register_bitmap_identifier, (const char *ext, ALLEGRO_IIO_IDENTIFIER_FUNCTION identifier));
     */
    public boolean al_register_bitmap_identifier(String ext, InterfaceAlgrIioIdentifier_boolean_file identifier);

    /**
     * AL_FUNC(ALLEGRO_BITMAP *, al_load_bitmap, (const char *filename));
     */
    public Pointer al_load_bitmap(String filename);

    /**
     * AL_FUNC(ALLEGRO_BITMAP *, al_load_bitmap_flags, (const char *filename, int flags));
     */
    public Pointer al_load_bitmap_flags(String filename, int flags);

    /**
     * AL_FUNC(ALLEGRO_BITMAP *, al_load_bitmap_f, (ALLEGRO_FILE *fp, const char *ident));
     */
    public Pointer al_load_bitmap_f(Pointer fp, String ident);

    /**
     * AL_FUNC(ALLEGRO_BITMAP *, al_load_bitmap_flags_f, (ALLEGRO_FILE *fp, const char *ident, int flags));
     */
    public Pointer al_load_bitmap_flags_f(Pointer fp, String ident, int flags);

    /**
     * AL_FUNC(bool, al_save_bitmap, (const char *filename, ALLEGRO_BITMAP *bitmap));
     */
    public boolean al_save_bitmap(String filename, Pointer bitmap);

    /**
     * AL_FUNC(bool, al_save_bitmap_f, (ALLEGRO_FILE *fp, const char *ident, ALLEGRO_BITMAP *bitmap));
     */
    public boolean al_save_bitmap_f(Pointer fp, String ident, Pointer bitmap);

    /**
     * AL_FUNC(char const *, al_identify_bitmap_f, (ALLEGRO_FILE *fp));
     */
    public String al_identify_bitmap_f(Pointer fp);

    /**
     * AL_FUNC(char const *, al_identify_bitmap, (char const *filename));
     */
    public String al_identify_bitmap(String filename);

    //--------------------------------------------------------------------------
    // bitmap_lock.h
    //--------------------------------------------------------------------------

    /*
     * Locking flags
     */

    public static final int ALLEGRO_LOCK_READWRITE = 0;
    public static final int ALLEGRO_LOCK_READONLY = 1;
    public static final int ALLEGRO_LOCK_WRITEONLY = 2;

    /**
     * Lock an entire bitmap for reading or writing. If the bitmap is a display
     * bitmap it will be updated from system memory after the bitmap is unlocked
     * (unless locked read only). Returns NULL if the bitmap cannot be locked,
     * e.g. the bitmap was locked previously and not unlocked.
     * 
     * AL_FUNC(ALLEGRO_LOCKED_REGION*, al_lock_bitmap, (ALLEGRO_BITMAP *bitmap, int format, int flags));
     */
    public Pointer al_lock_bitmap(Pointer bitmap, int format, int flags);

    /**
     * AL_FUNC(ALLEGRO_LOCKED_REGION*, al_lock_bitmap_region,
     *         (ALLEGRO_BITMAP *bitmap, int x, int y, int width, int height,
     *         int format, int flags));
     */
    public Pointer al_lock_bitmap_region(
            Pointer bitmap,
            int x, int y, int width, int height,
            int format, int flags);

    /**
     * AL_FUNC(ALLEGRO_LOCKED_REGION*, al_lock_bitmap_blocked,
     *         (ALLEGRO_BITMAP *bitmap, int flags));
     */
    public Pointer al_lock_bitmap_blocked(Pointer bitmap, int flags);

    /**
     * AL_FUNC(ALLEGRO_LOCKED_REGION*, al_lock_bitmap_region_blocked,
     *         (ALLEGRO_BITMAP *bitmap, int x_block, int y_block,
     *         int width_block, int height_block, int flags));
     */
    public Pointer al_lock_bitmap_region_blocked(
            Pointer bitmap,
            int x_block, int y_block,
            int width_block, int height_block, int flags);

    /**
     * AL_FUNC(void, al_unlock_bitmap, (ALLEGRO_BITMAP *bitmap));
     */
    public void al_unlock_bitmap(Pointer bitmap);

    /**
     * AL_FUNC(bool, al_is_bitmap_locked, (ALLEGRO_BITMAP *bitmap));
     */
    public boolean al_is_bitmap_locked(Pointer bitmap);

    //--------------------------------------------------------------------------
    // bitmap.h
    //--------------------------------------------------------------------------

    /*
     * @see AlgrBitmapFlag
     */

    /**
     * The default format is 0 and means the display driver will choose the best format.
     * 
     * AL_FUNC(void, al_set_new_bitmap_format, (int format));
     */
    public void al_set_new_bitmap_format(int format);

    /**
     * AL_FUNC(void, al_set_new_bitmap_flags, (int flags));
     */
    public void al_set_new_bitmap_flags(int flags);

    /**
     * AL_FUNC(int, al_get_new_bitmap_format, (void));
     */
    public int al_get_new_bitmap_format();

    /**
     * AL_FUNC(int, al_get_new_bitmap_flags, (void));
     */
    public int al_get_new_bitmap_flags();

    /**
     * AL_FUNC(void, al_add_new_bitmap_flag, (int flag));
     */
    public void al_add_new_bitmap_flag(int flags);

    /**
     * AL_FUNC(int, al_get_bitmap_width, (ALLEGRO_BITMAP *bitmap));
     */
    public int al_get_bitmap_width(Pointer bitmap);

    /**
     * AL_FUNC(int, al_get_bitmap_height, (ALLEGRO_BITMAP *bitmap));
     */
    public int al_get_bitmap_height(Pointer bitmap);

    /**
     * AL_FUNC(int, al_get_bitmap_format, (ALLEGRO_BITMAP *bitmap));
     */
    public int al_get_bitmap_format(Pointer bitmap);

    /**
     * AL_FUNC(int, al_get_bitmap_flags, (ALLEGRO_BITMAP *bitmap));
     */
    public int al_get_bitmap_flags(Pointer bitmap);

    /**
     * AL_FUNC(ALLEGRO_BITMAP*, al_create_bitmap, (int w, int h));
     */
    public Pointer al_create_bitmap(int w, int h);

    /**
     * AL_FUNC(void, al_destroy_bitmap, (ALLEGRO_BITMAP *bitmap));
     */
    public void al_destroy_bitmap(Pointer bitmap);

    /**
     * AL_FUNC(void, al_put_pixel, (int x, int y, ALLEGRO_COLOR color));
     */
    public void al_put_pixel(int x, int y, ALLEGRO_COLOR color);

    /**
     * AL_FUNC(void, al_put_blended_pixel, (int x, int y, ALLEGRO_COLOR color));
     */
    public void al_put_blended_pixel(int x, int y, ALLEGRO_COLOR color);

    /**
     * AL_FUNC(ALLEGRO_COLOR, al_get_pixel, (ALLEGRO_BITMAP *bitmap, int x, int y));
     */
    public Pointer al_get_pixel(Pointer bitmap, int x, int y);

    /*
     * Masking
     */

    /**
     * AL_FUNC(void, al_convert_mask_to_alpha, (ALLEGRO_BITMAP *bitmap, ALLEGRO_COLOR mask_color));
     */
    public void al_convert_mask_to_alpha(Pointer bitmap, ALLEGRO_COLOR mask_color);

    /*
     * Clipping
     */

    /**
     * AL_FUNC(void, al_set_clipping_rectangle, (int x, int y, int width, int height));
     */
    public void al_set_clipping_rectangle(int x, int y, int width, int height);

    /**
     * AL_FUNC(void, al_reset_clipping_rectangle, (void));
     */
    public void al_reset_clipping_rectangle();

    /**
     * AL_FUNC(void, al_get_clipping_rectangle, (int *x, int *y, int *w, int *h));
     */
    public void al_get_clipping_rectangle(IntByReference x, IntByReference y, IntByReference w, IntByReference h);

    //--------------------------------------------------------------------------
    // keyboard.h
    //--------------------------------------------------------------------------

    /**
     * AL_FUNC(bool,         al_is_keyboard_installed,   (void));
     */
    public boolean al_is_keyboard_installed();
    
    /**
     * AL_FUNC(bool,         al_install_keyboard,   (void));
     */
    public boolean al_install_keyboard();
    
    /**
     * AL_FUNC(void,         al_uninstall_keyboard, (void));
     */
    public void al_uninstall_keyboard();

    /**
     * AL_FUNC(bool,         al_set_keyboard_leds,  (int leds));
     */
    public boolean al_set_keyboard_leds(int leds);

    /**
     * AL_FUNC(const char *, al_keycode_to_name, (int keycode));
     */
    public String al_keycode_to_name(int keycode);

    /**
     * NB: This state might not be a fresh hardware state,
     * but an internal state maintained by Allegro depending
     * on the events received by its focused windows.
     * 
     * AL_FUNC(void,         al_get_keyboard_state, (ALLEGRO_KEYBOARD_STATE *ret_state));
     */
    public void al_get_keyboard_state(ALLEGRO_KEYBOARD_STATE ret_state);
    
    /**
     * AL_FUNC(bool,         al_key_down,           (const ALLEGRO_KEYBOARD_STATE *, int keycode));
     */
    public boolean al_key_down(ALLEGRO_KEYBOARD_STATE state, int keycode);

    /**
     * AL_FUNC(ALLEGRO_EVENT_SOURCE *, al_get_keyboard_event_source, (void));
     */
    public Pointer al_get_keyboard_event_source();

    //--------------------------------------------------------------------------
    // mouse.h
    //--------------------------------------------------------------------------

    /**
     * Allow up to four extra axes for future expansion.
     */
    public static final int ALLEGRO_MOUSE_MAX_EXTRA_AXES = ALLEGRO_MOUSE_STATE.ALLEGRO_MOUSE_MAX_EXTRA_AXES;

    public static final int ALGR_PRIMARY_MOUSE_BUTTON = 1;
    public static final int ALGR_MIDDLE_MOUSE_BUTTON = 3;
    public static final int ALGR_SECONDARY_MOUSE_BUTTON = 2;

    public static final int ALGR_PRIMARY_MOUSE_BUTTON_MASK = (1 << (ALGR_PRIMARY_MOUSE_BUTTON - 1));
    public static final int ALGR_MIDDLE_MOUSE_BUTTON_MASK = (1 << (ALGR_MIDDLE_MOUSE_BUTTON - 1));
    public static final int ALGR_SECONDARY_MOUSE_BUTTON_MASK = (1 << (ALGR_SECONDARY_MOUSE_BUTTON - 1));

    /**
     * AL_FUNC(bool,           al_is_mouse_installed,  (void));
     */
    public boolean al_is_mouse_installed();
    
    /**
     * AL_FUNC(bool,           al_install_mouse,       (void));
     */
    public boolean al_install_mouse();
    
    /**
     * AL_FUNC(void,           al_uninstall_mouse,     (void));
     */
    public boolean al_uninstall_mouse();
    
    /**
     * AL_FUNC(unsigned int,   al_get_mouse_num_buttons, (void));
     */
    public int al_get_mouse_num_buttons();
    
    /**
     * AL_FUNC(unsigned int,   al_get_mouse_num_axes,  (void));
     */
    public int al_get_mouse_num_axes();
    
    /**
     * AL_FUNC(bool,           al_set_mouse_xy,        (struct ALLEGRO_DISPLAY *display, int x, int y));
     */
    public boolean al_set_mouse_xy(Pointer display, int x, int y);
    
    /**
     * AL_FUNC(bool,           al_set_mouse_z,         (int z));
     */
    public boolean al_set_mouse_z(int z);
    
    /**
     * AL_FUNC(bool,           al_set_mouse_w,         (int w));
     */
    public boolean al_set_mouse_w(int w);
    
    /**
     * AL_FUNC(bool,           al_set_mouse_axis,      (int axis, int value));
     */
    public boolean al_set_mouse_axis(int axis, int value);
    
    /**
     * AL_FUNC(void,           al_get_mouse_state,     (ALLEGRO_MOUSE_STATE *ret_state));
     */
    public void al_get_mouse_state(ALLEGRO_MOUSE_STATE ret_state);
    
    /**
     * AL_FUNC(bool,           al_mouse_button_down,   (const ALLEGRO_MOUSE_STATE *state, int button));
     */
    public boolean al_mouse_button_down(ALLEGRO_MOUSE_STATE state, int button);
    
    /**
     * AL_FUNC(int,            al_get_mouse_state_axis, (const ALLEGRO_MOUSE_STATE *state, int axis));
     */
    public int al_get_mouse_state_axis(ALLEGRO_MOUSE_STATE state, int axis);
    
    /**
     * AL_FUNC(bool, al_get_mouse_cursor_position, (int *ret_x, int *ret_y));
     */
    public boolean al_get_mouse_cursor_position(IntByReference ret_x, IntByReference ret_y);
    
    /**
     * AL_FUNC(bool, al_grab_mouse, (struct ALLEGRO_DISPLAY *display));
     */
    public boolean al_grab_mouse(Pointer display);
    
    /**
     * AL_FUNC(bool, al_ungrab_mouse, (void));
     */
    public boolean al_ungrab_mouse();
    
    /**
     * AL_FUNC(void, al_set_mouse_wheel_precision, (int precision));
     */
    public void al_set_mouse_wheel_precision(int precision);
    
    /**
     * AL_FUNC(int, al_get_mouse_wheel_precision, (void));
     */
    public int al_get_mouse_wheel_precision();

    /**
     * AL_FUNC(ALLEGRO_EVENT_SOURCE *, al_get_mouse_event_source, (void));
     */
    public Pointer al_get_mouse_event_source();

    //--------------------------------------------------------------------------
    // mouse_cursor.h
    //--------------------------------------------------------------------------

    /**
     * AL_FUNC(ALLEGRO_MOUSE_CURSOR *, al_create_mouse_cursor, (
     *         struct ALLEGRO_BITMAP *sprite, int xfocus, int yfocus));
     */
    public Pointer al_create_mouse_cursor(Pointer sprite, int xfocus, int yfocus);

    /**
     * AL_FUNC(void, al_destroy_mouse_cursor, (ALLEGRO_MOUSE_CURSOR *));
     */
    public void al_destroy_mouse_cursor(Pointer cursor);

    /**
     * AL_FUNC(bool, al_set_mouse_cursor, (struct ALLEGRO_DISPLAY *display,
     *                                     ALLEGRO_MOUSE_CURSOR *cursor));
     */
    public boolean al_set_mouse_cursor(Pointer display, Pointer cursor);

    /**
     * AL_FUNC(bool, al_set_system_mouse_cursor, (struct ALLEGRO_DISPLAY *display,
     *                                            ALLEGRO_SYSTEM_MOUSE_CURSOR cursor_id));
     */
    public boolean al_set_system_mouse_cursor(Pointer display, int cursor_id);

    /**
     * AL_FUNC(bool, al_show_mouse_cursor, (struct ALLEGRO_DISPLAY *display));
     */
    public boolean al_show_mouse_cursor(Pointer display);

    /**
     * AL_FUNC(bool, al_hide_mouse_cursor, (struct ALLEGRO_DISPLAY *display));
     */
    public boolean al_hide_mouse_cursor(Pointer display);

    //--------------------------------------------------------------------------
    // clipboard.h
    //--------------------------------------------------------------------------

    /**
     * AL_FUNC(char *, al_get_clipboard_text, (ALLEGRO_DISPLAY *display));
     */
    public String al_get_clipboard_text(Pointer display);

    /**
     * AL_FUNC(bool, al_set_clipboard_text, (ALLEGRO_DISPLAY *display, const char *text));
     */
    public boolean al_set_clipboard_text(Pointer display, String text);

    /**
     * AL_FUNC(bool, al_clipboard_has_text, (ALLEGRO_DISPLAY *display));
     */
    public boolean al_clipboard_has_text(Pointer display);

    //--------------------------------------------------------------------------
    // events.h
    //--------------------------------------------------------------------------

    /**
     * Not present in Allegro API, but in its documentation.
     */
    public static final int ALLEGRO_UNRESERVED_USER_EVENT_MIN = 1024;

    /*
     * Event sources.
     */

    /**
     * AL_FUNC(void, al_init_user_event_source, (ALLEGRO_EVENT_SOURCE *));
     */
    public void al_init_user_event_source(ALLEGRO_EVENT_SOURCE event_source);
    
    /**
     * AL_FUNC(void, al_destroy_user_event_source, (ALLEGRO_EVENT_SOURCE *));
     */
    public void al_destroy_user_event_source(ALLEGRO_EVENT_SOURCE event_source);
    
    /**
     * / * The second argument is ALLEGRO_EVENT instead of ALLEGRO_USER_EVENT
     *  * to prevent users passing a pointer to a too-short structure.
     *  * /
     * AL_FUNC(bool, al_emit_user_event, (ALLEGRO_EVENT_SOURCE *, ALLEGRO_EVENT *,
     *                                    void (*dtor)(ALLEGRO_USER_EVENT *)));
     */
    public boolean al_emit_user_event(ALLEGRO_EVENT_SOURCE event_source, ALLEGRO_EVENT event, InterfaceAlgrUserEventDtor dtor);
    
    /**
     * AL_FUNC(void, al_unref_user_event, (ALLEGRO_USER_EVENT *));
     */
    public void al_unref_user_event(ALLEGRO_USER_EVENT user_event);
    
    /**
     * AL_FUNC(void, al_set_event_source_data, (ALLEGRO_EVENT_SOURCE*, intptr_t data));
     */
    public void al_set_event_source_data(ALLEGRO_EVENT_SOURCE event_source, IntByReference data);
    
    /**
     * AL_FUNC(intptr_t, al_get_event_source_data, (const ALLEGRO_EVENT_SOURCE*));
     */
    public IntByReference al_get_event_source_data(ALLEGRO_EVENT_SOURCE event_source);

    /*
     * Event queues.
     */

    /**
     * AL_FUNC(ALLEGRO_EVENT_QUEUE*, al_create_event_queue, (void));
     */
    public Pointer al_create_event_queue();
    
    /**
     * AL_FUNC(void, al_destroy_event_queue, (ALLEGRO_EVENT_QUEUE*));
     */
    public void al_destroy_event_queue(Pointer event_queue);
    
    /**
     * AL_FUNC(bool, al_is_event_source_registered, (ALLEGRO_EVENT_QUEUE *, ALLEGRO_EVENT_SOURCE *));
     */
    public boolean al_is_event_source_registered(Pointer event_queue, Pointer event_source);
    
    /**
     * AL_FUNC(void, al_register_event_source, (ALLEGRO_EVENT_QUEUE*, ALLEGRO_EVENT_SOURCE*));
     * 
     * Register the event source with the event queue specified.
     * An event source may be registered with any number of event queues
     * simultaneously, or none.
     * Trying to register an event source with the same event queue more than
     * once does nothing.
     * 
     * See also: al_unregister_event_source, ALLEGRO_EVENT_SOURCE
     */
    public void al_register_event_source(Pointer event_queue, Pointer event_source);
    
    /**
     * AL_FUNC(void, al_unregister_event_source, (ALLEGRO_EVENT_QUEUE*, ALLEGRO_EVENT_SOURCE*));
     */
    public void al_unregister_event_source(Pointer event_queue, Pointer event_source);
    
    /**
     * AL_FUNC(void, al_pause_event_queue, (ALLEGRO_EVENT_QUEUE*, bool));
     */
    public void al_pause_event_queue(Pointer event_queue, boolean pause);
    
    /**
     * AL_FUNC(bool, al_is_event_queue_paused, (const ALLEGRO_EVENT_QUEUE*));
     */
    public boolean al_is_event_queue_paused(Pointer event_queue);
    
    /**
     * AL_FUNC(bool, al_is_event_queue_empty, (ALLEGRO_EVENT_QUEUE*));
     */
    public boolean al_is_event_queue_empty(Pointer event_queue);
    
    /**
     * AL_FUNC(bool, al_get_next_event, (ALLEGRO_EVENT_QUEUE*, ALLEGRO_EVENT *ret_event));
     */
    public boolean al_get_next_event(Pointer event_queue, ALLEGRO_EVENT ret_event);
    
    /**
     * AL_FUNC(bool, al_peek_next_event, (ALLEGRO_EVENT_QUEUE*, ALLEGRO_EVENT *ret_event));
     */
    public boolean al_peek_next_event(Pointer event_queue, ALLEGRO_EVENT ret_event);
    
    /**
     * AL_FUNC(bool, al_drop_next_event, (ALLEGRO_EVENT_QUEUE*));
     */
    public boolean al_drop_next_event(Pointer event_queue);
    
    /**
     * AL_FUNC(void, al_flush_event_queue, (ALLEGRO_EVENT_QUEUE*));
     */
    public void al_flush_event_queue(Pointer event_queue);
    
    /**
     * AL_FUNC(void, al_wait_for_event, (ALLEGRO_EVENT_QUEUE*, ALLEGRO_EVENT *ret_event));
     */
    public void al_wait_for_event(Pointer event_queue, ALLEGRO_EVENT ret_event);
    
    /**
     * AL_FUNC(bool, al_wait_for_event_timed, (ALLEGRO_EVENT_QUEUE*, ALLEGRO_EVENT *ret_event, float secs));
     */
    public boolean al_wait_for_event_timed(Pointer event_queue, ALLEGRO_EVENT ret_event, float secs);
    
    /**
     * AL_FUNC(bool, al_wait_for_event_until, (ALLEGRO_EVENT_QUEUE *queue, ALLEGRO_EVENT *ret_event, ALLEGRO_TIMEOUT *timeout));
     */
    public boolean al_wait_for_event_until(Pointer event_queue, ALLEGRO_EVENT ret_event, ALLEGRO_TIMEOUT timeout);

    //--------------------------------------------------------------------------
    // system.h
    //--------------------------------------------------------------------------

    /**
     * AL_FUNC(bool, al_install_system, (int version, int (*atexit_ptr)(void (*)(void))));
     */
    public boolean al_install_system(int version, InterfaceAlgrAtExit atexit_ptr);
    
    /**
     * AL_FUNC(void, al_uninstall_system, (void));
     */
    public void al_uninstall_system();
    
    /**
     * AL_FUNC(boolean, al_is_system_installed, (void));
     */
    public boolean al_is_system_installed();

    /**
     * AL_FUNC(bool, al_inhibit_screensaver, (bool inhibit));
     */
    public boolean al_inhibit_screensaver(boolean inhibit);

    //--------------------------------------------------------------------------
    // error.h
    //--------------------------------------------------------------------------

    /**
     * AL_FUNC(int, al_get_errno, (void));
     */
    public int al_get_errno();

    //--------------------------------------------------------------------------
    // debug.h
    //--------------------------------------------------------------------------

    public void al_register_trace_handler(InterfaceAlgrTraceHandler handler);
}
