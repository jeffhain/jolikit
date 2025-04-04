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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.graphics.PpTlData.PooledIntArrHolder;
import net.jolikit.lang.NbrsUtils;

public class SrdBoxsampled extends AbstractParallelSrd {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Source pixels are iterated over more efficiently than destination pixels,
     * for which source coverage computation machinery takes place,
     * so we use a larger threshold for these. 
     */
    private static final int DEFAULT_SRC_AREA_THRESHOLD_FOR_SPLIT = 1024;
    
    private static final int DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT = 512;
    
    /**
     * Ignoring src pixels which less than an epsilon is covered.
     * Allows to rule out approximation error related edge cases
     * that would take us out of src area, without adding
     * bounds checks overhead.
     * 
     * We consider the following worst case: src is a (1*1) square,
     * and dst is a (n*(n+1)) rectangle with n = sqrt(Integer.MAX_VALUE)
     * (doesn't overflow).
     * Each dst pixel will cover only 1/(n*(n+1)) of src pixel,
     * but we don't want to ignore src contribution.
     * We therefore choose epsilon = 1.0/Integer.MAX_VALUE
     * = 4.656612875245797E-10 (which is < 1/(n*(n+1))).
     */
    private static final double PIXEL_RATIO_EPSILON = 1.0 / Integer.MAX_VALUE;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double H = 0.5;
    
    private final int srcAreaThresholdForSplit;
    
    private final int dstAreaThresholdForSplit;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public SrdBoxsampled() {
        this(
            DEFAULT_SRC_AREA_THRESHOLD_FOR_SPLIT,
            DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT);
    }
    
