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
 * enum SDL_Keymod
 * 
 * \brief Enumeration of valid key mods (possibly OR'd together).
 */
public enum SdlKeymod implements InterfaceIntValued {
    KMOD_NONE(0x0000),
    KMOD_LSHIFT(0x0001),
    KMOD_RSHIFT(0x0002),
    KMOD_LCTRL(0x0040),
    KMOD_RCTRL(0x0080),
    KMOD_LALT(0x0100),
    KMOD_RALT(0x0200),
    KMOD_LGUI(0x0400),
    KMOD_RGUI(0x0800),
    KMOD_NUM(0x1000),
    KMOD_CAPS(0x2000),
    KMOD_MODE(0x4000),
    KMOD_RESERVED(0x8000),
    //
    KMOD_CTRL(0x0040 | 0x0080),
    KMOD_SHIFT(0x0001 | 0x0002),
    KMOD_ALT(0x0100 | 0x0200),
    KMOD_GUI(0x0400 | 0x0800);
    
    private static final IntValuedHelper<SdlKeymod> HELPER =
            new IntValuedHelper<SdlKeymod>(SdlKeymod.values());

    private final int intValue;

    private SdlKeymod(int intValue) {
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
    public static SdlKeymod valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
