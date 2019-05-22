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
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.glfw.GLFWWindowIconifyCallbackI;
import org.lwjgl.glfw.GLFWWindowPosCallbackI;
import org.lwjgl.glfw.GLFWWindowRefreshCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;

/**
 * Helper for host-tied events.
 * 
 * Hides the complexity of callbacks profusion and their signature conflicts.
 */
public abstract class AbstractLwjglHostEventListener extends AbstractLwjglEventListener {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final long window;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Must call setCallbacks() for it to work.
     */
    public AbstractLwjglHostEventListener(long window) {
        this.window = window;
    }
    
    public void setCallbacks() {
        final long theWindow = this.window;
        
        /*
         * Window events.
         */
        
        GLFW.glfwSetFramebufferSizeCallback(theWindow, inColl(new GLFWFramebufferSizeCallbackI() {
            @Override
            public void invoke(long window, int width, int height) {
                onFramebufferSizeEvent(width, height);
            }
        }));
        
        GLFW.glfwSetWindowIconifyCallback(theWindow, new GLFWWindowIconifyCallbackI() {
            @Override
            public void invoke(long window, boolean iconified) {
                onWindowIconifyEvent(iconified);
            }
        });
        
        GLFW.glfwSetWindowFocusCallback(theWindow, new GLFWWindowFocusCallbackI() {
            @Override
            public void invoke(long window, boolean focused) {
                onWindowFocusEvent(focused);
            }
        });
        
        GLFW.glfwSetWindowRefreshCallback(theWindow, new GLFWWindowRefreshCallbackI() {
            @Override
            public void invoke(long window) {
                onWindowRefreshEvent();
            }
        });
        
        GLFW.glfwSetWindowPosCallback(theWindow, new GLFWWindowPosCallbackI() {
            @Override
            public void invoke(long window, int xpos, int ypos) {
                onWindowPosEvent(xpos, ypos);
            }
        });
        
        GLFW.glfwSetWindowSizeCallback(theWindow, new GLFWWindowSizeCallbackI() {
            @Override
            public void invoke(long window, int width, int height) {
                onWindowSizeEvent(width, height);
            }
        });
        
        GLFW.glfwSetWindowCloseCallback(theWindow, new GLFWWindowCloseCallbackI() {
            @Override
            public void invoke(long window) {
                onWindowCloseEvent();
            }
        });
        
        /*
         * Key events.
         */

        GLFW.glfwSetKeyCallback(theWindow, inColl(new GLFWKeyCallbackI() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                onKeyEvent(key, scancode, action, mods);
            }
        }));
        
        GLFW.glfwSetCharModsCallback(theWindow, inColl(new GLFWCharModsCallbackI() {
            @Override
            public void invoke(long window, int codepoint, int mods) {
                onCharModsEvent(codepoint, mods);
            }
        }));

        GLFW.glfwSetCharCallback(theWindow, inColl(new GLFWCharCallbackI() {
            @Override
            public void invoke(long window, int codepoint) {
                onCharEvent(codepoint);
            }
        }));
        
        /*
         * Mouse events.
         */

        GLFW.glfwSetCursorPosCallback(theWindow, inColl(new GLFWCursorPosCallbackI() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                onCursorPosEvent(xpos, ypos);
            }
        }));
        
        GLFW.glfwSetCursorEnterCallback(theWindow, inColl(new GLFWCursorEnterCallbackI() {
            @Override
            public void invoke(long window, boolean entered) {
                onCursorEnterEvent(entered);
            }
        }));
        
        GLFW.glfwSetMouseButtonCallback(theWindow, inColl(new GLFWMouseButtonCallbackI() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                onMouseButtonEvent(button, action, mods);
            }
        }));
        
        /*
         * Wheel events.
         */
        
        GLFW.glfwSetScrollCallback(theWindow, new GLFWScrollCallbackI() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                onScrollEvent(xoffset, yoffset);
            }
        });
        
        /*
         * 
         */
        
        GLFW.glfwSetDropCallback(theWindow, inColl(new GLFWDropCallbackI() {
            @Override
            public void invoke(long window, int count, long names) {
                onDropEvent(count, names);
            }
        }));
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Window events.
     */
    
    protected abstract void onFramebufferSizeEvent(int width, int height);
    
    protected abstract void onWindowIconifyEvent(boolean iconified);
    
    protected abstract void onWindowFocusEvent(boolean focused);
    
    protected abstract void onWindowRefreshEvent();
    
    protected abstract void onWindowPosEvent(int xpos, int ypos);
    
    protected abstract void onWindowSizeEvent(int width, int height);
    
    protected abstract void onWindowCloseEvent();
    
    /*
     * Key events.
     */
    
    protected abstract void onKeyEvent(int key, int scancode, int action, int mods);
    
    protected abstract void onCharModsEvent(int codepoint, int mods);
    
    protected abstract void onCharEvent(int codepoint);
    
    /*
     * Mouse events.
     */
    
    protected abstract void onCursorPosEvent(double xpos, double ypos);
    
    protected abstract void onCursorEnterEvent(boolean entered);
    
    protected abstract void onMouseButtonEvent(int button, int action, int mods);
    
    /*
     * Wheel events.
     */
    
    protected abstract void onScrollEvent(double xoffset, double yoffset);
    
    /*
     * Drop event.
     */
    
    protected abstract void onDropEvent(int count, long names);
}
