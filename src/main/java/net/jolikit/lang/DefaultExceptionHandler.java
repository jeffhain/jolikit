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
package net.jolikit.lang;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Does same output as JDK's default.
 */
public class DefaultExceptionHandler implements UncaughtExceptionHandler {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private boolean mustSwallowElseRethrow;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param mustSwallowElseRethrow If true, exceptions are swallowed,
     *        else rethrown.
     */
    public DefaultExceptionHandler(boolean mustSwallowElseRethrow) {
        this.mustSwallowElseRethrow = mustSwallowElseRethrow;
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        /*
         * Synchronizing to avoid mixed up logs.
         */
        synchronized (System.err) {
            System.err.print("Exception in thread \"" + thread.getName() + "\" ");
            throwable.printStackTrace(System.err);
        }
        if (!this.mustSwallowElseRethrow) {
            Unchecked.throwIt(throwable);
        }
    }
}
