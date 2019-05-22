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

import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * typedef struct ALLEGRO_KEYBOARD_EVENT
 * {
 *    _AL_EVENT_HEADER(struct ALLEGRO_KEYBOARD)
 *    struct ALLEGRO_DISPLAY *display; / * the window the key was pressed in * /
 *    int keycode;                 / * the physical key pressed * /
 *    int unichar;                 / * unicode character or negative * /
 *    unsigned int modifiers;      / * bitfield * /
 *    bool repeat;                 / * auto-repeated or not * /
 * } ALLEGRO_KEYBOARD_EVENT;
 */
public class ALLEGRO_KEYBOARD_EVENT extends _AL_EVENT_HEADER {
    public static class ByReference extends ALLEGRO_KEYBOARD_EVENT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_KEYBOARD_EVENT implements Structure.ByValue {
    }
    public Pointer display;
    /**
     * "The code corresponding to the physical key which was pressed."
     * TODO algr Then why does it looks like the virtual key?
     */
    public int keycode;
    public int unichar;
    public int modifiers;
    public boolean repeat;
    @Override
    protected List<String> getFieldOrder() {
        return concat(super.getFieldOrder(), "display", "keycode", "unichar", "modifiers", "repeat");
    }
}
