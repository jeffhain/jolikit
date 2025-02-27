/*
 * Copyright 2024-2025 Jeff Hain
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

    private static final ScaledRectDrawerBoxsampled DRAWER_BOXSAMPLED =
        new ScaledRectDrawerBoxsampled();

    private static final ScaledRectDrawerBilinear DRAWER_BILINEAR =
        new ScaledRectDrawerBilinear();

    private static final ScaledRectDrawerBicubic DRAWER_BICUBIC =
        new ScaledRectDrawerBicubic();

    private static final ScaledRectDrawerBoxsampledBilinear DRAWER_BOXSAMPLED_BILINEAR =
        new ScaledRectDrawerBoxsampledBilinear();

    private static final ScaledRectDrawerBoxsampledBicubic DRAWER_BOXSAMPLED_BICUBIC =
        new ScaledRectDrawerBoxsampledBicubic();

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
        
        /*
         * Early switch to NEAREST in case of no-scaling
         * or in case of pixel-aligned upscaling with BOXSAMPLED,
         * to use proper split thresholds.
         * Doesn't make the eventual delegations from within
         * other algos useless, since these algos can be used
         * as sub-algos after others, in which case they might
         * end up equivalent to NEAREST as well.
         */
        if (((srcRect.xSpan() == dstRect.xSpan())
            && (srcRect.ySpan() == dstRect.ySpan()))
            || ((scalingType == BwdScalingType.BOXSAMPLED)
                && ScaledRectUtils.isNearestExact(
                    srcRect.xSpan(), srcRect.ySpan(),
                    dstRect.xSpan(), dstRect.ySpan()))) {
            /*
             * NEAREST equivalent but much faster.
             */
            scalingType = BwdScalingType.NEAREST;
        }
        
        final InterfaceScaledRectDrawer drawer;
        switch (scalingType) {
            case NEAREST:
                drawer = DRAWER_NEAREST;
                break;
            case BOXSAMPLED:
                drawer = DRAWER_BOXSAMPLED;
                break;
            case BILINEAR:
                drawer = DRAWER_BILINEAR;
                break;
            case BICUBIC:
                drawer = DRAWER_BICUBIC;
                break;
            case BOXSAMPLED_BILINEAR:
                drawer = DRAWER_BOXSAMPLED_BILINEAR;
                break;
            case BOXSAMPLED_BICUBIC:
                drawer = DRAWER_BOXSAMPLED_BICUBIC;
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
