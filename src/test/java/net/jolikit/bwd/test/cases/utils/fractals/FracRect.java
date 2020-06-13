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

/**
 * Rectangle in fractal coordinates.
 * 
 * Uses max coordinates instead of spans as GRect does,
 * for it's usually more convenient when not having to stick
 * to the (x, y, width, height) UI frames/components convention.
 */
public final class FracRect {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    public static final FracRect DEFAULT_EMPTY =
            new FracRect(0.0, 0.0, 0.0, 0.0);
    
    private final double xMin;
    private final double yMin;
    
    private final double xMax;
    private final double yMax;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public FracRect(
            double xMin,
            double yMin,
            double xMax,
            double yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }
    
    @Override
    public String toString() {
        return "[" + this.xMin
                + ", " + this.yMin
                + ", " + this.xMax
                + ", " + this.yMax
                + ", (xSpan = " + this.xSpan()
                + ", ySpan = " + this.ySpan()
                + ")]";
    }

    public double xMin() {
        return this.xMin;
    }

    public double yMin() {
        return this.yMin;
    }

    public double xMax() {
        return this.xMax;
    }

    public double yMax() {
        return this.yMax;
    }

    public double xSpan() {
        return this.xMax - this.xMin;
    }
    
    public double ySpan() {
        return this.yMax - this.yMin;
    }

    public double xMid() {
        return this.xMin + (this.xMax - this.xMin) * 0.5;
    }

    public double yMid() {
        return this.yMin + (this.yMax - this.yMin) * 0.5;
    }
    
    public boolean isEmpty() {
        return (this.xMin >= this.xMax)
                || (this.yMin >= this.yMax);
    }
    
    public FracRect intersected(FracRect other) {
        return new FracRect(
                Math.max(this.xMin, other.xMin),
                Math.max(this.yMin, other.yMin),
                Math.min(this.xMax, other.xMax),
                Math.min(this.yMax, other.yMax));
    }
}
