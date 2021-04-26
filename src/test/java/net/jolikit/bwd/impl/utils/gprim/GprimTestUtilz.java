/*
 * Copyright 2019-2021 Jeff Hain
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

import java.util.Map;
import java.util.Random;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.NbrsTestUtils;

class GprimTestUtilz {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Random random;
    
    private final NbrsTestUtils nbrUtils;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public GprimTestUtilz(Random random) {
        this.random = random;
        this.nbrUtils = new NbrsTestUtils(random);
    }
    
    /**
     * @return A random clip nearby the specified rectangle,
     *         possibly overlapping it.
     */
    public GRect randomClipNearRect(GRect rect) {
        final GRect clip;
        // Nearby bounding box, possibly overlapping it.
        if (rect.isEmpty()) {
            // Shouldn't hurt.
            clip = GRect.DEFAULT_HUGE;
        } else {
            clip = this.newClipIn(
                    NbrsUtils.toInt(rect.x() - (long) rect.xSpan()),
                    NbrsUtils.toInt(rect.y() - (long) rect.ySpan()),
                    NbrsUtils.toInt(rect.xMax() + (long) rect.xSpan()),
                    NbrsUtils.toInt(rect.yMax() + (long) rect.ySpan()));
        }
        return clip;
    }
    
    /**
     * @return A random clip included in the specified bounding box.
     */
    public GRect newClipIn(
            int minX,
            int minY,
            int maxX,
            int maxY) {
        final int clipX = this.nbrUtils.randomIntUniform(minX, maxX);
        final int clipY = this.nbrUtils.randomIntUniform(minY, maxY);
        final int maxClipXSpan = (int) (maxX - (long) clipX + 1);
        final int maxClipYSpan = (int) (maxY - (long) clipY + 1);
        // Allowing empty spans.
        final int clipXSpan = this.random.nextInt(maxClipXSpan);
        final int clipYSpan = this.random.nextInt(maxClipYSpan);
        final GRect clip = GRect.valueOf(
                clipX,
                clipY,
                clipXSpan,
                clipYSpan);
        return clip;
    }
    
    /*
     * 
     */
    
    public static boolean randomBoolean(Random random, double proba) {
        if (proba >= 1.0) {
            return true;
        }
        if (proba <= 0.0) {
            return false;
        }
        return (random.nextDouble() < proba);
    }
    
    /**
     * @return Random value in [-1.0,1.0[
     */
    public static double randomMinusOneOne(Random random) {
        return (2.0 * random.nextDouble() - 1.0);
    }
    
    /**
     * @return Random value in [-value,value]
     */
    public static int randomMinusIntInt(Random random, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("" + value);
        }
        return (int) Math.rint(value * randomMinusOneOne(random));
    }
    
    public static boolean computeFoundPixelOutOfClippedBBox(
            boolean debug,
            GRect clippedBBox,
            Map<GPoint,Integer> paintedCountByPixel) {
        boolean foundPixelOutOfClippedBBox = false;
        for (GPoint pixel : paintedCountByPixel.keySet()) {
            if (!clippedBBox.contains(pixel)) {
                if (debug) {
                    Dbg.log("pixel out of clipped bounding box : " + pixel + " out of " + clippedBBox);
                }
                foundPixelOutOfClippedBBox = true;
            }
        }
        return foundPixelOutOfClippedBBox;
    }
    
    /**
     * @return {foundMissingPixel, foundExceedingPixel, foundMultipaintedPixel}
     */
    public static boolean[] computePixelPaintingIssues(
            boolean debug,
            Map<GPoint,TestPixelStatus> statusByPixel,
            boolean isFillElseDraw) {
        final boolean[] resArr = new boolean[3];
        for (Map.Entry<GPoint,TestPixelStatus> entry : statusByPixel.entrySet()) {
            final GPoint pixel = entry.getKey();
            final TestPixelStatus status = entry.getValue();
            
            if (status == TestPixelStatus.REQUIRED_NOT_PAINTED) {
                // Missing.
                resArr[0] = true;
                if (debug) {
                    Dbg.log("pixel wrongly NOT " + (isFillElseDraw ? "filled" : "drawn") + " : " + pixel);
                }
            } else if ((status == TestPixelStatus.OUT_OF_CLIP_PAINTED_ONCE)
                    || (status == TestPixelStatus.OUT_OF_CLIP_PAINTED_MULTIPLE_TIMES)
                    || (status == TestPixelStatus.NOT_ALLOWED_PAINTED_ONCE)
                    || (status == TestPixelStatus.NOT_ALLOWED_PAINTED_MULTIPLE_TIMES)) {
                // Exceeding.
                resArr[1] = true;
                if (debug) {
                    Dbg.log("pixel wrongly " + (isFillElseDraw ? "filled" : "drawn") + " : " + pixel + " (" + status +")");
                }
            } else if ((status == TestPixelStatus.ALLOWED_PAINTED_MULTIPLE_TIMES)
                    || (status == TestPixelStatus.REQUIRED_PAINTED_MULTIPLE_TIMES)) {
                // Multipainted.
                resArr[2] = true;
                if (debug) {
                    Dbg.log("pixel " + (isFillElseDraw ? "filled" : "drawn") + " multiple times : " + pixel + " (" + status +")");
                }
            }
        }
        return resArr;
    }
    
    /**
     * @param paintCount Can be null.
     */
    public static TestPixelStatus computePixelStatus(
            Integer paintCount,
            boolean pixelInClip,
            PixelFigStatus pixelFigStatus) {
        
        final boolean pixelPaintingRequired =
                (pixelFigStatus == PixelFigStatus.PIXEL_REQUIRED);
        final boolean pixelPaintingAllowed =
                pixelPaintingRequired
                || (pixelFigStatus == PixelFigStatus.PIXEL_ALLOWED);
        
        final TestPixelStatus status;
        
        if (!pixelInClip) {
            if (paintCount == null) {
                status = TestPixelStatus.OUT_OF_CLIP_NOT_PAINTED;
            } else if (paintCount.intValue() == 1) {
                status = TestPixelStatus.OUT_OF_CLIP_PAINTED_ONCE;
            } else {
                status = TestPixelStatus.OUT_OF_CLIP_PAINTED_MULTIPLE_TIMES;
            }
        } else if (pixelPaintingAllowed) {
            if (pixelPaintingRequired) {
                if (paintCount == null) {
                    status = TestPixelStatus.REQUIRED_NOT_PAINTED;
                } else if (paintCount.intValue() == 1) {
                    status = TestPixelStatus.REQUIRED_PAINTED_ONCE;
                } else {
                    status = TestPixelStatus.REQUIRED_PAINTED_MULTIPLE_TIMES;
                }
            } else {
                if (paintCount == null) {
                    status = TestPixelStatus.ALLOWED_NOT_PAINTED;
                } else if (paintCount.intValue() == 1) {
                    status = TestPixelStatus.ALLOWED_PAINTED_ONCE;
                } else {
                    status = TestPixelStatus.ALLOWED_PAINTED_MULTIPLE_TIMES;
                }
            }
        } else {
            if (paintCount == null) {
                status = TestPixelStatus.NOT_ALLOWED_NOT_PAINTED;
            } else if (paintCount.intValue() == 1) {
                status = TestPixelStatus.NOT_ALLOWED_PAINTED_ONCE;
            } else {
                status = TestPixelStatus.NOT_ALLOWED_PAINTED_MULTIPLE_TIMES;
            }
        }
        
        return status;
    }
    
    /**
     * @param statusByPixel Supposed to contain status for
     *        all the pixels of rectangle.
     */
    public static void logStatusByPixel(Map<GPoint,TestPixelStatus> statusByPixel) {
        
        Dbg.log("[- = out of clip, ./: = out of clip and painted once/multiple times]");
        Dbg.log("[e/E = exceeding, m = missing, x/X = required painted, y/Y = allowed painted,");
        Dbg.log(" z = allowed not painted, space = not allowed not painted, caps = multiple times]");
        Dbg.log("---");
        
        final StringBuilder sb = new StringBuilder();
        sb.append("|");
        
        int prevY = Integer.MIN_VALUE;
        
        for (Map.Entry<GPoint,TestPixelStatus> entry : statusByPixel.entrySet()) {
            final GPoint pixel = entry.getKey();
            
            final boolean isNewLine = (pixel.y() > prevY);
            if (isNewLine) {
                if (sb.length() > 1) {
                    sb.append("|");
                    Dbg.log(sb.toString());
                    sb.setLength(0);
                    sb.append("|");
                }
            }
            prevY = pixel.y();
            
            final TestPixelStatus status = entry.getValue();
            
            final char c = convertToChar(status);
            // Proportions closer to 1:1 with added space.
            sb.append(' ');
            sb.append(c);
        }
        
        if (sb.length() > 1) {
            sb.append("|");
            Dbg.log(sb.toString());
        }
        
        Dbg.log("---");
    }
    
    /*
     * 
     */
    
    /**
     * @return The number of surrounding eights, above of the specified line
     *         if it's rather horizontal, or left of it if it's rather vertical.
     */
    public static int computeNbrOfSurroundingEightsAboveElseLeftOfLine(
            double x1d,
            double y1d,
            double x2d,
            double y2d,
            GPoint pixel) {
        
        final boolean horizontal = (y1d == y2d);
        final boolean vertical = (x1d == x2d);
        if (horizontal && vertical) {
            // Means the segment is a point.
            return 0;
        }
        
        /*
         * Line equation:
         * y = a * x + b
         * or
         * x = c
         */
        final double a = GprimUtils.computeLineA(x1d, y1d, x2d, y2d);
        final double b = GprimUtils.computeLineB(x1d, y1d, x2d, y2d, a);
        final double c = GprimUtils.computeLineC(x1d, x2d);
        
        int nbrOfPointsIn = 0;
        
        final double[] xyArr = GprimUtils.computeSurroundingEightsArr(pixel.x(), pixel.y(), null);
        for (int i = 0; i < xyArr.length; i += 2) {
            final double pointX = xyArr[i];
            final double pointY = xyArr[i+1];
            
            final boolean pointIsIn = isPointAboveElseLeftOfLine(
                    a, b, c,
                    pointX, pointY);
            
            if (DEBUG) {
                final String inOut = (pointIsIn ? "IN" : "OUT");
                Dbg.log(inOut + " : point = (" + pointX + ", " + pointY + ")"
                        + ", pixel = " + pixel
                        + ", line = " + x1d + ", " + y1d + ", " + x2d + ", " + y2d + ")");
            }
            
            if (pointIsIn) {
                nbrOfPointsIn++;
            }
        }
        
        return nbrOfPointsIn;
    }
    
    /*
     * 
     */
    
    public static double dist(double dx, double dy) {
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Line equation:
     * y = a * x + b
     * or, for vertical lines:
     * x = c
     * 
     * Ties not defined (subject to approximation errors anyway).
     * 
     * @param b For non-vertical lines.
     * @param c For vertical lines.
     * @return True if point is above rather horizontal lines,
     *         or if point is left of rather vertical lines,
     *         false otherwise.
     */
    public static boolean isPointAboveElseLeftOfLine(
            double a,
            double b,
            double c,
            double xd,
            double yd) {

        final boolean horizontal = (a == 0.0);
        final boolean vertical = Double.isInfinite(a);

        final boolean result;
        if (horizontal) {
            final double yOnLine = b;
            result = (yd < yOnLine);
        } else if (vertical) {
            final double xOnLine = c;
            result = (xd < xOnLine);
        } else if (Math.abs(a) < 1.0) {
            // |a| small, line closer to horizontal.
            final double yOnLine = a * xd + b;
            result = (yd < yOnLine);
        } else {
            // |a| large, line closer to vertical.
            // x = (y - b) / a
            final double xOnLine = (yd - b) / a;
            result = (xd < xOnLine);
        }
        
        return result;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static char convertToChar(TestPixelStatus status) {
        final char c;
        if (status == TestPixelStatus.OUT_OF_CLIP_NOT_PAINTED) {
            c = '-';
        } else if (status == TestPixelStatus.OUT_OF_CLIP_PAINTED_ONCE) {
            c = '.';
        } else if (status == TestPixelStatus.OUT_OF_CLIP_PAINTED_MULTIPLE_TIMES) {
            c = ':';
        } else if (status == TestPixelStatus.NOT_ALLOWED_NOT_PAINTED) {
            c = ' ';
        } else if (status == TestPixelStatus.NOT_ALLOWED_PAINTED_ONCE) {
            c = 'e';
        } else if (status == TestPixelStatus.NOT_ALLOWED_PAINTED_MULTIPLE_TIMES) {
            c = 'E';
        } else if (status == TestPixelStatus.ALLOWED_NOT_PAINTED) {
            c = 'z';
        } else if (status == TestPixelStatus.ALLOWED_PAINTED_ONCE) {
            c = 'y';
        } else if (status == TestPixelStatus.ALLOWED_PAINTED_MULTIPLE_TIMES) {
            c = 'Y';
        } else if (status == TestPixelStatus.REQUIRED_NOT_PAINTED) {
            c = 'm';
        } else if (status == TestPixelStatus.REQUIRED_PAINTED_ONCE) {
            c = 'x';
        } else if (status == TestPixelStatus.REQUIRED_PAINTED_MULTIPLE_TIMES) {
            c = 'X';
        } else {
            throw new AssertionError("" + status);
        }
        return c;
    }
}
