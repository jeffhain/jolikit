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
 * Interface for AbstractBwdHost not to depend on AbstractBwdBinding,
 * and parameterized for this class not to depend on AbstractBwdHost,
 * each of which would cause a cyclic dependency.
 */
public interface InterfaceHostLifecycleListener<H extends InterfaceBwdHost> {

    /**
     * For host to register itself into the binding, when it gets created.
     * "this" publication should not hurt, as long as everything is done
     * in UI thread.
     */
    public void onHostCreated(H host);
    
    /**
     * Must be called in host's close() method implementation,
     * just before running event logic for event firing
     * (which must be done last, in particular because it calls
     * listeners and therefore might throw).
     */
    public void onHostClosing(H host);
    
    /**
     * Must be called when CLOSED event is about to be fired.
     */
    public void onHostClosedEventFiring(H host);
}
