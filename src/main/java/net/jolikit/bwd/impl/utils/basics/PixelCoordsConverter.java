/*
 * Copyright 2019 Jeff Hain
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
package net.jolikit.bwd.impl.utils.basics;

/**
 * To convert positions and spans between OS pixels and device pixels.
 */
public class PixelCoordsConverter {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final double pixelRatioOsOverDeviceX;
    private final double pixelRatioOsOverDeviceY;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PixelCoordsConverter(
            double pixelRatioOsOverDeviceX,
            double pixelRatioOsOverDeviceY) {
        this.pixelRatioOsOverDeviceX = pixelRatioOsOverDeviceX;
        this.pixelRatioOsOverDeviceY = pixelRatioOsOverDeviceY;
    }
    
    /*
     * 
     */
    
    public int computeXInDevicePixel(int xInOsPixel) {
        return BindingCoordsUtils.roundToInt(xInOsPixel / this.pixelRatioOsOverDeviceX);
    }
    
    public int computeYInDevicePixel(int yInOsPixel) {
        return BindingCoordsUtils.roundToInt(yInOsPixel / this.pixelRatioOsOverDeviceY);
    }
    
    public int computeXInOsPixel(int xInDevicePixel) {
        return BindingCoordsUtils.roundToInt(xInDevicePixel * this.pixelRatioOsOverDeviceX);
    }
    
    public int computeYInOsPixel(int yInDevicePixel) {
        return BindingCoordsUtils.roundToInt(yInDevicePixel * this.pixelRatioOsOverDeviceY);
    }
    
    /*
     * 
     */
    
    public int computeXSpanInDevicePixel(int xSpanInOsPixel) {
        return roundedSpan(xSpanInOsPixel / this.pixelRatioOsOverDeviceX);
    }
    
    public int computeYSpanInDevicePixel(int ySpanInOsPixel) {
        return roundedSpan(ySpanInOsPixel / this.pixelRatioOsOverDeviceY);
    }
    
    public int computeXSpanInOsPixel(int xSpanInDevicePixel) {
        return roundedSpan(xSpanInDevicePixel * this.pixelRatioOsOverDeviceX);
    }
    
    public int computeYSpanInOsPixel(int ySpanInDevicePixel) {
        return roundedSpan(ySpanInDevicePixel * this.pixelRatioOsOverDeviceY);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @return Span rounded, or 1 if span is in ]0.0,0.5],
     *         to make sure a non-zero span never gets zeroes.
     */
    private int roundedSpan(double span) {
        if (span <= 0.0) {
            return 0;
        }
        return Math.max(1, BindingCoordsUtils.roundToInt(span));
    }
}
