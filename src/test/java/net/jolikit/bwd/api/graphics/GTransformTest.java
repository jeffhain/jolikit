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

public class GTransformTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final int[] BAD_DEG_ARR = new int[]{
        Integer.MIN_VALUE,
        -1, 1,
        360,
        Integer.MAX_VALUE};

    private static final int[] DXY_ARR = new int[]{
        Integer.MIN_VALUE,
        -17, -1, 0, 1, 17,
        Integer.MAX_VALUE};
    
    /**
     * General and pathological cases.
     */
    private static final List<GTransform> TRANSFORM_LIST;
    static {
        final int[][] paramArrArr = new int[][]{
                {0, 0, 0},
                {90, 0, 0},
                {180, 0, 0},
                {270, 0, 0},
                //
                {0, 5, 7},
                {90, 5, 7},
                {180, 5, 7},
                {270, 5, 7},
                //
                {0, Integer.MIN_VALUE, Integer.MIN_VALUE},
                {90, Integer.MIN_VALUE, Integer.MIN_VALUE},
                {180, Integer.MIN_VALUE, Integer.MIN_VALUE},
                {270, Integer.MIN_VALUE, Integer.MIN_VALUE},
                //
                {0, Integer.MIN_VALUE, Integer.MAX_VALUE},
                {90, Integer.MIN_VALUE, Integer.MAX_VALUE},
                {180, Integer.MIN_VALUE, Integer.MAX_VALUE},
                {270, Integer.MIN_VALUE, Integer.MAX_VALUE},
                //
                {0, Integer.MAX_VALUE, Integer.MIN_VALUE},
                {90, Integer.MAX_VALUE, Integer.MIN_VALUE},
                {180, Integer.MAX_VALUE, Integer.MIN_VALUE},
                {270, Integer.MAX_VALUE, Integer.MIN_VALUE},
                //
                {0, Integer.MAX_VALUE, Integer.MAX_VALUE},
                {90, Integer.MAX_VALUE, Integer.MAX_VALUE},
                {180, Integer.MAX_VALUE, Integer.MAX_VALUE},
                {270, Integer.MAX_VALUE, Integer.MAX_VALUE},
        };
        
        final List<GTransform> list = new ArrayList<GTransform>();
        for (int[] paramArr : paramArrArr) {
            final int angDeg = paramArr[0];
            final int dx = paramArr[1];
            final int dy = paramArr[2];
            
            final GTransform transform = GTransform.valueOf(angDeg, dx, dy);
            
            list.add(transform);
        }
        TRANSFORM_LIST = Collections.unmodifiableList(list);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_IDENTITY() {
        assertEquals(GRotation.ROT_0, GTransform.IDENTITY.rotation());
        assertEquals(0, GTransform.IDENTITY.frame2XIn1());
        assertEquals(0, GTransform.IDENTITY.frame2YIn1());
    }
    
    /*
     * 
     */
    
    public void test_valueOf_3int() {
        for (int badDeg : BAD_DEG_ARR) {
            try {
                GTransform.valueOf(badDeg, 0, 0);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        assertSame(GTransform.IDENTITY, GTransform.valueOf(0, 0, 0));
        
        for (int angDeg : new int[]{0, 90, 180, 270}) {
            assertSame(GTransform.valueOf(angDeg, 0, 0), GTransform.valueOf(angDeg, 0, 0));
            
            for (int dx : DXY_ARR) {
                for (int dy : DXY_ARR) {
                    final GTransform trans = GTransform.valueOf(angDeg, dx, dy);
                    assertEquals(GRotation.valueOf(angDeg), trans.rotation());
                    assertEquals(dx, trans.frame2XIn1());
                    assertEquals(dy, trans.frame2YIn1());
                }
            }
        }
    }
    
    public void test_valueOf_GRotation_2int() {
        for (int dx : DXY_ARR) {
            for (int dy : DXY_ARR) {
                try {
                    GTransform.valueOf(null, dx, dy);
                    fail();
                } catch (NullPointerException e) {
                    // ok
                }
            }
        }
        
        assertSame(GTransform.IDENTITY, GTransform.valueOf(GRotation.ROT_0, 0, 0));
        
        for (GRotation rot : GRotation.values()) {
            assertSame(GTransform.valueOf(rot, 0, 0), GTransform.valueOf(rot, 0, 0));
            
            for (int dx : DXY_ARR) {
                for (int dy : DXY_ARR) {
                    final GTransform trans = GTransform.valueOf(rot, dx, dy);
                    assertEquals(rot, trans.rotation());
                    assertEquals(dx, trans.frame2XIn1());
                    assertEquals(dy, trans.frame2YIn1());
                }
            }
        }
    }
    
    public void test_valueOf_GRotation_4int() {
        for (int dx : DXY_ARR) {
            for (int dy : DXY_ARR) {
                try {
                    GTransform.valueOf(null, dx, dy, dx, dy);
                    fail();
                } catch (NullPointerException e) {
                    // ok
                }
            }
        }
        
        assertSame(GTransform.IDENTITY, GTransform.valueOf(GRotation.ROT_0, 0, 0, 0, 0));
        
        assertEquals(GTransform.valueOf(GRotation.ROT_0, 5, 7), GTransform.valueOf(GRotation.ROT_0, 5, 7, 11, 17));
        assertEquals(GTransform.valueOf(GRotation.ROT_90, 5+11-1, 7), GTransform.valueOf(GRotation.ROT_90, 5, 7, 11, 17));
        assertEquals(GTransform.valueOf(GRotation.ROT_180, 5+11-1, 7+17-1), GTransform.valueOf(GRotation.ROT_180, 5, 7, 11, 17));
        assertEquals(GTransform.valueOf(GRotation.ROT_270, 5, 7+17-1), GTransform.valueOf(GRotation.ROT_270, 5, 7, 11, 17));
    }
    
    public void test_valueOf_GRotation_4int_GTransform() {
        for (int dx : DXY_ARR) {
            for (int dy : DXY_ARR) {
                try {
                    GTransform.valueOf(null, dx, dy, dx, dy, GTransform.IDENTITY);
                    fail();
                } catch (NullPointerException e) {
                    // ok
                }
            }
        }
        
        final GTransform notExpected = GTransform.valueOf(GRotation.ROT_90, -1, -2);
        
        {
            final GTransform expected = GTransform.IDENTITY;
            assertSame(expected, GTransform.valueOf(GRotation.ROT_0, 0, 0, 0, 0, null));
            assertSame(expected, GTransform.valueOf(GRotation.ROT_0, 0, 0, 0, 0, notExpected));
            assertSame(expected, GTransform.valueOf(GRotation.ROT_0, 0, 0, 0, 0, expected));
        }
        
        {
            final int xSpan = 11;
            final int ySpan = 17;
            for (Object[] rotExpDxExpDy : new Object[][]{
                    {GRotation.ROT_0, 0, 0},
                    {GRotation.ROT_90, xSpan-1, 0},
                    {GRotation.ROT_180, xSpan-1, ySpan-1},
                    {GRotation.ROT_270, 0, ySpan-1},
            }) {
                final GRotation rotation = (GRotation) rotExpDxExpDy[0];
                final int expectedDx = (Integer) rotExpDxExpDy[1];
                final int expectedDy = (Integer) rotExpDxExpDy[2];
                final GTransform expected = GTransform.valueOf(rotation, 5 + expectedDx, 7 + expectedDy);
                assertEquals(expected, GTransform.valueOf(rotation, 5, 7, xSpan, ySpan, null));
                assertEquals(expected, GTransform.valueOf(rotation, 5, 7, xSpan, ySpan, notExpected));
                assertSame(expected, GTransform.valueOf(rotation, 5, 7, xSpan, ySpan, expected));
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_inversed() {
        assertSame(GTransform.IDENTITY, GTransform.IDENTITY.inverted());
        
        for (GTransform transform : TRANSFORM_LIST) {
            assertSame(transform.inverted(), transform.inverted());
        }

        // The result seem to work even in case of integer overflow,
        // not just with reasonable transforms and coordinates.
        for (GTransform transform : TRANSFORM_LIST) {
            final int xIn1 = 11;
            final int yIn1 = 17;
            
            final int xIn2 = transform.xIn2(xIn1, yIn1);
            final int yIn2 = transform.yIn2(xIn1, yIn1);
            
            final int xIn1_bis = transform.inverted().xIn2(xIn2, yIn2);
            final int yIn1_bis = transform.inverted().yIn2(xIn2, yIn2);
            
            assertEquals(xIn1, xIn1_bis);
            assertEquals(yIn1, yIn1_bis);
        }
    }
    
    public void test_composed_GTransform() {
        for (GTransform transform : TRANSFORM_LIST) {
            try {
                transform.composed(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        
        // The result seems to work even in case of integer overflow,
        // not just with reasonable transforms and coordinates.
        for (GTransform t12 : TRANSFORM_LIST) {
            for (GTransform t23 : TRANSFORM_LIST) {
                final int xIn1 = 11;
                final int yIn1 = 17;
                
                final int xIn2 = t12.xIn2(xIn1, yIn1);
                final int yIn2 = t12.yIn2(xIn1, yIn1);
                
                final int xIn3 = t23.xIn2(xIn2, yIn2);
                final int yIn3 = t23.yIn2(xIn2, yIn2);
                
                final GTransform t13 = t12.composed(t23);
                
                final int xIn3_bis = t13.xIn2(xIn1, yIn1);
                final int yIn3_bis = t13.yIn2(xIn1, yIn1);
                
                assertEquals(xIn3, xIn3_bis);
                assertEquals(yIn3, yIn3_bis);
            }
        }
    }

    /*
     * 
     */

    public void test_toString() {
        assertEquals("[0, 0, 0]", GTransform.IDENTITY.toString());
        assertEquals("[90, 5, 7]", GTransform.valueOf(GRotation.ROT_90, 5, 7).toString());
    }

    public void test_hashCode() {
        final GTransform t0 = GTransform.valueOf(GRotation.ROT_90, 5, 7);
        final GTransform t1 = GTransform.valueOf(GRotation.ROT_180, 5, 7);
        final GTransform t2 = GTransform.valueOf(GRotation.ROT_90, 5+1, 7);
        final GTransform t3 = GTransform.valueOf(GRotation.ROT_90, 5, 7+1);
        final List<GTransform> list = Arrays.asList(t0, t1, t2, t3);
        
        for (GTransform a : list) {
            for (GTransform b : list) {
                if (a.equals(b)) {
                    continue;
                }
                assertTrue(a.hashCode() != b.hashCode());
            }
        }
    }
    
    public void test_equals_Object() {
        for (GTransform transform : TRANSFORM_LIST) {
            assertFalse(transform.equals(null));
            assertFalse(transform.equals(new Object()));

            assertTrue(transform.equals(transform));
            
            assertTrue(transform.equals(
                    GTransform.valueOf(
                            transform.rotation(),
                            transform.frame2XIn1(),
                            transform.frame2YIn1())));
        }
        
        assertFalse(GTransform.valueOf(GRotation.ROT_90, 5, 7).equals(GTransform.valueOf(GRotation.ROT_180, 5, 7)));
        assertFalse(GTransform.valueOf(GRotation.ROT_90, 5, 7).equals(GTransform.valueOf(GRotation.ROT_90, 5+1, 7)));
        assertFalse(GTransform.valueOf(GRotation.ROT_90, 5, 7).equals(GTransform.valueOf(GRotation.ROT_90, 5, 7+1)));
    }
    
    public void test_equalsTransform_GRotation_2int() {
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).equalsTransform(GRotation.ROT_90, 5, 7));
        
        assertFalse(GTransform.valueOf(GRotation.ROT_90, 5, 7).equalsTransform(GRotation.ROT_180, 5, 7));
        assertFalse(GTransform.valueOf(GRotation.ROT_90, 5, 7).equalsTransform(GRotation.ROT_90, 5+1, 7));
        assertFalse(GTransform.valueOf(GRotation.ROT_90, 5, 7).equalsTransform(GRotation.ROT_90, 5, 7+1));
    }

    public void test_compareTo_GTransform() {
        for (GTransform transform : TRANSFORM_LIST) {
            try {
                transform.compareTo(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_90, 5, 7)) == 0);
        
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_180, 5, 7)) < 0);
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_90, 5+1, 7)) < 0);
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_90, 5, 7+1)) < 0);
        
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_0, 5, 7)) > 0);
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_90, 5-1, 7)) > 0);
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_90, 5, 7-1)) > 0);
        
        // dy checked before dx
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_90, 5-1, 7+1)) < 0);
        // dx checked before rotation
        assertTrue(GTransform.valueOf(GRotation.ROT_90, 5, 7).compareTo(GTransform.valueOf(GRotation.ROT_0, 5+1, 7)) < 0);
    }
    
    /*
     * 
     */
    
    public void test_rotation() {
        final GTransform transform = GTransform.valueOf(GRotation.ROT_90, 5, 7);
        assertEquals(GRotation.ROT_90, transform.rotation());
    }
    
    public void test_frame2XIn1() {
        final GTransform transform = GTransform.valueOf(GRotation.ROT_90, 5, 7);
        assertEquals(5, transform.frame2XIn1());
    }
    
    public void test_frame2YIn1() {
        final GTransform transform = GTransform.valueOf(GRotation.ROT_90, 5, 7);
        assertEquals(7, transform.frame2YIn1());
    }

    /*
     * 
     */
    
    public void test_isIdentity() {
        assertTrue(GTransform.IDENTITY.isIdentity());
        
        assertFalse(GTransform.valueOf(GRotation.ROT_90, 0, 0).isIdentity());
        assertFalse(GTransform.valueOf(GRotation.ROT_0, 1, 0).isIdentity());
        assertFalse(GTransform.valueOf(GRotation.ROT_0, 0, 1).isIdentity());
    }
    
    /*
     * 
     */
    
    public void test_xIn2_2int() {
        assertEquals(11, GTransform.IDENTITY.xIn2(11, 17));
        
        assertEquals(11-5, GTransform.valueOf(GRotation.ROT_0, 5, 7).xIn2(11, 17));
        assertEquals(17, GTransform.valueOf(GRotation.ROT_90, 0, 0).xIn2(11, 17));
        assertEquals(17-7, GTransform.valueOf(GRotation.ROT_90, 5, 7).xIn2(11, 17));
    }
    
    public void test_yIn2_2int() {
        assertEquals(17, GTransform.IDENTITY.yIn2(11, 17));
        
        assertEquals(17-7, GTransform.valueOf(GRotation.ROT_0, 5, 7).yIn2(11, 17));
        assertEquals(-11, GTransform.valueOf(GRotation.ROT_90, 0, 0).yIn2(11, 17));
        assertEquals(-11+5, GTransform.valueOf(GRotation.ROT_90, 5, 7).yIn2(11, 17));
    }
    
    public void test_xIn1_2int() {
        assertEquals(11, GTransform.IDENTITY.xIn1(11, 17));
        
        assertEquals(11+5, GTransform.valueOf(GRotation.ROT_0, 5, 7).xIn1(11, 17));
        assertEquals(-17, GTransform.valueOf(GRotation.ROT_90, 0, 0).xIn1(11, 17));
        assertEquals(5-17, GTransform.valueOf(GRotation.ROT_90, 5, 7).xIn1(11, 17));
    }
    
    public void test_yIn1_2int() {
        assertEquals(17, GTransform.IDENTITY.yIn1(11, 17));
        
        assertEquals(17+7, GTransform.valueOf(GRotation.ROT_0, 5, 7).yIn1(11, 17));
        assertEquals(11, GTransform.valueOf(GRotation.ROT_90, 0, 0).yIn1(11, 17));
        assertEquals(11+7, GTransform.valueOf(GRotation.ROT_90, 5, 7).yIn1(11, 17));
    }

    /*
     * 
     */
    
    public void test_minXIn1_4int() {
        final GRect rect = GRect.valueOf(5, 7, 11, 17);
        for (GRotation rotation : GRotation.values()) {
            final GTransform transform = GTransform.valueOf(rotation, 19, 23);
            final int xA = transform.xIn1(rect.x(), rect.y());
            final int xB = transform.xIn1(rect.xMax(), rect.yMax());
            final int expected = Math.min(xA, xB);
            assertEquals(expected, transform.minXIn1(rect.x(), rect.y(), rect.xSpan(), rect.ySpan()));
        }
    }
    
    public void test_minYIn1_4int() {
        final GRect rect = GRect.valueOf(5, 7, 11, 17);
        for (GRotation rotation : GRotation.values()) {
            final GTransform transform = GTransform.valueOf(rotation, 19, 23);
            final int yA = transform.yIn1(rect.x(), rect.y());
            final int yB = transform.yIn1(rect.xMax(), rect.yMax());
            final int expected = Math.min(yA, yB);
            assertEquals(expected, transform.minYIn1(rect.x(), rect.y(), rect.xSpan(), rect.ySpan()));
        }
    }
    
    public void test_minXIn2_4int() {
        final GRect rect = GRect.valueOf(5, 7, 11, 17);
        for (GRotation rotation : GRotation.values()) {
            final GTransform transform = GTransform.valueOf(rotation, 19, 23);
            final int xA = transform.xIn2(rect.x(), rect.y());
            final int xB = transform.xIn2(rect.xMax(), rect.yMax());
            final int expected = Math.min(xA, xB);
            assertEquals(expected, transform.minXIn2(rect.x(), rect.y(), rect.xSpan(), rect.ySpan()));
        }
    }
    
    public void test_minYIn2_4int() {
        final GRect rect = GRect.valueOf(5, 7, 11, 17);
        for (GRotation rotation : GRotation.values()) {
            final GTransform transform = GTransform.valueOf(rotation, 19, 23);
            final int yA = transform.yIn2(rect.x(), rect.y());
            final int yB = transform.yIn2(rect.xMax(), rect.yMax());
            final int expected = Math.min(yA, yB);
            assertEquals(expected, transform.minYIn2(rect.x(), rect.y(), rect.xSpan(), rect.ySpan()));
        }
    }
    
    /*
     * 
     */
    
    public void test_rectIn1_GRect() {
        final GRect rect = GRect.valueOf(5, 7, 11, 17);
        
        assertEquals(rect, GTransform.IDENTITY.rectIn1(rect));
        
        assertEquals(GRect.valueOf(19+5, 23+7, 11, 17), GTransform.valueOf(GRotation.ROT_0, 19, 23).rectIn1(rect));
        assertEquals(GRect.valueOf(19-7-(17-1), 23+5, 17, 11), GTransform.valueOf(GRotation.ROT_90, 19, 23).rectIn1(rect));
    }
    
    public void test_rectIn2_GRect() {
        final GRect rect = GRect.valueOf(5, 7, 11, 17);
        
        assertEquals(rect, GTransform.IDENTITY.rectIn2(rect));
        
        // Assuming rectIn1(...) works.
        final GTransform transform = GTransform.valueOf(GRotation.ROT_90, 19, 23);
        assertEquals(transform.inverted().rectIn1(rect), transform.rectIn2(rect));
    }
}
