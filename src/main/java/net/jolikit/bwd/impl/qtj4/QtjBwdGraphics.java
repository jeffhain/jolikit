/*
 * Copyright 2019-2021 Jeff Hain
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
import com.trolltech.qt.gui.QPolygon;
import com.trolltech.qt.gui.QTransform;

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
import net.jolikit.lang.NbrsUtils;
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
    }
    
    /**
     * Qt objects, in which some of the backing graphics state is held,
     * that are pooled because they are heavy to create.
     */
    private static class MyQtStuffs {
        
        /*
         * QPainter.pen(), QPen.color(), etc., return new instances at each call,
         * so to avoid garbage we keep our own instances here.
         */
        
        private final QTransform backingTransform = new QTransform();
        
        private final QColor backingColor = new QColor();
        private final QColor backingColorOpaque = new QColor();
        
        private final QPen backingPen = new QPen();
        
        /*
         * Temps.
         */
        
        private final QTransform tmpBackingTransform = new QTransform();
    }
    
    /**
     * Resources shared between a root graphics and all its descendants.
     */
    private static class MyShared {
        final boolean isImageGraphics;
        /**
         * For a same device, at most one QPainter can be bound to it
         * (between QPainter.begin(device) and QPainter.end()) at a time,
         * else we get the error:
         * "QPainter::begin: A paint device can only be painted by one painter at a time."
         * That means that we must share a same QPainter between root graphics
         * and its descendants.
         */
        final QPainter painter;
        /**
         * For backing image.
         */
        final QtjImageHelper backingImageHelper;
        /**
         * For any image to draw on backing image.
         */
        final QtjImgDrawingUtils imgDrawingUtils = new QtjImgDrawingUtils();
        final ArrayList<MyQtStuffs> qtStuffsPool;
        /**
         * Since multiple graphics are allowed to be in use at once
         * (as long as used from UI thread), that means that we must keep track
         * of which graphic is "active" (being used), and configure the painter
         * appropriately on the fly.
         * 
         * This reference holds the currently "active" (last used) graphics,
         * among root graphics and its descendants.
         */
        QtjBwdGraphics painterCurrentGraphics = null;
        private int graphicsCount = 0;
        private boolean beginDone = false;
        private int finishCount = 0;
        public MyShared(
            boolean isImageGraphics,
            QPainter painter,
            QtjImageHelper backingImageHelper,
            ArrayList<MyQtStuffs> qtStuffsPool) {
            this.isImageGraphics = isImageGraphics;
            this.painter = painter;
            this.backingImageHelper = backingImageHelper;
            this.qtStuffsPool = qtStuffsPool;
        }
        public void onGraphicsCreation() {
            this.graphicsCount++;
        }
        public void onGraphicsInit() {
            if (!this.beginDone) {
                this.painter.begin(this.backingImageHelper.getImage());
                this.beginDone = true;
            }
        }
        public void onGraphicsFinish() {
            this.finishCount++;
            final boolean isLastFinish =
                (this.finishCount == this.graphicsCount);
            if (isLastFinish) {
                if (this.beginDone) {
                    if (this.painter.nativeId() == 0) {
                        /*
                         * Can happen if user did shut down during painting,
                         * in which case calling painter methods could cause
                         * "com.trolltech.qt.QNoNativeResourcesException: Function call on incomplete object of type: com.trolltech.qt.gui.QPainter".
                         */
                    } else {
                        this.painter.end();
                    }
                }
            }
        }
        public MyQtStuffs borrowQtStuffs() {
            MyQtStuffs ret = null;
            final int size = this.qtStuffsPool.size();
            if (size != 0) {
                ret = this.qtStuffsPool.remove(size - 1);
            } else {
                ret = new MyQtStuffs();
            }
            return ret;
        }
        public void releaseQtStuffs(MyQtStuffs obj) {
            this.qtStuffsPool.add(obj);
        }
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
    
    /**
     * Shared with parent graphics.
     */
    private final MyShared shared;
    
    /*
     * 
     */
    
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
     * @param qtStuffsPoolRef (in,out) Reference to a pool of Qt objects
     *        managed by this class.
     */
    public QtjBwdGraphics(
        InterfaceBwdBindingImpl binding,
        GRect box,
        //
        boolean isImageGraphics,
        QtjImageHelper backingImageHelper,
        //
        ObjectWrapper<Object> qtStuffsPoolRef) {
        this(
            binding,
            topLeftOf(box),
            box,
            box, // initialClip
            //
            new MyShared(
                isImageGraphics,
                new QPainter(),
                backingImageHelper,
                getOrCreateQtStuffsPool(qtStuffsPoolRef)));
    }

    /*
     * 
     */

    @Override
    public QtjBwdGraphics newChildGraphics(
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
        
        return new QtjBwdGraphics(
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
    public QtjBwdFont getFont() {
        return (QtjBwdFont) super.getFont();
    }
    
    /*
     * 
     */
    
    @Override
    public void clearRect(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        if (this.shared.isImageGraphics) {
            this.fillRectWithCompositionMode(
                    x, y, xSpan, ySpan,
                    CompositionMode.CompositionMode_Source);
        } else {
            // Relying on backing clipping.
            this.fillRect_raw(x, y, xSpan, ySpan, this.qtStuffs.backingColorOpaque);
        }
        
        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    /*
     * 
     */
    
    @Override
    public void drawPoint(int x, int y) {
        super.drawPoint(x, y);
        
        this.shared.backingImageHelper.onPointDrawing(
            this.getTransform(), x, y);
    }

    /*
     * 
     */

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        super.drawLine(x1, y1, x2, y2);
        
        this.shared.backingImageHelper.onLineDrawing(
            this.getTransform(), x1, y1, x2, y2);
    }
    
    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        
        final int ret;
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            
            GprimUtils.checkFactorAndPixelNum(factor, pixelNum);
            
            final List<Double> qtPattern = new ArrayList<Double>();
            computeQtPatternInto(factor, pattern, qtPattern);

            final QPainter painter = this.getConfiguredPainter();

            // QPen not in MyQtStuffs because
            // we actually never use this code.
            final QPen tmpPen = new QPen();
            tmpPen.setColor(this.qtStuffs.backingColor);
            tmpPen.setDashPattern(qtPattern);
            painter.setPen(tmpPen);
            try {
                painter.drawLine(x1, y1, x2, y2);
            } finally {
                painter.setPen(this.qtStuffs.backingPen);
            }
            
            if (GprimUtils.mustComputePixelNum(pattern)) {
                // Best effort.
                pixelNum = GprimUtils.pixelNumPlusSegmentPixelLengthNormalized(
                        x1, y1, x2, y2,
                        factor, pixelNum);
            }
            ret = pixelNum;
        } else {
            ret = super.drawLineStipple(
                x1, y1, x2, y2,
                factor, pattern, pixelNum);
        }
        
        this.shared.backingImageHelper.onLineDrawing(
            this.getTransform(), x1, y1, x2, y2);

        return ret;
    }

    /*
     * 
     */

    @Override
    public void drawRect(int x, int y, int xSpan, int ySpan) {
        super.drawRect(x, y, xSpan, ySpan);

        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillRect(int x, int y, int xSpan, int ySpan) {
        super.fillRect(x, y, xSpan, ySpan);

        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }

    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                final QPainter painter = this.getConfiguredPainter();

                painter.drawEllipse(x, y, xSpan - 1, ySpan - 1);
            }
        } else {
            super.drawOval(x, y, xSpan, ySpan);
        }
        
        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            
            if ((xSpan > 0) && (ySpan > 0)) {
                
                final QPainter painter = this.getConfiguredPainter();

                // TODO qtj Why doesn't fill?
                final QBrush brush = new QBrush(BrushStyle.SolidPattern);
                painter.setBrush(brush);
                try {
                    // TODO qtj Sometimes paints the wrong pixel.
                    painter.drawEllipse(x, y, xSpan - 1, ySpan - 1);
                } finally {
                    painter.setBrush(QBrush.NoBrush);
                }
            }
        } else {
            super.fillOval(x, y, xSpan, ySpan);
        }
        
        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
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
                
                final QPainter painter = this.getConfiguredPainter();

                // TODO qtj Sometimes paints the wrong pixel.
                painter.drawArc(
                        x, y, xSpan - 1, ySpan - 1,
                        startAngle, spanAngle);
            }
        } else {
            super.drawArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
        
        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
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
                
                final QPainter painter = this.getConfiguredPainter();

                // TODO qtj Why doesn't fill?
                final QBrush brush = new QBrush(BrushStyle.SolidPattern);
                painter.setBrush(brush);
                try {
                    // TODO qtj Sometimes paints the wrong pixel.
                    painter.drawArc(
                            x, y, xSpan - 1, ySpan - 1,
                            startAngle, spanAngle);
                } finally {
                    painter.setBrush(QBrush.NoBrush);
                }
            }
        } else {
            super.fillArc(
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
        
        this.shared.backingImageHelper.onRectDrawing(
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
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            if (pointCount > 0) {
                final QPolygon polygon = QtjUtils.toQPolygon(
                        xArr,
                        yArr,
                        pointCount);
                
                final QPainter painter = this.getConfiguredPainter();

                painter.drawPolyline(polygon);
            }
        } else {
            super.drawPolyline(xArr, yArr, pointCount);
        }
        
        final GRect bbox = GprimUtils.computePolyBoundingBox(xArr, yArr, pointCount);
        this.shared.backingImageHelper.onRectDrawing(
                this.getTransform(),
                bbox.x(), bbox.y(), bbox.xSpan(), bbox.ySpan());
    }

    @Override
    public void drawPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            if (pointCount > 0) {
                final QPolygon polygon = QtjUtils.toQPolygon(
                        xArr,
                        yArr,
                        pointCount);
                
                final QPainter painter = this.getConfiguredPainter();

                painter.drawPolygon(polygon);
            }
        } else {
            super.drawPolygon(xArr, yArr, pointCount);
        }
        
        final GRect bbox = GprimUtils.computePolyBoundingBox(xArr, yArr, pointCount);
        this.shared.backingImageHelper.onRectDrawing(
                this.getTransform(),
                bbox.x(), bbox.y(), bbox.xSpan(), bbox.ySpan());
    }

    @Override
    public void fillPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        if (FORCED_BACKING_GRAPHICS_USAGE) {
            this.checkUsable();
            GprimUtils.checkPolyArgs(xArr, yArr, pointCount);
            
            if (pointCount > 0) {
                final QPolygon polygon = QtjUtils.toQPolygon(
                        xArr,
                        yArr,
                        pointCount);
                
                final QPainter painter = this.getConfiguredPainter();

                // TODO qtj Why doesn't fill?
                final QBrush brush = new QBrush(BrushStyle.SolidPattern);
                painter.setBrush(brush);
                try {
                    painter.drawPolygon(polygon);
                } finally {
                    painter.setBrush(QBrush.NoBrush);
                }
            }
        } else {
            super.fillPolygon(xArr, yArr, pointCount);
        }
        
        final GRect bbox = GprimUtils.computePolyBoundingBox(xArr, yArr, pointCount);
        this.shared.backingImageHelper.onRectDrawing(
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

        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".drawText(x,y,string)");
        }
        
        final AbstractBwdFont<?> font = this.getFont();
        if (font.isDisposed()) {
            throw new IllegalStateException("font is disposed: " + font);
        }
        
        final int ascent = font.metrics().ascent();
        
        final QPainter painter = this.getConfiguredPainter();
        
        painter.drawText(x, y + ascent, text);
        
        this.shared.backingImageHelper.onTextDrawing(
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
         * TODO qtj CompositionMode.RasterOp_SourceXorDestination
         * doesn't seem to work with transparent background,
         * for it causes non alpha-premultiplied pixels in our
         * Format.Format_ARGB32_Premultiplied image:
         * "AssertionError: premul axyz = 128, 239, 223, 127".
         * As a result, for writable images,
         * we use a more brutal way.
         */
        if (this.shared.isImageGraphics) {
            
            /*
             * About ten times faster than the previous way
             * (using CompositionMode.CompositionMode_Source),
             * and not way slower than using
             * CompositionMode.RasterOp_SourceXorDestination.
             */
            
            final GRect clip = this.getClipInUser();
            
            final GTransform transform = this.getTransform();
            final GRotation rotation = transform.rotation();
            
            final int cx = GRect.intersectedPos(clip.x(), x);
            final int cy = GRect.intersectedPos(clip.y(), y);
            final int cxSpan = GRect.intersectedSpan(clip.x(), clip.xSpan(), x, xSpan);
            final int cySpan = GRect.intersectedSpan(clip.y(), clip.ySpan(), y, ySpan);
            
            final int rx = transform.minXIn1(cx, cy, cxSpan, cySpan);
            final int ry = transform.minYIn1(cx, cy, cxSpan, cySpan);
            final int rxSpan = rotation.xSpanInOther(cxSpan, cySpan);
            final int rySpan = rotation.ySpanInOther(cxSpan, cySpan);
            
            this.shared.backingImageHelper.invertPixels(
                rx,
                ry,
                rxSpan,
                rySpan);
        } else {
            this.fillRectWithCompositionModeAndPen(
                x, y, xSpan, ySpan,
                CompositionMode.RasterOp_SourceXorDestination,
                WHITE_PEN);
        }
        
        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
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
        
        final QtjImageHelper helper = this.shared.backingImageHelper;
        return helper.getSnapshotNonPremulArgb32At(xInBase, yInBase);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void initImpl() {
        this.shared.onGraphicsInit();
        
        super.initImpl();
    }
    
    @Override
    protected void finishImpl() {
        this.shared.releaseQtStuffs(this.qtStuffs);
        
        this.shared.onGraphicsFinish();
    }
    
    @Override
    protected void finishWithoutInitImpl() {
        this.shared.releaseQtStuffs(this.qtStuffs);
        
        this.shared.onGraphicsFinish();
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
        QtjUtils.setBackingColor(
            this.qtStuffs.backingColor,
            argb32);
        
        this.qtStuffs.backingPen.setColor(this.qtStuffs.backingColor);
        
        this.shared.painter.setPen(this.qtStuffs.backingPen);
        
        final int opaqueArgb32 = Argb32.toOpaque(argb32);
        this.qtStuffs.backingColorOpaque.setRgb(opaqueArgb32);
    }
    
    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        final QtjBwdFont fontImpl = (QtjBwdFont) font;
        this.shared.painter.setFont(fontImpl.getBackingFont().backingFont());
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
        
        final AbstractQtjBwdImage imageImpl = (AbstractQtjBwdImage) image;
        final QtjImageHelper imageHelper = imageImpl.getBackingImageHelper();
        
        final QPainter painter = this.getConfiguredPainter();
        
        this.shared.imgDrawingUtils.drawBackingImageOnPainter(
            this.xShiftInUser,
            this.yShiftInUser,
            this.qtStuffs.backingTransform,
            this.qtStuffs.tmpBackingTransform,
            //
            x, y, xSpan, ySpan,
            //
            imageHelper,
            //
            sx, sy, sxSpan, sySpan,
            //
            this.getBinding().getInternalParallelizer(),
            this.getBindingConfig().getMustEnsureAccurateImageScaling(),
            //
            painter);
        
        this.shared.backingImageHelper.onRectDrawing(
            this.getTransform(), x, y, xSpan, ySpan);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor to reuse instances from parent graphics.
     */
    private QtjBwdGraphics(
        InterfaceBwdBindingImpl binding,
        GPoint topLeftCoord,
        GRect box,
        GRect initialClip,
        //
        MyShared shared) {
        super(
            binding,
            topLeftCoord,
            box,
            initialClip);
        
        // Inplicit null check.
        shared.onGraphicsCreation();
        
        final QImage backingImage = shared.backingImageHelper.getImage();
        
        /*
         * Format assumed by our pixel reading treatment,
         * which wants non alpha-premultiplied pixels in the end,
         * but having an alpha-premultiplied image should be better
         * for overall preformances.
         */
        if (backingImage.format() != QtjPaintUtils.FORMAT_ARGB_PRE) {
            throw new IllegalStateException("" + backingImage.format());
        }
        
        this.shared = shared;
        
        this.qtStuffs = shared.borrowQtStuffs();
    }
    
    /*
     * 
     */
    
    /**
     * Method to use to retrieve painter for writing or reading pixels.
     * 
     * We use the pattern of retrieving the painter from this method each time,
     * even though it's always the same and accessible as a field,
     * because otherwise we might forget to call
     * ensurePainterConfiguredForThisGraphics()
     * before using this.painter.
     * 
     * @return The shared painter aligned on this graphics configuration,
     *         ready to be used for writing or reading pixels.
     */
    private QPainter getConfiguredPainter() {
        this.ensurePainterConfiguredForThisGraphics();
        return this.shared.painter;
    }
    
    private void ensurePainterConfiguredForThisGraphics() {
        final QtjBwdGraphics painterGraphics = this.shared.painterCurrentGraphics;
        if (painterGraphics != this) {
            this.configurePainterForThisGraphics();
            
            /*
             * Set after configuration, to have stack overflow
             * if trying to configure it while already being
             * configuring it.
             */
            this.shared.painterCurrentGraphics = this;
        }
    }
    
    private void configurePainterForThisGraphics() {
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
    
    private static ArrayList<MyQtStuffs> getOrCreateQtStuffsPool(
            ObjectWrapper<Object> qtStuffsPoolRef) {
        @SuppressWarnings("unchecked")
        ArrayList<MyQtStuffs> ret =
                (ArrayList<MyQtStuffs>) qtStuffsPoolRef.value;
        if (ret == null) {
            /*
             * Since painting is not done concurrently,
             * the pool doesn't need to be thread-safe.
             */
            ret = new ArrayList<MyQtStuffs>();
            qtStuffsPoolRef.value = ret;
        }
        return ret;
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

        final int _x = clip.x() + this.xShiftInUser;
        final int _y = clip.y() + this.yShiftInUser;
        
        this.shared.painter.setClipRect(_x, _y, clip.xSpan(), clip.ySpan());
    }
    
    /**
     * Sets backing transform to be current transform.
     */
    private void setBackingTransformToCurrent() {
        final GTransform transform = this.getTransform();
        final GRotation rotation = transform.rotation();
        
        // scaleInv = 2 would mean twice smaller.
        // Not using our binding's scale here, it's faster
        // to just draw with graphics on a small offscreen image
        // and then draw it scaled at the end.
        final double scaleInv = 1.0;
        
        final GPoint rootBoxTopLeft = this.getRootBoxTopLeft();
        
        this.qtStuffs.backingTransform.setMatrix(
                rotation.cos(),
                rotation.sin(),
                0.0,
                //
                -rotation.sin(),
                rotation.cos(),
                0.0,
                //
                transform.frame2XIn1() - rootBoxTopLeft.x(),
                transform.frame2YIn1() - rootBoxTopLeft.y(),
                scaleInv);
        
        this.shared.painter.setTransform(this.qtStuffs.backingTransform);

        this.xShiftInUser = ((rotation.angDeg() == 180) || (rotation.angDeg() == 270) ? -1 : 0);
        this.yShiftInUser = ((rotation.angDeg() == 90) || (rotation.angDeg() == 180) ? -1 : 0);
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
        NbrsUtils.requireSup(0, factor, "factor");
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
        if (NbrsUtils.isOdd(qtPattern.size())) {
            // Adding empty length of non-painted part.
            qtPattern.add(0.0);
        }
    }
    
    /*
     * No need to set helper's dirty flag for these raw drawing methods,
     * since they are only called from higher level public methods
     * that already take care of setting the flag.
     */
    
    private void drawPoint_raw(int x, int y) {
        final QPainter painter = this.getConfiguredPainter();
        painter.drawPoint(x, y);
    }

    private void drawLine_raw(int x1, int y1, int x2, int y2) {
        final QPainter painter = this.getConfiguredPainter();
        painter.drawLine(x1, y1, x2, y2);
    }
    
    private void drawRect_raw(int x, int y, int xSpan, int ySpan) {
        final QPainter painter = this.getConfiguredPainter();
        if ((xSpan == 1) && (ySpan == 1)) {
            // TODO qtj If w and h are both 0,
            // drawRect(...) doesn't draw anything,
            // so we use drawLine(...).
            painter.drawLine(x, y, x, y);
        } else {
            // TODO qtj Yup, need -1 here.
            painter.drawRect(x, y, xSpan - 1, ySpan - 1);
        }
    }
    
    private void fillRect_raw(int x, int y, int xSpan, int ySpan, QColor color) {
        final int _x = x + this.xShiftInUser;
        final int _y = y + this.yShiftInUser;
        
        final QPainter painter = this.getConfiguredPainter();
        
        painter.fillRect(_x, _y, xSpan, ySpan, color);
    }
    
    private void fillRectWithCompositionModeAndPen(
            int x, int y, int xSpan, int ySpan,
            CompositionMode compositionMode,
            QPen pen) {
        
        final QPainter painter = this.getConfiguredPainter();
        
        painter.setPen(pen);
        try {
            this.fillRectWithCompositionMode(
                    x, y, xSpan, ySpan,
                    compositionMode);
        } finally {
            painter.setPen(this.qtStuffs.backingPen);
        }
    }
    
    private void fillRectWithCompositionMode(
            int x, int y, int xSpan, int ySpan,
            CompositionMode compositionMode) {
        
        final QPainter painter = this.getConfiguredPainter();
        
        painter.setCompositionMode(compositionMode);
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
            
            if (this.areHorVerFlipped()) {
                final int yMaxClipped = yClipped + ySpanClipped - 1;
                for (int i = 0; i < xSpanClipped; i++) {
                    final int lineX = xClipped + i;
                    this.drawLine(lineX, yClipped, lineX, yMaxClipped);
                }
            } else {
                final int xMaxClipped = xClipped + xSpanClipped - 1;
                for (int j = 0; j < ySpanClipped; j++) {
                    final int lineY = yClipped + j;
                    this.drawLine(xClipped, lineY, xMaxClipped, lineY);
                }
            }
        } finally {
            painter.setCompositionMode(CompositionMode.CompositionMode_SourceOver);
        }
    }
}
