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
package net.jolikit.bwd.impl.awt;

import java.awt.image.BufferedImage;
import java.util.Random;

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.test.utils.TestUtils;

public class BufferedImageHelperPerfs {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_RUNS = 2;
    private static final int NBR_OF_CALLS = 10 * 1000 * 1000;
    
    /*
     * Small image, for all array to stay in cache
     * and just bench computations.
     */
    
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        newRun(args);
    }
    
    public static void newRun(String[] args) {
        new BufferedImageHelperPerfs().run(args);
    }
    
    public BufferedImageHelperPerfs() {
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run(String[] args) {
        final long a = System.nanoTime();
        System.out.println("--- " + BufferedImageHelperPerfs.class.getSimpleName() + "... ---");
        
        bench_getArgb32At_nonPremul();
        
        bench_getArgb32At_premul();
        
        bench_setArgb32At_nonPremul();
        
        bench_setArgb32At_premul();
        
        bench_drawPointPremulAt();
        
        bench_getPixelsInto_128_128();
        
        bench_getPixelsInto_1024_1024();
        
        bench_getPixelsInto_4096_2048();
        
        final long b = System.nanoTime();
        System.out.println("--- ..." + BufferedImageHelperPerfs.class.getSimpleName()
            + ", " + TestUtils.nsToSRounded(b-a) + " s ---");
    }
    
    /*
     * 
     */
    
    private static void bench_getArgb32At_nonPremul() {
        bench_getArgb32At_xxx(false);
    }
    
    private static void bench_getArgb32At_premul() {
        bench_getArgb32At_xxx(true);
    }
    
    private static void bench_getArgb32At_xxx(boolean getPremulElseNonPremul) {
        
        final int width = WIDTH;
        final int height = HEIGHT;
        
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            
            System.out.println();
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            final int imageType = image.getType();
            final ImageTypeEnum imageTypeEnum;
            if (pixelFormat != null) {
                imageTypeEnum = null;
            } else {
                imageTypeEnum = ImageTypeEnum.enumByType().get(imageType);
            }
            
            final boolean hasAlpha =
                (pixelFormat != null)
                ? pixelFormat.hasAlpha()
                    : imageTypeEnum.hasAlpha();
            
            for (boolean withAlphaPixels : BihTestUtils.newBooleanArr(hasAlpha)) {
                {
                    final Random random = TestUtils.newRandom123456789L();
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int argb = random.nextInt();
                            if (!withAlphaPixels) {
                                argb = Argb32.toOpaque(argb);
                            }
                            image.setRGB(x, y, argb);
                        }
                    }
                }
                
                for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                    
                    for (int k = 0; k < NBR_OF_RUNS; k++) {
                        int x = 0;
                        int y = 0;
                        int antiOptim = 0;
                        final long a = System.nanoTime();
                        for (int i = 0; i < NBR_OF_CALLS; i++) {
                            if (++x == width) {
                                x = 0;
                                if (++y == height) {
                                    y = 0;
                                }
                            }
                            final int read;
                            if (getPremulElseNonPremul) {
                                read = helper.getPremulArgb32At(x, y);
                            } else {
                                read = helper.getNonPremulArgb32At(x, y);
                            }
                            antiOptim += read;
                        }
                        final long b = System.nanoTime();
                        if (antiOptim == 0) {
                            System.out.println("rare");
                        }
                        final String typeStr;
                        if (pixelFormat != null) {
                            typeStr = pixelFormat + (imagePremul ? "_P" : "");
                        } else {
                            typeStr = imageTypeEnum.toString();
                        }
                        final String methodStr;
                        if (getPremulElseNonPremul) {
                            methodStr = "getP()";
                        } else {
                            methodStr = "getNp()";
                        }
                        System.out.println(NBR_OF_CALLS + " calls"
                            + ", " + methodStr
                            + ", " + typeStr
                            + (withAlphaPixels ? ", (a-pix)" : "")
                            + (helper.isColorModelAvoided() ? ", (CM bypass)" : "")
                            + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static void bench_setArgb32At_nonPremul() {
        bench_setArgb32At_drawPointPremulAt_xxx(false, true);
    }
    
    private static void bench_setArgb32At_premul() {
        bench_setArgb32At_drawPointPremulAt_xxx(true, true);
    }
    
    private static void bench_drawPointPremulAt() {
        bench_setArgb32At_drawPointPremulAt_xxx(true, false);
    }
    
    private static void bench_setArgb32At_drawPointPremulAt_xxx(
        boolean setPremulElseNonPremul,
        boolean setElseDraw) {
        
        if ((!setElseDraw)
            && (!setPremulElseNonPremul)) {
            // draw only accepts premul.
            throw new IllegalArgumentException();
        }
        
        final int width = WIDTH;
        final int height = HEIGHT;
        
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            
            System.out.println();
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            final int imageType = image.getType();
            final ImageTypeEnum imageTypeEnum;
            if (pixelFormat != null) {
                imageTypeEnum = null;
            } else {
                imageTypeEnum = ImageTypeEnum.enumByType().get(imageType);
            }
            
            final boolean hasAlpha =
                (pixelFormat != null)
                ? pixelFormat.hasAlpha()
                    : imageTypeEnum.hasAlpha();
            
            for (boolean withAlphaPixels : BihTestUtils.newBooleanArr(hasAlpha)) {
                
                final int[] toSetArr = new int[width * height];
                {
                    final Random random = TestUtils.newRandom123456789L();
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int argb = random.nextInt();
                            if (!withAlphaPixels) {
                                argb = Argb32.toOpaque(argb);
                            }
                            if (setPremulElseNonPremul) {
                                argb = BindingColorUtils.toPremulAxyz32(argb);
                            }
                            toSetArr[y * width + x] = argb;
                        }
                    }
                }
                
                for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                    
                    for (int k = 0; k < NBR_OF_RUNS; k++) {
                        int x = 0;
                        int y = 0;
                        final long a = System.nanoTime();
                        for (int i = 0; i < NBR_OF_CALLS; i++) {
                            if (++x == width) {
                                x = 0;
                                if (++y == height) {
                                    y = 0;
                                }
                            }
                            final int toSet = toSetArr[y * width + x];
                            if (setPremulElseNonPremul) {
                                if (setElseDraw) {
                                    helper.setPremulArgb32At(x, y, toSet);
                                } else {
                                    helper.drawPointPremulAt(x, y, toSet);
                                }
                            } else {
                                helper.setNonPremulArgb32At(x, y, toSet);
                            }
                        }
                        final long b = System.nanoTime();
                        final String typeStr;
                        if (pixelFormat != null) {
                            typeStr = pixelFormat + (imagePremul ? "_P" : "");
                        } else {
                            typeStr = imageTypeEnum.toString();
                        }
                        final String methodStr;
                        if (setPremulElseNonPremul) {
                            if (setElseDraw) {
                                methodStr = "setP()";
                            } else {
                                methodStr = "drawP()";
                            }
                        } else {
                            methodStr = "setNp()";
                        }
                        System.out.println(NBR_OF_CALLS + " calls"
                            + ", " + methodStr
                            + ", " + typeStr
                            + (withAlphaPixels ? ", (a-pix)" : "")
                            + (helper.isColorModelAvoided() ? ", (CM bypass)" : "")
                            + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static void bench_getPixelsInto_128_128() {
        bench_getPixelsInto_xxx(1000, 128, 128);
    }
    
    private static void bench_getPixelsInto_1024_1024() {
        bench_getPixelsInto_xxx(10, 1024, 1024);
    }
    
    private static void bench_getPixelsInto_4096_2048() {
        bench_getPixelsInto_xxx(1, 4096, 2048);
    }
    
    private static void bench_getPixelsInto_xxx(
        int bulkNbrOfCalls,
        int width,
        int height) {
        
        final int color32ArrScanlineStride = width;
        final int[] color32Arr = new int[color32ArrScanlineStride * height];
        
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            
            System.out.println();
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            final int imageType = image.getType();
            final ImageTypeEnum imageTypeEnum;
            if (pixelFormat != null) {
                imageTypeEnum = null;
            } else {
                imageTypeEnum = ImageTypeEnum.enumByType().get(imageType);
            }
            
            // Randomizing image.
            {
                final Random random = TestUtils.newRandom123456789L();
                final BufferedImageHelper helperForSet =
                    new BufferedImageHelper(image);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        // Not bothering to bench alpha (not a common case).
                        final int argb = Argb32.toOpaque(random.nextInt());
                        helperForSet.setNonPremulArgb32At(x, y, argb);
                    }
                }
            }
            
            for (BihPixelFormat pixelFormatTo : new BihPixelFormat[] {
                BihPixelFormat.XRGB24,
                BihPixelFormat.ARGB32,
            }) {
                for (boolean premulTo : BihTestUtils.newPremulArr(pixelFormatTo)) {
                    
                    for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                        
                        for (int k = 0; k < NBR_OF_RUNS; k++) {
                            int antiOptim = 0;
                            final long a = System.nanoTime();
                            for (int i = 0; i < bulkNbrOfCalls; i++) {
                                helper.getPixelsInto(
                                    color32Arr,
                                    color32ArrScanlineStride,
                                    pixelFormatTo,
                                    premulTo);
                                antiOptim += color32Arr[0];
                            }
                            final long b = System.nanoTime();
                            if (antiOptim == 0) {
                                System.out.println("rare");
                            }
                            final String typeStr;
                            if (pixelFormat != null) {
                                typeStr = pixelFormat + (imagePremul ? "_P" : "");
                            } else {
                                typeStr = imageTypeEnum.toString();
                            }
                            final String typeToStr = pixelFormatTo + (premulTo ? "_P" : "");
                            final String methodStr = "getPix()(" + width + "x" + height + ")";
                            System.out.println(bulkNbrOfCalls + " call"
                                + (bulkNbrOfCalls >= 2 ? "s" : "")
                                + ", " + methodStr
                                + ", " + typeStr
                                + "->" + typeToStr
                                + (helper.isColorModelAvoided() ? ", (CM bypass)" : "")
                                + ", took " + TestUtils.nsToSRounded(b-a) + " s");
                        }
                    }
                }
            }
        }
    }
}
