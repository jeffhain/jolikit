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
package net.jolikit.bwd.impl.awt;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import net.jolikit.test.utils.TestUtils;

/**
 * Main to check drawImage() accuracy and speed.
 */
public class BihDrawImageTestMain {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        log_drawImage_accuracy();
        
        log_drawImage_speed();
        
        System.out.println("DONE");
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BihDrawImageTestMain() {
    }
    
    private static void log_drawImage_accuracy() {
        
        System.out.println();
        System.out.println("log_drawImage_accuracy()");
        
        final int cptDeltaTol = 1;
        
        // Large enough to encounter all possible issues.
        final int width = 256;
        final int height = 256;
        
        /*
         * To simplify the output.
         * Tests fail if these don't hold hold.
         */
        final Set<Integer> ignoredImageTypeSet = new TreeSet<>(
            Arrays.asList(
                BufferedImage.TYPE_3BYTE_BGR,// same as for TYPE_INT_RGB
                BufferedImage.TYPE_4BYTE_ABGR,// same as for TYPE_INT_ARGB
                BufferedImage.TYPE_4BYTE_ABGR_PRE, // same as for TYPE_INT_ARGB_PRE
                BufferedImage.TYPE_INT_BGR)); // same as for TYPE_INT_RGB
        
        final Set<String> resStrSet = new TreeSet<>();
        
        for (BufferedImage srcImage : BihTestUtils.newImageList_allImageType(width, height)) {
            if (ignoredImageTypeSet.contains(srcImage.getType())) {
                continue;
            }
            final BufferedImageHelper srcHelper = new BufferedImageHelper(srcImage);
            
            // Randomizing input image
            // (always the same, to have same deltas
            // for equivalent image kinds).
            {
                final Random random = TestUtils.newRandom123456789L();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        final int argb32 = random.nextInt();
                        srcHelper.setNonPremulArgb32At(x, y, argb32);
                    }
                }
            }
            
            for (BufferedImage dstImage : BihTestUtils.newImageList_allImageType(width, height)) {
                if (ignoredImageTypeSet.contains(dstImage.getType())) {
                    continue;
                }
                final BufferedImageHelper dstHelper = new BufferedImageHelper(dstImage);
                
                final BufferedImageHelper expectedDstHelper =
                    BihTestUtils.newIdenticalImageAndHelper(dstHelper);
                
                BihTestUtils.copyImage_reference(
                    srcHelper,
                    0,
                    0,
                    expectedDstHelper,
                    0,
                    0,
                    width,
                    height);
                
                final Graphics g = dstImage.getGraphics();
                try {
                    g.drawImage(srcImage, 0, 0, null);
                } finally {
                    g.dispose();
                }
                
                final int maxCptDelta =
                    BihTestUtils.computeMaxCptDelta(
                        expectedDstHelper,
                        dstHelper);
                final String srcStr = BihTestUtils.toStringImageKind(srcImage);
                final String dstStr = BihTestUtils.toStringImageKind(dstImage);
                final String srcDstStr = srcStr + "->" + dstStr;
                final String resStr;
                if (maxCptDelta <= cptDeltaTol) {
                    resStr = srcDstStr + " : good (" + maxCptDelta + ")";
                } else {
                    resStr = srcDstStr + " : bad (" + maxCptDelta + ")";
                }
                final boolean didAdd = resStrSet.add(resStr);
                if (!didAdd) {
                    throw new AssertionError("dupe: " + resStr);
                }
            }
        }
        
        for (String resStr : resStrSet) {
            System.out.println(resStr);
        }
    }
    
    /*
     * 
     */
    
    private static void log_drawImage_speed() {
        
        System.out.println();
        System.out.println("log_drawImage_speed()");
        
        // Can bench both.
        // Code tuned for true, to keep things simple.
        final boolean optimGetSet = true;
        final double equivFactor = 1.2;
        
        // Large enough to take some time.
        final int width = 3840;
        final int height = 2160;
        // 1 warmup, 2 bench.
        final int nbrOfRuns = 3;
        
        final Set<String> resStrSet = new TreeSet<>();
        
        // All images types and pixel formats.
        for (BufferedImage srcImage : BihTestUtils.newImageList_forBench(width, height)) {
            final BufferedImageHelper srcHelper = new BufferedImageHelper(
                srcImage,
                optimGetSet,
                optimGetSet);
            
            // Randomizing input image.
            {
                final Random random = TestUtils.newRandom123456789L();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        final int argb32 = random.nextInt();
                        srcHelper.setNonPremulArgb32At(x, y, argb32);
                    }
                }
            }
            
            for (BufferedImage dstImage : BihTestUtils.newImageList_forBench(width, height)) {
                final BufferedImageHelper dstHelper = new BufferedImageHelper(
                    dstImage,
                    optimGetSet,
                    optimGetSet);
                
                if (!BufferedImageHelper.isDrawImageMaxCptDeltaSmallEnough(
                    BufferedImageHelper.getDrawImageMaxCptDelta(
                        srcImage.getType(),
                        srcHelper.getPixelFormat(),
                        srcImage.isAlphaPremultiplied(),
                        //
                        dstImage.getType(),
                        dstHelper.getPixelFormat(),
                        dstImage.isAlphaPremultiplied()))) {
                    // No need to bench.
                    continue;
                }
                
                long minDtNsPixelLoop = Long.MAX_VALUE;
                for (int k = 0; k < nbrOfRuns; k++) {
                    long a = System.nanoTime();
                    BihTestUtils.copyImage_reference(
                        srcHelper,
                        0,
                        0,
                        dstHelper,
                        0,
                        0,
                        width,
                        height);
                    long b = System.nanoTime();
                    minDtNsPixelLoop = Math.min(minDtNsPixelLoop, b-a);
                }
                
                long minDtNsDrawImage = Long.MAX_VALUE;
                for (int k = 0; k < nbrOfRuns; k++) {
                    long a = System.nanoTime();
                    final Graphics g = dstImage.getGraphics();
                    try {
                        g.drawImage(srcImage, 0, 0, null);
                    } finally {
                        g.dispose();
                    }
                    long b = System.nanoTime();
                    minDtNsDrawImage = Math.min(minDtNsDrawImage, b-a);
                }
                
                final float dtSPixelLoop = (float) (minDtNsPixelLoop / (1000 * 1000) / 1e3);
                final float dtSDrawImage = (float) (minDtNsDrawImage / (1000 * 1000) / 1e3);
                final float speedUp = (dtSPixelLoop / dtSDrawImage);

                final String srcStr = BihTestUtils.toStringImageKind(srcImage);
                final String dstStr = BihTestUtils.toStringImageKind(dstImage);
                final String srcDstStr = srcStr + "->" + dstStr;
                final String resStr;
                if (dtSDrawImage < dtSPixelLoop / equivFactor) {
                    resStr = srcDstStr + " : drawImage speedUp = " + speedUp;
                } else if (dtSDrawImage < dtSPixelLoop * equivFactor) {
                    resStr = srcDstStr + " : drawImage speedUp (equivvvvvvvalent) = " + speedUp;
                } else {
                    resStr = srcDstStr + " : drawImage speedUp (slowwwwwwwwwwwwwwwwwwwwwer) = " + speedUp;
                }
                System.out.println(resStr);
                final boolean didAdd = resStrSet.add(resStr);
                if (!didAdd) {
                    throw new AssertionError("dupe: " + resStr);
                }
            }
        }
        
        System.out.println("#");
        System.out.println("# SORTED:");
        System.out.println("#");
        for (String resStr : resStrSet) {
            System.out.println(resStr);
        }
    }
}
