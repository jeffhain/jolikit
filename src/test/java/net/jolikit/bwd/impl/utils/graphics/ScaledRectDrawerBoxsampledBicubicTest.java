/*
 * Copyright 2024 Jeff Hain
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

import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;

public class ScaledRectDrawerBoxsampledBicubicTest extends AbstractScaledRectDrawerTezt {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final ScaledRectDrawerBicubic DRAWER_BICUBIC =
        new ScaledRectDrawerBicubic();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerBoxsampledBicubicTest() {
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
        ScaledRectDrawing.drawScaledRect(
            parallelizer,
            BwdScalingType.BOXSAMPLED_BICUBIC,
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
        
        final boolean gotSomeShrinking =
            (dstRect.xSpan() < srcRect.xSpan())
            || (dstRect.ySpan() < srcRect.ySpan());
        
        if (gotSomeShrinking) {
            /*
             * Can't check simply.
             */
        } else {
            /*
             * Must be same as BICUBIC.
             */
            final MyPixels expected = new MyPixels();
            expected.configure(
                dstRect,
                new int[dstRect.area()],
                dstRect.xSpan());
            
            DRAWER_BICUBIC.drawScaledRect(
                SequentialParallelizer.getDefault(),
                colorTypeHelper,
                input,
                srcRect,
                dstRect,
                dstClip,
                expected);
            
            // Just checking all pixels.
            final GRect checkArea = dstRect;
            final int cptDeltaTol = 0x00;
            ScaledRectTestUtils.checkCloseArgb32Over(
                expected,
                actual,
                checkArea,
                cptDeltaTol);
        }
    }
}
