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
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * Java binding for (a subset of) SDL2.
 */
public interface SdlJnaLib extends Library {

    public static final SdlJnaLib INSTANCE = (SdlJnaLib) Native.loadLibrary("SDL2", SdlJnaLib.class);
    
    //--------------------------------------------------------------------------
    // SDL_surface.h
    //--------------------------------------------------------------------------

    /**
     *  Allocate and free an RGB surface.
     *
     *  If the depth is 4 or 8 bits, an empty palette is allocated for the surface.
     *  If the depth is greater than 8 bits, the pixel format is set using the
     *  flags '[RGB]mask'.
     *
     *  If the function runs out of memory, it will return NULL.
     *
     *  \param flags The \c flags are obsolete and should be set to 0.
     *  \param width The width in pixels of the surface to create.
     *  \param height The height in pixels of the surface to create.
     *  \param depth The depth in bits of the surface to create.
     *  \param Rmask The red mask of the surface to create.
     *  \param Gmask The green mask of the surface to create.
     *  \param Bmask The blue mask of the surface to create.
     *  \param Amask The alpha mask of the surface to create.
     * 
     * extern DECLSPEC SDL_Surface *SDLCALL SDL_CreateRGBSurface(Uint32 flags, int width, int height, int depth, Uint32 Rmask, Uint32 Gmask, Uint32 Bmask, Uint32 Amask);
     * 
     * @see SdlSurfaceFlag
     * 
     * @param flags Bitwise or of {SDL_PREALLOC, SDL_RLEACCEL, SDL_DONTFREE}.
     * @param depth Number of bits per pixel.
     * @param pitch Number of bytes per row.
     */
    public Pointer SDL_CreateRGBSurface(
            int flags, int width, int height, int depth,
            int Rmask, int Gmask, int Bmask, int Amask);
    
    /**
     * extern DECLSPEC void SDLCALL SDL_FreeSurface(SDL_Surface * surface);
     */
    public void SDL_FreeSurface(SDL_Surface surface);
    
    /**
     *  \brief Sets up a surface for directly accessing the pixels.
     *
     *  Between calls to SDL_LockSurface() / SDL_UnlockSurface(), you can write
     *  to and read from \c surface->pixels, using the pixel format stored in
     *  \c surface->format.  Once you are done accessing the surface, you should
     *  use SDL_UnlockSurface() to release it.
     *
     *  Not all surfaces require locking.  If SDL_MUSTLOCK(surface) evaluates
     *  to 0, then you can read and write to the surface at any time, and the
     *  pixel format of the surface will not change.
     *
     *  No operating system or library calls should be made between lock/unlock
     *  pairs, as critical system locks may be held during this time.
     *
     *  SDL_LockSurface() returns 0, or -1 if the surface couldn't be locked.
     * 
     * extern DECLSPEC int SDLCALL SDL_LockSurface(SDL_Surface * surface);
     * 
     * @return 0, or -1 if the surface couldn't be locked.
     */
    public int SDL_LockSurface(SDL_Surface surface);
    
    /**
     * extern DECLSPEC void SDLCALL SDL_UnlockSurface(SDL_Surface * surface);
     */
    public void SDL_UnlockSurface(SDL_Surface surface);
    
    //--------------------------------------------------------------------------
    // SDL_video.h
    //--------------------------------------------------------------------------
    
    /**
     * \brief Get the desktop area represented by a display, with the primary
     *        display located at 0,0
     *
     * \return 0 on success, or -1 if the index is out of range.
     *
     * \sa SDL_GetNumVideoDisplays()
     * extern DECLSPEC int SDLCALL SDL_GetDisplayBounds(int displayIndex, SDL_Rect * rect);
     */
    public int SDL_GetDisplayBounds(int displayIndex, SDL_Rect rect);
    
    /**
     * \brief Get the dots/pixels-per-inch for a display
     *
     * \note Diagonal, horizontal and vertical DPI can all be optionally
     *       returned if the parameter is non-NULL.
     *
     * \return 0 on success, or -1 if no DPI information is available or the index is out of range.
     *
     * \sa SDL_GetNumVideoDisplays()
     * 
     * extern DECLSPEC int SDLCALL SDL_GetDisplayDPI(int displayIndex, float * ddpi, float * hdpi, float * vdpi);
     */
    public int SDL_GetDisplayDPI(int displayIndex, FloatByReference ddpi, FloatByReference hdpi, FloatByReference vdpi);
    
