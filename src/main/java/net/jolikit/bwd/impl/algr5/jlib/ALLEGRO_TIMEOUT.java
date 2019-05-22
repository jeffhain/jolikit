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

import com.sun.jna.Structure;

/**
 * typedef struct ALLEGRO_TIMEOUT ALLEGRO_TIMEOUT;
 * struct ALLEGRO_TIMEOUT {
 *    uint64_t __pad1__;
 *    uint64_t __pad2__;
 * };
 */
public class ALLEGRO_TIMEOUT extends Structure {
    public static class ByReference extends ALLEGRO_TIMEOUT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_TIMEOUT implements Structure.ByValue {
    }
    public long __pad1__;
    public long __pad2__;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("__pad1__", "__pad2__");
    }
}
