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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.Random;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;

/**
 * Mock using all painting methods (primitives, text, images...),
 * along with transforms.
 */
public class DrawingMethodsBwdPainter {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static GTransform[] LOCAL_TRANSFORM_BY_QUADRANT = new GTransform[]{
        GTransform.valueOf(GRotation.ROT_0, 0, 0),
        GTransform.valueOf(GRotation.ROT_90, 0, 0),
        GTransform.valueOf(GRotation.ROT_180, 0, 0),
        GTransform.valueOf(GRotation.ROT_270, 0, 0),
    };
    
    /**
     * Transforms used translate to the center of each cell.
     * We step 1 pixel away along X and Y axes, in user space,
     * not to overlap with the cross.
     * This step also allows to check that the x and y specified
     * to painting methods in user space are not always zero,
     * and are properly taken into account.
     */
    private static int LOCAL_OFFSET = 1;
    
    /*
     * 
     */
    
    private static final int NBR_OF_COLUMNS = 8;
    private static final int NBR_OF_ROWS = 8;
    private static final int NBR_OF_CELLS = NBR_OF_ROWS * NBR_OF_COLUMNS;
    
    /**
     * Each cell, along both X and Y axes, is divided into 4 parts
     * of same length, for easy check of rotated non-square figures.
     */
    private static final int CELL_QUARTER_INNER_SPAN = 21;
    private static final int CELL_HALF_INNER_SPAN = 2 * CELL_QUARTER_INNER_SPAN + 1;
    private static final int CELL_INNER_SPAN = 2 * CELL_HALF_INNER_SPAN + 1;

    /**
     * -1 so that line height should be CELL_QUARTER_INNER_SPAN.
     */
    private static final int TARGET_FONT_HEIGHT = CELL_QUARTER_INNER_SPAN - 1;

    private static final int AREA_X_SPAN = NBR_OF_COLUMNS * (CELL_INNER_SPAN + 1) + 1;
    private static final int AREA_Y_SPAN = NBR_OF_ROWS * (CELL_INNER_SPAN + 1) + 1;
    
    private static final int INITIAL_WIDTH = DrawingMethodsBwdPainter.AREA_X_SPAN + 28;
    private static final int INITIAL_HEIGHT = DrawingMethodsBwdPainter.AREA_Y_SPAN + 60;
    public static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    /*
     * 
     */
    
    private final BwdColor COLOR_BG = BwdColor.BLACK;
    private final BwdColor COLOR_CELLS = BwdColor.WHITE;
    private final BwdColor COLOR_FG = BwdColor.GRAY;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBwdBinding binding;
    
    private InterfaceBwdFont font;
    
    private InterfaceBwdImage image_filled_grey;
    private InterfaceBwdImage image_struct_grey;
    private InterfaceBwdImage image_struct_color;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DrawingMethodsBwdPainter(InterfaceBwdBinding binding) {
        this.binding = LangUtils.requireNonNull(binding);
        this.image_filled_grey = binding.newImage(BwdTestResources.TEST_IMG_FILE_PATH_FILLED_GREY_PNG);
        this.image_struct_grey = binding.newImage(BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_GREY_PNG);
        this.image_struct_color = binding.newImage(BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG);
    }
    
