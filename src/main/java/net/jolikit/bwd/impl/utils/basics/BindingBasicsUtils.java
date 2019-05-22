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
package net.jolikit.bwd.impl.utils.basics;

import java.nio.ByteOrder;

import net.jolikit.lang.NumbersUtils;

/**
 * Basic utilities for implementing bindings.
 * 
 * See also:
 * - BindingColorUtils
 * - BindingCoordsUtils
 * - BindingSchedUtils
 * - BindingStringUtils
 * - BindingTextUtils
 */
public class BindingBasicsUtils {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    public static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    public static final boolean NATIVE_IS_BIG = (NATIVE_ORDER == ByteOrder.BIG_ENDIAN);
    public static final boolean NATIVE_IS_LITTLE = !NATIVE_IS_BIG;
    
    /*
     * 
     */

    /**
     * For offscreen images.
     * 
     * Some storages can't be created with zero spans,
     * but 1 should be OK, and fits out storage span values politics.
     */
    public static final int MIN_STORAGE_SPAN = 1;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * For computing spans of offscreen images,
     * as client area grows or shrinks.
     */
    public static int computeNewStorageSpan(int oldStorageSpan, int newSpan) {
        /*
         * Hysteresis: new width/height capacities when new values
         * are larger, or less than 4 times smaller.
         * 
         * Except for Integer.MAX_VALUE, capacity is computed as the
         * ceiling power of two of new span, so dividing by 2 for
         * the hysteresis would not be enough, since it could cause
         * storage span reset with only 1-pixel back-and-forth resizes
         * around a power of two limit, that's why we set our threshold
         * to old storage span divided by 4.
         * NB: This means that the pixel amount can be up to
         * 4*4 = 16 times bigger than actually needed, but still,
         * it will never be more than 2*2 = 4 times bigger than
         * has ever been needed.
         * 
         * Using "<", not "<=", to avoid pointless storage change when
         * old storage span is < divisor (so already small) and new span
         * is 0 (like: divisor = 4, old storage span = 2, new span = 0).
         */
        
        final boolean needNewStorageSpan =
                (newSpan > oldStorageSpan)
                || (newSpan < (oldStorageSpan >> 2));
        
        final int newStorageSpan;
        if (needNewStorageSpan) {
            newStorageSpan = computeStorageSpan(newSpan);
        } else {
            newStorageSpan = oldStorageSpan;
        }
        return newStorageSpan;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingBasicsUtils() {
    }
    
    private static int computeStorageSpan(int minSpan) {
        if (minSpan > (1<<30)) {
            // NB: Not a power of two.
            return Integer.MAX_VALUE;
        } else {
            return Math.max(MIN_STORAGE_SPAN, NumbersUtils.ceilingPowerOfTwo(minSpan));
        }
    }
}
