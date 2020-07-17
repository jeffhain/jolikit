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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.gprim.DefaultClippedLineDrawer;
import net.jolikit.bwd.impl.utils.gprim.DefaultClippedRectDrawer;
import net.jolikit.bwd.impl.utils.gprim.DefaultLineDrawer;
import net.jolikit.bwd.impl.utils.gprim.DefaultPointDrawer;
import net.jolikit.bwd.impl.utils.gprim.DefaultPolyDrawer;
import net.jolikit.bwd.impl.utils.gprim.DefaultRectDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceArcDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceClippedLineDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceClippedPointDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceClippedRectDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceColorDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceLineDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceOvalDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfacePointDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfacePolyDrawer;
import net.jolikit.bwd.impl.utils.gprim.InterfaceRectDrawer;
import net.jolikit.bwd.impl.utils.gprim.MidPointArcDrawer;
import net.jolikit.bwd.impl.utils.gprim.MidPointOvalDrawer;
import net.jolikit.bwd.impl.utils.gprim.PolyArcDrawer;
import net.jolikit.bwd.impl.utils.gprim.PolyOvalDrawer;

/**
 * Optional abstract class, that makes it easier to implement graphics,
 * in particular by providing default implementations for graphic primitives.
 * 
 * Designed for minimal memory overhead, by having all primitive interfaces
 * implemented by a single instance, of this class.
 * Does not extend AbstractBwdGraphics, to make sure all these treatments
 * don't appear in graphics public API.
 * 
 * For provided methods, clipping is fully taken care of,
 * which allows for pixel perfect (except ambiguous cases) portability
 * across libraries, which might not all do clipping identically.
 * More precisely, the clipped extremities of a line are not necessarily
 * mathematical integers, such as the line should still appear to have its
 * exact non-clipped extremities.
 */
