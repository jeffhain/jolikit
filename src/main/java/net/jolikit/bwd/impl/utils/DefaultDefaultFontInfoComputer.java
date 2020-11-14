/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.utils;

import java.util.SortedSet;

import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.BwdFontStyles;
import net.jolikit.bwd.impl.utils.basics.BindingStringUtils;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;

public class DefaultDefaultFontInfoComputer implements InterfaceDefaultFontInfoComputer {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * 12 makes Allegro and SDL fonts too ugly.
     */
    private static final int DEFAULT_TARGET_FONT_HEIGHT = 13;
    
    /**
     * Default font is the first which family contains
     * the parts of one of these sets of parts,
     * with main loop on sets of parts and inner loop on fonts.
     */
    private static final String[][] PARTS_ARR = new String[][]{
            {"lucida","console"},
            {"lucida"},
            {"sans","serif"},
            {"sans"},
            {"free","mono"},
            {"mono"},
    };
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final boolean mustUseFontSizeElseTargetFontHeight;

    private final int fontSize;

    private final int targetFontHeight;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates default fonts of target font height 13.
     */
    public DefaultDefaultFontInfoComputer() {
        this(
                false,
                -1,
                DEFAULT_TARGET_FONT_HEIGHT);
    }
    
    public DefaultDefaultFontInfoComputer(
            boolean mustUseFontSizeElseTargetFontHeight,
            int fontSize,
            int targetFontHeight) {
        this.mustUseFontSizeElseTargetFontHeight = mustUseFontSizeElseTargetFontHeight;
        this.fontSize = fontSize;
        this.targetFontHeight = targetFontHeight;
    }
    
    @Override
    public BwdFontKind computeDefaultFontKind(
            SortedSet<BwdFontKind> systemLoadedFontKindSet,
            SortedSet<BwdFontKind> userLoadedFontKindSet) {
        
        BwdFontKind defaultFontKind = null;

        for (String[] parts : PARTS_ARR) {
            defaultFontKind = getMatchingFontKind(
                    systemLoadedFontKindSet,
                    parts);
            if (defaultFontKind != null) {
                break;
            }
        }
        
        if (defaultFontKind == null) {
            defaultFontKind = systemLoadedFontKindSet.first();
        }
        
        return defaultFontKind;
    }

    @Override
    public boolean getMustUseFontSizeElseTargetFontHeight() {
        return this.mustUseFontSizeElseTargetFontHeight;
    }

    @Override
    public int getDefaultFontSize() {
        return this.fontSize;
    }
    
    @Override
    public int getTargetDefaultFontHeight() {
        return this.targetFontHeight;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return First font kind found which family contains the specified parts,
     *         ignoring case, or null if none found.
     */
    private static BwdFontKind getMatchingFontKind(
            SortedSet<BwdFontKind> fontKindSet,
            String... parts) {
        
        BwdFontKind defaultFontKind = null;

        for (BwdFontKind fontKind : fontKindSet) {
            if (fontKind.style() != BwdFontStyles.NORMAL) {
                continue;
            }
            
            final String family = fontKind.family();
            boolean onePartNotFound = false;
            for (String part : parts) {
                if (!BindingStringUtils.containsIgnoreCase(family, part)) {
                    onePartNotFound = true;
                    break;
                }
            }
            if (!onePartNotFound) {
                defaultFontKind = fontKind;
                break;
            }
        }
        
        return defaultFontKind;
    }
}
