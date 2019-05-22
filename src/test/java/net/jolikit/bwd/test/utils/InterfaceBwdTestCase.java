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

/**
 * Interface for BWD tests making use of a client mock.
 * 
 * Ending the name of this class with "TestCase", not just "Test",
 * to avoid issue with libraries supposing that all classes classes
 * ending with "Test" must contain JUnit tests or else.
 */
public interface InterfaceBwdTestCase {
    
    /*
     * Stuffs for configuring the binding.
     */
    
    public boolean getMustImplementBestEffortPixelReading();

    /*
     * 
     */
    
    public InterfaceBwdTestCaseClient newClient();

    public boolean getHostDecorated();
    
    public double getWindowAlphaFp();
}
