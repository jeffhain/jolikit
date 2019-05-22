/*
 * Copyright 2019 Jeff Hain
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

import net.jolikit.bwd.impl.utils.graphics.AbstractIntGraphicBuffer;
import net.jolikit.lang.NumbersUtils;

/**
 * Resizable graphic buffer of pixels, using an hysteresis for spans of the
 * backing storage object to avoid systematic storage resizing and too much
 * useless memory usage.
 * 
 * This implementation uses an AWT Image as backing storage.
 */
public class AwtGraphicBuffer extends AbstractIntGraphicBuffer<BufferedImage> {
    
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
     * Creates a graphic buffer with:
     * - true for mustCopyOnImageResize (to avoid user being surprised),
     * - true for mustUseIntArrayRaster (in case it can help perfs),
     * - BufferedImage.TYPE_INT_ARGB_PRE for bufferedImageType.
     */
    public AwtGraphicBuffer() {
        this(
                true,
                true,
                BufferedImage.TYPE_INT_ARGB_PRE);
    }

    /**
     * TODO awt For some reason, if using BufferedImage constructor
     * that takes image type as argument, things can be quite slower
     * in some cases, which is why we provide the possibility to use
     * the constructor taking a raster as argument instead.
     * 
     * @param mustCopyOnImageResize If true, when a new backing image is
     *        created, copies pixels of previous image into it.
     * @param mustUseIntArrayRaster If true, for internal image, using
     *        BufferedImage constructor that takes a raster as argument,
     *        after creating a raster on top of an int array corresponding
     *        to the specified image type, else simply using BufferedImage
     *        constructor that takes image type as argument.
     * @param bufferedImageType Type of the backing BufferedImage.
     *        If mustUseIntArrayRaster is true, only supporting
     *        BufferedImage.TYPE_INT_XXX types (ARGB, ARGB_PRE, BGR, RGB).
     */
    public AwtGraphicBuffer(
            boolean mustCopyOnImageResize,
            boolean mustUseIntArrayRaster,
            int bufferedImageType) {
        super(mustCopyOnImageResize);
        
        this.mustUseIntArrayRaster = mustUseIntArrayRaster;
        
        this.bufferedImageType = bufferedImageType;
        
        final int initialStorageSpan = this.getInitialStorageSpan();
        this.createStorage(initialStorageSpan, initialStorageSpan);
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
     * unless image is drawn into its graphics before applying transforms.
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
    protected int getStorageWidth() {
        return this.image.getWidth();
    }

    @Override
    protected int getStorageHeight() {
        return this.image.getHeight();
    }
    
    @Override
    protected final void createStorage(int newStorageWidth, int newStorageHeight) {
        final BufferedImage image;
        if (this.mustUseIntArrayRaster) {
            final int pixelCapacity = NumbersUtils.timesExact(newStorageWidth, newStorageHeight);
            final int[] pixelArr = new int[pixelCapacity];
            
            /*
             * TODO awt If using for example RGBA here,
             * image drawing is much slower,
             * with much time spent here:
             * sun.java2d.loops.OpaqueCopyArgbToAny.Blit(CustomComponent.java:202)
             * sun.java2d.loops.GraphicsPrimitive.convertTo(GraphicsPrimitive.java:571)
             * sun.java2d.loops.MaskBlit$General.MaskBlit(MaskBlit.java:225)
             *    - locked sun.java2d.loops.MaskBlit$General@75c77d5e
             * sun.java2d.loops.Blit$GeneralMaskBlit.Blit(Blit.java:204)
             * sun.java2d.pipe.DrawImage.blitSurfaceData(DrawImage.java:959)
             * sun.java2d.pipe.DrawImage.renderImageCopy(DrawImage.java:577)
             * sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:67)
             * sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:1014)
             * sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3318)
             * sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3296)
             */
            final boolean isRasterPremultiplied;
            final int aIndex;
            final int rIndex;
            final int gIndex;
            final int bIndex;
            switch (this.bufferedImageType) {
            case BufferedImage.TYPE_INT_ARGB: {
                isRasterPremultiplied = false;
                aIndex = 0;
                rIndex = 1;
                gIndex = 2;
                bIndex = 3;
            } break;
            case BufferedImage.TYPE_INT_ARGB_PRE: {
                isRasterPremultiplied = true;
                aIndex = 0;
                rIndex = 1;
                gIndex = 2;
                bIndex = 3;
            } break;
            case BufferedImage.TYPE_INT_BGR: {
                isRasterPremultiplied = false;
                aIndex = -1;
                bIndex = 0;
                gIndex = 1;
                rIndex = 2;
            } break;
            case BufferedImage.TYPE_INT_RGB: {
                isRasterPremultiplied = false;
                aIndex = -1;
                rIndex = 0;
                gIndex = 1;
                bIndex = 2;
            } break;
            default:
                throw new IllegalArgumentException("" + this.bufferedImageType);
            }
            image = BufferedImageHelper.newBufferedImage(
                    pixelArr,
                    newStorageWidth,
                    newStorageHeight,
                    //
                    isRasterPremultiplied,
                    aIndex,
                    //
                    rIndex,
                    gIndex,
                    bIndex);
        } else {
            image = new BufferedImage(
                    newStorageWidth,
                    newStorageHeight,
                    this.bufferedImageType);
        }
        this.image = image;
    }

    @Override
    protected BufferedImage getStorage() {
        return this.image;
    }

    @Override
    protected void copyFromStorage(BufferedImage storage, int widthToCopy, int heightToCopy) {
        final BufferedImage imageToCopy = storage;
        
        final Graphics2D g = this.createClippedGraphics();
        try {
            g.drawImage(
                    imageToCopy,
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

    @Override
    protected void disposeStorage(BufferedImage storage) {
        // Nothing to do.
    }
}
