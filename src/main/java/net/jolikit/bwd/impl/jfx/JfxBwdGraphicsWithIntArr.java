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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
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
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.graphics.InterfaceColorTypeHelper;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.ObjectWrapper;

/**
 * Graphics based on an int array.
 * This graphics addresses the blocking point that JavaFX GraphicsContext
 * requires a Canvas, which background is always opaque and as a result
 * doesn't allow for transparent offscreen images.
 * Almost all of its methods are also much, much faster than those
 * of GraphicsContext based graphics, the only exception being
 * its drawText() method, which has a large overhead (especially
 * compared to drawing small text on directly a GraphicsContext),
 * since it needs to draw text on an intermediary Canvas,
 * and then take a snapshot of it.
 */
public class JfxBwdGraphicsWithIntArr extends AbstractIntArrayBwdGraphics {

    /*
     * For making such an offscreen graphics,
     * instead of directly using an int array,
     * we could use a JavaFX WritableImage,
     * along with PixelReader and PixelWriter,
     * which would make it easier to draw it on a GC,
     * but it would be much slower in particular
     * for pixel wise operations, so we don't.
     * 
     * TODO jfx For line stipples, Shape.getStrokeDashArray() doesn't suit
     * our needs (but I don't remember why), so we use our default algorithm.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Clipped Text Data Accessor.
     */
    private static class MyCtda {
        final int[] argb32Arr;
        final int scanlineStride;
        final GRect rectInText;
        final int fgArgb32;
        public MyCtda(
                int[] argb32Arr,
                int scanlineStride,
                GRect rectInText,
                int fgArgb32) {
            this.argb32Arr = argb32Arr;
            this.scanlineStride = scanlineStride;
            this.rectInText = rectInText;
            this.fgArgb32 = fgArgb32;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Allows for sharper fonts, less antialiased-like,
     * TODO jfx For some reason it does not give consistent results across rotations
     * (seems to mostly affect text rotated 90 or 270 degrees, and does not much
     * on text rotated 0 or 180 degrees), so we keep it false.
     */
    private static final boolean MUST_USE_LCD_FONT_SMOOTHING_TYPE = false;
    
    private static final FontSmoothingType FONT_SMOOTHING_TYPE =
            (MUST_USE_LCD_FONT_SMOOTHING_TYPE ? FontSmoothingType.LCD : FontSmoothingType.GRAY);
    
    /**
     * Drawn text spans can change a lot during painting,
     * so shrinking could cause much GC.
     * These storages are tied to root graphics,
     * and will be garbaged once we are done with it.
     * Also, we don't know whether shrinking a canvas
     * actually shrinks its backing storage.
     */
    private static final boolean ALLOW_TEXT_STORAGE_SHRINKING = false;

    /**
     * Half.
     */
    private static final double H = 0.5;
    
    /*
     * 
     */

    /**
     * Shared with children graphics, since can't draw in parallel.
     * 
     * Must not be null, but the held reference initially is.
     */
    private final ObjectWrapper<JfxSnapshotHelper> textSnapshotHelperLazyRef;
    
    /*
     * 
     */
    
    private boolean isWritableImageDirty = true;
    
    private WritableImage readOnlyWritableImage;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for root graphics.
     */
    public JfxBwdGraphicsWithIntArr(
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
            pixelArr,
            pixelArrScanlineStride,
            //
            new ObjectWrapper<JfxSnapshotHelper>());
    }

    /*
     * 
     */

    @Override
    public JfxBwdGraphicsWithIntArr newChildGraphics(
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
        
        return new JfxBwdGraphicsWithIntArr(
                this.getBinding(),
                this.getRootBoxTopLeft(),
                childBox,
                childInitialClip,
                //
                this.isImageGraphics(),
                this.getPixelArr(),
                this.getPixelArrScanlineStride(),
                //
                this.textSnapshotHelperLazyRef);
    }
    
    /*
     * 
     */
    
    @Override
    public JfxBwdFont getFont() {
        return (JfxBwdFont) super.getFont();
    }
    
    /*
     * 
     */
    
    @Override
    public void clearRect(int x, int y, int xSpan, int ySpan) {
        super.clearRect(x, y, xSpan, ySpan);
        
        this.isWritableImageDirty = true;
    }
    
    /*
     * 
     */

