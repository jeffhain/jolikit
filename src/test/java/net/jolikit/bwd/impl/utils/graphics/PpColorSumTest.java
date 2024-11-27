/*
 * Copyright 2024 Jeff Hain
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

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;

public class PpColorSumTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PpColorSumTest() {
    }
    
    public void test_toXxxPremulColor32_nonPremul_fullWeight() {
        final PpColorSum sum = new PpColorSum();
        final InterfaceColorTypeHelper colorTypeHelper =
            NonPremulArgbHelper.getInstance();
        sum.configure(colorTypeHelper);
        
        sum.addFullPixelContrib(0xFF224466);
        sum.addFullPixelContrib(0xFF88AACC);
        
        assertEquals(
            Argb32.toString(0xFF557799),
            Argb32.toString(sum.toPremulColor32(1 / 2.0)));
        assertEquals(
            Argb32.toString(0xFF557799),
            Argb32.toString(sum.toValidPremulColor32(
                colorTypeHelper,
                1 / 2.0)));
    }
    
    public void test_toXxxPremulColor32_nonPremul_halfWeight() {
        final PpColorSum sum = new PpColorSum();
        final InterfaceColorTypeHelper colorTypeHelper =
            NonPremulArgbHelper.getInstance();
        sum.configure(colorTypeHelper);
        
        sum.addPixelContrib(0xFF224466, 0.5);
        sum.addPixelContrib(0xFF88AACC, 0.5);
        
        assertEquals(
            Argb32.toString(0xFF557799),
            Argb32.toString(sum.toPremulColor32(1 / 1.0)));
        assertEquals(
            Argb32.toString(0xFF557799),
            Argb32.toString(sum.toValidPremulColor32(
                colorTypeHelper,
                1 / 1.0)));
    }
    
    public void test_toXxxPremulColor32_premul_fullWeight() {
        final PpColorSum sum = new PpColorSum();
        final InterfaceColorTypeHelper colorTypeHelper =
            PremulArgbHelper.getInstance();
        sum.configure(colorTypeHelper);
        
        sum.addFullPixelContrib(0x66224466);
        sum.addFullPixelContrib(0xCC88AACC);
        
        assertEquals(
            Argb32.toString(0x99557799),
            Argb32.toString(sum.toPremulColor32(1 / 2.0)));
        assertEquals(
            Argb32.toString(0x99557799),
            Argb32.toString(sum.toValidPremulColor32(
                colorTypeHelper,
                1 / 2.0)));
    }
    
    public void test_toXxxPremulColor32_premul_halfWeight() {
        final PpColorSum sum = new PpColorSum();
        final InterfaceColorTypeHelper colorTypeHelper =
            PremulArgbHelper.getInstance();
        sum.configure(colorTypeHelper);
        
        sum.addPixelContrib(0x66224466, 0.5);
        sum.addPixelContrib(0xCC88AACC, 0.5);
        
        assertEquals(
            Argb32.toString(0x99557799),
            Argb32.toString(sum.toPremulColor32(1 / 1.0)));
        assertEquals(
            Argb32.toString(0x99557799),
            Argb32.toString(sum.toValidPremulColor32(
                colorTypeHelper,
                1 / 1.0)));
    }
    
    public void test_toXxxPremulColor32_premul_underflow() {
        final PpColorSum sum = new PpColorSum();
        final InterfaceColorTypeHelper colorTypeHelper =
            PremulArgbHelper.getInstance();
        sum.configure(colorTypeHelper);
        
        sum.addPixelContrib(0x81828384, -1.0);
        
        // Cpts are (+1 due to adding 0.5 to negative value
        // and then taking int part):
        // -0x81 = -129, +1: -128 = 0xFFFFFF80
        // -0x82 = -130, +1: -129 = 0xFFFFFF7F
        // -0x83 = -131, +1: -130 = 0xFFFFFF7E
        // -0x84 = -132, +1: -131 = 0xFFFFFF7D
        assertEquals(
            Argb32.toString(0xFFFFFF7D),
            Argb32.toString(sum.toPremulColor32(1 / 1.0)));
        assertEquals(
            Argb32.toString(0x00000000),
            Argb32.toString(sum.toValidPremulColor32(
                colorTypeHelper,
                1 / 1.0)));
    }
    
    public void test_toXxxPremulColor32_premul_overflow() {
        final PpColorSum sum = new PpColorSum();
        final InterfaceColorTypeHelper colorTypeHelper =
            PremulArgbHelper.getInstance();
        sum.configure(colorTypeHelper);
        
        sum.addPixelContrib(0x81828384, 2.0);
        
        // Lowest digit of each cpt multiplied by 2,
        // plus eventual carry from next cpt due to
        // 0x80 multiplied by 2.
        // Result is an invalid alpha-premultiplied value:
        // must not use toPremulColor32() when having
        // overflows.
        assertEquals(
            Argb32.toString(0x03050708),
            Argb32.toString(sum.toPremulColor32(1 / 1.0)));
        assertEquals(
            Argb32.toString(0xFFFFFFFF),
            Argb32.toString(sum.toValidPremulColor32(
                colorTypeHelper,
                1 / 1.0)));
    }
    
    public void test_toXxxPremulColor32_premul_overflowingAlpha() {
        final PpColorSum sum = new PpColorSum();
        final InterfaceColorTypeHelper colorTypeHelper =
            PremulArgbHelper.getInstance();
        sum.configure(colorTypeHelper);
        
        sum.addPixelContrib(0x20000000, -0.5);
        sum.addPixelContrib(0x40403020, 1.0);
        
        // Result is an invalid alpha-premultiplied value:
        // must not use toPremulColor32() when having
        // negative weights.
        assertEquals(
            Argb32.toString(0x30403020),
            Argb32.toString(sum.toPremulColor32(1 / 1.0)));
        // Alpha = 0x40 - 0x20/2 = 0x30.
        // Non-alpha components are kept below alpha,
        // due to alpha-premultiplied format.
        assertEquals(
            Argb32.toString(0x30303020),
            Argb32.toString(sum.toValidPremulColor32(
                colorTypeHelper,
                1 / 1.0)));
    }
}
