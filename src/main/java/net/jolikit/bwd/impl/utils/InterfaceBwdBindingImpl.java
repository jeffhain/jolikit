/*
 * Copyright 2021 Jeff Hain
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

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Interface for our bindings, with specific methods
 * used by our bindings implementations.
 * 
 * To avoid dependency from AbstractBwdHost to AbstractBwdBinding,
 * and a cyclic dependency between them.
 */
public interface InterfaceBwdBindingImpl extends InterfaceBwdBinding {
    
    /**
     * @return Bounds within which drawing is supposed to occur,
     *         in screen coordinates, in OS pixels.
     */
    public GRect getScreenBoundsInOs();
    
    /**
     * @return Coordinates of the mouse pointer in screen coordinates,
     *         in OS pixels, or a best effort computation of it.
     */
    public GPoint getMousePosInScreenInOs();
    
    public BaseBwdBindingConfig getBindingConfig();
    
    /**
     * @return Parallelizer usable in bindings internals,
     *         in places where it doesn't cause side effects in user code.
     */
    public InterfaceParallelizer getInternalParallelizer();
}
