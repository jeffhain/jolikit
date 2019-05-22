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
package net.jolikit.bwd.impl.swt;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

public class SwtBwdGraphics extends AbstractIntArrayBwdGraphics {
    
    /*
     * TODO swt We don't do all the drawing with SWT's GC methods,
     * for they don't allow for per-pixel alpha, only for a single
     * window-wide alpha.
     * Also, GC drawing methods don't allow to implement our flipColors(...)
     * method ("XOR mode" in practice in our bindings), cf.
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50228
     * Also: "Note that this mode in fundamentally unsupportable on certain
     * platforms, notably Carbon (Mac OS X). Clients that want their
     * code to run on all platforms need to avoid this method."
     * 
     * Instead, since SWT allows to read the pixels from GC, we just use GC
     * to figure out the pixels of texts, and the colors of images,
     * and do all the actual drawing and blending ourselves into an int array
     * based graphics (which allows for the "fundamentally unsupportable"
     * feature), which we afterwards flush into SWT.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyTextDataAccessor {
        final ImageData textImageData;
        final GRect maxTextRelativeRect;
        final int fgArgb32;
        public MyTextDataAccessor(
                ImageData textImageData,
                GRect maxTextRelativeRect,
                int fgArgb32) {
            this.textImageData = textImageData;
            this.maxTextRelativeRect = maxTextRelativeRect;
            this.fgArgb32 = fgArgb32;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Is also the device.
     */
    private final Display display;
    
    /**
     * Shared resources only disposed by root (non-child) graphics.
     */
    private final boolean isRootGraphics;

    /**
     * Created and disposed by root graphics,
     * and shared will all children graphics.
     */
    private final Color backingColor_0xFF000000;
    private final Color backingColor_0xFFFFFFFF;
    
    /*
     * temps
     */
    