    /**
     *  \brief Get the usable desktop area represented by a display, with the
     *         primary display located at 0,0
     *
     *  This is the same area as SDL_GetDisplayBounds() reports, but with portions
     *  reserved by the system removed. For example, on Mac OS X, this subtracts
     *  the area occupied by the menu bar and dock.
     *
     *  Setting a window to be fullscreen generally bypasses these unusable areas,
     *  so these are good guidelines for the maximum space available to a
     *  non-fullscreen window.
     *
     *  \return 0 on success, or -1 if the index is out of range.
     *
     *  \sa SDL_GetDisplayBounds()
     *  \sa SDL_GetNumVideoDisplays()
     * 
     * extern DECLSPEC int SDLCALL SDL_GetDisplayUsableBounds(int displayIndex, SDL_Rect * rect);
     */
    public int SDL_GetDisplayUsableBounds(int displayIndex, SDL_Rect rect);
    
    /**
     *  \brief Create a window with the specified position, dimensions, and flags.
     *
     *  \param title The title of the window, in UTF-8 encoding.
     *  \param x     The x position of the window, ::SDL_WINDOWPOS_CENTERED, or
     *               ::SDL_WINDOWPOS_UNDEFINED.
     *  \param y     The y position of the window, ::SDL_WINDOWPOS_CENTERED, or
     *               ::SDL_WINDOWPOS_UNDEFINED.
     *  \param w     The width of the window, in screen coordinates.
     *  \param h     The height of the window, in screen coordinates.
     *  \param flags The flags for the window, a mask of any of the following:
     *               ::SDL_WINDOW_FULLSCREEN,    ::SDL_WINDOW_OPENGL,
     *               ::SDL_WINDOW_HIDDEN,        ::SDL_WINDOW_BORDERLESS,
     *               ::SDL_WINDOW_RESIZABLE,     ::SDL_WINDOW_MAXIMIZED,
     *               ::SDL_WINDOW_MINIMIZED,     ::SDL_WINDOW_INPUT_GRABBED,
     *               ::SDL_WINDOW_ALLOW_HIGHDPI.
     *
     *  \return The id of the window created, or zero if window creation failed.
     *
     *  If the window is created with the SDL_WINDOW_ALLOW_HIGHDPI flag, its size
     *  in pixels may differ from its size in screen coordinates on platforms with
     *  high-DPI support (e.g. iOS and Mac OS X). Use SDL_GetWindowSize() to query
     *  the client area's size in screen coordinates, and SDL_GL_GetDrawableSize()
     *  or SDL_GetRendererOutputSize() to query the drawable size in pixels.
     *
     *  \sa SDL_DestroyWindow()
     * 
     * extern DECLSPEC SDL_Window * SDLCALL SDL_CreateWindow(const char *title, int x, int y, int w, int h, Uint32 flags);
     */
    public Pointer SDL_CreateWindow(String title, int x, int y, int w, int h, int flags);
    
    /**
     *  \brief Get the numeric ID of a window, for logging purposes.
     * 
     * extern DECLSPEC Uint32 SDLCALL SDL_GetWindowID(SDL_Window * window);
     */
    public int SDL_GetWindowID(Pointer window);
    
    /**
     *  \brief Get the window flags.
     * 
     * extern DECLSPEC Uint32 SDLCALL SDL_GetWindowFlags(SDL_Window * window);
     */
    public int SDL_GetWindowFlags(Pointer window);
    
    /**
     *  \brief Set the position of a window.
     *
     *  \param window   The window to reposition.
     *  \param x        The x coordinate of the window in screen coordinates, or
     *                  ::SDL_WINDOWPOS_CENTERED or ::SDL_WINDOWPOS_UNDEFINED.
     *  \param y        The y coordinate of the window in screen coordinates, or
     *                  ::SDL_WINDOWPOS_CENTERED or ::SDL_WINDOWPOS_UNDEFINED.
     *
     *  \note The window coordinate origin is the upper left of the display.
     *
     *  \sa SDL_GetWindowPosition()
     * 
     * extern DECLSPEC void SDLCALL SDL_SetWindowPosition(SDL_Window * window, int x, int y);
     */
    public void SDL_SetWindowPosition(Pointer window, int x, int y);

