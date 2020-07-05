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

import java.util.Arrays;

import net.jolikit.bwd.api.graphics.GRect;

/**
 * Package-private class for polylines/polygons drawing arguments.
 */
final class TestPolyArgs {
    
    final int[] xArr;
    final int[] yArr;
    final int pointCount;
    final boolean mustDrawAsPolyline;
    
    public TestPolyArgs(
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean mustDrawAsPolyline) {
        this.xArr = xArr;
        this.yArr = yArr;
        this.pointCount = pointCount;
        this.mustDrawAsPolyline = mustDrawAsPolyline;
    }
    
    @Override
    public String toString() {
        return "[xArr = " + Arrays.toString(this.xArr)
        + ", yArr = " + Arrays.toString(this.yArr)
        + ", pointCount = " + this.pointCount
        + ", mustDrawAsPolyline = " + this.mustDrawAsPolyline
        + "]";
    }
    
    public GRect computeBoundingBox() {
        GRect bBox = GRect.DEFAULT_EMPTY;
        for (int i = 0; i < this.pointCount; i++) {
            bBox = bBox.unionBoundingBox(this.xArr[i], this.yArr[i]);
        }
        return bBox;
    }
}
