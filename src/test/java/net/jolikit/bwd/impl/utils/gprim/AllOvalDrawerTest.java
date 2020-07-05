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
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;

public class AllOvalDrawerTest extends AbstractDrawerTezt<TestOvalArgs> {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    /*
     * Comprehensive.
     */
    
    private static final int MAX_COMPREHENSIVE_OVAL_SPAN = 25;
    
    /*
     * Random.
     */
    
    private static final boolean ALLOW_HUGE_COORDS = true;
    
    private static final int NBR_OF_CALLS_RANDOM = 100;
    
    private static final int MAX_RANDOM_OVAL_SPAN = 100;
    
    private static final double CIRCLE_PROBA = 0.25;
    
    /*
     * 
     */
    
    private static final int NBR_OF_CALLS_COMPREHENSIVE =
            NumbersUtils.pow2(MAX_COMPREHENSIVE_OVAL_SPAN + 1);
    private static final int NBR_OF_CALLS =
            NBR_OF_CALLS_COMPREHENSIVE
            + NBR_OF_CALLS_RANDOM;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AllOvalDrawerTest() {
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
    protected AbstractDrawerTestHelper<TestOvalArgs> newDrawerTestHelper(InterfaceClippedPointDrawer clippedPointDrawer) {
        return new AllOvalDrawerTestHelper(
                clippedPointDrawer);
    }

    @Override
    protected int getNbrOfCalls() {
        return NBR_OF_CALLS;
    }
    
    @Override
    protected TestOvalArgs newDrawingArgs(int index) {
        
        final boolean isComprehensive = (index < NBR_OF_CALLS_COMPREHENSIVE);
        
        final int xSpan;
        final int ySpan;
        final int x;
        final int y;
        if (isComprehensive) {
            // Prime coordinates.
            x = 3;
            y = 7;
            xSpan = index % (MAX_COMPREHENSIVE_OVAL_SPAN + 1);
            ySpan = index / (MAX_COMPREHENSIVE_OVAL_SPAN + 1);
        } else {
            final boolean mustBeCircle = this.randomBoolean(CIRCLE_PROBA);
            
            final int minSpan = 0;
            final int maxSpan = MAX_RANDOM_OVAL_SPAN;
            
            xSpan = minSpan + this.random.nextInt(maxSpan - minSpan + 1);
            if (mustBeCircle) {
                ySpan = xSpan;
            } else {
                ySpan = minSpan + this.random.nextInt(maxSpan - minSpan + 1);
            }

            final int tmpX = (ALLOW_HUGE_COORDS ? this.random.nextInt() : 0);
            final int tmpY = (ALLOW_HUGE_COORDS ? this.random.nextInt() : 0);
            x = (int) Math.min(tmpX, Integer.MAX_VALUE - (long) xSpan + 1);
            y = (int) Math.min(tmpY, Integer.MAX_VALUE - (long) ySpan + 1);
        }

        final GRect oval = GRect.valueOf(x, y, xSpan, ySpan);
        
        final boolean mustUseHugeAlgo = this.random.nextBoolean();
        
        final TestOvalArgs drawingArgs = new TestOvalArgs(
                oval,
                mustUseHugeAlgo);
        if (DEBUG) {
            Dbg.log();
            Dbg.log("drawingArgs = " + drawingArgs);
        }
        return drawingArgs;
    }
}
