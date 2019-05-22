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
package net.jolikit.bwd.impl.utils.graphics;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.NumbersUtils;

/**
 * Resizable graphic buffer of pixels, using an hysteresis for spans of the
 * backing storage object to avoid systematic storage resizing and too much
 * useless memory usage.
 * 
 * This implementation uses an array of int as backing storage.
 * 
 * Works for any pixel format.
 */
public class IntArrayGraphicBuffer extends AbstractIntGraphicBuffer<IntArrayGraphicBuffer.MyStorage> {

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE CLASSES
    //--------------------------------------------------------------------------

    static class MyStorage {
        final int[] pixelArr;
        final int storageWidth;
        final int storageHeight;
        public MyStorage(
                int storageWidth,
                int storageHeight) {
            final int pixelCapacity = NumbersUtils.timesExact(storageWidth, storageHeight);
            this.pixelArr = new int[pixelCapacity];
            this.storageWidth = storageWidth;
            this.storageHeight = storageHeight;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private MyStorage storage;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param mustCopyOnStorageResize If true, when a new backing array is
     *        created, copies pixels of previous array into it.
     */
    public IntArrayGraphicBuffer(boolean mustCopyOnStorageResize) {
        super(mustCopyOnStorageResize);
        
        final int initialStorageSpan = this.getInitialStorageSpan();
        this.createStorage(initialStorageSpan, initialStorageSpan);
    }
    
    /**
     * Exists for historical reasons,
     * but history might repeat itself so we keep it.
     * 
     * Clears this buffer with the specified color.
     */
    public void clear(GRect clip, int color32) {
        final int[] pixelArr = this.storage.pixelArr;

        // Making sure we use a clip in range.
        final GRect bufferRect = GRect.valueOf(0, 0, this.getWidth(), this.getHeight());
        clip = clip.intersected(bufferRect);
        
        final int yMax = clip.yMax();
        final int xMax = clip.xMax();
        final int ss = this.getScanlineStride();
        for (int y = clip.y(); y <= yMax; y++) {
            final int offset = y * ss;
            for (int x = clip.x(); x <= xMax; x++) {
                pixelArr[offset + x] = color32;
            }
        }
    }
    
    /**
     * @return The current backing image (can change on call to
     *         setSize(int,int)), which width and height can be larger than
     *         those of this buffer.
     */
    public int[] getPixelArr() {
        return this.storage.pixelArr;
    }
    
    public int getScanlineStride() {
        return this.storage.storageWidth;
    }

    @Override
    public int getWidth() {
        return super.getWidth();
    }

    @Override
    public int getHeight() {
        return super.getHeight();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getStorageWidth() {
        return this.storage.storageWidth;
    }

    @Override
    protected int getStorageHeight() {
        return this.storage.storageHeight;
    }

    @Override
    protected void createStorage(int newStorageWidth, int newStorageHeight) {
        this.storage = new MyStorage(newStorageWidth, newStorageHeight);
    }

    @Override
    protected MyStorage getStorage() {
        return this.storage;
    }

    @Override
    protected void copyFromStorage(MyStorage storage, int widthToCopy, int heightToCopy) {
        final MyStorage storageToCopy = storage;
        
        copyPixels_srcClipped_dstClipped(
                storageToCopy.pixelArr,
                storageToCopy.storageWidth,
                0,
                0,
                //
                this.storage.pixelArr,
                this.storage.storageWidth,
                0,
                0,
                //
                widthToCopy,
                heightToCopy);
    }

    @Override
    protected void disposeStorage(MyStorage storage) {
        // Nothing to do.
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * NB: This method is quite general, it could be moved
     * into the public API of some utility class if needed.
     */
    private static void copyPixels_srcClipped_dstClipped(
            int[] src,
            int srcScanlineStride,
            int sx,
            int sy,
            //
            int[] dst,
            int dstScanlineStride,
            int dx,
            int dy,
            //
            int xSpan,
            int ySpan) {
        for (int j = 0; j < ySpan; j++) {
            final int srcY = sy + j;
            final int dstY = dy + j;
            final int srcIndex0 = srcY * srcScanlineStride + sx;
            final int dstIndex0 = dstY * dstScanlineStride + dx;
            System.arraycopy(src, srcIndex0, dst, dstIndex0, xSpan);
        }
    }
}
