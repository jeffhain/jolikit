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
package net.jolikit.bwd.impl.utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;

/**
 * Helper to make calling client's paintClient() method simpler.
 */
public class PaintClientHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBwdClient client;
    
    /**
     * Could also not use atomic, assuming that painting calls are always done
     * in UI thread, but it shouldn't hurt to use it.
     */
    private final AtomicBoolean clientPaintBeingCalled = new AtomicBoolean(false);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PaintClientHelper(InterfaceBwdClient client) {
        this.client = client;
    }

    /**
     * Takes care of:
     * - Calling graphics init()/finish() around call to paintClient().
     * - Clipping input dirty rectangle (mandatory)
     *   and output rectangles (handy) to graphics box.
     * - Ensuring that a client's painting method is never called
     *   recursively, assuming that all paintings for a same client
     *   are done using a same instance of this class.
     * 
     * Also takes care of clipping input and output rectangles to graphics box.
     * 
     * @throws IllegalStateException if client's painting method
     *         is already being called by this instance.
     */
    public List<GRect> initPaintFinish(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        if (this.clientPaintBeingCalled.compareAndSet(false, true)) {
            try {
                // Input clipping (mandatory, easier to deal with for clients).
                dirtyRect = g.getBox().intersected(dirtyRect);
                
                List<GRect> paintedRectList = null;
                g.init();
                try {
                    paintedRectList = this.client.paintClient(g, dirtyRect);
                } finally {
                    g.finish();
                }
                
                // Output clipping (easier to deal with for bindings).
                paintedRectList = BindingCoordsUtils.clippedRectList(g.getBox(), paintedRectList);
                
                return paintedRectList;
            } finally {
                this.clientPaintBeingCalled.set(false);
            }
        } else {
            throw new IllegalStateException("client's painting method already being called");
        }
    }
}