    @Override
    public void drawPoint(int x, int y) {
        super.drawPoint(x, y);
        
        this.isWritableImageDirty = true;
    }
    
    /*
     * 
     */
    
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        super.drawLine(x1, y1, x2, y2);
        
        this.isWritableImageDirty = true;
    }

    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        final int ret = super.drawLineStipple(
                x1, y1, x2, y2,
                factor, pattern, pixelNum);
        
        this.isWritableImageDirty = true;
        
        return ret;
    }
    
    /*
     * 
     */

    @Override
    public void drawRect(int x, int y, int xSpan, int ySpan) {
        super.drawRect(x, y, xSpan, ySpan);
        
        this.isWritableImageDirty = true;
    }
    
    @Override
    public void fillRect(int x, int y, int xSpan, int ySpan) {
        super.fillRect(x, y, xSpan, ySpan);
        
        this.isWritableImageDirty = true;
    }

    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        super.drawOval(x, y, xSpan, ySpan);
        
        this.isWritableImageDirty = true;
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        super.fillOval(x, y, xSpan, ySpan);
        
        this.isWritableImageDirty = true;
    }
    
    /*
     * 
     */
    
    @Override
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        super.drawArc(
                x, y, xSpan, ySpan,
                startDeg, spanDeg);

        this.isWritableImageDirty = true;
    }
    
    @Override
    public void fillArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        super.fillArc(
                x, y, xSpan, ySpan,
                startDeg, spanDeg);

        this.isWritableImageDirty = true;
    }
    
    /*
     * 
     */
    
    @Override
    public void drawText(
            int x, int y,
            String text) {
        super.drawText(x, y, text);
        
        this.isWritableImageDirty = true;
    }
    
    /*
     * 
     */

    @Override
    public void flipColors(int x, int y, int xSpan, int ySpan) {
        super.flipColors(x, y, xSpan, ySpan);

        this.isWritableImageDirty = true;
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
        // Nothing to do.
    }
    
    /*
     * 
     */
    
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
    protected void drawImageImpl(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan) {
        super.drawImageImpl(
                x, y, xSpan, ySpan,
                image,
                sx, sy, sxSpan, sySpan);
        
        this.isWritableImageDirty = true;
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
        
        final int mcTextWidth = maxClippedTextRectInText.xSpan();
        final int mcTextHeight = maxClippedTextRectInText.ySpan();
        
        final JfxSnapshotHelper textSnapshotHelper = this.getTextSnapshotHelper(
                mcTextWidth,
                mcTextHeight);
        final Canvas textCanvas = textSnapshotHelper.getCanvas();
        
        /*
         * TODO jfx It doesn't seem possible to just clear the GC with
         * a transparent color, and then draw text on top of it: all pixels
         * always end up with 0xFF alpha.
         * What we do:
         * - On an offscreen GC, we draw white text on a black background,
         *   and then take a snapshot of it
         * - For each pixel, we convert the {r,g,b} components of the
         *   resulting color, into a floating-point into [0,1],
         *   and use it as a factor for alpha, to obtain the color to use
         *   from the user-specified color.
         * To use user color to draw text on the offscreen GC,
         * we would need to take care to use a different background,
         * but that would be hairy, in particular due to anti-aliasing.
         */
        
        final GraphicsContext textGc = textCanvas.getGraphicsContext2D();

        final int x0 = 0;
        final int y0 = 0;
        final GRotation rotation = GRotation.ROT_0;
        final double xShift = JfxUtils.computeXShiftInUser(rotation);
        final double yShift = JfxUtils.computeYShiftInUser(rotation);

        /*
         * Clearing BG.
         */
        
        {
            textGc.setFill(Color.BLACK);

            final double rectXShifted = x0 + xShift;
            final double rectYShifted = y0 + yShift;
            /*
             * NB: Might not work well if spans are huge
             * (cf. MUST_CLIP_RECTS_BEFORE_USING_BACKING_GRAPHICS
             * in other graphics implementation),
             * but text spans should not be huge so
             * we prefer to keep code simple.
             */
            fillRect_raw_shifted(
                    textGc,
                    rectXShifted,
                    rectYShifted,
                    mcTextWidth,
                    mcTextHeight);
        }

        /*
         * Drawing text.
         */
        
        {
            textGc.setFill(Color.WHITE);

            final JfxBwdFont fontImpl = this.getFont();
            textGc.setFont(fontImpl.getBackingFont());
            textGc.setFontSmoothingType(FONT_SMOOTHING_TYPE);

            final double textXShifted = xShift - maxClippedTextRectInText.x();
            final double textYShifted = yShift - maxClippedTextRectInText.y();
            
            final InterfaceBwdFontMetrics fontMetrics = fontImpl.metrics();
            drawText_raw_shifted(
                    textGc,
                    textXShifted,
                    textYShifted,
                    text,
                    rotation,
                    fontMetrics);
        }

        // Snapshot of the whole clipped area.
        textSnapshotHelper.takeSnapshot(0, 0, mcTextWidth, mcTextHeight);

        final int[] mcArgb32Arr = textSnapshotHelper.getArgb32Arr();
        final int scanlineStride = textSnapshotHelper.getScanlineStride();
        final int fgArgb32 = this.getArgb32();
        final MyCtda accessor = new MyCtda(
                mcArgb32Arr,
                scanlineStride,
                maxClippedTextRectInText,
                fgArgb32);

        return accessor;
    }

    @Override
    protected void disposeClippedTextDataAccessor(
            Object clippedTextDataAccessor) {
        // Nothing to do.
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
        
        final int index = yInClippedText * accessor.scanlineStride + xInClippedText;
        // White on black.
        final int textArgb32From = accessor.argb32Arr[index];
        
        if (textArgb32From == BwdColor.BLACK.toArgb32()) {
            // Not drawn upon.
            return 0x00000000;
        }

        final double tmpRedFp = Argb32.getRedFp(textArgb32From);
        final double tmpGreenFp = Argb32.getGreenFp(textArgb32From);
        final double tmpBlueFp = Argb32.getBlueFp(textArgb32From);

        final double alphaFactor = (tmpRedFp + tmpGreenFp + tmpBlueFp) * (1.0/3);
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

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final AbstractJfxBwdImage imageImpl = (AbstractJfxBwdImage) image;
        final int[] premulArgb32Arr = imageImpl.getPremulArgb32ArrElseNull();
        if (premulArgb32Arr != null) {
            return premulArgb32Arr;
        } else {
            /*
             * Only (very) slow in case of image
             * using an int array graphics,
             * which is not the case here.
             */
            return imageImpl.getBackingImageForGcDrawOrRead();
        }
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
        final int premulArgb32;
        if (imageDataAccessor instanceof int[]) {
            final int[] premulArgb32Arr = (int[]) imageDataAccessor;
            final int index = yInImage * image.getWidth() + xInImage;
            premulArgb32 = premulArgb32Arr[index];
        } else {
            final Image backingImage = (Image) imageDataAccessor;
            final int argb32 = backingImage.getPixelReader().getArgb(xInImage, yInImage);
            premulArgb32 = BindingColorUtils.toPremulAxyz32(argb32);
        }
        return premulArgb32;
    }
    
    /*
     * 
     */
    
    /**
     * Useful to draw a BWD writable image that uses this type of graphics,
     * on a GC, without having to re-create a JavaFX WritableImage
     * each time.
     * 
     * Can be called even if graphics finish() method has been called,
     * because it is used for drawing the image.
     * 
     * @return A snapshot of pixels corresponding to initial clip
     *         (which is same as box for root graphics),
     *         in base coordinates. Never modified once returned.
     */
    protected WritableImage getSnapshotOverInitialClip() {
        final WritableImage ret;
        if (this.isWritableImageDirty) {
            /*
             * Not box, i.e. same constraint as for
             * other pixel reading methods,
             * such as getArgb32At() etc.
             */
            final GRect initialClip = this.getInitialClipInBase();
            final int width = initialClip.xSpan();
            final int height = initialClip.ySpan();
            final WritableImage newWi = new WritableImage(width, height);
            final int[] premulArgb32Arr = this.getPixelArr();
            final int offset = 0;
            final int scanlineStride = width;
            newWi.getPixelWriter().setPixels(
                    initialClip.x(),
                    initialClip.y(),
                    width,
                    height,
                    JfxPaintUtils.FORMAT_INT_ARGB_PRE,
                    premulArgb32Arr,
                    offset,
                    scanlineStride);
            ret = newWi;
            this.readOnlyWritableImage = newWi;
            this.isWritableImageDirty = false;
        } else {
            ret = this.readOnlyWritableImage;
        }
        return ret;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private JfxBwdGraphicsWithIntArr(
        InterfaceBwdBindingImpl binding,
        GPoint rootBoxTopLeft,
        GRect box,
        GRect initialClip,
        //
        boolean isImageGraphics,
        int[] pixelArr,
        int pixelArrScanlineStride,
        //
        ObjectWrapper<JfxSnapshotHelper> textSnapshotHelperLazyRef) {
        super(
            binding,
            rootBoxTopLeft,
            box,
            initialClip,
            //
            isImageGraphics,
            pixelArr,
            pixelArrScanlineStride);
        this.textSnapshotHelperLazyRef = textSnapshotHelperLazyRef;
    }
    
    /*
     * 
     */
    
    /**
     * @return Helper to use, with canvas large enough for the specified spans.
     */
    private JfxSnapshotHelper getTextSnapshotHelper(
            int maxTextWidth,
            int maxTextHeight) {
        JfxSnapshotHelper textSnapshotHelper = this.textSnapshotHelperLazyRef.value;
        if (textSnapshotHelper == null) {
            final Canvas canvas = JfxUtils.newCanvas(maxTextWidth, maxTextHeight);
            textSnapshotHelper = new JfxSnapshotHelper(
                    canvas,
                    ALLOW_TEXT_STORAGE_SHRINKING);
            this.textSnapshotHelperLazyRef.value = textSnapshotHelper;
        }
        
        updateTextCanvasSize(
                textSnapshotHelper.getCanvas(),
                maxTextWidth,
                maxTextHeight);
        
        return textSnapshotHelper;
    }
    
    private static void updateTextCanvasSize(
            Canvas textCanvas,
            int minWidth,
            int minHeight) {
        
        final int oldStorageWidth = (int) textCanvas.getWidth();
        final int oldStorageHeight = (int) textCanvas.getHeight();

        final int newStorageWidth =
                BindingBasicsUtils.computeStorageSpan(
                        oldStorageWidth,
                        minWidth,
                        ALLOW_TEXT_STORAGE_SHRINKING);
        final int newStorageHeight =
                BindingBasicsUtils.computeStorageSpan(
                        oldStorageHeight,
                        minHeight,
                        ALLOW_TEXT_STORAGE_SHRINKING);

        if (newStorageWidth != oldStorageWidth) {
            textCanvas.setWidth(newStorageWidth);
        }
        if (newStorageHeight != oldStorageHeight) {
            textCanvas.setHeight(newStorageHeight);
        }
    }

    /**
     * Rotation-dependent shifts must have bee done already.
     */
    private static void fillRect_raw_shifted(
            GraphicsContext gc,
            double xShifted, double yShifted,
            int xSpan, int ySpan) {
        // Need -H rework, because we want to end up at pixels limits.
        final double _x = xShifted - H;
        final double _y = yShifted - H;
        gc.fillRect(_x, _y, xSpan, ySpan);
    }

    /**
     * Rotation-dependent shifts must have bee done already.
     */
    private static void drawText_raw_shifted(
            GraphicsContext gc,
            double xShifted,
            double yShifted,
            String text,
            GRotation rotation,
            InterfaceBwdFontMetrics fontMetrics) {
        final int ascent = fontMetrics.ascent();
        final double dx;
        final double dy;
        if (JfxUtils.H_IN_T) {
            /*
             * TODO jfx For some reason we need to hack a bit depending on a lot
             * of stuffs to end up with the text being displayed about at the
             * same place as AWT text.
             */
            if (MUST_USE_LCD_FONT_SMOOTHING_TYPE) {
                dx = -H;
                dy = -H + ((rotation.angDeg() == 270) ? -1.0 : 0.0);
            } else {
                dx = -H + ((rotation.angDeg() == 90) ? -1.0 : 0.0);
                dy = -H + ((rotation.angDeg() == 270) ? -1.0 : 0.0);
            }
        } else {
            dx = -H;
            dy = -H;
        }
        final double _x = xShifted + dx;
        final double _y = yShifted + dy + ascent;
        gc.fillText(text, _x, _y);
    }
}
