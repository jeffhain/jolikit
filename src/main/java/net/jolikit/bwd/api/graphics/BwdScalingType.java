/*
 * Copyright 2024 Jeff Hain
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
package net.jolikit.bwd.api.graphics;

import java.util.Arrays;
import java.util.List;

/**
 * Types of interpolation for image scaling,
 * when drawing an image with a graphics.
 * 
 * Design: Not using int values to allow for additional
 * future values or user-defined values, as we do for
 * mouse buttons, because for performances reasons
 * we don't want to add more predefined values,
 * and user can always implement its own scaling
 * using images and/or graphics.
 */
public enum BwdScalingType {
    /**
     * Fastest, but can quickly get ugly when downscaling
     * as more and more source pixels get not used. 
     */
    NEAREST,
    /**
     * Slower than NEAREST, but all source pixels covered
     * by each destination pixel contribute proportionally to the overlap
     * (i.e. must use box sampling), so is much better when downscaling.
     * 
     * If there is no scaling, or integer-multiple upscaling,
     * is equivalent to NEAREST, so should delegate to NEAREST
     * in these cases for speed.
     */
    BILINEAR,
    /**
     * Also called cardinal cubic spline.
     * 
     * The main purpose of BICUBIC for us is, on upscaling,
     * its property to preserve curves better than BILINEAR,
     * for better and more agreable zoomed text readability.
     * Its downside compared to BILINEAR is the blurriness
     * it causes to zommed-in (upscaled) pixel art,
     * or when zooming in order to see pixels more clearly
     * (they actually get more blurry), and its relative slowness. 
     * 
     * There can be many variations of bicubic;
     * this one must take care, on downscaling, of using
     * the information of (at least) all the source pixels
     * covering the destination pixel, typically by downscaling
     * iteratively (faster and more in the spirit of bicubic
     * than using preliminary box sampling, which could be done
     * separately using a preliminary BILINEAR downscaling),
     * and must also prefer speed to low value/cost subtleties.
     * 
     * If there is no scaling, is equivalent to NEAREST,
     * so should delegate to NEAREST in that case for speed.
     */
    BICUBIC;
    
    /*
     * 
     */
    
    private static final List<BwdScalingType> LIST =
        Arrays.asList(BwdScalingType.values());
    
    /**
     * @return An unmodifiable list of enum values, with ordinal as index.
     */
    public static List<BwdScalingType> valueList() {
        return LIST;
    }
}
