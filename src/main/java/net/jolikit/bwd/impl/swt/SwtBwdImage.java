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
package net.jolikit.bwd.impl.swt;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;

public class SwtBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int[] premulArgb32Arr;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param filePath Path of an image file.
     * @param display Display to use for the image. Must not be null.
     * @param disposalListener Must not be null.
     * @throws IllegalArgumentException if could not load the specified image.
     * @throws NullPointerException if filePath or display or disposalListener is null.
     */
    public SwtBwdImage(
            String filePath,
            Display display,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        LangUtils.requireNonNull(display);
        
        final Image backingImage;
        try {
            backingImage = new Image(display, filePath);
        } catch (SWTException e) {
            /*
             * org.eclipse.swt.SWTException: Unsupported or unrecognized format
             */
            throw new IllegalArgumentException("", e);
        }

        /*
         * TODO swt Image.getImageData() is not just a simple "get" operation,
         * as this call stack shows:
         * org.eclipse.swt.internal.win32.OS.MoveMemory(Native Method)
         * org.eclipse.swt.graphics.Image.getImageData(Unknown Source).
         * And more importantly, it's also extremely slow, such as we really
         * don't want to do it for each pixel.
         * Also, not to have to do pixel conversion for each drawing,
         * we just transform the image data into a proper array of pixels
         * here.
         */
        try {
            if (false) {
                /*
                 * TODO swt Looks like a SWT hack to simulate transparency,
                 * for non-transparent images that have a "transparent pixel",
                 * so it should be useless for "pure" and freshly loaded images.
                 */
                backingImage.getBackground();
            }
            
            // This is slow: must only do it once.
            final ImageData imageData = backingImage.getImageData();

            final int width = imageData.width;
            final int height = imageData.height;
            final int pixelCapacity = NumbersUtils.timesExact(width, height);
            final int[] premulArgb32Arr = new int[pixelCapacity];

            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int argb32 = SwtPaintUtils.getArgb32At(imageData, x, y);
                    final int premulArgb32 = BindingColorUtils.toPremulAxyz32(argb32);
                    premulArgb32Arr[index++] = premulArgb32;
                }
            }

            this.premulArgb32Arr = premulArgb32Arr;
            this.setWidth_final(width);
            this.setHeight_final(height);
        } finally {
            backingImage.dispose();
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int index = y * this.getWidth() + x;
        final int premulArgb32 = this.premulArgb32Arr[index];
        return BindingColorUtils.toNonPremulAxyz32(premulArgb32);
    }

    @Override
    protected void disposeImpl() {
        // Nothing to do.
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    int[] getColor32Arr() {
        return this.premulArgb32Arr;
    }
}
