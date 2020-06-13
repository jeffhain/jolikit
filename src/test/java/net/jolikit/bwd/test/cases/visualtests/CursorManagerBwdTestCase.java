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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdCursorManager;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

public class CursorManagerBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int CELL_X_SPAN = 160;
    private static final int CELL_Y_SPAN = 40;
    
    private static final int TARGET_FONT_HEIGHT = 24;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Row count as side length of a square grid with enough cells.
     */
    private static final int ROW_COUNT = (int) Math.ceil(Math.sqrt(BwdCursors.cursorList().size()));
    /**
     * Just enough columns.
     */
    private static final int COL_COUNT = 1 + (BwdCursors.cursorList().size() - 1) / ROW_COUNT;

    private static final int INITIAL_WIDTH = COL_COUNT * CELL_X_SPAN;
    private static final int INITIAL_HEIGHT = ROW_COUNT * CELL_Y_SPAN;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    private final SortedMap<Integer,Object> cursorAddKeyByCursor =
            new TreeMap<Integer,Object>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public CursorManagerBwdTestCase() {
    }
    
    public CursorManagerBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new CursorManagerBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new CursorManagerBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * 
     */

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        super.onMouseMoved(event);
        
        final int x = event.xInClient();
        final int y = event.yInClient();
        
        this.onEventOfInterest(x, y);
    }
    
    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        super.onMouseEnteredClient(event);
        
        final int x = event.xInClient();
        final int y = event.yInClient();
        
        this.onEventOfInterest(x, y);
    }
    
    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        super.onMouseExitedClient(event);
        
        /*
         * Mouse coordinates can be inside client even for this event,
         * so we use made-up coordinates outside of client.
         */
        
        final int x = -1;
        final int y = -1;
        
        this.onEventOfInterest(x, y);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        final InterfaceBwdFont font = fontHome.newFontWithClosestHeight(defaultFont.fontKind(), TARGET_FONT_HEIGHT);
        g.setFont(font);
        
        for (int i = 0; i < BwdCursors.cursorList().size(); i++) {
            final int cursor = BwdCursors.cursorList().get(i);
            
            final int row = row(i);
            final int col = col(i);
            
            final String text = BwdCursors.toString(cursor);
            
            final int x = col * CELL_X_SPAN;
            final int y = row * CELL_Y_SPAN;
            
            // Not GRAY background, else XOR-mode paint cursors won't be visible.
            g.setColor(BwdColor.GREEN);
            g.fillRect(x, y, CELL_X_SPAN, CELL_Y_SPAN);
            g.setColor(BwdColor.BLACK);
            g.drawRect(x, y, CELL_X_SPAN, CELL_Y_SPAN);
            
            g.setColor(BwdColor.BLACK);
            g.drawText(x + 1, y + 1, text);
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int row(int cursorIndex) {
        final int row = cursorIndex / COL_COUNT;
        return row;
    }
    
    private static int col(int cursorIndex) {
        final int row = row(cursorIndex);
        // i % colCount
        final int col = cursorIndex - row * COL_COUNT;
        return col;
    }
    
    /**
     * @param x Mouse X position in client.
     * @param y Mouse Y position in client.
     */
    private void onEventOfInterest(int x, int y) {
        
        final InterfaceBwdCursorManager cursorManager =
                this.getHost().getCursorManager();

        final GRect clientRect = this.getHost().getClientBounds();
        final int xSpan = clientRect.xSpan();
        final int ySpan = clientRect.ySpan();
        
        int pointedCellIndex = -1;
        if ((x >= 0)
                && (y >= 0)
                && (x < xSpan)
                && (y < ySpan)) {
            // Mouse in client.
            final int row = y / CELL_Y_SPAN;
            final int col = x / CELL_X_SPAN;
            final int cellIndex = col + row * COL_COUNT;
            if (cellIndex < BwdCursors.cursorList().size()) {
                pointedCellIndex = cellIndex;
            } else {
                // Not over a cursor cell.
            }
        } else {
            // Mouse outside client.
        }
        
        for (int i = 0; i < BwdCursors.cursorList().size(); i++) {
            final int cursor = BwdCursors.cursorList().get(i);
            
            final Object oldKey = this.cursorAddKeyByCursor.get(cursor);
            if (i == pointedCellIndex) {
                if (oldKey == null) {
                    final Object newKey = cursorManager.addCursorAndGetAddKey(cursor);
                    this.cursorAddKeyByCursor.put(cursor, newKey);
                }
            } else {
                if (oldKey != null) {
                    cursorManager.removeCursorForAddKey(oldKey);
                    this.cursorAddKeyByCursor.remove(cursor);
                }
            }
        }
    }
}
