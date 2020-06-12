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
package net.jolikit.bwd.impl.lwjgl3;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.awt.AwtBwdFont;
import net.jolikit.bwd.impl.awt.BufferedImageHelper;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;

public class LwjglBwdGraphics extends AbstractIntArrayBwdGraphics {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyTextDataAccessor {
        final int[] color32Arr;
        final GRect maxTextRelativeRect;
        public MyTextDataAccessor(
                int[] color32Arr,
                GRect maxTextRelativeRect) {
            this.color32Arr = color32Arr;
            this.maxTextRelativeRect = maxTextRelativeRect;
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for root graphics.
     */
    public LwjglBwdGraphics(
            InterfaceBwdBinding binding,
            GRect box,
            //
            int[] pixelArr,
            int pixelArrScanlineStride) {
        this(
                binding,
                box,
                box, // initialClip
                //
                pixelArr,
                pixelArrScanlineStride);
    }

    /*
     * 
     */

    @Override
    public InterfaceBwdGraphics newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".newChildGraphics(" + childBox + ")");
        }
        final GRect childInitialClip = this.getInitialClipInBase().intersected(childBox);
        return new LwjglBwdGraphics(
                this.getBinding(),
                childBox,
                childInitialClip,
                //
                this.getPixelArr(),
                this.getPixelArrScanlineStride());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void finishImpl() {
        // Nothing to do.
    }
    
    /*
     * 
     */

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        // We use AWT backed font stored in super.
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
        long argb64,
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
                argb64,
                //
                mustSetFont,
                font);
    }
    
    /*
     * 
     */

    @Override
    protected int getArrayColor32FromArgb32(int argb32) {
        return LwjglPaintHelper.getArrayColor32FromArgb32(argb32);
    }
    
    @Override
    protected int getArgb32FromArrayColor32(int premulColor32) {
        return LwjglPaintHelper.getArgb32FromArrayColor32(premulColor32);
    }

    @Override
    protected int toInvertedArrayColor32(int premulColor32) {
        return LwjglPaintHelper.toInvertedArrayColor32(premulColor32);
    }
    
    @Override
    protected int getArrayColorAlpha8(int premulColor32) {
        return LwjglPaintHelper.getArrayColorAlpha8(premulColor32);
    }

    @Override
    protected int blendArrayColor32(int srcPremulColor32, int dstPremulColor32) {
        return LwjglPaintHelper.blendArrayColor32(srcPremulColor32, dstPremulColor32);
    }

    /*
     * Text.
     */
    
    @Override
    protected Object getTextDataAccessor(
            String text,
            GRect maxTextRelativeRect) {
        
        final AwtBwdFont font = (AwtBwdFont) this.getFont();
        final Font backingFont = font.getBackingFont();
        
        final int maxTextWidth = maxTextRelativeRect.xSpan();
        final int maxTextHeight = maxTextRelativeRect.ySpan();
        
        final int pixelCapacity = NumbersUtils.timesExact(maxTextWidth, maxTextHeight);
        final int[] textPixelArr = new int[pixelCapacity];
        
        final BufferedImage image = BufferedImageHelper.newBufferedImageWithIntArray(
                textPixelArr,
                maxTextWidth,
                maxTextHeight,
                BufferedImageHelper.NATIVE_RGBA32_PIXEL_FORMAT,
                BufferedImageHelper.PREMUL);
        
        // Drawing the text in the image.
        final Graphics2D g2d = image.createGraphics();
        try {
            g2d.setFont(backingFont);
            final int argb32 = this.getArgb32();
            final Color awtColor = new Color(
                    Argb32.getRed8(argb32),
                    Argb32.getGreen8(argb32),
                    Argb32.getBlue8(argb32),
                    Argb32.getAlpha8(argb32));
            g2d.setColor(awtColor);
            
            final int x = 0 - maxTextRelativeRect.x();
            final int y = font.fontMetrics().fontAscent() - maxTextRelativeRect.y();
            g2d.drawString(text, x, y);
        } finally {
            g2d.dispose();
        }
        
        final MyTextDataAccessor accessor = new MyTextDataAccessor(
                textPixelArr,
                maxTextRelativeRect);
        return accessor;
    }

    @Override
    protected void disposeTextDataAccessor(Object textDataAccessor) {
        // Nothing to do.
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
        final int index = yInText * accessor.maxTextRelativeRect.xSpan() + xInText;
        // Already alpha-premultiplied, due to the image we used for text drawing.
        final int color32 = accessor.color32Arr[index];
        return color32;
    }

    /*
     * Images.
     */

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final LwjglBwdImage imageImpl = (LwjglBwdImage) image;
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
    
    private LwjglBwdGraphics(
            InterfaceBwdBinding binding,
            GRect box,
            GRect initialClip,
            //
            int[] pixelArr,
            int pixelArrScanlineStride) {
        super(
                binding,
                box,
                initialClip,
                //
                pixelArr,
                pixelArrScanlineStride);
    }
}
