/*
 * Copyright 2019-2021 Jeff Hain
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

public class Alpha1LayerWinOpaqueBwdTestCase extends AbstractAlpha1LayerBwdTestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public Alpha1LayerWinOpaqueBwdTestCase() {
    }

    public Alpha1LayerWinOpaqueBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new Alpha1LayerWinOpaqueBwdTestCase(binding);
    }
}
