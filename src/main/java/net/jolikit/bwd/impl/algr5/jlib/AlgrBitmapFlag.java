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
 * Possible bit combinations for the flags parameter of al_set_new_bitmap_flags.
 */
public enum AlgrBitmapFlag implements InterfaceIntValued {
    ALLEGRO_MEMORY_BITMAP(0x0001),
    /**
     * now a bitmap loader flag
     */
    _ALLEGRO_KEEP_BITMAP_FORMAT(0x0002),
    /**
     * no longer honoured
     */
    ALLEGRO_FORCE_LOCKING(0x0004),
    ALLEGRO_NO_PRESERVE_TEXTURE(0x0008),
    /**
     * now a render state flag
     */
    _ALLEGRO_ALPHA_TEST(0x0010),
    _ALLEGRO_INTERNAL_OPENGL(0x0020),
    ALLEGRO_MIN_LINEAR(0x0040),
    ALLEGRO_MAG_LINEAR(0x0080),
    ALLEGRO_MIPMAP(0x0100),
    /**
     * now a bitmap loader flag
     */
    _ALLEGRO_NO_PREMULTIPLIED_ALPHA(0x0200),
    ALLEGRO_VIDEO_BITMAP(0x0400),
    ALLEGRO_CONVERT_BITMAP(0x1000);
    
    private static final IntValuedHelper<AlgrBitmapFlag> HELPER =
            new IntValuedHelper<AlgrBitmapFlag>(AlgrBitmapFlag.values());

    private final int intValue;
    
    private AlgrBitmapFlag(int intValue) {
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
    public static AlgrBitmapFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
