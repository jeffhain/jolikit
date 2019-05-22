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
package net.jolikit.bwd.impl.utils.fonts;

import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontMetrics;
import junit.framework.TestCase;

/**
 * To test AbstractBwdFontMetrics.
 */
public class BwdFontMetricsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyMetrics extends AbstractBwdFontMetrics {
        public MyMetrics() {
        }
        @Override
        protected int computeCharWidth_noCache(int codePoint, String cpText) {
            return 0;
        }
        @Override
        protected int computeTextWidth_twoOrMoreCp(String text) {
            return 0;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final MyMetrics metrics = new MyMetrics();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_initialize_final_exceptions() {
        try {
            this.metrics.initialize_final(Double.NaN, 0.0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        
        try {
            this.metrics.initialize_final(0.0, Double.NaN);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
    
    public void test_initialize_final_normalCases() {
        
        // Height at least 1.
        // Prefer ascent.
        testIt(
                0.0, 0.0,
                1, 0, 1);
        
        testIt(
                0.1, 0.0,
                1, 0, 1);
        
        testIt(
                0.0, 0.1,
                0, 1, 1);
        
        // Same ascent and descent: prefer ascent.
        testIt(
                0.1, 0.1,
                1, 0, 1);
        testIt(
                2.5, 2.5,
                3, 2, 5);
        testIt(
                3.5, 3.5,
                4, 3, 7);
        
        // Integers.
        testIt(
                7, 5,
                7, 5, 12);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void testIt(
            double backingAscent,
            double backingDescent,
            int expectedAscent,
            int expectedDescent,
            int expectedFontHeight) {
        /*
         * Checking that input signs doesn't count (using abs).
         */
        for (double s1 : new double[]{-1.0, 1.0}) {
            for (double s2 : new double[]{-1.0, 1.0}) {
                final double a = s1 * backingAscent;
                final double d = s2 * backingDescent;
                if (DEBUG) {
                    System.out.println("backingAscent = " + a);
                    System.out.println("backingDescent = " + d);
                }
                metrics.initialize_final(a, d);
                if (DEBUG) {
                    System.out.println("metrics = " + metrics);
                }
                assertEquals(expectedAscent, metrics.fontAscent());
                assertEquals(expectedDescent, metrics.fontDescent());
                assertEquals(expectedFontHeight, metrics.fontHeight());
            }
        }
    }
}
