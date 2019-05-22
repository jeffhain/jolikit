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
 * Packed component layout.
 */
public enum SdlPackedComponentLayout implements InterfaceIntValued {
    SDL_PACKEDLAYOUT_NONE,
    SDL_PACKEDLAYOUT_332,
    SDL_PACKEDLAYOUT_4444,
    SDL_PACKEDLAYOUT_1555,
    SDL_PACKEDLAYOUT_5551,
    SDL_PACKEDLAYOUT_565,
    SDL_PACKEDLAYOUT_8888,
    SDL_PACKEDLAYOUT_2101010,
    SDL_PACKEDLAYOUT_1010102;

    private static final IntValuedHelper<SdlPackedComponentLayout> HELPER =
            new IntValuedHelper<SdlPackedComponentLayout>(SdlPackedComponentLayout.values());

    @Override
    public int intValue() {
        return this.ordinal();
    }

    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlPackedComponentLayout valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
