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
package net.jolikit.bwd.impl.awt;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.impl.utils.cursor.AbstractBwdCursorManager;

public class AwtBwdCursorManager extends AbstractBwdCursorManager {

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
    
    public AwtBwdCursorManager(Component component) {
        super(component);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setCursor(Object component, int cursor) {
        final Component cpt = (Component) component;
        
        final Cursor backingCursor = convertToBackingCursor(cursor);
        
        cpt.setCursor(backingCursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static Cursor convertToBackingCursor(int cursor) {
        return BACKING_CURSOR_BY_CURSOR.get(cursor);
    }

    private static Cursor newBackingCursor(int cursor) {
        switch (cursor) {
        case BwdCursors.INVISIBLE: {
            // Transparent 1 x 1 pixel cursor image.
            final BufferedImage cursorImg = new BufferedImage(
                    1, 1,
                    BufferedImage.TYPE_INT_ARGB);
            final Point hotSpot = new Point(0, 0);
            return Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg,
                hotSpot,
                "INVISIBLE");
        }
        case BwdCursors.ARROW: return new Cursor(Cursor.DEFAULT_CURSOR);
        case BwdCursors.CROSSHAIR: return new Cursor(Cursor.CROSSHAIR_CURSOR);
        case BwdCursors.IBEAM_TEXT: return new Cursor(Cursor.TEXT_CURSOR);
        case BwdCursors.WAIT: return new Cursor(Cursor.WAIT_CURSOR);
        case BwdCursors.RESIZE_NESW: return new Cursor(Cursor.SW_RESIZE_CURSOR);
        case BwdCursors.RESIZE_NWSE: return new Cursor(Cursor.SE_RESIZE_CURSOR);
        case BwdCursors.RESIZE_NS: return new Cursor(Cursor.S_RESIZE_CURSOR);
        case BwdCursors.RESIZE_WE: return new Cursor(Cursor.E_RESIZE_CURSOR);
        case BwdCursors.HAND: return new Cursor(Cursor.HAND_CURSOR);
        case BwdCursors.MOVE: return new Cursor(Cursor.MOVE_CURSOR);
        default:
            throw new AssertionError("" + cursor);
        }
    }
}
