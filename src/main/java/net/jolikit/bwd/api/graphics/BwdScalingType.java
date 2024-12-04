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
     * Fastest, but can quickly get ugly on downscaling
     * as more and more source pixels don't get used. 
     */
    NEAREST,
    /**
     * Slower than NEAREST, but all source pixels covered
     * by each destination pixel contribute proportionally to the overlap,
     * so the quality is much better on downscaling.
     * 
     * If there is no scaling, or integer-multiple upscaling,
     * is equivalent to NEAREST, so should delegate to NEAREST
     * in these cases for speed.
     */
    BOXSAMPLED,
    /**
     * Unlike BOXSAMPLED, on upscaling it causes smoothing,
     * but also some blurring.
     * On downscaling, should use iterations to make sure that
     * all covered source pixels contribute.
     * Note that iterations on downscaling can both make it slower
     * than BOXSAMPLED, and cause the end result to drift further away
     * from original image than with BOXSAMPLED, due to accumulated deltas,
     * such as for large downscaling BOXSAMPLED might be preferable.
     * 
     * If there is no scaling, is equivalent to NEAREST,
     * so should delegate to NEAREST in that case for speed.
     */
    BILINEAR,
    /**
     * Similar to BILINEAR but gives a sharper result,
     * at the cost of being slower.
     * On downscaling, same remarks as for BILINEAR.
     * 
     * If there is no scaling, is equivalent to NEAREST,
     * so should delegate to NEAREST in that case for speed.
     */
    BICUBIC,
    /**
     * Same as BILINEAR except when downscaling divides width or height
     * by more than two, in which case, instead of splitting
     * the downscaling in multiple bilinear iterations,
     * downscaling is first partially done using BOXSAMPLED,
     * and then terminated using bilinear with width or height
     * divided by two.
     * 
     * Combines the smoothing qualities of BILINEAR,
     * with the quality and speed of BOXSAMPLED for large downscalings. 
     * 
     * If there is no scaling, is equivalent to NEAREST,
     * so should delegate to NEAREST in that case for speed.
     */
    BOXSAMPLED_BILINEAR,
    /**
     * Same as BICUBIC except when downscaling divides width or height
     * by more than two, in which case, instead of splitting
     * the downscaling in multiple bicubic iterations,
     * downscaling is first partially done using BOXSAMPLED,
     * and then terminated using bicubic with width or height
     * divided by two.
     * 
     * Combines the smoothing and sharpening qualities of BICUBIC,
     * with the quality and speed of BOXSAMPLED for large downscalings. 
     * 
     * If there is no scaling, is equivalent to NEAREST,
     * so should delegate to NEAREST in that case for speed.
     */
    BOXSAMPLED_BICUBIC;
    
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
