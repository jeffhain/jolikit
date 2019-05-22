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

import net.jolikit.bwd.api.InterfaceBwdHost;

/**
 * To make sure a client doesn't receive a WINDOW_FOCUS_GAINED event
 * before proper firing of an eventual WINDOW_FOCUS_LOST event
 * for another client.
 * 
 * Storing the host, not the client, in case some client gets reused
 * across multiple hosts.
 */
public class HostOfFocusedClientHolder {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdHost hostOfFocusedClient;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public HostOfFocusedClientHolder() {
    }
    
    public void setHostOfFocusedClient(InterfaceBwdHost hostOfFocusedClient) {
        this.hostOfFocusedClient = hostOfFocusedClient;
    }

    public InterfaceBwdHost getHostOfFocusedClient() {
        return this.hostOfFocusedClient;
    }
}
