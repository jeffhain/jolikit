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

/**
 * Interface to draw lines, or stippled lines,
 * once clipping has been done.
 * 
 * Using same API whether the line is stippled or not,
 * both to factor code, and ensure stippled lines with
 * specific plain pattern are naturally drawn as plain lines.
 */
public interface InterfaceClippedLineDrawer {

    /**
     * Coordinates are in user frame of reference,
     * and must be in clip.
     * 
     * @param factor Must be in [1,256].
     * @param pattern To be set to -1 for non-stippled lines.
     * @param pixelNum Must be >= 0.
     */
    public int drawHorizontalLineInClip(
            int x1, int x2, int y,
            int factor, short pattern, int pixelNum);
    
    /**
     * Coordinates are in user frame of reference,
     * and must be in clip.
     * 
     * @param factor Must be in [1,256].
     * @param pattern To be set to -1 for non-stippled lines.
     * @param pixelNum Must be >= 0.
     */
    public int drawVerticalLineInClip(
            int x, int y1, int y2,
            int factor, short pattern, int pixelNum);
    
    /**
     * Coordinates are in user frame of reference,
     * and must be in clip.
     * 
     * @param factor Must be in [1,256].
     * @param pattern To be set to -1 for non-stippled lines.
     * @param pixelNum Must be >= 0.
     */
    public int drawGeneralLineInClip(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum);
    
    /**
     * Integer coordinates are initially specified coordinates,
     * which are not supposed to be in clip since this method
     * is being called.
     * They are provided for they might be useful, for example
     * for computing pixelNum to use for first pixel in clip.
     * 
     * Floating-point (clipped) coordinates are in
     * user frame of reference, and might be up to 0.5 (excluded)
     * out of clip, but still round into the clip.
     * 
     * pixelNum must be incremented according to whole segment
     * to draw, not only over the part in clip.
     * 
     * @param factor Must be in [1,256].
     * @param pattern To be set to -1 for non-stippled lines.
     * @param pixelNum Must be >= 0.
     */
    public int drawGeneralLineClipped(
            GRect clip,
            int x1, int y1, int x2, int y2,
            double x1d, double y1d, double x2d, double y2d,
            int factor, short pattern, int pixelNum);
}
