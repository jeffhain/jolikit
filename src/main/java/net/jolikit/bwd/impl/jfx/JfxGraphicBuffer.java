/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.impl.jfx;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.graphics.AbstractGraphicBuffer;

/**
 * Resizable graphic buffer of pixels, using an hysteresis for spans of the
 * backing storage object to avoid systematic storage resizing and too much
 * useless memory usage.
 * 
 * This implementation uses a JavaFX WritableImage as backing storage.
 */
public class JfxGraphicBuffer extends AbstractGraphicBuffer<WritableImage> {
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Wrapper on top of pixel reader, that returns a default color
     * for pixels out of range.
     * Useful due to no API to create a WritableImage initialized
     * from a sub area of another one.
     */
    private static class MyPixelReader implements PixelReader {
        private final PixelReader reader;
        private final GRect box;
        public MyPixelReader(
                PixelReader reader,
                GRect box) {
            this.reader = reader;
            this.box = box;
        }
        @Override
        public void getPixels(
                int x,
                int y,
                int w,
                int h,
                WritablePixelFormat<IntBuffer> pixelformat,
                int[] buffer,
                int offset,
                int scanlineStride) {
            GRect srcRect = GRect.valueOf(x, y, w, h);
            srcRect = srcRect.intersected(this.box);
            this.reader.getPixels(
                    srcRect.x(),
                    srcRect.y(),
                    srcRect.xSpan(),
                    srcRect.ySpan(),
                    pixelformat,
                    buffer,
                    offset,
                    scanlineStride);
        }
        @Override
        public void getPixels(
                int x,
                int y,
                int w,
                int h,
                WritablePixelFormat<ByteBuffer> pixelformat,
                byte[] buffer,
                int offset,
                int scanlineStride) {
            GRect srcRect = GRect.valueOf(x, y, w, h);
            srcRect = srcRect.intersected(this.box);
            this.reader.getPixels(
                    srcRect.x(),
                    srcRect.y(),
                    srcRect.xSpan(),
                    srcRect.ySpan(),
                    pixelformat,
                    buffer,
                    offset,
                    scanlineStride);
        }
        @Override
        public <T extends Buffer> void getPixels(
                int x,
                int y,
                int w,
                int h,
                WritablePixelFormat<T> pixelformat,
                T buffer,
                int scanlineStride) {
            GRect srcRect = GRect.valueOf(x, y, w, h);
            srcRect = srcRect.intersected(this.box);
            this.reader.getPixels(
                    srcRect.x(),
                    srcRect.y(),
                    srcRect.xSpan(),
                    srcRect.ySpan(),
                    pixelformat,
                    buffer,
                    scanlineStride);
        }
        @Override
        public PixelFormat getPixelFormat() {
            return this.reader.getPixelFormat();
        }
        @Override
        public Color getColor(int x, int y) {
            final Color ret;
            if (this.box.contains(x, y)) {
                ret = this.reader.getColor(x, y);
            } else {
                ret = Color.TRANSPARENT;
            }
            return ret;
        }
        @Override
        public int getArgb(int x, int y) {
            final int ret;
            if (this.box.contains(x, y)) {
                ret = this.reader.getArgb(x, y);
            } else {
                ret = 0;
            }
            return ret;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private WritableImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a graphic buffer with a default configuration:
     * - true for mustCopyOnImageResize (to avoid user being surprised),
     * - true for allowShrinking (to avoid memory waste),
     */
    public JfxGraphicBuffer() {
        this(
                true, // mustCopyOnImageResize
                true); // allowShrinking
    }

    /**
     * @param mustCopyOnImageResize If true, when a new backing image is
     *        created, copies pixels of previous image into it.
     * @param allowShrinking If true, backing array can be renewed
     *        when old one is too large for the new size.
     */
    public JfxGraphicBuffer(
            boolean mustCopyOnImageResize,
            boolean allowShrinking) {
        super(
                mustCopyOnImageResize,
                allowShrinking);
        
        final int initialStorageSpan = this.getInitialStorageSpan();
        this.createInitialStorage(
                initialStorageSpan,
                initialStorageSpan);
    }
    
    /**
     * @return The current backing image (can change on call to
     *         setSize(int,int)), which width and height can be larger than
     *         those of this buffer.
     */
    public WritableImage getImage() {
        return this.image;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected WritableImage getStorage() {
        return this.image;
    }

    @Override
    protected int getStorageWidth() {
        return (int) this.image.getWidth();
    }

    @Override
    protected int getStorageHeight() {
        return (int) this.image.getHeight();
    }
    
    @Override
    protected final void createStorage(
            int newStorageWidth,
            int newStorageHeight,
            //
            WritableImage oldStorageToCopy,
            int widthToCopy,
            int heightToCopy) {
        if (oldStorageToCopy != null) {
            final GRect boxToCopy =
                    GRect.valueOf(0, 0, widthToCopy, heightToCopy);
            final MyPixelReader pixelReaderForCopy =
                    new MyPixelReader(
                            oldStorageToCopy.getPixelReader(),
                            boxToCopy);
            this.image = new WritableImage(
                    pixelReaderForCopy,
                    newStorageWidth,
                    newStorageHeight);
        } else {
            this.image = new WritableImage(
                    newStorageWidth,
                    newStorageHeight);
        }
    }

    @Override
    protected void disposeStorage(WritableImage storage) {
        // Nothing to do.
    }
}
