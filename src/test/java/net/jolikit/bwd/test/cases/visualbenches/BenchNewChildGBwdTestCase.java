/*
 * Copyright 2020-2021 Jeff Hain
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

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.NbrsUtils;

/**
 * To bench g.newChildGraphics(...) and related init()/finish() overhead
 * for painting parts of client area.
 * 
 * Measured grid painting times, and new child graphics overhead per cell
 * (magnitudes, ignoring spikes):
 * On Windows:
 * - awt : 2/3 ms, 150 ns
 * - swing : 2/3 ms, 150 ns
 * - jfx : 1/6 ms, 400 ns (was 600 ns before optim)
 * - swt : 0.9/1.3 ms, 40 ns
 * - lwjgl : 3/4 ms, 120 ns
 * - jogl : 3/4 ms, 100 ns
 * - qtj : 7/15 ms, 1000 ns (but lots of spikes) (was 4500+ ns before optim)
 * - algr : 11/20 ms, 900 ns (but lots of spikes)
 * - sdl : 0.9/1.2 ms, 30 ns
 */
public class BenchNewChildGBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Helps figure out whether painting actually occurs for a given
     * painting type, and whether some GC is going on (freezes),
     * but you should turn it off if you are epileptic.
     */
    private static final boolean MUST_CHANGE_COLORS_WITH_EACH_PAINTING = true;

    /**
     * Not too low, else it brings some bindings to the knees (like for JavaFX,
     * which is heavily asynchronous, causing much piling-up of events).
     */
    private static final int CELL_SPAN = 10;

    /**
     * Number of times we do something to compute an average.
     * 
     * Not too large, else if spikes are frequent we can't get "clean" measures.
     */
    private static final int NBR_FOR_AVERAGE = 20;

    private static final int HEADER_HEIGHT = 4 * 15;

    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = HEADER_HEIGHT + 1000;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyPaintingType {
        SIMPLEST,
        WITH_NEW_CHILD,
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final MyPaintingType[] MyPaintingType_VALUES = MyPaintingType.values();
    
    /**
     * 1 during first painting.
     */
    private int paintingNum = 0;
    
    /**
     * Switching when done measuring averages.
     */
    private MyPaintingType cellPaintingType = MyPaintingType.SIMPLEST;

    private int nbrOfCells = 0;
    
    private long[] paintingTimeSumNsArr = new long[MyPaintingType_VALUES.length];

    private long currentPaintingSumNs = 0L;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BenchNewChildGBwdTestCase() {
    }

    public BenchNewChildGBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new BenchNewChildGBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new BenchNewChildGBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /**
     * Overriding to make sure things never go too fast,
     * for behavioral homogeneity across bindings benches.
     */
    @Override
    public Double getClientPaintDelaySElseNull() {
        return 1.0/60;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final int pNum = ++this.paintingNum;
        
        final GRect box = g.getBox();
        
        {
            g.setColor(BwdColor.BLACK);
            g.fillRect(0, 0, box.xSpan(), HEADER_HEIGHT);
            
            g.setColor(BwdColor.WHITE);
            
            final int dh = 1 + g.getFont().metrics().height();
            int textY = 0;

            g.drawText(0, textY, "cell span = " + CELL_SPAN + ", nbr of cells = " + this.nbrOfCells
                    + ", painting num = " + pNum);
            textY += dh;

            final long sum1Ns = this.paintingTimeSumNsArr[MyPaintingType.SIMPLEST.ordinal()];
            final long sum2Ns = this.paintingTimeSumNsArr[MyPaintingType.WITH_NEW_CHILD.ordinal()];
            
            {
                final long paintingNs = (sum1Ns / NBR_FOR_AVERAGE);
                g.drawText(0, textY, "grid painting time (simplest) =                " + nsToMsStr(paintingNs) + " ms");
                textY += dh;
            }
            
            {
                final long paintingNs = (sum2Ns / NBR_FOR_AVERAGE);
                g.drawText(0, textY, "grid painting time (per-cell child graphics) = " + nsToMsStr(paintingNs) + " ms");
                textY += dh;
                if (this.nbrOfCells != 0) {
                    final long overheadNs = ((sum2Ns - sum1Ns) / NBR_FOR_AVERAGE);
                    final long overheadPerCellNs = (overheadNs / this.nbrOfCells);
                    g.drawText(0, textY, "(new child graphics overhead per cell = " + overheadPerCellNs + " ns)");
                    textY += dh;
                }
            }
        }
        
        /*
         * Benching.
         * 
         * Not using black or white, which might cause
         * special cases in bindings, flawing the bench.
         */
        
        final BwdColor c1 = BwdColor.GREEN;
        final BwdColor c2 = BwdColor.YELLOW;
        
        /*
         * Benching.
         */
        
        final long startNs = System.nanoTime();

        int cellCount = 0;
        
        int j = 0;
        while (true) {
            final int subY = box.y() + HEADER_HEIGHT + j * CELL_SPAN;
            if (subY > box.yMax()) {
                break;
            }
                    
            int i = 0;
            while (true) {
                final int subX = box.x() + i * CELL_SPAN;
                if (subX > box.xMax()) {
                    break;
                }
                
                cellCount++;
                
                final BwdColor c;
                {
                    // To have a grid pattern.
                    int evenOrNot = i + j;
                    if (MUST_CHANGE_COLORS_WITH_EACH_PAINTING) {
                        evenOrNot += pNum;
                    }
                    if (NbrsUtils.isEven(evenOrNot)) {
                        c = c1;
                    } else {
                        c = c2;
                    }
                }
                
                final GRect cellBox = GRect.valueOf(
                        subX,
                        subY,
                        CELL_SPAN,
                        CELL_SPAN);
                
                if (this.cellPaintingType == MyPaintingType.SIMPLEST) {
                    g.setColor(c);
                    g.fillRect(cellBox);
                    
                } else if (this.cellPaintingType == MyPaintingType.WITH_NEW_CHILD) {
                    final InterfaceBwdGraphics childG = g.newChildGraphics(cellBox);
                    childG.init();
                    try {
                        childG.setColor(c);
                        childG.fillRect(cellBox);
                    } finally {
                        childG.finish();
                    }
                    
                } else {
                    throw new AssertionError(this.cellPaintingType);
                }
                
                i++;
            }
            j++;
        }

        final long endNs = System.nanoTime();
        final long dtNs = endNs - startNs;
        
        this.nbrOfCells = cellCount;
        
        this.currentPaintingSumNs += dtNs;
        
        /*
         * 
         */
        
        if ((pNum % NBR_FOR_AVERAGE) == 0) {
            this.paintingTimeSumNsArr[this.cellPaintingType.ordinal()] = this.currentPaintingSumNs;
            
            // Next one.
            final int newCellPaintingTypeOrdinal =
                    (this.cellPaintingType.ordinal() + 1) % MyPaintingType_VALUES.length;
            this.cellPaintingType = MyPaintingType_VALUES[newCellPaintingTypeOrdinal];
            
            this.currentPaintingSumNs = 0;
        }
        
        /*
         * 
         */
        
        getHost().ensurePendingClientPainting();
        
        return null;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static String nsToMsStr(long ns) {
        // Truncating below 1 microsecond.
        final double ms = (ns / 1000) / 1000.0;
        return NbrsUtils.toStringNoCSN(ms);
    }
}
