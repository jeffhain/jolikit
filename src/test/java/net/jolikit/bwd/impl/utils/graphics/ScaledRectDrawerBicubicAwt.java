/*
 * Copyright 2024-2025 Jeff Hain
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
package net.jolikit.bwd.impl.utils.graphics;

import java.awt.RenderingHints;

/**
 * For tests against ScaledRectDrawerBicubic.
 */
public class ScaledRectDrawerBicubicAwt extends AbstractScaledRectDrawerAwt {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScaledRectDrawerBicubicAwt() {
        super(RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getSrcAreaThresholdForSplit() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    protected int getDstAreaThresholdForSplit() {
        return ScaledRectAlgoBicubic.DST_AREA_THRESHOLD_FOR_SPLIT;
    }
}
