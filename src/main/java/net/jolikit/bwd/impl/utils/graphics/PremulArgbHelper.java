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

public class PremulArgbHelper implements InterfaceColorTypeHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final PremulArgbHelper INSTANCE = new PremulArgbHelper();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static PremulArgbHelper getInstance() {
        return INSTANCE;
    }
    
    @Override
    public boolean isPremul() {
        return true;
    }
    
    @Override
    public int asNonPremul32FromType(int color32) {
        return BindingColorUtils.toNonPremulAxyz32(color32);
    }
    
    @Override
    public int asPremul32FromType(int color32) {
        return color32;
    }
    
    @Override
    public int asTypeFromNonPremul32(int nonPremulColor32) {
        return BindingColorUtils.toPremulAxyz32(nonPremulColor32);
    }
    
    @Override
    public int asTypeFromPremul32(int premulColor32) {
        return premulColor32;
    }

    @Override
    public int toValidPremul32(int a8, int b8, int c8, int d8) {
        a8 = BindingColorUtils.saturate(a8, 0xFF);
        b8 = BindingColorUtils.saturate(b8, a8);
        c8 = BindingColorUtils.saturate(c8, a8);
        d8 = BindingColorUtils.saturate(d8, a8);
        return BindingColorUtils.toAbcd32_noCheck(a8, b8, c8, d8);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private PremulArgbHelper() {
    }
}
