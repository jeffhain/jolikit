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

import net.jolikit.lang.LangUtils;

public class DefaultClippedRectDrawer implements InterfaceClippedRectDrawer {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceClippedLineDrawer clippedLineDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultClippedRectDrawer(InterfaceClippedLineDrawer clippedLineDrawer) {
        this.clippedLineDrawer = LangUtils.requireNonNull(clippedLineDrawer);
    }
    
    /*
     * Instance methods.
     */
    
    @Override
    public void fillRectInClip(
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped) {
        fillRectInClip(
                x, y, xSpan, ySpan,
                areHorVerFlipped,
                this.clippedLineDrawer);
    }
    
    /*
     * Static methods.
     */

    public static void fillRectInClip(
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped,
            InterfaceClippedLineDrawer clippedLineDrawer) {
        if (areHorVerFlipped) {
            if (ySpan <= 0) {
                return;
            }
            final int yMax = y + ySpan - 1;
            for (int i = 0; i < xSpan; i++) {
                clippedLineDrawer.drawVerticalLineInClip(
                        x + i, y, yMax,
                        1, GprimUtils.PLAIN_PATTERN, 0);
            }
        } else {
            if (xSpan <= 0) {
                return;
            }
            final int xMax = x + xSpan - 1;
            for (int i = 0; i < ySpan; i++) {
                clippedLineDrawer.drawHorizontalLineInClip(
                        x, xMax, y + i,
                        1, GprimUtils.PLAIN_PATTERN, 0);
            }
        }
    }
}
