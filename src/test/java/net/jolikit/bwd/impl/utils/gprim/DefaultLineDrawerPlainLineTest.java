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

import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;

public class DefaultLineDrawerPlainLineTest extends AbstractDrawerTezt<TestLineArgs> {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    private static final int MAX_FACTOR = 5;
    private static final int MAX_PIXEL_NUM = 5;
    
    /*
     * Comprehensive.
     */
    
    private static final int MAX_COMPREHENSIVE_SEGMENT_SPAN = 25;
    
    /*
     * Random.
     */
    
    /**
     * Each = plain with plain method, and plain with stipple method.
     */
    private static final int NBR_OF_CALLS_RANDOM_EACH = 1000;
    
    private static final int MAX_RANDOM_SEGMENT_SPAN = 50;
    
    /*
     * 
     */

    private static final int NBR_OF_CALLS_COMPREHENSIVE_EACH =
            NumbersUtils.pow2(MAX_COMPREHENSIVE_SEGMENT_SPAN + 1);
    private static final int NBR_OF_CALLS_COMPREHENSIVE = 2 * NBR_OF_CALLS_COMPREHENSIVE_EACH;
    private static final int NBR_OF_CALLS_RANDOM = 2 * NBR_OF_CALLS_RANDOM_EACH;
    private static final int NBR_OF_CALLS = NBR_OF_CALLS_COMPREHENSIVE + NBR_OF_CALLS_RANDOM;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DefaultLineDrawerPlainLineTest() {
    }
    
    /*
     * 
     */
    
    @Override
    public void test_drawer_bBoxAsClip() {
        super.test_drawer_bBoxAsClip();
    }
    
    @Override
    public void test_drawer_clipNearbyBBox() {
        super.test_drawer_clipNearbyBBox();
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected AbstractDrawerTestHelper<TestLineArgs> newDrawerTestHelper(
            InterfaceClippedPointDrawer clippedPointDrawer) {
        return new DefaultLineDrawerTestHelper(clippedPointDrawer);
    }

    @Override
    protected int getNbrOfCalls() {
        return NBR_OF_CALLS;
    }

    @Override
    protected TestLineArgs newDrawingArgs(int index) {
        
        final boolean isComprehensive = (index < NBR_OF_CALLS_COMPREHENSIVE);
        final boolean plainStipple =
                (index >= NBR_OF_CALLS_COMPREHENSIVE_EACH)
                || (index >= NBR_OF_CALLS_COMPREHENSIVE + NBR_OF_CALLS_RANDOM_EACH);
        
        final int x1;
        final int y1;
        final int x2;
        final int y2;
        if (isComprehensive) {
            // Prime coordinates for P1.
            x1 = 3;
            y1 = 7;
            x2 = index % (MAX_COMPREHENSIVE_SEGMENT_SPAN + 1);
            y2 = index / (MAX_COMPREHENSIVE_SEGMENT_SPAN + 1);
        } else {
            x1 = this.randomMinusIntInt(1 + MAX_RANDOM_SEGMENT_SPAN / 2);
            y1 = this.randomMinusIntInt(1 + MAX_RANDOM_SEGMENT_SPAN / 2);
            if (this.randomBoolean(0.1)) {
                // Special cases: horizontal, vertical, point.
                if (this.random.nextBoolean()) {
                    x2 = x1;
                } else {
                    x2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
                }
                if (this.random.nextBoolean()) {
                    y2 = y1;
                } else {
                    y2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
                }
            } else {
                x2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
                y2 = this.randomMinusIntInt(MAX_RANDOM_SEGMENT_SPAN / 2);
            }
        }
        
        final TestLineArgs drawingArgs;
        if (plainStipple) {
            final int factor = 1 + this.random.nextInt(MAX_FACTOR);
            final short pattern = GprimUtils.PLAIN_PATTERN;
            final int pixelNum = this.random.nextInt(MAX_PIXEL_NUM + 1);
            drawingArgs = new TestLineArgs(
                    x1, y1, x2, y2,
                    factor, pattern, pixelNum);
        } else {
            drawingArgs = new TestLineArgs(x1, y1, x2, y2);
        }
        
        if (DEBUG) {
            Dbg.log();
            Dbg.log("drawingArgs = " + drawingArgs);
        }
        return drawingArgs;
    }
}
