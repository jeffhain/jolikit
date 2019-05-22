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
package net.jolikit.bwd.impl.utils;

/**
 * Interface for treatments working on a host's backing window.
 */
public interface InterfaceBackingWindowHolder {
    
    /**
     * Useful for logs.
     */
    public String getTitle();

    /**
     * Must only be called from UI thread.
     * 
     * Useful to avoid crashes with C-or-similar based libraries,
     * when the backing window has been deleted and close() has been called.
     * 
     * @return True if close() has been called.
     */
    public boolean isClosed_nonVolatile();

    public boolean isDecorated();
    
    public Object getBackingWindow();
}
