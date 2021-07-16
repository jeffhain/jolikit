/*
 * Copyright 2019-2021 Jeff Hain
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

/**
 * Class for MOUSE_XXX events.
 * 
 * Contains (x,y) in client coordinates, and also in screen coordinates, for
 * convenience in case wanting to know them even after conversion between
 * client and screen coordinates changed (typically due to window move).
 * 
 * Does not contain a time (would require the definition of a clock),
 * nor a click count (to be computed at higher level, independently
 * of eventual system-tuned delays, to be more flexible), nor other
 * non-essential fancy features.
 * 
 * Can correspond to buttons other than defined in BwdMouseButton,
 * except for the isXxxButtonDown() convenience methods which
 * are only defined for buttons defined in BwdMouseButton.  
 */
public class BwdMouseEvent extends AbstractBwdPosAwareEvent {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int button;

    private final SortedSet<Integer> buttonDownSet;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param clientBounds Client bounds at the time the event was constructed.
     *        Must not be empty.
     * @param button @see #BwdMouseButtons
     */
    public BwdMouseEvent(
        Object source,
        BwdEventType eventType,
        GPoint posInScreen,
        GRect clientBounds,
        int button,
        SortedSet<Integer> buttonDownSet,
        SortedSet<Integer> modifierKeyDownSet) {
        this(
            null,
            //
            source,
            checkedEventType(eventType),
            posInScreen,
            clientBounds,
            button,
            newImmutableSortedSet(buttonDownSet),
            newImmutableSortedSet(modifierKeyDownSet));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("[").append(this.getEventType());
        
        this.appendPositions(sb);
        
        sb.append(", button = ").append(BwdMouseButtons.toString(this.button));
        
        for (int down : this.buttonDownSet) {
            sb.append(", ");
            sb.append(BwdMouseButtons.toString(down));
            sb.append(" down");
        }
        
        this.appendModifiers(sb);

        sb.append("]");

        return sb.toString();
    }

    /*
     * 
     */

    /**
     * Useful to create a MOUSE_CLICKED from a MOUSE_RELEASED event.
     * 
     * @return A new event, identical to this one except that its event type is
     *         MOUSE_CLICKED.
     * @throws IllegalStateException if this event's type is not MOUSE_RELEASED.
     */
    public BwdMouseEvent asMouseClickedEvent() {
        if (this.getEventType() != BwdEventType.MOUSE_RELEASED) {
            throw new IllegalStateException();
        }
        return new BwdMouseEvent(
            null,
            //
            this.getSource(),
            BwdEventType.MOUSE_CLICKED,
            this.posInScreen(),
            this.clientBounds(),
            this.button,
            this.getButtonDownSet(),
            this.getModifierKeyDownSet());
    }

    /**
     * Useful to create a MOUSE_DRAGGED from a MOUSE_MOVED event.
     * 
     * @return A new event, identical to this one except that its event type is
     *         MOUSE_DRAGGED.
     * @throws IllegalStateException if this event's type is not MOUSE_MOVED.
     */
    public BwdMouseEvent asMouseDraggedEvent() {
        if (this.getEventType() != BwdEventType.MOUSE_MOVED) {
            throw new IllegalStateException();
        }
        return new BwdMouseEvent(
            null,
            //
            this.getSource(),
            BwdEventType.MOUSE_DRAGGED,
            this.posInScreen(),
            this.clientBounds(),
            this.button,
            this.getButtonDownSet(),
            this.getModifierKeyDownSet());
    }

    /**
     * Useful in case of scaling, when backing client is larger
     * than scaled client.
     * 
     * @return An event, identical to this one except that its event type is
     *         MOUSE_ENTERED_CLIENT, or this one if it has this type already.
     */
    public BwdMouseEvent asMouseEnteredClientEvent() {
        if (this.getEventType() == BwdEventType.MOUSE_ENTERED_CLIENT) {
            return this;
        }
        return new BwdMouseEvent(
            null,
            //
            this.getSource(),
            BwdEventType.MOUSE_ENTERED_CLIENT,
            this.posInScreen(),
            this.clientBounds(),
            this.button,
            this.getButtonDownSet(),
            this.getModifierKeyDownSet());
    }

