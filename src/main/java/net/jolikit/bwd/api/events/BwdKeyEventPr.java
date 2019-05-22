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

import java.util.SortedSet;

/**
 * Class for KEY_PRESSED and KEY_RELEASED events.
 */
public class BwdKeyEventPr extends AbstractBwdModAwareEvent {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * A best effort computation of key.
     * @see #BwdKeys
     * 
     * Unless specified otherwise by binding implementations,
     * the key values must all be virtual, or all physical
     * if virtual keys are not available.
     */
    private final int key;
    
    /**
     * A best effort computation of key location.
     * @see #BwdKeyLocations
     */
    private final int keyLocation;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param eventType The type of event, either KEY_PRESSED or KEY_RELEASED.
     */
    public BwdKeyEventPr(
            Object source,
            BwdEventType eventType,
            //
            int key,
            int keyLocation,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        this(
                null,
                //
                source,
                checkedEventType(eventType),
                //
                key,
                keyLocation,
                //
                newImmutableSortedSet(modifierKeyDownSet));
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("[").append(this.getEventType());
        
        /*
         * 
         */
        
        sb.append(", key = ").append(BwdKeys.toString(this.key));
        
        sb.append(", key location = ").append(BwdKeyLocations.toString(this.keyLocation));
        
        /*
         * 
         */
        
        this.appendModifiers(sb);
        
        sb.append("]");
        
        return sb.toString();
    }

    /*
     * 
     */
    
    /**
     * @see #BwdKeys
     * 
     * Unless specified otherwise by binding implementation,
     * the key values must all be virtual, or all physical
     * if virtual keys are not available.
     * 
     * @return A best effort computation of key.
     */
    public int getKey() {
        return this.key;
    }

    /**
     * @see #BwdKeyLocations
     * 
     * @return A best effort computation of key location.
     */
    public int getKeyLocation() {
        return this.keyLocation;
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For usage by trusted code.
     * 
     * @param eventType Must be KEY_PRESSED or KEY_RELEASED (not checked).
     * @param modifierKeyDownSet Instance to use internally. Must never be modified.
     */
    BwdKeyEventPr(
            Void nnul,
            //
            Object source,
            BwdEventType eventType,
            //
            int key,
            int keyLocation,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        super(
                nnul,
                //
                source,
                eventType,
                //
                modifierKeyDownSet);
        
        this.key = key;
        this.keyLocation = keyLocation;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static BwdEventType checkedEventType(BwdEventType eventType) {
        // Implicit null check.
        if (!eventType.isKeyEventType()) {
            throw new IllegalArgumentException("" + eventType);
        }
        return eventType;
    }
}
