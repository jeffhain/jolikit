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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.BindingPrlUtils;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;

abstract class AbstractScaledRectDrawerTezt extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    protected static final boolean DEBUG = false;
    
    /**
     * Enough to have a concurrent mess,
     * not too much to keep things simple.
     */
    private static final int PARALLELISM = 4;
    
    private static final int NBR_OF_CALLS_FOR_STURDINESS = 10 * 1000;
    
    /*
     * Large enough to have appreciable
     * upscalings and downscalings.
     */
    private static final int MAX_SPAN_FOR_STURDINESS = 30;

    protected static final List<InterfaceColorTypeHelper> CTH_LIST;
    static {
        final List<InterfaceColorTypeHelper> list = new ArrayList<>();
        for (boolean premul : new boolean[] {false,true}) {
            list.add(ScaledRectTestUtils.getColorTypeHelper(premul));
        }
        CTH_LIST = Collections.unmodifiableList(list);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED CLASSES
    //--------------------------------------------------------------------------
    
    protected static class MyPixels
    extends IntArrSrcPixels
    implements InterfaceRowDrawer {
        public MyPixels() {
        }
        public void reset(
            int width,
            int height,
            int scanlineStride) {
            this.configure(
                width,
                height,
                new int[scanlineStride * height],
                scanlineStride);
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
        @Override
        public String toString() {
            return ScaledRectTestUtils.toStringPixels(this.getPixels());
        }
        @Override
        public int hashCode() {
            final GRect rect = this.getRect();
            final int width = this.getWidth();
            final int height = this.getHeight();
            final int[] color32Arr = this.color32Arr();
            final int scanlineStride = this.getScanlineStride();
            
            final int prime = 31;
            int hc = rect.hashCode();
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
            
            final int[] color32Arr2 = other.color32Arr();
            final int scanlineStride2 = other.getScanlineStride();
            
            boolean foundDiff = false;
            if (!this.getRect().equals(other.getRect())) {
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
        public void randomize(
            Random random,
            InterfaceColorTypeHelper colorTypeHelper,
            int minAlpha8) {
            final GRect rect = this.getRect();
            final int w = rect.xSpan();
            final int h = rect.ySpan();
            for (int j = 0; j < h; j++) {
                final int y = rect.y() + j;
                for (int i = 0; i < w; i++) {
                    final int x = rect.x() + i;
                    int argb32 = random.nextInt();
                    if (Argb32.getAlpha8(argb32) < minAlpha8) {
                        argb32 = Argb32.withAlpha8(argb32, minAlpha8);
                    }
                    final int color32 =
                        colorTypeHelper.asTypeFromNonPremul32(
                            argb32);
                    super.setColor32At(x, y, color32);
                }
            }
        }
        public int getMinAlpha8() {
            int ret = 0xFF;
            // super.color32Arr() never null
            for (int argb32 : super.color32Arr()) {
                ret = Math.min(ret, Argb32.getAlpha8(argb32));
            }
            return ret;
        }
        @Override
        public int getColor32At(int x, int y) {
            this.checkInRange(x, y);
            return super.getColor32At(x, y);
        }
        @Override
        public void setColor32At(int x, int y, int color32) {
            this.checkInRange(x, y);
            super.setColor32At(x, y, color32);
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
        public int[][] getPixels() {
            final GRect rect = this.getRect();
            final int w = rect.xSpan();
            final int h = rect.ySpan();
            
            final int[][] pixelArrArr = new int[h][];
            for (int j = 0; j < h; j++) {
                final int[] pixelArr = new int[w];
                pixelArrArr[j] = pixelArr;
                final int y = rect.y() + j;
                for (int i = 0; i < w; i++) {
                    final int x = rect.x() + i;
                    pixelArr[i] = this.getColor32At(x, y);
                }
            }
            return pixelArrArr;
        }
        private void checkInRange(int x, int y) {
            final GRect rect = this.getRect();
            if (!rect.contains(x, y)) {
                throw new IllegalArgumentException(
                    "(" + x + "," + y + ") not in " + rect);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    protected static final InterfaceParallelizer PRLZR =
        BindingPrlUtils.newParallelizer(
            new BaseBwdBindingConfig(),
            PARALLELISM,
            AbstractScaledRectDrawerTezt.class.getClass().getSimpleName());
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractScaledRectDrawerTezt() {
    }
    
    /*
     * 
     */
    
    public void test_drawRectScaled_identity() {
        
        // Scanline stride must have "no effect".
        @SuppressWarnings("unused")
        int scanlineStride;
        
        // Opaque, for same values whether or not premul.
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
        
        final GRect srcRect = GRect.valueOf(0, 0, 2, 2);
        final GRect dstRect = GRect.valueOf(0, 0, 2, 2);
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
     * Shifted, other dimensions, scanlineStride,
     * but no scaling.
     */
    public void test_drawRectScaled_identity_shifted() {
        
        // Scanline stride must have "no effect".
        @SuppressWarnings("unused")
        int scanlineStride;
        
        // Opaque, for same values whether or not premul.
        final MyPixels input = new MyPixels();
        input.setTopLeft(-10, -20);
        input.setPixels(new int[][] {
            {0, 0, 0, 0, 0},
            {0, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0},
            {0, 0xFF808000, 0xFF008080, 0xFF800080, 0},
            {0, 0xFFF0F000, 0xFF00F0F0, 0xFFF000F0, 0},
            {0, 0xFF01000, 0xFF000100, 0xFF000001, 0},
            {0, 0, 0, 0, 0},
        }, scanlineStride = 7);
        
        final MyPixels expected = new MyPixels();
        expected.setTopLeft(-30, -40);
        expected.setPixels(new int[][] {
            {0xFFFF0000, 0xFF00FF00, 0xFF0000FF},
            {0xFF808000, 0xFF008080, 0xFF800080},
            {0xFFF0F000, 0xFF00F0F0, 0xFFF000F0},
            {0xFF01000, 0xFF000100, 0xFF000001},
        }, scanlineStride = 8);
        
        final MyPixels actual = new MyPixels();
        // rect is not set by scaling computation,
        // so must just use same as expected one.
        actual.reset(
            expected.getRect(),
            scanlineStride = 9);
        
        final GRect srcRect = GRect.valueOf(-10+1, -20+1, 3, 4);
        final GRect dstRect = GRect.valueOf(-30, -40, 3, 4);
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
    
    public void test_drawRectScaled_sturdiness_opaque_nonPremul_seq() {
        final boolean opaque = true;
        final boolean premul = false;
        final boolean prlElseSeq = false;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_opaque_premul_seq() {
        final boolean opaque = true;
        final boolean premul = true;
        final boolean prlElseSeq = false;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_transp_nonPremul_seq() {
        final boolean opaque = false;
        final boolean premul = false;
        final boolean prlElseSeq = false;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_transp_premul_seq() {
        final boolean opaque = false;
        final boolean premul = true;
        final boolean prlElseSeq = false;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_opaque_nonPremul_prl() {
        final boolean opaque = true;
        final boolean premul = false;
        final boolean prlElseSeq = true;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_opaque_premul_prl() {
        final boolean opaque = true;
        final boolean premul = true;
        final boolean prlElseSeq = true;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_transp_nonPremul_prl() {
        final boolean opaque = false;
        final boolean premul = false;
        final boolean prlElseSeq = true;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_transp_premul_prl() {
        final boolean opaque = false;
        final boolean premul = true;
        final boolean prlElseSeq = true;
        this.test_drawRectScaled_sturdiness_xxx(
            opaque,
            premul,
            prlElseSeq);
    }

    public void test_drawRectScaled_sturdiness_xxx(
        boolean opaque,
        boolean premul,
        boolean prlElseSeq) {
        
        @SuppressWarnings("unused")
        int scanlineStride;
        
        final InterfaceColorTypeHelper colorTypeHelper =
            ScaledRectTestUtils.getColorTypeHelper(premul);
        
        final Random random = TestUtils.newRandom123456789L();
        
        final int nbrOfCalls = NBR_OF_CALLS_FOR_STURDINESS;
        final int minDstPos = -3;
        final int minPos = -2;
        final int maxPos = 3;
        final int maxSpan = MAX_SPAN_FOR_STURDINESS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            final InterfaceParallelizer parallelizer;
            if (prlElseSeq) {
                parallelizer = PRLZR;
            } else {
                parallelizer = SequentialParallelizer.getDefault();
            }
            // If null, scaling can only read src pixels
            // using getColor32At().
            final boolean mustUseNullArr = random.nextBoolean();
            
            // Sometimes negative top-left coordinates (now supported).
            final GRect srcRect = GRect.valueOf(
                minPos + random.nextInt((maxPos - minPos) + 1),
                minPos + random.nextInt((maxPos - minPos) + 1),
                random.nextInt(maxSpan + 1),
                random.nextInt(maxSpan + 1));
            // input needs to cover srcRect.
            final int inputX = srcRect.x() - random.nextInt(3);
            final int inputY = srcRect.y() - random.nextInt(3);
            final int inputWidth =
                (srcRect.x() - inputX) + srcRect.xSpan() + random.nextInt(3);
            final int inputHeight =
                (srcRect.y() - inputY) + srcRect.ySpan() + random.nextInt(3);
            final GRect inputRect = GRect.valueOf(
                inputX,
                inputY,
                inputWidth,
                inputHeight);
            final int inputScanlineStride = inputWidth + random.nextInt(3);
            
            final GRect dstRect = GRect.valueOf(
                minDstPos + random.nextInt(maxPos - minDstPos + 1),
                minDstPos + random.nextInt(maxPos - minDstPos + 1),
                random.nextInt(maxSpan + 1),
                random.nextInt(maxSpan + 1));
            // dst just matching dstRect.
            final int dstWidth = dstRect.xSpan();
            final int dstScanlineStride = dstWidth + random.nextInt(3);
            
            final GRect dstClip;
            if (random.nextBoolean()) {
                dstClip = dstRect.withBordersDeltasElseEmpty(1, 1, -1, -1);
            } else {
                dstClip = dstRect;
            }
            
            if (DEBUG) {
                System.out.println();
                System.out.println("i = " + i);
                System.out.println("parallelism = " + parallelizer.getParallelism());
                System.out.println("mustUseNullArr = " + mustUseNullArr);
                System.out.println("inputRect = " + inputRect);
                System.out.println("inputScanlineStride = " + inputScanlineStride);
                System.out.println("srcRect = " + srcRect);
                System.out.println("srcScanlineStride = " + inputScanlineStride);
                System.out.println("dstRect = " + dstRect);
                System.out.println("dstScanlineStride = " + dstScanlineStride);
                System.out.println("dstClip = " + dstClip);
            }
            
            final MyPixels input;
            if (mustUseNullArr) {
                input = new MyPixels() {
                    @Override
                    public int[] color32Arr() {
                        return null;
                    }
                    @Override
                    public int getScanlineStride() {
                        return 0;
                    }
                };
            } else {
                input = new MyPixels();
            }
            input.reset(
                inputRect,
                inputScanlineStride);
            final int minAlpha8;
            if (opaque) {
                minAlpha8 = 0xFF;
            } else {
                // Any alpha <= 0xFE.
                minAlpha8 = random.nextInt(0xFF);
            }
            input.randomize(
                random,
                colorTypeHelper,
                minAlpha8);
            
            final MyPixels actual = new MyPixels();
            actual.reset(
                dstRect,
                dstScanlineStride);
            
            /*
             * Drawing, with bounds checks in MyPixels.
             */
            
            this.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                input,
                srcRect,
                //
                dstRect,
                dstClip,
                actual);
            
            /*
             * Result check.
             */
            
            this.checkSturdinessResult(
                colorTypeHelper,
                //
                input,
                srcRect,
                //
                dstRect,
                dstClip,
                actual);
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected abstract void drawScaledRect(
        InterfaceParallelizer parallelizer,
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer);
    
    protected abstract void checkSturdinessResult(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        MyPixels input,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        MyPixels actual);
    
    /*
     * 
     */

    protected void drawScaledRect_seq(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer) {
        this.drawScaledRect(
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
    
    /*
     * 
     */
    
    protected void drawScaledRectPremulAndNotAndCheckEqual_seq(
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        MyPixels dstActual,
        //
        MyPixels expected) {
        
        for (InterfaceColorTypeHelper colorTypeHelper : CTH_LIST) {
            
            if (DEBUG) {
                System.out.println();
                System.out.println("srcPixels = " + srcPixels);
                System.out.println("srcRect = " + srcRect);
                System.out.println("dstRect = " + dstRect);
                System.out.println("dstClip = " + dstClip);
                System.out.println("colorTypeHelper = " + colorTypeHelper);
            }
            
            drawScaledRect_seq(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                //
                dstRect,
                dstClip,
                dstActual);
            
            if (DEBUG) {
                System.out.println("expected = " + expected);
                System.out.println("dstActual = " + dstActual);
            }
            
            assertEquals(expected, dstActual);
        }
    }
    
    protected static void checkMeanColorPreserved(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels input,
        GRect srcRect,
        //
        InterfaceSrcPixels actual,
        GRect dstRect,
        //
        int cptDeltaTol) {
        
        final int srcMeanColor32 =
            ScaledRectTestUtils.computeMeanColor32(
                colorTypeHelper,
                input,
                srcRect);
        
        final int dstMeanColor32 =
            ScaledRectTestUtils.computeMeanColor32(
                colorTypeHelper,
                actual,
                dstRect);
        
        ScaledRectTestUtils.checkCloseColor32(
            srcMeanColor32,
            dstMeanColor32,
            cptDeltaTol);
    }
}
