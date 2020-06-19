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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jolikit.lang.NumbersUtils;

/**
 * A graphical rectangle, defined as (x,y,xSpan,ySpan),
 * but can also be used to represent borders spans,
 * as in (left,top,right,bottom).
 * 
 * Using (xSpan,ySpan) naming instead of (width,height), because the rectangle
 * might be rotated, and because this naming convention makes it easier
 * to figure our what's going on (where x's and y's are) in complex computations.
 * 
 * Convenience methods workings on ints rather than GRects are also provided,
 * if wanting not to generate GRect garbage for computations.
 * 
 * We allow for rectangles which max bounds leak out of int range,
 * in case of large position and/or spans, for it could be useful
 * for some intermediary computations, or to be able to represent
 * any (x,y,xSpan,ySpan) tuple with a GRect (with positive spans).
 * This also allows to represent huge (left,top,right,bottom) spans,
 * and allows not to have to trim rectangles specified with four ints
 * for convenience methods working on ints instead of GRect instances.
 * 
 * Immutable.
 */
public final class GRect implements Comparable<GRect> {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * The default empty instance, containing (0,0,0,0).
     * 
     * Note that an empty instance can still contain information,
     * such as position and/or one positive span, so this default instance
     * should not always be used instead of other more helpful empty instances.
     */
    public static final GRect DEFAULT_EMPTY = new GRect(0, 0, 0, 0);

    /**
     * A default value with (0,0) position and spans of Integer.MAX_VALUE/2.
     * 
     * Doesn't cover negative positions (such as if centered on (0,0)),
     * so that its union with (0,0,MAX,MAX) (with MAX = Integer.MAX_VALUE),
     * which could be used a huge dirty rectangle by user,
     * can be computed without overflow.
     * Also, taking care for its spans not to be (MAX,MAX),
     * so that its union with a rectangle containing not too huge
     * negative coordinates can be computed without overflow as well,
     * and because actual client areas should not have actual spans
     * of MAX anyway.
     */
    public static final GRect DEFAULT_HUGE = new GRect(
            0,
            0,
            Integer.MAX_VALUE/2,
            Integer.MAX_VALUE/2);

    /*
     * 
     */
    
    /**
     * Empty list (does NOT contain DEFAULT_EMPTY).
     * 
     * Immutable.
     */
    public static final List<GRect> DEFAULT_EMPTY_LIST;
    static {
        final ArrayList<GRect> list = new ArrayList<GRect>();
        DEFAULT_EMPTY_LIST = Collections.unmodifiableList(list);
    }

    /**
     * Contains DEFAULT_HUGE rectangle.
     * 
     * Immutable.
     */
    public static final List<GRect> DEFAULT_HUGE_IN_LIST;
    static {
        final ArrayList<GRect> list = new ArrayList<GRect>();
        list.add(GRect.DEFAULT_HUGE);
        DEFAULT_HUGE_IN_LIST = Collections.unmodifiableList(list);
    }
    
    /*
     * 
     */

    private final int x;

    private final int y;

    /**
     * Always >= 0.
     */
    private final int xSpan;

    /**
     * Always >= 0.
     */
    private final int ySpan;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /*
     * Construction.
     */
    
    /**
     * @param x Min x coordinate.
     * @param y Min y coordinate.
     * @param xSpan X span. Must be >= 0.
     * @param ySpan Y span. Must be >= 0.
     * @return An instance corresponding to the specified arguments.
     * @throws IllegalArgumentException if a span is < 0.
     */
    public static GRect valueOf(
            int x,
            int y,
            int xSpan,
            int ySpan) {
        
        if (xSpan <= 0) {
            if (xSpan < 0) {
                throw new IllegalArgumentException("xSpan [" + xSpan + "] must be >= 0");
            }
            // xSpan is zero, quick check to see if args are (0,0,0,0).
            if ((x|y|ySpan) == 0) {
                return GRect.DEFAULT_EMPTY;
            }
        }
        
        if (ySpan < 0) {
            throw new IllegalArgumentException("ySpan [" + ySpan + "] must be >= 0");
        }
        
        return new GRect(x, y, xSpan, ySpan);
    }

    /*
     * Derivation.
     */

