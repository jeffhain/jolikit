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
 * typedef struct ALLEGRO_DISPLAY_MODE
 * {
 *    int width;
 *    int height;
 *    int format;
 *    int refresh_rate;
 * } ALLEGRO_DISPLAY_MODE;
 */
public class ALLEGRO_DISPLAY_MODE extends Structure {
    public static class ByReference extends ALLEGRO_DISPLAY_MODE implements Structure.ByReference {
        public ByReference() {
        }
        public ByReference(Pointer pointer) {
            super(pointer);
        }
    }
    public static class ByValue extends ALLEGRO_DISPLAY_MODE implements Structure.ByValue {
        public ByValue() {
        }
        public ByValue(Pointer pointer) {
            super(pointer);
        }
    }
    public ALLEGRO_DISPLAY_MODE() {
    }
    public ALLEGRO_DISPLAY_MODE(Pointer pointer) {
        super(pointer);
    }
    public int width;
    public int height;
    public int format;
    public int refresh_rate;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("width", "height", "format", "refresh_rate");
    }
}
