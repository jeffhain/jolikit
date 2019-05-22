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
package net.jolikit.bwd.impl.jfx;

import java.util.SortedMap;
import java.util.TreeMap;

import javafx.scene.Cursor;
import javafx.scene.Node;
import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.impl.utils.cursor.AbstractBwdCursorManager;

public class JfxBwdCursorManager extends AbstractBwdCursorManager {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SortedMap<Integer,Cursor> BACKING_CURSOR_BY_CURSOR =
            new TreeMap<Integer,Cursor>();
    static {
        for (int cursor : BwdCursors.cursorList()) {
            final Cursor backingCursor = newBackingCursor(cursor);
            BACKING_CURSOR_BY_CURSOR.put(cursor, backingCursor);
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JfxBwdCursorManager(Node component) {
        super(component);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setCursor(Object component, int cursor) {
        final Node node = (Node) component;
        
        final Cursor backingCursor = convertToBackingCursor(cursor);
        
        node.setCursor(backingCursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static Cursor convertToBackingCursor(int cursor) {
        return BACKING_CURSOR_BY_CURSOR.get(cursor);
    }
    
    private static Cursor newBackingCursor(int cursor) {
        switch (cursor) {
        case BwdCursors.INVISIBLE: return Cursor.NONE;
        case BwdCursors.ARROW: return Cursor.DEFAULT;
        case BwdCursors.CROSSHAIR: return Cursor.CROSSHAIR;
        case BwdCursors.IBEAM_TEXT: return Cursor.TEXT;
        case BwdCursors.WAIT: return Cursor.WAIT;
        case BwdCursors.RESIZE_NESW: return Cursor.SW_RESIZE;
        case BwdCursors.RESIZE_NWSE: return Cursor.SE_RESIZE;
        case BwdCursors.RESIZE_NS: return Cursor.S_RESIZE;
        case BwdCursors.RESIZE_WE: return Cursor.E_RESIZE;
        case BwdCursors.HAND: return Cursor.HAND;
        case BwdCursors.MOVE: return Cursor.MOVE;
        default:
            throw new AssertionError("" + cursor);
        }
    }
}
