/*
 * Copyright 2019 Jeff Hain
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

import java.util.ArrayList;
import java.util.List;

/**
 * A default implementation, that just indicates
 * "this" class name as main class name.
 */
public class DefaultMainLaunchInfo implements InterfaceMainLaunchInfo {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultMainLaunchInfo() {
    }
    
    @Override
    public String getMainClassName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getSpecificClasspathList() {
        return new ArrayList<String>();
    }

    @Override
    public String getSpecificJvmArgs() {
        return "";
    }
}
