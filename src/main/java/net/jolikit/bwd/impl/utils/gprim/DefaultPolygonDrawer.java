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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.LangUtils;

public class DefaultPolygonDrawer implements InterfacePolygonDrawer {

    /*
     * For polygon filling, we don't use scanline algorithm,
     * which is in O(height * max(width, pointCount)),
     * and requires to store and sort edges to avoid too much
     * polygon belonging tests.
     * 
     * Instead, we first draw polygon edges, and in the process
     * also flag edge pixels as being on edges.
     * Then, we iterate on pixels from top to bottom and
     * from left to right, drawing interior horizontal segments
     * for points being in the polygon, which is only tested
     * with heavy O(pointCount) method on a single point
     * of each segment, if it could not already be determined from
     * adjacent points from the above (and already treated) line.
     * This usually leads to far less belonging checks
     * than the scanline algorithm (unless the polygon
     * looks like a saw), i.e. to an usual complexity
     * of O(max(width * height, pointCount)),
     * and makes fill polygons outline consistent with
     * drawPolygon() and with drawLine().
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * For a byte[], that's 16Mo.
     */
    private static final int MAX_REUSE_CAPACITY = 4096 * 4096;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyTemps {
        private static final byte[] EMPTY_BYTE_ARR = new byte[0];
        final MyClippedPointDrawerWithFlag clippedPointDrawerWithFlag =
                new MyClippedPointDrawerWithFlag();
        final DefaultClippedLineDrawer clippedLineDrawer =
                new DefaultClippedLineDrawer();
        byte[] tmpByteArr1 = EMPTY_BYTE_ARR;
    }

    private static class MyClippedPointDrawerWithFlag implements InterfaceClippedPointDrawer {
        private InterfaceClippedPointDrawer clippedPointDrawer;
        private GRect bbox = GRect.DEFAULT_EMPTY;
        private byte[] flagByIndex = null;
        public MyClippedPointDrawerWithFlag() {
        }
        public void reset(
                InterfaceClippedPointDrawer clippedPointDrawer,
                GRect bbox) {
            this.clippedPointDrawer = clippedPointDrawer;
            this.bbox = bbox;

            final int area = bbox.area();
            this.flagByIndex = getByteArrReset(area);
        }
        @Override
        public void drawPointInClip(int x, int y) {
            final int i = x - bbox.x();
            final int j = y - bbox.y();
            final int index = j * bbox.xSpan() + i;
            if (this.flagByIndex[index] != FLAG_PENDING) {
                // Already drawn: not drawing again.
            } else {
                this.flagByIndex[index] = FLAG_EDGE;
                this.clippedPointDrawer.drawPointInClip(x, y);
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final byte FLAG_PENDING = 0;
    private static final byte FLAG_EDGE = 1;
    private static final byte FLAG_IN = 2;
    private static final byte FLAG_OUT = 3;

    /**
     * Using thread local temps, to avoid possibly big arrays creation
     * for each graphics instance.
     */
    private static final ThreadLocal<MyTemps> TL_TEMPS =
            new ThreadLocal<MyTemps>() {
        @Override
        protected MyTemps initialValue() {
            return new MyTemps();
        }
    };

    /*
     * 
     */

    private final InterfaceColorDrawer colorDrawer;

    private final InterfaceClippedPointDrawer clippedPointDrawer;
    private final InterfaceClippedLineDrawer clippedLineDrawer;

    private final InterfaceLineDrawer lineDrawer;
    private final InterfaceRectDrawer rectDrawer;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DefaultPolygonDrawer(
            InterfaceColorDrawer colorDrawer,
            //
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer) {

        this.colorDrawer = LangUtils.requireNonNull(colorDrawer);

        this.clippedPointDrawer = LangUtils.requireNonNull(clippedPointDrawer);
        this.clippedLineDrawer = LangUtils.requireNonNull(clippedLineDrawer);

        this.lineDrawer = LangUtils.requireNonNull(lineDrawer);
        this.rectDrawer = LangUtils.requireNonNull(rectDrawer);
    }

    /*
     * Instance methods.
     */

    @Override
    public void drawPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount) {
        drawPolygon(
                clip,
                xArr,
                yArr,
                pointCount,
                //
                this.colorDrawer,
                //
                this.clippedPointDrawer,
                //
                this.lineDrawer);
    }

    @Override
    public void fillPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean areHorVerFlipped) {
        fillPolygon(
                clip,
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                //
                this.colorDrawer,
                //
                this.clippedPointDrawer,
                this.clippedLineDrawer,
                //
                this.lineDrawer,
                this.rectDrawer);
    }

