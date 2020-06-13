/*
 * Copyright 2019-2020 Jeff Hain
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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.ClientPainterNoRec;
import net.jolikit.lang.InterfaceFactory;

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

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AwtPaintUtils() {
    }
    
    /**
     * Paints the client into the offscreen buffer,
     * and then the offscreen buffer into the specified graphics.
     * 
     * NB: Does nothing if any client span is <= 0.
     */
    public void paintClientOnObThenG(
            AbstractAwtBwdBinding binding,
            Window window,
            ClientPainterNoRec clientPainterNoRec,
            AwtGraphicBuffer offscreenBuffer,
            InterfaceFactory<GRect> dirtyRectProvider,
            Container backingGContainer,
            Graphics2D backingG) {

        final Insets insets = backingGContainer.getInsets();
        final int clientXInWindow = insets.left;
        final int clientYInWindow = insets.top;
        final int width = backingGContainer.getWidth() - (insets.left + insets.right);
        final int height = backingGContainer.getHeight() - (insets.top + insets.bottom);
        if ((width <= 0) || (height <= 0)) {
            return;
        }

        final boolean isImageGraphics = false;
        final GRect box = GRect.valueOf(0, 0, width, height);

        final GRect dirtyRect = dirtyRectProvider.newInstance();
        
        /*
         * Painting client into offscreen buffer.
         */

        offscreenBuffer.setSize(width, height);
        final BufferedImage offscreenImage = offscreenBuffer.getImage();

        final AwtBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        final List<GRect> paintedRectList;
        if (bindingConfig.getMustUseIntArrayGraphicsForClients()) {
            final AwtBwdGraphicsWithIntArr g = new AwtBwdGraphicsWithIntArr(
                    binding,
                    isImageGraphics,
                    box,
                    //
                    offscreenImage);

            paintedRectList = clientPainterNoRec.paintClientAndClipRects(
                    g,
                    dirtyRect);
        } else {
            final AwtBwdGraphicsWithG g = new AwtBwdGraphicsWithG(
                    binding,
                    isImageGraphics,
                    box,
                    //
                    offscreenImage);

            paintedRectList = clientPainterNoRec.paintClientAndClipRects(
                    g,
                    dirtyRect);
        }
        
        if (paintedRectList.size() != 0) {
            if (AwtUtils.isShowingAndDeiconified(window)) {
                /*
                 * Painting offscreen buffer into the graphics.
                 */

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

                final Graphics2D properlyClippedBackingG = (Graphics2D) backingG.create();
                properlyClippedBackingG.setClip(
                        clientXInWindow,
                        clientYInWindow,
                        width,
                        height);
                try {
                    for (GRect paintedRect : paintedRectList) {
                        drawImageRectOnG(
                                offscreenImage,
                                paintedRect,
                                properlyClippedBackingG,
                                clientXInWindow,
                                clientYInWindow);
                    }
                } finally {
                    properlyClippedBackingG.dispose();
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void drawImageRectOnG(
            BufferedImage image,
            GRect rect,
            Graphics2D g,
            int clientXInWindow,
            int clientYInWindow) {

        final int w = rect.xSpan();
        final int h = rect.ySpan();
        // Javadoc doesn't tell it, but second corners
        // coordinates are exclusive.
        final int dx = clientXInWindow + rect.x();
        final int dy = clientYInWindow + rect.y();
        final int sx = rect.x();
        final int sy = rect.y();

        final Composite previousComposite = g.getComposite();
        try {
            if (MUST_SET_ALPHA_1_COMPOSITE) {
                AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f);
                g.setComposite(composite);
            }
            g.drawImage(
                    image,
                    dx, // dx1
                    dy, // dy1
                    dx + w, // dx2 (exclusive)
                    dy + h, // dy2 (exclusive)
                    sx, // sx1
                    sy, // sy1
                    sx + w, // sx2 (exclusive)
                    sy + h , // sy2 (exclusive)
                    null);
        } finally {
            if (MUST_SET_ALPHA_1_COMPOSITE) {
                g.setComposite(previousComposite);
            }
        }
    }
}
