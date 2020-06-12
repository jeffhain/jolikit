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
package net.jolikit.bwd.impl.utils.graphics;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb3264;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.gprim.GprimUtils;
import net.jolikit.lang.LangUtils;

/**
 * Abstract class to make it easier to implement BWD graphics
 * drawing on a Java int array of pixels, using an eventually specific
 * color model.
 */
public abstract class AbstractIntArrayBwdGraphics extends AbstractBwdGraphics {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyPrimitives extends AbstractBwdPrimitives {
        @Override
        public void drawPointInClip(int x, int y) {
            drawPointInClip_raw(x, y);
        }
        @Override
        public int drawHorizontalLineInClip(
                int x1, int x2, int y,
                int factor, short pattern, int pixelNum) {
            if (pattern == GprimUtils.PLAIN_PATTERN) {
                drawHorizontalLineInClip_raw(x1, x2, y);
                return pixelNum;
            } else {
                return super.drawHorizontalLineInClip(
                        x1, x2, y,
                        factor, pattern, pixelNum);
            }
        }
        @Override
        public int drawVerticalLineInClip(
                int x, int y1, int y2,
                int factor, short pattern, int pixelNum) {
            if (pattern == GprimUtils.PLAIN_PATTERN) {
                drawVerticalLineInClip_raw(x, y1, y2);
                return pixelNum;
            } else {
                return super.drawVerticalLineInClip(
                        x, y1, y2,
                        factor, pattern, pixelNum);
            }
        }
        /**
         * @param areHorVerFlipped We ignore that, we always do the right thing.
         */
        @Override
        public void fillRectInClip(
                int x, int y, int xSpan, int ySpan,
                boolean areHorVerFlipped) {
            final boolean isClear = false;
            fillRectInClip_raw(
                    x, y, xSpan, ySpan,
                    isClear);
        }
    };
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MyPrimitives primitives = new MyPrimitives();
    
    /**
     * Pixels corresponding to the client area, which top-left corner is (0,0).
     * index = y * xSpan + x.
     * Can be larger than needed, to allow for reusing a same array.
     * 
     * Can use any color format.
     * 
     * Each graphics instance writes and read directly into it, but due to
     * clipping, or properly designed parallel view paintings, a same pixel
     * should not be written or read concurrently, so it still allows for
     * parallel painting.
     */
    private final int[] pixelArr;
    
    /**
     * Scanline stride, in pixels.
     */
    private final int pixelArrScanlineStride;
    
    /**
     * The color to use for drawing in the array of pixels.
     * 
     * Can be any format.
     */
    private int arrColor;
    
    /**
     * For clearing.
     */
    private int arrColorOpaque;
    
    /*
     * temps
     */
    
    private final IntArrHolder tmpArr = new IntArrHolder();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractIntArrayBwdGraphics(
            InterfaceBwdBinding binding,
            GRect box,
            GRect initialClip,
            //
            int[] pixelArr,
            int pixelArrScanlineStride) {
        super(
                binding,
                box,
                initialClip);

        this.pixelArr = LangUtils.requireNonNull(pixelArr);
        this.pixelArrScanlineStride = pixelArrScanlineStride;
    }

    /**
     * @return The array of pixels on which drawing takes places.
     */
    public int[] getPixelArr() {
        return this.pixelArr;
    }

    public int getPixelArrScanlineStride() {
        return this.pixelArrScanlineStride;
    }
    
    /*
     * 
     */
    