    /*
     * Static methods.
     */

    public static void drawPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount,
            //
            InterfaceColorDrawer colorDrawer,
            //
            InterfaceClippedPointDrawer clippedPointDrawer,
            //
            InterfaceLineDrawer lineDrawer) {

        // Not used.
        final InterfaceClippedLineDrawer clippedLineDrawer = null;
        // Not used.
        final InterfaceRectDrawer rectDrawer = null;
        // Not used.
        final boolean areHorVerFlipped = false;
        final boolean mustFill = false;
        drawOrFillPolygon(
                clip,
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                mustFill,
                //
                colorDrawer,
                //
                clippedPointDrawer,
                clippedLineDrawer,
                //
                lineDrawer,
                rectDrawer);
    }

    public static void fillPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean areHorVerFlipped,
            //
            InterfaceColorDrawer colorDrawer,
            //
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer) {

        final boolean mustFill = true;
        drawOrFillPolygon(
                clip,
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                mustFill,
                //
                colorDrawer,
                //
                clippedPointDrawer,
                clippedLineDrawer,
                //
                lineDrawer,
                rectDrawer);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @param areHorVerFlipped Only used when filling.
     */
    private static void drawOrFillPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean areHorVerFlipped,
            boolean mustFill,
            //
            InterfaceColorDrawer colorDrawer,
            //
            InterfaceClippedPointDrawer clippedPointDrawer,
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer) {

        /*
         * Clip checks.
         * Usually not doing these early clip checks for other primitives,
         * since they just draw one thing.
         * Here, it can avoid trying to draw a lot of edges.
         */

        if (clip.isEmpty()) {
            // Nothing to draw.
            return;
        }

        final GRect bbox = GprimUtils.computePolygonBoundingBox(
                xArr,
                yArr,
                pointCount);
        final GRect cbbox = bbox.intersected(clip);
        if (cbbox.isEmpty()) {
            // Nothing to draw.
            return;
        }

        /*
         * Here, pointCount is >= 1 (since cbbox is not empty).
         * 
         * Ruling out pathological cases now
         * to make further code simpler and safer.
         */

        if (pointCount <= 2) {
            if (pointCount <= 0) {
                /*
                 * Not complaining.
                 */
            } else {
                if (pointCount == 1) {
                    clippedPointDrawer.drawPointInClip(
                            xArr[0], yArr[0]);
                } else {
                    lineDrawer.drawLine(
                            clip,
                            xArr[0], yArr[0], xArr[1], yArr[1]);
                }
            }
            return;
        }

        /*
         * 
         */

        final boolean isOpaque = colorDrawer.isColorOpaque();
        if (isOpaque && (!mustFill)) {
            // Quick path: just drawing polygon's edges.
            for (int j = 0; j < pointCount; j++) {
                final int i = ((j == 0) ? pointCount - 1 : j - 1);
                lineDrawer.drawLine(
                        clip,
                        xArr[i], yArr[i], xArr[j], yArr[j]);
            }
            return;
        }

        /*
         * Here we need to use pixels flags.
         */

        final MyTemps temps = TL_TEMPS.get();

        final MyClippedPointDrawerWithFlag clippedPointDrawerWithFlag =
                temps.clippedPointDrawerWithFlag;
        final DefaultClippedLineDrawer clippedLineDrawerWithFlag =
                temps.clippedLineDrawer;

        // cbbox is possibly smaller than clip, doesn't hurt
        // even if we still give clip as drawXxx() arg.
        clippedPointDrawerWithFlag.reset(
                clippedPointDrawer,
                cbbox);
        clippedLineDrawerWithFlag.configure(
                clippedPointDrawerWithFlag);

        /*
         * Drawing edges, and while doing it setting flags
         * for edges pixels.
         */
        for (int j = 0; j < pointCount; j++) {
            final int i = ((j == 0) ? pointCount - 1 : j - 1);
            DefaultLineDrawer.drawLine(
                    clip,
                    xArr[i], yArr[i], xArr[j], yArr[j],
                    clippedLineDrawerWithFlag);
        }

        if (!mustFill) {
            if (isOpaque) {
                /*
                 * Case already handled above.
                 */
                throw new AssertionError();
            } else {
                /*
                 * Nothing more to do.
                 */
                return;
            }
        }
        
        /*
         * Here, must fill.
         */
        
        fillPolygon_edgePixelsFlagsComputed(
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                //
                clippedLineDrawer,
                //
                rectDrawer,
                //
                cbbox,
                clippedPointDrawerWithFlag.flagByIndex);
    }

    /**
     * @param cbbox Clipped bounding box.
     * @param flagByIndex (in,out) Pixel flag by pixel index
     *        in clipped bounding box, with edges pixels flags
     *        already set to FLAG_EDGE, and other pixels set to
     *        FLAG_PENDING.
     */
    private static void fillPolygon_edgePixelsFlagsComputed(
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean areHorVerFlipped,
            //
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            InterfaceRectDrawer rectDrawer,
            //
            GRect cbbox,
            byte[] flagByIndex) {
        
        boolean foundLitPoint = false;
        int topLeftNonLitX = Integer.MIN_VALUE;
        int topLeftNonLitY = Integer.MIN_VALUE;
        
        // Looping on cbbox pixels, top-down and left-right,
        // looking for first lit and first non-lit.
        Loop1 : for (int j = 0; j < cbbox.ySpan(); j++) {
            final int offset = j * cbbox.xSpan();
            for (int i = 0; i < cbbox.xSpan(); i++) {
                final int index = offset + i;
                final int flag = flagByIndex[index];
                final boolean lit = (flag != FLAG_PENDING);
                if (lit) {
                    if (!foundLitPoint) {
                        foundLitPoint = true;
                        if (topLeftNonLitX != Integer.MIN_VALUE) {
                            // Found both.
                            break Loop1;
                        }
                    }
                } else {
                    if (topLeftNonLitX == Integer.MIN_VALUE) {
                        topLeftNonLitX = cbbox.x() + i;
                        topLeftNonLitY = cbbox.y() + j;
                        if (foundLitPoint) {
                            // Found both.
                            break Loop1;
                        }
                    }
                }
            }
        }

        if (!foundLitPoint) {
            final int xMid = cbbox.xMid();
            final int yMid = cbbox.yMid();
            final boolean isMidIn = computeBelongingToPolygon(
                    xArr,
                    yArr,
                    pointCount,
                    xMid,
                    yMid);
            if (isMidIn) {
                /*
                 * cbbox fully in the polygon:
                 * we just need to fill cbbox.
                 */
                rectDrawer.fillRect(
                        cbbox,
                        cbbox.x(),
                        cbbox.y(),
                        cbbox.xSpan(),
                        cbbox.ySpan(),
                        areHorVerFlipped);
            } else {
                /*
                 * cbbox fully outside of polygon:
                 * nothing to draw.
                 */
            }
            return;
        }

        final boolean foundNonLitPoint =
                (topLeftNonLitX != Integer.MIN_VALUE);
        if (!foundNonLitPoint) {
            /*
             * Means drawing just edges already filled cbbox:
             * nothing more to draw.
             */
            return;
        }
        
        /*
         * Here, we have some lit points,
         * and some non lit points.
         */
        
        fillPolygon_someLitSomeNonLit(
                xArr,
                yArr,
                pointCount,
                //
                clippedLineDrawer,
                //
                cbbox,
                flagByIndex,
                //
                topLeftNonLitX,
                topLeftNonLitY);
    }

    private static void fillPolygon_someLitSomeNonLit(
            int[] xArr,
            int[] yArr,
            int pointCount,
            //
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            GRect cbbox,
            byte[] flagByIndex,
            //
            int topLeftNonLitX,
            int topLeftNonLitY) {

        // All points above and left of (j0i0,j0) are on edge.
        final int j0 = topLeftNonLitY - cbbox.y();
        final int j0i0 = topLeftNonLitX - cbbox.x();

        // Looping on lines.
        for (int j = j0; j < cbbox.ySpan(); j++) {

            final int y = cbbox.y() + j;
            final int i0;
            if (j == j0) {
                i0 = j0i0;
            } else {
                i0 = 0;
            }

            // A segment is a connex part of the line
            // which pixels are either all IN or all OUT.
            // Only valid if >= 0.
            int segmentStartI = -1;
            byte segmentFlag = -1;
            
            // Looping on columns.
            int i;
            for (i = i0; i < cbbox.xSpan(); i++) {
                final int x = cbbox.x() + i;

                final int curIndex = j * cbbox.xSpan() + i;
                final byte curFlag = flagByIndex[curIndex];
                if (curFlag == FLAG_PENDING) {
                    if (segmentFlag >= FLAG_IN) {
                        // Left point is IN or OUT:
                        // copying flag and moving
                        // to next (right) point.
                        flagByIndex[curIndex] = segmentFlag;
                        continue;
                    } else if (segmentStartI < 0) {
                        // Starting the line,
                        // or first pixel after an EDGE pixel.
                        segmentStartI = i;
                    }
                    
                    /*
                     * Looking the point above,
                     * and if it's IN or OUT,
                     * copying the flag to current point.
                     */
                    if (j > 0) {
                        final int aboveIndex = curIndex - cbbox.xSpan();
                        final byte aboveFlag = flagByIndex[aboveIndex];
                        if (aboveFlag >= FLAG_IN) {
                            flagByIndex[curIndex] = aboveFlag;
                            if (segmentStartI < 0) {
                                segmentStartI = i;
                                segmentFlag = aboveFlag;
                            } else if (segmentFlag < 0) {
                                segmentFlag = aboveFlag;
                                final int indexFrom = curIndex - (i - segmentStartI);
                                final int indexTo = curIndex - 1;
                                fillWithFlag(
                                        flagByIndex,
                                        indexFrom,
                                        indexTo,
                                        segmentFlag);
                            }
                        } else {
                            /*
                             * Above is EDGE.
                             */
                        }
                    }
                } else {
                    /*
                     * Not PENDING, must be EDGE (already drawn).
                     */
                    if (curFlag != FLAG_EDGE) {
                        throw new AssertionError("" + curFlag);
                    }
                    
                    if (segmentStartI >= 0) {
                        final int segmentStartX = cbbox.x() + segmentStartI;
                        final int segmentEndX = x - 1;
                        onEdgeOrLineEndReachedWithStartedSegment(
                                xArr,
                                yArr,
                                pointCount,
                                //
                                cbbox,
                                flagByIndex,
                                y,
                                segmentStartX,
                                segmentEndX,
                                segmentFlag,
                                //
                                clippedLineDrawer);
                        segmentStartI = -1;
                        segmentFlag = -1;
                    }
                }
            }

            /*
             * Drawing line of IN pixels, if any to draw.
             */
            final int x = cbbox.x() + i;
            if (segmentStartI >= 0) {
                final int segmentStartX = cbbox.x() + segmentStartI;
                final int segmentEndX = x - 1;
                onEdgeOrLineEndReachedWithStartedSegment(
                        xArr,
                        yArr,
                        pointCount,
                        //
                        cbbox,
                        flagByIndex,
                        y,
                        segmentStartX,
                        segmentEndX,
                        segmentFlag,
                        //
                        clippedLineDrawer);
                segmentStartI = -1;
                segmentFlag = -1;
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * @param segmentStartX Must be valid (>= 0).
     * @param segmentFlag Can be invalid (< 0).
     */
    private static void onEdgeOrLineEndReachedWithStartedSegment(
            int[] xArr,
            int[] yArr,
            int pointCount,
            //
            GRect cbbox,
            byte[] flagByIndex,
            int y,
            int segmentStartX,
            int segmentEndX,
            byte segmentFlag,
            //
            InterfaceClippedLineDrawer clippedLineDrawer) {
        if (segmentFlag < 0) {
            /*
             * Segment flag is not yet known:
             * doing heavy (O(pointCount)) computation
             * and ensuring its pixels are flagged accordingly.
             */
            final boolean isIn = computeBelongingToPolygon(
                    xArr,
                    yArr,
                    pointCount,
                    segmentStartX,
                    y);
            if (isIn) {
                segmentFlag = FLAG_IN;
            } else {
                segmentFlag = FLAG_OUT;
            }
            final int i = (segmentStartX - cbbox.x());
            final int j = (y - cbbox.y());
            final int indexFrom = j * cbbox.xSpan() + i;
            final int indexTo = indexFrom + (segmentEndX - segmentStartX);
            fillWithFlag(
                    flagByIndex,
                    indexFrom,
                    indexTo,
                    segmentFlag);
        }
        if (segmentFlag == FLAG_IN) {
            clippedLineDrawer.drawHorizontalLineInClip(
                    segmentStartX, segmentEndX,
                    y,
                    1, GprimUtils.PLAIN_PATTERN, 0);
        }
    }
    
    /**
     * For use when segment flag gets know after segment start,
     * to set previous (and PENDING) segment pixels
     * to the now known flag.
     */
    private static void fillWithFlag(
            byte[] flagByIndex,
            int indexFrom,
            int indexTo,
            byte flag) {
        for (int index = indexFrom; index <= indexTo; index++) {
            flagByIndex[index] = flag;
        }
    }
    
    /*
     * 
     */

    /**
     * Derived from java.awt.Polygon.contains().
     * 
     * Assumes that the polygon contains at least 3 points.
     */
    private static boolean computeBelongingToPolygon(
            final int[] xArr,
            final int[] yArr,
            final int pointCount,
            //
            final int x,
            final int y) {

        int hits = 0;

        int lastx = xArr[pointCount - 1];
        int lasty = yArr[pointCount - 1];
        int curx;
        int cury;

        // Looping on edges.
        for (int i = 0; i < pointCount; lastx = curx, lasty = cury, i++) {
            curx = xArr[i];
            cury = yArr[i];

            if (cury == lasty) {
                // Point on same line.
                continue;
            }

            int leftx;
            if (curx < lastx) {
                // Going left.
                if (x >= lastx) {
                    // But not as far as before.
                    continue;
                }
                // New leftmost.
                leftx = curx;
            } else {
                // Going rightish.
                if (x >= curx) {
                    continue;
                }
                leftx = lastx;
            }

            double test1;
            double test2;
            if (cury < lasty) {
                if ((y < cury) || (y >= lasty)) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - curx;
                test2 = y - cury;
            } else {
                if ((y < lasty) || (y >= cury)) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - lastx;
                test2 = y - lasty;
            }

            /*
             * JDK code uses "test1 < (test2 / dy * dx)" here,
             * but we want to avoid the division.
             */
            final int dx = (lastx - curx);
            final int dy = (lasty - cury);
            if (dy < 0) {
                if (test1 * dy > test2 * dx) {
                    hits++;
                }
            } else {
                if (test1 * dy < test2 * dx) {
                    hits++;
                }
            }
        }

        return ((hits & 1) != 0);
    }

    /*
     * 
     */

    /**
     * @return Reusable byte array with [0,minCapacity-1] set to zero,
     *         which is also FLAG_PENDING.
     */
    private static byte[] getByteArrReset(
            int minCapacity) {
        if (minCapacity > MAX_REUSE_CAPACITY) {
            return new byte[minCapacity];
        }

        final MyTemps temps = TL_TEMPS.get();
        byte[] ret = temps.tmpByteArr1;
        if (ret.length < minCapacity) {
            int newCap = Math.max(minCapacity, (ret.length << 1));
            newCap = Math.min(newCap, MAX_REUSE_CAPACITY);
            ret = new byte[newCap];
            temps.tmpByteArr1 = ret;
        } else {
            for (int i = minCapacity; --i >= 0;) {
                ret[i] = FLAG_PENDING;
            }
        }
        return ret;
    }
}
