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
 * enum SDL_SystemCursor
 */
public enum SdlSystemCursor implements InterfaceIntValued {
    SDL_SYSTEM_CURSOR_ARROW,
    /**
     * Text cursor.
     */
    SDL_SYSTEM_CURSOR_IBEAM,
    SDL_SYSTEM_CURSOR_WAIT,
    SDL_SYSTEM_CURSOR_CROSSHAIR,
    /**
     * Small wait cursor (or Wait if not available).
     */
    SDL_SYSTEM_CURSOR_WAITARROW,
    /**
     * Double arrow pointing northwest and southeast.
     */
    SDL_SYSTEM_CURSOR_SIZENWSE,
    /**
     * Double arrow pointing northeast and southwest.
     */
    SDL_SYSTEM_CURSOR_SIZENESW,
    /**
     * Double arrow pointing west and east.
     */
    SDL_SYSTEM_CURSOR_SIZEWE,
    /**
     * Double arrow pointing north and south.
     */
    SDL_SYSTEM_CURSOR_SIZENS,
    /**
     * Four pointed arrow pointing north, south, east, and west.
     */
    SDL_SYSTEM_CURSOR_SIZEALL,
    /**
     * Slashed circle or crossbones.
     */
    SDL_SYSTEM_CURSOR_NO,
    /**
     * Hand.
     */
    SDL_SYSTEM_CURSOR_HAND,
    SDL_NUM_SYSTEM_CURSORS;
    
    private static final IntValuedHelper<SdlSystemCursor> HELPER =
            new IntValuedHelper<SdlSystemCursor>(SdlSystemCursor.values());
    
    @Override
    public int intValue() {
        return this.ordinal();
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlSystemCursor valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
