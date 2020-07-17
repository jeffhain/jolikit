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

public class PixelFigStatusComputer {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static PixelFigStatus computePixelFigStatus(
            boolean isFillElseDraw,
            int nbrOfSurroundingEightsIn) {
        
        final boolean paintingRequired = PixelFigStatusComputer.computePixelPaintingRequired(
                isFillElseDraw,
                nbrOfSurroundingEightsIn);
        if (paintingRequired) {
            return PixelFigStatus.PIXEL_REQUIRED;
        }
        
        final boolean paintingAllowed = PixelFigStatusComputer.computePixelPaintingAllowed(
                isFillElseDraw,
                nbrOfSurroundingEightsIn);
        if (paintingAllowed) {
            return PixelFigStatus.PIXEL_ALLOWED;
        }
        
        return PixelFigStatus.PIXEL_NOT_ALLOWED;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private PixelFigStatusComputer() {
    }
    
    private static boolean computePixelPaintingAllowed(
            boolean isFillElseDraw,
            int nbrOfSurroundingEightsIn) {
        if (isFillElseDraw) {
            return (nbrOfSurroundingEightsIn >= 1);
        } else {
            return (nbrOfSurroundingEightsIn >= 1)
                    && (nbrOfSurroundingEightsIn <= 7);
        }
    }

    private static boolean computePixelPaintingRequired(
            boolean isFillElseDraw,
            int nbrOfSurroundingEightsIn) {
        if (isFillElseDraw) {
            return (nbrOfSurroundingEightsIn >= 3);
        } else {
            /*
             * If [3,5] causes some pixel to be erroneously required,
             * could also just use [4] instead.
             */
            return (nbrOfSurroundingEightsIn >= 3)
                    && (nbrOfSurroundingEightsIn <= 5);
        }
    }
}
