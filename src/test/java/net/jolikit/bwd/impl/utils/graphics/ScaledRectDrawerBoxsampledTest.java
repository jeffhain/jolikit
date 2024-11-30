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

public class ScaledRectDrawerBoxsampledTest extends AbstractScaledRectDrawerTezt {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerBoxsampledTest() {
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
     * X and Y spans divided by 2.
     */
    public void test_drawRectScaled_shrinking_2_2() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF140000, 0xFF108000, 0xFF014000, 0xFF010800},
            {0xFF1080C0, 0xFF500000, 0xFF01080C, 0xFF050000},
            {0xFF001400, 0xFF001080, 0xFF000140, 0xFF000108},
            {0xFFC01080, 0xFF005000, 0xFF0C0108, 0xFF000500},
            {0xFF000014, 0xFF800010, 0xFF400001, 0xFF080001},
            {0xFF80C010, 0xFF000050, 0xFF080C01, 0xFF000005},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0},
            {0, 0xFF214030, 0xFF021403, 0},
            {0, 0xFF302140, 0xFF030214, 0},
            {0, 0xFF403021, 0xFF140302, 0},
            {0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 4, 6);
        final GRect dstRect = GRect.valueOf(1, 1, 2, 3);
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
     * X and Y spans divided by 3.
     */
    public void test_drawRectScaled_shrinking_3_3() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF190000, 0xFF105000, 0xFF000001, 0xFF019000, 0xFF010500, 0xFF100000},
            {0xFF104004, 0xFF600000, 0xFF000001, 0xFF410400, 0xFF060000, 0xFF100000},
            {0xFF000001, 0xFF000001, 0xFF000001, 0xFF100000, 0xFF100000, 0xFF100000},
            {0xFF001900, 0xFF001050, 0xFF010000, 0xFF000190, 0xFF000104, 0xFF001000},
            {0xFF041040, 0xFF006000, 0xFF010000, 0xFF004104, 0xFF000600, 0xFF001000},
            {0xFF010000, 0xFF010000, 0xFF010000, 0xFF001000, 0xFF001000, 0xFF001000},
            {0xFF000019, 0xFF500010, 0xFF000100, 0xFF900001, 0xFF040001, 0xFF000010},
            {0xFF400410, 0xFF000060, 0xFF000100, 0xFF040041, 0xFF000006, 0xFF000010},
            {0xFF000100, 0xFF000100, 0xFF000100, 0xFF000010, 0xFF000010, 0xFF000010},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0},
            {0, 0xFF111001, 0xFF111100, 0},
            {0, 0xFF011110, 0xFF001111, 0},
            {0, 0xFF100111, 0xFF110011, 0},
            {0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 6, 9);
        final GRect dstRect = GRect.valueOf(1, 1, 2, 3);
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
     * X span multiplied by 1.5.
     */
    public void test_drawRectScaled_growth_1d5_1() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF800000, 0xFF008000},
            {0xFF000080, 0xFF040000},
            {0xFF000400, 0xFF000004},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0},
            {0, 0xFF800000, 0xFF404000, 0xFF008000, 0},
            {0, 0xFF000080, 0xFF020040, 0xFF040000, 0},
            {0, 0xFF000400, 0xFF000202, 0xFF000004, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 3);
        final GRect dstRect = GRect.valueOf(1, 1, 3, 3);
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
     * X and Y spans multiplied by 1.5.
     */
    public void test_drawRectScaled_growth_1d5_1d5() {
        
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
            {0, 0xFF800000, 0xFF404000, 0xFF008000, 0},
            {0, 0xFF400020, 0xFF212010, 0xFF024000, 0},
            {0, 0xFF000040, 0xFF020020, 0xFF040000, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 2);
        final GRect dstRect = GRect.valueOf(1, 1, 3, 3);
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
            {0, 0, 0, 0, 0},
            {0, 0xFF400020, 0xFF212010, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 2);
        final GRect dstRect = GRect.valueOf(1, 1, 3, 3);
        final GRect dstClip = GRect.valueOf(1, 2, 2, 1);
        
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
     * X and Y spans multiplied by 1.5,
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
            {0xFF800000, 0xFF800000, 0xFF404000, 0xFF008000, 0xFF008000},
            {0xFF800000, 0xFF800000, 0xFF404000, 0xFF008000, 0xFF008000},
            {0xFF400020, 0xFF400020, 0xFF212010, 0xFF024000, 0xFF024000},
            {0xFF000040, 0xFF000040, 0xFF020020, 0xFF040000, 0xFF040000},
            {0xFF000040, 0xFF000040, 0xFF020020, 0xFF040000, 0xFF040000},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 2);
        final GRect dstRect = GRect.valueOf(-1, -1, 7, 7);
        final GRect dstClip = GRect.valueOf(0, 0, 5, 5);
        
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
     * X and Y spans divided by 1.5.
     */
    public void test_drawRectScaled_shrinking_1d5_1d5() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        /*
         * Pixels contributions to resulting pixels:
         * - corner pixels: 4/9th
         * - sides centers pixels: 2/9th
         * - center pixel: 1/9th
         * ===> Using multiples of 9 for their components,
         *      actually just 9 itself to keep things simple.
         */
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF900000, 0xFF009000, 0xFF000090},
            {0xFF090000, 0xFF000900, 0xFF000009},
            {0xFF009000, 0xFF000090, 0xFF900000},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0},
            {0, 0xFF422100, 0xFF002142, 0},
            {0, 0xFF024120, 0xFF400122, 0},
            {0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 3, 3);
        final GRect dstRect = GRect.valueOf(1, 1, 2, 2);
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
    
    /*
     * 
     */
    
    /**
     * Case where a dst pixel's X range is strictly within
     * the X range of a src pixel.
     */
    public void test_drawRectScaled_dstPixelXRangeStrictlyInSrc() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF800000},
            {0xFF008000},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0, 0, 0},
            {0, 0xFF404000, 0xFF404000, 0xFF404000, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 1, 2);
        final GRect dstRect = GRect.valueOf(1, 1, 3, 1);
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
     * Case where a dst pixel's Y range is strictly within
     * the Y range of a src pixel.
     */
    public void test_drawRectScaled_dstPixelYRangeStrictlyInSrc() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFF800000, 0xFF008000},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0, 0, 0},
            {0, 0xFF404000, 0},
            {0, 0xFF404000, 0},
            {0, 0xFF404000, 0},
            {0, 0, 0},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 1);
        final GRect dstRect = GRect.valueOf(1, 1, 1, 3);
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
            BwdScalingType.BOXSAMPLED,
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
        
        if (dstClip.equals(dstRect)) {
            /*
             * Mean color must be preserved.
             */
            // Need more tolerance if input is not opaque and color type is not premul,
            // due to precision loss when going premul for interpolation,
            // which is propagated when going back to non-premul.
            final boolean isOpaque = (input.getMinAlpha8() == 0xFF);
            final int cptDeltaTol = 0x01
                + ((!isOpaque) && (!colorTypeHelper.isPremul())
                    ? 3 : 0);
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
             * Counting on ScaledRectAlgoBoxsampledTest instead.
             */
        }
    }
}
