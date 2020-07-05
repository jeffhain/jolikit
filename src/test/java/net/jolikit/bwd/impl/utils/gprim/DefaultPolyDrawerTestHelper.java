/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.impl.utils.gprim;

import java.util.Random;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.NumbersUtils;

public class DefaultPolyDrawerTestHelper extends AbstractDrawerTestHelper<TestPolyArgs> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Random random = new Random(123456789L);
    
    private final DefaultPolyDrawer polyDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultPolyDrawerTestHelper(
            InterfaceColorDrawer colorDrawer,
            InterfaceClippedPointDrawer clippedPointDrawer) {
        
        final DefaultClippedLineDrawer clippedLineDrawer =
                new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer =
                new DefaultLineDrawer(clippedLineDrawer);
        final DefaultClippedRectDrawer defaultClippedRectDrawer =
                new DefaultClippedRectDrawer(clippedLineDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(
                lineDrawer,
                defaultClippedRectDrawer);
        
        final DefaultPolyDrawer polyDrawer = new DefaultPolyDrawer(
                colorDrawer,
                //
                clippedPointDrawer,
                clippedLineDrawer,
                //
                lineDrawer,
                rectDrawer);
        this.polyDrawer = polyDrawer;
    }

    @Override
    public boolean isFillSupported() {
        return true;
    }

    @Override
    public void callDrawMethod(GRect clip, TestPolyArgs drawingArgs) {
        if (drawingArgs.mustDrawAsPolyline) {
            this.polyDrawer.drawPolyline(
                    clip,
                    drawingArgs.xArr,
                    drawingArgs.yArr,
                    drawingArgs.pointCount);
        } else {
            this.polyDrawer.drawPolygon(
                    clip,
                    drawingArgs.xArr,
                    drawingArgs.yArr,
                    drawingArgs.pointCount);
        }
    }

    @Override
    public void callFillMethod(GRect clip, TestPolyArgs drawingArgs) {
        final boolean areHorVerFlipped = this.random.nextBoolean();
        this.polyDrawer.fillPolygon(
                clip,
                drawingArgs.xArr,
                drawingArgs.yArr,
                drawingArgs.pointCount,
                areHorVerFlipped);
    }

    @Override
    public long getAllowedNbrOfDanglingPixels(
            boolean isFillElseDraw,
            TestPolyArgs drawingArgs) {
        if (isSimple(drawingArgs)) {
            return drawingArgs.pointCount;
        } else {
            return NumbersUtils.pow2((long) drawingArgs.pointCount);
        }
    }

    @Override
    public GRect computeBoundingBox(TestPolyArgs drawingArgs) {
        return drawingArgs.computeBoundingBox();
    }

    @Override
    public PixelFigStatus computePixelFigStatus(
            TestPolyArgs drawingArgs,
            GRect drawingBBox,
            boolean isFillElseDraw,
            GPoint pixel) {
        
        final int x = pixel.x();
        final int y = pixel.y();
        
        final int[] xArr = drawingArgs.xArr;
        final int[] yArr = drawingArgs.yArr;
        final int pointCount = drawingArgs.pointCount;
        final boolean mustDrawAsPolyline = drawingArgs.mustDrawAsPolyline;
        
        PixelFigStatus ret = PixelFigStatus.PIXEL_NOT_ALLOWED;
        
        // Looping on edges.
        final int iBound;
        if (isFillElseDraw) {
            iBound = pointCount;
        } else {
            if (pointCount >= 3) {
                iBound = pointCount - (mustDrawAsPolyline ? 1 : 0);
            } else {
                iBound = pointCount;
            }
        }
        for (int i = 0; i < iBound; i++) {
            final int ax = drawingArgs.xArr[i];
            final int ay = drawingArgs.yArr[i];
            final int ii = ((i == pointCount - 1) ? 0 : i + 1);
            final int bx = drawingArgs.xArr[ii];
            final int by = drawingArgs.yArr[ii];
            
            final double dist = computeDistancePointSegment(
                    x, y,
                    ax, ay, bx, by);
            if (dist < 0.25) {
                // Very close to an edge.
                ret = PixelFigStatus.PIXEL_REQUIRED;
                break;
            } else if (dist <= 0.5) {
                // Close to an edge.
                ret = PixelFigStatus.PIXEL_ALLOWED;
            }
        }

        if (isFillElseDraw
                && (ret == PixelFigStatus.PIXEL_NOT_ALLOWED)) {
            /*
             * Not close to a segment:
             * if in polygon, means must be drawn.
             */
            if (isInPolygon(xArr, yArr, pointCount, x, y)) {
                ret = PixelFigStatus.PIXEL_REQUIRED;
            }
        }
        
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * This is not the same algorithm that in the implementation we test,
     * on purpose.
     * This one could work for polygons defined with doubles,
     * but we use int[] to match the type of the polygons to test.
     */
    private static boolean isInPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount,
            double x,
            double y) {
        /*
         * Using ray tracing, with a ray going right (towards positive x).
         */
        boolean isIn = false;
        // first round: i=0,j=nbrOfPoints-1
        // other rounds: j = i+1
        for (int i = 0, j = pointCount-1; i < pointCount; j = i++) {
            double ypi = yArr[i];
            double ypj = yArr[j];
            // First, testing if our y is in y range of [point_i,point_j] segment.
            if (((ypi <= y) && (y < ypj)) || ((ypj <= y) && (y < ypi))) {
                // Second, testing if segment's x for our y, is right of our x.
                // To avoid a division, we multiply both sides of inequality by (ypj-ypi).
                // We need to check the sign to inverse inequality in case.
                if (ypj > ypi) {
                    if (x * (ypj-ypi) < (xArr[j]-xArr[i]) * (y-ypi) + xArr[i] * (ypj-ypi)) {
                        isIn = !isIn;
                    }
                } else {
                    if (x * (ypj-ypi) > (xArr[j]-xArr[i]) * (y-ypi) + xArr[i] * (ypj-ypi)) {
                        isIn = !isIn;
                    }
                }
            }
        }
        return isIn;
    }

    /**
     * @return The distance between two points.
     */
    private static double computeDistancePointPoint(
            double ax,
            double ay,
            double bx,
            double by) {
        return Math.sqrt((bx-ax)*(bx-ax) + (by-ay)*(by-ay));
    }

    /**
     * @return The distance from a point (x,y) to a segment AB
     */
    private static double computeDistancePointSegment(
            double x,
            double y,
            double ax,
            double ay,
            double bx,
            double by) {

        if ((ax == bx) && (ay == by)) {
            return computeDistancePointPoint(ax, ay, x, y);
        }

        double dxAB = bx-ax;
        double dyAB = by-ay;

        // r = (AC dot AB)/||AB||^2
        double r = ((x-ax)*dxAB + (y-ay)*dyAB) / (dxAB*dxAB + dyAB*dyAB);

        if (r <= 0.0) {
            // point on the backward extension of AB
            return computeDistancePointPoint(ax,ay,x,y);
        }
        if (r >= 1.0) {
            // point on the forward extension of AB
            return computeDistancePointPoint(bx,by,x,y);
        }
        // point interior to AB
        return Math.abs((ay-y)*dxAB - (ax-x)*dyAB) / Math.sqrt(dxAB*dxAB + dyAB*dyAB);
    }

    /**
     * @return true if intersection exists
     *         between segments AB and UV,
     *         false otherwise.
     */
    private static boolean doSegmentsIntersect(
            double ax,
            double ay,
            double bx,
            double by,
            double ux,
            double uy,
            double vx,
            double vy) {
        
        double dxAB = bx - ax;
        double dyAB = by - ay;
        double dxCD = vx - ux;
        double dyCD = vy - uy;

        double x = dyAB*dxCD;
        double y = dxAB*dyCD;

        x = ((uy-ay)*(dxAB*dxCD) + x*ax - y*ux) / (x-y);
        y = Math.abs(dxCD) > Math.abs(dxAB) ? (dyCD/dxCD)*(x-ux)+uy : (dyAB/dxAB)*(x-ax)+ay;

        if ((dxAB != 0.0) && !(dxAB<0 ? (x<=ax && x>=ax+dxAB) : (x>=ax && x<=ax+dxAB))) {
            return false;
        }
        if ((dxCD != 0.0) && !(dxCD<0 ? (x<=ux && x>=ux+dxCD) : (x>=ux && x<=ux+dxCD))) {
            return false;
        }
        if ((dyAB != 0.0) && !(dyAB<0 ? (y<=ay && y>=ay+dyAB) : (y>=ay && y<=ay+dyAB))) {
            return false;
        }
        if ((dyCD != 0.0) && !(dyCD<0 ? (y<=uy && y>=uy+dyCD) : (y>=uy && y<=uy+dyCD))) {
            return false;
        }

        return true;
    }
    
    /**
     * Brute force (O(n^2)).
     */
    private static boolean isSimple(TestPolyArgs drawingArgs) {
        final int[] xArr = drawingArgs.xArr;
        final int[] yArr = drawingArgs.yArr;
        final int pointCount = drawingArgs.pointCount;
        
        for (int i = 0; i < pointCount; i++) {
            final int ax = xArr[i];
            final int ay = yArr[i];
            final int ii = ((i == pointCount - 1) ? 0 : i + 1);
            final int bx = xArr[ii];
            final int by = yArr[ii];
            
            for (int j = i + 1; j < pointCount; j++) {
                final int ux = xArr[j];
                final int uy = yArr[j];
                final int jj = ((j == pointCount - 1) ? 0 : j + 1);
                final int vx = xArr[jj];
                final int vy = yArr[jj];
                
                if (doSegmentsIntersect(
                        ax, ay, bx, by,
                        ux, uy, vx, vy)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
