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
 * struct ALLEGRO_FONT
 * {
 *    void *data;
 *    int height;
 *    ALLEGRO_FONT *fallback;
 *    ALLEGRO_FONT_VTABLE *vtable;
 * };
 */
public class ALLEGRO_FONT extends Structure {
    public static class ByReference extends ALLEGRO_FONT implements Structure.ByReference {
        public ByReference() {
        }
        public ByReference(Pointer pointer) {
            super(pointer);
        }
    }
    public static class ByValue extends ALLEGRO_FONT implements Structure.ByValue {
        public ByValue() {
        }
        public ByValue(Pointer pointer) {
            super(pointer);
        }
    }
    public ALLEGRO_FONT() {
    }
    public ALLEGRO_FONT(Pointer pointer) {
        super(pointer);
    }
    public Pointer data;
    public int height;
    /**
     * ByReference else infinite constructor loop.
     */
    public ALLEGRO_FONT.ByReference fallback;
    public Pointer vtable;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("data", "height", "fallback", "vtable");
    }
}
