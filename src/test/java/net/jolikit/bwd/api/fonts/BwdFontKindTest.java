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
package net.jolikit.bwd.api.fonts;

import java.util.Arrays;
import java.util.List;

import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.BwdFontStyles;
import junit.framework.TestCase;

public class BwdFontKindTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final String FONT_FAMILY = "family";
    private static final int FONT_STYLE = 0x12345679;
    
    private static final int[] FONT_STYLE_ARR = new int[]{
        0,
        BwdFontStyles.BOLD,
        BwdFontStyles.ITALIC,
        BwdFontStyles.BOLD | BwdFontStyles.ITALIC,
        //
        0x12345678,
        0x12345678 | BwdFontStyles.BOLD,
        0x12345678 | BwdFontStyles.ITALIC,
        0x12345678 | BwdFontStyles.BOLD | BwdFontStyles.ITALIC
    };

    private static final boolean[] BOOL_ARR = new boolean[]{false, true};

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_BwdFontKind_String() {
        try {
            new BwdFontKind(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY);
            assertSame(FONT_FAMILY, fontKind.fontFamily());
            assertEquals(BwdFontStyles.NORMAL, fontKind.fontStyle());
        }
    }
    
    public void test_BwdFontKind_String_int() {
        for (int fontStyle : FONT_STYLE_ARR) {
            try {
                new BwdFontKind(null, fontStyle);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        
        for (int fontStyle : FONT_STYLE_ARR) {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, fontStyle);
            assertSame(FONT_FAMILY, fontKind.fontFamily());
            assertEquals(fontStyle, fontKind.fontStyle());
        }
    }
    
    public void test_BwdFontKind_String_2boolean() {
        for (boolean bold : BOOL_ARR) {
            for (boolean italic : BOOL_ARR) {
                try {
                    new BwdFontKind(null, bold, italic);
                    fail();
                } catch (NullPointerException e) {
                    // ok
                }
            }
        }
        
        for (boolean bold : BOOL_ARR) {
            for (boolean italic : BOOL_ARR) {
                final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, bold, italic);
                assertSame(FONT_FAMILY, fontKind.fontFamily());
                assertEquals(bold, fontKind.isBold());
                assertEquals(italic, fontKind.isItalic());
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_withStyle_int() {
        final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, 5);

        final BwdFontKind fontKind2 = fontKind.withStyle(FONT_STYLE);

        assertEquals(fontKind.fontFamily(), fontKind2.fontFamily());
        assertEquals(FONT_STYLE, fontKind2.fontStyle());
    }
    
    public void test_withBold_boolean() {
        for (int style : FONT_STYLE_ARR) {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, style);

            for (boolean bold : BOOL_ARR) {
                final BwdFontKind fontKind2 = fontKind.withBold(bold);

                assertEquals(fontKind.fontFamily(), fontKind2.fontFamily());
                final int expectedStyle = BwdFontStyles.withBold(fontKind.fontStyle(), bold);
                assertEquals(expectedStyle, fontKind2.fontStyle());
            }
        }
    }
    
    public void test_withItalic_boolean() {
        for (int style : FONT_STYLE_ARR) {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, style);

            for (boolean italic : BOOL_ARR) {
                final BwdFontKind fontKind2 = fontKind.withItalic(italic);

                assertEquals(fontKind.fontFamily(), fontKind2.fontFamily());
                final int expectedStyle = BwdFontStyles.withItalic(fontKind.fontStyle(), italic);
                assertEquals(expectedStyle, fontKind2.fontStyle());
            }
        }
    }

    /*
     * 
     */
    
    public void test_toString() {
        for (int style : FONT_STYLE_ARR) {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, style);
            assertEquals(
                    "[" + FONT_FAMILY + ", " + BwdFontStyles.toString(style) + "]",
                    fontKind.toString());
        }
    }
    
    public void test_hashCode() {
        final BwdFontKind kind0 = new BwdFontKind("m", 5);
        final BwdFontKind kind1 = new BwdFontKind("n", 5);
        final BwdFontKind kind3 = new BwdFontKind("m", 5+1);
        
        final List<BwdFontKind> list = Arrays.asList(kind0, kind1, kind3);
        
        for (BwdFontKind a : list) {
            for (BwdFontKind b : list) {
                if (a.equals(b)) {
                    continue;
                }
                assertTrue(a.hashCode() != b.hashCode());
            }
        }
    }
    
    public void test_equals_Object() {
        {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, 5);
            
            assertFalse(fontKind.equals(null));
            assertFalse(fontKind.equals(new Object()));
            
            assertTrue(fontKind.equals(fontKind));
            
            assertTrue(fontKind.equals(new BwdFontKind(fontKind.fontFamily(), fontKind.fontStyle())));
        }
        
        assertFalse(new BwdFontKind("m", 5).equals(new BwdFontKind("n", 5)));
        assertFalse(new BwdFontKind("m", 5).equals(new BwdFontKind("m", 5+1)));
    }
    
    public void test_compareTo_BwdFontId() {
        {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, 5);
            
            try {
                fontKind.compareTo(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        
        assertTrue(new BwdFontKind("m", 5).compareTo(new BwdFontKind("m", 5)) == 0);
        
        assertTrue(new BwdFontKind("m", 5).compareTo(new BwdFontKind("n", 5)) < 0);
        assertTrue(new BwdFontKind("m", 5).compareTo(new BwdFontKind("m", 5+1)) < 0);
        
        assertTrue(new BwdFontKind("m", 5).compareTo(new BwdFontKind("l", 5)) > 0);
        assertTrue(new BwdFontKind("m", 5).compareTo(new BwdFontKind("m", 5-1)) > 0);
        
        // family checked before style
        assertTrue(new BwdFontKind("m", 5).compareTo(new BwdFontKind("n", 5-1)) < 0);
    }
    
    /*
     * 
     */
    
    public void test_fontFamily() {
        final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, FONT_STYLE);
        // Checking String identity: might (annoyingly) work "by chance"
        assertSame(FONT_FAMILY, fontKind.fontFamily());
    }
    
    public void test_fontStyle() {
        for (int style : FONT_STYLE_ARR) {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, style);
            assertEquals(style, fontKind.fontStyle());
        }
    }
    
    /*
     * 
     */
    
    public void test_isBold() {
        for (int style : FONT_STYLE_ARR) {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, style);
            assertEquals(BwdFontStyles.isBold(style), fontKind.isBold());
        }
    }
    
    public void test_isItalic() {
        for (int style : FONT_STYLE_ARR) {
            final BwdFontKind fontKind = new BwdFontKind(FONT_FAMILY, style);
            assertEquals(BwdFontStyles.isItalic(style), fontKind.isItalic());
        }
    }
}
