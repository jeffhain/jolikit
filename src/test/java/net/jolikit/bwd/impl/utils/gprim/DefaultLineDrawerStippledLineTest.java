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
package net.jolikit.bwd.impl.utils.gprim;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;

public class DefaultLineDrawerStippledLineTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    private static final int MAX_FACTOR = 5;
    private static final int MAX_PIXEL_NUM = 5;
    
    /*
     * Comprehensive.
     */
    
    private static final int MAX_COMPREHENSIVE_SEGMENT_SPAN = 25;
    
    private static final short[] COMP_STIPPLE_PATTERN_ARR = new short[]{
        0x0000,
        0x0001,
        0x1234,
        GprimUtils.PLAIN_PATTERN
    };
    
    /*
     * Random.
     */
    
    private static final int NBR_OF_CALLS_RANDOM = 1000;
    
    private static final int MAX_RANDOM_SEGMENT_SPAN = 50;

    private static final double SPECIAL_PATTERN_PROBA = 10 * (1.0/NBR_OF_CALLS_RANDOM);
    
    /*
     * 
     */
    
    private static final int NBR_OF_CALLS_COMPREHENSIVE =
            NbrsUtils.pow2(MAX_COMPREHENSIVE_SEGMENT_SPAN + 1);
    private static final int NBR_OF_CALLS = NBR_OF_CALLS_COMPREHENSIVE + NBR_OF_CALLS_RANDOM;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Random random = TestUtils.newRandom123456789L();
    
    private final GprimTestUtilz utilz = new GprimTestUtilz(this.random);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DefaultLineDrawerStippledLineTest() {
    }
    
    /*
     * 
     */
    
    public void test_drawer_bBoxAsClip() {
        final boolean mustUseBBoxAsClip = true;
        this.test_drawer_generic(mustUseBBoxAsClip);
    }
    
    public void test_drawer_clipNearbyBBox() {
        final boolean mustUseBBoxAsClip = false;
        this.test_drawer_generic(mustUseBBoxAsClip);
    }
    
    public void test_drawer_generic(boolean mustUseBBoxAsClip) {
        
        final TestClippedPointDrawer clippedPointDrawer = new TestClippedPointDrawer();
        final Map<GPoint,Integer> paintedCountByPixel = clippedPointDrawer.paintedCountByPixel;
        
        final DefaultLineDrawer drawer = new DefaultLineDrawer(clippedPointDrawer);
        
        for (int index = 0; index < NBR_OF_CALLS; index++) {
            
            final boolean isComprehensive = (index < NBR_OF_CALLS_COMPREHENSIVE);
            
            final int x1;
            final int y1;
            final int x2;
            final int y2;
            final int factor;
            final short pattern;
            final int pixelNum;
            if (isComprehensive) {
                final int spanIndex = index / COMP_STIPPLE_PATTERN_ARR.length;
                final int stippleIndex = index % COMP_STIPPLE_PATTERN_ARR.length;
                
                // Prime coordinates for P1.
                x1 = 3;
                y1 = 7;
                x2 = spanIndex % (MAX_COMPREHENSIVE_SEGMENT_SPAN + 1);
                y2 = spanIndex / (MAX_COMPREHENSIVE_SEGMENT_SPAN + 1);
                
                factor = 1;
                pattern = COMP_STIPPLE_PATTERN_ARR[stippleIndex];
                pixelNum = 0;
            } else {
                x1 = this.randomMinusIntInt(1 + MAX_RANDOM_SEGMENT_SPAN / 2);
                y1 = this.randomMinusIntInt(1 + MAX_RANDOM_SEGMENT_SPAN / 2);
                if (this.randomBoolean(0.1)) {
                    // Special cases: horizontal, vertical, point.
                    if (this.random.nextBoolean()) {
                        x2 = x1;
                    } else {
                        x2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
                    }
                    if (this.random.nextBoolean()) {
                        y2 = y1;
                    } else {
                        y2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
                    }
                } else {
                    x2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
                    y2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
                }
                
                factor = 1 + this.random.nextInt(MAX_FACTOR);
                pattern = this.randomPattern();
                pixelNum = this.random.nextInt(MAX_PIXEL_NUM + 1);
            }
            
            final TestLineArgs drawingArgs = new TestLineArgs(
                    x1,
                    y1,
                    x2,
                    y2,
                    //
                    factor,
                    pattern,
                    pixelNum);
            if (DEBUG) {
                Dbg.log();
                Dbg.log("drawingArgs = " + drawingArgs);
            }

            final GRect drawingBBox = drawingArgs.computeBoundingBox();
            if (DEBUG) {
                Dbg.log("drawingBBox = " + drawingBBox);
            }
            
            final GRect clip;
            if (mustUseBBoxAsClip) {
                clip = drawingBBox;
            } else {
                clip = this.utilz.randomClipNearRect(drawingBBox);
            }
            if (DEBUG) {
                Dbg.log("clip = " + clip);
            }
            
            final GRect clippedBBox = drawingBBox.intersected(clip);
            if (DEBUG) {
                Dbg.log("clippedBBox = " + clippedBBox);
            }
            clippedPointDrawer.setClippedBBox(clippedBBox);
            
            final Set<Integer> expectedDrawnCoordSet =
                    computeExpectedDrawByPixelCoordSet(
                            drawingArgs,
                            clippedBBox);
            if (DEBUG) {
                Dbg.log("expectedDrawnCoordSet = " + expectedDrawnCoordSet);
            }
            
            /*
             * 
             */
            
            paintedCountByPixel.clear();
            final int newPixelNum = drawer.drawLine(
                    clip,
                    x1, y1, x2, y2,
                    factor, pattern, pixelNum);
            if (DEBUG) {
                Dbg.log("newPixelNum = " + newPixelNum);
            }
            
            /*
             * 
             */
            
            final int expectedNewPixelNum;
            if (GprimUtils.mustComputePixelNum(pattern)) {
                expectedNewPixelNum = GprimUtils.pixelNumPlusSegmentPixelLengthNormalized(
                        x1, y1, x2, y2,
                        factor,
                        pixelNum);
            } else {
                expectedNewPixelNum = pixelNum;
            }
            final boolean isNewPixelNumWrong =
                    (newPixelNum != expectedNewPixelNum);

            /*
             * 
             */
            
            final Map<GPoint,TestPixelStatus> statusByPixel =
                    computeStatusByPixelInDrawingBBox(
                            drawingArgs,
                            drawingBBox,
                            clip,
                            expectedDrawnCoordSet,
                            paintedCountByPixel);

            final boolean foundPixelOutOfClippedBBox =
                    GprimTestUtilz.computeFoundPixelOutOfClippedBBox(
                            DEBUG,
                            clippedBBox,
                            paintedCountByPixel);
            
            /*
             * 
             */
            
            boolean foundMissingPixel = false;
            boolean foundExceedingPixel = false;
            boolean foundMultipaintedPixel = false;
            {
                final boolean isFillElseDraw = false;
                final boolean[] resArr = GprimTestUtilz.computePixelPaintingIssues(
                        DEBUG,
                        statusByPixel,
                        isFillElseDraw);
                foundMissingPixel = resArr[0];
                foundExceedingPixel = resArr[1];
                foundMultipaintedPixel = resArr[2];
            }
            
            /*
             * 
             */

            final boolean valid =
                    (!foundPixelOutOfClippedBBox)
                    && (!foundMissingPixel)
                    && (!foundExceedingPixel)
                    && (!foundMultipaintedPixel)
                    && (!isNewPixelNumWrong);

            if ((!valid)
                    || DEBUG) {
                Dbg.log();
                Dbg.log("statusByPixel in bounding box:");
                GprimTestUtilz.logStatusByPixel(statusByPixel);
                Dbg.log("drawingArgs = " + drawingArgs);
                Dbg.log("drawingBBox = " + drawingBBox);
                Dbg.log("clip = " + clip);
                Dbg.log("clippedBBox = " + clippedBBox);
                Dbg.log("foundPixelOutOfClippedBBox = " + foundPixelOutOfClippedBBox);
                Dbg.log("foundMissingPixel = " + foundMissingPixel);
                Dbg.log("foundExceedingPixel = " + foundExceedingPixel);
                Dbg.log("foundMultipaintedPixel = " + foundMultipaintedPixel);
                Dbg.log("isNewPixelNumWrong = " + isNewPixelNumWrong
                        + " (expected = " + expectedNewPixelNum + ", actual = " + newPixelNum + ")");
                if (!valid) {
                    throw new AssertionError();
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private boolean randomBoolean(double proba) {
        return GprimTestUtilz.randomBoolean(this.random, proba);
    }
    
    /**
     * @return Random value in [-value,value]
     */
    private int randomMinusIntInt(int value) {
        return GprimTestUtilz.randomMinusIntInt(this.random, value);
    }
    
    /*
     * 
     */

    private short randomPattern() {
        final short pattern;
        if (this.randomBoolean(SPECIAL_PATTERN_PROBA)) {
            pattern = (this.random.nextBoolean() ? 0 : GprimUtils.PLAIN_PATTERN);
        } else {
            pattern = (short) this.random.nextInt();
        }
        return pattern;
    }
    
    /*
     * 
     */
    
    private static Set<Integer> computeExpectedDrawByPixelCoordSet(
            TestLineArgs drawingArgs,
            GRect clippedBBox) {
        final int coord0;
        final int coordN;
        final int clippedMinCoord;
        final int clippedMaxCoord;
        if (mustIterateOnXForStipple(drawingArgs)) {
            coord0 = drawingArgs.x1;
            coordN = drawingArgs.x2;
            clippedMinCoord = clippedBBox.x();
            clippedMaxCoord = clippedBBox.xMax();
        } else {
            coord0 = drawingArgs.y1;
            coordN = drawingArgs.y2;
            clippedMinCoord = clippedBBox.y();
            clippedMaxCoord = clippedBBox.yMax();
        }
        
        final Set<Integer> res = new TreeSet<Integer>();
        
        final int n;
        final int step;
        if (coordN < coord0) {
            n = (coord0 - coordN + 1);
            step = -1;
        } else {
            n = (coordN - coord0 + 1);
            step = 1;
        }
        
        int coord = coord0;
        for (int i = 0; i < n; i++, coord += step) {
            if ((coord < clippedMinCoord) || (coord > clippedMaxCoord)) {
                continue;
            }
            final int coordIndex = drawingArgs.pixelNum + step * (coord - coord0);
            final boolean drawExpected = LineStippleUtils.mustDraw(
                    drawingArgs.factor,
                    drawingArgs.pattern,
                    coordIndex);
            if (drawExpected) {
                res.add(coord);
            }
        }
        
        return res;
    }
    
    private static boolean mustIterateOnXForStipple(TestLineArgs drawingArgs) {
        final int dx = (drawingArgs.x2 - drawingArgs.x1);
        final int dy = (drawingArgs.y2 - drawingArgs.y1);
        final boolean mustIterateOnX = (Math.abs(dx) > Math.abs(dy));
        if (DEBUG) {
            Dbg.log("mustIterateOnX for stipple = " + mustIterateOnX);
        }
        return mustIterateOnX;
    }
    
    private static Map<GPoint,TestPixelStatus> computeStatusByPixelInDrawingBBox(
            TestLineArgs drawingArgs,
            GRect drawingBBox,
            GRect clip,
            Set<Integer> expectedDrawnCoordSet,
            Map<GPoint,Integer> paintedCountByPixel) {
        
        final Map<GPoint,TestPixelStatus> statusByPixel =
                new TreeMap<GPoint,TestPixelStatus>();

        final Map<GPoint,Integer> plainPaintedCountByPixel;
        {
            final TestClippedPointDrawer plainClippedPointDrawer = new TestClippedPointDrawer();
            
            final GRect clippedBBox = drawingBBox.intersected(clip);
            plainClippedPointDrawer.setClippedBBox(clippedBBox);
            
            plainPaintedCountByPixel = plainClippedPointDrawer.paintedCountByPixel;
            final DefaultLineDrawer plainDrawer = new DefaultLineDrawer(plainClippedPointDrawer);
            plainDrawer.drawLine(
                    clip,
                    drawingArgs.x1,
                    drawingArgs.y1,
                    drawingArgs.x2,
                    drawingArgs.y2);
        }
        
        final boolean mustUseX = mustIterateOnXForStipple(drawingArgs);
        
        for (int j = 0; j < drawingBBox.ySpan(); j++) {
            final int y = drawingBBox.y() + j;
            for (int i = 0; i < drawingBBox.xSpan(); i++) {
                final int x = drawingBBox.x() + i;
                final GPoint pixel = GPoint.valueOf(x, y);
                
                final boolean pixelInClip = clip.contains(pixel.x(), pixel.y());
                
                final int coord = (mustUseX ? pixel.x() : pixel.y());
                final boolean drawExpected =
                        pixelInClip // NB: optional check, since not plain-painted if not in clip.
                        && plainPaintedCountByPixel.containsKey(pixel)
                        && expectedDrawnCoordSet.contains(coord);
                
                final PixelFigStatus pixelFigStatus;
                if (drawExpected) {
                    pixelFigStatus = PixelFigStatus.PIXEL_REQUIRED;
                } else {
                    pixelFigStatus = PixelFigStatus.PIXEL_NOT_ALLOWED;
                }
                
                final Integer paintCount = paintedCountByPixel.get(pixel);
                
                final TestPixelStatus status = GprimTestUtilz.computePixelStatus(
                        paintCount,
                        pixelInClip,
                        pixelFigStatus);
                
                statusByPixel.put(pixel, status);
            }
        }
        
        return statusByPixel;
    }
}
