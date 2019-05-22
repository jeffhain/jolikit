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
package net.jolikit.bwd.impl.utils.basics;

/**
 * Helper class to ensure that a treatment is only done once,
 * throwing in case of multiple calls.
 */
public class OneCallChecker {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Object mutex = new Object();
    
    private RuntimeException callStackForFirstCall = null;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public OneCallChecker() {
    }
    
    /**
     * Meant to just be called once (or never).
     * 
     * The check works even in case of concurrent calls.
     * 
     * @throws IllegalStateException if has already been called.
     */
    public void throwIfCalledMoreThanOnce() {
        synchronized (this.mutex) {
            final RuntimeException callStackForFirstCall = this.callStackForFirstCall;
            if (callStackForFirstCall != null) {
                throw new IllegalStateException(
                        "This method must only be called once. Call stack of the first call:",
                        callStackForFirstCall);
            }
            this.callStackForFirstCall = new RuntimeException("call stack for first call");
        }
    }
}
