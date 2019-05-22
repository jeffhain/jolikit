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
package net.jolikit.bwd.api.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class GPointTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * General and pathological cases.
     */
    private static final List<GPoint> POINT_LIST;
    static {
        final int[][] coordArrArr = new int[][]{
                {0, 0},
                //
                {-5, -7},
                {5, 7},
                //
                {Integer.MIN_VALUE, Integer.MIN_VALUE},
                {Integer.MIN_VALUE, Integer.MAX_VALUE},
                {Integer.MAX_VALUE, Integer.MIN_VALUE},
                {Integer.MAX_VALUE, Integer.MAX_VALUE},
        };
        
        final List<GPoint> list = new ArrayList<GPoint>();
        for (int[] coordArr : coordArrArr) {
            final int x = coordArr[0];
            final int y = coordArr[1];
            
            final GPoint rect = GPoint.valueOf(x, y);
            
            list.add(rect);
        }
        POINT_LIST = Collections.unmodifiableList(list);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_valueOf_2int() {
        assertSame(GPoint.ZERO, GPoint.valueOf(0, 0));
        assertSame(
                GPoint.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE),
                GPoint.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        final int[] coordArr = new int[]{Integer.MIN_VALUE, -7, -1, 0, 1, 7, Integer.MAX_VALUE};
        for (int x : coordArr) {
            for (int y : coordArr) {
                final GPoint point = GPoint.valueOf(x, y);
                assertEquals(x, point.x());
                assertEquals(y, point.y());
            }
        }
    }
    
    /*
     * 
     */

    public void test_withX_int() {
        assertEquals(GPoint.valueOf(19,  7), GPoint.valueOf(5, 7).withX(19));
    }

    public void test_withY_int() {
        assertEquals(GPoint.valueOf(5, 19), GPoint.valueOf(5, 7).withY(19));
    }
    
    public void test_withDeltas_2int() {
        assertEquals(
                GPoint.valueOf(5 + 11, 7 + 19),
                GPoint.valueOf(5, 7).withDeltas(11, 19));
        assertEquals(
                GPoint.valueOf(5 + Integer.MIN_VALUE, 7 + Integer.MAX_VALUE),
                GPoint.valueOf(5, 7).withDeltas(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
    
    /*
     * 
     */
    
    public void test_toString() {
        assertEquals("[0, 0]", GPoint.ZERO.toString());
        assertEquals("[5, 7]", GPoint.valueOf(5, 7).toString());
    }

    public void test_hashCode() {
        final GPoint p0 = GPoint.valueOf(5, 7);
        final GPoint p1 = GPoint.valueOf(5+1, 7);
        final GPoint p2 = GPoint.valueOf(5, 7+1);
        final List<GPoint> list = Arrays.asList(p0, p1, p2);
        
        for (GPoint a : list) {
            for (GPoint b : list) {
                if (a.equals(b)) {
                    continue;
                }
                assertTrue(a.hashCode() != b.hashCode());
            }
        }
    }

    public void test_equals_Object() {
        for (GPoint point : POINT_LIST) {
            assertFalse(point.equals(null));
            assertFalse(point.equals(new Object()));
            
            assertTrue(point.equals(point));
            
            assertTrue(point.equals(GPoint.valueOf(point.x(), point.y())));
        }
        
        assertFalse(GPoint.valueOf(5, 7).equals(GPoint.valueOf(5+1, 7)));
        assertFalse(GPoint.valueOf(5, 7).equals(GPoint.valueOf(5, 7+1)));
    }
    
    public void test_equalsPoint_2int() {
        assertTrue(GPoint.valueOf(5, 7).equalsPoint(5, 7));
        
        assertFalse(GPoint.valueOf(5, 7).equalsPoint(5+1, 7));
        assertFalse(GPoint.valueOf(5, 7).equalsPoint(5, 7+1));
    }
    
    public void test_compareTo_GPoint() {
        for (GPoint point : POINT_LIST) {
            try {
                point.compareTo(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        
        assertTrue(GPoint.valueOf(5, 7).compareTo(GPoint.valueOf(5, 7)) == 0);
        
        assertTrue(GPoint.valueOf(5, 7).compareTo(GPoint.valueOf(5+1, 7)) < 0);
        assertTrue(GPoint.valueOf(5, 7).compareTo(GPoint.valueOf(5, 7+1)) < 0);
        
        assertTrue(GPoint.valueOf(5, 7).compareTo(GPoint.valueOf(5-1, 7)) > 0);
        assertTrue(GPoint.valueOf(5, 7).compareTo(GPoint.valueOf(5, 7-1)) > 0);
        
        // y checked before x
        assertTrue(GPoint.valueOf(5, 7).compareTo(GPoint.valueOf(5-1, 7+1)) < 0);
    }
    
    /*
     * 
     */

    public void test_x() {
        assertEquals(5, GPoint.valueOf(5, 7).x());
    }

    public void test_y() {
        assertEquals(7, GPoint.valueOf(5, 7).y());
    }
}
