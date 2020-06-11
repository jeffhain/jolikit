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
package net.jolikit.bwd.impl.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.AbstractBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

/**
 * TODO lwjgl 1-bit BMPs are not supported.
 */
public class LwjglBwdImage extends AbstractBwdImage {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * TODO lwjgl For performances when drawing image on graphics,
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
    public LwjglBwdImage(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        // Forcing non-premultiplied, else we don't know
        // if it is or not.
        STBImage.stbi_set_unpremultiply_on_load(true);
        
        if (false) {
            /*
             * To have image origin being bottom-left,
             * i.e. matching OpenGL conventions.
             * Need not to do it, since we want to use
             * client coordinates.
             */
            STBImage.stbi_set_flip_vertically_on_load(true);
        }

        // TODO lwjgl Called filename, but is actually a file path.
        final CharSequence filename = filePath;
        final IntBuffer wBuff = BufferUtils.createIntBuffer(1);
        final IntBuffer hBuff = BufferUtils.createIntBuffer(1);
        final IntBuffer compBuff = BufferUtils.createIntBuffer(1);
        /*
         * TODO lwjgl Had this bug and a workaround:
         * The amount of bytes per pixel doesn't appear to be "comp",
         * but "req_comp", and "comp" appears to only be the amount of bytes
         * actually coming from the file.
         * Ex.:
         * - req_comp = 4 -> data = [7F,7F,7F,FF, 7F,7F,7F,FF, (...) 3F,48,CC,FF, 3F,48,CC,FF (...)]
         *                   comp = 3
         * - req_comp = 3 -> data = [7F,7F,7F, 7F,7F,7F, (...) 3F,48,CC, 3F,48,CC (...)]
         *                   comp = 3
         * That said, the amount of bytes in the returned ByteBuffer is not (req_comp * w * h),
         * but appears to be _truncated_ at (comp * w * h), causing 1/4 of the expected bytes
         * to be lost.
         * A workaround is to use req_comp = 3, and just assume alpha is 0xFF.
         * But with 3.1.2 build 29, the bug seems corrected:
         * the amount of bytes in the returned ByteBuffer is now properly
         * (req_comp * w * h), so now we use req_comp = 4,
         * and don't assume an alpha.
         */
        final int req_comp = 4;
        
        /*
         * "The supported formats are JPEG, PNG, TGA, BMP, PSD, GIF, HDR, PIC and PNM."
         * (PNG : issue if 16 bits)
         * 
         * TODO lwjgl Maybe better to load from a ByteBuffer obtained from a FileChannel,
         * using STBImage.stbi_load(ByteBuffer, ...),
         * in case STBImage would have issues with Unicode file names.
         */
        final ByteBuffer data = STBImage.stbi_load(filename, wBuff, hBuff, compBuff, req_comp);
        if (data == null) {
            throw new IllegalArgumentException(
                    "stbi_load: could not load image at "
                            + filePath
                            + ": "
                            + STBImage.stbi_failure_reason());
        }
        try {
            final int w = wBuff.get(0);
            final int h = hBuff.get(0);
            final int comp = compBuff.get(0);

            this.setWidth_final(w);
            this.setHeight_final(h);
            final int bytesPerPixel = req_comp;
            
            final int wh = w * h;
            final int expectedCapacity = wh * bytesPerPixel;
            if (data.capacity() != expectedCapacity) {
                throw new BindingError("data = " + data + ", expectedCapacity = " + expectedCapacity);
            }
            
            final int[] color32Arr = new int[wh];
            this.color32Arr = color32Arr;
            
            int byteIndex = 0;
            for (int pixelIndex = 0; pixelIndex < wh; pixelIndex++) {
                final int color32 = data.getInt(byteIndex);
                byteIndex += bytesPerPixel;
                final int premulColor32;
                if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
                    premulColor32 = BindingColorUtils.toPremulAxyz32(color32);
                } else {
                    premulColor32 = BindingColorUtils.toPremulXyza32(color32);
                }
                color32Arr[pixelIndex] = premulColor32;
            }
        } finally {
            // Clearing to make sure position is 0 and limit is capacity,
            // in case they would be used for freeing.
            data.clear();
            STBImage.stbi_image_free(data);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int index = y * this.getWidth() + x;
        final int color32 = this.color32Arr[index];
        final int argb32 = LwjglPaintHelper.getArgb32FromArrayColor32(color32);
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
