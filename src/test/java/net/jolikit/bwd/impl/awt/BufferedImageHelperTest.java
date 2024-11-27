/*
 * Copyright 2024 Jeff Hain
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
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;

public class BufferedImageHelperTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BufferedImageHelperTest() {
    }
    
    public void test_requireCompatible() {
        final int width = 10;
        final int height = 5;
        final int[] pixelArr = new int[width * height];
        
        // Bad array type.
        {
            final BufferedImage image =
                new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_3BYTE_BGR);
            
            try {
                final boolean premul = false;
                BufferedImageHelper.requireCompatible(
                    image,
                    BihPixelFormat.ARGB32,
                    premul);
                fail();
            } catch (IllegalArgumentException e) {
                assertEquals("image is not backed by an int array", e.getMessage());
            }
        }
        
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : new boolean[] {false,true}) {
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        pixelArr,
                        width,
                        height,
                        pixelFormat,
                        premul);
                
                // All good.
                BufferedImageHelper.requireCompatible(
                    image,
                    pixelFormat,
                    premul);
                
                // Bad pixel formats.
                for (BihPixelFormat badPixelFormat : BihPixelFormat.values()) {
                    if (badPixelFormat == pixelFormat) {
                        // good format
                        continue;
                    }
                    try {
                        BufferedImageHelper.requireCompatible(
                            image,
                            badPixelFormat,
                            premul);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                }
                
                // Bad premul.
                {
                    final boolean badPremum = !premul;
                    try {
                        BufferedImageHelper.requireCompatible(
                            image,
                            pixelFormat,
                            badPremum);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                }
            }
        }
    }
}
