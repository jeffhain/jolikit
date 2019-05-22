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
package net.jolikit.bwd.impl.lwjgl3;

import net.jolikit.bwd.impl.utils.cursor.AbstractBwdCursorManager;
import net.jolikit.lang.LangUtils;

import org.lwjgl.glfw.GLFW;

public class LwjglBwdCursorManager extends AbstractBwdCursorManager {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final LwjglCursorRepository backingCursorRepository;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public LwjglBwdCursorManager(
            LwjglCursorRepository backingCursorRepository,
            long window) {
        super(window);
        this.backingCursorRepository = LangUtils.requireNonNull(backingCursorRepository);
    }
    
    public LwjglCursorRepository getBackingCursorRepository() {
        return this.backingCursorRepository;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void setCursor(Object component, int cursor) {
        final long window = ((Long) component).longValue();
        
        final Long backingCursor = this.backingCursorRepository.convertToBackingCursor(cursor);
        if (backingCursor == null) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
            
            /*
             * Not required to nullify here,
             * but it shouldn't hurt, in case
             * it is ever made visible.
             */
            final long NULL_CURSOR = 0;
            GLFW.glfwSetCursor(window, NULL_CURSOR);
        } else {
            GLFW.glfwSetCursor(window, backingCursor.longValue());
            
            // Ensuring cursor is shown, in case it has been hidden.
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }
}