    /**
     *  \brief Get the position of a window.
     *
     *  \param window   The window to query.
     *  \param x        Pointer to variable for storing the x position, in screen
     *                  coordinates. May be NULL.
     *  \param y        Pointer to variable for storing the y position, in screen
     *                  coordinates. May be NULL.
     *
     *  \sa SDL_SetWindowPosition()
     * 
     * extern DECLSPEC void SDLCALL SDL_GetWindowPosition(SDL_Window * window, int *x, int *y);
     */
    public void SDL_GetWindowPosition(Pointer window, IntByReference x, IntByReference y);
    
    /**
     *  \brief Set the size of a window's client area.
     *
     *  \param window   The window to resize.
     *  \param w        The width of the window, in screen coordinates. Must be >0.
     *  \param h        The height of the window, in screen coordinates. Must be >0.
     *
     *  \note You can't change the size of a fullscreen window, it automatically
     *        matches the size of the display mode.
     *
     *  The window size in screen coordinates may differ from the size in pixels, if
     *  the window was created with SDL_WINDOW_ALLOW_HIGHDPI on a platform with
     *  high-dpi support (e.g. iOS or OS X). Use SDL_GL_GetDrawableSize() or
     *  SDL_GetRendererOutputSize() to get the real client area size in pixels.
     *
     *  \sa SDL_GetWindowSize()
     * 
     * extern DECLSPEC void SDLCALL SDL_SetWindowSize(SDL_Window * window, int w, int h);
     */
    public void SDL_SetWindowSize(Pointer window, int w, int h);
    
    /**
     *  \brief Get the size of a window's client area.
     *
     *  \param window   The window to query.
     *  \param w        Pointer to variable for storing the width, in screen
     *                  coordinates. May be NULL.
     *  \param h        Pointer to variable for storing the height, in screen
     *                  coordinates. May be NULL.
     *
     *  The window size in screen coordinates may differ from the size in pixels, if
     *  the window was created with SDL_WINDOW_ALLOW_HIGHDPI on a platform with
     *  high-dpi support (e.g. iOS or OS X). Use SDL_GL_GetDrawableSize() or
     *  SDL_GetRendererOutputSize() to get the real client area size in pixels.
     *
     *  \sa SDL_SetWindowSize()
     * 
     * extern DECLSPEC void SDLCALL SDL_GetWindowSize(SDL_Window * window, int *w, int *h);
     */
    public void SDL_GetWindowSize(Pointer window, IntByReference w, IntByReference h);
    
    /**
     *  \brief Get the size of a window's borders (decorations) around the client area.
     *
     *  \param window The window to query.
     *  \param top Pointer to variable for storing the size of the top border. NULL is permitted.
     *  \param left Pointer to variable for storing the size of the left border. NULL is permitted.
     *  \param bottom Pointer to variable for storing the size of the bottom border. NULL is permitted.
     *  \param right Pointer to variable for storing the size of the right border. NULL is permitted.
     *
     *  \return 0 on success, or -1 if getting this information is not supported.
     *
     *  \note if this function fails (returns -1), the size values will be
     *        initialized to 0, 0, 0, 0 (if a non-NULL pointer is provided), as
     *        if the window in question was borderless.
     * 
     * extern DECLSPEC int SDLCALL SDL_GetWindowBordersSize(SDL_Window * window, int *top, int *left, int *bottom, int *right);
     */
    public int SDL_GetWindowBordersSize(Pointer window, IntByReference top, IntByReference left, IntByReference bottom, IntByReference right);
    
    /**
     *  \brief Set the minimum size of a window's client area.
     *
     *  \param window    The window to set a new minimum size.
     *  \param min_w     The minimum width of the window, must be >0
     *  \param min_h     The minimum height of the window, must be >0
     *
     *  \note You can't change the minimum size of a fullscreen window, it
     *        automatically matches the size of the display mode.
     *
     *  \sa SDL_GetWindowMinimumSize()
     *  \sa SDL_SetWindowMaximumSize()
     * 
     * extern DECLSPEC void SDLCALL SDL_SetWindowMinimumSize(SDL_Window * window, int min_w, int min_h);
     */
    public void SDL_SetWindowMinimumSize(Pointer window, int min_w, int min_h);
    
