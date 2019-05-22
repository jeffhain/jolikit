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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic key locations buttons that should be supported by each binding.
 * @see #BwdKeyEventPr
 * 
 * Bindings can support additional key locations, as long as
 * they don't conflict with the values defined in this class.
 * 
 * Not using an enum class, to make it easier to deal transparently
 * with these default key locations and with additional ones.
 * 
 * It's fine to use NO_STATEMENT for key events for keys that can only be
 * at a specific location, such as in NUMPAD, for in these cases
 * the location info is useless, and it makes bindings easier to implement.
 */
public class BwdKeyLocations {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MIN_LOCATION = 1;
    private static final int MAX_LOCATION = 3;
    /**
     * Keeping some room for eventual new locations.
     * More than a few, in case we would want to use many locations
     * to emulate multiple virtual keyboards, for example for languages
     * with a log of characters.
     */
    private static final int MIN_ADDITIONAL_LOCATION = 32;
    
    /*
     * 
     */
    
    /**
     * Not a location, or a location for which no specific value could be computed.
     */
    public static final int NO_STATEMENT = 0;
    
    /*
     * Actual key locations.
     */

    /**
     * Left (of right).
     */
    public static final int LEFT = MIN_LOCATION;

    /**
     * Right (of left).
     */
    public static final int RIGHT = 2;
    
    /**
     * The numeric key pad.
     */
    public static final int NUMPAD = MAX_LOCATION;

    /*
     * 
     */

    private static final List<Integer> LOCATION_LIST;
    static {
        final List<Integer> list = new ArrayList<Integer>();
        // Our values are contiguous.
        for (int location = MIN_LOCATION; location <= MAX_LOCATION; location++) {
            list.add(location);
        }
        LOCATION_LIST = Collections.unmodifiableList(list);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Methods rather than constants, to avoid confusion with constants.
     */
    
    /**
     * @return An unmodifiable list of the locations defined in this class
     *         (NO_STATEMENT excluded), in increasing order. 
     */
    public static List<Integer> locationList() {
        return LOCATION_LIST;
    }
    
    /**
     * @return Min value for eventual additional locations that can be generated
     *         by a binding.
     */
    public static int minAdditionalLocation() {
        return MIN_ADDITIONAL_LOCATION;
    }

    /*
     * 
     */

    /**
     * @param location A location. Can be any int value.
     * @return A string representation of the specified location.
     */
    public static String toString(int location) {
        switch (location) {
        case NO_STATEMENT: return "NO_STATEMENT";
        //
        case LEFT: return "LEFT";
        case RIGHT: return "RIGHT";
        case NUMPAD: return "NUMPAD";
        default:
            return "UNKNOWN_LOCATION(" + location + ")";
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdKeyLocations() {
    }
}
