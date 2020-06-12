/*
 * Copyright 2019-2020 Jeff Hain
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

import java.util.ArrayList;
import java.util.Collection;

import org.lwjgl.system.CallbackI;

/**
 * Makes it easier to keep strong references to callbacks.
 */
public abstract class AbstractLwjglEventListener {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * To keep strong references to callbacks.
     * 
     * "ALWAYS KEEP A STRONG REFERENCE TO THE CALLBACK.
     * Otherwise the callback will be garbage collected
     * which will cause quite a tricky to debug error."
     */
    private final Collection<CallbackI> callbackColl = new ArrayList<CallbackI>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractLwjglEventListener() {
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param callback Some callback.
     * @return The specified callback, after storage of its reference
     *         into an internal collection.
     */
    protected <T extends CallbackI> T inColl(T callback) {
        this.callbackColl.add(callback);
        return callback;
    }
}
