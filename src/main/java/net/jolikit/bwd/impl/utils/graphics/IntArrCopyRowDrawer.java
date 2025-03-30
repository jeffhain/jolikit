/*
 * Copyright 2021-2025 Jeff Hain
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

import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.lang.LangUtils;

/**
 * A default implementation backed by an int array,
 * that does COPY (or SRC) blending.
 */
public class IntArrCopyRowDrawer implements InterfaceRowDrawer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private GTransform transformArrToUser;
    
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
    public IntArrCopyRowDrawer() {
        this.configure_final(
            GTransform.IDENTITY,
            LangUtils.EMPTY_INT_ARR,
            0);
    }

    /**
     * @param transformArrToUser Transform from the specified array,
     *        into coordinates of drawRow() user. Must not be null.
     * @param color32Arr Must not be null.
     */
    public void configure(
        GTransform transformArrToUser,
        int[] color32Arr,
        int scanlineStride) {
        this.configure_final(
            transformArrToUser,
            color32Arr,
            scanlineStride);
    }
    
    @Override
    public String toString() {
        return "[transformArrToUser = " + this.transformArrToUser
            + ", color32Arr.length = " + this.color32Arr.length
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
        
        final GTransform transformArrToUser = this.transformArrToUser;
        if (transformArrToUser.rotation() == GRotation.ROT_0) {
            final int xInArr = dstX + transformArrToUser.frame2XIn1();
            final int yInArr = dstY + transformArrToUser.frame2YIn1();
            final int dstIndex = yInArr * this.scanlineStride + xInArr;
            System.arraycopy(
                rowArr,
                rowOffset,
                this.color32Arr,
                dstIndex,
                length);
        } else {
            final GRotation rotation = this.transformArrToUser.rotation();
            // Optimization not to have to use transform for each pixel.
            final int xStepInArr = rotation.cos();
            final int yStepInArr = rotation.sin();
            
            int xInArr = this.transformArrToUser.xIn1(dstX, dstY);
            int yInArr = this.transformArrToUser.yIn1(dstX, dstY);
            for (int i = 0; i < length; i++) {
                final int color32 = rowArr[rowOffset + i];
                final int dstIndex = yInArr * this.scanlineStride + xInArr;
                this.color32Arr[dstIndex] = color32;
                xInArr += xStepInArr;
                yInArr += yStepInArr;
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param transformArrToUser Must not be null.
     * @param color32Arr Must not be null.
     */
    private final void configure_final(
        GTransform transformArrToUser,
        int[] color32Arr,
        int scanlineStride) {
        this.transformArrToUser = LangUtils.requireNonNull(transformArrToUser);
        this.color32Arr = LangUtils.requireNonNull(color32Arr);
        this.scanlineStride = scanlineStride;
    }
}
