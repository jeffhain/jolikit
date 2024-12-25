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
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.gprim.GprimUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.AbstractBwdPrimitives;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Graphics based on an AWT Graphics when it's correct and fast enough,
 * otherwise on redefined treatments using backing BufferedImage API.
 * 
 * Extending AbstractIntArrayBwdGraphics and using redefined treatments
 * drawing directly on an array of pixels is usually much faster,
 * but we keep this class around for the knowledge it contains
 * and in case there would ever be a point to use it instead.
 */
public class AwtBwdGraphicsWithG extends AbstractBwdGraphics {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /*
     * Flags for use of redefined treatments, from AbstractBwdGraphics
     * and/or BufferedImageHelper, instead of the backing graphics,
     * which can be:
     * - much slower, for example for drawLine(...) with non-opaque colors.
     * - much less accurate, especially for ovals and arcs.
     */
    
    /**
     * Using backing graphics is much faster,
     * so we only use redefined treatments when necessary,
     * i.e. when clearing a writable image with a non opaque color.
     * 
     * backing : 0.16 ms
     * redefined : 12 ms
     */
    private static final boolean MUST_USE_REDEF_FOR_CLEAR_RECT = false;
    
    /**
     * True, else filling a rectangle with drawPoint() is
     * 200 times slower than using fillRect() for opaque colors,
     * and 1000 times slower for transparent colors.
     * 
     * fillRect : 0.2 ms, 2.5 ms (opaque, alpha)
     * false : 50 ms, 2500 ms
     * true : 22 ms, 30 ms
     */
    private static final boolean MUST_USE_REDEF_FOR_DRAW_POINT = true;
    
    /**
     * Redefined treatment can be a bit faster for general case (2 times)
     * or vertical lines with alpha (4 times), but for horizontal lines
     * or opaque colors it can be an order of magnitude slower,
     * so we prefer not to use it.
     * 
     * fillRect : 0.2 ms, 2.5 ms
     * false :
     * - H : 0.001 ms, 0.02 ms
     * - V : 0.002 ms, 0.13 ms
     * - GEN : 0.002 ms, 0.13 ms
     * - HRECT : 0.2 ms, 7 ms
     * - VRECT : 1.8 ms, 120 ms
     * true :
     * - H : 0.02 ms, 0.03 ms
     * - V : 0.03 ms, 0.03 ms
     * - GEN : 0.05 ms, 0.05 ms
     * - HRECT : 20 ms, 31 ms
     * - VRECT : 23 ms, 31 ms
     */
    private static final boolean MUST_USE_REDEF_FOR_DRAW_LINE = false;
    
    /**
     * Equivalent, no need to use redefined treatment.
     */
    private static final boolean MUST_USE_REDEF_FOR_DRAW_RECT = false;
    
    /**
     * Redefined treatment is much slower, not using it.
     * 
     * fillRect : 0.2 ms, 2.5 ms
     * redefined : 18 ms, 24 ms
     */
    private static final boolean MUST_USE_REDEF_FOR_FILL_RECT = false;
    
    /**
     * True because backing ovals are messy.
     */
    private static final boolean MUST_USE_REDEF_FOR_OVALS = true;
    
    /**
     * True because backing arcs are messy.
     */
    private static final boolean MUST_USE_REDEF_FOR_ARCS = true;

    /**
     * True to be consistent with drawLine().
     * Also, our implementation is faster most often
     * and in worse cases, especially with alpha.
     * 
     * - Ellipse polygon ("trivial"):
     * drawPolygon : 0.14 ms, 0.2 ms
     * redefined : 0.04 ms, 0.12 ms
     * fillPolygon : 2.2 ms, 8.5 ms
     * redefined : 1.7 ms, 4.8 ms
     * - Spiral polygon ("complex"):
     * drawPolygon : 0.26 ms, 4.2 ms
     * redefined : 0.23 ms, 1.6 ms
     * fillPolygon : 10.4 ms, 91 ms
     * redefined : 7.5 ms, 9.8 ms
     */
    private static final boolean MUST_USE_REDEF_FOR_POLYGONS = true;

    /**
     * Redefined treatment is much slower, not using it
     * (unless needed, since backing don't handle transparency properly).
     * 
     * backing : 0.2 ms
     * redefined : 18 ms
     */
    private static final boolean MUST_USE_REDEF_FOR_FLIP_COLORS = false;
    
