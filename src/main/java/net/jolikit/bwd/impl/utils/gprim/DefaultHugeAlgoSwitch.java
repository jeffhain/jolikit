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
package net.jolikit.bwd.impl.utils.gprim;

public class DefaultHugeAlgoSwitch implements InterfaceHugeAlgoSwitch {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * For huge ovals or arcs, considering that the figure is very large
     * compared oval and clip intersection, and that lines (used when filling)
     * are clipped before drawing, regular (Bresenham-like) algorithms are
     * in O(span) both for drawing and filling.
     * This means that even for quite large spans, regular algorithms should
     * still go fast, and that we must only switch to huge-specific algorithm,
     * which is in O(box_span^2) (where "box" is the intersection of oval and
     * clip), above a quite large threshold.
     * 
     * Threshold figured out with benches using
     * an almost no-op clipped point drawer.
     * 
     * Could use different (default) thresholds for oval/arc and draw/fill
     * (it could be a bit larger when filling ovals (not arcs),
     * or much larger when just drawing (not filling)),
     * but we prefer to stick to one, for drawing consistency between
     * these different operations, and because it's quite large already.
     */
    public static final int DEFAULT_HUGE_SPAN_THRESHOLD = 10 * 1000;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int hugeSpanThreshold;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses a default threshold.
     */
    public DefaultHugeAlgoSwitch() {
        this(DEFAULT_HUGE_SPAN_THRESHOLD);
    }
    
    /**
     * @param hugeSpanThreshold Must be >= 0.
     */
    public DefaultHugeAlgoSwitch(int hugeSpanThreshold) {
        if (!(hugeSpanThreshold >= 0)) {
            throw new IllegalArgumentException("" + hugeSpanThreshold);
        }
        this.hugeSpanThreshold = hugeSpanThreshold;
    }
    
    @Override
    public boolean mustUseHugeAlgorithm(int xSpan, int ySpan) {
        return this.isHuge(xSpan)
                || this.isHuge(ySpan);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private boolean isHuge(int span) {
        // > and not >=, so that Integer.MAX_VALUE
        // can be used to disable huge-specific algorithm.
        return (span > this.hugeSpanThreshold);
    }
}
