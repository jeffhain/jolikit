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

/**
 * Class to help implement (optional) InterfaceScaledRectAlgo.
 */
public abstract class AbstractScaledRectAlgo implements InterfaceScaledRectAlgo {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractScaledRectAlgo() {
    }
    
    /**
     * This default implementation returns Integer.MAX_VALUE,
     * i.e. ensures no splitting (no parallelism).
     */
    @Override
    public int getSrcAreaThresholdForSplit() {
        return Integer.MAX_VALUE;
    }

    /**
     * This default implementation returns Integer.MAX_VALUE,
     * i.e. ensures no splitting (no parallelism).
     */
    @Override
    public int getDstAreaThresholdForSplit() {
        return Integer.MAX_VALUE;
    }

    /**
     * This default implementation returns Double.POSITIVE_INFINITY,
     * i.e. ensures a single iteration for span growth.
     */
    @Override
    public double getIterationSpanGrowthFactor() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * This default implementation returns zero,
     * i.e. ensures a single iteration for span shrinking.
     */
    @Override
    public double getIterationSpanShrinkFactor() {
        return 0.0;
    }
}
