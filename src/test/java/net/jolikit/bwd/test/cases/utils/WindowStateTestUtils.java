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
package net.jolikit.bwd.test.cases.utils;

import net.jolikit.bwd.api.graphics.GRect;

/**
 * Utilities to test windows states.
 */
public class WindowStateTestUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Tolerance for position/span ratio, when comparing window bounds
     * to screen bounds to decide whether window looks maximized or not.
     * 
     * Tolerance is quite large, and we allow for leak outside screen bounds,
     * because libraries typically leak outside of them, or use full
     * instead of available screen bounds for maximization.
     */
    private static final double MAXIMIZED_LIKE_TOLERANCE_RATIO = 0.1;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static boolean doesWindowLookMaximized(
            GRect screenBounds,
            GRect windowBounds) {
        final int sw = screenBounds.xSpan();
        final int sh = screenBounds.ySpan();
        
        if (isDeltaOutOfMaximizedLikeToleranceRatio(
                windowBounds.x() - (double) screenBounds.x(),
                sw)) {
            return false;
        }
        if (isDeltaOutOfMaximizedLikeToleranceRatio(
                windowBounds.xMax() - (double) screenBounds.xMax(),
                sw)) {
            return false;
        }
        if (isDeltaOutOfMaximizedLikeToleranceRatio(
                windowBounds.y() - (double) screenBounds.y(),
                sh)) {
            return false;
        }
        if (isDeltaOutOfMaximizedLikeToleranceRatio(
                windowBounds.yMax() - (double) screenBounds.yMax(),
                sh)) {
            return false;
        }
        
        return true;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private WindowStateTestUtils() {
    }
    
    private static boolean isDeltaOutOfMaximizedLikeToleranceRatio(
            double delta,
            int span) {
        final double ratio = Math.abs(delta) / span;
        return (ratio > MAXIMIZED_LIKE_TOLERANCE_RATIO);
    }
}