    /**
     * Useful in case of scaling, when backing client is larger
     * than scaled client.
     * 
     * @return An event, identical to this one except that its event type is
     *         MOUSE_EXITED_CLIENT, or this one if it has this type already.
     */
    public BwdMouseEvent asMouseExitedClientEvent() {
        if (this.getEventType() == BwdEventType.MOUSE_EXITED_CLIENT) {
            return this;
        }
        return new BwdMouseEvent(
            null,
            //
            this.getSource(),
            BwdEventType.MOUSE_EXITED_CLIENT,
            this.posInScreen(),
            this.clientBounds(),
            this.button,
            this.getButtonDownSet(),
            this.getModifierKeyDownSet());
    }

    /*
     * 
     */

    /**
     * @see #BwdMouseButtons
     * 
     * @return The button which state change caused this event,
     *         or BwdMouseButtons.NO_STATEMENT if none.
     */
    public int getButton() {
        return this.button;
    }

    /*
     * 
     */

    /**
     * Should at least contain the information for
     * PRIMARY, MIDDLE and SECONDARY buttons.
     * 
     * @see BwdMouseButtons
     * 
     * @return An immutable set of buttons that are down.
     */
    public SortedSet<Integer> getButtonDownSet() {
        return this.buttonDownSet;
    }

    /*
     * Convenience methods.
     * We don't suffix well-known buttons with "button".
     */

    /**
     * If the information is not available, must return false.
     * 
     * @return True if PRIMARY button is down, false otherwise.
     */
    public boolean isPrimaryDown() {
        return this.buttonDownSet.contains(BwdMouseButtons.PRIMARY);
    }

    /**
     * If the information is not available, must return false.
     * 
     * @return True if MIDDLE button is down, false otherwise.
     */
    public boolean isMiddleDown() {
        return this.buttonDownSet.contains(BwdMouseButtons.MIDDLE);
    }

    /**
     * If the information is not available, must return false.
     * 
     * @return True if SECONDARY button is down, false otherwise.
     */
    public boolean isSecondaryDown() {
        return this.buttonDownSet.contains(BwdMouseButtons.SECONDARY);
    }

    /*
     * 
     */

    /**
     * If the information is not available, must return false.
     * 
     * @see BwdMouseButtons
     * 
     * @return button A button.
     * @return True if the specified button is down, false otherwise.
     */
    public boolean isButtonDown(int button) {
        return this.getButtonDownSet().contains(button);
    }

    /**
     * Drag button is primary button, but this method allows for user
     * not to have to know it, and to make drag related code more explicit.
     * 
     * @return True if this event was caused by a button state change
     *         and if it was drag button.
     */
    public boolean hasDragButton() {
        return (this.getButton() == BwdMouseButtons.PRIMARY);
    }

    /**
     * Drag button is primary button, but this method allows for user
     * not to have to know it, and to make drag related code more explicit.
     * 
     * If the information is not available, must return false.
     * 
     * @return True if drag button is down in this event.
     */
    public boolean isDragButtonDown() {
        return this.isPrimaryDown();
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * For usage by trusted code.
     * 
     * @param eventType Must be a mouse event type (not checked).
     * @param clientBounds Client bounds at the time the event was constructed.
     *        Must not be empty.
     * @param buttonDownSet Instance to use internally. Must never be modified.
     * @param modifierKeyDownSet Instance to use internally. Must never be modified.
     */
    BwdMouseEvent(
        Void nnul,
        //
        Object source,
        BwdEventType eventType,
        GPoint posInScreen,
        GRect clientBounds,
        int button,
        SortedSet<Integer> buttonDownSet,
        SortedSet<Integer> modifierKeyDownSet) {
        super(
            nnul,
            //
            source,
            eventType,
            posInScreen,
            clientBounds,
            //
            modifierKeyDownSet);
        
        this.button = button;

        this.buttonDownSet = buttonDownSet;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static BwdEventType checkedEventType(BwdEventType eventType) {
        // Implicit null check.
        if (!eventType.isMouseEventType()) {
            throw new IllegalArgumentException("" + eventType);
        }
        return eventType;
    }
}
