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
 * Class for WHEEL_ROLLED events.
 * 
 * The unit of roll is a click if any, else three to four standard deviations
 * of user-plus-device accuracy.
 * Roll quantities are mathematical integers, which allows to easily implement
 * integer increments (1 or more per event) for pixel positions or model indexes,
 * without having to buffer eventually small quantities up to reaching some
 * meaningful threshold (to avoid undesired high-speed scrolls or such),
 * which must be done by the binding if needed.
 * Roll quantity is usually 1, but could be higher if buffering and merging
 * these events for some reason.
 * 
 * xRoll() and yRoll() must not be both zero.
 */
public class BwdWheelEvent extends AbstractBwdPosAwareEvent {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final GPoint roll;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param clientBounds Client bounds at the time the event was constructed.
     *        Must not be empty.
     * @param roll Signed roll quantity along x and y axis, in clicks if any,
     *        or about three to four standard deviations of
     *        user-plus-device accuracy.
     */
    public BwdWheelEvent(
        Object source,
        GPoint posInScreen,
        GRect clientBounds,
        GPoint roll,
        SortedSet<Integer> modifierKeyDownSet) {
        this(
            null,
            //
            source,
            posInScreen,
            clientBounds,
            roll,
            newImmutableSortedSet(modifierKeyDownSet));
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("[").append(this.getEventType());
        
        this.appendPositions(sb);
        
        sb.append(", roll = ").append(this.roll);
        
        this.appendModifiers(sb);

        sb.append("]");
        
        return sb.toString();
    }
    
    /*
     * 
     */
    
    /**
     * @return Signed roll quantity along x and y axis, in clicks if any,
     *         or about three to four standard deviations of
     *         user-plus-device accuracy.
     */
    public GPoint roll() {
        return this.roll();
    }
    
    /**
     * @return Signed roll quantity along x axis, in clicks if any, or about
     *         three to four standard deviations of user-plus-device accuracy.
     */
    public int xRoll() {
        return this.roll.x();
    }

    /**
     * @return Signed roll quantity along y axis, in clicks if any, or about
     *         three to four standard deviations of user-plus-device accuracy.
     */
    public int yRoll() {
        return this.roll.y();
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
    BwdWheelEvent(
        Void nnul,
        //
        Object source,
        GPoint posInScreen,
        GRect clientBounds,
        GPoint roll,
        SortedSet<Integer> modifierKeyDownSet) {
        super(
            nnul,
            //
            source,
            BwdEventType.WHEEL_ROLLED,
            posInScreen,
            clientBounds,
            //
            modifierKeyDownSet);
        
        this.roll = roll;
    }
}
