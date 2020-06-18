/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.impl.utils.gprim;

import net.jolikit.bwd.api.graphics.GRect;

public interface InterfacePolygonDrawer {

    /**
     * Clip and coordinates are in user frame of reference.
     * 
     * @param pointCount Must be >= 0.
     */
    public void drawPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount);

    /**
     * Clip and coordinates are in user frame of reference.
     * 
     * @param pointCount Must be >= 0.
     */
    public void fillPolygon(
            GRect clip,
            int[] xArr,
            int[] yArr,
            int pointCount,
            boolean areHorVerFlipped);
}
