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
 * Flags for the blitting functions.
 */
public enum AlgrBitmapLoadFlag implements InterfaceIntValued {
    /**
     * was a bitmap flag in 5.0
     */
    ALLEGRO_KEEP_BITMAP_FORMAT(0x0002),
    /**
     * was a bitmap flag in 5.0
     */
    ALLEGRO_NO_PREMULTIPLIED_ALPHA(0x0200),
    ALLEGRO_KEEP_INDEX(0x0800);
    
    private static final IntValuedHelper<AlgrBitmapLoadFlag> HELPER =
            new IntValuedHelper<AlgrBitmapLoadFlag>(AlgrBitmapLoadFlag.values());

    private final int intValue;
    
    private AlgrBitmapLoadFlag(int intValue) {
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
    public static AlgrBitmapLoadFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
