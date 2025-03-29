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

/**
 * Tests Srd1Down2Up through use of BOXSAMPLED and BICUBIC.
 */
public class SrdBoxBicuTest extends AbstractSrdTezt {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SrdBoxBicu TESTED_SRD_BOX_BICU = new SrdBoxBicu();
    
    private static final SrdBoxsampled REF_SRD_BOX = new SrdBoxsampled();
    
    private static final SrdBicubic REF_SRD_BICU = new SrdBicubic();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SrdBoxBicuTest() {
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
        TESTED_SRD_BOX_BICU.drawScaledRect(
            parallelizer,
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
        
        final boolean wDown = (dstRect.xSpan() < srcRect.xSpan());
        final boolean hDown = (dstRect.ySpan() < srcRect.ySpan());
        
        final InterfaceScaledRectDrawer refDrawer;
        final int cptDeltaTol;
        if (wDown && hDown) {
            refDrawer = REF_SRD_BOX;
            cptDeltaTol = 0x00;
        } else if ((!wDown) && (!hDown)) {
            refDrawer = REF_SRD_BICU;
            cptDeltaTol = 0x00;
        } else {
            /*
             * Can't check simply.
             */
            refDrawer = null;
            cptDeltaTol = -1;
        }
        
        if (refDrawer != null) {
            final MyPixels expected = new MyPixels();
            expected.configure(
                dstRect,
                new int[dstRect.area()],
                dstRect.xSpan());
            
            refDrawer.drawScaledRect(
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
