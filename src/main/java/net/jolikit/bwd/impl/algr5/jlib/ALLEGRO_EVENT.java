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

import com.sun.jna.Union;

/**
 * / * Type: ALLEGRO_EVENT
 *  * /
 * typedef union ALLEGRO_EVENT ALLEGRO_EVENT;
 * union ALLEGRO_EVENT
 * {
 *    / * This must be the same as the first field of _AL_EVENT_HEADER.  * /
 *    ALLEGRO_EVENT_TYPE type;
 *    / * `any' is to allow the user to access the other fields which are
 *     * common to all event types, without using some specific type
 *     * structure.
 *     * /
 *    ALLEGRO_ANY_EVENT      any;
 *    ALLEGRO_DISPLAY_EVENT  display;
 *    ALLEGRO_JOYSTICK_EVENT joystick;
 *    ALLEGRO_KEYBOARD_EVENT keyboard;
 *    ALLEGRO_MOUSE_EVENT    mouse;
 *    ALLEGRO_TIMER_EVENT    timer;
 *    ALLEGRO_TOUCH_EVENT    touch;
 *    ALLEGRO_USER_EVENT     user;
 * };
 * 
 * "When writing to native memory, the field corresponding to the type
 * passed to setType(java.lang.Class) will be written to native memory."
 */
public class ALLEGRO_EVENT extends Union {
    public static class ByReference extends ALLEGRO_EVENT implements Union.ByReference {
        public ByReference() {
        }
    }
    public static class ByValue extends ALLEGRO_EVENT implements Union.ByValue {
        public ByValue() {
        }
    }
    public ALLEGRO_ANY_EVENT.ByValue any;
    public ALLEGRO_DISPLAY_EVENT.ByValue display;
    public ALLEGRO_JOYSTICK_EVENT.ByValue joystick;
    public ALLEGRO_KEYBOARD_EVENT.ByValue keyboard;
    public ALLEGRO_MOUSE_EVENT.ByValue mouse;
    public ALLEGRO_TIMER_EVENT.ByValue timer;
    public ALLEGRO_TOUCH_EVENT.ByValue touch;
    public ALLEGRO_USER_EVENT.ByValue user;
    public ALLEGRO_EVENT() {
    }
}
