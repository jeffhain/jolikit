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
import net.jolikit.lang.NbrsUtils;

/**
 * Package-private class for line (actually, segment) drawing arguments.
 */
final class TestLineArgs {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    final int x1;
    final int y1;
    final int x2;
    final int y2;
    /*
     * For line stipples.
     */
    final int factor;
    final short pattern;
    final int pixelNum;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public TestLineArgs(
            int x1,
            int y1,
            int x2,
            int y2) {
        this(
                x1, y1, x2, y2,
                0, (short) 0, 0);
    }
    
    public TestLineArgs(
            int x1,
            int y1,
            int x2,
            int y2,
            //
            int factor,
            short pattern,
            int pixelNum) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        //
        this.factor = factor;
        this.pattern = pattern;
        this.pixelNum = pixelNum;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[x1 = ").append(this.x1);
        sb.append(", y1 = ").append(this.y1);
        sb.append(", x2 = ").append(this.x2);
        sb.append(", y2 = ").append(this.y2);
        if (this.isLineStipple()) {
            sb.append(", factor = ").append(this.factor);
            sb.append(", pattern = ").append(NbrsUtils.toStringBits(pattern));
            sb.append(", pixelNum = ").append(this.pixelNum);
        }
        sb.append("]");
        return sb.toString();
    }
    
    public boolean isLineStipple() {
        // Line stipples with pattern 0 must just draw nothing,
        // so we use it as special value to mean no line stipple.
        return this.pattern != 0;
    }
    
    public GRect computeBoundingBox() {
        GRect bBox = GRect.DEFAULT_EMPTY;
        bBox = bBox.unionBoundingBox(this.x1, this.y1);
        bBox = bBox.unionBoundingBox(this.x2, this.y2);
        return bBox;
    }
}
