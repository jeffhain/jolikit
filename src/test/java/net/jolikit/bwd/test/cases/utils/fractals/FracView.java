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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.NbrsUtils;

/**
 * Defines central fractal position and the scale.
 * 
 * Immutable.
 */
public final class FracView {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Center of graphic area in fractal coordinates.
     * Not using top-left corner as reference,
     * to preserve central area when growing/shrinking client.
     */
    private final FracPoint center;
    
    /**
     * Fractal span per pixel, same for horizontal and vertical
     * (to preserve natural fractal aspect).
     */
    private final double scale;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public FracView(
            FracPoint center,
            double scale) {
        this.center = center;
        this.scale = scale;
    }
    
    @Override
    public String toString() {
        return "[" + this.center
                + ", " + this.scale
                + "]";
    }
    
    public FracPoint center() {
        return this.center;
    }

    public double scale() {
        return this.scale;
    }

    /*
     * 
     */
    
    public FracRect computeFRect(GRect gBox) {
        final double fxMin = this.gToFX(gBox.x(), gBox);
        final double fyMin = this.gToFY(gBox.y(), gBox);
        final double fxMax = this.gToFX(gBox.xMax(), gBox);
        final double fyMax = this.gToFY(gBox.yMax(), gBox);
        return new FracRect(fxMin, fyMin, fxMax, fyMax);
    }
    
    public double deltaGToFX(int gdx) {
        final double fdx = gdx * this.scale;
        return fdx;
    }
    
    public double deltaGToFY(int gdy) {
        final double fdy = gdy * this.scale;
        return fdy;
    }
    
    public GRect fToGRect(FracRect fRect, GRect gBox) {
        final int xMin = this.fToGX(fRect.xMin(), gBox);
        final int yMin = this.fToGY(fRect.yMin(), gBox);
        final int xMax = this.fToGX(fRect.xMax(), gBox);
        final int yMax = this.fToGY(fRect.yMax(), gBox);
        final int xSpan = Math.max(0, xMax - xMin + 1);
        final int ySpan = Math.max(0, yMax - yMin + 1);
        return GRect.valueOf(xMin, yMin, xSpan, ySpan);
    }
    
    public int fToGX(double fx, GRect gBox) {
        final double fdx = fx - this.center.x();
        // center is middle of mid pixel, so no xMidFp().
        final int gx = gBox.xMid() + NbrsUtils.roundToInt(fdx / this.scale);
        return gx;
    }
    
    public int fToGY(double fy, GRect gBox) {
        final double fdy = fy - this.center.y();
        // center is middle of mid pixel, so no yMidFp().
        final int gy = gBox.yMid() + NbrsUtils.roundToInt(fdy / this.scale);
        return gy;
    }
    
    public double gToFX(int gx, GRect gBox) {
        // center is middle of mid pixel, so no xMidFp().
        final double fdx = (gx - gBox.xMid()) * this.scale;
        final double fx = this.center.x() + fdx;
        return fx;
    }
    
    public double gToFY(int gy, GRect gBox) {
        // center is middle of mid pixel, so no yMidFp().
        final double fdy = (gy - gBox.yMid()) * this.scale;
        final double fy = this.center.y() + fdy;
        return fy;
    }
}
