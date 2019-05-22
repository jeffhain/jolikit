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
package net.jolikit.bwd.api;

import java.util.List;

import junit.framework.TestCase;

public class BwdCursorsTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_cursorList() {
        final List<Integer> list = BwdCursors.cursorList();
        
        try {
            list.add(0);
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertSame(list, BwdCursors.cursorList());
        
        assertEquals(11, list.size());
        assertEquals(BwdCursors.INVISIBLE, (int) list.get(0));
        assertEquals(BwdCursors.ARROW, (int) list.get(1));
        //
        assertEquals(BwdCursors.MOVE, (int) list.get(list.size() - 1));
    }
    
    public void test_minAdditionalCursor() {
        assertEquals(32, BwdCursors.minAdditionalCursor());
    }
    
    /*
     * 
     */
    
    public void test_toString_int() {
        assertEquals("NO_STATEMENT", BwdCursors.toString(BwdCursors.NO_STATEMENT));
        //
        assertEquals("INVISIBLE", BwdCursors.toString(BwdCursors.INVISIBLE));
        assertEquals("ARROW", BwdCursors.toString(BwdCursors.ARROW));
        //
        assertEquals("MOVE", BwdCursors.toString(BwdCursors.MOVE));
        
        final int minAdd = BwdCursors.minAdditionalCursor();
        // "unknown" even if in reserved range.
        assertEquals("UNKNOWN_CURSOR(" + (minAdd - 1) + ")", BwdCursors.toString(minAdd - 1));
        assertEquals("UNKNOWN_CURSOR(" + minAdd + ")", BwdCursors.toString(minAdd));
    }
}
