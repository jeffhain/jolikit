/*
 * Copyright 2019-2020 Jeff Hain
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

import net.jolikit.lang.NbrsUtils;

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
     * Some storages can't be created with zero spans,
     * but 1 should be OK.
     */
    public static final int MIN_STORAGE_SPAN = 1;
    
    /**
     * Small enough not to waste more than 1.5 of actually used memory
     * when growing.
     * (sqrt() because surfaces are in 2D).
     */
    private static final double SPAN_GROWTH_FACTOR = Math.sqrt(1.5);
    private static final double SPAN_SHRINK_FACTOR = 1.0/SPAN_GROWTH_FACTOR;

    /**
     * Small enough not to waste more than (1.5^2) of actually used memory
     * when shrinking.
     * Not using SPAN_SHRINK_FACTOR, for hysteresis, to avoid having
     * storage span jump between K and K * FACTOR when used span
     * oscillates around K.
     */
    private static final double SPAN_SHRINK_THRESHOLD = NbrsUtils.pow2(SPAN_SHRINK_FACTOR);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param oldStorageSpan Old storage span. Must be >= 0.
     *        (even though MIN_STORAGE_SPAN is 1, 0 is accepted here)
     * @param newSpanToStore New span to store. Must be >= 0.
     * @param allowShrinking If true, computed span can be smaller
     *        than old span. Allows for example not to keep wasting memory
     *        after some client area shrank a lot.
     * @return Storage span to use, considering old storage span
     *         and new span to store. Always >= MIN_STORAGE_SPAN.
     */
    public static int computeStorageSpan(
            int oldStorageSpan,
            int newSpanToStore,
            boolean allowShrinking) {
        
        if (oldStorageSpan < 0) {
            throw new IllegalArgumentException("" + oldStorageSpan);
        }
        
        if (newSpanToStore < 0) {
            throw new IllegalArgumentException("" + newSpanToStore);
        }
        
        // Simplifies our code, and makes sure we don't return 0.
        oldStorageSpan = Math.max(MIN_STORAGE_SPAN, oldStorageSpan);
        
        final int ret;
        if (newSpanToStore < oldStorageSpan) {
            if (allowShrinking) {
                /*
                 * NB: Since we work on one coordinate at a time (x or y),
                 * we might uselessly re-create storage when a span shrinks
                 * but the other grows, but if one spans shrinks a lot
                 * the overall surface should not be too large so it shouldn't
                 * hurt much, and these cases should not be too common.
                 */
                
                final boolean mustShrink =
                        (newSpanToStore < oldStorageSpan * SPAN_SHRINK_THRESHOLD);
                if (mustShrink) {
                    // Might not actually shrink for small old spans,
                    // but it doesn't hurt.
                    ret = (int) Math.max(MIN_STORAGE_SPAN, oldStorageSpan * SPAN_SHRINK_FACTOR);
                } else {
                    ret = oldStorageSpan;
                }
            } else {
                ret = oldStorageSpan;
            }
            
        } else if (newSpanToStore > oldStorageSpan) {
            final boolean mustGrow =
                    (newSpanToStore > oldStorageSpan);
            if (mustGrow) {
                // Always grows, even in case of small old span
                // and small growth factor.
                ret = (int) Math.max(newSpanToStore, oldStorageSpan * SPAN_GROWTH_FACTOR);
            } else {
                ret = oldStorageSpan;
            }
        } else {
            ret = oldStorageSpan;
        }
        
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingBasicsUtils() {
    }
}
