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

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * typedef struct ALLEGRO_KEYBOARD_STATE ALLEGRO_KEYBOARD_STATE;
 * struct ALLEGRO_KEYBOARD_STATE
 * {
 *    struct ALLEGRO_DISPLAY *display;  / * public * /
 *    / * internal * /
 *    unsigned int __key_down__internal__[(ALLEGRO_KEY_MAX + 31) / 32]; 
 * };
 */
public class ALLEGRO_KEYBOARD_STATE extends Structure {
    public static class ByReference extends ALLEGRO_KEYBOARD_STATE implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_KEYBOARD_STATE implements Structure.ByValue {
    }
    public Pointer display;
    public int[] __key_down__internal__ = new int[(AlgrKey.ALLEGRO_KEY_MAX + 31) / 32];
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("display", "__key_down__internal__");
    }
}
