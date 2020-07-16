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
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

public class DefaultPolyDrawer implements InterfacePolyDrawer {

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
     * We also keep track of min/max lit X in each row,
     * which allows for nice filling speed-up in case
     * of fully out rows, or in case of oblique polygons.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    /**
     * For a byte[], that's 16Mo.
     */
    private static final int MAX_FLAG_ARR_REUSE_CAPACITY = 4096 * 4096;

    /**
     * Should be large enough for most screens.
     */
    private static final int MAX_X_ARR_REUSE_CAPACITY = 4096 * 4;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyTemps {
        private static final byte[] EMPTY_BYTE_ARR = new byte[0];
        private static final int[] EMPTY_INT_ARR = new int[0];
        final MyClippedPointDrawerWithFlag clippedPointDrawerWithFlag =
                new MyClippedPointDrawerWithFlag();
        final DefaultClippedLineDrawer clippedLineDrawer =
                new DefaultClippedLineDrawer();
        byte[] tmpFlagByIndex = EMPTY_BYTE_ARR;
        /*
         * min/max lit X on each row.
         */
        int[] tmpXMinArr = EMPTY_INT_ARR;
        int[] tmpXMaxArr = EMPTY_INT_ARR;
    }

    private static class MyFlagManager {
        private byte[] flagByIndex = null;
        private int flagOffset = 0;
        private int flagByIndexMaxArea = 0;
        public MyFlagManager() {
        }
        public void reset(
                MyTemps temps,
                GRect bbox) {
            final int area = bbox.area();
            final byte[] flagByIndex = getFlagByIndex(temps, area);
            this.flagByIndex = flagByIndex;
            if (flagByIndex != temps.tmpFlagByIndex) {
                /*
                 * Not a reusable array:
                 * is already filled with PENDING,
                 * and whatever current flagOffset works.
                 */
            } else {
                if (this.flagOffset < MAX_FLAG_OFFSET) {
                    this.flagOffset += FLAG_OFFSET_INCREMENT;
                    if (this.flagByIndexMaxArea < area) {
                        this.flagByIndexMaxArea = area;
                    }
                } else {
                    // Only resetting what might have been set.
                    for (int i = this.flagByIndexMaxArea; --i >= 0;) {
                        flagByIndex[i] = FLAG_PENDING;
                    }
                    this.flagOffset = 0;
                    this.flagByIndexMaxArea = 0;
                }
            }
        }
        /**
         * @return Flag in [0,3].
         */
        public byte getFlagAt(int index) {
            // In [0,255].
            final int offsetFlagOrLess = (this.flagByIndex[index] & 0xFF);
            
            // In [-252,255].
            final int flagOrLess = offsetFlagOrLess - this.flagOffset;
            final byte flag;
            if (flagOrLess <= FLAG_PENDING) {
                // Any value lower (i.e. from previous
                // post-reset-calls settings) or equal
                // (i.e. from current call setting) matches.
                flag = FLAG_PENDING;
            } else {
                // Means has been set during current call,
                // and since it is not PENDING, it is in [1,3].
                flag = (byte) flagOrLess;
            }
            return flag;
        }
        /**
         * @param flag In [0,3].
         */
        public void setFlagAt(int index, byte flag) {
            // In [0,255].
            final int offsetFlag = this.flagOffset + flag;
            this.flagByIndex[index] = (byte) offsetFlag;
        }
        /**
         * Returns "inversed" boolean, for caller not to have to not it.
         * 
         * @param flagToSet In [0,3].
         * @return True if did NOT set, false if did set.
         */
        public boolean setFlagAtIfIsPending(int index, byte flagToSet) {
            final int offsetFlagOrLess = (this.flagByIndex[index] & 0xFF);
            final boolean ret;
            if (offsetFlagOrLess - this.flagOffset <= FLAG_PENDING) {
                this.flagByIndex[index] = (byte) (this.flagOffset + flagToSet);
                ret = false;
            } else {
                ret = true;
            }
            return ret;
        }
    }
    
