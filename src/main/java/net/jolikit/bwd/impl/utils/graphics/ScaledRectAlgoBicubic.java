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

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.graphics.PpTlData.PooledIntArrHolder;

public class ScaledRectAlgoBicubic extends AbstractScaledRectAlgo {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    static final int DST_AREA_THRESHOLD_FOR_SPLIT = 4 * 1024;
    
    private static final double H = 0.5;
    
    /**
     * For bicubic interpolation.
     */
    private static final double A = -0.5;
    
    private static final double IT_SPAN_SHRINK_FACTOR = 0.5;
    
    private static final ScaledRectAlgoNearest ALGO_NEAREST =
        new ScaledRectAlgoNearest();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectAlgoBicubic() {
    }
    
    @Override
    public int getSrcAreaThresholdForSplit() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    public int getDstAreaThresholdForSplit() {
        return DST_AREA_THRESHOLD_FOR_SPLIT;
    }
    
    @Override
    public double getIterationSpanShrinkFactor() {
        return IT_SPAN_SHRINK_FACTOR;
    }
    
    @Override
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
        //
        InterfaceRowDrawer dstRowDrawer) {
        
        if ((srcRect.xSpan() == dstRect.xSpan())
            && (srcRect.ySpan() == dstRect.ySpan())) {
            /*
             * Faster, and exact in case of alpha.
             */
            ALGO_NEAREST.drawScaledRectChunk(
                colorTypeHelper,
                srcPixels,
                srcRect,
                dstRect,
                dstRectClipped,
                dstYStart,
                dstYEnd,
                dstRowDrawer);
        } else {
            drawScaledRectChunk_bicubic(
                colorTypeHelper,
                srcPixels,
                srcRect,
                dstRect,
                dstRectClipped,
                dstYStart,
                dstYEnd,
                dstRowDrawer);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int bicubicInterpolate(
        PpTlData tl,
        //
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        int sxFloor,
        int syFloor,
        double sxFracFp,
        double syFracFp) {
        
        /*
         * Weight in x cached in array to avoid recomputation.
         * 
         * Caching wx in a 4*dxSpan length array
         * and wy in a 4 length array, both given to this method,
         * doesn't help (probably due to cache misses and memory load).
         */
        
        final double[] wxArr = tl.tmpDoubleArr.getArr(4);
        for (int kx = -1; kx <= 2; kx++) {
            final double wx = cubicWeight(kx - sxFracFp);
            wxArr[kx + 1] = wx;
        }
        
        /*
         * A bit faster to inline PpColorSum logic here
         * rather than using the class (like 27ms vs 33ms).
         */

        int a8 = 0;
        int b8 = 0;
        int c8 = 0;
        int d8 = 0;
        int prevSrcColor32 = 0;

        double resAFp = 0.0;
        double resBFp = 0.0;
        double resCFp = 0.0;
        double resDFp = 0.0;
        
        // Iterating over 4x4 neighborhood.
        for (int ky = -1; ky <= 2; ky++) {
            final int sy = srcRect.clampY(syFloor + ky);
            final double wy = cubicWeight(ky - syFracFp);
            
            for (int kx = -1; kx <= 2; kx++) {
                final int sx = srcRect.clampX(sxFloor + kx);
                final double wx = wxArr[kx + 1];
                
                final double w = wx * wy;
                
                final int srcColor32 = srcPixels.getColor32At(sx, sy);
                if (srcColor32 != prevSrcColor32) {
                    /*
                     * Interpolating in premul, else RGB from low alpha pixels
                     * would have same weight as RGB from high alpha pixels.
                     */
                    final int premulSrcColor32 =
                        colorTypeHelper.asPremul32FromType(srcColor32);
                    a8 = Argb32.getAlpha8(premulSrcColor32);
                    b8 = Argb32.getRed8(premulSrcColor32);
                    c8 = Argb32.getGreen8(premulSrcColor32);
                    d8 = Argb32.getBlue8(premulSrcColor32);
                    prevSrcColor32 = srcColor32;
                }
                
                resAFp += (a8 * w);
                resBFp += (b8 * w);
                resCFp += (c8 * w);
                resDFp += (d8 * w);
            }
        }
        
        final int resA8 = (int) (resAFp + H);
        final int resB8 = (int) (resBFp + H);
        final int resC8 = (int) (resCFp + H);
        final int resD8 = (int) (resDFp + H);
        
        final int dstPremulColor32 =
            colorTypeHelper.toValidPremul32(
                resA8,
                resB8,
                resC8,
                resD8);
        
        final int dstColor32 =
            colorTypeHelper.asTypeFromPremul32(
                dstPremulColor32);
        
        return dstColor32;
    }
    
    /**
     * This weight can be negative.
     * 
     * @param x Its absolute value must be <= 2,
     *        which holds as long as we don't go further
     *        than +-2 around interpolation position.
     */
    private static double cubicWeight(double x) {
        /*
         * Can have accuracy tests to still pass
         * if using a 1024-long table of precomputed weights,
         * but perfs seem the same so not bothering.
         */
        x = Math.abs(x);
        final double x2 = x * x;
        final double ret;
        if (x <= 1.0) {
            ret = x2 * (x * (A + 2) - (A + 3)) + 1;
        } else {
            // Here we assume x <= 2.
            ret = A * (x2 * (x - 5) + 8 * x - 4);
        }
        return ret;
    }
    
    /*
     * 
     */
    
    private static void drawScaledRectChunk_bicubic(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstRectClipped,
        int dstYStart,
        int dstYEnd,
        InterfaceRowDrawer dstRowDrawer) {
        
        // dst pixel width in src pixels,
        // when scaled up/down to match its corresponding src pixels.
        final double dxPixelSpanFp = srcRect.xSpan() / (double) dstRect.xSpan();
        // dst pixel height in src pixels,
        // when scaled up/down to match its corresponding src pixels.
        final double dyPixelSpanFp = srcRect.ySpan() / (double) dstRect.ySpan();
        
        final PpTlData tl = PpTlData.DEFAULT_TL_DATA.get();
        
        final PooledIntArrHolder tmpArrHolder1 = tl.borrowArrHolder();
        
        final int[] dstRowArr = tmpArrHolder1.getArr(dstRectClipped.xSpan());
        
        /*
         * For each destination pixel,
         * this loop chooses a single source position,
         * and then computes a color from the
         * 4x4 surrounding source pixels.
         * ===>
         * If large downscaling (big dst pixels),
         * will not use some source pixels
         * (unless using intermediary scaling iterations).
         * If large upscaling (small dst pixels),
         * will use source pixels from very far,
         * causing blurriness.
         */
        
        // +-0.5 needed due to integer coordinates
        // corresponding to pixels centers.
        final double H = 0.5;
        
        final int dj0 = dstRectClipped.y() - dstRect.y();
        final int djcStart = dstYStart - dstRectClipped.y();
        final int djcEnd = djcStart + (dstYEnd - dstYStart);
        final int di0 = dstRectClipped.x() - dstRect.x();
        
        // Loop on srcY and inside on srcX (memory-friendly).
        for (int djc = djcStart; djc <= djcEnd; djc++) {
            final int dj = dj0 + djc;
            // y in src of destination pixel's center. 
            final double srcYFp = srcRect.y() + (dj + H) * dyPixelSpanFp - H;
            
            final int syFloor = (int) Math.floor(srcYFp);
            final double syFracFp = srcYFp - syFloor;

            for (int dic = 0; dic < dstRectClipped.xSpan(); dic++) {
                final int di = di0 + dic;
                // x in src of destination pixel's center. 
                final double srcXFp = srcRect.x() + (di + H) * dxPixelSpanFp - H;
                
                final int sxFloor = (int) Math.floor(srcXFp);
                final double sxFracFp = srcXFp - sxFloor;
                
                final int dstColor32 = bicubicInterpolate(
                    tl,
                    //
                    colorTypeHelper,
                    //
                    srcPixels,
                    srcRect,
                    sxFloor,
                    syFloor,
                    sxFracFp,
                    syFracFp);
                dstRowArr[dic] = dstColor32;
            }
            
            final int dstRowOffset = 0;
            final int dstRowX = dstRectClipped.x();
            final int dstRowY = dstRectClipped.y() + djc;
            final int dstRowLength = dstRectClipped.xSpan();
            dstRowDrawer.drawRow(
                dstRowArr,
                dstRowOffset,
                dstRowX,
                dstRowY,
                dstRowLength);
        }
        
        tmpArrHolder1.release();
    }
}
