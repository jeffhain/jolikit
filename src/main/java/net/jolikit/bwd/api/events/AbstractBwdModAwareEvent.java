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
package net.jolikit.bwd.api.events;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Base class for events that are aware of modifier keys.
 * 
 * Allows both to factor implementation code, and user code that deals with
 * modifiers flags (without the burden of introducing an intermediate
 * immutable BwdEventModifiers class that would hold these booleans).
 * 
 * Not providing an isAltGraphDown() method, because AltGraph key
 * is too messy to deal with, for example it's often confused
 * with Alt key by backing libraries.
 */
public abstract class AbstractBwdModAwareEvent extends BwdEvent {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Not using specific booleans for BWD meta keys:
     * since their int values are small, most likely the JVM
     * will use cached Integer instances when we check them.
     */
    private final SortedSet<Integer> modifierKeyDownSet;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractBwdModAwareEvent(
            Object source,
            BwdEventType eventType,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        this(
                null,
                //
                source,
                eventType,
                //
                newImmutableSortedSet(modifierKeyDownSet));
    }

    /*
     * 
     */

    /**
     * Should at least contain the information for
     * SHIFT, CONTROL, ALT, ALT_GRAPH and META keys.
     * 
     * @see BwdKeys
     * 
     * @return An immutable set of modifier keys that are down.
     */
    public SortedSet<Integer> getModifierKeyDownSet() {
        return this.modifierKeyDownSet;
    }

    /*
     * Convenience methods.
     * We don't suffix well-known keys with "key".
     */

    /**
     * If the information is not available, must return false.
     * 
     * @return True if SHIFT key is down, false otherwise.
     */
    public boolean isShiftDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.SHIFT);
    }

    /**
     * If the information is not available, must return false.
     * 
     * @return True if CONTROL key is down, false otherwise.
     */
    public boolean isControlDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.CONTROL);
    }

    /**
     * This convenience method conflates ALT and ALT_GRAPH keys,
     * because libraries often either conflate them, or don't indicate
     * when ALT_GRAPH is down, or call ALT "left alt" and ALT_GRAPH
     * "right alt", which easily makes code relying on "alt" and
     * "alt graph" discrimination not portable.
     * Also, this is consistent with other similar methods,
     * which conflate the left and the right modifier key.
     * 
     * If the information is not available, must return false.
     * 
     * @return True if ALT key is down or if ALT_GRAPH key is down,
     *         false otherwise.
     */
    public boolean isAltOrAltGraphDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.ALT)
                || this.modifierKeyDownSet.contains(BwdKeys.ALT_GRAPH);
    }

    /**
     * If the information is not available, must return false.
     * 
     * @return True if META key is down, false otherwise.
     */
    public boolean isMetaDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.META);
    }

    /*
     * 
     */

    /**
     * If the information is not available, must return false.
     * 
     * @see BwdKeys
     * 
     * @return key A key.
     * @return True if the specified key is a modifier key and is down,
     *         false otherwise.
     */
    public boolean isModifierKeyDown(int key) {
        return this.getModifierKeyDownSet().contains(key);
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * For usage by trusted code.
     * 
     * @param modifierKeyDownSet Instance to use internally. Must never be modified.
     */
    AbstractBwdModAwareEvent(
            Void nnul,
            //
            Object source,
            BwdEventType eventType,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        super(source, eventType);

        this.modifierKeyDownSet = modifierKeyDownSet;
    }

    /**
     * For toString() implementations.
     */
    void appendModifiers(StringBuilder sb) {
        for (int down : this.modifierKeyDownSet) {
            sb.append(", ");
            sb.append(BwdKeys.toString(down));
            sb.append(" down");
        }
    }
    
    /**
     * For use in public constructors,
     * to create internal sets from specified sets.
     */
    static <T> SortedSet<T> newImmutableSortedSet(SortedSet<T> set) {
        /*
         * New intermediate set even if the specified set
         * will never be modified, to avoid the memory leak risk
         * of having zillions of unmodifiable sets wrapped on top
         * of each other.
         * Would be lighter with immutable collections in the language
         * and as public arguments.
         */
        return Collections.unmodifiableSortedSet(new TreeSet<T>(set));
    }
}
