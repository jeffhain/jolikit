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
import net.jolikit.lang.Dbg;

/**
 * Algorithm to compute whether a pixel must, could, or must not be painted
 * when drawing or filling an oval.
 */
public class PixelFigStatusOvalAlgo {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------

    /**
     * Contains temporary objects used by this algorithm.
     * 
     * Can use a same instance when calling the computation many times
     * from a same thread, to avoid memory creation/destruction overhead.
     */
    public static class MyTemps {
        final double[] tmpXyArr = new double[2 * (4 * 2)];
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static PixelFigStatus computePixelFigStatus(
            GRect oval,
            boolean isFillElseDraw,
            int x,
            int y,
            //
            MyTemps temps) {

        final int ox = oval.x();
        final int oy = oval.y();
        final int oxSpan = oval.xSpan();
        final int oySpan = oval.ySpan();

        /*
         * Special cases.
         */

        if (oval.minSpan() <= 2) {
            /*
             * Empty-or-line-or-rectangle-like case.
             */
            if (oval.contains(x, y)) {
                return PixelFigStatus.PIXEL_REQUIRED;
            } else {
                return PixelFigStatus.PIXEL_NOT_ALLOWED;
            }
        }

        /*
         * 
         */

        final double rx = (oxSpan - 1) * 0.5;
        final double ry = (oySpan - 1) * 0.5;

        final double cx = ox + rx;
        final double cy = oy + ry;

        int nbrOfPointsIn = 0;

        final double[] xyArr = GprimUtils.computeSurroundingEightsArr(x, y, temps.tmpXyArr);
        for (int i = 0; i < xyArr.length; i += 2) {
            final double px = xyArr[i];
            final double py = xyArr[i+1];

            final double pdx = px - cx;
            final double pdy = py - cy;

            final double pointDistSq = GprimUtils.distSq(pdx, pdy);

            final double distSqOnOval = GprimUtils.distSqOnOval(
                    rx, ry,
                    pdx, pdy);

            final boolean pointIsIn = (pointDistSq < distSqOnOval);

            if (DEBUG) {
                final String inOut = (pointIsIn ? "IN" : "OUT");
                Dbg.log(inOut + " : point = (" + px + ", " + py + "), pixel = (" + x + ", " + y + "), oval = " + oval);
            }

            if (pointIsIn) {
                nbrOfPointsIn++;
            }
        }

        return PixelFigStatusComputer.computePixelFigStatus(
                isFillElseDraw,
                nbrOfPointsIn);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private PixelFigStatusOvalAlgo() {
    }
}
