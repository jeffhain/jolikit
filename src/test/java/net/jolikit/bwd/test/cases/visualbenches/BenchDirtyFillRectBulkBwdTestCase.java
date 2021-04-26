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
 * with multiple parts painted per client painting.
 */
public class BenchDirtyFillRectBulkBwdTestCase extends AbstractDirtyBenchBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int NBR_OF_WHOLE_CLIENT_PAINT = 2;
    private static final int NBR_OF_CELLS_PER_PAINT = 20;

    private static final int CELL_SPAN = 10;

    private static final int CELL_COUNT_SQRT = 100;
    private static final int CELL_COUNT = CELL_COUNT_SQRT * CELL_COUNT_SQRT;
    
    private static final int INITIAL_WIDTH = CELL_COUNT_SQRT * CELL_SPAN;
    private static final int INITIAL_HEIGHT = CELL_COUNT_SQRT * CELL_SPAN;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    /**
     * Number of paintings for benching, done after first painting.
     */
    private static final int NBR_OF_REPAINT = NBR_OF_WHOLE_CLIENT_PAINT * (CELL_COUNT / NBR_OF_CELLS_PER_PAINT);

    private static final int BULK_CELL_WIDTH = NBR_OF_CELLS_PER_PAINT * CELL_SPAN;

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
    // FIELDS
    //--------------------------------------------------------------------------
    
    private int cellCounter = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BenchDirtyFillRectBulkBwdTestCase() {
        super(NBR_OF_REPAINT);
    }

    public BenchDirtyFillRectBulkBwdTestCase(InterfaceBwdBinding binding) {
        super(NBR_OF_REPAINT, binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new BenchDirtyFillRectBulkBwdTestCase(binding);
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

        final int paintCounter = this.getPaintIndex();
        
        for (int k = 0; k < NBR_OF_CELLS_PER_PAINT; k++) {
            final int cellCounter = this.cellCounter++;
            
            final BwdColor color = COLORS[cellCounter % COLORS.length];
            g.setColor(color);
            
            final GRect cellRect = computeCellRect(CELL_SPAN, cellCounter);
            
            final GRect paintedRect = cellRect.unionBoundingBox(dirtyRect);
            
            g.fillRect(paintedRect);
            paintedRectList.add(paintedRect);
        }
        
        nextDirtyRect[0] = computeCellRect(BULK_CELL_WIDTH, paintCounter + 1);
        
        return paintedRectList;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static GRect computeCellRect(int width, int counter) {
        
        final int colCount = 1 + (INITIAL_WIDTH - 1) / width;
        final int rowCount = 1 + (INITIAL_HEIGHT - 1) / CELL_SPAN;
        
        final int xIndex = counter % colCount;
        final int yIndex = (counter / colCount) % rowCount;
        
        final int x = xIndex * width;
        final int y = yIndex * CELL_SPAN;
        
        return GRect.valueOf(x, y, width, CELL_SPAN);
    }
}
