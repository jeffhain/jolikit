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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.graphics.InterfaceColorTypeHelper;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Graphics based on an int array, except for drawText()
 * for which we draw directly on the backing image (much faster).
 * 
 * The point is that AWT Graphics can be relatively very slow
 * for some operations, like drawing pixel-by-pixel with drawLine(...)
 * (costs more to draw each pixel than to compute whether it is in
 * Mandelbrot!).
 */
public class AwtBwdGraphicsWithIntArr extends AbstractIntArrayBwdGraphics {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * Much faster that using super class API with
     * drawing on a temporary buffered image.
     */
    private static final boolean MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY = true;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Clipped Text Data Accessor.
     */
    private static class MyCtda {
        final int[] color32Arr;
        final GRect rectInText;
        public MyCtda(
                int[] color32Arr,
                GRect rectInText) {
            this.color32Arr = color32Arr;
            this.rectInText = rectInText;
        }
        public int getScanlineStride() {
            return this.rectInText.xSpan();
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final BufferedImageHelper backingHelper;
    
    private final AwtPrlResizeImage prlResizeImage = new AwtPrlResizeImage();
    
    /*
     * We use backing graphics only in some particular cases,
     * so to avoid the overhead of updating it all the time
     * (e.g. when color changes, etc.), we only set dirty flags
     * and update it when needed.
     */
    
    private Graphics2D gPossiblyDirty;
    private int xShiftInUserPossiblyDirty;
    private int yShiftInUserPossiblyDirty;
    
    /**
     * Counts for shifts too.
     */
    private boolean isDirtyGTransform;
    private boolean isDirtyGClip;
    private boolean isDirtyGColor;
    private boolean isDirtyGFont;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for root graphics.
     * 
     * @param backingHelper Image must be backed by an int array
     *        or alpha premultiplied ARGB32 pixels
     *        (due to this graphics drawing on it either
     *        directly into its array of pixels,
     *        or through its graphics).
     * @throws IllegalArgumentException if the image is not compatible.
     */
    public AwtBwdGraphicsWithIntArr(
        InterfaceBwdBindingImpl binding,
        GRect box,
        //
        boolean isImageGraphics,
        BufferedImageHelper backingHelper) {
        this(
                binding,
                topLeftOf(box),
                box,
                box, // initialClip
                //
                isImageGraphics,
                checkedArgbPre(backingHelper));
    }
    
    /*
     * 
     */

    @Override
    public InterfaceBwdGraphics newChildGraphics(
            GRect childBox,
            GRect childMaxInitialClip) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(
                    this.getClass().getSimpleName() + "-" + this.hashCode()
                    + ".newChildGraphics(" + childBox
                    + "," + childMaxInitialClip + ")");
        }
        
        final GRect childInitialClip =
                this.getInitialClipInBase().intersected(
                        childMaxInitialClip.intersected(childBox));
        
        return new AwtBwdGraphicsWithIntArr(
                this.getBinding(),
                this.getRootBoxTopLeft(),
                childBox,
                childInitialClip,
                //
                this.isImageGraphics(),
                this.backingHelper.duplicate());
    }
    
    /*
     * 
     */

    @Override
    public AwtBwdFont getFont() {
        return (AwtBwdFont) super.getFont();
    }
    
    /*
     * Text.
     */
    
    @Override
    public void drawText(
            int x, int y,
            String text) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.checkUsable();
            LangUtils.requireNonNull(text);

            final AwtBwdFont font = this.getFont();
            if (font.isDisposed()) {
                throw new IllegalStateException("current font is disposed: " + font);
            }
            
            final InterfaceBwdFontMetrics metrics = font.metrics();
            
            final int theoTextHeight = metrics.height();
            
            if (theoTextHeight <= 0) {
                return;
            }
            
            final Graphics2D g = this.ensureAndGetUpToDateG();
            
            final int ascent = font.metrics().ascent();
            
            final int _x = x + this.xShiftInUserPossiblyDirty;
            final int _y = y + this.yShiftInUserPossiblyDirty + ascent;
            
