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
package net.jolikit.bwd.test.utils;

import java.util.List;

import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;

public abstract class AbstractBwdTestCaseHome extends BwdClientMock implements InterfaceBwdTestCaseHome {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractBwdTestCaseHome() {
    }
    
    /*
     * 
     */
    
    /**
     * This default implementation returns null.
     * 
     * Note that tests use a sequential parallelizer if the binding
     * doesn't support parallelism for their test cases.
     */
    @Override
    public Integer getParallelizerParallelismElseNull() {
        return null;
    }
    
    /*
     * 
     */

    /**
     * This default implementation returns null.
     */
    @Override
    public Integer getScaleElseNull() {
        return null;
    }
    
    /**
     * This default implementation returns null.
     */
    @Override
    public Double getClientPaintDelaySElseNull() {
        return null;
    }

    /**
     * This default implementation returns null.
     */
    @Override
    public Boolean getMustUseFontBoxForFontKindElseNull() {
        return null;
    }
    
    /**
     * This default implementation returns null.
     */
    @Override
    public Boolean getMustUseFontBoxForCanDisplayElseNull() {
        return null;
    }
    
    @Override
    public List<String> getBonusSystemFontFilePathList() {
        return BwdTestUtils.BONUS_SYSTEM_FONT_FILE_PATH_LIST;
    }
    
    @Override
    public InterfaceDefaultFontInfoComputer getDefaultFontInfoComputer() {
        return BwdTestUtils.DEFAULT_FONT_INFO_COMPUTER;
    }
    
    /**
     * This default implementation returns null.
     */
    @Override
    public List<String> getUserFontFilePathListElseNull() {
        return null;
    }
}
