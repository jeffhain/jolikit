/*
 * Copyright 2021-2024 Jeff Hain
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
import net.jolikit.lang.LangUtils;

/**
 * A default implementation backed by an int array.
 */
public class IntArrSrcPixels implements InterfaceSrcPixels {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private GRect rect;
    
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
        this.configure_final(
            GRect.DEFAULT_EMPTY,
            LangUtils.EMPTY_INT_ARR,
            0);
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
            GRect.valueOf(0, 0, width, height),
            color32Arr,
            scanlineStride);
    }

    /**
     * @param color32Arr Must not be null.
     */
    public void configure(
        GRect rect,
        int[] color32Arr,
        int scanlineStride) {
        this.configure_final(
            rect,
            color32Arr,
            scanlineStride);
    }

    public void setRect(GRect rect) {
        this.rect = rect;
    }

    @Override
    public String toString() {
        return "[rect = " + this.rect
            + ", color32Arr.length = " + this.color32Arr.length
            + ", scanlineStride = " + this.scanlineStride
            + "]";
    }
    
    @Override
    public GRect getRect() {
        return this.rect;
    }
    
    @Override
    public int getWidth() {
        return this.rect.xSpan();
    }
    
    @Override
    public int getHeight() {
        return this.rect.ySpan();
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
        final int index =
            (y - this.rect.y()) * this.scanlineStride
            + (x - this.rect.x());
        return this.color32Arr[index];
    }
    
    /**
     * Does no check (optimized for speed).
     */
    public void setColor32At(int x, int y, int color32) {
        final int index =
            (y - this.rect.y()) * this.scanlineStride
            + (x - this.rect.x());
        this.color32Arr[index] = color32;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param color32Arr Must not be null.
     */
    private final void configure_final(
        GRect rect,
        int[] color32Arr,
        int scanlineStride) {
        this.rect = rect;
        this.color32Arr = LangUtils.requireNonNull(color32Arr);
        this.scanlineStride = scanlineStride;
    }
}
