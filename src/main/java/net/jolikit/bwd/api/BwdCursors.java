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
package net.jolikit.bwd.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic cursors that should be supported by each binding.
 * 
 * Bindings can support additional cursors (typically either defined
 * by the backing library or custom cursors defined by the user),
 * as long as they don't conflict with the values defined in this class.
 * 
 * Not using an enum class, to make it easier to deal transparently
 * with these default cursors and with additional ones.
 */
public class BwdCursors {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MIN_CURSOR = 1;
    private static final int MAX_CURSOR = 11;
    /**
     * Keeping some room for eventual new cursors.
     */
    private static final int MIN_ADDITIONAL_CURSOR = 32;
    
    /*
     * 
     */
    
    /**
     * Not a cursor, or a cursor for which no specific value could be computed.
     * Not to be confused with INVISIBLE, which is a cursor.
     * 
     * Even though the case of an unknown cursor don't occur in BWD API
     * (cursors are just user inputs for cursor manager), we prefer to use
     * NO_STATEMENT than just NOT_A_CURSOR, for consistency with other enums,
     * and to make it applicable for eventual additional APIs in which
     * unknown cursors could occur.
     * 
     * Note (it has no impact on our API, but has for bindings implementations)
     * that for some backing libraries, no (backing) cursor means an invisible
     * (backing) cursor.
     */
    public static final int NO_STATEMENT = 0;
    
    /*
     * Actual cursors.
     */
    
    /**
     * Invisible, yet effective.
     * Also called blank cursor.
     */
    public static final int INVISIBLE = MIN_CURSOR;
    
    /**
     * Usually the default.
     */
    public static final int ARROW = 2;
    
    public static final int CROSSHAIR = 3;
    
    /**
     * Also called edit cursor.
     */
    public static final int IBEAM_TEXT = 4;
    
    /**
     * Preferably just wait cursor, else arrow + wait.
     * Also called busy cursor.
     */
    public static final int WAIT = 5;
    
    /**
     * If only north-east or south-west are available,
     * must prefer south-west (i.e. north-east -> south-west).
     */
    public static final int RESIZE_NESW = 6;
    
    /**
     * If only north-west or south-east are available,
     * must prefer south-east (i.e. north-west -> south-east).
     */
    public static final int RESIZE_NWSE = 7;
    
    /**
     * If only north or south resize are available,
     * must prefer south (i.e. north -> south).
     */
    public static final int RESIZE_NS = 8;
    
    /**
     * If only west or east resize are available,
     * must prefer east (i.e. west -> east).
     */
    public static final int RESIZE_WE = 9;
    
    /**
     * Preferably pointing, else open.
     * Also called link cursor.
     */
    public static final int HAND = 10;
    
    /**
     * Typically the union of RESIZE_NS and RESIZE_WE cursors.
     */
    public static final int MOVE = MAX_CURSOR;

    /*
     * 
     */
    
    private static final List<Integer> CURSOR_LIST;
    static {
        final List<Integer> list = new ArrayList<Integer>();
        // Our values are contiguous.
        for (int cursor = MIN_CURSOR; cursor <= MAX_CURSOR; cursor++) {
            list.add(cursor);
        }
        CURSOR_LIST = Collections.unmodifiableList(list);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Methods rather than constants, to avoid confusion with constants.
     */
    
    /**
     * @return An unmodifiable list of the cursors defined in this class
     *         (NO_STATEMENT excluded), in increasing order. 
     */
    public static List<Integer> cursorList() {
        return CURSOR_LIST;
    }
    
    /**
     * @return Min value for eventual additional cursors that can be supported
     *         by a binding.
     */
    public static int minAdditionalCursor() {
        return MIN_ADDITIONAL_CURSOR;
    }
    
    /*
     * 
     */
    
    /**
     * @param cursor A cursor (can be any int value).
     * @return A string representation of the specified cursor.
     */
    public static String toString(int cursor) {
        switch (cursor) {
        case NO_STATEMENT: return "NO_STATEMENT";
        //
        case INVISIBLE: return "INVISIBLE";
        case ARROW: return "ARROW";
        case CROSSHAIR: return "CROSSHAIR";
        case IBEAM_TEXT: return "IBEAM_TEXT";
        case WAIT: return "WAIT";
        case RESIZE_NESW: return "RESIZE_NESW";
        case RESIZE_NWSE: return "RESIZE_NWSE";
        case RESIZE_NS: return "RESIZE_NS";
        case RESIZE_WE: return "RESIZE_WE";
        case HAND: return "HAND";
        case MOVE: return "MOVE";
        default:
            return "UNKNOWN_CURSOR(" + cursor + ")";
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private BwdCursors() {
    }
}
