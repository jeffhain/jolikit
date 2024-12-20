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

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

/**
 * Hints that we might want to use.
 * Hint name is the toString() of the enum.
 * 
 * Cf. SDL_hints.h.
 * Enum values are the string names of the hints,
 * not their constant names in SDL_hints.h.
 */
public enum SdlHint implements InterfaceIntValued {
    /**
     *  \brief  A variable controlling how 3D acceleration is used to accelerate the SDL screen surface.
     *
     *  SDL can try to accelerate the SDL screen surface by using streaming
     *  textures with a 3D rendering engine.  This variable controls whether and
     *  how this is done.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable 3D acceleration
     *    "1"       - Enable 3D acceleration, using the default renderer.
     *    "X"       - Enable 3D acceleration, using X where X is one of the valid rendering drivers.  (e.g. "direct3d", "opengl", etc.)
     *
     *  By default SDL tries to make a best guess for each platform whether
     *  to use acceleration or not.
     */
    SDL_FRAMEBUFFER_ACCELERATION,
    /**
     *  \brief  A variable specifying which render driver to use.
     *
     *  If the application doesn't pick a specific renderer to use, this variable
     *  specifies the name of the preferred renderer.  If the preferred renderer
     *  can't be initialized, the normal default renderer is used.
     *
     *  This variable is case insensitive and can be set to the following values:
     *    "direct3d"
     *    "opengl"
     *    "opengles2"
     *    "opengles"
     *    "software"
     *
     *  The default varies by platform, but it's the first one in the list that
     *  is available on the current platform.
     */
    SDL_RENDER_DRIVER,
    /**
     *  \brief  A variable controlling whether the OpenGL render driver uses shaders if they are available.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable shaders
     *    "1"       - Enable shaders
     *
     *  By default shaders are used if OpenGL supports them.
     */
    SDL_RENDER_OPENGL_SHADERS,
    /**
     *  \brief  A variable controlling whether the Direct3D device is initialized for thread-safe operations.
     *
     *  This variable can be set to the following values:
     *    "0"       - Thread-safety is not enabled (faster)
     *    "1"       - Thread-safety is enabled
     *
     *  By default the Direct3D device is created with thread-safety disabled.
     */
    SDL_RENDER_DIRECT3D_THREADSAFE,
    /**
     *  \brief  A variable controlling whether to enable Direct3D 11+'s Debug Layer.
     *
     *  This variable does not have any effect on the Direct3D 9 based renderer.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable Debug Layer use
     *    "1"       - Enable Debug Layer use
     *
     *  By default, SDL does not use Direct3D Debug Layer.
     */
    SDL_RENDER_DIRECT3D11_DEBUG,
    /**
     *  \brief  A variable controlling the scaling quality
     *
     *  This variable can be set to the following values:
     *    "0" or "nearest" - Nearest pixel sampling
     *    "1" or "linear"  - Linear filtering (supported by OpenGL and Direct3D)
     *    "2" or "best"    - Currently this is the same as "linear"
     *
     *  By default nearest pixel sampling is used
     */
    SDL_RENDER_SCALE_QUALITY,
    /**
     *  \brief  A variable controlling whether updates to the SDL screen surface should be synchronized with the vertical refresh, to avoid tearing.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable vsync
     *    "1"       - Enable vsync
     *
     *  By default SDL does not sync screen surface updates with vertical refresh.
     */
    SDL_RENDER_VSYNC,
    /**
     *  \brief  A variable controlling whether the screensaver is enabled. 
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable screensaver
     *    "1"       - Enable screensaver
     *
     *  By default SDL will disable the screensaver.
     */
    SDL_VIDEO_ALLOW_SCREENSAVER,
    /**
     *  \brief  A variable controlling whether the X11 VidMode extension should be used.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable XVidMode
     *    "1"       - Enable XVidMode
     *
     *  By default SDL will use XVidMode if it is available.
     */
    SDL_VIDEO_X11_XVIDMODE,
    /**
     *  \brief  A variable controlling whether the X11 Xinerama extension should be used.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable Xinerama
     *    "1"       - Enable Xinerama
     *
     *  By default SDL will use Xinerama if it is available.
     */
    SDL_VIDEO_X11_XINERAMA,
    /**
     *  \brief  A variable controlling whether the X11 XRandR extension should be used.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable XRandR
     *    "1"       - Enable XRandR
     *
     *  By default SDL will not use XRandR because of window manager issues.
     */
    SDL_VIDEO_X11_XRANDR,
    /**
     *  \brief  A variable controlling whether the X11 _NET_WM_PING protocol should be supported.
     *
     *  This variable can be set to the following values:
     *    "0"       - Disable _NET_WM_PING
     *    "1"       - Enable _NET_WM_PING
     *
     *  By default SDL will use _NET_WM_PING, but for applications that know they
     *  will not always be able to respond to ping requests in a timely manner they can
     *  turn it off to avoid the window manager thinking the app is hung.
     *  The hint is checked in CreateWindow.
     */
    SDL_VIDEO_X11_NET_WM_PING,
    /**
     *  \brief  A variable controlling whether the window frame and title bar are interactive when the cursor is hidden 
     *
     *  This variable can be set to the following values:
     *    "0"       - The window frame is not interactive when the cursor is hidden (no move, resize, etc)
     *    "1"       - The window frame is interactive when the cursor is hidden
     *
     *  By default SDL will allow interaction with the window frame when the cursor is hidden
     */
    SDL_WINDOW_FRAME_USABLE_WHILE_CURSOR_HIDDEN,
    /**
     *  \brief  A variable controlling whether the windows message loop is processed by SDL 
     *
     *  This variable can be set to the following values:
     *    "0"       - The window message loop is not run
     *    "1"       - The window message loop is processed in SDL_PumpEvents()
     *
     *  By default SDL will process the windows message loop
     */
    SDL_WINDOWS_ENABLE_MESSAGELOOP,
    /**
     *  \brief  A variable controlling whether grabbing input grabs the keyboard
     *
     *  This variable can be set to the following values:
     *    "0"       - Grab will affect only the mouse
     *    "1"       - Grab will affect mouse and keyboard
     *
     *  By default SDL will not grab the keyboard so system shortcuts still work.
     */
    SDL_GRAB_KEYBOARD,
    /**
     *  \brief  A variable controlling whether relative mouse mode is implemented using mouse warping
     *
     *  This variable can be set to the following values:
     *    "0"       - Relative mouse mode uses raw input
     *    "1"       - Relative mouse mode uses mouse warping
     *
     *  By default SDL will use raw input for relative mouse mode
     */
    SDL_MOUSE_RELATIVE_MODE_WARP,
    /**
     *  \brief Allow mouse click events when clicking to focus an SDL window
     *
     *  This variable can be set to the following values:
     *    "0"       - Ignore mouse clicks that activate a window
     *    "1"       - Generate events for mouse clicks that activate a window
     *
     *  By default SDL will ignore mouse clicks that activate a window
     */
    SDL_MOUSE_FOCUS_CLICKTHROUGH,
    /**
     *  \brief Minimize your SDL_Window if it loses key focus when in fullscreen mode. Defaults to true.
     */
    SDL_VIDEO_MINIMIZE_ON_FOCUS_LOSS,
    /**
     *  \brief  A variable controlling whether the idle timer is disabled on iOS.
     *
     *  When an iOS app does not receive touches for some time, the screen is
     *  dimmed automatically. For games where the accelerometer is the only input
     *  this is problematic. This functionality can be disabled by setting this
     *  hint.
     *
     *  As of SDL 2.0.4, SDL_EnableScreenSaver() and SDL_DisableScreenSaver()
     *  accomplish the same thing on iOS. They should be preferred over this hint.
     *
     *  This variable can be set to the following values:
     *    "0"       - Enable idle timer
     *    "1"       - Disable idle timer
     */
    SDL_IOS_IDLE_TIMER_DISABLED,
    /**
     *  \brief  A variable controlling which orientations are allowed on iOS.
     *
     *  In some circumstances it is necessary to be able to explicitly control
     *  which UI orientations are allowed.
     *
     *  This variable is a space delimited list of the following values:
     *    "LandscapeLeft", "LandscapeRight", "Portrait" "PortraitUpsideDown"
     */
    SDL_IOS_ORIENTATIONS,
    /**
     *  \brief  A variable controlling whether controllers used with the Apple TV
     *  generate UI events.
     *
     *  When UI events are generated by controller input, the app will be
     *  backgrounded when the Apple TV remote's menu button is pressed, and when the
     *  pause or B buttons on gamepads are pressed.
     *
     *  More information about properly making use of controllers for the Apple TV
     *  can be found here:
     *  https://developer.apple.com/tvos/human-interface-guidelines/remote-and-controllers/
     *
     *  This variable can be set to the following values:
     *    "0"       - Controller input does not generate UI events (the default).
     *    "1"       - Controller input generates UI events.
     */
    SDL_APPLE_TV_CONTROLLER_UI_EVENTS,
    /**
     *  \brief  A variable controlling whether the Apple TV remote's joystick axes
     *         will automatically match the rotation of the remote.
     *
     *  This variable can be set to the following values:
     *    "0"       - Remote orientation does not affect joystick axes (the default).
     *    "1"       - Joystick axes are based on the orientation of the remote.
     */
    SDL_APPLE_TV_REMOTE_ALLOW_ROTATION,
    /**
     *  \brief  A variable controlling whether the Android / iOS built-in
     *  accelerometer should be listed as a joystick device, rather than listing
     *  actual joysticks only.
     *
     *  This variable can be set to the following values:
     *    "0"       - List only real joysticks and accept input from them
     *    "1"       - List real joysticks along with the accelerometer as if it were a 3 axis joystick (the default).
     */
    SDL_ACCELEROMETER_AS_JOYSTICK,
    /**
     *  \brief  A variable that lets you disable the detection and use of Xinput gamepad devices
     *
     *  The variable can be set to the following values:
     *    "0"       - Disable XInput detection (only uses direct input)
     *    "1"       - Enable XInput detection (the default)
     */
    SDL_XINPUT_ENABLED,
    /**
     *  \brief  A variable that causes SDL to use the old axis and button mapping for XInput devices.
     *
     *  This hint is for backwards compatibility only and will be removed in SDL 2.1
     *
     *  The default value is "0".  This hint must be set before SDL_Init()
     */
    SDL_XINPUT_USE_OLD_JOYSTICK_MAPPING,
    /**
     *  \brief  A variable that lets you manually hint extra gamecontroller db entries
     *
     *  The variable should be newline delimited rows of gamecontroller config data, see SDL_gamecontroller.h
     *
     *  This hint must be set before calling SDL_Init(SDL_INIT_GAMECONTROLLER)
     *  You can update mappings after the system is initialized with SDL_GameControllerMappingForGUID() and SDL_GameControllerAddMapping()
     */
    SDL_GAMECONTROLLERCONFIG,
    /**
     *  \brief  A variable that lets you enable joystick (and gamecontroller) events even when your app is in the background.
     *
     *  The variable can be set to the following values:
     *    "0"       - Disable joystick & gamecontroller input events when the
     *                application is in the background.
     *    "1"       - Enable joystick & gamecontroller input events when the
     *                application is in the background.
     *
     *  The default value is "0".  This hint may be set at any time.
     */
    SDL_JOYSTICK_ALLOW_BACKGROUND_EVENTS,
    /**
     *  \brief If set to "0" then never set the top most bit on a SDL Window, even if the video mode expects it.
     *      This is a debugging aid for developers and not expected to be used by end users. The default is "1"
     *
     *  This variable can be set to the following values:
     *    "0"       - don't allow topmost
     *    "1"       - allow topmost
     */
    SDL_ALLOW_TOPMOST,
    /**
     *  \brief A variable that controls the timer resolution, in milliseconds.
     *
     *  The higher resolution the timer, the more frequently the CPU services
     *  timer interrupts, and the more precise delays are, but this takes up
     *  power and CPU time.  This hint is only used on Windows 7 and earlier.
     *
     *  See this blog post for more information:
     *  http://randomascii.wordpress.com/2013/07/08/windows-timer-resolution-megawatts-wasted/
     *
     *  If this variable is set to "0", the system timer resolution is not set.
     *
     *  The default value is "1". This hint may be set at any time.
     */
    SDL_TIMER_RESOLUTION,
    /**
     *  \brief  A string specifying SDL's threads stack size in bytes or "0" for the backend's default size
     *
     *  Use this hint in case you need to set SDL's threads stack size to other than the default.
     *  This is specially useful if you build SDL against a non glibc libc library (such as musl) which
     *  provides a relatively small default thread stack size (a few kilobytes versus the default 8MB glibc uses).
     *  Support for this hint is currently available only in the pthread, Windows, and PSP backend.
     */
    SDL_THREAD_STACK_SIZE,
    /**
     *  \brief If set to 1, then do not allow high-DPI windows. ("Retina" on Mac and iOS)
     */
    SDL_VIDEO_HIGHDPI_DISABLED,
    /**
     *  \brief A variable that determines whether ctrl+click should generate a right-click event on Mac
     *
     *  If present, holding ctrl while left clicking will generate a right click
     *  event when on Mac.
     */
    SDL_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK,
    /**
     *  \brief  A variable specifying which shader compiler to preload when using the Chrome ANGLE binaries
     *
     *  SDL has EGL and OpenGL ES2 support on Windows via the ANGLE project. It
     *  can use two different sets of binaries, those compiled by the user from source
     *  or those provided by the Chrome browser. In the later case, these binaries require
     *  that SDL loads a DLL providing the shader compiler.
     *
     *  This variable can be set to the following values:
     *    "d3dcompiler_46.dll" - default, best for Vista or later.
     *    "d3dcompiler_43.dll" - for XP support.
     *    "none" - do not load any library, useful if you compiled ANGLE from source and included the compiler in your binaries.
     */
    SDL_VIDEO_WIN_D3DCOMPILER,
    /**
     *  \brief  A variable that is the address of another SDL_Window* (as a hex string formatted with "%p").
     *  
     *  If this hint is set before SDL_CreateWindowFrom() and the SDL_Window* it is set to has
     *  SDL_WINDOW_OPENGL set (and running on WGL only, currently), then two things will occur on the newly 
     *  created SDL_Window:
     *
     *  1. Its pixel format will be set to the same pixel format as this SDL_Window.  This is
     *  needed for example when sharing an OpenGL context across multiple windows.
     *
     *  2. The flag SDL_WINDOW_OPENGL will be set on the new window so it can be used for
     *  OpenGL rendering.
     *
     *  This variable can be set to the following values:
     *    The address (as a string "%p") of the SDL_Window* that new windows created with SDL_CreateWindowFrom() should
     *    share a pixel format with.
     */
    SDL_VIDEO_WINDOW_SHARE_PIXEL_FORMAT,
    /**
     *  \brief A URL to a WinRT app's privacy policy
     *
     *  All network-enabled WinRT apps must make a privacy policy available to its
     *  users.  On Windows 8, 8.1, and RT, Microsoft mandates that this policy be
     *  be available in the Windows Settings charm, as accessed from within the app.
     *  SDL provides code to add a URL-based link there, which can point to the app's
     *  privacy policy.
     *
     *  To setup a URL to an app's privacy policy, set SDL_HINT_WINRT_PRIVACY_POLICY_URL
     *  before calling any SDL_Init() functions.  The contents of the hint should
     *  be a valid URL.  For example, "http://www.example.com".
     *
     *  The default value is "", which will prevent SDL from adding a privacy policy
     *  link to the Settings charm.  This hint should only be set during app init.
     *
     *  The label text of an app's "Privacy Policy" link may be customized via another
     *  hint, SDL_HINT_WINRT_PRIVACY_POLICY_LABEL.
     *
     *  Please note that on Windows Phone, Microsoft does not provide standard UI
     *  for displaying a privacy policy link, and as such, SDL_HINT_WINRT_PRIVACY_POLICY_URL
     *  will not get used on that platform.  Network-enabled phone apps should display
     *  their privacy policy through some other, in-app means.
     */
    SDL_WINRT_PRIVACY_POLICY_URL,
    /** 
     *  \brief Label text for a WinRT app's privacy policy link
     *
     *  Network-enabled WinRT apps must include a privacy policy.  On Windows 8, 8.1, and RT,
     *  Microsoft mandates that this policy be available via the Windows Settings charm.
     *  SDL provides code to add a link there, with its label text being set via the
     *  optional hint, SDL_HINT_WINRT_PRIVACY_POLICY_LABEL.
     *
     *  Please note that a privacy policy's contents are not set via this hint.  A separate
     *  hint, SDL_HINT_WINRT_PRIVACY_POLICY_URL, is used to link to the actual text of the
     *  policy.
     *
     *  The contents of this hint should be encoded as a UTF8 string.
     *
     *  The default value is "Privacy Policy".  This hint should only be set during app
     *  initialization, preferably before any calls to SDL_Init().
     *
     *  For additional information on linking to a privacy policy, see the documentation for
     *  SDL_HINT_WINRT_PRIVACY_POLICY_URL.
     */
    SDL_WINRT_PRIVACY_POLICY_LABEL,
    /** 
     *  \brief Allows back-button-press events on Windows Phone to be marked as handled
     *
     *  Windows Phone devices typically feature a Back button.  When pressed,
     *  the OS will emit back-button-press events, which apps are expected to
     *  handle in an appropriate manner.  If apps do not explicitly mark these
     *  events as 'Handled', then the OS will invoke its default behavior for
     *  unhandled back-button-press events, which on Windows Phone 8 and 8.1 is to
     *  terminate the app (and attempt to switch to the previous app, or to the
     *  device's home screen).
     *
     *  Setting the SDL_HINT_WINRT_HANDLE_BACK_BUTTON hint to "1" will cause SDL
     *  to mark back-button-press events as Handled, if and when one is sent to
     *  the app.
     *
     *  Internally, Windows Phone sends back button events as parameters to
     *  special back-button-press callback functions.  Apps that need to respond
     *  to back-button-press events are expected to register one or more
     *  callback functions for such, shortly after being launched (during the
     *  app's initialization phase).  After the back button is pressed, the OS
     *  will invoke these callbacks.  If the app's callback(s) do not explicitly
     *  mark the event as handled by the time they return, or if the app never
     *  registers one of these callback, the OS will consider the event
     *  un-handled, and it will apply its default back button behavior (terminate
     *  the app).
     *
     *  SDL registers its own back-button-press callback with the Windows Phone
     *  OS.  This callback will emit a pair of SDL key-press events (SDL_KEYDOWN
     *  and SDL_KEYUP), each with a scancode of SDL_SCANCODE_AC_BACK, after which
     *  it will check the contents of the hint, SDL_HINT_WINRT_HANDLE_BACK_BUTTON.
     *  If the hint's value is set to "1", the back button event's Handled
     *  property will get set to 'true'.  If the hint's value is set to something
     *  else, or if it is unset, SDL will leave the event's Handled property
     *  alone.  (By default, the OS sets this property to 'false', to note.)
     *
     *  SDL apps can either set SDL_HINT_WINRT_HANDLE_BACK_BUTTON well before a
     *  back button is pressed, or can set it in direct-response to a back button
     *  being pressed.
     *
     *  In order to get notified when a back button is pressed, SDL apps should
     *  register a callback function with SDL_AddEventWatch(), and have it listen
     *  for SDL_KEYDOWN events that have a scancode of SDL_SCANCODE_AC_BACK.
     *  (Alternatively, SDL_KEYUP events can be listened-for.  Listening for
     *  either event type is suitable.)  Any value of SDL_HINT_WINRT_HANDLE_BACK_BUTTON
     *  set by such a callback, will be applied to the OS' current
     *  back-button-press event.
     *
     *  More details on back button behavior in Windows Phone apps can be found
     *  at the following page, on Microsoft's developer site:
     *  http://msdn.microsoft.com/en-us/library/windowsphone/develop/jj247550(v=vs.105).aspx
     */
    SDL_WINRT_HANDLE_BACK_BUTTON,
    /**
     *  \brief  A variable that dictates policy for fullscreen Spaces on Mac OS X.
     *
     *  This hint only applies to Mac OS X.
     *
     *  The variable can be set to the following values:
     *    "0"       - Disable Spaces support (FULLSCREEN_DESKTOP won't use them and
     *                SDL_WINDOW_RESIZABLE windows won't offer the "fullscreen"
     *                button on their titlebars).
     *    "1"       - Enable Spaces support (FULLSCREEN_DESKTOP will use them and
     *                SDL_WINDOW_RESIZABLE windows will offer the "fullscreen"
     *                button on their titlebars).
     *
     *  The default value is "1". Spaces are disabled regardless of this hint if
     *  the OS isn't at least Mac OS X Lion (10.7). This hint must be set before
     *  any windows are created.
     */
    SDL_VIDEO_MAC_FULLSCREEN_SPACES,
    /**
     *  \brief  When set don't force the SDL app to become a foreground process
     *
     *  This hint only applies to Mac OS X.
     */
    SDL_MAC_BACKGROUND_APP,
    /**
     *  \brief Android APK expansion main file version. Should be a string number like "1", "2" etc.
     *
     *  Must be set together with SDL_HINT_ANDROID_APK_EXPANSION_PATCH_FILE_VERSION.
     *
     *  If both hints were set then SDL_RWFromFile() will look into expansion files
     *  after a given relative path was not found in the internal storage and assets.
     *
     *  By default this hint is not set and the APK expansion files are not searched.
     */
    SDL_ANDROID_APK_EXPANSION_MAIN_FILE_VERSION,
    /**
     *  \brief Android APK expansion patch file version. Should be a string number like "1", "2" etc.
     *
     *  Must be set together with SDL_HINT_ANDROID_APK_EXPANSION_MAIN_FILE_VERSION.
     *
     *  If both hints were set then SDL_RWFromFile() will look into expansion files
     *  after a given relative path was not found in the internal storage and assets.
     *
     *  By default this hint is not set and the APK expansion files are not searched.
     */
    SDL_ANDROID_APK_EXPANSION_PATCH_FILE_VERSION,
    /**
     *  \brief A variable to control whether certain IMEs should handle text editing internally instead of sending SDL_TEXTEDITING events.
     *
     *  The variable can be set to the following values:
     *    "0"       - SDL_TEXTEDITING events are sent, and it is the application's
     *                responsibility to render the text from these events and 
     *                differentiate it somehow from committed text. (default)
     *    "1"       - If supported by the IME then SDL_TEXTEDITING events are not sent, 
     *                and text that is being composed will be rendered in its own UI.
     */
    SDL_IME_INTERNAL_EDITING,
    /**
     *  \brief A variable to control whether mouse and touch events are to be treated together or separately
     *
     *  The variable can be set to the following values:
     *    "0"       - Mouse events will be handled as touch events, and touch will raise fake mouse
     *                events. This is the behaviour of SDL <= 2.0.3. (default)
     *    "1"       - Mouse events will be handled separately from pure touch events.
     *
     *  The value of this hint is used at runtime, so it can be changed at any time.
     */
    SDL_ANDROID_SEPARATE_MOUSE_AND_TOUCH,
    /**
     *  \brief override the binding element for keyboard inputs for Emscripten builds
     *
     *  This hint only applies to the emscripten platform
     *
     *  The variable can be one of
     *    "#window"      - The javascript window object (this is the default)
     *    "#document"    - The javascript document object
     *    "#screen"      - the javascript window.screen object
     *    "#canvas"      - the WebGL canvas element
     *    any other string without a leading # sign applies to the element on the page with that ID.
     */
    SDL_EMSCRIPTEN_KEYBOARD_ELEMENT,
    /**
     *  \brief Tell SDL not to catch the SIGINT or SIGTERM signals.
     *
     *  This hint only applies to Unix-like platforms.
     *
     *  The variable can be set to the following values:
     *    "0"       - SDL will install a SIGINT and SIGTERM handler, and when it
     *                catches a signal, convert it into an SDL_QUIT event.
     *    "1"       - SDL will not install a signal handler at all.
     */
    SDL_NO_SIGNAL_HANDLERS,
    /**
     *  \brief Tell SDL not to generate window-close events for Alt+F4 on Windows.
     *
     *  The variable can be set to the following values:
     *    "0"       - SDL will generate a window-close event when it sees Alt+F4.
     *    "1"       - SDL will only do normal key handling for Alt+F4.
     */
    SDL_WINDOWS_NO_CLOSE_ON_ALT_F4,
    /**
     *  \brief Prevent SDL from using version 4 of the bitmap header when saving BMPs.
     *
     *  The bitmap header version 4 is required for proper alpha channel support and
     *  SDL will use it when required. Should this not be desired, this hint can
     *  force the use of the 40 byte header version which is supported everywhere.
     *
     *  The variable can be set to the following values:
     *    "0"       - Surfaces with a colorkey or an alpha channel are saved to a
     *                32-bit BMP file with an alpha mask. SDL will use the bitmap
     *                header version 4 and set the alpha mask accordingly.
     *    "1"       - Surfaces with a colorkey or an alpha channel are saved to a
     *                32-bit BMP file without an alpha mask. The alpha channel data
     *                will be in the file, but applications are going to ignore it.
     *
     *  The default value is "0".
     */
    SDL_BMP_SAVE_LEGACY_FORMAT,
    /**
     *  \brief Tell SDL not to name threads on Windows.
     *
     *  The variable can be set to the following values:
     *    "0"       - SDL will raise the 0x406D1388 Exception to name threads.
     *                This is the default behavior of SDL <= 2.0.4. (default)
     *    "1"       - SDL will not raise this exception, and threads will be unnamed.
     *                For .NET languages this is required when running under a debugger.
     */
    SDL_WINDOWS_DISABLE_THREAD_NAMING,
    /**
     *  \brief Tell SDL which Dispmanx layer to use on a Raspberry PI
     *
     *  Also known as Z-order. The variable can take a negative or positive value.
     *  The default is 10000.
     */
    SDL_RPI_VIDEO_LAYER;

    private static final IntValuedHelper<SdlHint> HELPER =
            new IntValuedHelper<SdlHint>(SdlHint.values());

    @Override
    public int intValue() {
        return this.ordinal();
    }

    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlHint valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
