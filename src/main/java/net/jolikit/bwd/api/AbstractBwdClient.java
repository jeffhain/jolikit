/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.api;

import net.jolikit.bwd.api.events.NoOpBwdEventListener;

/**
 * Abstract class to reduce boilerplate when implementing simple BWD Clients.
 * Defaults implementations for all event listening methods are all empty.
 */
public abstract class AbstractBwdClient extends NoOpBwdEventListener implements InterfaceBwdClient {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdHost host = null;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractBwdClient() {
    }
    
    @Override
    public void setHost(Object host) {
        this.host = (InterfaceBwdHost) host;
    }

    /**
     * This default implementation does nothing.
     */
    @Override
    public void processEventualBufferedEvents() {
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return The host this client is bound to.
     */
    protected InterfaceBwdHost getHost() {
        return this.host;
    }
}
