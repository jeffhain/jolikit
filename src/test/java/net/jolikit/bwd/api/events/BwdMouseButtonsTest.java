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

import junit.framework.TestCase;

public class BwdMouseButtonsTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_buttonList() {
        final List<Integer> list = BwdMouseButtons.buttonList();
        
        try {
            list.add(0);
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertSame(list, BwdMouseButtons.buttonList());
        
        assertEquals(3, list.size());
        assertEquals(BwdMouseButtons.PRIMARY, (int) list.get(0));
        assertEquals(BwdMouseButtons.MIDDLE, (int) list.get(1));
        assertEquals(BwdMouseButtons.SECONDARY, (int) list.get(2));
    }
    
    public void test_minAdditionalButton() {
        assertEquals(32, BwdMouseButtons.minAdditionalButton());
    }
    
    /*
     * 
     */
    
    public void test_toString_int() {
        assertEquals("NO_STATEMENT", BwdMouseButtons.toString(BwdMouseButtons.NO_STATEMENT));
        //
        assertEquals("PRIMARY", BwdMouseButtons.toString(BwdMouseButtons.PRIMARY));
        assertEquals("MIDDLE", BwdMouseButtons.toString(BwdMouseButtons.MIDDLE));
        assertEquals("SECONDARY", BwdMouseButtons.toString(BwdMouseButtons.SECONDARY));
        
        final int minAdd = BwdMouseButtons.minAdditionalButton();
        // "unknown" even if in reserved range.
        assertEquals("UNKNOWN_BUTTON(" + (minAdd - 1) + ")", BwdMouseButtons.toString(minAdd - 1));
        assertEquals("UNKNOWN_BUTTON(" + minAdd + ")", BwdMouseButtons.toString(minAdd));
    }
}