    /**
     * Offscreen graphic buffer for drawing text.
     * 
     * Created and disposed by root graphics,
     * and shared with all children graphics,
     * so that we don't have too many of them at once,
     * since it's better not to (cf. the javadoc).
     * 
     * NB: Drawing must therefore be single-threaded.
     */
    private final SwtGraphicBuffer tmpGraphicBuffer;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for root graphics.
     * 
     * @param display Is also the device.
     * @param g Not disposed on close (is reused for child graphics,
     *        for we can't create multiple GC for a same Image).
     */
    public SwtBwdGraphics(
            InterfaceBwdBinding binding,
            Display display,
            GRect box,
            //
            int[] clientPixelArr,
            int clientPixelArrScanlineStride) {
        this(
                binding,
                display,
                box,
                box, // baseClip
                //
                clientPixelArr,
                clientPixelArrScanlineStride,
                //
                null); // parentGraphics
    }
    
    /*
     * 
     */

    @Override
    public SwtBwdGraphics newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".newChildGraphics(" + childBox + ")");
        }
        final GRect childBaseClip = this.getBaseClipInClient().intersected(childBox);
        
        final SwtBwdGraphics parentGraphics = this;
        return new SwtBwdGraphics(
                this.getBinding(),
                this.display,
                childBox,
                childBaseClip,
                //
                this.getClientPixelArr(),
                this.getClientPixelArrScanlineStride(),
                //
                parentGraphics);
    }

    /*
     * 
     */

    @Override
    public SwtBwdFont getFont() {
        return (SwtBwdFont) super.getFont();
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void finishImpl() {
        if (this.isRootGraphics) {
            this.backingColor_0xFF000000.dispose();
            this.backingColor_0xFFFFFFFF.dispose();
            
            this.tmpGraphicBuffer.dispose();
        }
    }

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        // Nothing to do, we retrieve it from BWD font when needed.
    }
    
    /*
     * 
     */

    @Override
    protected int getArrayColor32FromArgb32(int argb32) {
        return BindingColorUtils.toPremulAxyz32(argb32);
    }
    
    @Override
    protected int getArgb32FromArrayColor32(int premulArgb32) {
        return BindingColorUtils.toNonPremulAxyz32(premulArgb32);
    }

    @Override
    protected int toInvertedArrayColor32(int premulArgb32) {
        return BindingColorUtils.toInvertedPremulAxyz32_noCheck(premulArgb32);
    }
    
    @Override
    protected int getArrayColorAlpha8(int premulArgb32) {
        return Argb32.getAlpha8(premulArgb32);
    }

    @Override
    protected int blendArrayColor32(int srcPremulArgb32, int dstPremulArgb32) {
        return BindingColorUtils.blendPremulAxyz32_srcOver(srcPremulArgb32, dstPremulArgb32);
    }

    /*
     * Text.
     */

    @Override
    protected Object getTextDataAccessor(
            String text,
            GRect maxTextRelativeRect) {
        
        /*
         * To avoid unnecessary arrays creations, and because text often
         * occupies only a small portion of its bounding box, and because
         * we don't draw a same input text multiple times, we don't do
         * a complete ImageData-to-int[] conversion as we do for images,
         * and just process pixels to draw one by one from the temporary
         * ImageData that we use.
         */
        
        final int maxTextWidth = maxTextRelativeRect.xSpan();
        final int maxTextHeight = maxTextRelativeRect.ySpan();
        
        this.tmpGraphicBuffer.setSize(maxTextWidth, maxTextHeight);
        
        final MyTextDataAccessor accessor;

        // Drawing the text in the image.
        final GC textGc = this.tmpGraphicBuffer.createClippedGraphics();
        try {
            if (false) {
                // TODO swt Doesnt seem to work.
                textGc.setAntialias(SWT.OFF);
            }
            /*
             * TODO swt It doesn't seem possible to just clear the GC with
             * a transparent color, and then draw text on top of it: all pixels
             * always end up with 0xFF alpha (even with transparent = true),
             * and anti-aliased colors seem inconsistent (for example, ending up
             * with different {r,g,b}, when drawing with a black background
             * and a white foreground).
             * What we do:
             * - On the "tmp" GC, we draw white text on a black background.
             * - For each pixel, we convert the {r,g,b} components of the
             *   resulting color, into a floating-point into [0,1],
             *   and use it as a factor for alpha, to obtain the color to use
             *   from the user-specified color.
             */
            
            final SwtBwdFont fontImpl = this.getFont();
            textGc.setFont(fontImpl.getBackingFont());
            
            textGc.setBackground(this.backingColor_0xFF000000);
            textGc.setForeground(this.backingColor_0xFFFFFFFF);
            
            // Uses background color.
            textGc.fillRectangle(0, 0, maxTextWidth, maxTextHeight);
            
            /*
             * Here, "transparent" means "transparent background",
             * i.e. without filling the background.
             * NB: "transparent" in SWT seems to just mean "not drawn",
             * rather than "with non-opaque alpha",
             * cf. ImageData.transparentPixel.
             */
            final int textX = 0 - maxTextRelativeRect.x();
            final int textY;
            if (false) {
                /*
                 * TODO swt SWT's method spec says that (x,y)
                 * is the top left corner of the text to draw,
                 * but for some fonts (like Impact, or Interface)
                 * we have to remove descent to be properly located.
                 */
                textY = -fontImpl.fontMetrics().fontDescent() - maxTextRelativeRect.y();
            } else {
                textY = 0 - maxTextRelativeRect.y();
            }
            final boolean isTransparent = true;
            textGc.drawString(text, textX, textY, isTransparent);
            
            final Image textImage = this.tmpGraphicBuffer.getImage();
            // This is slow: must only do it once.
            final ImageData textImageData = textImage.getImageData();
            
            final int fgArgb32 = this.getArgb32();
            accessor = new MyTextDataAccessor(
                    textImageData,
                    maxTextRelativeRect,
                    fgArgb32);
        } finally {
            textGc.dispose();
        }

        return accessor;
    }
    
    @Override
    protected void disposeTextDataAccessor(Object textDataAccessor) {
        // Nothing to do, all the data is in tmp class field.
    }

    @Override
    protected GRect getRenderedTextRelativeRect(Object textDataAccessor) {
        final MyTextDataAccessor accessor = (MyTextDataAccessor) textDataAccessor;
        return accessor.maxTextRelativeRect;
    }

    @Override
    protected int getTextColor32(
            String text,
            Object textDataAccessor,
            int xInText,
            int yInText) {
        final MyTextDataAccessor accessor = (MyTextDataAccessor) textDataAccessor;
        final ImageData textImageData = accessor.textImageData;
        
        final int tmpArgb32 = SwtPaintUtils.getArgb32At(textImageData, xInText, yInText);
        final double tmpRedFp = Argb32.getRedFp(tmpArgb32);
        final double tmpGreenFp = Argb32.getGreenFp(tmpArgb32);
        final double tmpBlueFp = Argb32.getBlueFp(tmpArgb32);

        /*
         * TODO swt For small font sizes, with Lucida Console font,
         * taking min value makes things too light,
         * and taking max value makes things too thick.
         * We use mean value, which can sometimes be a bit more thick.
         * than what we get by drawing with SWT directly on the window GC.
         * Could use other formula, and could also not do anti-aliasing,
         * by just using either user-specified color or a fully transparent one,
         * depending on the value of alphaFactor relative to some threshold.
         */
        final double alphaFactor = (tmpRedFp + tmpGreenFp + tmpBlueFp) * (1.0/3);
        
        /*
         * TODO swt If we draw even for small alpha factors,
         * can cause quite thick glyphs when using a very opaque
         * color on a very transparent background, in particular
         * when using small size with some fonts
         * (such as FreeMono.otf - but not FreeMono.ttf).
         * Though, we don't want to risk not drawing glyphs parts,
         * so we use a threshold of 0.
         */
        if (alphaFactor <= 0.0) {
            // Not drawn upon.
            return 0x00000000;
        }
        
        final int fgArgb32 = accessor.fgArgb32;
        final double oldAlphaFp = Argb32.getAlphaFp(fgArgb32);
        final double newAlphaFp = oldAlphaFp * alphaFactor;
        final int newAlpha8 = BindingColorUtils.toInt8FromFp_noCheck(newAlphaFp);
        if (newAlpha8 == 0x00) {
            // Not drawn upon.
            return 0x00000000;
        }
        
        final int textArgb32 = BindingColorUtils.withAlpha8_noCheck(fgArgb32, newAlpha8);
        final int color32 = this.getArrayColor32FromArgb32(textArgb32);
        return color32;
    }

    /*
     * Images.
     */

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final SwtBwdImage imageImpl = (SwtBwdImage) image;
        final int[] color32Arr = imageImpl.getColor32Arr();
        return color32Arr;
    }

    @Override
    protected void disposeImageDataAccessor(Object imageDataAccessor) {
        // Nothing to do.
    }

    @Override
    protected int getImageColor32(
            InterfaceBwdImage image,
            Object imageDataAccessor,
            int xInImage,
            int yInImage) {
        final int[] color32Arr = (int[]) imageDataAccessor;
        final int index = yInImage * image.getWidth() + xInImage;
        final int color32 = color32Arr[index];
        return color32;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param parentGraphics Null for root graphics.
     */
    private SwtBwdGraphics(
            InterfaceBwdBinding binding,
            Display display,
            GRect box,
            GRect baseClip,
            //
            int[] clientPixelArr,
            int clientPixelArrScanlineStride,
            //
            SwtBwdGraphics parentGraphics) {
        super(
                binding,
                box,
                baseClip,
                //
                clientPixelArr,
                clientPixelArrScanlineStride);
        
        this.display = LangUtils.requireNonNull(display);
        
        if (parentGraphics == null) {
            this.isRootGraphics = true;
            
            final Device device = display;
            final int alpha8 = 0xFF;
            this.backingColor_0xFF000000 = new Color(device, 0x00, 0x00, 0x00, alpha8);
            this.backingColor_0xFFFFFFFF = new Color(device, 0xFF, 0xFF, 0xFF, alpha8);
            
            final boolean mustCopyOnImageResize = false;
            this.tmpGraphicBuffer = new SwtGraphicBuffer(display, mustCopyOnImageResize);
        } else {
            this.isRootGraphics = false;
            
            this.backingColor_0xFF000000 = parentGraphics.backingColor_0xFF000000;
            this.backingColor_0xFFFFFFFF = parentGraphics.backingColor_0xFFFFFFFF;
            
            this.tmpGraphicBuffer = parentGraphics.tmpGraphicBuffer;
        }
    }
}