    /**
     * @param x An x coordinate.
     * @return The rectangle obtained from replacing x in this rectangle
     *         with the specified value, preserving y and spans.
     */
    public GRect withX(int x) {
        return valueOf(x, this.y, this.xSpan, this.ySpan);
    }

    /**
     * @param y An y coordinate.
     * @return The rectangle obtained from replacing y in this rectangle
     *         with the specified value, preserving x and spans.
     */
    public GRect withY(int y) {
        return valueOf(this.x, y, this.xSpan, this.ySpan);
    }

    /**
     * @param xSpan An x span.
     * @return The rectangle obtained from replacing xSpan in this rectangle
     *         with the specified value, preserving position and ySpan.
     * @throws IllegalArgumentException if the specified span is < 0.
     */
    public GRect withXSpan(int xSpan) {
        return valueOf(this.x, this.y, xSpan, this.ySpan);
    }

    /**
     * @param ySpan An y span.
     * @return The rectangle obtained from replacing ySpan in this rectangle
     *         with the specified value, preserving position and xSpan.
     * @throws IllegalArgumentException if the specified span is < 0.
     */
    public GRect withYSpan(int ySpan) {
        return valueOf(this.x, this.y, this.xSpan, ySpan);
    }
    
    /*
     * 
     */

    /**
     * @param x An x coordinate.
     * @param y An y coordinate.
     * @return The rectangle obtained from replacing (x,y) in this rectangle
     *         with the specified values, preserving spans.
     */
    public GRect withPos(int x, int y) {
        return valueOf(x, y, this.xSpan, this.ySpan);
    }

    /**
     * @param xSpan An x span.
     * @param ySpan An y span.
     * @return The rectangle obtained from replacing (xSpan,ySpan) in this rectangle
     *         with the specified values, preserving position.
     * @throws IllegalArgumentException if a specified span is < 0.
     */
    public GRect withSpans(int xSpan, int ySpan) {
        return valueOf(this.x, this.y, xSpan, ySpan);
    }
    
    /*
     * 
     */

    /**
     * Shifts are done with modulo arithmetic
     * (so that we can avoid checks overhead).
     * 
     * @param dx Value to add to x, preserving x span.
     * @param dy Value to add to y, preserving y span.
     * @return The resulting rectangle.
     */
    public GRect withPosDeltas(int dx, int dy) {
        return valueOf(this.x + dx, this.y + dy, this.xSpan, this.ySpan);
    }

    /**
     * Shifts are done with modulo arithmetic
     * (so that we can avoid checks overhead).
     * 
     * @param dxSpan Value to add to x span, preserving x.
     * @param dySpan Value to add to y span, preserving y.
     * @return The resulting rectangle.
     * @throws IllegalArgumentException if a resulting span is < 0.
     */
    public GRect withSpansDeltas(int dxSpan, int dySpan) {
        return valueOf(this.x, this.y, this.xSpan + dxSpan, this.ySpan + dySpan);
    }

    /*
     * 
     */
    
    /**
     * Shifts are done with modulo arithmetic
     * (so that we can avoid checks overhead).
     * 
     * @param dxMin Delta to apply to left border.
     * @param dyMin Delta to apply to top border.
     * @param dxMax Delta to apply to right border.
     * @param dyMax Delta to apply to bottom border.
     * @return The resulting rectangle.
     * @throws IllegalArgumentException if a resulting span is < 0.
     */
    public GRect withBordersDeltas(int dxMin, int dyMin, int dxMax, int dyMax) {
        return valueOf(
                this.x + dxMin,
                this.y + dyMin,
                this.xSpan + (dxMax - dxMin),
                this.ySpan + (dyMax - dyMin));
    }
    
    /*
     * Generic.
     */
    