    public void paint(InterfaceBwdGraphics g) {
        
        paintCells(g);
        
        //@SuppressWarnings("unused")
        int cellIndex = 0;
        
        /*
         * Clearing.
         */
        
        clearRects(g, cellIndex++);
        
        /*
         * Geometry.
         */
        
        drawPoints(g, cellIndex++);
        
        drawLines(g, cellIndex++);
        drawVariousLineTypes(g, cellIndex++);
        
        drawLineStipples(g, cellIndex++);
        
        drawRects(g, cellIndex++);
        drawVariousRectTypes(g, cellIndex++);
        fillRects(g, cellIndex++);
        fillVariousRectTypes(g, cellIndex++);

        drawCircles(g, cellIndex++, true, true);
        drawCircles(g, cellIndex++, false, false);
        drawCircles(g, cellIndex++, false, true);
        drawCircles(g, cellIndex++, true, false);
        fillCircles(g, cellIndex++);
        
        drawOvals(g, cellIndex++);
        fillOvals(g, cellIndex++);
        
        /*
         * NB: Having as many draw/fillArcs as columns
         * helps compare them against each other.
         */
        
        // Span < 180 deg.
        drawArcs(g, CELL_HALF_INNER_SPAN, CELL_QUARTER_INNER_SPAN, 90.0, 135.0, cellIndex++);
        drawArcs(g, CELL_HALF_INNER_SPAN, CELL_QUARTER_INNER_SPAN, 45.0, 315.0, cellIndex++);
        for (int ySpan : new int[]{3,2,1}) {
            drawArcs(g, CELL_HALF_INNER_SPAN, ySpan, 45.0, 225.0, cellIndex++);
            // Must only draw the part left of center.
            drawArcs(g, CELL_HALF_INNER_SPAN, ySpan, 90.0, 180.0, cellIndex++);
        }
        // Span < 180 deg.
        fillArcs(g, CELL_HALF_INNER_SPAN, CELL_QUARTER_INNER_SPAN, 90.0, 135.0, cellIndex++);
        fillArcs(g, CELL_HALF_INNER_SPAN, CELL_QUARTER_INNER_SPAN, 45.0, 315.0, cellIndex++);
        for (int ySpan : new int[]{3,2,1}) {
            fillArcs(g, CELL_HALF_INNER_SPAN, ySpan, 45.0, 225.0, cellIndex++);
            // Must only draw the part left of center.
            fillArcs(g, CELL_HALF_INNER_SPAN, ySpan, 90.0, 180.0, cellIndex++);
        }
        
        drawPolylines(g, cellIndex++, 100, 3.0);
        drawPolylines(g, cellIndex++, 50, 2.0);
        drawPolygons(g, cellIndex++, 26, 1.0);
        // Crossing.
        drawPolygons(g, cellIndex++, 20, 3.0);
        //
        fillPolygons(g, cellIndex++, 100, 3.0);
        fillPolygons(g, cellIndex++, 50, 2.0);
        fillPolygons(g, cellIndex++, 26, 1.0);
        // Crossing.
        fillPolygons(g, cellIndex++, 20, 3.0);
        
        /*
         * String.
         */
        
        drawTexts(g, cellIndex++);
        
        drawStringInBoxRotated(g, cellIndex++);
        
        /*
         * Clipping.
         */
        
        drawClipped(g, cellIndex++);

        /*
         * Colors flipping.
         */
        
        flipColorsColoredLines(g, cellIndex++);
        
        flipColorsFilledRectHalf(g, cellIndex++);
        
        /*
         * Non-colored images.
         */
        
        drawImages(g, cellIndex++);
        
        {
            final InterfaceBwdImage image = this.image_struct_grey;
            // Checking that draws properly when using image spans.
            drawImages_adjusted_1(g, image.getWidth(), image.getHeight(), image, cellIndex++);
            drawImages_adjusted_1(g, CELL_HALF_INNER_SPAN, CELL_HALF_INNER_SPAN, image, cellIndex++);
            drawImages_adjusted_1(g, CELL_QUARTER_INNER_SPAN, CELL_QUARTER_INNER_SPAN, image, cellIndex++);
        }
        {
            final InterfaceBwdImage image = this.image_filled_grey;
            drawImages_adjusted_1(g, 10, 10, image, cellIndex++);
            drawImages_adjusted_1(g, 2, 2, image, cellIndex++);
        }
        
        {
            final InterfaceBwdImage image = this.image_struct_grey;
            // Checking that draws properly when using image spans.
            drawImages_adjusted_2(g, image.getWidth(), image.getHeight(), image, cellIndex++);
            drawImages_adjusted_2(g, CELL_HALF_INNER_SPAN, CELL_HALF_INNER_SPAN, image, cellIndex++);
            drawImages_adjusted_2(g, CELL_QUARTER_INNER_SPAN, CELL_QUARTER_INNER_SPAN, image, cellIndex++);
        }
        {
            final InterfaceBwdImage image = this.image_filled_grey;
            drawImages_adjusted_2(g, 10, 10, image, cellIndex++);
            drawImages_adjusted_2(g, 2, 2, image, cellIndex++);
        }
        
        /*
         * Colored images.
         */
        
        drawImagesColoredLinesReworked(g, cellIndex++);
        
        drawImagesColoredXor(g, cellIndex++);
        drawImagesColoredXor_adjusted_1(g, cellIndex++);
        drawImagesColoredXor_adjusted_2(g, cellIndex++);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static BwdColor randomColor(Random random) {
        final double red = random.nextDouble();
        final double green = random.nextDouble();
        final double blue = random.nextDouble();
        return BwdColor.valueOfRgbFp(red, green, blue);
    }

    private static BwdColor invertedColor(BwdColor color) {
        return BwdColor.valueOfArgb64(Argb64.inverted(color.toArgb64()));
    }
    
    /*
     * 
     */
    
    private void resetColor(InterfaceBwdGraphics g) {
        g.setColor(COLOR_FG);
    }
    
    /*
     * 
     */
    
    private static int cellCenterX(InterfaceBwdGraphics g, int cellIndex) {
        final int column = cellIndex % NBR_OF_COLUMNS;
        return g.getBox().x() + (CELL_INNER_SPAN + 1)/2 + column * (CELL_INNER_SPAN + 1);
    }
    
    private static int cellCenterY(InterfaceBwdGraphics g, int cellIndex) {
        final int row = cellIndex / NBR_OF_COLUMNS;
        return g.getBox().y() + (CELL_INNER_SPAN + 1)/2 + row * (CELL_INNER_SPAN + 1);
    }
    
    /**
     * @param x0 X of center of cell.
     * @param y0 Y of center of cell.
     * @return Transforms for painting in each quadrant.
     */
    private static GTransform[] getQuadrantTransforms(int x0, int y0) {
        final GTransform[] arr = new GTransform[LOCAL_TRANSFORM_BY_QUADRANT.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = GTransform.valueOf(0, x0, y0).composed(LOCAL_TRANSFORM_BY_QUADRANT[i]);
        }
        return arr;
    }
    
    /*
     * 
     */
    
    private void paintCells(InterfaceBwdGraphics g) {
        final GRect box = g.getBox();
        final int x = box.x();
        final int y = box.y();
        final int xSpan = AREA_X_SPAN;
        final int ySpan = AREA_Y_SPAN;
        
        // Background.
        g.setColor(COLOR_BG);
        g.fillRect(x, y, xSpan, ySpan);
        
        // Cells.
        for (int i = 0; i < NBR_OF_CELLS; i++) {
            final int xMid = cellCenterX(g, i);
            final int yMid = cellCenterY(g, i);
            paintCellStructure(g, xMid, yMid);
        }
    }

    private void paintCellStructure(InterfaceBwdGraphics g, int xMid, int yMid) {
        // Cells.
        g.setColor(COLOR_CELLS);
        
        final int xMin = xMid - (CELL_HALF_INNER_SPAN + 1);
        final int xMax = xMid + (CELL_HALF_INNER_SPAN + 1);
        final int yMin = yMid - (CELL_HALF_INNER_SPAN + 1);
        final int yMax = yMid + (CELL_HALF_INNER_SPAN + 1);
        
        {
            // Not full line, to see more easily
            // where painting to test ends.
            final int span = CELL_QUARTER_INNER_SPAN/2;
            
            // Cell shape horizontals.
            for (int _y : new int[]{yMin, yMax}) {
                g.drawLine(xMin, _y, xMin + span, _y);
                g.drawLine(xMid - span, _y, xMid + span, _y);
                g.drawLine(xMax - span, _y, xMax, _y);
            }
            
            // Cells shapes verticals.
            for (int _x : new int[]{xMin, xMax}) {
                g.drawLine(_x, yMin, _x, yMin + span);
                g.drawLine(_x, yMid - span, _x, yMid + span);
                g.drawLine(_x, yMax - span, _x, yMax);
            }
        }
        
        // Cell crosses.
        {
            paintCross(g, xMid, yMid);
            final int delta = (CELL_QUARTER_INNER_SPAN+1);
            paintCross(g, xMid - delta, yMid - delta);
            paintCross(g, xMid - delta, yMid + delta);
            paintCross(g, xMid + delta, yMid - delta);
            paintCross(g, xMid + delta, yMid + delta);
        }
    }

    private void paintCross(InterfaceBwdGraphics g, int x0, int y0) {
        final int halfish = CELL_QUARTER_INNER_SPAN/2;
        g.drawLine(x0, y0 - halfish, x0, y0 + halfish);
        g.drawLine(x0 - halfish, y0, x0 + halfish, y0);
    }

    /*
     * 
     */
    
    private void clearRects(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            clearRect(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, transform);
        }
    }

