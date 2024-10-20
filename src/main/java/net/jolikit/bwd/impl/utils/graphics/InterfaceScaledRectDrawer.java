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
package net.jolikit.bwd.impl.utils.graphics;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.threading.prl.InterfaceParallelizer;

public interface InterfaceScaledRectDrawer {

    /**
     * Draws the specified source rectangle of the specified pixels,
     * into the specified destination rectangle.
     * 
     * Dst clip specified in addition to dst rectangle,
     * to be able to get scaling right even in case of clipping
     * (clipped src and dst rectangles could yield slightly different scaling).
     * 
     * We don't have a use case for clipping src rectangle,
     * so no clip is specified for it.
     * If you do, just use your src clip in your srcPixels input,
     * returning default values (like fully transparent color)
     * for pixels out of clip.
     * 
     * @param parallelizer The parallelizer to use.
     * @param colorTypeHelper Helper for source (and destination) color type.
     * @param srcPixels Source pixels.
     * @param srcRect Source rectangle.
     * @param dstRect Destination rectangle.
     * @param dstClip Destination clip.
     * @param dstRowDrawer Drawer where to draw destination pixels,
     *        in the same color format as input.
     * @throws IllegalArgumentException if srcRect is not empty
     *         and not included in srcPixels.getRect().
     */
    public void drawScaledRect(
        InterfaceParallelizer parallelizer,
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer);
}
