/*
 * Copyright 2019 Jeff Hain
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

import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;

/**
 * Designed for drawing of client area (dirty) parts into
 * some backing library's device pixel sized buffer.
 * 
 * Simple quick algorithm, without anti aliasing (which would cause a dependency
 * to some color model).
 */
public class ScaledIntRectDrawingUtils {

    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------

    /**
     * The interface to read pixels from.
     */
    public interface InterfaceSrcPixels {
        public int getWidth();
        public int getHeight();
        /**
         * Can be null, in which case getColor32At(...) can be used instead.
         */
        public int[] color32Arr();
        /**
         * Must use this, and not width, when computing indexes.
         */
        public int getScanlineStride();
        /**
         * Useful if color32Arr() returns null.
         * 
         * We prefer specifying (x,y) than just index, because computing
         * index from (x,y) is fast, but not (x,y) from index, due to modulo.
         */
        public int getColor32At(int x, int y);
    }
    
    /**
     * A default implementation backed by an int array.
     */
    public static class IntArrSrcPixels implements InterfaceSrcPixels {
        private final int width;
        private final int height;
        private final int[] color32Arr;
        private final int scanlineStride;
        public IntArrSrcPixels(
                int width,
                int height,
                int[] color32Arr,
                int scanlineStride) {
            this.width = width;
            this.height = height;
            this.color32Arr = color32Arr;
            this.scanlineStride = scanlineStride;
        }
        @Override
        public int getWidth() {
            return this.width;
        }
        @Override
        public int getHeight() {
            return this.height;
        }
        @Override
        public int[] color32Arr() {
            return this.color32Arr;
        }
        @Override
        public int getScanlineStride() {
            return this.scanlineStride;
        }
        @Override
        public int getColor32At(int x, int y) {
            final int index = y * this.scanlineStride + x;
            return this.color32Arr[index];
        }
    }
    
    /**
     * The interface to draw pixels into.
     */
    public interface InterfaceScaledRowPartDrawer {
        /**
         * To draw a part of an src row, scaled such as
         * it has as many pixels as it must cover on destination.
         */
        public void drawScaledRowPart(
                int[] scaledSrcRowArr,
                int scaledSrcRowOffset,
                //
                int partDstX,
                int partDstY,
                //
                int partLength);
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final IntArrHolder tmpArr1 = new IntArrHolder();
    private final IntArrHolder tmpArr2 = new IntArrHolder();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ScaledIntRectDrawingUtils() {
    }

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
    public static int computeSi(
            int srcLength,
            double dstLengthInv,
            int di) {

        final double ratio_0_1 = (di + 0.5) * dstLengthInv;
        final double sid = ratio_0_1 * srcLength - 0.5;
        final int si = BindingCoordsUtils.roundToInt(sid);

        return si;
    }
    
    /**
     * Draws the specified source rectangle of the specified pixels,
     * into the specified destination rectangle.
     */
    public void drawRectScaled(
            InterfaceSrcPixels srcPixels,
            int sx, int sy, int sxSpan, int sySpan,
            //
            int dx, int dy, int dxSpan, int dySpan,
            //
            InterfaceScaledRowPartDrawer scaledRowPartDrawer) {
        if ((sxSpan <= 0)
                || (sySpan <= 0)
                || (dxSpan <= 0)
                || (dySpan <= 0)) {
            return;
        }
        
        // Possibly null.
        final int[] srcColor32Arr = srcPixels.color32Arr();
        final int srcScanlineStride = srcPixels.getScanlineStride();

        /*
         * Scaling as done for images in int array graphics.
         */

        final double dxi_to_0_1_factor = 1.0 / dxSpan;
        final double dyi_to_0_1_factor = 1.0 / dySpan;

        final int dstXMin = dx;
        final int dstXMax = dx + dxSpan - 1;
        if (dstXMax < dstXMin) {
            throw new ArithmeticException("overflow: dx = " + dx + ", dxSpan = " + dxSpan);
        }
        final int dstYMin = dy;
        final int dstYMax = dy + dySpan - 1;
        if (dstYMax < dstYMin) {
            throw new ArithmeticException("overflow: dy = " + dy + ", dySpan = " + dySpan);
        }

        final int srcRowLength = sxSpan;
        final int srcColLength = sySpan;
        final int dstRowLength = dxSpan;
        final int dstColLength = dySpan;

        // Optimization, to avoid useless scalings.
        final boolean gotXScaling = (dxSpan != sxSpan);
        final boolean gotYScaling = (dySpan != sySpan);

        final int[] scaledSrcRowArr;
        // Optimization, to avoid computing columns scaling for each row.
        final int[] sxiByDxiArr;
        if (!gotXScaling) {
            // Src is already row-scaled.
            if (srcColor32Arr != null) {
                scaledSrcRowArr = srcColor32Arr;
            } else {
                scaledSrcRowArr = this.tmpArr1.getArr(dstRowLength);
            }

            sxiByDxiArr = null;
        } else {
            scaledSrcRowArr = this.tmpArr1.getArr(dstRowLength);

            sxiByDxiArr = this.tmpArr2.getArr(dstRowLength);
            for (int dxi = 0; dxi < dstRowLength; dxi++) {
                final int sxi = computeSi(
                        srcRowLength,
                        dxi_to_0_1_factor,
                        dxi);
                sxiByDxiArr[dxi] = sxi;
            }
        }

        int lastSyi = -1;
        for (int dyi = 0; dyi < dstColLength; dyi++) {
            final int syi;
            if (!gotYScaling) {
                syi = dyi;
            } else {
                syi = computeSi(
                        srcColLength,
                        dyi_to_0_1_factor,
                        dyi);
            }

            final int scaledSrcRowOffset;
            if (!gotXScaling && (srcColor32Arr != null)) {
                // Using input pixel array directly.
                scaledSrcRowOffset = (sy + syi) * srcScanlineStride + sx;
            } else {
                scaledSrcRowOffset = 0;
                final boolean movedToNewSrcRow = (syi != lastSyi);
                if (movedToNewSrcRow) {
                    final int srcY = sy + syi;
                    for (int dxi = 0; dxi < dstRowLength; dxi++) {
                        final int sxi = ((sxiByDxiArr != null) ? sxiByDxiArr[dxi] : dxi);
                        final int srcX = sx + sxi;
                        // Not bothering to optimize in case we got the src array,
                        // this method should be fast enough, and not do useless checks.
                        final int color32 = srcPixels.getColor32At(srcX, srcY);
                        scaledSrcRowArr[dxi] = color32;
                    }
                }
            }

            final int partDstX = dx;
            final int partDstY = dy + dyi;
            final int partLength = dstRowLength;
            scaledRowPartDrawer.drawScaledRowPart(
                    scaledSrcRowArr, scaledSrcRowOffset,
                    partDstX, partDstY, partLength);

            lastSyi = syi;
        }
    }
}
