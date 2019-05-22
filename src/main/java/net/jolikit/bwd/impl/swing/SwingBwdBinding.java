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
package net.jolikit.bwd.impl.swing;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.impl.awt.AbstractAwtBwdBinding;
import net.jolikit.bwd.impl.awt.AwtBwdBinding;

/**
 * Extends AWT binding.
 */
public class SwingBwdBinding extends AwtBwdBinding {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param bindingConfig Must not be null.
     */
    public SwingBwdBinding(SwingBwdBindingConfig bindingConfig) {
        super(bindingConfig);
    }
    
    /*
     * Hosts.
     */
    
    @Override
    public InterfaceBwdHost newHost(
            String title,
            boolean decorated,
            //
            InterfaceBwdClient client) {
        final AbstractAwtBwdBinding binding = this;
        final SwingBwdHost owner = null;
        final boolean modal = false;
        return new SwingBwdHost(
                binding,
                this.getHostLifecycleListener(),
                owner,
                //
                title,
                decorated,
                modal,
                //
                client);
    }
}
