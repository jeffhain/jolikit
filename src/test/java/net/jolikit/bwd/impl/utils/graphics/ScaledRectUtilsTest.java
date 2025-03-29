/*
 * Copyright 2019-2025 Jeff Hain
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

import java.util.Random;

import junit.framework.TestCase;
import net.jolikit.test.utils.TestUtils;

public class ScaledRectUtilsTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_isNearestExact_4int() {
        final Random random = TestUtils.newRandom123456789L();
        for (int i = 0; i < 1000; i++) {
            final int sxSpan = (1 + random.nextInt(5));
            final int sySpan = (1 + random.nextInt(5));
            final int dxSpan = (1 + random.nextInt(5)) * sxSpan;
            final int dySpan = (1 + random.nextInt(5)) * sySpan;
            assertTrue(ScaledRectUtils.isNearestExact(
                sxSpan, sySpan,
                dxSpan, dySpan));
        }
        
        assertTrue(ScaledRectUtils.isNearestExact(1, 0, 1, 0));
        assertTrue(ScaledRectUtils.isNearestExact(1, 1, 1, 0));
        //
        assertTrue(ScaledRectUtils.isNearestExact(0, 1, 0, 1));
        assertTrue(ScaledRectUtils.isNearestExact(1, 1, 0, 1));
        
        assertFalse(ScaledRectUtils.isNearestExact(1, 0, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(1, 2, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(1, 3, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(1, 4, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(1, 4, 1, 2));
        assertFalse(ScaledRectUtils.isNearestExact(1, 4, 1, 3));
        //
        assertFalse(ScaledRectUtils.isNearestExact(0, 1, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(2, 1, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(3, 1, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(4, 1, 1, 1));
        assertFalse(ScaledRectUtils.isNearestExact(4, 1, 2, 1));
        assertFalse(ScaledRectUtils.isNearestExact(4, 1, 3, 1));
    }
}
