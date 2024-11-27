/*
 * Copyright 2019-2024 Jeff Hain
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
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

    private static final AffineTransform BACKING_TRANSFORM_IDENTITY =
            new AffineTransform();

    private static final AffineTransform[] ROTATION_TRANSFORM_BY_ORDINAL =
            AwtUtils.newRotationTransformArr();
    
    private final BufferedImage backingImage;
    
    /*
     * For drawing text on backing image directly.
     */
    
    private Graphics2D g;
    
    private boolean mustUpdateGClip;
    private boolean mustUpdateGTransform;
    private boolean mustUpdateGColor;
    private boolean mustUpdateGFont;
    
    private int xShiftInUser;
    private int yShiftInUser;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for root graphics.
     * 
     * @param backingImage Must be backed by an int array
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
        BufferedImage backingImage) {
        this(
                binding,
                topLeftOf(box),
                box,
                box, // initialClip
                //
                isImageGraphics,
                requireCompatible(backingImage));
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
                this.backingImage);
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
            
            this.ensureUpToDateG();
            
            final int ascent = font.metrics().ascent();
            
            final int _x = x + this.xShiftInUser;
            final int _y = y + this.yShiftInUser + ascent;
            
            this.g.drawString(text, _x, _y);
        } else {
            super.drawText(x, y, text);
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void initImpl() {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.g = this.backingImage.createGraphics();
            
            this.mustUpdateGClip = true;
            this.mustUpdateGTransform = true;
            this.mustUpdateGColor = true;
            this.mustUpdateGFont = true;
        }
        
        super.initImpl();
    }

    @Override
    protected void finishImpl() {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.g.dispose();
        }
    }

    /*
     * 
     */
    
    @Override
    protected void setBackingClip(GRect clipInBase) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.mustUpdateGClip = true;
        }
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.mustUpdateGTransform = true;
        }
    }

    @Override
    protected void setBackingArgb(int argb32, BwdColor colorElseNull) {
        super.setBackingArgb(argb32, colorElseNull);
        
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.mustUpdateGColor = true;
        }
    }
    
    /*
     * 
     */
    
    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        if (MUST_DRAW_TEXT_ON_BACKING_IMAGE_DIRECTLY) {
            this.mustUpdateGFont = true;
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
        
        final AwtBwdFont font = (AwtBwdFont) this.getFont();
        final Font backingFont = font.getBackingFont();
        
        final int mcTextWidth = maxClippedTextRectInText.xSpan();
        final int mcTextHeight = maxClippedTextRectInText.ySpan();

        final int pixelCapacity = maxClippedTextRectInText.area();
        final int[] mcColor32Arr = new int[pixelCapacity];

        final BufferedImage image = BufferedImageHelper.newBufferedImageWithIntArray(
                mcColor32Arr,
                mcTextWidth,
                mcTextHeight,
                AwtPaintUtils.BUFFERED_IMAGE_TYPE_FOR_OFFSCREEN);

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
        BufferedImage backingImage) {
        super(
                binding,
                rootBoxTopLeft,
                box,
                initialClip,
                //
                isImageGraphics,
                BufferedImageHelper.getIntPixelArr(backingImage),
                backingImage.getWidth());
        this.backingImage = backingImage;
    }

    /**
     * @param image Image to use for this graphics.
     * @return The image.
     * @throws IllegalArgumentException if the image
     *         is not of compatible type.
     */
    private static BufferedImage requireCompatible(BufferedImage image) {
        final BihPixelFormat pixelFormat = BihPixelFormat.ARGB32;
        final boolean premul = true;
        return BufferedImageHelper.requireCompatible(
                image,
                pixelFormat,
                premul);
    }
    
    /**
     * Sets backing clip to be current clip.
     */
    private void setBackingClipToCurrent() {
        /*
         * Must apply transformed clip (and adjusting deltas),
         * since the backing graphics has transform applied.
         */
        
        final GRect clip = this.getClipInUser();

        this.g.setClip(
                clip.x() + this.xShiftInUser,
                clip.y() + this.yShiftInUser,
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
        
        this.g.setTransform(BACKING_TRANSFORM_IDENTITY);
        
        this.g.translate(
            transform.frame2XIn1() - rootBoxTopLeft.x(),
            transform.frame2YIn1() - rootBoxTopLeft.y());
        this.g.transform(ROTATION_TRANSFORM_BY_ORDINAL[rotation.ordinal()]);
        
        this.xShiftInUser = AwtUtils.computeXShiftInUser(rotation);
        this.yShiftInUser = AwtUtils.computeYShiftInUser(rotation);
    }
    
    private void ensureUpToDateG() {
        if (this.mustUpdateGTransform) {
            this.setBackingTransformToCurrent();
            this.mustUpdateGTransform = false;
            
            // Must reset clip as well, since we use transformed clip.
            this.setBackingClipToCurrent(); 
            this.mustUpdateGClip = false;
            
        } else if (this.mustUpdateGClip) {
            this.setBackingClipToCurrent(); 
            this.mustUpdateGClip = false;
        }
        
        if (this.mustUpdateGColor) {
            final int argb32 = this.getArgb32();
            final Color backingColor = AwtUtils.newColor(argb32);
            this.g.setColor(backingColor);
            this.mustUpdateGColor = false;
        }
        
        if (this.mustUpdateGFont) {
            final AwtBwdFont font = this.getFont();
            this.g.setFont(font.getBackingFont());
            this.mustUpdateGFont = false;
        }
    }
}
