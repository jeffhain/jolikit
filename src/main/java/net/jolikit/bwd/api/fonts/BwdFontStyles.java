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
package net.jolikit.bwd.api.fonts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jolikit.lang.NbrsUtils;

/**
 * Utilities to deal with font styles, which are bits fields,
 * each bit corresponding to a pure style (other than NORMAL, which is 0),
 * and mixed styles being allowed.
 * 
 * Bindings can support additional styles, as long as
 * they don't conflict with the bits defined in this class.
 * 
 * Not using an enum class, to make it easier to deal transparently
 * with these default styles and with additional ones.
 * 
 * "underline" and "strikethrough" decorations are not part of font style
 * (even if in some libraries (such as SDL2), they are), for it would multiply
 * the amount of "loaded" fonts (16 instead of 4 per family), and would
 * complicate font disposal treatments further (due to sharing backing fonts
 * for different decorations).
 * Instead, they can be implemented by using drawLine(...) after drawText(...)
 * (and possibly using another color, or with line stipples, etc.).
 */
public class BwdFontStyles {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int BIT_SIZE = 2;
    
    private static final int MIN_BIT = 1;
    private static final int MAX_BIT = (1 << (BIT_SIZE - 1));
    
    /**
     * Keeping room for 2 eventual new styles
     * (for reserved bits to correspond to a full hex digit).
     */
    private static final int MIN_ADDITIONAL_STYLE_BIT = (1 << (BIT_SIZE + 2));
    
    /*
     * Only having constant for pure styles (styles bits),
     * else it doesn't scale.
     */
    
    public static final int NORMAL = 0;
    
    /**
     * Pure bold style.
     */
    public static final int BOLD = MIN_BIT;
    
    /**
     * Pure italic (slanted) style.
     * 
     * Either true italic, or oblique.
     */
    public static final int ITALIC = MAX_BIT;

    /*
     * 
     */

    private static final List<Integer> PURE_STYLE_LIST;
    static {
        final List<Integer> list = new ArrayList<Integer>();
        
        list.add(NORMAL);
        
        for (int i = 0; i < BIT_SIZE; i++) {
            final int style = (1 << i);
            list.add(style);
        }
        
        PURE_STYLE_LIST = Collections.unmodifiableList(list);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Methods rather than constants, to avoid confusion with constants.
     */
    
    /**
     * @return An unmodifiable list of the pure styles (not mixed)
     *         defined in this class, NORMAL included, in increasing order. 
     */
    public static List<Integer> pureStyleList() {
        return PURE_STYLE_LIST;
    }
    
    /**
     * @return Min bit for eventual additional styles that can be supported
     *         by a binding.
     */
    public static int minAdditionalStyleBit() {
        return MIN_ADDITIONAL_STYLE_BIT;
    }

    /*
     * 
     */


    /**
     * @return A string representation of the specified style.
     */
    public static String toString(int style) {
        final StringBuilder sb = new StringBuilder();
        boolean didAddStyle = false;
        
        if (isBold(style)) {
            sb.append("bold");
            didAddStyle = true;
        }
        if (isItalic(style)) {
            if (didAddStyle) {
                sb.append("-");
            }
            sb.append("italic");
            didAddStyle = true;
        }
        
        // Eventual additional styles.
        if ((style >>> BIT_SIZE) != 0) {
            if (didAddStyle) {
                sb.append("-");
            }
            // Shifting known bits out, and then shifting back
            // to preserve bits positions, else it would be confusing.
            final int additionalBits = ((style >> BIT_SIZE) << BIT_SIZE);
            sb.append("unknown_style(");
            sb.append(toHexString(additionalBits));
            sb.append(")");
            didAddStyle = true;
        }
        
        if (!didAddStyle) {
            sb.append("normal");
        }
        
        return sb.toString();
    }

    /*
     * 
     */
    
    /**
     * @return True if the specified style is bold
     *         (and possibly something else), false otherwise.
     */
    public static boolean isBold(int style) {
        return (style & BOLD) != 0;
    }
    
    /**
     * @return True if the specified style is italic
     *         (and possibly something else), false otherwise.
     */
    public static boolean isItalic(int style) {
        return (style & ITALIC) != 0;
    }
    
    /*
     * Construction.
     */
    
    /**
     * @return The style corresponding to the specified booleans.
     */
    public static int of(boolean bold, boolean italic) {
        return ofBold(bold) | ofItalic(italic);
    }
    
    /**
     * @return BOLD style if bold is true, else normal style.
     */
    public static int ofBold(boolean bold) {
        return (bold ? BOLD : NORMAL);
    }

    /**
     * @return ITALIC style if italic is true, else normal style.
     */
    public static int ofItalic(boolean italic) {
        return (italic ? ITALIC : NORMAL);
    }
    
    /*
     * Derivation.
     */
    
    /**
     * @param style A style.
     * @return The specified style, with or without bold style
     *         depending on the specified boolean.
     */
    public static int withBold(int style, boolean bold) {
        return (bold ? style | BOLD : style & ~BOLD);
    }
    
    /**
     * @param style A style.
     * @return The specified style, with or without italic style
     *         depending on the specified boolean.
     */
    public static int withItalic(int style, boolean italic) {
        return (italic ? style | ITALIC : style & ~ITALIC);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdFontStyles() {
    }
    
    private static String toHexString(int value) {
        final long valueLong = ((long) value) & 0xFFFFFFFFL;
        final int radix = 16;
        return "0x" + NbrsUtils.toString(valueLong, radix);
    }
}
