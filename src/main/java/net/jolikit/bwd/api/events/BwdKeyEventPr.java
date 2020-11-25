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
    
    /**
     * Whether this event is a repetition of a previous event.
     */
    private final boolean isRepeat;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param eventType The type of event, either KEY_PRESSED or KEY_RELEASED.
     * @param isRepeat Whether this event is a repetition of a previous event.
     * @throws IllegalArgumentException if isRepeat is true and eventType
     *         is not KEY_PRESSED.
     */
    public BwdKeyEventPr(
            Object source,
            BwdEventType eventType,
            //
            int key,
            int keyLocation,
            boolean isRepeat,
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
                isRepeat,
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
        
        if (this.getEventType() == BwdEventType.KEY_PRESSED) {
            sb.append(", is repeat = ").append(this.isRepeat());
        }
        
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
     * Only works for KEY_PRESSED events.
     * 
     * Note: the fact that we preserve modifiers should not hurt,
     * since on key press or release the repetition should stop.
     * 
     * @return An event, identical to this one modulo that its isRepeat
     *         boolean is the specified one.
     * @throws UnsupportedOperationException if the type of this event
     *         is not KEY_PRESSED. 
     */
    public BwdKeyEventPr withIsRepeat(boolean isRepeat) {
        if (this.getEventType() != BwdEventType.KEY_PRESSED) {
            throw new UnsupportedOperationException();
        }
        if (isRepeat == this.isRepeat) {
            return this;
        }
        return new BwdKeyEventPr(
                (Void) null,
                //
                this.getSource(),
                this.getEventType(),
                //
                this.key,
                this.keyLocation,
                isRepeat,
                //
                this.getModifierKeyDownSet());
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
    
    /**
     * Can be true only for KEY_PRESSED events.
     * 
     * @return Whether this event is a repetition of a previous event.
     */
    public boolean isRepeat() {
        return this.isRepeat;
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For usage by trusted code.
     * 
     * @param eventType Must be KEY_PRESSED or KEY_RELEASED (not checked).
     * @param modifierKeyDownSet Instance to use internally. Must never be modified.
     * @throws IllegalArgumentException if isRepeat is true and eventType
     *         is not KEY_PRESSED.
     */
    BwdKeyEventPr(
            Void nnul,
            //
            Object source,
            BwdEventType eventType,
            //
            int key,
            int keyLocation,
            boolean isRepeat,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        super(
                nnul,
                //
                source,
                eventType,
                //
                modifierKeyDownSet);
        if (isRepeat
            && (eventType != BwdEventType.KEY_PRESSED)) {
            throw new IllegalArgumentException("can't repeat " + eventType);
        }
        this.key = key;
        this.keyLocation = keyLocation;
        this.isRepeat = isRepeat;
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