            g.drawString(text, _x, _y);
        } else {
            super.drawText(x, y, text);
        }
    }
    
    /*
     * Images.
     */

    @Override
    protected void drawImageImpl(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan) {
        
        final boolean mustUseBackingImageScalingIfApplicable =
            this.getBindingConfig().getMustUseBackingImageScalingIfApplicable();
        if (!mustUseBackingImageScalingIfApplicable) {
            /*
             * A tad simpler than using ScaledRectDrawing from PrlResizeImage
             * (row drawer already configured).
             */
            super.drawImageImpl(
                x, y, xSpan, ySpan,
                image,
                sx, sy, sxSpan, sySpan);
            return;
        }
        
        /*
         * Using AWT drawImage() whenever possible, for speed.
         */
        
        final AbstractAwtBwdImage toDrawAwtImage =
            (AbstractAwtBwdImage) image;
        
        final GRect srcRect = GRect.valueOf(sx, sy, sxSpan, sySpan);
        final GRect dstRect = GRect.valueOf(x, y, xSpan, ySpan);
        final GRect dstClip = this.getClipInUser();
        
        this.prlResizeImage.drawImage(
            this.getBinding().getInternalParallelizer(),
            //
            this.getImageScalingType(),
            mustUseBackingImageScalingIfApplicable,
            this.getRootBoxTopLeft(),
            this.getTransform(),
            AlphaComposite.SrcOver,
            //
            toDrawAwtImage.getBufferedImageHelperArgbPre(),
            srcRect,
            //
            this.backingHelper,
            dstRect,
            dstClip);
    }
    
    /*
     * 
     */
    
    @Override
    public void flipColors(int x, int y, int xSpan, int ySpan) {
        /*
         * TODO awt Graphics.setXORMode(...) is fast,
         * but doesn't work properly with non-opaque background,
         * so if image graphics (i.e. with potentially non-opaque background)
         * we use super.
         */
        if (this.isImageGraphics()) {
            super.flipColors(x, y, xSpan, ySpan);
        } else {
            this.checkUsable();
            if ((xSpan <= 0) || (ySpan <= 0)) {
                return;
            }
            
            final Graphics2D g = this.ensureAndGetUpToDateG();
            
            final Color c = g.getColor();
            g.setBackground(Color.WHITE);
            g.setColor(Color.WHITE);
            g.setXORMode(Color.BLACK);
            try {
                final int _x = x + this.xShiftInUserPossiblyDirty;
                final int _y = y + this.yShiftInUserPossiblyDirty;
                // Relying on backing clipping.
                g.fillRect(_x, _y, xSpan, ySpan);
            } finally {
                g.setBackground(c);
                g.setColor(c);
                g.setPaintMode();
            }
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void initImpl() {
        this.gPossiblyDirty = this.backingHelper.getImage().createGraphics();
        this.isDirtyGTransform = true;
        this.isDirtyGClip = true;
        this.isDirtyGColor = true;
        this.isDirtyGFont = MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY;
        
        super.initImpl();
    }

    @Override
    protected void finishImpl() {
        this.gPossiblyDirty.dispose();
    }

    /*
     * 
     */
    
    @Override
    protected void setBackingClip(GRect clipInBase) {
        this.isDirtyGClip = true;
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        this.isDirtyGTransform = true;
    }

    @Override
    protected void setBackingArgb(int argb32, BwdColor colorElseNull) {
        super.setBackingArgb(argb32, colorElseNull);
        
        this.isDirtyGColor = true;
    }
    
    /*
     * 
     */
    
    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.isDirtyGFont = true;
        } else {
            // We use AWT backed font stored in super.
        }
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
    protected InterfaceColorTypeHelper getArrayColorHelper() {
        return PremulArgbHelper.getInstance();
    }

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
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            throw new UnsupportedOperationException();
        }
        
        final AwtBwdFont font = this.getFont();
        final Font backingFont = font.getBackingFont();
        
        final int mcTextWidth = maxClippedTextRectInText.xSpan();
        final int mcTextHeight = maxClippedTextRectInText.ySpan();

        final int pixelCapacity = maxClippedTextRectInText.area();
        final int[] mcColor32Arr = new int[pixelCapacity];

        final int scanlineStride = mcTextWidth;
        final BufferedImage image = BufferedImageHelper.newBufferedImageWithIntArray(
                mcColor32Arr,
                scanlineStride,
                mcTextWidth,
                mcTextHeight,
                AwtUtils.COMMON_BUFFERED_IMAGE_TYPE_ARGB_PRE);

        // Drawing the text in the image.
        final Graphics2D g2d = image.createGraphics();
        try {
            g2d.setFont(backingFont);
            final int argb32 = this.getArgb32();
            final Color awtColor = AwtUtils.newColor(argb32);
            g2d.setColor(awtColor);

            final int x = 0 - maxClippedTextRectInText.x();
            final int y = font.metrics().ascent() - maxClippedTextRectInText.y();
            g2d.drawString(text, x, y);
        } finally {
            g2d.dispose();
        }

        return new MyCtda(
                mcColor32Arr,
                maxClippedTextRectInText);
    }

    @Override
    protected void disposeClippedTextDataAccessor(
            Object clippedTextDataAccessor) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            throw new UnsupportedOperationException();
        }
        
        // Nothing to do.
    }

    @Override
    protected GRect getRenderedClippedTextRectInText(
            Object clippedTextDataAccessor) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            throw new UnsupportedOperationException();
        }
        
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        return accessor.rectInText;
    }

    @Override
    protected int getTextColor32(
            String text,
            Object clippedTextDataAccessor,
            int xInClippedText,
            int yInClippedText) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            throw new UnsupportedOperationException();
        }
        
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        final int index = yInClippedText * accessor.getScanlineStride() + xInClippedText;
        // Already alpha-premultiplied, due to the image we used for text drawing.
        final int color32 = accessor.color32Arr[index];
        return color32;
    }

    /*
     * Images.
     */

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final AbstractAwtBwdImage imageImpl = (AbstractAwtBwdImage) image;
        final int[] color32Arr = imageImpl.getPremulArgb32Arr();
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
        return color32Arr[index];
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private AwtBwdGraphicsWithIntArr(
        InterfaceBwdBindingImpl binding,
        GPoint rootBoxTopLeft,
        GRect box,
        GRect initialClip,
        //
        boolean isImageGraphics,
        BufferedImageHelper backingHelper) {
        super(
                binding,
                rootBoxTopLeft,
                box,
                initialClip,
                //
                isImageGraphics,
                backingHelper.getIntArrayDirectlyUsed(),
                backingHelper.getScanlineStride());
        this.backingHelper = backingHelper;
    }
    
    private static BufferedImageHelper checkedArgbPre(BufferedImageHelper backingHelper) {
        if (backingHelper.getIntArrayDirectlyUsed() == null) {
            throw new IllegalArgumentException("helper has no directly usable int array");
        }
        if (backingHelper.getPixelFormat() != BihPixelFormat.ARGB32) {
            throw new IllegalArgumentException("helper image is not ARGB32");
        }
        if (!backingHelper.isAlphaPremultiplied()) {
            throw new IllegalArgumentException("helper image is not alpha-premultipled");
        }
        return backingHelper;
    }
    
    /**
     * Sets backing clip to be current clip.
     * Backing transform must be up to date.
     */
    private void setBackingClipToCurrent() {
        /*
         * Must apply transformed clip (and adjusting deltas),
         * since the backing graphics has transform applied.
         */
        
        final GRect clip = this.getClipInUser();

        this.gPossiblyDirty.setClip(
                clip.x() + this.xShiftInUserPossiblyDirty,
                clip.y() + this.yShiftInUserPossiblyDirty,
                clip.xSpan(),
                clip.ySpan());
    }
    
    /**
     * Sets backing transform to be current transform.
     */
    private void setBackingTransformToCurrent() {
        final GTransform transform = this.getTransform();
        final GRotation rotation = transform.rotation();
        
        final GPoint rootBoxTopLeft = this.getRootBoxTopLeft();
        
        AwtUtils.setGraphicsTransform(rootBoxTopLeft, transform, this.gPossiblyDirty);
        
        this.xShiftInUserPossiblyDirty = AwtUtils.computeXShiftInUser(rotation);
        this.yShiftInUserPossiblyDirty = AwtUtils.computeYShiftInUser(rotation);
    }
    
    /**
     * @return Backing graphics ready to use (no longer dirty).
     */
    private Graphics2D ensureAndGetUpToDateG() {
        if (this.isDirtyGTransform) {
            this.setBackingTransformToCurrent();
            this.isDirtyGTransform = false;
            
            // Must reset clip as well, since we use transformed clip.
            this.setBackingClipToCurrent(); 
            this.isDirtyGClip = false;
            
        } else if (this.isDirtyGClip) {
            this.setBackingClipToCurrent(); 
            this.isDirtyGClip = false;
        }
        
        if (this.isDirtyGColor) {
            final int argb32 = this.getArgb32();
            final Color backingColor = AwtUtils.newColor(argb32);
            this.gPossiblyDirty.setColor(backingColor);
            this.isDirtyGColor = false;
        }
        
        if (this.isDirtyGFont) {
            final AwtBwdFont font = this.getFont();
            this.gPossiblyDirty.setFont(font.getBackingFont());
            this.isDirtyGFont = false;
        }
        
        return this.gPossiblyDirty;
    }
}
