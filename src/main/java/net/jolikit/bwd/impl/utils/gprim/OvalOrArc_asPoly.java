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

import java.util.Arrays;
import java.util.Comparator;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;

/**
 * Algorithm to draw or fill arcs as polylines or polygons.
 * 
 * Useful in case of huge arcs, for which "Midpoint circle algorithm"
 * can take ages, and much faster than going brute-force on each pixel
 * of clipped bounding box.
 * 
 * For arcs filling, is consistent with drawLine()
 * for start and end segments.
 * 
 * The basic idea is simple, but tricks are needed not to use many more points
 * for the polygon than the number of pixels of the visible (clipped) arc.
 */
public class OvalOrArc_asPoly {

    /*
     * Input angles are in degrees, and counter clockwise.
     * 
     * Angles used internally are in radians, and clockwise
     * (more consistent with trigonometry operations).
     * 
     * To simplify computations, angles in radians
     * are always ensured to be in [0,2*PI].
     */

    /*
     * Computing intersections of oval, not with clipped oval bounding box,
     * but with an enlarged clipped oval bounding box, and drawing arc parts
     * accurately between these intersections.
     * 
     * This allows to draw pixels accurately on clipped oval bounding box edges,
     * and to draw discontinuous arc parts with a single polyline,
     * using paths on enlarged clipped oval bounding box edges as wormholes.
     */

    /*
     * To ease code exploration, the many internals are mostly ordered
     * like this: first from high level to low level ones,
     * then from first used to last used.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * True to speed things up in some trivial cases,
     * without having to use waypoints.
     * 
     * NB: Slows down a bit the general case,
     * but not by much, so should be worth it.
     * 
     * Tests must pass whatever this value.
     */
    private static final boolean MUST_DO_EARLY_CHECKS_TO_AVOID_WAYPOINTS = true;

    /**
     * One pixel per step can be too inaccurate,
     * causing coordinates jump and bad straight line
     * between non contiguous pixels.
     * 
     * Also helps against gross inaccuracies when oval spans are small,
     * due to pixels (which we use as unit of angular iteration)
     * being almost as large as the whole oval
     * (for that, could also use a MAX_STEP_RAD constant).
     * 
     * Half a pixel per step is good enough for our tests.
     * 
     * Could lower it to hit more pixels (and be more symmetric/consistent),
     * but drawing would get slower.
     */
    private static final double PIXEL_STEP_PIXEL_RATIO = 0.5;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Order matters, for compareTo() to match
     * start < entering < corner < exiting < end.
     */
    private enum MyWaypointType {
        ARC_START,
        OVAL_INTER_ENTERING,
        ECOB_CORNER,
        OVAL_INTER_EXITING,
        ARC_END;
    }

    private static class MyWaypoint {
        private final OvalOrArc_asPoly owner;
        //
        MyWaypointType type;
        /**
         * In [0,2*PI].
         */
        double angRad;
        double sinAng;
        double cosAng;
        double xd;
        double yd;
        int isInEcobInt;
        //
        boolean mustDoSteps;
        boolean isInOval;
        //
        public MyWaypoint(OvalOrArc_asPoly owner) {
            this.owner = owner;
        }
        @Override
        public String toString() {
            final StringBuilder bonusSb = new StringBuilder();
            if ((this.type == MyWaypointType.ECOB_CORNER)
                    || (this.type == MyWaypointType.OVAL_INTER_ENTERING)
                    || (this.type == MyWaypointType.OVAL_INTER_EXITING)) {
                bonusSb.append(" (");
                if (this.xd == this.owner.ecobXMin) {
                    bonusSb.append("L");
                }
                if (this.xd == this.owner.ecobXMax) {
                    bonusSb.append("R");
                }
                if (this.yd == this.owner.ecobYMin) {
                    bonusSb.append("T");
                }
                if (this.yd == this.owner.ecobYMax) {
                    bonusSb.append("B");
                }
                bonusSb.append(")");
            }
            final String bonusStr = bonusSb.toString();
            return "[" + this.type + bonusStr
                    + ", " + Math.toDegrees(this.angRad)
                    + " deg, " + this.xd
                    + ", " + this.yd
                    + ", mustDoSteps = " + this.mustDoSteps
                    + ", isInOval = " + this.isInOval
                    + ", isInEcob = " + this.isInEcob()
                    + "]";
        }
        public void configureBase(
                MyWaypointType type,
                double angRad,
                double sinAng,
                double cosAng,
                double xd,
                double yd) {
            this.type = type;
            this.angRad = angRad;
            this.sinAng = sinAng;
            this.cosAng = cosAng;
            this.xd = xd;
            this.yd = yd;
            this.isInEcobInt = -1;
        }
        public void configureStartEnd(
                MyWaypointType type,
                double angRad) {
            final double sinAng = Math.sin(angRad);
            final double cosAng = Math.cos(angRad);
            final double xd = this.owner.cxd + this.owner.rxd * cosAng;
            final double yd = this.owner.cyd + this.owner.ryd * sinAng;
            this.configureBase(type, angRad, sinAng, cosAng, xd, yd);

            if (type == MyWaypointType.ARC_START) {
                this.mustDoSteps = this.isInEcob();
            } else {
                // Doesn't matter.
                this.mustDoSteps = false;
            }
            // On it.
            this.isInOval = true;
        }
        public void configureInterWithOval(
                int sideType,
                double angRad,
                double sinAng,
                double cosAng,
                double xd,
                double yd) {
            final MyWaypointType wpType = computeInterType(
                    sideType,
                    angRad);
            this.configureBase(
                    wpType,
                    angRad,
                    sinAng,
                    cosAng,
                    xd,
                    yd);
            this.mustDoSteps = (wpType == MyWaypointType.OVAL_INTER_ENTERING);
            // On it.
            this.isInOval = true;
        }
        public void configureCorner(
                double xd,
                double yd) {

            final MyWaypointType type = MyWaypointType.ECOB_CORNER;
            final double dx = xd - this.owner.cxd;
            final double dy = yd - this.owner.cyd;
            final double angRad = atan2ZeroTwoPi(dy * this.owner.rxdOverRyd, dx);
            final double sinAng = Math.sin(angRad);
            final double cosAng = Math.cos(angRad);
            this.configureBase(type, angRad, sinAng, cosAng, xd, yd);

            this.mustDoSteps = false;

            final double dxForAng = this.owner.rxd * this.cosAng;
            final double dyForAng = this.owner.ryd * this.sinAng;
            // NB: only one test should be enough
            final boolean isCornerInOval =
                    (Math.abs(dx) <= Math.abs(dxForAng))
                    && (Math.abs(dy) <= Math.abs(dyForAng));
            this.isInOval = isCornerInOval;
        }
        public boolean isInEcob() {
            if (this.isInEcobInt < 0) {
                this.isInEcobInt = (this.owner.isInEcob(this.xd, this.yd) ? 1 : 0);
            }
            return (this.isInEcobInt == 1);
        }
    }
    private static final Comparator<MyWaypoint> WAYPOINT_ANG_COMPARATOR =
            new Comparator<MyWaypoint>() {
        @Override
        public int compare(MyWaypoint o1, MyWaypoint o2) {
            // Both are in [0,2*PI].
            final double a1 = o1.angRad;
            final double a2 = o2.angRad;
            final int cmp;
            if (a1 == a2) {
                // start < entering < corner < exiting < end
                cmp = o1.type.compareTo(o2.type);
            } else {
                cmp = Double.compare(a1, a2);
            }
            return cmp;
        }
    };

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final double H = 0.5;
    
