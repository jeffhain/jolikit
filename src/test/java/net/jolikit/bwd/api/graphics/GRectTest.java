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
package net.jolikit.bwd.api.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.jolikit.lang.NumbersUtils;
import junit.framework.TestCase;

public class GRectTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MIN = Integer.MIN_VALUE;
    private static final int MAX = Integer.MAX_VALUE;
    
    /**
     * General and special cases.
     */
    private static final int[] POS_ARR = new int[]{
            MIN, MIN+1,
            MIN/2-1, MIN/2, MIN/2+1,
            -3, -2, -1,
            0, 1, 2,
            MAX/2-1, MAX/2, MAX/2+1,
            MAX-1, MAX,
    };
    
    /**
     * General and special cases.
     */
    private static final int[] SPAN_ARR = new int[]{
        0, 1, 2,
        MAX/2-1, MAX/2, MAX/2+1,
        MAX-1, MAX,
    };
    
    /**
     * General and special cases.
     */
    private static final List<GRect> RECT_LIST;
    static {
        final List<GRect> list = new ArrayList<GRect>();
        /*
         * Separating X and Y cases,
         * not to have too many zillions of rectangles.
         */
        /*
         * X cases.
         */
        for (int x : POS_ARR) {
            for (int xSpan : SPAN_ARR) {
                // Empty in y.
                list.add(GRect.valueOf(x, 0, xSpan, 0));
                // Not empty in y.
                list.add(GRect.valueOf(x, 0, xSpan, 1));
            }
        }
        /*
         * Y cases.
         */
        for (int y : POS_ARR) {
            for (int ySpan : SPAN_ARR) {
                // Empty in x.
                list.add(GRect.valueOf(0, y, 0, ySpan));
                // Not empty in x.
                list.add(GRect.valueOf(0, y, 1, ySpan));
            }
        }
        RECT_LIST = Collections.unmodifiableList(list);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_DEFAULT_EMPTY() {
        assertEquals(0, GRect.DEFAULT_EMPTY.x());
        assertEquals(0, GRect.DEFAULT_EMPTY.y());
        assertEquals(0, GRect.DEFAULT_EMPTY.xSpan());
        assertEquals(0, GRect.DEFAULT_EMPTY.ySpan());
    }

    public void test_DEFAULT_HUGE() {
        assertEquals(0, GRect.DEFAULT_HUGE.x());
        assertEquals(0, GRect.DEFAULT_HUGE.y());
        assertEquals(MAX/2, GRect.DEFAULT_HUGE.xSpan());
        assertEquals(MAX/2, GRect.DEFAULT_HUGE.ySpan());
    }
    
    /*
     * 
     */
    
    public void test_DEFAULT_EMPTY_LIST() {
        assertEquals(0, GRect.DEFAULT_EMPTY_LIST.size());
    }

    public void test_DEFAULT_HUGE_IN_LIST() {
        try {
            GRect.DEFAULT_HUGE_IN_LIST.add(null);
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertEquals(1, GRect.DEFAULT_HUGE_IN_LIST.size());
        assertSame(GRect.DEFAULT_HUGE, GRect.DEFAULT_HUGE_IN_LIST.get(0));
    }
    
    /*
     * 
     */

    public void test_valueOf_4int() {
        
        assertSame(GRect.DEFAULT_EMPTY, GRect.valueOf(0, 0, 0, 0));
        
        /*
         * 
         */
        
        for (int[] badArgs : new int[][]{
                // Negative spans.
                {0, 0, -1, 0},
                {0, 0, 0, -1},
                {0, 0, -MAX, 0},
                {0, 0, 0, -MAX},
                {0, 0, MIN, 0},
                {0, 0, 0, MIN},
        }) {
            final int x = badArgs[0];
            final int y = badArgs[1];
            final int xSpan = badArgs[2];
            final int ySpan = badArgs[3];
            if (DEBUG) {
                System.out.println("x = " + x);
                System.out.println("y = " + y);
                System.out.println("xSpan = " + xSpan);
                System.out.println("ySpan = " + ySpan);
            }
            try {
                GRect.valueOf(x, y, xSpan, ySpan);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        /*
         * 
         */
        
        for (int[] goodArgs : new int[][]{
                {MIN, MIN, 0, 0},
                {MIN, MIN, 1, 1},
                {MIN, MIN, MAX, MAX},
                //
                {0, 0, 0, 0},
                {0, 0, 1, 1},
                {0, 0, MAX, MAX},
                // Next to overflow.
                {1, 1, MAX, MAX},
                // Overflowing rectangle.
                {2, 2, MAX, MAX},
                //
                {MAX, MAX, 0, 0},
                // Next to overflow.
                {MAX, MAX, 1, 1},
                // Overflowing rectangles.
                {MAX, MAX, 2, 2},
                {MAX, MAX, MAX, MAX},
        }) {
            final int x = goodArgs[0];
            final int y = goodArgs[1];
            final int xSpan = goodArgs[2];
            final int ySpan = goodArgs[3];
            if (DEBUG) {
                System.out.println("x = " + x);
                System.out.println("y = " + y);
                System.out.println("xSpan = " + xSpan);
                System.out.println("ySpan = " + ySpan);
            }
            {
                final GRect rect = GRect.valueOf(x, y, xSpan, ySpan);
                assertEquals(x, rect.x());
                assertEquals(y, rect.y());
                assertEquals(xSpan, rect.xSpan());
                assertEquals(ySpan, rect.ySpan());
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_withX_int() {
        assertEquals(
                GRect.valueOf(19, 7, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withX(19));
        
        // Overflowing rectangle.
        assertEquals(
                GRect.valueOf(MAX, 7, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withX(MAX));
    }
    
    public void test_withY_int() {
        assertEquals(
                GRect.valueOf(5, 19, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withY(19));
        
        // Overflowing rectangle.
        assertEquals(
                GRect.valueOf(5, MAX, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withY(MAX));
    }

    public void test_withXSpan_int() {
        assertEquals(
                GRect.valueOf(5, 7, 19, 17),
                GRect.valueOf(5, 7, 11, 17).withXSpan(19));
        
        // Overflowing rectangle.
        assertEquals(
                GRect.valueOf(5, 7, MAX, 17),
                GRect.valueOf(5, 7, 11, 17).withXSpan(MAX));
    }

    public void test_withYSpan_int() {
        assertEquals(
                GRect.valueOf(5, 7, 11, 19),
                GRect.valueOf(5, 7, 11, 17).withYSpan(19));
        
        // Overflowing rectangle.
        assertEquals(
                GRect.valueOf(5, 7, 11, MAX),
                GRect.valueOf(5, 7, 11, 17).withYSpan(MAX));
    }

    /*
     * 
     */

    public void test_withPos_2int() {
        assertEquals(
                GRect.valueOf(19, 23, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withPos(19, 23));
        
        // Overflowing rectangles.
        assertEquals(
                GRect.valueOf(MAX, 23, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withPos(MAX, 23));
        assertEquals(
                GRect.valueOf(19, MAX, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withPos(19, MAX));
    }

    public void test_withSpans_2int() {
        assertEquals(
                GRect.valueOf(5, 7, 19, 23),
                GRect.valueOf(5, 7, 11, 17).withSpans(19, 23));
        
        // Overflowing rectangles.
        assertEquals(
                GRect.valueOf(5, 7, MAX, 23),
                GRect.valueOf(5, 7, 11, 17).withSpans(MAX, 23));
        assertEquals(
                GRect.valueOf(5, 7, 19, MAX),
                GRect.valueOf(5, 7, 11, 17).withSpans(19, MAX));
    }

    /*
     * 
     */

    public void test_withPosDeltas_2int() {
        // Modulo arithmetic.
        assertEquals(
                GRect.valueOf(5+MAX, 7+MAX, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withPosDeltas(MAX, MAX));
        
        assertEquals(
                GRect.valueOf(5+19, 7+23, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withPosDeltas(19, 23));
        
        // Overflowing rectangles.
        assertEquals(
                GRect.valueOf(MAX, 7+23, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withPosDeltas(MAX-5, 23));
        assertEquals(
                GRect.valueOf(5+19, MAX, 11, 17),
                GRect.valueOf(5, 7, 11, 17).withPosDeltas(19, MAX-7));
    }

    public void test_withSpansDeltas_2int() {
        // Modulo arithmetic causing negative spans.
        try {
            GRect.valueOf(5, 7, 11, 17).withSpansDeltas(MAX, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            GRect.valueOf(5, 7, 11, 17).withSpansDeltas(0, MAX);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        
        assertEquals(
                GRect.valueOf(5, 7, 11+19, 17+23),
                GRect.valueOf(5, 7, 11, 17).withSpansDeltas(19, 23));
        
        // Overflowing rectangles.
        assertEquals(
                GRect.valueOf(5, 7, MAX, 17+23),
                GRect.valueOf(5, 7, 11, 17).withSpansDeltas(MAX-11, 23));
        assertEquals(
                GRect.valueOf(5, 7, 11+19, MAX),
                GRect.valueOf(5, 7, 11, 17).withSpansDeltas(19, MAX-17));
    }

    /*
     * 
     */
    
    public void test_withBordersDeltas_4int() {
        // Position move to negative span.
        try {
            GRect.valueOf(5, 7, 11, 17).withBordersDeltas(11+1, 0, 0, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 17+1, 0, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        // Span reduction to negative span.
        try {
            GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, -(11+1), 0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, 0, -(17+1));
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        // Modulo arithmetic causing negative spans.
        try {
            GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, MAX, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, 0, MAX);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        // Position move to span of 1.
        assertEquals(
                GRect.valueOf(5+11-1, 7, 1, 17),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(11-1, 0, 0, 0));
        assertEquals(
                GRect.valueOf(5, 7+17-1, 11, 1),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 17-1, 0, 0));

        // Position move to span of 0.
        assertEquals(
                GRect.valueOf(5+11, 7, 0, 17),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(11, 0, 0, 0));
        assertEquals(
                GRect.valueOf(5, 7+17, 11, 0),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 17, 0, 0));
        
        // Span reduction to 1.
        assertEquals(
                GRect.valueOf(5, 7, 1, 17),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, -(11-1), 0));
        assertEquals(
                GRect.valueOf(5, 7, 11, 1),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, 0, -(17-1)));
        
        // Span reduction to 0.
        assertEquals(
                GRect.valueOf(5, 7, 0, 17),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, -11, 0));
        assertEquals(
                GRect.valueOf(5, 7, 11, 0),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, 0, -17));
        
        // Regular growth.
        assertEquals(
                GRect.valueOf(5-1, 7-2, 11+(1+3), 17+(2+4)),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(-1, -2, 3, 4));
        
        // Overflowing rectangles.
        assertEquals(
                GRect.valueOf(5, 7, MAX, 17),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, MAX-11, 0));
        assertEquals(
                GRect.valueOf(5, 7, 11, MAX),
                GRect.valueOf(5, 7, 11, 17).withBordersDeltas(0, 0, 0, MAX-17));
    }

    /*
     * 
     */
    
    public void test_toString() {
        assertEquals("[0, 0, 0, 0]", GRect.DEFAULT_EMPTY.toString());
        assertEquals("[5, 7, 11, 17]", GRect.valueOf(5, 7, 11, 17).toString());
    }

    public void test_hashCode() {
        final GRect r0 = GRect.valueOf(5, 7, 11, 17);
        final GRect r1 = GRect.valueOf(5+1, 7, 11, 17);
        final GRect r2 = GRect.valueOf(5, 7+1, 11, 17);
        final GRect r3 = GRect.valueOf(5, 7, 11+1, 17);
        final GRect r4 = GRect.valueOf(5, 7, 11, 17+1);
        final List<GRect> list = Arrays.asList(r0, r1, r2, r3, r4);
        
        for (GRect a : list) {
            for (GRect b : list) {
                if (a.equals(b)) {
                    continue;
                }
                assertTrue(a.hashCode() != b.hashCode());
            }
        }
    }

    public void test_equals_Object() {
        for (GRect rect : RECT_LIST) {
            assertFalse(rect.equals(null));
            assertFalse(rect.equals(new Object()));
            
            assertTrue(rect.equals(rect));
            
            assertTrue(rect.equals(
                    GRect.valueOf(
                            rect.x(),
                            rect.y(),
                            rect.xSpan(),
                            rect.ySpan())));
        }
        
        assertFalse(GRect.valueOf(5, 7, 11, 17).equals(GRect.valueOf(5+1, 7, 11, 17)));
        assertFalse(GRect.valueOf(5, 7, 11, 17).equals(GRect.valueOf(5, 7+1, 11, 17)));
        assertFalse(GRect.valueOf(5, 7, 11, 17).equals(GRect.valueOf(5, 7, 11+1, 17)));
        assertFalse(GRect.valueOf(5, 7, 11, 17).equals(GRect.valueOf(5, 7, 11, 17+1)));
        
        for (GRect rect1 : RECT_LIST) {
            for (GRect rect2 : RECT_LIST) {
                final boolean expected =
                        (rect1.x() == rect2.x())
                        && (rect1.y() == rect2.y())
                        && (rect1.xSpan() == rect2.xSpan())
                        && (rect1.ySpan() == rect2.ySpan());
                assertEquals(expected, rect1.equals(rect2));
            }
        }
    }
    
    public void test_equalsRect_4int() {
        // Negative spans not zeroized.
        assertFalse(GRect.valueOf(5, 7, 0, 17).equalsRect(5, 7, -11, 17));
        assertFalse(GRect.valueOf(5, 7, 11, 0).equalsRect(5, 7, 11, -17));
        
        assertTrue(GRect.valueOf(5, 7, 11, 17).equalsRect(5, 7, 11, 17));
        
        assertFalse(GRect.valueOf(5, 7, 11, 17).equalsRect(5+1, 7, 11, 17));
        assertFalse(GRect.valueOf(5, 7, 11, 17).equalsRect(5, 7+1, 11, 17));
        assertFalse(GRect.valueOf(5, 7, 11, 17).equalsRect(5, 7, 11+1, 17));
        assertFalse(GRect.valueOf(5, 7, 11, 17).equalsRect(5, 7, 11, 17+1));
    }

    public void test_compareTo_GRect() {
        for (GRect rect : RECT_LIST) {
            try {
                rect.compareTo(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7, 11, 17)) == 0);
        
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5+1, 7, 11, 17)) < 0);
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7+1, 11, 17)) < 0);
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7, 11+1, 17)) < 0);
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7, 11, 17+1)) < 0);
        
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5-1, 7, 11, 17)) > 0);
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7-1, 11, 17)) > 0);
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7, 11-1, 17)) > 0);
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7, 11, 17-1)) > 0);
        
        // y checked before x
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5-1, 7+1, 11, 17)) < 0);
        // ySpan checked before xSpan
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7, 11-1, 17+1)) < 0);
        // x checked before xSpan
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5-1, 7, 11+1, 17)) > 0);
        // y checked before ySpan
        assertTrue(GRect.valueOf(5, 7, 11, 17).compareTo(GRect.valueOf(5, 7-1, 11, 17+1)) > 0);
    }

    /*
     * 
     */

    public void test_x() {
        assertEquals(5, GRect.valueOf(5, 7, 11, 17).x());
    }

    public void test_y() {
        assertEquals(7, GRect.valueOf(5, 7, 11, 17).y());
    }

    public void test_xSpan() {
        assertEquals(11, GRect.valueOf(5, 7, 11, 17).xSpan());
    }

    public void test_ySpan() {
        assertEquals(17, GRect.valueOf(5, 7, 11, 17).ySpan());
    }

    /*
     * 
     */

    public void test_xMid() {
        for (GRect rect : new GRect[]{
                GRect.valueOf(MAX/2 + 2, 7, MAX, 17),
                GRect.valueOf(MAX, 7, 2, 17),
                GRect.valueOf(MAX, 7, MAX, 17),
        }) {
            try {
                rect.xMid();
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(5 + 11/2, GRect.valueOf(5, 7, 11, 17).xMid());
        assertEquals(5 + 12/2, GRect.valueOf(5, 7, 12, 17).xMid());
        
        // Overflowing rectangle.
        assertEquals(MAX, GRect.valueOf(MAX-1, 7, 3, 17).xMid());
    }

    public void test_yMid() {
        for (GRect rect : new GRect[]{
                GRect.valueOf(5, MAX/2 + 2, 11, MAX),
                GRect.valueOf(5, MAX, 11, 2),
                GRect.valueOf(5, MAX, 11, MAX),
        }) {
            try {
                rect.yMid();
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(7 + 17/2, GRect.valueOf(5, 7, 11, 17).yMid());
        assertEquals(7 + 14/2, GRect.valueOf(5, 7, 11, 14).yMid());
        
        // Overflowing rectangle.
        assertEquals(MAX, GRect.valueOf(5, MAX-1, 11, 3).yMid());
    }
    
    /*
     * 
     */

    public void test_xMidLong() {
        for (GRect rect : RECT_LIST) {
            final long expected = rect.x() + (long) (rect.xSpan() / 2);
            final long actual = rect.xMidLong();
            assertEquals(expected, actual);
        }
    }

    public void test_yMidLong() {
        for (GRect rect : RECT_LIST) {
            final long expected = rect.y() + (long) (rect.ySpan() / 2);
            final long actual = rect.yMidLong();
            assertEquals(expected, actual);
        }
    }
    
    /*
     * 
     */
    
    public void test_xMidFp() {
        for (GRect rect : RECT_LIST) {
            final double expected = rect.x() + (rect.xMaxLong() - rect.x()) / 2.0;
            final double actual = rect.xMidFp();
            assertEquals(expected, actual);
        }
    }
    
    public void test_yMidFp() {
        for (GRect rect : RECT_LIST) {
            final double expected = rect.y() + (rect.yMaxLong() - rect.y()) / 2.0;
            final double actual = rect.yMidFp();
            assertEquals(expected, actual);
        }
    }

    /*
     * 
     */

    public void test_xMax() {
        for (GRect rect : new GRect[]{
                GRect.valueOf(MIN, 7, 0, 17),
                //
                GRect.valueOf(2, 7, MAX, 17),
                GRect.valueOf(MAX/2, 7, (MAX - MAX/2) + 2, 17),
                GRect.valueOf(MAX, 7, 2, 17),
                GRect.valueOf(MAX, 7, MAX, 17),
        }) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            try {
                rect.xMax();
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(5+11-1, GRect.valueOf(5, 7, 11, 17).xMax());
        
        assertEquals(MIN, GRect.valueOf(MIN, 7, 1, 17).xMax());
        assertEquals(MAX, GRect.valueOf(5, 7, MAX-5+1, 17).xMax());
        assertEquals(MAX, GRect.valueOf(MAX, 7, 1, 17).xMax());
    }

    public void test_yMax() {
        for (GRect rect : new GRect[]{
                GRect.valueOf(5, MIN, 11, 0),
                //
                GRect.valueOf(5, 2, 11, MAX),
                GRect.valueOf(5, MAX/2, 11, (MAX - MAX/2) + 2),
                GRect.valueOf(5, MAX, 11, 2),
                GRect.valueOf(5, MAX, 11, MAX),
        }) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            try {
                rect.yMax();
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(7+17-1, GRect.valueOf(5, 7, 11, 17).yMax());
        
        assertEquals(MIN, GRect.valueOf(5, MIN, 11, 1).yMax());
        assertEquals(MAX, GRect.valueOf(5, 7, 11, MAX-7+1).yMax());
        assertEquals(MAX, GRect.valueOf(5, MAX, 11, 1).yMax());
    }
    
    /*
     * 
     */

    public void test_xMaxLong() {
        for (GRect rect : RECT_LIST) {
            final long expected = rect.x() + (long) rect.xSpan() - 1;
            final long actual = rect.xMaxLong();
            assertEquals(expected, actual);
        }
    }

    public void test_yMaxLong() {
        for (GRect rect : RECT_LIST) {
            final long expected = rect.y() + (long) rect.ySpan() - 1;
            final long actual = rect.yMaxLong();
            assertEquals(expected, actual);
        }
    }

    /*
     * 
     */

    public void test_isEmpty() {
        assertTrue(GRect.valueOf(0, 0, 0, 0).isEmpty());
        assertTrue(GRect.valueOf(0, 0, 1, 0).isEmpty());
        assertTrue(GRect.valueOf(0, 0, 0, 1).isEmpty());
        //
        assertFalse(GRect.valueOf(0, 0, 1, 1).isEmpty());
    }
    
    public void test_contains_2int() {
        for (GRect rect : RECT_LIST) {
            final int x = rect.x();
            final int y = rect.y();
            
            if (rect.isEmpty()) {
                assertFalse(rect.contains(x, y));
                continue;
            }
            
            final int xMaxTrimmed = (rect.doesOverflowInX() ? MAX : rect.xMax());
            final int yMaxTrimmed = (rect.doesOverflowInY() ? MAX : rect.yMax());
            
            /*
             * In.
             */
            
            for (int[] inArr : new int[][]{
                    {x, y},
                    {x, yMaxTrimmed},
                    {xMaxTrimmed, y},
                    {xMaxTrimmed, yMaxTrimmed},
            }) {
                final int px = inArr[0];
                final int py = inArr[1];
                if (DEBUG) {
                    System.out.println("rect = " + rect);
                    System.out.println("px = " + px);
                    System.out.println("py = " + py);
                }
                if (rect.isEmpty()) {
                    assertFalse(rect.contains(px, py));
                } else {
                    assertTrue(rect.contains(px, py));
                }
            }
            
            /*
             * Not in.
             */
            
            for (int[] outArr : new int[][]{
                    {x-1, y},
                    {x, y-1},
                    //
                    {x-1, yMaxTrimmed},
                    {x, yMaxTrimmed+1},
                    //
                    {xMaxTrimmed+1, y},
                    {xMaxTrimmed, y-1},
                    //
                    {xMaxTrimmed+1, yMaxTrimmed},
                    {xMaxTrimmed, yMaxTrimmed+1},
            }) {
                final int px = outArr[0];
                final int py = outArr[1];
                if (DEBUG) {
                    System.out.println("rect = " + rect);
                    System.out.println("px = " + px);
                    System.out.println("py = " + py);
                }
                assertFalse(rect.contains(px, py));
            }
        }
    }
    
    public void test_contains_GRect() {
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }

            try {
                rect.contains(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }

            for (GRect other : newRectsWithCoordsAllIn(rect)) {
                if (DEBUG) {
                    System.out.println("other = " + other);
                }
                if (rect.isEmpty()) {
                    // Empty never containing.
                    assertFalse(rect.contains(other));
                } else if (other.isEmpty()) {
                    // Empty never contained.
                    assertFalse(rect.contains(other));
                } else {
                    assertTrue(rect.contains(other));
                }
            }
            
            for (GRect other : newRectsWithCoordsNotAllIn(rect)) {
                if (DEBUG) {
                    System.out.println("other = " + other);
                }
                assertFalse(rect.contains(other));
            }
        }
    }
    
    public void test_containsCoordinatesOf_GRect() {
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            
            try {
                rect.containsCoordinatesOf(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
            
            for (GRect other : newRectsWithCoordsAllIn(rect)) {
                if (DEBUG) {
                    System.out.println("other = " + other);
                }
                assertTrue(rect.containsCoordinatesOf(other));
            }
            
            for (GRect other : newRectsWithCoordsNotAllIn(rect)) {
                if (DEBUG) {
                    System.out.println("other = " + other);
                }
                assertFalse(rect.containsCoordinatesOf(other));
            }
        }
    }

    public void test_overlaps_GRect() {
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            
            try {
                rect.overlaps(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
            
            if (rect.isEmpty()) {
                for (GRect other : RECT_LIST) {
                    if (DEBUG) {
                        System.out.println("other = " + other);
                    }
                    assertFalse(rect.overlaps(other));
                    assertFalse(other.overlaps(rect));
                }
            } else {
                for (GRect other : newRectsOverlapping(rect)) {
                    if (DEBUG) {
                        System.out.println("other = " + other);
                    }
                    assertTrue(rect.overlaps(other));
                    assertTrue(other.overlaps(rect));
                }
                for (GRect other : newRectsNotOverlapping(rect)) {
                    if (DEBUG) {
                        System.out.println("other = " + other);
                    }
                    assertFalse(rect.overlaps(other));
                    assertFalse(other.overlaps(rect));
                }
            }
        }
        
        /*
         * More pathological cases.
         */
        
        for (GRect lo : new GRect[]{
                GRect.valueOf(MIN, MIN, 1, 1),
                GRect.valueOf(MIN, MIN, MAX/2, MAX/2),
                GRect.valueOf(MIN, MIN, MAX, MAX),
        }) {
            for (GRect hi : new GRect[]{
                    GRect.valueOf(MAX, MAX, 1, 1),
                    GRect.valueOf(MAX, MAX, MAX/2, MAX/2),
                    GRect.valueOf(MAX, MAX, MAX, MAX),
            }) {
                assertFalse(lo.overlaps(hi));
                assertFalse(hi.overlaps(lo));
            }
        }
    }

    public void test_overlap_8int() {
        for (GRect rect1 : RECT_LIST) {
            for (GRect rect2 : RECT_LIST) {
                if (DEBUG) {
                    System.out.println("rect1 = " + rect1);
                    System.out.println("rect2 = " + rect2);
                }
                
                {
                    final boolean actual = GRect.overlap(
                            rect1.x(), rect1.y(), rect1.xSpan(), rect1.ySpan(),
                            rect2.x(), rect2.y(), rect2.xSpan(), rect2.ySpan());
                    final boolean expected = rect1.overlaps(rect2);
                    assertEquals(expected, actual);
                }
                
                /*
                 * Checks with spans <= 0.
                 */
                
                {
                    final boolean actual = GRect.overlap(
                            rect1.x(), rect1.y(), -rect1.xSpan(), rect1.ySpan(),
                            rect2.x(), rect2.y(), rect2.xSpan(), rect2.ySpan());
                    final boolean expected = rect1.withXSpan(0).overlaps(rect2);
                    assertEquals(expected, actual);
                }
                
                {
                    final boolean actual = GRect.overlap(
                            rect1.x(), rect1.y(), rect1.xSpan(), -rect1.ySpan(),
                            rect2.x(), rect2.y(), rect2.xSpan(), rect2.ySpan());
                    final boolean expected = rect1.withYSpan(0).overlaps(rect2);
                    assertEquals(expected, actual);
                }
                
                {
                    final boolean actual = GRect.overlap(
                            rect1.x(), rect1.y(), rect1.xSpan(), rect1.ySpan(),
                            rect2.x(), rect2.y(), -rect2.xSpan(), rect2.ySpan());
                    final boolean expected = rect1.overlaps(rect2.withXSpan(0));
                    assertEquals(expected, actual);
                }
                
                {
                    final boolean actual = GRect.overlap(
                            rect1.x(), rect1.y(), rect1.xSpan(), rect1.ySpan(),
                            rect2.x(), rect2.y(), rect2.xSpan(), -rect2.ySpan());
                    final boolean expected = rect1.overlaps(rect2.withYSpan(0));
                    assertEquals(expected, actual);
                }
            }
        }
    }

    /*
     * 
     */
    
    public void test_intersected_GRect() {
        for (GRect rect : RECT_LIST) {
            try {
                rect.intersected(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }

        assertEquals(
                GRect.valueOf(5+1, 7, 11-1, 17),
                GRect.valueOf(5, 7, 11, 17).intersected(GRect.valueOf(5+1, 7, 11, 17)));
        assertEquals(
                GRect.valueOf(5, 7+1, 11, 17-1),
                GRect.valueOf(5, 7, 11, 17).intersected(GRect.valueOf(5, 7+1, 11, 17)));
        assertEquals(
                GRect.valueOf(5, 7, 11-1, 17),
                GRect.valueOf(5, 7, 11, 17).intersected(GRect.valueOf(5, 7, 11-1, 17)));
        assertEquals(
                GRect.valueOf(5, 7, 11, 17-1),
                GRect.valueOf(5, 7, 11, 17).intersected(GRect.valueOf(5, 7, 11, 17-1)));
        
        for (GRect rect1 : RECT_LIST) {
            for (GRect rect2 : RECT_LIST) {
                if (DEBUG) {
                    System.out.println("rect1 = " + rect1);
                    System.out.println("rect2 = " + rect2);
                }
                
                final GRect res = rect1.intersected(rect2);
                
                // Checking rectangle reuse.
                if (res.equals(rect1)) {
                    assertSame(rect1, res);
                } else if (res.equals(rect2)) {
                    assertSame(rect2, res);
                }
                
                final int expectedX = Math.max(rect1.x(), rect2.x());
                assertEquals(expectedX, res.x());
                
                final int expectedY = Math.max(rect1.y(), rect2.y());
                assertEquals(expectedY, res.y());
                
                final long xMaxExcl1Long = rect1.x() + (long) rect1.xSpan();
                final long xMaxExcl2Long = rect2.x() + (long) rect2.xSpan();
                final long expectedXSpanLong = Math.max(0, Math.min(xMaxExcl1Long, xMaxExcl2Long) - expectedX);
                final int expectedXSpan = NumbersUtils.asInt(expectedXSpanLong);
                assertEquals(expectedXSpan, res.xSpan());

                final long yMaxExcl1Long = rect1.y() + (long) rect1.ySpan();
                final long yMaxExcl2Long = rect2.y() + (long) rect2.ySpan();
                final long expectedYSpanLong = Math.max(0, Math.min(yMaxExcl1Long, yMaxExcl2Long) - expectedY);
                final int expectedYSpan = NumbersUtils.asInt(expectedYSpanLong);
                assertEquals(expectedYSpan, res.ySpan());
            }
        }
    }

    public void test_intersectedPos_2int() {
        for (int pos1 : POS_ARR) {
            for (int pos2 : POS_ARR) {
                final int expected = Math.max(pos1, pos2);
                final int actual = GRect.intersectedPos(pos1, pos2);
                assertEquals(expected, actual);
            }
        }
    }

    public void test_intersectedSpan_4int() {
        for (GRect rect1 : RECT_LIST) {
            for (GRect rect2 : RECT_LIST) {
                if (DEBUG) {
                    System.out.println("rect1 = " + rect1);
                    System.out.println("rect2 = " + rect2);
                }
                
                {
                    final int actual = GRect.intersectedSpan(
                            rect1.x(), rect1.xSpan(),
                            rect2.x(), rect2.xSpan());
                    final int expected = rect1.intersected(rect2).xSpan();
                    assertEquals(expected, actual);
                }
                
                /*
                 * Checks with spans <= 0.
                 */
                
                {
                    final int actual = GRect.intersectedSpan(
                            rect1.x(), -rect1.xSpan(),
                            rect2.x(), rect2.xSpan());
                    final int expected = rect1.withXSpan(0).intersected(rect2).xSpan();
                    assertEquals(expected, actual);
                }
                
                {
                    final int actual = GRect.intersectedSpan(
                            rect1.x(), rect1.xSpan(),
                            rect2.x(), -rect2.xSpan());
                    final int expected = rect1.intersected(rect2.withXSpan(0)).xSpan();
                    assertEquals(expected, actual);
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_unionBoundingBox_2int() {
        for (GRect rect : new GRect[]{
                GRect.valueOf(MIN, 0, 1, 1),
                GRect.valueOf(MIN, 0, MAX, 1),
                GRect.valueOf(MIN/2, 0, 1, 1),
                GRect.valueOf(MIN/2, 0, MAX, 1),
                GRect.valueOf(0, 0, 1, 1),
                GRect.valueOf(0, 0, MAX, 1),
        }) {
            try {
                rect.unionBoundingBox(MAX, 0);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        for (GRect rect : new GRect[]{
                GRect.valueOf(0, MIN, 1, 1),
                GRect.valueOf(0, MIN, 1, MAX),
                GRect.valueOf(0, MIN/2, 1, 1),
                GRect.valueOf(0, MIN/2, 1, MAX),
                GRect.valueOf(0, 0, 1, 1),
                GRect.valueOf(0, 0, 1, MAX),
        }) {
            try {
                rect.unionBoundingBox(0, MAX);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            if (rect.isEmpty()) {
                assertEquals(GRect.valueOf(5, 7, 1, 1), rect.unionBoundingBox(5, 7));
            } else {
                final int xMaxTrimmed = (rect.doesOverflowInX() ? MAX : rect.xMax());
                final int yMaxTrimmed = (rect.doesOverflowInY() ? MAX : rect.yMax());
                
                if (DEBUG) {
                    System.out.println("xMaxTrimmed = " + xMaxTrimmed);
                    System.out.println("yMaxTrimmed = " + yMaxTrimmed);
                }
                
                assertSame(rect, rect.unionBoundingBox(rect.x(), rect.y()));
                assertSame(rect, rect.unionBoundingBox(xMaxTrimmed, rect.y()));
                assertSame(rect, rect.unionBoundingBox(rect.x(), yMaxTrimmed));
                assertSame(rect, rect.unionBoundingBox(xMaxTrimmed, yMaxTrimmed));
                
                if ((rect.x() > MIN) && (rect.xSpan() < MAX)) {
                    final GRect bb = rect.unionBoundingBox(rect.x()-1, rect.y());
                    assertEquals(rect.withBordersDeltas(-1, 0, 0, 0), bb);
                }
                if ((rect.y() > MIN) && (rect.ySpan() < MAX)) {
                    final GRect bb = rect.unionBoundingBox(rect.x(), rect.y()-1);
                    assertEquals(rect.withBordersDeltas(0, -1, 0, 0), bb);
                }
                if ((xMaxTrimmed < MAX) && (rect.xSpan() < MAX)) {
                    final GRect bb = rect.unionBoundingBox(xMaxTrimmed+1, rect.y());
                    assertEquals(rect.withBordersDeltas(0, 0, 1, 0), bb);
                }
                if ((yMaxTrimmed < MAX) && (rect.ySpan() < MAX)) {
                    final GRect bb = rect.unionBoundingBox(rect.x(), yMaxTrimmed+1);
                    assertEquals(rect.withBordersDeltas(0, 0, 0, 1), bb);
                }
            }
        }
    }

    public void test_unionBoundingBox_GRect() {
        for (GRect rect : RECT_LIST) {
            try {
                rect.unionBoundingBox(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
        }
        {
            final GRect a = GRect.valueOf(-1, 0, 1, 1);
            final GRect b = GRect.valueOf(MAX, 0, 1, 1);
            try {
                a.unionBoundingBox(b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        {
            final GRect a = GRect.valueOf(0, -1, 1, 1);
            final GRect b = GRect.valueOf(0, MAX, 1, 1);
            try {
                a.unionBoundingBox(b);
                fail();
            } catch (ArithmeticException e) {
                // ok
            }
        }
        
        assertEquals(
                GRect.valueOf(5, 7, 11+1+3, 17+2+4),
                GRect.valueOf(5+1, 7, 11+3, 17).unionBoundingBox(GRect.valueOf(5, 7+2, 11, 17+4)));
        // Both empty, one in x, one in y.
        assertEquals(
                GRect.valueOf(5, 7, 0, 0),
                GRect.valueOf(5+1, 7, 11+3, 0).unionBoundingBox(GRect.valueOf(5, 7+2, 0, 17+4)));
        // Both empty in x, but not in y.
        assertEquals(
                GRect.valueOf(5, 7, 0, 17+2+4),
                GRect.valueOf(5+1, 7, 0, 17).unionBoundingBox(GRect.valueOf(5, 7+2, 0, 17+4)));
        // Both empty in y, but not in x.
        assertEquals(
                GRect.valueOf(5, 7, 11+1+3, 0),
                GRect.valueOf(5+1, 7, 11+3, 0).unionBoundingBox(GRect.valueOf(5, 7+2, 11, 0)));
        
        for (GRect rect1 : RECT_LIST) {
            for (GRect rect2 : RECT_LIST) {
                if (DEBUG) {
                    System.out.println("rect1 = " + rect1);
                    System.out.println("rect2 = " + rect2);
                }

                final int theoX = Math.min(rect1.x(), rect2.x());
                final int theoY = Math.min(rect1.y(), rect2.y());
                final long theoXMaxExclLong =
                        Math.max(rect1.x() + (long) rect1.xSpan(),
                                rect2.x() + (long) rect2.xSpan());
                final long theoYMaxExclLong =
                        Math.max(rect1.y() + (long) rect1.ySpan(),
                                rect2.y() + (long) rect2.ySpan());
                final boolean doesOverflowInX = (theoXMaxExclLong - theoX > MAX);
                final boolean doesOverflowInY = (theoYMaxExclLong - theoY > MAX);

                if (doesOverflowInX || doesOverflowInY) {
                    try {
                        rect1.unionBoundingBox(rect2);
                    } catch (ArithmeticException e) {
                        // ok
                    }
                } else {
                    final GRect res = rect1.unionBoundingBox(rect2);
                    if (DEBUG) {
                        System.out.println("res = " + res);
                    }
                    
                    if (rect1.isEmpty() != rect2.isEmpty()) {
                        if (rect1.isEmpty()) {
                            assertSame(rect2, res);
                        } else {
                            assertSame(rect1, res);
                        }
                    } else {
                        final int expectedX = theoX;
                        assertEquals(expectedX, res.x());
                        
                        final int expectedY = theoY;
                        assertEquals(expectedY, res.y());
                        
                        final int expectedXSpan;
                        if ((rect1.xSpan() > 0) && (rect2.xSpan() > 0)) {
                            expectedXSpan = NumbersUtils.asInt(Math.max(0, theoXMaxExclLong - theoX));
                        } else {
                            expectedXSpan = 0;
                        }
                        assertEquals(expectedXSpan, res.xSpan());
                        
                        final int expectedYSpan;
                        if ((rect1.ySpan() > 0) && (rect2.ySpan() > 0)) {
                            expectedYSpan = NumbersUtils.asInt(Math.max(0, theoYMaxExclLong - theoY));
                        } else {
                            expectedYSpan = 0;
                        }
                        assertEquals(expectedYSpan, res.ySpan());
                        
                        // rect1.isEmpty() = rect2.isEmpty() here.
                        // Result must be the same.
                        assertEquals(rect1.isEmpty(), res.isEmpty());
                        
                        // Checking rectangle reuse.
                        if (res.equals(rect1)) {
                            assertSame(rect1, res);
                        } else if (res.equals(rect2)) {
                            assertSame(rect2, res);
                        }
                    }
                }
            }
        }
    }

    /*
     * 
     */
    
    public void test_doesOverflowInX() {
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            if ((rect.x() == Integer.MIN_VALUE) && (rect.xSpan() == 0)) {
                // Special case.
                assertFalse(rect.doesOverflowInX());
            } else {
                final long xMaxLong = rect.x() + (long) rect.xSpan() - 1;
                final boolean expected = (xMaxLong != (int) xMaxLong);
                assertEquals(expected, rect.doesOverflowInX());
            }
        }
    }
    
    public void test_doesOverflowInY() {
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            if ((rect.y() == Integer.MIN_VALUE) && (rect.ySpan() == 0)) {
                // Special case.
                assertFalse(rect.doesOverflowInY());
            } else {
                final long yMaxLong = rect.y() + (long) rect.ySpan() - 1;
                final boolean expected = (yMaxLong != (int) yMaxLong);
                assertEquals(expected, rect.doesOverflowInY());
            }
        }
    }

    public void test_doesOverflow() {
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            final boolean expected =
                    rect.doesOverflowInX()
                    || rect.doesOverflowInY();
            assertEquals(expected, rect.doesOverflow());
        }
    }

    public void test_doesOverflow_2int() {
        for (int pos : POS_ARR) {
            // Pos list for spans, to have negative spans.
            for (int span : POS_ARR) {
                final boolean expected =
                        (span > 0)
                        && ((pos + (long) span - 1) > Integer.MAX_VALUE);
                assertEquals(expected, GRect.doesOverflow(pos, span));
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_trimmed() {
        for (GRect rect : RECT_LIST) {
            if (DEBUG) {
                System.out.println("rect = " + rect);
            }
            
            final GRect trimmed = rect.trimmed();
            if (DEBUG) {
                System.out.println("trimmed = " + trimmed);
            }
            
            if (rect.doesOverflow()) {
                assertFalse(trimmed.doesOverflow());
                assertEquals(rect.x(), trimmed.x());
                assertEquals(rect.y(), trimmed.y());
                
                if (rect.doesOverflowInX()) {
                    assertEquals(MAX, trimmed.xMax());
                } else {
                    assertEquals(rect.xSpan(), trimmed.xSpan());
                }
                if (rect.doesOverflowInY()) {
                    assertEquals(MAX, trimmed.yMax());
                } else {
                    assertEquals(rect.ySpan(), trimmed.ySpan());
                }
            } else {
                assertSame(rect, trimmed);
            }
        }
    }

    public void test_trimmedSpan_2int() {
        for (int pos : POS_ARR) {
            // Pos list for spans, to have negative spans.
            for (int span : POS_ARR) {
                final int expected;
                if (span <= 0) {
                    expected = span;
                } else {
                    final long maxLong = pos + (long) span - 1;
                    if (maxLong > MAX) {
                        final int overflow = (int) (maxLong - MAX);
                        expected = span - overflow;
                    } else {
                        expected = span;
                    }
                }
                assertEquals(expected, GRect.trimmedSpan(pos, span));
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static List<GRect> newRectsWithCoordsAllIn(GRect rect) {
        final List<GRect> list = new ArrayList<GRect>();
        final int x = rect.x();
        final int y = rect.y();
        final int xSpan = rect.xSpan();
        final int ySpan = rect.ySpan();
        
        list.add(rect);

        if ((x < MAX) && (xSpan > 0)) {
            list.add(GRect.valueOf(x+1, y, xSpan-1, ySpan));
        }
        if ((y < MAX) && (ySpan > 0)) {
            list.add(GRect.valueOf(x, y+1, xSpan, ySpan-1));
        }
        if (xSpan > 0) {
            list.add(GRect.valueOf(x, y, xSpan-1, ySpan));
        }
        if (ySpan > 0) {
            list.add(GRect.valueOf(x, y, xSpan, ySpan-1));
        }
        
        return list;
    }
    
    private static List<GRect> newRectsWithCoordsNotAllIn(GRect rect) {
        final List<GRect> list = new ArrayList<GRect>();
        final int x = rect.x();
        final int y = rect.y();
        final int xSpan = rect.xSpan();
        final int ySpan = rect.ySpan();
        
        if ((x == MAX) && (xSpan > 0)) {
            list.add(GRect.valueOf(x+1, y, xSpan-1, ySpan));
        }
        if ((y == MAX) && (ySpan > 0)) {
            list.add(GRect.valueOf(x, y+1, xSpan, ySpan-1));
        }
        
        if ((x > MIN) || (xSpan <= 1)) {
            list.add(GRect.valueOf(x-1, y, xSpan, ySpan));
        }
        if ((x > MIN) && (xSpan < MAX)) {
            list.add(GRect.valueOf(x-1, y, xSpan+1, ySpan));
        }
        if ((y > MIN) || (ySpan <= 1)) {
            list.add(GRect.valueOf(x, y-1, xSpan, ySpan));
        }
        if ((y > MIN) && (ySpan < MAX)) {
            list.add(GRect.valueOf(x, y-1, xSpan, ySpan+1));
        }
        if (xSpan < MAX) {
            list.add(GRect.valueOf(x, y, xSpan+1, ySpan));
        }
        if (ySpan < MAX) {
            list.add(GRect.valueOf(x, y, xSpan, ySpan+1));
        }

        return list;
    }
    
    /**
     * @param rect Must not be empty.
     */
    private static List<GRect> newRectsOverlapping(GRect rect) {
        if (rect.isEmpty()) {
            throw new IllegalArgumentException("" + rect);
        }
        
        final List<GRect> list = new ArrayList<GRect>();
        final int x = rect.x();
        final int y = rect.y();
        final int xSpan = rect.xSpan();
        final int ySpan = rect.ySpan();
        final int xMaxTrimmed = (rect.doesOverflowInX() ? MAX : rect.xMax());
        final int yMaxTrimmed = (rect.doesOverflowInY() ? MAX : rect.yMax());
        
        // Self.
        list.add(rect);

        // Sides.
        list.add(GRect.valueOf(x, y, 1, ySpan));
        list.add(GRect.valueOf(x, y, xSpan, 1));
        list.add(GRect.valueOf(xMaxTrimmed, y, 1, ySpan));
        list.add(GRect.valueOf(x, yMaxTrimmed, xSpan, 1));

        // Corners.
        list.add(GRect.valueOf(x, y, 1, 1));
        list.add(GRect.valueOf(xMaxTrimmed, y, 1, ySpan));
        list.add(GRect.valueOf(x, yMaxTrimmed, xSpan, 1));
        list.add(GRect.valueOf(xMaxTrimmed, yMaxTrimmed, 1, 1));
        
        final boolean mustOverlap = true;
        addSidishAndCornerishInto(rect, mustOverlap, list);
        
        // Possibly overflowing rectangles.
        list.add(GRect.valueOf(x, y, MAX, ySpan));
        list.add(GRect.valueOf(x, y, xSpan, MAX));
        list.add(GRect.valueOf(x, y, MAX, MAX));

        return list;
    }
    
    /**
     * @param rect Must not be empty.
     */
    private static List<GRect> newRectsNotOverlapping(GRect rect) {
        if (rect.isEmpty()) {
            throw new IllegalArgumentException("" + rect);
        }
        
        final List<GRect> list = new ArrayList<GRect>();
        
        final boolean mustOverlap = false;
        addSidishAndCornerishInto(rect, mustOverlap, list);

        return list;
    }
    
    private static void addSidishAndCornerishInto(
            GRect rect,
            boolean mustOverlap,
            List<GRect> list) {
        
        final int x = rect.x();
        final int y = rect.y();
        final int xSpan = rect.xSpan();
        final int ySpan = rect.ySpan();
        final int xMaxTrimmed = (rect.doesOverflowInX() ? MAX : rect.xMax());
        final int yMaxTrimmed = (rect.doesOverflowInY() ? MAX : rect.yMax());
        
        // Max coord bonus.
        final int mcb = (mustOverlap ? 0 : 1);
        // Small span.
        final int ssp = (mustOverlap ? 2 : 1);
        
        // Sides if must overlap,
        // and next to them one step outside.
        if (x > MIN) {
            list.add(GRect.valueOf(x-1, y, ssp, ySpan));
        }
        if (y > MIN) {
            list.add(GRect.valueOf(x, y-1, xSpan, ssp));
        }
        if (xMaxTrimmed < MAX) {
            list.add(GRect.valueOf(xMaxTrimmed+mcb, y, ssp, ySpan));
        }
        if (yMaxTrimmed < MAX) {
            list.add(GRect.valueOf(x, yMaxTrimmed+mcb, xSpan, ssp));
        }

        // Top-left if must overlap,
        // and next to it one step outside.
        if (x > MIN) {
            list.add(GRect.valueOf(x-1, y, ssp, 1));
        }
        if (y > MIN) {
            list.add(GRect.valueOf(x, y-1, 1, ssp));
        }
        if ((x > MIN) && (y > MIN)) {
            list.add(GRect.valueOf(x-1, y-1, ssp, ssp));
        }

        // Top-Right if must overlap,
        // and next to it one step outside.
        if (xMaxTrimmed < MAX) {
            list.add(GRect.valueOf(xMaxTrimmed+mcb, y, ssp, 1));
        }
        if (y > MIN) {
            list.add(GRect.valueOf(xMaxTrimmed+mcb, y-1, 1, ssp));
        }
        if ((xMaxTrimmed < MAX) && (y > MIN)) {
            list.add(GRect.valueOf(xMaxTrimmed+mcb, y-1, ssp, ssp));
        }

        // Bottom-left if must overlap,
        // and next to it one step outside.
        if (x > MIN) {
            list.add(GRect.valueOf(x-1, yMaxTrimmed, ssp, 1));
        }
        if (yMaxTrimmed < MAX) {
            list.add(GRect.valueOf(x, yMaxTrimmed+mcb, 1, ssp));
        }
        if ((x > MIN) && (yMaxTrimmed < MAX)) {
            list.add(GRect.valueOf(x-1, yMaxTrimmed+mcb, ssp, ssp));
        }

        // Bottom-right if must overlap,
        // and next to it one step outside.
        if (xMaxTrimmed < MAX) {
            list.add(GRect.valueOf(xMaxTrimmed+mcb, yMaxTrimmed, ssp, 1));
        }
        if (yMaxTrimmed < MAX) {
            list.add(GRect.valueOf(xMaxTrimmed, yMaxTrimmed+mcb, 1, ssp));
        }
        if ((xMaxTrimmed < MAX) && (yMaxTrimmed < MAX)) {
            list.add(GRect.valueOf(xMaxTrimmed+mcb, yMaxTrimmed+mcb, ssp, ssp));
        }
    }
}
