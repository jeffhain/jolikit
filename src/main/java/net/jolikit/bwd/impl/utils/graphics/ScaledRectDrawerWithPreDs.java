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
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.utils.graphics.PpTlData.PooledIntArrHolder;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Uses a preliminary drawer for large downscalings,
 * for regular drawer to only do bounded downscaling
 * (typically when it's not suited for large downscalings).
 */
public class ScaledRectDrawerWithPreDs implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final double DEFAULT_MAX_REGULAR_SHRINKING = 2.0;
    
    /*
     * 
     */
    
    private final InterfaceScaledRectDrawer preliminaryDrawer;
    
    private final InterfaceScaledRectDrawer regularDrawer;
    
    private final double maxRegularShrinking;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerWithPreDs(
        final InterfaceScaledRectDrawer preliminaryDrawer,
        final InterfaceScaledRectDrawer regularDrawer) {
        this(
            preliminaryDrawer,
            regularDrawer,
            DEFAULT_MAX_REGULAR_SHRINKING);
    }
    
    /**
     * @param maxRegularShrinking Max span division to cover with regular
     *        drawer, after shrinking with preliminary drawer.
     *        Must be >= 1. Use 1 to only shrink using preliminary drawer.
     *        Default is 2.
     */
    public ScaledRectDrawerWithPreDs(
        final InterfaceScaledRectDrawer preliminaryDrawer,
        final InterfaceScaledRectDrawer regularDrawer,
        double maxRegularShrinking) {
        if (!(maxRegularShrinking >= 1.0)) {
            throw new IllegalArgumentException(
                "maxRegularShrinking ["
                    + maxRegularShrinking
                    + "] must be >= 1");
        }
        this.preliminaryDrawer = LangUtils.requireNonNull(preliminaryDrawer);
        this.regularDrawer = LangUtils.requireNonNull(regularDrawer);
        this.maxRegularShrinking = maxRegularShrinking;
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
            srcPixels,
            srcRect,
            //
            dstRect,
            dstClip)) {
            return;
        }
        
        /*
         * shr = srcSpan / dstSpan
         * shr = shr1 * shr2
         * shr1 = srcSpan / interSpan
         * shr2 = interSpan / dstSpan
         * interSpan = srcSpan / shr1
         *           = dstSpan * shr2
         */
        
        // Actual shrinking only if > 1.
        final double xShr = srcRect.xSpan() / (double) dstRect.xSpan();
        final double yShr = srcRect.ySpan() / (double) dstRect.ySpan();
        
        final double xShrForReg = Math.min(this.maxRegularShrinking, xShr);
        final double yShrForReg = Math.min(this.maxRegularShrinking, yShr);
        
        final int interXSpan = (int) Math.rint(dstRect.xSpan() * xShrForReg);
        final int interYSpan = (int) Math.rint(dstRect.ySpan() * yShrForReg);
        
        final boolean needPre =
            (interXSpan < srcRect.xSpan())
            || (interYSpan < srcRect.ySpan());
        
        final PpTlData tl = PpTlData.DEFAULT_TL_DATA.get();
        
        final PooledIntArrHolder tmpBigArrHolder1 = tl.borrowBigArrHolder();
        
        final InterfaceSrcPixels regSrcPixels;
        final GRect regSrcRect;
        if (needPre) {
            final boolean needReg =
                (interXSpan != dstRect.xSpan())
                || (interYSpan != dstRect.ySpan());
            
            final GRect regDstRect;
            final GRect regDstClip;
            final InterfaceRowDrawer regDstRowDrawer;
            if (needReg) {
                // Using dst position for intermediary rectangle,
                // and then only comparing spans to figure out scaling needs.
                final GRect interRect = dstRect.withSpans(interXSpan, interYSpan);
                final GRect interClip = interRect;
                
                final IntArrCopyRowDrawer interRowDrawer = new IntArrCopyRowDrawer();
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
                interRowDrawer.configure(
                    interTransformArrToDst,
                    interColor32Arr,
                    interScanlineStride);
                
                regDstRect = interRect;
                regDstClip = interClip;
                regDstRowDrawer = interRowDrawer;
                
                final IntArrSrcPixels regSrcPixelsImpl = new IntArrSrcPixels();
                regSrcPixelsImpl.configure(
                    interRect,
                    interColor32Arr,
                    interScanlineStride);
                regSrcPixels = regSrcPixelsImpl;
                regSrcRect = interRect;
            } else {
                regDstRect = dstRect;
                regDstClip = dstClip;
                regDstRowDrawer = dstRowDrawer;
                
                regSrcPixels = null;
                regSrcRect = null;
            }
            
            this.preliminaryDrawer.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                //
                regDstRect,
                regDstClip,
                regDstRowDrawer);
        } else {
            regSrcPixels = srcPixels;
            regSrcRect = srcRect;
        }
        
        if (regSrcPixels != null) {
            this.regularDrawer.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                regSrcPixels,
                regSrcRect,
                //
                dstRect,
                dstClip,
                dstRowDrawer);
        }
        
        tmpBigArrHolder1.release();
    }
}
