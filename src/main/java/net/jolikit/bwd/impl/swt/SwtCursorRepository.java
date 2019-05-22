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
package net.jolikit.bwd.impl.swt;

import java.util.IdentityHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.lang.LangUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

public class SwtCursorRepository {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final SortedMap<Integer,Cursor> backingCursorByCursor =
            new TreeMap<Integer,Cursor>();
    
    private final IdentityHashMap<Cursor,Integer> cursorByBackingCursor =
            new IdentityHashMap<Cursor,Integer>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SwtCursorRepository(Display display) {
        LangUtils.requireNonNull(display);
        
        // Cleanup in case of multiple init calls.
        this.close();
        
        for (int cursor : BwdCursors.cursorList()) {
            final Cursor backingCursor = newBackingCursor(display, cursor);
            if (backingCursor != null) {
                this.backingCursorByCursor.put(cursor, backingCursor);
                this.cursorByBackingCursor.put(backingCursor, cursor);
            }
        }
    }
    
    public void close() {
        for (Cursor backingCursor : this.cursorByBackingCursor.keySet()) {
            backingCursor.dispose();
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
    public int convertToCursor(Cursor backingCursor) {
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
    public Cursor convertToBackingCursor(int cursor) {
        return this.backingCursorByCursor.get(cursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static Cursor newBackingCursor(Display display, int cursor) {
        switch (cursor) {
        case BwdCursors.INVISIBLE: {
            if (false) {
                // Creates a "you shall not pass" cursor, not "no" cursor.
                return new Cursor(display, SWT.CURSOR_NO);
            }
            final int width = 1;
            final int height = 1;
            final PaletteData paletteData = new PaletteData(
                    0xFF000000,
                    0x00FF0000,
                    0x0000FF00);
            final int bytePerPixel = 4;
            final int depth = 8 * bytePerPixel;
            // "If one scanline of the image is not a multiple of this number,
            // it will be padded with zeros until it is."
            final int scanlinePad = 1;
            final byte[] data = new byte[width * height * bytePerPixel];
            final ImageData imageData = new ImageData(
                    width, height,
                    depth, paletteData,
                    scanlinePad, data);
            final int hotspotX = 0;
            final int hotspotY = 0;
            final Cursor backingCursor = new Cursor(
                    display,
                    imageData,
                    hotspotX,
                    hotspotY);
            return backingCursor;
        }
        case BwdCursors.ARROW: return new Cursor(display, SWT.CURSOR_ARROW);
        case BwdCursors.CROSSHAIR: return new Cursor(display, SWT.CURSOR_CROSS);
        case BwdCursors.IBEAM_TEXT: return new Cursor(display, SWT.CURSOR_IBEAM);
        case BwdCursors.WAIT: return new Cursor(display, SWT.CURSOR_WAIT);
        case BwdCursors.RESIZE_NESW: return new Cursor(display, SWT.CURSOR_SIZESW);
        case BwdCursors.RESIZE_NWSE: return new Cursor(display, SWT.CURSOR_SIZESE);
        case BwdCursors.RESIZE_NS: return new Cursor(display, SWT.CURSOR_SIZES);
        case BwdCursors.RESIZE_WE: return new Cursor(display, SWT.CURSOR_SIZEE);
        case BwdCursors.HAND: return new Cursor(display, SWT.CURSOR_HAND);
        case BwdCursors.MOVE: return new Cursor(display, SWT.CURSOR_SIZEALL);
        default:
            throw new AssertionError("" + cursor);
        }
    }
}
