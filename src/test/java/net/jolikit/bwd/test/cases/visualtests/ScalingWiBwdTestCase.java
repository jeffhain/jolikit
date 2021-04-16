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
package net.jolikit.bwd.test.cases.visualtests;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * Testing scaling with inner box drawing happening
 * on a writable image.
 */
public class ScalingWiBwdTestCase extends ScalingCliBwdTestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScalingWiBwdTestCase() {
    }
    
    public ScalingWiBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ScalingWiBwdTestCase(binding);
    }
    
    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ScalingWiBwdTestCase(this.getBinding());
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void paintInnerBox(
        InterfaceBwdGraphics g,
        GRect innerBox) {
        
        final GRect box = g.getBox();
        
        final InterfaceBwdWritableImage wi =
            this.getBinding().newWritableImage(
                box.xSpan(), box.ySpan());
        try {
            super.paintInnerBox(
                wi.getGraphics(),
                innerBox);
            g.drawImage(box.x(), box.y(), wi);
        } finally {
            wi.dispose();
        }
    }
}
