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
public enum AlgrBlitFlag implements InterfaceIntValued {
    ALLEGRO_FLIP_HORIZONTAL(0x00001),
    ALLEGRO_FLIP_VERTICAL(0x00002);
    
    private static final IntValuedHelper<AlgrBlitFlag> HELPER =
            new IntValuedHelper<AlgrBlitFlag>(AlgrBlitFlag.values());

    private final int intValue;
    
    private AlgrBlitFlag(int intValue) {
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
    public static AlgrBlitFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
