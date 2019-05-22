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

import com.sun.jna.Union;

/**
 *  \brief General event structure
 * typedef union SDL_Event
 * {
 *     Uint32 type;                    // Event type, shared with all events
 *     SDL_CommonEvent common;         // Common event data
 *     SDL_WindowEvent window;         // Window event data
 *     SDL_KeyboardEvent key;          // Keyboard event data
 *     SDL_TextEditingEvent edit;      // Text editing event data
 *     SDL_TextInputEvent text;        // Text input event data
 *     SDL_MouseMotionEvent motion;    // Mouse motion event data
 *     SDL_MouseButtonEvent button;    // Mouse button event data
 *     SDL_MouseWheelEvent wheel;      // Mouse wheel event data
 *     (disabled) SDL_JoyAxisEvent jaxis;         // Joystick axis event data
 *     (disabled) SDL_JoyBallEvent jball;         // Joystick ball event data
 *     (disabled) SDL_JoyHatEvent jhat;           // Joystick hat event data
 *     (disabled) SDL_JoyButtonEvent jbutton;     // Joystick button event data
 *     (disabled) SDL_JoyDeviceEvent jdevice;     // Joystick device change event data
 *     (disabled) SDL_ControllerAxisEvent caxis;      // Game Controller axis event data
 *     (disabled) SDL_ControllerButtonEvent cbutton;  // Game Controller button event data
 *     (disabled) SDL_ControllerDeviceEvent cdevice;  // Game Controller device event data
 *     (disabled) SDL_AudioDeviceEvent adevice;   // Audio device event data
 *     SDL_QuitEvent quit;             // Quit request event data
 *     SDL_UserEvent user;             // Custom event data
 *     SDL_SysWMEvent syswm;           // System dependent window event data
 *     SDL_TouchFingerEvent tfinger;   // Touch finger event data
 *     SDL_MultiGestureEvent mgesture; // Gesture event data
 *     SDL_DollarGestureEvent dgesture; // Gesture event data
 *     SDL_DropEvent drop;             // Drag and drop event data
 *     / * This is necessary for ABI compatibility between Visual C++ and GCC
 *         Visual C++ will respect the push pack pragma and use 52 bytes for
 *         this structure, and GCC will use the alignment of the largest datatype
 *         within the union, which is 8 bytes.
 *         So... we'll add padding to force the size to be 56 bytes for both. * /
 *     Uint8 padding[56];
 * } SDL_Event;
 */
public class SDL_Event extends Union {
    public static class ByReference extends SDL_Event implements Union.ByReference {
    }
    public static class ByValue extends SDL_Event implements Union.ByValue {
    }
    public int type; // Must be same as "type" field of events.
    public SDL_CommonEvent.ByValue common;
    public SDL_WindowEvent.ByValue window;
    public SDL_KeyboardEvent.ByValue key;
    public SDL_TextEditingEvent.ByValue edit;
    public SDL_TextInputEvent.ByValue text;
    public SDL_MouseMotionEvent.ByValue motion;
    public SDL_MouseButtonEvent.ByValue button;
    public SDL_MouseWheelEvent.ByValue wheel;
    public SDL_QuitEvent.ByValue quit;
    public SDL_UserEvent.ByValue user;
    public SDL_SysWMEvent.ByValue syswm;
    public SDL_TouchFingerEvent.ByValue tfinger;
    public SDL_MultiGestureEvent.ByValue mgesture;
    public SDL_DollarGestureEvent.ByValue dgesture;
    public SDL_DropEvent.ByValue drop;
    public byte[] padding = new byte[56];
    public SDL_Event() {
    }
    /**
     * @return The type.
     */
    public int type() {
        return this.type;
    }
    public String toStringType() {
        final SdlEventType value = SdlEventType.valueOf(this.type());
        return String.valueOf(value);
    }
}
