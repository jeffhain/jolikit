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
 * These are the flags which may be passed to SDL_CreateRGBSurface(...).
 */
public enum SdlSurfaceFlag implements InterfaceIntValued {
    /**
     * Just here for compatibility
     */
    SDL_SWSURFACE(0),
    /**
     * Surface uses preallocated memory
     */
    SDL_PREALLOC(0x00000001),
    /**
     * Surface is RLE encoded
     */
    SDL_RLEACCEL(0x00000002),
    /**
     * Surface is referenced internally
     */
    SDL_DONTFREE(0x00000004);
    
    private static final IntValuedHelper<SdlSurfaceFlag> HELPER =
            new IntValuedHelper<SdlSurfaceFlag>(SdlSurfaceFlag.values());

    private final int intValue;

    private SdlSurfaceFlag(int intValue) {
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
    public static SdlSurfaceFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
