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
 * Definition of an arc for pixel fig status computation.
 */
final class PixelFigStatusArcDef {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final GRect oval;
    
    private final double startDeg;
    private final double spanDeg;
    
    /**
     * In [0.0,360.0[.
     */
    private final double reworkedStartDeg;
    
    /**
     * In [0.0,360.0].
     */
    private final double reworkedSpanDeg;

    private final double sinStart;
    private final double cosStart;

    private final double sinEnd;
    private final double cosEnd;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PixelFigStatusArcDef(
            GRect oval,
            double startDeg,
            double spanDeg) {
        this.oval = oval;
        this.startDeg = startDeg;
        this.spanDeg = spanDeg;
        
        startDeg = GprimUtils.computeNormalizedStartDeg(startDeg);
        spanDeg = GprimUtils.computeClampedSpanDeg(spanDeg);
        
        startDeg = GprimUtils.computeReworkedStartDeg(startDeg, spanDeg);
        spanDeg = GprimUtils.computeReworkedSpanDeg(spanDeg);
        
        this.reworkedStartDeg = startDeg;
        this.reworkedSpanDeg = spanDeg;
        
        /*
         * Computing sin/cos AFTER rework,
         * for less normalization overhead.
         */
        
        this.sinStart = GprimUtils.sinDeg(startDeg);
        this.cosStart = GprimUtils.cosDeg(startDeg);
        
        final double endDeg = startDeg + spanDeg;
        this.sinEnd = GprimUtils.sinDeg(endDeg);
        this.cosEnd = GprimUtils.cosDeg(endDeg);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[oval = ").append(this.oval);
        sb.append(", startDeg = ").append(this.startDeg);
        sb.append(", spanDeg = ").append(this.spanDeg);
        sb.append(", reworkedStartDeg = ").append(this.reworkedStartDeg);
        sb.append(", reworkedSpanDeg = ").append(this.reworkedSpanDeg);
        sb.append("]");
        return sb.toString();
    }
    
    public GRect getOval() {
        return this.oval;
    }

    public double getStartDeg() {
        return this.startDeg;
    }

    public double getSpanDeg() {
        return this.spanDeg;
    }
    
    /**
     * In [0.0,360.0[.
     */
    public double getReworkedStartDeg() {
        return this.reworkedStartDeg;
    }
    
    /**
     * In [0.0,360.0].
     */
    public double getReworkedSpanDeg() {
        return this.reworkedSpanDeg;
    }
    
    public double getSinStart() {
        return this.sinStart;
    }
    
    public double getCosStart() {
        return this.cosStart;
    }
    
    public double getSinEnd() {
        return this.sinEnd;
    }
    
    public double getCosEnd() {
        return this.cosEnd;
    }
}
