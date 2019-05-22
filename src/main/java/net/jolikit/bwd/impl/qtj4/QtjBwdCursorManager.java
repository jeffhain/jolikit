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
package net.jolikit.bwd.impl.qtj4;

import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.impl.utils.cursor.AbstractBwdCursorManager;

import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QWidget;

public class QtjBwdCursorManager extends AbstractBwdCursorManager {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SortedMap<Integer,QCursor> BACKING_CURSOR_BY_CURSOR =
            new TreeMap<Integer,QCursor>();
    static {
        for (int cursor : BwdCursors.cursorList()) {
            final QCursor backingCursor = newBackingCursor(cursor);
            BACKING_CURSOR_BY_CURSOR.put(cursor, backingCursor);
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjBwdCursorManager(QWidget component) {
        super(component);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setCursor(Object component, int cursor) {
        final QWidget widget = (QWidget) component;
        
        final QCursor backingCursor = convertToBackingCursor(cursor);
        
        widget.setCursor(backingCursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static QCursor convertToBackingCursor(int cursor) {
        return BACKING_CURSOR_BY_CURSOR.get(cursor);
    }
    
    private static QCursor newBackingCursor(int cursor) {
        switch (cursor) {
        case BwdCursors.INVISIBLE: return new QCursor(CursorShape.BlankCursor);
        case BwdCursors.ARROW: return new QCursor(CursorShape.ArrowCursor);
        case BwdCursors.CROSSHAIR: return new QCursor(CursorShape.CrossCursor);
        case BwdCursors.IBEAM_TEXT: return new QCursor(CursorShape.IBeamCursor);
        case BwdCursors.WAIT: return new QCursor(CursorShape.WaitCursor);
        case BwdCursors.RESIZE_NESW: return new QCursor(CursorShape.SizeBDiagCursor);
        case BwdCursors.RESIZE_NWSE: return new QCursor(CursorShape.SizeFDiagCursor);
        case BwdCursors.RESIZE_NS: return new QCursor(CursorShape.SizeVerCursor);
        case BwdCursors.RESIZE_WE: return new QCursor(CursorShape.SizeHorCursor);
        case BwdCursors.HAND: return new QCursor(CursorShape.PointingHandCursor);
        case BwdCursors.MOVE: return new QCursor(CursorShape.SizeAllCursor);
        default:
            throw new AssertionError("" + cursor);
        }
    }
}
