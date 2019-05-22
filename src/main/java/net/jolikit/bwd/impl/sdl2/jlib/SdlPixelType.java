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
 * enum SDL_PixelType
 */
public enum SdlPixelType implements InterfaceIntValued {
    SDL_PIXELTYPE_UNKNOWN,
    SDL_PIXELTYPE_INDEX1,
    SDL_PIXELTYPE_INDEX4,
    SDL_PIXELTYPE_INDEX8,
    SDL_PIXELTYPE_PACKED8,
    SDL_PIXELTYPE_PACKED16,
    SDL_PIXELTYPE_PACKED32,
    SDL_PIXELTYPE_ARRAYU8,
    SDL_PIXELTYPE_ARRAYU16,
    SDL_PIXELTYPE_ARRAYU32,
    SDL_PIXELTYPE_ARRAYF16,
    SDL_PIXELTYPE_ARRAYF32;

    private static final IntValuedHelper<SdlPixelType> HELPER =
            new IntValuedHelper<SdlPixelType>(SdlPixelType.values());

    @Override
    public int intValue() {
        return this.ordinal();
    }

    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlPixelType valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
