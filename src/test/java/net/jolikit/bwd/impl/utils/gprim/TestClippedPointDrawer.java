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

import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.lang.Dbg;

class TestClippedPointDrawer implements InterfaceClippedPointDrawer {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * Set it to true to help debug where it comes from.
     */
    private static final boolean MUST_FAIL_FAST_ON_PIXEL_MULTIPLE_PAINTING = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * No key if no painted, both for perfs,
     * and to allow to use key set to know which pixels were painted.
     */
    final SortedMap<GPoint,Integer> paintedCountByPixel = new TreeMap<GPoint,Integer>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public TestClippedPointDrawer() {
    }
    
    @Override
    public void drawPointInClip(int x, int y) {
        if (DEBUG) {
            Dbg.log("drawPointInClip(" + x + "," + y + ")");
        }
        final GPoint pixel = GPoint.valueOf(x, y);
        
        Integer count = this.paintedCountByPixel.get(pixel);
        if (count == null) {
            count = 1;
        } else {
            count = count + 1;
            if (MUST_FAIL_FAST_ON_PIXEL_MULTIPLE_PAINTING) {
                final AssertionError e = new AssertionError("painted twice : " + pixel);
                Dbg.log(e);
                throw e;
            }
        }
        this.paintedCountByPixel.put(pixel, count);
    }
}
