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

/**
 * Must be thread-safe and non-blocking,
 * to be usable in parallel.
 */
public interface InterfaceScaledRectAlgo {

    /**
     * Not making this value depend on particular inputs,
     * to keep things simple.
     * 
     * @return The area, in number of source or destination pixels
     *         (using the max) corresponding to clipped destination
     *         rectangle, from which it's worth to split in two
     *         for parallelization.
     */
    public int getAreaThresholdForSplit();
    
    /**
     * @return Factor by which to multiply growing destination span
     *         at each iteration, until reaching destination span.
     *         Must be > 1. Double.POSITIVE_INFINITY means
     *         just using one iteration for the growing span(s),
     *         i.e. using destination rectangle span directly.
     */
    public double getIterationSpanGrowthFactor();

    /**
     * @return Factor by which to multiply shrinking destination span
     *         at each iteration, until reaching destination span.
     *         Must be in [0,1[. Zero means just using one iteration
     *         for the shrinking span(s), i.e. using destination rectangle
     *         span directly.
     */
    public double getIterationSpanShrinkFactor();

    /**
     * Specifying dstRectClipped and not dstClip,
     * because it typically has been computed before this call,
     * and only binding code is supposed to call this method
     * so no risk of user giving dstClip instead.
     * 
     * dstYStart and dstYEnd are useful to split dstRectClipped
     * into multiple sub-areas processed in parallel.
     * 
     * Must work whether colors are alpha-premultiplied or not,
     * i.e. if input is alpha-premultiplied (and valid), each
     * output color's alpha must never be greater than any RGB component
     * (which in practice typically means that the same transform must be
     * applied to each component).
     * 
     * @param colorTypeHelper Helper for source (and destination) color type.
     * @param srcPixels The source pixels. Never empty.
     * @param srcRect Source rectangle. Never empty.
     * @param dstRect Destination rectangle. Never empty.
     * @param dstRectClipped The clipped destination rectangle. Never empty.
     * @param dstYStart Destination "y" to start from (inclusive).
     * @param dstYEnd Destination "y" to end at (inclusive).
     * @param dstRowDrawer Drawer where to draw destination pixels,
     *        in the same color format as input.
     */
    public void drawScaledRectChunk(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstRectClipped,
        int dstYStart,
        int dstYEnd,
        InterfaceRowDrawer dstRowDrawer);
}
