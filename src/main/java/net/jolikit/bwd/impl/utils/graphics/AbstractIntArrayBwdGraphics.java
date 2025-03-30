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
package net.jolikit.bwd.impl.utils.graphics;

import java.util.Arrays;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
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
        public boolean isColorOpaque() {
            return Argb32.isOpaque(getArgb32());
        }
        @Override
        public void drawPointInClip(int x, int y) {
            drawPointInClip_raw(x, y);
        }
        @Override
        public int drawHorizontalLineInClip(
                int x1, int x2, int y,
                int factor, short pattern, int pixelNum) {
            if (GprimUtils.mustComputePixelNum(pattern)) {
                return super.drawHorizontalLineInClip(
                        x1, x2, y,
                        factor, pattern, pixelNum);
            } else {
                drawHorizontalLineInClip_raw(x1, x2, y);
                return pixelNum;
            }
        }
        @Override
        public int drawVerticalLineInClip(
                int x, int y1, int y2,
                int factor, short pattern, int pixelNum) {
            if (GprimUtils.mustComputePixelNum(pattern)) {
                return super.drawVerticalLineInClip(
                        x, y1, y2,
                        factor, pattern, pixelNum);
            } else {
                drawVerticalLineInClip_raw(x, y1, y2);
                return pixelNum;
            }
        }
        /**
         * @param areHorVerFlipped We ignore that, we always do the right thing.
         */
        @Override
        public void fillRectInClip(
                int x, int y, int xSpan, int ySpan,
                boolean areHorVerFlipped) {
            final boolean mustUseOpaqueColor = false;
            final boolean mustSetColor = false;
            fillRectInClip_raw(
                    x, y, xSpan, ySpan,
                    mustUseOpaqueColor,
                    mustSetColor);
        }
    }
    
    /*
     * 
     */
    
    private class MyImgSrcPixels implements InterfaceSrcPixels {
        private InterfaceBwdImage image;
        private Object imageDataAccessor;
        public MyImgSrcPixels() {
        }
        public void configure(
            InterfaceBwdImage image,
            Object imageDataAccessor) {
            this.image = image;
            this.imageDataAccessor = imageDataAccessor;
        }
        @Override
        public GRect getRect() {
            return this.image.getRect();
        }
        @Override
        public int[] color32Arr() {
            return null;
        }
        @Override
        public int getScanlineStride() {
            return this.getRect().xSpan();
        }
        @Override
        public int getColor32At(int x, int y) {
            return getImageColor32(this.image, this.imageDataAccessor, x, y);
        }
    }

    private class MyRowDrawer implements InterfaceRowDrawer {
        public MyRowDrawer() {
        }
        @Override
        public void drawRow(
            int[] rowArr,
            int rowOffset,
            int dstX,
            int dstY,
            int length) {
            drawRowImpl(
                rowArr,
                rowOffset,
                dstX,
                dstY,
                length);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MyPrimitives primitives = new MyPrimitives();
    
    private final boolean isImageGraphics;
    
    /**
     * Pixels corresponding to the base area,
     * which top-left corner is rootBoxTopLeft.
     * index = (y - rootBoxTopLeft.y()) * pixelArrScanlineStride
     *         + (x - rootBoxTopLeft.x()).
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
    
    /*
     * Optimization, to reduce per-pixel conversion overhead.
     */
    
    /**
     * Transform between array coordinates (frame 1)
     * (in which rootBoxTopLeft corresponds to pixelArr[0])
     * and user coordinates (frame 2).
     */
    private GTransform transformArrToUser;
    
    /*
     * 
     */
    
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
     * 
     */
    
    private final MyRowDrawer rowDrawer = new MyRowDrawer();
    
    /*
     * temps
     */
    
    private final MyImgSrcPixels tmpImgSrcPixels = new MyImgSrcPixels();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param isImageGraphics True if it is an image graphics,
     *        false if it is a client graphics.
     */
    public AbstractIntArrayBwdGraphics(
            InterfaceBwdBindingImpl binding,
            GPoint rootBoxTopLeft,
            GRect box,
            GRect initialClip,
            //
            boolean isImageGraphics,
            int[] pixelArr,
            int pixelArrScanlineStride) {
        super(
                binding,
                rootBoxTopLeft,
                box,
                initialClip);
        
        this.isImageGraphics = isImageGraphics;
        
        this.pixelArr = LangUtils.requireNonNull(pixelArr);
        this.pixelArrScanlineStride = pixelArrScanlineStride;
        
        this.updateTransformArrToUser();
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
    
    public GTransform getTransformArrToUser() {
        return this.transformArrToUser;
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
        
        final boolean mustUseOpaqueColor = !this.isImageGraphics;
        final boolean mustSetColor = this.isImageGraphics;
        fillRectInClip_raw(
                clippedX,
                clippedY,
                clippedXSpan,
                clippedYSpan,
                mustUseOpaqueColor,
                mustSetColor);
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
        
        final InterfaceBwdFont font = this.getFont();
        if (font.isDisposed()) {
            throw new IllegalStateException("current font is disposed: " + font);
        }
        
        final InterfaceBwdFontMetrics metrics = font.metrics();
        
        final int theoTextWidth = metrics.computeTextWidth(text);
        final int theoTextHeight = metrics.height();
        
        if (theoTextHeight <= 0) {
            /*
             * Early abort if font height is zero.
             * We don't abort on zero theoretical text width, because fonts are
             * such a mess that it might be 0 even when the font actually has
             * something do draw.
             */
            return;
        }
        
        final GRect maxTextRectInText = this.computeMaxTextRectInText(
                theoTextWidth,
                theoTextHeight);
        final GRect maxTextRectInUser = maxTextRectInText.withPosDeltas(x, y);
        
        final GRect clipInUser = this.getClipInUser();
        
        /*
         * Coordinates rework based on clipping.
         */
        
        final GRect maxClippedTextRectInUser = maxTextRectInUser.intersected(clipInUser);
        if (maxClippedTextRectInUser.isEmpty()) {
            // All would be drawn out of clip,
            // and getClippedTextDataAccessor(...)
            // requires non-empty rectangle.
            return;
        }
        
        final GRect maxClippedTextRectInText =
                maxClippedTextRectInUser.withPosDeltas(-x, -y);
        
        final Object accessor = this.getClippedTextDataAccessor(
                text,
                maxClippedTextRectInText);
        try {
            final GRect renderedClippedTextRectInText =
                    this.getRenderedClippedTextRectInText(accessor);
            GRect renderedClippedTextRectInUser;
            if (renderedClippedTextRectInText.equals(maxClippedTextRectInText)) {
                // Rendered on whole max rect.
                renderedClippedTextRectInUser = maxClippedTextRectInUser;
            } else {
                // Rendered on (less) than max rect.
                // From text to user coordinates.
                renderedClippedTextRectInUser =
                        renderedClippedTextRectInText.withPosDeltas(x, y);
                if (renderedClippedTextRectInUser.isEmpty()) {
                    return;
                }
            }
            
            /*
             * No obvious gain from bothering to do row-major looping
             * in base coordinates (in case rotation is 90 or 270 degrees),
             * since then it would cause column-major looping
             * on input array which is row-major in user coordinates.
             */
            
            final int clippedWidth = renderedClippedTextRectInUser.xSpan();
            final int clippedHeight = renderedClippedTextRectInUser.ySpan();
            
            final GTransform transformArrToUser = this.transformArrToUser;
            
            for (int j = 0; j < clippedHeight; j++) {
                final int dstYInUser = renderedClippedTextRectInUser.y() + j;
                final int srcYInClippedText = j;

                for (int i = 0; i < clippedWidth; i++) {
                    final int dstXInUser = renderedClippedTextRectInUser.x() + i;
                    final int srcXInClippedText = i;

                    final int color32 = this.getTextColor32(
                            text,
                            accessor,
                            srcXInClippedText,
                            srcYInClippedText);
                    final int alpha8 = this.getArrayColorAlpha8(color32);
                    if (alpha8 == 0) {
                        /*
                         * Should be a quite common case for text,
                         * especially since we use enlarged rectangles,
                         * so worth to optimize this case away
                         * before coordinates conversions
                         * and messing with array.
                         */
                    } else {
                        final int xInArr = transformArrToUser.xIn1(dstXInUser, dstYInUser);
                        final int yInArr = transformArrToUser.yIn1(dstXInUser, dstYInUser);
                        final int dstIndex = this.toPixelArrIndexFromArr(xInArr, yInArr);

                        this.blendColor32(dstIndex, color32);
                    }
                }
            }
        } finally {
            this.disposeClippedTextDataAccessor(accessor);
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
            int index = this.toPixelArrIndexFromBase(
                xInBaseClipped,
                yInBaseClipped + j);
            for (int i = 0; i < xSpanInBaseClipped; i++) {
                final int color32From = pixelArr[index];
                pixelArr[index++] = toInvertedArrayColor32(color32From);
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
        
        final GTransform transformArrToUser = this.transformArrToUser;
        final int xInArr = transformArrToUser.xIn1(x, y);
        final int yInArr = transformArrToUser.yIn1(x, y);
        final int index = this.toPixelArrIndexFromArr(xInArr, yInArr);
        
        final int color32 = this.pixelArr[index];
        return this.getArgb32FromArrayColor32(color32);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected final boolean isImageGraphics() {
        return this.isImageGraphics;
    }

    @Override
    protected void setInternalTransform(GTransform newTransform) {
        super.setInternalTransform(newTransform);
        this.updateTransformArrToUser();
    }

    /*
     * 
     */
    
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
    protected void setBackingArgb(int argb32, BwdColor colorElseNull) {
        this.arrColor = this.getArrayColor32FromArgb32(argb32);
        this.arrColorOpaque = this.getArrayColor32FromArgb32(Argb32.toOpaque(argb32));
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
    protected GRect computeMaxTextRectInText(
            int theoTextWidth,
            int theoTextHeight) {
        
        final InterfaceBwdFont font = this.getFont();
        // Font heights can be quite far from font size,
        // so we use the max of both.
        final int magnitude = Math.max(
                font.size(),
                font.metrics().height());
        
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
            final InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan) {
        
        final GRect dstRectInUser = GRect.valueOf(x, y, xSpan, ySpan);
        final GRect clipInUser = this.getClipInUser();
        if (!clipInUser.overlaps(dstRectInUser)) {
            /*
             * Nothing to be drawn.
             */
            return;
        }
        
        final GRect srcRectInImg = GRect.valueOf(sx, sy, sxSpan, sySpan);
        
        final Object imageDataAccessor = this.getImageDataAccessor(image);
        try {
            final MyImgSrcPixels srcPixels = this.tmpImgSrcPixels;
            srcPixels.configure(image, imageDataAccessor);
            
            ScaledRectDrawing.drawScaledRect(
                this.getBinding().getInternalParallelizer(),
                this.getImageScalingType(),
                this.getArrayColorHelper(),
                //
                srcPixels,
                srcRectInImg,
                //
                dstRectInUser,
                clipInUser,
                this.rowDrawer);
        } finally {
            this.disposeImageDataAccessor(imageDataAccessor);
        }
    }
    
    /*
     * 
     */
    
    /**
     * @return The helper for array color.
     */
    protected abstract InterfaceColorTypeHelper getArrayColorHelper();
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The same color in the format to use in the array of pixels.
     */
    protected abstract int getArrayColor32FromArgb32(int argb32);
    
    /**
     * @param A color in the format to use in the array of pixels.
     * @return The same color in 32 bits ARGB format.
     */
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
     * @param text The text to draw.
     * @param maxClippedTextRectInText Rectangle expressed in text coordinates
     *        (i.e. (0,0) corresponding to (x,y) given to drawText() method).
     *        Pixels painted out of it won't be drawn.
     *        Passed here so as not to have to recompute it. Not empty.
     * @return An object from which clipped text data (pixels to draw, etc.)
     *         can be quickly retrieved.
     */
    protected abstract Object getClippedTextDataAccessor(
            String text,
            GRect maxClippedTextRectInText);

    /**
     * @param clippedTextDataAccessor An instance returned by
     *        getClippedTextDataAccessor().
     */
    protected abstract void disposeClippedTextDataAccessor(
            Object clippedTextDataAccessor);

    /**
     * @param clippedTextDataAccessor An instance returned by
     *        getClippedTextDataAccessor().
     * @return The rectangle containing rendered clipped text,
     *         relative to the (x,y) position given to drawText(...) method,
     *         i.e. with (0,0) position in rectangle corresponding to (x,y)
     *         in user coordinates.
     */
    protected abstract GRect getRenderedClippedTextRectInText(
            Object clippedTextDataAccessor);

    /**
     * This method returns a color, and not just a boolean indicating
     * whether there is text at the specified position, to allow for
     * anti-aliased text rendering, without which, with some backing libraries,
     * text can be quite ugly with small-sized fonts.
     * 
     * @param text The text itself.
     * @param clippedTextDataAccessor An instance returned by
     *        getClippedTextDataAccessor() for the specified text.
     * @param xInClippedText X coordinate in clipped text,
     *        i.e. in [0,clippedTextXSpan[.
     * @param yInClippedText Y coordinate in clipped text,
     *        i.e. in [0,clippedTextYSpan[.
     * @return The color to use for drawing the text pixel,
     *         typically an alpha-variant of current graphics color,
     *         or a fully transparent color if the pixel is not to be drawn.
     */
    protected abstract int getTextColor32(
            String text,
            Object clippedTextDataAccessor,
            int xInClippedText,
            int yInClippedText);

    /*
     * Images.
     */

    /**
     * The point of this method is to avoid superfluous internal object
     * retrieval for each pixel drawing, when image drawing is done
     * pixel by pixel.
     * 
     * @param image An image.
     * @return An object from which image data (pixels to draw, etc.)
     *         can be quickly retrieved.
     */
    protected abstract Object getImageDataAccessor(InterfaceBwdImage image);

    /**
     * @param imageDataAccessor An instance returned by
     *        getImageDataAccessor().
     */
    protected abstract void disposeImageDataAccessor(Object imageDataAccessor);
    
    /**
     * @param image The image itself.
     * @param imageDataAccessor An instance returned by
     *        getImageDataAccessor() for the specified image.
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
    
    private void drawRowImpl(
        int[] rowArr,
        int rowOffset,
        int dstX,
        int dstY,
        int length) {
        
        final GRotation rotation = this.transformArrToUser.rotation();
        // Optimization not to have to use transform for each pixel.
        final int xStepInArr = rotation.cos();
        final int yStepInArr = rotation.sin();
        
        int xInArr = this.transformArrToUser.xIn1(dstX, dstY);
        int yInArr = this.transformArrToUser.yIn1(dstX, dstY);
        for (int i = 0; i < length; i++) {
            final int color32 = rowArr[rowOffset + i];
            final int dstIndex = this.toPixelArrIndexFromArr(xInArr, yInArr);
            this.blendColor32(dstIndex, color32);
            xInArr += xStepInArr;
            yInArr += yStepInArr;
        }
    }

    private void updateTransformArrToUser() {
        final GPoint rootBoxTopLeft = this.getRootBoxTopLeft();
        final GTransform transform = this.getTransform_final();
        this.transformArrToUser =
            BindingCoordsUtils.computeTransformBoxToUser(
                rootBoxTopLeft,
                transform);
    }

    private int toPixelArrIndexFromBase(int xInBase, int yInBase) {
        final GPoint rootBoxTopLeft = this.getRootBoxTopLeft();
        final int xInArr = xInBase - rootBoxTopLeft.x();
        final int yInArr = yInBase - rootBoxTopLeft.y();
        return yInArr * this.pixelArrScanlineStride + xInArr;
    }

    private int toPixelArrIndexFromArr(int xInArr, int yInArr) {
        return yInArr * this.pixelArrScanlineStride + xInArr;
    }

    private void blendColor32(int index, int srcColor32) {
        final int dstColor32 = this.pixelArr[index];
        final int newColor32 = this.blendArrayColor32(srcColor32, dstColor32);
        this.pixelArr[index] = newColor32;
    }
    
    /*
     * 
     */
    
    private void drawPointInClip_raw(int x, int y) {
        final GTransform transformArrToUser = this.transformArrToUser;
        final int xInArr = transformArrToUser.xIn1(x, y);
        final int yInArr = transformArrToUser.yIn1(x, y);
        final int index = this.toPixelArrIndexFromArr(xInArr, yInArr);
        
        this.blendColor32(index, this.arrColor);
    }
    
    private void drawHorizontalLineInClip_raw(int x1, int x2, int y) {
        final int x = Math.min(x1, x2);
        final int xSpan = Math.abs(x2 - x1) + 1;
        final int ySpan = 1;
        final boolean mustUseOpaqueColor = false;
        final boolean mustSetColor = false;
        fillRectInClip_raw(x, y, xSpan, ySpan, mustUseOpaqueColor, mustSetColor);
    }
    
    private void drawVerticalLineInClip_raw(int x, int y1, int y2) {
        final int y = Math.min(y1, y2);
        final int xSpan = 1;
        final int ySpan = Math.abs(y2 - y1) + 1;
        final boolean mustUseOpaqueColor = false;
        final boolean mustSetColor = false;
        fillRectInClip_raw(x, y, xSpan, ySpan, mustUseOpaqueColor, mustSetColor);
    }

    /**
     * @param mustUseOpaqueColor If true, using a fully opaque version
     *        of current array color.
     * @param mustSetColor If true, uses COPY (or SRC) blending
     *        (replaces previous color with specified color)
     *        instead of SRC_OVER.
     */
    private void fillRectInClip_raw(
            int x, int y, int xSpan, int ySpan,
            boolean mustUseOpaqueColor,
            boolean mustSetColor) {
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

        int index = this.toPixelArrIndexFromBase(xInBase, yInBase);

        final boolean mustUseCopyAlgo =
            mustSetColor
            || (this.getArrayColorAlpha8(color32) == 0xFF);
        if (xSpanInBase == 1) {
            /*
             * Optimization for vertical lines
             * (can be like 4 times faster).
             */
            if (mustUseCopyAlgo) {
                fillRectInClip_raw_inBase_verticalLine_copy(
                    scanlineStride,
                    xSpanInBase,
                    ySpanInBase,
                    index,
                    color32);
            } else {
                fillRectInClip_raw_inBase_verticalLine_blend(
                    scanlineStride,
                    xSpanInBase,
                    ySpanInBase,
                    index,
                    color32);
            }
        } else {
            if (mustUseCopyAlgo) {
                fillRectInClip_raw_inBase_copy(
                    scanlineStride,
                    xSpanInBase,
                    ySpanInBase,
                    index,
                    color32);
            } else {
                fillRectInClip_raw_inBase_blend(
                    scanlineStride,
                    xSpanInBase,
                    ySpanInBase,
                    index,
                    color32);
            }
        }
    }
    
    private void fillRectInClip_raw_inBase_verticalLine_copy(
        int scanlineStride,
        int xSpanInBase,
        int ySpanInBase,
        int index,
        int color32) {
        final int indexJump = scanlineStride - xSpanInBase;
        for (int j = 0; j < ySpanInBase; j++) {
            this.pixelArr[index++] = color32;
            index += indexJump;
        }
    }
    
    private void fillRectInClip_raw_inBase_verticalLine_blend(
        int scanlineStride,
        int xSpanInBase,
        int ySpanInBase,
        int index,
        int color32) {
        final int indexJump = scanlineStride - xSpanInBase;
        for (int j = 0; j < ySpanInBase; j++) {
            this.blendColor32(index++, color32);
            index += indexJump;
        }
    }
    
    private void fillRectInClip_raw_inBase_copy(
        int scanlineStride,
        int xSpanInBase,
        int ySpanInBase,
        int index,
        int color32) {
        if (xSpanInBase == scanlineStride) {
            final int area = xSpanInBase * ySpanInBase;
            Arrays.fill(this.pixelArr, index, index + area, color32);
        } else {
            for (int j = 0; j < ySpanInBase; j++) {
                // Twice faster than a loop (when width not very small):
                // VM must optimize it.
                Arrays.fill(this.pixelArr, index, index + xSpanInBase, color32);
                index += scanlineStride;
            }
        }
    }
    
    private void fillRectInClip_raw_inBase_blend(
        int scanlineStride,
        int xSpanInBase,
        int ySpanInBase,
        int index,
        int color32) {
        final int indexJump = scanlineStride - xSpanInBase;
        for (int j = 0; j < ySpanInBase; j++) {
            for (int i = 0; i < xSpanInBase; i++) {
                this.blendColor32(index++, color32);
            }
            index += indexJump;
        }
    }
}
