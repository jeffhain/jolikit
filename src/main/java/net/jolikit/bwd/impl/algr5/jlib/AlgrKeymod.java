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

public enum AlgrKeymod implements InterfaceIntValued {
    ALLEGRO_KEYMOD_SHIFT(0x00001),
    ALLEGRO_KEYMOD_CTRL(0x00002),
    ALLEGRO_KEYMOD_ALT(0x00004),
    ALLEGRO_KEYMOD_LWIN(0x00008),
    ALLEGRO_KEYMOD_RWIN(0x00010),
    ALLEGRO_KEYMOD_MENU(0x00020),
    ALLEGRO_KEYMOD_ALTGR(0x00040),
    ALLEGRO_KEYMOD_COMMAND(0x00080),
    ALLEGRO_KEYMOD_SCROLLLOCK(0x00100),
    ALLEGRO_KEYMOD_NUMLOCK(0x00200),
    ALLEGRO_KEYMOD_CAPSLOCK(0x00400),
    ALLEGRO_KEYMOD_INALTSEQ(0x00800),
    ALLEGRO_KEYMOD_ACCENT1(0x01000),
    ALLEGRO_KEYMOD_ACCENT2(0x02000),
    ALLEGRO_KEYMOD_ACCENT3(0x04000),
    ALLEGRO_KEYMOD_ACCENT4(0x08000);
    
    private static final IntValuedHelper<AlgrKeymod> HELPER =
            new IntValuedHelper<AlgrKeymod>(AlgrKeymod.values());

    private final int intValue;
    
    private AlgrKeymod(int intValue) {
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
    public static AlgrKeymod valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
