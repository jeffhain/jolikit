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

import net.jolikit.lang.LangUtils;

/**
 * Base class for BWD events, from windows or input devices.
 * 
 * Must be effectively immutable, except for the source which might itself
 * mutate (but the event reference to it must never change).
 * 
 * Does not extend EventObject, even though it's just part of java.util,
 * not to pull the serialization mess.
 */
public class BwdEvent {
    
    /*
     * Not overriding equals(Object) and hashCode() for these events,
     * as this would not make much sense (two events could then be "equals",
     * in the sense that they would have same values, but yet would not
     * correspond to a same actual event, for example if the same user action
     * takes place at different times).
     */

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * The source that generated this event.
     */
    private final Object source;
    
    private final BwdEventType eventType;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param source The source that generated this event. Must not be null.
     * @param eventType Must not be null.
     */
    public BwdEvent(
            Object source,
            BwdEventType eventType) {
        this.source = LangUtils.requireNonNull(source);
        this.eventType = LangUtils.requireNonNull(eventType);
    }
    
    /*
     * 
     */
    
    /**
     * @return The source that generated this event. Never null.
     */
    public Object getSource() {
        return this.source;
    }

    /**
     * @return The event type of this event.
     */
    public BwdEventType getEventType() {
        return this.eventType;
    }
}
