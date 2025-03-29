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
import net.jolikit.test.utils.TestUtils;

/**
 * SrdBilinear is mostly tested through SrdIterBiliTest,
 * here we only test the core interpolation method.
 */
public class SrdBilinearAlgoTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int NBR_OF_CALLS = 10 * 1000;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public SrdBilinearAlgoTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_computeBilinearColor32_4x4_generalCase() {
        for (boolean opaque : new boolean[] {false,true}) {
            // Premul first, for early fail on validity check.
            for (boolean premul : new boolean[] {true,false}) {
                test_computeBilinearColor32_4x4_generalCase_xxx(
                    opaque,
                    premul);
            }
        }
    }
    
    public void test_computeBilinearColor32_4x4_generalCase_xxx(
        boolean opaque,
        boolean premul) {
        
        final InterfaceColorTypeHelper colorTypeHelper =
            SrdTestUtils.getColorTypeHelper(premul);
        
        final int srcWidth = 4;
        final int srcHeight = 4;
        final int scanlineStride = srcWidth + 1;
        
        final Random random = TestUtils.newRandom123456789L();
        final int nbrOfCalls = NBR_OF_CALLS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            // If not opaque, can use any alpha,
            // since we do an equivalent computation in the test.
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            
            if (DEBUG) {
                System.out.println();
                System.out.println("opaque = " + opaque);
                System.out.println("premul = " + premul);
            }
            
            final int srcArrSize = scanlineStride * srcHeight;
            final int[] srcArr = new int[srcArrSize];
            for (int j = 0; j < srcArr.length; j++) {
                final int argb32 = randomArgb32(random, minAlpha8);
                final int color32 = colorTypeHelper.asTypeFromNonPremul32(argb32);
                srcArr[j] = color32;
            }
            
            final IntArrSrcPixels srcPixels = new IntArrSrcPixels();
            final GRect srcPixelsRect = GRect.valueOf(
                0,
                0,
                srcWidth,
                srcHeight);
            srcPixels.configure(
                srcPixelsRect,
                srcArr,
                scanlineStride);
            // So that we can use any src pixel.
            final GRect srcRect = srcPixelsRect;
            
            final double centerXFp = srcWidth * random.nextDouble();
            final double centerYFp = srcWidth * random.nextDouble();
            
            if (DEBUG) {
                System.out.println("centerXFp = " + centerXFp);
                System.out.println("centerYFp = " + centerYFp);
            }
            
            // These need to be 1 for box sampled
            // to be equivalent to bilinear.
            final double dxPixelSpanFp = 1.0;
            final double dyPixelSpanFp = 1.0;
            
            final PpColorSum tmpColorSum = new PpColorSum();
            tmpColorSum.configure(colorTypeHelper);
            final int expected =
                SrdBoxsampledAlgoTest.call_boxsampledInterpolate_general(
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
            
            final int actual = call_bilinearInterpolate(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                centerXFp,
                centerYFp);
            
            if (DEBUG) {
                System.out.println("expected = " + Argb32.toString(expected));
                System.out.println("actual =   " + Argb32.toString(actual));
            }
            
            SrdTestUtils.checkIsValidColor(
                colorTypeHelper,
                actual);
            
            // No differences due to ties with our random values so far.
            final int cptDeltaTol = 0;
            checkEqualColor32AsPremul(
                colorTypeHelper,
                expected,
                actual,
                cptDeltaTol);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Method to test ScaledRectAlgoBilinear.bilinearInterpolate()
     * more easily, by doing preliminary parameters computations
     * exactly as done in ScaledRectAlgoBilinear.
     */
    private static int call_bilinearInterpolate(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        double srcXFp,
        double srcYFp) {
        
        final int syFloor = (int) Math.floor(srcYFp);
        final double syFracFp = srcYFp - syFloor;
        
        final int sxFloor = (int) Math.floor(srcXFp);
        final double sxFracFp = srcXFp - sxFloor;
        
        if (DEBUG) {
            System.out.println("sxFloor = " + sxFloor);
            System.out.println("syFloor = " + syFloor);
            System.out.println("sxFracFp = " + sxFracFp);
            System.out.println("syFracFp = " + syFracFp);
        }
        
        return SrdBilinear.bilinearInterpolate(
            colorTypeHelper,
            //
            srcPixels,
            srcRect,
            sxFloor,
            syFloor,
            sxFracFp,
            syFracFp);
    }
    
    /*
     * 
     */
    
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
