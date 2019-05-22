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

/**
 * A graphical point, but can also be used to represent spans.
 * 
 * Immutable.
 */
public final class GPoint implements Comparable<GPoint> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final GPoint INSTANCE_0_0 = new GPoint(0, 0);
    
    /**
     * A constant for (max,max), in case used a lot for max spans.
     */
    private static final GPoint INSTANCE_MAX_MAX = new GPoint(Integer.MAX_VALUE, Integer.MAX_VALUE);
    
    /**
     * The (0,0) instance ((0,0) is always necessarily this instance).
     */
    public static final GPoint ZERO = INSTANCE_0_0;
    
    private final int x;
    private final int y;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Construction.
     */
    
    /**
     * @param x X coordinate.
     * @param y Y coordinate.
     * @return An instance corresponding to the specified arguments.
     */
    public static GPoint valueOf(int x, int y) {
        if ((x|y) == 0) {
            return INSTANCE_0_0;
        } else if ((x == Integer.MAX_VALUE)
                && (y == Integer.MAX_VALUE)) {
            return INSTANCE_MAX_MAX;
        } else {
            return new GPoint(x, y);
        }
    }
    
    /*
     * Derivation.
     */
    
    /**
     * @param x A value.
     * @return The point obtained from replacing x in this point
     *         with the specified value, preserving y.
     */
    public GPoint withX(int x) {
        return valueOf(x, this.y);
    }
    
    /**
     * @param y A value.
     * @return The point obtained from replacing y in this point
     *         with the specified value, preserving x.
     */
    public GPoint withY(int y) {
        return valueOf(this.x, y);
    }
    
    /**
     * Shifts are done with modulo arithmetic
     * (so that we can avoid checks overhead).
     * 
     * @param dx Value to add to x.
     * @param dy Value to add to y.
     * @return The resulting point.
     */
    public GPoint withDeltas(int dx, int dy) {
        return valueOf(this.x + dx, this.y + dy);
    }

    /*
     * Generic.
     */
    
    @Override
    public String toString() {
        return "[" + this.x + ", " + this.y + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        return this.x + prime * this.y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof GPoint)) {
            return false;
        }
        final GPoint other = (GPoint) obj;
        return this.equalsPoint(other.x, other.y);
    }
    
    /**
     * @param x An x coordinate.
     * @param y An y coordinate.
     * @return True if this instance has the specified coordinates,
     *         false otherwise.
     */
    public boolean equalsPoint(int x, int y) {
        return (this.x == x)
                && (this.y == y);
    }

    /**
     * Orders first by increasing y, and then by increasing x.
     */
    @Override
    public int compareTo(GPoint other) {
        /*
         * Low y first, for line-by-line ordering.
         */
        if (this.y < other.y) {
            return -1;
        } else if (this.y > other.y) {
            return 1;
        }
        if (this.x < other.x) {
            return -1;
        } else if (this.x > other.x) {
            return 1;
        }
        return 0;
    }
    
    /*
     * Getters.
     */

    /**
     * @return The x coordinate.
     */
    public int x() {
        return this.x;
    }
    
    /**
     * @return The y coordinate.
     */
    public int y() {
        return this.y;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private GPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
