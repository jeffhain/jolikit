/*
 * Copyright 2019-2021 Jeff Hain
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
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

/**
 * Designed for drawing of client area (dirty) parts into
 * some backing library's device pixel sized buffer.
 * 
 * Simple quick algorithm, without anti aliasing (which would cause a dependency
 * to some color model).
 */
public class ScaledRectDrawer {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * 2000 works, but using 10 times that to make sure
     * that we don't get too many wasteful splits.
     */
    static final double MIN_AREA_COST_FOR_SPLIT_SMOOTH = 10.0 * 2000.0;
    
    /**
     * 10 times smooth threshold,
     * because this algorithm has much less overhead.
     */
    static final double MIN_AREA_COST_FOR_SPLIT_CLOSEST = 10.0 * MIN_AREA_COST_FOR_SPLIT_SMOOTH;
    
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
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyAlgoType {
        CLOSEST,
        SMOOTH_ALIGNED_SHRINKING,
        SMOOTH_GENERAL;
    }
    
    /*
     * 
     */
    
    private static class MyArgbSum {
        double contribSumA8;
        double contribSumR8;
        double contribSumG8;
        double contribSumB8;
        //
        int newestArgb32 = 0;
        double newestArgb32A8;
        double newestArgb32R8;
        double newestArgb32G8;
        double newestArgb32B8;
        public MyArgbSum() {
        }
        public void clear() {
            this.contribSumA8 = 0.0;
            this.contribSumR8 = 0.0;
            this.contribSumG8 = 0.0;
            this.contribSumB8 = 0.0;
            /*
             * Not clearing cache: can be used
             * across multiple destination pixels.
             */
        }
        public void addFullPixelContrib(int toAddArgb32) {
            if (toAddArgb32 != this.newestArgb32) {
                this.updateNewestArgbData(toAddArgb32);
            }
            this.contribSumA8 += this.newestArgb32A8;
            this.contribSumR8 += this.newestArgb32R8;
            this.contribSumG8 += this.newestArgb32G8;
            this.contribSumB8 += this.newestArgb32B8;
        }
        /**
         * @param ratio Covering ratio (in [0,1]).
         */
        public void addPixelContrib(int toAddArgb32, double ratio) {
            if (toAddArgb32 != this.newestArgb32) {
                this.updateNewestArgbData(toAddArgb32);
            }
            this.contribSumA8 += ratio * this.newestArgb32A8;
            this.contribSumR8 += ratio * this.newestArgb32R8;
            this.contribSumG8 += ratio * this.newestArgb32G8;
            this.contribSumB8 += ratio * this.newestArgb32B8;
        }
        /**
         * This caching helps when consecutively used
         * src pixels (for a same or different dst pixels)
         * have the same ARGB.
         */
        private void updateNewestArgbData(int toAddArgb32) {
            this.newestArgb32A8 = Argb32.getAlpha8(toAddArgb32);
            this.newestArgb32R8 = Argb32.getRed8(toAddArgb32);
            this.newestArgb32G8 = Argb32.getGreen8(toAddArgb32);
            this.newestArgb32B8 = Argb32.getBlue8(toAddArgb32);
            this.newestArgb32 = toAddArgb32;
        }
        public int toArgb32(double dstPixelSurfInSrcInv) {
            /*
             * Getting values back into [0.0,255.0] (approx)
             * by dividing by total dst pixel surf in src.
             */
            final int alpha8 = (int) (this.contribSumA8 * dstPixelSurfInSrcInv + 0.5);
            final int red8 = (int) (this.contribSumR8 * dstPixelSurfInSrcInv + 0.5);
            final int green8 = (int) (this.contribSumG8 * dstPixelSurfInSrcInv + 0.5);
            final int blue8 = (int) (this.contribSumB8 * dstPixelSurfInSrcInv + 0.5);
            return toArgb32FromInt8_noCheck(alpha8, red8, green8, blue8);
        }
    }
    
    /*
     * 
     */
    
