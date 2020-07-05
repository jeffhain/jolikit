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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.Dbg;

/**
 * For tests for (point, line, rect, oval, arc) drawers/fillers.
 * 
 * @param ARG Type for storing arguments for tested method call.
 */
public abstract class AbstractDrawerTezt<ARG> extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    private static final long RANDOM_SEED = 123456789L;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    protected final Random random = new Random(RANDOM_SEED);

    private final GprimTestUtilz utilz = new GprimTestUtilz(this.random);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractDrawerTezt() {
    }
    
    public void test_drawer_bBoxAsClip() {
        final boolean mustUseBBoxAsClip = true;
        this.test_drawer_generic(mustUseBBoxAsClip);
    }
    
    public void test_drawer_clipNearbyBBox() {
        final boolean mustUseBBoxAsClip = false;
        this.test_drawer_generic(mustUseBBoxAsClip);
    }
    
    /**
     * Tests according to abstract methods implementations.
     */
    public void test_drawer_generic(
            boolean mustUseBBoxAsClip) {
        final TestClippedPointDrawer clippedPointDrawer = new TestClippedPointDrawer();
        final SortedMap<GPoint,Integer> paintCountByPixel = clippedPointDrawer.paintedCountByPixel;

        final int nbrOfCalls = this.getNbrOfCalls();
        
        final AbstractDrawerTestHelper<ARG> helper = this.newDrawerTestHelper(clippedPointDrawer);

        for (int i = 0; i < nbrOfCalls; i++) {

            final ARG drawingArgs = this.newDrawingArgs(i);
            final boolean multipaintedPixelsAllowed =
                    this.mustAllowMultipaintedPixels(drawingArgs);
            if (DEBUG) {
                Dbg.log();
                Dbg.log("drawingArgs = " + drawingArgs);
                Dbg.log("multipaintedPixelsAllowed = " + multipaintedPixelsAllowed);
            }

            final GRect drawingBBox = helper.computeBoundingBox(drawingArgs);
            if (DEBUG) {
                Dbg.log("drawingBBox = " + drawingBBox);
            }
            
            final GRect clip;
            if (mustUseBBoxAsClip) {
                clip = drawingBBox;
            } else {
                clip = this.utilz.randomClipNearRect(drawingBBox);
            }
            
            /*
             * Drawing, and checking it.
             */
            
            paintCountByPixel.clear();
            if (DEBUG) {
                Dbg.log("BEGIN calling draw method");
            }
            helper.callDrawMethod(clip, drawingArgs);
            if (DEBUG) {
                Dbg.log("END calling draw method");
            }
            
            {
                final SortedMap<GPoint,Integer> drawnCountByPixel = paintCountByPixel;
                
                final boolean isFillElseDraw = false;
                this.checkPaintedCountByPixel(
                        helper,
                        clip,
                        drawingArgs,
                        multipaintedPixelsAllowed,
                        drawingBBox,
                        drawnCountByPixel,
                        isFillElseDraw);
            }
            
            /*
             * Filling, and checking it, if supported.
             */
            
            if (helper.isFillSupported()) {
                final SortedMap<GPoint,Integer> drawnCountByPixel =
                        new TreeMap<GPoint,Integer>(paintCountByPixel);
                
                paintCountByPixel.clear();
                if (DEBUG) {
                    Dbg.log("BEGIN calling fill method");
                }
                helper.callFillMethod(clip, drawingArgs);
                if (DEBUG) {
                    Dbg.log("END calling fill method");
                }
                final SortedMap<GPoint,Integer> filledCountByPixel = paintCountByPixel;
                
                final boolean isFillElseDraw = true;
                this.checkPaintedCountByPixel(
                        helper,
                        clip,
                        drawingArgs,
                        multipaintedPixelsAllowed,
                        drawingBBox,
                        filledCountByPixel,
                        isFillElseDraw);
                
                this.checkPaintedOnFillIfPaintedOnDraw(
                        drawnCountByPixel.keySet(),
                        filledCountByPixel.keySet());
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected abstract AbstractDrawerTestHelper<ARG> newDrawerTestHelper(
            InterfaceClippedPointDrawer clippedPointDrawer);
    
    protected abstract int getNbrOfCalls();
    
    /**
     * Called getNbrOfCalls() times.
     * 
     * @param index From 0 upward.
     * @return The arguments for next draw call, and next fill call if supported.
     */
    protected abstract ARG newDrawingArgs(int index);

    /**
     * This default implementation returns false.
     * 
     * Called after each call to newDrawingArgs().
     * 
     * Useful for implementations that are faster
     * when eventually painting some pixels multiple times,
     * while still being correct due to only doing it
     * when color is opaque.
     */
    protected boolean mustAllowMultipaintedPixels(ARG drawingArgs) {
        return false;
    }

    /*
     * Utilities.
     */
    
    protected boolean randomBoolean(double proba) {
        return GprimTestUtilz.randomBoolean(this.random, proba);
    }
    
    /**
     * @return Random value in [-1.0,1.0[
     */
    protected double randomMinusOneOne() {
        return GprimTestUtilz.randomMinusOneOne(this.random);
    }
    
    /**
     * @return Random value in [-value,value]
     */
    protected int randomMinusIntInt(int value) {
        return GprimTestUtilz.randomMinusIntInt(this.random, value);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * Checks that pixels painted on draw are also
     * all painted on fill.
     * 
     * We want that for consistency,
     * else user will be quite surprised.
     */
    private void checkPaintedOnFillIfPaintedOnDraw(
            Set<GPoint> drawnPixelSet,
            Set<GPoint> filledPixelSet) {
        /*
         * Checking that pixels painted on draw are also
         * all painted on fill.
         */
        
        final SortedSet<GPoint> drawnNotFilledSet = new TreeSet<GPoint>();
        drawnNotFilledSet.addAll(drawnPixelSet);
        drawnNotFilledSet.removeAll(filledPixelSet);
        
        boolean foundDrawnPixelsNotFilled = false;
        
        if (drawnNotFilledSet.size() != 0) {
            foundDrawnPixelsNotFilled = true;
            if (DEBUG) {
                for (GPoint pixel : drawnNotFilledSet) {
                    Dbg.log("drawn but not filled : " + pixel);
                }
            }
        }

        if (foundDrawnPixelsNotFilled) {
            throw new AssertionError("some pixels drawn but not filled");
        }
    }

    /**
     * @param paintCount Can be null.
     */
    private static TestPixelStatus computePixelStatus(
            Integer paintCount,
            boolean isFillElseDraw,
            boolean pixelInClip,
            PixelFigStatus pixelFigStatus) {
        return GprimTestUtilz.computePixelStatus(
                paintCount,
                pixelInClip,
                pixelFigStatus);
    }
    
    private SortedMap<GPoint,TestPixelStatus> computeStatusByPixelInDrawingBBox(
            AbstractDrawerTestHelper<ARG> helper,
            ARG drawingArgs,
            GRect drawingBBox,
            GRect clip,
            Map<GPoint,Integer> paintedCountByPixel,
            boolean isFillElseDraw) {
        
        final SortedMap<GPoint,TestPixelStatus> statusByPixel =
                new TreeMap<GPoint,TestPixelStatus>();
        
        for (int j = 0; j < drawingBBox.ySpan(); j++) {
            final int y = drawingBBox.y() + j;
            for (int i = 0; i < drawingBBox.xSpan(); i++) {
                final int x = drawingBBox.x() + i;
                final GPoint pixel = GPoint.valueOf(x, y);
                
                final boolean pixelInClip = clip.contains(pixel.x(), pixel.y());
                
                final PixelFigStatus pixelFigStatus =
                        helper.computePixelFigStatus(
                                drawingArgs,
                                drawingBBox,
                                isFillElseDraw,
                                pixel);

                final Integer paintCount = paintedCountByPixel.get(pixel);
                final TestPixelStatus status = computePixelStatus(
                        paintCount,
                        isFillElseDraw,
                        pixelInClip,
                        pixelFigStatus);
                
                statusByPixel.put(pixel, status);
            }
        }
        
        return statusByPixel;
    }

    /**
     * Must throw if not valid, or just log if you want to keep going.
     * @param paintedCountByPixel Must have no key when not painted.
     */
    private void checkPaintedCountByPixel(
            AbstractDrawerTestHelper<ARG> helper,
            GRect clip,
            ARG drawingArgs,
            boolean multipaintedPixelsAllowed,
            GRect drawingBBox,
            Map<GPoint,Integer> paintedCountByPixel,
            boolean isFillElseDraw) {
        
        final GRect clippedBBox = drawingBBox.intersected(clip);
        if (DEBUG) {
            Dbg.log("clippedBBox = " + clippedBBox);
        }
        
        /*
         * Checking that pixels are in clip
         * and in bounding box.
         */
        
        final boolean foundPixelOutOfClippedBBox =
                GprimTestUtilz.computeFoundPixelOutOfClippedBBox(
                        DEBUG,
                        clippedBBox,
                        paintedCountByPixel);
        
        /*
         * Checking that pixels are paint as expected.
         */
        
        final SortedMap<GPoint,TestPixelStatus> statusByPixel =
                this.computeStatusByPixelInDrawingBBox(
                        helper,
                        drawingArgs,
                        drawingBBox,
                        clip,
                        paintedCountByPixel,
                        isFillElseDraw);
        
        boolean foundMissingPixel = false;
        boolean foundExceedingPixel = false;
        boolean foundMultipaintedPixel = false;
        {
            final boolean[] resArr = GprimTestUtilz.computePixelPaintingIssues(
                    DEBUG,
                    statusByPixel,
                    isFillElseDraw);
            foundMissingPixel = resArr[0];
            foundExceedingPixel = resArr[1];
            foundMultipaintedPixel = resArr[2];
        }
        
        /*
         * Checking that each pixel, if not on clip border,
         * has at least two non-adjacent neighbors,
         * to check that there is no gap along the curve.
         * 
         * Not bothering to check that in case of filling,
         * for which we instead count on check for required pixels
         * and check that all pixels painted on draw are also painted
         * on fill.
         */
        
        final long maxNbrOfDanglingPixels =
                helper.getAllowedNbrOfDanglingPixels(
                        isFillElseDraw,
                        drawingArgs);
        final long nbrOfDanglingPixels =
                computeNbrOfDanglingPixels(
                        clippedBBox,
                        paintedCountByPixel.keySet());
        final boolean foundTooManyDanglingPixels =
                (nbrOfDanglingPixels > maxNbrOfDanglingPixels);
        
        /*
         * 
         */

        final boolean valid =
                (!foundPixelOutOfClippedBBox)
                && (!foundMissingPixel)
                && (!foundExceedingPixel)
                && ((!foundMultipaintedPixel) || multipaintedPixelsAllowed)
                && (!foundTooManyDanglingPixels);

        if ((!valid)
                || DEBUG) {
            Dbg.log();
            Dbg.log("isFillElseDraw = " + isFillElseDraw);
            Dbg.log("statusByPixel in bounding box:");
            GprimTestUtilz.logStatusByPixel(statusByPixel);
            Dbg.log("drawingArgs = " + drawingArgs);
            Dbg.log("drawingBBox = " + drawingBBox);
            Dbg.log("clip = " + clip);
            Dbg.log("clippedBBox = " + clippedBBox);
            Dbg.log("foundPixelOutOfClippedBBox = " + foundPixelOutOfClippedBBox);
            Dbg.log("foundMissingPixel = " + foundMissingPixel);
            Dbg.log("foundExceedingPixel = " + foundExceedingPixel);
            Dbg.log("multipaintedPixelsAllowed = " + multipaintedPixelsAllowed);
            Dbg.log("foundMultipaintedPixel = " + foundMultipaintedPixel);
            Dbg.log(
                    "foundTooManyDanglingPixels = " + foundTooManyDanglingPixels
                    + " (found = " + nbrOfDanglingPixels + ", max = " + maxNbrOfDanglingPixels + ")");
            if (!valid) {
                throw new AssertionError();
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * NB: Our bounding box in these tests is allowed to leak
     * outside actual accurate bounding box, except when test
     * implementations know it does not, and take that into account.
     * 
     * @return The number of pixels with no more than one neighbor,
     *         or with only two adjacent neighbors,
     *         that are not on clipped bounding box border.
     */
    private static long computeNbrOfDanglingPixels(
            GRect clippedBBox,
            Set<GPoint> pixelSet) {
        
        long danglingCount = 0;
        
        if (DEBUG) {
            Dbg.log("pixelSet = " + pixelSet);
        }

        for (GPoint pixel : pixelSet) {
            if ((pixel.x() == clippedBBox.x())
                    || (pixel.x() == clippedBBox.xMax())
                    || (pixel.y() == clippedBBox.y())
                    || (pixel.y() == clippedBBox.yMax())) {
                continue;
            }
            
            final TreeSet<GPoint> neighbors = new TreeSet<GPoint>();
            final int x = pixel.x();
            final int y = pixel.y();
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    if ((i != 0) || (j != 0)) {
                        // No need to bother with overflows,
                        // since drawn are done in clips.
                        putIfExists(pixelSet, x + i, y + j, neighbors);
                    }
                }
            }

            if (neighbors.size() <= 1) {
                if (DEBUG) {
                    Dbg.log("dangling : " + pixel + " (1 neighbor)");
                }
                danglingCount++;
            } else if (neighbors.size() == 2) {
                GPoint p1 = neighbors.first();
                GPoint p2 = neighbors.last();
                if (areAdjacent(p1, p2)) {
                    if (DEBUG) {
                        Dbg.log("dangling : " + pixel + "(" + p1 + " and " + p2 + " adjacent)");
                    }
                    danglingCount++;
                }
            }
        }
        
        if (DEBUG) {
            Dbg.log("danglingCount = " + danglingCount);
        }
        
        return danglingCount;
    }

    /**
     * @return True if both pixel have a common side (4 possibilities).
     */
    private static boolean areAdjacent(GPoint p1, GPoint p2) {
        if ((p1.x() == p2.x())
                && (Math.abs(p1.y() - p2.y()) == 1)) {
            return true;
        }
        if ((p1.y() == p2.y())
                && (Math.abs(p1.x() - p2.x()) == 1)) {
            return true;
        }
        return false;
    }
    
    private static void putIfExists(Set<GPoint> pixelSetIn, int x, int y, Set<GPoint> pixelSetOut) {
        final GPoint point = GPoint.valueOf(x, y);
        if (pixelSetIn.contains(point)) {
            pixelSetOut.add(point);
        }
    }
}
