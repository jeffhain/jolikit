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
package net.jolikit.bwd.impl.jogl;

import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.opengl.GLWindow;

import net.jolikit.bwd.impl.utils.cursor.AbstractBwdCursorManager;
import net.jolikit.lang.LangUtils;

public class JoglBwdCursorManager extends AbstractBwdCursorManager {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final JoglCursorRepository backingCursorRepository;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JoglBwdCursorManager(
            JoglCursorRepository backingCursorRepository,
            GLWindow component) {
        super(component);
        this.backingCursorRepository = LangUtils.requireNonNull(backingCursorRepository);
    }
    
    public JoglCursorRepository getBackingCursorRepository() {
        return this.backingCursorRepository;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setCursor(Object component, int cursor) {
        final GLWindow window = (GLWindow) component;
        
        final PointerIcon backingCursor = this.backingCursorRepository.convertToBackingCursor(cursor);
        if (backingCursor == null) {
            window.setPointerVisible(false);
            /*
             * Not required to nullify here,
             * which cause cursor to be default,
             * but it shouldn't hurt, in case
             * it is ever made visible.
             */
            window.setPointerIcon(null);
        } else {
            window.setPointerIcon(backingCursor);
            window.setPointerVisible(true);
        }
    }
}