    @Override
    public void clearRect(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        final GRect clip = this.getClipInUser();
        
        final int clippedX = GRect.intersectedPos(clip.x(), x);
        final int clippedY = GRect.intersectedPos(clip.y(), y);
        final int clippedXSpan = GRect.intersectedSpan(clip.x(), clip.xSpan(), x, xSpan);
        final int clippedYSpan = GRect.intersectedSpan(clip.y(), clip.ySpan(), y, ySpan);
        
        final boolean isClear = true;
        fillRectInClip_raw(
                clippedX,
                clippedY,
                clippedXSpan,
                clippedYSpan,
                isClear);
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
        
        final GTransform transform = this.getTransform();
        
        final InterfaceBwdFont font = this.getFont();
        if (font.isDisposed()) {
            throw new IllegalStateException("current font is disposed: " + font);
        }
        
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        
        final int theoTextWidth = metrics.computeTextWidth(text);
        final int theoTextHeight = metrics.fontHeight();
        
        if (theoTextHeight <= 0) {
            /*
             * Early abort if font height is zero.
             * We don't abort on zero theoretical text width, because fonts are
             * such a mess that it might be 0 even when the font actually has
             * something do draw.
             */
            return;
        }
        
        final GRect maxTextRelativeRectInUser = this.computeMaxTextRelativeRect(
                theoTextWidth,
                theoTextHeight);
        final GRect maxTextRectInUser = maxTextRelativeRectInUser.withPosDeltas(x, y);
        
        final GRect clipInUser = this.getClipInUser();
        
        /*
         * Coordinates rework based on clipping.
         */
        
        final GRect maxTextRectInUserClipped = maxTextRectInUser.intersected(clipInUser);
        if (maxTextRectInUserClipped.isEmpty()) {
            // All would be drawn out of clip.
            return;
        }
        
        /*
         * 
         */
        
        final Object textDataAccessor = this.getTextDataAccessor(text, maxTextRelativeRectInUser);
        try {
            final GRect renderedTextRelativeRectInUser = this.getRenderedTextRelativeRect(textDataAccessor);
            final GRect renderedTextRectInUser;
            final GRect renderedTextRectInUserClipped;
            if (renderedTextRelativeRectInUser.equals(maxTextRelativeRectInUser)) {
                renderedTextRectInUser = maxTextRectInUser;
                renderedTextRectInUserClipped = maxTextRectInUserClipped;
            } else {
                renderedTextRectInUser = renderedTextRelativeRectInUser.withPosDeltas(x, y);
                renderedTextRectInUserClipped = renderedTextRectInUser.intersected(clipInUser);
                if (renderedTextRectInUserClipped.isEmpty()) {
                    return;
                }
            }
            
            /*
             * 
             */
            
            final int clippedXOffset = renderedTextRectInUserClipped.x() - renderedTextRectInUser.x();
            final int clippedYOffset = renderedTextRectInUserClipped.y() - renderedTextRectInUser.y();
            
            final int clippedWidth = renderedTextRectInUserClipped.xSpan();
            final int clippedHeight = renderedTextRectInUserClipped.ySpan();
            
            for (int yi = 0; yi < clippedHeight; yi++) {
                final int dstY = renderedTextRectInUserClipped.y() + yi;
                final int srcY = clippedYOffset + yi;

                for (int xi = 0; xi < clippedWidth; xi++) {
                    final int dstX = renderedTextRectInUserClipped.x() + xi;
                    final int srcX = clippedXOffset + xi;

                    final int color32 = this.getTextColor32(text, textDataAccessor, srcX, srcY);
                    final int alpha8 = this.getArrayColorAlpha8(color32);
                    if (alpha8 == 0) {
                        /*
                         * Should be a quite common case for text,
                         * especially since we use enlarged rectangles,
                         * so worth to optimize this case away
                         * before coordinates conversions
                         * and messing with array.
                         */
                        continue;
                    }

                    final int xInBase = transform.xIn1(dstX, dstY);
                    final int yInBase = transform.yIn1(dstX, dstY);
                    final int dstIndex = yInBase * this.pixelArrScanlineStride + xInBase;

                    this.blendColor32(dstIndex, color32);
                }
            }
        } finally {
            this.disposeTextDataAccessor(textDataAccessor);
        }
    }
    
    /*
     * 
     */

    @Override
    public void flipColors(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }

        final GTransform transform = this.getTransform();
        
        final GRect rectInUser = GRect.valueOf(x, y, xSpan, ySpan);
        final GRect rectInBase = transform.rectIn1(rectInUser);
        
        final GRect clipInBase = this.getClipInBase();
        final GRect rectInBaseClipped = rectInBase.intersected(clipInBase);
        
        final int xInBaseClipped = rectInBaseClipped.x();
        final int yInBaseClipped = rectInBaseClipped.y();
        final int xSpanInBaseClipped = rectInBaseClipped.xSpan();
        final int ySpanInBaseClipped = rectInBaseClipped.ySpan();
        
        /*
         * Taking care to loop on lines first
         * (true lines, i.e. in base coordinates,
         * which might be verticals in user coordinates).
         */
        
        final int[] pixelArr = this.pixelArr;
        
