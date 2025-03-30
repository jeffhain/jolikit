/*
 * Copyright 2019-2025 Jeff Hain
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
package net.jolikit.bwd.impl.awt;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;

public class AwtUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * TODO awt TODO mac On Mac, at some point had trouble when using
     * buffered images of type TYPE_INT_ARGB: painting did visibly show up
     * only after some delay, or if we used a lot of drawXxx() primitives
     * on window graphics (like when painting pixels one by one)
     * (this workaround seemed to only work if client area height was above
     * about 210), or did never show up in case of window painting frenzy
     * such as in benches.
     * For some time our workaround was to use TYPE_INT_RGB instead on Mac,
     * which for some reason still allowed to do semi-transparent painting,
     * although it could make things much slower (most likely because of
     * conversions needed between "ARGB" and "RGB_").
     * Another workaround was to do all of these:
     * - have repaint() call super.repaint()
     * - have paint() call paintComponentNowOnG()
     *   and/or have paintComponents() call paintComponentNowOnG()
     * - have paintClientNowOrLater() call window.repaint()
     * Lately though, we didn't encounter this issue anymore,
     * and now we always use TYPE_INT_ARGB_PRE, which is required
     * by our AwtBwdGraphicsWithIntArr implementation.
     * 
     * NB: Default filling is white if using BufferedImage.TYPE_INT_ARGB(_PRE),
     * and black if using BufferedImage.TYPE_INT_RGB.
     */
    public static final int COMMON_BUFFERED_IMAGE_TYPE_ARGB_PRE =
        BufferedImage.TYPE_INT_ARGB_PRE;
    
    public static final BihPixelFormat COMMON_BUFFERED_IMAGE_PIXEL_FORMAT_ARGB =
        BihPixelFormat.ARGB32;
    public static final boolean COMMON_BUFFERED_IMAGE_PREMUL = true;

    private static final AffineTransform BACKING_TRANSFORM_IDENTITY =
        new AffineTransform();
    
    private static final AffineTransform[] ROTATION_TRANSFORM_BY_ORDINAL =
        newRotationTransformArr();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param rect A backing rectangle.
     * @return Corresponding GRect.
     */
    public static GRect toGRect(Rectangle rect) {
        return GRect.valueOf(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @param window A window.
     * @param g A graphics of the specified window, with or without a clip.
     * @return The corresponding dirty rectangle to use, in client area
     *         coordinates.
     */
    public static GRect dirtyRectMovedFromWindowToClient(Window window, Graphics g) {
        final Rectangle clip = g.getClipBounds();
        final GRect dirtyRect;
        if (clip == null) {
            dirtyRect = GRect.DEFAULT_HUGE;
        } else {
            dirtyRect = dirtyRectMovedFromWindowToClient(
                    window,
                    clip.x,
                    clip.y,
                    clip.width,
                    clip.height);
        }
        return dirtyRect;
    }
    
    /**
     * @param window A window.
     * @param x Clip X in window coordinates.
     * @param y Clip Y in window coordinates.
     * @param xSpan Clip X span in window coordinates.
     * @param ySpan Clip Y span in window coordinates.
     * @return The corresponding dirty rectangle to use, in client area
     *         coordinates.
     */
    public static GRect dirtyRectMovedFromWindowToClient(
            Window window,
            int x, int y, int width, int height) {
        final Insets insets = window.getInsets();
        return GRect.valueOf(
                x - insets.left,
                y - insets.top,
                width,
                height);
    }
    
    /*
     * 
     */
    
    public static Color newColor(int argb32) {
        // Called "rgba", but actually alpha is in MSBits.
        final int rgba = argb32;
        final boolean hasalpha = true;
        return new Color(rgba, hasalpha);
    }
    
    /*
     * 
     */
    
    /**
     * Useful to cache backing transforms corresponding to rotations,
     * because Graphics2D.transform(AffineTransform) is much faster
     * than Graphics2D.rotate(double) (no sin/cos computation).
     * 
     * @return Backing transform corresponding to the GRotation
     *         which ordinal is the index.
     */
    public static AffineTransform[] newRotationTransformArr() {
        final GRotation[] rotations = GRotation.values();
        final AffineTransform[] ret = new AffineTransform[rotations.length];
        for (int i = 0; i < rotations.length; i++) {
            final GRotation rotation = rotations[i];
            final AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate(rotation.cos(), rotation.sin());
            ret[i] = affineTransform;
        }
        return ret;
    }
    
    /**
     * Sets transform from root box top-left to user
     * into the specified graphics.
     * (transform (dx,dy) = graphics transform (dx,dy) + rootBoxTopLeft (x,y))
     * 
     * @param rootBoxTopLeft Coordinates in base of graphics top-left pixel.
     * @param transform Transform from base to user.
     * @param g Graphics of which to configure the transform.
     */
    public static void setGraphicsTransform(
        GPoint rootBoxTopLeft,
        GTransform transform,
        Graphics2D g) {
        
        g.setTransform(BACKING_TRANSFORM_IDENTITY);
        
        g.translate(
            transform.frame2XIn1() - rootBoxTopLeft.x(),
            transform.frame2YIn1() - rootBoxTopLeft.y());
        
        g.transform(ROTATION_TRANSFORM_BY_ORDINAL[transform.rotation().ordinal()]);
    }
    
    /**
     * Computes the corresponding graphics transform.
     * (transform (dx,dy) = graphics transform (dx,dy) + rootBoxTopLeft (x,y))
     * 
     * @param rootBoxTopLeft Coordinates in base of graphics top-left pixel.
     * @param transform Transform from base to user.
     * @return The transform from root box top-left to user.
     */
    public static AffineTransform computeGraphicsTransform(
        GPoint rootBoxTopLeft,
        GTransform transform) {
        
        final AffineTransform ret = new AffineTransform();
        
        ret.translate(
            transform.frame2XIn1() - rootBoxTopLeft.x(),
            transform.frame2YIn1() - rootBoxTopLeft.y());
        
        ret.rotate(
            transform.rotation().cos(),
            transform.rotation().sin());
        
        return ret;
    }
    
    /**
     * @param rotation Rotation between base coordinates (frame 1)
     *        and user coordinates (frame 2).
     * @return Delta to (usually) add to user X coordinate to compute
     *         corresponding backing graphics X coordinate.
     */
    public static int computeXShiftInUser(GRotation rotation) {
        final int angDeg = rotation.angDeg();
        if ((angDeg == 180) || (angDeg == 270)) {
            return -1;
        } else {
            return 0;
        }
    }
    
    /**
     * @param rotation Rotation between base coordinates (frame 1)
     *        and user coordinates (frame 2).
     * @return Delta to (usually) add to user Y coordinate to compute
     *         corresponding backing graphics Y coordinate.
     */
    public static int computeYShiftInUser(GRotation rotation) {
        final int angDeg = rotation.angDeg();
        if ((angDeg == 90) || (angDeg == 180)) {
            return -1;
        } else {
            return 0;
        }
    }

    /*
     * 
     */
    
    /**
     * Only returns true if both isShowing() and isVisible()
     * are true.
     */
    public static boolean isShowing(Window window) {
        /*
         * TODO awt isShowing() seems to return the boolean
         * set with setVisible(...), but isVisible() is more
         * terminologically consistent with setVisible(...),
         * so we're not sure which to use, but using both
         * shouldn't hurt (better be wrongly false
         * than wrongly true).
         */
        return window.isShowing()
                && window.isVisible();
    }
    
    public static boolean isIconified(Window window) {
        if (window instanceof Frame) {
            final Frame frame = (Frame) window;
            return (frame.getExtendedState() & Frame.ICONIFIED) != 0;
        } else {
            // Dialogs never iconified.
            return false;
        }
    }
    
    public static boolean isShowingAndDeiconified(Window window) {
        return isShowing(window)
                && (!isIconified(window));
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private AwtUtils() {
    }
}
