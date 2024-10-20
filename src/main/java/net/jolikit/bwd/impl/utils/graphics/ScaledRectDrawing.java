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

import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.threading.prl.InterfaceParallelizer;

public class ScaledRectDrawing {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final ScaledRectDrawerNearest DRAWER_NEAREST =
        new ScaledRectDrawerNearest();

    private static final ScaledRectDrawerBilinear DRAWER_BILINEAR =
        new ScaledRectDrawerBilinear();

    private static final ScaledRectDrawerBicubic DRAWER_BICUBIC =
        new ScaledRectDrawerBicubic();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Delegates to the proper drawer depending on BwdScalingType.
     * 
     * @param scalingType The scaling type to use.
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
    public static void drawScaledRect(
        InterfaceParallelizer parallelizer,
        BwdScalingType scalingType,
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer) {
        
        final InterfaceScaledRectDrawer drawer;
        switch (scalingType) {
            case NEAREST:
                drawer = DRAWER_NEAREST;
                break;
            case BILINEAR:
                drawer = DRAWER_BILINEAR;
                break;
            case BICUBIC:
                drawer = DRAWER_BICUBIC;
                break;
            default:
                throw new AssertionError();
        }
        
        drawer.drawScaledRect(
            parallelizer,
            colorTypeHelper,
            //
            srcPixels,
            srcRect,
            //
            dstRect,
            dstClip,
            dstRowDrawer);
    }
}
