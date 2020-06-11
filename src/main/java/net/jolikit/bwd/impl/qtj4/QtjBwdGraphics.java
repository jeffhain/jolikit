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
package net.jolikit.bwd.impl.qtj4;

import java.util.ArrayList;
import java.util.List;

import com.trolltech.qt.core.Qt.BrushStyle;
import com.trolltech.qt.core.Qt.GlobalColor;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPainter.CompositionMode;
import com.trolltech.qt.gui.QPen;
import com.trolltech.qt.gui.QTransform;

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
import net.jolikit.lang.NumbersUtils;
import net.jolikit.lang.ObjectWrapper;

public class QtjBwdGraphics extends AbstractBwdGraphics {

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
            fillRect_raw(x, y, xSpan, ySpan, qtStuffs.backingColor);
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
            fillRect_raw(x, y, xSpan, ySpan, qtStuffs.backingColor);
        }
    };
    
    /**
     * Qt objects, in which some of the backing state is held,
     * that are pooled because they are heavy to create.
     */
    private static class MyQtStuffs {
        
        /*
         * QPainter.pen(), QPen.color(), etc., return new instances at each call,
         * so to avoid garbage we keep our own instances here.
         */
        
        private final QTransform backingTransform = new QTransform();
        
        private final QColor backingColor = new QColor();
        
        private final QPen backingPen = new QPen();
        
        private final QColor clearColor = new QColor();
        
        /*
         * Temps.
         */
        
        private final QTransform tmpBackingTransform = new QTransform();
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Line stipples stuffs.
     */

    private static final int NBR_OF_PATTERN_BITS_FOR_STROKE = 16;
    
    /**
     * Qt integer angles unit is 1/16th of degree.
     */
    private static final int DEG_TO_QT_ANGLE = 16;

    private static final QPen WHITE_PEN = new QPen(new QColor(GlobalColor.white));
    
    /*
     * 
     */
    
    private final MyPrimitives primitives = new MyPrimitives();
    
    /*
     * Shared with parent graphics.
     */
    
    private final QPainter painter;
    
    /**
     * Can be null.
     */
    private final QImage imageForRead;
    
    private final QTransform initialBackingTransform;
    
    /*
     * 
     */
    
    private final ArrayList<MyQtStuffs> qtStuffsPool;
    
    private final MyQtStuffs qtStuffs;
    
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
     * 
     * @param imageForRead Can be null, in which case pixel reading
     *        returns a default value.
     * @param hostGraphicsQtStuffsPoolRef (in,out) Host-held reference
     *        to a pool of Qt objects managed by this class.
     */
    public QtjBwdGraphics(
            InterfaceBwdBinding binding,
            QPainter painter,
            QImage imageForRead,
            GRect box,
            //
            ObjectWrapper<Object> hostGraphicsQtStuffsPoolRef) {
        this(
                binding,
                painter,
                imageForRead,
                box,
                box, // baseClip
                //
                painter.combinedTransform(),
                getOrCreateQtStuffsPool(hostGraphicsQtStuffsPoolRef));
    }

    /**
     * Take care to restore its current transform after usage,
     * and before usage of methods of this object.
     * 
     * @return The backing QPainter.
     */
    public QPainter getBackingGraphics() {
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".getBackingGraphics()");
        }
        return this.painter;
    }

    /**
     * @return The transform of the backing QPainter before
     *         eventually changing its transform for this object methods
     *         purpose.
     */
    public QTransform getInitialBackingTransform() {
        return this.initialBackingTransform;
    }
    
    /*
     * 
     */

    @Override
    public QtjBwdGraphics newChildGraphics(GRect childBox) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".newChildGraphics(" + childBox + ")");
        }
        final GRect childBaseClip = this.getBaseClipInClient().intersected(childBox);
        /*
         * Parallel painting on a same painting device is not supported in Qt
         * (cf. http://doc.qt.io/qt-4.8/threads-modules.html:
         *  "Any number of threads can paint at any given time,
         *  however only one thread at a time can paint on a given paint device
         *  [QImage, QPrinter, or QPicture].")
         * and we don't want to mess with multiple painting devices,
         * so we can just use the same QPainter for all our graphics.
         */
        final QPainter subG = this.painter;
        return new QtjBwdGraphics(
                this.getBinding(),
                subG,
                this.imageForRead,
                childBox,
                childBaseClip,
                //
                this.initialBackingTransform,
                this.qtStuffsPool);
    }

    /*
     * 
     */

    @Override
    public QtjBwdFont getFont() {
        return (QtjBwdFont) super.getFont();
    }
    
    /*
     * 
     */
    
    @Override
    public void clearRectOpaque(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        // Relying on backing clipping.
        this.fillRect_raw(x, y, xSpan, ySpan, this.qtStuffs.clearColor);
    }

    /*
     * 
     */

    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            
            GprimUtils.checkFactorAndPixelNum(factor, pixelNum);
            
            final List<Double> qtPattern = new ArrayList<Double>();
            computeQtPatternInto(factor, pattern, qtPattern);

            final QPen tmpPen = new QPen();
            tmpPen.setColor(this.qtStuffs.backingColor);
            tmpPen.setDashPattern(qtPattern);
            this.painter.setPen(tmpPen);
            try {
                this.painter.drawLine(x1, y1, x2, y2);
            } finally {
                this.painter.setPen(this.qtStuffs.backingPen);
            }
            
            // Best effort.
            pixelNum = GprimUtils.pixelNumPlusSegmentPixelLengthNormalized(
                    x1, y1, x2, y2,
                    factor, pixelNum);
            return pixelNum;
        } else {
            return super.drawLineStipple(
                    x1, y1, x2, y2,
                    factor, pattern, pixelNum);
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
                this.painter.drawEllipse(x, y, xSpan - 1, ySpan - 1);
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
                // TODO qtj Why doesn't fill?
                final QBrush brush = new QBrush(BrushStyle.SolidPattern);
                this.painter.setBrush(brush);
                try {
                    // TODO qtj Sometimes paints the wrong pixel.
                    this.painter.drawEllipse(x, y, xSpan - 1, ySpan - 1);
                } finally {
                    this.painter.setBrush(QBrush.NoBrush);
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
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final int startAngle = BindingCoordsUtils.roundToInt(startDeg * DEG_TO_QT_ANGLE);
                final int spanAngle = BindingCoordsUtils.roundToInt(spanDeg * DEG_TO_QT_ANGLE);
                // TODO qtj Sometimes paints the wrong pixel.
                this.painter.drawArc(
                        x, y, xSpan - 1, ySpan - 1,
                        startAngle, spanAngle);
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
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            GprimUtils.checkArcAngles(startDeg, spanDeg);
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final int startAngle = BindingCoordsUtils.roundToInt(startDeg * DEG_TO_QT_ANGLE);
                final int spanAngle = BindingCoordsUtils.roundToInt(spanDeg * DEG_TO_QT_ANGLE);
                
                // TODO qtj Why doesn't fill?
                final QBrush brush = new QBrush(BrushStyle.SolidPattern);
                this.painter.setBrush(brush);
                try {
                    // TODO qtj Sometimes paints the wrong pixel.
                    this.painter.drawArc(
                            x, y, xSpan - 1, ySpan - 1,
                            startAngle, spanAngle);
                } finally {
                    this.painter.setBrush(QBrush.NoBrush);
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
        
        this.painter.drawText(x, y + ascent, text);
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

        this.painter.setPen(WHITE_PEN);
        
        this.painter.setCompositionMode(CompositionMode.RasterOp_SourceXorDestination);
        try {
            /*
             * TODO qtj Can't use QPainter.fillRect(...),
             * because it uses a specified color,
             * without using custom composition mode,
             * so we have to resort to line drawing.
             * NB: If rotation is 90 or 270, should go faster
             * with drawing vertical (in user) lines.
             */
            
            final GRect clip = this.getClipInUser();
            
            final int xClipped = GRect.intersectedPos(clip.x(), x);
            final int yClipped = GRect.intersectedPos(clip.y(), y);
            
            final int xSpanClipped = GRect.intersectedSpan(clip.x(), clip.xSpan(), x, xSpan);
            final int ySpanClipped = GRect.intersectedSpan(clip.y(), clip.ySpan(), y, ySpan);
            
            final int xMaxClipped = xClipped + xSpanClipped - 1;
            
            for (int j = 0; j < ySpanClipped; j++) {
                final int lineY = yClipped + j;
                this.drawLine(xClipped, lineY, xMaxClipped, lineY);
            }
        } finally {
            this.painter.setPen(this.qtStuffs.backingPen);
            
            this.painter.setCompositionMode(CompositionMode.CompositionMode_SourceOver);
        }
    }

    /*
     * 
     */

    @Override
    public int getArgb32At(int x, int y) {
        // For usability and coordinates check, and default result.
        final int defaultRes = super.getArgb32At(x, y);
        if (this.imageForRead == null) {
            return defaultRes;
        }
        
        final GTransform transform = this.getTransform();
        final int xInClient = transform.xIn1(x, y);
        final int yInClient = transform.yIn1(x, y);
        
        final int arb32 = this.imageForRead.pixel(xInClient, yInClient);
        return arb32;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void finishImpl() {
        if (this.painter.nativeId() == 0) {
            /*
             * Can happen if user did shut down during painting,
             * in which case calling painter methods could cause
             * "com.trolltech.qt.QNoNativeResourcesException: Function call on incomplete object of type: com.trolltech.qt.gui.QPainter".
             */
            return;
        }

        this.restoreBackingInitialTransform();
        
        this.releaseQtStuffs(this.qtStuffs);
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
        
        // TODO qtj Says rgba, but needs argb.
        this.qtStuffs.backingColor.setRgba(argb32);
        
        this.qtStuffs.backingPen.setColor(this.qtStuffs.backingColor);
        
        this.painter.setPen(this.qtStuffs.backingPen);
        
        final int opaqueArgb32 = Argb32.toOpaque(argb32);
        this.qtStuffs.clearColor.setRgb(opaqueArgb32);
    }
    
    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        final QtjBwdFont fontImpl = (QtjBwdFont) font;
        this.painter.setFont(fontImpl.getBackingFont().backingFont());
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

        final QtjBwdImage imageImpl = (QtjBwdImage) image;
        final QImage backingImage = imageImpl.getBackingImage();
        
        final GTransform transform = this.getTransform();
        final GRotation rotation = transform.rotation();

        final int _x;
        final int _y;
        final boolean scaling = (xSpan != sxSpan) || (ySpan != sySpan);
        if (scaling) {
            final QTransform backingTransformToCombine = this.qtStuffs.tmpBackingTransform;
            backingTransformToCombine.reset();
            
            final double xScale = (xSpan / (double) sxSpan);
            final double yScale = (ySpan / (double) sySpan);
            
            /*
             * TODO qtj Seems we need these complicated shift hacks
             * to get proper results, at least for drawn image bounds.
             */
            
            {
                final int specialXShiftInUserForTransform = ((rotation.angDeg() == 0) || (rotation.angDeg() == 90) ? 1 : 0);
                final int specialYShiftInUserForTransform = ((rotation.angDeg() == 0) || (rotation.angDeg() == 270) ? 1 : 0);
                backingTransformToCombine.translate(
                        specialXShiftInUserForTransform,
                        specialYShiftInUserForTransform);
            }
            
            backingTransformToCombine.scale(xScale, yScale);

            final int specialXShiftInUser = -1;
            final int specialYShiftInUser = -1;
            
            _x = (int) ((1.0/xScale) * (x + specialXShiftInUser));
            _y = (int) ((1.0/yScale) * (y + specialYShiftInUser));
            
            final boolean combine = true;
            this.painter.setTransform(backingTransformToCombine, combine);
        } else {
            _x = x + this.xShiftInUser;
            _y = y + this.yShiftInUser;
        }
        
        /*
         * Using simplest applicable method,
         * in case it would help perfs.
         */
        
        final boolean drawingPart =
                (sx != 0) || (sy != 0)
                || (sxSpan != imageWidth) || (sySpan != imageHeight);
        try {
            if (drawingPart || scaling) {
                this.painter.drawImage(
                        _x, _y,
                        backingImage,
                        sx, sy, sxSpan, sySpan);
            } else {
                this.painter.drawImage(_x, _y, backingImage);
            }
        } finally {
            if (scaling) {
                this.painter.setTransform(this.qtStuffs.backingTransform);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor to reuse instances from parent graphics.
     */
    private QtjBwdGraphics(
            InterfaceBwdBinding binding,
            QPainter painter,
            QImage imageForRead,
            GRect box,
            GRect baseClip,
            //
            QTransform initialBackingTransform,
            ArrayList<MyQtStuffs> qtStuffsPool) {
        super(
                binding,
                box,
                baseClip);
        
        this.painter = LangUtils.requireNonNull(painter);
        
        this.imageForRead = imageForRead;
        
        this.initialBackingTransform = initialBackingTransform;
        
        this.qtStuffsPool = qtStuffsPool;
        
        this.qtStuffs = this.borrowQtStuffs();
    }
    
    private static ArrayList<MyQtStuffs> getOrCreateQtStuffsPool(
            ObjectWrapper<Object> hostGraphicsQtStuffsPoolRef) {
        @SuppressWarnings("unchecked")
        ArrayList<MyQtStuffs> ret =
                (ArrayList<MyQtStuffs>) hostGraphicsQtStuffsPoolRef.value;
        if (ret == null) {
            /*
             * Since painting is not done concurrently,
             * the pool doesn't need to be thread-safe.
             */
            ret = new ArrayList<MyQtStuffs>();
            hostGraphicsQtStuffsPoolRef.value = ret;
        }
        return ret;
    }
    
    private MyQtStuffs borrowQtStuffs() {
        MyQtStuffs ret = null;
        final int size = this.qtStuffsPool.size();
        if (size != 0) {
            ret = this.qtStuffsPool.remove(size - 1);
        } else {
            ret = new MyQtStuffs();
        }
        return ret;
    }
    
    private void releaseQtStuffs(MyQtStuffs obj) {
        this.qtStuffsPool.add(obj);
    }
    
    /**
     * Sets backing clip to be current clip.
     */
    private void setBackingClipToCurrent() {
        // Must apply transformed clip (and adjusting deltas),
        // since the backing graphics has transform applied.
        final GRect clip = this.getClipInUser();

        final int _x = clip.x() + this.xShiftInUser;
        final int _y = clip.y() + this.yShiftInUser;

        this.painter.setClipRect(_x, _y, clip.xSpan(), clip.ySpan());
    }
    
    /**
     * Sets backing transform to be current transform.
     */
    private void setBackingTransformToCurrent() {
        final GTransform transform = this.getTransform();
        final GRotation rotation = transform.rotation();
        
        // scaleInv = 2 means twice smaller.
        final double scaleInv = 1.0;
        
        this.qtStuffs.backingTransform.setMatrix(
                rotation.cos(),
                rotation.sin(),
                0.0,
                //
                -rotation.sin(),
                rotation.cos(),
                0.0,
                //
                transform.frame2XIn1(),
                transform.frame2YIn1(),
                scaleInv);

        this.painter.setTransform(this.qtStuffs.backingTransform);

        this.xShiftInUser = ((rotation.angDeg() == 180) || (rotation.angDeg() == 270) ? -1 : 0);
        this.yShiftInUser = ((rotation.angDeg() == 90) || (rotation.angDeg() == 180) ? -1 : 0);
    }

    private void restoreBackingInitialTransform() {
        this.painter.setTransform(this.initialBackingTransform);
    }
    
    /*
     * 
     */
    
    /**
     * @param qtPattern (in,out)
     */
    private static void computeQtPatternInto(
            int factor,
            short pattern,
            List<Double> qtPattern) {
        if (factor <= 0) {
            throw new IllegalArgumentException("factor [" + factor + "] must be > 0");
        }
        if (pattern == 0) {
            throw new IllegalArgumentException("pattern [" + pattern + "] must be != 0");
        }
        
        qtPattern.clear();
        
        final int patternBits = ((int)pattern) & 0xFF;
        
        final int nbrOfTrailingZeros = Integer.numberOfTrailingZeros(patternBits);
        // Checking that we have at least a 1 bit among the 16 LSBits.
        if (nbrOfTrailingZeros >= NBR_OF_PATTERN_BITS_FOR_STROKE) {
            throw new IllegalArgumentException(Short.toString(pattern));
        }
        
        // BasicStroke doesn't allow to start with a non-drawn part,
        // so we skip trailing zeros.
        int previousBit = 1;
        int bitIndex = nbrOfTrailingZeros;
        
        int nbrOfConsecutiveIdenticalBits = 0;
        int bit = Integer.MIN_VALUE;
        while (bitIndex < NBR_OF_PATTERN_BITS_FOR_STROKE) {
            bit = ((pattern>>bitIndex)&1);
            if (bit == previousBit) {
                nbrOfConsecutiveIdenticalBits++;
            } else {
                // Bit change.
                qtPattern.add(factor * (double) nbrOfConsecutiveIdenticalBits);
                previousBit = bit;
                nbrOfConsecutiveIdenticalBits = 1;
            }
            bitIndex++;
        }
        // Last part.
        qtPattern.add(factor * (double) nbrOfConsecutiveIdenticalBits);
        
        final int nbrOfDashLengths = qtPattern.size();
        if (!((nbrOfDashLengths > 0) && (nbrOfDashLengths <= NBR_OF_PATTERN_BITS_FOR_STROKE))) {
            throw new AssertionError(Integer.toString(nbrOfDashLengths));
        }
        
        // Qt pattern must contain an even number of elements,
        // i.e. starts with a length of painted part,
        // and ends with a length of non-painted part.
        if (NumbersUtils.isOdd(qtPattern.size())) {
            // Adding empty length of non-painted part.
            qtPattern.add(0.0);
        }
    }
    
    /*
     * 
     */
    
    private void drawPoint_raw(int x, int y) {
        this.painter.drawPoint(x, y);
    }

    private void drawLine_raw(int x1, int y1, int x2, int y2) {
        this.painter.drawLine(x1, y1, x2, y2);
    }
    
    private void drawRect_raw(int x, int y, int xSpan, int ySpan) {
        if ((xSpan == 1) && (ySpan == 1)) {
            // TODO qtj If w and h are both 0,
            // drawRect(...) doesn't draw anything,
            // so we use drawLine(...).
            this.painter.drawLine(x, y, x, y);
        } else {
            // TODO qtj Yup, need -1 here.
            this.painter.drawRect(x, y, xSpan - 1, ySpan - 1);
        }
    }
    
    private void fillRect_raw(int x, int y, int xSpan, int ySpan, QColor color) {
        final int _x = x + this.xShiftInUser;
        final int _y = y + this.yShiftInUser;
        this.painter.fillRect(_x, _y, xSpan, ySpan, color);
    }
}
