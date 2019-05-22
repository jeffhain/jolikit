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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class to retrieve object instances from corresponding int values.
 * 
 * Can be used for example for enums implementing InterfaceIntValued.
 */
public class IntValuedHelper<E extends IntValuedHelper.InterfaceIntValued> {
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    public interface InterfaceIntValued {
        public int intValue();
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Map<Integer,E> instanceByIntValue;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param instances Instances to be int-value-retrievable.
     * @throws NullPointerException if instances, or any contained instance, is null.
     * @throws IllegalArgumentException if multiple instances use a same int value.
     */
    public IntValuedHelper(E[] instances) {
        final Map<Integer,E> map = new HashMap<Integer,E>();
        // Implicit null check.
        for (E instance : instances) {
            // Implicit null check.
            final int intValue = instance.intValue();
            final Object prev = map.put(intValue, instance);
            if (prev != null) {
                throw new IllegalArgumentException("multiple instances for int value " + intValue);
            }
        }
        if (false) {
            // No need, since map order is not publicly visible.
            final TreeMap<Integer,E> sortedMap = new TreeMap<Integer,E>(map);
            // No need, since map is not publicly visible.
            this.instanceByIntValue = Collections.unmodifiableSortedMap(sortedMap);
        } else {
            this.instanceByIntValue = map;
        }
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public E instanceOf(int intValue) {
        return this.instanceByIntValue.get(intValue);
    }
}
