/*
 * Copyright 2019-2024 Jeff Hain
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
package net.jolikit.bwd.impl.utils.basics;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.lang.NbrsUtils;

public class BindingCoordsUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static int ceilToInt(double value) {
        return NbrsUtils.ceilToInt(value);
    }
    
    public static int floorToInt(double value) {
        return NbrsUtils.floorToInt(value);
    }
    
    /**
     * @return The closest int to the argument, with ties rounding to positive infinity.
     */
    public static int roundToInt(double value) {
        return NbrsUtils.roundToInt(value);
    }
    
    /**
     * @return The closest long to the argument, with ties rounding to positive infinity.
     */
    public static long roundToLong(double value) {
        return NbrsUtils.round(value);
    }
    
    /*
     * 
     */
    
    /**
     * @param rootBoxTopLeft Base coordinates of box top-left pixel.
     * @param transformBaseToUser Transform from base coordinates to user coordinates.
     * @return Transform from box coordinates to user coordinates.
     */
    public static GTransform computeTransformBoxToUser(
        GPoint rootBoxTopLeft,
        GTransform transformBaseToUser) {
        final GTransform ret;
        if (rootBoxTopLeft.equalsPoint(0, 0)) {
            // Passing here when client (pixels) scale is 1.
            ret = transformBaseToUser;
        } else {
            // Passing here when client (pixels) scale is >= 2.
            ret = GTransform.valueOf(
                transformBaseToUser.rotation(),
                transformBaseToUser.frame2XIn1() - rootBoxTopLeft.x(),
                transformBaseToUser.frame2YIn1() - rootBoxTopLeft.y());
        }
        return ret;
    }
    
    /*
     * 
     */
    
    public static GRect computeInsets(GRect clientBounds, GRect windowBounds) {
        if (clientBounds.equals(windowBounds)) {
            return GRect.DEFAULT_EMPTY;
        }
        // Using max(0,_) not to throw in case of bad inputs.
        return GRect.valueOf(
                Math.max(0, clientBounds.x() - windowBounds.x()),
                Math.max(0, clientBounds.y() - windowBounds.y()),
                Math.max(0, windowBounds.xMax() - clientBounds.xMax()),
                Math.max(0, windowBounds.yMax() - clientBounds.yMax()));
    }

    public static GRect computeClientBounds(GRect insets, GRect windowBounds) {
        if (insets.equals(GRect.DEFAULT_EMPTY)) {
            return windowBounds;
        }
        // Using max(0,_) not to throw in case of bad inputs.
        return GRect.valueOf(
                windowBounds.x() + insets.x(),
                windowBounds.y() + insets.y(),
                Math.max(0, windowBounds.xSpan() - (insets.x() + insets.xSpan())),
                Math.max(0, windowBounds.ySpan() - (insets.y() + insets.ySpan())));
    }
    
    public static GRect computeWindowBounds(GRect insets, GRect clientBounds) {
        if (insets.equals(GRect.DEFAULT_EMPTY)) {
            return clientBounds;
        }
        // Using max(0,_) not to throw in case of bad inputs.
        return GRect.valueOf(
                clientBounds.x() - insets.x(),
                clientBounds.y() - insets.y(),
                Math.max(0, clientBounds.xSpan() + (insets.x() + insets.xSpan())),
                Math.max(0, clientBounds.ySpan() + (insets.y() + insets.ySpan())));
    }
    
    /*
     * 
     */
    
    public static List<GRect> asList(GRect rect) {
        final ArrayList<GRect> result = new ArrayList<GRect>();
        result.add(rect);
        return result;
    }
    
    public static ArrayList<GRect> clippedRectList(
            GRect rect,
            List<GRect> rectList) {
        final ArrayList<GRect> result = new ArrayList<GRect>(rectList);
        clipRectList(rect, result);
        return result;
    }
    
    /**
     * Replaces each rect of the list with the intersection
     * of itself and the specified clip, when needed,
     * and removes it if the result is empty.
     * 
     * Preserves order (in case it would matter).
     * 
     * @param clip A clip.
     * @param rectList (in,out) An ArrayList of rectangles.
     */
    public static void clipRectList(
            GRect clip,
            ArrayList<GRect> rectList) {
        
        /*
         * Clipping rectangles and (re-)putting into the list
         * the clipped ones that are not empty.
         */
        
        final int initialSize = rectList.size();
        int i = 0;
        for (int j = 0; j < initialSize; j++) {
            final GRect rect = rectList.get(j);
            final GRect clippedRect = rect.intersected(clip);
            if (!clippedRect.isEmpty()) {
                rectList.set(i, clippedRect);
                i++;
            }
        }
        
        /*
         * Removing unused last slots.
         */
        
        final int newSize = i;
        while (rectList.size() > newSize) {
            final int lastIndex = rectList.size() - 1;
            rectList.remove(lastIndex);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingCoordsUtils() {
    }
}
