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

public interface InterfaceRectDrawer {

    /**
     * Clip and coordinates are in user frame of reference.
     * Coordinates are not supposed to be clipped yet.
     * 
     * @param clip Clip in user coordinates.
     * @param x Top-left x in user coordinates.
     * @param y Top-left y in user coordinates.
     * @param xSpan X span in user coordinates.
     * @param ySpan Y span in user coordinates.
     */
    public void drawRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan);
    
    /**
     * Clip and coordinates are in user frame of reference.
     * Coordinates are not supposed to be clipped yet.
     * 
     * @param clip Clip in user coordinates.
     * @param x Top-left x in user coordinates.
     * @param y Top-left y in user coordinates.
     * @param xSpan X span in user coordinates.
     * @param ySpan Y span in user coordinates.
     * @param areHorVerFlipped True if transform rotation is 90 or 270 degrees,
     *        in which case it should be more efficient to fill using
     *        vertical lines than horizontal lines.
     */
    public void fillRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped);
}