    /**
     *  \brief Show a window.
     *
     *  \sa SDL_HideWindow()
     * 
     * extern DECLSPEC void SDLCALL SDL_ShowWindow(SDL_Window * window);
     */
    public void SDL_ShowWindow(Pointer window);
    
    /**
     *  \brief Hide a window.
     *
     *  \sa SDL_ShowWindow()
     * 
     * extern DECLSPEC void SDLCALL SDL_HideWindow(SDL_Window * window);
     */
    public void SDL_HideWindow(Pointer window);
    
    /**
     *  \brief Raise a window above other windows and set the input focus.
     * 
     * extern DECLSPEC void SDLCALL SDL_RaiseWindow(SDL_Window * window);
     */
    public void SDL_RaiseWindow(Pointer window);
    
    /**
     * Since 2.0.5
     * Works only on W11.
     * @return 0 if success, another value if error.
     */
    public int SDL_SetWindowInputFocus(Pointer window);
    
    /**
     *  \brief Make a window as large as possible.
     *
     *  \sa SDL_RestoreWindow()
     * 
     * extern DECLSPEC void SDLCALL SDL_MaximizeWindow(SDL_Window * window);
     */
    public void SDL_MaximizeWindow(Pointer window);

    /**
     *  \brief Minimize a window to an iconic representation.
     *
     *  \sa SDL_RestoreWindow()
     * 
     * extern DECLSPEC void SDLCALL SDL_MinimizeWindow(SDL_Window * window);
     */
    public void SDL_MinimizeWindow(Pointer window);

    /**
     *  \brief Restore the size and position of a minimized or maximized window.
     *
     *  \sa SDL_MaximizeWindow()
     *  \sa SDL_MinimizeWindow()
     * 
     * extern DECLSPEC void SDLCALL SDL_RestoreWindow(SDL_Window * window);
     */
    public void SDL_RestoreWindow(Pointer window);

    /**
     *  \brief Set a window's fullscreen state.
     *
     *  \return 0 on success, or -1 if setting the display mode failed.
     *
     *  \sa SDL_SetWindowDisplayMode()
     *  \sa SDL_GetWindowDisplayMode()
     * 
     * extern DECLSPEC int SDLCALL SDL_SetWindowFullscreen(SDL_Window * window, Uint32 flags);
     */
    public int SDL_SetWindowFullscreen(Pointer window, int flags);
    
    /**
     *  \brief Get the SDL surface associated with the window.
     *
     *  \return The window's framebuffer surface, or NULL on error.
     *
     *  A new surface will be created with the optimal format for the window,
     *  if necessary. This surface will be freed when the window is destroyed.
     *
     *  \note You may not combine this with 3D or the rendering API on this window.
     *
     *  \sa SDL_UpdateWindowSurface()
     *  \sa SDL_UpdateWindowSurfaceRects()
     * 
     * extern DECLSPEC SDL_Surface * SDLCALL SDL_GetWindowSurface(SDL_Window * window);
     */
    public Pointer SDL_GetWindowSurface(Pointer window);

    /**
     *  \brief Copy the window surface to the screen.
     *
     *  \return 0 on success, or -1 on error.
     *
     *  \sa SDL_GetWindowSurface()
     *  \sa SDL_UpdateWindowSurfaceRects()
     * 
     * extern DECLSPEC int SDLCALL SDL_UpdateWindowSurface(SDL_Window * window);
     */
    public int SDL_UpdateWindowSurface(Pointer window);

    /**
     *  \brief Copy a number of rectangles on the window surface to the screen.
     *
     *  \return 0 on success, or -1 on error.
     *
     *  \sa SDL_GetWindowSurface()
     *  \sa SDL_UpdateWindowSurfaceRect()
     * 
     * extern DECLSPEC int SDLCALL SDL_UpdateWindowSurfaceRects(SDL_Window * window, const SDL_Rect * rects, int numrects);
     */
    public int SDL_UpdateWindowSurfaceRects(Pointer window, SDL_Rect[] rects, int numrects);

    /**
     *  \brief Set the opacity for a window
     *
     *  \param window The window which will be made transparent or opaque
     *  \param opacity Opacity (0.0f - transparent, 1.0f - opaque) This will be
     *                 clamped internally between 0.0f and 1.0f.
     * 
     *  \return 0 on success, or -1 if setting the opacity isn't supported.
     *
     *  \sa SDL_GetWindowOpacity()
     * 
     * extern DECLSPEC int SDLCALL SDL_SetWindowOpacity(SDL_Window * window, float opacity);
     */
    public int SDL_SetWindowOpacity(Pointer window, float opacity);

