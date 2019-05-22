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
package net.jolikit.bwd.impl.sdl2;

import java.util.IdentityHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlSystemCursor;
import net.jolikit.bwd.impl.utils.basics.BindingError;

import com.sun.jna.Pointer;

public class SdlCursorRepository {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;

    private final SortedMap<Integer,Pointer> backingCursorByCursor =
            new TreeMap<Integer,Pointer>();
    
    private final IdentityHashMap<Pointer,Integer> cursorByBackingCursor =
            new IdentityHashMap<Pointer,Integer>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SdlCursorRepository() {
    }
    
    public void init() {
        // Cleanup in case of multiple init calls.
        this.close();
        
        for (int cursor : BwdCursors.cursorList()) {
            final Pointer backingCursor = newBackingCursor(cursor);
            if (backingCursor != null) {
                this.backingCursorByCursor.put(cursor, backingCursor);
                this.cursorByBackingCursor.put(backingCursor, cursor);
            }
        }
    }
    
    public void close() {
        for (Pointer backingCursor : this.cursorByBackingCursor.keySet()) {
            LIB.SDL_FreeCursor(backingCursor);
        }
        this.backingCursorByCursor.clear();
        this.cursorByBackingCursor.clear();
    }
    
    /**
     * @param backingCursor A backing cursor returned by the convert(cursor)
     *        method of this instance, or null.
     * @return The corresponding cursor.
     * @throws IllegalArgumentException if the specified backing cursor
     *         is not null and does not come from this instance.
     */
    public int convertToCursor(Pointer backingCursor) {
        if (backingCursor == null) {
            return BwdCursors.INVISIBLE;
        }
        final Integer cursorRef = this.cursorByBackingCursor.get(backingCursor);
        if (cursorRef == null) {
            throw new IllegalArgumentException(
                    "backing cursor is not from this insance : " + backingCursor);
        }
        return cursorRef.intValue();
    }
    
    /**
     * @param cursor A cursor.
     * @return The corresponding backing cursor, possibly null.
     */
    public Pointer convertToBackingCursor(int cursor) {
        return this.backingCursorByCursor.get(cursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return The corresponding backing cursor, possibly null.
     */
    private static Pointer newBackingCursor(int cursor) {
        final int id = computeBackingCursorId(cursor);
        if (id < 0) {
            return null;
        }
        final Pointer backingCursor = LIB.SDL_CreateSystemCursor(id);
        if (backingCursor == null) {
            throw new BindingError("could not create cursor of id " + id);
        }
        return backingCursor;
    }
    
    /**
     * @return Backing system cursor id, or -1 for no cursor.
     */
    private static int computeBackingCursorId(int cursor) {
        switch (cursor) {
        case BwdCursors.INVISIBLE: return -1;
        case BwdCursors.ARROW: return SdlSystemCursor.SDL_SYSTEM_CURSOR_ARROW.intValue();
        case BwdCursors.CROSSHAIR: return SdlSystemCursor.SDL_SYSTEM_CURSOR_CROSSHAIR.intValue();
        case BwdCursors.IBEAM_TEXT: return SdlSystemCursor.SDL_SYSTEM_CURSOR_IBEAM.intValue();
        case BwdCursors.WAIT: return SdlSystemCursor.SDL_SYSTEM_CURSOR_WAIT.intValue();
        case BwdCursors.RESIZE_NESW: return SdlSystemCursor.SDL_SYSTEM_CURSOR_SIZENESW.intValue();
        case BwdCursors.RESIZE_NWSE: return SdlSystemCursor.SDL_SYSTEM_CURSOR_SIZENWSE.intValue();
        case BwdCursors.RESIZE_NS: return SdlSystemCursor.SDL_SYSTEM_CURSOR_SIZENS.intValue();
        case BwdCursors.RESIZE_WE: return SdlSystemCursor.SDL_SYSTEM_CURSOR_SIZEWE.intValue();
        case BwdCursors.HAND: return SdlSystemCursor.SDL_SYSTEM_CURSOR_HAND.intValue();
        case BwdCursors.MOVE: return SdlSystemCursor.SDL_SYSTEM_CURSOR_SIZEALL.intValue();
        default:
            throw new AssertionError("" + cursor);
        }
    }
}
