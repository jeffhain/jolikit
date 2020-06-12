/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.jfx;

import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;

public class JfxUtils {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * True for having the 0.5 offset (integer BWD coordinates being centered on
     * pixels, not pixels borders as in JavaFX) in the transform itself.
     * TODO jfx For some reason, some rotated Strings are more badly rendered
     * when it is true, so we keep it false.
     */
    public static final boolean H_IN_T = false;
    
    /**
     * Half.
     */
    private static final double H = 0.5;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param rect A backing rectangle.
     * @return Corresponding GRect.
     */
    public static GRect toGRect(Rectangle2D rect) {
        return GRect.valueOf(
                (int) rect.getMinX(),
                (int) rect.getMinY(),
                (int) rect.getWidth(),
                (int) rect.getHeight());
    }
    
    public static Color newColor(int argb32) {
        final double alphaFp = Argb32.getAlphaFp(argb32);
        final double redFp = Argb32.getRedFp(argb32);
        final double greenFp = Argb32.getGreenFp(argb32);
        final double blueFp = Argb32.getBlueFp(argb32);

        return new Color(redFp, greenFp, blueFp, alphaFp);
    }

    public static Color newColor(long argb64) {
        final double alphaFp = Argb64.getAlphaFp(argb64);
        final double redFp = Argb64.getRedFp(argb64);
        final double greenFp = Argb64.getGreenFp(argb64);
        final double blueFp = Argb64.getBlueFp(argb64);

        return new Color(redFp, greenFp, blueFp, alphaFp);
    }

    /**
     * @return A new canvas, with a configuration common to all our needs.
     */
    public static Canvas newCanvas(int width, int height) {
        final Canvas canvas = new Canvas(width, height);
        
        /*
         * Making sure X axis goes to the right (Language-related orientation
         * must be taken care of at a higher level. It could also be
         * TOP_TO_BOTTOM, which JavaFX doesn't support).
         */
        canvas.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        
        return canvas;
    }
    
    /*
     * 
     */

    /**
     * @param rotation Rotation between base coordinates (frame 1)
     *        and user coordinates (frame 2).
     * @return Delta to (usually) add to user X coordinate to compute
     *         corresponding GC X coordinate.
     */
    public static double computeXShiftInUser(GRotation rotation) {
        if (H_IN_T) {
            return 0;
        } else {
            return (rotation.cos() + rotation.sin()) * H;
        }
    }

    /**
     * @param rotation Rotation between base coordinates (frame 1)
     *        and user coordinates (frame 2).
     * @return Delta to (usually) add to user Y coordinate to compute
     *         corresponding GC Y coordinate.
     */
    public static double computeYShiftInUser(GRotation rotation) {
        if (H_IN_T) {
            return 0;
        } else {
            return (rotation.cos() - rotation.sin()) * H;
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JfxUtils() {
    }
}
