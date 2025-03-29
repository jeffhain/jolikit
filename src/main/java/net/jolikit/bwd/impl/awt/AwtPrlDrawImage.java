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

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

/**
 * Uses Graphics.drawImage(), possibly in parallel.
 * 
 * Just uses drawImage() with the specified images as input and output:
 * does not take care of eventual pre or post image copy
 * to avoid drawImage() accuracy and/or speed issues:
 * these have to be taken care of aside.
 */
public class AwtPrlDrawImage {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    public static final int DEFAULT_NEAREST_DST_AREA_THRESHOLD_FOR_SPLIT = 32 * 1024;
    
    public static final int DEFAULT_BILINEAR_DST_AREA_THRESHOLD_FOR_SPLIT = 2 * 1024;
    
    public static final int DEFAULT_BICUBIC_DST_AREA_THRESHOLD_FOR_SPLIT = 512;
    
    /**
     * Only contains RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR.
     */
    public static final Map<RenderingHints.Key, Object> HINTS_NEAREST =
        toInterpolationHintMap(
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    
    /**
     * Only contains RenderingHints.VALUE_INTERPOLATION_BILINEAR.
     */
    public static final Map<RenderingHints.Key, Object> HINTS_BILINEAR =
        toInterpolationHintMap(
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    
    /**
     * Only contains RenderingHints.VALUE_INTERPOLATION_BICUBIC.
     */
    public static final Map<RenderingHints.Key, Object> HINTS_BICUBIC =
        toInterpolationHintMap(
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Data common to splittables, to save memory.
     */
    private static class MyCmnData {
        final int dstAreaThresholdForSplit;
        final Map<RenderingHints.Key, Object> hints;
        final AffineTransform transform;
        final Composite composite;
        //
        final BufferedImage srcImage;
        final GRect srcRect;
        //
        final BufferedImage dstImage;
        final GRect dstRect;
        final GRect dstRectClipped;
        public MyCmnData(
            int dstAreaThresholdForSplit,
            Map<RenderingHints.Key, Object> hints,
            AffineTransform transform,
            Composite composite,
            //
            BufferedImage srcImage,
            GRect srcRect,
            //
            BufferedImage dstImage,
            GRect dstRect,
            GRect dstRectClipped) {
            this.dstAreaThresholdForSplit = dstAreaThresholdForSplit;
            this.hints = hints;
            this.transform = transform;
            this.composite = composite;
            //
            this.srcImage = srcImage;
            this.srcRect = srcRect;
            //
            this.dstImage = dstImage;
            this.dstRect = dstRect;
            this.dstRectClipped = dstRectClipped;
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
            final MyCmnData cmn = this.cmn;
            drawImageChunk(
                cmn.hints,
                cmn.transform,
                cmn.composite,
                //
                cmn.srcImage,
                cmn.srcRect,
                //
                cmn.dstImage,
                cmn.dstRect,
                cmn.dstRectClipped,
                //
                this.offset,
                this.length);
        }
        @Override
        public boolean worthToSplit() {
            return ScaledRectUtils.isWorthToSplit(
                this.cmn.dstAreaThresholdForSplit,
                this.cmn.dstRectClipped.xSpan(),
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
    
    private AwtPrlDrawImage() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Draws the specified source image on the specified destination image,
     * 
     * @param parallelizer Must not be null. If parallelism <= 1, not used.
     * @param dstAreaThresholdForSplit Must be >= 2.
     * @param hints Hints to be added to the graphics (and not set into,
     *        not to inadvertently wipe good defaults for other hints).
     * @param transform Can be null, in which case using identity.
     * @param composite Must not be null.
     */
    public static void drawImage(
        InterfaceParallelizer parallelizer,
        //
        int dstAreaThresholdForSplit,
        Map<RenderingHints.Key, Object> hints,
        AffineTransform transform,
        Composite composite,
        //
        BufferedImage srcImage,
        GRect srcRect,
        //
        BufferedImage dstImage,
        GRect dstRect,
        GRect dstClip) {
        
        NbrsUtils.requireSupOrEq(2, dstAreaThresholdForSplit, "dstAreaThresholdForSplit");
        if (hints.get(RenderingHints.KEY_INTERPOLATION) == null) {
            throw new IllegalArgumentException(
                "hints doesn't contain a value for KEY_INTERPOLATION");
        }
        LangUtils.requireNonNull(composite);
        
        final GRect srcImageRect = GRect.valueOf(
            0,
            0,
            srcImage.getWidth(),
            srcImage.getHeight());
        if (ScaledRectUtils.drawArgsCheckAndMustReturn(
            srcImageRect,
            srcRect,
            //
            dstRect,
            dstClip)) {
            return;
        }
        
        /*
         * 
         */
        
        final GRect dstRectClipped = dstRect.intersected(dstClip);
        
        boolean didGoPrl = false;
        
        final int offset = 0;
        final int length = dstRectClipped.ySpan();
        
        if ((parallelizer.getParallelism() >= 2)
            && ScaledRectUtils.isWorthToSplit(
                dstAreaThresholdForSplit,
                dstRectClipped.xSpan(),
                length)) {
            
            didGoPrl = true;
            
            final MyCmnData cmn = new MyCmnData(
                dstAreaThresholdForSplit,
                hints,
                transform,
                composite,
                //
                srcImage,
                srcRect,
                //
                dstImage,
                dstRect,
                dstRectClipped);
            
            final MySplittable splittable =
                new MySplittable(
                    cmn,
                    offset,
                    length);
            
            parallelizer.execute(splittable);
        }
        
        if (!didGoPrl) {
            drawImageChunk(
                hints,
                transform,
                composite,
                //
                srcImage,
                srcRect,
                //
                dstImage,
                dstRect,
                dstRectClipped,
                //
                offset,
                length);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void drawImageChunk(
        Map<RenderingHints.Key, Object> hints,
        AffineTransform transform,
        Composite composite,
        //
        BufferedImage srcImage,
        GRect srcRect,
        //
        BufferedImage dstImage,
        GRect dstRect,
        GRect dstRectClipped,
        //
        int offset,
        int length) {
        
        final Graphics2D g = dstImage.createGraphics();
        try {
            if (transform != null) {
                g.setTransform(transform);
            }
            
            if (!(dstRectClipped.equals(dstRect))
                || (length < dstRectClipped.ySpan())) {
                /*
                 * This clips covers user-defined clipping
                 * plus clipping for parallel chunks.
                 * 
                 * Must be done after eventual transform setting,
                 * since this is the transformed clip.
                 */
                g.setClip(
                    dstRectClipped.x(),
                    dstRectClipped.y() + offset,
                    dstRectClipped.xSpan(),
                    length);
            }
            
            g.addRenderingHints(hints);
            
            /*
             * NB: Only reliable if no more than two
             * of (srcX,srcY,dstX,dstY) are zero.
             */
            g.setComposite(composite);
            
            final int sx = srcRect.x();
            final int sy = srcRect.y();
            final int sw = srcRect.xSpan();
            final int sh = srcRect.ySpan();
            
            final int dx = dstRect.x();
            final int dy = dstRect.y();
            final int dw = dstRect.xSpan();
            final int dh = dstRect.ySpan();
            
            final ImageObserver observer = null;
            g.drawImage(
                srcImage,
                dx, // dx1
                dy, // dy1
                dx + dw, // dx2 (exclusive)
                dy + dh, // dy2 (exclusive)
                sx, // sx1
                sy, // sy1
                sx + sw, // sx2 (exclusive)
                sy + sh, // sy2 (exclusive)
                observer);
        } finally {
            g.dispose();
        }
    }
    
    private static Map<RenderingHints.Key, Object> toInterpolationHintMap(Object hint) {
        final Map<RenderingHints.Key, Object> map = new HashMap<>(1);
        map.put(RenderingHints.KEY_INTERPOLATION, hint);
        return Collections.unmodifiableMap(map);
    }
}