    /**
     *  \brief Get the opacity of a window.
     *
     *  If transparency isn't supported on this platform, opacity will be reported
     *  as 1.0f without error.
     *
     *  \param window The window in question.
     *  \param out_opacity Opacity (0.0f - transparent, 1.0f - opaque)
     *
     *  \return 0 on success, or -1 on error (invalid window, etc).
     *
     *  \sa SDL_SetWindowOpacity()
     * 
     * extern DECLSPEC int SDLCALL SDL_GetWindowOpacity(SDL_Window * window, float * out_opacity);
     */
    public int SDL_GetWindowOpacity(Pointer window, FloatByReference out_opacity);

    /**
     *  \brief Sets the window as a modal for another window
     *
     *  \param modal_window The window that should be modal
     *  \param parent_window The parent window
     * 
     *  \return 0 on success, or -1 otherwise.
     * 
     * extern DECLSPEC int SDLCALL SDL_SetWindowModalFor(SDL_Window * modal_window, SDL_Window * parent_window);
     */
    public int SDL_SetWindowModalFor(Pointer modal_window, Pointer parent_window);

    /**
     *  \brief Destroy a window.
     * 
     * extern DECLSPEC void SDLCALL SDL_DestroyWindow(SDL_Window * window);
     */
    public void SDL_DestroyWindow(Pointer window);

    //--------------------------------------------------------------------------
    // SDL_mouse.h
    //--------------------------------------------------------------------------

    public static final int SDL_BUTTON_LEFT = 1;
    public static final int SDL_BUTTON_MIDDLE = 2;
    public static final int SDL_BUTTON_RIGHT = 3;
    public static final int SDL_BUTTON_X1 = 4;
    public static final int SDL_BUTTON_X2 = 5;
    
    public static final int SDL_BUTTON_LMASK = SdlStatics.SDL_BUTTON(SDL_BUTTON_LEFT);
    public static final int SDL_BUTTON_MMASK = SdlStatics.SDL_BUTTON(SDL_BUTTON_MIDDLE);
    public static final int SDL_BUTTON_RMASK = SdlStatics.SDL_BUTTON(SDL_BUTTON_RIGHT);
    public static final int SDL_BUTTON_X1MASK = SdlStatics.SDL_BUTTON(SDL_BUTTON_X1);
    public static final int SDL_BUTTON_X2MASK = SdlStatics.SDL_BUTTON(SDL_BUTTON_X2);

    /**
     *  \brief Get the current state of the mouse, in relation to the desktop
     *
     *  This works just like SDL_GetMouseState(), but the coordinates will be
     *  reported relative to the top-left of the desktop. This can be useful if
     *  you need to track the mouse outside of a specific window and
     *  SDL_CaptureMouse() doesn't fit your needs. For example, it could be
     *  useful if you need to track the mouse while dragging a window, where
     *  coordinates relative to a window might not be in sync at all times.
     *
     *  \note SDL_GetMouseState() returns the mouse position as SDL understands
     *        it from the last pump of the event queue. This function, however,
     *        queries the OS for the current mouse position, and as such, might
     *        be a slightly less efficient function. Unless you know what you're
     *        doing and have a good reason to use this function, you probably want
     *        SDL_GetMouseState() instead.
     *
     *  \param x Returns the current X coord, relative to the desktop. Can be NULL.
     *  \param y Returns the current Y coord, relative to the desktop. Can be NULL.
     *  \return The current button state as a bitmask, which can be tested using the SDL_BUTTON(X) macros.
     *
     *  \sa SDL_GetMouseState
     * 
     * extern DECLSPEC Uint32 SDLCALL SDL_GetGlobalMouseState(int *x, int *y);
     */
    public int SDL_GetGlobalMouseState(IntByReference x, IntByReference y);

