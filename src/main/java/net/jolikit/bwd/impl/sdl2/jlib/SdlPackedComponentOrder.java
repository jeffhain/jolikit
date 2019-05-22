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
 * Packed component order, high bit -> low bit.
 */
public enum SdlPackedComponentOrder implements InterfaceIntValued {
    SDL_PACKEDORDER_NONE,
    SDL_PACKEDORDER_XRGB,
    SDL_PACKEDORDER_RGBX,
    SDL_PACKEDORDER_ARGB,
    SDL_PACKEDORDER_RGBA,
    SDL_PACKEDORDER_XBGR,
    SDL_PACKEDORDER_BGRX,
    SDL_PACKEDORDER_ABGR,
    SDL_PACKEDORDER_BGRA;

    private static final IntValuedHelper<SdlPackedComponentOrder> HELPER =
            new IntValuedHelper<SdlPackedComponentOrder>(SdlPackedComponentOrder.values());

    @Override
    public int intValue() {
        return this.ordinal();
    }

    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlPackedComponentOrder valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
