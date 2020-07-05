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

/**
 * Benches DefaultPolyDrawer.
 * 
 * Not bothering to bench polylines,
 * it's about the same as drawing polygons.
 */
public class DefaultPolyDrawerPerf {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyClippedPointDrawer implements InterfaceClippedPointDrawer {
        @Override
        public void drawPointInClip(int x, int y) {
            antiOptim += x + y;
        }
    }
    
    private static class MyGraphics extends ThrowingBwdGraphics {
        private final InterfacePolyDrawer drawer;
        private final GRect clip;
        public MyGraphics(
                InterfacePolyDrawer drawer,
                GRect clip) {
            this.drawer = drawer;
            this.clip = clip;
        }
        @Override
        public GRect getClipInBase() {
            return this.clip;
        }
        @Override
        public GRect getClipInUser() {
            return this.clip;
        }
        @Override
        public void drawPolygon(int[] xArr, int[] yArr, int pointCount) {
            this.drawer.drawPolygon(this.clip, xArr, yArr, pointCount);
        }
        @Override
        public void fillPolygon(int[] xArr, int[] yArr, int pointCount) {
            final boolean areHorVerFlipped = false;
            this.drawer.fillPolygon(this.clip, xArr, yArr, pointCount, areHorVerFlipped);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Updated by drawing methods, to avoid calls being optimized away.
     */
    public static int antiOptim = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        newRun(args);
    }

    public static void newRun(String[] args) {
        new DefaultPolyDrawerPerf().run(args);
    }
    
    public DefaultPolyDrawerPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void run(String[] args) {
        System.out.println("--- " + DefaultPolyDrawerPerf.class.getSimpleName() + "... ---");
        
        final MyClippedPointDrawer clippedPointDrawer = new MyClippedPointDrawer();
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultClippedRectDrawer clippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        final DefaultColorDrawer colorDrawer = new DefaultColorDrawer(true);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedPointDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, clippedRectDrawer);
        final DefaultPolyDrawer polyDrawer = new DefaultPolyDrawer(
                colorDrawer,
                clippedPointDrawer,
                clippedLineDrawer,
                lineDrawer,
                rectDrawer);
        final MyGraphics g = new MyGraphics(
                polyDrawer,
                GRect.valueOf(0, 0, 1000, 1000));
        
        GraphicsBencher_drawAndFillPoly.bench_drawPolygon_static(g);

        System.out.println();
        GraphicsBencher_drawAndFillPoly.bench_fillPolygon_static(g);
        
        if (antiOptim == 0) {
            System.out.println("antiOptim");
        }

        System.out.println("--- ..." + DefaultPolyDrawerPerf.class.getSimpleName() + " ---");
    }
}
