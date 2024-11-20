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
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Uses BICUBIC, except when shrinking by a factor
 * superior to two, in which case BILINEAR is used first
 * to reduce BICUBIC shrinking to a factor of two.
 */
public class ScaledRectDrawerBilicubic implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final ScaledRectDrawerBilinear DRAWER_BILINEAR =
        new ScaledRectDrawerBilinear();
    
    private static final ScaledRectDrawerBicubic DRAWER_BICUBIC =
        new ScaledRectDrawerBicubic();
    
    /*
     * 
     */
    
    /**
     * Use 1 to shrink only with BILINEAR.
     */
    private static final double DEFAULT_MAX_BICUBIC_SHRINKING = 2.0;
    
    private static final ThreadLocal<PpTlData> TL_DATA =
        new ThreadLocal<PpTlData>() {
        @Override
        public PpTlData initialValue() {
            return new PpTlData();
        }
    };
    
    /*
     * 
     */
    
    private final double maxBicubicShrinking;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerBilicubic() {
        this(DEFAULT_MAX_BICUBIC_SHRINKING);
    }
    
    /**
     * @param maxBicubicShrinking Max span division to cover with BICUBIC,
     *        after preliminary shrinking using BILINEAR. Must be >= 1.
     *        Use 1 to only shrink using BILINEAR. Default is 2.
     */
    public ScaledRectDrawerBilicubic(double maxBicubicShrinking) {
        if (!(maxBicubicShrinking >= 1.0)) {
            throw new IllegalArgumentException(
                "maxBicubicShrinking ["
                    + maxBicubicShrinking
                    + "] must be >= 1");
        }
        this.maxBicubicShrinking = maxBicubicShrinking;
    }
    
    @Override
    public void drawScaledRect(
        InterfaceParallelizer parallelizer,
        InterfaceColorTypeHelper colorTypeHelper,
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer dstRowDrawer) {
        
        /*
         * Guard closes and args checks.
         */
        
        if (srcRect.isEmpty()) {
            return;
        }
        
        final GRect srcPixelsRect = srcPixels.getRect();
        if (!srcPixelsRect.contains(srcRect)) {
            throw new IllegalArgumentException(
                "srcRect ("
                    + srcRect
                    + ") is not included in srcPixels.getRect() ("
                    + srcPixelsRect
                    + ")");
        }
        
        if (!dstRect.overlaps(dstClip)) {
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
        
        final double xShrForBicu = Math.min(this.maxBicubicShrinking, xShr);
        final double yShrForBicu = Math.min(this.maxBicubicShrinking, yShr);
        
        final int interXSpan = (int) Math.rint(dstRect.xSpan() * xShrForBicu);
        final int interYSpan = (int) Math.rint(dstRect.ySpan() * yShrForBicu);
        
        final boolean needBili =
            (interXSpan < srcRect.xSpan())
            || (interYSpan < srcRect.ySpan());
        
        final InterfaceSrcPixels bicuSrcPixels;
        final GRect bicuSrcRect;
        if (needBili) {
            final boolean needBicu =
                (interXSpan != dstRect.xSpan())
                || (interYSpan != dstRect.ySpan());
            
            final GRect biliDstRect;
            final GRect biliDstClip;
            final InterfaceRowDrawer biliDstRowDrawer;
            if (needBicu) {
                final PpTlData tl = TL_DATA.get();
                
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
                    tl.tmpBigArr1.getArr(interRectArea);
                final int interScanlineStride = interRect.xSpan();
                interRowDrawer.configure(
                    interTransformArrToDst,
                    interColor32Arr,
                    interScanlineStride);
                
                biliDstRect = interRect;
                biliDstClip = interClip;
                biliDstRowDrawer = interRowDrawer;
                
                final IntArrSrcPixels bicuSrcPixelsImpl = new IntArrSrcPixels();
                bicuSrcPixelsImpl.configure(
                    interRect,
                    interColor32Arr,
                    interScanlineStride);
                bicuSrcPixels = bicuSrcPixelsImpl;
                bicuSrcRect = interRect;
            } else {
                biliDstRect = dstRect;
                biliDstClip = dstClip;
                biliDstRowDrawer = dstRowDrawer;
                
                bicuSrcPixels = null;
                bicuSrcRect = null;
            }
            
            DRAWER_BILINEAR.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                //
                biliDstRect,
                biliDstClip,
                biliDstRowDrawer);
        } else {
            bicuSrcPixels = srcPixels;
            bicuSrcRect = srcRect;
        }
        
        if (bicuSrcPixels != null) {
            DRAWER_BICUBIC.drawScaledRect(
                parallelizer,
                colorTypeHelper,
                //
                bicuSrcPixels,
                bicuSrcRect,
                //
                dstRect,
                dstClip,
                dstRowDrawer);
        }
    }
}
