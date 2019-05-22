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
package net.jolikit.bwd.impl.sdl2.jlib;

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

/**
 * enum IMG_InitFlags
 */
public enum SdlImgInitFlag implements InterfaceIntValued {
    IMG_INIT_JPG(0x00000001),
    IMG_INIT_PNG(0x00000002),
    IMG_INIT_TIF(0x00000004),
    IMG_INIT_WEBP(0x00000008);
    
    private static final IntValuedHelper<SdlImgInitFlag> HELPER =
            new IntValuedHelper<SdlImgInitFlag>(SdlImgInitFlag.values());

    private final int intValue;

    private SdlImgInitFlag(int intValue) {
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
    public static SdlImgInitFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
