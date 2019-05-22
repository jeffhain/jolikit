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
 * \name SDL_INIT_*
 *
 * These are the flags which may be passed to SDL_Init().  You should
 * specify the subsystems which you will be using in your application.
 */
public enum SdlInitFlag implements InterfaceIntValued {
    SDL_INIT_TIMER(0x00000001),
    SDL_INIT_AUDIO(0x00000010),
    /**
     * SDL_INIT_VIDEO implies SDL_INIT_EVENTS
     */
    SDL_INIT_VIDEO(0x00000020),
    /**
     * SDL_INIT_JOYSTICK implies SDL_INIT_EVENTS
     */
    SDL_INIT_JOYSTICK(0x00000200),
    SDL_INIT_HAPTIC(0x00001000),
    /**
     * SDL_INIT_GAMECONTROLLER implies SDL_INIT_JOYSTICK
     */
    SDL_INIT_GAMECONTROLLER(0x00002000),
    SDL_INIT_EVENTS(0x00004000),
    /**
     * compatibility; this flag is ignored.
     */
    SDL_INIT_NOPARACHUTE(0x00100000),
    SDL_INIT_EVERYTHING(
            0x00000001 | 0x00000010 | 0x00000020
            | 0x00000200 | 0x00001000 | 0x00002000
            | 0x00004000);
    
    private static final IntValuedHelper<SdlInitFlag> HELPER =
            new IntValuedHelper<SdlInitFlag>(SdlInitFlag.values());

    private final int intValue;

    private SdlInitFlag(int intValue) {
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
    public static SdlInitFlag valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
