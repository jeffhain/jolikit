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

public class ImageScalingOnCliScale2ClipWinAlphaBwdTestCase extends ImageScalingOnCliScale2BwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final double WINDOW_ALPHA_FP = 0.5;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ImageScalingOnCliScale2ClipWinAlphaBwdTestCase() {
    }

    public ImageScalingOnCliScale2ClipWinAlphaBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ImageScalingOnCliScale2ClipWinAlphaBwdTestCase(binding);
    }
    
    /*
     * 
     */
    
    @Override
    public boolean getHostDecorated() {
        // Some libraries don't allow for transparency if host is decorated.
        return false;
    }

    @Override
    public double getWindowAlphaFp() {
        return WINDOW_ALPHA_FP;
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected boolean mustClipScaling() {
        return true;
    }
}
