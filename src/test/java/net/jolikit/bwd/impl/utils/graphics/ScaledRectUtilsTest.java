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
package net.jolikit.bwd.impl.utils.graphics;

import java.util.Random;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.test.utils.NbrsTestUtils;
import net.jolikit.test.utils.TestUtils;

public class ScaledRectUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final int NBR_OF_CASES = 100 * 1000;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Random random = TestUtils.newRandom123456789L();
    
    private final NbrsTestUtils ntu = new NbrsTestUtils(this.random);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_computeNewPeerRect_3GRect_sturdiness() {
        // oldRect empty.
        try {
            ScaledRectUtils.computeNewPeerRect(
                    GRect.valueOf(0, 0, 1, 0),
                    GRect.valueOf(0, 0, 1, 1),
                    GRect.valueOf(0, 0, 1, 1));
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        // newRect empty.
        try {
            ScaledRectUtils.computeNewPeerRect(
                    GRect.valueOf(0, 0, 1, 1),
                    GRect.valueOf(0, 0, 1, 0),
                    GRect.valueOf(0, 0, 1, 1));
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        // newRect not in oldRect.
        try {
            ScaledRectUtils.computeNewPeerRect(
                    GRect.valueOf(0, 0, 1, 1),
                    GRect.valueOf(0, 0, 2, 1),
                    GRect.valueOf(0, 0, 1, 1));
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        for (int i = 0; i < NBR_OF_CASES; i++) {
            final GRect oldRect = ensureNotEmpty(newRandomRect());
            final GRect newRect = ensureNotEmpty(newRandomShrinkedRect(oldRect));
            final GRect oldPeerRect = newRandomRect();
            
            if (DEBUG) {
                System.out.println();
                System.out.println("oldRect = " + oldRect);
                System.out.println("oldRect = " + toStringRect(oldRect));
                System.out.println("newRect = " + newRect);
                System.out.println("newRect = " + toStringRect(newRect));
                System.out.println("oldPeerRect = " + oldPeerRect);
                System.out.println("oldPeerRect = " + toStringRect(oldPeerRect));
            }
            
            /*
             * 
             */
            
            final GRect newPeerRect = ScaledRectUtils.computeNewPeerRect(
                    oldRect,
                    newRect,
                    oldPeerRect);

            if (DEBUG) {
                System.out.println("newPeerRect = " + newPeerRect);
                System.out.println("newPeerRect = " + toStringRect(newPeerRect));
            }
            
            /*
             * Specific checks.
             */
            
            final boolean expectedXSpanZeroized = (oldRect.xSpan() != 0) && (newRect.xSpan() == 0);
            final boolean expectedYSpanZeroized = (oldRect.ySpan() != 0) && (newRect.ySpan() == 0);
            if (expectedXSpanZeroized) {
                assertEquals(0, newPeerRect.xSpan());
            }
            if (expectedYSpanZeroized) {
                assertEquals(0, newPeerRect.ySpan());
            }
            
            final boolean expectedXMaxBackwrap = (expectedXSpanZeroized && (newPeerRect.x() == Integer.MIN_VALUE));
            final boolean expectedYMaxBackwrap = (expectedYSpanZeroized && (newPeerRect.y() == Integer.MIN_VALUE));
            if (expectedXMaxBackwrap || expectedYMaxBackwrap) {
                assertTrue((newPeerRect.x() >= oldPeerRect.x()));
                if (expectedXMaxBackwrap) {
                    assertEquals(Integer.MAX_VALUE, newPeerRect.xMax());
                } else {
                    assertTrue((newPeerRect.xMax() <= oldPeerRect.xMax()));
                }
                
                assertTrue((newPeerRect.y() >= oldPeerRect.y()));
                if (expectedYMaxBackwrap) {
                    assertEquals(Integer.MAX_VALUE, newPeerRect.yMax());
                } else {
                    assertTrue((newPeerRect.yMax() <= oldPeerRect.yMax()));
                }
            } else {
                assertTrue(oldPeerRect.containsCoordinatesOf(newPeerRect));
            }
            
            /*
             * Ratio checks.
             */
            
            {
                final double oldSpan = (double) oldRect.xSpan();
                final double oldPeerSpan = (double) oldPeerRect.xSpan();
                final double minOldSpan = Math.min(oldSpan, oldPeerSpan);
                if (minOldSpan != 0.0) {
                    {
                        final double ratio = (newRect.x() - oldRect.x()) / oldSpan;
                        final double peerRatio = (newPeerRect.x() - oldPeerRect.x()) / oldPeerSpan;
                        final double epsilon = (1.0 / oldSpan + 1.0 / oldPeerSpan);
                        assertEquals(ratio, peerRatio, epsilon);
                    }
                    {
                        final double ratio = (newRect.xMaxLong() - oldRect.xMaxLong()) / oldSpan;
                        final double peerRatio = (newPeerRect.xMaxLong() - oldPeerRect.xMaxLong()) / oldPeerSpan;
                        final double epsilon = (1.0 / oldSpan + 1.0 / oldPeerSpan);
                        assertEquals(ratio, peerRatio, epsilon);
                    }
                }
            }
            {
                final double oldSpan = (double) oldRect.ySpan();
                final double oldPeerSpan = (double) oldPeerRect.ySpan();
                final double minOldSpan = Math.min(oldSpan, oldPeerSpan);
                if (minOldSpan != 0.0) {
                    {
                        final double ratio = (newRect.y() - oldRect.y()) / oldSpan;
                        final double peerRatio = (newPeerRect.y() - oldPeerRect.y()) / oldPeerSpan;
                        final double epsilon = (1.0 / oldSpan + 1.0 / oldPeerSpan);
                        assertEquals(ratio, peerRatio, epsilon);
                    }
                    {
                        final double ratio = (newRect.yMaxLong() - oldRect.yMaxLong()) / oldSpan;
                        final double peerRatio = (newPeerRect.yMaxLong() - oldPeerRect.yMaxLong()) / oldPeerSpan;
                        final double epsilon = (1.0 / oldSpan + 1.0 / oldPeerSpan);
                        assertEquals(ratio, peerRatio, epsilon);
                    }
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static String toStringRect(GRect rect) {
        final StringBuilder sb = new StringBuilder();
        
        sb.append(rect);
        
        final String xMaxStr = (rect.xSpan() != 0) ? "" + rect.xMaxLong() : "(none)";
        final String yMaxStr = (rect.ySpan() != 0) ? "" + rect.yMaxLong() : "(none)";
        sb.append(" (xMax = " + xMaxStr + ", yMax = " + yMaxStr + ")");
        
        final double intSpan = (Integer.MAX_VALUE - (double) Integer.MIN_VALUE);
        final double xRatio = (rect.x() - (double) Integer.MIN_VALUE) / intSpan;
        final double yRatio = (rect.y() - (double) Integer.MIN_VALUE) / intSpan;
        
        final double xMaxRatio = (rect.xMaxLong() - (double) Integer.MIN_VALUE) / intSpan;
        final double yMaxRatio = (rect.yMaxLong() - (double) Integer.MIN_VALUE) / intSpan;
        sb.append("\n(xRatio : " + xRatio + " .. " + xMaxRatio);
        sb.append(", yRatio : " + yRatio + " .. " + yMaxRatio + ")");
        
        sb.append("\n(xSpanRatio = " + (xMaxRatio - xRatio));
        sb.append(", ySpanRatio = " + (yMaxRatio - yRatio));
        
        return sb.toString();
    }
    
    /*
     * 
     */

    private GRect newRandomRect() {
        final int x = this.ntu.randomIntWhatever();
        final int y = this.ntu.randomIntWhatever();
        int xSpan = this.ntu.randomIntWhatever() & Integer.MAX_VALUE;
        int ySpan = this.ntu.randomIntWhatever() & Integer.MAX_VALUE;
        xSpan = computeBoundedSpan(x, xSpan);
        ySpan = computeBoundedSpan(y, ySpan);
        final GRect rect = GRect.valueOf(x, y, xSpan, ySpan);
        return rect;
    }

    private GRect newRandomShrinkedRect(GRect oldRect) {
        
        /*
         * -1 to make sure dxMin and dyMin are not equal to spans,
         * else that would make the rectangle empty,
         * and non-empty ensuring would make it leak out of oldRect.
         */
        
        final int dxMin = this.random_0_max(oldRect.xSpan() - 1);
        final int dxMax = -this.random_0_max(oldRect.xSpan() - dxMin);

        final int dyMin = this.random_0_max(oldRect.ySpan() - 1);
        final int dyMax = -this.random_0_max(oldRect.ySpan() - dyMin);

        final GRect shrunk = oldRect.withBordersDeltas(dxMin, dyMin, dxMax, dyMax);
        return shrunk;
    }
    
    /**
     * @return The specified rectangle if not empty, else
     *         one with spans of 1 where the specified rectangle
     *         has spans of 0.
     */
    private static GRect ensureNotEmpty(GRect rect) {
        if (rect.xSpan() == 0) {
            rect = rect.withXSpan(1);
        }
        if (rect.ySpan() == 0) {
            rect = rect.withYSpan(1);
        }
        return rect;
    }
    
    private int random_0_or_1() {
        return (this.random.nextBoolean() ? 1 : 0);
    }
    
    /**
     * @param max Must be >= 0.
     * @return A uniform random value in [0,max].
     */
    private int random_0_max(int max) {
        if (max == 0) {
            return 0;
        } else {
            return this.random_0_or_1() + this.random.nextInt(max);
        }
    }
    
    /**
     * @param start Any value.
     * @param span Some value >= 0.
     * @return The span closest to the specified one, so that
     *         start + span - 1 doesn't positively overflow.
     */
    private static int computeBoundedSpan(int start, int span) {
        final boolean posOverflow = (start + (long) span - 1 > Integer.MAX_VALUE);
        if (posOverflow) {
            span = Integer.MAX_VALUE - start;
        }
        return span;
    }
}
