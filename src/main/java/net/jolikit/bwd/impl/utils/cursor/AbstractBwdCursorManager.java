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
package net.jolikit.bwd.impl.utils.cursor;

import java.util.ArrayList;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.api.InterfaceBwdCursorManager;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Abstract class for simple implementations.
 */
public abstract class AbstractBwdCursorManager implements InterfaceBwdCursorManager {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Must not use cursor as key: we want each key to be specific
     * to an add action.
     */
    private static class MyKey {
        private final int cursor;
        private MyKey(int cursor) {
            this.cursor = cursor;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final ArrayList<MyKey> keysList = new ArrayList<MyKey>();
    
    /**
     * Can be null.
     */
    private final Object component;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a cursor manager that calls setCursor(component,cursor)
     * with the specified component, on each cursor add or removal.
     * 
     * @param component Component where to set current added cursor.
     *        Can be null, for libraries that don't use such a component.
     */
    public AbstractBwdCursorManager(Object component) {
        this.component = component;
    }

    @Override
    public int getCurrentAddedCursor() {
        final int size = this.keysList.size();
        if (size == 0) {
            return this.getDefaultCursor();
        } else {
            return this.keysList.get(size-1).cursor;
        }
    }
    
    @Override
    public Object addCursorAndGetAddKey(int cursor) {
        final MyKey key = new MyKey(cursor);
        this.keysList.add(key);
        final int newCursor = this.getCurrentAddedCursor();
        if (DEBUG) {
            Dbg.log("addCursor(" + cursor + ") : newCursor = " + BwdCursors.toString(newCursor));
        }
        this.setCursor(this.component, newCursor);
        return key;
    }
    
    @Override
    public void removeCursorForAddKey(Object key) {
        LangUtils.requireNonNull(key);
        // Early type check in case the specified key would have fancy equals.
        if ((!(key instanceof MyKey))
                || (!this.keysList.remove(key))) {
            throw new IllegalArgumentException("key unknown: " + key);
        }
        final int newCursor = this.getCurrentAddedCursor();
        if (DEBUG) {
            Dbg.log("removeCursorForKey(...) : newCursor = " + BwdCursors.toString(newCursor));
        }
        this.setCursor(this.component, newCursor);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * This default implementation returns BwdCursors.ARROW.
     * Override if the backing library uses a different one.
     * 
     * @return The cursor used when no cursor has been added.
     */
    protected int getDefaultCursor() {
        return BwdCursors.ARROW;
    }
    
    protected abstract void setCursor(Object component, int cursor);
}
