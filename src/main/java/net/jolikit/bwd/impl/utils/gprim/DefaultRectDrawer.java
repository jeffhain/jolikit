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

public class DefaultRectDrawer implements InterfaceRectDrawer {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceLineDrawer lineDrawer;
    
    private final InterfaceClippedRectDrawer clippedRectDrawer;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DefaultRectDrawer(
            InterfaceLineDrawer lineDrawer,
            InterfaceClippedRectDrawer clippedRectDrawer) {
        this.lineDrawer = LangUtils.requireNonNull(lineDrawer);
        this.clippedRectDrawer = LangUtils.requireNonNull(clippedRectDrawer);
    }
    
    /*
     * Instance methods.
     */
    
    @Override
    public void drawRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan) {
        drawRect(clip, x, y, xSpan, ySpan, this.lineDrawer);
    }

    @Override
    public void fillRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped) {
        fillRect(clip, x, y, xSpan, ySpan, areHorVerFlipped, this.clippedRectDrawer);
    }
    
    /*
     * Static methods.
     */
    
    /**
     * Takes care of span reduction in case of overflow.
     */
    public static void drawRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            InterfaceLineDrawer lineDrawer) {
        
        if (clip.isEmpty()) {
            return;
        }

        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }
        
        xSpan = GRect.trimmedSpan(x, xSpan);
        ySpan = GRect.trimmedSpan(y, ySpan);

        final int xMax = x + xSpan - 1;
        final int yMax = y + ySpan - 1;

        // Top.
        lineDrawer.drawLine(clip, x, y, xMax, y);
        if (yMax != y) {
            // Bottom.
            lineDrawer.drawLine(clip, x, yMax, xMax, yMax);
        }
        if (ySpan > 2) {
            // Left (without corners).
            lineDrawer.drawLine(clip, x, y + 1, x, yMax - 1);
            if (xMax != x) {
                // Right (without corners).
                lineDrawer.drawLine(clip, xMax, y + 1, xMax, yMax - 1);
            }
        }
    }

    /**
     * Takes care of span reduction in case of overflow.
     */
    public static void fillRect(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            boolean areHorVerFlipped,
            InterfaceClippedRectDrawer clippedRectDrawer) {
        
        if (clip.isEmpty()) {
            return;
        }

        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }
        
        xSpan = GRect.trimmedSpan(x, xSpan);
        ySpan = GRect.trimmedSpan(y, ySpan);

        final int xMax = x + xSpan - 1;
        final int yMax = y + ySpan - 1;
        
        if ((xMax < clip.x())
                || (yMax < clip.y())
                || (x > clip.xMax())
                || (y > clip.yMax())) {
            // Rect to fill doesn't intersect the clip.
            return;
        }
        
        /*
         * Ensuring that the rect to fill is in the clip
         * (to avoid line drawer having to clip each line,
         * and be able to use clipped drawer).
         */
        
        {
            int surplus = clip.x() - x;
            if (surplus > 0) {
                x = clip.x();
                xSpan -= surplus;
            }
        }
        {
            int surplus = clip.y() - y;
            if (surplus > 0) {
                y = clip.y();
                ySpan -= surplus;
            }
        }
        {
            int surplus = xMax - clip.xMax();
            if (surplus > 0) {
                xSpan -= surplus;
            }
        }
        {
            int surplus = yMax - clip.yMax();
            if (surplus > 0) {
                ySpan -= surplus;
            }
        }

        /*
         * 
         */
        
        clippedRectDrawer.fillRectInClip(
                x, y, xSpan, ySpan,
                areHorVerFlipped);
    }
}
