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
 * typedef struct ALLEGRO_TOUCH_EVENT
 * {
 *    _AL_EVENT_HEADER(struct ALLEGRO_TOUCH_INPUT)
 *    struct ALLEGRO_DISPLAY *display;
 *    / * (id) Identifier of the event, always positive number.
 *     * (x, y) Touch position on the screen in 1:1 resolution.
 *     * (dx, dy) Relative touch position.
 *     * (primary) True, if touch is a primary one (usually first one).
 *     * /
 *    int id;
 *    float x, y;
 *    float dx, dy;
 *    bool primary;
 * } ALLEGRO_TOUCH_EVENT;
 */
public class ALLEGRO_TOUCH_EVENT extends _AL_EVENT_HEADER {
    public static class ByReference extends ALLEGRO_TOUCH_EVENT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_TOUCH_EVENT implements Structure.ByValue {
    }
    public Pointer display;
    public int id;
    public float x;
    public float y;
    public float dx;
    public float dy;
    public boolean primary;
    @Override
    protected List<String> getFieldOrder() {
        return concat(super.getFieldOrder(), "display", "id", "x", "y", "dx", "dy", "primary");
    }
}
