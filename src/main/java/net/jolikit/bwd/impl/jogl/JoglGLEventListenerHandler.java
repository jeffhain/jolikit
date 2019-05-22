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

import java.lang.Thread.UncaughtExceptionHandler;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

/**
 * TODO jogl JOGL seems to silently swallow exceptions,
 * so we use this wrapper to catch them and report them
 * in some way.
 */
public class JoglGLEventListenerHandler implements GLEventListener {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final UncaughtExceptionHandler exceptionHandler;
    
    private final GLEventListener userListener;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JoglGLEventListenerHandler(
            UncaughtExceptionHandler exceptionHandler,
            GLEventListener userListener) {
        this.exceptionHandler = exceptionHandler;
        this.userListener = userListener;
    }
    
    @Override
    public void init(GLAutoDrawable drawable) {
        try {
            this.userListener.init(drawable);
        } catch (Throwable t) {
            this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
        }
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        try {
            this.userListener.reshape(drawable, x, y, width, height);
        } catch (Throwable t) {
            this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
        }
    }
    
    @Override
    public void display(GLAutoDrawable drawable) {
        try {
            this.userListener.display(drawable);
        } catch (Throwable t) {
            this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
        }
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
        try {
            this.userListener.dispose(drawable);
        } catch (Throwable t) {
            this.exceptionHandler.uncaughtException(Thread.currentThread(), t);
        }
    }
}
