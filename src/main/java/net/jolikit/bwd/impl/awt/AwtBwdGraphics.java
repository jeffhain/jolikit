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
package net.jolikit.bwd.impl.awt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.Argb3264;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.gprim.GprimUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.AbstractBwdPrimitives;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

public class AwtBwdGraphics extends AbstractBwdGraphics {
    
    /*
     * TODO awt Since we don't use much of AWT's primitives (oval etc.),
     * we could as well just paint directly into an int array, as we do for
     * OpenGL bindings, eventually using intermediary graphic buffers for
     * strings and images.
     * Performances should then be worse for drawText(...) and drawImage(...)
     * methods, but much better for methods like drawPoint(...), and also
     * for alpha blending (even though we already use an alpha-premultiplied
     * format, it's about twice slower as our alpha blending on int array).
     * But we try to be corporate, which allows us to pass the blame to AWT
     * in case of performance issue.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * True to use backing graphics when somehow relevant, to see how well
     * we can do with backing graphics.
     * False to be as pixel-perfect as possible.
     */
    private static final boolean FORCED_BACKING_GRAPHICS_USAGE = false;
    
    /**
     * For when using AWT oval methods (FORCED_BACKING_GRAPHICS_USAGE = true).
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
     */
    private static final boolean MUST_COMPLETE_CURVES_FILL_WITH_DRAW = false;
    
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
    
    private static final AffineTransform BACKING_TRANSFORM_IDENTITY =
            new AffineTransform();

    private static final AffineTransform[] ROTATION_TRANSFORM_BY_ORDINAL =
            AwtUtils.newRotationTransformArr();
    
    /*
     * 
     */
    
    private final MyPrimitives primitives = new MyPrimitives();
    
    /**
     * Using backing graphics to store color and font.
     */
    private final Graphics2D g;
    
    private final BufferedImage imageForRead;
    
    /*
     * Common adjustments for some methods.
     */
    
