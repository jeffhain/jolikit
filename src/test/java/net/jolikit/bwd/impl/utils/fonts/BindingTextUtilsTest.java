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
package net.jolikit.bwd.impl.utils.fonts;

import junit.framework.TestCase;

public class BindingTextUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_withoutNul_String() {
        try {
            BindingTextUtils.withoutNul(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        assertEquals("", BindingTextUtils.withoutNul(""));
        assertEquals("a", BindingTextUtils.withoutNul("a"));
        assertEquals("abc", BindingTextUtils.withoutNul("abc"));
        
        assertEquals("", BindingTextUtils.withoutNul("\u0000"));
        assertEquals("a", BindingTextUtils.withoutNul("a\u0000"));
        assertEquals("a", BindingTextUtils.withoutNul("\u0000a"));
        assertEquals("aabbcc", BindingTextUtils.withoutNul("aa\u0000bb\u0000cc"));
    }
}
