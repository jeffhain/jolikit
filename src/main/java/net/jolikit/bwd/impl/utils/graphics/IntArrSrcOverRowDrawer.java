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

import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.lang.LangUtils;

/**
 * A default implementation backed by an alpha-premultiplied ARGB32 array,
 * that does SRC_OVER blending.
 */
public class IntArrSrcOverRowDrawer implements InterfaceRowDrawer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private GTransform transformArrToUser;
    
    /**
     * Never null.
     */
    private int[] premulArgb32Arr;
    
    private int scanlineStride;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates an empty instance.
     * To be configured before use.
     */
    public IntArrSrcOverRowDrawer() {
        this.configure_final(
            GTransform.IDENTITY,
            LangUtils.EMPTY_INT_ARR,
            0);
    }

    /**
     * @param transformArrToUser Transform from the specified array,
     *        into coordinates of drawRow() user. Must not be null.
     * @param premulArgb32Arr Must not be null.
     */
    public void configure(
        GTransform transformArrToUser,
        int[] premulArgb32Arr,
        int scanlineStride) {
        this.configure_final(
            transformArrToUser,
            premulArgb32Arr,
            scanlineStride);
    }
    
    @Override
    public String toString() {
        return "[transformArrToUser = " + this.transformArrToUser
            + ", premulArgb32Arr.length = " + this.premulArgb32Arr.length
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
        
        final GRotation rotation = this.transformArrToUser.rotation();
        // Optimization not to have to use transform for each pixel.
        final int xStepInArr = rotation.cos();
        final int yStepInArr = rotation.sin();
        
        int xInArr = this.transformArrToUser.xIn1(dstX, dstY);
        int yInArr = this.transformArrToUser.yIn1(dstX, dstY);
        for (int i = 0; i < length; i++) {
            final int srcPremulArgb32 = rowArr[rowOffset + i];
            final int dstIndex = yInArr * this.scanlineStride + xInArr;
            this.blendPremulArgb32(dstIndex, srcPremulArgb32);
            xInArr += xStepInArr;
            yInArr += yStepInArr;
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param transformArrToUser Must not be null.
     * @param premulArgb32Arr Must not be null.
     */
    private final void configure_final(
        GTransform transformArrToUser,
        int[] premulArgb32Arr,
        int scanlineStride) {
        this.transformArrToUser = LangUtils.requireNonNull(transformArrToUser);
        this.premulArgb32Arr = LangUtils.requireNonNull(premulArgb32Arr);
        this.scanlineStride = scanlineStride;
    }
    
    private void blendPremulArgb32(int index, int srcPremulArgb32) {
        final int dstPremulArgb32 = this.premulArgb32Arr[index];
        final int newPremulArgb32 =
            BindingColorUtils.blendPremulAxyz32_srcOver(
                srcPremulArgb32,
                dstPremulArgb32);
        this.premulArgb32Arr[index] = newPremulArgb32;
    }
}
