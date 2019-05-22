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
 * Possible bit combinations for the flags parameter of al_create_display.
 */
public enum AlgrDisplayFlag implements InterfaceIntValued {
    ALLEGRO_WINDOWED(1 << 0),
    ALLEGRO_FULLSCREEN(1 << 1),
    ALLEGRO_OPENGL(1 << 2),
    ALLEGRO_DIRECT3D_INTERNAL(1 << 3),
    ALLEGRO_RESIZABLE(1 << 4),
    /**
     * Older synonym: ALLEGRO_NOFRAME.
     */
    ALLEGRO_FRAMELESS(1 << 5),
    ALLEGRO_GENERATE_EXPOSE_EVENTS(1 << 6),
    ALLEGRO_OPENGL_3_0(1 << 7),
    ALLEGRO_OPENGL_FORWARD_COMPATIBLE(1 << 8),
    ALLEGRO_FULLSCREEN_WINDOW(1 << 9),
    ALLEGRO_MINIMIZED(1 << 10),
    ALLEGRO_PROGRAMMABLE_PIPELINE(1 << 11),
    ALLEGRO_GTK_TOPLEVEL_INTERNAL(1 << 12),
    ALLEGRO_MAXIMIZED(1 << 13),
    ALLEGRO_OPENGL_ES_PROFILE(1 << 14);
    
    private static final IntValuedHelper<AlgrDisplayFlag> HELPER =
            new IntValuedHelper<AlgrDisplayFlag>(AlgrDisplayFlag.values());

    private final int intValue;
    
    private AlgrDisplayFlag(int intValue) {
        this.intValue = intValue;
    }
    
    @Override
    public int intValue() {
        return this.intValue;
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static AlgrDisplayFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
