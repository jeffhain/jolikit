/*
 * Copyright 2021 Jeff Hain
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

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;

public abstract class AbstractBwdPosAwareEvent extends AbstractBwdModAwareEvent {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final GPoint posInScreen;

    private final GPoint posInClient;

    /**
     * Client bounds at the time the event was constructed.
     * Never empty (if client bounds are empty,
     * these events must not be generated).
     */
    private final GRect clientBounds;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param clientBounds Client bounds at the time the event was constructed.
     *        Must not be empty.
     */
    public AbstractBwdPosAwareEvent(
            Object source,
            BwdEventType eventType,
            GPoint posInScreen,
            GRect clientBounds,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        this(
            null,
            //
            source,
            eventType,
            posInScreen,
            clientBounds,
            //
            newImmutableSortedSet(modifierKeyDownSet));
    }

    /*
     * 
     */

    /**
     * @return Position in screen.
     */
    public GPoint posInScreen() {
        return this.posInScreen;
    }

    /**
     * @return x in screen.
     */
    public int xInScreen() {
        return this.posInScreen.x();
    }

    /**
     * @return y in screen.
     */
    public int yInScreen() {
        return this.posInScreen.y();
    }

    /*
     * 
     */

    /**
     * @return Position in client frame of reference
     *         (might actually be outside of client bounds).
     */
    public GPoint posInClient() {
        return this.posInClient;
    }

    /**
     * @return x in client.
     */
    public int xInClient() {
        return this.posInClient.x();
    }

    /**
     * @return y in client.
     */
    public int yInClient() {
        return this.posInClient.y();
    }
    
    /*
     * 
     */
    
    /**
     * @return Client bounds at the time the event was constructed.
     *         Never empty.
     */
    public GRect clientBounds() {
        return this.clientBounds;
    }
    
    /**
     * @return Whether this event's mouse position is in its client bounds.
     */
    public boolean isPosInClient() {
        return this.clientBounds.contains(this.posInScreen);
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * For usage by trusted code.
     * 
     * @param clientBounds Client bounds at the time the event was constructed.
     *        Must not be empty.
     * @param modifierKeyDownSet Instance to use internally. Must never be modified.
     */
    AbstractBwdPosAwareEvent(
            Void nnul,
            //
            Object source,
            BwdEventType eventType,
            GPoint posInScreen,
            GRect clientBounds,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        super(
            nnul,
            source,
            eventType,
            modifierKeyDownSet);
        if (clientBounds.isEmpty()) {
            throw new IllegalArgumentException("empty client bounds");
        }
        this.posInScreen = posInScreen;
        this.clientBounds = clientBounds;
        this.posInClient = clientBounds.toThisRelative(posInScreen);
    }

    /**
     * For toString() implementations.
     */
    void appendPositions(StringBuilder sb) {
        sb.append(", posInClient = ").append(this.posInClient);
        sb.append(", posInScreen = ").append(this.posInScreen);
        sb.append(", clientBounds = ").append(this.clientBounds);
    }
}
