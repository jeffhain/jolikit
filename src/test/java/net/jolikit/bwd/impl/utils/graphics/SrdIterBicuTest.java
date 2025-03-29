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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;

public class SrdIterBicuTest extends AbstractSrdTezt {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SrdIterBicu TESTED_SRD_ITER_BICU = new SrdIterBicu();
    
    private static final SrdIterBicuAwt REF_SRD_ITER_BICU = new SrdIterBicuAwt();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SrdIterBicuTest() {
    }
    
    public void test_drawRectScaled_centeredSinglePixel() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0xFFFFFFFF, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0xFFFFFFFF, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 5, 5);
        final GRect dstRect = GRect.valueOf(0, 0, 5, 5);
        final GRect dstClip = dstRect;
        
        drawScaledRectPremulAndNotAndCheckEqual_seq(
            input,
            srcRect,
            //
            dstRect,
            dstClip,
            actual,
            //
            expected);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void drawScaledRect(
        InterfaceParallelizer parallelizer,
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer) {
        TESTED_SRD_ITER_BICU.drawScaledRect(
            SequentialParallelizer.getDefault(),
            colorTypeHelper,
            //
            srcPixels,
            srcRect,
            //
            dstRect,
            dstClip,
            dstRowDrawer);
    }
    
    @Override
    protected void checkSturdinessResult(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        MyPixels input,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        MyPixels actual) {
        
        if (srcRect.isEmpty()
            || dstRect.isEmpty()) {
            // Nothing to check.
            return;
        }
        
        final boolean gotSomeClipping =
            !dstClip.equals(dstRect);
        final boolean gotShrinking =
            (dstRect.xSpan() < srcRect.xSpan())
            || (dstRect.ySpan() < srcRect.ySpan());
        // Allowing for one iteration.
        final boolean gotTooMuchShrinking =
            (3 * dstRect.xSpan() < srcRect.xSpan())
            || (3 * dstRect.ySpan() < srcRect.ySpan());
        
        if (gotSomeClipping
            || gotTooMuchShrinking) {
            /*
             * Can't check simply.
             */
        } else {
            final int inputMinAlpha8 = input.getMinAlpha8();
            final boolean isOpaque = (inputMinAlpha8 == 0xFF);
            /*
             * Mean color must be preserved.
             */
            if (true) {
                final int minSpan = Math.min(srcRect.minSpan(), dstRect.minSpan());
                /*
                 * Need more tolerance if got tiny min span,
                 * possibly due to edge artifacts becoming non negligible.
                 * 
                 * Need more tolerance if input is not opaque and color type is not premul,
                 * due to precision loss when going premul for interpolation,
                 * which is propagated when going back to non-premul.
                 */
                final int cptDeltaTol = 0x03
                    + (gotShrinking ? 23 : 0)
                    + ((minSpan <= 1) ? 3 : 0)
                    + ((!isOpaque) && (!colorTypeHelper.isPremul())
                        ? 2 : 0);
                checkMeanColorPreserved(
                    colorTypeHelper,
                    //
                    input,
                    srcRect,
                    //
                    actual,
                    dstRect,
                    //
                    cptDeltaTol);
            }
            /*
             * If opaque or not too transparent
             * (else tolerance need would run up),
             * comparing against AWT.
             */
            if (inputMinAlpha8 >= 0x90) {
                final MyPixels expected = new MyPixels();
                expected.configure(
                    dstRect,
                    new int[dstRect.area()],
                    dstRect.xSpan());
                
                REF_SRD_ITER_BICU.drawScaledRect(
                    SequentialParallelizer.getDefault(),
                    colorTypeHelper,
                    //
                    input,
                    srcRect,
                    //
                    dstRect,
                    dstClip,
                    expected);
                
                // Just checking all pixels.
                final GRect checkArea = dstRect;
                // We assume that the base part
                // of the tolerance is for AWT,
                // which might use approximations
                // (cf. "bicubic_coeff[xcindex]"
                // and "#ifdef BICUBIC_USE_INT_MATH"),
                // not for us.
                // Need more tolerance if input is not opaque,
                // for some reason (maybe AWT being less accurate in this case?).
                final int cptDeltaTol = 0x03
                    + ((!isOpaque) ? 2 : 0);
                try {
                    SrdTestUtils.checkCloseArgb32Over(
                        expected,
                        actual,
                        checkArea,
                        cptDeltaTol);
                } catch (AssertionError e) {
                    if (MUST_PRINT_IMAGE_ON_ERROR) {
                        SrdTestUtils.printPixels("input", input);
                        SrdTestUtils.printPixels("expected", expected);
                        SrdTestUtils.printPixels("actual", actual);
                    }
                    System.out.println("srcRect = " + srcRect);
                    System.out.println("dstRect = " + dstRect);
                    System.out.println("dstClip = " + dstClip);
                    throw e;
                }
            }
        }
    }
}
