/*
 * Copyright 2024-2025 Jeff Hain
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
    
    /*
     * Base algos.
     */
    
    /**
     * Fastest, but can quickly get ugly on downscaling
     * as more and more source pixels don't get used. 
     */
    NEAREST,
    /**
     * More accurate than iterative bilinear
     * in case of non-uniform downscaling.
     * 
     * Not blurry on upscaling, as it preserves pixels shape,
     * except eventually on pixel edges, which might make
     * NEAREST preferable for large upscalings.
     */
    BOXSAMPLED,
    /**
     * BILINEAR with iterations for downscaling.
     */
    ITERATIVE_BILINEAR,
    /**
     * BICUBIC with iterations for downscaling.
     */
    ITERATIVE_BICUBIC,
    
    /*
     * Composite algos, with most useful/usual cases.
     * 
     * Useful both in case of non-uniform scaling,
     * and not to have to change the scaling type
     * depending on whether we have downscaling or upscaling.
     */
    
    /**
     * Uses ITERATIVE_BILINEAR for downscaling,
     * and BICUBIC for upscaling.
     */
    ITERATIVE_BILINEAR_BICUBIC,
    /**
     * Uses BOXSAMPLED for downscaling,
     * and BICUBIC for upscaling.
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
