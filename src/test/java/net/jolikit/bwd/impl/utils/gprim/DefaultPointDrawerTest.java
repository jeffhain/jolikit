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

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;

public class DefaultPointDrawerTest extends AbstractDrawerTezt<GPoint> {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    /*
     * Comprehensive.
     */
    
    private static final int COMP_MAX_XY_ABS = 10;
    private static final int COMP_XY_SPAN = 2 * COMP_MAX_XY_ABS + 1;
    
    /*
     * Random.
     */
    
    private static final int NBR_OF_CALLS_RANDOM = 100;
    
    /*
     * 
     */
    
    private static final int NBR_OF_CALLS_COMPREHENSIVE =
            NbrsUtils.pow2(COMP_XY_SPAN);
    private static final int NBR_OF_CALLS =
            NBR_OF_CALLS_COMPREHENSIVE
            + NBR_OF_CALLS_RANDOM;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DefaultPointDrawerTest() {
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
    protected AbstractDrawerTestHelper<GPoint> newDrawerTestHelper(InterfaceClippedPointDrawer clippedPointDrawer) {
        return new DefaultPointDrawerTestHelper(clippedPointDrawer);
    }

    @Override
    protected int getNbrOfCalls() {
        return NBR_OF_CALLS;
    }
    
    @Override
    protected GPoint newDrawingArgs(int index) {
        
        final boolean isComprehensive = (index < NBR_OF_CALLS_COMPREHENSIVE);
        
        final int x;
        final int y;
        if (isComprehensive) {
            final int xi = index % COMP_XY_SPAN;
            final int yi = index / COMP_XY_SPAN;
            x = -COMP_MAX_XY_ABS + xi;
            y = -COMP_MAX_XY_ABS + yi;
        } else {
            x = this.random.nextInt();
            y = this.random.nextInt();
        }

        final GPoint drawingArgs = GPoint.valueOf(x, y);
        if (DEBUG) {
            Dbg.log();
            Dbg.log("drawingArgs = " + drawingArgs);
        }
        return drawingArgs;
    }
}
