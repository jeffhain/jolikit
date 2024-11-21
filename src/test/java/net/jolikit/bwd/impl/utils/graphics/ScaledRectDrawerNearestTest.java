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

public class ScaledRectDrawerNearestTest extends AbstractScaledRectDrawerTezt {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerNearestTest() {
    }
    
    /**
     * X span multiplied by 2.
     */
    public void test_drawRectScaled_growth_2_1() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00},
            {0xFF808000, 0xFF008080},
            {0xFFF0F000, 0xFF00F0F0},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0, 0},
            {0, 0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00, 0},
            {0, 0xFF808000, 0xFF808000, 0xFF008080, 0xFF008080, 0},
            {0, 0xFFF0F000, 0xFFF0F000, 0xFF00F0F0, 0xFF00F0F0, 0},
            {0, 0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 3);
        final GRect dstRect = GRect.valueOf(1, 1, 4, 3);
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
    
    /**
     * X and Y spans multiplied by 2.
     */
    public void test_drawRectScaled_growth_2_2() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00},
            {0xFF808000, 0xFF008080},
            {0xFFF0F000, 0xFF00F0F0},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0, 0},
            {0, 0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00, 0},
            {0, 0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00, 0},
            {0, 0xFF808000, 0xFF808000, 0xFF008080, 0xFF008080, 0},
            {0, 0xFF808000, 0xFF808000, 0xFF008080, 0xFF008080, 0},
            {0, 0xFFF0F000, 0xFFF0F000, 0xFF00F0F0, 0xFF00F0F0, 0},
            {0, 0xFFF0F000, 0xFFF0F000, 0xFF00F0F0, 0xFF00F0F0, 0},
            {0, 0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 3);
        final GRect dstRect = GRect.valueOf(1, 1, 4, 6);
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
    
    /**
     * X and Y spans multiplied by 2.
     */
    public void test_drawRectScaled_growth_2_2_clipped() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00},
            {0xFF808000, 0xFF008080},
            {0xFFF0F000, 0xFF00F0F0},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0},
            {0, 0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0, 0},
            {0, 0xFF808000, 0xFF808000, 0xFF008080, 0, 0},
            {0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 3);
        final GRect dstRect = GRect.valueOf(1, 1, 4, 6);
        final GRect dstClip = GRect.valueOf(1, 2, 3, 2);
        
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
    
    /**
     * X and Y spans multiplied by 3.
     */
    public void test_drawRectScaled_growth_3_3() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00},
            {0xFF808000, 0xFF008080},
            {0xFFF0F000, 0xFF00F0F0},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0},
            {0, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0},
            {0, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0},
            {0, 0xFF808000, 0xFF808000, 0xFF808000, 0xFF008080, 0xFF008080, 0xFF008080, 0},
            {0, 0xFF808000, 0xFF808000, 0xFF808000, 0xFF008080, 0xFF008080, 0xFF008080, 0},
            {0, 0xFF808000, 0xFF808000, 0xFF808000, 0xFF008080, 0xFF008080, 0xFF008080, 0},
            {0, 0xFFF0F000, 0xFFF0F000, 0xFFF0F000, 0xFF00F0F0, 0xFF00F0F0, 0xFF00F0F0, 0},
            {0, 0xFFF0F000, 0xFFF0F000, 0xFFF0F000, 0xFF00F0F0, 0xFF00F0F0, 0xFF00F0F0, 0},
            {0, 0xFFF0F000, 0xFFF0F000, 0xFFF0F000, 0xFF00F0F0, 0xFF00F0F0, 0xFF00F0F0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 3);
        final GRect dstRect = GRect.valueOf(1, 1, 6, 9);
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

    /**
     * X and Y spans multiplied by 1.5, clipped.
     */
    public void test_drawRectScaled_growth_1d5_1d5_clipped() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF800000, 0xFF008000},
            {0xFF000040, 0xFF040000},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0},
            {0, 0xFF800000, 0xFF008000, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 2);
        final GRect dstRect = GRect.valueOf(1, 1, 3, 3);
        final GRect dstClip = GRect.valueOf(1, 1, 2, 1);
        
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

    /**
     * X and Y spans multiplied by 1.5, closest
     * with dstRect leaking outside of dst pixels range but clipped inside.
     */
    public void test_drawRectScaled_clippedToInside() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF800000, 0xFF008000},
            {0xFF000040, 0xFF040000},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0xFF800000, 0xFF800000, 0xFF008000, 0xFF008000},
            {0xFF800000, 0xFF800000, 0xFF008000, 0xFF008000},
            {0xFF000040, 0xFF000040, 0xFF040000, 0xFF040000},
            {0xFF000040, 0xFF000040, 0xFF040000, 0xFF040000},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 2);
        final GRect dstRect = GRect.valueOf(-2, -2, 8, 8);
        final GRect dstClip = GRect.valueOf(0, 0, 4, 4);
        
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
            BwdScalingType.NEAREST,
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
        
        if (dstClip.equals(dstRect)
            && ScaledRectUtils.isNearestExact(
                srcRect.xSpan(),
                srcRect.ySpan(),
                dstRect.xSpan(),
                dstRect.ySpan())) {
            /*
             * Mean color must be preserved.
             */
            // Tolerance of 1 because can have ties.
            final int cptDeltaTol = 0x01;
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
        } else {
            /*
             * Can't check simply.
             */
        }
    }
}
