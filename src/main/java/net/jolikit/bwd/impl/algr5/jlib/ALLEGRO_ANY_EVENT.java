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

import com.sun.jna.Structure;

/**
 * typedef struct ALLEGRO_ANY_EVENT
 * {
 *    _AL_EVENT_HEADER(ALLEGRO_EVENT_SOURCE)
 * } ALLEGRO_ANY_EVENT;
 */
public class ALLEGRO_ANY_EVENT extends _AL_EVENT_HEADER {
    public static class ByReference extends ALLEGRO_ANY_EVENT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_ANY_EVENT implements Structure.ByValue {
    }
}
