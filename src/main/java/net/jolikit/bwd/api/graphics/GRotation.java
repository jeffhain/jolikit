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
package net.jolikit.bwd.api.graphics;

import java.util.Arrays;
import java.util.List;

/**
 * A graphical rotation, multiple of 90 degrees,
 * with methods for conversions of int coordinates
 * between initial frame of reference ("1")
 * and rotated one ("2").
 * 
 * Conversions are designed for speed, not for handling coordinates overflow.
 * 
 * Immutable.
 */
public enum GRotation {
    ROT_0,
    ROT_90,
    ROT_180,
    ROT_270;
    
    /*
     * [[xIn2]  = [[ cos(rot1To2), sin(rot1To2)]  * [[xIn1]
     *  [yIn2]]    [-sin(rot1To2), cos(rot1To2)]]    [yIn1]]
     * 
     * rot1To2 = 0:
     * [[xIn2]  = [[1, 0]  * [[xIn1]
     *  [yIn2]]    [0, 1]]    [yIn1]]
     * xIn2 = xIn1
     * yIn2 = yIn1
     * 
     * rot1To2 = 90:
     * [[xIn2]  = [[ 0, 1]  * [[xIn1]
     *  [yIn2]]    [-1, 0]]    [yIn1]]
     * xIn2 = yIn1
     * yIn2 = -xIn1
     * 
     * rot1To2 = 180:
     * [[xIn2]  = [[-1,  0]  * [[xIn1]
     *  [yIn2]]    [ 0, -1]]    [yIn1]]
     * xIn2 = -xIn1
     * yIn2 = -yIn1
     * 
     * rot1To2 = 270:
     * [[xIn2]  = [[0, -1]  * [[xIn1]
     *  [yIn2]]    [1,  0]]    [yIn1]]
     * xIn2 = -yIn1
     * yIn2 = xIn1
     */

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final GRotation[] ARR = GRotation.values();
    
    private static final List<GRotation> LIST = Arrays.asList(ARR);

    private final int angDeg;
    private final int sin;
    private final int cos;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return An unmodifiable list of enum values, with ordinal as index.
     */
    public static List<GRotation> valueList() {
        return LIST;
    }
    
    /*
     * Construction.
     */
    
    /**
     * @param The rotation angle, positive from (0x) to (0y),
     *        in degrees, in {0,90,180,270}.
     */
    public static GRotation valueOf(int angDeg) {
        switch (angDeg) {
        case 0: return ROT_0;
        case 90: return ROT_90;
        case 180: return ROT_180;
        case 270: return ROT_270;
        default:
            throw new IllegalArgumentException("" + angDeg);
        }
    }
    
    /*
     * Derivation.
     */
    
    /**
     * @return The previous rotation, i.e. after rotating 90 degrees counter-clockwise.
     */
    public GRotation prev() {
        switch (this) {
        case ROT_0: return ROT_270;
        case ROT_90: return ROT_0;
        case ROT_180: return ROT_90;
        case ROT_270: return ROT_180;
        default:
            throw new AssertionError();
        }
    }
    
    /**
     * @return The next rotation, i.e. after rotating 90 degrees clockwise.
     */
    public GRotation next() {
        switch (this) {
        case ROT_0: return ROT_90;
        case ROT_90: return ROT_180;
        case ROT_180: return ROT_270;
        case ROT_270: return ROT_0;
        default:
            throw new AssertionError();
        }
    }
    
    /**
     * @return The inverse rotation.
     */
    public GRotation inverted() {
        switch (this) {
        case ROT_0: return ROT_0;
        case ROT_90: return ROT_270;
        case ROT_180: return ROT_180;
        case ROT_270: return ROT_90;
        default:
            throw new AssertionError();
        }
    }
    
    /**
     * @param quadrants Number of 90 degrees rotations to apply.
     *        Can be negative, in which case rotations are applied
     *        backward.
     * @return The rotation corresponding to this rotation
     *         plus the specified number of quadrants.
     */
    public GRotation plusQuadrants(int quadrants) {
        int endQuadrant = (this.ordinal() + quadrants) % 4;
        if (endQuadrant < 0) {
            endQuadrant += 4;
        }
        return ARR[endQuadrant];
    }
    
    /**
     * @param rotation Rotation to add.
     * @return The rotation corresponding to this rotation
     *         plus the specified rotation.
     * @throws NullPointerException if the specified rotation is null.
     */
    public GRotation plus(GRotation rotation) {
        return this.plusQuadrants(rotation.ordinal());
    }

