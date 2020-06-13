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
package net.jolikit.bwd.impl.algr5;

import java.util.IdentityHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_LOCKED_REGION;
import net.jolikit.bwd.impl.algr5.jlib.AlgrSystemMouseCursor;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaUtils;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.cursor.FallbackCursors;

import com.sun.jna.Pointer;

public class AlgrCursorRepository {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    
    private final boolean mustUseSystemCursorsWhenAvailable;
    
    /**
     * Value: Integer for system cursors, or Pointer for custom cursors.
     */
    private final SortedMap<Integer,Object> backingCursorByCursor =
            new TreeMap<Integer,Object>();
    
    private final IdentityHashMap<Object,Integer> cursorByBackingCursor =
            new IdentityHashMap<Object,Integer>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AlgrCursorRepository(boolean mustUseSystemCursorsWhenAvailable) {
        this.mustUseSystemCursorsWhenAvailable = mustUseSystemCursorsWhenAvailable;
    }
    
    public void init() {
        // Cleanup in case of multiple init calls.
        this.close();
        
        for (int cursor : BwdCursors.cursorList()) {
            final Object backingCursor = newBackingCursor(cursor);
            if (backingCursor != null) {
                this.backingCursorByCursor.put(cursor, backingCursor);
                this.cursorByBackingCursor.put(backingCursor, cursor);
            }
        }
    }
    
    public void close() {
        for (Object backingCursor : this.cursorByBackingCursor.keySet()) {
            if (backingCursor instanceof Pointer) {
                LIB.al_destroy_mouse_cursor((Pointer) backingCursor);
            }
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
    public int convertToCursor(Object backingCursor) {
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
    public Object convertToBackingCursor(int cursor) {
        return this.backingCursorByCursor.get(cursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return The corresponding backing cursor, possibly null.
     */
    private Object newBackingCursor(int cursor) {
        if (cursor == BwdCursors.INVISIBLE) {
            return null;
        }
        
        /*
         * System cursors.
         */
        
        if (this.mustUseSystemCursorsWhenAvailable) {
            switch (cursor) {
            case BwdCursors.ARROW: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_ARROW.intValue();
            case BwdCursors.CROSSHAIR: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_PRECISION.intValue();
            case BwdCursors.IBEAM_TEXT: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_EDIT.intValue();
            case BwdCursors.WAIT: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_BUSY.intValue();
            case BwdCursors.RESIZE_NESW: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_SW.intValue();
            case BwdCursors.RESIZE_NWSE: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_SE.intValue();
            case BwdCursors.RESIZE_NS: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_S.intValue();
            case BwdCursors.RESIZE_WE: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_RESIZE_E.intValue();
            case BwdCursors.HAND: return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_LINK.intValue();
            case BwdCursors.MOVE: {
                if (OsUtils.isMac()) {
                    /*
                     * TODO algr On Mac, MOVE system cursor (6) doesn't work, with error code 34:
                     * Exception in thread "main" net.jolikit.bwd.impl.utils.basics.BindingError: could not set system cursor: 34
                     *     at net.jolikit.bwd.impl.algr5.AlgrBwdCursorManager.setCursor(AlgrBwdCursorManager.java:76)
                     * 
                     * Instead, we use fallback cursor.
                     */
                    break;
                } else {
                    return AlgrSystemMouseCursor.ALLEGRO_SYSTEM_MOUSE_CURSOR_MOVE.intValue();
                }
            }
            default:
                throw new AssertionError("" + cursor);
            }
        }
        
        /*
         * Fallback cursors.
         */
        
        if (!FallbackCursors.isAvailable(cursor)) {
            return null;
        }
        
        final int width = FallbackCursors.width(cursor);
        final int height = FallbackCursors.height(cursor);
        final int hotX = FallbackCursors.hotX(cursor);
        final int hotY = FallbackCursors.hotY(cursor);
        final int[] argb32Arr = FallbackCursors.argb32Arr(cursor);
        
        final Pointer backingCursor;
        
        final Pointer sprite = LIB.al_create_bitmap(width, height);
        if (sprite == null) {
            throw new BindingError("could not create bitmap: " + LIB.al_get_errno());
        }
        try {
            final int spriteLockFormat = AlgrFormatUtils.getLockFormat(sprite);
            final Pointer regionPtr = LIB.al_lock_bitmap(
                    sprite,
                    spriteLockFormat,
                    AlgrJnaLib.ALLEGRO_LOCK_READWRITE);
            if (regionPtr == null) {
                throw new BindingError("could not lock bitmap: " + LIB.al_get_errno());
            }
            try {
                final ALLEGRO_LOCKED_REGION region = AlgrJnaUtils.newAndRead(
                        ALLEGRO_LOCKED_REGION.class,
                        regionPtr);
                
                final Pointer dataPtr = region.data;
                
                /*
                 * al_create_bitmap(...) doesn't zeroize,
                 * but we don't need to since we write data
                 * all over it.
                 */
                
                final int scanlineStride = width;
                
                for (int j = 0; j < height; j++) {
                    final long offset = region.pitch * j;
                    final int index = scanlineStride * j;
                    final int length = width;
                    dataPtr.write(offset, argb32Arr, index, length);
                }
            } finally {
                LIB.al_unlock_bitmap(sprite);
            }
            
            backingCursor = LIB.al_create_mouse_cursor(sprite, hotX, hotY);
            
        } finally {
            LIB.al_destroy_bitmap(sprite);
        }

        return backingCursor;
    }
}
