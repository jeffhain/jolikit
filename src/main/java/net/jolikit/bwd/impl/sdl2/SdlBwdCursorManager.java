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

import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.utils.cursor.AbstractBwdCursorManager;
import net.jolikit.lang.LangUtils;

import com.sun.jna.Pointer;

public class SdlBwdCursorManager extends AbstractBwdCursorManager {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;

    private final SdlCursorRepository backingCursorRepository;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SdlBwdCursorManager(SdlCursorRepository backingCursorRepository) {
        super(null);
        this.backingCursorRepository = LangUtils.requireNonNull(backingCursorRepository);
    }
    
    public SdlCursorRepository getBackingCursorRepository() {
        return this.backingCursorRepository;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setCursor(Object component, int cursor) {
        /*
         * TODO sdl Cursor is set globally, not for a particular window.
         * Hoping it should not cause much issue.
         */
        final Pointer backingCursor = this.backingCursorRepository.convertToBackingCursor(cursor);
        if (backingCursor == null) {
            // 0 means hide cursor.
            final int toggle = 0;
            LIB.SDL_ShowCursor(toggle);
        } else {
            LIB.SDL_SetCursor(backingCursor);
            
            // Ensuring cursor is shown, in case it has been hidden.
            final int toggle = 1;
            LIB.SDL_ShowCursor(toggle);
        }
    }
}
