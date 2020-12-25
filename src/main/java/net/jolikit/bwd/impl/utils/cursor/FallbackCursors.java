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
package net.jolikit.bwd.impl.utils.cursor;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.lang.NbrsUtils;

/**
 * Cursors that can be used when the backing library doesn't define them.
 */
public class FallbackCursors {

    /*
     * TODO Could have cursors in some png resources,
     * once we figure out a simple and reliable way of loading resources,
     * but on another hand it's so much simpler to just have it in code,
     * in particular when it's not meant to change often.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int MAX_CURSOR = BwdCursors.cursorList().get(BwdCursors.cursorList().size() - 1);
    
    /**
     * null for INVISIBLE cursor.
     */
    private static final MyCursorData[] CURSOR_DATA_BY_CURSOR = new MyCursorData[MAX_CURSOR + 1];
    
    /**
     * Opaque black.
     * 
     * NB: Can use red or blue, and check that red or blue
     * is displayed, to make sure that RGB/BGR ordering
     * is properly done.
     */
    private static final int B = 0xFF000000;
    /**
     * Opaque white.
     */
    private static final int W = 0xFFFFFFFF;
    /**
     * Transparent.
     */
    private static final int T = 0x00000000;
    
    static {
        CURSOR_DATA_BY_CURSOR[BwdCursors.ARROW] =
                new MyCursorData(
                        13, 21,
                        0, 0,
                        new int[]{
                                B,T,T,T,T,T,T,T,T,T,T,T,T,
                                B,B,T,T,T,T,T,T,T,T,T,T,T,
                                B,W,B,T,T,T,T,T,T,T,T,T,T,
                                B,W,W,B,T,T,T,T,T,T,T,T,T,
                                B,W,W,W,B,T,T,T,T,T,T,T,T,
                                B,W,W,W,W,B,T,T,T,T,T,T,T,
                                B,W,W,W,W,W,B,T,T,T,T,T,T,
                                B,W,W,W,W,W,W,B,T,T,T,T,T,
                                B,W,W,W,W,W,W,W,B,T,T,T,T,
                                B,W,W,W,W,W,W,W,W,B,T,T,T,
                                B,W,W,W,W,W,W,W,W,W,B,T,T,
                                B,W,W,W,W,W,W,B,B,B,B,B,T,
                                B,W,W,W,B,W,W,B,T,T,T,T,T,
                                B,W,W,B,B,W,W,B,T,T,T,T,T,
                                B,W,B,T,T,B,W,W,B,T,T,T,T,
                                B,B,T,T,T,B,W,W,B,T,T,T,T,
                                B,T,T,T,T,T,B,W,W,B,T,T,T,
                                T,T,T,T,T,T,B,W,W,B,T,T,T,
                                T,T,T,T,T,T,T,B,W,W,B,T,T,
                                T,T,T,T,T,T,T,B,W,W,B,T,T,
                                T,T,T,T,T,T,T,T,B,B,T,T,T,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.CROSSHAIR] =
                new MyCursorData(
                        23, 23,
                        11, 11,
                        new int[]{
                                T,T,T,T,T,T,T,T,T,T,W,W,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                W,W,W,W,W,W,W,W,W,W,W,B,W,W,W,W,W,W,W,W,W,W,W,
                                W,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,W,
                                W,W,W,W,W,W,W,W,W,W,W,B,W,W,W,W,W,W,W,W,W,W,W,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,W,W,T,T,T,T,T,T,T,T,T,T,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.IBEAM_TEXT] =
                new MyCursorData(
                        9, 18,
                        4, 8,
                        new int[]{
                                T,W,W,W,T,W,W,W,T,
                                W,B,B,B,W,B,B,B,W,
                                T,W,W,W,B,W,W,W,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,W,W,W,B,W,W,W,T,
                                W,B,B,B,W,B,B,B,W,
                                T,W,W,W,T,W,W,W,T,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.WAIT] =
                new MyCursorData(
                        13, 22,
                        6, 10,
                        new int[]{
                                B,B,B,B,B,B,B,B,B,B,B,B,B,
                                B,B,W,W,W,W,W,W,W,W,W,B,B,
                                B,B,B,B,B,B,B,B,B,B,B,B,B,
                                T,B,W,W,W,W,W,W,W,W,W,B,T,
                                T,B,W,W,W,W,W,W,W,W,W,B,T,
                                T,B,W,W,B,W,B,W,B,W,W,B,T,
                                T,B,W,W,W,B,W,B,W,W,W,B,T,
                                T,B,B,W,W,W,B,W,W,W,B,B,T,
                                T,T,B,B,W,W,W,W,W,B,B,T,T,
                                T,T,T,B,B,W,B,W,B,B,T,T,T,
                                T,T,T,T,B,B,W,B,B,T,T,T,T,
                                T,T,T,T,B,B,W,B,B,T,T,T,T,
                                T,T,T,B,B,W,W,W,B,B,T,T,T,
                                T,T,B,B,W,W,B,W,W,B,B,T,T,
                                T,B,B,W,W,W,W,W,W,W,B,B,T,
                                T,B,W,W,W,W,B,W,W,W,W,B,T,
                                T,B,W,W,W,B,W,B,W,W,W,B,T,
                                T,B,W,W,B,W,B,W,B,W,W,B,T,
                                T,B,W,B,W,B,W,B,W,B,W,B,T,
                                B,B,B,B,B,B,B,B,B,B,B,B,B,
                                B,B,W,W,W,W,W,W,W,W,W,B,B,
                                B,B,B,B,B,B,B,B,B,B,B,B,B,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.RESIZE_NESW] =
                new MyCursorData(
                        17, 17,
                        8, 8,
                        new int[]{
                                T,T,T,T,T,T,T,T,T,T,W,W,W,W,W,W,W,
                                T,T,T,T,T,T,T,T,T,T,W,B,B,B,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,T,W,B,B,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,T,T,W,B,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,T,W,B,W,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,W,B,W,
                                T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,W,W,
                                T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,
                                W,W,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,
                                W,B,W,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,W,B,W,T,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,B,W,T,T,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,B,B,W,T,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,B,B,B,W,T,T,T,T,T,T,T,T,T,T,
                                W,W,W,W,W,W,W,T,T,T,T,T,T,T,T,T,T,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.RESIZE_NWSE] =
                new MyCursorData(
                        17, 17,
                        8, 8,
                        new int[]{
                                W,W,W,W,W,W,W,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,B,B,B,W,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,B,B,W,T,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,B,W,T,T,T,T,T,T,T,T,T,T,T,T,
                                W,B,B,W,B,W,T,T,T,T,T,T,T,T,T,T,T,
                                W,B,W,T,W,B,W,T,T,T,T,T,T,T,T,T,T,
                                W,W,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,W,W,
                                T,T,T,T,T,T,T,T,T,T,W,B,W,T,W,B,W,
                                T,T,T,T,T,T,T,T,T,T,T,W,B,W,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,T,T,W,B,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,T,W,B,B,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,W,B,B,B,B,B,W,
                                T,T,T,T,T,T,T,T,T,T,W,W,W,W,W,W,W,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.RESIZE_NS] =
                new MyCursorData(
                        9, 22,
                        4, 10,
                        new int[]{
                                T,T,T,T,W,T,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,W,B,B,B,W,T,T,
                                T,W,B,B,B,B,B,W,T,
                                W,B,B,B,B,B,B,B,W,
                                W,W,W,W,B,W,W,W,W,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,W,B,W,T,T,T,
                                W,W,W,W,B,W,W,W,W,
                                W,B,B,B,B,B,B,B,W,
                                T,W,B,B,B,B,B,W,T,
                                T,T,W,B,B,B,W,T,T,
                                T,T,T,W,B,W,T,T,T,
                                T,T,T,T,W,T,T,T,T,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.RESIZE_WE] =
                new MyCursorData(
                        22, 9,
                        10, 4,
                        new int[]{
                                T,T,T,T,W,W,T,T,T,T,T,T,T,T,T,T,W,W,T,T,T,T,
                                T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,
                                T,T,W,B,B,W,T,T,T,T,T,T,T,T,T,T,W,B,B,W,T,T,
                                T,W,B,B,B,W,W,W,W,W,W,W,W,W,W,W,W,B,B,B,W,T,
                                W,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,W,
                                T,W,B,B,B,W,W,W,W,W,W,W,W,W,W,W,W,B,B,B,W,T,
                                T,T,W,B,B,W,T,T,T,T,T,T,T,T,T,T,W,B,B,W,T,T,
                                T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,
                                T,T,T,T,W,W,T,T,T,T,T,T,T,T,T,T,W,W,T,T,T,T,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.HAND] =
                new MyCursorData(
                        17, 22,
                        5, 0,
                        new int[]{
                                T,T,T,T,T,B,B,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,B,W,W,B,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,B,W,W,B,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,B,W,W,B,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,B,W,W,B,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,B,W,W,B,B,B,T,T,T,T,T,T,T,
                                T,T,T,T,B,W,W,B,W,W,B,B,B,T,T,T,T,
                                T,T,T,T,B,W,W,B,W,W,B,W,W,B,B,T,T,
                                T,T,T,T,B,W,W,B,W,W,B,W,W,B,W,B,T,
                                B,B,B,T,B,W,W,B,W,W,B,W,W,B,W,W,B,
                                B,W,W,B,B,W,W,W,W,W,W,W,W,B,W,W,B,
                                B,W,W,W,B,W,W,W,W,W,W,W,W,W,W,W,B,
                                T,B,W,W,B,W,W,W,W,W,W,W,W,W,W,W,B,
                                T,T,B,W,B,W,W,W,W,W,W,W,W,W,W,W,B,
                                T,T,B,W,W,W,W,W,W,W,W,W,W,W,W,W,B,
                                T,T,T,B,W,W,W,W,W,W,W,W,W,W,W,W,B,
                                T,T,T,B,W,W,W,W,W,W,W,W,W,W,W,B,T,
                                T,T,T,T,B,W,W,W,W,W,W,W,W,W,W,B,T,
                                T,T,T,T,B,W,W,W,W,W,W,W,W,W,W,B,T,
                                T,T,T,T,T,B,W,W,W,W,W,W,W,W,B,T,T,
                                T,T,T,T,T,B,W,W,W,W,W,W,W,W,B,T,T,
                                T,T,T,T,T,B,B,B,B,B,B,B,B,B,B,T,T,
                        });
        CURSOR_DATA_BY_CURSOR[BwdCursors.MOVE] =
                new MyCursorData(
                        21, 21,
                        10, 10,
                        new int[]{
                                T,T,T,T,T,T,T,T,T,T,W,T,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,W,B,B,B,W,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,W,B,B,B,B,B,W,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,W,B,B,B,B,B,B,B,W,T,T,T,T,T,T,
                                T,T,T,T,T,T,W,W,W,W,B,W,W,W,W,T,T,T,T,T,T,
                                T,T,T,T,W,W,T,T,T,W,B,W,T,T,T,W,W,T,T,T,T,
                                T,T,T,W,B,W,T,T,T,W,B,W,T,T,T,W,B,W,T,T,T,
                                T,T,W,B,B,W,T,T,T,W,B,W,T,T,T,W,B,B,W,T,T,
                                T,W,B,B,B,W,W,W,W,W,B,W,W,W,W,W,B,B,B,W,T,
                                W,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,B,W,
                                T,W,B,B,B,W,W,W,W,W,B,W,W,W,W,W,B,B,B,W,T,
                                T,T,W,B,B,W,T,T,T,W,B,W,T,T,T,W,B,B,W,T,T,
                                T,T,T,W,B,W,T,T,T,W,B,W,T,T,T,W,B,W,T,T,T,
                                T,T,T,T,W,W,T,T,T,W,B,W,T,T,T,W,W,T,T,T,T,
                                T,T,T,T,T,T,W,W,W,W,B,W,W,W,W,T,T,T,T,T,T,
                                T,T,T,T,T,T,W,B,B,B,B,B,B,B,W,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,W,B,B,B,B,B,W,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,W,B,B,B,W,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,W,B,W,T,T,T,T,T,T,T,T,T,
                                T,T,T,T,T,T,T,T,T,T,W,T,T,T,T,T,T,T,T,T,T,
                        });
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyCursorData {
        final int width;
        final int height;
        final int hotX;
        final int hotY;
        final int[] argb32Arr;
        public MyCursorData(
                int width,
                int height,
                int hotX,
                int hotY,
                int[] argb32Arr) {
            final int prod = width * height;
            if (prod != argb32Arr.length) {
                throw new IllegalArgumentException(width + " * " + height + " = " + prod + " != " + argb32Arr.length);
            }
            NbrsUtils.requireInRange(0, width - 1, hotX, "hotX");
            NbrsUtils.requireInRange(0, height - 1, hotY, "hotY");
            this.width = width;
            this.height = height;
            this.hotX = hotX;
            this.hotY = hotY;
            this.argb32Arr = argb32Arr;
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @return True if data for the specified cursor is available,
     *         false otherwise.
     */
    public static boolean isAvailable(int cursor) {
        if ((cursor < 0) && (cursor >= CURSOR_DATA_BY_CURSOR.length)) {
            return false;
        }
        return CURSOR_DATA_BY_CURSOR[cursor] != null;
    }

    /**
     * @param cursor Must be available.
     */
    public static int width(int cursor) {
        return getData(cursor).width;
    }

    /**
     * @param cursor Must be available.
     */
    public static int height(int cursor) {
        return getData(cursor).height;
    }

    /**
     * @param cursor Must be available.
     */
    public static int hotX(int cursor) {
        return getData(cursor).hotX;
    }

    /**
     * @param cursor Must be available.
     */
    public static int hotY(int cursor) {
        return getData(cursor).hotY;
    }
    
    /**
     * @param cursor Must be available.
     */
    public static int[] argb32Arr(int cursor) {
        return getData(cursor).argb32Arr.clone();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private FallbackCursors() {
    }
    
    private static MyCursorData getData(int cursor) {
        final MyCursorData data = CURSOR_DATA_BY_CURSOR[cursor];
        if (data == null) {
            throw new IllegalArgumentException("no data for cursor " + BwdCursors.toString(cursor));
        }
        return data;
    }
}
