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
 * enum SDL_MouseWheelDirection
 * 
 * \brief Scroll direction types for the Scroll event
 */
public enum SdlMouseWheelDirection implements InterfaceIntValued {
    /**
     * The scroll direction is normal.
     * 
     * TODO sdl No clue what "normal" means.
     */
    SDL_MOUSEWHEEL_NORMAL,
    /**
     * The scroll direction is flipped / natural.
     */
    SDL_MOUSEWHEEL_FLIPPED;

    private static final IntValuedHelper<SdlMouseWheelDirection> HELPER =
            new IntValuedHelper<SdlMouseWheelDirection>(SdlMouseWheelDirection.values());

    @Override
    public int intValue() {
        return this.ordinal();
    }

    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlMouseWheelDirection valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
