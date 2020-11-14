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

import java.util.List;

import junit.framework.TestCase;

public class BwdFontStylesTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_pureStyleList() {
        final List<Integer> list = BwdFontStyles.pureStyleList();
        
        try {
            list.add(0);
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertSame(list, BwdFontStyles.pureStyleList());
        
        assertEquals(3, list.size());
        assertEquals(BwdFontStyles.NORMAL, (int) list.get(0));
        assertEquals(BwdFontStyles.BOLD, (int) list.get(1));
        assertEquals(BwdFontStyles.ITALIC, (int) list.get(2));
    }
    
    public void test_minAdditionalStyleBit() {
        assertEquals((1 << 4), BwdFontStyles.minAdditionalStyleBit());
    }
    
    /*
     * 
     */
    
    public void test_toString_int() {
        assertEquals("normal", BwdFontStyles.toString(BwdFontStyles.NORMAL));
        
        assertEquals("bold", BwdFontStyles.toString(BwdFontStyles.BOLD));
        assertEquals("italic", BwdFontStyles.toString(BwdFontStyles.ITALIC));
        assertEquals("bold-italic", BwdFontStyles.toString(BwdFontStyles.BOLD | BwdFontStyles.ITALIC));
        
        final int minAddBit = BwdFontStyles.minAdditionalStyleBit();
        // "unknown" even if in reserved range.
        assertEquals("unknown_style(0x8)", BwdFontStyles.toString(minAddBit >> 1));
        assertEquals("unknown_style(0x10)", BwdFontStyles.toString(minAddBit));
        assertEquals("unknown_style(0x8765432C)", BwdFontStyles.toString(0x8765432C));
        assertEquals("unknown_style(0xFFFFFFFC)", BwdFontStyles.toString(0xFFFFFFFC));
        
        assertEquals("bold-unknown_style(0x4)", BwdFontStyles.toString(BwdFontStyles.BOLD | (1 << 2)));
        assertEquals("bold-italic-unknown_style(0x4)", BwdFontStyles.toString(BwdFontStyles.BOLD | BwdFontStyles.ITALIC | (1 << 2)));
    }
    
    /*
     * 
     */
    
    public void test_isBold_int() {
        assertFalse(BwdFontStyles.isBold(0));
        assertFalse(BwdFontStyles.isBold(BwdFontStyles.ITALIC));
        assertFalse(BwdFontStyles.isBold(~BwdFontStyles.BOLD));
        
        assertTrue(BwdFontStyles.isBold(BwdFontStyles.BOLD));
        assertTrue(BwdFontStyles.isBold(BwdFontStyles.BOLD | BwdFontStyles.ITALIC));
    }
    
    public void test_isItalic_int() {
        assertFalse(BwdFontStyles.isItalic(0));
        assertFalse(BwdFontStyles.isItalic(BwdFontStyles.BOLD));
        assertFalse(BwdFontStyles.isItalic(~BwdFontStyles.ITALIC));
        
        assertTrue(BwdFontStyles.isItalic(BwdFontStyles.ITALIC));
        assertTrue(BwdFontStyles.isItalic(BwdFontStyles.ITALIC | BwdFontStyles.BOLD));
    }
    
    /*
     * 
     */
    
    public void test_of_2boolean() {
        assertEquals(BwdFontStyles.NORMAL, BwdFontStyles.of(false, false));
        assertEquals(BwdFontStyles.BOLD, BwdFontStyles.of(true, false));
        assertEquals(BwdFontStyles.ITALIC, BwdFontStyles.of(false, true));
        assertEquals(BwdFontStyles.BOLD | BwdFontStyles.ITALIC, BwdFontStyles.of(true, true));
    }
    
    public void test_ofBold_boolean() {
        assertEquals(BwdFontStyles.NORMAL, BwdFontStyles.ofBold(false));
        assertEquals(BwdFontStyles.BOLD, BwdFontStyles.ofBold(true));
    }
    
    public void test_ofItalic_boolean() {
        assertEquals(BwdFontStyles.NORMAL, BwdFontStyles.ofItalic(false));
        assertEquals(BwdFontStyles.ITALIC, BwdFontStyles.ofItalic(true));
    }
    
    /*
     * 
     */
    
    public void test_withBold_int_boolean() {
        assertEquals(BwdFontStyles.NORMAL, BwdFontStyles.withBold(BwdFontStyles.NORMAL, false));
        assertEquals(BwdFontStyles.BOLD, BwdFontStyles.withBold(BwdFontStyles.NORMAL, true));

        assertEquals(BwdFontStyles.NORMAL, BwdFontStyles.withBold(BwdFontStyles.BOLD, false));
        assertEquals(BwdFontStyles.BOLD, BwdFontStyles.withBold(BwdFontStyles.BOLD, true));
        
        assertEquals(BwdFontStyles.ITALIC, BwdFontStyles.withBold(BwdFontStyles.ITALIC, false));
        assertEquals(BwdFontStyles.BOLD | BwdFontStyles.ITALIC, BwdFontStyles.withBold(BwdFontStyles.ITALIC, true));

        assertEquals(~BwdFontStyles.BOLD, BwdFontStyles.withBold(~BwdFontStyles.BOLD, false));
        assertEquals(0xFFFFFFFF, BwdFontStyles.withBold(~BwdFontStyles.BOLD, true));
    }
    
    public void test_withItalic_int_boolean() {
        assertEquals(BwdFontStyles.NORMAL, BwdFontStyles.withItalic(BwdFontStyles.NORMAL, false));
        assertEquals(BwdFontStyles.ITALIC, BwdFontStyles.withItalic(BwdFontStyles.NORMAL, true));

        assertEquals(BwdFontStyles.NORMAL, BwdFontStyles.withItalic(BwdFontStyles.ITALIC, false));
        assertEquals(BwdFontStyles.ITALIC, BwdFontStyles.withItalic(BwdFontStyles.ITALIC, true));
        
        assertEquals(BwdFontStyles.BOLD, BwdFontStyles.withItalic(BwdFontStyles.BOLD, false));
        assertEquals(BwdFontStyles.ITALIC | BwdFontStyles.BOLD, BwdFontStyles.withItalic(BwdFontStyles.BOLD, true));

        assertEquals(~BwdFontStyles.ITALIC, BwdFontStyles.withItalic(~BwdFontStyles.ITALIC, false));
        assertEquals(0xFFFFFFFF, BwdFontStyles.withItalic(~BwdFontStyles.ITALIC, true));
    }
}