    /**
     * @param rotation Rotation to subtract.
     * @return The rotation corresponding to this rotation
     *         minus the specified rotation.
     * @throws NullPointerException if the specified rotation is null.
     */
    public GRotation minus(GRotation rotation) {
        return this.plusQuadrants(-rotation.ordinal());
    }
    
    /*
     * Getters.
     */
    
    /**
     * @return The rotation angle, in degrees, in {0,90,180,270}.
     */
    public int angDeg() {
        return this.angDeg;
    }

    /**
     * @return The rotation angle sine, in {-1,0,1}.
     */
    public int sin() {
        return this.sin;
    }
    
    /**
     * @return The rotation angle cosine, in {-1,0,1}.
     */
    public int cos() {
        return this.cos;
    }
    
    /*
     * Computations.
     */
    
    /**
     * @return True horizontal/vertical lines in one frame
     *         are vertical/horizontal lines in the other.
     */
    public boolean areHorVerFlipped() {
        return this.sin != 0;
    }

    /*
     * 
     */
    
    /**
     * @param dxIn2 X coordinate delta in frame 2.
     * @param dyIn2 Y coordinate delta in frame 2.
     * @return The corresponding x coordinate delta in frame 1.
     */
    public int dxIn1(int dxIn2, int dyIn2) {
        return dxIn2 * this.cos + dyIn2 * -this.sin;
    }
    
    /**
     * @param dxIn2 X coordinate delta in frame 2.
     * @param dyIn2 Y coordinate delta in frame 2.
     * @return The corresponding y coordinate delta in frame 1.
     */
    public int dyIn1(int dxIn2, int dyIn2) {
        return dxIn2 * this.sin + dyIn2 * this.cos;
    }
    
    /**
     * @param dxIn1 X coordinate delta in frame 1.
     * @param dyIn1 Y coordinate delta in frame 1.
     * @return The corresponding x coordinate delta in frame 2.
     */
    public int dxIn2(int dxIn1, int dyIn1) {
        return dxIn1 * this.cos + dyIn1 * this.sin;
    }
    
    /**
     * @param dxIn1 X coordinate delta in frame 1.
     * @param dyIn1 Y coordinate delta in frame 1.
     * @return The corresponding y coordinate delta in frame 2.
     */
    public int dyIn2(int dxIn1, int dyIn1) {
        return dxIn1 * -this.sin + dyIn1 * this.cos;
    }
    
    /*
     * 
     */
    
    /**
     * Accepts negative spans.
     * 
     * @param xSpan X span in 1 or 2.
     * @param ySpan Y span in 1 or 2 (same as for xSpan).
     * @return X span in 2 or 1 (not the same as for arguments).
     */
    public int xSpanInOther(int xSpan, int ySpan) {
        return (this.sin == 0) ? xSpan : ySpan;
    }
    
    /**
     * Accepts negative spans.
     * 
     * @param xSpan X span in 1 or 2.
     * @param ySpan Y span in 1 or 2 (same as for xSpan).
     * @return Y span in 2 or 1 (not the same as for arguments).
     */
    public int ySpanInOther(int xSpan, int ySpan) {
        return (this.sin == 0) ? ySpan : xSpan;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private GRotation() {
        // Quadrant in [0,3].
        final int q = this.ordinal();
        
        final int angDeg = q * 90;
        this.angDeg = angDeg;
        
        /*
         * Computing sin and cos, instead of just specifying them
         * to the constructor, even if our algorithm is based on
         * knowing them in the first place, because we don't like
         * redundant inputs.
         */
        
        this.sin = sinOfQ(q);
        this.cos = cosOfQ(q);
    }
    
    /*
     * 
     */
    
    /**
     * @param q Quadrant in [0,3].
     * @return sine.
     */
    private static int sinOfQ(int q) {
        /*
         * q | sin | cos | qb1 | qb0
         * --+-----+-----+-----+----
         * 0 |  0  |  1  |  0  |  0
         * 1 |  1  |  0  |  0  |  1
         * 2 |  0  | -1  |  1  |  0
         * 3 | -1  |  0  |  1  |  1
         */
        final int qb0 = (q & 1);
        final int qb1 = ((q >> 1) & 1);
        return qb0 * (1 - 2 * qb1);
    }
    
    /**
     * @param q Quadrant in [0,3].
     * @return cosine.
     */
    private static int cosOfQ(int q) {
        /*
         * cos(a) = sin(a + PI/2)
         * "& 3" does "% 4"
         */
        return sinOfQ((q + 1) & 3);
    }
}
