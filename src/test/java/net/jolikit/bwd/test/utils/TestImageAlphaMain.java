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
package net.jolikit.bwd.test.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;

/**
 * Tool to create an image with alpha going
 * top to bottom from 255 to 0, from an input image.
 */
public class TestImageAlphaMain {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private enum MyAlphaType {
        /**
         * For nothing to be done.
         */
        NO_STATEMENT,
        /**
         * Top opaque, bottom transparent.
         */
        TRANSP_DOWN,
        /**
         * Vertical lines top to bottom and bottom to top.
         */
        TRANSP_DOWN_AND_UP_LINES,
    }
    
    private static final MyAlphaType ALPHA_TYPE = MyAlphaType.NO_STATEMENT;
        
    private static final String INPUT_IMG_PATH =
        BwdTestResources.TEST_IMG_FILE_PATH_TIME_SQUARE_PNG;
    private static final String OUTPUT_IMG_PATH =
        BwdTestResources.TEST_IMG_FILE_PATH_TIME_SQUARE_ALPHA_COLUMNS_PNG;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        if (ALPHA_TYPE == MyAlphaType.NO_STATEMENT) {
            System.out.println("nothing to do");
            return;
        }
        
        final InterfaceBwdBinding binding =
            BwdTestUtils.getDefaultBinding();
        binding.getUiThreadScheduler().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runImpl(binding);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    binding.shutdownAbruptly();
                }
            }
        });
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void runImpl(final InterfaceBwdBinding binding) {
        final InterfaceBwdImage srcImg =
            binding.newImage(INPUT_IMG_PATH);
        final int sw = srcImg.getWidth();
        final int sh = srcImg.getHeight();
        /*
         * 
         */
        final int[] dstBiArr = new int[sw * sh];
        final int scanlineStride = sw;
        final BufferedImage dstBi =
            BufferedImageHelper.newBufferedImageWithIntArray(
                dstBiArr,
                scanlineStride,
                sw,
                sh,
                BufferedImage.TYPE_INT_ARGB);
        if (ALPHA_TYPE == MyAlphaType.TRANSP_DOWN) {
            for (int y = 0; y < sh; y++) {
                final double ratio;
                if (sh == 1) {
                    ratio = 0.5;
                } else {
                    ratio = (sh - 1 - y) / (double) (sh - 1);
                }
                final int alpha8 = Argb32.toInt8FromFp(ratio);
                for (int x = 0; x < sw; x++) {
                    final int srcArgb32 = srcImg.getArgb32At(x, y);
                    final int dstArgb32 = Argb32.withAlpha8(srcArgb32, alpha8);
                    dstBiArr[sw * y + x] = dstArgb32;
                }
            }
        } else {
            boolean flipFlop = false;
            for (int x = 0; x < sw; x++) {
                flipFlop = !flipFlop;
                for (int y = 0; y < sh; y++) {
                    final double ratio;
                    if (sh == 1) {
                        ratio = 0.5;
                    } else {
                        if (flipFlop) {
                            // Transparent down.
                            ratio = (sh - 1 - y) / (double) (sh - 1);
                        } else {
                            // Transparent up.
                            ratio = y / (double) (sh - 1);
                        }
                    }
                    final int alpha8 = Argb32.toInt8FromFp(ratio);
                    final int srcArgb32 = srcImg.getArgb32At(x, y);
                    final int dstArgb32 = Argb32.withAlpha8(srcArgb32, alpha8);
                    dstBiArr[sw * y + x] = dstArgb32;
                }
            }
        }
        /*
         * 
         */
        final File dstFile = new File(OUTPUT_IMG_PATH);
        final String dstFileName = dstFile.getName();
        final int dstFileLastDotIndex = dstFileName.lastIndexOf('.');
        final String formatName =
            dstFileName.substring(dstFileLastDotIndex + 1);
        try {
            System.out.println("writing " + dstFile);
            ImageIO.write(dstBi, formatName, dstFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dstFile.setLastModified(System.currentTimeMillis());
    }
}