public abstract class AbstractBwdPrimitives implements
//
InterfaceColorDrawer,
//
InterfaceClippedPointDrawer,
InterfaceClippedLineDrawer,
InterfaceClippedRectDrawer,
//
InterfacePointDrawer,
InterfaceLineDrawer,
InterfaceRectDrawer,
//
InterfaceOvalDrawer,
InterfaceArcDrawer,
//
InterfacePolyDrawer {

    /*
     * TODO optim Could optimize away user-to-client coordinates conversion
     * for pixel-by-pixel drawings, by drawing directly in client coordinates.
     * But that would only be useful and work when the transform
     * is not taken care of by the backing library.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /*
     * Draw oval thresholds.
     */
    
    /**
     * Above that, always using poly.
     */
    private static final int DRAW_OVAL_FIG_SPAN_THRESHOLD_UP_FOR_POLY = 20 * 1000;
    
    /*
     * Draw arc thresholds.
     */
    
    /**
     * Above that, always using poly.
     */
    private static final int DRAW_ARC_FIG_SPAN_THRESHOLD_UP_FOR_POLY = 10 * 1000;
    
    /*
     * Fill oval thresholds.
     */
    
    /**
     * Above that, always using poly.
     */
    private static final int FILL_OVAL_FIG_SPAN_THRESHOLD_UP_FOR_POLY = 100 * 1000;
    
    /*
     * Fill arc thresholds.
     */
    
    /**
     * When angular span is below that we use poly,
     * which is much faster for small angles.
     * Also, allows to be consistent, since for small angles (< 30 deg)
     * our mid point algorithm might cause some dangling pixels.
     */
    private static final double FILL_ARC_SPAN_DEG_THRESHOLD_DOWN_FOR_POLY = 180.0;
    
    /**
     * Above that, always using poly,
     * which is faster-or-so regardless of angular span.
     */
    private static final int FILL_ARC_FIG_SPAN_THRESHOLD_UP_FOR_POLY = 250;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractBwdPrimitives() {
    }
    
    /*
     * InterfaceColorDrawer
     */

    @Override
    public abstract boolean isColorOpaque();

    /*
     * InterfaceClippedPointDrawer
     */
    
    @Override
    public abstract void drawPointInClip(int x, int y);
    
    /*
     * InterfaceClippedLineDrawer
     */

    /**
     * Most likely, for plain lines (pattern = -1), it's worth it to override
     * this method and delegate to the backing library.
     */
    @Override
    public int drawHorizontalLineInClip(
            int x1, int x2, int y,
            int factor, short pattern, int pixelNum) {
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        return DefaultClippedLineDrawer.drawHorizontalLineInClip(
                x1, x2, y,
                factor, pattern, pixelNum,
                clippedPointDrawer);
    }

    /**
     * Most likely, for plain lines (pattern = -1), it's worth it to override
     * this method and delegate to the backing library.
     */
    @Override
    public int drawVerticalLineInClip(
            int x, int y1, int y2,
            int factor, short pattern, int pixelNum) {
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        return DefaultClippedLineDrawer.drawVerticalLineInClip(
                x, y1, y2,
                factor, pattern, pixelNum,
                clippedPointDrawer);
    }

    /**
     * Most likely, for plain lines (pattern = -1), it's worth it to override
     * this method and delegate to the backing library.
     */
    @Override
    public int drawGeneralLineInClip(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        return DefaultClippedLineDrawer.drawGeneralLineInClip(
                x1, y1, x2, y2,
                factor, pattern, pixelNum,
                clippedPointDrawer);
    }

    @Override
    public int drawGeneralLineClipped(
            GRect clip,
            int x1, int y1, int x2, int y2,
            double x1d, double y1d, double x2d, double y2d,
            int factor, short pattern, int pixelNum) {
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        return DefaultClippedLineDrawer.drawGeneralLineClipped(
                clip,
                x1, y1, x2, y2,
                x1d, y1d, x2d, y2d,
                factor, pattern, pixelNum,
                clippedPointDrawer);
    }

    /*
     * InterfaceClippedRectDrawer
     */

    /**
     * Most likely, it's worth it to override this method
     * and delegate to the backing library.
     */
    @Override
    public void fillRectInClip(
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped) {
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        DefaultClippedRectDrawer.fillRectInClip(
                x, y, xSpan, ySpan,
                areHorVerFlipped,
                clippedLineDrawer);
    }

    /*
     * InterfacePointDrawer
     */
    
    /**
     * Most likely, it's worth it to override this method
     * and delegate to the backing library, unless it wouldn't
     * properly take care of clipping.
     */
    @Override
    public void drawPoint(GRect clip, int x, int y) {
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        DefaultPointDrawer.drawPoint(
                clip, x, y,
                clippedPointDrawer);
    }

    /*
     * InterfaceLineDrawer
     */

    @Override
    public void drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2) {
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        DefaultLineDrawer.drawLine(
                clip,
                x1, y1, x2, y2,
                clippedLineDrawer);
    }

    @Override
    public int drawLine(
            GRect clip,
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        return DefaultLineDrawer.drawLine(
                clip,
                x1, y1, x2, y2,
                factor, pattern, pixelNum,
                clippedLineDrawer);
    }
    
    /*
     * InterfaceRectDrawer
     */

    /**
     * Most likely, it's worth it to override this method
     * and delegate to the backing library, unless it wouldn't
     * properly take care of clipping.
     */
    @Override
    public void drawRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan) {
        final InterfaceLineDrawer lineDrawer = this;
        DefaultRectDrawer.drawRect(
                clip,
                x, y, xSpan, ySpan,
                lineDrawer);
    }
    
    /**
     * Most likely, it's worth it to override this method
     * and delegate to the backing library, unless it wouldn't
     * properly take care of clipping.
     */
    @Override
    public void fillRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped) {
        final InterfaceClippedRectDrawer clippedRectDrawer = this;
        DefaultRectDrawer.fillRect(
                clip,
                x, y, xSpan, ySpan,
                areHorVerFlipped,
                clippedRectDrawer);
    }
    
    /*
     * InterfaceOvalDrawer
     */
    
    @Override
    public void drawOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan) {
        
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        
        final InterfacePointDrawer pointDrawer = this;
        final InterfaceLineDrawer lineDrawer = this;
        final InterfaceRectDrawer rectDrawer = this;
        final InterfacePolyDrawer polyDrawer = this;

        if (Math.max(xSpan, ySpan) >= DRAW_OVAL_FIG_SPAN_THRESHOLD_UP_FOR_POLY) {
            PolyOvalDrawer.drawOval(
                    clip,
                    x, y, xSpan, ySpan,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer,
                    polyDrawer);
        } else {
            MidPointOvalDrawer.drawOval(
                    clip,
                    x, y, xSpan, ySpan,
                    //
                    clippedPointDrawer,
                    clippedLineDrawer,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer);
        }
    }
    
    @Override
    public void fillOval(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped) {
        
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        
        final InterfacePointDrawer pointDrawer = this;
        final InterfaceLineDrawer lineDrawer = this;
        final InterfaceRectDrawer rectDrawer = this;
        final InterfacePolyDrawer polyDrawer = this;

        if (Math.max(xSpan, ySpan) >= FILL_OVAL_FIG_SPAN_THRESHOLD_UP_FOR_POLY) {
            PolyOvalDrawer.fillOval(
                    clip,
                    x, y, xSpan, ySpan,
                    areHorVerFlipped,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer,
                    polyDrawer);
        } else {
            MidPointOvalDrawer.fillOval(
                    clip,
                    x, y, xSpan, ySpan,
                    areHorVerFlipped,
                    //
                    clippedPointDrawer,
                    clippedLineDrawer,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer);
        }
    }
    
    /*
     * InterfaceArcDrawer
     */
    
    @Override
    public void drawArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        
        final InterfacePointDrawer pointDrawer = this;
        final InterfaceLineDrawer lineDrawer = this;
        final InterfaceRectDrawer rectDrawer = this;
        final InterfacePolyDrawer polyDrawer = this;

        if (Math.max(xSpan, ySpan) >= DRAW_ARC_FIG_SPAN_THRESHOLD_UP_FOR_POLY) {
            PolyArcDrawer.drawArc(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer,
                    polyDrawer);
        } else {
            MidPointArcDrawer.drawArc(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    //
                    clippedPointDrawer,
                    clippedLineDrawer,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer);
        }
    }
    
    @Override
    public void fillArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            boolean areHorVerFlipped) {
        
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        
        final InterfacePointDrawer pointDrawer = this;
        final InterfaceLineDrawer lineDrawer = this;
        final InterfaceRectDrawer rectDrawer = this;
        final InterfacePolyDrawer polyDrawer = this;
        
        final int maxSpan = Math.max(xSpan, ySpan);
        
        final boolean mustUsePoly;
        if (spanDeg <= FILL_ARC_SPAN_DEG_THRESHOLD_DOWN_FOR_POLY) {
            mustUsePoly = true;
        } else {
            mustUsePoly = (maxSpan >= FILL_ARC_FIG_SPAN_THRESHOLD_UP_FOR_POLY);
        }
        
        if (mustUsePoly) {
            PolyArcDrawer.fillArc(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    areHorVerFlipped,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer,
                    polyDrawer);
        } else {
            MidPointArcDrawer.fillArc(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    areHorVerFlipped,
                    //
                    clippedPointDrawer,
                    clippedLineDrawer,
                    //
                    pointDrawer,
                    lineDrawer,
                    rectDrawer);
        }
    }
    
    /*
     * InterfacePolyDrawer
     */
    
    @Override
    public void drawPolyline(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount) {
        
        final InterfaceColorDrawer colorDrawer = this;
        
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        
        final InterfaceLineDrawer lineDrawer = this;
        
        DefaultPolyDrawer.drawPolyline(
                clip,
                xArr,
                yArr,
                pointCount,
                //
                colorDrawer,
                //
                clippedPointDrawer,
                //
                lineDrawer);
    }

    @Override
    public void drawPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount) {
        
        final InterfaceColorDrawer colorDrawer = this;
        
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        
        final InterfaceLineDrawer lineDrawer = this;
        
        DefaultPolyDrawer.drawPolygon(
                clip,
                xArr,
                yArr,
                pointCount,
                //
                colorDrawer,
                //
                clippedPointDrawer,
                //
                lineDrawer);
    }

    @Override
    public void fillPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean areHorVerFlipped) {
        
        final InterfaceColorDrawer colorDrawer = this;
        
        final InterfaceClippedPointDrawer clippedPointDrawer = this;
        final InterfaceClippedLineDrawer clippedLineDrawer = this;
        
        final InterfaceLineDrawer lineDrawer = this;
        final InterfaceRectDrawer rectDrawer = this;
        
        DefaultPolyDrawer.fillPolygon(
                clip,
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                //
                colorDrawer,
                //
                clippedPointDrawer,
                clippedLineDrawer,
                //
                lineDrawer,
                rectDrawer);
    }
}
