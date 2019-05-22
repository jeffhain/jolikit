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
 * enum SDL_EventType
 * 
 * \brief The types of events that can be delivered.
 */
public enum SdlEventType implements InterfaceIntValued {
    SDL_FIRSTEVENT(0),     /**< Unused (do not remove) */

    /* Application events */
    SDL_QUIT(0x100), /**< User-requested quit */

    /* These application events have special meaning on iOS, see README-ios.md for details */
    SDL_APP_TERMINATING(0x100 + 1),        /**< The application is being terminated by the OS
                                     Called on iOS in applicationWillTerminate()
                                     Called on Android in onDestroy()
     */
    SDL_APP_LOWMEMORY(0x100 + 2),          /**< The application is low on memory, free memory if possible.
                                     Called on iOS in applicationDidReceiveMemoryWarning()
                                     Called on Android in onLowMemory()
     */
    SDL_APP_WILLENTERBACKGROUND(0x100 + 3), /**< The application is about to enter the background
                                     Called on iOS in applicationWillResignActive()
                                     Called on Android in onPause()
     */
    SDL_APP_DIDENTERBACKGROUND(0x100 + 4), /**< The application did enter the background and may not get CPU for some time
                                     Called on iOS in applicationDidEnterBackground()
                                     Called on Android in onPause()
     */
    SDL_APP_WILLENTERFOREGROUND(0x100 + 5), /**< The application is about to enter the foreground
                                     Called on iOS in applicationWillEnterForeground()
                                     Called on Android in onResume()
     */
    SDL_APP_DIDENTERFOREGROUND(0x100 + 6), /**< The application is now interactive
                                     Called on iOS in applicationDidBecomeActive()
                                     Called on Android in onResume()
     */

    /* Window events */
    SDL_WINDOWEVENT(0x200), /**< Window state change */
    SDL_SYSWMEVENT(0x200 + 1),             /**< System specific event */

    /* Keyboard events */
    SDL_KEYDOWN(0x300), /**< Key pressed */
    SDL_KEYUP(0x300 + 1),                  /**< Key released */
    SDL_TEXTEDITING(0x300 + 2),            /**< Keyboard text editing (composition) */
    SDL_TEXTINPUT(0x300 + 3),              /**< Keyboard text input */
    SDL_KEYMAPCHANGED(0x300 + 4),          /**< Keymap changed due to a system event such as an
                                     input language or keyboard layout change.
     */

    /* Mouse events */
    SDL_MOUSEMOTION(0x400), /**< Mouse moved */
    SDL_MOUSEBUTTONDOWN(0x400 + 1),        /**< Mouse button pressed */
    SDL_MOUSEBUTTONUP(0x400 + 2),          /**< Mouse button released */
    SDL_MOUSEWHEEL(0x400 + 3),             /**< Mouse wheel motion */

    /* Joystick events */
    SDL_JOYAXISMOTION(0x600), /**< Joystick axis motion */
    SDL_JOYBALLMOTION(0x600 + 1),          /**< Joystick trackball motion */
    SDL_JOYHATMOTION(0x600 + 2),           /**< Joystick hat position change */
    SDL_JOYBUTTONDOWN(0x600 + 3),          /**< Joystick button pressed */
    SDL_JOYBUTTONUP(0x600 + 4),            /**< Joystick button released */
    SDL_JOYDEVICEADDED(0x600 + 5),         /**< A new joystick has been inserted into the system */
    SDL_JOYDEVICEREMOVED(0x600 + 6),       /**< An opened joystick has been removed */

    /* Game controller events */
    SDL_CONTROLLERAXISMOTION(0x650), /**< Game controller axis motion */
    SDL_CONTROLLERBUTTONDOWN(0x650 + 1),          /**< Game controller button pressed */
    SDL_CONTROLLERBUTTONUP(0x650 + 2),            /**< Game controller button released */
    SDL_CONTROLLERDEVICEADDED(0x650 + 3),         /**< A new Game controller has been inserted into the system */
    SDL_CONTROLLERDEVICEREMOVED(0x650 + 4),       /**< An opened Game controller has been removed */
    SDL_CONTROLLERDEVICEREMAPPED(0x650 + 5),      /**< The controller mapping was updated */

    /* Touch events */
    SDL_FINGERDOWN(0x700),
    SDL_FINGERUP(0x700 + 1),
    SDL_FINGERMOTION(0x700 + 2),

    /* Gesture events */
    SDL_DOLLARGESTURE(0x800),
    SDL_DOLLARRECORD(0x800 + 1),
    SDL_MULTIGESTURE(0x800 + 2),

    /* Clipboard events */
    SDL_CLIPBOARDUPDATE(0x900), /**< The clipboard changed */

    /* Drag and drop events */
    SDL_DROPFILE(0x1000), /**< The system requests a file open */

    /* Audio hotplug events */
    SDL_AUDIODEVICEADDED(0x1100), /**< A new audio device is available */
    SDL_AUDIODEVICEREMOVED(0x1100 + 1),        /**< An audio device has been removed. */

    /* Render events */
    SDL_RENDER_TARGETS_RESET(0x2000), /**< The render targets have been reset and their contents need to be updated */
    SDL_RENDER_DEVICE_RESET(0x2000 + 1), /**< The device has been reset and all textures need to be recreated */

    /** Events ::SDL_USEREVENT through ::SDL_LASTEVENT are for your use,
     *  and should be allocated with SDL_RegisterEvents()
     */
    SDL_USEREVENT(0x8000),

    /**
     *  This last event is only for bounding internal arrays
     */
    SDL_LASTEVENT(0xFFFF);
    
    private static final IntValuedHelper<SdlEventType> HELPER =
            new IntValuedHelper<SdlEventType>(SdlEventType.values());
    
    private final int intValue;
    
    private SdlEventType(int intValue) {
        this.intValue = intValue;
    }
    
    @Override
    public int intValue() {
        return this.intValue;
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlEventType valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
