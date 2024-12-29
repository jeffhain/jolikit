/*
 * Copyright 2019-2024 Jeff Hain
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
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class AwtBwdImageFromFile extends AbstractAwtBwdImage {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final BufferedImageHelper bufferedImageHelper;

    private final int[] premulArgb32Arr;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param filePath Path of an image file.
     * @param disposalListener Must not be null.
     * @throws NullPointerException if filePath or disposalListener is null.
     * @throws IllegalArgumentException if could not load the specified image.
     */
    public AwtBwdImageFromFile(
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
            // Can happen.
            throw new IllegalArgumentException("could not load image at " + filePath);
        }
        this.bufferedImageHelper = new BufferedImageHelper(readImage);
        
        final int width = readImage.getWidth();
        final int height = readImage.getHeight();
        this.setWidth_final(width);
        this.setHeight_final(height);
        
        final GRect box = this.getRect();

        final int pixelCapacity = box.area();
        final int[] color32Arr = new int[pixelCapacity];
        final int color32ArrScanlineStride = width;

        final BihPixelFormat pixelFormat = BihPixelFormat.ARGB32;
        final boolean premul = true;
        this.bufferedImageHelper.getPixelsInto(
            0,
            0,
            //
            color32Arr,
            color32ArrScanlineStride,
            pixelFormat,
            premul,
            0,
            0,
            //
            width,
            height);
        this.premulArgb32Arr = color32Arr;
    }

    @Override
    public BufferedImage getBufferedImage() {
        return this.bufferedImageHelper.getImage();
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
    int[] getPremulArgb32Arr() {
        return this.premulArgb32Arr;
    }
}
