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

import net.jolikit.bwd.api.graphics.GRect;

public interface InterfaceLineDrawer {
    
    /**
     * Clip and coordinates are in user frame of reference.
     * Coordinates are not supposed to be clipped yet.
     * 
     * Convenience method.
     * 
     * Should be equivalent to drawLine(clip, x1, y1, x2, y2, 1, (short) -1, 0).
     */
    public void drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2);

    /**
     * Clip and coordinates are in user frame of reference.
     * Coordinates are not supposed to be clipped yet.
     * 
     * This method is not means to be only used for actually stippled lines,
     * and should special-case to efficient plain line drawing if pattern is plain.
     * 
     * @param factor Must be in [1,256].
     * @param pixelNum Must be >= 0.
     * @return The pixelNum to continue drawing.
     */
    public int drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum);
}