        for (int j = 0; j < ySpanInBaseClipped; j++) {
            int index = (yInBaseClipped + j) * this.pixelArrScanlineStride + xInBaseClipped;
            for (int i = 0; i < xSpanInBaseClipped; i++) {
                final int color32_from = pixelArr[index];
                final int color32_to = toInvertedArrayColor32(color32_from);
                pixelArr[index] = color32_to;
                ++index;
            }
        }
    }
    
    /*
     * 
     */

    @Override
    public int getArgb32At(int x, int y) {
        // For usability and coordinates check.
        super.getArgb32At(x, y);
        
        final GTransform transform = this.getTransform();
        final int xInBase = transform.xIn1(x, y);
        final int yInBase = transform.yIn1(x, y);
        
        final int index = yInBase * this.pixelArrScanlineStride + xInBase;
        final int color32 = this.pixelArr[index];
        final int argb32 = this.getArgb32FromArrayColor32(color32);
        return argb32;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void setBackingClip(GRect clipInBase) {
        // Usually no backing clip to update.
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        // Usually no backing transform to update.
    }

    /**
     * Overriding implementations must call this one.
     */
    @Override
    protected void setBackingArgb64(long argb64) {
        final int argb32 = Argb3264.toArgb32(argb64);
        this.arrColor = this.getArrayColor32FromArgb32(argb32);
        this.arrColorOpaque = this.getArrayColor32FromArgb32(0xFF000000 | argb32);
    }
    
    /*
     * 
     */

    @Override
    protected AbstractBwdPrimitives getPrimitives() {
        return this.primitives;
    }
    
    /*
     * Text.
     */
    
    /**
     * Glyphs can leak outside theoretical bounding boxes computed out of
     * font metrics, on any of their four sides, and possibly a lot when
     * combining diacritical marks (but we don't add additional overhead
     * to deal with this pathological case).
     * In particular, code points like 0x11A8 can have glyphs with
     * a quite large width, while font metrics pretend they have
     * a regular width.
     * For these reasons, we don't trust font metrics much, and consider an enlarged
     * rectangle to make sure eventually leaking pixels are properly drawn.
     * 
     * @param theoTextWidth Result of metrics computeTextWidth(...).
     * @param theoTextHeight Font height.
     * @param Max bounding box for text, in text coordinates,
     *        i.e. relative to the (x,y) position given to drawText(...) method.
     */
    protected GRect computeMaxTextRelativeRect(
            int theoTextWidth,
            int theoTextHeight) {
        
        final InterfaceBwdFont font = this.getFont();
        // Font heights can be quite far from font size,
        // so we use the max of both.
        final int magnitude = Math.max(
                font.fontSize(),
                font.fontMetrics().fontHeight());
        
        final int leftLeakTolerance = magnitude;
        final int topLeakTolerance = magnitude;
        final int rightLeakTolerance = 2 * magnitude;
        final int bottomLeakTolerance = magnitude;
        
        final GRect maxTextRect = GRect.valueOf(
                -leftLeakTolerance,
                -topLeakTolerance,
                theoTextWidth + (leftLeakTolerance + rightLeakTolerance),
                theoTextHeight + (topLeakTolerance + bottomLeakTolerance));
        return maxTextRect;
    }

    /*
     * Images.
     */
    
    /**
     * Supposes that src rect is clipped to image, which is the case.
     */
    @Override
    protected void drawImageImpl(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan) {
        
        final GTransform transform = this.getTransform();
        
        final GRect dstRectInUser_initial = GRect.valueOf(x, y, xSpan, ySpan);
        final GRect srcRectInImg_initial = GRect.valueOf(sx, sy, sxSpan, sySpan);
        
        final GRect clipInUser = this.getClipInUser();
        
        /*
         * 1) Coordinates rework based on clippings (already done for image clip for src).
         */
        
        final GRect dstRectInUser_userClipRework = dstRectInUser_initial.intersected(clipInUser);
        if (dstRectInUser_userClipRework.isEmpty()) {
            return;
        }

        /*
         * 2) Rework clips according to each other, taking scaling into account.
         * There is no rotation between user and image frames.
         * Transform to base frame is taken into account last.
         */
        
        final GRect srcRectInImg_userClipRework = ScaledRectUtils.computeNewPeerRect(
                dstRectInUser_initial,
                dstRectInUser_userClipRework,
                srcRectInImg_initial);
        if (srcRectInImg_userClipRework.isEmpty()) {
            return;
        }
        
        /*
         * 
         */
        
        final GRect dstRect = dstRectInUser_userClipRework;
        
        final int srcRowLengthInitial = srcRectInImg_initial.xSpan();
        final int srcColLengthInitial = srcRectInImg_initial.ySpan();
        final int dstRowLengthInitial = dstRectInUser_initial.xSpan();
        final int dstColLengthInitial = dstRectInUser_initial.ySpan();
        
        /*
         * Taking care to scale as well as possible, i.e. such as if there is no scaling,
         * floating-point pixel coordinates are exactly (modulo ULP error) computed,
         * by computing a ratio over the full span (number of pixels),
         * but using 0.5 offset due to first pixel being centered 0.5 away
         * from span start.
         * This way, no clipping test is required during loops.
         */
        
        final double dxi_to_0_1_factor = 1.0 / dstRowLengthInitial;
        final double dyi_to_0_1_factor = 1.0 / dstColLengthInitial;
        
        // dyio = destYIndexOffset, from specified dest Y.
        final int dyio = dstRect.y() - dstRectInUser_initial.y();
        final int dxio = dstRect.x() - dstRectInUser_initial.x();
        
        // Optimization, to avoid useless scalings.
        final boolean gotXScaling = (dstRowLengthInitial != srcRowLengthInitial);
        final boolean gotYScaling = (dstColLengthInitial != srcColLengthInitial);
        
        // Optimization, to avoid computing columns scaling for each row.
        final int[] sxiByDxicArr;
        if (!gotXScaling) {
            sxiByDxicArr = null;
        } else {
            sxiByDxicArr = this.tmpArr.getArr(dstRect.xSpan());
            for (int dxic = 0; dxic < dstRect.xSpan(); dxic++) {
                final int dxi = dxio + dxic;
                final int sxi = ScaledIntRectDrawingUtils.computeSi(
                        srcRowLengthInitial,
                        dxi_to_0_1_factor,
                        dxi);
                sxiByDxicArr[dxic] = sxi;
            }
        }
        
        final Object imageDataAccessor = this.getImageDataAccessor(image);
        try {
            // dyic = destYIndexClipped (0 at clip start,
            // not specified dest Y).
            for (int dyic = 0; dyic < dstRect.ySpan(); dyic++) {
                final int dyi = dyio + dyic;
                final int syi;
                if (!gotYScaling) {
                    syi = dyi;
                } else {
                    syi = ScaledIntRectDrawingUtils.computeSi(
                            srcColLengthInitial,
                            dyi_to_0_1_factor,
                            dyi);
                }
                final int srcY = sy + syi;
                final int dstY = y + dyi;
                
                for (int dxic = 0; dxic < dstRect.xSpan(); dxic++) {
                    final int dxi = dxio + dxic;
                    final int sxi;
                    if (!gotXScaling) {
                        sxi = dxi;
                    } else {
                        sxi = sxiByDxicArr[dxic];
                    }
                    final int srcX = sx + sxi;
                    final int dstX = x + dxi;

                    final int color32 = this.getImageColor32(image, imageDataAccessor, srcX, srcY);

                    final int xInBase = transform.xIn1(dstX, dstY);
                    final int yInBase = transform.yIn1(dstX, dstY);
                    final int dstIndex = yInBase * this.pixelArrScanlineStride + xInBase;

                    this.blendColor32(dstIndex, color32);
                }
            }
        } finally {
            this.disposeImageDataAccessor(imageDataAccessor);
        }
    }

    /*
     * 
     */
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The same color in the format to use in the array of pixels.
     */
    protected abstract int getArrayColor32FromArgb32(int argb32);

    protected abstract int getArgb32FromArrayColor32(int color32);

    /**
     * @param color32 A 32 bits color, in the format to use
     *        in the array of pixels.
     * @return The specified color with alpha preserved but with
     *         each other value replaced with (255 - value).
     */
    protected abstract int toInvertedArrayColor32(int color32);
    
    /**
     * Useful for optimization, to special case for fully transparent
     * or fully opaque colors.
     */
    protected abstract int getArrayColorAlpha8(int color32);
    
    protected abstract int blendArrayColor32(int srcColor32, int dstColor32);

    /*
     * Text.
     */

    /**
     * @param maxTextRelativeRect Must not paint pixels out of it.
     *        Passed here so as not to have to recompute it.
     * @return An object from which text data (bounding box, drawn pixels, etc.)
     *         can be quickly retrieved.
     */
    protected abstract Object getTextDataAccessor(
            String text,
            GRect maxTextRelativeRect);

    protected abstract void disposeTextDataAccessor(Object textDataAccessor);

    /**
     * @return The rectangle containing rendered text,
     *         relative to the (x,y) position given to drawText(...) method,
     *         i.e. with (0,0) position in rectangle corresponding to (x,y)
     *         in user coordinates.
     */
    protected abstract GRect getRenderedTextRelativeRect(Object textDataAccessor);

    /**
     * This method returns a color, and not just a boolean indicating
     * whether there is text at the specified position, to allow for
     * anti-aliased text rendering, without which, with some backing libraries,
     * text can be quite ugly with small-sized fonts.
     * 
     * @return The color to use for drawing the text pixel,
     *         typically an alpha-variant of current graphics color,
     *         or a fully transparent color if the pixel is not to be drawn.
     */
    protected abstract int getTextColor32(
            String text,
            Object textDataAccessor,
            int xInText,
            int yInText);

    /*
     * Images.
     */

    /**
     * The point of this method is to avoid superfluous internal object
     * retrieval for each pixel drawing, when image drawing is done
     * pixel by pixel.
     * 
     * @param image An image.
     * @return The object on which actual drawing is done.
     */
    protected abstract Object getImageDataAccessor(InterfaceBwdImage image);

    protected abstract void disposeImageDataAccessor(Object imageDataAccessor);
    
    /**
     * @return The pixel to copy or blend into the array of pixels.
     */
    protected abstract int getImageColor32(
            InterfaceBwdImage image,
            Object imageDataAccessor,
            int xInImage,
            int yInImage);

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void blendColor32(int index, int srcColor32) {
        final int dstColor32 = this.pixelArr[index];
        final int newColor32 = this.blendArrayColor32(srcColor32, dstColor32);
        this.pixelArr[index] = newColor32;
    }
    
    /*
     * 
     */
    
    private void drawPointInClip_raw(int x, int y) {
        final GTransform transform = this.getTransform();
        final int xInBase = transform.xIn1(x, y);
        final int yInBase = transform.yIn1(x, y);
        
        final int index = yInBase * this.pixelArrScanlineStride + xInBase;
        
        this.blendColor32(index, this.arrColor);
    }
    
    private void drawHorizontalLineInClip_raw(int x1, int x2, int y) {
        final int x = Math.min(x1, x2);
        final int xSpan = Math.abs(x2 - x1) + 1;
        final int ySpan = 1;
        final boolean isClear = false;
        fillRectInClip_raw(x, y, xSpan, ySpan, isClear);
    }
    
    private void drawVerticalLineInClip_raw(int x, int y1, int y2) {
        final int y = Math.min(y1, y2);
        final int xSpan = 1;
        final int ySpan = Math.abs(y2 - y1) + 1;
        final boolean isClear = false;
        fillRectInClip_raw(x, y, xSpan, ySpan, isClear);
    }

    private void fillRectInClip_raw(
            int x, int y, int xSpan, int ySpan,
            boolean mustUseOpaqueColor) {
        if ((xSpan <= 0) || (ySpan <= 0)) {
            // Not wasting time looping on some coordinate.
            return;
        }
        
        final GTransform transform = this.getTransform();
        final GRotation rotation = transform.rotation();
        
        final int xInBase = transform.minXIn1(x, y, xSpan, ySpan);
        final int yInBase = transform.minYIn1(x, y, xSpan, ySpan);
        final int xSpanInBase = rotation.xSpanInOther(xSpan, ySpan);
        final int ySpanInBase = rotation.ySpanInOther(xSpan, ySpan);
        
        /*
         * Taking care to loop on lines first
         * (true lines, i.e. in base coordinates,
         * which might be verticals in user coordinates).
         */
        
        final int scanlineStride = this.pixelArrScanlineStride;
        final int color32;
        if (mustUseOpaqueColor) {
            color32 = this.arrColorOpaque;
        } else {
            color32 = this.arrColor;
        }

        // Hack: "-1" so that we can do "++index" instead of "index++".
        int index = yInBase * scanlineStride + xInBase - 1;

        // To optimize for the case where xSpanInBase is small.
        final int indexJump = scanlineStride - xSpanInBase;

        final int alpha8 = this.getArrayColorAlpha8(color32);
        if (alpha8 == 0xFF) {
            /*
             * Optimization for fully opaque colors.
             */
            for (int j = 0; j < ySpanInBase; j++) {
                for (int i = 0; i < xSpanInBase; i++) {
                    this.pixelArr[++index] = color32;
                }
                index += indexJump;
            }
        } else {
            for (int j = 0; j < ySpanInBase; j++) {
                for (int i = 0; i < xSpanInBase; i++) {
                    this.blendColor32(++index, color32);
                }
                index += indexJump;
            }
        }
    }
}
