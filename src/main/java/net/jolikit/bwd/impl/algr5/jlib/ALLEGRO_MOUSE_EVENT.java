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
 * typedef struct ALLEGRO_MOUSE_EVENT
 * {
 *    _AL_EVENT_HEADER(struct ALLEGRO_MOUSE)
 *    struct ALLEGRO_DISPLAY *display;
 *    / * (display) Window the event originate from
 *     * (x, y) Primary mouse position
 *     * (z) Mouse wheel position (1D 'wheel'), or,
 *     * (w, z) Mouse wheel position (2D 'ball')
 *     * (pressure) The pressure applied, for stylus (0 or 1 for normal mouse)
 *     * /
 *    int x,  y,  z, w;
 *    int dx, dy, dz, dw;
 *    unsigned int button;
 *    float pressure;
 * } ALLEGRO_MOUSE_EVENT;
 */
public class ALLEGRO_MOUSE_EVENT extends _AL_EVENT_HEADER {
    public static class ByReference extends ALLEGRO_MOUSE_EVENT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_MOUSE_EVENT implements Structure.ByValue {
    }
    public Pointer display;
    public int x;
    public int y;
    public int z;
    public int w;
    public int dx;
    public int dy;
    public int dz;
    public int dw;
    public int button;
    public float pressure;
    /**
     * @return A clone, without the annoying Cloneable API.
     */
    public ALLEGRO_MOUSE_EVENT duplicate() {
        final ALLEGRO_MOUSE_EVENT dolly = new ALLEGRO_MOUSE_EVENT();
        dolly.type = this.type;
        dolly.source = this.source;
        dolly.timestamp = this.timestamp;
        //
        dolly.display = this.display;
        dolly.x = this.x;
        dolly.y = this.y;
        dolly.z = this.z;
        dolly.w = this.w;
        dolly.dx = this.dx;
        dolly.dy = this.dy;
        dolly.dz = this.dz;
        dolly.dw = this.dw;
        dolly.button = this.button;
        dolly.pressure = this.pressure;
        return dolly;
    }
    @Override
    protected List<String> getFieldOrder() {
        return concat(super.getFieldOrder(),
                "display",
                "x", "y", "z", "w",
                "dx", "dy", "dz", "dw",
                "button", "pressure");
    }
}
