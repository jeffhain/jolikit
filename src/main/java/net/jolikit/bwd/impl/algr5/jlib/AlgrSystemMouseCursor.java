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

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

/**
 * enum ALLEGRO_SYSTEM_MOUSE_CURSOR
 */
public enum AlgrSystemMouseCursor implements InterfaceIntValued {
    /**
     * !!!Cannot be set!!!
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_NONE,
    /**
     * Typically arrow.
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_DEFAULT,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_ARROW,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_BUSY,
    /**
     * Arrow and question mark.
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_QUESTION,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_EDIT,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_MOVE,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_N,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_W,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_S,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_E,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_NW,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_SW,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_SE,
    ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_NE,
    /**
     * Arrow and question mark.
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_PROGRESS,
    /**
     * Crosshair.
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_PRECISION,
    /**
     * Hand with one (index) pointing finger.
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_LINK,
    /**
     * Arrow up.
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_ALT_SELECT,
    /**
     * Circle with a slash inside.
     */
    ALLEGRO_SYSTEM_MOUSE_CURSOR_UNAVAILABLE;
    
    private static final IntValuedHelper<AlgrSystemMouseCursor> HELPER =
            new IntValuedHelper<AlgrSystemMouseCursor>(AlgrSystemMouseCursor.values());
    
    @Override
    public int intValue() {
        return this.ordinal();
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static AlgrSystemMouseCursor valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
