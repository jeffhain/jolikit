/*
 * Copyright 2020-2021 Jeff Hain
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

/**
 * Text clipping using writable image graphics.
 */
public class TextClippingWiBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final TextClippingBwdPainter painter;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public TextClippingWiBwdTestCase() {
        this.painter = null;
    }

    public TextClippingWiBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        this.painter = new TextClippingBwdPainter(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new TextClippingWiBwdTestCase(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return TextClippingBwdPainter.INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final InterfaceBwdBinding binding = this.getBinding();
        
        final InterfaceBwdWritableImage wi = binding.newWritableImage(
                getInitialClientSpans().x(),
                getInitialClientSpans().y());
        try {
            this.painter.paint(wi.getGraphics());
            g.drawImage(0, 0, wi);
        } finally {
            wi.dispose();
        }
        
        return null;
    }
}
