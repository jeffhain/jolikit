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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test that when using client spans larger than screen spans,
 * whether the specified bounds or spans are honored or reduced to fit,
 * things are consistent, for example without X or Y flattening
 * of displayed text.
 */
public class HostCoordsHugeBwdTestCase extends HostCoordsRegularBwdTestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private boolean hugeSpansEnsured = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HostCoordsHugeBwdTestCase() {
    }

    public HostCoordsHugeBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostCoordsHugeBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new HostCoordsHugeBwdTestCase(this.getBinding());
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        if (!this.hugeSpansEnsured) {
            this.hugeSpansEnsured = true;
            getBinding().getUiThreadScheduler().execute(new Runnable() {
                @Override
                public void run() {
                    final GRect screenBounds = getBinding().getScreenBounds();
                    final GRect targetWindowBounds = screenBounds.withSpansDeltas(
                            screenBounds.xSpan() / 2,
                            screenBounds.ySpan() / 2);
                    getHost().setWindowBounds(targetWindowBounds);
                }
            });
        }
        
        return super.paintClientImpl(g, dirtyRect);
    }
}