    /**
     * Large enough to always add to or subtract from angle values.
     */
    private static final double ULP_360 =
            360.0 - Math.nextAfter(360.0, Double.NEGATIVE_INFINITY);

    /**
     * Span growth on each side, to obtain extended clipped oval box
     * from clipped oval box.
     * 
     * Must be at least 0.5, to include edge pixels completely.
     * 
     * Value chosen for ecob sides to never be on pixels edges or centers,
     * to avoid special cases and related issues.
     */
    private static final double ECOB_GROWTH = 0.75;
    
    private static final int SIDE_LEFT = 0;
    private static final int SIDE_RIGHT = 1;
    private static final int SIDE_TOP = 2;
    private static final int SIDE_BOTTOM = 3;

    /**
     * arc start and end, 8 arc intersections, 4 corners.
     */
    private static final int MAX_NBR_OF_WAYPOINTS = 14;

    private static final ThreadLocal<OvalOrArc_asPoly> TL_TEMPS = new ThreadLocal<OvalOrArc_asPoly>() {
        @Override
        public OvalOrArc_asPoly initialValue() {
            return new OvalOrArc_asPoly();
        }
    };

    /*
     * Args data.
     */

    private boolean isFillElseDraw;

    private boolean isFull;

    /*
     * Arc data.
     */

    private double cxd;
    private double cyd;

    private int cx;
    private int cy;

    private double rxd;
    private double ryd;

    private double rxdInv;
    private double rydInv;

    private double rxdOverRyd;

    private double arcStartRad;
    private double arcEndRad;
    private double arcSpanRad;

    /*
     * Ecob data.
     */

    private double ecobXMin;
    private double ecobYMin;
    private double ecobXMax;
    private double ecobYMax;

    private boolean isCenterInEcob;

    private boolean isCenterInDiagNinth;

    /*
     * Base waypoints.
     */

    /**
     * Waypoints instances always all referenced from this array,
     * even when unused.
     */
    private final MyWaypoint[] baseWaypointArr = new MyWaypoint[MAX_NBR_OF_WAYPOINTS];
    {
        for (int i = 0; i < this.baseWaypointArr.length; i++) {
            this.baseWaypointArr[i] = new MyWaypoint(this);
        }
    }

    /**
     * Number of waypoints used in baseWaypointArr.
     */
    private int baseWaypointCount;

    /*
     * Base waypoints analysis.
     */

    private MyWaypoint closestCornerWp;
    private MyWaypoint farthestCornerWp;

    /**
     * null if center in ecob.
     */
    private MyWaypoint ecobStartCornerWp;

    /**
     * null if center in ecob.
     */
    private MyWaypoint ecobEndCornerWp;

    /**
     * zero if center in ecob.
     */
    private double ecobStartRad;

    /**
     * 2*PI if center is in ecob, else <= PI.
     */
    private double ecobSpanRad;

    /*
     * Kept waypoints.
     */

    /**
     * Waypoints retained for current arc drawing/filling.
     */
    private final MyWaypoint[] keptWaypointArr = new MyWaypoint[MAX_NBR_OF_WAYPOINTS];
    private int keptWaypointCount;

    /*
     * Stepped iterations.
     */

    /**
     * Max step to use, in radians.
     */
    private double maxStepRad;

    /*
     * Points for poly.
     */