    private void clearRect(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GTransform transform) {
        g.setTransform(transform);
        g.clearRect(x, y, xSpan, ySpan);
    }

    /*
     * 
     */
    
    private void drawPoints(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        final int xSpan = 1;
        if (xSpan >= CELL_HALF_INNER_SPAN) {
            // Would draw out of range.
            throw new AssertionError();
        }

        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            for (int x = 0; x <= CELL_HALF_INNER_SPAN - xSpan; x += (xSpan + 1)) {
                // Multiple lines of points, to see pattern more easily.
                for (int y = 0; y < CELL_QUARTER_INNER_SPAN; y++) {
                    drawPoint(g, LOCAL_OFFSET + x, LOCAL_OFFSET + y, transform);
                }
            }
        }
    }
    
    private void drawPoint(InterfaceBwdGraphics g, int x, int y, GTransform transform) {
        g.setTransform(transform);
        g.drawPoint(x, y);
    }
    
    /*
     * 
     */

    private void drawLines(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int span = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawLine(g, LOCAL_OFFSET, LOCAL_OFFSET, LOCAL_OFFSET + span-1, LOCAL_OFFSET, transform);
            drawLine(g, LOCAL_OFFSET + span-1, LOCAL_OFFSET, LOCAL_OFFSET + span-1, LOCAL_OFFSET + span-1, transform);
            drawLine(g, LOCAL_OFFSET, LOCAL_OFFSET, LOCAL_OFFSET + span-1, LOCAL_OFFSET + span-1, transform);
        }
    }
    
    private void drawLine(InterfaceBwdGraphics g, int x1, int y1, int x2, int y2, GTransform transform) {
        g.setTransform(transform);
        g.drawLine(x1, y1, x2, y2);
    }
    
    /*
     * Lines of various types (1-length or not, vertical/horizontal or not, etc.),
     * to spot eventual special cases with general lines.
     */
    
    private void drawVariousLineTypes(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        final int span = 4;
        if (span >= CELL_HALF_INNER_SPAN) {
            // Would draw out of range.
            throw new AssertionError();
        }
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            int typeIndex = 0;
            for (int x = 0; x < CELL_HALF_INNER_SPAN - span; x += (span+1)) {
                for (int y = 0; y < CELL_QUARTER_INNER_SPAN - span; y += (span+1)) {
                    if (typeIndex == 0) {
                        // Point.
                        drawLine(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                transform);
                    } else if (typeIndex == 1) {
                        // Horizontal.
                        drawLine(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                LOCAL_OFFSET + x + (span - 1),
                                LOCAL_OFFSET + y,
                                transform);
                    } else if (typeIndex == 2) {
                        // Vertical.
                        drawLine(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y + (span - 1),
                                transform);
                    } else if (typeIndex == 3) {
                        // Diagonal.
                        drawLine(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                LOCAL_OFFSET + x + (span - 1),
                                LOCAL_OFFSET + y + (span - 1),
                                transform);
                    } else if (typeIndex == 4) {
                        // Below diagonal.
                        drawLine(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                LOCAL_OFFSET + x + (span - 1)/2,
                                LOCAL_OFFSET + y + (span - 1),
                                transform);
                    } else if (typeIndex == 5) {
                        // Above diagonal.
                        drawLine(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                LOCAL_OFFSET + x + (span - 1),
                                LOCAL_OFFSET + y + (span - 1)/2,
                                transform);
                        
                        // Reset.
                        typeIndex = -1;
                    }
                    
                    typeIndex++;
                }
            }
        }
    }
    
    /*
     * 
     */

    private void drawLineStipples(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int span = CELL_QUARTER_INNER_SPAN;
        
        final int factor = 1;
        // 1001_1000_1110_1110
        // (AFAIK, the pattern is read backward,
        // i.e. first pixel won't be draw, then 3 pixels, etc.)
        final short pattern = (short)0x98EE;

        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            final int x1 = LOCAL_OFFSET;
            final int y1 = LOCAL_OFFSET;
            final int x2 = x1 + span-1;
            final int y2 = y1 + span-1;
            // Diagonal first, for pattern start to be more visible,
            // then lines following each other.
            int pixelNum = 0;
            pixelNum = drawLineStipple(g, x1, y1, x2, y2, factor, pattern, pixelNum, transform);
            pixelNum = drawLineStipple(g, x2, y2, x2, y1, factor, pattern, pixelNum, transform);
            pixelNum = drawLineStipple(g, x2, y1, x1, y1, factor, pattern, pixelNum, transform);
        }
    }
    
    private int drawLineStipple(
            InterfaceBwdGraphics g,
            int x1, int y1, int x2, int y2,
            int factor,
            short pattern,
            int pixelNum,
            GTransform transform) {
        g.setTransform(transform);
        return g.drawLineStipple(x1, y1, x2, y2, factor, pattern, pixelNum);
    }

    /*
     * 
     */
    
    private void drawRects(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawRect(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, transform);
        }
    }

    private void drawRect(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GTransform transform) {
        g.setTransform(transform);
        g.drawRect(x, y, xSpan, ySpan);
    }
    
    /*
     * 
     */
    
    private void drawVariousRectTypes(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        final int span = 2;
        if (span >= CELL_HALF_INNER_SPAN) {
            // Would draw out of range.
            throw new AssertionError();
        }
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            int typeIndex = 0;
            for (int x = 0; x < CELL_HALF_INNER_SPAN - span; x += (span+1)) {
                for (int y = 0; y < CELL_QUARTER_INNER_SPAN - span; y += (span+1)) {
                    if (typeIndex == 0) {
                        // Point.
                        drawRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                1,
                                1,
                                transform);
                    } else if (typeIndex == 1) {
                        // Horizontal.
                        drawRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                span,
                                1,
                                transform);
                    } else if (typeIndex == 2) {
                        // Vertical.
                        drawRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                1,
                                span,
                                transform);
                    } else if (typeIndex == 3) {
                        // Square.
                        drawRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                span,
                                span,
                                transform);
                        
                        // Reset.
                        typeIndex = -1;
                    }
                    
                    typeIndex++;
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private void fillRects(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            fillRect(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, transform);
        }
    }
    
    private void fillRect(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GTransform transform) {
        g.setTransform(transform);
        g.fillRect(x, y, xSpan, ySpan);
    }
    
    /*
     * 
     */
    
    private void fillVariousRectTypes(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        final int span = 2;
        if (span >= CELL_HALF_INNER_SPAN) {
            // Would draw out of range.
            throw new AssertionError();
        }
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            int typeIndex = 0;
            for (int x = 0; x < CELL_HALF_INNER_SPAN - span; x += (span+1)) {
                for (int y = 0; y < CELL_QUARTER_INNER_SPAN - span; y += (span+1)) {
                    if (typeIndex == 0) {
                        // Point.
                        fillRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                1,
                                1,
                                transform);
                    } else if (typeIndex == 1) {
                        // Horizontal.
                        fillRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                span,
                                1,
                                transform);
                    } else if (typeIndex == 2) {
                        // Vertical.
                        fillRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                1,
                                span,
                                transform);
                    } else if (typeIndex == 3) {
                        // Square.
                        fillRect(
                                g,
                                LOCAL_OFFSET + x,
                                LOCAL_OFFSET + y,
                                span,
                                span,
                                transform);
                        
                        // Reset.
                        typeIndex = -1;
                    }
                    
                    typeIndex++;
                }
            }
        }
    }

    /*
     * 
     */
    
    private void drawCircles(
            InterfaceBwdGraphics g,
            int cellIndex,
            boolean oddXSpan,
            boolean oddYSpan) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            int x = LOCAL_OFFSET;
            int y = LOCAL_OFFSET;
            int xSpan = (oddXSpan ? floorOddLength(CELL_HALF_INNER_SPAN) : floorEvenLength(CELL_HALF_INNER_SPAN));
            int ySpan = (oddYSpan ? floorOddLength(CELL_HALF_INNER_SPAN) : floorEvenLength(CELL_HALF_INNER_SPAN));

            final int delta = 3;
            
            while ((xSpan >= 1) && (ySpan >= 1)) {
                drawOval(g, x, y, xSpan, ySpan, transform);
                x += delta;
                y += delta;
                xSpan -= 2 * delta;
                ySpan -= 2 * delta;
            }
        }
    }
    
    /*
     * 
     */
    
    private void fillCircles(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        final int centerX = LOCAL_OFFSET + CELL_QUARTER_INNER_SPAN;
        final int centerY = LOCAL_OFFSET + CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            int radiusMinusDotFive;
            BwdColor color = COLOR_FG;
            color = fillCircle(g, centerX, centerY, radiusMinusDotFive = CELL_QUARTER_INNER_SPAN, transform, color);
            final int spacing = 3;
            for (radiusMinusDotFive = CELL_QUARTER_INNER_SPAN - spacing; radiusMinusDotFive >= 2; radiusMinusDotFive -= spacing) {
                color = fillCircle(g, centerX, centerY, radiusMinusDotFive, transform, color);
            }
        }
    }
    
    private BwdColor fillCircle(InterfaceBwdGraphics g, int centerX, int centerY, int radiusMinusDotFive, GTransform transform, BwdColor color) {
        g.setTransform(transform);
        g.setColor(color);
        
        final int rIsh = radiusMinusDotFive;
        final int diameter = 2 * rIsh + 1;
        
        g.fillOval(centerX - rIsh, centerY - rIsh, diameter, diameter);
        
        if (color == COLOR_FG) {
            return COLOR_BG;
        } else {
            return COLOR_FG;
        }
    }

    /*
     * 
     */
    
    private void drawOvals(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawOval(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, transform);
        }
    }
    
    private void drawOval(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GTransform transform) {
        g.setTransform(transform);
        g.drawOval(x, y, xSpan, ySpan);
    }
    
    /*
     * 
     */
    
    private void fillOvals(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;

        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            BwdColor color = COLOR_FG;
            final int step = CELL_QUARTER_INNER_SPAN/8;
            int space = 0;
            for (int k = 0; k < 5; k++) {
                color = fillOval(g, LOCAL_OFFSET + space, LOCAL_OFFSET + space, xSpan-2*space, ySpan-2*space, transform, color);
                space += step;
            }
        }
    }
    
    private BwdColor fillOval(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GTransform transform, BwdColor color) {
        g.setTransform(transform);
        g.setColor(color);
        
        g.fillOval(x, y, xSpan, ySpan);
        
        if (color == COLOR_FG) {
            return COLOR_BG;
        } else {
            return COLOR_FG;
        }
    }

    /*
     * 
     */
    
    private void drawArcs(
            InterfaceBwdGraphics g,
            int xSpan,
            int ySpan,
            double startDeg,
            double spanDeg,
            int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawArc(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, startDeg, spanDeg, transform);
        }
    }
    
    private void drawArc(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            GTransform transform) {
        /*
         * Sometimes using setTransform(...),
         * sometimes using addTransform(...),
         * to test that both work.
         */
        if (x < y) {
            g.setTransform(transform);
        } else {
            // Reset.
            g.setTransform(GTransform.IDENTITY);
            // Adding translation.
            g.addTransform(GTransform.valueOf(0, transform.frame2XIn1(), transform.frame2YIn1()));
            // Adding rotation.
            g.addTransform(GTransform.valueOf(transform.rotation().angDeg(), 0, 0));
        }
        g.drawArc(x, y, xSpan, ySpan, startDeg, spanDeg);
    }
    
    /*
     * 
     */
    
    private void fillArcs(
            InterfaceBwdGraphics g,
            int xSpan,
            int ySpan,
            double startDeg,
            double spanDeg,
            int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            fillArc(g, LOCAL_OFFSET, LOCAL_OFFSET,
                    xSpan, ySpan, startDeg, spanDeg, transform);
        }
    }
    
    private void fillArc(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg,
            GTransform transform) {
        g.setTransform(transform);
        g.fillArc(x, y, xSpan, ySpan, startDeg, spanDeg);
    }
    
    /*
     * 
     */
    
    private void drawPolylines(
            InterfaceBwdGraphics g,
            int cellIndex,
            int pointCount,
            double roundCount) {
        final boolean isFillElseDraw = false;
        final boolean isPolyline = true;
        drawOrFillPolys(
                g,
                cellIndex,
                pointCount,
                roundCount,
                isFillElseDraw,
                isPolyline);
    }
    
    private void drawPolygons(
            InterfaceBwdGraphics g,
            int cellIndex,
            int pointCount,
            double roundCount) {
        final boolean isFillElseDraw = false;
        final boolean isPolyline = false;
        drawOrFillPolys(
                g,
                cellIndex,
                pointCount,
                roundCount,
                isFillElseDraw,
                isPolyline);
    }
    
    private void fillPolygons(
            InterfaceBwdGraphics g,
            int cellIndex,
            int pointCount,
            double roundCount) {
        final boolean isFillElseDraw = true;
        final boolean isPolyline = false;
        drawOrFillPolys(
                g,
                cellIndex,
                pointCount,
                roundCount,
                isFillElseDraw,
                isPolyline);
    }
    
    private void drawOrFillPolys(
            InterfaceBwdGraphics g,
            int cellIndex,
            int pointCount,
            double roundCount,
            boolean isFillElseDraw,
            boolean isPolyline) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawOrFillPoly(
                    g,
                    pointCount,
                    roundCount,
                    isFillElseDraw,
                    isPolyline,
                    transform);
        }
    }
    
    private void drawOrFillPoly(
            InterfaceBwdGraphics g,
            int pointCount,
            double roundCount,
            boolean isFillElseDraw,
            boolean isPolyline,
            GTransform transform) {
        
        g.setTransform(transform);
        
        final int span = CELL_HALF_INNER_SPAN;
        
        final int xMaxRadius = span / 2;
        final int yMaxRadius = span / 2;
        final int[] xArr = new int[pointCount];
        final int[] yArr = new int[pointCount];
        BwdTestUtils.computeSpiralPolygonPoints(
                xMaxRadius,
                yMaxRadius,
                pointCount,
                roundCount,
                xArr,
                yArr);
        g.addTransform(GTransform.valueOf(0, xMaxRadius, yMaxRadius));
        if (isFillElseDraw) {
            g.fillPolygon(xArr, yArr, pointCount);
        } else {
            if (isPolyline) {
                // Shifting points for non-drawn segment
                // to be on spiral edge, not center.
                for (int i = pointCount / 2; i < pointCount; i++) {
                    int j = ((i + (pointCount >> 1)) % pointCount);
                    int tmp;
                    tmp = xArr[i]; xArr[i] = xArr[j]; xArr[j] = tmp;
                    tmp = yArr[i]; yArr[i] = yArr[j]; yArr[j] = tmp;
                }
                g.drawPolyline(xArr, yArr, pointCount);
            } else {
                g.drawPolygon(xArr, yArr, pointCount);
            }
        }
        g.removeLastAddedTransform();
    }

    /*
     * 
     */
    
    private InterfaceBwdFont ensureAndGetFont() {
        final InterfaceBwdFontHome fontHome = this.binding.getFontHome();
        
        InterfaceBwdFont font = this.font;
        if (font == null) {
            font = fontHome.newFontWithClosestHeight(TARGET_FONT_HEIGHT);
            this.font = font;
        }
        
        return font;
    }
    
    private void drawTexts(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        final InterfaceBwdFont font = this.ensureAndGetFont();
        g.setFont(font);
        
        final int fontHeight = font.metrics().height();
        
        final String text = "Moy";
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawText(g, LOCAL_OFFSET, LOCAL_OFFSET, text, transform);
            drawText(g, LOCAL_OFFSET, LOCAL_OFFSET + fontHeight, text, transform);
        }
    }
    
    private void drawText(InterfaceBwdGraphics g, int x, int y, String string, GTransform transform) {
        g.setTransform(transform);
        g.drawText(x, y, string);
    }
    
    /*
     * 
     */

    /**
     * Visual testing (through usage) of the computation of a transform for drawing
     * a rotated drawing, in a specified and non-rotated box.
     */
    private void drawStringInBoxRotated(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int HS = CELL_HALF_INNER_SPAN;
        final int QS = CELL_QUARTER_INNER_SPAN;
        
        // Box = upper of bottom-right quadrant.
        drawStringInBoxRotated(g, x0 + LOCAL_OFFSET, y0 + LOCAL_OFFSET, HS, QS, GRotation.ROT_0);
        
        // Box = right of bottom-left quadrant.
        drawStringInBoxRotated(g, x0 - QS, y0 + LOCAL_OFFSET, QS, HS, GRotation.ROT_90);
        
        // Box = bottom of top-left quadrant.
        drawStringInBoxRotated(g, x0 - HS, y0 - QS, HS, QS, GRotation.ROT_180);
        
        // Box = left of top-right quadrant.
        drawStringInBoxRotated(g, x0 + LOCAL_OFFSET, y0 - HS, QS, HS, GRotation.ROT_270);
    }
    
    private void drawStringInBoxRotated(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GRotation rotation) {
        final GTransform transform = GTransform.valueOf(rotation, x, y, xSpan, ySpan);
        
        g.setTransform(transform);
        
        /*
         * 
         */
        
        // Will be rotated, such as to match the non-rotated box.
        g.drawRect(0, 0, CELL_HALF_INNER_SPAN, CELL_QUARTER_INNER_SPAN);
        
        /*
         * 
         */
        
        final InterfaceBwdFont font = this.ensureAndGetFont();
        g.setFont(font);

        g.drawText(
                0,
                0,
                "" + rotation.angDeg());
    }
    
    /*
     * 
     */
    
    /**
     * Draws two rectangles, one just in the clip and one just outside,
     * such as only the inner one must be visible.
     */
    private void drawClipped(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawClipped(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, transform);
        }
    }
    
    private void drawClipped(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GTransform transform) {
        final GRect tClip = GRect.valueOf(x, y, xSpan, ySpan);
        g.setTransform(transform);
        g.removeAllAddedClips();
        g.addClipInBase(transform.rectIn1(tClip));
        // Must not be drawn.
        g.drawRect(tClip.x()-1, tClip.y()-1, tClip.xSpan()+2, tClip.ySpan()+2);
        // Must be drawn.
        g.drawRect(tClip);
        g.removeAllAddedClips();
    }

    /*
     * 
     */
    
    private void flipColorsColoredLines(
            InterfaceBwdGraphics g,
            int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            flipColorsColoredLines(
                    g,
                    LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan,
                    transform);
        }
    }
    
    private void flipColorsColoredLines(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            GTransform transform) {
        g.setTransform(transform);
        
        /*
         * Drawing lines of random colors, so that we can check
         * that rework works consistently across implementations,
         * whatever the colors.
         * 
         * Half of each line won't be reworked, and serves as reference
         * for expected result.
         * Result is OK if we only see a single line for each x.
         */
        
        final Random random = TestUtils.newRandom123456789L();
        
        final int yMax = y + ySpan - 1;
        final int halfYSpan = ySpan / 2;
        for (int i = 0; i < xSpan; i++) {
            final int _x = x + i;

            // Painting upper part with expected color.
            final BwdColor expectedColor = randomColor(random);
            g.setColor(expectedColor);
            g.drawLine(_x, y, _x, y + halfYSpan);

            // Painting lower part with inverted color:
            // since our bindings flipping do more precisely invert,
            // we must end up with the same colors as in the upper part.
            final BwdColor colorToFlip = invertedColor(expectedColor);
            g.setColor(colorToFlip);
            g.drawLine(_x, y + halfYSpan, _x, yMax);
        }

        // Reworking lower part: should end up with one-color lines.
        g.flipColors(x, y + halfYSpan, xSpan, ySpan - halfYSpan);
    }

    /*
     * 
     */
    
    /**
     * Useful to test that rework also works on pixels resulting from fillRect(...),
     * because with some bindings (like based on QtJambi 4.8), fillRect(...) is
     * kind of special and doesn't cause Xor, unlike simple and normally equivalent
     * drawLine(...) calls.
     */
    private void flipColorsFilledRectHalf(
            InterfaceBwdGraphics g,
            int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            flipColorsFilledRectHalf(
                    g,
                    LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan,
                    transform);
        }
    }
    
    private void flipColorsFilledRectHalf(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            GTransform transform) {
        g.setTransform(transform);
        
        // Must not be grey, else see no effect
        // (since ou flippings do invert).
        g.setColor(BwdColor.YELLOW);
        
        g.fillRect(x, y, xSpan, ySpan);
        
        final int halfYSpan = ySpan/2;

        // Reworking lower part.
        g.flipColors(x, y+halfYSpan, xSpan, ySpan-halfYSpan);
    }
    
    /*
     * 
     */

    private void drawImages(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);

        final InterfaceBwdImage image = this.image_struct_grey;
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawImage(g, LOCAL_OFFSET, LOCAL_OFFSET, image, transform);
        }
    }
    
    private  void drawImage(
            InterfaceBwdGraphics g,
            int x, int y,
            InterfaceBwdImage image,
            GTransform transform) {
        g.setTransform(transform);
        g.drawImage(x, y, image);
    }

    /*
     * 
     */
    
    private void drawImages_adjusted_1(
            InterfaceBwdGraphics g,
            int xSpan,
            int ySpan,
            InterfaceBwdImage image,
            int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawImage_adjusted_1(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, image, transform);
        }
    }
    
    private void drawImage_adjusted_1(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, InterfaceBwdImage image, GTransform transform) {
        g.setTransform(transform);
        g.drawImage(
                x, y, xSpan, ySpan,
                image);
    }

    /*
     * 
     */
    
    private void drawImages_adjusted_2(
            InterfaceBwdGraphics g,
            int xSpan,
            int ySpan,
            InterfaceBwdImage image,
            int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawImage_adjusted_2(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, image, transform);
        }
    }
    
    private void drawImage_adjusted_2(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            GTransform transform) {
        final int sxSpan = image.getWidth();
        if (sxSpan <= 0) {
            return;
        }
        final int sySpan = image.getHeight();
        if (sySpan <= 0) {
            return;
        }
        
        g.setTransform(transform);
        g.drawImage(
                x,
                y,
                xSpan,
                ySpan,
                image,
                0, // sx
                10, // sy
                21, // sxSpan
                11); // sySpan
    }
    
    /*
     * 
     */
    
    private void drawImagesColoredLinesReworked(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_QUARTER_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawImageColoredLinesReworked(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, transform);
        }
    }
    
    private void drawImageColoredLinesReworked(InterfaceBwdGraphics g, int x, int y, int xSpan, int ySpan, GTransform transform) {
        g.setTransform(transform);

        final InterfaceBwdImage image = this.image_struct_color;

        drawImage(g, LOCAL_OFFSET, LOCAL_OFFSET, image, transform);
        
        g.flipColors(x, y, image.getWidth(), image.getHeight());
    }
    
    /*
     * 
     */
    
    private void drawImagesColoredXor(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final InterfaceBwdImage image = this.image_struct_color;
        
        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawImage(g, LOCAL_OFFSET, LOCAL_OFFSET, image, transform);
        }
    }
    
    /*
     * 
     */
    
    private void drawImagesColoredXor_adjusted_1(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final InterfaceBwdImage image = this.image_struct_color;

        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_HALF_INNER_SPAN;
        final int ySpan = CELL_HALF_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawImage_adjusted_1(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, image, transform);
        }
    }

    /*
     * 
     */
    
    private void drawImagesColoredXor_adjusted_2(InterfaceBwdGraphics g, int cellIndex) {
        resetColor(g);
        
        final InterfaceBwdImage image = this.image_struct_color;

        final int x0 = cellCenterX(g, cellIndex);
        final int y0 = cellCenterY(g, cellIndex);
        final int xSpan = CELL_QUARTER_INNER_SPAN;
        final int ySpan = CELL_HALF_INNER_SPAN;
        
        for (GTransform transform : getQuadrantTransforms(x0, y0)) {
            drawImage_adjusted_2(g, LOCAL_OFFSET, LOCAL_OFFSET, xSpan, ySpan, image, transform);
        }
    }

    /*
     * 
     */

    private static int floorOddLength(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("" + length);
        }
        if (NbrsUtils.isOdd(length)) {
            return length;
        } else {
            return length - 1;
        }
    }
    
    private static int floorEvenLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("" + length);
        }
        if (NbrsUtils.isEven(length)) {
            return length;
        } else {
            return length - 1;
        }
    }
}
