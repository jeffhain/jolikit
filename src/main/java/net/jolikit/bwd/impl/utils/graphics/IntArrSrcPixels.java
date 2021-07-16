/*
 * Copyright 2021 Jeff Hain
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

import net.jolikit.lang.LangUtils;

/**
 * A default implementation backed by an int array.
 */
public class IntArrSrcPixels implements InterfaceSrcPixels {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private int width;
    
    private int height;
    
    /**
     * Never null.
     */
    private int[] color32Arr;
    
    private int scanlineStride;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates an empty instance.
     * To be configured before use.
     */
    public IntArrSrcPixels() {
        this.configure_final(0, 0, LangUtils.EMPTY_INT_ARR, 0);
    }

    /**
     * @param color32Arr Must not be null.
     */
    public void configure(
            int width,
            int height,
            int[] color32Arr,
            int scanlineStride) {
        this.configure_final(
            width,
            height,
            color32Arr,
            scanlineStride);
    }

    @Override
    public String toString() {
        return "[width = " + this.width
            + ", height = " + this.height
            + ", color32Arr.length = " + this.color32Arr.length
            + ", scanlineStride = " + this.scanlineStride
            + "]";
    }
    
    @Override
    public int getWidth() {
        return this.width;
    }
    
    @Override
    public int getHeight() {
        return this.height;
    }
    
    @Override
    public int[] color32Arr() {
        return this.color32Arr;
    }
    
    @Override
    public int getScanlineStride() {
        return this.scanlineStride;
    }
    
    /**
     * Does no check (optimized for speed).
     * 
     * Thread-safe as long as not being configured
     * or having same pixels written concurrently.
     */
    @Override
    public int getColor32At(int x, int y) {
        final int index = y * this.scanlineStride + x;
        return this.color32Arr[index];
    }
    
    /**
     * Does no check (optimized for speed).
     */
    public void setColor32At(int x, int y, int color32) {
        final int index = y * this.scanlineStride + x;
        this.color32Arr[index] = color32;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param color32Arr Must not be null.
     */
    private final void configure_final(
            int width,
            int height,
            int[] color32Arr,
            int scanlineStride) {
        this.width = width;
        this.height = height;
        this.color32Arr = LangUtils.requireNonNull(color32Arr);
        this.scanlineStride = scanlineStride;
    }
}
