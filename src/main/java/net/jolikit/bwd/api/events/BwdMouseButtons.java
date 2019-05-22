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
 * Basic mouse buttons that should be supported by each binding.
 * 
 * Bindings can support additional buttons, as long as
 * they don't conflict with the values defined in this class.
 * 
 * Not using an enum class, to make it easier to deal transparently
 * with these default buttons and with additional ones.
 */
public class BwdMouseButtons {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MIN_BUTTON = 1;
    private static final int MAX_BUTTON = 3;
    /**
     * Keeping some room for eventual new buttons.
     * At least 20, in case people get to use all hands and feet fingers.
     */
    private static final int MIN_ADDITIONAL_BUTTON = 32;
    
    /*
     * 
     */

    /**
     * Not a button, or a button for which no specific value could be computed.
     */
    public static final int NO_STATEMENT = 0;
    
    /*
     * Actual buttons.
     */
    
    /**
     * First mouse button (1, as traditionally designated),
     * i.e. the left one.
     * 
     * This is the mouse button which must cause MOUSE_DRAGGED events
     * to be generated instead of MOUSE_MOVED events when it is down.
     * We fix that here, to avoid cluttering bindings and user code
     * with some drag button configuration that would in practice
     * always be PRIMARY, and to allow for more factoring of drag-related code.
     * 
     * Not defining (at least here) a DRAG_BUTTON alias for it,
     * nor related utility methods, to avoid confusion (one could think
     * that drag button is different than primary), and to keep
     * button related code homogeneous.
     */
    public static final int PRIMARY = MIN_BUTTON;
    
    /**
     * Middle mouse button (2, as traditionally designated).
     */
    public static final int MIDDLE = 2;
    
    /**
     * Secondary mouse button (3, as traditionally designated),
     * i.e. the right one.
     */
    public static final int SECONDARY = MAX_BUTTON;

    /*
     * 
     */

    private static final List<Integer> BUTTON_LIST;
    static {
        final List<Integer> list = new ArrayList<Integer>();
        // Our values are contiguous.
        for (int button = MIN_BUTTON; button <= MAX_BUTTON; button++) {
            list.add(button);
        }
        BUTTON_LIST = Collections.unmodifiableList(list);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Methods rather than constants, to avoid confusion with constants.
     */
    
    /**
     * @return An unmodifiable list of the buttons defined in this class
     *         (NO_STATEMENT excluded), in increasing order. 
     */
    public static List<Integer> buttonList() {
        return BUTTON_LIST;
    }
    
    /**
     * @return Min value for eventual additional buttons that can be generated
     *         by a binding.
     */
    public static int minAdditionalButton() {
        return MIN_ADDITIONAL_BUTTON;
    }
    
    /*
     * 
     */

    /**
     * @param button A button (can be any int value).
     * @return A string representation of the specified button.
     */
    public static String toString(int button) {
        switch (button) {
        case NO_STATEMENT: return "NO_STATEMENT";
        //
        case PRIMARY: return "PRIMARY";
        case MIDDLE: return "MIDDLE";
        case SECONDARY: return "SECONDARY";
        default:
            return "UNKNOWN_BUTTON(" + button + ")";
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdMouseButtons() {
    }
}
