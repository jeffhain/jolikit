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
package net.jolikit.bwd.impl.utils.basics;

import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;

/**
 * To avoid dependency from AbstractEventConverter to AbstractBwdHost
 * (not having this dependency at the time we create this, but we could),
 * and a cyclic dependency between them.
 */
public interface InterfaceBwdHostInOs extends InterfaceBwdHost {
    
    /**
     * Not used by our bindings, but here for completeness.
     * 
     * @return Coordinates of the mouse pointer in screen coordinates,
     *         in OS pixels, or a best effort computation of it.
     */
    public GPoint getMousePosInScreenInOs();
    
    /**
     * If not showing or if iconified, must return GRect.DEFAULT_EMPTY.
     * 
     * @return {x=left, y=top, xSpan=right, ySpan=bottom} border span
     *         around client area, in OS pixels.
     */
    public GRect getInsetsInOs();
    
    /**
     * If not showing or if iconified, must return GRect.DEFAULT_EMPTY.
     * 
     * @return The bounds of the client area (where BWD is paint),
     *         in screen and in OS pixels.
     */
    public GRect getClientBoundsInOs();
    
    /**
     * If not showing or if iconified, must return GRect.DEFAULT_EMPTY.
     * 
     * @return The bounds of the window, in screen and in OS pixels.
     */
    public GRect getWindowBoundsInOs();
}
