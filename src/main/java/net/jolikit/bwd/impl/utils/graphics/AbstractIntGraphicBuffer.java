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

import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.lang.LangUtils;

/**
 * Abstract class to make it easier to implement a resizable graphic buffer
 * of pixels, using an hysteresis for spans of the backing storage object
 * to avoid systematic storage resizing and too much useless memory usage.
 * 
 * @param S Type for the storage.
 */
public abstract class AbstractIntGraphicBuffer<S> {

    /*
     * TODO optim We use powers of two capacities when possible, in case backing
     * libraries would have trouble with non-power-of-two scan line strides,
     * but it can cause 2*2 = 4 times larger memory usage than ever needed,
     * and 4*4 = 16 times larger memory usage than needed on shrinking,
     * so we might consider using floating-point growth and shrink factors
     * instead, like 1.5 and 1.0/1.5, and also use max theoretical capacities
     * (like screen spans) and use smaller growth factors above them, to avoid
     * using much more than ever required according to screen size.
     */

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final boolean mustCopyOnStorageResize;
    
    private int width;
    private int height;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Implementation must call its (normally final, since called in a
     * constructor) version of createStorage_noCheck(int,int) with
     * (current width, current height) as spans, other non-zero storage spans
     * being (if needed) later computed by generic resizing treatments when user
     * sets used spans.
     * 
     * @param mustCopyOnStorageResize If true, when a new backing storage is
     *        created, copies pixels of previous storage into it.
     */
    public AbstractIntGraphicBuffer(
            boolean mustCopyOnStorageResize) {
        this.mustCopyOnStorageResize = mustCopyOnStorageResize;
        this.setSize_raw(0, 0);
    }
    
    /**
     * Might change the current backing storage.
     * 
     * On size growth, pixel values covering new areas are undefined,
     * whatever the value of mustCopyOnStorageResize at buffer creation.
     * This allows not to clear pixels uselessly, since on window resize
     * everything is typically repaint.
     * 
     * @param newWidth Must be >= 0.
     * @param newHeight Must be >= 0.
     */
    public void setSize(int newWidth, int newHeight) {
        if (newWidth < 0) {
            throw new IllegalArgumentException("newWidth [" + newWidth + "] must be >= 0");
        }
        if (newHeight < 0) {
            throw new IllegalArgumentException("newHeight [" + newHeight + "] must be >= 0");
        }
        
        final int oldStorageWidth = this.getStorageWidth();
        final int oldStorageHeight = this.getStorageHeight();
        
        final int newStorageWidth = BindingBasicsUtils.computeNewStorageSpan(oldStorageWidth, newWidth);
        final int newStorageHeight = BindingBasicsUtils.computeNewStorageSpan(oldStorageHeight, newHeight);
        
        final boolean needNewStorage =
                (newStorageWidth != oldStorageWidth)
                || (newStorageHeight != oldStorageHeight);
        
        if (needNewStorage) {
            final S oldStorage = LangUtils.requireNonNull(this.getStorage());
            
            final int oldWidth = this.getWidth();
            final int oldHeight = this.getHeight();

            this.createStorage(newStorageWidth, newStorageHeight);
            this.setSize_raw(newWidth, newHeight);

            if (this.mustCopyOnStorageResize) {
                final int widthToCopy = Math.min(oldWidth, newWidth);
                final int heightToCopy = Math.min(oldHeight, newHeight);
                this.copyFromStorage(oldStorage, widthToCopy, heightToCopy);
            }
            
            this.disposeStorage(oldStorage);
        } else {
            // Previous storage still suits.
            this.setSize_raw(newWidth, newHeight);
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /**
     * @return Value for initial storage's width and height.
     */
    protected final int getInitialStorageSpan() {
        return BindingBasicsUtils.MIN_STORAGE_SPAN;
    }
    
    /**
     * @return The currently used width.
     */
    protected int getWidth() {
        return this.width;
    }

    /**
     * @return The currently used height.
     */
    protected int getHeight() {
        return this.height;
    }
    
    /*
     * 
     */
    
    /**
     * This is the scan line stride for this buffer.
     * 
     * @return The width of the current storage object.
     */
    protected abstract int getStorageWidth();

    /**
     * @return The height of the current storage object.
     */
    protected abstract int getStorageHeight();
    
    /**
     * Specified spans must be >= 0 (not supposed to be checked).
     */
    protected abstract void createStorage(int newStorageWidth, int newStorageHeight);
    
    /*
     * Methods used to copy old storage pixels to a newly created current storage.
     */
    
    /**
     * @return The current storage object.
     */
    protected abstract S getStorage();
    
    /**
     * Specified spans are in range of both current and specified storages.
     * 
     * @param storage Storage object which pixels must be copied (not blended)
     *        into current storage.
     * @param widthToCopy Width to copy, from the start.
     * @param heightToCopy Height to copy, from the start.
     */
    protected abstract void copyFromStorage(S storage, int widthToCopy, int heightToCopy);
    
    /*
     * 
     */
    
    /**
     * Called on no longer used storages.
     */
    protected abstract void disposeStorage(S storage);
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void setSize_raw(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
