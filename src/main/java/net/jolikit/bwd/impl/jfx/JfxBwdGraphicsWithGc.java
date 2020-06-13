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
import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
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
     * To avoid this issue, we clip rectangles before using
     * backing treatments.
     */
    private static final boolean MUST_CLIP_RECTS_BEFORE_USING_BACKING_GRAPHICS = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyPrimitives extends AbstractBwdPrimitives {
        @Override
        public void drawPointInClip(int x, int y) {
            drawPoint_raw(x, y);
        }
        @Override
        public int drawHorizontalLineInClip(
                int x1, int x2, int y,
                int factor, short pattern, int pixelNum) {
            if (pattern == GprimUtils.PLAIN_PATTERN) {
                drawLine_raw(x1, y, x2, y);
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
                drawLine_raw(x, y1, x, y2);
                return pixelNum;
            } else {
                return super.drawVerticalLineInClip(
                        x, y1, y2,
                        factor, pattern, pixelNum);
            }
        }
        @Override
        public int drawGeneralLineInClip(
                int x1, int y1, int x2, int y2,
                int factor, short pattern, int pixelNum) {
            if (pattern == GprimUtils.PLAIN_PATTERN) {
                drawLine_raw(x1, y1, x2, y2);
                return pixelNum;
            } else {
                return super.drawGeneralLineInClip(
                        x1, y1, x2, y2,
                        factor, pattern, pixelNum);
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
    
    private final boolean isImageGraphics;
    
    private final GraphicsContext gc;
    
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

    /*
     * 
     */
    
    private final JfxDirtySnapshotHelper dirtySnapshotHelper;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for root graphics.
     * 
     * @param dirtySnapshotHelper Must not be null.
     */
    public JfxBwdGraphicsWithGc(
            InterfaceBwdBinding binding,
            GraphicsContext gc,
            boolean isImageGraphics,
            GRect box,
            //
            JfxDirtySnapshotHelper dirtySnapshotHelper) {
        this(
                binding,
                gc,
                isImageGraphics,
                box,
                box, // initialClip
                //
                dirtySnapshotHelper);
    }

    /**
     * Take care to restore its current state after usage,
     * and before usage of methods of this object.
     * 
     * @return The backing GraphicsContext.
     */
    public GraphicsContext getBackingGraphics() {
        return this.gc;
    }

    /*
     * 
     */

    @Override
    public JfxBwdGraphicsWithGc newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        final GRect childInitialClip = this.getInitialClipInBase().intersected(childBox);
        return new JfxBwdGraphicsWithGc(
                this.getBinding(),
                this.gc,
                this.isImageGraphics,
                childBox,
                childInitialClip,
                //
                this.dirtySnapshotHelper);
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
        
        /*
         * Implicit dirtySnapshotHelper calls.
         * 
         * NB: Can't clear with a transparent color with GC,
         * so even in case of image graphics, we use this
         * opaque clearing method.
         */

        final Color backingColor = (Color) this.gc.getStroke();

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

        this.gc.setFill(backingColorOpaque);
        try {
            this.fillRect(x, y, xSpan, ySpan);
        } finally {
            this.gc.setFill(backingColor);
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawPoint(int x, int y) {
        super.drawPoint(x, y);
        
        this.dirtySnapshotHelper.onPointDrawing(this.getTransform(), x, y);
    }

    /*
     * 
     */
    
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        super.drawLine(x1, y1, x2, y2);
        
        this.dirtySnapshotHelper.onLineDrawing(this.getTransform(), x1, y1, x2, y2);
    }

    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        final int ret = super.drawLineStipple(
                x1, y1, x2, y2,
                factor, pattern, pixelNum);
        
        this.dirtySnapshotHelper.onLineDrawing(this.getTransform(), x1, y1, x2, y2);
        
        return ret;
    }

    /*
     * 
     */

    @Override
    public void drawRect(int x, int y, int xSpan, int ySpan) {
        super.drawRect(x, y, xSpan, ySpan);

        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillRect(int x, int y, int xSpan, int ySpan) {
        super.fillRect(x, y, xSpan, ySpan);

        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
    }

    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final double _x = x + this.xShiftInUser;
                final double _y = y + this.yShiftInUser;
                this.gc.strokeOval(_x, _y, xSpan - 1, ySpan - 1);
            }
        } else {
            super.drawOval(x, y, xSpan, ySpan);
        }
        
        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                // Need -H rework, because we want to end up at pixels limits.
                final double _x = x + this.xShiftInUser - H;
                final double _y = y + this.yShiftInUser - H;
                this.gc.fillOval(_x, _y, xSpan, ySpan);
            }
        } else {
            super.fillOval(x, y, xSpan, ySpan);
        }
        
        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
    }
    
    /*
     * 
     */
    
    @Override
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final double _x = x + this.xShiftInUser;
                final double _y = y + this.yShiftInUser;
                this.gc.strokeArc(
                        _x, _y, xSpan - 1, ySpan - 1,
                        startDeg, spanDeg,
                        ArcType.OPEN);
            }
        } else {
            super.drawArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
        
        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                // Need -H rework, because we want to end up at pixels limits.
                final double _x = x + this.xShiftInUser - H;
                final double _y = y + this.yShiftInUser - H;
                this.gc.fillArc(
                        _x, _y, xSpan, ySpan,
                        startDeg, spanDeg,
                        ArcType.ROUND);
            }
        } else {
            super.fillArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
        
        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
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
        
        final double _x = x + this.xShiftInUser;
        final double _y = y + this.yShiftInUser;
        final GRotation rotation = this.getTransform().rotation();
        final InterfaceBwdFontMetrics fontMetrics = font.fontMetrics();
        drawText_raw_shifted(this.gc, _x, _y, text, rotation, fontMetrics);
        
        this.dirtySnapshotHelper.onTextDrawing(this.getTransform(), x, y, text, font);
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
        
        final Color c = (Color) this.gc.getStroke();
        
        final Color xorColor = Color.WHITE;
        this.gc.setFill(xorColor);
        this.gc.setGlobalBlendMode(BlendMode.DIFFERENCE);
        try {
            this.fillRect(x, y, xSpan, ySpan);
        } finally {
            this.gc.setFill(c);
            this.gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        }
        
        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
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
        
        this.dirtySnapshotHelper.beforePixelReading(xInBase, yInBase);
        final GRect snapshotBox = this.dirtySnapshotHelper.getSnapshotBox();
        if (!snapshotBox.contains(xInBase, yInBase)) {
            return defaultRes;
        }
        
        final int[] snapshotPremulArgb32Arr = this.dirtySnapshotHelper.getSnapshotPremulArgb32Arr();
        final int snapshotScanlineStride = this.dirtySnapshotHelper.getSnapshotScanlineStride();
        
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
            printClipStack(this.gc, "before initImpl()");
        }

        /*
         * Saving state prior to clip and transform setting.
         * We revert back to this state in case of clip change
         * (and in case of transform change also since we use
         * transformed clip), and then re-apply transform and
         * apply new transform clip, because there seem to be
         * no other way to replace clipping (like a clearClip()
         * method).
         * Must be done before super.initImpl(), for it calls
         * setBackingClip(...) and setBackingTransform(...),
         * which require first save to be done already.
         */
        
        this.gc.save();
        
        super.initImpl();
    }
    
    @Override
    protected void finishImpl() {
        // Popping state, else memory leak in the backing graphics
        // (and also restoring before-init state as a side effect).
        this.gc.restore();
        
        if (DEBUG) {
            printClipStack(this.gc, "after finishImpl()");
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
        
        final Color color;
        if (colorElseNull != null) {
            color = JfxUtils.newColor(colorElseNull.toArgb64());
        } else {
            color = JfxUtils.newColor(argb32);
        }

        this.gc.setFill(color);
        this.gc.setStroke(color);
        
        /*
         * Not computing opaque color, at least not here:
         * for the rare cases where it would be needed,
         * if would be best to compute it lazily as needed.
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
        
        final Color oldBackingColor = (Color) this.gc.getStroke();
        
        final Font oldBackingFont = this.gc.getFont();
        
        boolean didResetBackingState = false;
        if (mustSetClip || mustSetTransform) {
            this.resetBackingStateAndSetClipAndTransformToCurrent();
            didResetBackingState = true;
        }
        
        if (mustSetColor) {
            this.setBackingArgb(argb32, colorElseNull);
        } else {
            if (didResetBackingState) {
                // Restoring backing color.
                this.gc.setFill(oldBackingColor);
                this.gc.setStroke(oldBackingColor);
            }
        }
        
        if (mustSetFont) {
            this.setBackingFont(font);
        } else {
            if (didResetBackingState) {
                // Restoring backing font.
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

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        
        final AbstractJfxBwdImage imageImpl = (AbstractJfxBwdImage) image;
        final Image img = imageImpl.getBackingImageForGcDrawOrRead();
        
        // Only for destination box, not source.
        final double _x = x + this.xShiftInUser - H;
        final double _y = y + this.yShiftInUser - H;

        /*
         * Using simplest applicable method,
         * in case it would help perfs.
         */

        final boolean drawingPart =
                (sx != 0) || (sy != 0)
                || (sxSpan != imageWidth) || (sySpan != imageHeight);
        if (drawingPart) {
            this.gc.drawImage(
                    img,
                    sx, sy, sxSpan, sySpan,
                    _x, _y, xSpan, ySpan);
        } else {
            final boolean scaling = (xSpan != sxSpan) || (ySpan != sySpan);
            if (scaling) {
                this.gc.drawImage(img, _x, _y, xSpan, ySpan);
            } else {
                this.gc.drawImage(img, _x, _y);
            }
        }
        
        this.dirtySnapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
    }

    /*
     * 
     */
    
    protected JfxDirtySnapshotHelper getDirtySnapshotHelper() {
        return this.dirtySnapshotHelper;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private JfxBwdGraphicsWithGc(
            InterfaceBwdBinding binding,
            GraphicsContext gc,
            boolean isImageGraphics,
            GRect box,
            GRect initialClip,
            //
            JfxDirtySnapshotHelper dirtySnapshotHelper) {
        super(
                binding,
                box,
                initialClip);
        
        this.gc = LangUtils.requireNonNull(gc);
        
        this.isImageGraphics = isImageGraphics;
        
        this.dirtySnapshotHelper = LangUtils.requireNonNull(dirtySnapshotHelper);
    }
    
    private void setFontAndFontSmoothingType(Font font) {
        this.gc.setFont(font);
        this.gc.setFontSmoothingType(FONT_SMOOTHING_TYPE);
    }

    private void setBackingClipAndTransformToCurrent() {
        // Works whether we are in XOR mode or not,
        // unlike getStroke().
        final Color currentBackingColor = (Color) this.gc.getStroke();
        
        final Font currentBackingFont = this.gc.getFont();
        
        this.resetBackingStateAndSetClipAndTransformToCurrent();

        this.gc.setFill(currentBackingColor);
        this.gc.setStroke(currentBackingColor);

        this.setFontAndFontSmoothingType(currentBackingFont);
    }

    /**
     * Resets all backing state to undefined initial state,
     * and then sets its clip and transform to current.
     * 
     * After this call, backing color and font are undefined.
     */
    private void resetBackingStateAndSetClipAndTransformToCurrent() {
        // Restores initial (last saved) state (and pops it!!!).
        this.gc.restore();
        // Re-save restored initial state, so that we can restore it later
        // (with a restoreButDontPopState() method we would not have to do that).
        this.gc.save();
        
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
         */

        final GraphicsContext gc = this.gc;
        gc.beginPath();
        gc.moveTo(x, y);
        gc.lineTo(x + xSpan, y);
        gc.lineTo(x + xSpan, y + ySpan);
        gc.lineTo(x, y + ySpan);
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
        
        // Concatenating our transform.
        if (JfxUtils.H_IN_T) {
            this.gc.transform(
                    rotation.cos(), // mxx (0,0)
                    rotation.sin(), // myx (1,0)
                    -rotation.sin(), // mxy (0,1)
                    rotation.cos(), // myy (1,1)
                    transform.frame2XIn1() + H, // mxt
                    transform.frame2YIn1() + H); // myt
        } else {
            this.gc.transform(
                    rotation.cos(), // mxx (0,0)
                    rotation.sin(), // myx (1,0)
                    -rotation.sin(), // mxy (0,1)
                    rotation.cos(), // myy (1,1)
                    transform.frame2XIn1(), // mxt
                    transform.frame2YIn1()); // myt
        }

        this.xShiftInUser = JfxUtils.computeXShiftInUser(rotation);
        this.yShiftInUser = JfxUtils.computeYShiftInUser(rotation);
    }
    
    /*
     * 
     */

    private void drawPoint_raw(int x, int y) {
        final double _x = x + this.xShiftInUser;
        final double _y = y + this.yShiftInUser;
        this.gc.strokeLine(_x, _y, _x, _y);
    }

    private void drawLine_raw(int x1, int y1, int x2, int y2) {
        final double dx = this.xShiftInUser;
        final double dy = this.yShiftInUser;
        this.gc.strokeLine(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
    }

    private void drawRect_raw(int x, int y, int xSpan, int ySpan) {
        if ((xSpan == 1) && (ySpan == 1)) {
            /*
             * TODO jfx Using strokeRect(...) would just draw nothing
             * in this case.
             */
            this.drawPoint_raw(x, y);
        } else {
            final double _x = x + this.xShiftInUser;
            final double _y = y + this.yShiftInUser;
            this.gc.strokeRect(_x, _y, xSpan - 1, ySpan - 1);
        }
    }

    private void fillRect_raw(int x, int y, int xSpan, int ySpan) {
        final double _x = x + this.xShiftInUser;
        final double _y = y + this.yShiftInUser;
        fillRect_raw_shifted(this.gc, _x, _y, xSpan, ySpan);
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
        final int ascent = fontMetrics.fontAscent();
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