    private int xShiftInUser;
    private int yShiftInUser;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for root graphics.
     */
    public AwtBwdGraphics(
            InterfaceBwdBinding binding,
            Graphics2D g,
            BufferedImage imageForRead,
            GRect box) {
        this(
                binding,
                g,
                imageForRead,
                box,
                box); // baseClip
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
    public AwtBwdGraphics newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".newChildGraphics(" + childBox + ")");
        }
        final GRect childBaseClip = this.getBaseClipInClient().intersected(childBox);
        // create() useful in case of parallel painting.
        final Graphics2D subG = (Graphics2D) this.g.create();
        return new AwtBwdGraphics(
                this.getBinding(),
                subG,
                this.imageForRead,
                childBox,
                childBaseClip);
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
    public void clearRectOpaque(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        /*
         * From clearRect(...) javadoc:
         * "Beginning with Java 1.1, the background color
         * of offscreen images may be system dependent.
         * Applications should use setColor(...) followed by
         * fillRect(...) to ensure that an offscreen image
         * is cleared to a specific color."
         * ===> We obey.
         */
        final Color userColor = this.g.getColor();
        // We store clear color as "background".
        final Color clearColor = this.g.getBackground();
        this.g.setColor(clearColor);
        try {
            // Relying on backing clipping.
            fillRect_raw(x, y, xSpan, ySpan);
        } finally {
            this.g.setColor(userColor);
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
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            
            this.g.drawLine(x1, y1, x2, y2);
        } else {
            super.drawLine(x1, y1, x2, y2);
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                this.g.drawOval(x, y, xSpan - 1, ySpan - 1);
            } else {
                /*
                 * If both spans are Integer.MIN_VALUE,
                 * "- 1" would cause drawing of a huge oval.
                 */
            }
        } else {
            super.drawOval(x, y, xSpan, ySpan);
        }
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                this.g.fillOval(x, y, xSpan - 1, ySpan - 1);
                if (MUST_COMPLETE_CURVES_FILL_WITH_DRAW) {
                    this.drawOval(x, y, xSpan, ySpan);
                }
            }
        } else {
            super.fillOval(x, y, xSpan, ySpan);
        }
    }
    
    /*
     * 
     */
    
    @Override
    public void drawArc(int x, int y, int xSpan, int ySpan, double startDeg, double spanDeg) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                this.g.drawArc(
                        x, y, xSpan - 1, ySpan - 1,
                        BindingCoordsUtils.roundToInt(startDeg),
                        BindingCoordsUtils.roundToInt(spanDeg));
            }
        } else {
            super.drawArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
    }
    
    @Override
    public void fillArc(int x, int y, int xSpan, int ySpan, double startDeg, double spanDeg) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
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
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".drawText(x,y,string)");
        }
        
        final AbstractBwdFont<?> font = this.getFont();
        if (font.isDisposed()) {
            throw new IllegalStateException("font is disposed: " + font);
        }
        
        final int ascent = font.fontMetrics().fontAscent();
        
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
        
        final Color c = this.g.getColor();
        this.g.setBackground(Color.WHITE);
        this.g.setColor(Color.WHITE);
        this.g.setXORMode(Color.BLACK);
        try {
            this.fillRect(x, y, xSpan, ySpan);
        } finally {
            this.g.setBackground(c);
            this.g.setColor(c);
            this.g.setPaintMode();
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
        final int xInClient = transform.xIn1(x, y);
        final int yInClient = transform.yIn1(x, y);
        
        final int argb32 = this.imageForRead.getRGB(xInClient, yInClient);
        return argb32;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void finishImpl() {
    }
    
    /*
     * 
     */

    @Override
    protected void setBackingClip(GRect clipInClient) {
        this.setBackingClipToCurrent();
    }
    
    @Override
    protected void setBackingTransform(GTransform transform) {
        this.setBackingTransformToCurrent();

        // Must reset clip as well, since we use transformed clip.
        this.setBackingClipToCurrent(); 
    }

    @Override
    protected void setBackingArgb64(long argb64) {
        final int argb32 = Argb3264.toArgb32(argb64);
        
        final Color color = AwtUtils.newColor(argb32);
        this.g.setColor(color);
        
        // Storing clear color as background,
        // even if we don't use backing graphics's clear method.
        final Color clearColor;
        if (Argb32.isOpaque(argb32)) {
            clearColor = color;
        } else {
            clearColor = AwtUtils.newColor(Argb32.toOpaque(argb32));
        }
        this.g.setBackground(clearColor);
    }

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        final AwtBwdFont fontImpl = (AwtBwdFont) font;
        this.g.setFont(fontImpl.getBackingFont());
    }

    @Override
    protected void setBackingState(
        boolean mustSetClip,
        GRect clipInClient,
        //
        boolean mustSetTransform,
        GTransform transform,
        //
        boolean mustSetColor,
        long argb64,
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
                this.setBackingClip(clipInClient);
            }
        }
        
        if (mustSetColor) {
            this.setBackingArgb64(argb64);
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
        
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        
        final AwtBwdImage imageImpl = (AwtBwdImage) image;
        final BufferedImage img = imageImpl.getBackingImage();
        
        final ImageObserver observer = null;
        
        final int _x = x + this.xShiftInUser;
        final int _y = y + this.yShiftInUser;
        
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
                    _x, // dx1
                    _y, // dy1
                    _x+xSpan, // dx2 (exclusive)
                    _y+ySpan, // dy2 (exclusive)
                    sx, // sx1
                    sy, // sy1
                    sx+sxSpan, // sx2 (exclusive)
                    sy+sySpan, // sy2 (exclusive)
                    observer);
        } else {
            final boolean scaling = (xSpan != sxSpan) || (ySpan != sySpan);
            if (scaling) {
                this.g.drawImage(img, _x, _y, xSpan, ySpan, observer);
            } else {
                this.g.drawImage(img, _x, _y, observer);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor to reuse initialTransform instance.
     */
    private AwtBwdGraphics(
            InterfaceBwdBinding binding,
            Graphics2D g,
            BufferedImage imageForRead,
            GRect boxInClient,
            GRect clipInClient) {
        super(
                binding,
                boxInClient,
                clipInClient);
        
        this.g = LangUtils.requireNonNull(g);
        this.imageForRead = LangUtils.requireNonNull(imageForRead);
    }
    
    /**
     * Sets backing clip to be current clip.
     */
    private void setBackingClipToCurrent() {
        // Must apply transformed clip (and adjusting deltas),
        // since the backing graphics has transform applied.
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
        
        this.g.setTransform(BACKING_TRANSFORM_IDENTITY);
        
        final GRotation rotation = transform.rotation();
        this.g.translate(transform.frame2XIn1(), transform.frame2YIn1());
        this.g.transform(ROTATION_TRANSFORM_BY_ORDINAL[rotation.ordinal()]);
        
        this.xShiftInUser = AwtUtils.computeXShiftInUser(rotation);
        this.yShiftInUser = AwtUtils.computeYShiftInUser(rotation);
    }
    
    /*
     * 
     */

    private void drawPoint_raw(int x, int y) {
        this.g.drawLine(x, y, x, y);
    }

    private void drawLine_raw(int x1, int y1, int x2, int y2) {
        this.g.drawLine(x1, y1, x2, y2);
    }

    private void drawRect_raw(int x, int y, int xSpan, int ySpan) {
        this.g.drawRect(x, y, xSpan - 1, ySpan - 1);
    }

    private void fillRect_raw(int x, int y, int xSpan, int ySpan) {
        final int _x = x + this.xShiftInUser;
        final int _y = y + this.yShiftInUser;
        this.g.fillRect(_x, _y, xSpan, ySpan);
    }
}
