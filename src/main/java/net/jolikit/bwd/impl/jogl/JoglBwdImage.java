/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.jogl;

import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.NumbersUtils;

public class JoglBwdImage extends AbstractBwdImage {

    /*
     * TODO jogl Tried to use FreeImage 3.17 library,
     * but had trouble loading it with JNA,
     * so using AWT for now.
     */

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * TODO jogl For performances when drawing image on graphics,
     * we store its content as an array of premultiplied colors.
     * Note that as a result, in getArgb32AtImpl(...), we compute
     * the non-premultiplied color from a premultiplied one,
     * which can damage RGB components in case of semi transparent images.
     * 
     * ABGR if native is little (which will give RGBA in little,
     * Java being in big), else RGBA, with 8/8/8/8 bits.
     */
    private final int[] color32Arr;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param filePath Path of an image file.
     * @param disposalListener Must not be null.
     * @throws NullPointerException if filePath or disposalListener is null.
     * @throws IllegalArgumentException if could not load the specified image.
     */
    public JoglBwdImage(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        final File file = new File(filePath);
        final BufferedImage readImage;
        try {
            readImage = ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("could not load image at " + filePath, e);
        }
        if (readImage == null) {
            throw new IllegalArgumentException("could not load image at " + filePath);
        }
        
        final ImageObserver observer = null;
        final int width = readImage.getWidth(observer);
        final int height = readImage.getHeight(observer);
        this.setWidth_final(width);
        this.setHeight_final(height);

        final int pixelCapacity = NumbersUtils.timesExact(width, height);
        final int[] color32Arr = new int[pixelCapacity];
        final int color32ArrScanlineStride = width;
        
        final BufferedImageHelper bufferedImageHelper =
                new BufferedImageHelper(readImage);
        bufferedImageHelper.getPixelsInto(
                color32Arr,
                color32ArrScanlineStride,
                BufferedImageHelper.NATIVE_RGBA32_PIXEL_FORMAT,
                BufferedImageHelper.PREMUL);
        this.color32Arr = color32Arr;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int index = y * this.getWidth() + x;
        final int premulColor32 = this.color32Arr[index];
        final int argb32 = JoglPaintHelper.getArgb32FromArrayColor32(premulColor32);
        return argb32;
    }

    @Override
    protected void disposeImpl() {
        // Nothing to do.
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * ABGR if native is little, else RGBA.
     * 
     * @return For read only purpose.
     */
    int[] getColor32Arr() {
        return this.color32Arr;
    }
}
