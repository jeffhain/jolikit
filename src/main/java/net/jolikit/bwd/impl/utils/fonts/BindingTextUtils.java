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

import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.utils.basics.BindingStringUtils;

public class BindingTextUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param str Some char sequence.
     * @return Whether the specified char sequence contains "bold",
     *         ignoring case.
     */
    public static boolean containsBoldInfo(CharSequence str) {
        return BindingStringUtils.containsIgnoreCase(str, "bold");
    }
    
    /**
     * @param str Some char sequence.
     * @return Whether the specified char sequence contains "italic",
     *         "oblique" or "slanted", ignoring case.
     */
    public static boolean containsItalicInfo(CharSequence str) {
        return BindingStringUtils.containsIgnoreCase(str, "italic")
                || BindingStringUtils.containsIgnoreCase(str, "oblique")
                || BindingStringUtils.containsIgnoreCase(str, "slanted");
    }
    
    /*
     * 
     */
    
    /**
     * @param text Some text.
     * @return The specified text, stripped of NUL chars.
     */
    public static String withoutNul(String text) {
        
        /*
         * Looking for NUL.
         */
        
        final int firstNulIndex = firstNulIndex(text);
        if (firstNulIndex < 0) {
            return text;
        }
        
        /*
         * Stripping.
         */
        
        // Default size with CP count (exact if all in BMP).
        final StringBuilder sb = new StringBuilder(text.length());
        int ci = 0;
        while (ci < text.length()) {
            final int cp = text.codePointAt(ci);
            if (cp != 0) {
                sb.appendCodePoint(cp);
            }
            ci += Character.charCount(cp);
        }
        return sb.toString();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingTextUtils() {
    }
    
    public static void throwIAEInvalidCodePoint(int codePoint) {
        throw new IllegalArgumentException("codePoint [" + BwdUnicode.toDisplayString(codePoint) + "] is not valid");
    }
    
    private static int firstNulIndex(String text) {
        int firstNulIndex = -1;
        {
            int ci = 0;
            while (ci < text.length()) {
                // NUL never uses two chars,
                // so we can just check chars one by one.
                final char ch = text.charAt(ci);
                if (ch == 0) {
                    firstNulIndex = ci;
                    break;
                }
                ci++;
            }
        }
        return firstNulIndex;
    }
}
