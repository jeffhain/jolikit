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
package net.jolikit.bwd.api.graphics;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class GRotationTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_valueList() {
        assertSame(GRotation.valueList(), GRotation.valueList());
        try {
            GRotation.valueList().add(null);
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        assertEquals(4, GRotation.valueList().size());
        assertEquals(GRotation.ROT_0, GRotation.valueList().get(0));
        assertEquals(GRotation.ROT_90, GRotation.valueList().get(1));
        assertEquals(GRotation.ROT_180, GRotation.valueList().get(2));
        assertEquals(GRotation.ROT_270, GRotation.valueList().get(3));
    }

    /*
     * 
     */
    
    public void test_valueOf_int() {
        assertEquals(GRotation.ROT_0, GRotation.valueOf(0));
        assertEquals(GRotation.ROT_90, GRotation.valueOf(90));
        assertEquals(GRotation.ROT_180, GRotation.valueOf(180));
        assertEquals(GRotation.ROT_270, GRotation.valueOf(270));
        
        for (int badAngDeg : new int[]{-91, -90, -1, 271, 360, 361}) {
            try {
                GRotation.valueOf(badAngDeg);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    public void test_prev_GRotation() {
        assertEquals(GRotation.ROT_270, GRotation.ROT_0.prev());
        assertEquals(GRotation.ROT_0, GRotation.ROT_90.prev());
        assertEquals(GRotation.ROT_90, GRotation.ROT_180.prev());
        assertEquals(GRotation.ROT_180, GRotation.ROT_270.prev());
    }

    public void test_next_GRotation() {
        assertEquals(GRotation.ROT_90, GRotation.ROT_0.next());
        assertEquals(GRotation.ROT_180, GRotation.ROT_90.next());
        assertEquals(GRotation.ROT_270, GRotation.ROT_180.next());
        assertEquals(GRotation.ROT_0, GRotation.ROT_270.next());
    }

    public void test_inverted() {
        assertEquals(GRotation.ROT_0, GRotation.ROT_0.inverted());
        assertEquals(GRotation.ROT_270, GRotation.ROT_90.inverted());
        assertEquals(GRotation.ROT_180, GRotation.ROT_180.inverted());
        assertEquals(GRotation.ROT_90, GRotation.ROT_270.inverted());
    }
    
    public void test_plusQuadrants_int() {
        final int qDelta = 4 * 3;
        
        final List<GRotation> rotList = new ArrayList<GRotation>();
        for (int i = 0; i < 2 * qDelta + 4; i++) {
            for (GRotation rotation : GRotation.values()) {
                rotList.add(rotation);
            }
        }
        
        final int n = rotList.size();
        // Starting from each of the 4 rotations.
        for (int i = 0; i < 4; i++) {
            // Start rotations around middle.
            final int ri = n/2 - 4/2 + i;
            final GRotation rotation = rotList.get(ri);
            for (int q = -qDelta; q <= qDelta; q++) {
                final GRotation rotated = rotation.plusQuadrants(q);
                final GRotation expected = rotList.get(ri + q);
                assertEquals(expected, rotated);
            }
        }
    }

    public void test_plus_GRotation() {
        for (GRotation rotation : GRotation.values()) {
            try {
                rotation.plus(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
            
            assertEquals(rotation, rotation.plus(GRotation.ROT_0));
            assertEquals(rotation.plusQuadrants(1), rotation.plus(GRotation.ROT_90));
            assertEquals(rotation.plusQuadrants(2), rotation.plus(GRotation.ROT_180));
            assertEquals(rotation.plusQuadrants(3), rotation.plus(GRotation.ROT_270));
        }
    }

    public void test_minus_GRotation() {
        for (GRotation rotation : GRotation.values()) {
            try {
                rotation.minus(null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
            
            assertEquals(rotation, rotation.minus(GRotation.ROT_0));
            assertEquals(rotation.plusQuadrants(-1), rotation.minus(GRotation.ROT_90));
            assertEquals(rotation.plusQuadrants(-2), rotation.minus(GRotation.ROT_180));
            assertEquals(rotation.plusQuadrants(-3), rotation.minus(GRotation.ROT_270));
        }
    }
    
    /*
     * 
     */
    
    public void test_angDeg() {
        assertEquals(0, GRotation.ROT_0.angDeg());
        assertEquals(90, GRotation.ROT_90.angDeg());
        assertEquals(180, GRotation.ROT_180.angDeg());
        assertEquals(270, GRotation.ROT_270.angDeg());
    }
    
    public void test_sin() {
        assertEquals(0, GRotation.ROT_0.sin());
        assertEquals(1, GRotation.ROT_90.sin());
        assertEquals(0, GRotation.ROT_180.sin());
        assertEquals(-1, GRotation.ROT_270.sin());
    }
    
    public void test_cos() {
        assertEquals(1, GRotation.ROT_0.cos());
        assertEquals(0, GRotation.ROT_90.cos());
        assertEquals(-1, GRotation.ROT_180.cos());
        assertEquals(0, GRotation.ROT_270.cos());
    }
    
    /*
     * 
     */
    
    public void test_dxIn1_2int() {
        assertEquals(5, GRotation.ROT_0.dxIn1(5, 7));
        assertEquals(-7, GRotation.ROT_90.dxIn1(5, 7));
        assertEquals(-5, GRotation.ROT_180.dxIn1(5, 7));
        assertEquals(7, GRotation.ROT_270.dxIn1(5, 7));
    }
    
    public void test_dyIn1_2int() {
        assertEquals(7, GRotation.ROT_0.dyIn1(5, 7));
        assertEquals(5, GRotation.ROT_90.dyIn1(5, 7));
        assertEquals(-7, GRotation.ROT_180.dyIn1(5, 7));
        assertEquals(-5, GRotation.ROT_270.dyIn1(5, 7));
    }
    
    public void test_dxIn2_2int() {
        assertEquals(5, GRotation.ROT_0.dxIn2(5, 7));
        assertEquals(7, GRotation.ROT_90.dxIn2(5, 7));
        assertEquals(-5, GRotation.ROT_180.dxIn2(5, 7));
        assertEquals(-7, GRotation.ROT_270.dxIn2(5, 7));
    }
    
    public void test_dyIn2_2int() {
        assertEquals(7, GRotation.ROT_0.dyIn2(5, 7));
        assertEquals(-5, GRotation.ROT_90.dyIn2(5, 7));
        assertEquals(-7, GRotation.ROT_180.dyIn2(5, 7));
        assertEquals(5, GRotation.ROT_270.dyIn2(5, 7));
    }
    
    /*
     * 
     */
    
    public void test_xSpanInOther_2int() {
        // Negative spans accepted (not checked).
        assertEquals(-7, GRotation.ROT_90.xSpanInOther(-5, -7));
        
        assertEquals(5, GRotation.ROT_0.xSpanInOther(5, 7));
        assertEquals(7, GRotation.ROT_90.xSpanInOther(5, 7));
        assertEquals(5, GRotation.ROT_180.xSpanInOther(5, 7));
        assertEquals(7, GRotation.ROT_270.xSpanInOther(5, 7));
    }
    
    public void test_ySpanInOther_2int() {
        // Negative spans accepted (not checked).
        assertEquals(-5, GRotation.ROT_90.ySpanInOther(-5, -7));
        
        assertEquals(7, GRotation.ROT_0.ySpanInOther(5, 7));
        assertEquals(5, GRotation.ROT_90.ySpanInOther(5, 7));
        assertEquals(7, GRotation.ROT_180.ySpanInOther(5, 7));
        assertEquals(5, GRotation.ROT_270.ySpanInOther(5, 7));
    }
}
