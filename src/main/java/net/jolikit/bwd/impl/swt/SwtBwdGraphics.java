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
package net.jolikit.bwd.impl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

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
    
    /**
     * Drawn text spans can change a lot during painting,
     * so shrinking could cause much GC.
     * These storages are tied to root graphics,
     * and will be garbaged once we are done with it.
     */
    private static final boolean ALLOW_TEXT_STORAGE_SHRINKING = false;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Clipped Text Data Accessor.
     */
    private static class MyCtda {
        final ImageData textImageData;
        final GRect rectInText;
        final int fgArgb32;
        public MyCtda(
                ImageData textImageData,
                GRect rectInText,
                int fgArgb32) {
            this.textImageData = textImageData;
            this.rectInText = rectInText;
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
    private final Color backingColor_BLACK;
    private final Color backingColor_WHITE;
    
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
    private final SwtGraphicBuffer textGraphicBuffer;

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
            int[] pixelArr,
            int pixelArrScanlineStride) {
        this(
                binding,
                display,
                box,
                box, // initialClip
                //
                pixelArr,
                pixelArrScanlineStride,
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
        final GRect childInitialClip = this.getInitialClipInBase().intersected(childBox);
        
        final SwtBwdGraphics parentGraphics = this;
        return new SwtBwdGraphics(
                this.getBinding(),
                this.display,
                childBox,
                childInitialClip,
                //
                this.getPixelArr(),
                this.getPixelArrScanlineStride(),
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
            this.backingColor_BLACK.dispose();
            this.backingColor_WHITE.dispose();
            
            this.textGraphicBuffer.dispose();
        }
    }
    
    /*
     * 
     */

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        // Nothing to do, we retrieve it from BWD font when needed.
    }
    
    @Override
    protected void setBackingState(
        boolean mustSetClip,
        GRect clipInBase,
        //
        boolean mustSetTransform,
        GTransform transform,
        //
        boolean mustSetColor,
        int argb32,
        BwdColor colorElseNull,
        //
        boolean mustSetFont,
        InterfaceBwdFont font) {
        
        this.setBackingStateDefaultImpl(
                mustSetClip,
                clipInBase,
                //
                mustSetTransform,
                transform,
                //
                mustSetColor,
                argb32,
                colorElseNull,
                //
                mustSetFont,
                font);
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
    protected Object getClippedTextDataAccessor(
            String text,
            GRect maxClippedTextRectInText) {
        
        /*
         * To avoid unnecessary arrays creations, and because text often
         * occupies only a small portion of its bounding box, and because
         * we don't draw a same input text multiple times, we don't do
         * a complete ImageData-to-int[] conversion as we do for images,
         * and just process pixels to draw one by one from the temporary
         * ImageData that we use.
         */
        
        final int mcTextWidth = maxClippedTextRectInText.xSpan();
        final int mcTextHeight = maxClippedTextRectInText.ySpan();
        
        this.textGraphicBuffer.setSize(mcTextWidth, mcTextHeight);
        
        final MyCtda accessor;

        // Drawing the text in the image.
        final GC textGc = this.textGraphicBuffer.createClippedGraphics();
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
            
            textGc.setBackground(this.backingColor_BLACK);
            textGc.setForeground(this.backingColor_WHITE);
            
            // Uses background color.
            textGc.fillRectangle(0, 0, mcTextWidth, mcTextHeight);
            
            /*
             * Here, "transparent" means "transparent background",
             * i.e. without filling the background.
             * NB: "transparent" in SWT seems to just mean "not drawn",
             * rather than "with non-opaque alpha",
             * cf. ImageData.transparentPixel.
             */
            final int textX = 0 - maxClippedTextRectInText.x();
            final int textY;
            if (false) {
                /*
                 * TODO swt SWT's method spec says that (x,y)
                 * is the top left corner of the text to draw,
                 * but for some fonts (like Impact, or Interface)
                 * we have to remove descent to be properly located.
                 */
                textY = -fontImpl.fontMetrics().fontDescent() - maxClippedTextRectInText.y();
            } else {
                textY = 0 - maxClippedTextRectInText.y();
            }
            final boolean isTransparent = true;
            textGc.drawString(text, textX, textY, isTransparent);
            
            final Image textImage = this.textGraphicBuffer.getImage();
            // This is slow: must only do it once.
            final ImageData textImageData = textImage.getImageData();
            
            final int fgArgb32 = this.getArgb32();
            accessor = new MyCtda(
                    textImageData,
                    maxClippedTextRectInText,
                    fgArgb32);
        } finally {
            textGc.dispose();
        }

        return accessor;
    }
    
    @Override
    protected void disposeClippedTextDataAccessor(
            Object clippedTextDataAccessor) {
        // Nothing to do, all the data is in tmp class field.
    }

    @Override
    protected GRect getRenderedClippedTextRectInText(
            Object clippedTextDataAccessor) {
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        return accessor.rectInText;
    }

    @Override
    protected int getTextColor32(
            String text,
            Object clippedTextDataAccessor,
            int xInClippedText,
            int yInClippedText) {
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        final ImageData textImageData = accessor.textImageData;
        
        final int tmpArgb32 = SwtPaintUtils.getArgb32At(
                textImageData,
                xInClippedText,
                yInClippedText);
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
        final SwtBwdImageFromFile imageImpl = (SwtBwdImageFromFile) image;
        final int[] premulArgb32Arr = imageImpl.getPremulArgb32Arr();
        return premulArgb32Arr;
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
        final int[] premulArgb32Arr = (int[]) imageDataAccessor;
        final int index = yInImage * image.getWidth() + xInImage;
        final int premulArgb32 = premulArgb32Arr[index];
        return premulArgb32;
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
            GRect initialClip,
            //
            int[] pixelArr,
            int pixelArrScanlineStride,
            //
            SwtBwdGraphics parentGraphics) {
        super(
                binding,
                box,
                initialClip,
                //
                pixelArr,
                pixelArrScanlineStride);
        
        this.display = LangUtils.requireNonNull(display);
        
        if (parentGraphics == null) {
            this.isRootGraphics = true;
            
            final Device device = display;
            final int alpha8 = 0xFF;
            this.backingColor_BLACK = SwtUtils.newColor(device, 0x00, 0x00, 0x00, alpha8);
            this.backingColor_WHITE = SwtUtils.newColor(device, 0xFF, 0xFF, 0xFF, alpha8);
            
            final boolean mustCopyOnImageResize = false;
            this.textGraphicBuffer = new SwtGraphicBuffer(
                    display,
                    mustCopyOnImageResize,
                    ALLOW_TEXT_STORAGE_SHRINKING);
        } else {
            this.isRootGraphics = false;
            
            this.backingColor_BLACK = parentGraphics.backingColor_BLACK;
            this.backingColor_WHITE = parentGraphics.backingColor_WHITE;
            
            this.textGraphicBuffer = parentGraphics.textGraphicBuffer;
        }
    }
}
