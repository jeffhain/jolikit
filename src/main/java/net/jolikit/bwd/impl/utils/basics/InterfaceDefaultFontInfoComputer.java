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
package net.jolikit.bwd.impl.utils.basics;

import java.util.SortedSet;

import net.jolikit.bwd.api.fonts.BwdFontKind;

/**
 * To provide info used to create the default font.
 */
public interface InterfaceDefaultFontInfoComputer {

    /**
     * Specifying system and user font kinds separately:
     * system font kinds BEFORE eventual system fonts replacements
     * with user fonts, so that default font kind can be deterministically
     * chosen from loaded system fonts whatever the loaded user fonts,
     * and user font kinds AFTER eventual system fonts replacements
     * with user fonts, so that replaced fonts can be figured out.
     * As a result, some font kinds might be in both sets.
     * 
     * @param systemLoadedFontKindSet Set of font kinds of loaded system fonts,
     *        BEFORE eventual removals due to user fonts loading. Never empty.
     * @param userLoadedFontKindSet Set of font kinds of loaded user fonts,
     *        whether or not they were already among font kinds of loaded
     *        system fonts. Can be empty.
     */
    public BwdFontKind computeDefaultFontKind(
            SortedSet<BwdFontKind> systemLoadedFontKindSet,
            SortedSet<BwdFontKind> userLoadedFontKindSet);

    /**
     * Must be called after computeDefaultFontKind(...).
     * 
     * @return True if must create default font of getDefaultFontSize() size,
     *         false if must create default font of getTargetDefaultFontHeight() target height.
     */
    public boolean getMustUseFontSizeElseTargetFontHeight();

    /**
     * Must be called after getMustUseFontSizeElseTargetFontHeight(), if it returned true.
     * 
     * @return Font size to use, if getMustUseFontSizeElseTargetFontHeight() is true.
     */
    public int getDefaultFontSize();
    
    /**
     * Must be called after getMustUseFontSizeElseTargetFontHeight(), if it returned false.
     * 
     * @return Target font height to use, if getMustUseFontSizeElseTargetFontHeight() is false.
     */
    public int getTargetDefaultFontHeight();
}
