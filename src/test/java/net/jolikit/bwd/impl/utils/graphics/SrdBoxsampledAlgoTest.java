/*
 * Copyright 2024-2025 Jeff Hain
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
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;

/**
 * SrdBoxsampled is mostly tested through SrdBoxsampledTest,
 * here we only test the core interpolation method.
 */
public class SrdBoxsampledAlgoTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int NBR_OF_CALLS = 10 * 1000;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MySrcPixels extends IntArrSrcPixels {
        public MySrcPixels() {
        }
        public void reset(
            GRect rect,
            int scanlineStride) {
            this.configure(
                rect,
                new int[scanlineStride * rect.ySpan()],
                scanlineStride);
        }
        /**
         * Top-left coordinates.
         */
        public void setTopLeft(int topLeftX, int topLeftY) {
            GRect rect = this.getRect();
            rect = rect.withPos(topLeftX, topLeftY);
            this.setRect(rect);
        }
        /*
         * Resets spans, but not top-left corner.
         */
        public void setPixels(
            int[][] pixelArrArr,
            int scanlineStride) {
            
            GRect rect = this.getRect();
            final int h = pixelArrArr.length;
            final int w = pixelArrArr[0].length;
            rect = rect.withSpans(w, h);
            this.reset(rect, scanlineStride);
            
            for (int j = 0; j < h; j++) {
                final int[] pixelArr = pixelArrArr[j];
                // Structural consistency check.
                assertEquals(pixelArr.length, w);
                final int y = rect.y() + j;
                for (int i = 0; i < w; i++) {
                    final int x = rect.x() + i;
                    this.setColor32At(x, y, pixelArr[i]);
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public SrdBoxsampledAlgoTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_boxsampledInterpolate_uniform() {
        final int[] spanArr = new int[] {1,2,3,4,5};
        for (int width : spanArr) {
            for (int height : spanArr) {
                for (boolean opaque : new boolean[] {false,true}) {
                    // Premul first, for early fail on validity check.
                    for (boolean premul : new boolean[] {true,false}) {
                        test_boxsampledInterpolate_uniform_xxx(
                            width,
                            height,
                            opaque,
                            premul);
                    }
                }
            }
        }
    }
    
    public void test_boxsampledInterpolate_uniform_xxx(
        int width,
        int height,
        boolean opaque,
        boolean premul) {
        
        final InterfaceColorTypeHelper colorTypeHelper =
            SrdTestUtils.getColorTypeHelper(premul);
        
        final int scanlineStride = width + 3;
        
        final MySrcPixels srcPixels = new MySrcPixels();
        final int[] srcArr = new int[scanlineStride * height];
        final GRect srcRect = GRect.valueOf(0, 0, width, height);
        srcPixels.configure(
            srcRect,
            srcArr,
            scanlineStride);
        final PpColorSum tmpColorSum = new PpColorSum();
        tmpColorSum.configure(colorTypeHelper);
        
        /*
         * Specified surface is clamped to srcRect,
         * so OK if it leaks out.
         */
        final double leakRatio = 0.1;
        final double leakFactor = (1.0 + leakRatio);
        
        final Random random = TestUtils.newRandom123456789L();
        final int nbrOfCalls = NBR_OF_CALLS;
        final double lowProba = Math.min(0.1, 10.0 / nbrOfCalls);
        for (int i = 0; i < nbrOfCalls; i++) {
            
            // Random color at each round.
            // Can use any alpha if not opaque,
            // since the only color change is due to conversion to premul,
            // not to (absent) interpolation.
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            final int expectedNp = randomArgb32(random, minAlpha8);
            
            final int expected = colorTypeHelper.asTypeFromNonPremul32(expectedNp);
            for (int sj = 0; sj < height; sj++) {
                for (int si = 0; si < width; si++) {
                    srcArr[sj * scanlineStride + si] = expected;
                }
            }
            
            final double centerXFp;
            final double centerYFp;
            if (random.nextDouble() < lowProba) {
                // Exact center of rectangle.
                centerXFp = (width - 1) / 2.0;
                centerYFp = (height - 1) / 2.0;
            } else {
                centerXFp =
                    (width * leakFactor) * (random.nextDouble() - leakRatio/2);
                centerYFp =
                    (height * leakFactor) * (random.nextDouble() - leakRatio/2);
            }
            
            final double dxPixelSpanFp;
            if (random.nextDouble() < lowProba) {
                // integer span, possibly zero (tiny surface case)
                dxPixelSpanFp = random.nextInt(width + 1);
            } else {
                dxPixelSpanFp = (width * leakFactor) * random.nextDouble();
            }
            
            final double dyPixelSpanFp;
            if (random.nextDouble() < lowProba) {
                // integer span, possibly zero (tiny surface case)
                dyPixelSpanFp = random.nextInt(height + 1);
            } else {
                dyPixelSpanFp = (height * leakFactor) * random.nextDouble();
            }
            
            if (DEBUG) {
                System.out.println();
                System.out.println("width = " + width);
                System.out.println("height = " + height);
                System.out.println("opaque = " + opaque);
                System.out.println("premul = " + premul);
                System.out.println("expectedNp = " + Argb32.toString(expectedNp));
                System.out.println("centerXFp = " + centerXFp);
                System.out.println("centerYFp = " + centerYFp);
                System.out.println("dxPixelSpanFp = " + dxPixelSpanFp);
                System.out.println("dyPixelSpanFp = " + dyPixelSpanFp);
            }
            
            final int actual = call_boxsampledInterpolate_general(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                //
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                tmpColorSum);
            
            final int cptDeltaTol = 0;
            checkEqualColor32AsPremul(
                colorTypeHelper,
                expected,
                actual,
                cptDeltaTol);
        }
    }
    
    /*
     * 
     */
    
    public void test_boxsampledInterpolate_exactPixel() {
        final int[] spanArr = new int[] {4,5};
        for (int width : spanArr) {
            for (int height : spanArr) {
                for (boolean opaque : new boolean[] {false,true}) {
                    // Premul first, for early fail on validity check.
                    for (boolean premul : new boolean[] {true,false}) {
                        test_boxsampledInterpolate_exactPixel_xxx(
                            width,
                            height,
                            opaque,
                            premul);
                    }
                }
            }
        }
    }
    
    public void test_boxsampledInterpolate_exactPixel_xxx(
        int width,
        int height,
        boolean opaque,
        boolean premul) {
        
        final InterfaceColorTypeHelper colorTypeHelper =
            SrdTestUtils.getColorTypeHelper(premul);
        
        final int scanlineStride = width + 3;
        
        final MySrcPixels srcPixels = new MySrcPixels();
        final int[] srcArr = new int[scanlineStride * height];
        final GRect srcRect = GRect.valueOf(0, 0, width, height);
        srcPixels.configure(
            srcRect,
            srcArr,
            scanlineStride);
        final PpColorSum tmpColorSum = new PpColorSum();
        tmpColorSum.configure(colorTypeHelper);
        
        final Random random = TestUtils.newRandom123456789L();
        final int nbrOfCalls = NBR_OF_CALLS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            if (DEBUG) {
                System.out.println();
                System.out.println("width = " + width);
                System.out.println("height = " + height);
                System.out.println("opaque = " + opaque);
                System.out.println("premul = " + premul);
            }
            
            // Random color at each round.
            // Can use any alpha if not opaque,
            // since the only color change is due to conversion to premul,
            // not to (absent) interpolation.
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            for (int sj = 0; sj < height; sj++) {
                for (int si = 0; si < width; si++) {
                    final int nonPremulArgb32 =
                        randomArgb32(random, minAlpha8);
                    srcArr[sj * scanlineStride + si] =
                        colorTypeHelper.asTypeFromNonPremul32(
                            nonPremulArgb32);
                }
            }

            final int x = random.nextInt(width);
            final int y = random.nextInt(height);
            final double centerXFp = x;
            final double centerYFp = y;
            final double dxPixelSpanFp = 1.0;
            final double dyPixelSpanFp = 1.0;
            
            final int expected = srcPixels.getColor32At(x, y);
            
            final int actual = call_boxsampledInterpolate_general(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                tmpColorSum);
            
            final int cptDeltaTol = 0;
            checkEqualColor32AsPremul(
                colorTypeHelper,
                expected,
                actual,
                cptDeltaTol);
        }
    }
    
    /*
     * 
     */
    
    public void test_boxsampledInterpolate_2x2_oneOverFour() {
        for (boolean opaque : new boolean[] {false,true}) {
            // Premul first, for early fail on validity check.
            for (boolean premul : new boolean[] {true,false}) {
                test_boxsampledInterpolate_2x2_oneOverFour_xxx(
                    opaque,
                    premul);
            }
        }
    }
    
    public void test_boxsampledInterpolate_2x2_oneOverFour_xxx(
        boolean opaque,
        boolean premul) {
        
        final InterfaceColorTypeHelper colorTypeHelper =
            SrdTestUtils.getColorTypeHelper(premul);
        
        final Random random = TestUtils.newRandom123456789L();
        final int nbrOfCalls = NBR_OF_CALLS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            final int alpha8 = uniform(random, minAlpha8, 0xFF);
            // Need tolerance depending on alpha,
            // since we check against theoretical result
            // computed in non-premul.
            final int cptDeltaTol = (alpha8 <= 0xFD ? 1 : 0);
            
            if (DEBUG) {
                System.out.println();
                System.out.println("opaque = " + opaque);
                System.out.println("premul = " + premul);
                System.out.println("alpha8 = " + alpha8);
                System.out.println("cptDeltaTol = " + cptDeltaTol);
            }
            
            final int width = 2;
            final int height = 2;
            
            final int scanlineStride = width + 3;
            
            /*
             * For each component: sum/4.
             * We use multiples of 4:
             * k * 4 = (k-2) + (k-1) + (k+1) + (k+2)
             * - with k = 11 :
             *   0x0B * 4 = 0x09 + 0x0A + 0x0C + 0x0D    
             * - with k = 12 :
             *   0x0C * 4 = 0x0A + 0x0B + 0x0D + 0x0E    
             * - with k = 13 :
             *   0x0D * 4 = 0x0B + 0x0C + 0x0E + 0x0F    
             * ===> That, but with two-digits components to avoid
             *      large relative error when interpolating in premul.
             */
            
            final int aNp = Argb32.withAlpha8(0xFF99AABB, alpha8);
            final int bNp = Argb32.withAlpha8(0xFFAABBCC, alpha8);
            final int cNp = Argb32.withAlpha8(0xFFCCDDEE, alpha8);
            final int dNp = Argb32.withAlpha8(0xFFDDEEFF, alpha8);
            
            final int a = colorTypeHelper.asTypeFromNonPremul32(aNp);
            final int b = colorTypeHelper.asTypeFromNonPremul32(bNp);
            final int c = colorTypeHelper.asTypeFromNonPremul32(cNp);
            final int d = colorTypeHelper.asTypeFromNonPremul32(dNp);
            
            final MySrcPixels srcPixels = new MySrcPixels();
            srcPixels.setPixels(new int[][] {
                {a, b},
                {c, d},
            }, scanlineStride);
            final GRect srcRect = GRect.valueOf(0, 0, width, height);
            final PpColorSum tmpColorSum = new PpColorSum();
            tmpColorSum.configure(colorTypeHelper);
            
            final double centerXFp = 0.5;
            final double centerYFp = 0.5;
            final double dxPixelSpanFp = 1.0;
            final double dyPixelSpanFp = 1.0;
            
            final int expectedNp = Argb32.withAlpha8(0xFFBBCCDD, alpha8);
            final int expected = colorTypeHelper.asTypeFromNonPremul32(expectedNp);
            
            final int actual = call_boxsampledInterpolate_general(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                tmpColorSum);
            
            checkEqualColor32AsPremul(
                colorTypeHelper,
                expected,
                actual,
                cptDeltaTol);
        }
    }
    
    /**
     * General case:
     * - with topLeft
     * - srcRect strictly included in srcPixels
     * - with clamping
     * - with one full pixel and others partially covered
     */
    public void test_boxsampledInterpolate_2x2_generalCase() {
        for (boolean opaque : new boolean[] {false,true}) {
            // Premul first, for early fail on validity check.
            for (boolean premul : new boolean[] {true,false}) {
                test_boxsampledInterpolate_2x2_generalCase_xxx(
                    opaque,
                    premul);
            }
        }
    }
    
    public void test_boxsampledInterpolate_2x2_generalCase_xxx(
        boolean opaque,
        boolean premul) {
        
        final InterfaceColorTypeHelper colorTypeHelper =
            SrdTestUtils.getColorTypeHelper(premul);
        
        final int srcWidth = 2;
        final int srcHeight = 2;
        final int pixelsWidth = srcWidth + 2;
        
        final int scanlineStride = pixelsWidth + 3;
        
        final Random random = TestUtils.newRandom123456789L();
        final int nbrOfCalls = NBR_OF_CALLS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            // If not opaque, can use any alpha,
            // since we do an equivalent computation in the test.
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            
            final int aNp = randomArgb32(random, minAlpha8);
            final int bNp = randomArgb32(random, minAlpha8);
            final int cNp = randomArgb32(random, minAlpha8);
            final int dNp = randomArgb32(random, minAlpha8);
            
            final int a = colorTypeHelper.asTypeFromNonPremul32(aNp);
            final int b = colorTypeHelper.asTypeFromNonPremul32(bNp);
            final int c = colorTypeHelper.asTypeFromNonPremul32(cNp);
            final int d = colorTypeHelper.asTypeFromNonPremul32(dNp);
            
            /*
             * Must ignore the zeroized pixels out of srcRect
             * (thanks to clamping).
             * Pixel weights are 1, 1/p, 1/q, 1/(p*q).
             * We have (1 + 1/p + 1/q + 1/(p*q) = K)
             * i.e. (p*q + q + p + 1 = K * (p*q)).
             * With p=2, q=3: 6 + 3 + 2 + 1 = 12 = K * 6.
             * ===> K=2 (no need to, but remarkable).
             * 
             * Here we test that we give the proper weights
             * to covered pixels, so it's fine to just mimic
             * what the rest of the interpolation code does.
             */
            
            final double rp = 1.0 / 2;
            final double rq = 1.0 / 3;
            final double rpq = rp * rq;
            final double K = 1.0 + rp + rq + rpq;
            
            // Center at the center of either of the 4 pixels,
            // to test all 9 possible types of partial pixel covering code.
            final int centerIndex = random.nextInt(4);
            
            if (DEBUG) {
                System.out.println();
                System.out.println("opaque = " + opaque);
                System.out.println("premul = " + premul);
                System.out.println("aNp = " + Argb32.toString(aNp));
                System.out.println("bNp = " + Argb32.toString(bNp));
                System.out.println("cNp = " + Argb32.toString(cNp));
                System.out.println("dNp = " + Argb32.toString(dNp));
                System.out.println("centerIndex = " + centerIndex);
            }
            
            /*
             * Center and colors positions such as:
             * a ratio is 1 (where center is)
             * b ratio is rp
             * c ratio is rq
             * d ratio is rpq
             */
            final int[][] rowArr;
            final double centerXFp;
            final double centerYFp;
            if (centerIndex == 0) {
                centerXFp = -1.0;
                centerYFp = -1.0;
                rowArr = new int[][] {
                    {0, 0, 0, 0},
                    {0, a, b, 0},
                    {0, c, d, 0},
                    {0, 0, 0, 0},
                };
            } else if (centerIndex == 1) {
                centerXFp = 0.0;
                centerYFp = -1.0;
                rowArr = new int[][] {
                    {0, 0, 0, 0},
                    {0, b, a, 0},
                    {0, d, c, 0},
                    {0, 0, 0, 0},
                };
            } else if (centerIndex == 2) {
                centerXFp = -1.0;
                centerYFp = 0.0;
                rowArr = new int[][] {
                    {0, 0, 0, 0},
                    {0, c, d, 0},
                    {0, a, b, 0},
                    {0, 0, 0, 0},
                };
            } else if (centerIndex == 3) {
                centerXFp = 0.0;
                centerYFp = 0.0;
                rowArr = new int[][] {
                    {0, 0, 0, 0},
                    {0, d, c, 0},
                    {0, b, a, 0},
                    {0, 0, 0, 0},
                };
            } else {
                throw new AssertionError();
            }
            
            final double dxPixelSpanFp = 1.0 + 2 * rp;
            final double dyPixelSpanFp = 1.0 + 2 * rq;
            
            final MySrcPixels srcPixels = new MySrcPixels();
            srcPixels.setTopLeft(-2, -2);
            srcPixels.setPixels(rowArr, scanlineStride);
            final GRect srcRect = GRect.valueOf(
                -1,
                -1,
                srcWidth,
                srcHeight);
            final PpColorSum tmpColorSum = new PpColorSum();
            tmpColorSum.configure(colorTypeHelper);
            
            // Interpolation always done in premul by the algo.
            final int aP = colorTypeHelper.asPremul32FromType(a);
            final int bP = colorTypeHelper.asPremul32FromType(b);
            final int cP = colorTypeHelper.asPremul32FromType(c);
            final int dP = colorTypeHelper.asPremul32FromType(d);
            
            final double eAlpha8d =
                (Argb32.getAlpha8(aP)
                    + rp * Argb32.getAlpha8(bP)
                    + rq * Argb32.getAlpha8(cP)
                    + rpq * Argb32.getAlpha8(dP)) / K;
            final double eRed8d =
                (Argb32.getRed8(aP)
                    + rp * Argb32.getRed8(bP)
                    + rq * Argb32.getRed8(cP)
                    + rpq * Argb32.getRed8(dP)) / K;
            final double eGreen8d =
                (Argb32.getGreen8(aP)
                    + rp * Argb32.getGreen8(bP)
                    + rq * Argb32.getGreen8(cP)
                    + rpq * Argb32.getGreen8(dP)) / K;
            final double eBlue8d =
                (Argb32.getBlue8(aP)
                    + rp * Argb32.getBlue8(bP)
                    + rq * Argb32.getBlue8(cP)
                    + rpq * Argb32.getBlue8(dP)) / K;
            
            final int eAlpha8 = (int) (eAlpha8d + 0.5);
            final int eRed8 = (int) (eRed8d + 0.5);
            final int eGreen8 = (int) (eGreen8d + 0.5);
            final int eBlue8 = (int) (eBlue8d + 0.5);
            
            final int expectedP =
                colorTypeHelper.toValidPremul32(
                    eAlpha8,
                    eRed8,
                    eGreen8,
                    eBlue8);
            
            final int expected = colorTypeHelper.asTypeFromPremul32(expectedP);
            
            final int actual = call_boxsampledInterpolate_general(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                tmpColorSum);
            
            if (DEBUG) {
                System.out.println("expected = " + Argb32.toString(expected));
                System.out.println("actual =   " + Argb32.toString(actual));
            }

            SrdTestUtils.checkIsValidColor(
                colorTypeHelper,
                actual);
            
            // One because might have ties due to rounding errors
            // (like 124.5 giving 125, vs 124.49999999999997 giving 124).
            final int cptDeltaTol = 1;
            checkEqualColor32AsPremul(
                colorTypeHelper,
                expected,
                actual,
                cptDeltaTol);
        }
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Method to test ScaledRectAlgoBoxsampled.boxsampledInterpolate_general()
     * more easily, by doing preliminary Y parameters computations
     * exactly as done in ScaledRectAlgoBoxsampled.
     * 
     * Package-private for use in bilinear test.
     */
    static int call_boxsampledInterpolate_general(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        double centerXFp,
        double centerYFp,
        final double dxPixelSpanFp,
        final double dyPixelSpanFp,
        //
        PpColorSum tmpColorSum) {
        
        final double H = 0.5;
        
        final int sy = srcRect.y();
        
        // Reference for coordinates deltas.
        // Makes intersections with pixels borders easier to compute.
        final double yaFp = sy - H;
        final double ybFp = srcRect.yMaxLong() + H;
        
        // Clamping.
        final double clpYMinFp = NbrsUtils.toRange(yaFp, ybFp, centerYFp - dyPixelSpanFp * H);
        final double clpYMaxFp = NbrsUtils.toRange(yaFp, ybFp, centerYFp + dyPixelSpanFp * H);
        //
        final double clpYSpanFp = clpYMaxFp - clpYMinFp;
        
        final double clpDyMinFp = (clpYMinFp - yaFp);
        final double clpDyMaxFp = (clpYMaxFp - yaFp);
        
        final int clpDyMinFloor = (int) clpDyMinFp;
        final int clpDyMaxFloor = (int) clpDyMaxFp;
        
        // When no pixel is fully covered in Y,
        // fullDyMin is the coordinate of the pixel after the one covered,
        // so as to always have loYRatio used for pixel at fullDyMin-1.
        final double loYRatio;
        final int fullDyMin;
        final int fullDyMax;
        final double hiYRatio;
        if ((clpDyMinFloor == clpDyMaxFloor)
            || (clpDyMinFloor + 1.0 == clpDyMaxFp)) {
            /*
             * Area in same Y row.
             * Will only use (loYRatio,fullDyMin-1).
             */
            loYRatio = (clpDyMaxFp - clpDyMinFp);
            fullDyMin = clpDyMinFloor + 1;
            fullDyMax = Integer.MIN_VALUE;
            hiYRatio = 0.0;
        } else {
            loYRatio = (clpDyMinFloor + 1 - clpDyMinFp);
            fullDyMin = clpDyMinFloor + 1;
            fullDyMax = clpDyMaxFloor - 1;
            hiYRatio = (clpDyMaxFp - clpDyMaxFloor);
        }
        
        return SrdBoxsampled.boxsampledInterpolate_general(
            colorTypeHelper,
            //
            srcPixels,
            srcRect,
            centerXFp,
            centerYFp,
            dxPixelSpanFp,
            //
            clpYSpanFp,
            loYRatio,
            fullDyMin,
            fullDyMax,
            hiYRatio,
            //
            tmpColorSum);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int randomArgb32(Random random, int minAlpha8) {
        return Argb32.withAlpha8(random.nextInt(), uniform(random, minAlpha8, 0xFF));
    }
    
    private static int uniform(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * Always interpolating in premul,
     * so if color type is not premul,
     * we convert expected and actual to premul before comparison,
     * to simulate the same conversion error.
     */
    private static void checkEqualColor32AsPremul(
        InterfaceColorTypeHelper colorTypeHelper,
        int expected,
        int actual,
        int cptDeltaTol) {
        
        final int expectedP =
            colorTypeHelper.asPremul32FromType(
                expected);
        final int actualP =
            colorTypeHelper.asPremul32FromType(
                actual);
        
        SrdTestUtils.checkCloseColor32(
            expectedP,
            actualP,
            cptDeltaTol);
    }
}
