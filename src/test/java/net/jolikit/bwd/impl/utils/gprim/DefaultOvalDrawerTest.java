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

public class DefaultOvalDrawerTest extends AbstractDrawerTezt<GRect> {

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
    
    private static final int NBR_OF_CALLS_RANDOM = 100;
    
    private static final int MAX_RANDOM_OVAL_SPAN = 100;
    
    /**
     * Not too large so that we can test huge-specific algorithm
     * easily and fast.
     */
    private static final int HUGE_SPAN_THRESHOLD = (int) (MAX_RANDOM_OVAL_SPAN * 0.8);
    
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

    public DefaultOvalDrawerTest() {
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
    protected AbstractDrawerTestHelper<GRect> newDrawerTestHelper(InterfaceClippedPointDrawer clippedPointDrawer) {
        final DefaultHugeAlgoSwitch hugeAlgoSwitch = new DefaultHugeAlgoSwitch(HUGE_SPAN_THRESHOLD);
        return new DefaultOvalDrawerTestHelper(
                hugeAlgoSwitch,
                clippedPointDrawer);
    }

    @Override
    protected int getNbrOfCalls() {
        return NBR_OF_CALLS;
    }
    
    @Override
    protected GRect newDrawingArgs(int index) {
        
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

            final int tmpX = this.random.nextInt();
            final int tmpY = this.random.nextInt();
            x = (int) Math.min(tmpX, Integer.MAX_VALUE - (long) xSpan + 1);
            y = (int) Math.min(tmpY, Integer.MAX_VALUE - (long) ySpan + 1);
        }

        final GRect drawingArgs = GRect.valueOf(x, y, xSpan, ySpan);
        if (DEBUG) {
            Dbg.log();
            Dbg.log("drawingArgs = " + drawingArgs);
        }
        return drawingArgs;
    }
}
