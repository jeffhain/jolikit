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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * Draws using writable image graphics.
 */
public class DrawingMethodsWiBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DrawingMethodsBwdPainter painter;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DrawingMethodsWiBwdTestCase() {
        this.painter = null;
    }

    public DrawingMethodsWiBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        this.painter = new DrawingMethodsBwdPainter(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new DrawingMethodsWiBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new DrawingMethodsWiBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return DrawingMethodsBwdPainter.INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final InterfaceBwdBinding binding = this.getBinding();
        
        final InterfaceBwdWritableImage wi = binding.newWritableImage(
                getInitialClientSpans().x(),
                getInitialClientSpans().y());
        
        {
            final InterfaceBwdGraphics wig = wi.getGraphics();
            this.painter.paint(wig);
        }
        
        g.drawImage(0, 0, wi);
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
