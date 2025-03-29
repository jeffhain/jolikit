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
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

public abstract class AbstractParallelSrd implements InterfaceScaledRectDrawer {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Data common to splittables, to save memory.
     */
    private class MyCmnData {
        final InterfaceColorTypeHelper colorTypeHelper;
        final InterfaceSrcPixels srcPixels;
        final GRect srcRect;
        final GRect dstRect;
        final GRect dstRectClipped;
        final InterfaceRowDrawer dstRowDrawer;
        final double srcAreaOverDstArea;
        public MyCmnData(
            InterfaceColorTypeHelper colorTypeHelper,
            InterfaceSrcPixels srcPixels,
            GRect srcRect,
            GRect dstRect,
            GRect dstRectClipped,
            InterfaceRowDrawer dstRowDrawer,
            double srcAreaOverDstArea) {
            this.colorTypeHelper = colorTypeHelper;
            this.srcPixels = srcPixels;
            this.srcRect = srcRect;
            this.dstRect = dstRect;
            this.dstRectClipped = dstRectClipped;
            this.dstRowDrawer = dstRowDrawer;
            this.srcAreaOverDstArea = srcAreaOverDstArea;
        }
    }
            
    private class MySplittable implements InterfaceSplittable {
        final MyCmnData cmn;
        private int dstYStart;
        private int dstYEnd;
        public MySplittable(
            MyCmnData cmn,
            int dstYStart,
            int dstYEnd) {
            if (dstYStart > dstYEnd) {
                throw new IllegalArgumentException(dstYStart + " > " + dstYEnd);
            }
            this.cmn = cmn;
            this.dstYStart = dstYStart;
            this.dstYEnd = dstYEnd;
        }
        @Override
        public String toString() {
            return "[" + this.dstYStart + "," + this.dstYEnd + "]";
        }
        @Override
        public void run() {
            drawScaledRectChunk(
                this.cmn.colorTypeHelper,
                this.cmn.srcPixels,
                this.cmn.srcRect,
                this.cmn.dstRect,
                this.cmn.dstRectClipped,
                this.dstYStart,
                this.dstYEnd,
                this.cmn.dstRowDrawer);
        }
        @Override
        public boolean worthToSplit() {
            final int currentHeight =
                (this.dstYEnd - this.dstYStart + 1);
            return ScaledRectUtils.isWorthToSplit(
                getSrcAreaThresholdForSplit(),
                getDstAreaThresholdForSplit(),
                this.cmn.srcAreaOverDstArea,
                this.cmn.dstRectClipped.xSpan(),
                currentHeight);
        }
        @Override
        public InterfaceSplittable split() {
            final int dstYMid =
                this.dstYStart
                + ((this.dstYEnd - this.dstYStart) >> 1);
            final MySplittable ret = new MySplittable(
                this.cmn,
                dstYMid + 1,
                this.dstYEnd);
            this.dstYEnd = dstYMid;
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    protected AbstractParallelSrd() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
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
        
        final GRect dstRectClipped = dstRect.intersected(dstClip);
        
        final int dstYStart = dstRectClipped.y();
        final int dstYEnd = dstRectClipped.yMax();
        
        boolean didGoPrl = false;
        
        if (parallelizer.getParallelism() >= 2) {
            final double srcAreaOverDstArea =
                srcRect.areaLong() / (double) dstRect.areaLong();
            if (ScaledRectUtils.isWorthToSplit(
                this.getSrcAreaThresholdForSplit(),
                this.getDstAreaThresholdForSplit(),
                srcAreaOverDstArea,
                dstRectClipped.xSpan(),
                dstRectClipped.ySpan())) {
                
                didGoPrl = true;
                
                final MyCmnData cmn =
                    new MyCmnData(
                        colorTypeHelper,
                        srcPixels,
                        srcRect,
                        dstRect,
                        dstRectClipped,
                        dstRowDrawer,
                        srcAreaOverDstArea);
                
                final MySplittable splittable =
                    new MySplittable(
                        cmn,
                        dstYStart,
                        dstYEnd);
                
                parallelizer.execute(splittable);
            }
        }
        
        if (!didGoPrl) {
            this.drawScaledRectChunk(
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
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Returning Integer.MAX_VALUE prevents splitting/parallelism
     * due to source rectangle.
     * 
     * @return The area of clipped source rectangle,
     *         from which it's worth to split in two for parallelization.
     */
    protected abstract int getSrcAreaThresholdForSplit();

    /**
     * Returning Integer.MAX_VALUE prevents splitting/parallelism
     * due to destination rectangle.
     * 
     * @return The area of clipped destination rectangle,
     *         from which it's worth to split in two for parallelization.
     */
    protected abstract int getDstAreaThresholdForSplit();
    
    /**
     * Specifying dstRectClipped and not dstClip,
     * because it typically has been computed before this call,
     * and only binding code is supposed to call this method
     * so no risk of user giving dstClip instead.
     * 
     * dstYStart and dstYEnd are useful to split dstRectClipped
     * into multiple sub-areas processed in parallel.
     * 
     * Must work whether colors are alpha-premultiplied or not,
     * i.e. if input is alpha-premultiplied (and valid), each
     * output color's alpha must never be greater than any RGB component
     * (which in practice typically means that the same transform must be
     * applied to each component).
     * 
     * @param colorTypeHelper Helper for source (and destination) color type.
     * @param srcPixels The source pixels. Never empty.
     * @param srcRect Source rectangle. Never empty.
     * @param dstRect Destination rectangle. Never empty.
     * @param dstRectClipped The clipped destination rectangle. Never empty.
     * @param dstYStart Destination "y" to start from (inclusive).
     * @param dstYEnd Destination "y" to end at (inclusive).
     * @param dstRowDrawer Drawer where to draw destination pixels,
     *        in the same color format as input.
     */
    protected abstract void drawScaledRectChunk(
        InterfaceColorTypeHelper colorTypeHelper,
        //
        InterfaceSrcPixels srcPixels,
        GRect srcRect,
        //
        GRect dstRect,
        GRect dstRectClipped,
        int dstYStart,
        int dstYEnd,
        InterfaceRowDrawer dstRowDrawer);
}
