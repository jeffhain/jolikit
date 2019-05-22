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

public interface InterfaceBwdTestCaseHomeProvider {
    
    public InterfaceBwdTestCaseHome getDefaultHome();
    
    /**
     * @param homeClassName Class name of a home.
     * @return The home of specified class name, or null if none.
     */
    public InterfaceBwdTestCaseHome getHome(String homeClassName);
}
