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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;

/**
 * To implement 1-layer alpha painting, with opaque or transparent background.
 */
public abstract class AbstractAlpha1LayerBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /*
     * Cells with various opacity (left to right),
     * and various colors (top to bottom).
     */
    
    private static final int CELL_WIDTH = 80;
    private static final int CELL_HEIGHT = 25;
    
    private static final int[] RGB24_ARR = new int[]{
        0x000000,
        0xFF0000,
        0x00FF00,
        0x0000FF,
        0xFFFF00,
        0xFF00FF,
        0x00FFFF,
        0xFFFFFF
    };
    
    /**
     * One column per pixel alpha level, in ]0,1].
     */
    private static final int NBR_OF_COLUMNS = 4;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_ROWS = RGB24_ARR.length;
    
    private static final int INITIAL_WIDTH = NBR_OF_COLUMNS * CELL_WIDTH;
    private static final int INITIAL_HEIGHT = NBR_OF_ROWS * CELL_HEIGHT;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractAlpha1LayerBwdTestCase() {
    }

    public AbstractAlpha1LayerBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect box = g.getBoxInClient();
        final int xMin = box.x();
        final int yMin = box.y();
        
        final boolean isDirtyPainting =
                !(dirtyRect.isEmpty()
                        || dirtyRect.equals(box));
        
        if (isDirtyPainting) {
            // Making sure we only paint the dirty rect.
            g.addClipInClient(dirtyRect);
        }

        /*
         * Cells.
         */
        
        for (int rowIndex = 0; rowIndex < NBR_OF_ROWS; rowIndex++) {
            final int rgb32 = RGB24_ARR[rowIndex];
            for (int colIndex = 0; colIndex < NBR_OF_COLUMNS; colIndex++) {
                final int cellX = xMin + colIndex * CELL_WIDTH;
                final int cellY = yMin + rowIndex * CELL_HEIGHT;
                
                final double alphaFp = colIndex / (double) (NBR_OF_COLUMNS - 1);
                final int alpha8 = Argb32.toInt8FromFp(alphaFp);
                
                if (isDirtyPainting) {
                    /*
                     * NOT clearing, so that we can observe blending
                     * onto previously paint pixels (if the binding
                     * doesn't clear everything every time).
                     */
                } else {
                    /*
                     * Letting some uncleared border,
                     * so that we can observe what happens
                     * to uncleared regions.
                     */
                    
                    final int unclearedBorder = 2;
                    
                    final int clearX = cellX + unclearedBorder;
                    final int clearY = cellY + unclearedBorder;
                    final int clearXSpan = CELL_WIDTH - 2 * unclearedBorder;
                    final int clearYSpan = CELL_HEIGHT - 2 * unclearedBorder;
                    
                    // Alpha not used, so not bothering.
                    g.setArgb32(0x4080C0);
                    g.clearRectOpaque(clearX, clearY, clearXSpan, clearYSpan);
                }
                
                final int argb32 = Argb32.withAlpha8(rgb32, alpha8);
                g.setArgb32(argb32);
                g.fillRect(cellX, cellY, CELL_WIDTH, CELL_HEIGHT);
                
                /*
                 * 
                 */
                
                final String text = Argb32.toString(argb32);
                
                final boolean withBg = false;
                drawStringCentered(
                        g,
                        cellX + CELL_WIDTH / 2,
                        cellY + CELL_HEIGHT / 2,
                        text,
                        withBg);
            }
        }
        
        /*
         * For each non-dirty painting, we ensure a subsequent dirty (partial)
         * painting, to observe eventual blending with colors preserved from
         * previous paintings (if the binding doesn't clear everything every time).
         */
        
        if (isDirtyPainting) {
            return BindingCoordsUtils.asList(dirtyRect);
        } else {
            // Taking care not to end up on a color border,
            // to make it easier to see the difference.
            final GRect nextDirtyRect = GRect.valueOf(
                    box.x() + box.xSpan() / 3,
                    box.y() + box.ySpan() / 3,
                    box.xSpan() / 5,
                    box.ySpan() / 5);
            this.getHost().makeDirtyAndEnsurePendingClientPainting(nextDirtyRect);
            
            return GRect.DEFAULT_HUGE_IN_LIST;
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static void drawStringCentered(
            InterfaceBwdGraphics g,
            int centerX,
            int centerY,
            String text,
            boolean withBg) {
        final InterfaceBwdFont font = g.getFont();
        final int textHeight = font.fontMetrics().fontHeight();
        final int textWidth = font.fontMetrics().computeTextWidth(text);

        final int textX = centerX - textWidth / 2;
        final int textY = centerY - textHeight / 2;
        
        if (withBg) {
            g.setColor(BwdColor.WHITE);
            g.fillRect(textX, textY, textWidth, textHeight);
        }

        g.setColor(BwdColor.BLACK);
        g.drawText(textX, textY, text);
    }
}
