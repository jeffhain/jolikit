/*
 * Copyright 2024 Jeff Hain
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
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

public class ImageScalingOnWiScale2ClipBwdTestCase extends ImageScalingOnCliScale2BwdTestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ImageScalingOnWiScale2ClipBwdTestCase() {
    }

    public ImageScalingOnWiScale2ClipBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ImageScalingOnWiScale2ClipBwdTestCase(binding);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected boolean mustClipScaling() {
        return true;
    }
    
    @Override
    protected boolean mustDrawScaledOnWritableImage() {
        return true;
    }
}
