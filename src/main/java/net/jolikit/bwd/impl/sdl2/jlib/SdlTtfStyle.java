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

public enum SdlTtfStyle implements InterfaceIntValued {
    TTF_STYLE_NORMAL(0x00),
    TTF_STYLE_BOLD(0x01),
    TTF_STYLE_ITALIC(0x02),
    TTF_STYLE_UNDERLINE(0x04),
    TTF_STYLE_STRIKETHROUGH(0x08);
    
    private static final IntValuedHelper<SdlTtfStyle> HELPER =
            new IntValuedHelper<SdlTtfStyle>(SdlTtfStyle.values());
    
    private final int intValue;
    
    private SdlTtfStyle(int intValue) {
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
    public static SdlTtfStyle valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
