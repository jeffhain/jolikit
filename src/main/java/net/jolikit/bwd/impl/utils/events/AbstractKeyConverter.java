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
package net.jolikit.bwd.impl.utils.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * Base class for backing key to key converters, that allows to define
 * backingKey-to-key mapping(s) while iterating on keys,
 * which makes it easy to figure out the mapping(s) (if any) of each key,
 * compared for example to a switch-case on backing key values,
 * for which we would need to scan the whole code to see if a key
 * can be produced and in which cases.
 * 
 * @param BK Backing key type. These keys are used as keys in a map.
 */
public abstract class AbstractKeyConverter<BK> {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int MIN_ADDITIONAL_KEY = BwdKeys.minAdditionalKey();
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Map<BK,Integer> keyByBackingKey = new HashMap<BK,Integer>();
    
    /**
     * For checks.
     */
    private int maxMappedKey = Integer.MIN_VALUE;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractKeyConverter() {
    }

    /**
     * @param backingKey A backing key, possibly null.
     * @return The mapped key, or BwdKeys.NO_STATEMENT if there is none.
     */
    public int get(BK backingKey) {
        if (DEBUG) {
            Dbg.log("AbstractKeyConverter.get : backingKey = " + backingKey);
        }
        
        final Integer keyRef = this.keyByBackingKey.get(backingKey);
        if (keyRef == null) {
            return BwdKeys.NO_STATEMENT;
        }
        return keyRef.intValue();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /**
     * final for use in constructor.
     * 
     * Useful to make a key appear in the code,
     * so as to explicitly indicate that it was not forgotten
     * but that there is no mapping for it.
     * Also allows to make code more consistent across bindings,
     * for easier comparisons.
     * 
     * @param key A key for which there is no corresponding backing key.
     */
    protected final void noMatch(int key) {
    }

    /**
     * final for use in constructor.
     * 
     * Must be called even for backing key(s) corresponding to unknown key,
     * or to no key information (in which case only must use BwdKeys.NO_STATEMENT),
     * so that such backing keys don't get mapped to additional keys.
     * 
     * @param key Key to be mapped to the specified backing keys.
     * @param backingKeyArr One or multiple backing keys to which the specified
     *        key must be mapped.
     */
    //@SafeVarargs // Safe or not, we don't like obnoxious warnings.
    protected final void mapTo(int key, BK... backingKeyArr) {
        if (backingKeyArr.length == 0) {
            throw new IllegalArgumentException("no backing key");
        }
        for (BK backingKey : backingKeyArr) {
            LangUtils.requireNonNull(backingKey);
            
            final Integer prevKeyRef = this.keyByBackingKey.get(backingKey);
            if (prevKeyRef != null) {
                final String k1Str = BwdKeys.toString(prevKeyRef.intValue());
                final String k2Str = BwdKeys.toString(key);
                throw new IllegalArgumentException(
                        "keys " + k1Str + " and " + k2Str
                        + " both mapped to backing key " + backingKey);
            }
            this.keyByBackingKey.put(backingKey, key);
        }
        this.maxMappedKey = Math.max(this.maxMappedKey, key);
    }

    /**
     * Can be called after all calls to mapTo(...),
     * to map unmapped (= remaining) backing keys to additional key values.
     */
    protected final void mapUnmappedBackingKeys(Collection<BK> backingKeyColl) {
        
        // Making sure we start past BWD keys range.
        int nextKey = Math.max(this.maxMappedKey, MIN_ADDITIONAL_KEY);
        
        // Starting at a power of two, for used range to be easy to deal with.
        // NB: throws if > 2^30, but our bindings don't do that.
        nextKey = NbrsUtils.ceilingPowerOfTwo(nextKey);
        
        for (BK backingKey : backingKeyColl) {
            LangUtils.requireNonNull(backingKey);
            
            if (this.keyByBackingKey.containsKey(backingKey)) {
                // Already mapped: pass.
                continue;
            }
            
            final int key = nextKey++;
            
            this.keyByBackingKey.put(backingKey, key);
        }
    }
}
