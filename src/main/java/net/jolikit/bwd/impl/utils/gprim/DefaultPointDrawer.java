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
package net.jolikit.bwd.impl.utils.gprim;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.LangUtils;

public class DefaultPointDrawer implements InterfacePointDrawer {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceClippedPointDrawer clippedPointDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultPointDrawer(InterfaceClippedPointDrawer clippedPointDrawer) {
        this.clippedPointDrawer = LangUtils.requireNonNull(clippedPointDrawer);
    }
    
    /*
     * Instance methods.
     */
    
    @Override
    public void drawPoint(GRect clip, int x, int y) {
        drawPoint(
                clip, x, y,
                this.clippedPointDrawer);
    }
    
    /*
     * Static methods.
     */
    
    public static void drawPoint(
            GRect clip, int x, int y,
            InterfaceClippedPointDrawer clippedPointDrawer) {
        if (clip.contains(x, y)) {
            clippedPointDrawer.drawPointInClip(x, y);
        }
    }
}
