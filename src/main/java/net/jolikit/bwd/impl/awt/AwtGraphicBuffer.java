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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.jolikit.bwd.impl.utils.graphics.AbstractGraphicBuffer;
import net.jolikit.lang.NbrsUtils;

/**
 * Resizable graphic buffer of pixels, using an hysteresis for spans of the
 * backing storage object to avoid systematic storage resizing and too much
 * useless memory usage.
 * 
 * This implementation uses an AWT Image as backing storage.
 */
public class AwtGraphicBuffer extends AbstractGraphicBuffer<BufferedImage> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final boolean mustUseIntArrayRaster;
    
    private final int bufferedImageType;
    
    private BufferedImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a graphic buffer with a default configuration:
     * - true for mustCopyOnImageResize (to avoid user being surprised),
     * - true for allowShrinking (to avoid memory waste),
     * - true for mustUseIntArrayRaster (in case it can help perfs),
     * - BufferedImage.TYPE_INT_ARGB_PRE for bufferedImageType.
     */
    public AwtGraphicBuffer() {
        this(
            true, // mustCopyOnImageResize
            true, // allowShrinking
            true, // mustUseIntArrayRaster
            AwtPaintUtils.COMMON_BUFFERED_IMAGE_TYPE_ARGB_PRE);
    }

    /**
     * TODO awt For some reason, if using BufferedImage constructor
     * that takes image type as argument, things can be quite slower
     * in some cases, which is why we provide the possibility to use
     * the constructor taking a raster as argument instead.
     * 
     * @param mustCopyOnImageResize If true, when a new backing image is
     *        created, copies pixels of previous image into it.
     * @param allowShrinking If true, backing array can be renewed
     *        when old one is too large for the new size.
     * @param mustUseIntArrayRaster If true, for internal image, using
     *        BufferedImage constructor that takes a raster as argument,
     *        after creating a raster on top of an int array corresponding
     *        to the specified image type, else simply using BufferedImage
     *        constructor that takes image type as argument.
     * @param bufferedImageType Type of the backing BufferedImage.
     *        If mustUseIntArrayRaster is true, only supporting
     *        BufferedImage.TYPE_INT_XXX types {ARGB, ARGB_PRE, BGR, RGB}.
     */
    public AwtGraphicBuffer(
            boolean mustCopyOnImageResize,
            boolean allowShrinking,
            boolean mustUseIntArrayRaster,
            int bufferedImageType) {
        super(
                mustCopyOnImageResize,
                allowShrinking);
        
        this.mustUseIntArrayRaster = mustUseIntArrayRaster;
        
        this.bufferedImageType = bufferedImageType;
        
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
    public BufferedImage getImage() {
        return this.image;
    }
    
    /**
     * @return A new graphics for the current backing image, with clip set to
     *         (0,0,width,height).
     */
    public Graphics2D createClippedGraphics() {
        final Graphics2D g = this.image.createGraphics();
        g.setClip(0, 0, this.getWidth(), this.getHeight());
        return g;
    }

    /*
     * 
     */

    /**
     * TODO awt Data drawn directly into the backing array of pixels,
     * are not taken into account when applying transforms on graphics,
     * unless image is drawn into its graphics before applying transforms
     * (maybe that only occurs if the array is not marked "untrackable"?).
     */
    public final void drawImageIntoItsGraphics() {
        final Graphics2D g = this.image.createGraphics();
        try {
            g.drawRenderedImage(this.image, null);
        } finally {
            g.dispose();
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected BufferedImage getStorage() {
        return this.image;
    }

    @Override
    protected int getStorageWidth() {
        return this.image.getWidth();
    }

    @Override
    protected int getStorageHeight() {
        return this.image.getHeight();
    }
    
    @Override
    protected final void createStorage(
            int newStorageWidth,
            int newStorageHeight,
            //
            BufferedImage oldStorageToCopy,
            int widthToCopy,
            int heightToCopy) {
        final BufferedImage image;
        if (this.mustUseIntArrayRaster) {
            final int pixelCapacity = NbrsUtils.timesExact(newStorageWidth, newStorageHeight);
            final int[] pixelArr = new int[pixelCapacity];
            final int scanlineStride = newStorageWidth;
            image = BufferedImageHelper.newBufferedImageWithIntArray(
                    pixelArr,
                    scanlineStride,
                    //
                    newStorageWidth,
                    newStorageHeight,
                    //
                    this.bufferedImageType);
        } else {
            image = new BufferedImage(
                    newStorageWidth,
                    newStorageHeight,
                    this.bufferedImageType);
        }
        
        if (oldStorageToCopy != null) {
            final Graphics2D g = this.createClippedGraphics();
            try {
                g.drawImage(
                        oldStorageToCopy,
                        0,
                        0,
                        widthToCopy, // Exclusive.
                        heightToCopy, // Exclusive.
                        0,
                        0,
                        widthToCopy, // Exclusive.
                        heightToCopy, // Exclusive.
                        null);
            } finally {
                g.dispose();
            }
        }
        
        this.image = image;
    }

    @Override
    protected void disposeStorage(BufferedImage storage) {
        // Nothing to do.
    }
}
