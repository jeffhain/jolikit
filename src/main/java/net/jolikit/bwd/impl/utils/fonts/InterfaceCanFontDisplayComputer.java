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
 * Interface to inject canDisplay(...) logic into fonts.
 */
public interface InterfaceCanFontDisplayComputer {

    /**
     * Does not need to throw if the specified code point is invalid,
     * to keep implementations simple.
     * The check can be done only in font's method.
     * 
     * @param font A font.
     * @param codePoint A valid code point.
     * @return True if the specified font can displayed the specified
     *         code point for sure, false otherwise.
     */
    public boolean canFontDisplay(
            InterfaceBwdFont font,
            int codePoint);
}