    /**
     *  \brief Create a cursor, using the specified bitmap data and
     *         mask (in MSB format).
     *
     *  The cursor width must be a multiple of 8 bits.
     *
     *  The cursor is created in black and white according to the following:
     *  <table>
     *  <tr><td> data </td><td> mask </td><td> resulting pixel on screen </td></tr>
     *  <tr><td>  0   </td><td>  1   </td><td> White </td></tr>
     *  <tr><td>  1   </td><td>  1   </td><td> Black </td></tr>
     *  <tr><td>  0   </td><td>  0   </td><td> Transparent </td></tr>
     *  <tr><td>  1   </td><td>  0   </td><td> Inverted color if possible, black
     *                                         if not. </td></tr>
     *  </table>
     *
     *  \sa SDL_FreeCursor()
     *  
     * extern DECLSPEC SDL_Cursor *SDLCALL SDL_CreateCursor(const Uint8 * data, const Uint8 * mask, int w, int h, int hot_x, int hot_y);
     */
    public Pointer SDL_CreateCursor(
            byte data, byte mask,
            int w, int h, int hot_x, int hot_y);
    
    /**
     * TODO sdl What does "a color cursor" mean?
     * 
     *  \brief Create a color cursor.
     *
     *  \sa SDL_FreeCursor()
     * 
     * extern DECLSPEC SDL_Cursor *SDLCALL SDL_CreateColorCursor(SDL_Surface *surface, int hot_x, int hot_y);
     */
    public Pointer SDL_CreateColorCursor(Pointer surface, int hot_x, int hot_y);

    /**
     * \brief Create a system cursor.
     *
     * \sa SDL_FreeCursor()
     * 
     * extern DECLSPEC SDL_Cursor *SDLCALL SDL_CreateSystemCursor(SDL_SystemCursor id);
     */
    public Pointer SDL_CreateSystemCursor(int id);

    /**
     * \brief Set the active cursor.
     * 
     * extern DECLSPEC void SDLCALL SDL_SetCursor(SDL_Cursor * cursor);
     */
    public void SDL_SetCursor(Pointer cursor);

    /**
     * \brief Return the active cursor.
     * 
     * extern DECLSPEC SDL_Cursor *SDLCALL SDL_GetCursor(void);
     */
    public Pointer SDL_GetCursor();

    /**
     * \brief Return the default cursor.
     * 
     * extern DECLSPEC SDL_Cursor *SDLCALL SDL_GetDefaultCursor(void);
     */
    public Pointer SDL_GetDefaultCursor();

    /**
     * \brief Frees a cursor created with SDL_CreateCursor().
     *
     * \sa SDL_CreateCursor()
     * 
     * extern DECLSPEC void SDLCALL SDL_FreeCursor(SDL_Cursor * cursor);
     */
    public Pointer SDL_FreeCursor(Pointer cursor);
    
    /**
     * \brief Toggle whether or not the cursor is shown.
     *
     * \param toggle 1 to show the cursor, 0 to hide it, -1 to query the current
     *               state.
     *
     * \return 1 if the cursor is shown, or 0 if the cursor is hidden.
     * 
     * extern DECLSPEC int SDLCALL SDL_ShowCursor(int toggle);
     */
    public int SDL_ShowCursor(int toggle);
    
    //--------------------------------------------------------------------------
    // SDL_events.h
    //--------------------------------------------------------------------------

    /*
     * General keyboard/mouse state definitions
     */
    
    public static final int SDL_RELEASED = 0;
    public static final int SDL_PRESSED = 1;

    /*
     * 
     */
    
    /**
     * Pumps the event loop, gathering events from the input devices.
     *
     * This function updates the event queue and internal input device state.
     *
     * This should only be run in the thread that sets the video mode.
     * 
     * extern DECLSPEC void SDLCALL SDL_PumpEvents(void);
     */
    public void SDL_PumpEvents();

    /**
     *  Checks to see if certain event types are in the event queue.
     * 
     * extern DECLSPEC SDL_bool SDLCALL SDL_HasEvent(Uint32 type);
     */
    public boolean SDL_HasEvent(int type);
    
    /**
     * extern DECLSPEC SDL_bool SDLCALL SDL_HasEvents(Uint32 minType, Uint32 maxType);
     */
    public boolean SDL_HasEvents(int minType, int maxType);

    /**
     *  This function clears events from the event queue
     *  This function only affects currently queued events. If you want to make
     *  sure that all pending OS events are flushed, you can call SDL_PumpEvents()
     *  on the main thread immediately before the flush call.
     * 
     * extern DECLSPEC void SDLCALL SDL_FlushEvent(Uint32 type);
     */
    public void SDL_FlushEvent(int type);
    
