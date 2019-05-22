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
 */
public class BwdWheelEvent extends AbstractBwdModAwareEvent {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int xInScreen;
    private final int yInScreen;
    private final int xInClient;
    private final int yInClient;
    
    private final int xRoll;
    private final int yRoll;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param xRoll Signed roll quantity along x axis, in clicks if any, or about
     *        three to four standard deviations of user-plus-device accuracy.
     * @param yRoll Signed roll quantity along y axis, in clicks if any, or about
     *        three to four standard deviations of user-plus-device accuracy.
     */
    public BwdWheelEvent(
            Object source,
            //
            int xInScreen,
            int yInScreen,
            int xInClient,
            int yInClient,
            //
            int xRoll,
            int yRoll,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        this(
                null,
                //
                source,
                //
                xInScreen,
                yInScreen,
                xInClient,
                yInClient,
                //
                xRoll,
                yRoll,
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
        
        sb.append(", posInScreen = (");
        sb.append(this.xInScreen()).append(",").append(this.yInScreen());
        sb.append(")");
        sb.append(", posInClient = (");
        sb.append(this.xInClient()).append(",").append(this.yInClient());
        sb.append(")");

        /*
         * 
         */
        
        sb.append(", xRoll = ").append(this.xRoll());
        sb.append(", yRoll = ").append(this.yRoll());
        
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

    public int xInScreen() {
        return this.xInScreen;
    }

    public int yInScreen() {
        return this.yInScreen;
    }
    
    public int xInClient() {
        return this.xInClient;
    }

    public int yInClient() {
        return this.yInClient;
    }

    /*
     * 
     */
    
    public int xRoll() {
        return this.xRoll;
    }

    public int yRoll() {
        return this.yRoll;
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For usage by trusted code.
     * 
     * @param modifierKeyDownSet Instance to use internally. Must never be modified.
     */
    BwdWheelEvent(
            Void nnul,
            //
            Object source,
            //
            int xInScreen,
            int yInScreen,
            int xInClient,
            int yInClient,
            //
            int xRoll,
            int yRoll,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        super(
                nnul,
                //
                source,
                BwdEventType.WHEEL_ROLLED,
                //
                modifierKeyDownSet);
        
        this.xInScreen = xInScreen;
        this.yInScreen = yInScreen;
        this.xInClient = xInClient;
        this.yInClient = yInClient;
        
        this.xRoll = xRoll;
        this.yRoll = yRoll;
    }
}