    /**
     * Redefined treatment seems a bit slower for some reason,
     * in spite of doing about them same thing,
     * but we sill use it, for it allows not to create garbage.
     * 
     * backing : 0.016 us per call (lowest that could measure)
     * redefined : 0.019 us per call (lowest that could measure)
     * 
     * Evol: Now we also want to use it to avoid
     * color model usage if possible.
     */
    private static final boolean MUST_USE_REDEF_FOR_GET_ARGB32_AT = true;

    /*
     * 
     */
    
    /**
     * For when using AWT oval or arc methods
     * (MUST_USE_BIH_FOR_OVALS = false
     * or MUST_USE_BIH_FOR_ARCS = false).
     * 
     * TODO awt fillOval(...) and fillArc(...) are quite messy about the border
     * of the filled area, in that they easily leak outside.
     * We prefer not to leak than being closer to the average desired form,
     * so our calls to fillOval(...) and fillArc(...) don't fill as much as
     * they ought to.
     * To make up for that, set it to true to complete the border with
     * drawOval(...) and drawArc(...) methods, which use correct vertical and
     * horizontal limits, even though they draw less regular and a bit uglier
     * curves.
     * This also makes drawXXX and fillXXX methods consistent with each other.
     * Note that this would not be practical with XOR mode, if ever using it.
     * Also, this only works for opaque colors, else we can see redraws.
     */
    private static final boolean MUST_COMPLETE_CURVES_FILL_WITH_DRAW = false;
    
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
            if (MUST_USE_REDEF_FOR_DRAW_POINT) {
                drawPoint_raw_bih_inClip(x, y);
            } else {
                drawPoint_raw_g(x, y);
            }
        }
        @Override
        public int drawHorizontalLineInClip(
                int x1, int x2, int y,
                int factor, short pattern, int pixelNum) {
            if (MUST_USE_REDEF_FOR_DRAW_LINE
                    || GprimUtils.mustComputePixelNum(pattern)) {
                return super.drawHorizontalLineInClip(
                        x1, x2, y,
                        factor, pattern, pixelNum);
            } else {
                drawLine_raw_g(x1, y, x2, y);
                return pixelNum;
            }
        }
        @Override
        public int drawVerticalLineInClip(
                int x, int y1, int y2,
                int factor, short pattern, int pixelNum) {
            if (MUST_USE_REDEF_FOR_DRAW_LINE
                    || GprimUtils.mustComputePixelNum(pattern)) {
                return super.drawVerticalLineInClip(
                        x, y1, y2,
                        factor, pattern, pixelNum);
            } else {
                drawLine_raw_g(x, y1, x, y2);
                return pixelNum;
            }
        }
        @Override
        public int drawGeneralLineInClip(
                int x1, int y1, int x2, int y2,
                int factor, short pattern, int pixelNum) {
            if (MUST_USE_REDEF_FOR_DRAW_LINE
                    || GprimUtils.mustComputePixelNum(pattern)) {
                return super.drawGeneralLineInClip(
                        x1, y1, x2, y2,
                        factor, pattern, pixelNum);
            } else {
                drawLine_raw_g(x1, y1, x2, y2);
                return pixelNum;
            }
        }
        @Override
        public void fillRectInClip(
                int x, int y, int xSpan, int ySpan,
                boolean areHorVerFlipped) {
            if (MUST_USE_REDEF_FOR_FILL_RECT) {
                fillRect_raw_bih_inClip(x, y, xSpan, ySpan);
            } else {
                fillRect_raw_g(x, y, xSpan, ySpan);
            }
        }
        /*
         * 
         */
        @Override
        public void drawPoint(GRect clip, int x, int y) {
            if (MUST_USE_REDEF_FOR_DRAW_POINT) {
                super.drawPoint(clip, x, y);
            } else {
                drawPoint_raw_g(x, y);
            }
        }
        @Override
        public void drawLine(
                GRect clip,
                int x1, int y1, int x2, int y2) {
            if (MUST_USE_REDEF_FOR_DRAW_LINE) {
                super.drawLine(clip, x1, y1, x2, y2);
            } else {
                drawLine_raw_g(x1, y1, x2, y2);
            }
        }
        @Override
        public int drawLine(
                GRect clip,
                int x1, int y1, int x2, int y2,
                int factor, short pattern, int pixelNum) {
            if (MUST_USE_REDEF_FOR_DRAW_LINE
                    || GprimUtils.mustComputePixelNum(pattern)) {
                return super.drawLine(
                        clip,
                        x1, y1, x2, y2,
                        factor, pattern, pixelNum);
            } else {
                drawLine_raw_g(x1, y1, x2, y2);
                return pixelNum;
            }
        }
        @Override
        public void drawRect(
                GRect clip,
                int x, int y, int xSpan, int ySpan) {
            if (MUST_USE_REDEF_FOR_DRAW_RECT) {
                super.drawRect(clip, x, y, xSpan, ySpan);
            } else {
                // Relying on backing clipping.
                drawRect_raw_g(x, y, xSpan, ySpan);
            }
        }
        @Override
        public void fillRect(
                GRect clip,
                int x, int y, int xSpan, int ySpan,
                boolean areHorVerFlipped) {
            if (MUST_USE_REDEF_FOR_FILL_RECT) {
                super.fillRect(clip, x, y, xSpan, ySpan, areHorVerFlipped);
            } else {
                // Relying on backing clipping.
                fillRect_raw_g(x, y, xSpan, ySpan);
            }
        }
        /*
         * No need to redefine methods for ovals or arcs,
         * since they are not used by others of our primitives,
         * so redirection is done only in public implementations.
         */
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AffineTransform BACKING_TRANSFORM_IDENTITY =
            new AffineTransform();

    private static final AffineTransform[] ROTATION_TRANSFORM_BY_ORDINAL =
            AwtUtils.newRotationTransformArr();
    
    /*
     * 
     */
    
    private final MyPrimitives primitives = new MyPrimitives();
    
    private final boolean isImageGraphics;
    
    private final BufferedImageHelper bufferedImageHelper;
    
    private final AwtImgDrawingUtils imgDrawingUtils = new AwtImgDrawingUtils();
    
    private Graphics2D g;
    
    /**
     * Lazily computed.
     */
    private Color backingColorOpaque = null;
    
    /*
     * Common adjustments for some methods.
     */
    
    private int xShiftInUser;
    private int yShiftInUser;
    
    /**
     * Transform from BufferedImage coordinates to user coordinates
     * (compared to getTransform(), includes eventual delta
     * due to rootBoxTopLeft).
     * Allows not to have to add rootBoxTopLeft delta
     * for each pixel drawing.
     */
    private GTransform transformBiToUser;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for root graphics.
     */
    public AwtBwdGraphicsWithG(
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
                backingImage);
    }

    /**
     * Take care to restore its current transform after usage,
     * and before usage of methods of this object.
     * 
     * @return The backing Graphics2D.
     */
    public Graphics2D getBackingGraphics() {
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".getBackingGraphics()");
        }
        return this.g;
    }
    
    /*
     * 
     */

    @Override
    public AwtBwdGraphicsWithG newChildGraphics(
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
        
        return new AwtBwdGraphicsWithG(
                this.getBinding(),
                this.getRootBoxTopLeft(),
                childBox,
                childInitialClip,
                //
                this.isImageGraphics,
                this.bufferedImageHelper.getImage());
    }

    /*
     * 
     */

    @Override
    public AwtBwdFont getFont() {
        return (AwtBwdFont) super.getFont();
    }
    
    /*
     * 
     */
    
    @Override
    public void clearRect(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        final int argb32 = this.getArgb32();
        
        /*
         * TODO awt Graphics.fillRect(...)
         * doesn't work properly for non-opaque colors,
         * so always using redefined treatments
         * for non-opaque colors on writable images,
         * regardless of our static flag.
         */
        if (MUST_USE_REDEF_FOR_CLEAR_RECT
                || (this.isImageGraphics
                        && (!Argb32.isOpaque(argb32)))) {

            final GTransform transform = this.getTransform();
            
            final GRect rectInUser = GRect.valueOf(x, y, xSpan, ySpan);
            final GRect rectInBase = transform.rectIn1(rectInUser);
            
            final GRect clipInBase = this.getClipInBase();
            final GRect rectInBaseClipped = rectInBase.intersected(clipInBase);
            
            final int xInBaseClipped = rectInBaseClipped.x();
            final int yInBaseClipped = rectInBaseClipped.y();
            final int xSpanInBaseClipped = rectInBaseClipped.xSpan();
            final int ySpanInBaseClipped = rectInBaseClipped.ySpan();

            final int argb32ToSet;
            if (this.isImageGraphics) {
                argb32ToSet = argb32;
            } else {
                // For client, must be opaque.
                argb32ToSet = Argb32.toOpaque(argb32);
            }
            final boolean premul = false;
            this.bufferedImageHelper.clearRect(
                    xInBaseClipped,
                    yInBaseClipped,
                    xSpanInBaseClipped,
                    ySpanInBaseClipped,
                    argb32ToSet,
                    premul);
        } else {
            /*
             * From clearRect(...) javadoc:
             * "Beginning with Java 1.1, the background color
             * of offscreen images may be system dependent.
             * Applications should use setColor(...) followed by
             * fillRect(...) to ensure that an offscreen image
             * is cleared to a specific color."
             */
            final Color color = this.g.getColor();
            
            Color colorOpaque = this.backingColorOpaque;
            if (colorOpaque == null) {
                if (Argb32.getAlpha8(argb32) == 0xFF) {
                    colorOpaque = color;
                } else {
                    final int argb32Opaque = Argb32.toOpaque(argb32);
                    colorOpaque = AwtUtils.newColor(argb32Opaque);
                }
                this.backingColorOpaque = colorOpaque;
            }
            
            this.g.setColor(colorOpaque);
            try {
                // Relying on backing clipping.
                final int _x = x + this.xShiftInUser;
                final int _y = y + this.yShiftInUser;
                this.g.fillRect(_x, _y, xSpan, ySpan);
            } finally {
                this.g.setColor(color);
            }
        }
    }

    /*
     * TODO awt It's possible to draw a stipple with AWT, using BasicStroke,
     * but it would not be consistent with what we can do with JavaFX Canvas,
     * and it is ugly (thick tortuous line in case of rotated graphics),
     * so we stay away from it.
     * It would also generate much garbage, unless if pooling strokes
     * in some way, which would make things a bit messy.
     */

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (MUST_USE_REDEF_FOR_DRAW_LINE) {
            super.drawLine(x1, y1, x2, y2);
        } else {
            this.checkUsable();
            
            // Relying on backing clipping.
            this.g.drawLine(x1, y1, x2, y2);
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        if (MUST_USE_REDEF_FOR_OVALS) {
            super.drawOval(x, y, xSpan, ySpan);
        } else {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                this.g.drawOval(x, y, xSpan - 1, ySpan - 1);
            } else {
                /*
                 * If both spans are Integer.MIN_VALUE,
                 * "- 1" would cause drawing of a huge oval.
                 */
            }
        }
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        if (MUST_USE_REDEF_FOR_OVALS) {
            super.fillOval(x, y, xSpan, ySpan);
        } else {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                this.g.fillOval(x, y, xSpan - 1, ySpan - 1);
                if (MUST_COMPLETE_CURVES_FILL_WITH_DRAW) {
                    this.drawOval(x, y, xSpan, ySpan);
                }
            }
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawArc(int x, int y, int xSpan, int ySpan, double startDeg, double spanDeg) {
        if (MUST_USE_REDEF_FOR_ARCS) {
            super.drawArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        } else {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                this.g.drawArc(
                        x, y, xSpan - 1, ySpan - 1,
                        BindingCoordsUtils.roundToInt(startDeg),
                        BindingCoordsUtils.roundToInt(spanDeg));
            }
        }
    }
    
    @Override
    public void fillArc(int x, int y, int xSpan, int ySpan, double startDeg, double spanDeg) {
        if (MUST_USE_REDEF_FOR_ARCS) {
            super.fillArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        } else {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                this.g.fillArc(
                        x, y, xSpan - 1, ySpan - 1,
                        BindingCoordsUtils.roundToInt(startDeg),
                        BindingCoordsUtils.roundToInt(spanDeg));
                if (MUST_COMPLETE_CURVES_FILL_WITH_DRAW) {
                    this.drawArc(x, y, xSpan, ySpan, startDeg, spanDeg);
                }
            }
        }
    }

    /*
     * 
     */
    
    @Override
    public void drawPolyline(int[] xArr, int[] yArr, int pointCount) {
        if (MUST_USE_REDEF_FOR_POLYGONS) {
            super.drawPolyline(xArr, yArr, pointCount);
        } else {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            // Relying on backing clipping.
            this.g.drawPolyline(xArr, yArr, pointCount);
        }
    }
    
    @Override
    public void drawPolygon(int[] xArr, int[] yArr, int pointCount) {
        if (MUST_USE_REDEF_FOR_POLYGONS) {
            super.drawPolygon(xArr, yArr, pointCount);
        } else {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            // Relying on backing clipping.
            this.g.drawPolygon(xArr, yArr, pointCount);
        }
    }
    
    @Override
    public void fillPolygon(int[] xArr, int[] yArr, int pointCount) {
        if (MUST_USE_REDEF_FOR_POLYGONS) {
            super.fillPolygon(xArr, yArr, pointCount);
        } else {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);

            // Relying on backing clipping.
            this.g.fillPolygon(xArr, yArr, pointCount);
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
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".drawText(x,y,string)");
        }
        
        final AbstractBwdFont<?> font = this.getFont();
        if (font.isDisposed()) {
            throw new IllegalStateException("font is disposed: " + font);
        }
        
        final int ascent = font.metrics().ascent();
        
        final int _x = x + this.xShiftInUser;
        final int _y = y + this.yShiftInUser + ascent;
        
        this.g.drawString(text, _x, _y);
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
         * TODO awt Graphics.setXORMode(...)
         * doesn't work properly with non-opaque background,
         * so always using redefined treatments for writable images,
         * regardless of our static flag.
         */
        if (MUST_USE_REDEF_FOR_FLIP_COLORS
                || this.isImageGraphics) {
            
            final GTransform transform = this.getTransform();
            
            final GRect rectInUser = GRect.valueOf(x, y, xSpan, ySpan);
            final GRect rectInBase = transform.rectIn1(rectInUser);
            
            final GRect clipInBase = this.getClipInBase();
            final GRect rectInBaseClipped = rectInBase.intersected(clipInBase);
            
            final int xInBaseClipped = rectInBaseClipped.x();
            final int yInBaseClipped = rectInBaseClipped.y();
            final int xSpanInBaseClipped = rectInBaseClipped.xSpan();
            final int ySpanInBaseClipped = rectInBaseClipped.ySpan();
            
            this.bufferedImageHelper.invertPixels(
                    xInBaseClipped,
                    yInBaseClipped,
                    xSpanInBaseClipped,
                    ySpanInBaseClipped);
        } else {
            final Color c = this.g.getColor();
            this.g.setBackground(Color.WHITE);
            this.g.setColor(Color.WHITE);
            this.g.setXORMode(Color.BLACK);
            try {
                final int _x = x + this.xShiftInUser;
                final int _y = y + this.yShiftInUser;
                // Relying on backing clipping.
                this.g.fillRect(_x, _y, xSpan, ySpan);
            } finally {
                this.g.setBackground(c);
                this.g.setColor(c);
                this.g.setPaintMode();
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
        
        // fix: was just using getTransform() instead.
        final GTransform transformBiToUser = this.transformBiToUser;
        final int xInBi = transformBiToUser.xIn1(x, y);
        final int yInBi = transformBiToUser.yIn1(x, y);
        
        final int argb32;
        if (MUST_USE_REDEF_FOR_GET_ARGB32_AT) {
            argb32 = this.bufferedImageHelper.getNonPremulArgb32At(xInBi, yInBi);
        } else {
            final BufferedImage bufferedImage = this.bufferedImageHelper.getImage();
            argb32 = bufferedImage.getRGB(xInBi, yInBi);
        }
        return argb32;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void initImpl() {
        this.g = this.bufferedImageHelper.getImage().createGraphics();
        
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
        this.setBackingClipToCurrent();
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        this.setBackingTransformToCurrent();

        // Must reset clip as well, since we use transformed clip.
        this.setBackingClipToCurrent(); 
    }

    @Override
    protected void setBackingArgb(int argb32, BwdColor colorElseNull) {
        /*
         * Not bothering with 64 bits color,
         * for consistency and optimization.
         */
        final Color color = AwtUtils.newColor(argb32);
        this.g.setColor(color);
        /*
         * Not computing opaque color, at least not here:
         * for the rare cases where it would be needed,
         * if would be best to compute it lazily as needed.
         */
        this.backingColorOpaque = null;
    }

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        final AwtBwdFont fontImpl = (AwtBwdFont) font;
        this.g.setFont(fontImpl.getBackingFont());
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
        
        if (mustSetTransform) {
            this.setBackingTransform(transform);
            if (mustSetClip) {
                // Clip already taken care of by setBackingTransform(...).
            }
        } else {
            if (mustSetClip) {
                this.setBackingClip(clipInBase);
            }
        }
        
        if (mustSetColor) {
            this.setBackingArgb(argb32, colorElseNull);
        }
        
        if (mustSetFont) {
            this.setBackingFont(font);
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
        
        final AbstractAwtBwdImage imageImpl = (AbstractAwtBwdImage) image;
        final BufferedImage backingImage = imageImpl.getBufferedImage();
        
        this.imgDrawingUtils.drawBufferedImageOnG(
            this.xShiftInUser,
            this.yShiftInUser,
            this.getRootBoxTopLeft(),
            this.getTransform(),
            this.getClipInUser(),
            //
            x, y, xSpan, ySpan,
            //
            backingImage,
            //
            sx, sy, sxSpan, sySpan,
            //
            this.getBinding().getInternalParallelizer(),
            this.getImageScalingType(),
            this.getBindingConfig().getMustUseBackingImageScalingIfApplicable(),
            //
            this.bufferedImageHelper,
            this.g);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private AwtBwdGraphicsWithG(
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
                initialClip);
        
        this.isImageGraphics = isImageGraphics;
        
        final boolean allowColorModelAvoiding = true;
        // The point of this graphics is to not use the array directly.
        // Also might allow for some hardware acceleration
        // due to no call to "theTrackable.setUntrackable()".
        final boolean allowArrayDirectUse = false;
        this.bufferedImageHelper = new BufferedImageHelper(
            backingImage,
            allowColorModelAvoiding,
            allowArrayDirectUse);
    }
    
    /*
     * 
     */

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
        
        this.transformBiToUser = BindingCoordsUtils.computeTransformBoxToUser(
            rootBoxTopLeft,
            transform);
    }
    
    /*
     * 
     */

    private void drawPoint_raw_g(int x, int y) {
        // Relying on backing clipping.
        this.g.drawLine(x, y, x, y);
    }

    private void drawPoint_raw_bih_inClip(int x, int y) {
        // fix: was just using getTransform() instead.
        final GTransform transformBiToUser = this.transformBiToUser;
        final int xInBi = transformBiToUser.xIn1(x, y);
        final int yInBi = transformBiToUser.yIn1(x, y);
        
        final int premulArgb32 = this.getPremulArgb32();
        this.bufferedImageHelper.drawPointPremulAt(
            xInBi,
            yInBi,
            premulArgb32);
    }
    
    /*
     * 
     */

    private void drawLine_raw_g(int x1, int y1, int x2, int y2) {
        // Relying on backing clipping.
        this.g.drawLine(x1, y1, x2, y2);
    }
    
    /*
     * 
     */

    private void drawRect_raw_g(int x, int y, int xSpan, int ySpan) {
        // Relying on backing clipping.
        this.g.drawRect(x, y, xSpan - 1, ySpan - 1);
    }
    
    /*
     * 
     */

    private void fillRect_raw_g(int x, int y, int xSpan, int ySpan) {
        // Relying on backing clipping.
        final int _x = x + this.xShiftInUser;
        final int _y = y + this.yShiftInUser;
        this.g.fillRect(_x, _y, xSpan, ySpan);
    }

    private void fillRect_raw_bih_inClip(int x, int y, int xSpan, int ySpan) {

        final int premulArgb32 = this.getPremulArgb32();
        
        final GTransform transform = this.getTransform();
        
        final GRect rectInUser = GRect.valueOf(x, y, xSpan, ySpan);
        final GRect rectInBase = transform.rectIn1(rectInUser);
        
        final int xInBase = rectInBase.x();
        final int yInBase = rectInBase.y();
        final int xSpanInBase = rectInBase.xSpan();
        final int ySpanInBase = rectInBase.ySpan();
        
        this.bufferedImageHelper.fillRectPremul(
                xInBase,
                yInBase,
                xSpanInBase,
                ySpanInBase,
                premulArgb32);
    }
}
