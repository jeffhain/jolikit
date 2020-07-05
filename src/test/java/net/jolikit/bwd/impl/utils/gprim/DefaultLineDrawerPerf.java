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

import net.jolikit.bwd.api.graphics.GRect;

/**
 * Benches DefaultLineDrawer.
 */
public class DefaultLineDrawerPerf {

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
        private final InterfaceLineDrawer drawer;
        private final GRect clip;
        public MyGraphics(
                InterfaceLineDrawer drawer,
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
        public void drawLine(int x1, int y1, int x2, int y2) {
            this.drawer.drawLine(
                    this.clip,
                    x1, y1, x2, y2);
        }
        @Override
        public int drawLineStipple(
                int x1, int y1, int x2, int y2,
                int factor, short pattern, int pixelNum) {
            return this.drawer.drawLine(
                    this.clip,
                    x1, y1, x2, y2,
                    factor, pattern, pixelNum);
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
        new DefaultLineDrawerPerf().run(args);
    }
    
    public DefaultLineDrawerPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void run(String[] args) {
        System.out.println("--- " + DefaultLineDrawerPerf.class.getSimpleName() + "... ---");
        
        final MyClippedPointDrawer clippedPointDrawer = new MyClippedPointDrawer();
        final DefaultLineDrawer lineDrawer = new DefaultLineDrawer(clippedPointDrawer);
        final MyGraphics g = new MyGraphics(
                lineDrawer,
                GRect.valueOf(0, 0, 1000, 1000));
        
        GraphicsBencher_drawLine.bench_drawLine_static(g);
        
        if (antiOptim == 0) {
            System.out.println("antiOptim");
        }

        System.out.println("--- ..." + DefaultLineDrawerPerf.class.getSimpleName() + " ---");
    }
}
