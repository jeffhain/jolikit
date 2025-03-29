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
package net.jolikit.bwd.impl.awt;

import net.jolikit.bwd.impl.utils.graphics.ScaledRectUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

/**
 * Uses BufferedImageHelper.copyImage(), possibly in parallel.
 */
public class AwtPrlCopyImage {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int AREA_THRESHOLD_FOR_SPLIT = 32 * 1024;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Data common to splittables, to save memory.
     */
    private static class MyCmnData {
        final BufferedImageHelper srcHelper;
        final int srcX;
        final int srcY;
        final BufferedImageHelper dstHelper;
        final int dstX;
        final int dstY;
        final int width;
        public MyCmnData(
            BufferedImageHelper srcHelper,
            int srcX,
            int srcY,
            BufferedImageHelper dstHelper,
            int dstX,
            int dstY,
            int width) {
            this.srcHelper = srcHelper;
            this.srcX = srcX;
            this.srcY = srcY;
            this.dstHelper = dstHelper;
            this.dstX = dstX;
            this.dstY = dstY;
            this.width = width;
        }
    }
    
    private static class MySplittable implements InterfaceSplittable {
        final MyCmnData cmn;
        int offset;
        int length;
        public MySplittable(
            MyCmnData cmn,
            int offset,
            int length) {
            this.cmn = cmn;
            this.offset = offset;
            this.length = length;
        }
        @Override
        public String toString() {
            return "[" + this.offset + "," + this.length + "]";
        }
        @Override
        public void run() {
            BufferedImageHelper.copyImage(
                this.cmn.srcHelper.duplicate(),
                this.cmn.srcX,
                this.cmn.srcY + this.offset,
                this.cmn.dstHelper.duplicate(),
                this.cmn.dstX,
                this.cmn.dstY + this.offset,
                this.cmn.width,
                this.length);
        }
        @Override
        public boolean worthToSplit() {
            return ScaledRectUtils.isWorthToSplit(
                AREA_THRESHOLD_FOR_SPLIT,
                this.cmn.width,
                this.length);
        }
        @Override
        public InterfaceSplittable split() {
            final int halfish = (this.length >> 1);
            final MySplittable ret = new MySplittable(
                this.cmn,
                this.offset + halfish,
                this.length - halfish);
            this.length = halfish;
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private AwtPrlCopyImage() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param parallelizer Must not be null. If parallelism <= 1, not used.
     */
    public static void copyImage(
        InterfaceParallelizer parallelizer,
        //
        BufferedImageHelper srcHelper,
        int srcX,
        int srcY,
        //
        BufferedImageHelper dstHelper,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        boolean didGoPrl = false;
        
        if ((parallelizer.getParallelism() >= 2)
            && ScaledRectUtils.isWorthToSplit(
                AREA_THRESHOLD_FOR_SPLIT,
                width,
                height)) {
            
            didGoPrl = true;
            
            final MyCmnData cmn = new MyCmnData(
                srcHelper,
                srcX,
                srcY,
                //
                dstHelper,
                dstX,
                dstY,
                //
                width);
            
            final MySplittable splittable =
                new MySplittable(cmn, 0, height);
            
            parallelizer.execute(splittable);
        }
        
        if (!didGoPrl) {
            BufferedImageHelper.copyImage(
                srcHelper,
                srcX,
                srcY,
                //
                dstHelper,
                dstX,
                dstY,
                //
                width,
                height);
        }
    }
}
