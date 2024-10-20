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

import java.lang.reflect.Field;
import java.util.LinkedList;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.gprim.GprimUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.AbstractBwdPrimitives;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.RethrowException;

/**
 * Graphics based on a JavaFX GraphicsContext.
 * Not too bad, other that doesn't allow for transparent offscreen images
 * (Canvas background always opaque).
 */
public class JfxBwdGraphicsWithGc extends AbstractBwdGraphics {

    /*
     * TODO jfx For line stipples, Shape.getStrokeDashArray() doesn't suit
     * our needs (but I don't remember why), so we use our default algorithm.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * True to use backing graphics when somehow relevant, to see how well
     * we can do with backing graphics.
     * False to be as pixel-consistent as possible with bindings using
     * our redefined primitives.
     */
    private static final boolean MUST_USE_BACKING_GRAPHICS_METHODS = false;
    
    /**
     * TODO jfx Can't rely on backing clipping:
     * if filling huge rectangles with coordinates ranges
     * like (0, Integer.MAX_VALUE/2), for some reason the origin
     * of the actually filled area is a few pixels away from (0,0)
     * (the larger the spans, the further away).
     * This might be due to JavaFX casting double coordinates
     * into floats internally, loosing accuracy for large ints.
     * To avoid this issue, we clip rectangles before using
     * backing treatments.
     */
    private static final boolean MUST_CLIP_RECTS_BEFORE_USING_BACKING_GRAPHICS = true;
    
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
            drawPoint_raw(x, y);
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
                drawLine_raw(x1, y, x2, y);
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
                drawLine_raw(x, y1, x, y2);
                return pixelNum;
            }
        }
        @Override
        public int drawGeneralLineInClip(
                int x1, int y1, int x2, int y2,
                int factor, short pattern, int pixelNum) {
            if (GprimUtils.mustComputePixelNum(pattern)) {
                return super.drawGeneralLineInClip(
                        x1, y1, x2, y2,
                        factor, pattern, pixelNum);
            } else {
                drawLine_raw(x1, y1, x2, y2);
                return pixelNum;
            }
        }
        @Override
        public void fillRectInClip(
                int x, int y, int xSpan, int ySpan,
                boolean areHorVerFlipped) {
            fillRect_raw(x, y, xSpan, ySpan);
        }
        @Override
        public void drawPoint(GRect clip, int x, int y) {
            // Relying on backing clipping.
            drawPoint_raw(x, y);
        }
        @Override
        public void drawRect(
                GRect clip,
                int x, int y, int xSpan, int ySpan) {
            if (MUST_CLIP_RECTS_BEFORE_USING_BACKING_GRAPHICS) {
                super.drawRect(clip, x, y, xSpan, ySpan);
            } else {
                // Relying on backing clipping.
                drawRect_raw(x, y, xSpan, ySpan);
            }
        }
        @Override
        public void fillRect(
                GRect clip,
                int x, int y, int xSpan, int ySpan,
                boolean areHorVerFlipped) {
            if (MUST_CLIP_RECTS_BEFORE_USING_BACKING_GRAPHICS) {
                super.fillRect(clip, x, y, xSpan, ySpan, areHorVerFlipped);
            } else {
                // Relying on backing clipping.
                fillRect_raw(x, y, xSpan, ySpan);
            }
        }
    }
    
    /**
     * Resources shared between a root graphics and all its descendants.
     */
    private static class MyShared {
        final GraphicsContext gc;
        final int gcScale;
        final JfxImgDrawingUtils imgDrawingUtils;
        final JfxDirtySnapshotHelper dirtySnapshotHelper;
        /**
         * Since multiple graphics are allowed to be in use at once
         * (as long as used from UI thread), that means that we must keep track
         * of which graphic is "active" (being used), and configure the gc
         * appropriately on the fly.
         * 
         * This reference holds the currently "active" (last used) graphics,
         * among root graphics and its descendants.
         */
        JfxBwdGraphicsWithGc gcCurrentGraphics = null;
        private int graphicsCount = 0;
        private boolean initialStateStored = false;
        private int finishCount = 0;
        public MyShared(
            GraphicsContext gc,
            int gcScale,
            JfxImgDrawingUtils imgDrawingUtils,
            JfxDirtySnapshotHelper dirtySnapshotHelper) {
            this.gc = gc;
            this.gcScale = gcScale;
            this.imgDrawingUtils = imgDrawingUtils;
            this.dirtySnapshotHelper = dirtySnapshotHelper;
        }
        public void onGraphicsCreation() {
            this.graphicsCount++;
        }
        public void onGraphicsInit() {
            if (!this.initialStateStored) {
                /*
                 * Saving state prior to clip and transform setting.
                 * We revert back to this state in case of clip change
                 * (and in case of transform change also since we use
                 * transformed clip), and then re-apply transform and
                 * apply new transform clip, because there seem to be
                 * no other way to replace clipping (like a clearClip()
                 * method).
                 */
                this.gc.save();
                this.initialStateStored = true;
            }
        }
        public void onGraphicsFinish() {
            this.finishCount++;
            final boolean isLastFinish =
                (this.finishCount == this.graphicsCount);
            if (isLastFinish) {
                if (this.initialStateStored) {
                    /*
                     * Popping state, else memory leak in the backing graphics
                     * (and also restoring before-init state as a side effect).
                     */
                    this.gc.restore();
                }
            }
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
     * Half.
     */
    private static final double H = 0.5;
    
    /*
     * 
     */

    private final MyPrimitives primitives = new MyPrimitives();
    
    private final MyShared shared;
    
    /*
     * 
     */
    
    /**
     * Lazily computed.
     */
    private Color backingColorOpaque = null;
    
    /*
     * Common adjustments for some methods.
     */
    
    private double xShiftInUser;
    private double yShiftInUser;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for root graphics.
     * 
     * @param gcScale Scale used for transforms and clips
     *        set into the backing graphics.
     * @param dirtySnapshotHelper Must not be null.
     */
    public JfxBwdGraphicsWithGc(
        InterfaceBwdBindingImpl binding,
        GRect box,
        //
        GraphicsContext gc,
        int gcScale,
        JfxImgDrawingUtils imgDrawingUtils,
        JfxDirtySnapshotHelper dirtySnapshotHelper) {
        this(
            binding,
            topLeftOf(box),
            box,
            box, // initialClip
            //
            new MyShared(
                gc,
                gcScale,
                imgDrawingUtils,
                dirtySnapshotHelper));
    }

    /**
     * Take care to restore its current state after usage,
     * and before usage of methods of this object.
     * 
     * @return The backing GraphicsContext.
     */
    public GraphicsContext getBackingGraphics() {
        return this.shared.gc;
    }

    /*
     * 
     */

    @Override
    public JfxBwdGraphicsWithGc newChildGraphics(
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
        
        return new JfxBwdGraphicsWithGc(
                this.getBinding(),
                this.getRootBoxTopLeft(),
                childBox,
                childInitialClip,
                //
                this.shared);
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
        this.checkUsable();
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        /*
         * Implicit dirtySnapshotHelper calls.
         * 
         * NB: Can't clear with a transparent color with GC,
         * so even in case of image graphics, we use this
         * opaque clearing method (that's why we don't have
         * an "isImageGraphics" field).
         */

        final Color backingColor = (Color) gc.getStroke();

        Color backingColorOpaque = this.backingColorOpaque;
        if (backingColorOpaque == null) {
            final long argb64 = this.getArgb64();
            if (Argb64.isOpaque(argb64)) {
                backingColorOpaque = backingColor;
            } else {
                backingColorOpaque = JfxUtils.newColor(Argb64.toOpaque(argb64));
            }
            this.backingColorOpaque = backingColorOpaque;
        }

        gc.setFill(backingColorOpaque);
        try {
            this.fillRect(x, y, xSpan, ySpan);
        } finally {
            gc.setFill(backingColor);
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawPoint(int x, int y) {
        super.drawPoint(x, y);
        
        this.shared.dirtySnapshotHelper.onPointDrawing(
            this.getTransform(), x, y);
    }

    /*
     * 
     */
    
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        super.drawLine(x1, y1, x2, y2);
        
        this.shared.dirtySnapshotHelper.onLineDrawing(
            this.getTransform(), x1, y1, x2, y2);
    }

    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        final int ret = super.drawLineStipple(
                x1, y1, x2, y2,
                factor, pattern, pixelNum);
        
        this.shared.dirtySnapshotHelper.onLineDrawing(
            this.getTransform(), x1, y1, x2, y2);
        
        return ret;
    }

    /*
     * 
     */

    @Override
    public void drawRect(int x, int y, int xSpan, int ySpan) {
        super.drawRect(x, y, xSpan, ySpan);

        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillRect(int x, int y, int xSpan, int ySpan) {
        super.fillRect(x, y, xSpan, ySpan);

        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }

    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final double _x = x + this.xShiftInUser;
                final double _y = y + this.yShiftInUser;
                gc.strokeOval(_x, _y, xSpan - 1, ySpan - 1);
            }
        } else {
            super.drawOval(x, y, xSpan, ySpan);
        }
        
        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                // Need -H rework, because we want to end up at pixels limits.
                final double _x = x + this.xShiftInUser - H;
                final double _y = y + this.yShiftInUser - H;
                gc.fillOval(_x, _y, xSpan, ySpan);
            }
        } else {
            super.fillOval(x, y, xSpan, ySpan);
        }
        
        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    /*
     * 
     */
    
    @Override
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final double _x = x + this.xShiftInUser;
                final double _y = y + this.yShiftInUser;
                gc.strokeArc(
                        _x, _y, xSpan - 1, ySpan - 1,
                        startDeg, spanDeg,
                        ArcType.OPEN);
            }
        } else {
            super.drawArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
        
        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                // Need -H rework, because we want to end up at pixels limits.
                final double _x = x + this.xShiftInUser - H;
                final double _y = y + this.yShiftInUser - H;
                gc.fillArc(
                        _x, _y, xSpan, ySpan,
                        startDeg, spanDeg,
                        ArcType.ROUND);
            }
        } else {
            super.fillArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
        
        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    /*
     * 
     */
    
    @Override
    public void drawPolyline(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            if (pointCount > 0) {
                final double[] xdArr = new double[pointCount];
                final double[] ydArr = new double[pointCount];
                for (int i = 0; i < pointCount; i++) {
                    xdArr[i] = xArr[i] + this.xShiftInUser;
                    ydArr[i] = yArr[i] + this.yShiftInUser;
                }
                gc.strokePolyline(xdArr, ydArr, pointCount);
            }
        } else {
            super.drawPolyline(xArr, yArr, pointCount);
        }
        
        final GRect bbox = GprimUtils.computePolyBoundingBox(xArr, yArr, pointCount);
        this.shared.dirtySnapshotHelper.onRectDrawing(
                this.getTransform(),
                bbox.x(), bbox.y(), bbox.xSpan(), bbox.ySpan());
    }

    @Override
    public void drawPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            if (pointCount > 0) {
                final double[] xdArr = new double[pointCount];
                final double[] ydArr = new double[pointCount];
                for (int i = 0; i < pointCount; i++) {
                    xdArr[i] = xArr[i] + this.xShiftInUser;
                    ydArr[i] = yArr[i] + this.yShiftInUser;
                }
                gc.strokePolygon(xdArr, ydArr, pointCount);
            }
        } else {
            super.drawPolygon(xArr, yArr, pointCount);
        }
        
        final GRect bbox = GprimUtils.computePolyBoundingBox(xArr, yArr, pointCount);
        this.shared.dirtySnapshotHelper.onRectDrawing(
                this.getTransform(),
                bbox.x(), bbox.y(), bbox.xSpan(), bbox.ySpan());
    }

    @Override
    public void fillPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            if (pointCount > 0) {
                final double[] xdArr = new double[pointCount];
                final double[] ydArr = new double[pointCount];
                for (int i = 0; i < pointCount; i++) {
                    xdArr[i] = xArr[i] + this.xShiftInUser;
                    ydArr[i] = yArr[i] + this.yShiftInUser;
                }
                gc.fillPolygon(xdArr, ydArr, pointCount);
            }
        } else {
            super.fillPolygon(xArr, yArr, pointCount);
        }
        
        final GRect bbox = GprimUtils.computePolyBoundingBox(xArr, yArr, pointCount);
        this.shared.dirtySnapshotHelper.onRectDrawing(
                this.getTransform(),
                bbox.x(), bbox.y(), bbox.xSpan(), bbox.ySpan());
    }

    /*
     * 
     */
    
    @Override
    public void drawText(
            int x, int y,
            String text) {
        this.checkUsable();
        LangUtils.requireNonNull(text);

        final AbstractBwdFont<?> font = this.getFont();
        if (font.isDisposed()) {
            throw new IllegalStateException("font is disposed: " + font);
        }
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        final double _x = x + this.xShiftInUser;
        final double _y = y + this.yShiftInUser;
        final GRotation rotation = this.getTransform().rotation();
        final InterfaceBwdFontMetrics fontMetrics = font.metrics();
        drawText_raw_shifted(gc, _x, _y, text, rotation, fontMetrics);
        
        this.shared.dirtySnapshotHelper.onTextDrawing(
            this.getTransform(), x, y, text, font);
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

        /*
         * TODO jfx Could also use Canvas.snapshot(...)
         * and then draw reworked colors,
         * but using blend mode is simpler and works fine.
         */
        
        final GraphicsContext gc = this.getConfiguredGc();
        
        final Color c = (Color) gc.getStroke();
        
        final Color xorColor = Color.WHITE;
        gc.setFill(xorColor);
        gc.setGlobalBlendMode(BlendMode.DIFFERENCE);
        try {
            this.fillRect(x, y, xSpan, ySpan);
        } finally {
            gc.setFill(c);
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        }
        
        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }

    /*
     * 
     */

    @Override
    public int getArgb32At(int x, int y) {
        // For usability and coordinates check, and default result.
        final int defaultRes = super.getArgb32At(x, y);
        
        final GTransform transform = this.getTransform();
        final int xInBase = transform.xIn1(x, y);
        final int yInBase = transform.yIn1(x, y);
        
        this.shared.dirtySnapshotHelper.beforePixelReading(xInBase, yInBase);
        final GRect snapshotBox = this.shared.dirtySnapshotHelper.getSnapshotBox();
        if (!snapshotBox.contains(xInBase, yInBase)) {
            return defaultRes;
        }
        
        final int[] snapshotPremulArgb32Arr =
            this.shared.dirtySnapshotHelper.getSnapshotPremulArgb32Arr();
        final int snapshotScanlineStride =
            this.shared.dirtySnapshotHelper.getSnapshotScanlineStride();
        
        final int index = yInBase * snapshotScanlineStride + xInBase;
        final int premulArgb32 = snapshotPremulArgb32Arr[index];
        final int argb32 = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
        return argb32;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void initImpl() {
        if (DEBUG) {
            printClipStack(this.shared.gc, "before initImpl()");
        }

        this.shared.onGraphicsInit();
        
        super.initImpl();
    }
    
    @Override
    protected void finishImpl() {
        this.shared.onGraphicsFinish();
        
        if (DEBUG) {
            printClipStack(this.shared.gc, "after finishImpl()");
        }
    }
    
    @Override
    protected void finishWithoutInitImpl() {
        this.shared.onGraphicsFinish();
        
        if (DEBUG) {
            printClipStack(this.shared.gc, "after finishWithoutInitImpl()");
        }
    }
    
    /*
     * 
     */

    @Override
    protected void setBackingClip(GRect clipInBase) {
        this.setBackingClipAndTransformToCurrent();
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        this.setBackingClipAndTransformToCurrent();
    }

    @Override
    protected void setBackingArgb(int argb32, BwdColor colorElseNull) {
        
        final GraphicsContext gc = this.shared.gc;
        
        final Color color;
        if (colorElseNull != null) {
            color = JfxUtils.newColor(colorElseNull.toArgb64());
        } else {
            color = JfxUtils.newColor(argb32);
        }

        gc.setFill(color);
        gc.setStroke(color);
        
        /*
         * Computed lazily (rarely used).
         */
        this.backingColorOpaque = null;
    }

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        final JfxBwdFont fontImpl = (JfxBwdFont) font;
        this.setFontAndFontSmoothingType(fontImpl.getBackingFont());
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
        
        final GraphicsContext gc = this.shared.gc;
        
        final Color oldBackingColor = (Color) gc.getStroke();
        
        final Font oldBackingFont = gc.getFont();
        
        boolean didResetBackingState = false;
        if (mustSetClip || mustSetTransform) {
            this.resetBackingStateAndSetClipAndTransformToCurrent();
            didResetBackingState = true;
        }
        
        if (mustSetColor) {
            this.setBackingArgb(argb32, colorElseNull);
        } else {
            if (didResetBackingState) {
                // Restoring backing color after reset.
                gc.setFill(oldBackingColor);
                gc.setStroke(oldBackingColor);
            }
        }
        
        if (mustSetFont) {
            this.setBackingFont(font);
        } else {
            if (didResetBackingState) {
                // Restoring backing font after reset.
                this.setFontAndFontSmoothingType(oldBackingFont);
            }
        }
    }

    /*
     * 
     */

    @Override
    protected AbstractBwdPrimitives getPrimitives() {
        return this.primitives;
    }
    
    /*
     * Images.
     */
    
    @Override
    protected void drawImageImpl(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan) {

        final GraphicsContext gc = this.getConfiguredGc();
        
        final AbstractJfxBwdImage imageImpl = (AbstractJfxBwdImage) image;
        final Image img = imageImpl.getBackingImageForGcDrawOrRead();
        
        // Only for destination box, not source.
        final double _x = x + this.xShiftInUser - H;
        final double _y = y + this.yShiftInUser - H;

        this.shared.imgDrawingUtils.drawBackingImageOnGc(
            _x, _y, xSpan, ySpan,
            //
            img,
            //
            sx, sy, sxSpan, sySpan,
            //
            this.getBinding().getInternalParallelizer(),
            this.getImageScalingType(),
            this.getBindingConfig().getMustUseBackingImageScalingIfApplicable(),
            //
            gc);
        
        this.shared.dirtySnapshotHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    JfxDirtySnapshotHelper getDirtySnapshotHelper() {
        return this.shared.dirtySnapshotHelper;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JfxBwdGraphicsWithGc(
        InterfaceBwdBindingImpl binding,
        GPoint rootBoxTopLeft,
        GRect box,
        GRect initialClip,
        //
        MyShared shared) {
        super(
            binding,
            rootBoxTopLeft,
            box,
            initialClip);
        
        // Implicit null check.
        shared.onGraphicsCreation();
        
        this.shared = shared;
    }
    
    /*
     * 
     */
    
    /**
     * Method to use to retrieve gc for writing or reading pixels.
     * 
     * We use the pattern of retrieving the gc from this method each time,
     * even though it's always the same and accessible as a field,
     * because otherwise we might forget to call
     * ensureGcConfiguredForThisGraphics()
     * before using this.gc.
     * 
     * @return The shared gc aligned on this graphics configuration,
     *         ready to be used for writing or reading pixels.
     */
    private GraphicsContext getConfiguredGc() {
        this.ensureGcConfiguredForThisGraphics();
        return this.shared.gc;
    }
    
    private void ensureGcConfiguredForThisGraphics() {
        final JfxBwdGraphicsWithGc gcGraphics = this.shared.gcCurrentGraphics;
        if (gcGraphics != this) {
            this.configureGcForThisGraphics();
            
            /*
             * Set after configuration, to have stack overflow
             * if trying to configure it while already being
             * configuring it.
             */
            this.shared.gcCurrentGraphics = this;
        }
    }
    
    private void configureGcForThisGraphics() {
        final boolean mustSetClip = true;
        final boolean mustSetTransform = true;
        final boolean mustSetColor = true;
        final boolean mustSetFont = true;
        // No need because we only use 32 bits accuracy.
        final BwdColor colorElseNull = null;
        this.setBackingState(
                mustSetClip,
                this.getClipInBase(),
                //
                mustSetTransform,
                this.getTransform(),
                //
                mustSetColor,
                this.getArgb32(),
                colorElseNull,
                //
                mustSetFont,
                this.getFont());
    }

    /*
     * 
     */
    
    private void setFontAndFontSmoothingType(Font font) {
        final GraphicsContext gc = this.shared.gc;
        
        gc.setFont(font);
        gc.setFontSmoothingType(FONT_SMOOTHING_TYPE);
    }

    private void setBackingClipAndTransformToCurrent() {
        
        final GraphicsContext gc = this.shared.gc;
        
        // Works whether we are in XOR mode or not,
        // unlike getStroke().
        final Color currentBackingColor = (Color) gc.getStroke();
        
        final Font currentBackingFont = gc.getFont();
        
        this.resetBackingStateAndSetClipAndTransformToCurrent();

        // Restoring backing color after reset.
        gc.setFill(currentBackingColor);
        gc.setStroke(currentBackingColor);

        // Restoring backing font after reset.
        this.setFontAndFontSmoothingType(currentBackingFont);
    }

    /**
     * Resets all backing state to undefined initial state,
     * and then sets its clip and transform to current.
     * 
     * After this call, backing color and font are undefined.
     */
    private void resetBackingStateAndSetClipAndTransformToCurrent() {
        final GraphicsContext gc = this.shared.gc;
        
        // Restores initial (last saved) state (and pops it!!!).
        gc.restore();
        // Re-save restored initial state, so that we can restore it later
        // (with a restoreButDontPopState() method we would not have to do that).
        gc.save();
        
        this.addCurrentClipToBacking();
        
        this.addCurrentTransformToBacking();
    }
    
    /**
     * Supposes that current backing graphics state is its initial state,
     * i.e. before init() call.
     */
    private void addCurrentClipToBacking() {
        final GRect clip = this.getClipInBase();
        final int x = clip.x();
        final int y = clip.y();
        final int xSpan = clip.xSpan();
        final int ySpan = clip.ySpan();

        /*
         * Since we only have rectangular clips on pixels edges,
         * it triggers rectangular clip detection, cf.
         * https://javafx-jira.kenai.com/browse/RT-37300 ("[Canvas] Canvas needs a fast rectangular clip capabiity ")
         * (http://hg.openjdk.java.net/openjfx/8u-dev/rt/rev/ddf059995fc0)
         * Otherwise, clipping is AWFULLY slow,
         * and we would have to do it all in the binding.
         * 
         * Clip appears to be in non-transformed coordinates,
         * so we have to hard-code the base scaling/shifting here.
         * NB: Not to have to do that, we could instead apply
         * the clip defined in user, and after the backing transform
         * has been set.
         */

        final GraphicsContext gc = this.shared.gc;
        
        final int gcs = this.shared.gcScale;
        
        final GPoint rootBoxTopLeft = this.getRootBoxTopLeft();
        
        final int scaledX = (x * gcs - rootBoxTopLeft.x());
        final int scaledY = (y * gcs - rootBoxTopLeft.y());
        final int scaledXSpan = xSpan * gcs;
        final int scaledYSpan = ySpan * gcs;
        
        gc.beginPath();
        gc.moveTo(scaledX, scaledY);
        gc.lineTo(scaledX + scaledXSpan, scaledY);
        gc.lineTo(scaledX + scaledXSpan, scaledY + scaledYSpan);
        gc.lineTo(scaledX, scaledY + scaledYSpan);
        gc.closePath();
        gc.clip();
    }
    
    /**
     * Supposes that current backing graphics state is its initial state,
     * i.e. before init() call, except for clipping.
     */
    private void addCurrentTransformToBacking() {
        final GTransform transform = this.getTransform();
        
        final GRotation rotation = transform.rotation();
        
        final int gcs = this.shared.gcScale;
        
        final double myH = (JfxUtils.H_IN_T ? H : 0.0);
        
        final GPoint rootBoxTopLeft = this.getRootBoxTopLeft();
        
        // Concatenating our transform.
        final double mxt =
            transform.frame2XIn1() * gcs + myH
            - rootBoxTopLeft.x();
        final double myt =
            transform.frame2YIn1() * gcs + myH
            - rootBoxTopLeft.y();
        
        this.shared.gc.transform(
            rotation.cos() * gcs, // mxx (0,0)
            rotation.sin() * gcs, // myx (1,0)
            -rotation.sin() * gcs, // mxy (0,1)
            rotation.cos() * gcs, // myy (1,1)
            mxt,
            myt);
        
        this.xShiftInUser = JfxUtils.computeXShiftInUser(rotation);
        this.yShiftInUser = JfxUtils.computeYShiftInUser(rotation);
    }
    
    /*
     * 
     */

    private void drawPoint_raw(int x, int y) {
        final GraphicsContext gc = this.getConfiguredGc();
        final double _x = x + this.xShiftInUser;
        final double _y = y + this.yShiftInUser;
        gc.strokeLine(_x, _y, _x, _y);
    }

    private void drawLine_raw(int x1, int y1, int x2, int y2) {
        final GraphicsContext gc = this.getConfiguredGc();
        final double dx = this.xShiftInUser;
        final double dy = this.yShiftInUser;
        gc.strokeLine(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
    }

    private void drawRect_raw(int x, int y, int xSpan, int ySpan) {
        if ((xSpan == 1) && (ySpan == 1)) {
            /*
             * TODO jfx Using strokeRect(...) would just draw nothing
             * in this case.
             */
            this.drawPoint_raw(x, y);
        } else {
            final GraphicsContext gc = this.getConfiguredGc();
            final double _x = x + this.xShiftInUser;
            final double _y = y + this.yShiftInUser;
            gc.strokeRect(_x, _y, xSpan - 1, ySpan - 1);
        }
    }

    private void fillRect_raw(int x, int y, int xSpan, int ySpan) {
        final GraphicsContext gc = this.getConfiguredGc();
        final double _x = x + this.xShiftInUser;
        final double _y = y + this.yShiftInUser;
        JfxUtils.fillRect_raw_shifted(gc, _x, _y, xSpan, ySpan);
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
        gc.fillText(text, xShifted + dx, yShifted + dy + ascent);
    }

    /*
     * For debug.
     */
    
    private static void logDebug(Object... messages) {
        if (DEBUG) {
            Dbg.log(messages);
        }
    }
    
    private static void printClipStack(GraphicsContext gc, String comment) {
        logDebug();
        {
            LinkedList<?> stateStack = (LinkedList<?>) getObject(gc, "stateStack");
            logDebug(comment + " : stateStack.size() = " + stateStack.size());
            for (int i = 0; i < stateStack.size(); i++) {
                final Object state = stateStack.get(i);
                logDebug(comment + " : stateStack[" + i + "] = " + state);
                final Integer numClipPaths = (Integer) getObject(state, "numClipPaths");
                logDebug(comment + " : stateStack[" + i + "].numClipPaths = " + numClipPaths);
            }
        }
        {
            Object curState = getObject(gc, "curState");
            logDebug(comment + " : curState = " + curState);
            final Integer numClipPaths = (Integer) getObject(curState, "numClipPaths");
            logDebug(comment + " : curState.numClipPaths = " + numClipPaths);
            Object transform = getObject(curState, "transform");
            logDebug(comment + " : curState.transform = " + transform);
        }
        {
            LinkedList<?> clipStack = (LinkedList<?>) getObject(gc, "clipStack");
            logDebug(comment + " : clipStack.size() = " + clipStack.size());
            logDebug(comment + " : clipStack = " + clipStack);
        }
    }

    private static Object getObject(Object owner, String fieldName) {
        Field field = null;
        try {
            field = owner.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RethrowException(e);
        } catch (SecurityException e) {
            throw new RethrowException(e);
        }
        field.setAccessible(true);
        Object object = null;
        try {
            object = field.get(owner);
        } catch (IllegalArgumentException e) {
            throw new RethrowException(e);
        } catch (IllegalAccessException e) {
            throw new RethrowException(e);
        }
        return object;
    }
}
