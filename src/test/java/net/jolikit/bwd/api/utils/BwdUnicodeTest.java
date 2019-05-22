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
package net.jolikit.bwd.api.utils;

import junit.framework.TestCase;

public class BwdUnicodeTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_toDisplayString_int() {
        assertEquals("0x0", BwdUnicode.toDisplayString(0));
        assertEquals("0x1", BwdUnicode.toDisplayString(1));
        assertEquals("0x357BD", BwdUnicode.toDisplayString(0x357BD));
        assertEquals("0xFFFF", BwdUnicode.toDisplayString(0xFFFF));
        assertEquals("0xFFFFFFFF", BwdUnicode.toDisplayString(0xFFFFFFFF));
    }
    
    public void test_checkCodePoint_int() {
        for (int cp : new int[]{Integer.MIN_VALUE, -1, BwdUnicode.MAX_10FFFF + 1, Integer.MAX_VALUE}) {
            try {
                BwdUnicode.checkCodePoint(cp);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        for (int cp : new int[]{0, 0x357B, BwdUnicode.MAX_FFFF, BwdUnicode.MAX_10FFFF}) {
            BwdUnicode.checkCodePoint(cp);
        }
    }
    
    public void test_isInBmp_int() {
        for (int cp : new int[]{Integer.MIN_VALUE, -1, 0xFFFF + 1, Integer.MAX_VALUE}) {
            assertFalse(BwdUnicode.isInBmp(cp));
        }
        
        for (int cp : new int[]{0, 0x357B, 0xFFFF}) {
            assertTrue(BwdUnicode.isInBmp(cp));
        }
    }
    
    public void test_isHorizontalSpace_int() {
        for (int cp = -1; cp <= BwdUnicode.MAX_10FFFF + 1; cp++) {
            final boolean expected =
                    (cp == BwdUnicode.HT)
                    || (cp == BwdUnicode.SPACE)
                    || (cp == BwdUnicode.NBSP)
                    || (cp == BwdUnicode.MMSP)
                    || (cp == BwdUnicode.NNBSP)
                    || (cp == BwdUnicode.HWHF)
                    || (cp == BwdUnicode.IDSP)
                    //
                    || ((cp >= BwdUnicode.EN_QUAD) && (cp <= BwdUnicode.HAIR_SPACE));
            final boolean actual = BwdUnicode.isHorizontalSpace(cp);
            assertEquals(expected, actual);
        }
    }
    
    public void test_isNewlineOrVerticalSpace_int() {
        for (int cp = -1; cp <= BwdUnicode.MAX_10FFFF + 1; cp++) {
            final boolean expected =
                    (cp == BwdUnicode.LF)
                    || (cp == BwdUnicode.VT)
                    || (cp == BwdUnicode.FF)
                    || (cp == BwdUnicode.CR)
                    || (cp == BwdUnicode.NEL)
                    || (cp == BwdUnicode.LSEP)
                    || (cp == BwdUnicode.PSEP);
            final boolean actual = BwdUnicode.isNewlineOrVerticalSpace(cp);
            assertEquals(expected, actual);
        }
    }
}
