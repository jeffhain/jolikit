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
package net.jolikit.bwd.impl.utils.gprim;

public class LineStippleUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param factor Must be in [1,256].
     * @param pixelNum Must be >= 0.
     * @return Whether the pixel of specified pixelNum must be drawn.
     */
    public static boolean mustDraw(int factor, short pattern, int pixelNum) {
        if (factor != 1) {
            pixelNum /= factor;
        }
        // Pattern is 16 bits long, so we do "% 16" on pixelNum, with bit mask.
        final int shift = pixelNum & 0xF;
        return ((pattern >> shift) & 1) != 0;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private LineStippleUtils() {
    }
}
