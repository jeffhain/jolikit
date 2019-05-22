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

public class BwdKeyLocationsTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_locationList() {
        final List<Integer> list = BwdKeyLocations.locationList();
        
        try {
            list.add(0);
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertSame(list, BwdKeyLocations.locationList());
        
        assertEquals(3, list.size());
        assertEquals(BwdKeyLocations.LEFT, (int) list.get(0));
        assertEquals(BwdKeyLocations.RIGHT, (int) list.get(1));
        assertEquals(BwdKeyLocations.NUMPAD, (int) list.get(2));
    }
    
    public void test_minAdditionalLocation() {
        assertEquals(32, BwdKeyLocations.minAdditionalLocation());
    }
    
    /*
     * 
     */
    
    public void test_toString_int() {
        assertEquals("NO_STATEMENT", BwdKeyLocations.toString(BwdKeyLocations.NO_STATEMENT));
        //
        assertEquals("LEFT", BwdKeyLocations.toString(BwdKeyLocations.LEFT));
        assertEquals("RIGHT", BwdKeyLocations.toString(BwdKeyLocations.RIGHT));
        assertEquals("NUMPAD", BwdKeyLocations.toString(BwdKeyLocations.NUMPAD));
        
        final int minAdd = BwdKeyLocations.minAdditionalLocation();
        // "unknown" even if in reserved range.
        assertEquals("UNKNOWN_LOCATION(" + (minAdd - 1) + ")", BwdKeyLocations.toString(minAdd - 1));
        assertEquals("UNKNOWN_LOCATION(" + minAdd + ")", BwdKeyLocations.toString(minAdd));
    }
}
