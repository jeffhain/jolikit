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
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

/**
 * Shows all events, with scaling.
 */
public class EventsScaledBwdTestCase extends EventsBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int SCALE = 4;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public EventsScaledBwdTestCase() {
    }

    public EventsScaledBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new EventsScaledBwdTestCase(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        final GPoint sup = super.getInitialClientSpans();
        return GPoint.valueOf(sup.x() / SCALE, sup.y() / SCALE);
    }

    /*
     * 
     */

    @Override
    public Integer getScaleElseNull() {
        return SCALE;
    }
}
