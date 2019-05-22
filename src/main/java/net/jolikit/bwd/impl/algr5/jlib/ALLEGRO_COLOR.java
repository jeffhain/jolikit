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
 * typedef struct ALLEGRO_COLOR ALLEGRO_COLOR;
 * struct ALLEGRO_COLOR
 * {
 *    float r, g, b, a;
 * };
 */
public class ALLEGRO_COLOR extends Structure {
    public static class ByReference extends ALLEGRO_COLOR implements Structure.ByReference {
        public ByReference() {
        }
        public ByReference(Pointer pointer) {
            super(pointer);
        }
    }
    public static class ByValue extends ALLEGRO_COLOR implements Structure.ByValue {
        public ByValue() {
        }
        public ByValue(Pointer pointer) {
            super(pointer);
        }
    }
    public ALLEGRO_COLOR() {
    }
    public ALLEGRO_COLOR(Pointer pointer) {
        super(pointer);
    }
    public float r;
    public float g;
    public float b;
    public float a;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("r", "g", "b", "a");
    }
}
