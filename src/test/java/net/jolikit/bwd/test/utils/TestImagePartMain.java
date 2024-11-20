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
package net.jolikit.bwd.test.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;

/**
 * Tool to create an image being a rectangle
 * of an input image.
 */
public class TestImagePartMain {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Empty means no output.
     */
    private static final GRect PART_RECT = GRect.valueOf(0, 0, 0, 0);
    
    private static final String INPUT_IMG_PATH =
        BwdTestResources.TEST_IMG_FILE_PATH_LOREM_PNG;
    private static final String OUTPUT_IMG_PATH = toOutputImgPath(INPUT_IMG_PATH, PART_RECT);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        if (PART_RECT.isEmpty()) {
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
        /*
         * 
         */
        final int dw = PART_RECT.xSpan();
        final int dh = PART_RECT.ySpan();
        final int[] dstBiArr = new int[dw * dh];
        final BufferedImage dstBi =
            BufferedImageHelper.newBufferedImageWithIntArray(
                dstBiArr,
                dw,
                dh,
                BufferedImage.TYPE_INT_ARGB);
        for (int dy = 0; dy < dh; dy++) {
            for (int dx = 0; dx < dw; dx++) {
                final int sx = PART_RECT.x() + dx;
                final int sy = PART_RECT.y() + dy;
                final int argb32 = srcImg.getArgb32At(sx, sy);
                dstBiArr[dw * dy + dx] = argb32;
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
    
    private static String toOutputImgPath(String inputImgPath, GRect partRect) {
        final int lastDotIndex = inputImgPath.lastIndexOf('.');
        final String base = inputImgPath.substring(0, lastDotIndex);
        final String dotExt = inputImgPath.substring(lastDotIndex);
        return base
            + "_"
            + partRect.x()
            + "_"
            + partRect.y()
            + "_"
            + partRect.xSpan()
            + "_"
            + partRect.ySpan()
            + dotExt;
    }
}
