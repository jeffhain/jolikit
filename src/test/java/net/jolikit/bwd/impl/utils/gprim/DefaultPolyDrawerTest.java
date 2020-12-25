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

import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;

public class DefaultPolyDrawerTest extends AbstractDrawerTezt<TestPolyArgs> {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final boolean ALLOW_HUGE_COORDS = true;
    
    private static final int NBR_OF_CALLS_SMALL_SPANS = 10 * 1000;
    private static final int NBR_OF_CALLS_LARGE_SPANS = 100;
    
    /**
     * To be able to check a lot of polygons (fast),
     * in particular possibly causing to fill the bounding box
     * with just edges drawing.
     */
    private static final int MAX_SPAN_SMALL = 13;
    
    /**
     * To check that non-small polygons also work.
     * Not too large else tests is quite slow.
     */
    private static final int MAX_SPAN_LARGE = 211;
    
    /**
     * Not too many, that wouldn't help testing anything
     * and could slow things down.
     */
    private static final int MAX_NBR_OF_POINTS_FOR_MAX_SPAN_LARGE = 11;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_CALLS =
            NBR_OF_CALLS_SMALL_SPANS
            + NBR_OF_CALLS_LARGE_SPANS;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DefaultColorDrawer colorDrawer = new DefaultColorDrawer();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DefaultPolyDrawerTest() {
    }
    
    /*
     * 
     */
    
    @Override
    public void test_drawer_bBoxAsClip() {
        super.test_drawer_bBoxAsClip();
    }
    
    @Override
    public void test_drawer_clipNearbyBBox() {
        super.test_drawer_clipNearbyBBox();
    }
    
    /**
     * Random tests don't (easily) cover it,
     * so we test it here.
     */
    public void test_fillPolygon_clipInPolygon() {
        
        final AtomicReference<GRect> clipArgRef =
                new AtomicReference<GRect>();
        final AtomicReference<GRect> rectArgRef =
                new AtomicReference<GRect>();
        
        final InterfaceClippedPointDrawer clippedPointDrawer =
                new InterfaceClippedPointDrawer() {
                    @Override
                    public void drawPointInClip(int x, int y) {
                        throw new UnsupportedOperationException();
                    }
                };
        final DefaultClippedLineDrawer clippedLineDrawer =
                new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer =
                new DefaultLineDrawer(clippedPointDrawer);
        final InterfaceRectDrawer rectDrawer =
                new InterfaceRectDrawer() {
                    @Override
                    public void fillRect(
                            GRect clip,
                            int x, int y, int xSpan, int ySpan,
                            boolean areHorVerFlipped) {
                        final GRect rect = GRect.valueOf(x, y, xSpan, ySpan);
                        clipArgRef.set(clip);
                        rectArgRef.set(rect);
                    }
                    @Override
                    public void drawRect(GRect clip, int x, int y, int xSpan, int ySpan) {
                        throw new UnsupportedOperationException();
                    }
                };
        final DefaultPolyDrawer drawer = new DefaultPolyDrawer(
                this.colorDrawer,
                clippedPointDrawer,
                clippedLineDrawer,
                lineDrawer,
                rectDrawer);
        
        /*
         * Polygon: a triangle.
         */
        
        final int[] xArr = new int[] {1, 100, 50};
        final int[] yArr = new int[] {2, 2, 100};
        final int pointCount = 3;
        
        /*
         * Clip: included in the polygon.
         */
        
        final GRect clip = GRect.valueOf(45, 45, 10, 10);

        /*
         * 
         */
        
        final boolean areHorVerFlipped = false;
        drawer.fillPolygon(clip, xArr, yArr, pointCount, areHorVerFlipped);
        
        final GRect clipArg = clipArgRef.get();
        final GRect rectArg = rectArgRef.get();
        assertSame(clip, clipArg);
        assertEquals(clip, rectArg);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected AbstractDrawerTestHelper<TestPolyArgs> newDrawerTestHelper(
            InterfaceClippedPointDrawer clippedPointDrawer) {
        return new DefaultPolyDrawerTestHelper(
                this.colorDrawer,
                clippedPointDrawer);
    }

    @Override
    protected int getNbrOfCalls() {
        return NBR_OF_CALLS;
    }
    
    @Override
    protected TestPolyArgs newDrawingArgs(int index) {

        final boolean mustUseLargeSpan = (index >= NBR_OF_CALLS_SMALL_SPANS);
        
        final boolean isColorOpaque = this.random.nextBoolean();
        this.colorDrawer.setIsColorOpaque(isColorOpaque);

        final int boundingBoxSpan;
        final int pointCount;
        {
            final int myMaxSpan;
            if (mustUseLargeSpan) {
                myMaxSpan = MAX_SPAN_LARGE;
            } else {
                myMaxSpan = MAX_SPAN_SMALL;
            }
            
            boundingBoxSpan = 1 + this.random.nextInt(myMaxSpan);
            
            final int myMaxNbrOfPoints;
            if (mustUseLargeSpan) {
                myMaxNbrOfPoints = MAX_NBR_OF_POINTS_FOR_MAX_SPAN_LARGE;
            } else {
                if (this.random.nextBoolean()) {
                    myMaxNbrOfPoints = boundingBoxSpan;
                } else {
                    // Possibly filling bounding box with just edges drawing.
                    myMaxNbrOfPoints = NbrsUtils.pow2(boundingBoxSpan);
                }
            }
            
            pointCount = this.random.nextInt(
                    myMaxNbrOfPoints + 1);
        }
        
        // Values past pointCount must have no effect;
        final int xOversize = this.random.nextInt(3);
        final int yOversize = this.random.nextInt(3);
        final int[] xArr = new int[pointCount + xOversize];
        final int[] yArr = new int[pointCount + yOversize];
        for (int i = 0; i < xOversize; i++) {
            xArr[pointCount + i] = this.random.nextInt();
        }
        for (int i = 0; i < yOversize; i++) {
            yArr[pointCount + i] = this.random.nextInt();
        }

        final int tmpX = (ALLOW_HUGE_COORDS ? this.random.nextInt() : 0);
        final int tmpY = (ALLOW_HUGE_COORDS ? this.random.nextInt() : 0);
        final int xOffset = (int) Math.min(tmpX, Integer.MAX_VALUE - (long) boundingBoxSpan + 1);
        final int yOffset = (int) Math.min(tmpY, Integer.MAX_VALUE - (long) boundingBoxSpan + 1);

        for (int i = 0; i < pointCount; i++) {
            final int x = xOffset + this.random.nextInt(boundingBoxSpan);
            final int y = yOffset + this.random.nextInt(boundingBoxSpan);
            xArr[i] = x;
            yArr[i] = y;
        }
        
        final boolean mustDrawAsPolyline = this.random.nextBoolean();

        final TestPolyArgs drawingArgs = new TestPolyArgs(
                xArr,
                yArr,
                pointCount,
                mustDrawAsPolyline);
        if (DEBUG) {
            Dbg.log();
            Dbg.log("isColorOpaque = " + isColorOpaque);
            Dbg.log("drawingArgs = " + drawingArgs);
        }
        return drawingArgs;
    }
    
    @Override
    protected boolean mustAllowMultipaintedPixels(TestPolyArgs drawingArgs) {
        return this.colorDrawer.isColorOpaque();
    }
}