    private static class MyClippedPointDrawerWithFlag implements InterfaceClippedPointDrawer {
        private InterfaceClippedPointDrawer clippedPointDrawer;
        private GRect bbox = GRect.DEFAULT_EMPTY;
        final MyFlagManager flagManager = new MyFlagManager();
        private int[] xMinArr = null;
        private int[] xMaxArr = null;
        public MyClippedPointDrawerWithFlag() {
        }
        public void reset(
                MyTemps temps,
                boolean isFillElseDraw,
                InterfaceClippedPointDrawer clippedPointDrawer,
                GRect bbox) {
            this.clippedPointDrawer = clippedPointDrawer;
            this.bbox = bbox;

            this.flagManager.reset(temps, bbox);
            
            if (isFillElseDraw) {
                this.xMinArr = getXArr(temps, bbox.ySpan(), false);
                this.xMaxArr = getXArr(temps, bbox.ySpan(), true);
                for (int i = 0; i < bbox.ySpan(); i++) {
                    this.xMinArr[i] = Integer.MAX_VALUE;
                    this.xMaxArr[i] = Integer.MIN_VALUE;
                }
            } else {
                this.xMinArr = null;
                this.xMaxArr = null;
            }
        }
        @Override
        public void drawPointInClip(int x, int y) {
            final int i = x - bbox.x();
            final int j = y - bbox.y();
            final int index = j * bbox.xSpan() + i;
            final boolean didNotSet =
                    this.flagManager.setFlagAtIfIsPending(index, FLAG_EDGE);
            if (didNotSet) {
                // Already drawn: not drawing again.
            } else {
                this.clippedPointDrawer.drawPointInClip(x, y);
                if (this.xMinArr != null) {
                    this.xMinArr[j] = Math.min(this.xMinArr[j], x);
                    this.xMaxArr[j] = Math.max(this.xMaxArr[j], x);
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * In flagByIndex array, storing flag values with an offset
     * incremented by (n-1) = 3 at each usage, up to (255-3)/3 = 84,
     * which allows to reset the array only once every 84 usages
     * (using modulo arithmetic, else we would use 127 instead of 255).
     * 
     * Meaningful flag values for current call are in
     * [flagOffset, flagOffset + 3].
     * 
     * Whatever the current value of flagOffset in [0,84*3=252],
     * it works for an array filled with zeros, i.e. with FLAG_PENDING
     * (which occurs when we create a new array, possibly because
     * required capacity is to large for reuse).
     */
    private static final int FLAG_OFFSET_INCREMENT = 3;
    
    private static final int FLAG_OFFSET_PERIOD =
            (255 - FLAG_OFFSET_INCREMENT) / FLAG_OFFSET_INCREMENT;
    private static final int MAX_FLAG_OFFSET =
            FLAG_OFFSET_PERIOD * FLAG_OFFSET_INCREMENT;
    
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

    public DefaultPolyDrawer(
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
    public void drawPolyline(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount) {
        drawPolyline(
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

    public static void drawPolyline(
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
        final boolean isFillElseDraw = false;
        final boolean isPolyline = true;
        drawOrFillPoly(
                clip,
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                isFillElseDraw,
                isPolyline,
                //
                colorDrawer,
                //
                clippedPointDrawer,
                clippedLineDrawer,
                //
                lineDrawer,
                rectDrawer);
    }

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
        final boolean isFillElseDraw = false;
        final boolean isPolyline = false;
        drawOrFillPoly(
                clip,
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                isFillElseDraw,
                isPolyline,
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

        final boolean isFillElseDraw = true;
        final boolean isPolyline = false;
        drawOrFillPoly(
                clip,
                xArr,
                yArr,
                pointCount,
                areHorVerFlipped,
                isFillElseDraw,
                isPolyline,
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
     * @param isPolyline Only used if isFillElseDraw is false.
     */
    private static void drawOrFillPoly(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean areHorVerFlipped,
            boolean isFillElseDraw,
            boolean isPolyline,
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

        final GRect bbox = GprimUtils.computePolyBoundingBox(
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
        if (isOpaque && (!isFillElseDraw)) {
            // Quick path: just drawing poly's edges.
            final int i0 = (isPolyline ? 1 : 0);
            for (int i = i0; i < pointCount; i++) {
                final int ii = ((i == 0) ? pointCount - 1 : i - 1);
                lineDrawer.drawLine(
                        clip,
                        xArr[ii], yArr[ii], xArr[i], yArr[i]);
            }
            return;
        }

        /*
         * Special casing for when the clip is in or out of the polygon,
         * so that in these cases we don't bother with using pixels flags,
         * which overhead is proportional to cbbox due to initial zeroization.
         * NB: Doesn't return for polyline when only the "missing" segment
         * overlaps clip, but that's fine.
         */
        
        {
            final boolean isClipClearlyInOrOut =
                    isClipClearlyInOrOutOfPolygon(
                            clip,
                            xArr,
                            yArr,
                            pointCount);
            if (isClipClearlyInOrOut) {
                if (isFillElseDraw) {
                    final boolean isClipInPolyElseOut =
                            computeBelongingToPolygon(
                                    xArr,
                                    yArr,
                                    pointCount,
                                    clip.xMid(),
                                    clip.yMid());
                    if (DEBUG) {
                        Dbg.log("isClipInPolyElseOut = " + isClipInPolyElseOut);
                    }
                    if (isClipInPolyElseOut) {
                        rectDrawer.fillRect(
                                clip,
                                clip.x(), clip.y(), clip.xSpan(), clip.ySpan(),
                                areHorVerFlipped);
                    } else {
                        // Nothing to fill.
                    }
                } else {
                    // Nothing to draw.
                }
                return;
            }
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
                temps,
                isFillElseDraw,
                clippedPointDrawer,
                cbbox);
        clippedLineDrawerWithFlag.configure(
                clippedPointDrawerWithFlag);

        /*
         * Drawing edges, and while doing it setting flags
         * for edges pixels.
         */
        final int i0 = (isPolyline ? 1 : 0);
        for (int i = i0; i < pointCount; i++) {
            final int ii = ((i == 0) ? pointCount - 1 : i - 1);
            DefaultLineDrawer.drawLine(
                    clip,
                    xArr[ii], yArr[ii], xArr[i], yArr[i],
                    clippedLineDrawerWithFlag);
        }

        if (!isFillElseDraw) {
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
                clippedPointDrawerWithFlag.flagManager,
                clippedPointDrawerWithFlag.xMinArr,
                clippedPointDrawerWithFlag.xMaxArr);
    }

    /*
     * 
     */
    
    private static boolean isClipClearlyInOrOutOfPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount) {
        
        boolean foundPolyPointInClip = false;
        for (int i = 0; i < pointCount; i++) {
            final int px = xArr[i];
            final int py = yArr[i];
            if (clip.contains(px, py)) {
                foundPolyPointInClip = true;
                break;
            }
        }
        if (DEBUG) {
            Dbg.log("foundPolyPointInClip = " + foundPolyPointInClip);
        }
        
        boolean foundSegmentClipOverlap = false;
        if (foundPolyPointInClip) {
            foundSegmentClipOverlap = true;
        } else {
            boolean foundInter = false;
            final double H = 0.5;
            final double eclipXMin = clip.x() - H;
            final double eclipYMin = clip.y() - H;
            final double eclipXMax = clip.xMax() + H;
            final double eclipYMax = clip.yMax() + H;
            for (int i = 0; i < pointCount; i++) {
                final int ii = ((i == 0) ? pointCount - 1 : i - 1);
                final int xA = xArr[ii];
                final int yA = yArr[ii];
                final int xB = xArr[i];
                final int yB = yArr[i];
                for (int k = 0; k < 4; k++) {
                    final double xC = (((k == 1) || (k == 2)) ? eclipXMax : eclipXMin);
                    final double yC = ((k >= 2) ? eclipYMax : eclipYMin);
                    final double xD = ((k <= 1) ? eclipXMax : eclipXMin);
                    final double yD = (((k == 1) || (k == 2)) ? eclipYMax : eclipYMin);
                    foundInter = GprimUtils.doSegmentsIntersect(
                            xA, yA, xB, yB,
                            xC, yC, xD, yD);
                    if (foundInter) {
                        if (DEBUG) {
                            Dbg.log("intersection of ("
                                    + xA + ", " + yA + ", " + xB + ", " + yB
                                    + ") with ("
                                    + xC + ", " + yC + ", " + xD + ", " + yD
                                    + ")");
                        }
                        break;
                    }
                }
                if (foundInter) {
                    break;
                }
            }
            foundSegmentClipOverlap = foundInter;
        }
        
        final boolean isClipClearlyInOrOut =
                (!foundSegmentClipOverlap);
        
        if (DEBUG) {
            Dbg.log("isClipClearlyInOrOut = " + isClipClearlyInOrOut);
        }
        
        return isClipClearlyInOrOut;
    }
    
    /*
     * 
     */
    
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
            MyFlagManager flagManager,
            int[] xMinArr,
            int[] xMaxArr) {
        
        boolean foundLitPoint = false;
        int topLeftNonLitX = Integer.MIN_VALUE;
        int topLeftNonLitY = Integer.MIN_VALUE;
        
        // Looping on cbbox pixels, top-down and left-right,
        // looking for first lit and first non-lit.
        Loop1 : for (int j = 0; j < cbbox.ySpan(); j++) {
            final int offset = j * cbbox.xSpan();
            final int xMinLit = xMinArr[j];
            final int xMaxLit = xMaxArr[j];
            if (xMinLit > xMaxLit) {
                // Nothing lit on that row.
                if (topLeftNonLitX == Integer.MIN_VALUE) {
                    topLeftNonLitX = cbbox.x();
                    topLeftNonLitY = cbbox.y() + j;
                    if (foundLitPoint) {
                        // Found both.
                        break Loop1;
                    }
                }
            } else {
                final int iMin = (xMinLit - cbbox.x());
                final int iMax;
                // Leaking up to 1 pixel past last max drawn,
                // to be able to detect first non-lit past it.
                if (xMaxLit < cbbox.xMax()) {
                    iMax = (xMaxLit - cbbox.x() + 1);
                } else {
                    iMax = (xMaxLit - cbbox.x());
                }
                // Starting at zero, to be able to detect
                // first non-lit before iMin, but then
                // jumping to iMin (or 1 if it is 0).
                for (int i = 0; i <= iMax;) {
                    final int index = offset + i;
                    final int flag = flagManager.getFlagAt(index);
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
                    if ((i == 0)
                            && (iMin != 0)) {
                        i = iMin;
                    } else {
                        i++;
                    }
                }
            }
        }

        if (!foundLitPoint) {
            final int xMid = cbbox.xMid();
            final int yMid = cbbox.yMid();
            final boolean isMidIn =
                    computeBelongingToPolygon(
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
        
        fillPolygon_someLitSomeNonLit_leftSegment(
                xArr,
                yArr,
                pointCount,
                //
                clippedLineDrawer,
                //
                cbbox,
                flagManager,
                xMinArr,
                xMaxArr,
                //
                topLeftNonLitX,
                topLeftNonLitY);
        
        fillPolygon_someLitSomeNonLit_rightSegment(
                xArr,
                yArr,
                pointCount,
                //
                clippedLineDrawer,
                //
                cbbox,
                flagManager,
                xMinArr,
                xMaxArr,
                //
                topLeftNonLitY);
        
        fillPolygon_someLitSomeNonLit_litPartsSegment(
                xArr,
                yArr,
                pointCount,
                //
                clippedLineDrawer,
                //
                cbbox,
                flagManager,
                xMinArr,
                xMaxArr,
                //
                topLeftNonLitX,
                topLeftNonLitY);
    }
    
    /*
     * 
     */

    private static void fillPolygon_someLitSomeNonLit_leftSegment(
            int[] xArr,
            int[] yArr,
            int pointCount,
            //
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            GRect cbbox,
            MyFlagManager flagManager,
            int[] xMinArr,
            int[] xMaxArr,
            //
            int topLeftNonLitX,
            int topLeftNonLitY) {

        if (DEBUG) {
            Dbg.log("fillPolygon_someLitSomeNonLit_leftSegment()");
        }

        byte leftSegmentFlag;
        if (topLeftNonLitX == cbbox.x()) {
            final boolean isIn =
                    computeBelongingToPolygon(
                            xArr,
                            yArr,
                            pointCount,
                            topLeftNonLitX,
                            topLeftNonLitY);
            leftSegmentFlag = (isIn ? FLAG_IN : FLAG_OUT);
        } else {
            leftSegmentFlag = FLAG_EDGE;
        }
        
        final int leftSegmentXMin = cbbox.x();
        
        final int j0 = topLeftNonLitY - cbbox.y();
        
        // Looping on rows.
        for (int j = j0; j < cbbox.ySpan(); j++) {
            final int xMinLit = xMinArr[j];
            final int xMaxLit = xMaxArr[j];
            final int leftSegmentXMax;
            if (xMinLit <= xMaxLit) {
                if (xMinLit == leftSegmentXMin) {
                    leftSegmentFlag = FLAG_EDGE;
                }
                leftSegmentXMax = xMinLit - 1;
            } else {
                leftSegmentXMax = cbbox.xMax();
            }

            if (leftSegmentXMin <= leftSegmentXMax) {
                final int y = cbbox.y() + j;
                if (leftSegmentFlag < FLAG_IN) {
                    final boolean isIn =
                            computeBelongingToPolygon(
                                    xArr,
                                    yArr,
                                    pointCount,
                                    leftSegmentXMin,
                                    y);
                    leftSegmentFlag = (isIn ? FLAG_IN : FLAG_OUT);
                    /*
                     * If there is a row below, to avoid belonging tests
                     * for its pixels in its [xMin + 1, xMax - 1] range,
                     * we fill our pixels in that range with the flag.
                     * This should not be a generally useful action,
                     * but in pathological cases it could speed things up nicely.
                     */
                    if (j < cbbox.ySpan() - 1) {
                        final int jj = j + 1;
                        final int xxMin = xMinArr[jj];
                        final int xxMax = xMaxArr[jj];
                        if (xxMin <= xxMax) {
                            final int ii = xxMin + 1 - cbbox.x();
                            final int indexFrom = ii + jj * cbbox.xSpan();
                            final int indexTo = ii + (xxMax - xxMin);
                            fillWithFlag(
                                    flagManager,
                                    indexFrom,
                                    indexTo,
                                    leftSegmentFlag);
                        }
                    }
                }
                if (leftSegmentFlag == FLAG_IN) {
                    drawHorizontalLineInClip(
                            clippedLineDrawer,
                            leftSegmentXMin, leftSegmentXMax, y);
                }
            }
        }
    }

    private static void fillPolygon_someLitSomeNonLit_rightSegment(
            int[] xArr,
            int[] yArr,
            int pointCount,
            //
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            GRect cbbox,
            MyFlagManager flagManager,
            int[] xMinArr,
            int[] xMaxArr,
            //
            int topLeftNonLitY) {

        if (DEBUG) {
            Dbg.log("fillPolygon_someLitSomeNonLit_rightSegment()");
        }

        byte rightSegmentFlag;
        {
            final boolean isIn =
                    computeBelongingToPolygon(
                            xArr,
                            yArr,
                            pointCount,
                            cbbox.xMax(),
                            topLeftNonLitY);
            rightSegmentFlag = (isIn ? FLAG_IN : FLAG_OUT);
        }
        
        final int rightSegmentXMax = cbbox.xMax();
        
        final int j0 = topLeftNonLitY - cbbox.y();
        
        // Looping on rows.
        for (int j = j0; j < cbbox.ySpan(); j++) {
            final int xMinLit = xMinArr[j];
            final int xMaxLit = xMaxArr[j];
            if (xMinLit > xMaxLit) {
                // Nothing lit.
                // Row already taken care of by left segment algo.
                continue;
            }
            if (xMaxLit == rightSegmentXMax) {
                rightSegmentFlag = FLAG_EDGE;
            }
            final int rightSegmentXMin = xMaxLit + 1;

            if (rightSegmentXMin <= rightSegmentXMax) {
                final int y = cbbox.y() + j;
                if (rightSegmentFlag < FLAG_IN) {
                    final boolean isIn =
                            computeBelongingToPolygon(
                                    xArr,
                                    yArr,
                                    pointCount,
                                    rightSegmentXMax,
                                    y);
                    rightSegmentFlag = (isIn ? FLAG_IN : FLAG_OUT);
                }
                if (rightSegmentFlag == FLAG_IN) {
                    drawHorizontalLineInClip(
                            clippedLineDrawer,
                            rightSegmentXMin, rightSegmentXMax, y);
                }
            }
        }
    }

    private static void fillPolygon_someLitSomeNonLit_litPartsSegment(
            int[] xArr,
            int[] yArr,
            int pointCount,
            //
            InterfaceClippedLineDrawer clippedLineDrawer,
            //
            GRect cbbox,
            MyFlagManager flagManager,
            int[] xMinArr,
            int[] xMaxArr,
            //
            int topLeftNonLitX,
            int topLeftNonLitY) {

        if (DEBUG) {
            Dbg.log("fillPolygon_someLitSomeNonLit_litPartsSegment()");
        }

        final int j0 = topLeftNonLitY - cbbox.y();

        // Looping on rows.
        for (int j = j0; j < cbbox.ySpan(); j++) {
            final int xMinLit = xMinArr[j];
            final int xMaxLit = xMaxArr[j];
            if (xMinLit > xMaxLit) {
                // Nothing lit.
                // Row already taken care of by left segment algo.
                continue;
            }
            
            final int iMinLit = (xMinLit - cbbox.x());
            final int iMaxLit = (xMaxLit - cbbox.x());

            final int y = cbbox.y() + j;
            final int iMin = iMinLit + 1;
            final int iMax = iMaxLit - 1;

            // A segment is a connex part of the line
            // which pixels are either all IN or all OUT.
            // Only valid if >= 0.
            int segmentStartI = -1;
            byte segmentFlag = -1;
            
            // Looping on columns.
            final int indexOffset = j * cbbox.xSpan();
            int i;
            for (i = iMin; i <= iMax; i++) {
                if (segmentFlag >= FLAG_IN) {
                    i = copySegmentFlagWhilePending(
                            segmentFlag,
                            flagManager,
                            indexOffset,
                            i,
                            iMax);
                    if (i > iMax) {
                        // Copied up to iMax.
                        break;
                    }
                    // Reached an EDGE pixel at i <= iMax.
                } else {
                    final int curIndex = indexOffset + i;
                    final byte curFlag = flagManager.getFlagAt(curIndex);
                    if (curFlag == FLAG_PENDING) {
                        if (segmentStartI < 0) {
                            // First row pixel encountered,
                            // or first pixel after an EDGE pixel.
                            segmentStartI = i;
                        }

                        /*
                         * Looking the pixel above,
                         * and if it's IN or OUT,
                         * copying the flag up to current pixel.
                         */
                        if (j > 0) {
                            final int aboveIndex = curIndex - cbbox.xSpan();
                            final byte aboveFlag = flagManager.getFlagAt(aboveIndex);
                            if (aboveFlag >= FLAG_IN) {
                                flagManager.setFlagAt(curIndex, aboveFlag);
                                if (segmentStartI < 0) {
                                    segmentStartI = i;
                                    segmentFlag = aboveFlag;
                                } else if (segmentFlag < 0) {
                                    segmentFlag = aboveFlag;
                                    final int indexFrom = curIndex - (i - segmentStartI);
                                    final int indexTo = curIndex - 1;
                                    fillWithFlag(
                                            flagManager,
                                            indexFrom,
                                            indexTo,
                                            segmentFlag);
                                }
                                /*
                                 * Segment flag determined.
                                 * Will copy keep copying it while pixels are PENDING.
                                 */
                            } else {
                                /*
                                 * Above is EDGE, or PENDING, since when taking care
                                 * of "left" and "right" segments, we don't necessarily
                                 * bother to set flags in pixels.
                                 * Will check next above pixel.
                                 */
                            }
                        } else {
                            /*
                             * No row above: no flag to copy.
                             * Will compute flag at end of loop.
                             */
                        }
                        continue;
                    } else {
                        /*
                         * Not PENDING, must be EDGE (already drawn).
                         */
                        if (curFlag != FLAG_EDGE) {
                            throw new AssertionError("" + curFlag);
                        }
                    }
                }
                
                /*
                 * Here, row pixel at index "i" is EDGE. 
                 */

                if (segmentStartI >= 0) {
                    final int segmentStartX = cbbox.x() + segmentStartI;
                    final int x = cbbox.x() + i;
                    final int segmentEndX = x - 1;
                    onEdgeOrRowEndReachedWithStartedSegment(
                            xArr,
                            yArr,
                            pointCount,
                            //
                            cbbox,
                            flagManager,
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

            /*
             * Drawing line of IN pixels, if any to draw.
             */
            final int x = cbbox.x() + i;
            if (segmentStartI >= 0) {
                final int segmentStartX = cbbox.x() + segmentStartI;
                final int segmentEndX = x - 1;
                onEdgeOrRowEndReachedWithStartedSegment(
                        xArr,
                        yArr,
                        pointCount,
                        //
                        cbbox,
                        flagManager,
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
    private static void onEdgeOrRowEndReachedWithStartedSegment(
            int[] xArr,
            int[] yArr,
            int pointCount,
            //
            GRect cbbox,
            MyFlagManager flagManager,
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
            final boolean isIn =
                    computeBelongingToPolygon(
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
                    flagManager,
                    indexFrom,
                    indexTo,
                    segmentFlag);
        }
        if (segmentFlag == FLAG_IN) {
            drawHorizontalLineInClip(
                    clippedLineDrawer,
                    segmentStartX, segmentEndX, y);
        }
    }
    
    private static void drawHorizontalLineInClip(
            InterfaceClippedLineDrawer clippedLineDrawer,
            int x1, int x2, int y) {
        if (DEBUG) {
            Dbg.log("drawHorizontalLineInClip(," +  x1 + ", " + x2 + ", " + y + ")");
        }
        clippedLineDrawer.drawHorizontalLineInClip(
                x1, x2,
                y,
                1, GprimUtils.PLAIN_PATTERN, 0);
    }
    
    /*
     * 
     */
    
    /**
     * To copy flag from above row to current row,
     * while current flag is PENDING and above flag
     * is IN or OUT.
     * 
     * Code extracted into this small method,
     * because it's kind of a hot spot,
     * that ought to be optimized.
     * 
     * @param segmentFlag Must be FLAG_IN or FLAG_OUT.
     * @param i Must be <= iMax.
     * @return Last checked index, post-copy (can be iMax +1).
     */
    private static int copySegmentFlagWhilePending(
            byte segmentFlag,
            MyFlagManager flagManager,
            int indexOffset,
            int i,
            int iMax) {
        while (i <= iMax) {
            final int curIndex = indexOffset + i;
            if (flagManager.setFlagAtIfIsPending(curIndex, segmentFlag)) {
                break;
            }
            i++;
        }
        return i;
    }
    
    /**
     * For use when segment flag gets know after segment start,
     * to set previous (and PENDING) segment pixels
     * to the now known flag.
     */
    private static void fillWithFlag(
            MyFlagManager flagManager,
            int indexFrom,
            int indexTo,
            byte flag) {
        for (int index = indexFrom; index <= indexTo; index++) {
            flagManager.setFlagAt(index, flag);
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
     * @return Reusable flagByIndex array.
     */
    private static byte[] getFlagByIndex(
            MyTemps temps,
            int minCapacity) {
        if (minCapacity > MAX_FLAG_ARR_REUSE_CAPACITY) {
            return new byte[minCapacity];
        }

        byte[] ret = temps.tmpFlagByIndex;
        if (ret.length < minCapacity) {
            int newCap = Math.max(minCapacity, (ret.length << 1));
            newCap = Math.min(newCap, MAX_FLAG_ARR_REUSE_CAPACITY);
            ret = new byte[newCap];
            temps.tmpFlagByIndex = ret;
        }
        return ret;
    }
    
    /**
     * @return Reusable X max or min array.
     */
    private static int[] getXArr(
            MyTemps temps,
            int minCapacity,
            boolean maxElseMin) {
        if (minCapacity > MAX_X_ARR_REUSE_CAPACITY) {
            return new int[minCapacity];
        }

        int[] ret = (maxElseMin ? temps.tmpXMaxArr : temps.tmpXMinArr);
        if (ret.length < minCapacity) {
            int newCap = Math.max(minCapacity, (ret.length << 1));
            newCap = Math.min(newCap, MAX_X_ARR_REUSE_CAPACITY);
            ret = new int[newCap];
            if (maxElseMin) {
                temps.tmpXMaxArr = ret;
            } else {
                temps.tmpXMinArr = ret;
            }
        }
        return ret;
    }
}
