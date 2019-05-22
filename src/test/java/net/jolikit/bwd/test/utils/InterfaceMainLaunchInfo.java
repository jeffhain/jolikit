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

import java.util.List;

/**
 * Provides info about how to launch a main (args excepted):
 * - what is its class name,
 * - what specific classpath to add to a common one defined aside,
 * - and what specific JVM options to define.
 */
public interface InterfaceMainLaunchInfo {
    
    /**
     * @return The name of the class containing the main to launch.
     */
    public String getMainClassName();
    
    /**
     * @return Classpath needed for the main to launch,
     *         in addition to some common classpath,
     *         or an empty list if none.
     */
    public List<String> getSpecificClasspathList();
    
    /**
     * @return JVM args (such as "-Dfoo=bar", or "-Xboo") specific to the main
     *         to launch, in addition to some common JVM args,
     *         using space separator, or an empty string if none.
     */
    public String getSpecificJvmArgs();
}
