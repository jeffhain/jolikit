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

/**
 * Interface to centralize the setting and unsetting of specific cursors.
 * 
 * Allows to always set the proper cursor in the window, whatever the order
 * in which they are set and unset (for example by components of a toolkit
 * based on BWD, as the mouse moves over them).
 * 
 * Note that with some libraries, or on some platforms, some cursors (like
 * "crosshair" and "text"/"i-beam") are drawn with XOR mode,
 * which makes them invisible on gray (0.5,0.5,0.5) background,
 * so gray should be avoided where such cursors must be visible.
 */
public interface InterfaceBwdCursorManager {
    
    /**
     * @return Last added and non yet removed cursor if any,
     *         else default cursor.
     */
    public int getCurrentAddedCursor();
    
    /**
     * @param cursor Cursor to use, unless someone adds another cursor
     *        before this one gets removed.
     * @return Key to undo this add of the specified cursor.
     * @throws IllegalArgumentException if the specified cursor is invalid.
     */
    public Object addCursorAndGetAddKey(int cursor);
    
    /**
     * @param key Key returned by the cursor add to undo it.
     * @throws NullPointerException if the specified key is null.
     * @throws IllegalArgumentException if the specified key is unknown.
     */
    public void removeCursorForAddKey(Object key);
}
