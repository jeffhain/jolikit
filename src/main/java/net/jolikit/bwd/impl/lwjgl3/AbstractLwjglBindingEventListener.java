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

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWJoystickCallbackI;
import org.lwjgl.glfw.GLFWMonitorCallbackI;

/**
 * Helper for non-host-tied events.
 * 
 * Hides the complexity of callbacks profusion and their signature conflicts.
 */
public abstract class AbstractLwjglBindingEventListener extends AbstractLwjglEventListener {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Must call setCallbacks() for it to work.
     */
    public AbstractLwjglBindingEventListener() {
    }
    
    public void setCallbacks() {
        GLFW.glfwSetErrorCallback(inColl(new GLFWErrorCallbackI() {
            @Override
            public void invoke(int error, long description) {
                onErrorEvent(error, description);
            }
        }));
        
        GLFW.glfwSetJoystickCallback(inColl(new GLFWJoystickCallbackI() {
            @Override
            public void invoke(int joy, int event) {
                onJoystickEvent(joy, event);
            }
        }));
        
        GLFW.glfwSetMonitorCallback(inColl(new GLFWMonitorCallbackI() {
            @Override
            public void invoke(long monitor, int event) {
                onMonitorEvent(monitor, event);
            }
        }));
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected abstract void onErrorEvent(int error, long description);
    
    protected abstract void onJoystickEvent(int joy, int event);
    
    protected abstract void onMonitorEvent(long monitor, int event);
}
