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
 * typedef struct ALLEGRO_JOYSTICK_EVENT
 * {
 *    _AL_EVENT_HEADER(struct ALLEGRO_JOYSTICK)
 *    struct ALLEGRO_JOYSTICK *id;
 *    int stick;
 *    int axis;
 *    float pos;
 *    int button;
 * } ALLEGRO_JOYSTICK_EVENT;
 */
public class ALLEGRO_JOYSTICK_EVENT extends _AL_EVENT_HEADER {
    public static class ByReference extends ALLEGRO_JOYSTICK_EVENT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_JOYSTICK_EVENT implements Structure.ByValue {
    }
    public Pointer id;
    public int stick;
    public int axis;
    public float pos;
    public int button;
    @Override
    protected List<String> getFieldOrder() {
        return concat(super.getFieldOrder(), "id", "stick", "axis", "pos", "button");
    }
}