    public SrdBoxsampled(
        int srcAreaThresholdForSplit,
        int dstAreaThresholdForSplit) {
        this.srcAreaThresholdForSplit = srcAreaThresholdForSplit;
        this.dstAreaThresholdForSplit = dstAreaThresholdForSplit;
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "["
            + this.getClass().getSimpleName()
            + "-"
            + this.srcAreaThresholdForSplit
            + "-"
            + this.dstAreaThresholdForSplit
            + "]";
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getSrcAreaThresholdForSplit() {
        return this.srcAreaThresholdForSplit;
    }
    
    @Override
    protected int getDstAreaThresholdForSplit() {
        return this.dstAreaThresholdForSplit;
    }
    
    /**
     * @param colorTypeHelper Not used.
     */
    @Override
    protected void drawScaledRectChunk(
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
        
        if (isAlignedDownscaling(srcRect, dstRect)) {
            drawRectScaled_boxsampled_alignedDownscaling(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                //
                dstRect,
                dstRectClipped,
                dstYStart,
                dstYEnd,
                dstRowDrawer);
        } else {
            drawRectScaled_boxsampled_general(
                colorTypeHelper,
                //
                srcPixels,
                srcRect,
                //
                dstRect,
                dstRectClipped,
                dstYStart,
                dstYEnd,
                dstRowDrawer);
        }
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Package-private for tests.
     */
    static int boxsampledInterpolate_general(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        double centerXFp,
        double centerYFp,
        double dxPixelSpanFp,
        //
        double clpYSpanFp,
        double loYRatio,
        int fullDyMin,
        int fullDyMax,
        double hiYRatio,
        //
        PpColorSum tmpColorSum) {
        
        final int sx = srcRect.x();
        final int sy = srcRect.y();
        
        // Reference for coordinates deltas.
        // Makes intersections with pixels borders easier to compute.
        final double xaFp = sx - H;
        final double xbFp = srcRect.xMaxLong() + H;
        
        // Clamping.
        final double clpXMinFp = NbrsUtils.toRange(xaFp, xbFp, centerXFp - dxPixelSpanFp * H);
        final double clpXMaxFp = NbrsUtils.toRange(xaFp, xbFp, centerXFp + dxPixelSpanFp * H);
        //
        final double clpXSpanFp = clpXMaxFp - clpXMinFp;
        
        final double dstPixelSurfInSrcInv = 1.0 / (clpXSpanFp * clpYSpanFp);
        if (dstPixelSurfInSrcInv == Double.POSITIVE_INFINITY) {
            /*
             * Tiny surface.
             * Can't happen due to pixel spans, which are never tiny enough,
             * but due to center being out of clip.
             */
            final int srcX = srcRect.clampX(
                BindingCoordsUtils.roundToInt(centerXFp));
            final int srcY = srcRect.clampY(
                BindingCoordsUtils.roundToInt(centerYFp));
            /*
             * Going to premul and back to color format,
             * for consistency with interpolation case.
             */
            final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
            final int srcPremulColor32 = colorTypeHelper.asPremul32FromType(srcColor32);
            return colorTypeHelper.asTypeFromPremul32(srcPremulColor32);
        }
        
        final double clpDxMinFp = (clpXMinFp - xaFp);
        final double clpDxMaxFp = (clpXMaxFp - xaFp);
        
        final int clpDxMinFloor = (int) clpDxMinFp;
        final int clpDxMaxFloor = (int) clpDxMaxFp;
        
        // When no pixel is fully covered in X,
        // fullDxMin is the coordinate of the pixel after the one covered,
        // so as to always have loXRatio used for pixel at fullDxMin-1.
        final double loXRatio;
        final int fullDxMin;
        final int fullDxMax;
        final double hiXRatio;
        if ((clpDxMinFloor == clpDxMaxFloor)
            || (clpDxMinFloor + 1.0 == clpDxMaxFp)) {
            /*
             * Area in same X column.
             * Will only use (loXRatio,fullDxMin-1).
             */
            loXRatio = (clpDxMaxFp - clpDxMinFp);
            fullDxMin = clpDxMinFloor + 1;
            fullDxMax = Integer.MIN_VALUE;
            hiXRatio = 0.0;
        } else {
            /*
             * Area over at least two X columns.
             */
            loXRatio = (clpDxMinFloor + 1 - clpDxMinFp);
            fullDxMin = clpDxMinFloor + 1;
            fullDxMax = clpDxMaxFloor - 1;
            hiXRatio = (clpDxMaxFp - clpDxMaxFloor);
        }
        
        tmpColorSum.clearSum();
        
        if (loYRatio > PIXEL_RATIO_EPSILON) {
            // Top side.
            final int srcY = sy + fullDyMin - 1;
            if (loXRatio > PIXEL_RATIO_EPSILON) {
                // Top-left corner.
                final double ratio = loXRatio * loYRatio;
                final int srcX = sx + fullDxMin - 1;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addPixelContrib(srcColor32, ratio);
            }
            {
                // Top side central.
                final double ratio = loYRatio;
                for (int ki = fullDxMin; ki <= fullDxMax; ki++) {
                    final int srcX = sx + ki;
                    final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                    tmpColorSum.addPixelContrib(srcColor32, ratio);
                }
            }
            if (hiXRatio > PIXEL_RATIO_EPSILON) {
                // Top-right corner.
                final double ratio = hiXRatio * loYRatio;
                final int srcX = sx + fullDxMax + 1;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addPixelContrib(srcColor32, ratio);
            }
        }
        // Horizontal central.
        for (int kj = fullDyMin; kj <= fullDyMax; kj++) {
            final int srcY = sy + kj;
            if (loXRatio > PIXEL_RATIO_EPSILON) {
                final double ratio = loXRatio;
                final int srcX = sx + fullDxMin - 1;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addPixelContrib(srcColor32, ratio);
            }
            // This is where we spend the most time
            // for big downscalings: taking care for this loop
            // be over x and not y.
            for (int ki = fullDxMin; ki <= fullDxMax; ki++) {
                final int srcX = sx + ki;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addFullPixelContrib(srcColor32);
            }
            if (hiXRatio > PIXEL_RATIO_EPSILON) {
                final double ratio = hiXRatio;
                final int srcX = sx + fullDxMax + 1;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addPixelContrib(srcColor32, ratio);
            }
        }
        if (hiYRatio > PIXEL_RATIO_EPSILON) {
            // Bottom side.
            final int srcY = sy + fullDyMax + 1;
            if (loXRatio > PIXEL_RATIO_EPSILON) {
                // Bottom-left corner.
                final double ratio = loXRatio * hiYRatio;
                final int srcX = sx + fullDxMin - 1;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addPixelContrib(srcColor32, ratio);
            }
            {
                // Bottom side central.
                final double ratio = hiYRatio;
                for (int ki = fullDxMin; ki <= fullDxMax; ki++) {
                    final int srcX = sx + ki;
                    final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                    tmpColorSum.addPixelContrib(srcColor32, ratio);
                }
            }
            if (hiXRatio > PIXEL_RATIO_EPSILON) {
                // Bottom-right corner.
                final double ratio = hiXRatio * hiYRatio;
                final int srcX = sx + fullDxMax + 1;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addPixelContrib(srcColor32, ratio);
            }
        }
        
        final int dstPremulColor32 =
            tmpColorSum.toPremulColor32(
                dstPixelSurfInSrcInv);
        
        final int dstColor32 =
            colorTypeHelper.asTypeFromPremul32(
                dstPremulColor32);
        
        return dstColor32;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static boolean isAlignedDownscaling(
        GRect srcRect,
        GRect dstRect) {
        // dstRect never empty due to args checks.
        return ((srcRect.xSpan() % dstRect.xSpan() == 0)
            && (srcRect.ySpan() % dstRect.ySpan() == 0));
    }
    
    /*
     * 
     */
    
    /**
     * sr spans must be multiples of dr spans.
     * 
     * About 10 percents performance gain
     * over the general sampling method.
     * 
     * Rectangles/clip emptiness checks are supposed already done
     * (must not be a public method).
     * 
     * @param srcRect Source rectangle.
     * @param dstRect Destination rectangle.
     * @param dstRectClipped The clipped destination rectangle (not just the clip).
     */
    private static void drawRectScaled_boxsampled_alignedDownscaling(
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
        // when scaled up to match its corresponding src pixels.
        // Exact division.
        final int dxPixelSpan = srcRect.xSpan() / dstRect.xSpan();
        // dst pixel height in src pixels,
        // when scaled up to match its corresponding src pixels.
        // Exact division.
        final int dyPixelSpan = srcRect.ySpan() / dstRect.ySpan();
        
        final PpTlData tl = PpTlData.DEFAULT_TL_DATA.get();
        
        final PooledIntArrHolder tmpArrHolder1 = tl.borrowArrHolder();

        final int[] dstRowArr = tmpArrHolder1.getArr(dstRectClipped.xSpan());
        final PpColorSum tmpColorSum = tl.tmpColorSum;
        tmpColorSum.configure(colorTypeHelper);
        
        final double dstPixelSurfInSrc = (dxPixelSpan * (double) dyPixelSpan);
        final double dstPixelSurfInSrcInv = 1.0 / dstPixelSurfInSrc;
        
        final int sx = srcRect.x();
        final int sy = srcRect.y();
        
        final int dj0 = dstRectClipped.y() - dstRect.y();
        final int djcStart = dstYStart - dstRectClipped.y();
        final int djcEnd = djcStart + (dstYEnd - dstYStart);
        // Looping on destination pixels.
        for (int djc = djcStart; djc <= djcEnd; djc++) {
            final int dj = dj0 + djc;
            final int srcYOffset = sy + dj * dyPixelSpan;
            final int di0 = dstRectClipped.x() - dstRect.x();
            for (int dic = 0; dic < dstRectClipped.xSpan(); dic++) {
                final int di = di0 + dic;
                final int srcXOffset = sx + di * dxPixelSpan;
                final int dstColor32 =
                    boxsampledInterpolate_alignedShrinking(
                        colorTypeHelper,
                        //
                        srcPixels,
                        dstPixelSurfInSrcInv,
                        srcXOffset,
                        srcYOffset,
                        dxPixelSpan,
                        dyPixelSpan,
                        //
                        tmpColorSum);
                dstRowArr[dic] = dstColor32;
            }
            
            final int dsrRowOffset = 0;
            final int dstRowX = dstRectClipped.x();
            final int dstRowY = dstRectClipped.y() + djc;
            final int dstRowLength = dstRectClipped.xSpan();
            dstRowDrawer.drawRow(
                dstRowArr,
                dsrRowOffset,
                dstRowX,
                dstRowY,
                dstRowLength);
        }
        
        tmpArrHolder1.release();
    }
    
    private static int boxsampledInterpolate_alignedShrinking(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        double dstPixelSurfInSrcInv,
        int srcXOffset,
        int srcYOffset,
        int dxPixelSpan,
        int dyPixelSpan,
        //
        PpColorSum tmpColorSum) {
        
        /*
         * Doesn't seem to help to inline PpColorSum logic here.
         */
        
        tmpColorSum.clearSum();
        
        for (int kj = 0; kj < dyPixelSpan; kj++) {
            final int srcY = srcYOffset + kj;
            for (int ki = 0; ki < dxPixelSpan; ki++) {
                final int srcX = srcXOffset + ki;
                final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                tmpColorSum.addFullPixelContrib(srcColor32);
            }
        }
        
        final int dstPremulColor32 =
            tmpColorSum.toPremulColor32(
                dstPixelSurfInSrcInv);
        
        final int dstColor32 =
            colorTypeHelper.asTypeFromPremul32(
                dstPremulColor32);
        
        return dstColor32;
    }
    
    /**
     * Rectangles/clip emptiness checks are supposed already done
     * (must not be a public method).
     * 
     * @param srcRect Source rectangle.
     * @param dstRect Destination rectangle.
     * @param dstRectClipped The clipped destination rectangle (not just the clip).
     */
    private static void drawRectScaled_boxsampled_general(
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
        final PpColorSum tmpColorSum = tl.tmpColorSum;
        tmpColorSum.configure(colorTypeHelper);
        
        final int sy = srcRect.y();
        // Reference for coordinates deltas.
        // Makes intersections with pixels borders easier to compute.
        final double yaFp = sy - H;
        final double ybFp = srcRect.yMaxLong() + H;
        
        // +-0.5 needed due to integer coordinates
        // corresponding to pixels centers.
        final double H = 0.5;
        final int dj0 = dstRectClipped.y() - dstRect.y();
        final int djcStart = dstYStart - dstRectClipped.y();
        final int djcEnd = djcStart + (dstYEnd - dstYStart);
        // Loop on srcY and inside on srcX (memory-friendly).
        for (int djc = djcStart; djc <= djcEnd; djc++) {
            final int dj = dj0 + djc;
            // y in src of destination pixel's center. 
            final double centerYFp = srcRect.y() + (dj + H) * dyPixelSpanFp - H;
            
            // Clamping.
            final double clpYMinFp = NbrsUtils.toRange(yaFp, ybFp, centerYFp - dyPixelSpanFp * H);
            final double clpYMaxFp = NbrsUtils.toRange(yaFp, ybFp, centerYFp + dyPixelSpanFp * H);
            //
            final double clpYSpanFp = clpYMaxFp - clpYMinFp;
            
            final double clpDyMinFp = (clpYMinFp - yaFp);
            final double clpDyMaxFp = (clpYMaxFp - yaFp);
            
            final int clpDyMinFloor = (int) clpDyMinFp;
            final int clpDyMaxFloor = (int) clpDyMaxFp;
            
            // When no pixel is fully covered in Y,
            // fullDyMin is the coordinate of the pixel after the one covered,
            // so as to always have loYRatio used for pixel at fullDyMin-1.
            final double loYRatio;
            final int fullDyMin;
            final int fullDyMax;
            final double hiYRatio;
            if ((clpDyMinFloor == clpDyMaxFloor)
                || (clpDyMinFloor + 1.0 == clpDyMaxFp)) {
                /*
                 * Area in same Y row.
                 * Will only use (loYRatio,fullDyMin-1).
                 */
                loYRatio = (clpDyMaxFp - clpDyMinFp);
                fullDyMin = clpDyMinFloor + 1;
                fullDyMax = Integer.MIN_VALUE;
                hiYRatio = 0.0;
            } else {
                loYRatio = (clpDyMinFloor + 1 - clpDyMinFp);
                fullDyMin = clpDyMinFloor + 1;
                fullDyMax = clpDyMaxFloor - 1;
                hiYRatio = (clpDyMaxFp - clpDyMaxFloor);
            }
            
            final int di0 = dstRectClipped.x() - dstRect.x();
            for (int dic = 0; dic < dstRectClipped.xSpan(); dic++) {
                final int di = di0 + dic;
                // x in src of destination pixel's center. 
                final double centerXFp = srcRect.x() + (di + H) * dxPixelSpanFp - H;
                final int dstColor32 = boxsampledInterpolate_general(
                    colorTypeHelper,
                    //
                    srcPixels,
                    srcRect,
                    centerXFp,
                    centerYFp,
                    dxPixelSpanFp,
                    //
                    clpYSpanFp,
                    loYRatio,
                    fullDyMin,
                    fullDyMax,
                    hiYRatio,
                    //
                    tmpColorSum);
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
