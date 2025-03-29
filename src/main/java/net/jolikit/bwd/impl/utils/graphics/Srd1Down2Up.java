/*
 * Copyright 2025 Jeff Hain
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
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.utils.graphics.PpTlData.PooledIntArrHolder;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Uses a drawer for downscaling and another for upscaling.
 */
public class Srd1Down2Up implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceScaledRectDrawer drawer1;
    
    private final InterfaceScaledRectDrawer drawer2;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public Srd1Down2Up(
        final InterfaceScaledRectDrawer drawer1,
        final InterfaceScaledRectDrawer drawer2) {
        this.drawer1 = LangUtils.requireNonNull(drawer1);
        this.drawer2 = LangUtils.requireNonNull(drawer2);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[down-" + this.drawer1 + ",up-" + this.drawer2 + "]";
    }
    
    @Override
    public void drawScaledRect(
        InterfaceParallelizer parallelizer,
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer) {
        
        if (ScaledRectUtils.drawArgsCheckAndMustReturn(
            srcPixels.getRect(),
            srcRect,
            //
            dstRect,
            dstClip)) {
            return;
        }
        
        final int sw = srcRect.xSpan();
        final int sh = srcRect.ySpan();
        final int dw = dstRect.xSpan();
        final int dh = dstRect.ySpan();
        
        final boolean wUp = (dw > sw);
        final boolean wDown = (dw < sw);
        final boolean hUp = (dh > sh);
        final boolean hDown = (dh < sh);
        
        if ((wUp && hDown) || (wDown && hUp)) {
            /*
             * Got both downscaling and upscaling.
             * First shrinking with drawer1,
             * then growing with drawer2.
             */
            final int interWidth = (wDown ? dw : sw);
            final int interHeight = (hDown ? dh : sh);
            
            final PpTlData tl = PpTlData.DEFAULT_TL_DATA.get();
            
            final PooledIntArrHolder tmpBigArrHolder1 = tl.borrowBigArrHolder();
            
            // Using dst position for intermediary rectangle,
            // and then only comparing spans to figure out scaling needs.
            final GRect interRect = dstRect.withSpans(interWidth, interHeight);
            final GRect interClip = interRect;
            
            final IntArrCopyRowDrawer dstRowDrawerFor1 = new IntArrCopyRowDrawer();
            // We want (interRect.x(),interRect.y())
            // to be interColor32Arr[0].
            final GTransform interTransformArrToDst =
                GTransform.valueOf(
                    GRotation.ROT_0,
                    -interRect.x(),
                    -interRect.y());
            final int interRectArea = interRect.area();
            // No need to zeroize it since interRowDrawer
            // uses COPY (or SRC) blending.
            final int[] interColor32Arr =
                tmpBigArrHolder1.getArr(interRectArea);
            final int interScanlineStride = interRect.xSpan();
            dstRowDrawerFor1.configure(
                interTransformArrToDst,
                interColor32Arr,
                interScanlineStride);
            
            final IntArrSrcPixels srcPixelsFor2 = new IntArrSrcPixels();
            srcPixelsFor2.configure(
                interRect,
                interColor32Arr,
                interScanlineStride);
            
            this.drawer1.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                //
                interRect,
                interClip,
                dstRowDrawerFor1);
            
            this.drawer2.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                srcPixelsFor2,
                interRect,
                //
                dstRect,
                dstClip,
                dstRowDrawer);
            
            tmpBigArrHolder1.release();
        } else {
            final InterfaceScaledRectDrawer scaler;
            if (wDown || hDown) {
                /*
                 * Some downscaling (and no upscaling).
                 */
                scaler = this.drawer1;
            } else {
                /*
                 * Some upscaling (and no downscaling),
                 * or no scaling.
                 */
                scaler = this.drawer2;
            }
            scaler.drawScaledRect(
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
}
