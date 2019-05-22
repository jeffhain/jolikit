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

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.BwdFontStyles;
import junit.framework.TestCase;

public class BwdFontIdTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final String FONT_FAMILY = "family";
    private static final BwdFontKind FONT_KIND = new BwdFontKind(FONT_FAMILY);
    
    private static final int[] GOOD_FONT_SIZE_ARR = new int[]{
        1, Integer.MAX_VALUE};
    private static final int[] BAD_FONT_SIZE_ARR = new int[]{
        Integer.MIN_VALUE, -1, 0};

    private static final int[] FONT_STYLE_ARR = new int[]{
        Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_BwdFontId_BwdFontKind_int() {
        for (int fontSize : GOOD_FONT_SIZE_ARR) {
            try {
                new BwdFontId(null, fontSize);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        for (int fontSize : BAD_FONT_SIZE_ARR) {
            try {
                new BwdFontId(FONT_KIND, fontSize);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        for (int fontSize : GOOD_FONT_SIZE_ARR) {
            final BwdFontId fontId = new BwdFontId(FONT_KIND, fontSize);
            assertSame(FONT_KIND, fontId.fontKind());
            assertEquals(fontSize, fontId.fontSize());
        }
    }
    
    public void test_BwdFontId_String_2int() {
        for (int fontSize : GOOD_FONT_SIZE_ARR) {
            for (int fontStyle : FONT_STYLE_ARR) {
                try {
                    new BwdFontId(null, fontStyle, fontSize);
                    fail();
                } catch (NullPointerException e) {
                    // ok
                }
            }
        }
        for (int fontSize : BAD_FONT_SIZE_ARR) {
            for (int fontStyle : FONT_STYLE_ARR) {
                try {
                    new BwdFontId(FONT_FAMILY, fontStyle, fontSize);
                    fail();
                } catch (IllegalArgumentException e) {
                    // ok
                }
            }
        }
        
        for (int fontStyle : FONT_STYLE_ARR) {
            for (int fontSize : GOOD_FONT_SIZE_ARR) {
                final BwdFontId fontId = new BwdFontId(FONT_FAMILY, fontStyle, fontSize);
                assertSame(FONT_FAMILY, fontId.fontKind().fontFamily());
                assertEquals(fontStyle, fontId.fontKind().fontStyle());
                assertEquals(fontSize, fontId.fontSize());
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_toString() {
        final BwdFontId fontId = new BwdFontId(FONT_KIND, 7);
        assertEquals(
                "[" + FONT_KIND.fontFamily() + ", " + BwdFontStyles.toString(FONT_KIND.fontStyle()) + ", 7]",
                fontId.toString());
    }
    
    public void test_hashCode() {
        final BwdFontId id0 = new BwdFontId("m", 5, 7);
        final BwdFontId id1 = new BwdFontId("n", 5, 7);
        final BwdFontId id2 = new BwdFontId("m", 5+1, 7);
        final BwdFontId id3 = new BwdFontId("m", 5, 7+1);
        
        final List<BwdFontId> list = Arrays.asList(id0, id1, id2, id3);
        
        for (BwdFontId a : list) {
            for (BwdFontId b : list) {
                if (a.equals(b)) {
                    continue;
                }
                assertTrue(a.hashCode() != b.hashCode());
            }
        }
    }
    
    public void test_equals_Object() {
        {
            final BwdFontId fontId = new BwdFontId(FONT_FAMILY, 5, 7);
            
            assertFalse(fontId.equals(null));
            assertFalse(fontId.equals(new Object()));
            
            assertTrue(fontId.equals(fontId));
            
            assertTrue(fontId.equals(new BwdFontId(fontId.fontKind(), fontId.fontSize())));
        }
        
        assertFalse(new BwdFontId("m", 5, 7).equals(new BwdFontId("n", 5, 7)));
        assertFalse(new BwdFontId("m", 5, 7).equals(new BwdFontId("m", 5+1, 7)));
        assertFalse(new BwdFontId("m", 5, 7).equals(new BwdFontId("m", 5, 7+1)));
    }
    
    public void test_compareTo_BwdFontId() {
        {
            final BwdFontId id = new BwdFontId(FONT_FAMILY, 5, 7);
            
            try {
                id.compareTo(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("m", 5, 7)) == 0);
        
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("n", 5, 7)) < 0);
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("m", 5+1, 7)) < 0);
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("m", 5, 7+1)) < 0);
        
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("l", 5, 7)) > 0);
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("m", 5-1, 7)) > 0);
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("m", 5, 7-1)) > 0);
        
        // family checked before style
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("n", 5-1, 7)) < 0);
        // style checked before size
        assertTrue(new BwdFontId("m", 5, 7).compareTo(new BwdFontId("m", 5+1, 7-1)) < 0);
    }
    
    /*
     * 
     */
    
    public void test_fontKind() {
        final BwdFontId fontId = new BwdFontId(FONT_KIND, 7);
        
        assertSame(FONT_KIND, fontId.fontKind());
    }
    
    public void test_fontSize() {
        final BwdFontId fontId = new BwdFontId(FONT_KIND, 7);
        
        assertSame(7, fontId.fontSize());
    }
}
