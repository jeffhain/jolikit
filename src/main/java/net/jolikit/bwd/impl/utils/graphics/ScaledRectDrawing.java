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
    
    private static final SrdNearest SRD_NEAREST = new SrdNearest();

    private static final SrdBoxsampled SRD_BOXSAMPLED = new SrdBoxsampled();

    private static final SrdIterBili SRD_ITER_BILI = new SrdIterBili();

    private static final SrdIterBicu SRD_ITER_BICU = new SrdIterBicu();

    private static final SrdIterBiliBicu SRD_ITER_BILI_BICU = new SrdIterBiliBicu();

    private static final SrdBoxBicu SRD_BOX_BICU = new SrdBoxBicu();

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
        
        final int sw = srcRect.xSpan();
        final int sh = srcRect.ySpan();
        final int dw = dstRect.xSpan();
        final int dh = dstRect.ySpan();
        
        /*
         * Eventual switch to NEAREST done here,
         * not to have to do it in drawers implementations.
         */
        
        if ((sw == dw)
            && (sh == dh)) {
            // No scaling: NEAREST is the fastest.
            scalingType = BwdScalingType.NEAREST;
        } else if ((scalingType == BwdScalingType.BOXSAMPLED)
            && ScaledRectUtils.isNearestExact(sw, sh, dw, dh)) {
            // Pixel-aligned growth.
            // NEAREST equivalent and faster.
            scalingType = BwdScalingType.NEAREST;
        }
        
        final InterfaceScaledRectDrawer drawer;
        switch (scalingType) {
            case NEAREST:
                drawer = SRD_NEAREST;
                break;
            case BOXSAMPLED:
                drawer = SRD_BOXSAMPLED;
                break;
            case ITERATIVE_BILINEAR:
                drawer = SRD_ITER_BILI;
                break;
            case ITERATIVE_BICUBIC:
                drawer = SRD_ITER_BICU;
                break;
            case ITERATIVE_BILINEAR_BICUBIC:
                drawer = SRD_ITER_BILI_BICU;
                break;
            case BOXSAMPLED_BICUBIC:
                drawer = SRD_BOX_BICU;
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
