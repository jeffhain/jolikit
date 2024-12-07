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
package net.jolikit.bwd.impl.utils.graphics;

/**
 * Uses BILINEAR, except when shrinking by a factor
 * superior to two, in which case BOXSAMPLED is used first
 * to reduce BILINEAR shrinking to a factor of two.
 */
public class ScaledRectDrawerBoxsampledBilinear extends ScaledRectDrawerWithPreDs {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final ScaledRectDrawerBoxsampled DRAWER_BOXSAMPLED =
        new ScaledRectDrawerBoxsampled();
    
    private static final ScaledRectDrawerBilinear DRAWER_BILINEAR =
        new ScaledRectDrawerBilinear();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses 2 for maxBilinearShrinking.
     */
    public ScaledRectDrawerBoxsampledBilinear() {
        super(
            DRAWER_BOXSAMPLED,
            DRAWER_BILINEAR);
    }
    
    /**
     * @param maxBilinearShrinking Max span division to cover with BILINEAR,
     *        after preliminary shrinking using BOXSAMPLED. Must be >= 1.
     *        Use 1 to only shrink using BOXSAMPLED.
     */
    public ScaledRectDrawerBoxsampledBilinear(double maxBilinearShrinking) {
        super(
            DRAWER_BOXSAMPLED,
            DRAWER_BILINEAR,
            maxBilinearShrinking);
    }
}
