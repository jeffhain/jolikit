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
 * typedef struct ALLEGRO_MOUSE_STATE ALLEGRO_MOUSE_STATE;
 * struct ALLEGRO_MOUSE_STATE
 * {
 *    / * (x, y) Primary mouse position
 *     * (z) Mouse wheel position (1D 'wheel'), or,
 *     * (w, z) Mouse wheel position (2D 'ball')
 *     * display - the display the mouse is on (coordinates are relative to this)
 *     * pressure - the pressure appleid to the mouse (for stylus/tablet)
 *     * /
 *    int x;
 *    int y;
 *    int z;
 *    int w;
 *    int more_axes[ALLEGRO_MOUSE_MAX_EXTRA_AXES];
 *    int buttons;
 *    float pressure;
 *    struct ALLEGRO_DISPLAY *display;
 */
public class ALLEGRO_MOUSE_STATE extends Structure {
    public static class ByReference extends ALLEGRO_MOUSE_STATE implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_MOUSE_STATE implements Structure.ByValue {
    }
    /**
     * Allow up to four extra axes for future expansion.
     */
    public static final int ALLEGRO_MOUSE_MAX_EXTRA_AXES = 4;
    public int x;
    public int y;
    public int z;
    public int w;
    public final int[] more_axes = new int[ALLEGRO_MOUSE_MAX_EXTRA_AXES];
    public int buttons;
    public float pressure;
    public Pointer display;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("x", "y", "z", "w", "more_axes", "buttons", "pressure", "display");
    }
}
