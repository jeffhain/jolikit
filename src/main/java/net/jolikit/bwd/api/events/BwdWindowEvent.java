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

/**
 * Class for window events.
 * 
 * Temporary focus:
 * We don't have a "temporary" boolean in focus events,
 * for at the BWD level they are always about window focus,
 * which is temporary by nature.
 * 
 * Could just use BwdEvent class for these, but it's clearer to give them
 * their own class.
 */
public class BwdWindowEvent extends BwdEvent {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BwdWindowEvent(
            Object source,
            BwdEventType eventType) {
        super(
                source,
                checkedEventType(eventType));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("[").append(this.getEventType());
        sb.append("]");
        
        return sb.toString();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static BwdEventType checkedEventType(BwdEventType eventType) {
        // Implicit null check.
        if (!eventType.isWindowEventType()) {
            throw new IllegalArgumentException("" + eventType);
        }
        return eventType;
    }
}
