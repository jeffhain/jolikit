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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class JoglBwdImageFromFile extends AbstractJoglBwdImage {

    /*
     * TODO jogl Tried to use FreeImage 3.17 library,
     * but had trouble loading it with JNA,
     * so using AWT for now.
     */

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
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
    public JoglBwdImageFromFile(
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
        
        final GRect box = this.getRect();

        final int pixelCapacity = box.area();
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
    protected void disposeImpl() {
        // Nothing to do.
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    @Override
    int[] getColor32Arr() {
        return this.color32Arr;
    }
}
