/*
 * Copyright 2019-2025 Jeff Hain
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

import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

public class AwtBwdImageFromFile extends AbstractAwtBwdImage {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final BufferedImageHelper bufferedImageHelperArgbPre;
    
    private final int[] premulArgb32Arr;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param filePath Path of an image file.
     * @param parallelizer Must not be null.
     * @param disposalListener Must not be null.
     * @throws NullPointerException if filePath or disposalListener is null.
     * @throws IllegalArgumentException if could not load the specified image.
     */
    public AwtBwdImageFromFile(
        String filePath,
        InterfaceParallelizer parallelizer,
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
        
        final int width = readImage.getWidth();
        final int height = readImage.getHeight();
        this.setWidth_final(width);
        this.setHeight_final(height);
        
        final BufferedImageHelper readHelper =
            new BufferedImageHelper(readImage);
        final BufferedImageHelper bufferedImageHelperArgbPre;
        if (readHelper.getImageType() == AwtUtils.COMMON_BUFFERED_IMAGE_TYPE_ARGB_PRE) {
            bufferedImageHelperArgbPre = readHelper;
        } else {
            bufferedImageHelperArgbPre =
                new BufferedImageHelper(
                    null,
                    width,
                    width,
                    height,
                    AwtUtils.COMMON_BUFFERED_IMAGE_PIXEL_FORMAT_ARGB,
                    AwtUtils.COMMON_BUFFERED_IMAGE_PREMUL);
            AwtPrlCopyImage.copyImage(
                parallelizer,
                //
                readHelper,
                0,
                0,
                //
                bufferedImageHelperArgbPre,
                0,
                0,
                //
                width,
                height);
        }
        this.bufferedImageHelperArgbPre = bufferedImageHelperArgbPre;
        
        this.premulArgb32Arr =
            LangUtils.requireNonNull(
                bufferedImageHelperArgbPre.getIntArrayDirectlyUsed());
    }
    
    @Override
    public BufferedImageHelper getBufferedImageHelperArgbPre() {
        return this.bufferedImageHelperArgbPre;
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
