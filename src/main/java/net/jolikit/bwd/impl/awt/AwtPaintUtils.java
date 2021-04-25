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
package net.jolikit.bwd.impl.awt;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.List;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;

public class AwtPaintUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * No need, because default composite is SRC_OVER,
     * and our client background is always opaque.
     */
    private static final boolean MUST_SET_ALPHA_1_COMPOSITE = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * TODO awt TODO mac On Mac, at some point had trouble when using
     * buffered images of type TYPE_INT_ARGB: painting did visibly show up
     * only after some delay, or if we used a lot of drawXxx() primitives
     * on window graphics (like when painting pixels one by one)
     * (this workaround seemed to only work if client area height was above
     * about 210), or did never show up in case of window painting frenzy
     * such as in benches.
     * For some time our workaround was to use TYPE_INT_RGB instead on Mac,
     * which for some reason still allowed to do semi-transparent painting,
     * although it could make things much slower (most likely because of
     * conversions needed between "ARGB" and "RGB_").
     * Another workaround was to do all of these:
     * - have repaint() call super.repaint()
     * - have paint() call paintComponentNowOnG()
     *   and/or have paintComponents() call paintComponentNowOnG()
     * - have paintClientNowOrLater() call window.repaint()
     * Lately though, we didn't encounter this issue anymore,
     * and now we always use TYPE_INT_ARGB_PRE, which is required
     * by our AwtBwdGraphicsWithIntArr implementation.
     * 
     * NB: Default filling is white if using BufferedImage.TYPE_INT_ARGB(_PRE),
     * and black if using BufferedImage.TYPE_INT_RGB.
     */
    public static final int BUFFERED_IMAGE_TYPE_FOR_CLIENT_G_DRAWING =
        BufferedImage.TYPE_INT_ARGB_PRE;
    
    public static final int BUFFERED_IMAGE_TYPE_FOR_OFFSCREEN =
        BufferedImage.TYPE_INT_ARGB_PRE;
    
    /*
     * 
     */
    
    private BufferedImage currentPainting_offscreenImageInBd;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AwtPaintUtils() {
    }
    
    public boolean canPaintClientNowProxyImpl(Window window) {
        return AwtUtils.isShowingAndDeiconified(window);
    }
    
    public InterfaceBwdGraphics newRootGraphicsImpl(
        InterfaceBwdBindingImpl binding,
        AwtBwdBindingConfig bindingConfig,
        AwtGraphicBuffer offscreenBuffer,
        //
        GRect boxWithBorder) {
        
        final boolean isImageGraphics = false;
        
        offscreenBuffer.setSize(
            boxWithBorder.xSpan(),
            boxWithBorder.ySpan());
        final BufferedImage offscreenImageInBd = offscreenBuffer.getImage();
        this.currentPainting_offscreenImageInBd = offscreenImageInBd;
        
        final InterfaceBwdGraphics gForBorder;
        if (bindingConfig.getMustUseIntArrayGraphicsForClients()) {
            gForBorder =
                new AwtBwdGraphicsWithIntArr(
                    binding,
                    boxWithBorder,
                    //
                    isImageGraphics,
                    offscreenImageInBd);
        } else {
            gForBorder =
                new AwtBwdGraphicsWithG(
                    binding,
                    boxWithBorder,
                    //
                    isImageGraphics,
                    offscreenImageInBd);
        }
        return gForBorder;
    }
    
    public void paintBackingClientImpl(
        ScaleHelper scaleHelper,
        GPoint clientSpansInOs,
        GPoint bufferPosInCliInOs,
        List<GRect> paintedRectList,
        //
        Container backingGContainer,
        Graphics2D backingG) {
        
        /*
         * TODO awt On size growth, sometimes AWT clip only allows
         * for painting over the new area, as if things over old area
         * never had to be repaint or moved accordingly.
         * Also, sometimes it allows painting to leak outside,
         * for example with JFrames.
         * As a result, we just ignore AWT graphics clip, and use
         * our own, corresponding to the whole client area.
         * Further clipping refinement is done anyway at paint time,
         * according to BWD graphics clip.
         */
        
        final Insets insets = backingGContainer.getInsets();
        final int clientXInBackingGInOs = insets.left;
        final int clientYInBackingGInOs = insets.top;
        final int bufferXInBackingGInOs =
            clientXInBackingGInOs + bufferPosInCliInOs.x();
        final int bufferYInBackingGInOs =
            clientYInBackingGInOs + bufferPosInCliInOs.y();

        final Graphics2D properlyClippedBackingG = (Graphics2D) backingG.create();
        properlyClippedBackingG.setClip(
            clientXInBackingGInOs,
            clientYInBackingGInOs,
            clientSpansInOs.x(),
            clientSpansInOs.y());
        try {
            for (GRect paintedRectInBd : paintedRectList) {
                drawImageRectOnG(
                    this.currentPainting_offscreenImageInBd,
                    paintedRectInBd,
                    properlyClippedBackingG,
                    bufferXInBackingGInOs,
                    bufferYInBackingGInOs,
                    scaleHelper);
            }
        } finally {
            properlyClippedBackingG.dispose();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void drawImageRectOnG(
        BufferedImage imageInBd,
        GRect rectInBd,
        Graphics2D g,
        int imageXInBackingGInOs,
        int imageYInBackingGInOs,
        ScaleHelper scaleHelper) {
        
        final int sxInBd = rectInBd.x();
        final int syInBd = rectInBd.y();
        final int wInBd = rectInBd.xSpan();
        final int hInBd = rectInBd.ySpan();
        
        final int dxInOs = imageXInBackingGInOs + scaleHelper.spanBdToOs(sxInBd);
        final int dyInOs = imageYInBackingGInOs + scaleHelper.spanBdToOs(syInBd);
        final int wInOs = scaleHelper.spanBdToOs(wInBd);
        final int hInOs = scaleHelper.spanBdToOs(hInBd);
        
        final Composite previousComposite = g.getComposite();
        try {
            if (MUST_SET_ALPHA_1_COMPOSITE) {
                AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f);
                g.setComposite(composite);
            }
            /*
             * Eventual scaling only due to pixel scaling,
             * so always exact: no need for
             * accurate scaling algorithm.
             */
            g.drawImage(
                imageInBd,
                dxInOs, // dx1
                dyInOs, // dy1
                dxInOs + wInOs, // dx2 (exclusive)
                dyInOs + hInOs, // dy2 (exclusive)
                sxInBd, // sx1
                syInBd, // sy1
                sxInBd + wInBd, // sx2 (exclusive)
                syInBd + hInBd , // sy2 (exclusive)
                null);
        } finally {
            if (MUST_SET_ALPHA_1_COMPOSITE) {
                g.setComposite(previousComposite);
            }
        }
    }
}
