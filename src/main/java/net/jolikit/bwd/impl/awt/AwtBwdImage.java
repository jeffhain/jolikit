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
package net.jolikit.bwd.impl.awt;

import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class AwtBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final BufferedImage backingImage;
    
    private final BufferedImageHelper bufferedImageHelper;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param filePath Path of an image file.
     * @param disposalListener Must not be null.
     * @throws NullPointerException if filePath or disposalListener is null.
     * @throws IllegalArgumentException if could not load the specified image.
     */
    public AwtBwdImage(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        final File file = new File(filePath);
        final BufferedImage backingImage;
        try {
            backingImage = ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("could not load image at " + filePath, e);
        }
        if (backingImage == null) {
            // Can happen.
            throw new IllegalArgumentException("could not load image at " + filePath);
        }
        this.backingImage = backingImage;
        this.bufferedImageHelper = new BufferedImageHelper(backingImage);
        final ImageObserver observer = null;
        this.setWidth_final(backingImage.getWidth(observer));
        this.setHeight_final(backingImage.getHeight(observer));
    }
    
    public BufferedImage getBackingImage() {
        return this.backingImage;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        return this.bufferedImageHelper.getPixelAt(
                x,
                y,
                BihPixelFormat.ARGB32);
    }

    @Override
    protected void disposeImpl() {
        // Nothing to do.
    }
}
