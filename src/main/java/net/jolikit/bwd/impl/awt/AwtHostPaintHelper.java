/*
 * Copyright 2019-2025 Jeff Hain
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

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.List;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;

public class AwtHostPaintHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private BufferedImage currentPainting_offscreenImageInBd;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public AwtHostPaintHelper() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public InterfaceBwdGraphics newRootGraphicsImpl(
        InterfaceBwdBindingImpl binding,
        AwtGraphicBuffer offscreenBuffer,
        //
        GRect boxWithBorder) {
        
        final boolean isImageGraphics = false;
        
        offscreenBuffer.setSize(
            boxWithBorder.xSpan(),
            boxWithBorder.ySpan());
        final BufferedImage offscreenImageInBd = offscreenBuffer.getImage();
        this.currentPainting_offscreenImageInBd = offscreenImageInBd;
        
        final BufferedImageHelper offscreenHelperInBd =
            new BufferedImageHelper(offscreenImageInBd);
        return new AwtBwdGraphics(
            binding,
            boxWithBorder,
            //
            isImageGraphics,
            offscreenHelperInBd);
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
        
        /*
         * No need to set AlphaComposite.Src, because default composite is SrcOver,
         * and our client background is always opaque.
         * 
         * Eventual scaling only due to pixel scaling, so always exact:
         * default NEAREST is fine.
         */
        
        final ImageObserver observer = null;
        g.drawImage(
            imageInBd,
            //
            dxInOs, // dx1
            dyInOs, // dy1
            dxInOs + wInOs, // dx2 (exclusive)
            dyInOs + hInOs, // dy2 (exclusive)
            //
            sxInBd, // sx1
            syInBd, // sy1
            sxInBd + wInBd, // sx2 (exclusive)
            syInBd + hInBd , // sy2 (exclusive)
            //
            observer);
    }
}
