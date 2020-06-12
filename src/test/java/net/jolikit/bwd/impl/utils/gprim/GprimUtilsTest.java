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
package net.jolikit.bwd.impl.utils.gprim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.jolikit.bwd.api.graphics.GRect;
import junit.framework.TestCase;

public class GprimUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public GprimUtilsTest() {
    }
    
    /*
     * 
     */
    
    public void test_normalize_neg180_180_double() {
        final double before180 = Math.nextAfter(180.0, Double.NEGATIVE_INFINITY);
        
        assertEquals(-180.0, GprimUtils.normalize_neg180_180(-180.0));
        assertEquals(-before180, GprimUtils.normalize_neg180_180(-before180));
        assertEquals(before180, GprimUtils.normalize_neg180_180(before180));
        assertEquals(-180.0, GprimUtils.normalize_neg180_180(180.0));
        
        assertEquals(178.0, GprimUtils.normalize_neg180_180(-182.0));
        assertEquals(-178.0, GprimUtils.normalize_neg180_180(182.0));
        
        assertEquals(178.0, GprimUtils.normalize_neg180_180(-182.0 - 360.0));
        assertEquals(-178.0, GprimUtils.normalize_neg180_180(182.0 + 360.0));
        
        assertEquals(178.0, GprimUtils.normalize_neg180_180(-182.0 - 360.0 * 2));
        assertEquals(-178.0, GprimUtils.normalize_neg180_180(182.0 + 360.0 * 2));
        
        /*
         * 
         */
        
        assertEquals("-0.0", "" + GprimUtils.normalize_neg180_180(-0.0));
        
        /*
         * 
         */
        
        final long randomSeed = 123456789L;
        final Random random = new Random(randomSeed);
        final int nbrOfCalls = 1000;
        for (int i = 0; i < nbrOfCalls; i++) {
            final double angDeg = (1.0 - 2.0 * random.nextDouble()) * 10000.0;
            final double resDeg = GprimUtils.normalize_neg180_180(angDeg);
            if (!((resDeg >= -180.0) && (resDeg < 180.0))) {
                throw new AssertionError("angDeg = " + angDeg + ", resDeg = " + resDeg);
            }
        }
    }
    
    public void test_normalize_0_360_double() {
        final double after0 = Double.MIN_VALUE;
        final double before360 = Math.nextAfter(360.0, Double.NEGATIVE_INFINITY);
        
        assertEquals(0.0, GprimUtils.normalize_0_360(0.0));
        assertEquals(after0, GprimUtils.normalize_0_360(after0));
        assertEquals(before360, GprimUtils.normalize_0_360(before360));
        assertEquals(0.0, GprimUtils.normalize_0_360(360.0));
        
        assertEquals(358.0, GprimUtils.normalize_0_360(-2.0));
        assertEquals(2.0, GprimUtils.normalize_0_360(362.0));
        
        assertEquals(358.0, GprimUtils.normalize_0_360(-2.0 - 360.0));
        assertEquals(2.0, GprimUtils.normalize_0_360(362.0 + 360.0));
        
        assertEquals(358.0, GprimUtils.normalize_0_360(-2.0 - 360.0 * 2));
        assertEquals(2.0, GprimUtils.normalize_0_360(362.0 + 360.0 * 2));
        
        /*
         * 
         */
        
        assertEquals("-0.0", "" + GprimUtils.normalize_0_360(-0.0));
        
        /*
         * 
         */
        
        final long randomSeed = 123456789L;
        final Random random = new Random(randomSeed);
        final int nbrOfCalls = 1000;
        for (int i = 0; i < nbrOfCalls; i++) {
            final double angDeg = (1.0 - 2.0 * random.nextDouble()) * 10000.0;
            final double resDeg = GprimUtils.normalize_0_360(angDeg);
            if (!((resDeg >= 0.0) && (resDeg < 360.0))) {
                throw new AssertionError("angDeg = " + angDeg + ", resDeg = " + resDeg);
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_computeNormalizedStartDeg_double() {
        {
            final double before360 = Math.nextAfter(360.0, 0.0);
            final double ulp = 360.0 - before360;
            
            assertEquals(before360, GprimUtils.computeNormalizedStartDeg(-ulp));
            // Making sure that, when negative arg is too small
            // to correspond to the double before 360
            // When removing a part too small to be the
            assertEquals(0.0, GprimUtils.computeNormalizedStartDeg(-ulp * 0.5));
        }
        
        for (int k = -3; k <= 3; k++) {
            for (double base : new double[]{0.0, 1.0, 359.0}) {
                final double val = base + k * 360.0;
                // +-0.0 not specified, adding 0.0 to get rid of -0.0.
                assertEquals(base, 0.0 + GprimUtils.computeNormalizedStartDeg(val));
            }
        }
    }
    
    public void test_computeClampedSpanDeg_double() {
        assertEquals(-360.0, GprimUtils.computeClampedSpanDeg(-1000.0));
        for (double val : new double[]{-360.0, -359.0, -1.0, 0.0, 1.0, 359.0, 360.0}) {
            assertEquals(val, GprimUtils.computeClampedSpanDeg(val));
        }
        assertEquals(360.0, GprimUtils.computeClampedSpanDeg(1000.0));
    }
    
    public void test_computeReworkedStartDeg_2double_computeReworkedSpanDeg_double() {
        // Too far for 360 magnet.
        {
            double clampedSpanDeg = 360.0 - Math.toDegrees(2.0 / Integer.MAX_VALUE);
            assertEquals(clampedSpanDeg, GprimUtils.computeReworkedSpanDeg(clampedSpanDeg));
            assertEquals(clampedSpanDeg, GprimUtils.computeReworkedSpanDeg(-clampedSpanDeg));
        }
        // Close enough for 360 magnet.
        {
            double clampedSpanDeg = 360.0 - Math.toDegrees(0.5 / Integer.MAX_VALUE);
            assertEquals(360.0, GprimUtils.computeReworkedSpanDeg(clampedSpanDeg));
            assertEquals(360.0, GprimUtils.computeReworkedSpanDeg(-clampedSpanDeg));
        }
        
        // Non-zero not tiny: too far for magnet to non-tiny.
        {
            double clampedSpanDeg = Math.toDegrees(2.0 / Integer.MAX_VALUE);
            assertEquals(clampedSpanDeg, GprimUtils.computeReworkedSpanDeg(clampedSpanDeg));
            assertEquals(clampedSpanDeg, GprimUtils.computeReworkedSpanDeg(-clampedSpanDeg));
        }
        // Non-zero tiny: magnet to non-tiny.
        {
            double clampedSpanDeg = Math.toDegrees(0.5 / Integer.MAX_VALUE);
            assertEquals(GprimUtils.SPAN_TOL_DEG, GprimUtils.computeReworkedSpanDeg(clampedSpanDeg));
            assertEquals(GprimUtils.SPAN_TOL_DEG, GprimUtils.computeReworkedSpanDeg(-clampedSpanDeg));
        }
        
        // {normalizedStart, clampedSpan, reworkedStart, reworkedSpan}
        for (double[] startSpanArr : new double[][]{
                // Tiny-to-non-tiny: positive span.
                {0.0, Double.MIN_VALUE, 0.0, GprimUtils.SPAN_TOL_DEG},
                // Tiny-to-non-tiny: negative span.
                {0.0, -Double.MIN_VALUE, 360.0 - GprimUtils.SPAN_TOL_DEG, GprimUtils.SPAN_TOL_DEG},
                //
                {0.0, 360.0, 0.0, 360.0},
                {10.0, 360.0, 10.0, 360.0},
                {10.0, 350.0, 10.0, 350.0},
                //
                {0.0, -0.0, 0.0, 0.0}, // abs
                //
                {0.0, -100.0, 260.0, 100.0},
                {10.0, -100.0, 270.0, 100.0},
                {0.0, -359.0, 1.0, 359.0},
                {0.0, -360.0, 0.0, 360.0},
                {-10.0, -350.0, 0.0, 350.0},
        }) {
            final double normalizedStartDeg = startSpanArr[0];
            final double clampedSpanDeg = startSpanArr[1];
            final double expectedReworkedStartDeg = startSpanArr[2];
            final double expectedReworkedSpanDeg = startSpanArr[3];
            assertEquals(expectedReworkedStartDeg, GprimUtils.computeReworkedStartDeg(normalizedStartDeg, clampedSpanDeg));
            // +-0.0 not specified, adding 0.0 to get rid of -0.0.
            assertEquals(expectedReworkedSpanDeg, 0.0 + GprimUtils.computeReworkedSpanDeg(clampedSpanDeg));
        }
    }
    
    /*
     * 
     */
    
    public void test_isCw_isCcw_4double() {
        
        final double eps = 0.1;
        
        // {v1x, v1y, v2x, v2y}
        for (double[] tiesArr : new double[][]{
                {2.0, 1.0, 4.0, 2.0},
                {2.0, 1.0, -4.0, -2.0},
        }) {
            final double v1x = tiesArr[0];
            final double v1y = tiesArr[1];
            final double v2x = tiesArr[2];
            final double v2y = tiesArr[3];
            // Ties: always true.
            assertTrue(GprimUtils.isCw(v1x, v1y, v2x, v2y));
            assertTrue(GprimUtils.isCcw(v1x, v1y, v2x, v2y));
        }
        
        // {v1x, v1y, v2x, v2y}
        for (double[] cwArr : new double[][]{
                {2.0, 1.0, 4.0, 2.0 + eps},
                {2.0, 1.0, -2.0, 4.0},
                {2.0, 1.0, -4.0, -2.0 + eps},
        }) {
            final double v1x = cwArr[0];
            final double v1y = cwArr[1];
            final double v2x = cwArr[2];
            final double v2y = cwArr[3];
            assertTrue(GprimUtils.isCw(v1x, v1y, v2x, v2y));
            assertFalse(GprimUtils.isCcw(v1x, v1y, v2x, v2y));
        }
        
        // {v1x, v1y, v2x, v2y}
        for (double[] ccwArr : new double[][]{
                {2.0, 1.0, -4.0, -2.0 - eps},
                {2.0, 1.0, 2.0, -4.0},
                {2.0, 1.0, 4.0, 2.0 - eps},
        }) {
            final double v1x = ccwArr[0];
            final double v1y = ccwArr[1];
            final double v2x = ccwArr[2];
            final double v2y = ccwArr[3];
            assertFalse(GprimUtils.isCw(v1x, v1y, v2x, v2y));
            assertTrue(GprimUtils.isCcw(v1x, v1y, v2x, v2y));
        }
    }

    public void test_isInAngularRange_5double() {
        
        // {startDeg, spanDeg, angDeg}
        for (double[] tiesArr : new double[][]{
                {0.0, 0.0, 0.0},
                {10.0, 0.0, 10.0},
        }) {
            final double startDeg = tiesArr[0];
            final double spanDeg = tiesArr[1];
            final double angDeg = tiesArr[2];
            final double dx = cosDeg(angDeg);
            final double dy = -sinDeg(angDeg);
            final double v1x = cosDeg(startDeg);
            final double v1y = -sinDeg(startDeg);
            final double v2x = cosDeg(startDeg + spanDeg);
            final double v2y = -sinDeg(startDeg + spanDeg);
            // Ties: always true.
            assertTrue(GprimUtils.isInAngularRange(dx, dy, spanDeg, v1x, v1y, v2x, v2y));
        }
        
        // Offset to avoid special case for start angle.
        final double o = 10.0;
        
        // {startDeg, spanDeg, angDeg}
        for (double[] inArr : new double[][]{
                {o, 179.0, o + 0.0},
                {o, 179.0, o + 1.0},
                {o, 179.0, o + 179.0},
                {o, 180.0, o + 0.0},
                {o, 180.0, o + 1.0},
                {o, 180.0, o + 179.0},
                {o, 180.0, o + 180.0},
                {o, 181.0, o + 0.0},
                {o, 181.0, o + 1.0},
                {o, 181.0, o + 180.0},
                {o, 181.0, o + 181.0},
                //
                {o, 360.0, o + 0.0},
                {o, 360.0, o + 1.0},
                {o, 360.0, o + 179.0},
                {o, 360.0, o + 180.0},
                {o, 360.0, o + 181.0},
                {o, 360.0, o + 359.0},
                {o, 360.0, o + 360.0},
        }) {
            final double startDeg = inArr[0];
            final double spanDeg = inArr[1];
            final double angDeg = inArr[2];
            final double dx = cosDeg(angDeg);
            final double dy = -sinDeg(angDeg);
            final double v1x = cosDeg(startDeg);
            final double v1y = -sinDeg(startDeg);
            final double v2x = cosDeg(startDeg + spanDeg);
            final double v2y = -sinDeg(startDeg + spanDeg);
            assertTrue(GprimUtils.isInAngularRange(dx, dy, spanDeg, v1x, v1y, v2x, v2y));
        }
        
        // {startDeg, spanDeg, angDeg}
        for (double[] outArr : new double[][]{
                {o, 179.0, o - 1.0},
                {o, 179.0, o + 180},
                {o, 180.0, o - 1.0},
                {o, 180.0, o + 181.0},
                {o, 181.0, o - 1.0},
                {o, 181.0, o + 182.0},
                //
                {o, 359.0, o - 0.5},
                {o, 359.0, o + 359.5},
        }) {
            final double startDeg = outArr[0];
            final double spanDeg = outArr[1];
            final double angDeg = outArr[2];
            final double dx = cosDeg(angDeg);
            final double dy = -sinDeg(angDeg);
            final double v1x = cosDeg(startDeg);
            final double v1y = -sinDeg(startDeg);
            final double v2x = cosDeg(startDeg + spanDeg);
            final double v2y = -sinDeg(startDeg + spanDeg);
            assertFalse(GprimUtils.isInAngularRange(dx, dy, spanDeg, v1x, v1y, v2x, v2y));
        }
    }
    
    /*
     * 
     */
    
    public void test_sinDeg_double() {
        assertEquals(-0.0, GprimUtils.sinDeg(-0.0));
        
        for (int k = -10; k <= 10; k++) {
            if (k < 0) {
                assertEquals(-0.0, GprimUtils.sinDeg(180.0 * k));
            } else {
                assertEquals(0.0, GprimUtils.sinDeg(180.0 * k));
            }
            assertEquals(1.0, GprimUtils.sinDeg(90.0 + 360.0 * k));
            assertEquals(-1.0, GprimUtils.sinDeg(-90.0 + 360.0 * k));
        }
        
        final Random random = new Random(123456789L);
        for (int i = 0; i < 10000; i++) {
            final double angDeg = 1000.0 * (1.0 - 2.0 * random.nextDouble());
            final double ref = Math.sin(Math.toRadians(angDeg));
            final double res = GprimUtils.sinDeg(angDeg);
            assertEquals(ref, res, 1e-14);
        }
    }
    
    public void test_cosDeg_double() {
        for (int k = -10; k <= 10; k++) {
            assertEquals(0.0, GprimUtils.cosDeg(90.0 + 180.0 * k));
            assertEquals(1.0, GprimUtils.cosDeg(360.0 * k));
            assertEquals(-1.0, GprimUtils.cosDeg(180.0 + 360.0 * k));
        }
        
        final Random random = new Random(123456789L);
        for (int i = 0; i < 10000; i++) {
            final double angDeg = 1000.0 * (1.0 - 2.0 * random.nextDouble());
            final double ref = Math.cos(Math.toRadians(angDeg));
            final double res = GprimUtils.cosDeg(angDeg);
            assertEquals(ref, res, 1e-14);
        }
    }
    
    /*
     * 
     */
    
    public void test_pixelNumPlusLongAmount_2int_long() {
        
        /*
         * factor 1
         */

        {
            for (int pixelNum = 0; pixelNum < (1 << 16); pixelNum++) {
                assertEquals(pixelNum, GprimUtils.pixelNumPlusLongAmountNormalized(1, pixelNum, 0));
            }
            assertEquals(0, GprimUtils.pixelNumPlusLongAmountNormalized(1, (1 << 16), 0));
            
            assertEquals(5, GprimUtils.pixelNumPlusLongAmountNormalized(1, 2, 3));
            assertEquals((1 << 16)-1, GprimUtils.pixelNumPlusLongAmountNormalized(1, (1 << 16)-1 - 3, 3));
            assertEquals(0, GprimUtils.pixelNumPlusLongAmountNormalized(1, (1 << 16)-1 - 2, 3));
            assertEquals(1, GprimUtils.pixelNumPlusLongAmountNormalized(1, (1 << 16)-1 - 1, 3));
            assertEquals(2, GprimUtils.pixelNumPlusLongAmountNormalized(1, (1 << 16)-1, 3));
            assertEquals(3, GprimUtils.pixelNumPlusLongAmountNormalized(1, (1 << 16), 3));
        }
        
        /*
         * factor 2
         */
        
        {
            for (int pixelNum = 0; pixelNum < (1 << 17); pixelNum++) {
                assertEquals(pixelNum, GprimUtils.pixelNumPlusLongAmountNormalized(2, pixelNum, 0));
            }
            assertEquals(0, GprimUtils.pixelNumPlusLongAmountNormalized(2, (1 << 17), 0));
            
            assertEquals(5, GprimUtils.pixelNumPlusLongAmountNormalized(2, 2, 3));
            assertEquals((1 << 17)-1, GprimUtils.pixelNumPlusLongAmountNormalized(2, (1 << 17)-1 - 3, 3));
            assertEquals(0, GprimUtils.pixelNumPlusLongAmountNormalized(2, (1 << 17)-1 - 2, 3));
            assertEquals(1, GprimUtils.pixelNumPlusLongAmountNormalized(2, (1 << 17)-1 - 1, 3));
            assertEquals(2, GprimUtils.pixelNumPlusLongAmountNormalized(2, (1 << 17)-1, 3));
            assertEquals(3, GprimUtils.pixelNumPlusLongAmountNormalized(2, (1 << 17), 3));
        }
    }
    
    /*
     * 
     */
    
    public void test_mustDrawOvalOrArc_GRect_4int() {
        
        // Large enough for smooth oval.
        final GRect oval = GRect.valueOf(0, 0, 100, 200);
        
        for (GRect goodClip : newRectList_overlappingOval(oval)) {
            if (DEBUG) {
                System.out.println("oval =     " + oval);
                System.out.println("goodClip = " + goodClip);
            }
            assertTrue(GprimUtils.mustDrawOvalOrArc(
                    goodClip,
                    oval.x(), oval.y(), oval.xSpan(), oval.ySpan()));
        }
        
        for (List<GRect> badClipList : new List[]{
                newRectList_inOval(oval),
                newRectList_outOfOval(oval),
        }) {
            for (GRect badClip : badClipList) {
                if (DEBUG) {
                    System.out.println("oval =    " + oval);
                    System.out.println("badClip = " + badClip);
                }
                assertFalse(GprimUtils.mustDrawOvalOrArc(
                        badClip,
                        oval.x(), oval.y(), oval.xSpan(), oval.ySpan()));
            }
        }
    }
    
    public void test_mustFillOvalOrArc_GRect_4int() {
        
        // Large enough for smooth oval.
        final GRect oval = GRect.valueOf(0, 0, 100, 200);
        
        for (List<GRect> goodClipList : new List[]{
                newRectList_inOval(oval),
                newRectList_overlappingOval(oval),
        }) {
            for (GRect goodClip : goodClipList) {
                if (DEBUG) {
                    System.out.println("oval =     " + oval);
                    System.out.println("goodClip = " + goodClip);
                }
                assertTrue(GprimUtils.mustFillOvalOrArc(
                        goodClip,
                        oval.x(), oval.y(), oval.xSpan(), oval.ySpan()));
            }
        }

        for (GRect badClip : newRectList_outOfOval(oval)) {
            if (DEBUG) {
                System.out.println("oval =    " + oval);
                System.out.println("badClip = " + badClip);
            }
            assertFalse(GprimUtils.mustFillOvalOrArc(
                    badClip,
                    oval.x(), oval.y(), oval.xSpan(), oval.ySpan()));
        }
    }
    
    public void test_isClipInOval_GRect_4int() {
        
        // Large enough for smooth oval.
        final GRect oval = GRect.valueOf(0, 0, 100, 200);
        
        for (GRect goodClip : newRectList_inOval(oval)) {
            if (DEBUG) {
                System.out.println("oval =     " + oval);
                System.out.println("goodClip = " + goodClip);
            }
            assertTrue(GprimUtils.isClipInOval(
                    goodClip,
                    oval.x(), oval.y(), oval.xSpan(), oval.ySpan()));
        }
        
        for (List<GRect> badClipList : new List[]{
                newRectList_overlappingOval(oval),
                newRectList_outOfOval(oval),
        }) {
            for (GRect badClip : badClipList) {
                if (DEBUG) {
                    System.out.println("oval =    " + oval);
                    System.out.println("badClip = " + badClip);
                }
                assertFalse(GprimUtils.isClipInOval(
                        badClip,
                        oval.x(), oval.y(), oval.xSpan(), oval.ySpan()));
            }
        }
    }

    /*
     * 
     */
    
    public void test_computeIsRectInOval_8int_double() {
        
        final double cosPiO4 = Math.cos(Math.PI/4);
        
        // Margin.
        for (boolean circle : new boolean[]{false,true}) {
            final int xSpan = 1000;
            // Not a circle, but close.
            final int ySpan = xSpan + (circle ? 0 : 1);
            for (boolean expectedIn : new boolean[]{false,true}) {
                final double margin = (expectedIn ? -0.35 : 0.15);
                final double xRadius = xSpan * 0.5;
                final double yRadius = ySpan * 0.5;
                
                final double rectXSpanDouble = xSpan * cosPiO4;
                final double rectYSpanDouble = ySpan * cosPiO4;
                final int rectXSpan = (int) rectXSpanDouble;
                final int rectYSpan = (int) rectYSpanDouble;
                final int rectX = (int) Math.round(xRadius - rectXSpanDouble * 0.5);
                final int rectY = (int) Math.round(yRadius - rectYSpanDouble * 0.5);
                assertEquals(expectedIn, GprimUtils.computeIsRectInOval(
                        0, 0, xSpan, ySpan,
                        rectX, rectY, rectXSpan, rectYSpan,
                        margin));
            }
        }
        
        /*
         * 
         */

        // Large enough for smooth oval.
        final GRect oval = GRect.valueOf(0, 0, 100, 200);
        
        for (GRect inRect : newRectList_inOval(oval)) {
            if (DEBUG) {
                System.out.println("oval =   " + oval);
                System.out.println("inRect = " + inRect);
            }
            final double margin = 0.0;
            assertTrue(GprimUtils.computeIsRectInOval(
                    oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                    inRect.x(), inRect.y(), inRect.xSpan(), inRect.ySpan(),
                    margin));
        }
        
        for (List<GRect> notInRectList : new List[]{
                newRectList_overlappingOval(oval),
                newRectList_outOfOval(oval),
        }) {
            for (GRect notInRect : notInRectList) {
                if (DEBUG) {
                    System.out.println("oval =      " + oval);
                    System.out.println("notInRect = " + notInRect);
                }
                final double margin = 0.0;
                assertFalse(GprimUtils.computeIsRectInOval(
                        oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                        notInRect.x(), notInRect.y(), notInRect.xSpan(), notInRect.ySpan(),
                        margin));
            }
        }
    }

    public void test_computeIsRectOutOfOval_8int_double() {
        
        final double oneMinusCosPiO4 = 1.0 - Math.cos(Math.PI/4);
        
        // Margin.
        for (boolean circle : new boolean[]{false,true}) {
            final int xSpan = 1000;
            // Not a circle, but close.
            final int ySpan = xSpan + (circle ? 0 : 1);
            for (boolean expectedOut : new boolean[]{false,true}) {
                final double margin = (expectedOut ? 0.0 : 1.38);
                final double xRadius = xSpan * 0.5;
                final double yRadius = ySpan * 0.5;
                // Penetration.
                final int xPen = (int) (xRadius * oneMinusCosPiO4);
                final int yPen = (int) (yRadius * oneMinusCosPiO4);
                assertEquals(expectedOut, GprimUtils.computeIsRectOutOfOval(
                        0, 0, xSpan, ySpan,
                        0, 0, xPen, yPen,
                        margin));
                assertEquals(expectedOut, GprimUtils.computeIsRectOutOfOval(
                        0, 0, xSpan, ySpan,
                        xSpan - xPen, 0, xPen, yPen,
                        margin));
                assertEquals(expectedOut, GprimUtils.computeIsRectOutOfOval(
                        0, 0, xSpan, ySpan,
                        0, ySpan - yPen, xPen, yPen,
                        margin));
                assertEquals(expectedOut, GprimUtils.computeIsRectOutOfOval(
                        0, 0, xSpan, ySpan,
                        xSpan - xPen, ySpan - yPen, xPen, yPen,
                        margin));
            }
        }
        
        /*
         * 
         */
        
        final GRect oval = GRect.valueOf(0, 0, 100, 200);
        
        for (GRect outRect : newRectList_outOfOval(oval)) {
            if (DEBUG) {
                System.out.println("oval =    " + oval);
                System.out.println("outRect = " + outRect);
            }
            final double margin = 0.0;
            assertTrue(GprimUtils.computeIsRectOutOfOval(
                    oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                    outRect.x(), outRect.y(), outRect.xSpan(), outRect.ySpan(),
                    margin));
        }
        
        for (List<GRect> notOutRectList : new List[]{
                newRectList_inOval(oval),
                newRectList_overlappingOval(oval),
        }) {
            for (GRect notOutRect : notOutRectList) {
                if (DEBUG) {
                    System.out.println("oval =       " + oval);
                    System.out.println("notOutRect = " + notOutRect);
                }
                final double margin = 0.0;
                assertFalse(GprimUtils.computeIsRectOutOfOval(
                        oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                        notOutRect.x(), notOutRect.y(), notOutRect.xSpan(), notOutRect.ySpan(),
                        margin));
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * Uses slow version as reference.
     */
    public void test_isInAngularRange_rxyPos_fast_9double() {
        
        final int nbrOfCalls = 100 * 1000;
        
        final Random random = new Random(123456789L);
        
        for (int i = 0; i < nbrOfCalls; i++) {
            final double dx = (2.0 * random.nextDouble() - 1.0);
            final double dy = (2.0 * random.nextDouble() - 1.0);
            
            final double startDeg = 360.0 * random.nextDouble();
            final double spanDeg = 360.0 * random.nextDouble();
            
            // 1 - [0,1[ to make sure is > 0.
            final double rx = 1.0 - random.nextDouble();
            final double ry = 1.0 - random.nextDouble();
            
            final double sinStart = Math.sin(Math.toRadians(startDeg));
            final double cosStart = Math.cos(Math.toRadians(startDeg));
            
            final double endDeg = startDeg + spanDeg;
            final double sinEnd = Math.sin(Math.toRadians(endDeg));
            final double cosEnd = Math.cos(Math.toRadians(endDeg));
            
            final boolean expected = GprimUtils.isInAngularRange_rxyPos_slow(
                    dx, dy,
                    startDeg, spanDeg,
                    rx, ry);
            final boolean actual = GprimUtils.isInAngularRange_rxyPos_fast(
                    dx, dy,
                    spanDeg,
                    sinStart, cosStart,
                    sinEnd, cosEnd,
                    rx, ry);
            
            if (actual != expected) {
                System.out.println();
                System.out.println("dx = " + dx);
                System.out.println("dy = " + dy);
                System.out.println("startDeg = " + startDeg);
                System.out.println("spanDeg = " + spanDeg);
                System.out.println("rx = " + rx);
                System.out.println("ry = " + ry);
                throw new AssertionError(actual + " != " + expected);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static double sinDeg(double angDeg) {
        return Math.sin(Math.toRadians(angDeg));
    }
    
    private static double cosDeg(double angDeg) {
        return Math.cos(Math.toRadians(angDeg));
    }
    
    /*
     * 
     */
    
    private static List<GRect> newRectList_inOval(GRect oval) {
        
        final List<GRect> list = new ArrayList<GRect>();
        
        final int xSpan = oval.xSpan();
        final int ySpan = oval.ySpan();
        final int xSpan1Q = (int) (0.25 * xSpan);
        final int ySpan1Q = (int) (0.25 * ySpan);
        
        // Strictly in oval.
        list.add(oval.withBordersDeltas(xSpan1Q, ySpan1Q, -xSpan1Q, -ySpan1Q));

        // Just a bit outside inner diamond, but still strictly in oval.
        list.add(oval.withBordersDeltas(xSpan1Q-1, ySpan1Q-1, -(xSpan1Q+1), -(ySpan1Q+1)));
        list.add(oval.withBordersDeltas(xSpan1Q-3, ySpan1Q-3, -(xSpan1Q+3), -(ySpan1Q+3)));
        
        return list;
    }
    
    private static List<GRect> newRectList_overlappingOval(GRect oval) {
        
        final List<GRect> list = new ArrayList<GRect>();
        
        final int xSpan = oval.xSpan();
        final int ySpan = oval.ySpan();
        final int xSpan1Q = (int) (0.25 * xSpan);
        final int ySpan1Q = (int) (0.25 * ySpan);
        final int xSpan3Q = (int) (0.75 * xSpan);
        final int ySpan3Q = (int) (0.75 * ySpan);
        
        list.add(oval);
        list.add(oval.withBordersDeltas(1, 1, -1, -1));
        list.add(oval.withBordersDeltas(-1, -1, 1, 1));
        
        list.add(oval.withPosDeltas(-xSpan1Q, -ySpan1Q));
        list.add(oval.withPosDeltas(-xSpan1Q, ySpan1Q));
        list.add(oval.withPosDeltas(xSpan1Q, -ySpan1Q));
        list.add(oval.withPosDeltas(xSpan1Q, ySpan1Q));
        
        list.add(oval.withPosDeltas(-xSpan3Q, -ySpan3Q));
        list.add(oval.withPosDeltas(-xSpan3Q, ySpan3Q));
        list.add(oval.withPosDeltas(xSpan3Q, -ySpan3Q));
        list.add(oval.withPosDeltas(xSpan3Q, ySpan3Q));
        
        // Edges overlap.
        list.add(oval.withPosDeltas(-(xSpan-1), 0));
        list.add(oval.withPosDeltas(xSpan-1, 0));
        list.add(oval.withPosDeltas(0, -(ySpan-1)));
        list.add(oval.withPosDeltas(0, ySpan-1));
        
        return list;
    }
    
    private static List<GRect> newRectList_outOfOval(GRect oval) {
        
        final List<GRect> list = new ArrayList<GRect>();
        
        final int xSpan = oval.xSpan();
        final int ySpan = oval.ySpan();
        
        // Only a bit more than corners overlap.
        list.add(oval.withPosDeltas(-(xSpan-1), -(ySpan-1)));
        list.add(oval.withPosDeltas(-(xSpan-1), ySpan-1));
        list.add(oval.withPosDeltas(xSpan-1, -(ySpan-1)));
        list.add(oval.withPosDeltas(xSpan-1, ySpan-1));
        
        // Only corners overlap.
        list.add(oval.withPosDeltas(-xSpan, -ySpan));
        list.add(oval.withPosDeltas(-xSpan, ySpan));
        list.add(oval.withPosDeltas(xSpan, -ySpan));
        list.add(oval.withPosDeltas(xSpan, ySpan));
        
        // A bit further out than corners overlap.
        list.add(oval.withPosDeltas(-(xSpan+1), -(ySpan+1)));
        list.add(oval.withPosDeltas(-(xSpan+1), ySpan+1));
        list.add(oval.withPosDeltas(xSpan+1, -(ySpan+1)));
        list.add(oval.withPosDeltas(xSpan+1, ySpan+1));

        // Edges just touching.
        list.add(oval.withPosDeltas(-xSpan, 0));
        list.add(oval.withPosDeltas(xSpan, 0));
        list.add(oval.withPosDeltas(0, -ySpan));
        list.add(oval.withPosDeltas(0, ySpan));
        
        return list;
    }
}
