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
package net.jolikit.bwd.impl.jogl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.awt.AwtBwdFont;
import net.jolikit.bwd.impl.awt.AwtUtils;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.InterfaceColorTypeHelper;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

public class JoglBwdGraphics extends AbstractIntArrayBwdGraphics {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final AffineTransform BACKING_TRANSFORM_IDENTITY =
            new AffineTransform();

    private static final AffineTransform[] ROTATION_TRANSFORM_BY_ORDINAL =
            AwtUtils.newRotationTransformArr();
    
    /*
     * For drawing text on backing int array directly.
     * 
     * The buffered image uses pixel array coordinates:
     * must take rootBoxTopLeft into account when
     * computing corresponding backing graphics transform.
     */
    
    private final BufferedImage bufferedImage;
    
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
     */
    public JoglBwdGraphics(
        InterfaceBwdBindingImpl binding,
        GRect box,
        //
        boolean isImageGraphics,
        int[] pixelArr,
        int pixelArrScanlineStride) {
        this(
            binding,
            topLeftOf(box),
            box,
            box, // initialClip
            //
            isImageGraphics,
            BufferedImageHelper.newBufferedImageWithIntArray(
                pixelArr,
                pixelArrScanlineStride,
                box.ySpan(),
                BufferedImageHelper.NATIVE_RGBA32_PIXEL_FORMAT,
                BufferedImageHelper.PREMUL));
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
        
        return new JoglBwdGraphics(
                this.getBinding(),
                this.getRootBoxTopLeft(),
                childBox,
                childInitialClip,
                //
                this.isImageGraphics(),
                this.bufferedImage);
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
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void initImpl() {
        this.g = this.bufferedImage.createGraphics();

        this.mustUpdateGClip = true;
        this.mustUpdateGTransform = true;
        this.mustUpdateGColor = true;
        this.mustUpdateGFont = true;

        super.initImpl();
    }

    @Override
    protected void finishImpl() {
        this.g.dispose();
    }

    /*
     * 
     */
    
    @Override
    protected void setBackingClip(GRect clipInBase) {
        this.mustUpdateGClip = true;
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        this.mustUpdateGTransform = true;
    }

    @Override
    protected void setBackingArgb(int argb32, BwdColor colorElseNull) {
        super.setBackingArgb(argb32, colorElseNull);
        
        this.mustUpdateGColor = true;
    }
    
    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        this.mustUpdateGFont = true;
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
        return JoglPaintHelper.getArrayColorHelper();
    }
    
    @Override
    protected int getArrayColor32FromArgb32(int argb32) {
        return JoglPaintHelper.getArrayColor32FromArgb32(argb32);
    }
    
    @Override
    protected int getArgb32FromArrayColor32(int premulColor32) {
        return JoglPaintHelper.getArgb32FromArrayColor32(premulColor32);
    }

    @Override
    protected int toInvertedArrayColor32(int premulColor32) {
        return JoglPaintHelper.toInvertedArrayColor32(premulColor32);
    }
    
    @Override
    protected int getArrayColorAlpha8(int premulColor32) {
        return JoglPaintHelper.getArrayColorAlpha8(premulColor32);
    }

    @Override
    protected int blendArrayColor32(int srcPremulColor32, int dstPremulColor32) {
        return JoglPaintHelper.blendArrayColor32(srcPremulColor32, dstPremulColor32);
    }

    /*
     * Text.
     */

    @Override
    protected Object getClippedTextDataAccessor(
            String text,
            GRect maxClippedTextRectInText) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void disposeClippedTextDataAccessor(
            Object clippedTextDataAccessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected GRect getRenderedClippedTextRectInText(
            Object clippedTextDataAccessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int getTextColor32(
            String text,
            Object clippedTextDataAccessor,
            int xInClippedText,
            int yInClippedText) {
        throw new UnsupportedOperationException();
    }

    /*
     * Images.
     */

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final AbstractJoglBwdImage imageImpl = (AbstractJoglBwdImage) image;
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
        return color32Arr[index];
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JoglBwdGraphics(
        InterfaceBwdBindingImpl binding,
        GPoint rootBoxTopLeft,
        GRect box,
        GRect initialClip,
        //
        boolean isImageGraphics,
        BufferedImage bufferedImage) {
        super(
            binding,
            rootBoxTopLeft,
            box,
            initialClip,
            //
            isImageGraphics,
            BufferedImageHelper.getIntPixelArr(bufferedImage),
            bufferedImage.getWidth());
        
        this.bufferedImage = bufferedImage;
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
        final GTransform transformArrToUser = this.getTransformArrToUser();
        
        this.g.setTransform(BACKING_TRANSFORM_IDENTITY);
        
        final GRotation rotation = transformArrToUser.rotation();
        this.g.translate(
            transformArrToUser.frame2XIn1(),
            transformArrToUser.frame2YIn1());
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
