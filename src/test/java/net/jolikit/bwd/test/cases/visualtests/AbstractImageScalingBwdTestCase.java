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
package net.jolikit.bwd.test.cases.visualtests;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.time.TimeUtils;

/**
 * To factor scaled drawing code.
 */
public abstract class AbstractImageScalingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
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
        BwdScalingType scalingType) {
        
        if (imgBox.isEmpty()) {
            return 0.0;
        }
        
        final int imgOffsetX = imgBox.x();
        final int imgOffsetY = imgBox.y();
        
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
        
        final double dtS;
        {
            gForScaling.setImageScalingType(scalingType);
            
            final long t1Ns = System.nanoTime();
            gForScaling.drawImage(imgBoxInGForScaling, img);
            final long t2Ns = System.nanoTime();
            
            dtS = TimeUtils.nsToS(t2Ns - t1Ns);
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
