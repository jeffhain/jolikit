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
 * struct ALLEGRO_LOCKED_REGION {
 *    void *data; // The leftmost pixel of the first row (row 0).
 *    int format; // The pixel format of the data.
 *    int pitch; // The size in bytes of a single row. Can be negative (the bitmap may be upside down).
 *    int pixel_size; // The number of bytes used to represent a single pixel.
 * };
 */
public class ALLEGRO_LOCKED_REGION extends Structure {
    public static class ByReference extends ALLEGRO_LOCKED_REGION implements Structure.ByReference {
        public ByReference() {
        }
        public ByReference(Pointer pointer) {
            super(pointer);
        }
    }
    public static class ByValue extends ALLEGRO_LOCKED_REGION implements Structure.ByValue {
        public ByValue() {
        }
        public ByValue(Pointer pointer) {
            super(pointer);
        }
    }
    public ALLEGRO_LOCKED_REGION() {
    }
    public ALLEGRO_LOCKED_REGION(Pointer pointer) {
        super(pointer);
    }
    public Pointer data;
    public int format;
    public int pitch;
    public int pixel_size;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("data", "format", "pitch", "pixel_size");
    }
}
