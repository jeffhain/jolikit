/*
 * Copyright 2021 Jeff Hain
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
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.BindingPrlUtils;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;

public class ScaledRectDrawerTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * Enough to have a good gain, not more because CPUs
     * with more core are still not that common.
     */
    private static final int PARALLELISM = 4;
    
    /*
     * Small thresholds to trigger parallelism even with small spans.
     */
    
    private static final double TEST_MIN_AREA_COST_FOR_SPLIT_CLOSEST = 2.0;
    private static final double TEST_MIN_AREA_COST_FOR_SPLIT_SMOOTH = 2.0;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyPixels
    extends IntArrSrcPixels
    implements InterfaceRowDrawer {
        private GRect pixelRect = GRect.DEFAULT_EMPTY;
        public MyPixels() {
        }
        public void reset(
            int width,
            int height,
            int scanlineStride) {
            this.pixelRect = GRect.valueOf(0, 0, width, height);
            this.configure(
                width,
                height,
                new int[scanlineStride * height],
                scanlineStride);
        }
        /**
         * Top-left coordinates.
         */
        public void configureTopLeft(int topLeftX, int topLeftY) {
            this.pixelRect = this.pixelRect.withPos(topLeftX, topLeftY);
        }
        @Override
        public String toString() {
            return toStringPixels(this.getPixels());
        }
        @Override
        public int hashCode() {
            final int width = this.getWidth();
            final int height = this.getHeight();
            final int[] color32Arr = this.color32Arr();
            final int scanlineStride = this.getScanlineStride();
            
            final int prime = 31;
            int hc = 0;
            hc = hc * prime + width;
            hc = hc * prime + height;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    hc = hc * prime + color32Arr[j * scanlineStride + i];
                }
            }
            return hc;
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MyPixels)) {
                return false;
            }
            
            final MyPixels other = (MyPixels) obj;
            
            final int width1 = this.getWidth();
            final int height1 = this.getHeight();
            final int[] color32Arr1 = this.color32Arr();
            final int scanlineStride1 = this.getScanlineStride();
            
            final int width2 = other.getWidth();
            final int height2 = other.getHeight();
            final int[] color32Arr2 = other.color32Arr();
            final int scanlineStride2 = other.getScanlineStride();
            
            boolean foundDiff = false;
            if ((width1 != width2)
                || (height1 != height2)) {
                foundDiff = true;
            } else {
                Loop1 : for (int j = 0; j < height1; j++) {
                    for (int i = 0; i < width1; i++) {
                        final int p1 = color32Arr1[i + scanlineStride1 * j];
                        final int p2 = color32Arr2[i + scanlineStride2 * j];
                        if (p1 != p2) {
                            foundDiff = true;
                            break Loop1;
                        }
                    }
                }
            }
            return (!foundDiff);
        }
        public void randomize(Random random) {
            for (int j = 0; j < this.getHeight(); j++) {
                for (int i = 0; i < this.getWidth(); i++) {
                    final int color32 = random.nextInt();
                    super.setColor32At(i, j, color32);
                }
            }
        }
        @Override
        public int getColor32At(int x, int y) {
            this.checkInRange(x, y);
            return super.getColor32At(x - this.pixelRect.x(), y - this.pixelRect.y());
        }
        @Override
        public void setColor32At(int x, int y, int color32) {
            this.checkInRange(x, y);
            super.setColor32At(x - this.pixelRect.x(), y - this.pixelRect.y(), color32);
        }
        @Override
        public void drawRow(
            int[] rowArr,
            int rowOffset,
            int dstX,
            int dstY,
            int length) {
            if (DEBUG) {
                System.out.println("drawRow()");
                System.out.println("rowArr.length = " + rowArr.length);
                System.out.println("rowOffset = " + rowOffset);
                System.out.println("dstX = " + dstX);
                System.out.println("dstY = " + dstY);
                System.out.println("length = " + length);
            }
            for (int i = 0; i < length; i++) {
                final int color32 = rowArr[rowOffset + i];
                this.setColor32At(
                    dstX + i,
                    dstY,
                    color32);
            }
        }
        /*
         * Resets dimensions.
         */
        public void setPixels(
            int[][] pixelArrArr,
            int scanlineStride) {
            
            final int height = pixelArrArr.length;
            final int width = pixelArrArr[0].length;
            this.reset(width, height, scanlineStride);
            for (int j = 0; j < height; j++) {
                final int[] pixelArr = pixelArrArr[j];
                
                // Structural consistency check.
                assertEquals(pixelArr.length, width);
                
                for (int i = 0; i < width; i++) {
                    this.setColor32At(i, j, pixelArr[i]);
                }
            }
        }
        public int[][] getPixels() {
            final int width = this.getWidth();
            final int height = this.getHeight();
            
            final int[][] pixelArrArr = new int[height][];
            for (int j = 0; j < height; j++) {
                final int[] pixelArr = new int[width];
                pixelArrArr[j] = pixelArr;
                for (int i = 0; i < width; i++) {
                    pixelArr[i] = this.getColor32At(i, j);
                }
            }
            return pixelArrArr;
        }
        private void checkInRange(int x, int y) {
            if (!this.pixelRect.contains(x, y)) {
                throw new IllegalArgumentException(
                    "(" + x + "," + y + ") not in " + this.pixelRect);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final InterfaceParallelizer PRLZR =
        BindingPrlUtils.newParallelizer(
            new BaseBwdBindingConfig(),
            PARALLELISM,
            ScaledRectDrawerPerf.class.getClass().getSimpleName());
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_drawRectScaled_identity() {
        
        // Scanline stride must have "no effect".
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00},
            {0xFF808000, 0xFF008080},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00},
            {0xFF808000, 0xFF008080},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 2, 2),
            GRect.valueOf(0, 0, 2, 2),
            actual);
        
        assertEquals(expected, actual);
    }
    
    /**
     * Shifted, other dimensions, scanlineStride,
     * but no scaling.
     */
    public void test_drawRectScaled_identity_shifted() {
        
        // Scanline stride must have "no effect".
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0, 0, 0, 0, 0},
            {0, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0},
            {0, 0xFF808000, 0xFF008080, 0xFF800080, 0},
            {0, 0xFFF0F000, 0xFF00F0F0, 0xFFF000F0, 0},
            {0, 0xFF01000, 0xFF000100, 0xFF000001, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00, 0xFF0000FF},
            {0xFF808000, 0xFF008080, 0xFF800080},
            {0xFFF0F000, 0xFF00F0F0, 0xFFF000F0},
            {0xFF01000, 0xFF000100, 0xFF000001},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        actual.reset(
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(1, 1, 3, 4),
            GRect.valueOf(0, 0, 3, 4),
            actual);
        
        assertEquals(expected, actual);
    }
    
    public void test_drawRectScaled_negativeSrcCoords() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final MyPixels input = new MyPixels();
        input.setPixels(new int[][] {
            {0, 0},
            {0, 0},
        }, scanlineStride = 2);
        // Negative srcRect rejected
        // even if in input pixels rect
        // (because there is no offset for src array).
        input.configureTopLeft(-1, -1);
        
        final MyPixels output = new MyPixels();
        output.reset(
            2,
            2,
            scanlineStride = 2);
        output.configureTopLeft(-1, -1);

        for (GRect srcRect : new GRect[] {
            GRect.valueOf(0, 0, 1, 1),
            GRect.valueOf(-1, 0, 2, 1),
            GRect.valueOf(0, -1, 1, 2),
            GRect.valueOf(-1, -1, 2, 2),
        }) {
            for (GRect dstRect : new GRect[] {
                GRect.valueOf(0, 0, 1, 1),
                GRect.valueOf(-1, 0, 1, 1),
                GRect.valueOf(0, -1, 1, 1),
                GRect.valueOf(-1, -1, 1, 1),
            }) {
                final boolean srcPositive =
                    (srcRect.x() >= 0)
                    && (srcRect.y() >= 0);
                try {
                    callDrawRectScaled_seq(
                        input,
                        srcRect,
                        dstRect,
                        output);
                    assertTrue(srcPositive);
                } catch (IllegalArgumentException e) {
                    assertFalse(srcPositive);
                }
            }
        }
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 2, 3),
            GRect.valueOf(1, 1, 4, 3),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 2, 3),
            GRect.valueOf(1, 1, 4, 6),
            actual);
        
        assertEquals(expected, actual);
    }
    
    /**
     * X and Y spans multiplied by 2.
     */
    public void test_drawRectScaled_growth_2_2_clipped() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final boolean mustUseSmoothElseClosest = true;
        
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            mustUseSmoothElseClosest,
            input,
            GRect.valueOf(0, 0, 2, 3),
            GRect.valueOf(1, 1, 4, 6),
            GRect.valueOf(1, 2, 3, 2),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 2, 3),
            GRect.valueOf(1, 1, 6, 9),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 4, 6),
            GRect.valueOf(1, 1, 2, 3),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 6, 9),
            GRect.valueOf(1, 1, 2, 3),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 2, 3),
            GRect.valueOf(1, 1, 3, 3),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 2, 2),
            GRect.valueOf(1, 1, 3, 3),
            actual);
        
        assertEquals(expected, actual);
    }

    /**
     * X and Y spans multiplied by 1.5, smooth clipped.
     */
    public void test_drawRectScaled_growth_1d5_1d5_smooth_clipped() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final boolean mustUseSmoothElseClosest = true;
        
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            mustUseSmoothElseClosest,
            input,
            GRect.valueOf(0, 0, 2, 2),
            GRect.valueOf(1, 1, 3, 3),
            GRect.valueOf(1, 2, 2, 1),
            actual);
        
        assertEquals(expected, actual);
    }

    /**
     * X and Y spans multiplied by 1.5, closest clipped.
     */
    public void test_drawRectScaled_growth_1d5_1d5_closest_clipped() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final boolean mustUseSmoothElseClosest = false;
        
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            mustUseSmoothElseClosest,
            input,
            GRect.valueOf(0, 0, 2, 2),
            GRect.valueOf(1, 1, 3, 3),
            GRect.valueOf(1, 1, 2, 1),
            actual);
        
        assertEquals(expected, actual);
    }

    /**
     * X and Y spans multiplied by 1.5, smooth
     * with dstRect leaking outside of dst pixels range but clipped inside.
     */
    public void test_drawRectScaled_smooth_clippedToInside() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final boolean mustUseSmoothElseClosest = true;
        
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            mustUseSmoothElseClosest,
            input,
            GRect.valueOf(0, 0, 2, 2),
            GRect.valueOf(-1, -1, 7, 7),
            GRect.valueOf(0, 0, 5, 5),
            actual);
        
        assertEquals(expected, actual);
    }

    /**
     * X and Y spans multiplied by 1.5, closest
     * with dstRect leaking outside of dst pixels range but clipped inside.
     */
    public void test_drawRectScaled_closest_clippedToInside() {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final boolean mustUseSmoothElseClosest = false;
        
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            mustUseSmoothElseClosest,
            input,
            GRect.valueOf(0, 0, 2, 2),
            GRect.valueOf(-2, -2, 8, 8),
            GRect.valueOf(0, 0, 4, 4),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 3, 3),
            GRect.valueOf(1, 1, 2, 2),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 1, 2),
            GRect.valueOf(1, 1, 3, 1),
            actual);
        
        assertEquals(expected, actual);
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
            expected.getWidth(),
            expected.getHeight(),
            scanlineStride = 9);
        
        callDrawRectScaled_seq(
            input,
            GRect.valueOf(0, 0, 2, 1),
            GRect.valueOf(1, 1, 1, 3),
            actual);
        
        assertEquals(expected, actual);
    }
    
    /*
     * 
     */
    
    public void test_drawRectScaled_sturdiness_seq() {
        final boolean prlElseSeq = false;
        this.test_drawRectScaled_sturdiness(prlElseSeq);
    }
    
    public void test_drawRectScaled_sturdiness_prl() {
        final boolean prlElseSeq = true;
        this.test_drawRectScaled_sturdiness(prlElseSeq);
    }
    
    public void test_drawRectScaled_sturdiness(boolean prlElseSeq) {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final Random random = TestUtils.newRandom123456789L();
        
        final MyPixels inputNullArr = new MyPixels() {
            @Override
            public int[] color32Arr() {
                return null;
            }
            @Override
            public int getScanlineStride() {
                return 0;
            }
        };

        final int nbrOfCases = 100 * 1000;
        final int minDstPos = -3;
        final int maxPos = 3;
        final int maxSpan = 13;
        for (int k = 0; k < nbrOfCases; k++) {
            
            final InterfaceParallelizer parallelizer;
            if (prlElseSeq) {
                parallelizer = PRLZR;
            } else {
                parallelizer = SequentialParallelizer.getDefault();
            }
            
            final boolean mustUseSmoothElseClosest = random.nextBoolean();
            
            final boolean mustUseNullArr = random.nextBoolean();

            final GRect srcRect = GRect.valueOf(
                random.nextInt(maxPos + 1),
                random.nextInt(maxPos + 1),
                random.nextInt(maxSpan + 1),
                random.nextInt(maxSpan + 1));
            // src needs to cover srcRect.
            final int srcWidth = srcRect.x() + srcRect.xSpan();
            final int srcHeight = srcRect.y() + srcRect.ySpan();
            final int srcScanlineStride = srcWidth + random.nextInt(4);
            
            final GRect dstRect = GRect.valueOf(
                minDstPos + random.nextInt(maxPos - minDstPos + 1),
                minDstPos + random.nextInt(maxPos - minDstPos + 1),
                random.nextInt(maxSpan + 1),
                random.nextInt(maxSpan + 1));
            // dst just matching dstRect.
            final int dstWidth = dstRect.xSpan();
            final int dstHeight = dstRect.ySpan();
            final int dstScanlineStride = dstWidth + random.nextInt(4);
            
            final GRect dstClip;
            if (random.nextBoolean()) {
                dstClip = dstRect.withBordersDeltasElseEmpty(1, 1, -1, -1);
            } else {
                dstClip = dstRect;
            }
            
            if (DEBUG) {
                System.out.println();
                System.out.println("k = " + k);
                System.out.println("parallelism = " + parallelizer.getParallelism());
                System.out.println("mustUseSmoothElseClosest = " + mustUseSmoothElseClosest);
                System.out.println("mustUseNullArr = " + mustUseNullArr);
                System.out.println("srcRect = " + srcRect);
                System.out.println("srcScanlineStride = " + srcScanlineStride);
                System.out.println("dstRect = " + dstRect);
                System.out.println("dstScanlineStride = " + dstScanlineStride);
                System.out.println("dstClip = " + dstClip);
            }
            
            final MyPixels input;
            if (mustUseNullArr) {
                input = inputNullArr;
            } else {
                input = new MyPixels();
            }
            input.reset(
                srcWidth,
                srcHeight,
                srcScanlineStride);
            input.randomize(random);
            
            final MyPixels actual = new MyPixels();
            actual.reset(
                dstWidth,
                dstHeight,
                dstScanlineStride);
            actual.configureTopLeft(dstRect.x(), dstRect.y());
            
            /*
             * Drawing, with bounds checks in MyPixels.
             */
            
            callDrawRectScaled(
                parallelizer,
                mustUseSmoothElseClosest,
                input,
                srcRect,
                dstRect,
                dstClip,
                actual);
            
            /*
             * Result check: mean color must be preserved.
             */
            
            if (srcRect.isEmpty()
                || dstRect.isEmpty()) {
                // Nothing to check.
            } else if (mustUseSmoothElseClosest
                && dstClip.equals(dstRect)) {
                
                final int srcMeanArgb32 = computeMeanArgb32(input, srcRect);
                final int dstMeanArgb32 = computeMeanArgb32(actual, dstRect);
                // This tolerance is enough for our tests.
                // Could have to be higher due to rounding errors.
                final int cptDeltaTol = 1;
                checkCloseArgb32(srcMeanArgb32, dstMeanArgb32, cptDeltaTol);
            } else {
                /*
                 * Can't check simply.
                 */
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void callDrawRectScaled_seq(
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        GRect dstRect,
        InterfaceRowDrawer rowDrawer) {
        final boolean mustUseSmoothElseClosest = true;
        final GRect dstClip = dstRect;
        ScaledRectDrawer.drawRectScaled(
            SequentialParallelizer.getDefault(),
            mustUseSmoothElseClosest,
            srcPixels,
            srcRect,
            dstRect,
            dstClip,
            rowDrawer);
    }
    
    private static void callDrawRectScaled_seq(
        boolean mustUseSmoothElseClosest,
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer rowDrawer) {
        ScaledRectDrawer.drawRectScaled(
            SequentialParallelizer.getDefault(),
            mustUseSmoothElseClosest,
            srcPixels,
            srcRect,
            dstRect,
            dstClip,
            rowDrawer);
    }

    private static void callDrawRectScaled(
        InterfaceParallelizer parallelizer,
        boolean mustUseSmoothElseClosest,
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer rowDrawer) {
        ScaledRectDrawer.drawRectScaled(
            TEST_MIN_AREA_COST_FOR_SPLIT_CLOSEST,
            TEST_MIN_AREA_COST_FOR_SPLIT_SMOOTH,
            parallelizer,
            mustUseSmoothElseClosest,
            srcPixels,
            srcRect,
            dstRect,
            dstClip,
            rowDrawer);
    }

    private static String toStringPixels(int[][] pixelArrArr) {
        final int height = pixelArrArr.length;
        final int width = pixelArrArr[0].length;
        final StringBuilder sb = new StringBuilder();
        for (int j = 0; j < height; j++) {
            sb.append("\n");
            final int[] pixelArr = pixelArrArr[j];
            for (int i = 0; i < width; i++) {
                if (i == 0) {
                    sb.append("[");
                } else {
                    sb.append(",");
                }
                sb.append(Argb32.toString(pixelArr[i]));
                if (i == width - 1) {
                    sb.append("]");
                }
            }
        }
        return sb.toString();
    }
    
    private static int computeMeanArgb32(
        MyPixels pixels,
        GRect rect) {
        if (rect.isEmpty()) {
            throw new IllegalArgumentException("rect must not be empty");
        }
        /*
         * fp values are biased
         * (twice less fp span for 0 and 255),
         * so we do mean on int values.
         */
        long a8Sum = 0;
        long r8Sum = 0;
        long g8Sum = 0;
        long b8Sum = 0;
        for (int y = rect.y(); y <= rect.yMax(); y++) {
            for (int x = rect.x(); x <= rect.xMax(); x++) {
                final int argb32 = pixels.getColor32At(x, y);
                a8Sum += Argb32.getAlpha8(argb32);
                r8Sum += Argb32.getRed8(argb32);
                g8Sum += Argb32.getGreen8(argb32);
                b8Sum += Argb32.getBlue8(argb32);
            }
        }
        final double divisor = 255.0 * rect.areaLong();
        return Argb32.toArgb32FromFp(
            a8Sum / divisor,
            r8Sum / divisor,
            g8Sum / divisor,
            b8Sum / divisor);
    }
    
    private static void checkCloseArgb32(
        int expectedArgb32,
        int actualArgb32,
        int cptDeltaTol) {
        {
            final int ve = Argb32.getAlpha8(expectedArgb32);
            final int va = Argb32.getAlpha8(actualArgb32);
            assertEquals(ve, va, cptDeltaTol);
        }
        {
            final int ve = Argb32.getRed8(expectedArgb32);
            final int va = Argb32.getRed8(actualArgb32);
            assertEquals(ve, va, cptDeltaTol);
        }
        {
            final int ve = Argb32.getGreen8(expectedArgb32);
            final int va = Argb32.getGreen8(actualArgb32);
            assertEquals(ve, va, cptDeltaTol);
        }
        {
            final int ve = Argb32.getBlue8(expectedArgb32);
            final int va = Argb32.getBlue8(actualArgb32);
            assertEquals(ve, va, cptDeltaTol);
        }
    }
}