    /**
     * extern DECLSPEC void SDLCALL SDL_FlushEvents(Uint32 minType, Uint32 maxType);
     */
    public void SDL_FlushEvents(int minType, int maxType);
    
    /**
     *  \brief Polls for currently pending events.
     *
     *  \return 1 if there are any pending events, or 0 if there are none available.
     *
     *  \param event If not NULL, the next event is removed from the queue and
     *               stored in that area.
     * 
     * extern DECLSPEC int SDLCALL SDL_PollEvent(SDL_Event * event);
     */
    public int SDL_PollEvent(SDL_Event event);
    
    /**
     *  \brief Waits indefinitely for the next available event.
     *
     *  \return 1, or 0 if there was an error while waiting for events.
     *
     *  \param event If not NULL, the next event is removed from the queue and
     *               stored in that area.
     * 
     * extern DECLSPEC int SDLCALL SDL_WaitEvent(SDL_Event * event);
     * 
     * @param event (out)
     */
    public int SDL_WaitEvent(SDL_Event event);

    /**
     *  \brief Waits until the specified timeout (in milliseconds) for the next
     *         available event.
     *
     *  \return 1, or 0 if there was an error while waiting for events.
     *
     *  \param event If not NULL, the next event is removed from the queue and
     *               stored in that area.
     *  \param timeout The timeout (in milliseconds) to wait for next event.
     * 
     * extern DECLSPEC int SDLCALL SDL_WaitEventTimeout(SDL_Event * event, int timeout);
     * 
     * @param event (out)
     */
    public int SDL_WaitEventTimeout(SDL_Event event, int timeout);

    /**
     * "It seems SDL_PushEvent and SDL_PeepEvents are safe to be called
     * from any thread - event queue is locked before any changes."
     * 
     *  \brief Add an event to the event queue.
     *
     *  \return 1 on success, 0 if the event was filtered, or -1 if the event queue
     *          was full or there was some other error.
     * 
     * extern DECLSPEC int SDLCALL SDL_PushEvent(SDL_Event * event);
     */
    public int SDL_PushEvent(SDL_Event event);
    
    /**
     *  This function allocates a set of user-defined events, and returns
     *  the beginning event number for that set of events.
     *
     *  If there aren't enough user-defined events left, this function
     *  returns (Uint32)-1
     * 
     * extern DECLSPEC Uint32 SDLCALL SDL_RegisterEvents(int numevents);
     */
    public int SDL_RegisterEvents(int numevents);

    //--------------------------------------------------------------------------
    // SDL_hints.h
    //--------------------------------------------------------------------------
    
    /**
     * @see SdlHint
     * 
     *  \brief Set a hint with normal priority
     * 
     *  \return SDL_TRUE if the hint was set, SDL_FALSE otherwise
     * 
     * extern DECLSPEC SDL_bool SDLCALL SDL_SetHint(const char *name, const char *value);
     */
    public boolean SDL_SetHint(String name, String value);
    
    /**
     * @see SdlHint
     * 
     *  \brief Get a hint
     * 
     *  \return The string value of a hint variable.
     * 
     * extern DECLSPEC const char * SDLCALL SDL_GetHint(const char *name);
     */
    public String SDL_GetHint(String name);

    //--------------------------------------------------------------------------
    // SDL.h
    //--------------------------------------------------------------------------
    
    /**
     * @see SdlInitFlag
     * 
     * This function initializes  the subsystems specified by \c flags
     *
     * extern DECLSPEC int SDLCALL SDL_Init(Uint32 flags);
     */
    public int SDL_Init(int flags);
    
    /**
     * This function cleans up all initialized subsystems. You should
     * call it upon all exit conditions.
     * 
     * extern DECLSPEC void SDLCALL SDL_Quit(void);
     */
    public void SDL_Quit();
    
    //--------------------------------------------------------------------------
    // SDL_error.h
    //--------------------------------------------------------------------------
    
    /**
     * extern DECLSPEC const char *SDLCALL SDL_GetError(void);
     */
    public String SDL_GetError();
    
    /**
     * extern DECLSPEC void SDLCALL SDL_ClearError(void);
     */
    public void SDL_ClearError();
}
