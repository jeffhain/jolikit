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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.cursor.AbstractBwdCursorManager;
import net.jolikit.lang.LangUtils;

import com.sun.jna.Pointer;

public class AlgrBwdCursorManager extends AbstractBwdCursorManager {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;

    private final AlgrCursorRepository backingCursorRepository;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AlgrBwdCursorManager(
            AlgrCursorRepository backingCursorRepository,
            Pointer display) {
        super(display);
        this.backingCursorRepository = LangUtils.requireNonNull(backingCursorRepository);
    }
    
    public AlgrCursorRepository getBackingCursorRepository() {
        return this.backingCursorRepository;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setCursor(Object component, int cursor) {
        final Pointer display = (Pointer) component;
        
        final Object backingCursor = this.backingCursorRepository.convertToBackingCursor(cursor);
        if (backingCursor == null) {
            {
                final boolean didIt = LIB.al_hide_mouse_cursor(display);
                if (!didIt) {
                    throw new BindingError("could not hide cursor: " + LIB.al_get_errno());
                }
            }
            
        } else {
            if (backingCursor instanceof Pointer) {
                final boolean didIt = LIB.al_set_mouse_cursor(display, (Pointer) backingCursor);
                if (!didIt) {
                    throw new BindingError("could not set custom cursor: " + LIB.al_get_errno());
                }
            } else {
                final boolean didIt = LIB.al_set_system_mouse_cursor(display, ((Integer) backingCursor).intValue());
                if (!didIt) {
                    throw new BindingError("could not set system cursor: " + LIB.al_get_errno());
                }
            }
            
            {
                final boolean didIt = LIB.al_show_mouse_cursor(display);
                if (!didIt) {
                    throw new BindingError("could not show cursor: " + LIB.al_get_errno());
                }
            }
        }
    }
}
