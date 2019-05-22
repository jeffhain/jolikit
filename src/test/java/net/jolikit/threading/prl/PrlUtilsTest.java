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
package net.jolikit.threading.prl;

import junit.framework.TestCase;

public class PrlUtilsTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_computeMaxDepth_2int() {
        
        for (int badParallelism : new int[]{Integer.MIN_VALUE, 0}) {
            try {
                PrlUtils.computeMaxDepth(badParallelism, 0);
                fail();
            } catch (IllegalArgumentException e) {
            }
        }
        
        for (int badMaxDepth : new int[]{Integer.MIN_VALUE, -1}) {
            try {
                PrlUtils.computeMaxDepth(1, badMaxDepth);
                fail();
            } catch (IllegalArgumentException e) {
            }
        }
        
        /*
         * 
         */

        assertEquals(0, PrlUtils.computeMaxDepth(1, 0));
        //
        assertEquals(1, PrlUtils.computeMaxDepth(2, 0));
        //
        assertEquals(2, PrlUtils.computeMaxDepth(3, 0));
        assertEquals(2, PrlUtils.computeMaxDepth(4, 0));
        //
        assertEquals(3, PrlUtils.computeMaxDepth(5, 0));
        assertEquals(3, PrlUtils.computeMaxDepth(8, 0));
        //
        assertEquals(4, PrlUtils.computeMaxDepth(9, 0));
        
        /*
         * 
         */

        assertEquals(2, PrlUtils.computeMaxDepth(2, 1));
        assertEquals(3, PrlUtils.computeMaxDepth(2, 2));
        assertEquals(4, PrlUtils.computeMaxDepth(2, 3));
        assertEquals(4, PrlUtils.computeMaxDepth(2, 4));
        assertEquals(5, PrlUtils.computeMaxDepth(2, 5));
        
        /*
         * 
         */
        
        assertEquals(3, PrlUtils.computeMaxDepth(2, 2));
        assertEquals(4, PrlUtils.computeMaxDepth(3, 2));
        assertEquals(5, PrlUtils.computeMaxDepth(4, 2));
        assertEquals(6, PrlUtils.computeMaxDepth(5, 2));
        assertEquals(7, PrlUtils.computeMaxDepth(6, 2));
        
        /*
         * 
         */

        assertEquals(0, PrlUtils.computeMaxDepth(1, Integer.MAX_VALUE));
        assertEquals(31, PrlUtils.computeMaxDepth(Integer.MAX_VALUE, 0));
        assertEquals(Integer.MAX_VALUE, PrlUtils.computeMaxDepth(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
