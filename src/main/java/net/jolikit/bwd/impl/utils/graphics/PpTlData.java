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

/**
 * Package-private.
 */
class PpTlData {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * For arrays containing a whole area of pixels.
     */
    
    final IntArrHolder tmpBigArr1 = new IntArrHolder();
    final IntArrHolder tmpBigArr2 = new IntArrHolder();
    
    /*
     * For arrays containing a row or a column of pixels.
     */
    
    final IntArrHolder tmpArr1 = new IntArrHolder();
    final IntArrHolder tmpArr2 = new IntArrHolder();
    
    /*
     * 
     */
    
    final DoubleArrHolder tmpDoubleArr = new DoubleArrHolder();
    
    /*
     * 
     */
    
    final PpColorSum tmpColorSum = new PpColorSum();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PpTlData() {
    }
}