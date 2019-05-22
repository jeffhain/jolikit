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
import com.sun.jna.ptr.IntByReference;

/**
 * typedef struct ALLEGRO_USER_EVENT ALLEGRO_USER_EVENT;
 * struct ALLEGRO_USER_EVENT
 * {
 *    _AL_EVENT_HEADER(struct ALLEGRO_EVENT_SOURCE)
 *    struct ALLEGRO_USER_EVENT_DESCRIPTOR *__internal__descr;
 *    intptr_t data1;
 *    intptr_t data2;
 *    intptr_t data3;
 *    intptr_t data4;
 * };
 * 
 * We don't use it in practice, but needs to be instantiable,
 * if we want to have it in the union. TODO si, make it usable.
 */
public class ALLEGRO_USER_EVENT extends _AL_EVENT_HEADER {
    public static class ByReference extends ALLEGRO_USER_EVENT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_USER_EVENT implements Structure.ByValue {
    }
    public ALLEGRO_USER_EVENT() {
    }
    public Pointer __internal__descr;
    public IntByReference data1;
    public IntByReference data2;
    public IntByReference data3;
    public IntByReference data4;
    @Override
    protected List<String> getFieldOrder() {
        return concat(super.getFieldOrder(), "__internal__descr", "data1", "data2", "data3", "data4");
    }
}
