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
package net.jolikit.bwd.impl.sdl2;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.fonts.InterfaceCanFontDisplayComputer;
import net.jolikit.bwd.impl.utils.fonts.InterfaceFontDisposeCallListener;

import com.sun.jna.Pointer;

public class SdlBwdFont extends AbstractBwdFont<Pointer> implements Comparable<SdlBwdFont> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final SdlBwdFontMetrics metrics;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SdlBwdFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            Pointer backingFont) {
        super(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                backingFont);
        
        this.metrics = new SdlBwdFontMetrics(backingFont);
    }
    
    @Override
    public int compareTo(SdlBwdFont other) {
        return this.compareToImpl(other);
    }

    /*
     * 
     */

    @Override
    public InterfaceBwdFontMetrics fontMetrics() {
        return this.metrics;
    }
}
