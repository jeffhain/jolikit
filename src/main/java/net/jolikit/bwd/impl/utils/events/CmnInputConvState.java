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
package net.jolikit.bwd.impl.utils.events;

import java.util.SortedSet;
import java.util.TreeSet;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;

/**
 * State common to events converters of all hosts, for a same binding instance.
 */
public class CmnInputConvState {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private GPoint mousePosInScreenInOs = GPoint.ZERO;

    private final SortedSet<Integer> buttonDownSet = new TreeSet<Integer>();
    
    private final SortedSet<Integer> modifierKeyDownSet = new TreeSet<Integer>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public CmnInputConvState() {
    }
    
    public GPoint getMousePosInScreenInOs() {
        return this.mousePosInScreenInOs;
    }
    
    public void setMousePosInScreenInOs(GPoint mousePosInScreenInOs) {
        this.mousePosInScreenInOs = mousePosInScreenInOs;
    }
    
    /*
     * 
     */
    
    public SortedSet<Integer> getButtonDownSet() {
        return this.buttonDownSet;
    }

    /*
     * 
     */
    
    public boolean getPrimaryButtonDown() {
        return this.buttonDownSet.contains(BwdMouseButtons.PRIMARY);
    }
    
    public void setPrimaryButtonDown(boolean primaryButtonDown) {
        if (primaryButtonDown) {
            this.buttonDownSet.add(BwdMouseButtons.PRIMARY);
        } else {
            this.buttonDownSet.remove(BwdMouseButtons.PRIMARY);
        }
    }

    public boolean getMiddleButtonDown() {
        return this.buttonDownSet.contains(BwdMouseButtons.MIDDLE);
    }

    public void setMiddleButtonDown(boolean middleButtonDown) {
        if (middleButtonDown) {
            this.buttonDownSet.add(BwdMouseButtons.MIDDLE);
        } else {
            this.buttonDownSet.remove(BwdMouseButtons.MIDDLE);
        }
    }

    public boolean getSecondaryButtonDown() {
        return this.buttonDownSet.contains(BwdMouseButtons.SECONDARY);
    }
    
    public void setSecondaryButtonDown(boolean secondaryButtonDown) {
        if (secondaryButtonDown) {
            this.buttonDownSet.add(BwdMouseButtons.SECONDARY);
        } else {
            this.buttonDownSet.remove(BwdMouseButtons.SECONDARY);
        }
    }
    
    /*
     * 
     */
    
    public SortedSet<Integer> getModifierKeyDownSet() {
        return this.modifierKeyDownSet;
    }

    /*
     * 
     */
    
    public boolean getShiftDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.SHIFT);
    }
    
    public void setShiftDown(boolean shiftDown) {
        if (shiftDown) {
            this.modifierKeyDownSet.add(BwdKeys.SHIFT);
        } else {
            this.modifierKeyDownSet.remove(BwdKeys.SHIFT);
        }
    }
    
    public boolean getControlDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.CONTROL);
    }

    public void setControlDown(boolean controlDown) {
        if (controlDown) {
            this.modifierKeyDownSet.add(BwdKeys.CONTROL);
        } else {
            this.modifierKeyDownSet.remove(BwdKeys.CONTROL);
        }
    }

    public boolean getAltDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.ALT);
    }

    public void setAltDown(boolean altDown) {
        if (altDown) {
            this.modifierKeyDownSet.add(BwdKeys.ALT);
        } else {
            this.modifierKeyDownSet.remove(BwdKeys.ALT);
        }
    }

    public boolean getAltGraphDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.ALT_GRAPH);
    }

    public void setAltGraphDown(boolean altGraphDown) {
        if (altGraphDown) {
            this.modifierKeyDownSet.add(BwdKeys.ALT_GRAPH);
        } else {
            this.modifierKeyDownSet.remove(BwdKeys.ALT_GRAPH);
        }
    }

    public boolean getMetaDown() {
        return this.modifierKeyDownSet.contains(BwdKeys.META);
    }
    
    public void setMetaDown(boolean metaDown) {
        if (metaDown) {
            this.modifierKeyDownSet.add(BwdKeys.META);
        } else {
            this.modifierKeyDownSet.remove(BwdKeys.META);
        }
    }
}
