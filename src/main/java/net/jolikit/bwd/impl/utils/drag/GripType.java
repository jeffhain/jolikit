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
package net.jolikit.bwd.impl.utils.drag;

import java.util.Arrays;
import java.util.List;

public enum GripType {
    CENTER(0, 0),
    TOP(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    BOTTOM(0, 1),
    TOP_LEFT(-1, -1),
    TOP_RIGHT(1, -1),
    BOTTOM_LEFT(-1, 1),
    BOTTOM_RIGHT(1, 1);
    
    /**
     * -1 if left, 0 if center, 1 if right.
     */
    final int xSide;
    
    /**
     * -1 if top, 0 if center, 1 if bottom.
     */
    final int ySide;
    
    /*
     * 
     */
    
    private static final List<GripType> LIST = Arrays.asList(GripType.values());
    
    /**
     * @return An unmodifiable list of enum values, with ordinal as index.
     */
    public static List<GripType> valueList() {
        return LIST;
    }
    
    /*
     * 
     */
    
    private GripType(
            int xSide,
            int ySide) {
        this.xSide = xSide;
        this.ySide = ySide;
    }
    
    /*
     * 
     */
    
    /**
     * @return -1 if left, 0 if center, 1 if right.
     */
    public int xSide() {
        return this.xSide;
    }
    
    /**
     * @return -1 if top, 0 if center, 1 if bottom.
     */
    public int ySide() {
        return this.ySide;
    }
}

