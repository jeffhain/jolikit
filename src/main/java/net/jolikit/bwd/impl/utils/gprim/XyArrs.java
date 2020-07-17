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

import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Helper for polygons (xArr,yArr) arguments.
 */
class XyArrs {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private int[] xArr = new int[3];
    private int[] yArr = new int[3];
    private int size;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public XyArrs() {
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        sb.append("xArr=");
        appendArr(this.xArr, this.size, sb);
        
        sb.append(LangUtils.LINE_SEPARATOR);
        
        sb.append("yArr=");
        appendArr(this.yArr, this.size, sb);
        
        return sb.toString();
    }
    
    public String toStringXArr() {
        final StringBuilder sb = new StringBuilder();
        appendArr(this.xArr, this.size, sb);
        return sb.toString();
    }
    
    public String toStringYArr() {
        final StringBuilder sb = new StringBuilder();
        appendArr(this.yArr, this.size, sb);
        return sb.toString();
    }
    
    /**
     * @return The internal X array.
     */
    public int[] xArr() {
        return this.xArr;
    }
    
    /**
     * @return The internal Y array.
     */
    public int[] yArr() {
        return this.yArr;
    }
    
    /**
     * @return The number of points.
     */
    public int size() {
        return this.size;
    }
    
    public void clear() {
        this.size = 0;
    }
    
    public void addPoint(int x, int y) {
        this.ensureRoom();
        this.xArr[this.size] = x;
        this.yArr[this.size] = y;
        this.size++;
    }
    
    /**
     * Allows not to end up with aligned points,
     * i.e. an uselessly large number of segments
     * in the polygon.
     */
    public void addPointSmart(int x, int y) {

        final int i = this.size;
        final int prevPointCount = i;
        final int[] xArr = this.xArr;
        final int[] yArr = this.yArr;
        
        if (prevPointCount >= 1) {
            if ((x == xArr[i - 1])
                    && (y == yArr[i - 1])) {
                // Same point.
                if (DEBUG) {
                    Dbg.log("new point = (" + x + ", " + y + ") : ignored");
                }
                return;
            }
        }
        
        if (prevPointCount >= 2) {
            final int ax = xArr[i - 2];
            final int ay = yArr[i - 2];
            final int bx = xArr[i - 1];
            final int by = yArr[i - 1];
            final boolean isExtrapolation;
            final int oldDx = bx - ax;
            final int oldDy = by - ay;
            final int newDx = x - bx;
            final int newDy = y - by;
            final boolean oldHor = (oldDy == 0);
            final boolean oldVer = (oldDx == 0);
            final boolean newHor = (newDy == 0);
            final boolean newVer = (newDx == 0);
            if ((newHor != oldHor)
                    || (newVer != oldVer)) {
                // Some hor/ver change: old and new
                // can't have same slopes.
            } else {
                if (oldHor) {
                    isExtrapolation = ((newDx < 0) == (oldDx < 0));
                } else if (oldVer) {
                    isExtrapolation = ((newDy < 0) == (oldDy < 0));
                } else {
                    // Neither is horizontal or vertical:
                    // ok to use slopes.
                    // oldSlope = oldDy / oldDx
                    // newSlope = newDy / newDx
                    // newDy / newDx = oldDy / oldDx
                    // oldDx * newDy = newDx * oldDy
                    isExtrapolation = ((oldDx * newDy) == (newDx * oldDy));
                }
                if (isExtrapolation) {
                    // Replacing last point.
                    if (DEBUG) {
                        Dbg.log("new point = (" + x + ", " + y + ") : replacing");
                    }
                    xArr[i - 1] = x;
                    yArr[i - 1] = y;
                    return;
                }
            }
        }

        // Adding point.
        if (DEBUG) {
            Dbg.log("new point = (" + x + ", " + y + ") : adding");
        }
        this.addPoint(x, y);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void appendArr(
            int[] arr,
            int size,
            StringBuilder sb) {
        sb.append("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arr[i]);
        }
        sb.append("]");
    }
    
    private void ensureRoom() {
        if (this.size == this.xArr.length) {
            this.grow();
        }
    }
    
    private void grow() {
        final int oldCap = this.xArr.length;
        final int minCap = oldCap + 1;

        final int newCap = LangUtils.increasedArrayLength(oldCap, minCap);
        final int[] newXArr = new int[newCap];
        final int[] newYArr = new int[newCap];
        System.arraycopy(this.xArr, 0, newXArr, 0, this.size);
        System.arraycopy(this.yArr, 0, newYArr, 0, this.size);
        this.xArr = newXArr;
        this.yArr = newYArr;
    }
}
