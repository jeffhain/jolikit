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
package net.jolikit.bwd.api.graphics;

import net.jolikit.lang.LangUtils;

/**
 * A graphical transform, as a rotation multiple of 90 degrees,
 * plus an integer translation.
 * 
 * The rotation is the one to obtain frame 2 orientation from frame 1,
 * and the translation is the position of frame 2 origin in frame 1.
 * 
 * The rotation angle can only be in {0,90,180,270} degrees,
 * and is positive from (Ox) axis to (Oy) axis (i.e. clockwise).
 * 
 * Conversions are designed for speed, not for handling coordinates overflow.
 * 
 * Immutable.
 */
public final class GTransform implements Comparable<GTransform> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * The identity instance (identity transform is always necessarily this instance).
     */
    public static final GTransform IDENTITY = new GTransform();
    
    private static final GTransform INSTANCE_90_0_0 = new GTransform(GRotation.ROT_90, 0, 0);
    private static final GTransform INSTANCE_180_0_0 = new GTransform(GRotation.ROT_180, 0, 0);
    private static final GTransform INSTANCE_270_0_0 = new GTransform(GRotation.ROT_270, 0, 0);
    
    private final GRotation rotation;
    
    private final int frame2XIn1;
    private final int frame2YIn1;
    
    /**
     * Not lazily initialized, to make sure the inverse instance returned
     * is always the same, even if in practice that should not hurt to have
     * possibly different instances returned.
     */
    private final GTransform inverse;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param angDeg Rotation of frame 2 from frame 1, in {0,90,180,270}.
     * @param frame2XIn1 Frame 2 origin X in frame 1.
     * @param frame2YIn1 Frame 2 origin Y in frame 1.
     * @return The corresponding transform.
     */
    public static GTransform valueOf(int angDeg, int frame2XIn1, int frame2YIn1) {
        return valueOf(GRotation.valueOf(angDeg), frame2XIn1, frame2YIn1);
    }
    
    /**
     * Representation of frame 2 depending on rotation:
     * 02 = origin of frame 2
     * xd2 = positive direction of frame 2 x axis
     * yd2 = positive direction of frame 2 y axis
     * 
     * ROT_0:
     *             02---------.xd2
     *              |         |
     *              .---------.
     *             yd2
     * 
     * ROT_90:
     *       yd2.---02
     *          |   |
     *          |   |
     *          |   |
     *          .---.
     *             xd2
     * 
     * ROT_180:
     *             yd2
     *    .---------.
     *    |         |
     * xd2.---------02
     * 
     * ROT_270:
     *             xd2
     *              .---.
     *              |   |
     *              |   |
     *              |   |
     *             02---.yd2
     * 
     * @param rotation Rotation of frame 2 from frame 1.
     * @param frame2XIn1 Frame 2 origin X in frame 1.
     * @param frame2YIn1 Frame 2 origin Y in frame 1.
     * @return The corresponding transform.
     * @throws NullPointerException is the specified rotation is null.
     */
    public static GTransform valueOf(GRotation rotation, int frame2XIn1, int frame2YIn1) {
        if ((frame2XIn1|frame2YIn1) == 0) {
            // Implicit null check.
            switch (rotation) {
            case ROT_0: return IDENTITY;
            case ROT_90: return INSTANCE_90_0_0;
            case ROT_180: return INSTANCE_180_0_0;
            case ROT_270: return INSTANCE_270_0_0;
            default:
                throw new AssertionError();
            }
        }
        return new GTransform(
                LangUtils.requireNonNull(rotation),
                frame2XIn1,
                frame2YIn1);
    }
    
    /**
     * Representation of frame 2 depending on rotation:
     * 02 = origin of frame 2
     * xd2 = positive direction of frame 2 x axis
     * yd2 = positive direction of frame 2 y axis
     * 
     * ROT_0:
     *       xSpan
     *   02---------.xd2
     *    |         | ySpan
     *    .---------.
     *   yd2
     * 
     * ROT_90:
     *    ySpan
     * yd2.---02
     *    |   |
     *    |   | xSpan
     *    |   |
     *    .---.
     *       xd2
     * 
     * ROT_180:
     *       xSpan yd2
     *    .---------.
     *    |         | ySpan
     * xd2.---------02
     * 
     * ROT_270:
     *   xd2 ySpan
     *    .---.
     *    |   |
     *    |   | xSpan
     *    |   |
     *   02---.yd2
     * 
     * The specified rectangle is rotated according to the specified rotation,
     * and is then moved for its new top-left corner to be at the same position
     * its initial top-left corner was.
     * The computed transform has the specified rotation, and a translation
     * corresponding to the new position of the initial top-left corner
     * of the rectangle.
     * Computing a transform this way is useful to draw a same component,
     * in frame 2, with identical code whatever its orientation, using its
     * position as rectangle position and its local spans as rectangle spans.
     * 
     * @param rotation Rotation for the returned transform.
     * @param xIn1 Rectangle x coordinate in frame 1, before and after rotation/translation.
     * @param yIn1 Rectangle y coordinate in frame 1, before and after rotation/translation.
     * @param xSpan Rectangle x span.
     * @param ySpan Rectangle y span.
     * @return The described transform.
     * @throws NullPointerException is the specified rotation is null.
     */
    public static GTransform valueOf(
            GRotation rotation,
            int xIn1, int yIn1, int xSpan, int ySpan) {
        final GTransform oldTransform = null;
        return valueOf(rotation, xIn1, yIn1, xSpan, ySpan, oldTransform);
    }
    
    /**
     * Same as valueOf(GRotation,int,int,int,int), but returns the specified transform
     * if it's appropriate, to avoid creating a new one.
     * 
     * @param rotation Rotation for the returned transform.
     * @param xIn1 Rectangle x coordinate in frame 1, before and after rotation/translation.
     * @param yIn1 Rectangle y coordinate in frame 1, before and after rotation/translation.
     * @param xSpan Rectangle x span.
     * @param ySpan Rectangle y span.
     * @param oldTransform Can be null. If not null and appropriate,
     *        is returned instead of creating a new instance.
     * @return The described transform.
     * @throws NullPointerException is the specified rotation is null.
     */
    public static GTransform valueOf(
            GRotation rotation,
            int xIn1, int yIn1, int xSpanIn1, int ySpanIn1,
            GTransform oldTransform) {
        final int dx;
        final int dy;
        // Implicit null check.
        switch (rotation) {
        case ROT_0: dx = xIn1; dy = yIn1; break;
        case ROT_90: dx = xIn1 + xSpanIn1-1; dy = yIn1; break;
        case ROT_180: dx = xIn1 + xSpanIn1-1; dy = yIn1 + ySpanIn1-1; break;
        case ROT_270: dx = xIn1; dy = yIn1 + ySpanIn1-1; break;
        default:
            throw new AssertionError();
        }
        if ((oldTransform != null)
                && oldTransform.equalsTransform(rotation, dx, dy)) {
            return oldTransform;
        } else {
            return GTransform.valueOf(rotation, dx, dy);
        }
    }

    /*
     * Derivation.
     */

    /**
     * @return A transform corresponding to the inverse of this one.
     */
    public GTransform inverted() {
        return this.inverse;
    }
    
    /**
     * @param transform Transform to compose with this one.
     * @return Transform(1,3), this being transform(1,2),
     *         and the specified one transform(2,3).
     * @throws NullPointerException is the specified transform is null.
     */
    public GTransform composed(GTransform transform) {
        final GRotation rot1To3 = this.rotation.plus(transform.rotation);
        final int frame3XIn1 = this.xIn1(transform.frame2XIn1, transform.frame2YIn1);
        final int frame3YIn1 = this.yIn1(transform.frame2XIn1, transform.frame2YIn1);
        return valueOf(
                rot1To3,
                frame3XIn1,
                frame3YIn1);
    }

    /*
     * 
     */
    
    @Override
    public String toString() {
        final int angDeg = this.rotation.angDeg();
        final int x = this.frame2XIn1;
        final int y = this.frame2YIn1;
        return "[" + angDeg + ", " + x + ", " + y + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hc = this.rotation.hashCode();
        hc = hc * prime + this.frame2XIn1;
        hc = hc * prime + this.frame2YIn1;
        return hc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof GTransform)) {
            return false;
        }
        final GTransform other = (GTransform) obj;
        return this.equalsTransform(
                other.rotation,
                other.frame2XIn1,
                other.frame2YIn1);
    }

    /**
     * @param rotation Can be null.
     * @param frame2XIn1 Frame 2 origin X in frame 1.
     * @param frame2YIn1 Frame 2 origin Y in frame 1.
     * @return True if this instance has the specified parameters,
     *         false otherwise.
     */
    public boolean equalsTransform(
            GRotation rotation,
            int frame2XIn1,
            int frame2YIn1) {
        return (this.rotation == rotation)
                && (this.frame2XIn1 == frame2XIn1)
                && (this.frame2YIn1 == frame2YIn1);
    }

    /**
     * Orders first by increasing y, then by increasing x,
     * then by increasing rotation.
     * 
     * @throws NullPointerException if the specified transform is null.
     */
    @Override
    public int compareTo(GTransform other) {
        /*
         * Low y first, for line-by-line ordering.
         */
        if (this.frame2YIn1 < other.frame2YIn1) {
            return -1;
        } else if (this.frame2YIn1 > other.frame2YIn1) {
            return 1;
        }
        if (this.frame2XIn1 < other.frame2XIn1) {
            return -1;
        } else if (this.frame2XIn1 > other.frame2XIn1) {
            return 1;
        }
        /*
         * Rotation last, for transforms of similar translations
         * not to be separated far away from each other
         * in 4 rotation groups.
         */
        return this.rotation.compareTo(other.rotation);
    }
    
    /*
     * Getters.
     */
    
    /**
     * @return The rotation from frame 1 to frame 2.
     */
    public GRotation rotation() {
        return this.rotation;
    }

    /**
     * @return Frame 2 origin x in frame 1.
     */
    public int frame2XIn1() {
        return this.frame2XIn1;
    }
    
    /**
     * @return Frame 2 origin y in frame 1.
     */
    public int frame2YIn1() {
        return this.frame2YIn1;
    }

    /*
     * Computations.
     */
    
    /**
     * Equivalent to "== IDENTITY", since IDENTITY
     * is the only possible identity instance.
     * 
     * @return True if this transform is the identity, false otherwise.
     */
    public boolean isIdentity() {
        return (this == IDENTITY);
    }

    /*
     * 
     */

    /**
     * @param xIn2 X coordinate in frame 2.
     * @param yIn2 Y coordinate in frame 2.
     * @return The corresponding x coordinate in frame 1.
     */
    public int xIn1(int xIn2, int yIn2) {
        return this.frame2XIn1 + this.rotation.dxIn1(xIn2, yIn2);
    }
    
    /**
     * @param xIn2 X coordinate in frame 2.
     * @param yIn2 Y coordinate in frame 2.
     * @return The corresponding y coordinate in frame 1.
     */
    public int yIn1(int xIn2, int yIn2) {
        return this.frame2YIn1 + this.rotation.dyIn1(xIn2, yIn2);
    }
    
    /**
     * @param xIn1 X coordinate in frame 1.
     * @param yIn1 Y coordinate in frame 1.
     * @return The corresponding x coordinate in frame 2.
     */
    public int xIn2(int xIn1, int yIn1) {
        // Not using inverse, to avoid eventual cache miss,
        // and to make the computation asymmetry explicit.
        return this.rotation.dxIn2(
                xIn1 - this.frame2XIn1,
                yIn1 - this.frame2YIn1);
    }
    
    /**
     * @param xIn1 X coordinate in frame 1.
     * @param yIn1 Y coordinate in frame 1.
     * @return The corresponding y coordinate in frame 2.
     */
    public int yIn2(int xIn1, int yIn1) {
        return this.rotation.dyIn2(
                xIn1 - this.frame2XIn1,
                yIn1 - this.frame2YIn1);
    }
    
    /*
     * 
     */
    
    /**
     * @return Min x in frame 1 for the specified rectangle in frame 2.
     */
    public int minXIn1(int xIn2, int yIn2, int xSpanIn2, int ySpanIn2) {
        final int xIn1 = this.xIn1(xIn2, yIn2);
        switch (this.rotation) {
        case ROT_0: return xIn1;
        case ROT_90: return xIn1 - (ySpanIn2 - 1);
        case ROT_180: return xIn1 - (xSpanIn2 - 1);
        case ROT_270: return xIn1;
        default:
            throw new AssertionError();
        }
    }
    
    public int minYIn1(int xIn2, int yIn2, int xSpanIn2, int ySpanIn2) {
        final int yIn1 = this.yIn1(xIn2, yIn2);
        switch (this.rotation) {
        case ROT_0: return yIn1;
        case ROT_90: return yIn1;
        case ROT_180: return yIn1 - (ySpanIn2 - 1);
        case ROT_270: return yIn1 - (xSpanIn2 - 1);
        default:
            throw new AssertionError();
        }
    }
    
    /**
     * @return Min x in frame 2 for the specified rectangle in frame 1.
     */
    public int minXIn2(int xIn1, int yIn1, int xSpanIn1, int ySpanIn1) {
        return this.inverted().minXIn1(xIn1, yIn1, xSpanIn1, ySpanIn1);
    }
    
    public int minYIn2(int xIn1, int yIn1, int xSpanIn1, int ySpanIn1) {
        return this.inverted().minYIn1(xIn1, yIn1, xSpanIn1, ySpanIn1);
    }
    
    /*
     * 
     */

    public GRect rectIn1(GRect rectIn2) {
        if (this.isIdentity()) {
            return rectIn2;
        }
        return rectIn1(rectIn2.x(), rectIn2.y(), rectIn2.xSpan(), rectIn2.ySpan());
    }
    
    public GRect rectIn2(GRect rectIn1) {
        return this.inverted().rectIn1(rectIn1);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructs identity.
     */
    private GTransform() {
        this.rotation = GRotation.ROT_0;
        this.frame2XIn1 = 0;
        this.frame2YIn1 = 0;
        this.inverse = this;
    }
    
    /**
     * Must not be used for identity.
     */
    private GTransform(
            GRotation rotation,
            int frame2XIn1,
            int frame2YIn1) {
        this.rotation = rotation;
        this.frame2XIn1 = frame2XIn1;
        this.frame2YIn1 = frame2YIn1;
        final int frame1XIn2 = -rotation.dxIn2(frame2XIn1, frame2YIn1);
        final int frame1YIn2 = -rotation.dyIn2(frame2XIn1, frame2YIn1);
        this.inverse = new GTransform(
                rotation.inverted(),
                frame1XIn2,
                frame1YIn2,
                this);
    }
    
    /**
     * For creating the inverse.
     * 
     * Must not be used for identity, which is the inverse of itself.
     */
    private GTransform(
            GRotation rotation,
            int dx,
            int dy,
            GTransform inv) {
        this.rotation = rotation;
        this.frame2XIn1 = dx;
        this.frame2YIn1 = dy;
        this.inverse = inv;
    }
    
    /*
     * 
     */
    
    private GRect rectIn1(int xIn2, int yIn2, int xSpanIn2, int ySpanIn2) {
        final int x = this.minXIn1(xIn2, yIn2, xSpanIn2, ySpanIn2);
        final int y = this.minYIn1(xIn2, yIn2, xSpanIn2, ySpanIn2);
        final int xSpan = this.rotation.xSpanInOther(xSpanIn2, ySpanIn2);
        final int ySpan = this.rotation.ySpanInOther(xSpanIn2, ySpanIn2);
        return GRect.valueOf(x, y, xSpan, ySpan);
    }
}
