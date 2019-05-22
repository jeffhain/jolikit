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
package net.jolikit.bwd.impl.utils.fonts;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;

/**
 * Implementation based on a CodePointSet.
 */
public class CodePointSetCfdc implements InterfaceCanFontDisplayComputer {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    public static final CodePointSetCfdc DEFAULT_EMPTY = new CodePointSetCfdc(CodePointSet.DEFAULT_EMPTY);
    
    private final CodePointSet codePointSet;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public CodePointSetCfdc(CodePointSet codePointSet) {
        this.codePointSet = codePointSet;
    }
    
    @Override
    public boolean canFontDisplay(
            InterfaceBwdFont font,
            int codePoint) {
        return this.codePointSet.contains(codePoint);
    }
}