    /**
     * Data common to splittables, to save memory.
     */
    private static class MyCmnData {
        final MyAlgoType algoType;
        final InterfaceSrcPixels srcPixels;
        final GRect sr;
        final GRect dr;
        final GRect drc;
        final InterfaceRowDrawer rowDrawer;
        final double lineCost;
        final double minAreaCostForSplit;
        public MyCmnData(
            MyAlgoType algoType,
            InterfaceSrcPixels srcPixels,
            GRect sr,
            GRect dr,
            GRect drc,
            InterfaceRowDrawer rowDrawer,
            double lineCost,
            double minAreaCostForSplit) {
            this.algoType = algoType;
            this.srcPixels = srcPixels;
            this.sr = sr;
            this.dr = dr;
            this.drc = drc;
            this.rowDrawer = rowDrawer;
            this.lineCost = lineCost;
            this.minAreaCostForSplit = minAreaCostForSplit;
        }
    }
            
    private static class MySplittable implements InterfaceSplittable {
        final MyCmnData cmn;
        private int startY;
        private int endY;
        public MySplittable(
            MyCmnData cmn,
            int startY,
            int endY) {
            if (startY > endY) {
                throw new IllegalArgumentException(startY + " > " + endY);
            }
            this.cmn = cmn;
            this.startY = startY;
            this.endY = endY;
        }
        public void configure(int startY, int endY) {
            this.startY = startY;
            this.endY = endY;
        }
        @Override
        public String toString() {
            return "[" + this.startY + "," + this.endY + "]";
        }
        @Override
        public void run() {
            drawRectScaled(
                this.cmn.algoType,
                this.cmn.srcPixels,
                this.cmn.sr,
                this.cmn.dr,
                this.cmn.drc,
                this.startY,
                this.endY,
                this.cmn.rowDrawer);
        }
        @Override
        public boolean worthToSplit() {
            return isWorthToSplit(
                this.cmn.lineCost,
                this.startY,
                this.endY,
                this.cmn.minAreaCostForSplit);
        }
        @Override
        public InterfaceSplittable split() {
            final int midY = this.startY + ((this.endY - this.startY) / 2);
            final MySplittable ret = new MySplittable(
                this.cmn,
                midY + 1,
                this.endY);
            // Configured last, since it changes endY.
            this.configure(this.startY, midY);
            return ret;
        }
    }
    
    /*
     * 
     */
    
