/*
 * Copyright 2020-2024 Jeff Hain
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
import net.jolikit.test.utils.TestUtils;

/**
 * Benches PolyArcDrawer.
 */
public class PolyArcDrawerPerf {

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
        private final InterfaceArcDrawer drawer;
        private final GRect clip;
        public MyGraphics(
                InterfaceArcDrawer drawer,
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
        public void drawArc(
                int x, int y, int xSpan, int ySpan,
                double startDeg, double spanDeg) {
            this.drawer.drawArc(
                    this.clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg);
        }
        @Override
        public void fillArc(
                int x, int y, int xSpan, int ySpan,
                double startDeg, double spanDeg) {
            final boolean areHorVerFlipped = false;
            this.drawer.fillArc(
                    this.clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    areHorVerFlipped);
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
        new PolyArcDrawerPerf().run(args);
    }
    
    public PolyArcDrawerPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void run(String[] args) {
        final long a = System.nanoTime();
        System.out.println("--- " + PolyArcDrawerPerf.class.getSimpleName() + "... ---");

        final DefaultColorDrawer colorDrawer = new DefaultColorDrawer();
        final MyClippedPointDrawer clippedPointDrawer = new MyClippedPointDrawer();
        final DefaultClippedLineDrawer clippedLineDrawer = new DefaultClippedLineDrawer(clippedPointDrawer);
        final DefaultClippedRectDrawer clippedRectDrawer = new DefaultClippedRectDrawer(clippedLineDrawer);
        final DefaultPointDrawer pointDrawer = new DefaultPointDrawer(clippedPointDrawer);
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedPointDrawer);
        final DefaultRectDrawer rectDrawer = new DefaultRectDrawer(lineDrawer, clippedRectDrawer);
        final DefaultPolyDrawer polyDrawer = new DefaultPolyDrawer(
                colorDrawer,
                clippedPointDrawer,
                clippedLineDrawer,
                lineDrawer,
                rectDrawer);
        final PolyArcDrawer arcDrawer = new PolyArcDrawer(
                pointDrawer,
                lineDrawer,
                rectDrawer,
                polyDrawer);
        final MyGraphics g = new MyGraphics(
                arcDrawer,
                GRect.valueOf(0, 0, 1000, 1000));
        
        GraphicsBencher_drawAndFillArc.bench_drawArc_static(g);

        System.out.println();
        GraphicsBencher_drawAndFillArc.bench_fillArc_static(g);
        
        if (antiOptim == 0) {
            System.out.println("antiOptim");
        }

        final long b = System.nanoTime();
        System.out.println("--- ..." + PolyArcDrawerPerf.class.getSimpleName()
            + ", " + TestUtils.nsToSRounded(b-a) + " s ---");
    }
}
