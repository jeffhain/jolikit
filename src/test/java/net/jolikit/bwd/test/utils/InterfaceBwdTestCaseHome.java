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
package net.jolikit.bwd.test.utils;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;

/**
 * Interface for objects allowing to know where and how to launch a test case,
 * and to create new instances of the test case bound to a specific binding.
 */
public interface InterfaceBwdTestCaseHome {

    /*
     * Stuffs for configuring the binding, before its creation.
     */
    
    /**
     * @return Parallelizer's parallelism to use,
     *         or null to use binding's config default.
     */
    public Integer getParallelizerParallelismElseNull();
    
    /*
     * Stuffs for configuring the binding, after its creation.
     */
    
    /**
     * @return A value, or null if wanting to rely on binding's default.
     */
    public Double getClientPaintDelaySElseNull();
    
    /**
     * @return True, false, or null if wanting to rely on binding's default.
     */
    public Boolean getMustUseFontBoxForFontKindElseNull();
    
    /**
     * @return True, false, or null if wanting to rely on binding's default.
     */
    public Boolean getMustUseFontBoxForCanDisplayElseNull();

    /**
     * Set into binding config even if null.
     * 
     * @return Can be null.
     */
    public List<String> getBonusSystemFontFilePathList();
    
    /**
     * @return Must not be null.
     */
    public InterfaceDefaultFontInfoComputer getDefaultFontInfoComputer();
    
    /**
     * @return The list for fonts loading method.
     */
    public List<String> getUserFontFilePathListElseNull();
    
    /*
     * 
     */
    
    /**
     * Useful to know where to locate hosts when launching multiple ones
     * for visual comparison tests (some tests need to be visual, because
     * there is no API to read pixels, and the desired behavior is typically
     * not pixel-identical across bindings, for example depending on fonts
     * rendering).
     * 
     * Supposed to be identical for all instances of a same class,
     * so that can create dummy instances just to query it for instances
     * created later for use with different bindings (TODO could move it
     * to some mock factory API then...).
     * 
     * @return The initial spans (width and height) of the client area.
     */
    public GPoint getInitialClientSpans();

    /**
     * Sequencing is useful for benches, so that they don't share
     * resources while executing.
     * 
     * @return True if tests must be executed one after the other
     *         (which supposes that they terminate or are terminated
     *         at some point in time), not all at once.
     */
    public boolean getMustSequenceLaunches();
    
    /*
     * 
     */
    
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding);
}