    private static class MyTlData {
        private final IntArrHolder tmpArr1 = new IntArrHolder();
        private final IntArrHolder tmpArr2 = new IntArrHolder();
        private final MyArgbSum tmpArgbSum = new MyArgbSum();
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Static, to avoid ThreadLocal and MyTlData churn when this class is used
     * from many short-lived objects, such as graphics.
     */
    private static final ThreadLocal<MyTlData> TL_DATA =
        new ThreadLocal<MyTlData>() {
        @Override
        public MyTlData initialValue() {
            return new MyTlData();
        }
    };
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Draws the specified source rectangle of the specified pixels,
     * into the specified destination rectangle.
     * 
     * Dst clip specified in addition to dst rectangle,
     * to be able to get scaling right even in case of clipping
     * (clipped dst rectangles could yield slightly different scaling).
     * 
     * We don't have a use case for clipping src rectangle,
     * so no clip is specified for it.
     * If you do, just use your src clip in your srcPixels input,
     * returning default values (like fully transparent color)
     * for pixels out of clip.
     * 
     * @param mustUseSmoothElseClosest True to use box sampling algorithm
     *        (slower but smooth), false to use nearest neighbor algorithm
     *        (much faster but pixelated and with information loss on shrinking).
     * @throws IllegalArgumentException if srcRect positions have negative coordinates
     *         (no offset being defined for srcPixels,
     *         pixel (0,0) must be the one of index 0).
     */
    public static void drawRectScaled(
        InterfaceParallelizer parallelizer,
        boolean mustUseSmoothElseClosest,
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer rowDrawer) {
        
        drawRectScaled(
            MIN_AREA_COST_FOR_SPLIT_CLOSEST,
            MIN_AREA_COST_FOR_SPLIT_SMOOTH,
            parallelizer,
            mustUseSmoothElseClosest,
            srcPixels,
            srcRect,
            dstRect,
            dstClip,
            rowDrawer);
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    static void drawRectScaled(
        double minAreaCostForSplitClosest,
        double minAreaCostForSplitSmooth,
        //
        InterfaceParallelizer parallelizer,
        boolean mustUseSmoothElseClosest,
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        GRect dstRect,
        GRect dstClip,
        InterfaceRowDrawer rowDrawer) {
        
        /*
         * Args checks.
         */
        
        final GRect sr = LangUtils.requireNonNull(srcRect);
        final GRect dr = dstRect;
        final GRect dc = dstClip;
        // Implicit null checks.
        final GRect drc = dr.intersected(dc);
        LangUtils.requireNonNull(rowDrawer);
        
        if ((sr.x() | sr.y()) < 0) {
            throw new IllegalArgumentException(
                "srcRect [" + sr + "] has negative coordinates");
        }
        
        /*
         * Guard clauses.
         */
        
        if (sr.isEmpty()
            || drc.isEmpty()) {
            return;
        }
        
        /*
         * 
         */
        
        final MyAlgoType algoType = computeAlgoType(
            mustUseSmoothElseClosest,
            sr,
            dr,
            drc);
        
        final int startY = drc.y();
        final int endY = drc.yMax();
        
        boolean didGoPrl = false;
        
        if (parallelizer.getParallelism() >= 2) {
            final double lineCost = computeLineCost(sr, dr, drc);
            final double minAreaCostForSplit;
            if (algoType == MyAlgoType.CLOSEST) {
                minAreaCostForSplit = minAreaCostForSplitClosest;
            } else {
                minAreaCostForSplit = minAreaCostForSplitSmooth;
            }
            if (isWorthToSplit(lineCost, startY, endY, minAreaCostForSplit)) {
                didGoPrl = true;
                
                final MyCmnData cmn =
                    new MyCmnData(
                        algoType,
                        srcPixels,
                        sr,
                        dr,
                        drc,
                        rowDrawer,
                        lineCost,
                        minAreaCostForSplit);
                
                final MySplittable splittable =
                    new MySplittable(
                        cmn,
                        startY,
                        endY);
                
                parallelizer.execute(splittable);
            }
        }
        
        if (!didGoPrl) {
            drawRectScaled(
                algoType,
                srcPixels,
                sr,
                dr,
                drc,
                startY,
                endY,
                rowDrawer);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private ScaledRectDrawer() {
    }
    
    /*
     * 
     */
    
    private static MyAlgoType computeAlgoType(
        boolean mustUseSmoothElseClosest,
        GRect sr,
        GRect dr,
        GRect drc) {
        
        final MyAlgoType ret;
        if ((!mustUseSmoothElseClosest)
            || ((dr.xSpan() % sr.xSpan() == 0)
                && (dr.ySpan() % sr.ySpan() == 0))) {
            // Always wanting to use closest,
            // or no scale change, or pixel-aligned growth.
            ret = MyAlgoType.CLOSEST;
        } else {
            if ((sr.xSpan() % dr.xSpan() == 0)
                && (sr.ySpan() % dr.ySpan() == 0)) {
                // Pixel-aligned shrinking.
                ret = MyAlgoType.SMOOTH_ALIGNED_SHRINKING;
            } else {
                // General case.
                ret = MyAlgoType.SMOOTH_GENERAL;
            }
        }
        
        return ret;
    }

    /*
     * 
     */
    
    private static double computeLineCost(
        GRect sr,
        GRect dr,
        GRect drc) {
        final double dstPixelCost = computeDstPixelCost(sr, dr);
        return drc.xSpan() * dstPixelCost;
    }
    
    private static double computeDstPixelCost(
        GRect sr,
        GRect dr) {
        final long srArea = sr.areaLong();
        final long drArea = dr.areaLong();
        final double ret;
        if (srArea <= drArea) {
            ret = 1.0;
        } else {
            ret = (srArea / (double) drArea);
        }
        return ret;
    }
    
    private static boolean isWorthToSplit(
        double lineCost,
        int startY,
        int endY,
        double minAreaCostForSplit) {
        final int lineCount = (endY - startY + 1);
        final boolean ret;
        if (lineCount <= 1) {
            ret = false;
        } else {
            final double areaCost = lineCount * lineCost;
            ret = (areaCost >= minAreaCostForSplit);  
        }
        return ret;
    }

    /*
     * 
     */

    /**
     * Rectangles/clip emptiness checks are supposed already done
     * (must not be a public method).
     * 
     * @param sr Source rectangle.
     * @param dr Destination rectangle.
     * @param drc The clipped destination rectangle (not just the clip).
     */
    private static void drawRectScaled(
        MyAlgoType algoType,
        InterfaceSrcPixels srcPixels,
        GRect sr,
        GRect dr,
        GRect drc,
        int startY,
        int endY,
        InterfaceRowDrawer rowDrawer) {
        
        switch (algoType) {
            case CLOSEST: {
                drawRectScaled_closest(
                    srcPixels,
                    sr,
                    dr,
                    drc,
                    startY,
                    endY,
                    rowDrawer);
            } break;
            case SMOOTH_ALIGNED_SHRINKING: {
                drawRectScaled_smooth_alignedShrinking(
                    srcPixels,
                    sr,
                    dr,
                    drc,
                    startY,
                    endY,
                    rowDrawer);
            } break;
            case SMOOTH_GENERAL: {
                drawRectScaled_smooth_general(
                    srcPixels,
                    sr,
                    dr,
                    drc,
                    startY,
                    endY,
                    rowDrawer);
            } break;
            default:
                throw new AssertionError();
        }
    }
    
    /**
     * Rectangles/clip emptiness checks are supposed already done
     * (must not be a public method).
     * 
     * @param sr Source rectangle.
     * @param dr Destination rectangle.
     * @param drc The clipped destination rectangle (not just the clip).
     */
    private static void drawRectScaled_closest(
        InterfaceSrcPixels srcPixels,
        GRect sr,
        //
        GRect dr,
        GRect drc,
        int startY,
        int endY,
        //
        InterfaceRowDrawer rowDrawer) {
        
        // Possibly null.
        final int[] srcColor32Arr = srcPixels.color32Arr();
        final int srcScanlineStride = srcPixels.getScanlineStride();
        
        final int sx = sr.x();
        final int sy = sr.y();
        final int srw = sr.xSpan();
        final int srh = sr.ySpan();
        
        final int drw = dr.xSpan();
        final int drh = dr.ySpan();
        final int drcw = drc.xSpan();
        
        final int dj0 = (drc.y() - dr.y());
        final int di0 = (drc.x() - dr.x());

        final double di_to_0_1_factor = 1.0 / drw;
        final double dj_to_0_1_factor = 1.0 / drh;

        // Optimization, to avoid useless scalings.
        final boolean gotXScaling = (drw != srw);
        final boolean gotYScaling = (drh != srh);

        final MyTlData tl = TL_DATA.get();
        
        final int[] rowArr;
        // Optimization, to avoid computing columns scaling for each row.
        final int[] siByDicArr;
        if (gotXScaling) {
            rowArr = tl.tmpArr1.getArr(drcw);

            siByDicArr = tl.tmpArr2.getArr(drcw);
            for (int dic = 0; dic < drcw; dic++) {
                final int di = di0 + dic;
                final int si = computeSrcIndex(
                    srw,
                    di_to_0_1_factor,
                    di);
                siByDicArr[dic] = si;
            }
        } else {
            // Src is already row-scaled.
            if (srcColor32Arr != null) {
                rowArr = srcColor32Arr;
            } else {
                rowArr = tl.tmpArr1.getArr(drcw);
            }

            siByDicArr = null;
        }

        int prevSj = -1;
        final int djcStart = startY - drc.y();
        final int djcEnd = djcStart + (endY - startY);
        for (int djc = djcStart; djc <= djcEnd; djc++) {
            final int dj = dj0 + djc;
            final int sj;
            if (gotYScaling) {
                sj = computeSrcIndex(
                    srh,
                    dj_to_0_1_factor,
                    dj);
            } else {
                sj = dj;
            }

            final int rowOffset;
            if ((!gotXScaling) && (srcColor32Arr != null)) {
                // Using input pixel array directly.
                rowOffset = (sy + sj) * srcScanlineStride + sx + di0;
            } else {
                rowOffset = 0;
                final boolean movedToNewSrcRow = (sj != prevSj);
                if (movedToNewSrcRow) {
                    final int py = sy + sj;
                    for (int dic = 0; dic < drcw; dic++) {
                        final int di = di0 + dic;
                        final int si = (gotXScaling ? siByDicArr[dic] : di);
                        final int px = sx + si;
                        // Not bothering to optimize in case we got the src array,
                        // this method should be fast enough, and not do useless checks.
                        final int color32 = srcPixels.getColor32At(px, py);
                        rowArr[dic] = color32;
                    }
                }
            }

            final int dstX = drc.x();
            final int dstY = drc.y() + djc;
            final int length = drcw;
            rowDrawer.drawRow(
                rowArr,
                rowOffset,
                dstX,
                dstY,
                length);

            prevSj = sj;
        }
    }

    /**
     * sr spans must be multiples of dr spans.
     * 
     * About 10 percents performance gain
     * over the general smoothing method.
     * 
     * Rectangles/clip emptiness checks are supposed already done
     * (must not be a public method).
     * 
     * @param sr Source rectangle.
     * @param dr Destination rectangle.
     * @param drc The clipped destination rectangle (not just the clip).
     */
    private static void drawRectScaled_smooth_alignedShrinking(
        InterfaceSrcPixels srcPixels,
        GRect sr,
        //
        GRect dr,
        GRect drc,
        int startY,
        int endY,
        //
        InterfaceRowDrawer rowDrawer) {
        
        // dst pixel width in src pixels,
        // when scaled up to match its corresponding src pixels.
        // Exact division.
        final int xFactor = (sr.xSpan() / dr.xSpan());
        final int srcHorCount = xFactor;
        // dst pixel height in src pixels,
        // when scaled up to match its corresponding src pixels.
        // Exact division.
        final int yFactor = (sr.ySpan() / dr.ySpan());
        final int srcVerCount = yFactor;
        
        final MyTlData tl = TL_DATA.get();
        
        final int[] rowArr = tl.tmpArr1.getArr(drc.xSpan());
        final MyArgbSum tmpArgbSum = tl.tmpArgbSum;
        
        final double dstPixelSurfInSrc = (xFactor * (double) yFactor);
        final double dstPixelSurfInSrcInv = 1.0 / dstPixelSurfInSrc;
        
        final int sx = sr.x();
        final int sy = sr.y();
        
        final int dj0 = drc.y() - dr.y();
        final int djcStart = startY - drc.y();
        final int djcEnd = djcStart + (endY - startY);
        for (int djc = djcStart; djc <= djcEnd; djc++) {
            final int dj = dj0 + djc;
            final int srcYOffset = sy + dj * srcVerCount;
            final int di0 = drc.x() - dr.x();
            for (int dic = 0; dic < drc.xSpan(); dic++) {
                final int di = di0 + dic;
                final int srcXOffset = sx + di * srcHorCount;
                //
                tmpArgbSum.clear();
                for (int kj = 0; kj < srcVerCount; kj++) {
                    final int srcY = srcYOffset + kj;
                    for (int ki = 0; ki < srcHorCount; ki++) {
                        final int srcX = srcXOffset + ki;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addFullPixelContrib(srcColor32);
                    }
                }
                rowArr[dic] = tmpArgbSum.toArgb32(dstPixelSurfInSrcInv);
            }
            
            final int rowOffset = 0;
            final int dstX = drc.x();
            final int dstY = drc.y() + djc;
            final int length = drc.xSpan();
            rowDrawer.drawRow(
                rowArr,
                rowOffset,
                dstX,
                dstY,
                length);
        }
    }
    
    /**
     * Rectangles/clip emptiness checks are supposed already done
     * (must not be a public method).
     * 
     * @param sr Source rectangle.
     * @param dr Destination rectangle.
     * @param drc The clipped destination rectangle (not just the clip).
     */
    private static void drawRectScaled_smooth_general(
            InterfaceSrcPixels srcPixels,
            GRect sr,
            //
            GRect dr,
            GRect drc,
            int startY,
            int endY,
            //
            InterfaceRowDrawer rowDrawer) {

        // dst pixel width in src pixels,
        // when scaled up/down to match its corresponding src pixels.
        final double xFactor = (sr.xSpan() / (double) dr.xSpan());
        // dst pixel height in src pixels,
        // when scaled up/down to match its corresponding src pixels.
        final double yFactor = (sr.ySpan() / (double) dr.ySpan());
        
        final MyTlData tl = TL_DATA.get();
        
        final int[] rowArr = tl.tmpArr1.getArr(drc.xSpan());
        final MyArgbSum tmpArgbSum = tl.tmpArgbSum;
        
        final double dstPixelSurfInSrc = xFactor * yFactor;
        final double dstPixelSurfInSrcInv = 1.0 / dstPixelSurfInSrc;
        
        final int sx = sr.x();
        final int sy = sr.y();
        
        final int dj0 = drc.y() - dr.y();
        final int djcStart = startY - drc.y();
        final int djcEnd = djcStart + (endY - startY);
        for (int djc = djcStart; djc <= djcEnd; djc++) {
            final int dj = dj0 + djc;
            final double relSrcYMin = dj * yFactor;
            final double relSrcYMax = (dj + 1) * yFactor;
            /*
             * ceil+cast for ceil, and just cast for floor,
             * works because (src) values are positive in int range.
             */
            final int relSrcYMinCeil = (int) Math.ceil(relSrcYMin);
            final int relSrcYMaxFloor = (int) relSrcYMax;
            final double topYRatio;
            final double bottomYRatio;
            if (relSrcYMinCeil <= relSrcYMaxFloor) {
                topYRatio = (relSrcYMinCeil - relSrcYMin);
                bottomYRatio = (relSrcYMax - relSrcYMaxFloor);
            } else {
                topYRatio = yFactor;
                bottomYRatio = 0.0;
            }
            //
            final int di0 = drc.x() - dr.x();
            for (int dic = 0; dic < drc.xSpan(); dic++) {
                final int di = di0 + dic;
                final double relSrcXMin = di * xFactor;
                final double relSrcXMax = (di + 1) * xFactor;
                /*
                 * ceil+cast for ceil, and just cast for floor,
                 * works because (src) values are positive in int range.
                 */
                final int relSrcXMinCeil = (int) Math.ceil(relSrcXMin);
                final int relSrcXMaxFloor = (int) relSrcXMax;
                final double leftXRatio;
                final double rightXRatio;
                if (relSrcXMinCeil <= relSrcXMaxFloor) {
                    leftXRatio = (relSrcXMinCeil - relSrcXMin);
                    rightXRatio = (relSrcXMax - relSrcXMaxFloor);
                } else {
                    leftXRatio = xFactor;
                    rightXRatio = 0.0;
                }
                //
                tmpArgbSum.clear();
                if (leftXRatio > PIXEL_RATIO_EPSILON) {
                    if (topYRatio > PIXEL_RATIO_EPSILON) {
                        final double ratio = leftXRatio * topYRatio;
                        final int srcX = sx + relSrcXMinCeil - 1;
                        final int srcY = sy + relSrcYMinCeil - 1;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addPixelContrib(srcColor32, ratio);
                    }
                    {
                        final double ratio = leftXRatio;
                        for (int kj = relSrcYMinCeil; kj < relSrcYMaxFloor; kj++) {
                            final int srcX = sx + relSrcXMinCeil - 1;
                            final int srcY = sy + kj;
                            final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                            tmpArgbSum.addPixelContrib(srcColor32, ratio);
                        }
                    }
                    if (bottomYRatio > PIXEL_RATIO_EPSILON) {
                        final double ratio = leftXRatio * bottomYRatio;
                        final int srcX = sx + relSrcXMinCeil - 1;
                        final int srcY = sy + relSrcYMaxFloor;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addPixelContrib(srcColor32, ratio);
                    }
                }
                for (int ki = relSrcXMinCeil; ki < relSrcXMaxFloor; ki++) {
                    if (topYRatio > PIXEL_RATIO_EPSILON) {
                        final double ratio = topYRatio;
                        final int srcX = sx + ki;
                        final int srcY = sy + relSrcYMinCeil - 1;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addPixelContrib(srcColor32, ratio);
                    }
                    for (int kj = relSrcYMinCeil; kj < relSrcYMaxFloor; kj++) {
                        final int srcX = sx + ki;
                        final int srcY = sy + kj;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addFullPixelContrib(srcColor32);
                    }
                    if (bottomYRatio > PIXEL_RATIO_EPSILON) {
                        final double ratio = bottomYRatio;
                        final int srcX = sx + ki;
                        final int srcY = sy + relSrcYMaxFloor;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addPixelContrib(srcColor32, ratio);
                    }
                }
                if (rightXRatio > PIXEL_RATIO_EPSILON) {
                    if (topYRatio > PIXEL_RATIO_EPSILON) {
                        final double ratio = rightXRatio * topYRatio;
                        final int srcX = sx + relSrcXMaxFloor;
                        final int srcY = sy + relSrcYMinCeil - 1;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addPixelContrib(srcColor32, ratio);
                    }
                    {
                        final double ratio = rightXRatio;
                        for (int kj = relSrcYMinCeil; kj < relSrcYMaxFloor; kj++) {
                            final int srcX = sx + relSrcXMaxFloor;
                            final int srcY = sy + kj;
                            final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                            tmpArgbSum.addPixelContrib(srcColor32, ratio);
                        }
                    }
                    if (bottomYRatio > PIXEL_RATIO_EPSILON) {
                        final double ratio = rightXRatio * bottomYRatio;
                        final int srcX = sx + relSrcXMaxFloor;
                        final int srcY = sy + relSrcYMaxFloor;
                        final int srcColor32 = srcPixels.getColor32At(srcX, srcY);
                        tmpArgbSum.addPixelContrib(srcColor32, ratio);
                    }
                }
                rowArr[dic] = tmpArgbSum.toArgb32(dstPixelSurfInSrcInv);
            }
            
            final int rowOffset = 0;
            final int dstX = drc.x();
            final int dstY = drc.y() + djc;
            final int length = drc.xSpan();
            rowDrawer.drawRow(
                rowArr,
                rowOffset,
                dstX,
                dstY,
                length);
        }
    }
    
    /*
     * 
     */
    
    /**
     * Computes the row or column index in source,
     * for the given row or column index in destination.
     * 
     * If there is no scaling, si = di,
     * so don't bother to call this.
     * 
     * @param srcLength Number of pixels in src.
     * @param dstLengthInv Inverse of the number of pixels in dst.
     * @return The index, from row left or column top, of the src column or row.
     */
    private static int computeSrcIndex(
            int srcLength,
            double dstLengthInv,
            int di) {

        /*
         * +-0.5 to account for the fact that pixels centers
         * coordinates are 0.5 away from pixels edges.
         */
        final double ratio_0_1 = (di + 0.5) * dstLengthInv;
        final double sid = ratio_0_1 * srcLength - 0.5;
        final int si = BindingCoordsUtils.roundToInt(sid);

        return si;
    }

    private static int toArgb32FromInt8_noCheck(int alpha8, int red8, int green8, int blue8) {
        int i = alpha8;
        i <<= 8;
        i |= red8;
        i <<= 8;
        i |= green8;
        i <<= 8;
        i |= blue8;
        return i;
    }
}
