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
 * typedef struct ALLEGRO_EVENT_SOURCE ALLEGRO_EVENT_SOURCE;
 * struct ALLEGRO_EVENT_SOURCE
 * {
 *    int __pad[32];
 * };
 */
public class ALLEGRO_EVENT_SOURCE extends Structure {
    public static class ByReference extends ALLEGRO_EVENT_SOURCE implements Structure.ByReference {
        public ByReference() {
        }
        public ByReference(Pointer pointer) {
            super(pointer);
        }
    }
    public static class ByValue extends ALLEGRO_EVENT_SOURCE implements Structure.ByValue {
        public ByValue() {
        }
        public ByValue(Pointer pointer) {
            super(pointer);
        }
    }
    public int[] __pad = new int[32];
    public ALLEGRO_EVENT_SOURCE() {
    }
    public ALLEGRO_EVENT_SOURCE(Pointer pointer) {
        super(pointer);
    }
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("__pad");
    }
}
