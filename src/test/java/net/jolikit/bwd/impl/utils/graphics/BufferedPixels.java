/*
 * Copyright 2025 Jeff Hain
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

import java.awt.image.BufferedImage;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.lang.LangUtils;

/**
 * InterfaceSrcPixels and InterfaceRowDrawer
 * on top of a BufferedImage.
 */
public class BufferedPixels implements InterfaceSrcPixels, InterfaceRowDrawer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final IntArrSrcPixels srcPixels;
    
    private final IntArrCopyRowDrawer rowDrawer;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public BufferedPixels(BufferedImage image) {
        final BufferedImageHelper helper = new BufferedImageHelper(image);
        
        final int[] arr = helper.getIntArrayDirectlyUsed();
        LangUtils.requireNonNull(arr);
        
        final int scanlineStride = helper.getScanlineStride();
        if (scanlineStride < 0) {
            throw new IllegalArgumentException();
        }
        
        final int width = image.getWidth();
        final int height = image.getHeight();
        
        final GRect rect = GRect.valueOf(0, 0, width, height);
        
        this.srcPixels = new IntArrSrcPixels();
        this.srcPixels.configure(rect, arr, scanlineStride);
        
        this.rowDrawer = new IntArrCopyRowDrawer();
        this.rowDrawer.configure(GTransform.IDENTITY, arr, scanlineStride);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public GRect getRect() {
        return this.srcPixels.getRect();
    }

    @Override
    public int[] color32Arr() {
        return this.srcPixels.color32Arr();
    }

    @Override
    public int getScanlineStride() {
        return this.srcPixels.getScanlineStride();
    }

    @Override
    public int getColor32At(int x, int y) {
        return this.srcPixels.getColor32At(x, y);
    }
    
    public void setColor32At(int x, int y, int color32) {
        this.srcPixels.setColor32At(x, y, color32);
    }
    
    @Override
    public void drawRow(int[] rowArr, int rowOffset, int dstX, int dstY, int length) {
        this.rowDrawer.drawRow(rowArr, rowOffset, dstX, dstY, length);
    }
}
