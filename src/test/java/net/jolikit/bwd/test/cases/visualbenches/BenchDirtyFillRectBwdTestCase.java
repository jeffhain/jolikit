/*
 * Copyright 2019-2021 Jeff Hain
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
package net.jolikit.bwd.test.cases.visualbenches;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

/**
 * Bench for fillRect(...) covering small parts of client area,
 * with only one part painted per client painting.
 */
public class BenchDirtyFillRectBwdTestCase extends AbstractDirtyBenchBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int NBR_OF_WHOLE_CLIENT_PAINT = 2;

    private static final int CELL_SPAN = 10;

    private static final int CELL_COUNT_SQRT = 100;
    private static final int CELL_COUNT = CELL_COUNT_SQRT * CELL_COUNT_SQRT;
    
    private static final int INITIAL_WIDTH = CELL_COUNT_SQRT * CELL_SPAN;
    private static final int INITIAL_HEIGHT = CELL_COUNT_SQRT * CELL_SPAN;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    /**
     * Number of paintings for benching, done after first painting.
     */
    private static final int NBR_OF_REPAINT = NBR_OF_WHOLE_CLIENT_PAINT * CELL_COUNT;

    private static final BwdColor[] COLORS = new BwdColor[]{
        BwdColor.BLUE,
        BwdColor.YELLOW,
        BwdColor.GREEN,
        BwdColor.RED,
        BwdColor.BROWN,
        BwdColor.CHOCOLATE,
        BwdColor.CYAN,
        BwdColor.LAVENDER,
        BwdColor.FUCHSIA,
        BwdColor.DEEPPINK,
        BwdColor.DEEPSKYBLUE,
    };

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BenchDirtyFillRectBwdTestCase() {
        super(NBR_OF_REPAINT);
    }

    public BenchDirtyFillRectBwdTestCase(InterfaceBwdBinding binding) {
        super(NBR_OF_REPAINT, binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new BenchDirtyFillRectBwdTestCase(binding);
    }
    
    @Override
    public boolean getMustSequenceLaunches() {
        /*
         * True not to share CPU among bindings.
         */
        return true;
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone_2(
            InterfaceBwdGraphics g,
            GRect dirtyRect,
            GRect[] nextDirtyRect) {

        final ArrayList<GRect> paintedRectList = new ArrayList<GRect>();
        
        final int counter = this.getPaintIndex();
        
        final BwdColor color = COLORS[counter % COLORS.length];
        g.setColor(color);
        
        // NB: Could fill full rect here,
        // clipping should work from second clip.
        final GRect cellRect = computeCellRect(counter);

        final GRect paintedRect = cellRect.unionBoundingBox(dirtyRect);
        
        g.fillRect(paintedRect);
        paintedRectList.add(paintedRect);
        
        nextDirtyRect[0] = computeCellRect(counter + 1);
        
        return paintedRectList;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static GRect computeCellRect(int counter) {
        
        final int colCount = 1 + (INITIAL_WIDTH - 1) / CELL_SPAN;
        final int rowCount = 1 + (INITIAL_HEIGHT - 1) / CELL_SPAN;
        
        final int cellXIndex = counter % colCount;
        final int cellYIndex = (counter / colCount) % rowCount;
        
        final int cellX = cellXIndex * CELL_SPAN;
        final int cellY = cellYIndex * CELL_SPAN;
        
        return GRect.valueOf(cellX, cellY, CELL_SPAN, CELL_SPAN);
    }
}
