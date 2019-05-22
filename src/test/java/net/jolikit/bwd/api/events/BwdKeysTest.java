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
package net.jolikit.bwd.api.events;

import java.util.List;

import net.jolikit.bwd.api.utils.BwdUnicode;
import junit.framework.TestCase;

public class BwdKeysTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MIN_KEYISH = BwdKeys.MIN_KEY - 10;
    private static final int MAX_KEYISH = BwdKeys.MAX_KEY + 10;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_keyList() {
        final List<Integer> list = BwdKeys.keyList();
        
        try {
            list.add(0);
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertSame(list, BwdKeys.keyList());
        
        assertEquals(109, list.size());
        assertEquals(BwdKeys.NUM_LOCK, (int) list.get(0));
        assertEquals(BwdKeys.CAPS_LOCK, (int) list.get(1));
        //
        assertEquals(BwdKeys.TILDE, (int) list.get(list.size() - 1));
    }
    
    public void test_minAdditionalKey() {
        assertEquals(1024, BwdKeys.minAdditionalKey());
    }
    
    /*
     * 
     */
    
    public void test_toString_int() {
        assertEquals("NO_STATEMENT", BwdKeys.toString(BwdKeys.NO_STATEMENT));
        //
        assertEquals("NUM_LOCK", BwdKeys.toString(BwdKeys.NUM_LOCK));
        assertEquals("CAPS_LOCK", BwdKeys.toString(BwdKeys.CAPS_LOCK));
        //
        assertEquals("TILDE", BwdKeys.toString(BwdKeys.TILDE));
        
        final int minAdd = BwdKeys.minAdditionalKey();
        // "unknown" even if in reserved range.
        assertEquals("UNKNOWN_KEY(" + (minAdd - 1) + ")", BwdKeys.toString(minAdd - 1));
        assertEquals("UNKNOWN_KEY(" + minAdd + ")", BwdKeys.toString(minAdd));
    }
    
    /*
     * 
     */
    
    public void test_keyForCodePoint_int() {
        for (int badCp : new int[]{Integer.MIN_VALUE, -1, BwdUnicode.MAX_10FFFF + 1, Integer.MAX_VALUE}) {
            assertEquals(BwdKeys.NO_STATEMENT, BwdKeys.keyForCodePoint(badCp));
        }
        
        assertEquals(BwdKeys.A, BwdKeys.keyForCodePoint(BwdUnicode.a));
        
        // Testing one part of bijectivity.
        for (int cp = 0; cp <= 127; cp++) {
            final int key = BwdKeys.keyForCodePoint(cp);
            if (key != BwdKeys.NO_STATEMENT) {
                assertEquals(cp, BwdKeys.codePointForKey(key));
            }
        }
    }
    
    public void test_codePointForKey_int() {
        assertEquals(-1, BwdKeys.codePointForKey(BwdKeys.NO_STATEMENT));
        assertEquals(-1, BwdKeys.codePointForKey(BwdKeys.TILDE + 1));
        
        assertEquals(BwdUnicode.a, BwdKeys.codePointForKey(BwdKeys.A));
        
        // Testing one part of bijectivity.
        for (int key = MIN_KEYISH; key <= MAX_KEYISH; key++) {
            final int cp = BwdKeys.codePointForKey(key);
            if (cp != -1) {
                assertEquals(key, BwdKeys.keyForCodePoint(cp));
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_isFunctionKey_int() {
        for (int key = MIN_KEYISH; key <= MAX_KEYISH; key++) {
            final boolean expected = (key >= BwdKeys.F1) && (key <= BwdKeys.F12);   
            assertEquals(expected, BwdKeys.isFunctionKey(key));
        }
    }

    public void test_isNavigationKey_int() {
        for (int key = MIN_KEYISH; key <= MAX_KEYISH; key++) {
            final boolean expected = (key >= BwdKeys.HOME) && (key <= BwdKeys.DOWN);   
            assertEquals(expected, BwdKeys.isNavigationKey(key));
        }
    }

    public void test_isArrowKey_int() {
        for (int key = MIN_KEYISH; key <= MAX_KEYISH; key++) {
            final boolean expected = (key >= BwdKeys.LEFT) && (key <= BwdKeys.DOWN);   
            assertEquals(expected, BwdKeys.isArrowKey(key));
        }
    }

    public void test_isModifierKey_int() {
        for (int key = MIN_KEYISH; key <= MAX_KEYISH; key++) {
            final boolean expected = (key >= BwdKeys.SHIFT) && (key <= BwdKeys.HYPER);   
            assertEquals(expected, BwdKeys.isModifierKey(key));
        }
    }

    public void test_isLetterKey_int() {
        for (int key = MIN_KEYISH; key <= MAX_KEYISH; key++) {
            final boolean expected = (key >= BwdKeys.A) && (key <= BwdKeys.Z);   
            assertEquals(expected, BwdKeys.isLetterKey(key));
        }
    }

    public void test_isDigitKey_int() {
        for (int key = MIN_KEYISH; key <= MAX_KEYISH; key++) {
            final boolean expected = (key >= BwdKeys.DIGIT_0) && (key <= BwdKeys.DIGIT_9);   
            assertEquals(expected, BwdKeys.isDigitKey(key));
        }
    }
}