    private final XyArrs arrs = new XyArrs();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Takes care of span reduction in case of overflow.
     * 
     * @param startDeg Must be in [0.0,360.0[.
     * @param spanDeg Must be in [0,360.0].
     */
    public static void drawOrFillOvalOrArc(
            GRect clip,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            boolean areHorVerFlipped,
            boolean isFillElseDraw,
            //
            InterfacePointDrawer pointDrawer,
            InterfaceLineDrawer lineDrawer,
            InterfaceRectDrawer rectDrawer,
            InterfacePolyDrawer polyDrawer) {

        if (DEBUG) {
            Dbg.log("drawOrFillOvalOrArc("
                    + clip
                    + ", " + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ", " + areHorVerFlipped + ", " + isFillElseDraw
                    + ",,,,)");
        }

        final boolean isEmpty =
                (xSpan <= 0)
                || (ySpan <= 0)
                || (spanDeg == 0.0);
        if (isEmpty) {
            return;
        }

        xSpan = GRect.trimmedSpan(x, xSpan);
        ySpan = GRect.trimmedSpan(y, ySpan);

        final GRect oval = GRect.valueOf(x, y, xSpan, ySpan);
        // cob = clipped oval box.
        final GRect cob = clip.intersected(oval);
        if (cob.isEmpty()) {
            return;
        }

        /*
         * Special case for ovals/arcs with a span of 1.
         */

        if ((xSpan == 1) || (ySpan == 1)) {
            if (DEBUG) {
                Dbg.log("special case : a span = 1");
            }
            final double rx = ((xSpan - 1) * 0.5);
            final double ry = ((ySpan - 1) * 0.5);
            final double cx = x + rx;
            final double cy = y + ry;
            OvalOrArc_span1.drawArc_xSpan1_or_ySpan1(
                    clip,
                    x, y, xSpan, ySpan,
                    startDeg, spanDeg,
                    cx, cy,
                    pointDrawer,
                    lineDrawer);
            return;
        }

        final boolean isFull = (spanDeg == 360.0);

        /*
         * 
         */

        if (MUST_DO_EARLY_CHECKS_TO_AVOID_WAYPOINTS) {
            if (isFillElseDraw) {
                if (!GprimUtils.mustFillOvalOrArc(clip, x, y, xSpan, ySpan)) {
                    if (DEBUG) {
                        Dbg.log("must not fill");
                    }
                    return;
                }
            } else {
                if (!GprimUtils.mustDrawOvalOrArc(clip, x, y, xSpan, ySpan)) {
                    if (DEBUG) {
                        Dbg.log("must not draw");
                    }
                    return;
                }
            }

            /*
             * Special case for ovals containing the clip.
             */

            if (isFillElseDraw
                    && isFull
                    && GprimUtils.isClipInOval(clip, x, y, xSpan, ySpan)) {
                if (DEBUG) {
                    Dbg.log("fill clip");
                }
                rectDrawer.fillRect(
                        clip,
                        clip.x(), clip.y(), clip.xSpan(), clip.ySpan(),
                        areHorVerFlipped);
                return;
            }
        }

        /*
         * Eventually reworking angles not to paint pixels
         * in wrong quadrant in case of ties.
         */

        if (isFull) {
            // To avoid drawing nothing instead of full.
            startDeg = 0.0;
        } else {
            double endDeg = plusZero360(startDeg, spanDeg);

            if ((startDeg == 0.0)
                    || (startDeg == 90.0)
                    || (startDeg == 180.0)
                    || (startDeg == 270.0)) {
                startDeg = startDeg + Math.min(ULP_360, spanDeg * 0.25);
            }

            if ((endDeg == 90.0)
                    || (endDeg == 180.0)
                    || (endDeg == 270.0)
                    || (endDeg == 360.0)) {
                endDeg = endDeg - Math.min(ULP_360, spanDeg * 0.25);
            }

            spanDeg = minusZero360(endDeg, startDeg);
        }

        /*
         * 
         */

        final OvalOrArc_asPoly temps = TL_TEMPS.get();

        temps.configureArgsData(isFillElseDraw, isFull);

        temps.configureArcData(oval, startDeg, spanDeg);

        temps.configureEcobData(cob);

        final double cxd = temps.cxd;
        final double cyd = temps.cyd;
        final int cx = temps.cx;
        final int cy = temps.cy;

        final double rxd = temps.rxd;
        final double ryd = temps.ryd;

        temps.computeBaseWaypoints(
                clip,
                oval);

        if (DEBUG) {
            for (int i = 0; i < temps.baseWaypointCount; i++) {
                final MyWaypoint wp = temps.baseWaypointArr[i];
                Dbg.log("baseWp[" + i + "] = " + wp);
            }
        }

        final boolean gotToDraw = temps.analyzeBaseWaypoints();
        if (!gotToDraw) {
            if (DEBUG) {
                Dbg.log("nothing to draw");
            }
            return;
        }

        temps.computeKeptWaypoints();

        if (DEBUG) {
            Dbg.log("keptWaypointCount = " + temps.keptWaypointCount);
            for (int i = 0; i < temps.keptWaypointCount; i++) {
                final MyWaypoint wp = temps.keptWaypointArr[i];
                Dbg.log("keptWp[" + i + "] = " + wp);
            }
        }

        /*
         * 
         */

        final XyArrs arrs = temps.arrs;
        arrs.clear();

        if (isFillElseDraw) {
            final boolean mustAddCenterPoint;
            if (temps.isCenterInEcob) {
                // If full span, not using point for center,
                // to make polygon convex (faster filling,
                // especially if filling algorithm can see
                // that clip is fully inside polygon).
                mustAddCenterPoint = !temps.isFull;
            } else {
                // Even if full, used arc span can be reduced.
                mustAddCenterPoint = (temps.arcSpanRad < 2*Math.PI);
            }
            if (DEBUG) {
                Dbg.log("mustAddCenterPoint = " + mustAddCenterPoint);
            }
            if (mustAddCenterPoint) {
                if (DEBUG) {
                    Dbg.log("arrs[0] = center point = (" + cx + ", " + cy + ")");
                }
                arrs.addPoint(cx, cy);
            }
        }

        /*
         * Looping on kept waypoints.
         */

        if (DEBUG) {
            Dbg.log("arcStartDeg = " + Math.toDegrees(temps.arcStartRad));
            Dbg.log("arcEndDeg = " + Math.toDegrees(temps.arcEndRad));
            Dbg.log("arcSpanDeg = " + Math.toDegrees(temps.arcSpanRad));
        }

        temps.computeMaxStepRad();
        if (DEBUG) {
            Dbg.log("maxStepDeg = " + Math.toDegrees(temps.maxStepRad));
        }

        MyWaypoint prevWp = null;

        for (int i = 0; i < temps.keptWaypointCount; i++) {
            if (DEBUG) {
                Dbg.log("wpIndex = " + i);
            }
            final MyWaypoint wp = temps.keptWaypointArr[i];
            if (DEBUG) {
                Dbg.log("wp = " + wp + " (" + (i + 1) + "/" + temps.keptWaypointCount + ")");
            }

            final boolean mustDoSteps = wp.mustDoSteps;
            if (DEBUG) {
                Dbg.log("mustDoSteps = " + mustDoSteps);
            }

            if ((prevWp != null)
                    && (!prevWp.mustDoSteps)) {
                final double jumpSpanRad = minusZeroTwoPi(wp.angRad, prevWp.angRad);
                if (DEBUG) {
                    Dbg.log("jumpSpanDeg = " + Math.toDegrees(jumpSpanRad));
                }
                if (jumpSpanRad > Math.PI) {
                    final int xi = cx;
                    final int yi = cy;
                    if (DEBUG) {
                        Dbg.log("wp[" + i + "] (anti-crossing) : xi = " + xi);
                        Dbg.log("wp[" + i + "] (anti-crossing) : yi = " + yi);
                    }
                    arrs.addPointSmart(xi, yi);
                }
            }

            if (mustDoSteps) {
                // Always a next waypoint here,
                // since we never do steps for end waypoint.
                final MyWaypoint nextWp = temps.keptWaypointArr[i + 1];
                final double itStartRad = wp.angRad;
                final double itEndRad = nextWp.angRad;
                final double itSpanRad = minusZeroTwoPi(itEndRad, itStartRad);
                if (DEBUG) {
                    Dbg.log("nextWp = " + nextWp);
                    Dbg.log("itStartDeg = " + Math.toDegrees(itStartRad));
                    Dbg.log("itEndDeg = " + Math.toDegrees(itEndRad));
                    Dbg.log("itSpanDeg = " + Math.toDegrees(itSpanRad));
                }

                double angRad = itStartRad;
                double sinAng = wp.sinAng;
                double cosAng = wp.cosAng;
                int k = 0;
                double deltaRad = 0.0;
                boolean mustBreakIterAfterNextAdd = false;
                while (true) {
                    if (DEBUG) {
                        Dbg.log("k = " + k);
                    }
                    if (k > 0) {
                        angRad = itStartRad + deltaRad;
                        sinAng = Math.sin(angRad);
                        cosAng = Math.cos(angRad);
                    }
                    final double dx = rxd * cosAng;
                    final double dy = ryd * sinAng;
                    final double xd = cxd + dx;
                    final double yd = cyd + dy;
                    final int xi = roundArcCoord_x(cxd, sinAng, cosAng, xd);
                    final int yi = roundArcCoord_y(cyd, sinAng, cosAng, yd);
                    if (DEBUG) {
                        Dbg.log("wp[" + i + "]it[" + k + "] : angDeg = " + Math.toDegrees(angRad));
                        Dbg.log("wp[" + i + "]it[" + k + "] : dx = " + dx);
                        Dbg.log("wp[" + i + "]it[" + k + "] : dy = " + dy);
                        Dbg.log("wp[" + i + "]it[" + k + "] : xd = " + xd);
                        Dbg.log("wp[" + i + "]it[" + k + "] : yd = " + yd);
                        Dbg.log("wp[" + i + "]it[" + k + "] : xi = " + xi);
                        Dbg.log("wp[" + i + "]it[" + k + "] : yi = " + yi);
                    }
                    arrs.addPointSmart(xi, yi);

                    if (mustBreakIterAfterNextAdd) {
                        if (DEBUG) {
                            Dbg.log("reached IT end");
                        }
                        break;
                    }

                    final double stepRad = temps.computeStepRad(sinAng, cosAng);
                    if (DEBUG) {
                        Dbg.log("stepDeg = " + Math.toDegrees(stepRad));
                    }

                    deltaRad += stepRad;
                    if (deltaRad >= itSpanRad) {
                        deltaRad = itSpanRad;
                        mustBreakIterAfterNextAdd = true;
                    }

                    if (DEBUG) {
                        Dbg.log("deltaDeg = " + Math.toDegrees(deltaRad));
                        Dbg.log("mustBreakIterAfterNextAdd = " + mustBreakIterAfterNextAdd);
                    }
                    // Here, when deltaRad is itSpanRad,
                    // next point is last point for this iteration.
                    k++;
                }
            } else {
                final double xd = wp.xd;
                final double yd = wp.yd;
                if (DEBUG) {
                    Dbg.log("straight xd = " + xd);
                    Dbg.log("straight yd = " + yd);
                }
                final double sinAng = wp.sinAng;
                final double cosAng = wp.cosAng;
                final int xi = roundArcCoord_x(cxd, sinAng, cosAng, xd);
                final int yi = roundArcCoord_y(cyd, sinAng, cosAng, yd);
                if (DEBUG) {
                    Dbg.log("wp[" + i + "] : xi = " + xi);
                    Dbg.log("wp[" + i + "] : yi = " + yi);
                }
                arrs.addPointSmart(xi, yi);
            }

            if (wp.type == MyWaypointType.ARC_END) {
                if (DEBUG) {
                    Dbg.log("reached ARC end");
                }
                break;
            }

            prevWp = wp;
        }

        /*
         * 
         */

        final int pointCount = arrs.size();

        final GRect clipForPoly = cob;
        if (DEBUG) {
            Dbg.log("clipForPoly = " + clipForPoly);
            Dbg.log("xArr = " + arrs.toStringXArr());
            Dbg.log("yArr = " + arrs.toStringYArr());
            Dbg.log("pointCount = " + pointCount);
        }

        if (isFillElseDraw) {
            polyDrawer.fillPolygon(
                    clipForPoly,
                    arrs.xArr(),
                    arrs.yArr(),
                    pointCount,
                    areHorVerFlipped);
        } else {
            polyDrawer.drawPolyline(
                    clipForPoly,
                    arrs.xArr(),
                    arrs.yArr(),
                    pointCount);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private OvalOrArc_asPoly() {
    }

    /*
     * Args data.
     */

    private void configureArgsData(
            boolean isFillElseDraw,
            boolean isFull) {

        if (DEBUG) {
            Dbg.log("configureArgsData()");
            Dbg.log("isFillElseDraw = " + isFillElseDraw);
            Dbg.log("isFull = " + isFull);
        }

        this.isFillElseDraw = isFillElseDraw;

        this.isFull = isFull;
    }

    /*
     * Arc data.
     */

    private void configureArcData(
            GRect oval,
            double startDeg, double spanDeg) {

        if (DEBUG) {
            Dbg.log("configureArcData()");
        }

        this.cxd = oval.xMidFp();
        this.cyd = oval.yMidFp();

        /*
         * In case of ties, taking care not to locate center
         * in a side where there is no arc.
         * NB: For this using input angles, so counter clockwise
         * (matters for vertical case).
         */

        if (this.isFull) {
            // Don't care.
            this.cx = oval.xMid();
            this.cy = oval.yMid();
        } else {
            // Ties: if only one side with arc, choosing it.
            final double endDeg = plusZero360(startDeg, spanDeg);
            if (DEBUG) {
                Dbg.log("startDeg = " + startDeg);
                Dbg.log("endDeg = " + endDeg);
            }
            if (NbrsUtils.isOdd(oval.xSpan())) {
                // Exact.
                this.cx = oval.xMid();
            } else {
                final boolean haveArcEast;
                if (startDeg < endDeg) {
                    haveArcEast = (startDeg < 90.0) || (endDeg > 270.0);
                } else {
                    haveArcEast = true;
                }
                if (DEBUG) {
                    Dbg.log("haveArcEast = " + haveArcEast);
                }
                if (haveArcEast) {
                    this.cx = oval.xMid();
                } else {
                    this.cx = oval.xMid() - 1;
                }
            }
            if (NbrsUtils.isOdd(oval.ySpan())) {
                // Exact.
                this.cy = oval.yMid();
            } else {
                final boolean haveArcSouth;
                if (startDeg < endDeg) {
                    haveArcSouth = (endDeg > 180.0);
                } else {
                    haveArcSouth = true;
                }
                if (DEBUG) {
                    Dbg.log("haveArcSouth = " + haveArcSouth);
                }
                if (haveArcSouth) {
                    this.cy = oval.yMid();
                } else {
                    this.cy = oval.yMid() - 1;
                }
            }
        }

        // For cxd + rxd to be in middle of extreme pixel.
        this.rxd = (oval.xSpan() - 1) * 0.5;
        this.ryd = (oval.ySpan() - 1) * 0.5;

        this.rxdInv = 1.0 / this.rxd;
        this.rydInv = 1.0 / this.ryd;

        this.rxdOverRyd = (this.rxd * this.rydInv);

        // Angles in radians, and clockwise (unlike args),
        // which is handier for our coordinates.
        final double endDeg = startDeg + spanDeg;
        // In [0,2*PI].
        if (endDeg > 360.0) {
            this.arcStartRad = Math.toRadians((2 * 360.0) - endDeg);
        } else {
            this.arcStartRad = Math.toRadians(360.0 - endDeg);
        }
        // In [0,2*PI].
        this.arcSpanRad = Math.toRadians(spanDeg);
        // In [0,2*PI].
        this.arcEndRad = plusZeroTwoPi(this.arcStartRad, this.arcSpanRad);

        if (DEBUG) {
            Dbg.log("cxd = " + this.cxd);
            Dbg.log("cyd = " + this.cyd);
            Dbg.log("cx = " + this.cx);
            Dbg.log("cy = " + this.cy);
            Dbg.log("rxd = " + this.rxd);
            Dbg.log("ryd = " + this.ryd);
            Dbg.log("arcStartDeg = " + Math.toDegrees(this.arcStartRad));
            Dbg.log("arcEndRad =   " + Math.toDegrees(this.arcEndRad));
            Dbg.log("arcSpanRad =  " + Math.toDegrees(this.arcSpanRad));
        }
    }

    /*
     * Ecob data.
     */

    /**
     * [[n0, n1, n2],
     *  [n3, n4, n5],
     *  [n6, n7, n8]]
     * 
     * n4 = ecob.
     * 
     * n0 = center top-left of ecob.
     * n4 = center in ecob, or on its border.
     * etc.
     * 
     * @return ninth
     */
    private static int computeCenterNinth(
            GRect cob,
            double cxd,
            double cyd) {

        final double ecobGrowth = ECOB_GROWTH;
        final double ecobXMin = cob.x() - ecobGrowth;
        final double ecobYMin = cob.y() - ecobGrowth;
        final double ecobXMax = cob.xMax() + ecobGrowth;
        final double ecobYMax = cob.yMax() + ecobGrowth;

        final int horIndex = ((cxd < ecobXMin) ? 0 : ((cxd <= ecobXMax) ? 1 : 2));
        final int verIndex = ((cyd < ecobYMin) ? 0 : ((cyd <= ecobYMax) ? 1 : 2));

        final int ninth = horIndex + 3 * verIndex;
        if (DEBUG) {
            Dbg.log("ninth = " + ninth);
        }

        return ninth;
    }

    private void configureEcobData(GRect cob) {

        if (DEBUG) {
            Dbg.log("configureEcobData()");
        }

        final double ecobGrowth = H * 1.5;
        this.ecobXMin = cob.x() - ecobGrowth;
        this.ecobYMin = cob.y() - ecobGrowth;
        this.ecobXMax = cob.xMax() + ecobGrowth;
        this.ecobYMax = cob.yMax() + ecobGrowth;

        this.isCenterInEcob = this.isInEcob(this.cxd, this.cyd);

        final int ninth = computeCenterNinth(cob, this.cxd, this.cyd);
        this.isCenterInDiagNinth =
                (ninth == 0)
                || (ninth == 2)
                || (ninth == 6)
                || (ninth == 8);

        if (DEBUG) {
            Dbg.log("ecobXMin = " + this.ecobXMin);
            Dbg.log("ecobYMin = " + this.ecobYMin);
            Dbg.log("ecobXMax = " + this.ecobXMax);
            Dbg.log("ecobYMax = " + this.ecobYMax);
            Dbg.log("isCenterInEcob = " + this.isCenterInEcob);
            Dbg.log("isCenterInDiagNinth = " + this.isCenterInDiagNinth);
        }
    }

    private boolean isInEcob(double xd, double yd) {
        return (xd >= this.ecobXMin)
                && (xd <= this.ecobXMax)
                && (yd >= this.ecobYMin)
                && (yd <= this.ecobYMax);
    }

    /*
     * Base waypoints.
     */

    private void computeBaseWaypoints(
            GRect clip,
            GRect oval) {

        if (DEBUG) {
            Dbg.log("computeBaseWaypoints()");
        }

        final double cxd = this.cxd;
        final double cyd = this.cyd;

        final double rxd = this.rxd;
        final double ryd = this.ryd;

        final double arcStartRad = this.arcStartRad;
        final double arcEndRad = this.arcEndRad;

        final double ecobXMin = this.ecobXMin;
        final double ecobYMin = this.ecobYMin;
        final double ecobXMax = this.ecobXMax;
        final double ecobYMax = this.ecobYMax;

        this.baseWaypointCount = 0;

        /*
         * Arc start/end.
         */

        this.baseWaypointArr[this.baseWaypointCount++].configureStartEnd(
                MyWaypointType.ARC_START,
                arcStartRad);

        this.baseWaypointArr[this.baseWaypointCount++].configureStartEnd(
                MyWaypointType.ARC_END,
                arcEndRad);

        /*
         * Ecob corners.
         */

        for (int k = 0; k < 4; k++) {
            final double coordX = ((k >= 2) ? ecobXMax : ecobXMin);
            final double coordY = (NbrsUtils.isOdd(k) ? ecobYMax : ecobYMin);
            this.baseWaypointArr[this.baseWaypointCount++].configureCorner(
                    coordX,
                    coordY);
        }

        /*
         * Oval intersections with verticals.
         * 
         * left/right/top/bottom: for ecob sides.
         * west/east/north/south: for oval, relative to its center.
         */

        for (int verK = 0; verK < 2; verK++) {
            final boolean ver = (verK != 0);

            final double cd = (ver ? cyd : cxd);
            final double rd = (ver ? ryd : rxd);
            final double coordMin = (ver ? ecobYMin : ecobXMin);
            final double coordMax = (ver ? ecobYMax : ecobXMax);

            for (int hiSideK = 0; hiSideK < 2; hiSideK++) {
                final boolean hiSide = (hiSideK != 0);

                final int sideType;
                final double ortCoord;
                if (ver) {
                    sideType = (hiSide ? SIDE_RIGHT : SIDE_LEFT);
                    ortCoord = (hiSide ? ecobXMax : ecobXMin);
                } else {
                    sideType = (hiSide ? SIDE_BOTTOM : SIDE_TOP);
                    ortCoord = (hiSide ? ecobYMax : ecobYMin);
                }

                final double angRadHi;
                if (ver) {
                    // In [0,PI].
                    angRadHi = computeVerticalInterAngRad(ortCoord);
                } else {
                    // In {[0,PI/2],[3*PI/2,2*PI]}.
                    angRadHi = computeHorizontalInterAngRad(ortCoord);
                }

                if (!Double.isNaN(angRadHi)) {
                    final double angRadLo;
                    if (ver) {
                        // In [0,2*PI].
                        angRadLo = minusZeroTwoPi(0.0, angRadHi);
                    } else {
                        // In [PI/2,3*PI/2].
                        angRadLo = minusZeroTwoPi(Math.PI, angRadHi);
                    }

                    final double sinHi = Math.sin(angRadHi);
                    final double cosHi = Math.cos(angRadHi);

                    final double sinLo = (ver ? -sinHi : sinHi);
                    final double cosLo = (ver ? cosHi : -cosHi);

                    final double sinOrCosHi = (ver ? sinHi : cosHi);

                    final double coordDelta = rd * sinOrCosHi;
                    final double coordHi = cd + coordDelta;
                    final double coordLo = cd - coordDelta;

                    for (int hiCoordK = 0; hiCoordK < 2; hiCoordK++) {
                        final boolean hiCoord = (hiCoordK != 0);

                        final double coord = (hiCoord ? coordHi : coordLo);

                        if (NbrsUtils.isInRange(coordMin, coordMax, coord)) {
                            this.baseWaypointArr[this.baseWaypointCount++].configureInterWithOval(
                                    sideType,
                                    (hiCoord ? angRadHi : angRadLo),
                                    (hiCoord ? sinHi : sinLo),
                                    (hiCoord ? cosHi : cosLo),
                                    (ver ? ortCoord : coord),
                                    (ver ? coord : ortCoord));
                        }
                    }
                }
            }
        }

        /*
         * Sorting by increasing angle in [0,2*PI].
         * Small enough not to create garbage,
         * else we would have to reimplement sorting.
         */

        Arrays.sort(
                this.baseWaypointArr,
                0,
                this.baseWaypointCount,
                WAYPOINT_ANG_COMPARATOR);
    }

    private int nextBaseWpIndex(int wpIndex) {
        final int ret;
        if (wpIndex == this.baseWaypointCount - 1) {
            ret = 0;
        } else {
            ret = wpIndex + 1;
        }
        return ret;
    }

    /*
     * Base waypoints analysis.
     */

    /**
     * @return True if there might be something to draw, false otherwise. 
     */
    private boolean analyzeBaseWaypoints() {

        if (DEBUG) {
            Dbg.log("analyzeBaseWaypoints()");
        }

        MyWaypoint closestCornerWp = null;
        MyWaypoint farthestCornerWp = null;
        double closestDistSq = Double.NaN;
        double farthestDistSq = Double.NaN;
        for (int i = 0; i < this.baseWaypointCount; i++) {
            final MyWaypoint wp = this.baseWaypointArr[i];
            if (wp.type == MyWaypointType.ECOB_CORNER) {
                final double distSq =
                        NbrsUtils.pow2(wp.xd - this.cxd)
                        + NbrsUtils.pow2(wp.yd - this.cyd);
                if ((closestCornerWp == null)
                        || (distSq < closestDistSq)) {
                    closestCornerWp = wp;
                    closestDistSq = distSq;
                }
                if ((farthestCornerWp == null)
                        || (distSq > farthestDistSq)) {
                    farthestCornerWp = wp;
                    farthestDistSq = distSq;
                }
            }
        }
        if (DEBUG) {
            Dbg.log("closestCornerWp = " + closestCornerWp);
            Dbg.log("closestDist = " + Math.sqrt(closestDistSq));
            Dbg.log("farthestCornerWp = " + farthestCornerWp);
            Dbg.log("farthestDist = " + Math.sqrt(farthestDistSq));
        }
        this.closestCornerWp = closestCornerWp;
        this.farthestCornerWp = farthestCornerWp;

        final boolean isCenterInDiagNinth = this.isCenterInDiagNinth;

        /*
         * Quick nothing-to-draw checks,
         * in case we didn't do early checks for these cases.
         */

        // Works because corners of extended cob.
        if (isCenterInDiagNinth
                && !closestCornerWp.isInOval) {
            if (DEBUG) {
                Dbg.log("center in diag ninth, and no corner in oval: nothing to draw or fill");
            }
            return false;
        }

        // Works because corners of extended cob.
        if ((!this.isFillElseDraw)
                && farthestCornerWp.isInOval) {
            if (DEBUG) {
                Dbg.log("all corners in oval: nothing to draw");
            }
            return false;
        }

        /*
         * 
         */

        if (this.isCenterInEcob) {
            this.ecobStartCornerWp = null;
            this.ecobEndCornerWp = null;
            this.ecobStartRad = 0.0;
            this.ecobSpanRad = 2*Math.PI;
        } else {
            MyWaypoint ecobStartCornerWp = null;
            MyWaypoint ecobEndCornerWp = null;
            double ecobStartRad = Double.NaN;
            double ecobSpanRad = 0.0;
            for (int i = 0; i < this.baseWaypointCount; i++) {
                final MyWaypoint wp = this.baseWaypointArr[i];
                if (wp.type == MyWaypointType.ECOB_CORNER) {
                    if (DEBUG) {
                        Dbg.log("corner = " + wp);
                    }
                    final double angRad = wp.angRad;
                    if (Double.isNaN(ecobStartRad)) {
                        ecobStartCornerWp = wp;
                        ecobEndCornerWp = wp;
                        ecobStartRad = angRad;
                    } else {
                        final double deltaRad = minusMPiPi(angRad, ecobStartRad);
                        if (deltaRad < 0.0) {
                            ecobStartCornerWp = wp;
                            final double absDeltaRad = (-deltaRad);
                            ecobStartRad = angRad;
                            ecobSpanRad = plusZeroTwoPi(ecobSpanRad, absDeltaRad);
                        } else if (deltaRad > ecobSpanRad) {
                            ecobEndCornerWp = wp;
                            ecobSpanRad = deltaRad;
                        }
                    }
                }
            }
            this.ecobStartCornerWp = ecobStartCornerWp;
            this.ecobEndCornerWp = ecobEndCornerWp;
            this.ecobStartRad = ecobStartRad;
            this.ecobSpanRad = ecobSpanRad;
        }
        if (DEBUG) {
            Dbg.log("ecobStartCornerWp = " + this.ecobStartCornerWp);
            Dbg.log("ecobEndCornerWp = " + this.ecobEndCornerWp);
            Dbg.log("ecobStartDeg = " + Math.toDegrees(this.ecobStartRad));
            Dbg.log("ecobSpanDeg = " + Math.toDegrees(this.ecobSpanRad));
        }

        if (!this.isCenterInEcob) {
            /*
             * Overlap if the start of one is in the other.
             */
            final boolean doEcobAndArcAnglesOverlap =
                    isInRangeZeroTwoPi(this.arcStartRad, this.arcSpanRad, this.ecobStartRad)
                    || isInRangeZeroTwoPi(this.ecobStartRad, this.ecobSpanRad, this.arcStartRad);
            if (!doEcobAndArcAnglesOverlap) {
                if (DEBUG) {
                    Dbg.log("arc and ecob angles don't overlap: nothing to draw");
                }
                return false;
            }
        }

        return true;
    }

    /*
     * Kept waypoints.
     */

    private void clearKeptWaypoints() {
        this.keptWaypointCount = 0;
    }

    private void addKeptWaypoints(MyWaypoint wp) {
        this.keptWaypointArr[this.keptWaypointCount++] = wp;
    }

    private MyWaypoint getLastKeptWp() {
        return this.keptWaypointArr[this.keptWaypointCount - 1];
    }

    /**
     * @param cornerWp A corner waypoint.
     * @param wp Another waypoint.
     * @return True if some of the [cornerWp,wp] segment
     *         is strictly inside ecob, false otherwise.
     */
    private boolean computeIsWpAcrossEcob(
            MyWaypoint cornerWp,
            MyWaypoint wp) {
        final boolean isCornerLoX = (cornerWp.xd == this.ecobXMin);
        final boolean isCornerLoY = (cornerWp.yd == this.ecobYMin);
        final boolean ret;
        if (isCornerLoX) {
            if (isCornerLoY) {
                ret = (wp.xd > this.ecobXMin)
                        && (wp.yd > this.ecobYMin);
            } else {
                ret = (wp.xd > this.ecobXMin)
                        && (wp.yd < this.ecobYMax);
            }
        } else {
            if (isCornerLoY) {
                ret = (wp.xd < this.ecobXMax)
                        && (wp.yd > this.ecobYMin);
            } else {
                ret = (wp.xd < this.ecobXMax)
                        && (wp.yd < this.ecobYMax);
            }
        }
        return ret;
    }

    /**
     * Computes kept waypoints, from base waypoints
     * sorted by increasing angle.
     */
    private void computeKeptWaypoints() {

        if (DEBUG) {
            Dbg.log("computeKeptWaypoints()");
        }

        this.clearKeptWaypoints();

        /*
         * Arc start waypoint.
         */

        MyWaypoint arcStartWp = null;
        int startWpIndex = -1;
        for (int i = 0; i < this.baseWaypointCount; i++) {
            final MyWaypoint wp = this.baseWaypointArr[i];
            if (wp.type == MyWaypointType.ARC_START) {
                arcStartWp = wp;
                startWpIndex = i;
                break;
            }
        }
        if (DEBUG) {
            Dbg.log("wp = " + arcStartWp);
        }
        this.addKeptWaypoints(arcStartWp);

        /*
         * Other waypoints.
         */

        int wpIndex = this.nextBaseWpIndex(startWpIndex);
        while (true) {
            final MyWaypoint wp = this.baseWaypointArr[wpIndex];
            if (DEBUG) {
                Dbg.log("wp = " + wp);
            }

            final int nextWpIndex = this.nextBaseWpIndex(wpIndex);

            if ((wp.type == MyWaypointType.OVAL_INTER_ENTERING)
                    || (wp.type == MyWaypointType.OVAL_INTER_EXITING)) {
                this.addKeptWaypoints(wp);

            } else if (wp.type == MyWaypointType.ECOB_CORNER) {
                final MyWaypoint prevKeptWp = this.getLastKeptWp();
                final MyWaypoint nextWp = this.baseWaypointArr[nextWpIndex];
                final boolean mustKeepIt = this.mustKeepCornerWp(
                        wp,
                        prevKeptWp,
                        nextWp);
                if (mustKeepIt) {
                    this.addKeptWaypoints(wp);
                }

            } else if (wp.type == MyWaypointType.ARC_END) {
                this.addKeptWaypoints(wp);
                // Done.
                break;
            }

            wpIndex = nextWpIndex;
        }
    }

    private boolean mustKeepCornerWp(
            MyWaypoint wp,
            MyWaypoint prevKeptWp,
            MyWaypoint nextWp) {
        
        final boolean ret;
        if (prevKeptWp.mustDoSteps) {
            if (DEBUG) {
                Dbg.log("ignoring corner (doing steps)");
            }
            ret = false;
        } else {
            if (this.isCenterInEcob) {
                if (DEBUG) {
                    Dbg.log("keeping corner (center in ecob)");
                }
                ret = true;
            } else if (!wp.isInOval) {
                if (DEBUG) {
                    Dbg.log("ignoring corner (not in oval)");
                }
                ret = false;
            } else {
                if (this.isCenterInDiagNinth) {
                    if (wp == this.closestCornerWp) {
                        // Check on farthest just to speed things up.
                        final boolean isPrevKeptAcrossAndNotBigJump =
                                (prevKeptWp == this.farthestCornerWp)
                                || ((computeIsWpAcrossEcob(wp, prevKeptWp)
                                        && (minusZeroTwoPi(wp.angRad, prevKeptWp.angRad) <= Math.PI)));
                        if (isPrevKeptAcrossAndNotBigJump) {
                            if (DEBUG) {
                                Dbg.log("ignoring corner (prev kept is across ecob)");
                            }
                            ret = false;
                        } else {
                            final boolean isNextToBeKeptAndAcrossAndNotBigJump;
                            if (nextWp.type == MyWaypointType.ECOB_CORNER) {
                                isNextToBeKeptAndAcrossAndNotBigJump =
                                        nextWp.isInOval
                                        && (nextWp == this.farthestCornerWp);
                            } else {
                                isNextToBeKeptAndAcrossAndNotBigJump =
                                        computeIsWpAcrossEcob(wp, nextWp)
                                        && (minusZeroTwoPi(nextWp.angRad, wp.angRad) <= Math.PI);
                            }
                            if (isNextToBeKeptAndAcrossAndNotBigJump) {
                                if (DEBUG) {
                                    Dbg.log("ignoring corner (next to keep is across ecob)");
                                }
                                ret = false;
                            } else {
                                if (DEBUG) {
                                    Dbg.log("keeping corner (center in diag ninth, closest)");
                                }
                                ret = true;
                            }
                        }
                    } else {
                        if (DEBUG) {
                            Dbg.log("keeping corner (center in diag ninth, not closest)");
                        }
                        ret = true;
                    }
                } else {
                    if (DEBUG) {
                        Dbg.log("keeping corner (center past sides)");
                    }
                    ret = true;
                }
            }
        }
        return ret;
    }

    /*
     * Stepped iterations.
     */

    private void computeMaxStepRad() {
        /*
         * For "flat" ovals (with one large and one tiny radius),
         * the angular step corresponding to angles near extremities
         * can be huge, and if used blindly can cause some extreme pixels
         * to be skipped.
         * 
         * To guard against this, we ensure to never use a step
         * greater than the angle corresponding to 1/4th of a pixel away
         * from oval extremity (which is the middle of a pixel),
         * going along the longest axis.
         * 
         * Using oval formula as we do to compute intersections angles,
         * this constraint translates into:
         * maxStep = acos((maxRadius - 0.25) / maxRadius)
         */
        final double maxRadius = Math.max(this.rxd, this.ryd);
        final double maxRadiusInv = Math.min(this.rxdInv, this.rydInv);
        this.maxStepRad = Math.acos((maxRadius - 0.25) * maxRadiusInv);
    }

    /**
     * @param sinAng Sine of current point angle.
     * @param cosAng Cosine of current point angle.
     * @return Step to use to compute next point, in radians.
     */
    private double computeStepRad(double sinAng, double cosAng) {
        double stepRad = computeRawStepRad(this.rxd, this.ryd, sinAng, cosAng);
        if (DEBUG) {
            Dbg.log("stepDeg (1) = " + Math.toDegrees(stepRad));
        }
        stepRad *= PIXEL_STEP_PIXEL_RATIO;
        if (DEBUG) {
            Dbg.log("stepDeg (2) = " + Math.toDegrees(stepRad));
        }
        if (stepRad > this.maxStepRad) {
            stepRad = this.maxStepRad;
        }
        return stepRad;
    }

    /**
     * @return The step in radians to move for up to one pixel along each
     *         coordinate, using curvature at the specified position.
     */
    private static double computeRawStepRad(
            double rx,
            double ry,
            double sinAng,
            double cosAng) {
        /*
         * x = rx * cos(t)
         * y = ry * sin(t)
         * dx = -rx * sin(t) * dt
         * dy = ry * cos(t) * dt
         * 
         * We want max(|dx|, |dy|) < 1
         * (and not sqrt(dx^2 + dy^2) = 1,
         * which requires to compute a sqrt,
         * and doesn't seem preferable regardless).
         * 
         * rx^2 * sin(t)^2 * dt^2 < 1
         * ry^2 * cos(t)^2 * dt^2 < 1
         * dt < 1 / |rx * sin(t)|
         * dt < 1 / |ry * cos(t)|
         * dt < 1 / max(rx * |sin(t)|, ry * |cos(t)|)
         */
        final double sinAbs = Math.abs(sinAng);
        final double cosAbs = Math.abs(cosAng);
        return 1.0 / Math.max(rx * sinAbs, ry * cosAbs);
    }

    /*
     * Coordinates rounding.
     */

    private static int roundArcCoord_x(double centerCoord, double sin, double cos, double coord) {
        final int coordI;
        if ((coord == centerCoord)
                && NbrsUtils.isEquidistant(coord)) {
            coordI = roundArcCoord_x_ties(sin, cos, coord);
        } else {
            coordI = BindingCoordsUtils.roundToInt(coord);
        }
        return coordI;
    }

    private static int roundArcCoord_y(double centerCoord, double sin, double cos, double coord) {
        final int coordI;
        if ((coord == centerCoord)
                && NbrsUtils.isEquidistant(coord)) {
            coordI = roundArcCoord_y_ties(sin, cos, coord);
        } else {
            coordI = BindingCoordsUtils.roundToInt(coord);
        }
        return coordI;
    }

    private static int roundArcCoord_x_ties(double sin, double cos, double coord) {
        final int coordI;
        if (cos > 0.0) {
            coordI = (int) Math.ceil(coord);
        } else if (cos < 0.0) {
            coordI = (int) Math.floor(coord);
        } else {
            if (sin > 0.0) {
                // Going left.
                coordI = (int) Math.floor(coord);
            } else {
                // Going right.
                coordI = (int) Math.ceil(coord);
            }
        }
        return coordI;
    }

    private static int roundArcCoord_y_ties(double sin, double cos, double coord) {
        final int coordI;
        if (sin > 0.0) {
            coordI = (int) Math.ceil(coord);
        } else if (sin < 0.0) {
            coordI = (int) Math.floor(coord);
        } else {
            if (cos > 0.0) {
                // Going down.
                coordI = (int) Math.ceil(coord);
            } else {
                // Going up.
                coordI = (int) Math.floor(coord);
            }
        }
        return coordI;
    }

    /*
     * Intersections.
     */

    /**
     * Oval intersection(s) with a vertical:
     * sx = cxd + rxd * cos(ang)
     * cos(ang) = (sx - cxd) / rxd
     * term = (sx - cxd) / rxd
     * ang = acos(term)
     * - intersection : if term in [-1,1]
     * - only one : if term is -1 or 1
     * - two : ang = +-acos(term) (which is in [0,PI])
     * 
     * Other angle is minus this one.
     * 
     * @return acos((sx - cxd) / rxd), which is in [0,PI].
     */
    private double computeVerticalInterAngRad(double sx) {
        final double term = (sx - this.cxd) * this.rxdInv;
        final double ret;
        if ((term >= -1.0) && (term <= 1.0)) {
            ret = Math.acos(term);
        } else {
            // Avoiding eventual Math.acos() slowness.
            ret = Double.NaN;
        }
        return ret;
    }

    /**
     * Oval intersection(s) with an horizontal:
     * sy = cyd + ryd * sin(ang)
     * sin(ang) = (sy - cyd) / ryd
     * term = (sy - cyd) / ryd
     * ang = asin(term)
     * - intersection : if term in [-1,1]
     * - only one : if term is -1 or 1
     * - two : ang = asin(term) (which is in [-PI/2,PI/2]) and PI - asin(term).
     * 
     * Other angle is (PI - this one).
     * 
     * @return asin((sy - cyd) / ryd), taken back into {[0,PI/2],[3*PI/2,2*PI]} range.
     */
    private double computeHorizontalInterAngRad(double sy) {
        final double term = (sy - this.cyd) * this.rydInv;
        final double ret;
        if ((term >= -1.0) && (term <= 1.0)) {
            final double angMpio2Pio2 = Math.asin(term);
            if (angMpio2Pio2 < 0.0) {
                ret = Math.PI - angMpio2Pio2;
            } else {
                ret = angMpio2Pio2;
            }
        } else {
            // Avoiding eventual Math.asin() slowness.
            ret = Double.NaN;
        }
        return ret;
    }

    /**
     * @param angRad In [0,2*PI].
     */
    private static MyWaypointType computeInterType(
            int sideType,
            double angRad) {
        final boolean isEntering = computeIsEnteringElseExiting(
                sideType,
                angRad);
        final MyWaypointType ret;
        if (isEntering) {
            ret = MyWaypointType.OVAL_INTER_ENTERING;
        } else {
            ret = MyWaypointType.OVAL_INTER_EXITING;
        }
        return ret;
    }

    /**
     * For ties, must use ENTERING, which translates
     * into accurate arc computation.
     */
    private static boolean computeIsEnteringElseExiting(
            int sideType,
            double angRad) {
        final boolean ret;
        switch (sideType) {
            case SIDE_LEFT: ret = (angRad >= Math.PI); break;
            case SIDE_RIGHT: ret = (angRad <= Math.PI); break;
            case SIDE_TOP: ret = (angRad <= Math.PI/2) || (angRad >= 3*Math.PI/2); break;
            case SIDE_BOTTOM: ret = (angRad >= Math.PI/2) && (angRad <= 3*Math.PI/2); break;
            default: throw new AssertionError();
        }
        return ret;
    }

    /*
     * Degree angles utils.
     */

    /**
     * @param ang1Deg Must be in [0,360].
     * @param ang2Deg Must be in [0,360].
     * @return ang1Deg + ang2Deg, in [0,360].
     */
    private static double plusZero360(double ang1Deg, double ang2Deg) {
        double sumDeg = (ang1Deg + ang2Deg);
        if (sumDeg > 360.0) {
            sumDeg -= 360.0;
        }
        return sumDeg;
    }

    /**
     * @param ang1Deg Must be in [0,360].
     * @param ang2Deg Must be in [0,360].
     * @return ang1Deg - ang2Deg, in [0,360].
     */
    private static double minusZero360(double ang1Deg, double ang2Deg) {
        double sumDeg = (ang1Deg - ang2Deg);
        if (sumDeg < 0.0) {
            sumDeg += 360.0;
        }
        return sumDeg;
    }

    /*
     * Radian angles utils.
     */

    /**
     * @param ang1Rad Must be in [0,2*PI].
     * @param ang2Rad Must be in [0,2*PI].
     * @return ang1Rad + ang2Rad, in [0,2*PI].
     */
    private static double plusZeroTwoPi(double ang1Rad, double ang2Rad) {
        double sumRad = (ang1Rad + ang2Rad);
        if (sumRad > 2*Math.PI) {
            sumRad -= 2*Math.PI;
        }
        return sumRad;
    }

    /**
     * @param ang1Rad Must be in [0,2*PI].
     * @param ang2Rad Must be in [0,2*PI].
     * @return ang1Rad - ang2Rad, in [0,2*PI].
     */
    private static double minusZeroTwoPi(double ang1Rad, double ang2Rad) {
        double diffRad = (ang1Rad - ang2Rad);
        if (diffRad < 0.0) {
            diffRad += 2*Math.PI;
        }
        return diffRad;
    }

    /**
     * @param ang1Rad Must be in [0,2*PI].
     * @param ang2Rad Must be in [0,2*PI].
     * @return ang1Rad - ang2Rad, in [-PI,PI].
     */
    private static double minusMPiPi(double ang1Rad, double ang2Rad) {
        double diffRad = (ang1Rad - ang2Rad);
        if (diffRad < -Math.PI) {
            diffRad += 2*Math.PI;
        } else if (diffRad > Math.PI) {
            diffRad -= 2*Math.PI;
        }
        return diffRad;
    }

    /**
     * @param startRad Must be in [0,2*PI].
     * @param spanRad Must be in [0,2*PI].
     * @param angRad Must be in [0,2*PI].
     * @return True if angRad is in [startRad, startRad + spanRad] angular range.
     */
    private static boolean isInRangeZeroTwoPi(double startRad, double spanRad, double angRad) {
        final double deltaRad = minusZeroTwoPi(angRad, startRad);
        return (deltaRad <= spanRad);
    }

    /**
     * @return atan2 in [0,2*PI].
     */
    private static double atan2ZeroTwoPi(double y, double x) {
        double ret = Math.atan2(y, x);
        if (ret < 0.0) {
            ret += 2*Math.PI;
        }
        return ret;
    }
}
