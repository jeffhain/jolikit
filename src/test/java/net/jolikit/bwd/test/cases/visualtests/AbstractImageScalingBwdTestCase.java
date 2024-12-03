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
package net.jolikit.bwd.test.cases.visualtests;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.graphics.IntArrCopyRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.IntArrHolder;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcPixels;
import net.jolikit.bwd.impl.utils.graphics.InterfaceColorTypeHelper;
import net.jolikit.bwd.impl.utils.graphics.InterfaceScaledRectDrawer;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawerBicubic;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawerBilinear;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawerBoxsampledBicubic;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawerBoxsampledBilinear;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawerBoxsampled;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawerNearest;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.time.TimeUtils;

/**
 * To factor scaled drawing code.
 */
public abstract class AbstractImageScalingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * temps
     */
    
    /**
     * Need temporary array for srcPixels, because graphics
     * are not thread-safe, so can't read pixels from them
     * concurrently.
     */
    private final IntArrHolder tmpSrcArr = new IntArrHolder();
    
    /**
     * Need temporary array for dstRowDrawer, because graphics
     * are not thread-safe, so can't write pixels to them
     * concurrently.
     */
    private final IntArrHolder tmpDstArr = new IntArrHolder();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractImageScalingBwdTestCase() {
    }

    public AbstractImageScalingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return False by default.
     */
    protected boolean mustClipScaling() {
        return false;
    }
    
    /**
     * @return False by default.
     */
    protected boolean mustDrawScaledOnWritableImage() {
        return false;
    }
    
    protected double drawScaledAndGetDrawDurationS(
        InterfaceBwdImage img,
        InterfaceBwdGraphics g,
        GRect imgBox,
        InterfaceScaledRectDrawer drawer) {
        
        if (imgBox.isEmpty()) {
            return 0.0;
        }
        
        final int imgOffsetX = imgBox.x();
        final int imgOffsetY = imgBox.y();
        
        final GRect imgRect = img.getRect();
        
        /*
         * Clearing background with white,
         * in case of alpha image.
         */
        
        g.setColor(BwdColor.WHITE);
        g.clearRect(imgBox);
        
        /*
         * 
         */
        
        final boolean mustClipScaling = this.mustClipScaling();
        
        final boolean mustScaleOnWi = this.mustDrawScaledOnWritableImage();
        
        InterfaceBwdWritableImage wi = null;
        InterfaceBwdGraphics gForScaling;
        final GRect imgBoxInGForScaling;
        if (mustScaleOnWi) {
            wi = this.getBinding().newWritableImage(
                imgBox.xSpan(),
                imgBox.ySpan());
            gForScaling = wi.getGraphics();
            imgBoxInGForScaling = imgBox.withPosDeltas(
                -imgOffsetX,
                -imgOffsetY);
        } else {
            gForScaling = g;
            imgBoxInGForScaling = imgBox;
        }
        
        if (mustClipScaling) {
            final int spanDiv = 8;
            gForScaling.addClipInBase(
                imgBoxInGForScaling.withBordersDeltasElseEmpty(
                    imgBoxInGForScaling.xSpan() / spanDiv,
                    imgBoxInGForScaling.ySpan() / spanDiv,
                    -imgBoxInGForScaling.xSpan() / spanDiv,
                    -imgBoxInGForScaling.ySpan() / spanDiv));
        }
        
        final BwdScalingType scalingType;
        if (drawer instanceof ScaledRectDrawerNearest) {
            scalingType = BwdScalingType.NEAREST;
        } else if (drawer instanceof ScaledRectDrawerBoxsampled) {
            scalingType = BwdScalingType.BOXSAMPLED;
        } else if (drawer instanceof ScaledRectDrawerBilinear) {
            scalingType = BwdScalingType.BILINEAR;
        } else if (drawer instanceof ScaledRectDrawerBicubic) {
            scalingType = BwdScalingType.BICUBIC;
        } else if (drawer instanceof ScaledRectDrawerBoxsampledBilinear) {
            scalingType = BwdScalingType.BOXSAMPLED_BILINEAR;
        } else if (drawer instanceof ScaledRectDrawerBoxsampledBicubic) {
            scalingType = BwdScalingType.BOXSAMPLED_BICUBIC;
        } else {
            scalingType = null;
        }
        
        final double dtS;
        if (scalingType != null) {
            gForScaling.setImageScalingType(scalingType);
            final long t1Ns = System.nanoTime();
            gForScaling.drawImage(imgBoxInGForScaling, img);
            final long t2Ns = System.nanoTime();
            dtS = TimeUtils.nsToS(t2Ns - t1Ns);
        } else {
            /*
             * Alien scaling type: will run the algo
             * on int arrays of pixels. 
             */
            final GRect srcRect = imgRect;
            final GRect dstRect = imgBoxInGForScaling;
            
            // Our graphics and scalings use premul,
            // so doing the same for AWT scaling to be fair.
            final InterfaceColorTypeHelper colorTypeHelper =
                PremulArgbHelper.getInstance();
            
            final IntArrSrcPixels srcPixels = new IntArrSrcPixels();
            {
                final int[] srcPixelsArr = this.tmpSrcArr.getArr(imgRect.area());
                final int sw = imgRect.xSpan();
                final int sh = imgRect.ySpan();
                for (int y = 0; y < sh; y++) {
                    for (int x = 0; x < sw; x++) {
                        final int srcNonPremulArgb32 = img.getArgb32At(x, y);
                        final int srcColor32 =
                            colorTypeHelper.asTypeFromNonPremul32(
                                srcNonPremulArgb32);
                        srcPixelsArr[y * sw + x] = srcColor32;
                    }
                }
                srcPixels.configure(imgRect, srcPixelsArr, sw);
            }
            final GRect dstClip = dstRect;
            final GRect dstRectClipped = dstRect.intersected(dstClip);
            final int dw = dstRectClipped.xSpan();
            final int dh = dstRectClipped.ySpan();
            //
            // Don't need to zeroize,
            // because using IntArrCopyRowDrawer.
            final int[] dstArr = this.tmpDstArr.getArr(dstRectClipped.area());
            final IntArrCopyRowDrawer dstRowDrawer = new IntArrCopyRowDrawer();
            // We want (dstRect.x(),dstRect.y())
            // to be itDstColor32Arr[0].
            final GTransform transformArrToDst =
                GTransform.valueOf(
                    GRotation.ROT_0,
                    -dstRect.x(),
                    -dstRect.y());
            dstRowDrawer.configure(
                transformArrToDst,
                dstArr,
                dw);
            //
            final long t1Ns = System.nanoTime();
            drawer.drawScaledRect(
                getBinding().getParallelizer(),
                colorTypeHelper,
                srcPixels,
                srcRect,
                dstRect,
                dstClip,
                dstRowDrawer);
            final long t2Ns = System.nanoTime();
            dtS = TimeUtils.nsToS(t2Ns - t1Ns);
            //
            for (int j = 0; j < dh; j++) {
                final int y = dstRect.y() + j;
                for (int i = 0; i < dw; i++) {
                    final int x = dstRect.x() + i;
                    final int dstColor32 = dstArr[j * dw + i];
                    final int dstNonPremulArgb32 =
                        colorTypeHelper.asNonPremul32FromType(dstColor32);
                    gForScaling.setArgb32(dstNonPremulArgb32);
                    gForScaling.drawPoint(x, y);
                }
            }
        }
        
        if (mustScaleOnWi) {
            g.drawImage(imgOffsetX, imgOffsetY, wi);
            
            wi.dispose();
            // No longer usable.
            wi = null;
            gForScaling = null;
        } else {
            if (mustClipScaling) {
                g.removeLastAddedClip();
            }
        }
        
        return dtS;
    }
}
