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
public class IntArrRowDrawer implements InterfaceRowDrawer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int[] EMPTY_ARR = new int[0];
    
    /*
     * 
     */
    
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
    public IntArrRowDrawer() {
        this.configure_final(EMPTY_ARR, 0);
    }

    /**
     * @param color32Arr Must not be null.
     */
    public void configure(
            int[] color32Arr,
            int scanlineStride) {
        this.configure_final(
            color32Arr,
            scanlineStride);
    }
    
    @Override
    public String toString() {
        return "[color32Arr.length = " + this.color32Arr.length
            + ", scanlineStride = " + this.scanlineStride
            + "]";
    }
    
    /**
     * Thread-safe as long as not being configured
     * or having same pixels written concurrently.
     */
    @Override
    public void drawRow(
        int[] rowArr,
        int rowOffset,
        int dstX,
        int dstY,
        int length) {
        
        final int dstIndex = dstY * this.scanlineStride + dstX;
        System.arraycopy(
            rowArr,
            rowOffset,
            this.color32Arr,
            dstIndex,
            length);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param color32Arr Must not be null.
     */
    private final void configure_final(
            int[] color32Arr,
            int scanlineStride) {
        this.color32Arr = LangUtils.requireNonNull(color32Arr);
        this.scanlineStride = scanlineStride;
    }
}
