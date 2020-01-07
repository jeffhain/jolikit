/*
 * Copyright 2019 Jeff Hain
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
import javafx.scene.transform.Affine;
import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb64;
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

public class JfxBwdGraphics extends AbstractBwdGraphics {

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
            // Relying on backing clipping.
            drawRect_raw(x, y, xSpan, ySpan);
        }
        @Override
        public void fillRect(
                GRect clip,
                int x, int y, int xSpan, int ySpan,
                boolean areHorVerFlipped) {
            // Relying on backing clipping.
            fillRect_raw(x, y, xSpan, ySpan);
        }
    };
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * True for having the 0.5 offset (integer client coordinates being centered on
     * pixels, not pixels borders) in the transform itself.
     * TODO jfx For some reason, some rotated Strings are more badly rendered
     * when it is true, so we keep it false.
     */
    private static final boolean H_IN_T = false;
    
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
    
    private final GraphicsContext g;
    
    private final Affine initialBackingTransform;
    
    /*
     * 
     */
    
    private Color clearColor;
    
    /*
     * Common adjustments for some methods.
     */
    
    private double xShiftInUser;
    private double yShiftInUser;

    /*
     * 
     */
    
    private final JfxSnapshotHelper snapshotHelper;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for root graphics.
     * 
     * @param snapshotHelper Must not be null.
     */
    public JfxBwdGraphics(
            InterfaceBwdBinding binding,
            GraphicsContext g,
            GRect box,
            //
            JfxSnapshotHelper snapshotHelper) {
        this(
                binding,
                g,
                LangUtils.requireNonNull(box),
                box, // baseClip
                g.getTransform(),
                //
                LangUtils.requireNonNull(snapshotHelper));
    }

    /**
     * Take care to restore its current state after usage,
     * and before usage of methods of this object.
     * 
     * @return The backing GraphicsContext.
     */
    public GraphicsContext getBackingGraphics() {
        return this.g;
    }

    /*
     * 
     */

    @Override
    public JfxBwdGraphics newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        final GRect childBaseClip = this.getBaseClipInClient().intersected(childBox);
        return new JfxBwdGraphics(
                this.getBinding(),
                this.g,
                childBox,
                childBaseClip,
                this.initialBackingTransform,
                this.snapshotHelper);
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
    public void clearRectOpaque(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        final Color c = (Color) this.g.getStroke();
        
        final Color clearColor = this.clearColor;
        this.g.setFill(clearColor);
        try {
            this.fillRect(x, y, xSpan, ySpan);
        } finally {
            this.g.setFill(c);
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawPoint(int x, int y) {
        // Not bothering to do usability check before calling that.
        this.snapshotHelper.onPointDrawing(this.getTransform(), x, y);
        
        super.drawPoint(x, y);
    }

    /*
     * 
     */
    
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        this.snapshotHelper.onLineDrawing(this.getTransform(), x1, y1, x2, y2);
        
        super.drawLine(x1, y1, x2, y2);
    }

    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        this.snapshotHelper.onLineDrawing(this.getTransform(), x1, y1, x2, y2);
        
        return super.drawLineStipple(
                x1, y1, x2, y2,
                factor, pattern, pixelNum);
    }

    /*
     * 
     */

    @Override
    public void drawRect(int x, int y, int xSpan, int ySpan) {
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
        
        super.drawRect(x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillRect(int x, int y, int xSpan, int ySpan) {
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
        
        super.fillRect(x, y, xSpan, ySpan);
    }

    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final double _x = x + this.xShiftInUser;
                final double _y = y + this.yShiftInUser;
                this.g.strokeOval(_x, _y, xSpan - 1, ySpan - 1);
            }
        } else {
            super.drawOval(x, y, xSpan, ySpan);
        }
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                // Need -H rework, because we want to end up at pixels limits.
                final double _x = x + this.xShiftInUser - H;
                final double _y = y + this.yShiftInUser - H;
                this.g.fillOval(_x, _y, xSpan, ySpan);
            }
        } else {
            super.fillOval(x, y, xSpan, ySpan);
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final double _x = x + this.xShiftInUser;
                final double _y = y + this.yShiftInUser;
                this.g.strokeArc(
                        _x, _y, xSpan - 1, ySpan - 1,
                        startDeg, spanDeg,
                        ArcType.OPEN);
            }
        } else {
            super.drawArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
    }
    
    @Override
    public void fillArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);
        
        if (MUST_USE_BACKING_GRAPHICS_METHODS) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                // Need -H rework, because we want to end up at pixels limits.
                final double _x = x + this.xShiftInUser - H;
                final double _y = y + this.yShiftInUser - H;
                this.g.fillArc(
                        _x, _y, xSpan, ySpan,
                        startDeg, spanDeg,
                        ArcType.ROUND);
            }
        } else {
            super.fillArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
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
        
        this.snapshotHelper.onTextDrawing(this.getTransform(), x, y, text, font);
        
        final int ascent = font.fontMetrics().fontAscent();

        final double dx;
        final double dy;
        if (H_IN_T) {
            /*
             * TODO jfx For some reason we need to hack a bit depending on a lot
             * of stuffs to end up with the text being displayed about at the
             * same place as AWT text.
             */
            if (MUST_USE_LCD_FONT_SMOOTHING_TYPE) {
                final GRotation rotation = this.getTransform().rotation();
                dx = this.xShiftInUser - H;
                dy = this.yShiftInUser - H + ((rotation.angDeg() == 270) ? -1.0 : 0.0);
            } else {
                final GRotation rotation = this.getTransform().rotation();
                dx = this.xShiftInUser - H + ((rotation.angDeg() == 90) ? -1.0 : 0.0);
                dy = this.yShiftInUser - H + ((rotation.angDeg() == 270) ? -1.0 : 0.0);
            }
        } else {
            dx = this.xShiftInUser - H;
            dy = this.yShiftInUser - H;
        }
        this.g.fillText(text, x + dx, y + dy + ascent);
    }
    
    /*
     * 
     */

    @Override
    public void flipColors(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);

        /*
         * TODO jfx Could also use Canvas.snapshot(...)
         * and then draw reworked colors,
         * but using blend mode is simpler and works fine.
         */
        
        final Color c = (Color) this.g.getStroke();
        
        final Color xorColor = Color.WHITE;
        this.g.setFill(xorColor);
        this.g.setGlobalBlendMode(BlendMode.DIFFERENCE);
        try {
            this.fillRect(x, y, xSpan, ySpan);
        } finally {
            this.g.setFill(c);
            this.g.setGlobalBlendMode(BlendMode.SRC_OVER);
        }
    }

    /*
     * 
     */

    @Override
    public int getArgb32At(int x, int y) {
        // For usability and coordinates check, and default result.
        final int defaultRes = super.getArgb32At(x, y);
        
        final GTransform transform = this.getTransform();
        final int xInClient = transform.xIn1(x, y);
        final int yInClient = transform.yIn1(x, y);
        
        this.snapshotHelper.beforePixelReading(xInClient, yInClient);
        final GRect snapshotBox = this.snapshotHelper.getSnapshotBox();
        if (!snapshotBox.contains(xInClient, yInClient)) {
            return defaultRes;
        }
        
        final int[] snapshotPremulArgb32Arr = this.snapshotHelper.getSnapshotPremulArgb32Arr();
        final int snapshotScanlineStride = this.snapshotHelper.getSnapshotScanlineStride();
        
        final int index = yInClient * snapshotScanlineStride + xInClient;
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
            printClipStack(this.g, "before initImpl()");
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
        
        this.g.save();
        
        super.initImpl();
    }
    
    @Override
    protected void finishImpl() {
        // Poping state, else memory leak in the backing graphics
        // (and also restoring before-init state as a side effect).
        this.g.restore();
        
        if (DEBUG) {
            printClipStack(this.g, "after finishImpl()");
        }
    }
    
    /*
     * 
     */

    @Override
    protected void setBackingClip(GRect clipInClient) {
        this.setBackingClipAndTransformToCurrent();
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        this.setBackingClipAndTransformToCurrent();
    }

    @Override
    protected void setBackingArgb64(long argb64) {
        final double alphaFp = Argb64.getAlphaFp(argb64);
        final double redFp = Argb64.getRedFp(argb64);
        final double greenFp = Argb64.getGreenFp(argb64);
        final double blueFp = Argb64.getBlueFp(argb64);
        
        final Color color = new Color(redFp, greenFp, blueFp, alphaFp);

        this.g.setFill(color);
        this.g.setStroke(color);
        
        final Color clearColor;
        if (alphaFp == 1.0) {
            clearColor = color;
        } else {
            clearColor = new Color(redFp, greenFp, blueFp, 1.0);
        }
        this.clearColor = clearColor;
    }

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        final JfxBwdFont fontImpl = (JfxBwdFont) font;
        this.setFontAndFontSmoothingType(fontImpl.getBackingFont());
    }
    
    @Override
    protected void setBackingState(
        boolean mustSetClip,
        GRect clip,
        //
        boolean mustSetTransform,
        GTransform transform,
        //
        boolean mustSetColor,
        long argb64,
        //
        boolean mustSetFont,
        InterfaceBwdFont font) {
        
        final Color oldBackingColor = (Color) this.g.getStroke();
        
        final Font oldBackingFont = this.g.getFont();
        
        boolean didResetBackingState = false;
        if (mustSetClip || mustSetTransform) {
            this.resetBackingStateAndSetClipAndTransformToCurrent();
            didResetBackingState = true;
        }
        
        if (mustSetColor) {
            this.setBackingArgb64(argb64);
        } else {
            if (didResetBackingState) {
                // Restoring backing color.
                this.g.setFill(oldBackingColor);
                this.g.setStroke(oldBackingColor);
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
        this.snapshotHelper.onRectDrawing(this.getTransform(), x, y, xSpan, ySpan);

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        
        final JfxBwdImage imageImpl = (JfxBwdImage) image;
        final Image img = imageImpl.getBackingImage();
        
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
            this.g.drawImage(
                    img,
                    sx, sy, sxSpan, sySpan,
                    _x, _y, xSpan, ySpan);
        } else {
            final boolean scaling = (xSpan != sxSpan) || (ySpan != sySpan);
            if (scaling) {
                this.g.drawImage(img, _x, _y, xSpan, ySpan);
            } else {
                this.g.drawImage(img, _x, _y);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private JfxBwdGraphics(
            InterfaceBwdBinding binding,
            GraphicsContext g,
            GRect box,
            GRect baseClip,
            //
            Affine initialBackingTransform,
            //
            JfxSnapshotHelper snapshotHelper) {
        super(
                binding,
                box,
                baseClip);
        
        this.g = g;
        
        this.initialBackingTransform = initialBackingTransform;
        
        this.snapshotHelper = snapshotHelper;
    }
    
    private void setFontAndFontSmoothingType(Font font) {
        this.g.setFont(font);
        this.g.setFontSmoothingType(FONT_SMOOTHING_TYPE);
    }

    private void setBackingClipAndTransformToCurrent() {
        // Works whether we are in XOR mode or not,
        // unlike getStroke().
        final Color currentBackingColor = (Color) this.g.getStroke();
        
        final Font currentBackingFont = this.g.getFont();
        
        this.resetBackingStateAndSetClipAndTransformToCurrent();

        this.g.setFill(currentBackingColor);
        this.g.setStroke(currentBackingColor);

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
        this.g.restore();
        // Re-save restored initial state, so that we can restore it later
        // (with a restoreButDontPopState() method we would not have to do that).
        this.g.save();
        
        this.addCurrentClipToBacking();
        
        this.addCurrentTransformToBacking();
    }
    
    /**
     * Supposes that current backing graphics state is its initial state,
     * i.e. before init() call.
     */
    private void addCurrentClipToBacking() {
        final GRect clip = this.getClipInClient();
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

        final GraphicsContext g = this.g;
        g.beginPath();
        g.moveTo(x, y);
        g.lineTo(x + xSpan, y);
        g.lineTo(x + xSpan, y + ySpan);
        g.lineTo(x, y + ySpan);
        g.closePath();
        g.clip();
    }
    
    /**
     * Supposes that current backing graphics state is its initial state,
     * i.e. before init() call, except for clipping.
     */
    private void addCurrentTransformToBacking() {
        final GTransform transform = this.getTransform();
        
        final GRotation rotation = transform.rotation();
        
        // Concatenating our transform.
        if (H_IN_T) {
            this.g.transform(
                    rotation.cos(), // mxx (0,0)
                    rotation.sin(), // myx (1,0)
                    -rotation.sin(), // mxy (0,1)
                    rotation.cos(), // myy (1,1)
                    transform.frame2XIn1() + H, // mxt
                    transform.frame2YIn1() + H); // myt
        } else {
            this.g.transform(
                    rotation.cos(), // mxx (0,0)
                    rotation.sin(), // myx (1,0)
                    -rotation.sin(), // mxy (0,1)
                    rotation.cos(), // myy (1,1)
                    transform.frame2XIn1(), // mxt
                    transform.frame2YIn1()); // myt
        }

        this.xShiftInUser = computeXShiftInUser();
        this.yShiftInUser = computeYShiftInUser();
    }
    
    /**
     * @return X delta to add to client coordinate to end up in graphics coordinates.
     */
    private double computeXShiftInUser() {
        if (H_IN_T) {
            return 0;
        } else {
            final GRotation rotation = this.getTransform().rotation();
            return (rotation.cos() + rotation.sin()) * H;
        }
    }
    
    /**
     * @return Y delta to add to client coordinate to end up in graphics coordinates.
     */
    private double computeYShiftInUser() {
        if (H_IN_T) {
            return 0;
        } else {
            final GRotation rotation = this.getTransform().rotation();
            return (rotation.cos() - rotation.sin()) * H;
        }
    }
    
    /*
     * 
     */

    private void drawPoint_raw(int x, int y) {
        final double _x = x + this.xShiftInUser;
        final double _y = y + this.yShiftInUser;
        this.g.strokeLine(_x, _y, _x, _y);
    }

    private void drawLine_raw(int x1, int y1, int x2, int y2) {
        final double dx = this.xShiftInUser;
        final double dy = this.yShiftInUser;
        this.g.strokeLine(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
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
            this.g.strokeRect(_x, _y, xSpan - 1, ySpan - 1);
        }
    }

    private void fillRect_raw(int x, int y, int xSpan, int ySpan) {
        // Need -H rework, because we want to end up at pixels limits.
        final double _x = x + this.xShiftInUser - H;
        final double _y = y + this.yShiftInUser - H;
        this.g.fillRect(_x, _y, xSpan, ySpan);
    }

    /*
     * For debug.
     */
    
    private static void logDebug(Object... messages) {
        if (DEBUG) {
            Dbg.log(messages);
        }
    }
    
    private static void printClipStack(GraphicsContext g, String comment) {
        logDebug();
        {
            LinkedList<?> stateStack = (LinkedList<?>) getObject(g, "stateStack");
            logDebug(comment + " : stateStack.size() = " + stateStack.size());
            for (int i = 0; i < stateStack.size(); i++) {
                final Object state = stateStack.get(i);
                logDebug(comment + " : stateStack[" + i + "] = " + state);
                final Integer numClipPaths = (Integer) getObject(state, "numClipPaths");
                logDebug(comment + " : stateStack[" + i + "].numClipPaths = " + numClipPaths);
            }
        }
        {
            Object curState = getObject(g, "curState");
            logDebug(comment + " : curState = " + curState);
            final Integer numClipPaths = (Integer) getObject(curState, "numClipPaths");
            logDebug(comment + " : curState.numClipPaths = " + numClipPaths);
            Object transform = getObject(curState, "transform");
            logDebug(comment + " : curState.transform = " + transform);
        }
        {
            LinkedList<?> clipStack = (LinkedList<?>) getObject(g, "clipStack");
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