    @Override
    public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.xSpan + ", " + this.ySpan + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return this.x + prime * (this.y + prime * (this.xSpan + prime * this.ySpan));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof GRect)) {
            return false;
        }
        final GRect other = (GRect) obj;
        return this.equalsRect(
                other.x,
                other.y,
                other.xSpan,
                other.ySpan);
    }

    /**
     * If a specified span is negative, necessarily returns false,
     * because doesn't consider that a span < 0 can be equal
     * to a span >= 0.
     * 
     * @param x An x position for a rectangle.
     * @param y An y position for a rectangle.
     * @param xSpan An x span for a rectangle. Can be negative.
     * @param ySpan An y span for a rectangle. Can be negative.
     * @return True if this instance has the specified coordinates,
     *         false otherwise.
     */
    public boolean equalsRect(
            int x, int y, int xSpan, int ySpan) {
        return equalRect(
                this.x, this.y, this.xSpan, this.ySpan,
                x, y, xSpan, ySpan);
    }

    /**
     * Orders first by increasing y, then by increasing x,
     * then by increasing ySpan, then by increasing xSpan.
     * 
     * @throws NullPointerException if the specified rectangle is null.
     */
    @Override
    public int compareTo(GRect other) {
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
        {
            final int cmp = (this.ySpan - other.ySpan);
            if (cmp != 0) {
                return cmp;
            }
        }
        {
            final int cmp = (this.xSpan - other.xSpan);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    /*
     * Getters.
     */

    /**
     * @return Min x coordinate (i.e. leftmost if no rotation).
     */
    public int x() {
        return this.x;
    }

    /**
     * @return Min y coordinate (i.e. topmost if no rotation).
     */
    public int y() {
        return this.y;
    }

    /**
     * @return X span (i.e. horizontal span if no rotation). Is >= 0.
     */
    public int xSpan() {
        return this.xSpan;
    }

    /**
     * @return Y span (i.e. vertical span if no rotation). Is >= 0.
     */
    public int ySpan() {
        return this.ySpan;
    }

    /*
     * 
     */

    /**
     * @return The middle x as x + xSpan/2, i.e. exact or closest to max,
     *         or just x if xSpan is 0.
     * @throws ArithmeticException if middle x overflows.
     */
    public int xMid() {
        final int xMid = this.x + (this.xSpan >> 1);
        if (xMid < this.x) {
            throw new ArithmeticException("xMid [" + xMid + "] overflows");
        }
        return xMid;
    }

    /**
     * @return The middle y as y + ySpan/2, i.e. exact or closest to max,
     *         or just y if ySpan is 0.
     * @throws ArithmeticException if middle y overflows.
     */
    public int yMid() {
        final int yMid = this.y + (this.ySpan >> 1);
        if (yMid < this.y) {
            throw new ArithmeticException("yMid [" + yMid + "] overflows");
        }
        return yMid;
    }
    
    /*
     * 
     */
    
    /**
     * @return The mid x, as long, without overflow.
     */
    public long xMidLong() {
        return this.x + (long) (this.xSpan >> 1);
    }
    
    /**
     * @return The mid y, as long, without overflow.
     */
    public long yMidLong() {
        return this.y + (long) (this.ySpan >> 1);
    }
    
    /*
     * 
     */
    
    /**
     * @return The mid x, as double, exact.
     */
    public double xMidFp() {
        return this.x + ((this.xSpan - 1) * 0.5);
    }
    
    /**
     * @return The mid y, as double, exact.
     */
    public double yMidFp() {
        return this.y + ((this.ySpan - 1) * 0.5);
    }

    /*
     * 
     */

    /**
     * If this rectangle is empty, this value is < x.
     * 
     * @return The max x, as x + xSpan - 1.
     * @throws ArithmeticException if max x overflows, either positively,
     *         or negatively (i.e. when x = Integer.MIN_VALUE and xSpan = 0).
     */
    public int xMax() {
        final int xMax = this.x + this.xSpan - 1;
        if (this.xSpan == 0) {
            if (this.x == Integer.MIN_VALUE) {
                throw new ArithmeticException("xMax [" + xMax + "] overflows (negatively)");
            }
        } else if (xMax < this.x) {
            throw new ArithmeticException("xMax [" + xMax + "] overflows (positively)");
        }
        return xMax;
    }

    /**
     * If this rectangle is empty, this value is < y.
     * 
     * @return The max y, as y + ySpan - 1.
     * @throws ArithmeticException if max y overflows, either positively,
     *         or negatively (i.e. when y = Integer.MIN_VALUE and ySpan = 0).
     */
    public int yMax() {
        final int yMax = this.y + this.ySpan - 1;
        if (this.ySpan == 0) {
            if (this.y == Integer.MIN_VALUE) {
                throw new ArithmeticException("yMax [" + yMax + "] overflows (negatively)");
            }
        } else if (yMax < this.y) {
            throw new ArithmeticException("yMax [" + yMax + "] overflows (positively)");
        }
        return yMax;
    }

    /*
     * 
     */
    
    /**
     * @return The max x, as long, without overflow.
     */
    public long xMaxLong() {
        return this.x + (long) this.xSpan - 1;
    }
    
    /**
     * @return The max y, as long, without overflow.
     */
    public long yMaxLong() {
        return this.y + (long) this.ySpan - 1;
    }
    
    /*
     * 
     */
    
    /**
     * @return min(xSpan, ySpan).
     */
    public int minSpan() {
        return Math.min(this.xSpan, this.ySpan);
    }

    /**
     * @return max(xSpan, ySpan).
     */
    public int maxSpan() {
        return Math.max(this.xSpan, this.ySpan);
    }
    
    /*
     * Area.
     */
    
    /**
     * @return xSpan * ySpan, as an int.
     * @throws ArithmeticException if the product overflows.
     */
    public int area() {
        return NumbersUtils.timesExact(this.xSpan, this.ySpan);
    }
    
    /**
     * @return xSpan * ySpan, as a long (can't overflow).
     */
    public long areaLong() {
        return this.xSpan * (long) this.ySpan;
    }

    /*
     * Computations.
     */

    /**
     * Note that an empty instance can still contain information,
     * such as position and/or one positive span.
     * 
     * @return True if this rectangle is empty, i.e. if xSpan is zero or ySpan is zero,
     *         false otherwise.
     */
    public boolean isEmpty() {
        return (this.xSpan == 0) || (this.ySpan == 0);
    }

    /**
     * @param x An x coordinate.
     * @param y An y coordinate.
     * @return True if the specified (x,y) position is contained in this rectangle,
     *         false otherwise.
     */
    public boolean contains(int x, int y) {
        return (this.x <= x)
                && (this.y <= y)
                && (x < this.xMaxExclLong())
                && (y < this.yMaxExclLong());
    }

    /**
     * An empty rectangle never contains another rectangle,
     * and an empty rectangle is never contained in another rectangle
     * (even if both are empty and have same coordinates).
     * 
     * To check whether the coordinates of a rectangle rect2 are included
     * in those of a rectangle rect1, whether or not they are empty,
     * you can use containsCoordinatesOf(GRect) instead.
     * 
     * @param rect A rectangle.
     * @return True if the specified rectangle is contained in this rectangle,
     *         false otherwise.
     * @throws NullPointerException if the specified rectangle is null.
     */
    public boolean contains(GRect rect) {
        if (rect.isEmpty()) {
            return false;
        }
        return this.containsCoordinatesOf(rect);
    }

    /**
     * Checks whether the coordinates of the specified rectangle
     * are included in those of this rectangle, whether or not
     * they are empty.
     * 
     * @param rect A rectangle.
     * @return True if the specified rectangle's coordinates are contained
     *         within this rectangle's coordinates, false otherwise.
     * @throws NullPointerException if the specified rectangle is null.
     */
    public boolean containsCoordinatesOf(GRect rect) {
        return (rect.x >= this.x)
                && (rect.y >= this.y)
                && (rect.xMaxExclLong() <= this.xMaxExclLong())
                && (rect.yMaxExclLong() <= this.yMaxExclLong());
    }

    /*
     * Overlapping.
     */

    /**
     * If either this or the specified rectangle is empty, returns false.
     * 
     * @param rect A rectangle.
     * @return True if the specified rectangle overlaps this rectangle,
     *         false otherwise.
     * @throws NullPointerException if the specified rectangle is null.
     */
    public boolean overlaps(GRect rect) {
        // Implicit null check.
        if (rect.isEmpty()
                || this.isEmpty()) {
            return false;
        }
        return (this.x < rect.xMaxExclLong())
                && (rect.x < this.xMaxExclLong())
                && (this.y < rect.yMaxExclLong())
                && (rect.y < this.yMaxExclLong());
    }

    /**
     * @param x1 X position of first rectangle.
     * @param y1 Y position of first rectangle.
     * @param xSpan1 X span of first rectangle. Can be negative.
     * @param ySpan1 Y span of first rectangle. Can be negative.
     * @param x2 X position of second rectangle.
     * @param y2 Y position of second rectangle.
     * @param xSpan2 X span of second rectangle. Can be negative.
     * @param ySpan2 Y span of second rectangle. Can be negative.
     * @return True if the specified rectangles overlap,
     *         false otherwise.
     */
    public static boolean overlap(
            int x1, int y1, int xSpan1, int ySpan1,
            int x2, int y2, int xSpan2, int ySpan2) {
        
        if ((xSpan1 <= 0)
                || (ySpan1 <= 0)
                || (xSpan2 <= 0)
                || (ySpan2 <= 0)) {
            return false;
        }
        
        return (x1 < posMaxExclLong(x2, xSpan2))
                && (y1 < posMaxExclLong(y2, ySpan2))
                && (x2 < posMaxExclLong(x1, xSpan1))
                && (y2 < posMaxExclLong(y1, ySpan1));
    }

    /*
     * Intersection.
     */
    
    /**
     * Intersecting two rectangles is computing the largest rectangle
     * where they overlap.
     * 
     * If intersection is empty, does not return a default empty instance,
     * but and instance with x = max(this.x, rect.x), y = max(this.y, rect.y),
     * xSpan = max(0, overlapping_x_span) and ySpan = max(0, overlapping_y_span),
     * for best effort continuity between emptied rectangles and de-emptied
     * rectangles obtained after restoring some coordinate.
     * 
     * @param rect A rectangle.
     * @return The rectangle corresponding to the intersection of this rectangle
     *         and the specified one.
     * @throws NullPointerException if the specified rectangle is null.
     */
    public GRect intersected(GRect rect) {
        // Implicit null check.
        final int interX = intersectedPos(this.x, rect.x);
        final int interY = intersectedPos(this.y, rect.y);
        final int interXSpan = intersectedSpan_raw(
                this.x, this.xSpan,
                rect.x, rect.xSpan,
                interX);
        final int interYSpan = intersectedSpan_raw(
                this.y, this.ySpan,
                rect.y, rect.ySpan,
                interY);

        // If intersection is same as either, returning it.
        final GRect inter;
        if (this.equalsRect(interX, interY, interXSpan, interYSpan)) {
            inter = this;
        } else if (rect.equalsRect(interX, interY, interXSpan, interYSpan)) {
            inter = rect;
        } else {
            inter = new GRect(interX, interY, interXSpan, interYSpan);
        }
        return inter;
    }
    
    /**
     * @param pos1 X or y position of first rectangle.
     * @param pos2 X or y position of second rectangle.
     * @return The position of the intersected rectangle,
     *         as computed by intersected(GRect).
     */
    public static int intersectedPos(int pos1, int pos2) {
        return Math.max(pos1, pos2);
    }

    /**
     * @param pos1 X or y position of first rectangle.
     * @param span1 X or y span of first rectangle. Can be negative.
     * @param pos2 X or y position of second rectangle.
     * @param span2 X or y span of second rectangle. Can be negative.
     * @return The span of the intersected rectangle,
     *         as computed by intersected(GRect).
     */
    public static int intersectedSpan(
            int pos1, int span1,
            int pos2, int span2) {
        if ((span1 <= 0)
                || (span2 <= 0)) {
            return 0;
        }
        final int interPos = intersectedPos(pos1, pos2);
        return intersectedSpan_raw(
                pos1, span1,
                pos2, span2,
                interPos);
    }

    /*
     * Union bounding box.
     */

    /**
     * If this rectangle is empty, returns a rectangle containing
     * only the specified position.
     * 
     * @param x An x coordinate.
     * @param y An y coordinate.
     * @return The bounding box containing this rectangle and the specified position.
     * @throws ArithmeticException if the resulting bounding box
     *         has a span larger than Integer.MAX_VALUE.
     */
    public GRect unionBoundingBox(int x, int y) {
        if (this.contains(x, y)) {
            return this;
        }
        if (this.isEmpty()) {
            return new GRect(x, y, 1, 1);
        }

        final int bbX = unionBoundingBoxPos_raw(this.x, x);
        final int bbY = unionBoundingBoxPos_raw(this.y, y);

        final int bbXSpan = unionBoundingBoxSpan_raw(
                this.x, this.xSpan,
                x, 1,
                bbX);
        final int bbYSpan = unionBoundingBoxSpan_raw(
                this.y, this.ySpan,
                y, 1,
                bbY);

        return new GRect(bbX, bbY, bbXSpan, bbYSpan);
    }

    /**
     * If one of the rectangles is empty and the other is not,
     * returns the non-empty one.
     * Else, separately for x and y, the bounding box coordinates
     * are computed as follows:
     * - pos = min(this.pos, rect.pos),
     * - span = 0 if either span is 0, else union span.
     * 
     * @param rect A rectangle.
     * @return The bounding box containing this rectangle and the specified
     *         rectangle.
     * @throws NullPointerException if the specified rectangle is null.
     * @throws ArithmeticException if the resulting bounding box
     *         has a span larger than Integer.MAX_VALUE.
     */
    public GRect unionBoundingBox(GRect rect) {
        // Implicit null check.
        final boolean rectIsEmpty = rect.isEmpty();
        final boolean thisIsEmpty = this.isEmpty();
        if (thisIsEmpty != rectIsEmpty) {
            if (thisIsEmpty) {
                return rect;
            } else {
                return this;
            }
        }

        final int bbX = unionBoundingBoxPos_raw(this.x, rect.x);
        final int bbY = unionBoundingBoxPos_raw(this.y, rect.y);
        final int bbXSpan;
        if ((this.xSpan > 0) && (rect.xSpan > 0)) {
            bbXSpan = unionBoundingBoxSpan_raw(
                    this.x, this.xSpan,
                    rect.x, rect.xSpan,
                    bbX);
        } else {
            bbXSpan = 0;
        }
        final int bbYSpan;
        if ((this.ySpan > 0) && (rect.ySpan > 0)) {
            bbYSpan = unionBoundingBoxSpan_raw(
                    this.y, this.ySpan,
                    rect.y, rect.ySpan,
                    bbY);
        } else {
            bbYSpan = 0;
        }

        // If bounding box is same as either, returning it.
        final GRect bb;
        if (this.equalsRect(bbX, bbY, bbXSpan, bbYSpan)) {
            bb = this;
        } else if (rect.equalsRect(bbX, bbY, bbXSpan, bbYSpan)) {
            bb = rect;
        } else {
            bb = new GRect(bbX, bbY, bbXSpan, bbYSpan);
        }
        return bb;
    }

    /*
     * Overflow.
     */
    
    /**
     * Note that if x is Integer.MIN_VALUE and x span is 0, then xMax()
     * negatively overflows, but then we don't consider that this rectangle
     * contains this negatively overflowing position (nor any other position,
     * since it's empty), so in this case this method returns false.
     * 
     * @return True if this rectangle covers x coordinates out of int range,
     *         false otherwise.
     */
    public boolean doesOverflowInX() {
        final int xMax = this.x + this.xSpan - 1;
        if (xMax < this.x) {
            return (this.xSpan > 0);
        }
        return false;
    }
    
    /**
     * Note that if y is Integer.MIN_VALUE and y span is 0, then yMax()
     * negatively overflows, but then we don't consider that this rectangle
     * contains this negatively overflowing position (nor any other position,
     * since it's empty), so in this case this method returns false.
     * 
     * @return True if this rectangle covers y coordinates out of int range,
     *         false otherwise.
     */
    public boolean doesOverflowInY() {
        final int yMax = this.y + this.ySpan - 1;
        if (yMax < this.y) {
            return (this.ySpan > 0);
        }
        return false;
    }

    /**
     * @return True if this rectangle covers x or y coordinates out of int range,
     *         false otherwise.
     */
    public boolean doesOverflow() {
        return this.doesOverflowInX() || this.doesOverflowInY();
    }
    
    /**
     * Useful for rectangles defined as (x,y,xSpan,ySpan).
     * 
     * @param pos A position.
     * @param span A span. Can be negative, in which case O is used instead
     *        (and therefore false is returned).
     * @return True if (pos + span - 1) positively overflows out of int range,
     *         false otherwise.
     */
    public static boolean doesOverflow(int pos, int span) {
        if (span < 0) {
            return false;
        }
        final int max = pos + span - 1;
        return (max < pos) && (span > 0);
    }
    
    /*
     * Trimming (when overflows).
     */

    /**
     * If this rectangle doesn't overflow, just returns it.
     * 
     * @return The largest rectangle with the same position but that
     *         doesn't overflow, i.e. with spans small enough for
     *         max x and max y to be in int range.
     */
    public GRect trimmed() {
        final int xSpan = trimmedSpan_raw(this.x, this.xSpan);
        final int ySpan = trimmedSpan_raw(this.y, this.ySpan);
        if ((xSpan == this.xSpan) && (ySpan == this.ySpan)) {
            return this;
        } else {
            return valueOf(this.x, this.y, xSpan, ySpan);
        }
    }

    /**
     * Useful for rectangles defined as (x,y,xSpan,ySpan).
     * 
     * @param pos A position.
     * @param span A span. Can be negative, in which case
     *        the specified value is returned (this method just trims,
     *        it doesn't act as normalizer).
     * @return The larger span inferior or equal to the specified span,
     *         such as (pos + span - 1) doesn't positively overflow
     *         out of int range.
     */
    public static int trimmedSpan(int pos, int span) {
        if (span <= 0) {
            return span;
        }
        return trimmedSpan_raw(pos, span);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * Doesn't do any check.
     * 
     * @param xSpan Must be >= 0.
     * @param ySpan Must be >= 0.
     */
    private GRect(
            int x,
            int y,
            int xSpan,
            int ySpan) {
        this.x = x;
        this.y = y;
        this.xSpan = xSpan;
        this.ySpan = ySpan;
    }

    /*
     * 
     */

    private static boolean equalRect(
            int x1, int y1, int xSpan1, int ySpan1,
            int x2, int y2, int xSpan2, int ySpan2) {
        return (x1 == x2)
                && (y1 == y2)
                && (xSpan1 == xSpan2)
                && (ySpan1 == ySpan2);
    }

    /*
     * 
     */
    
    /**
     * @return x + xSpan, as long, without overflow.
     */
    private long xMaxExclLong() {
        return posMaxExclLong(this.x, this.xSpan);
    }

    /**
     * @return y + ySpan, as long, without overflow.
     */
    private long yMaxExclLong() {
        return posMaxExclLong(this.y, this.ySpan);
    }

    /**
     * @return pos + span, as long, without overflow.
     */
    private static long posMaxExclLong(int pos, int span) {
        return pos + (long) span;
    }

    /*
     * 
     */
    
    /**
     * @param span1 Must be >= 0.
     * @param span2 Must be >= 0.
     * @param intersectedPos The result of intersectedPos(pos1, pos2).
     */
    private static int intersectedSpan_raw(
            int pos1, int span1,
            int pos2, int span2,
            int intersectedPos) {
        final long interPosMaxExclLong = Math.min(
                posMaxExclLong(pos1, span1),
                posMaxExclLong(pos2, span2));
        // Can negatively overflows int range,
        // but if positive is necessarily in int range.
        final long deltaLong = interPosMaxExclLong - intersectedPos;
        final int intersectedSpan = (int) Math.max(0, deltaLong);
        return intersectedSpan;
    }

    /*
     * 
     */
    
    /**
     * Only makes sense if rectangles are both empty or both non-empty:
     * else, the union is the non-empty one.
     */
    private static int unionBoundingBoxPos_raw(int pos1, int pos2) {
        return Math.min(pos1, pos2);
    }

    /**
     * Only makes sense if rectangles have both non-zero x span:
     * else, the union has the largest x span (possibly 0).
     * 
     * @param span1 Must be > 0.
     * @param span2 Must be > 0.
     * @param bbPos The result of unionBoundingBoxPos_raw(pos1, pos2).
     * @throws ArithmeticException if union span overflows.
     */
    private int unionBoundingBoxSpan_raw(
            int pos1, int span1,
            int pos2, int span2,
            int bbPos) {
        final long bbPosMaxExclLong = Math.max(
                posMaxExclLong(pos1, span1),
                posMaxExclLong(pos2, span2));
        final long bbSpanLong = (bbPosMaxExclLong - bbPos);
        if (bbSpanLong > (long) Integer.MAX_VALUE) {
            throw new ArithmeticException("int overflow: " + bbSpanLong);
        }
        final int bbSpan = (int) bbSpanLong;
        return bbSpan;
    }

    /*
     * 
     */
    
    /**
     * @param span Must be >= 0.
     */
    private static int trimmedSpan_raw(int pos, int span) {
        final int max = pos + span - 1;
        final boolean overflow = (max < pos) && (span > 0);
        if (overflow) {
            span = (Integer.MAX_VALUE - pos + 1);
        }
        return span;
    }
}
