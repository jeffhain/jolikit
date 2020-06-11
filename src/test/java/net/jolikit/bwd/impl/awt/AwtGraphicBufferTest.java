/*
 * Copyright 2019-2020 Jeff Hain
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

import java.awt.image.BufferedImage;

import junit.framework.TestCase;

public class AwtGraphicBufferTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Testing that the created buffered image has the proper type.
     */
    public void test_bufferedImageType() {
        for (boolean mustUseIntArrayRaster : new boolean[]{false,true}) {
            for (int bufferedImageType : new int[]{
                    BufferedImage.TYPE_INT_ARGB,
                    BufferedImage.TYPE_INT_ARGB_PRE,
                    BufferedImage.TYPE_INT_BGR,
                    BufferedImage.TYPE_INT_RGB
            }) {
                final boolean mustCopyOnImageResize = false;
                final boolean allowShrinking = false;
                final AwtGraphicBuffer buffer = new AwtGraphicBuffer(
                        mustCopyOnImageResize,
                        allowShrinking,
                        mustUseIntArrayRaster,
                        bufferedImageType);
                
                assertEquals(bufferedImageType, buffer.getImage().getType());
            }
        }
    }
}
