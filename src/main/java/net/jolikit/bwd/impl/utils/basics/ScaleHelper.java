/*
 * Copyright 2021 Jeff Hain
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

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.NbrsUtils;

/**
 * Scaling.
 * 
 * "OS" = in OS pixels.
 * "BD" = in binding pixels.
 * 
 * Not using an interface, to avoid megamorphism.
 */
public class ScaleHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private int scale;
    private double scaleInv;
    
    /**
     * >= 0 if scale is a power of two (which includes scale of 1),
     * -1 otherwise.
     */
    private int power;
    
    private int scaleM1;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Default scale is 1.
     */
    public ScaleHelper() {
        this.setScale(1);
    }

    public final void setScale(int scale) {
        NbrsUtils.requireSupOrEq(1, scale, "scale");
        this.scale = scale;
        this.scaleInv = 1.0 / scale;
        final boolean isPot = NbrsUtils.isPowerOfTwo(scale);
        this.power = (isPot ? NbrsUtils.log2(scale) : -1);
        this.scaleM1 = (scale - 1);
    }

    public int getScale() {
        return this.scale;
    }

    /**
     * @return The inverse of scale.
     */
    public double getScaleInv() {
        return this.scaleInv;
    }

    /*
     * Scalar.
     */
    
    /**
     * @param osPos Must be in screen coordinates,
     *        for computed BD pixel to be properly aligned.
     * @return Position in BD, of BD pixel which contains
     *         the specified OS pixels.
     */
    public int posOsToBd(int osPos) {
        return this.divByScale(osPos);
    }

    /**
     * @param osPos Must be in screen coordinates,
     *        for computed BD pixel to be properly aligned.
     * @return Position in BD, of BD pixel which all OS pixels
     *         positions are inferior or equal to the specified one.
     */
    public int posOsToBdFloor(int osPos) {
        return this.divByScale(osPos - this.scaleM1);
    }

    /**
     * @param osPos Must be in screen coordinates,
     *        for computed BD pixel to be properly aligned.
     * @return Position in BD, of BD pixel which all OS pixels
     *         positions are superior or equal to the specified one.
     */
    public int posOsToBdCeil(int osPos) {
        return this.divByScale(osPos + this.scaleM1);
    }
    
    public int posBdToOsFloor(int bdPos) {
        return this.scale * bdPos;
    }
    
    public int posBdToOsCeil(int bdPos) {
        return this.scale * bdPos + this.scaleM1;
    }
    
    /*
     * Point.
     */
    
    public GPoint pointOsToBd(GPoint osPoint) {
        if ((this.power == 0)
            || osPoint.equals(GPoint.ZERO)) {
            return osPoint;
        }
        return GPoint.valueOf(
            posOsToBd(osPoint.x()),
            posOsToBd(osPoint.y()));
    }
    
    public GPoint pointBdToOsFloor(GPoint bdPoint) {
        if ((this.power == 0)
            || bdPoint.equals(GPoint.ZERO)) {
            return bdPoint;
        }
        return GPoint.valueOf(
            posBdToOsFloor(bdPoint.x()),
            posBdToOsFloor(bdPoint.y()));
    }
    
    public GPoint pointBdToOsCeil(GPoint bdPoint) {
        if ((this.power == 0)
            || bdPoint.equals(GPoint.ZERO)) {
            return bdPoint;
        }
        return GPoint.valueOf(
            posBdToOsCeil(bdPoint.x()),
            posBdToOsCeil(bdPoint.y()));
    }

    /*
     * Span.
     */
    
    public int spanOsToBdFloor(int osSpan) {
        return this.divByScale(osSpan);
    }
    
    public int spanOsToBdCeil(int osSpan) {
        return this.divByScale(osSpan + this.scaleM1);
    }
    
    public int spanBdToOs(int bdSpan) {
        return this.scale * bdSpan;
    }
    
    /*
     * Rectangle.
     * 
     * Preserving DEFAULT_EMPTY instances,
     * some APIs requiring it as a special case.
     */

    /**
     * @param osRect Must be in screen coordinates,
     *        for computed BD pixels to be properly aligned.
     * @return Corresponding included rectangle.
     */
    public GRect rectOsToBdContained(GRect osRect) {
        if ((this.power == 0)
            || osRect.equals(GRect.DEFAULT_EMPTY)) {
            return osRect;
        }
        final int bdX = posOsToBdCeil(osRect.x());
        final int bdY = posOsToBdCeil(osRect.y());
        final int bdXMax = posOsToBdFloor(osRect.xMax());
        final int bdYMax = posOsToBdFloor(osRect.yMax());
        final int bdXSpan = Math.max(0, bdXMax - bdX + 1);
        final int bdYSpan = Math.max(0, bdYMax - bdY + 1);
        return GRect.valueOf(bdX, bdY, bdXSpan, bdYSpan);
    }
    
    /**
     * @param osRect Must be in screen coordinates,
     *        for computed BD pixels to be properly aligned.
     * @return Corresponding containing rectangle.
     */
    public GRect rectOsToBdContaining(GRect osRect) {
        if ((this.power == 0)
            || osRect.equals(GRect.DEFAULT_EMPTY)) {
            return osRect;
        }
        final int bdX = posOsToBd(osRect.x());
        final int bdY = posOsToBd(osRect.y());
        final int bdXMax = posOsToBd(osRect.xMax());
        final int bdYMax = posOsToBd(osRect.yMax());
        final int bdXSpan = (bdXMax - bdX + 1);
        final int bdYSpan = (bdYMax - bdY + 1);
        return GRect.valueOf(bdX, bdY, bdXSpan, bdYSpan);
    }
    
    /**
     * @return Corresponding (and containing) rectangle.
     */
    public GRect rectBdToOs(GRect bdRect) {
        if ((this.power == 0)
            || bdRect.equals(GRect.DEFAULT_EMPTY)) {
            return bdRect;
        }
        final int osX = posBdToOsFloor(bdRect.x());
        final int osY = posBdToOsFloor(bdRect.y());
        final int osXSpan = spanBdToOs(bdRect.xSpan());
        final int osYSpan = spanBdToOs(bdRect.ySpan());
        return GRect.valueOf(osX, osY, osXSpan, osYSpan);
    }
    
    /*
     * Insets.
     * 
     * Insets in BD must be ceiling of insets in OS,
     * to allow for proper (insets,clientBounds,windowBounds)
     * consistency (else scaled client could have to leak out
     * of actual client).
     */
    
    public GRect insetsOsToBdContaining(GRect bdInsets) {
        if ((this.power == 0)
            || bdInsets.equals(GRect.DEFAULT_EMPTY)) {
            return bdInsets;
        }
        /*
         * (left,top,right,bottom)
         */
        return GRect.valueOf(
            spanOsToBdCeil(bdInsets.x()),
            spanOsToBdCeil(bdInsets.y()),
            spanOsToBdCeil(bdInsets.xSpan()),
            spanOsToBdCeil(bdInsets.ySpan()));
    }
    
    public static GRect computeScaledClientInsetsInOs(
        GRect clientBoundsInOs,
        GRect scaledClientBoundsInOs) {
        final GRect scaledClientInsetsInOs =
            GRect.valueOf(
                scaledClientBoundsInOs.x() - clientBoundsInOs.x(),
                scaledClientBoundsInOs.y() - clientBoundsInOs.y(),
                clientBoundsInOs.xMax() - scaledClientBoundsInOs.xMax(),
                clientBoundsInOs.yMax() - scaledClientBoundsInOs.yMax());
        return scaledClientInsetsInOs;
    }
    
    /*
     * 
     */
    
    /**
     * All coordinates are in root graphics base coordinates.
     * 
     * @return Rectangles to fill (in black) around scaled client,
     *         to complete actual client.
     */
    public static GRect[] computeBorderRectsInBd(
        GRect boxWithBorder,
        int border) {
        
        /*
         * xxx
         * . .
         * ...
         */
        
        final GRect top = GRect.valueOf(
            boxWithBorder.x(),
            boxWithBorder.y(),
            boxWithBorder.xSpan(),
            border);

        /*
         * ...
         * . .
         * xxx
         */
        
        final GRect bottom = GRect.valueOf(
            boxWithBorder.x(),
            boxWithBorder.yMax() - (border - 1),
            boxWithBorder.xSpan(),
            border);
        
        /*
         * ...
         * x .
         * ...
         */
        
        final GRect left = GRect.valueOf(
            boxWithBorder.x(),
            boxWithBorder.y() + border,
            border,
            boxWithBorder.ySpan() - 2 * border);
        
        /*
         * ...
         * . x
         * ...
         */
        
        final GRect right = GRect.valueOf(
            boxWithBorder.xMax() - (border - 1),
            boxWithBorder.y() + border,
            border,
            boxWithBorder.ySpan() - 2 * border);
        
        return new GRect[] {top, bottom, left, right};
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private int divByScale(int val) {
        return (this.power >= 0) ? (val >> this.power) : (val / this.scale);
    }
}
