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
package net.jolikit.bwd.test.cases.utils.fractals;

import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;

/**
 * Draws Mandelbrot on a graphics.
 */
public class MandDrawer {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final FracColorComputer colorComputer =
            new FracColorComputer();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public MandDrawer() {
    }

    public FracColorComputer getColorComputer() {
        return this.colorComputer;
    }
    
    /**
     * @param maxIter Must be in [1,0xFFFFFF].
     *        Considered in fractal when iter >= maxIter.
     */
    public void setMaxIter(int maxIter) {
        /*
         * Using color computer as maxIter holder,
         * since it needs it as well.
         */
        this.colorComputer.setMaxIter(maxIter);
    }

    /**
     * Draws the specified fractal rectangle
     * over the specified graphic rectangle.
     * 
     * @param inputId Id corresponding to the specified inputs.
     * @param lastInputIdRef Must abort if
     *        it doesn't contain inputId anymore.
     * @return True if was aborted, false otherwise.
     */
    public boolean drawFractal(
            int inputId,
            AtomicInteger lastInputIdRef,
            FracRect fracRect,
            GRect rect,
            InterfaceBwdGraphics g) {
        
        // (x,y) in fractal coordinates.
        final double fx0 = fracRect.xMin();
        final double fy0 = fracRect.yMin();
        
        final double fxStep = (fracRect.xMax() - fx0) / (rect.xSpan() - 1);
        final double fyStep = (fracRect.yMax() - fy0) / (rect.ySpan() - 1);
        
        final int maxIter = this.colorComputer.getMaxIter();
        
        // To avoid color computation when iter didn't change.
        int lastIter = -1;

        boolean aborted = false;
        
        for (int j = 0; j < rect.ySpan(); j++) {
            final int y = rect.y() + j;
            final double fy = fy0 + j * fyStep;
            
            /*
             * Checked first, do no computation if true from start,
             * and to make it easy to compute return value
             * (always something remaining).
             */
            if (lastInputIdRef.get() != inputId) {
                aborted = true;
                break;
            }
            
            for (int i = 0; i < rect.xSpan(); i++) {
                final int x = rect.x() + i;
                final double fx = fx0 + i * fxStep;
                
                final int iter = computeIterForPoint(fx, fy, maxIter);
                if (iter != lastIter) {
                    final int argb32 = this.colorComputer.iterToArgb32(iter);
                    // Fast, doing it even if color didn't change.
                    g.setArgb32(argb32);
                    lastIter = iter;
                }

                /*
                 * Not bothering to draw lines when color doesn't change,
                 * it's either slower or barely faster than drawPoint()
                 * with our bindings, at least on writable images,
                 * especially when the fractal is messy. 
                 */
                g.drawPoint(x, y);
            }
        }
        
        return aborted;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int computeIterForPoint(
            double x,
            double y,
            int maxIter) {
        double x1 = x;
        double y1 = y;
        int iter = 0;
        double x2 = x1 * x1;
        double y2 = y1 * y1;
        while ((iter < maxIter)
                && (x2 + y2 < 4.0)) {
            /*
             * Incremented only if iter < maxIter
             * and distance check is OK:
             * - If maxIter is 0, iter will always be 0,
             *   and no way to distinguish whether in or out of fractal.
             * - If maxIter is 1, iter 0 should be considered as out,
             *   and iter 1 as in.
             * - etc.
             */
            iter++;
            /*
             * For Julia set, use fixed values instead of (x,y) here.
             */
            y1 = (x1 + x1) * y1 + y;
            x1 = x2 - y2 + x;
            x2 = x1 * x1;
            y2 = y1 * y1;
        }
        return iter;
    }
}
